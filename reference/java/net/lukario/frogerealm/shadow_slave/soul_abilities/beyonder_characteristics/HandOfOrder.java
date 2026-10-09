package net.lukario.frogerealm.shadow_slave.soul_abilities.beyonder_characteristics;
import net.lukario.frogerealm.ForgeRealm;
import net.lukario.frogerealm.combat.Later;
import net.lukario.frogerealm.combat.MeleeCombo;
import net.lukario.frogerealm.menu.AbilityMenu;
import net.lukario.frogerealm.menu.AbilityTextPrompt;
import net.lukario.frogerealm.particles.CustomParticles;
import net.lukario.frogerealm.particles.fx.ModelFx;
import net.lukario.frogerealm.particles.fx.ParticleFx;
import net.lukario.frogerealm.particles.fx.ParticleShapes;
import net.lukario.frogerealm.particles.fx.SlashFx;
import net.lukario.frogerealm.root.Freeze;
import net.lukario.frogerealm.root.Root;
import net.lukario.frogerealm.root.RootRestriction;
import net.lukario.frogerealm.screen.ScreenAnchor;
import net.lukario.frogerealm.screen.ScreenImages;
import net.lukario.frogerealm.shadow_slave.soul_shards.SoulCore;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Set;
import java.util.List;

import java.util.List;
public class HandOfOrder {
//test

    ///particle forgerealmmod:model{model:"forgerealmmod:model_fx/hand_of_order/sword",glow:1b,lifetime:100,keys:[{tick:20,pitch:90f,ease:"in"}]} ~ ~2 ~ 0 0 0 0 1

    private static final ModelFx HAND_OF_ORDER_SWORD = ModelFx.of("hand_of_order/sword")      // models/model_fx/rune.json
            .scale(2f)                  // 1 = Blockbench size (16 px = 1 block)
            .pivot(8, 8, 8)               // pixels, Blockbench coordinates
            .glow()                       // full bright, visible at night
            .aura(0xff0000, 0.1f, 3)    // glowing outline: ARGB color, thickness in blocks, softness
            .lifetime(100)                 // ticks (20 = 1 second)
            .fade(5, 10);            // fade in 5 ticks, fade out the last 10

    // Ability 1 extra (the damage/defense buffs stay as they were):
    //   normal cast  -> Frost of Order: freezes the surrounding mobs (see root/Freeze)
    //   sneak + cast -> Hammer of Order: a golden hammer appears in your hands and slams down in front of you
    private static final double FREEZE_RANGE = 8.0;
    private static final int FREEZE_TICKS = 80;               // 4 seconds

    private static final float HAMMER_DAMAGE = 12f;
    private static final double HAMMER_RADIUS = 2.5;          // around the spot where the headlands
    private static final double HAMMER_KNOCKBACK = 0.6;
    private static final float HAMMER_SCALE = 1.6f;           // bigger hammer = longer reach too
    private static final double HAMMER_GRIP_HEIGHT = 1.2;     // the grip is at the caster's hands
    private static final int HAMMER_IMPACT_TICK = 18;         // the keyframe where the head hits
    private static final int HAMMER_LIFETIME = 46;
    // Measured on models/model_fx/order_hammer.json, in pixels: grip -> middle of the head,
    // and handle -> striking face. Update them if you reshape the model.
    private static final float HAMMER_REACH_PX = 31f;
    private static final float HAMMER_FACE_PX = 12f;

    // 3D model in models/model_fx/order_hammer.json (+ textures/model_fx/order_hammer.png)
    private static final ModelFx ORDER_HAMMER = ModelFx.of("order_hammer")
            .scale(HAMMER_SCALE)
            .pivot(8, -10, 8)                       // the grip, just above the pommel
            .glow()
            .aura(0xFFFFB82E, 0.14f, 3)             // golden glow around it
            .lifetime(HAMMER_LIFETIME).fade(0, 8);

    // Ability 3 - Edict: normal = close wide cone, sneaking = long narrow cone
    private static final int EDICT_COST = 1250;
    private static final double EDICT_CLOSE_RANGE = 6.0;
    private static final double EDICT_CLOSE_ANGLE = 90.0;    // full cone angle in degrees
    private static final float EDICT_CLOSE_DAMAGE = 14f;
    private static final double EDICT_CLOSE_KNOCKBACK = 0.8;
    private static final double EDICT_LONG_RANGE = 24.0;
    private static final double EDICT_LONG_ANGLE = 16.0;
    private static final float EDICT_LONG_DAMAGE = 18f;


    public static final ParticleFx MUSHROOMS = ParticleFx.of("fx/hand_of_order_mushroom")
            .size(0.2f).endSize(0.2f)
            .lifetime(0, 20)
            .gravity(0.12f)
            .friction(0.9f)
            .glow()
            .collide();

    public static final ParticleFx MUSHROOMS_V2 = ParticleFx.of("fx/hand_of_order_mushroom")
            .size(0.6f).endSize(0f)
            .lifetime(0, 40)
            .friction(0.9f)
            .glow()
            .spin(15).randomRotation()
            .collide();


    // Particles (textures in assets/forgerealmmod/textures/particle/fx/)
    private static final ParticleFx GOLD_SHARD = ParticleFx.of("fx/shard")
            .color(0xFFFFD86B).endColor(0x00FF9A3C)
            .size(0.18f).endSize(0.06f).sizeRandom(0.3f)
            .lifetime(8, 3).friction(1f)
            .glow().spin(25f).randomRotation();
    private static final ParticleFx GOLD_GLOW = ParticleFx.of("fx/glow")
            .color(0xCCFFE9A8).fadeOut()
            .size(0.35f).endSize(0.1f)
            .lifetime(6, 2).friction(1f)
            .glow();
    private static final ParticleFx WHITE_SPARK = ParticleFx.of("fx/spark")
            .color(0xFFFFF6D8).endColor(0x00FFD86B)
            .size(0.14f).endSize(0.04f)
            .lifetime(14, 2).friction(1f)
            .glow().spin(-20f).randomRotation();

