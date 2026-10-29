package com.thegildedking.arcanum;

import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;

public enum Element {
    FIRE("fire", "Fire", "Pyromancer", 0xFF6A1A, Material.MAGMA_CREAM, "Scorch foes with blasts, walls of flame and meteors."),
    ICE("ice", "Ice", "Cryomancer", 0x6FD8FF, Material.BLUE_ICE, "Freeze, slow and shatter. Control the battlefield."),
    DARK("dark", "Dark", "Umbramancer", 0xA24FE0, Material.CRYING_OBSIDIAN, "Drain life, hex enemies and slip through shadow."),
    LIGHT("light", "Light", "Lightbringer", 0xFFF2A8, Material.GLOWSTONE, "Heal allies, smite evil and call down judgement."),
    LIGHTNING("lightning", "Lightning", "Stormcaller", 0xFFE033, Material.LIGHTNING_ROD, "Fast, chaining strikes and thunderous power."),
    NATURE("nature", "Nature", "Druid", 0x55CF3C, Material.MOSS_BLOCK, "Poison, roots, regrowth and ancient guardians."),
    WIND("wind", "Wind", "Windweaver", 0xB8F0E6, Material.PHANTOM_MEMBRANE, "Mobility, knockback and storms of air."),
    EARTH("earth", "Earth", "Geomancer", 0xC49A5A, Material.DRIPSTONE_BLOCK, "Stone skin, quakes and crushing landslides.");

    public final String id;
    public final String display;
    public final String title;
    public final int rgb;
    public final Material focus;
    public final String blurb;

    Element(String id, String display, String title, int rgb, Material focus, String blurb) {
        this.id = id;
        this.display = display;
        this.title = title;
        this.rgb = rgb;
        this.focus = focus;
        this.blurb = blurb;
    }

    public TextColor color() {
        return TextColor.color(rgb);
    }

    public static Element byId(String s) {
        if (s == null) return null;
        for (Element e : values()) if (e.id.equalsIgnoreCase(s)) return e;
        return null;
    }
}
