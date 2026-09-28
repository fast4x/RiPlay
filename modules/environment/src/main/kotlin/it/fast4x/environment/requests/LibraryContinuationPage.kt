package it.fast4x.environment.requests

import it.fast4x.environment.Environment

data class LibraryContinuationPage(
    val items: List<Environment.Item>,
    val continuation: String?,
)