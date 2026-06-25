package com.qiaoyuang.movie.appfunctions

import androidx.appfunctions.AppFunctionContext
import androidx.appfunctions.service.AppFunction

/**
 * AppFunctions exposing MyMovie's discovery capabilities to AI agents and system assistants.
 */
class MovieFunctions private constructor() {

    private val bridge = MovieDataBridge.getInstance()

    companion object {
        @Volatile
        private var instance: MovieFunctions? = null

        fun getInstance(): MovieFunctions = instance ?: synchronized(this) {
            instance ?: MovieFunctions().also { instance = it }
        }
    }

    /**
     * Search for movies by a keyword or phrase.
     * Required workflow: Use the returned id with getMovieDetails to fetch full details,
     * or with getSimilarMovies to explore related titles.
     *
     * @param appFunctionContext The execution context.
     * @param movieName Keyword or phrase to search for (e.g., "Inception", "Christopher Nolan").
     * @return A list of [MovieInfo] matching the movieName, or an empty list if no results were found.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun searchMovies(
        appFunctionContext: AppFunctionContext,
        movieName: String,
    ): List<MovieInfo> = bridge.searchMovies(movieName, 1)?.map { it.toMovieInfo() } ?: emptyList()

    /**
     * Retrieve the current list of top-rated movies.
     * Required workflow: Use the returned id with getMovieDetails or getSimilarMovies for deeper exploration.
     *
     * @param appFunctionContext The execution context.
     * @return A list of [MovieInfo] for the top-rated movies on TMDB.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun getTopRatedMovies(
        appFunctionContext: AppFunctionContext,
    ): List<MovieInfo> = bridge.getTopRatedMovies(1)?.map { it.toMovieInfo() } ?: emptyList()

    /**
     * Find movies similar to a given title.
     *
     * @param appFunctionContext The execution context.
     * @param movieName The title of the movie to find similar titles for (e.g., "Inception").
     * @return A list of [MovieInfo] for movies similar to the specified title.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun getSimilarMovies(
        appFunctionContext: AppFunctionContext,
        movieName: String,
    ): List<MovieInfo> = try {
        bridge
            .searchMovies(movieName, 1)
            ?.find { it.title.equals(movieName, true) }
            ?.let {
                bridge.getSimilarMovies(it.id, 1)
            }
            ?.map { it.toMovieInfo() }
            ?: emptyList()
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }
}

private fun MovieData.toMovieInfo() = MovieInfo(
    id = id,
    title = title,
    overview = overview,
    rating = rating,
    posterUrl = posterUrl,
)
