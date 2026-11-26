package net.lukario.circleofinevitability.item.artifacts.storing;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonders;
import net.lukario.circleofinevitability.combat.AbilityCatalog;
import net.lukario.circleofinevitability.combat.Casts;
import net.lukario.circleofinevitability.item.artifacts.SealedArtifactItem;
import net.lukario.circleofinevitability.menu.AbilityList;
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
 * needed: the drawbacks are the price); sneak + right-click opens the list to select or discard. Kills feed it.
 * Nothing here is specific to one artifact: declare a new one with StoringArtifact.
 */
public class StoringArtifactItem extends SealedArtifactItem {
    public static final String EMPTY_ICON = "storing_artifact/empty";
    public static final String DISCARD_ICON = "storing_artifact/discard";
    public static final String STORED_ICON = "storing_artifact/stored";

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

    // ---------- the lists (menu/AbilityList: scrollable, so any number of abilities fits)

    @Override
    protected boolean sneakUse(ServerLevel level, ServerPlayer player, ItemStack stack) {
        StoredAbilities.clean(stack);
        openSelectList(player, stack);
        return true;
    }

    /** Stored abilities (pick one to select it), the free slots greyed out, and Discard at the end. */
    public void openSelectList(ServerPlayer player, ItemStack stack) {
        List<AbilityCatalog.Power> inside = StoredAbilities.powers(stack);
        int selected = StoredAbilities.selected(stack);
        AbilityList list = AbilityList.create(artifact.id() + "_select")
                .title(title() + " · " + inside.size() + "/" + slots().size() + " stored · pick the ability to cast");
        for (int i = 0; i < inside.size(); i++) {
            AbilityCatalog.Power power = inside.get(i);
            list.row(icon(power), power.name(), "Sequence " + power.sequence() + (i == selected ? " · selected" : ""))
                    .marked(i == selected);
        }
        List<Integer> free = slots().free(StoredAbilities.sequences(stack));
        if (free != null) for (int cap : free) list.row(EMPTY_ICON, "Empty slot", AbilitySlots.capLabel(cap)).locked(true);
        int discard = list.rows().size();
        if (!inside.isEmpty()) list.row(DISCARD_ICON, "Discard...", "free slots by letting abilities go");
        list.onPick((picker, picks) -> {
            if (!stillCarried(picker, stack)) return;
            int index = picks.getFirst();
            if (!inside.isEmpty() && index == discard) {
                openDiscardList(picker, stack);
                return;
            }
            if (index >= inside.size() || !StoredAbilities.contains(stack, inside.get(index).id())) return;
            StoredAbilities.select(stack, StoredAbilities.indexOf(stack, inside.get(index).id()));
            picker.sendOverlayMessage(Component.literal(title() + ": " + inside.get(index).name() + " is ready."));
        }).open(player);
    }

