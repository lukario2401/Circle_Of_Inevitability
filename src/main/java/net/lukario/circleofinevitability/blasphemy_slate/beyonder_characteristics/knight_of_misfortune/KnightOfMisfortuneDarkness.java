package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.knight_of_misfortune;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonder;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.knight_of_misfortune.KnightOfMisfortuneFx.Edict;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.knight_of_misfortune.KnightOfMisfortuneFx.Status;
import net.lukario.circleofinevitability.combat.Dome;
import net.lukario.circleofinevitability.combat.Later;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.combat.Zone;
import net.lukario.circleofinevitability.menu.AbilityWheel;
import net.lukario.circleofinevitability.particles.fx.ParticleShapes;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.lukario.circleofinevitability.status.Blind;
import net.lukario.circleofinevitability.status.Concealment;
import net.lukario.circleofinevitability.status.ConjuredArmor;
import net.lukario.circleofinevitability.status.ConjuredWeapon;
import net.lukario.circleofinevitability.status.DeathWard;
import net.lukario.circleofinevitability.status.Fear;
import net.lukario.circleofinevitability.status.Nocturnality;
import net.lukario.circleofinevitability.status.Silence;
import net.lukario.circleofinevitability.status.Slumber;
import net.lukario.circleofinevitability.status.Unnoticed;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Knight Of Misfortune, key 7: the Darkness and Concealment wheel (Horror Bishop, Sequence 3, and up).
 * docs/KNIGHT_OF_MISFORTUNE.md, "Key 7 wheel".
 *
 *   Sword of Darkness         (3) a bone sword in your hand for SWORD_TICKS (status/ConjuredWeapon). A full swing that
 *                             lands sends a tide of Darkness forward: everything in its cone is submerged (a strike no
 *                             ward stops) and melts a while. From Knight of Misfortune (1) three full swings in a row
 *                             carry the edicts: Eternal Sleep, Despair, Destruction. Pick it again to let it go.
 *   Nightmare World           (3) a dream world WORLD_RADIUS round you (WORLD_RADIUS_LARGE from 2): enemies cannot leave
 *                             (combat/Dome), are slowed and dulled, and slide into Slumber; allies are shielded and
 *                             no harmful effect takes on them. It is night inside. Pick it again to wake it.
 *   Concealed State           (2) the one you aim at is taken out of the world for CONCEALED_TICKS: hidden, untouchable,
 *                             held, blind. Aimed at an ally it only hides them; at nothing, it hides you. Aimed at
 *                             someone already hidden (Concealment, Unnoticed) it is Reverse Concealment: torn out
 *                             and lit up.
 *   Secret Concealment        (2) a veil SECRET_RADIUS round where you stand: your side inside is Unnoticed and cannot be
 *                             picked by any ability from outside your side (Spells.addUnchoosableCheck).
 *   Concealed World           (2) a sealed sphere round you: nothing crosses it, no projectile either, and nothing
 *                             outside can pick anyone inside. Pick it again to open it.
 *   Mantle of Darkness        (1) your body half-dissolves into Darkness for MANTLE_TICKS: stronger, faster, tougher, a
 *                             quarter of all damage shed, and two shadow arms follow every swing to the two nearest.
 *   Eternal Rest              (0) the word of Darkness. A sleeper (Slumber, a bed) simply stops; anyone else (not a boss)
 *                             loses REST_SHARE of its health and falls into an Eternal Slumber. Aimed at nothing: your
 *                             own rest, a ward against your next death (status/DeathWard).
 *   Destination of All Things (0) a black sphere where you aim: everything is drawn in and dissolves, harder the longer
 *                             it stays (allies take a quarter). You cannot die while you stand in it.
 *
 * Sneak casts the last pick again without the wheel (no cooldown, double the cost), as for the other wheels.
 * The looks are KnightOfMisfortuneFx's; every number is right below.
 */
public final class KnightOfMisfortuneDarkness {

    private KnightOfMisfortuneDarkness() {}

    // =========================
    // Settings (cost = spirituality, ticks: 20 = 1 second, distances in blocks)
    // =========================

    static final int DARKNESS_SEQUENCE = 3;                      // the key
    private static final float QUICK_CAST_COST = 2f;

