package com.qiaoyuang.movie.model.local

import com.ctrip.sqllin.driver.DatabasePath
import com.ctrip.sqllin.dsl.DSLDBConfiguration
import com.ctrip.sqllin.dsl.Database
import com.ctrip.sqllin.dsl.DatabaseScope
import com.ctrip.sqllin.dsl.annotation.ExperimentalDSLDatabaseAPI

/**
 * Where the database file lives. Android needs a Context and iOS needs a directory string, so
 * this is the one platform-specific piece — the schema and every query stay in common code.
 */
internal expect val databasePath: DatabasePath

internal const val DATABASE_NAME = "movie_cache.db"

/**
 * 2 is SQLlin 2.4.0's DDL: single-column keys moved from @CompositePrimaryKey to @PrimaryKey,
 * which makes movies.id a rowid alias instead of a BIGINT column with its own index, and
 * composite-key columns are now declared NOT NULL. SQLlin never migrates an existing table, and
 * the create block only runs on creation, so without a version bump an installed app would keep
 * the old schema forever.
 */
private const val DATABASE_VERSION = 2

/**
 * Creates the cache database. DSLDBConfiguration lets `create` be written with the same DSL as
 * the queries — CREATE(Table) derives the DDL from the entity, so the schema cannot drift from
 * the data classes.
 *
 * The connection is opened once and kept for the process lifetime (a Koin `single`): a sqllin
 * Database wraps a single connection and serialises statement execution behind its own mutex,
 * so re-opening per query would only add cost.
 */
@OptIn(ExperimentalDSLDatabaseAPI::class)
internal fun createMovieDatabase(path: DatabasePath = databasePath): Database = Database(
    DSLDBConfiguration(
        name = DATABASE_NAME,
        path = path,
        version = DATABASE_VERSION,
        create = { createAllTables() },
        // Every row here is re-fetchable, so a schema change starts over rather than migrating:
        // the cost is one refetch against a table rebuild that would carefully preserve rows we
        // are happy to lose. Nothing user-authored is stored in this database.
        upgrade = { _, _ ->
            dropAllTables()
            createAllTables()
        },
    ),
)

// One list, used by both create and upgrade, so the two cannot drift.
@OptIn(ExperimentalDSLDatabaseAPI::class)
private fun DatabaseScope.createAllTables() {
    MovieEntityTable.CREATE()
    GenreEntityTable.CREATE()
    MovieGenreEntityTable.CREATE()
    MovieListEntryEntityTable.CREATE()
    ListRemoteKeyEntityTable.CREATE()
}

@OptIn(ExperimentalDSLDatabaseAPI::class)
private fun DatabaseScope.dropAllTables() {
    MovieEntityTable.DROP()
    GenreEntityTable.DROP()
    MovieGenreEntityTable.DROP()
    MovieListEntryEntityTable.DROP()
    ListRemoteKeyEntityTable.DROP()
}
