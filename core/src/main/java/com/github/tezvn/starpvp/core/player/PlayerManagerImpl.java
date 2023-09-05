package com.github.tezvn.starpvp.core.player;

import com.cryptomorin.xseries.XSound;
import com.github.tezvn.starpvp.api.AbstractDatabase.DatabaseInsertion;
import com.github.tezvn.starpvp.api.AbstractDatabase.MySQL;
import com.github.tezvn.starpvp.api.SPPlugin;
import com.github.tezvn.starpvp.api.player.PlayerManager;
import com.github.tezvn.starpvp.api.player.PlayerStatistic;
import com.github.tezvn.starpvp.api.player.SPPlayer;
import com.github.tezvn.starpvp.api.player.cooldown.Cooldown;
import com.github.tezvn.starpvp.api.player.cooldown.CooldownType;
import com.github.tezvn.starpvp.api.rank.SPRank;
import com.github.tezvn.starpvp.core.SPPluginImpl;
import com.github.tezvn.starpvp.core.integration.ClanIntegration;
import com.github.tezvn.starpvp.core.integration.WorldGuardIntegration;
import com.github.tezvn.starpvp.core.log.LogType;
import com.github.tezvn.starpvp.core.log.PlayerLog;
import com.github.tezvn.starpvp.core.log.TeamLog;
import com.github.tezvn.starpvp.core.player.cooldown.DeathCooldownImpl;
import com.github.tezvn.starpvp.core.player.cooldown.LogoutCooldownImpl;
import com.github.tezvn.starpvp.core.utils.ClickableText;
import com.github.tezvn.starpvp.core.utils.MessageUtils;
import com.github.tezvn.starpvp.core.utils.time.TimeUnits;
import com.github.tezvn.starpvp.core.utils.time.TimeUtils;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import de.tr7zw.changeme.nbtapi.NBTItem;
import me.ulrich.clans.Clans;
import me.ulrich.clans.data.ClanData;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.ItemTag;
import net.md_5.bungee.api.chat.hover.content.Item;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class PlayerManagerImpl implements PlayerManager, Listener {

    private final Map<UUID, SPPlayer> players = Maps.newHashMap();

    private final SPPlugin plugin;

    private final Map<UUID, Integer> killStreaks = Maps.newHashMap();

    private final PlayerLog playerLog;

    private final TeamLog teamLog;

    private final List<UUID> combatLogged = Lists.newArrayList();

    private final Map<Long, List<String>> lowEloPenalty = Maps.newHashMap();

    private final Map<String, List<String>> loggedPenalty = Maps.newHashMap();

    public PlayerManagerImpl(SPPlugin plugin) {
        this.plugin = plugin;
        this.playerLog = ((SPPluginImpl) plugin).getLog(LogType.PLAYER);
        this.teamLog = ((SPPluginImpl) plugin).getLog(LogType.TEAM);
        new PlayerHandler(this);
        registerOnline();
        loadFromDatabase();
        loadLowEloPenalty();
        loadLoggedPenalty();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private Optional<ProtectedRegion> getPVPRegion(Location location) {
        String pvpId = plugin.getDocument().getString("region.pvp");
        if (pvpId == null) return Optional.empty();
        ProtectedRegion region = WorldGuardIntegration.getRegion(location);
        if (region == null || !region.getId().equals(pvpId)) return Optional.empty();
        return Optional.of(region);
    }

    private Optional<Location> getLobbySpawnLocation(Location location) {
        String lobbyId = plugin.getDocument().getString("region.lobby");
        if (lobbyId == null) return Optional.empty();
        return Optional.ofNullable(WorldGuardIntegration.getSpawnLocation(lobbyId, location.getWorld(), true));
    }

    private void registerOnline() {
        Bukkit.getOnlinePlayers().forEach(player -> {
            if (getPlayer(player) == null)
                this.players.computeIfAbsent(player.getUniqueId(), uuid -> {
                    SPPlayer spPlayer = new SPPlayerImpl(player);
                    spPlayer.setEloPoint(1000);
                    saveToDatabase(player);
                    return spPlayer;
                });
        });
    }

    private void loadLowEloPenalty() {
        plugin.getDocument().getOptionalSection("penalty.low-elo").ifPresent(section -> section.getKeys()
                .forEach(key -> section.getOptionalStringList(key + ".commands")
                        .ifPresent(list -> lowEloPenalty.put(Long.parseLong(String.valueOf(key)), list))));
    }

    private void loadLoggedPenalty() {
        plugin.getDocument().getOptionalSection("penalty.combat-logged").ifPresent(section -> section.getKeys()
                .forEach(key -> {
                    String keyStr = String.valueOf(key);
                    String[] split = keyStr.split("-");
                    if (split.length == 2) {
                        int min = Integer.parseInt(split[0]);
                        int max = Integer.parseInt(split[1]);
                        if (min == max) return;
                        if (loggedPenalty.containsKey(split[0]) || loggedPenalty.containsKey(split[1])) return;
                    }
                    section.getOptionalStringList(key.toString())
                            .ifPresent(list -> loggedPenalty.put(String.valueOf(key), list));
                }));
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
                .filter(p -> p.asOfflinePlayer().getName() != null && p.asOfflinePlayer().getName().equals(name))
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
        } else loadFromLocal();
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

    private void loadFromLocal() {
        File folder = new File(plugin.getDataFolder() + "/users");
        if (!folder.exists()) {
            folder.mkdirs();
            return;
        }
        File[] files = folder.listFiles();
        if (files == null || files.length == 0)
            return;
        Arrays.stream(files)
                .filter(f -> !f.isDirectory() && f.getName().endsWith(".yml"))
                .forEach(file -> {
                    UUID uuid = UUID.fromString(file.getName().replace(".yml", ""));
                    FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                    SPPlayer spPlayer = new SPPlayerImpl(Bukkit.getOfflinePlayer(uuid));
                    String rankId = config.getString("rank", "");
                    spPlayer.setEloPoint(Long.parseLong(config.getString("elo", "0")));
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
                    ConfigurationSection kdSection = config.getConfigurationSection("kills-cooldown");
                    if (kdSection != null)
                        kdSection.getKeys(false).forEach(s -> {
                            ((SPPlayerImpl) spPlayer).addKillCooldown(UUID.fromString(s), config.getLong("kills-cooldown." + s));
                        });

                    this.players.putIfAbsent(uuid, spPlayer);
                });
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
        spPlayer.setEloPoint(Long.parseLong(config.getString("elo", "0")));
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
        ConfigurationSection kdSection = config.getConfigurationSection("kills-cooldown");
        if (kdSection != null)
            kdSection.getKeys(false).forEach(s -> {
                ((SPPlayerImpl) spPlayer).addKillCooldown(UUID.fromString(s), config.getLong("kills-cooldown." + s));
            });
        return spPlayer;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null)
            return;
        if (!(getPVPRegion(victim.getLocation()).isPresent() && getPVPRegion(killer.getLocation()).isPresent()))
            return;
        if (combatLogged.contains(victim.getUniqueId())) return;
        event.setDeathMessage("");
        SPPlayer spVictim = getPlayer(victim);
        SPPlayer spKiller = getPlayer(killer);
        long killCooldown = spKiller.getKillCooldown(victim);
        EloProcessor eloProcessor = new EloProcessor().setWinner(spKiller).setLoser(spVictim);
        long toAdd = eloProcessor.getWinnerEPGain();
        long toSubtract = eloProcessor.getLoserEPLost();

        if (killCooldown > 0) {
            if (killCooldown > TimeUtils.newInstance().getNewTime()) {
                MessageUtils.sendMessage(killer, "&cBạn vừa giết người chơi này rồi");
                toAdd = toSubtract = 0;
            } else
                spKiller.removeKillCooldown(victim);
        }

        addElo(spKiller, toAdd);
        subtractElo(spVictim, toSubtract);
        subtractTeamMembersElo(victim, toAdd);

        spVictim.setStatistic(PlayerStatistic.DEATH_COUNT, spVictim.getStatistic(PlayerStatistic.DEATH_COUNT) + 1);
        spKiller.setStatistic(PlayerStatistic.KILL_COUNT, spVictim.getStatistic(PlayerStatistic.KILL_COUNT) + 1);

        spVictim.setStatistic(PlayerStatistic.LAST_COMBAT_TIME, TimeUtils.newInstance().getNewTime());
        spKiller.setStatistic(PlayerStatistic.LAST_COMBAT_TIME, TimeUtils.newInstance().getNewTime());

        applyReviveCooldown(victim);
        int currentStreak = this.killStreaks.getOrDefault(killer.getUniqueId(), 0);
        this.killStreaks.put(killer.getUniqueId(), currentStreak + 1);
//        String badget = getBadget(currentStreak);
//        MessageUtils.sendTitle(killer, "&6&l⚔ " + badget + " ⚔", "&7Bạn đã hạ gục được &e" + (currentStreak + 1) + " &7người chơi");

//        MessageUtils.broadcast(Lists.newArrayList(killer.getUniqueId(), victim.getUniqueId()), "&b" + killer.getName() + "&7(&a+" + toAdd
//                + "&7) ⚔ &b" + victim.getName() + " &7(&c-" + toSubtract + "&7)");
        ItemStack item = killer.getInventory().getItemInMainHand();
        String itemName = "&b" + getTranslation(item);
        ItemMeta meta = item.getItemMeta();
        if (meta != null)
            itemName = meta.hasDisplayName() ? meta.getDisplayName() : itemName;

        String text = "&b" + killer.getName() + "&7(&a+" + toAdd + "&7) ⚔ &b" + victim.getName() + " &7(&c-" + toSubtract + "&7)"
                + (itemName.isEmpty() ? "" : " &fbởi ");

        List<BaseComponent> components = Lists.newArrayList();
        components.add(new ClickableText(text).build());
        if (!itemName.isEmpty()) {
            if (item.getType() != Material.AIR && item.getAmount() > 0) {
                components.add(new ClickableText(itemName)
                        .setHoverAction(HoverEvent.Action.SHOW_ITEM,
                                new Item("minecraft:" + item.getType().toString().toLowerCase(),
                                        item.getAmount(), ItemTag.ofNbt(new NBTItem(item).getCompound().toString())))
                        .build());
            }
        }
        Bukkit.getOnlinePlayers().stream().filter(p -> !(p.getUniqueId().equals(killer.getUniqueId())))
                .forEach(p -> p.spigot().sendMessage(components.toArray(new BaseComponent[0])));

        if (this.playerLog != null) this.playerLog.write(eloProcessor);

        spVictim.setStatistic(PlayerStatistic.COMBAT_TIMESTAMP, 0L);
        spKiller.addKillCooldown(victim);

        saveToDatabase(victim);
        saveToDatabase(killer);

        if (!victim.getUniqueId().toString().startsWith("00000000-"))
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                victim.spigot().respawn();
            });
    }

    private void subtractTeamMembersElo(Player player, long toSubtract) {
        ClanData clan = ClanIntegration.getClan(player);
        if(clan == null) return;

        StringBuilder sb = new StringBuilder(" | Online members: ");
        clan.getOnlineMembers().forEach(p -> {
            SPPlayer spPlayer = getPlayer(p);
            if (spPlayer == null) return;

            long divided = Math.max(1, toSubtract / 2);
            subtractElo(spPlayer, divided);
            saveToDatabase(p);
            sb.append(spPlayer.getPlayerName()).append(" (").append(spPlayer.getEloPoint()).append(") (-").append(divided).append(")");
        });
        if (this.teamLog != null) this.teamLog.write("Clan: " + clan.getId(), sb.toString());
    }

    private void addElo(SPPlayer spPlayer, long toAdd) {
        SPRank oldRank = spPlayer.getRank();
        spPlayer.addEloPoint(toAdd);
        SPRank newRank = spPlayer.getRank();
        MessageUtils.sendMessage(spPlayer.asPlayer(), "&7[&a&l+&7] &b" + toAdd + " &7điểm");
        boolean promoted = oldRank.getElo() < newRank.getElo();
        if (promoted) {
            MessageUtils.sendMessage(spPlayer.asPlayer(), "&6Thăng hạng: " + oldRank.getDisplayName() + " &a► " + newRank.getDisplayName());
            XSound.ENTITY_PLAYER_LEVELUP.play(spPlayer.asPlayer());
        }
    }

    private void subtractElo(SPPlayer spPlayer, long toSubtract) {
        SPRank oldRank = spPlayer.getRank();
        spPlayer.subtractEloPoint(toSubtract);
        SPRank newRank = spPlayer.getRank();
        MessageUtils.sendMessage(spPlayer.asPlayer(), "&7[&c&l-&7] &b" + toSubtract + " &7điểm");
        boolean promoted = oldRank.getElo() < newRank.getElo();
        if (promoted) {
            MessageUtils.sendMessage(spPlayer.asPlayer(), "&6Giáng hạng: " + oldRank.getDisplayName() + " &c► " + newRank.getDisplayName());
            XSound.BLOCK_ANVIL_FALL.play(spPlayer.asPlayer(), -.5f, -.5f);
        }
        List<Long> sorted = this.lowEloPenalty.keySet().stream().sorted(Comparator.reverseOrder()).toList();

        long selected = -1;
        for (long elo : sorted) {
            if (elo < spPlayer.getEloPoint()) break;
            selected = elo;
        }
        if (selected == -1) return;
        lowEloPenalty.get(selected).stream().map(s -> s.replace("@player-name@", spPlayer.getPlayerName()))
                .forEach(s -> Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), s));
    }

    private String getTranslation(ItemStack item) {
        InputStream stream = plugin.getResource("lang-vi.json");
        if (stream == null)
            return "";
        InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
        try {
            String key = item.getTranslationKey();
            if (!key.startsWith("item.minecraft"))
                return "";
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            JsonElement element = json.get(item.getTranslationKey());
            return element == null ? "" : element.getAsString();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
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
        if (event.getPlayer().hasMetadata("teleport-spawn")) return;
        onPlayerMove(event);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (event.getRespawnReason() != PlayerRespawnEvent.RespawnReason.DEATH)
            return;
        SPPlayer spPlayer = getPlayer(player);
        Cooldown cooldown = spPlayer.getCooldown();
        if (cooldown == null) return;

        switch (cooldown.getType()) {
            case COMBAT_DEATH -> {
//                getLobbySpawnLocation(player.getLocation()).ifPresent(event::setRespawnLocation);
                teleportToLobby(player);
                if (this.killStreaks.containsKey(player.getUniqueId())) {
                    MessageUtils.sendTitle(player, "&f&l⚑", "&7Bạn đã bị mất chuỗi hạ gục");
                    this.killStreaks.remove(player.getUniqueId());
                } else {
                    MessageUtils.sendTitle(player, "&f&l⚑", "&7Bạn đã bị đánh bại");
                    MessageUtils.sendMessage(player, "&cBạn đã được đặt vào hàng chờ, có thể tham gia giao tranh lại trong &6"
                            + TimeUtils.of(cooldown.getStartTime(), cooldown.getEndTime()).getShortDuration());
                }
            }
            case COMBAT_LOGOUT -> {
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player victim && event.getDamager() instanceof Player damager) {
            if (getPVPRegion(victim.getLocation()).isPresent() && getPVPRegion(damager.getLocation()).isPresent()) {
                applyCombatRestriction(victim);
                applyCombatRestriction(damager);
            }
        }
    }

    private void applyCombatRestriction(Player player) {
        SPPlayer spPlayer = getPlayer(player);
        boolean firstCombat = spPlayer.getStatistic(PlayerStatistic.COMBAT_TIMESTAMP) == 0;
        spPlayer.setStatistic(PlayerStatistic.COMBAT_TIMESTAMP, TimeUtils.newInstance().getNewTime());
        if (firstCombat) {
            MessageUtils.sendTitle(player, "&a&l⚒", "&7Tiến vào giao tranh");
            XSound.BLOCK_ENCHANTMENT_TABLE_USE.play(player, 1f, -1f);
        }
        applyRestriction(player);
        saveToDatabase(player);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null)
            return;
        SPPlayer spPlayer = getPlayer(player);
        if (spPlayer.getStatistic(PlayerStatistic.COMBAT_TIMESTAMP) > 0) {
            if (WorldGuardIntegration.isInPVPRegion(from) && !WorldGuardIntegration.isInPVPRegion(to)) {
                event.setCancelled(true);
//                player.setVelocity(to.toVector().subtract(from.toVector()).multiply(-1));
                MessageUtils.sendCooldownMessage(player, 10,
                        "&cBạn không được phép ra khỏi khu vực giao tranh trong khi đang giao tranh!");
            }
            return;
        }
        Cooldown cooldown = spPlayer.getCooldown();
        if (cooldown == null) return;

        Optional<ProtectedRegion> optFrom = getPVPRegion(from);
        Optional<ProtectedRegion> optTo = getPVPRegion(to);

        if (optFrom.isPresent()) {
            if (optTo.isPresent()) {
                event.setCancelled(true);
                teleportToLobby(player);
                return;
            }
        }
        if (optTo.isEmpty()) return;
//      player.setVelocity(to.toVector().subtract(from.toVector()).normalize().multiply(-1));
        event.setCancelled(true);
        player.teleport(player.getWorld().getSpawnLocation());
        long now = TimeUtils.newInstance().getNewTime();
        TimeUtils tu = TimeUtils.newInstance().add(cooldown.getEndTime() - now);
        String duration = tu.getShortDuration();
        MessageUtils.sendCooldownMessage(player, 10, cooldown.getType() == CooldownType.COMBAT_DEATH
                ? "&cBạn phải chờ &6" + tu.getShortDuration() + "&cmới được tham gia đấu trường tiếp"
                : "&cBạn đã bị cấm tham gia giao tranh, hiệu lực còn &6" + duration);
    }

    private void teleportToLobby(Player player) {
        player.setMetadata("teleport-spawn", new FixedMetadataValue(plugin, true));
        player.teleport(player.getWorld().getSpawnLocation());
//        getLobbySpawnLocation(player.getLocation()).ifPresentOrElse(player::teleport,
//                () -> player.teleport(player.getWorld().getSpawnLocation()));
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> player.removeMetadata("teleport-spawn", plugin), 5L);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        SPPlayer spPlayer = getPlayer(player);
        if (spPlayer == null)
            spPlayer = this.players.computeIfAbsent(player.getUniqueId(), uuid -> {
                SPPlayer spPlayer1 = new SPPlayerImpl(player);
                spPlayer1.setEloPoint(1000);
                saveToDatabase(player);
                return spPlayer1;
            });
        combatLogged.removeIf(u -> u.equals(player.getUniqueId()));
        Cooldown cooldown = spPlayer.getCooldown();
        if (cooldown == null) return;
        switch (cooldown.getType()) {
            case COMBAT_DEATH -> {
            }
            case COMBAT_LOGOUT -> {
//                player.damage(player.getHealth());
//                MessageUtils.sendTitle(player,
//                        "&c&lBẠN BỊ TRỪ ĐIỂM!",
//                        "&7Vì đã thoát trong lúc giao tranh.");
//                XSound.BLOCK_ENCHANTMENT_TABLE_USE.play(player);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        SPPlayer spPlayer = getPlayer(player);
        if (spPlayer == null) return;

        if (spPlayer.getStatistic(PlayerStatistic.COMBAT_TIMESTAMP) > 0) {
            if (!combatLogged.contains(player.getUniqueId()))
                combatLogged.add(player.getUniqueId());
            long combatLogoutTimes = spPlayer.getStatistic(PlayerStatistic.COMBAT_LOGOUT_TIMES);
            TimeUtils tu = TimeUtils.newInstance();
            plugin.getDocument().getOptionalString("cooldown.logout")
                    .ifPresentOrElse(tu::add, () -> tu.add(TimeUnits.SECOND, 30));
            spPlayer.setCooldown(new LogoutCooldownImpl(tu.getNewTime()));
            spPlayer.setStatistic(PlayerStatistic.COMBAT_LOGOUT_TIMES, combatLogoutTimes + 1);

            StringBuilder sb = new StringBuilder(player.getName() + " logged out (@elo-point@) (-@subtract-elo@)");
            long oldElo = spPlayer.getEloPoint();
            List<String> commands = loggedPenalty.getOrDefault(String.valueOf(combatLogoutTimes+1), loggedPenalty.entrySet().stream()
                    .filter(entry -> {
                        if(entry.getKey().equals("default")) return false;
                        String[] split = entry.getKey().split("-");
                        if(split.length != 2) return false;
                        int min = Integer.parseInt(split[0]);
                        int max = Integer.parseInt(split[1]);
                        return min <= combatLogoutTimes+1 && combatLogoutTimes+1 <= max;
                    }).map(Map.Entry::getValue).findAny().orElse(loggedPenalty.getOrDefault("default", Lists.newArrayList())));

            commands.stream().map(s -> s.replace("@player-name@", player.getName()))
                    .forEach(s -> Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), s));

//            plugin.getDocument().getOptionalLong("penalty.combat-logout").ifPresentOrElse(elo -> {
//                spPlayer.subtractEloPoint(elo);
//                MessageUtils.broadcast("&6Người chơi &b" + player.getName()
//                        + " &6đã thoát trong lúc giao tranh, bị &c-" + elo + " &6điểm và xử chết");
//                sb.append(" (-").append(elo).append(")");
//            }, () -> MessageUtils.broadcast("&6Người chơi &b" + player.getName()
//                    + " &6đã thoát trong lúc giao tranh và đã bị xử chết"));

            StringBuilder broadcast = new StringBuilder("&6Người chơi &b" + player.getName() + " đã thoát trong lúc giao tranh");
            long subtract = oldElo - spPlayer.getEloPoint();
            if(subtract > 0) broadcast.append(", bị &c-").append(subtract).append(" &6điểm");

            plugin.getDocument().getOptionalBoolean("penalty.combat-logged.kill-on-logout").ifPresent(value -> {
                if(!value) return;
                player.setHealth(0);
                broadcast.append(", bị xử chết");
            });
            MessageUtils.broadcast(broadcast.toString());

            spPlayer.setStatistic(PlayerStatistic.COMBAT_TIMESTAMP, 0);

            if (this.playerLog != null)
                this.playerLog.write(sb.toString().replace("@elo-point@", String.valueOf(spPlayer.getEloPoint()))
                        .replace("@subtract-elo@", String.valueOf(spPlayer.getEloPoint() - oldElo)));
        }
        saveToDatabase(player);
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        SPPlayer spPlayer = getPlayer(player);
        if (spPlayer == null) return;

        if (spPlayer.getStatistic(PlayerStatistic.COMBAT_TIMESTAMP) > 0) {
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
            if (event.getNewGameMode() == GameMode.CREATIVE || event.getNewGameMode() == GameMode.SPECTATOR)
                event.setCancelled(true);
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

    @EventHandler
    public void onPlayerToggleSwing(EntityToggleGlideEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Player)) return;

    }

    private void applyReviveCooldown(Player player) {
        SPPlayer spPlayer = getPlayer(player);
        plugin.getDocument().getOptionalString("cooldown.death").ifPresent(duration -> {
            TimeUtils tu = TimeUtils.newInstance().add(duration);
            spPlayer.setCooldown(new DeathCooldownImpl(tu.getNewTime()));
            spPlayer.setStatistic(PlayerStatistic.COMBAT_TIMESTAMP, 0L);
        });

//        long combatTimestamp = spPlayer.getStatistic(PlayerStatistic.COMBAT_TIMESTAMP);
//        if (combatTimestamp == 0)
//            return;
//        int minutes = (int) ((System.currentTimeMillis() - combatTimestamp) / 60000);
//        int reviveSeconds = 0;
//        if (minutes <= 6)
//            reviveSeconds = minutes * 2 + 4;
//        else if (minutes == 7)
//            reviveSeconds = 21;
//        else if (minutes > 8)
//            reviveSeconds = minutes * 2 + 6;
//        TimeUtils tu = TimeUtils.newInstance().add(TimeUnits.SECOND, reviveSeconds);
//        spPlayer.setCooldown(new DeathCooldownImpl(tu.getNewTime()));
//        spPlayer.setStatistic(PlayerStatistic.COMBAT_TIMESTAMP, 0L);
    }

    private void applyRestriction(Player player) {
        player.setFlying(false);
        if (player.getGameMode() != GameMode.SURVIVAL)
            player.setGameMode(GameMode.SURVIVAL);
        player.removePotionEffect(PotionEffectType.INVISIBILITY);
    }

    public SPPlugin getPlugin() {
        return plugin;
    }

}
