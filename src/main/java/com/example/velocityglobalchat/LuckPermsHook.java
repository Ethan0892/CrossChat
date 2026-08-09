package com.example.velocityglobalchat;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import org.slf4j.Logger;

/**
 * Optional integration with LuckPerms to resolve per-player chat metadata.
 *
 * <p>If LuckPerms is not installed on the proxy the hook is a no-op and
 * {@link #getPrefix(Player)}, {@link #getSuffix(Player)}, and
 * {@link #getGroup(Player)} always return an empty string.</p>
 */
public class LuckPermsHook {

    private LuckPerms luckPerms = null;

    public LuckPermsHook(ProxyServer server, Logger logger) {
        if (server.getPluginManager().getPlugin("luckperms").isPresent()) {
            try {
                // LuckPermsProvider.get() is the cross-platform static accessor.
                // It throws IllegalStateException if the API is not yet available.
                luckPerms = LuckPermsProvider.get();
                logger.info("LuckPerms integration enabled — chat metadata will be shown.");
            } catch (IllegalStateException e) {
                logger.warn("LuckPerms detected but provider not available ({}). "
                        + "LuckPerms placeholders will be empty.", e.getMessage());
            }
        } else {
            logger.info("LuckPerms not found — LuckPerms placeholders will be empty.");
        }
    }

    /**
     * Returns the LuckPerms prefix for the given player, or an empty string
     * if LuckPerms is unavailable or the user has no prefix set.
     *
     * <p>The prefix is returned in its raw form (e.g. {@code &c[Admin] }) so
     * that it is subsequently processed by the same deserializer as the rest
     * of the format string.</p>
     */
    public String getPrefix(Player player) {
        return getMetaValue(player, "prefix");
    }

    public String getSuffix(Player player) {
        return getMetaValue(player, "suffix");
    }

    public void setResolvedSuffix(Player player, String suffix) {
        resolvedSuffixes.put(player.getUniqueId(), suffix != null ? suffix : "");
    }

    public void clearResolvedSuffix(java.util.UUID uuid) {
        resolvedSuffixes.remove(uuid);
    }

    public String getGroup(Player player) {
        if (luckPerms == null) return "";

        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        return user != null && user.getPrimaryGroup() != null ? user.getPrimaryGroup() : "";
    }

    private String getMetaValue(Player player, String type) {
        if (luckPerms == null) return "";

        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user == null) return "";

        String value = "prefix".equals(type)
                ? user.getCachedData().getMetaData().getPrefix()
                : user.getCachedData().getMetaData().getSuffix();
        if ("suffix".equals(type)) {
            String resolved = resolvedSuffixes.get(player.getUniqueId());
            if (resolved != null) return resolved;
        }
        return value != null ? value : "";
    }

    private final java.util.Map<java.util.UUID, String> resolvedSuffixes =
            new java.util.concurrent.ConcurrentHashMap<>();
}
