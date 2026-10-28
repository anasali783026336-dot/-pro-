package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onSaveQuota: (Long, Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val currentQuotaGb = (state.quotaConfig.quotaBytes / (1024L * 1024L * 1024L)).toFloat().coerceIn(1f, 200f)
    var quotaGbSlider by remember(currentQuotaGb) { mutableFloatStateOf(currentQuotaGb) }
    var renewalDay by remember(state.quotaConfig.renewalDayOfMonth) { mutableIntStateOf(state.quotaConfig.renewalDayOfMonth) }
    var warningPercent by remember(state.quotaConfig.warningPercent) { mutableIntStateOf(state.quotaConfig.warningPercent) }

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
                text = "تخصيص الباقة الشهرية، فحص الصلاحيات، وحدود النظام",
                style = MaterialTheme.typography.bodySmall,
                color = CyberCyan
            )
        }

        // Quota configuration Card
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
                            Icon(
                                imageVector = Icons.Default.DataUsage,
                                contentDescription = null,
                                tint = CyberCyan,
                                modifier = Modifier.size(20.dp)
                            )
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

                    // Quota Size Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "حجم الباقة الشهرية:", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                        Text(
                            text = "${quotaGbSlider.toInt()} جيجابايت (GB)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = CyberCyan
                        )
                    }

                    Slider(
                        value = quotaGbSlider,
                        onValueChange = { quotaGbSlider = it },
                        valueRange = 1f..150f,
                        steps = 149,
                        colors = SliderDefaults.colors(
                            thumbColor = CyberCyan,
                            activeTrackColor = CyberCyan,
                            inactiveTrackColor = CyberSurfaceVariant
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Renewal Day Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "يوم تجديد الباقة في الشهر:", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                        Text(
                            text = "يوم $renewalDay",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = CyberEmerald
                        )
                    }

                    Slider(
                        value = renewalDay.toFloat(),
                        onValueChange = { renewalDay = it.toInt() },
                        valueRange = 1f..28f,
                        steps = 27,
                        colors = SliderDefaults.colors(
                            thumbColor = CyberEmerald,
                            activeTrackColor = CyberEmerald,
                            inactiveTrackColor = CyberSurfaceVariant
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val bytes = (quotaGbSlider.toLong()) * 1024L * 1024L * 1024L
                            onSaveQuota(bytes, renewalDay, warningPercent)
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

        // Permissions Status Diagnostic
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
                        text = "فحص صلاحيات النظام",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Usage Stats
                    PermissionRowItem(
                        title = "صلاحية الوصول لبيانات الاستخدام (Usage Access)",
                        isGranted = state.hasPermission,
                        onFix = {
                            try {
                                context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                            } catch (_: Exception) {}
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // VPN Service readiness
                    PermissionRowItem(
                        title = "خدمة VPN المحلية (جدار الحماية بدون روت)",
                        isGranted = state.isFirewallRunning,
                        onFix = {}
                    )
                }
            }
        }

        // System Limitations & Transparency
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
                        Text(
                            text = "قيود أندرويد ودقة القياسات",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "1. قياسات الهاتف تقيس بدقة كل البايتات المحمولة عبر كروت الشبكة الحقيقية.\n" +
                                "2. قد تختلف هذه الأرقام عن فاتورة شركة الاتصالات بسبب حزم البيانات المجانية غير المحتسبة لدى المزود أو تقريب الاستهلاك في دورات الفوترة.\n" +
                                "3. جدار الحماية المحلي يوجّه التطبيقات المقيدة إلى نفق محلي (Loopback) بدون أي خوادم خارجية حفاظاً على الخصوصية الكاملة.",
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
                    Text(
                        text = "حارس البيانات PRO",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = CyberCyan
                    )
                    Text(
                        text = "«راقب استهلاكك، تحكّم في اتصالك، واحمِ باقتك»",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "الإصدار 1.0 • محلي 100% بدون اتصال خارجي",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
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
