package com.neaplugins.welcomer;

import com.neaplugins.welcomer.constants.Permissions;
import com.neaplugins.welcomer.listeners.PlayerConnectionListener;
import com.neaplugins.welcomer.managers.ConfigManager;
import com.neaplugins.welcomer.managers.LangManager;
import com.neaplugins.welcomer.managers.UniquePlayerTracker;
import com.neaplugins.welcomer.utils.MessageUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.util.Ticks;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Empirical Adversarial Challenger Test Suite: Titles, Subtitles & ActionBars (Milestone 2 - R2).
 * <p>
 * Rigorously stress-tests:
 * 1. Title & Subtitle delivery with Adventure Title.Times (0 ticks, large ticks, asymmetric ticks).
 * 2. Missing title, missing subtitle, and empty templates.
 * 3. Strict suppression when config toggles are set to false (title.enabled, actionbar.enabled).
 * 4. Fallback routing when first-join is disabled.
 * 5. MiniMessage injection immunity in Title and Subtitle components.
 * 6. Offline player safety check when 5-tick delayed task fires.
 */
class TitleActionBarChallengerTest {

    private NeaWelcomer plugin;
    private ChallengerConfigManager configManager;
    private ChallengerLangManager langManager;
    private UniquePlayerTracker playerTracker;

    private Server mockServer;
    private ConsoleCommandSender mockConsole;
    private BukkitScheduler mockScheduler;
    private PluginManager mockPluginManager;

    private Player joiningPlayer;

    static class ChallengerConfigManager extends ConfigManager {
        boolean disableDefault = true;

        boolean firstJoinEnabled = true;
        boolean firstJoinBroadcast = true;
        boolean firstJoinTitleEnabled = true;
        long firstJoinTitleFadeIn = 10L;
        long firstJoinTitleStay = 70L;
        long firstJoinTitleFadeOut = 20L;
        boolean firstJoinActionBarEnabled = true;
        boolean firstJoinSound = false;

        boolean joinEnabled = true;
        boolean joinBroadcast = true;
        boolean joinTitleEnabled = true;
        long joinTitleFadeIn = 10L;
        long joinTitleStay = 70L;
        long joinTitleFadeOut = 20L;
        boolean joinActionBarEnabled = true;
        boolean joinSound = false;

        boolean quitEnabled = true;
        boolean quitBroadcast = true;

        ChallengerConfigManager(NeaWelcomer plugin) {
            super(plugin);
        }

        @Override public boolean isDisableDefaultMessages() { return disableDefault; }

        @Override public boolean isFirstJoinEnabled() { return firstJoinEnabled; }
        @Override public boolean isFirstJoinBroadcast() { return firstJoinBroadcast; }
        @Override public boolean isFirstJoinTitleEnabled() { return firstJoinTitleEnabled; }
        @Override public long getFirstJoinTitleFadeIn() { return firstJoinTitleFadeIn; }
        @Override public long getFirstJoinTitleStay() { return firstJoinTitleStay; }
        @Override public long getFirstJoinTitleFadeOut() { return firstJoinTitleFadeOut; }
        @Override public boolean isFirstJoinActionBarEnabled() { return firstJoinActionBarEnabled; }
        @Override public boolean isFirstJoinSoundEnabled() { return firstJoinSound; }

        @Override public boolean isJoinEnabled() { return joinEnabled; }
        @Override public boolean isJoinBroadcast() { return joinBroadcast; }
        @Override public boolean isJoinTitleEnabled() { return joinTitleEnabled; }
        @Override public long getJoinTitleFadeIn() { return joinTitleFadeIn; }
        @Override public long getJoinTitleStay() { return joinTitleStay; }
        @Override public long getJoinTitleFadeOut() { return joinTitleFadeOut; }
        @Override public boolean isJoinActionBarEnabled() { return joinActionBarEnabled; }
        @Override public boolean isJoinSoundEnabled() { return joinSound; }

        @Override public boolean isQuitEnabled() { return quitEnabled; }
        @Override public boolean isQuitBroadcast() { return quitBroadcast; }
    }

