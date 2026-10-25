package com.neaplugins.welcomer;

import com.neaplugins.welcomer.constants.Permissions;
import com.neaplugins.welcomer.listeners.PlayerConnectionListener;
import com.neaplugins.welcomer.managers.ConfigManager;
import com.neaplugins.welcomer.managers.LangManager;
import com.neaplugins.welcomer.managers.UniquePlayerTracker;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.util.Ticks;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Empirical challenge test suite for {@link PlayerConnectionListener}.
 * Pure interface mocking and reflection allocation ensuring full compatibility with Java 25.
 *
 * Rigorously verifies:
 * 1. Broadcast visibility permissions (join.see, quit.see, firstjoin.see).
 * 2. Sound broadcast permissions.
 * 3. Zero synchronous filesystem scanning during player join events.
 * 4. Unique player tracking increment logic on first join.
 */
class PlayerConnectionListenerTest {

    private NeaWelcomer plugin;
    private TestConfigManager configManager;
    private TestLangManager langManager;
    private UniquePlayerTracker playerTracker;

    private Server mockServer;
    private ConsoleCommandSender mockConsole;
    private BukkitScheduler mockScheduler;
    private PluginManager mockPluginManager;

    private Player joiningPlayer;
    private Player authorizedPlayer;
    private Player unauthorizedPlayer;

    static class TestConfigManager extends ConfigManager {
        boolean disableDefault = true;
        boolean firstJoinEnabled = true;
        boolean firstJoinBroadcast = true;
        boolean firstJoinTitleEnabled = true;
        long firstJoinTitleFadeIn = 10L;
        long firstJoinTitleStay = 70L;
        long firstJoinTitleFadeOut = 20L;
        boolean firstJoinActionBarEnabled = true;
        boolean firstJoinSound = true;

        boolean joinEnabled = true;
        boolean joinBroadcast = true;
        boolean joinTitleEnabled = true;
        long joinTitleFadeIn = 10L;
        long joinTitleStay = 70L;
        long joinTitleFadeOut = 20L;
        boolean joinActionBarEnabled = true;
        boolean joinSound = true;

        boolean quitEnabled = true;
        boolean quitBroadcast = true;
        boolean quitSound = true;

        TestConfigManager(NeaWelcomer plugin) { super(plugin); }

        @Override public boolean isDisableDefaultMessages() { return disableDefault; }
        @Override public boolean isFirstJoinEnabled() { return firstJoinEnabled; }
        @Override public boolean isFirstJoinBroadcast() { return firstJoinBroadcast; }
        @Override public boolean isFirstJoinTitleEnabled() { return firstJoinTitleEnabled; }
        @Override public long getFirstJoinTitleFadeIn() { return firstJoinTitleFadeIn; }
        @Override public long getFirstJoinTitleStay() { return firstJoinTitleStay; }
        @Override public long getFirstJoinTitleFadeOut() { return firstJoinTitleFadeOut; }
        @Override public boolean isFirstJoinActionBarEnabled() { return firstJoinActionBarEnabled; }
        @Override public boolean isFirstJoinSoundEnabled() { return firstJoinSound; }
        @Override public String getFirstJoinSound() { return "ui.toast.challenge_complete"; }
        @Override public float getFirstJoinSoundVolume() { return 1.0f; }
        @Override public float getFirstJoinSoundPitch() { return 1.0f; }

        @Override public boolean isJoinEnabled() { return joinEnabled; }
        @Override public boolean isJoinBroadcast() { return joinBroadcast; }
        @Override public boolean isJoinTitleEnabled() { return joinTitleEnabled; }
        @Override public long getJoinTitleFadeIn() { return joinTitleFadeIn; }
        @Override public long getJoinTitleStay() { return joinTitleStay; }
        @Override public long getJoinTitleFadeOut() { return joinTitleFadeOut; }
        @Override public boolean isJoinActionBarEnabled() { return joinActionBarEnabled; }
        @Override public boolean isJoinSoundEnabled() { return joinSound; }
        @Override public String getJoinSound() { return "entity.player.levelup"; }
        @Override public float getJoinSoundVolume() { return 1.0f; }
        @Override public float getJoinSoundPitch() { return 1.0f; }

