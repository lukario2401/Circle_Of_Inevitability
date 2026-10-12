package net.lukario.circleofinevitability.blasphemy_slate.soul_abilities.beyonder_characteristics;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonder;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.SoulCore;
import net.lukario.circleofinevitability.combat.Later;
import net.lukario.circleofinevitability.combat.Shot;
import net.lukario.circleofinevitability.combat.SpellFx;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.combat.Transit;
import net.lukario.circleofinevitability.combat.Trap;
import net.lukario.circleofinevitability.particles.fx.ModelFx;
import net.lukario.circleofinevitability.particles.fx.ParticleFx;
import net.lukario.circleofinevitability.particles.fx.ParticleShapes;
import net.lukario.circleofinevitability.particles.fx.SlashFx;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.lukario.circleofinevitability.status.Concealment;
import net.lukario.circleofinevitability.status.Marionette;
import net.lukario.circleofinevitability.status.Taunt;
import net.lukario.circleofinevitability.status.Vulnerable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Conqueror: the Hunter pathway (Red Priest) of Lord of the Mysteries. Each key is one step up the pathway,
 * and every ability has a normal cast and a sneak cast:
 *
 *   1 Hunter's Trap        (Hunter)               a hidden trap that holds and hurts whoever steps on it       | sneak: Explosive Charge, a fuse and a blast
 *   2 Provocation          (Provoker)             a mob drops everything to come for you, and takes more damage | sneak: Repugnant Aura, the mobs around you fight each other
 *   3 Fire Ravens          (Pyromaniac)           not built yet
 *   4 Flame Transformation (Conspirer)            merge with a flame lance and step out where it lands         | sneak: Misdirection, vanish and leave a flame figure that blows up
 *   5 Cull                 (Reaper)               not built yet
 *   6 Galvanization        (Iron-blooded Knight)  not built yet
 *   7 Fog of War           (War Bishop)           not built yet
 *
 * ADDING THE NEXT ABILITY (four places, nothing else):
 *   1. Here: its numbers in "Ability settings", then a method like conquerorHuntersTrap under its own heading.
 *      Start it with ready(player, bypassClassCheck, SEQUENCE), check that the cast makes sense, pay, then do it.
 *   2. network/SKeyPressAbility<Two..Seven>Used: one import static and one line next to the other Pathways'.
 *   3. PathwayAbilities.ALL: one line under "---- Conqueror ----" (the same name and Sequence as here).
 *   4. docs/SEQUENCES.md: its place in the Conqueror row of "Required Sequence of every ability key".
 * Later, if they are wanted: a melee combo (MeleeCombo.forPathway(PATHWAY) here + one register line in
 * combat/MeleeCombos, and this class in SlashFxCommand.SOURCES once it has SlashFx constants), passives
 * (one line in SequencePassives) and a HUD icon (PathwayHudIcon). State on the player goes into
 * player.getPersistentData() under keys that start with "conqueror_"; an event subscriber class in here needs
 * a name nobody else has (ConquerorEvents).
 *
 * What is reusable lives outside this class (see docs/SPELL_KIT.md): combat/Trap (the trap itself),
 * status/Taunt (a mob made to go after one entity: you, or another mob), combat/Transit (travel inside a Shot),
 * status/Vulnerable, status/Concealment, combat/Spells, combat/SpellFx, combat/Later, root/Root. fireBlast (where, how wide, how hard) and fuseBlast (the same
 * after a fuse) below are written so the fire abilities to come can call them too.
 *
 * Nothing here breaks blocks or lights real fire. The numbers for every ability are right below.
 */
public class Conqueror {

    private static final String PATHWAY = "Conqueror";

    // =========================
    // Ability settings (cost = spirituality, ticks: 20 = 1 second, distances in blocks)
    // =========================

