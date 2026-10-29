package com.thegildedking.arcanum;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/** Spell binding screen for the Multi-Staff. Two pages: element picker and spell list for that element. */
public final class MultiGui implements InventoryHolder {
    public final EquipmentSlot hand;
    public final Element element; // null = element picker page
    private final Inventory inv;

    static final int BACK = 40;
    static final int SPELL_START = 1;   // slots 1..8
    static final int BOUND_START = 19;  // slots 19..26

    private MultiGui(Player p, EquipmentSlot hand, Element element) {
        this.hand = hand;
        this.element = element;
        ItemStack staff = p.getInventory().getItem(hand);
        if (element == null) {
            inv = Bukkit.createInventory(this, 36, Component.text("Multi-Staff: pick an element", NamedTextColor.GOLD)
                    .decorate(TextDecoration.BOLD));
            fill(inv);
            Element[] els = Element.values();
            for (int i = 0; i < els.length; i++) {
                ItemStack it = Items.staff(els[i], 4);
                ItemMeta m = it.getItemMeta();
                m.displayName(Items.t(els[i].display, els[i].rgb).decorate(TextDecoration.BOLD));
                List<Component> lore = new ArrayList<>();
                lore.add(Items.t("Click to browse " + els[i].display + " spells", NamedTextColor.GRAY));
                m.lore(lore);
                it.setItemMeta(m);
                inv.setItem(ChooseGui.SLOTS[i], it);
            }
        } else {
            inv = Bukkit.createInventory(this, 45, Component.text("Multi-Staff: " + element.display, element.color())
                    .decorate(TextDecoration.BOLD));
            fill(inv);
            List<String> bound = Items.slotIds(staff);
            int cap = Items.slotCapacity(Items.level(staff));
            List<Spell> spells = Spells.of(element);
            for (int i = 0; i < spells.size(); i++) {
                Spell s = spells.get(i);
                ItemStack it = Items.scroll(s);
                ItemMeta m = it.getItemMeta();
                boolean on = bound.contains(s.id);
                List<Component> lore = new ArrayList<>(m.lore() == null ? List.<Component>of() : m.lore());
                lore.add(Items.blank());
                lore.add(on ? Items.t("Bound - click a bound slot below to remove", NamedTextColor.GREEN)
                        : Items.t("Click to bind to the staff", 0xFFD45E));
                m.lore(lore);
                m.setEnchantmentGlintOverride(on);
                it.setItemMeta(m);
                inv.setItem(SPELL_START + i, it);
            }
            for (int i = 0; i < 8; i++) {
                ItemStack it;
                if (i >= cap) {
                    it = named(Material.RED_STAINED_GLASS_PANE, Items.t("Locked slot", NamedTextColor.RED),
                            Items.t("Upgrade the staff for more slots", NamedTextColor.GRAY));
                } else if (i < bound.size() && Spells.get(bound.get(i)) != null) {
                    Spell s = Spells.get(bound.get(i));
                    it = Items.scroll(s);
                    ItemMeta m = it.getItemMeta();
                    List<Component> lore = new ArrayList<>();
                    lore.add(Items.t(s.element.display + " - " + s.desc, NamedTextColor.GRAY));
                    lore.add(Items.blank());
                    lore.add(Items.t("Click to remove", NamedTextColor.RED));
                    m.lore(lore);
                    it.setItemMeta(m);
                } else {
                    it = named(Material.GRAY_STAINED_GLASS_PANE, Items.t("Empty slot", NamedTextColor.GRAY),
                            Items.t("Click a spell above to bind it", NamedTextColor.DARK_GRAY));
                }
                inv.setItem(BOUND_START + i, it);
            }
            inv.setItem(BACK, named(Material.ARROW, Items.t("Back", NamedTextColor.YELLOW)));
        }
    }

    private static ItemStack named(Material m, Component name, Component... lore) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(name);
        if (lore.length > 0) meta.lore(List.of(lore));
        it.setItemMeta(meta);
        return it;
    }

    private static void fill(Inventory inv) {
        ItemStack pane = named(Material.BLACK_STAINED_GLASS_PANE, Component.text(" "));
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, pane);
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }

    public static void openElements(Player p, EquipmentSlot hand) {
        p.openInventory(new MultiGui(p, hand, null).getInventory());
    }

    public static void openSpells(Player p, EquipmentSlot hand, Element e) {
        p.openInventory(new MultiGui(p, hand, e).getInventory());
    }
}