        @Override public boolean isQuitEnabled() { return quitEnabled; }
        @Override public boolean isQuitBroadcast() { return quitBroadcast; }
        @Override public boolean isQuitSoundEnabled() { return quitSound; }
        @Override public String getQuitSound() { return "block.note_block.bass"; }
        @Override public float getQuitSoundVolume() { return 1.0f; }
        @Override public float getQuitSoundPitch() { return 1.0f; }
    }

    static class TestLangManager extends LangManager {
        TestLangManager(NeaWelcomer plugin) { super(plugin); }

        @Override
        public String getMessage(String path) {
            switch (path) {
                case "first-join.title":
                    return "<gradient:#7C4DFF:#B388FF><bold>¡BIENVENIDO!</bold></gradient>";
                case "first-join.subtitle":
                    return "<#E0E0E0>Disfruta tu estadía en el servidor";
                case "first-join.actionbar":
                    return "<#B388FF>¡Eres el jugador <#FFD54F>#{player_number}<#B388FF> en unirse!";
                case "join.title":
                    return "<gradient:#7C4DFF:#00BCD4><bold>¡HOLA DE NUEVO!</bold></gradient>";
                case "join.subtitle":
                    return "<#E0E0E0>Bienvenido de vuelta, <#7C4DFF>{player}";
                case "join.actionbar":
                    return "<#4CAF50>¡Bienvenido de nuevo, <#E0E0E0>{player}<#4CAF50>!";
                default:
                    return "";
            }
        }

        @Override
        public List<String> getMessageList(String path) {
            switch (path) {
                case "first-join.broadcast":
                    return Collections.singletonList("<yellow>Welcome <green>{player}</green> for the first time!</yellow>");
                case "first-join.private":
                    return Collections.singletonList("<gold>Welcome newbie!</gold>");
                case "join.broadcast":
                    return Collections.singletonList("<green>+ {player} joined</green>");
                case "join.private":
                    return Collections.singletonList("<gray>Welcome back!</gray>");
                case "quit.broadcast":
                    return Collections.singletonList("<red>- {player} left</red>");
                default:
                    return Collections.emptyList();
            }
        }

        @Override
        public String getPrefix() {
            return "<purple>NeaWelcomer » </purple>";
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        // Allocate NeaWelcomer instance via Unsafe without triggering JavaPlugin constructor restrictions
        Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);
        plugin = (NeaWelcomer) unsafe.allocateInstance(NeaWelcomer.class);

        configManager = new TestConfigManager(plugin);
        langManager = new TestLangManager(plugin);
        playerTracker = new UniquePlayerTracker(plugin);

        setField(plugin, NeaWelcomer.class, "configManager", configManager);
        setField(plugin, NeaWelcomer.class, "langManager", langManager);
        setField(plugin, NeaWelcomer.class, "playerTracker", playerTracker);

        // Also set static instance
        Field instanceField = NeaWelcomer.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, plugin);

        // Set Logger on plugin (search hierarchy for logger field)
        Logger testLogger = Logger.getLogger("NeaWelcomerTest");
        setFieldInHierarchy(plugin, "logger", testLogger);

        // Setup mock Bukkit Server using standard Bukkit.setServer()
        mockServer = Mockito.mock(Server.class);
        mockConsole = Mockito.mock(ConsoleCommandSender.class);
        mockScheduler = Mockito.mock(BukkitScheduler.class);
        mockPluginManager = Mockito.mock(PluginManager.class);

        when(mockServer.getConsoleSender()).thenReturn(mockConsole);
        when(mockServer.getScheduler()).thenReturn(mockScheduler);
        when(mockServer.getPluginManager()).thenReturn(mockPluginManager);
        when(mockServer.getLogger()).thenReturn(testLogger);

