package com.github.tezvn.starpvp.core.log;

import com.github.tezvn.starpvp.api.player.SPPlayer;
import com.github.tezvn.starpvp.core.player.EloProcessor;
import org.bukkit.plugin.Plugin;

public class PlayerLog extends BaseLog {

    public PlayerLog(Plugin plugin) {
        super(plugin, LogType.PLAYER);
    }

    public void write(EloProcessor eloProcessor) {
        SPPlayer winner = eloProcessor.getWinner();
        SPPlayer loser = eloProcessor.getLoser();

        boolean isKillCooldown = winner.getKillCooldown(loser.asOfflinePlayer()) > 0;
        long winnerGain = isKillCooldown ? 0 : eloProcessor.getWinnerEPGain();
        long loserLost = isKillCooldown ? 0 : eloProcessor.getLoserEPLost();
        long oldWinnerElo = winner.getEloPoint() - winnerGain;
        long oldLoserElo = loser.getEloPoint() + loserLost;
        String winnerLog = " (" + oldWinnerElo + " -> " + winner.getEloPoint() + ")" + " (+" + winnerGain + ")";
        String loserLog = " (" + oldLoserElo + " -> " + loser.getEloPoint() + ")" + " (-" + loserLost + ")";
        write(eloProcessor.getWinner().getPlayerName() + winnerLog + ", "
                + eloProcessor.getLoser().getPlayerName() + loserLog);
    }


}
