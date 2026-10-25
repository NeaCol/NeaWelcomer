package com.neaplugins.welcomer.managers;

import com.neaplugins.welcomer.NeaWelcomer;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

/**
 * Tracks the total count of unique players who have ever connected to the server.
 * <p>
 * Performs an initial asynchronous scan using Java NIO {@link DirectoryStream}
 * on the world's playerdata directory to avoid blocking the Paper/Purpur main server thread.
 * Once initialized, all queries are O(1) non-blocking reads from an {@link AtomicInteger}.
 */
public class UniquePlayerTracker {

    private final NeaWelcomer plugin;
    private final AtomicInteger uniqueCount = new AtomicInteger(0);
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    /**
     * Constructs a new UniquePlayerTracker.
     *
     * @param plugin The NeaWelcomer plugin instance.
     */
    public UniquePlayerTracker(NeaWelcomer plugin) {
        this.plugin = plugin;
    }

    /**
     * Initializes the unique player count asynchronously off the main thread.
     * Prevents any server tick drops or watchdog timeouts during startup or reload.
     *
     * @return A CompletableFuture completing with the counted unique player total.
     */
    public CompletableFuture<Integer> initializeAsync() {
        return CompletableFuture.supplyAsync(this::scanPlayerdataFolder)
            .thenApply(count -> {
                uniqueCount.set(count);
                initialized.set(true);
                if (plugin != null) {
                    plugin.getLogger().info("Cached " + count + " unique players from disk.");
                }
                return count;
            })
            .exceptionally(ex -> {
                if (plugin != null) {
                    plugin.getLogger().log(
                        Level.WARNING,
                        "Failed to count unique players asynchronously, using fallback",
                        ex
                    );
                }
                int fallback = 0;
                try {
                    if (Bukkit.getServer() != null && Bukkit.getOnlinePlayers() != null) {
                        fallback = Bukkit.getOnlinePlayers().size();
                    }
                } catch (Throwable ignored) {}
                uniqueCount.set(fallback);
                initialized.set(true);
                return fallback;
            });
    }

    /**
     * Scans the default world's playerdata folder using Java NIO DirectoryStream.
     *
     * @return The number of .dat files found, or fallback player count if unavailable.
     */
    private int scanPlayerdataFolder() {
        try {
            if (Bukkit.getServer() == null || Bukkit.getWorlds().isEmpty()) {
                return getOfflineFallback();
            }

            World defaultWorld = Bukkit.getWorlds().get(0);
            if (defaultWorld == null || defaultWorld.getWorldFolder() == null) {
                return getOfflineFallback();
            }

            Path playerdataPath = defaultWorld.getWorldFolder().toPath().resolve("playerdata");
            if (!Files.exists(playerdataPath) || !Files.isDirectory(playerdataPath)) {
                return getOfflineFallback();
            }

            int count = 0;
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(playerdataPath, "*.dat")) {
                for (Path ignored : stream) {
                    count++;
                }
            }

            int online = (Bukkit.getOnlinePlayers() != null) ? Bukkit.getOnlinePlayers().size() : 0;
            return Math.max(count, online);
        } catch (IOException e) {
            if (plugin != null) {
                plugin.getLogger().log(Level.WARNING, "I/O error scanning playerdata directory", e);
            }
            return getOfflineFallback();
        } catch (Throwable t) {
            return getOfflineFallback();
        }
    }

    /**
     * Fallback player count using Bukkit.getOfflinePlayers().
     */
    private int getOfflineFallback() {
        try {
            if (Bukkit.getServer() != null) {
                return Bukkit.getOfflinePlayers().length;
            }
        } catch (Throwable ignored) {}
        return 0;
    }

    /**
     * Returns the cached unique player count in O(1) time without disk I/O.
     *
     * @return The total number of unique players ever joined.
     */
    public int getUniquePlayerCount() {
        return uniqueCount.get();
    }

    /**
     * Atomically increments the unique player count by one.
     * Called when a player joins the server for the first time.
     *
     * @return The updated unique player count.
     */
    public int incrementAndGet() {
        return uniqueCount.incrementAndGet();
    }

    /**
     * Checks whether the initial scan has completed.
     *
     * @return true if initialized, false otherwise.
     */
    public boolean isInitialized() {
        return initialized.get();
    }
}
