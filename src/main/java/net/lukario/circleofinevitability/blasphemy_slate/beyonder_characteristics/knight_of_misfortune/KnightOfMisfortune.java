package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.knight_of_misfortune;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonders;
import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonder;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Physique;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.SoulCore;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.knight_of_misfortune.KnightOfMisfortuneFx.Dream;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.knight_of_misfortune.KnightOfMisfortuneFx.Status;
import net.lukario.circleofinevitability.combat.Later;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.combat.Trap;
import net.lukario.circleofinevitability.combat.Zone;
import net.lukario.circleofinevitability.menu.AbilityMenu;
import net.lukario.circleofinevitability.particles.fx.ParticleShapes;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.lukario.circleofinevitability.status.Concealment;
import net.lukario.circleofinevitability.status.ConjuredArmor;
import net.lukario.circleofinevitability.status.FallGuard;
import net.lukario.circleofinevitability.status.Fear;
import net.lukario.circleofinevitability.status.Flight;
import net.lukario.circleofinevitability.status.GiantForm;
import net.lukario.circleofinevitability.status.Marionette;
import net.lukario.circleofinevitability.status.Misfortune;
import net.lukario.circleofinevitability.status.Nocturnality;
import net.lukario.circleofinevitability.status.Silence;
import net.lukario.circleofinevitability.status.Slumber;
import net.lukario.circleofinevitability.status.SupernaturalResistance;
import net.lukario.circleofinevitability.status.Taunt;
import net.lukario.circleofinevitability.status.Unnoticed;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Knight Of Misfortune: the Darkness Pathway of Lord of the Mysteries (docs/KNIGHT_OF_MISFORTUNE.md). Seven keys in
 * the Attendant Of Mysteries pattern, from Sleepless (Sequence 9) up to Darkness (Sequence 0). All seven keys and every
 * passive are written.
 *
 *   1 Night Sight     (Sleepless)      Spirit Vision: everything alive round you glows for you alone
 *                                                                      | sneak: Dowsing (who hunts you?)
 *   2 Midnight Poem   (Midnight Poet)  the poem wheel: Tranquilize, Lullaby, Pacify, Fear, Agitating
 *                                      (KnightOfMisfortunePoems)       | sneak: the last poem again
 *   3 Nightmare       (Nightmare)      Nightmare Limbs: four tentacles lash the nearest enemies
 *                                                                      | sneak: Nightmare State (your soul steps out);
 *                                      inside it the key is Dream Invasion and Dream Shaping, sneak goes back
 *   4 Requiem         (Soul Assurer)   a field of lullaby: enemies slow, then freeze, silenced; spirits held
 *                                                                      | sneak: Soothing Requiem (allies)
 *   5 Spirit Commanding (Spirit Warlock) the spirit wheel: Terror Banshee, Earthen Spirit, Gale Spirit, Command
 *                                      Spirits (KnightOfMisfortuneSpirits) | sneak: the last pick again
 *   6 Nightwatcher    (Nightwatcher)   the Nightwatcher wheel: Night Domain, Hair Entanglement, Curse of Misfortune
 *                                      (Misfortune at 1, Catastrophe at 0), Demonic Wolf Form
 *                                      (KnightOfMisfortuneNightwatcher)  | sneak: the last pick again
 *   7 Darkness and Concealment (Horror Bishop) the Darkness wheel: Sword of Darkness, Nightmare World, Concealed
 *                                      State, Secret Concealment, Concealed World, Mantle of Darkness, Eternal Rest,
 *                                      Destination of All Things (KnightOfMisfortuneDarkness) | sneak: the last pick again
 *
 * Every look and sound is in KnightOfMisfortuneFx (see /knightfx); the ability classes only decide who sleeps, fears,
 * is held and helped, and when. The reusable pieces new with this Pathway: status/Slumber, status/Fear, status/Silence
 * (and Silence.addCastsThrough), status/Misfortune, status/Nocturnality, Spells.addUnchoosableCheck and
 * Trap.hiddenNear; it also uses status/Concealment, Flight, ConjuredArmor, ConjuredWeapon, GiantForm, Marionette,
 * Decay, DeathWard, Unnoticed, combat/Zone, combat/Dome, menu/AbilityWheel and menu/AbilityMenu, root/Root.
 *
 * The passives that are a potion effect or a check once a second are in SequencePassives; the ones that react to
 * events (the danger sense, Steady Hand's falls and bow, the gale's empty hands, Ill Omen, the Archangel's body,
 * what Darkness cannot be touched by) are here. Nothing here changes blocks.
 */
public class KnightOfMisfortune {

    public static final String PATHWAY = "Knight Of Misfortune";

    // =========================
    // Ability settings (cost = spirituality, ticks: 20 = 1 second, distances in blocks)
    // =========================

    // Ability 1 - Night Sight (Sequence 9): Spirit Vision
    static final int SIGHT_SEQUENCE = 9;
    static final int VISION_COST = 250;
    static final int VISION_COOLDOWN = 300;
    static final int VISION_TICKS = 240;
    static final double VISION_RANGE = 32.0;
    static final double VISION_DARK_RANGE = 48.0;       // in the dark (Nocturnality)
    // sneak: Dowsing
    static final int DOWSE_COST = 250;
    static final int DOWSE_COOLDOWN = 600;
    static final int DOWSE_TICKS = 100;
    static final double DOWSE_HUNTER_RANGE = 48.0;      // a mob hunting you, this close...
    static final double DOWSE_RANGE = 128.0;            // ...else who last hurt you, else the nearest Beyonder
    static final int DOWSE_MEMORY = 1200;               // "last hurt you" means within this long

    // Ability 3 - Nightmare (Sequence 7): Nightmare Limbs
    static final int NIGHTMARE_SEQUENCE = 7;
    static final int LIMBS_COST = 700;
    static final int LIMBS_COOLDOWN = 300;
    static final int LIMBS_TICKS = 240;
    static final int LIMBS_COUNT = 4;
    static final double LIMBS_REACH = 6.0;
    static final float LIMBS_DAMAGE = 3f;               // per lash, times the power level
    static final float LIMBS_LAYER = 6f;                // the limbs' own flesh: absorption...
    static final float LIMBS_LAYER_PER_LEVEL = 1f;      // ...plus this per power level
    static final float LIMBS_BACKLASH = 0.2f;           // torn off: this much of your maximum health
    // sneak: Nightmare State
    static final int STATE_COST = 700;
    static final float STATE_UPKEEP = 35f;              // each second
    static final int STATE_TICKS = 600;                 // 30 s at most
    static final int BODY_THREAT_SECONDS = 3;           // enemies beside the body this long wreck it...
    static final double BODY_THREAT_RANGE = 1.8;
    static final float BODY_WRECK = 0.3f;               // ...and you take this much of your maximum health
    static final int LULL_TICKS = 100;                  // watched unseen this long, a mob drifts off...
    static final int LULL_SLEEP = 200;                  // ...to sleep for this long
    // inside the state: Dream Invasion and Dream Shaping
    static final int DREAM_COST = 400;
    static final int DREAM_COOLDOWN = 200;
    static final int DREAM_TICKS = 120;                 // the dreamer is held this long
    static final int DREAM_PICK_TICKS = 100;            // no pick within this long: Secrets
    static final float TERROR_SHARE = 0.25f;            // Terror: this much of its current health
    static final int TERROR_FEAR = 120;
    static final int OBEDIENCE_TICKS = 300;             // a mob wakes as your marionette this long...
    static final float OBEDIENCE_HEALTH = 60f;          // ...if its maximum health is at most this...
    static final float OBEDIENCE_HEALTH_PER_SEQUENCE = 30f;     // ...plus this for every Sequence past 7
    static final int OBEDIENCE_LIMP = 160;              // a player wakes limp (pacified) this long

