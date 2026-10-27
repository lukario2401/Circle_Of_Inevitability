package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonder;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.SoulCore;
import net.lukario.circleofinevitability.combat.*;
import net.lukario.circleofinevitability.fate.*;
import net.lukario.circleofinevitability.menu.AbilityMenu;
import net.lukario.circleofinevitability.menu.AbilityWheel;
import net.lukario.circleofinevitability.root.*;
import net.lukario.circleofinevitability.status.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/** Native keys and menus. Resource-producing actions are never copyable. */
public final class SnakeOfMercuryPowers {
    public static final int SILVER = 0xC9D3E0, GOLD = 0xF4CE62, RED = 0xE75060;
    static final String PREFIX = "snake_power_";
    private static final String ICON = "snake_of_mercury/omen_gold";
    public static final MeleeCombo MELEE_COMBO = MeleeCombo.forPathway(Fate.PATHWAY)
            .step((p, t, sl) -> ring(sl, t.position(), 1, SILVER, 12))
            .step((p, t, sl) -> ring(sl, p.position(), 2, RED, 12))
            .randomStep((p, t, sl) -> ring(sl, t.position().add(0, 1, 0), .5, GOLD, 15),
                    (p, t, sl) -> ring(sl, t.position().add(0, 1, 0), .5, RED, 15));
    private SnakeOfMercuryPowers() {}

    private static AbilityWheel.Cast checked(int sequence, AbilityWheel.Cast action) {
        return (p, sl) -> allowed(p, sequence, Casts.isCopying(p)) ? action.cast(p, sl) : AbilityWheel.Result.REFUSED;
    }
    static final AbilityWheel MISFORTUNE = AbilityWheel.create("snake_of_mercury_misfortune", "Misfortune")
            .entry("bestow", ICON, "Misfortune Bestowal", 5, 3000, 100, checked(5, SnakeOfMercuryPowers::misfortune)).notCopyable()
            .entry("gambit", ICON, "Winner's Gambit", 5, 3000, 300, checked(5, SnakeOfMercuryPowers::gambit)).notCopyable()
            .entry("chain", ICON, "Misfortune Chain", 4, 6000, 160, checked(4, SnakeOfMercuryPowers::chain))
            .entry("serpent", ICON, "Silver Serpent Form", 4, 6000, 0, checked(4, SnakeOfMercuryPowers::serpent))
            .free(p -> active(p, "serpent")).notCopyable()
            .onCast(SnakeOfMercuryPowers::misfortuneFinished);
    static final AbilityWheel CHAOS = AbilityWheel.create("snake_of_mercury_chaos", "Chaos and Prophecy")
            .entry("walk", ICON, "Chaos Walk", 3, 9000, 120, checked(3, SnakeOfMercuryPowers::walk))
            .entry("aura", ICON, "Aura of Chaos", 3, 9000, 240, checked(3, SnakeOfMercuryPowers::aura))
            .entry("words", ICON, "Words of Fortune and Misfortune", 2, 0, 0, checked(2, SnakeOfMercuryPowers::wordsMenu)).notCopyable()
            .entry("conceal", ICON, "Fate Concealment", 2, 12000, 160, checked(2, SnakeOfMercuryPowers::conceal))
            .entry("opportunity", ICON, "Arranged Opportunity", 2, 12000, 400, checked(2, SnakeOfMercuryPowers::opportunity)).notCopyable()
            .onCast((p, sl, entry) -> finish(p));
    static final AbilityWheel CYCLE = AbilityWheel.create("snake_of_mercury_cycle", "Cycle")
            .entry("reboot", ICON, "Reboot", 1, 20000, 2400, checked(1, SnakeOfMercuryCycle::reboot)).notCopyable()
            .entry("loop", ICON, "Fate Loop", 1, 20000, 400, checked(1, SnakeOfMercuryCycle::loop)).notCopyable()
            .entry("connection", ICON, "Fated Connection", 1, 20000, 0, checked(1, SnakeOfMercuryCycle::connection))
            .free(SnakeOfMercuryCycle::aimingAtAnchor).notCopyable()
            .entry("reincarnation", ICON, "Reincarnation", 1, 20000, 18000, checked(1, SnakeOfMercuryCycle::reincarnation)).notCopyable()
            .entry("wheel", ICON, "The Wheel of Fortune", 0, 40000, 6000, checked(0, SnakeOfMercuryCycle::wheel))
            .free(p -> active(p, "sky")).notCopyable()
            .entry("authority", ICON, "Fate Authority", 0, 0, 0, checked(0, SnakeOfMercuryPowers::authority)).notCopyable()
            .onCast((p, sl, entry) -> finish(p));