    private static final ParticleFx LANCE_CORE = GOLD_GLOW.size(0.22f).endSize(0.02f).lifetime(8, 4);
    private static final ParticleFx HIT_SHARD = GOLD_SHARD.friction(0.8f).gravity(0.6f).lifetime(12, 6);

    // Ability 2 - Decree (menu with 3 choices)
    private static final int DECREE_COST = 1250;
    private static final float DECREE_EXPLOSION_POWER = 12.0f;   // TNT is 4
    private static final int DECREE_SPEED_TICKS = 200;          // 10 seconds
    private static final int DECREE_SPEED_LEVEL = 5;            // 0 = Speed I, 2 = Speed III
    private static final float DECREE_JUDGEMENT_CHANCE = 0.30f;
    private static final float DECREE_JUDGEMENT_DAMAGE = 47f;
    private static final double DECREE_JUDGEMENT_RANGE = 16.0;

    // Icons are in assets/forgerealmmod/textures/gui/ability_menus/hand_of_order/
    // x, y = icon center relative to the middle of the screen -> triangle
    private static final AbilityMenu DECREE_MENU = AbilityMenu.create("hand_of_order_decree")
            .title("Decree of Order")
            .option("ability_menus/hand_of_order/explosion", "Detonate",    0, -46, HandOfOrder::decreeExplosion)
            .option("ability_menus/hand_of_order/speed",     "Haste",     -46,  30, HandOfOrder::decreeSpeed)
            .option("ability_menus/hand_of_order/judgement", "Judgement",  46,  30, HandOfOrder::decreeJudgement);


    // Ability 6 - Words of Order: type a phrase, the phrase decides what happens
    private static final int WORDS_COST = 6000;              // only paid when a phrase works
    private static final float GOD_EXPLOSION_POWER = 10f;    // TNT is 4 - this can easily kill the caster
    private static final double BETHEL_RANGE = 24.0;
    private static final int BETHEL_SLOWNESS_TICKS = 400;    // 20 seconds
    private static final int BETHEL_SLOWNESS_LEVEL = 120;    // same number as /effect give ... slowness 20 120
    private static final double KNEEL_RANGE = 16.0;
    private static final int KNEEL_TICKS = 160;              // 8 seconds
    private static final double JUDGEMENT_FALLS_RANGE = 24.0;
    private static final int JUDGEMENT_FALLS_TARGETS = 5;

    private static final ParticleFx ORDER_WAVE = ParticleFx.of("fx/glow")
            .color(0xFFFFE08A).fadeOut()
            .size(0.5f).endSize(0.2f)
            .lifetime(20).friction(1f)
            .glow();
    private static final ParticleFx SEAL_MOTE = ParticleFx.of("fx/spark")
            .color(0xFF9FB8FF).fadeOut()
            .size(0.15f).endSize(0.05f)
            .lifetime(30, 10).gravity(-0.02f).friction(0.95f)
            .glow().spin(10f).randomRotation();

    private static final AbilityTextPrompt WORDS = AbilityTextPrompt.create("hand_of_order_words")
            .title("Words of Order")
            .hint("Speak...")
            .phrase("The sentence was God", HandOfOrder::wordsTheSentenceWasGod)
            .phrase("Bethel Abraham",       HandOfOrder::wordsBethelAbraham)
            .phrase("Kneel",                HandOfOrder::wordsKneel)
            .phrase("Order restored",       HandOfOrder::wordsOrderRestored)
            .phrase("Judgement falls",      HandOfOrder::wordsJudgementFalls)
            .otherwise((player, sl, text) ->
                    player.sendSystemMessage(Component.literal("The words hold no power.")));

    // Ability 7 - Bind: roots the target you look at. The closer it is, the more it loses.
    private static final int BIND_COST = 6000;
    private static final double BIND_RANGE = 20.0;

    /** Up to maxDistance blocks away -> rooted for ticks with these restrictions. Checked top to bottom. */
    private record BindTier(double maxDistance, int ticks, Set<RootRestriction> restrictions) {}

    private static final List<BindTier> BIND_TIERS = List.of(
            new BindTier(4,  160, RootRestriction.EVERYTHING),        // 8s, can't do anything
            new BindTier(8,  120, EnumSet.of(RootRestriction.MOVE, RootRestriction.JUMP, RootRestriction.TELEPORT,
                    RootRestriction.ATTACK, RootRestriction.ABILITIES)), // 6s
            new BindTier(14, 100, RootRestriction.MOVEMENT),          // 5s, held in place
            new BindTier(20,  80, EnumSet.of(RootRestriction.MOVE))   // 4s, can't walk
    );

    private static final ParticleFx BIND_CHAIN = ParticleFx.of("fx/glow")
            .color(0xFFFFE08A).fadeOut()
            .size(0.12f).endSize(0.06f)
            .lifetime(16, 6)
            .glow();

    // Melee combo (hitting mobs as Hand of Order): 1 = crescent, 2 = rising claws, 3 = random finisher
    // Registered in combat/MeleeCombos. Slash textures are in textures/particle/slash/.
    private static final int COMBO_GLOW = 0xE070F0E0;   // mint/cyan glow (alpha = brightness)
    private static final int COMBO_CORE = 0xFFFFFFFF;   // white center line

