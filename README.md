# VelocityGlobalChat

A Velocity 3.x proxy plugin that routes chat from every backend server into a single global channel, visible to all connected players in real time.

---

## Features

- **Cross-server chat** — messages sent on any backend server are broadcast to all players on all (configured) servers
- **Fully customisable format** — edit `config.yml` with your own colour codes and placeholders
- **`&` legacy codes and MiniMessage** — auto-detected; use whichever style you prefer
- **LuckPerms integration** — optional; prefixes are pulled automatically if LuckPerms is installed on the proxy
- **Server filter** — choose which servers participate; exclude lobby-only or mini-game servers
- **Toggle on/off** without restarting — flip `enabled: false` and restart the proxy

---

## Requirements

| Requirement | Version |
|---|---|
| Java | 17 or later |
| Velocity | 3.x (tested against 3.4.0) |
| LuckPerms *(optional)* | 5.x |

---

## Installation

1. Download `velocityglobalchat-1.0.0.jar` from [Releases](../../releases)
2. Drop it into your Velocity `plugins/` folder
3. Restart the proxy — `plugins/velocityglobalchat/config.yml` is created automatically
4. Edit the config to taste, then restart again

---

## Building from source

Requirements: **Java 17+**, **Maven 3.6+**

```bash
git clone https://github.com/Ethan0892/CrossChat.git
cd CrossChat
mvn clean package
# → target/velocityglobalchat-1.0.0.jar
```

---

## Configuration

`plugins/velocityglobalchat/config.yml`

```yaml
# Enable or disable the plugin entirely
enabled: true

# Chat format — placeholders:
#   {server}  — backend server name (e.g. HUB, SURVIVAL)
#   {player}  — sender's Minecraft username
#   {prefix}  — LuckPerms prefix (empty if LuckPerms is absent)
#   {message} — the chat message (always plain text — injection-safe)
#
# Use & colour codes OR MiniMessage tags — do not mix both.
format: "&8[&b{server}&8] &7{prefix}{player}&f: {message}"

# Servers that participate in global chat.
# Players on servers NOT listed here neither see nor send global messages.
# Leave the list empty to include ALL servers.
servers:
  - hub
  - survival
  - farming
```

### Format examples

| Style | Example |
|---|---|
| Legacy `&` codes | `&8[&b{server}&8] &7{prefix}{player}&f: {message}` |
| MiniMessage | `<dark_gray>[<aqua>{server}</aqua>]</dark_gray> <gray>{prefix}{player}</gray><white>: {message}</white>` |

> **Note:** Player message content is always inserted as plain text regardless of the format style, preventing colour-code injection by players.

---

## How it works

1. `PlayerChatEvent` fires on the proxy when a player sends a chat message
2. The handler resolves the player's current backend server via `player.getCurrentServer()`
3. The format string is rendered using the Adventure API (legacy or MiniMessage)
4. The component is broadcast to all players whose current server is in the configured `servers` list
5. The original event is **denied** — the backend server never receives the message, so it is not echoed locally

---

## Project structure

```
src/main/java/com/example/velocityglobalchat/
├── VelocityGlobalChat.java    — @Plugin entry point, initialisation
├── Config.java                — config.yml loader/writer
├── GlobalChatListener.java    — PlayerChatEvent handler, formatting, broadcast
└── LuckPermsHook.java         — optional LuckPerms prefix integration
src/main/resources/
└── config.yml                 — default config (documentation copy)
pom.xml
```

---

## Licence

MIT — see [LICENSE](LICENSE)
