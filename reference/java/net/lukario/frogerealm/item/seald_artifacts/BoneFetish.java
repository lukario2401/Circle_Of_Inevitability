package net.lukario.frogerealm.item.seald_artifacts;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Comparator;
import java.util.List;

/**
 * ══════════════════════════════════════════════════════════════
 *   THE BONE FETISH  —  Beyonder Characteristic Item
 *   Sequence 3 — Witch (Hunter / Predator Pathway)
 * ══════════════════════════════════════════════════════════════
 *
 * A totem roughly the length of a hand, carved from the shinbone
 * of something that used to be human. The carving is precise —
 * too precise to have been done with metal tools. The symbols
 * don't repeat, but they feel like they do.
 * It smells like wet soil, old smoke, and something you can't name
 * but recognize from dreams.
 *
 * The Witch who made it never named it. Witches at Sequence 3
 * don't name things anymore — names are too close to curses,
 * and curses have a way of completing themselves.
 * She carved it from her own shinbone.
 * She walked fine afterward.
 * That's how you know it worked.
 *
 * Carrying it feels like being watched from just behind your left
 * shoulder. Something is always just behind your left shoulder.
 * That something is the Fetish's spirit — nameless, patient,
 * and slowly deciding whether you are prey or not.
 *
 * TYPE: Carry-on (held in hand)
 * PATHWAY: Hunter (Witch sub-branch — Curse / Totem / Primal)
 * SEQUENCE EQUIVALENT: 3 — Witch
 *
 *
 * ── PASSIVE I — Primal Sense ──────────────────────────────────
 *   While held, the player perceives as a predator:
 *     • All living entities within 24 blocks glow faintly
 *       (Glowing applied each second — even through walls).
 *     • Player receives Night Vision (no HUD icon).
 *     • Player movement is quieter: nearby hostile mobs have
 *       their detection range reduced by 30% (simulated via
 *       frequent Invisibility pulses, 1-tick, no visual).
 *     • Player receives a passive Speed I.
 *
 * ── PASSIVE II — Curse Accumulator ────────────────────────────
 *   The Fetish drinks suffering. Each time the player deals
 *   damage to a living entity, the Fetish stores "Hex Charge":
 *     +1 per hit. +3 if the hit kills the entity.
 *     +5 if the killed entity was a boss or had > 100 max HP.
 *   Hex Charge is capped at 99 (stored: "BF_HexCharge").
 *   Hex Charge decays -1 per 30 s of not killing anything.
 *
 * ── PASSIVE III — Totem Ward ──────────────────────────────────
 *   At 20+ Hex Charge: a Ward forms around the player.
 *   The Ward intercepts the first hit each minute that would
 *   deal > 6 HP in one blow — halves that damage.
 *   (Ward stored as "BF_WardCooldown"; 1200 tick cooldown.)
 *   At 50+ Charge: Ward intercepts hits > 4 HP.
 *   At 80+ Charge: Ward intercepts ALL hits > 2 HP.
 *   Intercepted hits trigger a bone-smoke particle burst.
 *
 * ── PASSIVE IV — Bloodcurse Aura ──────────────────────────────
 *   At 40+ Hex Charge: passive hexes leak from the Fetish.
 *   All hostile mobs within 10 blocks receive:
 *     • Weakness I (permanent while in range)
 *     • Slowness I (permanent while in range)
 *     • Poison I (applied every 5 s, short duration — a slow rot)
 *   At 70+ Charge: range expands to 18 blocks.
 *     • Weakness II and Slowness II instead.
 *     • Wither I applied every 10 s (bone-deep curse).
 *
 * ── ACTIVE (right-click) — Lay a Hex ─────────────────────────
 *   Costs 15 Hex Charge. Cooldown: 20 s.
 *   Targets the nearest entity within 30 blocks.
 *
 *   Effect scales with current Hex Charge AT TIME OF CAST:
 *
 *   15–29 Charge — Minor Hex:
 *     Target: Weakness II + Slowness II + Poison I for 15 s.
 *     Bone smoke particles erupt from target.
 *
 *   30–54 Charge — Binding Hex:
 *     Target: Weakness II + Slowness III + Poison II + Blindness
 *     for 12 s. Target is pulled 5 blocks toward the player.
 *     A second random nearby entity is also Weakened + Slowed.
 *
 *   55–79 Charge — Shattering Hex:
 *     Target: above + Wither II for 10 s.
 *     Target's armor is bypassed: 8 true magic damage on cast.
 *     All entities within 8 blocks of target: Poison II for 8 s.
 *     (The curse splinters and finds new hosts.)
 *
 *   80–99 Charge — PRIMAL CURSE:
 *     The Fetish screams (sound implied by message).
 *     Nearest entity: Wither III + Weakness III + Slowness IV
 *     + Poison III for 20 s. 15 true magic damage.
 *     All entities within 16 blocks: Weakness II + Poison II
 *     for 10 s.
 *     Target is lifted 3 blocks then dropped (brief Levitation).
 *     Ground beneath target: 3x3 area of grass/dirt → Mycelium
 *     (the curse marks the earth).
 *     Costs ALL Hex Charge (drains to 0).
 *
 *
 * ── DRAWBACK — The Watching ────────────────────────────────────
 *   The spirit in the Fetish is nameless. It is deciding.
 *   The longer you carry the Fetish, the more it learns you.
 *   The more it learns you, the less certain it is you are the
 *   predator and not the prey.
 *
 *   "Vigil Points" accumulate:
 *     +1 per second held
 *     +5 per Hex cast
 *     +10 per kill while holding
 *     +20 per Primal Curse cast
 *     -3 per second NOT held (the spirit loses interest slowly)
 *     -20 when the player takes more than 6 damage in one hit
 *       (pain reassures the spirit — prey does not carry fetishes)
 *
 *   Vigil thresholds (stored: "BF_Vigil"):
 *
 *   0–199 — Dormant. Only the smell and the feeling of being
 *           watched. Faint bone-dust particles near the player.
 *           No mechanical drawbacks yet.
 *
 *   200–399 — Stirring: the spirit has noticed you.
 *     • Animals within 12 blocks flee the player constantly
 *       (Fear effect — Scared AI triggered via repeated
 *       velocity push away from player each second).
 *     • Peaceful mobs no longer spawn near you (simulated:
 *       nearby grass occasionally turns to podzol — the land
 *       reads as hunted ground).
 *     • Occasional whisper in chat: fragments of the Witch's
 *       last thoughts, incompletely transferred to the bone.
 *     • Player receives Hunger debuff passively (the spirit
 *       drains appetite — you feed it instead of yourself).
 *
 *   400–599 — Evaluation: the spirit is measuring you.
 *     • Player begins attracting mobs. Hostile mobs within 24
 *       blocks gain permanent Speed I and Strength I while
 *       the player holds the Fetish (the spirit tips the scale).
 *     • Player's own attacks occasionally trigger on themselves:
 *       5% chance any outgoing melee hit reflects 1 damage back.
 *     • Night Vision flickers — occasionally replaced by
 *       Blindness for 1 s (the spirit covers your eyes briefly).
 *     • Hex Charge decay rate doubles.
 *
 *   600–799 — Judgment: the spirit has decided you might be prey.
 *     • Wither I applied to the player every 20 s (brief, 3 s).
 *     • Player cannot use Totem Ward (BF_WardCooldown locked).
 *     • Hostile mobs gain Strength II and Speed II in range.
 *     • Primal Sense range halved (spirit obscures your senses).
 *     • Hex Charge cap reduced to 49 (curse leaks back into you).
 *     • Whispers become more frequent and more coherent.
 *       They start giving instructions.
 *
 *   800–999 — Verdict: you are prey.
 *     • Permanent Weakness I + Slowness I + Hunger while held.
 *     • Hostile mobs in 30 blocks target the player preferentially
 *       (simulated: all hostiles given a velocity nudge toward
 *       player every 3 s).
 *     • Player is occasionally rooted: Slowness V for 2 s every
 *       15 s (the spirit pins prey before the kill).
 *     • Bone smoke pours constantly from player.
 *     • All Hex casts cost double Hex Charge.
 *     • Speed I passive removed.
 *
 *   1000+ — THE HUNT BEGINS:
 *     The spirit has made its decision. You are prey.
 *     The Fetish begins the ritual.
 *     Player receives: Wither III + Weakness III + Slowness V
 *     + Blindness for 90 s. 12 true magic damage.
 *     All Hex Charge drained to 0. Vigil resets to 700.
 *     (The spirit doesn't kill you. A hunt without a chase
 *      is just slaughter. It wants the chase.)
 *     All nearby hostile mobs gain a 30 s Strength III buff.
 *
 *
 * ── NOTE ──────────────────────────────────────────────────────
 *   The Fetish can be dropped — unlike the Chord, it does not
 *   bind itself to you. But if you carry it long enough, the
 *   spirit will remember your scent. Vigil decays slowly
 *   (3/s) but the spirit has a long memory.
 *   Drop it early. Don't let it learn your name.
 *   Don't have a name it can learn.
 *
 *
 * ── STATE (player.getPersistentData()) ────────────────────────
 *   "BF_HexCharge"       int    — accumulated hex charge (0–99)
 *   "BF_Vigil"           int    — watching threshold (0–1000+)
 *   "BF_WardCooldown"    int    — ticks until ward resets
 *   "BF_HexCooldown"     int    — ticks until next hex cast
 *   "BF_HexDecayTick"    int    — ticks until next charge decay
 *   "BF_WhisperTick"     int    — ticks until next whisper
 *   "BF_RootTick"        int    — ticks until next root (800+)
 *   "BF_MobNudgeTick"    int    — ticks until next mob nudge
 *   "BF_NotHeldTick"     int    — ticks since last held
 *
 *   Per-entity (on entity persistentData):
 *   "BF_HexedTick"       int    — remaining ticks of hex tracking
 *
 * ── REGISTRATION ──────────────────────────────────────────────
 *   public static final RegistryObject<Item> BONE_FETISH =
 *       ITEMS.register("bone_fetish",
 *           () -> new BoneFetish(
 *               new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
 */
