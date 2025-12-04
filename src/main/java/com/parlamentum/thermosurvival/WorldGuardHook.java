package com.parlamentum.thermosurvival;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;

public class WorldGuardHook {

    private final ThermoSurvivalPlugin plugin;
    private boolean enabled;

    public WorldGuardHook(ThermoSurvivalPlugin plugin) {
        this.plugin = plugin;
        this.enabled = false;
        try {
            Class.forName("com.sk89q.worldguard.WorldGuard");
            this.enabled = plugin.getConfig().getBoolean("worldguard.enabled", true);
        } catch (ClassNotFoundException e) {
            this.enabled = false;
            plugin.getLogger().info("WorldGuard not found, integration disabled.");
        }
    }

    public boolean isInDisabledRegion(Player player) {
        if (!enabled)
            return false;

        List<String> disabledRegions = plugin.getConfig().getStringList("worldguard.disabled-regions");
        if (disabledRegions.isEmpty())
            return false;

        Location loc = player.getLocation();
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionQuery query = container.createQuery();
        ApplicableRegionSet set = query.getApplicableRegions(BukkitAdapter.adapt(loc));

        for (ProtectedRegion region : set) {
            if (disabledRegions.contains(region.getId())) {
                return true;
            }
        }

        return false;
    }
}
