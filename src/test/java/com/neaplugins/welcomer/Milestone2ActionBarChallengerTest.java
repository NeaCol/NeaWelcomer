package com.neaplugins.welcomer;

import com.neaplugins.welcomer.constants.Permissions;
import com.neaplugins.welcomer.listeners.PlayerConnectionListener;
import com.neaplugins.welcomer.managers.ConfigManager;
import com.neaplugins.welcomer.managers.LangManager;
import com.neaplugins.welcomer.managers.UniquePlayerTracker;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.title.Title;
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
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Empirical Adversarial Challenger Test Suite: Milestone 2 ActionBar & Title Verification.
 * Focuses on:
 * 1. Empirical verification of ActionBar delivery on first-join and normal-join.
 * 2. Toggle suppression when actionbar.enabled = false.
 * 3. Suppression when actionbar template string is empty or null.
 * 4. Toggle independence between Titles and ActionBars.
 * 5. MiniMessage tag injection protection inside ActionBar components.
 */
class Milestone2ActionBarChallengerTest {

    private NeaWelcomer plugin;
    private TestChallengerConfigManager configManager;
    private TestChallengerLangManager langManager;
    private UniquePlayerTracker playerTracker;

    private Server mockServer;
    private ConsoleCommandSender mockConsole;
    private BukkitScheduler mockScheduler;
    private PluginManager mockPluginManager;

    private Player mockPlayer;

    static class TestChallengerConfigManager extends ConfigManager {
        boolean firstJoinEnabled = true;
        boolean firstJoinTitleEnabled = true;
        long firstJoinTitleFadeIn = 10L;
        long firstJoinTitleStay = 70L;
        long firstJoinTitleFadeOut = 20L;
        boolean firstJoinActionBarEnabled = true;

        boolean joinEnabled = true;
        boolean joinTitleEnabled = true;
        long joinTitleFadeIn = 10L;
        long joinTitleStay = 70L;
        long joinTitleFadeOut = 20L;
        boolean joinActionBarEnabled = true;

        TestChallengerConfigManager(NeaWelcomer plugin) { super(plugin); }

        @Override public boolean isFirstJoinEnabled() { return firstJoinEnabled; }
        @Override public boolean isFirstJoinTitleEnabled() { return firstJoinTitleEnabled; }
        @Override public long getFirstJoinTitleFadeIn() { return firstJoinTitleFadeIn; }
        @Override public long getFirstJoinTitleStay() { return firstJoinTitleStay; }
        @Override public long getFirstJoinTitleFadeOut() { return firstJoinTitleFadeOut; }
        @Override public boolean isFirstJoinActionBarEnabled() { return firstJoinActionBarEnabled; }

        @Override public boolean isJoinEnabled() { return joinEnabled; }
        @Override public boolean isJoinTitleEnabled() { return joinTitleEnabled; }
        @Override public long getJoinTitleFadeIn() { return joinTitleFadeIn; }
        @Override public long getJoinTitleStay() { return joinTitleStay; }
        @Override public long getJoinTitleFadeOut() { return joinTitleFadeOut; }
        @Override public boolean isJoinActionBarEnabled() { return joinActionBarEnabled; }

        @Override public boolean isDisableDefaultMessages() { return true; }
        @Override public boolean isFirstJoinBroadcast() { return false; }
        @Override public boolean isJoinBroadcast() { return false; }
        @Override public boolean isFirstJoinSoundEnabled() { return false; }
        @Override public boolean isJoinSoundEnabled() { return false; }
    }

    static class TestChallengerLangManager extends LangManager {
        String firstJoinTitle = "<gold>Welcome First Time!</gold>";
        String firstJoinSubtitle = "<yellow>Enjoy your stay</yellow>";
        String firstJoinActionBar = "<green>You are player #{player_number} - welcome {player}!</green>";

        String joinTitle = "<aqua>Welcome Back!</aqua>";
        String joinSubtitle = "<blue>Good to see you</blue>";
        String joinActionBar = "<light_purple>Welcome back {player}!</light_purple>";

        TestChallengerLangManager(NeaWelcomer plugin) { super(plugin); }

