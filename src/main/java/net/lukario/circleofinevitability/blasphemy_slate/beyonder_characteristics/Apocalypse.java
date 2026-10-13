package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonder;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.SoulCore;
import net.lukario.circleofinevitability.blasphemy_slate.soul_abilities.beyonder_characteristics.DemonessFx;
import net.lukario.circleofinevitability.combat.Contagion;
import net.lukario.circleofinevitability.combat.Later;
import net.lukario.circleofinevitability.combat.SpellFx;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.particles.fx.ParticleShapes;
import net.lukario.circleofinevitability.root.Freeze;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.lukario.circleofinevitability.screen.ScreenAnchor;
import net.lukario.circleofinevitability.screen.ScreenImages;
import net.lukario.circleofinevitability.status.Concealment;
import net.lukario.circleofinevitability.status.DeathWard;
import net.lukario.circleofinevitability.status.EmpoweredHit;
import net.lukario.circleofinevitability.status.FallGuard;
import net.lukario.circleofinevitability.status.StoredDamage;
import net.lukario.circleofinevitability.status.Substitute;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Apocalypse: the Demoness pathway of Lord of the Mysteries, played as an assassin. Seven keys in the Attendant
 * Of Mysteries pattern, and every ability has a normal cast and a sneak cast:
 *
 *   1 Fatal Step       (Assassin)    behind the target you look at; your next hit is a backstab | sneak: leave a mirror shard
 *   2 Black Flame      (Instigator)  a bolt of cold black fire that burns, slows and creeps on  | sneak: Mirror Substitute
 *   3 Mirror World     (Witch)       step into a mirror, hidden and fast; press again to come out where you aim
 *                                                                                             | sneak (inside): back out where you went in
 *   4 Silk Weaver      (Pleasure)    not here yet: the silk wheel (see "Key 4" below)
 *   5 Plague           (Affliction)  a disease that jumps to the nearest enemies, stronger each jump | sneak: Curse
 *   6 Unaging          (Despair)     your next death within 60 s is undone                     | sneak: every mirror shard you placed explodes
 *   7 Catastrophe      (Unaging)     not here yet: the catastrophe wheel (see "Key 7" below)
 *
 * Mirror shards (key 1 sneak) charge while enemies stand in front of them: the longer they stand there, the worse
 * it gets for them, and the bigger the shard's blast when key 6 (sneak) sets them all off.
 * Every ability you cast adds a stack (the pictures next to the hotbar); the fifth calls down a catastrophe
 * around you (until the catastrophes exist, a black flame burst: see unleashStacks).
 *
 * The look of every ability is in DemonessFx (see /demonessfx). What is reusable lives outside this class,
 * so other classes can use it too (docs/SPELL_KIT.md): combat/Contagion (the plague), status/StoredDamage
 * (the curse), status/EmpoweredHit (the backstab), and the older Substitute, DeathWard, Concealment, FallGuard.
 *
 * Nothing here breaks blocks or lights real fire: the black flame is cold. The numbers for every ability are
 * right below.
 */
public class Apocalypse {

    private static final String PATHWAY = "Apocalypse";

    // =========================
    // Ability settings (cost = spirituality, ticks: 20 = 1 second, distances in blocks)
    // =========================

    // Stacks: every ability used adds one picture next to the hotbar, the 5th one calls down a catastrophe
    private static final int MAX_STACKS = 5;
    private static final float STACK_CATASTROPHE_POWER = 1.35f;  // how strong the 5th stack's catastrophe is (1 = as cast)
    private static final float STACK_BURST_DAMAGE = 40f;         // until there are catastrophes: a black flame burst
    private static final double STACK_BURST_RADIUS = 8.0;
    private static final int STACK_BURN_TICKS = 80;
    // Pictures for the stacks, from assets/circle_of_inevitability/textures/gui/ (without .png). One is picked at random.
    // (The Attendant's orb until the Demoness has a picture of her own.)
    private static final List<String> STACK_IMAGES = List.of(
            "attendant_stack_orb"
    );

