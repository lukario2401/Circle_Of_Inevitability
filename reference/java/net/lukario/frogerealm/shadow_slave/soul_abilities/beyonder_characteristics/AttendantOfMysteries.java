package net.lukario.frogerealm.shadow_slave.soul_abilities.beyonder_characteristics;

import net.lukario.frogerealm.ForgeRealm;
import net.lukario.frogerealm.combat.Countdown;
import net.lukario.frogerealm.combat.Later;
import net.lukario.frogerealm.combat.MeleeCombo;
import net.lukario.frogerealm.combat.Shot;
import net.lukario.frogerealm.combat.SpellFx;
import net.lukario.frogerealm.combat.Spells;
import net.lukario.frogerealm.particles.fx.ModelFx;
import net.lukario.frogerealm.particles.fx.ParticleFx;
import net.lukario.frogerealm.particles.fx.ParticleShapes;
import net.lukario.frogerealm.particles.fx.SlashFx;
import net.lukario.frogerealm.root.Root;
import net.lukario.frogerealm.root.RootRestriction;
import net.lukario.frogerealm.screen.ScreenAnchor;
import net.lukario.frogerealm.screen.ScreenImages;
import net.lukario.frogerealm.shadow_slave.soul_shards.SoulCore;
import net.lukario.frogerealm.status.Concealment;
import net.lukario.frogerealm.status.DamageLink;
import net.lukario.frogerealm.status.DeathWard;
import net.lukario.frogerealm.status.FallGuard;
import net.lukario.frogerealm.status.Marionette;
import net.lukario.frogerealm.status.Substitute;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Attendant Of Mysteries: the Seer pathway of Lord of the Mysteries. Each key is one step up the pathway,
 * and every ability has a normal cast and a sneak cast:
 *
 *   1 Paper Daggers        (Clown)            a fan of paper daggers; keep throwing and more follow        | sneak: a wide fan at close range
 *   2 Flaming Jump         (Magician)         leap through fire to the flame you look at, or light one     | sneak: Flame Control, conjure a flame
 *   3 Damage Transfer      (Magician)         half of what hits you lands on your target instead           | sneak: Paper Figurine Substitute
 *   4 Air Cannon           (Magician)         a shell of air that bursts on what it hits, and its recoil   | sneak: fired at your feet, it launches you
 *   5 Spirit Body Threads  (Marionettist)     5, 4, 3, 2, 1... and the target is your marionette           | sneak: the same on everything around you
 *   6 Historical Void      (Scholar of Yore)  a projection of your past self repeats your spells           | sneak: hide in the fog of history
 *   7 Grafting             (Attendant of Mysteries) a needle stitches the enemies in an area together      | sneak: Miracle (Miracle Invoker)
 *
 * Every ability you cast adds a stack (the pictures next to the hotbar); the fifth one bursts around you.
 *
 * What is reusable lives outside this class, so other classes can use it too (see docs/SPELL_KIT.md):
 *   combat/Spells (aiming, enemies), combat/SpellFx (rings, circles, tethers), combat/Shot (flying models),
 *   combat/Countdown, and the statuses in status/: DamageLink, Substitute, Marionette, Concealment, DeathWard, FallGuard.
 *
 * Nothing here breaks blocks or lights real fire. The numbers for every ability are right below;
 * the 3D models are in models/model_fx/ (see docs/MODEL_FX.md).
 */
public class AttendantOfMysteries {

    private static final String ASPECT = "Attendant Of Mysteries";

    // =========================
    // Ability settings (cost = soul essence, ticks: 20 = 1 second, distances in blocks)
    // =========================

    // Stacks: every ability used adds one picture next to the hotbar, the 5th one explodes
    private static final int MAX_STACKS = 5;
    private static final float STACK_BURST_DAMAGE = 40f;
    private static final double STACK_BURST_RADIUS = 8.0;
    // Pictures for the stacks, from assets/forgerealmmod/textures/gui/ (without .png). One is picked at random.
    private static final List<String> STACK_IMAGES = List.of(
            "attendant_stack_orb"
    );

    // Ability 1 - Paper Daggers (ascension stage 0)
    private static final int DAGGER_COST = 250;
    private static final int DAGGER_COUNT = 3;                   // an odd number, so one flies straight at the crosshair
    private static final double DAGGER_FAN = 30.0;               // degrees between the outermost daggers
    private static final double DAGGER_RANGE = 32.0;
    private static final float DAGGER_DAMAGE = 32f;              // each
    private static final double DAGGER_SPEED = 3.0;              // blocks per tick
    private static final int DAGGER_STICK_TICKS = 40;            // how long one stays stuck in a block
    private static final int JUGGLE_WINDOW = 60;                 // throw again within this many ticks to add a dagger
    private static final int JUGGLE_MAX_EXTRA = 3;               // 3 -> 4 -> 5 -> 6 daggers
    private static final float JUGGLE_DAMAGE = 16f;              // each extra dagger (they all fly at the same target)
    private static final int JUGGLE_STAGGER = 2;                 // ticks between the extra daggers: they follow each other
    private static final int WIDE_FAN_COUNT = 7;                 // sneak (odd as well)
    private static final double WIDE_FAN = 90.0;
    private static final double WIDE_FAN_RANGE = 12.0;
    private static final float WIDE_FAN_DAMAGE = 12f;

    // Ability 2 - Flaming Jump (stage 1)
    private static final int FLAME_COST = 1250;
    private static final double JUMP_RANGE = 32.0;               // how far away a flame can be to jump to it
    private static final double JUMP_AIM = 15.0;                 // degrees: a flame this close to your crosshair is "the one you look at"
    private static final double JUMP_NEW_FLAME_RANGE = 20.0;     // no flame there: one is lit where you aim, this far at most
    private static final int REAL_FIRE_REACH = 24;               // how far real fire and campfires are looked for, sideways
    private static final int REAL_FIRE_HEIGHT = 10;              // ... and up/down
    private static final double JUMP_BURST_RADIUS = 3.5;
    private static final float JUMP_BURST_DAMAGE = 18f;          // where you leave and where you arrive
    private static final int JUMP_BURN_TICKS = 100;
    private static final double FLAME_RANGE = 24.0;              // sneak: how far away you can conjure a flame
    private static final int FLAME_TICKS = 300;                  // how long a conjured flame stays (and can be jumped to)
    private static final double FLAME_RADIUS = 1.8;
    private static final float FLAME_HEIGHT = 2.6f;
    private static final int FLAME_PULSE = 20;                   // ticks between burns
    private static final float FLAME_DAMAGE = 4f;                // per burn

    // Ability 3 - Damage Transfer (stage 2)
    private static final int TRANSFER_COST = 1250;
    private static final double TRANSFER_RANGE = 16.0;
    private static final int TRANSFER_TICKS = 100;
    private static final float TRANSFER_SHARE = 0.5f;            // part of every hit on you that goes to the target instead
    private static final int SUBSTITUTE_TICKS = 200;             // sneak: how long the paper figurine waits for a hit
    private static final double SUBSTITUTE_ESCAPE = 5.0;         // how far you slip away
    private static final double SUBSTITUTE_SWAP_RANGE = 16.0;    // a marionette this close takes your place instead
    private static final int SUBSTITUTE_DAGGERS = 3;             // the torn figurine throws these back at the attacker
    private static final float SUBSTITUTE_DAGGER_DAMAGE = 12f;

    // Ability 4 - Air Cannon (stage 3)
    private static final int CANNON_COST = 2450;
    private static final double CANNON_RANGE = 24.0;
    private static final double CANNON_SPEED = 4.0;              // blocks per tick
    private static final float CANNON_DAMAGE = 64f;              // on what it hits
    private static final double CANNON_BLAST_RADIUS = 3.0;
    private static final float CANNON_SPLASH = 0.5f;             // part of the damage for everything else in the blast
    private static final double CANNON_RECOIL = 1.8;
    private static final double LEAP_SPEED = 3.9;                // sneak: how hard you are thrown upward. 3.9 is the most the game
                                                                 // allows (about 60 blocks up, as before); 1.9 is about 18 blocks
    private static final double LEAP_BLAST_RADIUS = 5.0;
    private static final float LEAP_DAMAGE = 20f;
    private static final int LEAP_GUARD_TICKS = 400;             // no fall damage until you land, for at most this long

    // Ability 5 - Spirit Body Threads (stage 4)
    private static final int THREADS_COST = 3000;
    private static final int COUNTDOWN_SECONDS = 5;
    private static final int COUNTDOWN_COLOR = 0xE8C872;         // pale gold
    private static final int COUNTDOWN_LAST_COLOR = 0xD83A4A;    // the final "1"
    private static final String SNEAK_IMAGE = "attendant_of_mysteries_sneak"; // textures/gui/attendant_of_mysteries_sneak.png
    private static final double THREADS_RANGE = 24.0;
    private static final double THREADS_LEASH = 32.0;            // the target gets further away than this: the threads slip
    private static final int MARIONETTE_TICKS = 600;             // how long a marionette serves
    private static final float MARIONETTE_MAX_HEALTH = 150f;     // more max health than this (bosses), or a player: held instead
    private static final int BIND_TICKS = 80;
    private static final float BIND_DAMAGE = 30f;
    private static final double WEB_RADIUS = 10.0;               // sneak: everything this close is caught
    private static final int WEB_MAX_TARGETS = 12;
    private static final double WEB_LEASH = 16.0;                // whoever got further away than this by the end is free
    private static final float WEB_DAMAGE = 20f;
    private static final int WEB_BIND_TICKS = 80;

