package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.knowledge_emperor;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonder;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Physique;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.SoulCore;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.PathwayAbilities;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.knowledge_emperor.KnowledgeEmperorFx.Element;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.knowledge_emperor.KnowledgeEmperorFx.Status;
import net.lukario.circleofinevitability.combat.Later;
import net.lukario.circleofinevitability.combat.MeleeCombo;
import net.lukario.circleofinevitability.combat.Shot;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.lukario.circleofinevitability.status.Concealment;
import net.lukario.circleofinevitability.status.EmpoweredHit;
import net.lukario.circleofinevitability.status.Fear;
import net.lukario.circleofinevitability.status.Marionette;
import net.lukario.circleofinevitability.status.Scrolls;
import net.lukario.circleofinevitability.status.Study;
import net.lukario.circleofinevitability.status.Substitute;
import net.lukario.circleofinevitability.status.Unnoticed;
import net.lukario.circleofinevitability.status.Vulnerable;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.Result;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.listener.Priority;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Knowledge Emperor: the Hermit Pathway of Lord of the Mysteries (docs/KNOWLEDGE_EMPEROR.md). Seven keys in the
 * Attendant Of Mysteries pattern, from Mystery Pryer (Sequence 9) up to Hermit (Sequence 0); every ability has a
 * normal and a sneak cast. Written so far (the first half of the plan):
 *
 *   1 Mystery Prying   (Mystery Pryer)       10 s: hidden things near glow, each enemy you look at is read (health,
 *                                            mood, effects; its weakness from 50% Study) and gains 25% Study
 *                                                                              | sneak: Dowsing
 *   2 Weak-Point Strike (Melee Scholar)      a step forward and a cut where it is weakest: through armor, a stagger
 *                                                                              | sneak: Sidestep
 *   3 Hex              (Warlock)             15 s: 20% more damage taken, Weakness, Slowness; it jumps when the
 *                                            hexed dies. Scrollable            | sneak: Spellbolts (Scrollable;
 *                                                                                from Sage: the Knowledge Orbs)
 *   4 Read Scroll      (Scrolls Professor)   the oldest scroll in the pouch goes off, free | sneak: Inscribe Scroll
 *   5 Stars            (Constellations Master) the Stars wheel: Starlight Cage, Stellar Self, Star Fall, Star Guard
 *                                            (KnowledgeEmperorStars)            | sneak: the last pick again
 *   6 Retellings and Prophecy (Mysticologist)  not written yet (docs/KNOWLEDGE_EMPEROR.md, "The second half")
 *   7 Knowledge        (Sage)                  not written yet
 *
 * The three systems the whole Pathway leans on are reusable parts of the kit: status/Study (what you know of an
 * enemy; every hit on it is stronger the more you know), status/Scrolls (spells kept on paper, read for free) and
 * combat/Orbit (the guarding stars). The hooks every later ability uses are here: costFor / pay / afterCast (what a
 * cast costs: the Whisper of the Hidden Sage makes the next one free; Law of Three and the Divine Kingdom go in the
 * same two methods), studyGain (the Study an ability adds), weaknessOf (the element a studied enemy fears).
 *
 * Every look and sound is in KnowledgeEmperorFx (see /knowledgefx); this class only decides who is hurt, held, read
 * and helped, and when. Nothing here breaks blocks or lights real fire. The numbers for every ability are below.
 */
public class KnowledgeEmperor {

    public static final String PATHWAY = "Knowledge Emperor";

    // =========================
    // Ability settings (cost = spirituality, ticks: 20 = 1 second, distances in blocks)
    // =========================

    // Ability 1 - Mystery Prying (Sequence 9)
    private static final int PRY_SEQUENCE = 9;
    private static final int PRY_COST = 250;
    private static final int PRY_TICKS = 200;                    // the eyes stay open this long
    private static final double PRY_REACH = 30.0;                // hidden things this close glow; enemies this far are read
    private static final int PRY_STUDY = 25;                     // each enemy read gains this much Study
    private static final int PRY_GLOW_TICKS = 40;                // the glow is put on again every second while it lasts
    private static final int INQUIRY_SEQUENCE = 5;               // Eyes of Secret Inquiry...
    private static final double INQUIRY_REACH = 60.0;            // ...see this far...
    private static final double INQUIRY_LIVING = 30.0;           // ...and everything alive this close glows
    private static final int SEE_THROUGH_SEQUENCE = 4;           // Mysticologist: concealment, substitutes, marionette strings
    private static final int CLAIRVOYANT_PRY_SEQUENCE = 3;       // Clairvoyant: Pathway, Sequence and abilities read...
    private static final double CLAIRVOYANT_REACH = 80.0;        // ...this far
    // sneak: Dowsing
    private static final int DOWSE_COST = 250;
    private static final double DOWSE_RANGE = 64.0;
    private static final int DOWSE_TICKS = 240;                  // the thread points the way this long

    // Ability 2 - Weak-Point Strike (Sequence 8)
    private static final int STRIKE_SEQUENCE = 8;
    private static final int STRIKE_COST = 350;                  // Sequence 8 holds 400
    private static final double STRIKE_STEP = 3.0;               // a step this long...
    private static final double STRIKE_REACH = 3.5;              // ...to an enemy this far beyond it
    private static final float STRIKE_DAMAGE = 30f;              // through armor (Spells.strikeTrue)
    private static final int STRIKE_STAGGER = 15;
    private static final int STRIKE_STUDY = 10;
    // sneak: Sidestep
    private static final int SIDESTEP_COST = 350;
    private static final int SIDESTEP_TICKS = 40;                // the next blow or projectile within this is slipped...
    private static final double SIDESTEP_DISTANCE = 4.0;         // ...by a step this long (to the attacker's side)
    private static final double SIDESTEP_TO_ATTACKER = 8.0;      // an attacker this close: you end up at its side
    private static final int SIDESTEP_WINDOW = 40;               // your next hit within this is a free Weak-Point Strike

    // Ability 3 - Hex (Sequence 7)
    static final int HEX_SEQUENCE = 7;
    static final int HEX_COST = 1250;
    private static final double HEX_RANGE = 24.0;
    private static final int HEX_TICKS = 300;
    private static final float HEX_VULNERABLE = 0.2f;            // 20% more damage from everything
    private static final double HEX_JUMP = 6.0;                  // the hexed died: it jumps to the nearest enemy this close
    private static final int HEX_STUDY = 20;
    // sneak: Spellbolts
    static final int BOLT_COST = 1250;
    private static final double BOLT_RANGE = 24.0;
    private static final int BOLT_COUNT = 3;
    private static final float BOLT_DAMAGE = 20f;
    private static final int FROST_TICKS = 60;                   // Frost: Slowness II this long
    private static final int LIGHTNING_STUN = 10;                // Lightning: stunned this long
    private static final int FIRE_TICKS = 80;                    // Fire: burning this long
    private static final int ORBS_SEQUENCE = 2;                  // Sage: the Knowledge Orbs instead
    private static final int ORB_COUNT = 5;
    private static final float ORB_DAMAGE = 20f;
    private static final int ORB_STUN = 10;
    private static final int ORB_HUSH = 20;                      // no abilities this long (spirit-draining powers shut off)

