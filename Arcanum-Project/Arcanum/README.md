# Arcanum - mage plugin for Paper 1.21.11

Elemental staffs (4 levels), single-use scrolls, infinite-use spell books, a mana system,
an Archmage Multi-Staff, and a first-join "choose your mage" popup. Includes the resource pack.

## Build
Push to GitHub. The workflow builds the plugin jar (Actions > Artifacts). The ready-made resource pack is the committed `Arcanum-ResourcePack.zip`.

## Install
1. Put `Arcanum-1.0.0.jar` in `plugins/`.
2. Host `Arcanum-ResourcePack.zip` (from this repo) at a direct link, e.g. its GitHub "raw" URL or a release.
3. In `plugins/Arcanum/config.yml` set `resource-pack.url` and `resource-pack.sha1`, then `/mage reload`.

## Gameplay
- Elements: Fire, Ice, Dark, Light, Lightning, Nature, Wind, Earth - 8 spells each (64 total).
- Staff level N unlocks 2N spells (L1 = 2, L2 = 4, L3 = 6, L4 = 8).
- Right-click casts, left-click or Shift+Scroll switches spell.
- Upgrade: craft staff + Arcane Crystal (II, III, IV) in the crafting grid.
- Level 1 staff: gold, element focus, sticks (`GFG / _S_ / _S_`).
- Scroll: paper + element focus + spell catalyst (+ lapis for higher tiers). Book: book + scroll + lapis.
- Multi-Staff: ring of 8 different elemental staffs (level 3+) around an Arcane Crystal IV.
  Starts at level 2; Shift+Right-click to bind any spells.
- Your own element costs less mana and recharges faster. Casting raises your mage rank (more max mana).

## Commands
`/mage info | choose | spells [element] | pack`
Admin: `/mage give <player> staff|multi|scroll|book|crystal ...`, `setmana`, `setmage`, `reset`, `reload`
