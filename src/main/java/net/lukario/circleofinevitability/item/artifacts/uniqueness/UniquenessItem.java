package net.lukario.circleofinevitability.item.artifacts.uniqueness;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.item.artifacts.SealedArtifactItem;
import net.lukario.circleofinevitability.menu.AbilityList;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwingAnimationType;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.SwingAnimation;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * The item of a Uniqueness. Right-click casts the selected ability; sneak + right-click opens the list of every
 * ability it lends, with the corruption level each needs (the ones out of reach greyed out). Nothing here is specific
 * to one uniqueness: declare a new one with Uniqueness.
 */
public class UniquenessItem extends SealedArtifactItem {
    public static final String DEFAULT_ICON = "storing_artifact/stored";
    static final String OWNER = "uniqueness_owner", STACK_GENERATION = "uniqueness_generation";

    private final Uniqueness uniqueness;

    public UniquenessItem(Properties properties, Uniqueness uniqueness) {
        super(uniqueness.isSpear() ? properties.component(DataComponents.SWING_ANIMATION, new SwingAnimation(SwingAnimationType.STAB, 8)) : properties,
                uniqueness.id(), Math.max(1, uniqueness.cooldown() + 20 * 60), uniqueness.lore());
        this.uniqueness = uniqueness;
    }

