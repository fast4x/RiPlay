package it.fast4x.riplay.extensions.voice

/**
 * Decides when a captured audio level means somebody started speaking.
 *
 * The old gate refused to trigger until the captured level had been below a
 * quiet threshold for well over a second ("settled"). That only works when an
 * echo canceler fully removes the song, which most devices do not do for media
 * playback: the AEC reference is the voice-call downlink, not the music mix.
 * With a song playing the level never dropped low enough, the gate never
 * settled and hands-free mode never fired - the recognizer was simply never
 * started while the user was speaking.
 *
 * The gate now works against a *recent-loud reference* ([peakFloorDb]) instead
 * of requiring prior silence:
 *
 * - While the level stays below the trigger margin the reference climbs slowly
 *   toward it ([PEAK_ATTACK_ALPHA]). Ordinary music beats and gradual
 *   crescendos are absorbed this way and never reach the margin at all.
 * - Once a voiced run starts the reference is frozen, so a spoken word keeps
 *   its distance from the reference for the whole run instead of being
 *   absorbed frame by frame.
 * - A short dip inside a word (the /p/ closure of "stop" lasts about 60 ms)
 *   only pauses the run without resetting it, as long as the level stays above
 *   [bridgeMarginDb]. Without the bridge, "stop" would arrive as two runs
 *   shorter than the trigger length and would never wake the recognizer.
 * - The reference falls slowly ([PEAK_DECAY_ALPHA]), so the music getting
 *   quieter does not lower the bar instantly.
 *
 * While nothing is playing the run length is shorter, so even a soft spoken
 * word wakes the recognizer quickly.
 *
 * [isSettled] is kept only for diagnostics: it still tells whether the room is
 * quiet enough that the echo canceler clearly converged.
 *
 * Kept free of Android APIs so the behaviour can be unit tested.
 */
internal class SpeechTriggerGate(
    private val warmupMs: Long = DEFAULT_WARMUP_MS,
    private val settleFrames: Int = DEFAULT_SETTLE_FRAMES,
    private val noiseMarginDb: Double = DEFAULT_NOISE_MARGIN_DB,
    private val playbackMarginDb: Double = DEFAULT_PLAYBACK_MARGIN_DB,
    private val minLevelDb: Double = DEFAULT_MIN_LEVEL_DB,
    private val playbackMinLevelDb: Double = DEFAULT_PLAYBACK_MIN_LEVEL_DB,
    private val triggerFramesQuiet: Int = DEFAULT_TRIGGER_FRAMES_QUIET,
    private val triggerFramesPlayback: Int = DEFAULT_TRIGGER_FRAMES_PLAYBACK,
    private val bridgeMarginDb: Double = DEFAULT_BRIDGE_MARGIN_DB,
) {

    /** Slow follower of the ambient level; used for diagnostics. */
    var noiseFloorDb: Double = INITIAL_NOISE_DB
        private set

    /** Recent-loud reference: absorbs music peaks, freezes while speech runs. */
    var peakFloorDb: Double = INITIAL_NOISE_DB
        private set

    var isSettled: Boolean = false
        private set

    private var startedAtMs: Long? = null
    private var voicedFrames = 0
    private var quietFrames = 0

    fun reset(nowMs: Long) {
        startedAtMs = nowMs
        noiseFloorDb = INITIAL_NOISE_DB
        peakFloorDb = INITIAL_NOISE_DB
        isSettled = false
        voicedFrames = 0
        quietFrames = 0
    }

    fun isWarmingUp(nowMs: Long): Boolean {
        val startedAt = startedAtMs ?: return true
        return nowMs - startedAt < warmupMs
    }

    /**
     * Feeds one audio frame.
     *
     * @param playbackActive true while the player is actually producing sound,
     * which selects the stricter margins that keep music transients from
     * waking the recognizer.
     * @return true when enough consecutive speech-like frames were seen.
     */
    fun onFrame(levelDb: Double, nowMs: Long, playbackActive: Boolean = false): Boolean {
        if (startedAtMs == null) {
            reset(nowMs)
        }

        if (isWarmingUp(nowMs)) {
            // Treat everything heard during warmup as the reference level,
            // including a loud music onset right after playback resumed.
            noiseFloorDb = maxOf(noiseFloorDb, levelDb)
            peakFloorDb = maxOf(peakFloorDb, levelDb)
            voicedFrames = 0
            quietFrames = 0
            isSettled = false
            return false
        }

        val marginDb = if (playbackActive) playbackMarginDb else noiseMarginDb
        val minDb = if (playbackActive) playbackMinLevelDb else minLevelDb
        val requiredFrames = if (playbackActive) triggerFramesPlayback else triggerFramesQuiet

        if (levelDb >= peakFloorDb + marginDb && levelDb >= minDb) {
            // Voiced frame. Freeze the reference for the length of the burst:
            // a spoken word must not chase its own level upwards.
            voicedFrames++
            if (voicedFrames >= requiredFrames) return true
        } else if (voicedFrames > 0 && levelDb >= peakFloorDb + bridgeMarginDb) {
            // Brief dip inside the same utterance (consonant closure, syllable
            // gap): keep the run alive without counting the frame as speech.
            // No state changes: the reference stays frozen.
        } else {
            // The burst is over. Sub-margin loudness (beats, backing vocals,
            // gradual crescendos) is absorbed, so the reference keeps
            // describing the music and the margin stays free for a real voice.
            // Falls back down slowly once the loudness passes.
            if (levelDb > peakFloorDb) {
                peakFloorDb += PEAK_ATTACK_ALPHA * (levelDb - peakFloorDb)
            } else {
                peakFloorDb += PEAK_DECAY_ALPHA * (levelDb - peakFloorDb)
            }
            voicedFrames = 0
        }

        if (levelDb < minLevelDb) {
            if (quietFrames < settleFrames) quietFrames++
        } else {
            quietFrames = 0
        }
        isSettled = quietFrames >= settleFrames

        return false
    }

    internal companion object {
        const val DEFAULT_WARMUP_MS = 1_200L
        const val DEFAULT_SETTLE_FRAMES = 75 // about 1.5 s at 20 ms per frame
        const val DEFAULT_NOISE_MARGIN_DB = 9.0
        const val DEFAULT_PLAYBACK_MARGIN_DB = 8.5
        const val DEFAULT_MIN_LEVEL_DB = -55.0
        const val DEFAULT_PLAYBACK_MIN_LEVEL_DB = -48.0
        const val DEFAULT_TRIGGER_FRAMES_QUIET = 4 // about 80 ms of speech
        const val DEFAULT_TRIGGER_FRAMES_PLAYBACK = 7 // about 140 ms, filters beats

        /** Level above the reference that still belongs to a voiced run. */
        const val DEFAULT_BRIDGE_MARGIN_DB = 4.0

        /** Fraction of the gap absorbed per 20 ms frame while below the margin. */
        const val PEAK_ATTACK_ALPHA = 0.08

        /** Slow decay so words inside one utterance keep crossing the margin. */
        const val PEAK_DECAY_ALPHA = 0.01
        const val INITIAL_NOISE_DB = -90.0
    }
}
