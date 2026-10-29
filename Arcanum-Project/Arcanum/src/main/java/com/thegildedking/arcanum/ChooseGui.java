package com.thegildedking.arcanum;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/** The "choose your mage" popup shown when a player first joins. */
public final class ChooseGui implements InventoryHolder {
    static final int[] SLOTS = {10, 12, 14, 16, 19, 21, 23, 25};

    private final Inventory inv;

    public ChooseGui() {
        inv = Bukkit.createInventory(this, 36, Component.text("✦ Choose Your Path ✦", NamedTextColor.GOLD)
                .decorate(TextDecoration.BOLD));
        ItemStack pane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta pm = pane.getItemMeta();
        pm.displayName(Component.text(" "));
        pane.setItemMeta(pm);
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, pane);

        Element[] els = Element.values();
        for (int i = 0; i < els.length; i++) inv.setItem(SLOTS[i], icon(els[i]));
    }

    private static ItemStack icon(Element e) {
        ItemStack it = Items.staff(e, 1);
        ItemMeta m = it.getItemMeta();
        m.displayName(Items.t(e.title, e.rgb).decorate(TextDecoration.BOLD));
        List<Component> lore = new ArrayList<>();
        lore.add(Items.t(e.display + " magic", NamedTextColor.GRAY));
        lore.add(Items.blank());
        lore.add(Items.t(e.blurb, NamedTextColor.GRAY));
        lore.add(Items.blank());
        lore.add(Items.t("You start with:", NamedTextColor.DARK_GRAY));
        List<Spell> spells = Spells.of(e);
        for (int i = 0; i < 2 && i < spells.size(); i++) lore.add(Items.t(" - " + spells.get(i).name, e.rgb));
        lore.add(Items.blank());
        lore.add(Items.t("Click to become a " + e.title, 0xFFD45E));
        m.lore(lore);
        it.setItemMeta(m);
        return it;
    }

    public Element at(int slot) {
        for (int i = 0; i < SLOTS.length; i++) if (SLOTS[i] == slot) return Element.values()[i];
        return null;
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }

    public static void open(Player p) {
        p.openInventory(new ChooseGui().getInventory());
    }
}
