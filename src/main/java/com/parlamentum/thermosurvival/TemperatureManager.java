package com.parlamentum.thermosurvival;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import org.bukkit.World;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class TemperatureManager {

    private final ThermoSurvivalPlugin plugin;
    private final WorldGuardHook worldGuardHook;
    private final ConfigCache configCache;
    private final Map<UUID, Double> playerTemperatures = new HashMap<>();
    private final Map<UUID, BossBar> playerBossBars = new HashMap<>();

    public TemperatureManager(ThermoSurvivalPlugin plugin, WorldGuardHook worldGuardHook) {
        this.plugin = plugin;
        this.worldGuardHook = worldGuardHook;
        this.configCache = new ConfigCache(plugin);
    }

    public double getTemperature(Player player) {
        return playerTemperatures.getOrDefault(player.getUniqueId(), configCache.getBaseTemp());
    }

    public void setTemperature(Player player, double temp) {
        temp = Math.max(configCache.getMinTemp(), Math.min(configCache.getMaxTemp(), temp));
        playerTemperatures.put(player.getUniqueId(), temp);
        updateBossBar(player, temp);
    }

    public void modifyTemperature(Player player, double amount) {
        setTemperature(player, getTemperature(player) + amount);
    }

    public double calculateTargetTemperature(Player player) {
        configCache.checkAndUpdateCache();
        
        // Check if player is in a WorldGuard disabled region
        if (worldGuardHook.isInDisabledRegion(player)) {
            return configCache.getBaseTemp();
        }

        double target = configCache.getBaseTemp();
        Location loc = player.getLocation();
        
        // Check if chunk is loaded (performance optimization)
        if (!loc.getChunk().isLoaded()) {
            return target; // Return base temp if chunk not loaded
        }

        // Biome - use cached value
        String biomeName = player.getWorld().getBiome(loc).name();
        Double biomeTemp = configCache.getBiomeTemp(biomeName);
        if (biomeTemp != null) {
            target = biomeTemp;
        } else {
            // Fallback biome detection
            if (biomeName.contains("SNOW") || biomeName.contains("ICE")) {
                target -= 20;
            } else if (biomeName.contains("DESERT") || biomeName.contains("BADLANDS")) {
                target += 20;
            } else if (biomeName.contains("NETHER")) {
                target += 50;
            }
        }

        // Height and Caves
        int y = loc.getBlockY();
        Block block = loc.getBlock();
        
        if (y > 80) {
            target -= (y - 80) / 10.0;
        } else if (y < 40) {
            // Caves are cold - check if player is underground
            boolean isUnderground = false;
            int skyLight = block.getLightFromSky();
            
            // If sky light is very low, player is likely in a cave
            if (skyLight < 4) {
                isUnderground = true;
            } else {
                // Check if there are solid blocks above (cave ceiling) - only check 5 blocks for performance
                int blocksAbove = 0;
                for (int i = 1; i <= 5; i++) {
                    Block above = loc.clone().add(0, i, 0).getBlock();
                    if (above.getType().isSolid()) {
                        blocksAbove++;
                    }
                }
                if (blocksAbove >= 3) {
                    isUnderground = true;
                }
            }
            
            if (isUnderground) {
                // Caves are cold - apply cold modifier based on depth
                int depth = 40 - y; // How deep below Y=40
                target += configCache.getCaveColdModifier() * depth; // Deeper = colder
            } else {
                // Not in cave, normal underground warmth
                target += (40 - y) / 10.0;
            }
        }

        // Weather - use cached values
        World world = player.getWorld();
        if (world.hasStorm()) {
            if (world.isThundering()) {
                target += configCache.getStormModifier();
            } else {
                if (target < 0) {
                    target += configCache.getSnowModifier();
                } else {
                    target += configCache.getRainModifier();
                }
            }
        }

        // Time of Day - use cached values
        long time = world.getTime();
        if (time > 12300 && time < 23850) { // Night
            target += configCache.getNightModifier();
        } else {
            target += configCache.getDayModifier();
        }

        // Armor - use cached config
        ConfigurationSection armorConfig = configCache.getArmor();
        if (armorConfig != null) {
            ItemStack helmet = player.getInventory().getHelmet();
            ItemStack chestplate = player.getInventory().getChestplate();
            ItemStack leggings = player.getInventory().getLeggings();
            ItemStack boots = player.getInventory().getBoots();
            
            if (helmet != null && armorConfig.contains(helmet.getType().name())) {
                target += armorConfig.getDouble(helmet.getType().name());
            }
            if (chestplate != null && armorConfig.contains(chestplate.getType().name())) {
                target += armorConfig.getDouble(chestplate.getType().name());
            }
            if (leggings != null && armorConfig.contains(leggings.getType().name())) {
                target += armorConfig.getDouble(leggings.getType().name());
            }
            if (boots != null && armorConfig.contains(boots.getType().name())) {
                target += armorConfig.getDouble(boots.getType().name());
            }
        }

        // Nearby Blocks - OPTIMIZED: Only scan blocks that are in our cache
        Set<String> cachedBlockTypes = configCache.getCachedBlockTypes();
        if (!cachedBlockTypes.isEmpty()) {
            // Find max radius needed
            int maxRadius = configCache.getDefaultBlockRadius();
            for (String blockType : cachedBlockTypes) {
                ConfigCache.BlockConfig blockConfig = configCache.getBlockConfig(blockType);
                if (blockConfig != null && blockConfig.radius > maxRadius) {
                    maxRadius = blockConfig.radius;
                }
            }
            
            // Limit max radius to 4 for performance (was 5, now 4)
            maxRadius = Math.min(maxRadius, 4);
            
            Block center = loc.getBlock();
            // Only scan blocks that are in our cache - much faster!
            for (int x = -maxRadius; x <= maxRadius; x++) {
                for (int yOffset = -maxRadius; yOffset <= maxRadius; yOffset++) {
                    for (int z = -maxRadius; z <= maxRadius; z++) {
                        Block b = center.getRelative(x, yOffset, z);
                        String blockType = b.getType().name();
                        
                        // Only check if this block type is in our cache
                        if (cachedBlockTypes.contains(blockType)) {
                            ConfigCache.BlockConfig blockConfig = configCache.getBlockConfig(blockType);
                            if (blockConfig != null) {
                                // Check distance
                                if (Math.abs(x) <= blockConfig.radius && 
                                    Math.abs(yOffset) <= blockConfig.radius &&
                                    Math.abs(z) <= blockConfig.radius) {
                                    target += blockConfig.temp;
                                }
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
        String statusKey = "status-normal";
        String status = plugin.getMessage(statusKey, "Normal");
        BarColor color = BarColor.GREEN;

        if (thresholds != null) {
            // Check extreme cold
            if (thresholds.contains("cold_extreme") && temp <= thresholds.getDouble("cold_extreme.trigger")) {
                color = BarColor.BLUE;
                statusKey = "status-freezing";
            }
            // Check severe cold
            else if (thresholds.contains("cold_severe") && temp <= thresholds.getDouble("cold_severe.trigger")) {
                color = BarColor.BLUE;
                statusKey = "status-very-cold";
            }
            // Check moderate cold
            else if (thresholds.contains("cold_moderate") && temp <= thresholds.getDouble("cold_moderate.trigger")) {
                color = BarColor.BLUE;
                statusKey = "status-cold";
            }
            // Check mild cold
            else if (thresholds.contains("cold_mild") && temp <= thresholds.getDouble("cold_mild.trigger")) {
                color = BarColor.WHITE;
                statusKey = "status-cool";
            }
            // Check extreme heat
            else if (thresholds.contains("heat_extreme") && temp >= thresholds.getDouble("heat_extreme.trigger")) {
                color = BarColor.RED;
                statusKey = "status-burning";
            }
            // Check severe heat
            else if (thresholds.contains("heat_severe") && temp >= thresholds.getDouble("heat_severe.trigger")) {
                color = BarColor.RED;
                statusKey = "status-very-hot";
            }
            // Check moderate heat
            else if (thresholds.contains("heat_moderate") && temp >= thresholds.getDouble("heat_moderate.trigger")) {
                color = BarColor.YELLOW;
                statusKey = "status-hot";
            }
            // Check mild heat
            else if (thresholds.contains("heat_mild") && temp >= thresholds.getDouble("heat_mild.trigger")) {
                color = BarColor.YELLOW;
                statusKey = "status-warm";
            }
        }

        status = plugin.getMessage(statusKey, status);
        bar.setColor(color);
        
        // Get bossbar title format and replace placeholders
        String titleFormat = plugin.getMessage("bossbar-title", "Temperature: {status} ({temp}°C)");
        String title = titleFormat.replace("{status}", status).replace("{temp}", String.format("%.1f", temp));
        bar.setTitle(title);
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
    
    public ConfigCache getConfigCache() {
        return configCache;
    }
}
