package com.github.tezvn.starpvp.core.utils;

import com.cryptomorin.xseries.ReflectionUtils;
import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public abstract class BaseMenu implements InventoryHolder {

    private static Plugin plugin;

    private final Inventory inventory;

    private final List<UUID> players;

    private final String title;

    private String lastTitle;

    private InventoryElement[] elements;

    public BaseMenu(int size, String title) {
        size = 9 * Math.max(1, Math.min(6, size));
        if (title == null)
            title = "";
        this.inventory = Bukkit.createInventory(this, size, title.replace("&", "§"));
        this.players = Lists.newArrayList();
        this.elements = new InventoryElement[size];
        this.title = title;
    }

    public static void forceCloseAll() {
        Bukkit.getOnlinePlayers().forEach(player -> {
            Inventory inventory = player.getOpenInventory().getTopInventory();
            if (inventory.getHolder() instanceof BaseMenu)
                player.closeInventory();
        });
    }

    private static String getVersion() {
        return Bukkit.getServer().getClass().getPackage().getName()
                .substring(Bukkit.getServer().getClass().getPackage().getName().lastIndexOf('.') + 1);
    }

    private static int getVersionNumber() {
        return Integer.parseInt(getVersion().split("_")[1]);
    }

    public static void register(Plugin instance) {
        plugin = instance;
        Bukkit.getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onClick(InventoryClickEvent e) {
                if (e.getCurrentItem() == null)
                    return;
                Inventory inv = e.getClickedInventory();
                try {
                    if (inv.getHolder() != null && inv.getHolder() instanceof BaseMenu)
                        ((BaseMenu) inv.getHolder()).onClick(e);
                } catch (Exception e2) {
                    e2.printStackTrace();
                }
            }
        }, plugin);

        Bukkit.getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onClose(InventoryCloseEvent e) {
                Inventory inv = e.getInventory();
                try {
                    if (inv.getHolder() != null && inv.getHolder() instanceof BaseMenu)
                        ((BaseMenu) inv.getHolder()).onClose(e);
                } catch (Exception e2) {
                    e2.printStackTrace();
                }
            }
        }, plugin);

        Bukkit.getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onOpen(InventoryOpenEvent e) {
                Inventory inv = e.getInventory();
                try {
                    if (inv.getHolder() != null && inv.getHolder() instanceof BaseMenu)
                        ((BaseMenu) inv.getHolder()).onOpen(e);
                } catch (Exception e2) {
                    e2.printStackTrace();
                }
            }
        }, plugin);
    }

    public static Plugin getPlugin() {
        return plugin;
    }

    public InventoryElement[] getElements() {
        return elements;
    }

    public void pushElements(InventoryElement[] elements) {
        this.elements = elements;
    }

    public String getTitle() {
        return this.title;
    }

    @NotNull
    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setTitle(Player player, String title, boolean async, long ticks) {
        setTitle(player, title);
        BukkitRunnable runnable = new BukkitRunnable() {
            @Override
            public void run() {
                setTitle(player, getTitle());
            }
        };
        if (async)
            runnable.runTaskLaterAsynchronously(plugin, ticks);
        else
            runnable.runTaskLater(plugin, ticks);
    }

    public void setTitle(Player player, String title) {
        InventoryUpdate.updateInventory(getPlugin(), player, title);
    }

    public void onClick(InventoryClickEvent event) {
        InventoryElement element = getElements()[event.getSlot()];
        if (element == null)
            return;
        element.onClick(event);
        element.sound.play((Player) event.getWhoClicked());
    }

    public void onOpen(InventoryOpenEvent event) {
        this.players.add(event.getPlayer().getUniqueId());
        onOpenActions(event);
    }

    public void onClose(InventoryCloseEvent event) {
        this.players.remove(event.getPlayer().getUniqueId());
//        for (InventoryElement element : getElements()) {
//            if(element == null || element.getItem() == null)
//                continue;
//            element.onClose(event);
//        }
        onCloseActions(event);
    }

    /**
     * Push element to specific slot in inventory
     *
     * @param slot    Slot to push element
     * @param element Element to push
     * @return True if success
     * <br>Otherwise false if slot < 0 or slot > inventory size
     */
    public boolean pushElement(int slot, InventoryElement element) {
        if (slot < 0 || slot > getInventory().getSize())
            return false;
        getInventory().setItem(slot, element.getItem());
        getElements()[slot] = element;
        return true;
    }

    /**
     * Open current inventory for player
     *
     * @param player Player to open
     * @return True if success
     * <br>Otherwise false if player is offline
     */
    public boolean open(Player player) {
        return open(player, false, this);
    }

    /**
     * Open other inventory for player
     *
     * @param player    Player to open
     * @param inventory Inventory to open
     * @return True if success
     * <br>Otherwise false if player is offline
     */
    public boolean open(Player player, boolean async, BaseMenu inventory) {
        if (!player.isOnline())
            return false;
        BukkitRunnable runnable = new BukkitRunnable() {
            @Override
            public void run() {
                player.openInventory(inventory.getInventory());
            }
        };
        if (async)
            runnable.runTaskAsynchronously(plugin);
        else
            runnable.runTask(plugin);
        return true;
    }

    public boolean close(Player player) {
        return close(player, false);
    }

    /**
     * Close inventory for player
     *
     * @param player Player to close
     * @return True if success
     * <br>Otherwise false if player is opening other inventory
     */
    public boolean close(Player player, boolean async) {
        if (!this.players.contains(player.getUniqueId()))
            return false;
        if (async) {
            player.closeInventory();
        } else {
            new BukkitRunnable() {
                @Override
                public void run() {
                    player.closeInventory();
                }
            }.runTask(plugin);
        }
        this.players.remove(player.getUniqueId());
        return true;
    }

    /**
     * Force close for all player is opening this inventory
     */
    public void closeAll(boolean async) {
        for (UUID uuid : this.players) {
            OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
            if (!player.isOnline())
                continue;
            close(player.getPlayer(), async);
        }
        this.players.clear();
    }

    public boolean recalculate(int page, int listSize, int slots) {
        return page != 0 && listSize <= page * slots;
    }

    public boolean isMax(int page, int listSize, int slots) {
        return (page + 1) * slots >= listSize;
    }

    /**
     * Triggered when open inventory
     */
    public abstract void onOpenActions(InventoryOpenEvent event);

    /**
     * Triggered when close inventory
     *
     * @param event
     */
    public abstract void onCloseActions(InventoryCloseEvent event);

    public abstract static class RunnableMenu extends BaseMenu {

        private final AtomicBoolean running;

        private final boolean async;

        private final int ticks;

        public RunnableMenu(int size, String title, int ticks, boolean async) {
            super(size, title);
            this.ticks = ticks;
            this.async = async;
            this.running = new AtomicBoolean(true);
            init();
        }

        public boolean isAsync() {
            return async;
        }

        public int getTicks() {
            return ticks;
        }

        private void init() {
            BukkitRunnable runnable = new BukkitRunnable() {
                @Override
                public void run() {
                    if (!running.get()) {
                        cancel();
                        onCancel();
                        return;
                    }
                    onTick();
                }
            };
            if (async)
                runnable.runTaskTimerAsynchronously(plugin, 0, ticks);
            else
                runnable.runTaskTimer(plugin, 0, ticks);
        }

        @Override
        public final void onOpen(InventoryOpenEvent event) {
            running.set(true);
            onOpenActions(event);
        }

        @Override
        public void onClose(InventoryCloseEvent event) {
            running.set(false);
            onCloseActions(event);
        }

        public abstract void onTick();

        public void onCancel() {
        }

    }

    @Deprecated
    public abstract static class PagedMenu extends BaseMenu {

        private final int size;
        protected ItemStack background;
        protected ItemStack toolBar;
        protected int[] itemSlots;
        private int page;

        public PagedMenu(int page, int row, String title) {
            super(9 * row, title);
            this.size = 9 * row;
            this.page = page;
            this.itemSlots = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8,
                    9, 10, 11, 12, 13, 14, 15, 16, 17,
                    18, 19, 20, 21, 22, 23, 24, 25, 26,
                    27, 28, 29, 30, 31, 32, 33, 34, 35,
                    36, 37, 38, 39, 40, 41, 42, 43, 44};
            setToolBar(XMaterial.BLACK_STAINED_GLASS_PANE.parseItem());
        }

        public int getPage() {
            return page;
        }

        public void setPage(int page) {
            this.page = page;
        }

        public void setToolBar(ItemStack item) {
            this.toolBar = item;
            for (int i = size - 9; i < size; i++) {
                pushElement(i, new InventoryElement(item, " ") {
                    @Override
                    public void onClick(InventoryClickEvent event) {
                        onToolBarClick(event);
                    }
                });
            }
        }

        public void onToolBarClick(InventoryClickEvent event) {
            event.setCancelled(true);
        }

        public final void setNextButton(int slot, int listSize, String name, String[] lore, Runnable onFailure) {
            boolean isMax = isMax(getPage(), listSize, 45);
            int max = listSize / 45;
            if (!isMax) {
                ItemStack item = new ItemCreator(XMaterial.PLAYER_HEAD.parseItem())
                        .setDisplayName(name)
                        .addLore(lore)
                        .setTexture("4ae29422db4047efdb9bac2cdae5a0719eb772fccc88a66d912320b343c341")
                        .build();
                pushElement(slot, new InventoryElement(item) {
                    @Override
                    public void onClick(InventoryClickEvent event) {
                        event.setCancelled(true);
                        setPage(Math.min(max, getPage() + 1));
                        onNextPage(event);
                    }
                });
            } else {
                if (onFailure != null)
                    onFailure.run();
            }
        }

        public abstract void onNextPage(InventoryClickEvent event);

        public final void setPreviousButton(int slot, String name, String[] lore, Runnable onFailure) {
            if (getPage() > 0) {
                ItemStack item = new ItemCreator(XMaterial.PLAYER_HEAD.parseItem())
                        .setDisplayName(name)
                        .addLore(lore)
                        .setTexture("9945491898496b136ffaf82ed398a54568289a331015a64c843a39c0cbf357f7")
                        .build();
                pushElement(slot, new InventoryElement(item) {
                    @Override
                    public void onClick(InventoryClickEvent event) {
                        event.setCancelled(true);
                        setPage(Math.max(0, getPage() - 1));
                        onPreviousPage(event);
                    }
                });
            } else {
                if (onFailure != null)
                    onFailure.run();
            }
        }

        public abstract void onPreviousPage(InventoryClickEvent event);

        public final void setPageInfo(int slot, String name) {
            ItemStack item = new ItemCreator(Objects.requireNonNull(XMaterial.PAPER.parseItem()))
                    .setDisplayName(name).build();
            pushElement(slot, new InventoryElement(item) {
                @Override
                public void onClick(InventoryClickEvent event) {
                    event.setCancelled(true);
                    onPageClick(event);
                }
            });
        }

        public void onPageClick(InventoryClickEvent event) {
        }

        public int[] getItemSlots() {
            return itemSlots;
        }

    }

    public abstract static class BoxedMenu extends PagedMenu {

        private final int[] borderSlots = {36, 27, 18, 9,
                0, 1, 2, 3, 4, 5, 6, 7, 8,
                17, 26, 35, 44};
        private ItemStack boxedMaterial;


        public BoxedMenu(int page, int row, String title) {
            super(page, row, title);
            this.itemSlots = new int[]{10, 11, 12, 13, 14, 15, 16,
                    19, 20, 21, 22, 23, 24, 25,
                    28, 29, 30, 31, 32, 33, 34,
                    37, 38, 39, 40, 41, 42, 43};
            setBorder(XMaterial.BLACK_STAINED_GLASS_PANE.parseItem());
            setToolBar(XMaterial.BLACK_STAINED_GLASS_PANE.parseItem());
        }

        public void setBorder(ItemStack item) {
            this.boxedMaterial = item;
            for (int i : getBorderSlots()) {
                pushElement(i, new InventoryElement(item, " ") {
                    @Override
                    public void onClick(InventoryClickEvent event) {
                        onBorderClick(event);
                    }
                });
            }
        }

        public void onBorderClick(InventoryClickEvent event) {
        }

        public int[] getBorderSlots() {
            return borderSlots;
        }
    }

    public abstract static class TradingMenu extends BaseMenu {

        private final Player sender, target;
        private final int[] leftSlots = {
                0, 1, 2, 3,
                9, 10, 11, 12,
                18, 19, 20, 21,
                27, 28, 29, 30,
                36, 37, 38, 39
        };
        private final int[] rightSlots = {
                5, 6, 7, 8,
                14, 15, 16, 17,
                23, 24, 26, 26,
                32, 33, 34, 35,
                41, 42, 43, 44
        };
        private final int[] leftToolBar = {45, 46, 47, 48};
        private final int[] rightToolBar = {50, 51, 52, 53};
        private final int[] borderSlots = {4, 13, 22, 31, 40, 45, 46, 47, 48, 49, 50, 51, 52, 53};
        private TradingMenu opposite;

        public TradingMenu(Player sender, Player target, int size, String title) {
            super(size, title);
            this.sender = sender;
            this.target = target;
        }

        @Override
        public final void onClose(InventoryCloseEvent event) {

            onCloseActions(event);
        }
    }

    public static abstract class PaginationMenu<T> extends BaseMenu {

        private final int[] borderSlots =
                {36, 27, 18, 9,
                        0, 1, 2, 3, 4, 5, 6, 7, 8,
                        17, 26, 35, 44,
                        45, 46, 47, 48, 49, 50, 51, 52, 53};
        private final int[] itemSlots =
                {10, 11, 12, 13, 14, 15, 16,
                        19, 20, 21, 22, 23, 24, 25,
                        28, 29, 30, 31, 32, 33, 34,
                        37, 38, 39, 40, 41, 42, 43};
        private int page;

        private Player player;

        private boolean updateInstantly = true;

        private long updateTick = 1;

        private BukkitRunnable runnable;

        private boolean exit;

        public PaginationMenu(int page, int row, String title) {
            super(9 * row, title);
            this.page = page;
            setup();
        }

        private void setupBackground() {
            for (int slot : borderSlots) {
                pushElement(slot, new InventoryElement(getBorderItem(), " ") {
                    @Override
                    public void onClick(InventoryClickEvent event) {
                        event.setCancelled(true);
                    }
                });
            }
            for (int slot : itemSlots) {
                pushElement(slot, new InventoryElement(getPlaceholderItem(), " ") {
                    @Override
                    public void onClick(InventoryClickEvent event) {
                        event.setCancelled(true);
                    }
                });
            }
        }

        private void setup() {
            setupBackground();
            this.runnable = new BukkitRunnable() {
                private long count = 0;

                @Override
                public void run() {
                    count++;
                    if (count == Long.MAX_VALUE)
                        count = 0;
                    if (count % updateTick > 0)
                        return;
                    if (count / updateTick > 1 && !updateInstantly)
                        return;
                    update();
                }
            };
        }

        public void update() {
            List<T> validated = getObjects().stream()
                    .filter(this::onValidate).collect(Collectors.toList());
            for (int i = 0; i < getItemSlots().length; i++) {
                int index = (getPage() + 1) * i;
                if (index >= validated.size()) {
                    pushElement(getItemSlots()[i], new InventoryElement(fillOtherSlotWhenFull() == null
                            ? new ItemStack(Material.AIR) : fillOtherSlotWhenFull(), " ") {
                        @Override
                        public void onClick(InventoryClickEvent event) {
                            event.setCancelled(true);
                        }
                    });
                    continue;
                }
                T object = validated.get(index);

                pushElement(getItemSlots()[i], getObjectItem(object));
            }

            pushElement(getPreviousButtonSlot(), getPage() == 0
                    ? new InventoryElement(getBorderItem(), " ") {
                @Override
                public void onClick(InventoryClickEvent event) {
                    event.setCancelled(true);
                }
            }
                    : new InventoryElement(getPreviousButton()) {
                @Override
                public void onClick(InventoryClickEvent event) {
                    event.setCancelled(true);
                    onPreviousButtonClick(event);
                    previous();
                }
            });

            pushElement(getInfoButtonSlot(), new InventoryElement(getInfoButton()) {
                @Override
                public void onClick(InventoryClickEvent event) {
                    event.setCancelled(true);
                    onInfoButtonClick(event);
                }
            });

            boolean reachMax = getPage() == getMaxPage();
            pushElement(getNextButtonSlot(), reachMax
                    ? new InventoryElement(getBorderItem(), " ") {
                @Override
                public void onClick(InventoryClickEvent event) {
                    event.setCancelled(true);
                }
            }
                    : new InventoryElement(getNextButton()) {
                @Override
                public void onClick(InventoryClickEvent event) {
                    event.setCancelled(true);
                    onNextButtonClick(event);
                    next();
                }
            });
            onIndexComplete();
        }

        @Override
        public void onOpen(InventoryOpenEvent event) {
            super.onOpen(event);
        }

        public Player getPlayer() {
            return player;
        }

        public int getPage() {
            return page;
        }

        public void setPage(int page) {
            this.page = Math.max(0, Math.min(getMaxPage(), page));
        }

        public void next() {
            this.page = Math.min(page + 1, getMaxPage());
        }

        public void previous() {
            this.page = Math.max(page - 1, 0);
        }

        public int getMaxPage() {
            return (int) getObjects().stream().filter(this::onValidate).count()
                    / ((getPage() + 1) * getItemSlots().length);
        }

        public int[] getBorderSlots() {
            return borderSlots;
        }

        public ItemStack getBorderItem() {
            return XMaterial.BLACK_STAINED_GLASS_PANE.parseItem();
        }

        public int[] getItemSlots() {
            return itemSlots;
        }

        public ItemStack getPlaceholderItem() {
            return XMaterial.GRAY_STAINED_GLASS_PANE.parseItem();
        }

        @Override
        public void onOpenActions(InventoryOpenEvent event) {
            this.player = (Player) event.getPlayer();
            if (this.runnable != null)
                this.runnable.runTaskTimerAsynchronously(plugin, 0, 1);
        }

        @Override
        public void onCloseActions(InventoryCloseEvent event) {
            Player player = (Player) event.getPlayer();
            if (getPlayer() != null && player.getUniqueId().equals(getPlayer().getUniqueId()))
                exit = true;
            if (this.runnable != null)
                this.runnable.cancel();
        }

        public int getPreviousButtonSlot() {
            return 48;
        }

        public ItemStack getPreviousButton() {
            return new ItemCreator(Objects.requireNonNull(XMaterial.PLAYER_HEAD.parseItem()))
                    .setDisplayName("&e« TRANG TRƯỚC")
                    .addLore("&7Trở về trang &e" + getPage())
                    .setTexture("9945491898496b136ffaf82ed398a54568289a331015a64c843a39c0cbf357f7")
                    .build();
        }

        public void onPreviousButtonClick(InventoryClickEvent event) {
        }

        public int getInfoButtonSlot() {
            return 49;
        }

        public ItemStack getInfoButton() {
            return new ItemCreator(Objects.requireNonNull(XMaterial.PAPER.parseItem()))
                    .setDisplayName("&eTrang " + (getPage() + 1)).build();
        }

        public void onInfoButtonClick(InventoryClickEvent event) {
        }

        public int getNextButtonSlot() {
            return 50;
        }

        public ItemStack getNextButton() {
            return new ItemCreator(Objects.requireNonNull(XMaterial.PLAYER_HEAD.parseItem()))
                    .setDisplayName("&eTRANG KẾ »")
                    .addLore("&7Qua trang &e" + (getPage() + 2))
                    .setTexture("4ae29422db4047efdb9bac2cdae5a0719eb772fccc88a66d912320b343c341")
                    .build();
        }

        public void setUpdateInstantly(boolean updateInstantly) {
            this.updateInstantly = updateInstantly;
        }

        public boolean isUpdateInstantly() {
            return updateInstantly;
        }

        public long getUpdateTick() {
            return updateTick;
        }

        public void setUpdateTick(long updateTick) {
            this.updateTick = updateTick;
        }

        public void onNextButtonClick(InventoryClickEvent event) {
        }

        public boolean onValidate(T object) {
            return true;
        }

        public boolean isExit() {
            return exit;
        }

        public abstract List<T> getObjects();

        public abstract InventoryElement getObjectItem(T object);

        public ItemStack fillOtherSlotWhenFull() {
            return null;
        }

        public void onIndexComplete() {}
    }

    public static class InventoryElement {

        private final ItemStack item;

        private ElementSound sound = ElementSound.useDefault();

        public InventoryElement(Material item, int amount) {
            this.item = new ItemStack(item, amount);
        }

        public InventoryElement(Material item) {
            this.item = new ItemStack(item);
        }

        public InventoryElement(ItemStack item, String name) {
            this.item = item;
            ItemMeta im = this.item.getItemMeta();
            if (im != null) {
                im.setDisplayName(name);
                this.item.setItemMeta(im);
            }
        }

        public InventoryElement(Material item, String name) {
            this.item = new ItemStack(item);
            ItemMeta im = this.item.getItemMeta();
            if (im != null) {
                im.setDisplayName(name);
                this.item.setItemMeta(im);
            }
        }

        public InventoryElement(ItemStack item) {
            this.item = item;
        }

        public void onClick(InventoryClickEvent event) {
        }

        public void onClose(InventoryCloseEvent event) {
        }

        public ItemStack getItem() {
            return this.item;
        }

        public InventoryElement setSound(XSound sound) {
            return setSound(sound, 1, 1);
        }

        public InventoryElement setSound(XSound sound, float volume, float pitch) {
            this.sound = new ElementSound(sound, volume, pitch);
            return this;
        }

    }

    private static final class ElementSound {

        private XSound sound = XSound.UI_BUTTON_CLICK;

        private float volume = 1;

        private float pitch = 1;

        private ElementSound() {

        }

        public ElementSound(XSound sound, float volume, float pitch) {
            this.sound = sound;
            this.volume = volume;
            this.pitch = pitch;
        }

        public ElementSound(Sound sound, float volume, float pitch) {
            try {
                this.sound = XSound.matchXSound(sound);
            } catch (Exception ignored) {

            }
            this.volume = volume;
            this.pitch = pitch;
        }

        public static ElementSound useDefault() {
            return new ElementSound();
        }

        public void play(Player player) {
            this.sound.play(player, volume, pitch);
        }

    }

    @SuppressWarnings("ConstantConditions")
    private static final class InventoryUpdate {

        // Classes.
        private final static Class<?> CRAFT_PLAYER;
        private final static Class<?> CHAT_MESSAGE;
        private final static Class<?> PACKET_PLAY_OUT_OPEN_WINDOW;
        private final static Class<?> I_CHAT_BASE_COMPONENT;
        private final static Class<?> CONTAINER;
        private final static Class<?> CONTAINERS;
        private final static Class<?> ENTITY_PLAYER;
        private final static Class<?> I_CHAT_MUTABLE_COMPONENT;

        // Methods.
        private final static MethodHandle getHandle;
        private final static MethodHandle getBukkitView;
        private final static MethodHandle literal;

        // Constructors.
        private final static MethodHandle chatMessage;
        private final static MethodHandle packetPlayOutOpenWindow;

        // Fields.
        private final static MethodHandle activeContainer;
        private final static MethodHandle windowId;

        // Methods factory.
        private final static MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

        private final static Set<String> UNOPENABLES = Sets.newHashSet("CRAFTING", "CREATIVE", "PLAYER");

        static {
            boolean supports19 = ReflectionUtils.supports(19);

            // Initialize classes.
            CRAFT_PLAYER = ReflectionUtils.getCraftClass("entity.CraftPlayer");
            CHAT_MESSAGE = supports19 ? null : ReflectionUtils.getNMSClass("network.chat", "ChatMessage");
            PACKET_PLAY_OUT_OPEN_WINDOW = ReflectionUtils.getNMSClass("network.protocol.game", "PacketPlayOutOpenWindow");
            I_CHAT_BASE_COMPONENT = ReflectionUtils.getNMSClass("network.chat", "IChatBaseComponent");
            // Check if we use containers, otherwise, can throw errors on older versions.
            CONTAINERS = useContainers() ? ReflectionUtils.getNMSClass("world.inventory", "Containers") : null;
            ENTITY_PLAYER = ReflectionUtils.getNMSClass("server.level", "EntityPlayer");
            CONTAINER = ReflectionUtils.getNMSClass("world.inventory", "Container");
            I_CHAT_MUTABLE_COMPONENT = supports19 ? ReflectionUtils.getNMSClass("network.chat", "IChatMutableComponent") : null;

            // Initialize methods.
            getHandle = getMethod(CRAFT_PLAYER, "getHandle", MethodType.methodType(ENTITY_PLAYER));
            getBukkitView = getMethod(CONTAINER, "getBukkitView", MethodType.methodType(InventoryView.class));
            literal = supports19 ? getMethod(I_CHAT_BASE_COMPONENT, "b", MethodType.methodType(I_CHAT_MUTABLE_COMPONENT, String.class), true) : null;

            // Initialize constructors.
            chatMessage = supports19 ? null : getConstructor(CHAT_MESSAGE, String.class, String[].class);
            System.out.println(chatMessage);
            packetPlayOutOpenWindow =
                    (useContainers()) ?
                            getConstructor(PACKET_PLAY_OUT_OPEN_WINDOW, int.class, CONTAINERS, I_CHAT_BASE_COMPONENT) :
                            // Older versions use String instead of Containers, and require an int for the inventory size.
                            getConstructor(PACKET_PLAY_OUT_OPEN_WINDOW, int.class, String.class, I_CHAT_BASE_COMPONENT, int.class);

            // Initialize fields.
            activeContainer = getField(ENTITY_PLAYER, CONTAINER, "activeContainer", "bV", "bW", "bU", "containerMenu");
            windowId = getField(CONTAINER, int.class, "windowId", "j", "containerId");
        }

        /**
         * Update the player inventory, so you can change the title.
         *
         * @param player   whose inventory will be updated.
         * @param newTitle the new title for the inventory.
         */

        public static void updateInventory(Plugin plugin, Player player, String newTitle) {
            Preconditions.checkArgument(player != null, "Cannot update inventory to null player.");

            try {
                // Get EntityPlayer from CraftPlayer.
                Object craftPlayer = CRAFT_PLAYER.cast(player);
                Object entityPlayer = getHandle.invoke(craftPlayer);

                if (newTitle != null && newTitle.length() > 32) {
                    newTitle = newTitle.substring(0, 32);
                } else if (newTitle == null) newTitle = "";

                // Create new title.
                Object title;
                if (ReflectionUtils.supports(19)) {
                    title = literal.invoke(newTitle);
                } else {
                    title = chatMessage.invoke(newTitle, new String[]{});
                }

                // Get activeContainer from EntityPlayer.
                Object activeContainer = InventoryUpdate.activeContainer.invoke(entityPlayer);

                // Get windowId from activeContainer.
                Integer windowId = (Integer) InventoryUpdate.windowId.invoke(activeContainer);

                // Get InventoryView from activeContainer.
                Object bukkitView = getBukkitView.invoke(activeContainer);
                if (!(bukkitView instanceof InventoryView)) return;

                InventoryView view = (InventoryView) bukkitView;
                InventoryType type = view.getTopInventory().getType();

                // Workbenchs and anvils can change their title since 1.14.
                if ((type == InventoryType.WORKBENCH || type == InventoryType.ANVIL) && !useContainers()) return;

                // You can't reopen crafting, creative and player inventory.
                if (UNOPENABLES.contains(type.name())) return;

                int size = view.getTopInventory().getSize();

                // Get container, check is not null.
                Containers container = Containers.getType(type, size);
                if (container == null) return;

                // If the container was added in a newer version than the current, return.
                if (container.getContainerVersion() > ReflectionUtils.MINOR_NUMBER && useContainers()) {
                    Bukkit.getLogger().warning(String.format(
                            "[%s] This container doesn't work on your current version.",
                            plugin.getDescription().getName()));
                    return;
                }

                Object object;
                // Dispensers and droppers use the same container, but in previous versions, use a diferrent minecraft name.
                if (!useContainers() && container == Containers.GENERIC_3X3) {
                    object = "minecraft:" + type.name().toLowerCase();
                } else {
                    object = container.getObject();
                }

                // Create packet.
                Object packet =
                        (useContainers()) ?
                                packetPlayOutOpenWindow.invoke(windowId, object, title) :
                                packetPlayOutOpenWindow.invoke(windowId, object, title, size);

                // Send packet sync.
                ReflectionUtils.sendPacketSync(player, packet);

                // Update inventory.
                player.updateInventory();
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }

        private static MethodHandle getField(Class<?> refc, Class<?> instc, String name, String... extraNames) {
            MethodHandle handle = getFieldHandle(refc, instc, name);
            if (handle != null) return handle;

            if (extraNames != null && extraNames.length > 0) {
                if (extraNames.length == 1) return getField(refc, instc, extraNames[0]);
                return getField(refc, instc, extraNames[0], removeFirst(extraNames));
            }

            return null;
        }

        private static String[] removeFirst(String[] array) {
            int length = array.length;

            String[] result = new String[length - 1];
            System.arraycopy(array, 1, result, 0, length - 1);

            return result;
        }

        private static MethodHandle getFieldHandle(Class<?> refc, Class<?> inscofc, String name) {
            try {
                for (Field field : refc.getFields()) {
                    field.setAccessible(true);

                    if (!field.getName().equalsIgnoreCase(name)) continue;

                    if (field.getType().isInstance(inscofc) || field.getType().isAssignableFrom(inscofc)) {
                        return LOOKUP.unreflectGetter(field);
                    }
                }
                return null;
            } catch (ReflectiveOperationException ignored) {
                return null;
            }
        }

        private static MethodHandle getConstructor(Class<?> refc, Class<?>... types) {
            try {
                Constructor<?> constructor = refc.getDeclaredConstructor(types);
                constructor.setAccessible(true);
                return LOOKUP.unreflectConstructor(constructor);
            } catch (ReflectiveOperationException exception) {
                exception.printStackTrace();
                return null;
            }
        }

        private static MethodHandle getMethod(Class<?> refc, String name, MethodType type) {
            return getMethod(refc, name, type, false);
        }

        private static MethodHandle getMethod(Class<?> refc, String name, MethodType type, boolean isStatic) {
            try {
                if (isStatic) return LOOKUP.findStatic(refc, name, type);
                return LOOKUP.findVirtual(refc, name, type);
            } catch (ReflectiveOperationException exception) {
                exception.printStackTrace();
                return null;
            }
        }

        /**
         * Containers were added in 1.14, a String were used in previous versions.
         *
         * @return whether to use containers.
         */
        private static boolean useContainers() {
            return ReflectionUtils.MINOR_NUMBER > 13;
        }

        /**
         * An enum class for the necessaries containers.
         */
        private enum Containers {
            GENERIC_9X1(14, "minecraft:chest", "CHEST"),
            GENERIC_9X2(14, "minecraft:chest", "CHEST"),
            GENERIC_9X3(14, "minecraft:chest", "CHEST", "ENDER_CHEST", "BARREL"),
            GENERIC_9X4(14, "minecraft:chest", "CHEST"),
            GENERIC_9X5(14, "minecraft:chest", "CHEST"),
            GENERIC_9X6(14, "minecraft:chest", "CHEST"),
            GENERIC_3X3(14, null, "DISPENSER", "DROPPER"),
            ANVIL(14, "minecraft:anvil", "ANVIL"),
            BEACON(14, "minecraft:beacon", "BEACON"),
            BREWING_STAND(14, "minecraft:brewing_stand", "BREWING"),
            ENCHANTMENT(14, "minecraft:enchanting_table", "ENCHANTING"),
            FURNACE(14, "minecraft:furnace", "FURNACE"),
            HOPPER(14, "minecraft:hopper", "HOPPER"),
            MERCHANT(14, "minecraft:villager", "MERCHANT"),
            // For an unknown reason, when updating a shulker box, the size of the inventory get a little bigger.
            SHULKER_BOX(14, "minecraft:blue_shulker_box", "SHULKER_BOX"),

            // Added in 1.14, so only works with containers.
            BLAST_FURNACE(14, null, "BLAST_FURNACE"),
            CRAFTING(14, null, "WORKBENCH"),
            GRINDSTONE(14, null, "GRINDSTONE"),
            LECTERN(14, null, "LECTERN"),
            LOOM(14, null, "LOOM"),
            SMOKER(14, null, "SMOKER"),
            // CARTOGRAPHY in 1.14, CARTOGRAPHY_TABLE in 1.15 & 1.16 (container), handle in getObject().
            CARTOGRAPHY_TABLE(14, null, "CARTOGRAPHY"),
            STONECUTTER(14, null, "STONECUTTER"),

            // Added in 1.14, functional since 1.16.
            SMITHING(16, null, "SMITHING");

            private final static char[] alphabet = "abcdefghijklmnopqrstuvwxyz".toCharArray();
            private final int containerVersion;
            private final String minecraftName;
            private final String[] inventoryTypesNames;

            Containers(int containerVersion, String minecraftName, String... inventoryTypesNames) {
                this.containerVersion = containerVersion;
                this.minecraftName = minecraftName;
                this.inventoryTypesNames = inventoryTypesNames;
            }

            /**
             * Get the container based on the current open inventory of the player.
             *
             * @param type type of inventory.
             * @return the container.
             */
            public static Containers getType(InventoryType type, int size) {
                if (type == InventoryType.CHEST) {
                    return Containers.valueOf("GENERIC_9X" + size / 9);
                }
                for (Containers container : Containers.values()) {
                    for (String bukkitName : container.getInventoryTypesNames()) {
                        if (bukkitName.equalsIgnoreCase(type.toString())) {
                            return container;
                        }
                    }
                }
                return null;
            }

            /**
             * Get the object from the container enum.
             *
             * @return a Containers object if 1.14+, otherwise, a String.
             */
            public Object getObject() {
                try {
                    if (!useContainers()) return getMinecraftName();
                    int version = ReflectionUtils.MINOR_NUMBER;
                    String name = (version == 14 && this == CARTOGRAPHY_TABLE) ? "CARTOGRAPHY" : name();
                    // Since 1.17, containers go from "a" to "x".
                    if (version > 16) name = String.valueOf(alphabet[ordinal()]);
                    Field field = CONTAINERS.getField(name);
                    return field.get(null);
                } catch (ReflectiveOperationException exception) {
                    exception.printStackTrace();
                }
                return null;
            }

            /**
             * Get the version in which the inventory container was added.
             *
             * @return the version.
             */
            public int getContainerVersion() {
                return containerVersion;
            }

            /**
             * Get the name of the inventory from Minecraft for older versions.
             *
             * @return name of the inventory.
             */
            public String getMinecraftName() {
                return minecraftName;
            }

            /**
             * Get inventory types names of the inventory.
             *
             * @return bukkit names.
             */
            public String[] getInventoryTypesNames() {
                return inventoryTypesNames;
            }
        }
    }
}
