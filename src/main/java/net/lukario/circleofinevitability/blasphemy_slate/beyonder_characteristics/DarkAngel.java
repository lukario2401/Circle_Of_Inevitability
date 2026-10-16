package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonder;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.SoulCore;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.pathway_fx.HangedManFx;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.pathway_fx.HangedManFx.Status;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.hand_of_god.HandOfGod;
import net.lukario.circleofinevitability.combat.Later;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.menu.AbilityMenu;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.lukario.circleofinevitability.status.Concealment;
import net.lukario.circleofinevitability.status.DeathWard;
import net.lukario.circleofinevitability.status.EmpoweredHit;
import net.lukario.circleofinevitability.status.NoHeal;
import net.lukario.circleofinevitability.status.Taunt;
import net.lukario.circleofinevitability.status.Vulnerable;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Hanged Man: the Hanged Man pathway of Lord of the Mysteries. Seven keys in the Attendant Of Mysteries pattern,
 * from Secrets Suppliant (Sequence 9) to Trinity Templar (Sequence 3); every ability has a normal and a sneak cast:
 *
 *   1 Divination         (Secrets Suppliant)  the pendulum: everything within 24 blocks glows, the strongest is marked
 *                                             and takes more damage            | sneak: Ritual Circle, paid in blood
 *   2 Whispers           (Listener)           profane whispers on the target: hurt, weakened, slowed, a mob loses track
 *                                             of whom it hunts                 | sneak: Listen, warns you of what comes for you
 *   3 Shadow Binding     (Shadow Ascetic)     shadows rise where you aim, hold and hurt everything there
 *                                                                              | sneak: Shadow Lurking (press again to rise)
 *   4 Flesh Bomb         (Rose Bishop)        a lump of flesh that sticks, swells and bursts | sneak: Flesh Cloak
 *   5 Grazing            (Shepherd)           what you kill leaves you an ability, kept for good in 7 slots: the key casts
 *                                             the selected one, at full power, for 3x its cost
 *                                                                              | sneak: the wheel of your 7 slots
 *   6 Cull of Spiritual Flesh (Black Knight)  a wide black cut that strips their defense     | sneak: Black Armor
 *   7 Degeneration       (Trinity Templar)    corruption that eats their buffs, rots them, turns a mob on its own
 *                                                                              | sneak: Trinity Body, two more bodies
 *
 * Every look and sound is in HangedManFx (see /hangedfx); this class only decides who is hurt, held and helped,
 * and when (at the ticks HangedManFx names: BIND_GRIPS, DEGENERATION_ARRIVES...).
 * What is reusable lives outside this class (docs/SPELL_KIT.md): status/Vulnerable (the divination mark, the cull),
 * status/Concealment (lurking), status/EmpoweredHit (the ambush), status/DeathWard (the spare bodies),
 * status/Taunt (the betrayal), status/NoHeal, root/Root, combat/Spells, combat/Later.
 *
 * Nothing here breaks blocks or lights real fire. The numbers for every ability are right below.
 */
public class DarkAngel {

    private static final String PATHWAY = "Dark Angel";

    // =========================
    // Ability settings (cost = spirituality, ticks: 20 = 1 second, distances in blocks)
    // =========================

    // Ability 1 - Divination (Sequence 9)
    private static final int DIVINATION_SEQUENCE = 9;
    private static final int DIVINATION_COST = 250;
    private static final double DIVINATION_RADIUS = 24.0;        // the pulse finds every enemy this close
    private static final int REVEAL_TICKS = 200;                 // they glow (seen through walls) this long
    private static final int MARK_TICKS = 200;                   // the strongest of them is marked this long...
    private static final float MARK_EXTRA = 0.25f;                // ...and takes this much more damage meanwhile (0.2 = 20%)
    // sneak: Ritual Circle. It costs blood, not spirituality.
    private static final float RITUAL_PRICE = 4f;                // health it takes (2 hearts); refused when you have too little
    private static final double RITUAL_RADIUS = 8.0;
    private static final int RITUAL_TICKS = 200;                 // how long the circle stands
    private static final int RITUAL_PULSE = 40;                  // every 2 s, while you stand in it...
    private static final float RITUAL_SPIRITUALITY = 0.08f;      // ...this share of your maximum spirituality comes back...
    private static final int RITUAL_STRENGTH_LEVEL = 1;          // ...and you have Strength I and Resistance I
    private static final int RITUAL_RESISTANCE_LEVEL = 1;

    // Ability 2 - Whispers (Sequence 8)
    private static final int WHISPERS_SEQUENCE = 8;
    private static final int WHISPERS_COST = 300;                // Sequence 8 holds 400
    private static final double WHISPERS_RANGE = 24.0;
    private static final int WHISPERS_TICKS = 160;               // how long the mouths whisper to it
    private static final float WHISPERS_DAMAGE = 4f;             // each second
    private static final int WHISPERS_WEAKNESS_LEVEL = 0;        // Weakness I and Slowness I meanwhile
    private static final int WHISPERS_SLOW_LEVEL = 0;
    // sneak: Listen
    private static final int LISTEN_COST = 300;
    private static final int LISTEN_TICKS = 300;                 // 15 s of listening
    private static final double LISTEN_RADIUS = 16.0;
    private static final int LISTEN_PULSE = 40;                  // ticks between two listens
    private static final int LISTEN_GLOW_TICKS = 50;             // every enemy heard glows this long (a little longer than a pulse)

    // Ability 3 - Shadow Binding (Sequence 7)
    private static final int BINDING_SEQUENCE = 7;
    private static final int BINDING_COST = 700;                 // Sequence 7 holds 1980
    private static final double BINDING_RANGE = 20.0;
    private static final double BINDING_RADIUS = 2.5;
    private static final int BINDING_WARNING = 10;               // ticks between the warning circle and the shadows rising
    private static final int BINDING_TICKS = 70;                 // the shadows stay this long (the hold starts at HangedManFx.BIND_GRIPS)
    private static final float BINDING_DAMAGE = 14f;             // when they close
    // sneak: Shadow Lurking (the key again, or attacking, brings you out)
    private static final int LURK_COST = 700;
    private static final int LURK_TICKS = 160;
    private static final int LURK_SPEED_LEVEL = 1;               // 0 = Speed I, 1 = Speed II while sunk
    private static final int AMBUSH_WINDOW = 40;                 // your first hit from the shadow, or this long after rising...
    private static final float AMBUSH_BONUS = 25f;               // ...does this much more...
    private static final int AMBUSH_HOLD_TICKS = 20;             // ...and holds what it hit in place for a moment

    // Ability 4 - Flesh Bomb (Sequence 6)
    private static final int BOMB_SEQUENCE = 6;
    private static final int BOMB_COST = 1500;
    private static final double BOMB_RANGE = 24.0;
    private static final int BOMB_FUSE = 40;                     // ticks it swells on the one it stuck to
    private static final double BOMB_RADIUS = 4.0;
    private static final float BOMB_DAMAGE = 36f;                // in the middle (and on the one it stuck to)...
    private static final float BOMB_EDGE = 0.5f;                 // ...this part of it at the rim
    private static final int BOMB_ROT_TICKS = 80;                // the blood rain rots: Wither I this long
    // sneak: Flesh Cloak (cast again to take it off, which costs nothing)
    private static final int CLOAK_COST = 1500;
    private static final int CLOAK_TICKS = 300;
    private static final float CLOAK_GUARD = 0.4f;               // share of every blow the soft flesh swallows
    private static final float CLOAK_HEAL = 1f;                  // health every half second while worn

