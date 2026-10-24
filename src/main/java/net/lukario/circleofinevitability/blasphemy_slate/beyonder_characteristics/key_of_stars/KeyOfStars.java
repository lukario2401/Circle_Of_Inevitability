package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.key_of_stars;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonders;
import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonder;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.SoulCore;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.key_of_stars.KeyOfStarsFx.Status;
import net.lukario.circleofinevitability.combat.Casts;
import net.lukario.circleofinevitability.combat.Cooldowns;
import net.lukario.circleofinevitability.combat.Echo;
import net.lukario.circleofinevitability.combat.Later;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.combat.Teleport;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.lukario.circleofinevitability.status.Anchors;
import net.lukario.circleofinevitability.status.Seal;
import net.lukario.circleofinevitability.status.Starlit;
import net.lukario.circleofinevitability.status.Unbound;
import net.lukario.circleofinevitability.status.Unreadable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.Result;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/**
 * Key Of Stars: the Door Pathway of Lord of the Mysteries, the Magician card (docs/KEY_OF_STARS.md). Seven keys in
 * the Attendant Of Mysteries pattern, from Apprentice (Sequence 9) up to Door (Sequence 0):
 *
 *   1 Door Opening   (Apprentice)        step through a wall by an illusory Door; a real door swings open for free
 *                                                                              | sneak: Slip Free (you or the ally aimed at)
 *   2 Tricks         (Trickmaster)       the wheel: Flash, Black Screen, Loud Noise, Frost Ray, Gale, Tumble, Escape
 *                                        Trick (KeyOfStarsTricks)          | sneak: the last trick again
 *   3 Astrology      (Astrologer)        Star Divination: the creature aimed at is Starlit and read; at nothing, every
 *                                        creature near shows as a star     | sneak: Interference (Unreadable)
 *   4 Record         (Scribe)            for a while the first power cast near you is written down (KeyOfStarsRecord)
 *                                                                              | sneak: Replay the oldest page
 *   5 Travel         (Traveler)          the wheel: Traveler's Door, Blink, Invisible Hand, Star Anchor, Wandering (3)
 *                                        (KeyOfStarsTravel); while Wandering, the key steps you out | sneak: the last pick
 *   6 Secrets        (Secrets Sorcerer)  the wheel: Space Concealment, Transfiguration, Exile, Pocket Space, Space
 *                                        Tearing (3) (KeyOfStarsSecrets)  | sneak: the last pick
 *   7 Starlight      (Planeswalker)      the wheel: Toy Box, Replication, Space Fragmentation (1), Starlight Seal (1),
 *                                        Space-Time Labyrinth (1), Alternate World (0), Omnipresence (0), Close the
 *                                        Door (0) (KeyOfStarsStarlight)   | sneak: the last pick
 *
 * The parts of the kit it leans on, any Pathway can use too: status/Starlit (the mark; KeyOfStars.starlit shows it),
 * status/Anchors (Star Anchors), status/Unbound (Slip Free), status/Unreadable (Interference; its turned-away hook
 * turns the search back), combat/Teleport (every door and hop; Safe Landing is its fall guard), Root.addDurationScale
 * (Footprints of Freedom, Astral Footing), combat/Casts (what everyone cast: Record, Replication, Quick Study, the
 * reading), combat/Echo (Omnipresence, the Planeswalker's Blink), combat/Enclosure (the Labyrinth, the Alternate
 * World), status/Seal (Starlight Seal), status/Toy (Toy Box).
 * Every cast's cost goes through costFor (the wheels through wheelCostFor) and every wheel's cooldown through
 * cooldownFor: the Secrets Sorcerer's quick powers after a Blink live there, Door of Doors in doorOfDoors.
 *
 * Every look and sound is in KeyOfStarsFx (see /starsfx); this class only decides who goes where, who is marked and
 * hurt, and when. Nothing here breaks blocks or lights real fire (a real door is only swung). Numbers are right below.
 */
public class KeyOfStars {

    public static final String PATHWAY = "Key Of Stars";

    // =========================
    // Ability settings (cost = spirituality, ticks: 20 = 1 second, distances in blocks)
    // =========================

