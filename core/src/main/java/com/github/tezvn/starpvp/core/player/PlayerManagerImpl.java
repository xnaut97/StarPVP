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

    private String lobbyId;

    private final Map<Type, AbstractHandler<?>> handler = Maps.newHashMap();

    private final Map<UUID, Integer> killStreaks = Maps.newHashMap();



    public PlayerManagerImpl(SPPlugin plugin) {
        this.plugin = plugin;
        registerHandler();
        registerOnline();
        loadLobby();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private void loadLobby() {
        this.lobbyId = plugin.getConfig().getString("lobby", null);
    }

    private void registerOnline() {
        Bukkit.getOnlinePlayers().forEach(player -> {
            if (getPlayer(player) == null)
                this.players.computeIfAbsent(player.getUniqueId(), uuid -> {
                    SPPlayer spPlayer = new SPPlayerImpl(player);
                    spPlayer.setRank(plugin.getRankManager().getLowestRank());
                    return spPlayer;
                });
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
        } else
            saveToLocal(map);
    }

    private void saveToLocal(Map<String, Object> map) {
        try {
            File folder = new File(plugin.getDataFolder() + "/users");
            if (!folder.exists())
                folder.mkdirs();
            File file = new File(plugin.getDataFolder() + "/users/" + map.get("uuid") + ".yml");
            if (!file.exists())
                file.createNewFile();
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            map.forEach(config::set);
            config.save(file);
        } catch (Exception e) {
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
                    SPRank rank = plugin.getRankManager().getRank(map.get("rank"));
                    if (rank == null)
                        rank = plugin.getRankManager().getLowestRank();
                    spPlayer.setRank(rank);
                    spPlayer.setEloPoint(Long.parseLong(map.get("sp.current")));
                    ((SPPlayerImpl) spPlayer).setPenaltyTimes(Integer.parseInt(map.get("penalty.times")));
                    ((SPPlayerImpl) spPlayer).setPenalty(Boolean.parseBoolean(map.get("penalty.activate")));
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
        } else
            spPlayer = loadFromLocal(uuid);
        return spPlayer;
    }

    private SPPlayer loadFromLocal(UUID uuid) {
        File folder = new File(plugin.getDataFolder() + "/users");
        if (!folder.exists())
            return null;
        File[] files = folder.listFiles();
        if (files == null || files.length == 0)
            return null;
        File file = Arrays.stream(files)
                .filter(f -> !f.isDirectory() && f.getName().endsWith(".yml"))
                .findAny().orElse(null);
        if (file == null)
            return null;
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        SPPlayer spPlayer = new SPPlayerImpl(Bukkit.getOfflinePlayer(uuid));
        String rankId = config.getString("rank", "none");
        SPRank rank = plugin.getRankManager().getRank(rankId);
        if (rank == null)
            rank = plugin.getRankManager().getLowestRank();
        spPlayer.setRank(rank);
        spPlayer.setEloPoint(Long.parseLong(config.getString("sp.current", "0")));
        ((SPPlayerImpl) spPlayer).setPenaltyTimes(Integer.parseInt(config.getString("penalty.times", "0")));
        ((SPPlayerImpl) spPlayer).setPenalty(Boolean.parseBoolean(config.getString("penalty.activate", "false")));
        boolean hasCooldown = config.getConfigurationSection("cooldown") != null;
        if (hasCooldown) {
            long startTime = Long.parseLong(config.getString("cooldown.start-time", "0"));
            long endTime = Long.parseLong(config.getString("cooldown.end-time", "0"));
            this.combatCooldown.put(uuid, new CombatCooldownImpl(spPlayer, startTime, endTime));
        }
        long combatTimestamp = Long.parseLong(config.getString("combat-since", "-1"));
        if (combatTimestamp != -1)
            this.combatTimestamp.put(uuid, combatTimestamp);

        long penaltyTime = Long.parseLong(config.getString("penalty.until", "-1"));
        if (penaltyTime != -1)
            this.combatPenalty.put(uuid, penaltyTime);

        return spPlayer;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null)
            return;
        long combatTimestamp = this.combatTimestamp.get(victim.getUniqueId());


        SPPlayer spVictim = getPlayer(victim);
        SPPlayer spKiller = getPlayer(killer);

        EloProcessor eloProcessor = new EloProcessor().setWinner(spKiller).setLoser(spVictim);
        long toAdd = eloProcessor.getNewLoserEP() - eloProcessor.getOldLoserEP();
        long toSubtract = eloProcessor.getOldLoserEP() - eloProcessor.getNewLoserEP();
        spVictim.addEloPoint(toAdd);
        spKiller.addEloPoint(toSubtract);

        spVictim.setStatistic(PlayerStatistic.DEATH_COUNT, spVictim.getStatistic(PlayerStatistic.DEATH_COUNT) + 1);
        spKiller.setStatistic(PlayerStatistic.KILL_COUNT, spVictim.getStatistic(PlayerStatistic.KILL_COUNT) + 1);

        applyReviveCooldown(victim);

        int currentStreak = this.killStreaks.getOrDefault(killer.getUniqueId(), 0);
        this.killStreaks.put(killer.getUniqueId(), currentStreak + 1);
//        String badget = getBadget(currentStreak);
//        MessageUtils.sendTitle(killer, "&6&l⚔ " + badget + " ⚔", "&7Bạn đã hạ gục được &e" + (currentStreak + 1) + " &7người chơi");
        long offsetKiller = spKiller.getEloPoint() - eloProcessor.getOldWinnerEP();
        long offsetVictim = eloProcessor.getOldLoserEP() - spVictim.getEloPoint();
        MessageUtils.sendMessage(killer, "&7[&a&l+&7] &b" + offsetKiller + " &7điểm");
        MessageUtils.sendMessage(killer, "&7[&c&l-&7] &b" + offsetVictim + " &7điểm");
        MessageUtils.broadcast("&b" + killer.getName() + "&7(&a+" + offsetKiller + "&7) ⚔ &b" + victim.getName() + " &7(&c-" + offsetVictim + "&7)");
    }

    private String getBadget(int currentStreak) {
        if (currentStreak == 0)
            return "HẠ GỤC";
        else if (currentStreak == 1)
            return "DOUBLE KILL";
        else if (currentStreak == 2)
            return "TRIPLE KILL";
        else if (currentStreak == 3)
            return "QUADRA KILL";
        else if (currentStreak == 4)
            return "PENTA KILL";
        else if (currentStreak == 5)
            return "HUYỀN THOẠI";
        else
            return "SIÊU VIỆT";
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (event.getRespawnReason() != PlayerRespawnEvent.RespawnReason.DEATH)
            return;
        CombatCooldown combatCooldown = this.combatCooldown.getOrDefault(player.getUniqueId(), null);
        if (combatCooldown == null)
            return;
        if (lobbyId != null) {
            Location location = WGUtils.getSpawnLocation(lobbyId, player.getWorld());
            event.setRespawnLocation(location);
        }
        if (this.killStreaks.containsKey(player.getUniqueId())) {
            MessageUtils.sendTitle(player, "&f&l⚑", "&7Bạn đã bị mất chuỗi hạ gục");
            this.killStreaks.remove(player.getUniqueId());
        } else
            MessageUtils.sendTitle(player, "&f&l⚑", "&7Bạn đã bị đánh bại");
        MessageUtils.sendMessage(player, "&cBạn đã được đặt vào hàng chờ, có thể tham gia giao tranh lại trong &6"
                + TimeUtils.of(combatCooldown.getStartTime(), combatCooldown.getEndTime()).getShortDuration());
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

            MessageUtils.sendTitle(victim, "&a&l⚒",
                    "&7Bạn nhận sát thương từ người chơi &6" + damager.getName());
            MessageUtils.sendTitle(damager, "&a&l⚒",
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
            this.players.computeIfAbsent(player.getUniqueId(), uuid -> {
                SPPlayer spPlayer1 = new SPPlayerImpl(player);
                spPlayer1.setRank(plugin.getRankManager().getLowestRank());
                return spPlayer1;
            });
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
        int reviveSeconds = 0;
        if (minutes <= 6)
            reviveSeconds = minutes * 2 + 4;
        else if (minutes == 7)
            reviveSeconds = 21;
        else if (minutes > 8)
            reviveSeconds = minutes * 2 + 6;
        TimeUtils tu = TimeUtils.newInstance().add(TimeUnits.SECOND, reviveSeconds);
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