        @Override
        public String getMessage(String path) {
            switch (path) {
                case "first-join.title": return firstJoinTitle;
                case "first-join.subtitle": return firstJoinSubtitle;
                case "first-join.actionbar": return firstJoinActionBar;
                case "join.title": return joinTitle;
                case "join.subtitle": return joinSubtitle;
                case "join.actionbar": return joinActionBar;
                default: return "";
            }
        }

        @Override public List<String> getMessageList(String path) { return Collections.emptyList(); }
    }

    @BeforeEach
    void setUp() throws Exception {
        Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);
        plugin = (NeaWelcomer) unsafe.allocateInstance(NeaWelcomer.class);

        configManager = new TestChallengerConfigManager(plugin);
        langManager = new TestChallengerLangManager(plugin);
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

        // Run delayed tasks immediately on the calling thread for synchronous evaluation
        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(1);
            if (task != null) {
                task.run();
            }
            return null;
        }).when(mockScheduler).runTaskLater(any(), any(Runnable.class), anyLong());

        Field serverField = Bukkit.class.getDeclaredField("server");
        serverField.setAccessible(true);
        serverField.set(null, mockServer);

        mockPlayer = Mockito.mock(Player.class);
        when(mockPlayer.getName()).thenReturn("TestHero");
        when(mockPlayer.displayName()).thenReturn(Component.text("TestHero"));
        when(mockPlayer.isOnline()).thenReturn(true);
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

    private static boolean hasClickOrHover(Component component) {
        if (component == null) return false;
        if (component.clickEvent() != null || component.hoverEvent() != null) return true;
        for (Component child : component.children()) {
            if (hasClickOrHover(child)) return true;
        }
        return false;
    }

    // ─── 1. Empirical ActionBar Delivery Verification ───────────

    @Test
    @DisplayName("First-join delivers ActionBar with resolved {player} and {player_number}")
    void testFirstJoinActionBarDeliveredWithExactContent() {
        when(mockPlayer.hasPlayedBefore()).thenReturn(false);

        PlayerConnectionListener listener = new PlayerConnectionListener(plugin);
        PlayerJoinEvent event = new PlayerJoinEvent(mockPlayer, Component.text("join"));
        listener.onPlayerJoin(event);

        // Verify scheduler delay 5L
        verify(mockScheduler, atLeastOnce()).runTaskLater(eq(plugin), any(Runnable.class), eq(5L));

        // Capture ActionBar component
        ArgumentCaptor<Component> abCaptor = ArgumentCaptor.forClass(Component.class);
        verify(mockPlayer, times(1)).sendActionBar(abCaptor.capture());
        Component ab = abCaptor.getValue();
        assertNotNull(ab, "Action bar component must not be null");

        String plainText = PlainTextComponentSerializer.plainText().serialize(ab);
        assertTrue(plainText.contains("TestHero"), "Action bar must contain player name 'TestHero'");
        assertTrue(plainText.contains("#1"), "Action bar must contain player number '#1' on first join");
    }

    @Test
    @DisplayName("Normal join delivers ActionBar with resolved {player}")
    void testNormalJoinActionBarDeliveredWithExactContent() {
        when(mockPlayer.hasPlayedBefore()).thenReturn(true);

        PlayerConnectionListener listener = new PlayerConnectionListener(plugin);
        PlayerJoinEvent event = new PlayerJoinEvent(mockPlayer, Component.text("join"));
        listener.onPlayerJoin(event);

        // Verify scheduler delay 5L
        verify(mockScheduler, atLeastOnce()).runTaskLater(eq(plugin), any(Runnable.class), eq(5L));

        // Capture ActionBar component
        ArgumentCaptor<Component> abCaptor = ArgumentCaptor.forClass(Component.class);
        verify(mockPlayer, times(1)).sendActionBar(abCaptor.capture());
        Component ab = abCaptor.getValue();
        assertNotNull(ab, "Action bar component must not be null");

        String plainText = PlainTextComponentSerializer.plainText().serialize(ab);
        assertTrue(plainText.contains("Welcome back TestHero!"), "Action bar must contain 'Welcome back TestHero!'");
    }

    // ─── 2. Toggle Suppression Verification ─────────────────────

    @Test
    @DisplayName("First-join ActionBar suppressed when config firstJoinActionBarEnabled = false")
    void testFirstJoinActionBarSuppressedWhenConfigDisabled() {
        when(mockPlayer.hasPlayedBefore()).thenReturn(false);
        configManager.firstJoinActionBarEnabled = false;

        PlayerConnectionListener listener = new PlayerConnectionListener(plugin);
        PlayerJoinEvent event = new PlayerJoinEvent(mockPlayer, Component.text("join"));
        listener.onPlayerJoin(event);

        verify(mockPlayer, never()).sendActionBar(any(Component.class));
    }

    @Test
    @DisplayName("Normal join ActionBar suppressed when config joinActionBarEnabled = false")
    void testNormalJoinActionBarSuppressedWhenConfigDisabled() {
        when(mockPlayer.hasPlayedBefore()).thenReturn(true);
        configManager.joinActionBarEnabled = false;

        PlayerConnectionListener listener = new PlayerConnectionListener(plugin);
        PlayerJoinEvent event = new PlayerJoinEvent(mockPlayer, Component.text("join"));
        listener.onPlayerJoin(event);

        verify(mockPlayer, never()).sendActionBar(any(Component.class));
    }

    @Test
    @DisplayName("First-join ActionBar suppressed when template string is empty")
    void testFirstJoinActionBarSuppressedWhenTemplateEmpty() {
        when(mockPlayer.hasPlayedBefore()).thenReturn(false);
        langManager.firstJoinActionBar = "";

        PlayerConnectionListener listener = new PlayerConnectionListener(plugin);
        PlayerJoinEvent event = new PlayerJoinEvent(mockPlayer, Component.text("join"));
        listener.onPlayerJoin(event);

        verify(mockPlayer, never()).sendActionBar(any(Component.class));
    }

    @Test
    @DisplayName("Normal join ActionBar suppressed when template string is empty")
    void testNormalJoinActionBarSuppressedWhenTemplateEmpty() {
        when(mockPlayer.hasPlayedBefore()).thenReturn(true);
        langManager.joinActionBar = "";

        PlayerConnectionListener listener = new PlayerConnectionListener(plugin);
        PlayerJoinEvent event = new PlayerJoinEvent(mockPlayer, Component.text("join"));
        listener.onPlayerJoin(event);

        verify(mockPlayer, never()).sendActionBar(any(Component.class));
    }

    @Test
    @DisplayName("First-join ActionBar safely suppressed when template string is null (no NPE)")
    void testFirstJoinActionBarSuppressedWhenTemplateNull() {
        when(mockPlayer.hasPlayedBefore()).thenReturn(false);
        langManager.firstJoinActionBar = null;

        PlayerConnectionListener listener = new PlayerConnectionListener(plugin);
        PlayerJoinEvent event = new PlayerJoinEvent(mockPlayer, Component.text("join"));

        assertDoesNotThrow(() -> listener.onPlayerJoin(event));
        verify(mockPlayer, never()).sendActionBar(any(Component.class));
    }

    @Test
    @DisplayName("Normal join ActionBar safely suppressed when template string is null (no NPE)")
    void testNormalJoinActionBarSuppressedWhenTemplateNull() {
        when(mockPlayer.hasPlayedBefore()).thenReturn(true);
        langManager.joinActionBar = null;

        PlayerConnectionListener listener = new PlayerConnectionListener(plugin);
        PlayerJoinEvent event = new PlayerJoinEvent(mockPlayer, Component.text("join"));

        assertDoesNotThrow(() -> listener.onPlayerJoin(event));
        verify(mockPlayer, never()).sendActionBar(any(Component.class));
    }

    // ─── 3. Toggle Independence ─────────────────────────────────

    @Test
    @DisplayName("First-join: Title disabled does NOT suppress ActionBar")
    void testFirstJoinTitleDisabledActionBarStillDelivered() {
        when(mockPlayer.hasPlayedBefore()).thenReturn(false);
        configManager.firstJoinTitleEnabled = false;
        configManager.firstJoinActionBarEnabled = true;

        PlayerConnectionListener listener = new PlayerConnectionListener(plugin);
        PlayerJoinEvent event = new PlayerJoinEvent(mockPlayer, Component.text("join"));
        listener.onPlayerJoin(event);

        verify(mockPlayer, never()).showTitle(any(Title.class));
        verify(mockPlayer, times(1)).sendActionBar(any(Component.class));
    }

    @Test
    @DisplayName("First-join: ActionBar disabled does NOT suppress Title")
    void testFirstJoinActionBarDisabledTitleStillDelivered() {
        when(mockPlayer.hasPlayedBefore()).thenReturn(false);
        configManager.firstJoinTitleEnabled = true;
        configManager.firstJoinActionBarEnabled = false;

        PlayerConnectionListener listener = new PlayerConnectionListener(plugin);
        PlayerJoinEvent event = new PlayerJoinEvent(mockPlayer, Component.text("join"));
        listener.onPlayerJoin(event);

        verify(mockPlayer, times(1)).showTitle(any(Title.class));
        verify(mockPlayer, never()).sendActionBar(any(Component.class));
    }

    @Test
    @DisplayName("Normal join: Title disabled does NOT suppress ActionBar")
    void testNormalJoinTitleDisabledActionBarStillDelivered() {
        when(mockPlayer.hasPlayedBefore()).thenReturn(true);
        configManager.joinTitleEnabled = false;
        configManager.joinActionBarEnabled = true;

        PlayerConnectionListener listener = new PlayerConnectionListener(plugin);
        PlayerJoinEvent event = new PlayerJoinEvent(mockPlayer, Component.text("join"));
        listener.onPlayerJoin(event);

        verify(mockPlayer, never()).showTitle(any(Title.class));
        verify(mockPlayer, times(1)).sendActionBar(any(Component.class));
    }

    @Test
    @DisplayName("Normal join: ActionBar disabled does NOT suppress Title")
    void testNormalJoinActionBarDisabledTitleStillDelivered() {
        when(mockPlayer.hasPlayedBefore()).thenReturn(true);
        configManager.joinTitleEnabled = true;
        configManager.joinActionBarEnabled = false;

        PlayerConnectionListener listener = new PlayerConnectionListener(plugin);
        PlayerJoinEvent event = new PlayerJoinEvent(mockPlayer, Component.text("join"));
        listener.onPlayerJoin(event);

        verify(mockPlayer, times(1)).showTitle(any(Title.class));
        verify(mockPlayer, never()).sendActionBar(any(Component.class));
    }

    // ─── 4. Injection Protection inside ActionBar ───────────────

    @ParameterizedTest(name = "Malicious name in ActionBar: {0}")
    @ValueSource(strings = {
        "<click:run_command:/op hacker>EvilPlayer",
        "<hover:show_text:'malicious tooltip'>TooltipPlayer",
        "<gradient:#FF0000:#00FF00>InjectedColors</gradient>",
        "<reset><bold>UnescapedFormatting",
        "<insert:attack>InsertedText"
    })
    @DisplayName("ActionBar strips/escapes MiniMessage injection payloads in player names")
    void testActionBarSanitizesInjectionInPlayerName(String maliciousName) {
        when(mockPlayer.hasPlayedBefore()).thenReturn(true);
        when(mockPlayer.getName()).thenReturn(maliciousName);

        PlayerConnectionListener listener = new PlayerConnectionListener(plugin);
        PlayerJoinEvent event = new PlayerJoinEvent(mockPlayer, Component.text("join"));
        listener.onPlayerJoin(event);

        ArgumentCaptor<Component> abCaptor = ArgumentCaptor.forClass(Component.class);
        verify(mockPlayer, times(1)).sendActionBar(abCaptor.capture());
        Component ab = abCaptor.getValue();

        assertFalse(hasClickOrHover(ab), "ActionBar component MUST NOT execute injected click or hover events!");
    }

    // ─── 5. Offline Player Suppression ──────────────────────────

    @Test
    @DisplayName("Offline player on delayed execution receives neither Title nor ActionBar")
    void testOfflinePlayerSuppressesBoth() {
        when(mockPlayer.hasPlayedBefore()).thenReturn(false);
        when(mockPlayer.isOnline()).thenReturn(false);

        PlayerConnectionListener listener = new PlayerConnectionListener(plugin);
        PlayerJoinEvent event = new PlayerJoinEvent(mockPlayer, Component.text("join"));
        listener.onPlayerJoin(event);

        verify(mockPlayer, never()).showTitle(any(Title.class));
        verify(mockPlayer, never()).sendActionBar(any(Component.class));
    }
}
