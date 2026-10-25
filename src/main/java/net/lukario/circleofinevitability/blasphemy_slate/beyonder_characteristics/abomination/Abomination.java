package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.abomination;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonder;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonders;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Physique;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.SoulCore;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.abomination.AbominationFx.Status;
import net.lukario.circleofinevitability.combat.Later;
import net.lukario.circleofinevitability.combat.Reel;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.lukario.circleofinevitability.status.Bleeding;
import net.lukario.circleofinevitability.status.Connection;
import net.lukario.circleofinevitability.status.ConjuredWeapon;
import net.lukario.circleofinevitability.status.DamageLink;
import net.lukario.circleofinevitability.status.Decay;
import net.lukario.circleofinevitability.status.FallGuard;
import net.lukario.circleofinevitability.status.Fear;
import net.lukario.circleofinevitability.status.GiantForm;
import net.lukario.circleofinevitability.status.Marionette;
import net.lukario.circleofinevitability.status.Misfortune;
import net.lukario.circleofinevitability.status.NoHeal;
import net.lukario.circleofinevitability.status.Silence;
import net.lukario.circleofinevitability.status.Slumber;
import net.lukario.circleofinevitability.status.Taunt;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.Result;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Abomination: the Chained Pathway of Lord of the Mysteries (docs/ABOMINATION.md). Seven keys in the Hand Of God
 * pattern, from Prisoner (Sequence 9) up to Chained (Sequence 0); every plain key has a normal and a sneak cast:
 *
 *   1 Blood Chain     (Prisoner)  a chain of blood drags the enemy you look at to you and holds it; the wound bleeds
 *                                                                              | sneak: Restraint
 *   2 Moonstruck      (Lunatic)   a moon that is not there drives the enemy you look at mad
 *                                                                              | sneak: Lunar Frenzy
 *   3 Rending Pounce  (Werewolf)  a leap onto the enemy you look at and three bleeding claws
 *                                                                              | sneak: Werewolf Form
 *   4 Zombie          (Zombie)    the Zombie wheel: Raise the Dead, Grave Chill, Undying Flesh, Death Sentence
 *                                 (AbominationZombie)                          | sneak: the last pick again
 *   5 Wraith          (Wraith)    the Wraith wheel: Mirror Step, Possession, Ghost Host (AbominationWraith)
 *                                                                              | sneak: the last pick again
 *   6 Chains          (Puppet)    the Chains wheel: Animate Objects, Grave Shackles, and from Chained (0) Thorned
 *                                 Mausoleum, Coffin of Ages, Thorn Garden (AbominationChains) | sneak: the last pick again
 *   7 Curses          (Disciple of Silence) the Curses wheel: Curse by Connection, Cursed Artifact, Rite of Silence,
 *                                 Bane Mist, Ancient Blight, Abomination Form (AbominationCurses) | sneak: the last pick again
 *
 * The wheels each have a class of their own beside this one, with their numbers at the top; this class ticks them
 * and starts them clean after a relog or a death. Every look and sound is in AbominationFx (see /abominationfx);
 * the ability classes only decide who is hurt, held and helped, and when. The reusable pieces come from
 * docs/SPELL_KIT.md: status/Bleeding (the wounds), combat/Fallen (the dead that rise), combat/Reel (the chain and
 * the pounce), status/GiantForm + status/ConjuredWeapon (Werewolf Form), Taunt and Fear (Moonstruck), root/Root,
 * combat/Hurled (Animate Objects), status/Connection (Curse by Connection) and status/Misfortune (every curse, laid
 * with curse(...) below so that Collection of Curses makes them all last longer).
 *
 * The passives that are a potion effect or an aura are in SequencePassives (the auras' bodies, baneAura and
 * chainedAura, are here); the ones that react to hits, effects, falls and mobs (Lunatic Mind, the Full Moon, Keen
 * Senses, Unliving Body, Weightless, Whispering Curse, Collection of Curses, Chained's thorns) are here too.
 * Nothing here breaks blocks. The numbers for every ability are right below.
 */
public class Abomination {

    public static final String PATHWAY = "Abomination";

    // =========================
    // Ability settings (cost = spirituality, ticks: 20 = 1 second, distances in blocks)
    // =========================

    // Ability 1 - Blood Chain (Sequence 9)
    static final int CHAIN_SEQUENCE = 9;
    static final int CHAIN_COST = 250;
    static final double CHAIN_RANGE = 16.0;
    static final float CHAIN_DAMAGE = 14f;
    static final int CHAIN_BLEED_TICKS = 80;             // the wound bleeds this long...
    static final float CHAIN_BLEED = 2f;                 // ...this much a second
    static final int CHAIN_DRAG_TICKS = 20;              // it is dragged for this long at most...
    static final double CHAIN_DRAG_SPEED = 1.1;
    static final double CHAIN_DRAG_TO = 2.0;             // ...until it is this close
    static final int CHAIN_HOLD = 30;                    // then held this long
    static final int CHAIN_TWO_SEQUENCE = 4;             // Puppet: a second chain...
    static final double CHAIN_SPREAD = 8.0;              // ...to the nearest other enemy this close to the first
    static final int CHAIN_FOUR_SEQUENCE = 1;            // Abomination: four chains...
    static final int CHAIN_FOUR_HOLD = 60;               // ...each holding this long
    static final float CHAIN_NO_DRAG_HEALTH = 150f;      // this much health or more: held where it stands, not dragged
    // sneak: Restraint
    static final int RESTRAINT_COST = 250;
    static final int RESTRAINT_TICKS = 200;
    static final float RESTRAINT_CUT = 0.5f;             // melee and projectiles 50% less
    static final int RESTRAINT_SILENT_SEQUENCE = 3;      // Disciple of Silence: 60% less, and not slowed
    static final float RESTRAINT_SILENT_CUT = 0.6f;
    static final float RESTRAINT_FULL_MOON_CUT = 0.35f;  // under the full moon it holds less
    static final int RESTRAINT_SLOW = 1;                 // Slowness II while bound

    // Ability 2 - Moonstruck (Sequence 8)
    static final int MOON_SEQUENCE = 8;
    static final int MOON_COST = 300;
    static final double MOON_RANGE = 24.0;
    static final int MOON_TICKS = 120;
    static final double MOON_TURN_RANGE = 16.0;          // a mob turns on the nearest other mob this close
    // sneak: Lunar Frenzy
    static final int FRENZY_COST = 400;
    static final int FRENZY_TICKS = 200;
    static final int FRENZY_STRENGTH = 1;                // Strength II
    static final int FRENZY_SPEED = 1;                   // Speed II
    static final int FRENZY_REGENERATION = 0;            // Regeneration I
    static final int CRASH_TICKS = 120;                  // then Weakness I and Slowness I this long
                                                                 // (twice as long after a frenzy under the full moon)

    // Ability 3 - Rending Pounce (Sequence 7)
    static final int POUNCE_SEQUENCE = 7;
    static final int POUNCE_COST = 700;
    static final double POUNCE_RANGE = 14.0;
    static final double POUNCE_SPEED = 1.3;
    static final int POUNCE_TICKS = 20;                  // the leap lets go after this long anyway
    static final int CLAWS = 3;
    static final int CLAW_EVERY = 4;
    static final float CLAW_DAMAGE = 14f;
    static final double CLAW_REACH = 4.0;
    static final int CLAW_BLEED_TICKS = 80;
    static final float CLAW_BLEED = 2f;
    static final int CLAW_DOWN = 20;                     // the last claw throws it down: it cannot move or jump this long
    // sneak: Werewolf Form
    static final int WOLF_COST = 1000;
    static final int WOLF_COOLDOWN = 1200;
    static final int WOLF_TICKS = 800;
    static final double WOLF_SCALE = 1.1;                // a tenth bigger...
    static final double WOLF_PUPPET_SCALE = 1.25;        // ...a quarter at Puppet (4)
    static final int WOLF_PUPPET_SEQUENCE = 4;
    static final int WOLF_BANE_SEQUENCE = 2;             // Ancient Bane: no madness, until cast again
    static final double WOLF_REACH = 0.5;
    static final double WOLF_STEP = 0.5;
    static final float WOLF_MADNESS = 1f / 40f;          // full after 40 s...
    static final float WOLF_PUPPET_MADNESS = 1f / 60f;   // ...60 s at Puppet; twice as fast under the full moon
    static final int WOLF_STRENGTH = 1;                  // Strength II
    static final int WOLF_SPEED = 1;                     // Speed II
    static final float WOLF_CLAW_DAMAGE = 5f;            // more on every swing while in the form...
    static final int WOLF_CLAW_BLEED_TICKS = 60;         // ...which bleeds...
    static final float WOLF_CLAW_BLEED = 2f;
    static final int WOLF_CLAW_NO_HEAL = 60;             // ...and stops it healing
    static final int WOLF_BROKEN_TICKS = 200;            // full madness: the form breaks and you are weak this long

    // Passives that need more than SequencePassives' check once a second
    static final int LUNATIC_SEQUENCE = 8;               // Lunatic Mind: fear and nausea do not take; the Full Moon
    static final int FULL_MOON_STRENGTH = 1;             // under the full moon: Strength II...
    static final int FULL_MOON_SPEED = 0;                // ...and Speed I
    static final int KEEN_SEQUENCE = 7;                  // Keen Senses: a sound when something starts hunting you unseen
    static final int KEEN_REST = 60;
    static final int UNLIVING_SEQUENCE = 6;              // Unliving Body: poison, hunger and freezing do not take...
    static final float UNLIVING_PROJECTILE_CUT = 0.25f;  // ...and projectiles do 25% less
    static final int WEIGHTLESS_SEQUENCE = 5;            // Weightless: no fall damage; spirits near you are marked
    static final double WEIGHTLESS_SIGHT = 16.0;
    static final int PUPPETEER_SEQUENCE = 4;             // Puppeteer: what you summon or take over lasts longer
    static final float PUPPETEER_LONGER = 1.5f;
    static final int WHISPER_SEQUENCE = 3;               // Whispering Curse: a melee attacker may be cursed...
    static final float WHISPER_CHANCE = 0.15f;
    static final int WHISPER_CURSE_TICKS = 200;          // ...Misfortune I this long; a hush does not stop your keys
    static final int BANE_AURA_SEQUENCE = 2;             // Bane Aura: enemies near you carry Misfortune I...
    static final double BANE_AURA_RADIUS = 10.0;
    static final int BANE_AURA_TICKS = 40;               // ...for 2 seconds after they leave; Misfortune, Fear, Decay do not take
    static final int COLLECTION_SEQUENCE = 1;            // Collection of Curses: curses you lay last longer...
    static final float COLLECTION_LONGER = 1.5f;
    static final float COLLECTION_CHANCE = 0.25f;        // ...and a melee attacker is cursed more often
    static final int CHAINED_SEQUENCE = 0;               // Chained Aura: enemies near you are slowed and cannot teleport...
    static final double CHAINED_AURA_RADIUS = 16.0;
    static final float CHAINED_THORNS = 4f;              // ...and a melee attacker takes thorn damage

    // =========================
    // Saved on the player (game times are "until" this tick)
    // =========================
    static final String RESTRAINT_UNTIL = "abomination_restraint_until";
    static final String RESTRAINT_TAG = "abomination_restraint_tag";
    static final String FRENZY_UNTIL = "abomination_frenzy_until";
    static final String FRENZY_TAG = "abomination_frenzy_tag";
    static final String FRENZY_FULL_MOON = "abomination_frenzy_full_moon";
    static final String CRASH_UNTIL = "abomination_crash_until";
    static final String POUNCE_UNTIL = "abomination_pounce_until";
    static final String WOLF_READY = "abomination_wolf_ready";
    static final String WOLF_TAG = "abomination_wolf_tag";
    static final String WOLF_HEIGHT = "abomination_wolf_height";
    static final String WOLF_MAD_SHOWN = "abomination_wolf_mad_shown";
    static final String FULL_MOON_ON = "abomination_full_moon_on";
    static final String KEEN_READY = "abomination_keen_ready";

    // Saved on an enemy
    static final String MOONSTRUCK_UNTIL = "abomination_moonstruck_until";
    /** On a risen servant or a ghost: what it hits carries a level of Misfortune (Ancient Bane). */
    static final String BANE_TOUCH = "abomination_bane_touch";
    static final int BANE_TOUCH_TICKS = 200;

    // Names of the parts that a player has one of
    static final String CLAWS_NAME = "abomination_werewolf_claws";

    // Attribute modifiers (beyonder/Physique)
    private static final Identifier RESTRAINT_FOOTING_ID = Physique.id("abomination_restraint_footing");

    @Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class AbominationEvents {

        static {
            // every drip of a wound an Abomination opened shows
            Bleeding.addListener((victim, amount, source) -> {
                if (source != null && SoulCore.getPathway(source).equals(PATHWAY) && victim.level() instanceof ServerLevel sl) {
                    AbominationFx.bleed(sl, victim);
                }
            });
            // Undying Flesh: a body that feels no pain does not bleed
            Bleeding.addImmunity(entity -> entity instanceof Player player && AbominationZombie.isUndying(player));
            // Lunatic Mind: fear does not take
            Fear.addImmunity(entity -> entity instanceof Player player && isAbomination(player, LUNATIC_SEQUENCE));
            // Bane Aura: the Bane's own curses do not take on it, nor does decay
            Misfortune.addImmunity(entity -> entity instanceof Player player && isAbomination(player, BANE_AURA_SEQUENCE));
            Decay.addImmunity(entity -> entity instanceof Player player && isAbomination(player, BANE_AURA_SEQUENCE));
            // Whispering Curse: a hush does not stop a Disciple of Silence's keys
            Silence.addCastsThrough((player, key) -> castsThroughHush(player));
            // Grave Shackles: each time pain passes along the chains, it shows
            DamageLink.addShareListener((from, to, amount, credit) -> {
                if (credit != null && SoulCore.getPathway(credit).equals(PATHWAY) && to.level() instanceof ServerLevel sl) {
                    AbominationFx.painShared(sl, from, to);
                }
            });
            // a new connection blinks over its target, for the Disciple's eyes only
            Connection.addListener((owner, target, fresh) -> {
                if (fresh && owner instanceof ServerPlayer viewer && isAbomination(owner, WHISPER_SEQUENCE)) {
                    AbominationFx.connectionMade(viewer, target);
                }
            });
        }

        @SubscribeEvent
        public static void onAbominationTick(TickEvent.PlayerTickEvent.Post event) {
            Player player = event.player();
            if (!(player.level() instanceof ServerLevel sl)) return;
            // these run out even if the Pathway is taken away in the meantime (or the cast was stolen)
            if (isActive(player, RESTRAINT_UNTIL)) {
                if (player.isSprinting()) player.setSprinting(false);
                if (player.tickCount % 10 == 0 && !Beyonder.hasSequence(player, RESTRAINT_SILENT_SEQUENCE)) {
                    Spells.keepEffect(player, MobEffects.SLOWNESS, RESTRAINT_SLOW, 30);
                }
            }
            if (isActive(player, POUNCE_UNTIL)) AbominationFx.pounceTrail(sl, player);
            if (player.tickCount % 5 == 0 && isActive(player, FRENZY_UNTIL)) AbominationFx.frenzyTick(sl, player);
            AbominationZombie.tick(player, sl);
            if (player.tickCount % 20 == 0) {
                bodyTick(player);
                fullMoonTick(player, sl);
                if (isAbomination(player, WEIGHTLESS_SEQUENCE)) weightlessTick(player, sl);
                if (isAbomination(player, UNLIVING_SEQUENCE) && player.getTicksFrozen() > 0) player.setTicksFrozen(0);
            }
        }

        @SubscribeEvent
        public static void onAbominationHurt(LivingHurtEvent event) {
            LivingEntity victim = event.getEntity();
            if (!(victim.level() instanceof ServerLevel sl)) return;
            DamageSource source = event.getSource();

            // what a risen servant or a ghost touches carries Misfortune (Ancient Bane)
            if (source.getEntity() instanceof Mob servant && servant.getPersistentData().getBooleanOr(BANE_TOUCH, false)) {
                UUID master = Marionette.masterId(servant);
                Player owner = master == null ? null : sl.getPlayerByUUID(master);
                if (owner != null && victim != owner) curse(victim, owner, 1, Misfortune.MAX_LEVEL, curseTicks(owner, BANE_TOUCH_TICKS), false);
            }

            // a player, or a Beyonder mob of this Pathway (its Restraint, its form and its curses answer blows too)
            if (!(victim instanceof Player) && !PATHWAY.equals(Beyonders.pathwayOf(victim))) return;
            LivingEntity player = victim;
            boolean projectile = source.is(DamageTypeTags.IS_PROJECTILE);
            LivingEntity attacker = source.getEntity() instanceof LivingEntity living && living != player ? living : null;
            boolean melee = !projectile && attacker != null && source.getDirectEntity() == attacker && !source.is(DamageTypes.THORNS);
            if (attacker != null && AbominationCurses.inForm(player)) AbominationCurses.hurtInForm(player, sl, attacker);
            if (melee) meleeAnswered(player, sl, attacker);
            if (isActive(player, RESTRAINT_UNTIL) && (melee || projectile)) {
                event.setAmount(event.getAmount() * (1f - restraintCut(player, sl)));
                AbominationFx.restraintHit(sl, player);
            }
            if (projectile && isAbomination(player, UNLIVING_SEQUENCE)) {
                event.setAmount(event.getAmount() * (1f - UNLIVING_PROJECTILE_CUT));
            }
        }

        /** Coffin of Ages: nothing hurts the sealed. Unliving Body: freezing does not hurt. */
        @SubscribeEvent
        public static boolean onAbominationAttacked(LivingAttackEvent event) {
            LivingEntity victim = event.getEntity();
            if (victim.level().isClientSide()) return false;
            if (AbominationChains.isSealed(victim) && !event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return true;
            if (!(victim instanceof Player player)) return false;
            return event.getSource().is(DamageTypeTags.IS_FREEZING) && isAbomination(player, UNLIVING_SEQUENCE);
        }

        /** Lunatic Mind: nausea does not take. Unliving Body: poison and hunger do not. Undying Flesh: poison does not. */
        @SubscribeEvent
        public static void onAbominationEffect(MobEffectEvent.Applicable event) {
            if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) return;
            Holder<MobEffect> effect = event.getEffectInstance().getEffect();
            if (effect == MobEffects.NAUSEA && isAbomination(player, LUNATIC_SEQUENCE)) event.setResult(Result.DENY);
            if ((effect == MobEffects.POISON || effect == MobEffects.HUNGER) && isAbomination(player, UNLIVING_SEQUENCE)) {
                event.setResult(Result.DENY);
            }
            if (effect == MobEffects.POISON && AbominationZombie.isUndying(player)) event.setResult(Result.DENY);
        }

        /** Weightless: no fall damage. */
        @SubscribeEvent
        public static boolean onAbominationFall(LivingFallEvent event) {
            if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) return false;
            return isAbomination(player, WEIGHTLESS_SEQUENCE);
        }

        /** Keen Senses: a mob starts hunting the Werewolf from where they cannot see it. */
        @SubscribeEvent
        public static void onAbominationTarget(LivingChangeTargetEvent event) {
            if (!(event.getNewTarget() instanceof ServerPlayer hunted) || event.getEntity().level().isClientSide()) return;
            LivingEntity mob = event.getEntity();
            if (!isAbomination(hunted, KEEN_SEQUENCE)) return;
            long now = hunted.level().getGameTime();
            if (hunted.getPersistentData().getLongOr(KEEN_READY, 0L) > now) return;
            Vec3 toMob = mob.position().subtract(hunted.position());
            boolean behind = toMob.horizontalDistanceSqr() > 0.01 && Spells.flatLook(hunted).dot(new Vec3(toMob.x, 0, toMob.z).normalize()) < 0.2;
            if (!behind && hunted.hasLineOfSight(mob)) return;                     // seen coming: no warning needed
            hunted.getPersistentData().putLong(KEEN_READY, now + KEEN_REST);
            AbominationFx.keenSenses(hunted);
        }

        // what was on the screen and the body does not survive relogging or dying: start clean
        @SubscribeEvent
        public static void onAbominationLogin(PlayerEvent.PlayerLoggedInEvent event) {
            clearState(event.getEntity());
        }

        @SubscribeEvent
        public static void onAbominationRespawn(PlayerEvent.PlayerRespawnEvent event) {
            clearState(event.getEntity());
        }
    }

    // =========================
    // Ability 1 - Blood Chain
    // Normal: a chain of dark blood whips from your hand to the enemy you look at (CHAIN_RANGE), strikes it for
    //         CHAIN_DAMAGE, drags it to within CHAIN_DRAG_TO of you and holds it there for CHAIN_HOLD. The wound
    //         bleeds CHAIN_BLEED a second for CHAIN_BLEED_TICKS. A boss (CHAIN_NO_DRAG_HEALTH) is held where it
    //         stands. Puppet (4): a second chain to the nearest other enemy within CHAIN_SPREAD of the first.
    //         Abomination (1): four chains, each holding CHAIN_FOUR_HOLD.
    // Sneak:  Restraint. For RESTRAINT_TICKS you bind yourself in your own chains: slowed and unable to sprint, but
    //         melee and projectiles do RESTRAINT_CUT less and nothing knocks you back. Disciple of Silence (3):
    //         RESTRAINT_SILENT_CUT and not slowed. Under the full moon it holds only RESTRAINT_FULL_MOON_CUT.
    //         Cast again to take the chains off (free).
    // =========================
    public static void abominationBloodChain(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!ready(player, bypassClassCheck, CHAIN_SEQUENCE)) return;

        if (player.isShiftKeyDown()) {
            if (isActive(player, RESTRAINT_UNTIL)) {                     // the key again: the chains come off
                endRestraint(player, sl);
                return;
            }
            if (!Spells.paySpirituality(player, RESTRAINT_COST)) return;
            restraint(player, sl);
            return;
        }
        LivingEntity target = Spells.aimEnemy(player, sl, CHAIN_RANGE);
        if (target == null) {
            player.sendSystemMessage(Component.literal("There is no one for your chain to reach."));
            return; // nothing spent
        }
        if (!Spells.paySpirituality(player, CHAIN_COST)) return;
        bloodChain(player, sl, target);
    }

    static void bloodChain(LivingEntity player, ServerLevel sl, LivingEntity first) {
        int count = Beyonder.hasSequence(player, CHAIN_FOUR_SEQUENCE) ? 4 : Beyonder.hasSequence(player, CHAIN_TWO_SEQUENCE) ? 2 : 1;
        int hold = Beyonder.hasSequence(player, CHAIN_FOUR_SEQUENCE) ? CHAIN_FOUR_HOLD : CHAIN_HOLD;
        List<LivingEntity> bound = new ArrayList<>();
        bound.add(first);
        for (LivingEntity other : Spells.enemiesAround(player, sl, first.position(), CHAIN_SPREAD)) {
            if (bound.size() >= count) break;
            if (other != first && Spells.canBeChosen(player, other)) bound.add(other);
        }
        for (LivingEntity target : bound) chainOne(player, sl, target, hold);
    }

    /** One chain: it reaches, strikes, drags and holds, then lets go at a fixed time. */
    private static void chainOne(LivingEntity player, ServerLevel sl, LivingEntity target, int hold) {
        AbominationFx.bloodChain(sl, player, target);
        String tag = "ab_chain_" + player.getId() + "_" + target.getId() + "_" + sl.getGameTime();
        Later.run(sl, AbominationFx.CHAIN_REACHES, () -> {
            if (!Spells.casterStillHere(player, sl) || !target.isAlive() || target.level() != sl) return;
            Spells.strike(player, target, CHAIN_DAMAGE);
            Bleeding.apply(target, player, CHAIN_BLEED_TICKS, CHAIN_BLEED);
            if (!target.isAlive()) return;
            int whole = CHAIN_DRAG_TICKS + hold;
            long letGo = sl.getGameTime() + whole;
            AbominationFx.bloodChainHold(sl, player, target, whole, tag);
            Later.run(sl, whole, () -> AbominationFx.bloodChainEnd(sl, target, tag));
            if (target.getMaxHealth() >= CHAIN_NO_DRAG_HEALTH) {                // too heavy: held where it stands
                Root.apply(target, whole, RootRestriction.MOVEMENT);
                return;
            }
            Reel.in(target, () -> Spells.casterStillHere(player, sl) ? player.position() : null)
                    .speed(CHAIN_DRAG_SPEED).stopAt(CHAIN_DRAG_TO).lasts(CHAIN_DRAG_TICKS)
                    .onEnd(() -> {
                        int left = (int) (letGo - sl.getGameTime());
                        if (left > 0 && target.isAlive()) Root.apply(target, left, RootRestriction.MOVEMENT);
                    })
                    .start(sl);
        });
    }

    static void restraint(LivingEntity player, ServerLevel sl) {
        CompoundTag data = player.getPersistentData();
        long until = sl.getGameTime() + RESTRAINT_TICKS;
        String tag = "ab_restraint_" + player.getId() + "_" + sl.getGameTime();
        data.putLong(RESTRAINT_UNTIL, until);
        data.putString(RESTRAINT_TAG, tag);
        restraintBody(player, true);
        AbominationFx.restraint(sl, player, RESTRAINT_TICKS, tag);
        AbominationFx.status(player, Status.RESTRAINT, RESTRAINT_TICKS);
        Later.run(sl, RESTRAINT_TICKS, () -> {
            if (player.getPersistentData().getLongOr(RESTRAINT_UNTIL, 0L) == until) endRestraint(player, sl);
        });
    }

    static void endRestraint(LivingEntity player, ServerLevel sl) {
        CompoundTag data = player.getPersistentData();
        String tag = data.getStringOr(RESTRAINT_TAG, "");
        data.remove(RESTRAINT_UNTIL);
        data.remove(RESTRAINT_TAG);
        restraintBody(player, false);
        MobEffectInstance slow = player.getEffect(MobEffects.SLOWNESS);
        if (slow != null && slow.getAmplifier() == RESTRAINT_SLOW && slow.getDuration() <= 30) player.removeEffect(MobEffects.SLOWNESS);
        if (player.isAlive()) AbominationFx.restraintEnd(sl, player, tag);
        AbominationFx.statusEnd(player, Status.RESTRAINT);
    }

    private static void restraintBody(LivingEntity player, boolean on) {
        Physique.keep(player, Attributes.KNOCKBACK_RESISTANCE, RESTRAINT_FOOTING_ID, 1.0, AttributeModifier.Operation.ADD_VALUE, on);
    }

    private static float restraintCut(LivingEntity player, ServerLevel sl) {
        float cut = Beyonder.hasSequence(player, RESTRAINT_SILENT_SEQUENCE) ? RESTRAINT_SILENT_CUT : RESTRAINT_CUT;
        return underFullMoon(player, sl) ? Math.min(cut, RESTRAINT_FULL_MOON_CUT) : cut;
    }

    // =========================
    // Ability 2 - Moonstruck
    // Normal: you show the enemy you look at (MOON_RANGE) a moon that is not there. For MOON_TICKS it is mad: a mob
    //         forgets you and goes after the nearest other mob within MOON_TURN_RANGE (Taunt), or runs from you
    //         (Fear) if there is none; a player is slowed, sick and cannot sprint toward you (Fear and nausea).
    //         The madness takes it when the moon is up (AbominationFx.MOON_SHOWN).
    // Sneak:  Lunar Frenzy. For FRENZY_TICKS: Strength II, Speed II and Regeneration I. When it ends you crash:
    //         Weakness and Slowness for CRASH_TICKS (twice that after a frenzy begun under the full moon).
    // =========================
    public static void abominationMoonstruck(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!ready(player, bypassClassCheck, MOON_SEQUENCE)) return;

        if (player.isShiftKeyDown()) {
            if (!Spells.paySpirituality(player, FRENZY_COST)) return;
            lunarFrenzy(player, sl);
            return;
        }
        LivingEntity target = Spells.aimEnemy(player, sl, MOON_RANGE);
        if (target == null) {
            player.sendSystemMessage(Component.literal("There is no one to show the moon to."));
            return; // nothing spent
        }
        if (!Spells.paySpirituality(player, MOON_COST)) return;
        moonstruck(player, sl, target);
    }

    static void moonstruck(LivingEntity player, ServerLevel sl, LivingEntity target) {
        String tag = "ab_moon_" + player.getId() + "_" + target.getId();
        long until = sl.getGameTime() + MOON_TICKS;
        target.getPersistentData().putLong(MOONSTRUCK_UNTIL, until);
        AbominationFx.moonstruckEnd(sl, target, tag);                       // shown again: one moon, not two
        AbominationFx.moonstruck(sl, player, target, MOON_TICKS, tag);
        int mad = MOON_TICKS - AbominationFx.MOON_SHOWN;
        Later.run(sl, AbominationFx.MOON_SHOWN, () -> {
            if (!Spells.casterStillHere(player, sl) || !target.isAlive() || target.level() != sl) return;
            if (target instanceof Player victim) {
                Fear.apply(victim, player, mad);
                Spells.keepEffect(victim, MobEffects.NAUSEA, 0, mad);
            } else if (target instanceof Mob mob) {
                LivingEntity other = nearestOtherMob(player, sl, mob);
                if (other == null || !Taunt.apply(mob, other, mad)) Fear.apply(mob, player, mad);
                else if (mob.getTarget() == player) mob.setTarget(other);
            }
            madness(sl, target, until, tag);
        });
    }

    /** Once a second while the moon stands over it: it shows its madness; at the end the moon fades. */
    private static void madness(ServerLevel sl, LivingEntity target, long until, String tag) {
        if (target.getPersistentData().getLongOr(MOONSTRUCK_UNTIL, 0L) != until) return;      // shown again since
        if (!target.isAlive() || sl.getGameTime() >= until) {
            target.getPersistentData().remove(MOONSTRUCK_UNTIL);
            if (target.isAlive()) AbominationFx.moonstruckEnd(sl, target, tag);
            return;
        }
        AbominationFx.madnessTick(sl, target, target.getRandom().nextFloat() < 0.3f);
        Later.run(sl, (int) Math.min(20, until - sl.getGameTime()), () -> madness(sl, target, until, tag));
    }

    /** The nearest mob to 'mob' that is not on the caster's side, within MOON_TURN_RANGE, or null. */
    private static LivingEntity nearestOtherMob(LivingEntity player, ServerLevel sl, Mob mob) {
        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (LivingEntity other : Spells.enemiesAround(player, sl, mob.position(), MOON_TURN_RANGE)) {
            if (other == mob || !(other instanceof Mob)) continue;
            double distance = other.distanceToSqr(mob);
            if (distance < bestDistance) {
                best = other;
                bestDistance = distance;
            }
        }
        return best;
    }

    static void lunarFrenzy(LivingEntity player, ServerLevel sl) {
        CompoundTag data = player.getPersistentData();
        AbominationFx.frenzyEnd(sl, player, data.getStringOr(FRENZY_TAG, ""));       // cast again: one glow, not two
        long until = sl.getGameTime() + FRENZY_TICKS;
        String tag = "ab_frenzy_" + player.getId() + "_" + sl.getGameTime();
        data.putLong(FRENZY_UNTIL, until);
        data.putString(FRENZY_TAG, tag);
        data.putBoolean(FRENZY_FULL_MOON, underFullMoon(player, sl));
        data.remove(CRASH_UNTIL);
        AbominationFx.statusEnd(player, Status.FRENZY_CRASH);
        player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, FRENZY_TICKS, FRENZY_STRENGTH));
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, FRENZY_TICKS, FRENZY_SPEED));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, FRENZY_TICKS, FRENZY_REGENERATION));
        AbominationFx.lunarFrenzy(sl, player, FRENZY_TICKS, tag);
        AbominationFx.status(player, Status.FRENZY, FRENZY_TICKS);
        Later.run(sl, FRENZY_TICKS, () -> {
            if (player.getPersistentData().getLongOr(FRENZY_UNTIL, 0L) != until || !Spells.casterStillHere(player, sl)) return;
            crash(player, sl, tag);
        });
    }

    /** The frenzy is over: the body pays for it. */
    private static void crash(LivingEntity player, ServerLevel sl, String tag) {
        CompoundTag data = player.getPersistentData();
        int ticks = data.getBooleanOr(FRENZY_FULL_MOON, false) ? CRASH_TICKS * 2 : CRASH_TICKS;
        long until = sl.getGameTime() + ticks;
        data.remove(FRENZY_UNTIL);
        data.remove(FRENZY_FULL_MOON);
        data.putLong(CRASH_UNTIL, until);
        removeOwn(player, MobEffects.STRENGTH, FRENZY_STRENGTH);
        removeOwn(player, MobEffects.SPEED, FRENZY_SPEED);
        removeOwn(player, MobEffects.REGENERATION, FRENZY_REGENERATION);
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 0));
        player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ticks, 0));
        AbominationFx.frenzyCrash(sl, player, ticks, tag);
        AbominationFx.status(player, Status.FRENZY_CRASH, ticks);
        Later.run(sl, ticks, () -> {
            if (player.getPersistentData().getLongOr(CRASH_UNTIL, 0L) != until) return;
            player.getPersistentData().remove(CRASH_UNTIL);
            player.getPersistentData().remove(FRENZY_TAG);
            if (Spells.casterStillHere(player, sl)) AbominationFx.frenzyEnd(sl, player, tag);
        });
    }

    // =========================
    // Ability 3 - Rending Pounce
    // Normal: you leap onto the enemy you look at (POUNCE_RANGE) and claw it CLAWS times, CLAW_EVERY ticks apart,
    //         CLAW_DAMAGE each. Every claw bleeds; the last one throws it down (CLAW_DOWN). No fall damage until you land.
    // Sneak:  Werewolf Form. For WOLF_TICKS you are a werewolf: WOLF_SCALE times your size (WOLF_PUPPET_SCALE from
    //         Puppet), Strength II, Speed II, Night Vision, and claws: every swing does WOLF_CLAW_DAMAGE more, bleeds
    //         and stops the target healing. Madness rises each second (slower from Puppet, twice as fast under the
    //         full moon); at full the form breaks and you are left weak. Ancient Bane (2): no madness, and it stays
    //         until you cast it again. Cast again to end it (free). Only for an Abomination's own body.
    // =========================
    public static void abominationRendingPounce(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!ready(player, bypassClassCheck, POUNCE_SEQUENCE)) return;

        if (player.isShiftKeyDown()) {
            if (isWolf(player)) {                                        // the key again: the form falls away
                GiantForm.end(player);
                return;
            }
            if (!SoulCore.getPathway(player).equals(PATHWAY)) {
                player.sendSystemMessage(Component.literal("Only a Werewolf's own body can take that shape."));
                return;
            }
            if (coolingDown(player, sl, WOLF_READY, "Werewolf Form")) return;
            if (!Spells.paySpirituality(player, WOLF_COST)) return;
            player.getPersistentData().putLong(WOLF_READY, sl.getGameTime() + WOLF_COOLDOWN);
            werewolfForm(player, sl);
            return;
        }
        if (isActive(player, POUNCE_UNTIL)) return;                     // already in the air
        if (Root.has(player, RootRestriction.MOVE)) {
            player.sendSystemMessage(Component.literal("You are held fast and cannot leap."));
            return; // nothing spent
        }
        LivingEntity target = Spells.aimEnemy(player, sl, POUNCE_RANGE);
        if (target == null) {
            player.sendSystemMessage(Component.literal("There is no prey within a leap."));
            return; // nothing spent
        }
        if (!Spells.paySpirituality(player, POUNCE_COST)) return;
        pounce(player, sl, target);
    }

    static void pounce(LivingEntity player, ServerLevel sl, LivingEntity target) {
        long until = sl.getGameTime() + POUNCE_TICKS;
        player.getPersistentData().putLong(POUNCE_UNTIL, until);
        FallGuard.protect(player, 100);
        AbominationFx.pounceLeap(sl, player);
        boolean[] clawed = {false};
        Reel.in(player, () -> Spells.casterStillHere(player, sl) && target.isAlive() && target.level() == sl ? target.position() : null)
                .speed(POUNCE_SPEED).stopAt(1.4).lasts(POUNCE_TICKS)
                .onArrive(() -> {
                    clawed[0] = true;
                    claws(player, sl, target);
                })
                .onEnd(() -> {
                    if (player.getPersistentData().getLongOr(POUNCE_UNTIL, 0L) == until) player.getPersistentData().remove(POUNCE_UNTIL);
                    // stopped just short (a step, a fence): close enough is close enough
                    if (!clawed[0] && target.isAlive() && player.distanceTo(target) <= CLAW_REACH) claws(player, sl, target);
                })
                .start(sl);
    }

    private static void claws(LivingEntity player, ServerLevel sl, LivingEntity target) {
        for (int i = 0; i < CLAWS; i++) {
            int claw = i;
            Later.run(sl, i * CLAW_EVERY, () -> {
                if (!Spells.casterStillHere(player, sl) || !target.isAlive() || target.level() != sl) return;
                if (player.distanceTo(target) > CLAW_REACH + target.getBbWidth()) return;     // it got away
                boolean last = claw == CLAWS - 1;
                Spells.strike(player, target, CLAW_DAMAGE);
                Bleeding.apply(target, player, CLAW_BLEED_TICKS, CLAW_BLEED);
                AbominationFx.pounceClaw(sl, player, target, claw);
                if (last && target.isAlive()) {
                    Spells.push(target, player.position(), 0.6, 0.1);
                    Root.apply(target, CLAW_DOWN, RootRestriction.MOVE, RootRestriction.JUMP);
                }
            });
        }
    }

    private static void werewolfForm(Player player, ServerLevel sl) {
        boolean bane = Beyonder.hasSequence(player, WOLF_BANE_SEQUENCE);
        boolean puppet = Beyonder.hasSequence(player, WOLF_PUPPET_SEQUENCE);
        double scale = puppet ? WOLF_PUPPET_SCALE : WOLF_SCALE;
        float madness = bane ? 0f : puppet ? WOLF_PUPPET_MADNESS : WOLF_MADNESS;
        if (underFullMoon(player, sl)) madness *= 2f;
        int ticks = bane ? 0 : WOLF_TICKS;                               // 0: until cast again
        double height = player.getBbHeight() * scale;
        String tag = "ab_wolf_" + player.getId() + "_" + sl.getGameTime();
        AbominationFx.werewolfForm(sl, player, height, bane ? AbominationFx.FOREVER : ticks, tag);   // first: it grows from the size they are now
        GiantForm.of(scale)
                .reach(WOLF_REACH).stepHeight(WOLF_STEP)
                .lasts(ticks)
                .madness(madness)
                .everySecond((giant, mad) -> wolfSecond(player, mad))
                .onMad(giant -> wolfMaddened(player))
                .onEnd((giant, why) -> wolfGone(player))
                .grow(player);                                           // (a form it had ends first, and clears its own marks)
        CompoundTag data = player.getPersistentData();
        data.putString(WOLF_TAG, tag);
        data.putDouble(WOLF_HEIGHT, height);
        data.remove(WOLF_MAD_SHOWN);
        ConjuredWeapon.of(CLAWS_NAME)
                .lasts(ticks)
                .damage(WOLF_CLAW_DAMAGE)
                .onHit((attacker, target, amount, charge, level) -> {
                    Bleeding.apply(target, attacker, WOLF_CLAW_BLEED_TICKS, WOLF_CLAW_BLEED);
                    NoHeal.apply(target, WOLF_CLAW_NO_HEAL);
                    AbominationFx.werewolfClaw(level, attacker, target);
                    return amount;
                })
                .draw(player);
        if (bane) AbominationFx.statusForever(player, Status.WEREWOLF);
        else AbominationFx.status(player, Status.WEREWOLF, ticks);
        wolfSecond(player, 0f);
    }

    /** Once a second in the form: its strength, and the madness shown. */
    private static void wolfSecond(LivingEntity player, float madness) {
        if (!(player.level() instanceof ServerLevel sl)) return;
        Spells.keepEffect(player, MobEffects.STRENGTH, WOLF_STRENGTH, 40);
        Spells.keepEffect(player, MobEffects.SPEED, WOLF_SPEED, 40);
        Spells.keepEffect(player, MobEffects.NIGHT_VISION, 0, 300);
        if (madness <= 0f) return;
        AbominationFx.werewolfMadness(sl, player, madness, player.getPersistentData().getDoubleOr(WOLF_HEIGHT, player.getBbHeight()));
        if (madness >= 0.5f && !player.getPersistentData().contains(WOLF_MAD_SHOWN)) {
            player.getPersistentData().putBoolean(WOLF_MAD_SHOWN, true);
            AbominationFx.statusForever(player, Status.MADNESS);
            Spells.tell(player, Component.literal("The wolf's madness is rising."));
        }
    }

    private static void wolfMaddened(LivingEntity player) {
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, WOLF_BROKEN_TICKS, 1));
        player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, WOLF_BROKEN_TICKS, 0));
        Spells.tell(player, Component.literal("The madness tears the wolf out of you."));
    }

    private static void wolfGone(Player player) {
        CompoundTag data = player.getPersistentData();
        ConjuredWeapon.putAway(player, CLAWS_NAME);
        if (player.level() instanceof ServerLevel sl && player.isAlive()) {
            AbominationFx.werewolfFormEnd(sl, player, data.getStringOr(WOLF_TAG, ""));
        }
        AbominationFx.statusEnd(player, Status.WEREWOLF);
        AbominationFx.statusEnd(player, Status.MADNESS);
        // the wolf's own strength goes (the Prisoner's Strength I and the full moon's come back within a second)
        removeOwn(player, MobEffects.STRENGTH, WOLF_STRENGTH);
        removeOwn(player, MobEffects.SPEED, WOLF_SPEED);
        for (String key : List.of(WOLF_TAG, WOLF_HEIGHT, WOLF_MAD_SHOWN)) data.remove(key);
    }

    private static boolean isWolf(LivingEntity player) {
        return GiantForm.isGiant(player) && player.getPersistentData().contains(WOLF_TAG);
    }

    // =========================
    // Passives (the plain potion effects are in SequencePassives)
    // =========================

    /** Once a second, for every player: the body. Restraint's footing while it lasts; off otherwise. */
    private static void bodyTick(LivingEntity player) {
        restraintBody(player, isActive(player, RESTRAINT_UNTIL));
    }

    /** True for a Lunatic (or stronger) out under the full moon at night. */
    static boolean underFullMoon(LivingEntity player, ServerLevel sl) {
        return isAbomination(player, LUNATIC_SEQUENCE) && Spells.isFullMoonNight(sl)
                && sl.canSeeSky(BlockPos.containing(player.getEyePosition()));
    }

    /** The Full Moon, once a second: Strength II and Speed I under it, and its rising and setting shown once. */
    private static void fullMoonTick(LivingEntity player, ServerLevel sl) {
        CompoundTag data = player.getPersistentData();
        boolean on = underFullMoon(player, sl);
        boolean was = data.getBooleanOr(FULL_MOON_ON, false);
        if (on) {
            Spells.keepEffect(player, MobEffects.STRENGTH, FULL_MOON_STRENGTH, 60);
            Spells.keepEffect(player, MobEffects.SPEED, FULL_MOON_SPEED, 60);
            if (!was) {
                data.putBoolean(FULL_MOON_ON, true);
                AbominationFx.fullMoonRises(sl, player);
                AbominationFx.statusForever(player, Status.FULL_MOON);
                Spells.tell(player, Component.literal("The full moon is up. Your blood answers it."));
            }
        } else if (was) {
            data.remove(FULL_MOON_ON);
            AbominationFx.statusEnd(player, Status.FULL_MOON);
        }
    }

    /** Weightless, once a second: spirits, ghosts and summons near the Wraith are marked, for them alone. */
    private static void weightlessTick(LivingEntity player, ServerLevel sl) {
        if (!(player instanceof ServerPlayer viewer)) return;
        for (LivingEntity one : Spells.livingAround(player, sl, player.position(), WEIGHTLESS_SIGHT)) {
            if (isSpirit(one)) AbominationFx.spiritSeen(viewer, one);
        }
    }

    /** A spirit, a ghost or something called up: vexes, phantoms, allays and every summon. */
    static boolean isSpirit(Entity entity) {
        return entity.getType() == EntityTypes.VEX || entity.getType() == EntityTypes.PHANTOM
                || entity.getType() == EntityTypes.ALLAY || Marionette.isSummoned(entity);
    }

    /** Puppeteer: what an Abomination of Puppet (4) or stronger summons or takes over lasts PUPPETEER_LONGER as long. */
    static int servantTicks(LivingEntity player, int ticks) {
        return isAbomination(player, PUPPETEER_SEQUENCE) ? Math.round(ticks * PUPPETEER_LONGER) : ticks;
    }

    /** Whispering Curse and Collection of Curses: a melee attacker may be cursed. Chained: it is pricked by thorns. */
    static void meleeAnswered(LivingEntity player, ServerLevel sl, LivingEntity attacker) {
        if (isAbomination(player, WHISPER_SEQUENCE)) {
            float chance = isAbomination(player, COLLECTION_SEQUENCE) ? COLLECTION_CHANCE : WHISPER_CHANCE;
            if (player.getRandom().nextFloat() < chance
                    && curse(attacker, player, 1, Misfortune.MAX_LEVEL, curseTicks(player, WHISPER_CURSE_TICKS), false) > 0) {
                AbominationFx.whisperingCurse(sl, attacker);
            }
        }
        if (isAbomination(player, CHAINED_SEQUENCE) && attacker.isAlive()) {
            attacker.hurt(player.damageSources().thorns(player), CHAINED_THORNS);
            AbominationFx.thornsBack(sl, attacker);
        }
    }

    /** Bane Aura, once a second (SequencePassives): enemies near the Bane carry Misfortune I while they stay. */
    public static void baneAura(LivingEntity player) {
        if (!(player.level() instanceof ServerLevel sl)) return;
        for (LivingEntity enemy : Spells.enemiesAround(player, sl, player.position(), BANE_AURA_RADIUS)) {
            if (Misfortune.level(enemy) == 0) curse(enemy, player, 1, 1, curseTicks(player, BANE_AURA_TICKS), false);
        }
        AbominationFx.baneAura(sl, player, BANE_AURA_RADIUS);
    }

    /** Chained Aura, once a second (SequencePassives): enemies near the Chained are slowed and cannot teleport. */
    public static void chainedAura(LivingEntity player) {
        if (!(player.level() instanceof ServerLevel sl)) return;
        for (LivingEntity enemy : Spells.enemiesAround(player, sl, player.position(), CHAINED_AURA_RADIUS)) {
            Spells.keepEffect(enemy, MobEffects.SLOWNESS, 0, 40);
            Root.apply(enemy, 30, RootRestriction.TELEPORT);
        }
        AbominationFx.chainedAura(sl, player, CHAINED_AURA_RADIUS);
    }

    /** Whispering Curse: a Disciple of Silence (or stronger) casts through a hush, but not asleep or held fast. */
    static boolean castsThroughHush(LivingEntity player) {
        return isAbomination(player, WHISPER_SEQUENCE) && !Root.has(player, RootRestriction.MOVE) && !Slumber.isAsleep(player);
    }

    /** Collection of Curses: curses an Abomination (1) or stronger lays last COLLECTION_LONGER as long. */
    static int curseTicks(LivingEntity player, int ticks) {
        return isAbomination(player, COLLECTION_SEQUENCE) ? Math.round(ticks * COLLECTION_LONGER) : ticks;
    }

    /**
     * Every curse of this Pathway: Misfortune ('levels' more, up to 'max') for 'ticks' (give it curseTicks), and the
     * cursed one becomes the caster's connection (status/Connection). Unlike a plain Misfortune.add it never makes a
     * longer curse shorter. Returns the level it has now (0 = it cannot be cursed).
     */
    static int curse(LivingEntity target, LivingEntity source, int levels, int max, int ticks, boolean catastrophe) {
        if (target == null || source == null || target == source || !target.isAlive()) return 0;
        int level = Misfortune.add(target, source, levels, max, Math.max(ticks, Misfortune.ticksLeft(target)), catastrophe);
        if (level > 0 && source instanceof Player owner) Connection.touch(owner, target);     // (a mob keeps no connections)
        return level;
    }

    /** True for this Pathway at this Sequence or stronger. */
    static boolean isAbomination(Player player, int sequence) {
        return SoulCore.getPathway(player).equals(PATHWAY) && Beyonder.hasSequence(player, sequence);
    }

    /** The same for anyone: a player as above, a Beyonder mob of this Pathway by its own Sequence. */
    static boolean isAbomination(LivingEntity entity, int sequence) {
        if (entity instanceof Player player) return isAbomination(player, sequence);
        return PATHWAY.equals(Beyonders.pathwayOf(entity)) && Beyonder.hasSequence(entity, sequence);
    }

    // =========================
    // Helpers shared by the abilities
    // =========================

    /** Has this pathway (or the check is skipped) and has reached the Sequence the ability needs. */
    static boolean ready(Player player, boolean bypassClassCheck, int sequence) {
        if (!bypassClassCheck && !SoulCore.getPathway(player).equals(PATHWAY)) return false;
        return Beyonder.hasSequence(player, sequence);
    }

    /** True while the game time saved under 'key' is still ahead. */
    static boolean isActive(Entity entity, String key) {
        return entity.getPersistentData().getLongOr(key, 0L) > entity.level().getGameTime();
    }

    /** True (and says so) while the cooldown saved under 'key' runs. */
    private static boolean coolingDown(LivingEntity player, ServerLevel sl, String key, String name) {
        long left = player.getPersistentData().getLongOr(key, 0L) - sl.getGameTime();
        if (left <= 0) return false;
        Spells.tell(player, Component.literal(name + " is not ready yet (" + ((left + 19) / 20) + "s)."));
        return true;
    }

    /** Takes an effect off only if it is the one this Pathway gave (same level), so a stronger one from elsewhere stays. */
    static void removeOwn(LivingEntity player, Holder<MobEffect> effect, int amplifier) {
        MobEffectInstance has = player.getEffect(effect);
        if (has != null && has.getAmplifier() == amplifier) player.removeEffect(effect);
    }

    /** After a relog or a death: nothing of the timed casts is left over (their looks did not survive either). */
    private static void clearState(Player player) {
        CompoundTag data = player.getPersistentData();
        for (String key : List.of(RESTRAINT_UNTIL, RESTRAINT_TAG, FRENZY_UNTIL, FRENZY_TAG, FRENZY_FULL_MOON, CRASH_UNTIL,
                POUNCE_UNTIL, WOLF_TAG, WOLF_HEIGHT, WOLF_MAD_SHOWN, FULL_MOON_ON)) {
            data.remove(key);
        }
        restraintBody(player, false);
        AbominationZombie.clearState(player);
        AbominationWraith.clearState(player);
        AbominationChains.clearState(player);
        AbominationCurses.clearState(player);
        if (!SoulCore.getPathway(player).equals(PATHWAY)) return;
        for (Status status : Status.values()) AbominationFx.statusEnd(player, status);      // a clock left running before death
    }
}
