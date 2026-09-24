package it.fast4x.riplay.extensions.musicbrainz.workers

import it.fast4x.riplay.data.Database
import it.fast4x.riplay.extensions.experimental.recommendationstrategy.repository.UserProfileRepository
import it.fast4x.riplay.extensions.musicbrainz.MusicBrainz

/**
 * Singleton provider per accedere alle dipendenze dai Workers.
 * Popolato in Application.onCreate.
 */
object WorkerDependencies {
    lateinit var database: Database
        private set
    lateinit var mbClient: MusicBrainz
        private set
    lateinit var profileRepository: UserProfileRepository
        private set

    fun initialize(
        database: Database,
        mbClient: MusicBrainz,
        profileRepository: UserProfileRepository
    ) {
        this.database = database
        this.mbClient = mbClient
        this.profileRepository = profileRepository
    }

    val isInitialized: Boolean
        get() = ::database.isInitialized && ::mbClient.isInitialized && ::profileRepository.isInitialized
}