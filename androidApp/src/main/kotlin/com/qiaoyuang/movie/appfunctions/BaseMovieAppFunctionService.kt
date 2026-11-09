package com.qiaoyuang.movie.appfunctions

import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint

/**
 * AppFunctions exposing MyMovie's discovery capabilities to AI agents and system assistants.
 *
 * Since AppFunctions 1.0.0-rc01 the functions are declared on the service itself: the compiler
 * generates `MovieAppFunctionService` from this class, and Android instantiates it like any other
 * service. That replaces the AppFunctionConfiguration factory the Application used to provide,
 * which handed out a singleton of a separate `MovieFunctions` class.
 *
 * The functions are suspend because a non-suspend AppFunction runs on the main thread; the
 * repository behind the bridge does its own dispatching.
 */
@AppFunctionServiceEntryPoint(
    serviceName = "MovieAppFunctionService",
    appFunctionXmlFileName = "movie_app_functions",
)
abstract class BaseMovieAppFunctionService : AppFunctionService() {

    // Koin is started in MovieApplication.onCreate, which runs before any service is created.
    private val bridge = MovieDataBridge.instance

    /**
     * Search for movies by a keyword or phrase.
     * Required workflow: Use the returned title with getSimilarMovies to explore related titles.
     *
     * @param movieName Keyword or phrase to search for (e.g., "Inception", "Christopher Nolan").
     * @return A list of [MovieInfo] matching the movieName, or an empty list if no results were found.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun searchMovies(movieName: String): List<MovieInfo> =
        bridge.searchMovies(movieName, 1)?.map { it.toMovieInfo() } ?: emptyList()

    /**
     * Retrieve the current list of top-rated movies.
     * Required workflow: Use a returned title with getSimilarMovies for deeper exploration.
     *
     * @return A list of [MovieInfo] for the top-rated movies on TMDB.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun getTopRatedMovies(): List<MovieInfo> =
        bridge.getTopRatedMovies(1)?.map { it.toMovieInfo() } ?: emptyList()

    /**
     * Find movies similar to a given title.
     *
     * @param movieName The title of the movie to find similar titles for (e.g., "Inception").
     * @return A list of [MovieInfo] for movies similar to the specified title, or an empty list if
     *   no movie with that exact title was found.
     */
    // No try/catch: the bridge already turns every failure into null, so the only exception that
    // can reach here is the cancellation of this call — which has to propagate, not become an
    // empty list.
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun getSimilarMovies(movieName: String): List<MovieInfo> =
        bridge.searchMovies(movieName, 1)
            ?.find { it.title.equals(movieName, ignoreCase = true) }
            ?.let { bridge.getSimilarMovies(it.id, 1) }
            ?.map { it.toMovieInfo() }
            ?: emptyList()
}

private fun MovieData.toMovieInfo() = MovieInfo(
    id = id,
    title = title,
    overview = overview,
    rating = rating,
    posterUrl = posterUrl,
)
