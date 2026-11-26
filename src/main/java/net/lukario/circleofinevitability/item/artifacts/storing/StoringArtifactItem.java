package net.lukario.circleofinevitability.item.artifacts.storing;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonders;
import net.lukario.circleofinevitability.combat.AbilityCatalog;
import net.lukario.circleofinevitability.combat.Casts;
import net.lukario.circleofinevitability.item.artifacts.SealedArtifactItem;
import net.lukario.circleofinevitability.menu.AbilityMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * One tier of a StoringArtifact. Right-click casts the selected stored ability (free of spirituality, no Sequence
 * needed: the drawbacks are the price); sneak + right-click opens the wheel to select or discard. Kills feed it.
 * Nothing here is specific to one artifact: declare a new one with StoringArtifact.
 */
public class StoringArtifactItem extends SealedArtifactItem {
    public static final String EMPTY_ICON = "storing_artifact/empty";
    public static final String DISCARD_ICON = "storing_artifact/discard";
    public static final String STORED_ICON = "storing_artifact/stored";
    private static final int WHEEL_RADIUS = 90;

    private final StoringArtifact artifact;
    private final int tier;

    public StoringArtifactItem(Properties properties, StoringArtifact artifact, int tier) {
        super(properties, artifact.id(), artifact.maxCooldown(), artifact.lore(tier));
        this.artifact = artifact;
        this.tier = tier;
    }

    public StoringArtifact artifact() { return artifact; }
    public int tier() { return tier; }
    public AbilitySlots slots() { return artifact.slots(tier); }

    @Override
    public int rank() { return tier; }

    private String title() {
        return artifact.name() + " " + StoringArtifact.roman(tier);
    }

    // ---------- casting

    @Override
    protected int cooldownFor(ServerPlayer player, ItemStack stack) {
        AbilityCatalog.Power power = StoredAbilities.selectedPower(stack);
        return power == null ? 0 : artifact.cooldownFor(power.sequence());
    }

    @Override
    protected boolean activate(ServerLevel level, ServerPlayer player, ItemStack stack) {
        StoredAbilities.clean(stack);
        AbilityCatalog.Power power = StoredAbilities.selectedPower(stack);
        if (power == null) {
            player.sendSystemMessage(Component.literal(title() + " holds no abilities. Kill a Beyonder while carrying it."));
            return false;
        }
        Bearer bearer = artifact.bearer(level, player, stack);
        for (ArtifactDrawback drawback : artifact.drawbacksAt(tier)) {
            Component refusal = drawback.blocksCast(bearer);
            if (refusal != null) {
                player.sendSystemMessage(refusal);
                return false;
            }
        }
        boolean wentOff;
        try {
            wentOff = Casts.recast(player, power.cast());
        } catch (RuntimeException e) {
            CircleOfInevitability.LOGGER.error("A stored ability failed: {}", power.name(), e);
            wentOff = false;
        }
        if (!wentOff) {
            player.sendOverlayMessage(Component.literal(power.name() + " found nothing to act on."));
            return false;
        }
        artifact.effects().cast(level, player, power);
        for (ArtifactDrawback drawback : artifact.drawbacksAt(tier)) drawback.cast(bearer, power);
        return true;
    }

    // ---------- the wheels

    @Override
    protected boolean sneakUse(ServerLevel level, ServerPlayer player, ItemStack stack) {
        StoredAbilities.clean(stack);
        openSelectWheel(player, stack);
        return true;
    }

    /** Stored abilities (pick one to select it), the free slots greyed out, and Discard. */
    public void openSelectWheel(ServerPlayer player, ItemStack stack) {
        List<AbilityCatalog.Power> inside = StoredAbilities.powers(stack);
        int selected = StoredAbilities.selected(stack);
        AbilityMenu wheel = AbilityMenu.create(artifact.id() + "_select")
                .title(title() + " · " + inside.size() + "/" + slots().size() + " · pick the ability to cast")
                .ring(WHEEL_RADIUS);
        for (int i = 0; i < inside.size(); i++) {
            int index = i;
            AbilityCatalog.Power power = inside.get(i);
            wheel.option(icon(power), power.name(), 0, 0, (picker, level) -> {
                        if (!stillCarried(picker, stack)) return;
                        StoredAbilities.select(stack, index);
                        picker.sendOverlayMessage(Component.literal(title() + ": " + power.name() + " is ready."));
                    })
                    .details(viewer -> (index == selected ? "selected · " : "") + "Sequence " + power.sequence());
        }
        List<Integer> free = slots().free(StoredAbilities.sequences(stack));
        if (free != null) for (int cap : free)
            wheel.option(EMPTY_ICON, "Empty", 0, 0, (picker, level) -> {})
                    .details(viewer -> AbilitySlots.capLabel(cap)).lockedWhen(viewer -> true);
        if (!inside.isEmpty())
            wheel.option(DISCARD_ICON, "Discard...", 0, 0, (picker, level) -> {
                if (stillCarried(picker, stack)) openDiscardWheel(picker, stack);
            }).details(viewer -> "free a slot");
        wheel.open(player);
    }