    private static final SlashFx COMBO_CRESCENT = SlashFx.arc("slash/streaks")
            .color(COMBO_GLOW).core(COMBO_CORE)
            .radius(1.9f).arc(170f).width(0.95f)
            .layers(3).spread(0.22f)
            .lifetime(9).sweep(3);
    private static final SlashFx COMBO_CLAW = SlashFx.arc("slash/streaks")
            .color(COMBO_GLOW).core(COMBO_CORE)
            .radius(1.6f).arc(130f).width(0.7f).taper(SlashFx.Taper.COMET)
            .layers(4).spread(0.3f)
            .lifetime(10).sweep(3)
            .rotation(0f, 0f, 40f);                    // rising diagonal (left-low to right-high)
    private static final SlashFx COMBO_MOON = SlashFx.arc("slash/streaks")
            .color(COMBO_GLOW).core(COMBO_CORE)
            .radius(1.25f).arc(320f).width(0.6f)
            .layers(2).spread(0.16f)
            .lifetime(12).sweep(4)
            .rotation(0f, -80f, 0f);                   // stood up, facing the attacker
    private static final SlashFx COMBO_VORTEX = SlashFx.arc("slash/streaks")
            .color(COMBO_GLOW).core(COMBO_CORE)
            .radius(0.5f).endRadius(1.5f).arc(560f).width(0.8f)   // spiral, 1.5 turns
            .layers(3).spread(0.3f)
            .lifetime(15).sweep(7);
    private static final SlashFx COMBO_CUT = SlashFx.line("slash/smooth")
            .color(0xE0B8FFF4).core(COMBO_CORE)
            .radius(2.1f).width(0.14f)
            .lifetime(7).sweep(2);
    private static final ParticleFx COMBO_SPARKLE = ParticleFx.of("fx/spark")
            .color(0xFFE8FFFA).fadeOut()
            .size(0.1f).endSize(0.02f)
            .lifetime(12, 8).friction(0.85f)
            .glow().spin(20f).randomRotation();

    public static final MeleeCombo MELEE_COMBO = MeleeCombo.forAspect("Hand Of Order")
            .step(HandOfOrder::comboCrescent)
            .step(HandOfOrder::comboRisingClaws)
            .randomStep(HandOfOrder::comboMoon, HandOfOrder::comboVortex, HandOfOrder::comboThousandCuts);

    private static final AbilityMenu JURISDICTION = AbilityMenu.create("hand_of_order_jurisdiction")
            .title("Jurisdiction Area")
            .option("ability_menus/hand_of_order/judgement", "16 blocks",
                    20, -100, (player, sl) -> setJurisdiction(player, sl, 16))
            .option("ability_menus/hand_of_order/judgement", "12 blocks",
                    -30, -50, (player, sl) -> setJurisdiction(player, sl, 12))
            .option("ability_menus/hand_of_order/judgement", "8 blocks",
                    40, 0, (player, sl) -> setJurisdiction(player, sl, 8))
            .option("ability_menus/hand_of_order/judgement", "4 blocks",
                    -50, 50, (player, sl) -> setJurisdiction(player, sl, 4));

    @Mod.EventBusSubscriber(modid = ForgeRealm.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class HandOfOrderEvents{


        @SubscribeEvent
        public static void onHandOfOrderEventTick(TickEvent.PlayerTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            Player player = event.player;
            if (!(player.level() instanceof ServerLevel sl)) return;

            if (player.getPersistentData().contains("Hand_Of_order_damage_boost")) {

                int duration = player.getPersistentData().getInt("Hand_Of_order_damage_boost");
                if (duration > 0) {
                    player.getPersistentData().putInt("Hand_Of_order_damage_boost", duration - 1);
                }
            }

            if (player.getPersistentData().contains("Hand_Of_order_defense_boost")) {
                int duration = player.getPersistentData().getInt("Hand_Of_order_defense_boost");
                if (duration > 0) {
                    player.getPersistentData().putInt("Hand_Of_order_defense_boost", duration - 1);
                }
            }
            if (!SoulCore.getAspect(player).equals("Hand Of Order")) return;
        }
    }
    //Ability 1
    public static void handOfOrderBuff(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!canUseCharacteristic(player, bypassClassCheck)) return;
        if (SoulCore.getSoulEssence(player) < 250) return;
        if (SoulCore.getAscensionStage(player) < 0) return;

        SoulCore.setSoulEssence(player,SoulCore.getSoulEssence(player)-250);


        if (player.isShiftKeyDown()){
            int x = 10;
            if (player.getPersistentData().getInt("Hand_Of_order_damage_boost")>0){
                x+=32;
            }
            ScreenImages.hide(player, "my_pic_3223123123123");
            ScreenImages.show(player, "my_pic_3223123123123", "amber_material",
                    ScreenAnchor.TOP_LEFT, x, 10, 32, 70);

            player.getPersistentData().putInt("Hand_Of_order_defense_boost",  60);

            summonOrderHammer(player, sl);
        }else{
            int x = 10;
            if (player.getPersistentData().getInt("Hand_Of_order_defense_boost")>0){
                x+=32;
            }
            ScreenImages.hide(player, "my_pic_1123123123123");
            ScreenImages.show(player, "my_pic_1123123123123", "perfect_ruby_gemstone",
                    ScreenAnchor.TOP_LEFT, x, 10, 32, 70);

            player.getPersistentData().putInt("Hand_Of_order_damage_boost",  60);

            freezeMobsAround(player, sl);
        }
    }

