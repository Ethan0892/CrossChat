package com.example.crosschatbackend;

import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Set;
import java.util.UUID;

/**
 * Cancels the local-chat display of a message that VelocityGlobalChat has
 * already broadcast globally from the proxy.
 *
 * <p>The proxy plugin strips the chat signature (so Velocity can handle it
 * without disconnecting the player) and sends a {@code crosschat:suppress}
 * plugin message containing the player's UUID.  This listener consumes that
 * one-shot suppression flag when the unsigned echo arrives from the proxy.</p>
 *
 * <p>Priority is {@link EventPriority#LOWEST} so that if no suppression flag
 * exists (e.g. the message was sent locally without going through the proxy)
 * other plugins still see the event at their normal priorities.</p>
 */
public class SuppressListener implements Listener {

    private final Set<UUID> pendingSuppressions;

    public SuppressListener(Set<UUID> pendingSuppressions) {
        this.pendingSuppressions = pendingSuppressions;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        // remove() returns true only if the UUID was present — atomic one-shot consume
        if (pendingSuppressions.remove(uuid)) {
            event.setCancelled(true);
        }
    }
}
