# Changelog

All notable changes to VelocityGlobalChat will be documented in this file.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).
Versioning follows [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.3.2] — 2026-06-26

### Fixed
- **Sender no longer sees their own message** — the 1.3.1 workaround that excluded the sender from the broadcast was too aggressive; it was only needed because the client-side optimistic render doubled the message for the sender without [SignedVelocity](https://modrinth.com/plugin/signedvelocity) installed
  - With SignedVelocity on the proxy, Velocity properly completes the Minecraft 1.19+ chat-signing handshake so `ChatResult.denied()` fully suppresses the client-side render — no double message occurs
  - The sender exclusion has been removed; all eligible players including the sender now receive the formatted broadcast exactly once

### Notes
- **[SignedVelocity](https://modrinth.com/plugin/signedvelocity) must be installed on the proxy** (not the backend). This is required for `ChatResult.denied()` to work correctly with signed chat on Velocity 3.x / Minecraft 1.19.3+. Installing it on backend servers is not needed and may cause issues.

---

## [1.3.1]

### Fixed
- **Double-message for the sender** — in Minecraft 1.19.3+ the client renders the sender's own chat message locally the instant they press Enter (client-side optimistic display), before the server processes the packet; the proxy's formatted broadcast then arrived on top of that local render, producing two visible messages for the sender only
  - Fix: the sender is now excluded from the `broadcast()` loop — they see their message exactly once via the client-side render; all other players continue to receive the formatted broadcast as normal

---

## [1.3.0]

### Fixed
- **Double-message bug** — players were seeing every global-chat message twice (once from the proxy broadcast and once echoed by the backend server)
  - Root cause: the `ChatResult.message()` + `crosschat:suppress` plugin-message approach introduced in 1.2.0 had a race condition on Velocity 3.5.0-SNAPSHOT; the suppress signal could arrive at the backend *after* the chat packet, causing `SuppressListener` to miss the event and Paper to echo the message locally
  - Fix: replaced `ChatResult.message()` with `ChatResult.denied()` — Velocity drops the packet entirely so the backend never receives it and cannot echo it
  - `ChatResult.denied()` is safe on Velocity 3.3+ / Minecraft 1.19.3+; the 1.19.1–1.19.2 strict-signing concern that motivated the 1.2.0 workaround no longer applies to modern server builds

### Removed
- **`crosschat:suppress` plugin-message channel** — no longer sent
- **`CrossChatBackend` companion plugin** is no longer required; it can be removed from backend servers (leaving it installed is harmless but it will never receive a suppress signal)

---

## [1.2.0] — 2026-04-18

### Fixed
- **Critical: players kicked with "A proxy plugin caused an illegal protocol state"** on Minecraft 1.19.1+
  - Root cause: `ChatResult.denied()` cannot cancel a cryptographically-signed chat message at the proxy level (Velocity/Minecraft protocol restriction introduced in 1.19.1)
  - Fix: replaced `ChatResult.denied()` with `ChatResult.message(text)` which strips the signature (making the message "unsigned") instead of attempting to cancel it — this prevents the disconnect entirely

### Added
- **`crosschat:suppress` plugin-message channel** — the proxy sends the sender's UUID to the backend over this channel immediately before forwarding the unsigned echo; the companion backend plugin uses this to silently cancel local display
- **`CrossChatBackend` companion Paper plugin** (`backend/`) — drop into each Paper/Purpur backend's `plugins/` directory to suppress the local-chat echo of messages already broadcast globally:
  - Listens on `crosschat:suppress`
  - Cancels `AsyncChatEvent` for the flagged player via a one-shot `ConcurrentHashMap` set
  - Compatible with Paper 1.19.1+, Java 17+
  - If the backend plugin is absent, global broadcast still works but the message will also appear in local backend chat

### Notes
- In `config/paper-global.yml` on each backend, ensure `enforce-secure-profiles: false` so that the signature-stripped message forwarded by Velocity is accepted rather than rejected with a "Chat message delivery failed" system message

---

## [1.1.0] — 2026-04-18

### Changed
- `servers` config block now uses a **key-value format** (`servername: "Display Text"`) instead of a plain list
  - The value is substituted for `{server}` in the format string, allowing per-server display names with colours/formatting (e.g. `hub: "&bHUB"`, `survival: "&a&lSURVIVAL"`)
  - Backwards-compatible: the old `  - name` list form is still accepted and defaults the display name to the uppercased server name
- `Config` internally changed from `Set<String>` to `Map<String, String>` for server entries; added `getDisplayName(String)` accessor
- If a server is included via the empty-list (all-servers) mode, `{server}` falls back to the raw velocity server name uppercased

---

## [1.0.0] — 2026-04-18

### Added
- Initial release of VelocityGlobalChat
- **Cross-server chat broadcast** — `PlayerChatEvent` intercepted at the proxy; formatted message sent to all eligible players across all backend servers
- **`config.yml`** auto-generated on first run with sensible defaults:
  - `enabled` toggle
  - `format` string with `{server}`, `{player}`, `{prefix}`, `{message}` placeholders
  - `servers` list to filter which backend servers participate in global chat
- **Dual format support** — `&` legacy colour codes and MiniMessage tags both supported; auto-detected per format string so neither style needs to be configured explicitly
- **Injection-safe messages** — player message content is always inserted as a plain-text `Component`, preventing colour-code or MiniMessage tag injection
- **LuckPerms integration** — optional; if LuckPerms is present on the proxy, the player's prefix is fetched from `CachedMetaData` and exposed as `{prefix}`; gracefully absent if LuckPerms is not installed
- **Server-scoped broadcast** — recipients are filtered so only players on servers in the configured `servers` list receive global messages; empty list means all servers
- **`ProxyInitializeEvent` / `ProxyShutdownEvent`** hooks for clean startup and shutdown logging
- Maven build with Velocity annotation-processor-generated `velocity-plugin.json`
- Compatible with Java 17+ and Velocity 3.x

[Unreleased]: https://github.com/Ethan0892/CrossChat/compare/v1.3.2...HEAD
[1.3.2]: https://github.com/Ethan0892/CrossChat/compare/v1.3.1...v1.3.2
[1.3.1]: https://github.com/Ethan0892/CrossChat/compare/v1.3.0...v1.3.1
[1.3.0]: https://github.com/Ethan0892/CrossChat/compare/v1.2.0...v1.3.0
[1.2.0]: https://github.com/Ethan0892/CrossChat/compare/v1.1.0...v1.2.0
[1.1.0]: https://github.com/Ethan0892/CrossChat/compare/v1.0.0...v1.1.0
[1.0.0]: https://github.com/Ethan0892/CrossChat/releases/tag/v1.0.0
