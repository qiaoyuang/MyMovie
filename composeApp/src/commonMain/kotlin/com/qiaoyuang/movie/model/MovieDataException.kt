package com.qiaoyuang.movie.model

import io.ktor.client.plugins.ResponseException
import io.ktor.serialization.JsonConvertException
import kotlinx.io.IOException

/**
 * The data layer's error contract. Layers above see only these types — never Ktor's or
 * kotlinx-serialization's — so the network stack can change without touching them.
 *
 * Sealed so a `when` over it is exhaustive, and a Throwable so it can go straight into Paging's
 * LoadResult.Error and through try/catch.
 *
 * Each subtype carries a fixed message rather than forwarding the cause's: Ktor's
 * ClientRequestException embeds the full request URL, and every URL carries the api_key
 * parameter. The original exception stays reachable as [cause] for debugging.
 */
internal sealed class MovieDataException(message: String, cause: Throwable) : Exception(message, cause) {

    /** No connection, DNS failure or timeout. Retrying may help. */
    class Network(cause: Throwable) : MovieDataException("Network unavailable", cause)

    /** The server answered with a non-2xx status. What a given code means is left to callers. */
    class Http(val statusCode: Int, cause: Throwable) : MovieDataException("HTTP $statusCode", cause)

    /** The response body did not have the expected shape. */
    class Parse(cause: Throwable) : MovieDataException("Unexpected response body", cause)

    /** Anything not classified above. */
    class Unknown(cause: Throwable) : MovieDataException("Unknown data error", cause)
}

/**
 * Classifies whatever the network stack threw.
 *
 * - Ktor's KotlinxSerializationConverter rethrows decoding failures as JsonConvertException, so
 *   matching kotlinx-serialization's own SerializationException here would never fire.
 * - kotlinx.io.IOException is java.io.IOException on the JVM (OkHttp's UnknownHostException,
 *   SocketTimeoutException, …) and the base of the Darwin engine's DarwinHttpRequestException on
 *   iOS, so this one branch covers network failures on both platforms.
 */
internal fun Throwable.toMovieDataException(): MovieDataException = when (this) {
    is MovieDataException -> this
    is ResponseException -> MovieDataException.Http(response.status.value, this)
    is JsonConvertException -> MovieDataException.Parse(this)
    is IOException -> MovieDataException.Network(this)
    else -> MovieDataException.Unknown(this)
}