    // Ability 1 - Door Opening (Sequence 9)
    static final int DOOR_SEQUENCE = 9;
    static final int DOOR_COST = 250;
    private static final int DOOR_COOLDOWN = 60;
    private static final double DOOR_REAL_RANGE = 8.0;                   // a real door this close just swings open (free)
    private static final double DOOR_WALL = 6.0;                         // the thickest wall it opens a Door in
    private static final int DOOR_THICK_SEQUENCE = 3;                    // Wanderer: walls up to...
    private static final double DOOR_THICK_WALL = 16.0;
    private static final int DOOR_COMPANY_SEQUENCE = 7;                  // Astrologer: allies this close come through too
    private static final double DOOR_COMPANY = 2.0;
    static final int UNBAR_SEQUENCE = 4;                         // Secrets Sorcerer: Door Opening and Slip Free
                                                                         // open Teleport roots on you
    // sneak: Slip Free
    static final int SLIP_COST = 400;
    private static final int SLIP_COOLDOWN = 400;
    private static final double SLIP_RANGE = 12.0;
    static final int SLIP_GUARD = 30;                            // nothing of that kind takes again for this long

    // Ability 3 - Star Divination (Sequence 7)
    static final int ASTROLOGY_SEQUENCE = 7;
    private static final int DIVINATION_COST = 1250;
    private static final int DIVINATION_COOLDOWN = 240;
    private static final double DIVINATION_RANGE = 48.0;
    private static final int DIMENSION_SIGHT_SEQUENCE = 2;               // Planeswalker: Dimension Sight...
    private static final double DIMENSION_SIGHT_RANGE = 128.0;           // ...this far, and your Starlit seen anywhere
    private static final double SWEEP_RADIUS = 40.0;                     // aimed at nothing: every creature this close...
    private static final int SWEEP_TICKS = 160;                          // ...shows as a star through walls this long
    // sneak: Interference
    private static final int INTERFERENCE_COST = 1250;
    private static final int INTERFERENCE_COOLDOWN = 600;
    private static final int INTERFERENCE_TICKS = 400;
    private static final double INTERFERENCE_RANGE = 8.0;                // nothing further than this picks you out
    private static final int TURNED_BACK_TICKS = 120;                    // the first search turned back: its caster Starlit

    // The Starlit mark (status/Starlit)
    public static final int STARLIT_TICKS = 240;
    private static final double STARLIT_SEEN = 64.0;                     // you and your allies see your marks this close

    // Passives (docs/KEY_OF_STARS.md, "Passives")
    private static final float FOOTPRINTS_SHORTER = 0.75f;               // Apprentice: roots and holds on you last 25% less
    private static final int NIMBLE_SEQUENCE = 8;                        // Trickmaster: no fall damage under...
    private static final double NIMBLE_FALL = 6.0;
    private static final int INTUITION_SEQUENCE = 7;                     // Astrologer: a chime when something starts hunting
    private static final int INTUITION_REST = 60;
    static final int SAFE_LANDING_SEQUENCE = 5;                          // Traveler: no fall damage for a while after a
    static final int SAFE_LANDING_TICKS = 40;                    // teleport of this Pathway; Anchors shown
    private static final int STARLIT_EYES_SEQUENCE = 4;                  // Secrets Sorcerer: blindness and nausea do not take
    private static final int WORM_AURA_EVERY = 200;                      // the Worms of Star are drawn again this often
    private static final int ASTRAL_SEQUENCE = 3;                        // Wanderer: Teleport roots last half as long, no fall
    private static final float ASTRAL_SHORTER = 0.5f;                    // damage at all
    private static final int AFTERIMAGES_SEQUENCE = 2;                   // Planeswalker: this share of the hits on you miss
    private static final float AFTERIMAGES_MISS = 0.15f;
    private static final int LOST_POSITION_SEQUENCE = 1;                 // Key of Stars: nothing further than this picks you
    private static final double LOST_POSITION_RANGE = 24.0;              // out unless it has you Starlit
    private static final int DOOR_OF_DOORS_SEQUENCE = 0;                 // Door: Door Opening, Escape Trick, Blink are free,
    private static final float DOOR_OF_DOORS_LONGER = 1.5f;              // and your roots last this many times as long

    // =========================
    // Names and what is saved on the player (game times are "until" this tick)
    // =========================
    static final String ANCHOR = "key_of_stars_anchor";                  // status/Anchors
    private static final String INTERFERENCE_TURNS = "key_of_stars_interference_turns";   // the first search is turned back
    private static final String INTERFERENCE_SHOWN = "key_of_stars_interference_shown";
    private static final String INTUITION_READY = "key_of_stars_intuition_ready";
    private static final String WORMS_SHOWN = "key_of_stars_worms_shown";

