package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.illuminator;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonder;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Physique;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.SoulCore;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.illuminator.IlluminatorFx.Status;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.illuminator.IlluminatorFx.Weakness;
import net.lukario.circleofinevitability.combat.Cooldowns;
import net.lukario.circleofinevitability.combat.History;
import net.lukario.circleofinevitability.combat.HudCounter;
import net.lukario.circleofinevitability.combat.Later;
import net.lukario.circleofinevitability.combat.MeleeCombo;
import net.lukario.circleofinevitability.combat.Shot;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.combat.Zone;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.DarkAngel;
import net.lukario.circleofinevitability.combat.Residue;
import net.lukario.circleofinevitability.status.AbilityLock;
import net.lukario.circleofinevitability.status.Appraisal;
import net.lukario.circleofinevitability.status.Concealment;
import net.lukario.circleofinevitability.status.Construct;
import net.lukario.circleofinevitability.status.Exposed;
import net.lukario.circleofinevitability.status.FallGuard;
import net.lukario.circleofinevitability.status.Marionette;
import net.lukario.circleofinevitability.status.Study;
import net.lukario.circleofinevitability.status.Substitute;
import net.lukario.circleofinevitability.status.Unnoticed;
import net.lukario.circleofinevitability.status.Vulnerable;
import net.lukario.circleofinevitability.status.WeaknessTable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.common.util.Result;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * Illuminator: the Paragon Pathway of Lord of the Mysteries (docs/ILLUMINATOR.md). Seven keys in the Attendant Of
 * Mysteries pattern, from Savant (Sequence 9) up to Paragon (Sequence 0); every key has a normal and a sneak cast:
 *
 *   1 Compound Flask  (Savant)            a flask of nitroglycerin in aqua regia: bursts, Corrodes   | sneak: Recall
 *   2 Survey          (Archaeologist)     a pulse that outlines every hostile thing and drags the hidden out
 *                                                                                                    | sneak: Ritual Circle
 *   3 Appraise        (Appraiser)         the enemy looked at is Appraised: its weakness, +15% from you | sneak: Exploit Flaw
 *   4 Workshop        (Artisan)           the wheel: Clockwork Rifle, Spirit Cannon, Mystic Brooch (IlluminatorWorkshop)
 *   5 Astronomy       (Astronomer)        the wheel: Star of Curses, Blessing of Stars, False Constellations, Meteor
 *                                         Shower (IlluminatorAstronomy)
 *   6 Alchemy and Arcana (Alchemist)      the wheel: Living Doll, Golem, Mechanical Body (4), Dissociation, Knowledge
 *                                         Spirit (3) (IlluminatorAlchemy)
 *   7 Civilization    (Knowledge Magister) the wheel: Rewrite Principles, Colossus (2), Scrolls of Civilization,
 *                                         Mathematical Maze, Masterwork (1), Reality Knowledge, Illusory Book,
 *                                         Mechanical Heaven (0) (IlluminatorCivilization)
 *
 * The Pathway's own mechanic is the mark of status/Appraisal (who appraised what, its weakness, the bonus on every
 * ability hit) with the weakness table WEAKNESS below. Every Illuminator hit goes through hit(...), which says what
 * kind it is, so the match bonus lands by itself. Every cast's cost goes through costFor / pay / afterCast (Quick
 * Study and the Rite live there; addCostRule / addAfterCast take more). What changes every Pathway's costs and cooldowns
 * (Eroded, Enlightenment, the Aura, the Kingdom) is registered in IlluminatorEvents with Spells.addCostScale and
 * Cooldowns.addScale.
 *
 * Every look and sound is in IlluminatorFx (see /illuminatorfx); this class only decides who is hurt, held, read and
 * helped, and when. Nothing here breaks blocks or lights real fire. The numbers for every ability are below.
 */
public class Illuminator {

    public static final String PATHWAY = "Illuminator";

    // =========================
    // Ability settings (cost = spirituality, ticks: 20 = 1 second, distances in blocks)
    // =========================

    // Ability 1 - Compound Flask (Sequence 9)
    private static final int FLASK_SEQUENCE = 9;
    private static final int FLASK_COST = 250;
    private static final int FLASK_COOLDOWN = 40;
    private static final double FLASK_RANGE = 24.0;
    private static final double FLASK_SPEED = 1.4;              // blocks a tick: a lob you can see coming
    private static final float FLASK_DAMAGE = 36f;              // to the one it hits...
    private static final float FLASK_SPLASH = 18f;              // ...and to everything else within...
    private static final double FLASK_RADIUS = 3.0;             // ...this
    private static final double FLASK_SHOVE = 0.5;
    private static final int CORRODED_TICKS = 160;              // Corroded: takes this much more damage...
    private static final float CORRODED_EXTRA = 0.15f;          // ...for this long (status/Vulnerable, not stacking)
    private static final int TWIN_FLASK_SEQUENCE = 6;           // Artisan: two flasks...
    private static final int TWIN_FLASK_DELAY = 4;              // ...this far apart
    private static final int ACID_SEQUENCE = 3;                 // Arcane Scholar: the burst leaves a puddle...
    private static final double ACID_RADIUS = 3.0;
    private static final int ACID_TICKS = 100;
    private static final float ACID_PER_SECOND = 4f;            // ...that bites whoever stands in it and Corrodes them
    // sneak: Recall
    private static final int RECALL_COST = 300;                 // Sequence 8 holds 400
    private static final int RECALL_COOLDOWN = 500;
    private static final int RECALL_TICKS = 60;                 // back 3 seconds...
    private static final int RECALL_FAR_SEQUENCE = 5;           // ...5 from Astronomer...
    private static final int RECALL_FAR_TICKS = 100;
    private static final int RECALL_ALLY_SEQUENCE = 1;          // ...8 from Illuminator, who can send an ally back instead
    private static final int RECALL_ALLY_TICKS = 160;
    private static final double RECALL_ALLY_RANGE = 24.0;
    private static final float RECALL_HEAL = 0.5f;              // half of what was lost since then comes back
    private static final int RECALL_LANDING = 40;               // no fall damage on arrival for this long

    // Ability 2 - Survey (Sequence 8)
    private static final int SURVEY_SEQUENCE = 8;
    private static final int SURVEY_COST = 350;                 // Sequence 8 holds 400 (the plan's 600 could not be paid)
    private static final int SURVEY_COOLDOWN = 300;
    private static final double SURVEY_REACH = 40.0;
    private static final int SURVEY_FAR_SEQUENCE = 5;           // Astronomer: further
    private static final double SURVEY_FAR_REACH = 64.0;
    private static final double MECH_EYES_REACH = 1.5;          // the Mechanical Body's eyes see this much further (key 6)
    private static final int SURVEY_TICKS = 160;                // the outlines last this long
    private static final int SURVEY_FORCED_OUT = 60;            // the hidden cannot hide again for this long
    private static final int RESIDUE_SEQUENCE = 3;              // Arcane Scholar (Environment Controller): where abilities...
    private static final int RESIDUE_AGE = 1200;                // ...were cast in the last minute show (combat/Residue)...
    // sneak: Ritual Circle
    private static final int RITUAL_COST = 400;
    private static final int RITUAL_COOLDOWN = 500;
    private static final double RITUAL_RADIUS = 6.0;
    private static final int RITUAL_WIDE_SEQUENCE = 3;          // Arcane Scholar: a wider circle
    private static final double RITUAL_WIDE_RADIUS = 10.0;
    private static final int RITUAL_TICKS = 400;
    private static final int RITUAL_CLEANSE_EVERY = 60;         // one harmful effect off each ally inside this often
    private static final float RITE_LONGER = 1.5f;              // the Rite: Workshop casts from inside last this much longer...
    private static final float RITE_CHEAPER = 0.8f;             // ...and cost this much
    private static final int RITE_ALCHEMY_SEQUENCE = 4;         // Alchemist: the Rite covers the Alchemy and Arcana wheel too

