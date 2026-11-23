package com.aryandigital.khata

import android.content.Context
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.util.UUID

const val OWNER_PHONE = "919128692966"
const val OWNER_PHONE_PRETTY = "+91 9128692966"

object Lang { var hindi by mutableStateOf(true) }

fun tr(hi: String, en: String): String = if (Lang.hindi) hi else en

data class Customer(val id: String, val name: String, val phone: String, val dueDate: Long?)
data class Entry(
    val id: String,
    val customerId: String,
    val amount: Double,
    val given: Boolean,   // true = उधार दिया, false = पैसे मिले
    val note: String,
    val date: Long
)

class Store(private val ctx: Context) {
    val customers = mutableStateListOf<Customer>()
    val entries = mutableStateListOf<Entry>()

    private val prefs = ctx.getSharedPreferences("khata_prefs", Context.MODE_PRIVATE)
    private val file = File(ctx.filesDir, "khata.json")

    var shopName by mutableStateOf(prefs.getString("shop", "") ?: "")
        private set
    var onboarded by mutableStateOf(prefs.getBoolean("onboarded", false))
        private set
    var themeMode by mutableStateOf(prefs.getInt("theme", 0)) // 0 auto, 1 light, 2 dark
        private set
    var pinHash by mutableStateOf(prefs.getString("pin", null))
        private set
    val hasPin: Boolean get() = pinHash != null

    val balanceMap: Map<String, Double> by derivedStateOf {
        val m = HashMap<String, Double>()
        for (e in entries) {
            m[e.customerId] = (m[e.customerId] ?: 0.0) + if (e.given) e.amount else -e.amount
        }
        m
    }

    init {
        Lang.hindi = prefs.getBoolean("hindi", true)
        try {
            if (file.exists()) {
                val (c, e) = parse(file.readText())
                customers.addAll(c); entries.addAll(e)
            }
        } catch (_: Exception) { }
    }

    // ---------- settings ----------
    fun setHindi(v: Boolean) { Lang.hindi = v; prefs.edit().putBoolean("hindi", v).apply() }
    fun setTheme(m: Int) { themeMode = m; prefs.edit().putInt("theme", m).apply() }
    fun setShop(n: String) { shopName = n; prefs.edit().putString("shop", n).apply() }
    fun finishOnboarding(n: String) {
        setShop(n); onboarded = true; prefs.edit().putBoolean("onboarded", true).apply()
    }
    private fun hash(p: String): String =
        MessageDigest.getInstance("SHA-256").digest(("ak:$p").toByteArray())
            .joinToString("") { "%02x".format(it) }
    fun setPin(p: String) { val h = hash(p); pinHash = h; prefs.edit().putString("pin", h).apply() }
    fun removePin() { pinHash = null; prefs.edit().remove("pin").apply() }
    fun checkPin(p: String): Boolean = hash(p) == pinHash

    // ---------- data ----------
    fun addCustomer(name: String, phone: String) {
        customers.add(Customer(UUID.randomUUID().toString(), name, phone, null)); save()
    }
    fun updateCustomer(id: String, name: String, phone: String) {
        val i = customers.indexOfFirst { it.id == id }
        if (i >= 0) { customers[i] = customers[i].copy(name = name, phone = phone); save() }
    }
    fun setDue(id: String, due: Long?) {
        val i = customers.indexOfFirst { it.id == id }
        if (i >= 0) { customers[i] = customers[i].copy(dueDate = due); save() }
    }
    fun deleteCustomer(id: String) {
        customers.removeAll { it.id == id }
        entries.removeAll { it.customerId == id }
        save()
    }
    fun addEntry(custId: String, amount: Double, given: Boolean, note: String) {
        entries.add(Entry(UUID.randomUUID().toString(), custId, amount, given, note, System.currentTimeMillis()))
        save()
    }
    fun deleteEntry(id: String) { entries.removeAll { it.id == id }; save() }
    fun clearAll() { customers.clear(); entries.clear(); save() }
    fun resetEverything() { clearAll(); removePin() }

    // ---------- storage ----------
    fun toJson(): String {
        val o = JSONObject()
        o.put("app", "aryan-khata"); o.put("v", 1)
        val cs = JSONArray()
        customers.forEach {
            cs.put(JSONObject().put("id", it.id).put("name", it.name).put("phone", it.phone)
                .put("due", it.dueDate ?: JSONObject.NULL))
        }
        val es = JSONArray()
        entries.forEach {
            es.put(JSONObject().put("id", it.id).put("cid", it.customerId).put("amt", it.amount)
                .put("given", it.given).put("note", it.note).put("date", it.date))
        }
        o.put("customers", cs); o.put("entries", es)
        return o.toString()
    }

    private fun parse(text: String): Pair<List<Customer>, List<Entry>> {
        val o = JSONObject(text)
        val cs = o.getJSONArray("customers")
        val es = o.getJSONArray("entries")
        val cl = ArrayList<Customer>()
        val el = ArrayList<Entry>()
        for (i in 0 until cs.length()) {
            val c = cs.getJSONObject(i)
            cl.add(Customer(
                c.getString("id"), c.getString("name"), c.optString("phone", ""),
                if (c.has("due") && !c.isNull("due")) c.getLong("due") else null
            ))
        }
        for (i in 0 until es.length()) {
            val e = es.getJSONObject(i)
            el.add(Entry(
                e.getString("id"), e.getString("cid"), e.getDouble("amt"),
                e.getBoolean("given"), e.optString("note", ""), e.getLong("date")
            ))
        }
        return cl to el
    }

    fun importJson(text: String): Boolean = try {
        val (c, e) = parse(text)
        customers.clear(); entries.clear()
        customers.addAll(c); entries.addAll(e)
        save(); true
    } catch (_: Exception) { false }

    private fun save() {
        try {
            val tmp = File(ctx.filesDir, "khata.tmp")
            tmp.writeText(toJson())
            if (!tmp.renameTo(file)) { file.writeText(tmp.readText()); tmp.delete() }
        } catch (_: Exception) { }
    }
}
