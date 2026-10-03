package com.qiaoyuang.movie.model.local

import com.ctrip.sqllin.dsl.annotation.CompositePrimaryKey
import com.ctrip.sqllin.dsl.annotation.DBRow
import com.ctrip.sqllin.dsl.annotation.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * The offline cache schema. sqllin-processor turns each @DBRow class into a `<ClassName>Table`
 * object, which is how the DSL refers to the table.
 *
 * Entities deliberately mirror the domain models rather than the API DTOs: what we cache is
 * what the UI needs, so a change to TMDB's wire format does not become a schema migration.
 *
 * No foreign keys: SQLite only enforces them after `PRAGMA foreign_keys=1`, and nothing in this
 * app deletes a movie or a genre row, so a cascade would never fire. The two places that need
 * referential cleanup (a movie's genres changing) delete explicitly instead.
 */

/**
 * One movie, shared by every list. A non-null Long @PrimaryKey means the caller supplies the
 * key — TMDB's id — and it is still an INTEGER PRIMARY KEY, so it is the rowid itself.
 */
@DBRow("movies")
@Serializable
internal data class MovieEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val overview: String,
    val posterPath: String?,
    val backdropPath: String?,
    val voteAverage: Double?,
)

/**
 * The genre catalogue (id -> display name).
 */
@DBRow("genres")
@Serializable
internal data class GenreEntity(
    @PrimaryKey val id: Int,
    val name: String,
)

/**
 * Which genres a movie belongs to. Rebuilds Movie.genreIds on read.
 */
@DBRow("movie_genres")
@Serializable
internal data class MovieGenreEntity(
    @CompositePrimaryKey val movieId: Long,
    @CompositePrimaryKey val genreId: Int,
)

/**
 * Membership and ordering of one paged list. `listKey` identifies the list ("top_rated",
 * "similar:550"), so one table and one PagingSource serve every paged screen.
 *
 * The primary key is (listKey, movieId) rather than (listKey, position): that is the real
 * identity of a row, it makes the duplicate a page overlap would produce a constraint
 * violation instead of a second copy, and its index's left prefix (listKey) is exactly what
 * every read filters on. The cost is that a position is no longer unique by construction of
 * the schema, so [listEntriesFor] owns that invariant and is tested directly.
 */
@DBRow("movie_list_entries")
@Serializable
internal data class MovieListEntryEntity(
    @CompositePrimaryKey val listKey: String,
    @CompositePrimaryKey val movieId: Long,
    val position: Int,
)

/**
 * Paging cursor and freshness stamp for one list. `nextPage` is null once the server has no
 * more pages; `lastRefreshedAt` drives the TTL that decides whether RemoteMediator refreshes.
 */
@DBRow("list_remote_keys")
@Serializable
internal data class ListRemoteKeyEntity(
    @PrimaryKey val listKey: String,
    val nextPage: Int?,
    val totalPages: Int,
    val lastRefreshedAt: Long,
)

/**
 * Not a table — the result type of a COUNT(*). sqllin reads a selected expression into a
 * property named with AS, so an aggregate needs a type to land in; Long because that is what
 * SQLite returns for count.
 */
@Serializable
internal data class ListEntryCount(val entries: Long)
