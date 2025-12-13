package com.parlamentum.thermosurvival;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Cache for frequently accessed config values to improve performance
 */
public class ConfigCache {
    
    private final ThermoSurvivalPlugin plugin;
    private long lastCacheUpdate = 0;
    private static final long CACHE_UPDATE_INTERVAL = 100; // Update cache every 5 seconds (100 ticks)
    
    // Cached values
    private double baseTemp;
    private double minTemp;
    private double maxTemp;
    private List<String> disabledWorlds;
    private ConfigurationSection biomes;
    private ConfigurationSection blocks;
    private int defaultBlockRadius;
    private double dayModifier;
    private double nightModifier;
    private double rainModifier;
    private double snowModifier;
    private double stormModifier;
    private ConfigurationSection armor;
    private double caveColdModifier;
    private Map<String, Double> biomeTempCache = new HashMap<>();
    private Map<String, BlockConfig> blockConfigCache = new HashMap<>();
    
    public static class BlockConfig {
        public final double temp;
        public final int radius;
        
        public BlockConfig(double temp, int radius) {
            this.temp = temp;
            this.radius = radius;
        }
    }
    
    public ConfigCache(ThermoSurvivalPlugin plugin) {
        this.plugin = plugin;
        updateCache();
    }
    
    public void updateCache() {
        FileConfiguration config = plugin.getConfig();
        baseTemp = config.getDouble("base-temp", 20.0);
        minTemp = config.getDouble("min-temp", -30);
        maxTemp = config.getDouble("max-temp", 50);
        disabledWorlds = config.getStringList("disabled-worlds");
        
        biomes = config.getConfigurationSection("biomes");
        blocks = config.getConfigurationSection("blocks");
        defaultBlockRadius = config.getInt("default-block-scan-radius", 2);
        
        dayModifier = config.getDouble("time.day-modifier", 2);
        nightModifier = config.getDouble("time.night-modifier", -5);
        rainModifier = config.getDouble("weather.raining", -3);
        snowModifier = config.getDouble("weather.snowing", -8);
        stormModifier = config.getDouble("weather.storming", -5);
        
        armor = config.getConfigurationSection("armor");
        caveColdModifier = config.getDouble("caves.cold-modifier", -0.5);
        
        // Cache biome temperatures
        biomeTempCache.clear();
        if (biomes != null) {
            Set<String> biomeKeys = biomes.getKeys(false);
            for (String key : biomeKeys) {
                biomeTempCache.put(key, biomes.getDouble(key));
            }
        }
        
        // Cache block configurations
        blockConfigCache.clear();
        if (blocks != null) {
            Set<String> blockKeys = blocks.getKeys(false);
            for (String key : blockKeys) {
                if (blocks.isConfigurationSection(key)) {
                    double temp = blocks.getDouble(key + ".temp", 0);
                    int radius = blocks.getInt(key + ".radius", defaultBlockRadius);
                    blockConfigCache.put(key, new BlockConfig(temp, radius));
                } else {
                    double temp = blocks.getDouble(key, 0);
                    blockConfigCache.put(key, new BlockConfig(temp, defaultBlockRadius));
                }
            }
        }
        
        lastCacheUpdate = System.currentTimeMillis();
    }
    
    public void checkAndUpdateCache() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCacheUpdate > CACHE_UPDATE_INTERVAL * 50) { // Convert ticks to ms
            updateCache();
        }
    }
    
    // Getters
    public double getBaseTemp() { return baseTemp; }
    public double getMinTemp() { return minTemp; }
    public double getMaxTemp() { return maxTemp; }
    public List<String> getDisabledWorlds() { return disabledWorlds; }
    public ConfigurationSection getBiomes() { return biomes; }
    public ConfigurationSection getBlocks() { return blocks; }
    public int getDefaultBlockRadius() { return defaultBlockRadius; }
    public double getDayModifier() { return dayModifier; }
    public double getNightModifier() { return nightModifier; }
    public double getRainModifier() { return rainModifier; }
    public double getSnowModifier() { return snowModifier; }
    public double getStormModifier() { return stormModifier; }
    public ConfigurationSection getArmor() { return armor; }
    public double getCaveColdModifier() { return caveColdModifier; }
    
    public Double getBiomeTemp(String biomeName) {
        return biomeTempCache.get(biomeName);
    }
    
    public BlockConfig getBlockConfig(String blockType) {
        return blockConfigCache.get(blockType);
    }
    
    public Set<String> getCachedBlockTypes() {
        return blockConfigCache.keySet();
    }
}

