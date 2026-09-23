package ai.openclaw.app.voice

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Follows the agent run that answers a wake-word command so its final reply can be spoken.
 *
 * The Gateway assigns the run id for a `voice.transcript` node event, so the node cannot know it up
 * front. The tracker adopts the first chat run seen on the wake session after arming and reports the
 * assistant text once that run reaches its final state, or a failure when the run errors, aborts,
 * ends without text, or never finishes within [timeoutMs].
 */
internal class VoiceWakeReplyTracker(
  private val timeoutMs: Long = DEFAULT_TIMEOUT_MS,
) {
  private class Armed(
    val token: Long,
    val sessionKey: String,
    val armedAtMs: Long,
  ) {
    var runId: String? = null
  }

  private val lock = Any()
  private var armed: Armed? = null
  private var nextToken = 0L

  /**
   * Starts waiting for the reply to a command just dispatched on [sessionKey]. Returns a token for
   * [expire], so a timer from an earlier command cannot fail a newer one.
   */
  fun arm(
    sessionKey: String,
    nowMs: Long,
  ): Long =
    synchronized(lock) {
      nextToken += 1
      armed = Armed(token = nextToken, sessionKey = sessionKey, armedAtMs = nowMs)
      nextToken
    }

  /** Stops tracking the command armed with [token] if it is still unanswered; true means it timed out. */
  fun expire(token: Long): Boolean =
    synchronized(lock) {
      if (armed?.token != token) return false
      armed = null
      true
    }

  fun clear() {
    synchronized(lock) { armed = null }
  }

  /** Returns the outcome when [payload] ends the tracked run, otherwise null. */
  fun onChatEvent(
    payload: JsonObject,
    nowMs: Long,
  ): VoiceWakeReplyOutcome? =
    synchronized(lock) {
      val current = armed ?: return null
      if (nowMs - current.armedAtMs > timeoutMs) {
        armed = null
        return VoiceWakeReplyOutcome.Failed
      }
      val sessionKey = payload["sessionKey"].asStringOrNull()
      if (sessionKey != null && sessionKey != current.sessionKey) return null
      val runId = payload["runId"].asStringOrNull() ?: return null
      val state = payload["state"].asStringOrNull() ?: return null
      if (current.runId == null) current.runId = runId
      if (current.runId != runId) return null
      when (state) {
        "final" -> {
          armed = null
          ChatEventText
            .assistantTextFromPayload(payload)
            ?.takeIf { it.isNotBlank() }
            ?.let(VoiceWakeReplyOutcome::Reply)
            ?: VoiceWakeReplyOutcome.Failed
        }

        "aborted", "error" -> {
          armed = null
          VoiceWakeReplyOutcome.Failed
        }

        else -> {
          null
        }
      }
    }

  companion object {
    const val DEFAULT_TIMEOUT_MS = 90_000L
  }
}

internal sealed interface VoiceWakeReplyOutcome {
  data class Reply(
    val text: String,
  ) : VoiceWakeReplyOutcome

  data object Failed : VoiceWakeReplyOutcome
}

private fun JsonElement?.asStringOrNull(): String? = (this as? JsonPrimitive)?.takeIf { it.isString }?.content
