package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.star_dragon;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder.SoulCore;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.MobForm;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.star_dragon.StarDragonFx.Status;
import net.lukario.circleofinevitability.combat.Orbit;
import net.lukario.circleofinevitability.combat.Shot;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.menu.AbilityWheel;
import net.lukario.circleofinevitability.status.Form;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Star Dragon, the two Mythical Creature Forms of the Cosmos wheel (docs/STAR_DRAGON.md): the Radiant Angel (Sequence
 * 2) and the Star Dragon (Sequence 1). Both are a status/Form like the Scaled Form in StarDragonTides: flying, a body
 * kept up while it lasts, an aftermath of weakness, and everything ended cleanly on death, logout or a lost Pathway.
 * The Dragon's small asteroid is a combat/Orbit of one body that is launched (Orbit.release) on the Dragon's blows.
 * The Dragon's thicker rays are in StarDragonRays (rayKind, rayWidth). Every number is right below.
 */
public final class StarDragonForms {

    private StarDragonForms() {}

    // =========================
    // Settings (cost = spirituality, ticks: 20 = 1 second, distances in blocks)
    // =========================

    // Radiant Angel Form (Radiant Angel)
    static final int ANGEL_SEQUENCE = 2;
    static final int ANGEL_COST = 25000;
    static final int ANGEL_COOLDOWN = 1800;
    static final int ANGEL_TICKS = 900;                 // 45 seconds...
    static final int ANGEL_AFTERMATH = 200;             // ...then 10 seconds of weakness
    static final double ANGEL_GLOW = 12.0;              // enemies this close are burned...
    static final float ANGEL_BURN = 20f;                // ...this much every half second...
    static final float ANGEL_UNDEAD = 2f;               // ...twice for the undead

    // Star Dragon Form (Star Dragon)
    static final int DRAGON_SEQUENCE = 1;
    static final int DRAGON_COST = 40000;
    static final int DRAGON_COOLDOWN = 2400;
    static final int DRAGON_TICKS = 1200;               // 60 seconds...
    static final int DRAGON_AFTERMATH = 300;            // ...then 15 seconds of weakness
    static final float DRAGON_TOUGH = 0.4f;             // 40% less damage taken (a ConjuredArmor, through Form)
    static final double DRAGON_SIZE = 1.2;              // 1.8 blocks tall to 4 (Attributes.SCALE, of the base)
    static final int DRAGON_BEAT_EVERY = 40;            // a wingbeat every 2 seconds while flying
    static final float DRAGON_GLOW = 0.5f;              // the Radiant Angel's glow at half strength
    static final double ASTEROID_RANGE = 24.0;
    static final float ASTEROID_DAMAGE = 80f;
    static final int ASTEROID_RECHARGE = 60;            // back 3 seconds after it was launched
    static final double ASTEROID_SPEED = 1.5;

    // =========================
    // Names
    // =========================
    static final String ANGEL = "star_dragon_radiant_angel";              // its Form
    static final String DRAGON = "star_dragon_dragon";                    // its Form
    static final String ASTEROID = "star_dragon_dragon_asteroid";         // its Orbit
    static final String STOLEN = "star_dragon_mythical_stolen";          // a form taken through a stolen cast

    /** In the Radiant Angel Form now (Fusion Cluster and Star Grazing hit harder). */
    public static boolean isRadiantAngel(Player player) {
        return Form.isIn(player, ANGEL);
    }

    /** In the Star Dragon Form now (thicker rays, the asteroid). */
    public static boolean isDragon(Player player) {
        return Form.isIn(player, DRAGON);
    }

    /** The same for anyone: a player's Form, or a Beyonder mob's (beyonder_mobs/MobForm, StarDragonMobs). */
    public static boolean isRadiantAngel(LivingEntity entity) {
        return entity instanceof Player player ? isRadiantAngel(player) : MobForm.isIn(entity, ANGEL);
    }

    public static boolean isDragon(LivingEntity entity) {
        return entity instanceof Player player ? isDragon(player) : MobForm.isIn(entity, DRAGON);
    }

