package com.neaplugins.welcomer.managers;

import com.neaplugins.welcomer.NeaWelcomer;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class LangManager {

    private final NeaWelcomer plugin;
    private File langFile;
    private FileConfiguration langConfig;

    public LangManager(NeaWelcomer plugin) {
        this.plugin = plugin;
    }

    /**
     * Loads or reloads the lang.yml file.
     * Creates the file from defaults if it doesn't exist,
     * then merges any missing keys from the jar resource.
     */
    public void load() {
        langFile = new File(plugin.getDataFolder(), "lang.yml");

        if (!langFile.exists()) {
            plugin.saveResource("lang.yml", false);
        }

        langConfig = YamlConfiguration.loadConfiguration(langFile);

        // Merge defaults from the jar so new keys are always present
        InputStream defaultStream = plugin.getResource("lang.yml");
        if (defaultStream != null) {
            YamlConfiguration defaultConfig =
                YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultStream, StandardCharsets.UTF_8)
                );
            langConfig.setDefaults(defaultConfig);
            langConfig.options().copyDefaults(true);
            save();
        }
    }

    /**
     * Reloads the lang.yml from disk.
     */
    public void reload() {
        load();
    }

    /**
     * Saves the current in-memory lang configuration to disk.
     */
    public void save() {
        try {
            langConfig.save(langFile);
        } catch (IOException e) {
            plugin
                .getLogger()
                .severe("Could not save lang.yml: " + e.getMessage());
        }
    }

    /**
     * Returns the raw FileConfiguration for lang.yml.
     */
    public FileConfiguration getConfig() {
        return langConfig;
    }

    // ─── Single-value accessors ──────────────────────────────

    /**
     * Gets a single message string from lang.yml.
     *
     * @param path the YAML path
     * @return the string value, or empty string if missing
     */
    public String getMessage(String path) {
        return langConfig.getString(path, "");
    }

    /**
     * Gets the global prefix defined in lang.yml.
     */
    public String getPrefix() {
        return langConfig.getString(
            "prefix",
            "<#7C4DFF><bold>Nea</bold><#B388FF>Welcomer <dark_gray>» <reset>"
        );
    }

    // ─── List-based accessors ────────────────────────────────

    /**
     * Gets a list of message lines from lang.yml.
     * If the path points to a single string it is wrapped in a list.
     *
     * @param path the YAML path
     * @return list of raw message strings (never null)
     */
    public List<String> getMessageList(String path) {
        if (langConfig.isList(path)) {
            return langConfig.getStringList(path);
        }
        String single = langConfig.getString(path);
        if (single != null && !single.isEmpty()) {
            return Collections.singletonList(single);
        }
        return Collections.emptyList();
    }

    // ─── First-join messages ─────────────────────────────────

    /**
     * Title shown to the player on their first join.
     */
    public String getFirstJoinTitle() {
        return getMessage("first-join.title");
    }

    /**
     * Subtitle shown to the player on their first join.
     */
    public String getFirstJoinSubtitle() {
        return getMessage("first-join.subtitle");
    }

    /**
     * Action bar message shown to the player on their first join.
     */
    public String getFirstJoinActionBar() {
        return getMessage("first-join.actionbar");
    }

    /**
     * Broadcast lines shown to all players on a first join.
     */
    public List<String> getFirstJoinBroadcast() {
        return getMessageList("first-join.broadcast");
    }

    /**
     * Private lines shown only to the player on their first join.
     */
    public List<String> getFirstJoinPrivate() {
        return getMessageList("first-join.private");
    }

    // ─── Join messages ───────────────────────────────────────

    /**
     * Title shown to the player on a normal join.
     */
    public String getJoinTitle() {
        return getMessage("join.title");
    }

    /**
     * Subtitle shown to the player on a normal join.
     */
    public String getJoinSubtitle() {
        return getMessage("join.subtitle");
    }

    /**
     * Action bar message shown to the player on a normal join.
     */
    public String getJoinActionBar() {
        return getMessage("join.actionbar");
    }

    /**
     * Broadcast lines shown to all players on a normal join.
     */
    public List<String> getJoinBroadcast() {
        return getMessageList("join.broadcast");
    }

    /**
     * Private lines shown only to the player on a normal join.
     */
    public List<String> getJoinPrivate() {
        return getMessageList("join.private");
    }

    // ─── Quit messages ───────────────────────────────────────

    /**
     * Broadcast lines shown to all players when someone quits.
     */
    public List<String> getQuitBroadcast() {
        return getMessageList("quit.broadcast");
    }

    // ─── Command messages ────────────────────────────────────

    public String getReloadMessage() {
        return getMessage("commands.reload");
    }

    public String getReloadSuccessMessage() {
        return getMessage("commands.reload-success");
    }

    public String getReloadFailMessage() {
        return getMessage("commands.reload-fail");
    }

    public String getNoPermissionMessage() {
        return getMessage("commands.no-permission");
    }

    public List<String> getHelpMessage() {
        return getMessageList("commands.help");
    }

    public List<String> getInfoMessage() {
        return getMessageList("commands.info");
    }

    // ─── Error messages ──────────────────────────────────────

    public String getPlayerOnlyMessage() {
        return getMessage("errors.player-only");
    }

    public String getUnknownCommandMessage() {
        return getMessage("errors.unknown-command");
    }
}
