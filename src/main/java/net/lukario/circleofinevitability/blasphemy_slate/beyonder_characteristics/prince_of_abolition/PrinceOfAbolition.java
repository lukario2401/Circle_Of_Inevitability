package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.prince_of_abolition;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonder;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonders;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Physique;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.SoulCore;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.prince_of_abolition.PrinceOfAbolitionFx.Status;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.prince_of_abolition.PrinceOfAbolitionMarks.Kind;
import net.lukario.circleofinevitability.combat.Dome;
import net.lukario.circleofinevitability.combat.Later;
import net.lukario.circleofinevitability.combat.MeleeCombo;
import net.lukario.circleofinevitability.combat.DamageFx;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.combat.Wall;
import net.lukario.circleofinevitability.combat.Zone;
import net.lukario.circleofinevitability.particles.fx.ParticleShapes;
import net.lukario.circleofinevitability.root.Freeze;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.lukario.circleofinevitability.status.Concealment;
import net.lukario.circleofinevitability.status.Corrosion;
import net.lukario.circleofinevitability.status.Exile;
import net.lukario.circleofinevitability.status.Falter;
import net.lukario.circleofinevitability.status.FallGuard;
import net.lukario.circleofinevitability.status.Fear;
import net.lukario.circleofinevitability.status.Slumber;
import net.lukario.circleofinevitability.status.Unnoticed;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * Prince Of Abolition: the Black Emperor Pathway of Lord of the Mysteries (docs/PRINCE_OF_ABOLITION.md). Seven keys,
 * from Lawyer (Sequence 9) up to Black Emperor (0); every plain key has a normal and a sneak cast, and the three
 * wheels are the other way round (sneak opens the wheel, a normal press recasts the last pick):
 *
 *   1 Verdict        (Lawyer)        a gavel no one dodges; the enemy is Compromised    | sneak: Brainwash
 *   2 Brute Force    (Barbarian)     one blow with the whole body                       | sneak: Iron Will
 *   3 Bribe          (Briber)        the Bribe wheel: Weaken, Charm, Arrogance, Connect, Coin Shot (PrinceOfAbolitionBribes)
 *   4 Distort        (Baron of Corruption) a shot turned back, an enemy's intent, or (Duke of Entropy) an enemy
 *                                    shell flipped                                     | sneak: Seal (the illusory door;
 *                                                                                        a gate where you look from Earl of the Fallen)
 *   5 Paces          (Mentor of Disorder) distance Disordered: a few strides, a fake self | sneak: Disorder
 *   6 The Fallen     (Barbarian)     the Fallen wheel: Raise Mausoleum, Corrosion, Step, Majesty, Bestowment, Exploit,
 *                                    Magnify, Mausoleum Salvo, Mausoleum Blast (PrinceOfAbolitionFallen; the
 *                                    mausoleums themselves in PrinceOfAbolitionMausoleums)
 *   7 Sovereign      (Baron of Corruption) the Sovereign wheel: Ruin, Frenzy, Entropy, Abolition, Exile, Inversion,
 *                                    Coronation, Decree (PrinceOfAbolitionSovereign)
 *
 * Every Sequence is written, Lawyer (9) to Black Emperor (0). Passives: Law & Order, Silver Tongue (9), Physical
 * Enhancement and Mental Resistance (8), Everyone Has A Price (7), Weakness Detection (6), Presence (5), Favorable
 * Ground (4), Loose Order (2), Beyond the World (1), Their Order and Last Rebirth (0).
 *
 * Sequence Might: every Sequence reached adds 5% attack, defense, range, area and duration (range(...), area(...),
 * duration(...) below; attack and defense in the hurt event), and every standing mausoleum another 5% attack and
 * defense. Order: what the Beyonder leaves standing (PrinceOfAbolitionOrders). The small statuses put on others are
 * PrinceOfAbolitionMarks; what they do is read by the events here. Every look and sound is in PrinceOfAbolitionFx
 * (see /abolitionfx); nothing here breaks or places blocks.
 */
public class PrinceOfAbolition {

    public static final String PATHWAY = "Prince Of Abolition";

    // =========================
    // Sequence Might, mausoleums and Order (docs/PRINCE_OF_ABOLITION.md)
    // =========================
    static final float MIGHT_PER_SEQUENCE = 0.05f;      // attack, defense, range, area, duration: 5% for every Sequence reached
    static final float MIGHT_PER_MAUSOLEUM = 0.05f;     // attack and defense: 5% for every standing mausoleum
    static final float LAW_AND_ORDER_CUT = 0.02f;       // Law & Order (Lawyer): 2% less damage taken for every standing order
    static final int THEIR_ORDER_SEQUENCE = 0;          // Their Order (Black Emperor): what you leave standing lasts...
    static final float THEIR_ORDER_LONGER = 1.25f;      // ...25% longer, mausoleums too
    static final double CONNECT_REACH = 3.0;            // aimed abilities reach a Connected enemy at 3 times their range

    // =========================
    // Ability settings (cost = spirituality, ticks: 20 = 1 second, distances in blocks; base values before Might)
    // =========================

    // Ability 1 - Verdict (Sequence 9)
    static final int VERDICT_SEQUENCE = 9;
    static final int VERDICT_COST = 250;
    static final double VERDICT_RANGE = 20.0;
    static final float VERDICT_DAMAGE = 40f;
    static final int VERDICT_STAGGER = 12;
    static final int COMPROMISED_TICKS = 200;
    static final float COMPROMISED_EXTRA = 0.25f;       // a Compromised enemy takes 25% more from the one who judged it
    // sneak: Brainwash
    static final int BRAINWASH_COST = 250;
    static final double BRAINWASH_RANGE = 16.0;
    static final int BRAINWASH_MOB_TICKS = 160;
    static final int BRAINWASH_PLAYER_TICKS = 100;
    static final float BRAINWASH_CUT = 0.3f;            // a brainwashed player's blows on you are 30% weaker
    static final float BOSS_HEALTH = 150f;              // more than this: shrugs off Brainwash and Charm

    // Ability 2 - Brute Force (Sequence 8)
    static final int BRUTE_SEQUENCE = 8;
    static final int BRUTE_COST = 600;
    static final double BRUTE_REACH = 4.0;
    static final float BRUTE_DAMAGE = 60f;
    static final double BRUTE_THROW = 1.6;      // about 5 blocks
    static final int BRUTE_GUARD_BREAK = 100;   // no shield, no item use for 5 seconds
    static final double BRUTE_SPLASH = 2.0;     // those this close to it...
    static final float BRUTE_SPLASH_SHARE = 0.5f;   // ...take half and are knocked aside
    // sneak: Iron Will
    static final int IRON_COST = 600;
    static final int IRON_TICKS = 160;

    // Ability 4 - Distort (Sequence 6)
    static final int DISTORT_SEQUENCE = 6;
    static final int DISTORT_COST = 2450;
    static final double DISTORT_RANGE = 24.0;
    static final double DISTORT_SHOT_AIM = 1.5; // a projectile this close to the line of sight is picked
    static final int DISTORT_TICKS = 100;
    static final int DISTORT_CHARGES = 3;       // its next 3 attacks land on its nearest ally...
    static final double DISTORT_ALLY_REACH = 8.0;   // ...this close, or miss
    // sneak: Seal
    static final int SEAL_COST = 2450;
    static final double SEAL_RADIUS = 6.0;
    static final int SEAL_TICKS = 160;
    static final String SEAL_DOME = "poa_seal";
    // Earl of the Fallen: the door anywhere you look, and a wall instead of a room (combat/Wall)
    static final int GATE_SEQUENCE = 4;
    static final double GATE_RANGE = 24.0;
    static final double GATE_WIDTH = 10.0;
    static final double GATE_HEIGHT = 5.0;
    static final int GATE_TICKS = 200;
    static final String GATE_WALL = "poa_gate";
    // Duke of Entropy: Distort aimed at an enemy's shell flips it (combat/Dome.flip)
    static final int FLIP_SEQUENCE = 2;

