package com.github.tezvn.starpvp.core.gui;

import com.cryptomorin.xseries.XMaterial;
import com.github.tezvn.starpvp.api.SPPlugin;
import com.github.tezvn.starpvp.api.player.SPPlayer;
import com.github.tezvn.starpvp.core.utils.BaseMenu;
import com.github.tezvn.starpvp.core.utils.ItemCreator;
import com.github.tezvn.starpvp.core.utils.ThreadWorker;
import de.tr7zw.changeme.nbtapi.NBTItem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

public class TopRanking extends BaseMenu.PaginationMenu<SPPlayer> {

    public TopRanking() {
        super(0, 6, "&lBẢNG XẾP HẠNG NGƯỜI CHƠI");
    }

    @Override
    public List<SPPlayer> getObjects() {
        return ((SPPlugin) getPlugin()).getPlayerManager().getPlayers().stream()
                .sorted(Comparator.comparing(SPPlayer::getEloPoint, Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }

    @Override
    public InventoryElement getObjectItem(SPPlayer player) {
        ItemCreator creator = new ItemCreator(Objects.requireNonNull(
                XMaterial.PLAYER_HEAD.parseItem()));
        int order = getOrder(player);
        String orderColor = "&f";
        String symbol = "&f&l";
        switch (order) {
            case 1:
                symbol = "&c&l♕";
                orderColor = "&a";
                break;
            case 2:
                symbol = "&6&l♔";
                orderColor = "&6";
                break;
            case 3:
                symbol = "&b&l♖";
                orderColor = "&b";
                break;
            case 4, 5, 6, 7, 8 ,9 ,10:
                symbol = "&3&l♙";
                break;
        }
        creator.setDisplayName(symbol + " " + player.getPlayerName())
                .addLore("&7- Xếp hạng: " + orderColor + order,
                        "&7- Điểm: &b" + player.getEloPoint(),
                        "&7- Cấp bậc: " + player.getRank().getDisplayName());
        NBTItem nbt = new NBTItem(creator.build());
        nbt.setUUID("uuid", player.getUniqueId());
        return new InventoryElement(nbt.getItem()) {
            @Override
            public void onClick(InventoryClickEvent event) {
                event.setCancelled(true);
            }
        };
    }

    @Override
    public void onIndexComplete() {
        for (int i = 0; i < getItemSlots().length; i++) {
            ItemStack item = getInventory().getItem(i);
            if(item == null || item.getType() != Material.PLAYER_HEAD)
                continue;
            NBTItem nbt = new NBTItem(item);
            UUID uuid = nbt.getUUID("uuid");
            ThreadWorker.THREAD.submit(() -> {
               ItemCreator creator = new ItemCreator(item);
               creator.setTexture(Bukkit.getOfflinePlayer(uuid));
            });
        }
    }

    private int getOrder(SPPlayer player) {
        for (int i = 0; i < getObjects().size(); i++) {
            SPPlayer spPlayer = getObjects().get(i);
            if(spPlayer.getUniqueId().equals(player.getUniqueId()))
                return i+1;
        }
        return 0;
    }

}
