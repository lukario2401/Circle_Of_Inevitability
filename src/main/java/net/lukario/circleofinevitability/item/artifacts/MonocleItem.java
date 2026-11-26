package net.lukario.circleofinevitability.item.artifacts;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonders;
import net.lukario.circleofinevitability.combat.HudTimer;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.item.ModItems;
import net.lukario.circleofinevitability.status.Concealment;
import net.lukario.circleofinevitability.status.Marionette;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.listener.Priority;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/**
 * The Monocle: a sealed artifact worn in the helmet slot. Its powers grow the longer it is worn, and so does the
 * price: the wearer slowly loses themself, doing things they did not choose more and more often, until it kills them.
 *
 * <ul>
 *   <li>"Lost" (seconds, saved per player) rises by one each second it is worn and falls by one each second it is not.</li>
 *   <li>Stage I, Spirit Vision (from the start): living things nearby glow, Night Vision, and what you look at is read
 *       on the action bar. Gaze (key, default B) reads a target in full: health, Pathway, Sequence, abilities.</li>
 *   <li>Stage II, Mystery Prying (STAGE_TWO): ores nearby shimmer for the wearer alone, and Foresight cancels one
 *       big hit (a third of max health or more) every FORESIGHT_RECHARGE ticks.</li>
 *   <li>Stage III, Spirit Body Threads (STAGE_THREE): Gaze on a mob pulls its threads: it becomes a marionette.</li>
 *   <li>From LOSS_STARTS the wearer acts on their own: glances, twitches, steps, jumps, hotbar swaps, dropped items,
 *       laughter, and at the end striking whoever is nearest. Chance per second grows from 1/60 to 1/3.</li>
 *   <li>At DEATH seconds it kills the wearer (damage type circle_of_inevitability:monocle).</li>
 * </ul>
 * Creative/spectator wearers still progress (so the powers can be tried) but never act on their own or die of it.
 */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MonocleItem extends SealedArtifactItem {
    public static final String ID = "monocle";
    public static final ResourceKey<EquipmentAsset> ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID,
            Identifier.fromNamespaceAndPath(CircleOfInevitability.MOD_ID, ID));
    public static final ResourceKey<DamageType> DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE,
            Identifier.fromNamespaceAndPath(CircleOfInevitability.MOD_ID, ID));

    /** Seconds of wearing. */
    public static final int STAGE_TWO = 150, STAGE_THREE = 300, LOSS_STARTS = 60, DEATH = 600;
    public static final double VISION_RANGE = 20, READ_RANGE = 40, GAZE_RANGE = 24, ORE_RANGE = 7;
    public static final int GAZE_COOLDOWN = 60, THREADS_COOLDOWN = 600, MARIONETTE_TICKS = 1200;
    public static final float THREADS_MAX_HEALTH = 100, FORESIGHT_SHARE = 1 / 3f;
    public static final int FORESIGHT_RECHARGE = 1200;
    /** Seconds of loss a gaze adds: looking into others costs a piece of yourself. */
    public static final int GAZE_COST = 3, THREADS_COST = 15;
    /** On the worn stack, for the model (the strings above the head from stage III). */
    public static final String STAGE_KEY = "coi_monocle_stage";

    static final String LOST = "lost", WARNED = "warned", GAZE_READY = "gaze_ready_at", FORESIGHT_READY = "foresight_ready_at";
    private static final int[] WARN_AT_PERCENT = {50, 75, 90};

    public MonocleItem(Properties properties) {
        super(properties.component(DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.HEAD)
                        .setAsset(ASSET).setEquipSound(SoundEvents.ARMOR_EQUIP_GOLD).build()), ID, 0,
                List.of(Component.translatable("tooltip.circle_of_inevitability.monocle.power").withStyle(ChatFormatting.GOLD),
                        Component.translatable("tooltip.circle_of_inevitability.monocle.stages"),
                        Component.translatable("tooltip.circle_of_inevitability.monocle.drawback").withStyle(ChatFormatting.DARK_RED),
                        Component.translatable("tooltip.circle_of_inevitability.monocle.containment")));
    }

    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(net.lukario.circleofinevitability.client.armor.ArmorModelExtensions.monocle());
    }

    // ---------- wearing

    public static boolean wearing(Player player) {
        return player.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof MonocleItem;
    }

    public static int lost(ServerPlayer player) {
        return Math.clamp(state(player, ID).getIntOr(LOST, 0), 0, DEATH);
    }

    public static void setLost(ServerPlayer player, int seconds) {
        state(player, ID).putInt(LOST, Math.clamp(seconds, 0, DEATH));
    }

    /** 1, 2 or 3. */
    public static int stage(int lost) {
        return lost >= STAGE_THREE ? 3 : lost >= STAGE_TWO ? 2 : 1;
    }

    /** The chance per second of acting on one's own: none before LOSS_STARTS, then 1/60 rising to 1/3 at DEATH. */
    public static float actChance(int lost) {
        if (lost < LOSS_STARTS) return 0;
        float t = (lost - LOSS_STARTS) / (float) (DEATH - LOSS_STARTS);
        return 1 / 60f + (1 / 3f - 1 / 60f) * t * t;
    }

    /** Right-click in hand puts it on (swapping whatever helmet is worn). */
    @Override
    protected boolean activate(ServerLevel level, ServerPlayer player, ItemStack stack) {
        ItemStack worn = player.getItemBySlot(EquipmentSlot.HEAD);
        InteractionHand hand = player.getMainHandItem() == stack ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        player.setItemSlot(EquipmentSlot.HEAD, stack.copy());
        player.setItemInHand(hand, worn.copy());
        Spells.sound(level, player.position(), SoundEvents.ARMOR_EQUIP_GOLD.value(), .8f, 1.3f);
        player.sendOverlayMessage(Component.literal("The lens settles over your eye. Something on the other side looks back.")
                .withStyle(ChatFormatting.GRAY));
        return true;
    }

    @Override
    public void carriedTick(ServerLevel level, ServerPlayer player, ItemStack carried) {
        boolean worn = wearing(player);
        boolean exempt = player.isCreative() || player.isSpectator();
        int before = lost(player);
        int lost = Math.clamp(before + (worn ? 1 : -1), 0, DEATH);
        setLost(player, lost);
        showWorn(player, before, lost);
        if (!worn) {
            if (state(player, ID).getIntOr(WARNED, 0) > 0) HudTimer.hide(player, "monocle_lost");
            state(player, ID).putInt(WARNED, 0);
            return;
        }
        ItemStack stack = player.getItemBySlot(EquipmentSlot.HEAD);
        int stage = stage(lost);
        markStage(stack, stage);
        if (stage(before) < stage) announceStage(level, player, stage);
        Spells.keepEffect(player, MobEffects.NIGHT_VISION, 0, 300);
        if (exempt) return;
        if (lost >= DEATH) {
            kill(level, player);
            return;
        }
        warn(player, lost);
        if (player.getRandom().nextFloat() < actChance(lost)) actOnItsOwn(level, player, lost);
    }

    /** "Worn 205/600": seconds worn (what is left of it while recovering), next to the hotbar. */
    private static void showWorn(ServerPlayer player, int before, int lost) {
        if (lost > 0) net.lukario.circleofinevitability.combat.HudCounter.show(player, "monocle_worn", "", "Worn (s)", lost, DEATH,
                lost >= DEATH * 3 / 4 ? 0xC0392B : lost >= LOSS_STARTS ? 0xB07CE0 : 0xD6AC4A);
        else if (before > 0) net.lukario.circleofinevitability.combat.HudCounter.hide(player, "monocle_worn");
    }

    private static void markStage(ItemStack stack, int stage) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag.getIntOr(STAGE_KEY, 0) == stage) return;
        tag.putInt(STAGE_KEY, stage);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private static void announceStage(ServerLevel level, ServerPlayer player, int stage) {
        player.sendSystemMessage(Component.literal(switch (stage) {
            case 2 -> "The lens deepens. You see what is hidden, and a moment of what is to come. (Mystery Prying)";
            default -> "You see threads now, running from every living thing. Some of them run from you. (Spirit Body Threads)";
        }).withStyle(ChatFormatting.LIGHT_PURPLE));
        Spells.sound(level, player.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 1, .6f);
    }

    private static void warn(ServerPlayer player, int lost) {
        int percent = 100 * lost / DEATH, warned = state(player, ID).getIntOr(WARNED, 0);
        for (int stage = WARN_AT_PERCENT.length; stage >= 1; stage--) {
            if (percent < WARN_AT_PERCENT[stage - 1]) continue;
            if (warned < stage) {
                state(player, ID).putInt(WARNED, stage);
                HudTimer.show(player, "monocle_lost", "Yourself", (DEATH - lost) * 20, 0x8E5BC4);
                player.sendSystemMessage(Component.literal(switch (stage) {
                    case 1 -> "Your hands feel like someone else's. Take the Monocle off before you are gone.";
                    case 2 -> "You cannot remember the last thing you chose to do. Take it OFF.";
                    default -> "There is almost nothing of you left behind the lens.";
                }).withStyle(ChatFormatting.DARK_PURPLE));
            }
            break;
        }
    }

    private static void kill(ServerLevel level, ServerPlayer player) {
        reset(player);
        player.sendSystemMessage(Component.literal("Whoever was wearing the Monocle is gone. Only the Monocle remains.")
                .withStyle(ChatFormatting.DARK_RED));
        player.invulnerableTime = 0;
        player.hurtServer(level, damage(level), Float.MAX_VALUE);
    }

    public static DamageSource damage(ServerLevel level) {
        return new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DAMAGE));
    }

    private static void reset(ServerPlayer player) {
        setLost(player, 0);
        net.lukario.circleofinevitability.combat.HudCounter.hide(player, "monocle_worn");
        state(player, ID).putInt(WARNED, 0);
        HudTimer.hide(player, "monocle_lost");
    }

    // ---------- losing yourself

    /** The things a wearer does that they did not choose. Worse ones unlock as more of them is lost. */
    public enum Act { GLANCE, TWITCH, MUTTER, STEP, JUMP, SWAP, STARE, WANDER, DROP, LAUGH, STRIKE }

    public static List<Act> actsAt(int lost) {
        float t = lost / (float) DEATH;
        List<Act> acts = new ArrayList<>(List.of(Act.GLANCE, Act.TWITCH, Act.MUTTER));
        if (t >= .25f) acts.addAll(List.of(Act.STEP, Act.JUMP, Act.SWAP));
        if (t >= .5f) acts.addAll(List.of(Act.STARE, Act.WANDER, Act.DROP, Act.LAUGH));
        if (t >= .75f) acts.add(Act.STRIKE);
        return acts;
    }

    private static final String[] MUTTERS = {
            "...who is wearing whom?", "...the threads are so pretty from up here.", "...I was about to do something.",
            "...it is not my hand.", "...someone above the fog is laughing.", "...one more minute. Just one more."};

    public static void actOnItsOwn(ServerLevel level, ServerPlayer player, int lost) {
        List<Act> acts = actsAt(lost);
        act(level, player, acts.get(player.getRandom().nextInt(acts.size())));
    }

    public static void act(ServerLevel level, ServerPlayer player, Act act) {
        var random = player.getRandom();
        switch (act) {
            case GLANCE -> player.forceSetRotation((random.nextFloat() - .5f) * 140, true, (random.nextFloat() - .5f) * 50, true);
            case TWITCH -> player.swing(random.nextBoolean() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND, true);
            case MUTTER -> player.sendSystemMessage(Component.literal(MUTTERS[random.nextInt(MUTTERS.length)])
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            case STEP, WANDER -> {
                double angle = random.nextDouble() * Math.PI * 2;
                player.setDeltaMovement(player.getDeltaMovement().add(new Vec3(Math.cos(angle), .1, Math.sin(angle))
                        .scale(act == Act.WANDER ? .9 : .4)));
                player.hurtMarked = true;
                if (act == Act.WANDER) player.forceSetRotation((float) Math.toDegrees(Math.atan2(-Math.cos(angle), Math.sin(angle))), false, 0, false);
            }
            case JUMP -> {
                if (player.onGround()) {
                    player.setDeltaMovement(player.getDeltaMovement().add(0, .45, 0));
                    player.hurtMarked = true;
                }
            }
            case SWAP -> {
                int slot = random.nextInt(9);
                player.getInventory().setSelectedSlot(slot);
                player.connection.send(new ClientboundSetHeldSlotPacket(slot));
            }
            case STARE -> player.forceSetRotation(player.getYRot(), false, -90, false);
            case DROP -> {
                ItemStack held = player.getMainHandItem();
                if (!held.isEmpty()) {
                    player.drop(held.copy(), false, true);
                    player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                }
            }
            case LAUGH -> {
                Spells.sound(level, player.position(), SoundEvents.WITCH_CELEBRATE, .8f, 1.4f);
                Component line = Component.literal(player.getName().getString() + " laughs at something no one else can see.")
                        .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC);
                for (ServerPlayer near : level.getEntitiesOfClass(ServerPlayer.class, player.getBoundingBox().inflate(16)))
                    near.sendSystemMessage(line);
            }
            case STRIKE -> {
                LivingEntity nearest = level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(3.5),
                                e -> e != player && e.isAlive() && !(e instanceof ArmorStand) && !e.isSpectator())
                        .stream().min(java.util.Comparator.comparingDouble(player::distanceToSqr)).orElse(null);
                if (nearest == null) {
                    act(level, player, Act.TWITCH);
                    return;
                }
                player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, nearest.getEyePosition());
                player.forceSetRotation(player.getYRot(), false, player.getXRot(), false);
                player.attack(nearest);
                player.swing(InteractionHand.MAIN_HAND, true);
            }
        }
        if (act != Act.MUTTER && act != Act.LAUGH)
            player.sendOverlayMessage(Component.literal("Your body moves on its own.").withStyle(ChatFormatting.DARK_PURPLE));
    }

    // ---------- powers

    /** Spirit Vision, every half second while worn; ores from stage II every two seconds. */
    @SubscribeEvent
    public static void tick(net.minecraftforge.event.TickEvent.PlayerTickEvent.Post event) {
        if (!(event.player() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)
                || !player.isAlive() || !wearing(player)) return;
        if (player.tickCount % 10 == 0) spiritVision(level, player);
        if (player.tickCount % 40 == 0 && stage(lost(player)) >= 2) pryOres(level, player);
    }

    static void spiritVision(ServerLevel level, ServerPlayer player) {
        for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(VISION_RANGE),
                e -> e != player && e.isAlive() && !(e instanceof ArmorStand) && !e.isSpectator() && !Concealment.isHidden(e)))
            other.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.GLOWING, 15, 0, true, false));
        LivingEntity seen = Spells.aimLiving(player, level, READ_RANGE, true,
                e -> !(e instanceof ArmorStand) && !Concealment.isHidden(e));
        if (seen != null) player.sendOverlayMessage(read(player, seen));
    }

    /** "Zombie · 14/20 hp · hostile · Hand Of Order, Sequence 7". */
    public static Component read(ServerPlayer player, LivingEntity seen) {
        boolean hostile = seen instanceof Mob mob && mob.getTarget() == player;
        boolean friend = Spells.isFriend(player, seen);
        MutableComponent line = Component.literal(seen.getName().getString()).withStyle(ChatFormatting.WHITE)
                .append(Component.literal(String.format(" · %.0f/%.0f hp", seen.getHealth(), seen.getMaxHealth())).withStyle(ChatFormatting.RED))
                .append(Component.literal(friend ? " · friend" : hostile ? " · hunting you" : seen instanceof Mob m && m.getTarget() != null ? " · hostile" : " · calm")
                        .withStyle(friend ? ChatFormatting.GREEN : hostile ? ChatFormatting.DARK_RED : ChatFormatting.GRAY));
        String beyonder = Beyonders.describe(seen);
        if (!beyonder.isEmpty()) line.append(Component.literal(" · " + beyonder).withStyle(ChatFormatting.LIGHT_PURPLE));
        if (Marionette.isMarionette(seen)) line.append(Component.literal(" · strings held").withStyle(ChatFormatting.GOLD));
        return line;
    }

    static void pryOres(ServerLevel level, ServerPlayer player) {
        BlockPos center = player.blockPosition();
        int r = (int) ORE_RANGE;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, -r, -r), center.offset(r, r, r))) {
            var block = level.getBlockState(pos);
            if (!isOre(block)) continue;
            level.sendParticles(player, new DustParticleOptions(0xC9A2FF, 1.2f), true, false,
                    pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, 3, .3, .3, .3, 0);
        }
    }

    static boolean isOre(net.minecraft.world.level.block.state.BlockState state) {
        return state.is(net.minecraftforge.common.Tags.Blocks.ORES) || state.is(BlockTags.GOLD_ORES)
                || state.is(BlockTags.IRON_ORES) || state.is(BlockTags.COPPER_ORES);
    }

    /** Foresight: from stage II, a hit of a third of max health or more is seen coming and never lands. */
    @SubscribeEvent(priority = Priority.HIGH)
    public static boolean foresight(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)
                || !wearing(player) || stage(lost(player)) < 2) return false;
        if (event.getAmount() < player.getMaxHealth() * FORESIGHT_SHARE || event.getSource().is(DAMAGE)
                || event.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
        CompoundTag state = state(player, ID);
        if (level.getGameTime() < state.getLongOr(FORESIGHT_READY, 0L)) return false;
        state.putLong(FORESIGHT_READY, level.getGameTime() + FORESIGHT_RECHARGE);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1, player.getZ(), 30, .4, .8, .4, .05);
        Spells.sound(level, player.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 1, .5f);
        player.sendOverlayMessage(Component.literal("You saw this coming.").withStyle(ChatFormatting.LIGHT_PURPLE));
        return true;
    }

    /**
     * Gaze (the Monocle key). Stage III on a mob that is not a friend: pull its threads (marionette). Otherwise, or
     * while sneaking: read the target in full and make it glow for 30 seconds.
     */
    public static void gaze(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level) || !player.isAlive() || player.isSpectator() || !wearing(player)) return;
        if (net.lukario.circleofinevitability.status.ArtifactLock.has(player)
                || net.lukario.circleofinevitability.status.AbilityLock.isLocked(player)) {
            player.sendSystemMessage(Component.translatable("message.circle_of_inevitability.artifact.locked"));
            return;
        }
        CompoundTag state = state(player, ID);
        long now = level.getGameTime();
        long readyAt = state.getLongOr(GAZE_READY, 0L);
        if (now < readyAt) {
            player.sendOverlayMessage(Component.translatable("message.circle_of_inevitability.artifact.cooldown", (readyAt - now + 19) / 20));
            return;
        }
        LivingEntity target = Spells.aimLiving(player, level, GAZE_RANGE, true,
                e -> !(e instanceof ArmorStand) && !Concealment.isHidden(e));
        if (target == null) {
            player.sendOverlayMessage(Component.literal("The lens finds nothing to look at.").withStyle(ChatFormatting.GRAY));
            return;
        }
        int lost = lost(player);
        if (stage(lost) >= 3 && !player.isShiftKeyDown() && target instanceof Mob mob && !Spells.isFriend(player, mob)) {
            if (pullThreads(level, player, mob)) {
                state.putLong(GAZE_READY, now + THREADS_COOLDOWN);
                if (!player.isCreative()) setLost(player, lost + THREADS_COST);
                return;
            }
        }
        inspect(level, player, target);
        state.putLong(GAZE_READY, now + GAZE_COOLDOWN);
        if (!player.isCreative()) setLost(player, lost + GAZE_COST);
    }

    static boolean pullThreads(ServerLevel level, ServerPlayer player, Mob mob) {
        if (mob.getMaxHealth() > THREADS_MAX_HEALTH || Marionette.isMarionette(mob)) {
            player.sendOverlayMessage(Component.literal(mob.getName().getString() + "'s threads are too thick to pull.")
                    .withStyle(ChatFormatting.GRAY));
            return false;
        }
        if (!Marionette.control(mob, player, MARIONETTE_TICKS)) {
            player.sendOverlayMessage(Component.literal("You cannot hold any more strings.").withStyle(ChatFormatting.GRAY));
            return false;
        }
        Vec3 from = player.getEyePosition(), to = mob.position().add(0, mob.getBbHeight() * .7, 0);
        for (int i = 0; i <= 20; i++) {
            Vec3 p = from.lerp(to, i / 20.0);
            level.sendParticles(new DustParticleOptions(0xE6E1D2, .6f), p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
        Spells.sound(level, mob.position(), SoundEvents.LEAD_TIED, 1, .6f);
        player.sendSystemMessage(Component.literal("You pull " + mob.getName().getString()
                + "'s Spirit Body Threads. It is yours, for a minute. Something above you does the same.").withStyle(ChatFormatting.GOLD));
        return true;
    }

    static void inspect(ServerLevel level, ServerPlayer player, LivingEntity target) {
        target.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.GLOWING, 600, 0, true, false));
        player.sendSystemMessage(Component.literal("— Through the Monocle —").withStyle(ChatFormatting.GOLD));
        player.sendSystemMessage(read(player, target));
        if (Beyonders.isBeyonder(target)) {
            List<String> names = Beyonders.abilitiesOf(target).stream().map(a -> a.name()).toList();
            player.sendSystemMessage(Component.literal(names.isEmpty() ? "No abilities." : "Abilities: " + String.join(", ", names))
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        } else player.sendSystemMessage(Component.literal("No Beyonder characteristic.").withStyle(ChatFormatting.GRAY));
        level.sendParticles(ParticleTypes.ENCHANT, target.getX(), target.getY() + target.getBbHeight(), target.getZ(), 20, .3, .3, .3, .5);
        Spells.sound(level, player.position(), SoundEvents.AMETHYST_BLOCK_CHIME, .7f, 1.4f);
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) reset(player);
    }

    /** The registered item, for tests. */
    public static MonocleItem item() {
        return (MonocleItem) ModItems.MONOCLE.get();
    }
}
