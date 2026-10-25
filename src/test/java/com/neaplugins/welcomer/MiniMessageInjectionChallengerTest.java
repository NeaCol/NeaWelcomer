package com.neaplugins.welcomer;

import com.neaplugins.welcomer.utils.MessageUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Empirical Adversarial Challenger Test Suite: MiniMessage Tag Injection.
 * <p>
 * Exhaustively stress-tests all untrusted user input vectors ({player}, {displayname}, {rank},
 * and bracket manipulation) against malicious Adventure MiniMessage payloads.
 */
class MiniMessageInjectionChallengerTest {

    private Player mockPlayer;
    private World mockWorld;

    @BeforeEach
    void setUp() {
        mockPlayer = Mockito.mock(Player.class);
        mockWorld = Mockito.mock(World.class);

        Mockito.when(mockPlayer.getName()).thenReturn("NormalPlayer");
        Mockito.when(mockPlayer.displayName()).thenReturn(Component.text("NormalDisplayName"));
        Mockito.when(mockWorld.getName()).thenReturn("survival");
        Mockito.when(mockPlayer.getWorld()).thenReturn(mockWorld);
    }

    // ─── 1. Click Event Injection Vectors in Player Name ───────────

    @ParameterizedTest(name = "Click payload variant: {0}")
    @ValueSource(strings = {
        "<click:run_command:/op hacker>",
        "<click:run_command:/op hacker>Evil</click>",
        "<click:run_command:'/op hacker'>Evil</click>",
        "<click:run_command:\"/op hacker\">Evil</click>",
        "<click:open_url:'https://malicious-phishing.com'>FreeRank</click>",
        "<click:open_url:\"https://attacker.net\">Claim</click>",
        "<click:suggest_command:'/banall'>Suggest</click>",
        "<click:suggest_command:\"/stop\">Stop</click>",
        "<click:copy_to_clipboard:'sensitive_secret_token'>StealToken</click>",
        "<click:copy_to_clipboard:\"payload\">Copy</click>",
        "<click:change_page:2>Page</click>",
        "<cLiCk:RuN_cOmMaNd:/op>MixedCase</cLiCk>",
        "<CLICK:RUN_COMMAND:/OP>UpperCase</CLICK>",
        "<click:run_command:/op><click:run_command:/deop>Nested</click></click>",
        "</click><click:run_command:/op hacker>Breakout",
        "<<click:run_command:/op>>DoubleBracket",
        "\\<click:run_command:/op\\>Escaped",
        "<click : run_command : '/op hacker' >Spaces"
    })
    @DisplayName("Player name with click tag MUST NOT produce any ClickEvent")
    void testClickEventInjectionVariantsInPlayerName(String payload) {
        Mockito.when(mockPlayer.getName()).thenReturn(payload);

        String template = "Welcome <gold>{player}</gold> to our server!";
        Component parsed = MessageUtils.parse(template, mockPlayer);

        assertNotNull(parsed, "Parsed component must not be null");
        assertFalse(hasClickEvent(parsed), "Component MUST NOT contain ClickEvent for payload: " + payload);

        // Verify payload is retained literally as plain text
        String plain = PlainTextComponentSerializer.plainText().serialize(parsed);
        assertTrue(plain.contains("Evil") || plain.contains("FreeRank") || plain.contains("Claim")
                || plain.contains("Suggest") || plain.contains("Stop") || plain.contains("StealToken")
                || plain.contains("Copy") || plain.contains("Page") || plain.contains("MixedCase")
                || plain.contains("UpperCase") || plain.contains("Nested") || plain.contains("Breakout")
                || plain.contains("DoubleBracket") || plain.contains("Escaped") || plain.contains("Spaces")
                || plain.contains("<click:run_command:/op hacker>"),
            "Payload text must be preserved literally");
    }

    // ─── 2. Hover Event Injection Vectors in Player Name ───────────

    @ParameterizedTest(name = "Hover payload variant: {0}")
    @ValueSource(strings = {
        "<hover:show_text:'malicious tooltip'>TooltipPlayer</hover>",
        "<hover:show_text:\"another tooltip\">DoubleQuotePlayer</hover>",
        "<hover:show_text:'<click:run_command:/op hacker>NestedClick</click>'>NestedPlayer</hover>",
        "<hover:show_text:'<red>RedHover</red>'>ColorHoverPlayer</hover>",
        "</hover>UnmatchedClosingHover",
        "<hOvEr:ShOw_TeXt:'test'>MixedCaseHover</hOvEr>"
    })
    @DisplayName("Player name with hover tag MUST NOT produce any HoverEvent")
    void testHoverEventInjectionVariantsInPlayerName(String payload) {
        Mockito.when(mockPlayer.getName()).thenReturn(payload);

        String template = "Welcome {player}!";
        Component parsed = MessageUtils.parse(template, mockPlayer);

        assertNotNull(parsed);
        assertFalse(hasHoverEvent(parsed), "Component MUST NOT contain HoverEvent for payload: " + payload);
        assertFalse(hasClickEvent(parsed), "Component MUST NOT contain ClickEvent for payload: " + payload);
    }

