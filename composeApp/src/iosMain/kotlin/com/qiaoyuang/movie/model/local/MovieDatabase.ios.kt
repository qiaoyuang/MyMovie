package com.qiaoyuang.movie.model.local

import com.ctrip.sqllin.driver.DatabasePath
import com.ctrip.sqllin.driver.toDatabasePath
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

/**
 * Caches, not Documents: this is a rebuildable cache, so it should not be backed up to iCloud,
 * and the OS is welcome to purge it under disk pressure. Losing the file costs one re-fetch —
 * the entries and the paging cursor go together, so there is no half-purged state to recover
 * from. (Android has no say here: sqllin's driver always uses the app's databases/ directory.)
 */
internal actual val databasePath: DatabasePath by lazy {
    val caches = NSSearchPathForDirectoriesInDomains(
        directory = NSCachesDirectory,
        domainMask = NSUserDomainMask,
        expandTilde = true,
    ).first() as String
    caches.toDatabasePath()
}
