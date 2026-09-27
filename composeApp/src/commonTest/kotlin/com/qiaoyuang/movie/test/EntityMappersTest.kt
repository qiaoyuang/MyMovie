package com.qiaoyuang.movie.test

import com.qiaoyuang.movie.model.domain.Movie
import com.qiaoyuang.movie.model.local.MovieGenreEntity
import com.qiaoyuang.movie.model.local.distinctMovies
import com.qiaoyuang.movie.model.local.filterAlreadyStored
import com.qiaoyuang.movie.model.local.listEntriesFor
import com.qiaoyuang.movie.model.local.toDomain
import com.qiaoyuang.movie.model.local.toEntity
import com.qiaoyuang.movie.model.local.toGenreCrossRefs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * The offline cache's pure logic. These are the invariants the schema can no longer enforce on
 * its own (positions) or that sqllin cannot express (INSERT OR IGNORE), so they are tested here
 * rather than through the database — sqllin's Android driver is android.database.sqlite-backed
 * and cannot run against the stubbed android.jar host tests use.
 */
class EntityMappersTest {

    private fun movie(id: Long, genreIds: List<Int>? = null) = Movie(
        id = id,
        title = "title$id",
        overview = "overview$id",
        posterPath = "/poster$id",
        backdropPath = null,
        voteAverage = 7.5,
        genreIds = genreIds,
    )

    @Test
    fun movieSurvivesARoundTripThroughTheEntity() {
        val original = movie(1L, listOf(28, 12))
        val restored = original.toEntity().toDomain(original.genreIds)
        assertEquals(original, restored)
    }

    @Test
    fun genresAreRebuiltPerMovieFromTheJunctionRows() {
        val movies = listOf(movie(1L, listOf(28, 12)), movie(2L, listOf(35)), movie(3L))
        val crossRefs = movies.flatMap(Movie::toGenreCrossRefs)
        assertEquals(3, crossRefs.size)

        val restored = movies.map(Movie::toEntity).toDomain(crossRefs)
        assertEquals(listOf(28, 12), restored[0].genreIds)
        assertEquals(listOf(35), restored[1].genreIds)
        // A movie with no rows reads back as null, matching an API response that omits genre_ids.
        assertNull(restored[2].genreIds)
    }

    @Test
    fun genreCrossRefsCarryTheOwningMovieId() {
        assertEquals(listOf(MovieGenreEntity(movieId = 7L, genreId = 99)), movie(7L, listOf(99)).toGenreCrossRefs())
        assertEquals(emptyList(), movie(7L).toGenreCrossRefs())
    }

    @Test
    fun pageOnePositionsStartAtZero() {
        val entries = listEntriesFor("top_rated", page = 1, pageSize = 20, movieIds = listOf(10L, 11L, 12L))
        assertEquals(listOf(0, 1, 2), entries.map { it.position })
        assertEquals(listOf(10L, 11L, 12L), entries.map { it.movieId })
        assertEquals(listOf("top_rated"), entries.map { it.listKey }.distinct())
    }

    @Test
    fun laterPagesStartAfterEveryPageBeforeThem() {
        val entries = listEntriesFor("top_rated", page = 3, pageSize = 20, movieIds = listOf(10L, 11L))
        assertEquals(listOf(40, 41), entries.map { it.position })
    }

    @Test
    fun positionsNeverCollideAcrossPages() {
        val ids = (1L..60L).toList()
        val positions = (1..3).flatMap { page ->
            listEntriesFor("top_rated", page, pageSize = 20, movieIds = ids.slice((page - 1) * 20 until page * 20))
        }.map { it.position }
        assertEquals(positions.size, positions.distinct().size)
        assertEquals((0..59).toList(), positions)
    }

    @Test
    fun emptyPageProducesNoEntries() {
        assertEquals(emptyList(), listEntriesFor("top_rated", page = 1, pageSize = 20, movieIds = emptyList()))
    }

    @Test
    fun moviesAlreadyInTheListAreDropped() {
        val page = listOf(movie(1L), movie(2L), movie(3L))
        assertEquals(listOf(2L, 3L), page.filterAlreadyStored(setOf(1L)).map(Movie::id))
        assertEquals(emptyList(), page.filterAlreadyStored(setOf(1L, 2L, 3L)))
        assertEquals(listOf(1L, 2L, 3L), page.filterAlreadyStored(emptySet()).map(Movie::id))
    }

    @Test
    fun aPageThatRepeatsAMovieInsideItselfKeepsTheFirstCopy() {
        val page = listOf(movie(1L), movie(2L), movie(1L))
        assertEquals(listOf(1L, 2L), page.distinctMovies().map(Movie::id))
        assertEquals(listOf(1L, 2L), page.filterAlreadyStored(emptySet()).map(Movie::id))
    }

    @Test
    fun filteringThenNumberingLeavesNoDuplicatePositions() {
        // Page 2 overlaps page 1 on movie 20; the survivors are renumbered from the page base,
        // so the gap lands at the end of the page rather than in the middle of the list.
        val page2 = listOf(movie(20L), movie(21L), movie(22L))
        val fresh = page2.filterAlreadyStored(setOf(20L))
        val entries = listEntriesFor("top_rated", page = 2, pageSize = 20, movieIds = fresh.map(Movie::id))
        assertEquals(listOf(20, 21), entries.map { it.position })
        assertEquals(listOf(21L, 22L), entries.map { it.movieId })
    }
}
