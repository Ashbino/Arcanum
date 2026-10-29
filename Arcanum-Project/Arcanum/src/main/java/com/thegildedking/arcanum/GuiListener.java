package com.thegildedking.arcanum;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class GuiListener implements Listener {
    private final Arcanum pl;

    public GuiListener(Arcanum pl) {
        this.pl = pl;
    }

    private static boolean ours(Inventory inv) {
        return inv.getHolder() instanceof ChooseGui || inv.getHolder() instanceof MultiGui;
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (ours(e.getView().getTopInventory())) e.setCancelled(true);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        Inventory top = e.getView().getTopInventory();
        if (!ours(top)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (e.getClickedInventory() != top) return;
        int slot = e.getRawSlot();

        if (top.getHolder() instanceof ChooseGui g) {
            Element el = g.at(slot);
            if (el != null) choose(p, el);
            return;
        }

        if (top.getHolder() instanceof MultiGui g) {
            ItemStack staff = p.getInventory().getItem(g.hand);
            if (staff == null || !Items.MULTI.equals(Items.type(staff))) {
                p.closeInventory();
                return;
            }
            if (g.element == null) {
                for (int i = 0; i < ChooseGui.SLOTS.length; i++) {
                    if (ChooseGui.SLOTS[i] == slot) {
                        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.PLAYERS, 0.6f, 1.2f);
                        Element target = Element.values()[i];
                        Bukkit.getScheduler().runTask(pl, () -> MultiGui.openSpells(p, g.hand, target));
                        return;
                    }
                }
                return;
            }
            if (slot == MultiGui.BACK) {
                Bukkit.getScheduler().runTask(pl, () -> MultiGui.openElements(p, g.hand));
                return;
            }
            List<String> bound = new java.util.ArrayList<>(Items.slotIds(staff));
            int cap = Items.slotCapacity(Items.level(staff));
            List<Spell> spells = Spells.of(g.element);

            if (slot >= MultiGui.SPELL_START && slot < MultiGui.SPELL_START + spells.size()) {
                Spell s = spells.get(slot - MultiGui.SPELL_START);
                if (bound.contains(s.id)) {
                    p.sendActionBar(Component.text("Already bound.", NamedTextColor.GRAY));
                    return;
                }
                if (bound.size() >= cap) {
                    Fx.fail(p, "No free slots. Remove a spell or upgrade the staff.");
                    return;
                }
                bound.add(s.id);
            } else if (slot >= MultiGui.BOUND_START && slot < MultiGui.BOUND_START + 8) {
                int idx = slot - MultiGui.BOUND_START;
                if (idx >= bound.size()) return;
                bound.remove(idx);
            } else {
                return;
            }
            Items.setSlots(staff, bound);
            p.getInventory().setItem(g.hand, staff);
            p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 0.8f, 1.4f);
            Bukkit.getScheduler().runTask(pl, () -> MultiGui.openSpells(p, g.hand, g.element));
        }
    }

    private void choose(Player p, Element el) {
        if (pl.mana.mage(p) != null) {
            p.closeInventory();
            return;
        }
        pl.mana.setMage(p, el);
        pl.mana.set(p, pl.mana.max(p));
        ItemStack staff = Items.staff(el, 1);
        Map<Integer, ItemStack> left = p.getInventory().addItem(staff);
        for (ItemStack rest : left.values()) p.getWorld().dropItemNaturally(p.getLocation(), rest);
        p.closeInventory();
        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 1f, 1.2f);
        p.playSound(p.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 1f, 1f);
        p.sendMessage(Component.text("✦ You are now a ", NamedTextColor.GOLD)
                .append(Component.text(el.title, el.color()))
                .append(Component.text("! Use your staff: right-click to cast, left-click to switch spell.", NamedTextColor.GOLD)));
        Fx.part(p.getLocation().add(0, 1, 0), org.bukkit.Particle.ENCHANT, 80, 0.5, 1, 0.5, 0.8);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getInventory().getHolder() instanceof ChooseGui)) return;
        if (!(e.getPlayer() instanceof Player p)) return;
        if (!pl.getConfig().getBoolean("force-choose", true)) return;
        if (pl.mana.mage(p) != null) return;
        Bukkit.getScheduler().runTaskLater(pl, () -> {
            if (p.isOnline() && pl.mana.mage(p) == null && !(p.getOpenInventory().getTopInventory().getHolder() instanceof ChooseGui)) {
                ChooseGui.open(p);
            }
        }, 10L);
    }
}