    private static final int SWORD_SEQUENCE = 3;
    private static final int SWORD_COST = 10000;
    private static final int SWORD_COOLDOWN = 400;
    private static final int SWORD_TICKS = 800;
    private static final float SWORD_DAMAGE = 1f;                // more on every hit, times the power level
    private static final double SWORD_REACH = 1.0;
    private static final float FULL_SWING = 0.9f;                // the attack bar at least this full: a tide
    private static final double TIDE_LENGTH = 10.0;
    private static final double TIDE_ANGLE = 50.0;
    private static final int TIDE_TICKS = 10;
    private static final float TIDE_DAMAGE = 4f;                 // times the power level
    private static final float MELT_DAMAGE = 1f;                 // each second, times the power level...
    private static final int MELT_SECONDS = 4;                   // ...this many seconds
    // Knight of Misfortune (1): the edicts
    private static final int EDICT_WINDOW = 40;                  // the next full swing within this long carries the next edict
    private static final int EDICT_SLEEP = 120;
    private static final int EDICT_DESPAIR = 120;
    private static final float EDICT_DESTRUCTION = 4f;           // more damage, times the power level
    private static final float EDICT_WEAR = 0.25f;               // of every worn and held item's durability

    private static final int WORLD_SEQUENCE = 3;
    private static final int WORLD_COST = 12000;
    private static final int WORLD_COOLDOWN = 2400;
    private static final double WORLD_RADIUS = 24.0;
    private static final double WORLD_RADIUS_LARGE = 48.0;       // Servant of Concealment (2)
    private static final int WORLD_TICKS = 600;
    private static final float WORLD_SHIELD = 0.3f;              // allies inside take this much less damage
    private static final int WORLD_SLEEP_AFTER = 160;            // enemies inside this long slide into Slumber...
    private static final int WORLD_SLEEP_AGAIN = 100;            // ...and again after this, if they woke
    private static final int WORLD_SLEEP = 80;

    private static final int CONCEAL_SEQUENCE = 2;
    private static final int CONCEAL_COST = 15000;
    private static final int CONCEAL_COOLDOWN = 900;
    private static final double CONCEAL_RANGE = 40.0;
    private static final int CONCEALED_TICKS = 160;
    private static final int CONCEALED_BOSS = 60;                // Darkness (0) only
    private static final int CONCEALED_ERASED = 400;             // Darkness (0): one inside your Concealed World
    private static final int REVEALED_TICKS = 200;               // Reverse Concealment: lit up this long

    private static final int SECRET_SEQUENCE = 2;
    private static final int SECRET_COST = 15000;
    private static final int SECRET_COOLDOWN = 1200;
    private static final double SECRET_RADIUS = 16.0;
    private static final int SECRET_TICKS = 900;

    private static final int CW_SEQUENCE = 2;
    private static final int CW_COST = 20000;
    private static final int CW_COOLDOWN = 2400;
    private static final double CW_RADIUS = 16.0;
    private static final int CW_TICKS = 400;

    private static final int MANTLE_SEQUENCE = 1;
    private static final int MANTLE_COST = 30000;
    private static final int MANTLE_COOLDOWN = 1800;
    private static final int MANTLE_TICKS = 500;
    private static final float MANTLE_SHED = 0.25f;              // this much of all damage simply dissolves
    private static final double MANTLE_REACH = 1.5;
    private static final float MANTLE_DAMAGE = 1f;               // more on every hit, times the power level
    private static final double MANTLE_ARMS_REACH = 5.0;         // the shadow arms find the two nearest this close
    private static final float MANTLE_ARMS_SHARE = 0.5f;         // and hit them for this much of the blow

    private static final int REST_SEQUENCE = 0;
    private static final int REST_COST = 40000;
    private static final int REST_COOLDOWN = 1800;
    private static final double REST_RANGE = 32.0;
    private static final float REST_SHARE = 0.4f;
    private static final int REST_SLUMBER = 160;
    private static final int REST_WARD = 1200;
    private static final float REST_WARD_HEALTH = 0.6f;

    private static final int DESTINATION_SEQUENCE = 0;
    private static final int DESTINATION_COST = 50000;
    private static final int DESTINATION_COOLDOWN = 3600;
    private static final double DESTINATION_RANGE = 48.0;
    private static final double DESTINATION_RADIUS = 24.0;
    private static final int DESTINATION_TICKS = 240;
    private static final float DESTINATION_DAMAGE = 2f;          // each second, times the power level, growing...
    private static final int DESTINATION_GROWS = 80;             // ...by its own size again every this long inside
    private static final float DESTINATION_ALLY_SHARE = 0.25f;

    // =========================
    // Saved on the player, and names
    // =========================
    private static final String SWORD_TAG = "knight_of_misfortune_sword_tag";
    private static final String EDICT_STEP = "knight_of_misfortune_edict_step";
    private static final String EDICT_AT = "knight_of_misfortune_edict_at";
    private static final String MANTLE_TAG = "knight_of_misfortune_mantle_tag";
    private static final String SWORD = "knight_of_misfortune_dark_sword";
    private static final String MANTLE = "knight_of_misfortune_mantle";
    private static final String WORLD_DOME = "knight_of_misfortune_nightmare_world";
    private static final String WORLD_ZONE = "knight_of_misfortune_nightmare_world_zone";
    private static final String SECRET_ZONE = "knight_of_misfortune_secret";
    private static final String CW_DOME = "knight_of_misfortune_concealed_world";
    private static final String DESTINATION_ZONE = "knight_of_misfortune_destination";

