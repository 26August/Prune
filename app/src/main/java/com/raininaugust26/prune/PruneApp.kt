package com.raininaugust26.prune

import android.app.Application
import com.topjohnwu.superuser.Shell

class PruneApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Shell.enableVerboseLogging = false
        Shell.setDefaultBuilder(Shell.Builder.create().setFlags(Shell.FLAG_MOUNT_MASTER).setTimeout(10))
    }
}