    // Ability 1 - Fatal Step (Sequence 9)
    private static final int STEP_COST = 250;
    private static final double STEP_RANGE = 20.0;
    private static final double STEP_BEHIND = 1.1;               // how far behind the target's back you come out
    private static final int BACKSTAB_WINDOW = 40;               // ticks your next hit stays a backstab
    private static final float BACKSTAB_CRIT = 1.5f;             // the backstab always crits...
    private static final float BACKSTAB_BONUS = 16f;             // ...and does this much more on top
    // sneak: a mirror shard
    private static final int SHARD_COST = 250;
    private static final int SHARD_TICKS = 1200;                 // how long a shard stands
    private static final int SHARD_MAX = 3;                      // shards one player can have; a 4th takes the oldest away
    private static final double SHARD_REACH = 7.0;               // how far in front of it an enemy charges it
    private static final double SHARD_FRONT = 55.0;              // degrees to either side of its front that still count
    private static final float SHARD_CHARGE_PER_SECOND = 0.08f;  // for each enemy standing in front of it (full = 1)
    private static final float SHARD_WEAK_AT = 0.34f;            // from this charge, those in front are also weakened
    private static final float SHARD_DREAD_AT = 0.67f;           // from this charge, they are blinded and bleed
    private static final float SHARD_DREAD_DAMAGE = 3f;          // each second, once it is that charged
    // what the shard does when key 6 (sneak) sets it off: from an uncharged one to a full one
    private static final float SHARD_MIN_DAMAGE = 12f;
    private static final float SHARD_MAX_DAMAGE = 70f;
    private static final int SHARD_FULL_ROOT_TICKS = 50;         // a full shard holds what it hit in place

    // Ability 2 - Black Flame (Sequence 8)
    private static final int FLAME_COST = 1250;
    private static final double FLAME_RANGE = 28.0;
    private static final float FLAME_DAMAGE = 22f;               // the bolt
    private static final int FLAME_BURN_TICKS = 100;             // how long the black flames burn on what it hit
    private static final float FLAME_BURN_DAMAGE = 3f;           // each second while it burns
    private static final int FLAME_SLOW_LEVEL = 1;               // 0 = Slowness I, 1 = Slowness II while it burns
    private static final int FLAME_FROST_TICKS = 12;             // the cold of it: frozen solid for a moment (0 = never)
    private static final double FLAME_SPLASH = 1.6;              // no enemy under the crosshair: hits the first one this close to where it lands
    private static final double CREEP_RADIUS = 5.0;              // it creeps to enemies this close to the one it hit...
    private static final int CREEP_COUNT = 3;                    // ...this many at most...
    private static final float CREEP_POWER = 0.5f;               // ...and burns them this much less hard
    // sneak: Mirror Substitute
    private static final int SUBSTITUTE_COST = 1250;
    private static final int SUBSTITUTE_TICKS = 200;             // how long the mirror copy waits for a hit
    private static final double SUBSTITUTE_ESCAPE = 5.0;         // how far you slip away
    private static final int SUBSTITUTE_HIDE_TICKS = 20;         // and you are unseen for this long after
    private static final float SUBSTITUTE_GLASS_DAMAGE = 14f;    // the broken glass flying back at the attacker

    // Ability 3 - Mirror World (Sequence 7)
    private static final int MIRROR_COST = 1250;
    private static final int MIRROR_TICKS = 100;                 // how long you can stay inside
    private static final int MIRROR_SPEED_LEVEL = 2;             // 0 = Speed I ... while inside
    private static final double MIRROR_EXIT_RANGE = 28.0;        // how far away you can come out where you aim
    private static final double RETURN_BURST_RADIUS = 3.5;       // sneak: coming back out where you went in
    private static final float RETURN_BURST_DAMAGE = 14f;

