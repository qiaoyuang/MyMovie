package com.qiaoyuang.movie.model.local

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json

/**
 * Stores a movie's genre ids as one JSON string in a TEXT column. sqllin uses a property's
 * serializer as its column converter, and a column has to hold a single value, which a List's
 * own serializer — a structure — cannot be.
 *
 * Order is part of the value: TMDB lists a movie's main genre first, and the similar-movies strip
 * joins the names in that order. The junction table this replaces lost it — read back through its
 * (movieId, genreId) key, the ids came out in ascending order.
 */
internal object GenreIdsAsJson : KSerializer<List<Int>> {

    private val ids = ListSerializer(Int.serializer())

    override val descriptor = PrimitiveSerialDescriptor("GenreIdsAsJson", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: List<Int>) =
        encoder.encodeString(Json.encodeToString(ids, value))

    override fun deserialize(decoder: Decoder): List<Int> =
        Json.decodeFromString(ids, decoder.decodeString())
}
