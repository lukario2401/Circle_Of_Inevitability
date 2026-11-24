package net.lukario.circleofinevitability.ritual;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Physique;
import net.lukario.circleofinevitability.combat.Spells;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

record RitualBlessing(String name, String perRank, Optional<Holder<Attribute>> attribute, double amount,
                      AttributeModifier.Operation operation, List<Stage> stages) {
    private static final Identifier LEGACY_ID = Physique.id("ritual_blessing");
    private static final Codec<AttributeModifier.Operation> OP = AttributeModifier.Operation.CODEC;
    static final Codec<RitualBlessing> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("name").forGetter(RitualBlessing::name),
            Codec.STRING.fieldOf("per_rank").forGetter(RitualBlessing::perRank),
            Attribute.CODEC.optionalFieldOf("attribute").forGetter(RitualBlessing::attribute),
            Codec.DOUBLE.optionalFieldOf("amount", 0.0).forGetter(RitualBlessing::amount),
            OP.optionalFieldOf("operation", AttributeModifier.Operation.ADD_VALUE).forGetter(RitualBlessing::operation),
            Stage.CODEC.listOf().optionalFieldOf("stages", List.of()).forGetter(RitualBlessing::stages)
    ).apply(i, RitualBlessing::new));

    record Modifier(Holder<Attribute> attribute, double amount, AttributeModifier.Operation operation) {
        static final Codec<Modifier> CODEC = RecordCodecBuilder.create(i -> i.group(
                Attribute.CODEC.fieldOf("attribute").forGetter(Modifier::attribute),
                Codec.DOUBLE.fieldOf("amount").forGetter(Modifier::amount),
                OP.optionalFieldOf("operation", AttributeModifier.Operation.ADD_VALUE).forGetter(Modifier::operation)
        ).apply(i, Modifier::new));
    }

    enum Condition implements StringRepresentable {
        DANGER, HUNGRY, WOUNDED, DARK, SUBMERGED, SPRINTING, FALLING;
        public String getSerializedName() { return name().toLowerCase(Locale.ROOT); }
        boolean met(ServerPlayer p, LivingEntity enemy) {
            return switch (this) {
                case DANGER -> enemy != null;
                case HUNGRY -> p.getFoodData().getFoodLevel() < 16;
                case WOUNDED -> p.getHealth() < p.getMaxHealth() * 0.75;
                case DARK -> p.level().getMaxLocalRawBrightness(p.blockPosition()) < 8;
                case SUBMERGED -> p.isUnderWater();
                case SPRINTING -> p.isSprinting();
                case FALLING -> p.fallDistance > 3;
            };
        }
    }

    record Pulse(Condition condition, Holder<MobEffect> effect, int ticks, boolean targetEnemy) {
        static final Codec<Pulse> CODEC = RecordCodecBuilder.create(i -> i.group(
                StringRepresentable.fromEnum(Condition::values).fieldOf("when").forGetter(Pulse::condition),
                MobEffect.CODEC.fieldOf("effect").forGetter(Pulse::effect),
                Codec.intRange(1, 600).fieldOf("ticks").forGetter(Pulse::ticks),
                Codec.BOOL.optionalFieldOf("target_enemy", false).forGetter(Pulse::targetEnemy)
        ).apply(i, Pulse::new));
    }

    record Stage(int rank, String description, List<Modifier> modifiers, List<Pulse> pulses) {
        static final Codec<Stage> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.intRange(1, 7).fieldOf("rank").forGetter(Stage::rank),
                Codec.STRING.fieldOf("description").forGetter(Stage::description),
                Modifier.CODEC.listOf().optionalFieldOf("modifiers", List.of()).forGetter(Stage::modifiers),
                Pulse.CODEC.listOf().optionalFieldOf("pulses", List.of()).forGetter(Stage::pulses)
        ).apply(i, Stage::new));
    }

    void apply(ServerPlayer player, int rank) {
        attribute.ifPresent(a -> Physique.keep(player, a, LEGACY_ID, amount * Math.min(rank, Worship.MAX_RANK), operation, rank > 0));
        for (Stage stage : stages) {
            for (Modifier mod : stage.modifiers) {
                Identifier id = Physique.id("ritual_stage_" + stage.rank);
                Physique.keep(player, mod.attribute, id, mod.amount, mod.operation, rank >= stage.rank);
            }
        }
    }

    /** Only one eligible pulse per interval. Rotate candidates so combat cannot starve recovery forever. */
    boolean pulse(ServerPlayer p, CompoundTag data, int rank) {
        List<Pulse> pulses = stages.stream().filter(s -> s.rank <= rank).flatMap(s -> s.pulses.stream()).toList();
        if (pulses.isEmpty()) return false;
        LivingEntity hostile = p.level().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(12),
                        e -> (e instanceof Enemy || e.getTarget() == p) && e.distanceToSqr(p) <= 144 && Spells.isEnemy(p, e)).stream()
                .min(Comparator.comparingDouble(p::distanceToSqr)).orElse(null);
        int cursor = Math.floorMod(data.getIntOr("pulse_cursor", 0), pulses.size());
        for (int i = 0; i < pulses.size(); i++) {
            int index = (cursor + i) % pulses.size();
            Pulse pulse = pulses.get(index);
            LivingEntity target = pulse.targetEnemy ? hostile : p;
            if (target == null || !pulse.condition.met(p, hostile) || target.hasEffect(pulse.effect)) continue;
            if (!target.addEffect(new MobEffectInstance(pulse.effect, pulse.ticks, 0, true, false, true))) continue;
            data.putInt("pulse_cursor", index + 1);
            if (target == p) {
                data.putString("own_effect", pulse.effect.unwrapKey().orElseThrow().identifier().toString());
                data.putLong("effect_end_play_tick", (long) p.getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME)) + pulse.ticks);
            }
            return true;
        }
        return false;
    }

    static void clearPulse(ServerPlayer p, CompoundTag data) {
        if (!data.contains("own_effect")) return;
        BuiltInRegistries.MOB_EFFECT.get(Identifier.parse(data.getStringOr("own_effect", ""))).ifPresent(effect -> {
            var active = p.getEffect(effect);
            if (active != null && active.getAmplifier() == 0 && active.isAmbient() && !active.isVisible()
                    && active.getDuration() <= data.getLongOr("effect_end_play_tick", 0) - p.getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME)) + 1) p.removeEffect(effect);
        });
        data.remove("own_effect");
    }
}
