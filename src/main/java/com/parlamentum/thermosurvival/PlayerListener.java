package com.parlamentum.thermosurvival;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

public class PlayerListener implements Listener {

    private final TemperatureManager manager;
    private final ThermoSurvivalPlugin plugin;

    public PlayerListener(ThermoSurvivalPlugin plugin, TemperatureManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        manager.removePlayer(event.getPlayer());
    }

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();
        ConfigurationSection consumables = plugin.getConfig().getConfigurationSection("consumables");

        if (consumables != null && consumables.contains(item.getType().name())) {
            double change = consumables.getDouble(item.getType().name());
            manager.modifyTemperature(event.getPlayer(), change);
        }
    }
}
