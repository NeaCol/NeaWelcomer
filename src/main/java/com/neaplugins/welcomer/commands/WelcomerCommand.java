package com.neaplugins.welcomer.commands;

import com.neaplugins.welcomer.NeaWelcomer;
import com.neaplugins.welcomer.constants.Permissions;
import com.neaplugins.welcomer.utils.MessageUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WelcomerCommand implements CommandExecutor, TabCompleter {

    private final NeaWelcomer plugin;

    public WelcomerCommand(NeaWelcomer plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(
        @NotNull CommandSender sender,
        @NotNull Command command,
        @NotNull String label,
        @NotNull String[] args
    ) {
        if (!sender.hasPermission(Permissions.ADMIN)) {
            String noPermMsg = plugin
                .getLangManager()
                .getMessage("commands.no-permission");
            if (noPermMsg.isEmpty()) {
                noPermMsg =
                    "{prefix}<#F44336>No tienes permiso para ejecutar este comando.";
            }
            noPermMsg = noPermMsg.replace(
                "{prefix}",
                plugin.getLangManager().getPrefix()
            );
            sender.sendMessage(MessageUtils.parse(noPermMsg));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                long start = System.currentTimeMillis();
                try {
                    plugin.reload();
                    long elapsed = System.currentTimeMillis() - start;

                    String reloadMsg = plugin
                        .getLangManager()
                        .getMessage("commands.reload-success");
                    if (reloadMsg.isEmpty()) {
                        reloadMsg =
                            "{prefix}<#4CAF50>Configuración y lenguaje recargados correctamente. <dark_gray>(<#9E9E9E>{time}ms<dark_gray>)";
                    }
                    reloadMsg = reloadMsg.replace(
                        "{prefix}",
                        plugin.getLangManager().getPrefix()
                    );
                    reloadMsg = reloadMsg.replace(
                        "{time}",
                        String.valueOf(elapsed)
                    );
                    sender.sendMessage(MessageUtils.parse(reloadMsg));
                } catch (Exception e) {
                    String failMsg = plugin
                        .getLangManager()
                        .getMessage("commands.reload-fail");
                    if (failMsg.isEmpty()) {
                        failMsg =
                            "{prefix}<#F44336>Error al recargar la configuración.";
                    }
                    failMsg = failMsg.replace(
                        "{prefix}",
                        plugin.getLangManager().getPrefix()
                    );
                    sender.sendMessage(MessageUtils.parse(failMsg));
                    plugin
                        .getLogger()
                        .severe(
                            "Error reloading configuration: " + e.getMessage()
                        );
                    e.printStackTrace();
                }
            }
            case "help" -> sendHelp(sender);
            case "info" -> sendInfo(sender);
            default -> sendHelp(sender);
        }

        return true;
    }

    private void sendInfo(CommandSender sender) {
        String version = plugin.getDescription().getVersion();
        boolean papiAvailable = MessageUtils.isPlaceholderAPIAvailable();

        String[] info = {
            "",
            "<gradient:#7C4DFF:#B388FF>  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</gradient>",
            "",
            "  <#7C4DFF><bold>NeaWelcomer</bold> <dark_gray>- <gray>v" +
            version,
            "  <gray>Autor: <#B388FF>NeaPlugins",
            "  <gray>API: <#B388FF>Paper/Purpur 1.21.1",
            "  <gray>MiniMessage: <#4CAF50>Activado",
            "  <gray>PlaceholderAPI: " +
            (papiAvailable ? "<#4CAF50>Conectado" : "<#F44336>No encontrado"),
            "",
            "<gradient:#7C4DFF:#B388FF>  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</gradient>",
            "",
        };
        for (String line : info) {
            sender.sendMessage(MessageUtils.parse(line));
        }
    }

    private void sendHelp(CommandSender sender) {
        // Try to get help from lang.yml first
        List<String> helpLines = plugin
            .getLangManager()
            .getMessageList("commands.help");

        if (!helpLines.isEmpty()) {
            for (String line : helpLines) {
                sender.sendMessage(MessageUtils.parse(line));
            }
            return;
        }

        // Fallback built-in help
        String[] help = {
            "",
            "<gradient:#7C4DFF:#B388FF>  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</gradient>",
            "",
            "  <#7C4DFF><bold>NeaWelcomer</bold> <dark_gray>- <gray>Comandos",
            "",
            "  <#B388FF>/neawelcomer reload <dark_gray>» <gray>Recarga la configuración",
            "  <#B388FF>/neawelcomer info <dark_gray>» <gray>Información del plugin",
            "  <#B388FF>/neawelcomer help <dark_gray>» <gray>Muestra esta ayuda",
            "",
            "  <dark_gray>Aliases: <gray>/nw, /welcomer",
            "",
            "<gradient:#7C4DFF:#B388FF>  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</gradient>",
            "",
        };
        for (String line : help) {
            sender.sendMessage(MessageUtils.parse(line));
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(
        @NotNull CommandSender sender,
        @NotNull Command command,
        @NotNull String alias,
        @NotNull String[] args
    ) {
        if (!sender.hasPermission(Permissions.ADMIN)) {
            return new ArrayList<>();
        }

        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("reload");
            completions.add("info");
            completions.add("help");
            return completions
                .stream()
                .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }

        return completions;
    }
}
