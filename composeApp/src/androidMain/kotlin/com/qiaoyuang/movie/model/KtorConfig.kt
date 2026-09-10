package com.qiaoyuang.movie.model

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import okhttp3.Cache
import java.io.File

private const val HTTP_CACHE_DIR = "http_cache"
private const val HTTP_CACHE_SIZE = 20L * 1024 * 1024 // 20 MiB

/**
 * Built lazily so that [appContext] is guaranteed to be set by the time the engine is
 * created (Koin only resolves the HttpClient once the UI asks for it).
 *
 * Giving OkHttp a [Cache] turns on its RFC 9111 disk cache, which iOS already gets for
 * free from NSURLSession's shared URLCache. TMDB answers with `Cache-Control: public,
 * max-age=26992` and an ETag, so a repeated GET is served from disk while still fresh and
 * revalidated with `If-None-Match` afterwards — a 304 costs a round trip but no body.
 */
internal actual val ktorEngine: HttpClientEngine by lazy {
    OkHttp.create {
        config {
            cache(Cache(File(appContext.cacheDir, HTTP_CACHE_DIR), HTTP_CACHE_SIZE))
        }
    }
}
