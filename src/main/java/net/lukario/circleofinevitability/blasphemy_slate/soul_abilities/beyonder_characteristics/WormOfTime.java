package net.lukario.circleofinevitability.blasphemy_slate.soul_abilities.beyonder_characteristics;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.combat.Later;
import net.lukario.circleofinevitability.combat.MeleeCombo;
import net.lukario.circleofinevitability.combat.SpellFx;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.particles.fx.ModelFx;
import net.lukario.circleofinevitability.particles.fx.ParticleFx;
import net.lukario.circleofinevitability.particles.fx.ParticleShapes;
import net.lukario.circleofinevitability.particles.fx.SlashFx;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonder;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.SoulCore;
import net.lukario.circleofinevitability.status.FallGuard;
import net.lukario.circleofinevitability.status.Marks;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class WormOfTime {

    private static final String PATHWAY = "Worm Of Time";

    private static final String LIFE_THEFT_VULNERABILITY = "word_of_time_life_theft_vulnerability";

    private static final int COMBO_HEAD = 0xF2F4F2C9;   // cream front
    private static final int COMBO_MID = 0xEE52E3C4;    // mint cyan middle
    private static final int COMBO_TAIL = 0xA05B3B33;   // dark brown fading tail

    private static final SlashFx COMBO_RING = SlashFx.arc("slash/band")
            .color(COMBO_MID).tailColor(COMBO_TAIL).headColor(COMBO_HEAD)
            .translucent()
            .radius(3.6f).arc(320f).width(1f).taper(SlashFx.Taper.COMET)
            .layers(4).spread(0.28f)
            .lifetime(12).sweep(5).spin(10f);             // draws itself, keeps spinning, then wipes away



    private static final SlashFx COMBO_TILTED_RING = COMBO_RING
            .radius(2.4f).arc(-270f).width(1.2f)          // negative arc = sweeps the other way
            .sweep(4).spin(-8f)
            .rotation(0f, 0, -45);                     // big arch over and around you, tilted


    private static final SlashFx COMBO_GROUND_SPIRAL = COMBO_RING.flat()
            .radius(1.2f).endRadius(3.6f).arc(540f).width(1.3f)   // lies on the ground, winds outward
            .spread(0.4f)
            .lifetime(14).sweep(6).spin(9f);
    private static final SlashFx COMBO_CRYSTAL = SlashFx.line("slash/crystal")
            .color(0xC89CFFE6).core(0x70FFFFFF)
            .radius(1.9f).width(0.32f).taper(SlashFx.Taper.CRESCENT)   // pointed at both ends
            .lifetime(16).sweep(2).fromCenter();                       // shoots out, holds, fades
    private static final ParticleFx COMBO_STAR = ParticleFx.of("fx/spark")
            .color(0xFFB8FFEE).fadeOut()
            .size(0.12f).endSize(0.03f).sizeRandom(0.4f)
            .lifetime(28, 14).friction(0.96f)
            .glow().spin(12f).randomRotation();
    private static final ParticleFx COMBO_STAR_WHITE = COMBO_STAR.color(0xFFFFFFFF).fadeOut();

    // =========================
    // Ability 2 - Theft: the numbers and the looks
    // =========================
    private static final int THEFT_SEQUENCE = 8;
    private static final int THEFT_COST = 250;                 // for stealing. Releasing costs nothing
    private static final double THEFT_RANGE = 24;
    private static final int THEFT_HELD_TICKS = 1200;          // a stolen ability is held for a minute at most
    private static final int THEFT_MOB_EMPTY_TICKS = 600;      // a mob that was stolen from has nothing more to give for this long (0 = no limit)
    private static final int THEFT_FLIGHT_TICKS = 10;          // how long the stolen light takes to reach the thief
    private static final float THEFT_ORB_SCALE = 0.28f;        // the lights circling over the thief's head: their size,
    private static final double THEFT_ORB_RADIUS = 0.5;        // how far from the middle they circle (blocks),
    private static final float THEFT_ORB_SPIN = 6f;            // and how fast (degrees per tick)
    private static final String THEFT_EMPTY = "worm_of_time_emptied";    // the mark (status/Marks) on a mob that was stolen from
    private static final String THEFT_ORB_TAG = "worm_of_time_stolen_";  // + the slot: the tag of one light over the head

    private static final SlashFx THEFT_THREAD = SlashFx.line("slash/smooth")
            .color(COMBO_MID).core(0xE0FFFFFF)
            .width(0.09f).taper(SlashFx.Taper.UNIFORM);
    private static final ModelFx THEFT_LIGHT = ModelFx.of("orb")
            .pivot(8, 8, 8).glow().unshaded().seeThrough()
            .color(0xF08CFFE6);

    public static final MeleeCombo MELEE_COMBO = MeleeCombo.forPathway(PATHWAY)
            .step(WormOfTime::comboRing)
            .step(WormOfTime::comboTiltedRing)
            .step(WormOfTime::comboTiltedRing)
            .step(WormOfTime::comboTiltedRing)
            .step(WormOfTime::comboCrystalCross)
            .randomStep(WormOfTime::comboGroundSpiral, WormOfTime::comboCrystalCross);

    public static void wormOfTimeAbility1(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!canUseCharacteristic(player, bypassClassCheck)) return;
        if (SoulCore.getSpirituality(player) < 250) return;
        if (!Beyonder.hasSequence(player, 9)) return;

        SoulCore.setSpirituality(player,SoulCore.getSpirituality(player)-250);


        if (!player.isShiftKeyDown()){

            LivingEntity livingEntity = Spells.aimEnemy(player,sl,24);
            if (livingEntity == null) return;
            int hitCount = livingEntity.getPersistentData().getIntOr(LIFE_THEFT_VULNERABILITY, 0);
            if (hitCount==0)hitCount=1;
            int healthStealAmount = 15*hitCount;

            if (livingEntity.getHealth()>healthStealAmount){
                livingEntity.setHealth(livingEntity.getHealth()-healthStealAmount);
                player.sendSystemMessage(Component.literal(""+hitCount));
                player.setHealth(player.getHealth()+healthStealAmount);
            }else{
                player.setHealth((player.getHealth()+livingEntity.getHealth()));
                livingEntity.kill(sl);
            }
            livingEntity.getPersistentData().putInt(LIFE_THEFT_VULNERABILITY,hitCount+1);

        }else{
            if (Root.has(player, RootRestriction.TELEPORT)) {
                player.sendSystemMessage(Component.literal("You are held in place."));
                return;
            }

            Vec3 there = Spells.blinkSpot(player, sl, 32);

            LivingEntity livingEntity = Spells.aimEnemy(player,sl,32);
            if (livingEntity!=null){
                there=livingEntity.position();
            }

            if (there == null) {
                player.sendSystemMessage(Component.literal("There is no room for you there."));
                return;                                                  // nothing spent
            }
            player.teleportTo(there.x, there.y, there.z);
            FallGuard.protect(player, 200);

        }
    }


    // =========================
    // Ability 2 - Theft
    // Normal: steals an ability from what you look at and holds it. From a player with a Pathway: one of the
    //         abilities their Sequence gives them, and they cannot use it while you hold it. From anything
    //         else: a random ability of another Pathway.
    // Sneak:  releases the newest one you hold: it is cast once, whatever its Sequence, at no cost.
    // Who holds what is kept by StolenAbilities; the list of abilities is PathwayAbilities.
    // =========================
    public static void wormOfTimeAbility2(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!canUseCharacteristic(player, bypassClassCheck)) return;
        if (!Beyonder.hasSequence(player, THEFT_SEQUENCE)) return;

        if (player.isShiftKeyDown()) releaseStolen(player, level, sl);
        else steal(player, sl);
    }

    private static void steal(Player player, ServerLevel sl) {
        LivingEntity target = Spells.aimEnemy(player, sl, THEFT_RANGE);
        if (target == null) {
            player.sendSystemMessage(Component.literal("No one stands before you."));
            return;                                                  // nothing spent
        }
        if (StolenAbilities.isFull(player)) {
            player.sendSystemMessage(Component.literal("You cannot hold more than " + StolenAbilities.MAX_HELD
                    + " stolen abilities. Release one first (sneak)."));
            return;
        }
        PathwayAbilities.Ability ability = pickToSteal(player, target);
        if (ability == null) {
            player.sendSystemMessage(Component.literal(hasPathway(target)
                    ? "They have nothing you can steal right now."
                    : "It has nothing left to give. Look for another."));
            return;
        }
        if (!Spells.paySpirituality(player, THEFT_COST)) return;

        boolean sneakForm = ability.sneakForm() && player.getRandom().nextBoolean();
        Player victim = hasPathway(target) ? (Player) target : null;
        StolenAbilities.Held held = StolenAbilities.hold(player, ability, sneakForm, victim, THEFT_HELD_TICKS);
        if (held == null) return;                                    // (full: checked above)
        if (victim == null && THEFT_MOB_EMPTY_TICKS > 0) Marks.add(target, THEFT_EMPTY, 1, THEFT_MOB_EMPTY_TICKS);

        player.sendSystemMessage(Component.literal("You stole " + held.label() + " of the " + ability.pathway()
                + " Pathway. Sneak and press the key again to release it."));
        if (victim != null) {
            victim.sendSystemMessage(Component.literal("Your " + ability.name() + " was stolen."));
        }
        theftShows(player, sl, target, held, victim == null);
    }

    /** A player who follows a Pathway that has abilities: only those lose one of their own. */
    private static boolean hasPathway(LivingEntity target) {
        return target instanceof Player other && !PathwayAbilities.of(SoulCore.getPathway(other)).isEmpty();
    }

    /** What the theft takes from this target, or null if there is nothing to take right now. */
    private static PathwayAbilities.Ability pickToSteal(Player thief, LivingEntity target) {
        List<PathwayAbilities.Ability> choices = new ArrayList<>();
        if (hasPathway(target)) {
            Player victim = (Player) target;
            // what their Sequence gives them, and what nobody holds already
            for (PathwayAbilities.Ability ability : PathwayAbilities.of(SoulCore.getPathway(victim))) {
                if (ability.stealable() && Beyonder.hasSequence(victim, ability.sequence())
                        && !StolenAbilities.isTaken(victim, ability)) {
                    choices.add(ability);
                }
            }
        } else {
            if (Marks.count(target, THEFT_EMPTY) > 0) return null;
            // anything of another Pathway, whatever its Sequence
            for (PathwayAbilities.Ability ability : PathwayAbilities.ALL) {
                if (ability.stealable() && !ability.pathway().equals(PATHWAY)) choices.add(ability);
            }
        }
        return choices.isEmpty() ? null : choices.get(thief.getRandom().nextInt(choices.size()));
    }

    private static void releaseStolen(Player player, Level level, ServerLevel sl) {
        StolenAbilities.Held held = StolenAbilities.newest(player);
        if (held == null) {
            player.sendSystemMessage(Component.literal("You hold nothing stolen."));
            return;
        }
        if (!castStolen(player, level, sl, held)) {
            // it refused (no target, no room...) and has said why: it is not used up
            player.sendSystemMessage(Component.literal(held.label() + " did not go off. You still hold it."));
            return;
        }
        StolenAbilities.spend(player, held);
        releaseShows(player, sl, held);

        StolenAbilities.Held next = StolenAbilities.newest(player);
        player.sendSystemMessage(Component.literal("Released " + held.label() + "."
                + (next == null ? "" : " Next: " + next.label() + ".")));
    }

    /**
     * Casts a held ability for the thief: no Pathway check, no Sequence gate, no cost (Beyonder.castUnbound).
     * True if it went off. The thief is sneaking to release it, so for that moment they are made to stand or
     * sneak as the stolen form needs.
     */
    private static boolean castStolen(Player player, Level level, ServerLevel sl, StolenAbilities.Held held) {
        boolean wasSneaking = player.isShiftKeyDown();
        player.setShiftKeyDown(held.sneakForm());
        try {
            return Beyonder.castUnbound(player, () -> held.ability().cast().run(player, level, sl));
        } catch (RuntimeException e) {
            CircleOfInevitability.LOGGER.error("A stolen ability failed: {}", held.label(), e);
            return true;                                             // used up all the same, so it cannot fail again and again
        } finally {
            player.setShiftKeyDown(wasSneaking);
        }
    }

    // ---------- what is seen and heard (nothing below changes what happens) ----------

    /** The theft: a ring closes on the target, a thread joins it to the thief's hand and a light flies along it. */
    private static void theftShows(Player player, ServerLevel sl, LivingEntity target, StolenAbilities.Held held, boolean emptied) {
        Vec3 heart = target.getBoundingBox().getCenter();
        SpellFx.closingRing(sl, target.position(), Math.max(1.2, target.getBbWidth() * 1.6), COMBO_MID, 8, 0);
        ParticleShapes.burst(sl, COMBO_STAR_WHITE, heart, 18, 0.05, 0.22);
        SpellFx.tether(sl, THEFT_THREAD, () -> SpellFx.middleOf(target, sl), () -> SpellFx.handOf(player, sl),
                THEFT_FLIGHT_TICKS + 4);
        flyInto(sl, THEFT_LIGHT.scale(0.5f).spin(24f).lifetime(THEFT_FLIGHT_TICKS + 3).fade(1, 3),
                heart, player, 1.1, THEFT_FLIGHT_TICKS);
        Spells.sound(sl, target.position(), SoundEvents.ENCHANTMENT_TABLE_USE, 0.9f, 1.7f);
        Spells.sound(sl, player.position(), SoundEvents.ILLUSIONER_CAST_SPELL, 0.7f, 1.4f);

        // a mob that has nothing more to give carries a mark for as long as that lasts
        if (emptied && THEFT_MOB_EMPTY_TICKS > 0) {
            SpellFx.circleUnder(sl, target, Math.max(0.6, target.getBbWidth()), 0x9052E3C4, THEFT_MOB_EMPTY_TICKS, THEFT_EMPTY);
        }

        // the light arrives: it joins the ones circling over the thief's head
        Later.run(sl, THEFT_FLIGHT_TICKS, () -> {
            if (!Spells.casterStillHere(player, sl) || !StolenAbilities.holds(player, held)) return;
            float outPixels = (float) (THEFT_ORB_RADIUS * 16.0 / THEFT_ORB_SCALE);   // a pivot this far from its middle makes it circle
            ModelFx light = THEFT_LIGHT.scale(THEFT_ORB_SCALE).pivot(8 + outPixels, 8, 8).spin(THEFT_ORB_SPIN)
                    .lifetime(THEFT_HELD_TICKS - THEFT_FLIGHT_TICKS).fade(6, 20)
                    .tag(THEFT_ORB_TAG + held.slot());
            ParticleShapes.modelOn(sl, light, player, new Vec3(0, player.getBbHeight() + 0.55, 0),
                    held.slot() * 360f / StolenAbilities.MAX_HELD, 0f, 0f);
            SpellFx.risingRings(sl, player.position(), 0.9, 1.8, 3, COMBO_HEAD);
            Spells.sound(sl, player.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 1f, 1.5f);
        });
    }

    /** The release: its light over the head goes out, and a ring and a ribbon of the Pathway's colors burst from the thief. */
    private static void releaseShows(Player player, ServerLevel sl, StolenAbilities.Held held) {
        ParticleShapes.clearModels(sl, player, THEFT_ORB_TAG + held.slot());
        Vec3 chest = player.position().add(0, 1.1, 0);
        SpellFx.shockRing(sl, player.position(), 2.4, COMBO_HEAD, 8, 0);
        ParticleShapes.slash(sl, COMBO_RING.radius(1.6f).lifetime(8).sweep(3), chest, player.getYRot(), 0f, 0f);
        ParticleShapes.burst(sl, COMBO_STAR, chest.add(0, 0.9, 0), 22, 0.08, 0.3);
        Spells.sound(sl, player.position(), SoundEvents.BEACON_POWER_SELECT, 0.8f, 1.6f);
    }

    /** Like SpellFx.homing, but the model ends 'height' blocks above the entity's feet instead of at them. */
    private static void flyInto(ServerLevel sl, ModelFx model, Vec3 from, Entity target, double height, int ticks) {
        Vec3 way = target.position().add(0, height, 0).subtract(from);
        float away = (float) Math.sqrt(way.x * way.x + way.z * way.z);
        float yaw = away < 0.1f ? target.getYRot() : Spells.yawOf(way);
        ModelFx flying = model
                .during(0, 0, ModelFx.pose().forward(-away).up((float) -way.y))                     // where it starts, seen from where it ends
                .during(0, Math.max(1, ticks), ModelFx.pose().forward(0f).up(0f), ModelFx.Ease.IN);
        ParticleShapes.modelOn(sl, flying, target, new Vec3(0, height, 0), yaw, 0f, 0f);
    }


    private static void comboRing(ServerPlayer player, LivingEntity target, ServerLevel sl) {
        Vec3 center = player.position().add(0, 0.9, 0);
        // sweeps either way, starts from a different side each time, slightly tilted
        SlashFx ring = COMBO_RING.varied(player.getRandom(), 60f, 8f, 12f);
        ParticleShapes.slash(sl, ring, center, player.getYRot(), 0f, 0f);
        sl.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1f, 0.8f);
    }

    // Hit 2: a tilted, almost upright ring around the player
    private static void comboTiltedRing(ServerPlayer player, LivingEntity target, ServerLevel sl) {
        Vec3 center = player.position().add(0, 1.1, 0);
        // tilted to the left or right side (mirrored), leaning a bit more or less each time
        SlashFx ring = COMBO_TILTED_RING.varied(player.getRandom(), 45f, 45f, 45f);
        ParticleShapes.slash(sl, ring, center, player.getYRot(), 0f, 0f);
        sl.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1f, 1.1f);
    }


    // Hit 3 (a): spiral whirling on the ground around the player
    private static void comboGroundSpiral(ServerPlayer player, LivingEntity target, ServerLevel sl) {
        Vec3 center = player.position().add(0, 0.15, 0);
        // starts at any angle and spins either way (both spirals the same way)
        RandomSource random = player.getRandom();
        SlashFx spiral = random.nextBoolean() ? COMBO_GROUND_SPIRAL : COMBO_GROUND_SPIRAL.mirrored();
        float start = random.nextFloat() * 360f;
        ParticleShapes.slash(sl, spiral, center, start, 0f, 0f);
        ParticleShapes.slash(sl, spiral.layers(2).radius(0.8f).endRadius(2.6f).delay(3),
                center.add(0, 0.05, 0), start + 180f, 0f, 0f);   // second, smaller spiral
        sl.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1f, 0.6f);
    }

    // Hit 3 (b): crossed crystal blades in an X on the target + a swirling galaxy of sparkles under it
    private static void comboCrystalCross(ServerPlayer player, LivingEntity target, ServerLevel sl) {
        Vec3 hit = target.getBoundingBox().getCenter();
        RandomSource random = player.getRandom();
        float xTurn = (random.nextFloat() * 2f - 1f) * 25f;   // the whole X turned a bit
        float xOpen = 35f + random.nextFloat() * 20f;          // wide or narrow X
        for (int bundle = 0; bundle < 2; bundle++) {
            float baseRoll = xTurn + (bundle == 0 ? xOpen : -xOpen);   // the two diagonals
            for (int i = 0; i < 4; i++) {
                Vec3 center = hit.add((random.nextDouble() - 0.5) * 0.4, (random.nextDouble() - 0.5) * 0.4,
                        (random.nextDouble() - 0.5) * 0.4);
                SlashFx blade = COMBO_CRYSTAL
                        .radius(1.5f + random.nextFloat() * 0.7f)
                        .width(0.24f + random.nextFloat() * 0.14f)
                        .delay(bundle * 2 + i / 2);      // blades appear in quick steps
                float yaw = player.getYRot() + (random.nextFloat() - 0.5f) * 20f;
                float roll = baseRoll + (random.nextFloat() - 0.5f) * 16f;
                ParticleShapes.slash(sl, blade, center, yaw, 0f, roll);
            }
        }
        Vec3 ground = target.position().add(0, 0.15, 0);
        ParticleShapes.spiral(sl, COMBO_STAR, ground, 4, 220, 0.6, 4.0, 0.8, 0.06);
        ParticleShapes.spiral(sl, COMBO_STAR_WHITE, ground, 4, 90, 0.6, 4.0, 0.8, 0.06);
        sl.playSound(null, target.blockPosition(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 1.2f, 1.2f);
        sl.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1f, 1f);
    }

    private static boolean canUseCharacteristic(Player player, boolean bypassClassCheck){
        if (bypassClassCheck) return true;
        return "Worm Of Time".equals(SoulCore.getPathway(player));
    }
}
