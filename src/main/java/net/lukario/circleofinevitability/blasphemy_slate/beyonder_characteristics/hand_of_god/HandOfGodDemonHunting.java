package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.hand_of_god;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonders;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonder;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.SoulCore;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.hand_of_god.HandOfGodFx.Coat;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.hand_of_god.HandOfGodFx.Status;
import net.lukario.circleofinevitability.combat.Later;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.menu.AbilityWheel;
import net.lukario.circleofinevitability.particles.fx.ParticleShapes;
import net.lukario.circleofinevitability.root.Freeze;
import net.lukario.circleofinevitability.status.Decay;
import net.lukario.circleofinevitability.status.GiantForm;
import net.lukario.circleofinevitability.status.Marionette;
import net.lukario.circleofinevitability.status.Unnoticed;
import net.lukario.circleofinevitability.status.Vulnerable;
import net.lukario.circleofinevitability.status.WeaponCoat;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Hand Of God, key 6: the Demon Hunting wheel (Demon Hunter, Sequence 4). docs/HAND_OF_GOD.md, "Key 6 wheel".
 *
 *   Eye of Demon Hunting  12 s: evil and undead around you glow through walls; the enemy you look at is read (its
 *                         health, weakness and effects, in chat) and takes 20% more damage. From Silver Knight (3)
 *                         twice the reach, and it also tells a Beyonder's Pathway and Sequence
 *   Alchemy               a concoction for what you look at, on your weapon for 10 hits (status/WeaponCoat)
 *   Mind Concealment      15 s: mobs forget you and abilities cannot choose you (status/Unnoticed); hitting ends it
 *   Giant Form            40 s as the one-eyed Giant (status/GiantForm): bigger, Strength III, Resistance II, longer
 *                         reach, steps that slow; weaker onlookers are struck with terror. Madness grows until it
 *                         breaks the form (slower at Silver Knight, none from Glory). Cast again to end it
 *
 * Sneak casts the last pick again without the wheel (no cooldown, double the cost).
 * Giant Form is the Demon Hunter's own body: a thief cannot cast it.
 */
public final class HandOfGodDemonHunting {

    private HandOfGodDemonHunting() {}

    // =========================
    // Settings (cost = spirituality, ticks: 20 = 1 second, distances in blocks)
    // =========================

    static final int HUNTING_SEQUENCE = 4;                       // the key
    static final float QUICK_CAST_COST = 2f;

    // Eye of Demon Hunting
    static final int EYE_SEQUENCE = 4;
    static final int EYE_COST = 3000;
    static final int EYE_COOLDOWN = 200;
    static final int EYE_TICKS = 240;
    static final double EYE_RANGE = 40.0;
    static final int EYE_KNIGHT_SEQUENCE = 3;            // Silver Knight: further, and it reads Beyonders
    static final double EYE_KNIGHT_RANGE = 80.0;
    static final float EYE_WEAKNESS = 0.2f;              // the one whose weakness is found takes 20% more damage
    static final int EYE_GLOW_TICKS = 30;                // renewed every second while the eye is open

    // Alchemy
    static final int ALCHEMY_SEQUENCE = 4;
    static final int ALCHEMY_COST = 3500;
    static final int ALCHEMY_COOLDOWN = 300;
    static final double ALCHEMY_RANGE = 24.0;            // what you look at this far decides the coating
    static final int ALCHEMY_TICKS = 400;
    static final int ALCHEMY_HITS = 10;
    static final float DECAY_COAT_HEALTH = 40f;          // a living thing with this much health or more: Decay
    static final float PURIFY_EVIL = 12f;                // Purification: this much more to evil and undead...
    static final float PURIFY_OTHER = 4f;                // ...and this much to anything else
    static final int FREEZE_TICKS = 40;                  // Freeze: frozen this long...
    static final float FREEZE_DAMAGE = 4f;               // ...and this much more
    static final float LIGHTNING_DAMAGE = 8f;            // Lightning Strike: this much more...
    static final float LIGHTNING_CONDUCTED = 6f;         // ...and this much on top through water or metal
    static final int BURN_TICKS = 100;                   // Burning: set alight this long (it, never the ground)...
    static final float BURN_DAMAGE = 4f;
    static final float BURN_WEAK = 6f;                   // ...and this much more to plants, spiders, ice creatures
    static final int DECAY_COAT_TICKS = 100;             // Decay: rots this long...
    static final float DECAY_COAT_DEPTH = 0.15f;         // ...this much of its health gone meanwhile
    static final float DECAY_COAT_DAMAGE = 4f;
    static final float EXORCISM_SPIRIT = 20f;            // Exorcism: spirits take this much more; marionettes are freed
    static final float EXORCISM_OTHER = 6f;

