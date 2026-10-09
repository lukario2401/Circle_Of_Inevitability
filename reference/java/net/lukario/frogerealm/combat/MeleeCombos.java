package net.lukario.frogerealm.combat;

import net.lukario.frogerealm.ForgeRealm;
import net.lukario.frogerealm.shadow_slave.soul_abilities.beyonder_characteristics.AttendantOfMysteries;
import net.lukario.frogerealm.shadow_slave.soul_abilities.beyonder_characteristics.HandOfOrder;
import net.lukario.frogerealm.shadow_slave.soul_abilities.beyonder_characteristics.KeyOfStars;
import net.lukario.frogerealm.shadow_slave.soul_abilities.beyonder_characteristics.PaleEmperor;
import net.lukario.frogerealm.shadow_slave.soul_abilities.beyonder_characteristics.PrinceOfAbolition;
import net.lukario.frogerealm.shadow_slave.soul_shards.SoulCore;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;

/**
 * All melee combos, one per aspect. To give another aspect a combo, define a MeleeCombo in its
 * class and add one register(...) line below.
 */
@Mod.EventBusSubscriber(modid = ForgeRealm.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MeleeCombos {

    private static final Map<String, MeleeCombo> COMBOS = new HashMap<>();

    static {
        register(HandOfOrder.MELEE_COMBO);
        register(AttendantOfMysteries.MELEE_COMBO);
        register(KeyOfStars.MELEE_COMBO);
        register(PrinceOfAbolition.MELEE_COMBO);
        register(PaleEmperor.MELEE_COMBO);
    }

    private MeleeCombos() {}

    public static void register(MeleeCombo combo) {
        COMBOS.put(combo.aspect(), combo);
    }

    // LOW priority: runs after anything that cancels the attack (roots, protection...) had its say
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onMeleeComboAttack(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(event.getTarget() instanceof LivingEntity target) || !target.isAlive()) return;

        MeleeCombo combo = COMBOS.get(SoulCore.getAspect(player));
        if (combo != null) combo.onHit(player, target);
    }
}
