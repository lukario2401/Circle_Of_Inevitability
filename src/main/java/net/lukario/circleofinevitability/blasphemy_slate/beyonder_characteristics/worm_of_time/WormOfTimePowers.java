package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.worm_of_time;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder.*;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.StolenAbilities;
import net.lukario.circleofinevitability.combat.*;
import net.lukario.circleofinevitability.menu.AbilityWheel;
import net.lukario.circleofinevitability.root.*;
import net.lukario.circleofinevitability.status.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.listener.Priority;
import net.minecraft.tags.DamageTypeTags;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

/** Marauder through Trojan Horse of Destiny. See docs/WORM_OF_TIME.md for bindings and adaptations. */
@Mod.EventBusSubscriber(modid = "circle_of_inevitability")
public final class WormOfTimePowers {
    public static final String PATHWAY = "Worm Of Time";
    public static final String DECRYPTED = "worm_of_time_decrypted";
    private static final String AVATARS = "worm_of_time_avatars";
    private static final Map<UUID, Long> SUCCESSION = new HashMap<>();
    private static final Identifier AGILITY = Physique.id("worm_of_time_agility");
    private static final Identifier HANDS = Physique.id("worm_of_time_hands");
    private static final Identifier BODY = Physique.id("worm_of_time_body");
    private WormOfTimePowers() {}

    public static boolean ready(Player player, boolean bypass, int sequence) {
        return player.level() instanceof ServerLevel && (bypass || PATHWAY.equals(SoulCore.getPathway(player)))
                && Beyonder.hasSequence(player, sequence);
    }

    public static int sequence(Player player) {
        return Math.max(0, Math.min(9, SoulCore.getSequence(player)));
    }

    public static float might(Player player) {
        return switch (sequence(player)) { case 0 -> 5f; case 1 -> 4f; case 2 -> 3f; case 3 -> 2.5f; case 4 -> 2f; default -> 1f; };
    }

    public static AbilityWheel.Result refuse(Player player, String text) {
        player.sendSystemMessage(Component.literal(text));
        return AbilityWheel.Result.REFUSED;
    }

    public static LivingEntity target(Player player, ServerLevel sl, double range) {
        LivingEntity target = Spells.aimEnemy(player, sl, range);
        if (target == null) refuse(player, "There is no target in sight.");
        return target;
    }

    /** Key 1: mundane theft until Parasite, then life theft; sneak observes until distance theft unlocks. */
    public static void marauder(Player player, ServerLevel sl) {
        if (player.isShiftKeyDown() && !Beyonder.hasSequence(player, 4)) {
            observation(player, sl, false);
            return;
        }
        if (player.isShiftKeyDown()) {
            if (Root.has(player, RootRestriction.TELEPORT)) { refuse(player, "Your position is bound."); return; }
            Vec3 to = Spells.blinkSpot(player, sl, Beyonder.hasSequence(player, 1) ? 48 : 24);
            if (to == null) { refuse(player, "There is no safe space there."); return; }
            if (!Spells.paySpirituality(player, 1500)) return;
            Vec3 from = player.position();
            player.teleportTo(to.x, to.y, to.z);
            FallGuard.protect(player, 100);
            WormOfTimeFx.burst(sl, from.add(0, 1, 0));
            WormOfTimeFx.burst(sl, to.add(0, 1, 0));
        } else {
            LivingEntity target = target(player, sl, Beyonder.hasSequence(player, 4) ? 24 : 6);
            if (target == null) return;
            if (Beyonder.hasSequence(player, 4)) {
                if (!Spells.paySpirituality(player, 1800)) return;
                float before = target.getHealth();
                Spells.strikeTrue(player, target, 12f * might(player));
                player.heal(Math.max(0, before - target.getHealth()) * 0.6f);
            } else {
                ItemStack held = target.getMainHandItem();
                if (held.isEmpty()) { refuse(player, "Its hands hold nothing to steal."); return; }
                if (target instanceof Player other && (other.isCreative() || other.isSpectator())) {
                    refuse(player, "That inventory cannot be stolen from."); return;
                }
                if (!Spells.paySpirituality(player, 250)) return;
                ItemStack stolen = held.split(1);
                if (!player.getInventory().add(stolen)) player.drop(stolen, false);
            }
            WormOfTimeFx.steal(sl, player, target);
        }
    }

