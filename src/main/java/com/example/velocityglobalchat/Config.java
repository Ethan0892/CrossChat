package com.example.velocityglobalchat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Minimal YAML-like config parser. Supports:
 *   format: <string>         — may optionally be wrapped in single or double quotes
 *   enabled: true|false
 *   servers:
 *     - <name>
 *     - <name>
 */
public class Config {

    private static final String DEFAULT_FORMAT =
            "&8[&b{server}&8] &7{prefix}{player}&f: {message}";

    private String format  = DEFAULT_FORMAT;
    private boolean enabled = true;
    private final Set<String> servers = new LinkedHashSet<>();

    public Config(Path configPath) throws IOException {
        if (Files.exists(configPath)) {
            load(Files.readAllLines(configPath));
        } else {
            writeDefaults(configPath);
        }
    }

    // -------------------------------------------------------------------------
    // Parsing
    // -------------------------------------------------------------------------

    private void load(List<String> lines) {
        boolean inServersList = false;

        for (String raw : lines) {
            // Skip blank lines and comments
            String line = raw;
            if (line.isBlank() || line.stripLeading().startsWith("#")) continue;

            if (line.startsWith("format:")) {
                format = stripQuotes(line.substring("format:".length()).trim());
                inServersList = false;

            } else if (line.startsWith("enabled:")) {
                enabled = Boolean.parseBoolean(
                        stripQuotes(line.substring("enabled:".length()).trim()));
                inServersList = false;

            } else if (line.startsWith("servers:")) {
                inServersList = true;

            } else if (inServersList && line.stripLeading().startsWith("- ")) {
                // Handles "  - name" lines
                String entry = stripQuotes(
                        line.stripLeading().substring(2).trim());
                if (!entry.isEmpty()) {
                    servers.add(entry.toLowerCase());
                }
            } else {
                inServersList = false;
            }
        }
    }

    /** Strips surrounding single or double quotes from a YAML scalar value. */
    private static String stripQuotes(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last  = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }

    // -------------------------------------------------------------------------
    // Default config
    // -------------------------------------------------------------------------

    private static void writeDefaults(Path configPath) throws IOException {
        String defaultConfig =
                "# VelocityGlobalChat configuration\n" +
                "\n" +
                "# Enable or disable the plugin entirely\n" +
                "enabled: true\n" +
                "\n" +
                "# Chat format. Supported placeholders:\n" +
                "#   {server}  — name of the backend server the sender is on\n" +
                "#   {player}  — sender's username\n" +
                "#   {prefix}  — LuckPerms prefix (empty if LuckPerms is absent)\n" +
                "#   {message} — the chat message (always rendered as plain text)\n" +
                "#\n" +
                "# Use & colour codes (legacy) OR MiniMessage tags (<red>, <bold>, etc.).\n" +
                "# Do NOT mix both styles in the same format string.\n" +
                "format: \"&8[&b{server}&8] &7{prefix}{player}&f: {message}\"\n" +
                "\n" +
                "# Servers that participate in global chat.\n" +
                "# Players on servers NOT in this list will not see or send global messages.\n" +
                "# Leave the list empty to include ALL servers.\n" +
                "servers:\n" +
                "  - hub\n" +
                "  - survival\n" +
                "  - farming\n";

        Files.writeString(configPath, defaultConfig);
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    public String getFormat() {
        return format;
    }

    public Set<String> getServers() {
        return servers;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