    // Mind Concealment
    static final int MIND_SEQUENCE = 4;
    static final int MIND_COST = 4000;
    static final int MIND_COOLDOWN = 600;
    static final int MIND_TICKS = 300;

    // Giant Form (the sizes are times the body it has)
    static final int GIANT_SEQUENCE = 4;
    static final int GIANT_COST = 8000;
    static final int GIANT_COOLDOWN = 1200;
    static final int GIANT_TICKS = 800;
    static final int GIANT_KNIGHT_SEQUENCE = 3;
    static final int GIANT_GLORY_SEQUENCE = 2;
    static final double GIANT_SCALE = 1.2;               // Demon Hunter (4)
    static final double GIANT_KNIGHT_SCALE = 1.5;        // Silver Knight (3)
    static final double GIANT_GLORY_SCALE = 1.75;        // Glory (2) and up
    static final double GIANT_REACH = 1.0;               // more reach: 4 / 3 / 2 and up
    static final double GIANT_KNIGHT_REACH = 1.5;
    static final double GIANT_GLORY_REACH = 2.0;
    static final double GIANT_STEP_UP = 0.5;             // steps up this much more (a full block from Silver Knight)
    static final float GIANT_MADNESS = 1f / 30f;         // madness each second: full after 30 s at Demon Hunter...
    static final float GIANT_KNIGHT_MADNESS = 1f / 50f;  // ...slower at Silver Knight, none from Glory
    static final int GIANT_STRENGTH = 2;                 // Strength III
    static final int GIANT_RESISTANCE = 1;               // Resistance II
    static final double GIANT_STRIDE = 2.2;              // a step every this many blocks walked (times the size)...
    static final double GIANT_STOMP_RADIUS = 4.0;        // ...slows enemies on the ground this close (times the size)
    static final int GIANT_STOMP_SLOW = 40;
    static final double GIANT_TERROR_RANGE = 16.0;       // weaker ones that see the Giant...
    static final int GIANT_TERROR_TICKS = 60;            // ...get nausea and weakness this long...
    static final int GIANT_TERROR_REST = 200;            // ...at most once in this long
    static final int GIANT_BROKEN_TICKS = 200;           // full madness: the form breaks and you are weakened this long

    // =========================
    // Names and what is saved on the player
    // =========================
    static final String COAT = "hand_of_god_alchemy";
    static final String COAT_KIND = "hand_of_god_coat_kind";
    static final String MIND_TAG = "hand_of_god_mind_tag";
    static final String GIANT_TAG = "hand_of_god_giant_tag";
    static final String GIANT_HEIGHT = "hand_of_god_giant_height";
    static final String GIANT_MAD_SHOWN = "hand_of_god_giant_mad_shown";

    /** The open Eye of Demon Hunting of each player. Not saved. */
    private static final class Eye {
        final long until;
        final String tag;
        final double range;
        final boolean knight;
        final Set<UUID> marked = new HashSet<>();
        final Set<UUID> evilSeen = new HashSet<>();

        Eye(long until, String tag, double range, boolean knight) {
            this.until = until;
            this.tag = tag;
            this.range = range;
            this.knight = knight;
        }
    }
    private static final Map<UUID, Eye> EYES = new HashMap<>();

    /** True while its Eye of Demon Hunting is open. */
    static boolean eyeOpen(LivingEntity one) {
        return EYES.containsKey(one.getUUID());
    }

    /** Where each Giant last stepped, and when each onlooker was last struck with terror. Not saved. */
    private static final Map<UUID, Vec3> STRIDES = new HashMap<>();
    private static final Map<UUID, Long> TERRIFIED = new HashMap<>();

