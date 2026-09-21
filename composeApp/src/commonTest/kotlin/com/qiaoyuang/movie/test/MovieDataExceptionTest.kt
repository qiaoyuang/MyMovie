package com.qiaoyuang.movie.test

import com.qiaoyuang.movie.model.MovieDataException
import com.qiaoyuang.movie.model.dto.ApiMovieResponseDTO
import com.qiaoyuang.movie.model.toMovieDataException
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Drives a real HttpClient over MockEngine, so the mapper is fed the exceptions Ktor actually
 * throws rather than ones built by hand. That matters: Ktor rethrows decoding failures as
 * JsonConvertException, and a hand-built SerializationException would have let a wrong
 * `is SerializationException` branch pass.
 */
class MovieDataExceptionTest {

    private fun client(handler: MockRequestHandler) = HttpClient(MockEngine(handler)) {
        expectSuccess = true // as in production: a non-2xx status throws a ResponseException
        install(ContentNegotiation) { json() }
    }

    private suspend fun failureOf(url: String = "https://example.com/movie", handler: MockRequestHandler): Throwable =
        assertNotNull(
            runCatching { client(handler).get(url).body<ApiMovieResponseDTO>() }.exceptionOrNull()
        )

    @Test
    fun test_http_error_keeps_the_status_code() = runTest {
        val failure = failureOf { respond("", HttpStatusCode.NotFound) }.toMovieDataException()
        assertEquals(404, assertIs<MovieDataException.Http>(failure).statusCode)
    }

    @Test
    fun test_malformed_body_is_parse() = runTest {
        val failure = failureOf {
            respond("not json", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        assertIs<MovieDataException.Parse>(failure.toMovieDataException())
    }

    @Test
    fun test_io_failure_is_network() = runTest {
        assertIs<MovieDataException.Network>(failureOf { throw IOException("offline") }.toMovieDataException())
    }

    @Test
    fun test_anything_else_is_unknown() {
        assertIs<MovieDataException.Unknown>(IllegalArgumentException("boom").toMovieDataException())
    }

    @Test
    fun test_already_classified_passes_through_unchanged() {
        val failure = MovieDataException.Network(RuntimeException())
        assertSame(failure, failure.toMovieDataException())
    }

    /** Ktor's ClientRequestException embeds the full request URL, and ours all carry api_key. */
    @Test
    fun test_message_does_not_leak_the_request_url() = runTest {
        val raw = failureOf(url = "https://example.com/movie?api_key=SECRET") {
            respond("", HttpStatusCode.Unauthorized)
        }
        // Proves the test can fail: the raw Ktor exception really does carry the key...
        assertTrue(raw.message.orEmpty().contains("SECRET"))
        // ...and the classified one does not surface it.
        assertFalse(raw.toMovieDataException().message.orEmpty().contains("SECRET"))
    }
}
