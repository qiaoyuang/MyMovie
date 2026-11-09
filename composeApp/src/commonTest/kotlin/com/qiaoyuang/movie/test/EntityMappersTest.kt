package com.qiaoyuang.movie.test

import com.qiaoyuang.movie.model.domain.Movie
import com.qiaoyuang.movie.model.local.GenreIdsAsJson
import com.qiaoyuang.movie.model.local.distinctMovies
import com.qiaoyuang.movie.model.local.listEntriesFor
import com.qiaoyuang.movie.model.local.toDomain
import com.qiaoyuang.movie.model.local.toEntity
import kotlinx.serialization.json.Json
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
        assertEquals(original, original.toEntity().toDomain())
        // Absent genres stay absent rather than becoming an empty list.
        assertNull(movie(2L).toEntity().toDomain().genreIds)
    }

    /**
     * The column converter is ordinary kotlinx.serialization, so its round trip can be checked
     * without a database: order, an empty list and duplicates all have to come back as given.
     */
    @Test
    fun genreIdsKeepTheirOrderThroughTheColumnConverter() {
        val json = Json
        for (ids in listOf(listOf(28, 12), listOf(12, 28), emptyList(), listOf(35, 35))) {
            val stored = json.encodeToString(GenreIdsAsJson, ids)
            assertEquals(ids, json.decodeFromString(GenreIdsAsJson, stored))
        }
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
    fun aPageThatRepeatsAMovieInsideItselfKeepsTheFirstCopy() {
        val page = listOf(movie(1L), movie(2L), movie(1L))
        assertEquals(listOf(1L, 2L), page.distinctMovies().map(Movie::id))
    }

    /**
     * Overlap between pages is no longer filtered here — INSERT OR IGNORE drops the conflicting
     * row — so positions now mirror the server's ordering and the skipped movie's position is
     * simply left unused. Harmless, because reads page through with OFFSET, which counts rows.
     * The behaviour itself is covered by MovieRemoteMediatorTest through the fake.
     */
    @Test
    fun positionsMirrorTheServerOrderingRatherThanBeingCompacted() {
        val page2 = listOf(movie(20L), movie(21L), movie(22L))
        val entries = listEntriesFor("top_rated", page = 2, pageSize = 20, movieIds = page2.map(Movie::id))
        assertEquals(listOf(20, 21, 22), entries.map { it.position })
        // Movie 20 already sits at position 19 from page one, so storing these skips its row and
        // leaves position 20 empty — movies 21 and 22 keep 21 and 22.
        assertEquals(listOf(20L, 21L, 22L), entries.map { it.movieId })
    }
}
