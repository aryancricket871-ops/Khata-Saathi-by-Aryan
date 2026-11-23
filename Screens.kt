@file:OptIn(ExperimentalMaterial3Api::class)

package com.aryandigital.khata

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

sealed interface Route {
    data class Cust(val id: String) : Route
    data class Ent(val custId: String, val given: Boolean) : Route
    data class EditCust(val id: String?) : Route
    data object About : Route
    data object Privacy : Route
}

@Composable
fun KhataApp(store: Store) {
    var unlocked by remember { mutableStateOf(!store.hasPin) }
    val stack = remember { mutableStateListOf<Route>() }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val pop: () -> Unit = { if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex) }
    BackHandler(enabled = stack.isNotEmpty()) { pop() }

    when {
        !store.onboarded -> Onboarding(store)
        !unlocked -> LockScreen(store) { unlocked = true }
        else -> when (val r = stack.lastOrNull()) {
            null -> MainScreen(store, tab, { tab = it }) { stack.add(it) }
            is Route.Cust -> CustomerScreen(store, r.id, { stack.add(it) }, pop)
            is Route.Ent -> EntryScreen(store, r.custId, r.given, pop)
            is Route.EditCust -> EditCustomerScreen(store, r.id, pop)
            Route.About -> AboutScreen(pop)
            Route.Privacy -> PrivacyScreen(pop)
        }
    }
}

