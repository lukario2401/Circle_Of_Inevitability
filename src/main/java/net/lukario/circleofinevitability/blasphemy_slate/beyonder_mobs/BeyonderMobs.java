package net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonder;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonders;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Physique;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.SequenceCommands;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.DarkAngel;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.PathwayAbilities;
import net.lukario.circleofinevitability.combat.Casts;
import net.lukario.circleofinevitability.item.BeyonderCharacteristicItem;
import net.lukario.circleofinevitability.status.Marionette;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.listener.Priority;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Beyonder mobs (docs/BEYONDER_MOBS.md): a mob that carries a Beyonder Characteristic of one Pathway and Sequence.
 * When it dies, the characteristic drops (item/BeyonderCharacteristicItem). Abilities that steal or copy powers from
 * a mob (Worm Of Time's Theft, Dark Angel's Grazing, Key Of Stars' Replication and Record) only find something in
 * these, and only from their Pathway, up to their Sequence: ask Beyonders (blasphemy_slate/beyonder) for that.
 *
 * Which mobs, how often, which Pathway and Sequence, and how much stronger they get: BeyonderMobRules. Here is the
 * state and what happens to them:
 *
 *   BeyonderMobs.isBeyonder(mob)                       // true for a Beyonder mob (never for a player)
 *   BeyonderMobs.pathway(mob) / sequence(mob)          // null / -1 when it is none
 *   BeyonderMobs.make(mob, "Hand Of Order", 7)         // makes it one, whatever its Rule says (commands, abilities)
 *   BeyonderMobs.summon(sl, type, at, "Star Dragon", 3) // spawns a new mob that already is one
 *   BeyonderMobs.clear(mob)                            // a mob again
 *   BeyonderMobs.roll(mob)                             // rolls for it by its Rule (each mob is rolled once by itself)
 *
 * Every mob is rolled once, on the first tick it is seen (so a summon is already marked as one and is skipped). What
 * it is is saved with the mob (persistent data); its extra health and damage are permanent attribute modifiers, so
 * they are saved too.
 *
 * A Beyonder mob does not cast its Pathway's abilities (yet). When that comes, this is the place to give it a brain.
 */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID)
public final class BeyonderMobs {

    private BeyonderMobs() {}

    // what it is: a compound with "pathway", "sequence" and "named" (true if we gave it its name)
    private static final String TAG = "circle_of_inevitability_beyonder_mob";
    // true once it was rolled for (whatever came of it)
    private static final String ROLLED = "circle_of_inevitability_beyonder_rolled";
    private static final String PATHWAY = "pathway";
    private static final String SEQUENCE = "sequence";
    private static final String NAMED = "named";

    private static final Identifier HEALTH_ID = Physique.id("beyonder_mob_health");
    private static final Identifier DAMAGE_ID = Physique.id("beyonder_mob_damage");

    /** How far down a dropped characteristic looks for ground; with none (the void), it drops at the killer's feet. */
    private static final int GROUND_SEARCH = 64;

    // =========================
    // Asking
    // =========================

    /** True for a mob that carries a characteristic. Never for a player (they have SoulCore). */
    public static boolean isBeyonder(LivingEntity entity) {
        return entity != null && !(entity instanceof Player) && entity.getPersistentData().contains(TAG);
    }

    /** Its Pathway, or null if it is no Beyonder. */
    public static String pathway(LivingEntity entity) {
        if (!isBeyonder(entity)) return null;
        String pathway = data(entity).getStringOr(PATHWAY, "");
        return pathway.isEmpty() ? null : pathway;
    }

    /** Its Sequence (9 .. 0), or -1 if it is no Beyonder. */
    public static int sequence(LivingEntity entity) {
        if (!isBeyonder(entity)) return -1;
        int sequence = data(entity).getIntOr(SEQUENCE, Beyonder.WEAKEST_SEQUENCE);
        return Math.max(Beyonder.STRONGEST_SEQUENCE, Math.min(Beyonder.WEAKEST_SEQUENCE, sequence));
    }

    /**
     * For the abilities that copy a cast (Replication, Record): one of its Pathway's abilities that can be copied, up
     * to its Sequence, as a cast it just made (combat/Casts.madeUpFor), sneak form or not at random. Null if it is no
     * Beyonder mob or has nothing that can be copied.
     */
    public static Casts.Cast castOf(LivingEntity mob, ServerLevel sl, RandomSource random) {
        if (!isBeyonder(mob)) return null;
        PathwayAbilities.Ability ability = Beyonders.randomTakeable(mob, random, null);
        return ability == null ? null : Casts.madeUpFor(mob, sl, ability, random.nextBoolean());
    }

    // =========================
    // Making and unmaking
    // =========================

