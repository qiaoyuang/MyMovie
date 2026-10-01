package com.qiaoyuang.movie.model.local

import com.qiaoyuang.movie.model.domain.Movie
import com.qiaoyuang.movie.model.domain.MovieGenre
import com.qiaoyuang.movie.model.domain.MovieResponse

/**
 * Domain <-> entity mapping, plus the two list invariants (position numbering and page-overlap
 * de-duplication). All pure functions, so they are testable without a SQLite connection —
 * which matters here because sqllin's Android driver is android.database.sqlite-backed and
 * cannot run against the stubbed android.jar used by host tests.
 */

internal fun Movie.toEntity(): MovieEntity = MovieEntity(
    id = id,
    title = title,
    overview = overview,
    posterPath = posterPath,
    backdropPath = backdropPath,
    voteAverage = voteAverage,
)

/**
 * Genres live in their own table, so they are handed back in separately on read.
 */
internal fun MovieEntity.toDomain(genreIds: List<Int>?): Movie = Movie(
    id = id,
    title = title,
    overview = overview,
    posterPath = posterPath,
    backdropPath = backdropPath,
    voteAverage = voteAverage,
    genreIds = genreIds,
)

internal fun Movie.toGenreCrossRefs(): List<MovieGenreEntity> =
    genreIds?.map { MovieGenreEntity(movieId = id, genreId = it) } ?: emptyList()

/**
 * Rebuilds each movie's genreIds from the junction rows. A movie with no rows gets null rather
 * than an empty list, matching what the API sends when it omits genre_ids.
 */
internal fun List<MovieEntity>.toDomain(crossRefs: List<MovieGenreEntity>): List<Movie> {
    val byMovie = crossRefs.groupBy(MovieGenreEntity::movieId)
    return map { entity -> entity.toDomain(byMovie[entity.id]?.map(MovieGenreEntity::genreId)) }
}

/**
 * The cursor a freshly fetched page implies. Shared so the one rule that decides when a list has
 * run out — the requested page reaching totalPages — cannot be stated twice and differently.
 * Derived from the requested page rather than the one the response echoes back, because a server
 * that misreports its page number would otherwise stall or repeat pagination.
 */
internal fun MovieResponse.cursorAfter(page: Int, refreshedAt: Long): ListCursor = ListCursor(
    nextPage = if (page < totalPages) page + 1 else null,
    totalPages = totalPages,
    lastRefreshedAt = refreshedAt,
)

internal fun ListCursor.toEntity(listKey: String): ListRemoteKeyEntity = ListRemoteKeyEntity(
    listKey = listKey,
    nextPage = nextPage,
    totalPages = totalPages,
    lastRefreshedAt = lastRefreshedAt,
)

internal fun ListRemoteKeyEntity.toCursor(): ListCursor = ListCursor(
    nextPage = nextPage,
    totalPages = totalPages,
    lastRefreshedAt = lastRefreshedAt,
)

internal fun MovieGenre.toEntity(): GenreEntity = GenreEntity(id = id, name = name)

internal fun GenreEntity.toDomain(): MovieGenre = MovieGenre(id = id, name = name)

/**
 * Where page [page] of a list lands. Page N occupies positions [(N-1) * pageSize, N * pageSize),
 * so a page can be rewritten in place and the ordering never depends on insertion order.
 *
 * [movieIds] must already be de-duplicated against what is stored — see [filterAlreadyStored].
 * Its indices, not the caller's, decide positions, so filtering before this call leaves gaps in
 * the numbering. Gaps are harmless: reads order by position and page through with OFFSET, which
 * counts rows rather than position values.
 */
internal fun listEntriesFor(
    listKey: String,
    page: Int,
    pageSize: Int,
    movieIds: List<Long>,
): List<MovieListEntryEntity> {
    val base = (page - 1) * pageSize
    return movieIds.mapIndexed { index, movieId ->
        MovieListEntryEntity(listKey = listKey, movieId = movieId, position = base + index)
    }
}

/**
 * TMDB can return the same movie on two pages when the underlying ordering shifts between
 * requests, which would violate the (listKey, movieId) primary key. sqllin has no
 * INSERT OR IGNORE, and INSERT OR REPLACE would move the movie to its newer position — making
 * an item the user already scrolled past jump down the list. So the duplicate is dropped
 * instead, keeping the first position we saw it at.
 */
internal fun List<Movie>.filterAlreadyStored(storedMovieIds: Set<Long>): List<Movie> =
    distinctMovies().filterNot { it.id in storedMovieIds }

/** A single page can also repeat a movie inside itself, which the same key would reject. */
internal fun List<Movie>.distinctMovies(): List<Movie> = distinctBy(Movie::id)

/** TMDB pages are 1-based. */
internal const val FIRST_PAGE = 1