    // Ability 5 - Disorder (Sequence 5)
    static final int DISORDER_SEQUENCE = 5;
    static final int DISORDER_COST = 3000;
    static final double DISORDER_RANGE = 20.0;
    static final int DISORDER_TICKS = 120;
    static final float DISORDER_MISS = 0.4f;            // 40% of its attacks miss
    static final int UNBOUND_TICKS = 60;        // freed: no hold takes for 3 seconds
    // sneak: Paces
    static final int PACES_COST = 3000;
    static final double PACES_RANGE = 24.0;
    static final double PACES_EARL_RANGE = 48.0;    // Earl of the Fallen: the standard of measure itself
    static final int PACES_EARL_SEQUENCE = 4;
    static final int DECOY_TICKS = 80;
    static final double DECOY_REACH = 1.6;      // an enemy this close to the fake self bursts it...
    static final double DECOY_BURST = 3.0;      // ...and everything this close is Disordered...
    static final int DECOY_DISORDER = 40;       // ...this long
    static final double DECOY_LURE = 24.0;      // mobs hunting you this close go for the fake self
    static final int UNTOUCHABLE_SEQUENCE = 4;  // Earl of the Fallen: after Disorder or Paces...
    static final int UNTOUCHABLE_TICKS = 40;    // ...everything aimed at you misses this long

    // Passives (the plain potion effects are in SequencePassives)
    static final int ANY_SEQUENCE = 9;
    static final int MENTAL_RESISTANCE_SEQUENCE = 8;    // Barbarian: mind marks half as long on you (PrinceOfAbolitionMarks)
    static final int PHYSICAL_SEQUENCE = 8;     // Physical Enhancement: two extra hearts (Strength I in SequencePassives)
    static final int PRICE_SEQUENCE = 7;        // Everyone Has A Price: piglins never turn on you
    static final int WEAKNESS_SEQUENCE = 6;     // Weakness Detection: what you look at is marked for you...
    static final double WEAKNESS_SIGHT = 24.0;
    static final float WEAKNESS_CRIT = 1.5f;    // ...and the first hit on one that has not noticed you is a crit
    static final int PRESENCE_SEQUENCE = 5;     // Presence: weak hostile mobs near you hesitate...
    static final double PRESENCE_RADIUS = 6.0;
    static final float PRESENCE_MAX_HEALTH = 30f;
    static final float PRESENCE_IGNORE = 0.5f;  // ...and are half as likely to pick you
    static final int FAVORABLE_SEQUENCE = 4;    // Favorable Ground: hidden or invisible enemies this close...
    static final double FAVORABLE_RADIUS = 16.0;    // ...glow for you and cannot stay hidden
    static final int LOOSE_SEQUENCE = 2;        // Loose Order: projectiles coming at you...
    static final double LOOSE_RADIUS = 10.0;    // ...this close...
    static final double LOOSE_DEGREES = 10.0;   // ...drift up to this many degrees off
    static final int BEYOND_SEQUENCE = 1;       // Beyond the World: damage with no attacker...
    static final float BEYOND_CUT = 0.5f;       // ...is half lost

    // =========================
    // Saved on the player (game times are "until" this tick)
    // =========================
    static final String IRON_UNTIL = "poa_iron_until";
    static final String IRON_TAG = "poa_iron_tag";
    static final String UNBOUND_UNTIL = "poa_unbound_until";
    static final String UNTOUCHABLE_UNTIL = "poa_untouchable_until";
    static final String WAS_PRINCE = "poa_was_prince";
    // on an enemy
    static final String DISTORT_LEFT = "poa_distort_left";
    static final String NOTICED_BY = "poa_noticed_by";

    static final Identifier HEARTS_ID = Physique.id("poa_physical_enhancement");
    static final Identifier IRON_FOOTING_ID = Physique.id("poa_iron_will_footing");
    static final String DRIFTED = "poa_drifted";      // on a projectile Loose Order has bent already

    /** True while a Distorted blow is being passed on (so the passed-on blow is not Distorted again). */
    private static boolean redirecting;
    /** True while Inversion turns a heal into harm (so nothing that harm sets off is turned again). */
    private static boolean inverting;

    /** The melee combo (docs/PRINCE_OF_ABOLITION.md, "Melee combo"), registered in combat/MeleeCombos. */
    public static final MeleeCombo MELEE_COMBO = MeleeCombo.forPathway(PATHWAY)
            .step((player, target, level) -> PrinceOfAbolitionFx.combo(level, player, target, 0))
            .step((player, target, level) -> PrinceOfAbolitionFx.combo(level, player, target, 1))
            .step((player, target, level) -> PrinceOfAbolitionFx.combo(level, player, target, 2));

