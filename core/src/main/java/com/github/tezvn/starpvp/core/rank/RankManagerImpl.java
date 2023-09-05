package com.github.tezvn.starpvp.core.rank;

import com.github.tezvn.starpvp.api.SPPlugin;
import com.github.tezvn.starpvp.api.rank.RankManager;
import com.github.tezvn.starpvp.api.rank.RankPenalty;
import com.github.tezvn.starpvp.api.rank.SPRank;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;
import java.util.Map;

public class RankManagerImpl implements RankManager {

    private final SPPlugin plugin;

    private final Map<String, SPRank> ranks = Maps.newHashMap();

    public RankManagerImpl(SPPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    @Override
    public void reload() {
        this.ranks.clear();
        FileConfiguration config = plugin.getConfig();
        ConfigurationSection section = config.getConfigurationSection("ranks");
        if(section == null)
            return;
        section.getKeys(false).forEach(id -> {
            long elo = config.getLong("ranks." + id + ".elo");
            String name = config.getString("ranks." + id + ".name");
            boolean duplicate = ranks.values().stream().anyMatch(r -> r.getElo() == elo);
            if(duplicate) {
                getPlugin().getLogger().severe("Error while loading rank '" + name + "' due to duplicated elo point with other rank!");
                return;
            }
            PenaltyData penaltyData = null;
            ConfigurationSection penaltySection = config.getConfigurationSection("penalty");
            if(penaltySection != null) {
                int activeDays = config.getInt("penalty.active-days");
                int period = config.getInt("penalty.elo-lost.period");
                long eloLost = config.getLong("penalty.elo-lost.amount");
                penaltyData = new PenaltyData(activeDays, period, eloLost);
            }
            this.ranks.putIfAbsent(id, new SPRankImpl(this, id, elo, name, penaltyData));
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
        return this.ranks.values().stream().min((o1, o2) -> (int) (o1.getElo() - o2.getElo())).orElse(null);
    }

    @Override
    public SPRank getHighestRank() {
        return this.ranks.values().stream().max((o1, o2) -> (int) (o2.getElo() - o1.getElo())).orElse(null);
    }

}
