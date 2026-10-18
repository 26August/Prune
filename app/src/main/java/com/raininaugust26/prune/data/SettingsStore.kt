package com.raininaugust26.prune.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.ds by preferencesDataStore("prune")

class SettingsStore(private val ctx: Context) {
    private val K_ACTION = stringPreferencesKey("default_action")
    private val K_DRY = booleanPreferencesKey("dry_run")
    private val K_WIPE = booleanPreferencesKey("wipe")
    private val K_SHOW_CAUTION = booleanPreferencesKey("show_caution")
    private val K_REBOOT = stringPreferencesKey("reboot")
    private val K_THEME = stringPreferencesKey("theme")
    private val K_BRAND_OVERRIDE = stringPreferencesKey("brand_override")

    val flow = ctx.ds.data.map { p ->
        SettingsState(
            defaultAction = if (p[K_ACTION] == "uninstall") DefaultAction.uninstall else DefaultAction.disable,
            dryRun = p[K_DRY] ?: true,
            wipe = p[K_WIPE] ?: false,
            showCaution = p[K_SHOW_CAUTION] ?: false,
            reboot = when (p[K_REBOOT]) { "auto" -> RebootPref.Auto; "never" -> RebootPref.Never; else -> RebootPref.Ask },
            theme = p[K_THEME] ?: "system",
            brandOverride = p[K_BRAND_OVERRIDE]
        )
    }
    suspend fun setAction(v: DefaultAction) { ctx.ds.edit { it[K_ACTION] = v.name } }
    suspend fun setDry(v: Boolean) { ctx.ds.edit { it[K_DRY] = v } }
    suspend fun setWipe(v: Boolean) { ctx.ds.edit { it[K_WIPE] = v } }
    suspend fun setShowCaution(v: Boolean) { ctx.ds.edit { it[K_SHOW_CAUTION] = v } }
    suspend fun setReboot(v: RebootPref) { ctx.ds.edit { it[K_REBOOT] = v.name.lowercase() } }
    suspend fun setTheme(v: String) { ctx.ds.edit { it[K_THEME] = v } }
    suspend fun setBrandOverride(v: String?) {
        ctx.ds.edit { if (v.isNullOrBlank()) it.remove(K_BRAND_OVERRIDE) else it[K_BRAND_OVERRIDE] = v }
    }
}
data class SettingsState(
    val defaultAction: DefaultAction = DefaultAction.disable,
    val dryRun: Boolean = true, val wipe: Boolean = false,
    val showCaution: Boolean = false, val reboot: RebootPref = RebootPref.Ask,
    val theme: String = "system", val brandOverride: String? = null
)
