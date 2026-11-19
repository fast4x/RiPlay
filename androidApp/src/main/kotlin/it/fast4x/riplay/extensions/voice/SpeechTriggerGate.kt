package it.fast4x.riplay.extensions.voice

/**
 * Decides when a captured audio level means somebody started speaking.
 *
 * The reference is a "peak floor": it follows the music upward quickly enough
 * to absorb a louder section, and downward very slowly, so a short word can
 * never raise the bar it just spoke over. While a voiced run is in progress
 * the floor is frozen — the user's own voice must not lift the reference
 * mid-word and disarm the trigger. A brief dip inside the utterance (a
 * consonant, a pause between "RiPlay" and the command) keeps the run alive
 * instead of restarting the count from zero.
 *
 * Two working points:
 * - playback active: the microphone also hears the song, so the trigger needs
 *   more consecutive frames and a slightly wider margin over the floor;
 * - room quiet (headphones or paused music): the trigger is faster because
 *   almost everything above the ambient floor is a voice.
 *
 * The first [warmupMs] only calibrate the floor: right after playback resumes
 * the song can be far louder than the converged floor, and a first beat must
 * not wake the recognizer.
 *
 * Kept free of Android APIs so the behaviour can be unit tested.
 */
internal class SpeechTriggerGate(
    private val playbackActive: Boolean = true,
    private val warmupMs: Long = DEFAULT_WARMUP_MS,
) {

    var peakFloorDb: Double = INITIAL_FLOOR_DB
        private set

    private var startedAtMs: Long? = null
    private var voicedFrames = 0

    fun reset(nowMs: Long) {
        startedAtMs = nowMs
        peakFloorDb = INITIAL_FLOOR_DB
        voicedFrames = 0
    }

    fun isWarmingUp(nowMs: Long): Boolean {
        val startedAt = startedAtMs ?: return true
        return nowMs - startedAt < warmupMs
    }

    /**
     * Feeds one audio frame.
     *
     * @return true when enough consecutive speech-like frames were seen.
     */
    fun onFrame(levelDb: Double, nowMs: Long): Boolean {
        if (startedAtMs == null) {
            reset(nowMs)
        }

        if (isWarmingUp(nowMs)) {
            // Treat everything heard during warmup as the reference, including
            // a loud first moment.
            peakFloorDb = maxOf(peakFloorDb, levelDb)
            voicedFrames = 0
            return false
        }

        val marginDb =
            if (playbackActive) PLAYBACK_MARGIN_DB else QUIET_MARGIN_DB
        val minLevelDb =
            if (playbackActive) PLAYBACK_MIN_LEVEL_DB else QUIET_MIN_LEVEL_DB
        val requiredFrames =
            if (playbackActive) TRIGGER_PLAYBACK_FRAMES else TRIGGER_QUIET_FRAMES

        return when {
            // Voiced frame: count it, freeze the floor (the voice must not
            // raise the reference while it is speaking).
            levelDb >= peakFloorDb + marginDb && levelDb >= minLevelDb -> {
                voicedFrames++
                voicedFrames >= requiredFrames
            }

            // Brief dip inside the utterance: keep the run alive, do not touch
            // the floor.
            voicedFrames > 0 && levelDb >= peakFloorDb + BRIDGE_MARGIN_DB -> false

            // Silence or music below the trigger zone: follow the level and
            // restart the voiced count. The reference rises fast enough to
            // swallow a louder music section, and sinks slowly enough that a
            // word cannot lower the bar behind itself.
            else -> {
                val alpha =
                    if (levelDb > peakFloorDb) PEAK_ATTACK_ALPHA else PEAK_DECAY_ALPHA
                peakFloorDb += alpha * (levelDb - peakFloorDb)
                voicedFrames = 0
                false
            }
        }
    }

    internal companion object {
        const val DEFAULT_WARMUP_MS = 1_200L
        const val PLAYBACK_MARGIN_DB = 8.5
        const val QUIET_MARGIN_DB = 9.0
        const val PLAYBACK_MIN_LEVEL_DB = -48.0
        const val QUIET_MIN_LEVEL_DB = -55.0
        const val TRIGGER_PLAYBACK_FRAMES = 7 // about 140 ms of speech
        const val TRIGGER_QUIET_FRAMES = 4 // about 80 ms of speech
        const val BRIDGE_MARGIN_DB = 4.0
        const val PEAK_ATTACK_ALPHA = 0.08
        const val PEAK_DECAY_ALPHA = 0.01
        const val INITIAL_FLOOR_DB = -90.0
    }
}