    // ─── 3. Font, Color, Style Injection Vectors in Player Name ────

    @ParameterizedTest(name = "Style payload variant: {0}")
    @ValueSource(strings = {
        "<font:custom:malicious_font>CustomFontPlayer</font>",
        "<font:minecraft:uniform>UniformPlayer</font>",
        "<color:#123456>HexPlayer</color>",
        "<#ff0055>ShortHexPlayer</#ff0055>",
        "<red>RedPlayer</red>",
        "<rainbow>RainbowPlayer</rainbow>",
        "<gradient:#ff0000:#00ff00>GradientPlayer</gradient>",
        "<bold><italic><underlined><strikethrough><obfuscated>StyledPlayer",
        "<reset>ResetPlayer",
        "<insertion:malicious_paste>InsertionPlayer</insertion>",
        "<key:key.jump>KeybindPlayer",
        "<translatable:chat.cannotSend>TranslatablePlayer"
    })
    @DisplayName("Player name with formatting/font/color tags MUST be rendered as literal text")
    void testStyleAndFontInjectionInPlayerName(String payload) {
        Mockito.when(mockPlayer.getName()).thenReturn(payload);

        String template = "Player: {player}";
        Component parsed = MessageUtils.parse(template, mockPlayer);

        assertNotNull(parsed);
        assertFalse(hasClickEvent(parsed));
        assertFalse(hasHoverEvent(parsed));

        // The serialized plain text must contain the literal tags because Placeholder.unparsed does not interpret them
        String plain = PlainTextComponentSerializer.plainText().serialize(parsed);
        assertTrue(plain.contains("<") && plain.contains(">"),
            "Tags must be preserved literally in plain text output without evaluation");
    }

    // ─── 4. Recursive Placeholder & Bracket Manipulation Vectors ───

    @Test
    @DisplayName("Player name containing {player} placeholder does NOT cause recursion or crash")
    void testRecursivePlayerPlaceholder() {
        Mockito.when(mockPlayer.getName()).thenReturn("{player}");

        String template = "Welcome {player} to the server!";
        Component parsed = MessageUtils.parse(template, mockPlayer);

        assertNotNull(parsed);
        String plain = PlainTextComponentSerializer.plainText().serialize(parsed);
        assertEquals("Welcome {player} to the server!", plain);
    }

    @Test
    @DisplayName("Player name containing <player> tag does NOT cause recursion or tag resolution")
    void testTagInjectionMatchingTagResolverName() {
        Mockito.when(mockPlayer.getName()).thenReturn("<player>");

        String template = "Welcome {player} to the server!";
        Component parsed = MessageUtils.parse(template, mockPlayer);

        assertNotNull(parsed);
        String plain = PlainTextComponentSerializer.plainText().serialize(parsed);
        assertEquals("Welcome <player> to the server!", plain);
    }

    @Test
    @DisplayName("Player name containing {displayname} or {rank} does NOT resolve other placeholders")
    void testCrossPlaceholderInjectionInPlayerName() {
        Mockito.when(mockPlayer.getName()).thenReturn("{rank}_{displayname}_{world}");

        String template = "Join: {player}";
        Component parsed = MessageUtils.parse(template, mockPlayer);

        assertNotNull(parsed);
        String plain = PlainTextComponentSerializer.plainText().serialize(parsed);
        assertEquals("Join: {rank}_{displayname}_{world}", plain);
    }

    @Test
    @DisplayName("Double bracket manipulation {{player}} is handled gracefully without crashing")
    void testDoubleBracketNormalization() {
        Mockito.when(mockPlayer.getName()).thenReturn("TestUser");

        String template = "Hello {{player}}!";
        Component parsed = MessageUtils.parse(template, mockPlayer);

        assertNotNull(parsed);
        String plain = PlainTextComponentSerializer.plainText().serialize(parsed);
        // {player} inside {} gets normalized to {<player>}, which renders as {TestUser}
        assertTrue(plain.contains("TestUser"), "Should safely contain player name: " + plain);
    }

