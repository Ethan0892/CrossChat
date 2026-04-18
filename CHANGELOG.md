# Changelog

All notable changes to VelocityGlobalChat will be documented in this file.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).
Versioning follows [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

_Nothing yet._

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

[Unreleased]: https://github.com/Ethan0892/CrossChat/compare/v1.1.0...HEAD
[1.1.0]: https://github.com/Ethan0892/CrossChat/compare/v1.0.0...v1.1.0
[1.0.0]: https://github.com/Ethan0892/CrossChat/releases/tag/v1.0.0
