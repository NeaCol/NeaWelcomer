package com.neaplugins.welcomer.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class MessageUtils {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_SERIALIZER =
        LegacyComponentSerializer.builder()
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    // Pattern to match legacy &#RRGGBB hex codes and convert them to MiniMessage <#RRGGBB> format
    private static final Pattern LEGACY_HEX_PATTERN = Pattern.compile(
        "&#([0-9a-fA-F]{6})"
    );
    // Pattern to match legacy & color codes like &a, &b, &l, etc.
    private static final Pattern LEGACY_COLOR_PATTERN = Pattern.compile(
        "&([0-9a-fk-orA-FK-OR])"
    );

    private MessageUtils() {
        // Utility class, no instantiation
    }

    /**
     * Parses a raw message string into an Adventure Component.
     * Supports MiniMessage tags, legacy &#RRGGBB hex, legacy &color codes,
     * and PlaceholderAPI placeholders.
     *
     * @param message the raw message string
     * @param player  the player for placeholder resolution (can be null)
     * @return the parsed Adventure Component
     */
    public static Component parse(String message, Player player) {
        if (message == null || message.isEmpty()) {
            return Component.empty();
        }

        // Step 1: Replace built-in placeholders
        if (player != null) {
            message = applyBuiltInPlaceholders(message, player);
        }

        // Step 2: Apply PlaceholderAPI placeholders if available
        message = applyPlaceholderAPI(message, player);

        // Step 3: Convert legacy hex format &#RRGGBB -> MiniMessage <#RRGGBB>
        message = convertLegacyHex(message);

        // Step 4: Convert legacy color codes &a, &b, etc. -> MiniMessage equivalents
        message = convertLegacyColors(message);

        // Step 5: Parse with MiniMessage
        return MINI_MESSAGE.deserialize(message);
    }

    /**
     * Parses a message without a player context (no placeholders).
     *
     * @param message the raw message string
     * @return the parsed Adventure Component
     */
    public static Component parse(String message) {
        return parse(message, null);
    }

    /**
     * Replaces built-in plugin placeholders in the message string and returns
     * the resulting string (without parsing to Component). Useful when you need
     * to do further string processing before parsing.
     *
     * @param message the raw message string
     * @param player  the player for placeholder resolution
     * @return the message with placeholders replaced
     */
    public static String replacePlaceholders(String message, Player player) {
        if (message == null || message.isEmpty()) {
            return message;
        }

        if (player != null) {
            message = applyBuiltInPlaceholders(message, player);
        }

        message = applyPlaceholderAPI(message, player);

        return message;
    }

    /**
     * Replaces built-in plugin placeholders in the message.
     */
    private static String applyBuiltInPlaceholders(
        String message,
        Player player
    ) {
        message = message.replace("{player}", player.getName());
        message = message.replace("{PLAYER}", player.getName());
        message = message.replace("{displayname}", getPlainDisplayName(player));
        message = message.replace("{DISPLAYNAME}", getPlainDisplayName(player));
        message = message.replace(
            "{online}",
            String.valueOf(Bukkit.getOnlinePlayers().size())
        );
        message = message.replace(
            "{ONLINE}",
            String.valueOf(Bukkit.getOnlinePlayers().size())
        );
        message = message.replace(
            "{max_players}",
            String.valueOf(Bukkit.getMaxPlayers())
        );
        message = message.replace(
            "{MAX_PLAYERS}",
            String.valueOf(Bukkit.getMaxPlayers())
        );
        message = message.replace("{world}", player.getWorld().getName());
        message = message.replace("{WORLD}", player.getWorld().getName());

        // {player_number} placeholder — count of unique players who have ever joined
        int playerNumber = countUniquePlayersEverJoined();
        message = message.replace(
            "{player_number}",
            String.valueOf(playerNumber)
        );
        message = message.replace(
            "{PLAYER_NUMBER}",
            String.valueOf(playerNumber)
        );

        // {rank} placeholder — resolved via PlaceholderAPI if available, otherwise fallback
        String rank = resolveRank(player);
        message = message.replace("{rank}", rank);
        message = message.replace("{RANK}", rank);

        return message;
    }

    /**
     * Resolves the player's rank/group using PlaceholderAPI if available.
     * Tries multiple common placeholders from Vault and LuckPerms.
     * Falls back to "Player" if nothing is available.
     *
     * @param player the player
     * @return the rank string
     */
    private static String resolveRank(Player player) {
        if (isPlaceholderAPIAvailable()) {
            try {
                // Try vault prefix first (most common)
                String vaultPrefix = PlaceholderAPI.setPlaceholders(
                    player,
                    "%vault_prefix%"
                );
                if (isValidPlaceholderResult(vaultPrefix, "%vault_prefix%")) {
                    return vaultPrefix.trim();
                }

                // Try LuckPerms primary group
                String lpGroup = PlaceholderAPI.setPlaceholders(
                    player,
                    "%luckperms_primary_group%"
                );
                if (
                    isValidPlaceholderResult(
                        lpGroup,
                        "%luckperms_primary_group%"
                    )
                ) {
                    return lpGroup.trim();
                }

                // Try vault group
                String vaultGroup = PlaceholderAPI.setPlaceholders(
                    player,
                    "%vault_group%"
                );
                if (isValidPlaceholderResult(vaultGroup, "%vault_group%")) {
                    return vaultGroup.trim();
                }
            } catch (Exception ignored) {
                // Silently fail
            }
        }

        // Fallback: check if player is OP
        if (player.isOp()) {
            return "Admin";
        }

        return "Player";
    }

    /**
     * Checks whether a PlaceholderAPI result is a valid resolved value
     * (i.e., not empty and not the original unresolved placeholder).
     */
    private static boolean isValidPlaceholderResult(
        String result,
        String originalPlaceholder
    ) {
        return (
            result != null &&
            !result.isEmpty() &&
            !result.equals(originalPlaceholder) &&
            !result.equalsIgnoreCase("none") &&
            !result.equalsIgnoreCase("null")
        );
    }

    /**
     * Gets the plain text display name of a player.
     */
    private static String getPlainDisplayName(Player player) {
        return LegacyComponentSerializer.legacySection().serialize(
            player.displayName()
        );
    }

    /**
     * Applies PlaceholderAPI placeholders if the plugin is available.
     */
    private static String applyPlaceholderAPI(String message, Player player) {
        if (player != null && isPlaceholderAPIAvailable()) {
            try {
                return PlaceholderAPI.setPlaceholders(player, message);
            } catch (Exception e) {
                // Silently fail if PAPI has issues
                return message;
            }
        }
        return message;
    }

    /**
     * Converts legacy &#RRGGBB hex color codes to MiniMessage format.
     * Example: &#FF5555 -> <#FF5555>
     */
    private static String convertLegacyHex(String message) {
        Matcher matcher = LEGACY_HEX_PATTERN.matcher(message);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(sb, "<#" + matcher.group(1) + ">");
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Converts legacy & color codes to MiniMessage tags.
     */
    private static String convertLegacyColors(String message) {
        Matcher matcher = LEGACY_COLOR_PATTERN.matcher(message);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String code = matcher.group(1).toLowerCase();
            String replacement = legacyToMiniMessage(code);
            matcher.appendReplacement(
                sb,
                Matcher.quoteReplacement(replacement)
            );
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Maps a single legacy color/formatting code character to a MiniMessage tag.
     */
    private static String legacyToMiniMessage(String code) {
        return switch (code) {
            case "0" -> "<black>";
            case "1" -> "<dark_blue>";
            case "2" -> "<dark_green>";
            case "3" -> "<dark_aqua>";
            case "4" -> "<dark_red>";
            case "5" -> "<dark_purple>";
            case "6" -> "<gold>";
            case "7" -> "<gray>";
            case "8" -> "<dark_gray>";
            case "9" -> "<blue>";
            case "a" -> "<green>";
            case "b" -> "<aqua>";
            case "c" -> "<red>";
            case "d" -> "<light_purple>";
            case "e" -> "<yellow>";
            case "f" -> "<white>";
            case "k" -> "<obfuscated>";
            case "l" -> "<bold>";
            case "m" -> "<strikethrough>";
            case "n" -> "<underlined>";
            case "o" -> "<italic>";
            case "r" -> "<reset>";
            default -> "&" + code;
        };
    }

    /**
     * Checks if PlaceholderAPI is available on the server.
     *
     * @return true if PlaceholderAPI is loaded and enabled
     */
    public static boolean isPlaceholderAPIAvailable() {
        return Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
    }

    /**
     * Strips all MiniMessage tags and legacy codes from a message, returning plain text.
     *
     * @param message the raw message
     * @return plain text version
     */
    public static String stripFormatting(String message) {
        if (message == null || message.isEmpty()) {
            return "";
        }
        message = convertLegacyHex(message);
        message = convertLegacyColors(message);
        return MINI_MESSAGE.stripTags(message);
    }

    /**
     * Converts a Component to a legacy string (for console logging, etc.)
     *
     * @param component the component to serialize
     * @return legacy formatted string
     */
    public static String toLegacy(Component component) {
        return LEGACY_SERIALIZER.serialize(component);
    }

    /**
     * Counts the total number of unique players who have ever joined the server.
     * This includes both online and offline players.
     *
     * @return the count of unique players
     */
    private static int countUniquePlayersEverJoined() {
        try {
            // Get all player data files from the world's playerdata folder
            java.io.File playerdataFolder = new java.io.File(
                Bukkit.getWorlds().get(0).getWorldFolder(),
                "playerdata"
            );

            if (!playerdataFolder.exists()) {
                return Bukkit.getOnlinePlayers().size();
            }

            // Count .dat files in the playerdata folder
            java.io.File[] playerFiles = playerdataFolder.listFiles(
                (dir, name) -> name.endsWith(".dat")
            );

            if (playerFiles == null) {
                return Bukkit.getOnlinePlayers().size();
            }

            return playerFiles.length;
        } catch (Exception e) {
            // Fallback to online player count if something goes wrong
            return Bukkit.getOnlinePlayers().size();
        }
    }
}