    // Ability 5 - Plague (Sequence 5)
    private static final int PLAGUE_COST = 3000;
    private static final double PLAGUE_RANGE = 24.0;
    private static final int PLAGUE_TICKS = 100;                 // how long one carries the mark before it jumps
    private static final float PLAGUE_DAMAGE = 5f;               // each second, on the first one
    private static final float PLAGUE_GROWTH = 1.5f;             // each jump makes it this much stronger
    private static final int PLAGUE_JUMPS_MIN = 2;               // it jumps to 2 to 3 of the nearest enemies
    private static final int PLAGUE_JUMPS_MAX = 3;
    private static final double PLAGUE_REACH = 8.0;
    private static final int PLAGUE_GENERATIONS = 4;             // the 4th one to carry it passes it on no more
    private static final int PLAGUE_MAX_VICTIMS = 24;            // in all, for one cast
    // sneak: Curse
    private static final int CURSE_COST = 3000;
    private static final double CURSE_RANGE = 24.0;
    private static final int CURSE_TICKS = 200;                  // everything it takes for 10 s is stored...
    private static final float CURSE_REPLAY = 1.0f;              // ...and hits it again at once, this much of it
    private static final float CURSE_FULL = 200f;                // stored damage at which the eye throbs its hardest (only the look)

    // Ability 6 - Unaging (Sequence 4)
    private static final int UNAGING_COST = 6000;
    private static final int UNAGING_TICKS = 1200;               // the next death within 60 s is cancelled
    private static final float UNAGING_HEALTH = 0.5f;            // you come back with half your health
    private static final int UNAGING_HIDE_TICKS = 40;            // and unseen for this long
    // sneak: every placed shard explodes
    private static final int SHATTER_COST = 6000;

    // =========================
    // Saved on the player
    // =========================
    private static final String STACKS = "apocalypse_stacks";
    private static final String STACK_IMAGE_KEY = "apocalypse_stack_image_";      // + slot number
    private static final String STACK_IMAGE_ID = "apo_stack_";                    // screen image ids
    private static final String MIRROR_LEFT = "apocalypse_mirror_ticks";          // > 0 while inside the mirror world
    private static final String MIRROR_X = "apocalypse_mirror_x";                 // where you went in
    private static final String MIRROR_Y = "apocalypse_mirror_y";
    private static final String MIRROR_Z = "apocalypse_mirror_z";

    // Names of things on other entities (models, empowered hits)
    private static final String BACKSTAB = "apocalypse_backstab";
    private static final String BURN_TAG = "apocalypse_black_flame";
    private static final String SUBSTITUTE_TAG = "apocalypse_substitute_";        // + player
    private static final String PLAGUE_TAG = "apocalypse_plague";
    private static final String CURSE_TAG = "apocalypse_curse";
    private static final String WARD_TAG = "apocalypse_unaging";
    private static final String SHARD_TAG = "apocalypse_shard_";                  // + a number for each shard

    // =========================
    // Mirror shards lying around (not saved: a world that closes takes its shards with it)
    // =========================
    private static final class Shard {
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

