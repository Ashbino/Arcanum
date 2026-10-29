package com.thegildedking.arcanum;

import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

import java.util.EnumSet;
import java.util.Set;

/** Validates the "dummy" recipes (upgrade, spell book, multi staff) and keeps custom items out of vanilla recipes. */
public final class CraftListener implements Listener {
    private final Arcanum pl;

    public CraftListener(Arcanum pl) {
        this.pl = pl;
    }

    @EventHandler
    public void onPrepare(PrepareItemCraftEvent e) {
        Recipe r = e.getRecipe();
        if (r == null) return;
        CraftingInventory inv = e.getInventory();
        ItemStack[] matrix = inv.getMatrix();

        boolean hasCustom = false;
        for (ItemStack s : matrix) if (s != null && Items.isArcanum(s)) hasCustom = true;

        String path = null;
        if (r instanceof Keyed k && k.getKey().getNamespace().equals(pl.getName().toLowerCase())) {
            path = k.getKey().getKey();
        }

        // vanilla (or other plugin) recipe
        if (path == null) {
            if (hasCustom) inv.setResult(null);
            return;
        }

        switch (path) {
            case "upgrade" -> inv.setResult(upgrade(matrix));
            case "book" -> inv.setResult(book(matrix));
            case "multi" -> inv.setResult(multi(matrix));
            default -> {
                // normal recipes may not use custom items as ingredients
                if (hasCustom) inv.setResult(null);
            }
        }
    }

    private ItemStack upgrade(ItemStack[] matrix) {
        ItemStack staff = null, crystal = null;
        int count = 0;
        for (ItemStack s : matrix) {
            if (s == null || s.getType().isAir()) continue;
            count++;
            if (Items.isStaff(s)) staff = s;
            else if (Items.CRYSTAL.equals(Items.type(s))) crystal = s;
        }
        if (count != 2 || staff == null || crystal == null) return null;
        int level = Items.level(staff);
        if (level >= 4 || Items.tier(crystal) != level + 1) return null;
        ItemStack out = staff.clone();
        out.setAmount(1);
        Items.setLevel(out, level + 1);
        return out;
    }

    private ItemStack book(ItemStack[] matrix) {
        ItemStack scroll = null;
        int count = 0;
        for (ItemStack s : matrix) {
            if (s == null || s.getType().isAir()) continue;
            count++;
            if (Items.SCROLL.equals(Items.type(s))) scroll = s;
            else if (Items.isArcanum(s)) return null;
        }
        if (count != 3 || scroll == null) return null;
        Spell sp = Items.spell(scroll);
        return sp == null ? null : Items.book(sp);
    }

    private ItemStack multi(ItemStack[] matrix) {
        if (matrix.length != 9) return null;
        ItemStack center = matrix[4];
        if (center == null || !Items.CRYSTAL.equals(Items.type(center)) || Items.tier(center) != 4) return null;
        int minLevel = pl.getConfig().getInt("multi-staff-min-level", 3);
        Set<Element> seen = EnumSet.noneOf(Element.class);
        for (int i = 0; i < 9; i++) {
            if (i == 4) continue;
            ItemStack s = matrix[i];
            if (s == null || !Items.STAFF.equals(Items.type(s))) return null;
            if (Items.level(s) < minLevel) return null;
            Element el = Items.element(s);
            if (el == null || !seen.add(el)) return null;
        }
        return seen.size() == Element.values().length ? Items.multi(2) : null;
    }
}