    // Ability 4 - Requiem (Sequence 6)
    static final int REQUIEM_SEQUENCE = 6;
    static final int REQUIEM_COST = 2500;
    static final int REQUIEM_COOLDOWN = 600;
    static final int REQUIEM_TICKS = 200;               // from the moment it is sung
    static final double REQUIEM_RADIUS = 10.0;
    static final int REQUIEM_FREEZE = 120;              // this long inside: frozen still
    static final int REQUIEM_SLEEP_SEQUENCE = 4;        // Nightwatcher: whoever stays...
    static final int REQUIEM_SLEEP_AFTER = 100;         // ...this long falls asleep
    // sneak: Soothing Requiem
    static final int SOOTHE_COST = 2000;
    static final int SOOTHE_COOLDOWN = 600;
    static final double SOOTHE_RADIUS = 12.0;
    static final int SOOTHE_TICKS = 200;                // fear and sleep cannot take this long; Regeneration I

    // Passives
    static final float DARK = 0.75f;                    // this dark (0..1) is "full dark" for Nocturnality
    static final int DANGER_SEQUENCE = 9;               // Sleepless: a chime when something hunts you unseen
    static final int DANGER_REST = 60;
    static final int STEADY_SEQUENCE = 8;               // Midnight Poet: falls shorter than this do not hurt
    static final double STEADY_FALL = 6.0;
    static final int RESONANCE_SEQUENCE = 7;            // Nightmare: poems cost less, spirituality comes back faster
    static final float RESONANCE_REFUND = 0.25f;
    static final float RESONANCE_REGEN = 0.25f;
    static final double SPIRIT_SIGHT_RANGE = 16.0;      // Soul Assurer: spirits round you are seen
    static final int VISION_HEALTH_SEQUENCE = 6;        // Soul Assurer: Spirit Vision shows health, and costs
                                                                // no cooldown in the dark
    static final int STEADY_DRAW_EVERY = 4;             // Midnight Poet: every 4th tick of drawing counts twice
    static final int DEEP_NIGHT_SEQUENCE = 4;           // Nightwatcher: Strength II and Regeneration in full dark,
                                                                // Weakness I in bright day sun
    static final int VEIL_STILL = 3;                    // standing still in the dark this many seconds: Unnoticed
    static final int HORROR_SEQUENCE = 3;               // Horror Bishop: the aura
    static final double HORROR_RADIUS = 8.0;            // (halved while sneaking)
    static final float HORROR_FLEE_HEALTH = 40f;        // mobs with at most this much maximum health flee it
    static final int PRESENCE_SEQUENCE = 2;             // Servant of Concealment: not picked in the dark or sneaking;
    static final double PRESENCE_RADIUS = 10.0;         // enemies this close are always slowed
    static final int ARCHANGEL_SEQUENCE = 1;            // Knight of Misfortune: six more hearts...
    static final double ARCHANGEL_HEALTH = 12.0;
    static final float ILL_OMEN_CHANCE = 0.25f;         // ...and whoever hurts you may carry Misfortune
    static final int ILL_OMEN_TICKS = 400;
    static final int SLUMBER_SEQUENCE = 0;              // Darkness: sleep, fear and taunts do not take...
    static final float SLUMBER_RESIST = 0.5f;           // ...supernatural damage is halved...
    static final int HORROR_EVERY = 6;                  // ...and every 6 s whoever looks at you flinches
    static final double HORROR_GAZE_RANGE = 24.0;
    static final float BOSS_HEALTH = 150f;                      // at least this much maximum health: a boss
    static final Identifier ARCHANGEL_HEALTH_ID = Physique.id("knight_of_misfortune_archangel_health");

    // =========================
    // Saved on the player (game times are "until" this tick)
    // =========================
    static final String VISION_READY = "knight_of_misfortune_vision_ready";
    static final String VISION_UNTIL = "knight_of_misfortune_vision_until";
    static final String DOWSE_READY = "knight_of_misfortune_dowse_ready";
    static final String DOWSE_TAG = "knight_of_misfortune_dowse_tag";
    static final String LIMBS_READY = "knight_of_misfortune_limbs_ready";
    static final String LIMBS_UNTIL = "knight_of_misfortune_limbs_until";
    static final String LIMBS_TAG = "knight_of_misfortune_limbs_tag";
    static final String STATE_UNTIL = "knight_of_misfortune_state_until";
    static final String STATE_TAG = "knight_of_misfortune_state_tag";
    static final String BODY_X = "knight_of_misfortune_body_x";
    static final String BODY_Y = "knight_of_misfortune_body_y";
    static final String BODY_Z = "knight_of_misfortune_body_z";
    static final String BODY_THREAT = "knight_of_misfortune_body_threat";
    static final String LULL_TARGET = "knight_of_misfortune_lull_target";
    static final String LULL_COUNT = "knight_of_misfortune_lull_count";
    static final String DREAM_READY = "knight_of_misfortune_dream_ready";
    static final String REQUIEM_READY = "knight_of_misfortune_requiem_ready";
    static final String SOOTHE_READY = "knight_of_misfortune_soothe_ready";
    static final String DANGER_READY = "knight_of_misfortune_danger_ready";
    static final String STILL_AT = "knight_of_misfortune_still_at";       // where they stood last second...
    static final String STILL_FOR = "knight_of_misfortune_still_for";     // ...and how many seconds they have

    // Saved on whoever it touches
    static final String SOOTHED_UNTIL = "knight_of_misfortune_soothed_until";
    static final String SLUMBER_SHOWN = "knight_of_misfortune_slumber_shown";

    // Names of the parts that a player has one of
    static final String LIMBS_ARMOR = "knight_of_misfortune_limbs";
    static final String REQUIEM_ZONE = "knight_of_misfortune_requiem";
    static final String DREAM_MENU = "knight_of_misfortune_dream";

    @Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class KnightOfMisfortuneEvents {

        static {
            // a soothed ally cannot be frightened or put to sleep (Soothing Requiem); Darkness (0) never can
            Slumber.addImmunity(KnightOfMisfortune::isSoothed);
            Fear.addImmunity(KnightOfMisfortune::isSoothed);
            Slumber.addImmunity(KnightOfMisfortune::isEternalSlumber);
            Fear.addImmunity(KnightOfMisfortune::isEternalSlumber);
            // Misfortune: the Knight of Misfortune (1) shrugs it off, bosses carry half, and this Pathway shows it
            Misfortune.addImmunity(entity -> entity instanceof Player player && isKnight(player, ARCHANGEL_SEQUENCE));
            Misfortune.addHalfLevels(KnightOfMisfortune::isBoss);
            Misfortune.addListener((victim, mishap, source) -> {
                if (source == null || !SoulCore.getPathway(source).equals(PATHWAY) || !(victim.level() instanceof ServerLevel sl)) return;
                KnightOfMisfortuneFx.mishap(sl, victim, KnightOfMisfortuneFx.Mishap.valueOf(mishap.name()));
            });
            // what cannot be picked: a Secret Concealment's side, whoever is inside a Concealed World, and a Servant of
            // Concealment (2) in the dark or sneaking (Presence of Concealment)
            Spells.addUnchoosableCheck(KnightOfMisfortuneDarkness::veiled);
            Spells.addUnchoosableCheck((chooser, target) -> target instanceof Player knight && chooser != knight
                    && isKnight(knight, PRESENCE_SEQUENCE) && (knight.isShiftKeyDown() || darkness(knight) >= DARK));
            // Resonance (7): a poem needs no voice
            Silence.addCastsThrough((player, key) -> key == 2 && isKnight(player, RESONANCE_SEQUENCE));
            // whoever wakes from a sleep this Pathway showed loses its look
            Slumber.addWakeListener((sleeper, kind) -> {
                if (!sleeper.getPersistentData().getBooleanOr(SLUMBER_SHOWN, false)) return;
                sleeper.getPersistentData().remove(SLUMBER_SHOWN);
                if (sleeper.level() instanceof ServerLevel sl) KnightOfMisfortuneFx.wake(sl, sleeper, slumberTag(sleeper));
                if (sleeper instanceof Player player) KnightOfMisfortuneFx.statusEnd(player, Status.SLUMBER);
            });
        }

        @SubscribeEvent
        public static void onKnightOfMisfortuneTick(TickEvent.PlayerTickEvent.Post event) {
            Player player = event.player();
            if (!(player.level() instanceof ServerLevel sl)) return;
            // these run out even if the Pathway is taken away in the meantime (or the cast was stolen)
            if (player.tickCount % 20 == 0 && isActive(player, VISION_UNTIL)) visionTick(player, sl);
            limbsTick(player, sl);
            nightmareTick(player, sl);
            KnightOfMisfortuneSpirits.tick(player, sl);
            KnightOfMisfortuneNightwatcher.tick(player, sl);
            KnightOfMisfortuneDarkness.tick(player, sl);
            if (player.tickCount % 20 == 0) {                              // the Archangel's body, on or off
                Physique.keep(player, Attributes.MAX_HEALTH, ARCHANGEL_HEALTH_ID, ARCHANGEL_HEALTH,
                        AttributeModifier.Operation.ADD_VALUE, isKnight(player, ARCHANGEL_SEQUENCE));
            }
        }

        /** Steady Hand: a Midnight Poet draws a bow or a crossbow faster. */
        @SubscribeEvent
        public static void onKnightOfMisfortuneDraw(LivingEntityUseItemEvent.Tick event) {
            if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) return;
            if (!(event.getItem().getItem() instanceof BowItem) && !(event.getItem().getItem() instanceof CrossbowItem)) return;
            if (player.tickCount % STEADY_DRAW_EVERY != 0 || !isKnight(player, STEADY_SEQUENCE)) return;
            if (event.getDuration() > 1) event.setDuration(event.getDuration() - 1);
        }

