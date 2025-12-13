package com.parlamentum.thermosurvival;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ThermoCommandExecutor implements CommandExecutor {

    private final ThermoSurvivalPlugin plugin;
    private final TemperatureManager manager;

    public ThermoCommandExecutor(ThermoSurvivalPlugin plugin, TemperatureManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("thermosurvival.admin")) {
            sender.sendMessage(plugin.getMessage("no-permission", "&cYou do not have permission to use this command."));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(plugin.getMessage("usage-main", "&cUsage: /thermo <reload|toggle>"));
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "reload":
                plugin.reloadConfig();
                sender.sendMessage(plugin.getMessage("config-reloaded", "&aThermoSurvival configuration reloaded!"));
                break;
            case "toggle":
                if (args.length < 2) {
                    sender.sendMessage(plugin.getMessage("usage-toggle", "&cUsage: /thermo toggle <bossbar|actionbar>"));
                    return true;
                }
                String type = args[1].toLowerCase();
                if (type.equals("bossbar")) {
                    boolean current = plugin.getConfig().getBoolean("ui.bossbar", true);
                    plugin.getConfig().set("ui.bossbar", !current);
                    plugin.saveConfig();
                    // Force update for online players
                    if (!current) { // If we just turned it on
                        // Nothing needed, next tick will add it.
                    } else { // If we just turned it off
                        manager.cleanup(); // Remove all bars
                    }
                    String toggleMsg = !current ? 
                        plugin.getMessage("bossbar-toggled-on", "&aBossBar toggled ON") :
                        plugin.getMessage("bossbar-toggled-off", "&aBossBar toggled OFF");
                    sender.sendMessage(toggleMsg);
                } else if (type.equals("actionbar")) {
                    boolean current = plugin.getConfig().getBoolean("ui.actionbar", true);
                    plugin.getConfig().set("ui.actionbar", !current);
                    plugin.saveConfig();
                    String toggleMsg = !current ? 
                        plugin.getMessage("actionbar-toggled-on", "&aActionBar toggled ON") :
                        plugin.getMessage("actionbar-toggled-off", "&aActionBar toggled OFF");
                    sender.sendMessage(toggleMsg);
                } else {
                    sender.sendMessage(plugin.getMessage("invalid-toggle", "&cInvalid toggle option. Use bossbar or actionbar."));
                }
                break;
            default:
                sender.sendMessage(plugin.getMessage("unknown-command", "&cUnknown command."));
                break;
        }

        return true;
    }
}
