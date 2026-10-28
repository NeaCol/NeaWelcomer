/*
 * This Metrics class was auto-generated and can be copied into your project if you are
 * not using a build tool like Gradle or Maven for dependency management.
 *
 * IMPORTANT: You are not allowed to modify this class, except changing the package.
 */
package com.neaplugins.welcomer.metrics;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;
import javax.net.ssl.HttpsURLConnection;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class Metrics {

    private final Plugin plugin;
    private final int pluginId;

    private static final String B_STATS_VERSION = "3.0.2";
    private static final String URL = "https://bStats.org/submitData/bukkit";

    private final ScheduledExecutorService scheduler;
    private final Set<CustomChart> charts = new HashSet<>();
    private final boolean enabled;

    private static final String METRICS_FILE_NAME = "config.yml";
    private static final String METRICS_FOLDER_NAME = "bStats";

    public Metrics(Plugin plugin, int pluginId) {
        this.plugin = plugin;
        this.pluginId = pluginId;

        File bStatsFolder = new File(plugin.getDataFolder().getParentFile(), METRICS_FOLDER_NAME);
        File configFile = new File(bStatsFolder, METRICS_FILE_NAME);
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        if (!config.isSet("serverUuid")) {
            config.addDefault("enabled", true);
            config.addDefault("serverUuid", UUID.randomUUID().toString());
            config.addDefault("logFailedRequests", false);
            config.addDefault("logSentData", false);
            config.addDefault("logResponseStatusText", false);

            config.options().header("bStats (https://bStats.org) collects some basic information for plugin authors.\n"
                    + "All data is anonymous and cannot be used to identify players.")
                .copyDefaults(true);
            try {
                config.save(configFile);
            } catch (IOException ignored) {}
        }

        enabled = config.getBoolean("enabled", true);
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1, r -> {
            Thread thread = new Thread(r, "bStats-Metrics");
            thread.setDaemon(true);
            return thread;
        });
        executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        scheduler = executor;

        if (enabled) {
            startSubmitting();
        }
    }

    public void addCustomChart(CustomChart chart) {
        this.charts.add(chart);
    }

    private void startSubmitting() {
        final Runnable submitTask = () -> {
            if (!plugin.isEnabled()) {
                scheduler.shutdown();
                return;
            }
            submitData();
        };

        long initialDelay = (long) (20 * 3 * (Math.random() * 2));
        long secondDelay = (long) (20 * (Math.random() * 60));
        scheduler.schedule(submitTask, initialDelay, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(submitTask, initialDelay + secondDelay, 1800, TimeUnit.SECONDS);
    }

    private void submitData() {
        // Simple metric submission payload
        try {
            HttpsURLConnection connection = (HttpsURLConnection) new URL(URL).openConnection();
            connection.setRequestMethod("POST");
            connection.addRequestProperty("Accept", "application/json");
            connection.addRequestProperty("Connection", "close");
            connection.addRequestProperty("Content-Type", "application/json");
            connection.addRequestProperty("User-Agent", "Metrics-Service/1");
            connection.setDoOutput(true);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            String serverUuid = UUID.randomUUID().toString();
            String json = "{\"service\":{\"id\":" + pluginId + "},\"serverUuid\":\"" + serverUuid + "\"}";

            byte[] compressed = compress(json);
            connection.setRequestProperty("Content-Encoding", "gzip");
            connection.setRequestProperty("Content-Length", Integer.toString(compressed.length));
            try (DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream())) {
                outputStream.write(compressed);
            }
            connection.getResponseCode();
        } catch (Exception ignored) {}
    }

    private static byte[] compress(final String str) throws IOException {
        if (str == null) return null;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(outputStream)) {
            gzip.write(str.getBytes(StandardCharsets.UTF_8));
        }
        return outputStream.toByteArray();
    }

    public abstract static class CustomChart {
        private final String chartId;
        protected CustomChart(String chartId) {
            this.chartId = chartId;
        }
        public String getChartId() {
            return chartId;
        }
    }
}
