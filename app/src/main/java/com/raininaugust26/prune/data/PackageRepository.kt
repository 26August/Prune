package com.raininaugust26.prune.data

import android.content.Context
import org.json.JSONObject

class PackageRepository(private val ctx: Context) {
    var denylist: List<String> = emptyList()
        private set
    private var groups: List<GroupFile> = emptyList()

    fun load() {
        val am = ctx.assets
        denylist = try {
            val j = JSONObject(am.open("packages/denylist.json").bufferedReader().readText())
            val arr = j.optJSONArray("patterns")
            (0 until (arr?.length() ?: 0)).map { arr!!.getString(it) }
        } catch (_: Exception) { emptyList() }
        val files = try { am.list("packages")?.toList() ?: emptyList() } catch (_: Exception) { emptyList() }
        groups = files.filter { it.endsWith(".json") && it != "denylist.json" }.mapNotNull { name ->
            try {
                val txt = am.open("packages/$name").bufferedReader().readText()
                val j = JSONObject(txt)
                val g = j.optString("group", name.removeSuffix(".json"))
                val mObj = j.optJSONObject("matchers")
                val man = mObj?.optJSONArray("manufacturer")?.let { a -> (0 until a.length()).map { a.getString(it) } } ?: emptyList()
                val br = mObj?.optJSONArray("brand")?.let { a -> (0 until a.length()).map { a.getString(it) } } ?: emptyList()
                val pkgs = j.optJSONArray("packages")?.let { arr ->
                    (0 until arr.length()).map { idx ->
                        val o = arr.getJSONObject(idx)
                        PackageEntry(
                            id = o.getString("id"), name = o.optString("name", o.getString("id")),
                            level = o.optString("level", "optional"), category = o.optString("category", "other"),
                            description = o.optString("description", ""), impact = o.optString("impact", ""),
                            actions = o.optJSONArray("actions")?.let { a -> (0 until a.length()).map { a.getString(it) } } ?: listOf("disable","uninstall"),
                            verified = o.optBoolean("verified", false)
                        )
                    }
                } ?: emptyList()
                GroupFile(group = g, matchers = Matchers(man, br), packages = pkgs)
            } catch (_: Exception) { null }
        }
    }

    fun matchedFor(info: DeviceInfo, brandOverride: String? = null): List<PackageEntry> {
        val brand = (brandOverride ?: info.brand).lowercase()
        val man = info.manufacturer.lowercase()
        val result = mutableListOf<PackageEntry>()
        for (g in groups) {
            if (g.group == "common") { result += g.packages; continue }
            val manMatch = g.matchers.manufacturer.any { it.lowercase() == man }
            val brandMatch = g.matchers.brand.any { it.lowercase() == brand }
            if (manMatch || brandMatch) result += g.packages
        }
        if (result.isEmpty()) result += groups.find { it.group == "common" }?.packages ?: emptyList()
        return result.distinctBy { it.id }
    }

    fun isDenylisted(pkg: String): Boolean {
        for (pat in denylist) {
            if (pat.endsWith("*")) { if (pkg.startsWith(pat.dropLast(1))) return true }
            else if (pkg == pat) return true
        }
        return false
    }
}