    // Ability 6 - Historical Void (stage 5)
    private static final int HISTORY_COST = 6000;
    private static final int PROJECTION_TICKS = 300;             // how long your past self stays
    private static final float PROJECTION_POWER = 0.5f;          // how hard its copies of your spells hit
    private static final int PROJECTION_DELAY = 8;               // ticks after your cast that it repeats it (Air Cannon: at once,
                                                                 // a later blast would only find what the first threw away)
    private static final double PROJECTION_SIDE = 1.4;           // how far beside you it stands
    private static final int FOG_TICKS = 80;                     // sneak: how long you stay hidden
    private static final int FOG_SPEED_LEVEL = 1;                // 0 = Speed I, 1 = Speed II while hidden

    // Ability 7 - Grafting (stage 6)
    private static final int GRAFT_COST = 12000;
    private static final double GRAFT_RANGE = 28.0;
    private static final double GRAFT_RADIUS = 8.0;
    private static final float GRAFT_DAMAGE = 45f;
    private static final int GRAFT_WARNING = 20;                 // ticks the needle hangs in the air first
    private static final int NEEDLE_FALL = 5;                    // ticks the fall takes
    private static final float NEEDLE_HANG = 9f;                 // how high its point hangs above the spot
    private static final int GRAFT_PULL_TICKS = 8;               // how long the thread drags everything to the needle
    private static final double GRAFT_PULL_SPEED = 1.5;          // blocks per tick, at most
    private static final int GRAFT_TICKS = 200;                  // how long they stay stitched together
    private static final float GRAFT_SHARE = 0.5f;               // part of one's damage that each of the others takes
    private static final int MIRACLE_TICKS = 1200;               // sneak: how long the miracle waits for your death
    private static final float MIRACLE_HEALTH = 0.5f;            // the health you come back with (part of your maximum)
    private static final double MIRACLE_PUSH_RADIUS = 6.0;
    private static final int MIRACLE_FOG_TICKS = 40;             // you come back hidden for this long

    // =========================
    // Saved on the player
    // =========================
    private static final String STACKS = "attendant_of_mysteries_stacks";
    private static final String STACK_IMAGE_KEY = "attendant_of_mysteries_stack_image_"; // + slot number
    private static final String STACK_IMAGE_ID = "aom_stack_";                           // screen image ids
    private static final String JUGGLE_EXTRA = "attendant_of_mysteries_juggle_extra";
    private static final String JUGGLE_LEFT = "attendant_of_mysteries_juggle_ticks";
    private static final String PROJECTION_LEFT = "attendant_of_mysteries_projection_ticks";
    private static final String PROJECTION_X = "attendant_of_mysteries_projection_x";   // where it stands, from you
    private static final String PROJECTION_Z = "attendant_of_mysteries_projection_z";

    // Small pictures above the stacks that show what is active. Files: textures/gui/attendant_of_mysteries/<name>.png
    private static final String STATUS_SUBSTITUTE = "aom_status_substitute";
    private static final String STATUS_PROJECTION = "aom_status_projection";
    private static final String STATUS_MIRACLE = "aom_status_miracle";
    private static final String STATUS_TRANSFER = "aom_status_transfer";

    // =========================
    // 3D models (models/model_fx/..., textures in textures/model_fx/...)
    // =========================
    private static final String MODELS = "attendant_of_mysteries/";
    private static final float DAGGER_SIZE = 1.0f;
    private static final double DAGGER_TIP = (20 - 10) / 16.0 * DAGGER_SIZE;      // middle of the dagger -> its tip, in blocks
    private static final ModelFx PAPER_DAGGER = ModelFx.of(MODELS + "paper_dagger")
            .scale(DAGGER_SIZE).pivot(8, 10, 8)                   // tip up, turns around its middle
            .glow().aura(0x60FFF2D0, 0.04f, 2).spin(34f);         // whirls around its own length as it flies
    private static final ModelFx PAPER_FIGURINE = ModelFx.of(MODELS + "paper_figurine")   // a paper doll, 2 blocks tall
            .glow().aura(0x50FFF2D0, 0.05f, 2);
    private static final ModelFx AIR_SHELL = ModelFx.of("orb")    // the shared white orb, tinted
            .pivot(8, 8, 8).glow().unshaded().seeThrough()
            .color(0xD0E4F4FF).spin(30f);
    private static final ModelFx PROJECTION = ModelFx.of(MODELS + "projection")       // a grey figure in a top hat
            .pivot(8, -6, 8).glow().unshaded().seeThrough()
            .color(0xC0C4CCDC).tag("aom_projection");
    private static final float NEEDLE_SIZE = 4.5f;                // 1 = the size it has in Blockbench (3 blocks tall)
    private static final ModelFx NEEDLE = ModelFx.of(MODELS + "needle")
            .scale(NEEDLE_SIZE).pivot(8, -16, 8)                  // hangs point down, turns around its point
            .glow().aura(0xB0D8C0FF, 0.2f, 3);                    // not full strength: the shaft is two cubes in one, the glow adds up
    private static final ModelFx THREAD = ModelFx.of(MODELS + "thread")   // a taut thread, 3 blocks long, standing up
            .pivot(8, -16, 8).glow().unshaded().seeThrough();
    private static final ModelFx FOOL_CARD = ModelFx.of(MODELS + "fool_card")
            .pivot(8, 8, 8).glow().aura(0x90FFE9A8, 0.08f, 3);

    // =========================
    // Small particles and trails that go with them
    // =========================
    private static final ParticleFx PAPER_SCRAP = ParticleFx.of("fx/paper")
            .color(0xFFFFF8E8).endColor(0x00E0D4B8)
            .size(0.1f).endSize(0.07f).sizeRandom(0.45f)
            .lifetime(24, 14).gravity(0.3f).friction(0.9f)
            .collide().spin(16f).randomRotation();
    private static final ParticleFx AIR_PUFF = ParticleFx.of("fx/smoke")
            .color(0x80E8F4FF).endColor(0x00C8DCF0)
            .size(0.3f).endSize(0.9f).sizeRandom(0.3f)
            .lifetime(14, 8).friction(0.86f)
            .spin(3f).randomRotation();
    private static final ParticleFx THREAD_MOTE = ParticleFx.of("fx/glow")
            .color(0xFFD8C8FF).endColor(0x00483070)
            .size(0.07f).endSize(0.02f)
            .lifetime(16, 8).friction(0.9f)
            .glow();
    private static final ParticleFx HISTORY_FOG = ParticleFx.of("fx/smoke")
            .color(0xB0C4C8D2).endColor(0x00808490)
            .size(0.5f).endSize(1.3f).sizeRandom(0.35f)
            .lifetime(30, 16).gravity(-0.01f).friction(0.9f)
            .spin(2f).randomRotation();
    private static final ParticleFx GOLD_MOTE = ParticleFx.of("fx/spark")
            .color(0xFFFFE9A8).endColor(0x00FFB040)
            .size(0.12f).endSize(0.03f).sizeRandom(0.4f)
            .lifetime(26, 14).gravity(-0.04f).friction(0.92f)
            .glow().spin(12f).randomRotation();
    private static final ParticleFx STACK_SPARK = ParticleFx.of("fx/glow")
            .color(0xFFFF5A6A).endColor(0x00801020)
            .size(0.12f).endSize(0.03f).sizeRandom(0.4f)
            .lifetime(18, 10).friction(0.9f)
            .glow();

    private static final SlashFx DAGGER_STREAK = SlashFx.line("slash/smooth")      // the streak behind a thrown dagger
            .color(0xA0FFF4DC).core(0xE0FFFFFF)
            .width(0.12f).taper(SlashFx.Taper.COMET);
    private static final SlashFx AIR_STREAK = SlashFx.line("slash/smooth")         // the wake of the air shell
            .color(0x70DCEEFF).core(0xC0FFFFFF)
            .width(0.5f).taper(SlashFx.Taper.COMET);
    // Spirit Body Threads: dark with a pale core, so they show against the sky as well as in a cave
    private static final SlashFx SPIRIT_THREAD = SlashFx.line("slash/smooth")
            .translucent().color(0xD0281C3C).core(0xD0E6DCFF)
            .width(0.07f).taper(SlashFx.Taper.UNIFORM);
    private static final SlashFx TRANSFER_THREAD = SlashFx.line("slash/smooth")    // the red line damage travels along
            .color(0xC0FF4A5A).core(0xE0FFE0E0)
            .width(0.06f).taper(SlashFx.Taper.UNIFORM);
    private static final SlashFx STITCH = SlashFx.line("slash/dashed")             // the golden seam of Grafting
            .color(0xF0FFD890)
            .width(0.16f).taper(SlashFx.Taper.UNIFORM);

    /** A paper dagger in flight. What it does when it hits is added per throw (see throwDagger). */
    private static final Shot DAGGER_SHOT = Shot.of(PAPER_DAGGER)
            .speed(DAGGER_SPEED).width(0.35).tip(DAGGER_TIP)
            .trail(DAGGER_STREAK).stick(DAGGER_STICK_TICKS);
    private static final Shot AIR_SHOT = Shot.of(AIR_SHELL)
            .speed(CANNON_SPEED).range(CANNON_RANGE).width(0.5)
            .trail(AIR_STREAK);

    /** The flames you conjured: where they are and until when they can be jumped to. */
    private record SpiritFlame(ServerLevel level, UUID owner, Vec3 spot, long expires) {}

    private static final List<SpiritFlame> FLAMES = new ArrayList<>();