    // Ability 1 - Hunter's Trap (Sequence 9)
    private static final int TRAP_SEQUENCE = 9;
    private static final int TRAP_COST = 250;
    private static final double TRAP_RANGE = 16.0;               // how far away a trap can be set
    private static final double TRAP_RADIUS = 1.5;               // how close to its middle someone must step
    private static final int TRAP_MAX = 3;                       // traps lying at once: one more replaces the oldest
    private static final int TRAP_ARM_TICKS = 20;                // it cannot spring for this long after it was set
    private static final int TRAP_TICKS = 1200;                  // how long it waits before it is gone
    private static final float TRAP_DAMAGE = 40f;                // on everything standing on it when it springs
    private static final int TRAP_HOLD_TICKS = 60;               // how long they are held in place
    private static final int TRAP_GLINT_EVERY = 40;              // ticks between the glints that show where it lies
    private static final int TRAP_COLOR = 0xC0D8DCE4;            // pale steel
    private static final String TRAP_NAME = "conqueror_trap";    // (combat/Trap: the kind of trap)
    private static final int CHARGE_COST = 250;                  // sneak: Explosive Charge
    private static final double CHARGE_RANGE = 16.0;
    private static final int CHARGE_FUSE = 40;                   // ticks from planting it to the blast
    private static final int CHARGE_TICK_EVERY = 10;             // ticks between the sparks and clicks of the fuse
    private static final double CHARGE_RADIUS = 4.0;
    private static final float CHARGE_DAMAGE = 48f;              // in the middle of the blast
    private static final int CHARGE_COLOR = 0xE0FF4A1A;          // the warning circle: red-orange

    // Ability 2 - Provocation (Sequence 8)
    private static final int PROVOKE_SEQUENCE = 8;
    private static final int PROVOKE_COST = 300;                 // Sequence 8 holds 400, so it can be paid from there on
    private static final double PROVOKE_RANGE = 24.0;
    private static final int PROVOKE_TICKS = 200;                // how long the target stays provoked
    private static final float PROVOKE_EXTRA = 0.25f;            // it takes this much more damage meanwhile (0.25 = 25%)
    private static final int PROVOKE_COLOR = 0xD0FF3A2A;         // angry red
    private static final String PROVOKED_MARK = "conqueror_provoked";   // the tag of the circle under a provoked one
    private static final int AURA_COST = 300;                    // sneak: Repugnant Aura
    private static final double AURA_RADIUS = 10.0;              // the mobs this close turn on each other
    private static final int AURA_MAX_TARGETS = 12;              // the nearest this many
    private static final int AURA_TICKS = 200;                   // how long they fight each other
    private static final int AURA_COLOR = 0xC0602A1E;            // murky red-brown

    // Ability 4 - Flame Transformation (Sequence 6)
    private static final int FLAME_SEQUENCE = 6;
    private static final int FLAME_COST = 1500;
    private static final double FLAME_RANGE_MIN = 10.0;          // how far the lance flies when the Sequence is new
    private static final double FLAME_RANGE_MAX = 30.0;          // ...and once it is fully digested (and from Sequence 5)
    private static final double LANCE_SPEED = 2.5;               // blocks per tick
    private static final float LANCE_DAMAGE = 30f;               // on the enemy it flies into (it stops there)
    private static final double FLAME_BURST_RADIUS = 2.5;        // the bursts where you leave and where you arrive
    private static final float FLAME_BURST_DAMAGE = 16f;
    private static final int DECOY_COST = 1500;                  // sneak: Misdirection
    private static final int DECOY_FUSE = 40;                    // ticks until the figure of flame goes off
    private static final double DECOY_RADIUS = 3.5;
    private static final float DECOY_DAMAGE = 36f;
    private static final int DECOY_HIDE_TICKS = 60;              // how long you stay hidden
    private static final int DECOY_SPEED_LEVEL = 1;              // 0 = Speed I, 1 = Speed II while hidden

    // Fire blasts (the Explosive Charge and Flame Transformation, the fire abilities later)
    private static final float BLAST_EDGE = 0.5f;                // part of the damage that is left at the rim
    private static final double BLAST_PUSH = 1.1;                // how hard it throws back, blocks per tick
    private static final double BLAST_LIFT = 0.45;
    private static final int BLAST_BURN_TICKS = 80;              // how long what it hit keeps burning

