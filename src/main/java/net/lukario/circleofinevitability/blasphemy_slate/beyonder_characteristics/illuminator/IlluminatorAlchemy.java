package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.illuminator;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonder;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.SoulCore;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.illuminator.Illuminator.Group;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.illuminator.IlluminatorFx.Material;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.illuminator.IlluminatorFx.Status;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.illuminator.IlluminatorFx.Weakness;
import net.lukario.circleofinevitability.combat.Later;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.menu.AbilityWheel;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.lukario.circleofinevitability.status.Appraisal;
import net.lukario.circleofinevitability.status.Blind;
import net.lukario.circleofinevitability.status.Concealment;
import net.lukario.circleofinevitability.status.ConjuredArmor;
import net.lukario.circleofinevitability.status.Construct;
import net.lukario.circleofinevitability.status.Enfeebled;
import net.lukario.circleofinevitability.status.Exposed;
import net.lukario.circleofinevitability.status.FallGuard;
import net.lukario.circleofinevitability.status.Flight;
import net.lukario.circleofinevitability.status.Study;
import net.lukario.circleofinevitability.status.Substitute;
import net.lukario.circleofinevitability.status.Taunt;
import net.lukario.circleofinevitability.status.Unnoticed;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Illuminator, key 6: the Alchemy and Arcana wheel (Alchemist, Sequence 4; Arcane Scholar, 3). docs/ILLUMINATOR.md,
 * "Key 6 wheel".
 *
 *   Living Doll       a doll rises out of what you stand on and fights for you; what it is made of decides what it is.
 *                     From Arcane Scholar two (one of them a decoy that looks like you), from Magister three
 *   Golem             brass and iron fly together into a golem that calls the enemies near to it. From Arcane Scholar
 *                     two, from Paragon with no timer
 *   Mechanical Body   brass plates and gears: Resistance, a layer of absorption, a jetpack, cybernetic eyes, the arm
 *                     cannon. Cast again to take it off; from Magister a sneak recast turns you to white steam
 *   Dissociation      a clear, misty beam from your eye that dissolves what it touches. Wider from Magister; from
 *                     Paragon it shreds what the target shows (marks, hiding, what it knows)
 *   Knowledge Spirit  you fly as drifting pages into the mind of the enemy you look at and step out beside it: it is
 *                     Eroded (sick, weaker, its abilities dearer)
 *
 * Sneak casts the last pick again without the wheel (no cooldown, double the cost). Every entry's cost goes through
 * Illuminator.costFor (Quick Study, and from Alchemist the Rite of your Ritual Circle), every duration through
 * Illuminator.lasting. The constructs are status/Construct. The looks are IlluminatorFx's; every number is right below.
 */
public final class IlluminatorAlchemy {

    private IlluminatorAlchemy() {}

    // =========================
    // Settings (cost = spirituality, ticks: 20 = 1 second, distances in blocks)
    // =========================

    static final int ALCHEMY_SEQUENCE = 4;                      // the key
    private static final float QUICK_CAST_COST = Illuminator.QUICK_CAST_COST;            // a sneak recast: no cooldown, this many times the cost

    // Living Doll
    private static final int DOLL_SEQUENCE = 4;
    private static final int DOLL_COST = 4000;                  // Sequence 4 holds 32400
    private static final int DOLL_COOLDOWN = 400;
    private static final int DOLL_TICKS = 500;
    private static final int TWO_DOLLS_SEQUENCE = 3;            // Arcane Scholar: two, the second a decoy that looks like you
    private static final int THREE_DOLLS_SEQUENCE = 2;          // Knowledge Magister: three
    private static final float DOLL_SCALE = 0.75f;              // a doll is a husk this size under its model
    private static final float DOLL_DAMAGE = 6f;
    private static final double DOLL_LEASH = 40.0;
    private static final int BOLT_EVERY = 40;                   // the Iron Doll throws a bolt this often...
    private static final double BOLT_RANGE = 16.0;              // ...at what it fights this close...
    private static final float BOLT_DAMAGE = 14f;               // ...for this much
    private static final int DUST_BLIND = 40;                   // the Dust Doll's hits blind this long
    private static final int FROST_SLOW = 60;                   // the Frost Doll's hits slow (Slowness II) this long
    private static final double STRAW_SHOVE = 3.0;              // the Straw Doll falling shoves everything this close
    private static final double DECOY_TAUNT = 10.0;             // mobs this close go for the decoy...
    private static final int DECOY_TAUNT_EVERY = 60;            // ...told again this often