    static class ChallengerLangManager extends LangManager {
        String firstJoinTitle = "<gradient:#7C4DFF:#B388FF><bold>¡BIENVENIDO!</bold></gradient>";
        String firstJoinSubtitle = "<#E0E0E0>Disfruta tu estadía en el servidor";
        String firstJoinActionBar = "<#B388FF>¡Eres el jugador <#FFD54F>#{player_number}<#B388FF> en unirse!";

        String joinTitle = "<gradient:#7C4DFF:#00BCD4><bold>¡HOLA DE NUEVO!</bold></gradient>";
        String joinSubtitle = "<#E0E0E0>Bienvenido de vuelta, <#7C4DFF>{player}";
        String joinActionBar = "<#4CAF50>¡Bienvenido de nuevo, <#E0E0E0>{player}<#4CAF50>!";

        ChallengerLangManager(NeaWelcomer plugin) {
            super(plugin);
        }

        @Override
        public String getMessage(String path) {
            return switch (path) {
                case "first-join.title" -> firstJoinTitle;
                case "first-join.subtitle" -> firstJoinSubtitle;
                case "first-join.actionbar" -> firstJoinActionBar;
                case "join.title" -> joinTitle;
                case "join.subtitle" -> joinSubtitle;
                case "join.actionbar" -> joinActionBar;
                default -> "";
            };
        }

        @Override
        public List<String> getMessageList(String path) {
            return switch (path) {
                case "first-join.broadcast" -> Collections.singletonList("First join broadcast");
                case "first-join.private" -> Collections.singletonList("First join private message");
                case "join.broadcast" -> Collections.singletonList("Normal join broadcast");
                case "join.private" -> Collections.singletonList("Normal join private message");
                default -> Collections.emptyList();
            };
        }

        @Override
        public String getPrefix() {
            return "<purple>NeaWelcomer » </purple>";
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);
        plugin = (NeaWelcomer) unsafe.allocateInstance(NeaWelcomer.class);

        configManager = new ChallengerConfigManager(plugin);
        langManager = new ChallengerLangManager(plugin);
        playerTracker = new UniquePlayerTracker(plugin);

        setField(plugin, NeaWelcomer.class, "configManager", configManager);
        setField(plugin, NeaWelcomer.class, "langManager", langManager);
        setField(plugin, NeaWelcomer.class, "playerTracker", playerTracker);

        Field instanceField = NeaWelcomer.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, plugin);

        Logger testLogger = Logger.getLogger("NeaWelcomerChallengerTest");
        setFieldInHierarchy(plugin, "logger", testLogger);

        mockServer = Mockito.mock(Server.class);
        mockConsole = Mockito.mock(ConsoleCommandSender.class);
        mockScheduler = Mockito.mock(BukkitScheduler.class);
        mockPluginManager = Mockito.mock(PluginManager.class);

        when(mockServer.getConsoleSender()).thenReturn(mockConsole);
        when(mockServer.getScheduler()).thenReturn(mockScheduler);
        when(mockServer.getPluginManager()).thenReturn(mockPluginManager);
        when(mockServer.getLogger()).thenReturn(testLogger);
        doReturn(Collections.emptyList()).when(mockServer).getOnlinePlayers();