    // =========================
    // Small particles that go with them
    // =========================
    private static final ParticleFx TRAP_GLINT = ParticleFx.of("fx/glow")
            .color(0xFFE8ECF4).endColor(0x00707888)
            .size(0.08f).endSize(0.02f).sizeRandom(0.4f)
            .lifetime(16, 8).friction(0.9f)
            .glow();
    private static final ParticleFx ANGER_SPARK = ParticleFx.of("fx/spark")
            .color(0xFFFF4A3A).endColor(0x00801010)
            .size(0.12f).endSize(0.03f).sizeRandom(0.4f)
            .lifetime(18, 10).friction(0.9f)
            .glow();
    private static final SlashFx PROVOKE_LINE = SlashFx.line("slash/smooth")     // caster to the provoked, for a moment
            .color(0xC0FF3A2A).core(0xE0FFE0D0)
            .width(0.08f).taper(SlashFx.Taper.UNIFORM);

    private static final ModelFx LANCE_MODEL = ModelFx.of("fireball")       // the shared fireball, stretched into a lance
            .scale(0.6f, 1.6f, 0.6f).pivot(8, 8, 8).glow().unshaded().seeThrough()
            .aura(0xFFFF7A1A, 0.14f, 3).spin(26f);
    private static final SlashFx LANCE_STREAK = SlashFx.line("slash/smooth")
            .color(0xC0FF7A1A).core(0xE0FFE8B0)
            .width(0.35f).taper(SlashFx.Taper.COMET);
    /** The flame lance of Flame Transformation. Its range is set per cast (flameRange). */
    private static final Shot FLAME_LANCE = Shot.of(LANCE_MODEL)
            .speed(LANCE_SPEED).width(0.5).trail(LANCE_STREAK);

    // =========================
    // Ability 1 - Hunter's Trap
    // Normal: a hidden trap on the ground you aim at. After a second it is armed; the first enemy to step on it
    //         springs it, and everything standing on it is hurt and held in place. Up to TRAP_MAX lie at once.
    // Sneak:  Explosive Charge: a charge on the ground you aim at that goes off when its fuse has burnt down.
    // =========================
    public static void conquerorHuntersTrap(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!ready(player, bypassClassCheck, TRAP_SEQUENCE)) return;

        boolean sneak = player.isShiftKeyDown();
        Vec3 ground = Spells.aimGround(player, sl, sneak ? CHARGE_RANGE : TRAP_RANGE);
        if (!onSolidGround(sl, ground)) {
            player.sendSystemMessage(Component.literal("There is no ground there to set it on."));
            return;                                                  // nothing spent
        }
        if (!Spells.paySpirituality(player, sneak ? CHARGE_COST : TRAP_COST)) return;

