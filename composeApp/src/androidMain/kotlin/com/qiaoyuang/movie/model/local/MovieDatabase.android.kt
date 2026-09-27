package com.qiaoyuang.movie.model.local

import com.ctrip.sqllin.driver.DatabasePath
import com.ctrip.sqllin.driver.toDatabasePath
import com.qiaoyuang.movie.model.appContext

/**
 * sqllin's Android driver is SQLiteOpenHelper-backed, so the path it wants is a Context — the
 * helper then puts the file under the app's databases/ directory. appContext is set by
 * AppContextInitializer, which androidx.startup runs before anything can resolve this.
 */
internal actual val databasePath: DatabasePath
    get() = appContext.toDatabasePath()