    public void openDiscardWheel(ServerPlayer player, ItemStack stack) {
        List<AbilityCatalog.Power> inside = StoredAbilities.powers(stack);
        AbilityMenu wheel = AbilityMenu.create(artifact.id() + "_discard").title(title() + " · discard which ability?")
                .ring(WHEEL_RADIUS);
        for (int i = 0; i < inside.size(); i++) {
            int index = i;
            AbilityCatalog.Power power = inside.get(i);
            wheel.option(icon(power), power.name(), 0, 0, (picker, level) -> {
                if (!stillCarried(picker, stack) || !StoredAbilities.contains(stack, power.id())) return;
                StoredAbilities.remove(stack, index);
                picker.sendSystemMessage(Component.literal(title() + " lets go of " + power.name() + "."));
            }).details(viewer -> "Sequence " + power.sequence());
        }
        wheel.open(player);
    }

    // ---------- feeding

    /** The bearer killed 'victim' while this stack was their carried one of the family. */
    public void onKill(ServerLevel level, ServerPlayer player, ItemStack stack, LivingEntity victim) {
        Bearer bearer = artifact.bearer(level, player, stack);
        for (ArtifactDrawback drawback : artifact.drawbacksAt(tier)) drawback.killed(bearer, victim);
        if (!Beyonders.isBeyonder(victim)) return;
        StoredAbilities.clean(stack);
        List<AbilityCatalog.Power> theirs = new ArrayList<>(AbilityCatalog.of(victim).stream()
                .filter(p -> p.cast().copyable() && !StoredAbilities.contains(stack, p.id()))
                .sorted(Comparator.comparingInt(AbilityCatalog.Power::sequence).thenComparing(AbilityCatalog.Power::name))
                .toList());
        if (theirs.isEmpty()) return;
        if (artifact.choosesOnKill(tier)) {
            openDevourWheel(player, stack, victim, theirs);
            return;
        }
        List<AbilityCatalog.Power> fitting = theirs.stream()
                .filter(p -> StoredAbilities.check(stack, slots(), p) == StoredAbilities.Result.ADDED).toList();
        if (fitting.isEmpty()) {
            player.sendSystemMessage(Component.literal(title() + " finds nothing in " + victim.getName().getString()
                    + " it can hold (" + slotsLeft(stack) + "). Discard an ability to make room."));
            return;
        }
        AbilityCatalog.Power taken = fitting.get(player.getRandom().nextInt(fitting.size()));
        StoredAbilities.add(stack, slots(), taken);
        devoured(level, player, victim, taken, null);
    }

    /** Tier choosesOnKill: the victim's abilities, the ones that cannot fit greyed out. */
    public void openDevourWheel(ServerPlayer player, ItemStack stack, LivingEntity victim, List<AbilityCatalog.Power> theirs) {
        String victimName = victim.getName().getString();
        AbilityMenu wheel = AbilityMenu.create(artifact.id() + "_devour")
                .title(title() + " · devour which ability of " + victimName + "? (Esc: none)").ring(WHEEL_RADIUS);
        for (AbilityCatalog.Power power : theirs) {
            wheel.option(icon(power), power.name(), 0, 0, (picker, level) -> {
                        if (!stillCarried(picker, stack)) return;
                        StoredAbilities.Result result = StoredAbilities.add(stack, slots(), power);
                        if (result == StoredAbilities.Result.ADDED) devoured(level, picker, victim, power, null);
                        else if (canReplaceAny(stack, power)) openReplaceWheel(picker, stack, victim, power);
                    })
                    .details(viewer -> "Sequence " + power.sequence() + " · " + fitLabel(stack, power))
                    .lockedWhen(viewer -> StoredAbilities.check(stack, slots(), power) != StoredAbilities.Result.ADDED
                            && !canReplaceAny(stack, power));
        }
        wheel.open(player);
        player.sendSystemMessage(Component.literal(title() + " hungers for one of " + victimName + "'s abilities."));
    }

