package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonder;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonders;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.SoulCore;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.pathway_fx.BeautyGoddessFx;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.pathway_fx.BeautyGoddessFx.Status;
import net.lukario.circleofinevitability.combat.Later;
import net.lukario.circleofinevitability.combat.SpellFx;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.combat.Zone;
import net.lukario.circleofinevitability.menu.AbilityWheel;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.lukario.circleofinevitability.status.Blind;
import net.lukario.circleofinevitability.status.Concealment;
import net.lukario.circleofinevitability.status.DeathWard;
import net.lukario.circleofinevitability.status.FallGuard;
import net.lukario.circleofinevitability.status.Flight;
import net.lukario.circleofinevitability.status.Glide;
import net.lukario.circleofinevitability.status.Marionette;
import net.lukario.circleofinevitability.status.Swarm;
import net.lukario.circleofinevitability.status.Taunt;
import net.lukario.circleofinevitability.status.Vulnerable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Beauty Goddess: the Moon Pathway of Lord of the Mysteries. Seven keys in the Attendant Of Mysteries pattern,
 * from Apothecary (Sequence 9) up to Moon (Sequence 0); every ability has a normal and a sneak cast:
 *
 *   1 Medicine Concoction (Apothecary)       a flask that bursts into a cloud: heals allies, poisons enemies
 *                                                                              | sneak: Spirit Vision
 *   2 Taming             (Beast Tamer)       the mob you look at fights for you for a minute (bosses, players resist)
 *                                                                              | sneak: Animal Sense
 *   3 Wings of Darkness  (Vampire)           a fast glide on bat wings; at the end bats bite everything you passed
 *                                                                              | sneak: Abyss Shackles
 *   4 Potions            (Potions Professor) the wheel: Invisibility, Fire Breathing, Sun Water, Shadow
 *                                                                              | sneak: the last potion again
 *   5 Lunar Battlefield  (Scarlet Scholar)   a full moon over where you aim; inside it the key is Flash Teleportation
 *                                                                              | sneak: Spectral Moonlight Form
 *   6 Gaze of Darkness   (Shaman King)       darkness holds and hurts the target; the key again crushes the eye
 *                                                                              | sneak: Bat Swarm Transformation
 *   7 Moon wheel         (High Summoner)     Door of Summoning (3), Creation (2), Beauty (1), Blood Moon (0)
 *                                                                              | sneak: the last one again
 *
 * The two wheels' sneak recast (the last pick again) has no cooldown but costs twice as much (QUICK_CAST_COST).
 *
 * Every look and sound is in BeautyGoddessFx (see /beautyfx); this class only decides who is hurt, held and
 * helped, and when (at the ticks BeautyGoddessFx names: TAME_LANDS, SHACKLES_LOCK, DRINK_DONE, MOON_RISEN...).
 * The reusable pieces come from docs/SPELL_KIT.md: combat/Zone (the cloud, the moons, Beauty, Creation),
 * status/Marionette (the tamed, and the summoned with Marionette.spawnSummon), status/Glide (the wings),
 * status/Swarm and status/Flight (the bat swarm), status/Concealment (invisibility, shadow, moonlight), status/DeathWard (the re-forming moonlight),
 * status/Taunt (the Blood Moon's rampage), root/Root, combat/Spells, combat/Later.
 *
 * The passives (poison resistance, Strength, Night Vision...) are in SequencePassives; the ones that need more
 * than a check once a second (afterimages, longer potions, mobs that hesitate) are here.
 * Nothing here breaks blocks or lights real fire; Creation grows plants (as bone meal does). The numbers for every ability are right below.
 */
public class BeautyGoddess {

    private static final String PATHWAY = "Beauty Goddess";

    // =========================
    // Ability settings (cost = spirituality, ticks: 20 = 1 second, distances in blocks)
    // =========================

    // Ability 1 - Medicine Concoction (Sequence 9)
    private static final int MEDICINE_SEQUENCE = 9;
    private static final int MEDICINE_COST = 250;
    private static final double MEDICINE_RANGE = 20.0;
    private static final double MEDICINE_RADIUS = 3.5;
    private static final int MEDICINE_TICKS = 160;               // the cloud lingers this long
    private static final float MEDICINE_HEAL = 1f;               // health every half second, for you and your allies in it
    private static final int MEDICINE_POISON_LEVEL = 0;          // enemies in it: Poison I
    // sneak: Spirit Vision
    private static final int VISION_COST = 250;
    private static final double VISION_RADIUS = 24.0;
    private static final int VISION_TICKS = 300;                 // everything seen glows (through walls) this long
    private static final int VISION_REFRESH = 40;                // its health circle is brought up to date this often

    // Ability 2 - Taming (Sequence 8)
    private static final int TAMING_SEQUENCE = 8;
    private static final int TAMING_COST = 300;                  // Sequence 8 holds 400
    private static final double TAMING_RANGE = 20.0;
    private static final int TAME_TICKS = 1200;                  // it fights for you this long (Marionette, 3 at most)
    private static final float TAME_MAX_HEALTH = 40f;            // at Sequence 8 only mobs with this much max health or less...
    private static final float TAME_MAX_HEALTH_PER_SEQUENCE = 20f; // ...and this much more for every Sequence past it
    private static final int TAME_RESIST_HOLD = 30;              // a boss or a player is only held in place this long
    // sneak: Animal Sense
    private static final int SENSE_COST = 300;
    private static final double SENSE_RADIUS = 16.0;
    private static final double SENSE_HUNT_RANGE = 24.0;
    private static final int SENSE_TICKS = 200;                  // speed and strength for the animals this long
    private static final int SENSE_BUFF_LEVEL = 1;               // Speed II and Strength II

    // Ability 3 - Wings of Darkness (Sequence 7)
    private static final int WINGS_SEQUENCE = 7;
    private static final int WINGS_COST = 700;                   // Sequence 7 holds 1980
    private static final int WINGS_TICKS = 50;                   // the glide (BeautyGoddessFx.WINGS_SHORTEST is 44)
    private static final double WINGS_SPEED = 0.95;              // blocks per tick along your look
    private static final double WINGS_SINK = 0.08;               // the glide drifts down this much per tick when you look level
    private static final double WINGS_CATCH = 2.5;               // enemies this close to you on the way are marked for the bats
    private static final double WINGS_BURST_RADIUS = 8.0;        // nobody passed: the bats go for enemies this close to the end
    private static final int WINGS_BATS = 8;                     // at least this many bats; one for each enemy passed...
    private static final int WINGS_MOST_BATS = 16;               // ...up to this many
    private static final float BAT_DAMAGE = 6f;                  // each bat that bites
    private static final float BAT_DRAIN = 0.5f;                 // health each bite gives you back
    // sneak: Abyss Shackles
    private static final int SHACKLES_COST = 700;
    private static final double SHACKLES_RANGE = 20.0;
    private static final int SHACKLES_TICKS = 100;               // held from BeautyGoddessFx.SHACKLES_LOCK to the end
    private static final float SHACKLES_DRAIN = 3f;              // each second...
    private static final float SHACKLES_HEAL = 1.5f;             // ...and you get this much of it back

    // Ability 4 - Potions (Sequence 6): a wheel; each potion has its Sequence, cost and cooldown
    private static final int POTIONS_SEQUENCE = 6;
    private static final int INVISIBILITY_SEQUENCE = 6, INVISIBILITY_COST = 1500, INVISIBILITY_COOLDOWN = 400;
    private static final int INVISIBILITY_TICKS = 200;           // unseen and untouchable; attacking ends it
    private static final int BREATH_SEQUENCE = 6, BREATH_COST = 1500, BREATH_COOLDOWN = 300;
    private static final int BREATH_TICKS = 60;
    private static final double BREATH_RANGE = 7.0;
    private static final double BREATH_HALF_ANGLE = 25.0;
    private static final float BREATH_DAMAGE = 3f;               // every 4 ticks to each one in the flames
    private static final int BREATH_BURN_TICKS = 60;
    private static final int SUN_SEQUENCE = 5, SUN_COST = 2500, SUN_COOLDOWN = 300;
    private static final double SUN_RANGE = 24.0;
    private static final double SUN_RADIUS = 4.0;
    private static final float SUN_UNDEAD_DAMAGE = 30f;          // undead hit by Sun Water...
    private static final int SUN_UNDEAD_BURN_TICKS = 100;
    private static final float SUN_DAMAGE = 8f;                  // ...everything else less, and dazzled
    private static final int SUN_BLIND_TICKS = 40;
    private static final int SHADOW_SEQUENCE = 4, SHADOW_COST = 3000, SHADOW_COOLDOWN = 400;
    private static final int SHADOW_TICKS = 160;                 // a shadow on the ground: unseen, untouchable, fast
    private static final int SHADOW_SPEED_LEVEL = 1;
    // Potions Professor's passive (Sequence 6 and stronger): your own potions and medicine last this much longer
    private static final float POTION_MASTERY = 1.5f;

    // Ability 5 - Lunar Battlefield (Sequence 5)
    private static final int MOON_SEQUENCE = 5;
    private static final int MOON_COST = 4000;                   // Sequence 5 holds 15500
    private static final double MOON_RANGE = 28.0;
    private static final double MOON_RADIUS = 10.0;
    private static final int MOON_TICKS = 600;                   // it works from BeautyGoddessFx.MOON_RISEN on
    private static final int MOON_ALLY_LEVEL = 0;                // you and allies inside: Speed I, Strength I, Regeneration I
    private static final int MOON_ENEMY_SLOW = 1;                // enemies inside: Slowness II
    // Flash Teleportation: the key again while you stand in your moonlight
    private static final int FLASH_COST = 300;
    private static final int FLASH_COOLDOWN = 10;
    private static final double FLASH_RANGE = 24.0;
    // sneak: Spectral Moonlight Form
    private static final int MOONLIGHT_COST = 3500;
    private static final int MOONLIGHT_TICKS = 100;
    private static final int MOONLIGHT_SPEED_LEVEL = 1;
    private static final float MOONLIGHT_REFORM_HEALTH = 0.4f;   // cast inside the moon: the next death is cancelled, you stand with this much

    // Ability 6 - Gaze of Darkness (Sequence 4)
    private static final int GAZE_SEQUENCE = 4;
    private static final int GAZE_COST = 6000;                   // Sequence 4 holds 32400
    private static final double GAZE_RANGE = 28.0;
    private static final int GAZE_TICKS = 120;                   // the longest you can hold the gaze
    private static final float GAZE_DAMAGE = 8f;                 // each second while held
    private static final float GAZE_CRUSH_DAMAGE = 40f;          // the key again: the eye is crushed and this lands
    private static final int GAZE_LOST_SIGHT = 2;                // checks (half seconds) in a row without seeing it and the gaze breaks
    // sneak: Bat Swarm Transformation (the key again re-forms you early)
    private static final int SWARM_COST = 6000;
    private static final int SWARM_TICKS = 120;                  // you fly as a swarm this long, invisible
    private static final int SWARM_BATS = 12;
    private static final float SWARM_BAT_HEALTH = 4f;            // each blow kills one bat for every this much damage (one at least)
    private static final double SWARM_BITE_REACH = 2.5;          // twice a second the bats bite every enemy this close...
    private static final float SWARM_BITE_DAMAGE = 3f;           // ...for this much...
    private static final float SWARM_BITE_DRAIN = 0.5f;          // ...and you get this much back for each bite

    // Both wheels: the sneak recast skips the cooldown and costs this many times as much
    private static final float QUICK_CAST_COST = 2f;

    // Ability 7 - Moon wheel (Sequence 3)
    private static final int MOON_WHEEL_SEQUENCE = 3;
    private static final int DOOR_SEQUENCE = 3, DOOR_COST = 10000, DOOR_COOLDOWN = 900;
    private static final double DOOR_RANGE = 20.0;
    private static final int DOOR_TICKS = 100;                   // the door stands (the summoned step out from BeautyGoddessFx.DOOR_OPEN)
    private static final int SUMMON_TICKS = 600;                 // they fight for you this long, then go back (no drops)
    private static final int SUMMON_EVERY = 10;                  // ticks between two of them stepping out
    private static final float DOOR_ANOMALY_CHANCE = 0.08f;      // something stranger comes through with them
    private static final int CREATION_SEQUENCE = 2, CREATION_COST = 25000, CREATION_COOLDOWN = 1200;
    private static final double CREATION_RANGE = 24.0;
    private static final double CREATION_RADIUS = 8.0;
    private static final int CREATION_TICKS = 400;               // it heals from BeautyGoddessFx.SEED_BLOOMS for this long
    private static final int CREATION_GROWTH = 8;                // each second, this many spots in it grow as if bone-mealed
    private static final int BEAUTY_SEQUENCE = 1, BEAUTY_COST = 20000, BEAUTY_COOLDOWN = 1200;
    private static final double BEAUTY_RADIUS = 16.0;
    private static final int BEAUTY_TICKS = 200;
    private static final double BEAUTY_PULL = 0.18;              // blocks per tick they drift toward you at most
    private static final float BEAUTY_EXTRA = 0.3f;              // they take 30% more damage while they stare
    private static final int BLOOD_MOON_SEQUENCE = 0, BLOOD_MOON_COST = 40000, BLOOD_MOON_COOLDOWN = 2400;
    private static final double BLOOD_MOON_RANGE = 32.0;
    private static final double BLOOD_MOON_RADIUS = 24.0;
    private static final int BLOOD_MOON_TICKS = 1200;            // a minute (it works from BeautyGoddessFx.BLOOD_MOON_RISEN on)
    private static final float BLOOD_MOON_DAMAGE = 6f;           // each second to every enemy under it
    private static final int BLOOD_MOON_RAMPAGE_EVERY = 100;     // the monsters under it are set on each other this often
    private static final int BLOOD_MOON_OWNER_LEVEL = 1;         // you: Regeneration II, Strength II
    private static final int BLOOD_MOON_ALLY_LEVEL = 0;          // allies: Regeneration I, Strength I

    // Passives that need more than SequencePassives' check once a second
    private static final int AFTERIMAGE_SEQUENCE = 5;            // a copy of moonlight left behind as you sprint
    private static final int AFTERIMAGE_EVERY = 4;
    private static final int HESITATE_SEQUENCE = 1;
    private static final double HESITATE_RADIUS = 16.0;
    private static final float HESITATE_CHANCE = 0.3f;           // each second, for each mob hunting you

    // =========================
    // Saved on the player (game times are "until" this tick)
    // =========================
    private static final String VISION_UNTIL = "beauty_goddess_vision_until";
    private static final String INVISIBLE_UNTIL = "beauty_goddess_invisible_until";
    private static final String BREATH_UNTIL = "beauty_goddess_breath_until";
    private static final String SHADOW_UNTIL = "beauty_goddess_shadow_until";
    private static final String MOONLIGHT_UNTIL = "beauty_goddess_moonlight_until";
    private static final String MOON_UNTIL = "beauty_goddess_moon_until";
    private static final String MOON_X = "beauty_goddess_moon_x";
    private static final String MOON_Y = "beauty_goddess_moon_y";
    private static final String MOON_Z = "beauty_goddess_moon_z";
    private static final String FLASH_READY = "beauty_goddess_flash_ready";

    // Saved on whoever it is put on
    private static final String TAMED_UNTIL = "beauty_goddess_tamed_until";
    private static final String SHACKLED_UNTIL = "beauty_goddess_shackled_until";

    // Zones (one of each per caster: a new one replaces the old)
    private static final String ZONE_MEDICINE = "beauty_goddess_medicine";
    private static final String ZONE_MOON = "beauty_goddess_moon";
    private static final String ZONE_BEAUTY = "beauty_goddess_beauty";
    private static final String ZONE_BLOOD_MOON = "beauty_goddess_blood_moon";
    private static final String ZONE_CREATION = "beauty_goddess_creation";


    /** Who each caster is gazing at right now. Not saved. */
    private record Gaze(LivingEntity target, long started, long until, String tag) {}
    private static final Map<UUID, Gaze> GAZES = new HashMap<>();

    @Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class BeautyGoddessEvents {

        @SubscribeEvent
        public static void onBeautyGoddessTick(TickEvent.PlayerTickEvent.Post event) {
            Player player = event.player();
            if (!(player.level() instanceof ServerLevel sl)) return;
            // these run out even if the pathway is taken away in the meantime (or the cast was stolen)
            invisibilityTick(player, sl);
            breathTick(player, sl);
            shadowTick(player, sl);
            moonlightTick(player, sl);
            swarmTick(player, sl);
            if (player.tickCount % AFTERIMAGE_EVERY == 0 && player.isSprinting() && isBeautyGoddess(player, AFTERIMAGE_SEQUENCE)) {
                BeautyGoddessFx.afterimage(sl, player);
            }
        }

        // what was on the screen and the body does not survive relogging or dying: start clean
        @SubscribeEvent
        public static void onBeautyGoddessLogin(PlayerEvent.PlayerLoggedInEvent event) {
            clearState(event.getEntity());
        }

        @SubscribeEvent
        public static void onBeautyGoddessRespawn(PlayerEvent.PlayerRespawnEvent event) {
            clearState(event.getEntity());
        }

        @SubscribeEvent
        public static void onBeautyGoddessLogout(PlayerEvent.PlayerLoggedOutEvent event) {
            GAZES.remove(event.getEntity().getUUID());
        }
    }

    // =========================
    // Ability 1 - Medicine Concoction
    // Normal: a flask thrown where you aim bursts into a cloud MEDICINE_RADIUS wide for MEDICINE_TICKS: you and
    //         your allies in it are healed and cured of poison, enemies in it are poisoned.
    // Sneak:  Spirit Vision. For VISION_TICKS every living thing within VISION_RADIUS glows through walls, with a
    //         circle under it from green (healthy) to red (nearly dead); you are told the health of the one you look at.
    // =========================
    public static void beautyGoddessMedicine(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!ready(player, bypassClassCheck, MEDICINE_SEQUENCE)) return;

        if (player.isShiftKeyDown()) {
            if (!Spells.paySpirituality(player, VISION_COST)) return;
            spiritVision(player, sl);
            return;
        }
        if (!Spells.paySpirituality(player, MEDICINE_COST)) return;
        revealFromForms(player, sl);
        medicine(player, sl);
    }

    private static void medicine(Player player, ServerLevel sl) {
        Vec3 hand = SpellFx.handOf(player, sl);
        if (hand == null) hand = player.getEyePosition();
        Vec3 ground = Spells.aimGround(player, sl, MEDICINE_RANGE);
        Vec3 burst = ground.add(0, 0.4, 0);
        int flight = BeautyGoddessFx.throwFlask(sl, hand, burst, BeautyGoddessFx.MEDICINE);
        int ticks = brewed(player, MEDICINE_TICKS);
        Later.run(sl, flight, () -> {
            if (!Spells.casterStillHere(player, sl)) return;
            BeautyGoddessFx.flaskShatter(sl, burst, BeautyGoddessFx.MEDICINE, MEDICINE_RADIUS);
            String tag = "bg_medicine_" + sl.getGameTime();
            long until = sl.getGameTime() + ticks;
            BeautyGoddessFx.medicineCloud(sl, ground, MEDICINE_RADIUS, ticks, tag);
            BeautyGoddessFx.status(player, Status.MEDICINE, ticks);
            Zone.at(ground, MEDICINE_RADIUS).height(3).lasts(ticks).every(10)
                    .affects(BeautyGoddess::isBody)
                    .onInside((entity, inside) -> {
                        boolean second = (inside / 10) % 2 == 0;
                        if (Spells.isEnemy(player, entity)) {
                            Spells.keepEffect(entity, MobEffects.POISON, MEDICINE_POISON_LEVEL, 60);
                            if (second) BeautyGoddessFx.medicinePoison(sl, entity);
                        } else {
                            mend(sl, entity, second);
                        }
                    })
                    .onPulse(left -> {
                        BeautyGoddessFx.medicineCloudTick(sl, ground, MEDICINE_RADIUS);
                        if (inCircle(player, ground, MEDICINE_RADIUS, 3)) mend(sl, player, left % 20 < 10);
                    })
                    .onEnd(() -> {
                        if (sl.getGameTime() < until - 5) BeautyGoddessFx.medicineCloudEnd(sl, ground, tag);   // ended early
                    })
                    .open(player, sl, ZONE_MEDICINE);
        });
    }

    /** One half second of the medicine on an ally (or the caster): healed and cured of poison. */
    private static void mend(ServerLevel sl, LivingEntity ally, boolean show) {
        boolean helped = ally.hasEffect(MobEffects.POISON) || ally.getHealth() < ally.getMaxHealth();
        ally.removeEffect(MobEffects.POISON);
        if (ally.getHealth() < ally.getMaxHealth()) ally.heal(MEDICINE_HEAL);
        if (helped && show) BeautyGoddessFx.medicineHeal(sl, ally);
    }

    private static void spiritVision(Player player, ServerLevel sl) {
        long until = sl.getGameTime() + VISION_TICKS;
        player.getPersistentData().putLong(VISION_UNTIL, until);
        BeautyGoddessFx.spiritVision(sl, player, VISION_RADIUS);
        BeautyGoddessFx.status(player, Status.SPIRIT_VISION, VISION_TICKS);
        List<LivingEntity> seen = Spells.livingAround(player, sl, player.position(), VISION_RADIUS);
        for (LivingEntity one : seen) {
            one.addEffect(new MobEffectInstance(MobEffects.GLOWING, VISION_TICKS, 0, false, false));
        }
        LivingEntity looked = Spells.aimEnemy(player, sl, VISION_RADIUS);
        player.sendSystemMessage(Component.literal("Your spirit sees " + seen.size() + " living things around you."
                + (looked == null ? "" : " " + looked.getDisplayName().getString() + " has "
                + Math.round(looked.getHealth()) + " of " + Math.round(looked.getMaxHealth()) + " health.")));
        visionRefresh(player, sl, until);
    }

    /** Every VISION_REFRESH: the circles show each one's health as it is now. */
    private static void visionRefresh(Player player, ServerLevel sl, long until) {
        if (!Spells.casterStillHere(player, sl)) return;
        if (player.getPersistentData().getLongOr(VISION_UNTIL, 0L) != until) return;      // cast again since
        long left = until - sl.getGameTime();
        if (left <= 0) {
            player.getPersistentData().remove(VISION_UNTIL);
            return;
        }
        for (LivingEntity one : Spells.livingAround(player, sl, player.position(), VISION_RADIUS)) {
            if (!one.hasEffect(MobEffects.GLOWING)) continue;                              // only what was seen at the cast
            BeautyGoddessFx.spiritVisionMark(sl, one, (int) Math.min(left, VISION_REFRESH + 4), one.getHealth() / one.getMaxHealth());
        }
        Later.run(sl, VISION_REFRESH, () -> visionRefresh(player, sl, until));
    }

    // =========================
    // Ability 2 - Taming
    // Normal: a blood pact with the mob you look at: from BeautyGoddessFx.TAME_LANDS it fights for you for
    //         TAME_TICKS (status/Marionette, so 3 at most). A boss (more max health than your Sequence can tame)
    //         or a player resists and is only held in place for TAME_RESIST_HOLD.
    // Sneak:  Animal Sense. Every animal and tamed beast within SENSE_RADIUS answers: Speed II and Strength II for
    //         SENSE_TICKS. Looking at an enemy, they all go for it; looking at nothing, they come to you.
    // =========================
    public static void beautyGoddessTaming(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!ready(player, bypassClassCheck, TAMING_SEQUENCE)) return;

        if (player.isShiftKeyDown()) {
            if (!Spells.paySpirituality(player, SENSE_COST)) return;
            animalSense(player, sl);
            return;
        }
        LivingEntity target = Spells.aimEnemy(player, sl, TAMING_RANGE);
        if (target == null) {
            player.sendSystemMessage(Component.literal("There is no beast before you to tame."));
            return; // nothing spent
        }
        if (!Spells.paySpirituality(player, TAMING_COST)) return;
        revealFromForms(player, sl);
        tame(player, sl, target);
    }

    private static void tame(Player player, ServerLevel sl, LivingEntity target) {
        if (!(target instanceof Mob mob) || target.getMaxHealth() > tameLimit(player)) {
            BeautyGoddessFx.tameResisted(sl, target);
            Root.apply(target, TAME_RESIST_HOLD, RootRestriction.MOVEMENT);
            player.sendSystemMessage(Component.literal(target.getDisplayName().getString() + " is too strong to be tamed."));
            return;
        }
        BeautyGoddessFx.tame(sl, player, mob, TAME_TICKS);
        Later.run(sl, BeautyGoddessFx.TAME_LANDS, () -> {
            if (!mob.isAlive() || mob.level() != sl || !Spells.casterStillHere(player, sl)) return;
            // a little longer than the pact: the pact lets it go before Marionette's own time would kill it
            if (!Marionette.control(mob, player, TAME_TICKS + 100)) {
                BeautyGoddessFx.tameEnd(sl, mob);
                player.sendSystemMessage(Component.literal("Someone else already holds that one."));
                return;
            }
            long until = sl.getGameTime() + TAME_TICKS;
            mob.getPersistentData().putLong(TAMED_UNTIL, until);
            BeautyGoddessFx.status(player, Status.TAMED, TAME_TICKS);
            Later.run(sl, TAME_TICKS, () -> {
                if (mob.getPersistentData().getLongOr(TAMED_UNTIL, 0L) != until) return;      // tamed again since
                mob.getPersistentData().remove(TAMED_UNTIL);
                if (mob.isAlive() && Marionette.isMarionetteOf(mob, player)) Marionette.release(mob);
                if (mob.isAlive()) BeautyGoddessFx.tameEnd(sl, mob);
            });
        });
    }

    /** The most max health a mob may have for this player to tame it. */
    private static float tameLimit(Player player) {
        int past = TAMING_SEQUENCE - SoulCore.getSequence(player);
        return Math.max(TAME_MAX_HEALTH / 2f, TAME_MAX_HEALTH + TAME_MAX_HEALTH_PER_SEQUENCE * past);
    }

    private static void animalSense(Player player, ServerLevel sl) {
        BeautyGoddessFx.animalSense(sl, player, SENSE_RADIUS);
        LivingEntity prey = Spells.aimEnemy(player, sl, SENSE_HUNT_RANGE);
        List<Mob> called = new ArrayList<>();
        for (Mob mob : sl.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(SENSE_RADIUS))) {
            if (!mob.isAlive() || mob == prey || Concealment.isHidden(mob)) continue;
            if (mob.distanceTo(player) > SENSE_RADIUS) continue;
            if (isBeast(mob) || Marionette.isMarionetteOf(mob, player) || isPetOf(mob, player)) called.add(mob);
        }
        if (called.isEmpty()) {
            player.sendSystemMessage(Component.literal("No beast answers your call."));
            return;
        }
        for (Mob mob : called) {
            mob.addEffect(new MobEffectInstance(MobEffects.SPEED, SENSE_TICKS, SENSE_BUFF_LEVEL));
            mob.addEffect(new MobEffectInstance(MobEffects.STRENGTH, SENSE_TICKS, SENSE_BUFF_LEVEL));
            BeautyGoddessFx.animalSenseCalled(sl, mob, SENSE_TICKS);
            if (prey == null) {
                if (!Marionette.isMarionette(mob)) mob.getNavigation().moveTo(player, 1.3);
            } else if (Marionette.isMarionetteOf(mob, player)) {
                // a marionette goes after what its master last hit
            } else if (isPetOf(mob, player)) {
                mob.setTarget(prey);
            } else {
                Taunt.apply(mob, prey, SENSE_TICKS);          // a wolf or a bee bites; a cow only stares it down
            }
        }
        if (prey != null) {
            player.setLastHurtMob(prey);
            BeautyGoddessFx.animalSenseHunt(sl, prey, SENSE_TICKS);
        }
        BeautyGoddessFx.status(player, Status.ANIMAL_SENSE, SENSE_TICKS);
        player.sendSystemMessage(Component.literal(called.size() + (called.size() == 1 ? " beast answers" : " beasts answer")
                + (prey == null ? " and comes to you." : " and goes for " + prey.getDisplayName().getString() + ".")));
    }

    /** An animal: anything alive that is not a monster and not a villager, golem or other MISC mob. */
    private static boolean isBeast(Mob mob) {
        MobCategory category = mob.getType().getCategory();
        return category != MobCategory.MONSTER && category != MobCategory.MISC;
    }

    private static boolean isPetOf(Entity entity, Player player) {
        return entity instanceof OwnableEntity owned && owned.getOwnerReference() != null
                && player.getUUID().equals(owned.getOwnerReference().getUUID());
    }

    // =========================
    // Ability 3 - Wings of Darkness
    // Normal: bat wings carry you along your look for WINGS_TICKS, with no fall damage. The key again ends it
    //         early. At the end the wings burst into WINGS_BATS bats that bite the enemies you passed (or, with
    //         none, those near the end): BAT_DAMAGE each, and a little of it comes back to you.
    // Sneak:  Abyss Shackles. Chains hold the enemy you look at in place from BeautyGoddessFx.SHACKLES_LOCK until
    //         SHACKLES_TICKS, and drain SHACKLES_DRAIN a second into you.
    // =========================
    public static void beautyGoddessWings(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!ready(player, bypassClassCheck, WINGS_SEQUENCE)) return;

        if (Glide.isGliding(player)) {                             // gliding: the key ends it, for nothing
            Glide.end(player);
            return;
        }
        if (player.isShiftKeyDown()) {
            LivingEntity target = Spells.aimEnemy(player, sl, SHACKLES_RANGE);
            if (target == null) {
                player.sendSystemMessage(Component.literal("No one stands before you to bind."));
                return; // nothing spent
            }
            if (!Spells.paySpirituality(player, SHACKLES_COST)) return;
            revealFromForms(player, sl);
            shackles(player, sl, target);
            return;
        }
        if (!Spells.paySpirituality(player, WINGS_COST)) return;
        revealFromForms(player, sl);
        wings(player, sl);
    }

    private static void wings(Player player, ServerLevel sl) {
        BeautyGoddessFx.wings(sl, player, WINGS_TICKS, wingsTag(player));
        BeautyGoddessFx.status(player, Status.WINGS, WINGS_TICKS);
        Glide.start(player, WINGS_TICKS, WINGS_SPEED, WINGS_SINK, WINGS_CATCH, (glider, passed) -> wingsBurst(glider, sl, passed));
    }

    /** The end of the glide: the wings burst into bats that bite the enemies passed (or, with none, those near). */
    private static void wingsBurst(Player player, ServerLevel sl, List<LivingEntity> passed) {
        BeautyGoddessFx.statusEnd(player, Status.WINGS);
        BeautyGoddessFx.wingsBurst(sl, player, wingsTag(player));
        List<LivingEntity> prey = passed.isEmpty() ? Spells.enemiesAround(player, sl, player.position(), WINGS_BURST_RADIUS) : passed;
        if (prey.isEmpty()) return;                                 // the bats only scatter
        BeautyGoddessFx.BAT_SEEKER.onHit((target, at) -> {
            Spells.strike(player, target, BAT_DAMAGE);
            if (Spells.casterStillHere(player, sl)) player.heal(BAT_DRAIN);
        }).launchAt(player, sl, player.getBoundingBox().getCenter(), prey, Math.min(WINGS_MOST_BATS, Math.max(WINGS_BATS, prey.size())), 1);
    }

    private static String wingsTag(Player player) {
        return "bg_wings_" + player.getId();
    }

    private static void shackles(Player player, ServerLevel sl, LivingEntity target) {
        long until = sl.getGameTime() + SHACKLES_TICKS;
        target.getPersistentData().putLong(SHACKLED_UNTIL, until);
        BeautyGoddessFx.shackles(sl, target, SHACKLES_TICKS);
        BeautyGoddessFx.status(player, Status.SHACKLES, SHACKLES_TICKS);
        Later.run(sl, BeautyGoddessFx.SHACKLES_LOCK, () -> {
            if (!target.isAlive() || target.level() != sl || !Spells.casterStillHere(player, sl)) return;
            if (target.getPersistentData().getLongOr(SHACKLED_UNTIL, 0L) != until) return;
            Root.apply(target, SHACKLES_TICKS - BeautyGoddessFx.SHACKLES_LOCK, RootRestriction.MOVEMENT);
            shacklesTick(player, sl, target, until);
        });
    }

    /** Once a second while bound: blood is drawn out of it into the caster. */
    private static void shacklesTick(Player player, ServerLevel sl, LivingEntity target, long until) {
        Later.run(sl, 20, () -> {
            if (target.getPersistentData().getLongOr(SHACKLED_UNTIL, 0L) != until) return;  // bound again since
            if (!target.isAlive() || target.level() != sl || !Spells.casterStillHere(player, sl) || sl.getGameTime() > until) {
                target.getPersistentData().remove(SHACKLED_UNTIL);
                if (target.isAlive()) BeautyGoddessFx.shacklesEnd(sl, target);
                return;
            }
            BeautyGoddessFx.shacklesDrain(sl, target, player);
            Spells.strike(player, target, SHACKLES_DRAIN);
            player.heal(SHACKLES_HEAL);
            shacklesTick(player, sl, target, until);
        });
    }

    // =========================
    // Ability 4 - Potions
    // Normal: the potion wheel. Sneak: the last potion again, without the wheel, with no cooldown but
    //         QUICK_CAST_COST times its cost. Each is drunk (or thrown) and
    //         takes hold at BeautyGoddessFx.DRINK_DONE:
    //   Invisibility (6): unseen and untouchable for INVISIBILITY_TICKS; attacking brings you out.
    //   Fire Breathing (6): BREATH_TICKS of flame out of your mouth where you look.
    //   Sun Water (5): a flask that bursts into sunlight: undead burn badly, everything else is hurt and dazzled,
    //                  your allies in it are cleansed.
    //   Shadow (4): you melt into a shadow on the ground for SHADOW_TICKS: unseen, untouchable and fast.
    // From Sequence 6 (Potions Professor) your potions and your medicine last POTION_MASTERY times as long.
    // =========================
    private static final AbilityWheel POTIONS = AbilityWheel.create("beauty_goddess_potions", "Potions")
            .radius(72)
            .quickCastCooldown(false).quickCastCost(QUICK_CAST_COST)
            .entry("invisibility", BeautyGoddessFx.WHEEL_INVISIBILITY, "Invisibility Potion", INVISIBILITY_SEQUENCE,
                    INVISIBILITY_COST, INVISIBILITY_COOLDOWN, BeautyGoddess::invisibilityPotion)
            .entry("fire_breathing", BeautyGoddessFx.WHEEL_FIRE_BREATHING, "Fire Breathing Potion", BREATH_SEQUENCE,
                    BREATH_COST, BREATH_COOLDOWN, BeautyGoddess::fireBreathingPotion)
            .entry("sun_water", BeautyGoddessFx.WHEEL_SUN_WATER, "Sun Water", SUN_SEQUENCE,
                    SUN_COST, SUN_COOLDOWN, BeautyGoddess::sunWater)
            .entry("shadow", BeautyGoddessFx.WHEEL_SHADOW, "Shadow Potion", SHADOW_SEQUENCE,
                    SHADOW_COST, SHADOW_COOLDOWN, BeautyGoddess::shadowPotion);

    public static void beautyGoddessPotions(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!ready(player, bypassClassCheck, POTIONS_SEQUENCE)) return;
        if (player.isShiftKeyDown()) POTIONS.quickCast(player);
        else POTIONS.open(player);
    }

    private static AbilityWheel.Result invisibilityPotion(ServerPlayer player, ServerLevel sl) {
        endForms(player, sl);
        int ticks = brewed(player, INVISIBILITY_TICKS);
        BeautyGoddessFx.drink(sl, player, BeautyGoddessFx.INVISIBILITY);
        Later.run(sl, BeautyGoddessFx.DRINK_DONE, () -> {
            if (!Spells.casterStillHere(player, sl)) return;
            endForms(player, sl);
            player.getPersistentData().putLong(INVISIBLE_UNTIL, sl.getGameTime() + ticks);
            Concealment.hide(player, ticks + 5);                   // a little longer: invisibilityTick ends it
            BeautyGoddessFx.invisibility(sl, player);
            BeautyGoddessFx.status(player, Status.INVISIBILITY, ticks);
        });
        return AbilityWheel.Result.DONE;
    }

    private static void invisibilityTick(Player player, ServerLevel sl) {
        if (!player.getPersistentData().contains(INVISIBLE_UNTIL)) return;
        if (!isActive(player, INVISIBLE_UNTIL) || !Concealment.isHidden(player)) endInvisibility(player, sl);
    }

    private static void endInvisibility(Player player, ServerLevel sl) {
        player.getPersistentData().remove(INVISIBLE_UNTIL);
        if (Concealment.isHidden(player)) Concealment.reveal(player);
        BeautyGoddessFx.invisibilityEnd(sl, player);
        BeautyGoddessFx.statusEnd(player, Status.INVISIBILITY);
    }

    private static AbilityWheel.Result fireBreathingPotion(ServerPlayer player, ServerLevel sl) {
        int ticks = brewed(player, BREATH_TICKS);
        BeautyGoddessFx.drink(sl, player, BeautyGoddessFx.FIRE);
        Later.run(sl, BeautyGoddessFx.DRINK_DONE, () -> {
            if (!Spells.casterStillHere(player, sl)) return;
            revealFromForms(player, sl);
            player.getPersistentData().putLong(BREATH_UNTIL, sl.getGameTime() + ticks);
            BeautyGoddessFx.fireBreath(sl, player);
            BeautyGoddessFx.status(player, Status.FIRE_BREATH, ticks);
        });
        return AbilityWheel.Result.DONE;
    }

    /** Every 2 ticks a puff of flame, every 4 it burns what is in it. */
    private static void breathTick(Player player, ServerLevel sl) {
        if (!player.getPersistentData().contains(BREATH_UNTIL)) return;
        if (!isActive(player, BREATH_UNTIL)) {
            player.getPersistentData().remove(BREATH_UNTIL);
            return;
        }
        if (player.tickCount % 2 != 0) return;
        BeautyGoddessFx.fireBreathTick(sl, player);
        if (player.tickCount % 4 != 0) return;
        Vec3 mouth = player.getEyePosition().add(0, -0.15, 0);
        for (LivingEntity enemy : Spells.enemiesInCone(player, sl, mouth, player.getLookAngle(), BREATH_RANGE, BREATH_HALF_ANGLE)) {
            if (!Spells.clearLine(player, sl, mouth, enemy.getBoundingBox().getCenter())) continue;
            Spells.strike(player, enemy, BREATH_DAMAGE);
            if (enemy.isAlive()) enemy.setRemainingFireTicks(Math.max(enemy.getRemainingFireTicks(), BREATH_BURN_TICKS));
        }
    }

    private static AbilityWheel.Result sunWater(ServerPlayer player, ServerLevel sl) {
        revealFromForms(player, sl);
        Vec3 hand = SpellFx.handOf(player, sl);
        if (hand == null) hand = player.getEyePosition();
        Vec3 at = Spells.aimGround(player, sl, SUN_RANGE).add(0, 0.4, 0);
        int flight = BeautyGoddessFx.throwFlask(sl, hand, at, BeautyGoddessFx.SUN);
        Later.run(sl, flight, () -> {
            if (!Spells.casterStillHere(player, sl)) return;
            BeautyGoddessFx.sunWater(sl, at, SUN_RADIUS);
            for (LivingEntity one : Spells.livingAround(player, sl, at, SUN_RADIUS)) {
                if (!Spells.isEnemy(player, one)) {
                    Spells.cleanse(one);
                    continue;
                }
                if (one.is(EntityTypeTags.UNDEAD)) {
                    Spells.strike(player, one, SUN_UNDEAD_DAMAGE);
                    if (one.isAlive()) one.setRemainingFireTicks(Math.max(one.getRemainingFireTicks(), SUN_UNDEAD_BURN_TICKS));
                    BeautyGoddessFx.sunWaterBurn(sl, one);
                } else {
                    Spells.strike(player, one, SUN_DAMAGE);
                    if (one.isAlive()) Blind.apply(one, SUN_BLIND_TICKS);
                }
            }
            if (inCircle(player, at, SUN_RADIUS, 3)) Spells.cleanse(player);
        });
        return AbilityWheel.Result.DONE;
    }

    private static AbilityWheel.Result shadowPotion(ServerPlayer player, ServerLevel sl) {
        int ticks = brewed(player, SHADOW_TICKS);
        BeautyGoddessFx.drink(sl, player, BeautyGoddessFx.SHADOW);
        Later.run(sl, BeautyGoddessFx.DRINK_DONE, () -> {
            if (!Spells.casterStillHere(player, sl)) return;
            endForms(player, sl);
            player.getPersistentData().putLong(SHADOW_UNTIL, sl.getGameTime() + ticks);
            Concealment.hide(player, ticks + 5);
            player.addEffect(new MobEffectInstance(MobEffects.SPEED, ticks, SHADOW_SPEED_LEVEL, false, false));
            BeautyGoddessFx.shadowForm(sl, player, ticks, shadowTag(player));
            BeautyGoddessFx.status(player, Status.SHADOW, ticks);
        });
        return AbilityWheel.Result.DONE;
    }

    private static void shadowTick(Player player, ServerLevel sl) {
        if (!player.getPersistentData().contains(SHADOW_UNTIL)) return;
        if (!isActive(player, SHADOW_UNTIL) || !Concealment.isHidden(player)) {
            endShadow(player, sl);
            return;
        }
        if (player.tickCount % 5 == 0) BeautyGoddessFx.shadowFormTick(sl, player);
    }

    private static void endShadow(Player player, ServerLevel sl) {
        player.getPersistentData().remove(SHADOW_UNTIL);
        if (Concealment.isHidden(player)) Concealment.reveal(player);
        removeOwnSpeed(player, SHADOW_SPEED_LEVEL, brewed(player, SHADOW_TICKS));
        BeautyGoddessFx.shadowFormEnd(sl, player, shadowTag(player));
        BeautyGoddessFx.statusEnd(player, Status.SHADOW);
    }

    private static String shadowTag(Player player) {
        return "bg_shadow_" + player.getId();
    }

    /** Your own potions and medicine last longer from Sequence 6 (Potions Professor) of this Pathway. */
    private static int brewed(Player player, int ticks) {
        return isBeautyGoddess(player, POTIONS_SEQUENCE) ? Math.round(ticks * POTION_MASTERY) : ticks;
    }

    // =========================
    // Ability 5 - Lunar Battlefield
    // Normal: a full moon rises over where you aim, MOON_RADIUS wide, for MOON_TICKS. From
    //         BeautyGoddessFx.MOON_RISEN you and your allies under it are quickened, strengthened and healed,
    //         enemies under it slowed. Pressed again while you stand in it: Flash Teleportation, a blink to where
    //         you aim (FLASH_COST), as long as that is in the moonlight too. Outside it: a new moon in its place.
    // Sneak:  Spectral Moonlight Form. MOONLIGHT_TICKS as moonlight: unseen, untouchable, fast. Cast in your
    //         moonlight, your next death while the moon is up is cancelled and you re-form on the spot.
    // =========================
    public static void beautyGoddessLunarBattlefield(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!ready(player, bypassClassCheck, MOON_SEQUENCE)) return;

        if (player.isShiftKeyDown()) {
            moonlightForm(player, sl);
            return;
        }
        if (inOwnMoon(player, sl)) {
            flashTeleport(player, sl);
            return;
        }
        if (!Spells.paySpirituality(player, MOON_COST)) return;
        revealFromForms(player, sl);
        lunarBattlefield(player, sl);
    }

    private static void lunarBattlefield(Player player, ServerLevel sl) {
        CompoundTag data = player.getPersistentData();
        String tag = moonTag(player);
        if (isActive(player, MOON_UNTIL)) {                        // the old moon sets
            BeautyGoddessFx.lunarBattlefieldEnd(sl, moonCenter(player), tag);
            Zone.close(player, ZONE_MOON);
        }
        Vec3 center = Spells.aimGround(player, sl, MOON_RANGE);
        long until = sl.getGameTime() + MOON_TICKS;
        data.putLong(MOON_UNTIL, until);
        data.putDouble(MOON_X, center.x);
        data.putDouble(MOON_Y, center.y);
        data.putDouble(MOON_Z, center.z);
        BeautyGoddessFx.lunarBattlefield(sl, center, MOON_RADIUS, MOON_TICKS, tag);
        BeautyGoddessFx.status(player, Status.LUNAR_BATTLEFIELD, MOON_TICKS);

        Later.run(sl, BeautyGoddessFx.MOON_RISEN, () -> {
            if (!Spells.casterStillHere(player, sl) || data.getLongOr(MOON_UNTIL, 0L) != until) return;
            Zone.at(center, MOON_RADIUS).height(6).lasts(MOON_TICKS - BeautyGoddessFx.MOON_RISEN).every(20)
                    .affects(BeautyGoddess::isBody)
                    .onInside((entity, inside) -> {
                        if (Spells.isEnemy(player, entity)) Spells.keepEffect(entity, MobEffects.SLOWNESS, MOON_ENEMY_SLOW, 40);
                        else moonBlessing(entity);
                    })
                    .onPulse(left -> {
                        if (left % 40 < 20) BeautyGoddessFx.lunarBattlefieldPulse(sl, center, MOON_RADIUS);
                        if (inCircle(player, center, MOON_RADIUS, 6)) moonBlessing(player);
                    })
                    .onEnd(() -> {
                        if (data.getLongOr(MOON_UNTIL, 0L) != until) return;           // a new moon took its place
                        data.remove(MOON_UNTIL);
                        if (sl.getGameTime() < until - 5) BeautyGoddessFx.lunarBattlefieldEnd(sl, center, tag);
                        BeautyGoddessFx.statusEnd(player, Status.LUNAR_BATTLEFIELD);
                    })
                    .open(player, sl, ZONE_MOON);
        });
    }

    private static void moonBlessing(LivingEntity ally) {
        Spells.keepEffect(ally, MobEffects.SPEED, MOON_ALLY_LEVEL, 40);
        Spells.keepEffect(ally, MobEffects.STRENGTH, MOON_ALLY_LEVEL, 40);
        Spells.keepEffect(ally, MobEffects.REGENERATION, MOON_ALLY_LEVEL, 60);
    }

    /** True while the player's moon is up (risen or rising) and they stand under it. */
    private static boolean inOwnMoon(Player player, ServerLevel sl) {
        return isActive(player, MOON_UNTIL) && inCircle(player, moonCenter(player), MOON_RADIUS, 6);
    }

    private static Vec3 moonCenter(Player player) {
        CompoundTag data = player.getPersistentData();
        return new Vec3(data.getDoubleOr(MOON_X, player.getX()), data.getDoubleOr(MOON_Y, player.getY()), data.getDoubleOr(MOON_Z, player.getZ()));
    }

    private static String moonTag(Player player) {
        return "bg_moon_" + player.getId();
    }

    private static void flashTeleport(Player player, ServerLevel sl) {
        long now = sl.getGameTime();
        CompoundTag data = player.getPersistentData();
        if (data.getLongOr(FLASH_READY, 0L) > now) return;           // a blink a moment ago: let the key bounce off
        Vec3 there = Spells.blinkSpot(player, sl, FLASH_RANGE);
        if (there == null || !inCircle(there, moonCenter(player), MOON_RADIUS, 6)) {
            player.sendSystemMessage(Component.literal("The moonlight does not reach there."));
            return; // nothing spent
        }
        if (!Spells.paySpirituality(player, FLASH_COST)) return;
        data.putLong(FLASH_READY, now + FLASH_COOLDOWN);
        Vec3 from = player.position();
        player.teleportTo(there.x, there.y, there.z);
        player.fallDistance = 0f;
        FallGuard.protect(player, 100);
        BeautyGoddessFx.flashTeleport(sl, from, there);
    }

    private static void moonlightForm(Player player, ServerLevel sl) {
        if (isActive(player, MOONLIGHT_UNTIL)) {
            player.sendSystemMessage(Component.literal("You are moonlight already."));
            return;
        }
        if (!Spells.paySpirituality(player, MOONLIGHT_COST)) return;
        endForms(player, sl);
        player.getPersistentData().putLong(MOONLIGHT_UNTIL, sl.getGameTime() + MOONLIGHT_TICKS);
        Concealment.hide(player, MOONLIGHT_TICKS + 5);
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, MOONLIGHT_TICKS, MOONLIGHT_SPEED_LEVEL, false, false));
        BeautyGoddessFx.moonlightForm(sl, player, MOONLIGHT_TICKS, moonlightTag(player));
        BeautyGoddessFx.status(player, Status.MOONLIGHT_FORM, MOONLIGHT_TICKS);

        if (inOwnMoon(player, sl)) {
            int left = (int) (player.getPersistentData().getLongOr(MOON_UNTIL, 0L) - sl.getGameTime());
            DeathWard.arm(player, Math.max(MOONLIGHT_TICKS, left), MOONLIGHT_REFORM_HEALTH, BeautyGoddess::reform);
            player.sendSystemMessage(Component.literal("Under your moon, your next death will not hold."));
        }
    }

    /** A lethal blow on the moonlight body: it scatters and gathers again where it stood. */
    private static void reform(LivingEntity saved, DamageSource source, ServerLevel sl) {
        if (!(saved instanceof Player player)) return;
        BeautyGoddessFx.moonlightShatter(sl, player.getBoundingBox().getCenter());
        BeautyGoddessFx.moonlightReform(sl, player);
        player.sendSystemMessage(Component.literal("The moonlight gathers you again."));
    }

    private static void moonlightTick(Player player, ServerLevel sl) {
        if (!player.getPersistentData().contains(MOONLIGHT_UNTIL)) return;
        if (!isActive(player, MOONLIGHT_UNTIL) || !Concealment.isHidden(player)) {
            endMoonlight(player, sl);
            return;
        }
        if (player.tickCount % 5 == 0) BeautyGoddessFx.moonlightFormTick(sl, player);
    }

    private static void endMoonlight(Player player, ServerLevel sl) {
        player.getPersistentData().remove(MOONLIGHT_UNTIL);
        if (Concealment.isHidden(player)) Concealment.reveal(player);
        removeOwnSpeed(player, MOONLIGHT_SPEED_LEVEL, MOONLIGHT_TICKS);
        BeautyGoddessFx.moonlightFormEnd(sl, player, moonlightTag(player));
        BeautyGoddessFx.statusEnd(player, Status.MOONLIGHT_FORM);
        // the death ward (if any) stays as long as the moon: it was given for the moon, not for the form
    }

    private static String moonlightTag(Player player) {
        return "bg_moonlight_" + player.getId();
    }

    // =========================
    // Ability 6 - Gaze of Darkness
    // Normal: the reflection of the enemy you look at is caught in your eye. From BeautyGoddessFx.GAZE_GRIPS
    //         solid darkness holds it in place and hurts it GAZE_DAMAGE a second, for as long as you keep it in
    //         sight (GAZE_TICKS at most). The key again crushes the eye: GAZE_CRUSH_DAMAGE, and the gaze ends.
    // Sneak:  Bat Swarm Transformation. For SWARM_TICKS you are SWARM_BATS bats: you fly, are invisible, and bite
    //         every enemy close by. A blow kills bats instead of hurting you (one for every SWARM_BAT_HEALTH of it);
    //         you cannot die while one is left. The key again, or the last bat falling, and you re-form where you are.
    // =========================
    public static void beautyGoddessGaze(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!ready(player, bypassClassCheck, GAZE_SEQUENCE)) return;

        Gaze held = GAZES.get(player.getUUID());
        if (held != null) {                                        // the key again: crush the eye, for nothing
            crushGaze(player, sl, held);
            return;
        }
        if (Swarm.isActive(player)) {                              // the key again: re-form now, for nothing
            Swarm.end(player);
            return;
        }
        if (player.isShiftKeyDown()) {
            if (!Spells.paySpirituality(player, SWARM_COST)) return;
            batSwarm(player, sl);
            return;
        }
        LivingEntity target = Spells.aimEnemy(player, sl, GAZE_RANGE);
        if (target == null) {
            player.sendSystemMessage(Component.literal("Nothing is reflected in your eyes."));
            return; // nothing spent
        }
        if (!Spells.paySpirituality(player, GAZE_COST)) return;
        revealFromForms(player, sl);
        gaze(player, sl, target);
    }

    private static void gaze(Player player, ServerLevel sl, LivingEntity target) {
        long now = sl.getGameTime();
        Gaze gaze = new Gaze(target, now, now + GAZE_TICKS, "bg_gaze_" + player.getId() + "_" + now);
        GAZES.put(player.getUUID(), gaze);
        BeautyGoddessFx.gaze(sl, player, target, GAZE_TICKS, gaze.tag());
        BeautyGoddessFx.status(player, Status.GAZE, GAZE_TICKS);
        Later.run(sl, BeautyGoddessFx.GAZE_GRIPS, () -> {
            if (GAZES.get(player.getUUID()) != gaze || !target.isAlive() || target.level() != sl) return;
            Root.apply(target, GAZE_TICKS - BeautyGoddessFx.GAZE_GRIPS, RootRestriction.MOVEMENT);
        });
        gazeStep(player, sl, gaze, 0);
    }

    /** Every half second while gazing: the darkness seeps, every second it hurts; lose sight of it and it breaks. */
    private static void gazeStep(Player player, ServerLevel sl, Gaze gaze, int unseen) {
        Later.run(sl, 10, () -> {
            if (GAZES.get(player.getUUID()) != gaze) return;          // crushed, or a new gaze
            LivingEntity target = gaze.target();
            if (!Spells.casterStillHere(player, sl)) {
                GAZES.remove(player.getUUID());
                return;
            }
            if (!target.isAlive() || target.level() != sl || sl.getGameTime() >= gaze.until()) {
                endGaze(player, sl, gaze, null);
                return;
            }
            boolean sees = player.distanceTo(target) <= GAZE_RANGE * 1.25
                    && (Spells.clearLine(player, sl, player.getEyePosition(), target.getBoundingBox().getCenter())
                    || Spells.clearLine(player, sl, player.getEyePosition(), target.getEyePosition()));
            int lost = sees ? 0 : unseen + 1;
            if (lost >= GAZE_LOST_SIGHT) {
                endGaze(player, sl, gaze, "You lost sight of it; the darkness crumbles.");
                return;
            }
            long held = sl.getGameTime() - gaze.started();
            if (held >= BeautyGoddessFx.GAZE_GRIPS) {
                BeautyGoddessFx.gazeTick(sl, target);
                if ((held / 10) % 2 == 1) Spells.strike(player, target, GAZE_DAMAGE);
            }
            gazeStep(player, sl, gaze, lost);
        });
    }

    private static void crushGaze(Player player, ServerLevel sl, Gaze gaze) {
        GAZES.remove(player.getUUID());
        LivingEntity target = gaze.target();
        BeautyGoddessFx.gazeCrush(sl, player, target, gaze.tag());
        BeautyGoddessFx.statusEnd(player, Status.GAZE);
        Later.run(sl, BeautyGoddessFx.GAZE_CRUSH_HITS, () -> {
            if (!target.isAlive() || target.level() != sl || !Spells.casterStillHere(player, sl)) return;
            Root.remove(target, RootRestriction.MOVE, RootRestriction.JUMP, RootRestriction.TELEPORT);
            Spells.strike(player, target, GAZE_CRUSH_DAMAGE);
        });
    }

    private static void endGaze(Player player, ServerLevel sl, Gaze gaze, String message) {
        GAZES.remove(player.getUUID());
        LivingEntity target = gaze.target();
        BeautyGoddessFx.gazeEnd(sl, player, target, gaze.tag());
        BeautyGoddessFx.statusEnd(player, Status.GAZE);
        if (target.isAlive()) Root.remove(target, RootRestriction.MOVE, RootRestriction.JUMP, RootRestriction.TELEPORT);
        if (message != null) player.sendSystemMessage(Component.literal(message));
    }

    private static void batSwarm(Player player, ServerLevel sl) {
        endForms(player, sl);
        Flight.grant(player, SWARM_TICKS);
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, SWARM_TICKS, 0, false, false));
        BeautyGoddessFx.batSwarm(sl, player, SWARM_TICKS, swarmTag(player));
        BeautyGoddessFx.status(player, Status.BAT_SWARM, SWARM_TICKS);
        // a blow kills bats instead of hurting you (status/Swarm); the last one falling ends it
        Swarm.start(player, SWARM_TICKS, SWARM_BATS, SWARM_BAT_HEALTH, new Swarm.Hooks() {
            @Override
            public void bodyLost(LivingEntity who, ServerLevel level, Vec3 at, int left) {
                BeautyGoddessFx.batSwarmBatLost(level, at);
            }

            @Override
            public void over(LivingEntity who, ServerLevel level, Swarm.End why) {
                if (who instanceof Player swarmed) reform(swarmed, level, why);
            }
        });
    }

    /** Every tick as a swarm: it smokes; twice a second the bats bite what is close. */
    private static void swarmTick(Player player, ServerLevel sl) {
        if (!Swarm.isActive(player)) return;
        if (player.tickCount % 5 == 0) BeautyGoddessFx.batSwarmTick(sl, player);
        if (player.tickCount % 10 != 0) return;
        for (LivingEntity enemy : Spells.enemiesAround(player, sl, player.getBoundingBox().getCenter(), SWARM_BITE_REACH)) {
            Spells.strike(player, enemy, SWARM_BITE_DAMAGE);
            player.heal(SWARM_BITE_DRAIN);
        }
    }

    /** The swarm is over (time, the last bat, the key): flesh again where you are. */
    private static void reform(Player player, ServerLevel sl, Swarm.End why) {
        if (Flight.has(player)) Flight.end(player);                // lands you safely (Flight guards the fall)
        MobEffectInstance invisible = player.getEffect(MobEffects.INVISIBILITY);   // only the swarm's own
        if (invisible != null && invisible.getAmplifier() == 0 && invisible.getDuration() <= SWARM_TICKS) {
            player.removeEffect(MobEffects.INVISIBILITY);
        }
        BeautyGoddessFx.batSwarmEnd(sl, player, swarmTag(player));
        BeautyGoddessFx.statusEnd(player, Status.BAT_SWARM);
        if (why == Swarm.End.BROKEN) player.sendSystemMessage(Component.literal("Your last bat falls: you are flesh again."));
    }

    private static String swarmTag(Player player) {
        return "bg_swarm_" + player.getId();
    }

    // =========================
    // Ability 7 - Moon wheel
    // Normal: the Moon wheel. Sneak: the last entry again, without the wheel, with no cooldown but QUICK_CAST_COST
    //         times its cost.
    //   Door of Summoning (3): a door opens where you aim and three creatures of the Spirit World step out (two
    //               hounds and a vex) to fight for you for SUMMON_TICKS (status/Marionette's summon: they do not
    //               take the place of the tamed). Now and then something stranger comes with them: a ravager.
    //   Creation (2): a Seed of Life where you aim. From its bloom, for CREATION_TICKS, everything that is not your
    //               enemy within CREATION_RADIUS is healed to full and cleansed every second, and plants grow.
    //   Beauty (1): for BEAUTY_TICKS every enemy within BEAUTY_RADIUS of you is drawn gently toward you, stands in
    //               awe (cannot attack or cast) and takes BEAUTY_EXTRA more damage.
    //   Blood Moon (0): a red moon over where you aim, BLOOD_MOON_RADIUS wide, for BLOOD_MOON_TICKS. Under it every
    //               enemy burns BLOOD_MOON_DAMAGE a second, the monsters are set on each other, and you and your
    //               allies are healed and strengthened.
    // =========================
    private static final AbilityWheel MOON_WHEEL = AbilityWheel.create("beauty_goddess_moon", "Moon")
            .radius(72)
            .quickCastCooldown(false).quickCastCost(QUICK_CAST_COST)
            .entry("door", BeautyGoddessFx.WHEEL_DOOR, "Door of Summoning", DOOR_SEQUENCE,
                    DOOR_COST, DOOR_COOLDOWN, BeautyGoddess::doorOfSummoning)
            .entry("creation", BeautyGoddessFx.WHEEL_CREATION, "Creation", CREATION_SEQUENCE,
                    CREATION_COST, CREATION_COOLDOWN, BeautyGoddess::creation)
            .entry("beauty", BeautyGoddessFx.WHEEL_BEAUTY, "Beauty", BEAUTY_SEQUENCE,
                    BEAUTY_COST, BEAUTY_COOLDOWN, BeautyGoddess::beauty)
            .entry("blood_moon", BeautyGoddessFx.WHEEL_BLOOD_MOON, "Blood Moon", BLOOD_MOON_SEQUENCE,
                    BLOOD_MOON_COST, BLOOD_MOON_COOLDOWN, BeautyGoddess::bloodMoon);

    public static void beautyGoddessMoonWheel(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!ready(player, bypassClassCheck, MOON_WHEEL_SEQUENCE)) return;
        if (player.isShiftKeyDown()) MOON_WHEEL.quickCast(player);
        else MOON_WHEEL.open(player);
    }

    private static AbilityWheel.Result doorOfSummoning(ServerPlayer player, ServerLevel sl) {
        revealFromForms(player, sl);
        Vec3 ground = Spells.aimGround(player, sl, DOOR_RANGE);
        Vec3 toCaster = player.position().subtract(ground);
        float yaw = toCaster.horizontalDistanceSqr() < 0.01 ? player.getYRot() + 180f : Spells.yawOf(toCaster);   // it opens toward you
        String tag = "bg_door_" + player.getId() + "_" + sl.getGameTime();
        BeautyGoddessFx.summoningDoor(sl, ground, yaw, DOOR_TICKS, tag);
        BeautyGoddessFx.status(player, Status.SUMMONING, SUMMON_TICKS);

        List<EntityType<? extends Mob>> coming = new ArrayList<>(List.<EntityType<? extends Mob>>of(EntityTypes.WOLF, EntityTypes.VEX, EntityTypes.WOLF));
        boolean anomaly = player.getRandom().nextFloat() < DOOR_ANOMALY_CHANCE;
        if (anomaly) coming.add(EntityTypes.RAVAGER);
        for (int i = 0; i < coming.size(); i++) {
            EntityType<? extends Mob> type = coming.get(i);
            int step = i;
            boolean stranger = anomaly && i == coming.size() - 1;
            Later.run(sl, BeautyGoddessFx.DOOR_OPEN + i * SUMMON_EVERY, () -> {
                if (!Spells.casterStillHere(player, sl)) return;
                if (stranger) {
                    BeautyGoddessFx.summonAnomaly(sl, ground, yaw);
                    player.sendSystemMessage(Component.literal("Something stranger answers the door."));
                }
                BeautyGoddessFx.spiritCharge(sl, ground, yaw);
                double side = (step % 2 == 0 ? 1 : -1) * 0.6 * ((step + 1) / 2);
                Vec3 out = Spells.spotAround(ground, yaw, 1.6).add(Spells.spotAround(Vec3.ZERO, yaw + 90f, side));
                Mob summoned = Marionette.spawnSummon(type, player, sl, Spells.groundAt(player, sl, out.add(0, 1, 0)), yaw, SUMMON_TICKS);
                if (summoned != null) BeautyGoddessFx.summonArrives(sl, summoned);
            });
        }
        player.sendSystemMessage(Component.literal("The Door of Summoning opens."));
        return AbilityWheel.Result.DONE;
    }

    private static AbilityWheel.Result creation(ServerPlayer player, ServerLevel sl) {
        revealFromForms(player, sl);
        Vec3 ground = Spells.aimGround(player, sl, CREATION_RANGE);
        String tag = "bg_creation_" + player.getId();
        int whole = BeautyGoddessFx.SEED_BLOOMS + CREATION_TICKS;
        long until = sl.getGameTime() + whole;
        Zone.close(player, ZONE_CREATION);                           // an older seed wilts first
        BeautyGoddessFx.seedOfLife(sl, ground, CREATION_RADIUS, whole, tag);
        BeautyGoddessFx.status(player, Status.CREATION, whole);
        Later.run(sl, BeautyGoddessFx.SEED_BLOOMS, () -> {
            if (!Spells.casterStillHere(player, sl)) return;
            Zone.at(ground, CREATION_RADIUS).height(5).lasts(CREATION_TICKS).every(20)
                    .affects(entity -> isBody(entity) && !Spells.isEnemy(player, entity))
                    .onInside((ally, inside) -> restore(sl, ally))
                    .onPulse(left -> {
                        BeautyGoddessFx.seedOfLifePulse(sl, ground, CREATION_RADIUS);
                        if (inCircle(player, ground, CREATION_RADIUS, 5)) restore(sl, player);
                        Spells.growPlants(player, sl, ground, CREATION_RADIUS, CREATION_GROWTH);
                    })
                    .onEnd(() -> {
                        if (sl.getGameTime() < until - 5) BeautyGoddessFx.seedOfLifeEnd(sl, ground, tag);
                        BeautyGoddessFx.statusEnd(player, Status.CREATION);
                    })
                    .open(player, sl, ZONE_CREATION);
        });
        return AbilityWheel.Result.DONE;
    }

    /** The Seed of Life on one of yours: whole again, and nothing harmful left on them. */
    private static void restore(ServerLevel sl, LivingEntity ally) {
        boolean helped = ally.getHealth() < ally.getMaxHealth() || !Spells.harmfulEffects(ally).isEmpty();
        Spells.cleanse(ally);
        ally.setHealth(ally.getMaxHealth());
        if (helped) BeautyGoddessFx.seedOfLifeHeal(sl, ally);
    }

    private static AbilityWheel.Result beauty(ServerPlayer player, ServerLevel sl) {
        revealFromForms(player, sl);
        String tag = "bg_beauty_" + player.getId();
        long until = sl.getGameTime() + BEAUTY_TICKS;
        BeautyGoddessFx.beauty(sl, player, BEAUTY_RADIUS, BEAUTY_TICKS, tag);
        BeautyGoddessFx.status(player, Status.BEAUTY, BEAUTY_TICKS);
        Zone.around(player, BEAUTY_RADIUS).height(6).lasts(BEAUTY_TICKS).every(5)
                .onEnter(enemy -> BeautyGoddessFx.beautyEnthralled(sl, enemy, (int) Math.max(10, until - sl.getGameTime())))
                .onInside((enemy, inside) -> {
                    Spells.pull(enemy, player.position(), BEAUTY_PULL, Math.min(0, enemy.getDeltaMovement().y));   // they still fall
                    Root.apply(enemy, 10, RootRestriction.ATTACK, RootRestriction.ABILITIES);
                    Vulnerable.add(enemy, BEAUTY_EXTRA, BEAUTY_EXTRA, 10);
                    if (enemy instanceof Mob mob && mob.getTarget() != null) mob.setTarget(null);
                    if (inside % 20 == 0) BeautyGoddessFx.beautyPull(sl, enemy, player);
                })
                .onEnd(() -> {
                    if (sl.getGameTime() < until - 5) BeautyGoddessFx.beautyEnd(sl, player, tag);   // ended early
                    BeautyGoddessFx.statusEnd(player, Status.BEAUTY);
                })
                .open(player, sl, ZONE_BEAUTY);
        return AbilityWheel.Result.DONE;
    }

    private static AbilityWheel.Result bloodMoon(ServerPlayer player, ServerLevel sl) {
        revealFromForms(player, sl);
        Vec3 center = Spells.aimGround(player, sl, BLOOD_MOON_RANGE);
        String tag = "bg_blood_moon_" + player.getId();
        long until = sl.getGameTime() + BLOOD_MOON_TICKS;
        Zone.close(player, ZONE_BLOOD_MOON);                         // an old one sets first
        BeautyGoddessFx.bloodMoon(sl, center, BLOOD_MOON_RADIUS, BLOOD_MOON_TICKS, tag);
        BeautyGoddessFx.status(player, Status.BLOOD_MOON, BLOOD_MOON_TICKS);
        Later.run(sl, BeautyGoddessFx.BLOOD_MOON_RISEN, () -> {
            if (!Spells.casterStillHere(player, sl)) return;
            Zone.at(center, BLOOD_MOON_RADIUS).height(10).lasts(BLOOD_MOON_TICKS - BeautyGoddessFx.BLOOD_MOON_RISEN).every(20)
                    .affects(BeautyGoddess::isBody)
                    .onInside((entity, inside) -> {
                        if (Spells.isEnemy(player, entity)) {
                            Spells.strike(player, entity, BLOOD_MOON_DAMAGE);
                            BeautyGoddessFx.bloodMoonBurn(sl, entity);
                        } else {
                            Spells.keepEffect(entity, MobEffects.REGENERATION, BLOOD_MOON_ALLY_LEVEL, 60);
                            Spells.keepEffect(entity, MobEffects.STRENGTH, BLOOD_MOON_ALLY_LEVEL, 60);
                            if (inside % 60 == 0) BeautyGoddessFx.bloodMoonEmpower(sl, entity);
                        }
                    })
                    .onPulse(left -> {
                        BeautyGoddessFx.bloodMoonTick(sl, center, BLOOD_MOON_RADIUS);
                        if (inCircle(player, center, BLOOD_MOON_RADIUS, 10)) {
                            Spells.keepEffect(player, MobEffects.REGENERATION, BLOOD_MOON_OWNER_LEVEL, 60);
                            Spells.keepEffect(player, MobEffects.STRENGTH, BLOOD_MOON_OWNER_LEVEL, 60);
                            if (left % 60 < 20) BeautyGoddessFx.bloodMoonEmpower(sl, player);
                        }
                        if (left % BLOOD_MOON_RAMPAGE_EVERY < 20) rampage(player, sl, center);
                    })
                    .onEnd(() -> {
                        if (sl.getGameTime() < until - 5) BeautyGoddessFx.bloodMoonEnd(sl, center, tag);
                        BeautyGoddessFx.statusEnd(player, Status.BLOOD_MOON);
                    })
                    .open(player, sl, ZONE_BLOOD_MOON);
        });
        return AbilityWheel.Result.DONE;
    }

    /** The monsters under the Blood Moon go mad and turn on each other. */
    private static void rampage(Player player, ServerLevel sl, Vec3 center) {
        List<Mob> mad = new ArrayList<>();
        AABB area = new AABB(center, center).inflate(BLOOD_MOON_RADIUS, 10, BLOOD_MOON_RADIUS);
        for (Mob mob : sl.getEntitiesOfClass(Mob.class, area)) {
            if (!mob.isAlive() || !Spells.isEnemy(player, mob) || Taunt.isTaunted(mob)) continue;
            if (inCircle(mob, center, BLOOD_MOON_RADIUS, 10)) mad.add(mob);
        }
        if (mad.size() >= 2) Taunt.brawl(mad, BLOOD_MOON_RAMPAGE_EVERY + 20, sl.getRandom());
    }

    // =========================
    // Passives (the rest are in SequencePassives)
    // =========================

    /** Beauty Goddess (Sequence 1): a mob hunting you hesitates and loses you now and then. Run once a second. */
    public static void hesitate(Player player) {
        if (!(player.level() instanceof ServerLevel sl)) return;
        for (Mob mob : sl.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(HESITATE_RADIUS))) {
            if (mob.getTarget() != player || Taunt.isTaunted(mob)) continue;
            if (player.getRandom().nextFloat() < HESITATE_CHANCE) mob.setTarget(null);
        }
    }

    /** True for this Pathway at this Sequence or stronger. */
    private static boolean isBeautyGoddess(Player player, int sequence) {
        return SoulCore.getPathway(player).equals(PATHWAY) && Beyonder.hasSequence(player, sequence);
    }

    /** The same for anyone: a player as above, a Beyonder mob of this Pathway by its own Sequence. */
    static boolean isBeautyGoddess(LivingEntity entity, int sequence) {
        if (entity instanceof Player player) return isBeautyGoddess(player, sequence);
        return PATHWAY.equals(Beyonders.pathwayOf(entity)) && Beyonder.hasSequence(entity, sequence);
    }

    // =========================
    // Helpers shared by the abilities
    // =========================

    /** Has this pathway (or the check is skipped) and has reached the Sequence the ability needs. */
    private static boolean ready(Player player, boolean bypassClassCheck, int sequence) {
        if (!bypassClassCheck && !SoulCore.getPathway(player).equals(PATHWAY)) return false;
        return Beyonder.hasSequence(player, sequence);
    }

    /** True while the game time saved under 'key' is still ahead. */
    private static boolean isActive(Entity entity, String key) {
        return entity.getPersistentData().getLongOr(key, 0L) > entity.level().getGameTime();
    }

    /** Something with a body a zone can act on: alive, not a spectator, not an armor stand. */
    private static boolean isBody(LivingEntity entity) {
        return entity.isAlive() && !entity.isSpectator() && !(entity instanceof ArmorStand);
    }

    private static boolean inCircle(Entity entity, Vec3 ground, double radius, double height) {
        return inCircle(entity.position(), ground, radius, height);
    }

    private static boolean inCircle(Vec3 at, Vec3 ground, double radius, double height) {
        double dx = at.x - ground.x, dz = at.z - ground.z;
        return dx * dx + dz * dz <= radius * radius && at.y >= ground.y - 1.5 && at.y <= ground.y + height;
    }

    /** Only one of the forms (invisible, shadow, moonlight, swarm) at a time: a new one ends the others first. */
    private static void endForms(Player player, ServerLevel sl) {
        if (player.getPersistentData().contains(INVISIBLE_UNTIL)) endInvisibility(player, sl);
        if (player.getPersistentData().contains(SHADOW_UNTIL)) endShadow(player, sl);
        if (player.getPersistentData().contains(MOONLIGHT_UNTIL)) endMoonlight(player, sl);
        if (Swarm.isActive(player)) Swarm.end(player);
    }

    /** Casting something that shows you brings you out of a hiding form first. */
    private static void revealFromForms(Player player, ServerLevel sl) {
        endForms(player, sl);
    }

    /** Takes away only the speed a form gave (not one from a potion or a beacon). */
    private static void removeOwnSpeed(Player player, int level, int ticks) {
        MobEffectInstance speed = player.getEffect(MobEffects.SPEED);
        if (speed != null && speed.getAmplifier() == level && speed.getDuration() <= ticks) player.removeEffect(MobEffects.SPEED);
    }

    /** After a relog or a death: nothing of the timed casts is left over (their looks did not survive either). */
    private static void clearState(Player player) {
        CompoundTag data = player.getPersistentData();
        for (String key : List.of(VISION_UNTIL, INVISIBLE_UNTIL, BREATH_UNTIL, SHADOW_UNTIL,
                MOONLIGHT_UNTIL, MOON_UNTIL, MOON_X, MOON_Y, MOON_Z, FLASH_READY)) {
            data.remove(key);
        }
        GAZES.remove(player.getUUID());
        if (!SoulCore.getPathway(player).equals(PATHWAY)) return;
        for (Status status : Status.values()) BeautyGoddessFx.statusEnd(player, status);     // a clock left running before death
    }
}
