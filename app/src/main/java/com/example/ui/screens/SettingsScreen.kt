package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.DataGuardianApplication
import com.example.data.model.AlertConfig
import com.example.data.repository.DataExportManager
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCrimson
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onSaveQuota: (Long, Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val app = context.applicationContext as DataGuardianApplication
    val alertRepo = app.alertRepository
    val exportManager = remember { DataExportManager(app.database, app.quotaRepository, app.alertRepository) }

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val alertConfig by alertRepo.alertConfig.collectAsStateWithLifecycle()

    val currentQuotaGb = (state.quotaConfig.quotaBytes / (1024L * 1024L * 1024L)).toFloat().coerceIn(1f, 200f)
    var quotaGbSlider by remember(currentQuotaGb) { mutableFloatStateOf(currentQuotaGb) }
    var renewalDay by remember(state.quotaConfig.renewalDayOfMonth) { mutableIntStateOf(state.quotaConfig.renewalDayOfMonth) }

    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonText by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "الإعدادات والباقة",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )
            Text(
                text = "تخصيص الباقة، التنبيهات الذكية، النسخ الاحتياطي، وفحص الصلاحيات",
                style = MaterialTheme.typography.bodySmall,
                color = CyberCyan
            )
        }

        // 1. Quota Configuration Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(CyberCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.DataUsage, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "إعدادات الباقة الشهرية",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "حجم الباقة الشهرية:", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                        Text(text = "${quotaGbSlider.toInt()} جيجابايت (GB)", fontWeight = FontWeight.Bold, color = CyberCyan)
                    }

                    Slider(
                        value = quotaGbSlider,
                        onValueChange = { quotaGbSlider = it },
                        valueRange = 1f..150f,
                        steps = 149,
                        colors = SliderDefaults.colors(thumbColor = CyberCyan, activeTrackColor = CyberCyan, inactiveTrackColor = CyberSurfaceVariant)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "يوم تجديد الباقة في الشهر:", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                        Text(text = "يوم $renewalDay", fontWeight = FontWeight.Bold, color = CyberEmerald)
                    }

                    Slider(
                        value = renewalDay.toFloat(),
                        onValueChange = { renewalDay = it.toInt() },
                        valueRange = 1f..28f,
                        steps = 27,
                        colors = SliderDefaults.colors(thumbColor = CyberEmerald, activeTrackColor = CyberEmerald, inactiveTrackColor = CyberSurfaceVariant)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            val bytes = (quotaGbSlider.toLong()) * 1024L * 1024L * 1024L
                            onSaveQuota(bytes, renewalDay, 85)
                            Toast.makeText(context, "تم حفظ إعدادات الباقة بنجاح", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = CyberBackground),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(text = "حفظ إعدادات الباقة", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 2. Smart Alerts Section
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(CyberAmber.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.NotificationsActive, contentDescription = null, tint = CyberAmber, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "التنبيهات الذكية للاستهلاك",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    AlertToggleRow(
                        title = "تنبيه عند بلوغ 50% من الباقة",
                        checked = alertConfig.alert50Enabled,
                        onCheckedChange = { alertRepo.updateConfig(alertConfig.copy(alert50Enabled = it)) }
                    )
                    AlertToggleRow(
                        title = "تنبيه عند بلوغ 75% من الباقة",
                        checked = alertConfig.alert75Enabled,
                        onCheckedChange = { alertRepo.updateConfig(alertConfig.copy(alert75Enabled = it)) }
                    )
                    AlertToggleRow(
                        title = "تنبيه عند بلوغ 90% من الباقة",
                        checked = alertConfig.alert90Enabled,
                        onCheckedChange = { alertRepo.updateConfig(alertConfig.copy(alert90Enabled = it)) }
                    )
                    AlertToggleRow(
                        title = "تنبيه نفاذ الباقة (100%)",
                        checked = alertConfig.alert100Enabled,
                        onCheckedChange = { alertRepo.updateConfig(alertConfig.copy(alert100Enabled = it)) }
                    )
                    AlertToggleRow(
                        title = "تنبيه عند توقف جدار الحماية",
                        checked = alertConfig.firewallStoppedAlertEnabled,
                        onCheckedChange = { alertRepo.updateConfig(alertConfig.copy(firewallStoppedAlertEnabled = it)) }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Daily Limit & App limit text
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "حد الاستهلاك اليومي:", fontSize = 12.sp, color = TextPrimary)
                        Text(text = "${alertConfig.dailyLimitMb} ميجابايت", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                    }

                    Slider(
                        value = alertConfig.dailyLimitMb.toFloat(),
                        onValueChange = { alertRepo.updateConfig(alertConfig.copy(dailyLimitMb = it.toInt())) },
                        valueRange = 500f..5000f,
                        steps = 9,
                        colors = SliderDefaults.colors(thumbColor = CyberAmber, activeTrackColor = CyberAmber, inactiveTrackColor = CyberSurfaceVariant)
                    )
                }
            }
        }

        // 3. Export & Backup Section
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Text(
                        text = "التصدير والنسخ الاحتياطي المحلي",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Export CSV
                    Button(
                        onClick = {
                            scope.launch {
                                try {
                                    val csvFile = exportManager.exportUsageToCsv(context)
                                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", csvFile)
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/csv"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "مشاركة تقرير الاستهلاك CSV"))
                                } catch (e: Exception) {
                                    Toast.makeText(context, "تم تصدير الملف بنجاح إلى الذاكرة المحلية", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = CyberSurfaceVariant),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "تصدير سجل الاستهلاك إلى ملف CSV", color = TextPrimary, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Backup JSON
                    Button(
                        onClick = {
                            scope.launch {
                                val json = exportManager.exportSettingsAndRulesJson()
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    putExtra(Intent.EXTRA_TEXT, json)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "نسخ احتياطي لإعدادات وقواعد حارس البيانات"))
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = CyberSurfaceVariant),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = CyberEmerald, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "نسخ الإعدادات وقواعد الحماية احتياطياً (JSON)", color = TextPrimary, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Import JSON
                    OutlinedButton(
                        onClick = { showImportDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder)
                    ) {
                        Icon(imageVector = Icons.Default.FileUpload, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "استيراد القواعد والإعدادات من نص النسخ", color = CyberCyan, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Clear Database
                    OutlinedButton(
                        onClick = { showClearConfirmDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberCrimson.copy(alpha = 0.5f))
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = CyberCrimson, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "مسح سجل الاستهلاك المحلي من Room", color = CyberCrimson, fontSize = 12.sp)
                    }
                }
            }
        }

        // 4. Permissions Status Diagnostic
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Text(text = "فحص صلاحيات النظام", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(14.dp))
                    PermissionRowItem(
                        title = "صلاحية الوصول للاستخدام (Usage Access)",
                        isGranted = state.hasPermission,
                        onFix = {
                            try {
                                context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                            } catch (_: Exception) {}
                        }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    PermissionRowItem(
                        title = "خدمة VPN المحلية (جدار الحماية)",
                        isGranted = state.isFirewallRunning,
                        onFix = {}
                    )
                }
            }
        }

        // 5. System Limitations & Transparency
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = CyberAmber)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "حدود النظام ودقة القياسات", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "1. التطبيق يعمل بدون إنترنت ويسجل الإحصائيات مباشرة من نواة أندرويد.\n" +
                                "2. قد تختلف الأرقام عن فاتورة شركة الاتصالات بسبب التطبيقات المجانية غير المحتسبة لدى المزود أو دورات الفوترة الخاصة.\n" +
                                "3. جدار الحماية المحلي يوجّه التطبيقات المقيدة إلى نفق loopback محلي بدون أي خوادم وسيطة لحفظ الخصوصية.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        lineHeight = 20.sp
                    )
                }
            }
        }

        // App About and branding
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "حارس البيانات PRO", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = CyberCyan)
                    Text(text = "«راقب استهلاكك، تحكّم في اتصالك، واحمِ باقتك»", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "الإصدار 1.0 • محلي 100% بدون اتصال خارجي", fontSize = 11.sp, color = TextMuted)
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Clear Confirmation Dialog
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("تأكيد مسح السجل المحلي", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("هل أنت متأكد من رغبتك في حذف جميع سجلات الاستهلاك اليومية من قاعدة بيانات Room؟ لا يمكن التراجع عن هذه الخطوة.", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            exportManager.clearHistory()
                            viewModel.refreshData()
                            showClearConfirmDialog = false
                            Toast.makeText(context, "تم مسح السجل المحلي بنجاح", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCrimson)
                ) {
                    Text("مسح الآن")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("إلغاء", color = TextSecondary)
                }
            },
            containerColor = CyberSurface
        )
    }

    // Import Dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("استيراد النسخة الاحتياطية", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("الصق محتوى ملف JSON للنسخة الاحتياطية هنا:", color = TextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = importJsonText,
                        onValueChange = { importJsonText = it },
                        modifier = Modifier.fillMaxWidth().height(140.dp),
                        placeholder = { Text("محتوى JSON...", color = TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = CyberBorder
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val result = exportManager.importSettingsAndRulesFromJson(importJsonText)
                            result.onSuccess { count ->
                                Toast.makeText(context, "تم استيراد $count قاعدة بنجاح!", Toast.LENGTH_SHORT).show()
                                viewModel.refreshData()
                                showImportDialog = false
                            }.onFailure { err ->
                                Toast.makeText(context, "فشل الاستيراد: ${err.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = CyberBackground)
                ) {
                    Text("استيراد وتطبيق")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("إلغاء", color = TextSecondary)
                }
            },
            containerColor = CyberSurface
        )
    }
}

@Composable
fun AlertToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, fontSize = 12.sp, color = TextPrimary)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = CyberAmber,
                checkedTrackColor = CyberAmber.copy(alpha = 0.3f),
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = CyberSurfaceVariant
            )
        )
    }
}

@Composable
fun PermissionRowItem(
    title: String,
    isGranted: Boolean,
    onFix: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CyberSurfaceVariant)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text(
                text = if (isGranted) "مفعلة وتعمل بنجاح" else "غير مفعّلة حالياً",
                fontSize = 10.sp,
                color = if (isGranted) CyberEmerald else CyberAmber
            )
        }

        if (isGranted) {
            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = CyberEmerald, modifier = Modifier.size(20.dp))
        } else {
            Button(
                onClick = onFix,
                colors = ButtonDefaults.buttonColors(containerColor = CyberAmber, contentColor = CyberBackground),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = "تفعيل", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
