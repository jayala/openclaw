package ai.openclaw.app.voice

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper
import android.util.Log

/** Short non-speech cues for the wake-word command lifecycle. */
internal enum class VoiceWakeEarcon {
  /** The command was heard and is being sent to the Gateway. */
  Acknowledged,

  /** The command could not be sent, or its run failed, timed out, or produced nothing to speak. */
  Failed,
}

internal fun interface VoiceWakeEarconPlayer {
  fun play(earcon: VoiceWakeEarcon)
}

/**
 * Plays earcons with the platform tone generator, so no audio assets ship with the app.
 *
 * The acknowledgement stays near 120 ms because fast replies (for example a Home Assistant
 * on/off answered by a Gateway plugin) start speaking well under a second later.
 */
internal class ToneVoiceWakeEarconPlayer : VoiceWakeEarconPlayer {
  private val mainHandler = Handler(Looper.getMainLooper())

  override fun play(earcon: VoiceWakeEarcon) {
    val (tone, durationMs) =
      when (earcon) {
        VoiceWakeEarcon.Acknowledged -> ToneGenerator.TONE_PROP_BEEP to ACK_DURATION_MS
        VoiceWakeEarcon.Failed -> ToneGenerator.TONE_PROP_NACK to FAILED_DURATION_MS
      }
    val generator =
      try {
        ToneGenerator(AudioManager.STREAM_MUSIC, TONE_VOLUME)
      } catch (err: RuntimeException) {
        // The tone generator is unavailable while another client holds exclusive audio.
        Log.w(TAG, "earcon unavailable: ${err.message ?: err::class.java.simpleName}")
        return
      }
    if (!generator.startTone(tone, durationMs)) {
      generator.release()
      return
    }
    mainHandler.postDelayed({ generator.release() }, durationMs + RELEASE_MARGIN_MS)
  }

  private companion object {
    const val TAG = "VoiceWake"
    const val TONE_VOLUME = 70
    const val ACK_DURATION_MS = 120
    const val FAILED_DURATION_MS = 400
    const val RELEASE_MARGIN_MS = 100L
  }
}