    /**
     * Makes it a Beyonder of that Pathway and Sequence: the extra health and damage of that Sequence (from its Rule, or
     * else the first Rule that has that Sequence), healed to full, and its name and aura. A player, or a Pathway that
     * does not exist, is refused (false).
     */
    public static boolean make(LivingEntity entity, String pathway, int sequence) {
        if (entity == null || entity instanceof Player || !SequenceCommands.PATHWAYS.contains(pathway)) return false;
        int clamped = Math.max(Beyonder.STRONGEST_SEQUENCE, Math.min(Beyonder.WEAKEST_SEQUENCE, sequence));
        boolean namedBefore = isBeyonder(entity) && data(entity).getBooleanOr(NAMED, false);
        // a name of its own (a name tag, a named spawn) is kept; ours is written again for the new Pathway or Sequence
        boolean name = BeyonderMobRules.SHOW_NAME && (namedBefore || !entity.hasCustomName());

        CompoundTag tag = new CompoundTag();
        tag.putString(PATHWAY, pathway);
        tag.putInt(SEQUENCE, clamped);
        tag.putBoolean(NAMED, name);
        entity.getPersistentData().put(TAG, tag);
        entity.getPersistentData().putBoolean(ROLLED, true);

        BeyonderMobRules.Rule.Tier tier = tierFor(entity, clamped);
        boost(entity, Attributes.MAX_HEALTH, HEALTH_ID, tier == null ? 0 : tier.extraHealth());
        boost(entity, Attributes.ATTACK_DAMAGE, DAMAGE_ID, tier == null ? 0 : tier.extraDamage());
        entity.setHealth(entity.getMaxHealth());

        if (name) {
            entity.setCustomName(nameFor(entity, pathway, clamped));
            entity.setCustomNameVisible(BeyonderMobRules.NAME_ALWAYS_VISIBLE);
        } else if (namedBefore) {                                          // names were switched off since
            entity.setCustomName(null);
            entity.setCustomNameVisible(false);
        }
        return true;
    }

    /**
     * Summons a 'type' at 'at' that is already a Beyonder of that Pathway and Sequence (/beyondermob summon; a ritual,
     * a structure or a boss fight can call it too). It spawns the way /summon spawns it (a skeleton gets its bow),
     * then make() runs before its first tick, so it is never rolled. Any living mob can be one this way, hostile or
     * not, at any Sequence 9 to 0. Null if 'type' is no living mob (or a player), the Pathway does not exist, or it
     * could not spawn there.
     */
    public static LivingEntity summon(ServerLevel sl, EntityType<?> type, Vec3 at, String pathway, int sequence) {
        if (sl == null || type == null || at == null || !SequenceCommands.PATHWAYS.contains(pathway)) return null;
        Entity spawned = type.spawn(sl, BlockPos.containing(at), EntitySpawnReason.COMMAND);
        if (spawned == null) return null;
        if (!(spawned instanceof LivingEntity living) || spawned instanceof Player) {
            spawned.discard();
            return null;
        }
        living.snapTo(at.x, at.y, at.z, living.getYRot(), living.getXRot());       // exactly there, not the block's middle
        if (!make(living, pathway, sequence)) {
            living.discard();
            return null;
        }
        return living;
    }

    /** It is no Beyonder any more: no characteristic, no extra health or damage, and the name we gave it is gone. */
    public static void clear(LivingEntity entity) {
        if (!isBeyonder(entity)) return;
        boolean named = data(entity).getBooleanOr(NAMED, false);
        entity.getPersistentData().remove(TAG);
        boost(entity, Attributes.MAX_HEALTH, HEALTH_ID, 0);
        boost(entity, Attributes.ATTACK_DAMAGE, DAMAGE_ID, 0);
        if (named) {
            entity.setCustomName(null);
            entity.setCustomNameVisible(false);
        }
    }

    /**
     * Rolls for it by its Rule (BeyonderMobRules): the chance, then a Pathway and a Sequence by the weights. It is
     * marked as rolled either way. True if it became a Beyonder. A mob that already is one is left as it is.
     */
    public static boolean roll(LivingEntity entity) {
        if (entity == null || entity instanceof Player || isBeyonder(entity)) return false;
        entity.getPersistentData().putBoolean(ROLLED, true);
        if (BeyonderMobRules.SUMMONS_NEVER && Marionette.isSummoned(entity)) return false;
        BeyonderMobRules.Rule rule = BeyonderMobRules.ruleFor(entity);
        if (rule == null || rule.chance() <= 0) return false;
        if (entity.getRandom().nextDouble() >= rule.chance()) return false;
        String pathway = rule.rollPathway(entity.getRandom());
        int sequence = rule.rollSequence(entity.getRandom());
        if (pathway == null || sequence < 0) return false;
        return make(entity, pathway, sequence);
    }

    /** Clears it and rolls for it again (the command). True if it is a Beyonder now. */
    public static boolean reroll(LivingEntity entity) {
        clear(entity);
        return roll(entity);
    }

