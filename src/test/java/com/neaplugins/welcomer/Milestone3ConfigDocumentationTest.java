package com.neaplugins.welcomer;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Automated test suite for Milestone 3:
 * Verifies professional configuration documentation, absence of ASCII box-drawing art,
 * YAML validity, and preservation of all required configuration and message keys.
 */
class Milestone3ConfigDocumentationTest {

    @Test
    @DisplayName("config.yml and lang.yml contain zero box-drawing characters (U+2500 to U+257F)")
    void testNoBoxDrawingCharactersInResourceFiles() throws Exception {
        String[] resources = {"config.yml", "lang.yml"};

        for (String resource : resources) {
            try (InputStream in = getClass().getClassLoader().getResourceAsStream(resource)) {
                assertNotNull(in, "Resource " + resource + " must exist in classpath");
                String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);

                int boxDrawingCount = 0;
                for (char c : content.toCharArray()) {
                    if (c >= 0x2500 && c <= 0x257F) {
                        boxDrawingCount++;
                    }
                }

                assertEquals(0, boxDrawingCount,
                    resource + " must not contain box-drawing characters in unicode range U+2500 to U+257F");
            }
        }
    }

    @Test
    @DisplayName("config.yml is valid YAML and preserves all required configuration keys")
    void testConfigYmlStructureAndKeys() {
        InputStream in = getClass().getClassLoader().getResourceAsStream("config.yml");
        assertNotNull(in, "config.yml must exist in classpath");

        YamlConfiguration config = YamlConfiguration.loadConfiguration(
            new InputStreamReader(in, StandardCharsets.UTF_8)
        );

        // General settings
        assertTrue(config.contains("disable-default-messages"), "config.yml must have disable-default-messages");
        assertTrue(config.isBoolean("disable-default-messages"));
        assertTrue(config.getBoolean("disable-default-messages"));

        assertTrue(config.contains("placeholderapi"), "config.yml must have placeholderapi");
        assertTrue(config.isBoolean("placeholderapi"));
        assertTrue(config.getBoolean("placeholderapi"));

        // First-join keys
        assertTrue(config.contains("first-join.enabled"));
        assertTrue(config.contains("first-join.broadcast"));
        assertTrue(config.contains("first-join.private-message"));
        assertTrue(config.contains("first-join.title.enabled"));
        assertTrue(config.contains("first-join.title.fade-in"));
        assertTrue(config.contains("first-join.title.stay"));
        assertTrue(config.contains("first-join.title.fade-out"));
        assertTrue(config.contains("first-join.actionbar.enabled"));

        assertEquals(10L, config.getLong("first-join.title.fade-in"));
        assertEquals(70L, config.getLong("first-join.title.stay"));
        assertEquals(20L, config.getLong("first-join.title.fade-out"));

        // Join keys
        assertTrue(config.contains("join.enabled"));
        assertTrue(config.contains("join.broadcast"));
        assertTrue(config.contains("join.private-message"));
        assertTrue(config.contains("join.title.enabled"));
        assertTrue(config.contains("join.title.fade-in"));
        assertTrue(config.contains("join.title.stay"));
        assertTrue(config.contains("join.title.fade-out"));
        assertTrue(config.contains("join.actionbar.enabled"));

        assertEquals(10L, config.getLong("join.title.fade-in"));
        assertEquals(70L, config.getLong("join.title.stay"));
        assertEquals(20L, config.getLong("join.title.fade-out"));

        // Quit keys
        assertTrue(config.contains("quit.enabled"));
        assertTrue(config.contains("quit.broadcast"));

        // Sound keys
        assertTrue(config.contains("sounds.first-join.enabled"));
        assertTrue(config.contains("sounds.first-join.sound"));
        assertTrue(config.contains("sounds.first-join.volume"));
        assertTrue(config.contains("sounds.first-join.pitch"));

        assertTrue(config.contains("sounds.join.enabled"));
        assertTrue(config.contains("sounds.join.sound"));
        assertTrue(config.contains("sounds.join.volume"));
        assertTrue(config.contains("sounds.join.pitch"));

        assertTrue(config.contains("sounds.quit.enabled"));
        assertTrue(config.contains("sounds.quit.sound"));
        assertTrue(config.contains("sounds.quit.volume"));
        assertTrue(config.contains("sounds.quit.pitch"));
    }

    @Test
    @DisplayName("lang.yml is valid YAML and preserves all required localized message keys")
    void testLangYmlStructureAndKeys() {
        InputStream in = getClass().getClassLoader().getResourceAsStream("lang.yml");
        assertNotNull(in, "lang.yml must exist in classpath");

        YamlConfiguration lang = YamlConfiguration.loadConfiguration(
            new InputStreamReader(in, StandardCharsets.UTF_8)
        );

        // Prefix
        assertTrue(lang.contains("prefix"), "lang.yml must contain prefix");
        assertFalse(lang.getString("prefix", "").isEmpty());

        // First join messages
        assertTrue(lang.contains("first-join.title"), "first-join.title required");
        assertTrue(lang.contains("first-join.subtitle"), "first-join.subtitle required");
        assertTrue(lang.contains("first-join.actionbar"), "first-join.actionbar required");
        assertTrue(lang.contains("first-join.broadcast"), "first-join.broadcast required");
        assertTrue(lang.contains("first-join.private"), "first-join.private required");

        List<String> firstJoinBroadcast = lang.getStringList("first-join.broadcast");
        assertFalse(firstJoinBroadcast.isEmpty(), "first-join.broadcast list must not be empty");

        List<String> firstJoinPrivate = lang.getStringList("first-join.private");
        assertFalse(firstJoinPrivate.isEmpty(), "first-join.private list must not be empty");

        // Join messages
        assertTrue(lang.contains("join.title"), "join.title required");
        assertTrue(lang.contains("join.subtitle"), "join.subtitle required");
        assertTrue(lang.contains("join.actionbar"), "join.actionbar required");
        assertTrue(lang.contains("join.broadcast"), "join.broadcast required");
        assertTrue(lang.contains("join.private"), "join.private required");

        // Quit messages
        assertTrue(lang.contains("quit.broadcast"), "quit.broadcast required");

        // Command messages
        assertTrue(lang.contains("commands.reload"), "commands.reload required");
        assertTrue(lang.contains("commands.reload-success"), "commands.reload-success required");
        assertTrue(lang.contains("commands.reload-fail"), "commands.reload-fail required");
        assertTrue(lang.contains("commands.no-permission"), "commands.no-permission required");
        assertTrue(lang.contains("commands.help"), "commands.help required");
        assertTrue(lang.contains("commands.info"), "commands.info required");

        // Error messages
        assertTrue(lang.contains("errors.player-only"), "errors.player-only required");
        assertTrue(lang.contains("errors.unknown-command"), "errors.unknown-command required");
    }
}
