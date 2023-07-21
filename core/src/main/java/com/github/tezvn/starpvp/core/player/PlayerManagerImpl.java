package com.github.tezvn.starpvp.core.player;

import com.cryptomorin.xseries.XSound;
import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import com.github.tezvn.starpvp.api.AbstractDatabase.DatabaseInsertion;
import com.github.tezvn.starpvp.api.AbstractDatabase.MySQL;
import com.github.tezvn.starpvp.api.SPPlugin;
import com.github.tezvn.starpvp.api.player.PlayerManager;
import com.github.tezvn.starpvp.api.player.PlayerStatistic;
import com.github.tezvn.starpvp.api.player.cooldown.Cooldown;
import com.github.tezvn.starpvp.api.player.cooldown.CooldownType;
import com.github.tezvn.starpvp.api.player.cooldown.LogoutCooldown;
import com.github.tezvn.starpvp.api.player.cooldown.DeathCooldown;
import com.github.tezvn.starpvp.api.player.SPPlayer;
import com.github.tezvn.starpvp.api.rank.SPRank;
import com.github.tezvn.starpvp.core.player.cooldown.LogoutCooldownImpl;
import com.github.tezvn.starpvp.core.player.cooldown.DeathCooldownImpl;
import com.github.tezvn.starpvp.core.rank.SPRankImpl;
import com.github.tezvn.starpvp.core.utils.GsonHelper;
import com.github.tezvn.starpvp.core.utils.MessageUtils;
import com.github.tezvn.starpvp.core.utils.WGUtils;
import com.github.tezvn.starpvp.core.utils.time.TimeUnits;
import com.github.tezvn.starpvp.core.utils.time.TimeUtils;
import com.google.common.collect.Lists;
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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class PlayerManagerImpl implements PlayerManager, Listener {

    private final Map<UUID, SPPlayer> players = Maps.newHashMap();

    private final SPPlugin plugin;

    private String lobbyId;

    private final Map<UUID, Integer> killStreaks = Maps.newHashMap();

    public PlayerManagerImpl(SPPlugin plugin) {
        this.plugin = plugin;
        new PlayerHandler(this);
        registerOnline();
        loadLobby();
        loadFromDatabase();
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
                    saveToDatabase(player);
                    return spPlayer;
                });
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

    private void loadFromDatabase() {
        MySQL database = getPlugin().getDatabase();
        if (database != null && database.isConnected()) {
            String tableName = getPlugin().getDocument().getString("database.table-name", "user");
            try (Connection connection = plugin.getDatabase().getConnection()) {
                PreparedStatement statement = connection.prepareStatement("SELECT * FROM " + tableName);
                ResultSet rs = statement.executeQuery();
                while (rs.next()) {
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    SPPlayer spPlayer = new SPPlayerImpl(Bukkit.getOfflinePlayer(uuid));
                    SPRank rank = getPlugin().getRankManager().getRank(rs.getString("rank"));
                    if (rank == null)
                        rank = getPlugin().getRankManager().getLowestRank();
                    spPlayer.setRank(rank);
                    spPlayer.setEloPoint(Integer.parseInt(rs.getString("elo")));
                    Arrays.stream(rs.getString("statistic").split(",")).forEach(s -> {
                        String[] split = s.split(":");
                        PlayerStatistic statistic = PlayerStatistic.valueOf(split[0]);
                        spPlayer.setStatistic(statistic, Long.parseLong(split[1]));
                    });
                    String cdData = rs.getString("cooldown");
                    if (!cdData.isEmpty()) {
                        String[] cooldownData = cdData.split(",");
                        if (cooldownData.length == 3) {
                            CooldownType cooldownType = CooldownType.valueOf(cooldownData[0]);
                            Cooldown cooldown = null;
                            switch (cooldownType) {
                                case COMBAT_DEATH -> {
                                    cooldown = new DeathCooldownImpl(Long.parseLong(cooldownData[1]), Long.parseLong(cooldownData[2]));
                                }
                                case COMBAT_LOGOUT -> {
                                    cooldown = new LogoutCooldownImpl(Long.parseLong(cooldownData[1]), Long.parseLong(cooldownData[2]));
                                }
                            }
                            spPlayer.setCooldown(cooldown);
                        }
                    }

                    String kcData = rs.getString("kills_cooldown");
                    if (!kcData.isEmpty()) {
                        Arrays.stream(kcData.split(",")).forEach(s -> {
                            String[] split = s.split(":");
                            ((SPPlayerImpl) spPlayer).addKillCooldown(UUID.fromString(split[0]), Long.parseLong(split[1]));
                        });
                    }
                    this.players.put(uuid, spPlayer);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void saveToDatabase(UUID uuid) {
        SPPlayer spPlayer = getPlayer(uuid);
        if (spPlayer == null)
            return;
        MySQL database = getPlugin().getDatabase();
        if (database != null && database.isConnected()) {
            String tableName = getPlugin().getDocument().getString("database.table-name", "user");
            List<DatabaseInsertion> insertions = Lists.newArrayList();
            insertions.add(new DatabaseInsertion("uuid", spPlayer.getUniqueId().toString()));
            insertions.add(new DatabaseInsertion("player_name", spPlayer.getPlayerName()));
            insertions.add(new DatabaseInsertion("rank", spPlayer.getRank().getId()));
            insertions.add(new DatabaseInsertion("elo", String.valueOf(spPlayer.getEloPoint())));
            insertions.add(new DatabaseInsertion("statistic", Arrays.stream(PlayerStatistic.values())
                    .map(statistic -> statistic.name() + ":" + spPlayer.getStatistic(statistic))
                    .collect(Collectors.joining(","))));
            if (spPlayer.getCooldown() != null) {
                Cooldown cooldown = spPlayer.getCooldown();
                insertions.add(new DatabaseInsertion("cooldown", cooldown.getType().name() + "," + cooldown.getStartTime() + "," + cooldown.getEndTime()));
            } else
                insertions.add(new DatabaseInsertion("cooldown", ""));
            insertions.add(new DatabaseInsertion("kills_cooldown", spPlayer.getKillsCooldown().entrySet().stream()
                    .map(entry -> entry.getKey().toString() + ":" + entry.getValue())
                    .collect(Collectors.joining(","))));

            database.addOrUpdate(tableName,
                    new DatabaseInsertion("uuid", uuid.toString()), //Player to update
                    insertions.toArray(new DatabaseInsertion[0]));
        } else
            saveToLocal(spPlayer);
    }

    private void saveToLocal(SPPlayer spPlayer) {
        try {
            File folder = new File(plugin.getDataFolder() + "/users");
            if (!folder.exists())
                folder.mkdirs();
            File file = new File(plugin.getDataFolder() + "/users/" + spPlayer.getUniqueId().toString() + ".yml");
            if (!file.exists())
                file.createNewFile();
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            spPlayer.serialize().forEach(config::set);
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
        MySQL database = getPlugin().getDatabase();
        if (database != null && database.isConnected()) {
            String tableName = getPlugin().getDocument().getString("database.table-name", "user");
            try (Connection connection = plugin.getDatabase().getConnection()) {
                PreparedStatement statement = connection.prepareStatement("SELECT * FROM " + tableName + " WHERE uuid='" + uuid.toString() + "';");
                ResultSet rs = statement.executeQuery();
                if (rs == null || !rs.next())
                    return null;
                SPPlayer spPlayer = new SPPlayerImpl(Bukkit.getOfflinePlayer(uuid));
                SPRank rank = getPlugin().getRankManager().getRank(rs.getString("rank"));
                if (rank == null)
                    rank = getPlugin().getRankManager().getLowestRank();
                spPlayer.setRank(rank);
                spPlayer.setEloPoint(Integer.parseInt(rs.getString("elo")));
                Arrays.stream(rs.getString("statistic").split(",")).forEach(s -> {
                    String[] split = s.split(":");
                    PlayerStatistic statistic = PlayerStatistic.valueOf(split[0]);
                    spPlayer.setStatistic(statistic, Long.parseLong(split[2]));
                });
                String[] cooldownData = rs.getString("cooldown").split(",");
                if (cooldownData.length == 3) {
                    CooldownType cooldownType = CooldownType.valueOf(cooldownData[0]);
                    Cooldown cooldown = null;
                    switch (cooldownType) {
                        case COMBAT_DEATH -> {
                            cooldown = new DeathCooldownImpl(Long.parseLong(cooldownData[1]), Long.parseLong(cooldownData[2]));
                        }
                        case COMBAT_LOGOUT -> {
                            cooldown = new LogoutCooldownImpl(Long.parseLong(cooldownData[1]), Long.parseLong(cooldownData[2]));
                        }
                    }
                    spPlayer.setCooldown(cooldown);
                }

                Arrays.stream(rs.getString("kills_cooldown").split(",")).forEach(s -> {
                    String[] split = s.split(":");
                    ((SPPlayerImpl) spPlayer).addKillCooldown(UUID.fromString(split[0]), Long.parseLong(split[1]));
                });
                return spPlayer;
            } catch (SQLException e) {
                e.printStackTrace();
                return null;
            }
        } else
            return loadFromLocal(uuid);
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
        String rankId = config.getString("rank", "");
        SPRank rank = plugin.getRankManager().getRank(rankId);
        if (rank == null)
            rank = plugin.getRankManager().getLowestRank();
        spPlayer.setRank(rank);
        spPlayer.setEloPoint(Long.parseLong(config.getString("elo.current", "0")));
        boolean hasCooldown = config.getConfigurationSection("cooldown") != null;
        if (hasCooldown) {
            CooldownType cooldownType = CooldownType.valueOf(config.getString("cooldown.type", ""));
            long startTime = Long.parseLong(config.getString("cooldown.start-time", "0"));
            long endTime = Long.parseLong(config.getString("cooldown.end-time", "0"));
            Cooldown cooldown = null;
            switch (cooldownType) {
                case COMBAT_DEATH -> {
                    cooldown = new DeathCooldownImpl(startTime, endTime);
                }
                case COMBAT_LOGOUT -> {
                    cooldown = new LogoutCooldownImpl(startTime, endTime);
                }
            }
            spPlayer.setCooldown(cooldown);
        }
        config.getConfigurationSection("statistic").getKeys(false).forEach(s -> {
            PlayerStatistic statistic = PlayerStatistic.valueOf(s);
            spPlayer.setStatistic(statistic, config.getLong("statistic." + s));
        });
        config.getConfigurationSection("kills_cooldown").getKeys(false).forEach(s -> {
            String[] split = s.split(":");
            ((SPPlayerImpl) spPlayer).addKillCooldown(UUID.fromString(split[0]), Long.parseLong(split[1]));
        });
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
        long killCooldown = spKiller.getKillCooldown(victim);
        EloProcessor eloProcessor = new EloProcessor().setWinner(spKiller).setLoser(spVictim);
        long toAdd = eloProcessor.getNewWinnerEP() - eloProcessor.getOldWinnerEP();
        long toSubtract = eloProcessor.getOldLoserEP() - eloProcessor.getNewLoserEP();

        if (killCooldown != 0) {
            if (killCooldown > TimeUtils.newInstance().getNewTime()) {
                MessageUtils.sendMessage(killer, "&cBạn vừa giết người chơi này rồi");
                toAdd = toSubtract = 0;
            } else
                spKiller.removeKillCooldown(victim);
        }
        spVictim.subtractEloPoint(toSubtract);
        spKiller.addEloPoint(toAdd);

        spVictim.setStatistic(PlayerStatistic.DEATH_COUNT, spVictim.getStatistic(PlayerStatistic.DEATH_COUNT) + 1);
        spKiller.setStatistic(PlayerStatistic.KILL_COUNT, spVictim.getStatistic(PlayerStatistic.KILL_COUNT) + 1);

        applyReviveCooldown(victim);
        spKiller.addKillCooldown(victim);
        int currentStreak = this.killStreaks.getOrDefault(killer.getUniqueId(), 0);
        this.killStreaks.put(killer.getUniqueId(), currentStreak + 1);
//        String badget = getBadget(currentStreak);
//        MessageUtils.sendTitle(killer, "&6&l⚔ " + badget + " ⚔", "&7Bạn đã hạ gục được &e" + (currentStreak + 1) + " &7người chơi");
        MessageUtils.sendMessage(killer, "&7[&a&l+&7] &b" + toAdd + " &7điểm");
        MessageUtils.sendMessage(victim, "&7[&c&l-&7] &b" + toSubtract + " &7điểm");
        MessageUtils.broadcast(Lists.newArrayList(killer.getUniqueId(), victim.getUniqueId()), "&b" + killer.getName() + "&7(&a+" + toAdd
                + "&7) ⚔ &b" + victim.getName() + " &7(&c-" + toSubtract + "&7)");

        saveToDatabase(victim);
        saveToDatabase(killer);
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
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        onPlayerMove(event);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (event.getRespawnReason() != PlayerRespawnEvent.RespawnReason.DEATH)
            return;
        SPPlayer spPlayer = getPlayer(player);
        DeathCooldown deathCooldown = spPlayer.getCooldown();
        if (deathCooldown == null)
            return;
        if (lobbyId != null) {
            Location location = WGUtils.getSpawnLocation(lobbyId, player.getWorld());
            if (location != null)
                event.setRespawnLocation(location);
        }
        if (this.killStreaks.containsKey(player.getUniqueId())) {
            MessageUtils.sendTitle(player, "&f&l⚑", "&7Bạn đã bị mất chuỗi hạ gục");
            this.killStreaks.remove(player.getUniqueId());
        } else {
            MessageUtils.sendTitle(player, "&f&l⚑", "&7Bạn đã bị đánh bại");
            MessageUtils.sendMessage(player, "&cBạn đã được đặt vào hàng chờ, có thể tham gia giao tranh lại trong &6"
                    + TimeUtils.of(deathCooldown.getStartTime(), deathCooldown.getEndTime()).getShortDuration());
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player victim && event.getDamager() instanceof Player damager) {
            SPPlayer spVictim = getPlayer(victim);
            SPPlayer spDamager = getPlayer(damager);
            if (!WGUtils.isInPVPRegion(damager.getLocation()))
                return;
            if (spDamager.getStatistic(PlayerStatistic.COMBAT_TIMESTAMP) == 0L) {
                spDamager.setStatistic(PlayerStatistic.COMBAT_TIMESTAMP, TimeUtils.newInstance().getNewTime());
                MessageUtils.sendTitle(damager, "&a&l⚒",
                        "&7Tiến vào giao tranh");
                XSound.BLOCK_ENCHANTMENT_TABLE_USE.play(damager, 1f, -1f);
                applyRestriction(damager);
                saveToDatabase(damager);
            }
            if (spVictim.getStatistic(PlayerStatistic.COMBAT_TIMESTAMP) == 0L) {
                spVictim.setStatistic(PlayerStatistic.COMBAT_TIMESTAMP, TimeUtils.newInstance().getNewTime());
                MessageUtils.sendTitle(damager, "&a&l⚒",
                        "&7Tiến vào giao tranh");
                XSound.BLOCK_ENCHANTMENT_TABLE_USE.play(damager, 1f, -1f);
                applyRestriction(damager);
                saveToDatabase(victim);
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null)
            return;
        SPPlayer spPlayer = getPlayer(player);
        if (spPlayer.getStatistic(PlayerStatistic.COMBAT_TIMESTAMP) != 0) {
            if (WGUtils.isInPVPRegion(from) && !WGUtils.isInPVPRegion(to)) {
                event.setCancelled(true);
                MessageUtils.sendCooldownMessage(player, 10,
                        "&cBạn không được phép ra khỏi khu vực giao tranh trong khi đang giao tranh!");
            }
        } else if (spPlayer.getCooldown() != null) {
            if (!WGUtils.isInPVPRegion(from) && WGUtils.isInPVPRegion(to)) {
                event.setCancelled(true);

                for (CooldownType type : CooldownType.values()) {
                    Cooldown cooldown = spPlayer.getCooldown();
                    if (cooldown.getEndTime() < TimeUtils.newInstance().getNewTime()) {
                        spPlayer.removeCooldown();
                        continue;
                    }
                    TimeUtils tu = TimeUtils.of(cooldown.getStartTime()).add(cooldown.getEndTime());
                    String duration = tu.getShortDuration();
                    MessageUtils.sendCooldownMessage(player, 10, type == CooldownType.COMBAT_DEATH
                            ? "&cBạn phải chờ &6" + tu.getShortDuration() + " &cmới được tham gia đấu trường tiếp"
                            : "&cBạn đã bị cấm tham gia giao tranh, hiệu lực còn &6" + duration);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (getPlayer(player) == null)
            this.players.computeIfAbsent(player.getUniqueId(), uuid -> {
                SPPlayer spPlayer1 = new SPPlayerImpl(player);
                spPlayer1.setRank(plugin.getRankManager().getLowestRank());
                saveToDatabase(player);
                return spPlayer1;
            });
        SPPlayer spPlayer = getPlayer(player);

        LogoutCooldown logoutCooldown = spPlayer.getCooldown();
        if (logoutCooldown != null) {
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
        if (spPlayer.getStatistic(PlayerStatistic.COMBAT_TIMESTAMP) != 0) {
            if (spPlayer.getCooldown() != null && spPlayer.getCooldown().getType() == CooldownType.COMBAT_LOGOUT)
                return;
            spPlayer.setStatistic(PlayerStatistic.COMBAT_TIMESTAMP, 0);
            long combatLogoutTimes = spPlayer.getStatistic(PlayerStatistic.COMBAT_LOGOUT_TIMES);
            TimeUtils tu = TimeUtils.newInstance();
            if (combatLogoutTimes == 0)
                tu.add(TimeUnits.HOUR, 1);
            else if (combatLogoutTimes == 1)
                tu.add(TimeUnits.HOUR, 12);
            else if (combatLogoutTimes >= 2)
                tu.add(TimeUnits.DAY, 1);
            spPlayer.setCooldown(new LogoutCooldownImpl(tu.getNewTime()));
            spPlayer.setStatistic(PlayerStatistic.COMBAT_LOGOUT_TIMES, combatLogoutTimes + 1);
        }
        saveToDatabase(player);
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        SPPlayer spPlayer = getPlayer(player);
        if (spPlayer == null)
            return;
        if (spPlayer.getStatistic(PlayerStatistic.COMBAT_TIMESTAMP) != 0) {
            event.setCancelled(true);
            MessageUtils.sendTitle(player, "&c&l✖", "&7Không thể xài lệnh khi đang giao tranh");
        }
    }

    @EventHandler
    public void onToggleFlying(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        SPPlayer spPlayer = getPlayer(player);
        if (spPlayer == null)
            return;
        if (spPlayer.getStatistic(PlayerStatistic.COMBAT_TIMESTAMP) != 0)
            event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerGamemode(PlayerGameModeChangeEvent event) {
        Player player = event.getPlayer();
        SPPlayer spPlayer = getPlayer(player);
        if (spPlayer == null)
            return;
        if (spPlayer.getStatistic(PlayerStatistic.COMBAT_TIMESTAMP) != 0) {
            event.setCancelled(true);
            if (player.getGameMode() != GameMode.ADVENTURE)
                player.setGameMode(GameMode.ADVENTURE);
        }
    }

    @EventHandler
    public void onPlayerPotion(EntityPotionEffectEvent event) {
        if (event.getEntity() instanceof Player player) {
            SPPlayer spPlayer = getPlayer(player);
            if (spPlayer == null)
                return;
            if (spPlayer.getStatistic(PlayerStatistic.COMBAT_TIMESTAMP) != 0) {
                if (event.getModifiedType() != PotionEffectType.INVISIBILITY)
                    return;
                if (event.getAction() == EntityPotionEffectEvent.Action.ADDED
                        || event.getAction() == EntityPotionEffectEvent.Action.CHANGED)
                    event.setCancelled(true);
            }
        }
    }

    private void applyReviveCooldown(Player player) {
        SPPlayer spPlayer = getPlayer(player);
        long combatTimestamp = spPlayer.getStatistic(PlayerStatistic.COMBAT_TIMESTAMP);
        if (combatTimestamp == 0)
            return;
        int minutes = (int) ((System.currentTimeMillis() - combatTimestamp) / 60000);
        int reviveSeconds = 0;
        if (minutes <= 6)
            reviveSeconds = minutes * 2 + 4;
        else if (minutes == 7)
            reviveSeconds = 21;
        else if (minutes > 8)
            reviveSeconds = minutes * 2 + 6;
        TimeUtils tu = TimeUtils.newInstance().add(TimeUnits.SECOND, reviveSeconds);
        spPlayer.setCooldown(new DeathCooldownImpl(tu.getNewTime()));
        spPlayer.setStatistic(PlayerStatistic.COMBAT_TIMESTAMP, 0L);
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
