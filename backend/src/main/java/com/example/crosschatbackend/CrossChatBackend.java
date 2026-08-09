package com.example.crosschatbackend;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import me.clip.placeholderapi.PlaceholderAPI;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

/**
 * Companion backend plugin for VelocityGlobalChat.
 *
 * <p>When the Velocity proxy broadcasts a global chat message it also sends a
 * plugin message ({@code crosschat:suppress}) to the backend server containing
 * the sender's UUID.  This plugin listens for those messages and marks the UUID
 * for suppression so that when Paper processes the forwarded unsigned chat
 * packet the local display is silently cancelled.</p>
 *
 * <h3>Required backend configuration</h3>
 * In {@code config/paper-global.yml} set:
 * <pre>
 * proxies:
 *   velocity:
 *     enabled: true
 *     ...
 * unsupported-settings:
 *   allow-permanent-block-break-exploits: false   # unrelated, leave default
 * misc:
 *   chat-threads:
 *     ...
 * </pre>
 * Also ensure {@code enforce-secure-profiles: false} is set so that the
 * unsigned (signature-stripped) message forwarded by Velocity is accepted:
 * <pre>
 * # paper-global.yml
 * proxies:
 *   velocity:
 *     enabled: true
 *     enforce-secure-profiles: false
 * </pre>
 *
 * Without {@code enforce-secure-profiles: false}, Paper will reject the
 * unsigned forwarded message and players will see a "Chat message delivery
 * failed" warning — the global broadcast still works, but the backend won't
 * echo the message locally regardless.
 */
public class CrossChatBackend extends JavaPlugin {

    /**
     * Channel name must match {@code GlobalChatListener.SUPPRESS_CHANNEL} in
     * the Velocity proxy plugin exactly.
     */
    private static final String CHANNEL = "crosschat:suppress";
    private static final String METADATA_CHANNEL = "crosschat:metadata";

    /**
     * UUIDs of players whose next incoming chat event should be suppressed.
     * Entries are added by the plugin-message handler and removed (consumed)
     * by the chat listener on first use — each suppression is one-shot.
     */
    final Set<UUID> pendingSuppressions = ConcurrentHashMap.newKeySet();

    @Override
    public void onEnable() {
        // Register the incoming plugin-message channel
        getServer().getMessenger().registerIncomingPluginChannel(
                this, CHANNEL, this::handleSuppressMessage);
        getServer().getMessenger().registerOutgoingPluginChannel(this, METADATA_CHANNEL);

        // Register the AsyncChatEvent listener
        getServer().getPluginManager().registerEvents(
                new SuppressListener(pendingSuppressions), this);

        getServer().getScheduler().runTaskTimer(this, this::publishMetadata, 20L, 20L);

        getLogger().info("CrossChatBackend enabled — listening on channel: " + CHANNEL);
    }

    private void publishMetadata() {
        LuckPerms luckPerms = getServer().getPluginManager().isPluginEnabled("LuckPerms")
                ? LuckPermsProvider.get() : null;
        boolean placeholderApiEnabled =
                getServer().getPluginManager().isPluginEnabled("PlaceholderAPI");

        for (Player player : Bukkit.getOnlinePlayers()) {
            String suffix = "";
            if (luckPerms != null) {
                User user = luckPerms.getUserManager().getUser(player.getUniqueId());
                if (user != null && user.getCachedData().getMetaData().getSuffix() != null) {
                    suffix = user.getCachedData().getMetaData().getSuffix();
                }
            }
            if (placeholderApiEnabled) {
                suffix = PlaceholderAPI.setPlaceholders(player, suffix);
            }

            try {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                DataOutputStream output = new DataOutputStream(bytes);
                output.writeLong(player.getUniqueId().getMostSignificantBits());
                output.writeLong(player.getUniqueId().getLeastSignificantBits());
                output.writeUTF(suffix);
                player.sendPluginMessage(this, METADATA_CHANNEL, bytes.toByteArray());
            } catch (Exception e) {
                getLogger().warning("Failed to publish metadata for " + player.getName());
            }
        }
    }

    @Override
    public void onDisable() {
        getServer().getMessenger().unregisterIncomingPluginChannel(this, CHANNEL);
        getLogger().info("CrossChatBackend disabled.");
    }

    // -------------------------------------------------------------------------
    // Plugin-message handler
    // -------------------------------------------------------------------------

    private void handleSuppressMessage(String channel, org.bukkit.entity.Player player,
                                       byte[] bytes) {
        if (bytes.length < 16) {
            getLogger().warning(
                    "Received malformed suppress message (expected 16 bytes, got "
                    + bytes.length + ")");
            return;
        }

        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));
            UUID uuid = new UUID(in.readLong(), in.readLong());
            pendingSuppressions.add(uuid);
        } catch (Exception e) {
            getLogger().warning("Failed to read suppress message: " + e.getMessage());
        }
    }
}