    // Ability 3 - Appraise (Sequence 7)
    private static final int APPRAISE_SEQUENCE = 7;
    private static final int APPRAISE_COST = 1250;
    private static final int APPRAISE_COOLDOWN = 60;
    private static final double APPRAISE_RANGE = 24.0;
    private static final int APPRAISED_TICKS = 240;             // the mark lasts 12 seconds...
    private static final float APPRAISED_BONUS = 0.15f;         // ...and your ability hits on it do this much more
    private static final int ANALYSIS_SEQUENCE = 2;             // Knowledge Magister: Analysis sees the essence...
    private static final float ANALYSIS_BONUS = 0.25f;          // ...a bigger bonus, and its hiding and substitutes go
    private static final int PARAGON_SEQUENCE = 0;              // Paragon: the mark lasts twice as long...
    private static final double PARAGON_EXPLOIT_RANGE = 64.0;   // ...and can be Exploited from anywhere you see it
    // sneak: Exploit Flaw
    private static final int EXPLOIT_COST = 1500;
    private static final int EXPLOIT_COOLDOWN = 200;
    private static final double EXPLOIT_RANGE = 24.0;
    private static final float EXPLOIT_DAMAGE = 90f;            // through armor (Spells.strikeTrue)
    private static final int EXPLOIT_STAGGER = 20;
    private static final int CONSTRUCT_STUN = 60;               // a construct is held this long...
    private static final float CONSTRUCT_FRAGILE = 150f;        // ...or taken apart if its maximum health is under this
    private static final int EXPLOIT_STRIP_SEQUENCE = 3;        // Arcane Scholar: its Substitute goes too
    private static final int EXPLOIT_ALL_SEQUENCE = 1;          // Illuminator: every Appraised enemy within...
    private static final double EXPLOIT_ALL_RANGE = 24.0;
    private static final float EXPLOIT_ALL_SHARE = 0.6f;        // ...for this share (the one you look at takes it all)

    // Costs and passives (docs/ILLUMINATOR.md, "Passives"); the once-a-second ones run from SequencePassives
    private static final int QUICK_STUDY_SEQUENCE = 9;          // Quick Study: the same ability again within...
    private static final int QUICK_STUDY_WINDOW = 400;
    private static final float QUICK_STUDY_COST = 0.9f;         // ...costs this much
    private static final int FIELD_SEQUENCE = 8;                // Field Physique: two more hearts...
    private static final double FIELD_HEARTS = 4.0;
    private static final float FIELD_CUT = 0.25f;               // ...and falls, drowning, cacti, walls hurt this much less
    private static final int EYE_SEQUENCE = 7;                  // Appraiser's Eye: the health of what you look at...
    private static final double EYE_RANGE = 24.0;               // ...this far, by the hotbar
    private static final int EYE_EVERY = 5;
    static final int CRAFTSMAN_SEQUENCE = 6;                    // Craftsman's Hands: the Workshop's cooldowns...
    static final float CRAFTSMAN_COOLDOWN = 0.85f;              // ...this much...
    static final float CRAFTSMAN_LONGER = 1.25f;                // ...and what it makes lasts this much longer
    private static final int STARLIGHT_SEQUENCE = 5;            // Starlight: at night under open sky...
    private static final float STARLIGHT_REGEN = 0.2f;          // ...spirituality returns this much faster, and night vision
    private static final double BOND_RADIUS = 16.0;             // Constructor's Bond (4): your constructs this close...
    private static final float BOND_HEAL = 0.01f;               // ...heal this share of their health a second, traced for you
    private static final double ABSORB_RADIUS = 6.0;            // Absorption (3): every enemy this close...
    private static final float ABSORB_DRAIN = 1f;               // ...loses this a second...
    private static final float ABSORB_MOST = 2f;                // ...and you heal half of it, this much a second at most
    private static final double ANALYSIS_RADIUS = 16.0;         // Analysis (2): the hidden this close are plain to you
    private static final int ANALYSIS_EXPOSED = 30;
    private static final int AURA_SEQUENCE = 1;                 // Enlightenment Aura (1): allies this close...
    private static final double AURA_RADIUS = 16.0;
    private static final float AURA_COST = 0.9f;                // ...spend this much...
    private static final float AURA_SHORTER = 0.5f;             // ...and confusion and blindness on them last half as long

    static final float BOSS_HEALTH = 150f;

    // =========================
    // Saved on the player (game times are "until" this tick)
    // =========================
    private static final String LAST_CAST = "illuminator_last_cast_";            // + ability key: Quick Study
    private static final String RITUAL_CAST = "illuminator_ritual_cast";          // which Ritual Circle is the current one
    private static final String ACID_TURN = "illuminator_acid_turn";
    /** Set by the Mechanical Body (key 6, the second half): game time until it is worn. */
    static final String MECH_UNTIL = "illuminator_mechanical_body_until";

    // Names
    static final String RITUAL = "illuminator_ritual_circle";                      // its Zone
    private static final String ACID = "illuminator_acid_";                       // + 0..2: up to three puddles
    private static final String EYE = "illuminator_eye";                           // Appraiser's Eye's counter
    private static final Identifier FIELD_HEARTS_ID = Physique.id("illuminator_field_hearts");

    // Cooldown keys (combat/Cooldowns)
    private static final String CD_FLASK = "illuminator_flask";
    private static final String CD_RECALL = "illuminator_recall";
    private static final String CD_SURVEY = "illuminator_survey";
    private static final String CD_RITUAL = "illuminator_ritual";
    private static final String CD_APPRAISE = "illuminator_appraise";
    private static final String CD_EXPLOIT = "illuminator_exploit";

    // =========================
    // The weakness table (status/WeaknessTable): read from the top, the first that fits wins
    // =========================
    static final WeaknessTable WEAKNESS = WeaknessTable.otherwise(kind(Weakness.PIERCE))
            .rule(kind(Weakness.PIERCE), one -> one instanceof Player)                          // players: pierce
            .rule(kind(Weakness.STARLIGHT), Illuminator::isSpiritual)                           // undead, spirits, summons
            .rule(kind(Weakness.CORROSION), one -> Construct.isConstruct(one) || one.getArmorValue() >= 8)
            .rule(kind(Weakness.GRAVITY), Illuminator::isFlightyOrQuick);                       // flying, fast, jumpy

    /** Who the Paragon Pathway sees as made of spirit: the undead, vexes and allays, and every summon. */
    private static boolean isSpiritual(LivingEntity one) {
        return one.is(EntityTypeTags.UNDEAD) || one.getType() == EntityTypes.VEX || one.getType() == EntityTypes.ALLAY
                || Marionette.isSummoned(one);
    }

