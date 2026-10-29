package com.thegildedking.arcanum;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class MageCommand implements CommandExecutor, TabCompleter {
    private final Arcanum pl;

    public MageCommand(Arcanum pl) {
        this.pl = pl;
    }

    private static void msg(CommandSender s, String text, NamedTextColor c) {
        s.sendMessage(Component.text(text, c));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] a) {
        if (a.length == 0) {
            help(sender);
            return true;
        }
        String sub = a[0].toLowerCase(Locale.ROOT);
        boolean admin = sender.hasPermission("arcanum.admin");

        switch (sub) {
            case "info" -> {
                if (!(sender instanceof Player p)) return true;
                Element m = pl.mana.mage(p);
                msg(sender, "Mage: " + (m == null ? "not chosen" : m.title + " (" + m.display + ")"), NamedTextColor.GOLD);
                msg(sender, "Rank " + pl.mana.rank(p) + "  -  Mana " + (int) pl.mana.get(p) + "/" + (int) pl.mana.max(p),
                        NamedTextColor.AQUA);
            }
            case "choose" -> {
                if (!(sender instanceof Player p)) return true;
                if (pl.mana.mage(p) != null) msg(sender, "You already chose your path.", NamedTextColor.RED);
                else ChooseGui.open(p);
            }
            case "spells" -> {
                Element e = a.length > 1 ? Element.byId(a[1]) : null;
                if (e == null && sender instanceof Player p) e = pl.mana.mage(p);
                if (e == null) {
                    msg(sender, "Usage: /mage spells <element>", NamedTextColor.RED);
                    return true;
                }
                msg(sender, e.display + " spells:", NamedTextColor.GOLD);
                for (Spell s : Spells.of(e)) {
                    msg(sender, " Tier " + Items.ROMAN[s.tier() - 1] + "  " + s.name + " - " + s.desc + " (" + (int) s.mana + " mana)",
                            NamedTextColor.GRAY);
                }
            }
            case "give" -> {
                if (!admin) return noPerm(sender);
                give(sender, a);
            }
            case "setmana" -> {
                if (!admin) return noPerm(sender);
                if (a.length < 3) {
                    msg(sender, "Usage: /mage setmana <player> <amount>", NamedTextColor.RED);
                    return true;
                }
                Player t = Bukkit.getPlayerExact(a[1]);
                if (t == null) {
                    msg(sender, "Player not found.", NamedTextColor.RED);
                    return true;
                }
                try {
                    pl.mana.set(t, Double.parseDouble(a[2]));
                    msg(sender, "Mana set.", NamedTextColor.GREEN);
                } catch (NumberFormatException ex) {
                    msg(sender, "Not a number.", NamedTextColor.RED);
                }
            }
            case "setmage" -> {
                if (!admin) return noPerm(sender);
                if (a.length < 3) {
                    msg(sender, "Usage: /mage setmage <player> <element|none>", NamedTextColor.RED);
                    return true;
                }
                Player t = Bukkit.getPlayerExact(a[1]);
                if (t == null) {
                    msg(sender, "Player not found.", NamedTextColor.RED);
                    return true;
                }
                pl.mana.setMage(t, Element.byId(a[2]));
                msg(sender, "Mage updated.", NamedTextColor.GREEN);
            }
            case "reset" -> {
                if (!admin) return noPerm(sender);
                if (a.length < 2) {
                    msg(sender, "Usage: /mage reset <player>", NamedTextColor.RED);
                    return true;
                }
                Player t = Bukkit.getPlayerExact(a[1]);
                if (t == null) {
                    msg(sender, "Player not found.", NamedTextColor.RED);
                    return true;
                }
                pl.mana.setMage(t, null);
                pl.mana.resetProgress(t);
                msg(sender, "Reset. They will see the choose popup again on /mage choose.", NamedTextColor.GREEN);
            }
            case "pack" -> {
                if (!(sender instanceof Player p)) return true;
                String url = pl.getConfig().getString("resource-pack.url", "");
                if (url == null || url.isBlank()) {
                    msg(sender, "No resource pack URL is set in config.yml.", NamedTextColor.RED);
                } else {
                    String sha1 = pl.getConfig().getString("resource-pack.sha1", "");
                    if (sha1 != null && sha1.length() == 40) p.setResourcePack(url, sha1);
                    else p.setResourcePack(url);
                }
            }
            case "reload" -> {
                if (!admin) return noPerm(sender);
                pl.reloadConfig();
                msg(sender, "Arcanum config reloaded.", NamedTextColor.GREEN);
            }
            default -> help(sender);
        }
        return true;
    }

    private boolean noPerm(CommandSender s) {
        msg(s, "You don't have permission.", NamedTextColor.RED);
        return true;
    }

    private void help(CommandSender s) {
        msg(s, "Arcanum commands:", NamedTextColor.GOLD);
        msg(s, " /mage info  /mage choose  /mage spells [element]  /mage pack", NamedTextColor.GRAY);
        if (s.hasPermission("arcanum.admin")) {
            msg(s, " /mage give <player> staff <element> [level]", NamedTextColor.GRAY);
            msg(s, " /mage give <player> multi [level] | scroll <spell> [amount] | book <spell> | crystal <tier>", NamedTextColor.GRAY);
            msg(s, " /mage setmana <player> <n>  /mage setmage <player> <element|none>  /mage reset <player>  /mage reload",
                    NamedTextColor.GRAY);
        }
    }

    private void give(CommandSender sender, String[] a) {
        if (a.length < 3) {
            msg(sender, "Usage: /mage give <player> <staff|multi|scroll|book|crystal> ...", NamedTextColor.RED);
            return;
        }
        Player t = Bukkit.getPlayerExact(a[1]);
        if (t == null) {
            msg(sender, "Player not found.", NamedTextColor.RED);
            return;
        }
        ItemStack item = null;
        String kind = a[2].toLowerCase(Locale.ROOT);
        try {
            switch (kind) {
                case "staff" -> {
                    Element e = a.length > 3 ? Element.byId(a[3]) : null;
                    if (e == null) {
                        msg(sender, "Unknown element.", NamedTextColor.RED);
                        return;
                    }
                    item = Items.staff(e, a.length > 4 ? Integer.parseInt(a[4]) : 1);
                }
                case "multi" -> item = Items.multi(a.length > 3 ? Integer.parseInt(a[3]) : 2);
                case "scroll", "book" -> {
                    Spell s = a.length > 3 ? Spells.get(a[3].toLowerCase(Locale.ROOT)) : null;
                    if (s == null) {
                        msg(sender, "Unknown spell id (example: fire_fireball). Use tab-complete.", NamedTextColor.RED);
                        return;
                    }
                    item = kind.equals("scroll") ? Items.scroll(s) : Items.book(s);
                    if (kind.equals("scroll") && a.length > 4) item.setAmount(Math.max(1, Math.min(16, Integer.parseInt(a[4]))));
                }
                case "crystal" -> item = Items.crystal(a.length > 3 ? Integer.parseInt(a[3]) : 2);
                default -> {
                    msg(sender, "Unknown item type.", NamedTextColor.RED);
                    return;
                }
            }
        } catch (NumberFormatException ex) {
            msg(sender, "Not a number.", NamedTextColor.RED);
            return;
        }
        Map<Integer, ItemStack> left = t.getInventory().addItem(item);
        for (ItemStack rest : left.values()) t.getWorld().dropItemNaturally(t.getLocation(), rest);
        msg(sender, "Gave " + t.getName() + " a " + kind + ".", NamedTextColor.GREEN);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] a) {
        List<String> out = new ArrayList<>();
        boolean admin = sender.hasPermission("arcanum.admin");
        if (a.length == 1) {
            List<String> subs = new ArrayList<>(List.of("info", "choose", "spells", "pack"));
            if (admin) subs.addAll(List.of("give", "setmana", "setmage", "reset", "reload"));
            add(out, subs, a[0]);
        } else if (a.length == 2) {
            if (a[0].equalsIgnoreCase("spells")) add(out, elementIds(), a[1]);
            else if (admin && List.of("give", "setmana", "setmage", "reset").contains(a[0].toLowerCase(Locale.ROOT))) {
                List<String> names = new ArrayList<>();
                for (Player p : Bukkit.getOnlinePlayers()) names.add(p.getName());
                add(out, names, a[1]);
            }
        } else if (admin && a.length == 3) {
            if (a[0].equalsIgnoreCase("give")) add(out, List.of("staff", "multi", "scroll", "book", "crystal"), a[2]);
            if (a[0].equalsIgnoreCase("setmage")) {
                List<String> ids = elementIds();
                ids.add("none");
                add(out, ids, a[2]);
            }
        } else if (admin && a.length == 4 && a[0].equalsIgnoreCase("give")) {
            String k = a[2].toLowerCase(Locale.ROOT);
            if (k.equals("staff")) add(out, elementIds(), a[3]);
            if (k.equals("scroll") || k.equals("book")) {
                List<String> ids = new ArrayList<>();
                for (Spell s : Spells.all()) ids.add(s.id);
                add(out, ids, a[3]);
            }
            if (k.equals("crystal")) add(out, List.of("2", "3", "4"), a[3]);
            if (k.equals("multi")) add(out, List.of("2", "3", "4"), a[3]);
        }
        return out;
    }

    private static List<String> elementIds() {
        List<String> ids = new ArrayList<>();
        for (Element e : Element.values()) ids.add(e.id);
        return ids;
    }

    private static void add(List<String> out, List<String> options, String prefix) {
        String p = prefix.toLowerCase(Locale.ROOT);
        for (String o : options) if (o.toLowerCase(Locale.ROOT).startsWith(p)) out.add(o);
    }
}
