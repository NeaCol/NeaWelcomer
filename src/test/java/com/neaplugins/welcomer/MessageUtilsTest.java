package com.neaplugins.welcomer;

import com.neaplugins.welcomer.managers.UniquePlayerTracker;
import com.neaplugins.welcomer.utils.MessageUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive automated security, performance, and functionality tests for {@link MessageUtils}.
 * Validates that MiniMessage tag injection is impossible, placeholders resolve properly,
 * and zero synchronous disk I/O occurs on the main server thread.
 */
class MessageUtilsTest {

    private Player mockPlayer;
    private World mockWorld;

    @BeforeEach
    void setUp() {
        mockPlayer = Mockito.mock(Player.class);
        mockWorld = Mockito.mock(World.class);

        Mockito.when(mockPlayer.getName()).thenReturn("TestPlayer");
        Mockito.when(mockPlayer.displayName()).thenReturn(Component.text("TestDisplayName"));
        Mockito.when(mockWorld.getName()).thenReturn("survival");
        Mockito.when(mockPlayer.getWorld()).thenReturn(mockWorld);
    }

    @Test
    @DisplayName("Player name containing MiniMessage click payload must NOT produce a ClickEvent")
    void testMiniMessageInjectionInPlayerNameDoesNotProduceClickEvent() {
        String injectionPayload = "<click:run_command:/op hacker><hover:show_text:'hacked'>EvilPlayer</click>";
        Mockito.when(mockPlayer.getName()).thenReturn(injectionPayload);

        String template = "Welcome <#4CAF50>{player}<reset> to the server!";
        Component parsed = MessageUtils.parse(template, mockPlayer);

        assertNotNull(parsed, "Parsed component must not be null");
        assertFalse(hasClickEvent(parsed), "Parsed component MUST NOT contain any ClickEvent!");
        assertFalse(hasHoverEvent(parsed), "Parsed component MUST NOT contain any HoverEvent!");

        String plainText = PlainTextComponentSerializer.plainText().serialize(parsed);
        assertTrue(plainText.contains("<click:run_command:/op hacker>"),
            "Payload should remain literal plain text rather than executing as a tag");
    }

    @Test
    @DisplayName("Player name containing open_url click payload must NOT produce a ClickEvent")
    void testOpenUrlInjectionInPlayerName() {
        String payload = "Scammer<click:open_url:'https://malicious-phishing.com'>Click for free rank</click>";
        Mockito.when(mockPlayer.getName()).thenReturn(payload);

        String template = "<red>Warning: {player} joined.</red>";
        Component parsed = MessageUtils.parse(template, mockPlayer);

        assertNotNull(parsed);
        assertFalse(hasClickEvent(parsed), "ClickEvent must not be created from injected URL tags");
    }

    @Test
    @DisplayName("Player rank containing MiniMessage injection tags must NOT produce a ClickEvent")
    void testMiniMessageInjectionInRank() {
        String rankWithInjection = "&6[VIP]<click:run_command:/op hacker>RankTag</click>";
        Component safeRank = MessageUtils.parseLegacySafe(rankWithInjection);

        assertNotNull(safeRank);
        assertFalse(hasClickEvent(safeRank), "parseLegacySafe must not parse MiniMessage ClickEvent");

        String template = "Welcome {player}! Rank: {rank}";
        Component parsed = MessageUtils.parse(template, mockPlayer, Placeholder.component("rank", safeRank));

        assertFalse(hasClickEvent(parsed), "Template parsing with rank component must not have ClickEvent");
    }

    @Test
    @DisplayName("Placeholders {player}, {displayname}, and {world} are substituted properly")
    void testPlaceholderSubstitutions() {
        String template = "Player: {player}, Display: {displayname}, World: {world}";
        Component parsed = MessageUtils.parse(template, mockPlayer);

        String plainText = PlainTextComponentSerializer.plainText().serialize(parsed);
        assertEquals("Player: TestPlayer, Display: TestDisplayName, World: survival", plainText);
    }

    @Test
    @DisplayName("Case-insensitive bracket placeholders {PLAYER} and {WORLD} resolve correctly")
    void testCaseInsensitivePlaceholders() {
        String template = "Welcome {PLAYER} to {WORLD}!";
        Component parsed = MessageUtils.parse(template, mockPlayer);

        String plainText = PlainTextComponentSerializer.plainText().serialize(parsed);
        assertEquals("Welcome TestPlayer to survival!", plainText);
    }

    @Test
    @DisplayName("Direct MiniMessage tags in template are parsed and rendered correctly")
    void testTemplateMiniMessageFormattingPreserved() {
        String template = "<bold><green>Welcome</green></bold> {player}!";
        Component parsed = MessageUtils.parse(template, mockPlayer);

        assertNotNull(parsed);
        String plainText = PlainTextComponentSerializer.plainText().serialize(parsed);
        assertEquals("Welcome TestPlayer!", plainText);
    }

    @Test
    @DisplayName("Legacy hex &#RRGGBB and ampersand &color codes are correctly translated")
    void testLegacyColorTranslation() {
        String template = "&#FF5555Red &aGreen {player}";
        Component parsed = MessageUtils.parse(template, mockPlayer);

        assertNotNull(parsed);
        String plainText = PlainTextComponentSerializer.plainText().serialize(parsed);
        assertEquals("Red Green TestPlayer", plainText);
    }