// ================= शुरुआत और लॉक =================
@Composable
fun Onboarding(store: Store) {
    var name by remember { mutableStateOf("") }
    Column(
        Modifier.fillMaxSize().systemBarsPadding().imePadding().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("📒", fontSize = 56.sp)
        Text("Aryan Khata", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text(
            tr("दुकान का उधार-खाता — 100% फ्री", "Your shop's udhaar ledger — 100% free"),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = Lang.hindi, onClick = { store.setHindi(true) }, label = { Text("हिन्दी") })
            FilterChip(selected = !Lang.hindi, onClick = { store.setHindi(false) }, label = { Text("English") })
        }
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = name, onValueChange = { if (it.length <= 40) name = it },
            label = { Text(tr("दुकान / आपका नाम", "Shop / your name")) },
            singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { store.finishOnboarding(name.trim()) },
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) { Text(tr("शुरू करें", "Get started")) }
        Spacer(Modifier.height(12.dp))
        Text(
            tr("आपका सारा डेटा सिर्फ़ इसी फ़ोन में रहता है। कोई लॉगिन नहीं, कोई सर्वर नहीं।",
                "All your data stays only on this phone. No login, no server."),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun LockScreen(store: Store, onUnlock: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var err by remember { mutableStateOf(false) }
    var reset by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().systemBarsPadding().imePadding().padding(24.dp),
        verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Lock, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(12.dp))
        Text(tr("PIN डालें", "Enter PIN"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = pin,
            onValueChange = { v ->
                if (v.length <= 4 && v.all { it.isDigit() }) {
                    pin = v; err = false
                    if (v.length == 4) {
                        if (store.checkPin(v)) onUnlock() else { err = true; pin = "" }
                    }
                }
            },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true, isError = err,
            supportingText = { if (err) Text(tr("गलत PIN", "Wrong PIN")) }
        )
        TextButton(onClick = { reset = true }) { Text(tr("PIN भूल गए?", "Forgot PIN?")) }
    }
    if (reset) {
        AlertDialog(
            onDismissRequest = { reset = false },
            title = { Text(tr("रीसेट करें?", "Reset?")) },
            text = { Text(tr("PIN हटेगा और सारा डेटा मिट जाएगा। यह वापस नहीं आएगा।",
                "PIN will be removed and ALL data will be erased. This cannot be undone.")) },
            confirmButton = { TextButton(onClick = { store.resetEverything(); reset = false; onUnlock() }) {
                Text(tr("मिटाएँ", "Erase"), color = Cx.red) } },
            dismissButton = { TextButton(onClick = { reset = false }) { Text(tr("रद्द", "Cancel")) } }
        )
    }
}

// ================= मुख्य स्क्रीन (4 टैब) =================
@Composable
fun MainScreen(store: Store, tab: Int, onTab: (Int) -> Unit, nav: (Route) -> Unit) {
    Scaffold(
        bottomBar = {
            Column {
                PromoBanner()
                NavigationBar {
                    val items = listOf(
                        Triple(tr("होम", "Home"), Icons.Default.Home, 0),
                        Triple(tr("ग्राहक", "Customers"), Icons.Default.Person, 1),
                        Triple(tr("रिपोर्ट", "Reports"), Icons.Default.DateRange, 2),
                        Triple(tr("सेटिंग्स", "Settings"), Icons.Default.Settings, 3)
                    )
                    items.forEach { (label, icon, i) ->
                        NavigationBarItem(
                            selected = tab == i, onClick = { onTab(i) },
                            icon = { Icon(icon, null) }, label = { Text(label) }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (tab <= 1) {
                ExtendedFloatingActionButton(
                    text = { Text(tr("ग्राहक जोड़ें", "Add customer")) },
                    icon = { Icon(Icons.Default.Add, null) },
                    onClick = { nav(Route.EditCust(null)) }
                )
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (tab) {
                0 -> HomeTab(store, nav)
                1 -> CustomersTab(store, nav)
                2 -> ReportsTab(store)
                else -> SettingsTab(store, nav)
            }
        }
    }
}

@Composable
fun SectionTitle(t: String) {
    Text(t, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
}

@Composable
fun balColor(b: Double): Color = when {
    b > 0.005 -> Cx.red
    b < -0.005 -> Cx.green
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

fun balLabel(b: Double): String = when {
    b > 0.005 -> tr("आपको मिलना है", "You'll get")
    b < -0.005 -> tr("आपको देना है", "You'll give")
    else -> tr("हिसाब बराबर", "Settled")
}

@Composable
fun EmptyState(text: String) {
    Text(text, Modifier.fillMaxWidth().padding(vertical = 40.dp), textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
fun CustomerRow(c: Customer, b: Double, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(c.name)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(c.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (c.phone.isNotBlank()) Text(c.phone, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(money(b), color = balColor(b), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(balLabel(b), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun StatCard(modifier: Modifier, title: String, value: String, color: Color) {
    Card(modifier, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = color)
        }
    }
}

// ---------- टैब 1: होम ----------
@Composable
fun HomeTab(store: Store, nav: (Route) -> Unit) {
    val bal = store.balanceMap
    val total = store.customers.sumOf { maxOf(bal[it.id] ?: 0.0, 0.0) }
    val pending = store.customers.count { (bal[it.id] ?: 0.0) > 0.005 }
    val end = endOfToday()
    val dueList = store.customers.filter { (bal[it.id] ?: 0.0) > 0.005 && (it.dueDate ?: Long.MAX_VALUE) <= end }
    val recent = store.entries.sortedByDescending { it.date }.take(6)

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF0F766E), Color(0xFF115E59))))
                    .padding(20.dp)
            ) {
                Column {
                    Text(
                        tr("नमस्ते", "Hello") + if (store.shopName.isNotBlank()) ", ${store.shopName}" else "",
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(tr("कुल बकाया (आपको मिलना है)", "Total due to you"), color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.labelLarge)
                    Text(money(total), color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                    Text(tr("$pending ग्राहकों का बकाया", "$pending customers pending"), color = Cx.gold)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(Modifier.weight(1f), tr("कुल ग्राहक", "Customers"), "${store.customers.size}",
                    MaterialTheme.colorScheme.primary)
                StatCard(Modifier.weight(1f), tr("कुल एंट्री", "Entries"), "${store.entries.size}",
                    MaterialTheme.colorScheme.primary)
            }
        }
        if (dueList.isNotEmpty()) {
            item { SectionTitle(tr("⏰ आज / बीती वापसी तारीख", "⏰ Due today / overdue")) }
            items(dueList, key = { "d" + it.id }) { c ->
                CustomerRow(c, bal[c.id] ?: 0.0) { nav(Route.Cust(c.id)) }
            }
        }
        item { SectionTitle(tr("हाल की एंट्री", "Recent entries")) }
        if (recent.isEmpty()) {
            item { EmptyState(tr("अभी कोई एंट्री नहीं।\nनीचे \"ग्राहक जोड़ें\" दबाएँ।", "No entries yet.\nTap \"Add customer\" below.")) }
        }
        items(recent, key = { "r" + it.id }) { e ->
            val c = store.customers.find { it.id == e.customerId }
            if (c != null) {
                Card(onClick = { nav(Route.Cust(c.id)) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(c.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(dateStr(e.date) + if (e.note.isNotBlank()) " • ${e.note}" else "",
                                style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            (if (e.given) "−" else "+") + money(e.amount),
                            color = if (e.given) Cx.red else Cx.green, fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// ---------- टैब 2: ग्राहक ----------
@Composable
fun CustomersTab(store: Store, nav: (Route) -> Unit) {
    var q by rememberSaveable { mutableStateOf("") }
    val bal = store.balanceMap
    val list = store.customers
        .filter { it.name.contains(q, true) || it.phone.contains(q) }
        .sortedWith(compareByDescending<Customer> { bal[it.id] ?: 0.0 }.thenBy { it.name.lowercase() })

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            OutlinedTextField(
                value = q, onValueChange = { q = it }, singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, null) },
                placeholder = { Text(tr("नाम या नंबर खोजें", "Search name or number")) },
                shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()
            )
        }
        if (list.isEmpty()) {
            item { EmptyState(tr("कोई ग्राहक नहीं मिला।", "No customers found.")) }
        }
        items(list, key = { it.id }) { c ->
            CustomerRow(c, bal[c.id] ?: 0.0) { nav(Route.Cust(c.id)) }
        }
    }
}

// ---------- टैब 3: रिपोर्ट ----------
@Composable
fun ReportsTab(store: Store) {
    val ctx = LocalContext.current
    var off by rememberSaveable { mutableIntStateOf(0) }
    val cal = Calendar.getInstance().apply {
        add(Calendar.MONTH, off); set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    val start = cal.timeInMillis
    val label = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
    cal.add(Calendar.MONTH, 1)
    val end = cal.timeInMillis

    val inMonth = store.entries.filter { it.date in start until end }
    val given = inMonth.filter { it.given }.sumOf { it.amount }
    val got = inMonth.filter { !it.given }.sumOf { it.amount }
    val bal = store.balanceMap
    val top = store.customers.filter { (bal[it.id] ?: 0.0) > 0.005 }.sortedByDescending { bal[it.id] }.take(5)

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { off-- }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                Text(label, Modifier.weight(1f), textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                IconButton(onClick = { off++ }, enabled = off < 0) { Icon(Icons.AutoMirrored.Filled.ArrowForward, null) }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(Modifier.weight(1f), tr("उधार दिया", "Credit given"), money(given), Cx.red)
                StatCard(Modifier.weight(1f), tr("पैसे मिले", "Received"), money(got), Cx.green)
            }
        }
        item { SectionTitle(tr("सबसे ज़्यादा बकाया", "Highest dues")) }
        if (top.isEmpty()) item { EmptyState(tr("कोई बकाया नहीं 🎉", "No dues 🎉")) }
        items(top, key = { "t" + it.id }) { c ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Avatar(c.name, 36)
                Spacer(Modifier.width(10.dp))
                Text(c.name, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(money(bal[c.id] ?: 0.0), color = Cx.red, fontWeight = FontWeight.Bold)
            }
        }
        item {
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    val sb = StringBuilder()
                    sb.append("📒 ${store.shopName.ifBlank { "Aryan Khata" }} — $label\n\n")
                    sb.append("${tr("उधार दिया", "Credit given")}: ${money(given)}\n")
                    sb.append("${tr("पैसे मिले", "Received")}: ${money(got)}\n\n")
                    sb.append(tr("सबसे ज़्यादा बकाया:", "Highest dues:")).append("\n")
                    top.forEach { sb.append("• ${it.name}: ${money(bal[it.id] ?: 0.0)}\n") }
                    shareText(ctx, sb.toString())
                },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Icon(Icons.Default.Share, null); Spacer(Modifier.width(8.dp))
                Text(tr("रिपोर्ट शेयर करें", "Share report"))
            }
        }
    }
}

// ---------- टैब 4: सेटिंग्स ----------
@Composable
fun SettingItem(title: String, subtitle: String? = null, danger: Boolean = false, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 14.dp)) {
        Text(title, fontWeight = FontWeight.Medium, color = if (danger) Cx.red else MaterialTheme.colorScheme.onSurface)
        if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SettingsTab(store: Store, nav: (Route) -> Unit) {
    val ctx = LocalContext.current
    var pinDlg by remember { mutableStateOf(false) }
    var shopDlg by remember { mutableStateOf(false) }
    var restoreDlg by remember { mutableStateOf(false) }
    var wipeDlg by remember { mutableStateOf(false) }

    val backup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        if (uri != null) {
            try {
                ctx.contentResolver.openOutputStream(uri)?.use { it.write(store.toJson().toByteArray(Charsets.UTF_8)) }
                toast(ctx, tr("बैकअप सेव हो गया ✅", "Backup saved ✅"))
            } catch (e: Exception) { toast(ctx, tr("बैकअप नहीं बन सका", "Backup failed")) }
        }
    }
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            val ok = try {
                val t = ctx.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
                t != null && store.importJson(t)
            } catch (e: Exception) { false }
            toast(ctx, if (ok) tr("डेटा रिस्टोर हो गया ✅", "Data restored ✅") else tr("गलत फ़ाइल", "Invalid file"))
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 16.dp)) {
        Column(Modifier.padding(16.dp)) {
            SectionTitle(tr("भाषा", "Language"))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = Lang.hindi, onClick = { store.setHindi(true) }, label = { Text("हिन्दी") })
                FilterChip(selected = !Lang.hindi, onClick = { store.setHindi(false) }, label = { Text("English") })
            }
            Spacer(Modifier.height(10.dp))
            SectionTitle(tr("थीम", "Theme"))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = store.themeMode == 0, onClick = { store.setTheme(0) }, label = { Text(tr("ऑटो", "Auto")) })
                FilterChip(selected = store.themeMode == 1, onClick = { store.setTheme(1) }, label = { Text(tr("लाइट", "Light")) })
                FilterChip(selected = store.themeMode == 2, onClick = { store.setTheme(2) }, label = { Text(tr("डार्क", "Dark")) })
            }
        }
        HorizontalDivider()
        SettingItem(tr("दुकान का नाम", "Shop name"), store.shopName.ifBlank { tr("सेट नहीं", "Not set") }) { shopDlg = true }
        SettingItem(
            if (store.hasPin) tr("PIN बदलें", "Change PIN") else tr("PIN लॉक लगाएँ", "Set PIN lock"),
            tr("ऐप खोलते समय 4 अंक का PIN", "4-digit PIN when opening the app")
        ) { pinDlg = true }
        if (store.hasPin) SettingItem(tr("PIN हटाएँ", "Remove PIN")) { store.removePin() }
        HorizontalDivider()
        SettingItem(tr("बैकअप सेव करें", "Save backup"),
            tr("फ़ाइल फ़ोन में सेव होगी — WhatsApp/Drive पर भी रख सकते हैं", "Saved as a file — keep a copy on WhatsApp/Drive")) {
            backup.launch("aryan-khata-backup-" + SimpleDateFormat("yyyyMMdd", Locale.US).format(Date()) + ".json")
        }
        SettingItem(tr("बैकअप से रिस्टोर करें", "Restore from backup"),
            tr("नया फ़ोन? पुरानी बैकअप फ़ाइल चुनें", "New phone? Pick your old backup file")) { restoreDlg = true }
        SettingItem(tr("सारा डेटा मिटाएँ", "Delete all data"), danger = true) { wipeDlg = true }
        HorizontalDivider()
        SettingItem(tr("ऐप दोस्तों को भेजें", "Share this app")) {
            shareText(ctx, tr("Aryan Khata — दुकान का फ्री उधार-खाता ऐप 📒\nhttps://play.google.com/store/apps/details?id=com.aryandigital.khata",
                "Aryan Khata — free udhaar ledger app for shops 📒\nhttps://play.google.com/store/apps/details?id=com.aryandigital.khata"))
        }
        SettingItem(tr("प्राइवेसी पॉलिसी", "Privacy Policy")) { nav(Route.Privacy) }
        SettingItem(tr("हमारे बारे में / अपना ऐप बनवाएँ", "About / Get your own app")) { nav(Route.About) }
    }

    if (pinDlg) {
        var p by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { pinDlg = false },
            title = { Text(tr("4 अंक का नया PIN", "New 4-digit PIN")) },
            text = {
                OutlinedTextField(
                    value = p, onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) p = it },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), singleLine = true
                )
            },
            confirmButton = { TextButton(enabled = p.length == 4, onClick = { store.setPin(p); pinDlg = false }) { Text(tr("सेव", "Save")) } },
            dismissButton = { TextButton(onClick = { pinDlg = false }) { Text(tr("रद्द", "Cancel")) } }
        )
    }
    if (shopDlg) {
        var n by remember { mutableStateOf(store.shopName) }
        AlertDialog(
            onDismissRequest = { shopDlg = false },
            title = { Text(tr("दुकान का नाम", "Shop name")) },
            text = { OutlinedTextField(value = n, onValueChange = { if (it.length <= 40) n = it }, singleLine = true) },
            confirmButton = { TextButton(onClick = { store.setShop(n.trim()); shopDlg = false }) { Text(tr("सेव", "Save")) } },
            dismissButton = { TextButton(onClick = { shopDlg = false }) { Text(tr("रद्द", "Cancel")) } }
        )
    }
    if (restoreDlg) {
        AlertDialog(
            onDismissRequest = { restoreDlg = false },
            title = { Text(tr("रिस्टोर करें?", "Restore?")) },
            text = { Text(tr("अभी का सारा डेटा बदल जाएगा।", "This will replace all current data.")) },
            confirmButton = { TextButton(onClick = { restoreDlg = false; restore.launch(arrayOf("*/*")) }) { Text(tr("आगे बढ़ें", "Continue")) } },
            dismissButton = { TextButton(onClick = { restoreDlg = false }) { Text(tr("रद्द", "Cancel")) } }
        )
    }
    if (wipeDlg) {
        AlertDialog(
            onDismissRequest = { wipeDlg = false },
            title = { Text(tr("सारा डेटा मिटाएँ?", "Delete all data?")) },
            text = { Text(tr("सभी ग्राहक और एंट्री हमेशा के लिए मिट जाएँगी।", "All customers and entries will be erased permanently.")) },
            confirmButton = { TextButton(onClick = { store.clearAll(); wipeDlg = false }) { Text(tr("मिटाएँ", "Delete"), color = Cx.red) } },
            dismissButton = { TextButton(onClick = { wipeDlg = false }) { Text(tr("रद्द", "Cancel")) } }
        )
    }
}

