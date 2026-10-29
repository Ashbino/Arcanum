package com.thegildedking.arcanum;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/** Factory and reader for every custom item. All data lives in the item's PersistentDataContainer. */
public final class Items {
    private Items() {}

    public static final String STAFF = "staff", MULTI = "multi", SCROLL = "scroll", BOOK = "book", CRYSTAL = "crystal";
    public static final String[] ROMAN = {"I", "II", "III", "IV"};
    private static final int GOLD = 0xFFD45E;

    static NamespacedKey TYPE, ELEMENT, LEVEL, SPELL, SEL, SLOTS, TIER;

    public static void init(Arcanum pl) {
        TYPE = new NamespacedKey(pl, "type");
        ELEMENT = new NamespacedKey(pl, "element");
        LEVEL = new NamespacedKey(pl, "level");
        SPELL = new NamespacedKey(pl, "spell");
        SEL = new NamespacedKey(pl, "sel");
        SLOTS = new NamespacedKey(pl, "slots");
        TIER = new NamespacedKey(pl, "tier");
    }

    // ------------------------------------------------------------------ text helpers

    static Component t(String s, int rgb) {
        return Component.text(s, TextColor.color(rgb)).decoration(TextDecoration.ITALIC, false);
    }

    static Component t(String s, NamedTextColor c) {
        return Component.text(s, c).decoration(TextDecoration.ITALIC, false);
    }

    static Component blank() {
        return Component.empty().decoration(TextDecoration.ITALIC, false);
    }

    // ------------------------------------------------------------------ readers

    public static String type(ItemStack it) {
        if (it == null || !it.hasItemMeta()) return null;
        return it.getItemMeta().getPersistentDataContainer().get(TYPE, PersistentDataType.STRING);
    }

    public static boolean isArcanum(ItemStack it) {
        return type(it) != null;
    }

    public static boolean isStaff(ItemStack it) {
        String t = type(it);
        return STAFF.equals(t) || MULTI.equals(t);
    }

    private static int getInt(ItemStack it, NamespacedKey k, int def) {
        if (it == null || !it.hasItemMeta()) return def;
        Integer v = it.getItemMeta().getPersistentDataContainer().get(k, PersistentDataType.INTEGER);
        return v == null ? def : v;
    }

    private static String getStr(ItemStack it, NamespacedKey k) {
        if (it == null || !it.hasItemMeta()) return null;
        return it.getItemMeta().getPersistentDataContainer().get(k, PersistentDataType.STRING);
    }

    public static int level(ItemStack it) {
        return getInt(it, LEVEL, 1);
    }

    public static int tier(ItemStack it) {
        return getInt(it, TIER, 1);
    }

    public static Element element(ItemStack it) {
        return Element.byId(getStr(it, ELEMENT));
    }

    public static Spell spell(ItemStack it) {
        return Spells.get(getStr(it, SPELL));
    }

    public static int slotCapacity(int level) {
        return level * 2;
    }

    /** Spells the staff can currently cast, in order. */
    public static List<Spell> staffSpells(ItemStack it) {
        List<Spell> out = new ArrayList<>();
        String type = type(it);
        int level = level(it);
        if (STAFF.equals(type)) {
            Element e = element(it);
            if (e != null) {
                List<Spell> all = Spells.of(e);
                for (int i = 0; i < Math.min(all.size(), slotCapacity(level)); i++) out.add(all.get(i));
            }
        } else if (MULTI.equals(type)) {
            String csv = getStr(it, SLOTS);
            if (csv != null && !csv.isEmpty()) {
                for (String id : csv.split(",")) {
                    Spell s = Spells.get(id);
                    if (s != null && out.size() < slotCapacity(level)) out.add(s);
                }
            }
        }
        return out;
    }

    public static Spell selected(ItemStack it) {
        String type = type(it);
        if (BOOK.equals(type) || SCROLL.equals(type)) return spell(it);
        List<Spell> list = staffSpells(it);
        if (list.isEmpty()) return null;
        int sel = Math.floorMod(getInt(it, SEL, 0), list.size());
        return list.get(sel);
    }

    // ------------------------------------------------------------------ writers

    public static void setLevel(ItemStack it, int level) {
        ItemMeta m = it.getItemMeta();
        m.getPersistentDataContainer().set(LEVEL, PersistentDataType.INTEGER, level);
        it.setItemMeta(m);
        refresh(it);
    }

    public static void cycle(ItemStack it, int dir) {
        List<Spell> list = staffSpells(it);
        if (list.isEmpty()) return;
        int sel = Math.floorMod(getInt(it, SEL, 0) + dir, list.size());
        ItemMeta m = it.getItemMeta();
        m.getPersistentDataContainer().set(SEL, PersistentDataType.INTEGER, sel);
        it.setItemMeta(m);
        refresh(it);
    }