    /** The extra health and damage of that Sequence for this mob: its own Rule's, else HOSTILE's, else BOSS's. */
    private static BeyonderMobRules.Rule.Tier tierFor(LivingEntity entity, int sequence) {
        BeyonderMobRules.Rule own = BeyonderMobRules.ruleFor(entity);
        BeyonderMobRules.Rule.Tier tier = own == null ? null : own.tier(sequence);
        if (tier == null) tier = BeyonderMobRules.HOSTILE.tier(sequence);
        if (tier == null) tier = BeyonderMobRules.BOSS.tier(sequence);
        return tier;
    }

    /** "Zombie (Hand Of Order, Sequence 7)", in its Pathway's color. */
    private static Component nameFor(LivingEntity entity, String pathway, int sequence) {
        int rgb = DarkAngel.pathwayColor(pathway) & 0xFFFFFF;
        return Component.translatable("entity.circle_of_inevitability.beyonder_mob",
                        entity.getType().getDescription(), pathway, sequence)
                .withStyle(style -> style.withColor(TextColor.fromRgb(rgb)));
    }

    /** A permanent (saved) modifier of +amount times the base value; 0 takes it off. */
    private static void boost(LivingEntity entity, Holder<Attribute> attribute, Identifier id, double amount) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance == null) return;                                   // (the dragon has no attack damage)
        if (instance.hasModifier(id)) instance.removeModifier(id);
        if (amount != 0) instance.addPermanentModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        if (entity.getHealth() > entity.getMaxHealth()) entity.setHealth(entity.getMaxHealth());
    }

    private static CompoundTag data(LivingEntity entity) {
        return entity.getPersistentData().getCompoundOrEmpty(TAG);
    }

    // =========================
    // The characteristic drops
    // =========================

    /** Drops its characteristic where it fell (at the killer's feet if it fell over the void). It is a mob again. */
    private static void dropCharacteristic(ServerLevel sl, LivingEntity body, Entity killer) {
        String pathway = pathway(body);
        int sequence = sequence(body);
        body.getPersistentData().remove(TAG);                           // one characteristic per body, never two
        if (pathway == null) return;

        ItemStack stack = BeyonderCharacteristicItem.of(pathway, sequence);
        Vec3 at = body.position().add(0, Math.min(1.0, body.getBbHeight() * 0.5), 0);
        if (!hasGroundBelow(sl, body.blockPosition()) && killer != null && killer.level() == sl) {
            at = killer.position().add(0, 0.5, 0);
        }
        ItemEntity item = new ItemEntity(sl, at.x, at.y, at.z, stack);
        item.setPickUpDelay(10);
        sl.addFreshEntity(item);
    }

    private static boolean hasGroundBelow(ServerLevel sl, BlockPos from) {
        BlockPos pos = from;
        for (int i = 0; i < GROUND_SEARCH; i++) {
            if (!sl.getBlockState(pos).isAir()) return true;
            pos = pos.below();
        }
        return false;
    }

    // =========================
    // Events
    // =========================

    /** The first tick of every mob: it is rolled for. Then, for a Beyonder: its faint aura. */
    @SubscribeEvent
    public static void onBeyonderMobTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Mob) || !(entity.level() instanceof ServerLevel sl)) return;
        if (!entity.getPersistentData().getBooleanOr(ROLLED, false)) roll(entity);

        int every = BeyonderMobRules.AURA_EVERY_TICKS;
        if (every <= 0 || (entity.tickCount + entity.getId()) % every != 0) return;
        if (entity.isInvisible() || !isBeyonder(entity)) return;           // an aura would give the hidden away
        aura(sl, entity);
    }

    /** A few motes of its Pathway's color around its body. */
    private static void aura(ServerLevel sl, LivingEntity entity) {
        String pathway = pathway(entity);
        if (pathway == null) return;
        int rgb = DarkAngel.pathwayColor(pathway) & 0xFFFFFF;
        double width = entity.getBbWidth();
        double height = entity.getBbHeight();
        sl.sendParticles(new DustParticleOptions(rgb, 0.8f),
                entity.getX(), entity.getY() + height * 0.5, entity.getZ(),
                BeyonderMobRules.AURA_MOTES, width * 0.4, height * 0.35, width * 0.4, 0.0);
    }

    /** It dies: its characteristic drops. LOW: after a death ward has had its say (DeathWard runs first). */
    @SubscribeEvent(priority = Priority.LOW)
    public static void onBeyonderMobDeath(LivingDeathEvent event) {
        LivingEntity body = event.getEntity();
        if (!(body.level() instanceof ServerLevel sl) || !isBeyonder(body)) return;
        if (body.getHealth() > 0f) return;                                 // its death was undone
        if (BeyonderMobRules.SUMMONS_NEVER && Marionette.isSummoned(body)) return;
        dropCharacteristic(sl, body, event.getSource().getEntity());
    }
}
