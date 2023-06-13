package com.github.tezvn.starpvp.core.player;

import com.cryptomorin.xseries.XSound;
import com.github.tezvn.starpvp.api.SPPlugin;
import com.github.tezvn.starpvp.api.player.PlayerManager;
import com.github.tezvn.starpvp.api.player.PlayerStatistic;
import com.github.tezvn.starpvp.api.player.CombatCooldown;
import com.github.tezvn.starpvp.api.player.SPPlayer;
import com.github.tezvn.starpvp.core.utils.MessageUtils;
import com.github.tezvn.starpvp.core.utils.WGUtils;
import com.github.tezvn.starpvp.core.utils.time.TimeUnits;
import com.github.tezvn.starpvp.core.utils.time.TimeUtils;
import com.google.common.collect.Maps;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class PlayerManagerImpl implements PlayerManager, Listener {

    private final Map<UUID, SPPlayer> players = Maps.newHashMap();

    private final SPPlugin plugin;

    private final Map<UUID, Long> combatTimestamp = Maps.newHashMap();

    private final Map<UUID, CombatCooldown> combatCooldown = Maps.newHashMap();

    public PlayerManagerImpl(SPPlugin plugin) {
        this.plugin = plugin;
        new CooldownHandler(this);
    }

    @Override
    public List<SPPlayer> getPlayers() {
        return List.copyOf(this.players.values());
    }

    @Override
    public SPPlayer getPlayer(OfflinePlayer player) {
        return getPlayer(player.getUniqueId());
    }

    public Map<UUID, CombatCooldown> getCombatCooldown() {
        return combatCooldown;
    }

    public Map<UUID, Long> getCombatTimestamp() {
        return combatTimestamp;
    }

    @Override
    public SPPlayer getPlayer(String name) {
        return this.players.values().stream()
                .filter(p -> p.getPlayer().getName() != null && p.getPlayer().getName().equals(name))
                .findAny().orElse(null);
    }

    @Override
    public SPPlayer getPlayer(UUID uniqueId) {
        return this.players.getOrDefault(uniqueId, null);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null)
            return;
        SPPlayer spVictim = getPlayer(victim);
        SPPlayer spKiller = getPlayer(killer);

        EloProcessor eloProcessor = new EloProcessor().setWinner(spKiller).setLoser(spVictim);

        spVictim.setStarPoint(eloProcessor.getNewLoserSP());
        spKiller.setStarPoint(eloProcessor.getNewWinnerSP());

        spVictim.setStatistic(PlayerStatistic.DEATH_COUNT, spVictim.getStatistic(PlayerStatistic.DEATH_COUNT) + 1);
        spKiller.setStatistic(PlayerStatistic.KILL_COUNT, spVictim.getStatistic(PlayerStatistic.KILL_COUNT) + 1);

        applyReviveCooldown(victim);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (!this.combatCooldown.containsKey(player.getUniqueId()))
            return;
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (this.combatTimestamp.containsKey(event.getEntity().getUniqueId())
                && this.combatTimestamp.containsKey(event.getDamager().getUniqueId()))
            return;
        if (event.getEntity() instanceof Player victim && event.getDamager() instanceof Player damager) {
            ProtectedRegion region = WGUtils.getRegion(damager.getLocation());
            if (region == null)
                return;
            StateFlag.State state = region.getFlag(Flags.PVP);
            if (state == null || state == StateFlag.State.DENY)
                return;
            this.combatTimestamp.putIfAbsent(victim.getUniqueId(), System.currentTimeMillis());
            this.combatTimestamp.putIfAbsent(damager.getUniqueId(), System.currentTimeMillis());
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location location = this.combatTimestamp.containsKey(player.getUniqueId()) ? event.getFrom()
                : this.combatCooldown.containsKey(player.getUniqueId()) ? event.getTo() : null;
        if (location == null)
            return;
        ProtectedRegion region = WGUtils.getRegion(location);
        if (region == null)
            return;
        StateFlag.State state = region.getFlag(Flags.PVP);
        if (state == null || state == StateFlag.State.DENY)
            return;
        event.setCancelled(true);
        if (this.combatTimestamp.containsKey(player.getUniqueId()))
            MessageUtils.sendCooldownMessage(player, 10,
                    "&cBạn không được phép ra khỏi khu vực giao tranh trong khi đang giao tranh!");
        else if(this.combatCooldown.containsKey(player.getUniqueId())) {
            CombatCooldown cooldown = this.combatCooldown.get(player.getUniqueId());
            TimeUtils tu = TimeUtils.newInstance().add(cooldown.getDuration());
            MessageUtils.sendCooldownMessage(player, 10,
                    "&cBạn phải chờ &6" + tu.getShortDuration() + " &cmới được tham gia đấu trường tiếp");
        }
    }

    private void applyReviveCooldown(Player player) {
        long firstCombatTime = this.combatTimestamp.getOrDefault(player.getUniqueId(), -1L);
        if (firstCombatTime == -1)
            return;
        int minutes = (int) ((System.currentTimeMillis() - firstCombatTime) / 60000);
        int revivieMinutes = 0;
        if (minutes <= 6)
            revivieMinutes = minutes * 2 + 4;
        else if (minutes == 7)
            revivieMinutes = 21;
        else if (minutes > 8)
            revivieMinutes = minutes * 2 + 6;
        TimeUtils tu = TimeUtils.newInstance().add(TimeUnits.MINUTE, revivieMinutes);
        this.combatCooldown.putIfAbsent(player.getUniqueId(), new CombatCooldownImpl(getPlayer(player), tu.getNewTime()));
        this.combatTimestamp.remove(player.getUniqueId());
    }

    private static class CooldownHandler extends BukkitRunnable {

        private final Map<UUID, CombatCooldown> combatCooldown;

        public CooldownHandler(PlayerManagerImpl instance) {
            this.combatCooldown = instance.getCombatCooldown();
            runTaskTimerAsynchronously(instance.plugin, 20, 20);
        }

        @Override
        public void run() {
            Iterator<Map.Entry<UUID, CombatCooldown>> it = this.combatCooldown.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<UUID, CombatCooldown> entry = it.next();
                UUID uuid = entry.getKey();
                CombatCooldown cooldown = entry.getValue();
                if (cooldown.getEndTime() > TimeUtils.newInstance().getNewTime())
                    continue;
                it.remove();
                Player player = Bukkit.getPlayer(uuid);
                if (player == null)
                    continue;
                MessageUtils.sendMessage(player, "&aBạn có thể tham gia đấu trường trở lại.");
                XSound.ENTITY_EXPERIENCE_ORB_PICKUP.play(player);
            }
        }

    }

}