@Mod.EventBusSubscriber
public class BoneFetish extends Item {

    // ── Vigil thresholds ──────────────────────────────────────
    private static final int VIGIL_STIRRING   = 200;
    private static final int VIGIL_EVAL       = 400;
    private static final int VIGIL_JUDGMENT   = 600;
    private static final int VIGIL_VERDICT    = 800;
    private static final int VIGIL_HUNT       = 1000;

    // ── Hex charge thresholds ─────────────────────────────────
    private static final int HEX_CAP_NORMAL   = 99;
    private static final int HEX_CAP_JUDGED   = 49;  // at Judgment tier
    private static final int HEX_COST_HEX     = 15;
    private static final int HEX_DECAY_INT    = 600; // 30 s

    // ── Cooldowns ─────────────────────────────────────────────
    private static final int HEX_COOLDOWN     = 400;  // 20 s
    private static final int WARD_COOLDOWN    = 1200; // 60 s
    private static final int WHISPER_INT      = 500;  // 25 s
    private static final int ROOT_INT         = 300;  // 15 s
    private static final int MOB_NUDGE_INT    = 60;   // 3 s
    private static final int ANIMAL_FLEE_INT  = 20;   // 1 s

    // ── Ranges ────────────────────────────────────────────────
    private static final double SENSE_RANGE   = 24.0;
    private static final double AURA_RANGE_LO = 10.0;
    private static final double AURA_RANGE_HI = 18.0;
    private static final double HEX_RANGE     = 30.0;

