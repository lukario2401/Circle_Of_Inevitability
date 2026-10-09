package net.lukario.frogerealm.shadow_slave.soul_abilities.beyonder_characteristics;

import net.lukario.frogerealm.ForgeRealm;
import net.lukario.frogerealm.combat.AnimatedShot;
import net.lukario.frogerealm.combat.HudTimer;
import net.lukario.frogerealm.combat.Later;
import net.lukario.frogerealm.combat.MeleeCombo;
import net.lukario.frogerealm.combat.SpellFx;
import net.lukario.frogerealm.combat.Spells;
import net.lukario.frogerealm.combat.Zone;
import net.lukario.frogerealm.particles.fx.ModelFx;
import net.lukario.frogerealm.particles.fx.ParticleFx;
import net.lukario.frogerealm.particles.fx.ParticleShapes;
import net.lukario.frogerealm.particles.fx.SlashFx;
import net.lukario.frogerealm.root.Root;
import net.lukario.frogerealm.root.RootRestriction;
import net.lukario.frogerealm.shadow_slave.soul_shards.SoulCore;
import net.lukario.frogerealm.status.Blind;
import net.lukario.frogerealm.status.Burial;
import net.lukario.frogerealm.status.Concealment;
import net.lukario.frogerealm.status.DeathWard;
import net.lukario.frogerealm.status.FallGuard;
import net.lukario.frogerealm.status.Flight;
import net.lukario.frogerealm.status.Marks;
import net.lukario.frogerealm.status.Vulnerable;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public class PaleEmperor {

    private static ModelFx.Pose pose() {
        return ModelFx.pose();
    }

    // =====================================================================================
    // CHAINS
    // =====================================================================================

    private static final SlashFx CHAIN_SLASH = SlashFx.line("slash/smooth")
            .color(0xC0F5C211)
            .core(0xC0AADD20)
            .width(0.12f)
            .taper(SlashFx.Taper.UNIFORM);

    /*
     * One chain lasts 120 ticks = 6 seconds.
     */
    public static final int CHAIN_TICKS = 120;

    /**
     * Chain used by the Pale Emperor root ability.
     *
     * 0-5    : appears
     * 5-108  : stays fully extended
     * 108-118: retracts
     * 120    : disappears
     */
    public static final ModelFx CHAIN = ModelFx.of("pale_emperor/underworld_chain")
            .pivot(8, -14, 8)
            .glow()
            .lifetime(CHAIN_TICKS)
            .fade(0, 4)
            .key(0, pose().scale(0.04f))
            .key(5, pose().scale(1f), ModelFx.Ease.OUT)
            .during(108, 118, pose().scale(0.04f), ModelFx.Ease.IN);

    public static final ModelFx CHAIN_STRONG = ModelFx.of("pale_emperor/underworld_chain")
            .pivot(8, -14, 8)
            .glow()
            .lifetime(100)
            .fade(0, 4)
            .key(0, pose().scale(0.04f))
            .key(5, pose().scale(1f), ModelFx.Ease.OUT)
            .during(58, 68, pose().scale(0.04f), ModelFx.Ease.IN);

    /**
     * Creates a chain between two points.
     */
    public static void chain(ServerLevel sl, Vec3 from, Vec3 to) {
        Vec3 reach = to.subtract(from);

        float length = (float) reach.length() / 2.875f;

        float thick = Math.max(
                1.1f,
                Math.min(1.8f, length)
        );

        ParticleShapes.modelAlong(
                sl,
                CHAIN.scale(thick, length, thick),
                from,
                reach
        );
    }

    // =====================================================================================
    // SEAL
    // =====================================================================================

    private static final ModelFx SEAL_APPEARS = ModelFx.of("pale_emperor/death_sigil_ring")
            .glow()
            .unshaded()
            .seeThrough()
            .fade(0, 14)
            .key(0, pose().scale(0.2f).alpha(0f))
            .key(10, pose().scale(1f).alpha(1f), ModelFx.Ease.OUT);

    public static final ModelFx SEAL_RING = SEAL_APPEARS.spin(1.2f);

    public static final ModelFx SEAL_CORE = ModelFx.of("pale_emperor/death_sigil_core")
            .glow()
            .unshaded()
            .seeThrough()
            .fade(0, 14)
            .key(0, pose().scale(0.2f).alpha(0f))
            .key(10, pose().scale(1f).alpha(1f), ModelFx.Ease.OUT)
            .spin(-2.4f);

    /**
     * Places the Pale Emperor seal on the ground.
     */
    public static void seal(
            ServerLevel sl,
            Vec3 ground,
            float yaw,
            float blocksWide,
            int ticks
    ) {
        float scale = blocksWide / 3f;

        Vec3 floor = ground.add(0, 0.06, 0);

        ParticleShapes.model(
                sl,
                SEAL_RING.scale(scale).lifetime(ticks),
                floor,
                yaw,
                0f,
                0f
        );

        ParticleShapes.model(
                sl,
                SEAL_CORE.scale(scale).lifetime(ticks),
                floor.add(0, 0.02, 0),
                yaw,
                0f,
                0f
        );
    }

    // =====================================================================================
    // GATE MODELS
    // =====================================================================================

    /** How long the gate stands, and the tick its doors have sunk away: from then on it pulls. */
    public static final int GATE_TICKS = 500;
    public static final int GATE_OPENS = 100;

    private static final ModelFx GATE = ModelFx.of("pale_emperor/underworld_gate")
            .scale(2f)
            .pivot(8, 0, 8)
            .glow()
            .aura(0xFF0000, 0.1f, 3)
            .lifetime(GATE_TICKS)
            .fade(5, 10);

    private static final ModelFx GATE_VOID = ModelFx.of("pale_emperor/underworld_void")
            .scale(2f)
            .pivot(8, 0, 8)
            .glow()
            .aura(0xFF0000, 0.1f, 3)
            .lifetime(GATE_TICKS)
            .fade(5, 10);

    private static final ModelFx GATE_DOOR_LEFT = ModelFx.of("pale_emperor/underworld_gate_door_left")
            .scale(2f)
            .pivot(8, 0, 8)
            .glow()
            .aura(0xFF0000, 0.1f, 3)
            .lifetime(GATE_TICKS)
            .fade(5, 10);

    private static final ModelFx GATE_DOOR_RIGHT = ModelFx.of("pale_emperor/underworld_gate_door_right")
            .scale(2f)
            .pivot(8, 0, 8)
            .glow()
            .aura(0xFF0000, 0.1f, 3)
            .lifetime(GATE_TICKS)
            .fade(5, 10);

    // =====================================================================================
    // PALE EMPEROR ROOT (ability 1)
    // =====================================================================================
    //
    // Normal cast: the ground around the caster becomes cursed land, and the caster wears the crown
    //   (THE CURSED LAND, below).
    // Sneak cast: the enemy under the crosshair is held by a seal and four chains.

    public static void paleEmperorRoot(
            Player player,
            ServerLevel sl,
            boolean bypassClassCheck
    ) {
        if (!canUseCharacteristic(player, bypassClassCheck)) return;

        if (SoulCore.getAscensionStage(player) < 0) return;

        // =====================================================================
        // NORMAL CAST — THE CURSED LAND
        // =====================================================================

        if (player.isShiftKeyDown()) {
            cursedLand(player, sl);
            return;
        }

        // =====================================================================
        // SHIFT CAST — SEAL + 4 CHAINS
        // =====================================================================

        if (SoulCore.getSoulEssence(player) < 1250) return;

        LivingEntity enemy = Spells.aimEnemy(
                player,
                sl,
                32
        );

        if (enemy == null) return;

        int duration = 100;

        SoulCore.setSoulEssence(
                player,
                SoulCore.getSoulEssence(player) - 1250
        );

        Root.apply(
                enemy,
                duration,
                RootRestriction.EVERYTHING
        );

        tether(
                sl,
                CHAIN_SLASH,
                player,
                enemy,
                duration
        );

        Vec3 ground = enemy.position();

        float yaw = player.getYRot();

        seal(
                sl,
                ground,
                yaw,
                6f,
                CHAIN_TICKS
        );

        Vec3 heart = ground.add(
                0,
                1.5,
                0
        );

        for (int i = 0; i < 4; i++) {

            double angle = Math.toRadians(
                    yaw + 45 + i * 90
            );

            Vec3 from = ground.add(
                    -Math.sin(angle) * 2.6,
                    0,
                    Math.cos(angle) * 2.6
            );

            Vec3 stop = heart.add(
                    from.subtract(heart)
                            .normalize()
                            .scale(0.45)
            );

            chain(
                    sl,
                    from,
                    stop
            );
        }

        chainsBite(sl, enemy, ground, heart, duration);
    }

    /** The tick the chains have reached what they were sent at (see CHAIN: 0 -> 5). */
    private static final int CHAIN_ARRIVES = 5;

    /** What is seen and heard of the chains of the sneak cast: they rattle out, bite, and let go when the root ends. */
    private static void chainsBite(ServerLevel sl, LivingEntity enemy, Vec3 ground, Vec3 heart, int holds) {
        Spells.sound(sl, ground, SoundEvents.CHAIN_PLACE, 1.4f, 0.5f);
        Spells.sound(sl, ground, SoundEvents.WITHER_AMBIENT, 0.5f, 0.7f);

        SpellFx.closingRing(sl, ground, 3.0, 0xD0F5C211, CHAIN_ARRIVES + 1, 0);

        // they bite
        Later.run(sl, CHAIN_ARRIVES, () -> {
            ParticleShapes.burst(sl, GOLD_SPARK, heart, 20, 0.05, 0.3);
            ParticleShapes.burst(sl, PALE_SPARK, heart, 10, 0.03, 0.2);
            Spells.sound(sl, heart, SoundEvents.ANVIL_LAND, 0.5f, 1.6f);
        });

        // it is free again
        Later.run(sl, holds, () -> {
            if (!enemy.isAlive() || enemy.level() != sl) return;

            Vec3 middle = enemy.getBoundingBox().getCenter();

            ParticleShapes.burst(sl, GOLD_SPARK, middle, 12, 0.04, 0.25);
            Spells.sound(sl, middle, SoundEvents.CHAIN_PLACE, 0.9f, 1.3f);
        });
    }

    // =====================================================================================
    // THE CURSED LAND (ability 1, normal cast)
    // =====================================================================================
    //
    // The ground around the caster is cursed for LAND_TICKS, and for as long the caster wears the crown.
    // Every enemy standing on it (mobs and other players) is cursed, and the curse goes one stage deeper
    // every CURSE_DEEPENS ticks it stays, CURSE_STAGES at most:
    //
    //   stage 1   slowed
    //   stage 2   and weakened, and takes 10% more damage
    //   stage 3   slowed more, takes 20% more damage, withers
    //   stage 4   weakened more, takes 30% more damage, blind
    //   stage 5   slowed more still, takes 40% more damage, withers faster,
    //             and the hand of the emperor closes on it once and holds it where it stands
    //
    // Stepping off the land does not wipe the curse: the stage sinks back as slowly as it rose, and what
    // is on them clings for a few seconds more. Casting again moves the land to where the caster stands now.
    //
    // What it looks like: the seal covers the land and headstones stand round its edge. Mist creeps along
    // the edge and rises inside, arms of the dead reach out of the ground, feathers fall. Under each of the
    // cursed turns a mark that grows and reddens with the stage, and an arm rises beside them when it deepens.
    //
    // Built from the kit: combat/Zone (the land, and who has stood on it for how long), Spells.keepEffect,
    // status/Vulnerable, status/Blind, SpellFx.circleUnder (the mark), Spells.groundNear (things on uneven
    // land), PaleEmperorFx.crown (worn on the head: ParticleShapes.modelOnHead), tombstoneFor, underworldArm.

    private static final float LAND_COST = 1250;

    /** How long the land stays cursed: 15 seconds. The crown is worn exactly as long. */
    public static final int LAND_TICKS = 300;

    /** How far the land reaches from its middle, and how far above the ground it still catches someone. */
    public static final double LAND_RADIUS = 8;
    public static final double LAND_HEIGHT = 5;

    /** The land acts on those standing on it once every this many ticks. */
    private static final int LAND_BEAT = 10;

    /** The curse goes a stage deeper every this many ticks on the land (2 seconds), up to this many stages. */
    public static final int CURSE_DEEPENS = 40;
    public static final int CURSE_STAGES = 5;

    /** What the land put on someone lasts this long after its last beat on them: up to 3 seconds after they got off it. */
    private static final int CURSE_CLINGS = 60;

    /** How much more damage each stage after the first makes them take: 10%, 20%, 30%, 40%. */
    public static final float CURSE_WEAKENS = 0.10f;

    /** At the deepest stage the hand holds them where they stand for this long, once per land. 0 = no hand. */
    public static final int LAND_HAND_HOLDS = 50;

    /** How many headstones stand round the edge of the land. 0 = none. */
    public static final int LAND_GRAVES = 8;

    /** An arm of the dead reaches out of the land somewhere once every this many ticks. 0 = never. */
    public static final int LAND_ARM_EVERY = 30;

    /** The color of the mark under the cursed at each stage, from pale green to red (0xAARRGGBB). */
    private static final int[] CURSE_COLORS = {0x909CFFD2, 0xA0C8F08C, 0xB0F5C211, 0xC0F08A2A, 0xD0E03A3A};

    /** The name of the caster's land, of the crown on their head, and of the mark under the cursed. */
    private static final String LAND = "pale_emperor_land";
    private static final String CROWN_TAG = "pale_emperor_crown";
    private static final String CURSE_MARK = "pale_emperor_curse";

    private static final ParticleFx LAND_MIST = ParticleFx.of("fx/smoke")
            .color(0x90183A2C).endColor(0x00081410)
            .size(0.3f).endSize(0.8f).sizeRandom(0.3f)
            .lifetime(30, 14).gravity(-0.02f).friction(0.92f)
            .spin(2f).randomRotation();

    private static final ParticleFx GOLD_SPARK = ParticleFx.of("fx/glow")
            .color(0xFFFFE9A0).endColor(0x00C08A10)
            .size(0.1f).endSize(0.03f).sizeRandom(0.4f)
            .lifetime(18, 10).gravity(-0.015f).friction(0.9f)
            .glow();

    /** The tick the crown, coming down, has settled on the head (see PaleEmperorFx.CROWN). */
    private static final int CROWN_SETTLES = 26;

    /** The tick the fingers of the hand below are shut. */
    private static final int HAND_SHUT = 32;

    /** The hand that closes on whoever reaches the deepest stage. */
    private static final ModelFx EMPEROR_HAND = ModelFx.of("pale_emperor/emperor_hand")
            .frames(5)
            .smooth()
            .scale(1.5f)
            .pivot(7, -6, 7)
            .glow()
            .fade(3, 8)
            .key(0, pose().frame(3))
            .during(4, 14, pose().frame(0), ModelFx.Ease.OUT)
            .during(26, HAND_SHUT, pose().frame(4), ModelFx.Ease.IN);

    /**
     * One cursed land, from its cast to its end: where it lies, and what it remembers of those on it.
     * tag: the name of everything it put on the ground (seal, headstones, arms), so it can all go at once.
     */
    private record CursedLand(
            Player caster,
            ServerLevel sl,
            Vec3 ground,
            String tag,
            Set<UUID> told,                 // those who were told they stand on it (once each)
            Set<UUID> held,                 // those the hand has taken (once each)
            Map<UUID, Integer> marked       // the stage whose mark each of them carries right now
    ) {}

    /** True while the land this player cursed is still there. */
    public static boolean hasCursedLand(Player player) {
        return Zone.isOpen(player, LAND);
    }

    /** The stage of the curse on someone who has stood on the land for this long: 1 to CURSE_STAGES. */
    public static int curseStage(int ticksOnLand) {
        return Math.min(CURSE_STAGES, 1 + ticksOnLand / CURSE_DEEPENS);
    }

    private static void cursedLand(Player player, ServerLevel sl) {
        // the land lies on the ground: under a caster in the air it is the ground beneath them
        Vec3 feet = player.position();
        Vec3 ground = Spells.groundAt(player, sl, feet);

        if (!player.onGround() && feet.y - ground.y < 0.05) {
            player.sendSystemMessage(Component.literal("There is no ground beneath you to curse."));
            return;                                                  // nothing spent
        }

        if (!Spells.payEssence(player, LAND_COST)) return;

        float yaw = player.getYRot();

        // what lies on the ground has its own name for every caster, so a land that ends early can take it away
        CursedLand land = new CursedLand(
                player,
                sl,
                ground,
                LAND + "_" + player.getUUID(),
                new HashSet<>(),
                new HashSet<>(),
                new HashMap<>()
        );

        Zone.at(ground, LAND_RADIUS)
                .height(LAND_HEIGHT)
                .lasts(LAND_TICKS)
                .every(LAND_BEAT)
                .countsTo(CURSE_DEEPENS * CURSE_STAGES)              // past the deepest stage there is nothing to count
                .onEnter(enemy -> landTakes(land, enemy))
                .onInside((enemy, ticksOnLand) -> landCurses(land, enemy, ticksOnLand))
                .onLeave(enemy -> landLetsGo(land, enemy))
                .onPulse(ticksLeft -> landBeats(land, ticksLeft))
                .onEnd(() -> landCloses(land))
                .open(player, sl, LAND);                             // a land this caster already had ends here

        // only now what shows it: opening the land took away what the one before it had put down
        float wide = (float) (LAND_RADIUS * 2);
        float scale = wide / 3f;
        Vec3 floor = ground.add(0, 0.06, 0);

        ParticleShapes.model(sl, SEAL_RING.scale(scale).lifetime(LAND_TICKS).tag(land.tag()), floor, yaw, 0f, 0f);
        ParticleShapes.model(sl, SEAL_CORE.scale(scale).lifetime(LAND_TICKS).tag(land.tag()), floor.add(0, 0.02, 0), yaw, 0f, 0f);

        landGraves(land, yaw);

        PaleEmperorFx.crown(sl, player, LAND_TICKS, CROWN_TAG);

        SpellFx.shockRing(sl, ground, LAND_RADIUS, 0xE09CFFD2, 12, 0);
        SpellFx.shockRing(sl, ground.add(0, 0.03, 0), LAND_RADIUS, 0x90F5C211, 16, 4);
        PaleEmperorFx.featherFall(sl, ground, LAND_RADIUS * 0.6, 12);

        Spells.sound(sl, ground, SoundEvents.WITHER_SPAWN, 0.6f, 0.6f);
        Spells.sound(sl, ground, SoundEvents.BELL_BLOCK, 1.2f, 0.5f);

        // the moment the crown has settled on their head
        Later.run(sl, CROWN_SETTLES, () -> {
            if (!Spells.casterStillHere(player, sl) || !hasCursedLand(player) || Concealment.isHidden(player)) return;

            Vec3 brow = player.position().add(0, player.getBbHeight() + 0.15, 0);

            ParticleShapes.burst(sl, GOLD_SPARK, brow, 16, 0.03, 0.18);
            Spells.sound(sl, brow, SoundEvents.AMETHYST_BLOCK_CHIME, 1.0f, 0.7f);
        });
    }

    /** Headstones rise round the edge of the land one after another, each with its grave toward the middle. */
    private static void landGraves(CursedLand land, float yaw) {
        for (int i = 0; i < LAND_GRAVES; i++) {
            float around = yaw + (i + 0.5f) * 360f / LAND_GRAVES;

            // on the ground where it stands; none where the edge runs over a cliff or a pit
            Vec3 stands = Spells.groundNear(
                    land.caster(),
                    land.sl(),
                    Spells.spotAround(land.ground(), around, LAND_RADIUS),
                    3.0
            );

            if (stands == null) continue;

            int wait = i * 3;

            ParticleShapes.model(
                    land.sl(),
                    PaleEmperorFx.tombstoneFor(LAND_TICKS - wait).delay(wait).tag(land.tag()),
                    stands,
                    around + 180f,                                   // its front, and so its grave, looks at the middle
                    0f,
                    0f
            );
        }
    }

    /** One beat of the land itself: mist along its edge and inside it, and now and then a ring, an arm, feathers. */
    private static void landBeats(CursedLand land, int ticksLeft) {
        ServerLevel sl = land.sl();
        Vec3 ground = land.ground();

        ParticleShapes.ring(sl, LAND_MIST, ground.add(0, 0.2, 0), LAND_RADIUS, 28, 0);

        for (int i = 0; i < 6; i++) {
            ParticleShapes.spawn(sl, LAND_MIST, somewhereOn(land).add(0, 0.2, 0), new Vec3(0, 0.03, 0));
        }

        // the crown glints
        Player caster = land.caster();

        if (!Concealment.isHidden(caster)) {
            ParticleShapes.burst(sl, GOLD_SPARK, caster.position().add(0, caster.getBbHeight() + 0.3, 0), 2, 0.01, 0.05);
        }

        // every two seconds a ring runs out to the edge
        if (ticksLeft % 40 == 0 && ticksLeft > 20) {
            SpellFx.steadyRing(sl, ground, LAND_RADIUS, 0x709CFFD2, 20, 0);
            Spells.sound(sl, ground, SoundEvents.BEACON_POWER_SELECT, 0.5f, 0.5f);
        }

        // an arm of the dead reaches out of the land somewhere (not in its last seconds: it would be cut off)
        if (LAND_ARM_EVERY > 0 && ticksLeft % LAND_ARM_EVERY == 0 && ticksLeft >= PaleEmperorFx.ARMS_TICKS) {
            Vec3 rises = Spells.groundNear(caster, sl, somewhereOn(land), 3.0);

            if (rises != null) {
                PaleEmperorFx.underworldArm(sl, rises, sl.getRandom().nextFloat() * 360f, land.tag());
            }
        }

        // feathers drift down
        if (ticksLeft % 60 == 0 && ticksLeft >= PaleEmperorFx.FEATHERS_TICKS) {
            PaleEmperorFx.featherFall(sl, ground, LAND_RADIUS * 0.8, 3);
        }
    }

    /** A spot on the land picked at random (at the height of its middle). */
    private static Vec3 somewhereOn(CursedLand land) {
        float around = land.sl().getRandom().nextFloat() * 360f;
        double out = Math.sqrt(land.sl().getRandom().nextDouble()) * LAND_RADIUS * 0.9;      // spread evenly, not bunched in the middle
        return Spells.spotAround(land.ground(), around, out);
    }

    /** Someone set foot on the land. */
    private static void landTakes(CursedLand land, LivingEntity enemy) {
        // (in chat: the line above the hotbar is rewritten every tick by the soul essence display)
        if (enemy instanceof Player victim && land.told().add(victim.getUUID())) {
            victim.sendSystemMessage(Component.literal("You stand on cursed land. The longer you stay, the deeper its curse."));
        }

        Vec3 feet = enemy.position();

        SpellFx.closingRing(land.sl(), feet, enemy.getBbWidth() * 0.5 + 1.1, 0xB09CFFD2, 8, 0);
        ParticleShapes.burst(land.sl(), LAND_MIST, feet.add(0, 0.2, 0), 6, 0.02, 0.08);
    }

    /** Someone got off the land (or it ended under them): the mark under them goes. What is on them clings a while. */
    private static void landLetsGo(CursedLand land, LivingEntity enemy) {
        land.marked().remove(enemy.getUUID());
        ParticleShapes.clearModels(land.sl(), enemy, CURSE_MARK);
    }

    /** One beat of the land on one of those standing on it: the curse of its stage is laid on it, or kept up. */
    private static void landCurses(CursedLand land, LivingEntity enemy, int ticksOnLand) {
        ServerLevel sl = land.sl();
        Player player = land.caster();

        int stage = curseStage(ticksOnLand);

        Spells.keepEffect(enemy, MobEffects.MOVEMENT_SLOWDOWN, (stage - 1) / 2, CURSE_CLINGS);

        if (stage >= 2) {
            Spells.keepEffect(enemy, MobEffects.WEAKNESS, (stage - 2) / 2, CURSE_CLINGS);

            // (the same number twice = it is set, not added again at every beat)
            float more = CURSE_WEAKENS * (stage - 1);
            Vulnerable.add(enemy, more, more, CURSE_CLINGS);
        }

        if (stage >= 3) {
            Spells.keepEffect(enemy, MobEffects.WITHER, stage >= CURSE_STAGES ? 1 : 0, CURSE_CLINGS);
        }

        // (Blind also makes a mob forget what it hunts, so only when it is about to see again, not at every beat)
        if (stage >= 4 && Blind.ticksLeft(enemy) < CURSE_CLINGS / 2) {
            Blind.apply(enemy, CURSE_CLINGS);
        }

        Vec3 feet = enemy.position();

        ParticleShapes.burst(sl, LAND_MIST, feet.add(0, 0.2, 0), stage, 0.01, 0.06);

        // the mark under it shows how deep the curse is: wider and redder with every stage.
        // (It is put down again whenever the stage is not the one it shows: on stepping in, and at every change)
        Integer shows = land.marked().put(enemy.getUUID(), stage);

        if (shows == null || shows != stage) {
            SpellFx.circleUnder(
                    sl,
                    enemy,
                    enemy.getBbWidth() * 0.5 + 0.45 + stage * 0.12,
                    CURSE_COLORS[Math.min(stage, CURSE_COLORS.length) - 1],
                    Zone.ticksLeft(player, LAND),
                    CURSE_MARK
            );
        }

        // the beat at which it reached this stage
        boolean deepened = ticksOnLand > 0 && stage > curseStage(ticksOnLand - LAND_BEAT);

        if (!deepened) return;

        ParticleShapes.burst(sl, PALE_SPARK, enemy.getBoundingBox().getCenter(), 6 + stage * 3, 0.04, 0.2);
        Spells.sound(sl, feet, SoundEvents.WITHER_HURT, 0.35f, 0.5f + stage * 0.08f);

        // an arm of the dead comes up beside it and clutches at it
        Vec3 under = Spells.groundUnder(player, sl, enemy);
        float side = sl.getRandom().nextFloat() * 360f;

        PaleEmperorFx.underworldArm(
                sl,
                Spells.spotAround(under, side, enemy.getBbWidth() * 0.5 + 0.9),
                side + 180f,                                         // its palm looks back at the one it reaches for
                land.tag()
        );

        // the deepest stage: the hand of the emperor closes on it, once
        if (stage >= CURSE_STAGES && LAND_HAND_HOLDS > 0 && land.held().add(enemy.getUUID())) {
            Root.apply(enemy, LAND_HAND_HOLDS, RootRestriction.MOVEMENT);

            ParticleShapes.model(
                    sl,
                    EMPEROR_HAND.lifetime(LAND_HAND_HOLDS),
                    under.add(0, -1.5, 0),
                    player.getYRot(),
                    0f,
                    0f
            );

            Spells.sound(sl, feet, SoundEvents.CHAIN_PLACE, 1.2f, 0.5f);

            if (enemy instanceof Player victim) {
                victim.sendSystemMessage(Component.literal("The hand of the Pale Emperor closes on you."));
            }

            // the moment its fingers shut
            Later.run(sl, HAND_SHUT - 2, () -> {
                if (!enemy.isAlive() || enemy.level() != sl) return;

                Vec3 middle = enemy.getBoundingBox().getCenter();

                ParticleShapes.burst(sl, GOLD_SPARK, middle, 18, 0.05, 0.3);
                SpellFx.shockRing(sl, enemy.position(), 2.0, 0xE0E03A3A, 8, 0);
                Spells.sound(sl, middle, SoundEvents.PLAYER_ATTACK_CRIT, 0.9f, 0.5f);
            });
        }
    }

    /** The land is over: what it put down is taken away, and it closes on its middle. */
    private static void landCloses(CursedLand land) {
        ServerLevel sl = land.sl();
        Vec3 ground = land.ground();

        // (when its time simply ran out these have sunk and faded away already)
        ParticleShapes.clearModels(sl, ground, land.tag());
        PaleEmperorFx.takeCrown(sl, land.caster(), CROWN_TAG);

        SpellFx.closingRing(sl, ground, LAND_RADIUS, 0xC09CFFD2, 14, 0);
        ParticleShapes.ring(sl, LAND_MIST, ground.add(0, 0.2, 0), LAND_RADIUS, 28, -0.25);     // the mist is drawn in
        Spells.sound(sl, ground, SoundEvents.BELL_BLOCK, 0.9f, 0.4f);
    }

    // =====================================================================================
    // PALE EMPEROR DOOR (ability 2)
    // =====================================================================================
    //
    // Normal cast: the gate of the underworld opens above the caster and drags everything near into it.
    // Sneak cast: for 30 seconds death cannot keep the caster (THE SECOND LIFE, below).

    public static void paleEmperorDoor(
            Player player,
            ServerLevel sl,
            boolean bypassClassCheck
    ) {
        if (!canUseCharacteristic(player, bypassClassCheck)) return;

        if (SoulCore.getAscensionStage(player) < 1) return;

        // =====================================================================
        // SHIFT CAST — THE SECOND LIFE
        // =====================================================================

        if (player.isShiftKeyDown()) {
            secondLife(player, sl);
            return;
        }

        // =====================================================================
        // NORMAL CAST — THE GATE
        // =====================================================================

        if (SoulCore.getSoulEssence(player) < 11250) return;

        SoulCore.setSoulEssence(
                player,
                SoulCore.getSoulEssence(player) - 11250
        );

        Vec3 position = player.position().add(
                0,
                2,
                0
        );

        ModelFx gate = GATE;

        ModelFx gateVoid = GATE_VOID;

        ModelFx gateDoorLeft = GATE_DOOR_LEFT.key(
                GATE_OPENS,
                pose().up(-6),
                ModelFx.Ease.IN
        );

        ModelFx gateDoorRight = GATE_DOOR_RIGHT.key(
                GATE_OPENS,
                pose().up(-6),
                ModelFx.Ease.IN
        );

        for (int i = GATE_OPENS; i <= GATE_TICKS; i += 10) {

            Later.run(
                    sl,
                    i,
                    () -> paleEmpGatePull(
                            sl,
                            player,
                            position
                    )
            );
        }

        ParticleShapes.model(
                sl,
                gate,
                position,
                player.getYRot(),
                0f,
                0f
        );

        ParticleShapes.model(
                sl,
                gateVoid,
                position,
                player.getYRot(),
                0f,
                0f
        );

        ParticleShapes.model(
                sl,
                gateDoorLeft,
                position,
                player.getYRot(),
                0f,
                0f
        );

        ParticleShapes.model(
                sl,
                gateDoorRight,
                position,
                player.getYRot(),
                0f,
                0f
        );

        gateShows(player, sl, position);
    }

    // =====================================================================================
    // GATE PULL
    // =====================================================================================

    private static void paleEmpGatePull(
            ServerLevel sl,
            Player player,
            Vec3 position
    ) {
        List<LivingEntity> entities = sl.getEntitiesOfClass(
                LivingEntity.class,
                new AABB(position, position).inflate(12),
                entity -> entity != player
        );

        for (LivingEntity entity : entities) {
            Spells.pull(
                    entity,
                    position,
                    1,
                    0.1
            );
        }

        List<LivingEntity> enemies = sl.getEntitiesOfClass(
                LivingEntity.class,
                new AABB(position, position).inflate(2),
                entity -> entity != player
        );

        for (LivingEntity enemy : enemies) {
            enemy.hurt(
                    sl.damageSources().playerAttack(player),
                    4
            );
        }

        gateDrags(sl, player, position, entities, enemies);
    }

    // =====================================================================================
    // THE GATE: WHAT IS SEEN AND HEARD OF IT
    // =====================================================================================
    //
    // Nothing here pulls or hurts: that is paleEmpGatePull above. A seal lies under the gate for as long as
    // it stands. While its doors sink away the ground shakes; when they are gone it opens with a roar and
    // the dead reach up under it; while it pulls, mist and embers are sucked into it and chains run from it
    // to those it drags.

    /** How many of those it drags have a chain drawn to them at one time. */
    private static final int GATE_CHAINS = 10;

    /** The gate's pivot is its threshold: this far above it is the middle of its opening. */
    private static final double GATE_MOUTH = 2.5;

    private static final ParticleFx GATE_EMBER = ParticleFx.of("fx/glow")
            .color(0xFFFF6A4A).endColor(0x00801010)
            .size(0.12f).endSize(0.03f).sizeRandom(0.4f)
            .lifetime(18, 10).friction(0.92f)
            .glow();

    /** From the moment the gate appears to the moment it is gone. 'gate' is where it stands (its threshold). */
    private static void gateShows(Player player, ServerLevel sl, Vec3 gate) {
        float yaw = player.getYRot();
        Vec3 below = Spells.groundAt(player, sl, player.position());
        Vec3 mouth = gate.add(0, GATE_MOUTH, 0);

        // it appears
        seal(sl, below, yaw, 10f, GATE_TICKS);
        SpellFx.shockRing(sl, below, 6.0, 0xE0D02A2A, 12, 0);

        Spells.sound(sl, gate, SoundEvents.WITHER_SPAWN, 0.9f, 0.5f);
        Spells.sound(sl, gate, SoundEvents.BELL_BLOCK, 1.5f, 0.4f);

        // its doors sink away: the ground shakes
        for (int tick = 10; tick < GATE_OPENS; tick += 20) {
            Later.run(sl, tick, () -> {
                ParticleShapes.ring(sl, LAND_MIST, below.add(0, 0.2, 0), 3.0, 14, 0.06);
                Spells.sound(sl, gate, SoundEvents.CHAIN_PLACE, 1.0f, 0.5f);
            });
        }

        // they are gone: it opens
        Later.run(sl, GATE_OPENS, () -> {
            SpellFx.shockRing(sl, below, 12.0, 0xE0D02A2A, 14, 0);
            SpellFx.shockRing(sl, mouth, 6.0, 0xC09CFFD2, 10, 2);
            ParticleShapes.burst(sl, GATE_EMBER, mouth, 40, 0.1, 0.5);

            PaleEmperorFx.underworldArms(sl, below, yaw, 6);

            Spells.sound(sl, gate, SoundEvents.ENDER_DRAGON_GROWL, 1.2f, 0.5f);
            Spells.sound(sl, gate, SoundEvents.WITHER_AMBIENT, 1.0f, 0.5f);
        });

        // it is gone
        Later.run(sl, GATE_TICKS, () -> {
            SpellFx.closingRing(sl, below, 12.0, 0xC0D02A2A, 14, 0);
            ParticleShapes.burst(sl, GATE_EMBER, mouth, 30, 0.05, 0.35);
            Spells.sound(sl, gate, SoundEvents.BELL_BLOCK, 1.2f, 0.4f);
        });
    }

    /** One pull of the gate: what it drags in is 'dragged', what it hurt this time is 'hurt'. */
    private static void gateDrags(
            ServerLevel sl,
            Player player,
            Vec3 gate,
            List<LivingEntity> dragged,
            List<LivingEntity> hurt
    ) {
        Vec3 mouth = gate.add(0, GATE_MOUTH, 0);
        Vec3 below = Spells.groundAt(player, sl, gate);

        // mist along the ground and embers in the air, both sucked in
        ParticleShapes.ring(sl, LAND_MIST, below.add(0, 0.3, 0), 9.0, 18, -0.45);
        ParticleShapes.ring(sl, GATE_EMBER, mouth, 5.0, 10, -0.35);

        // a chain from the gate to each of those it drags (a pull comes every 10 ticks: drawn until the next)
        int chains = 0;

        for (LivingEntity entity : dragged) {
            if (chains++ >= GATE_CHAINS) break;

            SpellFx.tether(sl, CHAIN_SLASH, () -> mouth, () -> SpellFx.middleOf(entity, sl), 10);
        }

        for (LivingEntity entity : hurt) {
            ParticleShapes.burst(sl, GATE_EMBER, entity.getBoundingBox().getCenter(), 6, 0.05, 0.25);
        }

        // every 2 seconds it groans, every 4 the dead reach up under it
        long time = sl.getGameTime();

        if (time % 40 < 10) {
            Spells.sound(sl, gate, SoundEvents.WITHER_AMBIENT, 0.6f, 0.5f);
        }

        if (time % 80 < 10) {
            PaleEmperorFx.underworldArms(sl, below, sl.getRandom().nextFloat() * 360f, 5);
        }
    }

    // =====================================================================================
    // THE SECOND LIFE (ability 2, sneak cast)
    // =====================================================================================
    //
    // For REVIVE_TICKS the caster cannot be killed for good: if they die in that time they rise again where
    // they fell, with part of their health, free of what was on them, and whoever stood over them is thrown
    // back. It works once. A small clock next to the hotbar shows how long it still waits.
    //
    // What it looks like: ribs close round the caster and fade into them, and while death waits a pale mark
    // turns under their feet (everyone can see it). When they rise, the arms of the dead reach for them and
    // miss, rings climb round them, wings open on their back for a moment, and they shine for as long as
    // nothing can hurt them.
    //
    // Built from the kit: status/DeathWard (dying is undone), combat/HudTimer (the clock by the hotbar),
    // SpellFx.circleUnder (the mark), SpellFx.risingRings, PaleEmperorFx.ribsAround, underworldArms, wings.

    private static final float REVIVE_COST = 11250;

    /** How long death waits: 30 seconds. */
    public static final int REVIVE_TICKS = 600;

    /** The health they rise with, as a part of their maximum. */
    public static final float REVIVE_HEALTH = 0.5f;

    /** Enemies this near are thrown back when the caster rises. */
    public static final double REVIVE_THROWS = 5;

    /** How long the wings stay open on the one who rose. They are only for show. 0 = no wings. */
    public static final int REVIVE_WINGS_TICKS = 84;

    /** Set while death waits for the player. Saved with the player (the ward itself is not: it is gone after a logout). */
    private static final String REVIVE_ON = "pale_emperor_revive_on";

    /** The name of the clock by the hotbar, of the mark under the caster, and of the wings they rise with. */
    private static final String REVIVE_TIMER = "pale_emperor_revive";
    private static final String REVIVE_MARK = "pale_emperor_revive";
    private static final String REVIVE_WINGS = "pale_emperor_revive_wings";

    private static final int REVIVE_COLOR = 0x9CFFD2;

    private static void secondLife(Player player, ServerLevel sl) {
        if (DeathWard.isArmed(player)) {
            int seconds = (DeathWard.ticksLeft(player) + 19) / 20;
            player.sendSystemMessage(Component.literal("Death already waits for you: " + seconds + " seconds more."));
            return;                                                  // nothing spent
        }

        if (!Spells.payEssence(player, REVIVE_COST)) return;

        DeathWard.arm(
                player,
                REVIVE_TICKS,
                REVIVE_HEALTH,
                (saved, source, level) -> risesAgain(saved, level)
        );

        player.getPersistentData().putBoolean(REVIVE_ON, true);

        secondLifeShows(player, sl, REVIVE_TICKS);

        Vec3 feet = player.position();

        // ribs close round them and fade into them
        PaleEmperorFx.ribsAround(sl, player);

        seal(sl, feet, player.getYRot(), 4f, 40);
        SpellFx.closingRing(sl, feet, 3.0, 0xE0F5C211, 12, 0);
        PaleEmperorFx.featherFall(sl, feet, 1.2, 6);

        Spells.sound(sl, feet, SoundEvents.BEACON_ACTIVATE, 0.8f, 0.6f);
        Spells.sound(sl, feet, SoundEvents.ENCHANTMENT_TABLE_USE, 1.0f, 0.6f);

        // the moment the ribs snap shut
        Later.run(sl, PaleEmperorFx.RIBS_AROUND_SHUT, () -> {
            if (!Spells.casterStillHere(player, sl)) return;

            Vec3 middle = player.getBoundingBox().getCenter();

            ParticleShapes.burst(sl, PALE_SPARK, middle, 16, 0.04, 0.22);
            Spells.sound(sl, middle, SoundEvents.CHAIN_PLACE, 1.0f, 0.5f);
        });

        player.sendSystemMessage(Component.literal("For " + REVIVE_TICKS / 20 + " seconds death cannot keep you."));
    }

    /** What shows that death waits for 'ticks' more: the clock by the hotbar and the mark under the caster's feet. */
    private static void secondLifeShows(Player player, ServerLevel sl, int ticks) {
        HudTimer.show(player, REVIVE_TIMER, "Revive", ticks, REVIVE_COLOR);
        SpellFx.circleUnder(sl, player, 0.85, 0x70000000 | REVIVE_COLOR, ticks, REVIVE_MARK);
    }

    /** One tick while death waits: notices when it stopped waiting without having been needed. */
    private static void secondLifeTick(Player player, ServerLevel sl) {
        if (!player.getPersistentData().getBoolean(REVIVE_ON)) return;
        if (DeathWard.isArmed(player)) return;

        // its time ran out, or the player logged out meanwhile (rising again clears the flag itself)
        player.getPersistentData().remove(REVIVE_ON);

        HudTimer.hide(player, REVIVE_TIMER);
        ParticleShapes.clearModels(sl, player, REVIVE_MARK);

        Spells.sound(sl, player.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 0.8f, 0.5f);

        player.sendSystemMessage(Component.literal("Death no longer waits for you."));
    }

    /** The caster died while death waited: they stand again. (Their health is given back by the ward itself.) */
    private static void risesAgain(LivingEntity saved, ServerLevel sl) {
        Vec3 feet = saved.position();
        Vec3 middle = saved.getBoundingBox().getCenter();
        float yaw = saved.getYRot();

        Spells.cleanse(saved);                                       // what was killing them is gone too

        // they shine for as long as nothing can hurt them
        if (DeathWard.GRACE_TICKS > 0) {
            saved.addEffect(new MobEffectInstance(MobEffects.GLOWING, DeathWard.GRACE_TICKS, 0, false, false));
        }

        saved.getPersistentData().remove(REVIVE_ON);
        ParticleShapes.clearModels(sl, saved, REVIVE_MARK);

        // the arms of the dead come up round the spot and close on nothing
        Vec3 ground = feet;

        if (saved instanceof Player player) {
            HudTimer.hide(player, REVIVE_TIMER);

            for (LivingEntity enemy : Spells.enemiesAround(player, sl, middle, REVIVE_THROWS)) {
                Spells.push(enemy, feet, 1.1, 0.35);                 // room to breathe
            }

            ground = Spells.groundUnder(player, sl, player);

            // wings open on their back for a moment (not over the wings of ability 6)
            if (REVIVE_WINGS_TICKS > 0 && !hasWings(player)) {
                PaleEmperorFx.wings(sl, player, REVIVE_WINGS_TICKS, REVIVE_WINGS);
            }

            player.sendSystemMessage(Component.literal("You rise again."));
        }

        PaleEmperorFx.underworldArms(sl, ground, yaw, 6);

        seal(sl, ground, yaw, 6f, 50);
        SpellFx.blast(sl, middle, 3.0, 0xE09CFFD2, PALE_SPARK);
        SpellFx.shockRing(sl, ground, REVIVE_THROWS, 0xE0F5C211, 12, 0);
        SpellFx.risingRings(sl, feet, 1.6, saved.getBbHeight() + 0.6, 4, 0xD09CFFD2);
        ParticleShapes.burst(sl, GOLD_SPARK, middle, 24, 0.05, 0.3);
        PaleEmperorFx.featherFall(sl, feet, 2.0, 14);

        Spells.sound(sl, feet, SoundEvents.WITHER_SPAWN, 0.7f, 1.4f);
        Spells.sound(sl, feet, SoundEvents.ZOMBIE_VILLAGER_CURE, 0.8f, 1.2f);
        Spells.sound(sl, feet, SoundEvents.ENDER_DRAGON_FLAP, 1.0f, 0.7f);
    }

    // =====================================================================================
    // MELEE COMBO "TOLL OF THE PALE EMPEROR" (hitting mobs as the Pale Emperor)
    // =====================================================================================
    //
    // Registered in combat/MeleeCombos. Only how a hit looks and sounds: the damage is the weapon's own.
    //   hit 1  Pale claw      three claw marks rake across it
    //   hit 2  Gilded arc     a wide pale-and-gold crescent sweeps in front, shedding sparks
    //   hit 3  Grave hand     an arm of the dead breaks out beside it, and a rising cut of bone-light goes up through it
    //   hit 4  one of:
    //          Verdict        a tall golden cleave comes down on it, a seal flashes under it
    //          Wingbeat       a ring of light spins round the caster and feathers scatter
    // Then it starts over. Two seconds without a hit also starts it over.

    private static final int TOLL_PALE = 0xF09CFFD2;      // pale green body
    private static final int TOLL_TAIL = 0x809CFFD2;      // dimmer tail
    private static final int TOLL_GOLD = 0xF0F5C211;      // gold
    private static final int TOLL_WHITE = 0xFFF4FFE8;     // bright front / core
    private static final int TOLL_LEAD = 0xC09CFFD2;      // dotted lead-in lines

    // hit 1: three claw marks
    private static final SlashFx TOLL_CLAW = SlashFx.line("slash/smooth")
            .color(TOLL_PALE).core(TOLL_WHITE)
            .width(0.12f).taper(SlashFx.Taper.CRESCENT)
            .lifetime(16).sweep(2);

    // hit 2: a wide crescent in front
    private static final SlashFx TOLL_ARC_LEAD = SlashFx.arc("slash/dashed_fine")
            .color(TOLL_LEAD).radius(2.7f).arc(220f).width(0.11f).taper(SlashFx.Taper.UNIFORM)
            .lifetime(6).sweep(3);
    private static final SlashFx TOLL_ARC = SlashFx.arc("slash/edge")
            .color(TOLL_PALE).tailColor(TOLL_GOLD).headColor(TOLL_WHITE)
            .radius(2.55f).arc(200f).width(0.8f).taper(SlashFx.Taper.CRESCENT)
            .layers(2).spread(0.18f)
            .lifetime(10).sweep(3).delay(2);

    // hit 3: a cut of bone-light rising through it
    private static final SlashFx TOLL_RISE = SlashFx.line("slash/shatter")
            .color(TOLL_PALE).tailColor(TOLL_TAIL).headColor(TOLL_WHITE)
            .width(0.7f).taper(SlashFx.Taper.COMET)
            .layers(2).spread(0.2f)
            .lifetime(14).sweep(3).delay(3);
    private static final SlashFx TOLL_RISE_CORE = SlashFx.line("slash/smooth")
            .color(0xE0F4FFE8).core(0xFFFFFFFF)
            .width(0.12f).taper(SlashFx.Taper.UNIFORM)
            .lifetime(12).sweep(3).delay(3);

    // hit 4a: a tall golden cleave coming down in front (the same stance as the Prince's cleave)
    private static final SlashFx TOLL_CLEAVE = SlashFx.arc("slash/edge")
            .color(TOLL_GOLD).tailColor(TOLL_TAIL).headColor(TOLL_WHITE)
            .radius(2.45f).arc(130f).width(0.85f).taper(SlashFx.Taper.CRESCENT)
            .layers(2).spread(0.18f)
            .lifetime(10).sweep(2).delay(2)
            .rotation(0f, -30f, -72f);
    private static final SlashFx TOLL_CLEAVE_LEAD = SlashFx.arc("slash/smooth")
            .color(0xE0FFF0B0).core(0xFFFFFFFF)
            .radius(2.55f).arc(135f).width(0.12f).taper(SlashFx.Taper.CRESCENT)
            .lifetime(6).sweep(2)
            .rotation(0f, -30f, -72f);

    // hit 4b: a spinning ring round the caster
    private static final SlashFx TOLL_RING = SlashFx.arc("slash/edge")
            .color(TOLL_PALE).tailColor(TOLL_TAIL).headColor(TOLL_WHITE)
            .radius(2.5f).arc(300f).width(1.0f).taper(SlashFx.Taper.COMET)
            .layers(2).spread(0.22f)
            .lifetime(11).sweep(3).delay(2).spin(14f);
    private static final SlashFx TOLL_RING_LEAD = SlashFx.arc("slash/dashed_fine")
            .color(TOLL_LEAD).radius(2.7f).arc(360f).width(0.11f).taper(SlashFx.Taper.UNIFORM)
            .lifetime(6).sweep(3);

    // short rays of light round whatever is hit
    private static final SlashFx TOLL_HIT_RAY = SlashFx.line("slash/smooth")
            .color(TOLL_PALE).tailColor(TOLL_GOLD).headColor(TOLL_WHITE)
            .width(0.09f).taper(SlashFx.Taper.CRESCENT)
            .lifetime(7).sweep(2);

    public static final MeleeCombo MELEE_COMBO = MeleeCombo.forAspect("Pale Emperor")
            .step(PaleEmperor::tollClaw)
            .step(PaleEmperor::tollArc)
            .step(PaleEmperor::tollGraveHand)
            .randomStep(PaleEmperor::tollVerdict, PaleEmperor::tollWingbeat)
            .resetAfter(40)
            .minCharge(0.8f);

    // hit 1: three parallel claw marks rake across it, at a slant that changes every time
    private static void tollClaw(ServerPlayer player, LivingEntity target, ServerLevel sl) {
        RandomSource random = player.getRandom();
        Vec3 hit = target.getBoundingBox().getCenter();
        Vec3 look = Spells.flatLook(player);
        Vec3 side = look.cross(Spells.UP).normalize();

        // the direction of the marks (rising one way or the other), and the way they lie next to each other
        double angle = Math.toRadians((random.nextBoolean() ? 1 : -1) * (35 + random.nextFloat() * 20));
        Vec3 along = side.scale(Math.cos(angle)).add(Spells.UP.scale(Math.sin(angle))).normalize();
        Vec3 apart = side.scale(-Math.sin(angle)).add(Spells.UP.scale(Math.cos(angle))).normalize();
        double half = target.getBbHeight() * 0.45 + 0.6;

        for (int i = -1; i <= 1; i++) {
            Vec3 middle = hit.add(apart.scale(i * 0.28)).subtract(look.scale(0.2));
            double length = half * (i == 0 ? 1.0 : 0.8);         // the middle claw is the longest
            ParticleShapes.slashBetween(sl, TOLL_CLAW.delay(i + 1),
                    middle.subtract(along.scale(length)), middle.add(along.scale(length)));
        }

        tollHitRays(sl, hit, random, 2);
        ParticleShapes.burst(sl, PALE_SPARK, hit, 8, 0.04, 0.2);

        Spells.sound(sl, player.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 0.9f, 1.4f);
        Later.run(sl, 2, () -> Spells.sound(sl, hit, SoundEvents.PLAYER_ATTACK_CRIT, 0.9f, 1.2f));
    }

    // hit 2: a wide pale crescent with a golden tail sweeps in front, and sparks fly off it
    private static void tollArc(ServerPlayer player, LivingEntity target, ServerLevel sl) {
        RandomSource random = player.getRandom();
        Vec3 center = player.position().add(0, 1.0, 0);
        float yaw = player.getYRot();
        SlashFx arc = TOLL_ARC.varied(random, 20f, 6f, 14f);       // left or right, a little tilted

        ParticleShapes.slash(sl, TOLL_ARC_LEAD.alignedWith(arc), center, yaw, 0f, 0f);
        ParticleShapes.slash(sl, arc, center, yaw, 0f, 0f);

        Later.run(sl, 6, () -> {
            ParticleShapes.alongSlash(sl, PALE_SPARK, arc, center, yaw, 0f, 0f, 5f, 12, 0.06, 0.03);
            ParticleShapes.alongSlash(sl, GOLD_SPARK, arc, center, yaw, 0f, 0f, 5f, 8, 0.06, 0.03);
        });

        tollHitRays(sl, target.getBoundingBox().getCenter(), random, 3);

        Spells.sound(sl, player.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 0.9f);
        Later.run(sl, 3, () -> Spells.sound(sl, center, SoundEvents.AMETHYST_BLOCK_CHIME, 1.0f, 1.3f));
    }

    // hit 3: an arm of the dead breaks out beside it and clutches at it, while a cut of light rises through it
    private static void tollGraveHand(ServerPlayer player, LivingEntity target, ServerLevel sl) {
        RandomSource random = player.getRandom();
        Vec3 feet = target.position();
        Vec3 hit = target.getBoundingBox().getCenter();

        // the arm, on the ground beside it, palm toward it (none if it is in the air: no ground to come out of)
        if (target.onGround()) {
            float side = random.nextFloat() * 360f;

            PaleEmperorFx.underworldArm(
                    sl,
                    Spells.spotAround(feet, side, target.getBbWidth() * 0.5 + 0.9),
                    side + 180f,
                    ""
            );

            SpellFx.closingRing(sl, feet, target.getBbWidth() * 0.5 + 1.2, 0xC09CFFD2, 6, 0);
            ParticleShapes.burst(sl, LAND_MIST, feet.add(0, 0.2, 0), 6, 0.02, 0.08);
        }

        // the rising cut: from under its feet to above its head, leaning a little
        Vec3 lean = Spells.flatLook(player).cross(Spells.UP).normalize().scale((random.nextFloat() - 0.5f) * 1.2f);
        Vec3 from = feet.add(lean.scale(-1)).add(0, -0.3, 0);
        Vec3 to = feet.add(lean).add(0, target.getBbHeight() + 0.9, 0);

        ParticleShapes.slashBetween(sl, TOLL_RISE, from, to);
        ParticleShapes.slashBetween(sl, TOLL_RISE_CORE, from, to);

        tollHitRays(sl, hit, random, 4);

        Spells.sound(sl, feet, SoundEvents.CHAIN_PLACE, 1.0f, 0.6f);
        Later.run(sl, 3, () -> Spells.sound(sl, hit, SoundEvents.PLAYER_ATTACK_CRIT, 1.0f, 0.8f));
    }

    // hit 4a: a tall golden cleave comes down through it, and the seal flashes under it
    private static void tollVerdict(ServerPlayer player, LivingEntity target, ServerLevel sl) {
        RandomSource random = player.getRandom();
        float yaw = player.getYRot();
        Vec3 feet = target.position();
        Vec3 hit = target.getBoundingBox().getCenter();

        // the arc's center sits between the caster and the target, so the blade comes down right through it
        Vec3 center = feet.subtract(Spells.flatLook(player).scale(2.0)).add(0, 1.1, 0);
        SlashFx cleave = TOLL_CLEAVE.varied(random, 15f, 8f, 12f);

        ParticleShapes.slash(sl, TOLL_CLEAVE_LEAD.alignedWith(cleave), center, yaw, 0f, 0f);
        ParticleShapes.slash(sl, cleave, center, yaw, 0f, 0f);

        Later.run(sl, 4, () -> {
            seal(sl, feet, yaw, 2.6f, 20);
            SpellFx.shockRing(sl, feet, 2.2, 0xE0F5C211, 8, 0);
            ParticleShapes.alongSlash(sl, GOLD_SPARK, cleave, center, yaw, 0f, 0f, 5f, 14, 0.05, 0.04);
            Spells.sound(sl, feet, SoundEvents.ANVIL_LAND, 0.4f, 0.7f);
        });

        tollHitRays(sl, hit, random, 3);

        Spells.sound(sl, player.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 0.7f);
        Later.run(sl, 2, () -> Spells.sound(sl, hit, SoundEvents.PLAYER_ATTACK_CRIT, 1.0f, 0.7f));
    }

    // hit 4b: a ring of light spins round the caster, a beat of unseen wings scatters feathers
    private static void tollWingbeat(ServerPlayer player, LivingEntity target, ServerLevel sl) {
        RandomSource random = player.getRandom();
        Vec3 center = player.position().add(0, 0.95, 0);
        float yaw = player.getYRot();
        SlashFx ring = TOLL_RING.varied(random, 180f, 5f, 8f);      // starts anywhere, spins either way

        ParticleShapes.slash(sl, TOLL_RING_LEAD.alignedWith(ring), center, yaw, 0f, 0f);
        ParticleShapes.slash(sl, ring, center, yaw, 0f, 0f);

        Later.run(sl, 8, () -> {
            ParticleShapes.alongSlash(sl, PALE_SPARK, ring, center, yaw, 0f, 0f, 7f, 14, 0.07, 0.03);
            SpellFx.shockRing(sl, player.position(), 3.0, 0xC09CFFD2, 8, 0);
        });

        PaleEmperorFx.featherFall(sl, player.position(), 2.5, 6);
        tollHitRays(sl, target.getBoundingBox().getCenter(), random, 4);

        Spells.sound(sl, player.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 0.8f);
        Spells.sound(sl, player.position(), SoundEvents.ENDER_DRAGON_FLAP, 0.6f, 1.2f);
    }

    /** Short rays of pale light with golden tails shooting out round a hit. */
    private static void tollHitRays(ServerLevel sl, Vec3 center, RandomSource random, int delay) {
        for (int i = 0; i < 6; i++) {
            Vec3 dir = ParticleShapes.randomDirectionInCone(random, Spells.UP, 360);
            double inner = 0.5 + random.nextDouble() * 0.2;
            double outer = inner + 0.5 + random.nextDouble() * 0.3;
            ParticleShapes.slashBetween(sl, TOLL_HIT_RAY.delay(delay), center.add(dir.scale(inner)), center.add(dir.scale(outer)));
        }
    }

    // =====================================================================================
    // CLASS CHECK
    // =====================================================================================

    private static boolean canUseCharacteristic(
            Player player,
            boolean bypassClassCheck
    ) {
        if (bypassClassCheck) return true;

        return "Pale Emperor".equals(
                SoulCore.getAspect(player)
        );
    }

    public static void tether(ServerLevel sl, SlashFx line, Supplier<Vec3> from, Supplier<Vec3> to, int ticks) {
        Vec3 a = from.get().add(0,-0.95,0);
        Vec3 b = to.get().add(0,-1,0);
        if (a == null || b == null) return;
        // each copy lives a little longer than the gap to the next one, so the line never blinks
        ParticleShapes.slashBetween(sl, line.lifetime(2 * 3).sweep(1), a, b);
        if (ticks > 2) {
            Later.run(sl, 2, () -> tether(sl, line, from, to, ticks - 2));
        }
    }

    public static void tether(ServerLevel sl, SlashFx line, Entity from, Entity to, int ticks) {
        tether(sl, line, () -> middleOf(from, sl), () -> middleOf(to, sl), ticks);
    }

    public static Vec3 middleOf(Entity entity, ServerLevel sl) {
        if (entity == null || !entity.isAlive() || entity.isRemoved() || entity.level() != sl) return null;
        return entity.getBoundingBox().getCenter();
    }

    // =====================================================================================
    // FEATHERED SERPENT MODELS
    // =====================================================================================

    public static final int SERPENT_TICKS = 150;

    /** The tick the serpent's jaws are wide open: the screech leaves it, or the lines do. */
    public static final int SERPENT_ROARS = 40;

    /**
     * The other moments of its animation: it breaks out of the ground, it stands tall, its jaws snap shut at
     * the end of its strike (normal cast only), it starts to sink. The models below are built from these,
     * and so is what is seen and heard at each of them (serpentShows).
     */
    private static final int SERPENT_BREAKS_OUT = 12;
    private static final int SERPENT_UP = 30;
    private static final int SERPENT_BITES = 86;
    private static final int SERPENT_SINKS = 128;

    /** How far its head tips forward when it strikes, in degrees. */
    private static final float SERPENT_STRIKES = 28f;

    public static final ModelFx SERPENT = ModelFx.of("pale_emperor/feathered_serpent").frames(3).smooth()
            .scale(2f).pivot(8, -16, 12).glow().lifetime(SERPENT_TICKS).fade(0, 4)
            .key(0, pose().up(-6.2f))                                    // under the ground
            .key(SERPENT_BREAKS_OUT, pose().up(-6.2f))                   // waits for the coils and the seal
            .key(SERPENT_UP, pose().up(0), ModelFx.Ease.OUT)                     // rears up
            .during(34, 40, pose().frame(2), ModelFx.Ease.OUT)                   // roars: jaws wide, collar flared
            .during(60, 68, pose().frame(0), ModelFx.Ease.IN_OUT)                // shuts its mouth
            .during(78, 84, pose().frame(2), ModelFx.Ease.OUT)                   // opens it again...
            .during(SERPENT_BITES - 6, SERPENT_BITES, pose().pitch(SERPENT_STRIKES), ModelFx.Ease.IN)   // ...and strikes forward and down
            .during(SERPENT_BITES, SERPENT_BITES + 3, pose().frame(0), ModelFx.Ease.IN)                 // the bite
            .during(94, 108, pose().pitch(0), ModelFx.Ease.IN_OUT)               // draws back
            .during(SERPENT_SINKS, 148, pose().up(-6.2f), ModelFx.Ease.IN);      // sinks away

    /**
     * The serpent of the sneak cast: it rears up, opens its jaws and keeps them open for as long as
     * its lines hold, then shuts them and sinks away. No bite.
     */
    public static final ModelFx SERPENT_BINDING = ModelFx.of("pale_emperor/feathered_serpent").frames(3).smooth()
            .scale(2f).pivot(8, -16, 12).glow().lifetime(SERPENT_TICKS).fade(0, 4)
            .key(0, pose().up(-6.2f))                                    // under the ground
            .key(SERPENT_BREAKS_OUT, pose().up(-6.2f))                   // waits for the coils and the seal
            .key(SERPENT_UP, pose().up(0), ModelFx.Ease.OUT)                     // rears up
            .during(34, 40, pose().frame(2), ModelFx.Ease.OUT)                   // jaws wide: the lines leave its mouth
            .during(100, 108, pose().frame(0), ModelFx.Ease.IN_OUT)              // lets go and shuts its mouth
            .during(SERPENT_SINKS, 148, pose().up(-6.2f), ModelFx.Ease.IN);      // sinks away

    /** Its coiled body, 5 blocks across. */
    public static final ModelFx SERPENT_COIL = ModelFx.of("pale_emperor/feathered_serpent_coil")
            .scale(2f).pivot(8, -16, 8).glow().lifetime(SERPENT_TICKS).fade(0, 4)
            .key(0, pose().up(-2.3f))
            .key(12, pose().up(0), ModelFx.Ease.OUT)
            .during(132, 148, pose().up(-2.3f), ModelFx.Ease.IN);

    /** A seal opens, the coils rise out of it, the serpent rears up, roars, strikes once and sinks away. */
    public static void serpent(ServerLevel sl, Vec3 ground, float yaw) {
        serpent(sl, SERPENT, ground, yaw);
    }

    /** The same with another animation for the head, e.g. SERPENT_BINDING. */
    public static void serpent(ServerLevel sl, ModelFx head, Vec3 ground, float yaw) {
        seal(sl, ground, yaw, 7.5f, SERPENT_TICKS);
        ParticleShapes.model(sl, SERPENT_COIL, ground, yaw, 0f, 0f);
        ParticleShapes.model(sl, head, ground, yaw, 0f, 0f);
    }

    /** Where the open mouth of a serpent that rose at 'ground' is. */
    private static Vec3 serpentMouth(Vec3 ground, float yaw) {
        return ground.add(0, 2.4, 0).add(Vec3.directionFromRotation(0f, yaw).scale(1.7));
    }

    /** Where its mouth is at the end of its strike: the head has tipped SERPENT_STRIKES degrees forward round its base. */
    private static Vec3 serpentBite(Vec3 ground, float yaw) {
        double tipped = Math.toRadians(SERPENT_STRIKES);
        double forward = 1.7 * Math.cos(tipped) + 2.4 * Math.sin(tipped);
        double up = 2.4 * Math.cos(tipped) - 1.7 * Math.sin(tipped);

        return ground.add(0, up, 0).add(Vec3.directionFromRotation(0f, yaw).scale(forward));
    }

    /**
     * What is seen and heard of the serpent itself, for both casts: the ground breaks, feathers fall as it
     * rears up, and mist closes over the place as it sinks. 'bites': the normal cast, whose serpent strikes
     * once and snaps its jaws (that is only for show: the screech is what hurts).
     */
    private static void serpentShows(ServerLevel sl, Vec3 ground, float yaw, boolean bites) {
        Spells.sound(sl, ground, SoundEvents.WITHER_AMBIENT, 0.9f, 0.5f);
        ParticleShapes.ring(sl, LAND_MIST, ground.add(0, 0.2, 0), 2.5, 16, 0.05);

        Later.run(sl, SERPENT_BREAKS_OUT, () -> {
            SpellFx.shockRing(sl, ground, 4.5, 0xD09CFFD2, 10, 0);
            ParticleShapes.burst(sl, LAND_MIST, ground.add(0, 0.4, 0), 14, 0.04, 0.2);
            Spells.sound(sl, ground, SoundEvents.ENDER_DRAGON_FLAP, 0.9f, 0.5f);
        });

        Later.run(sl, SERPENT_UP, () -> PaleEmperorFx.featherFall(sl, ground, 3.5, 10));

        if (bites) {
            Later.run(sl, SERPENT_BITES, () -> {
                Vec3 jaws = serpentBite(ground, yaw);

                ParticleShapes.burst(sl, PALE_SPARK, jaws, 16, 0.05, 0.3);
                Spells.sound(sl, jaws, SoundEvents.PLAYER_ATTACK_CRIT, 1.2f, 0.5f);
                Spells.sound(sl, jaws, SoundEvents.ANVIL_LAND, 0.4f, 1.4f);
            });
        }

        Later.run(sl, SERPENT_SINKS, () -> {
            SpellFx.closingRing(sl, ground, 4.5, 0xB09CFFD2, 16, 0);
            ParticleShapes.ring(sl, LAND_MIST, ground.add(0, 0.2, 0), 4.0, 18, -0.12);
        });
    }

    // =====================================================================================
    // SERPENT: SCREECH (normal cast)
    // =====================================================================================

    /** What the screech does to everyone its wave reaches. */
    public static final float SCREECH_DAMAGE = 45f;

    /** How far the wave travels from the serpent, in blocks. */
    public static final double SCREECH_RADIUS = 16;

    /** How far above and below the serpent's ground the wave still catches someone. */
    public static final double SCREECH_HEIGHT = 6;

    /** How long the wave takes to get that far: 16 blocks in 20 ticks = 16 blocks a second. */
    public static final int SCREECH_TICKS = 20;

    /**
     * The serpent screeches: a wave races outward from it in every direction, and each enemy is hurt
     * once, at the moment the wave gets to it.
     */
    private static void screech(Player player, ServerLevel sl, Vec3 ground, float yaw) {
        if (!Spells.casterStillHere(player, sl)) return;

        Vec3 mouth = serpentMouth(ground, yaw);

        Spells.sound(sl, mouth, SoundEvents.ENDER_DRAGON_GROWL, 1.6f, 1.7f);
        Spells.sound(sl, mouth, SoundEvents.WARDEN_SONIC_BOOM, 1.2f, 1.4f);

        // its breath
        Vec3 facing = Vec3.directionFromRotation(0f, yaw);

        ParticleShapes.cone(sl, PALE_SPARK, mouth, facing, 70, 40, 0.3, 0.9);
        ParticleShapes.cone(sl, LAND_MIST, mouth, facing, 50, 12, 0.15, 0.45);

        // the first ring is the wave itself, the others follow it like an echo
        SpellFx.steadyRing(sl, ground, SCREECH_RADIUS, 0xF09CFFD2, SCREECH_TICKS, 0);
        SpellFx.steadyRing(sl, ground.add(0, 0.02, 0), SCREECH_RADIUS, 0xB0F5C211, SCREECH_TICKS, 3);
        SpellFx.steadyRing(sl, ground.add(0, 0.04, 0), SCREECH_RADIUS, 0x809CFFD2, SCREECH_TICKS, 6);
        SpellFx.steadyRing(sl, new Vec3(ground.x, mouth.y, ground.z), SCREECH_RADIUS, 0x709CFFD2, SCREECH_TICKS, 0);

        // the wave keeps pace with the first ring: each enemy is hurt when that ring touches it
        Spells.wave(
                player,
                sl,
                ground,
                SCREECH_RADIUS,
                SCREECH_TICKS,
                SCREECH_HEIGHT,
                enemy -> {
                    Spells.strike(player, enemy, SCREECH_DAMAGE);

                    Vec3 middle = enemy.getBoundingBox().getCenter();

                    ParticleShapes.burst(sl, PALE_SPARK, middle, 8, 0.05, 0.25);
                    Spells.sound(sl, middle, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 0.5f, 1.4f);
                }
        );
    }

    // =====================================================================================
    // SERPENT: LINES (sneak cast)
    // =====================================================================================

    private static final SlashFx SERPENT_LINE = SlashFx.line("slash/smooth")
            .color(0xC09CFFD2)
            .core(0xE0F4FFE8)
            .width(0.1f)
            .taper(SlashFx.Taper.UNIFORM);

    /** How far in front of the serpent a mob can stand and still be caught, in blocks. */
    public static final double LINE_RANGE = 24;

    /** How far to either side of straight ahead still counts as "in front", in degrees. */
    public static final double LINE_HALF_ANGLE = 50;

    /** How long the lines hold. The serpent keeps its jaws open exactly this long. */
    public static final int LINE_TICKS = 60;

    /** A line hurts what it holds once every this many ticks: 6 times in all. */
    public static final int LINE_PULSE = 10;

    /** What each of those does. */
    public static final float LINE_DAMAGE = 8f;

    /**
     * The serpent draws a line from its mouth to every enemy standing in front of it. Each line hurts
     * what it holds again and again until its time is up, the enemy dies or it gets too far away.
     */
    private static void serpentLines(
            Player player,
            ServerLevel sl,
            Vec3 ground,
            float yaw,
            Vec3 castFrom
    ) {
        if (!Spells.casterStillHere(player, sl)) return;

        Vec3 mouth = serpentMouth(ground, yaw);

        List<LivingEntity> caught = enemiesInFront(player, sl, ground, yaw, mouth, castFrom);

        if (caught.isEmpty()) {
            // it hisses at nothing
            Spells.sound(sl, mouth, SoundEvents.ENDER_DRAGON_GROWL, 0.6f, 1.9f);
            player.sendSystemMessage(Component.literal("The serpent finds no one before it."));
            return;
        }

        ParticleShapes.cone(sl, PALE_SPARK, mouth, Vec3.directionFromRotation(0f, yaw), 60, 24, 0.2, 0.6);

        Spells.sound(sl, mouth, SoundEvents.ENDER_DRAGON_GROWL, 1.0f, 1.9f);
        Spells.sound(sl, mouth, SoundEvents.CHAIN_PLACE, 1.4f, 0.6f);

        double snapsAt = (LINE_RANGE + 8) * (LINE_RANGE + 8);

        for (LivingEntity enemy : caught) {

            boolean[] snapped = {false};

            // the far end of the line. Once it is null the line is gone for good, and so is its damage
            Supplier<Vec3> end = () -> {
                if (snapped[0]) return null;

                Vec3 middle = Spells.casterStillHere(player, sl) && Spells.isEnemy(player, enemy)
                        ? SpellFx.middleOf(enemy, sl)
                        : null;

                if (middle == null || middle.distanceToSqr(mouth) > snapsAt) {
                    snapped[0] = true;
                    return null;
                }

                return middle;
            };

            SpellFx.tether(
                    sl,
                    SERPENT_LINE,
                    () -> mouth,
                    end,
                    LINE_TICKS
            );

            // the line takes hold
            SpellFx.closingRing(sl, enemy.position(), enemy.getBbWidth() * 0.5 + 1.0, 0xC09CFFD2, 6, 0);

            for (int tick = 0; tick < LINE_TICKS; tick += LINE_PULSE) {
                Later.run(
                        sl,
                        tick,
                        () -> {
                            Vec3 held = end.get();

                            if (held == null) return;

                            Spells.strike(player, enemy, LINE_DAMAGE);

                            ParticleShapes.burst(sl, PALE_SPARK, held, 6, 0.04, 0.2);
                            ParticleShapes.spawn(sl, PALE_SPARK, mouth, Vec3.ZERO);        // its mouth glows while it holds them
                        }
                );
            }
        }
    }

    /**
     * Every enemy standing in front of the serpent: within LINE_RANGE of it, no more than
     * LINE_HALF_ANGLE to either side of the way it faces, and not behind a wall.
     */
    private static List<LivingEntity> enemiesInFront(
            Player player,
            ServerLevel sl,
            Vec3 ground,
            float yaw,
            Vec3 mouth,
            Vec3 castFrom
    ) {
        Vec3 facing = Vec3.directionFromRotation(0f, yaw);

        double narrowest = Math.cos(Math.toRadians(LINE_HALF_ANGLE));

        List<LivingEntity> result = new ArrayList<>();

        for (LivingEntity enemy : Spells.enemiesAround(player, sl, ground, LINE_RANGE)) {

            Vec3 body = enemy.getBoundingBox().getCenter();

            Vec3 toEnemy = new Vec3(
                    body.x - ground.x,
                    0,
                    body.z - ground.z
            );

            // something standing right on the serpent has no direction: it counts as in front
            if (toEnemy.lengthSqr() > 0.01 && toEnemy.normalize().dot(facing) < narrowest) continue;

            // seen from the serpent's mouth, or from where the caster stood (the head may be up in a ceiling)
            if (!Spells.clearLine(player, sl, mouth, body)
                    && !Spells.clearLine(player, sl, castFrom, body)) continue;

            result.add(enemy);
        }

        return result;
    }

    // =====================================================================================
    // PALE EMPEROR SERPENT
    // =====================================================================================

    public static void paleEmperorSerpent(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!canUseCharacteristic(player, bypassClassCheck)) return;
        if (SoulCore.getSoulEssence(player) < 6000) return;
        if (SoulCore.getAscensionStage(player) < 2) return;

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 6000);

        Vec3 direction = player.getLookAngle().normalize();

        Vec3 ground = player.position();

        float yaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));

        // =====================================================================
        // NORMAL CAST - THE SERPENT SCREECHES
        // =====================================================================

        if (!player.isShiftKeyDown()) {

            serpent(sl, ground, yaw);
            serpentShows(sl, ground, yaw, true);

            Later.run(
                    sl,
                    SERPENT_ROARS,
                    () -> screech(player, sl, ground, yaw)
            );

            // =====================================================================
            // SHIFT CAST - LINES TO THE MOBS IN FRONT OF IT
            // =====================================================================

        } else {

            Vec3 castFrom = player.getEyePosition();

            serpent(sl, SERPENT_BINDING, ground, yaw);
            serpentShows(sl, ground, yaw, false);

            Later.run(
                    sl,
                    SERPENT_ROARS,
                    () -> serpentLines(player, sl, ground, yaw, castFrom)
            );
        }
    }

    public static final int SKULL_TICKS = 40;
    public static final ModelFx SKULL = ModelFx.of("pale_emperor/death_skull").frames(3).smooth()
            .scale(0.5f).pivot(8, 8, 8).glow().lifetime(SKULL_TICKS).fade(0, 12)
            .key(0, pose().scale(0.15f).alpha(0f))
            .key(10, pose().scale(1f).alpha(1f), ModelFx.Ease.OUT_BACK)
            .during(10, 40, pose().forward(32f), ModelFx.Ease.IN_OUT);


    public static final int SPEAR_TICKS = 80;
    public static final ModelFx SPEAR = ModelFx.of("pale_emperor/bone_spear")
            .scale(1.2f).pivot(8, 8, 8).glow().aura(0x409CFFD2, 0.07f, 2).lifetime(40).fade(0, 4);
    private static final ModelFx SPEAR_HOVERS = SPEAR.lifetime(SPEAR_TICKS)
            .key(0, pose().scale(0.3f).alpha(0f))
            .key(8, pose().scale(1f).alpha(1f), ModelFx.Ease.OUT_BACK)           // appears, point up
            .during(4, 25, pose().spin(360), ModelFx.Ease.IN_OUT)                // turns once
            .during(25, 30, pose().pitch(90), ModelFx.Ease.IN_OUT)               // tips over: the point looks forward
            .during(30, 32, pose().forward(-1f), ModelFx.Ease.OUT)               // draws back
            .during(32, 64, pose().forward(36f), ModelFx.Ease.IN);


    // =====================================================================================
    // SPEAR AND SKULL (ability 4)
    // =====================================================================================
    //
    // SPEAR_HOVERS and SKULL above are only the animations. The two AnimatedShots below follow them tick by
    // tick and hit what they reach, so the keys, the timing and the lifetime up there can be changed freely:
    // nothing down here names a tick. Both fly at what the crosshair is on, up and down too.

    /** How high above the caster's feet they appear. */
    private static final double SPEAR_HEIGHT = 2.2;
    private static final double SKULL_HEIGHT = 2.6;

    /** What the spear does to each enemy it goes through. */
    public static final float SPEAR_DAMAGE = 40f;

    /** What the skull's blast does to every enemy within SKULL_BLAST_RADIUS blocks of it. */
    public static final float SKULL_DAMAGE = 30f;
    public static final double SKULL_BLAST_RADIUS = 4.5;

    private static final AnimatedShot SPEAR_SHOT = AnimatedShot.of(SPEAR_HOVERS)
            .width(0.6)              // how close its path must pass to a body to hit it
            .tip(1.8)                // blocks from the middle of the shaft (its pivot) to its point, at scale 1.2
            .pierce();               // goes through every enemy on its way

    private static final AnimatedShot SKULL_SHOT = AnimatedShot.of(SKULL)
            .width(0.6);             // stops at the first enemy: that is where it bursts

    private static final ParticleFx PALE_SPARK = ParticleFx.of("fx/glow")
            .color(0xFFC8FFE4).endColor(0x0018A070)
            .size(0.12f).endSize(0.03f).sizeRandom(0.4f)
            .lifetime(16, 10).gravity(-0.02f).friction(0.9f)
            .glow();

    public static void paleEmperorSpearSkull(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!canUseCharacteristic(player, bypassClassCheck)) return;
        if (SoulCore.getSoulEssence(player) < 600) return;
        if (SoulCore.getAscensionStage(player) < 2) return;

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 600);

        if (!player.isShiftKeyDown()){

            // SPEAR: hurts every enemy it goes through and flies on. A block ends it.
            Vec3 from = Spells.clearStart(player, sl, player.position().add(0, SPEAR_HEIGHT, 0));

            shotAppears(sl, from);

            boolean[] thrown = {false};

            SPEAR_SHOT
                    .onMove(at -> {
                        // the first tick it flies is the throw; from then on it leaves a trail
                        if (!thrown[0]) {
                            thrown[0] = true;
                            Spells.sound(sl, at, SoundEvents.WITHER_SHOOT, 0.6f, 1.6f);
                            Spells.sound(sl, at, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 0.6f);
                        }
                        ParticleShapes.burst(sl, PALE_SPARK, at, 2, 0.0, 0.04);
                    })
                    .onHit((target, at) -> {
                        Spells.strike(player, target, SPEAR_DAMAGE);
                        ParticleShapes.burst(sl, PALE_SPARK, at, 12, 0.05, 0.25);
                        ParticleShapes.burst(sl, GOLD_SPARK, at, 6, 0.05, 0.2);
                        SpellFx.shockRing(sl, at.add(0, -0.3, 0), 1.4, 0xD09CFFD2, 6, 0);
                        Spells.sound(sl, at, SoundEvents.PLAYER_ATTACK_CRIT, 0.9f, 0.7f);
                    })
                    .onStop((at, hitBlock) -> {
                        ParticleShapes.burst(sl, PALE_SPARK, at, 16, 0.05, 0.3);
                        ParticleShapes.burst(sl, LAND_MIST, at, 5, 0.02, 0.1);
                        SpellFx.shockRing(sl, at.add(0, -0.2, 0), 1.6, 0xB0F5C211, 7, 0);
                        Spells.sound(sl, at, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 0.7f, 0.6f);
                    })
                    .fire(player, sl, from);

        }else{

            // SKULL: bursts on the first enemy it reaches, or on a block. With nothing in its way it only fades.
            Vec3 from = Spells.clearStart(player, sl, player.position().add(0, SKULL_HEIGHT, 0));

            shotAppears(sl, from);

            boolean[] loosed = {false};

            SKULL_SHOT
                    .onMove(at -> {
                        // the first tick it flies it howls; from then on it trails mist and sparks
                        if (!loosed[0]) {
                            loosed[0] = true;
                            Spells.sound(sl, at, SoundEvents.WITHER_AMBIENT, 0.7f, 1.5f);
                        }
                        ParticleShapes.burst(sl, LAND_MIST, at, 1, 0.0, 0.02);
                        ParticleShapes.burst(sl, PALE_SPARK, at, 1, 0.0, 0.05);
                    })
                    .onStop((at, hitBlock) -> skullBursts(player, sl, at))
                    .onExpire(at -> {
                        // nothing was in its way: it only fades
                        ParticleShapes.burst(sl, LAND_MIST, at, 8, 0.02, 0.1);
                        ParticleShapes.burst(sl, PALE_SPARK, at, 8, 0.03, 0.15);
                    })
                    .fire(player, sl, from);
        }
    }

    /** The spear or the skull takes shape over the caster's shoulder. */
    private static void shotAppears(ServerLevel sl, Vec3 at) {
        ParticleShapes.burst(sl, PALE_SPARK, at, 14, 0.03, 0.18);
        SpellFx.closingRing(sl, at.add(0, -0.4, 0), 1.3, 0xC09CFFD2, 8, 0);
        Spells.sound(sl, at, SoundEvents.ILLUSIONER_CAST_SPELL, 0.8f, 0.7f);
    }

    /** The skull is gone in a pale blast that hurts every enemy around it. */
    private static void skullBursts(Player player, ServerLevel sl, Vec3 at) {
        for (LivingEntity enemy : Spells.enemiesAround(player, sl, at, SKULL_BLAST_RADIUS)) {
            Spells.strike(player, enemy, SKULL_DAMAGE);
            ParticleShapes.burst(sl, PALE_SPARK, enemy.getBoundingBox().getCenter(), 8, 0.05, 0.25);
        }

        // what is left of it hangs in the air for a moment
        ParticleShapes.burst(sl, LAND_MIST, at, 16, 0.02, 0.12);
        ParticleShapes.burst(sl, GOLD_SPARK, at, 14, 0.05, 0.3);

        SpellFx.blast(sl, at, SKULL_BLAST_RADIUS, 0xE09CFFD2, PALE_SPARK);
        SpellFx.shockRing(sl, at.add(0, -0.3, 0), SKULL_BLAST_RADIUS * 0.7, 0xB0F5C211, 10, 2);       // a second, golden ring

        Spells.sound(sl, at, SoundEvents.GENERIC_EXPLODE.value(), 1f, 1.3f);
        Spells.sound(sl, at, SoundEvents.WITHER_HURT, 0.8f, 0.6f);
    }

    // =====================================================================================
    // BONE SPIKES AND THE HANDS OF THE UNDERWORLD (ability 5)
    // =====================================================================================
    //
    // Normal cast: spikes break out under every enemy the caster can see. They hurt and blind it, and it
    //   remembers how often it was hit: every hit after the first hurts more.
    // Sneak cast: every enemy in sight pays for the hits it remembers, 15 damage for each. With 3 or more,
    //   hands come out of the ground around it, close on it and drag it under.
    //
    // Built from the kit: Spells.enemiesInSight (who), status/Marks (the hits it remembers), SpellFx.overHead
    // (the feathers that show them), status/Blind, status/Burial (dragged under).

    private static final float SPIKES_COST = 2000;               // either cast
    private static final int SPIKES_STAGE = 3;

    /** Who it reaches: every enemy in sight this far away, this many degrees to either side of the crosshair. */
    public static final double SIGHT_RANGE = 24;
    public static final double SIGHT_ANGLE = 45;

    /** The first hit of the spikes, and how much more each hit it already remembers adds. */
    public static final float SPIKE_DAMAGE = 10f;
    public static final float SPIKE_DAMAGE_PER_HIT = 5f;

    /** How long the spikes blind, and how near a blinded mob must be to something to go after it. */
    public static final int BLIND_TICKS = 80;
    public static final double BLIND_MOB_SEES = 3.0;

    /** An enemy forgets its hits this long after the last one, and never remembers more than HITS_MAX. */
    public static final int HITS_FORGOTTEN_AFTER = 300;
    public static final int HITS_MAX = 10;

    /** Sneak cast: damage for each hit it remembers, and how many hits it takes for the hands to come. */
    public static final float HANDS_DAMAGE_PER_HIT = 15f;
    public static final int HANDS_NEED_HITS = 3;

    /** How long the hands keep it under the ground before it is given back. */
    public static final int BURIED_TICKS = 40;

    /** The name of the hits an enemy remembers (status/Marks), and of the feathers over its head that show them. */
    public static final String SPIKE_HITS = "pale_emperor_hits";

    public static void paleEmperorSpikes(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!canUseCharacteristic(player, bypassClassCheck)) return;
        if (SoulCore.getAscensionStage(player) < SPIKES_STAGE) return;

        List<LivingEntity> seen = Spells.enemiesInSight(player, sl, SIGHT_RANGE, SIGHT_ANGLE);

        // =====================================================================
        // NORMAL CAST - SPIKES UNDER EVERYONE IN SIGHT
        // =====================================================================

        if (!player.isShiftKeyDown()) {

            if (seen.isEmpty()) {
                player.sendSystemMessage(Component.literal("No one stands before you."));
                return;                                             // nothing spent
            }

            if (!Spells.payEssence(player, SPIKES_COST)) return;

            // the caster calls them up
            SpellFx.shockRing(sl, player.position(), 2.2, 0xD09CFFD2, 8, 0);
            Spells.sound(sl, player.position(), SoundEvents.ANVIL_LAND, 0.35f, 0.6f);

            for (LivingEntity enemy : seen) {
                spikesUnder(player, sl, enemy);
            }

            // =====================================================================
            // SHIFT CAST - THEY PAY FOR THE HITS THEY REMEMBER
            // =====================================================================

        } else {

            List<LivingEntity> marked = new ArrayList<>();

            for (LivingEntity enemy : seen) {
                if (Marks.count(enemy, SPIKE_HITS) > 0) marked.add(enemy);
            }

            if (marked.isEmpty()) {
                player.sendSystemMessage(Component.literal("No one before you has felt your spikes."));
                return;                                             // nothing spent
            }

            if (!Spells.payEssence(player, SPIKES_COST)) return;

            // the caster calls in what is owed
            SpellFx.closingRing(sl, player.position(), 2.2, 0xD0F5C211, 8, 0);
            Spells.sound(sl, player.position(), SoundEvents.BELL_BLOCK, 0.9f, 0.6f);

            for (LivingEntity enemy : marked) {
                handsTake(player, sl, enemy);
            }
        }
    }

    /** One small feather for each hit, in a ring over its head. They turn gold when the hands can come. */
    private static final ModelFx HIT_FEATHER = ModelFx.of("pale_emperor/pale_feather")
            .scale(0.3f).pivot(8, 8, 8).glow().spin(6f)
            .lifetime(HITS_FORGOTTEN_AFTER).fade(4, 10);                 // gone when the hits are forgotten

    // =====================================================================================
    // SPIKES (normal cast)
    // =====================================================================================

    /** The tick the spikes are out (the hit lands), the tick they start to sink, and when they are gone. */
    private static final int SPIKES_OUT = 4;
    private static final int SPIKES_SINK = 30;
    private static final int SPIKES_TICKS = 44;

    /** The spikes at this size: they are 2.75 blocks tall at scale 1. */
    private static ModelFx boneSpikes(float scale) {
        float hidden = -2.9f * scale;                                // this far down they are under the ground

        return ModelFx.of("pale_emperor/bone_spikes")
                .scale(scale).pivot(8, -16, 8).glow().lifetime(SPIKES_TICKS).fade(0, 4)
                .key(0, pose().up(hidden))
                .key(SPIKES_OUT, pose().up(0), ModelFx.Ease.OUT_BACK)                 // burst out, a little too far, and settle
                .during(SPIKES_SINK, SPIKES_TICKS - 2, pose().up(hidden), ModelFx.Ease.IN);
    }

    private static void spikesUnder(Player player, ServerLevel sl, LivingEntity enemy) {
        // a little taller than what they hit
        float scale = (float) Math.max(0.5, Math.min(1.6, enemy.getBbHeight() / 2.75 * 1.2));

        Vec3 ground = Spells.groundUnder(player, sl, enemy);

        ParticleShapes.model(
                sl,
                boneSpikes(scale),
                ground,
                sl.getRandom().nextFloat() * 360f,
                0f,
                0f
        );

        // the ground breaks where they come out
        ParticleShapes.burst(sl, LAND_MIST, ground.add(0, 0.2, 0), 6, 0.02, 0.1);
        SpellFx.shockRing(sl, ground, 0.9 + scale, 0xC09CFFD2, 6, SPIKES_OUT - 1);

        Later.run(
                sl,
                SPIKES_OUT,
                () -> spikesHit(player, sl, enemy)
        );
    }

    private static void spikesHit(Player player, ServerLevel sl, LivingEntity enemy) {
        if (!Spells.casterStillHere(player, sl)) return;
        if (enemy.level() != sl || !Spells.isEnemy(player, enemy)) return;

        // this hit counts too: the first does SPIKE_DAMAGE, each one after it more
        int hits = Math.min(HITS_MAX, Marks.count(enemy, SPIKE_HITS) + 1);

        Spells.strike(player, enemy, SPIKE_DAMAGE + SPIKE_DAMAGE_PER_HIT * (hits - 1));

        Vec3 middle = enemy.getBoundingBox().getCenter();

        ParticleShapes.burst(sl, PALE_SPARK, middle, 10, 0.05, 0.25);
        Spells.sound(sl, middle, SoundEvents.AMETHYST_CLUSTER_BREAK, 0.9f, 0.5f);

        if (!enemy.isAlive()) return;                                // that was the end of it

        hits = Marks.add(enemy, SPIKE_HITS, HITS_MAX, HITS_FORGOTTEN_AFTER);

        SpellFx.overHead(
                sl,
                enemy,
                hits >= HANDS_NEED_HITS ? HIT_FEATHER.color(0xFFFFC83C) : HIT_FEATHER,
                hits,
                SPIKE_HITS
        );

        // (a mob does not care about the blindness effect: Blind also makes it lose what it was hunting)
        Blind.apply(enemy, BLIND_TICKS, BLIND_MOB_SEES);
    }

    // =====================================================================================
    // HANDS (sneak cast)
    // =====================================================================================

    /**
     * The hands' timing. Their animation is built from these four numbers and so is everything that happens
     * to the enemy, so changing one moves both.
     */
    private static final int HANDS_OUT = 8;                          // they are out of the ground
    private static final int HANDS_GRASP = 16;                       // they have closed: the damage lands
    private static final int HANDS_PULL = 24;                        // they start to pull down
    private static final int HANDS_UNDER = 44;                       // they are gone, and so is what they held

    /** How far under the ground an arm is before it rises and after it has sunk, at scale 1 (it is 3 blocks tall). */
    private static final float ARM_HIDDEN = 3.1f;

    /** One arm at this size. */
    private static ModelFx draggingArm(float scale) {
        float hidden = -ARM_HIDDEN * scale;

        return ModelFx.of("pale_emperor/underworld_arm").frames(5).smooth()
                .scale(scale).pivot(8, -16, 8).glow().lifetime(HANDS_UNDER + 2).fade(0, 4)
                .key(0, pose().frame(3).up(hidden))                                   // half closed, under the ground
                .key(HANDS_OUT, pose().up(0), ModelFx.Ease.OUT)                       // rises
                .during(2, HANDS_OUT + 2, pose().frame(0), ModelFx.Ease.OUT)          // opens wide
                .during(HANDS_GRASP - 5, HANDS_GRASP, pose().frame(4), ModelFx.Ease.IN)       // closes on it
                .during(HANDS_PULL, HANDS_UNDER, pose().up(hidden), ModelFx.Ease.IN);         // and takes it down
    }

    /** One enemy pays for the hits it remembers. They are used up by this. */
    private static void handsTake(Player player, ServerLevel sl, LivingEntity enemy) {
        int hits = Marks.take(enemy, SPIKE_HITS);

        ParticleShapes.clearModels(sl, enemy, SPIKE_HITS);           // the feathers over its head

        float damage = HANDS_DAMAGE_PER_HIT * hits;

        // too few hits, or nothing under its feet for hands to come out of: only the damage
        if (hits < HANDS_NEED_HITS || !enemy.onGround()) {
            Vec3 middle = enemy.getBoundingBox().getCenter();

            Spells.strike(player, enemy, damage);

            ParticleShapes.burst(sl, PALE_SPARK, middle, 14, 0.05, 0.3);
            Spells.sound(sl, middle, SoundEvents.WITHER_HURT, 0.7f, 0.8f);

            // one hand of the dead claws at it, if there is ground for it to come out of
            if (enemy.onGround()) {
                float side = sl.getRandom().nextFloat() * 360f;

                PaleEmperorFx.underworldArm(
                        sl,
                        Spells.spotAround(enemy.position(), side, enemy.getBbWidth() * 0.5 + 0.9),
                        side + 180f,
                        ""
                );
            }
            return;
        }

        Vec3 ground = enemy.position();
        float yaw = player.getYRot();

        // arms about as tall as one and a half of what they take
        float scale = Math.max(0.8f, Math.min(2.0f, enemy.getBbHeight() * 0.55f));
        ModelFx arm = draggingArm(scale);

        // they lean in by 16 degrees: this far out, their hands close just outside its body
        double radius = enemy.getBbWidth() / 2.0 + 0.25 + 0.83 * scale;

        int arms = 5;

        for (int i = 0; i < arms; i++) {

            float around = yaw + i * 360f / arms;

            // around + 180 = its palm looks back at the middle; pitch 16 = it leans that way
            ParticleShapes.model(
                    sl,
                    i % 2 == 0 ? arm : arm.mirrored(),
                    Spells.spotAround(ground, around, radius),
                    around + 180f,
                    16f,
                    0f
            );
        }

        seal(sl, ground, yaw, (float) (radius * 2.0 + 1.5), HANDS_UNDER);

        // held until the hands pull (Burial holds it from then on) or, if it is too big to bury, until they are gone
        boolean buried = Burial.canBury(enemy);

        Root.apply(enemy, buried ? HANDS_PULL + 2 : HANDS_UNDER, RootRestriction.EVERYTHING);

        Spells.sound(sl, ground, SoundEvents.WITHER_AMBIENT, 0.9f, 0.5f);

        Later.run(
                sl,
                HANDS_GRASP,
                () -> handsClose(player, sl, enemy, damage)
        );

        if (!buried) return;

        Later.run(
                sl,
                HANDS_PULL,
                () -> {
                    if (!Spells.casterStillHere(player, sl) || enemy.level() != sl || !enemy.isAlive()) return;

                    // it goes down exactly as fast as the arms do (their depth is the 'pull'), stays under
                    // for BURIED_TICKS and is then put back where it stood
                    Burial.bury(enemy, HANDS_UNDER - HANDS_PULL, BURIED_TICKS, ARM_HIDDEN * scale);

                    // the ground closes over it
                    SpellFx.closingRing(sl, ground, radius + 1.0, 0xD09CFFD2, HANDS_UNDER - HANDS_PULL, 0);
                    ParticleShapes.ring(sl, LAND_MIST, ground.add(0, 0.2, 0), radius + 0.5, 14, -0.08);
                    Spells.sound(sl, ground, SoundEvents.WITHER_AMBIENT, 0.7f, 0.4f);
                }
        );

        // the moment it is given back (Burial puts it where it stood)
        Later.run(
                sl,
                HANDS_UNDER + BURIED_TICKS,
                () -> {
                    if (enemy.level() != sl || !enemy.isAlive()) return;

                    SpellFx.shockRing(sl, ground, radius + 1.0, 0xC09CFFD2, 8, 0);
                    ParticleShapes.burst(sl, LAND_MIST, ground.add(0, 0.3, 0), 12, 0.03, 0.15);
                    Spells.sound(sl, ground, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 0.8f, 0.5f);
                }
        );
    }

    private static void handsClose(Player player, ServerLevel sl, LivingEntity enemy, float damage) {
        if (!Spells.casterStillHere(player, sl)) return;
        if (enemy.level() != sl || !enemy.isAlive()) return;

        Vec3 middle = enemy.getBoundingBox().getCenter();

        Spells.strike(player, enemy, damage);

        ParticleShapes.burst(sl, PALE_SPARK, middle, 24, 0.05, 0.35);
        ParticleShapes.burst(sl, GOLD_SPARK, middle, 10, 0.05, 0.3);
        SpellFx.shockRing(sl, enemy.position(), enemy.getBbWidth() * 0.5 + 1.4, 0xD0F5C211, 7, 0);
        Spells.sound(sl, middle, SoundEvents.PLAYER_ATTACK_CRIT, 1.0f, 0.5f);
    }

    // =====================================================================================
    // PALE WINGS AND THE STEP (ability 6)
    // =====================================================================================
    //
    // Normal cast: wings grow from the caster's back and for FLIGHT_TICKS they can fly the way a creative
    //   player does. Cast again to fold them early. However the flight ends, the fall after it does no harm.
    // Sneak cast: the caster is at once where they look.
    //
    // Built from the kit: status/Flight (the flying), PaleEmperorFx.wings (wings that turn with the wearer),
    // Spells.blinkSpot (where to stand), status/FallGuard.

    private static final float WINGS_COST = 4000;
    private static final float STEP_COST = 1500;
    private static final int WINGS_STAGE = 4;

    /** How long the wings carry: 20 seconds. */
    public static final int FLIGHT_TICKS = 400;

    /** The caster is told this many ticks before the wings fold. */
    private static final int FLIGHT_WARNING = 60;

    /** How far the step reaches. */
    public static final double STEP_RANGE = 32;

    /** Set while the player wears the wings of this ability (their flight itself is status/Flight). Saved with the player. */
    private static final String WINGS_ON = "pale_emperor_wings_on";

    /** The name of the wing models on the player. */
    private static final String WINGS_TAG = "pale_emperor_wings";

    public static void paleEmperorWings(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!canUseCharacteristic(player, bypassClassCheck)) return;
        if (SoulCore.getAscensionStage(player) < WINGS_STAGE) return;

        if (player.isShiftKeyDown()) {
            stepThere(player, sl);
            return;
        }

        if (hasWings(player)) {                                      // cast again: the wings fold, nothing spent
            foldWings(player, sl);
            return;
        }

        if (!Spells.payEssence(player, WINGS_COST)) return;

        spreadWings(player, sl);
    }

    // =====================================================================================
    // WINGS (normal cast)
    // =====================================================================================

    /** True while the wings of this ability carry the player. */
    public static boolean hasWings(Player player) {
        return player.getPersistentData().getBoolean(WINGS_ON) && Flight.has(player);
    }

    private static void spreadWings(Player player, ServerLevel sl) {
        player.getPersistentData().putBoolean(WINGS_ON, true);

        Flight.grant(player, FLIGHT_TICKS);                          // lifts them off the ground too

        PaleEmperorFx.wings(sl, player, FLIGHT_TICKS, WINGS_TAG);
        PaleEmperorFx.featherFall(sl, player.position(), 1.5, 10);

        Spells.sound(sl, player.position(), SoundEvents.ENDER_DRAGON_FLAP, 1.0f, 0.8f);

        // the gust of the first beat
        Vec3 feet = player.position();

        SpellFx.shockRing(sl, feet, 3.5, 0xD09CFFD2, 10, 0);
        SpellFx.risingRings(sl, feet, 1.6, 2.4, 3, 0xA0F5C211);
        ParticleShapes.burst(sl, PALE_SPARK, feet.add(0, 1.0, 0), 20, 0.05, 0.3);
    }

    /** The wings are gone: a few feathers and sparks are left where they were. */
    private static void wingsShed(Player player, ServerLevel sl) {
        Vec3 back = player.position().add(0, 1.2, 0);

        ParticleShapes.burst(sl, PALE_SPARK, back, 14, 0.04, 0.22);
        PaleEmperorFx.featherFall(sl, player.position().add(0, -3.5, 0), 1.2, 6);      // (from about the player's height)
    }

    /** Ends the flight before its time. */
    private static void foldWings(Player player, ServerLevel sl) {
        player.getPersistentData().remove(WINGS_ON);

        Flight.end(player);                                          // (the way down does no harm)

        PaleEmperorFx.foldWings(sl, player, WINGS_TAG);

        Spells.sound(sl, player.position(), SoundEvents.ENDER_DRAGON_FLAP, 0.7f, 0.6f);

        wingsShed(player, sl);
    }

    /** One tick of wearing the wings: the warning, the sound of the beats, a feather now and then. */
    private static void wingsTick(Player player, ServerLevel sl) {
        if (!player.getPersistentData().getBoolean(WINGS_ON)) return;

        int left = Flight.ticksLeft(player);

        // the flight ran out (the wings folded by themselves at the end of their time)
        if (left <= 0) {
            player.getPersistentData().remove(WINGS_ON);
            Spells.sound(sl, player.position(), SoundEvents.ENDER_DRAGON_FLAP, 0.7f, 0.6f);
            wingsShed(player, sl);
            return;
        }

        if (left == FLIGHT_WARNING) {
            player.sendSystemMessage(Component.literal("Your wings grow heavy."));
        }

        if (player.getAbilities().flying && !player.isSpectator()) {
            if (left % 20 == 0) {
                Spells.sound(sl, player.position(), SoundEvents.ENDER_DRAGON_FLAP, 0.35f, 1.2f);
            }
            if (left % 10 == 0) {
                // one feather, starting about where the player is and drifting down behind them
                PaleEmperorFx.featherFall(sl, player.position().add(0, -3.5, 0), 0.5, 1);
            }
            if (left % 4 == 0) {
                // a spark off the wings
                ParticleShapes.burst(sl, PALE_SPARK, player.position().add(0, 1.2, 0), 1, 0.0, 0.04);
            }
        }
    }

    // =====================================================================================
    // THE STEP (sneak cast)
    // =====================================================================================

    private static final SlashFx STEP_STREAK = SlashFx.line("slash/smooth")
            .color(0xA09CFFD2)
            .core(0xC0F4FFE8)
            .width(0.25f)
            .taper(SlashFx.Taper.COMET);

    private static void stepThere(Player player, ServerLevel sl) {
        if (Root.has(player, RootRestriction.TELEPORT)) {
            player.sendSystemMessage(Component.literal("You are held in place."));
            return;
        }

        // at what the crosshair is on, as near to it as the player fits. Never inside a block
        Vec3 there = Spells.blinkSpot(player, sl, STEP_RANGE);

        if (there == null) {
            player.sendSystemMessage(Component.literal("There is no room for you there."));
            return;                                                  // nothing spent
        }

        if (!Spells.payEssence(player, STEP_COST)) return;

        Vec3 left = player.position();

        if (player.isPassenger()) player.stopRiding();

        player.teleportTo(there.x, there.y, there.z);
        player.fallDistance = 0;

        FallGuard.protect(player, 200);                              // it may end in the air: the way down does no harm

        for (Vec3 end : new Vec3[]{left, there}) {
            ParticleShapes.burst(sl, PALE_SPARK, end.add(0, 1.0, 0), 18, 0.05, 0.3);
            PaleEmperorFx.featherFall(sl, end, 0.9, 6);
            Spells.sound(sl, end, SoundEvents.ENDERMAN_TELEPORT, 0.7f, 0.6f);
        }

        ParticleShapes.slashBetween(sl, STEP_STREAK.lifetime(8).sweep(3), left.add(0, 1.0, 0), there.add(0, 1.0, 0));

        // the air closes where they were and opens where they are
        SpellFx.closingRing(sl, left, 2.0, 0xD09CFFD2, 8, 0);
        SpellFx.shockRing(sl, there, 2.5, 0xD09CFFD2, 8, 0);
        SpellFx.risingRings(sl, there, 1.2, 2.0, 3, 0xB0F5C211);
        ParticleShapes.burst(sl, GOLD_SPARK, there.add(0, 1.0, 0), 10, 0.04, 0.22);
    }

    // =====================================================================================
    // WRAITHS AND THE UNDERWORLD (ability 7)
    // =====================================================================================
    //
    // Normal cast: wraiths rise around the caster, three for every enemy within WRAITH_RADIUS, fly at the
    //   enemies and slam into them. Each slam hurts and lays a curse: the cursed wither and take more damage.
    // Sneak cast: the caster steps into the underworld. Nothing sees or hurts them there. They come back
    //   when they deal damage, cast it again, or run out of soul essence.
    //
    // Built from the kit: SpellFx.homing (a model that cannot miss), status/Vulnerable (the curse),
    // status/Concealment (the underworld).

    private static final float WRAITHS_COST = 5000;
    private static final float UNDERWORLD_COST = 3000;
    private static final int WRAITHS_STAGE = 5;

    /** Enemies this near are counted, each of them gets this many wraiths. */
    public static final double WRAITH_RADIUS = 12;
    public static final int WRAITHS_PER_ENEMY = 3;

    /** Only the nearest this many enemies are counted, so a crowd does not bring a hundred wraiths. */
    public static final int WRAITH_MAX_ENEMIES = 10;

    /** What one wraith does when it slams into its enemy. */
    public static final float WRAITH_DAMAGE = 12f;

    /** The curse: how long it lasts, how much more damage each one makes it take, and the most that can add up to. */
    public static final int CURSE_TICKS = 200;
    public static final float CURSE_DAMAGE = 0.10f;
    public static final float CURSE_DAMAGE_MAX = 0.60f;

    /** Soul essence the underworld takes each second. With too little left the caster is thrown back. 0 = it is free. */
    public static final float UNDERWORLD_DRAIN = 100f;

    /** Set while the player is in the underworld. Saved with the player. */
    private static final String IN_UNDERWORLD = "pale_emperor_underworld";

    public static void paleEmperorWraiths(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!canUseCharacteristic(player, bypassClassCheck)) return;
        if (SoulCore.getAscensionStage(player) < WRAITHS_STAGE) return;

        // =====================================================================
        // SHIFT CAST - INTO THE UNDERWORLD, OR BACK OUT OF IT
        // =====================================================================

        if (player.isShiftKeyDown()) {

            if (inUnderworld(player)) {
                leaveUnderworld(player, sl);                        // nothing spent
                return;
            }

            if (!Spells.payEssence(player, UNDERWORLD_COST)) return;

            enterUnderworld(player, sl);
            return;
        }

        // =====================================================================
        // NORMAL CAST - WRAITHS
        // =====================================================================

        Vec3 middle = player.getBoundingBox().getCenter();

        List<LivingEntity> enemies = Spells.enemiesAround(player, sl, middle, WRAITH_RADIUS);

        if (enemies.isEmpty()) {
            player.sendSystemMessage(Component.literal("There is no one near for the dead to take."));
            return;                                                 // nothing spent
        }

        if (!Spells.payEssence(player, WRAITHS_COST)) return;

        enemies.sort(Comparator.comparingDouble(
                (LivingEntity enemy) -> enemy.getBoundingBox().getCenter().distanceToSqr(middle)
        ));

        if (enemies.size() > WRAITH_MAX_ENEMIES) {
            enemies = new ArrayList<>(enemies.subList(0, WRAITH_MAX_ENEMIES));
        }

        summonWraiths(player, sl, enemies);
    }

    // =====================================================================================
    // WRAITHS (normal cast)
    // =====================================================================================

    /** How long a wraith takes to rise, and how fast it flies at its enemy after that (blocks per tick). */
    private static final int WRAITH_RISES = 12;
    private static final double WRAITH_SPEED = 0.9;

    /** All the wraiths of one cast rise within this many ticks, one after another. */
    private static final int WRAITHS_SPREAD = 20;

    /** The wraith: 3 blocks tall, its pivot under it, its face its front. Frame 0 = arms hanging, 2 = reaching out. */
    private static ModelFx wraithModel() {
        return ModelFx.of("pale_emperor/wraith").frames(3).smooth()
                .scale(1.1f).pivot(8, -16, 8).glow();
    }

    /** It rises out of the ground as it appears. It fades out at the end, while the flying one fades in. */
    private static final ModelFx WRAITH_RISING = wraithModel()
            .lifetime(WRAITH_RISES + 4).fade(0, 4)
            .key(0, pose().up(-0.5f).alpha(0f))
            .key(WRAITH_RISES, pose().up(0.35f).alpha(1f), ModelFx.Ease.OUT)
            .during(WRAITH_RISES / 2, WRAITH_RISES, pose().frame(1), ModelFx.Ease.OUT);       // its arms begin to lift

    /** Three wraiths for each enemy, in a ring around the caster, rising one after another. */
    private static void summonWraiths(Player player, ServerLevel sl, List<LivingEntity> enemies) {
        int total = enemies.size() * WRAITHS_PER_ENEMY;

        Vec3 feet = player.position();
        float yaw = player.getYRot();

        for (int i = 0; i < total; i++) {

            // going round the enemies again and again: each gets its first wraith before any gets its second
            LivingEntity enemy = enemies.get(i % enemies.size());

            Vec3 spot = Spells.spotAround(feet, yaw + i * 360f / total, 2.2 + (i % 3) * 0.8);

            // on the ground there. A caster high in the air gets them at the height of their feet
            Vec3 ground = Spells.groundAt(player, sl, spot);
            Vec3 rises = Math.abs(ground.y - feet.y) <= 3.0 ? ground : spot;

            Later.run(
                    sl,
                    i * WRAITHS_SPREAD / total,
                    () -> wraithRises(player, sl, rises, enemy)
            );
        }

        seal(sl, feet, yaw, 9f, WRAITHS_SPREAD + WRAITH_RISES + 20);

        Spells.sound(sl, feet, SoundEvents.WITHER_SPAWN, 0.5f, 1.6f);
    }

    private static void wraithRises(Player player, ServerLevel sl, Vec3 ground, LivingEntity enemy) {
        if (!Spells.casterStillHere(player, sl)) return;

        Vec3 toEnemy = enemy.position().subtract(ground);

        // it faces its enemy (one standing right on the spot has no direction: the way the caster looks, then)
        float yaw = toEnemy.x * toEnemy.x + toEnemy.z * toEnemy.z < 0.01 ? player.getYRot() : Spells.yawOf(toEnemy);

        ParticleShapes.model(sl, WRAITH_RISING, ground, yaw, 0f, 0f);

        // the ground smokes where it comes out
        ParticleShapes.burst(sl, LAND_MIST, ground.add(0, 0.3, 0), 4, 0.02, 0.08);

        // where it hangs when it has risen: that is where it flies from
        Vec3 hangs = ground.add(0, 0.35, 0);

        Later.run(
                sl,
                WRAITH_RISES,
                () -> wraithFlies(player, sl, hangs, enemy)
        );
    }

    /** The wraith goes for its enemy and cannot miss it (SpellFx.homing). */
    private static void wraithFlies(Player player, ServerLevel sl, Vec3 from, LivingEntity wanted) {
        if (!Spells.casterStillHere(player, sl)) return;

        // its enemy may be dead by now (another wraith was faster): it takes the nearest one that is left
        LivingEntity enemy = wanted;

        if (!isPrey(player, sl, enemy)) {
            enemy = null;

            double nearest = Double.MAX_VALUE;

            for (LivingEntity other : Spells.enemiesAround(player, sl, from, WRAITH_RADIUS)) {
                double distance = other.position().distanceToSqr(from);
                if (distance < nearest) {
                    nearest = distance;
                    enemy = other;
                }
            }

            if (enemy == null) return;                              // no one left: it just fades
        }

        int flight = (int) Math.max(6, Math.min(18, Math.round(enemy.position().distanceTo(from) / WRAITH_SPEED)));

        ModelFx flying = wraithModel()
                .lifetime(flight + 6).fade(3, 6)
                .key(0, pose().frame(1))
                .during(0, Math.max(2, flight / 2), pose().frame(2), ModelFx.Ease.OUT)        // arms out
                .during(flight, flight + 6, pose().forward(1.0f));                            // on through it as it fades

        SpellFx.homing(sl, flying, from, enemy, flight);

        Spells.sound(sl, from, SoundEvents.WITHER_SHOOT, 0.25f, 1.6f);

        LivingEntity prey = enemy;

        Later.run(
                sl,
                flight,
                () -> wraithSlams(player, sl, prey)
        );
    }

    private static void wraithSlams(Player player, ServerLevel sl, LivingEntity enemy) {
        if (!Spells.casterStillHere(player, sl)) return;
        if (!isPrey(player, sl, enemy)) return;

        Vec3 middle = enemy.getBoundingBox().getCenter();

        Spells.strike(player, enemy, WRAITH_DAMAGE);

        ParticleShapes.burst(sl, PALE_SPARK, middle, 12, 0.05, 0.3);
        ParticleShapes.burst(sl, SpellFx.FIRE_SMOKE, middle, 4, 0.02, 0.08);
        SpellFx.shockRing(sl, enemy.position(), enemy.getBbWidth() * 0.5 + 1.1, 0xC0B48CFF, 6, 0);
        Spells.sound(sl, middle, SoundEvents.WITHER_HURT, 0.5f, 1.4f);

        if (enemy.isAlive()) curse(sl, enemy);
    }

    private static boolean isPrey(Player player, ServerLevel sl, LivingEntity enemy) {
        return enemy.level() == sl && !enemy.isRemoved() && Spells.isEnemy(player, enemy);
    }

    /** One more curse: it takes more damage from everything, and withers, for CURSE_TICKS from now. */
    private static void curse(ServerLevel sl, LivingEntity enemy) {
        float more = Vulnerable.add(enemy, CURSE_DAMAGE, CURSE_DAMAGE_MAX, CURSE_TICKS);

        enemy.addEffect(new MobEffectInstance(MobEffects.WITHER, CURSE_TICKS, 0));

        // a violet mark under it for as long as it is cursed, wider and brighter the more curses lie on it
        float part = Math.max(0f, Math.min(1f, more / CURSE_DAMAGE_MAX));
        int strength = 0x60 + (int) (0x80 * part);

        SpellFx.circleUnder(
                sl,
                enemy,
                enemy.getBbWidth() * 0.5 + 0.4 + 0.6 * part,
                (strength << 24) | 0xB48CFF,
                CURSE_TICKS,
                WRAITH_CURSE_MARK
        );
    }

    /** The name of the mark under those the wraiths cursed. */
    private static final String WRAITH_CURSE_MARK = "pale_emperor_wraith_curse";

    // =====================================================================================
    // THE UNDERWORLD (sneak cast)
    // =====================================================================================

    /** True while the player is in the underworld of this ability. */
    public static boolean inUnderworld(Player player) {
        return player.getPersistentData().getBoolean(IN_UNDERWORLD);
    }

    private static void enterUnderworld(Player player, ServerLevel sl) {
        Vec3 feet = player.position();

        seal(sl, feet, player.getYRot(), 4f, 30);
        SpellFx.closingRing(sl, feet, 3.0, 0xE09CFFD2, 10, 0);
        Spells.sound(sl, feet, SoundEvents.WITHER_AMBIENT, 0.8f, 0.6f);

        // the arms of the dead take them down, and the mist closes over the place
        PaleEmperorFx.underworldArms(sl, Spells.groundUnder(player, sl, player), player.getYRot(), 5);
        ParticleShapes.ring(sl, LAND_MIST, feet.add(0, 0.3, 0), 2.5, 14, -0.15);
        ParticleShapes.burst(sl, PALE_SPARK, player.getBoundingBox().getCenter(), 14, 0.03, 0.2);

        player.getPersistentData().putBoolean(IN_UNDERWORLD, true);

        // unseen, untargeted, unhurt. It is kept up in underworldTick for as long as the player stays
        Concealment.hide(player, 100);

        player.sendSystemMessage(Component.literal("You step into the underworld. Deal damage or cast again to return."));
    }

    private static void leaveUnderworld(Player player, ServerLevel sl) {
        player.getPersistentData().remove(IN_UNDERWORLD);

        Concealment.reveal(player);                                  // (does nothing if they are back already)

        Vec3 feet = player.position();

        seal(sl, feet, player.getYRot(), 4f, 30);
        SpellFx.shockRing(sl, feet, 3.0, 0xE09CFFD2, 10, 0);
        Spells.sound(sl, feet, SoundEvents.WITHER_AMBIENT, 0.8f, 1.2f);

        // they come up out of it
        SpellFx.risingRings(sl, feet, 1.4, 2.2, 3, 0xC09CFFD2);
        ParticleShapes.burst(sl, LAND_MIST, feet.add(0, 0.3, 0), 10, 0.03, 0.14);
        ParticleShapes.burst(sl, PALE_SPARK, player.getBoundingBox().getCenter(), 18, 0.04, 0.25);
        PaleEmperorFx.featherFall(sl, feet, 1.2, 5);

        player.sendSystemMessage(Component.literal("You return from the underworld."));
    }

    /** One tick in the underworld: keeps the player hidden, takes its price, and notices when they are out of it. */
    private static void underworldTick(Player player, ServerLevel sl) {
        if (!inUnderworld(player)) return;

        // something else brought them back (a normal attack does): tidy up
        if (!Concealment.isHidden(player)) {
            leaveUnderworld(player, sl);
            return;
        }

        if (Concealment.ticksLeft(player) < 40) Concealment.hide(player, 100);

        if (UNDERWORLD_DRAIN > 0 && player.tickCount % 20 == 0 && !player.isCreative()) {
            if (SoulCore.getSoulEssence(player) < UNDERWORLD_DRAIN) {
                player.sendSystemMessage(Component.literal("Your soul essence is spent."));
                leaveUnderworld(player, sl);
                return;
            }
            SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - UNDERWORLD_DRAIN);
        }
    }

    // =====================================================================================
    // EVENTS
    // =====================================================================================

    @Mod.EventBusSubscriber(modid = ForgeRealm.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class PaleEmperorEvents {

        /**
         * What goes with the wings of ability 6, the underworld of ability 7 is kept up, and the second life
         * of ability 2 is watched (all run on even if the aspect is taken away meanwhile).
         */
        @SubscribeEvent
        public static void onPaleEmperorPlayerTick(TickEvent.PlayerTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;

            Player player = event.player;

            if (!(player.level() instanceof ServerLevel sl)) return;

            wingsTick(player, sl);
            underworldTick(player, sl);
            secondLifeTick(player, sl);
        }

        /** Whoever deals damage is back from the underworld. */
        @SubscribeEvent
        public static void onPaleEmperorHurt(LivingHurtEvent event) {
            LivingEntity victim = event.getEntity();

            if (!(victim.level() instanceof ServerLevel sl)) return;

            if (event.getSource().getEntity() instanceof Player attacker
                    && attacker != victim
                    && event.getAmount() > 0
                    && inUnderworld(attacker)) {
                leaveUnderworld(attacker, sl);
            }
        }

        // Models are forgotten by a player's game when they log in or change dimension, so the wings of a
        // flight that is still going are put on again, and so is the mark of a second life that still waits.
        // A second later: by then their game knows where they are.

        @SubscribeEvent
        public static void onPaleEmperorLogin(PlayerEvent.PlayerLoggedInEvent event) {
            wingsAgain(event.getEntity());
        }

        @SubscribeEvent
        public static void onPaleEmperorChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
            wingsAgain(event.getEntity());
            secondLifeAgain(event.getEntity());
        }

        private static void secondLifeAgain(Player player) {
            if (!player.getPersistentData().getBoolean(REVIVE_ON) || !(player.level() instanceof ServerLevel sl)) return;

            Later.run(sl, 20, () -> {
                if (player.isAlive() && !player.isRemoved() && player.level() == sl && DeathWard.isArmed(player)) {
                    secondLifeShows(player, sl, DeathWard.ticksLeft(player));
                }
            });
        }

        private static void wingsAgain(Player player) {
            if (!hasWings(player) || !(player.level() instanceof ServerLevel sl)) return;

            Later.run(sl, 20, () -> {
                if (player.isAlive() && !player.isRemoved() && player.level() == sl && hasWings(player)) {
                    PaleEmperorFx.wings(sl, player, Flight.ticksLeft(player), WINGS_TAG);
                }
            });
        }
    }

}
