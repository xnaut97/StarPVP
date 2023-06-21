package com.github.tezvn.starpvp.core.utils;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.List;
import java.util.Objects;

public class WGUtils {

    public static ProtectedRegion getRegion(Location location) {
        if(!hasWorldGuard())
            return null;
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionQuery query = container.createQuery();
        ApplicableRegionSet set = query.getApplicableRegions(BukkitAdapter.adapt(location));
        return set.getRegions().stream()
                .filter(region -> region.contains(location.getBlockX(), location.getBlockY(), location.getBlockZ()))
                .findAny().orElse(null);
    }

    public static ProtectedRegion getRegion(String id, World world) {
        if(!hasWorldGuard())
            return null;
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager manager = container.get(BukkitAdapter.adapt(world));
        return manager == null ? null : manager.getRegion(id);
    }

    public static Location getSpawnLocation(String id, World world) {
        return getSpawnLocation(id, world, true);
    }

    public static Location getSpawnLocation(String id, World world, boolean center) {
        ProtectedRegion region = getRegion(id, world);
        if(region == null)
            return null;
        com.sk89q.worldedit.util.Location wgLocation = region.getFlag(Flags.SPAWN_LOC);
        double offset = center ? .5 : 0;
        return wgLocation == null ? null : new Location(world, wgLocation.getBlockX() + offset,
                wgLocation.getBlockY() + offset, wgLocation.getBlockZ() + offset);
    }

    public static boolean isInPVPRegion(Location location) {
        if(!hasWorldGuard())
            return false;
        ProtectedRegion region = getRegion(location);
        if(region == null)
            return false;
        StateFlag.State state = region.getFlag(Flags.PVP);
        return state == StateFlag.State.ALLOW;
    }

    public static boolean hasWorldGuard() {
        return Bukkit.getPluginManager().getPlugin("WorldGuard") != null;
    }
}
