package com.thegildedking.arcanum;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.function.Consumer;

/** All 64 spells: 8 elements x 8 spells. Spell index 0-1 = tier 1, 2-3 = tier 2, 4-5 = tier 3, 6-7 = tier 4. */
public final class Spells {
    private Spells() {}

    private static final Map<String, Spell> ALL = new LinkedHashMap<>();
    private static final Map<Element, List<Spell>> BY = new EnumMap<>(Element.class);

    public static Spell get(String id) {
        return id == null ? null : ALL.get(id);
    }

    public static Collection<Spell> all() {
        return ALL.values();
    }

    public static List<Spell> of(Element e) {
        return BY.getOrDefault(e, List.of());
    }

    private static Spell.Effect run(Consumer<Player> c) {
        return p -> {
            c.accept(p);
            return true;
        };
    }

    private static void reg(Element e, int idx, String name, String desc, double mana, int cd,
                            Material catalyst, Spell.Effect fx) {
        String id = e.id + "_" + name.toLowerCase().replace("'", "").replace(" ", "_");
        Spell s = new Spell(id, e, idx, name, desc, mana, cd, catalyst, fx);
        ALL.put(id, s);
        BY.computeIfAbsent(e, k -> new ArrayList<>()).add(s);
    }

    public static void init() {
        ALL.clear();
        BY.clear();
        fire();
        ice();
        dark();
        light();
        lightning();
        nature();
        wind();
        earth();
    }

    // =====================================================================================
    //  FIRE
    // =====================================================================================
    private static void fire() {
        Element E = Element.FIRE;

        reg(E, 0, "Fireball", "Hurl an exploding fireball.", 12, 25, Material.FIRE_CHARGE, run(p -> {
            Fx.sound(p.getLocation(), "entity.blaze.shoot", 1f, 0.8f);
            Fx.projectile(p, 1.3, 40, l -> {
                Fx.part(l, Particle.FLAME, 3, 0.1, 0.1, 0.1, 0.01);
                Fx.part(l, Particle.SMOKE, 1, 0.05, 0.05, 0.05, 0);
            }, (l, e) -> {
                Fx.blast(p, l, 3.0, 7, 80, 0.6);
                Fx.part(l, Particle.FLAME, 40, 0.8, 0.8, 0.8, 0.05);
            });
        }));

        reg(E, 1, "Flame Burst", "Breathe a cone of flames.", 18, 40, Material.COAL, run(p -> {
            Fx.sound(p.getLocation(), "entity.blaze.shoot", 1f, 0.6f);
            Location eye = p.getEyeLocation();
            Vector d = eye.getDirection();
            for (int i = 0; i < 40; i++) {
                double t = 1 + Fx.R.nextDouble() * 6;
                Vector s = d.clone().add(new Vector(Fx.R.nextGaussian() * 0.18, Fx.R.nextGaussian() * 0.18,
                        Fx.R.nextGaussian() * 0.18)).normalize();
                Fx.part(eye.clone().add(s.multiply(t)), Particle.FLAME, 1, 0, 0, 0, 0);
            }
            for (LivingEntity e : Fx.cone(p, 7, 0.75)) {
                Fx.hurt(p, e, 5);
                e.setFireTicks(100);
                Fx.pushAway(e, p.getLocation(), 0.5, 0.2);
            }
        }));

        reg(E, 2, "Ember Shield", "Wreath yourself in flames that scorch anyone near you.", 25, 200,
                Material.GUNPOWDER, run(p -> {
                    Fx.fx(p, PotionEffectType.FIRE_RESISTANCE, 220, 0);
                    Fx.sound(p.getLocation(), "item.firecharge.use", 1f, 1f);
                    Fx.repeat(40, 5, i -> {
                        if (!p.isOnline() || p.isDead()) return;
                        Fx.ring(p.getLocation(), 1.6, 14, l ->
                                Fx.part(l.clone().add(0, 0.4 + (i % 3) * 0.5, 0), Particle.FLAME, 1, 0, 0, 0, 0));
                        for (LivingEntity e : Fx.near(p, p.getLocation(), 3)) {
                            if (i % 2 == 0) Fx.hurt(p, e, 2);
                            e.setFireTicks(60);
                        }
                    });
                }));

        reg(E, 3, "Fire Wall", "Raise a wall of fire that burns everything crossing it.", 30, 200,
                Material.NETHER_BRICK, run(p -> {
                    final Location c = Fx.ground(Fx.aim(p, 12));
                    final Vector d = Fx.flat(p);
                    final Vector side = new Vector(-d.getZ(), 0, d.getX());
                    Fx.sound(c, "item.firecharge.use", 1f, 0.7f);
                    Fx.repeat(20, 5, i -> {
                        for (double o = -4; o <= 4; o += 0.5) {
                            Location l = c.clone().add(side.clone().multiply(o));
                            Fx.part(l.clone().add(0, 0.5, 0), Particle.FLAME, 2, 0.1, 0.5, 0.1, 0.01);
                        }
                        for (LivingEntity e : Fx.near(p, c, 5.5)) {
                            Vector rel = e.getLocation().toVector().subtract(c.toVector());
                            if (Math.abs(rel.dot(side)) <= 4.2 && Math.abs(rel.dot(d)) <= 1.3 && Math.abs(rel.getY()) < 3) {
                                Fx.hurt(p, e, 2);
                                e.setFireTicks(80);
                            }
                        }
                    });
                }));

        reg(E, 4, "Meteor", "Call a meteor down from the sky.", 45, 300, Material.MAGMA_BLOCK, run(p -> {
            final Location t = Fx.aim(p, 32);
            final Location sky = t.clone().add(0, 22, 0);
            Fx.sound(t, "entity.blaze.shoot", 1.5f, 0.5f);
            Fx.repeat(22, 1, i -> {
                Location l = sky.clone().subtract(0, i, 0);
                Fx.part(l, Particle.FLAME, 12, 0.5, 0.5, 0.5, 0.02);
                Fx.part(l, Particle.LARGE_SMOKE, 4, 0.4, 0.4, 0.4, 0.01);
                Fx.ring(t, 3, 16, r -> Fx.part(r, Particle.FLAME, 1, 0, 0, 0, 0));
                if (i == 21) {
                    Fx.blast(p, t, 5.5, 15, 120, 1.0);
                    Fx.part(t, Particle.EXPLOSION_EMITTER, 1, 0, 0, 0, 0);
                    Fx.part(t, Particle.LAVA, 30, 2, 0.5, 2, 0);
                }
            });
        }));

        reg(E, 5, "Inferno Nova", "Detonate in a ring of fire around you.", 40, 300, Material.GHAST_TEAR, run(p -> {
            final Location c = p.getLocation();
            Fx.sound(c, "entity.blaze.shoot", 1.5f, 0.5f);
            Fx.expandRing(c, 7, 10, l -> Fx.part(l, Particle.FLAME, 2, 0.1, 0.2, 0.1, 0.02));
            Fx.later(4, () -> {
                for (LivingEntity e : Fx.near(p, c, 7)) {
                    Fx.hurt(p, e, 10);
                    e.setFireTicks(140);
                    Fx.pushAway(e, c, 1.0, 0.5);
                }
                Fx.sound(c, "entity.generic.explode", 1f, 1f);
            });
        }));

        reg(E, 6, "Phoenix Dash", "Surge forward as a trail of living flame.", 55, 160, Material.BLAZE_ROD, run(p -> {
            Fx.dash(p, 2.0);
            Fx.fx(p, PotionEffectType.RESISTANCE, 40, 1);
            Fx.sound(p.getLocation(), "entity.blaze.shoot", 1f, 1.4f);
            final Set<UUID> hit = new HashSet<>();
            Fx.repeat(10, 2, i -> {
                Location l = p.getLocation();
                Fx.part(l.clone().add(0, 1, 0), Particle.FLAME, 15, 0.5, 0.6, 0.5, 0.03);
                Fx.part(l, Particle.LAVA, 2, 0.3, 0.1, 0.3, 0);
                for (LivingEntity e : Fx.near(p, l, 2.5)) {
                    if (hit.add(e.getUniqueId())) {
                        Fx.hurt(p, e, 8);
                        e.setFireTicks(100);
                    }
                }
            });
        }));

        reg(E, 7, "Dragonfire", "Unleash a sustained beam of searing dragonfire.", 85, 400, Material.NETHER_WART, run(p -> {
            Fx.sound(p.getLocation(), "entity.ender_dragon.shoot", 1.5f, 0.8f);
            Fx.repeat(24, 2, i -> {
                if (!p.isOnline() || p.isDead()) return;
                Set<LivingEntity> hit = Fx.beam(p, 16, l -> {
                    Fx.part(l, Particle.FLAME, 3, 0.15, 0.15, 0.15, 0.01);
                    if (i % 3 == 0) Fx.part(l, Particle.SOUL_FIRE_FLAME, 1, 0.1, 0.1, 0.1, 0);
                }, 1.0);
                for (LivingEntity e : hit) {
                    Fx.hurt(p, e, 1.6);
                    e.setFireTicks(60);
                }
            });
        }));
    }