    /** An area that hides its side from being picked (Secret Concealment, Concealed World). Not saved. */
    private record Veil(Player owner, ServerLevel level, Vec3 center, double radius, long until, boolean sealed) {
        boolean holds(LivingEntity entity) {
            return entity.level() == level && entity.level().getGameTime() < until
                    && entity.getBoundingBox().getCenter().distanceToSqr(center) <= radius * radius;
        }

        boolean onOwnersSide(LivingEntity entity) {
            return entity == owner || entity.getUUID().equals(owner.getUUID()) || Spells.isFriend(owner, entity);
        }
    }

    private static final List<Veil> VEILS = new ArrayList<>();

    // =========================
    // The key
    // =========================

    private static final AbilityWheel WHEEL = AbilityWheel.create("knight_of_misfortune_darkness", "Darkness and Concealment")
            .radius(80)
            .quickCastCooldown(false).quickCastCost(QUICK_CAST_COST)
            .entry("sword_of_darkness", KnightOfMisfortuneFx.WHEEL_SWORD_OF_DARKNESS, "Sword of Darkness", SWORD_SEQUENCE,
                    SWORD_COST, SWORD_COOLDOWN, KnightOfMisfortuneDarkness::swordOfDarkness)
            .free(player -> ConjuredWeapon.isDrawn(player, SWORD))                          // letting it go
            .entry("nightmare_world", KnightOfMisfortuneFx.WHEEL_NIGHTMARE_WORLD, "Nightmare World", WORLD_SEQUENCE,
                    WORLD_COST, WORLD_COOLDOWN, KnightOfMisfortuneDarkness::nightmareWorld)
            .free(player -> Dome.isRaised(player, WORLD_DOME))                              // waking it
            .entry("concealed_state", KnightOfMisfortuneFx.WHEEL_CONCEALED_STATE, "Concealed State", CONCEAL_SEQUENCE,
                    CONCEAL_COST, CONCEAL_COOLDOWN, KnightOfMisfortuneDarkness::concealedState)
            .entry("secret_concealment", KnightOfMisfortuneFx.WHEEL_SECRET_CONCEALMENT, "Secret Concealment", SECRET_SEQUENCE,
                    SECRET_COST, SECRET_COOLDOWN, KnightOfMisfortuneDarkness::secretConcealment)
            .free(player -> Zone.isOpen(player, SECRET_ZONE))                               // lifting the veil
            .entry("concealed_world", KnightOfMisfortuneFx.WHEEL_CONCEALED_WORLD, "Concealed World", CW_SEQUENCE,
                    CW_COST, CW_COOLDOWN, KnightOfMisfortuneDarkness::concealedWorld)
            .free(player -> Dome.isRaised(player, CW_DOME))                                 // opening it
            .entry("mantle_of_darkness", KnightOfMisfortuneFx.WHEEL_MANTLE_OF_DARKNESS, "Mantle of Darkness", MANTLE_SEQUENCE,
                    MANTLE_COST, MANTLE_COOLDOWN, KnightOfMisfortuneDarkness::mantleOfDarkness)
            .free(player -> ConjuredWeapon.isDrawn(player, MANTLE))                         // taking it off
            .entry("eternal_rest", KnightOfMisfortuneFx.WHEEL_ETERNAL_REST, "Eternal Rest", REST_SEQUENCE,
                    REST_COST, REST_COOLDOWN, KnightOfMisfortuneDarkness::eternalRest)
            .entry("destination_of_all_things", KnightOfMisfortuneFx.WHEEL_DESTINATION_OF_ALL_THINGS, "Destination of All Things",
                    DESTINATION_SEQUENCE, DESTINATION_COST, DESTINATION_COOLDOWN, KnightOfMisfortuneDarkness::destination);