    // Ability 4 - Read Scroll / Inscribe Scroll (Sequence 6)
    private static final int SCROLLS_SEQUENCE = 6;
    private static final int POUCH = 3;                          // scrolls the pouch holds...
    private static final int FREE_SCROLL_EVERY = 500;            // ...and a free one (Freezing, Paralysis in turn) this often
    private static final int BIG_POUCH_SEQUENCE = 4;             // Mysticologist:
    private static final int BIG_POUCH = 5;
    private static final int BIG_POUCH_EVERY = 300;
    private static final float INSCRIBE_SURCHARGE = 1.25f;       // writing one costs the spell's cost plus 25%, now
    private static final double SCROLL_RANGE = 24.0;
    private static final int FREEZING_TICKS = 60;                // the Scroll of Freezing: Slowness V, no jumping
    private static final int FREEZING_SLOWNESS = 4;
    private static final int PARALYSIS_TICKS = 30;               // the Scroll of Paralysis: held completely
    private static final int SCROLL_STUDY = 10;

    // Study (status/Study): what is gained, and the eye over each enemy
    static final int ABILITY_STUDY = 10;                         // an ability hit, unless it says otherwise
    private static final int MELEE_STUDY = 5;                    // a melee hit (a full swing; any swing from Melee Scholar)
    private static final float MELEE_MIN_CHARGE = 0.8f;
    private static final int EYE_TICKS = 120;                    // the eye's life; it is shown again before it fades
    private static final int EYE_REFRESH = 100;

    // Passives (docs/KNOWLEDGE_EMPEROR.md, "Passives"); the once-a-second ones run from SequencePassives
    private static final int INTUITION_SEQUENCE = 9;             // Spiritual Intuition: a chime when hunted unseen...
    private static final int INTUITION_REST = 60;
    private static final double INTUITION_RADIUS = 6.0;          // ...and hidden things this close show faintly
    private static final int WHISPER_SEQUENCE = 9;               // Whisper of the Hidden Sage: about every 5 minutes...
    private static final int WHISPER_EVERY = 6000;
    private static final int WHISPER_SPREAD = 1200;              // ...give or take a minute: the next ability is free
    private static final int FIGHTING_SEQUENCE = 8;              // Fighting Knowledge: faster attacks, more reach
    private static final double FIGHTING_ATTACK_SPEED = 0.1;
    private static final double FIGHTING_REACH = 0.5;
    private static final int MIND_SEQUENCE = 7;                  // Warlock's Mind: harmful effects end this much sooner
    private static final float MIND_SHORTER = 0.25f;
    private static final int STARLIGHT_SEQUENCE = 5;             // Starlight: at night under open sky...
    private static final float STARLIGHT_REGEN = 0.25f;          // ...spirituality returns this much faster

    static final float BOSS_HEALTH = 150f;

    // =========================
    // Saved on the player (game times are "until" this tick)
    // =========================
    private static final String PRY_UNTIL = "knowledge_emperor_pry_until";
    private static final String PRY_REACH_NOW = "knowledge_emperor_pry_reach";
    private static final String SIDESTEP_UNTIL = "knowledge_emperor_sidestep_until";
    private static final String NEXT_SCROLL = "knowledge_emperor_next_scroll";
    private static final String SCROLL_TURN = "knowledge_emperor_scroll_turn";
    private static final String WHISPER_NEXT = "knowledge_emperor_whisper_next";
    private static final String WHISPER_WAITING = "knowledge_emperor_whisper_waiting";
    private static final String INTUITION_READY = "knowledge_emperor_intuition_ready";

    // Names
    private static final String SIDESTEP_STRIKE = "knowledge_emperor_sidestep_strike";
    static final String SCROLL_HEX = "knowledge_emperor_hex";
    static final String SCROLL_SPELLBOLTS = "knowledge_emperor_spellbolts";
    static final String SCROLL_FREEZING = "knowledge_emperor_freezing";
    static final String SCROLL_PARALYSIS = "knowledge_emperor_paralysis";

    private static final Identifier FIGHTING_SPEED_ID = Physique.id("knowledge_emperor_fighting_speed");
    private static final Identifier FIGHTING_REACH_ID = Physique.id("knowledge_emperor_fighting_reach");

    private static final Set<Item> METAL_ARMOR = Set.of(
            Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS,
            Items.CHAINMAIL_HELMET, Items.CHAINMAIL_CHESTPLATE, Items.CHAINMAIL_LEGGINGS, Items.CHAINMAIL_BOOTS,
            Items.GOLDEN_HELMET, Items.GOLDEN_CHESTPLATE, Items.GOLDEN_LEGGINGS, Items.GOLDEN_BOOTS,
            Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS);

    /** Who a Pry has read already, while the eyes are open. Not saved. */
    private static final Map<UUID, Set<UUID>> PRY_READ = new HashMap<>();

    /** Hexes: hexed -> whose, until when, under which tag. Not saved (the curse itself is Vulnerable and potion effects). */
    private record Hex(UUID caster, long until, String tag) {}
    private static final Map<UUID, Hex> HEXES = new HashMap<>();

    /** A real swing of a player, waiting for the damage it does in the same tick. Not saved. */
    private record Swing(UUID target, long tick, float charge) {}
    private static final Map<UUID, Swing> SWINGS = new HashMap<>();

    /** Players whose own shortened effect is being put back on (Warlock's Mind must let that one through). */
    private static final Set<UUID> SHORTENING = new HashSet<>();

    // =========================
    // The melee combo (combat/MeleeCombos): three hits that repeat; KnowledgeEmperorFx.combo is the look of each
    // =========================
    public static final MeleeCombo MELEE_COMBO = MeleeCombo.forPathway(PATHWAY)
            .step((player, target, sl) -> KnowledgeEmperorFx.combo(sl, player, target, 0))
            .step((player, target, sl) -> KnowledgeEmperorFx.combo(sl, player, target, 1))
            .step((player, target, sl) -> KnowledgeEmperorFx.combo(sl, player, target, 2))
            .resetAfter(40)
            .minCharge(MELEE_MIN_CHARGE);