    /** No room: pick the stored ability it takes the place of (the ones it cannot replace greyed out). */
    public void openReplaceWheel(ServerPlayer player, ItemStack stack, LivingEntity victim, AbilityCatalog.Power power) {
        List<AbilityCatalog.Power> inside = StoredAbilities.powers(stack);
        AbilityMenu wheel = AbilityMenu.create(artifact.id() + "_replace")
                .title("Devour " + power.name() + " in place of...").ring(WHEEL_RADIUS);
        for (int i = 0; i < inside.size(); i++) {
            int index = i;
            AbilityCatalog.Power old = inside.get(i);
            wheel.option(icon(old), old.name(), 0, 0, (picker, level) -> {
                        if (!stillCarried(picker, stack) || !StoredAbilities.contains(stack, old.id())) return;
                        if (StoredAbilities.replace(stack, slots(), index, power)) devoured(level, picker, victim, power, old);
                    })
                    .details(viewer -> "Sequence " + old.sequence())
                    .lockedWhen(viewer -> !StoredAbilities.canReplace(stack, slots(), index, power));
        }
        wheel.open(player);
    }

    private void devoured(ServerLevel level, ServerPlayer player, LivingEntity victim, AbilityCatalog.Power power,
                          AbilityCatalog.Power replaced) {
        artifact.effects().devoured(level, player, victim, power);
        player.sendSystemMessage(Component.literal(title() + " devoured " + power.name() + " (Sequence " + power.sequence()
                + ")" + (replaced == null ? "" : ", in place of " + replaced.name()) + ". It is selected."));
    }

    private boolean canReplaceAny(ItemStack stack, AbilityCatalog.Power power) {
        int count = StoredAbilities.powers(stack).size();
        for (int i = 0; i < count; i++) if (StoredAbilities.canReplace(stack, slots(), i, power)) return true;
        return false;
    }

    private String fitLabel(ItemStack stack, AbilityCatalog.Power power) {
        return switch (StoredAbilities.check(stack, slots(), power)) {
            case ADDED -> "fits";
            case FULL, TOO_STRONG -> canReplaceAny(stack, power) ? "replace one" : "too strong for its slots";
            case ALREADY_STORED -> "already stored";
            case NOT_STORABLE -> "cannot be stored";
        };
    }

    private String slotsLeft(ItemStack stack) {
        List<Integer> free = slots().free(StoredAbilities.sequences(stack));
        if (free == null || free.isEmpty()) return "no free slots";
        return free.size() + " free, the strongest " + AbilitySlots.capLabel(free.getFirst());
    }

    /** The catalog icon if the mod has that picture, else a generic one (server-side check of the mod's own files). */
    public static String icon(AbilityCatalog.Power power) {
        String icon = power.icon();
        String path = "/assets/" + CircleOfInevitability.MOD_ID + "/textures/gui/" + icon + ".png";
        return icon != null && !icon.contains(":") && StoringArtifactItem.class.getResource(path) != null ? icon : STORED_ICON;
    }

    /** Menus answer later: the stack must still be on the player (by identity), or the pick does nothing. */
    public static boolean stillCarried(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) return false;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++)
            if (player.getInventory().getItem(slot) == stack) return true;
        return player.getMainHandItem() == stack || player.getOffhandItem() == stack;
    }

    // ---------- drawbacks

    @Override
    public void carriedTick(ServerLevel level, ServerPlayer player, ItemStack carried) {
        Bearer bearer = artifact.bearer(level, player, carried);
        for (ArtifactDrawback drawback : artifact.drawbacks()) {
            try {
                drawback.second(bearer);
            } catch (RuntimeException e) {
                CircleOfInevitability.LOGGER.error("A {} drawback failed", artifact.id(), e);
            }
            if (!player.isAlive()) return;
        }
    }
}
