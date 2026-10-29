package com.thegildedking.arcanum;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Mana, mage choice and mage rank (xp) for every player. */
public final class ManaManager {
    private final Arcanum pl;
    private final Map<UUID, Double> mana = new HashMap<>();
    private final NamespacedKey kMana, kXp, kMage;

    public ManaManager(Arcanum pl) {
        this.pl = pl;
        kMana = new NamespacedKey(pl, "mana");
        kXp = new NamespacedKey(pl, "xp");
        kMage = new NamespacedKey(pl, "mage");
    }

    // ------------------------------------------------------------------ persistence

    public void load(Player p) {
        Double saved = p.getPersistentDataContainer().get(kMana, PersistentDataType.DOUBLE);
        mana.put(p.getUniqueId(), saved == null ? max(p) : Math.min(saved, max(p)));
    }

    public void save(Player p) {
        Double v = mana.get(p.getUniqueId());
        if (v != null) p.getPersistentDataContainer().set(kMana, PersistentDataType.DOUBLE, v);
    }

    public void saveAll() {
        for (Player p : Bukkit.getOnlinePlayers()) save(p);
    }

    public void unload(Player p) {
        save(p);
        mana.remove(p.getUniqueId());
    }

    // ------------------------------------------------------------------ mage / rank

    public Element mage(Player p) {
        return Element.byId(p.getPersistentDataContainer().get(kMage, PersistentDataType.STRING));
    }

    public void setMage(Player p, Element e) {
        PersistentDataContainer pdc = p.getPersistentDataContainer();
        if (e == null) pdc.remove(kMage);
        else pdc.set(kMage, PersistentDataType.STRING, e.id);
    }

    public Element affinity(Player p) {
        return mage(p);
    }

    public int xp(Player p) {
        Integer v = p.getPersistentDataContainer().get(kXp, PersistentDataType.INTEGER);
        return v == null ? 0 : v;
    }

    public int rank(Player p) {
        int maxRank = pl.getConfig().getInt("mana.max-rank", 20);
        return Math.min(maxRank, (int) Math.floor(Math.sqrt(xp(p) / 6.0)));
    }

    public void resetProgress(Player p) {
        p.getPersistentDataContainer().remove(kXp);
        mana.put(p.getUniqueId(), max(p));
    }

    public void addXp(Player p, int amount) {
        int before = rank(p);
        p.getPersistentDataContainer().set(kXp, PersistentDataType.INTEGER, xp(p) + amount);
        int after = rank(p);
        if (after > before) {
            p.sendMessage(Component.text("✦ Your magic grows! Mage rank " + after + " - max mana is now "
                    + (int) max(p) + ".", NamedTextColor.LIGHT_PURPLE));
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.8f, 1.6f);
        }
    }

    // ------------------------------------------------------------------ mana

    public double max(Player p) {
        return pl.getConfig().getDouble("mana.base", 100) + rank(p) * pl.getConfig().getDouble("mana.per-rank", 10);
    }

    public double get(Player p) {
        return mana.getOrDefault(p.getUniqueId(), max(p));
    }

    public void set(Player p, double v) {
        mana.put(p.getUniqueId(), Math.max(0, Math.min(max(p), v)));
    }

    public boolean has(Player p, double cost) {
        return get(p) + 1e-6 >= cost;
    }

    public void take(Player p, double cost) {
        set(p, get(p) - cost);
        hud(p);
    }

    // ------------------------------------------------------------------ regen + HUD

    public void start() {
        Bukkit.getScheduler().runTaskTimer(pl, () -> {
            double regen = pl.getConfig().getDouble("mana.regen-per-second", 2.5) * 0.25;
            double affBonus = pl.getConfig().getDouble("mana.affinity-regen-bonus", 1.25);
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!mana.containsKey(p.getUniqueId())) load(p);
                double r = regen * (1.0 + rank(p) * 0.03);
                ItemStack main = p.getInventory().getItemInMainHand();
                if (Items.isStaff(main) && mage(p) != null && mage(p) == Items.element(main)) r *= affBonus;
                if (get(p) < max(p)) set(p, get(p) + r);
                if (Items.holdingMagic(p)) hud(p);
            }
        }, 20L, 5L);
    }

    public void hud(Player p) {
        double cur = get(p), max = max(p);
        int filled = (int) Math.round(cur / max * 10.0);
        ItemStack held = p.getInventory().getItemInMainHand();
        if (!Items.isArcanum(held)) held = p.getInventory().getItemInOffHand();
        Spell sel = Items.isArcanum(held) ? Items.selected(held) : null;
        TextColor barColor = sel != null ? sel.element.color() : TextColor.color(0x66CCFF);

        Component c = Component.text("✦ ", TextColor.color(0x66CCFF))
                .append(Component.text("▮".repeat(Math.max(0, filled)), barColor))
                .append(Component.text("▯".repeat(Math.max(0, 10 - filled)), NamedTextColor.DARK_GRAY))
                .append(Component.text(" " + (int) cur + "/" + (int) max, NamedTextColor.WHITE));
        if (sel != null) {
            c = c.append(Component.text("   ◆ ", NamedTextColor.DARK_GRAY))
                    .append(Component.text(sel.name, sel.element.color()).decorate(TextDecoration.BOLD))
                    .append(Component.text(" (" + (int) sel.mana + ")", NamedTextColor.GRAY));
        }
        p.sendActionBar(c);
    }
}
