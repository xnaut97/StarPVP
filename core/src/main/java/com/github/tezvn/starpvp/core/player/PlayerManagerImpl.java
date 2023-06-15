package com.github.tezvn.starpvp.core.player;

import com.cryptomorin.xseries.XSound;
import com.github.tezvn.starpvp.api.AbstractDatabase.DatabaseInsertion;
import com.github.tezvn.starpvp.api.AbstractDatabase.MySQL;
import com.github.tezvn.starpvp.api.SPPlugin;
import com.github.tezvn.starpvp.api.player.PlayerManager;
import com.github.tezvn.starpvp.api.player.PlayerStatistic;
import com.github.tezvn.starpvp.api.player.CombatCooldown;
import com.github.tezvn.starpvp.api.player.SPPlayer;
import com.github.tezvn.starpvp.api.rank.SPRank;
import com.github.tezvn.starpvp.core.handler.AbstractHandler;
import com.github.tezvn.starpvp.core.handler.CombatHandler;
import com.github.tezvn.starpvp.core.handler.CooldownHandler;
import com.github.tezvn.starpvp.core.handler.PenaltyHandler;
import com.github.tezvn.starpvp.core.utils.GsonHelper;
import com.github.tezvn.starpvp.core.utils.MessageUtils;
import com.github.tezvn.starpvp.core.utils.WGUtils;
import com.github.tezvn.starpvp.core.utils.time.TimeUnits;
import com.github.tezvn.starpvp.core.utils.time.TimeUtils;
import com.google.common.collect.Maps;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static com.github.tezvn.starpvp.core.handler.AbstractHandler.*;

public class PlayerManagerImpl implements PlayerManager, Listener {

    private final Map<UUID, SPPlayer> players = Maps.newHashMap();

    private final SPPlugin plugin;

    private final Map<UUID, Long> combatTimestamp = Maps.newHashMap();

    private final Map<UUID, CombatCooldown> combatCooldown = Maps.newHashMap();

    private final Map<UUID, Long> combatPenalty = Maps.newHashMap();

    private Location lobby;

    private final Map<Type, AbstractHandler<?>> handler = Maps.newHashMap();

    public PlayerManagerImpl(SPPlugin plugin) {
        this.plugin = plugin;
        registerHandler();
        registerOnline();
    }

    private void registerOnline() {
        Bukkit.getOnlinePlayers().forEach(player -> {
            if (getPlayer(player) == null)
                this.players.putIfAbsent(player.getUniqueId(), new SPPlayerImpl(player));
        });
    }