    /** Pick any number of stored abilities to let go of. */
    public void openDiscardList(ServerPlayer player, ItemStack stack) {
        List<AbilityCatalog.Power> inside = StoredAbilities.powers(stack);
        AbilityList list = AbilityList.create(artifact.id() + "_discard").title(title() + " · discard which abilities?")
                .maxPicks(Math.max(1, inside.size())).confirmLabel("Discard");
        for (AbilityCatalog.Power power : inside) list.row(icon(power), power.name(), "Sequence " + power.sequence());
        list.onPick((picker, picks) -> {
            if (!stillCarried(picker, stack)) return;
            List<String> gone = new ArrayList<>();
            for (int pick : picks) {
                AbilityCatalog.Power power = inside.get(pick);
                int now = StoredAbilities.indexOf(stack, power.id());
                if (now < 0) continue;
                StoredAbilities.remove(stack, now);
                gone.add(power.name());
            }
            if (!gone.isEmpty())
                picker.sendSystemMessage(Component.literal(title() + " lets go of " + String.join(", ", gone) + "."));
        }).open(player);
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
            openDevourList(player, stack, victim, theirs);
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

    /**
     * Tiers that choose: the victim's abilities as a list; up to picksAt(tier) of them may be picked. The ones that can
     * neither fit nor replace a stored one are greyed out. Picks that fit are devoured at once; each that needs room
     * then asks which stored ability it replaces, one after another.
     */
    public void openDevourList(ServerPlayer player, ItemStack stack, LivingEntity victim, List<AbilityCatalog.Power> theirs) {
        String victimName = victim.getName().getString();
        int picks = artifact.picksAt(tier);
        AbilityList list = AbilityList.create(artifact.id() + "_devour").maxPicks(picks).confirmLabel("Devour")
                .title(title() + " · devour " + (picks == 1 ? "one" : "up to " + picks) + " of " + victimName + "'s abilities"
                        + " · " + slotsLeft(stack));
        for (AbilityCatalog.Power power : theirs)
            list.row(icon(power), power.name(), "Sequence " + power.sequence() + " · " + fitLabel(stack, power))
                    .locked(StoredAbilities.check(stack, slots(), power) != StoredAbilities.Result.ADDED
                            && !canReplaceAny(stack, power));
        list.onPick((picker, chosen) -> {
            if (!stillCarried(picker, stack)) return;
            List<AbilityCatalog.Power> needRoom = new ArrayList<>();
            for (int pick : chosen) {
                AbilityCatalog.Power power = theirs.get(pick);
                if (StoredAbilities.add(stack, slots(), power) == StoredAbilities.Result.ADDED)
                    devoured(picker.level(), picker, victim, power, null);
                else if (!StoredAbilities.contains(stack, power.id())) needRoom.add(power);
            }
            replaceNext(picker, stack, victim, needRoom);
        });
        list.open(player);
        player.sendSystemMessage(Component.literal(title() + " hungers for " + (picks == 1 ? "one" : "up to " + picks)
                + " of " + victimName + "'s abilities."));
    }

    /** Asks, for the first of 'queue', which stored ability it replaces; then the rest, in turn. */
    private void replaceNext(ServerPlayer player, ItemStack stack, LivingEntity victim, List<AbilityCatalog.Power> queue) {
        if (queue.isEmpty()) return;
        AbilityCatalog.Power power = queue.getFirst();
        List<AbilityCatalog.Power> rest = queue.subList(1, queue.size());
        if (!canReplaceAny(stack, power)) {
            player.sendSystemMessage(Component.literal(title() + " has no room for " + power.name() + "."));
            replaceNext(player, stack, victim, rest);
            return;
        }
        openReplaceList(player, stack, victim, power, () -> replaceNext(player, stack, victim, rest));
    }

    /** No room: pick the stored ability 'power' takes the place of (the ones it cannot replace greyed out). */
    public void openReplaceList(ServerPlayer player, ItemStack stack, LivingEntity victim, AbilityCatalog.Power power,
                                Runnable then) {
        List<AbilityCatalog.Power> inside = StoredAbilities.powers(stack);
        AbilityList list = AbilityList.create(artifact.id() + "_replace")
                .title("No room: devour " + power.name() + " (Sequence " + power.sequence() + ") in place of...");
        for (int i = 0; i < inside.size(); i++) {
            AbilityCatalog.Power old = inside.get(i);
            list.row(icon(old), old.name(), "Sequence " + old.sequence())
                    .locked(!StoredAbilities.canReplace(stack, slots(), i, power));
        }
        list.onPick((picker, picks) -> {
            AbilityCatalog.Power old = inside.get(picks.getFirst());
            int now = StoredAbilities.indexOf(stack, old.id());
            if (stillCarried(picker, stack) && now >= 0 && StoredAbilities.replace(stack, slots(), now, power))
                devoured(picker.level(), picker, victim, power, old);
            then.run();
        }).onCancel(picker -> {
            picker.sendSystemMessage(Component.literal(title() + " lets " + power.name() + " go."));
            then.run();
        }).open(player);
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
