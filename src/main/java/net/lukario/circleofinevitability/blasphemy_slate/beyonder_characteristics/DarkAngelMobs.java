package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonders;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.MobAbilities;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.MobAbility;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.MobSpells;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Dark Angel abilities as a Beyonder mob casts them (blasphemy_slate/beyonder_mobs: MobAbility, MobCasting).
 *
 * The Dark Angel's abilities are written for any caster (its caster a LivingEntity, its aim the caster's facing),
 * so a mob runs the code a player does once it has paid (MobSpells.aimedCore), or through MobSpells.paidCore for the
 * ones that pay inside. As for a player, casting brings it out of its shadow first. DarkAngelMobs.Events keeps its
 * casts going as a player's tick does (the shadow, the cloak, the armor, the bodies).
 *
 *   1  Divination (9), Ritual Circle (9, sneak: paid in health, when its bar runs low)
 *   2  Whispers (8), Listen (8, sneak)
 *   3  Shadow Binding (7), Shadow Lurking (7, sneak: its first blow from the shadow is an ambush)
 *   4  Flesh Bomb (6), Flesh Cloak (6, sneak)
 *   6  Cull of Spiritual Flesh (4), Black Armor (4, sneak)
 *   7  Degeneration (3), Trinity Body (3, sneak)
 *
 * Left out: Grazing (5: the souls of a player's kills, kept in slots and cast from a wheel).
 */
public final class DarkAngelMobs {

    private static final String P = DarkAngel.PATHWAY;

    private DarkAngelMobs() {}

    /** MobAbilities calls this once. */
    public static void register() {
        // ---- key 1: Divination / Ritual Circle
        MobAbilities.register(MobAbility.of(P, 1, MobAbility.NORMAL)
                .cost(DarkAngel.DIVINATION_COST).cooldown(DarkAngel.MARK_TICKS + 200).range(DarkAngel.DIVINATION_RADIUS)
                .cast(cast(MobSpells.aimedCore(DarkAngel::divination))));
        MobAbilities.register(MobAbility.of(P, 1, MobAbility.SNEAK).named("Ritual Circle")
                .cooldown(DarkAngel.RITUAL_TICKS + 400).range(16.0)                // its price is health, not spirituality
                .when((mob, target, sl) -> !DarkAngel.isActive(mob, DarkAngel.RITUAL_UNTIL)
                        && mob.getHealth() > DarkAngel.RITUAL_PRICE * 3f && !MobSpells.hurt(mob, 0.5f))
                .cast(MobSpells.aimedCore(DarkAngel::ritualCircle)));

        // ---- key 2: Whispers / Listen
        MobAbilities.register(MobAbility.of(P, 2, MobAbility.NORMAL)
                .cost(DarkAngel.WHISPERS_COST).cooldown(DarkAngel.WHISPERS_TICKS + 60).weight(14)
                .range(DarkAngel.WHISPERS_RANGE).sight()
                .cast(cast((mob, target, sl) -> {
                    MobSpells.face(mob, target);
                    DarkAngel.whispers(mob, sl, target);
                    return true;
                })));
        MobAbilities.register(MobAbility.of(P, 2, MobAbility.SNEAK).named("Listen")
                .cost(DarkAngel.LISTEN_COST).cooldown(DarkAngel.LISTEN_TICKS + 300).weight(4).range(DarkAngel.LISTEN_RADIUS)
                .when((mob, target, sl) -> !DarkAngel.isActive(mob, DarkAngel.LISTEN_UNTIL))
                .cast(cast(MobSpells.aimedCore(DarkAngel::listen))));

        // ---- key 3: Shadow Binding / Shadow Lurking
        MobAbilities.register(MobAbility.of(P, 3, MobAbility.NORMAL)
                .cost(DarkAngel.BINDING_COST).cooldown(160).weight(14).range(DarkAngel.BINDING_RANGE).sight()
                .cast(cast(MobSpells.aimedCore(DarkAngel::shadowBinding))));
        MobAbilities.register(MobAbility.of(P, 3, MobAbility.SNEAK).named("Shadow Lurking")
                .cost(DarkAngel.LURK_COST).cooldown(DarkAngel.LURK_TICKS + 200).range(16.0)
                .when((mob, target, sl) -> !DarkAngel.isActive(mob, DarkAngel.LURK_UNTIL) && mob.distanceTo(target) > 4.0)
                .cast(MobSpells.aimedCore(DarkAngel::lurk)));                     // (not cast(): it does not bring it out)

        // ---- key 4: Flesh Bomb / Flesh Cloak
        MobAbilities.register(MobAbility.of(P, 4, MobAbility.NORMAL)
                .cost(DarkAngel.BOMB_COST).cooldown(140).weight(14).range(DarkAngel.BOMB_RANGE).sight()
                .cast(cast(MobSpells.aimedCore(DarkAngel::fleshBomb))));
        MobAbilities.register(MobAbility.of(P, 4, MobAbility.SNEAK).named("Flesh Cloak")
                .cost(DarkAngel.CLOAK_COST).cooldown(DarkAngel.CLOAK_TICKS + 200).range(12.0)
                .when((mob, target, sl) -> !DarkAngel.isActive(mob, DarkAngel.CLOAK_UNTIL))   // never to take it off
                .cast(MobSpells.paidCore(DarkAngel::fleshCloak)));

        // ---- key 6: Cull of Spiritual Flesh / Black Armor
        MobAbilities.register(MobAbility.of(P, 6, MobAbility.NORMAL)
                .cost(DarkAngel.CULL_COST).cooldown(160).range(DarkAngel.CULL_REACH).sight()
                .cast(cast(MobSpells.aimedCore(DarkAngel::cull))));
        MobAbilities.register(MobAbility.of(P, 6, MobAbility.SNEAK).named("Black Armor")
                .cost(DarkAngel.ARMOR_COST).cooldown(DarkAngel.ARMOR_TICKS + 200).range(12.0)
                .when((mob, target, sl) -> !DarkAngel.isActive(mob, DarkAngel.ARMOR_UNTIL))   // never to take it off
                .cast(MobSpells.paidCore(DarkAngel::blackArmor)));

        // ---- key 7: Degeneration / Trinity Body
        MobAbilities.register(MobAbility.of(P, 7, MobAbility.NORMAL)
                .cost(DarkAngel.DEGENERATION_COST).cooldown(DarkAngel.DEGENERATION_TICKS + 100)
                .range(DarkAngel.DEGENERATION_RANGE).sight()
                .cast(cast((mob, target, sl) -> {
                    MobSpells.face(mob, target);
                    DarkAngel.degeneration(mob, sl, target);
                    return true;
                })));
        MobAbilities.register(MobAbility.of(P, 7, MobAbility.SNEAK).named("Trinity Body")
                .cost(DarkAngel.TRINITY_COST).cooldown(DarkAngel.TRINITY_TICKS + 400).range(16.0)
                .when((mob, target, sl) -> !DarkAngel.isActive(mob, DarkAngel.TRINITY_UNTIL))
                .cast(MobSpells.paidCore(DarkAngel::trinityBody)));
    }

    /** Casting brings it out of its shadow first, as for a player. */
    private static MobAbility.Cast cast(MobAbility.Cast cast) {
        return (mob, target, sl) -> {
            DarkAngel.riseFromShadow(mob, sl);
            return cast.run(mob, target, sl);
        };
    }

    // =========================
    // Events
    // =========================

    @Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class Events {

        /** A Dark Angel mob: its shadow, its cloak, its armor and its other bodies run out as a player's do. */
        @SubscribeEvent
        public static void onDarkAngelMobTick(LivingEvent.LivingTickEvent event) {
            if (!(event.getEntity() instanceof Mob mob) || !(mob.level() instanceof ServerLevel sl)) return;
            if (!P.equals(Beyonders.pathwayOf(mob))) return;
            DarkAngel.lurkTick(mob, sl);
            DarkAngel.cloakTick(mob, sl);
            DarkAngel.armorTick(mob, sl);
            DarkAngel.trinityTick(mob, sl);
        }
    }
}