    /** Flying, fast or jumpy: what hangs in the air, what is quicker than a player walks, what bounds. */
    private static boolean isFlightyOrQuick(LivingEntity one) {
        if (one.isNoGravity()) return true;
        if (one.getType() == EntityTypes.BAT || one.getType() == EntityTypes.BEE || one.getType() == EntityTypes.BLAZE
                || one.getType() == EntityTypes.GHAST || one.getType() == EntityTypes.PARROT || one.getType() == EntityTypes.BREEZE
                || one.getType() == EntityTypes.SLIME || one.getType() == EntityTypes.MAGMA_CUBE || one.getType() == EntityTypes.RABBIT
                || one.getType() == EntityTypes.FROG || one.getType() == EntityTypes.ENDERMAN) {
            return true;
        }
        return one.getAttributeValue(Attributes.MOVEMENT_SPEED) >= 0.3;
    }

    /** The word status/Appraisal keeps for a weakness, and back. */
    static String kind(Weakness weakness) {
        return weakness == null ? null : weakness.name().toLowerCase();
    }

    static Weakness weakness(String kind) {
        if (kind == null) return null;
        for (Weakness weakness : Weakness.values()) if (kind(weakness).equals(kind)) return weakness;
        return null;
    }

    /** What 'caster' knows 'target' to be weak to (Appraised by them), or null. */
    static Weakness appraisedWeakness(Player caster, LivingEntity target) {
        return weakness(Appraisal.weakness(caster, target));
    }

    // =========================
    // Costs: every key and every wheel entry goes through costFor
    // =========================

    /** Where a cast comes from, for the rules that only touch some of them (the Rite covers the Workshop). */
    public enum Group { KEY, WORKSHOP, ASTRONOMY, ALCHEMY, CIVILIZATION }

    /** A rule of the second half that changes a cost: (player, cost, ability key, group) -> new cost. */
    @FunctionalInterface
    public interface CostRule {
        int cost(Player player, int cost, String ability, Group group);
    }

    private static final List<CostRule> COST_RULES = new ArrayList<>();
    private static final List<BiConsumer<Player, String>> AFTER_CAST = new ArrayList<>();

    /** One more rule for what a cast costs (Enlightenment's -20%, Mechanical Heaven's -30%...). Once, in a static block. */
    public static void addCostRule(CostRule rule) {
        COST_RULES.add(rule);
    }

    /**
     * Runs after every cast that went off, with the ability's key ("compound_flask", a wheel entry's key...): Masterwork
     * stops when anything else is cast. Once, in a static block.
     */
    public static void addAfterCast(BiConsumer<Player, String> action) {
        AFTER_CAST.add(action);
    }

    /**
     * What a cast of 'cost' really costs this player now. Quick Study (9): the same ability again within 20 seconds,
     * 10% less. The Rite: a Workshop cast (from Alchemist also Alchemy and Arcana) from inside your Ritual Circle, 20%
     * less. Then every addCostRule. The one place for every rule that changes an Illuminator's costs.
     */
    public static int costFor(Player player, int cost, String ability, Group group) {
        if (cost <= 0 || Beyonder.isCastingUnbound(player) || !SoulCore.getPathway(player).equals(PATHWAY)) return cost;
        float due = cost;
        if (Beyonder.hasSequence(player, QUICK_STUDY_SEQUENCE)
                && player.level().getGameTime() - player.getPersistentData().getLongOr(LAST_CAST + ability, -100000L) <= QUICK_STUDY_WINDOW) {
            due *= QUICK_STUDY_COST;
        }
        if (inRite(player, group)) due *= RITE_CHEAPER;
        int result = Math.round(due);
        for (CostRule rule : COST_RULES) result = Math.max(0, rule.cost(player, result, ability, group));
        return result;
    }

    /** Pays for a key's cast (after costFor) and runs afterCast. False, with the usual message, if it cannot. */
    static boolean pay(Player player, int cost, String ability) {
        int due = costFor(player, cost, ability, Group.KEY);
        if (due > 0 && !Spells.paySpirituality(player, due)) return false;
        afterCast(player, ability);
        return true;
    }

    /** Every cast that went off (a key through pay, a wheel entry through .onCast): Quick Study remembers it. */
    public static void afterCast(Player player, String ability) {
        if (Beyonder.isCastingUnbound(player) || !SoulCore.getPathway(player).equals(PATHWAY)) return;
        player.getPersistentData().putLong(LAST_CAST + ability, player.level().getGameTime());
        for (BiConsumer<Player, String> action : AFTER_CAST) action.accept(player, ability);
    }

    /** True while the player stands in their own Ritual Circle and the Rite covers a cast of this group. */
    public static boolean inRite(Player player, Group group) {
        boolean covered = group == Group.WORKSHOP || (group == Group.ALCHEMY && Beyonder.hasSequence(player, RITE_ALCHEMY_SEQUENCE));
        return covered && Zone.covers(player, RITUAL, player.position());
    }

    /** How long something a cast of this group makes lasts: Craftsman's Hands (Workshop) and the Rite make it longer. */
    public static int lasting(Player player, int ticks, Group group) {
        float scale = 1f;
        if (group == Group.WORKSHOP && isIlluminator(player, CRAFTSMAN_SEQUENCE)) scale *= CRAFTSMAN_LONGER;
        if (inRite(player, group)) scale *= RITE_LONGER;
        return Math.round(ticks * scale);
    }

    // =========================
    // The melee combo (combat/MeleeCombos): three hits that repeat; IlluminatorFx.combo is the look of each
    // =========================
    public static final MeleeCombo MELEE_COMBO = MeleeCombo.forPathway(PATHWAY)
            .step((player, target, sl) -> IlluminatorFx.combo(sl, player, target, 0))
            .step((player, target, sl) -> IlluminatorFx.combo(sl, player, target, 1))
            .step((player, target, sl) -> IlluminatorFx.combo(sl, player, target, 2))
            .resetAfter(40)
            .minCharge(0.8f);

    /** Players whose own shortened effect is being put back on (the Enlightenment Aura must let that one through). */
    private static final Set<UUID> SHORTENING = new HashSet<>();

    /** Which (caster, target) pairs carry an Illuminator's lens, so the end of another Pathway's mark shows nothing. */
    private static final Set<String> LENSES = new HashSet<>();
    /** Who sees the Appraiser's Eye counter right now, and what it says (it is only sent again when that changes). */
    private static final Map<UUID, String> EYE_SHOWN = new HashMap<>();

