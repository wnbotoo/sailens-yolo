package com.sailens.presentation.device

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.sailens.domain.model.common.EventPriority
import com.sailens.domain.model.scene.SceneEvent
import com.sailens.domain.service.LogService
import java.util.IllegalFormatException
import java.util.Locale

/**
 * Android TTS 服务实现
 */
class SpeechManager(
    private val context: Context,
    private val logger: LogService,
) {
    private var tts: TextToSpeech? = null
    @Volatile
    private var _isReady = false
    private var isInitializing = false
    private var initAttempt = 0
    private var initFailureCount = 0
    private var nextInitRetryAtMs = 0L
    private var initTimeoutRunnable: Runnable? = null
    private var pendingSpeech: PendingSpeech? = null
    private val onReadyCallbacks = mutableListOf<() -> Unit>()
    private val mainHandler = Handler(Looper.getMainLooper())

    val isReady: Boolean get() = _isReady

    fun initialize(onReady: (() -> Unit)? = null) {
        runOnMain {
            initializeOnMain(onReady = onReady, forceRetry = true)
        }
    }

    private fun initializeOnMain(
        onReady: (() -> Unit)? = null,
        forceRetry: Boolean = false,
    ) {
        if (_isReady) {
            onReady?.invoke()
            return
        }

        onReady?.let { onReadyCallbacks += it }

        if (isInitializing) {
            logger.debug(TAG, "TTS initialization already in progress")
            return
        }

        val now = SystemClock.elapsedRealtime()
        if (!forceRetry) {
            if (initFailureCount >= MAX_AUTOMATIC_INIT_FAILURES) {
                logger.warning(
                    TAG,
                    "TTS automatic initialization suppressed after repeated failures",
                    mapOf("failureCount" to initFailureCount)
                )
                clearPendingSpeech("automatic retry limit reached")
                return
            }

            if (now < nextInitRetryAtMs) {
                logger.debug(
                    TAG,
                    "TTS initialization retry delayed",
                    mapOf(
                        "retryInMs" to (nextInitRetryAtMs - now),
                        "failureCount" to initFailureCount,
                    )
                )
                return
            }
        }

        if (tts != null) {
            logger.warning(TAG, "Discarding stale TTS instance before retry")
            tts?.shutdown()
            tts = null
        }

        isInitializing = true
        val attempt = ++initAttempt
        logger.info(
            TAG,
            "Initializing TTS",
            mapOf(
                "locale" to Locale.getDefault().toLanguageTag(),
            )
        )

        var createdEngine: TextToSpeech? = null
        createdEngine = TextToSpeech(context.applicationContext) { status ->
            mainHandler.post {
                handleInitCallback(
                    attempt = attempt,
                    createdEngine = createdEngine,
                    status = status,
                )
            }
        }
        tts = createdEngine
        scheduleInitTimeout(attempt)
    }

    /**
     * 播报 SceneEvent 中的文案（通过 strings.xml 的资源 ID）
     */
    fun speak(event: SceneEvent) {
        runOnMain {
            speakOnMain(event)
        }
    }

    private fun speakOnMain(event: SceneEvent) {
        if (!_isReady) {
            logger.warning(
                TAG,
                "TTS speak queued because engine is not ready",
                mapOf("messageKey" to event.messageKey)
            )
            storePendingSpeech(event)
            if (!isInitializing) {
                initializeOnMain(forceRetry = false)
            }
            return
        }

        speakReadyEvent(event)
    }

    private fun speakReadyEvent(event: SceneEvent) {
        if (!_isReady) return

        val text = resolveMessage(event)
        val queueMode = if (event.priority == EventPriority.CRITICAL) {
            TextToSpeech.QUEUE_FLUSH
        } else {
            TextToSpeech.QUEUE_ADD
        }

        val result = tts?.speak(text, queueMode, null, event.id.toString()) ?: TextToSpeech.ERROR
        if (result == TextToSpeech.ERROR) {
            logger.warning(
                TAG,
                "TTS speak failed",
                mapOf(
                    "messageKey" to event.messageKey,
                    "queueMode" to queueMode,
                    "textLength" to text.length,
                )
            )
        } else {
            logger.debug(
                TAG,
                "TTS speak requested",
                mapOf(
                    "messageKey" to event.messageKey,
                    "queueMode" to queueMode,
                )
            )
        }
    }

    fun stop() {
        runOnMain {
            stopOnMain()
        }
    }

    private fun stopOnMain() {
        if (_isReady) {
            tts?.stop()
        } else {
            logger.debug(TAG, "Ignoring TTS stop before engine is ready")
        }
    }

    fun release() {
        runOnMain {
            releaseOnMain()
        }
    }

    private fun releaseOnMain() {
        mainHandler.removeCallbacksAndMessages(null)
        initTimeoutRunnable = null
        stopOnMain()
        tts?.shutdown()
        tts = null
        _isReady = false
        isInitializing = false
        initFailureCount = 0
        nextInitRetryAtMs = 0L
        pendingSpeech = null
        onReadyCallbacks.clear()
        initAttempt++
    }

    private fun scheduleInitTimeout(attempt: Int) {
        clearInitTimeout()
        val timeoutRunnable = Runnable {
            if (attempt != initAttempt || !isInitializing) return@Runnable

            recordInitFailure(
                message = "TTS initialization timed out",
                data = mapOf(
                    "timeoutMs" to INIT_TIMEOUT_MS,
                    "locale" to Locale.getDefault().toLanguageTag(),
                ),
                engine = tts,
            )
        }
        initTimeoutRunnable = timeoutRunnable
        mainHandler.postDelayed(timeoutRunnable, INIT_TIMEOUT_MS)
    }

    private fun clearInitTimeout() {
        initTimeoutRunnable?.let(mainHandler::removeCallbacks)
        initTimeoutRunnable = null
    }

    private fun handleInitCallback(
        attempt: Int,
        createdEngine: TextToSpeech?,
        status: Int,
    ) {
        if (attempt != initAttempt) {
            logger.debug(
                TAG,
                "Ignoring stale TTS init callback",
                mapOf("status" to status)
            )
            createdEngine?.shutdown()
            return
        }

        isInitializing = false
        val engine = createdEngine ?: tts
        if (status == TextToSpeech.SUCCESS) {
            if (engine == null) {
                recordInitFailure(
                    message = "TTS init callback completed but engine was never assigned",
                    data = mapOf("status" to status),
                    engine = null,
                )
                return
            }

            configureEngine(engine)
        } else {
            recordInitFailure(
                message = "TTS initialization failed",
                data = mapOf("status" to status),
                engine = engine,
            )
        }
    }

    private fun configureEngine(engine: TextToSpeech) {
        clearInitTimeout()
        isInitializing = false
        tts = engine
        val selectedLocale = selectSupportedLocale(engine)
        if (selectedLocale == null) {
            recordInitFailure(
                message = "No supported TTS language found",
                data = mapOf(
                    "locale" to Locale.getDefault().toLanguageTag(),
                    "defaultEngine" to (engine.defaultEngine ?: "unknown"),
                ),
                engine = engine,
            )
            return
        }

        val languageResult = engine.setLanguage(selectedLocale)
        if (!isLanguageResultSupported(languageResult)) {
            recordInitFailure(
                message = "TTS language selection failed",
                data = mapOf(
                    "locale" to selectedLocale.toLanguageTag(),
                    "result" to languageResult,
                    "defaultEngine" to (engine.defaultEngine ?: "unknown"),
                ),
                engine = engine,
            )
            return
        }

        engine.setSpeechRate(1.0f)
        engine.setOnUtteranceProgressListener(createUtteranceListener())
        _isReady = true
        initFailureCount = 0
        nextInitRetryAtMs = 0L
        logger.info(
            TAG,
            "TTS initialized",
            mapOf(
                "locale" to selectedLocale.toLanguageTag(),
                "languageResult" to languageResult,
                "defaultEngine" to (engine.defaultEngine ?: "unknown"),
            )
        )
        notifyReadyCallbacks()
        speakPendingSpeechIfFresh()
    }

    private fun recordInitFailure(
        message: String,
        data: Map<String, Any>,
        engine: TextToSpeech?,
    ) {
        clearInitTimeout()
        initAttempt++
        isInitializing = false
        _isReady = false
        onReadyCallbacks.clear()

        if (engine != null) {
            if (tts === engine) {
                tts = null
            }
            engine.shutdown()
        } else {
            tts?.shutdown()
            tts = null
        }

        initFailureCount++
        val retryDelayMs = retryDelayForFailure(initFailureCount)
        nextInitRetryAtMs = SystemClock.elapsedRealtime() + retryDelayMs
        val automaticRetriesPaused = initFailureCount >= MAX_AUTOMATIC_INIT_FAILURES

        logger.warning(
            TAG,
            message,
            data + mapOf(
                "failureCount" to initFailureCount,
                "retryDelayMs" to retryDelayMs,
                "automaticRetriesPaused" to automaticRetriesPaused,
            )
        )

        if (automaticRetriesPaused) {
            clearPendingSpeech("automatic retry limit reached")
        }
    }

    private fun retryDelayForFailure(failureCount: Int): Long {
        val shift = (failureCount - 1).coerceIn(0, 5)
        return minOf(MAX_INIT_RETRY_DELAY_MS, INITIAL_INIT_RETRY_DELAY_MS * (1L shl shift))
    }

    private fun storePendingSpeech(event: SceneEvent) {
        val now = SystemClock.elapsedRealtime()
        val pending = pendingSpeech
        if (
            pending == null ||
            now - pending.enqueuedAtMs > PENDING_SPEECH_MAX_AGE_MS ||
            event.priority.value >= pending.event.priority.value
        ) {
            pendingSpeech = PendingSpeech(event = event, enqueuedAtMs = now)
        }
    }

    private fun speakPendingSpeechIfFresh() {
        val pending = pendingSpeech ?: return
        pendingSpeech = null

        val ageMs = SystemClock.elapsedRealtime() - pending.enqueuedAtMs
        if (ageMs > PENDING_SPEECH_MAX_AGE_MS) {
            logger.warning(
                TAG,
                "Dropping stale pending TTS event",
                mapOf(
                    "messageKey" to pending.event.messageKey,
                    "ageMs" to ageMs,
                )
            )
            return
        }

        speakReadyEvent(pending.event)
    }

    private fun clearPendingSpeech(reason: String) {
        val pending = pendingSpeech ?: return
        pendingSpeech = null
        logger.warning(
            TAG,
            "Dropping pending TTS event",
            mapOf(
                "messageKey" to pending.event.messageKey,
                "reason" to reason,
            )
        )
    }

    private fun notifyReadyCallbacks() {
        val callbacks = onReadyCallbacks.toList()
        onReadyCallbacks.clear()
        callbacks.forEach { it.invoke() }
    }

    private fun selectSupportedLocale(engine: TextToSpeech): Locale? {
        return languageCandidates().firstOrNull { locale ->
            isLanguageResultSupported(engine.isLanguageAvailable(locale))
        }
    }

    private fun languageCandidates(): List<Locale> {
        return listOf(
            Locale.getDefault(),
            Locale.SIMPLIFIED_CHINESE,
            Locale.CHINESE,
        ).distinctBy { it.toLanguageTag() }
    }

    private fun isLanguageResultSupported(result: Int): Boolean {
        return result >= TextToSpeech.LANG_AVAILABLE
    }

    private fun createUtteranceListener(): UtteranceProgressListener {
        return object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                logger.debug(TAG, "TTS utterance started", mapOf("utteranceId" to (utteranceId ?: "unknown")))
            }

            override fun onDone(utteranceId: String?) {
                logger.debug(TAG, "TTS utterance completed", mapOf("utteranceId" to (utteranceId ?: "unknown")))
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                logger.warning(TAG, "TTS utterance failed", mapOf("utteranceId" to (utteranceId ?: "unknown")))
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                logger.warning(
                    TAG,
                    "TTS utterance failed",
                    mapOf(
                        "utteranceId" to (utteranceId ?: "unknown"),
                        "errorCode" to errorCode,
                    )
                )
            }
        }
    }

    @Suppress("DiscouragedApi")
    private fun resolveMessage(event: SceneEvent): String {
        val resId = context.resources.getIdentifier(event.messageKey, "string", context.packageName)
        if (resId == 0) return event.messageKey

        if (event.messageParams.isEmpty()) {
            return context.getString(resId)
        }

        val args = event.messageParams.toSortedMap().values.toTypedArray()
        return try {
            context.getString(resId, *args)
        } catch (_: IllegalFormatException) {
            context.getString(resId)
        }
    }

    private fun runOnMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            mainHandler.post(block)
        }
    }

    private data class PendingSpeech(
        val event: SceneEvent,
        val enqueuedAtMs: Long,
    )

    private companion object {
        private const val TAG = "SpeechManager"
        private const val INIT_TIMEOUT_MS = 5_000L
        private const val INITIAL_INIT_RETRY_DELAY_MS = 1_000L
        private const val MAX_INIT_RETRY_DELAY_MS = 30_000L
        private const val MAX_AUTOMATIC_INIT_FAILURES = 5
        private const val PENDING_SPEECH_MAX_AGE_MS = 3_000L
    }
}