    // =========================
    // Melee combo (hitting mobs as Attendant Of Mysteries), registered in combat/MeleeCombos:
    // 1 = ring sweeping around you, 2 = tilted ring around you, 3 = ground spiral around you OR crystal X on the target
    // =========================
    private static final int COMBO_HEAD = 0xF2F4F2C9;   // cream front
    private static final int COMBO_MID = 0xEE52E3C4;    // mint cyan middle
    private static final int COMBO_TAIL = 0xA05B3B33;   // dark brown fading tail

    private static final SlashFx COMBO_RING = SlashFx.arc("slash/band")
            .color(COMBO_MID).tailColor(COMBO_TAIL).headColor(COMBO_HEAD)
            .translucent()
            .radius(2.6f).arc(320f).width(1.1f).taper(SlashFx.Taper.COMET)
            .layers(3).spread(0.28f)
            .lifetime(12).sweep(5).spin(10f);             // draws itself, keeps spinning, then wipes away
    private static final SlashFx COMBO_TILTED_RING = COMBO_RING
            .radius(2.4f).arc(-270f).width(1.2f)          // negative arc = sweeps the other way
            .sweep(4).spin(-8f)
            .rotation(0f, -35f, 30f);                     // big arch over and around you, tilted
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

    public static final MeleeCombo MELEE_COMBO = MeleeCombo.forAspect(ASPECT)
            .step(AttendantOfMysteries::comboRing)
            .step(AttendantOfMysteries::comboTiltedRing)
            .randomStep(AttendantOfMysteries::comboGroundSpiral, AttendantOfMysteries::comboCrystalCross);

    @Mod.EventBusSubscriber(modid = ForgeRealm.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class AttendantOfMysteriesEvents {

        @SubscribeEvent
        public static void onAttendantOfMysteriesTick(TickEvent.PlayerTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            Player player = event.player;
            if (!(player.level() instanceof ServerLevel)) return;

            // timers (they run out even if the aspect is taken away in the meantime)
            CompoundTag data = player.getPersistentData();
            countDown(data, JUGGLE_LEFT);
            countDown(data, PROJECTION_LEFT);

            if (!SoulCore.getAspect(player).equals(ASPECT)) return;

            // Flame Controlling: fire cannot hurt you. (No potion swirls: they would give you away when you hide in history.)
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 60, 1, false, false, true));
            if (player.isOnFire()) {
                player.clearFire();
            }
        }

        // pictures on screen don't survive relogging/respawning, so redraw them from the saved stack count
        @SubscribeEvent
        public static void onAttendantOfMysteriesLogin(PlayerEvent.PlayerLoggedInEvent event) {
            refreshStackImages(event.getEntity());
            // the figure of the projection is not shown again after logging in, so the projection ends too
            event.getEntity().getPersistentData().remove(PROJECTION_LEFT);
        }

        @SubscribeEvent
        public static void onAttendantOfMysteriesRespawn(PlayerEvent.PlayerRespawnEvent event) {
            refreshStackImages(event.getEntity());
        }