    @Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class IlluminatorEvents {

        static {
            // the lens over an Appraised enemy goes when the mark does (spent, run out, wiped, its caster gone)
            Appraisal.addListener(new Appraisal.Listener() {
                @Override
                public void ended(UUID caster, LivingEntity target, Appraisal.End why) {
                    if (!LENSES.remove(lensKey(caster, target))) return;
                    if (target.level() instanceof ServerLevel sl) IlluminatorFx.appraisedEnd(sl, target, lensTag(caster, target));
                    if (target instanceof Player appraised && !Appraisal.isMarked(appraised)) IlluminatorFx.statusEnd(appraised, Status.APPRAISED);
                }
            });
            // a built construct's vanilla body stays hidden under its model, whatever exposes things
            Exposed.addException(Construct::isBuilt);
            // Reality Knowledge (0): nobody can read or lock what the Paragon knows
            Appraisal.addImmunity(one -> one instanceof Player paragon && isIlluminator(paragon, PARAGON_SEQUENCE));
            AbilityLock.addImmunity(one -> one instanceof Player paragon && isIlluminator(paragon, PARAGON_SEQUENCE));
            Study.addImmunity(one -> one instanceof Player paragon && isIlluminator(paragon, PARAGON_SEQUENCE));
            // what every Pathway's abilities cost, and how long they wait: Eroded, Enlightenment, the Aura, the Kingdom
            Spells.addCostScale(IlluminatorAlchemy::erodedCost);
            Spells.addCostScale(IlluminatorCivilization::enlightenedCost);
            Spells.addCostScale(Illuminator::auraCost);
            Spells.addCostScale(IlluminatorCivilization::heavenCost);
            Cooldowns.addScale(IlluminatorCivilization::heavenCooldown);
            // Masterwork stops when anything else is cast
            addAfterCast(IlluminatorCivilization::castSomething);
        }

        @SubscribeEvent
        public static void onIlluminatorTick(TickEvent.PlayerTickEvent.Post event) {
            Player player = event.player();
            if (!(player.level() instanceof ServerLevel sl)) return;
            if (player.tickCount % EYE_EVERY == 0) appraisersEye(player, sl);
            if (player.tickCount % 20 == 0) bodyTick(player);
            IlluminatorAlchemy.tick(player, sl);
            IlluminatorCivilization.tick(player, sl);
        }

        /** Field Physique (8): falls, drowning, cacti and being stuck in a wall hurt a quarter less. */
        @SubscribeEvent
        public static void onIlluminatorHurt(LivingHurtEvent event) {
            if (event.getEntity().level().isClientSide()) return;
            IlluminatorAlchemy.hurt(event);
            IlluminatorCivilization.hurt(event);
            if (!(event.getEntity() instanceof Player player)) return;
            if (!isIlluminator(player, FIELD_SEQUENCE) || !isEnvironment(event.getSource())) return;
            event.setAmount(event.getAmount() * (1f - FIELD_CUT));
        }

        /** Rewrite Principles: combustion makes no flame for the Magister's side inside it. */
        @SubscribeEvent
        public static boolean onIlluminatorAttack(LivingAttackEvent event) {
            return !event.getEntity().level().isClientSide() && IlluminatorCivilization.shelters(event.getEntity(), event.getSource());
        }

        /**
         * Enlightenment Aura (1): confusion and blindness put on an ally near an Illuminator last half as long. The effect
         * is turned away and put on again a tick later, shorter (SHORTENING lets that one through).
         */
        @SubscribeEvent
        public static void onIlluminatorEffect(MobEffectEvent.Applicable event) {
            if (!(event.getEntity() instanceof Player player) || !(player.level() instanceof ServerLevel sl)) return;
            MobEffectInstance effect = event.getEffectInstance();
            if (effect.getEffect() != MobEffects.NAUSEA && effect.getEffect() != MobEffects.BLINDNESS) return;
            if (effect.isInfiniteDuration() || effect.getDuration() < 20 || SHORTENING.contains(player.getUUID())) return;
            if (auraCost(player) >= 1f) return;                                   // no Illuminator's aura on them
            event.setResult(Result.DENY);
            Holder<MobEffect> kind = effect.getEffect();
            int ticks = Math.max(1, Math.round(effect.getDuration() * AURA_SHORTER));
            int amplifier = effect.getAmplifier();
            UUID id = player.getUUID();
            Later.run(sl, 1, () -> {
                if (!player.isAlive()) return;
                SHORTENING.add(id);
                try {
                    player.addEffect(new MobEffectInstance(kind, ticks, amplifier));
                } finally {
                    SHORTENING.remove(id);
                }
            });
        }

        // what was on the screen does not survive relogging or dying: start clean
        @SubscribeEvent
        public static void onIlluminatorLogin(PlayerEvent.PlayerLoggedInEvent event) {
            clearState(event.getEntity());
        }

        @SubscribeEvent
        public static void onIlluminatorRespawn(PlayerEvent.PlayerRespawnEvent event) {
            clearState(event.getEntity());
        }

        @SubscribeEvent
        public static void onIlluminatorLogout(PlayerEvent.PlayerLoggedOutEvent event) {
            EYE_SHOWN.remove(event.getEntity().getUUID());
            IlluminatorAstronomy.forget(event.getEntity());
            IlluminatorCivilization.forget(event.getEntity());
        }
    }

    // =========================
    // Ability 1 - Compound Flask
    // Normal: a flask flies where you aim (FLASK_RANGE, a Shot) and bursts on the first enemy or block it meets:
    //         FLASK_DAMAGE to the one it hits, FLASK_SPLASH to everything else within FLASK_RADIUS, a shove, and
    //         everything hit is Corroded (CORRODED_EXTRA more damage for CORRODED_TICKS). A corrosion hit.
    //         Artisan (6): two flasks, TWIN_FLASK_DELAY apart. Arcane Scholar (3): the burst leaves an acid puddle.
    // Sneak:  Recall. You step back to where you stood RECALL_TICKS ago (combat/History), with back half the health
    //         you lost since, and no fall damage on arrival. Not while held in place, nor onto a spot that is blocked
    //         now (nothing spent). Astronomer (5): RECALL_FAR_TICKS. Illuminator (1): RECALL_ALLY_TICKS, and the
    //         player ally you look at (RECALL_ALLY_RANGE) is sent back instead.
    // =========================
    public static void illuminatorCompoundFlask(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!ready(player, bypassClassCheck, FLASK_SEQUENCE)) return;