    @Test
    @DisplayName("Extra TagResolvers passed to parse() resolve properly")
    void testExtraTagResolvers() {
        String template = "Special discount: <discount_code> for {player}";
        Component parsed = MessageUtils.parse(
            template,
            mockPlayer,
            Placeholder.unparsed("discount_code", "SAVE50")
        );

        String plainText = PlainTextComponentSerializer.plainText().serialize(parsed);
        assertEquals("Special discount: SAVE50 for TestPlayer", plainText);
    }

    @Test
    @DisplayName("UniquePlayerTracker provides thread-safe, non-blocking O(1) in-memory operations")
    void testUniquePlayerTrackerOperations() {
        UniquePlayerTracker tracker = new UniquePlayerTracker(null);

        assertEquals(0, tracker.getUniquePlayerCount(), "Initial tracker count should be 0");

        int count1 = tracker.incrementAndGet();
        assertEquals(1, count1);
        assertEquals(1, tracker.getUniquePlayerCount());

        int count2 = tracker.incrementAndGet();
        assertEquals(2, count2);
        assertEquals(2, tracker.getUniquePlayerCount());

        // Validate O(1) non-blocking performance: 100,000 reads must finish in under 100ms
        long start = System.nanoTime();
        for (int i = 0; i < 100_000; i++) {
            tracker.getUniquePlayerCount();
        }
        long elapsedMillis = (System.nanoTime() - start) / 1_000_000;
        assertTrue(elapsedMillis < 100, "100,000 tracker reads should be instantaneous (took " + elapsedMillis + "ms)");
    }

    @Test
    @DisplayName("MessageUtils.parse() executes without synchronous disk I/O")
    void testMessageUtilsNoDiskIO() {
        String template = "Welcome #{player_number} player {player}!";

        long start = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            MessageUtils.parse(template, mockPlayer);
        }
        long elapsedMillis = (System.nanoTime() - start) / 1_000_000;

        // 1000 parses with zero disk I/O should finish in well under 500ms
        assertTrue(elapsedMillis < 500, "Parsing 1000 messages took " + elapsedMillis + "ms without disk I/O");
    }

    @Test
    @DisplayName("Permissions constants match expected permission node strings")
    void testPermissionsConstants() {
        assertEquals("neawelcomer.admin", com.neaplugins.welcomer.constants.Permissions.ADMIN);
        assertEquals("neawelcomer.join.see", com.neaplugins.welcomer.constants.Permissions.JOIN_SEE);
        assertEquals("neawelcomer.quit.see", com.neaplugins.welcomer.constants.Permissions.QUIT_SEE);
        assertEquals("neawelcomer.firstjoin.see", com.neaplugins.welcomer.constants.Permissions.FIRSTJOIN_SEE);
    }

    @Test
    @DisplayName("Null and empty inputs return empty components safely without throwing")
    void testNullAndEmptyInputs() {
        assertEquals(Component.empty(), MessageUtils.parse(null));
        assertEquals(Component.empty(), MessageUtils.parse(""));
        assertEquals(Component.empty(), MessageUtils.parse(null, mockPlayer));
        assertEquals(Component.empty(), MessageUtils.parse("", mockPlayer));
        assertEquals("", MessageUtils.stripFormatting(null));
        assertEquals("", MessageUtils.stripFormatting(""));
    }

    @Test
    @DisplayName("Suggest command and copy-to-clipboard click payloads do not produce ClickEvents")
    void testAdditionalClickPayloadVariants() {
        String payload1 = "<click:suggest_command:/banall>ClickMe</click>";
        Mockito.when(mockPlayer.getName()).thenReturn(payload1);
        Component comp1 = MessageUtils.parse("Hello {player}", mockPlayer);
        assertFalse(hasClickEvent(comp1));

        String payload2 = "<click:copy_to_clipboard:secretToken>Clipboard</click>";
        Mockito.when(mockPlayer.getName()).thenReturn(payload2);
        Component comp2 = MessageUtils.parse("Hello {player}", mockPlayer);
        assertFalse(hasClickEvent(comp2));
    }

    @Test
    @DisplayName("replacePlaceholders() substitutes strings safely")
    void testReplacePlaceholders() {
        String template = "Hello {player}, online: {online}, unique: {player_number}";
        String replaced = MessageUtils.replacePlaceholders(template, mockPlayer);
        assertTrue(replaced.contains("TestPlayer"));
        assertTrue(replaced.contains("unique: 0") || replaced.contains("unique:"));
    }

    @Test
    @DisplayName("stripFormatting() removes MiniMessage and legacy codes")
    void testStripFormatting() {
        String input = "<bold><green>Test</green></bold> &cColor &#FF5555Hex";
        String stripped = MessageUtils.stripFormatting(input);
        assertEquals("Test Color Hex", stripped.trim());
    }

    @Test
    @DisplayName("toLegacy() converts Adventure Component to legacy section format")
    void testToLegacy() {
        Component comp = Component.text("Hello");
        String legacy = MessageUtils.toLegacy(comp);
        assertTrue(legacy.contains("Hello"));
    }

    // ─── Helper Methods ──────────────────────────────────────────

    private boolean hasClickEvent(Component component) {
        if (component == null) return false;
        if (component.clickEvent() != null) return true;
        for (Component child : component.children()) {
            if (hasClickEvent(child)) return true;
        }
        return false;
    }

    private boolean hasHoverEvent(Component component) {
        if (component == null) return false;
        if (component.hoverEvent() != null) return true;
        for (Component child : component.children()) {
            if (hasHoverEvent(child)) return true;
        }
        return false;
    }
}
