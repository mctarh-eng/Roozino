package com.roozino.app.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.roozino.app.AppViewModel
import com.roozino.app.UiSettings
import com.roozino.app.ui.components.SectionTitle
import com.roozino.app.ui.components.SubScaffold

@Composable
fun SettingsScreen(vm: AppViewModel, settings: UiSettings, nav: NavController) {
    SubScaffold(title = "تنظیمات", nav = nav) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            SectionTitle("حالت نمایش")
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                listOf("system" to "مطابق سیستم", "light" to "روشن", "dark" to "تاریک").forEach { (v, label) ->
                    FilterChip(
                        selected = settings.themeMode == v,
                        onClick = { vm.setSetting("theme_mode", v) },
                        label = { Text(label) }
                    )
                }
            }

            SectionTitle("شکل اعداد")
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                listOf("1" to "فارسی (۱۲۳)", "0" to "انگلیسی (123)").forEach { (v, label) ->
                    FilterChip(
                        selected = settings.persianDigits == (v == "1"),
                        onClick = { vm.setSetting("persian_digits", v) },
                        label = { Text(label) }
                    )
                }
            }

            SectionTitle("اندازه فونت")
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                listOf("0.85" to "کوچک", "1" to "متوسط", "1.2" to "بزرگ").forEach { (v, label) ->
                    FilterChip(
                        selected = settings.fontScale == v.toFloatOrNull(),
                        onClick = { vm.setSetting("font_scale", v) },
                        label = { Text(label) }
                    )
                }
            }

            SectionTitle("مدیریت داده")
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("پشتیبان‌گیری و بازیابی", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "همه داده‌هایت را در یک فایل ذخیره کن یا به گوشی دیگر منتقل کن.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = { nav.navigate("backup") }) { Text("رفتن به پشتیبان‌گیری") }
                }
            }

            SectionTitle("درباره روزینو")
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("روزینو ۰.۱.۰ (نسخه آزمایشی)", fontWeight = FontWeight.Bold)
                    Text(
                        "دستیار روزمره فارسی — کاملاً آفلاین. همه داده‌ها فقط روی همین گوشی ذخیره می‌شوند؛ هیچ داده‌ای جایی ارسال نمی‌شود و برنامه هیچ مجوز اینترنتی ندارد.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Text(
                        "ورودی صوتی از سرویس گفتار خود گوشی استفاده می‌کند و متن آن پیش از ثبت، قابل ویرایش است.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            Spacer(Modifier.height(30.dp))
        }
    }
}