    // Golem
    private static final int GOLEM_SEQUENCE = 4;
    private static final int GOLEM_COST = 6000;
    private static final int GOLEM_COOLDOWN = 1200;
    private static final int GOLEM_TICKS = 1800;
    private static final float GOLEM_HEALTH = 200f;
    private static final float GOLEM_DAMAGE = 20f;
    private static final double GOLEM_TAUNT = 10.0;             // it calls the enemies this close to it...
    private static final int GOLEM_TAUNT_EVERY = 60;            // ...this often
    private static final double GOLEM_LEASH = 40.0;             // gone if you are further than this
    private static final int TWO_GOLEMS_SEQUENCE = 3;           // Arcane Scholar: two golems
    private static final int ENDLESS_GOLEM_SEQUENCE = 0;        // Paragon: no timer

    // Mechanical Body
    private static final int MECH_SEQUENCE = 4;
    private static final int MECH_COST = 6000;
    private static final int MECH_COOLDOWN = 1200;
    private static final int MECH_TICKS = 800;
    private static final float MECH_ABSORPTION = 20f;           // a layer of absorption (status/ConjuredArmor)
    private static final int JET_FUEL = 100;                    // ticks of flight on the jetpack, refilled on the ground
    private static final int STEAM_SEQUENCE = 2;                // Knowledge Magister: a sneak recast turns you to steam...
    private static final int STEAM_TICKS = 80;
    private static final double STEAM_RANGE = 24.0;             // ...and you re-form where you aim, this far at most
    private static final int ENDLESS_MECH_SEQUENCE = 1;         // Illuminator: no timer

    // Dissociation
    private static final int DISSOCIATION_SEQUENCE = 3;
    private static final int DISSOCIATION_COST = 9000;          // Sequence 3 holds 59780
    private static final int DISSOCIATION_COOLDOWN = 240;
    private static final double DISSOCIATION_RANGE = 24.0;
    private static final double DISSOCIATION_WIDTH = 1.5;
    private static final int WIDE_BEAM_SEQUENCE = 2;            // Knowledge Magister: a wider beam
    private static final double WIDE_BEAM_WIDTH = 3.0;
    private static final float DISSOCIATION_DAMAGE = 70f;       // a corrosion hit
    private static final float CONSTRUCT_FRAGILE = 150f;        // a construct under this maximum health is dissolved
    private static final float ARMOR_WEAR = 0.25f;              // each armor piece loses this share of its durability
    private static final int SHREDDER_SEQUENCE = 0;             // Paragon: the Information Shredder
    private static final int SHREDDED_EXPOSED = 100;

    // Knowledge Spirit
    private static final int SPIRIT_SEQUENCE = 3;
    private static final int SPIRIT_COST = 12000;
    private static final int SPIRIT_COOLDOWN = 500;
    private static final double SPIRIT_RANGE = 30.0;
    private static final double SPIRIT_JUMP = 20.0;             // with no enemy in view, a jump this far
    private static final double SPIRIT_SPEED = 1.2;             // blocks a tick as pages
    private static final int SPIRIT_MOST_FLIGHT = 80;           // 4 seconds at most
    private static final int SPIRIT_AFTER = 40;                 // still hidden this long after stepping out
    private static final int ERODED_TICKS = 160;                // Eroded: nausea, it deals this much less...
    private static final float ERODED_WEAKER = 0.3f;
    private static final float ERODED_COST = 1.5f;              // ...and every ability costs it this much (Spells.addCostScale)

    // =========================
    // Names and what is kept
    // =========================
    private static final String DOLLS = "illuminator_doll";                       // the Construct groups
    private static final String GOLEMS = "illuminator_golem";
    private static final String MECH_ARMOR = "illuminator_mechanical_body";      // its ConjuredArmor
    private static final String JET_FUEL_LEFT = "illuminator_jet_fuel";
    private static final String STEAM_UNTIL = "illuminator_steam_until";
    static final String ERODED_UNTIL = "illuminator_eroded_until";
    private static final String ERODED = "illuminator_eroded";                    // its Enfeebled

    /** What each doll is made of and the tag of its body; and the golems' tags. Not saved (nor are the constructs). */
    private record Doll(Material material, String tag, boolean decoy) {}
    private static final Map<UUID, Doll> DOLL_BODIES = new HashMap<>();
    private static final Map<UUID, String> GOLEM_BODIES = new HashMap<>();

    // =========================
    // The key
    // =========================

