package com.raininaugust26.prune.root

import android.content.Context
import com.raininaugust26.prune.data.PackageStatus
import com.topjohnwu.superuser.Shell
import java.io.File

object RootExecutor {
    fun workDir(ctx: Context): File = File(ctx.filesDir, "prune").also { it.mkdirs() }

    fun ensureScript(ctx: Context): File {
        val dir = workDir(ctx)
        val sh = File(dir, "debloat.sh")
        val dl = File(dir, "denylist.txt")
        if (!sh.exists() || sh.length() == 0L) {
            ctx.assets.open("debloat.sh").use { i -> sh.outputStream().use { o -> i.copyTo(o) } }
        }
        try {
            val txt = ctx.assets.open("packages/denylist.json").bufferedReader().readText()
            val pats = Regex("\"([^\"]+)\"").findAll(txt).map { it.groupValues[1] }.filter { it.contains(".") || it.endsWith("*") }.toList()
            dl.writeText(pats.joinToString("\n"))
        } catch (_: Exception) {}
        return sh
    }

    fun isRooted(): Boolean = runCatching {
        val r = Shell.cmd("id -u").exec()
        r.out.firstOrNull()?.trim() == "0"
    }.getOrDefault(false)

    fun provider(): String = runCatching {
        val out = Shell.cmd("which magisk; ls /data/adb/ksu 2>/dev/null; ls /data/adb/ap 2>/dev/null; su -v 2>/dev/null").exec().out.joinToString(" ")
        when {
            out.contains("magisk") -> "Magisk"
            out.contains("/data/adb/ksu") || out.contains("KernelSU") -> "KernelSU"
            out.contains("/data/adb/ap") || out.contains("APatch") -> "APatch"
            else -> "Root (su)"
        }
    }.getOrDefault("Unknown")

    fun deviceInfo(ctx: Context): Map<String,String> {
        val sh = ensureScript(ctx)
        val wd = workDir(ctx)
        val out = Shell.cmd("sh ${sh.absolutePath} info --workdir ${wd.absolutePath}").exec().out
        val m = mutableMapOf<String,String>()
        for (l in out) {
            val p = l.split("|", limit = 3); if (p.size==3 && p[0]=="INFO") m[p[1]] = p[2]
        }
        return m
    }

    fun scan(ctx: Context): Map<String, PackageStatus> {
        val sh = ensureScript(ctx)
        val wd = workDir(ctx)
        val out = Shell.cmd("sh ${sh.absolutePath} scan --workdir ${wd.absolutePath}").exec().out
        val map = mutableMapOf<String, PackageStatus>()
        for (l in out) {
            val p = l.split("|", limit = 3); if (p.size<3 || p[0]!="INFO") continue
            if (p[1]=="DONE" || p[1]=="brand" || p[1]=="manufacturer" || p[1]=="model" || p[1]=="android" || p[1]=="sdk" || p[1]=="backup") continue
            map[p[1]] = ShellContract.mapStatus(p[2])
        }
        return map
    }

    fun run(ctx: Context, action: String, pkgs: List<String>, dryRun: Boolean, wipe: Boolean, onLine: (String)->Unit): Int {
        val sh = ensureScript(ctx)
        val wd = workDir(ctx)
        val listFile = File(wd, "selection.txt")
        listFile.writeText(pkgs.joinToString("\n"))
        val dry = if (dryRun) " --dry-run" else ""
        val w = if (wipe && action=="uninstall") " --wipe" else ""
        val cmd = "sh ${sh.absolutePath} $action --list ${listFile.absolutePath} --workdir ${wd.absolutePath}$dry$w"
        val res = Shell.cmd(cmd).to(object : com.topjohnwu.superuser.CallbackList<String>() {
            override fun onAddElement(s: String) { onLine(s) }
        }).exec()
        res.out.forEach { /* already streamed */ }
        return res.code
    }
}
