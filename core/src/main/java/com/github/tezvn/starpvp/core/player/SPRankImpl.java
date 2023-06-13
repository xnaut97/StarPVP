package com.github.tezvn.starpvp.core.player;

import com.github.tezvn.starpvp.api.player.RankTier;
import com.github.tezvn.starpvp.api.player.SPPlayer;
import com.github.tezvn.starpvp.api.player.SPRank;
import com.github.tezvn.starpvp.core.SPPluginImpl;
import org.bukkit.plugin.java.JavaPlugin;

public class SPRankImpl implements SPRank {

    private final SPPlayer player;

    private RankTier tier;

    private int level;

    public SPRankImpl(SPPlayer player) {
        this(player, RankTier.GRASS, 1);
    }

    public SPRankImpl(SPPlayer player, RankTier tier, int level) {
        this.player = player;
        this.tier = tier;
        this.level = level;
    }

    public SPPlayer getPlayer() {
        return player;
    }

    @Override
    public RankTier getTier() {
        return this.tier;
    }

    @Override
    public int getLevel() {
        return this.level;
    }

    @Override
    public boolean promote() {
        if (getTier().isHighest() || getPlayer().getStarPoint() < getSPToPromote())
            return false;
        getPlayer().setStarPoint(getPlayer().getStarPoint() - getSPToPromote());
        this.level+=1;
        if(getLevel() > getMaxLevel()) {
            if(this.tier.ordinal() < 5)
                this.level = 1;
            this.tier = tier.getNext();
        }
        return promote();
    }

    @Override
    public boolean demote() {
        return demote(false);
    }

    @Override
    public boolean demote(boolean resetSP) {
        if(getTier().isLowest())
            return false;
        if(resetSP)
            getPlayer().setStarPoint(0);

        return false;
    }

    private long getMaxLevel() {
        return JavaPlugin.getPlugin(SPPluginImpl.class).getConfig().getLong("max-level." + tier.name().toLowerCase());
    }

    private long getSPToPromote() {
        return JavaPlugin.getPlugin(SPPluginImpl.class).getConfig().getLong("sp-upgrade." + tier.name().toLowerCase());
    }

}
