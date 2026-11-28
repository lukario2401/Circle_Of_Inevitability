package net.lukario.circleofinevitability.combat;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonder;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.SoulCore;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.ToDoubleFunction;

/**
 * Territory: one home ground per owner, with a bonus for the owner while they stand in it. A Shaman's Totem Territory
 * (docs/pathways/observer/OBSERVER.md), a sanctuary, a camp, any "my ground" power. A Zone underneath (combat/Zone),
 * opened under one name, so an owner has one Territory: opening another closes the old one.
 *
 *   Territory.at(ground, 16)
 *           .height(6)                                   // blocks above the ground it still covers (default 6)
 *           .onEnter(enemy -> ...)                       // an enemy walked in (the intruder chime)
 *           .onPulse(ticksOpen -> ...)                   // every second: the look of it
 *           .onEnd(() -> ...)                            // it closed: a new one, death, logout, Territory.close
 *           .open(player, sl);                           // lasts until then
 *
 *   Territory.isOpen(owner)    Territory.center(owner)    Territory.radius(owner)
 *   Territory.inside(owner, entity)                      // is this one in the owner's Territory
 *   Territory.close(owner)
 *
 * The owner's bonus inside it, for every Pathway that opens one (once, in a static block; each says how much for this
 * owner, so a rule can grow with the Sequence or be only for one Pathway):
 *
 *   Territory.addCostScale(player -> 0.85)               // every ability costs 15% less in your own Territory
 *   Territory.addRegen(player -> 0.30)                   // spirituality comes back 30% faster in it
 *
 * Not saved: a restart closes it (as every Zone).
 *
 * The guide with examples for all of these tools: docs/SPELL_KIT.md
 */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Territory {

    /** The Zone name every Territory is opened under. */
    public static final String NAME = "territory";

    private static final List<ToDoubleFunction<Player>> COST_SCALES = new ArrayList<>();
    private static final List<ToDoubleFunction<Player>> REGENS = new ArrayList<>();

    static {
        Spells.addCostScale(player -> {
            if (COST_SCALES.isEmpty() || !inside(player, player)) return 1f;
            double scale = 1.0;
            for (ToDoubleFunction<Player> rule : COST_SCALES) scale *= Math.max(0.0, rule.applyAsDouble(player));
            return (float) scale;
        });
    }

    private final Vec3 ground;
    private final double radius;
    private double height = 6.0;
    private Consumer<LivingEntity> onEnter;
    private IntConsumer onPulse;
    private Runnable onEnd;

    private Territory(Vec3 ground, double radius) {
        this.ground = ground;
        this.radius = radius;
    }

    // =========================
    // Opening one
    // =========================

    public static Territory at(Vec3 ground, double radius) {
        return new Territory(ground, radius);
    }

    public Territory height(double blocks) { this.height = blocks; return this; }

    public Territory onEnter(Consumer<LivingEntity> action) { this.onEnter = action; return this; }

    public Territory onPulse(IntConsumer action) { this.onPulse = action; return this; }

    public Territory onEnd(Runnable action) { this.onEnd = action; return this; }

    /** Opens it for 'owner' (closing the one they had), until a new one, their death, logout or close. */
    public void open(LivingEntity owner, ServerLevel sl) {
        close(owner);
        int[] open = {0};
        Zone.at(ground, radius)
                .height(height)
                .lasts(Integer.MAX_VALUE / 4)
                .every(20)
                .onEnter(enemy -> {
                    if (onEnter != null) onEnter.accept(enemy);
                })
                .onPulse(left -> {
                    open[0] += 20;
                    if (onPulse != null) onPulse.accept(open[0]);
                })
                .onEnd(() -> {
                    if (onEnd != null) onEnd.run();
                })
                .open(owner, sl, NAME);
    }

    // =========================
    // Asking
    // =========================

    public static void addCostScale(ToDoubleFunction<Player> rule) {
        COST_SCALES.add(rule);
    }

    public static void addRegen(ToDoubleFunction<Player> rule) {
        REGENS.add(rule);
    }

    public static boolean isOpen(LivingEntity owner) {
        return Zone.isOpen(owner, NAME);
    }

    /** Its middle on the ground, or null. */
    public static Vec3 center(LivingEntity owner) {
        return isOpen(owner) ? Zone.centerOf(owner, NAME) : null;
    }

    public static double radius(LivingEntity owner) {
        return isOpen(owner) ? Zone.radiusOf(owner, NAME) : 0.0;
    }

    /** True while 'entity' stands in the owner's Territory. */
    public static boolean inside(LivingEntity owner, Entity entity) {
        return entity.level() == owner.level() && isOpen(owner) && Zone.covers(owner, NAME, entity.position());
    }

    public static void close(LivingEntity owner) {
        if (isOpen(owner)) Zone.close(owner, NAME);
    }

    // =========================
    // Inside
    // =========================

    /** Once a second: the owner's faster spirituality in their own Territory. */
    @SubscribeEvent
    public static void onTerritoryPlayerTick(TickEvent.PlayerTickEvent.Post event) {
        Player player = event.player();
        if (REGENS.isEmpty() || player.tickCount % 20 != 0 || !(player.level() instanceof ServerLevel)) return;
        if (!inside(player, player)) return;
        double share = 0.0;
        for (ToDoubleFunction<Player> rule : REGENS) share += Math.max(0.0, rule.applyAsDouble(player));
        if (share <= 0.0) return;
        float extra = (float) (Beyonder.spiritualityRegenPerTick(player) * 20f * share);
        SoulCore.setSpiritualityQuietly(player, Math.min(Beyonder.maxSpirituality(player), SoulCore.getSpirituality(player) + extra));
    }
}