        /** Ill Omen (1): whoever hurts the Knight may carry Misfortune; at Darkness (0) always, and it is Catastrophe. */
        @SubscribeEvent
        public static void onKnightOfMisfortuneHurt(LivingHurtEvent event) {
            if (!(event.getEntity() instanceof Player knight) || !(knight.level() instanceof ServerLevel sl)) return;
            if (!(event.getSource().getEntity() instanceof LivingEntity attacker) || attacker == knight) return;
            if (!isKnight(knight, ARCHANGEL_SEQUENCE) || Spells.isFriend(knight, attacker)) return;
            boolean darkness = isKnight(knight, SLUMBER_SEQUENCE);
            if (!darkness && knight.getRandom().nextFloat() >= ILL_OMEN_CHANCE) return;
            if (curse(knight, sl, attacker, 1, Misfortune.MAX_LEVEL, ILL_OMEN_TICKS, darkness) > 0) {
                KnightOfMisfortuneFx.illOmen(sl, attacker);
            }
        }

        /** Gale Spirit: hands of wind, no weapon. */
        @SubscribeEvent
        public static boolean onKnightOfMisfortuneSwing(AttackEntityEvent event) {
            Player player = event.getEntity();
            if (player.level().isClientSide() || !KnightOfMisfortuneSpirits.gliding(player)) return false;
            player.sendOverlayMessage(Component.literal("The wind carries you: you cannot strike while it does."));
            return true;
        }

