package com.thegildedking.arcanum;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class MageListener implements Listener {
    private final Arcanum pl;
    private final Map<UUID, Integer> lastUseTick = new HashMap<>();

    public MageListener(Arcanum pl) {
        this.pl = pl;
    }

    // ------------------------------------------------------------------ join / quit

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        pl.mana.load(p);
        Recipes.discover(p);

        String url = pl.getConfig().getString("resource-pack.url", "");
        if (url != null && !url.isBlank()) {
            String sha1 = pl.getConfig().getString("resource-pack.sha1", "");
            boolean required = pl.getConfig().getBoolean("resource-pack.required", false);
            Component prompt = Component.text(pl.getConfig().getString("resource-pack.prompt", ""));
            Bukkit.getScheduler().runTaskLater(pl, () -> {
                if (!p.isOnline()) return;
                if (sha1 != null && sha1.length() == 40) p.setResourcePack(url, sha1, required, prompt);
                else p.setResourcePack(url);
            }, 10L);
        }

        if (pl.getConfig().getBoolean("choose-on-join", true) && pl.mana.mage(p) == null) {
            Bukkit.getScheduler().runTaskLater(pl, () -> {
                if (p.isOnline() && pl.mana.mage(p) == null) ChooseGui.open(p);
            }, 50L);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        pl.mana.unload(e.getPlayer());
        pl.casts.clear(e.getPlayer());
        lastUseTick.remove(e.getPlayer().getUniqueId());
    }

    // ------------------------------------------------------------------ using items

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getHand() == null) return;
        ItemStack held = e.getItem();
        String type = Items.type(held);
        if (type == null) return;

        Player p = e.getPlayer();
        Action a = e.getAction();
        if (a == Action.PHYSICAL) return;

        // let people still use chests, doors, etc. while holding a magic item (unless sneaking)
        if (a == Action.RIGHT_CLICK_BLOCK && e.getClickedBlock() != null
                && e.getClickedBlock().getType().isInteractable() && !p.isSneaking()) {
            return;
        }

        boolean staff = Items.STAFF.equals(type) || Items.MULTI.equals(type);
        boolean book = Items.BOOK.equals(type);
        boolean scroll = Items.SCROLL.equals(type);
        if (!staff && !book && !scroll) return;

        e.setCancelled(true);

        // only one action per tick per player (main + off hand both fire)
        int now = Bukkit.getCurrentTick();
        Integer last = lastUseTick.get(p.getUniqueId());
        if (last != null && last == now) return;
        lastUseTick.put(p.getUniqueId(), now);

        EquipmentSlot hand = e.getHand();
        ItemStack stack = p.getInventory().getItem(hand);
        if (stack == null || !Items.isArcanum(stack)) return;

        if (a.isLeftClick()) {
            if (staff) {
                Items.cycle(stack, p.isSneaking() ? -1 : 1);
                p.getInventory().setItem(hand, stack);
                p.playSound(p.getLocation(), "block.note_block.chime", 0.6f, 1.6f);
                pl.mana.hud(p);
            }
            return;
        }

        // right click
        if (staff && Items.MULTI.equals(type) && p.isSneaking()) {
            MultiGui.openElements(p, hand);
            return;
        }
        Spell s = Items.selected(stack);
        if (s == null) {
            Fx.fail(p, staff ? "This staff has no spells bound. Shift+Right-click to bind spells." : "Empty.");
            return;
        }

        if (staff) {
            double mult = 1.0 - (Items.level(stack) - 1) * 0.05;
            pl.casts.cast(p, s, CastManager.Source.STAFF, mult);
        } else if (book) {
            pl.casts.cast(p, s, CastManager.Source.BOOK, 1.0);
        } else {
            if (pl.casts.cast(p, s, CastManager.Source.SCROLL, 1.0)) {
                stack.setAmount(stack.getAmount() - 1);
                p.getInventory().setItem(hand, stack.getAmount() <= 0 ? null : stack);
                p.getWorld().spawnParticle(org.bukkit.Particle.ENCHANT, p.getLocation().add(0, 1, 0), 30, 0.4, 0.6, 0.4, 0.5);
            }
        }
        pl.mana.hud(p);
    }

    /** Shift + scroll wheel cycles the spell instead of changing hotbar slot. */
    @EventHandler
    public void onHeld(PlayerItemHeldEvent e) {
        Player p = e.getPlayer();
        if (!p.isSneaking()) return;
        ItemStack old = p.getInventory().getItem(e.getPreviousSlot());
        if (!Items.isStaff(old)) return;
        int delta = ((e.getNewSlot() - e.getPreviousSlot() + 9 + 4) % 9) - 4;
        if (delta == 0) return;
        e.setCancelled(true);
        Items.cycle(old, delta > 0 ? 1 : -1);
        p.getInventory().setItem(e.getPreviousSlot(), old);
        p.playSound(p.getLocation(), "block.note_block.chime", 0.6f, 1.6f);
        pl.mana.hud(p);
    }

    // ------------------------------------------------------------------ temporary blocks

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        if (pl.temp.containsKey(e.getBlock())) {
            e.setCancelled(true);
            e.getPlayer().sendActionBar(Component.text("Magic holds this block together.", NamedTextColor.GRAY));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent e) {
        e.blockList().removeIf(pl.temp::containsKey);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent e) {
        e.blockList().removeIf(pl.temp::containsKey);
    }
}