    private static final String DOOR_KEY = "key_of_stars_door";          // combat/Cooldowns
    private static final String SLIP_KEY = "key_of_stars_slip";
    private static final String DIVINATION_KEY = "key_of_stars_divination";
    private static final String INTERFERENCE_KEY = "key_of_stars_interference";


    // =========================
    // Events
    // =========================

    @Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class KeyOfStarsEvents {

        // (here, not in KeyOfStars: the event bus loads this class when the mod starts, so the rules are in from the start)
        static {
            // Footprints of Freedom (9): roots and holds on you last a quarter less; Astral Footing (3): Teleport roots half
            Root.addDurationScale((target, restriction) -> {
                if (!(target instanceof Player player) || !isKeyOfStars(player, DOOR_SEQUENCE)) return 1f;
                float scale = FOOTPRINTS_SHORTER;
                if (restriction == RootRestriction.TELEPORT && isKeyOfStars(player, ASTRAL_SEQUENCE)) scale *= ASTRAL_SHORTER;
                return scale;
            });
            // Lost Position (1): abilities cannot pick you out from further than LOST_POSITION_RANGE unless you are Starlit by them
            Spells.addUnchoosableCheck((chooser, target) -> target instanceof Player player && chooser != null && chooser != player
                    && isKeyOfStars(player, LOST_POSITION_SEQUENCE) && chooser.distanceTo(player) > LOST_POSITION_RANGE
                    && !Starlit.has(player, chooser));
            // Interference: the first search that is turned away from you is turned back, and its caster is Starlit
            Unreadable.addTurnedAwayListener((chooser, target) -> {
                if (!(target instanceof Player player) || !(player.level() instanceof ServerLevel sl)) return;
                if (player.getPersistentData().getLongOr(INTERFERENCE_TURNS, 0L) <= sl.getGameTime()) return;
                player.getPersistentData().remove(INTERFERENCE_TURNS);
                Later.run(sl, 1, () -> {                                     // after the aiming that asked is done
                    if (!chooser.isAlive() || chooser.level() != sl) return;
                    KeyOfStarsFx.interferenceTurned(sl, player, chooser);
                    if (Spells.isEnemy(player, chooser)) starlit(player, chooser, TURNED_BACK_TICKS);
                });
            });
            // a Starlit mark ended: its look comes off
            Starlit.addListener((caster, target) -> {
                if (caster == null || target == null || !(target.level() instanceof ServerLevel sl)) return;
                KeyOfStarsFx.starlitEnd(sl, target, starlitTag(caster, target));
            });
            // every cast anyone makes: a watching Scribe records it (Quick Study glints); Omnipresence and the
            // Planeswalker's Blink afterimages repeat their own Beyonder's
            Casts.addListener((caster, sl, cast) -> {
                KeyOfStarsRecord.seen(caster, sl, cast);
                if (!SoulCore.getPathway(caster).equals(PATHWAY)) return;
                KeyOfStarsStarlight.echo(caster, sl, cast);
                KeyOfStarsTravel.echo(caster, sl, cast);
            });
            // what an echo never repeats from elsewhere: this Pathway's travels and what shapes the world round you
            Echo.addSkip(cast -> cast.pathway().equals(PATHWAY) && ((cast.key() == 1 && !cast.sneak()) || cast.key() == 4
                    || (cast.entry() != null && (KeyOfStarsTravel.isTravel(cast.entry().key())
                    || KeyOfStarsTricks.isTravel(cast.entry().key()) || KeyOfStarsSecrets.isOwnSpace(cast.entry().key())
                    || KeyOfStarsStarlight.isWorldShaping(cast.entry().key())))));
            // Alternate World: roots on the enemies inside last twice as long
            Root.addDurationScale((target, restriction) -> KeyOfStarsStarlight.worldHoldScale(target));
            // a Starlight Seal mended itself
            Seal.addListener((target, sl) -> KeyOfStarsFx.sealRepaired(sl, target));
        }

        @SubscribeEvent
        public static void onKeyOfStarsTick(TickEvent.PlayerTickEvent.Post event) {
            Player player = event.player();
            if (!(player.level() instanceof ServerLevel sl) || !SoulCore.getPathway(player).equals(PATHWAY)) return;
            KeyOfStarsTravel.tick(player, sl);
            KeyOfStarsSecrets.tick(player, sl);
            KeyOfStarsStarlight.tick(player, sl);
            if (player.tickCount % 20 == 0) {
                starlitTick(player, sl);
                anchorsTick(player);
                interferenceTick(player, sl);
                KeyOfStarsRecord.tick(player, sl);
            }
            if (isKeyOfStars(player, STARLIT_EYES_SEQUENCE) && player.tickCount % WORM_AURA_EVERY == 0) {
                KeyOfStarsFx.wormAura(sl, player, WORM_AURA_EVERY + 20, wormsTag(player));
                player.getPersistentData().putBoolean(WORMS_SHOWN, true);
            }
        }

        /** Nimble (8): no fall damage under NIMBLE_FALL blocks; Astral Footing (3): none at all. */
        @SubscribeEvent
        public static boolean onKeyOfStarsFall(LivingFallEvent event) {
            if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) return false;
            if (isKeyOfStars(player, ASTRAL_SEQUENCE)) return true;
            return isKeyOfStars(player, NIMBLE_SEQUENCE) && event.getDistance() < NIMBLE_FALL;
        }

