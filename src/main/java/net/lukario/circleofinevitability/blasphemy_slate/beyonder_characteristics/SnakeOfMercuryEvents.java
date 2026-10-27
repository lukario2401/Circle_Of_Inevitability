package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.combat.*;
import net.lukario.circleofinevitability.fate.*;
import net.lukario.circleofinevitability.root.*;
import net.lukario.circleofinevitability.status.*;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.Result;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.listener.*;
import net.minecraftforge.fml.common.Mod;

import java.util.*;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.SnakeOfMercuryPowers.*;

@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID)
public final class SnakeOfMercuryEvents {
    private static final List<Holder<MobEffect>> MIND = List.of(MobEffects.NAUSEA, MobEffects.BLINDNESS, MobEffects.SLOWNESS, MobEffects.DARKNESS);
    private static final class Ledger {
        final float multiplier;
        final Map<UUID, Float> attackers = new HashMap<>();
        final List<MobEffectInstance> effects = new ArrayList<>();
        float damage;
        Ledger(float multiplier) { this.multiplier = multiplier; }
    }
    private static final class Read {
        final long until;
        final float strength;
        int hits;
        Read(long until, float strength) { this.until = until; this.strength = strength; }
    }
    private static final Map<ServerLevel, Map<UUID, Map<UUID, Read>>> READS = new IdentityHashMap<>();
    static void read(ServerPlayer p, LivingEntity target, int ticks, boolean replace) {
        Map<UUID, Read> readings = READS.computeIfAbsent(p.level(), key -> new HashMap<>()).computeIfAbsent(p.getUUID(), key -> new HashMap<>());
        if (replace) readings.clear();
        readings.values().removeIf(read -> read.until <= p.level().getGameTime());
        if (readings.size() < 4 || readings.containsKey(target.getUUID())) readings.put(target.getUUID(), new Read(p.level().getGameTime() + ticks, Echo.strength(p)));
    }
    private record Opportunity(UUID owner, Vec3 at, double radius, long until) {}
    private record Patch(UUID owner, Vec3 at, long until) {}
    private record Curse(UUID owner, UUID target, long until) {}
    private static final Map<ServerLevel, Map<UUID, Ledger>> LEDGERS = new IdentityHashMap<>();
    private static final Map<ServerLevel, List<Opportunity>> OPPORTUNITIES = new IdentityHashMap<>();
    private static final Map<ServerLevel, List<Patch>> PATCHES = new IdentityHashMap<>();
    private static final Map<ServerLevel, List<Curse>> CURSES = new IdentityHashMap<>();
    private static boolean payingLedger;
    static {
        SnakeOfMercuryCycle.initialize();
        Fear.addImmunity(e -> active(e, "mind"));
    }
    private SnakeOfMercuryEvents() {}
    static void clearMind(LivingEntity e) { MIND.forEach(e::removeEffect); Fear.end(e); }
    static void openLedger(Player p, float multiplier) {
        ServerLevel sl = (ServerLevel) p.level();
        Ledger ledger = new Ledger(multiplier);
        LEDGERS.computeIfAbsent(sl, key -> new HashMap<>()).put(p.getUUID(), ledger);
        int ticks = (int) Math.max(1, p.getPersistentData().getLongOr(PREFIX + "ledger", 0L) - sl.getGameTime());
        Later.run(sl, ticks, () -> {
            Map<UUID, Ledger> current = LEDGERS.get(sl);
            if (current == null || current.get(p.getUUID()) != ledger) return;
            current.remove(p.getUUID());
            if (p instanceof ServerPlayer player && Spells.casterStillHere(player, sl)) settle(player, sl, ledger);
        });
    }
    static void opportunity(ServerPlayer p, Vec3 at, double radius, int ticks) {
        List<Opportunity> list = OPPORTUNITIES.computeIfAbsent(p.level(), k -> new ArrayList<>());
        list.removeIf(o -> o.owner.equals(p.getUUID()));
        list.add(new Opportunity(p.getUUID(), at, radius, p.level().getGameTime() + ticks));
    }
    static void curse(ServerPlayer p, LivingEntity t, int ticks) {
        List<Curse> list = CURSES.computeIfAbsent(p.level(), k -> new ArrayList<>());
        list.removeIf(c -> c.owner.equals(p.getUUID()) && c.target.equals(t.getUUID()));
        list.add(new Curse(p.getUUID(), t.getUUID(), p.level().getGameTime() + ticks));
    }
    static void mishap(LivingEntity target, boolean severe) { Misfortune.trigger(target, severe); }

