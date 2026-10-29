package com.thegildedking.arcanum;

import org.bukkit.Material;
import org.bukkit.entity.Player;

public final class Spell {

    @FunctionalInterface
    public interface Effect {
        /** @return true if the spell was cast (mana + cooldown get used), false if it fizzled. */
        boolean cast(Player p);
    }

    public final String id;
    public final Element element;
    public final int index;      // 0..7 inside the element
    public final String name;
    public final String desc;
    public final double mana;
    public final int cooldown;   // ticks
    public final Material catalyst;
    public final Effect effect;

    public Spell(String id, Element element, int index, String name, String desc,
                 double mana, int cooldown, Material catalyst, Effect effect) {
        this.id = id;
        this.element = element;
        this.index = index;
        this.name = name;
        this.desc = desc;
        this.mana = mana;
        this.cooldown = cooldown;
        this.catalyst = catalyst;
        this.effect = effect;
    }

    /** Spell tier 1-4. A staff of level N can use every spell up to tier N. */
    public int tier() {
        return index / 2 + 1;
    }
}
