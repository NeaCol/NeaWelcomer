package com.neaplugins.welcomer.utils;

import com.neaplugins.welcomer.NeaWelcomer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Utility class for parsing messages, colors, placeholders, and MiniMessage components.
 * <p>
 * Hardened against MiniMessage tag injection by evaluating dynamic player inputs
 * as literal unparsed text or components via Adventure {@link TagResolver}, rather than
 * naive string replacement prior to deserialization.
 */
public final class MessageUtils {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer AMPERSAND_SERIALIZER =
        LegacyComponentSerializer.builder().character('&').hexColors().build();
    private static final LegacyComponentSerializer SECTION_SERIALIZER =
        LegacyComponentSerializer.builder().character('§').hexColors().build();
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

    // Precompiled placeholder normalization pattern: converts {name} to <name> on templates
    private static final Pattern BRACKET_PLACEHOLDER_PATTERN = Pattern.compile(
        "\\{(player|displayname|rank|player_number|online|max_players|world|prefix|time)\\}",
        Pattern.CASE_INSENSITIVE
    );

    private MessageUtils() {
        // Utility class, no instantiation
    }

    /**
     * Parses a raw message string into an Adventure Component safely using TagResolver.
     * Dynamic user inputs (such as player names and ranks) are evaluated as unparsed text
     * or pre-sanitized components, eliminating MiniMessage tag injection attacks.
     *
     * @param message        The raw message template.
     * @param player         The player for placeholder resolution (can be null).
     * @param extraResolvers Optional additional tag resolvers.
     * @return The parsed Adventure Component.
     */
    public static Component parse(
        String message,
        Player player,
        TagResolver... extraResolvers
    ) {
        if (message == null || message.isEmpty()) {
            return Component.empty();
        }

        // Step 1: Expand PlaceholderAPI on the template if available
        if (player != null && isPlaceholderAPIAvailable()) {
            try {
                message = PlaceholderAPI.setPlaceholders(player, message);
            } catch (Throwable ignored) {}
        }

        // Step 2: Convert legacy hex formatting &#RRGGBB -> <#RRGGBB>
        message = convertLegacyHex(message);

        // Step 3: Convert legacy color codes &a -> <green>
        message = convertLegacyColors(message);

        // Step 4: Normalize bracket placeholders {tag} -> <tag> on the static template
        message = normalizeBracketPlaceholders(message);

        // Step 5: Build safe TagResolvers (protecting against MiniMessage tag injection)
        TagResolver combinedResolver = buildTagResolver(player, extraResolvers);

        // Step 6: Safely deserialize template with MiniMessage
        return MINI_MESSAGE.deserialize(message, combinedResolver);
    }

    /**
     * Parses a message with a player context.
     *
     * @param message The raw message string.
     * @param player  The player for placeholder resolution.
     * @return The parsed Adventure Component.
     */
    public static Component parse(String message, Player player) {
        return parse(message, player, new TagResolver[0]);
    }

    /**
     * Parses a message with custom tag resolvers and no player context.
     *
     * @param message        The raw message string.
     * @param extraResolvers Additional tag resolvers.
     * @return The parsed Adventure Component.
     */
    public static Component parse(String message, TagResolver... extraResolvers) {
        return parse(message, (Player) null, extraResolvers);
    }

    /**
     * Parses a message without a player context.
     *
     * @param message The raw message string.
     * @return The parsed Adventure Component.
     */
    public static Component parse(String message) {
        return parse(message, (Player) null, new TagResolver[0]);
    }

