package com.thegildedking.arcanum;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public final class Arcanum extends JavaPlugin {

    private static Arcanum instance;

    public ManaManager mana;
    public CastManager casts;
    /** Temporary blocks placed by spells -> the block data to restore. */
    public final Map<Block, BlockData> temp = new HashMap<>();

    public static Arcanum get() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        Items.init(this);
        Spells.init();
        mana = new ManaManager(this);
        casts = new CastManager();
        Recipes.register(this);

        Bukkit.getPluginManager().registerEvents(new MageListener(this), this);
        Bukkit.getPluginManager().registerEvents(new GuiListener(this), this);
        Bukkit.getPluginManager().registerEvents(new CraftListener(this), this);

        PluginCommand cmd = getCommand("mage");
        if (cmd != null) {
            MageCommand mc = new MageCommand(this);
            cmd.setExecutor(mc);
            cmd.setTabCompleter(mc);
        }
        mana.start();
        for (var p : Bukkit.getOnlinePlayers()) {
            mana.load(p);
            Recipes.discover(p);
        }
        getLogger().info("Arcanum enabled with " + Spells.all().size() + " spells.");
    }

    @Override
    public void onDisable() {
        if (mana != null) mana.saveAll();
        for (Block b : new ArrayList<>(temp.keySet())) Fx.revert(b);
        Recipes.unregister();
    }
}
