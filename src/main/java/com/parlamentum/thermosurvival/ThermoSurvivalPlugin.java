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
        getCommand("thermo").setExecutor(new ThermoCommandExecutor(this, temperatureManager));

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
}
