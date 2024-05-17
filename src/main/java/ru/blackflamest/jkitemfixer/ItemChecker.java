package ru.blackflamest.jkitemfixer;

import com.comphenix.protocol.wrappers.nbt.NbtBase;
import com.comphenix.protocol.wrappers.nbt.NbtCompound;
import com.comphenix.protocol.wrappers.nbt.NbtList;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.ShulkerBox;
import org.bukkit.block.banner.Pattern;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.*;
import org.bukkit.potion.PotionEffect;
import org.codehaus.plexus.util.Base64;

import java.util.*;
import java.util.stream.Collectors;

public class ItemChecker {

    private HashSet<String> nbt = new HashSet<>();
    private HashSet<Material> tiles = new HashSet<>(Arrays.asList(new Material[] {
        Material.FURNACE, Material.CHEST, Material.TRAPPED_CHEST, Material.DROPPER, Material.DISPENSER, Material.LEGACY_COMMAND_MINECART, Material.HOPPER_MINECART, Material.HOPPER, Material.LEGACY_BREWING_STAND_ITEM, Material.BEACON,
                Material.LEGACY_SIGN, Material.LEGACY_MOB_SPAWNER, Material.NOTE_BLOCK, Material.LEGACY_COMMAND, Material.JUKEBOX }));


    private boolean checkNbt(ItemStack stack, Player p) {
        boolean cheat = false;
        try {
            if (p.hasPermission("jkitemfixer.bypass.nbt"))
                return false;
            Material mat = stack.getType();
            NbtCompound tag = (NbtCompound)MiniNbtFactory.fromItemTag(stack);
            if (tag == null)
                return false;
            if (isCrashItem(stack, tag, mat)) {
                tag.getKeys().clear();
                stack.setAmount(1);
                return true;
            }
            String tagS = tag.toString();
            for (String nbt1 : this.nbt) {
                if (tag.containsKey(nbt1)) {
                    tag.remove(nbt1);
                    cheat = true;
                }
            }
            if (tag.containsKey("BlockEntityTag") && !isShulkerBox(stack, stack) && !needIgnore(stack)) {
                tag.remove("BlockEntityTag");
                cheat = true;
            } else if (mat == Material.WRITTEN_BOOK) {
                tag.getKeys().clear();
                cheat = true;
            } else if (mat.toString().toLowerCase().contains("_spawn_egg") && fixEgg(tag)) {
                cheat = true;
            } else if (mat == Material.ARMOR_STAND && tag.containsKey("EntityTag")) {
                tag.remove("EntityTag");
                cheat = true;
            } else if (mat.toString().toLowerCase().contains("player_head") && stack.getDurability() == 3) {
                if (isCrashSkull(tag))
                    cheat = true;
            } else if (mat.toString().toLowerCase().contains("firework_rocket") && checkFireWork(stack)) {
                cheat = true;
            } else if (mat == Material.LEGACY_BANNER && checkBanner(stack)) {
                cheat = true;
            } else if (isPotion(stack) && tag.containsKey("CustomPotionEffects") && (
                    checkPotion(stack, p) || checkCustomColor(tag.getCompound("CustomPotionEffects")))) {
                cheat = true;
            }
        } catch (Exception exception) {}
        return cheat;
    }

    private boolean isCrashItem(ItemStack stack, NbtCompound tag, Material mat) {
        if (stack.getAmount() < 1 || stack.getAmount() > 64 || tag.getKeys().size() > 20)
            return true;
        int tagL = tag.toString().length();
        if ((mat == Material.NAME_TAG || this.tiles.contains(mat)) && tagL > 600)
            return true;
        if (isShulkerBox(stack, stack))
            return false;
        return (mat == Material.WRITTEN_BOOK) ? ((tagL >= 22000)) : ((tagL >= 13000));
    }



