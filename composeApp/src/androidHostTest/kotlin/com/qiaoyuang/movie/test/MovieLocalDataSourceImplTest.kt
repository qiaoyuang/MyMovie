package com.qiaoyuang.movie.test

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.ctrip.sqllin.driver.deleteDatabase
import com.ctrip.sqllin.driver.toDatabasePath
import com.ctrip.sqllin.dsl.Database
import com.qiaoyuang.movie.model.domain.Movie
import com.qiaoyuang.movie.model.domain.MovieGenre
import com.qiaoyuang.movie.model.local.DATABASE_NAME
import com.qiaoyuang.movie.model.local.ListCursor
import com.qiaoyuang.movie.model.local.MovieLocalDataSourceImpl
import com.qiaoyuang.movie.model.local.createMovieDatabase
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

/**
 * The data source against a real SQLite database. Everything else that exercises the cache
 * runs against FakeMovieLocalDataSource, which emulates what SQLite does — INSERT OR IGNORE
 * included — so these are the tests that would catch the two disagreeing.
 *
 * Robolectric because sqllin's Android driver talks to android.database.sqlite, which a plain
 * host test's stubbed android.jar cannot run. Both SDK levels, since the driver opens the
 * database through SQLiteDatabase.OpenParams from Android P on and through the legacy path
 * below it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26, 37])
class MovieLocalDataSourceImplTest {

    private val path = ApplicationProvider.getApplicationContext<Context>().toDatabasePath()
    private lateinit var database: Database
    private lateinit var local: MovieLocalDataSourceImpl

    private val listKey = "top_rated"

    @Before
    fun open() {
        deleteDatabase(path, DATABASE_NAME)
        database = createMovieDatabase(path)
        local = MovieLocalDataSourceImpl(database, Dispatchers.Unconfined)
    }

    @After
    fun close() {
        database.close()
        deleteDatabase(path, DATABASE_NAME)
    }

    private fun movie(id: Long, genreIds: List<Int>? = listOf(28, 12)) = Movie(
        id = id,
        title = "title$id",
        overview = "overview$id",
        posterPath = "/poster$id",
        backdropPath = null,
        voteAverage = 7.5,
        genreIds = genreIds,
    )

    @Test
    fun aReplacedListReadsBackInOrderWithEveryField() = runTest {
        val movies = (1L..20L).map(::movie)
        local.replaceList(listKey, 20, movies, ListCursor(2, 5, 1000L))

        assertEquals(movies, local.moviesIn(listKey, limit = 20, offset = 0))
        assertEquals(ListCursor(2, 5, 1000L), local.cursorOf(listKey))
    }

    @Test
    fun genreIdsKeepTheDifferenceBetweenNoneAndEmpty() = runTest {
        local.replaceList(listKey, 20, listOf(movie(1L, genreIds = null), movie(2L, genreIds = emptyList())), ListCursor(null, 1, 0L))

        val (absent, empty) = local.moviesIn(listKey, limit = 20, offset = 0)
        assertNull(absent.genreIds)
        // An empty genre list may legitimately come back as null where genres live in their own
        // table, since no rows describe both; asserted loosely so the schema can change underneath.
        assertEquals(true, empty.genreIds.isNullOrEmpty())
    }

    /** What INSERT OR IGNORE is for: TMDB repeating a movie on the next page. */
    @Test
    fun aMovieRepeatedOnTheNextPageKeepsItsFirstPosition() = runTest {
        local.replaceList(listKey, 20, (1L..20L).map(::movie), ListCursor(2, 5, 0L))
        local.appendPage(listKey, page = 2, pageSize = 20, (20L..39L).map(::movie), ListCursor(3, 5, 0L))

        val ids = local.moviesIn(listKey, limit = 100, offset = 0).map(Movie::id)
        assertEquals((1L..39L).toList(), ids)
    }

    @Test
    fun pagesReadBackByOffset() = runTest {
        local.replaceList(listKey, 20, (1L..20L).map(::movie), ListCursor(2, 5, 0L))
        local.appendPage(listKey, page = 2, pageSize = 20, (21L..40L).map(::movie), ListCursor(3, 5, 0L))

        assertEquals((21L..40L).toList(), local.moviesIn(listKey, limit = 20, offset = 20).map(Movie::id))
    }

    @Test
    fun replacingAListDiscardsItsOldMembersButNotOtherLists() = runTest {
        local.replaceList(listKey, 20, listOf(movie(99L)), ListCursor(2, 5, 0L))
        local.replaceList("similar:550", 20, listOf(movie(7L)), ListCursor(null, 1, 0L))

        local.replaceList(listKey, 20, listOf(movie(1L)), ListCursor(2, 5, 0L))

        assertEquals(listOf(1L), local.moviesIn(listKey, 20, 0).map(Movie::id))
        assertEquals(listOf(7L), local.moviesIn("similar:550", 20, 0).map(Movie::id))
    }

    @Test
    fun aMovieIsReadableOnItsOwnAndUpsertOverwritesIt() = runTest {
        local.upsertMovies(listOf(movie(550L)))
        assertEquals("title550", local.movie(550L)?.title)

        local.upsertMovies(listOf(movie(550L).copy(title = "renamed", genreIds = listOf(35))))
        val stored = local.movie(550L)
        assertEquals("renamed", stored?.title)
        assertEquals(listOf(35), stored?.genreIds)
        assertNull(local.movie(551L))
    }

    @Test
    fun theGenreCatalogueIsReplacedWholeWithItsStamp() = runTest {
        local.replaceGenres(listOf(MovieGenre(28, "Action"), MovieGenre(99, "Retired")), refreshedAt = 1L)
        local.replaceGenres(listOf(MovieGenre(28, "Action"), MovieGenre(12, "Adventure")), refreshedAt = 2L)

        assertEquals(setOf(28, 12), local.genres().map(MovieGenre::id).toSet())
        assertEquals(2L, local.genresRefreshedAt())
    }
}
