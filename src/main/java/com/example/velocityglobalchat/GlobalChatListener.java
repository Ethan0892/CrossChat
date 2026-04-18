package com.example.velocityglobalchat;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.regex.Pattern;

/**
 * Listens for chat events on the proxy and broadcasts a formatted message
 * to all players on participating servers, cancelling the original event so
 * the message isn't echoed locally by the backend server.
 */
public class GlobalChatListener {

    /**
     * Detects common MiniMessage tag patterns such as {@code <red>}, {@code </bold>},
     * {@code <#ff0000>}, {@code <!italic>}, {@code <gradient:red:blue>}.
     * Used to auto-select the correct Adventure deserializer.
     */
    private static final Pattern MINI_MESSAGE_TAG =
            Pattern.compile("<[!/]?[a-zA-Z#][a-zA-Z0-9_:#.-]*(?::[^>]*)?>", Pattern.CASE_INSENSITIVE);

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.legacyAmpersand();

    private final ProxyServer server;
    private final Config config;
    private final LuckPermsHook luckPerms;

    public GlobalChatListener(ProxyServer server, Config config, LuckPermsHook luckPerms) {
        this.server   = server;
        this.config   = config;
        this.luckPerms = luckPerms;
    }

    // -------------------------------------------------------------------------
    // Event handler
    // -------------------------------------------------------------------------

    @Subscribe
    public void onPlayerChat(PlayerChatEvent event) {
        if (!config.isEnabled()) return;

        Player player = event.getPlayer();

        // Resolve current backend server
        String serverName = player.getCurrentServer()
                .map(conn -> conn.getServerInfo().getName())
                .orElse(null);

        if (serverName == null) return;

        // Check sender's server is included in the configured list
        if (!config.getServers().isEmpty()
                && !config.getServers().contains(serverName.toLowerCase())) {
            return;
        }

        String prefix      = luckPerms.getPrefix(player);
        String message     = event.getMessage();
        String displayName = config.getDisplayName(serverName);

        Component formatted = buildComponent(displayName, player.getUsername(), prefix, message);

        broadcast(formatted);

        // Deny the event so the message is NOT forwarded to the backend server,
        // which prevents the player's own server from also displaying it.
        event.setResult(PlayerChatEvent.ChatResult.denied());
    }

    // -------------------------------------------------------------------------
    // Formatting
    // -------------------------------------------------------------------------

    /**
     * Builds the final Adventure Component.
     *
     * <p>The format string is split at {@code {message}} so the player's message
     * content is always inserted as a plain-text Component — this prevents
     * players from injecting colour codes or MiniMessage tags into the chat
     * format and impersonating prefixes or formatting other players' names.</p>
     */
    private Component buildComponent(String serverName, String playerName,
                                     String prefix, String message) {
        String format = config.getFormat();

        // Split at the {message} placeholder (max 2 parts)
        String[] parts = format.split("\\{message\\}", 2);

        String prefixPart = applyPlaceholders(parts[0], serverName, playerName, prefix);
        String suffixPart = parts.length > 1
                ? applyPlaceholders(parts[1], serverName, playerName, prefix)
                : "";

        Component prefixComp  = deserialize(prefixPart);
        Component messageComp = Component.text(message);   // always plain — no injection
        Component suffixComp  = suffixPart.isEmpty()
                ? Component.empty()
                : deserialize(suffixPart);

        return prefixComp.append(messageComp).append(suffixComp);
    }

    private static String applyPlaceholders(String text, String serverName,
                                            String playerName, String prefix) {
        return text
                .replace("{server}",  serverName)
                .replace("{player}",  playerName)
                .replace("{prefix}",  prefix);
    }

    /**
     * Auto-detects the format used in the string and returns the appropriate
     * Adventure Component:
     * <ul>
     *   <li>MiniMessage — if the text contains {@code <tag>} patterns</li>
     *   <li>Legacy {@code &} codes — otherwise (the default config style)</li>
     * </ul>
     */
    private static Component deserialize(String text) {
        if (text.isEmpty()) return Component.empty();
        if (MINI_MESSAGE_TAG.matcher(text).find()) {
            return MINI_MESSAGE.deserialize(text);
        }
        return LEGACY.deserialize(text);
    }

    // -------------------------------------------------------------------------
    // Broadcasting
    // -------------------------------------------------------------------------

    /**
     * Sends the component to every player whose current server is included in
     * the configured {@code servers} list (or to all players if the list is empty).
     */
    private void broadcast(Component component) {
        for (Player p : server.getAllPlayers()) {
            if (config.getServers().isEmpty()) {
                p.sendMessage(component);
            } else {
                p.getCurrentServer().ifPresent(conn -> {
                    if (config.getServers().contains(
                            conn.getServerInfo().getName().toLowerCase())) {
                        p.sendMessage(component);
                    }
                });
            }
        }
    }
}