    // =====================================================================================
    //  ICE
    // =====================================================================================
    private static void ice() {
        Element E = Element.ICE;

        reg(E, 0, "Frost Bolt", "Launch a bolt of ice that chills the target.", 10, 20, Material.SNOWBALL, run(p -> {
            Fx.sound(p.getLocation(), "entity.snowball.throw", 1f, 0.7f);
            Fx.projectile(p, 1.4, 40, l -> {
                Fx.part(l, Particle.SNOWFLAKE, 3, 0.1, 0.1, 0.1, 0.01);
                Fx.dust(l, 0x8FE3FF, 1f, 2, 0.1);
            }, (l, e) -> {
                Fx.part(l, Particle.SNOWFLAKE, 30, 0.5, 0.5, 0.5, 0.1);
                Fx.sound(l, "block.glass.break", 1f, 1.3f);
                if (e != null) {
                    Fx.hurt(p, e, 5);
                    Fx.fx(e, PotionEffectType.SLOWNESS, 80, 2);
                    e.setFreezeTicks(120);
                }
            });
        }));

        reg(E, 1, "Frost Nova", "Burst outward, chilling everything near you.", 18, 80, Material.SNOW_BLOCK, run(p -> {
            final Location c = p.getLocation();
            Fx.sound(c, "block.glass.break", 1f, 0.6f);
            Fx.expandRing(c, 5, 6, l -> Fx.part(l, Particle.SNOWFLAKE, 3, 0.1, 0.1, 0.1, 0.02));
            for (LivingEntity e : Fx.near(p, c, 5)) {
                Fx.hurt(p, e, 4);
                Fx.fx(e, PotionEffectType.SLOWNESS, 120, 1);
                e.setFreezeTicks(140);
            }
        }));

        reg(E, 2, "Frozen Armor", "Coat yourself in ice for absorption and resistance.", 25, 300, Material.ICE, run(p -> {
            Fx.fx(p, PotionEffectType.ABSORPTION, 600, 1);
            Fx.fx(p, PotionEffectType.RESISTANCE, 200, 0);
            Fx.sound(p.getLocation(), "block.glass.place", 1f, 0.6f);
            Fx.repeat(10, 2, i -> Fx.ring(p.getLocation(), 1.0, 14, l ->
                    Fx.part(l.clone().add(0, i * 0.2, 0), Particle.SNOWFLAKE, 1, 0, 0, 0, 0)));
        }));

        reg(E, 3, "Blizzard", "Summon a howling blizzard over an area.", 32, 260, Material.PACKED_ICE, run(p -> {
            final Location c = Fx.aim(p, 22);
            Fx.sound(c, "entity.player.hurt_freeze", 1f, 0.8f);
            Fx.repeat(24, 5, i -> {
                for (int k = 0; k < 30; k++) {
                    Fx.part(c.clone().add(Fx.R.nextGaussian() * 2.5, 3 + Fx.R.nextDouble() * 3, Fx.R.nextGaussian() * 2.5),
                            Particle.SNOWFLAKE, 1, 0, 0, 0, 0.02);
                }
                Fx.ring(c, 5, 24, l -> Fx.part(l, Particle.CLOUD, 1, 0, 0, 0, 0));
                for (LivingEntity e : Fx.near(p, c, 5.5)) {
                    if (i % 2 == 0) Fx.hurt(p, e, 2.5);
                    Fx.fx(e, PotionEffectType.SLOWNESS, 40, 2);
                    e.setFreezeTicks(100);
                }
            });
        }));

        reg(E, 4, "Deep Freeze", "Lock a target in place with crushing cold.", 40, 220, Material.PRISMARINE_SHARD, p -> {
            LivingEntity t = Fx.target(p, 22);
            if (t == null) {
                Fx.fail(p, "No target in sight.");
                return false;
            }
            Fx.sound(t.getLocation(), "block.glass.break", 1f, 0.5f);
            Fx.fx(t, PotionEffectType.SLOWNESS, 100, 9);
            Fx.fx(t, PotionEffectType.JUMP_BOOST, 100, 128);
            t.setFreezeTicks(220);
            if (t instanceof Mob m) {
                m.setAI(false);
                Fx.later(100, () -> {
                    if (!m.isDead()) m.setAI(true);
                });
            }
            Fx.repeat(20, 5, i -> Fx.block(t.getLocation().add(0, 1, 0), Material.ICE, 6, 0.5));
            Fx.hurt(p, t, 6);
            return true;
        });

        reg(E, 5, "Glacial Spike", "Spikes of ice erupt in a line ahead of you.", 42, 160, Material.PRISMARINE_CRYSTALS, run(p -> {
            final Location s = p.getLocation();
            final Vector d = Fx.flat(p);
            final Set<UUID> hit = new HashSet<>();
            Fx.sound(s, "block.glass.break", 1f, 0.4f);
            Fx.repeat(14, 1, i -> {
                Location l = Fx.ground(s.clone().add(d.clone().multiply(i + 1.5)));
                for (int y = 0; y < 3; y++) Fx.block(l.clone().add(0, y * 0.6 + 0.3, 0), Material.PACKED_ICE, 6, 0.25);
                Fx.part(l, Particle.SNOWFLAKE, 10, 0.4, 0.6, 0.4, 0.02);
                for (LivingEntity e : Fx.near(p, l, 1.6)) {
                    if (hit.add(e.getUniqueId())) {
                        Fx.hurt(p, e, 9);
                        Fx.push(e, new Vector(0, 0.8, 0));
                        Fx.fx(e, PotionEffectType.SLOWNESS, 60, 1);
                    }
                }
            });
        }));

        reg(E, 6, "Ice Prison", "Encase a target in a cage of ice.", 62, 300, Material.DIAMOND, p -> {
            LivingEntity t = Fx.target(p, 22);
            if (t == null) {
                Fx.fail(p, "No target in sight.");
                return false;
            }
            Location b = t.getLocation().getBlock().getLocation();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    for (int dy = 0; dy <= 2; dy++) {
                        if (dx == 0 && dz == 0 && dy < 2) continue;
                        if (Math.abs(dx) == 1 || Math.abs(dz) == 1 || dy == 2) {
                            Fx.tempBlock(b.clone().add(dx, dy, dz), Material.PACKED_ICE.createBlockData(), 120);
                        }
                    }
                }
            }
            Fx.sound(t.getLocation(), "block.glass.break", 1.2f, 0.5f);
            Fx.fx(t, PotionEffectType.SLOWNESS, 120, 4);
            t.setFreezeTicks(200);
            Fx.hurt(p, t, 8);
            return true;
        });

        reg(E, 7, "Absolute Zero", "Freeze the world around you in a devastating wave.", 85, 450, Material.HEART_OF_THE_SEA, run(p -> {
            final Location c = p.getLocation();
            Fx.sound(c, "entity.player.hurt_freeze", 2f, 0.5f);
            Fx.expandRing(c, 10, 12, l -> {
                Fx.part(l, Particle.SNOWFLAKE, 4, 0.2, 0.4, 0.2, 0.03);
                Fx.part(l, Particle.CLOUD, 1, 0, 0, 0, 0);
            });
            Fx.later(6, () -> {
                for (LivingEntity e : Fx.near(p, c, 10)) {
                    Fx.hurt(p, e, 11);
                    Fx.fx(e, PotionEffectType.SLOWNESS, 180, 4);
                    Fx.fx(e, PotionEffectType.WEAKNESS, 180, 1);
                    e.setFreezeTicks(300);
                    Fx.block(e.getLocation().add(0, 1, 0), Material.BLUE_ICE, 10, 0.6);
                }
            });
        }));
    }

    // =====================================================================================
    //  DARK
    // =====================================================================================
    private static void dark() {
        Element E = Element.DARK;

        reg(E, 0, "Shadow Bolt", "Fire a bolt of shadow that withers and blinds.", 11, 25, Material.BONE, run(p -> {
            Fx.sound(p.getLocation(), "entity.evoker.cast_spell", 1f, 1.4f);
            Fx.projectile(p, 1.3, 40, l -> {
                Fx.dust(l, 0x4B0082, 1.3f, 3, 0.1);
                Fx.part(l, Particle.SMOKE, 1, 0.05, 0.05, 0.05, 0);
            }, (l, e) -> {
                Fx.part(l, Particle.SOUL, 15, 0.4, 0.4, 0.4, 0.05);
                if (e != null) {
                    Fx.hurt(p, e, 5);
                    Fx.fx(e, PotionEffectType.WITHER, 60, 0);
                    Fx.fx(e, PotionEffectType.BLINDNESS, 40, 0);
                }
            });
        }));

        reg(E, 1, "Life Drain", "Siphon the life force from a target to heal yourself.", 20, 100, Material.SPIDER_EYE, p -> {
            LivingEntity t = Fx.target(p, 14);
            if (t == null) {
                Fx.fail(p, "No target in sight.");
                return false;
            }
            Fx.sound(p.getLocation(), "entity.evoker.prepare_attack", 1f, 1f);
            Fx.repeat(10, 4, i -> {
                if (t.isDead() || !p.isOnline() || p.isDead()) return;
                Fx.hurt(p, t, 1.3);
                Fx.heal(p, 0.7);
                Fx.line(Fx.center(t), p.getEyeLocation().subtract(0, 0.3, 0),
                        l -> Fx.part(l, Particle.SOUL, 1, 0, 0, 0, 0));
            });
            return true;
        });

        reg(E, 2, "Shadow Step", "Teleport behind your target, or blink forward.", 24, 100, Material.FERMENTED_SPIDER_EYE, p -> {
            LivingEntity t = Fx.target(p, 25);
            Location from = p.getLocation();
            Location dest;
            if (t != null) {
                Vector td = t.getLocation().getDirection().setY(0);
                if (td.lengthSquared() < 0.01) td = Fx.flat(p);
                td.normalize();
                dest = t.getLocation().subtract(td.multiply(1.6));
                dest.setDirection(t.getLocation().toVector().subtract(dest.toVector()));
            } else {
                Vector f = Fx.flat(p);
                Location best = from.clone();
                for (double x = 1; x <= 10; x += 1) {
                    Location c = from.clone().add(f.clone().multiply(x));
                    if (Fx.safe(c)) best = c;
                    else break;
                }
                dest = best;
                dest.setDirection(p.getEyeLocation().getDirection());
            }
            if (!Fx.safe(dest) || dest.distanceSquared(from) < 2.25) {
                Fx.fail(p, "No room to step.");
                return false;
            }
            Fx.part(from.clone().add(0, 1, 0), Particle.LARGE_SMOKE, 25, 0.4, 0.6, 0.4, 0.02);
            p.teleport(dest);
            p.setFallDistance(0);
            Fx.part(dest.clone().add(0, 1, 0), Particle.PORTAL, 40, 0.4, 0.6, 0.4, 0.5);
            Fx.sound(dest, "entity.enderman.teleport", 1f, 1.2f);
            return true;
        });

        reg(E, 3, "Hex", "Curse a target with weakness, slowness and decay.", 28, 140, Material.SOUL_SAND, p -> {
            LivingEntity t = Fx.target(p, 22);
            if (t == null) {
                Fx.fail(p, "No target in sight.");
                return false;
            }
            Fx.fx(t, PotionEffectType.WITHER, 160, 1);
            Fx.fx(t, PotionEffectType.WEAKNESS, 200, 1);
            Fx.fx(t, PotionEffectType.SLOWNESS, 160, 0);
            Fx.fx(t, PotionEffectType.GLOWING, 160, 0);
            Fx.sound(t.getLocation(), "entity.evoker.cast_spell", 1f, 0.6f);
            Fx.part(Fx.center(t), Particle.WITCH, 40, 0.5, 0.8, 0.5, 0.1);
            return true;
        });

        reg(E, 4, "Void Rift", "Tear open a rift that drags everything toward it.", 48, 300, Material.ENDER_PEARL, run(p -> {
            final Location c = Fx.aim(p, 20).add(0, 1, 0);
            Fx.sound(c, "entity.enderman.teleport", 1.5f, 0.5f);
            Fx.repeat(60, 1, i -> {
                Fx.ring(c, 1.2 + 0.3 * Math.sin(i / 4.0), 14, l ->
                        Fx.part(l.clone().add(0, Fx.R.nextDouble() - 0.5, 0), Particle.PORTAL, 2, 0, 0, 0, 0.3));
                Fx.dust(c, 0x2B0057, 2f, 6, 0.4);
                for (LivingEntity e : Fx.near(p, c, 7)) {
                    Vector pull = c.toVector().subtract(e.getLocation().toVector());
                    if (pull.lengthSquared() > 0.5) {
                        e.setVelocity(e.getVelocity().multiply(0.6).add(pull.normalize().multiply(0.22)));
                    }
                    if (i % 10 == 0) Fx.hurt(p, e, 2.5);
                }
            });
        }));

        reg(E, 5, "Nightfall", "Plunge the area into darkness, blinding your enemies.", 55, 300, Material.ENDER_EYE, run(p -> {
            final Location c = p.getLocation();
            Fx.sound(c, "entity.wither.ambient", 1.2f, 0.6f);
            Fx.expandRing(c, 10, 10, l -> Fx.dust(l.clone().add(0, 0.5, 0), 0x1A0030, 2f, 3, 0.3));
            for (LivingEntity e : Fx.near(p, c, 10)) {
                Fx.fx(e, PotionEffectType.BLINDNESS, 160, 0);
                Fx.fx(e, PotionEffectType.DARKNESS, 200, 0);
                Fx.fx(e, PotionEffectType.WITHER, 100, 1);
            }
            Fx.fx(p, PotionEffectType.NIGHT_VISION, 240, 0);
        }));

        reg(E, 6, "Shadow Fangs", "Spectral fangs erupt from the ground in a line.", 68, 240, Material.WITHER_ROSE, run(p -> {
            final Location s = p.getLocation();
            final Vector d = Fx.flat(p);
            Fx.sound(s, "entity.evoker.prepare_attack", 1.2f, 0.8f);
            Fx.repeat(14, 1, i -> {
                Location l = Fx.ground(s.clone().add(d.clone().multiply(i + 1.5)));
                p.getWorld().spawn(l, EvokerFangs.class, f -> f.setOwner(p));
                Fx.part(l, Particle.SMOKE, 6, 0.3, 0.2, 0.3, 0.01);
            });
        }));

        reg(E, 7, "Soul Reap", "Rip the souls from everything nearby, feeding your strength.", 88, 450,
                Material.WITHER_SKELETON_SKULL, run(p -> {
                    final Location c = p.getLocation();
                    Fx.sound(c, "entity.wither.spawn", 0.8f, 1.5f);
                    int hits = 0;
                    for (LivingEntity e : Fx.near(p, c, 9)) {
                        Fx.hurt(p, e, 9);
                        Fx.line(Fx.center(e), p.getEyeLocation(), l -> Fx.part(l, Particle.SOUL, 1, 0, 0, 0, 0));
                        hits++;
                    }
                    if (hits > 0) {
                        Fx.heal(p, Math.min(12, hits * 2.0));
                        Fx.fx(p, PotionEffectType.STRENGTH, 200, 1);
                    }
                    Fx.expandRing(c, 9, 8, l -> Fx.part(l, Particle.SOUL_FIRE_FLAME, 2, 0.1, 0.2, 0.1, 0.01));
                }));
    }

    // =====================================================================================
    //  LIGHT
    // =====================================================================================
    private static void light() {
        Element E = Element.LIGHT;

        reg(E, 0, "Holy Bolt", "A bolt of radiance. Burns the undead.", 11, 25, Material.GLOWSTONE_DUST, run(p -> {
            Fx.sound(p.getLocation(), "block.amethyst_block.chime", 1.2f, 1.6f);
            Fx.projectile(p, 1.5, 40, l -> {
                Fx.part(l, Particle.END_ROD, 2, 0.05, 0.05, 0.05, 0.01);
                Fx.dust(l, 0xFFF2A8, 1f, 2, 0.1);
            }, (l, e) -> {
                Fx.part(l, Particle.END_ROD, 25, 0.4, 0.4, 0.4, 0.08);
                if (e != null) Fx.hurt(p, e, Fx.undead(e) ? 12 : 5);
            });
        }));

        reg(E, 1, "Healing Light", "Mend your wounds and those of nearby allies.", 18, 120, Material.GOLD_NUGGET, run(p -> {
            Fx.sound(p.getLocation(), "entity.player.levelup", 0.8f, 1.5f);
            Fx.heal(p, 8);
            Fx.fx(p, PotionEffectType.REGENERATION, 100, 1);
            for (Player o : Fx.friends(p, p.getLocation(), 4)) {
                if (o.equals(p)) continue;
                Fx.heal(o, 4);
            }
            Fx.repeat(8, 2, i -> Fx.ring(p.getLocation(), 1.2, 12, l ->
                    Fx.part(l.clone().add(0, i * 0.25, 0), Particle.END_ROD, 1, 0, 0, 0, 0)));
        }));

        reg(E, 2, "Smite", "Holy fire scorches everything around you. Undead suffer greatly.", 26, 120,
                Material.SUGAR, run(p -> {
                    final Location c = p.getLocation();
                    Fx.sound(c, "block.beacon.activate", 1f, 1.8f);
                    Fx.expandRing(c, 6, 6, l -> Fx.part(l, Particle.END_ROD, 2, 0.1, 0.2, 0.1, 0.02));
                    for (LivingEntity e : Fx.near(p, c, 6)) {
                        Fx.hurt(p, e, Fx.undead(e) ? 10 : 3);
                        Fx.fx(e, PotionEffectType.GLOWING, 200, 0);
                    }
                }));

        reg(E, 3, "Blessing", "Grant yourself and allies speed and strength.", 30, 300,
                Material.GLISTERING_MELON_SLICE, run(p -> {
                    Fx.sound(p.getLocation(), "block.beacon.power_select", 1f, 1.5f);
                    for (Player o : Fx.friends(p, p.getLocation(), 8)) {
                        Fx.fx(o, PotionEffectType.SPEED, 300, 1);
                        Fx.fx(o, PotionEffectType.STRENGTH, 300, 0);
                        Fx.part(o.getLocation().add(0, 1, 0), Particle.END_ROD, 20, 0.4, 0.8, 0.4, 0.05);
                    }
                }));

        reg(E, 4, "Judgement", "Call a pillar of holy light down on a target.", 42, 220, Material.GOLDEN_CARROT, run(p -> {
            LivingEntity t = Fx.target(p, 30);
            final Location c = t != null ? t.getLocation() : Fx.aim(p, 30);
            Fx.sound(c, "block.beacon.activate", 1.5f, 1.5f);
            Fx.repeat(14, 1, i -> Fx.ring(c, 3 - 0.2 * i, 20, l -> Fx.part(l, Particle.END_ROD, 1, 0, 0.1, 0, 0)));
            Fx.later(14, () -> {
                for (int y = 0; y < 24; y++) Fx.part(c.clone().add(0, y, 0), Particle.END_ROD, 6, 0.25, 0.1, 0.25, 0.02);
                Fx.part(c, Particle.EXPLOSION_EMITTER, 1, 0, 0, 0, 0);
                Fx.sound(c, "entity.lightning_bolt.thunder", 1.5f, 1.6f);
                for (LivingEntity e : Fx.near(p, c, 3)) Fx.hurt(p, e, Fx.undead(e) ? 26 : 13);
            });
        }));

        reg(E, 5, "Purify", "Cleanse harmful effects from yourself and allies.", 38, 200, Material.GOLD_INGOT, run(p -> {
            Fx.sound(p.getLocation(), "block.amethyst_block.resonate", 1f, 1.5f);
            PotionEffectType[] bad = {PotionEffectType.POISON, PotionEffectType.WITHER, PotionEffectType.SLOWNESS,
                    PotionEffectType.WEAKNESS, PotionEffectType.BLINDNESS, PotionEffectType.DARKNESS,
                    PotionEffectType.NAUSEA, PotionEffectType.HUNGER, PotionEffectType.MINING_FATIGUE,
                    PotionEffectType.LEVITATION};
            for (Player o : Fx.friends(p, p.getLocation(), 6)) {
                for (PotionEffectType t : bad) o.removePotionEffect(t);
                o.setFireTicks(0);
                o.setFreezeTicks(0);
                Fx.heal(o, 4);
                Fx.part(o.getLocation().add(0, 1, 0), Particle.END_ROD, 25, 0.4, 0.8, 0.4, 0.04);
            }
        }));

        reg(E, 6, "Divine Shield", "A shield of pure light absorbs punishing damage.", 60, 400, Material.GOLDEN_APPLE, run(p -> {
            Fx.sound(p.getLocation(), "block.beacon.activate", 1.2f, 1.2f);
            Fx.fx(p, PotionEffectType.ABSORPTION, 300, 4);
            Fx.fx(p, PotionEffectType.RESISTANCE, 160, 1);
            for (Player o : Fx.friends(p, p.getLocation(), 6)) {
                if (!o.equals(p)) Fx.fx(o, PotionEffectType.ABSORPTION, 300, 1);
            }
            Fx.repeat(20, 2, i -> {
                for (int k = 0; k < 10; k++) {
                    double a = Fx.R.nextDouble() * Math.PI * 2, b = Fx.R.nextDouble() * Math.PI;
                    Location l = p.getLocation().add(0, 1, 0).add(Math.cos(a) * Math.sin(b) * 1.5,
                            Math.cos(b) * 1.5, Math.sin(a) * Math.sin(b) * 1.5);
                    Fx.part(l, Particle.END_ROD, 1, 0, 0, 0, 0);
                }
            });
        }));

        reg(E, 7, "Sunburst", "A blinding explosion of light. Harms foes, heals friends.", 85, 450,
                Material.TOTEM_OF_UNDYING, run(p -> {
                    final Location c = p.getLocation();
                    Fx.sound(c, "entity.lightning_bolt.thunder", 1.5f, 2f);
                    Fx.expandRing(c, 12, 10, l -> {
                        Fx.part(l, Particle.END_ROD, 3, 0.1, 0.4, 0.1, 0.05);
                        Fx.dust(l.clone().add(0, 0.5, 0), 0xFFE066, 2f, 2, 0.2);
                    });
                    Fx.part(c.clone().add(0, 1, 0), Particle.EXPLOSION_EMITTER, 1, 0, 0, 0, 0);
                    Fx.later(5, () -> {
                        for (LivingEntity e : Fx.near(p, c, 12)) {
                            Fx.hurt(p, e, Fx.undead(e) ? 20 : 10);
                            Fx.fx(e, PotionEffectType.BLINDNESS, 60, 0);
                        }
                        for (Player o : Fx.friends(p, c, 12)) Fx.heal(o, 8);
                    });
                }));
    }

    // =====================================================================================
    //  LIGHTNING
    // =====================================================================================
    private static void lightning() {
        Element E = Element.LIGHTNING;

        reg(E, 0, "Spark", "A crackling bolt of static.", 10, 15, Material.REDSTONE, run(p -> {
            Fx.sound(p.getLocation(), "entity.lightning_bolt.impact", 0.5f, 2f);
            Fx.projectile(p, 2.2, 25, l -> Fx.part(l, Particle.ELECTRIC_SPARK, 2, 0.05, 0.05, 0.05, 0.02), (l, e) -> {
                Fx.part(l, Particle.ELECTRIC_SPARK, 25, 0.4, 0.4, 0.4, 0.2);
                if (e != null) Fx.hurt(p, e, 4.5);
            });
        }));

        reg(E, 1, "Static Dash", "Dash forward in a flash, shocking whatever you pass.", 15, 70, Material.COPPER_INGOT, run(p -> {
            Fx.dash(p, 1.9);
            Fx.sound(p.getLocation(), "entity.lightning_bolt.impact", 0.6f, 1.8f);
            final Set<UUID> hit = new HashSet<>();
            Fx.repeat(6, 2, i -> {
                Fx.part(p.getLocation().add(0, 1, 0), Particle.ELECTRIC_SPARK, 20, 0.4, 0.6, 0.4, 0.15);
                for (LivingEntity e : Fx.near(p, p.getLocation(), 2.2)) {
                    if (hit.add(e.getUniqueId())) Fx.hurt(p, e, 3);
                }
            });
        }));

        reg(E, 2, "Chain Lightning", "Lightning leaps between up to five enemies.", 24, 100, Material.IRON_INGOT, p -> {
            LivingEntity first = Fx.target(p, 22);
            if (first == null) {
                Fx.fail(p, "No target in sight.");
                return false;
            }
            Fx.sound(p.getLocation(), "entity.lightning_bolt.thunder", 0.6f, 2f);
            final Set<UUID> seen = new HashSet<>();
            final LivingEntity[] cur = {first};
            final Location[] from = {p.getEyeLocation().subtract(0, 0.3, 0)};
            Fx.repeat(5, 3, i -> {
                LivingEntity t = cur[0];
                if (t == null || t.isDead()) return;
                seen.add(t.getUniqueId());
                Fx.line(from[0], Fx.center(t), l -> Fx.part(l, Particle.ELECTRIC_SPARK, 2, 0.05, 0.05, 0.05, 0.02));
                Fx.hurt(p, t, 5);
                Fx.sound(t.getLocation(), "entity.lightning_bolt.impact", 0.5f, 1.8f);
                from[0] = Fx.center(t);
                LivingEntity next = null;
                double best = 36;
                for (LivingEntity e : Fx.near(p, t.getLocation(), 6)) {
                    if (seen.contains(e.getUniqueId())) continue;
                    double d = e.getLocation().distanceSquared(t.getLocation());
                    if (d < best) {
                        best = d;
                        next = e;
                    }
                }
                cur[0] = next;
            });
            return true;
        });

        reg(E, 3, "Thunder Clap", "A shockwave of thunder knocks enemies back.", 30, 120, Material.REDSTONE_BLOCK, run(p -> {
            final Location c = p.getLocation();
            Fx.sound(c, "entity.lightning_bolt.thunder", 1f, 1.6f);
            Fx.expandRing(c, 5, 5, l -> Fx.part(l, Particle.ELECTRIC_SPARK, 5, 0.1, 0.3, 0.1, 0.1));
            Fx.part(c, Particle.EXPLOSION, 1, 0, 0, 0, 0);
            for (LivingEntity e : Fx.near(p, c, 5)) {
                Fx.hurt(p, e, 6);
                Fx.pushAway(e, c, 1.3, 0.4);
            }
        }));

        reg(E, 4, "Lightning Strike", "Call a bolt of lightning down on your target.", 40, 150, Material.COPPER_BLOCK, run(p -> {
            LivingEntity t = Fx.target(p, 40);
            final Location c = t != null ? t.getLocation() : Fx.aim(p, 40);
            c.getWorld().strikeLightningEffect(c);
            Fx.blast(p, c, 3, 11, 0, 0.5);
        }));

        reg(E, 5, "Overcharge", "Supercharge yourself with speed and haste.", 45, 300, Material.IRON_BLOCK, run(p -> {
            Fx.sound(p.getLocation(), "entity.lightning_bolt.impact", 0.8f, 1.5f);
            Fx.fx(p, PotionEffectType.SPEED, 240, 2);
            Fx.fx(p, PotionEffectType.HASTE, 240, 2);
            Fx.repeat(24, 5, i -> {
                if (p.isOnline()) Fx.part(p.getLocation().add(0, 1, 0), Particle.ELECTRIC_SPARK, 12, 0.4, 0.8, 0.4, 0.1);
            });
        }));

        reg(E, 6, "Storm Call", "Summon a storm that rains lightning on an area.", 66, 320, Material.DIAMOND, run(p -> {
            LivingEntity t = Fx.target(p, 30);
            final Location c = t != null ? t.getLocation() : Fx.aim(p, 30);
            Fx.sound(c, "entity.lightning_bolt.thunder", 1.5f, 0.8f);
            Fx.repeat(8, 5, i -> {
                Location l = c.clone().add(Fx.R.nextGaussian() * 2.5, 0, Fx.R.nextGaussian() * 2.5);
                l = Fx.ground(l);
                l.getWorld().strikeLightningEffect(l);
                Fx.blast(p, l, 2.5, 7, 0, 0.4);
            });
        }));

        reg(E, 7, "Tempest", "Unleash the full fury of the storm on everything nearby.", 90, 500, Material.NETHERITE_SCRAP, run(p -> {
            final Location c = p.getLocation();
            List<LivingEntity> list = Fx.near(p, c, 14);
            list.sort(Comparator.comparingDouble(e -> e.getLocation().distanceSquared(c)));
            Fx.sound(c, "entity.lightning_bolt.thunder", 2f, 0.6f);
            Fx.expandRing(c, 14, 12, l -> Fx.part(l, Particle.ELECTRIC_SPARK, 3, 0.1, 0.5, 0.1, 0.1));
            int n = Math.min(8, list.size());
            for (int i = 0; i < n; i++) {
                final LivingEntity e = list.get(i);
                Fx.later(8 + i * 4, () -> {
                    if (e.isDead()) return;
                    e.getWorld().strikeLightningEffect(e.getLocation());
                    Fx.hurt(p, e, 12);
                });
            }
        }));
    }

    // =====================================================================================
    //  NATURE
    // =====================================================================================
    private static void nature() {
        Element E = Element.NATURE;

        reg(E, 0, "Thorn Shot", "Fire a poisonous thorn.", 10, 22, Material.WHEAT_SEEDS, run(p -> {
            Fx.sound(p.getLocation(), "entity.arrow.shoot", 1f, 1.5f);
            Fx.projectile(p, 1.5, 40, l -> {
                Fx.dust(l, 0x3FAE2A, 1f, 2, 0.05);
                Fx.part(l, Particle.CRIT, 1, 0, 0, 0, 0);
            }, (l, e) -> {
                Fx.part(l, Particle.HAPPY_VILLAGER, 15, 0.4, 0.4, 0.4, 0);
                if (e != null) {
                    Fx.hurt(p, e, 4);
                    Fx.fx(e, PotionEffectType.POISON, 80, 0);
                }
            });
        }));

        reg(E, 1, "Entangle", "Roots burst from the ground and hold a target in place.", 18, 100, Material.VINE, p -> {
            LivingEntity t = Fx.target(p, 18);
            if (t == null) {
                Fx.fail(p, "No target in sight.");
                return false;
            }
            Fx.sound(t.getLocation(), "block.grass.break", 1f, 0.6f);
            Fx.fx(t, PotionEffectType.SLOWNESS, 80, 6);
            Fx.fx(t, PotionEffectType.JUMP_BOOST, 80, 128);
            Fx.repeat(8, 5, i -> {
                Fx.part(t.getLocation().add(0, 0.4, 0), Particle.COMPOSTER, 8, 0.5, 0.4, 0.5, 0);
                Fx.block(t.getLocation().add(0, 0.5, 0), Material.OAK_LEAVES, 6, 0.4);
            });
            return true;
        });

        reg(E, 2, "Regrowth", "Nature knits your wounds closed over time.", 24, 160, Material.OAK_SAPLING, run(p -> {
            Fx.sound(p.getLocation(), "block.grass.place", 1f, 1.2f);
            Fx.heal(p, 6);
            Fx.fx(p, PotionEffectType.REGENERATION, 160, 1);
            for (Player o : Fx.friends(p, p.getLocation(), 5)) {
                if (o.equals(p)) continue;
                Fx.heal(o, 3);
                Fx.fx(o, PotionEffectType.REGENERATION, 100, 0);
            }
            Fx.repeat(8, 3, i -> Fx.part(p.getLocation().add(0, 0.2 + i * 0.25, 0), Particle.HAPPY_VILLAGER, 6, 0.6, 0.1, 0.6, 0));
        }));

        reg(E, 3, "Bramble Ring", "A ring of thorns lashes everything around you.", 28, 120, Material.SWEET_BERRIES, run(p -> {
            final Location c = p.getLocation();
            Fx.sound(c, "block.sweet_berry_bush.pick_berries", 1.2f, 0.6f);
            Fx.expandRing(c, 5, 6, l -> Fx.dust(l.clone().add(0, 0.3, 0), 0x2E7D32, 1.5f, 3, 0.15));
            for (LivingEntity e : Fx.near(p, c, 5)) {
                Fx.hurt(p, e, 5);
                Fx.fx(e, PotionEffectType.SLOWNESS, 80, 1);
            }
        }));

        reg(E, 4, "Toxic Spores", "Release a lingering cloud of poison spores.", 40, 240, Material.BONE_MEAL, run(p -> {
            final Location c = Fx.aim(p, 20);
            Fx.sound(c, "entity.puffer_fish.blow_out", 1f, 0.5f);
            Fx.repeat(25, 4, i -> {
                for (int k = 0; k < 14; k++) {
                    Fx.part(c.clone().add(Fx.R.nextGaussian() * 1.8, 0.5 + Fx.R.nextDouble() * 1.5, Fx.R.nextGaussian() * 1.8),
                            Particle.SPORE_BLOSSOM_AIR, 1, 0, 0, 0, 0);
                }
                Fx.dust(c.clone().add(0, 1, 0), 0x7BC043, 2f, 8, 1.4);
                for (LivingEntity e : Fx.near(p, c, 4)) Fx.fx(e, PotionEffectType.POISON, 60, 1);
            });
        }));

        reg(E, 5, "Wild Growth", "Make the land flourish and feed nearby allies.", 36, 200, Material.SPORE_BLOSSOM, run(p -> {
            final Location c = p.getLocation();
            Fx.sound(c, "item.bone_meal.use", 1f, 1f);
            Fx.expandRing(c, 6, 8, l -> Fx.part(l, Particle.HAPPY_VILLAGER, 2, 0.1, 0.2, 0.1, 0));
            if (Fx.pl().getConfig().getBoolean("world-effects", true)) {
                int n = 0;
                for (int k = 0; k < 40 && n < 14; k++) {
                    Block b = c.clone().add(Fx.R.nextInt(13) - 6, Fx.R.nextInt(5) - 2, Fx.R.nextInt(13) - 6).getBlock();
                    if (b.applyBoneMeal(BlockFace.UP)) n++;
                }
            }
            for (Player o : Fx.friends(p, c, 6)) {
                Fx.heal(o, 6);
                o.setFoodLevel(Math.min(20, o.getFoodLevel() + 4));
            }
        }));

        reg(E, 6, "Summon Treant", "Awaken an ancient guardian to fight beside you.", 62, 600, Material.FLOWERING_AZALEA, run(p -> {
            Location l = p.getLocation().add(Fx.flat(p).multiply(2));
            IronGolem g = p.getWorld().spawn(l, IronGolem.class, e -> {
                e.setPlayerCreated(true);
                e.customName(Component.text("Treant", NamedTextColor.GREEN));
                e.setCustomNameVisible(true);
                e.setRemoveWhenFarAway(true);
            });
            for (Entity e : p.getNearbyEntities(20, 10, 20)) {
                if (e instanceof Monster m) {
                    g.setTarget(m);
                    break;
                }
            }
            Fx.sound(l, "block.grass.break", 1.5f, 0.5f);
            Fx.part(l.clone().add(0, 1, 0), Particle.HAPPY_VILLAGER, 40, 0.8, 1, 0.8, 0);
            Fx.later(400, () -> {
                if (!g.isDead()) {
                    Fx.part(g.getLocation().add(0, 1, 0), Particle.HAPPY_VILLAGER, 30, 1, 1, 1, 0);
                    g.remove();
                }
            });
        }));

        reg(E, 7, "Nature's Wrath", "The earth erupts with thorned vines, rooting and ravaging foes.", 88, 450, Material.TORCHFLOWER, run(p -> {
            final Location c = Fx.aim(p, 20);
            Fx.sound(c, "block.grass.break", 1.5f, 0.4f);
            Fx.repeat(6, 10, i -> {
                Fx.ring(c, 3 + i, 24, l -> {
                    Fx.dust(l.clone().add(0, 0.4, 0), 0x2E7D32, 1.6f, 3, 0.2);
                    Fx.block(l.clone().add(0, 0.5, 0), Material.OAK_LEAVES, 2, 0.3);
                });
                for (LivingEntity e : Fx.near(p, c, 8)) {
                    Fx.hurt(p, e, 5);
                    Fx.fx(e, PotionEffectType.SLOWNESS, 40, 5);
                    Fx.fx(e, PotionEffectType.POISON, 60, 0);
                }
            });
        }));
    }

    // =====================================================================================
    //  WIND
    // =====================================================================================
    private static void wind() {
        Element E = Element.WIND;

        reg(E, 0, "Gust", "A blast of wind that shoves a target away.", 10, 25, Material.FEATHER, run(p -> {
            Fx.sound(p.getLocation(), "entity.phantom.flap", 1f, 1.6f);
            final Vector dir = p.getEyeLocation().getDirection().normalize();
            Fx.projectile(p, 1.6, 20, l -> Fx.part(l, Particle.CLOUD, 3, 0.1, 0.1, 0.1, 0.01), (l, e) -> {
                Fx.part(l, Particle.CLOUD, 20, 0.4, 0.4, 0.4, 0.1);
                if (e != null) {
                    Fx.hurt(p, e, 3);
                    e.setVelocity(dir.clone().multiply(1.6).setY(0.5));
                }
            });
        }));

        reg(E, 1, "Air Dash", "Launch yourself forward on a rush of wind.", 12, 50, Material.STRING, run(p -> {
            Fx.dash(p, 1.7);
            Fx.sound(p.getLocation(), "entity.phantom.flap", 1f, 1.2f);
            Fx.repeat(8, 1, i -> Fx.part(p.getLocation().add(0, 0.8, 0), Particle.CLOUD, 5, 0.3, 0.4, 0.3, 0.02));
        }));

        reg(E, 2, "Updraft", "Soar skyward, then drift gently down.", 22, 100, Material.WIND_CHARGE, run(p -> {
            p.setVelocity(new Vector(0, 1.5, 0));
            Fx.fx(p, PotionEffectType.SLOW_FALLING, 120, 0);
            Fx.sound(p.getLocation(), "entity.breeze.wind_burst", 1f, 1f);
            Fx.repeat(10, 2, i -> Fx.part(p.getLocation(), Particle.CLOUD, 8, 0.4, 0.1, 0.4, 0.05));
        }));

        reg(E, 3, "Cyclone", "A whirling cyclone flings enemies into the air.", 30, 140, Material.WHITE_WOOL, run(p -> {
            final Location c = Fx.aim(p, 15);
            Fx.sound(c, "entity.phantom.flap", 1.5f, 0.6f);
            Fx.repeat(12, 1, i -> {
                for (int k = 0; k < 10; k++) {
                    double a = i * 0.8 + k * 0.63;
                    double h = k * 0.4;
                    Fx.part(c.clone().add(Math.cos(a) * (0.5 + h * 0.3), h, Math.sin(a) * (0.5 + h * 0.3)),
                            Particle.CLOUD, 1, 0, 0, 0, 0);
                }
            });
            for (LivingEntity e : Fx.near(p, c, 4)) {
                Fx.hurt(p, e, 4);
                e.setVelocity(new Vector(0, 1.1, 0));
            }
        }));

        reg(E, 4, "Wind Slash", "A razor-sharp arc of wind cuts through everything ahead.", 40, 100, Material.FIREWORK_ROCKET, run(p -> {
            Fx.sound(p.getLocation(), "entity.player.attack.sweep", 1.2f, 0.8f);
            Set<LivingEntity> hit = Fx.beam(p, 13, l -> {
                Fx.part(l, Particle.CLOUD, 2, 0.1, 0.1, 0.1, 0.01);
                Fx.part(l, Particle.CRIT, 1, 0.1, 0.1, 0.1, 0.01);
            }, 1.3);
            for (LivingEntity e : hit) {
                Fx.hurt(p, e, 8);
                Fx.pushAway(e, p.getLocation(), 0.5, 0.2);
            }
        }));

        reg(E, 5, "Tailwind", "Ride the wind: speed, jump boost and feather falling.", 36, 300, Material.BREEZE_ROD, run(p -> {
            Fx.sound(p.getLocation(), "entity.breeze.wind_burst", 1f, 1.4f);
            Fx.fx(p, PotionEffectType.SPEED, 300, 2);
            Fx.fx(p, PotionEffectType.SLOW_FALLING, 300, 0);
            Fx.fx(p, PotionEffectType.JUMP_BOOST, 300, 2);
            Fx.repeat(15, 4, i -> {
                if (p.isOnline()) Fx.part(p.getLocation().add(0, 0.3, 0), Particle.CLOUD, 4, 0.4, 0.1, 0.4, 0.02);
            });
        }));

        reg(E, 6, "Tornado", "Conjure a tornado that sweeps across the land.", 65, 350, Material.NAUTILUS_SHELL, run(p -> {
            final Vector d = Fx.flat(p);
            final Location s = Fx.ground(p.getLocation().add(d.clone().multiply(4)));
            Fx.sound(s, "entity.phantom.flap", 1.5f, 0.5f);
            Fx.repeat(70, 1, i -> {
                Location c = s.clone().add(d.clone().multiply(i * 0.18));
                for (int k = 0; k < 18; k++) {
                    double h = k * 0.35;
                    double a = i * 0.6 + k * 0.7;
                    double r = 0.4 + h * 0.28;
                    Fx.part(c.clone().add(Math.cos(a) * r, h, Math.sin(a) * r), Particle.CLOUD, 1, 0, 0, 0, 0);
                }
                for (LivingEntity e : Fx.near(p, c.clone().add(0, 2, 0), 3.2)) {
                    double rx = e.getLocation().getX() - c.getX();
                    double rz = e.getLocation().getZ() - c.getZ();
                    e.setVelocity(new Vector(-rz * 0.12, 0.45, rx * 0.12));
                    if (i % 10 == 0) Fx.hurt(p, e, 2);
                }
            });
        }));

        reg(E, 7, "Hurricane", "A devastating gale blasts everything away from you.", 88, 450, Material.ENDER_PEARL, run(p -> {
            final Location c = p.getLocation();
            Fx.sound(c, "entity.breeze.wind_burst", 2f, 0.5f);
            Fx.expandRing(c, 12, 10, l -> Fx.part(l, Particle.CLOUD, 3, 0.1, 0.5, 0.1, 0.1));
            Fx.later(4, () -> {
                for (LivingEntity e : Fx.near(p, c, 12)) {
                    Fx.hurt(p, e, 8);
                    Fx.pushAway(e, c, 2.0, 0.6);
                    Fx.fx(e, PotionEffectType.LEVITATION, 30, 0);
                }
            });
            Fx.fx(p, PotionEffectType.SPEED, 100, 1);
        }));
    }

    // =====================================================================================
    //  EARTH
    // =====================================================================================
    private static void earth() {
        Element E = Element.EARTH;

        reg(E, 0, "Rock Throw", "Hurl a chunk of rock.", 12, 28, Material.COBBLESTONE, run(p -> {
            Fx.sound(p.getLocation(), "block.stone.break", 1f, 0.7f);
            Fx.projectile(p, 1.2, 35, l -> Fx.block(l, Material.STONE, 3, 0.1), (l, e) -> {
                Fx.block(l, Material.STONE, 30, 0.4);
                Fx.sound(l, "block.stone.break", 1f, 0.6f);
                if (e != null) {
                    Fx.hurt(p, e, 7);
                    Fx.pushAway(e, p.getLocation(), 0.7, 0.3);
                }
            });
        }));

        reg(E, 1, "Stone Skin", "Harden your skin for resistance and a little absorption.", 18, 200, Material.GRAVEL, run(p -> {
            Fx.sound(p.getLocation(), "block.stone.place", 1f, 0.6f);
            Fx.fx(p, PotionEffectType.RESISTANCE, 200, 1);
            Fx.fx(p, PotionEffectType.ABSORPTION, 400, 0);
            Fx.block(p.getLocation().add(0, 1, 0), Material.STONE, 30, 0.5);
        }));

        reg(E, 2, "Quake", "The ground shakes, damaging and tossing nearby enemies.", 28, 120, Material.CLAY_BALL, run(p -> {
            final Location c = p.getLocation();
            Fx.sound(c, "entity.generic.explode", 1f, 0.5f);
            Fx.expandRing(c, 5, 6, l -> Fx.block(l.clone().add(0, 0.3, 0), Material.DIRT, 5, 0.2));
            for (LivingEntity e : Fx.near(p, c, 5)) {
                Fx.hurt(p, e, 5);
                Fx.fx(e, PotionEffectType.SLOWNESS, 60, 1);
                Fx.push(e, new Vector(0, 0.6, 0));
            }
        }));

        reg(E, 3, "Stone Wall", "Raise a temporary wall of stone in front of you.", 32, 240, Material.FLINT, p -> {
            if (!Fx.pl().getConfig().getBoolean("temporary-blocks", true)) {
                Fx.fail(p, "Temporary blocks are disabled.");
                return false;
            }
            Vector d = Fx.flat(p);
            Vector side = new Vector(-d.getZ(), 0, d.getX());
            Location base = p.getLocation().getBlock().getLocation().add(0.5, 0, 0.5).add(d.clone().multiply(3));
            int placed = 0;
            for (int w = -2; w <= 2; w++) {
                for (int h = 0; h < 3; h++) {
                    Location l = base.clone().add(side.clone().multiply(w)).add(0, h, 0);
                    if (Fx.tempBlock(l, Material.COBBLESTONE.createBlockData(), 160)) placed++;
                }
            }
            if (placed == 0) {
                Fx.fail(p, "No room for a wall.");
                return false;
            }
            Fx.sound(base, "block.stone.place", 1.5f, 0.5f);
            return true;
        });

        reg(E, 4, "Seismic Wave", "Send a shockwave through the ground, launching foes.", 42, 160, Material.STONE_BRICKS, run(p -> {
            final Location s = p.getLocation();
            final Vector d = Fx.flat(p);
            final Set<UUID> hit = new HashSet<>();
            Fx.sound(s, "entity.generic.explode", 1f, 0.5f);
            Fx.repeat(14, 1, i -> {
                Location l = Fx.ground(s.clone().add(d.clone().multiply(i + 1.5)));
                Fx.block(l.clone().add(0, 0.3, 0), Material.DIRT, 14, 0.5);
                Fx.part(l, Particle.CLOUD, 3, 0.4, 0.1, 0.4, 0.01);
                for (LivingEntity e : Fx.near(p, l, 1.8)) {
                    if (hit.add(e.getUniqueId())) {
                        Fx.hurt(p, e, 6);
                        Fx.push(e, new Vector(0, 0.9, 0));
                    }
                }
            });
        }));

        reg(E, 5, "Titan's Might", "Gain the strength and toughness of a titan.", 46, 320, Material.RAW_IRON, run(p -> {
            Fx.sound(p.getLocation(), "entity.iron_golem.repair", 1.2f, 0.6f);
            Fx.fx(p, PotionEffectType.STRENGTH, 240, 1);
            Fx.fx(p, PotionEffectType.RESISTANCE, 240, 1);
            Fx.block(p.getLocation().add(0, 1, 0), Material.IRON_BLOCK, 30, 0.5);
        }));

        reg(E, 6, "Landslide", "Boulders crash down across a wide area.", 66, 320, Material.OBSIDIAN, run(p -> {
            final Location c = Fx.aim(p, 25);
            Fx.sound(c, "entity.generic.explode", 1.2f, 0.4f);
            Fx.repeat(8, 4, i -> {
                for (int k = 0; k < 2; k++) {
                    Location l = c.clone().add(Fx.R.nextGaussian() * 2.5, 0, Fx.R.nextGaussian() * 2.5);
                    l = Fx.ground(l);
                    for (int y = 6; y > 0; y--) Fx.block(l.clone().add(0, y, 0), Material.STONE, 3, 0.2);
                    Fx.blast(p, l, 2.5, 4.5, 0, 0.3);
                }
            });
        }));

        reg(E, 7, "Tectonic Slam", "Leap into the sky and crash down, shattering the ground.", 88, 450, Material.AMETHYST_BLOCK, run(p -> {
            Vector d = Fx.flat(p).multiply(0.5);
            p.setVelocity(new Vector(d.getX(), 1.4, d.getZ()));
            p.setFallDistance(0);
            Fx.sound(p.getLocation(), "entity.iron_golem.attack", 1f, 0.6f);
            final int[] t = {0};
            new BukkitRunnable() {
                @Override
                public void run() {
                    t[0]++;
                    if (!p.isOnline() || p.isDead() || t[0] > 70) {
                        cancel();
                        return;
                    }
                    p.setFallDistance(0);
                    Fx.part(p.getLocation(), Particle.CLOUD, 2, 0.2, 0.1, 0.2, 0);
                    if (t[0] > 6 && p.isOnGround()) {
                        cancel();
                        Location c = p.getLocation();
                        Fx.sound(c, "entity.generic.explode", 1.5f, 0.4f);
                        Fx.expandRing(c, 8, 8, l -> Fx.block(l.clone().add(0, 0.3, 0), Material.DIRT, 6, 0.3));
                        for (LivingEntity e : Fx.near(p, c, 8)) {
                            Fx.hurt(p, e, 14);
                            Fx.pushAway(e, c, 1.2, 0.7);
                        }
                    }
                }
            }.runTaskTimer(Fx.pl(), 1, 1);
        }));
    }
}