        // conjured flames are not saved: a world that closes takes its flames with it
        @SubscribeEvent
        public static void onAttendantOfMysteriesLevelUnload(LevelEvent.Unload event) {
            if (!(event.getLevel() instanceof ServerLevel)) return;     // (the players' side unloads its own copy too)
            FLAMES.removeIf(flame -> flame.level() == event.getLevel());
        }
    }

    // =========================
    // Ability 1 - Paper Daggers
    // =========================
    public static void attendantOfMysteriesPaperDagger(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!ready(player, bypassClassCheck, 0)) return;
        if (!Spells.payEssence(player, DAGGER_COST)) return;
        stepOutOfTheFog(player);

        boolean sneak = player.isShiftKeyDown();
        int extra = sneak ? 0 : juggle(player);
        // from the right hand, but never from inside a wall you stand next to
        Vec3 hand = Spells.clearStart(player, sl, player.getEyePosition().add(Spells.rightOf(player).scale(0.35)).add(0, -0.25, 0));
        paperDaggers(player, sl, hand, sneak, extra, 1f);
        echo(player, sl, PROJECTION_DELAY, () -> paperDaggers(player, sl, projectionHand(player), sneak, extra, PROJECTION_POWER));

        addStack(player, sl);
    }

    /**
     * Juggling: every normal cast within JUGGLE_WINDOW ticks of the one before throws one dagger more
     * (they follow the fan one after another, straight at the crosshair).
     * Returns how many extra daggers this cast gets.
     */
    private static int juggle(Player player) {
        CompoundTag data = player.getPersistentData();
        int extra = data.getInt(JUGGLE_LEFT) > 0 ? Math.min(JUGGLE_MAX_EXTRA, data.getInt(JUGGLE_EXTRA) + 1) : 0;
        data.putInt(JUGGLE_EXTRA, extra);
        data.putInt(JUGGLE_LEFT, JUGGLE_WINDOW);
        return extra;
    }

    /**
     * Normal: a narrow fan that flies far. Sneak: a wide fan at close range. Also used by the projection.
     * Thrown from 'from' toward what the caster's crosshair is on.
     */
    private static void paperDaggers(Player player, ServerLevel sl, Vec3 from, boolean sneak, int extra, float power) {
        int count = sneak ? WIDE_FAN_COUNT : DAGGER_COUNT;
        double fan = sneak ? WIDE_FAN : DAGGER_FAN;
        double range = sneak ? WIDE_FAN_RANGE : DAGGER_RANGE;
        float damage = (sneak ? WIDE_FAN_DAMAGE : DAGGER_DAMAGE) * power;
        Vec3 look = Spells.aimFrom(player, sl, from, range);
        for (int i = 0; i < count; i++) {
            double turn = count == 1 ? 0.0 : ((double) i / (count - 1) - 0.5) * fan;   // spread evenly, left to right
            throwDagger(player, sl, from, Spells.turned(look, turn), range, damage, 0);
        }
        // the juggled ones hang in the air for a moment and follow one after another, straight at the crosshair
        for (int i = 1; i <= extra; i++) {
            throwDagger(player, sl, from, look, range, JUGGLE_DAMAGE * power, i * JUGGLE_STAGGER);
        }
        // the more you juggle, the higher it sings
        Spells.sound(sl, from, SoundEvents.ARROW_SHOOT, 0.9f, (sneak ? 1.2f : 1.5f) + extra * 0.12f);
    }

    /**
     * One paper dagger: it stops in the first enemy it reaches, or stays stuck in the block it hits.
     * 'windup' = ticks it hangs in the air before it leaves (0 = at once).
     */
    private static void throwDagger(Player player, ServerLevel sl, Vec3 from, Vec3 direction, double range, float damage, int windup) {
        DAGGER_SHOT.range(range).windup(windup)
                .onHit((target, at) -> {
                    Spells.strike(player, target, damage);
                    ParticleShapes.burst(sl, PAPER_SCRAP, at, 7, 0.05, 0.2);
                    Spells.sound(sl, at, SoundEvents.PLAYER_ATTACK_CRIT, 0.7f, 1.5f);
                })
                .onEnd((at, hitBlock) -> {
                    if (!hitBlock) return;
                    ParticleShapes.burst(sl, PAPER_SCRAP, at, 4, 0.03, 0.12);
                    Spells.sound(sl, at, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 0.3f, 1.9f);
                })
                .fire(player, sl, from, direction);
    }

    // =========================
    // Ability 2 - Flaming Jump
    // =========================
    public static void attendantOfMysteriesFlamingJump(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!ready(player, bypassClassCheck, 1)) return;

        if (player.isShiftKeyDown()) {
            // Sneak - Flame Control: a flame on the spot you aim at. It burns what stands in it and you can jump to it.
            Vec3 spot = Spells.aimGround(player, sl, FLAME_RANGE);
            if (!onSolidGround(sl, spot)) {
                player.sendSystemMessage(Component.literal("There is nothing there to set alight."));
                return; // nothing spent
            }
            if (!Spells.payEssence(player, FLAME_COST)) return;
            stepOutOfTheFog(player);
            conjureFlame(player, sl, spot);
            Spells.sound(sl, player.position(), SoundEvents.FIRECHARGE_USE, 0.8f, 1.4f);
            echo(player, sl, PROJECTION_DELAY, () -> flameBurst(player, sl, spot, PROJECTION_POWER));
        } else {
            if (Root.has(player, RootRestriction.TELEPORT)) {
                player.sendSystemMessage(Component.literal("You cannot leave this place."));
                return;
            }
            // Normal: to the flame you are looking at. If there is none, one is lit where you aim.
            Vec3 lit = flameToJumpTo(player, sl);
            Vec3 destination = lit != null ? lit : Spells.aimGround(player, sl, JUMP_NEW_FLAME_RANGE);
            if (lit == null && !onSolidGround(sl, destination)) {
                player.sendSystemMessage(Component.literal("There is nothing there to set alight."));
                return; // nothing spent
            }
            if (!Spells.fits(player, sl, destination)) {
                player.sendSystemMessage(Component.literal("There is no room to step out of that flame."));
                return; // nothing spent
            }
            if (!Spells.payEssence(player, FLAME_COST)) return;
            stepOutOfTheFog(player);
            flamingJump(player, sl, destination, lit == null);
            echo(player, sl, PROJECTION_DELAY, () -> flameBurst(player, sl, destination, PROJECTION_POWER));
        }

        addStack(player, sl);
    }

    /** Leaves in a burst of fire and arrives in another. Both ends keep a flame, so you can jump back and forth. */
    private static void flamingJump(Player player, ServerLevel sl, Vec3 destination, boolean lightOneThere) {
        Vec3 left = player.position();
        flameBurst(player, sl, left, 1f);
        if (onSolidGround(sl, left) && flameAt(player, sl, left) == null) conjureFlame(player, sl, left);

        player.teleportTo(destination.x, destination.y, destination.z);
        player.fallDistance = 0;
        if (lightOneThere) conjureFlame(player, sl, destination);
        flameBurst(player, sl, destination, 1f);
        Spells.sound(sl, left, SoundEvents.ENDERMAN_TELEPORT, 0.6f, 0.6f);
        Spells.sound(sl, destination, SoundEvents.BLAZE_SHOOT, 1f, 0.7f);
    }

    /** A burst of fire around a spot on the ground: hurts and sets alight. Also used by the projection. */
    private static void flameBurst(Player player, ServerLevel sl, Vec3 ground, float power) {
        Vec3 center = ground.add(0, 0.9, 0);
        for (LivingEntity target : Spells.enemiesAround(player, sl, center, JUMP_BURST_RADIUS)) {
            Spells.strike(player, target, JUMP_BURST_DAMAGE * power);
            target.setRemainingFireTicks(JUMP_BURN_TICKS);
        }
        SpellFx.fireBurst(sl, center, JUMP_BURST_RADIUS);
        Spells.sound(sl, center, SoundEvents.GENERIC_EXPLODE.value(), 0.7f, 1.4f);
    }

    /** A flame that stays for FLAME_TICKS: it burns whoever stands in it, and Flaming Jump can go to it. */
    private static void conjureFlame(Player player, ServerLevel sl, Vec3 ground) {
        FLAMES.removeIf(flame -> flame.expires() < flame.level().getGameTime());
        FLAMES.add(new SpiritFlame(sl, player.getUUID(), ground, sl.getGameTime() + FLAME_TICKS));

        float width = (float) (FLAME_RADIUS * 1.4);
        ModelFx fire = SpellFx.FLAMES.scale(width, FLAME_HEIGHT / 2f, width)      // the model is 1 block wide and 2 tall
                .lifetime(FLAME_TICKS).fade(0, 14).spin(2.5f)
                .key(0, ModelFx.pose().scale(0.15f))
                .key(7, ModelFx.pose().scale(1f), ModelFx.Ease.OUT_BACK);         // flares up out of the ground
        ParticleShapes.model(sl, fire, ground, sl.getRandom().nextFloat() * 360f, 0f, 0f);
        SpellFx.runeCircle(sl, ground, FLAME_RADIUS, 0xC0FF7A2A, FLAME_TICKS, 2f);
        burnInFlame(player, sl, ground, FLAME_TICKS);
    }

    /** One burn every FLAME_PULSE ticks for as long as the flame lasts. It stops burning when its caster is gone. */
    private static void burnInFlame(Player player, ServerLevel sl, Vec3 ground, int ticksLeft) {
        if (ticksLeft <= FLAME_PULSE) return;
        Later.run(sl, FLAME_PULSE, () -> {
            if (!Spells.casterStillHere(player, sl)) return;
            for (LivingEntity target : Spells.enemiesAround(player, sl, ground.add(0, 0.9, 0), FLAME_RADIUS)) {
                Spells.strike(player, target, FLAME_DAMAGE);
                target.setRemainingFireTicks(60);
            }
            ParticleShapes.cone(sl, SpellFx.FIRE_EMBER, ground.add(0, 0.3, 0), Spells.UP, 50, 5, 0.06, 0.22);
            burnInFlame(player, sl, ground, ticksLeft - FLAME_PULSE);
        });
    }

    /**
     * The flame nearest to the caster's crosshair (within JUMP_AIM degrees of it): one they conjured, real fire
     * or a lit campfire. Returns where to stand in it, or null if they are not looking at any flame.
     */
    private static Vec3 flameToJumpTo(Player player, ServerLevel sl) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        double bestAngle = Math.toRadians(JUMP_AIM);
        Vec3 best = null;

        long now = sl.getGameTime();
        for (SpiritFlame flame : FLAMES) {
            if (flame.level() != sl || flame.expires() < now || !flame.owner().equals(player.getUUID())) continue;
            double angle = aimAngle(eye, look, flame.spot().add(0, 0.9, 0), JUMP_RANGE);
            if (angle < bestAngle) {
                bestAngle = angle;
                best = flame.spot();
            }
        }

        BlockPos middle = player.blockPosition();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -REAL_FIRE_REACH; dx <= REAL_FIRE_REACH; dx++) {
            for (int dy = -REAL_FIRE_HEIGHT; dy <= REAL_FIRE_HEIGHT; dy++) {
                for (int dz = -REAL_FIRE_REACH; dz <= REAL_FIRE_REACH; dz++) {
                    pos.set(middle.getX() + dx, middle.getY() + dy, middle.getZ() + dz);
                    BlockState state = sl.getBlockState(pos);
                    boolean fire = state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE);
                    if (!fire && !CampfireBlock.isLitCampfire(state)) continue;
                    // in a fire block you stand on its floor, on a campfire you stand on top of it
                    Vec3 spot = new Vec3(pos.getX() + 0.5, pos.getY() + (fire ? 0.0 : 0.5), pos.getZ() + 0.5);
                    double angle = aimAngle(eye, look, spot.add(0, 0.5, 0), JUMP_RANGE);
                    if (angle < bestAngle) {
                        bestAngle = angle;
                        best = spot;
                    }
                }
            }
        }
        return best;
    }

    /** Angle (radians) between the look and the way to a point. Huge if the point is too close or too far to count. */
    private static double aimAngle(Vec3 eye, Vec3 look, Vec3 point, double range) {
        Vec3 to = point.subtract(eye);
        double distance = to.length();
        if (distance < 2.0 || distance > range) return Double.MAX_VALUE;
        return Math.acos(Mth.clamp(to.dot(look) / distance, -1.0, 1.0));
    }

    /** The caster's conjured flame on this spot (within a block), or null. */
    private static SpiritFlame flameAt(Player player, ServerLevel sl, Vec3 spot) {
        long now = sl.getGameTime();
        for (SpiritFlame flame : FLAMES) {
            if (flame.level() != sl || flame.expires() < now || !flame.owner().equals(player.getUUID())) continue;
            if (flame.spot().distanceToSqr(spot) < 1.0) return flame;
        }
        return null;
    }

    // =========================
    // Ability 3 - Damage Transfer
    // =========================
    public static void attendantOfMysteriesDamageTransfer(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!ready(player, bypassClassCheck, 2)) return;

        if (player.isShiftKeyDown()) {
            if (!Spells.payEssence(player, TRANSFER_COST)) return;
            stepOutOfTheFog(player);
            readySubstitute(player, sl);
        } else {
            LivingEntity target = Spells.aimEnemy(player, sl, TRANSFER_RANGE);
            if (target == null) {
                player.sendSystemMessage(Component.literal("No one stands before you."));
                return; // nothing spent
            }
            if (!Spells.payEssence(player, TRANSFER_COST)) return;
            stepOutOfTheFog(player);
            damageTransfer(player, sl, target);
        }

        addStack(player, sl);
    }

    // Normal: for TRANSFER_TICKS, part of every hit on you is dealt to the target instead.
    // The harmful effects you carry right now move onto it as well.
    private static void damageTransfer(Player player, ServerLevel sl, LivingEntity target) {
        DamageLink.redirect(player, target, TRANSFER_SHARE, TRANSFER_TICKS);

        for (MobEffectInstance wound : Spells.harmfulEffects(player)) {
            target.addEffect(new MobEffectInstance(wound.getEffect(), wound.getDuration(), wound.getAmplifier()));
            player.removeEffect(wound.getEffect());
        }

        // a small paper figurine turning over its head marks who carries your wounds
        ModelFx mark = PAPER_FIGURINE.scale(0.32f).lifetime(TRANSFER_TICKS).fade(4, 8).spin(5f).tag("aom_transfer")
                .key(0, ModelFx.pose().up(0.6f).scale(0.2f))
                .key(6, ModelFx.pose().up(0f).scale(1f), ModelFx.Ease.OUT_BACK);
        ParticleShapes.clearModels(sl, target, "aom_transfer");
        ParticleShapes.modelOn(sl, mark, target, new Vec3(0, target.getBbHeight() + 0.35, 0), player.getYRot(), 0f, 0f);
        // the line is there for as long as the link is (a new link to someone else ends this one)
        SpellFx.tether(sl, TRANSFER_THREAD,
                () -> DamageLink.redirectTarget(player) == target ? SpellFx.handOf(player, sl) : null,
                () -> SpellFx.middleOf(target, sl), TRANSFER_TICKS);
        showStatus(player, 3, STATUS_TRANSFER, "transfer", TRANSFER_TICKS);
        ParticleShapes.burst(sl, PAPER_SCRAP, target.getBoundingBox().getCenter(), 8, 0.04, 0.16);
        Spells.sound(sl, target.position(), SoundEvents.ENCHANTMENT_TABLE_USE, 1f, 0.7f);
    }

    // Sneak: the next hit within SUBSTITUTE_TICKS lands on a paper figurine instead of you (see figurineEscape).
    private static void readySubstitute(Player player, ServerLevel sl) {
        Substitute.arm(player, SUBSTITUTE_TICKS, 1, AttendantOfMysteries::figurineEscape);

        // a figurine folds itself in front of you and slips into your sleeve
        ModelFx folded = PAPER_FIGURINE.scale(0.3f).lifetime(18).fade(0, 6)
                .key(0, ModelFx.pose().up(0.5f).scale(0.1f).alpha(0f))
                .key(5, ModelFx.pose().up(0.2f).scale(1f).alpha(1f), ModelFx.Ease.OUT_BACK)
                .key(18, ModelFx.pose().up(-0.3f).scale(0.4f), ModelFx.Ease.IN)
                .during(0, 18, ModelFx.pose().spin(540f), ModelFx.Ease.OUT);
        Vec3 front = player.getEyePosition().add(Spells.flatLook(player).scale(0.9)).add(0, -0.4, 0);
        ParticleShapes.model(sl, folded, front, player.getYRot(), 0f, 0f);
        showStatus(player, 0, STATUS_SUBSTITUTE, "substitute", SUBSTITUTE_TICKS);
        Spells.sound(sl, player.position(), SoundEvents.ENCHANTMENT_TABLE_USE, 0.8f, 1.6f);
    }

    /**
     * What happens instead of the hit while the substitute is ready:
     * a marionette close by takes your place (and the hit); otherwise a paper figurine does and you slip away.
     */
    private static void figurineEscape(LivingEntity saved, DamageSource source, float amount, ServerLevel sl) {
        if (!(saved instanceof Player player)) return;
        ScreenImages.hide(player, STATUS_SUBSTITUTE);
        Vec3 was = player.position();
        Entity attacker = source.getEntity();
        boolean held = Root.has(player, RootRestriction.TELEPORT);         // bound in place: the hit is still lost, but you stay

        Mob puppet = held ? null : nearestMarionette(player, SUBSTITUTE_SWAP_RANGE);
        if (puppet != null) {
            Vec3 there = puppet.position();
            puppet.teleportTo(was.x, was.y, was.z);
            player.teleportTo(there.x, there.y, there.z);
            player.fallDistance = 0;
            puppet.hurt(source, amount);                                    // it takes the blow meant for you
            for (Vec3 end : new Vec3[]{was, there}) {
                ParticleShapes.burst(sl, THREAD_MOTE, end.add(0, 1.0, 0), 16, 0.05, 0.2);
                SpellFx.shockRing(sl, end, 1.6, 0xC0C8B0FF, 7, 0);
            }
            Spells.sound(sl, was, SoundEvents.ENDERMAN_TELEPORT, 0.8f, 1.5f);
            return;
        }

        Vec3 away = attacker == null ? Spells.flatLook(player).scale(-1) : was.subtract(attacker.position());
        Vec3 to = held ? null : Spells.freeSpotNear(player, sl, was, away, SUBSTITUTE_ESCAPE);
        if (to != null) {
            player.teleportTo(to.x, to.y, to.z);
            player.fallDistance = 0;
        }
        paperFigurineDecoy(player, sl, was, player.getYRot(), attacker);
    }

    /** The figurine that stands where you stood: it is torn apart, and throws paper daggers back at the attacker. */
    private static void paperFigurineDecoy(Player player, ServerLevel sl, Vec3 where, float facing, Entity attacker) {
        ModelFx decoy = PAPER_FIGURINE.scale(0.95f).lifetime(14).fade(0, 4)
                .key(0, ModelFx.pose().scale(0.5f).alpha(0f))
                .key(2, ModelFx.pose().scale(1f).alpha(1f), ModelFx.Ease.OUT_BACK)
                .key(7, ModelFx.pose().pitch(0f))                                   // stands for a moment...
                .key(14, ModelFx.pose().pitch(-75f).scale(0.8f), ModelFx.Ease.IN);  // ...and is thrown onto its back
        ParticleShapes.model(sl, decoy, where, facing, 0f, 0f);
        Spells.sound(sl, where, SoundEvents.ENDERMAN_TELEPORT, 0.5f, 1.8f);

        Later.run(sl, 7, () -> {
            ParticleShapes.burst(sl, PAPER_SCRAP, where.add(0, 1.0, 0), 30, 0.06, 0.3);
            Spells.sound(sl, where, SoundEvents.PLAYER_ATTACK_SWEEP, 0.8f, 1.7f);
            if (!(attacker instanceof LivingEntity enemy) || !Spells.casterStillHere(player, sl)) return;
            if (!enemy.isAlive() || enemy.level() != sl || !Spells.isEnemy(player, enemy)) return;
            Vec3 from = where.add(0, 1.2, 0);
            Vec3 aim = enemy.getBoundingBox().getCenter().subtract(from);
            if (aim.lengthSqr() < 0.25) return;
            for (int i = 0; i < SUBSTITUTE_DAGGERS; i++) {
                double turn = (i - (SUBSTITUTE_DAGGERS - 1) / 2.0) * 7.0;
                throwDagger(player, sl, from, Spells.turned(aim.normalize(), turn), aim.length() + 4.0, SUBSTITUTE_DAGGER_DAMAGE, 0);
            }
        });
    }

    /** The caster's nearest marionette within range, or null. */
    private static Mob nearestMarionette(Player player, double range) {
        Mob nearest = null;
        double best = range * range;
        for (Mob puppet : Marionette.of(player)) {
            double distance = puppet.distanceToSqr(player);
            if (distance <= best) {
                best = distance;
                nearest = puppet;
            }
        }
        return nearest;
    }

    // =========================
    // Ability 4 - Air Cannon
    // =========================
    public static void attendantOfMysteriesAirCannon(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!ready(player, bypassClassCheck, 3)) return;
        if (!Spells.payEssence(player, CANNON_COST)) return;
        stepOutOfTheFog(player);

        if (player.isShiftKeyDown()) {
            // Sneak: fired at the ground under you. It throws enemies away and you into the air.
            Vec3 ground = player.position();
            airBurst(player, sl, ground, 1f);
            Vec3 motion = player.getDeltaMovement();
            player.setDeltaMovement(motion.x, LEAP_SPEED, motion.z);
            player.hurtMarked = true;
            FallGuard.protect(player, LEAP_GUARD_TICKS);
            echo(player, sl, 0, () -> airBurst(player, sl, ground, PROJECTION_POWER));
        } else {
            Vec3 look = player.getLookAngle().normalize();
            Vec3 muzzle = Spells.clearStart(player, sl, player.getEyePosition().add(look.scale(0.9)).add(0, -0.15, 0));
            airCannon(player, sl, muzzle, 1f);

            // the recoil throws you back (and up, if you fire downward)
            player.setDeltaMovement(-look.x * CANNON_RECOIL, -look.y * CANNON_RECOIL * 0.5 + 0.2, -look.z * CANNON_RECOIL);
            player.hurtMarked = true;
            FallGuard.protect(player, 100);
            echo(player, sl, 0, () -> airCannon(player, sl, projectionHand(player), PROJECTION_POWER));
        }

        addStack(player, sl);
    }

    /**
     * Normal: a shell of compressed air. What it hits takes the full damage; everything else in the blast takes
     * part of it and is thrown away from it. Also used by the projection.
     * Fired from 'from' toward what the caster's crosshair is on.
     */
    private static void airCannon(Player player, ServerLevel sl, Vec3 from, float power) {
        Vec3 direction = Spells.aimFrom(player, sl, from, CANNON_RANGE);
        LivingEntity[] struck = new LivingEntity[1];                // what the shell itself hit, if anything
        int arrives = AIR_SHOT
                .onHit((target, at) -> {
                    struck[0] = target;
                    Spells.strike(player, target, CANNON_DAMAGE * power);
                })
                .onEnd((at, hitBlock) -> {
                    for (LivingEntity target : Spells.enemiesAround(player, sl, at, CANNON_BLAST_RADIUS)) {
                        if (target != struck[0]) Spells.strike(player, target, CANNON_DAMAGE * CANNON_SPLASH * power);
                        Spells.push(target, at.subtract(direction.scale(1.5)), 1.1, 0.4);   // away, mostly the way it flew
                    }
                    airBlastLook(sl, at, CANNON_BLAST_RADIUS);
                    Spells.sound(sl, at, SoundEvents.GENERIC_EXPLODE.value(), 1.1f, 1.6f);
                })
                .fire(player, sl, from, direction);

        // rings of pressed air left hanging along its path (it is at tick * speed blocks at each tick)
        for (int tick = 1; tick < arrives; tick += 2) {
            Vec3 at = from.add(direction.scale(tick * CANNON_SPEED));
            ModelFx ring = SpellFx.SHOCK_RING.color(0xB0E4F4FF).delay(tick).lifetime(8)
                    .key(0, ModelFx.pose().scale(0.5f))
                    .key(8, ModelFx.pose().scale(2.4f), ModelFx.Ease.OUT)
                    .during(2, 8, ModelFx.pose().alpha(0f));
            ParticleShapes.modelAlong(sl, ring, at, direction);             // the flat ring faces the way the shell flies
        }
        ParticleShapes.cone(sl, AIR_PUFF, from, direction, 35, 8, 0.1, 0.5);
        Spells.sound(sl, from, SoundEvents.WARDEN_SONIC_BOOM, 0.7f, 1.7f);
        Spells.sound(sl, from, SoundEvents.BREEZE_JUMP, 1f, 0.7f);
    }

    /** Sneak: a blast against the ground. Hurts and throws away everything around the spot. Also used by the projection. */
    private static void airBurst(Player player, ServerLevel sl, Vec3 ground, float power) {
        for (LivingEntity target : Spells.enemiesAround(player, sl, ground.add(0, 0.5, 0), LEAP_BLAST_RADIUS)) {
            Spells.strike(player, target, LEAP_DAMAGE * power);
            Spells.push(target, ground, 1.3, 0.55);
        }
        airBlastLook(sl, ground.add(0, 0.2, 0), LEAP_BLAST_RADIUS);
        ParticleShapes.cone(sl, AIR_PUFF, ground.add(0, 0.2, 0), Spells.UP, 150, 26, 0.2, 0.7);
        Spells.sound(sl, ground, SoundEvents.GENERIC_EXPLODE.value(), 1f, 1.5f);
        Spells.sound(sl, ground, SoundEvents.BREEZE_JUMP, 1.2f, 0.6f);
    }

    /** Air bursting outward: two pale rings and a round puff. */
    private static void airBlastLook(ServerLevel sl, Vec3 center, double radius) {
        SpellFx.shockRing(sl, center.add(0, -0.2, 0), radius * 1.2, 0xD0E8F6FF, 7, 0);
        SpellFx.shockRing(sl, center.add(0, -0.2, 0), radius * 0.8, 0xA0FFFFFF, 10, 2);
        ModelFx pop = AIR_SHELL.lifetime(6)
                .key(0, ModelFx.pose().scale(1f))
                .key(6, ModelFx.pose().scale((float) (radius * 1.6)), ModelFx.Ease.OUT)
                .during(1, 6, ModelFx.pose().alpha(0f));
        ParticleShapes.model(sl, pop, center, sl.getRandom().nextFloat() * 360f, 0f, 0f);
        ParticleShapes.burst(sl, AIR_PUFF, center, (int) (radius * 7), 0.1, 0.45);
    }

    // =========================
    // Ability 5 - Spirit Body Threads
    // =========================
    public static void attendantOfMysteriesCountdown(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!ready(player, bypassClassCheck, 4)) return;
        if (!(player instanceof ServerPlayer)) return;
        if (Countdown.isRunning(player)) return; // already counting

        if (player.isShiftKeyDown()) {
            List<LivingEntity> caught = Spells.enemiesAround(player, sl, player.position().add(0, 1.0, 0), WEB_RADIUS);
            if (caught.isEmpty()) {
                player.sendSystemMessage(Component.literal("There are no threads within your reach."));
                return; // nothing spent
            }
            if (!Spells.payEssence(player, THREADS_COST)) return;
            stepOutOfTheFog(player);
            caught.sort((a, b) -> Double.compare(a.distanceToSqr(player), b.distanceToSqr(player)));
            threadWeb(player, sl, new ArrayList<>(caught.subList(0, Math.min(WEB_MAX_TARGETS, caught.size()))));
        } else {
            LivingEntity target = Spells.aimEnemy(player, sl, THREADS_RANGE);
            if (target == null) {
                player.sendSystemMessage(Component.literal("No one stands before you."));
                return; // nothing spent
            }
            if (!Spells.payEssence(player, THREADS_COST)) return;
            stepOutOfTheFog(player);
            seizeThreads(player, sl, target);
        }

        addStack(player, sl);
    }

    // Normal: you take hold of one target's Spirit Body Threads. For five seconds it grows slower and slower;
    // if it is still within reach at 0, it is yours (see takeHold).
    private static void seizeThreads(Player player, ServerLevel sl, LivingEntity target) {
        int ticks = COUNTDOWN_SECONDS * 20;
        Countdown.seconds(COUNTDOWN_SECONDS)
                .colors(COUNTDOWN_COLOR, COUNTDOWN_LAST_COLOR)
                .onSecond(left -> {
                    stiffen(target, left);
                    ParticleShapes.burst(sl, THREAD_MOTE, target.getBoundingBox().getCenter(), 6, 0.03, 0.12);
                    Spells.sound(sl, target.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 0.9f, 0.6f + (COUNTDOWN_SECONDS - left) * 0.2f);
                })
                .keepWhile(() -> target.isAlive() && target.level() == sl && player.level() == sl
                        && player.distanceToSqr(target) <= THREADS_LEASH * THREADS_LEASH)
                .onCancel(() -> {
                    player.sendSystemMessage(Component.literal("The threads slip from your fingers."));
                    Spells.sound(sl, player.position(), SoundEvents.CHAIN_PLACE, 0.7f, 0.5f);
                })
                .onFinish(() -> takeHold(player, sl, target))
                .start(player);

        // two threads from your hand, to its head and to its body, for as long as the countdown runs
        SpellFx.tether(sl, SPIRIT_THREAD, () -> Countdown.isRunning(player) ? SpellFx.handOf(player, sl) : null,
                () -> SpellFx.middleOf(target, sl), ticks);
        SpellFx.tether(sl, SPIRIT_THREAD, () -> Countdown.isRunning(player) ? SpellFx.handOf(player, sl) : null,
                () -> SpellFx.topOf(target, sl), ticks);
        Spells.sound(sl, player.position(), SoundEvents.CHAIN_PLACE, 0.8f, 1.8f);
    }

    /** Each second of the countdown the target moves and strikes more slowly: 5 = a little, 1 = barely at all. */
    private static void stiffen(LivingEntity target, int secondsLeft) {
        int level = Mth.clamp(COUNTDOWN_SECONDS - secondsLeft, 0, 5);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, level));
        if (level >= 2) target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 30, level - 2));
    }

    /** The countdown reached 0. A mob becomes a marionette; a player or something too strong for that is bound and hurt. */
    private static void takeHold(Player player, ServerLevel sl, LivingEntity target) {
        Vec3 feet = target.position();
        if (target instanceof Mob mob && mob.getMaxHealth() <= MARIONETTE_MAX_HEALTH
                && Marionette.control(mob, player, MARIONETTE_TICKS)) {
            mob.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);         // it moves freely again, for you
            mob.removeEffect(MobEffects.WEAKNESS);
            SpellFx.closingRing(sl, feet, 3.0, 0xD0C8B0FF, 8, 0);
            ParticleShapes.burst(sl, THREAD_MOTE, mob.getBoundingBox().getCenter(), 24, 0.04, 0.22);
            Spells.sound(sl, feet, SoundEvents.ZOMBIE_VILLAGER_CONVERTED, 0.8f, 1.5f);
            return;
        }
        Spells.strike(player, target, BIND_DAMAGE);
        Root.apply(target, BIND_TICKS, RootRestriction.EVERYTHING);
        stringUp(sl, target, BIND_TICKS);
        Spells.sound(sl, feet, SoundEvents.CHAIN_PLACE, 1.2f, 0.6f);
    }

    // Sneak: the eye opens and you see every thread around you. All of them are taken at once:
    // everything caught slows down for five seconds, and what is still close at the end is strung up and hurt.
    private static void threadWeb(Player player, ServerLevel sl, List<LivingEntity> caught) {
        int ticks = COUNTDOWN_SECONDS * 20;
        ScreenImages.show(player, "aom_sneak", SNEAK_IMAGE, 128, ticks);

        Countdown.seconds(COUNTDOWN_SECONDS)
                .silent()                                               // the eye is shown instead of the numbers
                .onSecond(left -> {
                    for (LivingEntity target : caught) {
                        if (inWeb(player, sl, target)) stiffen(target, left);
                    }
                    Spells.sound(sl, player.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 0.9f, 0.6f + (COUNTDOWN_SECONDS - left) * 0.2f);
                })
                .keepWhile(() -> player.level() == sl)
                .onCancel(() -> ScreenImages.hide(player, "aom_sneak"))
                .onFinish(() -> pullWeb(player, sl, caught))
                .start(player);

        for (LivingEntity target : caught) {
            SpellFx.tether(sl, SPIRIT_THREAD,
                    () -> Countdown.isRunning(player) && inWeb(player, sl, target) ? SpellFx.handOf(player, sl) : null,
                    () -> SpellFx.middleOf(target, sl), ticks);
        }
        SpellFx.runeCircle(sl, player.position(), WEB_RADIUS, 0x70C8B0FF, ticks, -2f);
        Spells.sound(sl, player.position(), SoundEvents.CHAIN_PLACE, 1f, 1.4f);
    }

    private static boolean inWeb(Player player, ServerLevel sl, LivingEntity target) {
        return target.isAlive() && target.level() == sl && player.distanceToSqr(target) <= WEB_LEASH * WEB_LEASH;
    }

    private static void pullWeb(Player player, ServerLevel sl, List<LivingEntity> caught) {
        for (LivingEntity target : caught) {
            if (!inWeb(player, sl, target)) continue;                   // it got away in time
            Spells.strike(player, target, WEB_DAMAGE);
            Root.apply(target, WEB_BIND_TICKS, RootRestriction.EVERYTHING);
            stringUp(sl, target, WEB_BIND_TICKS);
        }
        SpellFx.closingRing(sl, player.position(), WEB_RADIUS, 0xD0C8B0FF, 8, 0);
        Spells.sound(sl, player.position(), SoundEvents.CHAIN_PLACE, 1.4f, 0.5f);
        Spells.sound(sl, player.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 1.2f, 0.5f);
    }

    /** Taut threads rising from an entity's head and shoulders into the air for as long as it is held. */
    private static void stringUp(ServerLevel sl, LivingEntity target, int ticks) {
        double side = target.getBbWidth() * 0.45;
        float lean = 6f;
        Vec3[] roots = {new Vec3(0, target.getBbHeight(), 0), new Vec3(side, target.getBbHeight() * 0.75, 0),
                new Vec3(-side, target.getBbHeight() * 0.75, 0)};
        float[] rolls = {0f, -lean, lean};      // the two at the shoulders lean outward, like strings up to a wide bar
        ParticleShapes.clearModels(sl, target, "aom_strings");
        for (int i = 0; i < roots.length; i++) {
            ModelFx string = THREAD.color(0xE0E6DCFF).scale(1.2f, 2.2f, 1.2f).lifetime(ticks).fade(0, 8).tag("aom_strings")
                    .key(0, ModelFx.pose().scale(0.1f).alpha(0f))
                    .key(4, ModelFx.pose().scale(1f).alpha(1f), ModelFx.Ease.OUT);     // shoots up out of it
            ParticleShapes.modelOn(sl, string, target, roots[i], 0f, 0f, rolls[i]);
        }
        ParticleShapes.burst(sl, THREAD_MOTE, target.getBoundingBox().getCenter(), 14, 0.04, 0.2);
    }

    // =========================
    // Ability 6 - Historical Void
    // =========================
    public static void attendantOfMysteriesHistoricalVoid(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!ready(player, bypassClassCheck, 5)) return;

        if (player.isShiftKeyDown()) {
            if (Concealment.isHidden(player)) {
                Concealment.reveal(player);                         // pressing it again steps back out early, for free
                return;
            }
            if (!Spells.payEssence(player, HISTORY_COST)) return;
            hideInHistory(player, sl);
        } else {
            if (!Spells.payEssence(player, HISTORY_COST)) return;
            stepOutOfTheFog(player);
            summonProjection(player, sl);
        }

        addStack(player, sl);
    }

    // Normal: a projection of your past self stands beside you for PROJECTION_TICKS. When you cast Paper Daggers,
    // Flaming Jump or Air Cannon it casts them too, weaker and for free (see echo).
    private static void summonProjection(Player player, ServerLevel sl) {
        Vec3 side = Spells.rightOf(player).scale(PROJECTION_SIDE);
        CompoundTag data = player.getPersistentData();
        data.putInt(PROJECTION_LEFT, PROJECTION_TICKS);
        data.putDouble(PROJECTION_X, side.x);
        data.putDouble(PROJECTION_Z, side.z);

        ModelFx figure = PROJECTION.scale(0.94f).lifetime(PROJECTION_TICKS).fade(0, 14)
                .key(0, ModelFx.pose().up(-0.4f).scale(0.8f).alpha(0f))
                .key(10, ModelFx.pose().up(0.12f).scale(1f).alpha(1f), ModelFx.Ease.OUT);     // rises out of the fog
        ParticleShapes.clearModels(sl, player, "aom_projection");                             // cast again: one, not two
        ParticleShapes.modelOn(sl, figure, player, side, player.getYRot(), 0f, 0f);

        Vec3 there = player.position().add(side);
        ParticleShapes.burst(sl, HISTORY_FOG, there.add(0, 1.0, 0), 18, 0.02, 0.1);
        SpellFx.runeCircle(sl, there, 1.4, 0x90C4CCDC, 30, 3f);
        showStatus(player, 1, STATUS_PROJECTION, "projection", PROJECTION_TICKS);
        Spells.sound(sl, there, SoundEvents.BEACON_ACTIVATE, 0.8f, 0.6f);
    }

    private static boolean projectionActive(Player player) {
        return player.getPersistentData().getInt(PROJECTION_LEFT) > 0;
    }

    /** Where the projection's raised hand is. */
    private static Vec3 projectionHand(Player player) {
        CompoundTag data = player.getPersistentData();
        return player.position().add(data.getDouble(PROJECTION_X), 1.5, data.getDouble(PROJECTION_Z));
    }

    /**
     * If the projection is there, it repeats what the caster just did: 'repeat' runs 'delay' ticks later (0 = at once).
     * Give it the same spell cast from projectionHand(player) at PROJECTION_POWER. It costs nothing and adds no stack.
     */
    private static void echo(Player player, ServerLevel sl, int delay, Runnable repeat) {
        if (!projectionActive(player)) return;
        Later.run(sl, delay, () -> {
            if (!Spells.casterStillHere(player, sl) || !projectionActive(player)) return;
            Vec3 hand = projectionHand(player);
            ParticleShapes.burst(sl, HISTORY_FOG, hand, 5, 0.01, 0.06);
            ParticleShapes.burst(sl, THREAD_MOTE, hand, 8, 0.03, 0.14);
            Spells.sound(sl, hand, SoundEvents.ILLUSIONER_CAST_SPELL, 0.6f, 1.5f);
            repeat.run();
        });
    }

    // Sneak: you step into the fog of history. Nothing sees, targets or hurts you for FOG_TICKS, you move faster,
    // and what ailed you stays behind. Casting, attacking or pressing the key again steps back out.
    private static void hideInHistory(Player player, ServerLevel sl) {
        Spells.cleanse(player);
        Concealment.hide(player, FOG_TICKS);
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, FOG_TICKS, FOG_SPEED_LEVEL, false, false));

        Vec3 feet = player.position();
        ParticleShapes.burst(sl, HISTORY_FOG, feet.add(0, 1.0, 0), 40, 0.03, 0.2);
        SpellFx.runeCircle(sl, feet, 2.2, 0x90C4CCDC, 40, -3f);
        SpellFx.shockRing(sl, feet, 4.0, 0x80C4CCDC, 12, 0);
        Spells.sound(sl, feet, SoundEvents.ENDERMAN_TELEPORT, 0.7f, 0.5f);
    }

    /** Casting anything else ends the concealment: you cannot act from inside history. */
    private static void stepOutOfTheFog(Player player) {
        if (Concealment.isHidden(player)) Concealment.reveal(player);
    }

    // =========================
    // Ability 7 - Grafting
    // =========================
    public static void attendantOfMysteriesGrafting(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!ready(player, bypassClassCheck, 6)) return;
        if (!Spells.payEssence(player, GRAFT_COST)) return;
        stepOutOfTheFog(player);

        if (player.isShiftKeyDown()) miracle(player, sl);
        else graft(player, sl, Spells.aimGround(player, sl, GRAFT_RANGE));

        addStack(player, sl);
    }

    // Normal: a colossal needle hangs over the spot you aim at, then stabs down. Everything under it is hurt,
    // dragged to it by its thread and stitched together: for GRAFT_TICKS, what hurts one of them hurts them all.
    private static void graft(Player player, ServerLevel sl, Vec3 ground) {
        int land = GRAFT_WARNING + NEEDLE_FALL;
        SpellFx.warningCircle(sl, ground, GRAFT_RADIUS, 0xE0C8A8FF, land);

        // The needle hangs point down with its point on the spot, so up(9) lifts the point 9 blocks above it.
        ModelFx needle = needleFlight(NEEDLE, land).lifetime(land + GRAFT_PULL_TICKS + 34).fade(0, 14);
        ParticleShapes.model(sl, needle, ground, player.getYRot(), 0f, 0f);
        // The thread runs from the needle's eye up into the sky and makes every move the needle makes.
        float eye = 48f / 16f * NEEDLE_SIZE;                                           // the needle is 48 px long
        ModelFx thread = needleFlight(THREAD.color(0xE0FFE0A0).scale(3f, 9f, 3f), land)        // 27 blocks long
                .lifetime(land + GRAFT_PULL_TICKS + 34).fade(0, 14);
        ParticleShapes.model(sl, thread, ground.add(0, eye, 0), player.getYRot(), 0f, 0f);
        Spells.sound(sl, ground, SoundEvents.BEACON_POWER_SELECT, 1.4f, 0.5f);

        Later.run(sl, land, () -> graftImpact(player, sl, ground));
    }

    /** The flight shared by the needle and its thread: fades in high up, sinks, hangs, then falls into the ground. */
    private static ModelFx needleFlight(ModelFx model, int land) {
        return model
                .key(0, ModelFx.pose().up(NEEDLE_HANG + 5f).alpha(0f))
                .key(8, ModelFx.pose().alpha(1f), ModelFx.Ease.OUT)
                .key(GRAFT_WARNING, ModelFx.pose().up(NEEDLE_HANG), ModelFx.Ease.IN_OUT)       // hangs over the spot, sinking slowly
                .key(land, ModelFx.pose().up(-1.8f), ModelFx.Ease.IN)                          // falls, point deep into the ground
                .key(land + 3, ModelFx.pose().up(-1.4f), ModelFx.Ease.OUT)
                .during(0, GRAFT_WARNING, ModelFx.pose().spin(360f), ModelFx.Ease.OUT);        // one turn while it hangs
    }

    private static void graftImpact(Player player, ServerLevel sl, Vec3 ground) {
        List<LivingEntity> caught = Spells.enemiesAround(player, sl, ground, GRAFT_RADIUS);
        for (LivingEntity target : caught) Spells.strike(player, target, GRAFT_DAMAGE);

        SpellFx.shockRing(sl, ground, GRAFT_RADIUS * 1.3, 0xF0E0C8FF, 12, 0);
        SpellFx.shockRing(sl, ground, GRAFT_RADIUS * 0.9, 0xD0FFE0A0, 16, 3);
        ParticleShapes.burst(sl, GOLD_MOTE, ground.add(0, 0.6, 0), 60, 0.15, 0.7);
        Spells.sound(sl, ground, SoundEvents.ANVIL_LAND, 1.4f, 1.5f);
        Spells.sound(sl, ground, SoundEvents.LIGHTNING_BOLT_IMPACT, 1.2f, 1.4f);

        // The seams: a golden stitch from each of them to the needle, drawn while the thread pulls tight.
        Vec3 eye = ground.add(0, 0.25, 0);
        for (LivingEntity target : caught) {
            if (target.isAlive()) ParticleShapes.slashBetween(sl, STITCH.sweep(GRAFT_PULL_TICKS).lifetime(GRAFT_PULL_TICKS + 12),
                    target.position().add(0, 0.25, 0), eye);
        }
        SpellFx.closingRing(sl, ground, GRAFT_RADIUS, 0xD0FFE0A0, GRAFT_PULL_TICKS, 0);
        for (int tick = 1; tick <= GRAFT_PULL_TICKS; tick += 2) {             // one tug of the thread every other tick
            Later.run(sl, tick, () -> {
                for (LivingEntity target : caught) {
                    if (target.isAlive() && target.level() == sl) Spells.pull(target, ground, GRAFT_PULL_SPEED, 0.18);
                }
            });
        }
        Later.run(sl, GRAFT_PULL_TICKS + 1, () -> stitchTogether(player, sl, ground, caught));
    }

    /** Grafted: the survivors share their pain for GRAFT_TICKS, and the seams between them stay visible. */
    private static void stitchTogether(Player player, ServerLevel sl, Vec3 ground, List<LivingEntity> caught) {
        List<LivingEntity> grafted = new ArrayList<>();
        for (LivingEntity target : caught) {
            if (target.isAlive() && target.level() == sl) grafted.add(target);
        }
        SpellFx.runeCircle(sl, ground, GRAFT_RADIUS * 0.6, 0xA0FFE0A0, GRAFT_TICKS, 1.5f);
        if (grafted.size() < 2) return;                                     // one alone has nobody to share with

        DamageLink.share(grafted, GRAFT_SHARE, GRAFT_TICKS, player);
        Vec3 knot = ground.add(0, 1.2, 0);
        for (LivingEntity target : grafted) {
            // each seam is there for as long as that one is still stitched to the others
            SpellFx.tether(sl, STITCH, () -> knot,
                    () -> DamageLink.isShared(target) ? SpellFx.middleOf(target, sl) : null, GRAFT_TICKS);
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, GRAFT_TICKS, 0, false, false));
        }
        Spells.sound(sl, ground, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.4f, 0.7f);
    }

    // Sneak - Miracle: you are made whole, and for MIRACLE_TICKS your death is undone once (see miracleReturn).
    private static void miracle(Player player, ServerLevel sl) {
        player.setHealth(player.getMaxHealth());
        Spells.cleanse(player);
        DeathWard.arm(player, MIRACLE_TICKS, MIRACLE_HEALTH, AttendantOfMysteries::miracleReturn);

        // The Fool's card turns over your head, then sinks into you.
        ModelFx card = FOOL_CARD.scale(1.4f).lifetime(50).fade(0, 6).tag("aom_miracle")
                .key(0, ModelFx.pose().up(1.2f).scale(0.2f).alpha(0f))
                .key(8, ModelFx.pose().up(0f).scale(1f).alpha(1f), ModelFx.Ease.OUT_BACK)
                .key(36, ModelFx.pose().up(0.15f))
                .key(50, ModelFx.pose().up(-1.6f).scale(0.3f), ModelFx.Ease.IN)
                .during(0, 50, ModelFx.pose().spin(720f), ModelFx.Ease.OUT);
        ParticleShapes.clearModels(sl, player, "aom_miracle");
        ParticleShapes.modelOn(sl, card, player, new Vec3(0, player.getBbHeight() + 1.1, 0), player.getYRot(), 0f, 0f);

        Vec3 feet = player.position();
        SpellFx.runeCircle(sl, feet, 2.4, 0xC0FFE9A8, 50, 4f);
        ParticleShapes.cone(sl, GOLD_MOTE, feet.add(0, 0.2, 0), Spells.UP, 60, 40, 0.05, 0.25);
        showStatus(player, 2, STATUS_MIRACLE, "miracle", MIRACLE_TICKS);
        Spells.sound(sl, feet, SoundEvents.BEACON_ACTIVATE, 1f, 1.5f);
        Spells.sound(sl, feet, SoundEvents.PLAYER_LEVELUP, 0.7f, 0.7f);
    }

    /** The miracle happens: you were about to die and did not. Enemies are thrown back and you return hidden. */
    private static void miracleReturn(LivingEntity saved, DamageSource source, ServerLevel sl) {
        Vec3 feet = saved.position();
        Spells.cleanse(saved);
        if (saved instanceof Player player) {
            ScreenImages.hide(player, STATUS_MIRACLE);
            for (LivingEntity target : Spells.enemiesAround(player, sl, feet.add(0, 1.0, 0), MIRACLE_PUSH_RADIUS)) {
                Spells.push(target, feet, 1.4, 0.5);
            }
            player.sendSystemMessage(Component.literal("A miracle."));
        }
        Concealment.hide(saved, MIRACLE_FOG_TICKS);

        ModelFx card = FOOL_CARD.scale(2.2f).lifetime(24).fade(0, 10)
                .key(0, ModelFx.pose().scale(0.3f))
                .key(6, ModelFx.pose().scale(1f), ModelFx.Ease.OUT_BACK)
                .during(0, 24, ModelFx.pose().spin(540f).up(1.2f), ModelFx.Ease.OUT);       // the card flares and rises
        ParticleShapes.model(sl, card, feet.add(0, saved.getBbHeight() * 0.6, 0), saved.getYRot(), 0f, 0f);
        SpellFx.shockRing(sl, feet, MIRACLE_PUSH_RADIUS * 1.3, 0xF0FFE9A8, 12, 0);
        SpellFx.shockRing(sl, feet, MIRACLE_PUSH_RADIUS, 0xD0FFFFFF, 16, 3);
        ParticleShapes.burst(sl, GOLD_MOTE, feet.add(0, 1.0, 0), 80, 0.12, 0.6);
        Spells.sound(sl, feet, SoundEvents.END_PORTAL_SPAWN, 0.5f, 1.6f);
        Spells.sound(sl, feet, SoundEvents.BEACON_POWER_SELECT, 1.2f, 1.2f);
    }

    // =========================
    // Stacks
    // =========================

    private static void addStack(Player player, ServerLevel sl) {
        int slot = player.getPersistentData().getInt(STACKS); // 0..4
        String image = STACK_IMAGES.get(player.getRandom().nextInt(STACK_IMAGES.size()));
        player.getPersistentData().putString(STACK_IMAGE_KEY + slot, image);
        showStackImage(player, slot, image);

        int stacks = slot + 1;
        if (stacks >= MAX_STACKS) {
            unleashStacks(player, sl);
            ScreenImages.hide(player, STACK_IMAGE_ID + "*", 10); // let the 5th show for a moment, then all fade
            stacks = 0;
        }
        player.getPersistentData().putInt(STACKS, stacks);
    }

    // Runs when the 5th stack is added. Big hit on every enemy around the player.
    private static void unleashStacks(Player player, ServerLevel sl) {
        Vec3 feet = player.position();
        Vec3 center = feet.add(0, 1.0, 0);
        for (LivingEntity target : Spells.enemiesAround(player, sl, center, STACK_BURST_RADIUS)) {
            Spells.strike(player, target, STACK_BURST_DAMAGE);     // lands even right after the ability that triggered it
        }

        SpellFx.shockRing(sl, feet, STACK_BURST_RADIUS, 0xF0FF4A5A, 9, 0);
        SpellFx.shockRing(sl, feet, STACK_BURST_RADIUS * 0.7, 0xD0FFE0D0, 12, 2);
        SpellFx.runeCircle(sl, feet, STACK_BURST_RADIUS * 0.5, 0xB0FF4A5A, 14, 8f);
        ParticleShapes.burst(sl, STACK_SPARK, center, 70, 0.15, 0.75);
        sl.playSound(null, player.blockPosition(),
                SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.5f, 0.6f);
    }

    // Stacks sit to the right of the hotbar, 18px apart like hotbar slots
    private static void showStackImage(Player player, int slot, String image) {
        ScreenImages.show(player, STACK_IMAGE_ID + slot, image,
                ScreenAnchor.BOTTOM, 103 + slot * 18, -3, 16, ScreenImages.FOREVER);
    }

    private static void refreshStackImages(Player player) {
        int stacks = player.getPersistentData().getInt(STACKS);
        for (int slot = 0; slot < MAX_STACKS; slot++) {
            if (slot < stacks) {
                String image = player.getPersistentData().getString(STACK_IMAGE_KEY + slot);
                showStackImage(player, slot, image.isEmpty() ? STACK_IMAGES.get(0) : image);
            } else {
                ScreenImages.hide(player, STACK_IMAGE_ID + slot);
            }
        }
    }

    /**
     * A small picture in the row above the stacks for as long as something is active (it fades by itself).
     * 'image' is a file in textures/gui/attendant_of_mysteries/ without .png; slot 0 is right above the first stack.
     */
    private static void showStatus(Player player, int slot, String id, String image, int ticks) {
        ScreenImages.show(player, id, "attendant_of_mysteries/" + image,
                ScreenAnchor.BOTTOM, 103 + slot * 18, -21, 16, ticks);
    }

    // =========================
    // Helpers shared by the abilities
    // =========================

    /** Has this aspect (or the check is skipped) and has reached the ascension stage the ability needs. */
    private static boolean ready(Player player, boolean bypassClassCheck, int stage) {
        if (!bypassClassCheck && !SoulCore.getAspect(player).equals(ASPECT)) return false;
        return SoulCore.getAscensionStage(player) >= stage;
    }

    private static void countDown(CompoundTag data, String key) {
        int left = data.getInt(key);
        if (left > 0) data.putInt(key, left - 1);
    }

    /** True if there is a block right under the spot (so a flame there stands on something). */
    private static boolean onSolidGround(ServerLevel sl, Vec3 spot) {
        return !sl.getBlockState(BlockPos.containing(spot.x, spot.y - 0.2, spot.z)).isAir();
    }

    // =========================
    // Melee combo hits
    // =========================

    // Hit 1: a ring sweeping around the player
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
        SlashFx ring = COMBO_TILTED_RING.varied(player.getRandom(), 25f, 10f, 15f);
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
                        .delay(bundle * 2 + i / 2);      // blades appear in quick stages
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
}
