package net.lukario.circleofinevitability.ritual;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;

/** Preserve normal melee damage rules while identifying blows actually delivered with the ritual knife. */
public final class SacrificialKnifeItem extends Item {
    public SacrificialKnifeItem(Properties properties) {
        super(properties);
    }

    @Override
    public DamageSource getItemDamageSource(LivingEntity attacker) {
        return new KnifeStrike(attacker);
    }

    static boolean isStrike(DamageSource source) {
        return source instanceof KnifeStrike;
    }

    private static final class KnifeStrike extends DamageSource {
        KnifeStrike(LivingEntity attacker) {
            super(attacker.createDamageSource().typeHolder(), attacker);
        }
    }
}