        if (player.isShiftKeyDown()) {
            recall(player, sl);
            return;
        }
        if (!Cooldowns.ready(player, CD_FLASK, "Compound Flask")) return;
        if (!pay(player, FLASK_COST, CD_FLASK)) return;
        Cooldowns.start(player, CD_FLASK, FLASK_COOLDOWN);
        throwFlask(player, sl);
        if (Beyonder.hasSequence(player, TWIN_FLASK_SEQUENCE)) {
            Later.run(sl, TWIN_FLASK_DELAY, () -> {
                if (Spells.casterStillHere(player, sl)) throwFlask(player, sl);
            });
        }
    }

    private static void throwFlask(Player player, ServerLevel sl) {
        Vec3 hand = Spells.clearStart(player, sl, player.getEyePosition().add(Spells.rightOf(player).scale(0.35)).add(0, -0.25, 0));
        Vec3 direction = Spells.aimFrom(player, sl, hand, FLASK_RANGE);
        boolean acid = Beyonder.hasSequence(player, ACID_SEQUENCE);
        List<LivingEntity> struck = new ArrayList<>();
        IlluminatorFx.flaskThrow(sl, player);
        int arrives = Shot.of(IlluminatorFx.FLASK_FLYING)
                .speed(FLASK_SPEED).range(FLASK_RANGE).width(0.35).tip(0.2)
                .onHit((target, at) -> {
                    struck.add(target);
                    hit(player, target, FLASK_DAMAGE, Weakness.CORROSION);
                })
                .onEnd((at, hitBlock) -> {
                    if (!Spells.casterStillHere(player, sl)) return;
                    flaskBurst(player, sl, at, struck, acid);
                })
                .fire(player, sl, hand, direction);
        for (int t = 1; t < arrives; t++) {                                     // a drop of acid behind it every tick
            Vec3 at = hand.add(direction.normalize().scale(FLASK_SPEED * t));
            Later.run(sl, t, () -> IlluminatorFx.flaskTrail(sl, at));
        }
    }

    /** The flask bursts at 'at': the splash, the shove, and everyone hit Corroded. */
    private static void flaskBurst(Player player, ServerLevel sl, Vec3 at, List<LivingEntity> struck, boolean acid) {
        IlluminatorFx.flaskBurst(sl, at, FLASK_RADIUS);
        Set<LivingEntity> corroded = new HashSet<>(struck);
        for (LivingEntity enemy : Spells.enemiesAround(player, sl, at, FLASK_RADIUS)) {
            if (!struck.contains(enemy)) hit(player, enemy, FLASK_SPLASH, Weakness.CORROSION);
            corroded.add(enemy);
        }
        for (LivingEntity enemy : corroded) {
            if (!enemy.isAlive()) continue;
            Spells.push(enemy, at, FLASK_SHOVE, 0.25);
            corrode(sl, enemy, CORRODED_TICKS);
        }
        if (acid) acidPuddle(player, sl, Spells.groundAt(player, sl, at.add(0, 0.5, 0)));
    }

    /** Corroded: CORRODED_EXTRA more damage from everything for 'ticks' (status/Vulnerable), and its look. */
    static void corrode(ServerLevel sl, LivingEntity target, int ticks) {
        Vulnerable.add(target, CORRODED_EXTRA, CORRODED_EXTRA, ticks);
        IlluminatorFx.corroded(sl, target, ticks, "il_corroded_" + target.getId());
        if (target instanceof Player corroded) IlluminatorFx.status(corroded, Status.CORRODED, ticks);
    }

    /** Arcane Scholar: a puddle of acid ACID_RADIUS wide for ACID_TICKS; whoever stands in it is bitten and Corroded. */
    private static void acidPuddle(Player player, ServerLevel sl, Vec3 ground) {
        CompoundTag data = player.getPersistentData();
        int turn = (data.getIntOr(ACID_TURN, 0) + 1) % 3;
        data.putInt(ACID_TURN, turn);
        String tag = "il_acid_" + player.getId() + "_" + turn;
        IlluminatorFx.acidPuddleEnd(sl, ground, tag);                       // the puddle this one replaces, if it was here
        IlluminatorFx.acidPuddle(sl, ground, ACID_RADIUS, ACID_TICKS, tag);
        Zone.at(ground, ACID_RADIUS)
                .height(2)
                .lasts(ACID_TICKS)
                .every(20)
                .onInside((enemy, inside) -> {
                    hit(player, enemy, ACID_PER_SECOND, Weakness.CORROSION);
                    if (enemy.isAlive()) corrode(sl, enemy, 40);
                })
                .onEnd(() -> IlluminatorFx.acidPuddleEnd(sl, ground, tag))
                .open(player, sl, ACID + turn);
    }

    private static void recall(Player player, ServerLevel sl) {
        int ticks = Beyonder.hasSequence(player, RECALL_ALLY_SEQUENCE) ? RECALL_ALLY_TICKS
                : Beyonder.hasSequence(player, RECALL_FAR_SEQUENCE) ? RECALL_FAR_TICKS : RECALL_TICKS;
        Player who = player;
        if (Beyonder.hasSequence(player, RECALL_ALLY_SEQUENCE) && Spells.aimAlly(player, sl, RECALL_ALLY_RANGE) instanceof Player ally) {
            who = ally;
        }
        String name = who == player ? "You" : who.getDisplayName().getString();
        if (Root.has(who, RootRestriction.MOVE) || Root.has(who, RootRestriction.TELEPORT)) {
            player.sendSystemMessage(Component.literal(name + (who == player ? " are" : " is") + " held fast and cannot step back."));
            return; // nothing spent
        }
        History.Moment then = History.asFarAs(who, ticks);
        if (then == null || then.position().distanceToSqr(who.position()) < 0.25) {
            player.sendSystemMessage(Component.literal("There is nothing to remember yet: " + (who == player ? "you have" : name + " has") + " not moved."));
            return; // nothing spent
        }
        if (!Spells.fits(who, sl, then.position())) {
            player.sendSystemMessage(Component.literal("The place you remember is blocked now."));
            return; // nothing spent
        }
        if (!Cooldowns.ready(player, CD_RECALL, "Recall")) return;
        if (!pay(player, RECALL_COST, CD_RECALL)) return;
        Cooldowns.start(player, CD_RECALL, RECALL_COOLDOWN);

        Vec3 from = who.position();
        float lost = Math.max(0f, then.health() - who.getHealth());
        who.teleportTo(then.position().x, then.position().y, then.position().z);
        who.fallDistance = 0;
        FallGuard.protect(who, RECALL_LANDING);
        if (lost > 0f) who.heal(lost * RECALL_HEAL);
        IlluminatorFx.recall(sl, who, from, then.position());
        History.forget(who);                                                    // a step back cannot be stepped back again
    }

    // =========================
    // Ability 2 - Survey
    // Normal: a pulse of blueprint light spreads SURVEY_REACH (SURVEY_FAR_REACH from Astronomer; further through the
    //         Mechanical Body's eyes). At IlluminatorFx.SURVEY_PULSE, every hostile thing in it is outlined through walls
    //         for SURVEY_TICKS (the glowing effect), the hidden are dragged out (status/Exposed, SURVEY_FORCED_OUT), and
    //         the strongest of them is named with its health. Nothing hostile near: the pulse still goes, and says so.
    // Sneak:  Ritual Circle. A chalk circle RITUAL_RADIUS wide (RITUAL_WIDE_RADIUS from Arcane Scholar) at your feet
    //         for RITUAL_TICKS (a Zone): allies inside have Regeneration I and lose a harmful effect every
    //         RITUAL_CLEANSE_EVERY, enemies inside are slowed. The Rite: Workshop casts from inside cost RITE_CHEAPER and
    //         last RITE_LONGER (from Alchemist the Alchemy and Arcana wheel too). One circle: casting again moves it.
    // =========================
    public static void illuminatorSurvey(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!ready(player, bypassClassCheck, SURVEY_SEQUENCE)) return;

        if (player.isShiftKeyDown()) {
            if (!Cooldowns.ready(player, CD_RITUAL, "Ritual Circle")) return;
            if (!pay(player, RITUAL_COST, CD_RITUAL)) return;
            Cooldowns.start(player, CD_RITUAL, RITUAL_COOLDOWN);
            ritualCircle(player, sl);
            return;
        }
        if (!Cooldowns.ready(player, CD_SURVEY, "Survey")) return;
        if (!pay(player, SURVEY_COST, CD_SURVEY)) return;
        Cooldowns.start(player, CD_SURVEY, SURVEY_COOLDOWN);
        survey(player, sl);
    }

    private static double surveyReach(Player player) {
        double reach = Beyonder.hasSequence(player, SURVEY_FAR_SEQUENCE) ? SURVEY_FAR_REACH : SURVEY_REACH;
        return wearsMechanicalBody(player) ? reach * MECH_EYES_REACH : reach;
    }

    private static void survey(Player player, ServerLevel sl) {
        double reach = surveyReach(player);
        IlluminatorFx.survey(sl, player, reach);
        IlluminatorFx.status(player, Status.SURVEY, SURVEY_TICKS);
        Later.run(sl, IlluminatorFx.SURVEY_PULSE, () -> {
            if (!Spells.casterStillHere(player, sl)) return;
            LivingEntity strongest = null;
            int found = 0;
            for (LivingEntity one : Spells.livingAround(player, sl, player.position(), reach)) {
                if (Spells.isFriend(player, one) || !isHostile(player, one)) continue;
                found++;
                boolean wasHidden = Concealment.isHidden(one) || Unnoticed.isUnnoticed(one) || one.hasEffect(MobEffects.INVISIBILITY);
                Exposed.apply(one, SURVEY_FORCED_OUT);
                if (wasHidden && !Concealment.isHidden(one)) IlluminatorFx.surveyForcedOut(sl, one);
                if (Concealment.isHidden(one)) continue;                        // an Exile, or what an exception keeps hidden
                one.addEffect(new MobEffectInstance(MobEffects.GLOWING, SURVEY_TICKS, 0, false, false));
                IlluminatorFx.surveyed(sl, one);
                if (strongest == null || one.getMaxHealth() > strongest.getMaxHealth()) strongest = one;
            }
            if (Beyonder.hasSequence(player, RESIDUE_SEQUENCE) && player instanceof ServerPlayer viewer) surveyResidue(viewer, sl, reach);
            if (strongest == null) {
                player.sendSystemMessage(Component.literal("Nothing hostile within " + Math.round(reach) + " blocks."));
                return;
            }
            player.sendSystemMessage(Component.literal(found + " hostile within " + Math.round(reach) + " blocks. The strongest: "
                    + strongest.getDisplayName().getString() + ", " + Math.round(strongest.getHealth()) + "/"
                    + Math.round(strongest.getMaxHealth()) + " health, " + Math.round(strongest.distanceTo(player)) + " blocks away."));
        });
    }

    /** Hostile to the Illuminator: a monster, an enemy player, or a mob that is hunting them. */
    private static boolean isHostile(Player player, LivingEntity one) {
        return one instanceof Player || one instanceof Enemy || (one instanceof Mob mob && mob.getTarget() == player);
    }

    private static void ritualCircle(Player player, ServerLevel sl) {
        double radius = Beyonder.hasSequence(player, RITUAL_WIDE_SEQUENCE) ? RITUAL_WIDE_RADIUS : RITUAL_RADIUS;
        Vec3 center = Spells.groundAt(player, sl, player.position().add(0, 0.5, 0));
        long cast = sl.getGameTime();
        player.getPersistentData().putLong(RITUAL_CAST, cast);
        Zone.close(player, RITUAL);                                              // the old circle goes first
        String tag = "il_ritual_" + player.getId() + "_" + cast;
        IlluminatorFx.ritualCircle(sl, player, center, radius, RITUAL_TICKS, tag);
        IlluminatorFx.status(player, Status.RITUAL, RITUAL_TICKS);
        Later.run(sl, IlluminatorFx.RITUAL_DRAWN, () -> {
            if (player.getPersistentData().getLongOr(RITUAL_CAST, 0L) != cast || !Spells.casterStillHere(player, sl)) {
                IlluminatorFx.ritualEnd(sl, center, radius, tag);                // drawn again meanwhile, or the caster is gone
                return;
            }
            int[] beat = {0};
            Zone.at(center, radius)
                    .height(4)
                    .lasts(RITUAL_TICKS - IlluminatorFx.RITUAL_DRAWN)
                    .every(20)
                    .affects(one -> true)
                    .onInside((one, inside) -> {
                        if (Spells.isEnemy(player, one)) Spells.keepEffect(one, MobEffects.SLOWNESS, 0, 40);
                        else ritualBlesses(sl, one, beat[0]);
                    })
                    .onPulse(ticksLeft -> {
                        if (Zone.covers(player, RITUAL, player.position())) ritualBlesses(sl, player, beat[0]);
                        if (beat[0] % (RITUAL_CLEANSE_EVERY / 20) == 0) IlluminatorFx.ritualPulse(sl, center, radius);
                        beat[0]++;
                    })
                    .onEnd(() -> {
                        IlluminatorFx.ritualEnd(sl, center, radius, tag);
                        if (player.getPersistentData().getLongOr(RITUAL_CAST, 0L) == cast) IlluminatorFx.statusEnd(player, Status.RITUAL);
                    })
                    .open(player, sl, RITUAL);
        });
    }

    /** An ally in the circle: Regeneration I, and every RITUAL_CLEANSE_EVERY one harmful effect off. */
    private static void ritualBlesses(ServerLevel sl, LivingEntity ally, int beat) {
        Spells.keepEffect(ally, MobEffects.REGENERATION, 0, 60);
        if (beat % (RITUAL_CLEANSE_EVERY / 20) != 0) return;
        List<MobEffectInstance> harmful = Spells.harmfulEffects(ally);
        if (harmful.isEmpty()) return;
        ally.removeEffect(harmful.get(0).getEffect());
        IlluminatorFx.ritualCleansed(sl, ally);
    }

    // =========================
    // Ability 3 - Appraise
    // Normal: the enemy you look at (APPRAISE_RANGE) is Appraised at IlluminatorFx.APPRAISE_LANDS, for APPRAISED_TICKS:
    //         its weakness is read (WEAKNESS), your ability hits on it do APPRAISED_BONUS more (and Appraisal.MATCH_BONUS
    //         more with the ability that matches its weakness), it cannot hide (status/Exposed), and its health,
    //         effects and weakness are told to you. Knowledge Magister (2): Analysis, ANALYSIS_BONUS, and a ready
    //         Substitute is stripped. Paragon (0): the mark lasts twice as long.
    // Sneak:  Exploit Flaw. Spends your mark on the Appraised enemy you look at (EXPLOIT_RANGE; PARAGON_EXPLOIT_RANGE
    //         from Paragon): at IlluminatorFx.FLAW_LANDS, EXPLOIT_DAMAGE that nothing softens and a stagger. A
    //         construct is held CONSTRUCT_STUN, or taken apart under CONSTRUCT_FRAGILE maximum health. Arcane Scholar
    //         (3): its Substitute goes too. Illuminator (1): every enemy you Appraised within EXPLOIT_ALL_RANGE at once,
    //         EXPLOIT_ALL_SHARE each (the one you look at, all of it). With no mark: nothing spent.
    // =========================
    public static void illuminatorAppraise(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!ready(player, bypassClassCheck, APPRAISE_SEQUENCE)) return;

        if (player.isShiftKeyDown()) {
            exploitFlaw(player, sl);
            return;
        }
        LivingEntity target = Spells.aimEnemy(player, sl, appraiseRange(player));
        if (target == null) {
            player.sendSystemMessage(Component.literal("There is nothing in sight to appraise."));
            return; // nothing spent
        }
        if (!Cooldowns.ready(player, CD_APPRAISE, "Appraise")) return;
        if (!pay(player, APPRAISE_COST, CD_APPRAISE)) return;
        Cooldowns.start(player, CD_APPRAISE, APPRAISE_COOLDOWN);
        appraise(player, sl, target);
    }

    private static double appraiseRange(Player player) {
        return wearsMechanicalBody(player) ? APPRAISE_RANGE * MECH_EYES_REACH : APPRAISE_RANGE;
    }

    /** How long this player's marks last. */
    static int appraisedTicks(Player player) {
        return Beyonder.hasSequence(player, PARAGON_SEQUENCE) ? APPRAISED_TICKS * 2 : APPRAISED_TICKS;
    }

    /** Appraises 'target' for this player (also for the second half: a Survey or Starship Barrage that appraises). */
    static void appraise(Player player, ServerLevel sl, LivingEntity target) {
        Weakness weakness = weakness(WEAKNESS.of(target));
        int ticks = appraisedTicks(player);
        String tag = lensTag(player.getUUID(), target);
        IlluminatorFx.appraise(sl, player, target, weakness, ticks, tag);
        Later.run(sl, IlluminatorFx.APPRAISE_LANDS, () -> {
            if (!Spells.casterStillHere(player, sl) || !target.isAlive() || target.level() != sl) return;
            int left = ticks - IlluminatorFx.APPRAISE_LANDS;
            boolean analysis = Beyonder.hasSequence(player, ANALYSIS_SEQUENCE);
            LENSES.add(lensKey(player.getUUID(), target));
            Appraisal.mark(player, target, kind(weakness), left, analysis ? ANALYSIS_BONUS : APPRAISED_BONUS);
            Exposed.apply(target, left);
            if (analysis && Substitute.isArmed(target)) Substitute.disarm(target);
            if (target instanceof Player appraised) IlluminatorFx.status(appraised, Status.APPRAISED, left);
            player.sendSystemMessage(read(target, weakness));
        });
    }

    /** What the Appraiser reads on it: health, the effects on it, its weakness. */
    private static Component read(LivingEntity one, Weakness weakness) {
        StringBuilder text = new StringBuilder();
        text.append(one.getDisplayName().getString()).append(": ")
                .append(Math.round(one.getHealth())).append("/").append(Math.round(one.getMaxHealth())).append(" health");
        List<String> effects = new ArrayList<>();
        for (MobEffectInstance effect : one.getActiveEffects()) {
            if (effect.getEffect() == MobEffects.GLOWING) continue;
            effects.add(effect.getEffect().value().getDisplayName().getString() + " " + (effect.getAmplifier() + 1));
        }
        if (!effects.isEmpty()) text.append(", under ").append(String.join(", ", effects));
        text.append(". Weak to ").append(weaknessName(weakness)).append(" (").append(matching(weakness)).append(").");
        return Component.literal(text.toString());
    }

    private static String weaknessName(Weakness weakness) {
        return weakness == null ? "nothing" : kind(weakness);
    }

    /** The abilities that match a weakness (docs/ILLUMINATOR.md, the weakness table). */
    private static String matching(Weakness weakness) {
        if (weakness == null) return "no ability";
        return switch (weakness) {
            case STARLIGHT -> "Star of Curses, Meteor Shower";
            case CORROSION -> "Compound Flask, Dissociation";
            case GRAVITY -> "Blessing of Stars, Rewrite Principles";
            case PIERCE -> "Clockwork Rifle, Spirit Cannon";
        };
    }

    private static void exploitFlaw(Player player, ServerLevel sl) {
        boolean paragon = Beyonder.hasSequence(player, PARAGON_SEQUENCE);
        LivingEntity aimed = Spells.aimEnemy(player, sl, paragon ? PARAGON_EXPLOIT_RANGE : EXPLOIT_RANGE);
        List<LivingEntity> targets = new ArrayList<>();
        if (aimed != null && Appraisal.isMarked(player, aimed)) targets.add(aimed);
        if (Beyonder.hasSequence(player, EXPLOIT_ALL_SEQUENCE)) {
            for (LivingEntity marked : Appraisal.markedBy(player)) {
                if (marked != aimed && marked.distanceTo(player) <= EXPLOIT_ALL_RANGE && Spells.isEnemy(player, marked)) targets.add(marked);
            }
        }
        if (targets.isEmpty()) {
            IlluminatorFx.noFlaw(sl, player);
            player.sendSystemMessage(Component.literal("You do not yet know its flaw."));
            return; // nothing spent
        }
        if (!Cooldowns.ready(player, CD_EXPLOIT, "Exploit Flaw")) return;
        if (!pay(player, EXPLOIT_COST, CD_EXPLOIT)) return;
        Cooldowns.start(player, CD_EXPLOIT, EXPLOIT_COOLDOWN);
        if (targets.size() > 1) IlluminatorFx.exploitAll(sl, player, targets);
        else IlluminatorFx.exploitFlaw(sl, player, targets.get(0));
        for (LivingEntity target : targets) {
            float share = target == aimed ? 1f : EXPLOIT_ALL_SHARE;
            Later.run(sl, IlluminatorFx.FLAW_LANDS, () -> {
                if (!Spells.casterStillHere(player, sl) || !target.isAlive() || target.level() != sl) return;
                flawGives(player, sl, target, share);
            });
        }
    }

    /** The flaw gives: through armor, a stagger, the mark spent; a construct held or taken apart. */
    private static void flawGives(Player player, ServerLevel sl, LivingEntity target, float share) {
        if (Beyonder.hasSequence(player, EXPLOIT_STRIP_SEQUENCE) && Substitute.isArmed(target)) Substitute.disarm(target);
        if (Construct.isConstruct(target)) {
            boolean apart = target.getMaxHealth() < CONSTRUCT_FRAGILE;
            IlluminatorFx.constructTakenApart(sl, target, apart);
            if (apart) {
                Appraisal.strikeTrue(player, target, target.getHealth() + 1000f, null);
                return;
            }
            Root.apply(target, CONSTRUCT_STUN, RootRestriction.EVERYTHING);
        }
        Appraisal.strikeTrue(player, target, EXPLOIT_DAMAGE * share, null);
        if (target.isAlive()) {
            Root.apply(target, EXPLOIT_STAGGER, RootRestriction.MOVE, RootRestriction.ATTACK);
            Appraisal.spend(player, target);
        }
    }

    // =========================
    // Passives (the once-a-second ones are lines in SequencePassives)
    // =========================

    /** Appraiser's Eye (7), every few ticks: the health of the enemy looked at, by the hotbar (combat/HudCounter). */
    private static void appraisersEye(Player player, ServerLevel sl) {
        boolean shown = EYE_SHOWN.containsKey(player.getUUID());
        LivingEntity looked = isIlluminator(player, EYE_SEQUENCE) ? Spells.aimEnemy(player, sl, EYE_RANGE) : null;
        if (looked == null) {
            if (shown) {
                HudCounter.hide(player, EYE);
                EYE_SHOWN.remove(player.getUUID());
            }
            return;
        }
        String name = looked.getDisplayName().getString();
        if (name.length() > 18) name = name.substring(0, 17) + ".";
        Weakness weak = appraisedWeakness(player, looked);
        int color = weak != null ? weak.color & 0xFFFFFF : 0xD9A441;
        int health = Math.round(looked.getHealth());
        int most = Math.round(looked.getMaxHealth());
        String says = name + "|" + health + "|" + most + "|" + color;
        if (says.equals(EYE_SHOWN.get(player.getUUID()))) return;
        HudCounter.show(player, EYE, "illuminator/appraised", name, health, most, color);
        EYE_SHOWN.put(player.getUUID(), says);
    }

    /** Astronomer (5), once a second: Starlight. At night under open sky, spirituality back faster and night vision. */
    public static void starlight(Player player) {
        if (!(player.level() instanceof ServerLevel sl)) return;
        if (!sl.isDarkOutside() || !sl.canSeeSky(player.blockPosition())) return;
        float extra = Beyonder.spiritualityRegenPerTick(player) * 20f * STARLIGHT_REGEN;
        SoulCore.setSpiritualityQuietly(player, SoulCore.getSpirituality(player) + extra);
        Spells.keepEffect(player, MobEffects.NIGHT_VISION, 0, 600);
        if (player.tickCount % 60 == 0) IlluminatorFx.starlight(sl, player);
    }

    /** Alchemist (4), once a second: Constructor's Bond. Your constructs near you heal, and you see them through walls. */
    public static void constructorsBond(Player player) {
        if (!(player instanceof ServerPlayer viewer)) return;
        for (Mob made : Construct.ofOwner(player, null)) {
            if (made.level() != player.level() || made.distanceTo(player) > BOND_RADIUS) continue;
            if (made.getHealth() < made.getMaxHealth()) made.heal(made.getMaxHealth() * BOND_HEAL);
            IlluminatorFx.constructorsBond(viewer, made);
        }
    }

    /** Arcane Scholar (3), once a second: Absorption. Every enemy near you loses a little, and half of it is yours. */
    public static void absorption(Player player) {
        if (!(player.level() instanceof ServerLevel sl)) return;
        float drawn = 0f;
        for (LivingEntity enemy : Spells.enemiesAround(player, sl, player.position(), ABSORB_RADIUS)) {
            if (!isHostile(player, enemy)) continue;                             // monsters, enemy players, what hunts you
            Spells.strike(player, enemy, ABSORB_DRAIN);
            drawn += ABSORB_DRAIN * 0.5f;
            IlluminatorFx.absorption(sl, player, enemy.getBoundingBox().getCenter());
        }
        if (drawn > 0f && player.getHealth() < player.getMaxHealth()) player.heal(Math.min(ABSORB_MOST, drawn));
    }

    /** Knowledge Magister (2), once a second: Analysis. The concealed, unnoticed and invisible near you are dragged into
     *  the open (status/Exposed), and they and those with a Substitute ready are marked for you alone. */
    public static void analysis(Player player) {
        if (!(player instanceof ServerPlayer viewer) || !(player.level() instanceof ServerLevel sl)) return;
        for (LivingEntity one : Spells.livingAround(player, sl, player.position(), ANALYSIS_RADIUS)) {
            if (Spells.isFriend(player, one)) continue;
            boolean hiding = Concealment.isHidden(one) || Unnoticed.isUnnoticed(one) || one.hasEffect(MobEffects.INVISIBILITY);
            if (hiding) Exposed.apply(one, ANALYSIS_EXPOSED);
            if (hiding || Substitute.isArmed(one)) IlluminatorFx.analysis(viewer, one);
        }
    }

    /** Illuminator (1), once a second: the Enlightenment Aura shows on the allies near you now and then. */
    public static void enlightenmentAura(Player player) {
        if (!(player.level() instanceof ServerLevel sl)) return;
        for (LivingEntity ally : Spells.alliesAround(player, sl, player.position(), AURA_RADIUS)) {
            if (sl.getRandom().nextInt(3) == 0) IlluminatorFx.enlightenmentAura(sl, ally);
        }
    }

    /**
     * The Enlightenment Aura's cost for any player (Spells.addCostScale): AURA_COST when an Illuminator of Sequence 1 or
     * stronger is within AURA_RADIUS and counts them as an ally (the Illuminator too), else 1.
     */
    static float auraCost(Player player) {
        if (!(player.level() instanceof ServerLevel sl)) return 1f;
        for (Player one : sl.players()) {
            if (one.distanceToSqr(player) > AURA_RADIUS * AURA_RADIUS || !isIlluminator(one, AURA_SEQUENCE)) continue;
            if (Spells.isFriend(one, player)) return AURA_COST;
        }
        return 1f;
    }

    /** Arcane Scholar's Survey: where abilities were cast in the last minute within reach, for the Survey's 8 seconds. */
    private static void surveyResidue(ServerPlayer viewer, ServerLevel sl, double reach) {
        for (int t = 0; t < SURVEY_TICKS; t += 20) {
            Later.run(sl, t, () -> {
                if (!Spells.casterStillHere(viewer, sl)) return;
                for (Residue.Cast cast : Residue.near(sl, viewer.position(), reach, RESIDUE_AGE)) {
                    if (cast.caster().equals(viewer.getUUID())) continue;
                    IlluminatorFx.surveyResidue(viewer, cast.at(), DarkAngel.pathwayColor(cast.pathway()));
                }
            });
        }
    }

    /** Once a second, for every player: Field Physique's hearts (8) while they are this Pathway at that Sequence. */
    private static void bodyTick(Player player) {
        Physique.keep(player, Attributes.MAX_HEALTH, FIELD_HEARTS_ID, FIELD_HEARTS, AttributeModifier.Operation.ADD_VALUE,
                isIlluminator(player, FIELD_SEQUENCE));
    }

    /** What the environment does: falling, drowning, cacti, being stuck in a wall or flying into one. */
    private static boolean isEnvironment(DamageSource source) {
        return source.is(DamageTypes.FALL) || source.is(DamageTypes.DROWN) || source.is(DamageTypes.CACTUS)
                || source.is(DamageTypes.IN_WALL) || source.is(DamageTypes.FLY_INTO_WALL) || source.is(DamageTypes.SWEET_BERRY_BUSH);
    }

    // =========================
    // Helpers shared by the abilities
    // =========================

    /**
     * An Illuminator ability hit of one kind: Spells.strike, with the Appraised bonus and, when 'kind' is what the
     * target is weak to, the match (status/Appraisal). kind null: an ability hit of no kind.
     */
    static void hit(Player player, LivingEntity target, float damage, Weakness kind) {
        Appraisal.strike(player, target, damage, kind(kind));
    }

    /** True while the Mechanical Body (key 6, the second half) is worn: it sets MECH_UNTIL. */
    static boolean wearsMechanicalBody(Player player) {
        return isActive(player, MECH_UNTIL);
    }

    /** True for this Pathway at this Sequence or stronger. */
    public static boolean isIlluminator(Player player, int sequence) {
        return SoulCore.getPathway(player).equals(PATHWAY) && Beyonder.hasSequence(player, sequence);
    }

    /** Has this pathway (or the check is skipped) and has reached the Sequence the ability needs. */
    static boolean ready(Player player, boolean bypassClassCheck, int sequence) {
        if (!bypassClassCheck && !SoulCore.getPathway(player).equals(PATHWAY)) return false;
        return Beyonder.hasSequence(player, sequence);
    }

    /** True while the game time saved under 'key' is still ahead. */
    static boolean isActive(Entity entity, String key) {
        return entity.getPersistentData().getLongOr(key, 0L) > entity.level().getGameTime();
    }

    /** True for a boss: the dragon, the wither, the warden, an elder guardian, or anything with more than BOSS_HEALTH. */
    static boolean isBoss(LivingEntity one) {
        return one.getType() == EntityTypes.ENDER_DRAGON || one.getType() == EntityTypes.WITHER
                || one.getType() == EntityTypes.WARDEN || one.getType() == EntityTypes.ELDER_GUARDIAN
                || one.getMaxHealth() > BOSS_HEALTH;
    }

    private static String lensKey(UUID caster, LivingEntity target) {
        return caster + "/" + target.getUUID();
    }

    private static String lensTag(UUID caster, LivingEntity target) {
        return "il_appraised_" + Integer.toHexString(caster.hashCode()) + "_" + target.getId();
    }

    /** After a relog or a death: nothing of the timed casts is left over. */
    private static void clearState(Player player) {
        EYE_SHOWN.remove(player.getUUID());
        HudCounter.hide(player, EYE);
        Zone.close(player, RITUAL);
        player.getPersistentData().remove(RITUAL_CAST);
        IlluminatorWorkshop.clearState(player);
        IlluminatorAstronomy.forget(player);
        IlluminatorAlchemy.clearState(player);
        IlluminatorCivilization.clearState(player);
        if (!SoulCore.getPathway(player).equals(PATHWAY)) return;
        for (Status status : Status.values()) IlluminatorFx.statusEnd(player, status);   // a clock left running before death
    }
}