        /**
         * Footprints of Freedom (9): sweet berry bushes and powder snow do not hurt. Afterimages (2): a share of the hits
         * from an attacker pass through an illusory door and miss. Omnipresence (0): a hit may strike an afterimage instead.
         */
        @SubscribeEvent
        public static boolean onKeyOfStarsAttacked(LivingAttackEvent event) {
            if (!(event.getEntity() instanceof Player player) || !(player.level() instanceof ServerLevel sl)) return false;
            if (!isKeyOfStars(player, DOOR_SEQUENCE)) return false;
            DamageSource source = event.getSource();
            if (source.is(DamageTypes.SWEET_BERRY_BUSH)) return true;
            if (source.is(DamageTypes.FREEZE) && player.level().getBlockState(player.blockPosition()).is(Blocks.POWDER_SNOW)) return true;
            if (source.getEntity() != null && source.getEntity() != player && KeyOfStarsStarlight.afterimageTakes(player, sl)) return true;
            if (isKeyOfStars(player, AFTERIMAGES_SEQUENCE) && source.getEntity() != null && source.getEntity() != player
                    && player.getRandom().nextFloat() < AFTERIMAGES_MISS) {
                KeyOfStarsFx.afterimageMiss(sl, player);
                return true;
            }
            return false;
        }

        /** Starlit Eyes (4): blindness and nausea do not take. */
        @SubscribeEvent
        public static void onKeyOfStarsEffect(MobEffectEvent.Applicable event) {
            if (!(event.getEntity() instanceof Player player) || !(player.level() instanceof ServerLevel sl)) return;
            if (!isKeyOfStars(player, STARLIT_EYES_SEQUENCE)) return;
            var effect = event.getEffectInstance().getEffect();
            if (effect != MobEffects.BLINDNESS && effect != MobEffects.NAUSEA) return;
            event.setResult(Result.DENY);
            KeyOfStarsFx.illusionBroken(sl, player);
        }

        /** Spiritual Intuition (7): a soft chime and a star the way of something that starts hunting you. */
        @SubscribeEvent
        public static void onKeyOfStarsHunted(LivingChangeTargetEvent event) {
            if (!(event.getNewTarget() instanceof ServerPlayer hunted) || event.getEntity().level().isClientSide()) return;
            if (!isKeyOfStars(hunted, INTUITION_SEQUENCE)) return;
            long now = hunted.level().getGameTime();
            if (hunted.getPersistentData().getLongOr(INTUITION_READY, 0L) > now) return;
            hunted.getPersistentData().putLong(INTUITION_READY, now + INTUITION_REST);
            KeyOfStarsFx.intuition(hunted, event.getEntity().getEyePosition());
        }

        // what was on the screen does not survive relogging or dying: start clean (a Wandering is ended where they are)
        @SubscribeEvent
        public static void onKeyOfStarsLogin(PlayerEvent.PlayerLoggedInEvent event) {
            clearState(event.getEntity());
        }

