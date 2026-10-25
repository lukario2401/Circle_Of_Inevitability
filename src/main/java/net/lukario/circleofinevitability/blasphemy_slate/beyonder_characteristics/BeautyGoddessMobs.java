package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonders;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.BeyonderMobRules;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.MobAbilities;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.MobAbility;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.MobSpells;
import net.lukario.circleofinevitability.status.Marionette;
import net.lukario.circleofinevitability.status.Swarm;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Beauty Goddess abilities as a Beyonder mob casts them (blasphemy_slate/beyonder_mobs: MobAbility, MobCasting).
 *
 * The Beauty Goddess's abilities are written for any caster (its caster a LivingEntity, its aim the caster's facing),
 * so a mob runs the code a player does: the wheel entries through MobSpells.aimedEntry, the keys' parts once it has
 * paid (or MobSpells.paidCore for the ones that pay inside). As for a player, casting brings it out of its moonlight
 * and shadow forms first. BeautyGoddessMobs.Events keeps its casts going as a player's tick does.
 *
 *   1  Medicine Concoction (9: when it or a friend near is hurt)
 *   2  Taming (8: a mob that fights it), Animal Sense (8, sneak)
 *   3  Abyss Shackles (7, sneak)
 *   4  Potions (6): Invisibility, Fire Breathing, Sun Water (5), Shadow (4)
 *   5  Lunar Battlefield (5), Flash Teleportation (5: in its own moon), Spectral Moonlight Form (5, sneak: when hurt)
 *   6  Gaze of Darkness (4) and crushing the eye, Bat Swarm Transformation (4, sneak: when hurt; the swarm does not fly)
 *   7  Moon (3): Door of Summoning, Creation (2; its plants grow only while mobGriefing allows), Beauty (1),
 *      Blood Moon (0)
 *
 * Left out: Spirit Vision (1, sneak: it shows a player what is near) and Wings of Darkness (3: a player's glide).
 */
public final class BeautyGoddessMobs {

    private static final String P = BeautyGoddess.PATHWAY;

    private BeautyGoddessMobs() {}

    /** MobAbilities calls this once. */
    public static void register() {
        // ---- key 1: Medicine Concoction (Spirit Vision shows a player what is near: left out)
        MobAbilities.register(MobAbility.of(P, 1, MobAbility.NORMAL)
                .cost(BeautyGoddess.MEDICINE_COST).cooldown(BeautyGoddess.MEDICINE_TICKS + 100).range(BeautyGoddess.MEDICINE_RANGE)
                .when((mob, target, sl) -> MobSpells.hurtFriend(mob, sl, BeautyGoddess.MEDICINE_RADIUS + 2.0, 0.7f) != null)
                .cast(cast(MobSpells.aimedCore((caster, level) -> {
                    caster.setXRot(60f);                                            // the flask at its own feet
                    BeautyGoddess.medicine(caster, level);
                }))));

        // ---- key 2: Taming / Animal Sense
        MobAbilities.register(MobAbility.of(P, 2, MobAbility.NORMAL)
                .cost(BeautyGoddess.TAMING_COST).cooldown(600).range(BeautyGoddess.TAMING_RANGE).sight()
                .when((mob, target, sl) -> target instanceof Mob && Marionette.of(mob).size() < BeyonderMobRules.MAX_SUMMONS)
                .cast(cast((mob, target, sl) -> {
                    MobSpells.face(mob, target);
                    BeautyGoddess.tame(mob, sl, target);
                    return true;
                })));
        MobAbilities.register(MobAbility.of(P, 2, MobAbility.SNEAK).named("Animal Sense")
                .cost(BeautyGoddess.SENSE_COST).cooldown(BeautyGoddess.SENSE_TICKS + 200).range(BeautyGoddess.SENSE_HUNT_RANGE).sight()
                .cast(cast(MobSpells.aimedCore(BeautyGoddess::animalSense))));

        // ---- key 3: Abyss Shackles (the wings are a player's glide)
        MobAbilities.register(MobAbility.of(P, 3, MobAbility.SNEAK).named("Abyss Shackles")
                .cost(BeautyGoddess.SHACKLES_COST).cooldown(BeautyGoddess.SHACKLES_TICKS + 100)
                .range(BeautyGoddess.SHACKLES_RANGE).sight()
                .when((mob, target, sl) -> !BeautyGoddess.isActive(target, BeautyGoddess.SHACKLED_UNTIL))
                .cast(cast((mob, target, sl) -> {
                    MobSpells.face(mob, target);
                    BeautyGoddess.shackles(mob, sl, target);
                    return true;
                })));

        // ---- key 4: the Potions wheel
        entry(4, "invisibility", "Invisibility Potion", BeautyGoddess.INVISIBILITY_SEQUENCE, BeautyGoddess.INVISIBILITY_COST,
                Math.max(BeautyGoddess.INVISIBILITY_COOLDOWN, BeautyGoddess.INVISIBILITY_TICKS + 200), 16.0, false,
                BeautyGoddess::invisibilityPotion, null);
        entry(4, "fire_breathing", "Fire Breathing Potion", BeautyGoddess.BREATH_SEQUENCE, BeautyGoddess.BREATH_COST,
                BeautyGoddess.BREATH_COOLDOWN, BeautyGoddess.BREATH_RANGE, true, BeautyGoddess::fireBreathingPotion, null);
        entry(4, "sun_water", "Sun Water", BeautyGoddess.SUN_SEQUENCE, BeautyGoddess.SUN_COST,
                BeautyGoddess.SUN_COOLDOWN, BeautyGoddess.SUN_RANGE, true, BeautyGoddess::sunWater,
                (mob, target, sl) -> mob.distanceTo(target) > BeautyGoddess.SUN_RADIUS);
        entry(4, "shadow", "Shadow Potion", BeautyGoddess.SHADOW_SEQUENCE, BeautyGoddess.SHADOW_COST,
                Math.max(BeautyGoddess.SHADOW_COOLDOWN, BeautyGoddess.SHADOW_TICKS + 200), 16.0, false, BeautyGoddess::shadowPotion,
                (mob, target, sl) -> MobSpells.hurt(mob, 0.5f));

        // ---- key 5: Lunar Battlefield / Flash Teleportation / Spectral Moonlight Form
        MobAbilities.register(MobAbility.of(P, 5, MobAbility.NORMAL)
                .cost(BeautyGoddess.MOON_COST).cooldown(BeautyGoddess.MOON_TICKS + 200).range(BeautyGoddess.MOON_RANGE).sight()
                .when((mob, target, sl) -> !BeautyGoddess.isActive(mob, BeautyGoddess.MOON_UNTIL))
                .cast(cast(MobSpells.aimedCore(BeautyGoddess::lunarBattlefield))));
        MobAbilities.register(MobAbility.of(P, 5, MobAbility.NORMAL).entry("flash_teleport").named("Flash Teleportation")
                .cost(BeautyGoddess.FLASH_COST).cooldown(100).range(BeautyGoddess.FLASH_RANGE).sight()
                .when((mob, target, sl) -> BeautyGoddess.inOwnMoon(mob, sl) && mob.distanceTo(target) > 6.0)
                .cast(MobSpells.paidCore(BeautyGoddess::flashTeleport)));
        MobAbilities.register(MobAbility.of(P, 5, MobAbility.SNEAK).named("Spectral Moonlight Form")
                .cost(BeautyGoddess.MOONLIGHT_COST).cooldown(BeautyGoddess.MOONLIGHT_TICKS + 400).range(16.0)
                .when((mob, target, sl) -> MobSpells.hurt(mob, 0.4f) && !BeautyGoddess.isActive(mob, BeautyGoddess.MOONLIGHT_UNTIL))
                .cast(MobSpells.paidCore(BeautyGoddess::moonlightForm)));

        // ---- key 6: Gaze of Darkness (and crushing the eye) / Bat Swarm
        MobAbilities.register(MobAbility.of(P, 6, MobAbility.NORMAL)
                .cost(BeautyGoddess.GAZE_COST).cooldown(BeautyGoddess.GAZE_TICKS + 100).range(BeautyGoddess.GAZE_RANGE).sight()
                .when((mob, target, sl) -> !BeautyGoddess.GAZES.containsKey(mob.getUUID()))
                .cast(cast((mob, target, sl) -> {
                    MobSpells.face(mob, target);
                    BeautyGoddess.gaze(mob, sl, target);
                    return true;
                })));
        MobAbilities.register(MobAbility.of(P, 6, MobAbility.NORMAL).entry("crush_gaze").named("Gaze of Darkness (crushed)")
                .sequence(BeautyGoddess.GAZE_SEQUENCE).cooldown(60).weight(30).anyRange()      // free, as a player's
                .when((mob, target, sl) -> BeautyGoddess.gazeHeld(mob) >= BeautyGoddess.GAZE_TICKS / 2)   // held a while first
                .cast((mob, target, sl) -> BeautyGoddess.crushHeldGaze(mob, sl)));
        MobAbilities.register(MobAbility.of(P, 6, MobAbility.SNEAK).named("Bat Swarm Transformation")
                .cost(BeautyGoddess.SWARM_COST).cooldown(BeautyGoddess.SWARM_TICKS + 400).range(8.0)
                .when((mob, target, sl) -> MobSpells.hurt(mob, 0.5f) && !Swarm.isActive(mob))
                .cast(MobSpells.aimedCore(BeautyGoddess::batSwarm)));

        // ---- key 7: the Moon wheel
        entry(7, "door", "Door of Summoning", BeautyGoddess.DOOR_SEQUENCE, BeautyGoddess.DOOR_COST,
                BeautyGoddess.DOOR_COOLDOWN, BeautyGoddess.DOOR_RANGE, true, BeautyGoddess::doorOfSummoning,
                (mob, target, sl) -> Marionette.of(mob).size() < BeyonderMobRules.MAX_SUMMONS);
        entry(7, "creation", "Creation", BeautyGoddess.CREATION_SEQUENCE, BeautyGoddess.CREATION_COST,
                Math.max(BeautyGoddess.CREATION_COOLDOWN, BeautyGoddess.CREATION_TICKS), 16.0, false,
                (mob, sl) -> {
                    mob.setXRot(60f);                                               // the seed at its own feet
                    return BeautyGoddess.creation(mob, sl);
                },
                (mob, target, sl) -> MobSpells.hurt(mob, 0.6f));
        entry(7, "beauty", "Beauty", BeautyGoddess.BEAUTY_SEQUENCE, BeautyGoddess.BEAUTY_COST,
                BeautyGoddess.BEAUTY_COOLDOWN, BeautyGoddess.BEAUTY_RADIUS - 2.0, false, BeautyGoddess::beauty, null);
        entry(7, "blood_moon", "Blood Moon", BeautyGoddess.BLOOD_MOON_SEQUENCE, BeautyGoddess.BLOOD_MOON_COST,
                Math.max(BeautyGoddess.BLOOD_MOON_COOLDOWN, BeautyGoddess.BLOOD_MOON_TICKS + 600), BeautyGoddess.BLOOD_MOON_RANGE,
                true, BeautyGoddess::bloodMoon, null);
    }

    /** One wheel entry as an ability of its own (aimed at its target; a refusal spends nothing). */
    private static void entry(int key, String entry, String name, int sequence, int cost, int cooldown, double range,
                              boolean sight, MobSpells.Entry cast, MobAbility.When when) {
        MobAbility ability = MobAbility.of(P, key, MobAbility.NORMAL).entry(entry).named(name)
                .sequence(sequence).cost(cost).cooldown(Math.max(100, cooldown))
                .range(range).cast(cast(MobSpells.aimedEntry(cast)));
        if (sight) ability.sight();
        if (when != null) ability.when(when);
        MobAbilities.register(ability);
    }

    /** Casting brings it out of its moonlight and shadow forms first, as for a player. */
    private static MobAbility.Cast cast(MobAbility.Cast cast) {
        return (mob, target, sl) -> {
            BeautyGoddess.revealFromForms(mob, sl);
            return cast.run(mob, target, sl);
        };
    }

    // =========================
    // Events
    // =========================

    @Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class Events {

        /** A Beauty Goddess mob: its potions, forms and swarm run out as a player's do. */
        @SubscribeEvent
        public static void onBeautyGoddessMobTick(LivingEvent.LivingTickEvent event) {
            if (!(event.getEntity() instanceof Mob mob) || !(mob.level() instanceof ServerLevel sl)) return;
            if (!P.equals(Beyonders.pathwayOf(mob))) return;
            BeautyGoddess.invisibilityTick(mob, sl);
            BeautyGoddess.breathTick(mob, sl);
            BeautyGoddess.shadowTick(mob, sl);
            BeautyGoddess.moonlightTick(mob, sl);
            BeautyGoddess.swarmTick(mob, sl);
        }
    }
}