    // =========================
    // Ability 7 - Darkness and Concealment
    // Normal: the Darkness wheel. Sneak: the last pick again, without the wheel.
    // =========================
    public static void knightOfMisfortuneDarkness(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!KnightOfMisfortune.ready(player, bypassClassCheck, DARKNESS_SEQUENCE)) return;
        if (player.isShiftKeyDown()) WHEEL.quickCast(player);
        else WHEEL.open(player);
    }

    // =========================
    // Sword of Darkness
    // =========================

    private static AbilityWheel.Result swordOfDarkness(ServerPlayer player, ServerLevel sl) {
        if (ConjuredWeapon.isDrawn(player, SWORD)) {                      // the second pick: let it go
            ConjuredWeapon.putAway(player, SWORD);
            return AbilityWheel.Result.FREE;
        }
        String tag = "km_sword_" + player.getId() + "_" + sl.getGameTime();
        player.getPersistentData().putString(SWORD_TAG, tag);
        player.getPersistentData().putInt(EDICT_STEP, 0);
        KnightOfMisfortuneFx.darkSword(sl, player, SWORD_TICKS, tag);
        KnightOfMisfortuneFx.status(player, Status.DARK_SWORD, SWORD_TICKS);
        float extra = SWORD_DAMAGE * Beyonder.powerLevel(player);
        ConjuredWeapon.of(SWORD).lasts(SWORD_TICKS).damage(extra).reach(SWORD_REACH)
                .onHit((attacker, target, amount, charge, level) -> swordHit(attacker, target, amount, charge, level))
                .onEnd((wielder, why) -> {
                    String old = wielder.getPersistentData().getStringOr(SWORD_TAG, tag);
                    wielder.getPersistentData().remove(SWORD_TAG);
                    if (wielder.level() instanceof ServerLevel level) KnightOfMisfortuneFx.darkSwordEnd(level, wielder, old);
                    KnightOfMisfortuneFx.statusEnd(wielder, Status.DARK_SWORD);
                })
                .draw(player);
        return AbilityWheel.Result.DONE;
    }

    /** Every hit of the sword: a full swing sends the tide; from Sequence 1 it carries the next edict. */
    private static float swordHit(Player player, LivingEntity target, float amount, float charge, ServerLevel sl) {
        KnightOfMisfortuneFx.darkSwing(sl, player);
        if (charge < FULL_SWING) return amount;
        Later.run(sl, 1, () -> {
            if (Spells.casterStillHere(player, sl)) tide(player, sl);
        });
        if (!Beyonder.hasSequence(player, 1)) return amount;

        CompoundTag data = player.getPersistentData();
        long now = sl.getGameTime();
        int step = now - data.getLongOr(EDICT_AT, 0L) <= EDICT_WINDOW ? data.getIntOr(EDICT_STEP, 0) : 0;
        Edict edict = Edict.values()[step % Edict.values().length];
        data.putInt(EDICT_STEP, (step + 1) % Edict.values().length);
        data.putLong(EDICT_AT, now);
        KnightOfMisfortuneFx.edict(sl, player, target, edict);
        switch (edict) {
            case ETERNAL_SLEEP -> Later.run(sl, 1, () -> {
                if (target.isAlive() && !KnightOfMisfortune.isBoss(target) && Slumber.put(target, EDICT_SLEEP, Slumber.Kind.DEEP)) {
                    KnightOfMisfortune.showSlumber(sl, target, EDICT_SLEEP);
                }
            });
            case DESPAIR -> Later.run(sl, 1, () -> {
                if (!target.isAlive()) return;
                if (Fear.apply(target, player, EDICT_DESPAIR)) KnightOfMisfortune.showFear(sl, target, EDICT_DESPAIR);
                Silence.apply(target, EDICT_DESPAIR);
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, EDICT_DESPAIR, 1));
                if (!KnightOfMisfortune.isBoss(target)) Slumber.put(target, EDICT_DESPAIR, Slumber.Kind.LIMP);
            });
            case DESTRUCTION -> {
                for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
                        EquipmentSlot.FEET, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND}) {
                    ItemStack stack = target.getItemBySlot(slot);
                    if (stack.isEmpty() || !stack.isDamageableItem()) continue;
                    stack.hurtAndBreak(Math.max(1, Math.round(stack.getMaxDamage() * EDICT_WEAR)), target, slot);
                }
                return amount + EDICT_DESTRUCTION * Beyonder.powerLevel(player);
            }
        }
        return amount;
    }

    /** The tide of Darkness: everything in the cone in front is submerged as the wave reaches it, and melts. */
    private static void tide(Player player, ServerLevel sl) {
        KnightOfMisfortuneFx.darkTide(sl, player, TIDE_LENGTH, TIDE_TICKS);
        Vec3 from = player.position().add(0, 1.0, 0);
        float damage = TIDE_DAMAGE * Beyonder.powerLevel(player);
        float melt = MELT_DAMAGE * Beyonder.powerLevel(player);
        // chosen now: once the tide is spread it cannot be sidestepped
        for (LivingEntity enemy : Spells.enemiesInCone(player, sl, from, Spells.flatLook(player), TIDE_LENGTH, TIDE_ANGLE)) {
            int reaches = (int) Math.round(Math.min(1.0, enemy.position().distanceTo(player.position()) / TIDE_LENGTH) * TIDE_TICKS);
            Later.run(sl, reaches, () -> {
                if (!enemy.isAlive() || !Spells.casterStillHere(player, sl)) return;
                Spells.strikeThrough(player, enemy, damage);
                KnightOfMisfortuneFx.tideMelt(sl, enemy);
                for (int second = 1; second <= MELT_SECONDS; second++) {
                    Later.run(sl, second * 20, () -> {
                        if (!enemy.isAlive() || !Spells.casterStillHere(player, sl)) return;
                        Spells.strikeThrough(player, enemy, melt);
                        KnightOfMisfortuneFx.tideMelt(sl, enemy);
                    });
                }
            });
        }
    }

    // =========================
    // Nightmare World
    // =========================

    private static AbilityWheel.Result nightmareWorld(ServerPlayer player, ServerLevel sl) {
        if (Dome.isRaised(player, WORLD_DOME)) {                          // the second pick: wake it
            Dome.close(player, WORLD_DOME);
            return AbilityWheel.Result.FREE;
        }
        Vec3 ground = Spells.groundUnder(player, sl, player);
        double radius = Beyonder.hasSequence(player, 2) ? WORLD_RADIUS_LARGE : WORLD_RADIUS;
        String tag = "km_nightmare_world_" + player.getId() + "_" + sl.getGameTime();
        int closed = KnightOfMisfortuneFx.WORLD_CLOSED;
        KnightOfMisfortuneFx.nightmareWorld(sl, ground, radius, WORLD_TICKS + closed, tag);
        KnightOfMisfortuneFx.status(player, Status.NIGHTMARE_WORLD, WORLD_TICKS + closed);
        Nocturnality.forceNight(sl, ground, radius, WORLD_TICKS + closed);
        Later.run(sl, closed, () -> {
            if (!Spells.casterStillHere(player, sl)) {
                ParticleShapes.clearModels(sl, ground, tag);
                return;
            }
            Dome.sphere(ground, radius).lasts(WORLD_TICKS).keepsEnemiesIn().shields(WORLD_SHIELD).wardsEffects()
                    .onEnd(() -> {
                        Zone.close(player, WORLD_ZONE);
                        ParticleShapes.clearModels(sl, ground, tag);
                        KnightOfMisfortuneFx.statusEnd(player, Status.NIGHTMARE_WORLD);
                    })
                    .raise(player, sl, WORLD_DOME);
            Zone.at(ground, radius).height(radius).lasts(WORLD_TICKS).every(20)
                    .onInside((enemy, inside) -> dreamt(sl, enemy, inside))
                    .onPulse(left -> KnightOfMisfortuneFx.nightmareWorldPulse(sl, ground, radius))
                    .open(player, sl, WORLD_ZONE);
        });
        return AbilityWheel.Result.DONE;
    }

    /** Once a second for each enemy in the Nightmare World: slow, dull, and at last asleep. */
    private static void dreamt(ServerLevel sl, LivingEntity enemy, int inside) {
        Spells.keepEffect(enemy, MobEffects.SLOWNESS, 1, 40);
        Spells.keepEffect(enemy, MobEffects.WEAKNESS, 0, 40);
        Spells.keepEffect(enemy, MobEffects.MINING_FATIGUE, 0, 40);
        if (inside < WORLD_SLEEP_AFTER || (inside - WORLD_SLEEP_AFTER) % WORLD_SLEEP_AGAIN != 0) return;
        if (KnightOfMisfortune.isBoss(enemy) || Slumber.kind(enemy) != null) return;
        if (Slumber.put(enemy, WORLD_SLEEP, Slumber.Kind.SLEEP)) KnightOfMisfortune.showSlumber(sl, enemy, WORLD_SLEEP);
    }

    // =========================
    // Concealed State / Reverse Concealment
    // =========================

    private static AbilityWheel.Result concealedState(ServerPlayer player, ServerLevel sl) {
        LivingEntity target = aimedAtAnyone(player, sl, CONCEAL_RANGE);
        if (target == null) target = player;                              // aimed at nothing: hide yourself
        boolean hidden = Concealment.isHidden(target) || Unnoticed.isUnnoticed(target);
        if (target != player && hidden && !Spells.isFriend(player, target)) {
            reverseConcealment(player, sl, target);
            return AbilityWheel.Result.DONE;
        }
        boolean enemy = target != player && Spells.isEnemy(player, target);
        int ticks = CONCEALED_TICKS;
        if (enemy && KnightOfMisfortune.isBoss(target)) {
            if (!Beyonder.hasSequence(player, 0)) {
                player.sendSystemMessage(Component.literal(target.getName().getString() + " is too vast to be hidden away."));
                return AbilityWheel.Result.REFUSED;
            }
            ticks = CONCEALED_BOSS;
        } else if (enemy && Beyonder.hasSequence(player, 0) && Dome.isInside(player, CW_DOME, target)) {
            ticks = CONCEALED_ERASED;                                     // erased inside your own Concealed World
        }
        String tag = "km_concealed_" + target.getId();
        LivingEntity hid = target;
        int held = ticks;
        KnightOfMisfortuneFx.concealedState(sl, hid, held, tag);
        Later.run(sl, KnightOfMisfortuneFx.CONCEALED, () -> {
            if (!hid.isAlive()) return;
            Concealment.hide(hid, held);
            if (enemy) {
                Root.apply(hid, held, RootRestriction.EVERYTHING);
                Blind.apply(hid, held);
            }
            if (hid instanceof Player hiddenPlayer) KnightOfMisfortuneFx.status(hiddenPlayer, Status.CONCEALED, held);
            Later.run(sl, held, () -> KnightOfMisfortuneFx.concealedStateEnd(sl, hid, tag));
        });
        return AbilityWheel.Result.DONE;
    }

    private static void reverseConcealment(Player player, ServerLevel sl, LivingEntity target) {
        Concealment.reveal(target);
        Unnoticed.end(target);
        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, REVEALED_TICKS, 0, false, false));
        KnightOfMisfortuneFx.reverseConcealment(sl, target);
        player.sendSystemMessage(Component.literal(target.getName().getString() + " is torn out of hiding."));
    }

    /** The first living body on the crosshair's line, hidden or not, friend or foe (not the caster). */
    private static LivingEntity aimedAtAnyone(Player player, ServerLevel sl, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = Spells.aimPoint(player, sl, range);
        List<LivingEntity> onLine = new ArrayList<>();
        for (LivingEntity candidate : sl.getEntitiesOfClass(LivingEntity.class, new AABB(eye, end).inflate(1.0),
                other -> other != player && other.isAlive() && !other.isSpectator() && !(other instanceof ArmorStand))) {
            AABB body = candidate.getBoundingBox().inflate(0.4);
            if (body.contains(eye) || body.clip(eye, end).isPresent()) onLine.add(candidate);
        }
        onLine.sort(Comparator.comparingDouble(candidate -> candidate.distanceToSqr(player)));
        return onLine.isEmpty() ? null : onLine.get(0);
    }

    // =========================
    // Secret Concealment
    // =========================

    private static AbilityWheel.Result secretConcealment(ServerPlayer player, ServerLevel sl) {
        if (Zone.isOpen(player, SECRET_ZONE)) {                           // the second pick: lift the veil
            Zone.close(player, SECRET_ZONE);
            return AbilityWheel.Result.FREE;
        }
        Vec3 ground = Spells.groundUnder(player, sl, player);
        String tag = "km_secret_" + player.getId() + "_" + sl.getGameTime();
        Veil veil = new Veil(player, sl, ground, SECRET_RADIUS, sl.getGameTime() + SECRET_TICKS, false);
        VEILS.add(veil);
        KnightOfMisfortuneFx.secretConcealment(sl, ground, SECRET_RADIUS, SECRET_TICKS, tag);
        KnightOfMisfortuneFx.status(player, Status.SECRET, SECRET_TICKS);
        Zone.at(ground, SECRET_RADIUS).height(8).lasts(SECRET_TICKS).every(20)
                .affects(entity -> Spells.isFriend(player, entity))
                .onInside((ally, inside) -> Unnoticed.hide(ally, 40))
                .onPulse(left -> {
                    KnightOfMisfortuneFx.secretPulse(sl, ground, SECRET_RADIUS);
                    if (player.position().distanceTo(ground) <= SECRET_RADIUS) Unnoticed.hide(player, 40);
                })
                .onEnd(() -> {
                    VEILS.remove(veil);
                    ParticleShapes.clearModels(sl, ground, tag);
                    KnightOfMisfortuneFx.statusEnd(player, Status.SECRET);
                })
                .open(player, sl, SECRET_ZONE);
        return AbilityWheel.Result.DONE;
    }

    // =========================
    // Concealed World
    // =========================

    private static AbilityWheel.Result concealedWorld(ServerPlayer player, ServerLevel sl) {
        if (Dome.isRaised(player, CW_DOME)) {                             // the second pick: open it
            Dome.close(player, CW_DOME);
            return AbilityWheel.Result.FREE;
        }
        Vec3 center = player.position().add(0, 1.0, 0);
        String tag = "km_concealed_world_" + player.getId() + "_" + sl.getGameTime();
        Veil veil = new Veil(player, sl, center, CW_RADIUS, sl.getGameTime() + CW_TICKS, true);
        VEILS.add(veil);
        KnightOfMisfortuneFx.concealedWorld(sl, center, CW_RADIUS, CW_TICKS, tag);
        KnightOfMisfortuneFx.status(player, Status.CONCEALED_WORLD, CW_TICKS);
        Dome.sphere(center, CW_RADIUS).lasts(CW_TICKS).keepsEnemiesIn().stopsProjectiles()
                .onEnd(() -> {
                    VEILS.remove(veil);
                    KnightOfMisfortuneFx.concealedWorldEnd(sl, center, CW_RADIUS, tag);
                    KnightOfMisfortuneFx.statusEnd(player, Status.CONCEALED_WORLD);
                })
                .raise(player, sl, CW_DOME);
        return AbilityWheel.Result.DONE;
    }

    /**
     * Spells.addUnchoosableCheck (registered by KnightOfMisfortune): a Secret Concealment hides the owner's side inside
     * it from everyone not on that side; a Concealed World hides everyone inside it from everyone outside it.
     */
    static boolean veiled(Player chooser, LivingEntity target) {
        if (VEILS.isEmpty()) return false;
        VEILS.removeIf(veil -> veil.level().getGameTime() >= veil.until());
        for (Veil veil : VEILS) {
            if (!veil.holds(target)) continue;
            if (veil.sealed()) {
                if (chooser == null || !veil.holds(chooser)) return true;
            } else if (veil.onOwnersSide(target) && (chooser == null || !veil.onOwnersSide(chooser))) {
                return true;
            }
        }
        return false;
    }

    // =========================
    // Mantle of Darkness
    // =========================

    private static AbilityWheel.Result mantleOfDarkness(ServerPlayer player, ServerLevel sl) {
        if (ConjuredWeapon.isDrawn(player, MANTLE)) {                     // the second pick: take it off
            ConjuredWeapon.putAway(player, MANTLE);
            return AbilityWheel.Result.FREE;
        }
        String tag = "km_mantle_" + player.getId() + "_" + sl.getGameTime();
        player.getPersistentData().putString(MANTLE_TAG, tag);
        KnightOfMisfortuneFx.mantle(sl, player, MANTLE_TICKS, tag);
        KnightOfMisfortuneFx.status(player, Status.MANTLE, MANTLE_TICKS);
        mantleBody(player);
        ConjuredArmor.of(MANTLE).lasts(MANTLE_TICKS).cut(MANTLE_SHED).wear(player);
        ConjuredWeapon.of(MANTLE).lasts(MANTLE_TICKS).damage(MANTLE_DAMAGE * Beyonder.powerLevel(player)).reach(MANTLE_REACH)
                .onHit((attacker, target, amount, charge, level) -> {
                    float share = amount * MANTLE_ARMS_SHARE;
                    Later.run(level, 1, () -> shadowArms(attacker, level, target, share));
                    return amount;
                })
                .onEnd((wielder, why) -> {
                    String old = wielder.getPersistentData().getStringOr(MANTLE_TAG, tag);
                    wielder.getPersistentData().remove(MANTLE_TAG);
                    ConjuredArmor.takeOff(wielder, MANTLE);
                    if (wielder.level() instanceof ServerLevel level) KnightOfMisfortuneFx.mantleEnd(level, wielder, old);
                    KnightOfMisfortuneFx.statusEnd(wielder, Status.MANTLE);
                })
                .draw(player);
        return AbilityWheel.Result.DONE;
    }

    private static void mantleBody(Player player) {
        Spells.keepEffect(player, MobEffects.STRENGTH, 2, 60);
        Spells.keepEffect(player, MobEffects.SPEED, 1, 60);
        Spells.keepEffect(player, MobEffects.RESISTANCE, 1, 60);
    }

    /** The two shadow arms follow a blow: the two nearest other enemies are hit for a share of it and slowed. */
    private static void shadowArms(Player player, ServerLevel sl, LivingEntity struck, float share) {
        if (!Spells.casterStillHere(player, sl)) return;
        List<LivingEntity> near = Spells.enemiesAround(player, sl, struck.position(), MANTLE_ARMS_REACH);
        near.remove(struck);
        near.sort(Comparator.comparingDouble(enemy -> enemy.distanceToSqr(struck)));
        List<LivingEntity> hit = near.subList(0, Math.min(2, near.size()));
        for (LivingEntity enemy : hit) {
            Spells.strike(player, enemy, share);
            Spells.keepEffect(enemy, MobEffects.SLOWNESS, 1, 40);
        }
        KnightOfMisfortuneFx.mantleSwing(sl, player, hit);
    }

    // =========================
    // Eternal Rest
    // =========================

    private static AbilityWheel.Result eternalRest(ServerPlayer player, ServerLevel sl) {
        LivingEntity target = Spells.aimEnemy(player, sl, REST_RANGE);
        if (target == null) {                                             // aimed at nothing: your own rest
            String tag = "km_rest_ward_" + player.getId();
            KnightOfMisfortuneFx.restWard(sl, player, REST_WARD, tag);
            KnightOfMisfortuneFx.status(player, Status.ETERNAL_REST, REST_WARD);
            DeathWard.arm(player, REST_WARD, REST_WARD_HEALTH, (saved, source, level) -> {
                if (saved instanceof Player rested) {
                    KnightOfMisfortuneFx.restWake(level, rested, tag);
                    KnightOfMisfortuneFx.statusEnd(rested, Status.ETERNAL_REST);
                }
            });
            player.sendSystemMessage(Component.literal("Your rest waits over you: the next death is not yours to die."));
            return AbilityWheel.Result.DONE;
        }
        if (KnightOfMisfortune.isBoss(target)) {
            player.sendSystemMessage(Component.literal(target.getName().getString() + " is too vast to be laid to rest."));
            return AbilityWheel.Result.REFUSED;
        }
        KnightOfMisfortuneFx.eternalRest(sl, player, target);
        Later.run(sl, KnightOfMisfortuneFx.REST_FALLS, () -> {
            if (!target.isAlive() || !Spells.casterStillHere(player, sl)) return;
            boolean sleeping = Slumber.kind(target) != null || (target instanceof Player sleeper && sleeper.isSleeping());
            if (sleeping) {
                Slumber.wake(target);
                Spells.strikeThrough(player, target, target.getMaxHealth() * 10f + target.getAbsorptionAmount() + 1000f);
                return;
            }
            Spells.strikeThrough(player, target, target.getMaxHealth() * REST_SHARE);
            if (target.isAlive() && Slumber.put(target, REST_SLUMBER, Slumber.Kind.ETERNAL)) {
                KnightOfMisfortune.showSlumber(sl, target, REST_SLUMBER);
            }
        });
        return AbilityWheel.Result.DONE;
    }

    // =========================
    // Destination of All Things
    // =========================

    private static AbilityWheel.Result destination(ServerPlayer player, ServerLevel sl) {
        Vec3 ground = Spells.aimGround(player, sl, DESTINATION_RANGE);
        String tag = "km_destination_" + player.getId() + "_" + sl.getGameTime();
        int opens = KnightOfMisfortuneFx.DESTINATION_OPENS;
        float base = DESTINATION_DAMAGE * Beyonder.powerLevel(player);
        KnightOfMisfortuneFx.destination(sl, ground, DESTINATION_RADIUS, DESTINATION_TICKS + opens, tag);
        Later.run(sl, opens, () -> {
            if (!Spells.casterStillHere(player, sl)) {
                KnightOfMisfortuneFx.destinationEnd(sl, ground, tag);
                return;
            }
            Zone.at(ground, DESTINATION_RADIUS).height(16).lasts(DESTINATION_TICKS).every(5)
                    .affects(entity -> !(entity instanceof ArmorStand))
                    .onInside((entity, inside) -> drawnIn(player, sl, ground, entity, inside, base))
                    .onPulse(left -> {
                        if (left % 10 == 0) KnightOfMisfortuneFx.destinationTick(sl, ground, DESTINATION_RADIUS);
                        // the Darkness will not take you while you stand in it
                        if (player.position().distanceTo(ground) <= DESTINATION_RADIUS && !DeathWard.isArmed(player)) {
                            DeathWard.arm(player, 40, 0.3f, null);
                        }
                    })
                    .onEnd(() -> KnightOfMisfortuneFx.destinationEnd(sl, ground, tag))
                    .open(player, sl, DESTINATION_ZONE);
        });
        return AbilityWheel.Result.DONE;
    }

    /** Every 5 ticks for everything inside: pulled to the middle, and once a second it dissolves a little more. */
    private static void drawnIn(Player player, ServerLevel sl, Vec3 ground, LivingEntity entity, int inside, float base) {
        Spells.pull(entity, ground, 0.5, 0.04);
        if (inside % 20 != 0) return;
        float damage = base * (1f + inside / (float) DESTINATION_GROWS);
        if (Spells.isFriend(player, entity)) damage *= DESTINATION_ALLY_SHARE;
        Spells.strikeThrough(player, entity, damage);
        KnightOfMisfortuneFx.dissolve(sl, entity);
    }

    // =========================
    // Called by KnightOfMisfortune
    // =========================

    /** Every tick for every player: the mantle's body and its dripping darkness. */
    static void tick(Player player, ServerLevel sl) {
        if (!ConjuredWeapon.isDrawn(player, MANTLE)) return;
        if (player.tickCount % 4 == 0) KnightOfMisfortuneFx.mantleTick(sl, player);
        if (player.tickCount % 20 == 0) mantleBody(player);
    }

    static void clearState(Player player) {
        CompoundTag data = player.getPersistentData();
        for (String key : List.of(SWORD_TAG, MANTLE_TAG, EDICT_STEP, EDICT_AT)) data.remove(key);
        VEILS.removeIf(veil -> veil.owner().getUUID().equals(player.getUUID()));
    }
}
