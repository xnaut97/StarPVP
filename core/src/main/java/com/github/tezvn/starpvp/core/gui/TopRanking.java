package com.github.tezvn.starpvp.core.gui;

import com.cryptomorin.xseries.XMaterial;
import com.github.tezvn.starpvp.api.SPPlugin;
import com.github.tezvn.starpvp.api.player.SPPlayer;
import com.github.tezvn.starpvp.core.utils.BaseMenu;
import com.github.tezvn.starpvp.core.utils.ItemCreator;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class TopRanking extends BaseMenu.PaginationMenu<SPPlayer> {

    public TopRanking() {
        super(0, 6, "&lBẢNG XẾP HẠNG NGƯỜI CHƠI");
    }

    @Override
    public List<SPPlayer> getObjects() {
        return ((SPPlugin) getPlugin()).getPlayerManager().getPlayers().stream()
                .sorted(Comparator.comparing(SPPlayer::getStarPoint, Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }

    @Override
    public InventoryElement getObjectItem(SPPlayer player) {
        ItemCreator creator = new ItemCreator(Objects.requireNonNull(
                XMaterial.PLAYER_HEAD.parseItem()));
        creator.setDisplayName("&6&l" + Objects.requireNonNull(player.getPlayer().getName()))
                .addLore("&7- Xếp hạng: &d" + getOrder(player),
                        "&7- SP: &b" + player.getStarPoint())
                .setTexture(player.getPlayer());
        return new InventoryElement(creator.build()) {
            @Override
            public void onClick(InventoryClickEvent event) {
                event.setCancelled(true);
            }
        };
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