    // Ability 5 - Grazing (Sequence 5): the souls of what you kill, kept for good in GRAZE_SLOTS slots
    private static final int GRAZING_SEQUENCE = 5;
    private static final int GRAZE_SLOTS = 7;
    private static final float GRAZE_COST_MULTIPLIER = 3f;       // a stored ability costs 3x its own cost (no Sequence gate, no cooldown)
    private static final int SOUL_RISE_TICKS = 10;               // a slot is free: the soul rises off the corpse this long, then flies to you
    private static final int SOUL_WAIT_TICKS = 150;              // every slot is full: the soul waits on the corpse 30 s...
    private static final double SOUL_REACH = 4.0;                // ...for you to stand this close and take it (sneak + key)
    private static final int WHEEL_RADIUS = 76;                  // GUI pixels from the middle of the screen to the slots

    // Ability 6 - Cull of Spiritual Flesh (Sequence 4)
    private static final int CULL_SEQUENCE = 4;
    private static final int CULL_COST = 6000;                   // Sequence 4 holds 32400
    private static final double CULL_REACH = 4.5;                // how far in front of you the cut reaches...
    private static final double CULL_HALF_ANGLE = 75.0;          // ...and how wide (degrees to either side; the arc shows 150)
    private static final float CULL_DAMAGE = 50f;
    private static final float CULL_STRIP = 0.3f;                // their spiritual flesh is cut: 30% more damage taken...
    private static final int CULL_STRIP_TICKS = 160;             // ...and Weakness I, this long
    // sneak: Black Armor (cast again to take it off, which costs nothing)
    private static final int ARMOR_COST = 6000;
    private static final int ARMOR_TICKS = 400;
    private static final float ARMOR_GUARD = 0.65f;              // share of every blow the plates take
    private static final float ARMOR_GROWTH = 0.25f;             // you grow this much bigger in it (0.25 = a quarter)
    private static final double ARMOR_KNOCKBACK_RESISTANCE = 0.6;
    private static final int ARMOR_STRENGTH_LEVEL = 1;           // Strength I while worn

    // Ability 7 - Degeneration (Sequence 3)
    private static final int DEGENERATION_SEQUENCE = 3;
    private static final int DEGENERATION_COST = 12000;          // Sequence 3 holds 59780
    private static final double DEGENERATION_RANGE = 28.0;
    private static final float DEGENERATION_HIT = 20f;           // when it lands; it also eats every buff on the target
    private static final int DEGENERATION_TICKS = 160;           // then it rots this long: no healing, Wither I...
    private static final float DEGENERATION_DAMAGE = 6f;         // ...and this much each second
    private static final double BETRAY_REACH = 12.0;             // a mob turns on the nearest other mob this close to it
    private static final int DEGENERATION_PLAYER_WEAKNESS = 1;   // a player cannot betray: Weakness II instead
    // sneak: Trinity Body
    private static final int TRINITY_COST = 12000;
    private static final int TRINITY_TICKS = 400;
    private static final float TRINITY_BONUS = 0.65f;            // the other two strike with you: 35% more damage dealt
    private static final int TRINITY_BODIES = 2;                 // each lethal blow kills one of them instead of you...
    private static final float TRINITY_REVIVE_HEALTH = 0.5f;     // ...and you stand with this share of your health
    private static final int TRINITY_ECHO_EVERY = 10;            // ticks between two flashes of the copies striking (only the look)

    // =========================
    // Saved on the player (game times are "until" this tick)
    // =========================
    private static final String RITUAL_UNTIL = "dark_angel_ritual_until";
    private static final String LISTEN_UNTIL = "dark_angel_listen_until";
    private static final String LURK_UNTIL = "dark_angel_lurk_until";
    private static final String CLOAK_UNTIL = "dark_angel_cloak_until";
    private static final String ARMOR_UNTIL = "dark_angel_armor_until";
    private static final String TRINITY_UNTIL = "dark_angel_trinity_until";
    private static final String TRINITY_LEFT = "dark_angel_trinity_bodies";
    private static final String TRINITY_ECHO = "dark_angel_trinity_echo";        // when the copies last flashed

    // Grazing's slots: a compound with slot0..slot6 ("Pathway|key|sneak" or missing) and "selected".
    // Kept through death (copied over in onHangedManClone).
    private static final String GRAZING = "dark_angel_grazing";
    private static final String GRAZING_SELECTED = "selected";
    private static final String GRAZING_WHEEL = "dark_angel_grazing";            // the menu's id

    // Saved on whoever it is put on
    private static final String WHISPERED_UNTIL = "dark_angel_whispered_until";
    private static final String DEGENERATING_UNTIL = "dark_angel_degenerating_until";

    // Other names
    private static final String AMBUSH = "dark_angel_ambush";
    private static final Identifier ARMOR_SCALE = Identifier.fromNamespaceAndPath(CircleOfInevitability.MOD_ID, "dark_angel_black_armor_scale");
    private static final Identifier ARMOR_FOOTING = Identifier.fromNamespaceAndPath(CircleOfInevitability.MOD_ID, "dark_angel_black_armor_footing");

    /** Who each caster's whispers are on right now (a new target ends the old whispers). Not saved. */
    private static final Map<UUID, LivingEntity> WHISPERED = new HashMap<>();

    @Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class HangedManEvents {

        @SubscribeEvent
        public static void onHangedManTick(TickEvent.PlayerTickEvent.Post event) {
            Player player = event.player();
            if (!(player.level() instanceof ServerLevel sl)) return;
            // these run out even if the pathway is taken away in the meantime
            lurkTick(player, sl);
            if (player.tickCount % 10 == 0) waitingSoulsTick(player, sl);
            cloakTick(player, sl);
            armorTick(player, sl);
            trinityTick(player, sl);
        }

        /** Blows on someone in the cloak or the armor are softened; blows from someone with three bodies are harder. */
        @SubscribeEvent
        public static void onHangedManHurt(LivingHurtEvent event) {
            LivingEntity victim = event.getEntity();
            if (!(victim.level() instanceof ServerLevel sl) || event.getAmount() <= 0f) return;

            if (victim instanceof Player player) {
                if (isActive(player, CLOAK_UNTIL)) {
                    event.setAmount(event.getAmount() * (1f - CLOAK_GUARD));
                    HangedManFx.fleshCloakAbsorb(sl, player);
                }
                if (isActive(player, ARMOR_UNTIL)) {
                    event.setAmount(event.getAmount() * (1f - ARMOR_GUARD));
                    HangedManFx.blackArmorHit(sl, player);
                }
            }

            if (event.getSource().getEntity() instanceof Player attacker && attacker != victim
                    && isActive(attacker, TRINITY_UNTIL)) {
                event.setAmount(event.getAmount() * (1f + TRINITY_BONUS));
                CompoundTag data = attacker.getPersistentData();
                long now = sl.getGameTime();
                if (data.getLongOr(TRINITY_ECHO, 0L) + TRINITY_ECHO_EVERY <= now) {
                    data.putLong(TRINITY_ECHO, now);
                    HangedManFx.trinityEcho(sl, attacker);
                }
            }
        }

        // what was on the screen and the body does not survive relogging or dying: start clean
        @SubscribeEvent
        public static void onHangedManLogin(PlayerEvent.PlayerLoggedInEvent event) {
            clearState(event.getEntity());
        }

        @SubscribeEvent
        public static void onHangedManRespawn(PlayerEvent.PlayerRespawnEvent event) {
            clearState(event.getEntity());
        }

        @SubscribeEvent
        public static void onHangedManLogout(PlayerEvent.PlayerLoggedOutEvent event) {
            WHISPERED.remove(event.getEntity().getUUID());
            dropWaitingSouls(event.getEntity());
        }

        /** Grazing: what a Hanged Man of Sequence 5 or better kills leaves them an ability. */
        @SubscribeEvent
        public static void onHangedManKill(LivingDeathEvent event) {
            LivingEntity victim = event.getEntity();
            if (!(victim.level() instanceof ServerLevel sl)) return;
            if (!(event.getSource().getEntity() instanceof Player killer) || killer == victim) return;
            if (victim.getHealth() > 0f) return;                         // a death ward undid the death
            grazeKill(killer, victim, sl);
        }

        /** The slots are kept for good: they go with the player into their new body. */
        @SubscribeEvent
        public static void onHangedManClone(PlayerEvent.Clone event) {
            CompoundTag before = event.getOriginal().getPersistentData();
            if (before.contains(GRAZING)) {
                event.getEntity().getPersistentData().put(GRAZING, before.getCompoundOrEmpty(GRAZING).copy());
            }
        }

        // souls are not saved: a world that closes takes its waiting souls with it
        @SubscribeEvent
        public static void onHangedManLevelUnload(LevelEvent.Unload event) {
            if (!(event.getLevel() instanceof ServerLevel)) return;
            SOULS.removeIf(soul -> soul.level == event.getLevel());
        }
    }