    // ── Witch's last whispers ─────────────────────────────────
    private static final String[] WHISPERS_EARLY = {
            "it remembers the shape of things that ran",
            "you smell like the wrong side of the treeline",
            "don't run. running is an answer.",
            "the bone knows every footstep you've ever made",
            "she didn't name it because names are promises",
    };
    private static final String[] WHISPERS_LATE = {
            "face north",
            "leave the clearing",
            "something is faster than you",
            "you were prey before you picked it up",
            "put it down. you can still put it down.",
    };

    public BoneFetish(Properties properties) {
        super(properties);
    }

    // ═════════════════════════════════════════════════════════
    // ACTIVE — Lay a Hex
    // ═════════════════════════════════════════════════════════
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);

        var data = player.getPersistentData();
        int hexCD = data.getInt("BF_HexCooldown");
        if (hexCD > 0) {
            player.sendSystemMessage(
                    Component.literal("The curse hasn't settled yet. (" + (hexCD / 20) + "s)")
                            .withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.ITALIC));
            return InteractionResultHolder.pass(stack);
        }

        int charge = data.getInt("BF_HexCharge");
        if (charge < HEX_COST_HEX) {
            player.sendSystemMessage(
                    Component.literal("Not enough suffering stored. (" + charge + "/15)")
                            .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            return InteractionResultHolder.pass(stack);
        }

        int vigil = data.getInt("BF_Vigil");
        int cost  = (vigil >= VIGIL_VERDICT) ? HEX_COST_HEX * 2 : HEX_COST_HEX;
        if (charge < cost) {
            player.sendSystemMessage(
                    Component.literal("The spirit demands more. (" + charge + "/" + cost + ")")
                            .withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.ITALIC));
            return InteractionResultHolder.pass(stack);
        }

        // Find nearest target
        LivingEntity target = level.getEntitiesOfClass(LivingEntity.class,
                        player.getBoundingBox().inflate(HEX_RANGE),
                        e -> e != player)
                .stream()
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(player)))
                .orElse(null);

        if (target == null) {
            player.sendSystemMessage(
                    Component.literal("Nothing in range to curse.")
                            .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            return InteractionResultHolder.pass(stack);
        }

        // Determine tier and cast
        boolean isPrimal = (charge >= 80);
        if (isPrimal) {
            castPrimalCurse(player, level, target, data);
            data.putInt("BF_HexCharge", 0);
            addVigil(data, 20);
        } else {
            castHex(player, level, target, charge);
            data.putInt("BF_HexCharge", Math.max(0, charge - cost));
        }

        addVigil(data, 5);
        data.putInt("BF_HexCooldown", HEX_COOLDOWN);
        player.getCooldowns().addCooldown(this, HEX_COOLDOWN);
        return InteractionResultHolder.success(stack);
    }

    private static void castHex(Player player, Level level, LivingEntity target, int charge) {
        if (charge < 30) {
            // Minor Hex
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,          300, 1, false, true));
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 300, 1, false, true));
            target.addEffect(new MobEffectInstance(MobEffects.POISON,            300, 0, false, true));
            boneSmoke(level, target.position(), 20);
            player.sendSystemMessage(
                    Component.literal("Minor hex placed.")
                            .withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.ITALIC));

        } else if (charge < 55) {
            // Binding Hex
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,          240, 1, false, true));
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 240, 2, false, true));
            target.addEffect(new MobEffectInstance(MobEffects.POISON,            240, 1, false, true));
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,         240, 0, false, true));
            // Pull toward player
            Vec3 pull = player.position().subtract(target.position()).normalize().scale(0.9);
            target.setDeltaMovement(target.getDeltaMovement().add(pull.x, 0.3, pull.z));
            target.hurtMarked = true;
            // Secondary target
            level.getEntitiesOfClass(LivingEntity.class,
                    target.getBoundingBox().inflate(8), e -> e != player && e != target
            ).stream().findFirst().ifPresent(sec -> {
                sec.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,          160, 1, false, true));
                sec.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 160, 1, false, true));
            });
            boneSmoke(level, target.position(), 35);
            player.sendSystemMessage(
                    Component.literal("Binding hex — the curse finds new hosts.")
                            .withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.ITALIC));

        } else {
            // Shattering Hex
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,          200, 1, false, true));
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 2, false, true));
            target.addEffect(new MobEffectInstance(MobEffects.POISON,            200, 1, false, true));
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,         200, 0, false, true));
            target.addEffect(new MobEffectInstance(MobEffects.WITHER,            200, 1, false, true));
            target.hurt(level.damageSources().magic(), 8f);
            // Splinter to nearby
            level.getEntitiesOfClass(LivingEntity.class,
                    target.getBoundingBox().inflate(8), e -> e != player && e != target
            ).forEach(e ->
                    e.addEffect(new MobEffectInstance(MobEffects.POISON, 160, 1, false, true))
            );
            boneSmoke(level, target.position(), 50);
            player.sendSystemMessage(
                    Component.literal("Shattering hex — bone-deep.")
                            .withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.ITALIC));
        }
    }

    private static void castPrimalCurse(Player player, Level level,
                                        LivingEntity target, net.minecraft.nbt.CompoundTag data) {
        // Target
        target.addEffect(new MobEffectInstance(MobEffects.WITHER,            400, 2, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,          400, 2, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 400, 3, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.POISON,            400, 2, false, true));
        target.hurt(level.damageSources().magic(), 15f);
        // Lift and drop
        target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 40, 2, false, true));

        // Surrounding entities
        level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(16), e -> e != player && e != target
        ).forEach(e -> {
            e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 1, false, true));
            e.addEffect(new MobEffectInstance(MobEffects.POISON,   200, 1, false, true));
        });

        // Corrupt ground — 3x3 dirt/grass → mycelium
        if (level instanceof ServerLevel sl) {
            BlockPos center = target.blockPosition();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos pos = center.offset(dx, 0, dz);
                    var state = level.getBlockState(pos);
                    if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT)
                            || state.is(Blocks.PODZOL) || state.is(Blocks.COARSE_DIRT)) {
                        level.setBlock(pos, Blocks.MYCELIUM.defaultBlockState(), 3);
                    }
                }
            }
            boneSmoke(level, target.position(), 80);
            sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                    target.getX(), target.getY() + 1, target.getZ(), 1, 0, 0, 0, 0);
        }

        player.sendSystemMessage(Component.literal("══════════════════════════").withStyle(ChatFormatting.DARK_GREEN));
        player.sendSystemMessage(Component.literal("PRIMAL CURSE")
                .withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.BOLD));
        player.sendSystemMessage(Component.literal("The bone remembers what it is to hunt.")
                .withStyle(ChatFormatting.GREEN, ChatFormatting.ITALIC));
        player.sendSystemMessage(Component.literal("══════════════════════════").withStyle(ChatFormatting.DARK_GREEN));
    }

    // ═════════════════════════════════════════════════════════
    // MAIN TICK
    // ═════════════════════════════════════════════════════════
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = event.player;
        Level  level  = player.level();
        if (level.isClientSide) return;

        var data = player.getPersistentData();
        boolean holding = isHolding(player);

        tickDown(data, "BF_HexCooldown");
        tickDown(data, "BF_WardCooldown");

        if (!holding) {
            int notHeld = data.getInt("BF_NotHeldTick") + 1;
            data.putInt("BF_NotHeldTick", notHeld);
            // Vigil decays -3/s when not held
            if ((player.tickCount % 20) == 0) {
                int vigil = data.getInt("BF_Vigil");
                if (vigil > 0) data.putInt("BF_Vigil", Math.max(0, vigil - 3));
            }
            return;
        }

        data.putInt("BF_NotHeldTick", 0);

        // ── Vigil accrual: +1/s ───────────────────────────────
        if ((player.tickCount % 20) == 0) {
            addVigil(data, 1);
        }

        int vigil  = data.getInt("BF_Vigil");
        int charge = data.getInt("BF_HexCharge");


        if (vigil%50==0 && vigil != 0){
            player.sendSystemMessage(Component.literal("Vigil: " + vigil));
            data.putInt("BF_Vigil",data.getInt("BF_Vigil")+1);
        }

        // ── Hunt trigger ──────────────────────────────────────
        if (vigil >= VIGIL_HUNT) {
            executeHunt(player, level, data);
            return;
        }

        // ── Hex charge decay ──────────────────────────────────
        int decayRate = (vigil >= VIGIL_EVAL) ? HEX_DECAY_INT / 2 : HEX_DECAY_INT;
        int decayTick = data.getInt("BF_HexDecayTick") - 1;
        if (decayTick <= 0) {
            data.putInt("BF_HexDecayTick", decayRate);
            if (charge > 0) {
                data.putInt("BF_HexCharge", charge - 1);
            }
        } else {
            data.putInt("BF_HexDecayTick", decayTick);
        }

        // ── Hex charge cap check ──────────────────────────────
        int cap = (vigil >= VIGIL_JUDGMENT) ? HEX_CAP_JUDGED : HEX_CAP_NORMAL;
        if (charge > cap) data.putInt("BF_HexCharge", cap);

        // ── Base passives ─────────────────────────────────────
        applyBasePassives(player, level, vigil);

        // ── Bloodcurse aura ───────────────────────────────────
        charge = data.getInt("BF_HexCharge"); // re-read after decay
        if (charge >= 40 && (player.tickCount % 20) == 0) {
            tickBloodcurseAura(player, level, charge);
        }

        // ── Passive particles ─────────────────────────────────
        if (level instanceof ServerLevel sl && (player.tickCount % 25) == 0) {
            Vec3 p = player.position();
            sl.sendParticles(ParticleTypes.SMOKE,
                    p.x + (Math.random() - 0.5) * 0.6,
                    p.y + Math.random() * 2,
                    p.z + (Math.random() - 0.5) * 0.6,
                    1, 0, 0.01, 0, 0.01);
        }

        // ── Drawbacks ─────────────────────────────────────────
        applyVigilDrawbacks(player, level, data, vigil);
    }

    // ── Base passives ─────────────────────────────────────────
    private static void applyBasePassives(Player player, Level level, int vigil) {
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 240, 0, false, false));

        // Speed I unless Verdict+
        if (vigil < VIGIL_VERDICT) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 0, false, false));
        }

        // Sense — glow all living in range (halved at Judgment+)
        if ((player.tickCount % 20) == 0) {
            double range = (vigil >= VIGIL_JUDGMENT) ? SENSE_RANGE / 2 : SENSE_RANGE;
            level.getEntitiesOfClass(LivingEntity.class,
                    player.getBoundingBox().inflate(range), e -> e != player
            ).forEach(e ->
                    e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 25, 0, false, false))
            );
        }
    }

    // ── Bloodcurse aura ───────────────────────────────────────
    private static void tickBloodcurseAura(Player player, Level level, int charge) {
        double range = (charge >= 70) ? AURA_RANGE_HI : AURA_RANGE_LO;
        int weakLvl  = (charge >= 70) ? 1 : 0;
        int slowLvl  = (charge >= 70) ? 1 : 0;

        level.getEntitiesOfClass(Monster.class,
                player.getBoundingBox().inflate(range)
        ).forEach(mob -> {
            mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,          30, weakLvl, false, false));
            mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, slowLvl, false, false));
        });

        // Poison pulse every 5 s
        if ((player.tickCount % 100) == 0) {
            level.getEntitiesOfClass(Monster.class,
                    player.getBoundingBox().inflate(range)
            ).forEach(mob ->
                    mob.addEffect(new MobEffectInstance(MobEffects.POISON, 40, 0, false, false))
            );
        }

        // Wither pulse at 70+ every 10 s
        if (charge >= 70 && (player.tickCount % 200) == 0) {
            level.getEntitiesOfClass(Monster.class,
                    player.getBoundingBox().inflate(range)
            ).forEach(mob ->
                    mob.addEffect(new MobEffectInstance(MobEffects.WITHER, 40, 0, false, false))
            );
        }
    }

    // ── Vigil drawback tiers ──────────────────────────────────
    private static void applyVigilDrawbacks(Player player, Level level,
                                            net.minecraft.nbt.CompoundTag data, int vigil) {

        // Stirring (200+)
        if (vigil >= VIGIL_STIRRING) {
            // Animals flee
            if ((player.tickCount % ANIMAL_FLEE_INT) == 0) {
                Vec3 pos = player.position();
                level.getEntitiesOfClass(Animal.class,
                        player.getBoundingBox().inflate(12)
                ).forEach(animal -> {
                    Vec3 flee = animal.position().subtract(pos).normalize().scale(0.5);
                    animal.setDeltaMovement(
                            animal.getDeltaMovement().add(flee.x, 0.1, flee.z));
                    animal.hurtMarked = true;
                });
            }

            // Hunger drain
            if ((player.tickCount % 40) == 0) {
                player.getFoodData().setFoodLevel(
                        Math.max(0, player.getFoodData().getFoodLevel() - 1));
            }

            // Whispers
            int whisperTick = data.getInt("BF_WhisperTick") - 1;
            if (whisperTick <= 0) {
                data.putInt("BF_WhisperTick", WHISPER_INT);
                String[] pool = (vigil >= VIGIL_JUDGMENT) ? WHISPERS_LATE : WHISPERS_EARLY;
                String whisper = pool[level.random.nextInt(pool.length)];
                player.sendSystemMessage(
                        Component.literal(whisper)
                                .withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.ITALIC));
            } else {
                data.putInt("BF_WhisperTick", whisperTick);
            }

            // Occasional grass → podzol near player
            if ((player.tickCount % 200) == 0 && level instanceof ServerLevel sl) {
                BlockPos bp = player.blockPosition().offset(
                        level.random.nextInt(7) - 3, 0, level.random.nextInt(7) - 3);
                if (level.getBlockState(bp).is(Blocks.GRASS_BLOCK)) {
                    level.setBlock(bp, Blocks.PODZOL.defaultBlockState(), 3);
                }
            }
        }

        // Evaluation (400+)
        if (vigil >= VIGIL_EVAL) {
            // Buff nearby hostiles
            if ((player.tickCount % 20) == 0) {
                level.getEntitiesOfClass(Monster.class,
                        player.getBoundingBox().inflate(24)
                ).forEach(mob -> {
                    mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 30, 0, false, false));
                    mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST,   30, 0, false, false));
                });
            }
            // Night vision flicker: 3% chance each second of 1s blindness
            if ((player.tickCount % 20) == 0 && level.random.nextFloat() < 0.03f) {
                player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 20, 0, false, true));
                player.removeEffect(MobEffects.NIGHT_VISION);
            }
        }

        // Judgment (600+)
        if (vigil >= VIGIL_JUDGMENT) {
            // Wither pulse every 20 s
            if ((player.tickCount % 400) == 0) {
                player.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0, false, true));
            }
            // Stronger mob buffs
            if ((player.tickCount % 20) == 0) {
                level.getEntitiesOfClass(Monster.class,
                        player.getBoundingBox().inflate(24)
                ).forEach(mob -> {
                    mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 30, 1, false, false));
                    mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST,   30, 1, false, false));
                });
            }
        }

        // Verdict (800+)
        if (vigil >= VIGIL_VERDICT) {
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,          40, 0, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.HUNGER,            40, 0, false, false));

            // Root every 15 s
            int rootTick = data.getInt("BF_RootTick") - 1;
            if (rootTick <= 0) {
                data.putInt("BF_RootTick", ROOT_INT);
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 4, false, true));
                player.sendSystemMessage(
                        Component.literal("The spirit pins you.")
                                .withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.ITALIC));
            } else {
                data.putInt("BF_RootTick", rootTick);
            }

            // Nudge all hostiles toward player every 3 s
            int nudgeTick = data.getInt("BF_MobNudgeTick") - 1;
            if (nudgeTick <= 0) {
                data.putInt("BF_MobNudgeTick", MOB_NUDGE_INT);
                Vec3 pos = player.position();
                level.getEntitiesOfClass(Monster.class,
                        player.getBoundingBox().inflate(30)
                ).forEach(mob -> {
                    Vec3 toward = pos.subtract(mob.position()).normalize().scale(0.15);
                    mob.setDeltaMovement(mob.getDeltaMovement().add(toward));
                    mob.hurtMarked = true;
                });
            } else {
                data.putInt("BF_MobNudgeTick", nudgeTick);
            }

            // Dense bone smoke
            if (level instanceof ServerLevel sl && (player.tickCount % 8) == 0) {
                Vec3 p = player.position();
                sl.sendParticles(ParticleTypes.SMOKE,
                        p.x + (Math.random() - 0.5) * 1.2,
                        p.y + Math.random() * 2.2,
                        p.z + (Math.random() - 0.5) * 1.2,
                        2, 0, 0.02, 0, 0.02);
            }
        }
    }

    // ── The Hunt Begins ───────────────────────────────────────
    private static void executeHunt(Player player, Level level,
                                    net.minecraft.nbt.CompoundTag data) {
        player.hurt(level.damageSources().magic(), 24f); // 12 hearts
        player.addEffect(new MobEffectInstance(MobEffects.WITHER,            1800, 2, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,          1800, 2, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 1800, 4, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,         1800, 0, false, true));

        // Buff all nearby hostiles massively
        level.getEntitiesOfClass(Monster.class,
                player.getBoundingBox().inflate(40)
        ).forEach(mob -> {
            mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST,   600, 2, false, true));
            mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 1, false, true));
        });

        data.putInt("BF_Vigil",    VIGIL_JUDGMENT + 100); // reset to 700
        data.putInt("BF_HexCharge", 0);

        if (level instanceof ServerLevel sl) {
            Vec3 p = player.position();
            for (int i = 0; i < 60; i++) {
                double angle = (Math.PI * 2.0 / 60) * i;
                sl.sendParticles(ParticleTypes.SMOKE,
                        p.x + Math.cos(angle) * 5, p.y + 1,
                        p.z + Math.sin(angle) * 5,
                        1, 0, 0.3, 0, 0.05);
            }
        }

        player.sendSystemMessage(Component.literal("══════════════════════════").withStyle(ChatFormatting.DARK_GREEN));
        player.sendSystemMessage(Component.literal("THE HUNT BEGINS")
                .withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.BOLD));
        player.sendSystemMessage(Component.literal("The spirit has made its decision.")
                .withStyle(ChatFormatting.GREEN, ChatFormatting.ITALIC));
        player.sendSystemMessage(Component.literal("You are prey. Run.")
                .withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.ITALIC));
        player.sendSystemMessage(Component.literal("(It wants the chase.)")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        player.sendSystemMessage(Component.literal("══════════════════════════").withStyle(ChatFormatting.DARK_GREEN));
    }

    // ── On deal damage: +Hex Charge ───────────────────────────
    @SubscribeEvent
    public static void onDealDamage(LivingHurtEvent event) {
        Entity src = event.getSource().getEntity();
        if (!(src instanceof Player player)) return;
        if (player.level().isClientSide) return;
        if (!isHolding(player)) return;

        var data = player.getPersistentData();
        int vigil = data.getInt("BF_Vigil");
        int cap   = (vigil >= VIGIL_JUDGMENT) ? HEX_CAP_JUDGED : HEX_CAP_NORMAL;
        int charge = data.getInt("BF_HexCharge");
        data.putInt("BF_HexCharge", Math.min(cap, charge + 1));
        player.sendSystemMessage(Component.literal("Heck Charge: " + data.getInt("BF_HexCharge")));

        // Ward: intercept large incoming hits (this is on deal, not receive —
        // Ward is handled via receive event below)
        // 5% melee reflect at Eval+
        if (vigil >= VIGIL_EVAL && player.level().random.nextFloat() < 0.05f) {
            player.hurt(player.level().damageSources().magic(), 1f);
        }
    }

    // ── On receive damage: Totem Ward ─────────────────────────
    @SubscribeEvent
    public static void onReceiveDamage(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide) return;
        if (!isHolding(player)) return;

        var data = player.getPersistentData();
        int vigil  = data.getInt("BF_Vigil");
        int charge = data.getInt("BF_HexCharge");
        float dmg  = event.getAmount();

        // Ward threshold varies with charge
        float wardThreshold = (charge >= 80) ? 2f : (charge >= 50) ? 4f : 6f;
        boolean wardLocked = (vigil >= VIGIL_JUDGMENT);

        if (!wardLocked && charge >= 20 && dmg > wardThreshold) {
            int wardCD = data.getInt("BF_WardCooldown");
            if (wardCD <= 0) {
                event.setAmount(dmg * 0.5f);
                data.putInt("BF_WardCooldown", WARD_COOLDOWN);
                boneSmoke(player.level(), player.position(), 30);
                player.sendSystemMessage(
                        Component.literal("The Fetish absorbs.")
                                .withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.ITALIC));
            }
        }

        // Pain reduces Vigil
        if (dmg >= 6f) {
            addVigil(data, -20);
        }
    }

    // ── On kill: +3 charge, +10 vigil ────────────────────────
    @SubscribeEvent
    public static void onKill(LivingDeathEvent event) {
        Entity killer = event.getSource().getEntity();
        if (!(killer instanceof Player player)) return;
        if (player.level().isClientSide) return;
        if (!isHolding(player)) return;

        var data  = player.getPersistentData();
        int vigil = data.getInt("BF_Vigil");
        int cap   = (vigil >= VIGIL_JUDGMENT) ? HEX_CAP_JUDGED : HEX_CAP_NORMAL;
        int charge = data.getInt("BF_HexCharge");

        float maxHp = event.getEntity().getMaxHealth();
        int bonus   = (maxHp > 100) ? 5 : 3;
        data.putInt("BF_HexCharge", Math.min(cap, charge + bonus));
        addVigil(data, 10);

        if (player.level() instanceof ServerLevel sl) {
            boneSmoke(player.level(), event.getEntity().position(), 15);
        }
    }

    // ── Helpers ───────────────────────────────────────────────
    private static void addVigil(net.minecraft.nbt.CompoundTag data, int amount) {
        int v = data.getInt("BF_Vigil");
        data.putInt("BF_Vigil", Math.max(0, Math.min(VIGIL_HUNT + 100, v + amount)));
    }

    private static void tickDown(net.minecraft.nbt.CompoundTag data, String key) {
        int v = data.getInt(key);
        if (v > 0) data.putInt(key, v - 1);
    }

    private static boolean isHolding(Player player) {
        for (InteractionHand hand : InteractionHand.values())
            if (player.getItemInHand(hand).getItem() instanceof BoneFetish) return true;
        return false;
    }

    private static void boneSmoke(Level level, Vec3 pos, int count) {
        if (!(level instanceof ServerLevel sl)) return;
        for (int i = 0; i < count; i++) {
            sl.sendParticles(ParticleTypes.SMOKE,
                    pos.x + (sl.random.nextDouble() - 0.5) * 1.5,
                    pos.y + sl.random.nextDouble() * 2,
                    pos.z + (sl.random.nextDouble() - 0.5) * 1.5,
                    1, 0, 0.05, 0, 0.08);
        }
    }

    // ── Tooltip ───────────────────────────────────────────────

    @Override
    public boolean isFoil(ItemStack stack) {
        return true; // Forces the item to always render with the enchanted glow
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        tooltip.add(Component.literal("Beyonder Characteristic — Sequence 3")
                .withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.BOLD));
        tooltip.add(Component.literal("Witch  |  Hunter / Predator Pathway")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("Passive — Primal Sense:")
                .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
        tooltip.add(Component.literal("  Entities in 24 blocks glow. Night Vision. Speed I.")
                .withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("Passive — Hex Charge (hits +1, kills +3/+5):")
                .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
        tooltip.add(Component.literal("  Stored suffering. Powers all abilities. Cap: 99.")
                .withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("Passive — Totem Ward (needs 20+ charge):")
                .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
        tooltip.add(Component.literal("  Halves large hits once per minute.")
                .withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("Passive — Bloodcurse Aura (needs 40+ charge):")
                .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
        tooltip.add(Component.literal("  Weakens and rots hostiles in range.")
                .withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("Right-click — Lay a Hex (20 s cooldown, costs 15):")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        tooltip.add(Component.literal("  15–29: Minor. 30–54: Binding. 55–79: Shattering.")
                .withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal("  80+: PRIMAL CURSE — drains all charge.")
                .withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("⚠ Drawback — The Watching (Vigil Points):")
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        tooltip.add(Component.literal("  +1/s held, +10 per kill, -3/s when dropped.")
                .withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal("  200 — Animals flee. Hunger drain. Whispers.")
                .withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal("  400 — Nearby hostiles buffed. Night Vision flickers.")
                .withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal("  600 — Wither pulses. Ward disabled. Sense halved.")
                .withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.literal("  800 — Rooted every 15 s. Mobs hunt you.")
                .withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.literal("  1000 — THE HUNT BEGINS. Resets to 700.")
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        tooltip.add(Component.literal("  Can be dropped. It remembers your scent anyway.")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("\"Don't let it learn your name. Don't have a name it can learn.\"")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