// ================= ग्राहक का पेज =================
fun statementText(store: Store, c: Customer, list: List<Entry>, b: Double): String {
    val sb = StringBuilder()
    sb.append("📒 ${store.shopName.ifBlank { "Aryan Khata" }}\n${c.name}\n\n")
    list.sortedBy { it.date }.forEach {
        sb.append("${dateStr(it.date)}  ${if (it.given) "−" else "+"}${money(it.amount)}")
        if (it.note.isNotBlank()) sb.append("  (${it.note})")
        sb.append("\n")
    }
    sb.append("\n").append(if (b > 0.005) tr("कुल बाकी", "Total due") else tr("हिसाब", "Balance")).append(": ${money(b)}")
    return sb.toString()
}

@Composable
fun CustomerScreen(store: Store, id: String, nav: (Route) -> Unit, onBack: () -> Unit) {
    val c = store.customers.find { it.id == id }
    if (c == null) { LaunchedEffect(Unit) { onBack() }; return }
    val ctx = LocalContext.current
    val b = store.balanceMap[id] ?: 0.0
    val list = store.entries.filter { it.customerId == id }.sortedByDescending { it.date }
    var delCust by remember { mutableStateOf(false) }
    var delEntry by remember { mutableStateOf<Entry?>(null) }
    var showDate by remember { mutableStateOf(false) }

    val reminder = tr(
        "नमस्ते ${c.name} जी, ${store.shopName.ifBlank { "हमारी दुकान" }} में आपका ${money(b)} बाकी है। कृपया जल्द भुगतान करें। धन्यवाद 🙏",
        "Hello ${c.name}, you have ${money(b)} pending at ${store.shopName.ifBlank { "our shop" }}. Please pay soon. Thank you 🙏"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(c.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                actions = {
                    IconButton(onClick = { nav(Route.EditCust(id)) }) { Icon(Icons.Default.Edit, null) }
                    IconButton(onClick = { delCust = true }) { Icon(Icons.Default.Delete, null) }
                }
            )
        }
    ) { pad ->
        LazyColumn(
            Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(16.dp, 4.dp, 16.dp, 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.padding(18.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(balLabel(b), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(money(b), fontSize = 34.sp, fontWeight = FontWeight.Bold, color = balColor(b))
                        if (c.phone.isNotBlank()) Text(c.phone, style = MaterialTheme.typography.bodySmall)
                        if (c.dueDate != null) Text(
                            tr("वापसी की तारीख: ", "Due date: ") + dateStr(c.dueDate),
                            style = MaterialTheme.typography.bodySmall, color = Cx.red
                        )
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { nav(Route.Ent(id, true)) },
                        modifier = Modifier.weight(1f).height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Cx.red, contentColor = Color.White)
                    ) { Text(tr("उधार दिया", "Gave credit"), fontWeight = FontWeight.Bold) }
                    Button(
                        onClick = { nav(Route.Ent(id, false)) },
                        modifier = Modifier.weight(1f).height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Cx.green, contentColor = Color.White)
                    ) { Text(tr("पैसे मिले", "Received"), fontWeight = FontWeight.Bold) }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilledTonalButton(
                        onClick = { if (b > 0.005) openWhatsApp(ctx, c.phone, reminder) else toast(ctx, tr("कोई बकाया नहीं", "No dues")) },
                        modifier = Modifier.weight(1f)
                    ) { Text("WhatsApp ${tr("रिमाइंडर", "reminder")}", fontSize = 12.sp, maxLines = 1) }
                    FilledTonalButton(
                        onClick = { if (b > 0.005 && c.phone.isNotBlank()) sendSms(ctx, c.phone, reminder) else toast(ctx, tr("बकाया या नंबर नहीं है", "No dues or number")) },
                        modifier = Modifier.weight(1f)
                    ) { Text("SMS ${tr("रिमाइंडर", "reminder")}", fontSize = 12.sp, maxLines = 1) }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilledTonalButton(
                        onClick = { shareText(ctx, statementText(store, c, list, b)) },
                        modifier = Modifier.weight(1f)
                    ) { Icon(Icons.Default.Share, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text(tr("हिसाब शेयर", "Share"), fontSize = 12.sp, maxLines = 1) }
                    FilledTonalButton(onClick = { showDate = true }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.DateRange, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp))
                        Text(tr("वापसी तारीख", "Due date"), fontSize = 12.sp, maxLines = 1)
                    }
                }
            }
            item { SectionTitle(tr("हिस्ट्री", "History")) }
            if (list.isEmpty()) item { EmptyState(tr("अभी कोई एंट्री नहीं।", "No entries yet.")) }
            items(list, key = { it.id }) { e ->
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                    Row(Modifier.padding(start = 14.dp, top = 6.dp, bottom = 6.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                (if (e.given) tr("उधार दिया", "Gave credit") else tr("पैसे मिले", "Received")),
                                fontWeight = FontWeight.SemiBold, color = if (e.given) Cx.red else Cx.green
                            )
                            Text(dateStr(e.date) + if (e.note.isNotBlank()) " • ${e.note}" else "",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(money(e.amount), fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        IconButton(onClick = { delEntry = e }) { Icon(Icons.Default.Delete, null, Modifier.size(20.dp)) }
                    }
                }
            }
        }
    }

    if (showDate) {
        val st = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    st.selectedDateMillis?.let { store.setDue(id, pickerToLocal(it)) }; showDate = false
                }) { Text(tr("ठीक है", "OK")) }
            },
            dismissButton = { TextButton(onClick = { store.setDue(id, null); showDate = false }) { Text(tr("हटाएँ", "Clear")) } }
        ) { DatePicker(state = st) }
    }
    if (delCust) {
        AlertDialog(
            onDismissRequest = { delCust = false },
            title = { Text(tr("ग्राहक हटाएँ?", "Delete customer?")) },
            text = { Text(tr("इसकी सारी एंट्री भी मिट जाएँगी।", "All its entries will be deleted too.")) },
            confirmButton = { TextButton(onClick = { delCust = false; store.deleteCustomer(id) }) { Text(tr("हटाएँ", "Delete"), color = Cx.red) } },
            dismissButton = { TextButton(onClick = { delCust = false }) { Text(tr("रद्द", "Cancel")) } }
        )
    }
    delEntry?.let { e ->
        AlertDialog(
            onDismissRequest = { delEntry = null },
            title = { Text(tr("एंट्री हटाएँ?", "Delete entry?")) },
            text = { Text(money(e.amount)) },
            confirmButton = { TextButton(onClick = { store.deleteEntry(e.id); delEntry = null }) { Text(tr("हटाएँ", "Delete"), color = Cx.red) } },
            dismissButton = { TextButton(onClick = { delEntry = null }) { Text(tr("रद्द", "Cancel")) } }
        )
    }
}