    static boolean allowed(Player p, int sequence, boolean borrowed) {
        return !p.level().isClientSide() && p.isAlive() && Beyonder.hasSequence(p, sequence)
                && (borrowed || Fate.PATHWAY.equals(SoulCore.getPathway(p)));
    }
    static boolean active(LivingEntity p, String key) { return p.getPersistentData().getLongOr(PREFIX + key, 0L) > p.level().getGameTime(); }
    static void arm(Player p, String key, int ticks, String label) {
        long until = p.level().getGameTime() + ticks;
        if (Casts.isCopying(p)) until = Math.max(until, p.getPersistentData().getLongOr(PREFIX + key, 0L));
        p.getPersistentData().putLong(PREFIX + key, until);
        if (ticks < Integer.MAX_VALUE / 2) HudTimer.show(p, PREFIX + key, label, ticks, SILVER);
    }
    static WheelTier material(Player p) { return Casts.isCopying(p) ? WheelTier.GOLD : Fate.tier(p); }
    static int duration(Player p, int ticks) { return Math.max(1, Math.round(ticks * material(p).duration * Echo.strength(p))); }
    static int sequence(Player p) { return SoulCore.getSequence(p); }
    static void finish(Player p) { if (Fate.applies(p) && !Casts.isCopying(p)) Fate.consume(p); }
    static AbilityWheel.Result refuse(Player p, String text) { p.sendSystemMessage(Component.literal(text)); return AbilityWheel.Result.REFUSED; }
    static void ring(ServerLevel sl, Vec3 at, double radius, int color, int ticks) { SpellFx.runeCircle(sl, at, radius, color, ticks, 3); }

