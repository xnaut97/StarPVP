package com.github.tezvn.starpvp.core.integration;

import me.ulrich.clans.Clans;
import me.ulrich.clans.data.ClanData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class ClanIntegration {

    public static ClanData getClan(Player player) {
        if(!isHooked()) return null;
        return JavaPlugin.getPlugin(Clans.class).getPlayerAPI().getPlayerClan(player.getUniqueId()).orElse(null);
    }

    public static boolean inSameClan(Player player, Player other) {
        ClanData clan = getClan(player);
        if(clan == null) return false;
        return clan.getMembers().contains(other.getUniqueId());
    }

    private static boolean isHooked() {
        return Bukkit.getPluginManager().getPlugin("UltimateClans") != null;
    }

}