    public static List<String> slotIds(ItemStack it) {
        List<String> out = new ArrayList<>();
        String csv = getStr(it, SLOTS);
        if (csv != null && !csv.isEmpty()) for (String s : csv.split(",")) out.add(s);
        return out;
    }

    public static void setSlots(ItemStack it, List<String> ids) {
        ItemMeta m = it.getItemMeta();
        PersistentDataContainer pdc = m.getPersistentDataContainer();
        pdc.set(SLOTS, PersistentDataType.STRING, String.join(",", ids));
        pdc.set(SEL, PersistentDataType.INTEGER, 0);
        it.setItemMeta(m);
        refresh(it);
    }

    // ------------------------------------------------------------------ factories

    public static ItemStack staff(Element e, int level) {
        ItemStack it = new ItemStack(Material.BLAZE_ROD);
        ItemMeta m = it.getItemMeta();
        PersistentDataContainer pdc = m.getPersistentDataContainer();
        pdc.set(TYPE, PersistentDataType.STRING, STAFF);
        pdc.set(ELEMENT, PersistentDataType.STRING, e.id);
        pdc.set(LEVEL, PersistentDataType.INTEGER, Math.max(1, Math.min(4, level)));
        pdc.set(SEL, PersistentDataType.INTEGER, 0);
        it.setItemMeta(m);
        refresh(it);
        return it;
    }

    public static ItemStack multi(int level) {
        ItemStack it = new ItemStack(Material.BLAZE_ROD);
        ItemMeta m = it.getItemMeta();
        PersistentDataContainer pdc = m.getPersistentDataContainer();
        pdc.set(TYPE, PersistentDataType.STRING, MULTI);
        pdc.set(LEVEL, PersistentDataType.INTEGER, Math.max(2, Math.min(4, level)));
        pdc.set(SEL, PersistentDataType.INTEGER, 0);
        pdc.set(SLOTS, PersistentDataType.STRING, "");
        it.setItemMeta(m);
        refresh(it);
        return it;
    }

    public static ItemStack scroll(Spell s) {
        ItemStack it = new ItemStack(Material.PAPER);
        ItemMeta m = it.getItemMeta();
        PersistentDataContainer pdc = m.getPersistentDataContainer();
        pdc.set(TYPE, PersistentDataType.STRING, SCROLL);
        pdc.set(SPELL, PersistentDataType.STRING, s.id);
        it.setItemMeta(m);
        refresh(it);
        return it;
    }

    public static ItemStack book(Spell s) {
        ItemStack it = new ItemStack(Material.BOOK);
        ItemMeta m = it.getItemMeta();
        PersistentDataContainer pdc = m.getPersistentDataContainer();
        pdc.set(TYPE, PersistentDataType.STRING, BOOK);
        pdc.set(SPELL, PersistentDataType.STRING, s.id);
        it.setItemMeta(m);
        refresh(it);
        return it;
    }

    public static ItemStack crystal(int tier) {
        ItemStack it = new ItemStack(Material.AMETHYST_SHARD);
        ItemMeta m = it.getItemMeta();
        m.getPersistentDataContainer().set(TYPE, PersistentDataType.STRING, CRYSTAL);
        m.getPersistentDataContainer().set(TIER, PersistentDataType.INTEGER, Math.max(2, Math.min(4, tier)));
        it.setItemMeta(m);
        refresh(it);
        return it;
    }

    // ------------------------------------------------------------------ appearance

    private static String stars(int level) {
        return "★".repeat(level) + "☆".repeat(4 - level);
    }

    private static void model(ItemMeta m, String name) {
        NamespacedKey k = NamespacedKey.fromString("arcanum:" + name);
        if (k != null) m.setItemModel(k);
    }

    /** Rebuilds name, lore and model from the stored data. */
    public static void refresh(ItemStack it) {
        String type = type(it);
        if (type == null) return;
        ItemMeta m = it.getItemMeta();
        List<Component> lore = new ArrayList<>();
        m.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);