    // =========================
    // Radiant Angel Form: your Mythical Creature Form for ANGEL_TICKS, a being of starlight with six pairs of wings.
    // You fly, have Strength II and Resistance II, and every enemy within ANGEL_GLOW is burned by your glow (ANGEL_BURN
    // every half second, ANGEL_UNDEAD times for the undead). Fusion Cluster and Star Grazing hit
    // StarDragonSky.RADIANT_BOOST harder. Cast it again to turn back (free). Then ANGEL_AFTERMATH of weakness.
    // =========================
    static AbilityWheel.Result radiantAngelForm(ServerPlayer player, ServerLevel sl) {
        if (isRadiantAngel(player)) {
            Form.end(player);                                                    // turning back is free (the weakness follows)
            return AbilityWheel.Result.FREE;
        }
        String tag = angelTag(player);
        Form.of(ANGEL)
                .lasts(ANGEL_TICKS)
                .effect(MobEffects.STRENGTH, 1)
                .effect(MobEffects.RESISTANCE, 1)
                .flies()
                .aftermath(ANGEL_AFTERMATH)
                .keepWhile(StarDragonForms::keeps)
                .every(10, one -> glow(one, 1f))
                .onEnd((one, why) -> ended(one, why, tag, Status.RADIANT_ANGEL, ANGEL_AFTERMATH, false))
                .take(player);
        markStolen(player);
        StarDragonFx.radiantAngel(sl, player, ANGEL_TICKS, tag);
        StarDragonFx.status(player, Status.RADIANT_ANGEL, ANGEL_TICKS);
        return AbilityWheel.Result.DONE;
    }

    /** Every 10 ticks of a form that shines: the glow, and the enemies near it burned ('strength' of a full burn). */
    static void glow(LivingEntity player, float strength) {
        if (!(player.level() instanceof ServerLevel sl)) return;
        StarDragonFx.radiance(sl, player, ANGEL_GLOW);
        for (LivingEntity enemy : Spells.enemiesAround(player, sl, player.position(), ANGEL_GLOW)) {
            float burn = ANGEL_BURN * strength * (enemy.is(EntityTypeTags.UNDEAD) ? ANGEL_UNDEAD : 1f);
            StarDragon.hit(player, enemy, burn);
            StarDragonFx.radiantBurn(sl, enemy);
        }
    }