    private boolean checkEnchants(ItemStack stack, Player p) {
        boolean cheat = false;
        if (!p.hasPermission("jkitemfixer.bypass.enchant") && stack.hasItemMeta() && stack.getItemMeta().hasEnchants()) {
            ItemMeta meta = stack.getItemMeta();
            Map<Enchantment, Integer> enchantments = null;
            try {
                enchantments = meta.getEnchants();
            } catch (Exception e) {
                clearData(stack);
                p.updateInventory();
                return true;
            }
            for (Map.Entry<Enchantment, Integer> ench : enchantments.entrySet()) {
                Enchantment enchant = ench.getKey();
                String perm = "jkitemfixer.allow." + stack.getType().toString() + "." + enchant.getName() + "." + ench.getValue();
                if (!enchant.canEnchantItem(stack) && !p.hasPermission(perm)) {
                    meta.removeEnchant(enchant);
                    cheat = true;
                }
                if ((((Integer)ench.getValue()).intValue() > enchant.getMaxLevel() || ((Integer)ench.getValue()).intValue() < 0) && !p.hasPermission(perm)) {
                    meta.removeEnchant(enchant);
                    cheat = true;
                }
            }
            if (cheat)
                stack.setItemMeta(meta);
        }
        return cheat;
    }

    public boolean isShulkerBox(ItemStack stack, ItemStack rootStack) {
        if (stack == null || stack.getType() == Material.AIR)
            return false;
        if (!stack.hasItemMeta())
            return false;
        try {
            if (!(stack.getItemMeta() instanceof BlockStateMeta))
                return false;
        } catch (IllegalArgumentException e) {
            clearData(rootStack);
            return false;
        }
        BlockStateMeta meta = (BlockStateMeta)stack.getItemMeta();
        return meta.getBlockState() instanceof ShulkerBox;
    }

    public void checkShulkerBox(ItemStack stack, Player p) {
        if(stack.getType().toString().toLowerCase().endsWith("_shulker_box")) {
            BlockStateMeta meta = (BlockStateMeta) stack.getItemMeta();
            ShulkerBox box = (ShulkerBox) meta.getBlockState();
            for (ItemStack is : box.getInventory().getContents()) {
                if(is != null) {
                    if (checkEnchants(is, p)) {
                        box.getInventory().clear();
                        meta.setBlockState((BlockState) box);
                        stack.setItemMeta((ItemMeta) meta);
                        return;
                    }
                    if (checkNbt(is, p)) {
                        box.getInventory().clear();
                        meta.setBlockState((BlockState) box);
                        stack.setItemMeta((ItemMeta) meta);
                        return;
                    }
                }
            }
        }
    }

    public boolean isHackedItem(ItemStack stack, Player p) {
        if (stack == null || stack.getType() == Material.AIR)
            return false;
        checkShulkerBox(stack, p);
        checkEnchants(stack, p);
        checkNbt(stack, p);
        return checkEnchants(stack, p);
    }


    private boolean needIgnore(ItemStack stack) {
        Material m = stack.getType();
        return (m.toString().endsWith("_banner"));
    }



    private void clearData(ItemStack stack) {
        NbtCompound tag = (NbtCompound) MiniNbtFactory.fromItemTag(stack);
        if (tag == null)
            return;
        tag.getKeys().clear();
    }


    private boolean checkBanner(ItemStack stack) {
        ItemMeta meta = stack.getItemMeta();
        boolean cheat = false;
        if (meta instanceof BannerMeta) {
            BannerMeta bmeta = (BannerMeta)meta;

            ArrayList<Pattern> patterns = new ArrayList<>();
            for (org.bukkit.block.banner.Pattern pattern : bmeta.getPatterns()) {
                if (pattern.getPattern() == null) {
                    cheat = true;
                    continue;
                }
                patterns.add(pattern);
            }
            if (cheat) {
                bmeta.setPatterns(patterns);
                stack.setItemMeta((ItemMeta)bmeta);
            }
        }
        return cheat;
    }