    @Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class KnowledgeEmperorEvents {

        static {
            // the scrolls (registered before any pouch is read, so nothing saved in one is dropped as unknown)
            Scrolls.register(SCROLL_HEX, "Hex", HEX_COST, KnowledgeEmperor::hexSpell);
            Scrolls.register(SCROLL_SPELLBOLTS, "Spellbolts", BOLT_COST, KnowledgeEmperor::spellbolts);
            Scrolls.register(SCROLL_FREEZING, "Freezing", 0, KnowledgeEmperor::freezingSpell);
            Scrolls.register(SCROLL_PARALYSIS, "Paralysis", 0, KnowledgeEmperor::paralysisSpell);
            KnowledgeEmperorStars.registerScrolls();
            Scrolls.addListener(KnowledgeEmperor::showPouch);
            Study.addListener(new Study.Listener() {
                @Override
                public void changed(Player caster, LivingEntity target, int before, int after) {
                    showStudy(caster, target, before, after);
                }

                @Override
                public void forgotten(Player caster, LivingEntity target) {
                    if (caster instanceof ServerPlayer viewer && isKnowledgeEmperor(viewer, 9)) KnowledgeEmperorFx.studyEyeHide(viewer, target);
                }
            });
        }

        @SubscribeEvent
        public static void onKnowledgeEmperorTick(TickEvent.PlayerTickEvent.Post event) {
            Player player = event.player();
            if (!(player.level() instanceof ServerLevel sl)) return;
            if (isActive(player, PRY_UNTIL)) pryTick(player, sl);
            if (player.tickCount % EYE_REFRESH == 0) refreshEyes(player);
            if (player.tickCount % 20 == 0) {
                bodyTick(player);
                if (player.tickCount % 400 == 0 && !HEXES.isEmpty()) HEXES.values().removeIf(hex -> hex.until() < sl.getGameTime());
                if (isKnowledgeEmperor(player, SCROLLS_SEQUENCE)) freeScrollTick(player, sl);
            }
        }

        // LOWEST: whatever cancels the swing (a root, a concealed target) has had its say. A cancelled swing never gets here.
        @SubscribeEvent(priority = Priority.LOWEST)
        public static void onKnowledgeEmperorSwing(AttackEntityEvent event) {
            Player player = event.getEntity();
            if (!(player.level() instanceof ServerLevel sl) || !SoulCore.getPathway(player).equals(PATHWAY)) return;
            SWINGS.put(player.getUUID(), new Swing(event.getTarget().getUUID(), sl.getGameTime(), player.getAttackStrengthScale(0.5f)));
        }

        /** A real melee hit landed: Study grows (Fighting Knowledge). */
        @SubscribeEvent
        public static void onKnowledgeEmperorHurt(LivingHurtEvent event) {
            LivingEntity target = event.getEntity();
            if (!(target.level() instanceof ServerLevel sl)) return;
            if (!(event.getSource().getEntity() instanceof Player player) || event.getSource().getDirectEntity() != player) return;
            Swing swing = SWINGS.remove(player.getUUID());
            if (swing == null || !swing.target().equals(target.getUUID()) || swing.tick() != sl.getGameTime()) return;
            if (!Spells.isEnemy(player, target)) return;
            if (swing.charge() >= MELEE_MIN_CHARGE || Beyonder.hasSequence(player, FIGHTING_SEQUENCE)) {
                // a tick later, so the hit itself is still dealt with what was known before it
                Later.run(sl, 1, () -> {
                    if (target.isAlive()) Study.add(player, target, MELEE_STUDY);
                });
            }
        }

        // LOW: after armor and the rest, what really got through
        @SubscribeEvent(priority = Priority.LOW)
        public static void onKnowledgeEmperorDamage(LivingDamageEvent event) {
            if (event.getEntity().level() instanceof ServerLevel sl) KnowledgeEmperorStars.damaged(event.getEntity(), sl, event.getAmount());
        }

        /** A hexed one died: the hex jumps to the nearest enemy of its caster, for the time it had left. */
        @SubscribeEvent
        public static void onKnowledgeEmperorDeath(LivingDeathEvent event) {
            LivingEntity dead = event.getEntity();
            if (!(dead.level() instanceof ServerLevel sl)) return;
            Hex hex = HEXES.remove(dead.getUUID());
            if (hex != null) hexJump(sl, dead, hex);
            KnowledgeEmperorStars.died(dead, sl);
        }

        /** Spiritual Intuition: a mob starts hunting the Hermit from where they cannot see it. */
        @SubscribeEvent
        public static void onKnowledgeEmperorTarget(LivingChangeTargetEvent event) {
            if (!(event.getNewTarget() instanceof ServerPlayer hunted) || event.getEntity().level().isClientSide()) return;
            if (!isKnowledgeEmperor(hunted, INTUITION_SEQUENCE)) return;
            LivingEntity mob = event.getEntity();
            long now = hunted.level().getGameTime();
            if (hunted.getPersistentData().getLongOr(INTUITION_READY, 0L) > now) return;
            Vec3 toMob = mob.position().subtract(hunted.position());
            boolean behind = toMob.horizontalDistanceSqr() > 0.01 && Spells.flatLook(hunted).dot(new Vec3(toMob.x, 0, toMob.z).normalize()) < 0.2;
            if (!behind && hunted.hasLineOfSight(mob)) return;                       // seen coming: no warning needed
            hunted.getPersistentData().putLong(INTUITION_READY, now + INTUITION_REST);
            KnowledgeEmperorFx.intuition(hunted);
        }

        /**
         * Warlock's Mind: a harmful effect put on a Warlock lasts MIND_SHORTER less. The effect is turned away and put
         * on again a tick later, shorter (SHORTENING lets that one through).
         */
        @SubscribeEvent
        public static void onKnowledgeEmperorEffect(MobEffectEvent.Applicable event) {
            if (!(event.getEntity() instanceof Player player) || !(player.level() instanceof ServerLevel sl)) return;
            MobEffectInstance effect = event.getEffectInstance();
            if (effect.getEffect().value().isBeneficial() || effect.isInfiniteDuration() || effect.getDuration() < 20) return;
            if (SHORTENING.contains(player.getUUID()) || !isKnowledgeEmperor(player, MIND_SEQUENCE)) return;
            event.setResult(Result.DENY);
            Holder<MobEffect> kind = effect.getEffect();
            int ticks = Math.max(1, Math.round(effect.getDuration() * (1f - MIND_SHORTER)));
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

        // what was on the screen does not survive relogging or dying: start clean, and show what is kept
        @SubscribeEvent
        public static void onKnowledgeEmperorLogin(PlayerEvent.PlayerLoggedInEvent event) {
            clearState(event.getEntity());
        }

        @SubscribeEvent
        public static void onKnowledgeEmperorRespawn(PlayerEvent.PlayerRespawnEvent event) {
            clearState(event.getEntity());
        }

        @SubscribeEvent
        public static void onKnowledgeEmperorLogout(PlayerEvent.PlayerLoggedOutEvent event) {
            Player player = event.getEntity();
            SWINGS.remove(player.getUUID());
            PRY_READ.remove(player.getUUID());
            SHORTENING.remove(player.getUUID());
            HEXES.values().removeIf(hex -> hex.caster().equals(player.getUUID()));
        }
    }

    // =========================
    // Ability 1 - Mystery Prying
    // Normal: the eyes open for PRY_TICKS. Every second, hidden things within PRY_REACH glow through walls (invisible,
    //         unnoticed, spirits and summons; from Mysticologist (4) also the concealed, marionettes and those with a
    //         Substitute ready). Each enemy you look at while they are open is read once (health, mood, effects; its
    //         weakness from 50% Study; from Clairvoyant (3) its Pathway, Sequence and abilities) and gains PRY_STUDY.
    //         Eyes of Secret Inquiry (5): INQUIRY_REACH, and everything alive within INQUIRY_LIVING glows.
    // Sneak:  Dowsing. A pendulum swings toward the nearest enemy you cannot see within DOWSE_RANGE (behind walls or
    //         hidden) and a thread points the way for DOWSE_TICKS. With nothing hidden near it hangs still: free.
    // =========================
    public static void knowledgeEmperorMysteryPrying(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!ready(player, bypassClassCheck, PRY_SEQUENCE)) return;

        if (player.isShiftKeyDown()) {
            dowsing(player, sl);
            return;
        }
        if (!pay(player, PRY_COST)) return;
        pry(player, sl);
    }

    private static void pry(Player player, ServerLevel sl) {
        double reach = pryReach(player);
        CompoundTag data = player.getPersistentData();
        data.putLong(PRY_UNTIL, sl.getGameTime() + PRY_TICKS);
        data.putDouble(PRY_REACH_NOW, reach);
        PRY_READ.put(player.getUUID(), new HashSet<>());
        KnowledgeEmperorFx.pry(sl, player, reach, Beyonder.hasSequence(player, INQUIRY_SEQUENCE));
        KnowledgeEmperorFx.status(player, Status.PRYING, PRY_TICKS);
        glowHidden(player, sl, reach);
    }

    private static double pryReach(Player player) {
        if (Beyonder.hasSequence(player, CLAIRVOYANT_PRY_SEQUENCE)) return CLAIRVOYANT_REACH;
        if (Beyonder.hasSequence(player, INQUIRY_SEQUENCE)) return INQUIRY_REACH;
        return PRY_REACH;
    }