    private static final List<Shard> SHARDS = new ArrayList<>();
    private static long shardNumber = 0;

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
        }
    }

    // =========================
    // Ability 1 - Fatal Step
    // =========================
    public static void apocalypseFatalStep(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!ready(player, bypassClassCheck, 9)) return;

        if (player.isShiftKeyDown()) {
            if (!Spells.paySpirituality(player, SHARD_COST)) return;
            leaveMirrorWorld(player, sl);
            placeShard(player, sl);
        } else {
            if (Root.has(player, RootRestriction.TELEPORT)) {
                player.sendSystemMessage(Component.literal("You cannot leave this place."));
                return;
            }
            LivingEntity target = Spells.aimEnemy(player, sl, STEP_RANGE);
            if (target == null) {
                player.sendSystemMessage(Component.literal("No one stands before you."));
                return; // nothing spent
            }
            Vec3 behind = spotBehind(player, sl, target);
            if (behind == null) {
                player.sendSystemMessage(Component.literal("There is no room behind them."));
                return; // nothing spent
            }
            if (!Spells.paySpirituality(player, STEP_COST)) return;
            leaveMirrorWorld(player, sl);
            fatalStep(player, sl, target, behind);
        }

        addStack(player, sl);
    }

    /** Where to stand right behind the target's back, or null if there is no room there or near it. */
    private static Vec3 spotBehind(Player player, ServerLevel sl, LivingEntity target) {
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
    private static void fatalStep(Player player, ServerLevel sl, LivingEntity target, Vec3 behind) {
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

        EmpoweredHit.arm(player, BACKSTAB, BACKSTAB_WINDOW, (attacker, victim, amount, level) -> {
            backstabLook(level, attacker, victim);
            return amount * BACKSTAB_CRIT + BACKSTAB_BONUS;
        });
    }

    /** The backstab lands: the dagger's thrust, violet and red sparks out of the wound. */
    private static void backstabLook(ServerLevel sl, Player attacker, LivingEntity target) {
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
    private static void placeShard(Player player, ServerLevel sl) {
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
    private static List<Shard> shardsOf(Player player, ServerLevel sl) {
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
    private static void chargeShards(Player player, ServerLevel sl) {
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
    private static List<LivingEntity> inFrontOf(Player player, ServerLevel sl, Shard shard) {
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
    public static void apocalypseBlackFlame(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
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
    private static void blackFlame(Player player, ServerLevel sl) {
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
    private static void burnBlack(Player player, ServerLevel sl, LivingEntity target, int ticks, float power) {
        if (!target.isAlive()) return;
        DemonessFx.burn(sl, target, ticks, BURN_TAG);
        long until = sl.getGameTime() + ticks;
        target.getPersistentData().putLong(BURN_TAG, until);    // a newer burn takes over from this one
        burnTick(player, sl, target, until, power);
    }

    private static void burnTick(Player player, ServerLevel sl, LivingEntity target, long until, float power) {
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
    private static void creepFrom(Player player, ServerLevel sl, LivingEntity source) {
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
    private static void readyMirrorSubstitute(Player player, ServerLevel sl) {
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
    public static void apocalypseMirrorWorld(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
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
    private static void enterMirrorWorld(Player player, ServerLevel sl) {
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

    public static boolean inMirrorWorld(Player player) {
        return player.getPersistentData().getIntOr(MIRROR_LEFT, 0) > 0;
    }

    /** Normal press inside: out of a mirror where the crosshair is (or where you are, if you may not go there). */
    private static void exitWhereYouAim(Player player, ServerLevel sl) {
        Vec3 there = Root.has(player, RootRestriction.TELEPORT) ? null : Spells.blinkSpot(player, sl, MIRROR_EXIT_RANGE);
        if (there != null) {
            player.teleportTo(there.x, there.y, there.z);
            player.fallDistance = 0;
            FallGuard.protect(player, 60);                          // out of a mirror over a drop
        }
        exitHere(player, sl);
    }

    /** Sneak press inside: back out where you went in, the mirror bursting into glass that cuts whoever is near. */
    private static void returnFromMirror(Player player, ServerLevel sl) {
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
    private static void exitHere(Player player, ServerLevel sl) {
        endMirrorWorld(player);
        DemonessFx.mirrorExit(sl, player.position(), player.getYRot());
        Spells.sound(sl, player.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 1f, 1.2f);
    }

    /** The mirror world is over: seen again, normal speed. */
    private static void endMirrorWorld(Player player) {
        player.getPersistentData().remove(MIRROR_LEFT);
        if (Concealment.isHidden(player)) Concealment.reveal(player);
        removeMirrorSpeed(player);
    }

    /** Takes away the speed the mirror world gave, and only that (not a speed potion they drank). */
    private static void removeMirrorSpeed(Player player) {
        MobEffectInstance speed = player.getEffect(MobEffects.SPEED);
        if (speed != null && speed.getAmplifier() == MIRROR_SPEED_LEVEL && speed.getDuration() <= MIRROR_TICKS) {
            player.removeEffect(MobEffects.SPEED);
        }
    }

    /** Casting anything else pulls you out of the mirror world, where you are. */
    private static void leaveMirrorWorld(Player player, ServerLevel sl) {
        if (inMirrorWorld(player)) exitHere(player, sl);
    }

    /** Every tick: counts the mirror world down, and lets you out when it runs out. */
    private static void mirrorWorldTick(Player player, ServerLevel sl) {
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
    // Ability 4 - Silk Weaver (Sequence 6): the silk wheel, not made yet.
    //   Normal: open the silk wheel (one radial screen shared with key 7), sneak: recast the last silk.
    //   Add it the way the others are added: a method apocalypseSilkWeaver(player, level, sl, bypassClassCheck),
    //   a line in PathwayAbilities and a call in network/SKeyPressAbilityFourUsed. Its looks are ready in
    //   DemonessFx (silkSnare, webSwing, tripWeb, cocoon, threadsOfLife, webOfFate...).
    // =========================

    // =========================
    // Ability 5 - Plague
    // =========================
    public static void apocalypsePlague(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!ready(player, bypassClassCheck, 5)) return;

        boolean sneak = player.isShiftKeyDown();
        LivingEntity target = Spells.aimEnemy(player, sl, sneak ? CURSE_RANGE : PLAGUE_RANGE);
        if (target == null) {
            player.sendSystemMessage(Component.literal("No one stands before you."));
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
    private static void plague(Player player, ServerLevel sl, LivingEntity target) {
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
    private static void curse(Player player, ServerLevel sl, LivingEntity target) {
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
    public static void apocalypseUnaging(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!ready(player, bypassClassCheck, 4)) return;

        if (player.isShiftKeyDown()) {
            if (shardsOf(player, sl).isEmpty()) {
                player.sendSystemMessage(Component.literal("You have placed no mirror shards."));
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
    private static void unaging(Player player, ServerLevel sl) {
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
    private static void shatterShards(Player player, ServerLevel sl) {
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
    // Ability 7 - Catastrophe (Sequence 3): the catastrophe wheel, not made yet.
    //   Normal: open the catastrophe wheel and cast the pick where you aim, sneak: recast the last pick.
    //   Its looks are ready in DemonessFx (earthquake, hailstorm, tornado, iceAge, blackDeath, blackFlameRain,
    //   doomsdaySky). When it exists, have lastCatastrophe(...) below return the player's last pick, so the
    //   fifth stack calls it down too.
    // =========================

    /** A catastrophe the fifth stack can call down around the player, at 'power' (1 = as strong as when cast). */
    @FunctionalInterface
    public interface StackCatastrophe {
        void cast(Player player, ServerLevel sl, Vec3 ground, float power);
    }

    /**
     * The catastrophe the fifth stack calls down: the player's last Catastrophe pick, else a random one they have
     * unlocked, else null. There are no catastrophes yet, so for now it is always null.
     */
    private static StackCatastrophe lastCatastrophe(Player player) {
        return null;
    }

    // =========================
    // Stacks
    // =========================

    private static void addStack(Player player, ServerLevel sl) {
        int slot = player.getPersistentData().getIntOr(STACKS, 0); // 0..4
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

    // Runs when the 5th stack is added: the last catastrophe, around you, at STACK_CATASTROPHE_POWER.
    // Until there are catastrophes: black flame bursts out of you and burns every enemy around.
    private static void unleashStacks(Player player, ServerLevel sl) {
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
    private static void showStackImage(Player player, int slot, String image) {
        ScreenImages.show(player, STACK_IMAGE_ID + slot, image,
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
    private static boolean ready(Player player, boolean bypassClassCheck, int sequence) {
        if (!bypassClassCheck && !SoulCore.getPathway(player).equals(PATHWAY)) return false;
        return Beyonder.hasSequence(player, sequence);
    }
}
