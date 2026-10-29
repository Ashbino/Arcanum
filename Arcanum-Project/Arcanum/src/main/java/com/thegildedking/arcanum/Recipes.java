package com.thegildedking.arcanum;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;

import java.util.ArrayList;
import java.util.List;

public final class Recipes {
    private Recipes() {}

    private static final List<NamespacedKey> KEYS = new ArrayList<>();

    private static NamespacedKey key(Arcanum pl, String s) {
        NamespacedKey k = new NamespacedKey(pl, s);
        KEYS.add(k);
        return k;
    }

    public static void register(Arcanum pl) {
        unregister();

        // Level 1 staffs: one per element
        for (Element e : Element.values()) {
            ShapedRecipe r = new ShapedRecipe(key(pl, "staff_" + e.id), Items.staff(e, 1));
            r.shape("GFG", " S ", " S ");
            r.setIngredient('G', Material.GOLD_INGOT);
            r.setIngredient('F', e.focus);
            r.setIngredient('S', Material.STICK);
            Bukkit.addRecipe(r);
        }

        // Scrolls: paper + element focus + a unique catalyst (+ lapis for higher tiers)
        for (Spell s : Spells.all()) {
            ShapelessRecipe r = new ShapelessRecipe(key(pl, "scroll_" + s.id), Items.scroll(s));
            r.addIngredient(Material.PAPER);
            r.addIngredient(s.element.focus);
            r.addIngredient(s.catalyst);
            int lapis = s.tier() - 1;
            if (lapis > 0) r.addIngredient(lapis, Material.LAPIS_LAZULI);
            Bukkit.addRecipe(r);
        }

        // Arcane Crystals
        crystal(pl, 2, new String[]{"ADA", "DLD", "ADA"},
                'A', Material.AMETHYST_SHARD, 'D', Material.DIAMOND, 'L', Material.LAPIS_BLOCK);
        crystal(pl, 3, new String[]{"ABA", "BEB", "ABA"},
                'A', Material.AMETHYST_BLOCK, 'B', Material.DIAMOND_BLOCK, 'E', Material.EMERALD_BLOCK);
        crystal(pl, 4, new String[]{"ANA", "NSN", "ANA"},
                'A', Material.DIAMOND_BLOCK, 'N', Material.NETHERITE_INGOT, 'S', Material.NETHER_STAR);

        // Dummy recipes - the real result is computed in CraftListener
        ShapelessRecipe upgrade = new ShapelessRecipe(key(pl, "upgrade"), new ItemStack(Material.BLAZE_ROD));
        upgrade.addIngredient(Material.BLAZE_ROD);
        upgrade.addIngredient(Material.AMETHYST_SHARD);
        Bukkit.addRecipe(upgrade);

        ShapelessRecipe book = new ShapelessRecipe(key(pl, "book"), new ItemStack(Material.BOOK));
        book.addIngredient(Material.BOOK);
        book.addIngredient(Material.PAPER);
        book.addIngredient(Material.LAPIS_LAZULI);
        Bukkit.addRecipe(book);

        ShapedRecipe multi = new ShapedRecipe(key(pl, "multi"), Items.multi(2));
        multi.shape("BBB", "BAB", "BBB");
        multi.setIngredient('B', Material.BLAZE_ROD);
        multi.setIngredient('A', Material.AMETHYST_SHARD);
        Bukkit.addRecipe(multi);
    }

    private static void crystal(Arcanum pl, int tier, String[] shape, Object... ing) {
        ShapedRecipe r = new ShapedRecipe(key(pl, "crystal_" + tier), Items.crystal(tier));
        r.shape(shape);
        for (int i = 0; i < ing.length; i += 2) r.setIngredient((Character) ing[i], (Material) ing[i + 1]);
        Bukkit.addRecipe(r);
    }

    public static void discover(Player p) {
        p.discoverRecipes(KEYS);
    }

    public static void unregister() {
        for (NamespacedKey k : KEYS) Bukkit.removeRecipe(k);
        KEYS.clear();
    }
}
