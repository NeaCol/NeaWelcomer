package com.neaplugins.welcomer.managers;

import com.neaplugins.welcomer.NeaWelcomer;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {

    private final NeaWelcomer plugin;

    // General
    private boolean disableDefaultMessages;

    // First join
    private boolean firstJoinEnabled = true;
    private boolean firstJoinBroadcast = true;
    private boolean firstJoinWelcomeButton = true;
    private boolean firstJoinPrivateMessage = true;
    private boolean firstJoinTitleEnabled = true;
    private long firstJoinTitleFadeIn = 10L;
    private long firstJoinTitleStay = 70L;
    private long firstJoinTitleFadeOut = 20L;
    private boolean firstJoinActionBarEnabled = true;

    // Join
    private boolean joinEnabled = true;
    private boolean joinBroadcast = true;
    private boolean joinPrivateMessage = true;
    private boolean joinTitleEnabled = true;
    private long joinTitleFadeIn = 10L;
    private long joinTitleStay = 70L;
    private long joinTitleFadeOut = 20L;
    private boolean joinActionBarEnabled = true;

    // Quit
    private boolean quitEnabled;
    private boolean quitBroadcast;

    // Integrations
    private boolean placeholderApiEnabled;

    // Sounds - First join
    private boolean firstJoinSoundEnabled;
    private String firstJoinSound;
    private float firstJoinSoundVolume;
    private float firstJoinSoundPitch;

    // Sounds - Join
    private boolean joinSoundEnabled;
    private String joinSound;
    private float joinSoundVolume;
    private float joinSoundPitch;

    // Sounds - Quit
    private boolean quitSoundEnabled;
    private String quitSound;
    private float quitSoundVolume;
    private float quitSoundPitch;

    public ConfigManager(NeaWelcomer plugin) {
        this.plugin = plugin;
    }

    /**
     * Loads (or reloads) the config.yml file and caches all values.
     */
    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();

        FileConfiguration config = plugin.getConfig();

        // General
        this.disableDefaultMessages = config.getBoolean(
            "disable-default-messages",
            true
        );

        // First join
        this.firstJoinEnabled = config.getBoolean("first-join.enabled", true);
        this.firstJoinBroadcast = config.getBoolean(
            "first-join.broadcast",
            true
        );
        this.firstJoinWelcomeButton = config.getBoolean(
            "first-join.welcome-button",
            true
        );
        this.firstJoinPrivateMessage = config.getBoolean(
            "first-join.private-message",
            true
        );
        this.firstJoinTitleEnabled = config.getBoolean(
            "first-join.title.enabled",
            true
        );
        this.firstJoinTitleFadeIn = config.getLong(
            "first-join.title.fade-in",
            10L
        );
        this.firstJoinTitleStay = config.getLong(
            "first-join.title.stay",
            70L
        );
        this.firstJoinTitleFadeOut = config.getLong(
            "first-join.title.fade-out",
            20L
        );
        this.firstJoinActionBarEnabled = config.getBoolean(
            "first-join.actionbar.enabled",
            true
        );

        // Join
        this.joinEnabled = config.getBoolean("join.enabled", true);
        this.joinBroadcast = config.getBoolean("join.broadcast", true);
        this.joinPrivateMessage = config.getBoolean(
            "join.private-message",
            true
        );
        this.joinTitleEnabled = config.getBoolean("join.title.enabled", true);
        this.joinTitleFadeIn = config.getLong("join.title.fade-in", 10L);
        this.joinTitleStay = config.getLong("join.title.stay", 70L);
        this.joinTitleFadeOut = config.getLong("join.title.fade-out", 20L);
        this.joinActionBarEnabled = config.getBoolean(
            "join.actionbar.enabled",
            true
        );

        // Quit
        this.quitEnabled = config.getBoolean("quit.enabled", true);
        this.quitBroadcast = config.getBoolean("quit.broadcast", true);

        // Integrations
        this.placeholderApiEnabled = config.getBoolean("placeholderapi", true);

        // Sounds - First join
        this.firstJoinSoundEnabled = config.getBoolean(
            "sounds.first-join.enabled",
            false
        );
        this.firstJoinSound = config.getString(
            "sounds.first-join.sound",
            "ENTITY_PLAYER_LEVELUP"
        );
        this.firstJoinSoundVolume = (float) config.getDouble(
            "sounds.first-join.volume",
            1.0
        );
        this.firstJoinSoundPitch = (float) config.getDouble(
            "sounds.first-join.pitch",
            1.0
        );

        // Sounds - Join
        this.joinSoundEnabled = config.getBoolean("sounds.join.enabled", false);
        this.joinSound = config.getString(
            "sounds.join.sound",
            "ENTITY_EXPERIENCE_ORB_PICKUP"
        );
        this.joinSoundVolume = (float) config.getDouble(
            "sounds.join.volume",
            1.0
        );
        this.joinSoundPitch = (float) config.getDouble(
            "sounds.join.pitch",
            1.0
        );

        // Sounds - Quit
        this.quitSoundEnabled = config.getBoolean("sounds.quit.enabled", false);
        this.quitSound = config.getString(
            "sounds.quit.sound",
            "BLOCK_NOTE_BLOCK_BASS"
        );
        this.quitSoundVolume = (float) config.getDouble(
            "sounds.quit.volume",
            1.0
        );
        this.quitSoundPitch = (float) config.getDouble(
            "sounds.quit.pitch",
            1.0
        );
    }

    /**
     * Reloads the configuration from disk.
     */
    public void reload() {
        load();
    }

    // ─── General ────────────────────────────────────────────────

    public boolean isDisableDefaultMessages() {
        return disableDefaultMessages;
    }

    // ─── First Join ─────────────────────────────────────────────

    public boolean isFirstJoinEnabled() {
        return firstJoinEnabled;
    }

    public boolean isFirstJoinBroadcast() {
        return firstJoinBroadcast;
    }

    public boolean isFirstJoinWelcomeButton() {
        return firstJoinWelcomeButton;
    }

    public boolean isFirstJoinPrivateMessage() {
        return firstJoinPrivateMessage;
    }

    public boolean isFirstJoinPrivateMessageEnabled() {
        return firstJoinPrivateMessage;
    }

    public boolean isFirstJoinTitleEnabled() {
        return firstJoinTitleEnabled;
    }

    public long getFirstJoinTitleFadeIn() {
        return firstJoinTitleFadeIn;
    }

    public long getFirstJoinTitleStay() {
        return firstJoinTitleStay;
    }

    public long getFirstJoinTitleFadeOut() {
        return firstJoinTitleFadeOut;
    }

    public boolean isFirstJoinActionBarEnabled() {
        return firstJoinActionBarEnabled;
    }

    // ─── Join ───────────────────────────────────────────────────

    public boolean isJoinEnabled() {
        return joinEnabled;
    }

    public boolean isJoinBroadcast() {
        return joinBroadcast;
    }

    public boolean isJoinPrivateMessage() {
        return joinPrivateMessage;
    }

    public boolean isJoinPrivateMessageEnabled() {
        return joinPrivateMessage;
    }

    public boolean isJoinTitleEnabled() {
        return joinTitleEnabled;
    }

    public long getJoinTitleFadeIn() {
        return joinTitleFadeIn;
    }

    public long getJoinTitleStay() {
        return joinTitleStay;
    }

    public long getJoinTitleFadeOut() {
        return joinTitleFadeOut;
    }

    public boolean isJoinActionBarEnabled() {
        return joinActionBarEnabled;
    }

    // ─── Quit ───────────────────────────────────────────────────

    public boolean isQuitEnabled() {
        return quitEnabled;
    }

    public boolean isQuitBroadcast() {
        return quitBroadcast;
    }

    // ─── Integrations ───────────────────────────────────────────

    public boolean isPlaceholderApiEnabled() {
        return placeholderApiEnabled;
    }

    // ─── Sounds: First Join ─────────────────────────────────────

    public boolean isFirstJoinSoundEnabled() {
        return firstJoinSoundEnabled;
    }

    public String getFirstJoinSound() {
        return firstJoinSound;
    }

    public float getFirstJoinSoundVolume() {
        return firstJoinSoundVolume;
    }

    public float getFirstJoinSoundPitch() {
        return firstJoinSoundPitch;
    }

    // ─── Sounds: Join ───────────────────────────────────────────

    public boolean isJoinSoundEnabled() {
        return joinSoundEnabled;
    }

    public String getJoinSound() {
        return joinSound;
    }

    public float getJoinSoundVolume() {
        return joinSoundVolume;
    }

    public float getJoinSoundPitch() {
        return joinSoundPitch;
    }

    // ─── Sounds: Quit ───────────────────────────────────────────

    public boolean isQuitSoundEnabled() {
        return quitSoundEnabled;
    }

    public String getQuitSound() {
        return quitSound;
    }

    public float getQuitSoundVolume() {
        return quitSoundVolume;
    }

    public float getQuitSoundPitch() {
        return quitSoundPitch;
    }
}
