package it.fast4x.riplay.extensions.voice

/**
 * Decides when a captured audio level means somebody started speaking.
 *
 * The first [warmupMs] only calibrates the noise floor: right after playback
 * resumes the song can be far louder than the converged floor, and a beat must
 * not wake the recognizer. After warmup the gate also requires [settleFrames]
 * of quiet frames before it trusts a voiced burst, because the echo canceler
 * needs a moment to adapt to the song that just started playing. Only then do
 * [triggerFrames] consecutive voiced frames trigger.
 *
 * Kept free of Android APIs so the behaviour can be unit tested.
 */
internal class SpeechTriggerGate(
    private val warmupMs: Long = DEFAULT_WARMUP_MS,
    private val settleFrames: Int = DEFAULT_SETTLE_FRAMES,
    private val triggerFrames: Int = DEFAULT_TRIGGER_FRAMES,
    private val noiseMarginDb: Double = DEFAULT_NOISE_MARGIN_DB,
    private val minLevelDb: Double = DEFAULT_MIN_LEVEL_DB,
) {

    var noiseFloorDb: Double = INITIAL_NOISE_DB
        private set

    var isSettled: Boolean = false
        private set

    private var startedAtMs: Long? = null
    private var voicedFrames = 0
    private var quietFrames = 0

    fun reset(nowMs: Long) {
        startedAtMs = nowMs
        noiseFloorDb = INITIAL_NOISE_DB
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
     * @return true when enough consecutive speech-like frames were seen.
     */
    fun onFrame(levelDb: Double, nowMs: Long): Boolean {
        if (startedAtMs == null) {
            reset(nowMs)
        }

        if (isWarmingUp(nowMs)) {
            // Treat everything heard during warmup as the noise floor,
            // including a loud first moment.
            noiseFloorDb = maxOf(noiseFloorDb, levelDb)
            voicedFrames = 0
            quietFrames = 0
            isSettled = false
            return false
        }

        val voiced = levelDb >= noiseFloorDb + noiseMarginDb && levelDb >= minLevelDb
        if (voiced) {
            // A burst before the floor has been quiet for a while is music
            // leaking through, not the user. Do not count it.
            if (isSettled) {
                voicedFrames++
            } else {
                voicedFrames = 0
            }
        } else {
            voicedFrames = 0
            // Follow the room/music floor quickly downwards and slowly
            // upwards so a short word cannot raise it.
            val alpha = if (levelDb < noiseFloorDb) 0.2 else 0.03
            noiseFloorDb += alpha * (levelDb - noiseFloorDb)
            // Only a genuinely low level proves the echo canceler converged.
            // Music that is merely below the speech margin must not settle the
            // gate, or its next beat would look like a command.
            if (levelDb < minLevelDb) {
                if (quietFrames < settleFrames) {
                    quietFrames++
                }
            } else {
                quietFrames = 0
            }
        }

        isSettled = quietFrames >= settleFrames
        return voicedFrames >= triggerFrames
    }

    internal companion object {
        const val DEFAULT_WARMUP_MS = 1_600L
        const val DEFAULT_SETTLE_FRAMES = 75 // about 1.5 s at 20 ms per frame
        const val DEFAULT_TRIGGER_FRAMES = 4 // about 80 ms of speech
        const val DEFAULT_NOISE_MARGIN_DB = 9.0
        const val DEFAULT_MIN_LEVEL_DB = -50.0
        const val INITIAL_NOISE_DB = -90.0
    }
}