        @SubscribeEvent
        public static void onKeyOfStarsRespawn(PlayerEvent.PlayerRespawnEvent event) {
            clearState(event.getEntity());
        }
    }

    private static void clearState(Player player) {
        player.getPersistentData().remove(INTERFERENCE_TURNS);
        player.getPersistentData().remove(INTERFERENCE_SHOWN);
        player.getPersistentData().remove(WORMS_SHOWN);
        KeyOfStarsTravel.clearState(player);
        KeyOfStarsTricks.clearState(player);
        KeyOfStarsSecrets.clearState(player);
        KeyOfStarsStarlight.clearState(player);
        boolean mine = SoulCore.getPathway(player).equals(PATHWAY);
        if (mine) for (Status status : Status.values()) KeyOfStarsFx.statusEnd(player, status);   // a clock left running
        KeyOfStarsRecord.clearState(player);                                    // the pages are drawn again (or dropped)
    }

    // =========================
    // Ability 1 - Door Opening
    // Normal: Door Opening. Aimed at a real door, trapdoor or fence gate within DOOR_REAL_RANGE it swings it open (or
    //         shut), free. Else an illusory Door opens in the wall in front of you (no thicker than DOOR_WALL; Wanderer:
    //         DOOR_THICK_WALL) and you step through at KeyOfStarsFx.DOOR_OPENED. It goes even with a Teleport root on
    //         you (the book's Apprentice); a root on everything stops casting altogether. Astrologer: allies within
    //         DOOR_COMPANY come with you. Secrets Sorcerer: the Teleport roots on you open.
    // Sneak:  Slip Free. You, or the ally aimed at within SLIP_RANGE: every root (not a Teleport one until the Secrets
    //         Sorcerer), harmful effect, fire and control ends, and nothing of that kind takes for SLIP_GUARD
    //         (status/Unbound).
    // =========================
    public static void keyOfStarsDoorOpening(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!ready(player, bypassClassCheck, DOOR_SEQUENCE)) return;
        if (player.isShiftKeyDown()) slipFree(player, sl);
        else doorOpening(player, sl);
    }

    private static void doorOpening(Player player, ServerLevel sl) {
        if (swingRealDoor(player, sl)) return;                                  // free, no cooldown
        boolean free = freeTravel(player);
        if (!free && !Cooldowns.ready(player, DOOR_KEY, "Door Opening")) return;
        double thickness = Beyonder.hasSequence(player, DOOR_THICK_SEQUENCE) ? DOOR_THICK_WALL : DOOR_WALL;
        Teleport.WallPass pass = Teleport.throughWall(player, sl, thickness);
        if (pass == null) {
            player.sendSystemMessage(Component.literal("There is no wall here to open a Door in, or no room behind it."));
            return; // nothing spent
        }
        if (!free && !pay(player, DOOR_COST)) return;
        if (!free) Cooldowns.start(player, DOOR_KEY, DOOR_COOLDOWN);
        else KeyOfStarsFx.doorOfDoors(sl, player);
        if (Beyonder.hasSequence(player, UNBAR_SEQUENCE) && Root.ticksLeft(player, RootRestriction.TELEPORT) > 0) {
            Root.remove(player, RootRestriction.TELEPORT);
            KeyOfStarsFx.travelUnbarred(sl, player);
        }
        List<LivingEntity> company = Beyonder.hasSequence(player, DOOR_COMPANY_SEQUENCE)
                ? Spells.alliesAround(player, sl, player.position(), DOOR_COMPANY) : List.of();
        Vec3 exit = pass.exit();
        KeyOfStarsFx.doorOpening(sl, player, pass.face(), exit);
        Later.run(sl, KeyOfStarsFx.DOOR_OPENED, () -> {
            if (!Spells.casterStillHere(player, sl)) return;
            teleport(player, sl, exit, true);
            for (LivingEntity ally : company) {
                if (!ally.isAlive() || ally.level() != sl) continue;
                Vec3 spot = Teleport.standNear(ally, sl, exit, 2.0);
                if (spot != null && Teleport.of(ally).ignoringTeleportRoots().fallGuard(SAFE_LANDING_TICKS).go(sl, spot)) {
                    KeyOfStarsFx.doorStep(sl, ally, spot);
                }
            }
        });
    }

    /** A real door, trapdoor or fence gate aimed at within DOOR_REAL_RANGE swings open (or shut). True if there was one. */
    private static boolean swingRealDoor(Player player, ServerLevel sl) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(DOOR_REAL_RANGE));
        BlockHitResult hit = sl.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (hit.getType() != HitResult.Type.BLOCK) return false;
        BlockPos pos = hit.getBlockPos();
        BlockState state = sl.getBlockState(pos);
        if (!(state.getBlock() instanceof DoorBlock || state.getBlock() instanceof TrapDoorBlock
                || state.getBlock() instanceof FenceGateBlock) || !state.hasProperty(BlockStateProperties.OPEN)) return false;
        boolean open = !state.getValue(BlockStateProperties.OPEN);
        sl.setBlock(pos, state.setValue(BlockStateProperties.OPEN, open), 3);   // a door's other half follows by itself
        KeyOfStarsFx.realDoor(sl, Vec3.atCenterOf(pos));
        return true;
    }

    private static void slipFree(Player player, ServerLevel sl) {
        if (!Cooldowns.ready(player, SLIP_KEY, "Slip Free")) return;
        LivingEntity ally = Spells.aimAlly(player, sl, SLIP_RANGE);
        LivingEntity target = ally != null ? ally : player;
        if (!pay(player, SLIP_COST)) return;
        Cooldowns.start(player, SLIP_KEY, SLIP_COOLDOWN);
        Unbound.free(target, SLIP_GUARD, Beyonder.hasSequence(player, UNBAR_SEQUENCE));
        KeyOfStarsFx.slipFree(sl, player, target);
    }

    // =========================
    // Ability 3 - Astrology
    // Normal: Star Divination. A crystal ball of starlight in the hand. The enemy aimed at within DIVINATION_RANGE
    //         (Planeswalker: DIMENSION_SIGHT_RANGE) is Starlit for STARLIT_TICKS at KeyOfStarsFx.DIVINED and read to
    //         you: its health, its Pathway and Sequence, every effect on it, the last power it cast. Aimed at nothing: every creature within
    //         SWEEP_RADIUS shows as a small star through walls for SWEEP_TICKS, for you alone.
    // Sneak:  Interference. For INTERFERENCE_TICKS you are Unreadable from further than INTERFERENCE_RANGE (status/
    //         Unreadable: abilities cannot pick you, mobs further away forget you). The first search turned away from
    //         you is turned back: its caster is Starlit for TURNED_BACK_TICKS.
    // =========================
    public static void keyOfStarsAstrology(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!ready(player, bypassClassCheck, ASTROLOGY_SEQUENCE)) return;
        if (player.isShiftKeyDown()) interference(player, sl);
        else starDivination(player, sl);
    }

    private static void starDivination(Player player, ServerLevel sl) {
        if (!Cooldowns.ready(player, DIVINATION_KEY, "Star Divination")) return;
        double range = Beyonder.hasSequence(player, DIMENSION_SIGHT_SEQUENCE) ? DIMENSION_SIGHT_RANGE : DIVINATION_RANGE;
        LivingEntity target = Spells.aimEnemy(player, sl, range);
        if (!pay(player, DIVINATION_COST)) return;
        Cooldowns.start(player, DIVINATION_KEY, DIVINATION_COOLDOWN);
        KeyOfStarsFx.starDivination(sl, player, target);
        if (target == null) {
            if (!(player instanceof ServerPlayer owner)) return;
            for (LivingEntity one : sl.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(SWEEP_RADIUS),
                    other -> other != player && other.isAlive() && other.distanceTo(player) <= SWEEP_RADIUS)) {
                KeyOfStarsFx.smallStar(owner, one, SWEEP_TICKS);
            }
            return;
        }
        if (target instanceof ServerPlayer watched && isKeyOfStars(watched, INTUITION_SEQUENCE)) KeyOfStarsFx.divinationFelt(watched);
        Later.run(sl, KeyOfStarsFx.DIVINED, () -> {
            if (!Spells.casterStillHere(player, sl) || !target.isAlive() || target.level() != sl) return;
            starlit(player, target, STARLIT_TICKS);
            player.sendSystemMessage(reading(target));
        });
    }

    /** What the crystal ball tells about a creature. */
    private static Component reading(LivingEntity target) {
        StringBuilder text = new StringBuilder();
        text.append(target.getName().getString()).append(": ")
                .append(Math.round(target.getHealth())).append(" / ").append(Math.round(target.getMaxHealth())).append(" health");
        if (Beyonders.isBeyonder(target)) {                                  // a player or a Beyonder mob
            text.append(", ").append(Beyonders.pathwayOf(target)).append(" Sequence ").append(Beyonders.sequenceOf(target));
        }
        List<String> effects = new ArrayList<>();
        for (MobEffectInstance effect : target.getActiveEffects()) {
            effects.add(effect.getEffect().value().getDisplayName().getString() + " " + (effect.getAmplifier() + 1));
        }
        if (!effects.isEmpty()) text.append(". Effects: ").append(String.join(", ", effects));
        Casts.Cast last = Casts.last(target);                                   // combat/Casts: what it cast last
        if (last != null && target.level() instanceof ServerLevel sl) {
            text.append(". Last power: ").append(last.name()).append(", ").append(Math.max(1, last.age(sl) / 20)).append("s ago");
        }
        return Component.literal(text.append('.').toString());
    }

    private static void interference(Player player, ServerLevel sl) {
        if (!Cooldowns.ready(player, INTERFERENCE_KEY, "Interference")) return;
        if (!pay(player, INTERFERENCE_COST)) return;
        Cooldowns.start(player, INTERFERENCE_KEY, INTERFERENCE_COOLDOWN);
        Unreadable.apply(player, INTERFERENCE_TICKS, INTERFERENCE_RANGE);
        player.getPersistentData().putLong(INTERFERENCE_TURNS, sl.getGameTime() + INTERFERENCE_TICKS);
        player.getPersistentData().putBoolean(INTERFERENCE_SHOWN, true);
        String tag = interferenceTag(player);
        KeyOfStarsFx.interferenceEnd(sl, player, tag);                          // cast again: one veil, not two
        KeyOfStarsFx.interference(sl, player, INTERFERENCE_TICKS, tag);
    }

    /** Once a second: static drifts off while Unreadable; the veil comes off when it ends. */
    private static void interferenceTick(Player player, ServerLevel sl) {
        if (!player.getPersistentData().getBooleanOr(INTERFERENCE_SHOWN, false)) return;
        if (Unreadable.isUnreadable(player)) {
            KeyOfStarsFx.interferenceTick(sl, player);
            return;
        }
        player.getPersistentData().remove(INTERFERENCE_SHOWN);
        player.getPersistentData().remove(INTERFERENCE_TURNS);
        KeyOfStarsFx.interferenceEnd(sl, player, interferenceTag(player));
    }

    private static String interferenceTag(Player player) {
        return "ks_interference_" + player.getId();
    }

    // =========================
    // Abilities 4, 6 and 7 live in their own classes; the keys (network/SKeyPressAbility...Used) call these.
    // =========================
    public static void keyOfStarsRecord(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        KeyOfStarsRecord.keyOfStarsRecord(player, level, sl, bypassClassCheck);
    }

    public static void keyOfStarsSecrets(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        KeyOfStarsSecrets.keyOfStarsSecrets(player, level, sl, bypassClassCheck);
    }

    public static void keyOfStarsStarlight(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        KeyOfStarsStarlight.keyOfStarsStarlight(player, level, sl, bypassClassCheck);
    }

    // =========================
    // The Starlit mark, Star Anchors, the worms
    // =========================

    /** 'caster' Starlits 'target' for 'ticks' (status/Starlit) and its mark rises over it. Only on enemies. */
    public static void starlit(Player caster, LivingEntity target, int ticks) {
        if (!(target.level() instanceof ServerLevel sl) || !Spells.isEnemy(caster, target)) return;
        if (!Starlit.mark(target, caster, ticks)) return;                       // (sealed connections: status/Seal)
        String tag = starlitTag(caster, target);
        KeyOfStarsFx.starlitEnd(sl, target, tag);                               // marked again: one mark, not two
        KeyOfStarsFx.starlit(sl, caster, target, ticks, tag);
    }

    static String starlitTag(Player caster, LivingEntity target) {
        return "ks_starlit_" + caster.getUUID() + "_" + target.getId();
    }

    /**
     * Once a second: the caster and their allies (players within STARLIT_SEEN) see the caster's Starlit through walls
     * while near it. Dimension Sight (Planeswalker) and Lost Position (Key of Stars): the caster sees theirs anywhere.
     */
    private static void starlitTick(Player player, ServerLevel sl) {
        List<LivingEntity> marked = Starlit.marked(player);
        if (marked.isEmpty()) return;
        boolean anywhere = Beyonder.hasSequence(player, DIMENSION_SIGHT_SEQUENCE);
        List<ServerPlayer> viewers = new ArrayList<>();
        if (player instanceof ServerPlayer self) viewers.add(self);
        for (ServerPlayer other : sl.players()) {
            if (other != player && other.distanceTo(player) <= STARLIT_SEEN && Spells.isFriend(player, other)) viewers.add(other);
        }
        for (LivingEntity target : marked) {
            if (target.level() != sl) continue;
            for (ServerPlayer viewer : viewers) {
                boolean near = viewer.distanceTo(target) <= STARLIT_SEEN;
                if (near) KeyOfStarsFx.starlitSeen(viewer, target);
                else if (anywhere && viewer == player) KeyOfStarsFx.dimensionSight(viewer, target);
            }
        }
    }

    /** Sense of Direction (Traveler): once a second, a small star the way of each of your Star Anchors here. */
    private static void anchorsTick(Player player) {
        if (!(player instanceof ServerPlayer owner) || !isKeyOfStars(player, SAFE_LANDING_SEQUENCE)) return;
        for (Anchors.Anchor anchor : Anchors.list(player, ANCHOR)) {
            if (anchor.isIn(player.level())) KeyOfStarsFx.anchorDirection(owner, anchor.at());
        }
    }

    private static String wormsTag(Player player) {
        return "ks_worms_" + player.getId();
    }

    // =========================
    // What a cast costs, how long a cooldown is (every key and wheel entry goes through these)
    // =========================

    /** True for a Key Of Stars at that Sequence or stronger. */
    public static boolean isKeyOfStars(Player player, int sequence) {
        return SoulCore.getPathway(player).equals(PATHWAY) && Beyonder.hasSequence(player, sequence);
    }

    /** The key's gate: the Pathway (unless a stolen cast), the Sequence, and not while Wandering. */
    static boolean ready(Player player, boolean bypassClassCheck, int sequence) {
        if (!bypassClassCheck && !SoulCore.getPathway(player).equals(PATHWAY)) return false;
        if (!Beyonder.hasSequence(player, sequence)) return false;
        if (KeyOfStarsTravel.isWandering(player)) {
            player.sendSystemMessage(Component.literal("Nothing can be cast from the Astral World."));
            return false;
        }
        return true;
    }

    /**
     * What a key's cast of 'cost' really costs this player now. The one place for the rules that change only a Key Of
     * Stars' own costs.
     */
    public static int costFor(Player player, int cost) {
        return cost;
    }

    /** What a wheel entry of 'cost' costs: the Secrets Sorcerer's quick powers after a Blink make it 30% cheaper. */
    public static int wheelCostFor(Player player, int cost) {
        if (cost <= 0 || Beyonder.isCastingUnbound(player)) return cost;
        return KeyOfStarsTravel.isQuickened(player) ? Math.round(cost * KeyOfStarsTravel.QUICK_COST) : costFor(player, cost);
    }

    /** A wheel cooldown of 'ticks' for this player now: halved by the quick powers after a Blink. */
    public static int cooldownFor(Player player, int ticks) {
        return KeyOfStarsTravel.isQuickened(player) ? Math.round(ticks * KeyOfStarsTravel.QUICK_COOLDOWN) : ticks;
    }

    /** Pays for a key's cast (after costFor). False, with the usual message, if it cannot. */
    static boolean pay(Player player, int cost) {
        int due = costFor(player, cost);
        return due <= 0 || Spells.paySpirituality(player, due);
    }

    /** Door of Doors (Door): Door Opening, Escape Trick and Blink cost nothing and have no cooldown. */
    static boolean doorOfDoors(Player player) {
        return isKeyOfStars(player, DOOR_OF_DOORS_SEQUENCE);
    }

    /**
     * Door Opening, Escape Trick, Blink and Traveler's Door cost nothing and have no cooldown now: Door of Doors, or
     * an Alternate World on your side (its Open Doors rule).
     */
    static boolean freeTravel(Player player) {
        return doorOfDoors(player) || (isKeyOfStars(player, DOOR_SEQUENCE) && KeyOfStarsStarlight.inFriendlyWorld(player));
    }

    /** How long a root this Beyonder puts on someone lasts: Door of Doors makes it DOOR_OF_DOORS_LONGER. */
    static int rootTicks(Player caster, int ticks) {
        return isKeyOfStars(caster, DOOR_OF_DOORS_SEQUENCE) ? Math.round(ticks * DOOR_OF_DOORS_LONGER) : ticks;
    }

    /** Fall damage spared after one of this Pathway's teleports: Safe Landing from the Traveler. */
    static int safeLanding(Player player) {
        return isKeyOfStars(player, SAFE_LANDING_SEQUENCE) ? SAFE_LANDING_TICKS : 0;
    }

    /** The Pathway's own teleport (combat/Teleport) with Safe Landing's fall guard. ignoreRoots: Door Opening's. */
    static boolean teleport(Player player, ServerLevel sl, Vec3 feet, boolean ignoreRoots) {
        Teleport move = Teleport.of(player).fallGuard(safeLanding(player));
        if (ignoreRoots) move.ignoringTeleportRoots();
        return move.go(sl, feet);
    }
}
