package net.lukario.frogerealm.client.animation;

import net.lukario.frogerealm.ForgeRealm;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Which player is playing which swing right now, on this client.
 *
 * Every client tick it looks at each player's vanilla swing state (swinging / swingTime, which vanilla already
 * syncs for other players), so a new swing is noticed with no extra packet. The local player's swing only counts
 * if it came from the attack key that tick (not from dropping an item or using a block). A new swing asks
 * SwingRules for a rule; the rule's sequence picks the next swing id. No rule or no definition = vanilla swing.
 *
 * FirstPersonSwing and ThirdPersonSwing ask poseFor(...) every frame; null means "draw vanilla".
 */
@Mod.EventBusSubscriber(modid = ForgeRealm.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class SwingPlayer {

    /**
     * One swing being played: what, for how many ticks, since when, with which item in hand, and the pose it
     * blends from (where the previous swing was when this one interrupted it; null = start from rest).
     */
    private record Playing(SwingAnimation animation, float ticks, long start, Item item, boolean fromSwing, SwingAnimation.Pose from) {}

    /** Where a player is in their rule's sequence. */
    private static final class Cycle {
        SwingRule rule;
        int next;
        long lastSwing;
    }

    private static final int NOT_SWINGING = -2;

    private static final Map<Integer, Playing> PLAYING = new HashMap<>();   // entity id -> swing
    private static final Map<Integer, Cycle> CYCLES = new HashMap<>();      // entity id -> sequence position
    private static final Map<Integer, Integer> LAST_SWING_TIME = new HashMap<>(); // entity id -> swingTime last tick
    private static long clientTicks;
    private static boolean attackClicked;
    private static boolean loggedError;

    private SwingPlayer() {}

    /**
     * The pose 'player' should be drawn in this frame, or null to draw vanilla. Null when nothing plays, when the
     * item in the main hand changed since the swing started, or when the swing is over.
     */
    public static SwingAnimation.Pose poseFor(Player player, float partialTick) {
        if (player == null) return null;
        Playing playing = PLAYING.get(player.getId());
        if (playing == null || player.getMainHandItem().getItem() != playing.item()) return null;
        float elapsed = clientTicks - playing.start() + partialTick;
        if (elapsed >= playing.ticks()) {
            // our swing is done but vanilla's (6 ticks, longer with Mining Fatigue) may still be running: hold the rest pose
            return playing.fromSwing() && player.swinging ? SwingAnimation.Pose.REST : null;
        }
        return playing.animation().sample(elapsed / playing.ticks(), playing.from());
    }

    /** Plays a swing on the local player now, 'slow' times slower (for /swinganim). False if there is no such id. */
    public static boolean preview(String id, int slow) {
        return play(Minecraft.getInstance().player, id, slow);
    }

    /**
     * Plays a swing on any player this client sees, now, 'slow' times slower (1 = normal), whatever the rules say.
     * For code that knows better than the rules, e.g. a packet from a MeleeCombos step (see docs/ANIMATIONS.md).
     * False (and nothing changes) if the player is null or there is no swing with that id.
     */
    public static boolean play(Player player, String id, int slow) {
        SwingAnimation animation = SwingAnimations.get(id);
        if (player == null || animation == null) return false;
        play(player, animation, Math.max(1, slow), false);
        return true;
    }

    private static void play(Player player, SwingAnimation animation, int slow, boolean fromSwing) {
        // a swing still on screen: the new one winds up from where that one is now instead of snapping to rest
        SwingAnimation.Pose from = poseFor(player, 0f);
        float speed = SwingAnimations.SPEED > 0f ? SwingAnimations.SPEED : 1f;
        PLAYING.put(player.getId(), new Playing(animation, animation.ticks() * slow / speed, clientTicks,
                player.getMainHandItem().getItem(), fromSwing, from));
    }

    /** A new main-hand swing: the matching rule's next swing, or vanilla. */
    private static void onSwing(Player player) {
        int id = player.getId();
        ItemStack held = player.getMainHandItem();
        SwingRule rule = SwingRules.find(player, held);
        if (rule == null) {
            PLAYING.remove(id);
            CYCLES.remove(id);
            return;
        }
        Cycle cycle = CYCLES.computeIfAbsent(id, k -> new Cycle());
        if (cycle.rule != rule || clientTicks - cycle.lastSwing > rule.resetTicks()) cycle.next = 0;
        cycle.rule = rule;
        cycle.lastSwing = clientTicks;
        List<String> sequence = rule.sequence();
        String next = sequence.get(Math.floorMod(cycle.next, sequence.size()));
        cycle.next = (cycle.next + 1) % sequence.size();

        SwingAnimation animation = SwingAnimations.get(next);
        if (animation == null) PLAYING.remove(id); // missing definition: vanilla swing
        else play(player, animation, 1, true);
    }

    // The local player's attack key (fires before vanilla swings for it)
    @SubscribeEvent
    public static void onSwingPlayerAttackKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (event.isAttack() && event.getHand() == InteractionHand.MAIN_HAND) attackClicked = true;
    }

    @SubscribeEvent
    public static void onSwingPlayerClientTick(TickEvent.ClientTickEvent.Post event) {
        boolean clicked = attackClicked;
        attackClicked = false;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {                      // left the world
            PLAYING.clear();
            CYCLES.clear();
            LAST_SWING_TIME.clear();
            PlayerPathwayCache.clear();
            return;
        }
        if (minecraft.isPaused()) return;
        clientTicks++;
        try {
            Set<Integer> seen = new HashSet<>();
            for (Player player : minecraft.level.players()) {
                int id = player.getId();
                seen.add(id);
                int now = player.swinging ? player.swingTime : NOT_SWINGING;
                Integer before = LAST_SWING_TIME.put(id, now);
                boolean mainHand = player.swingingArm != InteractionHand.OFF_HAND;
                // a swing that just started, or restarted (swingTime went back down)
                boolean newSwing = player.swinging && mainHand && (before == null || before == NOT_SWINGING || now < before);
                if (newSwing && (player != minecraft.player || clicked)) onSwing(player);
            }
            LAST_SWING_TIME.keySet().retainAll(seen);
            CYCLES.keySet().retainAll(seen);
            for (Iterator<Map.Entry<Integer, Playing>> it = PLAYING.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<Integer, Playing> entry = it.next();
                Playing playing = entry.getValue();
                boolean over = clientTicks - playing.start() >= playing.ticks();
                if (!seen.contains(entry.getKey())) it.remove();
                else if (over && !(playing.fromSwing() && isSwinging(minecraft, entry.getKey()))) it.remove();
            }
        } catch (RuntimeException e) {
            // never let an animation problem take the game down: forget everything, vanilla swings from here
            PLAYING.clear();
            CYCLES.clear();
            if (!loggedError) {
                loggedError = true;
                ForgeRealm.LOGGER.error("Swing animations: tick failed, falling back to vanilla swings", e);
            }
        }
    }

    private static boolean isSwinging(Minecraft minecraft, int entityId) {
        return minecraft.level.getEntity(entityId) instanceof Player player && player.swinging;
    }
}
