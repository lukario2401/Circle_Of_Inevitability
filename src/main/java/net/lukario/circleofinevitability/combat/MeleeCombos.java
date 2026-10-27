package net.lukario.circleofinevitability.combat;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.*;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.SoulCore;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.prince_of_abolition.PrinceOfAbolition;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.hand_of_order.HandOfOrder;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.knowledge_emperor.KnowledgeEmperor;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.illuminator.Illuminator;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.worm_of_time.WormOfTime;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.listener.Priority;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;

/**
 * All melee combos, one per pathway. To give another pathway a combo, define a MeleeCombo in its
 * class and add one register(...) line below.
 */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MeleeCombos {

    private static final Map<String, MeleeCombo> COMBOS = new HashMap<>();

    static {
        register(HandOfOrder.MELEE_COMBO);
        register(AttendantOfMysteries.MELEE_COMBO);
        register(Wanderer.MELEE_COMBO);
        register(FrenziedMage.MELEE_COMBO);
        register(PaleEmperor.MELEE_COMBO);
        register(WormOfTime.MELEE_COMBO);
        register(Conqueror.MELEE_COMBO);
        register(PrinceOfAbolition.MELEE_COMBO);
        register(KnowledgeEmperor.MELEE_COMBO);
        register(Illuminator.MELEE_COMBO);
    }

    private MeleeCombos() {}

    public static void register(MeleeCombo combo) {
        COMBOS.put(combo.pathway(), combo);
    }

    // LOW priority: runs after anything that cancels the attack (roots, protection...) had its say
    @SubscribeEvent(priority = Priority.LOW)
    public static void onMeleeComboAttack(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(event.getTarget() instanceof LivingEntity target) || !target.isAlive()) return;

        MeleeCombo combo = COMBOS.get(SoulCore.getPathway(player));
        if (combo != null) combo.onHit(player, target);
    }
}
