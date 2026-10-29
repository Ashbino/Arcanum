package com.thegildedking.arcanum;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.Predicate;

/** Shared helpers for spell effects. */
public final class Fx {
    private Fx() {}

    public static final Random R = new Random();

    public static Arcanum pl() {
        return Arcanum.get();
    }

    // ------------------------------------------------------------------ feedback

    public static void sound(Location l, String key, float vol, float pitch) {
        l.getWorld().playSound(l, key, SoundCategory.PLAYERS, vol, pitch);
    }

    public static void part(Location l, Particle p, int n, double dx, double dy, double dz, double speed) {
        l.getWorld().spawnParticle(p, l, n, dx, dy, dz, speed);
    }

    public static void dust(Location l, int rgb, float size, int n, double spread) {
        l.getWorld().spawnParticle(Particle.DUST, l, n, spread, spread, spread, 0,
                new Particle.DustOptions(Color.fromRGB(rgb), size));
    }

    public static void block(Location l, Material m, int n, double spread) {
        l.getWorld().spawnParticle(Particle.BLOCK, l, n, spread, spread, spread, 0.05, m.createBlockData());
    }

    public static void fail(Player p, String msg) {
        p.sendActionBar(Component.text(msg, NamedTextColor.RED));
        p.playSound(p.getLocation(), "block.note_block.bass", SoundCategory.PLAYERS, 0.6f, 0.6f);
    }

    // ------------------------------------------------------------------ targeting

    public static boolean ally(Player c, Entity e) {
        if (!(e instanceof Player o)) return false;
        var t = c.getScoreboard().getEntryTeam(c.getName());
        return t != null && t.hasEntry(o.getName());
    }

    public static boolean valid(Player c, Entity e) {
        if (!(e instanceof LivingEntity le)) return false;
        if (e.equals(c) || le.isDead() || e instanceof ArmorStand) return false;
        if (e instanceof Player o) {
            if (o.getGameMode() == GameMode.SPECTATOR || ally(c, o)) return false;
            if (!pl().getConfig().getBoolean("pvp", true)) return false;
        }
        return !le.isInvulnerable();
    }

    public static Predicate<Entity> filter(Player c) {
        return e -> valid(c, e);
    }

    public static RayTraceResult trace(Player p, double range, double size) {
        Location eye = p.getEyeLocation();
        return p.getWorld().rayTrace(eye, eye.getDirection(), range, FluidCollisionMode.NEVER, true, size, filter(p));
    }

    /** Where the player is looking: the entity/block hit point or the end of the range. */
    public static Location aim(Player p, double range) {
        RayTraceResult r = trace(p, range, 0.3);
        if (r != null && r.getHitPosition() != null) return r.getHitPosition().toLocation(p.getWorld());
        Location eye = p.getEyeLocation();
        return eye.clone().add(eye.getDirection().multiply(range));
    }

    public static LivingEntity target(Player p, double range) {
        RayTraceResult r = trace(p, range, 0.6);
        if (r != null && r.getHitEntity() instanceof LivingEntity le) return le;
        return null;
    }

    public static Vector flat(Player p) {
        Vector d = p.getEyeLocation().getDirection().setY(0);
        if (d.lengthSquared() < 0.0001) d = new Vector(0, 0, 1);
        return d.normalize();
    }

    public static Location center(Entity e) {
        return e.getBoundingBox().getCenter().toLocation(e.getWorld());
    }