    // =========================
    // Star Dragon Form: your Mythical Creature Form for DRAGON_TICKS, a colossal dragon of transparent starlight (your
    // body DRAGON_SIZE larger, 4 blocks tall). You fly and take DRAGON_TOUGH less damage; it beats its wings every
    // DRAGON_BEAT_EVERY while you are in the air. A small asteroid circles you (a combat/Orbit): when you strike a blow,
    // it is launched at the enemy you look at (else the one you swung at): ASTEROID_DAMAGE, and it is back
    // ASTEROID_RECHARGE later. Your rays are thicker (StarDragonRays) and the Radiant Angel's glow burns at
    // DRAGON_GLOW strength. Cast it again to turn back (free). Then DRAGON_AFTERMATH of weakness.
    // =========================
    static AbilityWheel.Result starDragonForm(ServerPlayer player, ServerLevel sl) {
        if (isDragon(player)) {
            Form.end(player);
            return AbilityWheel.Result.FREE;
        }
        String tag = dragonTag(player);
        Form.of(DRAGON)
                .lasts(DRAGON_TICKS)
                .attribute(Attributes.SCALE, DRAGON_SIZE, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
                .flies()
                .cutsDamage(DRAGON_TOUGH)
                .aftermath(DRAGON_AFTERMATH)
                .keepWhile(StarDragonForms::keeps)
                .every(10, one -> {
                    glow(one, DRAGON_GLOW);
                    if (one.tickCount % DRAGON_BEAT_EVERY == 0 && !one.onGround() && one.level() instanceof ServerLevel level) {
                        StarDragonFx.dragonBeat(level, one, Math.max(1, Form.ticksLeft(one)), tag);
                    }
                })
                .onEnd((one, why) -> ended(one, why, tag, Status.STAR_DRAGON, DRAGON_AFTERMATH, true))
                .take(player);
        markStolen(player);
        StarDragonFx.starDragonForm(sl, player, DRAGON_TICKS, tag);
        StarDragonFx.status(player, Status.STAR_DRAGON, DRAGON_TICKS);
        StarDragonFx.dragonAsteroid(sl, player, DRAGON_TICKS, tag, 0);
        Orbit.around(player, 1)
                .radius(StarDragonFx.ASTEROID_RADIUS).height(StarDragonFx.ASTEROID_HEIGHT).speed(StarDragonFx.ASTEROID_SPEED)
                .lasts(DRAGON_TICKS)
                .onTick(age -> StarDragonFx.dragonAsteroidOrbit(sl, player, tag, age))
                .onBack((index, ticksLeft, age) -> StarDragonFx.dragonAsteroidBack(sl, player, ticksLeft, tag, age))
                .open(player, sl, ASTEROID);
        return AbilityWheel.Result.DONE;
    }

    /**
     * A swing of the Star Dragon at 'struck' (AttackEntityEvent: a real blow, not an ability's hit): the asteroid, if
     * it is there, is launched.
     */
    static void swing(LivingEntity dragon, LivingEntity struck) {
        if (!isDragon(dragon) || struck == dragon || !(dragon.level() instanceof ServerLevel sl)) return;
        int age = Orbit.age(dragon, ASTEROID);
        Vec3 from = Orbit.release(dragon, ASTEROID, 0, ASTEROID_RECHARGE);
        if (from == null) return;                                                // still away
        StarDragonFx.dragonShot(sl, dragon, dragonTag(dragon), age);
        LivingEntity aimed = Spells.aimEnemy(dragon, sl, ASTEROID_RANGE);
        LivingEntity target = aimed != null ? aimed : struck;
        Vec3 way = target.getBoundingBox().getCenter().subtract(from);
        Shot.of(StarDragonFx.ASTEROID_FLYING)
                .speed(ASTEROID_SPEED).range(ASTEROID_RANGE).width(0.8).tip(0.4)
                .onHit((hit, at) -> StarDragon.hit(dragon, hit, ASTEROID_DAMAGE))
                .onEnd((at, hitBlock) -> StarDragonFx.dragonShotHit(sl, at))
                .fire(dragon, sl, from, way);
    }

    // =========================
    // Shared
    // =========================

    /** A form ends: its look, its picture, the weakness after it (when it ran out or was ended), the asteroid. */
    private static void ended(LivingEntity player, Form.Ending why, String tag, Status status, int aftermath, boolean dragon) {
        player.getPersistentData().remove(STOLEN);
        StarDragonFx.statusEnd(player, status);
        if (dragon) Orbit.close(player, ASTEROID);
        if (!(player.level() instanceof ServerLevel sl)) return;
        if (dragon) StarDragonFx.starDragonFormEnd(sl, player, tag);
        else StarDragonFx.radiantAngelEnd(sl, player, tag);
        if (why != Form.Ending.TIME && why != Form.Ending.ENDED) return;
        StarDragonFx.formWeakness(sl, player, aftermath, "sd_form_weakness_" + player.getId());
        StarDragonFx.status(player, Status.FORM_WEAKNESS, aftermath);
    }

    /** A form stays while you are a Star Dragon, or while it was taken through a stolen cast. */
    private static boolean keeps(Player player) {
        return SoulCore.getPathway(player).equals(StarDragon.PATHWAY) || player.getPersistentData().getBooleanOr(STOLEN, false);
    }

    private static void markStolen(Player player) {
        player.getPersistentData().putBoolean(STOLEN, !SoulCore.getPathway(player).equals(StarDragon.PATHWAY));
    }

    private static String angelTag(LivingEntity player) {
        return "sd_radiant_angel_" + player.getId();
    }

    private static String dragonTag(LivingEntity player) {
        return "sd_star_dragon_" + player.getId();
    }

    /** After a relog or a death (the Form itself ends by itself then). */
    static void clearState(LivingEntity player) {
        Orbit.close(player, ASTEROID);
        player.getPersistentData().remove(STOLEN);
    }
}