    /** A spear is held like the vanilla ones: arm raised and forward, the point following your gaze. Runs inside the
     *  Item constructor, before 'uniqueness' is set, so it is only read when a frame is drawn. */
    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
            @Override
            public net.minecraft.client.model.HumanoidModel.ArmPose getArmPose(net.minecraft.world.entity.LivingEntity entity,
                    net.minecraft.world.InteractionHand hand, ItemStack stack) {
                return uniqueness != null && uniqueness.isSpear() ? net.minecraft.client.model.HumanoidModel.ArmPose.SPEAR : null;
            }
        });
    }

    public Uniqueness uniqueness() { return uniqueness; }

    @Override
    public int rank() { return Uniqueness.TIER; }

    // ---------- casting

    @Override
    protected int cooldownFor(ServerPlayer player, ItemStack stack) {
        return uniqueness.cooldown();
    }

    @Override
    protected boolean activate(ServerLevel level, ServerPlayer player, ItemStack stack) {
        UniquePower power = uniqueness.selected(player);
        if (power == null) return false;
        Wielder wielder = uniqueness.wielder(level, player, stack);
        int needs = uniqueness.requiredLevel(power.sequence());
        if (wielder.corruptionLevel() < needs) {
            player.sendOverlayMessage(Component.literal(power.name() + " needs ").append(uniqueness.level(needs).label())
                    .append(Component.literal(" corruption (yours is ")).append(uniqueness.level(wielder.corruptionLevel()).label())
                    .append(Component.literal(").")));
            return false;
        }
        String readyKey = "ready_" + power.id();
        long left = wielder.state().getLongOr(readyKey, 0L) - level.getGameTime();
        if (left > 0) {
            player.sendOverlayMessage(Component.literal(power.name() + " recovers in " + (left + 19) / 20 + " s."));
            return false;
        }
        for (UniquenessDrawback drawback : uniqueness.drawbacks()) {
            if (!active(drawback, wielder)) continue;
            Component refusal = drawback.blocksCast(wielder, power);
            if (refusal != null) {
                player.sendOverlayMessage(refusal);
                return false;
            }
        }
        boolean wentOff;
        try {
            wentOff = power.cast(player, level);
        } catch (RuntimeException e) {
            CircleOfInevitability.LOGGER.error("A {} ability failed: {}", uniqueness.id(), power.name(), e);
            wentOff = false;
        }
        if (!wentOff) {
            player.sendOverlayMessage(Component.literal(power.name() + " found nothing to act on."));
            return false;
        }
        if (power.ownCooldown() > 0) wielder.state().putLong(readyKey, level.getGameTime() + power.ownCooldown());
        uniqueness.addCorruption(player, uniqueness.perCast(power.sequence()));
        uniqueness.effects().cast(level, player, power);
        for (UniquenessDrawback drawback : uniqueness.drawbacks())
            if (active(drawback, wielder)) drawback.cast(wielder, power);
        return true;
    }

    /** A downside acts on this wielder now: it applies at their level and they suffer (carried or bound, not creative). */
    public static boolean active(UniquenessDrawback drawback, Wielder wielder) {
        return wielder.suffering() && drawback.appliesAt(wielder.corruptionLevel());
    }

    // ---------- the ability list

    @Override
    protected boolean sneakUse(ServerLevel level, ServerPlayer player, ItemStack stack) {
        openPicker(player);
        return true;
    }

    /** Every ability it lends; the ones the corruption does not reach yet are greyed out with the level they need. */
    public void openPicker(ServerPlayer player) {
        List<UniquePower> powers = uniqueness.powers();
        double corruption = uniqueness.corruption(player);
        int now = uniqueness.levelAt(corruption);
        UniquePower selected = uniqueness.selected(player);
        Uniqueness.Level level = uniqueness.level(now);
        AbilityList list = AbilityList.create(uniqueness.id() + "_select")
                .title(uniqueness.name() + " · corruption " + level.name() + " " + (int) corruption + "/100 · pick the ability to cast")
                .hint(hint(now));
        for (UniquePower power : powers) {
            int needs = uniqueness.requiredLevel(power.sequence());
            boolean locked = needs > now;
            String detail = "Sequence " + power.sequence()
                    + (locked ? " · needs " + uniqueness.level(needs).name() + " corruption" : "")
                    + (power.ownCooldown() > 0 ? " · " + power.ownCooldown() / 20 + " s cooldown" : "")
                    + (power == selected ? " · selected" : "");
            list.row(icon(power), power.name(), detail).locked(locked).marked(power == selected);
            if (!power.description().isEmpty()) list.description(power.description());
        }
        list.onPick((picker, picks) -> {
            UniquePower power = powers.get(picks.getFirst());
            if (uniqueness.requiredLevel(power.sequence()) > uniqueness.levelAt(uniqueness.corruption(picker))) return;
            uniqueness.select(picker, power);
            picker.sendOverlayMessage(Component.literal(uniqueness.name() + ": " + power.name() + " is ready."));
        }).open(player);
    }

    private String hint(int now) {
        StringBuilder text = new StringBuilder("Levels: ");
        List<Uniqueness.Level> levels = uniqueness.levels();
        for (int i = 0; i < levels.size(); i++) {
            if (i > 0) text.append(" · ");
            text.append(i == now ? "[" + levels.get(i).name() + "]" : levels.get(i).name())
                    .append(" ").append((int) levels.get(i).from()).append("+");
        }
        return text.toString();
    }

    /** The catalog icon if the mod has that picture, else a generic one. */
    public static String icon(UniquePower power) {
        String icon = power.icon();
        String path = "/assets/" + CircleOfInevitability.MOD_ID + "/textures/gui/" + icon + ".png";
        return icon != null && !icon.contains(":") && UniquenessItem.class.getResource(path) != null ? icon : DEFAULT_ICON;
    }

    // ---------- carrying, binding, the stale copies a recall leaves behind

    @Override
    public void carriedTick(ServerLevel level, ServerPlayer player, ItemStack carried) {
        if (!carried.isEmpty()) stamp(carried, player, uniqueness.state(player).getIntOr(Uniqueness.GENERATION, 0));
        uniqueness.second(level, player, carried);
    }

    private static void stamp(ItemStack stack, ServerPlayer owner, int generation) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (owner.getUUID().toString().equals(tag.getStringOr(OWNER, "")) && tag.getIntOr(STACK_GENERATION, -1) == generation) return;
        CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> {
            t.putString(OWNER, owner.getUUID().toString());
            t.putInt(STACK_GENERATION, generation);
        });
    }

    /** A copy left behind by a recall (its owner's uniqueness came back to them since) crumbles where it is found. */
    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity holder, @Nullable EquipmentSlot slot) {
        super.inventoryTick(stack, level, holder, slot);
        if (holder.tickCount % 20 != 0 || !isStale(stack, level)) return;
        stack.setCount(0);
        if (holder instanceof ServerPlayer player)
            player.sendSystemMessage(Component.literal("The " + uniqueness.name() + " you held crumbles to ash: it went back to its bearer.")
                    .withStyle(ChatFormatting.DARK_GRAY));
    }

    boolean isStale(ItemStack stack, ServerLevel level) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        String owner = tag.getStringOr(OWNER, "");
        if (owner.isEmpty()) return false;
        ServerPlayer player;
        try {
            player = level.getServer().getPlayerList().getPlayer(UUID.fromString(owner));
        } catch (IllegalArgumentException e) {
            return false;
        }
        return player != null && tag.getIntOr(STACK_GENERATION, 0) < uniqueness.state(player).getIntOr(Uniqueness.GENERATION, 0);
    }

    /**
     * It is bound and not on the player: bring it back. From the menu they have open (a chest), from the ground near
     * them, or, if it is nowhere to be found, it forms again in their hands and the copy left behind becomes stale.
     * Returns the stack now on them.
     */
    static ItemStack recall(Uniqueness uniqueness, ServerLevel level, ServerPlayer player) {
        ItemStack found = ItemStack.EMPTY;
        for (Slot slot : player.containerMenu.slots) {
            if (slot.container == player.getInventory()) continue;
            ItemStack in = slot.getItem();
            if (in.getItem() instanceof UniquenessItem item && item.uniqueness == uniqueness) {
                found = in.copy();
                slot.set(ItemStack.EMPTY);
                break;
            }
        }
        if (found.isEmpty()) {
            for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(64),
                    e -> e.getItem().getItem() instanceof UniquenessItem item && item.uniqueness == uniqueness)) {
                found = entity.getItem().copy();
                entity.discard();
                break;
            }
        }
        if (found.isEmpty()) {
            int generation = uniqueness.state(player).getIntOr(Uniqueness.GENERATION, 0) + 1;
            uniqueness.state(player).putInt(Uniqueness.GENERATION, generation);
            found = new ItemStack(uniqueness.item());
            stamp(found, player, generation);
        }
        if (!player.getInventory().add(found)) {
            ItemStack held = player.getMainHandItem().copy();
            player.setItemSlot(EquipmentSlot.MAINHAND, found);
            player.drop(held, false, true);
        }
        player.sendSystemMessage(Component.literal(uniqueness.name() + " will not leave you. It is back in your hands.")
                .withStyle(ChatFormatting.DARK_RED));
        Spells.sound(level, player.position(), SoundEvents.SOUL_ESCAPE.value(), 1f, .6f);
        return net.lukario.circleofinevitability.item.artifacts.SealedArtifacts.carried(player, uniqueness.id());
    }
}
