package com.neaplugins.welcomer.listeners;

import com.neaplugins.welcomer.NeaWelcomer;
import com.neaplugins.welcomer.constants.Permissions;
import com.neaplugins.welcomer.managers.ConfigManager;
import com.neaplugins.welcomer.managers.LangManager;
import com.neaplugins.welcomer.utils.MessageUtils;
import java.util.List;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerConnectionListener implements Listener {

    private final NeaWelcomer plugin;

    public PlayerConnectionListener(NeaWelcomer plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        ConfigManager config = plugin.getConfigManager();

        // Disable default Minecraft join message
        if (config.isDisableDefaultMessages()) {
            event.joinMessage(null);
        }

        // Check if this is the player's first time joining
        if (!player.hasPlayedBefore()) {
            if (plugin.getPlayerTracker() != null) {
                plugin.getPlayerTracker().incrementAndGet();
            }
            if (config.isFirstJoinEnabled()) {
                handleFirstJoin(player);
                return;
            }
        }

        // Normal join
        if (config.isJoinEnabled()) {
            handleJoin(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        ConfigManager config = plugin.getConfigManager();

        // Disable default Minecraft quit message
        if (config.isDisableDefaultMessages()) {
            event.quitMessage(null);
        }

        // Custom quit message
        if (config.isQuitEnabled()) {
            handleQuit(player);
        }
    }

    // ─── First Join ─────────────────────────────────────────────

    private void handleFirstJoin(Player player) {
        LangManager lang = plugin.getLangManager();
        ConfigManager config = plugin.getConfigManager();

        // Broadcast first-join messages to online players with permission
        if (config.isFirstJoinBroadcast()) {
            List<String> broadcastLines = lang.getMessageList(
                "first-join.broadcast"
            );
            broadcastMessages(broadcastLines, player, Permissions.FIRSTJOIN_SEE);
        }

        // Send private welcome message only to the joining player
        List<String> privateLines = lang.getMessageList("first-join.private");
        sendPrivateMessages(player, privateLines);

        // Play sound to online players with permission
        if (config.isFirstJoinSoundEnabled()) {
            playBroadcastSound(
                config.getFirstJoinSound(),
                config.getFirstJoinSoundVolume(),
                config.getFirstJoinSoundPitch(),
                Permissions.FIRSTJOIN_SEE
            );
        }

        plugin
            .getLogger()
            .info(
                "[First Join] " +
                    player.getName() +
                    " has joined for the first time!"
            );
    }

    // ─── Normal Join ────────────────────────────────────────────

    private void handleJoin(Player player) {
        LangManager lang = plugin.getLangManager();
        ConfigManager config = plugin.getConfigManager();

        // Broadcast join message to online players with permission
        if (config.isJoinBroadcast()) {
            List<String> broadcastLines = lang.getMessageList("join.broadcast");
            broadcastMessages(broadcastLines, player, Permissions.JOIN_SEE);
        }

        // Send private welcome-back message only to the joining player
        List<String> privateLines = lang.getMessageList("join.private");
        sendPrivateMessages(player, privateLines);

        // Play sound to online players with permission
        if (config.isJoinSoundEnabled()) {
            playBroadcastSound(
                config.getJoinSound(),
                config.getJoinSoundVolume(),
                config.getJoinSoundPitch(),
                Permissions.JOIN_SEE
            );
        }
    }

    // ─── Quit ───────────────────────────────────────────────────

    private void handleQuit(Player player) {
        LangManager lang = plugin.getLangManager();
        ConfigManager config = plugin.getConfigManager();

        // Broadcast quit message to online players with permission
        if (config.isQuitBroadcast()) {
            List<String> broadcastLines = lang.getMessageList("quit.broadcast");
            broadcastMessages(broadcastLines, player, Permissions.QUIT_SEE);
        }

        // Play sound to online players with permission
        if (config.isQuitSoundEnabled()) {
            playBroadcastSound(
                config.getQuitSound(),
                config.getQuitSoundVolume(),
                config.getQuitSoundPitch(),
                Permissions.QUIT_SEE
            );
        }
    }

    // ─── Utility Methods ────────────────────────────────────────

    /**
     * Broadcasts a list of messages (parsed with MiniMessage + placeholders)
     * to online players with the specified permission.
     * Console always receives the broadcast for server audit logging.
     *
     * @param lines      The message lines to broadcast.
     * @param player     The player triggering the broadcast.
     * @param permission The required permission node, or null for no restriction.
     */
    private void broadcastMessages(
        List<String> lines,
        Player player,
        String permission
    ) {
        if (lines == null || lines.isEmpty()) return;

        for (String line : lines) {
            Component component = MessageUtils.parse(line, player);
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (permission == null || online.hasPermission(permission)) {
                    online.sendMessage(component);
                }
            }
            // Also send to console for logging
            Bukkit.getConsoleSender().sendMessage(component);
        }
    }

    /**
     * Sends a list of private messages (parsed with MiniMessage + placeholders)
     * only to the specified player.
     */
    private void sendPrivateMessages(Player player, List<String> lines) {
        if (lines == null || lines.isEmpty()) return;

        // Delay by 1 tick so the player is fully loaded into the server
        Bukkit.getScheduler().runTaskLater(
            plugin,
            () -> {
                if (player.isOnline()) {
                    for (String line : lines) {
                        Component component = MessageUtils.parse(line, player);
                        player.sendMessage(component);
                    }
                }
            },
            5L
        );
    }

    /**
     * Plays a sound to online players with the specified permission.
     *
     * @param soundKey   The namespaced sound key (e.g., "entity.player.levelup" or "minecraft:entity.player.levelup")
     * @param volume     Sound volume (0.0 - 1.0+)
     * @param pitch      Sound pitch (0.5 - 2.0)
     * @param permission The required permission node, or null for no restriction.
     */
    private void playBroadcastSound(
        String soundKey,
        float volume,
        float pitch,
        String permission
    ) {
        if (soundKey == null || soundKey.isEmpty()) return;

        try {
            // Normalize the sound key
            String normalizedKey = soundKey.toLowerCase().replace("_", ".");
            if (!normalizedKey.contains(":")) {
                normalizedKey = "minecraft:" + normalizedKey;
            }

            Sound sound = Sound.sound(
                Key.key(normalizedKey),
                Sound.Source.MASTER,
                volume,
                pitch
            );

            for (Player online : Bukkit.getOnlinePlayers()) {
                if (permission == null || online.hasPermission(permission)) {
                    online.playSound(sound);
                }
            }
        } catch (Exception e) {
            plugin
                .getLogger()
                .warning(
                    "Invalid sound key: " + soundKey + " - " + e.getMessage()
                );
        }
    }
}