    private void registerHandler() {
        Arrays.stream(Type.values()).forEach(type -> {
            switch (type) {
                case COOLDOWN -> this.handler.putIfAbsent(type, new CooldownHandler(this));
                case PENALTY -> this.handler.putIfAbsent(type, new PenaltyHandler(this));
                case COMBAT -> this.handler.putIfAbsent(type, new CombatHandler(this));
            }
        });
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

    public Map<UUID, Long> getCombatPenalty() {
        return combatPenalty;
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

    @Override
    public void saveToDatabase(UUID uuid) {
        SPPlayer spPlayer = getPlayer(uuid);
        if (spPlayer == null)
            return;
        Map<String, Object> map = spPlayer.serialize();
        CombatCooldown cooldown = this.combatCooldown.getOrDefault(uuid, null);
        if (cooldown != null) {
            map.put("cooldown.start-time", String.valueOf(cooldown.getStartTime()));
            map.put("cooldown.end-time", String.valueOf(cooldown.getStartTime()));
            map.put("cooldown.until", TimeUtils.format(cooldown.getEndTime()));
        }
        long timestamp = this.combatTimestamp.getOrDefault(uuid, -1L);
        if (timestamp != -1)
            map.put("combat-since", String.valueOf(timestamp));
        long penaltyTime = this.combatPenalty.getOrDefault(uuid, -1L);
        if (penaltyTime != -1)
            map.put("penalty.until", String.valueOf(penaltyTime));

        MySQL database = getPlugin().getDatabase();
        if (database != null && database.isConnected()) {
            String tableName = getPlugin().getDocument().getString("database.table-name", "user");
            if (!database.hasTable(tableName))
                return;
            database.addOrUpdate(tableName,
                    new DatabaseInsertion("uuid", uuid.toString()),

                    new DatabaseInsertion("uuid", uuid.toString()),
                    new DatabaseInsertion("player_name", spPlayer.getPlayerName()),
                    new DatabaseInsertion("data", GsonHelper.encode(map)));
        }else
            saveToLocal(map);
    }

    private void saveToLocal(Map<String, Object> map) {
        try {
            File folder = new File(plugin.getDataFolder() + "/users");
            if(!folder.exists())
                folder.mkdirs();
            File file = new File(plugin.getDataFolder() + "/users/" + map.get("uuid") + ".yml");
            if(!file.exists())
                file.createNewFile();
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            map.forEach(config::set);
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void saveToDatabase(OfflinePlayer player) {
        saveToDatabase(player.getUniqueId());
    }

    @Override
    public SPPlayer loadFromDatabase(OfflinePlayer player) {
        return loadFromDatabase(player.getUniqueId());
    }

    @SuppressWarnings("unchecked")
    @Override
    public SPPlayer loadFromDatabase(UUID uuid) {
        SPPlayer spPlayer = null;
        MySQL database = getPlugin().getDatabase();
        if (database != null && database.isConnected()) {
            String tableName = getPlugin().getDocument().getString("database.table-name", "user");
            if (!database.hasTable(tableName))
                return null;
            ResultSet set = database.find(tableName, "uuid", uuid.toString());
            try {
                while (set.next()) {
                    spPlayer = new SPPlayerImpl(Bukkit.getOfflinePlayer(uuid));
                    Map<String, String> map = (Map<String, String>) GsonHelper.decode(set.getString("data"));
                    spPlayer.setRank(SPRank.valueOf(map.get("rank")));
                    spPlayer.addStarPoint(Long.parseLong(map.get("sp.current")));
                    long penaltyTimes = Long.parseLong(map.get("penalty.times"));
                    boolean penaltyActivate = Boolean.parseBoolean(map.get("penalty.activate"));
                    boolean hasCooldown = map.keySet().stream().anyMatch(key -> key.startsWith("cooldown."));
                    if (hasCooldown) {
                        long startTime = Long.parseLong(map.get("cooldown.start-time"));
                        long endTime = Long.parseLong(map.get("cooldown.end-time"));
                        this.combatCooldown.put(uuid, new CombatCooldownImpl(spPlayer, startTime, endTime));
                    }
                    long combatTimestamp = Long.parseLong(map.getOrDefault("combat-since", "-1"));
                    if (combatTimestamp != -1)
                        this.combatTimestamp.put(uuid, combatTimestamp);

                    long penaltyTime = Long.parseLong(map.getOrDefault("penalty.until", "-1"));
                    if (penaltyTime != -1)
                        this.combatPenalty.put(uuid, penaltyTime);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return spPlayer;
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

        spVictim.addStarPoint(eloProcessor.getNewLoserSP());
        spKiller.addStarPoint(eloProcessor.getNewWinnerSP());

        spVictim.setStatistic(PlayerStatistic.DEATH_COUNT, spVictim.getStatistic(PlayerStatistic.DEATH_COUNT) + 1);
        spKiller.setStatistic(PlayerStatistic.KILL_COUNT, spVictim.getStatistic(PlayerStatistic.KILL_COUNT) + 1);

        applyReviveCooldown(victim);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (event.getRespawnReason() != PlayerRespawnEvent.RespawnReason.DEATH)
            return;
        if (!this.combatCooldown.containsKey(player.getUniqueId()))
            return;
        event.setRespawnLocation(lobby);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (this.combatTimestamp.containsKey(event.getEntity().getUniqueId())
                && this.combatTimestamp.containsKey(event.getDamager().getUniqueId()))
            return;
        if (event.getEntity() instanceof Player victim && event.getDamager() instanceof Player damager) {
            if (!WGUtils.isInPVPRegion(damager.getLocation()))
                return;
            this.combatTimestamp.putIfAbsent(victim.getUniqueId(), System.currentTimeMillis());
            this.combatTimestamp.putIfAbsent(damager.getUniqueId(), System.currentTimeMillis());

            MessageUtils.sendTitle(victim, "&a&lBẬT GIAO TRANH",
                    "&7Bạn nhận sát thương từ người chơi &6" + damager.getName());
            MessageUtils.sendTitle(victim, "&a&lBẬT GIAO TRANH",
                    "&7Bạn gây sát thương lên người chơi &6" + victim.getName());
            XSound.BLOCK_ENCHANTMENT_TABLE_USE.play(victim, 1f, -1f);
            XSound.BLOCK_ENCHANTMENT_TABLE_USE.play(damager, 1f, -1f);

            applyRestriction(damager);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null)
            return;
        if (this.combatTimestamp.containsKey(player.getUniqueId())) {
            if (WGUtils.isInPVPRegion(from) && !WGUtils.isInPVPRegion(to)) {
                event.setCancelled(true);
                MessageUtils.sendCooldownMessage(player, 10,
                        "&cBạn không được phép ra khỏi khu vực giao tranh trong khi đang giao tranh!");
            }
        } else if (this.combatCooldown.containsKey(player.getUniqueId())
                || this.combatPenalty.containsKey(player.getUniqueId())) {
            if (!WGUtils.isInPVPRegion(from) && WGUtils.isInPVPRegion(to)) {
                event.setCancelled(true);
                boolean isCooldown = this.combatCooldown.containsKey(player.getUniqueId());
                long endTime = isCooldown
                        ? this.combatCooldown.get(player.getUniqueId()).getEndTime()
                        : this.combatPenalty.get(player.getUniqueId());
                TimeUtils tu = TimeUtils.newInstance().add(endTime);
                String duration = tu.getShortDuration();
                MessageUtils.sendCooldownMessage(player, 10, isCooldown
                        ? "&cBạn phải chờ &6" + tu.getShortDuration() + " &cmới được tham gia đấu trường tiếp"
                        : "&cBạn đã bị cấm tham gia giao tranh, hiệu lực còn &6" + duration);
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        SPPlayer spPlayer = getPlayer(player);
        if (spPlayer == null)
            this.players.putIfAbsent(player.getUniqueId(), new SPPlayerImpl(player));
        if (this.combatPenalty.containsKey(player.getUniqueId())) {
            MessageUtils.sendTitle(player,
                    "&c&lBẠN BỊ PHẠT!",
                    "&7Vì đã thoát trong lúc giao tranh.");
            XSound.BLOCK_ENCHANTMENT_TABLE_USE.play(player);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        SPPlayer spPlayer = getPlayer(player);
        if (spPlayer == null)
            return;
        if (this.combatTimestamp.containsKey(player.getUniqueId())) {
            ((SPPlayerImpl) spPlayer).enterCombatLogout();
            this.combatTimestamp.remove(player.getUniqueId());

            long combatLogoutTimes = spPlayer.getPenaltyTimes();
            TimeUtils tu = TimeUtils.newInstance();
            if (combatLogoutTimes == 1)
                tu.add(TimeUnits.HOUR, 1);
            else if (combatLogoutTimes == 2)
                tu.add(TimeUnits.HOUR, 12);
            else if (combatLogoutTimes >= 3)
                tu.add(TimeUnits.DAY, 1);
            this.combatPenalty.put(player.getUniqueId(), tu.getNewTime());
        }
        saveToDatabase(player);
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (this.combatTimestamp.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
            MessageUtils.sendTitle(player, "&c&l✖", "&7Không thể xài lệnh khi đang giao tranh");
        }
    }

    @EventHandler
    public void onToggleFlying(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        if (this.combatTimestamp.containsKey(player.getUniqueId()))
            event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerGamemode(PlayerGameModeChangeEvent event) {
        Player player = event.getPlayer();
        if (this.combatTimestamp.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
            if (player.getGameMode() != GameMode.ADVENTURE)
                player.setGameMode(GameMode.ADVENTURE);
        }
    }

    @EventHandler
    public void onPlayerPotion(EntityPotionEffectEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (!this.combatTimestamp.containsKey(player.getUniqueId()))
                return;
            if (event.getModifiedType() != PotionEffectType.INVISIBILITY)
                return;
            if (event.getAction() == EntityPotionEffectEvent.Action.ADDED
                    || event.getAction() == EntityPotionEffectEvent.Action.CHANGED)
                event.setCancelled(true);
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

    private void applyRestriction(Player player) {
        player.setFlying(false);
        player.setGameMode(GameMode.ADVENTURE);
        player.removePotionEffect(PotionEffectType.INVISIBILITY);
    }

    public SPPlugin getPlugin() {
        return plugin;
    }
}
