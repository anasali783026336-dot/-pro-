package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.example.DataGuardianApplication
import com.example.MainActivity
import com.example.R
import com.example.data.model.FirewallPolicy
import com.example.data.model.NetworkType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer

class DataGuardianVpnService : VpnService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var vpnInterface: ParcelFileDescriptor? = null
    private var packetLoopJob: Job? = null

    private lateinit var connectivityManager: ConnectivityManager
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    companion object {
        const val ACTION_START = "com.example.service.START_FIREWALL"
        const val ACTION_STOP = "com.example.service.STOP_FIREWALL"
        const val ACTION_RELOAD = "com.example.service.RELOAD_RULES"

        const val CHANNEL_ID = "data_guardian_firewall_channel"
        const val NOTIFICATION_ID = 1001

        private val _isFirewallRunning = MutableStateFlow(false)
        val isFirewallRunning: StateFlow<Boolean> = _isFirewallRunning.asStateFlow()

        private val _blockedAppsCount = MutableStateFlow(0)
        val blockedAppsCount: StateFlow<Int> = _blockedAppsCount.asStateFlow()

        fun startService(context: Context) {
            val intent = Intent(context, DataGuardianVpnService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, DataGuardianVpnService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun reloadRules(context: Context) {
            val intent = Intent(context, DataGuardianVpnService::class.java).apply {
                action = ACTION_RELOAD
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        createNotificationChannel()
        registerNetworkCallback()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopFirewall()
                return START_NOT_STICKY
            }
            ACTION_RELOAD -> {
                if (_isFirewallRunning.value) {
                    reconfigureVpn()
                }
            }
            ACTION_START, null -> {
                startForeground(NOTIFICATION_ID, createNotification("جاري تهيئة جدار الحماية المحلي..."))
                startFirewall()
            }
        }
        return START_STICKY
    }

    private fun startFirewall() {
        serviceScope.launch {
            try {
                reconfigureVpn()
                _isFirewallRunning.value = true
            } catch (e: Exception) {
                e.printStackTrace()
                stopFirewall()
            }
        }
    }

    private fun reconfigureVpn() {
        serviceScope.launch {
            val app = application as? DataGuardianApplication ?: return@launch
            val firewallRepo = app.firewallRepository
            val allRules = app.database.firewallRuleDao().getAllRulesDirect()
            val activeNetwork = getCurrentNetworkType()

            // Filter apps that should be blocked on the active network
            val blockedPackages = mutableListOf<String>()
            for (rule in allRules) {
                val policy = try { FirewallPolicy.valueOf(rule.policy) } catch (_: Exception) { FirewallPolicy.ALLOW_ALL }
                val isAllowed = policy.isAllowedOn(activeNetwork)
                if (!isAllowed) {
                    blockedPackages.add(rule.packageName)
                }
            }

            _blockedAppsCount.value = blockedPackages.size

            // Update Notification
            val notifText = if (blockedPackages.isEmpty()) {
                "جدار الحماية نشط • جميع التطبيقات مسموح بها حالياً"
            } else {
                "جدار الحماية نشط • يتم حظر ${blockedPackages.size} تطبيق على ${activeNetwork.titleAr}"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(NOTIFICATION_ID, createNotification(notifText))

            // Stop previous packet loop
            packetLoopJob?.cancel()
            vpnInterface?.close()
            vpnInterface = null

            // Build new VPN configuration
            try {
                val builder = Builder()
                    .setSession(getString(R.string.app_name))
                    .setMtu(1500)
                    .addAddress("10.120.0.1", 32)
                    .addRoute("10.120.0.0", 24)

                // Route blocked applications through the blackhole VPN interface
                var addedCount = 0
                for (pkg in blockedPackages) {
                    try {
                        packageManager.getPackageInfo(pkg, 0)
                        builder.addAllowedApplication(pkg)
                        addedCount++
                    } catch (_: PackageManager.NameNotFoundException) {
                        // Package uninstalled
                    }
                }

                // If no apps are currently blocked, route our own package to keep tunnel open safely
                if (addedCount == 0) {
                    builder.addAllowedApplication(packageName)
                }

                vpnInterface = builder.establish()

                vpnInterface?.let { pfd ->
                    startPacketDropLoop(pfd)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun startPacketDropLoop(pfd: ParcelFileDescriptor) {
        packetLoopJob = serviceScope.launch(Dispatchers.IO) {
            val inputStream = FileInputStream(pfd.fileDescriptor)
            val buffer = ByteBuffer.allocate(32768)
            val app = application as? DataGuardianApplication

            while (isActive) {
                try {
                    val length = inputStream.read(buffer.array())
                    if (length > 0) {
                        // Blocked packet captured by VPN!
                        // In interactive mode, log the attempt
                        val networkType = getCurrentNetworkType().titleAr
                        app?.firewallRepository?.logAttempt(
                            packageName = "تطبيق مقيد",
                            appName = "حركة شبكة محظورة",
                            networkType = networkType,
                            action = "تم حظر محاولة اتصال عبر الجدار"
                        )
                        buffer.clear()
                    } else if (length < 0) {
                        break
                    }
                } catch (_: Exception) {
                    break
                }
            }
        }
    }

    private fun getCurrentNetworkType(): NetworkType {
        val activeNetwork = connectivityManager.activeNetwork ?: return NetworkType.NONE
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return NetworkType.NONE
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.CELLULAR
            else -> NetworkType.NONE
        }
    }

    private fun registerNetworkCallback() {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                if (_isFirewallRunning.value) {
                    reconfigureVpn()
                }
            }

            override fun onLost(network: Network) {
                if (_isFirewallRunning.value) {
                    reconfigureVpn()
                }
            }
        }
        connectivityManager.registerNetworkCallback(request, networkCallback!!)
    }

    private fun stopFirewall() {
        _isFirewallRunning.value = false
        _blockedAppsCount.value = 0
        packetLoopJob?.cancel()
        packetLoopJob = null

        try {
            vpnInterface?.close()
            vpnInterface = null
        } catch (_: Exception) {}

        networkCallback?.let {
            try {
                connectivityManager.unregisterNetworkCallback(it)
            } catch (_: Exception) {}
            networkCallback = null
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "جدار الحماية المحلي",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "إشعارات تشغيل جدار حماية حارس البيانات"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(content: String): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingOpen = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val stopIntent = Intent(this, DataGuardianVpnService::class.java).apply {
            action = ACTION_STOP
        }
        val pendingStop = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("حارس البيانات PRO")
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingOpen)
            .addAction(0, "إيقاف الحماية", pendingStop)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        stopFirewall()
        serviceScope.cancel()
        super.onDestroy()
    }
}