    @Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class PrinceOfAbolitionEvents {

        static {
            // a brainwashed player trusts the one who did it: no aimed ability of theirs locks onto them
            Spells.addUnchoosableCheck((chooser, target) -> chooser != null && PrinceOfAbolitionMarks.from(chooser, Kind.BRAINWASHED, target));
            // Earl of the Fallen, just after Disorder or Paces: nothing can be aimed at them
            Spells.addUnchoosableCheck((chooser, target) -> isActive(target, UNTOUCHABLE_UNTIL)
                    && (chooser == null || chooser != target));
            // Iron Will: fear and sleep do not take
            Fear.addImmunity(entity -> entity instanceof Player player && shrugsMind(player));
            Slumber.addImmunity(entity -> entity instanceof Player player && shrugsMind(player));
            // Abolition: nothing is aimed across the shell, either way; Black Emperor: no outside zone reaches in
            Spells.addUnchoosableCheck((chooser, target) -> chooser != null && PrinceOfAbolitionSovereign.separated(chooser, target));
            Zone.addShelter(PrinceOfAbolitionSovereign::shelteredFromZone);
            // the look of a miss this Pathway caused (status/Falter does the missing)
            Falter.addListener((attacker, victim, name) -> {
                if (name.startsWith("poa_") && victim.level() instanceof ServerLevel sl) {
                    PrinceOfAbolitionFx.looseOrder(sl, victim.position().add(0, victim.getBbHeight() * 0.6, 0));
                }
            });
            // the look of the rot of Corrosion and Ruin (status/Corrosion does the rotting)
            Corrosion.addListener(new Corrosion.Listener() {
                @Override
                public void started(LivingEntity victim, LivingEntity source) {
                    if (source == null || !isPrince(source, ANY_SEQUENCE) || !(victim.level() instanceof ServerLevel sl)) return;
                    PrinceOfAbolitionFx.corroded(sl, victim, PrinceOfAbolitionFx.FOREVER, rotTag(victim));
                    if (victim instanceof Player bearer) PrinceOfAbolitionFx.statusForever(bearer, Status.CORRODED);
                }

                @Override
                public void bit(LivingEntity victim, float amount, LivingEntity source) {
                    if (source != null && isPrince(source, ANY_SEQUENCE) && victim.level() instanceof ServerLevel sl) {
                        PrinceOfAbolitionFx.corrosionBite(sl, victim, amount);
                    }
                }

                @Override
                public void ended(LivingEntity victim, Corrosion.End why) {
                    if (victim.level() instanceof ServerLevel sl) ParticleShapes.clearModels(sl, victim, rotTag(victim));
                    if (victim instanceof Player bearer) PrinceOfAbolitionFx.statusEnd(bearer, Status.CORRODED);
                }
            });
        }

        @SubscribeEvent
        public static void onPrinceOfAbolitionTick(TickEvent.PlayerTickEvent.Post event) {
            Player player = event.player();
            if (!(player.level() instanceof ServerLevel sl)) return;
            PrinceOfAbolitionMausoleums.swingTick(player, sl);          // anyone may strike someone else's mausoleum
            boolean prince = isPrince(player, ANY_SEQUENCE);
            if (!prince) {
                if (player.getPersistentData().getBooleanOr(WAS_PRINCE, false)) lostPathway(player, sl);
                return;
            }
            player.getPersistentData().putBoolean(WAS_PRINCE, true);
            if (isActive(player, UNBOUND_UNTIL)) shakeHolds(player);
            if (player.tickCount % 2 == 0 && isPrince(player, LOOSE_SEQUENCE)) looseOrder(player, sl);
            if (player.tickCount % 10 == 0 && isPrince(player, WEAKNESS_SEQUENCE)) weaknessDetection(player, sl);
            if (player.tickCount % 20 == 0) {
                bodyTick(player);
                if (isPrince(player, FAVORABLE_SEQUENCE)) favorableGround(player, sl);
                PrinceOfAbolitionOrders.tick(player);
                PrinceOfAbolitionMausoleums.ownerTick(player, sl);
            }
        }

        @SubscribeEvent
        public static void onPrinceOfAbolitionHurt(LivingHurtEvent event) {
            LivingEntity victim = event.getEntity();
            if (victim.level().isClientSide()) return;
            DamageSource source = event.getSource();
            LivingEntity attacker = source.getEntity() instanceof LivingEntity living && living != victim ? living : null;
            float amount = event.getAmount();

            // what the attacker carries
            if (attacker != null) {
                if (PrinceOfAbolitionMarks.has(attacker, Kind.WEAKENED)) amount *= 1f - PrinceOfAbolitionBribes.WEAKEN_CUT;
                if (PrinceOfAbolitionMarks.from(attacker, Kind.CHARMED, victim)) amount *= 1f - PrinceOfAbolitionBribes.CHARM_CUT;
                if (PrinceOfAbolitionMarks.from(attacker, Kind.BRAINWASHED, victim)) amount *= 1f - BRAINWASH_CUT;
                if (PrinceOfAbolitionMarks.has(attacker, Kind.DEFEATED)) amount *= 1f - PrinceOfAbolitionFallen.DEFEATED_CUT;
                if (attacker instanceof Player && PrinceOfAbolitionMarks.has(attacker, Kind.AVARICIOUS)) amount *= 1f - PrinceOfAbolitionFallen.AVARICE_CUT;
                if (victim.level() instanceof ServerLevel sl) amount *= PrinceOfAbolitionSovereign.entropySwing(sl, attacker);
            }
            // the Prince Of Abolition strikes: Sequence Might, the mausoleums, and what it has put on the victim
            if (attacker != null && isPrince(attacker, ANY_SEQUENCE)) {                 // a player, or a Beyonder mob of it
                LivingEntity prince = attacker;
                amount *= attack(prince);
                if (PrinceOfAbolitionMarks.from(victim, Kind.COMPROMISED, prince)) amount *= 1f + COMPROMISED_EXTRA;
                if (PrinceOfAbolitionMarks.from(victim, Kind.WEAKENED, prince)) amount *= 1f + PrinceOfAbolitionBribes.WEAKEN_EXTRA;
                if (isPrince(prince, WEAKNESS_SEQUENCE) && victim instanceof Mob mob && mob.getTarget() != prince
                        && !prince.getUUID().toString().equals(victim.getPersistentData().getStringOr(NOTICED_BY, ""))) {
                    amount *= WEAKNESS_CRIT;                             // it never saw you coming
                    if (victim.level() instanceof ServerLevel sl) PrinceOfAbolitionFx.magnifiedHit(sl, victim.position().add(0, victim.getBbHeight() * 0.6, 0));
                }
                if (isPrince(prince, WEAKNESS_SEQUENCE)) victim.getPersistentData().putString(NOTICED_BY, prince.getUUID().toString());
                amount = PrinceOfAbolitionFallen.magnifyHit(prince, victim, amount);
            }
            // the Prince Of Abolition is struck: Sequence Might, the mausoleums, Law & Order
            if (isPrince(victim, ANY_SEQUENCE) && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
                LivingEntity prince = victim;
                amount /= defense(prince);
                amount *= 1f - LAW_AND_ORDER_CUT * PrinceOfAbolitionOrders.count(prince);
                if (attacker == null && source.getEntity() == null && isPrince(prince, BEYOND_SEQUENCE)) {
                    amount *= 1f - BEYOND_CUT;                           // Beyond the World: harm with no hand behind it
                    if (prince.tickCount % 10 == 0 && prince.level() instanceof ServerLevel sl) PrinceOfAbolitionFx.beyondTheWorld(sl, prince);
                }
            }
            if (amount != event.getAmount()) event.setAmount(Math.max(0f, amount));
        }

        /**
         * Blows that do not land: across an Abolition shell, turned into healing by Inversion, the untouchable Earl,
         * Distorted blows landing on an ally. (The misses of Arrogance, Disorder, Corrosion and Anxious are
         * status/Falter's, put on by PrinceOfAbolitionMarks.)
         */
        @SubscribeEvent
        public static boolean onPrinceOfAbolitionAttacked(LivingAttackEvent event) {
            LivingEntity victim = event.getEntity();
            if (!(victim.level() instanceof ServerLevel sl) || redirecting) return false;
            DamageSource source = event.getSource();
            if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
            LivingEntity attacker = source.getEntity() instanceof LivingEntity living && living != victim ? living : null;
            Entity from = attacker != null ? attacker : source.getDirectEntity();
            if (from != null && PrinceOfAbolitionSovereign.separated(from, victim)) {
                PrinceOfAbolitionFx.looseOrder(sl, victim.position().add(0, victim.getBbHeight() * 0.6, 0));
                return true;                                             // Abolition: nothing reaches across
            }
            if (attacker == null) return false;

            Player inverter = PrinceOfAbolitionSovereign.inverterGuarding(victim, attacker);
            if (inverter != null) {
                inverter.heal(event.getAmount());                        // Inversion: their blow heals you
                PrinceOfAbolitionFx.inverted(sl, attacker);
                return true;
            }
            if (victim instanceof Player prince && isActive(prince, UNTOUCHABLE_UNTIL)) {
                PrinceOfAbolitionFx.untouchable(sl, prince);
                return true;
            }
            if (PrinceOfAbolitionMarks.has(attacker, Kind.DISTORTED)) return distortedBlow(sl, attacker, victim, source, event.getAmount());
            return false;
        }

        /** Mobs that will not pick the one who bribed, brainwashed or charmed them; Presence; piglins. */
        @SubscribeEvent
        public static boolean onPrinceOfAbolitionTarget(LivingChangeTargetEvent event) {
            LivingEntity mob = event.getEntity();
            if (mob.level().isClientSide() || event.getNewTarget() == null) return false;
            if (PrinceOfAbolitionMarks.has(mob, Kind.DEFEATED) || PrinceOfAbolitionMarks.has(mob, Kind.AVARICIOUS)) return true;
            if (PrinceOfAbolitionSovereign.separated(mob, event.getNewTarget())) return true;      // Abolition
            if (!(event.getNewTarget() instanceof Player wanted)) return false;
            if (PrinceOfAbolitionMarks.from(mob, Kind.BRAINWASHED, wanted)) return true;
            if (PrinceOfAbolitionMarks.from(mob, Kind.CHARMED, wanted)) return true;
            if (PrinceOfAbolitionMarks.has(mob, Kind.DISORDERED) && mob.getRandom().nextFloat() < DISORDER_MISS) return true;
            if (!isPrince(wanted, ANY_SEQUENCE)) return false;
            if (isPrince(wanted, PRICE_SEQUENCE) && mob.getType() == EntityTypes.PIGLIN) return true;
            return isPrince(wanted, PRESENCE_SEQUENCE) && weakHostile(mob) && mob.distanceTo(wanted) <= PRESENCE_RADIUS
                    && mob.getRandom().nextFloat() < PRESENCE_IGNORE;
        }

        /** Inversion: the healing an enemy receives in the field hurts it instead. (Corrosion's no-healing is status/NoHeal.) */
        @SubscribeEvent
        public static boolean onPrinceOfAbolitionHeal(LivingHealEvent event) {
            LivingEntity entity = event.getEntity();
            if (!(entity.level() instanceof ServerLevel sl) || inverting) return false;
            Player inverter = PrinceOfAbolitionSovereign.inverterOver(entity);
            if (inverter == null) return false;
            inverting = true;
            try {
                DamageFx.credit(inverter, entity);
                entity.invulnerableTime = 0;
                entity.hurt(sl.damageSources().magic(), event.getAmount());
            } finally {
                inverting = false;
            }
            return true;
        }

        /** A mark does not outlive its bearer: its order goes too. */
        @SubscribeEvent
        public static void onPrinceOfAbolitionDeath(LivingDeathEvent event) {
            if (event.getEntity().level() instanceof ServerLevel sl) PrinceOfAbolitionOrders.bearerDied(sl, event.getEntity());
        }

        // what was on the screen does not survive relogging or dying: start clean
        @SubscribeEvent
        public static void onPrinceOfAbolitionLogin(PlayerEvent.PlayerLoggedInEvent event) {
            clearState(event.getEntity());
        }

        @SubscribeEvent
        public static void onPrinceOfAbolitionRespawn(PlayerEvent.PlayerRespawnEvent event) {
            clearState(event.getEntity());
        }

        @SubscribeEvent
        public static void onPrinceOfAbolitionLogout(PlayerEvent.PlayerLoggedOutEvent event) {
            Player player = event.getEntity();
            if (player.level() instanceof ServerLevel sl) PrinceOfAbolitionMausoleums.crumbleAll(player, sl);
            PrinceOfAbolitionOrders.clear(player);
        }
    }