    /** Existing single-key and wheel timestamps only; durations/resources/cooldown settings are untouched. */
    static void reduceCooldowns(Player p, int ticks, boolean reset) {
        CompoundTag data = p.getPersistentData(); long now = p.level().getGameTime();
        for (String key : new ArrayList<>(data.keySet())) {
            if (!key.startsWith("circle_of_inevitability_cooldown_") && !key.contains("_ready_")) continue;
            long ready = data.getLongOr(key, 0L);
            if (ready > now) data.putLong(key, reset ? now : Math.max(now, ready - ticks));
        }
    }

    @SubscribeEvent(priority = Priority.HIGH)
    public static boolean attack(LivingAttackEvent event) {
        LivingEntity victim = event.getEntity();
        if (!(victim.level() instanceof ServerLevel sl) || event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY) || Spells.isPiercing(victim)) return false;
        if (active(victim, "mercury_grace")) return true;
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)
                || !Substitute.isRealHit(victim, event.getSource(), event.getAmount())) return false;
        if (active(victim, "walk") && event.getSource().is(DamageTypeTags.IS_PROJECTILE)
                && victim.getPersistentData().getBooleanOr(PREFIX + "projectile", false)) {
            victim.getPersistentData().putBoolean(PREFIX + "projectile", false); return true;
        }
        List<Opportunity> opportunities = OPPORTUNITIES.get(sl);
        if (opportunities != null) for (Opportunity o : List.copyOf(opportunities)) {
            if (o.until <= sl.getGameTime() || victim.position().distanceToSqr(o.at) > o.radius * o.radius) continue;
            if (!(sl.getEntity(o.owner) instanceof ServerPlayer p) || !p.isAlive() || !Fate.applies(p) || !Spells.isEnemy(p, attacker)) continue;
            opportunities.remove(o); // consume before dealing retaliatory damage, preventing recursive triggers
            Misfortune.add(p, attacker, 4); mishap(attacker, true);
            for (LivingEntity ally : Spells.alliesAround(p, sl, o.at, o.radius)) if (ally instanceof Player player) reduceCooldowns(player, 160, false);
            if (p.position().distanceToSqr(o.at) <= o.radius * o.radius) reduceCooldowns(p, 160, false);
            Luck.add(p, 5); ring(sl, victim.position(), 2, GOLD, 20); return true;
        }
        double luck = Luck.balance(victim);
        double dodge = active(victim, "blessing") ? 1d / 3 : Math.min(.2, Math.max(0, luck) * .002);
        if (luck > 0 && victim.getRandom().nextDouble() < dodge) {
            Luck.add(victim, -Math.min(1, luck));
            sl.sendParticles(ParticleTypes.ENCHANT, victim.getX(), victim.getY() + 1, victim.getZ(), 8, .3, .4, .3, .1);
            return true;
        }
        return false;
    }

    @SubscribeEvent(priority = Priority.LOW)
    public static void hurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        if (!(victim.level() instanceof ServerLevel sl) || event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY) || Spells.isPiercing(victim)) return;
        LivingEntity dealer = event.getSource().getEntity() instanceof LivingEntity l ? l : DamageFx.abilityDealerOf(victim, event.getSource(), sl);
        if (dealer != null && dealer != victim && Spells.isEnemy(dealer, victim)) {
            Read read = READS.getOrDefault(sl, Map.of()).getOrDefault(dealer.getUUID(), Map.of()).get(victim.getUUID());
            if (read != null && read.until > sl.getGameTime()) {
                read.hits = Math.min(5, read.hits + 1);
                event.setAmount(event.getAmount() * (1 + read.hits * .1f * read.strength));
            }
            double luck = Luck.balance(dealer);
            double crit = active(dealer, "blessing") ? .2 : dealer instanceof Player p && Fate.applies(p) && sequence(p) <= 5 ? .1 : .05;
            if (luck > 0 && dealer.getRandom().nextDouble() < crit) { Luck.add(dealer, -Math.min(1, luck)); event.setAmount(event.getAmount() * 1.5f); }
            if (dealer instanceof Player p && active(p, "serpent") && !Spells.isSpellHit(victim)
                    && !event.getSource().is(DamageTypeTags.IS_PROJECTILE)
                    && victim.getPersistentData().getLongOr(PREFIX + "coil_ready", 0L) <= sl.getGameTime()) {
                Root.apply(victim, Spells.holdLength(victim, 20), RootRestriction.MOVE, RootRestriction.JUMP);
                victim.getPersistentData().putLong(PREFIX + "coil_ready", sl.getGameTime() + 60);
            }
        }
        if (victim instanceof Player p && Fate.applies(p) && sequence(p) <= 2 && event.getAmount() > p.getMaxHealth() / 3
                && p.getPersistentData().getLongOr(PREFIX + "body_ready", 0L) <= sl.getGameTime()) {
            p.getPersistentData().putLong(PREFIX + "body_ready", sl.getGameTime() + 1800);
            arm(p, "mercury_grace", 100, "Body of Mercury");
            p.getPersistentData().putBoolean(PREFIX + "mercury_reward", true);
            event.setAmount(0); return;
        }
        if (!payingLedger && active(victim, "ledger")) {
            Ledger ledger = LEDGERS.getOrDefault(sl, Map.of()).get(victim.getUUID());
            if (ledger != null) {
                float held = event.getAmount() * .5f; ledger.damage += held; event.setAmount(event.getAmount() - held);
                if (dealer != null && Spells.isEnemy(victim, dealer)) ledger.attackers.merge(dealer.getUUID(), held, Float::sum);
            }
        }
    }

    @SubscribeEvent
    public static void effects(MobEffectEvent.Applicable event) {
        LivingEntity e = event.getEntity(); if (!(e.level() instanceof ServerLevel sl)) return;
        MobEffectInstance effect = event.getEffectInstance();
        if (active(e, "mercury_grace") || active(e, "mind") && MIND.contains(effect.getEffect())) { event.setResult(Result.DENY); return; }
        if (active(e, "ledger") && !effect.getEffect().value().isBeneficial()) {
            Ledger ledger = LEDGERS.getOrDefault(sl, Map.of()).get(e.getUUID());
            if (ledger != null) { if (ledger.effects.size() < 32) ledger.effects.add(new MobEffectInstance(effect)); event.setResult(Result.DENY); }
        }
    }

    @SubscribeEvent
    public static boolean medium(AttackEntityEvent event) {
        Entity target = event.getTarget();
        if (target.level().isClientSide() || !target.getPersistentData().getBooleanOr(PREFIX + "medium", false)) return false;
        Player p = event.getEntity();
        float health = target.getPersistentData().getFloatOr(PREFIX + "medium_health", 40f);
        float damage = Math.max(1, (float) p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) * p.getAttackStrengthScale(.5f));
        target.getPersistentData().putFloat(PREFIX + "medium_health", health - damage);
        p.resetAttackStrengthTicker();
        if (health <= damage) target.discard();
        return true;
    }

    @SubscribeEvent
    public static void mediumProjectile(net.minecraftforge.event.entity.ProjectileImpactEvent event) {
        if (event.getProjectile().level().isClientSide() || !(event.getRayTraceResult() instanceof net.minecraft.world.phys.EntityHitResult hit)
                || !hit.getEntity().getPersistentData().getBooleanOr(PREFIX + "medium", false)) return;
        Entity medium = hit.getEntity();
        float damage = (float) Math.clamp(event.getProjectile().getDeltaMovement().length() * 2, 2, 20);
        float health = medium.getPersistentData().getFloatOr(PREFIX + "medium_health", 40f) - damage;
        medium.getPersistentData().putFloat(PREFIX + "medium_health", health);
        if (health <= 0) medium.discard();
        event.setImpactResult(net.minecraftforge.event.entity.ProjectileImpactEvent.ImpactResult.STOP_AT_CURRENT_NO_DAMAGE);
        event.getProjectile().discard();
    }

    @SubscribeEvent
    public static boolean knockback(LivingKnockBackEvent event) {
        return !event.getEntity().level().isClientSide() && active(event.getEntity(), "mind");
    }

    @SubscribeEvent
    public static void drops(LivingDropsEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel sl) || event.getEntity() instanceof Player) return;
        LivingEntity dealer = event.getSource().getEntity() instanceof LivingEntity l ? l : DamageFx.abilityDealerOf(event.getEntity(), event.getSource(), sl);
        if (dealer == null || Luck.balance(dealer) <= 0 || dealer.getRandom().nextDouble() >= Math.min(.2, Luck.balance(dealer) * .002)) return;
        // Only increase an existing ordinary stack; never duplicate equipment, containers or unique items.
        for (net.minecraft.world.entity.item.ItemEntity drop : event.getDrops()) {
            var stack = drop.getItem();
            if (stack.getMaxStackSize() <= 1 || stack.getCount() >= stack.getMaxStackSize() || !stack.getComponentsPatch().isEmpty()) continue;
            stack.grow(1); Luck.add(dealer, -Math.min(1, Luck.balance(dealer))); return;
        }
    }

    @SubscribeEvent
    public static void tick(LivingEvent.LivingTickEvent event) {
        LivingEntity e = event.getEntity(); if (!(e.level() instanceof ServerLevel sl) || !e.isAlive()) return;
        if (active(e, "mind")) clearMind(e);
        if (e.tickCount % 20 == 0 && Luck.balance(e) < 0 && e.getRandom().nextDouble() < -Luck.balance(e) / 500) {
            if (e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20, 1))) Luck.add(e, Math.min(1, -Luck.balance(e)));
        }
        if (!(e instanceof ServerPlayer p)) return;
        Map<UUID, Ledger> ledgers = LEDGERS.get(sl);
        if (ledgers != null && !active(p, "ledger")) {
            Ledger ledger = ledgers.remove(p.getUUID());
            if (ledger != null) settle(p, sl, ledger);
        }
        if (!active(p, "mercury_grace") && p.getPersistentData().getBooleanOr(PREFIX + "mercury_reward", false)) {
            p.getPersistentData().remove(PREFIX + "mercury_reward"); Luck.add(p, 10);
        }
        if (active(p, "walk") && p.tickCount % 10 == 0) {
            Spells.keepEffect(p, MobEffects.SPEED, 1, 15); Unnoticed.hide(p, 15);
            List<Patch> patches = PATCHES.computeIfAbsent(sl, key -> new ArrayList<>());
            long count = patches.stream().filter(patch -> patch.owner.equals(p.getUUID())).count();
            if (count < 32) patches.add(new Patch(p.getUUID(), p.position(), sl.getGameTime() + 160));
        }
        if (active(p, "serpent") && (!Fate.applies(p) || sequence(p) > 4)) p.getPersistentData().remove(PREFIX + "serpent");
        if (active(p, "serpent") && p.tickCount % 5 == 0) {
            Spells.keepEffect(p, MobEffects.SPEED, sequence(p) <= 2 ? 2 : 1, 10);
            Spells.keepEffect(p, MobEffects.RESISTANCE, sequence(p) <= 2 ? 1 : 0, 10);
            serpentFx(p, sl);
            if (sequence(p) > 2 && sl.getGameTime() >= p.getPersistentData().getLongOr(PREFIX + "serpent_fray_at", Long.MAX_VALUE)) {
                Luck.add(p, -1); p.getPersistentData().putLong(PREFIX + "serpent_fray_at", sl.getGameTime() + 200);
            }
        }
        if (p.tickCount % 10 == 0 && active(p, "calculation")) {
            String id = p.getPersistentData().getStringOr(PREFIX + "read_target", "");
            try { if (sl.getEntity(UUID.fromString(id)) instanceof LivingEntity target && target.isAlive())
                ring(sl, target.position().add(target.getLookAngle().scale(2)), .5, RED, 10); } catch (IllegalArgumentException ignored) { }
        }
        if (p.tickCount % 20 == 0) {
            if (active(p, "attraction")) for (LivingEntity t : Spells.enemiesAround(p, sl, p.position(), p.getPersistentData().getDoubleOr(PREFIX + "attraction_radius", 10d))) Misfortune.add(p, t, 1);
            if (active(p, "aura")) for (LivingEntity t : Spells.enemiesAround(p, sl, p.position(), p.getPersistentData().getDoubleOr(PREFIX + "aura_radius", 10d))) {
                if (Spells.isBoss(t)) continue;
                Misfortune.add(p, t, 2); Falter.apply(t, "snake_chaos", .25f, 25);
                if (t.getRandom().nextFloat() < .25f) Root.apply(t, 20, RootRestriction.ABILITIES);
            }
        }
        if (!Fate.applies(p)) return;
        if (sequence(p) <= 8 && p.tickCount % 5 == 0) { Spells.keepEffect(p, MobEffects.STRENGTH, 0, 10); Spells.keepEffect(p, MobEffects.SPEED, 0, 10); }
        if (sequence(p) <= 4 && p.tickCount % 100 == 0) for (LivingEntity t : Spells.enemiesAround(p, sl, p.position(), 6)) Misfortune.add(p, t, 1);
        if (p.tickCount % 20 == 0) {
            boolean hunted = Spells.enemiesAround(p, sl, p.position(), 24).stream().anyMatch(t -> t instanceof Mob mob && mob.getTarget() == p
                    && !Spells.clearLine(p, sl, p.getEyePosition(), t.getEyePosition()));
            if (hunted && p.getPersistentData().getLongOr(PREFIX + "warning_ready", 0L) <= sl.getGameTime()) {
                p.getPersistentData().putLong(PREFIX + "warning_ready", sl.getGameTime() + 100);
                Spells.sound(sl, p.position(), net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME, .35f, 1.5f);
            }
        }
        SnakeOfMercuryCycle.playerTick(p, sl);
    }
    private static void serpentFx(ServerPlayer p, ServerLevel sl) {
        double phase = sl.getGameTime() * .06;
        for (int i = 0; i < 24; i++) {
            double angle = phase + i * Math.PI * 2 / 24;
            double radius = sequence(p) <= 2 ? 3.5 : 2;
            Vec3 at = p.position().add(Math.cos(angle) * radius, .5 + i / 24d * 2, Math.sin(angle) * radius);
            sl.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, 1, .02, .02, .02, 0);
        }
    }
    private static void settle(ServerPlayer p, ServerLevel sl, Ledger ledger) {
        UUID id = ledger.attackers.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);
        if (id == null || !(sl.getEntity(id) instanceof LivingEntity target) || !target.isAlive() || !Spells.isEnemy(p, target)) return;
        payingLedger = true;
        try { Spells.strike(p, target, ledger.damage * ledger.multiplier); ledger.effects.forEach(effect -> target.addEffect(new MobEffectInstance(effect))); }
        finally { payingLedger = false; }
        ring(sl, target.position(), 2, RED, 25);
    }

    @SubscribeEvent(priority = Priority.LOWEST)
    public static void damaged(LivingDamageEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel sl) || event.getAmount() <= 0) return;
        LivingEntity victim = event.getEntity();
        if (victim instanceof ServerPlayer p && Fate.applies(p) && sequence(p) <= 3) Spells.enemiesAround(p, sl, p.position(), 6).stream()
                .min(Comparator.comparingDouble(t -> t.distanceToSqr(p))).ifPresent(t -> Misfortune.add(p, t, 1));
        if (Misfortune.sourceId(victim) != null && sl.getEntity(Misfortune.sourceId(victim)) instanceof ServerPlayer p && active(p, "attraction")
                && Spells.isEnemy(p, victim) && Misfortune.isMishapDamage(victim))
            p.setAbsorptionAmount(Math.min(p.getMaxHealth(), p.getAbsorptionAmount() + event.getAmount() * .25f));
    }
    @SubscribeEvent
    public static void level(TickEvent.LevelTickEvent.Post event) {
        if (!(event.level() instanceof ServerLevel sl)) return;
        SnakeOfMercuryCycle.levelTick(sl);
        if (sl.getGameTime() % 20 != 0) return;
        Map<UUID, Map<UUID, Read>> reads = READS.get(sl);
        if (reads != null) reads.entrySet().removeIf(entry -> {
            entry.getValue().values().removeIf(read -> read.until <= sl.getGameTime());
            return entry.getValue().isEmpty() || sl.getEntity(entry.getKey()) == null;
        });
        List<Opportunity> opportunities = OPPORTUNITIES.get(sl);
        if (opportunities != null) opportunities.removeIf(o -> o.until <= sl.getGameTime() || !(sl.getEntity(o.owner) instanceof ServerPlayer p) || !p.isAlive());
        List<Patch> patches = PATCHES.get(sl);
        if (patches != null) {
            patches.removeIf(patch -> patch.until <= sl.getGameTime() || !(sl.getEntity(patch.owner) instanceof ServerPlayer p) || !p.isAlive());
            // One application per victim per caster per second, even where patches overlap.
            Map<UUID, Set<UUID>> touched = new HashMap<>();
            for (Patch patch : patches) if (sl.getEntity(patch.owner) instanceof ServerPlayer p)
                for (LivingEntity t : Spells.enemiesAround(p, sl, patch.at, 3))
                    if (touched.computeIfAbsent(p.getUUID(), key -> new HashSet<>()).add(t.getUUID())) Misfortune.add(p, t, 1);
        }
        List<Curse> curses = CURSES.get(sl);
        if (curses != null) {
            curses.removeIf(c -> c.until <= sl.getGameTime() || !(sl.getEntity(c.owner) instanceof ServerPlayer p) || !p.isAlive() || sl.getEntity(c.target) == null);
            for (Curse curse : curses) if (sl.getEntity(curse.target) instanceof LivingEntity target) mishap(target, true);
        }
    }
    @SubscribeEvent
    public static void death(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) { clearPlayer(p); SnakeOfMercuryCycle.forget(p); }
    }
    private static void clearPlayer(ServerPlayer p) {
        Map<UUID, Map<UUID, Read>> reads = READS.get(p.level()); if (reads != null) reads.remove(p.getUUID());
        Map<UUID, Ledger> ledger = LEDGERS.get(p.level()); if (ledger != null) ledger.remove(p.getUUID());
        for (String key : new ArrayList<>(p.getPersistentData().keySet())) if (key.startsWith(PREFIX) && !key.equals(PREFIX + "body_ready") && !key.equals(PREFIX + "warning_ready")) p.getPersistentData().remove(key);
    }
    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) { clearPlayer(p); SnakeOfMercuryCycle.forget(p); }
    }
    @SubscribeEvent
    public static void unload(LevelEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel sl)) return;
        SnakeOfMercuryCycle.clear(sl); READS.remove(sl); LEDGERS.remove(sl); OPPORTUNITIES.remove(sl); PATCHES.remove(sl); CURSES.remove(sl);
    }
}
