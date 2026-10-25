package net.lukario.circleofinevitability.status;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.combat.Later;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.combat.Zone;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingUseTotemEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.listener.Priority;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.Predicate;

/**
 * A Decree: a round piece of ground where Rules are in force. Whoever the Rules bind (the owner's enemies, never the
 * owner or their allies) cannot do what is forbidden there, and every try is a Violation the owner hears of, counted
 * one by one, so each can be punished harder than the last. The Hand Of Order's Prohibitions, Laws and Court:
 *
 *   Decree.at(ground, 14)                               // its middle on the ground, and how far it reaches (blocks)
 *           .forbid(Decree.Rule.PASSAGE, Decree.Rule.CONCEALMENT)
 *           .lasts(240)                                 // ticks (default 300)
 *           .height(8)                                  // how far up it still holds (default 6)
 *           .binds(enemy -> !isStronger(enemy))         // of the owner's enemies, only these (default: all of them)
 *           .onViolation((who, rule, count, sl) -> ...) // a Rule broken: count = its 1st, 2nd, 3rd... Violation here
 *           .onEnter(enemy -> ...)                      // one it binds stepped in (also those in it when it is written)
 *           .onPulse(40, ticksLeft -> ...)              // every 40 ticks, once: its look
 *           .onEnd(() -> ...)
 *           .write(player, sl, "my_pathway_decree");
 *
 * Every part after at(...) is optional. Build a new one for every cast. Writing one with a name the owner already
 * has closes the old one first. It is a combat/Zone underneath, so it ends with its owner too (death, logout, another
 * dimension), and anything that closes zones near a spot (Zone.closeNear, a Quell) closes it.
 *
 * WHAT EACH RULE STOPS (for those it binds, while they stand in it):
 *   PASSAGE       teleports (ender pearls, chorus fruit, every ability that asks Root.has(.., TELEPORT): Flaming
 *                 Jump, the Sword of Judgment...), flying, Flight, Glide, elytra and levitation
 *   CONCEALMENT   hiding: Concealment.hide is refused, and anyone hidden inside is pulled out (status/Exposed)
 *   STAND_INS     a Substitute is refused, and one armed before does not take the hit inside
 *   RESURRECTION  a DeathWard is refused, an armed one does not save them, a totem of undying does not work
 *   MARIONETTES   marionettes of those it binds stand frozen, and Marionette.control is refused
 *   HEALING       no healing at all
 *   PROJECTILES   their projectiles end at the edge or as they are shot
 *   ABILITIES     a player's ability keys refuse (Decree.blocksKey), a mob's special attacks are locked (AbilityLock)
 *   REALITY       not a ban, a weight, and on EVERYONE inside (allies too): ability damage is halved, plain weapon,
 *                 arrow and fall damage is REALITY_PLAIN, and abilities cost REALITY_COST. The owner's own are spared.
 * Every refused try counts as a Violation (the same Rule again within VIOLATION_GAP ticks counts once).
 *
 * WHILE IT IS WRITTEN:
 *   Decree.isWritten(player, name)   Decree.ticksLeft(player, name)   Decree.close(player, name)
 *   Decree.extend(player, name, 100)        // 100 ticks more (a second layer of a Prohibition)
 *   Decree.forbid(player, name, Rule.HEALING)   // one more Rule, now (a Court writing its clauses one by one)
 *   Decree.rules(player, name)       Decree.centerOf(player, name)   Decree.radiusOf(player, name)
 *
 * FROM ANYWHERE:
 *   Decree.forbids(entity, Rule.PASSAGE)    // is it bound by that Rule where it stands
 *   if (Decree.violate(entity, Rule.STAND_INS)) return;   // it tries: refused, and the Violation is counted
 *   Decree.violations(entity)               // its Violations in every Decree it is in now (combat/Disorder counts them)
 *   Decree.inReality(entity)                // inside an Imperative: Reality
 *   Decree.addImmunity((owner, entity, rule) -> ...)      // some cannot be bound (a higher Sequence, an Order Proxy)
 *
 * Kit statuses ask it themselves (Concealment.hide, Substitute, DeathWard, Marionette.control), the ability keys ask
 * Decree.blocksKey (network/SKeyPressAbility...Used). Not saved: a server restart lifts every Decree.
 *
 * The guide with examples for all of these tools: docs/SPELL_KIT.md
 */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Decree {

    /** What a Decree can forbid (REALITY is a weight on everyone rather than a ban: see the class comment). */
    public enum Rule {
        PASSAGE, CONCEALMENT, STAND_INS, RESURRECTION, MARIONETTES, HEALING, PROJECTILES, ABILITIES, REALITY
    }

    /** A Rule broken: 'count' is how many Violations 'who' has made in this Decree so far (1 = the first). */
    @FunctionalInterface
    public interface Violation {
        void run(LivingEntity who, Rule rule, int count, ServerLevel level);
    }

    /** True when 'entity' cannot be bound by 'rule' of a Decree that 'owner' wrote. */
    @FunctionalInterface
    public interface Immunity {
        boolean immune(Player owner, LivingEntity entity, Rule rule);
    }

    /** The same Rule broken again by the same one within this many ticks is one Violation, not many. */
    public static int VIOLATION_GAP = 20;
    /** Imperative: Reality: what ability damage inside comes to... */
    public static float REALITY_ABILITY = 0.5f;
    /** ...what plain weapon, arrow and fall damage comes to... */
    public static float REALITY_PLAIN = 1.25f;
    /** ...and what abilities cost there. */
    public static float REALITY_COST = 1.5f;

    private static final int BEAT = 5;                        // ticks between two looks at who is inside
    private static final String ZONE_PREFIX = "decree:";

    private static final List<Decree> WRITTEN = new ArrayList<>();
    private static final List<Immunity> IMMUNITIES = new ArrayList<>();

    static {
        // abilities cost more inside a Reality (its owner is spared)
        Spells.addCostScale(player -> realityOver(player, null) != null ? REALITY_COST : 1f);
        // PASSAGE holds like a teleport root, without a root's ring: Flaming Jump, the Sword of Judgment and every
        // teleport the game makes (Root's own EntityTeleportEvent) refuse
        Root.addHold((entity, restriction) -> restriction == RootRestriction.TELEPORT
                && entity instanceof LivingEntity living && forbids(living, Rule.PASSAGE));
    }

    // ---------- what it is ----------

    private final Vec3 center;
    private final double radius;
    private double height = 6.0;
    private int lasts = 300;
    private final EnumSet<Rule> rules = EnumSet.noneOf(Rule.class);
    private Predicate<LivingEntity> binds;
    private Violation onViolation;
    private Consumer<LivingEntity> onEnter;
    private int pulseEvery = 20;
    private IntConsumer onPulse;
    private Runnable onEnd;

    // ---------- while it is written ----------

    private LivingEntity owner;
    private ServerLevel level;
    private String name;
    private boolean closed = true;
    private int beats;
    private final Map<UUID, Integer> counts = new HashMap<>();
    private final Map<String, Long> lastBroken = new HashMap<>();      // uuid + rule -> game time

    private Decree(Vec3 center, double radius) {
        this.center = center;
        this.radius = Math.max(0.5, radius);
    }

    /** A Decree on the ground at 'ground', 'radius' blocks round. Nothing is in force until .write(...). */
    public static Decree at(Vec3 ground, double radius) {
        return new Decree(ground, radius);
    }

    // ---------- setting it up ----------

    public Decree forbid(Rule... forbidden) {
        rules.addAll(List.of(forbidden));
        return this;
    }

    public Decree lasts(int ticks) {
        this.lasts = Math.max(1, ticks);
        return this;
    }

    public Decree height(double blocks) {
        this.height = Math.max(1.0, blocks);
        return this;
    }

    /** Of the owner's enemies, only those this says true for are bound (the others shrug it off). */
    public Decree binds(Predicate<LivingEntity> who) {
        this.binds = who;
        return this;
    }

    public Decree onViolation(Violation action) {
        this.onViolation = action;
        return this;
    }

    public Decree onEnter(Consumer<LivingEntity> action) {
        this.onEnter = action;
        return this;
    }

    /** Every 'ticks' ticks (rounded to its beat of 5), once, with the ticks it has left. The first time at once. */
    public Decree onPulse(int ticks, IntConsumer action) {
        this.pulseEvery = Math.max(BEAT, ticks);
        this.onPulse = action;
        return this;
    }

    public Decree onEnd(Runnable action) {
        this.onEnd = action;
        return this;
    }

    /** Puts it in force. If this player already has a Decree with this name, that one ends first. */
    public void write(LivingEntity owner, ServerLevel level, String name) {
        close(owner, name);
        this.owner = owner;
        this.level = level;
        this.name = name;
        this.closed = false;
        this.beats = 0;
        WRITTEN.add(this);
        Zone.at(center, radius).height(height).lasts(lasts).every(BEAT)
                .affects(entity -> true)                     // everyone: Reality weighs on all, the Rules sort out the rest
                .onEnter(entity -> {
                    if (onEnter != null && bindsAny(entity)) onEnter.accept(entity);
                })
                .onInside((entity, ticksInside) -> enforce(entity))
                .onPulse(ticksLeft -> {
                    if (onPulse != null && (beats * BEAT) % pulseEvery == 0) onPulse.accept(ticksLeft);
                    beats++;
                })
                .onEnd(this::ended)
                .open(owner, level, ZONE_PREFIX + name);
    }

    // ---------- asking / changing ----------

    public static boolean isWritten(LivingEntity owner, String name) {
        return find(owner, name) != null;
    }

    public static int ticksLeft(LivingEntity owner, String name) {
        return find(owner, name) == null ? 0 : Zone.ticksLeft(owner, ZONE_PREFIX + name);
    }

    public static Vec3 centerOf(LivingEntity owner, String name) {
        Decree decree = find(owner, name);
        return decree == null ? null : decree.center;
    }

    public static double radiusOf(LivingEntity owner, String name) {
        Decree decree = find(owner, name);
        return decree == null ? 0.0 : decree.radius;
    }

    /** The Rules in force in it now (empty if there is no such Decree). */
    public static Set<Rule> rules(LivingEntity owner, String name) {
        Decree decree = find(owner, name);
        return decree == null ? EnumSet.noneOf(Rule.class) : EnumSet.copyOf(decree.rulesOrNone());
    }

    /** One more Rule, in force from now. False if there is no such Decree. */
    public static boolean forbid(LivingEntity owner, String name, Rule rule) {
        Decree decree = find(owner, name);
        if (decree == null) return false;
        decree.rules.add(rule);
        return true;
    }

    /** It lasts 'ticks' more. False if there is no such Decree. */
    public static boolean extend(LivingEntity owner, String name, int ticks) {
        return find(owner, name) != null && Zone.extend(owner, ZONE_PREFIX + name, ticks);
    }

    /** Ends it now (its onEnd runs). */
    public static void close(LivingEntity owner, String name) {
        if (find(owner, name) != null) Zone.close(owner, ZONE_PREFIX + name);
    }

    /** How many Violations 'entity' has made in this player's Decree (0 if none, or no such Decree). */
    public static int violationsIn(LivingEntity owner, String name, Entity entity) {
        Decree decree = find(owner, name);
        return decree == null || entity == null ? 0 : decree.counts.getOrDefault(entity.getUUID(), 0);
    }

    // ---------- from anywhere ----------

    /** Some can never be bound by some Rules (once, in a static block). */
    public static void addImmunity(Immunity immunity) {
        IMMUNITIES.add(immunity);
    }

    /** True when a Decree forbids 'rule' to this entity where it stands now. */
    public static boolean forbids(LivingEntity entity, Rule rule) {
        if (entity == null || WRITTEN.isEmpty()) return false;
        for (Decree decree : WRITTEN) {
            if (decree.bound(entity, rule)) return true;
        }
        return false;
    }

    /**
     * It tries something 'rule' forbids: true if a Decree forbids it here (refuse it then), and each such Decree counts
     * the Violation and runs its onViolation (once per VIOLATION_GAP for the same Rule). False: it is allowed.
     */
    public static boolean violate(LivingEntity entity, Rule rule) {
        if (entity == null || WRITTEN.isEmpty() || !(entity.level() instanceof ServerLevel)) return false;
        boolean forbidden = false;
        for (Decree decree : new ArrayList<>(WRITTEN)) {
            if (!decree.bound(entity, rule)) continue;
            forbidden = true;
            decree.broken(entity, rule);
        }
        return forbidden;
    }

    /** Its Violations in every Decree it stands in now. */
    public static int violations(LivingEntity entity) {
        if (entity == null || WRITTEN.isEmpty()) return 0;
        int total = 0;
        for (Decree decree : WRITTEN) {
            if (decree.covers(entity)) total += decree.counts.getOrDefault(entity.getUUID(), 0);
        }
        return total;
    }

    /** True inside an Imperative: Reality (anyone's, allies' too). */
    public static boolean inReality(LivingEntity entity) {
        return realityOver(entity, null) != null;
    }

    /**
     * Asked by the ability keys before anything else: true (and the player is told, and it is a Violation) when a
     * Decree forbids them ABILITIES here.
     */
    public static boolean blocksKey(Player player, int key) {
        if (!violate(player, Rule.ABILITIES)) return false;
        player.sendOverlayMessage(Component.literal("Abilities are prohibited here!"));
        return true;
    }

    // ---------- internals ----------

    private static Decree find(LivingEntity owner, String name) {
        for (Decree decree : WRITTEN) {
            if (!decree.closed && decree.name.equals(name) && decree.owner.getUUID().equals(owner.getUUID())) return decree;
        }
        return null;
    }

    private Set<Rule> rulesOrNone() {
        return rules.isEmpty() ? EnumSet.noneOf(Rule.class) : rules;
    }

    private boolean covers(LivingEntity entity) {
        return !closed && entity.level() == level && Zone.covers(owner, ZONE_PREFIX + name, entity.position());
    }

    private boolean covers(Vec3 at) {
        return !closed && Zone.covers(owner, ZONE_PREFIX + name, at);
    }

    /** Is this one held to 'rule' by this Decree where it stands. */
    private boolean bound(LivingEntity entity, Rule rule) {
        if (closed || rule == Rule.REALITY || !rules.contains(rule)) return false;
        if (!binds(entity, rule)) return false;
        return covers(entity);
    }

    /** Would this one be held to 'rule' by this Decree if it stood in it (who it is, not where). */
    private boolean binds(LivingEntity entity, Rule rule) {
        if (!entity.isAlive() || entity.isSpectator()) return false;
        if (entity.getUUID().equals(owner.getUUID()) || Spells.isFriend(owner, entity)) return false;   // Immunity: allies
        if (binds != null && !binds.test(entity)) return false;
        for (Immunity immunity : IMMUNITIES) {
            if (owner instanceof Player writer && immunity.immune(writer, entity, rule)) return false;
        }
        return true;
    }

    private boolean bindsAny(LivingEntity entity) {
        for (Rule rule : rules) {
            if (rule != Rule.REALITY && binds(entity, rule)) return true;
        }
        return false;
    }

    private void broken(LivingEntity entity, Rule rule) {
        String key = entity.getUUID() + "/" + rule.name();
        long now = level.getGameTime();
        Long last = lastBroken.get(key);
        if (last != null && now - last < VIOLATION_GAP) return;
        lastBroken.put(key, now);
        int count = counts.merge(entity.getUUID(), 1, Integer::sum);
        if (onViolation == null) return;
        try {
            onViolation.run(entity, rule, count, level);
        } catch (Exception e) {
            CircleOfInevitability.LOGGER.error("A Decree's Violation failed", e);
        }
    }

    /** One beat for one inside: what the Rules keep from it. */
    private void enforce(LivingEntity entity) {
        if (rules.contains(Rule.PASSAGE) && binds(entity, Rule.PASSAGE)) {
            boolean flew = false;
            if (entity instanceof Player player && !player.isCreative()) {
                if (player.getAbilities().flying) {
                    player.getAbilities().flying = false;
                    player.onUpdateAbilities();
                    flew = true;
                }
                if (Flight.has(player)) {
                    Flight.end(player);
                    flew = true;
                }
                if (Glide.isGliding(player)) {
                    Glide.end(player);
                    flew = true;
                }
                if (player.isFallFlying()) {
                    player.stopFallFlying();
                    flew = true;
                }
            }
            if (entity.hasEffect(MobEffects.LEVITATION)) {
                entity.removeEffect(MobEffects.LEVITATION);
                flew = true;
            }
            if (flew) broken(entity, Rule.PASSAGE);
        }
        if (rules.contains(Rule.CONCEALMENT) && binds(entity, Rule.CONCEALMENT)) {
            boolean hid = Concealment.isHidden(entity) || Unnoticed.isUnnoticed(entity) || entity.hasEffect(MobEffects.INVISIBILITY);
            Exposed.apply(entity, BEAT * 2 + 2);
            if (hid) broken(entity, Rule.CONCEALMENT);
        }
        if (rules.contains(Rule.MARIONETTES) && entity instanceof Mob puppet && Marionette.isMarionette(puppet)
                && binds(entity, Rule.MARIONETTES)) {
            Root.apply(puppet, BEAT + 2, RootRestriction.EVERYTHING);
        }
        if (rules.contains(Rule.ABILITIES) && !(entity instanceof Player) && binds(entity, Rule.ABILITIES)) {
            AbilityLock.apply(entity, BEAT + 5);
        }
    }

    private void ended() {
        if (closed) return;
        closed = true;
        WRITTEN.remove(this);
        counts.clear();
        lastBroken.clear();
        if (onEnd == null) return;
        try {
            onEnd.run();
        } catch (Exception e) {
            CircleOfInevitability.LOGGER.error("A Decree failed while ending", e);
        }
    }

    /** The Reality over this entity that is not 'spared''s own (null: none). */
    private static Decree realityOver(LivingEntity entity, Entity spared) {
        if (entity == null || WRITTEN.isEmpty()) return null;
        for (Decree decree : WRITTEN) {
            if (!decree.rules.contains(Rule.REALITY) || !decree.covers(entity)) continue;
            if (entity.getUUID().equals(decree.owner.getUUID())) continue;               // its owner's casts are Order
            if (spared != null && spared.getUUID().equals(decree.owner.getUUID())) continue;
            return decree;
        }
        return null;
    }

    /** A projectile that a Decree with PROJECTILES should stop here: its shooter is bound (or nobody shot it). */
    private Decree stopping(Projectile projectile) {
        for (Decree decree : WRITTEN) {
            if (decree.level != projectile.level() || !decree.rules.contains(Rule.PROJECTILES)) continue;
            if (!decree.covers(projectile.position())) continue;
            Entity shooter = projectile.getOwner();
            if (shooter == null) return decree;
            if (shooter instanceof LivingEntity living && decree.binds(living, Rule.PROJECTILES)) return decree;
        }
        return null;
    }

    private static boolean isAbilityHit(LivingEntity victim, DamageSource source) {
        return Spells.isSpellHit(victim) || source.is(DamageTypes.MAGIC) || source.is(DamageTypes.INDIRECT_MAGIC);
    }

    // =========================
    // Events
    // =========================

    @SubscribeEvent
    public static void onDecreeLevelTick(TickEvent.LevelTickEvent.Post event) {
        if (WRITTEN.isEmpty() || !(event.level() instanceof ServerLevel sl) || sl.getGameTime() % 2 != 0) return;
        for (Decree decree : new ArrayList<>(WRITTEN)) {
            if (decree.closed || decree.level != sl || !decree.rules.contains(Rule.PROJECTILES)) continue;
            AABB box = new AABB(decree.center.x - decree.radius, decree.center.y - 1.5, decree.center.z - decree.radius,
                    decree.center.x + decree.radius, decree.center.y + decree.height, decree.center.z + decree.radius);
            for (Projectile projectile : sl.getEntitiesOfClass(Projectile.class, box, p -> !p.isRemoved())) {
                Decree stops = decree.stopping(projectile);
                if (stops != decree) continue;
                if (projectile.getOwner() instanceof LivingEntity shooter && decree.covers(shooter)) decree.broken(shooter, Rule.PROJECTILES);
                projectile.discard();
            }
        }
    }

    /** A projectile shot inside by one who is bound never flies. */
    @SubscribeEvent
    public static void onDecreeShot(EntityJoinLevelEvent event) {
        if (WRITTEN.isEmpty() || !(event.getEntity() instanceof Projectile projectile) || !(event.getLevel() instanceof ServerLevel sl)) return;
        if (!(projectile.getOwner() instanceof LivingEntity shooter) || !violate(shooter, Rule.PROJECTILES)) return;
        Later.run(sl, 1, projectile::discard);              // as AbilityLock does: gone as it is fired
    }

    @SubscribeEvent
    public static boolean onDecreeHeal(LivingHealEvent event) {
        if (WRITTEN.isEmpty() || event.getEntity().level().isClientSide()) return false;
        return violate(event.getEntity(), Rule.HEALING);
    }

    // HIGH: before Root's own refusal, so the try is counted
    @SubscribeEvent(priority = Priority.HIGH)
    public static boolean onDecreeTeleport(EntityTeleportEvent event) {
        if (WRITTEN.isEmpty() || event instanceof EntityTeleportEvent.TeleportCommand
                || event instanceof EntityTeleportEvent.SpreadPlayersCommand) return false;
        if (!(event.getEntity() instanceof LivingEntity living) || living.level().isClientSide()) return false;
        if (!violate(living, Rule.PASSAGE)) return false;
        if (living instanceof Player player) player.sendOverlayMessage(Component.literal("You cannot leave this place."));
        return true;
    }

    @SubscribeEvent
    public static boolean onDecreeTotem(LivingUseTotemEvent event) {
        if (WRITTEN.isEmpty() || event.getEntity().level().isClientSide()) return false;
        return violate(event.getEntity(), Rule.RESURRECTION);
    }

    /** Imperative: Reality: abilities are weak inside and plain blows strong (the owner's own are spared). */
    @SubscribeEvent
    public static void onDecreeRealityHurt(LivingHurtEvent event) {
        if (WRITTEN.isEmpty()) return;
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide()) return;
        DamageSource source = event.getSource();
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        if (realityOver(victim, source.getEntity()) == null) return;
        if (isAbilityHit(victim, source)) {
            event.setAmount(event.getAmount() * REALITY_ABILITY);
        } else if (source.getEntity() != null || source.is(DamageTypeTags.IS_PROJECTILE) || source.is(DamageTypeTags.IS_FALL)) {
            event.setAmount(event.getAmount() * REALITY_PLAIN);
        }
    }

    @SubscribeEvent
    public static void onDecreeLevelUnload(LevelEvent.Unload event) {
        WRITTEN.removeIf(decree -> decree.level == event.getLevel());
    }
}
