package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R

object NotificationHelper {

    const val CHANNEL_QUOTA_ALERTS = "data_guardian_quota_alerts"
    const val CHANNEL_DAILY_ALERTS = "data_guardian_daily_alerts"

    const val NOTIF_ID_QUOTA_ALERT = 2001
    const val NOTIF_ID_DAILY_LIMIT = 2002
    const val NOTIF_ID_HIGH_APP = 2003

    fun initChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val quotaChannel = NotificationChannel(
                CHANNEL_QUOTA_ALERTS,
                "تنبيهات استهلاك باقة الإنترنت",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إشعارات عند بلوغ 50% أو 75% أو 90% أو 100% من الباقة"
            }
            manager.createNotificationChannel(quotaChannel)

            val dailyChannel = NotificationChannel(
                CHANNEL_DAILY_ALERTS,
                "تنبيهات الاستهلاك اليومي والتطبيقات",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "إشعارات عند تجاوز حد الاستهلاك اليومي أو ارتفاع استهلاك تطبيق محدد"
            }
            manager.createNotificationChannel(dailyChannel)
        }
    }

    fun sendQuotaAlert(context: Context, percent: Int, usedFormatted: String, totalFormatted: String) {
        initChannels(context)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val title = when (percent) {
            100 -> "⚠️ تنبيه عاجل: لقد استهلكت 100% من باقة الإنترنت!"
            90 -> "⚠️ تحذير: استهلكت 90% من باقة الإنترنت"
            75 -> "📊 إشعار: استهلكت 75% من باقة الإنترنت"
            else -> "📊 إشعار: استهلكت $percent% من باقة الإنترنت"
        }

        val text = "تم استهلاك $usedFormatted من أصل $totalFormatted. تحكّم في اتصالك عبر حارس البيانات PRO."

        val notif = NotificationCompat.Builder(context, CHANNEL_QUOTA_ALERTS)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        manager.notify(NOTIF_ID_QUOTA_ALERT, notif)
    }

    fun sendDailyLimitAlert(context: Context, usedFormatted: String, limitFormatted: String) {
        initChannels(context)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val text = "تجاوز استهلاكك اليومي $usedFormatted (الحد المعين: $limitFormatted). يمكنك تقييد التطبيقات عبر جدار الحماية."

        val notif = NotificationCompat.Builder(context, CHANNEL_DAILY_ALERTS)
            .setContentTitle("⚠️ تم تجاوز حد الاستهلاك اليومي")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        manager.notify(NOTIF_ID_DAILY_LIMIT, notif)
    }

    fun sendHighAppUsageAlert(context: Context, appName: String, usedFormatted: String) {
        initChannels(context)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            2,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val text = "تطبيق ($appName) استهلك $usedFormatted اليوم. انقر لإدارة سياسة الاتصال للتطبيق."

        val notif = NotificationCompat.Builder(context, CHANNEL_DAILY_ALERTS)
            .setContentTitle("🚨 ارتفاع مفاجئ في استهلاك تطبيق")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        manager.notify(NOTIF_ID_HIGH_APP, notif)
    }
}