    private static final AbilityWheel WHEEL = AbilityWheel.create("illuminator_alchemy", "Alchemy and Arcana")
            .radius(80)
            .quickCastCooldown(false).quickCastCost(QUICK_CAST_COST)
            .entry("living_doll", IlluminatorFx.WHEEL_LIVING_DOLL, "Living Doll", DOLL_SEQUENCE,
                    DOLL_COST, DOLL_COOLDOWN, IlluminatorAlchemy::livingDoll)
            .costBy((player, cost) -> Illuminator.costFor(player, cost, "living_doll", Group.ALCHEMY))
            .entry("golem", IlluminatorFx.WHEEL_GOLEM, "Golem", GOLEM_SEQUENCE,
                    GOLEM_COST, GOLEM_COOLDOWN, IlluminatorAlchemy::golem)
            .costBy((player, cost) -> Illuminator.costFor(player, cost, "golem", Group.ALCHEMY))
            .entry("mechanical_body", IlluminatorFx.WHEEL_MECHANICAL_BODY, "Mechanical Body", MECH_SEQUENCE,
                    MECH_COST, MECH_COOLDOWN, IlluminatorAlchemy::mechanicalBody)
            .free(Illuminator::wearsMechanicalBody)                                // taking it off, or the steam: free
            .costBy((player, cost) -> Illuminator.costFor(player, cost, "mechanical_body", Group.ALCHEMY))
            .entry("dissociation", IlluminatorFx.WHEEL_DISSOCIATION, "Dissociation", DISSOCIATION_SEQUENCE,
                    DISSOCIATION_COST, DISSOCIATION_COOLDOWN, IlluminatorAlchemy::dissociation)
            .costBy((player, cost) -> Illuminator.costFor(player, cost, "dissociation", Group.ALCHEMY))
            .entry("knowledge_spirit", IlluminatorFx.WHEEL_KNOWLEDGE_SPIRIT, "Knowledge Spirit", SPIRIT_SEQUENCE,
                    SPIRIT_COST, SPIRIT_COOLDOWN, IlluminatorAlchemy::knowledgeSpirit)
            .costBy((player, cost) -> Illuminator.costFor(player, cost, "knowledge_spirit", Group.ALCHEMY))
            .onCast((player, sl, entry) -> Illuminator.afterCast(player, entry.key()));

