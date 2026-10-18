package com.raininaugust26.prune.data

enum class PackageLevel { safe, optional, caution }
enum class PackageStatus { installed, disabled, uninstalled, unknown }
enum class ExecStatus { OK, FAIL, SKIP, DRY, INFO }
enum class Profile { Safe, Aggressive, Custom }
enum class DefaultAction { disable, uninstall }
enum class RebootPref { Ask, Auto, Never }

data class PackageEntry(
    val id: String, val name: String, val level: String,
    val category: String, val description: String, val impact: String,
    val actions: List<String> = listOf("disable","uninstall"), val verified: Boolean = false
)
data class Matchers(val manufacturer: List<String> = emptyList(), val brand: List<String> = emptyList())
data class GroupFile(val schemaVersion: Int = 1, val group: String, val matchers: Matchers = Matchers(), val packages: List<PackageEntry> = emptyList())
data class DenylistFile(val schemaVersion: Int = 1, val group: String = "denylist", val patterns: List<String> = emptyList())

data class DeviceInfo(val brand: String="", val manufacturer: String="", val model: String="", val android: String="", val sdk: String="", val rootProvider: String="Unknown")
data class ScannedPackage(val entry: PackageEntry, val status: PackageStatus)
data class ExecLine(val status: ExecStatus, val pkg: String, val msg: String)
data class ExecSummary(val total: Int, val ok: Int, val fail: Int, val skip: Int)
data class HistoryEntry(val ts: String, val action: String, val lines: List<ExecLine>, val summary: ExecSummary?)
