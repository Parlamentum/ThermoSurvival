package com.parlamentum.thermosurvival;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TemperatureTask extends BukkitRunnable {

    private final ThermoSurvivalPlugin plugin;
    private final TemperatureManager manager;
    private int tickCounter = 0;
    private final Map<UUID, String> lastEventMessage = new HashMap<>(); // Track last event message per player
    private final Map<UUID, String> lastBiome = new HashMap<>(); // Track last biome per player
    private final Map<UUID, Long> lastDayTime = new HashMap<>(); // Track last day time per player

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

            // Use cached disabled worlds list
            if (manager.getConfigCache().getDisabledWorlds().contains(player.getWorld().getName())) {
                continue;
            }
            
            // Check if chunk is loaded (performance optimization)
            if (!player.getLocation().getChunk().isLoaded()) {
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

            // Check for biome changes, sunrise, and other environmental changes
            String environmentalMessage = checkEnvironmentalChanges(player, targetTemp, currentTemp);
            
            String eventMessage = applyEffects(player, newTemp, targetTemp);
            
            // Prioritize environmental messages over event messages
            String messageToSend = environmentalMessage != null ? environmentalMessage : eventMessage;
            sendEventNotification(player, messageToSend);
        }
        tickCounter++;
    }

    private String applyEffects(Player player, double temp, double targetTemp) {
        ConfigurationSection thresholds = plugin.getConfig().getConfigurationSection("thresholds");
        if (thresholds == null)
            return null;

        // Find the most severe applicable threshold
        String[] coldLevels = { "cold_extreme", "cold_severe", "cold_moderate", "cold_mild" };
        String[] heatLevels = { "heat_extreme", "heat_severe", "heat_moderate", "heat_mild" };

        boolean effectApplied = false;
        boolean worldBorderEffect = false;
        String eventMessage = null;
        String eventLevel = null;

        // Check cold thresholds (from most severe to least)
        for (String level : coldLevels) {
            if (thresholds.contains(level)) {
                double trigger = thresholds.getDouble(level + ".trigger");
                if (temp <= trigger) {
                    List<String> effects = thresholds.getStringList(level + ".effects");
                    applyPotionEffects(player, effects);

                    // Apply visual effects for cold
                    applyColdVisualEffects(player, level, temp);

                    // Apply damage if configured
                    if (thresholds.contains(level + ".damage-interval")) {
                        int dmgInterval = thresholds.getInt(level + ".damage-interval", 200);
                        if (tickCounter % (dmgInterval / plugin.getConfig().getLong("update-interval", 20)) == 0) {
                            double damageAmount = thresholds.getDouble(level + ".damage-amount", 1.0);
                            Damageable damageable = (Damageable) player;
                            if (damageable.getHealth() > damageAmount) {
                                damageable.damage(damageAmount);
                            } else {
                                damageable.setHealth(0.0);
                            }
                        }
                    }

                    // Check for world border effect
                    if (thresholds.getBoolean(level + ".world-border-effect", false)) {
                        worldBorderEffect = true;
                    }

                    eventLevel = level;
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

                        // Apply visual effects for heat
                        applyHeatVisualEffects(player, level, temp);

                        // Apply damage if configured
                        if (thresholds.contains(level + ".damage-interval")) {
                            int dmgInterval = thresholds.getInt(level + ".damage-interval", 200);
                            if (tickCounter % (dmgInterval / plugin.getConfig().getLong("update-interval", 20)) == 0) {
                                double damageAmount = thresholds.getDouble(level + ".damage-amount", 1.0);
                                Damageable damageable = (Damageable) player;
                                if (damageable.getHealth() > damageAmount) {
                                    damageable.damage(damageAmount);
                                } else {
                                    damageable.setHealth(0.0);
                                }
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

                        eventLevel = level;
                        break; // Only apply the most severe level
                    }
                }
            }
        }

        // Apply world border effect if needed
        if (worldBorderEffect) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 60, 0, true, false, false));
        }

        // Check for acid rain (when raining and in certain biomes)
        World world = player.getWorld();
        if (world.hasStorm() && !world.isThundering()) {
            String biomeName = world.getBiome(player.getLocation()).name();
            // Acid rain in hot biomes or polluted areas
            if (biomeName.contains("DESERT") || biomeName.contains("BADLANDS") || 
                biomeName.contains("SWAMP") || biomeName.contains("MANGROVE") ||
                biomeName.contains("NETHER")) {
                applyAcidRainEffects(player);
                if (eventMessage == null) {
                    eventMessage = plugin.getMessage("acid-rain", "&2⚠ Acid Rain!");
                }
            }
        }

        // Generate event message if threshold is active
        if (eventLevel != null) {
            if (thresholds.contains(eventLevel + ".message")) {
                eventMessage = ChatColor.translateAlternateColorCodes('&', 
                    thresholds.getString(eventLevel + ".message", ""));
            } else {
                // Default messages from config
                if (eventLevel.startsWith("cold_")) {
                    String messageKey = "cold-" + eventLevel.replace("cold_", "");
                    eventMessage = plugin.getMessage(messageKey, getDefaultColdMessage(eventLevel));
                } else if (eventLevel.startsWith("heat_")) {
                    String messageKey = "heat-" + eventLevel.replace("heat_", "");
                    eventMessage = plugin.getMessage(messageKey, getDefaultHeatMessage(eventLevel));
                }
            }
        }

        return eventMessage;
    }

    private void applyColdVisualEffects(Player player, String level, double temp) {
        // Reduce particle frequency for performance - only show every 3 ticks
        if (tickCounter % 3 != 0) {
            return;
        }
        
        Location loc = player.getLocation();
        World world = player.getWorld();
        
        // Snow particles for cold
        if (level.equals("cold_extreme") || level.equals("cold_severe")) {
            // Heavy snow effect - reduced from 10 to 5 particles
            for (int i = 0; i < 5; i++) {
                double x = loc.getX() + (Math.random() - 0.5) * 2;
                double y = loc.getY() + Math.random() * 2;
                double z = loc.getZ() + (Math.random() - 0.5) * 2;
                world.spawnParticle(Particle.SNOWFLAKE, x, y, z, 1, 0, 0, 0, 0);
            }
            // Ice particles - reduced from 5 to 3
            world.spawnParticle(Particle.SNOWBALL, loc, 3, 0.5, 1, 0.5, 0);
        } else {
            // Light snow for moderate cold - reduced from 3 to 2
            world.spawnParticle(Particle.SNOWFLAKE, loc, 2, 0.5, 1, 0.5, 0);
        }
        
        // Frost breath effect (white smoke) - only every 10 ticks
        if (tickCounter % 10 == 0) {
            world.spawnParticle(Particle.CLOUD, loc.clone().add(0, 1.5, 0), 1, 0.2, 0.2, 0.2, 0.01);
        }
    }

    private void applyHeatVisualEffects(Player player, String level, double temp) {
        // Reduce particle frequency for performance - only show every 3 ticks
        if (tickCounter % 3 != 0) {
            return;
        }
        
        Location loc = player.getLocation();
        World world = player.getWorld();
        
        // Smoke particles for extreme heat
        if (level.equals("heat_extreme") || level.equals("heat_severe")) {
            // Heavy smoke effect - reduced from 8 to 4 particles
            for (int i = 0; i < 4; i++) {
                double x = loc.getX() + (Math.random() - 0.5) * 1.5;
                double y = loc.getY() + Math.random() * 0.5;
                double z = loc.getZ() + (Math.random() - 0.5) * 1.5;
                world.spawnParticle(Particle.SMOKE_LARGE, x, y, z, 1, 0, 0.1, 0, 0.02);
            }
            // Heat distortion (campfire smoke) - reduced from 3 to 2
            world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, loc.clone().add(0, 0.5, 0), 2, 0.3, 0.1, 0.3, 0.01);
        } else {
            // Light smoke for moderate heat - reduced from 2 to 1
            world.spawnParticle(Particle.SMOKE_NORMAL, loc.clone().add(0, 0.5, 0), 1, 0.2, 0.1, 0.2, 0.01);
        }
        
        // Heat shimmer effect (illusion particles) - only every 10 ticks
        if (tickCounter % 10 == 0 && (level.equals("heat_extreme") || level.equals("heat_severe"))) {
            world.spawnParticle(Particle.END_ROD, loc.clone().add(0, 1, 0), 1, 0.3, 0.3, 0.3, 0);
        }
    }

    private void applyAcidRainEffects(Player player) {
        Location loc = player.getLocation();
        World world = player.getWorld();
        
        // Acid rain particles (green/poison effect) - reduced frequency
        if (tickCounter % 10 == 0) {
            world.spawnParticle(Particle.DRIP_WATER, loc.clone().add(0, 2, 0), 3, 0.5, 0, 0.5, 0);
            world.spawnParticle(Particle.VILLAGER_ANGRY, loc.clone().add(0, 2, 0), 1, 0.3, 0, 0.3, 0);
        }
        
        // Apply poison effect occasionally
        if (tickCounter % 100 == 0) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 60, 0, true, false, true));
        }
        
        // Damage from acid rain
        if (tickCounter % 200 == 0) {
            Damageable damageable = (Damageable) player;
            if (damageable.getHealth() > 0.5) {
                damageable.damage(0.5);
            }
        }
    }

    private void applyPotionEffects(Player player, List<String> effects) {
        for (String effectStr : effects) {
            String[] parts = effectStr.split(":");
            PotionEffectType type = null;
            // Try to get by key first
            try {
                org.bukkit.NamespacedKey key = org.bukkit.NamespacedKey.minecraft(parts[0].toLowerCase());
                type = PotionEffectType.getByKey(key);
            } catch (Exception ignored) {
            }
            // Fallback to deprecated getByName if getByKey fails
            if (type == null) {
                type = PotionEffectType.getByName(parts[0]);
            }
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

    private String getDefaultColdMessage(String level) {
        if (level.equals("cold_extreme")) {
            return "&1❄❄❄❄ EXTREME COLD!";
        } else if (level.equals("cold_severe")) {
            return "&9❄❄❄ You're freezing!";
        } else if (level.equals("cold_moderate")) {
            return "&b❄❄ The cold is biting!";
        } else {
            return "&b❄ You feel a chill...";
        }
    }

    private String getDefaultHeatMessage(String level) {
        if (level.equals("heat_extreme")) {
            return "&4🔥🔥🔥🔥 EXTREME HEAT!";
        } else if (level.equals("heat_severe")) {
            return "&c🔥🔥🔥 You're overheating!";
        } else if (level.equals("heat_moderate")) {
            return "&6🔥🔥 The heat is intense!";
        } else {
            return "&e🔥 You're getting warm...";
        }
    }

    private String checkEnvironmentalChanges(Player player, double targetTemp, double currentTemp) {
        UUID playerId = player.getUniqueId();
        World world = player.getWorld();
        Location loc = player.getLocation();
        
        String message = null;
        
        // Check biome change
        String currentBiome = world.getBiome(loc).name();
        String lastBiome = this.lastBiome.get(playerId);
        
        if (lastBiome != null && !lastBiome.equals(currentBiome)) {
            // Calculate temperature difference between biomes
            ConfigurationSection biomes = plugin.getConfig().getConfigurationSection("biomes");
            double lastBiomeTemp = plugin.getConfig().getDouble("base-temp", 20.0);
            double currentBiomeTemp = plugin.getConfig().getDouble("base-temp", 20.0);
            
            if (biomes != null) {
                if (biomes.contains(lastBiome)) {
                    lastBiomeTemp = biomes.getDouble(lastBiome);
                }
                if (biomes.contains(currentBiome)) {
                    currentBiomeTemp = biomes.getDouble(currentBiome);
                }
            }
            
            double tempDiff = currentBiomeTemp - lastBiomeTemp;
            
            if (Math.abs(tempDiff) > 5) { // Only show message if significant change
                if (tempDiff > 0) {
                    message = plugin.getMessage("biome-warmer", "&e☀ You shifted to a warmer area");
                } else {
                    message = plugin.getMessage("biome-colder", "&b❄ You shifted to a colder area");
                }
            }
        }
        
        this.lastBiome.put(playerId, currentBiome);
        
        // Check for sunrise (time transition from night to day)
        long currentTime = world.getTime();
        Long lastTime = this.lastDayTime.get(playerId);
        
        if (lastTime != null) {
            // Check if we transitioned from night (12000-24000) to day (0-12000)
            boolean wasNight = lastTime > 12300 && lastTime < 23850;
            boolean isDay = currentTime >= 0 && currentTime < 12300;
            
            if (wasNight && isDay && currentTime < 1000) { // Within first 50 seconds of day
                message = plugin.getMessage("sunrise", "&6☀ Sun warms you now");
            }
        }
        
        this.lastDayTime.put(playerId, currentTime);
        
        return message;
    }

    private void sendEventNotification(Player player, String eventMessage) {
        if (eventMessage == null || eventMessage.isEmpty()) {
            return;
        }

        UUID playerId = player.getUniqueId();
        String lastMessage = lastEventMessage.get(playerId);
        
        // Only send if message changed (avoid spam)
        if (eventMessage.equals(lastMessage)) {
            return;
        }
        
        lastEventMessage.put(playerId, eventMessage);
        
        ConfigurationSection uiConfig = plugin.getConfig().getConfigurationSection("ui");
        if (uiConfig == null) {
            return;
        }

        // Send to chat if enabled
        if (uiConfig.getBoolean("events.chat", true)) {
            player.sendMessage(eventMessage);
        }

        // Send to action bar if enabled
        if (uiConfig.getBoolean("events.actionbar", false)) {
            BaseComponent[] components = TextComponent.fromLegacyText(eventMessage);
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, components);
        }
    }
}
