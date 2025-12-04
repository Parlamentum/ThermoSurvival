package com.parlamentum.thermosurvival;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TemperatureManager {

    private final ThermoSurvivalPlugin plugin;
    private final WorldGuardHook worldGuardHook;
    private final Map<UUID, Double> playerTemperatures = new HashMap<>();
    private final Map<UUID, BossBar> playerBossBars = new HashMap<>();

    public TemperatureManager(ThermoSurvivalPlugin plugin, WorldGuardHook worldGuardHook) {
        this.plugin = plugin;
        this.worldGuardHook = worldGuardHook;
    }

    public double getTemperature(Player player) {
        return playerTemperatures.getOrDefault(player.getUniqueId(), plugin.getConfig().getDouble("base-temp", 0.0));
    }

    public void setTemperature(Player player, double temp) {
        double min = plugin.getConfig().getDouble("min-temp", -100);
        double max = plugin.getConfig().getDouble("max-temp", 100);
        temp = Math.max(min, Math.min(max, temp));
        playerTemperatures.put(player.getUniqueId(), temp);
        updateBossBar(player, temp);
    }

    public void modifyTemperature(Player player, double amount) {
        setTemperature(player, getTemperature(player) + amount);
    }

    public double calculateTargetTemperature(Player player) {
        // Check if player is in a WorldGuard disabled region
        if (worldGuardHook.isInDisabledRegion(player)) {
            return plugin.getConfig().getDouble("base-temp", 0.0);
        }

        double target = plugin.getConfig().getDouble("base-temp", 0.0);

        // Biome
        String biomeName = player.getLocation().getBlock().getBiome().name();
        ConfigurationSection biomes = plugin.getConfig().getConfigurationSection("biomes");
        if (biomes != null && biomes.contains(biomeName)) {
            target = biomes.getDouble(biomeName);
        } else {
            if (biomeName.contains("SNOW") || biomeName.contains("ICE")) {
                target -= 20;
            } else if (biomeName.contains("DESERT") || biomeName.contains("BADLANDS")) {
                target += 20;
            } else if (biomeName.contains("NETHER")) {
                target += 50;
            }
        }

        // Height
        int y = player.getLocation().getBlockY();
        if (y > 80) {
            target -= (y - 80) / 10.0;
        } else if (y < 40) {
            target += (40 - y) / 10.0;
        }

        // Weather
        if (player.getWorld().hasStorm()) {
            if (player.getWorld().isThundering()) {
                target += plugin.getConfig().getDouble("weather.storming", -8);
            } else {
                double rainMod = plugin.getConfig().getDouble("weather.raining", -5);
                double snowMod = plugin.getConfig().getDouble("weather.snowing", -10);
                if (target < 0) {
                    target += snowMod;
                } else {
                    target += rainMod;
                }
            }
        }

        // Time of Day
        long time = player.getWorld().getTime();
        if (time > 12300 && time < 23850) { // Night
            target += plugin.getConfig().getDouble("time.night-modifier", -10);
        } else {
            target += plugin.getConfig().getDouble("time.day-modifier", 0);
        }

        // Armor
        ConfigurationSection armorConfig = plugin.getConfig().getConfigurationSection("armor");
        if (armorConfig != null) {
            for (ItemStack item : player.getInventory().getArmorContents()) {
                if (item != null && armorConfig.contains(item.getType().name())) {
                    target += armorConfig.getDouble(item.getType().name());
                }
            }
        }

        // Nearby Blocks (Variable Radius)
        ConfigurationSection blocks = plugin.getConfig().getConfigurationSection("blocks");
        int defaultRadius = plugin.getConfig().getInt("default-block-scan-radius", 2);

        if (blocks != null) {
            // Optimization: Instead of scanning a huge area, we scan a reasonable max area
            // and check distance for specific blocks?
            // OR: Just scan the max possible radius needed.
            // Let's find max radius first or just pick a safe upper limit like 5.
            int maxRadius = 5;

            Block center = player.getLocation().getBlock();
            for (int x = -maxRadius; x <= maxRadius; x++) {
                for (int yOffset = -maxRadius; yOffset <= maxRadius; yOffset++) {
                    for (int z = -maxRadius; z <= maxRadius; z++) {
                        Block b = center.getRelative(x, yOffset, z);
                        String type = b.getType().name();
                        if (blocks.contains(type)) {
                            // Check if it's a simple value or section
                            double blockTemp = 0;
                            int blockRadius = defaultRadius;

                            if (blocks.isConfigurationSection(type)) {
                                blockTemp = blocks.getDouble(type + ".temp");
                                blockRadius = blocks.getInt(type + ".radius", defaultRadius);
                            } else {
                                blockTemp = blocks.getDouble(type);
                            }

                            // Check distance
                            if (Math.abs(x) <= blockRadius && Math.abs(yOffset) <= blockRadius
                                    && Math.abs(z) <= blockRadius) {
                                target += blockTemp;
                            }
                        }
                    }
                }
            }
        }

        return target;
    }

    private void updateBossBar(Player player, double temp) {
        if (!plugin.getConfig().getBoolean("ui.bossbar", true))
            return;

        BossBar bar = playerBossBars.computeIfAbsent(player.getUniqueId(),
                k -> Bukkit.createBossBar("Temperature", BarColor.WHITE, BarStyle.SOLID));

        if (!bar.getPlayers().contains(player)) {
            bar.addPlayer(player);
        }

        double min = plugin.getConfig().getDouble("min-temp", -30);
        double max = plugin.getConfig().getDouble("max-temp", 50);
        double range = max - min;
        double progress = (temp - min) / range;
        progress = Math.max(0.0, Math.min(1.0, progress));

        bar.setProgress(progress);

        // Determine color and title based on progressive thresholds
        ConfigurationSection thresholds = plugin.getConfig().getConfigurationSection("thresholds");
        String status = "Normal";
        BarColor color = BarColor.GREEN;

        if (thresholds != null) {
            // Check extreme cold
            if (thresholds.contains("cold_extreme") && temp <= thresholds.getDouble("cold_extreme.trigger")) {
                color = BarColor.BLUE;
                status = "FREEZING";
            }
            // Check severe cold
            else if (thresholds.contains("cold_severe") && temp <= thresholds.getDouble("cold_severe.trigger")) {
                color = BarColor.BLUE;
                status = "VERY COLD";
            }
            // Check moderate cold
            else if (thresholds.contains("cold_moderate") && temp <= thresholds.getDouble("cold_moderate.trigger")) {
                color = BarColor.BLUE;
                status = "Cold";
            }
            // Check mild cold
            else if (thresholds.contains("cold_mild") && temp <= thresholds.getDouble("cold_mild.trigger")) {
                color = BarColor.WHITE;
                status = "Cool";
            }
            // Check extreme heat
            else if (thresholds.contains("heat_extreme") && temp >= thresholds.getDouble("heat_extreme.trigger")) {
                color = BarColor.RED;
                status = "BURNING";
            }
            // Check severe heat
            else if (thresholds.contains("heat_severe") && temp >= thresholds.getDouble("heat_severe.trigger")) {
                color = BarColor.RED;
                status = "VERY HOT";
            }
            // Check moderate heat
            else if (thresholds.contains("heat_moderate") && temp >= thresholds.getDouble("heat_moderate.trigger")) {
                color = BarColor.YELLOW;
                status = "Hot";
            }
            // Check mild heat
            else if (thresholds.contains("heat_mild") && temp >= thresholds.getDouble("heat_mild.trigger")) {
                color = BarColor.YELLOW;
                status = "Warm";
            }
        }

        bar.setColor(color);
        bar.setTitle("Temperature: " + status + " (" + String.format("%.1f", temp) + "°C)");
    }

    public void cleanup() {
        for (BossBar bar : playerBossBars.values()) {
            bar.removeAll();
        }
        playerBossBars.clear();
        playerTemperatures.clear();
    }

    public void removePlayer(Player player) {
        playerTemperatures.remove(player.getUniqueId());
        BossBar bar = playerBossBars.remove(player.getUniqueId());
        if (bar != null) {
            bar.removePlayer(player);
        }
    }
}
