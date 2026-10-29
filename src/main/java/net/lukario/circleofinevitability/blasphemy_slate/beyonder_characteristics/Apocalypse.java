package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonders;
import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonder;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.SoulCore;
import net.lukario.circleofinevitability.combat.Contagion;
import net.lukario.circleofinevitability.combat.HudTimer;
import net.lukario.circleofinevitability.combat.Later;
import net.lukario.circleofinevitability.combat.Reel;
import net.lukario.circleofinevitability.combat.SpellFx;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.combat.Zone;
import net.lukario.circleofinevitability.menu.AbilityWheel;
import net.lukario.circleofinevitability.particles.fx.ModelFx;
import net.lukario.circleofinevitability.particles.fx.ParticleShapes;
import net.lukario.circleofinevitability.root.Freeze;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.lukario.circleofinevitability.screen.ScreenAnchor;
import net.lukario.circleofinevitability.screen.ScreenImages;
import net.lukario.circleofinevitability.status.Concealment;
import net.lukario.circleofinevitability.status.DamageLink;
import net.lukario.circleofinevitability.status.DeathWard;
import net.lukario.circleofinevitability.status.EmpoweredHit;
import net.lukario.circleofinevitability.status.FallGuard;
import net.lukario.circleofinevitability.status.Marionette;
import net.lukario.circleofinevitability.status.NoHeal;
import net.lukario.circleofinevitability.status.StoredDamage;
import net.lukario.circleofinevitability.status.Substitute;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Apocalypse: the Demoness pathway of Lord of the Mysteries, played as an assassin. Seven keys in the Attendant
 * Of Mysteries pattern, and every ability has a normal cast and a sneak cast:
 *
 *   1 Fatal Step       (Assassin)    behind the target you look at; your next hit is a backstab | sneak: leave a mirror shard
 *   2 Black Flame      (Instigator)  a bolt of cold black fire that burns, slows and creeps on  | sneak: Mirror Substitute
 *   3 Mirror World     (Witch)       step into a mirror, hidden and fast; press again to come out where you aim
 *                                                                                             | sneak (inside): back out where you went in
 *   4 Silk Weaver      (Pleasure)    the silk wheel: Silk Snare, Web Swing, Trip-web, Cocoon, Threads of Life,
 *                                    Web of Fate                                             | sneak: the last silk again
 *   5 Plague           (Affliction)  a disease that jumps to the nearest enemies, stronger each jump | sneak: Curse
 *   6 Unaging          (Despair)     your next death within 60 s is undone                     | sneak: every mirror shard you placed explodes
 *   7 Catastrophe      (Unaging)     the catastrophe wheel: Earthquake, Hailstorm, Tornado, Hopeless Ice Age,
 *                                    Black Death, Black Flame Rain, Doomsday Sky, where you aim | sneak: the last one again
 *
 * Mirror shards (key 1 sneak) charge while enemies stand in front of them: the longer they stand there, the worse
 * it gets for them, and the bigger the shard's blast when key 6 (sneak) sets them all off.
 * Every ability you cast adds a stack (the pictures next to the hotbar); the fifth calls down your last
 * catastrophe around you, 35% harder (below Sequence 3, with none unlocked, a black flame burst: see unleashStacks).
 * The two wheels are AbilityWheel (menu/): every entry its own Sequence, cost and cooldown, shown on the wheel.
 *
 * The look of every ability is in DemonessFx (see /demonessfx). What is reusable lives outside this class,
 * so other classes can use it too (docs/SPELL_KIT.md): combat/Contagion (the plague), status/StoredDamage
 * (the curse), status/EmpoweredHit (the backstab), menu/AbilityWheel (keys 4 and 7), combat/Reel (Web Swing,
 * the Web of Fate's pull), status/NoHeal (the Ice Age), and the older Zone, Substitute, DeathWard, Concealment,
 * FallGuard, DamageLink, Marionette.
 *
 * Nothing here breaks blocks or lights real fire: the black flame is cold. The numbers for every ability are
 * right below.
 */
public class Apocalypse {

    static final String PATHWAY = "Apocalypse";

    // =========================
    // Ability settings (cost = spirituality, ticks: 20 = 1 second, distances in blocks)
    // =========================

    // Stacks: every ability used adds one picture next to the hotbar, the 5th one calls down a catastrophe
    static final int MAX_STACKS = 20;
    static final float STACK_CATASTROPHE_POWER = 1.35f;  // how strong the 5th stack's catastrophe is (1 = as cast)
    static final float STACK_BURST_DAMAGE = 40f;         // no catastrophe unlocked yet: a black flame burst
    static final double STACK_BURST_RADIUS = 8.0;
    static final int STACK_BURN_TICKS = 80;
    // Pictures for the stacks, from assets/circle_of_inevitability/textures/gui/ (without .png). One is picked at random.
    // (The Attendant's orb until the Demoness has a picture of her own.)
    static final List<String> STACK_IMAGES = List.of(
            "attendant_stack_orb"
    );

    // Ability 1 - Fatal Step (Sequence 9)
    static final int STEP_COST = 250;
    static final double STEP_RANGE = 20.0;
    static final double STEP_BEHIND = 1.1;               // how far behind the target's back you come out
    static final int BACKSTAB_WINDOW = 40;               // ticks your next hit stays a backstab
    static final float BACKSTAB_CRIT = 1.5f;             // the backstab always crits...
    static final float BACKSTAB_BONUS = 16f;             // ...and does this much more on top
    // sneak: a mirror shard
    static final int SHARD_COST = 250;
    static final int SHARD_TICKS = 1200;                 // how long a shard stands
    static final int SHARD_MAX = 3;                      // shards one player can have; a 4th takes the oldest away
    static final double SHARD_REACH = 7.0;               // how far in front of it an enemy charges it
    static final double SHARD_FRONT = 55.0;              // degrees to either side of its front that still count
    static final float SHARD_CHARGE_PER_SECOND = 0.08f;  // for each enemy standing in front of it (full = 1)
    static final float SHARD_WEAK_AT = 0.34f;            // from this charge, those in front are also weakened
    static final float SHARD_DREAD_AT = 0.67f;           // from this charge, they are blinded and bleed
    static final float SHARD_DREAD_DAMAGE = 3f;          // each second, once it is that charged
    // what the shard does when key 6 (sneak) sets it off: from an uncharged one to a full one
    static final float SHARD_MIN_DAMAGE = 12f;
    static final float SHARD_MAX_DAMAGE = 70f;
    static final int SHARD_FULL_ROOT_TICKS = 50;         // a full shard holds what it hit in place

    // Ability 2 - Black Flame (Sequence 8)
    static final int FLAME_COST = 1250;
    static final double FLAME_RANGE = 28.0;
    static final float FLAME_DAMAGE = 22f;               // the bolt
    static final int FLAME_BURN_TICKS = 100;             // how long the black flames burn on what it hit
    static final float FLAME_BURN_DAMAGE = 3f;           // each second while it burns
    static final int FLAME_SLOW_LEVEL = 1;               // 0 = Slowness I, 1 = Slowness II while it burns
    static final int FLAME_FROST_TICKS = 12;             // the cold of it: frozen solid for a moment (0 = never)
    static final double FLAME_SPLASH = 1.6;              // no enemy under the crosshair: hits the first one this close to where it lands
    static final double CREEP_RADIUS = 5.0;              // it creeps to enemies this close to the one it hit...
    static final int CREEP_COUNT = 3;                    // ...this many at most...
    static final float CREEP_POWER = 0.5f;               // ...and burns them this much less hard
    // sneak: Mirror Substitute
    static final int SUBSTITUTE_COST = 1250;
    static final int SUBSTITUTE_TICKS = 200;             // how long the mirror copy waits for a hit
    static final double SUBSTITUTE_ESCAPE = 5.0;         // how far you slip away
    static final int SUBSTITUTE_HIDE_TICKS = 20;         // and you are unseen for this long after
    static final float SUBSTITUTE_GLASS_DAMAGE = 14f;    // the broken glass flying back at the attacker

    // Ability 3 - Mirror World (Sequence 7)
    static final int MIRROR_COST = 1250;
    static final int MIRROR_TICKS = 100;                 // how long you can stay inside
    static final int MIRROR_SPEED_LEVEL = 2;             // 0 = Speed I ... while inside
    static final double MIRROR_EXIT_RANGE = 28.0;        // how far away you can come out where you aim
    static final double RETURN_BURST_RADIUS = 3.5;       // sneak: coming back out where you went in
    static final float RETURN_BURST_DAMAGE = 14f;

    // Ability 4 - Silk Weaver (Sequence 6): a wheel of six silks, each with its own cost and cooldown (the wheel
    // shows both). Sneak: the last silk again, without the wheel. The tiers are the design doc's.
    static final int SILK_LOW_COST = 1250;
    static final int SILK_MEDIUM_COST = 2500;
    static final int SILK_HIGH_COST = 5000;
    static final int SILK_VERY_HIGH_COST = 10000;
    static final int SILK_LOW_COOLDOWN = 40;             // ticks before that silk can be cast again
    static final int SILK_MEDIUM_COOLDOWN = 120;
    static final int SILK_HIGH_COOLDOWN = 240;
    static final int SILK_VERY_HIGH_COOLDOWN = 400;
    // Silk Snare (Sequence 6, low)
    static final double SNARE_RANGE = 24.0;
    static final double SNARE_RADIUS = 2.5;              // a web 5 blocks across
    static final int SNARE_TICKS = 400;                  // it lies there 20 s
    static final int SNARE_ROOT_TICKS = 40;              // whoever walks in is held 2 s...
    static final int SNARE_SLOW_LEVEL = 3;               // ...then slowed hard while in it (3 = Slowness IV)
    static final int SNARE_MAX = 3;                      // snares one player can have; a 4th takes the oldest away
    // Web Swing (Sequence 6, low)
    static final double SWING_RANGE = 28.0;
    static final double SWING_SPEED = 1.6;               // blocks per tick you are drawn along the silk
    static final float SWING_LIGHT_HEALTH = 40f;         // a mob with at most this much max health is pulled to you;
                                                                 // a heavier one pulls you to it
    static final double SWING_PULL_SPEED = 1.2;
    static final int SWING_FALL_GUARD = 200;             // no fall damage on landing, for at most 10 s
    // Trip-web (Sequence 5, medium): cast once to tie one end where you aim, again to tie the other end (free)
    static final double TRIP_RANGE = 20.0;
    static final double TRIP_LENGTH = 16.0;              // the most there can be between the two ends
    static final int TRIP_TIE_TICKS = 200;               // time to tie the second end before the first comes loose
    static final int TRIP_TICKS = 1200;                  // the line waits a minute
    static final float TRIP_DAMAGE = 24f;                // the first enemy through it falls...
    static final int TRIP_DOWN_TICKS = 40;               // ...and lies there 2 s
    static final int TRIP_MAX = 2;                       // lines one player can have; a 3rd snaps the oldest
    // Cocoon (Sequence 5, medium)
    static final double COCOON_RANGE = 16.0;
    static final int COCOON_HOLD_TICKS = 80;             // the enemy you look at is held 4 s
    static final int COCOON_SELF_TICKS = 100;            // no enemy there: yourself, 5 s, rooted but tough...
    static final int COCOON_SELF_RESISTANCE = 2;         // ...Resistance III (60% less damage)...
    static final int COCOON_SELF_REGENERATION = 2;       // ...and Regeneration III
    // Threads of Life (Sequence 4, high)
    static final double THREADS_RANGE = 16.0;
    static final int THREADS_TICKS = 100;                // 5 s of draining
    static final int THREADS_EVERY = 10;
    static final float THREADS_DRAIN = 5f;               // taken every half second, and given to you
    static final int SERVANT_TICKS = 600;                // a mob the threads kill rises as your servant for 30 s
    static final float SERVANT_MAX_HEALTH = 150f;        // not bosses: they just die
    static final int SERVANT_RESISTANCE = 2;             // stone-skinned: Resistance III
    // Web of Fate (Sequence 3, very high). Cast again while it hangs, standing under it: everything tangled is
    // drawn to the middle and the web snaps (free).
    static final double FATE_RANGE = 28.0;
    static final double FATE_RADIUS = 6.0;               // 12 blocks across
    static final double FATE_HEIGHT = 3.5;               // how high over the ground it hangs
    static final int FATE_TICKS = 160;                   // 8 s
    static final float FATE_SHARE = 0.5f;                // the tangled share half of the damage any one of them takes
    static final int FATE_SLOW_LEVEL = 1;                // while tangled
    static final float FATE_SNAP_DAMAGE = 40f;           // the recast: the snap...
    static final double FATE_SNAP_RADIUS = 3.5;          // ...hits these too, tangled or not...
    static final int FATE_STUN_TICKS = 30;               // ...and stuns for 1.5 s

    // Ability 5 - Plague (Sequence 5)
    static final int PLAGUE_COST = 3000;
    static final double PLAGUE_RANGE = 24.0;
    static final int PLAGUE_TICKS = 100;                 // how long one carries the mark before it jumps
    static final float PLAGUE_DAMAGE = 5f;               // each second, on the first one
    static final float PLAGUE_GROWTH = 1.5f;             // each jump makes it this much stronger
    static final int PLAGUE_JUMPS_MIN = 2;               // it jumps to 2 to 3 of the nearest enemies
    static final int PLAGUE_JUMPS_MAX = 3;
    static final double PLAGUE_REACH = 8.0;
    static final int PLAGUE_GENERATIONS = 4;             // the 4th one to carry it passes it on no more
    static final int PLAGUE_MAX_VICTIMS = 24;            // in all, for one cast
    // sneak: Curse
    static final int CURSE_COST = 3000;
    static final double CURSE_RANGE = 24.0;
    static final int CURSE_TICKS = 200;                  // everything it takes for 10 s is stored...
    static final float CURSE_REPLAY = 1.0f;              // ...and hits it again at once, this much of it
    static final float CURSE_FULL = 200f;                // stored damage at which the eye throbs its hardest (only the look)

    // Ability 6 - Unaging (Sequence 4)
    static final int UNAGING_COST = 6000;
    static final int UNAGING_TICKS = 1200;               // the next death within 60 s is cancelled
    static final float UNAGING_HEALTH = 0.5f;            // you come back with half your health
    static final int UNAGING_HIDE_TICKS = 40;            // and unseen for this long
    // sneak: every placed shard explodes
    static final int SHATTER_COST = 6000;

    // Ability 7 - Catastrophe (Sequence 3): a wheel of seven disasters, each cast where you aim (sneak: the last one
    // again). The fifth stack calls the last one down around you, STACK_CATASTROPHE_POWER times as hard.
    static final int DISASTER_MEDIUM_COST = 12000;
    static final int DISASTER_HIGH_COST = 20000;
    static final int DISASTER_VERY_HIGH_COST = 30000;
    static final int DISASTER_MEDIUM_COOLDOWN = 300;     // 15 s
    static final int DISASTER_HIGH_COOLDOWN = 400;
    static final int DISASTER_VERY_HIGH_COOLDOWN = 600;
    static final double DISASTER_RANGE = 40.0;
    // Earthquake (Sequence 3)
    static final double QUAKE_RADIUS = 8.0;
    static final float QUAKE_DAMAGE = 30f;
    static final double QUAKE_LIFT = 0.9;                // thrown up this hard...
    static final int QUAKE_STAGGER_TICKS = 30;           // ...and staggered (held, slowed) when they come down
    // Hailstorm (Sequence 3)
    static final double HAIL_RADIUS = 7.0;
    static final int HAIL_TICKS = 120;                   // 6 s
    static final int HAIL_PER_SECOND = 10;               // hailstones (the look)
    static final float HAIL_DAMAGE = 3f;                 // every half second on whoever is under it
    static final int HAIL_SLOW_LEVEL = 1;
    // Tornado (Sequence 2)
    static final int TORNADO_TICKS = 160;                // 8 s
    static final double TORNADO_SPEED = 0.12;            // blocks per tick it travels the way you look
    static final double TORNADO_PULL_RADIUS = 7.0;       // enemies this close are drawn in...
    static final double TORNADO_PULL_SPEED = 0.35;
    static final double TORNADO_CORE = 2.0;              // ...this close they are lifted and whirled round...
    static final double TORNADO_TOP = 6.0;               // ...up to this high...
    static final int TORNADO_HOLD = 30;                  // ...and after this long thrown out
    static final float TORNADO_SPIN_DAMAGE = 2f;         // every half second while whirled
    static final float TORNADO_THROW_DAMAGE = 22f;
    static final double TORNADO_THROW = 1.6;
    // Hopeless Ice Age (Sequence 2)
    static final double ICE_RADIUS = 9.0;
    static final int ICE_TICKS = 200;                    // 10 s
    static final int ICE_FROZEN_AFTER = 100;             // slower every second inside, frozen solid after 5 s
    static final int ICE_FREEZE_TICKS = 60;
    static final float ICE_DAMAGE = 2f;                  // every second; and no healing, and weakness
    // Black Death (Sequence 2)
    static final double DEATH_RADIUS = 3.0;              // the cloud starts this wide...
    static final double DEATH_GROWTH = 0.8;              // ...and grows this much with everyone it infects (no limit)
    static final int DEATH_TICKS = 240;                  // 12 s
    static final float DEATH_DAMAGE = 5f;               // every second inside
    // Black Flame Rain (Sequence 1)
    static final double RAIN_RADIUS = 8.0;
    static final int RAIN_TICKS = 160;                   // 8 s
    static final int RAIN_EVERY = 2;                     // a drop every 2 ticks
    static final float RAIN_AIMED = 0.5f;                // part of the drops that fall on an enemy (the rest at random)
    static final double RAIN_HIT_RADIUS = 1.6;
    static final float RAIN_DAMAGE = 10f;                // each drop; it also burns and puts the Curse on (key 5 sneak)
    static final int RAIN_BURN_TICKS = 60;
    // Doomsday Sky (Sequence 1)
    static final double DOOM_RADIUS = 14.0;
    static final int DOOM_TICKS = 300;                   // 15 s
    static final int DOOM_EVERY = 6;                     // something falls every 6 ticks...
    static final double DOOM_SPREAD = 2.0;               // ...this close to an enemy picked at random
    static final float METEOR_DAMAGE = 30f;
    static final float DOOM_FLAME_DAMAGE = 12f;
    static final float LIGHTNING_DAMAGE = 18f;

    // =========================
    // Saved on the player
    // =========================
    static final String STACKS = "apocalypse_stacks";
    static final String STACK_IMAGE_KEY = "apocalypse_stack_image_";      // + slot number
    static final String STACK_IMAGE_ID = "apo_stack_";                    // screen image ids
    static final String MIRROR_LEFT = "apocalypse_mirror_ticks";          // > 0 while inside the mirror world
    static final String MIRROR_X = "apocalypse_mirror_x";                 // where you went in
    static final String MIRROR_Y = "apocalypse_mirror_y";
    static final String MIRROR_Z = "apocalypse_mirror_z";
    static final String SNARE_NEXT = "apocalypse_snare_next";             // which of the SNARE_MAX snares is next

    // Names of things on other entities (models, empowered hits)
    static final String BACKSTAB = "apocalypse_backstab";
    static final String BURN_TAG = "apocalypse_black_flame";
    static final String SUBSTITUTE_TAG = "apocalypse_substitute_";        // + player
    static final String PLAGUE_TAG = "apocalypse_plague";
    static final String CURSE_TAG = "apocalypse_curse";
    static final String WARD_TAG = "apocalypse_unaging";
    static final String SHARD_TAG = "apocalypse_shard_";                  // + a number for each shard
    static final String SNARE_TAG = "apocalypse_snare_";                  // + a number; also the snares' zone names
    static final String SNARE_HOLD_TAG = "apocalypse_snared";
    static final String TRIP_TAG = "apocalypse_trip_";                    // + a number for each line
    static final String TRIP_TIMER = "apocalypse_trip";
    static final String COCOON_TAG = "apocalypse_cocoon";
    static final String FATE_ZONE = "apocalypse_web_of_fate";
    static final String FATE_TAG = "apocalypse_fate_";                    // + player
    static final String FATE_MARK_TAG = "apocalypse_fate_tangle";
    static final String DISASTER_TAG = "apocalypse_disaster_";            // + a number: zones and clouds
    static final String DEATH_MARK_TAG = "apocalypse_black_death";

    // =========================
    // Mirror shards lying around (not saved: a world that closes takes its shards with it)
    // =========================
    static final class Shard {
        final ServerLevel level;
        final UUID owner;
        final Vec3 ground;
        final float yaw;
        final String tag;
        final long expires;
        float charge;                                            // 0..1

        Shard(ServerLevel level, UUID owner, Vec3 ground, float yaw, String tag, long expires) {
            this.level = level;
            this.owner = owner;
            this.ground = ground;
            this.yaw = yaw;
            this.tag = tag;
            this.expires = expires;
        }
    }

    static final List<Shard> SHARDS = new ArrayList<>();
    private static long shardNumber = 0;

    // =========================
    // Silks lying around (not saved either)
    // =========================

    /** The first end of a Trip-web, waiting for its second. */
    private record Knot(ServerLevel level, Vec3 at, long expires, String tag) {}

    static final Map<UUID, Knot> TRIP_KNOTS = new HashMap<>();

    /** A Trip-web stretched between two points, waiting for someone to walk through it. */
    static final class TripLine {
        final LivingEntity owner;
        final ServerLevel level;
        final Vec3 a;
        final Vec3 b;
        final long expires;
        final String tag;
        boolean snapped;

        TripLine(LivingEntity owner, ServerLevel level, Vec3 a, Vec3 b, long expires, String tag) {
            this.owner = owner;
            this.level = level;
            this.a = a;
            this.b = b;
            this.expires = expires;
            this.tag = tag;
        }
    }

    static final List<TripLine> TRIP_LINES = new ArrayList<>();

    /** A Web of Fate hanging over the ground, and who it has tangled. */
    static final class Fate {
        final ServerLevel level;
        final Vec3 ground;
        final Vec3 center;
        final long expires;
        final String tag;
        final List<LivingEntity> tangled = new ArrayList<>();
        boolean snapped;

        Fate(ServerLevel level, Vec3 ground, Vec3 center, long expires, String tag) {
            this.level = level;
            this.ground = ground;
            this.center = center;
            this.expires = expires;
            this.tag = tag;
        }
    }

    static final Map<UUID, Fate> FATES = new HashMap<>();
    private static long silkNumber = 0;                          // makes the tags of webs and lines unique
    private static long disasterNumber = 0;

    @Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ApocalypseEvents {

        @SubscribeEvent
        public static void onApocalypseTick(TickEvent.PlayerTickEvent.Post event) {
            Player player = event.player();
            if (!(player.level() instanceof ServerLevel sl)) return;

            // the mirror world runs out even if the pathway is taken away in the meantime
            mirrorWorldTick(player, sl);
            // the shards charge once a second
            if (player.tickCount % 20 == 0) chargeShards(player, sl);
        }

        // pictures on screen don't survive relogging/respawning, so redraw them from the saved stack count
        @SubscribeEvent
        public static void onApocalypseLogin(PlayerEvent.PlayerLoggedInEvent event) {
            refreshStackImages(event.getEntity());
            event.getEntity().getPersistentData().remove(MIRROR_LEFT);     // the concealment wears off by itself
        }

        @SubscribeEvent
        public static void onApocalypseRespawn(PlayerEvent.PlayerRespawnEvent event) {
            refreshStackImages(event.getEntity());
            event.getEntity().getPersistentData().remove(MIRROR_LEFT);
        }

        @SubscribeEvent
        public static void onApocalypseLevelUnload(LevelEvent.Unload event) {
            if (!(event.getLevel() instanceof ServerLevel)) return;     // (the players' side unloads its own copy too)
            SHARDS.removeIf(shard -> shard.level == event.getLevel());
            TRIP_LINES.removeIf(line -> line.level == event.getLevel());
            TRIP_KNOTS.values().removeIf(knot -> knot.level() == event.getLevel());
            FATES.values().removeIf(fate -> fate.level == event.getLevel());
        }

        @SubscribeEvent
        public static void onApocalypseLogout(PlayerEvent.PlayerLoggedOutEvent event) {
            TRIP_KNOTS.remove(event.getEntity().getUUID());
            FATES.remove(event.getEntity().getUUID());                  // its zone closes by itself
        }
    }

    // =========================
    // Ability 1 - Fatal Step
    // =========================
    public static void apocalypseFatalStep(LivingEntity player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!ready(player, bypassClassCheck, 9)) return;

        if (player.isShiftKeyDown()) {
            if (!Spells.paySpirituality(player, SHARD_COST)) return;
            leaveMirrorWorld(player, sl);
            placeShard(player, sl);
        } else {
            if (Root.has(player, RootRestriction.TELEPORT)) {
                Spells.tell(player, Component.literal("You cannot leave this place."));
                return;
            }
            LivingEntity target = Spells.aimEnemy(player, sl, STEP_RANGE);
            if (target == null) {
                Spells.tell(player, Component.literal("No one stands before you."));
                return; // nothing spent
            }
            Vec3 behind = spotBehind(player, sl, target);
            if (behind == null) {
                Spells.tell(player, Component.literal("There is no room behind them."));
                return; // nothing spent
            }
            if (!Spells.paySpirituality(player, STEP_COST)) return;
            leaveMirrorWorld(player, sl);
            fatalStep(player, sl, target, behind);
        }

        addStack(player, sl);
    }

    /** Where to stand right behind the target's back, or null if there is no room there or near it. */
    private static Vec3 spotBehind(LivingEntity player, ServerLevel sl, LivingEntity target) {
        Vec3 facing = target.getLookAngle();
        facing = new Vec3(facing.x, 0, facing.z);
        if (facing.lengthSqr() < 1.0E-4) facing = target.position().subtract(player.position()).multiply(-1, 0, -1);
        if (facing.lengthSqr() < 1.0E-4) facing = Spells.flatLook(player).scale(-1);
        Vec3 back = facing.normalize().scale(-1);
        double distance = target.getBbWidth() * 0.5 + STEP_BEHIND;
        Vec3 spot = target.position().add(back.scale(distance));
        if (Spells.fits(player, sl, spot)) return spot;
        return Spells.freeSpotNear(player, sl, target.position(), back, distance + 0.6);
    }

    // Normal: gone in shadow, out of it behind the target, facing its back. For BACKSTAB_WINDOW ticks
    // your next hit is a backstab: it always crits and does BACKSTAB_BONUS more.
    private static void fatalStep(LivingEntity player, ServerLevel sl, LivingEntity target, Vec3 behind) {
        Vec3 from = player.position();
        DemonessFx.vanish(sl, player);
        player.teleportTo(behind.x, behind.y, behind.z);
        player.fallDistance = 0;
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.lookAt(EntityAnchorArgument.Anchor.EYES, target.getEyePosition());
        }
        DemonessFx.appear(sl, player);
        Spells.sound(sl, from, SoundEvents.ENDERMAN_TELEPORT, 0.5f, 1.4f);
        Spells.sound(sl, behind, SoundEvents.ILLUSIONER_CAST_SPELL, 0.6f, 1.6f);

        EmpoweredHit.armAny(player, BACKSTAB, BACKSTAB_WINDOW, (attacker, victim, amount, level) -> {
            backstabLook(level, attacker, victim);
            return amount * BACKSTAB_CRIT + BACKSTAB_BONUS;
        });
    }

    /** The backstab lands: the dagger's thrust, violet and red sparks out of the wound. */
    private static void backstabLook(ServerLevel sl, LivingEntity attacker, LivingEntity target) {
        Vec3 into = target.position().add(0, target.getBbHeight() * 0.55, 0);
        Vec3 hand = attacker.position().add(0, attacker.getBbHeight() * 0.6, 0);
        Vec3 along = into.subtract(hand);
        along = along.lengthSqr() < 1.0E-4 ? attacker.getLookAngle() : along.normalize();
        ParticleShapes.slashBetween(sl, DemonessFx.STAB.lifetime(6).sweep(2), hand, into.add(along.scale(0.4)));
        ParticleShapes.cone(sl, DemonessFx.VIOLET_MOTE, into, along, 70, 14, 0.08, 0.3);
        ParticleShapes.cone(sl, DemonessFx.BLOOD_MOTE, into, along, 50, 8, 0.05, 0.2);
        Spells.sound(sl, into, SoundEvents.PLAYER_ATTACK_CRIT, 1f, 0.8f);
    }

    // Sneak: a shard of dark mirror breaks out of the ground where you stand, its front the way you look.
    // It stands for SHARD_TICKS and charges while enemies stand in front of it (see chargeShards);
    // key 6 (sneak) sets every shard off (see shatterShards).
    private static void placeShard(LivingEntity player, ServerLevel sl) {
        long now = sl.getGameTime();
        SHARDS.removeIf(shard -> shard.expires < shard.level.getGameTime());
        List<Shard> mine = shardsOf(player, sl);
        while (mine.size() >= SHARD_MAX) {                     // the oldest sinks back into the ground
            Shard oldest = mine.remove(0);
            SHARDS.remove(oldest);
            ParticleShapes.clearModels(sl, oldest.ground, oldest.tag);
            ParticleShapes.burst(sl, DemonessFx.GLASS_GLINT, oldest.ground.add(0, 0.6, 0), 8, 0.04, 0.15);
        }

        Vec3 ground = Spells.groundAt(player, sl, player.position());
        float yaw = player.getYRot();
        String tag = SHARD_TAG + (shardNumber++);
        SHARDS.add(new Shard(sl, player.getUUID(), ground, yaw, tag, now + SHARD_TICKS));
        DemonessFx.placeShard(sl, ground, yaw, SHARD_TICKS, tag);
        Spells.sound(sl, ground, SoundEvents.AMETHYST_CLUSTER_PLACE, 1f, 0.6f);
        Spells.sound(sl, ground, SoundEvents.GLASS_BREAK, 0.4f, 1.6f);
    }

    /** The player's shards in this level that are still standing, oldest first. */
    private static List<Shard> shardsOf(LivingEntity player, ServerLevel sl) {
        List<Shard> mine = new ArrayList<>();
        long now = sl.getGameTime();
        for (Shard shard : SHARDS) {
            if (shard.level == sl && shard.owner.equals(player.getUUID()) && shard.expires >= now) mine.add(shard);
        }
        return mine;
    }

    /**
     * Once a second: every shard of this player charges for each enemy standing in front of it, and does to them
     * what its charge allows: slowed at once, also weakened from SHARD_WEAK_AT, blinded and bleeding from SHARD_DREAD_AT.
     */
    static void chargeShards(LivingEntity player, ServerLevel sl) {
        if (SHARDS.isEmpty()) return;
        SHARDS.removeIf(shard -> shard.expires < shard.level.getGameTime());
        for (Shard shard : shardsOf(player, sl)) {
            List<LivingEntity> watching = inFrontOf(player, sl, shard);
            if (watching.isEmpty()) continue;
            shard.charge = Math.min(1f, shard.charge + SHARD_CHARGE_PER_SECOND * watching.size());
            for (LivingEntity enemy : watching) {
                boolean dread = shard.charge >= SHARD_DREAD_AT;
                enemy.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 30, dread ? 1 : 0));
                if (shard.charge >= SHARD_WEAK_AT) enemy.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 30, 0));
                if (dread) {
                    enemy.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
                    Spells.strike(player, enemy, SHARD_DREAD_DAMAGE);
                }
            }
            DemonessFx.shardCharge(sl, shard.ground, shard.yaw, shard.charge, shard.tag);
            Spells.sound(sl, shard.ground, SoundEvents.AMETHYST_BLOCK_CHIME, 0.5f, 0.5f + shard.charge);
        }
    }

    /** The enemies standing in front of a shard: close enough, and within SHARD_FRONT degrees of the way it faces. */
    private static List<LivingEntity> inFrontOf(LivingEntity player, ServerLevel sl, Shard shard) {
        double angle = Math.toRadians(shard.yaw);
        Vec3 front = new Vec3(-Math.sin(angle), 0, Math.cos(angle));
        double cosLimit = Math.cos(Math.toRadians(SHARD_FRONT));
        List<LivingEntity> found = new ArrayList<>();
        for (LivingEntity enemy : Spells.enemiesAround(player, sl, shard.ground.add(0, 0.9, 0), SHARD_REACH)) {
            Vec3 to = enemy.position().subtract(shard.ground);
            to = new Vec3(to.x, 0, to.z);
            if (to.lengthSqr() < 0.04 || to.normalize().dot(front) >= cosLimit) found.add(enemy);
        }
        return found;
    }

    // =========================
    // Ability 2 - Black Flame
    // =========================
    public static void apocalypseBlackFlame(LivingEntity player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!ready(player, bypassClassCheck, 8)) return;

        if (player.isShiftKeyDown()) {
            if (!Spells.paySpirituality(player, SUBSTITUTE_COST)) return;
            leaveMirrorWorld(player, sl);
            readyMirrorSubstitute(player, sl);
        } else {
            if (!Spells.paySpirituality(player, FLAME_COST)) return;
            leaveMirrorWorld(player, sl);
            blackFlame(player, sl);
        }

        addStack(player, sl);
    }

    // Normal: a bolt of black flame at the enemy you look at (or where you aim). What it hits burns cold:
    // it is hurt, slowed and frozen for a moment, keeps burning for FLAME_BURN_TICKS, and the flame creeps
    // on to the enemies around it.
    private static void blackFlame(LivingEntity player, ServerLevel sl) {
        Vec3 hand = Spells.clearStart(player, sl, SpellFx.handOf(player, sl));
        LivingEntity target = Spells.aimEnemy(player, sl, FLAME_RANGE);
        Vec3 to = target != null ? target.getBoundingBox().getCenter() : Spells.aimPoint(player, sl, FLAME_RANGE);
        int lands = DemonessFx.blackFlameBolt(sl, hand, to);
        Spells.sound(sl, hand, SoundEvents.BLAZE_SHOOT, 0.8f, 0.5f);

        Later.run(sl, lands, () -> {
            if (!Spells.casterStillHere(player, sl)) return;
            LivingEntity hit = target != null && target.isAlive() && target.level() == sl ? target : null;
            if (hit == null) {
                List<LivingEntity> near = Spells.enemiesAround(player, sl, to, FLAME_SPLASH);
                if (!near.isEmpty()) hit = near.get(0);
            }
            Vec3 at = hit != null ? hit.getBoundingBox().getCenter() : to;
            DemonessFx.blackFlameHit(sl, at);
            Spells.sound(sl, at, SoundEvents.FIRECHARGE_USE, 0.9f, 0.5f);
            Spells.sound(sl, at, SoundEvents.FIRE_EXTINGUISH, 0.6f, 0.6f);
            if (hit == null) return;

            Spells.strike(player, hit, FLAME_DAMAGE);
            if (FLAME_FROST_TICKS > 0 && hit.isAlive()) Freeze.apply(hit, FLAME_FROST_TICKS);
            burnBlack(player, sl, hit, FLAME_BURN_TICKS, 1f);
            creepFrom(player, sl, hit);
        });
    }

    /**
     * Black flames on an entity for 'ticks' ticks: FLAME_BURN_DAMAGE * power every second and slowness while they
     * burn. Cold fire: nothing is really lit. Burning it again starts the time over.
     */
    private static void burnBlack(LivingEntity player, ServerLevel sl, LivingEntity target, int ticks, float power) {
        if (!target.isAlive()) return;
        DemonessFx.burn(sl, target, ticks, BURN_TAG);
        long until = sl.getGameTime() + ticks;
        target.getPersistentData().putLong(BURN_TAG, until);    // a newer burn takes over from this one
        burnTick(player, sl, target, until, power);
    }

    private static void burnTick(LivingEntity player, ServerLevel sl, LivingEntity target, long until, float power) {
        Spells.keepEffect(target, MobEffects.SLOWNESS, FLAME_SLOW_LEVEL, 30);
        if (sl.getGameTime() + 20 > until) return;
        Later.run(sl, 20, () -> {
            if (!target.isAlive() || target.level() != sl || !Spells.casterStillHere(player, sl)) return;
            if (target.getPersistentData().getLongOr(BURN_TAG, 0L) != until) return;   // burnt again since
            Spells.strike(player, target, FLAME_BURN_DAMAGE * power);
            ParticleShapes.burst(sl, DemonessFx.EMBER_BLACK, target.getBoundingBox().getCenter(), 4, 0.02, 0.08);
            burnTick(player, sl, target, until, power);
        });
    }

    /** The flame creeps from the one it hit to up to CREEP_COUNT enemies near it, which burn less hard. */
    private static void creepFrom(LivingEntity player, ServerLevel sl, LivingEntity source) {
        Vec3 center = source.getBoundingBox().getCenter();
        List<LivingEntity> near = new ArrayList<>(Spells.enemiesAround(player, sl, center, CREEP_RADIUS));
        near.remove(source);
        near.sort((a, b) -> Double.compare(a.distanceToSqr(center), b.distanceToSqr(center)));
        for (int i = 0; i < Math.min(CREEP_COUNT, near.size()); i++) {
            LivingEntity next = near.get(i);
            DemonessFx.creep(sl, source, next);
            Later.run(sl, DemonessFx.CREEP_TICKS, () -> {
                if (!next.isAlive() || next.level() != sl || !Spells.casterStillHere(player, sl)) return;
                Spells.strike(player, next, FLAME_DAMAGE * CREEP_POWER);
                burnBlack(player, sl, next, FLAME_BURN_TICKS, CREEP_POWER);
            });
        }
    }

    // Sneak - Mirror Substitute: for SUBSTITUTE_TICKS the next hit lands on a mirror copy instead of you (see mirrorEscape).
    private static void readyMirrorSubstitute(LivingEntity player, ServerLevel sl) {
        Substitute.arm(player, SUBSTITUTE_TICKS, 1, Apocalypse::mirrorEscape);
        Vec3 middle = player.getBoundingBox().getCenter();
        ParticleShapes.burst(sl, DemonessFx.GLASS_GLINT, middle, 14, 0.04, 0.16);
        ParticleShapes.burst(sl, DemonessFx.VIOLET_MOTE, middle, 12, 0.03, 0.12);
        SpellFx.risingRings(sl, player.position(), 0.9, 2.0, 3, 0xC0D27BFF);
        Spells.sound(sl, player.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 1f, 1.4f);
    }

    /**
     * Instead of the hit: a mirror stands where you stood and takes it, you slip away unseen for a moment,
     * and the mirror shatters, its glass flying back at the attacker.
     */
    private static void mirrorEscape(LivingEntity saved, DamageSource source, float amount, ServerLevel sl) {
        if (!(saved instanceof Player player)) return;
        Vec3 was = player.position();
        Entity attacker = source.getEntity();
        String tag = SUBSTITUTE_TAG + player.getUUID();
        DemonessFx.mirrorSubstitute(sl, player, DemonessFx.MIRROR_RISES + 4, tag);

        boolean held = Root.has(player, RootRestriction.TELEPORT);      // bound in place: the hit is still lost, but you stay
        Vec3 away = attacker == null ? Spells.flatLook(player).scale(-1) : was.subtract(attacker.position());
        Vec3 to = held ? null : Spells.freeSpotNear(player, sl, was, away, SUBSTITUTE_ESCAPE);
        if (to != null) {
            player.teleportTo(to.x, to.y, to.z);
            player.fallDistance = 0;
        }
        Concealment.hide(player, SUBSTITUTE_HIDE_TICKS);
        Spells.sound(sl, was, SoundEvents.ENDERMAN_TELEPORT, 0.5f, 1.8f);

        Later.run(sl, DemonessFx.MIRROR_RISES, () -> {
            Vec3 toward = attacker != null && attacker.isAlive() && attacker.level() == sl
                    ? attacker.getBoundingBox().getCenter()
                    : was.add(Spells.flatLook(player).scale(3)).add(0, 1.2, 0);
            DemonessFx.mirrorShatter(sl, was, toward, tag);
            Spells.sound(sl, was, SoundEvents.GLASS_BREAK, 1.2f, 0.8f);
            if (!(attacker instanceof LivingEntity enemy)) return;
            Later.run(sl, DemonessFx.SLIVERS_FLY, () -> {
                if (!Spells.casterStillHere(player, sl)) return;
                if (!enemy.isAlive() || enemy.level() != sl || !Spells.isEnemy(player, enemy)) return;
                Spells.strike(player, enemy, SUBSTITUTE_GLASS_DAMAGE);
            });
        });
    }

    // =========================
    // Ability 3 - Mirror World
    // =========================
    public static void apocalypseMirrorWorld(LivingEntity player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!ready(player, bypassClassCheck, 7)) return;

        // inside: the key is the way out, and costs nothing
        if (inMirrorWorld(player)) {
            if (player.isShiftKeyDown()) returnFromMirror(player, sl);
            else exitWhereYouAim(player, sl);
            return;
        }

        if (!Spells.paySpirituality(player, MIRROR_COST)) return;
        enterMirrorWorld(player, sl);
        addStack(player, sl);
    }

    // You step into a mirror: hidden and fast for MIRROR_TICKS. Press the key again to come out where you aim,
    // sneak-press it to come back out where you went in. Time up: you come out where you are.
    private static void enterMirrorWorld(LivingEntity player, ServerLevel sl) {
        Vec3 entry = player.position();
        CompoundTag data = player.getPersistentData();
        data.putInt(MIRROR_LEFT, MIRROR_TICKS);
        data.putDouble(MIRROR_X, entry.x);
        data.putDouble(MIRROR_Y, entry.y);
        data.putDouble(MIRROR_Z, entry.z);

        DemonessFx.mirrorEnter(sl, player);
        Concealment.hide(player, MIRROR_TICKS + 5);              // a little longer: the mirror world ends it (mirrorWorldTick)
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, MIRROR_TICKS, MIRROR_SPEED_LEVEL, false, false));
        Spells.sound(sl, entry, SoundEvents.AMETHYST_BLOCK_RESONATE, 1f, 0.6f);
        Spells.sound(sl, entry, SoundEvents.ENDERMAN_TELEPORT, 0.5f, 0.5f);
    }

    public static boolean inMirrorWorld(LivingEntity player) {
        return player.getPersistentData().getIntOr(MIRROR_LEFT, 0) > 0;
    }

    /** Normal press inside: out of a mirror where the crosshair is (or where you are, if you may not go there). */
    static void exitWhereYouAim(LivingEntity player, ServerLevel sl) {
        Vec3 there = Root.has(player, RootRestriction.TELEPORT) ? null : Spells.blinkSpot(player, sl, MIRROR_EXIT_RANGE);
        if (there != null) {
            player.teleportTo(there.x, there.y, there.z);
            player.fallDistance = 0;
            FallGuard.protect(player, 60);                          // out of a mirror over a drop
        }
        exitHere(player, sl);
    }

    /** Sneak press inside: back out where you went in, the mirror bursting into glass that cuts whoever is near. */
    static void returnFromMirror(LivingEntity player, ServerLevel sl) {
        CompoundTag data = player.getPersistentData();
        Vec3 entry = new Vec3(data.getDoubleOr(MIRROR_X, player.getX()), data.getDoubleOr(MIRROR_Y, player.getY()),
                data.getDoubleOr(MIRROR_Z, player.getZ()));
        if (Root.has(player, RootRestriction.TELEPORT) || !Spells.fits(player, sl, entry)) {
            exitHere(player, sl);
            return;
        }
        endMirrorWorld(player);
        player.teleportTo(entry.x, entry.y, entry.z);
        player.fallDistance = 0;
        DemonessFx.mirrorReturn(sl, entry, player.getYRot());
        Later.run(sl, DemonessFx.SLIVERS_FLY, () -> {
            if (!Spells.casterStillHere(player, sl)) return;
            for (LivingEntity target : Spells.enemiesAround(player, sl, entry.add(0, 1.0, 0), RETURN_BURST_RADIUS)) {
                Spells.strike(player, target, RETURN_BURST_DAMAGE);
            }
        });
        Spells.sound(sl, entry, SoundEvents.GLASS_BREAK, 1.2f, 0.7f);
    }

    /** Out of a mirror right where you are. */
    private static void exitHere(LivingEntity player, ServerLevel sl) {
        endMirrorWorld(player);
        DemonessFx.mirrorExit(sl, player.position(), player.getYRot());
        Spells.sound(sl, player.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 1f, 1.2f);
    }

    /** The mirror world is over: seen again, normal speed. */
    private static void endMirrorWorld(LivingEntity player) {
        player.getPersistentData().remove(MIRROR_LEFT);
        if (Concealment.isHidden(player)) Concealment.reveal(player);
        removeMirrorSpeed(player);
    }

    /** Takes away the speed the mirror world gave, and only that (not a speed potion they drank). */
    private static void removeMirrorSpeed(LivingEntity player) {
        MobEffectInstance speed = player.getEffect(MobEffects.SPEED);
        if (speed != null && speed.getAmplifier() == MIRROR_SPEED_LEVEL && speed.getDuration() <= MIRROR_TICKS) {
            player.removeEffect(MobEffects.SPEED);
        }
    }

    /** Casting anything else pulls you out of the mirror world, where you are. */
    static void leaveMirrorWorld(LivingEntity player, ServerLevel sl) {
        if (inMirrorWorld(player)) exitHere(player, sl);
    }

    /** Every tick: counts the mirror world down, and lets you out when it runs out. */
    static void mirrorWorldTick(LivingEntity player, ServerLevel sl) {
        CompoundTag data = player.getPersistentData();
        int left = data.getIntOr(MIRROR_LEFT, 0);
        if (left <= 0) return;
        if (!Concealment.isHidden(player)) {                     // something pulled you out already (you attacked)
            data.remove(MIRROR_LEFT);
            removeMirrorSpeed(player);
            return;
        }
        if (left == 1) exitHere(player, sl);
        else data.putInt(MIRROR_LEFT, left - 1);
    }

    // =========================
    // Ability 4 - Silk Weaver
    // =========================

    // Icons are in assets/circle_of_inevitability/textures/gui/ability_menus/apocalypse/
    static final AbilityWheel SILKS = AbilityWheel.create("apocalypse_silk", "Silk Weaver")
            .radius(76)                                          // GUI pixels from the middle of the screen
            .quickCastCooldown(false)                            // sneak recast: no cooldown
            .entry("snare", "ability_menus/apocalypse/silk_snare", "Silk Snare", 6,
                    SILK_LOW_COST, SILK_LOW_COOLDOWN, Apocalypse::silkSnare)
            .entry("swing", "ability_menus/apocalypse/web_swing", "Web Swing", 6,
                    SILK_LOW_COST, SILK_LOW_COOLDOWN, Apocalypse::webSwing)
            .entry("trip", "ability_menus/apocalypse/trip_web", "Trip-web", 5,
                    SILK_MEDIUM_COST, SILK_MEDIUM_COOLDOWN, Apocalypse::tripWeb)
            .free(Apocalypse::tyingTripWeb)                      // the second end is free
            .entry("cocoon", "ability_menus/apocalypse/cocoon", "Cocoon", 5,
                    SILK_MEDIUM_COST, SILK_MEDIUM_COOLDOWN, Apocalypse::cocoon)
            .entry("threads", "ability_menus/apocalypse/threads_of_life", "Threads of Life", 4,
                    SILK_HIGH_COST, SILK_HIGH_COOLDOWN, Apocalypse::threadsOfLife)
            .entry("fate", "ability_menus/apocalypse/web_of_fate", "Web of Fate", 3,
                    SILK_VERY_HIGH_COST, SILK_VERY_HIGH_COOLDOWN, Apocalypse::webOfFate)
            .free(Apocalypse::fateHangs)                         // the recast that draws it in is free
            .onCast((player, sl, entry) -> {
                leaveMirrorWorld(player, sl);
                addStack(player, sl);
            });

    // Normal: the silk wheel. Sneak: the last silk again (no last silk yet: the wheel).
    // Each silk checks and pays for itself when it is picked (see AbilityWheel), so opening the wheel is free.
    public static void apocalypseSilkWeaver(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!ready(player, bypassClassCheck, 6)) return;
        if (player.isShiftKeyDown()) SILKS.quickCast(player);
        else SILKS.open(player);
    }

    // ---------- Silk Snare ----------

    // A wad of silk thrown where you aim opens into a flat web, almost unseen. Whoever walks in is held for
    // SNARE_ROOT_TICKS, then slowed hard for as long as they stay on it.
    static AbilityWheel.Result silkSnare(LivingEntity player, ServerLevel sl) {
        Vec3 hand = Spells.clearStart(player, sl, SpellFx.handOf(player, sl));
        Vec3 ground = Spells.aimGround(player, sl, SNARE_RANGE);
        String tag = SNARE_TAG + (silkNumber++);
        int lands = DemonessFx.silkSnare(sl, hand, ground, SNARE_RADIUS, SNARE_TICKS, tag);
        Spells.sound(sl, hand, SoundEvents.SPIDER_AMBIENT, 0.6f, 1.6f);
        Later.run(sl, lands, () -> {
            if (!Spells.casterStillHere(player, sl)) {
                ParticleShapes.clearModels(sl, ground, tag);
                return;
            }
            laySnare(player, sl, ground, tag);
        });
        return AbilityWheel.Result.DONE;
    }

    private static void laySnare(LivingEntity player, ServerLevel sl, Vec3 ground, String tag) {
        // one of SNARE_MAX zone names in turn: opening a zone with a name already open closes the old one
        int next = player.getPersistentData().getIntOr(SNARE_NEXT, 0);
        player.getPersistentData().putInt(SNARE_NEXT, (next + 1) % SNARE_MAX);
        Set<UUID> held = new HashSet<>();
        Spells.sound(sl, ground, SoundEvents.SPIDER_STEP, 0.8f, 0.6f);
        Zone.at(ground, SNARE_RADIUS)
                .height(1.5)
                .lasts(SNARE_TICKS)
                .every(5)
                .onEnter(victim -> {
                    if (!held.add(victim.getUUID())) return;            // held once; after that only slowed
                    Root.apply(victim, SNARE_ROOT_TICKS, RootRestriction.MOVEMENT);
                    DemonessFx.snareCatch(sl, victim, SNARE_HOLD_TAG);
                    Spells.sound(sl, victim.position(), SoundEvents.SPIDER_AMBIENT, 0.8f, 0.8f);
                })
                .onInside((victim, ticksInside) -> Spells.keepEffect(victim, MobEffects.SLOWNESS, SNARE_SLOW_LEVEL, 30))
                .onEnd(() -> ParticleShapes.clearModels(sl, ground, tag))
                .open(player, sl, SNARE_TAG + next);
    }

    // ---------- Web Swing ----------

    // Silk to what you look at. A block: you are drawn to it. A light mob: it is yanked to you. A heavy one:
    // you are drawn to it. No fall damage when you land.
    static AbilityWheel.Result webSwing(LivingEntity player, ServerLevel sl) {
        if (Root.has(player, RootRestriction.MOVE)) {
            Spells.tell(player, Component.literal("You cannot move."));
            return AbilityWheel.Result.REFUSED;
        }
        LivingEntity mob = Spells.aimEnemy(player, sl, SWING_RANGE);
        if (mob != null) {
            if (mob.getMaxHealth() <= SWING_LIGHT_HEALTH) {
                int ticks = swingTicks(player.distanceTo(mob), SWING_PULL_SPEED);
                DemonessFx.webPull(sl, player, mob, ticks);
                Later.run(sl, DemonessFx.SWING_SHOT, () -> {
                    if (!Spells.casterStillHere(player, sl)) return;
                    Reel.in(mob, () -> Spells.casterStillHere(player, sl)
                                    ? player.position().add(Spells.flatLook(player).scale(1.5)) : null)
                            .speed(SWING_PULL_SPEED).stopAt(1.0).lasts(ticks).start(sl);
                });
            } else {
                double stop = mob.getBbWidth() * 0.5 + 1.0;
                int ticks = swingTicks(player.distanceTo(mob), SWING_SPEED);
                DemonessFx.webPull(sl, player, mob, ticks);
                swingTo(player, sl, () -> mob.isAlive() && mob.level() == sl ? mob.position() : null, stop, ticks);
            }
            Spells.sound(sl, player.position(), SoundEvents.SPIDER_STEP, 1f, 1.4f);
            return AbilityWheel.Result.DONE;
        }

        Vec3 eye = player.getEyePosition();
        Vec3 anchor = Spells.aimPoint(player, sl, SWING_RANGE);
        if (eye.distanceTo(anchor) > SWING_RANGE - 0.05) {
            Spells.tell(player, Component.literal("There is nothing for the silk to catch on."));
            return AbilityWheel.Result.REFUSED;
        }
        int ticks = swingTicks(eye.distanceTo(anchor), SWING_SPEED);
        DemonessFx.webSwing(sl, player, anchor, ticks);
        // the middle of the body to the anchor, so a ceiling does not hold you by the feet
        Vec3 feetThere = anchor.subtract(0, player.getBbHeight() * 0.6, 0);
        swingTo(player, sl, () -> feetThere, 1.2, ticks);
        Spells.sound(sl, player.position(), SoundEvents.SPIDER_STEP, 1f, 1.4f);
        return AbilityWheel.Result.DONE;
    }

    /** How long being drawn 'distance' blocks takes, with time to spare. */
    private static int swingTicks(double distance, double speed) {
        return DemonessFx.SWING_SHOT + (int) Math.ceil(distance / speed) + 8;
    }

    /** The player is drawn along the silk once it has caught; no fall damage until they land. */
    private static void swingTo(LivingEntity player, ServerLevel sl, Supplier<Vec3> to, double stopAt, int ticks) {
        FallGuard.protect(player, SWING_FALL_GUARD);
        Later.run(sl, DemonessFx.SWING_SHOT, () -> {
            if (!Spells.casterStillHere(player, sl) || Root.has(player, RootRestriction.MOVE)) return;
            Reel.in(player, to).speed(SWING_SPEED).stopAt(stopAt).lasts(ticks).start(sl);
        });
    }

    // ---------- Trip-web ----------

    // Two casts: the first ties one end where you aim (this one costs), the second, within TRIP_TIE_TICKS, ties
    // the other end where you aim then (free). The first enemy to cross the line falls, is hurt, and it snaps.
    static AbilityWheel.Result tripWeb(LivingEntity player, ServerLevel sl) {
        Vec3 at = tripEnd(player, sl);
        Knot knot = liveKnot(player);
        if (knot == null) {
            String tag = TRIP_TAG + (silkNumber++);
            long expires = sl.getGameTime() + TRIP_TIE_TICKS;
            TRIP_KNOTS.put(player.getUUID(), new Knot(sl, at, expires, tag));
            ModelFx tie = DemonessFx.WEB.color(0xA0FFFFFF).scale(0.5f).lifetime(TRIP_TIE_TICKS).fade(4, 10).tag(tag);
            ParticleShapes.modelAlong(sl, tie, at, player.getEyePosition().subtract(at));
            ParticleShapes.burst(sl, DemonessFx.SILK_WISP, at, 8, 0.03, 0.1);
            if (player instanceof Player viewerOfHud) HudTimer.show(viewerOfHud, TRIP_TIMER, "Trip-web", TRIP_TIE_TICKS, 0xF0ECFF);
            Spells.tell(player, Component.literal("Aim at the other end and cast Trip-web again."));
            Spells.sound(sl, at, SoundEvents.SPIDER_STEP, 0.6f, 1.6f);
            return AbilityWheel.Result.DONE;
        }

        double length = at.distanceTo(knot.at());
        if (length > TRIP_LENGTH || length < 1.0) {
            Spells.tell(player, Component.literal(length < 1.0
                    ? "That is where the first end is."
                    : "Too far from the first end (" + (int) TRIP_LENGTH + " blocks at most)."));
            return AbilityWheel.Result.REFUSED;
        }
        TRIP_KNOTS.remove(player.getUUID());
        if (player instanceof Player viewerOfHud) HudTimer.hide(viewerOfHud, TRIP_TIMER);
        ParticleShapes.clearModels(sl, knot.at(), knot.tag());
        stretchTripLine(player, sl, knot.at(), at, knot.tag());
        return AbilityWheel.Result.FREE;
    }

    /** True while the player has tied one end of a Trip-web and may tie the other (the wheel lets it be free). */
    static boolean tyingTripWeb(LivingEntity player) {
        return liveKnot(player) != null;
    }

    private static Knot liveKnot(LivingEntity player) {
        Knot knot = TRIP_KNOTS.get(player.getUUID());
        if (knot == null) return null;
        if (knot.level() != player.level() || knot.expires() < player.level().getGameTime()) {
            TRIP_KNOTS.remove(player.getUUID());
            return null;
        }
        return knot;
    }

    /** Where an end goes: on what the crosshair is on, a little out from it and a little above the ground. */
    private static Vec3 tripEnd(LivingEntity player, ServerLevel sl) {
        Vec3 point = Spells.aimPoint(player, sl, TRIP_RANGE);
        return point.subtract(player.getLookAngle().scale(0.15)).add(0, 0.35, 0);
    }

    private static void stretchTripLine(LivingEntity player, ServerLevel sl, Vec3 a, Vec3 b, String tag) {
        long now = sl.getGameTime();
        TRIP_LINES.removeIf(line -> line.snapped || line.expires < line.level.getGameTime());
        List<TripLine> mine = new ArrayList<>();
        for (TripLine line : TRIP_LINES) {
            if (line.owner == player) mine.add(line);
        }
        while (mine.size() >= TRIP_MAX) {                       // the oldest snaps
            TripLine oldest = mine.remove(0);
            oldest.snapped = true;
            TRIP_LINES.remove(oldest);
            DemonessFx.tripSnap(oldest.level, oldest.a, oldest.b, oldest.tag);
        }

        TripLine line = new TripLine(player, sl, a, b, now + TRIP_TICKS, tag);
        TRIP_LINES.add(line);
        DemonessFx.tripWeb(sl, a, b, TRIP_TICKS, () -> !line.snapped && sl.getGameTime() <= line.expires, tag);
        Spells.sound(sl, b, SoundEvents.SPIDER_STEP, 0.6f, 1.8f);
        watchTripLine(line);
    }

    /** Every 2 ticks: has an enemy walked into the line? */
    private static void watchTripLine(TripLine line) {
        Later.run(line.level, 2, () -> {
            if (line.snapped) return;
            ServerLevel sl = line.level;
            if (!Spells.casterStillHere(line.owner, sl) || sl.getGameTime() > line.expires) {
                line.snapped = true;
                TRIP_LINES.remove(line);
                ParticleShapes.clearModels(sl, line.a, line.tag);
                return;
            }
            LivingEntity victim = Spells.firstEnemyOnLine(line.owner, sl, line.a, line.b, 0.1);
            if (victim == null) {
                watchTripLine(line);
                return;
            }
            line.snapped = true;
            TRIP_LINES.remove(line);
            DemonessFx.tripSnap(sl, line.a, line.b, line.tag);
            Spells.strike(line.owner, victim, TRIP_DAMAGE);
            // down it goes: dropped to the ground and held there
            Vec3 motion = victim.getDeltaMovement();
            victim.setDeltaMovement(motion.x * 0.2, Math.min(motion.y, -0.6), motion.z * 0.2);
            victim.hurtMarked = true;
            Root.apply(victim, TRIP_DOWN_TICKS, RootRestriction.MOVEMENT);
            victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, TRIP_DOWN_TICKS + 40, 1));
            Spells.sound(sl, victim.position(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1f, 0.6f);
        });
    }

    // ---------- Cocoon ----------

    // The enemy you look at is wrapped in silk and held. No enemy there: you wrap yourself, rooted but taking
    // much less damage, and mending.
    static AbilityWheel.Result cocoon(LivingEntity player, ServerLevel sl) {
        LivingEntity target = Spells.aimEnemy(player, sl, COCOON_RANGE);
        if (target != null) {
            DemonessFx.cocoon(sl, target, COCOON_HOLD_TICKS, false, COCOON_TAG);
            Root.apply(target, COCOON_HOLD_TICKS, RootRestriction.EVERYTHING);
            Spells.sound(sl, target.position(), SoundEvents.SPIDER_AMBIENT, 1f, 0.7f);
            return AbilityWheel.Result.DONE;
        }
        DemonessFx.cocoon(sl, player, COCOON_SELF_TICKS, true, COCOON_TAG);
        Root.apply(player, COCOON_SELF_TICKS, RootRestriction.MOVE, RootRestriction.JUMP);   // abilities still work
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, COCOON_SELF_TICKS, COCOON_SELF_RESISTANCE));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, COCOON_SELF_TICKS, COCOON_SELF_REGENERATION));
        Spells.sound(sl, player.position(), SoundEvents.SPIDER_AMBIENT, 0.8f, 1.2f);
        return AbilityWheel.Result.DONE;
    }

    // ---------- Threads of Life ----------

    // Red threads drain the enemy you look at into you: THREADS_DRAIN every THREADS_EVERY ticks, healing you
    // by what it lost. A mob the threads would kill rises instead, stone-skinned, as your servant for SERVANT_TICKS.
    static AbilityWheel.Result threadsOfLife(LivingEntity player, ServerLevel sl) {
        LivingEntity target = Spells.aimEnemy(player, sl, THREADS_RANGE);
        if (target == null) {
            Spells.tell(player, Component.literal("No one stands before you."));
            return AbilityWheel.Result.REFUSED;
        }
        DemonessFx.threadsOfLife(sl, player, target, THREADS_TICKS);
        Spells.sound(sl, target.position(), SoundEvents.WITHER_AMBIENT, 0.4f, 1.8f);
        drain(player, sl, target, 0);
        return AbilityWheel.Result.DONE;
    }

    private static void drain(LivingEntity player, ServerLevel sl, LivingEntity target, int drained) {
        Later.run(sl, THREADS_EVERY, () -> {
            if (!Spells.casterStillHere(player, sl) || !target.isAlive() || target.level() != sl) return;
            if (target instanceof Mob mob && canServe(mob) && mob.getHealth() <= THREADS_DRAIN) {
                player.heal(mob.getHealth());
                riseAsServant(player, sl, mob);
                return;
            }
            float before = target.getHealth();
            Spells.strike(player, target, THREADS_DRAIN);
            float lost = before - Math.max(0f, target.getHealth());
            if (lost > 0f) player.heal(lost);
            if (drained + THREADS_EVERY < THREADS_TICKS) drain(player, sl, target, drained + THREADS_EVERY);
        });
    }

    /** Not a boss, and nobody's marionette yet. */
    private static boolean canServe(Mob mob) {
        return mob.getMaxHealth() <= SERVANT_MAX_HEALTH && !Marionette.isMarionette(mob);
    }

    /**
     * The drained mob does not die: it rises again, stone-skinned, and serves for SERVANT_TICKS (a Marionette:
     * it fights for you), then drops dead.
     */
    private static void riseAsServant(LivingEntity player, ServerLevel sl, Mob mob) {
        if (!Marionette.control(mob, player, SERVANT_TICKS)) {
            Spells.strike(player, mob, THREADS_DRAIN);
            return;
        }
        mob.setHealth(mob.getMaxHealth());
        Spells.cleanse(mob);
        mob.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, SERVANT_TICKS, SERVANT_RESISTANCE));
        Root.apply(mob, 20, RootRestriction.EVERYTHING);           // a moment to rise
        DemonessFx.servantRise(sl, mob);
        Spells.sound(sl, mob.position(), SoundEvents.ZOMBIE_VILLAGER_CURE, 0.6f, 0.6f);
    }

    // ---------- Web of Fate ----------

    // A great web hangs over where you aim for FATE_TICKS. Every enemy under it is tangled: slowed, seen through
    // walls, and the tangled share FATE_SHARE of every hit any one of them takes. Cast it again while it hangs,
    // standing under it: they are all drawn to the middle and the web snaps on them.
    static AbilityWheel.Result webOfFate(LivingEntity player, ServerLevel sl) {
        Fate open = liveFate(player);
        if (open != null) {
            double dx = player.getX() - open.ground.x;
            double dz = player.getZ() - open.ground.z;
            if (Math.sqrt(dx * dx + dz * dz) > FATE_RADIUS + 1.0) {
                Spells.tell(player, Component.literal("Stand under your Web of Fate to draw it in."));
                return AbilityWheel.Result.REFUSED;
            }
            drawInFate(player, sl, open);
            return AbilityWheel.Result.FREE;
        }

        Vec3 ground = Spells.aimGround(player, sl, FATE_RANGE);
        Vec3 center = ground.add(0, FATE_HEIGHT, 0);
        Fate fate = new Fate(sl, ground, center, sl.getGameTime() + FATE_TICKS, FATE_TAG + player.getUUID());
        FATES.put(player.getUUID(), fate);
        DemonessFx.webOfFate(sl, center, FATE_RADIUS, FATE_TICKS, fate.tag);
        Spells.sound(sl, center, SoundEvents.SPIDER_AMBIENT, 1.2f, 0.5f);
        Spells.sound(sl, center, SoundEvents.ENCHANTMENT_TABLE_USE, 1f, 0.6f);

        Zone.at(ground, FATE_RADIUS)
                .height(FATE_HEIGHT + 1.0)
                .lasts(FATE_TICKS)
                .every(5)
                .onEnter(victim -> tangle(player, sl, fate, victim))
                .onEnd(() -> {
                    if (FATES.get(player.getUUID()) == fate) FATES.remove(player.getUUID());
                    for (LivingEntity victim : fate.tangled) DamageLink.unshare(victim);
                    if (!fate.snapped) ParticleShapes.clearModels(sl, center, fate.tag);
                })
                .open(player, sl, FATE_ZONE);
        return AbilityWheel.Result.DONE;
    }

    /** True while the player's Web of Fate hangs: casting it again draws it in (free). */
    static boolean fateHangs(LivingEntity player) {
        return liveFate(player) != null;
    }

    private static Fate liveFate(LivingEntity player) {
        Fate fate = FATES.get(player.getUUID());
        if (fate == null || fate.snapped) return null;            // a snapped one is on its way out (drawInFate)
        if (fate.level != player.level() || !Zone.isOpen(player, FATE_ZONE)) {
            FATES.remove(player.getUUID());
            return null;
        }
        return fate;
    }

    /** Someone walked under the web: tangled until it is gone, and bound to everyone else it caught. */
    private static void tangle(LivingEntity player, ServerLevel sl, Fate fate, LivingEntity victim) {
        if (fate.snapped || fate.tangled.contains(victim)) return;
        fate.tangled.removeIf(other -> !other.isAlive() || other.level() != sl);
        int left = (int) Math.max(1, fate.expires - sl.getGameTime());
        LivingEntity before = fate.tangled.isEmpty() ? null : fate.tangled.get(fate.tangled.size() - 1);
        fate.tangled.add(victim);

        DemonessFx.fateTangle(sl, victim, left, FATE_MARK_TAG);
        victim.addEffect(new MobEffectInstance(MobEffects.GLOWING, left, 0));              // seen through walls
        victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, left, FATE_SLOW_LEVEL));
        if (before != null) DemonessFx.fateLink(sl, before, victim, left);
        if (fate.tangled.size() >= 2) DamageLink.share(fate.tangled, FATE_SHARE, left, player);
        Spells.sound(sl, victim.position(), SoundEvents.SPIDER_STEP, 0.8f, 0.8f);
    }

    /** The recast: everyone tangled is drawn to the middle, then the web snaps on them (and on whoever is there). */
    private static void drawInFate(LivingEntity player, ServerLevel sl, Fate fate) {
        fate.snapped = true;
        List<LivingEntity> pulled = new ArrayList<>();
        for (LivingEntity victim : fate.tangled) {
            if (victim.isAlive() && victim.level() == sl) pulled.add(victim);
        }
        Vec3 middle = fate.ground.add(0, 0.2, 0);
        DemonessFx.fatePull(sl, fate.center, pulled);
        for (LivingEntity victim : pulled) {
            Reel.in(victim, () -> middle).speed(1.4).stopAt(1.2).lasts(DemonessFx.FATE_PULL_TICKS).start(sl);
        }
        Spells.sound(sl, fate.center, SoundEvents.SPIDER_AMBIENT, 1.4f, 0.4f);

        Later.run(sl, DemonessFx.FATE_PULL_TICKS, () -> {
            // the shared damage ends before the snap lands (unless a new web was cast meanwhile: that closed this one)
            if (FATES.get(player.getUUID()) == fate) Zone.close(player, FATE_ZONE);
            DemonessFx.fateSnap(sl, fate.center, FATE_RADIUS, fate.tag);
            Spells.sound(sl, middle, SoundEvents.GENERIC_EXPLODE.value(), 1f, 1.4f);
            if (!Spells.casterStillHere(player, sl)) return;
            Set<LivingEntity> hit = new HashSet<>(pulled);
            hit.addAll(Spells.enemiesAround(player, sl, middle.add(0, 1.0, 0), FATE_SNAP_RADIUS));
            for (LivingEntity victim : hit) {
                if (!victim.isAlive() || victim.level() != sl) continue;
                ParticleShapes.clearModels(sl, victim, FATE_MARK_TAG);
                victim.removeEffect(MobEffects.GLOWING);
                Spells.strike(player, victim, FATE_SNAP_DAMAGE);
                Root.apply(victim, FATE_STUN_TICKS, RootRestriction.EVERYTHING);
            }
        });
    }

    // =========================
    // Ability 5 - Plague
    // =========================
    public static void apocalypsePlague(LivingEntity player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!ready(player, bypassClassCheck, 5)) return;

        boolean sneak = player.isShiftKeyDown();
        LivingEntity target = Spells.aimEnemy(player, sl, sneak ? CURSE_RANGE : PLAGUE_RANGE);
        if (target == null) {
            Spells.tell(player, Component.literal("No one stands before you."));
            return; // nothing spent
        }
        if (!Spells.paySpirituality(player, sneak ? CURSE_COST : PLAGUE_COST)) return;
        leaveMirrorWorld(player, sl);

        if (sneak) curse(player, sl, target);
        else plague(player, sl, target);

        addStack(player, sl);
    }

    // Normal: the target carries the plague for PLAGUE_TICKS, sickening every second. Then (or when it dies)
    // the disease jumps to the 2 to 3 enemies nearest to it, stronger each jump.
    private static void plague(LivingEntity player, ServerLevel sl, LivingEntity target) {
        Contagion.spread()
                .lasts(PLAGUE_TICKS)
                .every(20, (victim, stage) -> Spells.strike(player, victim, plagueDamage(stage)))
                .passesTo(PLAGUE_JUMPS_MIN, PLAGUE_JUMPS_MAX)
                .reach(PLAGUE_REACH)
                .generations(PLAGUE_GENERATIONS)
                .maxVictims(PLAGUE_MAX_VICTIMS)
                .travel(DemonessFx.PLAGUE_JUMP_TICKS)
                .onCatch((victim, stage) -> {
                    DemonessFx.plagueMark(sl, victim, PLAGUE_TICKS, stage, PLAGUE_TAG);
                    Spells.sound(sl, victim.position(), SoundEvents.ZOMBIE_VILLAGER_CONVERTED, 0.5f, 1.4f + stage * 0.1f);
                })
                .onRunOut((victim, stage) -> DemonessFx.plagueBloom(sl, victim))
                .onPass((from, to, stage) -> DemonessFx.plagueJump(sl, from, to))
                .start(player, sl, target);

        ParticleShapes.burst(sl, DemonessFx.SPORE, SpellFx.handOf(player, sl), 10, 0.03, 0.12);
        Spells.sound(sl, player.position(), SoundEvents.WITHER_AMBIENT, 0.4f, 1.6f);
    }

    /** What the plague does each second at a stage: PLAGUE_DAMAGE on the first one, PLAGUE_GROWTH times more each jump. */
    private static float plagueDamage(int stage) {
        return PLAGUE_DAMAGE * (float) Math.pow(PLAGUE_GROWTH, stage - 1);
    }

    // Sneak - Curse: for CURSE_TICKS everything the target takes is stored; then it all hits it again at once.
    private static void curse(LivingEntity player, ServerLevel sl, LivingEntity target) {
        DemonessFx.curseMark(sl, target, CURSE_TICKS, CURSE_TAG);
        StoredDamage.start(target, player, CURSE_TICKS,
                (victim, amount, total, level) -> DemonessFx.curseCharge(level, victim, total / CURSE_FULL),
                (victim, total, level) -> {
                    DemonessFx.curseRelease(level, victim, CURSE_TAG);
                    Spells.sound(level, victim.position(), SoundEvents.LIGHTNING_BOLT_IMPACT, 1f, 0.7f);
                    Later.run(level, DemonessFx.CURSE_RELEASE_HITS, () -> {
                        if (Spells.casterStillHere(player, level)) StoredDamage.deal(victim, player, total * CURSE_REPLAY);
                        else StoredDamage.deal(victim, null, total * CURSE_REPLAY);
                    });
                });
        Spells.sound(sl, target.position(), SoundEvents.WITHER_HURT, 0.5f, 0.6f);
    }

    // =========================
    // Ability 6 - Unaging
    // =========================
    public static void apocalypseUnaging(LivingEntity player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!ready(player, bypassClassCheck, 4)) return;

        if (player.isShiftKeyDown()) {
            if (shardsOf(player, sl).isEmpty()) {
                Spells.tell(player, Component.literal("You have placed no mirror shards."));
                return; // nothing spent
            }
            if (!Spells.paySpirituality(player, SHATTER_COST)) return;
            leaveMirrorWorld(player, sl);
            shatterShards(player, sl);
        } else {
            if (!Spells.paySpirituality(player, UNAGING_COST)) return;
            leaveMirrorWorld(player, sl);
            unaging(player, sl);
        }

        addStack(player, sl);
    }

    // Normal: for UNAGING_TICKS your next death is cancelled. You come back with half your health, unseen for a moment.
    private static void unaging(LivingEntity player, ServerLevel sl) {
        DeathWard.arm(player, UNAGING_TICKS, UNAGING_HEALTH, Apocalypse::unagingReturn);
        DemonessFx.unagingWard(sl, player, UNAGING_TICKS, WARD_TAG);
        Spells.sound(sl, player.position(), SoundEvents.BEACON_ACTIVATE, 0.8f, 0.6f);
    }

    private static void unagingReturn(LivingEntity saved, DamageSource source, ServerLevel sl) {
        DemonessFx.unagingRevive(sl, saved, WARD_TAG);
        Spells.cleanse(saved);
        Concealment.hide(saved, UNAGING_HIDE_TICKS);
        if (saved instanceof Player player) player.sendSystemMessage(Component.literal("Death passes you by."));
        Spells.sound(sl, saved.position(), SoundEvents.END_PORTAL_SPAWN, 0.4f, 1.8f);
    }

    // Sneak: every mirror shard you placed bursts. The more it was charged, the harder it hits and the wider:
    // from SHARD_WEAK_AT it also weakens and slows, a full one also holds what it hit in place.
    private static void shatterShards(LivingEntity player, ServerLevel sl) {
        for (Shard shard : shardsOf(player, sl)) {
            SHARDS.remove(shard);
            float charge = shard.charge;
            double radius = 1.5 + 2.5 * charge;                      // as wide as DemonessFx.shardExplode draws it
            float damage = SHARD_MIN_DAMAGE + (SHARD_MAX_DAMAGE - SHARD_MIN_DAMAGE) * charge;
            DemonessFx.shardExplode(sl, shard.ground, charge, shard.tag);
            for (LivingEntity target : Spells.enemiesAround(player, sl, shard.ground.add(0, 0.9, 0), radius + 0.5)) {
                Spells.strike(player, target, damage);
                if (charge >= SHARD_WEAK_AT) {
                    target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
                    target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 1));
                }
                if (charge >= 1f) Root.apply(target, SHARD_FULL_ROOT_TICKS, RootRestriction.EVERYTHING);
            }
            Spells.sound(sl, shard.ground, SoundEvents.GLASS_BREAK, 1.4f, 0.6f + 0.4f * (1f - charge));
            if (charge >= SHARD_DREAD_AT) Spells.sound(sl, shard.ground, SoundEvents.GENERIC_EXPLODE.value(), 0.8f, 1.3f);
        }
    }

    // =========================
    // Ability 7 - Catastrophe
    // =========================

    /**
     * A disaster called down on 'ground' at 'power' (1 = as strong as when cast from the wheel). The fifth stack
     * casts the last one around the player with STACK_CATASTROPHE_POWER.
     */
    @FunctionalInterface
    public interface StackCatastrophe {
        void cast(LivingEntity player, ServerLevel sl, Vec3 ground, float power);
    }

    /** One entry of the catastrophe wheel. A new disaster is one more line here and a method. */
    record Disaster(String key, String name, int sequence, int cost, int cooldown, StackCatastrophe cast) {}

    static final List<Disaster> DISASTERS = List.of(
            new Disaster("earthquake", "Earthquake", 3, DISASTER_MEDIUM_COST, DISASTER_MEDIUM_COOLDOWN, Apocalypse::earthquake),
            new Disaster("hailstorm", "Hailstorm", 3, DISASTER_MEDIUM_COST, DISASTER_MEDIUM_COOLDOWN, Apocalypse::hailstorm),
            new Disaster("tornado", "Tornado", 2, DISASTER_HIGH_COST, DISASTER_HIGH_COOLDOWN, Apocalypse::tornado),
            new Disaster("ice_age", "Hopeless Ice Age", 2, DISASTER_HIGH_COST, DISASTER_HIGH_COOLDOWN, Apocalypse::iceAge),
            new Disaster("black_death", "Black Death", 2, DISASTER_HIGH_COST, DISASTER_HIGH_COOLDOWN, Apocalypse::blackDeath),
            new Disaster("black_flame_rain", "Black Flame Rain", 1, DISASTER_VERY_HIGH_COST, DISASTER_VERY_HIGH_COOLDOWN, Apocalypse::blackFlameRain),
            new Disaster("doomsday_sky", "Doomsday Sky", 1, DISASTER_VERY_HIGH_COST, DISASTER_VERY_HIGH_COOLDOWN, Apocalypse::doomsdaySky));

    // Icons are in assets/circle_of_inevitability/textures/gui/ability_menus/apocalypse/ (named as the keys)
    static final AbilityWheel CATASTROPHES = catastropheWheel();

    private static AbilityWheel catastropheWheel() {
        AbilityWheel wheel = AbilityWheel.create("apocalypse_catastrophe", "Catastrophe")
                .radius(84)                                      // seven: a little wider
                .quickCastCooldown(false);                       // sneak recast: no cooldown
        for (Disaster disaster : DISASTERS) {
            wheel.entry(disaster.key(), "ability_menus/apocalypse/" + disaster.key(), disaster.name(), disaster.sequence(),
                    disaster.cost(), disaster.cooldown(), (player, sl) -> {
                        disaster.cast().cast(player, sl, Spells.aimGround(player, sl, DISASTER_RANGE), 1f);
                        return AbilityWheel.Result.DONE;
                    });
        }
        return wheel.onCast((player, sl, entry) -> {
            leaveMirrorWorld(player, sl);
            addStack(player, sl);
        });
    }

    // Normal: the catastrophe wheel; the pick falls where you aim. Sneak: the last one again.
    public static void apocalypseCatastrophe(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!ready(player, bypassClassCheck, 3)) return;
        if (player.isShiftKeyDown()) CATASTROPHES.quickCast(player);
        else CATASTROPHES.open(player);
    }

    /**
     * The catastrophe the fifth stack calls down: the player's last Catastrophe pick, else a random one they have
     * unlocked, else null (below Sequence 3: see unleashStacks).
     */
    private static StackCatastrophe lastCatastrophe(LivingEntity caster) {
        if (!(caster instanceof Player player)) {                      // a Beyonder mob: one its Sequence reaches, at random
            List<Disaster> open = new ArrayList<>();
            for (Disaster disaster : DISASTERS) if (Beyonder.hasSequence(caster, disaster.sequence())) open.add(disaster);
            return open.isEmpty() ? null : open.get(caster.getRandom().nextInt(open.size())).cast();
        }
        AbilityWheel.Entry entry = CATASTROPHES.lastPick(player);
        if (entry == null || !CATASTROPHES.unlocked(player, entry)) {
            List<AbilityWheel.Entry> open = CATASTROPHES.unlocked(player);
            if (open.isEmpty()) return null;
            entry = open.get(player.getRandom().nextInt(open.size()));
        }
        for (Disaster disaster : DISASTERS) {
            if (disaster.key().equals(entry.key())) return disaster.cast();
        }
        return null;
    }

    // ---------- Earthquake ----------

    // The ground splits in a wide circle: everyone on it is hurt and thrown up, and staggered when they come down.
    private static void earthquake(LivingEntity player, ServerLevel sl, Vec3 ground, float power) {
        DemonessFx.earthquake(sl, ground, QUAKE_RADIUS);
        Spells.sound(sl, ground, SoundEvents.WARDEN_SONIC_BOOM, 1f, 0.4f);
        Later.run(sl, DemonessFx.QUAKE_SPLITS, () -> {
            Spells.sound(sl, ground, SoundEvents.GENERIC_EXPLODE.value(), 1.6f, 0.5f);
            Spells.sound(sl, ground, SoundEvents.ANVIL_LAND, 0.8f, 0.4f);
            if (!Spells.casterStillHere(player, sl)) return;
            for (LivingEntity target : Spells.enemiesAround(player, sl, ground, QUAKE_RADIUS)) {
                if (Math.abs(target.getY() - ground.y) > 3.0) continue;             // on the shaking ground only
                Spells.strike(player, target, QUAKE_DAMAGE * power);
                Vec3 motion = target.getDeltaMovement();
                target.setDeltaMovement(motion.x, QUAKE_LIFT, motion.z);
                target.hurtMarked = true;
                Later.run(sl, 16, () -> {                                           // about when they land
                    if (!target.isAlive() || target.level() != sl) return;
                    Root.apply(target, QUAKE_STAGGER_TICKS, RootRestriction.MOVEMENT);
                    target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, QUAKE_STAGGER_TICKS + 20, 2));
                });
            }
        });
    }

    // ---------- Hailstorm ----------

    // Ice falls over the circle for HAIL_TICKS: steady damage and slowness for whoever stays under it.
    private static void hailstorm(LivingEntity player, ServerLevel sl, Vec3 ground, float power) {
        DemonessFx.hailstorm(sl, ground, HAIL_RADIUS, HAIL_TICKS, HAIL_PER_SECOND);
        Spells.sound(sl, ground, SoundEvents.LIGHTNING_BOLT_THUNDER, 0.6f, 1.4f);
        String name = DISASTER_TAG + (disasterNumber++);
        Later.run(sl, DemonessFx.HAIL_FALLS, () -> {                               // when the first stones land
            if (!Spells.casterStillHere(player, sl)) return;
            Zone.at(ground, HAIL_RADIUS)
                    .height(8)
                    .lasts(HAIL_TICKS)
                    .every(10)
                    .onInside((target, ticksInside) -> {
                        Spells.strike(player, target, HAIL_DAMAGE * power);
                        Spells.keepEffect(target, MobEffects.SLOWNESS, HAIL_SLOW_LEVEL, 30);
                    })
                    .onPulse(ticksLeft -> Spells.sound(sl, ground, SoundEvents.GLASS_BREAK, 0.5f, 1.6f))
                    .open(player, sl, name);
        });
    }

    // ---------- Tornado ----------

    // A tornado forms where you aim and travels the way you look for TORNADO_TICKS. It draws enemies in; those it
    // reaches are lifted and whirled round, then thrown out.
    private static void tornado(LivingEntity player, ServerLevel sl, Vec3 ground, float power) {
        float yaw = player.getYRot();
        DemonessFx.tornado(sl, ground, yaw, TORNADO_SPEED, TORNADO_TICKS);
        Spells.sound(sl, ground, SoundEvents.BREEZE_JUMP, 1.4f, 0.4f);
        Map<UUID, Integer> carried = new HashMap<>();                // how long each one has been whirled
        Map<UUID, Long> thrown = new HashMap<>();                    // thrown out: left alone until then
        for (int t = DemonessFx.TORNADO_FORMS; t < TORNADO_TICKS; t += 2) {
            int tick = t;
            Later.run(sl, t, () -> tornadoTick(player, sl, ground, yaw, tick, power, carried, thrown));
        }
    }

    private static void tornadoTick(LivingEntity player, ServerLevel sl, Vec3 ground, float yaw, int tick, float power,
                                    Map<UUID, Integer> carried, Map<UUID, Long> thrown) {
        if (!Spells.casterStillHere(player, sl)) return;
        Vec3 foot = DemonessFx.tornadoAt(ground, yaw, TORNADO_SPEED, tick);
        long now = sl.getGameTime();
        boolean last = tick + 2 >= TORNADO_TICKS;
        if (tick % 20 == 0) Spells.sound(sl, foot, SoundEvents.WIND_CHARGE_THROW, 1f, 0.5f);

        for (LivingEntity target : Spells.enemiesAround(player, sl, foot.add(0, 2.0, 0), TORNADO_PULL_RADIUS)) {
            if (thrown.getOrDefault(target.getUUID(), 0L) > now) continue;
            double dx = target.getX() - foot.x;
            double dz = target.getZ() - foot.z;
            double flat = Math.sqrt(dx * dx + dz * dz);
            if (flat > TORNADO_CORE) {
                Spells.pull(target, foot, TORNADO_PULL_SPEED, target.getDeltaMovement().y);
                continue;
            }
            int held = carried.merge(target.getUUID(), 2, Integer::sum);
            if (held >= TORNADO_HOLD || last) {                                     // thrown out
                carried.remove(target.getUUID());
                thrown.put(target.getUUID(), now + 30);
                Spells.push(target, foot, TORNADO_THROW, 0.8);
                Spells.strike(player, target, TORNADO_THROW_DAMAGE * power);
                continue;
            }
            // round and up: along the funnel's turn, rising until the top
            Vec3 around = flat < 0.05 ? new Vec3(0.4, 0, 0) : new Vec3(-dz / flat * 0.45, 0, dx / flat * 0.45);
            Vec3 in = flat < 0.05 ? Vec3.ZERO : new Vec3(-dx / flat * 0.1, 0, -dz / flat * 0.1);
            double up = target.getY() - foot.y < TORNADO_TOP ? 0.3 : 0.02;
            target.setDeltaMovement(around.add(in).add(0, up, 0));
            target.hurtMarked = true;
            if (held % 10 == 0) Spells.strike(player, target, TORNADO_SPIN_DAMAGE * power);
        }
    }

    // ---------- Hopeless Ice Age ----------

    // The ground freezes over for ICE_TICKS: whoever is on it gets slower every second, cannot heal, is weakened,
    // and after ICE_FROZEN_AFTER inside is frozen solid.
    private static void iceAge(LivingEntity player, ServerLevel sl, Vec3 ground, float power) {
        DemonessFx.iceAge(sl, ground, ICE_RADIUS, ICE_TICKS);
        Spells.sound(sl, ground, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.4f, 0.4f);
        Spells.sound(sl, ground, SoundEvents.GLASS_BREAK, 1f, 0.5f);
        Zone.at(ground, ICE_RADIUS)
                .height(5)
                .lasts(ICE_TICKS)
                .every(20)
                .countsTo(ICE_FROZEN_AFTER)
                .onInside((target, ticksInside) -> {
                    int seconds = ticksInside / 20;
                    Spells.keepEffect(target, MobEffects.SLOWNESS, Math.min(4, seconds), 30);
                    Spells.keepEffect(target, MobEffects.WEAKNESS, 1, 30);
                    NoHeal.apply(target, 30);
                    if (ticksInside > 0) Spells.strike(player, target, ICE_DAMAGE * power);
                    if (ticksInside >= ICE_FROZEN_AFTER && !Freeze.isFrozen(target)) {
                        Freeze.apply(target, ICE_FREEZE_TICKS);
                        Spells.sound(sl, target.position(), SoundEvents.GLASS_BREAK, 0.8f, 0.4f);
                    }
                })
                .onPulse(ticksLeft -> Spells.sound(sl, ground, SoundEvents.AMETHYST_BLOCK_CHIME, 0.6f, 0.4f))
                .open(player, sl, DISASTER_TAG + (disasterNumber++));
    }

    // ---------- Black Death ----------

    // A plague cloud settles where you aim for DEATH_TICKS and sickens whoever is in it; every new victim it
    // infects makes it wider, with no limit but its time.
    private static void blackDeath(LivingEntity player, ServerLevel sl, Vec3 ground, float power) {
        String tag = DISASTER_TAG + (disasterNumber++);
        Vec3 center = ground.add(0, 0.8, 0);
        DemonessFx.blackDeath(sl, center, DEATH_RADIUS, DEATH_TICKS, tag);
        Spells.sound(sl, ground, SoundEvents.WITHER_AMBIENT, 1f, 0.5f);
        blackDeathTick(player, sl, center, tag, new double[]{DEATH_RADIUS}, new HashSet<>(), 10, power);
    }

    private static void blackDeathTick(LivingEntity player, ServerLevel sl, Vec3 center, String tag, double[] radius,
                                       Set<UUID> infected, int tick, float power) {
        Later.run(sl, 10, () -> {
            if (!Spells.casterStillHere(player, sl)) {
                ParticleShapes.clearModels(sl, center, tag);
                return;
            }
            boolean second = tick % 20 == 0;
            for (LivingEntity target : Spells.enemiesAround(player, sl, center, radius[0])) {
                if (infected.add(target.getUUID())) {                               // a new one: the cloud grows
                    radius[0] += DEATH_GROWTH;
                    DemonessFx.blackDeathGrow(sl, tag, radius[0]);
                    DemonessFx.plagueMark(sl, target, 60, Math.min(4, 1 + infected.size() / 3), DEATH_MARK_TAG);
                    Spells.sound(sl, target.position(), SoundEvents.ZOMBIE_VILLAGER_CONVERTED, 0.5f, 1.4f);
                }
                if (second) {
                    Spells.strike(player, target, DEATH_DAMAGE * power);
                    Spells.keepEffect(target, MobEffects.WEAKNESS, 0, 40);
                }
            }
            if (tick + 10 <= DEATH_TICKS) blackDeathTick(player, sl, center, tag, radius, infected, tick + 10, power);
        });
    }

    // ---------- Black Flame Rain ----------

    // Black fire falls over the circle for RAIN_TICKS, half of it on the enemies there: what it hits is burnt by
    // the black flame and gets the Curse (key 5 sneak), which hits it again later with everything it took.
    private static void blackFlameRain(LivingEntity player, ServerLevel sl, Vec3 ground, float power) {
        DemonessFx.rainCloud(sl, ground, RAIN_RADIUS, RAIN_TICKS + DemonessFx.FLAME_FALLS);
        Spells.sound(sl, ground, SoundEvents.LIGHTNING_BOLT_THUNDER, 0.8f, 0.6f);
        float yaw = sl.getRandom().nextFloat() * 360f;
        for (int t = 0; t < RAIN_TICKS; t += RAIN_EVERY) {
            Later.run(sl, t, () -> {
                if (!Spells.casterStillHere(player, sl)) return;
                Vec3 spot = fallingSpot(player, sl, ground, RAIN_RADIUS, RAIN_AIMED, 0.8);
                DemonessFx.blackFlameDrop(sl, spot, yaw + (sl.getRandom().nextFloat() - 0.5f) * 30f);
                Later.run(sl, DemonessFx.FLAME_FALLS, () -> {
                    if (!Spells.casterStillHere(player, sl)) return;
                    if (sl.getRandom().nextInt(4) == 0) Spells.sound(sl, spot, SoundEvents.FIRECHARGE_USE, 0.5f, 0.5f);
                    for (LivingEntity target : Spells.enemiesAround(player, sl, spot.add(0, 0.5, 0), RAIN_HIT_RADIUS)) {
                        Spells.strike(player, target, RAIN_DAMAGE * power);
                        burnBlack(player, sl, target, RAIN_BURN_TICKS, power);
                        if (!StoredDamage.isStoring(target)) curse(player, sl, target);
                    }
                });
            });
        }
    }

    // ---------- Doomsday Sky ----------

    // The sky turns dark red over the circle for DOOM_TICKS: meteors, black flame and red lightning fall near
    // enemies picked at random.
    private static void doomsdaySky(LivingEntity player, ServerLevel sl, Vec3 ground, float power) {
        DemonessFx.bloodSky(sl, ground, DOOM_RADIUS, DOOM_TICKS);
        Spells.sound(sl, ground, SoundEvents.WITHER_SPAWN, 0.8f, 0.5f);
        for (int t = 20; t < DOOM_TICKS - 20; t += DOOM_EVERY) {
            Later.run(sl, t, () -> {
                if (!Spells.casterStillHere(player, sl)) return;
                doomFalls(player, sl, fallingSpot(player, sl, ground, DOOM_RADIUS, 1f, DOOM_SPREAD), power);
            });
        }
    }

    private static void doomFalls(LivingEntity player, ServerLevel sl, Vec3 spot, float power) {
        RandomSource random = sl.getRandom();
        switch (random.nextInt(3)) {
            case 0 -> {
                float size = 0.8f + random.nextFloat() * 0.6f;
                DemonessFx.meteorDrop(sl, spot, size);
                Later.run(sl, DemonessFx.METEOR_FALLS, () -> {
                    Spells.sound(sl, spot, SoundEvents.GENERIC_EXPLODE.value(), 1.2f, 0.7f);
                    if (!Spells.casterStillHere(player, sl)) return;
                    for (LivingEntity target : Spells.enemiesAround(player, sl, spot.add(0, 0.5, 0), 2.2 * size)) {
                        Spells.strike(player, target, METEOR_DAMAGE * power);
                        Spells.push(target, spot, 0.7, 0.5);
                    }
                });
            }
            case 1 -> {
                DemonessFx.blackFlameDrop(sl, spot, random.nextFloat() * 360f);
                Later.run(sl, DemonessFx.FLAME_FALLS, () -> {
                    if (!Spells.casterStillHere(player, sl)) return;
                    for (LivingEntity target : Spells.enemiesAround(player, sl, spot.add(0, 0.5, 0), RAIN_HIT_RADIUS)) {
                        Spells.strike(player, target, DOOM_FLAME_DAMAGE * power);
                        burnBlack(player, sl, target, RAIN_BURN_TICKS, power);
                    }
                });
            }
            default -> {
                DemonessFx.lightningStrike(sl, spot);
                Later.run(sl, DemonessFx.LIGHTNING_HITS, () -> {
                    Spells.sound(sl, spot, SoundEvents.LIGHTNING_BOLT_IMPACT, 1f, 0.8f);
                    if (!Spells.casterStillHere(player, sl)) return;
                    for (LivingEntity target : Spells.enemiesAround(player, sl, spot.add(0, 0.5, 0), 1.5)) {
                        Spells.strike(player, target, LIGHTNING_DAMAGE * power);
                    }
                });
            }
        }
    }

    /**
     * Where the next thing falls in a circle: with chance 'aimed', next to an enemy in it picked at random
     * (within 'spread'), otherwise anywhere in the circle. On the ground.
     */
    private static Vec3 fallingSpot(LivingEntity player, ServerLevel sl, Vec3 ground, double radius, float aimed, double spread) {
        RandomSource random = sl.getRandom();
        Vec3 spot = null;
        if (random.nextFloat() < aimed) {
            List<LivingEntity> under = new ArrayList<>();
            for (LivingEntity target : Spells.enemiesAround(player, sl, ground, radius + 1.0)) {
                if (Math.abs(target.getY() - ground.y) < 12.0) under.add(target);
            }
            if (!under.isEmpty()) {
                LivingEntity picked = under.get(random.nextInt(under.size()));
                double angle = random.nextDouble() * Math.PI * 2, far = random.nextDouble() * spread;
                spot = picked.position().add(Math.cos(angle) * far, 0, Math.sin(angle) * far);
            }
        }
        if (spot == null) {
            double angle = random.nextDouble() * Math.PI * 2, far = Math.sqrt(random.nextDouble()) * radius;
            spot = ground.add(Math.cos(angle) * far, 0, Math.sin(angle) * far);
        }
        return Spells.groundAt(player, sl, spot.add(0, 2.0, 0));
    }

    // =========================
    // Stacks
    // =========================

    static void addStack(LivingEntity player, ServerLevel sl) {
        int slot = player.getPersistentData().getIntOr(STACKS, 0); // 0..4
        String image = STACK_IMAGES.get(player.getRandom().nextInt(STACK_IMAGES.size()));
        player.getPersistentData().putString(STACK_IMAGE_KEY + slot, image);
        showStackImage(player, slot, image);

        int stacks = slot + 1;
        if (stacks >= MAX_STACKS) {
            unleashStacks(player, sl);
            if (player instanceof Player viewerOfHud) ScreenImages.hide(viewerOfHud, STACK_IMAGE_ID + "*", 10); // let the 5th show for a moment, then all fade
            stacks = 0;
        }
        player.getPersistentData().putInt(STACKS, stacks);
    }

    // Runs when the 5th stack is added: the last catastrophe (or a random unlocked one), around you, at
    // STACK_CATASTROPHE_POWER. Below Sequence 3 there is none: black flame bursts out of you and burns every enemy around.
    private static void unleashStacks(LivingEntity player, ServerLevel sl) {
        Vec3 feet = player.position();
        StackCatastrophe catastrophe = lastCatastrophe(player);
        if (catastrophe != null) {
            catastrophe.cast(player, sl, feet, STACK_CATASTROPHE_POWER);
            return;
        }

        Vec3 center = feet.add(0, 1.0, 0);
        for (LivingEntity target : Spells.enemiesAround(player, sl, center, STACK_BURST_RADIUS)) {
            Spells.strike(player, target, STACK_BURST_DAMAGE);
            burnBlack(player, sl, target, STACK_BURN_TICKS, 1f);
        }
        DemonessFx.blackFlameHit(sl, center);
        SpellFx.shockRing(sl, feet, STACK_BURST_RADIUS, 0xF0B050FF, 9, 0);
        SpellFx.shockRing(sl, feet, STACK_BURST_RADIUS * 0.7, 0xD0E0203A, 12, 2);
        SpellFx.runeCircle(sl, feet, STACK_BURST_RADIUS * 0.5, 0xB0B050FF, 14, 8f);
        ParticleShapes.burst(sl, DemonessFx.EMBER_BLACK, center, 70, 0.15, 0.75);
        ParticleShapes.burst(sl, DemonessFx.SHADOW_SMOKE, center, 20, 0.05, 0.3);
        Spells.sound(sl, feet, SoundEvents.GENERIC_EXPLODE.value(), 1.5f, 0.6f);
    }

    // Stacks sit to the right of the hotbar, 18px apart like hotbar slots
    private static void showStackImage(LivingEntity player, int slot, String image) {
        if (player instanceof Player viewerOfHud) ScreenImages.show(viewerOfHud, STACK_IMAGE_ID + slot, image,
                ScreenAnchor.BOTTOM, 103 + slot * 18, -3, 16, ScreenImages.FOREVER);
    }

    private static void refreshStackImages(Player player) {
        if (!SoulCore.getPathway(player).equals(PATHWAY)) return;   // the Attendant draws its own stacks in the same place
        int stacks = player.getPersistentData().getIntOr(STACKS, 0);
        for (int slot = 0; slot < MAX_STACKS; slot++) {
            if (slot < stacks) {
                String image = player.getPersistentData().getStringOr(STACK_IMAGE_KEY + slot, "");
                showStackImage(player, slot, image.isEmpty() ? STACK_IMAGES.get(0) : image);
            } else {
                ScreenImages.hide(player, STACK_IMAGE_ID + slot);
            }
        }
    }

    // =========================
    // Helpers shared by the abilities
    // =========================

    /** Has this pathway (or the check is skipped) and has reached the Sequence the ability needs. */
    private static boolean ready(LivingEntity player, boolean bypassClassCheck, int sequence) {
        if (!bypassClassCheck && !Beyonders.pathwayOf(player).equals(PATHWAY)) return false;   // a player, or a Beyonder mob
        return Beyonder.hasSequence(player, sequence);
    }
}
