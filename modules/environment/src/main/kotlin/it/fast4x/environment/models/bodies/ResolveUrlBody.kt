package it.fast4x.environment.models.bodies

import it.fast4x.environment.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class ResolveUrlBody(
    val context: Context = Context.DefaultWeb,
    val url: String
)