package it.fast4x.spotifymeta.utils

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/** JsonObject o null, se l'elemento non e' un oggetto. */
fun JsonElement?.jsonObjectOrNull(): JsonObject? = this as? JsonObject

/** JsonArray o null, se l'elemento non e' un array. */
fun JsonElement?.jsonArrayOrNull(): JsonArray? = this as? JsonArray

/** Contenuto di una primitiva, se l'elemento e' una primitiva non nulla. */
fun JsonElement?.jsonPrimitiveContent(): String? = (this as? JsonPrimitive)?.contentOrNull
