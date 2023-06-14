package com.github.tezvn.starpvp.core.utils;

import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class ItemCreator implements Cloneable {

    private final ItemStack item;

    private ItemMeta meta;

    private List<String> lore;

    private String texture;

    public ItemCreator(ItemStack item) {
        this.item = item;
        if (item.getType() != Material.AIR) {
            this.meta = item.getItemMeta();
            this.lore = this.meta.hasLore() ? this.meta.getLore() : Lists.newArrayList();
        }
    }

    public ItemCreator(Material material) {
        this.item = new ItemStack(material);
        if (material != Material.AIR) {
            this.meta = this.item.getItemMeta();
            this.lore = this.meta.hasLore() ? this.meta.getLore() : Lists.newArrayList();
        }
    }

    public ItemCreator setTexture(String texture) {
        if (!(this.meta instanceof SkullMeta))
            return this;
        this.texture = texture;
        return this;
    }

    public ItemCreator setTexture(OfflinePlayer player) {
        String[] value = getSkin(player.getName());
        if (value == null)
            return this;
        GameProfile profile = new GameProfile(player.getUniqueId(), player.getName());
        profile.getProperties().put("textures", new Property("textures", value[0], value[1]));
        applyGameProfile(profile);
        return this;
    }

    private String[] getSkin(String name) {
        try {
            URL url_0 = new URL("https://api.mojang.com/users/profiles/minecraft/" + name);
            InputStreamReader reader_0 = new InputStreamReader(url_0.openStream());
            String uuid = new JsonParser().parse(reader_0).getAsJsonObject().get("id").getAsString();

            URL url_1 = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid + "?unsigned=false");
            InputStreamReader reader_1 = new InputStreamReader(url_1.openStream());
            JsonObject textureProperty = new JsonParser().parse(reader_1).getAsJsonObject().get("properties").getAsJsonArray().get(0).getAsJsonObject();
            String texture = textureProperty.get("value").getAsString();
            String signature = textureProperty.get("signature").getAsString();

            return new String[]{texture, signature};
        } catch (IOException e) {
            System.err.println("Could not get skin data from session servers!");
            e.printStackTrace();
            return null;
        }
    }

    public ItemCreator setDisplayName(String displayName) {
        this.meta.setDisplayName(displayName.replace("&", "§"));
        return this;
    }

    public ItemCreator setAmount(int amount) {
        this.item.setAmount(Math.max(0, Math.min(64, amount)));
        return this;
    }

    public ItemCreator addLore(String... str) {
        for (String s : str) {
            this.lore.add(s.replace("&", "§"));
        }
        return this;
    }

    public ItemCreator removeLore(int index) {
        if (index < 0 || index >= lore.size())
            return this;
        this.lore.remove(index);
        return this;
    }

    public ItemCreator clearLore() {
        this.lore.clear();
        return this;
    }

    public ItemCreator setLore(List<String> lore) {
        this.lore.clear();
        this.lore.addAll(lore.stream().map(e -> e.replace("&", "§"))
                .collect(Collectors.toList()));
        return this;
    }

    public ItemCreator setLore(int index, String str) {
        if (index < 0 || index >= lore.size())
            return this;
        this.lore.set(index, str.replace("&", "§"));
        return this;
    }

    public ItemCreator addEnchant(Enchantment enchantment, int level, boolean ignoreRestriction) {
        if (level < 1)
            level = 1;
        this.meta.addEnchant(enchantment, level, ignoreRestriction);
        return this;
    }

    public ItemCreator removeEnchant(Enchantment enchantment) {
        this.meta.removeEnchant(enchantment);
        return this;
    }

    public ItemCreator setUnbreakable(boolean unbreakable) {
        if (getVersionNumber() < 11)
            return this;
        try {
            meta.getClass().getDeclaredMethod("setUnbreakable", boolean.class).invoke(meta, unbreakable);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return this;
    }

    public ItemCreator setCustomModelData(int modelData) {
        if (getVersionNumber() < 14)
            return this;
        try {
            meta.getClass().getDeclaredMethod("setCustomModelData", int.class).invoke(meta, modelData);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return this;
    }

    public ItemCreator addFlag(ItemFlag... flags) {
        this.meta.addItemFlags(flags);
        return this;
    }

    public ItemCreator removeFlag(ItemFlag... flags) {
        this.meta.removeItemFlags(flags);
        return this;
    }

    public ItemCreator setGlow(boolean glow) {
        if (glow) {
            addEnchant(Enchantment.DURABILITY, 1, true);
            addFlag(ItemFlag.HIDE_ENCHANTS);
        } else {
            removeEnchant(Enchantment.DURABILITY);
            removeFlag(ItemFlag.HIDE_ENCHANTS);
        }
        return this;
    }

    public ItemStack build() {
        return buildItem();
    }

    private ItemStack buildItem() {
        if (this.item.getType() != Material.AIR) {
            if (this.texture != null)
                applyGameProfile(createProfile());
            this.meta.setLore(this.lore);
            this.item.setItemMeta(meta);
        }
        return this.item;
    }

    private void applyGameProfile(GameProfile gameProfile) {
        try {
            Method setProfile = meta.getClass().getDeclaredMethod("setProfile", GameProfile.class);
            setProfile.setAccessible(true);
            setProfile.invoke(meta, gameProfile);
        } catch (Exception e) {
            try {
                Field profile = meta.getClass().getDeclaredField("profile");
                profile.setAccessible(true);
                profile.set(meta, gameProfile);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private GameProfile createProfile() {
        String b64 = urlToBase64(this.texture);
        // random uuid based on the b64 string
        UUID id = new UUID(
                b64.substring(b64.length() - 20).hashCode(),
                b64.substring(b64.length() - 10).hashCode()
        );
        GameProfile profile = new GameProfile(id, "Player");
        profile.getProperties().put("textures", new Property("textures", b64));
        return profile;
    }

    private String urlToBase64(String url) {
        url = "http://textures.minecraft.net/texture/" + url;
        String toEncode = "{\"textures\":{\"SKIN\":{\"url\":\"" + url + "\"}}}";
        return Base64.getEncoder().encodeToString(toEncode.getBytes());
    }

    private String getVersion() {
        return Bukkit.getServer().getClass().getPackage().getName().substring(Bukkit.getServer().getClass().getPackage().getName().lastIndexOf('.') + 1);
    }

    private int getVersionNumber() {
        return Integer.parseInt(getVersion().split("_")[1]);
    }

    @Override
    public ItemCreator clone() {
        try {
            return (ItemCreator) super.clone();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