    /** Every tick while the eyes are open: the one looked at is read (once); every second the hidden glow again. */
    private static void pryTick(Player player, ServerLevel sl) {
        double reach = player.getPersistentData().getDoubleOr(PRY_REACH_NOW, PRY_REACH);
        if (player.tickCount % 20 == 0) glowHidden(player, sl, reach);
        LivingEntity looked = Spells.aimEnemy(player, sl, reach);
        if (looked == null) return;
        Set<UUID> read = PRY_READ.computeIfAbsent(player.getUUID(), id -> new HashSet<>());
        if (!read.add(looked.getUUID())) return;
        Study.add(player, looked, PRY_STUDY);
        if (player instanceof ServerPlayer viewer) KnowledgeEmperorFx.pryRead(viewer, looked);
        player.sendSystemMessage(read(player, looked));
    }

    /** What can be seen of the hidden within reach: they glow (or, if they cannot be drawn at all, stars trace round them). */
    private static void glowHidden(Player player, ServerLevel sl, double reach) {
        boolean seeThrough = Beyonder.hasSequence(player, SEE_THROUGH_SEQUENCE);
        boolean inquiry = Beyonder.hasSequence(player, INQUIRY_SEQUENCE);
        for (LivingEntity one : Spells.livingAround(player, sl, player.position(), reach)) {
            boolean everything = inquiry && one.distanceTo(player) <= INQUIRY_LIVING;
            boolean hidden = isHiddenThing(one, seeThrough);
            if (!everything && !hidden) continue;
            if (Concealment.isHidden(one)) {
                if (seeThrough || everything) KnowledgeEmperorFx.pryGlimpse(sl, one);   // nothing of them is drawn: a glimpse
                continue;
            }
            Spells.keepEffect(one, MobEffects.GLOWING, 0, PRY_GLOW_TICKS);
            if (hidden && player.tickCount % 40 == 0) KnowledgeEmperorFx.pryGlimpse(sl, one);
        }
    }

    /** Something the Mystery Pryer's eyes find: the invisible, the unnoticed, spirits and summons; from 4 much more. */
    private static boolean isHiddenThing(LivingEntity one, boolean seeThrough) {
        if (one.hasEffect(MobEffects.INVISIBILITY) || Unnoticed.isUnnoticed(one)) return true;
        if (Marionette.isSummoned(one) || one.getType() == EntityTypes.VEX || one.getType() == EntityTypes.ALLAY) return true;
        if (!seeThrough) return false;
        return Concealment.isHidden(one) || Marionette.isMarionette(one) || Substitute.isArmed(one);
    }

    /** What the eyes read on someone. */
    private static Component read(Player player, LivingEntity one) {
        StringBuilder text = new StringBuilder();
        text.append(one.getDisplayName().getString())
                .append(": ").append(Math.round(one.getHealth())).append("/").append(Math.round(one.getMaxHealth())).append(" health, ")
                .append(moodOf(player, one));
        List<String> effects = new ArrayList<>();
        for (MobEffectInstance effect : one.getActiveEffects()) {
            if (effect.getEffect() == MobEffects.GLOWING) continue;                   // the eyes' own doing
            effects.add(effect.getEffect().value().getDisplayName().getString() + " " + (effect.getAmplifier() + 1));
        }
        if (!effects.isEmpty()) text.append(", under ").append(String.join(", ", effects));
        if (Study.knowsWeakness(player, one)) text.append(", weak to ").append(elementName(weaknessOf(one)));
        if (Beyonder.hasSequence(player, CLAIRVOYANT_PRY_SEQUENCE) && one instanceof Player other) {
            String pathway = SoulCore.getPathway(other);
            if (!pathway.equals("none") && !pathway.isEmpty()) {
                int sequence = SoulCore.getSequence(other);
                text.append(", a Beyonder of ").append(pathway).append(" at Sequence ").append(sequence);
                List<String> abilities = new ArrayList<>();
                for (PathwayAbilities.Ability ability : PathwayAbilities.of(pathway)) {
                    if (ability.sequence() >= sequence) abilities.add(ability.name());
                }
                if (!abilities.isEmpty()) text.append(" (").append(String.join(", ", abilities)).append(")");
            }
        }
        text.append(". Study ").append(Study.get(player, one)).append("%");
        return Component.literal(text.toString());
    }

    private static String moodOf(Player player, LivingEntity one) {
        if (Fear.isAfraid(one)) return "afraid";
        if (one instanceof Mob mob && mob.getTarget() != null) return mob.getTarget() == player ? "furious with you" : "furious";
        return one instanceof Player ? "watchful" : "calm";
    }

    private static void dowsing(Player player, ServerLevel sl) {
        LivingEntity found = unseenEnemy(player, sl);
        String tag = "ke_dowse_" + player.getId();
        KnowledgeEmperorFx.dowseEnd(sl, player, tag);                   // cast again: one pendulum, not two
        if (found == null) {
            KnowledgeEmperorFx.dowse(sl, player, null, 0, tag);
            player.sendSystemMessage(Component.literal("The pendulum hangs still: nothing hidden hunts near you."));
            return; // nothing spent
        }
        if (!pay(player, DOWSE_COST)) return;
        KnowledgeEmperorFx.dowse(sl, player, found, DOWSE_TICKS, tag);
        KnowledgeEmperorFx.status(player, Status.DOWSING, DOWSE_TICKS);
        player.sendSystemMessage(Component.literal("The pendulum leans: something hides " + Math.round(found.distanceTo(player)) + " blocks away."));
    }

    /** The nearest enemy within DOWSE_RANGE that the player cannot see (behind blocks, invisible, hidden), or null. */
    private static LivingEntity unseenEnemy(Player player, ServerLevel sl) {
        LivingEntity nearest = null;
        double best = Double.MAX_VALUE;
        for (LivingEntity one : Spells.livingAround(player, sl, player.position(), DOWSE_RANGE)) {
            if (Spells.isFriend(player, one)) continue;
            boolean hostile = one instanceof Player || one instanceof Enemy || (one instanceof Mob mob && mob.getTarget() == player);
            if (!hostile) continue;
            boolean unseen = Concealment.isHidden(one) || one.hasEffect(MobEffects.INVISIBILITY) || Unnoticed.isUnnoticed(one)
                    || !player.hasLineOfSight(one);
            if (!unseen) continue;
            double distance = one.distanceToSqr(player);
            if (distance < best) {
                best = distance;
                nearest = one;
            }
        }
        return nearest;
    }

    // =========================
    // Ability 2 - Weak-Point Strike
    // Normal: step forward (STRIKE_STEP) to the enemy in front and strike it where it is weakest: STRIKE_DAMAGE that
    //         no armor softens (Spells.strikeTrue), a stagger, STRIKE_STUDY. Fists or any weapon.
    // Sneak:  Sidestep. For SIDESTEP_TICKS the next blow or projectile is slipped (status/Substitute) by a step of
    //         SIDESTEP_DISTANCE that leaves you at the attacker's side; your next hit within SIDESTEP_WINDOW is a
    //         free Weak-Point Strike.
    // =========================
    public static void knowledgeEmperorWeakPointStrike(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!ready(player, bypassClassCheck, STRIKE_SEQUENCE)) return;

