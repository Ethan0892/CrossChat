package com.example.velocityglobalchat;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Plugin(
        id = "velocityglobalchat",
        name = "VelocityGlobalChat",
        version = "1.1.0",
        description = "Global cross-server chat for Velocity",
        authors = {"Ethan0892"}
)
public class VelocityGlobalChat {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;

    private Config config;

    @Inject
    public VelocityGlobalChat(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        try {
            Files.createDirectories(dataDirectory);
            config = new Config(dataDirectory.resolve("config.yml"));
            logger.info("Config loaded. Format: {}", config.getFormat());
        } catch (IOException e) {
            logger.error("Failed to load config — plugin functionality disabled: {}", e.getMessage());
            return;
        }

        LuckPermsHook luckPermsHook = new LuckPermsHook(server, logger);

        // Register the outgoing plugin-message channel used to signal backends
        // to suppress the unsigned echo of the global chat message.
        server.getChannelRegistrar().register(GlobalChatListener.SUPPRESS_CHANNEL);

        server.getEventManager().register(this, new GlobalChatListener(server, config, luckPermsHook));

        logger.info("VelocityGlobalChat enabled.");
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        logger.info("VelocityGlobalChat disabled.");
    }
}