    private static final Set<Item> METAL_ARMOR = Set.of(
            Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS,
            Items.CHAINMAIL_HELMET, Items.CHAINMAIL_CHESTPLATE, Items.CHAINMAIL_LEGGINGS, Items.CHAINMAIL_BOOTS,
            Items.GOLDEN_HELMET, Items.GOLDEN_CHESTPLATE, Items.GOLDEN_LEGGINGS, Items.GOLDEN_BOOTS,
            Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS);

    // =========================
    // The key
    // =========================

    private static final AbilityWheel WHEEL = AbilityWheel.create("hand_of_god_demon_hunting", "Demon Hunting")
            .radius(72)
            .quickCastCooldown(false).quickCastCost(QUICK_CAST_COST)
            .entry("eye", HandOfGodFx.WHEEL_EYE, "Eye of Demon Hunting", EYE_SEQUENCE,
                    EYE_COST, EYE_COOLDOWN, HandOfGodDemonHunting::eyeOfDemonHunting)
            .entry("alchemy", HandOfGodFx.WHEEL_ALCHEMY, "Alchemy", ALCHEMY_SEQUENCE,
                    ALCHEMY_COST, ALCHEMY_COOLDOWN, HandOfGodDemonHunting::alchemy)
            .entry("mind_concealment", HandOfGodFx.WHEEL_MIND_CONCEALMENT, "Mind Concealment", MIND_SEQUENCE,
                    MIND_COST, MIND_COOLDOWN, HandOfGodDemonHunting::mindConcealment)
            .entry("giant_form", HandOfGodFx.WHEEL_GIANT_FORM, "Giant Form", GIANT_SEQUENCE,
                    GIANT_COST, GIANT_COOLDOWN, HandOfGodDemonHunting::giantForm)
            .free(GiantForm::isGiant);                                          // the key again ends it