        // Run delayed tasks immediately in test thread
        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(1);
            if (task != null) {
                task.run();
            }
            return null;
        }).when(mockScheduler).runTaskLater(any(), any(Runnable.class), anyLong());

        joiningPlayer = Mockito.mock(Player.class);
        when(joiningPlayer.getName()).thenReturn("ChallengerPlayer");
        when(joiningPlayer.displayName()).thenReturn(Component.text("ChallengerPlayer"));
        when(joiningPlayer.isOnline()).thenReturn(true);

        Field serverField = Bukkit.class.getDeclaredField("server");
        serverField.setAccessible(true);
        serverField.set(null, mockServer);
    }

    @AfterEach
    void tearDown() throws Exception {
        Field instanceField = NeaWelcomer.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);

        try {
            Field serverField = Bukkit.class.getDeclaredField("server");
            serverField.setAccessible(true);
            serverField.set(null, null);
        } catch (Throwable ignored) {}
    }

    private static void setField(Object target, Class<?> clazz, String fieldName, Object value) throws Exception {
        Field f = clazz.getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    private static void setFieldInHierarchy(Object target, String fieldName, Object value) {
        Class<?> current = target.getClass();
        while (current != null && current != Object.class) {
            try {
                Field f = current.getDeclaredField(fieldName);
                f.setAccessible(true);
                f.set(target, value);
                return;
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            } catch (Exception e) {
                break;
            }
        }
    }

    // ─── 1. Title.Times Edge Cases (0 Ticks, Large Ticks, Asymmetric) ───

    @Test
    @DisplayName("Zero ticks for fadeIn, stay, and fadeOut produces valid Duration.ZERO in Title.Times without error")
    void testZeroTicksDurationInTitleTimes() {
        when(joiningPlayer.hasPlayedBefore()).thenReturn(false);

        configManager.firstJoinTitleFadeIn = 0L;
        configManager.firstJoinTitleStay = 0L;
        configManager.firstJoinTitleFadeOut = 0L;

        PlayerConnectionListener listener = new PlayerConnectionListener(plugin);
        PlayerJoinEvent event = new PlayerJoinEvent(joiningPlayer, Component.text("join"));

        assertDoesNotThrow(() -> listener.onPlayerJoin(event));

        ArgumentCaptor<Title> captor = ArgumentCaptor.forClass(Title.class);
        verify(joiningPlayer, times(1)).showTitle(captor.capture());
        Title title = captor.getValue();
        assertNotNull(title);
        assertNotNull(title.times());

        assertEquals(Duration.ZERO, title.times().fadeIn(), "0 ticks fadeIn must equal Duration.ZERO");
        assertEquals(Duration.ZERO, title.times().stay(), "0 ticks stay must equal Duration.ZERO");
        assertEquals(Duration.ZERO, title.times().fadeOut(), "0 ticks fadeOut must equal Duration.ZERO");
    }

    @ParameterizedTest(name = "Large ticks combination: fadeIn={0}, stay={1}, fadeOut={2}")
    @CsvSource({
        "100000, 500000, 100000",
        "1000000, 2000000, 1000000",
        "0, 1000000, 0",
        "1, 1, 1"
    })
    @DisplayName("Extreme and asymmetric tick values configure accurate Title.Times durations")
    void testExtremeAndAsymmetricTickValues(long fadeIn, long stay, long fadeOut) {
        when(joiningPlayer.hasPlayedBefore()).thenReturn(true);

        configManager.joinTitleFadeIn = fadeIn;
        configManager.joinTitleStay = stay;
        configManager.joinTitleFadeOut = fadeOut;

        PlayerConnectionListener listener = new PlayerConnectionListener(plugin);
        PlayerJoinEvent event = new PlayerJoinEvent(joiningPlayer, Component.text("join"));

        assertDoesNotThrow(() -> listener.onPlayerJoin(event));

        ArgumentCaptor<Title> captor = ArgumentCaptor.forClass(Title.class);
        verify(joiningPlayer, times(1)).showTitle(captor.capture());
        Title title = captor.getValue();
        assertNotNull(title.times());

        assertEquals(Ticks.duration(fadeIn), title.times().fadeIn());
        assertEquals(Ticks.duration(stay), title.times().stay());
        assertEquals(Ticks.duration(fadeOut), title.times().fadeOut());
    }

    @Test
    @DisplayName("Negative tick values in config are passed to Title.Times without unhandled exception")
    void testNegativeTickValuesHandledGracefully() {
        when(joiningPlayer.hasPlayedBefore()).thenReturn(true);

        configManager.joinTitleFadeIn = -10L;
        configManager.joinTitleStay = -20L;
        configManager.joinTitleFadeOut = -5L;

        PlayerConnectionListener listener = new PlayerConnectionListener(plugin);
        PlayerJoinEvent event = new PlayerJoinEvent(joiningPlayer, Component.text("join"));

        assertDoesNotThrow(() -> listener.onPlayerJoin(event));

        ArgumentCaptor<Title> captor = ArgumentCaptor.forClass(Title.class);
        verify(joiningPlayer, times(1)).showTitle(captor.capture());
        Title title = captor.getValue();
        assertNotNull(title.times());
    }

    // ─── 2. Missing Subtitle and Missing Title Edge Cases ───────────────

    @Test
    @DisplayName("Missing/empty subtitle results in Component.empty() and still delivers Title with times")
    void testMissingSubtitleDeliversTitleWithEmptySubtitle() {
        when(joiningPlayer.hasPlayedBefore()).thenReturn(false);
        langManager.firstJoinSubtitle = ""; // Empty subtitle

        PlayerConnectionListener listener = new PlayerConnectionListener(plugin);
        PlayerJoinEvent event = new PlayerJoinEvent(joiningPlayer, Component.text("join"));

        assertDoesNotThrow(() -> listener.onPlayerJoin(event));

        ArgumentCaptor<Title> captor = ArgumentCaptor.forClass(Title.class);
        verify(joiningPlayer, times(1)).showTitle(captor.capture());
        Title title = captor.getValue();

        assertNotNull(title);
        assertEquals(Component.empty(), title.subtitle(), "Missing subtitle must resolve to Component.empty()");
        assertNotEquals(Component.empty(), title.title(), "Title component must still be present");
        assertNotNull(title.times(), "Times must still be populated");
    }

    @Test
    @DisplayName("Null subtitle from LangManager gracefully defaults to Component.empty()")
    void testNullSubtitleHandledGracefully() {
        when(joiningPlayer.hasPlayedBefore()).thenReturn(true);
        langManager.joinSubtitle = null;

        PlayerConnectionListener listener = new PlayerConnectionListener(plugin);
        PlayerJoinEvent event = new PlayerJoinEvent(joiningPlayer, Component.text("join"));

        assertDoesNotThrow(() -> listener.onPlayerJoin(event));

        ArgumentCaptor<Title> captor = ArgumentCaptor.forClass(Title.class);
        verify(joiningPlayer, times(1)).showTitle(captor.capture());
        Title title = captor.getValue();

        assertNotNull(title);
        assertEquals(Component.empty(), title.subtitle());
        assertNotEquals(Component.empty(), title.title());
    }

    @Test
    @DisplayName("Missing title with present subtitle delivers empty title and non-empty subtitle")
    void testMissingTitleWithPresentSubtitle() {
        when(joiningPlayer.hasPlayedBefore()).thenReturn(false);
        langManager.firstJoinTitle = ""; // Empty title

        PlayerConnectionListener listener = new PlayerConnectionListener(plugin);
        PlayerJoinEvent event = new PlayerJoinEvent(joiningPlayer, Component.text("join"));

        assertDoesNotThrow(() -> listener.onPlayerJoin(event));

        ArgumentCaptor<Title> captor = ArgumentCaptor.forClass(Title.class);
        verify(joiningPlayer, times(1)).showTitle(captor.capture());
        Title title = captor.getValue();

        assertNotNull(title);
        assertEquals(Component.empty(), title.title(), "Missing title must resolve to Component.empty()");
        assertNotEquals(Component.empty(), title.subtitle(), "Subtitle component must still be present");
    }

    // ─── 3. Empty Templates Stress Test ────────────────────────────────

    @Test
    @DisplayName("Empty title and empty subtitle templates deliver empty Title without runtime exception")
    void testEmptyTitleAndSubtitleTemplates() {
        when(joiningPlayer.hasPlayedBefore()).thenReturn(false);
        langManager.firstJoinTitle = "";
        langManager.firstJoinSubtitle = "";

        PlayerConnectionListener listener = new PlayerConnectionListener(plugin);
        PlayerJoinEvent event = new PlayerJoinEvent(joiningPlayer, Component.text("join"));

        assertDoesNotThrow(() -> listener.onPlayerJoin(event));

        ArgumentCaptor<Title> captor = ArgumentCaptor.forClass(Title.class);
        verify(joiningPlayer, times(1)).showTitle(captor.capture());
        Title title = captor.getValue();
        assertEquals(Component.empty(), title.title());
        assertEquals(Component.empty(), title.subtitle());
    }

    @Test
    @DisplayName("Null and empty action bar templates strictly suppress sendActionBar")
    void testEmptyAndNullActionBarTemplatesSuppressed() {
        when(joiningPlayer.hasPlayedBefore()).thenReturn(false);

        // Case A: empty string
        langManager.firstJoinActionBar = "";
        PlayerConnectionListener listener = new PlayerConnectionListener(plugin);
        listener.onPlayerJoin(new PlayerJoinEvent(joiningPlayer, Component.text("join")));
        verify(joiningPlayer, never()).sendActionBar(any(Component.class));

        // Case B: null string
        langManager.firstJoinActionBar = null;
        listener.onPlayerJoin(new PlayerJoinEvent(joiningPlayer, Component.text("join")));
        verify(joiningPlayer, never()).sendActionBar(any(Component.class));
    }

    // ─── 4. Strict Toggle Suppression (title.enabled, actionbar.enabled) ──

    @Test
    @DisplayName("first-join.title.enabled = false strictly suppresses Title delivery while keeping ActionBar")
    void testFirstJoinTitleDisabledKeepsActionBar() {
        when(joiningPlayer.hasPlayedBefore()).thenReturn(false);
        configManager.firstJoinTitleEnabled = false;
        configManager.firstJoinActionBarEnabled = true;

        PlayerConnectionListener listener = new PlayerConnectionListener(plugin);
        listener.onPlayerJoin(new PlayerJoinEvent(joiningPlayer, Component.text("join")));

        verify(joiningPlayer, never()).showTitle(any(Title.class));
        verify(joiningPlayer, times(1)).sendActionBar(any(Component.class));
    }

    @Test
    @DisplayName("first-join.actionbar.enabled = false strictly suppresses ActionBar while keeping Title")
    void testFirstJoinActionBarDisabledKeepsTitle() {
        when(joiningPlayer.hasPlayedBefore()).thenReturn(false);
        configManager.firstJoinTitleEnabled = true;
        configManager.firstJoinActionBarEnabled = false;

        PlayerConnectionListener listener = new PlayerConnectionListener(plugin);
        listener.onPlayerJoin(new PlayerJoinEvent(joiningPlayer, Component.text("join")));

        verify(joiningPlayer, times(1)).showTitle(any(Title.class));
        verify(joiningPlayer, never()).sendActionBar(any(Component.class));
    }

    @Test
    @DisplayName("join.title.enabled = false strictly suppresses Title delivery while keeping ActionBar")
    void testNormalJoinTitleDisabledKeepsActionBar() {
        when(joiningPlayer.hasPlayedBefore()).thenReturn(true);
        configManager.joinTitleEnabled = false;
        configManager.joinActionBarEnabled = true;

        PlayerConnectionListener listener = new PlayerConnectionListener(plugin);
        listener.onPlayerJoin(new PlayerJoinEvent(joiningPlayer, Component.text("join")));

        verify(joiningPlayer, never()).showTitle(any(Title.class));
        verify(joiningPlayer, times(1)).sendActionBar(any(Component.class));
    }

    @Test
    @DisplayName("join.actionbar.enabled = false strictly suppresses ActionBar while keeping Title")
    void testNormalJoinActionBarDisabledKeepsTitle() {
        when(joiningPlayer.hasPlayedBefore()).thenReturn(true);
        configManager.joinTitleEnabled = true;
        configManager.joinActionBarEnabled = false;

        PlayerConnectionListener listener = new PlayerConnectionListener(plugin);
        listener.onPlayerJoin(new PlayerJoinEvent(joiningPlayer, Component.text("join")));

        verify(joiningPlayer, times(1)).showTitle(any(Title.class));
        verify(joiningPlayer, never()).sendActionBar(any(Component.class));
    }

    @Test
    @DisplayName("Both Title and ActionBar disabled in config completely suppresses HUD packets but allows private chat")
    void testBothTitleAndActionBarDisabledAllowsPrivateChat() {
        when(joiningPlayer.hasPlayedBefore()).thenReturn(false);
        configManager.firstJoinTitleEnabled = false;
        configManager.firstJoinActionBarEnabled = false;

        PlayerConnectionListener listener = new PlayerConnectionListener(plugin);
        listener.onPlayerJoin(new PlayerJoinEvent(joiningPlayer, Component.text("join")));

        verify(joiningPlayer, never()).showTitle(any(Title.class));
        verify(joiningPlayer, never()).sendActionBar(any(Component.class));
        // Private chat must still be delivered
        verify(joiningPlayer, times(1)).sendMessage(any(Component.class));
    }

    // ─── 5. Fallback Routing & Event Isolation ─────────────────────────

    @Test
    @DisplayName("First-time player uses first-join templates and timings, NOT normal join")
    void testFirstTimePlayerUsesFirstJoinConfig() {
        when(joiningPlayer.hasPlayedBefore()).thenReturn(false);
        configManager.firstJoinTitleFadeIn = 7L;
        configManager.joinTitleFadeIn = 33L;

        PlayerConnectionListener listener = new PlayerConnectionListener(plugin);
        listener.onPlayerJoin(new PlayerJoinEvent(joiningPlayer, Component.text("join")));

        ArgumentCaptor<Title> captor = ArgumentCaptor.forClass(Title.class);
        verify(joiningPlayer, times(1)).showTitle(captor.capture());
        Title title = captor.getValue();

        assertEquals(Ticks.duration(7L), title.times().fadeIn(), "Must use firstJoinTitleFadeIn (7L), not normal join (33L)");
        String plainTitle = PlainTextComponentSerializer.plainText().serialize(title.title());
        assertTrue(plainTitle.contains("BIENVENIDO"), "Must use first-join title template: " + plainTitle);
    }

    @Test
    @DisplayName("Returning player uses normal join templates and timings, NOT first-join")
    void testReturningPlayerUsesNormalJoinConfig() {
        when(joiningPlayer.hasPlayedBefore()).thenReturn(true);
        configManager.firstJoinTitleFadeIn = 7L;
        configManager.joinTitleFadeIn = 33L;

        PlayerConnectionListener listener = new PlayerConnectionListener(plugin);
        listener.onPlayerJoin(new PlayerJoinEvent(joiningPlayer, Component.text("join")));

        ArgumentCaptor<Title> captor = ArgumentCaptor.forClass(Title.class);
        verify(joiningPlayer, times(1)).showTitle(captor.capture());
        Title title = captor.getValue();

        assertEquals(Ticks.duration(33L), title.times().fadeIn(), "Must use joinTitleFadeIn (33L)");
        String plainTitle = PlainTextComponentSerializer.plainText().serialize(title.title());
        assertTrue(plainTitle.contains("HOLA DE NUEVO"), "Must use normal join title template: " + plainTitle);
    }

    @Test
    @DisplayName("When firstJoinEnabled is false, new player falls back to normal join handler")
    void testFirstJoinDisabledFallsThroughToNormalJoin() {
        when(joiningPlayer.hasPlayedBefore()).thenReturn(false);
        configManager.firstJoinEnabled = false;
        configManager.joinEnabled = true;

        PlayerConnectionListener listener = new PlayerConnectionListener(plugin);
        listener.onPlayerJoin(new PlayerJoinEvent(joiningPlayer, Component.text("join")));

        // Should use normal join title
        ArgumentCaptor<Title> captor = ArgumentCaptor.forClass(Title.class);
        verify(joiningPlayer, times(1)).showTitle(captor.capture());
        Title title = captor.getValue();

        String plainTitle = PlainTextComponentSerializer.plainText().serialize(title.title());
        assertTrue(plainTitle.contains("HOLA DE NUEVO"), "Fallback must use normal join title");
    }

    @Test
    @DisplayName("When joinEnabled is false for returning player, zero titles or actionbars are dispatched")
    void testJoinDisabledSuppressesAllForReturningPlayer() {
        when(joiningPlayer.hasPlayedBefore()).thenReturn(true);
        configManager.joinEnabled = false;

        PlayerConnectionListener listener = new PlayerConnectionListener(plugin);
        listener.onPlayerJoin(new PlayerJoinEvent(joiningPlayer, Component.text("join")));

        verify(joiningPlayer, never()).showTitle(any(Title.class));
        verify(joiningPlayer, never()).sendActionBar(any(Component.class));
        verify(joiningPlayer, never()).sendMessage(any(Component.class));
    }

    // ─── 6. MiniMessage Tag Injection Immunity in Titles & Subtitles ───

    @Test
    @DisplayName("MiniMessage injection tags in player name are neutralized in Title and Subtitle")
    void testMiniMessageInjectionNeutralizedInTitleAndSubtitle() {
        String maliciousName = "<click:run_command:/op hacker><hover:show_text:'steal'>InjectedPlayer</click>";
        when(joiningPlayer.getName()).thenReturn(maliciousName);
        when(joiningPlayer.hasPlayedBefore()).thenReturn(true);

        // Subtitle uses {player}
        langManager.joinSubtitle = "Welcome <gold>{player}</gold> to our world!";

        PlayerConnectionListener listener = new PlayerConnectionListener(plugin);
        listener.onPlayerJoin(new PlayerJoinEvent(joiningPlayer, Component.text("join")));

        ArgumentCaptor<Title> captor = ArgumentCaptor.forClass(Title.class);
        verify(joiningPlayer, times(1)).showTitle(captor.capture());
        Title title = captor.getValue();

        assertFalse(hasClickEvent(title.title()), "Title MUST NOT have ClickEvent");
        assertFalse(hasHoverEvent(title.title()), "Title MUST NOT have HoverEvent");
        assertFalse(hasClickEvent(title.subtitle()), "Subtitle MUST NOT have ClickEvent from player name injection");
        assertFalse(hasHoverEvent(title.subtitle()), "Subtitle MUST NOT have HoverEvent from player name injection");

        String plainSub = PlainTextComponentSerializer.plainText().serialize(title.subtitle());
        assertTrue(plainSub.contains("InjectedPlayer") || plainSub.contains("<click:run_command"),
            "Payload must be preserved as literal plain text");
    }

    @Test
    @DisplayName("MiniMessage injection tags in player name are neutralized in ActionBar")
    void testMiniMessageInjectionNeutralizedInActionBar() {
        String maliciousName = "<click:run_command:/stop>Attacker";
        when(joiningPlayer.getName()).thenReturn(maliciousName);
        when(joiningPlayer.hasPlayedBefore()).thenReturn(true);

        langManager.joinActionBar = "Welcome {player}!";

        PlayerConnectionListener listener = new PlayerConnectionListener(plugin);
        listener.onPlayerJoin(new PlayerJoinEvent(joiningPlayer, Component.text("join")));

        ArgumentCaptor<Component> captor = ArgumentCaptor.forClass(Component.class);
        verify(joiningPlayer, times(1)).sendActionBar(captor.capture());
        Component ab = captor.getValue();

        assertFalse(hasClickEvent(ab), "ActionBar MUST NOT contain ClickEvent");
        assertFalse(hasHoverEvent(ab), "ActionBar MUST NOT contain HoverEvent");
    }

    // ─── 7. Offline Player Safety on Delayed Task Execution ────────────

    @Test
    @DisplayName("Player disconnecting prior to the 5-tick delayed task strictly suppresses title and actionbar")
    void testPlayerDisconnectSuppressesTitleAndActionBar() {
        when(joiningPlayer.hasPlayedBefore()).thenReturn(false);
        when(joiningPlayer.isOnline()).thenReturn(false); // Disconnected!

        PlayerConnectionListener listener = new PlayerConnectionListener(plugin);
        listener.onPlayerJoin(new PlayerJoinEvent(joiningPlayer, Component.text("join")));

        verify(joiningPlayer, never()).showTitle(any(Title.class));
        verify(joiningPlayer, never()).sendActionBar(any(Component.class));
        verify(joiningPlayer, never()).sendMessage(any(Component.class));
    }

    // ─── Helper Methods ────────────────────────────────────────────────

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
