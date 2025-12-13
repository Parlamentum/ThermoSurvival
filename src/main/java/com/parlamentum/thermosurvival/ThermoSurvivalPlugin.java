package com.parlamentum.thermosurvival;

import org.bukkit.plugin.java.JavaPlugin;

public class ThermoSurvivalPlugin extends JavaPlugin {

    private TemperatureManager temperatureManager;
    private org.bukkit.scheduler.BukkitTask task;
    private WorldGuardHook worldGuardHook;

    @Override
    public void onEnable() {
        // Save default config
        saveDefaultConfig();

        // Initialize WorldGuard hook
        this.worldGuardHook = new WorldGuardHook(this);

        // Initialize manager
        this.temperatureManager = new TemperatureManager(this, worldGuardHook);

        // Register listeners
        getServer().getPluginManager().registerEvents(new PlayerListener(this, temperatureManager), this);

        // Register commands
        org.bukkit.command.PluginCommand thermoCommand = getCommand("thermo");
        if (thermoCommand != null) {
            thermoCommand.setExecutor(new ThermoCommandExecutor(this, temperatureManager));
        } else {
            getLogger().warning("Command 'thermo' not found in plugin.yml! Plugin may not work correctly.");
        }

        // Start task
        long interval = getConfig().getLong("update-interval", 20L);
        this.task = new TemperatureTask(this, temperatureManager).runTaskTimer(this, interval, interval);

        getLogger().info("ThermoSurvival has been enabled!");
    }

    @Override
    public void onDisable() {
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
        if (temperatureManager != null) {
            temperatureManager.cleanup();
        }
        getLogger().info("ThermoSurvival has been disabled!");
    }

    /**
     * Get a translated message from config
     * @param path The config path to the message
     * @param defaultValue The default value if message not found
     * @return The translated message with color codes
     */
    public String getMessage(String path, String defaultValue) {
        String message = getConfig().getString("messages." + path, defaultValue);
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', message);
    }
}