    @Test
    @DisplayName("Malformed bracket variations do not throw unhandled exceptions")
    void testMalformedBracketVariations() {
        assertDoesNotThrow(() -> {
            MessageUtils.parse("Welcome {", mockPlayer);
            MessageUtils.parse("Welcome }", mockPlayer);
            MessageUtils.parse("Welcome {}", mockPlayer);
            MessageUtils.parse("Welcome {unknown_tag}", mockPlayer);
            MessageUtils.parse("Welcome {player", mockPlayer);
            MessageUtils.parse("Welcome player}", mockPlayer);
            MessageUtils.parse("Welcome <{player}>", mockPlayer);
            MessageUtils.parse("Welcome {<player>}", mockPlayer);
        });
    }

    // ─── 5. DisplayName Vector Attacks ─────────────────────────────

    @Test
    @DisplayName("DisplayName component containing malicious MiniMessage text does NOT execute as tags")
    void testDisplayNameWithMiniMessageString() {
        Component evilDisplayName = Component.text("<click:run_command:/op hacker><hover:show_text:'x'>EvilNick</click>");
        Mockito.when(mockPlayer.displayName()).thenReturn(evilDisplayName);

        String template = "Welcome {displayname}!";
        Component parsed = MessageUtils.parse(template, mockPlayer);

        assertNotNull(parsed);
        assertFalse(hasClickEvent(parsed), "DisplayName containing MiniMessage text MUST NOT create ClickEvent");
        assertFalse(hasHoverEvent(parsed), "DisplayName containing MiniMessage text MUST NOT create HoverEvent");
    }

    // ─── 6. Rank Vector Attacks ────────────────────────────────────

    @ParameterizedTest(name = "Rank injection: {0}")
    @ValueSource(strings = {
        "&c[Admin]<click:run_command:/op hacker>ClickRank</click>",
        "§4[Owner]<click:open_url:'https://bad.com'>URLRank</click>",
        "[VIP]<click:run_command:/op hacker>PlainRank</click>",
        "<click:run_command:/op hacker>RawRank",
        "&a[VIP]<hover:show_text:'malicious'>HoverRank</hover>"
    })
    @DisplayName("Rank strings containing MiniMessage tags MUST NOT produce Click or Hover events")
    void testRankMiniMessageInjections(String maliciousRank) {
        Component safeRank = MessageUtils.parseLegacySafe(maliciousRank);
        assertFalse(hasClickEvent(safeRank));
        assertFalse(hasHoverEvent(safeRank));

        String template = "Player: {player} | Rank: {rank}";
        Component parsed = MessageUtils.parse(template, mockPlayer, Placeholder.component("rank", safeRank));

        assertNotNull(parsed);
        assertFalse(hasClickEvent(parsed), "Rank MUST NOT produce ClickEvent for: " + maliciousRank);
        assertFalse(hasHoverEvent(parsed), "Rank MUST NOT produce HoverEvent for: " + maliciousRank);
    }

    // ─── 7. Legitimate Template Tags With Malicious User Input ──────

    @Test
    @DisplayName("Legitimate template click event is preserved while player malicious payload is neutralized")
    void testLegitimateTemplateClickWithMaliciousPlayerName() {
        String maliciousName = "<click:run_command:/op hacker>InjectedPlayer</click>";
        Mockito.when(mockPlayer.getName()).thenReturn(maliciousName);

        String template = "<click:open_url:'https://legit-server.net'>Visit our website</click> - Welcome {player}!";
        Component parsed = MessageUtils.parse(template, mockPlayer);

        assertNotNull(parsed);

        // Template's legitimate click event must exist
        assertTrue(hasClickEvent(parsed), "Legitimate click event on template must be preserved");

        // But find the specific click event and assert it is ONLY the open_url to the legitimate website, NOT /op hacker!
        assertNoRunCommandClickEvent(parsed);
    }

    // ─── Helper Inspection Methods ─────────────────────────────────

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

    private void assertNoRunCommandClickEvent(Component component) {
        if (component == null) return;
        ClickEvent click = component.clickEvent();
        if (click != null) {
            assertNotEquals(ClickEvent.Action.RUN_COMMAND, click.action(),
                "Found prohibited RUN_COMMAND ClickEvent: " + click.value());
            assertFalse(click.value().contains("/op"),
                "ClickEvent value contains /op: " + click.value());
        }
        for (Component child : component.children()) {
            assertNoRunCommandClickEvent(child);
        }
    }
}