    public static List<LivingEntity> near(Player c, Location l, double r) {
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : l.getWorld().getNearbyLivingEntities(l, r)) {
            if (valid(c, e)) out.add(e);
        }
        return out;
    }

    /** The caster plus allies (teammates, or everyone if buff-nearby-players is on). */
    public static List<Player> friends(Player c, Location l, double r) {
        List<Player> out = new ArrayList<>();
        out.add(c);
        boolean all = pl().getConfig().getBoolean("buff-nearby-players", false);
        for (Player o : l.getWorld().getPlayers()) {
            if (o.equals(c) || o.isDead() || o.getGameMode() == GameMode.SPECTATOR) continue;
            if (o.getLocation().distanceSquared(l) > r * r) continue;
            if (all || ally(c, o)) out.add(o);
        }
        return out;
    }

    public static List<LivingEntity> cone(Player p, double range, double minDot) {
        Location eye = p.getEyeLocation();
        Vector d = eye.getDirection().normalize();
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : near(p, eye, range)) {
            Vector to = center(e).toVector().subtract(eye.toVector());
            if (to.lengthSquared() < 1.0 || to.normalize().dot(d) >= minDot) out.add(e);
        }
        return out;
    }

    // ------------------------------------------------------------------ combat helpers

    public static void hurt(Player c, LivingEntity e, double dmg) {
        e.setNoDamageTicks(0);
        e.damage(dmg, c);
    }

    public static void push(LivingEntity e, Vector v) {
        e.setVelocity(e.getVelocity().add(v));
    }

    public static void pushAway(LivingEntity e, Location from, double power, double up) {
        Vector v = e.getLocation().toVector().subtract(from.toVector());
        v.setY(0);
        if (v.lengthSquared() < 0.0001) v = new Vector(R.nextDouble() - 0.5, 0, R.nextDouble() - 0.5);
        Vector out = v.normalize().multiply(power);
        out.setY(up);
        push(e, out);
    }

    public static void fx(LivingEntity e, PotionEffectType t, int ticks, int amp) {
        e.addPotionEffect(new PotionEffect(t, ticks, amp, false, true, true));
    }

    public static void heal(LivingEntity e, double amt) {
        AttributeInstance a = e.getAttribute(Attribute.MAX_HEALTH);
        double max = a == null ? 20 : a.getValue();
        e.setHealth(Math.min(max, e.getHealth() + amt));
        part(center(e), Particle.HEART, 2, 0.3, 0.3, 0.3, 0);
    }

    public static boolean undead(Entity e) {
        return UNDEAD.contains(e.getType().name());
    }

    private static final Set<String> UNDEAD = Set.of("ZOMBIE", "SKELETON", "WITHER_SKELETON", "HUSK", "DROWNED",
            "STRAY", "ZOMBIE_VILLAGER", "PHANTOM", "WITHER", "ZOGLIN", "ZOMBIFIED_PIGLIN", "SKELETON_HORSE",
            "ZOMBIE_HORSE", "BOGGED", "CAMEL_HUSK", "PARCHED", "ZOMBIE_NAUTILUS");

    public static void blast(Player c, Location l, double r, double dmg, int fire, double knock) {
        l.getWorld().spawnParticle(Particle.EXPLOSION, l, 1);
        sound(l, "entity.generic.explode", 0.8f, 1.2f);
        for (LivingEntity e : near(c, l, r)) {
            double f = 1.0 - Math.min(1.0, e.getLocation().distance(l) / (r + 0.01)) * 0.4;
            hurt(c, e, dmg * f);
            if (fire > 0) e.setFireTicks(fire);
            if (knock > 0) pushAway(e, l, knock, 0.35);
        }
        if (pl().getConfig().getBoolean("block-damage", false)) {
            l.getWorld().createExplosion(l, (float) Math.min(4.0, r * 0.8), false, true, c);
        }
    }

    public static void dash(Player p, double power) {
        Vector v = p.getEyeLocation().getDirection().normalize().multiply(power);
        if (v.getY() < 0.1) v.setY(0.1);
        p.setVelocity(v);
        p.setFallDistance(0);
    }

    // ------------------------------------------------------------------ geometry / fx

    public static void line(Location a, Location b, Consumer<Location> per) {
        Vector d = b.toVector().subtract(a.toVector());
        double len = d.length();
        if (len < 0.01) return;
        d.normalize();
        for (double t = 0; t <= len; t += 0.4) per.accept(a.clone().add(d.clone().multiply(t)));
    }

    public static void ring(Location c, double r, int pts, Consumer<Location> per) {
        for (int i = 0; i < pts; i++) {
            double a = 2 * Math.PI * i / pts;
            per.accept(c.clone().add(Math.cos(a) * r, 0.15, Math.sin(a) * r));
        }
    }

    public static void expandRing(Location c, double max, int ticks, Consumer<Location> per) {
        repeat(ticks, 1, i -> {
            double r = max * (i + 1) / ticks;
            ring(c, r, Math.max(12, (int) (r * 8)), per);
        });
    }

    public static Location ground(Location l) {
        Location g = l.clone();
        int tries = 0;
        while (tries < 8 && g.getBlock().getType().isSolid()) {
            g.add(0, 1, 0);
            tries++;
        }
        tries = 0;
        while (tries < 8 && g.getY() > g.getWorld().getMinHeight()
                && !g.clone().add(0, -1, 0).getBlock().getType().isSolid()) {
            g.add(0, -1, 0);
            tries++;
        }
        return g;
    }

    public static boolean safe(Location l) {
        Block feet = l.getBlock();
        Block head = l.clone().add(0, 1, 0).getBlock();
        return feet.isPassable() && head.isPassable() && !feet.isLiquid();
    }

    // ------------------------------------------------------------------ scheduling

    public static void later(int ticks, Runnable r) {
        Bukkit.getScheduler().runTaskLater(pl(), r, ticks);
    }

    public static void repeat(int times, int period, IntConsumer body) {
        new BukkitRunnable() {
            int i = 0;

            @Override
            public void run() {
                if (i >= times) {
                    cancel();
                    return;
                }
                try {
                    body.accept(i++);
                } catch (Throwable t) {
                    pl().getLogger().warning("Spell task error: " + t);
                    cancel();
                }
            }
        }.runTaskTimer(pl(), 0, period);
    }

    // ------------------------------------------------------------------ projectiles and beams

    public static void projectile(Player p, double speed, int maxTicks,
                                  Consumer<Location> trail, BiConsumer<Location, LivingEntity> onHit) {
        World w = p.getWorld();
        Location eye = p.getEyeLocation();
        Vector dir = eye.getDirection().normalize();
        Location pos = eye.clone().add(dir.clone().multiply(0.6));
        new BukkitRunnable() {
            int n = 0;

            @Override
            public void run() {
                try {
                    RayTraceResult r = w.rayTrace(pos, dir, speed, FluidCollisionMode.NEVER, true, 0.25, filter(p));
                    double len = speed;
                    if (r != null && r.getHitPosition() != null) {
                        Location hit = r.getHitPosition().toLocation(w);
                        len = pos.distance(hit);
                        for (double t = 0; t <= len; t += 0.4) trail.accept(pos.clone().add(dir.clone().multiply(t)));
                        LivingEntity le = r.getHitEntity() instanceof LivingEntity l2 ? l2 : null;
                        onHit.accept(hit, le);
                        cancel();
                        return;
                    }
                    for (double t = 0; t <= len; t += 0.4) trail.accept(pos.clone().add(dir.clone().multiply(t)));
                    pos.add(dir.clone().multiply(speed));
                    if (++n > maxTicks || !p.isOnline()) cancel();
                } catch (Throwable t) {
                    pl().getLogger().warning("Projectile error: " + t);
                    cancel();
                }
            }
        }.runTaskTimer(pl(), 0, 1);
    }

    /** Draws a beam from the eye and returns everything it touches. */
    public static Set<LivingEntity> beam(Player p, double range, Consumer<Location> trail, double radius) {
        Location eye = p.getEyeLocation();
        Vector d = eye.getDirection().normalize();
        World w = p.getWorld();
        RayTraceResult br = w.rayTraceBlocks(eye, d, range, FluidCollisionMode.NEVER, true);
        double len = br == null || br.getHitPosition() == null ? range
                : eye.distance(br.getHitPosition().toLocation(w));
        Set<LivingEntity> hit = new LinkedHashSet<>();
        for (double t = 0.8; t <= len; t += 0.5) {
            Location pt = eye.clone().add(d.clone().multiply(t));
            trail.accept(pt);
            for (LivingEntity e : w.getNearbyLivingEntities(pt, radius)) {
                if (valid(p, e)) hit.add(e);
            }
        }
        return hit;
    }

    // ------------------------------------------------------------------ temporary blocks

    public static boolean tempBlock(Location l, BlockData data, int ticks) {
        if (!pl().getConfig().getBoolean("temporary-blocks", true)) return false;
        Block b = l.getBlock();
        if (!b.isEmpty() || pl().temp.containsKey(b)) return false;
        pl().temp.put(b, b.getBlockData());
        b.setBlockData(data, false);
        later(ticks, () -> revert(b));
        return true;
    }

    public static void revert(Block b) {
        BlockData old = pl().temp.remove(b);
        if (old != null) b.setBlockData(old, false);
    }
}
