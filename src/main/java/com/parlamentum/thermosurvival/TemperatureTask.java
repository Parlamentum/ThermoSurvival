package com.parlamentum.thermosurvival;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

public class TemperatureTask extends BukkitRunnable {

    private final ThermoSurvivalPlugin plugin;
    private final TemperatureManager manager;
    private int tickCounter = 0;

    public TemperatureTask(ThermoSurvivalPlugin plugin, TemperatureManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            // Checks
            if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
                continue;
            }

            if (player.hasPermission("thermosurvival.bypass")) {
                continue;
            }

            List<String> disabledWorlds = plugin.getConfig().getStringList("disabled-worlds");
            if (disabledWorlds.contains(player.getWorld().getName())) {
                continue;
            }

            double currentTemp = manager.getTemperature(player);
            double targetTemp = manager.calculateTargetTemperature(player);

            // Drift towards target
            double diff = targetTemp - currentTemp;
            double change = diff * 0.1;
            if (Math.abs(change) < 0.1) {
                change = Math.signum(diff) * 0.1;
            }
            if (Math.abs(change) > Math.abs(diff)) {
                change = diff;
            }

            double newTemp = currentTemp + change;
            manager.setTemperature(player, newTemp);

            applyEffects(player, newTemp);
            sendActionBar(player, newTemp);
        }
        tickCounter++;
    }

    private void applyEffects(Player player, double temp) {
        ConfigurationSection thresholds = plugin.getConfig().getConfigurationSection("thresholds");
        if (thresholds == null)
            return;

        // Find the most severe applicable threshold
        String[] coldLevels = { "cold_extreme", "cold_severe", "cold_moderate", "cold_mild" };
        String[] heatLevels = { "heat_extreme", "heat_severe", "heat_moderate", "heat_mild" };

        boolean effectApplied = false;
        boolean worldBorderEffect = false;

        // Check cold thresholds (from most severe to least)
        for (String level : coldLevels) {
            if (thresholds.contains(level)) {
                double trigger = thresholds.getDouble(level + ".trigger");
                if (temp <= trigger) {
                    List<String> effects = thresholds.getStringList(level + ".effects");
                    applyPotionEffects(player, effects);

                    // Apply damage if configured
                    if (thresholds.contains(level + ".damage-interval")) {
                        int dmgInterval = thresholds.getInt(level + ".damage-interval", 200);
                        if (tickCounter % (dmgInterval / plugin.getConfig().getLong("update-interval", 20)) == 0) {
                            player.damage(thresholds.getDouble(level + ".damage-amount", 1.0));
                        }
                    }

                    // Check for world border effect
                    if (thresholds.getBoolean(level + ".world-border-effect", false)) {
                        worldBorderEffect = true;
                    }

                    effectApplied = true;
                    break; // Only apply the most severe level
                }
            }
        }

        // Check heat thresholds (from most severe to least)
        if (!effectApplied) {
            for (String level : heatLevels) {
                if (thresholds.contains(level)) {
                    double trigger = thresholds.getDouble(level + ".trigger");
                    if (temp >= trigger) {
                        List<String> effects = thresholds.getStringList(level + ".effects");
                        applyPotionEffects(player, effects);

                        // Apply damage if configured
                        if (thresholds.contains(level + ".damage-interval")) {
                            int dmgInterval = thresholds.getInt(level + ".damage-interval", 200);
                            if (tickCounter % (dmgInterval / plugin.getConfig().getLong("update-interval", 20)) == 0) {
                                player.damage(thresholds.getDouble(level + ".damage-amount", 1.0));
                            }
                        }

                        // Check for fire ticks
                        if (thresholds.contains(level + ".fire-tick-trigger")) {
                            double fireTrigger = thresholds.getDouble(level + ".fire-tick-trigger");
                            if (temp >= fireTrigger) {
                                player.setFireTicks(40);
                            }
                        }

                        // Check for world border effect
                        if (thresholds.getBoolean(level + ".world-border-effect", false)) {
                            worldBorderEffect = true;
                        }

                        break; // Only apply the most severe level
                    }
                }
            }
        }

        // Apply world border effect if needed (creates dark vignette like approaching
        // world border)
        if (worldBorderEffect) {
            // Use wither effect for visual indicator
            player.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 60, 0, true, false, false));
        }
    }

    private void applyPotionEffects(Player player, List<String> effects) {
        for (String effectStr : effects) {
            String[] parts = effectStr.split(":");
            PotionEffectType type = PotionEffectType.getByName(parts[0]);
            if (type != null) {
                int amplifier = 0;
                if (parts.length > 1) {
                    try {
                        amplifier = Integer.parseInt(parts[1]);
                    } catch (NumberFormatException ignored) {
                    }
                }
                int duration = (int) plugin.getConfig().getLong("update-interval", 20) + 20;
                player.addPotionEffect(new PotionEffect(type, duration, amplifier, true, false, true));
            }
        }
    }

    private void sendActionBar(Player player, double temp) {
        if (!plugin.getConfig().getBoolean("ui.actionbar", true))
            return;

        // Dynamic thresholds
        double coldTrigger = -5.0;
        double heatTrigger = 50.0;
        ConfigurationSection thresholds = plugin.getConfig().getConfigurationSection("thresholds");
        if (thresholds != null) {
            coldTrigger = thresholds.getDouble("cold.trigger", -5.0);
            heatTrigger = thresholds.getDouble("heat.trigger", 50.0);
        }

        ChatColor color = ChatColor.GREEN;
        if (temp >= heatTrigger)
            color = ChatColor.RED;
        else if (temp <= coldTrigger)
            color = ChatColor.AQUA;

        String msg = color + "Temp: " + String.format("%.1f", temp) + "°";
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(msg));
    }
}
