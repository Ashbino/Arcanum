package com.thegildedking.arcanum;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public final class CastManager {
    public enum Source { STAFF, BOOK, SCROLL }

    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();

    /**
     * Tries to cast a spell for a player.
     *
     * @param manaMult cost multiplier (staff level discount); ignored for scrolls
     * @return true if the spell was cast
     */
    public boolean cast(Player p, Spell s, Source src, double manaMult) {
        Arcanum pl = Arcanum.get();
        if (pl.getConfig().getStringList("disabled-worlds").contains(p.getWorld().getName())) {
            Fx.fail(p, "Magic is silent in this world.");
            return false;
        }
        long now = System.currentTimeMillis();
        Map<String, Long> map = cooldowns.computeIfAbsent(p.getUniqueId(), k -> new HashMap<>());
        Long until = map.get(s.id);
        if (until != null && until > now) {
            Fx.fail(p, String.format("%s is recharging (%.1fs)", s.name, (until - now) / 1000.0));
            return false;
        }

        boolean affinity = pl.mana.affinity(p) == s.element;
        double affMult = pl.getConfig().getDouble("mana.affinity-cost-multiplier", 0.85);
        double cost = src == Source.SCROLL ? 0 : s.mana * manaMult * (affinity ? affMult : 1.0);
        if (cost > 0 && !pl.mana.has(p, cost)) {
            Fx.fail(p, "Not enough mana (" + (int) Math.ceil(cost) + " needed, you have " + (int) pl.mana.get(p) + ")");
            return false;
        }

        boolean ok;
        try {
            ok = s.effect.cast(p);
        } catch (Throwable t) {
            pl.getLogger().log(Level.WARNING, "Spell " + s.id + " failed", t);
            Fx.fail(p, "The spell fizzled.");
            return false;
        }
        if (!ok) return false;

        if (cost > 0) pl.mana.take(p, cost);
        double cdMult = pl.getConfig().getDouble("cooldown-multiplier", 1.0) * (affinity ? 0.9 : 1.0);
        map.put(s.id, now + (long) (s.cooldown * 50L * cdMult));
        pl.mana.addXp(p, 1 + s.tier());
        return true;
    }

    public void clear(Player p) {
        cooldowns.remove(p.getUniqueId());
    }
}
