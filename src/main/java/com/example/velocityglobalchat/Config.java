package com.example.velocityglobalchat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Minimal YAML-like config parser. Supports:
 *   format: <string>         — may optionally be wrapped in single or double quotes
 *   enabled: true|false
 *   servers:
 *     servername: "Display Text"   — key = velocity server name (case-insensitive)
 *                                    value = the text shown for {server} in the format
 *
 * Backwards-compatible list form is also accepted:
 *   servers:
 *     - servername              — display defaults to the name uppercased
 */
public class Config {

    private static final String DEFAULT_FORMAT =
            "&8[&b{server}&8] &7{prefix}{player}&f: {message}";

    private String format  = DEFAULT_FORMAT;
    private boolean enabled = true;
    /** key = lowercase velocity server name, value = configured display string */
    private final Map<String, String> serverDisplayNames = new LinkedHashMap<>();

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

            // Top-level keys are not indented; reset context when we see one
            boolean isIndented = !line.isEmpty()
                    && (line.charAt(0) == ' ' || line.charAt(0) == '\t');
            if (!isIndented) {
                inServersList = false;
            }

            if (line.startsWith("format:")) {
                format = stripQuotes(line.substring("format:".length()).trim());

            } else if (line.startsWith("enabled:")) {
                enabled = Boolean.parseBoolean(
                        stripQuotes(line.substring("enabled:".length()).trim()));

            } else if (line.startsWith("servers:")) {
                inServersList = true;

            } else if (inServersList && isIndented) {
                String stripped = line.stripLeading();

                if (stripped.startsWith("- ")) {
                    // Legacy list format — display defaults to name uppercased
                    String name = stripQuotes(stripped.substring(2).trim()).toLowerCase();
                    if (!name.isEmpty()) {
                        serverDisplayNames.put(name, name.toUpperCase());
                    }
                } else {
                    // New key: value format — "hub: \"&bHUB\""
                    int colonIdx = stripped.indexOf(':');
                    if (colonIdx > 0) {
                        String key = stripped.substring(0, colonIdx).trim().toLowerCase();
                        String val = stripQuotes(stripped.substring(colonIdx + 1).trim());
                        if (!key.isEmpty()) {
                            // If value is blank, fall back to uppercased key
                            serverDisplayNames.put(key,
                                    val.isEmpty() ? key.toUpperCase() : val);
                        }
                    }
                }
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
                "# Format: servername: \"Display Text\"\n" +
                "#   The key must match the server name in velocity.toml (case-insensitive).\n" +
                "#   The value is the text substituted for {server} in the format string.\n" +
                "#   Use the same colour style (&-codes or MiniMessage) as your format.\n" +
                "# Players on servers NOT listed here neither see nor send global messages.\n" +
                "# Leave the block empty (no entries) to include ALL servers; {server}\n" +
                "# will then show the raw server name uppercased.\n" +
                "servers:\n" +
                "  hub: \"&bHUB\"\n" +
                "  survival: \"&aSURVIVAL\"\n" +
                "  farming: \"&eFARMING\"\n";

        Files.writeString(configPath, defaultConfig);
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    public String getFormat() {
        return format;
    }

    /**
     * Returns the set of lowercase server names included in global chat.
     * Empty means all servers are included.
     */
    public Set<String> getServers() {
        return serverDisplayNames.keySet();
    }

    /**
     * Returns the configured display string for the given server name,
     * falling back to the name uppercased if no entry exists.
     */
    public String getDisplayName(String serverName) {
        return serverDisplayNames.getOrDefault(
                serverName.toLowerCase(), serverName.toUpperCase());
    }

    public boolean isEnabled() {
        return enabled;
    }
}
