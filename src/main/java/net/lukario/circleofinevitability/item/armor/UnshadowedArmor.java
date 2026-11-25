package net.lukario.circleofinevitability.item.armor;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.equipment.*;
import java.util.Map;

/**
 * Vestments of the Unshadowed (Sun pathway). Three orbiting Spears of Light strike from further away than the
 * Inevitability tentacles but for less, purify undead for double damage and fire, and the full set casts off
 * Blindness and Darkness.
 */
public final class UnshadowedArmor {
    public static final String SET = "unshadowed";
    public static final ArmorMaterial MATERIAL = new ArmorMaterial(42,
            Map.of(ArmorType.HELMET, 3, ArmorType.CHESTPLATE, 8, ArmorType.LEGGINGS, 6, ArmorType.BOOTS, 3),
            18, SoundEvents.ARMOR_EQUIP_GOLD, 3f, .1f,
            TagKey.create(Registries.ITEM, InevitabilityArmor.id("repairs_unshadowed_armor")),
            ResourceKey.create(EquipmentAssets.ROOT_ID, InevitabilityArmor.id(SET)));
    public static final TentacleCombat.Profile CHEST = new TentacleCombat.Profile(5, 5, 30, 8, 16);
    public static final TentacleCombat.Profile FULL_SET = new TentacleCombat.Profile(6, 7, 20, 8, 16);
    public static final float UNDEAD_MULTIPLIER = 2;
    public static final int UNDEAD_BURN_SECONDS = 4;
    /** Purification: undead take double damage and burn; the hit flashes with light. */
    public static final TentacleCombat.Impact IMPACT = new TentacleCombat.Impact() {
        @Override public float damage(LivingEntity wearer, LivingEntity target, float base) {
            return target.is(EntityTypeTags.UNDEAD) ? base * UNDEAD_MULTIPLIER : base;
        }
        @Override public void landed(ServerLevel level, LivingEntity wearer, LivingEntity target) {
            if (target.is(EntityTypeTags.UNDEAD)) target.igniteForSeconds(UNDEAD_BURN_SECONDS);
            level.sendParticles(ParticleTypes.END_ROD, target.getX(), target.getY() + target.getBbHeight() * .5,
                    target.getZ(), 14, .25, .35, .25, .06);
            level.playSound(null, target.blockPosition(), SoundEvents.TRIDENT_HIT, SoundSource.PLAYERS, .8f, 1.35f);
        }
    };
    private UnshadowedArmor() {}

    public static boolean wearing(LivingEntity entity, EquipmentSlot slot) {
        return entity.getItemBySlot(slot).getItem() instanceof TentacleArmorItem item
                && item.setId().equals(SET) && item.slot() == slot;
    }
    public static boolean fullSet(LivingEntity entity) {
        for (EquipmentSlot slot : InevitabilityArmor.SLOTS) if (!wearing(entity, slot)) return false;
        return true;
    }
    /** Full-set bonus: the Unshadowed cannot be blinded or swallowed by darkness. */
    public static void cleanse(LivingEntity wearer) {
        if (wearer.hasEffect(MobEffects.BLINDNESS)) wearer.removeEffect(MobEffects.BLINDNESS);
        if (wearer.hasEffect(MobEffects.DARKNESS)) wearer.removeEffect(MobEffects.DARKNESS);
    }
}
