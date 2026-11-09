package com.qiaoyuang.movie.model.local

import com.ctrip.sqllin.driver.DatabasePath
import com.ctrip.sqllin.dsl.DSLDBConfiguration
import com.ctrip.sqllin.dsl.Database
import com.ctrip.sqllin.dsl.DatabaseScope
import com.ctrip.sqllin.dsl.annotation.ExperimentalDSLDatabaseAPI
import com.ctrip.sqllin.dsl.sql.withName

/**
 * Where the database file lives. Android needs a Context and iOS needs a directory string, so
 * this is the one platform-specific piece — the schema and every query stay in common code.
 */
internal expect val databasePath: DatabasePath

internal const val DATABASE_NAME = "movie_cache.db"

/**
 * 3 moves each movie's genre ids into a column of `movies` and drops the `movie_genres` junction
 * table, which lost their order. (2 was SQLlin 2.4.0's DDL for the keys.) SQLlin never migrates
 * an existing table, and the create block only runs on creation, so without a version bump an
 * installed app would keep the old schema forever.
 */
private const val DATABASE_VERSION = 3

/** Version 2's genre junction table, whose class is gone; dropped by name on upgrade. */
private const val LEGACY_MOVIE_GENRES = "movie_genres"

/**
 * Creates the cache database. DSLDBConfiguration lets `create` be written with the same DSL as
 * the queries — CREATE(Table) derives the DDL from the entity, so the schema cannot drift from
 * the data classes.
 *
 * The connection is opened once and kept for the process lifetime (a Koin `single`): a sqllin
 * Database wraps a single connection and serialises statement execution behind its own mutex,
 * so re-opening per query would only add cost.
 */
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
private fun DatabaseScope.createAllTables() {
    MovieEntityTable.CREATE()
    GenreEntityTable.CREATE()
    MovieListEntryEntityTable.CREATE()
    ListRemoteKeyEntityTable.CREATE()
}

/**
 * IF EXISTS, so the step is repeatable: a plain DROP of a table a given installation never had
 * would fail the upgrade. That becomes reachable the moment a later version adds a table — a
 * database still on version 1 would then be asked to drop it on its way to version 3. Worth the
 * one opt-in that the IF EXISTS form still carries in 2.5.0.
 */
@OptIn(ExperimentalDSLDatabaseAPI::class)
private fun DatabaseScope.dropAllTables() {
    DROP_IF_EXISTS(MovieEntityTable)
    DROP_IF_EXISTS(GenreEntityTable)
    // DROP only uses the table's name, so any table's shape will do for one that has no class.
    DROP_IF_EXISTS(GenreEntityTable.withName(LEGACY_MOVIE_GENRES))
    DROP_IF_EXISTS(MovieListEntryEntityTable)
    DROP_IF_EXISTS(ListRemoteKeyEntityTable)
}
