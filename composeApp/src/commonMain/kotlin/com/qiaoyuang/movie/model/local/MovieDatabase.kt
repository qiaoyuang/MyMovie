package com.qiaoyuang.movie.model.local

import com.ctrip.sqllin.driver.DatabasePath
import com.ctrip.sqllin.dsl.DSLDBConfiguration
import com.ctrip.sqllin.dsl.Database
import com.ctrip.sqllin.dsl.annotation.ExperimentalDSLDatabaseAPI

/**
 * Where the database file lives. Android needs a Context and iOS needs a directory string, so
 * this is the one platform-specific piece — the schema and every query stay in common code.
 */
internal expect val databasePath: DatabasePath

internal const val DATABASE_NAME = "movie_cache.db"

private const val DATABASE_VERSION = 1

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
        create = {
            CREATE(MovieEntityTable)
            CREATE(GenreEntityTable)
            CREATE(MovieGenreEntityTable)
            CREATE(MovieListEntryEntityTable)
            CREATE(ListRemoteKeyEntityTable)
        },
    ),
)
