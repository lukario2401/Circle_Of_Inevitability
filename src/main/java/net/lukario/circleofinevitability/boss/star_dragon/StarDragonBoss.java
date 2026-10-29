package net.lukario.circleofinevitability.boss.star_dragon;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.star_dragon.*;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.*;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.*;
import java.util.*;

/** Sequence 2/0 encounters: animation windup -> checked shared cast -> recovery. No second random casting brain. */
public class StarDragonBoss extends Monster implements DirectedCaster {
    private static final EntityDataAccessor<String> ANIMATION = SynchedEntityData.defineId(StarDragonBoss.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Long> ANIMATION_START = SynchedEntityData.defineId(StarDragonBoss.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Integer> ANIMATION_LENGTH = SynchedEntityData.defineId(StarDragonBoss.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> RADIANT = SynchedEntityData.defineId(StarDragonBoss.class, EntityDataSerializers.BOOLEAN);
    private final ServerBossEvent bossBar = new ServerBossEvent(UUID.randomUUID(), Component.translatable("entity.circle_of_inevitability.star_dragon_boss"),
            BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10);
    private MobAbility pending;
    private StarDragonBossSettings.Attack pendingSettings;
    private LivingEntity pendingTarget;
    private int releaseAt, nextDecision;
    private boolean initialized, hovering, gravityBeforeHover;
    private String mode = "auto", lastAttack = "";
    private UUID commandedTarget;
    private final Set<String> disabled = new HashSet<>();
    private final Map<String, Integer> weights = new HashMap<>();

    public StarDragonBoss(EntityType<? extends StarDragonBoss> type, Level level) {
        super(type, level);
        xpReward = type == StarDragonBossRegistry.CONDENSER.get() ? 500 : 150;
        setPersistenceRequired();
    }
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 600).add(Attributes.ATTACK_DAMAGE, 14)
                .add(Attributes.MOVEMENT_SPEED, .25).add(Attributes.FOLLOW_RANGE, 48)
                .add(Attributes.ARMOR, 16).add(Attributes.KNOCKBACK_RESISTANCE, 1);
    }
    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1, false) {
            @Override public boolean canUse() { return mode.equals("auto") && pending == null && super.canUse(); }
            @Override public boolean canContinueToUse() { return mode.equals("auto") && pending == null && super.canContinueToUse(); }
        });
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(ANIMATION, "idle"); b.define(ANIMATION_START, 0L); b.define(ANIMATION_LENGTH, 1); b.define(RADIANT, false);
    }
    public String animation() { return entityData.get(ANIMATION); }
    public long animationStart() { return entityData.get(ANIMATION_START); }
    public int animationLength() { return entityData.get(ANIMATION_LENGTH); }
    public boolean radiant() { return entityData.get(RADIANT); }
    public int encounterSequence() { return getType() == StarDragonBossRegistry.CONDENSER.get() ? 0 : 2; }
    public StarDragonBossSettings settings() { return StarDragonBossSettings.get(encounterSequence()); }
    public boolean enraged() { return getHealth() / getMaxHealth() < settings().enragedBelow(); }
    public int windupTicks(StarDragonBossSettings.Attack attack) {
        return Math.max(1, (int)Math.round(attack.windupTicks() * (enraged() ? settings().enragedWindupMultiplier() : 1)));
    }
    private int recoveryTicks(StarDragonBossSettings.Attack attack) {
        return Math.max(0, (int)Math.round(attack.recoveryTicks() * (enraged() ? settings().enragedRecoveryMultiplier() : 1)));
    }
    /** Encounter adapters keep the player's original self-sacrifice spell and other mobs unchanged. */
    public MobAbility configuredAbility(MobAbility base) {
        MobAbility tuned = settings().attack(base).tune(base);
        if (encounterSequence() == 0 && base.id().equals("Star Dragon/7/singularity")) {
            var profile = settings().singularity();
            tuned.when((mob, target, sl) -> true).cast((mob, target, sl) ->
                    StarDragonMobs.bossSingularity(mob, target, sl, profile.healthCostFraction(), profile.spentTicks(), profile.damageCap()));
        }
        if (base.id().equals("Star Dragon/7/star_dragon_form"))
            tuned.when((mob, target, sl) -> MobForm.current(mob) == null);
        return tuned;
    }
    public String mode() { return mode; }
    public void mode(String value) { mode = value; lastAttack = ""; stopHover(); cancelWindup(); getNavigation().stop(); if (value.equals("passive")) { setTarget(null); MobForm.end(this); } }
    public void target(LivingEntity target) { commandedTarget = target.getUUID(); setTarget(target); }
    public void clearTarget() { commandedTarget = null; lastAttack = ""; setTarget(null); cancelWindup(); }
    public void enable(String id, boolean enabled) { if (enabled) disabled.remove(id); else disabled.add(id); cancelWindup(); }
    public boolean attackEnabled(MobAbility base) { return settings().attack(base).enabled() && !disabled.contains(base.id()); }
    public int attackWeight(MobAbility base) { return weights.getOrDefault(base.id(), settings().attack(base).weight()); }
    public void weight(String id, int weight) { weights.put(id, weight); }
    public void resetAttacks() { disabled.clear(); weights.clear(); lastAttack = ""; cancelWindup(); MobCasting.clearCooldowns(this); }
    public void cancelWindup() { pending = null; pendingTarget = null; pendingSettings = null; animate("idle", 1); }
    public void applySettings() {
        lastAttack = "";
        var s = settings();
        setBase(Attributes.MAX_HEALTH, s.health()); setBase(Attributes.ATTACK_DAMAGE, s.meleeDamage());
        setBase(Attributes.MOVEMENT_SPEED, s.speed()); setBase(Attributes.FOLLOW_RANGE, s.followRange());
        setHealth(Math.min(getHealth(), getMaxHealth()));
        cancelWindup();
    }
    private void setBase(net.minecraft.core.Holder<Attribute> key, double value) {
        AttributeInstance a = getAttribute(key); if (a != null) a.setBaseValue(value);
    }
    @Override public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) return;
        if (!initialized) {
            // Persistent data is restored before the first tick. Never refill health or spirituality on reload.
            boolean fresh = !getPersistentData().getBooleanOr("star_dragon_boss_initialized", false);
            if (fresh) {
                BeyonderMobs.make(this, StarDragon.PATHWAY, encounterSequence());
                getPersistentData().putBoolean("star_dragon_boss_initialized", true);
            }
            applySettings();
            if (fresh) setHealth(getMaxHealth());
            initialized = true;
        }
        bossBar.setName(getDisplayName());
        bossBar.setProgress(getHealth() / getMaxHealth());
        if (!isAlive()) { stopHover(); bossBar.setVisible(false); cancelWindup(); return; }
        // Form id is supplied by the characteristic, never inferred from a cosmetic phase.
        entityData.set(RADIANT, StarDragonForms.isRadiantAngel(this));
        bossBar.setColor(radiant() ? BossEvent.BossBarColor.YELLOW : BossEvent.BossBarColor.PURPLE);
        if (isNoAi()) { stopHover(); if (pending != null) cancelWindup(); return; }
        if (mode.equals("passive")) { setTarget(null); return; }
        if (settings().passives()) StarDragonMobs.bossPassiveTick(this, sl);
        if (commandedTarget != null) {
            Entity e = sl.getEntity(commandedTarget);
            if (e instanceof LivingEntity living && validTarget(living)) setTarget(living);
            else { commandedTarget = null; setTarget(null); }
        }
        updateHover(sl);
        if (!BeyonderMobRules.MOBS_CAST) { if (pending != null) cancelWindup(); return; }
        if (pending != null) {
            getNavigation().stop();
            if (!validTarget(pendingTarget) || !MobCasting.canCast(this, pending)) { cancelWindup(); return; }
            MobSpells.face(this, pendingTarget);
            if (tickCount >= releaseAt) {
                MobAbility cast = pending; LivingEntity target = pendingTarget; var settings = pendingSettings;
                pending = null; pendingTarget = null; pendingSettings = null;
                int recovery = recoveryTicks(settings);
                boolean success = MobCasting.tryCast(this, cast, target, sl, recovery);
                nextDecision = tickCount + (success ? recovery : settings().thinkTicks());
                if (!success) animate("idle", 1);
            }
        } else if (mode.equals("auto") && tickCount >= nextDecision && tickCount % settings().thinkTicks() == 0) {
            chooseAttack(sl);
        }
    }
    private void stopHover() {
        if (hovering) { setNoGravity(gravityBeforeHover); hovering=false; }
    }
    private void updateHover(ServerLevel sl) {
        LivingEntity target=getTarget();
        if (!(radiant() || StarDragonForms.isDragon(this)) || !settings().radiantFlight() || !mode.equals("auto") || !validTarget(target)
                || Root.has(this,RootRestriction.MOVE)) { stopHover(); return; }
        if (!hovering) { gravityBeforeHover=isNoGravity(); hovering=true; setNoGravity(true); }
        getNavigation().stop();
        var delta=target.position().subtract(position());
        double horizontal=delta.horizontalDistance();
        double speed=pending==null && horizontal>8 ? .14 : 0;
        var step=new net.minecraft.world.phys.Vec3(horizontal>.01?delta.x/horizontal*speed:0,
                Math.clamp((target.getY()+settings().hoverHeight()-getY())*.06,-.14,.14),
                horizontal>.01?delta.z/horizontal*speed:0);
        if (sl.noCollision(this,getBoundingBox().move(step))) setDeltaMovement(step);
        else setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        hurtMarked=true;
    }
    private boolean validTarget(LivingEntity target) {
        return target != null && target.isAlive() && target.level() == level() && !target.isSpectator()
                && !(target instanceof Player p && p.isCreative()) && !Spells.isFriend(this, target);
    }
    private void chooseAttack(ServerLevel sl) {
        LivingEntity target = getTarget();
        if (!validTarget(target) || distanceTo(target) > settings().followRange()) return;
        // Ordered follow-ups get first choice once; every normal gate still applies. A blocked chain falls back immediately.
        String previous = lastAttack; lastAttack = "";
        MobAbility last = MobAbilities.byId(previous);
        if (last != null) for (String id : settings().attack(last).followups()) {
            MobAbility followup = MobAbilities.byId(id);
            if (followup != null && attackWeight(followup) > 0 && queueAttack(followup, target)) return;
        }
        List<MobAbility> choices = new ArrayList<>(); int total = 0;
        for (MobAbility base : MobAbilities.knownBy(this)) {
            var a = settings().attack(base);
            MobAbility tuned = configuredAbility(base).weight(weights.getOrDefault(base.id(), a.weight()));
            double hp = getHealth() / getMaxHealth();
            if (hp < settings().enragedBelow() && base.key() >= 6)
                tuned.weight((int)Math.round(tuned.weight() * settings().enragedWeightMultiplier()));
            if (!a.enabled() || disabled.contains(base.id()) || tuned.weight() <= 0 || hp < a.minHealth() || hp > a.maxHealth()) continue;
            if (!MobCasting.canCast(this, tuned) || !MobCasting.inReach(this, tuned, target)) continue;
            if (tuned.when() != null && !tuned.when().test(this, target, sl)) continue;
            choices.add(tuned); total += tuned.weight();
        }
        if (total == 0) return;
        int roll = getRandom().nextInt(total);
        for (MobAbility a : choices) { roll -= a.weight(); if (roll < 0) { queueAttack(a, target); break; } }
    }
    /** Manual cast uses the same windup, costs, sequence gates and blockers as autonomous combat. */
    public boolean queueAttack(MobAbility base, LivingEntity target) {
        if (!(level() instanceof ServerLevel sl) || pending != null || !validTarget(target) || !BeyonderMobRules.MOBS_CAST
                || isNoAi() || mode.equals("passive")) return false;
        var a = settings().attack(base); MobAbility tuned = configuredAbility(base);
        double hp = getHealth() / getMaxHealth();
        if (!a.enabled() || disabled.contains(base.id()) || hp < a.minHealth() || hp > a.maxHealth()
                || !MobCasting.canCast(this, tuned) || !MobCasting.inReach(this, tuned, target)
                || (tuned.when() != null && !tuned.when().test(this, target, sl))) return false;
        pending = tuned; pendingTarget = target; pendingSettings = a;
        int windup = windupTicks(a);
        releaseAt = tickCount + windup; getNavigation().stop(); MobSpells.face(this, target);
        animate(a.animation(), windup);
        playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 2, .6f);
        return true;
    }
    @Override public float abilityDamageMultiplier() { return (float)settings().abilityDamageMultiplier(); }
    @Override public void onAbilityCast(MobAbility ability) {
        lastAttack = ability.id();
        animate(settings().attack(ability).animation(), 14);
    }
    private void animate(String clip, int length) {
        entityData.set(ANIMATION, clip); entityData.set(ANIMATION_START, level().getGameTime()); entityData.set(ANIMATION_LENGTH, Math.max(1, length));
    }
    @Override public boolean doHurtTarget(ServerLevel sl, Entity target) {
        if (!mode.equals("auto") || pending != null || Root.has(this, RootRestriction.ATTACK)) return false;
        animate("bite", 14);
        boolean hit = super.doHurtTarget(sl, target);
        if (hit && target instanceof LivingEntity living && settings().passives()) StarDragonMobs.bossMelee(this, living, sl);
        return hit;
    }
    @Override public boolean causeFallDamage(double distance, float modifier, net.minecraft.world.damagesource.DamageSource source) { return false; }
    @Override protected SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) { return SoundEvents.ENDER_DRAGON_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.ENDER_DRAGON_DEATH; }
    @Override public void startSeenByPlayer(ServerPlayer player) { super.startSeenByPlayer(player); bossBar.addPlayer(player); }
    @Override public void stopSeenByPlayer(ServerPlayer player) { super.stopSeenByPlayer(player); bossBar.removePlayer(player); }
    @Override public void remove(RemovalReason reason) {
        if (!level().isClientSide()) { stopHover(); MobForm.end(this); }
        bossBar.removeAllPlayers(); super.remove(reason);
    }
    @Override protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putBoolean("BossHovering", hovering); out.putBoolean("BossGravityBeforeHover", gravityBeforeHover);
        out.putString("BossMode", mode); out.putString("BossTarget", commandedTarget == null ? "" : commandedTarget.toString());
        out.putString("BossDisabled", String.join(";", disabled));
        out.putString("BossWeights", weights.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).collect(java.util.stream.Collectors.joining(";")));
    }
    @Override protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        hovering = in.getBooleanOr("BossHovering", false); gravityBeforeHover = in.getBooleanOr("BossGravityBeforeHover", false);
        stopHover();
        initialized=false; lastAttack=""; pending=null; pendingTarget=null; pendingSettings=null;
        mode = in.getStringOr("BossMode", "auto"); if (!Set.of("auto", "manual", "passive").contains(mode)) mode = "auto";
        try { commandedTarget = UUID.fromString(in.getStringOr("BossTarget", "")); } catch (IllegalArgumentException ignored) { commandedTarget = null; }
        disabled.clear(); for (String id : in.getStringOr("BossDisabled", "").split(";")) if (!id.isEmpty()) disabled.add(id);
        weights.clear(); for (String pair : in.getStringOr("BossWeights", "").split(";")) {
            String[] parts = pair.split("=");
            try { if (parts.length == 2) weights.put(parts[0], Math.max(0, Math.min(10000, Integer.parseInt(parts[1])))); } catch (NumberFormatException ignored) { }
        }
    }
}