    /**
     * Replaces {tag} with <tag> in the static template before any dynamic values are bound.
     */
    private static String normalizeBracketPlaceholders(String message) {
        Matcher matcher = BRACKET_PLACEHOLDER_PATTERN.matcher(message);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(sb, "<" + matcher.group(1).toLowerCase() + ">");
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Builds safe Adventure TagResolvers for all dynamic inputs.
     * User inputs are passed as unparsed placeholders or pre-sanitized components,
     * ensuring malicious tags are rendered as literal text.
     */
    private static TagResolver buildTagResolver(
        Player player,
        TagResolver... extraResolvers
    ) {
        List<TagResolver> resolvers = new ArrayList<>();

        // Add prefix resolver
        resolvers.add(resolvePrefixTag());

        // Global stats (accessible even when player == null)
        int uniqueCount = getUniquePlayerCount();
        resolvers.add(Placeholder.unparsed("player_number", String.valueOf(uniqueCount)));

        int onlineCount = getOnlineCount();
        resolvers.add(Placeholder.unparsed("online", String.valueOf(onlineCount)));

        int maxPlayers = getMaxPlayers();
        resolvers.add(Placeholder.unparsed("max_players", String.valueOf(maxPlayers)));

        if (player != null) {
            String playerName = player.getName() != null ? player.getName() : "";
            // Untrusted user input: MUST be unparsed to prevent tag injection
            resolvers.add(Placeholder.unparsed("player", playerName));

            Component displayName = null;
            try {
                displayName = player.displayName();
            } catch (Throwable ignored) {}
            if (displayName == null) {
                displayName = Component.text(playerName);
            }
            resolvers.add(Placeholder.component("displayname", displayName));

            // Rank: parse legacy color codes safely, treat MiniMessage tags as literal text
            String rawRank = resolveRank(player);
            Component rankComponent = parseLegacySafe(rawRank);
            resolvers.add(Placeholder.component("rank", rankComponent));

            String worldName = "world";
            try {
                if (player.getWorld() != null) {
                    worldName = player.getWorld().getName();
                }
            } catch (Throwable ignored) {}
            resolvers.add(Placeholder.unparsed("world", worldName));
        }

        if (extraResolvers != null) {
            for (TagResolver extra : extraResolvers) {
                if (extra != null) {
                    resolvers.add(extra);
                }
            }
        }

        return TagResolver.resolver(resolvers);
    }

    /**
     * Resolves the configured prefix into a MiniMessage component tag.
     */
    private static TagResolver resolvePrefixTag() {
        try {
            NeaWelcomer plugin = NeaWelcomer.getInstance();
            if (plugin != null && plugin.getLangManager() != null) {
                String rawPrefix = plugin.getLangManager().getPrefix();
                if (rawPrefix != null && !rawPrefix.isEmpty()) {
                    String formatted = convertLegacyHex(rawPrefix);
                    formatted = convertLegacyColors(formatted);
                    Component prefixComp = MINI_MESSAGE.deserialize(formatted);
                    return Placeholder.component("prefix", prefixComp);
                }
            }
        } catch (Throwable ignored) {}
        return Placeholder.component("prefix", Component.empty());
    }

    /**
     * Safely deserializes legacy colors (& and §) into a Component without
     * interpreting MiniMessage tags.
     */
    public static Component parseLegacySafe(String input) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }
        if (input.contains("&")) {
            return AMPERSAND_SERIALIZER.deserialize(input);
        }
        if (input.contains("§")) {
            return SECTION_SERIALIZER.deserialize(input);
        }
        return Component.text(input);
    }

    /**
     * Replaces built-in plugin placeholders in the message string and returns
     * the resulting string.
     *
     * @param message The raw message string.
     * @param player  The player for placeholder resolution.
     * @return The message with placeholders replaced.
     */
    public static String replacePlaceholders(String message, Player player) {
        if (message == null || message.isEmpty()) {
            return message;
        }

        if (player != null) {
            String name = player.getName() != null ? player.getName() : "";
            message = message.replace("{player}", name);
            message = message.replace("{PLAYER}", name);
            message = message.replace("{displayname}", getPlainDisplayName(player));
            message = message.replace("{DISPLAYNAME}", getPlainDisplayName(player));
            message = message.replace("{online}", String.valueOf(getOnlineCount()));
            message = message.replace("{ONLINE}", String.valueOf(getOnlineCount()));
            message = message.replace("{max_players}", String.valueOf(getMaxPlayers()));
            message = message.replace("{MAX_PLAYERS}", String.valueOf(getMaxPlayers()));
            try {
                if (player.getWorld() != null) {
                    message = message.replace("{world}", player.getWorld().getName());
                    message = message.replace("{WORLD}", player.getWorld().getName());
                }
            } catch (Throwable ignored) {}

            int playerNumber = getUniquePlayerCount();
            message = message.replace("{player_number}", String.valueOf(playerNumber));
            message = message.replace("{PLAYER_NUMBER}", String.valueOf(playerNumber));

            String rank = resolveRank(player);
            message = message.replace("{rank}", rank);
            message = message.replace("{RANK}", rank);
        }

        message = applyPlaceholderAPI(message, player);
        return message;
    }

    /**
     * Resolves the player's rank/group using PlaceholderAPI if available.
     * Tries multiple common placeholders from Vault and LuckPerms.
     * Falls back to OP status check or "Player".
     *
     * @param player The player.
     * @return The rank string.
     */
    public static String resolveRank(Player player) {
        if (player == null) {
            return "Player";
        }

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
            } catch (Throwable ignored) {
                // Silently fail
            }
        }

        // Fallback: check if player is OP
        try {
            if (player.isOp()) {
                return "Admin";
            }
        } catch (Throwable ignored) {}

        return "Player";
    }

    /**
     * Checks whether a PlaceholderAPI result is a valid resolved value.
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
        try {
            if (player.displayName() != null) {
                return LegacyComponentSerializer.legacySection().serialize(
                    player.displayName()
                );
            }
        } catch (Throwable ignored) {}
        return player.getName() != null ? player.getName() : "";
    }

    /**
     * Applies PlaceholderAPI placeholders if the plugin is available.
     */
    private static String applyPlaceholderAPI(String message, Player player) {
        if (player != null && isPlaceholderAPIAvailable()) {
            try {
                return PlaceholderAPI.setPlaceholders(player, message);
            } catch (Throwable e) {
                return message;
            }
        }
        return message;
    }

    /**
     * Converts legacy &#RRGGBB hex color codes to MiniMessage format.
     * Example: &#FF5555 -> <#FF5555>
     */
    public static String convertLegacyHex(String message) {
        if (message == null || message.isEmpty()) {
            return "";
        }
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
    public static String convertLegacyColors(String message) {
        if (message == null || message.isEmpty()) {
            return "";
        }
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
        try {
            return (
                Bukkit.getServer() != null &&
                Bukkit.getPluginManager() != null &&
                Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null
            );
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Strips all MiniMessage tags and legacy codes from a message, returning plain text.
     *
     * @param message The raw message.
     * @return Plain text version.
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
     * Converts a Component to a legacy string.
     *
     * @param component The component to serialize.
     * @return Legacy formatted string.
     */
    public static String toLegacy(Component component) {
        return LEGACY_SERIALIZER.serialize(component);
    }

    /**
     * Returns the cached unique player count in O(1) non-blocking time.
     * Completely eliminates any main-thread synchronous disk I/O.
     *
     * @return The unique player count.
     */
    public static int getUniquePlayerCount() {
        try {
            NeaWelcomer plugin = NeaWelcomer.getInstance();
            if (plugin != null && plugin.getPlayerTracker() != null) {
                return plugin.getPlayerTracker().getUniquePlayerCount();
            }
            if (Bukkit.getServer() != null && Bukkit.getOnlinePlayers() != null) {
                return Bukkit.getOnlinePlayers().size();
            }
        } catch (Throwable ignored) {}
        return 0;
    }

    /**
     * Gets online player count safely without throwing if Bukkit is uninitialized.
     */
    private static int getOnlineCount() {
        try {
            if (Bukkit.getServer() != null && Bukkit.getOnlinePlayers() != null) {
                return Bukkit.getOnlinePlayers().size();
            }
        } catch (Throwable ignored) {}
        return 0;
    }

    /**
     * Gets max players safely without throwing if Bukkit is uninitialized.
     */
    private static int getMaxPlayers() {
        try {
            if (Bukkit.getServer() != null) {
                return Bukkit.getMaxPlayers();
            }
        } catch (Throwable ignored) {}
        return 0;
    }
}
