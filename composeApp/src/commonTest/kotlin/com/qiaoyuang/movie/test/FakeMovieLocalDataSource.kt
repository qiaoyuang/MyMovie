package com.qiaoyuang.movie.test

import com.qiaoyuang.movie.model.domain.Movie
import com.qiaoyuang.movie.model.domain.MovieGenre
import com.qiaoyuang.movie.model.local.FIRST_PAGE
import com.qiaoyuang.movie.model.local.ListCursor
import com.qiaoyuang.movie.model.local.MovieListEntryEntity
import com.qiaoyuang.movie.model.local.MovieLocalDataSource
import com.qiaoyuang.movie.model.local.TableInvalidationTracker
import com.qiaoyuang.movie.model.local.distinctMovies
import com.qiaoyuang.movie.model.local.filterAlreadyStored
import com.qiaoyuang.movie.model.local.listEntriesFor

/**
 * In-memory stand-in for the real data source. It exists because sqllin's Android driver talks
 * to android.database.sqlite and so cannot run in host tests, which would otherwise leave the
 * RemoteMediator and the DB-backed PagingSource untested.
 *
 * It reuses the production helpers — listEntriesFor, filterAlreadyStored, distinctMovies — so
 * position numbering and page-overlap behaviour are the real ones, not a second implementation
 * that could quietly disagree.
 */
internal class FakeMovieLocalDataSource : MovieLocalDataSource {

    override val invalidationTracker = TableInvalidationTracker()

    private val moviesById = mutableMapOf<Long, Movie>()
    private val entriesByList = mutableMapOf<String, MutableList<MovieListEntryEntity>>()
    private val cursors = mutableMapOf<String, ListCursor>()
    private var storedGenres = emptyList<MovieGenre>()

    /** Counts calls, so a test can assert the cache answered instead of the network. */
    var readCount = 0
        private set

    override suspend fun moviesIn(listKey: String, limit: Int, offset: Int): List<Movie> {
        readCount++
        return entriesByList[listKey].orEmpty()
            .sortedBy(MovieListEntryEntity::position)
            .drop(offset)
            .take(limit)
            .mapNotNull { moviesById[it.movieId] }
    }

    override suspend fun countIn(listKey: String): Int = entriesByList[listKey].orEmpty().size

    override suspend fun cursorOf(listKey: String): ListCursor? = cursors[listKey]

    override suspend fun replaceList(
        listKey: String,
        pageSize: Int,
        movies: List<Movie>,
        cursor: ListCursor,
    ) {
        val fresh = movies.distinctMovies()
        entriesByList[listKey] =
            listEntriesFor(listKey, FIRST_PAGE, pageSize, fresh.map(Movie::id)).toMutableList()
        fresh.forEach { moviesById[it.id] = it }
        cursors[listKey] = cursor
        invalidationTracker.notifyChanged(listKey)
    }

    override suspend fun appendPage(
        listKey: String,
        page: Int,
        pageSize: Int,
        movies: List<Movie>,
        cursor: ListCursor,
    ) {
        val stored = entriesByList.getOrPut(listKey) { mutableListOf() }
        val fresh = movies.filterAlreadyStored(stored.mapTo(mutableSetOf(), MovieListEntryEntity::movieId))
        stored += listEntriesFor(listKey, page, pageSize, fresh.map(Movie::id))
        fresh.forEach { moviesById[it.id] = it }
        cursors[listKey] = cursor
        invalidationTracker.notifyChanged(listKey)
    }

    override suspend fun movie(id: Long): Movie? = moviesById[id]

    override suspend fun upsertMovies(movies: List<Movie>) {
        movies.distinctMovies().forEach { moviesById[it.id] = it }
    }

    override suspend fun genres(): List<MovieGenre> = storedGenres

    override suspend fun replaceGenres(genres: List<MovieGenre>) {
        storedGenres = genres
    }

    /** Positions as stored, for asserting the ordering the PagingSource will read back. */
    fun positionsIn(listKey: String): List<Pair<Int, Long>> =
        entriesByList[listKey].orEmpty()
            .sortedBy(MovieListEntryEntity::position)
            .map { it.position to it.movieId }
}