    // Ability 1 (normal): every mob around you that you can see is frozen in ice. Not players, not your own pets.
    private static void freezeMobsAround(Player player, ServerLevel sl) {
        int frozen = 0;
        for (LivingEntity target : livingAround(player, sl, FREEZE_RANGE)) {
            if (!(target instanceof Mob)) continue;
            if (target instanceof TamableAnimal pet && pet.isOwnedBy(player)) continue;
            if (!player.hasLineOfSight(target)) continue;
            Freeze.apply(target, FREEZE_TICKS);
            frozen++;
        }
        if (frozen > 0) {
            sl.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.2f, 0.5f);
        }
    }

    // Ability 1 (sneaking): the hammer appears raised behind you, winds up and slams down in front of you.
    // Only the hammer itself is shown - no effects on the ground.
    private static void summonOrderHammer(Player player, ServerLevel sl) {
        float yaw = player.getYRot();
        double yawRad = Math.toRadians(yaw);
        Vec3 forward = new Vec3(-Math.sin(yawRad), 0, Math.cos(yawRad));
        Vec3 right = new Vec3(-Math.cos(yawRad), 0, -Math.sin(yawRad));
        Vec3 grip = player.position().add(0, HAMMER_GRIP_HEIGHT, 0).add(right.scale(0.35));

        // swing exactly far enough for the head to land on the ground in front of you (works on slopes and ledges)
        double reach = HAMMER_REACH_PX * HAMMER_SCALE / 16.0;
        double face = HAMMER_FACE_PX * HAMMER_SCALE / 16.0;
        double groundY = player.getY();
        float strikePitch = 90f;
        for (int i = 0; i < 3; i++) {
            strikePitch = hammerStrikePitch(grip.y - groundY, reach, face);
            double landing = hammerLandingDistance(strikePitch, reach, face);
            groundY = groundBelow(player, sl, grip.add(forward.scale(landing)), player.getY());
        }
        double distance = hammerLandingDistance(strikePitch, reach, face);
        Vec3 impact = new Vec3(grip.x + forward.x * distance, groundY, grip.z + forward.z * distance);

        ModelFx hammer = ORDER_HAMMER
                .key(0, ModelFx.pose().pitch(-20).scale(0.3f).alpha(0f))
                .key(6, ModelFx.pose().pitch(-35).scale(1f).alpha(1f), ModelFx.Ease.OUT_BACK)      // appears behind you
                .key(13, ModelFx.pose().pitch(-55), ModelFx.Ease.IN_OUT)                           // winds up
                .key(HAMMER_IMPACT_TICK, ModelFx.pose().pitch(strikePitch), ModelFx.Ease.IN)       // slams down
                .key(HAMMER_IMPACT_TICK + 2, ModelFx.pose().pitch(strikePitch - 5), ModelFx.Ease.OUT) // small bounce
                .key(HAMMER_IMPACT_TICK + 5, ModelFx.pose().pitch(strikePitch), ModelFx.Ease.IN);
        ParticleShapes.model(sl, hammer, grip, yaw, 0f, 0f);

        sl.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1f, 1.6f);
        Later.run(sl, HAMMER_IMPACT_TICK - 4, () -> sl.playSound(null, BlockPos.containing(grip),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2f, 0.5f));
        Later.run(sl, HAMMER_IMPACT_TICK, () -> hammerImpact(player, sl, impact));
    }

    private static void hammerImpact(Player player, ServerLevel sl, Vec3 impact) {
        if (!player.isAlive() || player.level() != sl) return;

        AABB area = new AABB(impact.x - HAMMER_RADIUS, impact.y - 1.0, impact.z - HAMMER_RADIUS,
                impact.x + HAMMER_RADIUS, impact.y + 3.0, impact.z + HAMMER_RADIUS);
        for (LivingEntity target : sl.getEntitiesOfClass(LivingEntity.class, area, e -> e != player && e.isAlive())) {
            double dx = target.getX() - impact.x;
            double dz = target.getZ() - impact.z;
            double hitRange = HAMMER_RADIUS + target.getBbWidth() * 0.5;
            if (dx * dx + dz * dz > hitRange * hitRange) continue;
            target.hurt(player.damageSources().playerAttack(player), HAMMER_DAMAGE);
            target.knockback(HAMMER_KNOCKBACK, -dx, -dz); // away from where it landed
        }
        sl.playSound(null, BlockPos.containing(impact), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.6f, 0.5f);
        sl.playSound(null, impact.x, impact.y, impact.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.8f, 1.3f);
    }

    /**
     * How far the hammer has to swing (degrees, 90 = handle level) for its striking face to touch the ground
     * 'height' blocks below the grip. Face middle = grip + reach * (sin p, cos p) + face * (cos p, -sin p)
     * in (forward, up), so we solve reach * cos p - face * sin p = -height.
     */
    private static float hammerStrikePitch(double height, double reach, double face) {
        double length = Math.sqrt(reach * reach + face * face);
        double pitch = Math.toDegrees(Math.acos(Mth.clamp(-height / length, -1.0, 1.0)))
                - Math.toDegrees(Math.atan2(face, reach));
        return (float) Mth.clamp(pitch, 60.0, 150.0);
    }

    /** How far in front of the grip the head lands at that pitch. */
    private static double hammerLandingDistance(float pitch, double reach, double face) {
        double rad = Math.toRadians(pitch);
        return reach * Math.sin(rad) + face * Math.cos(rad);
    }

    /** Top of the blocks under a point (looking from a bit above it), or 'fallback' if there is nothing below. */
    private static double groundBelow(Player player, ServerLevel sl, Vec3 point, double fallback) {
        Vec3 from = new Vec3(point.x, point.y + 1.5, point.z);
        Vec3 to = new Vec3(point.x, point.y - 8.0, point.z);
        BlockHitResult hit = sl.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.MISS ? fallback : hit.getLocation().y;
    }


    //Ability 2
    public static void handOfOrderDecree(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!canUseCharacteristic(player, bypassClassCheck)) return;
        if (SoulCore.getSoulEssence(player) < DECREE_COST) return;
        if (SoulCore.getAscensionStage(player) < 1) return;

        // essence is only spent once an option is picked (Esc = free)
        DECREE_MENU.open(player);
    }

    // Top: explosion around the caster. Caster is the explosion's source, so it isn't hurt;
    // ExplosionInteraction.NONE means no blocks break.
    private static void decreeExplosion(ServerPlayer player, ServerLevel sl) {
        if (!payEssence(player, DECREE_COST)) return;
        sl.explode(player, player.getX(), player.getY() + 0.5, player.getZ(),
                DECREE_EXPLOSION_POWER, Level.ExplosionInteraction.NONE);
    }

    private static void setJurisdiction(ServerPlayer player, ServerLevel sl, int range) {
        if (!payEssence(player, DECREE_COST)) return;

        player.sendSystemMessage(Component.literal("Jurisdiction: " + range + " blocks"));
        sl.playSound(null, player.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.5f, 0.5f);

        AABB area = player.getBoundingBox().inflate(range);

        List<LivingEntity> hits = sl.getEntitiesOfClass(
                LivingEntity.class,
                area,
                entity -> entity != player
                        && entity.isAlive()
                        && entity.distanceTo(player) <= range
        );

        int durationMult = (16-range)/4;
        if (durationMult==0){
            durationMult=1;
        }

        if (range<=16){
            for (LivingEntity livingEntity : hits){
                livingEntity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60*durationMult, 0));
            }

        }
        if (range<=12){
            for (LivingEntity livingEntity : hits){
                livingEntity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60*durationMult, 0));
            }
        }
        if (range<=8){
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60*durationMult, 2));
        }
        if (range<=4){
            for (LivingEntity livingEntity : hits){
                livingEntity.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 60*durationMult, 2));
            }
        }

        CustomParticles.particleCircle(sl,player.position(), range);

    }

    // Bottom left: speed boost
    private static void decreeSpeed(ServerPlayer player, ServerLevel sl) {
        if (!payEssence(player, DECREE_COST)) return;
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, DECREE_SPEED_TICKS, DECREE_SPEED_LEVEL));
        sl.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.1, player.getZ(), 20, 0.4, 0.05, 0.4, 0.05);
        sl.playSound(null, player.blockPosition(), SoundEvents.BREEZE_JUMP, SoundSource.PLAYERS, 1f, 1.2f);
    }

    // Bottom right: 30% chance of a huge hit on whoever is in front
    private static void decreeJudgement(ServerPlayer player, ServerLevel sl) {


        Level level = (Level) sl;
        LivingEntity target = getTarget(player,sl,level,32);
        if (target == null) {
            player.sendSystemMessage(Component.literal("No one stands before you."));
            return; // no essence spent
        }
        if (!payEssence(player, DECREE_COST)) return;

        if (player.getRandom().nextFloat() < DECREE_JUDGEMENT_CHANCE) {
            target.invulnerableTime = 0;
            target.hurt(player.damageSources().playerAttack(player), DECREE_JUDGEMENT_DAMAGE);
            sl.sendParticles(ParticleTypes.ENCHANTED_HIT, target.getX(), target.getY(0.5), target.getZ(), 30, 0.4, 0.6, 0.4, 0.3);
            sl.playSound(null, target.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1f, 0.6f);
            player.sendSystemMessage(Component.literal("Judgement!"));
        } else {
            sl.sendParticles(ParticleTypes.SMOKE, target.getX(), target.getY(0.5), target.getZ(), 15, 0.3, 0.5, 0.3, 0.02);
            sl.playSound(null, target.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.8f, 1.2f);
            player.sendSystemMessage(Component.literal("The judgement failed."));
        }
    }

    // First living entity the player is looking at, stopped by blocks
    private static LivingEntity findTargetInFront(Player player, ServerLevel sl, double range) {
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getLookAngle().scale(range));

        // don't hit through walls
        end = sl.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getLocation();

        LivingEntity closest = null;
        double closestDistance = Double.MAX_VALUE;
        AABB searchBox = new AABB(start, end).inflate(1.0);
        for (LivingEntity entity : sl.getEntitiesOfClass(LivingEntity.class, searchBox, e -> e != player && e.isAlive())) {
            var hit = entity.getBoundingBox().inflate(0.3).clip(start, end);
            if (hit.isPresent()) {
                double distance = start.distanceToSqr(hit.get());
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closest = entity;
                }
            }
        }
        return closest;
    }

    //Ability 3
    public static void handOfOrderEdict(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!canUseCharacteristic(player, bypassClassCheck)) return;
        if (SoulCore.getSoulEssence(player) < EDICT_COST) return;
        if (SoulCore.getAscensionStage(player) < 2) return;

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - EDICT_COST);

        Vec3 look = player.getLookAngle().normalize();
        Vec3 origin = player.getEyePosition().add(look.scale(0.4)).add(0, -0.25, 0);

        if (player.isShiftKeyDown()) {
            // Long range narrow cone
            double speed = EDICT_LONG_RANGE / WHITE_SPARK.lifetimeTicks(); // reach the end of the range
            ParticleShapes.cone(sl, WHITE_SPARK, origin, look, EDICT_LONG_ANGLE, 90, speed * 0.7, speed);

            Vec3 end = player.getEyePosition().add(look.scale(EDICT_LONG_RANGE));
            end = sl.clip(new ClipContext(player.getEyePosition(), end, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, player)).getLocation();
            ParticleShapes.line(sl, LANCE_CORE, origin, end, 3);

            sl.playSound(null, player.blockPosition(), SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.PLAYERS, 1f, 1.4f);

            for (LivingEntity target : entitiesInCone(player, sl, EDICT_LONG_RANGE, EDICT_LONG_ANGLE)) {
                target.hurt(player.damageSources().playerAttack(player), EDICT_LONG_DAMAGE);
                ParticleShapes.burst(sl, HIT_SHARD, target.getBoundingBox().getCenter(), 14, 0.1, 0.3);
            }
        } else {
//            // Close range wide cone
//            double speed = EDICT_CLOSE_RANGE / GOLD_SHARD.lifetimeTicks();
//            ParticleShapes.cone(sl, GOLD_SHARD, origin, look, EDICT_CLOSE_ANGLE, 140, speed * 0.3, speed);
//            ParticleShapes.cone(sl, GOLD_GLOW, origin, look, EDICT_CLOSE_ANGLE, 40, speed * 0.4, speed);
//
//            sl.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1f, 0.8f);
//
//            for (LivingEntity target : entitiesInCone(player, sl, EDICT_CLOSE_RANGE, EDICT_CLOSE_ANGLE)) {
//                target.hurt(player.damageSources().playerAttack(player), EDICT_CLOSE_DAMAGE);
//                target.knockback(EDICT_CLOSE_KNOCKBACK, player.getX() - target.getX(), player.getZ() - target.getZ());
//                ParticleShapes.burst(sl, HIT_SHARD, target.getBoundingBox().getCenter(), 10, 0.1, 0.25);
//            }

            // A key only changes what it names; everything else stays where the earlier keys left it.
            ModelFx sword = HAND_OF_ORDER_SWORD
                    .key(5, ModelFx.pose().pitch(90), ModelFx.Ease.IN)       // ticks 0-20: tips forward (blade points ahead, on its edge)
                    .key(10, ModelFx.pose().spin(90), ModelFx.Ease.IN)        // 20-30: quarter turn around its own length -> lies flat
                    .key(30, ModelFx.pose().forward(5), ModelFx.Ease.LINEAR)      // 30-40: lunges forward (still flat)
                    .key(40, ModelFx.pose().forward(12), ModelFx.Ease.LINEAR)    // 40-100: flies on
                    // at the same time as the flight (ticks 30-100): 4 full turns. yaw turns around the world's up,
                    // so it stays flat against the ground the whole time, like a thrown disc.
                    .during(30, 100, ModelFx.pose().yaw(1440));

            ParticleShapes.model(sl, sword, player.position().add(0,3,0), player.getYRot(), 0f, 0f);
        }
    }


    //Ability 3
    public static void handOfOrderAbility4(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!canUseCharacteristic(player, bypassClassCheck)) return;
        if (SoulCore.getSoulEssence(player) < 12000) return;
        if (SoulCore.getAscensionStage(player) < 3) return;

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 12000);

        if (!player.isShiftKeyDown()) {
            ParticleShapes.sphere(sl, MUSHROOMS, getPos(player,3), 0.35, 16);

        } else {
            ParticleShapes.sphere(sl, MUSHROOMS_V2, getPos(player,3), 0.35, 16);
        }
    }


    //Ability 4
    public static void handOfOrderAbility5(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!canUseCharacteristic(player, bypassClassCheck)) return;
        if (SoulCore.getSoulEssence(player) < 12000) return;
        if (SoulCore.getAscensionStage(player) < 4) return;

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 12000);

        if (!player.isShiftKeyDown()) {

            JURISDICTION.open(player);

        } else {
            player.sendSystemMessage(Component.literal("Wrong Button Buddy"));
        }
    }

    /**
     * Living entities inside a cone in front of the player that the player can see.
     * angleDegrees is the full opening angle. Big mobs count if any part of them is roughly inside.
     */

    private static Vec3 getPos(Player player, int distance) {
        Vec3 location = player.getEyePosition();
        Vec3 baseDirection = player.getLookAngle().normalize();

        Vec3 c = null;

        for (float j = 0; j <= distance; j += 0.5f) {
            c = location.add(baseDirection.scale(j));
        }
        return c;
    }

    private static List<LivingEntity> entitiesInCone(Player player, ServerLevel sl, double range, double angleDegrees) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        double halfAngle = Math.toRadians(angleDegrees / 2.0);

        List<LivingEntity> result = new ArrayList<>();
        for (LivingEntity entity : sl.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(range),
                e -> e != player && e.isAlive())) {
            Vec3 toTarget = entity.getBoundingBox().getCenter().subtract(eye);
            double distance = toTarget.length();
            double radius = entity.getBbWidth() * 0.5;
            if (distance - radius > range) continue;

            if (distance > 0.01) {
                double angle = Math.acos(Math.max(-1, Math.min(1, toTarget.normalize().dot(look))));
                double allowance = Math.atan2(radius + 0.3, distance); // widen a bit for the entity's size
                if (angle > halfAngle + allowance) continue;
            }
            if (!player.hasLineOfSight(entity)) continue;

            result.add(entity);
        }
        return result;
    }

    private static LivingEntity getTarget(Player player, ServerLevel sl, Level level, float distance){
        Vec3 start = player.getEyePosition();
        Vec3 direction = player.getLookAngle().normalize();
        Vec3 current = start;
        LivingEntity livingEntity = null;

        for (float i = 0; i < distance; i+=0.5f){
            current = current.add(direction);
            sl.sendParticles(ParticleTypes.END_ROD,
                    current.x, current.y, current.z, 1, 0, 0, 0, 0);

            List<LivingEntity> hits = level.getEntitiesOfClass(
                    LivingEntity.class, new AABB(current, current).inflate(0.5),
                    e -> e != player && e.isAlive());
            if(!hits.isEmpty()){
                livingEntity = hits.get(0);
                return livingEntity;
            }
        }
        return livingEntity;
    }

    //Ability 6
    public static void handOfOrderWords(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!canUseCharacteristic(player, bypassClassCheck)) return;
        if (SoulCore.getSoulEssence(player) < WORDS_COST) return;
        if (SoulCore.getAscensionStage(player) < 5) return;

        WORDS.open(player); // essence is paid inside the phrase that works
    }

    // "The sentence was God" - huge explosion, no block damage, the caster is hit too
    private static void wordsTheSentenceWasGod(ServerPlayer player, ServerLevel sl) {
        if (!payEssence(player, WORDS_COST)) return;
        Vec3 center = player.position().add(0, 0.5, 0);
        ParticleShapes.ring(sl, ORDER_WAVE, center, 1.0, 120, 0.9);
        ParticleShapes.burst(sl, GOLD_SHARD, center, 80, 0.4, 1.2);
        // null source = nobody is excluded, so the caster takes the blast as well
        sl.explode(null, center.x, center.y, center.z, GOD_EXPLOSION_POWER, Level.ExplosionInteraction.NONE);
    }

    // "Bethel Abraham" - everything around except the caster: Slowness 120 for 20 seconds
    private static void wordsBethelAbraham(ServerPlayer player, ServerLevel sl) {
        if (!payEssence(player, WORDS_COST)) return;
        for (LivingEntity target : livingAround(player, sl, BETHEL_RANGE)) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, BETHEL_SLOWNESS_TICKS, BETHEL_SLOWNESS_LEVEL));
            ParticleShapes.ring(sl, SEAL_MOTE, target.position().add(0, 0.2, 0), 0.8, 12, 0.0);
        }
        // wave that reaches the edge of the range in 20 ticks
        ParticleShapes.ring(sl, ORDER_WAVE.color(0xFF9FB8FF).fadeOut(), player.position().add(0, 0.3, 0),
                1.0, 160, BETHEL_RANGE / ORDER_WAVE.lifetimeTicks());
        sl.playSound(null, player.blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 2f, 0.5f);
    }

    // "Kneel" - everything around is slammed down and weakened
    private static void wordsKneel(ServerPlayer player, ServerLevel sl) {
        if (!payEssence(player, WORDS_COST)) return;
        for (LivingEntity target : livingAround(player, sl, KNEEL_RANGE)) {
            target.setDeltaMovement(target.getDeltaMovement().x * 0.2, -1.5, target.getDeltaMovement().z * 0.2);
            target.hurtMarked = true; // makes players' clients accept the push
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, KNEEL_TICKS, 3));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, KNEEL_TICKS, 2));
            ParticleShapes.cone(sl, HIT_SHARD, target.position().add(0, target.getBbHeight() + 0.5, 0),
                    new Vec3(0, -1, 0), 40, 10, 0.2, 0.4);
        }
        sl.playSound(null, player.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.5f, 0.5f);
    }

    // "Order restored" - caster fully healed and cleansed
    private static void wordsOrderRestored(ServerPlayer player, ServerLevel sl) {
        if (!payEssence(player, WORDS_COST)) return;
        player.setHealth(player.getMaxHealth());
        player.clearFire();
        List<MobEffectInstance> harmful = new ArrayList<>();
        for (MobEffectInstance effect : player.getActiveEffects()) {
            if (!effect.getEffect().value().isBeneficial()) harmful.add(effect);
        }
        for (MobEffectInstance effect : harmful) player.removeEffect(effect.getEffect());

        ParticleShapes.sphere(sl, GOLD_GLOW.lifetime(20, 10).gravity(-0.05f), player.position().add(0, 1, 0), 1.2, 40);
        sl.playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1f, 1.2f);
    }

    // "Judgement falls" - lightning on the nearest few targets
    private static void wordsJudgementFalls(ServerPlayer player, ServerLevel sl) {
        List<LivingEntity> targets = livingAround(player, sl, JUDGEMENT_FALLS_RANGE);
        if (targets.isEmpty()) {
            player.sendSystemMessage(Component.literal("There is no one to judge."));
            return; // nothing spent
        }
        if (!payEssence(player, WORDS_COST)) return;

        targets.sort((a, b) -> Double.compare(a.distanceToSqr(player), b.distanceToSqr(player)));
        for (int i = 0; i < Math.min(JUDGEMENT_FALLS_TARGETS, targets.size()); i++) {
            LivingEntity target = targets.get(i);
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(sl);
            if (bolt == null) continue;
            bolt.moveTo(target.getX(), target.getY(), target.getZ());
            bolt.setCause(player);
            sl.addFreshEntity(bolt);
        }
    }

    //Ability 7
    public static void handOfOrderBind(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!canUseCharacteristic(player, bypassClassCheck)) return;
        if (SoulCore.getSoulEssence(player) < BIND_COST) return;
        if (SoulCore.getAscensionStage(player) < 6) return;

        LivingEntity target = findTargetInFront(player, sl, BIND_RANGE);
        if (target == null) {
            player.sendSystemMessage(Component.literal("No one stands before you."));
            return; // nothing spent
        }

        double distance = player.distanceTo(target);
        BindTier tier = null;
        for (BindTier t : BIND_TIERS) {
            if (distance <= t.maxDistance()) {
                tier = t;
                break;
            }
        }
        if (tier == null) return;
        if (!payEssence(player, BIND_COST)) return;

        Root.apply(target, tier.ticks(), tier.restrictions());

        // chain of light from caster to target + a ring around it
        ParticleShapes.line(sl, BIND_CHAIN, player.getEyePosition().add(0, -0.3, 0), target.getBoundingBox().getCenter(), 4);
        ParticleShapes.ring(sl, BIND_CHAIN.size(0.2f), target.position().add(0, 0.1, 0),
                Math.max(0.8, target.getBbWidth()), 24, 0.0);
        sl.playSound(null, target.blockPosition(), SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 1.5f, 0.6f);

        player.sendSystemMessage(Component.literal("Bound: " + describe(tier.restrictions())));
        if (target instanceof Player boundPlayer) {
            boundPlayer.sendSystemMessage(Component.literal("You are bound by Order."));
        }
    }

    private static String describe(Set<RootRestriction> restrictions) {
        if (restrictions.containsAll(RootRestriction.EVERYTHING)) return "everything";
        List<String> names = new ArrayList<>();
        for (RootRestriction r : restrictions) names.add(r.name().toLowerCase().replace('_', ' '));
        return String.join(", ", names);
    }

    /** Every living thing within range except the caster. */
    private static List<LivingEntity> livingAround(Player player, ServerLevel sl, double range) {
        List<LivingEntity> result = new ArrayList<>();
        for (LivingEntity entity : sl.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(range),
                e -> e != player && e.isAlive())) {
            if (entity.distanceToSqr(player) <= range * range) result.add(entity);
        }
        return result;
    }

    // =========================
    // Melee combo hits
    // =========================

    /** Horizontal direction the player faces. */
    private static Vec3 flatLook(Player player) {
        return Vec3.directionFromRotation(0f, player.getYRot());
    }

    // Hit 1: wide crescent swinging around the target (left or right at random)
    private static void comboCrescent(ServerPlayer player, LivingEntity target, ServerLevel sl) {
        Vec3 hit = target.getBoundingBox().getCenter();
        Vec3 center = hit.subtract(flatLook(player).scale(1.1)); // arc's middle passes just behind the target
        // left or right swing, slightly tilted/angled each time
        SlashFx slash = COMBO_CRESCENT.varied(player.getRandom(), 12f, 8f, 18f);
        ParticleShapes.slash(sl, slash, center, player.getYRot(), 0f, 0f);
        sl.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.8f, 1.2f);
    }

    // Hit 2: several claw streaks rising diagonally
    private static void comboRisingClaws(ServerPlayer player, LivingEntity target, ServerLevel sl) {
        Vec3 hit = target.getBoundingBox().getCenter();
        Vec3 center = hit.subtract(flatLook(player).scale(0.8));
        // rising to the right or (mirrored) to the left, steeper or flatter
        SlashFx claws = COMBO_CLAW.varied(player.getRandom(), 10f, 10f, 20f);
        ParticleShapes.slash(sl, claws, center, player.getYRot(), 0f, 0f);
        sl.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.8f, 1.4f);
    }

    // Hit 3 (a): a crescent moon standing up around the target
    private static void comboMoon(ServerPlayer player, LivingEntity target, ServerLevel sl) {
        Vec3 hit = target.getBoundingBox().getCenter();
        // either direction, turned and leaned a little
        ParticleShapes.slash(sl, COMBO_MOON.varied(player.getRandom(), 25f, 12f, 0f), hit.add(0, 0.15, 0),
                player.getYRot(), 0f, 0f);
        ParticleShapes.burst(sl, COMBO_SPARKLE, hit, 12, 0.05, 0.2);
        sl.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.9f, 0.9f);
        sl.playSound(null, target.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 1.5f);
    }

    // Hit 3 (b): two spirals whirling around the target
    private static void comboVortex(ServerPlayer player, LivingEntity target, ServerLevel sl) {
        Vec3 base = target.position().add(0, 0.7, 0).add(flatLook(player).scale(0.3)); // a bit away from the attacker
        // starts at any angle, spins either way, both spirals always spin the same way
        var random = player.getRandom();
        SlashFx vortex = random.nextBoolean() ? COMBO_VORTEX : COMBO_VORTEX.mirrored();
        float start = random.nextFloat() * 360f;
        ParticleShapes.slash(sl, vortex.jittered(random, 0f, 0f, 8f), base, start, 0f, 6f);
        ParticleShapes.slash(sl, vortex.layers(2).delay(3).jittered(random, 0f, 0f, 8f), base.add(0, 0.5, 0),
                start + 180f, 0f, -8f);
        ParticleShapes.burst(sl, COMBO_SPARKLE, target.getBoundingBox().getCenter(), 20, 0.05, 0.3);
        sl.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1f, 0.6f);
    }

    // Hit 3 (c): a flurry of thin straight cuts through the target
    private static void comboThousandCuts(ServerPlayer player, LivingEntity target, ServerLevel sl) {
        Vec3 hit = target.getBoundingBox().getCenter();
        var random = player.getRandom();
        for (int i = 0; i < 7; i++) {
            Vec3 center = hit.add((random.nextDouble() - 0.5) * 0.6, (random.nextDouble() - 0.5) * 0.6,
                    (random.nextDouble() - 0.5) * 0.6);
            float yaw = player.getYRot() + (random.nextFloat() - 0.5f) * 80f;
            float pitch = (random.nextFloat() - 0.5f) * 100f;
            float roll = random.nextFloat() * 180f;
            ParticleShapes.slash(sl, COMBO_CUT.delay(i), center, yaw, pitch, roll); // one new cut every tick
        }
        sl.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1f, 1.3f);
        sl.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.6f, 1.8f);
    }

    private static boolean payEssence(Player player, float cost) {
        if (SoulCore.getSoulEssence(player) < cost) {
            player.sendSystemMessage(Component.literal("Not enough soul essence."));
            return false;
        }
        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - cost);
        return true;
    }

    private static boolean canUseCharacteristic(Player player, boolean dontCheck) {
        if (dontCheck) return true;
        return SoulCore.getAspect(player).equals("Hand Of Order");
    }
}