    public boolean isCrashSkull(NbtCompound tag) {
        if (tag.containsKey("SkullOwner")) {
            NbtCompound skullOwner = tag.getCompound("SkullOwner");
            if (skullOwner.containsKey("Properties")) {
                NbtCompound properties = skullOwner.getCompound("Properties");
                if (properties.containsKey("textures")) {
                    NbtList<NbtBase> textures = properties.getList("textures");
                    for (NbtBase texture : textures.asCollection()) {
                        if (texture instanceof NbtCompound && (
                                (NbtCompound)texture).containsKey("Value") && (
                                (NbtCompound)texture).getString("Value").trim().length() > 0) {
                            String decoded = null;
                            try {
                                decoded = new String(Base64.decodeBase64(((NbtCompound)texture).getString("Value").getBytes()));
                            } catch (Exception e) {
                                tag.remove("SkullOwner");
                                return true;
                            }
                            if (decoded == null || decoded.isEmpty()) {
                                tag.remove("SkullOwner");
                                return true;
                            }
                            if (decoded.contains("textures") && decoded.contains("SKIN") &&
                                    decoded.contains("url")) {
                                String headUrl = null;
                                try {
                                    headUrl = decoded.split("url\":")[1].replace("}", "").replace("\"", "");
                                } catch (ArrayIndexOutOfBoundsException e) {
                                    tag.remove("SkullOwner");
                                    return true;
                                }
                                if (headUrl == null || headUrl.isEmpty() || headUrl.trim().length() == 0) {
                                    tag.remove("SkullOwner");
                                    return true;
                                }
                                if (headUrl.startsWith("http://textures.minecraft.net/texture/") || headUrl.startsWith("https://textures.minecraft.net/texture/"))
                                    return false;
                            }
                        }
                    }
                }
                tag.remove("SkullOwner");
                return true;
            }
        }
        return false;
    }


    public boolean checkFireWork(ItemStack stack) {
        boolean changed = false;
        FireworkMeta meta = (FireworkMeta)stack.getItemMeta();
        if (meta.getPower() > 3) {
            meta.setPower(3);
            changed = true;
        }
        if (meta.getEffectsSize() > 8) {
            List<FireworkEffect> list = (List<FireworkEffect>)meta.getEffects().stream().limit(8L).collect(Collectors.toList());
            meta.clearEffects();
            meta.addEffects(list);
            changed = true;
        }
        if (changed)
            stack.setItemMeta((ItemMeta)meta);
        return changed;
    }


    private boolean fixEgg(NbtCompound tag) {
        NbtCompound enttag = tag.getCompound("EntityTag");
        int size = enttag.getKeys().size();
        if (size >= 2) {
            Object id = enttag.getObject("id");
            Object color = enttag.getObject("Color");
            enttag.getKeys().clear();
            if (id != null && id instanceof String)
                enttag.put("id", (String)id);
            if (color != null && color instanceof Byte)
                enttag.put("Color", ((Byte)color).byteValue());
            tag.put("EntityTag", (NbtBase)enttag);
            return (color == null) ? true : ((size >= 3));
        }
        return false;
    }

    private boolean checkPotion(ItemStack stack, Player p) {
        boolean cheat = false;
        if (!p.hasPermission("jkitemfixer.bypass.potion")) {
            PotionMeta meta = (PotionMeta)stack.getItemMeta();
            for (PotionEffect ef : meta.getCustomEffects()) {
                String perm = "jkitemfixer.allow.".concat(ef.getType().toString()).concat(".").concat(String.valueOf(ef.getAmplifier() + 1));
                if (!p.hasPermission(perm)) {
                    meta.removeCustomEffect(ef.getType());
                    cheat = true;
                }
            }
            if (cheat)
                stack.setItemMeta((ItemMeta)meta);
        }
        return cheat;
    }

    private boolean checkCustomColor(NbtCompound tag) {
        if (tag.containsKey("CustomPotionColor")) {
            int color = tag.getInteger("CustomPotionColor");
            try {
                Color.fromBGR(color);
            } catch (IllegalArgumentException e) {
                tag.remove("CustomPotionColor");
                return true;
            }
        }
        return false;
    }


    private boolean isPotion(ItemStack stack) {
        try {
            return (stack.hasItemMeta() && stack.getItemMeta() instanceof PotionMeta);
        } catch (IllegalArgumentException e) {
            clearData(stack);
            return false;
        }
    }
}
