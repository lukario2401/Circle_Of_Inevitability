package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.knight_of_misfortune;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonders;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.BeyonderMobRules;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.MobAbilities;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.MobAbility;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.MobSpells;
import net.lukario.circleofinevitability.combat.Dome;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.combat.Zone;
import net.lukario.circleofinevitability.status.ConjuredWeapon;
import net.lukario.circleofinevitability.status.Marionette;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Knight Of Misfortune abilities as a Beyonder mob casts them (blasphemy_slate/beyonder_mobs: MobAbility, MobCasting).
 *
 * The Knight's abilities are written for any caster (its caster a LivingEntity, its aim the caster's facing), so a mob
 * casts the code a player does: every wheel entry through MobSpells.aimedEntry, Nightmare Limbs, the Requiem and the
 * Soothing Requiem once the mob has paid. Its Sword of Darkness and Mantle of Darkness strike with its own melee blows
 * (status/ConjuredWeapon), its upkeeps (Demonic Wolf Form) come out of its own bar. KnightOfMisfortuneMobs.Events
 * keeps its casts going as a player's tick does.
 *
 *   2  Midnight Poem (8): Tranquilize, Lullaby, Pacify, Fear, Agitating (6)
 *   3  Nightmare Limbs (7)
 *   4  Requiem (6), Soothing Requiem (6, sneak: when something is wrong with it)
 *   5  Spirit Commanding (5): Terror Banshee, Earthen Spirit (and rising out of it near its target), Command Spirits (1)
 *   6  Nightwatcher (4): Night Domain, Hair Entanglement, Curse of Misfortune (an area from 1), Demonic Wolf Form
 *   7  Darkness and Concealment (3): Sword of Darkness, Nightmare World, Concealed State (2), Secret Concealment (2),
 *      Concealed World (2), Mantle of Darkness (1), Eternal Rest (0), Destination of All Things (0)
 *
 * Left out: Night Sight (Spirit Vision and Dowsing show a player where things are), Nightmare State and Dream
 * Invasion (a soul out of its body, steered by a player, and a menu), Gale Spirit (a player's flight).
 */
public final class KnightOfMisfortuneMobs {

    private static final int SOOTHE_MOB_COOLDOWN = 600;
    private static final int EARTH_RISE_MOB_COOLDOWN = 60;
    private static final double EARTH_RISE_MOB_FAR = 6.0;       // it rises out of the earth once this close to its target

    private KnightOfMisfortuneMobs() {}

    /** MobAbilities calls this once. */
    public static void register() {
        String P = KnightOfMisfortune.PATHWAY;

        // ---- key 2: the Midnight Poem wheel
        entry(2, "tranquilize", "Tranquilize", KnightOfMisfortunePoems.TRANQUILIZE_SEQUENCE, KnightOfMisfortunePoems.TRANQUILIZE_COST,
                KnightOfMisfortunePoems.TRANQUILIZE_COOLDOWN, KnightOfMisfortunePoems.TRANQUILIZE_LOOK, false,
                KnightOfMisfortunePoems::tranquilize, (mob, target, sl) -> !Spells.harmfulEffects(mob).isEmpty());
        entry(2, "lullaby", "Lullaby", KnightOfMisfortunePoems.LULLABY_SEQUENCE, KnightOfMisfortunePoems.LULLABY_COST,
                KnightOfMisfortunePoems.LULLABY_COOLDOWN, KnightOfMisfortunePoems.LULLABY_RANGE, true, KnightOfMisfortunePoems::lullaby, null);
        entry(2, "pacify", "Pacify", KnightOfMisfortunePoems.PACIFY_SEQUENCE, KnightOfMisfortunePoems.PACIFY_COST,
                KnightOfMisfortunePoems.PACIFY_COOLDOWN, KnightOfMisfortunePoems.PACIFY_RANGE, true, KnightOfMisfortunePoems::pacify, null);
        entry(2, "fear", "Fear", KnightOfMisfortunePoems.FEAR_SEQUENCE, KnightOfMisfortunePoems.FEAR_COST,
                KnightOfMisfortunePoems.FEAR_COOLDOWN, KnightOfMisfortunePoems.FEAR_RANGE, true, KnightOfMisfortunePoems::fear, null);
        entry(2, "agitating", "Agitating", KnightOfMisfortunePoems.AGITATE_SEQUENCE, KnightOfMisfortunePoems.AGITATE_COST,
                KnightOfMisfortunePoems.AGITATE_COOLDOWN, KnightOfMisfortunePoems.AGITATE_RANGE, true, KnightOfMisfortunePoems::agitating, null);

        // ---- key 3: Nightmare Limbs
        MobAbilities.register(MobAbility.of(P, 3, MobAbility.NORMAL).named("Nightmare Limbs")
                .cost(KnightOfMisfortune.LIMBS_COST)
                .cooldown(Math.max(KnightOfMisfortune.LIMBS_COOLDOWN, KnightOfMisfortune.LIMBS_TICKS + 100))
                .range(KnightOfMisfortune.LIMBS_REACH + 2.0)
                .when((mob, target, sl) -> !KnightOfMisfortune.isActive(mob, KnightOfMisfortune.LIMBS_UNTIL))
                .cast(MobSpells.aimedCore(KnightOfMisfortune::nightmareLimbs)));

        // ---- key 4: Requiem / Soothing Requiem
        MobAbilities.register(MobAbility.of(P, 4, MobAbility.NORMAL)
                .cost(KnightOfMisfortune.REQUIEM_COST)
                .cooldown(Math.max(KnightOfMisfortune.REQUIEM_COOLDOWN, KnightOfMisfortune.REQUIEM_TICKS + 100))
                .range(KnightOfMisfortune.REQUIEM_RADIUS - 2.0)
                .cast(MobSpells.aimedCore(KnightOfMisfortune::requiem)));
        MobAbilities.register(MobAbility.of(P, 4, MobAbility.SNEAK).named("Soothing Requiem")
                .cost(KnightOfMisfortune.SOOTHE_COST).cooldown(SOOTHE_MOB_COOLDOWN)
                .when((mob, target, sl) -> !Spells.harmfulEffects(mob).isEmpty()
                        || mob.getHealth() < mob.getMaxHealth() * 0.5f)
                .cast(MobSpells.aimedCore(KnightOfMisfortune::soothingRequiem)));

        // ---- key 5: the Spirit Commanding wheel (Gale Spirit is a player's flight)
        entry(5, "terror_banshee", "Terror Banshee", KnightOfMisfortuneSpirits.BANSHEE_SEQUENCE, KnightOfMisfortuneSpirits.BANSHEE_COST,
                Math.max(KnightOfMisfortuneSpirits.BANSHEE_COOLDOWN, KnightOfMisfortuneSpirits.BANSHEE_TICKS),
                KnightOfMisfortuneSpirits.BANSHEE_RADIUS, false, KnightOfMisfortuneSpirits::terrorBanshee, null);
        entry(5, "earthen_spirit", "Earthen Spirit", KnightOfMisfortuneSpirits.EARTH_SEQUENCE, KnightOfMisfortuneSpirits.EARTH_COST,
                KnightOfMisfortuneSpirits.EARTH_COOLDOWN, KnightOfMisfortuneSpirits.EARTH_RISE_RANGE, false,
                KnightOfMisfortuneSpirits::earthenSpirit,
                (mob, target, sl) -> !KnightOfMisfortune.isActive(mob, KnightOfMisfortuneSpirits.EARTH_UNTIL)
                        && mob.distanceTo(target) > EARTH_RISE_MOB_FAR);
        MobAbilities.register(MobAbility.of(P, 5, MobAbility.NORMAL).entry("earthen_rise").named("Earthen Spirit (rising)")
                .sequence(KnightOfMisfortuneSpirits.EARTH_SEQUENCE).cooldown(EARTH_RISE_MOB_COOLDOWN)        // free
                .range(EARTH_RISE_MOB_FAR).weight(30)
                .when((mob, target, sl) -> KnightOfMisfortune.isActive(mob, KnightOfMisfortuneSpirits.EARTH_UNTIL))
                .cast(MobSpells.aimedEntry(KnightOfMisfortuneSpirits::earthenSpirit)));
        entry(5, "command_spirits", "Command Spirits", KnightOfMisfortuneSpirits.COMMAND_SEQUENCE, KnightOfMisfortuneSpirits.COMMAND_COST,
                KnightOfMisfortuneSpirits.COMMAND_COOLDOWN, 24.0, false, KnightOfMisfortuneSpirits::commandSpirits,
                (mob, target, sl) -> Marionette.of(mob).size() < BeyonderMobRules.MAX_SUMMONS);

        // ---- key 6: the Nightwatcher wheel
        entry(6, "night_domain", "Night Domain", KnightOfMisfortuneNightwatcher.DOMAIN_SEQUENCE, KnightOfMisfortuneNightwatcher.DOMAIN_COST,
                KnightOfMisfortuneNightwatcher.DOMAIN_COOLDOWN, KnightOfMisfortuneNightwatcher.DOMAIN_RADIUS - 4.0, false,
                KnightOfMisfortuneNightwatcher::nightDomain,
                (mob, target, sl) -> !Zone.isOpen(mob, KnightOfMisfortuneNightwatcher.DOMAIN_ZONE));       // never to lift it
        entry(6, "hair_entanglement", "Hair Entanglement", KnightOfMisfortuneNightwatcher.HAIR_SEQUENCE, KnightOfMisfortuneNightwatcher.HAIR_COST,
                KnightOfMisfortuneNightwatcher.HAIR_COOLDOWN, KnightOfMisfortuneNightwatcher.HAIR_RANGE, true,
                KnightOfMisfortuneNightwatcher::hairEntanglement, null);
        entry(6, "curse_of_misfortune", "Curse of Misfortune", KnightOfMisfortuneNightwatcher.CURSE_SEQUENCE, KnightOfMisfortuneNightwatcher.CURSE_COST,
                KnightOfMisfortuneNightwatcher.CURSE_COOLDOWN, KnightOfMisfortuneNightwatcher.CURSE_RANGE, true,
                KnightOfMisfortuneNightwatcher::curseOfMisfortune, null);
        entry(6, "demonic_wolf", "Demonic Wolf Form", KnightOfMisfortuneNightwatcher.WOLF_SEQUENCE, KnightOfMisfortuneNightwatcher.WOLF_COST,
                KnightOfMisfortuneNightwatcher.WOLF_COOLDOWN, 16.0, false, KnightOfMisfortuneNightwatcher::demonicWolf,
                (mob, target, sl) -> !KnightOfMisfortuneNightwatcher.isWolf(mob));                           // never to leave it

        // ---- key 7: the Darkness and Concealment wheel
        entry(7, "sword_of_darkness", "Sword of Darkness", KnightOfMisfortuneDarkness.SWORD_SEQUENCE, KnightOfMisfortuneDarkness.SWORD_COST,
                KnightOfMisfortuneDarkness.SWORD_COOLDOWN, 12.0, false, KnightOfMisfortuneDarkness::swordOfDarkness,
                (mob, target, sl) -> !ConjuredWeapon.isDrawn(mob, KnightOfMisfortuneDarkness.SWORD));
        entry(7, "nightmare_world", "Nightmare World", KnightOfMisfortuneDarkness.WORLD_SEQUENCE, KnightOfMisfortuneDarkness.WORLD_COST,
                KnightOfMisfortuneDarkness.WORLD_COOLDOWN, KnightOfMisfortuneDarkness.WORLD_RADIUS - 4.0, false,
                KnightOfMisfortuneDarkness::nightmareWorld,
                (mob, target, sl) -> !Dome.isRaised(mob, KnightOfMisfortuneDarkness.WORLD_DOME));
        entry(7, "concealed_state", "Concealed State", KnightOfMisfortuneDarkness.CONCEAL_SEQUENCE, KnightOfMisfortuneDarkness.CONCEAL_COST,
                KnightOfMisfortuneDarkness.CONCEAL_COOLDOWN, KnightOfMisfortuneDarkness.CONCEAL_RANGE, true,
                KnightOfMisfortuneDarkness::concealedState, null);
        entry(7, "secret_concealment", "Secret Concealment", KnightOfMisfortuneDarkness.SECRET_SEQUENCE, KnightOfMisfortuneDarkness.SECRET_COST,
                Math.max(KnightOfMisfortuneDarkness.SECRET_COOLDOWN, KnightOfMisfortuneDarkness.SECRET_TICKS), 24.0, false,
                KnightOfMisfortuneDarkness::secretConcealment,
                (mob, target, sl) -> !Zone.isOpen(mob, KnightOfMisfortuneDarkness.SECRET_ZONE));
        entry(7, "concealed_world", "Concealed World", KnightOfMisfortuneDarkness.CW_SEQUENCE, KnightOfMisfortuneDarkness.CW_COST,
                KnightOfMisfortuneDarkness.CW_COOLDOWN, KnightOfMisfortuneDarkness.CW_RADIUS - 2.0, false,
                KnightOfMisfortuneDarkness::concealedWorld,
                (mob, target, sl) -> !Dome.isRaised(mob, KnightOfMisfortuneDarkness.CW_DOME));
        entry(7, "mantle_of_darkness", "Mantle of Darkness", KnightOfMisfortuneDarkness.MANTLE_SEQUENCE, KnightOfMisfortuneDarkness.MANTLE_COST,
                KnightOfMisfortuneDarkness.MANTLE_COOLDOWN, 12.0, false, KnightOfMisfortuneDarkness::mantleOfDarkness,
                (mob, target, sl) -> !ConjuredWeapon.isDrawn(mob, KnightOfMisfortuneDarkness.MANTLE));
        entry(7, "eternal_rest", "Eternal Rest", KnightOfMisfortuneDarkness.REST_SEQUENCE, KnightOfMisfortuneDarkness.REST_COST,
                KnightOfMisfortuneDarkness.REST_COOLDOWN, KnightOfMisfortuneDarkness.REST_RANGE, true, KnightOfMisfortuneDarkness::eternalRest, null);
        entry(7, "destination_of_all_things", "Destination of All Things", KnightOfMisfortuneDarkness.DESTINATION_SEQUENCE,
                KnightOfMisfortuneDarkness.DESTINATION_COST, KnightOfMisfortuneDarkness.DESTINATION_COOLDOWN,
                KnightOfMisfortuneDarkness.DESTINATION_RANGE, true, KnightOfMisfortuneDarkness::destination, null);
    }

    /** One wheel entry as an ability of its own (aimed at its target; a refusal spends nothing). */
    private static void entry(int key, String entry, String name, int sequence, int cost, int cooldown, double range,
                              boolean sight, MobSpells.Entry cast, MobAbility.When when) {
        MobAbility ability = MobAbility.of(KnightOfMisfortune.PATHWAY, key, MobAbility.NORMAL).entry(entry).named(name)
                .sequence(sequence).cost(cost).cooldown(Math.max(100, cooldown))
                .range(range).cast(MobSpells.aimedEntry(cast));
        if (sight) ability.sight();
        if (when != null) ability.when(when);
        MobAbilities.register(ability);
    }

    // =========================
    // Events
    // =========================

    @Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class Events {

        /** A Knight Of Misfortune mob: what a player's tick does for its casts (the limbs, spirits, wolf, darkness). */
        @SubscribeEvent
        public static void onKnightOfMisfortuneMobTick(LivingEvent.LivingTickEvent event) {
            if (!(event.getEntity() instanceof Mob mob) || !(mob.level() instanceof ServerLevel sl)) return;
            if (!KnightOfMisfortune.PATHWAY.equals(Beyonders.pathwayOf(mob))) return;
            KnightOfMisfortune.limbsTick(mob, sl);
            KnightOfMisfortuneSpirits.tick(mob, sl);
            KnightOfMisfortuneNightwatcher.tick(mob, sl);
            KnightOfMisfortuneDarkness.tick(mob, sl);
        }
    }
}
