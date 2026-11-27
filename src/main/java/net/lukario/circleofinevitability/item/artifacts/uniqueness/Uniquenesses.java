package net.lukario.circleofinevitability.item.artifacts.uniqueness;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.combat.Healing;
import net.lukario.circleofinevitability.item.artifacts.SealedArtifacts;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.eventbus.api.listener.Priority;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * The events every Uniqueness shares: its protection and its might while carried, healing on kills, and the hooks its
 * downsides listen on (healing, sleep, menus, swallowed inputs, death). A downside never needs its own subscriber.
 */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Uniquenesses {
    /** Rules that say a hit comes from the uniqueness itself (its soldiers...): such hits are never softened. */
    private static final List<Predicate<Entity>> OWN_ATTACKERS = new ArrayList<>();

    private Uniquenesses() {}

    /** Hits from what this accepts get through a uniqueness's protection whole (its own creatures). */
    public static void addOwnAttacker(Predicate<Entity> isOwn) {
        OWN_ATTACKERS.add(isOwn);
    }

    /** The uniquenesses this player carries now, with their stacks. */
    public static List<Wielder> carried(ServerPlayer player) {
        List<Wielder> wielders = new ArrayList<>();
        if (!(player.level() instanceof ServerLevel level)) return wielders;
        for (Uniqueness uniqueness : Uniqueness.all()) {
            ItemStack stack = SealedArtifacts.carried(player, uniqueness.id());
            if (!stack.isEmpty()) wielders.add(uniqueness.wielder(level, player, stack));
        }
        return wielders;
    }

    /** The uniquenesses whose downsides act on this player now (carried or bound, not creative). */
    public static List<Wielder> suffering(ServerPlayer player) {
        List<Wielder> wielders = new ArrayList<>();
        if (!(player.level() instanceof ServerLevel level)) return wielders;
        for (Uniqueness uniqueness : Uniqueness.all()) {
            ItemStack stack = SealedArtifacts.carried(player, uniqueness.id());
            if (stack.isEmpty() && !uniqueness.bound(player)) continue;
            Wielder wielder = uniqueness.wielder(level, player, stack);
            if (wielder.suffering()) wielders.add(wielder);
        }
        return wielders;
    }

    private static boolean swallow(Player entity, Entity target) {
        if (!(entity instanceof ServerPlayer player)) return false;
        for (Wielder wielder : suffering(player))
            for (UniquenessDrawback drawback : wielder.uniqueness().drawbacks())
                if (UniquenessItem.active(drawback, wielder) && drawback.swallows(wielder, target)) return true;
        return false;
    }

    // ---------- protection and might

    @SubscribeEvent(priority = Priority.HIGH)
    public static void onUniquenessMight(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker) || attacker == event.getEntity()) return;
        for (Wielder wielder : carried(attacker)) event.setAmount(event.getAmount() * wielder.uniqueness().damageDealt());
    }

    @SubscribeEvent(priority = Priority.LOW)
    public static void onUniquenessProtection(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Entity attacker = event.getSource().getEntity();
        for (Predicate<Entity> own : OWN_ATTACKERS) if (attacker != null && own.test(attacker)) return;
        for (Wielder wielder : carried(player)) {
            if (event.getSource().is(wielder.uniqueness().damageType())) continue;
            event.setAmount(event.getAmount() * wielder.uniqueness().damageTaken());
        }
    }

    // ---------- kills and deaths

    @SubscribeEvent
    public static void onUniquenessDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (!(victim.level() instanceof ServerLevel level)) return;
        if (victim instanceof ServerPlayer dead) died(level, dead);
        if (!(event.getSource().getEntity() instanceof ServerPlayer killer) || killer == victim
                || victim instanceof ArmorStand || !killer.isAlive()) return;
        for (Wielder wielder : carried(killer)) {
            float heal = victim.getMaxHealth() * wielder.uniqueness().killHeal();
            if (heal > 0) Healing.apply(killer, heal);
            for (UniquenessDrawback drawback : wielder.uniqueness().drawbacks())
                if (UniquenessItem.active(drawback, wielder)) drawback.killed(wielder, victim);
        }
    }

    private static void died(ServerLevel level, ServerPlayer player) {
        for (Uniqueness uniqueness : Uniqueness.all()) {
            if (uniqueness.corruption(player) <= 0 && !uniqueness.bound(player)) continue;
            Wielder wielder = uniqueness.wielder(level, player, SealedArtifacts.carried(player, uniqueness.id()));
            for (UniquenessDrawback drawback : uniqueness.drawbacks()) {
                try {
                    if (drawback.appliesAt(wielder.corruptionLevel())) drawback.died(wielder);
                } catch (RuntimeException e) {
                    CircleOfInevitability.LOGGER.error("A {} downside failed on death", uniqueness.id(), e);
                }
            }
            uniqueness.reset(player);
        }
    }

    // ---------- the hooks downsides listen on

    @SubscribeEvent
    public static boolean onUniquenessHeal(LivingHealEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || Healing.isSpellHealing()) return false;
        float amount = event.getAmount();
        for (Wielder wielder : suffering(player))
            for (UniquenessDrawback drawback : wielder.uniqueness().drawbacks())
                if (UniquenessItem.active(drawback, wielder)) amount = drawback.healed(wielder, amount);
        if (amount <= 0) return true;
        event.setAmount(amount);
        return false;
    }

    @SubscribeEvent
    public static void onUniquenessSleep(PlayerSleepInBedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        for (Wielder wielder : suffering(player))
            for (UniquenessDrawback drawback : wielder.uniqueness().drawbacks()) {
                if (!UniquenessItem.active(drawback, wielder)) continue;
                Component refusal = drawback.refusesSleep(wielder);
                if (refusal != null) {
                    event.setResult(new Player.BedSleepingProblem(refusal));
                    return;
                }
            }
    }

    @SubscribeEvent
    public static void onUniquenessMenu(PlayerContainerEvent.Open event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        for (Wielder wielder : suffering(player))
            for (UniquenessDrawback drawback : wielder.uniqueness().drawbacks())
                if (UniquenessItem.active(drawback, wielder) && drawback.refusesMenu(wielder, event.getContainer())) {
                    player.closeContainer();
                    return;
                }
    }

    /** Put in a chest while bound: it comes straight back out. */
    @SubscribeEvent
    public static void onUniquenessMenuClosed(PlayerContainerEvent.Close event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) return;
        for (Uniqueness uniqueness : Uniqueness.all())
            if (uniqueness.bound(player) && SealedArtifacts.carried(player, uniqueness.id()).isEmpty())
                UniquenessItem.recall(uniqueness, level, player);
    }

    @SubscribeEvent
    public static boolean onUniquenessToss(ItemTossEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)
                || !(event.getEntity().getItem().getItem() instanceof UniquenessItem item)
                || !item.uniqueness().bound(player)) return false;
        player.getInventory().placeItemBackInInventory(event.getEntity().getItem().copy());
        player.sendOverlayMessage(Component.literal(item.uniqueness().name() + " will not leave your hand."));
        return true;
    }

    @SubscribeEvent
    public static boolean onUniquenessAttack(AttackEntityEvent event) {
        return swallow(event.getEntity(), event.getTarget());
    }

    @SubscribeEvent
    public static boolean onUniquenessUseItem(PlayerInteractEvent.RightClickItem event) {
        return swallow(event.getEntity(), null);
    }

    @SubscribeEvent
    public static boolean onUniquenessUseBlock(PlayerInteractEvent.RightClickBlock event) {
        return swallow(event.getEntity(), null);
    }

    @SubscribeEvent
    public static boolean onUniquenessBreakBlock(PlayerInteractEvent.LeftClickBlock event) {
        return swallow(event.getEntity(), null);
    }
}
