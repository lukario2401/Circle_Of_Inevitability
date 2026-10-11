package net.lukario.circleofinevitability.blasphemy_slate.soul_abilities.beyonder_characteristics;

import net.lukario.circleofinevitability.combat.MeleeCombo;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.particles.fx.ParticleFx;
import net.lukario.circleofinevitability.particles.fx.ParticleShapes;
import net.lukario.circleofinevitability.particles.fx.SlashFx;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonder;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.SoulCore;
import net.lukario.circleofinevitability.status.FallGuard;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class WormOfTime {

    private static final String PATHWAY = "Worm Of Time";

    private static final String LIFE_THEFT_VULNERABILITY = "word_of_time_life_theft_vulnerability";

    private static final int COMBO_HEAD = 0xF2F4F2C9;   // cream front
    private static final int COMBO_MID = 0xEE52E3C4;    // mint cyan middle
    private static final int COMBO_TAIL = 0xA05B3B33;   // dark brown fading tail

    private static final SlashFx COMBO_RING = SlashFx.arc("slash/band")
            .color(COMBO_MID).tailColor(COMBO_TAIL).headColor(COMBO_HEAD)
            .translucent()
            .radius(3.6f).arc(320f).width(1f).taper(SlashFx.Taper.COMET)
            .layers(4).spread(0.28f)
            .lifetime(12).sweep(5).spin(10f);             // draws itself, keeps spinning, then wipes away



    private static final SlashFx COMBO_TILTED_RING = COMBO_RING
            .radius(2.4f).arc(-270f).width(1.2f)          // negative arc = sweeps the other way
            .sweep(4).spin(-8f)
            .rotation(0f, 0, -45);                     // big arch over and around you, tilted


    private static final SlashFx COMBO_GROUND_SPIRAL = COMBO_RING.flat()
            .radius(1.2f).endRadius(3.6f).arc(540f).width(1.3f)   // lies on the ground, winds outward
            .spread(0.4f)
            .lifetime(14).sweep(6).spin(9f);
    private static final SlashFx COMBO_CRYSTAL = SlashFx.line("slash/crystal")
            .color(0xC89CFFE6).core(0x70FFFFFF)
            .radius(1.9f).width(0.32f).taper(SlashFx.Taper.CRESCENT)   // pointed at both ends
            .lifetime(16).sweep(2).fromCenter();                       // shoots out, holds, fades
    private static final ParticleFx COMBO_STAR = ParticleFx.of("fx/spark")
            .color(0xFFB8FFEE).fadeOut()
            .size(0.12f).endSize(0.03f).sizeRandom(0.4f)
            .lifetime(28, 14).friction(0.96f)
            .glow().spin(12f).randomRotation();
    private static final ParticleFx COMBO_STAR_WHITE = COMBO_STAR.color(0xFFFFFFFF).fadeOut();

    public static final MeleeCombo MELEE_COMBO = MeleeCombo.forPathway(PATHWAY)
            .step(WormOfTime::comboRing)
            .step(WormOfTime::comboTiltedRing)
            .step(WormOfTime::comboTiltedRing)
            .step(WormOfTime::comboTiltedRing)
            .step(WormOfTime::comboCrystalCross)
            .randomStep(WormOfTime::comboGroundSpiral, WormOfTime::comboCrystalCross);

    public static void wormOfTimeAbility1(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!canUseCharacteristic(player, bypassClassCheck)) return;
        if (SoulCore.getSpirituality(player) < 250) return;
        if (!Beyonder.hasSequence(player, 9)) return;

        SoulCore.setSpirituality(player,SoulCore.getSpirituality(player)-250);


        if (!player.isShiftKeyDown()){

            LivingEntity livingEntity = Spells.aimEnemy(player,sl,24);
            if (livingEntity == null) return;
            int hitCount = livingEntity.getPersistentData().getIntOr(LIFE_THEFT_VULNERABILITY, 0);
            if (hitCount==0)hitCount=1;
            int healthStealAmount = 15*hitCount;

            if (livingEntity.getHealth()>healthStealAmount){
                livingEntity.setHealth(livingEntity.getHealth()-healthStealAmount);
                player.sendSystemMessage(Component.literal(""+hitCount));
                player.setHealth(player.getHealth()+healthStealAmount);
            }else{
                player.setHealth((player.getHealth()+livingEntity.getHealth()));
                livingEntity.kill(sl);
            }
            livingEntity.getPersistentData().putInt(LIFE_THEFT_VULNERABILITY,hitCount+1);

        }else{
            if (Root.has(player, RootRestriction.TELEPORT)) {
                player.sendSystemMessage(Component.literal("You are held in place."));
                return;
            }

            Vec3 there = Spells.blinkSpot(player, sl, 32);

            LivingEntity livingEntity = Spells.aimEnemy(player,sl,32);
            if (livingEntity!=null){
                there=livingEntity.position();
            }

            if (there == null) {
                player.sendSystemMessage(Component.literal("There is no room for you there."));
                return;                                                  // nothing spent
            }
            player.teleportTo(there.x, there.y, there.z);
            FallGuard.protect(player, 200);

        }
    }


    private static void comboRing(ServerPlayer player, LivingEntity target, ServerLevel sl) {
        Vec3 center = player.position().add(0, 0.9, 0);
        // sweeps either way, starts from a different side each time, slightly tilted
        SlashFx ring = COMBO_RING.varied(player.getRandom(), 60f, 8f, 12f);
        ParticleShapes.slash(sl, ring, center, player.getYRot(), 0f, 0f);
        sl.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1f, 0.8f);
    }

    // Hit 2: a tilted, almost upright ring around the player
    private static void comboTiltedRing(ServerPlayer player, LivingEntity target, ServerLevel sl) {
        Vec3 center = player.position().add(0, 1.1, 0);
        // tilted to the left or right side (mirrored), leaning a bit more or less each time
        SlashFx ring = COMBO_TILTED_RING.varied(player.getRandom(), 45f, 45f, 45f);
        ParticleShapes.slash(sl, ring, center, player.getYRot(), 0f, 0f);
        sl.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1f, 1.1f);
    }


    // Hit 3 (a): spiral whirling on the ground around the player
    private static void comboGroundSpiral(ServerPlayer player, LivingEntity target, ServerLevel sl) {
        Vec3 center = player.position().add(0, 0.15, 0);
        // starts at any angle and spins either way (both spirals the same way)
        RandomSource random = player.getRandom();
        SlashFx spiral = random.nextBoolean() ? COMBO_GROUND_SPIRAL : COMBO_GROUND_SPIRAL.mirrored();
        float start = random.nextFloat() * 360f;
        ParticleShapes.slash(sl, spiral, center, start, 0f, 0f);
        ParticleShapes.slash(sl, spiral.layers(2).radius(0.8f).endRadius(2.6f).delay(3),
                center.add(0, 0.05, 0), start + 180f, 0f, 0f);   // second, smaller spiral
        sl.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1f, 0.6f);
    }

    // Hit 3 (b): crossed crystal blades in an X on the target + a swirling galaxy of sparkles under it
    private static void comboCrystalCross(ServerPlayer player, LivingEntity target, ServerLevel sl) {
        Vec3 hit = target.getBoundingBox().getCenter();
        RandomSource random = player.getRandom();
        float xTurn = (random.nextFloat() * 2f - 1f) * 25f;   // the whole X turned a bit
        float xOpen = 35f + random.nextFloat() * 20f;          // wide or narrow X
        for (int bundle = 0; bundle < 2; bundle++) {
            float baseRoll = xTurn + (bundle == 0 ? xOpen : -xOpen);   // the two diagonals
            for (int i = 0; i < 4; i++) {
                Vec3 center = hit.add((random.nextDouble() - 0.5) * 0.4, (random.nextDouble() - 0.5) * 0.4,
                        (random.nextDouble() - 0.5) * 0.4);
                SlashFx blade = COMBO_CRYSTAL
                        .radius(1.5f + random.nextFloat() * 0.7f)
                        .width(0.24f + random.nextFloat() * 0.14f)
                        .delay(bundle * 2 + i / 2);      // blades appear in quick steps
                float yaw = player.getYRot() + (random.nextFloat() - 0.5f) * 20f;
                float roll = baseRoll + (random.nextFloat() - 0.5f) * 16f;
                ParticleShapes.slash(sl, blade, center, yaw, 0f, roll);
            }
        }
        Vec3 ground = target.position().add(0, 0.15, 0);
        ParticleShapes.spiral(sl, COMBO_STAR, ground, 4, 220, 0.6, 4.0, 0.8, 0.06);
        ParticleShapes.spiral(sl, COMBO_STAR_WHITE, ground, 4, 90, 0.6, 4.0, 0.8, 0.06);
        sl.playSound(null, target.blockPosition(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 1.2f, 1.2f);
        sl.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1f, 1f);
    }

    private static boolean canUseCharacteristic(Player player, boolean bypassClassCheck){
        if (bypassClassCheck) return true;
        return "Worm Of Time".equals(SoulCore.getPathway(player));
    }
}
