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

        // Cold
        if (thresholds.contains("cold")) {
            double trigger = thresholds.getDouble("cold.trigger");
            if (temp <= trigger) {
                List<String> effects = thresholds.getStringList("cold.effects");
                applyPotionEffects(player, effects);

                int dmgInterval = thresholds.getInt("cold.damage-interval", 200);
                if (tickCounter % (dmgInterval / plugin.getConfig().getLong("update-interval", 20)) == 0) {
                    player.damage(thresholds.getDouble("cold.damage-amount", 1.0));
                }
            }
        }

        // Heat
        if (thresholds.contains("heat")) {
            double trigger = thresholds.getDouble("heat.trigger");
            if (temp >= trigger) {
                List<String> effects = thresholds.getStringList("heat.effects");
                applyPotionEffects(player, effects);

                double fireTrigger = thresholds.getDouble("heat.fire-tick-trigger", 90);
                if (temp >= fireTrigger) {
                    player.setFireTicks(40);
                }
            }
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

        ChatColor color = ChatColor.GREEN;
        if (temp > 40)
            color = ChatColor.RED;
        else if (temp < -20)
            color = ChatColor.AQUA;

        String msg = color + "Temp: " + String.format("%.1f", temp) + "°";
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(msg));
    }
}