    /** Key 4: observation; sneak decrypts evidence from the entity actually in sight. */
    public static void insight(Player player, Level level, ServerLevel sl, boolean bypass) {
        if (!ready(player, bypass, 9)) return;
        observation(player, sl, player.isShiftKeyDown());
    }

    private static void observation(Player player, ServerLevel sl, boolean decrypt) {
        if (decrypt && !Beyonder.hasSequence(player, 7)) { refuse(player, "Decryption requires Sequence 7."); return; }
        double range = Beyonder.hasSequence(player, 6) ? 32 : 10;
        if (decrypt) {
            LivingEntity target = target(player, sl, range);
            if (target == null || !Spells.paySpirituality(player, 700)) return;
            Marks.add(target, player, DECRYPTED, 1, 600);
            Exposed.apply(target, 100);
            Casts.Cast last = Casts.last(target);
            player.sendSystemMessage(Component.literal(target.getName().getString() + ": "
                    + Math.round(target.getHealth()) + "/" + Math.round(target.getMaxHealth()) + " health; "
                    + (Beyonders.isBeyonder(target) ? Beyonders.pathwayOf(target) + ", Sequence " + Beyonders.sequenceOf(target) : "no known Pathway")
                    + (last == null ? ". No recent cast witnessed." : ". Recent power: " + last.name() + ".")));
            WormOfTimeFx.steal(sl, player, target);
        } else {
            if (!Spells.paySpirituality(player, 150)) return;
            List<ItemEntity> items = sl.getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(range), e -> !e.getItem().isEmpty());
            ItemEntity nearest = items.stream().min(Comparator.comparingDouble(player::distanceToSqr)).orElse(null);
            if (nearest == null) player.sendSystemMessage(Component.literal("No loose valuables within " + (int) range + " blocks."));
            else {
                player.sendSystemMessage(Component.literal("You sense " + nearest.getItem().getHoverName().getString()
                        + " about " + (int) player.distanceTo(nearest) + " blocks away."));
                if (Beyonder.hasSequence(player, 6) && player instanceof ServerPlayer viewer) {
                    net.lukario.circleofinevitability.particles.fx.ParticleShapes.spawnFor(viewer,
                            net.lukario.circleofinevitability.particles.fx.ParticleFx.of("fx/spark").color(WormOfTimeFx.CYAN).glow(),
                            nearest.position(), Vec3.ZERO);
                }
            }
        }
    }

    private static final AbilityWheel DECEIT = AbilityWheel.create("worm_of_time_deceit", "Thought and Deceit")
            .quickCastCooldown(false).quickCastCost(3f)
            .entry("swindle", icon("swindle"), "Swindler's Charm", 8, 500, 160, WormOfTimePowers.guarded(WormOfTimePowers::swindle))
            .entry("intent", icon("intent"), "Steal Intent", 5, 2000, 160, WormOfTimePowers.guarded(WormOfTimePowers::intent))
            .entry("dream", icon("dream"), "Steal Dream", 5, 2400, 240, WormOfTimePowers.guarded(WormOfTimePowers::dream))
            .entry("deceive", icon("deceive"), "Mentor of Deceit", 3, 6000, 300, WormOfTimePowers.guarded(WormOfTimePowers::deceive))
            .entry("attack", icon("attack"), "Steal Attack", 5, 2400, 240, WormOfTimePowers.guarded(WormOfTimePowers::stealAttack));

    private static final AbilityWheel DESTINY = AbilityWheel.create("worm_of_time_destiny", "Parasite and Destiny")
            .quickCastCooldown(false).quickCastCost(3f)
            .entry("swarm", icon("swarm"), "Worms of Time", 4, 6000, 400, WormOfTimePowers.guarded(WormOfTimePowers::swarm)).notCopyable()
            .entry("avatar", icon("avatar"), "Create Avatar", 3, 9000, 200, WormOfTimePowers.guarded(WormOfTimePowers::avatar)).notCopyable()
            .entry("recall", icon("recall"), "Recall Avatars", 3, 0, 0, WormOfTimePowers.guarded(WormOfTimePowers::recall)).notCopyable()
            .entry("fate", icon("fate"), "Steal Fate", 2, 15000, 500, WormOfTimePowers.guarded(WormOfTimePowers::fate))
            .entry("exchange", icon("exchange"), "Exchange Positions", 2, 12000, 240, WormOfTimePowers.guarded(WormOfTimePowers::exchange))
            .entry("succession", icon("succession"), "Avatar Succession", 1, 24000, 1200, WormOfTimePowers.guarded(WormOfTimePowers::succession)).notCopyable();

    /** Recheck ownership when a delayed menu choice arrives, including after a respec or gamemode change. */
    static AbilityWheel.Cast guarded(AbilityWheel.Cast action) {
        return (p, sl) -> {
            if (p.isSpectator() || !p.isAlive() || (!PATHWAY.equals(SoulCore.getPathway(p))
                    && !Beyonder.isCastingUnbound(p) && !Casts.isCopying(p)))
                return refuse(p, "This power is no longer available to you.");
            return action.cast(p, sl);
        };
    }

    public static String icon(String id) { return "ability_menus/worm_of_time/" + id; }

    public static void deceit(Player p, Level level, ServerLevel sl, boolean bypass) {
        if (ready(p, bypass, 8)) { if (p.isShiftKeyDown()) DECEIT.quickCast(p); else DECEIT.open(p); }
    }

    public static void destiny(Player p, Level level, ServerLevel sl, boolean bypass) {
        if (ready(p, bypass, 4)) { if (p.isShiftKeyDown()) DESTINY.quickCast(p); else DESTINY.open(p); }
    }

    private static AbilityWheel.Result swindle(ServerPlayer p, ServerLevel sl) {
        Unnoticed.hide(p, 120);
        WormOfTimeFx.burst(sl, p.position().add(0, 1, 0));
        return AbilityWheel.Result.DONE;
    }

    private static AbilityWheel.Result intent(ServerPlayer p, ServerLevel sl) {
        LivingEntity t = target(p, sl, 32);
        if (t == null) return AbilityWheel.Result.REFUSED;
        Root.apply(t, Spells.holdLength(t, 50), RootRestriction.DISARM);
        if (t instanceof Mob mob) mob.setTarget(null);
        WormOfTimeFx.steal(sl, p, t);
        return AbilityWheel.Result.DONE;
    }

    private static AbilityWheel.Result dream(ServerPlayer p, ServerLevel sl) {
        LivingEntity t = target(p, sl, 32);
        if (t == null) return AbilityWheel.Result.REFUSED;
        if (!Slumber.put(t, Spells.holdLength(t, 100), Slumber.Kind.SLEEP)) return refuse(p, "Its mind resists the stolen dream.");
        float take = Math.min(1200, Spells.spiritualityOf(t) * 0.1f);
        if (take > 0 && Spells.paySpirituality(t, take)) Spells.giveSpirituality(p, take);
        WormOfTimeFx.steal(sl, p, t);
        return AbilityWheel.Result.DONE;
    }

    private static AbilityWheel.Result deceive(ServerPlayer p, ServerLevel sl) {
        List<LivingEntity> enemies = Spells.enemiesAround(p, sl, p.position(), 12);
        if (enemies.isEmpty()) return refuse(p, "There is no one nearby to deceive.");
        for (LivingEntity t : enemies.stream().limit(24).toList()) {
            Falter.apply(t, "worm_of_time_deceit", 0.35f, 160);
            Root.apply(t, Spells.holdLength(t, 25), RootRestriction.ABILITIES);
            if (t instanceof Mob mob) mob.setTarget(null);
        }
        WormOfTimeFx.clock(sl, p.position().add(0, 2, 0), 2, 30, false);
        return AbilityWheel.Result.DONE;
    }

    private static AbilityWheel.Result stealAttack(ServerPlayer p, ServerLevel sl) {
        if (Decree.violate(p, Decree.Rule.STAND_INS)) return refuse(p, "A stand-in is forbidden here.");
        Substitute.arm(p, 80, 1, (saved, source, amount, world) -> {
            if (source.getEntity() instanceof LivingEntity attacker && Spells.isEnemy(saved, attacker)) {
                Spells.strike(saved, attacker, Math.min(amount, 30f * might(p)));
                WormOfTimeFx.steal(world, saved, attacker);
            } else WormOfTimeFx.burst(world, saved.position());
        });
        if (!Substitute.isArmed(p)) return refuse(p, "A stand-in is forbidden here.");
        HudTimer.show(p, "worm_of_time_attack", "Steal Attack", 80, 0x72E8DC);
        return AbilityWheel.Result.DONE;
    }

    private static AbilityWheel.Result swarm(ServerPlayer p, ServerLevel sl) {
        Swarm.start(p, 160, 6 + (4 - Math.min(4, sequence(p))) * 2, 6f, new Swarm.Hooks() {
            public void bodyLost(LivingEntity who, ServerLevel world, Vec3 at, int left) { WormOfTimeFx.burst(world, at); }
            public void over(LivingEntity who, ServerLevel world, Swarm.End why) { WormOfTimeFx.burst(world, who.position()); }
        });
        WormOfTimeFx.clock(sl, p.position().add(0, 1, 0), 1.5f, 30, false);
        HudTimer.show(p, "worm_of_time_swarm", "Worms of Time", 160, 0x72E8DC);
        return AbilityWheel.Result.DONE;
    }

    private static AbilityWheel.Result avatar(ServerPlayer p, ServerLevel sl) {
        Vec3 at = Spells.freeSpotNear(p, sl, p.position(), Spells.flatLook(p), 2);
        if (at == null) return refuse(p, "There is no room for an avatar.");
        int limit = sequence(p) <= 1 ? 4 : sequence(p) <= 2 ? 3 : 2;
        Mob made = Construct.of(EntityTypes.HUSK).health(40 * might(p)).damage(6 * might(p))
                .speed(1.2f).lasts(1200).leash(48).limit(AVATARS, limit)
                .onEnd((body, why) -> {
                    if (body.level() instanceof ServerLevel world) WormOfTimeFx.burst(world, body.position());
                }).build(p, sl, at, p.getYRot());
        if (made == null) return refuse(p, "The avatar could not take shape.");
        made.setCustomName(Component.literal(p.getName().getString() + "'s Avatar"));
        made.setCustomNameVisible(true);
        WormOfTimeFx.avatar(sl, made, 1200);
        return AbilityWheel.Result.DONE;
    }

    private static AbilityWheel.Result recall(ServerPlayer p, ServerLevel sl) {
        List<Mob> bodies = Construct.ofOwner(p, AVATARS);
        if (bodies.isEmpty()) return refuse(p, "You have no avatars to recall.");
        float recovered = 0;
        for (Mob body : List.copyOf(bodies)) { recovered += Math.min(8f, body.getHealth() * 0.1f); Construct.end(body); }
        p.heal(recovered);
        return AbilityWheel.Result.FREE;
    }

    private static AbilityWheel.Result fate(ServerPlayer p, ServerLevel sl) {
        LivingEntity t = target(p, sl, 32);
        if (t == null) return AbilityWheel.Result.REFUSED;
        List<MobEffectInstance> gifts = new ArrayList<>(t.getActiveEffects());
        int stolen = 0;
        for (MobEffectInstance effect : gifts) {
            if (!effect.getEffect().value().isBeneficial() || stolen >= 3 || effect.isInfiniteDuration()) continue;
            int ticks = Math.min(200, effect.getDuration());
            p.addEffect(new MobEffectInstance(effect.getEffect(), ticks, Math.min(2, effect.getAmplifier())));
            t.removeEffect(effect.getEffect());
            stolen++;
        }
        Falter.apply(t, "worm_of_time_fate", 0.4f, 200);
        p.addEffect(new MobEffectInstance(MobEffects.LUCK, 200, 1));
        WormOfTimeFx.steal(sl, p, t);
        return AbilityWheel.Result.DONE;
    }

    private static AbilityWheel.Result exchange(ServerPlayer p, ServerLevel sl) {
        LivingEntity t = target(p, sl, 32);
        if (t == null) return AbilityWheel.Result.REFUSED;
        if (Root.has(p, RootRestriction.TELEPORT) || Root.has(t, RootRestriction.TELEPORT) || Spells.cannotBeMoved(t))
            return refuse(p, "One of the positions is bound.");
        Vec3 from = p.position(), to = t.position();
        if (!Spells.fits(p, sl, to) || !Spells.fits(t, sl, from)) return refuse(p, "The exchanged bodies would not fit.");
        p.teleportTo(to.x, to.y, to.z);
        t.teleportTo(from.x, from.y, from.z);
        FallGuard.protect(p, 100); FallGuard.protect(t, 100);
        WormOfTimeFx.burst(sl, from.add(0, 1, 0)); WormOfTimeFx.burst(sl, to.add(0, 1, 0));
        return AbilityWheel.Result.DONE;
    }

    private static AbilityWheel.Result succession(ServerPlayer p, ServerLevel sl) {
        if (Construct.ofOwner(p, AVATARS).isEmpty()) return refuse(p, "Create an avatar before preparing succession.");
        if (Decree.violate(p, Decree.Rule.RESURRECTION)) return refuse(p, "Resurrection is prohibited here.");
        SUCCESSION.put(p.getUUID(), sl.getGameTime() + 600);
        HudTimer.show(p, "worm_of_time_succession", "Avatar Succession", 600, 0x72E8DC);
        return AbilityWheel.Result.DONE;
    }

    // Validate the living avatar at the fatal blow, so a destroyed avatar cannot resurrect its owner.
    @SubscribeEvent(priority = Priority.HIGHEST)
    public static boolean onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return false;
        Long until = SUCCESSION.remove(p.getUUID());
        if (until == null || until < p.level().getGameTime() || !PATHWAY.equals(SoulCore.getPathway(p))
                || !Beyonder.hasSequence(p, 1) || event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                || Spells.isPiercing(p) || Root.has(p, RootRestriction.TELEPORT)
                || Decree.violate(p, Decree.Rule.RESURRECTION)) return false;
        Mob body = Construct.ofOwner(p, AVATARS).stream()
                .filter(m -> m.isAlive() && m.level() == p.level() && Spells.fits(p, p.level(), m.position()))
                .findFirst().orElse(null);
        if (body == null) return false;
        Vec3 at = body.position();
        Construct.end(body);
        p.setHealth(Math.max(1, p.getMaxHealth() * 0.4f));
        p.clearFire();
        p.teleportTo(at.x, at.y, at.z);
        Invulnerable.grant(p, 40);
        FallGuard.protect(p, 100);
        HudTimer.hide(p, "worm_of_time_succession");
        WormOfTimeFx.clock(p.level(), at.add(0, 1.5, 0), 2, 40, true);
        return true;
    }

    /** A bounded autonomous avatar can steal a little life from its own target, once every three seconds. */
    @SubscribeEvent
    public static void onTick(TickEvent.PlayerTickEvent.Post event) {
        Player p = event.player();
        if (!(p.level() instanceof ServerLevel sl)) return;
        boolean ours = PATHWAY.equals(SoulCore.getPathway(p)) && p.isAlive();
        if (p.tickCount % 20 != 0) return;
        Physique.keep(p, Attributes.MOVEMENT_SPEED, AGILITY, ours ? (sequence(p) <= 6 ? 0.20 : 0.10) : 0,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE, ours);
        Physique.keep(p, Attributes.ATTACK_SPEED, HANDS, 0.15, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, ours);
        Physique.keep(p, Attributes.MAX_HEALTH, BODY, 8, AttributeModifier.Operation.ADD_VALUE, ours && sequence(p) <= 4);
        if (!ours) {
            Construct.endAll(p, AVATARS); SUCCESSION.remove(p.getUUID());
            if (StolenAbilities.count(p) > 0) StolenAbilities.dropAll(p);
            return;
        }
        if (SUCCESSION.getOrDefault(p.getUUID(), 0L) < sl.getGameTime() || sequence(p) > 1)
            SUCCESSION.remove(p.getUUID());
        if (sequence(p) <= 6) p.removeEffect(MobEffects.NAUSEA);
        if (sequence(p) > 3) Construct.endAll(p, AVATARS);
        if (p.tickCount % 60 == 0) for (Mob body : List.copyOf(Construct.ofOwner(p, AVATARS))) {
            LivingEntity enemy = body.getTarget();
            if (enemy == null || !enemy.isAlive() || body.distanceTo(enemy) > 12 || !Spells.isEnemy(p, enemy)) continue;
            Spells.strike(p, enemy, 3f * might(p));
            body.heal(2f);
            WormOfTimeFx.steal(sl, body, enemy);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        Construct.endAll(event.getEntity(), AVATARS);
        SUCCESSION.remove(event.getEntity().getUUID());
    }
}
