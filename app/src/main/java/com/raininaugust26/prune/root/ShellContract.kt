package com.raininaugust26.prune.root

import com.raininaugust26.prune.data.ExecLine
import com.raininaugust26.prune.data.ExecStatus
import com.raininaugust26.prune.data.ExecSummary
import com.raininaugust26.prune.data.PackageStatus

object ShellContract {
    fun parseLine(raw: String): ExecLine? {
        val p = raw.split("|", limit = 3)
        if (p.size < 3) return null
        val st = runCatching { ExecStatus.valueOf(p[0].trim()) }.getOrNull() ?: return null
        return ExecLine(st, p[1].trim(), p[2].trim())
    }
    fun parseSummary(lines: List<ExecLine>): ExecSummary? {
        val d = lines.find { it.pkg == "DONE" || it.status == ExecStatus.INFO && it.pkg.startsWith("DONE") } ?: return null
        return null
    }
    fun parseDone(raw: String): ExecSummary? {
        val c = raw.trim().split("|")
        if (c.size < 5 || c[0] != "DONE") return null
        return ExecSummary(c[1].toIntOrNull()?:0, c[2].toIntOrNull()?:0, c[3].toIntOrNull()?:0, c[4].toIntOrNull()?:0, c.getOrNull(5)?.trim()?.toIntOrNull() ?: 0)
    }
    fun mapStatus(s: String): PackageStatus = when(s.lowercase().trim()) {
        "installed" -> PackageStatus.installed
        "disabled" -> PackageStatus.disabled
        "uninstalled" -> PackageStatus.uninstalled
        else -> PackageStatus.unknown
    }
}