// ================= एंट्री जोड़ना =================
@Composable
fun EntryScreen(store: Store, custId: String, given: Boolean, onBack: () -> Unit) {
    val c = store.customers.find { it.id == custId }
    if (c == null) { LaunchedEffect(Unit) { onBack() }; return }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val color = if (given) Cx.red else Cx.green
    val amt = amount.toDoubleOrNull() ?: 0.0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text((if (given) tr("उधार दिया", "Gave credit") else tr("पैसे मिले", "Received")) + " • " + c.name,
                    maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
            )
        }
    ) { pad ->
        Column(Modifier.padding(pad).padding(16.dp).imePadding(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            OutlinedTextField(
                value = amount,
                onValueChange = { if (Regex("^\\d{0,9}(\\.\\d{0,2})?$").matches(it)) amount = it },
                label = { Text(tr("रकम (₹)", "Amount (₹)")) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true, modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.headlineSmall
            )
            OutlinedTextField(
                value = note, onValueChange = { if (it.length <= 80) note = it },
                label = { Text(tr("नोट (वैकल्पिक)", "Note (optional)")) },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            Text(tr("तारीख: आज, ", "Date: today, ") + dateStr(System.currentTimeMillis()),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(
                onClick = { store.addEntry(custId, amt, given, note.trim()); onBack() },
                enabled = amt > 0,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color.White)
            ) { Text(tr("सेव करें", "Save"), fontWeight = FontWeight.Bold) }
        }
    }
}

// ================= ग्राहक जोड़ना / बदलना =================
@Composable
fun EditCustomerScreen(store: Store, id: String?, onBack: () -> Unit) {
    val existing = id?.let { i -> store.customers.find { it.id == i } }
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var phone by remember { mutableStateOf(existing?.phone ?: "") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existing == null) tr("नया ग्राहक", "New customer") else tr("ग्राहक बदलें", "Edit customer")) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
            )
        }
    ) { pad ->
        Column(Modifier.padding(pad).padding(16.dp).imePadding(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            OutlinedTextField(
                value = name, onValueChange = { if (it.length <= 40) name = it },
                label = { Text(tr("नाम", "Name")) }, singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = phone,
                onValueChange = { if (it.length <= 15 && it.all { c -> c.isDigit() || c == '+' }) phone = it },
                label = { Text(tr("फ़ोन नंबर (वैकल्पिक)", "Phone number (optional)")) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    if (existing == null) store.addCustomer(name.trim(), phone.trim())
                    else store.updateCustomer(existing.id, name.trim(), phone.trim())
                    onBack()
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(54.dp)
            ) { Text(tr("सेव करें", "Save"), fontWeight = FontWeight.Bold) }
        }
    }
}

// ================= हमारे बारे में =================
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tr("हमारे बारे में", "About")) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
            )
        }
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(Modifier.size(84.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                Text("AT", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            Text("Aryan Khata", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("v1.0.0", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(16.dp))
            Text(tr("Developer", "Developer"), style = MaterialTheme.typography.labelMedium)
            Text("Aryan Kumar Tiwari", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text("Founder, Aryan Digital Solutions", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text(tr("अपना ऐप बनवाना है?", "Want your own app?"), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(tr("वेबसाइट • मोबाइल ऐप • गेम • Play Store पर पब्लिश करना — सब कुछ हम करते हैं।",
                        "Websites • Mobile apps • Games • Play Store publishing — we do it all."))
                    Spacer(Modifier.height(6.dp))
                    Text(OWNER_PHONE_PRETTY, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { openWhatsApp(ctx, OWNER_PHONE, tr("नमस्ते, मुझे अपना ऐप बनवाना है।", "Hello, I want to get an app built.")) },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) { Text(tr("WhatsApp पर मैसेज करें", "Message on WhatsApp")) }
            Spacer(Modifier.height(8.dp))
            FilledTonalButton(onClick = { dial(ctx, OWNER_PHONE_PRETTY.replace(" ", "")) }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Icon(Icons.Default.Phone, null); Spacer(Modifier.width(8.dp)); Text(tr("कॉल करें", "Call"))
            }
            Spacer(Modifier.height(16.dp))
            Text("Made with ❤️ in India by Aryan Digital Solutions", style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ================= प्राइवेसी पॉलिसी =================
@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tr("प्राइवेसी पॉलिसी", "Privacy Policy")) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
            )
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(tr(
                "Aryan Khata आपका कोई डेटा इकट्ठा, स्टोर (सर्वर पर) या शेयर नहीं करता।\n\n" +
                "• ग्राहकों के नाम, फ़ोन नंबर और हिसाब सिर्फ़ आपके फ़ोन की स्टोरेज में रहते हैं।\n" +
                "• ऐप में कोई लॉगिन, अकाउंट, एनालिटिक्स या थर्ड-पार्टी ट्रैकर नहीं है।\n" +
                "• ऐप को कोई परमिशन (कॉन्टैक्ट, SMS, लोकेशन, कैमरा आदि) नहीं चाहिए।\n" +
                "• WhatsApp/SMS/शेयर बटन दबाने पर सिर्फ़ वही मैसेज आपके चुने हुए ऐप को जाता है जो आप भेजते हैं।\n" +
                "• बैकअप फ़ाइल आप खुद बनाते हैं और आप ही तय करते हैं कि वह कहाँ रखनी है।\n" +
                "• ऐप हटाने या \"सारा डेटा मिटाएँ\" दबाने पर आपका डेटा हमेशा के लिए मिट जाता है।\n" +
                "• नीचे का बैनर हमारी अपनी सर्विस का प्रमोशन है, इसमें कोई थर्ड-पार्टी विज्ञापन नहीं है।\n\n" +
                "संपर्क: Aryan Kumar Tiwari, Aryan Digital Solutions — $OWNER_PHONE_PRETTY",
                "Aryan Khata does not collect, upload or share any of your data.\n\n" +
                "• Customer names, phone numbers and ledger entries stay only in your phone's storage.\n" +
                "• No login, account, analytics or third-party trackers.\n" +
                "• The app needs no permissions (contacts, SMS, location, camera, etc.).\n" +
                "• WhatsApp/SMS/Share buttons only pass the message you choose to send to the app you choose.\n" +
                "• Backup files are created by you and stored where you choose.\n" +
                "• Uninstalling the app or tapping \"Delete all data\" permanently erases your data.\n" +
                "• The banner is a promotion of our own services; there are no third-party ads.\n\n" +
                "Contact: Aryan Kumar Tiwari, Aryan Digital Solutions — $OWNER_PHONE_PRETTY"
            ))
        }
    }
}