    // =========================
    // Ability 6 - Alchemy and Arcana
    // Normal: the wheel. Sneak: the last pick again, without the wheel.
    // =========================
    public static void illuminatorAlchemy(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!Illuminator.ready(player, bypassClassCheck, ALCHEMY_SEQUENCE)) return;
        if (player.isShiftKeyDown()) WHEEL.quickCast(player);
        else WHEEL.open(player);
    }

    // =========================
    // Living Doll: a doll of what you stand on rises beside you (at IlluminatorFx.DOLL_STANDS) and fights for you for
    // DOLL_TICKS (Illuminator.lasting): it goes after what you hit (status/Marionette). Stone, deepslate, ores: Stone
    // Doll, 80 health, slow, no knockback. Wood, leaves, grass, dirt and anything else: Straw Doll, 30, fast, its fall
    // shoves everything within STRAW_SHOVE. Iron, copper, gold, rails, chains: Iron Doll, 50, throws a bolt at its
    // enemy every BOLT_EVERY. Sand, gravel, clay: Dust Doll, 20, its hits blind. Snow, ice: Frost Doll, 30, its hits
    // slow. Arcane Scholar (3): two, the second a decoy that looks like you (your armor and what you hold, no doll's
    // body) that the mobs near go for first; Knowledge Magister (2): three. A new cast replaces the old dolls.
    // =========================
    private static AbilityWheel.Result livingDoll(ServerPlayer player, ServerLevel sl) {
        Material material = materialUnder(sl, player.position());
        int count = Beyonder.hasSequence(player, THREE_DOLLS_SEQUENCE) ? 3 : Beyonder.hasSequence(player, TWO_DOLLS_SEQUENCE) ? 2 : 1;
        int ticks = Illuminator.lasting(player, DOLL_TICKS, Group.ALCHEMY);
        Vec3 look = Spells.flatLook(player);
        for (int i = 0; i < count; i++) {
            boolean decoy = i == 1;
            Vec3 side = Spells.turned(look, (i - (count - 1) / 2.0) * 50.0);
            Vec3 spot = Spells.freeSpotNear(player, sl, player.position(), side, 1.6);
            Vec3 at = spot != null ? spot : player.position();
            float yaw = player.getYRot();
            if (!decoy) IlluminatorFx.dollRise(sl, at, yaw, material);
            Later.run(sl, IlluminatorFx.DOLL_STANDS, () -> {
                if (Spells.casterStillHere(player, sl)) raiseDoll(player, sl, at, yaw, material, ticks, count, decoy);
            });
        }
        return AbilityWheel.Result.DONE;
    }

    private static void raiseDoll(Player player, ServerLevel sl, Vec3 at, float yaw, Material material, int ticks, int count, boolean decoy) {
        String tag = "il_doll_" + player.getId() + "_" + sl.getGameTime() + "_" + (decoy ? "d" : material.name());
        Construct body = Construct.of(EntityTypes.HUSK)
                .health(dollHealth(material)).damage(DOLL_DAMAGE).speed(dollSpeed(material)).scale(DOLL_SCALE)
                .lasts(ticks).leash(DOLL_LEASH).limit(DOLLS, count)
                .onEnd((doll, why) -> dollEnds(player, sl, doll));
        if (material == Material.STONE) body = body.steady();
        if (decoy) body = body.shown();
        Mob doll = body.build(player, sl, at, yaw);
        if (doll == null) return;
        DOLL_BODIES.put(doll.getUUID(), new Doll(material, tag, decoy));
        if (decoy) {
            for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
                    EquipmentSlot.FEET, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND}) {
                ItemStack worn = player.getItemBySlot(slot);
                if (!worn.isEmpty()) doll.setItemSlot(slot, worn.copy());
            }
            IlluminatorFx.recall(sl, doll, player.position(), at);                // a copy of you steps out of you
            tauntToDecoy(player, sl, doll);
            return;
        }
        IlluminatorFx.dollBody(sl, doll, material, ticks, tag);
    }

    private static float dollHealth(Material material) {
        return switch (material) {
            case STONE -> 80f;
            case IRON -> 50f;
            case DUST -> 20f;
            case STRAW, FROST -> 30f;
        };
    }

    private static float dollSpeed(Material material) {
        return switch (material) {
            case STONE -> 0.7f;
            case STRAW -> 1.4f;
            case DUST -> 1.2f;
            case IRON, FROST -> 1.0f;
        };
    }

    /** What a doll is made of: the block under the feet. */
    static Material materialUnder(ServerLevel sl, Vec3 feet) {
        BlockState state = sl.getBlockState(BlockPos.containing(feet.x, feet.y - 0.2, feet.z));
        String name = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        if (state.is(BlockTags.ICE) || name.contains("snow") || name.contains("ice")) return Material.FROST;
        if (state.is(BlockTags.SAND) || name.contains("sand") || name.contains("gravel") || name.contains("clay")) return Material.DUST;
        if (name.contains("iron") || name.contains("copper") || name.contains("gold") || name.contains("rail")
                || name.contains("chain") || name.contains("anvil") || name.contains("netherite")) return Material.IRON;
        if (state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.BASE_STONE_NETHER) || name.contains("stone")
                || name.contains("deepslate") || name.contains("ore") || name.contains("brick") || name.contains("cobble")) {
            return Material.STONE;
        }
        return Material.STRAW;                                                     // wood, leaves, grass, dirt and the rest
    }

    /** A doll fell (dead, out of time, replaced, its maker too far): it crumbles; a Straw Doll shoves as it falls. */
    private static void dollEnds(Player player, ServerLevel sl, LivingEntity doll) {
        Doll made = DOLL_BODIES.remove(doll.getUUID());
        if (made == null) return;
        if (made.decoy()) {
            IlluminatorFx.constructTakenApart(sl, doll, false);
            return;
        }
        IlluminatorFx.dollFall(sl, doll, made.material(), made.tag());
        if (made.material() != Material.STRAW || !Spells.casterStillHere(player, sl)) return;
        for (LivingEntity enemy : Spells.enemiesAround(player, sl, doll.position(), STRAW_SHOVE)) {
            Spells.push(enemy, doll.position(), 0.8, 0.3);
        }
    }

    /** The mobs near the decoy go for it. */
    private static void tauntToDecoy(Player player, ServerLevel sl, LivingEntity decoy) {
        for (LivingEntity enemy : Spells.enemiesAround(player, sl, decoy.position(), DECOY_TAUNT)) {
            if (enemy instanceof Mob) Taunt.apply(enemy, decoy, DECOY_TAUNT_EVERY + 20);
        }
    }

    // =========================
    // Golem: brass and iron fly together for 4 seconds in front of you (IlluminatorFx.golemAssemble, silent) into a
    // golem (an iron golem's body under the model): GOLEM_HEALTH, GOLEM_DAMAGE a blow, for GOLEM_TICKS (Illuminator
    // .lasting). Every GOLEM_TAUNT_EVERY it calls the mobs within GOLEM_TAUNT to it (status/Taunt). Gone when you are
    // further than GOLEM_LEASH. One at a time (a new one replaces the oldest); Arcane Scholar (3): two; Paragon (0):
    // no timer.
    // =========================
    private static AbilityWheel.Result golem(ServerPlayer player, ServerLevel sl) {
        Vec3 spot = Spells.freeSpotNear(player, sl, player.position(), Spells.flatLook(player), 3.0);
        Vec3 at = spot != null ? spot : player.position();
        float yaw = player.getYRot();
        boolean endless = Beyonder.hasSequence(player, ENDLESS_GOLEM_SEQUENCE);
        int ticks = endless ? 0 : Illuminator.lasting(player, GOLEM_TICKS, Group.ALCHEMY);
        int count = Beyonder.hasSequence(player, TWO_GOLEMS_SEQUENCE) ? 2 : 1;
        IlluminatorFx.golemAssemble(sl, at, yaw);
        Later.run(sl, IlluminatorFx.GOLEM_STANDS, () -> {
            if (!Spells.casterStillHere(player, sl)) return;
            Mob golem = Construct.of(EntityTypes.IRON_GOLEM)
                    .health(GOLEM_HEALTH).damage(GOLEM_DAMAGE).steady()
                    .lasts(ticks).leash(GOLEM_LEASH).limit(GOLEMS, count)
                    .onEnd((made, why) -> {
                        String tag = GOLEM_BODIES.remove(made.getUUID());
                        if (tag != null) IlluminatorFx.golemEnd(sl, made, tag);
                    })
                    .build(player, sl, at, yaw);
            if (golem == null) {
                player.sendSystemMessage(Component.literal("There is no room for the golem to stand."));
                return;
            }
            String tag = "il_golem_" + golem.getId();
            GOLEM_BODIES.put(golem.getUUID(), tag);
            IlluminatorFx.golemBody(sl, golem, ticks > 0 ? ticks : IlluminatorFx.FOREVER, tag);
            golemCalls(player, sl, golem);
        });
        return AbilityWheel.Result.DONE;
    }

    /** The golem calls the mobs near to it. */
    private static void golemCalls(Player player, ServerLevel sl, LivingEntity golem) {
        IlluminatorFx.golemTaunt(sl, golem, GOLEM_TAUNT);
        for (LivingEntity enemy : Spells.enemiesAround(player, sl, golem.position(), GOLEM_TAUNT)) {
            if (enemy instanceof Mob) Taunt.apply(enemy, golem, GOLEM_TAUNT_EVERY + 20);
        }
    }

    // =========================
    // Mechanical Body: plates clamp on for MECH_TICKS (Illuminator.lasting; Illuminator (1): no timer). Resistance I,
    // MECH_ABSORPTION absorption (status/ConjuredArmor),
    // Night Vision, cybernetic eyes (Survey and Appraise reach half again: Illuminator.wearsMechanicalBody), the
    // Clockwork Rifle through the arm cannon (IlluminatorWorkshop), and a jetpack: jump twice to take off as in
    // creative and fly for JET_FUEL ticks, refilled on the ground, with no fall damage. Cast again: it comes off
    // (free). Knowledge Magister (2): a sneak recast while it is worn turns you to white steam for STEAM_TICKS instead
    // (hidden, untouchable, fast), and you re-form where you aim then (STEAM_RANGE).
    // =========================
    private static AbilityWheel.Result mechanicalBody(ServerPlayer player, ServerLevel sl) {
        if (Illuminator.wearsMechanicalBody(player)) {
            if (player.isShiftKeyDown() && Beyonder.hasSequence(player, STEAM_SEQUENCE)) {
                if (Illuminator.isActive(player, STEAM_UNTIL)) return AbilityWheel.Result.REFUSED;
                steamForm(player, sl);
                return AbilityWheel.Result.FREE;
            }
            ConjuredArmor.takeOff(player, MECH_ARMOR);                            // its onEnd takes the rest off
            return AbilityWheel.Result.FREE;
        }
        boolean endless = Beyonder.hasSequence(player, ENDLESS_MECH_SEQUENCE);
        int ticks = endless ? 0 : Illuminator.lasting(player, MECH_TICKS, Group.ALCHEMY);
        String tag = mechTag(player);
        player.getPersistentData().putLong(Illuminator.MECH_UNTIL, endless ? Long.MAX_VALUE : sl.getGameTime() + ticks);
        player.getPersistentData().putInt(JET_FUEL_LEFT, JET_FUEL);
        IlluminatorFx.mechanicalBody(sl, player, endless ? IlluminatorFx.FOREVER : ticks, tag);
        if (endless) IlluminatorFx.statusForever(player, Status.MECHANICAL_BODY);
        else IlluminatorFx.status(player, Status.MECHANICAL_BODY, ticks);
        ConjuredArmor.of(MECH_ARMOR)
                .lasts(ticks)
                .absorption(MECH_ABSORPTION)
                .onEnd(wearer -> {
                    if (wearer instanceof Player worn) bodyComesOff(worn);
                })
                .wear(player);
        return AbilityWheel.Result.DONE;
    }

    private static String mechTag(Player player) {
        return "il_mech_" + player.getId();
    }

    /** The plates come off: the flight, the effects and the look go with them. */
    private static void bodyComesOff(Player player) {
        if (!player.getPersistentData().contains(Illuminator.MECH_UNTIL)) return;
        player.getPersistentData().remove(Illuminator.MECH_UNTIL);
        player.getPersistentData().remove(JET_FUEL_LEFT);
        stopJet(player);
        player.removeEffect(MobEffects.RESISTANCE);
        IlluminatorFx.statusEnd(player, Status.MECHANICAL_BODY);
        if (player.level() instanceof ServerLevel sl) IlluminatorFx.mechanicalBodyEnd(sl, player, mechTag(player));
    }

    /** Every tick while the body is worn: its effects, and the jetpack. */
    private static void bodyTick(Player player, ServerLevel sl) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(Illuminator.MECH_UNTIL)) return;
        if (!Illuminator.wearsMechanicalBody(player) || !ConjuredArmor.isWorn(player, MECH_ARMOR)) {
            ConjuredArmor.takeOff(player, MECH_ARMOR);
            bodyComesOff(player);
            return;
        }
        if (player.tickCount % 20 == 0) {
            Spells.keepEffect(player, MobEffects.RESISTANCE, 0, 60);
            Spells.keepEffect(player, MobEffects.NIGHT_VISION, 0, 600);
        }
        if (player.isCreative() || player.isSpectator() || Flight.has(player)) return;   // they fly anyway
        int fuel = data.getIntOr(JET_FUEL_LEFT, JET_FUEL);
        if (player.onGround()) {
            if (fuel < JET_FUEL) data.putInt(JET_FUEL_LEFT, JET_FUEL);
            if (!player.getAbilities().mayfly) {
                player.getAbilities().mayfly = true;
                player.onUpdateAbilities();
            }
            return;
        }
        if (!player.getAbilities().flying) return;
        FallGuard.protect(player, 60);
        if (player.tickCount % 2 == 0) IlluminatorFx.jet(sl, player);
        fuel--;
        data.putInt(JET_FUEL_LEFT, fuel);
        if (fuel <= 0) stopJet(player);
    }

    /** The jetpack is out (or the body is off): no more flying until the ground. */
    private static void stopJet(Player player) {
        if (player.isCreative() || player.isSpectator() || Flight.has(player)) return;
        if (!player.getAbilities().mayfly && !player.getAbilities().flying) return;
        player.getAbilities().flying = false;
        player.getAbilities().mayfly = false;
        player.onUpdateAbilities();
        FallGuard.protect(player, 200);
    }

    private static void steamForm(Player player, ServerLevel sl) {
        long until = sl.getGameTime() + STEAM_TICKS;
        player.getPersistentData().putLong(STEAM_UNTIL, until);
        String tag = mechTag(player) + "_steam";
        Concealment.hide(player, STEAM_TICKS + 2);
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, STEAM_TICKS, 2, false, false));
        IlluminatorFx.steamForm(sl, player, STEAM_TICKS, tag);
        IlluminatorFx.status(player, Status.STEAM_FORM, STEAM_TICKS);
        for (int t = 2; t < STEAM_TICKS; t += 2) {
            Later.run(sl, t, () -> {
                if (Spells.casterStillHere(player, sl) && player.getPersistentData().getLongOr(STEAM_UNTIL, 0L) == until) {
                    IlluminatorFx.steamTrail(sl, player);
                }
            });
        }
        Later.run(sl, STEAM_TICKS, () -> {
            if (!Spells.casterStillHere(player, sl) || player.getPersistentData().getLongOr(STEAM_UNTIL, 0L) != until) return;
            player.getPersistentData().remove(STEAM_UNTIL);
            Vec3 there = Root.has(player, RootRestriction.TELEPORT) ? null : Spells.blinkSpot(player, sl, STEAM_RANGE);
            if (there != null) {
                player.teleportTo(there.x, there.y, there.z);
                player.fallDistance = 0;
                FallGuard.protect(player, 60);
            }
            Concealment.reveal(player);
            IlluminatorFx.steamReform(sl, player, tag);
        });
    }

    // =========================
    // Dissociation: a clear, misty beam from your eye, DISSOCIATION_RANGE long and DISSOCIATION_WIDTH wide
    // (WIDE_BEAM_WIDTH from Magister), through blocks (they flash and shed fragments, unchanged). Everything not on your
    // side that it touches, the hidden too: its concealment, unnoticing and Substitute end, a raised shield drops, each
    // armor piece loses ARMOR_WEAR of its durability, and DISSOCIATION_DAMAGE (a corrosion hit). A construct under
    // CONSTRUCT_FRAGILE maximum health is dissolved. Paragon (0), the Information Shredder: every mark on it is wiped
    // (status/Appraisal), it cannot hide (status/Exposed), and a player forgets everything they had studied
    // (status/Study).
    // =========================
    private static AbilityWheel.Result dissociation(ServerPlayer player, ServerLevel sl) {
        double width = Beyonder.hasSequence(player, WIDE_BEAM_SEQUENCE) ? WIDE_BEAM_WIDTH : DISSOCIATION_WIDTH;
        boolean shredder = Beyonder.hasSequence(player, SHREDDER_SEQUENCE);
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().normalize().scale(DISSOCIATION_RANGE));
        IlluminatorFx.dissociation(sl, player, eye, end, width);
        for (Vec3 block : blocksCrossed(sl, eye, end)) IlluminatorFx.blockFades(sl, block);
        for (LivingEntity touched : touchedBy(player, sl, eye, end, width / 2.0)) {
            Concealment.reveal(touched);
            Unnoticed.end(touched);
            if (Substitute.isArmed(touched)) Substitute.disarm(touched);
            touched.stopUsingItem();
            wearArmor(touched);
            IlluminatorFx.dissolved(sl, touched);
            if (shredder) {
                Appraisal.wipe(touched);
                Exposed.apply(touched, SHREDDED_EXPOSED);
                if (touched instanceof Player reader) Study.forgetAll(reader);
            }
            if (Construct.isConstruct(touched) && touched.getMaxHealth() < CONSTRUCT_FRAGILE) {
                IlluminatorFx.constructTakenApart(sl, touched, true);
                Appraisal.strikeTrue(player, touched, touched.getHealth() + 1000f, Illuminator.kind(Weakness.CORROSION));
                continue;
            }
            Illuminator.hit(player, touched, DISSOCIATION_DAMAGE, Weakness.CORROSION);
        }
        return AbilityWheel.Result.DONE;
    }

    /** Everything alive and not on the caster's side whose body (grown by 'grow') the line passes, the hidden too. */
    private static List<LivingEntity> touchedBy(Player player, ServerLevel sl, Vec3 from, Vec3 to, double grow) {
        List<LivingEntity> found = new ArrayList<>();
        AABB area = new AABB(from, to).inflate(grow + 0.5);
        for (LivingEntity one : sl.getEntitiesOfClass(LivingEntity.class, area, other -> other.isAlive()
                && !other.isSpectator() && !Spells.isFriend(player, other))) {
            AABB body = one.getBoundingBox().inflate(grow);
            if (body.contains(from) || body.clip(from, to).isPresent()) found.add(one);
        }
        return found;
    }

    /** The middles of the solid blocks a line crosses (32 at most). */
    private static List<Vec3> blocksCrossed(ServerLevel sl, Vec3 from, Vec3 to) {
        Set<BlockPos> crossed = new LinkedHashSet<>();
        Vec3 way = to.subtract(from);
        double length = way.length();
        Vec3 step = way.normalize().scale(0.4);
        Vec3 at = from;
        for (double d = 0; d < length && crossed.size() < 32; d += 0.4) {
            BlockPos pos = BlockPos.containing(at.x, at.y, at.z);
            if (!sl.getBlockState(pos).isAir()) crossed.add(pos.immutable());
            at = at.add(step);
        }
        List<Vec3> middles = new ArrayList<>();
        for (BlockPos pos : crossed) middles.add(new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
        return middles;
    }

    /** Each armor piece loses ARMOR_WEAR of its durability. */
    private static void wearArmor(LivingEntity target) {
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack worn = target.getItemBySlot(slot);
            if (worn.isEmpty() || !worn.isDamageableItem()) continue;
            worn.hurtAndBreak(Math.max(1, Math.round(worn.getMaxDamage() * ARMOR_WEAR)), target, slot);
        }
    }

    // =========================
    // Knowledge Spirit: you turn into drifting pages (hidden, untouchable: status/Concealment, and fast) and fly to the
    // enemy you look at (SPIRIT_RANGE) at SPIRIT_SPEED, 4 seconds at most; you enter its mind and step out beside it,
    // hidden SPIRIT_AFTER more. It is Eroded for ERODED_TICKS: nausea, it deals ERODED_WEAKER less (status/Enfeebled) and
    // every ability costs it ERODED_COST times as much (Spells.addCostScale, erodedCost). With no enemy in view you step
    // out where you aim instead (SPIRIT_JUMP), like a jump. Not while held in place.
    // =========================
    private static AbilityWheel.Result knowledgeSpirit(ServerPlayer player, ServerLevel sl) {
        if (Root.has(player, RootRestriction.MOVE) || Root.has(player, RootRestriction.TELEPORT)) {
            player.sendSystemMessage(Component.literal("You are held fast and cannot leave your body."));
            return AbilityWheel.Result.REFUSED;
        }
        LivingEntity target = Spells.aimEnemy(player, sl, SPIRIT_RANGE);
        Vec3 jump = target == null ? Spells.blinkSpot(player, sl, SPIRIT_JUMP) : null;
        if (target == null && jump == null) {
            player.sendSystemMessage(Component.literal("There is no mind in view and nowhere to go."));
            return AbilityWheel.Result.REFUSED;
        }
        Vec3 from = player.position();
        Vec3 aim = target != null ? target.position() : jump;
        int flight = (int) Math.max(6, Math.min(SPIRIT_MOST_FLIGHT, Math.ceil(from.distanceTo(aim) / SPIRIT_SPEED)));
        Concealment.hide(player, flight + SPIRIT_AFTER);
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, flight + SPIRIT_AFTER, 2, false, false));
        IlluminatorFx.knowledgeSpirit(sl, player, from, aim, flight);
        IlluminatorFx.status(player, Status.SPIRIT_FORM, flight + SPIRIT_AFTER);
        Later.run(sl, flight, () -> {
            if (!Spells.casterStillHere(player, sl)) return;
            Vec3 there = jump;
            if (target != null && target.isAlive() && target.level() == sl) {
                Vec3 away = player.position().subtract(target.position());
                Vec3 side = new Vec3(away.x, 0, away.z).lengthSqr() > 1.0E-4 ? new Vec3(away.x, 0, away.z).normalize() : Spells.flatLook(player).scale(-1);
                there = Spells.freeSpotNear(player, sl, target.position(), side, 1.2 + target.getBbWidth() * 0.5);
                IlluminatorFx.mindEntered(sl, target);
                erode(sl, target);
            }
            if (there != null) {
                player.teleportTo(there.x, there.y, there.z);
                player.fallDistance = 0;
                FallGuard.protect(player, 60);
            }
            IlluminatorFx.spiritStepOut(sl, player);
        });
        return AbilityWheel.Result.DONE;
    }

    /** Eroded: nausea, weaker, and dearer abilities for ERODED_TICKS. */
    static void erode(ServerLevel sl, LivingEntity target) {
        target.addEffect(new MobEffectInstance(MobEffects.NAUSEA, ERODED_TICKS, 0));
        Enfeebled.apply(target, ERODED, ERODED_WEAKER, ERODED_TICKS);
        target.getPersistentData().putLong(ERODED_UNTIL, sl.getGameTime() + ERODED_TICKS);
        IlluminatorFx.eroded(sl, target, ERODED_TICKS, "il_eroded_" + target.getId());
        if (target instanceof Player eroded) IlluminatorFx.status(eroded, Status.ERODED, ERODED_TICKS);
    }

    /** What every ability costs an Eroded player (Spells.addCostScale). */
    static float erodedCost(Player player) {
        return Illuminator.isActive(player, ERODED_UNTIL) ? ERODED_COST : 1f;
    }

    // =========================
    // Kept going and tidy by Illuminator
    // =========================

    /** Every tick, for every player: the Mechanical Body; every so often the dolls' bolts, the decoy's and golems' calls. */
    static void tick(Player player, ServerLevel sl) {
        bodyTick(player, sl);
        if (DOLL_BODIES.isEmpty() && GOLEM_BODIES.isEmpty()) return;
        boolean bolts = player.tickCount % BOLT_EVERY == 0;
        boolean decoys = player.tickCount % DECOY_TAUNT_EVERY == 0;
        boolean golems = player.tickCount % GOLEM_TAUNT_EVERY == 0;
        if (!bolts && !decoys && !golems) return;
        for (Mob made : Construct.ofOwner(player, null)) {
            if (made.level() != sl) continue;
            Doll doll = DOLL_BODIES.get(made.getUUID());
            if (doll != null && doll.decoy() && decoys) tauntToDecoy(player, sl, made);
            if (doll != null && doll.material() == Material.IRON && bolts) dollBolt(player, sl, made);
            if (golems && GOLEM_BODIES.containsKey(made.getUUID())) golemCalls(player, sl, made);
        }
    }

    /** The Iron Doll throws a bolt at what it fights. */
    private static void dollBolt(Player player, ServerLevel sl, Mob doll) {
        LivingEntity target = doll.getTarget();
        if (target == null || !target.isAlive() || target.distanceTo(doll) > BOLT_RANGE || !Spells.isEnemy(player, target)) return;
        IlluminatorFx.dollBolt(sl, doll, target.getBoundingBox().getCenter());
        Illuminator.hit(player, target, BOLT_DAMAGE, null);
    }

    /** A hit landed (LivingHurtEvent): a golem's blow is shown; a Dust Doll's blinds, a Frost Doll's slows. */
    static void hurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof Mob made) || made != event.getSource().getDirectEntity()) return;
        if (!(made.level() instanceof ServerLevel sl)) return;
        LivingEntity target = event.getEntity();
        String golemTag = GOLEM_BODIES.get(made.getUUID());
        if (golemTag != null) {
            int left = Construct.ticksLeft(made);
            IlluminatorFx.golemStrike(sl, made, left == Integer.MAX_VALUE ? IlluminatorFx.FOREVER : left, golemTag);
            return;
        }
        Doll doll = DOLL_BODIES.get(made.getUUID());
        if (doll == null) return;
        if (doll.material() == Material.DUST) Blind.apply(target, DUST_BLIND);
        else if (doll.material() == Material.FROST) Spells.keepEffect(target, MobEffects.SLOWNESS, 1, FROST_SLOW);
    }

    /** After a relog or a death: the body is off and no flight is left over. */
    static void clearState(Player player) {
        ConjuredArmor.takeOff(player, MECH_ARMOR);
        player.getPersistentData().remove(Illuminator.MECH_UNTIL);
        player.getPersistentData().remove(JET_FUEL_LEFT);
        player.getPersistentData().remove(STEAM_UNTIL);
        if (!player.isCreative() && !player.isSpectator() && !Flight.has(player) && player.getAbilities().mayfly
                && Illuminator.PATHWAY.equals(SoulCore.getPathway(player))) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
    }
}