    // =========================
    // Ability 1 - Divination
    // Normal: the pendulum swings and stops; a pulse goes out DIVINATION_RADIUS blocks. Every enemy in it glows
    //         for REVEAL_TICKS, and the strongest (the most max health) is marked: it takes MARK_EXTRA more damage
    //         for MARK_TICKS, and you are told who and where it is.
    // Sneak:  Ritual Circle. Pay RITUAL_PRICE of your own health; a circle stands at your feet for RITUAL_TICKS.
    //         While you stand in it you are strengthened, and every RITUAL_PULSE spirituality comes back.
    // =========================
    public static void hangedManDivination(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!ready(player, bypassClassCheck, DIVINATION_SEQUENCE)) return;

        if (player.isShiftKeyDown()) {
            ritualCircle(player, sl);
            return;
        }
        if (!Spells.paySpirituality(player, DIVINATION_COST)) return;
        riseFromShadow(player, sl);
        divination(player, sl);
    }

    private static void divination(Player player, ServerLevel sl) {
        HangedManFx.divination(sl, player);
        Later.run(sl, HangedManFx.DIVINATION_PULSE, () -> {
            if (!Spells.casterStillHere(player, sl)) return;
            List<LivingEntity> found = Spells.enemiesAround(player, sl, player.position(), DIVINATION_RADIUS);
            if (found.isEmpty()) {
                player.sendSystemMessage(Component.literal("The pendulum finds nothing."));
                return;
            }
            LivingEntity strongest = found.get(0);
            for (LivingEntity one : found) {
                HangedManFx.divinationReveal(sl, one, REVEAL_TICKS);
                if (one.getMaxHealth() > strongest.getMaxHealth()) strongest = one;
            }
            Vulnerable.add(strongest, MARK_EXTRA, MARK_EXTRA, MARK_TICKS);
            HangedManFx.divinationMark(sl, strongest, MARK_TICKS);
            HangedManFx.status(player, Status.DIVINATION, MARK_TICKS);
            int distance = (int) Math.round(player.distanceTo(strongest));
            player.sendSystemMessage(Component.literal("The pendulum stops on " + strongest.getDisplayName().getString()
                    + ", " + distance + " blocks away (" + Math.round(strongest.getHealth()) + " of "
                    + Math.round(strongest.getMaxHealth()) + " health). " + found.size() + " found."));
        });
    }

    private static void ritualCircle(Player player, ServerLevel sl) {
        if (isActive(player, RITUAL_UNTIL)) {
            player.sendSystemMessage(Component.literal("Your circle still stands."));
            return;
        }
        if (player.getHealth() <= RITUAL_PRICE + 1f) {
            player.sendSystemMessage(Component.literal("You have too little blood left to pay for a ritual."));
            return;
        }
        riseFromShadow(player, sl);
        // the price is paid straight from the body: not a blow, so nothing can block or soften it
        player.setHealth(player.getHealth() - RITUAL_PRICE);
        HangedManFx.ritualPrice(sl, player);

        Vec3 ground = Spells.groundAt(player, sl, player.position());
        long until = sl.getGameTime() + RITUAL_TICKS;
        player.getPersistentData().putLong(RITUAL_UNTIL, until);
        HangedManFx.ritualCircle(sl, ground, RITUAL_RADIUS, RITUAL_TICKS);
        HangedManFx.status(player, Status.RITUAL, RITUAL_TICKS);
        Later.run(sl, RITUAL_PULSE, () -> ritualPulse(player, sl, ground, until));
    }

    private static void ritualPulse(Player player, ServerLevel sl, Vec3 ground, long until) {
        if (!Spells.casterStillHere(player, sl)) return;
        if (player.getPersistentData().getLongOr(RITUAL_UNTIL, 0L) != until || sl.getGameTime() > until) return;
        HangedManFx.ritualPulse(sl, ground, RITUAL_RADIUS);
        if (inCircle(player, ground, RITUAL_RADIUS)) {
            float max = Beyonder.maxSpirituality(player);
            SoulCore.setSpirituality(player, Math.min(max, SoulCore.getSpirituality(player) + max * RITUAL_SPIRITUALITY));
            Spells.keepEffect(player, MobEffects.STRENGTH, RITUAL_STRENGTH_LEVEL, RITUAL_PULSE + 20);
            Spells.keepEffect(player, MobEffects.RESISTANCE, RITUAL_RESISTANCE_LEVEL, RITUAL_PULSE + 20);
        }
        if (sl.getGameTime() + RITUAL_PULSE <= until) Later.run(sl, RITUAL_PULSE, () -> ritualPulse(player, sl, ground, until));
    }

    private static boolean inCircle(Entity entity, Vec3 ground, double radius) {
        double dx = entity.getX() - ground.x, dz = entity.getZ() - ground.z;
        return dx * dx + dz * dz <= radius * radius && Math.abs(entity.getY() - ground.y) <= 2.0;
    }

    // =========================
    // Ability 2 - Whispers
    // Normal: whispering mouths circle the head of the enemy you look at for WHISPERS_TICKS: it is hurt every
    //         second, weakened and slowed, and a mob forgets whom it was hunting each time. One target at a time.
    // Sneak:  Listen. For LISTEN_TICKS, every LISTEN_PULSE: every enemy within LISTEN_RADIUS glows, and you alone
    //         hear the bell for each mob that has picked you as its prey (a red line runs from it to you).
    // =========================
    public static void hangedManWhispers(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!ready(player, bypassClassCheck, WHISPERS_SEQUENCE)) return;

        if (player.isShiftKeyDown()) {
            if (!Spells.paySpirituality(player, LISTEN_COST)) return;
            riseFromShadow(player, sl);
            listen(player, sl);
            return;
        }
        LivingEntity target = Spells.aimEnemy(player, sl, WHISPERS_RANGE);
        if (target == null) {
            player.sendSystemMessage(Component.literal("No one stands before you to whisper to."));
            return; // nothing spent
        }
        if (!Spells.paySpirituality(player, WHISPERS_COST)) return;
        riseFromShadow(player, sl);
        whispers(player, sl, target);
    }

    private static void whispers(Player player, ServerLevel sl, LivingEntity target) {
        LivingEntity before = WHISPERED.put(player.getUUID(), target);
        if (before != null && before != target && before.isAlive()) {
            before.getPersistentData().remove(WHISPERED_UNTIL);
            HangedManFx.whispersEnd(sl, before);
        }
        long until = sl.getGameTime() + WHISPERS_TICKS;
        target.getPersistentData().putLong(WHISPERED_UNTIL, until);
        HangedManFx.whispers(sl, player, target, WHISPERS_TICKS);
        HangedManFx.status(player, Status.WHISPERS, WHISPERS_TICKS);
        if (target instanceof Mob mob) mob.setTarget(null);
        whisperTick(player, sl, target, until);
    }

    /** Once a second while the whispers last (until a new whisper replaces them). */
    private static void whisperTick(Player player, ServerLevel sl, LivingEntity target, long until) {
        Later.run(sl, 20, () -> {
            if (!Spells.casterStillHere(player, sl)) return;
            if (!target.isAlive() || target.level() != sl) {
                WHISPERED.remove(player.getUUID(), target);
                if (target.getPersistentData().getLongOr(WHISPERED_UNTIL, 0L) == until) HangedManFx.statusEnd(player, Status.WHISPERS);
                return;
            }
            if (target.getPersistentData().getLongOr(WHISPERED_UNTIL, 0L) != until) return;   // whispered again since
            if (sl.getGameTime() > until) {
                target.getPersistentData().remove(WHISPERED_UNTIL);
                WHISPERED.remove(player.getUUID(), target);
                return;
            }
            Spells.strike(player, target, WHISPERS_DAMAGE);
            Spells.keepEffect(target, MobEffects.WEAKNESS, WHISPERS_WEAKNESS_LEVEL, 30);
            Spells.keepEffect(target, MobEffects.SLOWNESS, WHISPERS_SLOW_LEVEL, 30);
            if (target instanceof Mob mob && mob.getTarget() == player) mob.setTarget(null);   // it no longer knows who you are
            whisperTick(player, sl, target, until);
        });
    }

    private static void listen(Player player, ServerLevel sl) {
        long until = sl.getGameTime() + LISTEN_TICKS;
        player.getPersistentData().putLong(LISTEN_UNTIL, until);
        HangedManFx.status(player, Status.LISTEN, LISTEN_TICKS);
        listenPulse(player, sl, until);
    }

    private static void listenPulse(Player player, ServerLevel sl, long until) {
        if (!Spells.casterStillHere(player, sl)) return;
        if (player.getPersistentData().getLongOr(LISTEN_UNTIL, 0L) != until || sl.getGameTime() > until) return;
        HangedManFx.listenPulse(sl, player, LISTEN_RADIUS);
        for (LivingEntity heard : Spells.enemiesAround(player, sl, player.position(), LISTEN_RADIUS)) {
            heard.addEffect(new MobEffectInstance(MobEffects.GLOWING, LISTEN_GLOW_TICKS, 0, false, false));
            if (heard instanceof Mob mob && mob.getTarget() == player) HangedManFx.listenWarning(sl, mob, player);
        }
        if (sl.getGameTime() + LISTEN_PULSE <= until) Later.run(sl, LISTEN_PULSE, () -> listenPulse(player, sl, until));
    }

    // =========================
    // Ability 3 - Shadow Binding
    // Normal: a warning circle where you aim; BINDING_WARNING later the shadows of everything in it rise and close
    //         on them (at HangedManFx.BIND_GRIPS): BINDING_DAMAGE, and held in place until the shadows sink.
    // Sneak:  Shadow Lurking. You sink into your shadow for LURK_TICKS: unseen, untouchable, fast. The key again,
    //         casting anything else or attacking brings you out. Your first hit from the shadow (or within
    //         AMBUSH_WINDOW of rising) does AMBUSH_BONUS more and holds what it hit.
    // =========================
    public static void hangedManShadowBinding(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!ready(player, bypassClassCheck, BINDING_SEQUENCE)) return;

        // sunk in the shadow: the key is the way out, and costs nothing
        if (isActive(player, LURK_UNTIL)) {
            endLurk(player, sl);
            return;
        }
        if (player.isShiftKeyDown()) {
            if (!Spells.paySpirituality(player, LURK_COST)) return;
            lurk(player, sl);
            return;
        }
        if (!Spells.paySpirituality(player, BINDING_COST)) return;
        shadowBinding(player, sl);
    }

    private static void shadowBinding(Player player, ServerLevel sl) {
        Vec3 ground = Spells.aimGround(player, sl, BINDING_RANGE);
        HangedManFx.bindingWarning(sl, ground, BINDING_RADIUS);
        Later.run(sl, BINDING_WARNING, () -> {
            if (!Spells.casterStillHere(player, sl)) return;
            for (LivingEntity enemy : Spells.enemiesAround(player, sl, ground.add(0, 0.5, 0), BINDING_RADIUS)) {
                HangedManFx.bind(sl, enemy, BINDING_TICKS);
                Later.run(sl, HangedManFx.BIND_GRIPS, () -> {
                    if (!enemy.isAlive() || enemy.level() != sl || !Spells.casterStillHere(player, sl)) return;
                    Spells.strike(player, enemy, BINDING_DAMAGE);
                    if (enemy.isAlive()) Root.apply(enemy, BINDING_TICKS - HangedManFx.BIND_GRIPS - 10, RootRestriction.MOVEMENT);
                });
            }
        });
    }

    private static void lurk(Player player, ServerLevel sl) {
        player.getPersistentData().putLong(LURK_UNTIL, sl.getGameTime() + LURK_TICKS);
        HangedManFx.lurkEnter(sl, player, LURK_TICKS);
        HangedManFx.status(player, Status.LURKING, LURK_TICKS);
        Concealment.hide(player, LURK_TICKS + 5);                 // a little longer: lurkTick ends it
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, LURK_TICKS, LURK_SPEED_LEVEL, false, false));
        // a hit straight out of the shadow (it brings you out too) counts as the ambush
        EmpoweredHit.arm(player, AMBUSH, LURK_TICKS + AMBUSH_WINDOW, DarkAngel::ambush);
    }

    private static float ambush(Player attacker, LivingEntity target, float amount, ServerLevel sl) {
        Root.apply(target, AMBUSH_HOLD_TICKS, RootRestriction.MOVEMENT);
        HangedManFx.bind(sl, target, HangedManFx.BIND_GRIPS + 10);
        return amount + AMBUSH_BONUS;
    }

    public static boolean isLurking(Player player) {
        return isActive(player, LURK_UNTIL);
    }

    /** Every tick: lets the caster out when the time is up, or when something already pulled them out (an attack). */
    private static void lurkTick(Player player, ServerLevel sl) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(LURK_UNTIL)) return;
        if (!isActive(player, LURK_UNTIL) || !Concealment.isHidden(player)) {
            endLurk(player, sl);
            return;
        }
        if (player.tickCount % 10 == 0) HangedManFx.lurkTick(sl, player);
    }

    /** Out of the shadow, where you are. The ambush stays ready a moment longer. */
    private static void endLurk(Player player, ServerLevel sl) {
        player.getPersistentData().remove(LURK_UNTIL);
        if (Concealment.isHidden(player)) Concealment.reveal(player);
        MobEffectInstance speed = player.getEffect(MobEffects.SPEED);   // only the speed the shadow gave
        if (speed != null && speed.getAmplifier() == LURK_SPEED_LEVEL && speed.getDuration() <= LURK_TICKS) {
            player.removeEffect(MobEffects.SPEED);
        }
        HangedManFx.lurkExit(sl, player);
        HangedManFx.statusEnd(player, Status.LURKING);
        if (EmpoweredHit.isArmed(player, AMBUSH)) EmpoweredHit.arm(player, AMBUSH, AMBUSH_WINDOW, DarkAngel::ambush);
    }

    /** Casting anything else brings you out of the shadow first. */
    private static void riseFromShadow(Player player, ServerLevel sl) {
        if (isActive(player, LURK_UNTIL)) endLurk(player, sl);
    }

    // =========================
    // Ability 4 - Flesh Bomb
    // Normal: a lump of flesh thrown where you aim. On an enemy it sticks and swells for BOMB_FUSE, then bursts;
    //         on a block (or at the end of its flight) it bursts at once. The burst hurts everything within
    //         BOMB_RADIUS (less at the rim, all of it on the one it stuck to) and the blood rain rots them.
    // Sneak:  Flesh Cloak. For CLOAK_TICKS your body is soft flesh: it swallows CLOAK_GUARD of every blow and
    //         mends you every half second. Cast again to take it off.
    // =========================
    public static void hangedManFleshBomb(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!ready(player, bypassClassCheck, BOMB_SEQUENCE)) return;

        if (player.isShiftKeyDown()) {
            fleshCloak(player, sl);
            return;
        }
        if (!Spells.paySpirituality(player, BOMB_COST)) return;
        riseFromShadow(player, sl);
        fleshBomb(player, sl);
    }

    private static void fleshBomb(Player player, ServerLevel sl) {
        Vec3 hand = HangedManFx.fleshBombHand(player, sl);
        Vec3 direction = Spells.aimFrom(player, sl, hand, BOMB_RANGE);
        boolean[] stuck = {false};
        HangedManFx.FLESH_BOMB.range(BOMB_RANGE)
                .onHit((target, at) -> {
                    stuck[0] = true;
                    stickBomb(player, sl, target);
                })
                .onEnd((at, hitBlock) -> Later.run(sl, 1, () -> {      // (after onHit, whichever is called first)
                    if (!stuck[0] && Spells.casterStillHere(player, sl)) burstBomb(player, sl, at, null);
                }))
                .fire(player, sl, hand, direction);
    }

    private static void stickBomb(Player player, ServerLevel sl, LivingEntity target) {
        HangedManFx.fleshBombStick(sl, target, BOMB_FUSE);
        Later.run(sl, BOMB_FUSE, () -> {
            if (!Spells.casterStillHere(player, sl) || target.level() != sl) return;
            burstBomb(player, sl, target.getBoundingBox().getCenter(), target.isAlive() ? target : null);
        });
    }

    private static void burstBomb(Player player, ServerLevel sl, Vec3 center, LivingEntity stuckOn) {
        HangedManFx.fleshBombBurst(sl, center, BOMB_RADIUS, stuckOn);
        for (LivingEntity enemy : Spells.enemiesAround(player, sl, center, BOMB_RADIUS)) {
            double near = 1.0 - Math.min(1.0, Spells.distanceToBody(enemy, center) / BOMB_RADIUS);
            float damage = enemy == stuckOn ? BOMB_DAMAGE : BOMB_DAMAGE * (BOMB_EDGE + (1f - BOMB_EDGE) * (float) near);
            Spells.strike(player, enemy, damage);
            if (enemy.isAlive()) enemy.addEffect(new MobEffectInstance(MobEffects.WITHER, BOMB_ROT_TICKS, 0));
        }
    }

    private static void fleshCloak(Player player, ServerLevel sl) {
        if (isActive(player, CLOAK_UNTIL)) {                       // cast again: it comes off
            endCloak(player, sl);
            return;
        }
        if (!Spells.paySpirituality(player, CLOAK_COST)) return;
        riseFromShadow(player, sl);
        player.getPersistentData().putLong(CLOAK_UNTIL, sl.getGameTime() + CLOAK_TICKS);
        HangedManFx.fleshCloak(sl, player, CLOAK_TICKS);
        HangedManFx.status(player, Status.FLESH_CLOAK, CLOAK_TICKS);
    }

    private static void cloakTick(Player player, ServerLevel sl) {
        if (!player.getPersistentData().contains(CLOAK_UNTIL)) return;
        if (!isActive(player, CLOAK_UNTIL)) {
            endCloak(player, sl);
            return;
        }
        if (player.tickCount % 10 == 0) {
            if (player.getHealth() < player.getMaxHealth()) player.heal(CLOAK_HEAL);
            HangedManFx.fleshCloakRegen(sl, player);
        }
    }

    private static void endCloak(Player player, ServerLevel sl) {
        player.getPersistentData().remove(CLOAK_UNTIL);
        HangedManFx.fleshCloakEnd(sl, player);
        HangedManFx.statusEnd(player, Status.FLESH_CLOAK);
    }

    // =========================
    // Ability 5 - Grazing
    // Kills:  a hostile mob you kill gives you a random ability of another Pathway; a player you kill, one of
    //         the abilities their Sequence gives them. Its normal or its sneak form, at random. It flies into your
    //         first empty slot. With all GRAZE_SLOTS full its soul waits on the corpse for SOUL_WAIT_TICKS.
    // Sneak:  the wheel of your slots: click one to select it. Standing by a soul of yours that waits on its
    //         corpse, the wheel asks which slot it takes the place of instead.
    // Normal: casts the selected slot, in the form it was taken in, whatever its Sequence, for
    //         GRAZE_COST_MULTIPLIER times its own cost (Beyonder.castUnbound). No cooldown of its own.
    // The slots are saved on the player and kept for good (through death too).
    // =========================
    public static void hangedManGrazing(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!ready(player, bypassClassCheck, GRAZING_SEQUENCE)) return;

        if (player.isShiftKeyDown()) {
            WaitingSoul soul = soulNear(player, sl);
            if (soul != null) openReplaceWheel(player, soul);
            else openSlotWheel(player);
            return;
        }
        castSelected(player, level, sl);
    }

    /** One stored ability: what it is and which of its forms comes out. */
    private record Grazed(PathwayAbilities.Ability ability, boolean sneakForm) {

        /** "Dash" or "Dash (sneak)". */
        String label() {
            return sneakForm ? ability.name() + " (sneak)" : ability.name();
        }

        String save() {
            return ability.pathway() + "|" + ability.key() + "|" + (sneakForm ? 1 : 0);
        }

        /** Null for an empty slot, or one whose ability no longer exists. */
        static Grazed load(String saved) {
            String[] parts = saved.split("\\|");
            if (parts.length != 3) return null;
            try {
                PathwayAbilities.Ability ability = PathwayAbilities.of(parts[0], Integer.parseInt(parts[1]));
                return ability == null ? null : new Grazed(ability, ability.sneakForm() && parts[2].equals("1"));
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }

    /** A soul that waits on its corpse for its killer, whose slots were all full. Not saved. */
    private static final class WaitingSoul {
        final UUID owner;
        final ServerLevel level;
        final Vec3 corpse;
        final Grazed grazed;
        final int color;
        final String tag;                                        // its look (HangedManFx.graveSoul)
        final long expires;

        WaitingSoul(UUID owner, ServerLevel level, Vec3 corpse, Grazed grazed, int color, String tag, long expires) {
            this.owner = owner;
            this.level = level;
            this.corpse = corpse;
            this.grazed = grazed;
            this.color = color;
            this.tag = tag;
            this.expires = expires;
        }
    }

    private static final List<WaitingSoul> SOULS = new ArrayList<>();

    // ---------- the slots

    private static CompoundTag slots(Player player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(GRAZING)) data.put(GRAZING, new CompoundTag());
        return data.getCompoundOrEmpty(GRAZING);
    }

    private static Grazed slot(Player player, int index) {
        return Grazed.load(slots(player).getStringOr("slot" + index, ""));
    }

    private static void setSlot(Player player, int index, Grazed grazed) {
        slots(player).putString("slot" + index, grazed.save());
    }

    private static int selected(Player player) {
        return Math.floorMod(slots(player).getIntOr(GRAZING_SELECTED, 0), GRAZE_SLOTS);
    }

    private static void select(Player player, int index) {
        slots(player).putInt(GRAZING_SELECTED, index);
    }

    /** The first empty slot, or -1 when all are full. */
    private static int firstEmpty(Player player) {
        for (int i = 0; i < GRAZE_SLOTS; i++) {
            if (slot(player, i) == null) return i;
        }
        return -1;
    }

    // ---------- the kill

    private static void grazeKill(Player killer, LivingEntity victim, ServerLevel sl) {
        if (!SoulCore.getPathway(killer).equals(PATHWAY) || !Beyonder.hasSequence(killer, GRAZING_SEQUENCE)) return;
        if (!(victim instanceof Player) && !(victim instanceof Enemy)) return;     // animals have nothing to give

        PathwayAbilities.Ability ability = pickSoul(killer, victim);
        if (ability == null) {
            if (victim instanceof Player) killer.sendSystemMessage(Component.literal("Their soul holds nothing you can graze."));
            return;
        }
        Grazed grazed = new Grazed(ability, ability.sneakForm() && killer.getRandom().nextBoolean());
        int color = victim instanceof Player other ? pathwayColor(SoulCore.getPathway(other)) : HangedManFx.SOUL;
        Vec3 corpse = victim.position();

        int empty = firstEmpty(killer);
        if (empty >= 0) {
            setSlot(killer, empty, grazed);
            if (slot(killer, selected(killer)) == null || selected(killer) == empty) select(killer, empty);
            String soul = HangedManFx.graveSoul(sl, corpse, color, SOUL_RISE_TICKS + 20);
            Later.run(sl, SOUL_RISE_TICKS, () -> {
                if (Spells.casterStillHere(killer, sl)) HangedManFx.soulStolen(sl, corpse, soul, killer, color);
                else HangedManFx.graveSoulFades(sl, corpse, soul);
            });
            killer.sendSystemMessage(Component.literal("Grazed " + grazed.label() + " (" + ability.pathway()
                    + ") into slot " + (empty + 1) + "."));
            return;
        }

        String tag = HangedManFx.graveSoul(sl, corpse, color, SOUL_WAIT_TICKS);
        SOULS.add(new WaitingSoul(killer.getUUID(), sl, corpse, grazed, color, tag, sl.getGameTime() + SOUL_WAIT_TICKS));
        killer.sendSystemMessage(Component.literal("Your " + GRAZE_SLOTS + " slots are full. The soul of "
                + grazed.label() + " (" + ability.pathway() + ") waits on its corpse for " + SOUL_WAIT_TICKS / 20
                + " s: stand by it and sneak + press the key to take it in place of one."));
    }

    /** From a player with a Pathway: one of the abilities their Sequence gives them. Otherwise: any of another Pathway. */
    private static PathwayAbilities.Ability pickSoul(Player killer, LivingEntity victim) {
        List<PathwayAbilities.Ability> choices = new ArrayList<>();
        if (victim instanceof Player other && !PathwayAbilities.of(SoulCore.getPathway(other)).isEmpty()) {
            for (PathwayAbilities.Ability ability : PathwayAbilities.of(SoulCore.getPathway(other))) {
                if (ability.stealable() && Beyonder.hasSequence(other, ability.sequence())) choices.add(ability);
            }
        } else {
            for (PathwayAbilities.Ability ability : PathwayAbilities.ALL) {
                if (ability.stealable() && !ability.pathway().equals(PATHWAY)) choices.add(ability);
            }
        }
        return choices.isEmpty() ? null : choices.get(killer.getRandom().nextInt(choices.size()));
    }

    /** The color of a player's soul: the color of their Pathway. */
    private static int pathwayColor(String pathway) {
        return switch (pathway) {
            case "Key Of Stars" -> 0xB0A0B4FF;
            case "Worm Of Time" -> 0xB052E3C4;
            case "Hand Of Order" -> 0xB0FFD27A;
            case "Attendant Of Mysteries" -> 0xB09CFFE6;
            case "Prince Of Abolition" -> 0xB0FF7A3A;
            case "Pale Emperor" -> 0xB0E8E4D0;
            case "Conqueror" -> 0xB0FF4A1A;
            case "Apocalypse" -> 0xB0B050FF;
            case "Beauty Goddess" -> 0xB0DCE8FF;
            case HandOfGod.PATHWAY -> 0xB0FF7A1A;
            case PATHWAY -> 0xB0D0283C;
            default -> HangedManFx.SOUL;
        };
    }

    // ---------- the souls that wait on their corpses

    /** Twice a second for the owner: their waiting souls drift, and fade when their time is up. */
    private static void waitingSoulsTick(Player player, ServerLevel sl) {
        if (SOULS.isEmpty()) return;
        long now = sl.getGameTime();
        for (WaitingSoul soul : new ArrayList<>(SOULS)) {
            if (!soul.owner.equals(player.getUUID()) || soul.level != sl) continue;
            if (soul.expires <= now) {
                SOULS.remove(soul);
                HangedManFx.graveSoulFades(sl, soul.corpse, soul.tag);
            } else {
                HangedManFx.graveSoulTick(sl, soul.corpse);
            }
        }
    }

    /** The nearest of the player's waiting souls within SOUL_REACH, or null. */
    private static WaitingSoul soulNear(Player player, ServerLevel sl) {
        WaitingSoul best = null;
        double bestDistance = SOUL_REACH * SOUL_REACH;
        long now = sl.getGameTime();
        for (WaitingSoul soul : SOULS) {
            if (!soul.owner.equals(player.getUUID()) || soul.level != sl || soul.expires <= now) continue;
            double d = player.position().distanceToSqr(soul.corpse);
            if (d <= bestDistance) {
                bestDistance = d;
                best = soul;
            }
        }
        return best;
    }

    /** The player left: their waiting souls fade. */
    private static void dropWaitingSouls(Player player) {
        for (WaitingSoul soul : new ArrayList<>(SOULS)) {
            if (!soul.owner.equals(player.getUUID())) continue;
            SOULS.remove(soul);
            HangedManFx.graveSoulFades(soul.level, soul.corpse, soul.tag);
        }
    }

    // ---------- the wheels (a new menu every time: what is in the slots is this player's own)

    /** Sneak: pick the slot the key casts. Empty slots are greyed out. */
    private static void openSlotWheel(Player player) {
        AbilityMenu wheel = AbilityMenu.create(GRAZING_WHEEL).title("Grazing").ring(WHEEL_RADIUS);
        int chosen = selected(player);
        for (int i = 0; i < GRAZE_SLOTS; i++) {
            int index = i;
            Grazed grazed = slot(player, i);
            wheel.option(iconOf(grazed), grazed == null ? "Empty" : grazed.label(), 0, 0, (picker, level) -> {
                        Grazed now = slot(picker, index);
                        if (now == null) return;
                        select(picker, index);
                        picker.sendSystemMessage(Component.literal("Grazing: " + now.label() + " is ready."));
                    })
                    .details(viewer -> grazed == null ? ""
                            : (index == chosen ? "selected · " : "") + grazed.ability().pathway())
                    .lockedWhen(viewer -> grazed == null);
        }
        wheel.open(player);
    }

    /** Sneak by a waiting soul: pick the slot it takes the place of (an empty one, if any, just takes it). */
    private static void openReplaceWheel(Player player, WaitingSoul soul) {
        AbilityMenu wheel = AbilityMenu.create(GRAZING_WHEEL).title("Graze " + soul.grazed.label() + " in place of...")
                .ring(WHEEL_RADIUS);
        for (int i = 0; i < GRAZE_SLOTS; i++) {
            int index = i;
            Grazed grazed = slot(player, i);
            wheel.option(grazed == null ? HangedManFx.WHEEL_EMPTY : HangedManFx.WHEEL_REPLACE,
                            grazed == null ? "Empty" : grazed.label(), 0, 0,
                            (picker, level) -> takeSoul(picker, level, soul, index))
                    .details(viewer -> grazed == null ? "put it here" : grazed.ability().pathway());
        }
        wheel.open(player);
    }

    private static String iconOf(Grazed grazed) {
        if (grazed == null) return HangedManFx.WHEEL_EMPTY;
        return grazed.sneakForm() ? HangedManFx.WHEEL_SNEAK_BADGE : HangedManFx.WHEEL_SOUL;
    }

    /** The waiting soul goes into that slot (what was there is gone) and is selected. */
    private static void takeSoul(ServerPlayer player, ServerLevel sl, WaitingSoul soul, int index) {
        if (!SOULS.contains(soul) || soul.level != sl
                || player.position().distanceToSqr(soul.corpse) > (SOUL_REACH + 2) * (SOUL_REACH + 2)) {
            player.sendSystemMessage(Component.literal("That soul is gone."));
            return;
        }
        SOULS.remove(soul);
        Grazed before = slot(player, index);
        setSlot(player, index, soul.grazed);
        select(player, index);
        HangedManFx.soulStolen(sl, soul.corpse, soul.tag, player, soul.color);
        player.sendSystemMessage(Component.literal("Grazed " + soul.grazed.label() + " into slot " + (index + 1)
                + (before == null ? "." : ", in place of " + before.label() + ".")));
    }

    // ---------- the cast

    /** The selected slot goes off in its stored form, at full power, for GRAZE_COST_MULTIPLIER times its cost. */
    private static void castSelected(Player player, Level level, ServerLevel sl) {
        int index = selected(player);
        Grazed grazed = slot(player, index);
        if (grazed == null) {
            HangedManFx.emptySlot(sl, player);
            player.sendSystemMessage(Component.literal("Slot " + (index + 1) + " holds no soul. Kill something to"
                    + " graze one, or sneak + press the key to pick another slot."));
            return;
        }
        riseFromShadow(player, sl);
        boolean wasSneaking = player.isShiftKeyDown();
        player.setShiftKeyDown(grazed.sneakForm());                 // the form it was taken in, not how you stand
        boolean wentOff;
        try {
            wentOff = Beyonder.castUnbound(player, GRAZE_COST_MULTIPLIER, () -> grazed.ability().cast().run(player, level, sl));
        } catch (RuntimeException e) {
            CircleOfInevitability.LOGGER.error("A grazed ability failed: {}", grazed.label(), e);
            wentOff = false;
        } finally {
            player.setShiftKeyDown(wasSneaking);
        }
        if (wentOff) HangedManFx.stolenCast(sl, player);
    }

    // =========================
    // Ability 6 - Cull of Spiritual Flesh
    // Normal: a wide black cut in front of you: CULL_DAMAGE to everything within CULL_REACH and CULL_HALF_ANGLE of
    //         where you face, and its spiritual flesh is cut for CULL_STRIP_TICKS: CULL_STRIP more damage taken, weakened.
    // Sneak:  Black Armor. For ARMOR_TICKS black plates lock onto you: they take ARMOR_GUARD of every blow, you
    //         grow ARMOR_GROWTH bigger, are hard to knock back and strengthened. Cast again to take it off.
    // =========================
    public static void hangedManCull(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!ready(player, bypassClassCheck, CULL_SEQUENCE)) return;

        if (player.isShiftKeyDown()) {
            blackArmor(player, sl);
            return;
        }
        if (!Spells.paySpirituality(player, CULL_COST)) return;
        riseFromShadow(player, sl);
        cull(player, sl);
    }

    private static void cull(Player player, ServerLevel sl) {
        HangedManFx.cull(sl, player);
        Later.run(sl, 2, () -> {                                     // when the arc has swept through
            if (!Spells.casterStillHere(player, sl)) return;
            Vec3 from = player.position().add(0, player.getBbHeight() * 0.6, 0);
            for (LivingEntity enemy : Spells.enemiesInCone(player, sl, from, Spells.flatLook(player), CULL_REACH, CULL_HALF_ANGLE)) {
                Spells.strike(player, enemy, CULL_DAMAGE);
                if (!enemy.isAlive()) continue;
                Vulnerable.add(enemy, CULL_STRIP, CULL_STRIP, CULL_STRIP_TICKS);
                enemy.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, CULL_STRIP_TICKS, 0));
                HangedManFx.cullHit(sl, enemy, CULL_STRIP_TICKS);
            }
        });
    }

    private static void blackArmor(Player player, ServerLevel sl) {
        if (isActive(player, ARMOR_UNTIL)) {                       // cast again: it comes off
            endArmor(player, sl, true);
            return;
        }
        if (!Spells.paySpirituality(player, ARMOR_COST)) return;
        riseFromShadow(player, sl);
        player.getPersistentData().putLong(ARMOR_UNTIL, sl.getGameTime() + ARMOR_TICKS);
        setModifier(player, Attributes.SCALE, ARMOR_SCALE, ARMOR_GROWTH, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        setModifier(player, Attributes.KNOCKBACK_RESISTANCE, ARMOR_FOOTING, ARMOR_KNOCKBACK_RESISTANCE, AttributeModifier.Operation.ADD_VALUE);
        HangedManFx.blackArmor(sl, player, ARMOR_TICKS, 1f + ARMOR_GROWTH);
        HangedManFx.status(player, Status.BLACK_ARMOR, ARMOR_TICKS);
    }

    private static void armorTick(Player player, ServerLevel sl) {
        if (!player.getPersistentData().contains(ARMOR_UNTIL)) return;
        if (!isActive(player, ARMOR_UNTIL)) {
            endArmor(player, sl, false);                           // time up: the plates fade by themselves
            return;
        }
        if (player.tickCount % 20 == 0) Spells.keepEffect(player, MobEffects.STRENGTH, ARMOR_STRENGTH_LEVEL, 40);
    }

    /** 'early' = taken off before its time, so its look is taken away at once instead of fading. */
    private static void endArmor(Player player, ServerLevel sl, boolean early) {
        player.getPersistentData().remove(ARMOR_UNTIL);
        removeModifier(player, Attributes.SCALE, ARMOR_SCALE);
        removeModifier(player, Attributes.KNOCKBACK_RESISTANCE, ARMOR_FOOTING);
        if (early) HangedManFx.blackArmorEnd(sl, player);
        HangedManFx.statusEnd(player, Status.BLACK_ARMOR);
    }

    // =========================
    // Ability 7 - Degeneration
    // Normal: a rotten orb flies into the enemy you look at. It lands (HangedManFx.DEGENERATION_ARRIVES):
    //         DEGENERATION_HIT, every buff on it is eaten, and for DEGENERATION_TICKS it cannot heal and rots
    //         (DEGENERATION_DAMAGE a second, Wither). A mob turns on the nearest other mob; a player is weakened.
    // Sneak:  Trinity Body. Two more bodies stand at your sides for TRINITY_TICKS: they strike with you
    //         (TRINITY_BONUS more damage dealt) and each lethal blow kills one of them instead of you.
    // =========================
    public static void hangedManDegeneration(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!ready(player, bypassClassCheck, DEGENERATION_SEQUENCE)) return;

        if (player.isShiftKeyDown()) {
            trinityBody(player, sl);
            return;
        }
        LivingEntity target = Spells.aimEnemy(player, sl, DEGENERATION_RANGE);
        if (target == null) {
            player.sendSystemMessage(Component.literal("No one stands before you."));
            return; // nothing spent
        }
        if (!Spells.paySpirituality(player, DEGENERATION_COST)) return;
        riseFromShadow(player, sl);
        degeneration(player, sl, target);
    }

    private static void degeneration(Player player, ServerLevel sl, LivingEntity target) {
        HangedManFx.degeneration(sl, player, target);
        Later.run(sl, HangedManFx.DEGENERATION_ARRIVES, () -> {
            if (!target.isAlive() || target.level() != sl || !Spells.casterStillHere(player, sl)) return;
            Spells.strike(player, target, DEGENERATION_HIT);
            if (!target.isAlive()) return;

            List<MobEffectInstance> eaten = new ArrayList<>();
            for (MobEffectInstance effect : target.getActiveEffects()) {
                if (effect.getEffect().value().isBeneficial()) eaten.add(effect);
            }
            for (MobEffectInstance effect : eaten) target.removeEffect(effect.getEffect());
            if (!eaten.isEmpty()) HangedManFx.degenerationStrip(sl, target, eaten);

            long until = sl.getGameTime() + DEGENERATION_TICKS;
            target.getPersistentData().putLong(DEGENERATING_UNTIL, until);
            NoHeal.apply(target, DEGENERATION_TICKS);
            HangedManFx.degenerationMark(sl, target, DEGENERATION_TICKS);

            if (target instanceof Mob) {
                LivingEntity ally = nearestOtherMob(sl, target);
                if (ally != null && Taunt.apply(target, ally, DEGENERATION_TICKS)) {
                    HangedManFx.degenerationBetray(sl, target, ally, DEGENERATION_TICKS);
                }
            } else {
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, DEGENERATION_TICKS, DEGENERATION_PLAYER_WEAKNESS));
            }
            degenerationTick(player, sl, target, until, 0);
        });
    }

    /** Every half second while it rots: ash falls; every second it hurts. A new Degeneration takes over. */
    private static void degenerationTick(Player player, ServerLevel sl, LivingEntity target, long until, int count) {
        Later.run(sl, 10, () -> {
            if (!target.isAlive() || target.level() != sl || !Spells.casterStillHere(player, sl)) return;
            if (target.getPersistentData().getLongOr(DEGENERATING_UNTIL, 0L) != until) return;
            if (sl.getGameTime() > until) {
                target.getPersistentData().remove(DEGENERATING_UNTIL);
                return;
            }
            HangedManFx.degenerationTick(sl, target);
            Spells.keepEffect(target, MobEffects.WITHER, 0, 40);
            if (count % 2 == 1) Spells.strike(player, target, DEGENERATION_DAMAGE);
            degenerationTick(player, sl, target, until, count + 1);
        });
    }

    /** The nearest mob to 'target' within BETRAY_REACH that is not 'target' itself, or null. */
    private static LivingEntity nearestOtherMob(ServerLevel sl, LivingEntity target) {
        LivingEntity best = null;
        double bestDistance = BETRAY_REACH * BETRAY_REACH;
        for (Mob other : sl.getEntitiesOfClass(Mob.class, target.getBoundingBox().inflate(BETRAY_REACH))) {
            if (other == target || !other.isAlive() || Concealment.isHidden(other)) continue;
            double d = other.distanceToSqr(target);
            if (d < bestDistance) {
                bestDistance = d;
                best = other;
            }
        }
        return best;
    }

    private static void trinityBody(Player player, ServerLevel sl) {
        if (isActive(player, TRINITY_UNTIL)) {
            player.sendSystemMessage(Component.literal("Your other bodies already stand beside you."));
            return;
        }
        if (!Spells.paySpirituality(player, TRINITY_COST)) return;
        riseFromShadow(player, sl);
        CompoundTag data = player.getPersistentData();
        data.putLong(TRINITY_UNTIL, sl.getGameTime() + TRINITY_TICKS);
        data.putInt(TRINITY_LEFT, TRINITY_BODIES);
        HangedManFx.trinity(sl, player, TRINITY_TICKS);
        HangedManFx.status(player, Status.TRINITY, TRINITY_TICKS);
        DeathWard.arm(player, TRINITY_TICKS, TRINITY_REVIVE_HEALTH, DarkAngel::bodyDiesInstead);
    }

    /** A lethal blow: one of the other bodies dies in your place. While one is left, the next blow is covered too. */
    private static void bodyDiesInstead(LivingEntity saved, DamageSource source, ServerLevel sl) {
        if (!(saved instanceof Player player)) return;
        CompoundTag data = player.getPersistentData();
        int left = data.getIntOr(TRINITY_LEFT, 0) - 1;
        data.putInt(TRINITY_LEFT, left);
        HangedManFx.trinityEcho(sl, player);
        long remaining = data.getLongOr(TRINITY_UNTIL, 0L) - sl.getGameTime();
        if (left > 0 && remaining > 0) {
            player.sendSystemMessage(Component.literal("One of your bodies dies in your place. " + left + " left."));
            DeathWard.arm(player, (int) remaining, TRINITY_REVIVE_HEALTH, DarkAngel::bodyDiesInstead);
        } else {
            player.sendSystemMessage(Component.literal("Your last other body dies in your place."));
            endTrinity(player, sl);
        }
    }

    private static void trinityTick(Player player, ServerLevel sl) {
        if (!player.getPersistentData().contains(TRINITY_UNTIL)) return;
        if (!isActive(player, TRINITY_UNTIL)) endTrinity(player, sl);
    }

    private static void endTrinity(Player player, ServerLevel sl) {
        CompoundTag data = player.getPersistentData();
        data.remove(TRINITY_UNTIL);
        data.remove(TRINITY_LEFT);
        data.remove(TRINITY_ECHO);
        if (DeathWard.isArmed(player)) DeathWard.disarm(player);
        HangedManFx.trinityEnd(sl, player);
        HangedManFx.statusEnd(player, Status.TRINITY);
    }

    // =========================
    // Helpers shared by the abilities
    // =========================

    /** Has this pathway (or the check is skipped) and has reached the Sequence the ability needs. */
    private static boolean ready(Player player, boolean bypassClassCheck, int sequence) {
        if (!bypassClassCheck && !SoulCore.getPathway(player).equals(PATHWAY)) return false;
        return Beyonder.hasSequence(player, sequence);
    }

    /** True while the game time saved under 'key' is still ahead. */
    private static boolean isActive(Entity entity, String key) {
        return entity.getPersistentData().getLongOr(key, 0L) > entity.level().getGameTime();
    }

    private static void setModifier(LivingEntity entity, Holder<Attribute> attribute, Identifier id, double amount,
                                    AttributeModifier.Operation operation) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance == null) return;
        if (instance.hasModifier(id)) instance.removeModifier(id);
        instance.addTransientModifier(new AttributeModifier(id, amount, operation));   // transient: not saved
    }

    private static void removeModifier(LivingEntity entity, Holder<Attribute> attribute, Identifier id) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance != null && instance.hasModifier(id)) instance.removeModifier(id);
    }

    /** After a relog or a death: nothing of the timed casts is left over (their looks did not survive either). */
    private static void clearState(Player player) {
        CompoundTag data = player.getPersistentData();
        if (data.contains(TRINITY_UNTIL) && DeathWard.isArmed(player)) DeathWard.disarm(player);   // died anyway (the void)
        for (String key : List.of(RITUAL_UNTIL, LISTEN_UNTIL, LURK_UNTIL, CLOAK_UNTIL, ARMOR_UNTIL,
                TRINITY_UNTIL, TRINITY_LEFT, TRINITY_ECHO)) {
            data.remove(key);
        }
        removeModifier(player, Attributes.SCALE, ARMOR_SCALE);
        removeModifier(player, Attributes.KNOCKBACK_RESISTANCE, ARMOR_FOOTING);
        if (!SoulCore.getPathway(player).equals(PATHWAY)) return;
        for (Status status : Status.values()) HangedManFx.statusEnd(player, status);     // a clock left running before death
    }
}
