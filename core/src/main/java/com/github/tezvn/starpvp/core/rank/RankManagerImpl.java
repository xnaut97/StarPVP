package com.github.tezvn.starpvp.core.rank;

import com.github.tezvn.starpvp.api.SPPlugin;
import com.github.tezvn.starpvp.api.rank.RankManager;
import com.github.tezvn.starpvp.api.rank.SPRank;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class RankManagerImpl implements RankManager {

    private final SPPlugin plugin;

    private final Map<String, SPRank> ranks = Maps.newHashMap();

    public RankManagerImpl(SPPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    private void reload() {
        FileConfiguration config = plugin.getConfig();
        ConfigurationSection section = config.getConfigurationSection("ranks");
        if(section == null)
            return;
        section.getKeys(false).forEach(id -> {
            long sp = config.getLong("ranks." + id + ".sp");
            String name = config.getString("ranks." + id + ".name");
            this.ranks.putIfAbsent(id, new SPRankImpl(this, id, sp, name));
        });
    }

    public SPPlugin getPlugin() {
        return plugin;
    }

    @Override
    public List<SPRank> getRanks() {
        return Lists.newArrayList(this.ranks.values());
    }

    @Override
    public SPRank getRank(String id) {
        return this.ranks.getOrDefault(id, null);
    }

    @Override
    public SPRank getLowestRank() {
        return this.ranks.values().stream().min(Comparator.comparing(SPRank::getSP, Comparator.naturalOrder())).orElse(null);
    }

    @Override
    public SPRank getHighestRank() {
        return this.ranks.values().stream().max(Comparator.comparing(SPRank::getSP, Comparator.naturalOrder())).orElse(null);
    }

}