        if (player.isShiftKeyDown()) {
            if (!pay(player, SIDESTEP_COST)) return;
            sidestep(player, sl);
            return;
        }
        LivingEntity target = Spells.aimEnemy(player, sl, STRIKE_STEP + STRIKE_REACH);
        if (target == null) {
            player.sendSystemMessage(Component.literal("There is no one close enough to strike."));
            return; // nothing spent
        }
        Vec3 stand = strikeSpot(player, sl, target);
        if (stand == null) {
            player.sendSystemMessage(Component.literal("You cannot reach it from here."));
            return; // nothing spent
        }
        if (!pay(player, STRIKE_COST)) return;
        weakPointStrike(player, sl, target, stand);
    }

    /** Where to stand for the strike: where you are if it is in reach, else a step toward it. Null: out of reach. */
    private static Vec3 strikeSpot(Player player, ServerLevel sl, LivingEntity target) {
        double gap = Spells.distanceToBody(target, player.getEyePosition());
        if (gap <= STRIKE_REACH - 1.0) return player.position();
        if (Root.has(player, RootRestriction.MOVE) || Root.has(player, RootRestriction.TELEPORT)) {
            return gap <= STRIKE_REACH ? player.position() : null;
        }
        Vec3 toward = target.position().subtract(player.position());
        Vec3 flat = new Vec3(toward.x, 0, toward.z);
        if (flat.lengthSqr() < 1.0E-4) return player.position();
        Vec3 direction = flat.normalize();
        double step = Math.min(STRIKE_STEP, Math.max(0, flat.length() - target.getBbWidth() * 0.5 - 0.9));
        Vec3 there = player.position().add(direction.scale(step));
        if (Spells.fits(player, sl, there) && Spells.clearLine(player, sl, player.getEyePosition(), there.add(0, player.getEyeHeight(), 0))) return there;
        Vec3 near = Spells.freeSpotNear(player, sl, player.position(), direction, step);
        if (near != null && Spells.distanceToBody(target, near.add(0, player.getEyeHeight(), 0)) <= STRIKE_REACH) return near;
        return gap <= STRIKE_REACH ? player.position() : null;
    }

    private static void weakPointStrike(Player player, ServerLevel sl, LivingEntity target, Vec3 stand) {
        Vec3 from = player.position();
        if (stand.distanceToSqr(from) > 0.01) {
            player.teleportTo(stand.x, stand.y, stand.z);
            player.fallDistance = 0;
        }
        KnowledgeEmperorFx.weakPointStrike(sl, player, from, target);
        Later.run(sl, KnowledgeEmperorFx.STRIKE_LANDS, () -> {
            if (!Spells.casterStillHere(player, sl) || !target.isAlive() || target.level() != sl) return;
            weakPointLands(player, target);
        });
    }

    /** The cut itself: through armor, a stagger, and Study. */
    private static void weakPointLands(Player player, LivingEntity target) {
        Spells.strikeTrue(player, target, STRIKE_DAMAGE);
        if (!target.isAlive()) return;
        Root.apply(target, STRIKE_STAGGER, RootRestriction.MOVE, RootRestriction.ATTACK);
        Study.add(player, target, STRIKE_STUDY);
    }

    private static void sidestep(Player player, ServerLevel sl) {
        long until = sl.getGameTime() + SIDESTEP_TICKS;
        player.getPersistentData().putLong(SIDESTEP_UNTIL, until);
        String tag = "ke_sidestep_" + player.getId();
        KnowledgeEmperorFx.sidestepEnd(sl, player, tag);
        KnowledgeEmperorFx.sidestep(sl, player, SIDESTEP_TICKS, tag);
        KnowledgeEmperorFx.status(player, Status.SIDESTEP, SIDESTEP_TICKS);
        Substitute.arm(player, SIDESTEP_TICKS, 1, (saved, source, amount, level) -> slip(player, level, source.getEntity(), tag));
        Later.run(sl, SIDESTEP_TICKS, () -> {
            if (player.getPersistentData().getLongOr(SIDESTEP_UNTIL, 0L) != until) return;      // slipped, or cast again
            player.getPersistentData().remove(SIDESTEP_UNTIL);
            KnowledgeEmperorFx.sidestepEnd(sl, player, tag);
        });
    }

    /** The blow was slipped: a quick step to the attacker's side (or aside), and the next hit is a free Weak-Point Strike. */
    private static void slip(Player player, ServerLevel sl, Entity attacker, String tag) {
        player.getPersistentData().remove(SIDESTEP_UNTIL);
        Vec3 from = player.position();
        Vec3 to = null;
        if (attacker instanceof LivingEntity living && living != player && living.level() == sl
                && living.distanceTo(player) <= SIDESTEP_TO_ATTACKER) {
            Vec3 at = living.position().subtract(from);
            Vec3 side = new Vec3(-at.z, 0, at.x);                                    // across the line between you
            if (side.lengthSqr() < 1.0E-4) side = Spells.rightOf(player);
            to = Spells.freeSpotNear(player, sl, living.position(), side, 1.4 + living.getBbWidth() * 0.5);
        }
        if (to == null) {
            Vec3 aside = Spells.rightOf(player).scale(sl.getRandom().nextBoolean() ? 1 : -1);
            to = Spells.freeSpotNear(player, sl, from, aside, SIDESTEP_DISTANCE);
        }
        if (to != null) {
            player.teleportTo(to.x, to.y, to.z);
            player.fallDistance = 0;
        }
        KnowledgeEmperorFx.sidestepSlip(sl, player, from, to != null ? to : from);
        KnowledgeEmperorFx.sidestepEnd(sl, player, tag);
        KnowledgeEmperorFx.statusEnd(player, Status.SIDESTEP);
        EmpoweredHit.arm(player, SIDESTEP_STRIKE, SIDESTEP_WINDOW, (striker, target, amount, level) -> {
            KnowledgeEmperorFx.weakPointStrike(level, striker, striker.position(), target);
            Root.apply(target, STRIKE_STAGGER, RootRestriction.MOVE, RootRestriction.ATTACK);
            Study.add(striker, target, STRIKE_STUDY);
            return amount + STRIKE_DAMAGE;
        });
    }

    // =========================
    // Ability 3 - Hex
    // Normal: curse the enemy you look at (HEX_RANGE) for HEX_TICKS: HEX_VULNERABLE more damage from everything,
    //         Weakness I and Slowness I, HEX_STUDY. If it dies, the hex jumps to the nearest enemy within HEX_JUMP for
    //         the time left. Scrollable.
    // Sneak:  Spellbolts. BOLT_COUNT bolts at the enemy in front, BOLT_DAMAGE each, of one element: Frost (slowed),
    //         Lightning (stunned) or Fire (burning); random below 50% Study, the one it is weakest to from there. With
    //         nothing in sight, one bolt of Lightning flies where you aim. Scrollable. Sage (2): the Knowledge Orbs,
    //         ORB_COUNT homing orbs that stun and hush (no abilities for a moment).
    // =========================
    public static void knowledgeEmperorSpells(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!ready(player, bypassClassCheck, HEX_SEQUENCE)) return;
        if (!(player instanceof ServerPlayer caster)) return;

        if (player.isShiftKeyDown()) {
            if (!pay(player, BOLT_COST)) return;
            spellbolts(caster, sl, 1f);
            Scrolls.cast(player, SCROLL_SPELLBOLTS);
            return;
        }
        LivingEntity target = Spells.aimEnemy(player, sl, HEX_RANGE);
        if (target == null) {
            player.sendSystemMessage(Component.literal("There is no one to curse."));
            return; // nothing spent
        }
        if (!pay(player, HEX_COST)) return;
        hex(player, sl, target, HEX_TICKS);
        Scrolls.cast(player, SCROLL_HEX);
    }

    /** Hex as a spell (a scroll): the enemy the reader looks at, or nothing happens. */
    private static boolean hexSpell(ServerPlayer reader, ServerLevel sl, float power) {
        LivingEntity target = Spells.aimEnemy(reader, sl, HEX_RANGE);
        if (target == null) {
            reader.sendSystemMessage(Component.literal("There is no one to curse."));
            return false;
        }
        hex(reader, sl, target, Math.round(HEX_TICKS * power));
        return true;
    }

    private static void hex(Player player, ServerLevel sl, LivingEntity target, int ticks) {
        String tag = "ke_hex_" + target.getId();
        KnowledgeEmperorFx.hex(sl, player, target, ticks, tag);
        Later.run(sl, KnowledgeEmperorFx.HEX_LANDS, () -> {
            if (!target.isAlive() || target.level() != sl) return;
            curse(player, target, ticks - KnowledgeEmperorFx.HEX_LANDS, tag);
            Study.add(player, target, HEX_STUDY);
        });
    }

    /** The hex itself on 'target' for 'ticks' (its look is put on by KnowledgeEmperorFx.hex or hexJump). */
    private static void curse(Player player, LivingEntity target, int ticks, String tag) {
        if (ticks <= 0) return;
        Vulnerable.add(target, HEX_VULNERABLE, HEX_VULNERABLE, ticks);
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 0));
        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ticks, 0));
        HEXES.put(target.getUUID(), new Hex(player.getUUID(), target.level().getGameTime() + ticks, tag));
        if (target instanceof Player hexed) KnowledgeEmperorFx.status(hexed, Status.HEXED, ticks);
    }

    private static void hexJump(ServerLevel sl, LivingEntity dead, Hex hex) {
        int left = (int) (hex.until() - sl.getGameTime());
        if (left <= 20) return;
        Player caster = sl.getPlayerByUUID(hex.caster());
        if (caster == null) return;
        LivingEntity next = null;
        double best = Double.MAX_VALUE;
        for (LivingEntity enemy : Spells.enemiesAround(caster, sl, dead.position(), HEX_JUMP)) {
            Hex other = HEXES.get(enemy.getUUID());
            if (enemy == dead || (other != null && other.until() > sl.getGameTime())) continue;   // hexed already
            double distance = enemy.distanceToSqr(dead);
            if (distance < best) {
                best = distance;
                next = enemy;
            }
        }
        if (next == null) {
            KnowledgeEmperorFx.hexEnd(sl, dead, hex.tag());
            return;
        }
        LivingEntity to = next;
        String tag = "ke_hex_" + to.getId();
        KnowledgeEmperorFx.hexJump(sl, dead, to, left, tag);
        Later.run(sl, 8, () -> {
            if (to.isAlive() && to.level() == sl) curse(caster, to, left - 8, tag);
        });
    }

    /** Spellbolts as a spell (also when read from a scroll): at the enemy in front, or one bolt where the reader aims. */
    private static boolean spellbolts(ServerPlayer player, ServerLevel sl, float power) {
        if (Beyonder.hasSequence(player, ORBS_SEQUENCE)) return knowledgeOrbs(player, sl, power);
        LivingEntity target = Spells.aimEnemy(player, sl, BOLT_RANGE);
        if (target == null) {
            boltWhereAimed(player, sl, power);
            return true;
        }
        Element element = Study.knowsWeakness(player, target) ? weaknessOf(target)
                : Element.values()[sl.getRandom().nextInt(Element.values().length)];
        List<LivingEntity> targets = new ArrayList<>();
        for (int i = 0; i < BOLT_COUNT; i++) targets.add(target);
        KnowledgeEmperorFx.spellbolts(sl, player, targets, element);
        for (int i = 0; i < BOLT_COUNT; i++) {
            boolean first = i == 0;
            Later.run(sl, KnowledgeEmperorFx.BOLT_FLIGHT + i * KnowledgeEmperorFx.BOLT_STEP, () -> {
                if (!Spells.casterStillHere(player, sl) || !target.isAlive() || target.level() != sl) return;
                boltLands(player, target, element, BOLT_DAMAGE * power, first);
                KnowledgeEmperorFx.boltHit(sl, target, element);
            });
        }
        return true;
    }

    /** Nothing in sight: one bolt of Lightning flies where the caster aims (a Shot). */
    private static void boltWhereAimed(Player player, ServerLevel sl, float power) {
        Vec3 hand = Spells.clearStart(player, sl, player.getEyePosition().add(Spells.rightOf(player).scale(0.35)).add(0, -0.25, 0));
        Shot.of(KnowledgeEmperorFx.BOLT(Element.LIGHTNING))
                .speed(2.0).range(BOLT_RANGE).width(0.4).tip(0.4)
                .onHit((hit, at) -> {
                    boltLands(player, hit, Element.LIGHTNING, BOLT_DAMAGE * power, true);
                    KnowledgeEmperorFx.boltHit(sl, hit, Element.LIGHTNING);
                })
                .onEnd((at, hitBlock) -> {
                    if (hitBlock) KnowledgeEmperorFx.boltBurst(sl, at, Element.LIGHTNING);
                })
                .fire(player, sl, hand, Spells.aimFrom(player, sl, hand, BOLT_RANGE));
        KnowledgeEmperorFx.boltBurst(sl, hand, Element.LIGHTNING);
    }

    /** One bolt lands: its damage (and Study on the first), and what its element does. */
    private static void boltLands(Player player, LivingEntity target, Element element, float damage, boolean first) {
        Study.strike(player, target, damage, first ? ABILITY_STUDY : 0);
        if (!target.isAlive()) return;
        switch (element) {
            case FROST -> Spells.keepEffect(target, MobEffects.SLOWNESS, 1, FROST_TICKS);
            case LIGHTNING -> Root.apply(target, LIGHTNING_STUN, RootRestriction.MOVE, RootRestriction.JUMP, RootRestriction.ATTACK);
            case FIRE -> target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), FIRE_TICKS));
        }
    }

    /** Sage: the Knowledge Orbs, shared out over the enemies in sight. With none, a bolt where the caster aims. */
    private static boolean knowledgeOrbs(ServerPlayer player, ServerLevel sl, float power) {
        List<LivingEntity> seen = Spells.enemiesInSight(player, sl, BOLT_RANGE, 45);
        if (seen.isEmpty()) {
            boltWhereAimed(player, sl, power);
            return true;
        }
        List<LivingEntity> targets = new ArrayList<>();
        for (int i = 0; i < ORB_COUNT; i++) targets.add(seen.get(i % seen.size()));
        KnowledgeEmperorFx.knowledgeOrbs(sl, player, targets);
        Set<UUID> studied = new HashSet<>();
        for (int i = 0; i < targets.size(); i++) {
            LivingEntity target = targets.get(i);
            boolean first = studied.add(target.getUUID());
            Later.run(sl, KnowledgeEmperorFx.ORB_FLIGHT + i * KnowledgeEmperorFx.BOLT_STEP, () -> {
                if (!Spells.casterStillHere(player, sl) || !target.isAlive() || target.level() != sl) return;
                Study.strike(player, target, ORB_DAMAGE * power, first ? ABILITY_STUDY : 0);
                KnowledgeEmperorFx.orbHit(sl, target);
                if (!target.isAlive()) return;
                Root.apply(target, ORB_STUN, RootRestriction.MOVE, RootRestriction.JUMP, RootRestriction.ATTACK);
                Root.apply(target, ORB_HUSH, RootRestriction.ABILITIES);
            });
        }
        return true;
    }

    // =========================
    // Ability 4 - Read Scroll
    // Normal: the oldest scroll in the pouch (status/Scrolls) is burnt and its spell goes off where you aim, free.
    //         If its spell finds nothing to do (no target), the scroll is rolled up again, unspent.
    // Sneak:  Inscribe Scroll. A scroll of the last Scrollable spell you cast, for its cost plus INSCRIBE_SURCHARGE
    //         now; later it fires at full strength for free. A full pouch drops its oldest.
    // The pouch holds POUCH (BIG_POUCH from Mysticologist) and a free Scroll of Freezing or Paralysis (in turn) is
    // added every FREE_SCROLL_EVERY (BIG_POUCH_EVERY) while there is room.
    // =========================
    public static void knowledgeEmperorScrolls(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!ready(player, bypassClassCheck, SCROLLS_SEQUENCE)) return;
        if (!(player instanceof ServerPlayer reader)) return;

        if (player.isShiftKeyDown()) {
            inscribe(reader, sl);
            return;
        }
        String key = Scrolls.takeOldest(player);
        if (key == null) {
            player.sendSystemMessage(Component.literal("Your scroll pouch is empty."));
            return; // nothing spent
        }
        readScroll(reader, sl, key);
    }

    private static void readScroll(ServerPlayer reader, ServerLevel sl, String key) {
        Scrolls.Kind kind = Scrolls.kind(key);
        KnowledgeEmperorFx.readScroll(sl, reader, fxScroll(key));
        Later.run(sl, KnowledgeEmperorFx.READ_FIRES, () -> {
            if (!Spells.casterStillHere(reader, sl) || kind == null) return;
            if (!kind.spell().cast(reader, sl, 1f)) {
                Scrolls.putBack(reader, key);
                reader.sendSystemMessage(Component.literal("The Scroll of " + kind.name() + " finds nothing to work on; you roll it up again."));
            }
        });
    }

    private static void inscribe(ServerPlayer player, ServerLevel sl) {
        Scrolls.Kind kind = Scrolls.lastCast(player);
        if (kind == null) {
            player.sendSystemMessage(Component.literal("You have cast no spell that could be written down."));
            return; // nothing spent
        }
        if (!pay(player, Math.round(kind.cost() * INSCRIBE_SURCHARGE))) return;
        KnowledgeEmperorFx.inscribe(sl, player);
        Later.run(sl, KnowledgeEmperorFx.INSCRIBED, () -> {
            if (!Spells.casterStillHere(player, sl)) return;
            if (Scrolls.add(player, kind.key(), pouchSize(player)) != null) KnowledgeEmperorFx.scrollDropped(sl, player);
            player.sendSystemMessage(Component.literal("You write a Scroll of " + kind.name() + "."));
        });
    }

    /** How many scrolls this player's pouch holds. */
    static int pouchSize(Player player) {
        return Beyonder.hasSequence(player, BIG_POUCH_SEQUENCE) ? BIG_POUCH : POUCH;
    }

    /** Once a second: while there is room, a free Scroll of Freezing or Paralysis (in turn) arrives every so often. */
    private static void freeScrollTick(Player player, ServerLevel sl) {
        CompoundTag data = player.getPersistentData();
        long now = sl.getGameTime();
        int every = Beyonder.hasSequence(player, BIG_POUCH_SEQUENCE) ? BIG_POUCH_EVERY : FREE_SCROLL_EVERY;
        long next = data.getLongOr(NEXT_SCROLL, 0L);
        if (next == 0L || next - now > every) {                       // the first time, or the Sequence changed
            data.putLong(NEXT_SCROLL, now + every);
            return;
        }
        if (now < next) return;
        data.putLong(NEXT_SCROLL, now + every);
        if (Scrolls.isFull(player, pouchSize(player))) return;
        boolean paralysis = data.getBooleanOr(SCROLL_TURN, false);
        data.putBoolean(SCROLL_TURN, !paralysis);
        Scrolls.add(player, paralysis ? SCROLL_PARALYSIS : SCROLL_FREEZING, pouchSize(player));
        KnowledgeEmperorFx.scrollReady(sl, player, paralysis ? KnowledgeEmperorFx.Scroll.PARALYSIS : KnowledgeEmperorFx.Scroll.FREEZING);
    }

    /** The Scroll of Freezing: a clear ray falls on the enemy looked at: Slowness V and no jumping. */
    private static boolean freezingSpell(ServerPlayer reader, ServerLevel sl, float power) {
        LivingEntity target = Spells.aimEnemy(reader, sl, SCROLL_RANGE);
        if (target == null) return false;
        int ticks = Math.round(FREEZING_TICKS * power);
        String tag = "ke_frozen_" + target.getId();
        KnowledgeEmperorFx.freezingRay(sl, target, ticks, tag);
        Later.run(sl, KnowledgeEmperorFx.RAY_FALLS, () -> {
            if (!target.isAlive() || target.level() != sl) return;
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ticks, FREEZING_SLOWNESS));
            Root.apply(target, ticks, RootRestriction.JUMP);
            if (target instanceof Player held) KnowledgeEmperorFx.status(held, Status.FROZEN, ticks);
            Study.add(reader, target, SCROLL_STUDY);
        });
        return true;
    }

    /** The Scroll of Paralysis: a pale green ray flies at the enemy looked at and holds it completely. */
    private static boolean paralysisSpell(ServerPlayer reader, ServerLevel sl, float power) {
        LivingEntity target = Spells.aimEnemy(reader, sl, SCROLL_RANGE);
        if (target == null) return false;
        int ticks = Math.round(PARALYSIS_TICKS * power);
        String tag = "ke_paralyzed_" + target.getId();
        KnowledgeEmperorFx.paralysisRay(sl, reader, target, ticks, tag);
        Later.run(sl, KnowledgeEmperorFx.RAY_HITS, () -> {
            if (!target.isAlive() || target.level() != sl) return;
            Root.apply(target, ticks, RootRestriction.EVERYTHING);
            if (target instanceof Player held) KnowledgeEmperorFx.status(held, Status.PARALYZED, ticks);
            Study.add(reader, target, SCROLL_STUDY);
        });
        return true;
    }

    /** The picture a scroll is shown with: Freezing, Paralysis, or the gold seal of one written by hand. */
    private static KnowledgeEmperorFx.Scroll fxScroll(String key) {
        if (SCROLL_FREEZING.equals(key)) return KnowledgeEmperorFx.Scroll.FREEZING;
        if (SCROLL_PARALYSIS.equals(key)) return KnowledgeEmperorFx.Scroll.PARALYSIS;
        return KnowledgeEmperorFx.Scroll.INSCRIBED;
    }

    /** The pouch changed: its pictures by the hotbar. */
    private static void showPouch(Player player, List<String> pouch) {
        if (pouch.isEmpty()) {
            KnowledgeEmperorFx.pouchHide(player);
            return;
        }
        List<KnowledgeEmperorFx.Scroll> shown = new ArrayList<>();
        for (String key : pouch) shown.add(fxScroll(key));
        KnowledgeEmperorFx.pouch(player, shown);
    }

    // =========================
    // Study (status/Study): the eye over each studied enemy, which only the Hermit sees
    // =========================

    private static void showStudy(Player caster, LivingEntity target, int before, int after) {
        if (!(caster instanceof ServerPlayer viewer) || !isKnowledgeEmperor(viewer, 9)) return;
        KnowledgeEmperorFx.studyEye(viewer, target, after, EYE_TICKS);
        if (after >= Study.FULL && before < Study.FULL) KnowledgeEmperorFx.understood(viewer, target);
        else if (after > before) KnowledgeEmperorFx.studyGain(viewer, target);
        showUnderstood(target, after);
    }

    /** A player who is Understood sees it (the picture lasts a little longer than the eye, and is shown again with it). */
    private static void showUnderstood(LivingEntity target, int percent) {
        if (!(target instanceof Player known)) return;
        if (percent >= Study.FULL) KnowledgeEmperorFx.status(known, Status.UNDERSTOOD, EYE_TICKS + 20);
        else KnowledgeEmperorFx.statusEnd(known, Status.UNDERSTOOD);
    }

    /** Every few seconds: the eyes are shown again before they fade. */
    private static void refreshEyes(Player player) {
        if (!(player instanceof ServerPlayer viewer) || !isKnowledgeEmperor(viewer, 9)) return;
        for (Study.Known known : Study.knownBy(player)) {
            KnowledgeEmperorFx.studyEye(viewer, known.target(), known.percent(), EYE_TICKS);
            if (known.percent() >= Study.FULL) showUnderstood(known.target(), known.percent());
        }
    }

    /** The element a studied enemy fears most (Spellbolts from 50% Study, and Prying names it). */
    static Element weaknessOf(LivingEntity one) {
        if (one.fireImmune()) return Element.FROST;
        if (one.is(EntityTypeTags.FREEZE_IMMUNE_ENTITY_TYPES) || one.is(EntityTypeTags.ARTHROPOD) || one.is(EntityTypeTags.UNDEAD)) {
            return Element.FIRE;
        }
        if (one.is(EntityTypeTags.AQUATIC) || wearsMetal(one)) return Element.LIGHTNING;
        return one.getMaxHealth() >= 40f ? Element.FROST : Element.LIGHTNING;     // the big are slowed, the quick are stunned
    }

    private static boolean wearsMetal(LivingEntity one) {
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (METAL_ARMOR.contains(one.getItemBySlot(slot).getItem())) return true;
        }
        return false;
    }

    private static String elementName(Element element) {
        return switch (element) {
            case FROST -> "frost";
            case LIGHTNING -> "lightning";
            case FIRE -> "fire";
        };
    }

    /** True for a boss: the dragon, the wither, the warden, an elder guardian, or anything with more than BOSS_HEALTH. */
    static boolean isBoss(LivingEntity one) {
        return one.getType() == EntityTypes.ENDER_DRAGON || one.getType() == EntityTypes.WITHER
                || one.getType() == EntityTypes.WARDEN || one.getType() == EntityTypes.ELDER_GUARDIAN
                || one.getMaxHealth() > BOSS_HEALTH;
    }

    // =========================
    // What a cast costs (every key and every wheel entry goes through these three)
    // =========================

    /**
     * What a cast of 'cost' really costs this player now: nothing while a Whisper of the Hidden Sage is waiting. The
     * one place for every rule that changes a Knowledge Emperor's costs: the planned Law of Three (every third cast in
     * a row is free) and Divine Kingdom (half) go here too. Wheel entries call it through .costBy(...).
     */
    public static int costFor(Player player, int cost) {
        if (cost <= 0 || Beyonder.isCastingUnbound(player) || !SoulCore.getPathway(player).equals(PATHWAY)) return cost;
        if (whisperWaiting(player)) return 0;
        return cost;
    }

    /** Pays for a cast by key (after costFor) and runs afterCast. False, with the usual message, if it cannot. */
    static boolean pay(Player player, int cost) {
        int due = costFor(player, cost);
        if (due > 0 && !Spells.paySpirituality(player, due)) return false;
        afterCast(player, cost);
        return true;
    }

    /**
     * Every cast that went off (a key through pay, a wheel entry through .onCast(...)): a waiting Whisper is spent on
     * it. The place for whatever counts casts (the planned Law of Three).
     */
    public static void afterCast(Player player, int cost) {
        if (cost > 0 && whisperWaiting(player) && !Beyonder.isCastingUnbound(player)
                && SoulCore.getPathway(player).equals(PATHWAY)) {
            spendWhisper(player);
        }
    }

    // =========================
    // Passives (the once-a-second ones are lines in SequencePassives)
    // =========================

    /**
     * Mystery Pryer (9), once a second: Spiritual Intuition's outlines (hidden things within INTUITION_RADIUS, for
     * the Hermit alone) and the Whisper of the Hidden Sage (about every 5 minutes the next ability is free).
     */
    public static void mysteryPryerSenses(Player player) {
        if (!(player instanceof ServerPlayer viewer) || !(player.level() instanceof ServerLevel sl)) return;
        for (LivingEntity one : Spells.livingAround(player, sl, player.position(), INTUITION_RADIUS)) {
            if (Spells.isFriend(player, one)) continue;
            if (Concealment.isHidden(one) || one.hasEffect(MobEffects.INVISIBILITY) || Unnoticed.isUnnoticed(one)) {
                KnowledgeEmperorFx.intuitionOutline(viewer, one);
            }
        }
        if (!Beyonder.hasSequence(player, WHISPER_SEQUENCE) || whisperWaiting(player)) return;
        CompoundTag data = player.getPersistentData();
        long now = sl.getGameTime();
        long next = data.getLongOr(WHISPER_NEXT, 0L);
        if (next == 0L || next - now > WHISPER_EVERY + WHISPER_SPREAD) {
            data.putLong(WHISPER_NEXT, now + whisperGap(player));
            return;
        }
        if (now < next) return;
        data.putBoolean(WHISPER_WAITING, true);
        KnowledgeEmperorFx.whisper(viewer);
        player.sendSystemMessage(Component.literal("A voice from nowhere whispers knowledge to you. Your next ability costs nothing."));
    }

    /** Constellations Master (5), once a second: at night under open sky, spirituality comes back STARLIGHT_REGEN faster. */
    public static void starlight(Player player) {
        if (!(player.level() instanceof ServerLevel sl)) return;
        if (!sl.isDarkOutside() || !sl.canSeeSky(player.blockPosition())) return;
        float extra = Beyonder.spiritualityRegenPerTick(player) * 20f * STARLIGHT_REGEN;
        SoulCore.setSpiritualityQuietly(player, SoulCore.getSpirituality(player) + extra);
        if (player.tickCount % 60 == 0) KnowledgeEmperorFx.starlight(sl, player);
    }

    private static int whisperGap(Player player) {
        return WHISPER_EVERY - WHISPER_SPREAD + player.getRandom().nextInt(WHISPER_SPREAD * 2 + 1);
    }

    static boolean whisperWaiting(Player player) {
        return player.getPersistentData().getBooleanOr(WHISPER_WAITING, false);
    }

    private static void spendWhisper(Player player) {
        CompoundTag data = player.getPersistentData();
        data.remove(WHISPER_WAITING);
        data.putLong(WHISPER_NEXT, player.level().getGameTime() + whisperGap(player));
        KnowledgeEmperorFx.statusEnd(player, Status.WHISPER);
    }

    /** Once a second, for every player: Fighting Knowledge (8) while they are this Pathway at that Sequence; off otherwise. */
    private static void bodyTick(Player player) {
        boolean fighting = isKnowledgeEmperor(player, FIGHTING_SEQUENCE);
        Physique.keep(player, Attributes.ATTACK_SPEED, FIGHTING_SPEED_ID, FIGHTING_ATTACK_SPEED, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, fighting);
        Physique.keep(player, Attributes.ENTITY_INTERACTION_RANGE, FIGHTING_REACH_ID, FIGHTING_REACH, AttributeModifier.Operation.ADD_VALUE, fighting);
    }

    // =========================
    // Helpers shared by the abilities
    // =========================

    /** True for this Pathway at this Sequence or stronger. */
    public static boolean isKnowledgeEmperor(Player player, int sequence) {
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

    /** After a relog or a death: nothing of the timed casts is left over; what is kept (pouch, whisper) is shown again. */
    private static void clearState(Player player) {
        CompoundTag data = player.getPersistentData();
        for (String key : List.of(PRY_UNTIL, PRY_REACH_NOW, SIDESTEP_UNTIL)) data.remove(key);
        PRY_READ.remove(player.getUUID());
        SWINGS.remove(player.getUUID());
        KnowledgeEmperorStars.clearState(player);
        if (!SoulCore.getPathway(player).equals(PATHWAY)) return;
        for (Status status : Status.values()) KnowledgeEmperorFx.statusEnd(player, status);   // a clock left running before death
        Scrolls.show(player);
        if (whisperWaiting(player)) KnowledgeEmperorFx.statusForever(player, Status.WHISPER);
    }
}