    // =========================
    // Ability 1 - Verdict
    // Normal: a black gavel falls on the enemy you look at (VERDICT_RANGE) and cannot be dodged: VERDICT_DAMAGE and a
    //         short stagger. It finds the loophole: the enemy is Compromised for COMPROMISED_TICKS (it takes
    //         COMPROMISED_EXTRA more from you, and your Bribes and Disorder last half as long again on it).
    // Sneak:  Brainwash. A few quiet words for the enemy you look at (BRAINWASH_RANGE). A mob forgets whom it hunted and
    //         cannot pick you for BRAINWASH_MOB_TICKS; a player trusts you for BRAINWASH_PLAYER_TICKS: no aimed ability
    //         of theirs locks onto you and their blows on you are BRAINWASH_CUT weaker. Bosses shrug it off (free).
    // =========================
    public static void princeOfAbolitionVerdict(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!ready(player, bypassClassCheck, VERDICT_SEQUENCE)) return;

        if (player.isShiftKeyDown()) {
            LivingEntity target = aim(player, sl, BRAINWASH_RANGE);
            if (target == null) {
                player.sendSystemMessage(Component.literal("There is no one close enough to hear your words."));
                return; // nothing spent
            }
            if (target.getMaxHealth() > BOSS_HEALTH) {
                player.sendSystemMessage(Component.literal("Its will is too great for a few quiet words."));
                return; // nothing spent
            }
            if (!Spells.paySpirituality(player, BRAINWASH_COST)) return;
            brainwash(player, sl, target);
            return;
        }
        LivingEntity target = aim(player, sl, VERDICT_RANGE);
        if (target == null) {
            player.sendSystemMessage(Component.literal("There is no one before you to judge."));
            return; // nothing spent
        }
        if (!Spells.paySpirituality(player, VERDICT_COST)) return;
        verdict(player, sl, target);
    }

    static void verdict(LivingEntity player, ServerLevel sl, LivingEntity target) {
        PrinceOfAbolitionFx.verdict(sl, player, target);
        Later.run(sl, PrinceOfAbolitionFx.VERDICT_LANDS, () -> {
            if (!Spells.casterStillHere(player, sl) || !target.isAlive() || target.level() != sl) return;
            Spells.strike(player, target, VERDICT_DAMAGE);
            if (!target.isAlive()) return;
            Root.apply(target, VERDICT_STAGGER, RootRestriction.MOVE, RootRestriction.JUMP);
            int ticks = PrinceOfAbolitionMarks.put(player, target, Kind.COMPROMISED, duration(player, COMPROMISED_TICKS));
            if (ticks > 0) PrinceOfAbolitionFx.compromised(sl, target, ticks, PrinceOfAbolitionMarks.tag(target, Kind.COMPROMISED));
        });
    }

    static void brainwash(LivingEntity player, ServerLevel sl, LivingEntity target) {
        int wanted = duration(player, target instanceof Player ? BRAINWASH_PLAYER_TICKS : BRAINWASH_MOB_TICKS);
        String tag = PrinceOfAbolitionMarks.tag(target, Kind.BRAINWASHED);
        PrinceOfAbolitionFx.brainwash(sl, player, target, wanted, tag);
        Later.run(sl, PrinceOfAbolitionFx.WORDS_LAND, () -> {
            if (!Spells.casterStillHere(player, sl) || !target.isAlive() || target.level() != sl) return;
            int ticks = PrinceOfAbolitionMarks.put(player, target, Kind.BRAINWASHED, wanted);
            if (ticks <= 0) {
                PrinceOfAbolitionFx.brainwashEnd(sl, target, tag);
                return;
            }
            if (target instanceof Mob mob && mob.getTarget() == player) mob.setTarget(null);    // it no longer knows whom it hunted
        });
    }

    // =========================
    // Ability 2 - Brute Force
    // Normal: where the law fails, force rules. One blow with your whole body on the enemy in front of you
    //         (BRUTE_REACH): BRUTE_DAMAGE, thrown back, and its guard broken for BRUTE_GUARD_BREAK (no shield, no item
    //         use). Enemies within BRUTE_SPLASH of it take BRUTE_SPLASH_SHARE and are knocked aside.
    // Sneak:  Iron Will. For IRON_TICKS every mind effect ends on you at once and none can be put on you (Brainwash,
    //         Charm, Arrogance, Corrosion, Majesty, fear, sleep), and nothing knocks you back.
    // =========================
    public static void princeOfAbolitionBruteForce(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!ready(player, bypassClassCheck, BRUTE_SEQUENCE)) return;

        if (player.isShiftKeyDown()) {
            if (!Spells.paySpirituality(player, IRON_COST)) return;
            ironWill(player, sl);
            return;
        }
        double reach = range(player, BRUTE_REACH);
        LivingEntity target = Spells.aimEnemy(player, sl, reach);
        if (target == null) {
            List<LivingEntity> ahead = Spells.enemiesInCone(player, sl, player.getEyePosition(), player.getLookAngle(), reach, 35.0);
            if (!ahead.isEmpty()) target = ahead.get(0);
        }
        if (target == null) {
            player.sendSystemMessage(Component.literal("There is no one within reach of your fist."));
            return; // nothing spent
        }
        if (!Spells.paySpirituality(player, BRUTE_COST)) return;
        bruteForce(player, sl, target, reach);
    }

    static void bruteForce(LivingEntity player, ServerLevel sl, LivingEntity target, double reach) {
        PrinceOfAbolitionFx.bruteForce(sl, player, target);
        Later.run(sl, PrinceOfAbolitionFx.BRUTE_LANDS, () -> {
            if (!Spells.casterStillHere(player, sl) || !target.isAlive() || target.level() != sl) return;
            if (player.distanceTo(target) > reach + 1.5 + target.getBbWidth()) return;          // it got away
            Spells.strike(player, target, BRUTE_DAMAGE);
            if (target.isAlive()) {
                Spells.push(target, player.position(), BRUTE_THROW, 0.4);
                target.stopUsingItem();                                  // the shield comes down...
                Root.apply(target, BRUTE_GUARD_BREAK, RootRestriction.USE_ITEMS);   // ...and stays down
            }
            for (LivingEntity near : Spells.enemiesAround(player, sl, target.position(), area(player, BRUTE_SPLASH))) {
                if (near == target) continue;
                Spells.strike(player, near, BRUTE_DAMAGE * BRUTE_SPLASH_SHARE);
                if (near.isAlive()) Spells.push(near, target.position(), 0.8, 0.25);
            }
        });
    }

    static void ironWill(LivingEntity player, ServerLevel sl) {
        CompoundTag data = player.getPersistentData();
        PrinceOfAbolitionFx.ironWillEnd(sl, player, data.getStringOr(IRON_TAG, ""));     // cast again: one halo, not two
        long until = sl.getGameTime() + IRON_TICKS;
        String tag = "poa_iron_" + player.getId() + "_" + sl.getGameTime();
        data.putLong(IRON_UNTIL, until);
        data.putString(IRON_TAG, tag);
        PrinceOfAbolitionFx.ironWill(sl, player, IRON_TICKS, tag);
        PrinceOfAbolitionFx.status(player, Status.IRON_WILL, IRON_TICKS);
        if (shakeOffMind(player)) PrinceOfAbolitionFx.mindShrug(sl, player);
        Later.run(sl, IRON_TICKS, () -> {
            if (player.getPersistentData().getLongOr(IRON_UNTIL, 0L) != until) return;
            player.getPersistentData().remove(IRON_UNTIL);
            player.getPersistentData().remove(IRON_TAG);
            if (Spells.casterStillHere(player, sl)) PrinceOfAbolitionFx.ironWillEnd(sl, player, tag);
        });
    }

    /** Every mind effect on the player ends. True if there was one. */
    private static boolean shakeOffMind(LivingEntity player) {
        boolean any = PrinceOfAbolitionMarks.endMind(player);
        if (Fear.isAfraid(player)) {
            Fear.end(player);
            any = true;
        }
        if (Slumber.isAsleep(player) || Slumber.isLimp(player)) {
            Slumber.wake(player);
            any = true;
        }
        if (player.hasEffect(MobEffects.NAUSEA)) {
            player.removeEffect(MobEffects.NAUSEA);
            any = true;
        }
        return any;
    }

    /** True while Iron Will holds: no mind effect takes on them. */
    static boolean shrugsMind(LivingEntity player) {
        return isActive(player, IRON_UNTIL);
    }

    // =========================
    // Ability 4 - Distort
    // Normal: twist what you look at (DISTORT_RANGE). A projectile in flight (arrow, fireball, trident...) becomes
    //         yours and flies back at whoever sent it. Otherwise the enemy you look at has its intent Distorted for
    //         DISTORT_TICKS: its next DISTORT_CHARGES attacks (melee, ranged or ability) land on its nearest ally within
    //         DISTORT_ALLY_REACH instead, or miss if it has none. Duke of Entropy (2): aimed at an enemy's shell (a
    //         dome: a Seal, a Protection, a Kingdom), it flips it: what it kept out now keeps its owner in (Dome.flip).
    // Sneak:  Seal. An illusory door of black fog and gold rises before you, swings open for a heartbeat, and you slam
    //         it shut (PrinceOfAbolitionFx.SEAL_SLAMS): a room of SEAL_RADIUS round you is Sealed for SEAL_TICKS.
    //         Nothing crosses its shell but you and your allies: enemies cannot come in or go out, and their
    //         projectiles stop at it (combat/Dome). Casting it again closes the old room first. Earl of the Fallen
    //         (4): a gate instead, anywhere you look (gate).
    // =========================
    public static void princeOfAbolitionDistort(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!ready(player, bypassClassCheck, DISTORT_SEQUENCE)) return;

        if (player.isShiftKeyDown()) {
            if (Beyonder.hasSequence(player, GATE_SEQUENCE)) {
                Vec3 middle = gateSpot(player, sl);
                if (middle == null) {
                    player.sendSystemMessage(Component.literal("There is nowhere there for a door to stand."));
                    return; // nothing spent
                }
                if (!Spells.paySpirituality(player, SEAL_COST)) return;
                gate(player, sl, middle);
                return;
            }
            if (!Spells.paySpirituality(player, SEAL_COST)) return;
            seal(player, sl);
            return;
        }
        double range = range(player, DISTORT_RANGE);
        Projectile shot = aimedProjectile(player, sl, range);
        if (shot != null) {
            if (!Spells.paySpirituality(player, DISTORT_COST)) return;
            distortShot(player, sl, shot);
            return;
        }
        if (Beyonder.hasSequence(player, FLIP_SEQUENCE)) {
            Dome.Raised shell = aimedShell(player, sl, range);
            if (shell != null) {
                if (!Spells.paySpirituality(player, DISTORT_COST)) return;
                Dome.flip(shell.owner(), shell.name());
                PrinceOfAbolitionFx.shellFlip(sl, shell.center(), shell.radius());
                Spells.tell(shell.owner(), Component.literal("Your shell turns inside out: it holds you in now."));
                return;
            }
        }
        LivingEntity target = aim(player, sl, DISTORT_RANGE);
        if (target == null) {
            player.sendSystemMessage(Component.literal("There is nothing in sight to twist."));
            return; // nothing spent
        }
        if (!Spells.paySpirituality(player, DISTORT_COST)) return;
        distortIntent(player, sl, target);
    }

    /** The projectile nearest to the line of sight, within 'range' and DISTORT_SHOT_AIM of it, not the caster's own. */
    static Projectile aimedProjectile(LivingEntity player, ServerLevel sl, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 end = eye.add(look.scale(range));
        AABB box = new AABB(eye, end).inflate(DISTORT_SHOT_AIM + 0.5);
        Projectile best = null;
        double bestOff = DISTORT_SHOT_AIM;
        for (Projectile shot : sl.getEntitiesOfClass(Projectile.class, box, Entity::isAlive)) {
            if (shot.getOwner() == player) continue;
            Vec3 to = shot.position().subtract(eye);
            double along = to.dot(look);
            if (along < 0 || along > range) continue;
            double off = to.subtract(look.scale(along)).length();
            if (off < bestOff) {
                best = shot;
                bestOff = off;
            }
        }
        return best;
    }

    static void distortShot(LivingEntity player, ServerLevel sl, Projectile shot) {
        Entity sender = shot.getOwner();
        Vec3 at = shot.position();
        Vec3 velocity = shot.getDeltaMovement();
        Vec3 back = sender != null && sender.isAlive() && sender.level() == sl
                ? sender.position().add(0, sender.getBbHeight() * 0.6, 0).subtract(at)
                : velocity.scale(-1);
        if (back.lengthSqr() < 1.0E-4) back = player.getLookAngle();
        double speed = Math.max(1.2, velocity.length());
        Vec3 way = back.normalize();
        shot.setOwner(player);
        shot.shoot(way.x, way.y, way.z, (float) speed, 0f);
        shot.hurtMarked = true;
        PrinceOfAbolitionFx.distortShot(sl, player, at, sender == null ? null : sender.position());
    }

    static void distortIntent(LivingEntity player, ServerLevel sl, LivingEntity target) {
        int ticks = PrinceOfAbolitionMarks.put(player, target, Kind.DISTORTED, duration(player, DISTORT_TICKS));
        if (ticks <= 0) return;
        target.getPersistentData().putInt(DISTORT_LEFT, DISTORT_CHARGES);
        PrinceOfAbolitionFx.distortIntent(sl, player, target, ticks, PrinceOfAbolitionMarks.tag(target, Kind.DISTORTED));
    }

    /** A Distorted attacker's blow lands on its nearest ally instead (or misses). Always cancels the blow on 'victim'. */
    private static boolean distortedBlow(ServerLevel sl, LivingEntity attacker, LivingEntity victim, DamageSource source, float amount) {
        int left = attacker.getPersistentData().getIntOr(DISTORT_LEFT, 0) - 1;
        if (left <= 0) {
            attacker.getPersistentData().remove(DISTORT_LEFT);
            PrinceOfAbolitionMarks.end(attacker, Kind.DISTORTED);
        } else {
            attacker.getPersistentData().putInt(DISTORT_LEFT, left);
        }
        LivingEntity ally = nearestAllyOf(sl, attacker, victim);
        if (ally == null) {
            PrinceOfAbolitionFx.looseOrder(sl, victim.position().add(0, victim.getBbHeight() * 0.6, 0));
            return true;
        }
        PrinceOfAbolitionFx.distortedStrike(sl, attacker, ally);
        redirecting = true;
        try {
            ally.invulnerableTime = 0;
            ally.hurt(source, amount);
        } finally {
            redirecting = false;
        }
        return true;
    }

    /**
     * The attacker's nearest "ally": for a blow meant for a Prince Of Abolition (or their side), the nearest other one
     * who is also that Prince's enemy; otherwise the nearest other living thing that is not the victim.
     */
    private static LivingEntity nearestAllyOf(ServerLevel sl, LivingEntity attacker, LivingEntity victim) {
        LivingEntity distorter = PrinceOfAbolitionMarks.owner(attacker, Kind.DISTORTED);
        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        AABB box = attacker.getBoundingBox().inflate(DISTORT_ALLY_REACH);
        for (LivingEntity other : sl.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (other == attacker || other == victim || other == distorter) continue;
            if (distorter != null && !Spells.isEnemy(distorter, other)) continue;     // never the Prince's own side
            double distance = other.distanceToSqr(attacker);
            if (distance < bestDistance && distance <= DISTORT_ALLY_REACH * DISTORT_ALLY_REACH) {
                best = other;
                bestDistance = distance;
            }
        }
        return best;
    }

    /** The nearest shell (a dome) of an enemy that the line of sight passes through within 'range', or null. */
    static Dome.Raised aimedShell(LivingEntity player, ServerLevel sl, double range) {
        Vec3 eye = player.getEyePosition();
        for (Dome.Raised shell : Dome.crossedBy(sl, eye, Spells.aimPoint(player, sl, range))) {
            if (shell.owner() != player && shell.owner().isAlive() && Spells.isEnemy(player, shell.owner())
                    && !Dome.isFlipped(shell.owner(), shell.name())) return shell;
        }
        return null;
    }

    /** Where the gate stands: on the ground where you look (GATE_RANGE), or the spot in the air you look at. */
    static Vec3 gateSpot(LivingEntity player, ServerLevel sl) {
        double range = range(player, GATE_RANGE);
        Vec3 aimed = Spells.aimPoint(player, sl, range);
        Vec3 ground = Spells.groundNear(player, sl, aimed, 3.0);
        return ground != null ? ground : aimed;
    }

    /**
     * Earl of the Fallen's Seal: the illusory door is summoned where you look and slams at once
     * (PrinceOfAbolitionFx.GATE_SLAMS): a wall GATE_WIDTH wide and GATE_HEIGHT tall (the size its lattice is drawn
     * at, so Sequence Might does not widen it) stands across your view for GATE_TICKS. Enemies cannot cross it either way and their projectiles stop at it (combat/Wall).
     */
    static void gate(LivingEntity player, ServerLevel sl, Vec3 middle) {
        float yaw = player.getYRot();
        int ticks = duration(player, GATE_TICKS);
        double width = GATE_WIDTH;                                  // the size its lattice is drawn at
        double height = GATE_HEIGHT;
        String tag = "poa_gate_" + player.getId() + "_" + sl.getGameTime();
        PrinceOfAbolitionFx.gate(sl, player, middle, yaw, ticks, tag);
        Later.run(sl, PrinceOfAbolitionFx.GATE_SLAMS, () -> {
            if (!Spells.casterStillHere(player, sl)) {
                PrinceOfAbolitionFx.gateEnd(sl, middle, yaw, tag);
                return;
            }
            int stands = Math.max(20, ticks - PrinceOfAbolitionFx.GATE_SLAMS);
            Wall.at(middle, yaw, width, height)
                    .lasts(stands)
                    .stopsProjectiles()
                    .onBlock(at -> PrinceOfAbolitionFx.distortShot(sl, player, at, null))
                    .onEnd(() -> {
                        PrinceOfAbolitionFx.gateEnd(sl, middle, yaw, tag);
                        PrinceOfAbolitionOrders.remove(player, "seal");
                        PrinceOfAbolitionFx.statusEnd(player, Status.SEAL);
                    })
                    .raise(player, sl, GATE_WALL);
            PrinceOfAbolitionFx.status(player, Status.SEAL, stands);
            if (isPrince(player, ANY_SEQUENCE)) PrinceOfAbolitionOrders.add(player, "seal", sl.getGameTime() + stands, null);
        });
    }

    static void seal(LivingEntity player, ServerLevel sl) {
        double radius = area(player, SEAL_RADIUS);
        int ticks = duration(player, SEAL_TICKS);
        Vec3 center = player.position();
        String tag = "poa_seal_" + player.getId() + "_" + sl.getGameTime();
        PrinceOfAbolitionFx.seal(sl, player, center, radius, ticks, tag);
        Later.run(sl, PrinceOfAbolitionFx.SEAL_SLAMS, () -> {
            if (!Spells.casterStillHere(player, sl)) {
                PrinceOfAbolitionFx.sealEnd(sl, center, radius, tag);
                return;
            }
            int stands = Math.max(20, ticks - PrinceOfAbolitionFx.SEAL_SLAMS);
            long until = sl.getGameTime() + stands;
            Dome.sphere(center.add(0, 0.5, 0), radius)
                    .lasts(stands)
                    .keepsEnemiesIn()
                    .stopsProjectiles()
                    .onBlock(at -> PrinceOfAbolitionFx.distortShot(sl, player, at, null))
                    .onEnd(() -> {
                        PrinceOfAbolitionFx.sealEnd(sl, center, radius, tag);
                        PrinceOfAbolitionOrders.remove(player, "seal");
                        PrinceOfAbolitionFx.statusEnd(player, Status.SEAL);
                    })
                    .raise(player, sl, SEAL_DOME);
            PrinceOfAbolitionFx.status(player, Status.SEAL, stands);
            if (isPrince(player, ANY_SEQUENCE)) PrinceOfAbolitionOrders.add(player, "seal", until, null);
        });
    }

    // =========================
    // Ability 5 - Disorder (the casts were swapped in play: a normal press is Paces, sneak is Disorder)
    // Sneak:  chaos in the enemy's order. The enemy you look at (DISORDER_RANGE) has its perception and the order of its
    //         actions scrambled for DISORDER_TICKS: DISORDER_MISS of its attacks miss, a mob loses its target (and often
    //         cannot pick one), a player's view swims. If you are held (rooted, frozen, asleep, afraid) the cast frees
    //         you instead: every hold on you ends and none takes for UNBOUND_TICKS.
    // Normal: Paces. Disorder the distance: you cross up to PACES_RANGE (PACES_EARL_RANGE from Earl of the Fallen) to
    //         where you aim in a few strides, and a fake you stays where you stood for DECOY_TICKS. Mobs that hunted you
    //         go for it; when an enemy reaches it, it bursts and Disorders everything within DECOY_BURST for
    //         DECOY_DISORDER. Earl of the Fallen (4): after either cast, everything aimed at you misses for UNTOUCHABLE_TICKS.
    // =========================
    public static void princeOfAbolitionDisorder(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!ready(player, bypassClassCheck, DISORDER_SEQUENCE)) return;

        if (!player.isShiftKeyDown()) {
            if (Root.has(player, RootRestriction.TELEPORT) || Root.has(player, RootRestriction.MOVE)) {
                player.sendSystemMessage(Component.literal("You are held fast; distance will not bend for you."));
                return; // nothing spent
            }
            double range = range(player, Beyonder.hasSequence(player, PACES_EARL_SEQUENCE) ? PACES_EARL_RANGE : PACES_RANGE);
            Vec3 to = Spells.blinkSpot(player, sl, range);
            if (to == null) {
                player.sendSystemMessage(Component.literal("There is no room there to step to."));
                return; // nothing spent
            }
            if (!Spells.paySpirituality(player, PACES_COST)) return;
            paces(player, sl, to);
            return;
        }
        if (isHeld(player)) {
            if (!Spells.paySpirituality(player, DISORDER_COST)) return;
            unbind(player, sl);
            return;
        }
        LivingEntity target = aim(player, sl, DISORDER_RANGE);
        if (target == null) {
            player.sendSystemMessage(Component.literal("There is no order before you to undo."));
            return; // nothing spent
        }
        if (!Spells.paySpirituality(player, DISORDER_COST)) return;
        disorder(player, sl, target, duration(player, DISORDER_TICKS));
        untouchable(player, sl);
    }

    /** Disorders one enemy for 'ticks' (Disorder, and the fake self's burst). */
    static void disorder(LivingEntity player, ServerLevel sl, LivingEntity target, int ticks) {
        int lasts = PrinceOfAbolitionMarks.put(player, target, Kind.DISORDERED, ticks);
        if (lasts <= 0) return;
        PrinceOfAbolitionFx.disorder(sl, player, target, lasts, PrinceOfAbolitionMarks.tag(target, Kind.DISORDERED));
        if (target instanceof Mob mob) mob.setTarget(null);
        if (target instanceof Player victim) Spells.keepEffect(victim, MobEffects.NAUSEA, 0, lasts);
    }

    static boolean isHeld(LivingEntity player) {
        return Root.isRooted(player) || Freeze.isFrozen(player) || Slumber.isAsleep(player) || Fear.isAfraid(player);
    }

    static void unbind(LivingEntity player, ServerLevel sl) {
        player.getPersistentData().putLong(UNBOUND_UNTIL, sl.getGameTime() + UNBOUND_TICKS);
        shakeHolds(player);
        PrinceOfAbolitionFx.unbound(sl, player);
        untouchable(player, sl);
    }

    /** Every hold on the player ends (Disorder freeing them, and for UNBOUND_TICKS after). */
    static void shakeHolds(LivingEntity player) {
        if (Root.isRooted(player)) Root.remove(player);
        if (Freeze.isFrozen(player)) Freeze.thaw(player);
        if (Slumber.isAsleep(player)) Slumber.wake(player);
        if (Fear.isAfraid(player)) Fear.end(player);
    }

    static void paces(LivingEntity player, ServerLevel sl, Vec3 to) {
        Vec3 from = player.position();
        int ticks = duration(player, DECOY_TICKS);
        String tag = "poa_paces_" + player.getId() + "_" + sl.getGameTime();
        PrinceOfAbolitionFx.paces(sl, player, from, to, ticks, tag);
        PrinceOfAbolitionFx.status(player, Status.PACES, ticks);
        // the ones that hunted you go for the fake you
        for (LivingEntity enemy : Spells.enemiesAround(player, sl, from, DECOY_LURE)) {
            if (enemy instanceof Mob mob && mob.getTarget() == player) {
                mob.setTarget(null);
                mob.getNavigation().moveTo(from.x, from.y, from.z, 1.2);
            }
        }
        Unnoticed.hide(player, Math.min(ticks, 40));                    // for a moment no mind finds the real one
        player.teleportTo(to.x, to.y, to.z);
        player.fallDistance = 0;
        FallGuard.protect(player, 60);
        decoyWatch(player, sl, from, sl.getGameTime() + ticks, tag);
        untouchable(player, sl);
    }

    /** Four times a second: an enemy that reaches the fake self bursts it. */
    private static void decoyWatch(LivingEntity player, ServerLevel sl, Vec3 at, long until, String tag) {
        if (!Spells.casterStillHere(player, sl) || sl.getGameTime() >= until) {
            if (Spells.casterStillHere(player, sl)) PrinceOfAbolitionFx.statusEnd(player, Status.PACES);
            return;
        }
        if (!Spells.enemiesAround(player, sl, at.add(0, 1, 0), DECOY_REACH).isEmpty()) {
            PrinceOfAbolitionFx.decoyBurst(sl, at, tag);
            PrinceOfAbolitionFx.statusEnd(player, Status.PACES);
            for (LivingEntity enemy : Spells.enemiesAround(player, sl, at.add(0, 1, 0), area(player, DECOY_BURST))) {
                disorder(player, sl, enemy, duration(player, DECOY_DISORDER));
            }
            return;
        }
        Later.run(sl, 5, () -> decoyWatch(player, sl, at, until, tag));
    }

    /** Earl of the Fallen: for UNTOUCHABLE_TICKS everything aimed at the player misses. */
    static void untouchable(LivingEntity player, ServerLevel sl) {
        if (!Beyonder.hasSequence(player, UNTOUCHABLE_SEQUENCE) || !isPrince(player, ANY_SEQUENCE)) return;
        player.getPersistentData().putLong(UNTOUCHABLE_UNTIL, sl.getGameTime() + UNTOUCHABLE_TICKS);
        PrinceOfAbolitionFx.untouchable(sl, player);
    }

    // =========================
    // Passives (the plain potion effects are in SequencePassives)
    // =========================

    /** Once a second: two more hearts from Barbarian on, a footing nothing moves while Iron Will or Coronation lasts. */
    private static void bodyTick(LivingEntity player) {
        Physique.keep(player, Attributes.MAX_HEALTH, HEARTS_ID, 4.0, AttributeModifier.Operation.ADD_VALUE,
                isPrince(player, PHYSICAL_SEQUENCE));
        Physique.keep(player, Attributes.KNOCKBACK_RESISTANCE, IRON_FOOTING_ID, 1.0, AttributeModifier.Operation.ADD_VALUE,
                shrugsMind(player) || PrinceOfAbolitionSovereign.isCrowned(player));
    }

    /** Weakness Detection, twice a second: a gold keyhole over the enemy you look at, for your eyes only. */
    private static void weaknessDetection(LivingEntity player, ServerLevel sl) {
        if (!(player instanceof ServerPlayer viewer)) return;
        LivingEntity target = Spells.aimEnemy(player, sl, WEAKNESS_SIGHT);
        if (target != null) PrinceOfAbolitionFx.weaknessDetection(viewer, target);
    }

    /** Presence, once a second (SequencePassives): weak hostile mobs near you hesitate. */
    public static void presence(LivingEntity player) {
        if (!(player.level() instanceof ServerLevel sl)) return;
        boolean any = false;
        for (LivingEntity enemy : Spells.enemiesAround(player, sl, player.position(), PRESENCE_RADIUS)) {
            if (!weakHostile(enemy)) continue;
            Spells.keepEffect(enemy, MobEffects.SLOWNESS, 0, 40);
            any = true;
        }
        if (any && player.tickCount % 80 == 0) PrinceOfAbolitionFx.presence(sl, player, PRESENCE_RADIUS);
    }

    private static boolean weakHostile(LivingEntity entity) {
        return entity instanceof Enemy && entity.getMaxHealth() <= PRESENCE_MAX_HEALTH;
    }

    /**
     * Favorable Ground (Earl of the Fallen), once a second: enemies within FAVORABLE_RADIUS cannot stay hidden near
     * you (Concealment and Unnoticed end; the Exiled and those rising from a mausoleum are left alone), and the
     * invisible ones glow.
     */
    private static void favorableGround(LivingEntity player, ServerLevel sl) {
        Vec3 at = player.position();
        AABB box = player.getBoundingBox().inflate(FAVORABLE_RADIUS);
        for (LivingEntity enemy : sl.getEntitiesOfClass(LivingEntity.class, box, e -> e != player && e.isAlive())) {
            if (enemy.position().distanceTo(at) > FAVORABLE_RADIUS || Spells.isFriend(player, enemy)) continue;   // isFriend: hidden ones count
            if (Exile.isExiled(enemy) || (enemy instanceof Player other && PrinceOfAbolitionMausoleums.isReborn(other))) continue;
            boolean found = false;
            if (Concealment.isHidden(enemy)) {
                Concealment.reveal(enemy);
                found = true;
            }
            if (Unnoticed.isUnnoticed(enemy)) {
                Unnoticed.end(enemy);
                found = true;
            }
            if (enemy.hasEffect(MobEffects.INVISIBILITY)) {
                Spells.keepEffect(enemy, MobEffects.GLOWING, 0, 40);
                found = true;
            }
            if (found && player instanceof ServerPlayer viewer) PrinceOfAbolitionFx.weaknessDetection(viewer, enemy);
        }
    }

    /**
     * Loose Order (Duke of Entropy), every other tick: a projectile of an enemy (or of nobody) within LOOSE_RADIUS that
     * is coming at you drifts up to LOOSE_DEGREES off, once.
     */
    private static void looseOrder(LivingEntity player, ServerLevel sl) {
        Vec3 body = player.position().add(0, player.getBbHeight() * 0.5, 0);
        for (Projectile shot : sl.getEntitiesOfClass(Projectile.class, player.getBoundingBox().inflate(LOOSE_RADIUS), Entity::isAlive)) {
            if (shot.getPersistentData().getBooleanOr(DRIFTED, false)) continue;
            Entity owner = shot.getOwner();
            if (owner == player || (owner instanceof LivingEntity living && !Spells.isEnemy(player, living))) continue;
            Vec3 velocity = shot.getDeltaMovement();
            Vec3 toMe = body.subtract(shot.position());
            if (velocity.lengthSqr() < 1.0E-3 || velocity.normalize().dot(toMe.normalize()) < 0.9) continue;   // not at me
            double off = (shot.getRandom().nextDouble() * 2 - 1) * LOOSE_DEGREES;
            double rise = (shot.getRandom().nextDouble() * 2 - 1) * Math.tan(Math.toRadians(LOOSE_DEGREES)) * velocity.length();
            shot.setDeltaMovement(Spells.turned(velocity, off).add(0, rise, 0));
            shot.hurtMarked = true;
            shot.getPersistentData().putBoolean(DRIFTED, true);
            PrinceOfAbolitionFx.looseOrder(sl, shot.position());
        }
    }

    // =========================
    // Sequence Might and the mausoleums
    // =========================

    /** How many Sequences of Might the player has: 1 at Lawyer (9) ... 10 at Black Emperor (0); 0 off this Pathway. */
    static int mightSteps(LivingEntity player) {
        if (!Beyonders.pathwayOf(player).equals(PATHWAY)) return 0;                // a player, or a Beyonder mob of it
        return Math.max(0, Math.min(10, 10 - Beyonders.sequenceOf(player)));
    }

    /** Damage dealt times this: Sequence Might and the standing mausoleums. */
    public static float attack(LivingEntity player) {
        return 1f + MIGHT_PER_SEQUENCE * mightSteps(player) + PrinceOfAbolitionMausoleums.might(player);
    }

    /** Damage taken divided by this: Sequence Might and the standing mausoleums. */
    public static float defense(LivingEntity player) {
        return 1f + MIGHT_PER_SEQUENCE * mightSteps(player) + PrinceOfAbolitionMausoleums.might(player);
    }

    /** A distance this Beyonder aims or reaches, with Sequence Might. */
    public static double range(LivingEntity player, double blocks) {
        return blocks * (1.0 + MIGHT_PER_SEQUENCE * mightSteps(player)) * magnified(player);
    }

    /** A radius, width or height of something this Beyonder makes, with Sequence Might. */
    public static double area(LivingEntity player, double blocks) {
        return blocks * (1.0 + MIGHT_PER_SEQUENCE * mightSteps(player)) * magnified(player);
    }

    /** Magnify waiting for its blow: everything reaches PrinceOfAbolitionFallen.MAGNIFY_REACH times as far and wide. */
    private static double magnified(LivingEntity player) {
        return PrinceOfAbolitionFallen.isMagnified(player) ? PrinceOfAbolitionFallen.MAGNIFY_REACH : 1.0;
    }

    /** How long something this Beyonder puts on others or leaves standing lasts: Sequence Might and Their Order. */
    public static int duration(LivingEntity player, int ticks) {
        double longer = 1.0 + MIGHT_PER_SEQUENCE * mightSteps(player);
        if (isPrince(player, THEIR_ORDER_SEQUENCE)) longer *= THEIR_ORDER_LONGER;
        return (int) Math.round(ticks * longer);
    }

    /**
     * The enemy under the crosshair within range(baseRange); failing that, one this player has Connected (a Bribe)
     * up to CONNECT_REACH times as far.
     */
    static LivingEntity aim(LivingEntity player, ServerLevel sl, double baseRange) {
        double range = range(player, baseRange);
        LivingEntity target = Spells.aimEnemy(player, sl, range);
        if (target != null) return target;
        LivingEntity far = Spells.aimEnemy(player, sl, range * CONNECT_REACH);
        return far != null && PrinceOfAbolitionMarks.from(far, Kind.CONNECTED, player) ? far : null;
    }

    // =========================
    // The wheels' recast rules (docs/PRINCE_OF_ABOLITION.md, "How it is laid out")
    // =========================
    static final int RECAST_TWICE_SEQUENCE = 6;         // Baron of Corruption: a recast costs 2x (3x before)
    static final int RECAST_ONCE_SEQUENCE = 5;          // Mentor of Disorder: 1x
    static final int NO_COOLDOWN_SEQUENCE = 4;          // Earl of the Fallen: no cooldowns at all

    /** What a normal press (the last pick again, no cooldown) costs, times the entry's own cost. */
    static double recastCost(LivingEntity player) {
        if (Beyonder.hasSequence(player, RECAST_ONCE_SEQUENCE)) return 1.0;
        return Beyonder.hasSequence(player, RECAST_TWICE_SEQUENCE) ? 2.0 : 3.0;
    }

    /** From Earl of the Fallen no wheel entry has a cooldown. */
    static boolean noCooldowns(LivingEntity player) {
        return Beyonder.hasSequence(player, NO_COOLDOWN_SEQUENCE);
    }

    // =========================
    // Helpers shared by the abilities
    // =========================

    /** True for this Pathway at this Sequence or stronger. */
    public static boolean isPrince(Player player, int sequence) {
        return SoulCore.getPathway(player).equals(PATHWAY) && Beyonder.hasSequence(player, sequence);
    }

    /** The same for anyone: a player as above, a Beyonder mob of this Pathway by its own Sequence. */
    public static boolean isPrince(LivingEntity entity, int sequence) {
        if (entity instanceof Player player) return isPrince(player, sequence);
        return PATHWAY.equals(Beyonders.pathwayOf(entity)) && Beyonder.hasSequence(entity, sequence);
    }

    /** Has this pathway (or the check is skipped) and has reached the Sequence the ability needs. */
    static boolean ready(Player player, boolean bypassClassCheck, int sequence) {
        if (!bypassClassCheck && !SoulCore.getPathway(player).equals(PATHWAY)) return false;
        return Beyonder.hasSequence(player, sequence);
    }

    /** The tag the rot's picture on an entity is shown with (status/Corrosion's look, cleared when it ends). */
    static String rotTag(Entity entity) {
        return "poa_rot_" + entity.getId();
    }

    /** True while the game time saved under 'key' is still ahead. */
    static boolean isActive(Entity entity, String key) {
        return entity.getPersistentData().getLongOr(key, 0L) > entity.level().getGameTime();
    }

    /** The Pathway was taken away (or changed): the counter, the mausoleums and the body go. */
    private static void lostPathway(Player player, ServerLevel sl) {
        player.getPersistentData().remove(WAS_PRINCE);
        PrinceOfAbolitionMausoleums.crumbleAll(player, sl);
        PrinceOfAbolitionOrders.clear(player);
        bodyTick(player);
        for (Status status : Status.values()) PrinceOfAbolitionFx.statusEnd(player, status);
        if (Dome.isRaised(player, SEAL_DOME)) Dome.close(player, SEAL_DOME);
        if (Wall.isRaised(player, GATE_WALL)) Wall.close(player, GATE_WALL);
        PrinceOfAbolitionSovereign.endAll(player);
        PrinceOfAbolitionFallen.clearState(player);
    }

    /** After a relog or a death: nothing of the timed casts is left over (their looks did not survive either). */
    private static void clearState(Player player) {
        CompoundTag data = player.getPersistentData();
        for (String key : List.of(IRON_UNTIL, IRON_TAG, UNBOUND_UNTIL, UNTOUCHABLE_UNTIL)) data.remove(key);
        PrinceOfAbolitionFallen.clearState(player);
        PrinceOfAbolitionSovereign.clearState(player);
        PrinceOfAbolitionOrders.showAgain(player);
        if (!SoulCore.getPathway(player).equals(PATHWAY)) return;
        for (Status status : Status.values()) PrinceOfAbolitionFx.statusEnd(player, status);      // a clock left running before death
        if (PrinceOfAbolitionMausoleums.clearRebirth(player)) {                   // logged out while out of the world
            Root.remove(player);
            Concealment.reveal(player);
        }
    }
}
