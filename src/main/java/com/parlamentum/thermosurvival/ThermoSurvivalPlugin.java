package com.parlamentum.thermosurvival;

import org.bukkit.plugin.java.JavaPlugin;

public class ThermoSurvivalPlugin extends JavaPlugin {

    private TemperatureManager temperatureManager;

    @Override
    public void onEnable() {
        // Save default config
        saveDefaultConfig();

        // Initialize manager
        this.temperatureManager = new TemperatureManager(this);

        // Register listeners
        getServer().getPluginManager().registerEvents(new PlayerListener(this, temperatureManager), this);

        // Start task
        long interval = getConfig().getLong("update-interval", 20L);
        new TemperatureTask(this, temperatureManager).runTaskTimer(this, interval, interval);

        getLogger().info("ThermoSurvival has been enabled!");
    }

    @Override
    public void onDisable() {
        if (temperatureManager != null) {
            temperatureManager.cleanup();
        }
        getLogger().info("ThermoSurvival has been disabled!");
    }
}