    /** Any living target: allegiance never changes the sign of a bestowal. */
    static LivingEntity aim(Player p, ServerLevel sl, double range) {
        Vec3 from = p.getEyePosition(), to = Spells.aimPoint(p, sl, range);
        return sl.getEntitiesOfClass(LivingEntity.class, new AABB(from, to).inflate(.5), e -> e != p && e.isAlive()
                && !e.isSpectator() && !Concealment.isHidden(e) && !(e instanceof ArmorStand) && Spells.canBeChosen(p, e)
                && e.getBoundingBox().inflate(.4).clip(from, to).isPresent()).stream()
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(p))).orElse(null);
    }
    static LivingEntity targetOrSelf(Player p, ServerLevel sl, double range) { LivingEntity t = aim(p, sl, range); return t == null ? p : t; }

    public static void calculation(Player p, Level level, ServerLevel sl, boolean borrowed) {
        if (!allowed(p, 8, borrowed)) return;
        LivingEntity target = p.isShiftKeyDown() ? null : Spells.aimEnemy(p, sl, 32 * material(p).range);
        if (!p.isShiftKeyDown() && target == null) { refuse(p, "Aim at an enemy to calculate its attacks."); return; }
        if (!Spells.paySpirituality(p, 200)) return;
        if (p.isShiftKeyDown()) {
            arm(p, "mind", duration(p, 160), "Machine Mind");
            SnakeOfMercuryEvents.clearMind(p);
        } else {
            arm(p, "calculation", duration(p, 240), "Calculation");
            SnakeOfMercuryEvents.read((ServerPlayer) p, target, duration(p, 240), !Casts.isCopying(p));
            if (!Casts.isCopying(p)) p.getPersistentData().putString(PREFIX + "read_target", target.getUUID().toString());
            ring(sl, target.position(), 1, SILVER, 30);
        }
        finish(p);
    }

    public static void luck(Player p, Level level, ServerLevel sl, boolean borrowed) {
        if (!allowed(p, 7, borrowed) || Casts.isCopying(p)) return;
        if (p.isShiftKeyDown()) {
            if (sequence(p) > 4 && !Cooldowns.ready(p, "snake_of_mercury_spin", "Spin the Wheel")) return;
            if (!Spells.paySpirituality(p, 250)) return;
            Fate.spin(p);
            if (sequence(p) > 4) Cooldowns.start(p, "snake_of_mercury_spin", 120);
            ring(sl, p.position(), 1.3, SILVER, 15);
            return;
        }
        AbilityMenu menu = AbilityMenu.create("snake_bestow").title("Bestow signed luck · aim at a living target").ring(110);
        for (int amount : new int[]{5,10,20,-5,-10,-20}) {
            menu.option(ICON, String.format(Locale.ROOT, "%+d Luck", amount), 0, 0, (player, world) -> bestow(player, world, amount, false))
                    .details(player -> "1250 spirituality · transfer existing reserve");
        }
        if (sequence(p) <= 2) {
            for (int amount : new int[]{20,-20}) menu.option(ICON, String.format(Locale.ROOT, "%+d shared Luck", amount), 0, 0,
                    (player, world) -> bestow(player, world, amount, true)).details(player -> "1250 · split among nearby allies or enemies");
        }
        menu.open(p);
    }
    private static void bestow(ServerPlayer p, ServerLevel sl, int amount, boolean area) {
        if (!allowed(p, area ? 2 : 7, false)) return;
        LivingEntity target = targetOrSelf(p, sl, 32 * material(p).range);
        if (target == p) { refuse(p, "Aim at another living entity. Transferring your reserve to yourself changes nothing."); return; }
        List<LivingEntity> targets = new ArrayList<>(); targets.add(target);
        if (area) Spells.livingAround(p, sl, target.position(), 8).stream().filter(t -> t != p && t != target
                && Spells.isFriend(p, t) == Spells.isFriend(p, target)).limit(15).forEach(targets::add);
        double share = amount / (double) targets.size();
        if (targets.stream().noneMatch(t -> Luck.transferable(p, t, share) != 0)) { refuse(p, "No matching reserve or target capacity for that transfer."); return; }
        if (!Spells.paySpirituality(p, 1250)) return;
        for (LivingEntity t : targets) { Luck.transfer(p, t, share); ring(sl, t.position(), .8, amount > 0 ? GOLD : RED, 20); }
        finish(p);
    }

    public static void calamity(Player p, Level level, ServerLevel sl, boolean borrowed) {
        if (!allowed(p, 6, borrowed)) return;
        if (!p.isShiftKeyDown() && active(p, "ledger")) { refuse(p, "Your calamity ledger is already open."); return; }
        if (!Spells.paySpirituality(p, 2000)) return;
        if (p.isShiftKeyDown()) {
            arm(p, "attraction", duration(p, 300), "Calamity Attraction");
            p.getPersistentData().putDouble(PREFIX + "attraction_radius", 10 * material(p).aoe);
            if (!Casts.isCopying(p)) Luck.add(p, -10);
        } else {
            arm(p, "ledger", duration(p, 120), "Calamity Ledger");
            SnakeOfMercuryEvents.openLedger(p, sequence(p) <= 4 ? 2 : 1.5f);
        }
        finish(p);
    }
    public static void misfortuneWheel(Player p, Level l, ServerLevel sl, boolean borrowed) { open(p, 5, borrowed, MISFORTUNE); }
    public static void chaosWheel(Player p, Level l, ServerLevel sl, boolean borrowed) { open(p, 3, borrowed, CHAOS); }
    public static void cycleWheel(Player p, Level l, ServerLevel sl, boolean borrowed) { open(p, 1, borrowed, CYCLE); }
    private static void open(Player p, int seq, boolean borrowed, AbilityWheel wheel) {
        if (!allowed(p, seq, borrowed)) return;
        if (p.isShiftKeyDown()) wheel.quickCast(p); else wheel.open(p);
    }

    private static AbilityWheel.Result misfortune(ServerPlayer p, ServerLevel sl) {
        LivingEntity t = Spells.aimEnemy(p, sl, 24 * material(p).range);
        if (t == null) return refuse(p, "Aim at an enemy.");
        Misfortune.add(p, t, Spells.isBoss(t) ? 2 : 4);
        SnakeOfMercuryEvents.mishap(t, false);
        Luck.transfer(p, t, -10);
        ring(sl, t.position(), 1, RED, 30);
        return AbilityWheel.Result.DONE;
    }
    private static void misfortuneFinished(ServerPlayer p, ServerLevel sl, AbilityWheel.Entry entry) {
        if (!entry.key().equals("gambit")) finish(p);
        else if (p.getPersistentData().getBooleanOr(PREFIX + "jackpot", false)) {
            p.getPersistentData().remove(PREFIX + "jackpot");
            MISFORTUNE.resetCooldown(p, "gambit");
        }
    }
    private static AbilityWheel.Result gambit(ServerPlayer p, ServerLevel sl) {
        if (Casts.isCopying(p)) return AbilityWheel.Result.REFUSED;
        float stake = p.getHealth() * .2f;
        p.setHealth(Math.max(Math.min(1, p.getHealth()), p.getHealth() - stake));
        int lucky = 0;
        for (int i = 0; i < 3; i++) {
            Fate.spin(p);
            WheelTier tier = Fate.peek(p);
            ring(sl, p.position().add(0, i * .5, 0), 1 + i, tier.color, 25);
            if (tier.ordinal() <= WheelTier.GOLD.ordinal()) continue;
            lucky++;
            if (lucky == 1) p.heal(stake * 2);
            if (lucky == 2) for (LivingEntity t : Spells.enemiesAround(p, sl, p.position(), 6)) Spells.strike(p, t, 30);
            if (lucky == 3) SnakeOfMercuryEvents.reduceCooldowns(p, 200, false);
        }
        if (lucky == 3) { SnakeOfMercuryEvents.reduceCooldowns(p, 0, true); Luck.add(p, 10); p.getPersistentData().putBoolean(PREFIX + "jackpot", true); }
        p.sendSystemMessage(Component.literal("Winner's Gambit: " + lucky + " lucky stops" + (lucky == 3 ? " · JACKPOT" : "")));
        return AbilityWheel.Result.DONE;
    }
    private static AbilityWheel.Result chain(ServerPlayer p, ServerLevel sl) {
        LivingEntity first = Spells.aimEnemy(p, sl, 32 * material(p).range);
        if (first == null) return refuse(p, "Aim at an enemy to start the chain.");
        float size = material(p).range;
        int links = sequence(p) <= 2 ? 8 : 5;
        Set<UUID> hit = new HashSet<>();
        jump(p, sl, first, hit, 0, links, size);
        return AbilityWheel.Result.DONE;
    }
    private static void jump(ServerPlayer p, ServerLevel sl, LivingEntity t, Set<UUID> hit, int index, int max, float size) {
        if (!Spells.casterStillHere(p, sl) || !t.isAlive() || t.level() != sl || !Spells.isEnemy(p, t)) return;
        hit.add(t.getUUID()); Vec3 at = t.position();
        Spells.strike(p, t, 20 + index * 5); Misfortune.add(p, t, 1);
        ring(sl, at, 1, RED, 16);
        if (index + 1 >= max) return;
        Later.run(sl, 8, () -> {
            LivingEntity next = Spells.enemiesAround(p, sl, at, 8 * size).stream().filter(e -> !hit.contains(e.getUUID())
                    && (sequence(p) <= 2 || Spells.clearLine(p, sl, at.add(0, 1, 0), e.getEyePosition())))
                    .min(Comparator.comparingDouble(e -> e.position().distanceToSqr(at))).orElse(null);
            if (next != null) jump(p, sl, next, hit, index + 1, max, size);
        });
    }
    private static AbilityWheel.Result serpent(ServerPlayer p, ServerLevel sl) {
        if (active(p, "serpent")) { p.getPersistentData().remove(PREFIX + "serpent"); return AbilityWheel.Result.FREE; }
        arm(p, "serpent", sequence(p) <= 1 ? Integer.MAX_VALUE : duration(p, 800), "Silver Serpent Form");
        p.getPersistentData().putLong(PREFIX + "serpent_fray_at", sl.getGameTime() + 200);
        return AbilityWheel.Result.DONE;
    }
    private static AbilityWheel.Result walk(ServerPlayer p, ServerLevel sl) {
        arm(p, "walk", duration(p, 120), "Chaos Walk");
        Concealment.hide(p, duration(p, 120));
        p.getPersistentData().putBoolean(PREFIX + "projectile", true);
        return AbilityWheel.Result.DONE;
    }
    private static AbilityWheel.Result aura(ServerPlayer p, ServerLevel sl) {
        arm(p, "aura", duration(p, 240), "Aura of Chaos");
        p.getPersistentData().putDouble(PREFIX + "aura_radius", 10 * material(p).aoe);
        return AbilityWheel.Result.DONE;
    }
    private static AbilityWheel.Result wordsMenu(ServerPlayer p, ServerLevel sl) {
        AbilityMenu.create("snake_words").title("Words of Fortune and Misfortune").ring(90)
                .option(ICON, "Bless +30 Luck", 0, 0, (who, world) -> words(who, world, true)).details(who -> "12000 · 20s")
                .option(ICON, "Curse -30 Luck", 0, 0, (who, world) -> words(who, world, false)).details(who -> "12000 · 20s").open(p);
        return AbilityWheel.Result.FREE;
    }
    private static void words(ServerPlayer p, ServerLevel sl, boolean blessing) {
        if (!allowed(p, 2, false) || !Cooldowns.ready(p, "snake_words", "Words of Fate")) return;
        LivingEntity t = targetOrSelf(p, sl, 32 * material(p).range);
        if (!Spells.paySpirituality(p, 12000)) return;
        Luck.bestow(p, t, blessing ? 30 : Spells.isBoss(t) ? -15 : -30);
        if (blessing) t.getPersistentData().putLong(PREFIX + "blessing", sl.getGameTime() + duration(p, 1200));
        else {
            // Explicit curse mode also works on allies. The ordinary hostile Misfortune API remains enemy-only.
            int levels = Spells.isBoss(t) ? 5 : 10;
            Misfortune.addDeliberate(t, p, material(p).fortuneLevels(levels, p.getRandom().nextFloat()), 200);
            SnakeOfMercuryEvents.curse(p, t, Spells.isBoss(t) ? duration(p, 100) : duration(p, 200));
        }
        Cooldowns.start(p, "snake_words", 400);
        finish(p);
    }
    private static AbilityWheel.Result conceal(ServerPlayer p, ServerLevel sl) {
        LivingEntity t = Spells.aimAlly(p, sl, 32 * material(p).range); if (t == null) t = p;
        if (Casts.isCopying(p) && Substitute.isArmed(t)) return AbilityWheel.Result.REFUSED;
        if (Decree.violate(t, Decree.Rule.STAND_INS)) return refuse(p, "Stand-ins are forbidden here.");
        int ticks = duration(p, 160);
        Concealment.hide(t, ticks);
        if (Concealment.isHidden(t)) Unnoticed.hide(t, ticks);
        Substitute.arm(t, ticks, 3, (saved, source, amount, world) -> ring(world, saved.position(), 1, SILVER, 10));
        return AbilityWheel.Result.DONE;
    }
    private static AbilityWheel.Result opportunity(ServerPlayer p, ServerLevel sl) {
        Vec3 at = Spells.aimPoint(p, sl, 32 * material(p).range);
        SnakeOfMercuryEvents.opportunity(p, at, 8 * material(p).aoe, duration(p, 400));
        ring(sl, at, 8 * material(p).aoe, GOLD, duration(p, 400));
        return AbilityWheel.Result.DONE;
    }
    private static AbilityWheel.Result authority(ServerPlayer p, ServerLevel sl) {
        AbilityMenu menu = AbilityMenu.create("snake_authority").title("Choose fate · obey signed reserve").ring(100);
        for (WheelTier tier : WheelTier.values()) menu.option("snake_of_mercury/omen_" + tier.name().toLowerCase(Locale.ROOT), tier.label, 0, 0,
                (who, world) -> {
                    if (!allowed(who, 0, false)) return;
                    double balance = Luck.balance(who);
                    if (!permitted(balance, tier)) return;
                    Fate.set(who, tier); Luck.spendRoll(who, tier.ordinal());
                    who.getPersistentData().putInt("snake_wheel_sequence", sequence(who));
                    who.getPersistentData().putLong("snake_of_mercury_next_spin", world.getGameTime() + 160);
                }).lockedWhen(who -> !permitted(Luck.balance(who), tier));
        menu.open(p); return AbilityWheel.Result.FREE;
    }
    private static boolean permitted(double balance, WheelTier tier) { return tier == WheelTier.GOLD || balance > 0 && tier.ordinal() > 3 || balance < 0 && tier.ordinal() < 3; }
}