        if (sneak) plantCharge(player, sl, ground);
        else setTrap(player, sl, ground);
    }

    /** A circle shows where it is being set, then it lies hidden: only a glint now and then gives it away. */
    private static void setTrap(Player player, ServerLevel sl, Vec3 ground) {
        Trap.at(ground, TRAP_RADIUS)
                .armsAfter(TRAP_ARM_TICKS)
                .lasts(TRAP_TICKS)
                .showEvery(TRAP_GLINT_EVERY)
                .onWait(ticksLeft -> ParticleShapes.ring(sl, TRAP_GLINT, ground.add(0, 0.1, 0), TRAP_RADIUS * 0.6, 6, 0))
                .onSpring(victim -> springTrap(player, sl, ground, victim))
                .set(player, sl, TRAP_NAME, TRAP_MAX);

        SpellFx.warningCircle(sl, ground, TRAP_RADIUS, TRAP_COLOR, TRAP_ARM_TICKS);
        Spells.sound(sl, ground, SoundEvents.CHAIN_PLACE, 0.8f, 0.8f);
    }

    /** The trap closes: the one who stepped on it and everything else standing on it is hurt and held in place. */
    private static void springTrap(Player player, ServerLevel sl, Vec3 ground, LivingEntity victim) {
        List<LivingEntity> caught = Spells.enemiesAround(player, sl, ground.add(0, 0.5, 0), TRAP_RADIUS);
        if (!caught.contains(victim)) caught.add(victim);            // (it may have been in the air above the trap)
        for (LivingEntity target : caught) {
            Spells.strike(player, target, TRAP_DAMAGE);
            Root.apply(target, TRAP_HOLD_TICKS, RootRestriction.MOVEMENT);
        }

        SpellFx.closingRing(sl, ground, TRAP_RADIUS * 1.6, TRAP_COLOR, 6, 0);
        ParticleShapes.burst(sl, TRAP_GLINT, ground.add(0, 0.4, 0), 24, 0.08, 0.3);
        Spells.sound(sl, ground, SoundEvents.ANVIL_LAND, 0.5f, 1.8f);
        Spells.sound(sl, ground, SoundEvents.PLAYER_ATTACK_CRIT, 1f, 0.8f);
        player.sendSystemMessage(Component.literal("Your trap has sprung."));
    }

    /** Sneak: the charge lies in a red circle while its fuse burns, then it goes off. */
    private static void plantCharge(Player player, ServerLevel sl, Vec3 ground) {
        fuseBlast(player, sl, ground, CHARGE_FUSE, CHARGE_RADIUS, CHARGE_DAMAGE);
    }

    // =========================
    // Ability 2 - Provocation
    // Normal: the enemy you look at is provoked for PROVOKE_TICKS. A mob drops everything to come for you
    //         (status/Taunt), and anything provoked, players too, takes PROVOKE_EXTRA more damage meanwhile.
    // Sneak:  Repugnant Aura: the mobs around you turn on each other.
    // =========================
    public static void conquerorProvocation(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!ready(player, bypassClassCheck, PROVOKE_SEQUENCE)) return;

        if (player.isShiftKeyDown()) repugnantAura(player, sl);
        else provoke(player, sl);
    }

    private static void provoke(Player player, ServerLevel sl) {
        LivingEntity target = Spells.aimEnemy(player, sl, PROVOKE_RANGE);
        if (target == null) {
            player.sendSystemMessage(Component.literal("No one stands before you."));
            return;                                                  // nothing spent
        }
        if (!Spells.paySpirituality(player, PROVOKE_COST)) return;

        Vulnerable.add(target, PROVOKE_EXTRA, PROVOKE_EXTRA, PROVOKE_TICKS);    // a fixed share: casting again does not stack it
        Taunt.apply(target, player, PROVOKE_TICKS);                             // (does nothing to a player: they have no AI)
        if (target instanceof Player other) {
            other.sendSystemMessage(Component.literal("You have been provoked. Every blow lands harder on you for a while."));
        }
        provokeShows(player, sl, target);
    }

    private static void repugnantAura(Player player, ServerLevel sl) {
        List<LivingEntity> near = new ArrayList<>();
        for (LivingEntity enemy : Spells.enemiesAround(player, sl, player.position().add(0, 1, 0), AURA_RADIUS)) {
            if (enemy instanceof Mob && !Marionette.isMarionette(enemy)) near.add(enemy);   // only mobs can be steered
        }
        near.sort(Comparator.comparingDouble(enemy -> enemy.distanceToSqr(player)));
        if (near.size() > AURA_MAX_TARGETS) near = new ArrayList<>(near.subList(0, AURA_MAX_TARGETS));
        if (near.size() < 2) {
            player.sendSystemMessage(Component.literal("There is nothing here to set against each other."));
            return;                                                  // nothing spent
        }
        if (!Spells.paySpirituality(player, AURA_COST)) return;

        Taunt.brawl(near, AURA_TICKS, player.getRandom());
        auraShows(player, sl, near);
    }

    // ---------- what is seen and heard (nothing below changes what happens) ----------

    /** A red line flashes from the caster to the target, sparks fly over its head and a red circle stays under it. */
    private static void provokeShows(Player player, ServerLevel sl, LivingEntity target) {
        SpellFx.tether(sl, PROVOKE_LINE, player, target, 10);
        SpellFx.circleUnder(sl, target, Math.max(0.6, target.getBbWidth()), PROVOKE_COLOR, PROVOKE_TICKS, PROVOKED_MARK);
        ParticleShapes.burst(sl, ANGER_SPARK, target.position().add(0, target.getBbHeight() + 0.3, 0), 14, 0.05, 0.2);
        Spells.sound(sl, target.position(), SoundEvents.ENDER_DRAGON_GROWL, 0.4f, 1.8f);
        Spells.sound(sl, player.position(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, 0.8f, 0.7f);
    }

    /** A murky wave rolls out from the caster, and every mob that was turned gets a red circle and angry sparks. */
    private static void auraShows(Player player, ServerLevel sl, List<LivingEntity> turned) {
        SpellFx.shockRing(sl, player.position(), AURA_RADIUS, AURA_COLOR, 12, 0);
        SpellFx.shockRing(sl, player.position(), AURA_RADIUS * 0.6, PROVOKE_COLOR, 10, 3);
        for (LivingEntity mob : turned) {
            if (!Taunt.isTaunted(mob)) continue;
            SpellFx.circleUnder(sl, mob, Math.max(0.6, mob.getBbWidth()), PROVOKE_COLOR, AURA_TICKS, PROVOKED_MARK);
            ParticleShapes.burst(sl, ANGER_SPARK, mob.position().add(0, mob.getBbHeight() + 0.3, 0), 8, 0.04, 0.16);
        }
        Spells.sound(sl, player.position(), SoundEvents.WITHER_AMBIENT, 0.6f, 0.6f);
    }

    // =========================
    // Ability 4 - Flame Transformation
    // Normal: throw a flame lance and merge with it (combat/Transit): you travel inside it and step out where it
    //         lands, with a burst of fire where you left and where you arrive. It reaches FLAME_RANGE_MIN blocks
    //         when the Sequence is new and FLAME_RANGE_MAX once it is digested (always the most from Sequence 5).
    // Sneak:  Misdirection: you vanish, quick and unseen, and leave a figure of flame that blows up behind you.
    // =========================
    public static void conquerorFlameTransformation(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!ready(player, bypassClassCheck, FLAME_SEQUENCE)) return;

        if (player.isShiftKeyDown()) misdirection(player, sl);
        else flameTransformation(player, sl);
    }

    private static void flameTransformation(Player player, ServerLevel sl) {
        if (!Transit.canGo(player)) {
            player.sendSystemMessage(Component.literal("You cannot leave this place."));
            return;                                                  // nothing spent
        }
        if (!Spells.paySpirituality(player, FLAME_COST)) return;

        double range = flameRange(player);
        Vec3 hand = Spells.clearStart(player, sl, player.getEyePosition().add(Spells.rightOf(player).scale(0.35)).add(0, -0.25, 0));
        Shot lance = FLAME_LANCE.range(range).onHit((target, at) -> {
            Spells.strike(player, target, LANCE_DAMAGE);
            target.setRemainingFireTicks(BLAST_BURN_TICKS);
        });
        Transit.by(lance)
                .onDepart(from -> fireBlast(player, sl, from.add(0, 0.6, 0), FLAME_BURST_RADIUS, FLAME_BURST_DAMAGE))
                .onArrive((at, hitBlock) -> fireBlast(player, sl, at.add(0, 0.6, 0), FLAME_BURST_RADIUS, FLAME_BURST_DAMAGE))
                .go(player, sl, hand, Spells.aimFrom(player, sl, hand, range));
        Spells.sound(sl, player.position(), SoundEvents.BLAZE_SHOOT, 1f, 0.8f);
    }

    /** How far the lance flies: grows with the digestion of Sequence 6, the full reach from Sequence 5 on. */
    private static double flameRange(Player player) {
        if (Beyonder.hasSequence(player, FLAME_SEQUENCE - 1)) return FLAME_RANGE_MAX;
        float digested = Math.max(0f, Math.min(100f, SoulCore.getDigestion(player))) / 100f;
        return FLAME_RANGE_MIN + (FLAME_RANGE_MAX - FLAME_RANGE_MIN) * digested;
    }

    /** Sneak: a figure of flame stands where you stood and goes off after its fuse; you are hidden and quick meanwhile. */
    private static void misdirection(Player player, ServerLevel sl) {
        if (!Spells.paySpirituality(player, DECOY_COST)) return;

        Vec3 spot = player.position();
        ModelFx figure = SpellFx.FLAMES.scale(0.8f, 1.0f, 0.8f)     // about as tall as a player
                .lifetime(DECOY_FUSE).fade(0, 4).spin(3f);
        ParticleShapes.model(sl, figure, spot, player.getYRot(), 0f, 0f);
        fuseBlast(player, sl, spot, DECOY_FUSE, DECOY_RADIUS, DECOY_DAMAGE);

        Concealment.hide(player, DECOY_HIDE_TICKS);                 // mobs forget you and go for nothing
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, DECOY_HIDE_TICKS, DECOY_SPEED_LEVEL, false, false, true));
    }

    // =========================
    // Helpers shared by the abilities
    // =========================

    /**
     * A blast with a fuse at a spot on the ground: a red warning circle 'radius' wide, sparks and rising clicks
     * for 'fuse' ticks, then fireBlast. Nothing goes off if the caster died or left meanwhile.
     */
    private static void fuseBlast(Player player, ServerLevel sl, Vec3 ground, int fuse, double radius, float damage) {
        SpellFx.warningCircle(sl, ground, radius, CHARGE_COLOR, fuse);
        Spells.sound(sl, ground, SoundEvents.FIRECHARGE_USE, 0.8f, 0.7f);
        burnFuse(sl, ground, fuse, fuse);
        Later.run(sl, fuse, () -> {
            if (!Spells.casterStillHere(player, sl)) return;         // nobody left to set it off
            fireBlast(player, sl, ground.add(0, 0.6, 0), radius, damage);
        });
    }

    /** One spark and click of a fuse, then the next one CHARGE_TICK_EVERY ticks later: higher as it gets close. */
    private static void burnFuse(ServerLevel sl, Vec3 ground, int ticksLeft, int fuse) {
        if (ticksLeft <= 0) return;
        ParticleShapes.cone(sl, SpellFx.FIRE_EMBER, ground.add(0, 0.2, 0), Spells.UP, 40, 6, 0.05, 0.18);
        Spells.sound(sl, ground, SoundEvents.AMETHYST_BLOCK_CHIME, 0.6f, 2.0f - (float) ticksLeft / fuse);
        Later.run(sl, CHARGE_TICK_EVERY, () -> burnFuse(sl, ground, ticksLeft - CHARGE_TICK_EVERY, fuse));
    }

    /**
     * An explosion of fire at a spot: 'damage' in the middle, BLAST_EDGE of it at the rim. What it hits is
     * thrown back and set alight. Breaks no blocks.
     */
    private static void fireBlast(Player player, ServerLevel sl, Vec3 center, double radius, float damage) {
        for (LivingEntity target : Spells.enemiesAround(player, sl, center, radius)) {
            double near = 1.0 - Math.min(1.0, Spells.distanceToBody(target, center) / radius);   // 1 = in the middle, 0 = at the rim
            Spells.strike(player, target, (float) (damage * (BLAST_EDGE + (1.0 - BLAST_EDGE) * near)));
            Spells.push(target, center, BLAST_PUSH, BLAST_LIFT);
            target.setRemainingFireTicks(BLAST_BURN_TICKS);
        }
        SpellFx.fireBurst(sl, center, radius);
        Spells.sound(sl, center, SoundEvents.GENERIC_EXPLODE.value(), 1f, 0.9f);
    }

    /** Has this pathway (or the check is skipped) and has reached the Sequence the ability needs. */
    private static boolean ready(Player player, boolean bypassClassCheck, int sequence) {
        if (!bypassClassCheck && !SoulCore.getPathway(player).equals(PATHWAY)) return false;
        return Beyonder.hasSequence(player, sequence);
    }

    /** True if there is a block right under the spot (so what is set there lies on something). */
    private static boolean onSolidGround(ServerLevel sl, Vec3 spot) {
        return !sl.getBlockState(BlockPos.containing(spot.x, spot.y - 0.2, spot.z)).isAir();
    }
}