    // =========================
    // Ability 6 - Demon Hunting
    // Normal: the Demon Hunting wheel. Sneak: the last pick again, without the wheel.
    // =========================
    public static void handOfGodDemonHunting(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!HandOfGod.ready(player, bypassClassCheck, HUNTING_SEQUENCE)) return;
        if (player.isShiftKeyDown()) WHEEL.quickCast(player);
        else WHEEL.open(player);
    }

    // =========================
    // Eye of Demon Hunting: for EYE_TICKS, every second the evil and undead within EYE_RANGE glow (seen through walls);
    // the enemy you look at (that far) is read once, in chat, and takes EYE_WEAKNESS more damage until the eye closes.
    // Silver Knight (3): EYE_KNIGHT_RANGE, and a player's Pathway and Sequence are read too. Cannot choose someone
    // whose mind is concealed (status/Unnoticed).
    // =========================
    static AbilityWheel.Result eyeOfDemonHunting(LivingEntity player, ServerLevel sl) {
        closeEye(player, sl);                                                   // an older one first
        boolean knight = Beyonder.hasSequence(player, EYE_KNIGHT_SEQUENCE);
        String tag = "hog_eye_" + player.getId() + "_" + sl.getGameTime();
        EYES.put(player.getUUID(), new Eye(sl.getGameTime() + EYE_TICKS, tag, knight ? EYE_KNIGHT_RANGE : EYE_RANGE, knight));
        HandOfGodFx.eye(sl, player, EYE_TICKS, tag);
        HandOfGodFx.status(player, Status.EYE, EYE_TICKS);
        return AbilityWheel.Result.DONE;
    }

    private static void eyeTick(LivingEntity player, ServerLevel sl) {
        Eye eye = EYES.get(player.getUUID());
        if (eye == null) return;
        long now = sl.getGameTime();
        if (now >= eye.until) {
            closeEye(player, sl);
            return;
        }
        if (now < eye.until - EYE_TICKS + HandOfGodFx.EYE_OPENS) return;     // still opening
        int left = (int) (eye.until - now);

        if (player.tickCount % 20 == 0) {
            for (LivingEntity one : Spells.livingAround(player, sl, player.position(), eye.range)) {
                if (Spells.isFriend(player, one) || !Spells.isEvil(one)) continue;
                Spells.keepEffect(one, MobEffects.GLOWING, 0, EYE_GLOW_TICKS);
                if (eye.evilSeen.add(one.getUUID())) HandOfGodFx.eyeEvil(sl, one, left);
            }
        }
        LivingEntity looked = Spells.aimEnemy(player, sl, eye.range);
        if (looked == null || !eye.marked.add(looked.getUUID())) return;
        Vulnerable.add(looked, EYE_WEAKNESS, EYE_WEAKNESS, left);
        HandOfGodFx.eyeMark(sl, looked, left, eye.tag);
        Spells.tell(player, read(looked, eye.knight));
    }

    /** What the eye reads on someone: its health, its weakness, its effects (and, for a Silver Knight, its Pathway). */
    private static Component read(LivingEntity one, boolean knight) {
        StringBuilder text = new StringBuilder();
        text.append(one.getDisplayName().getString())
                .append(": ").append(Math.round(one.getHealth())).append("/").append(Math.round(one.getMaxHealth())).append(" health")
                .append(", weak to ").append(coatName(coatFor(one)));
        List<String> effects = new ArrayList<>();
        for (MobEffectInstance effect : one.getActiveEffects()) {
            effects.add(effect.getEffect().value().getDisplayName().getString() + " " + (effect.getAmplifier() + 1));
        }
        if (!effects.isEmpty()) text.append(", under ").append(String.join(", ", effects));
        if (knight && Beyonders.isBeyonder(one)) {                           // a player or a Beyonder mob
            text.append(", a Beyonder of ").append(Beyonders.pathwayOf(one)).append(" at Sequence ").append(Beyonders.sequenceOf(one));
        }
        return Component.literal(text.toString());
    }

    private static void closeEye(LivingEntity player, ServerLevel sl) {
        Eye eye = EYES.remove(player.getUUID());
        if (eye == null) return;
        HandOfGodFx.eyeEnd(sl, player, eye.tag);
        for (UUID id : eye.marked) {
            if (sl.getEntity(id) instanceof LivingEntity marked) ParticleShapes.clearModels(sl, marked, eye.tag);
        }
        HandOfGodFx.statusEnd(player, Status.EYE);
    }

    // =========================
    // Alchemy: a concoction brewed for what you look at (within ALCHEMY_RANGE; nothing: Lightning Strike) coats
    // your weapon at HandOfGodFx.VIAL_POURED, for ALCHEMY_HITS hits or ALCHEMY_TICKS (status/WeaponCoat):
    //   Exorcism      spirits and marionettes or summons (it frees them: summons vanish)
    //   Freeze        fire creatures
    //   Burning       spiders and other arthropods, ice creatures
    //   Lightning     water creatures and anyone in metal armor
    //   Purification  evil and undead
    //   Decay         living things with DECAY_COAT_HEALTH or more
    // =========================
    static AbilityWheel.Result alchemy(LivingEntity player, ServerLevel sl) {
        LivingEntity target = Spells.aimEnemy(player, sl, ALCHEMY_RANGE);
        Coat coat = target == null ? Coat.LIGHTNING : coatFor(target);
        HandOfGodFx.alchemy(sl, player, coat);
        Later.run(sl, HandOfGodFx.VIAL_POURED, () -> {
            if (!Spells.casterStillHere(player, sl)) return;
            WeaponCoat.armAny(player, COAT, ALCHEMY_HITS, ALCHEMY_TICKS, (attacker, hit, amount, level) -> coatHit(hit, amount, level, coat));
            player.getPersistentData().putString(COAT_KIND, coat.name());
            HandOfGodFx.status(player, Status.ALCHEMY, ALCHEMY_TICKS);
            Spells.tell(player, Component.literal("Your weapon is coated: " + coatName(coat) + "."));
        });
        return AbilityWheel.Result.DONE;
    }

    /** The concoction that works best against it (in the order of the list above). */
    static Coat coatFor(LivingEntity one) {
        if (Marionette.isMarionette(one) || Marionette.isSummoned(one) || one.getType() == EntityTypes.VEX) return Coat.EXORCISM;
        if (one.fireImmune()) return Coat.FREEZE;
        if (one.is(EntityTypeTags.ARTHROPOD) || one.is(EntityTypeTags.FREEZE_IMMUNE_ENTITY_TYPES)) return Coat.BURNING;
        if (one.is(EntityTypeTags.AQUATIC) || wearsMetal(one)) return Coat.LIGHTNING;
        if (Spells.isEvil(one)) return Coat.PURIFICATION;
        if (one.getMaxHealth() >= DECAY_COAT_HEALTH) return Coat.DECAY;
        return Coat.LIGHTNING;
    }

    private static boolean wearsMetal(LivingEntity one) {
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (METAL_ARMOR.contains(one.getItemBySlot(slot).getItem())) return true;
        }
        return false;
    }

    private static String coatName(Coat coat) {
        return switch (coat) {
            case PURIFICATION -> "Purification";
            case FREEZE -> "Freeze";
            case LIGHTNING -> "Lightning Strike";
            case BURNING -> "Burning";
            case DECAY -> "Decay";
            case EXORCISM -> "Exorcism";
        };
    }

    /** A coated hit. Returns its damage (the coat's part adds to it, so it is not a second hit). */
    private static float coatHit(LivingEntity target, float amount, ServerLevel sl, Coat coat) {
        HandOfGodFx.coatHit(sl, target, coat);
        return switch (coat) {
            case PURIFICATION -> amount + (Spells.isEvil(target) ? PURIFY_EVIL : PURIFY_OTHER);
            case FREEZE -> {
                Freeze.apply(target, FREEZE_TICKS);
                yield amount + FREEZE_DAMAGE;
            }
            case LIGHTNING -> amount + LIGHTNING_DAMAGE
                    + (target.is(EntityTypeTags.AQUATIC) || target.isInWater() || wearsMetal(target) ? LIGHTNING_CONDUCTED : 0f);
            case BURNING -> {
                target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), BURN_TICKS));
                boolean weak = target.is(EntityTypeTags.ARTHROPOD) || target.is(EntityTypeTags.FREEZE_IMMUNE_ENTITY_TYPES);
                yield amount + BURN_DAMAGE + (weak ? BURN_WEAK : 0f);
            }
            case DECAY -> {
                Decay.apply(target, DECAY_COAT_TICKS, DECAY_COAT_DEPTH);
                yield amount + DECAY_COAT_DAMAGE;
            }
            case EXORCISM -> {
                boolean spirit = target.getType() == EntityTypes.VEX || Marionette.isSummoned(target);
                if (target instanceof Mob mob && Marionette.isMarionette(mob)) Marionette.release(mob);   // the strings are cut
                yield amount + (spirit ? EXORCISM_SPIRIT : EXORCISM_OTHER);
            }
        };
    }

    // =========================
    // Mind Concealment: for MIND_TICKS your intentions cannot be read (status/Unnoticed): mobs forget you and abilities
    // cannot choose you. You are still seen and can be hurt; hurting something ends it.
    // =========================
    static AbilityWheel.Result mindConcealment(LivingEntity player, ServerLevel sl) {
        String old = player.getPersistentData().getStringOr(MIND_TAG, "");
        if (!old.isEmpty()) HandOfGodFx.mindConcealmentEnd(sl, player, old);
        String tag = "hog_mind_" + player.getId() + "_" + sl.getGameTime();
        player.getPersistentData().putString(MIND_TAG, tag);
        Unnoticed.end(player);
        Unnoticed.hide(player, MIND_TICKS);
        HandOfGodFx.mindConcealment(sl, player, MIND_TICKS, tag);
        HandOfGodFx.status(player, Status.CONCEALMENT, MIND_TICKS);
        return AbilityWheel.Result.DONE;
    }

    /** True while Mind Concealment hides the player (Light Concealment keeps them hidden longer then). */
    static boolean mindConcealed(LivingEntity player) {
        return player.getPersistentData().contains(MIND_TAG) && Unnoticed.isUnnoticed(player);
    }

    private static void mindTick(LivingEntity player, ServerLevel sl) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(MIND_TAG) || Unnoticed.isUnnoticed(player)) return;
        HandOfGodFx.mindConcealmentEnd(sl, player, data.getStringOr(MIND_TAG, ""));      // it ran out, or a blow ended it
        HandOfGodFx.statusEnd(player, Status.CONCEALMENT);
        data.remove(MIND_TAG);
    }

    // =========================
    // Giant Form: GIANT_TICKS as the one-eyed Giant (status/GiantForm): GIANT_SCALE times your size (Silver Knight
    // GIANT_KNIGHT_SCALE, Glory GIANT_GLORY_SCALE), more reach, a longer step, Strength III and Resistance II. Every
    // stride shakes the ground and slows enemies on it; weaker ones that see you get nausea and weakness. Madness
    // grows each second (GIANT_MADNESS; slower at Silver Knight, none from Glory): when it is full the form breaks
    // and you are left weakened. Cast again: the form ends (free). Only for the Demon Hunter's own body.
    // =========================
    static AbilityWheel.Result giantForm(LivingEntity player, ServerLevel sl) {
        if (GiantForm.isGiant(player)) {
            GiantForm.end(player);
            return AbilityWheel.Result.FREE;
        }
        if (!Beyonders.pathwayOf(player).equals(HandOfGod.PATHWAY)) {
            Spells.tell(player, Component.literal("Only a Demon Hunter's own body can become the Giant."));
            return AbilityWheel.Result.REFUSED;
        }
        boolean glory = Beyonder.hasSequence(player, GIANT_GLORY_SEQUENCE);
        boolean knight = Beyonder.hasSequence(player, GIANT_KNIGHT_SEQUENCE);
        double scale = glory ? GIANT_GLORY_SCALE : knight ? GIANT_KNIGHT_SCALE : GIANT_SCALE;
        double height = player.getBbHeight() * scale;
        String tag = "hog_giant_" + player.getId() + "_" + sl.getGameTime();
        CompoundTag data = player.getPersistentData();
        data.putString(GIANT_TAG, tag);
        data.putDouble(GIANT_HEIGHT, height);
        data.remove(GIANT_MAD_SHOWN);

        HandOfGodFx.giantForm(sl, player, height, GIANT_TICKS, tag);          // first: it grows the look from the size they are now
        GiantForm.of(scale)
                .reach(glory ? GIANT_GLORY_REACH : knight ? GIANT_KNIGHT_REACH : GIANT_REACH)
                .stepHeight(knight ? GIANT_STEP_UP * 2 : GIANT_STEP_UP)
                .lasts(GIANT_TICKS)
                .madness(glory ? 0f : knight ? GIANT_KNIGHT_MADNESS : GIANT_MADNESS)
                .everySecond((giant, madness) -> giantSecond(player, madness))
                .onMad(giant -> maddened(player))
                .onEnd((giant, why) -> giantGone(player))
                .grow(player);
        STRIDES.put(player.getUUID(), player.position());
        HandOfGodFx.status(player, Status.GIANT, GIANT_TICKS);
        giantSecond(player, 0f);
        return AbilityWheel.Result.DONE;
    }

    /** Once a second while giant: its strength, the terror of the weak, the madness shown. */
    private static void giantSecond(LivingEntity player, float madness) {
        if (!(player.level() instanceof ServerLevel sl)) return;
        Spells.keepEffect(player, MobEffects.STRENGTH, GIANT_STRENGTH, 40);
        Spells.keepEffect(player, MobEffects.RESISTANCE, GIANT_RESISTANCE, 40);
        double height = player.getPersistentData().getDoubleOr(GIANT_HEIGHT, player.getBbHeight());
        long now = sl.getGameTime();
        for (LivingEntity one : Spells.livingAround(player, sl, player.position(), GIANT_TERROR_RANGE)) {
            if (Spells.isFriend(player, one) || one.getMaxHealth() >= player.getMaxHealth()) continue;   // only the weaker
            Long last = TERRIFIED.get(one.getUUID());
            if (last != null && now - last < GIANT_TERROR_REST) continue;
            if (!one.hasLineOfSight(player)) continue;
            TERRIFIED.put(one.getUUID(), now);
            one.addEffect(new MobEffectInstance(MobEffects.NAUSEA, GIANT_TERROR_TICKS, 0));
            one.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, GIANT_TERROR_TICKS, 0));
            HandOfGodFx.giantTerror(sl, one);
        }
        if (madness <= 0f) return;
        HandOfGodFx.giantMadness(sl, player, madness, height);
        if (madness >= 0.5f && !player.getPersistentData().contains(GIANT_MAD_SHOWN)) {
            player.getPersistentData().putBoolean(GIANT_MAD_SHOWN, true);
            HandOfGodFx.statusForever(player, Status.MADNESS);
            Spells.tell(player, Component.literal("The Giant's madness is rising."));
        }
    }

    /** Every tick while giant: a stride every so far shakes the ground. */
    private static void strideTick(LivingEntity player, ServerLevel sl) {
        if (!GiantForm.isGiant(player) || !player.onGround()) return;
        Vec3 last = STRIDES.get(player.getUUID());
        double scale = GiantForm.scale(player);
        if (last != null && player.position().distanceTo(last) < GIANT_STRIDE * scale) return;
        STRIDES.put(player.getUUID(), player.position());
        if (last == null) return;
        HandOfGodFx.giantStep(sl, player, player.getPersistentData().getDoubleOr(GIANT_HEIGHT, player.getBbHeight()));
        for (LivingEntity enemy : Spells.enemiesAround(player, sl, player.position(), GIANT_STOMP_RADIUS * scale)) {
            if (enemy.onGround()) Spells.keepEffect(enemy, MobEffects.SLOWNESS, 0, GIANT_STOMP_SLOW);
        }
    }

    private static void maddened(LivingEntity player) {
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, GIANT_BROKEN_TICKS, 1));
        player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, GIANT_BROKEN_TICKS, 0));
        player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, GIANT_BROKEN_TICKS / 2, 0));
        Spells.tell(player, Component.literal("The madness breaks the Giant's form."));
    }

    private static void giantGone(LivingEntity player) {
        CompoundTag data = player.getPersistentData();
        if (player.level() instanceof ServerLevel sl) {
            HandOfGodFx.giantFormEnd(sl, player, data.getDoubleOr(GIANT_HEIGHT, player.getBbHeight()), data.getStringOr(GIANT_TAG, ""));
        }
        HandOfGodFx.statusEnd(player, Status.GIANT);
        HandOfGodFx.statusEnd(player, Status.MADNESS);
        // the Giant's own strength goes (the Warrior's Strength I and the Guardian's Resistance I come back within a second)
        MobEffectInstance strength = player.getEffect(MobEffects.STRENGTH);
        if (strength != null && strength.getAmplifier() == GIANT_STRENGTH) player.removeEffect(MobEffects.STRENGTH);
        MobEffectInstance resistance = player.getEffect(MobEffects.RESISTANCE);
        if (resistance != null && resistance.getAmplifier() == GIANT_RESISTANCE) player.removeEffect(MobEffects.RESISTANCE);
        STRIDES.remove(player.getUUID());
        for (String key : List.of(GIANT_TAG, GIANT_HEIGHT, GIANT_MAD_SHOWN)) data.remove(key);
    }

    // =========================
    // Called from HandOfGod
    // =========================

    /** Every tick. */
    static void tick(LivingEntity player, ServerLevel sl) {
        if (!EYES.isEmpty() && player.tickCount % 5 == 0) eyeTick(player, sl);
        strideTick(player, sl);
        if (player.tickCount % 5 == 0) {
            CompoundTag data = player.getPersistentData();
            if (data.contains(COAT_KIND)) {
                if (WeaponCoat.isArmed(player, COAT)) {
                    HandOfGodFx.coatTick(sl, player, coatOf(data.getStringOr(COAT_KIND, "")));
                } else {
                    data.remove(COAT_KIND);                                     // used up, or worn off
                    HandOfGodFx.statusEnd(player, Status.ALCHEMY);
                }
            }
        }
        if (player.tickCount % 20 == 0) {
            mindTick(player, sl);
            if (sl.getGameTime() % 1200 == 0) TERRIFIED.values().removeIf(last -> sl.getGameTime() - last > GIANT_TERROR_REST);
        }
    }

    private static Coat coatOf(String name) {
        for (Coat coat : Coat.values()) if (coat.name().equals(name)) return coat;
        return Coat.LIGHTNING;
    }

    /** After a relog or a death: start clean (the coat, the eye and the form do not survive either). */
    static void clearState(Player player) {
        EYES.remove(player.getUUID());
        STRIDES.remove(player.getUUID());
        WeaponCoat.disarm(player, COAT);
        CompoundTag data = player.getPersistentData();
        for (String key : List.of(COAT_KIND, MIND_TAG, GIANT_TAG, GIANT_HEIGHT, GIANT_MAD_SHOWN)) data.remove(key);
    }

    /** Logging out: what is kept here only is forgotten. */
    static void forget(LivingEntity player) {
        EYES.remove(player.getUUID());
        STRIDES.remove(player.getUUID());
        TERRIFIED.remove(player.getUUID());
    }
}
