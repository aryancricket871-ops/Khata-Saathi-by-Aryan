package com.aryandigital.khata

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object Cx {
    val red = Color(0xFFDC2626)
    val green = Color(0xFF16A34A)
    val gold = Color(0xFFF5B301)
}

private val LightColors = lightColorScheme(
    primary = Color(0xFF0F766E), onPrimary = Color.White,
    primaryContainer = Color(0xFFCCFBF1), onPrimaryContainer = Color(0xFF042F2E),
    tertiaryContainer = Color(0xFFFFF0BF), onTertiaryContainer = Color(0xFF3B2A00),
    background = Color(0xFFF6FAF9), surface = Color(0xFFF6FAF9)
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFF2DD4BF), onPrimary = Color(0xFF003733),
    primaryContainer = Color(0xFF0F4F4A), onPrimaryContainer = Color(0xFFCCFBF1),
    tertiaryContainer = Color(0xFF4A3A00), onTertiaryContainer = Color(0xFFFFE08A),
    background = Color(0xFF0E1513), surface = Color(0xFF0E1513)
)

@Composable
fun KhataTheme(dark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, content = content)
}

// ---------- helpers ----------
private val nf: NumberFormat by lazy {
    NumberFormat.getNumberInstance(Locale.forLanguageTag("en-IN")).apply {
        maximumFractionDigits = 2; minimumFractionDigits = 0
    }
}

fun money(v: Double): String = "₹" + nf.format(Math.abs(v))
fun dateStr(ms: Long): String = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(ms))

fun endOfToday(): Long = Calendar.getInstance().apply {
    set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
}.timeInMillis

/** DatePicker UTC मिलीसेकंड -> उसी तारीख की लोकल दिन-शुरुआत */
fun pickerToLocal(utc: Long): Long {
    val u = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utc }
    return Calendar.getInstance().apply {
        clear(); set(u.get(Calendar.YEAR), u.get(Calendar.MONTH), u.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}

fun toast(ctx: Context, msg: String) = Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()

fun normalizePhone(p: String): String {
    val d = p.filter { it.isDigit() }
    return if (d.length == 10) "91$d" else d
}

private fun safeStart(ctx: Context, i: Intent) {
    try { ctx.startActivity(i) } catch (e: Exception) { toast(ctx, tr("कोई ऐप नहीं मिला", "No app found")) }
}

fun openWhatsApp(ctx: Context, phone: String, text: String) =
    safeStart(ctx, Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/${normalizePhone(phone)}?text=${Uri.encode(text)}")))

fun sendSms(ctx: Context, phone: String, text: String) =
    safeStart(ctx, Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${phone.filter { it.isDigit() || it == '+' }}"))
        .putExtra("sms_body", text))

fun dial(ctx: Context, phone: String) =
    safeStart(ctx, Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))

fun shareText(ctx: Context, text: String) =
    safeStart(ctx, Intent.createChooser(
        Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), null))

// ---------- small components ----------
@Composable
fun Avatar(name: String, size: Int = 44) {
    Box(
        Modifier.size(size.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Text(
            name.trim().take(1).uppercase(), fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = (size / 2.4).sp
        )
    }
}

/** नीचे का टेक्स्ट बैनर: अपनी सर्विस का प्रमोशन */
@Composable
fun PromoBanner() {
    val ctx = LocalContext.current
    val msgs = listOf(
        tr("📱 अपना ऐप बनवाना है? Play Store पर डलवाना है? मैसेज करें",
            "📱 Want your own app on Play Store? Message us"),
        tr("🌐 वेबसाइट • गेम • ऐप — Aryan Digital Solutions",
            "🌐 Website • Game • App — Aryan Digital Solutions"),
        tr("📞 WhatsApp करें: $OWNER_PHONE_PRETTY", "📞 WhatsApp us: $OWNER_PHONE_PRETTY")
    )
    var i by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { while (true) { delay(4000); i = (i + 1) % 3 } }
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        modifier = Modifier.fillMaxWidth().clickable {
            openWhatsApp(ctx, OWNER_PHONE, tr("नमस्ते, मुझे अपना ऐप बनवाना है।", "Hello, I want to get an app built."))
        }
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                msgs[i], color = MaterialTheme.colorScheme.onTertiaryContainer,
                fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis
            )
        }
    }
}
