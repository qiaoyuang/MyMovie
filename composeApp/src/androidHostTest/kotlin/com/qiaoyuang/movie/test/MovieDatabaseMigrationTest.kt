package com.qiaoyuang.movie.test

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.ctrip.sqllin.driver.deleteDatabase
import com.ctrip.sqllin.driver.toDatabasePath
import com.ctrip.sqllin.dsl.DSLDBConfiguration
import com.ctrip.sqllin.dsl.Database
import com.ctrip.sqllin.dsl.annotation.ExperimentalDSLDatabaseAPI
import com.ctrip.sqllin.dsl.sql.pragma.ColumnInfo
import com.ctrip.sqllin.dsl.sql.statement.PragmaStatement
import com.ctrip.sqllin.dsl.sql.withName
import com.qiaoyuang.movie.model.domain.Movie
import com.qiaoyuang.movie.model.local.DATABASE_NAME
import com.qiaoyuang.movie.model.local.GenreEntityTable
import com.qiaoyuang.movie.model.local.ListCursor
import com.qiaoyuang.movie.model.local.ListRemoteKeyEntityTable
import com.qiaoyuang.movie.model.local.MovieListEntryEntityTable
import com.qiaoyuang.movie.model.local.MovieLocalDataSourceImpl
import com.qiaoyuang.movie.model.local.TOP_RATED_LIST_KEY
import com.qiaoyuang.movie.model.local.createMovieDatabase
import com.qiaoyuang.movie.model.local.toEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The upgrade from version 2, on a real database: the one part of the move to a genre column that
 * nothing else exercises, since every other test starts from a fresh version 3 file.
 *
 * Version 2's classes are gone, so its schema is rebuilt here from stand-ins with the right names:
 * a `movie_genres` table, and a `movies` table without the genre column. What is checked is what
 * the upgrade promises, not a byte-for-byte copy of the old DDL.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26, 37])
@OptIn(ExperimentalDSLDatabaseAPI::class)
class MovieDatabaseMigrationTest {

    private val path = ApplicationProvider.getApplicationContext<Context>().toDatabasePath()

    @Before
    fun clean() {
        deleteDatabase(path, DATABASE_NAME)
    }

    @After
    fun cleanUp() {
        deleteDatabase(path, DATABASE_NAME)
    }

    /** A version 2 file, holding a cached list, the way an installed app has it before upgrading. */
    private suspend fun createVersion2Database() {
        val v2 = Database(
            DSLDBConfiguration(
                name = DATABASE_NAME,
                path = path,
                version = 2,
                create = {
                    // Stand-ins named like version 2's tables. Neither has a genreIds column.
                    CREATE(GenreEntityTable.withName("movies"))
                    CREATE(GenreEntityTable.withName("movie_genres"))
                    CREATE(GenreEntityTable)
                    CREATE(MovieListEntryEntityTable)
                    CREATE(ListRemoteKeyEntityTable)
                },
            ),
        )
        v2 suspendedScope {
            ListRemoteKeyEntityTable INSERT_OR_REPLACE ListCursor(2, 5, 0L).toEntity(TOP_RATED_LIST_KEY)
        }
        v2.close()
    }

    private suspend fun Database.columnsOf(table: String): List<String> {
        lateinit var info: PragmaStatement<List<ColumnInfo>>
        suspendedScope { info = PRAGMA_TABLE_INFO(table) }
        return info.getResults().map(ColumnInfo::name)
    }

    @Test
    fun theJunctionTableIsDroppedOnUpgrade() = runTest {
        createVersion2Database()

        val database = createMovieDatabase(path)
        try {
            // PRAGMA table_info of a table that doesn't exist returns no columns.
            assertEquals(emptyList(), database.columnsOf("movie_genres"))
        } finally {
            database.close()
        }
    }

    @Test
    fun moviesGetsTheGenreColumnOnUpgrade() = runTest {
        createVersion2Database()

        val database = createMovieDatabase(path)
        try {
            assertTrue("genreIds" in database.columnsOf("movies"))
        } finally {
            database.close()
        }
    }

    /** A cache starts over rather than migrating: the cursor and the list go with the old tables. */
    @Test
    fun theCachedListStartsOverAndTheDatabaseIsUsable() = runTest {
        createVersion2Database()

        val database = createMovieDatabase(path)
        try {
            val local = MovieLocalDataSourceImpl(database, Dispatchers.Unconfined)
            assertNull(local.cursorOf(TOP_RATED_LIST_KEY))

            val movie = Movie(1L, "t", "o", null, null, 7.0, listOf(28, 12))
            local.replaceList(TOP_RATED_LIST_KEY, 20, listOf(movie), ListCursor(null, 1, 0L))
            assertEquals(listOf(movie), local.moviesIn(TOP_RATED_LIST_KEY, 20, 0))
        } finally {
            database.close()
        }
    }
}
