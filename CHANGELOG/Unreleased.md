## Unreleased

### Fixes

- Android: keep wake-word listening active after the app leaves the screen by holding the microphone foreground-service type while Voice Wake is enabled, stream the microphone to the on-device recognizer so one session stays open without the service's start/stop tones or silence timeouts, listen in the Gateway's `talk.speechLocale`, and speak the final reply to a wake-word command through the Talk voice instead of showing text only.
- Codex: restore background memory narratives and isolated text completions on agent-scoped local runtimes with administrator-managed hooks, preserving managed hooks and existing native-account/proxy routing while keeping ordinary hooks and model tools isolated. (#151658)
- Sandboxes: honor each registered runtime owner's pruning policy so a stricter agent cannot evict another agent's containers or browser bridges.

### Changes

- Android: add **Settings → Voice → Wake Word Agent** to send wake-word commands to a dedicated Gateway agent in a device-scoped session, so spoken requests can run on a faster model than Chat while the reply is still spoken on the phone.
- Android: play a short tone when a wake-word command is sent and a distinct low tone when it cannot be sent, its run fails, aborts, or ends without text, or no answer arrives within 90 seconds; toggle it under **Settings → Voice → Play command sounds**.
- Messaging: allow cross-provider sends and other guarded message actions by default, including WebChat-to-Discord notifications. Existing configurations that omit `tools.message.crossContext.allowAcrossProviders` adopt the new default on upgrade; explicit `false` remains enforced globally and per agent. Set `allowAcrossProviders: false` to retain provider isolation, or both it and `allowWithinProvider: false` to restrict guarded actions to the current bound conversation. See [security guidance](https://docs.openclaw.ai/gateway/security/tool-permissions#cross-provider-messaging). (#149875)