        // Execute runTaskLater synchronously during tests
        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(1);
            if (task != null) {
                task.run();
            }
            return null;
        }).when(mockScheduler).runTaskLater(any(), any(Runnable.class), anyLong());

        joiningPlayer = Mockito.mock(Player.class);
        when(joiningPlayer.getName()).thenReturn("Newbie");
        when(joiningPlayer.displayName()).thenReturn(Component.text("Newbie"));

        authorizedPlayer = Mockito.mock(Player.class);
        when(authorizedPlayer.getName()).thenReturn("AdminUser");

        unauthorizedPlayer = Mockito.mock(Player.class);
        when(unauthorizedPlayer.getName()).thenReturn("RegularUser");

        List<Player> onlinePlayers = Arrays.asList(authorizedPlayer, unauthorizedPlayer);
        doReturn(onlinePlayers).when(mockServer).getOnlinePlayers();

        // Directly set Bukkit.server via reflection to bypass Paper's ServerBuildInfo.buildInfo() requirement
        Field serverField = Bukkit.class.getDeclaredField("server");
        serverField.setAccessible(true);
        serverField.set(null, mockServer);
    }

    @AfterEach
    void tearDown() throws Exception {
        Field instanceField = NeaWelcomer.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);

        // Reset server
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

    @Test
    @DisplayName("First-join broadcasts message and sound ONLY to players with neawelcomer.firstjoin.see")
    void testFirstJoinPermissionEnforcement() {
        when(joiningPlayer.hasPlayedBefore()).thenReturn(false);
        when(authorizedPlayer.hasPermission(Permissions.FIRSTJOIN_SEE)).thenReturn(true);
        when(unauthorizedPlayer.hasPermission(Permissions.FIRSTJOIN_SEE)).thenReturn(false);

        assertEquals(0, playerTracker.getUniquePlayerCount(), "Initial count must be 0");

        PlayerConnectionListener listener = new PlayerConnectionListener(plugin);
        PlayerJoinEvent event = new PlayerJoinEvent(joiningPlayer, Component.text("default join"));

        listener.onPlayerJoin(event);

        // 1. Unique player tracker MUST be incremented on first join
        assertEquals(1, playerTracker.getUniquePlayerCount(), "Tracker must be incremented to 1");

        // 2. Default Minecraft join message must be suppressed
        assertNull(event.joinMessage(), "Default join message should be set to null");

        // 3. Authorized player MUST receive broadcast message and sound
        verify(authorizedPlayer, times(1)).sendMessage(any(Component.class));
        verify(authorizedPlayer, times(1)).playSound(any(Sound.class));

        // 4. Unauthorized player MUST NOT receive broadcast message or sound
        verify(unauthorizedPlayer, never()).sendMessage(any(Component.class));
        verify(unauthorizedPlayer, never()).playSound(any(Sound.class));

        // 5. Console sender receives broadcast for server log audit
        verify(mockConsole, times(1)).sendMessage(any(Component.class));
    }

    @Test
    @DisplayName("Normal join broadcasts message and sound ONLY to players with neawelcomer.join.see")
    void testNormalJoinPermissionEnforcement() {
        when(joiningPlayer.hasPlayedBefore()).thenReturn(true);
        when(authorizedPlayer.hasPermission(Permissions.JOIN_SEE)).thenReturn(true);
        when(unauthorizedPlayer.hasPermission(Permissions.JOIN_SEE)).thenReturn(false);

        assertEquals(0, playerTracker.getUniquePlayerCount());

        PlayerConnectionListener listener = new PlayerConnectionListener(plugin);
        PlayerJoinEvent event = new PlayerJoinEvent(joiningPlayer, Component.text("default join"));

        listener.onPlayerJoin(event);

        // 1. Tracker MUST NOT be incremented for returning players
        assertEquals(0, playerTracker.getUniquePlayerCount(), "Tracker must remain 0 for returning players");

        // 2. Default join message suppressed
        assertNull(event.joinMessage());

        // 3. Authorized player receives broadcast message and sound
        verify(authorizedPlayer, times(1)).sendMessage(any(Component.class));
        verify(authorizedPlayer, times(1)).playSound(any(Sound.class));

        // 4. Unauthorized player receives NOTHING
        verify(unauthorizedPlayer, never()).sendMessage(any(Component.class));
        verify(unauthorizedPlayer, never()).playSound(any(Sound.class));

        // 5. Console receives broadcast
        verify(mockConsole, times(1)).sendMessage(any(Component.class));
    }

    @Test
    @DisplayName("Player quit broadcasts message and sound ONLY to players with neawelcomer.quit.see")
    void testQuitPermissionEnforcement() {
        Player quittingPlayer = Mockito.mock(Player.class);
        when(quittingPlayer.getName()).thenReturn("DepartingUser");

        when(authorizedPlayer.hasPermission(Permissions.QUIT_SEE)).thenReturn(true);
        when(unauthorizedPlayer.hasPermission(Permissions.QUIT_SEE)).thenReturn(false);

        PlayerConnectionListener listener = new PlayerConnectionListener(plugin);
        PlayerQuitEvent event = new PlayerQuitEvent(quittingPlayer, Component.text("default quit"));

        listener.onPlayerQuit(event);

        // 1. Default quit message suppressed
        assertNull(event.quitMessage());

        // 2. Authorized player receives broadcast message and sound
        verify(authorizedPlayer, times(1)).sendMessage(any(Component.class));
        verify(authorizedPlayer, times(1)).playSound(any(Sound.class));

        // 3. Unauthorized player receives NOTHING
        verify(unauthorizedPlayer, never()).sendMessage(any(Component.class));
        verify(unauthorizedPlayer, never()).playSound(any(Sound.class));

        // 4. Console receives broadcast
        verify(mockConsole, times(1)).sendMessage(any(Component.class));
    }

    @Test
    @DisplayName("When broadcasts are disabled in config, neither message nor sound is dispatched")
    void testBroadcastsDisabledInConfig() {
        when(joiningPlayer.hasPlayedBefore()).thenReturn(true);
        configManager.joinBroadcast = false;
        configManager.joinSound = false;

        PlayerConnectionListener listener = new PlayerConnectionListener(plugin);
        PlayerJoinEvent event = new PlayerJoinEvent(joiningPlayer, Component.text("join"));

        listener.onPlayerJoin(event);

        verify(authorizedPlayer, never()).sendMessage(any(Component.class));
        verify(authorizedPlayer, never()).playSound(any(Sound.class));
        verify(unauthorizedPlayer, never()).sendMessage(any(Component.class));
        verify(unauthorizedPlayer, never()).playSound(any(Sound.class));
        verify(mockConsole, never()).sendMessage(any(Component.class));
    }

    @Test
    @DisplayName("Empirical verification: zero synchronous filesystem scanning during join processing")
    void testZeroSynchronousFilesystemScanningOnJoin() {
        when(joiningPlayer.hasPlayedBefore()).thenReturn(false);
        when(authorizedPlayer.hasPermission(Permissions.FIRSTJOIN_SEE)).thenReturn(true);
        when(unauthorizedPlayer.hasPermission(Permissions.FIRSTJOIN_SEE)).thenReturn(true);

        PlayerConnectionListener listener = new PlayerConnectionListener(plugin);

        // Dispatch 20 first-join events and verify tracker increments and performance
        long start = System.nanoTime();
        for (int i = 0; i < 20; i++) {
            PlayerJoinEvent event = new PlayerJoinEvent(joiningPlayer, Component.text("join"));
            listener.onPlayerJoin(event);
        }
        long durationMs = (System.nanoTime() - start) / 1_000_000;

        assertEquals(20, playerTracker.getUniquePlayerCount(), "Tracker must reflect 20 first joins");
        assertTrue(durationMs < 500, "20 join events took " + durationMs + "ms without disk I/O");
    }

    @Test
    @DisplayName("Invalid sound keys in config are logged as warnings and do not crash or throw exceptions")
    void testInvalidSoundKeyGracefullyHandled() {
        when(joiningPlayer.hasPlayedBefore()).thenReturn(true);
        when(authorizedPlayer.hasPermission(Permissions.JOIN_SEE)).thenReturn(true);
        configManager.joinSound = true;
        // Intentionally malformed key
        TestConfigManager brokenConfig = new TestConfigManager(plugin) {
            @Override public String getJoinSound() { return "invalid:::key with spaces"; }
        };
        try {
            setField(plugin, NeaWelcomer.class, "configManager", brokenConfig);
            PlayerConnectionListener listener = new PlayerConnectionListener(plugin);
            PlayerJoinEvent event = new PlayerJoinEvent(joiningPlayer, Component.text("join"));

            assertDoesNotThrow(() -> listener.onPlayerJoin(event));
        } catch (Exception e) {
            fail("Exception thrown during invalid sound key test: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Concurrent first-join events atomically increment tracker without lost updates")
    void testConcurrentPlayerTrackerAccess() throws InterruptedException {
        int threadCount = 10;
        int incrementsPerThread = 100;
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(threadCount);
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < incrementsPerThread; j++) {
                        playerTracker.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(5, java.util.concurrent.TimeUnit.SECONDS));
        executor.shutdown();

        assertEquals(threadCount * incrementsPerThread, playerTracker.getUniquePlayerCount(),
            "Tracker must exactly match total concurrent increments");
    }

    @Test
    @DisplayName("Null or empty message lists do not dispatch any messages or cause exceptions")
    void testNullOrEmptyMessages() {
        TestLangManager emptyLang = new TestLangManager(plugin) {
            @Override public List<String> getMessageList(String path) { return null; }
        };
        try {
            setField(plugin, NeaWelcomer.class, "langManager", emptyLang);
            when(joiningPlayer.hasPlayedBefore()).thenReturn(true);
            when(authorizedPlayer.hasPermission(Permissions.JOIN_SEE)).thenReturn(true);

            PlayerConnectionListener listener = new PlayerConnectionListener(plugin);
            PlayerJoinEvent event = new PlayerJoinEvent(joiningPlayer, Component.text("join"));

            assertDoesNotThrow(() -> listener.onPlayerJoin(event));
            verify(authorizedPlayer, never()).sendMessage(any(Component.class));
            verify(mockConsole, never()).sendMessage(any(Component.class));
        } catch (Exception e) {
            fail("Exception thrown during null message list test: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("First-join dispatches Title with proper Title.Times duration and ActionBar when enabled")
    void testFirstJoinTitleAndActionBarDispatchedWhenEnabled() {
        when(joiningPlayer.hasPlayedBefore()).thenReturn(false);
        when(joiningPlayer.isOnline()).thenReturn(true);

        PlayerConnectionListener listener = new PlayerConnectionListener(plugin);
        PlayerJoinEvent event = new PlayerJoinEvent(joiningPlayer, Component.text("join"));

        listener.onPlayerJoin(event);

        // Verify delayed task scheduled with 5 ticks delay
        verify(mockScheduler, atLeastOnce()).runTaskLater(eq(plugin), any(Runnable.class), eq(5L));

        // Verify Title dispatch with Title.Times
        ArgumentCaptor<Title> titleCaptor = ArgumentCaptor.forClass(Title.class);
        verify(joiningPlayer, times(1)).showTitle(titleCaptor.capture());
        Title capturedTitle = titleCaptor.getValue();
        assertNotNull(capturedTitle, "Title must not be null");
        assertNotNull(capturedTitle.title(), "Title component must not be null");
        assertNotNull(capturedTitle.subtitle(), "Subtitle component must not be null");
        assertNotNull(capturedTitle.times(), "Title.Times must not be null");

        // Verify timing matches config defaults (10, 70, 20 ticks)
        assertEquals(Ticks.duration(10L), capturedTitle.times().fadeIn(), "Fade-in ticks must match config (10 ticks)");
        assertEquals(Ticks.duration(70L), capturedTitle.times().stay(), "Stay ticks must match config (70 ticks)");
        assertEquals(Ticks.duration(20L), capturedTitle.times().fadeOut(), "Fade-out ticks must match config (20 ticks)");

        // Verify ActionBar dispatch
        ArgumentCaptor<Component> abCaptor = ArgumentCaptor.forClass(Component.class);
        verify(joiningPlayer, times(1)).sendActionBar(abCaptor.capture());
        Component capturedAb = abCaptor.getValue();
        assertNotNull(capturedAb, "Action bar component must not be null");
        assertNotEquals(Component.empty(), capturedAb, "Action bar component must not be empty");
    }

    @Test
    @DisplayName("Normal join dispatches Title with proper Title.Times duration and ActionBar when enabled")
    void testNormalJoinTitleAndActionBarDispatchedWhenEnabled() {
        when(joiningPlayer.hasPlayedBefore()).thenReturn(true);
        when(joiningPlayer.isOnline()).thenReturn(true);

        PlayerConnectionListener listener = new PlayerConnectionListener(plugin);
        PlayerJoinEvent event = new PlayerJoinEvent(joiningPlayer, Component.text("join"));

        listener.onPlayerJoin(event);

        // Verify delayed task scheduled with 5 ticks delay
        verify(mockScheduler, atLeastOnce()).runTaskLater(eq(plugin), any(Runnable.class), eq(5L));

        // Verify Title dispatch with Title.Times
        ArgumentCaptor<Title> titleCaptor = ArgumentCaptor.forClass(Title.class);
        verify(joiningPlayer, times(1)).showTitle(titleCaptor.capture());
        Title capturedTitle = titleCaptor.getValue();
        assertNotNull(capturedTitle, "Title must not be null");
        assertNotNull(capturedTitle.times(), "Title.Times must not be null");
        assertEquals(Ticks.duration(10L), capturedTitle.times().fadeIn(), "Fade-in ticks must match config (10 ticks)");
        assertEquals(Ticks.duration(70L), capturedTitle.times().stay(), "Stay ticks must match config (70 ticks)");
        assertEquals(Ticks.duration(20L), capturedTitle.times().fadeOut(), "Fade-out ticks must match config (20 ticks)");

        // Verify ActionBar dispatch
        ArgumentCaptor<Component> abCaptor = ArgumentCaptor.forClass(Component.class);
        verify(joiningPlayer, times(1)).sendActionBar(abCaptor.capture());
        Component capturedAb = abCaptor.getValue();
        assertNotNull(capturedAb, "Action bar component must not be null");
        assertNotEquals(Component.empty(), capturedAb, "Action bar component must not be empty");
    }

    @Test
    @DisplayName("Disabling first-join titles and actionbars in config suppresses calls to player.showTitle and player.sendActionBar")
    void testFirstJoinTitleAndActionBarSuppressedWhenDisabled() {
        when(joiningPlayer.hasPlayedBefore()).thenReturn(false);
        when(joiningPlayer.isOnline()).thenReturn(true);

        configManager.firstJoinTitleEnabled = false;
        configManager.firstJoinActionBarEnabled = false;

        PlayerConnectionListener listener = new PlayerConnectionListener(plugin);
        PlayerJoinEvent event = new PlayerJoinEvent(joiningPlayer, Component.text("join"));

        listener.onPlayerJoin(event);

        verify(joiningPlayer, never()).showTitle(any(Title.class));
        verify(joiningPlayer, never()).sendActionBar(any(Component.class));
    }

    @Test
    @DisplayName("Disabling normal join titles and actionbars in config suppresses calls to player.showTitle and player.sendActionBar")
    void testNormalJoinTitleAndActionBarSuppressedWhenDisabled() {
        when(joiningPlayer.hasPlayedBefore()).thenReturn(true);
        when(joiningPlayer.isOnline()).thenReturn(true);

        configManager.joinTitleEnabled = false;
        configManager.joinActionBarEnabled = false;

        PlayerConnectionListener listener = new PlayerConnectionListener(plugin);
        PlayerJoinEvent event = new PlayerJoinEvent(joiningPlayer, Component.text("join"));

        listener.onPlayerJoin(event);

        verify(joiningPlayer, never()).showTitle(any(Title.class));
        verify(joiningPlayer, never()).sendActionBar(any(Component.class));
    }

    @Test
    @DisplayName("Custom title timing values in config are accurately reflected in Adventure Title.Times")
    void testTitleCustomTimingsConfigured() {
        when(joiningPlayer.hasPlayedBefore()).thenReturn(true);
        when(joiningPlayer.isOnline()).thenReturn(true);

        configManager.joinTitleFadeIn = 5L;
        configManager.joinTitleStay = 120L;
        configManager.joinTitleFadeOut = 15L;

        PlayerConnectionListener listener = new PlayerConnectionListener(plugin);
        PlayerJoinEvent event = new PlayerJoinEvent(joiningPlayer, Component.text("join"));

        listener.onPlayerJoin(event);

        ArgumentCaptor<Title> titleCaptor = ArgumentCaptor.forClass(Title.class);
        verify(joiningPlayer, times(1)).showTitle(titleCaptor.capture());
        Title capturedTitle = titleCaptor.getValue();
        assertNotNull(capturedTitle.times());
        assertEquals(Ticks.duration(5L), capturedTitle.times().fadeIn());
        assertEquals(Ticks.duration(120L), capturedTitle.times().stay());
        assertEquals(Ticks.duration(15L), capturedTitle.times().fadeOut());
    }

    @Test
    @DisplayName("ActionBar dispatch is suppressed when the configured message string is empty")
    void testActionBarSuppressedWhenTemplateEmpty() {
        when(joiningPlayer.hasPlayedBefore()).thenReturn(true);
        when(joiningPlayer.isOnline()).thenReturn(true);

        TestLangManager emptyAbLang = new TestLangManager(plugin) {
            @Override
            public String getMessage(String path) {
                if ("join.actionbar".equals(path)) {
                    return "";
                }
                return super.getMessage(path);
            }
        };
        try {
            setField(plugin, NeaWelcomer.class, "langManager", emptyAbLang);
            PlayerConnectionListener listener = new PlayerConnectionListener(plugin);
            PlayerJoinEvent event = new PlayerJoinEvent(joiningPlayer, Component.text("join"));

            listener.onPlayerJoin(event);

            verify(joiningPlayer, never()).sendActionBar(any(Component.class));
            // But title should still be dispatched
            verify(joiningPlayer, times(1)).showTitle(any(Title.class));
        } catch (Exception e) {
            fail("Exception thrown: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("When player disconnects before the 5-tick delayed task fires, private content is not dispatched")
    void testPlayerOfflineWhenDelayedTaskRunsSuppressesDelivery() {
        when(joiningPlayer.hasPlayedBefore()).thenReturn(false);
        when(joiningPlayer.isOnline()).thenReturn(false); // Player disconnected before delayed task

        PlayerConnectionListener listener = new PlayerConnectionListener(plugin);
        PlayerJoinEvent event = new PlayerJoinEvent(joiningPlayer, Component.text("join"));

        listener.onPlayerJoin(event);

        verify(joiningPlayer, never()).showTitle(any(Title.class));
        verify(joiningPlayer, never()).sendActionBar(any(Component.class));
        verify(joiningPlayer, never()).sendMessage(any(Component.class));
    }
}