        /** Steady Hand: a Midnight Poet lands a fall shorter than STEADY_FALL unhurt. */
        @SubscribeEvent
        public static boolean onKnightOfMisfortuneFall(LivingFallEvent event) {
            if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) return false;
            return isKnight(player, STEADY_SEQUENCE) && player.fallDistance < STEADY_FALL;
        }

        /** The Sleepless's sense of hidden danger: a mob starts hunting them unseen, or in the dark. */
        @SubscribeEvent
        public static void onKnightOfMisfortuneHunted(LivingChangeTargetEvent event) {
            if (!(event.getNewTarget() instanceof ServerPlayer knight) || event.getEntity().level().isClientSide()) return;
            if (!isKnight(knight, DANGER_SEQUENCE)) return;
            LivingEntity hunter = event.getEntity();
            long now = knight.level().getGameTime();
            if (knight.getPersistentData().getLongOr(DANGER_READY, 0L) > now) return;
            Vec3 toHunter = hunter.position().subtract(knight.position());
            boolean behind = toHunter.horizontalDistanceSqr() > 0.01
                    && Spells.flatLook(knight).dot(new Vec3(toHunter.x, 0, toHunter.z).normalize()) < 0.2;
            boolean unseen = behind || !knight.hasLineOfSight(hunter) || darkness(hunter) >= DARK;
            if (!unseen) return;
            knight.getPersistentData().putLong(DANGER_READY, now + DANGER_REST);
            KnightOfMisfortuneFx.dangerChime(knight);
        }

        // what was on the screen and the body does not survive relogging or dying: start clean
        @SubscribeEvent
        public static void onKnightOfMisfortuneLogin(PlayerEvent.PlayerLoggedInEvent event) {
            Player player = event.getEntity();
            if (player.getPersistentData().contains(BODY_X)) {
                Vec3 body = bodyOf(player);
                player.teleportTo(body.x, body.y, body.z);                         // the soul went home with the body
            }
            clearState(player);
        }

        @SubscribeEvent
        public static void onKnightOfMisfortuneRespawn(PlayerEvent.PlayerRespawnEvent event) {
            clearState(event.getEntity());
        }

        @SubscribeEvent
        public static void onKnightOfMisfortuneLogout(PlayerEvent.PlayerLoggedOutEvent event) {
            Player player = event.getEntity();
            if (player.getPersistentData().contains(BODY_X)) {                    // logged out as a soul: back in the body
                Vec3 body = bodyOf(player);
                player.teleportTo(body.x, body.y, body.z);
                Concealment.reveal(player);
                Flight.end(player);
                clearNightmareKeys(player);
            }
            KnightOfMisfortuneSpirits.forget(player);
        }
    }

    // =========================
    // Ability 1 - Night Sight
    // Normal: Spirit Vision. For VISION_TICKS every living thing within VISION_RANGE (VISION_DARK_RANGE in the dark)
    //         is marked once a second, for you alone; spirits, summons and the undead in another color.
    // Sneak:  Dowsing. The pendulum swings toward a mob that hunts you, else whoever last hurt you, else the nearest
    //         Beyonder; a line of light points the way and chat says how far. It costs nothing when it finds nothing.
    // =========================
    public static void knightOfMisfortuneNightSight(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!ready(player, bypassClassCheck, SIGHT_SEQUENCE)) return;

        if (player.isShiftKeyDown()) {
            dowsing(player, sl);
            return;
        }
        if (!(player instanceof ServerPlayer viewer)) return;
        boolean freeInDark = isKnight(player, VISION_HEALTH_SEQUENCE) && darkness(player) >= DARK;
        if (!freeInDark && coolingDown(player, sl, VISION_READY, "Spirit Vision")) return;
        if (!Spells.paySpirituality(player, VISION_COST)) return;
        player.getPersistentData().putLong(VISION_READY, sl.getGameTime() + VISION_COOLDOWN);
        player.getPersistentData().putLong(VISION_UNTIL, sl.getGameTime() + VISION_TICKS);
        KnightOfMisfortuneFx.spiritVision(viewer, VISION_TICKS);
        KnightOfMisfortuneFx.status(player, Status.SPIRIT_VISION, VISION_TICKS);
        visionTick(player, sl);
    }

    /** Once a second while Spirit Vision lasts. */
    private static void visionTick(Player player, ServerLevel sl) {
        if (!(player instanceof ServerPlayer viewer)) return;
        double range = darkness(player) >= DARK ? VISION_DARK_RANGE : VISION_RANGE;
        for (LivingEntity seen : Spells.livingAround(player, sl, player.position(), range)) {
            KnightOfMisfortuneFx.spiritVisionMark(viewer, seen, isSpirit(seen));
        }
        for (Vec3 trap : Trap.hiddenNear(sl, player.position(), range)) KnightOfMisfortuneFx.spiritVisionTrap(viewer, trap);
        if (!Beyonder.hasSequence(player, VISION_HEALTH_SEQUENCE)) return;
        LivingEntity looked = Spells.aimEnemy(player, sl, range);           // Soul Assurer: its health and its spirit
        if (looked == null) return;
        String state = Slumber.kind(looked) != null ? ", asleep" : Fear.isAfraid(looked) ? ", afraid" : "";
        player.sendOverlayMessage(Component.literal(looked.getName().getString() + ": " + Math.round(looked.getHealth())
                + " / " + Math.round(looked.getMaxHealth()) + state));
    }

    private static void dowsing(Player player, ServerLevel sl) {
        if (coolingDown(player, sl, DOWSE_READY, "Dowsing")) return;
        String old = player.getPersistentData().getStringOr(DOWSE_TAG, "");
        if (!old.isEmpty()) KnightOfMisfortuneFx.dowsingEnd(sl, player, old);
        String tag = "km_dowse_" + player.getId() + "_" + sl.getGameTime();
        player.getPersistentData().putString(DOWSE_TAG, tag);

        LivingEntity quarry = dowse(player, sl);
        if (quarry == null) {
            KnightOfMisfortuneFx.dowsing(sl, player, null, 60, tag);                  // free: it found nothing
            player.sendSystemMessage(Component.literal("The pendulum swings, and settles on nothing."));
            return;
        }
        if (!Spells.paySpirituality(player, DOWSE_COST)) return;
        player.getPersistentData().putLong(DOWSE_READY, sl.getGameTime() + DOWSE_COOLDOWN);
        KnightOfMisfortuneFx.dowsing(sl, player, quarry.position(), DOWSE_TICKS, tag);
        KnightOfMisfortuneFx.status(player, Status.DOWSING, DOWSE_TICKS);
        int blocks = (int) Math.round(player.distanceTo(quarry));
        player.sendSystemMessage(Component.literal("The pendulum points to " + quarry.getName().getString()
                + ", " + blocks + " blocks away."));
    }

    /** Who the pendulum finds: a mob hunting you, else who last hurt you, else the nearest other Beyonder. */
    private static LivingEntity dowse(Player player, ServerLevel sl) {
        List<Mob> hunters = sl.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(DOWSE_HUNTER_RANGE),
                mob -> mob.isAlive() && mob.getTarget() == player);
        if (!hunters.isEmpty()) {
            hunters.sort(Comparator.comparingDouble(mob -> mob.distanceToSqr(player)));
            return hunters.get(0);
        }
        LivingEntity attacker = player.getLastHurtByMob();
        if (attacker != null && attacker.isAlive() && attacker.level() == sl && attacker.distanceTo(player) <= DOWSE_RANGE
                && player.tickCount - player.getLastHurtByMobTimestamp() <= DOWSE_MEMORY && !Concealment.isHidden(attacker)) {
            return attacker;
        }
        Player nearest = null;
        double best = DOWSE_RANGE * DOWSE_RANGE;
        for (Player other : sl.players()) {
            if (other == player || !other.isAlive() || Concealment.isHidden(other)) continue;
            String pathway = SoulCore.getPathway(other);
            if (pathway == null || pathway.equals("none")) continue;
            double d = other.distanceToSqr(player);
            if (d < best) {
                best = d;
                nearest = other;
            }
        }
        return nearest;
    }

    // =========================
    // Ability 3 - Nightmare
    // Normal: Nightmare Limbs. LIMBS_COUNT tentacles grow out of your back for LIMBS_TICKS; once a second each lashes
    //         one of the nearest enemies within LIMBS_REACH (LIMBS_DAMAGE times the power level). They are your own
    //         flesh: a layer of absorption on you is theirs, and when it is gone they tear off and you take a
    //         backlash (LIMBS_BACKLASH of your maximum health). Cast again to draw them back in.
    // Sneak:  Nightmare State. Your soul steps out (hidden, untouchable, floating) for STATE_TICKS at most and
    //         STATE_UPKEEP a second, within reach of your body (it grows with the Sequence). Enemies beside the body
    //         for BODY_THREAT_SECONDS wreck it. Sneak again to go back. Inside it, the normal cast is Dream Invasion:
    //         aimed at a sleeper, its dream is entered (it is held DREAM_TICKS) and you pick what to make of it
    //         (Dream Shaping): Secrets, Terror or Obedience. A mob you watch unseen for LULL_TICKS drifts off to sleep.
    // =========================
    public static void knightOfMisfortuneNightmare(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!ready(player, bypassClassCheck, NIGHTMARE_SEQUENCE)) return;

        if (inNightmare(player)) {
            if (player.isShiftKeyDown()) endNightmare(player, sl, false);
            else dreamInvasion(player, sl);
            return;
        }
        if (player.isShiftKeyDown()) {
            if (Root.has(player, RootRestriction.MOVE)) {
                player.sendSystemMessage(Component.literal("You are held fast: your soul cannot slip out."));
                return;
            }
            if (!Spells.paySpirituality(player, STATE_COST)) return;
            nightmareState(player, sl);
            return;
        }
        if (isActive(player, LIMBS_UNTIL)) {                                // cast again: draw them back in (free)
            retractLimbs(player, sl);
            return;
        }
        if (coolingDown(player, sl, LIMBS_READY, "Nightmare Limbs")) return;
        if (!Spells.paySpirituality(player, LIMBS_COST)) return;
        player.getPersistentData().putLong(LIMBS_READY, sl.getGameTime() + LIMBS_COOLDOWN);
        nightmareLimbs(player, sl);
    }

    // ---------- Nightmare Limbs

    static void nightmareLimbs(LivingEntity player, ServerLevel sl) {
        CompoundTag data = player.getPersistentData();
        String tag = "km_limbs_" + player.getId() + "_" + sl.getGameTime();
        data.putLong(LIMBS_UNTIL, sl.getGameTime() + LIMBS_TICKS);
        data.putString(LIMBS_TAG, tag);
        ConjuredArmor.of(LIMBS_ARMOR).lasts(LIMBS_TICKS)
                .absorption(LIMBS_LAYER + LIMBS_LAYER_PER_LEVEL * Beyonder.powerLevel(player))
                .wear(player);
        KnightOfMisfortuneFx.nightmareLimbs(sl, player, LIMBS_TICKS, tag);
        KnightOfMisfortuneFx.status(player, Status.LIMBS, LIMBS_TICKS);
    }

    /** Every tick: the lashes once a second, the limbs torn off when their layer is gone, the end of them. */
    static void limbsTick(LivingEntity player, ServerLevel sl) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(LIMBS_TAG)) return;
        if (!isActive(player, LIMBS_UNTIL)) {                             // their time is up
            data.remove(LIMBS_UNTIL);
            data.remove(LIMBS_TAG);
            return;
        }
        if (player.tickCount % 5 == 0 && player.getAbsorptionAmount() <= 0.01f) {
            tearLimbs(player, sl);
            return;
        }
        if (player.tickCount % 20 != 0) return;
        List<LivingEntity> near = Spells.enemiesAround(player, sl, player.position(), LIMBS_REACH);
        if (near.isEmpty()) return;
        near.sort(Comparator.comparingDouble(enemy -> enemy.distanceToSqr(player)));
        float damage = LIMBS_DAMAGE * Beyonder.powerLevel(player);
        for (int i = 0; i < LIMBS_COUNT; i++) {
            LivingEntity target = near.get(i % near.size());
            int index = i;
            Later.run(sl, i * 4, () -> {
                if (!Spells.casterStillHere(player, sl) || !target.isAlive() || !isActive(player, LIMBS_UNTIL)) return;
                Spells.strike(player, target, damage);
                KnightOfMisfortuneFx.limbLash(sl, player, target, index);
            });
        }
    }

    private static void retractLimbs(LivingEntity player, ServerLevel sl) {
        CompoundTag data = player.getPersistentData();
        String tag = data.getStringOr(LIMBS_TAG, "");
        data.remove(LIMBS_UNTIL);
        data.remove(LIMBS_TAG);
        ConjuredArmor.takeOff(player, LIMBS_ARMOR);
        if (!tag.isEmpty()) KnightOfMisfortuneFx.limbsRetract(sl, player, tag);
        KnightOfMisfortuneFx.statusEnd(player, Status.LIMBS);
    }

    private static void tearLimbs(LivingEntity player, ServerLevel sl) {
        String tag = player.getPersistentData().getStringOr(LIMBS_TAG, "");
        retractLimbs(player, sl);
        KnightOfMisfortuneFx.limbsTear(sl, player, tag);
        player.setHealth(Math.max(1f, player.getHealth() - player.getMaxHealth() * LIMBS_BACKLASH));
        Spells.tell(player, Component.literal("Your Nightmare Limbs are torn off."));
    }

    // ---------- Nightmare State

    static boolean inNightmare(LivingEntity player) {
        return isActive(player, STATE_UNTIL) && player.getPersistentData().contains(BODY_X);
    }

    private static void nightmareState(Player player, ServerLevel sl) {
        CompoundTag data = player.getPersistentData();
        Vec3 body = player.position();
        String tag = "km_soul_" + player.getId() + "_" + sl.getGameTime();
        data.putLong(STATE_UNTIL, sl.getGameTime() + STATE_TICKS);
        data.putString(STATE_TAG, tag);
        data.putDouble(BODY_X, body.x);
        data.putDouble(BODY_Y, body.y);
        data.putDouble(BODY_Z, body.z);
        data.putInt(BODY_THREAT, 0);
        data.remove(LULL_TARGET);
        data.putInt(LULL_COUNT, 0);
        Concealment.hide(player, STATE_TICKS);
        Flight.grant(player, STATE_TICKS);
        KnightOfMisfortuneFx.nightmareState(sl, player, STATE_TICKS, tag);
        KnightOfMisfortuneFx.sleepingBody(sl, body, player.getYRot(), STATE_TICKS, tag);
        KnightOfMisfortuneFx.status(player, Status.NIGHTMARE_STATE, STATE_TICKS);
        player.sendSystemMessage(Component.literal("Your soul steps out of your body. Sneak + the key to go back."));
    }

    /** Every tick while the soul is out: the leash, the upkeep, the body, the lulling, the end. */
    private static void nightmareTick(Player player, ServerLevel sl) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(BODY_X)) return;
        if (!isActive(player, STATE_UNTIL)) {
            endNightmare(player, sl, false);
            return;
        }
        Vec3 body = bodyOf(player);
        if (player.tickCount % 6 == 0) KnightOfMisfortuneFx.nightmareTick(sl, player);
        if (player.tickCount % 5 == 0) {
            double reach = stateReach(player);
            Vec3 offset = player.position().subtract(body);
            if (offset.length() > reach) {                                // the thread to the body pulls the soul back
                Spells.push(player, player.position().add(offset.normalize()), 0.8, 0.0);
                if (offset.length() > reach + 8) player.teleportTo(body.x, body.y, body.z);
            }
            lull(player, sl);
        }
        if (player.tickCount % 20 != 0) return;
        if (!Concealment.isHidden(player)) {                               // it struck something: the body calls it back
            endNightmare(player, sl, false);
            return;
        }
        if (!Spells.paySpirituality(player, STATE_UPKEEP)) {
            endNightmare(player, sl, false);
            return;
        }
        boolean threatened = false;
        for (LivingEntity near : Spells.enemiesAround(player, sl, body.add(0, 0.5, 0), BODY_THREAT_RANGE)) {
            if (near instanceof Enemy || near instanceof Player) {
                threatened = true;
                break;
            }
        }
        int threat = threatened ? data.getIntOr(BODY_THREAT, 0) + 1 : 0;
        data.putInt(BODY_THREAT, threat);
        if (threat == 1) player.sendSystemMessage(Component.literal("Something stands over your sleeping body."));
        if (threat >= BODY_THREAT_SECONDS) endNightmare(player, sl, true);
    }

    /** A mob the soul watches unseen, long enough, drifts off to sleep. */
    private static void lull(LivingEntity player, ServerLevel sl) {
        CompoundTag data = player.getPersistentData();
        LivingEntity watched = Spells.aimEnemy(player, sl, dreamReach(player));
        if (watched == null || isBoss(watched) || Slumber.kind(watched) != null) {
            data.remove(LULL_TARGET);
            data.putInt(LULL_COUNT, 0);
            return;
        }
        String id = watched.getUUID().toString();
        int count = id.equals(data.getStringOr(LULL_TARGET, "")) ? data.getIntOr(LULL_COUNT, 0) + 5 : 5;
        data.putString(LULL_TARGET, id);
        data.putInt(LULL_COUNT, count);
        if (count < LULL_TICKS) return;
        data.remove(LULL_TARGET);
        data.putInt(LULL_COUNT, 0);
        if (Slumber.put(watched, LULL_SLEEP, Slumber.Kind.SLEEP)) {
            showSlumber(sl, watched, LULL_SLEEP);
            Spells.tell(player, Component.literal(watched.getName().getString() + " drifts off to sleep."));
        }
    }

    /** The soul goes back into its body (wrecked = the body was set upon: it hurts). */
    private static void endNightmare(Player player, ServerLevel sl, boolean wrecked) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(BODY_X)) return;
        Vec3 body = bodyOf(player);
        String tag = data.getStringOr(STATE_TAG, "");
        clearNightmareKeys(player);
        player.teleportTo(body.x, body.y, body.z);
        Concealment.reveal(player);
        Flight.end(player);
        FallGuard.protect(player, 40);
        KnightOfMisfortuneFx.nightmareReturn(sl, player, body, tag);
        KnightOfMisfortuneFx.statusEnd(player, Status.NIGHTMARE_STATE);
        if (wrecked) {
            player.setHealth(Math.max(1f, player.getHealth() - player.getMaxHealth() * BODY_WRECK));
            player.sendSystemMessage(Component.literal("Your sleeping body was set upon: your soul is torn back into it."));
        }
    }

    private static void clearNightmareKeys(LivingEntity player) {
        CompoundTag data = player.getPersistentData();
        for (String key : List.of(STATE_UNTIL, STATE_TAG, BODY_X, BODY_Y, BODY_Z, BODY_THREAT, LULL_TARGET, LULL_COUNT)) {
            data.remove(key);
        }
    }

    private static Vec3 bodyOf(LivingEntity player) {
        CompoundTag data = player.getPersistentData();
        return new Vec3(data.getDoubleOr(BODY_X, player.getX()), data.getDoubleOr(BODY_Y, player.getY()),
                data.getDoubleOr(BODY_Z, player.getZ()));
    }

    /** How far the soul may wander from its body: 48 / 72 / 96 / 250 blocks at Sequence 7 / 6 / 5 / 4. */
    private static double stateReach(LivingEntity player) {
        if (Beyonder.hasSequence(player, 4)) return 250;
        if (Beyonder.hasSequence(player, 5)) return 96;
        if (Beyonder.hasSequence(player, 6)) return 72;
        return 48;
    }

    /** How far a dream can be entered: 24 / 36 / 48 / 96 blocks at Sequence 7 / 6 / 5 / 4. */
    private static double dreamReach(LivingEntity player) {
        if (Beyonder.hasSequence(player, 4)) return 96;
        if (Beyonder.hasSequence(player, 5)) return 48;
        if (Beyonder.hasSequence(player, 6)) return 36;
        return 24;
    }

    // ---------- Dream Invasion and Dream Shaping

    private static void dreamInvasion(Player player, ServerLevel sl) {
        if (coolingDown(player, sl, DREAM_READY, "Dream Invasion")) return;
        LivingEntity dreamer = Spells.aimEnemy(player, sl, dreamReach(player));
        if (dreamer == null) {
            player.sendSystemMessage(Component.literal("No dream lies before you."));
            return;
        }
        boolean sleeping = Slumber.kind(dreamer) != null || (dreamer instanceof Player sleeper && sleeper.isSleeping());
        if (!sleeping) {
            player.sendSystemMessage(Component.literal(dreamer.getName().getString()
                    + " is awake. Watch it unseen a while and it will drift off."));
            return;
        }
        if (!Spells.paySpirituality(player, DREAM_COST)) return;
        player.getPersistentData().putLong(DREAM_READY, sl.getGameTime() + DREAM_COOLDOWN);
        String tag = "km_dream_" + dreamer.getId() + "_" + sl.getGameTime();
        if (Slumber.put(dreamer, DREAM_TICKS, Slumber.Kind.DEEP)) showSlumber(sl, dreamer, DREAM_TICKS);
        KnightOfMisfortuneFx.dreamInvade(sl, player, dreamer, DREAM_TICKS, tag);
        KnightOfMisfortuneFx.status(player, Status.DREAM, DREAM_TICKS);
        boolean[] shaped = {false};
        Later.run(sl, KnightOfMisfortuneFx.DREAM_OPENS, () -> {
            if (!Spells.casterStillHere(player, sl) || !dreamer.isAlive()) return;
            openDreamMenu(player, dreamer, tag, shaped);
        });
        Later.run(sl, KnightOfMisfortuneFx.DREAM_OPENS + DREAM_PICK_TICKS, () -> {
            if (!shaped[0] && Spells.casterStillHere(player, sl) && dreamer.isAlive()) {
                shapeDream(player, sl, dreamer, Dream.SECRETS, tag, shaped);
            }
        });
    }

    private static void openDreamMenu(Player player, LivingEntity dreamer, String tag, boolean[] shaped) {
        AbilityMenu menu = AbilityMenu.create(DREAM_MENU).title("Shape the dream of " + dreamer.getName().getString()).ring(56);
        menu.option(KnightOfMisfortuneFx.WHEEL_SECRET_CONCEALMENT, "Secrets", 0, 0,
                        (picker, level) -> shapeDream(picker, level, dreamer, Dream.SECRETS, tag, shaped))
                .details(viewer -> "what it hides");
        menu.option(KnightOfMisfortuneFx.WHEEL_FEAR, "Terror", 0, 0,
                        (picker, level) -> shapeDream(picker, level, dreamer, Dream.TERROR, tag, shaped))
                .details(viewer -> "break its spirit");
        menu.option(KnightOfMisfortuneFx.WHEEL_COMMAND_SPIRITS, "Obedience", 0, 0,
                        (picker, level) -> shapeDream(picker, level, dreamer, Dream.OBEDIENCE, tag, shaped))
                .details(viewer -> "it wakes as yours");
        menu.open(player);
    }

    private static void shapeDream(Player player, ServerLevel sl, LivingEntity dreamer, Dream dream, String tag, boolean[] shaped) {
        if (shaped[0] || !dreamer.isAlive() || dreamer.level() != sl) return;
        shaped[0] = true;
        KnightOfMisfortuneFx.dreamShape(sl, dreamer, dream);
        switch (dream) {
            case SECRETS -> tellSecrets(player, dreamer);
            case TERROR -> {
                Slumber.wake(dreamer);
                Spells.strike(player, dreamer, dreamer.getHealth() * TERROR_SHARE);
                if (Fear.apply(dreamer, player, TERROR_FEAR)) showFear(sl, dreamer, TERROR_FEAR);
                player.sendSystemMessage(Component.literal("The nightmare breaks " + dreamer.getName().getString() + "'s spirit."));
            }
            case OBEDIENCE -> {
                Slumber.wake(dreamer);
                if (dreamer instanceof Mob mob) {
                    float limit = OBEDIENCE_HEALTH + OBEDIENCE_HEALTH_PER_SEQUENCE * Math.max(0, 7 - SoulCore.getSequence(player));
                    if (mob.getMaxHealth() <= limit && !isBoss(mob) && Marionette.control(mob, player, OBEDIENCE_TICKS)) {
                        player.sendSystemMessage(Component.literal(mob.getName().getString() + " wakes as yours."));
                    } else {
                        player.sendSystemMessage(Component.literal(mob.getName().getString() + "'s will is too strong to bend."));
                    }
                } else if (Slumber.put(dreamer, OBEDIENCE_LIMP, Slumber.Kind.LIMP)) {
                    showSlumber(sl, dreamer, OBEDIENCE_LIMP);
                    player.sendSystemMessage(Component.literal(dreamer.getName().getString() + " wakes without the will to fight."));
                }
            }
        }
        Later.run(sl, 30, () -> KnightOfMisfortuneFx.dreamEnd(sl, dreamer, tag));
    }

    /** Secrets: what the dream gives away, in chat. */
    private static void tellSecrets(LivingEntity player, LivingEntity dreamer) {
        String name = dreamer.getName().getString();
        Spells.tell(player, Component.literal("In the dream of " + name + ":"));
        if (dreamer instanceof Player other) {
            String pathway = SoulCore.getPathway(other);
            if (pathway == null || pathway.equals("none")) {
                Spells.tell(player, Component.literal("  no Beyonder power at all."));
            } else {
                int percent = Math.round(100f * SoulCore.getSpirituality(other) / Math.max(1f, Beyonder.maxSpirituality(other)));
                Spells.tell(player, Component.literal("  the " + pathway + " Pathway, Sequence " + SoulCore.getSequence(other)
                        + ", spirituality " + percent + "%."));
            }
        } else if (Beyonders.isBeyonder(dreamer)) {                          // a Beyonder mob (blasphemy_slate/beyonder_mobs)
            Spells.tell(player, Component.literal("  the " + Beyonders.pathwayOf(dreamer) + " Pathway, Sequence "
                    + Beyonders.sequenceOf(dreamer) + "."));
        }
        Spells.tell(player, Component.literal("  health " + Math.round(dreamer.getHealth()) + " of " + Math.round(dreamer.getMaxHealth()) + "."));
        ItemStack held = dreamer.getItemBySlot(EquipmentSlot.MAINHAND);
        if (!held.isEmpty()) Spells.tell(player, Component.literal("  it holds " + held.getHoverName().getString() + "."));
        if (GiantForm.isGiant(dreamer)) {
            Spells.tell(player, Component.literal("  its madness: " + Math.round(GiantForm.madness(dreamer) * 100) + "%."));
        }
    }

    // =========================
    // Ability 4 - Requiem
    // Normal: a field of lullaby REQUIEM_RADIUS round where you stand, for REQUIEM_TICKS once it is sung. Enemies in
    //         it slow more the longer they stay and after REQUIEM_FREEZE are frozen still; they are silenced while in
    //         it; ghosts, spirits, summons and the undead are held outright. From Nightwatcher (4) whoever stays
    //         REQUIEM_SLEEP_AFTER falls asleep.
    // Sneak:  Soothing Requiem. You and your allies within SOOTHE_RADIUS are cleansed and calmed (fear, sleep and taunts
    //         end and cannot take for SOOTHE_TICKS), regenerate, and your own marionettes and summons are healed full.
    // =========================
    public static void knightOfMisfortuneRequiem(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!ready(player, bypassClassCheck, REQUIEM_SEQUENCE)) return;

        if (player.isShiftKeyDown()) {
            if (coolingDown(player, sl, SOOTHE_READY, "Soothing Requiem")) return;
            if (!Spells.paySpirituality(player, SOOTHE_COST)) return;
            player.getPersistentData().putLong(SOOTHE_READY, sl.getGameTime() + SOOTHE_COOLDOWN);
            soothingRequiem(player, sl);
            return;
        }
        if (coolingDown(player, sl, REQUIEM_READY, "Requiem")) return;
        if (!Spells.paySpirituality(player, REQUIEM_COST)) return;
        player.getPersistentData().putLong(REQUIEM_READY, sl.getGameTime() + REQUIEM_COOLDOWN);
        requiem(player, sl);
    }

    static void requiem(LivingEntity player, ServerLevel sl) {
        Vec3 ground = Spells.groundUnder(player, sl, player);
        String tag = "km_requiem_" + player.getId() + "_" + sl.getGameTime();
        int risen = KnightOfMisfortuneFx.REQUIEM_RISEN;
        KnightOfMisfortuneFx.requiem(sl, ground, REQUIEM_RADIUS, REQUIEM_TICKS + risen, tag);
        KnightOfMisfortuneFx.status(player, Status.REQUIEM, REQUIEM_TICKS + risen);
        Later.run(sl, risen, () -> {
            if (!Spells.casterStillHere(player, sl)) {
                KnightOfMisfortuneFx.requiemEnd(sl, ground, tag);
                return;
            }
            Zone.at(ground, REQUIEM_RADIUS).height(4).lasts(REQUIEM_TICKS).every(20)
                    .onInside((enemy, inside) -> requiemTouches(player, sl, enemy, inside))
                    .onPulse(left -> KnightOfMisfortuneFx.requiemPulse(sl, ground, REQUIEM_RADIUS))
                    .onEnd(() -> KnightOfMisfortuneFx.requiemEnd(sl, ground, tag))
                    .open(player, sl, REQUIEM_ZONE);
        });
    }

    /** Once a second for each enemy in the requiem, 'inside' = how long it has been in it. */
    private static void requiemTouches(LivingEntity player, ServerLevel sl, LivingEntity enemy, int inside) {
        float depth = Math.min(1f, inside / (float) REQUIEM_FREEZE);
        Spells.keepEffect(enemy, MobEffects.SLOWNESS, Math.min(3, (int) (depth * 4)), 40);
        Silence.apply(enemy, 30);
        if (isSpirit(enemy)) {
            Root.apply(enemy, 30, RootRestriction.EVERYTHING);              // the dead are laid to rest at once
        } else if (depth >= 1f) {
            Root.apply(enemy, 30, RootRestriction.MOVEMENT);
        }
        if (isKnight(player, REQUIEM_SLEEP_SEQUENCE) && inside >= REQUIEM_SLEEP_AFTER && !isBoss(enemy)
                && Slumber.kind(enemy) == null && Slumber.put(enemy, 60, Slumber.Kind.SLEEP)) {
            showSlumber(sl, enemy, 60);
        }
        KnightOfMisfortuneFx.requiemOn(sl, enemy, depth);
    }

    static void soothingRequiem(LivingEntity player, ServerLevel sl) {
        long until = sl.getGameTime() + SOOTHE_TICKS;
        KnightOfMisfortuneFx.soothingRequiem(sl, player, SOOTHE_RADIUS);
        List<LivingEntity> soothed = new ArrayList<>(Spells.alliesAround(player, sl, player.position(), SOOTHE_RADIUS));
        soothed.add(player);
        for (LivingEntity ally : soothed) {
            Spells.cleanse(ally);
            Fear.end(ally);
            Slumber.wake(ally);
            if (ally instanceof Mob mob) Taunt.remove(mob);
            ally.getPersistentData().putLong(SOOTHED_UNTIL, until);
            ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, SOOTHE_TICKS, 0));
            if (Marionette.isMarionetteOf(ally, player)) ally.setHealth(ally.getMaxHealth());
            KnightOfMisfortuneFx.soothed(sl, ally);
            if (ally instanceof Player soothedPlayer) KnightOfMisfortuneFx.status(soothedPlayer, Status.SOOTHING, SOOTHE_TICKS);
        }
    }

    static boolean isSoothed(LivingEntity entity) {
        return entity.getPersistentData().getLongOr(SOOTHED_UNTIL, 0L) > entity.level().getGameTime();
    }

    // =========================
    // Passives (called once a second from SequencePassives)
    // =========================

    /**
     * Sleepless (9): Nocturnality. In full dark: Strength I, Speed I and wounds close a little. Nightwatcher (4), Deep
     * Night: Strength II and Regeneration I in full dark.
     */
    public static void nocturnality(LivingEntity player) {
        if (darkness(player) < DARK) return;
        boolean deep = isKnight(player, DEEP_NIGHT_SEQUENCE);
        Spells.keepEffect(player, MobEffects.STRENGTH, deep ? 1 : 0, 60);
        Spells.keepEffect(player, MobEffects.SPEED, 0, 60);
        if (deep) Spells.keepEffect(player, MobEffects.REGENERATION, 0, 60);
        if (player.getHealth() < player.getMaxHealth()) player.heal(0.5f);
        if (player.level() instanceof ServerLevel sl && player.getRandom().nextInt(3) == 0) KnightOfMisfortuneFx.nocturnality(sl, player);
    }

    /** Nightmare (7): Resonance. Spirituality comes back RESONANCE_REGEN faster. */
    public static void resonance(Player player) {
        float max = Beyonder.maxSpirituality(player);
        float now = SoulCore.getSpirituality(player);
        if (now >= max) return;
        float extra = Beyonder.spiritualityRegenPerTick(player) * 20f * RESONANCE_REGEN;
        SoulCore.setSpiritualityQuietly(player, Math.min(max, now + extra));
    }

    /** Soul Assurer (6): Spirit Sight. Spirits, summons, the undead and sleepers round you are marked for you alone. */
    public static void spiritSight(LivingEntity player) {
        if (!(player instanceof ServerPlayer viewer) || !(player.level() instanceof ServerLevel sl)) return;
        if (isActive(player, VISION_UNTIL)) return;                        // Spirit Vision shows them already
        for (LivingEntity seen : Spells.livingAround(player, sl, player.position(), SPIRIT_SIGHT_RANGE)) {
            if (isSpirit(seen) || Slumber.kind(seen) != null) KnightOfMisfortuneFx.spiritVisionMark(viewer, seen, true);
        }
    }

    /**
     * Nightwatcher (4), Deep Night: bright day sun under the open sky weakens you (Weakness I; a Night Domain lifts it).
     * Veil: standing still in the dark for VEIL_STILL seconds hides your mind (status/Unnoticed) until you move.
     */
    public static void deepNight(LivingEntity player) {
        if (Nocturnality.inDaySun(player)) Spells.keepEffect(player, MobEffects.WEAKNESS, 0, 40);
        CompoundTag data = player.getPersistentData();
        long here = player.blockPosition().asLong();
        int still = data.getLongOr(STILL_AT, Long.MIN_VALUE) == here && darkness(player) >= DARK ? data.getIntOr(STILL_FOR, 0) + 1 : 0;
        data.putLong(STILL_AT, here);
        data.putInt(STILL_FOR, still);
        if (still >= VEIL_STILL) Unnoticed.hide(player, 30);
    }

    /**
     * Horror Bishop (3), Horror Aura: every enemy weaker than you within HORROR_RADIUS (half while sneaking) trembles.
     * Mobs of little health flee you; weaker players are slowed, and sick with it when they look at you.
     */
    public static void horrorAura(Player player) {
        if (!(player.level() instanceof ServerLevel sl) || inNightmare(player)) return;
        double radius = player.isShiftKeyDown() ? HORROR_RADIUS / 2 : HORROR_RADIUS;
        KnightOfMisfortuneFx.horrorAura(sl, player, radius);
        int mine = SoulCore.getSequence(player);
        for (LivingEntity enemy : Spells.enemiesAround(player, sl, player.position(), radius)) {
            if (enemy instanceof Player other) {
                String pathway = SoulCore.getPathway(other);
                boolean weaker = pathway == null || pathway.equals("none") || SoulCore.getSequence(other) > mine;
                if (!weaker || isEternalSlumber(other)) continue;
                boolean looking = looksAt(other, player);
                Spells.keepEffect(other, MobEffects.SLOWNESS, looking ? 1 : 0, 40);
                if (looking) Spells.keepEffect(other, MobEffects.NAUSEA, 0, 100);
            } else if (enemy.getMaxHealth() <= HORROR_FLEE_HEALTH && !isBoss(enemy)) {
                boolean already = Fear.isAfraid(enemy);
                if (Fear.apply(enemy, player, 40) && !already) KnightOfMisfortuneFx.horrorFlinch(sl, enemy);
            }
        }
    }

    /** Servant of Concealment (2), the Requiem's presence: enemies within PRESENCE_RADIUS are always slowed, cannot sprint. */
    public static void requiemPresence(LivingEntity player) {
        if (!(player.level() instanceof ServerLevel sl)) return;
        for (LivingEntity enemy : Spells.enemiesAround(player, sl, player.position(), PRESENCE_RADIUS)) {
            if (isEternalSlumber(enemy)) continue;
            Spells.keepEffect(enemy, MobEffects.SLOWNESS, 0, 40);
            if (enemy instanceof Player other) other.setSprinting(false);
        }
    }

    /**
     * Darkness (0), Eternal Slumber: supernatural damage is halved and blindness and nausea do not take
     * (status/SupernaturalResistance), nor do taunts; Slumber and Fear never do (their immunities, above). Horror: every
     * HORROR_EVERY seconds, every enemy within HORROR_GAZE_RANGE that looks at you flinches: its next second of acting
     * is cancelled.
     */
    public static void eternalSlumber(LivingEntity player) {
        SupernaturalResistance.grant(player, "knight_of_misfortune_eternal_slumber", 40, SLUMBER_RESIST, false, null);
        Taunt.remove(player);
        if (player.tickCount % (HORROR_EVERY * 20) >= 20 || !(player.level() instanceof ServerLevel sl)) return;
        for (LivingEntity enemy : Spells.enemiesAround(player, sl, player.position(), HORROR_GAZE_RANGE)) {
            if (!looksAt(enemy, player)) continue;
            Root.apply(enemy, 20, RootRestriction.ATTACK, RootRestriction.ABILITIES, RootRestriction.USE_ITEMS);
            if (enemy instanceof Mob mob && mob.getTarget() == player) mob.setTarget(null);
            KnightOfMisfortuneFx.horrorFlinch(sl, enemy);
        }
    }

    // =========================
    // Helpers shared by the abilities (also KnightOfMisfortunePoems, ...Spirits, ...Nightwatcher and ...Darkness)
    // =========================

    /** How dark it is where it stands: 0 = full light, 1 = pitch black (status/Nocturnality: forced night counts). */
    static float darkness(LivingEntity entity) {
        return Nocturnality.darkness(entity);
    }

    /** True when 'viewer' looks at 'seen' (near the middle of its sight, nothing in between). */
    static boolean looksAt(LivingEntity viewer, LivingEntity seen) {
        Vec3 to = seen.getEyePosition().subtract(viewer.getEyePosition());
        if (to.lengthSqr() < 1.0E-4) return false;
        return viewer.getLookAngle().dot(to.normalize()) > 0.9 && viewer.hasLineOfSight(seen);
    }

    /** A Darkness (0) of this Pathway: what this Pathway puts on others does not take on them. */
    static boolean isEternalSlumber(LivingEntity entity) {
        return entity instanceof Player player && isKnight(player, SLUMBER_SEQUENCE);
    }

    /**
     * Misfortune from this Pathway: 'levels' more (up to 'max') for 'ticks', its mark over the head and its picture,
     * if it is a player. Never on the caster's side. Returns its level (0 = it took none).
     */
    static int curse(LivingEntity player, ServerLevel sl, LivingEntity target, int levels, int max, int ticks, boolean catastrophe) {
        if (Spells.isFriend(player, target)) return 0;
        int level = Misfortune.add(target, player, levels, max, ticks, catastrophe);
        if (level <= 0) return 0;
        KnightOfMisfortuneFx.misfortune(sl, target, level, ticks, "km_misfortune_" + target.getId());
        if (target instanceof Player cursed) KnightOfMisfortuneFx.status(cursed, Status.MISFORTUNE, ticks);
        return level;
    }

    /** A spirit to this Pathway: the undead, a summon, a marionette. */
    static boolean isSpirit(LivingEntity entity) {
        return entity.is(EntityTypeTags.UNDEAD) || Marionette.isSummoned(entity) || Marionette.isMarionette(entity);
    }

    static boolean isBoss(LivingEntity entity) {
        return entity.getMaxHealth() >= BOSS_HEALTH;
    }

    /** Puts the look of sleep on it (z's, a pale ring) and its status picture, if it is a player. */
    static void showSlumber(ServerLevel sl, LivingEntity sleeper, int ticks) {
        sleeper.getPersistentData().putBoolean(SLUMBER_SHOWN, true);
        ParticleShapes.clearModels(sl, sleeper, slumberTag(sleeper));        // one look at a time
        KnightOfMisfortuneFx.slumber(sl, sleeper, ticks, slumberTag(sleeper));
        if (sleeper instanceof Player player) KnightOfMisfortuneFx.status(player, Status.SLUMBER, ticks);
    }

    /** Puts the look of fear on it (an eye over its head) and its status picture, if it is a player. */
    static void showFear(ServerLevel sl, LivingEntity victim, int ticks) {
        KnightOfMisfortuneFx.afraid(sl, victim, ticks, "km_fear_" + victim.getId());
        if (victim instanceof Player player) KnightOfMisfortuneFx.status(player, Status.FEAR, ticks);
    }

    static String slumberTag(LivingEntity sleeper) {
        return "km_slumber_" + sleeper.getId();
    }

    static boolean isKnight(Player player, int sequence) {
        return SoulCore.getPathway(player).equals(PATHWAY) && Beyonder.hasSequence(player, sequence);
    }

    /** The same for anyone: a player as above, a Beyonder mob of this Pathway by its own Sequence. */
    static boolean isKnight(LivingEntity entity, int sequence) {
        if (entity instanceof Player player) return isKnight(player, sequence);
        return PATHWAY.equals(Beyonders.pathwayOf(entity)) && Beyonder.hasSequence(entity, sequence);
    }

    /** Has this pathway (or the check is skipped) and has reached the Sequence the ability needs. */
    static boolean ready(Player player, boolean bypassClassCheck, int sequence) {
        if (!bypassClassCheck && !SoulCore.getPathway(player).equals(PATHWAY)) return false;
        return Beyonder.hasSequence(player, sequence);
    }

    /** True while the game time saved under 'key' is still ahead. */
    static boolean isActive(LivingEntity entity, String key) {
        return entity.getPersistentData().getLongOr(key, 0L) > entity.level().getGameTime();
    }

    /** True (and says so) while the cooldown saved under 'key' runs. */
    static boolean coolingDown(LivingEntity player, ServerLevel sl, String key, String name) {
        long left = player.getPersistentData().getLongOr(key, 0L) - sl.getGameTime();
        if (left <= 0) return false;
        Spells.tell(player, Component.literal(name + " is not ready yet (" + ((left + 19) / 20) + "s)."));
        return true;
    }

    /** After a relog or a death: nothing of the timed casts is left over (their looks did not survive either). */
    private static void clearState(Player player) {
        CompoundTag data = player.getPersistentData();
        if (data.contains(BODY_X)) {
            Concealment.reveal(player);
            Flight.end(player);
        }
        clearNightmareKeys(player);
        for (String key : List.of(VISION_UNTIL, DOWSE_TAG, LIMBS_UNTIL, LIMBS_TAG)) data.remove(key);
        ConjuredArmor.takeOff(player, LIMBS_ARMOR);
        KnightOfMisfortuneSpirits.clearState(player);
        KnightOfMisfortuneNightwatcher.clearState(player);
        KnightOfMisfortuneDarkness.clearState(player);
        if (!SoulCore.getPathway(player).equals(PATHWAY)) return;
        for (Status status : Status.values()) KnightOfMisfortuneFx.statusEnd(player, status);   // a clock left running before death
    }
}
