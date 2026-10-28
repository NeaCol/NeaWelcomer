package com.neaplugins.welcomer;

import com.neaplugins.welcomer.commands.WelcomerCommand;
import com.neaplugins.welcomer.listeners.PlayerConnectionListener;
import com.neaplugins.welcomer.managers.ConfigManager;
import com.neaplugins.welcomer.managers.LangManager;
import com.neaplugins.welcomer.managers.UniquePlayerTracker;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class NeaWelcomer extends JavaPlugin {

    private static NeaWelcomer instance;
    private ConfigManager configManager;
    private LangManager langManager;
    private UniquePlayerTracker playerTracker;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private boolean placeholderAPIEnabled = false;

    @Override
    public void onEnable() {
        instance = this;

        // Initialize managers
        this.configManager = new ConfigManager(this);
        this.langManager = new LangManager(this);
        this.playerTracker = new UniquePlayerTracker(this);
        this.playerTracker.initializeAsync();

        // Load configurations
        this.configManager.load();
        this.langManager.load();

        // Register listeners
        Bukkit.getPluginManager().registerEvents(
            new PlayerConnectionListener(this),
            this
        );

        // Register commands
        if (getCommand("neawelcomer") != null) {
            WelcomerCommand welcomerCommand = new WelcomerCommand(this);
            getCommand("neawelcomer").setExecutor(welcomerCommand);
            getCommand("neawelcomer").setTabCompleter(welcomerCommand);
        }

        // Check for PlaceholderAPI
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            this.placeholderAPIEnabled = true;
            getLogger().info(
                "PlaceholderAPI found! Placeholder support enabled."
            );
        } else {
            this.placeholderAPIEnabled = false;
            getLogger().info(
                "PlaceholderAPI not found. Using built-in placeholders only."
            );
        }

        // Initialize bStats Metrics
        if (configManager.isMetricsEnabled()) {
            try {
                new com.neaplugins.welcomer.metrics.Metrics(this, 34682);
                getLogger().info("bStats metrics enabled.");
            } catch (Exception e) {
                getLogger().warning("Could not initialize bStats: " + e.getMessage());
            }
        }

        // Startup message
        sendStartupMessage();
    }

    @Override
    public void onDisable() {
        getLogger().info("NeaWelcomer has been disabled.");
        instance = null;
    }

    /**
     * Reloads all plugin configurations.
     */
    public void reload() {
        this.configManager.load();
        this.langManager.load();

        // Re-check PlaceholderAPI availability on reload
        this.placeholderAPIEnabled =
            Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;

        getLogger().info("Configuration reloaded successfully.");
    }

    /**
     * Sends a styled startup message to the console.
     */
    private void sendStartupMessage() {
        getLogger().info("");
        getLogger().info("  _   _            __        __   _");
        getLogger().info(
            " | \\ | | ___  __ _ \\ \\      / /__| | ___ ___  _ __ ___   ___ _ __"
        );
        getLogger().info(
            " |  \\| |/ _ \\/ _` | \\ \\ /\\ / / _ \\ |/ __/ _ \\| '_ ` _ \\ / _ \\ '__|"
        );
        getLogger().info(
            " | |\\  |  __/ (_| |  \\ V  V /  __/ | (_| (_) | | | | | |  __/ |"
        );
        getLogger().info(
            " |_| \\_|\\___|\\__,_|   \\_/\\_/ \\___|_|\\___\\___/|_| |_| |_|\\___|_|"
        );
        getLogger().info("");
        getLogger().info(" Version: " + getDescription().getVersion());
        getLogger().info(" Author: NeaPlugins");
        getLogger().info(" Platform: Purpur/Paper 1.21.1");
        getLogger().info(" MiniMessage: Enabled");
        getLogger().info(
            " PlaceholderAPI: " +
                (placeholderAPIEnabled ? "Connected" : "Not found")
        );
        getLogger().info("");
    }

    /**
     * Parses a MiniMessage string into an Adventure Component.
     *
     * @param message The MiniMessage string.
     * @return The parsed Adventure Component.
     */
    public Component parse(String message) {
        return miniMessage.deserialize(message);
    }

    /**
     * Checks if PlaceholderAPI is available and enabled.
     *
     * @return true if PlaceholderAPI is loaded and enabled
     */
    public boolean hasPlaceholderAPI() {
        return placeholderAPIEnabled;
    }

    // ─── Getters ──────────────────────────────────────────────

    public static NeaWelcomer getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public LangManager getLangManager() {
        return langManager;
    }

    public UniquePlayerTracker getPlayerTracker() {
        return playerTracker;
    }

    public MiniMessage getMiniMessage() {
        return miniMessage;
    }
}