        switch (type) {
            case STAFF -> {
                Element e = element(it);
                int level = level(it);
                Spell sel = selected(it);
                m.displayName(t(e.display + " Staff", e.rgb).decorate(TextDecoration.BOLD)
                        .append(t("  " + ROMAN[level - 1], GOLD)));
                model(m, "staff_" + e.id + "_" + level);
                m.setEnchantmentGlintOverride(level >= 4);
                lore.add(t("Level " + ROMAN[level - 1] + "  " + stars(level), GOLD));
                lore.add(t(e.title + " staff", e.rgb));
                lore.add(blank());
                lore.add(t("Spells", NamedTextColor.GRAY));
                List<Spell> all = Spells.of(e);
                for (int i = 0; i < all.size(); i++) {
                    Spell s = all.get(i);
                    if (i < slotCapacity(level)) {
                        boolean on = sel != null && sel.id.equals(s.id);
                        lore.add(t((on ? "▶ " : "  ") + s.name, on ? 0xFFFFFF : e.rgb)
                                .append(t("  " + (int) s.mana + " mana", NamedTextColor.DARK_GRAY)));
                    } else {
                        lore.add(t("  ✖ " + s.name, NamedTextColor.DARK_GRAY)
                                .append(t("  needs Level " + ROMAN[s.tier() - 1], NamedTextColor.DARK_GRAY)));
                    }
                }
                lore.add(blank());
                lore.add(t("Right-click: cast spell", NamedTextColor.GRAY));
                lore.add(t("Left-click or Shift+Scroll: switch spell", NamedTextColor.GRAY));
                if (level < 4) lore.add(t("Upgrade with an Arcane Crystal " + ROMAN[level], NamedTextColor.DARK_PURPLE));
            }
            case MULTI -> {
                int level = level(it);
                List<Spell> bound = staffSpells(it);
                Spell sel = selected(it);
                m.displayName(t("Archmage's Multi-Staff", 0xFFD45E).decorate(TextDecoration.BOLD)
                        .append(t("  " + ROMAN[level - 1], 0xFFFFFF)));
                model(m, "staff_multi_" + level);
                m.setEnchantmentGlintOverride(true);
                lore.add(t("Level " + ROMAN[level - 1] + "  " + stars(level), GOLD));
                lore.add(t("Wields every element", 0xE6B3FF));
                lore.add(blank());
                lore.add(t("Bound spells (" + bound.size() + "/" + slotCapacity(level) + ")", NamedTextColor.GRAY));
                for (int i = 0; i < slotCapacity(level); i++) {
                    if (i < bound.size()) {
                        Spell s = bound.get(i);
                        boolean on = sel != null && sel.id.equals(s.id);
                        lore.add(t((on ? "▶ " : "  ") + s.name, on ? 0xFFFFFF : s.element.rgb)
                                .append(t("  " + s.element.display, NamedTextColor.DARK_GRAY)));
                    } else {
                        lore.add(t("  - empty slot -", NamedTextColor.DARK_GRAY));
                    }
                }
                lore.add(blank());
                lore.add(t("Shift+Right-click: bind spells", NamedTextColor.GRAY));
                lore.add(t("Right-click: cast spell", NamedTextColor.GRAY));
                lore.add(t("Left-click or Shift+Scroll: switch spell", NamedTextColor.GRAY));
                if (level < 4) lore.add(t("Upgrade with an Arcane Crystal " + ROMAN[level], NamedTextColor.DARK_PURPLE));
            }
            case SCROLL -> {
                Spell s = spell(it);
                if (s == null) return;
                m.displayName(t("Scroll of " + s.name, s.element.rgb));
                model(m, "scroll_" + s.element.id);
                m.setMaxStackSize(16);
                lore.add(t(s.element.display + " - Tier " + ROMAN[s.tier() - 1], GOLD));
                lore.add(blank());
                lore.add(t(s.desc, NamedTextColor.GRAY));
                lore.add(blank());
                lore.add(t("Single use - costs no mana", NamedTextColor.DARK_GRAY));
                lore.add(t("Right-click to cast", NamedTextColor.GRAY));
            }
            case BOOK -> {
                Spell s = spell(it);
                if (s == null) return;
                m.displayName(t("Tome of " + s.name, s.element.rgb).decorate(TextDecoration.BOLD));
                model(m, "book_" + s.element.id);
                m.setMaxStackSize(1);
                lore.add(t(s.element.display + " - Tier " + ROMAN[s.tier() - 1], GOLD));
                lore.add(blank());
                lore.add(t(s.desc, NamedTextColor.GRAY));
                lore.add(blank());
                lore.add(t("Unlimited uses - costs " + (int) s.mana + " mana", NamedTextColor.DARK_GRAY));
                lore.add(t("Right-click to cast", NamedTextColor.GRAY));
            }
            case CRYSTAL -> {
                int tier = tier(it);
                m.displayName(t("Arcane Crystal " + ROMAN[tier - 1], 0xC77DFF).decorate(TextDecoration.BOLD));
                model(m, "crystal_" + tier);
                m.setEnchantmentGlintOverride(tier >= 4);
                lore.add(t("Upgrades a Level " + ROMAN[tier - 2] + " staff to Level " + ROMAN[tier - 1], NamedTextColor.GRAY));
                lore.add(blank());
                lore.add(t("Craft: staff + crystal", NamedTextColor.DARK_GRAY));
            }
            default -> {
                return;
            }
        }
        if (STAFF.equals(type) || MULTI.equals(type)) m.setMaxStackSize(1);
        m.lore(lore);
        it.setItemMeta(m);
    }

    public static boolean holdingMagic(Player p) {
        return isHoldable(p.getInventory().getItemInMainHand()) || isHoldable(p.getInventory().getItemInOffHand());
    }

    private static boolean isHoldable(ItemStack it) {
        String t = type(it);
        return STAFF.equals(t) || MULTI.equals(t) || BOOK.equals(t);
    }
}
