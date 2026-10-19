package net.lukario.circleofinevitability.blasphemy_slate.beyonder;

import net.lukario.circleofinevitability.item.BeyonderCharacteristic;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

/**
 * What happens when a player consumes a Beyonder Characteristic (item/BeyonderCharacteristicItem).
 *
 *   No Pathway yet + Sequence 9 of any Pathway       -> they start on that Pathway at Sequence 9. Safe.
 *   Their Pathway, the next Sequence (5 -> 4)         -> they advance, if the current Sequence is 100% digested
 *                                                       (Digestion.requirement). Below that it is refused and kept.
 *   Their Pathway, their Sequence or a weaker one     -> refused, kept (it can give them nothing).
 *   Their Pathway, more than one Sequence up (5 -> 2) -> a gamble: JUMP_CHANCE_PER_SKIPPED for every Sequence
 *                                                       skipped (5 -> 3 skips one, 5 -> 2 skips two). Also from
 *                                                       no Pathway (anything stronger than Sequence 9).
 *   Another Pathway                                   -> a gamble: PATHWAY_SWITCH_CHANCE if it is their own
 *                                                       Sequence, times JUMP_CHANCE_PER_SKIPPED for every Sequence
 *                                                       it is apart, up or down (at 5: a 5 is 20%, a 4 or a 6 is
 *                                                       10%, a 3 or a 7 is 5%). On success they take the new
 *                                                       Pathway at the stronger of the two Sequences.
 *
 * A gamble that fails: the characteristic is gone and the player dies. They keep their Pathway, Sequence and
 * digestion (SoulCore copies them over on death). Every gamble uses up the characteristic, won or lost.
 * The gambles ignore digestion; only the safe advance asks for it.
 *
 * Returns of consume(): true if the characteristic was used up (the item shrinks), false if it was refused.
 */
public final class CharacteristicConsumption {

    // =========================
    // Chances (0..1). To rebalance, change these.
    // =========================

    /** For every Sequence skipped, the chance is multiplied by this: 1 skipped = 50%, 2 = 25%, 3 = 12.5%... */
    public static final double JUMP_CHANCE_PER_SKIPPED = 0.5;
    /** The chance that another Pathway's characteristic takes (before any jump chance). */
    public static final double PATHWAY_SWITCH_CHANCE = 0.2;

    /** "No Pathway" counts as one Sequence weaker than 9, so its next Sequence is 9. */
    private static final int NO_PATHWAY_SEQUENCE = Beyonder.WEAKEST_SEQUENCE + 1;
    private static final String NO_PATHWAY = "none";

    private CharacteristicConsumption() {}

    /** The chance (0..1) that this player survives consuming this characteristic. 1 for the safe cases. */
    public static double successChance(ServerPlayer player, BeyonderCharacteristic characteristic) {
        int current = currentSequence(player);
        if (!sharesPathway(player, characteristic)) {
            // a switch wants the same Sequence: every Sequence apart, up or down, halves it again
            int apart = Math.abs(current - characteristic.sequence());
            return PATHWAY_SWITCH_CHANCE * Math.pow(JUMP_CHANCE_PER_SKIPPED, apart);
        }
        int skipped = Math.max(0, (current - 1) - characteristic.sequence());
        return Math.pow(JUMP_CHANCE_PER_SKIPPED, skipped);
    }

    /** Consumes the characteristic in 'stack' (see the class comment). True if it was used up. */
    public static boolean consume(ServerPlayer player, ServerLevel sl, ItemStack stack, BeyonderCharacteristic characteristic) {
        if (!SequenceCommands.PATHWAYS.contains(characteristic.pathway())) {
            player.sendSystemMessage(Component.translatable("message.circle_of_inevitability.characteristic.unknown_pathway",
                    characteristic.pathway()));
            return false;
        }

        boolean noPathway = hasNoPathway(player);
        boolean samePathway = sharesPathway(player, characteristic);
        int current = currentSequence(player);
        int target = characteristic.sequence();

        // their own Pathway and nothing stronger: it can give them nothing
        if (samePathway && target >= current) {
            player.sendSystemMessage(Component.translatable("message.circle_of_inevitability.characteristic.too_weak",
                    target, SoulCore.getSequence(player)));
            return false;
        }

        // the safe ones: no Pathway + Sequence 9, or their Pathway's next Sequence
        if (samePathway && target == current - 1) {
            if (noPathway) {
                stack.shrink(1);
                become(player, sl, characteristic.pathway(), target);
                player.sendSystemMessage(Component.translatable("message.circle_of_inevitability.characteristic.joined",
                        characteristic.pathway()));
                return true;
            }
            if (!Digestion.requirement.isMet(player)) {
                player.sendSystemMessage(Digestion.requirement.refusal(player));
                return false;
            }
            stack.shrink(1);
            Digestion.advance(player);               // Sequence - 1, digestion back to 0, the usual message and sound
            return true;
        }

        // the gambles: more than one Sequence up, or another Pathway
        double chance = successChance(player, characteristic);
        stack.shrink(1);
        if (player.getRandom().nextDouble() < chance) {
            int newSequence = Math.min(noPathway ? NO_PATHWAY_SEQUENCE : SoulCore.getSequence(player), target);
            become(player, sl, characteristic.pathway(), newSequence);
            player.sendSystemMessage(Component.translatable(samePathway
                            ? "message.circle_of_inevitability.characteristic.jump_survived"
                            : "message.circle_of_inevitability.characteristic.switch_survived",
                    newSequence, characteristic.pathway(), percent(chance)));
            return true;
        }

        player.sendSystemMessage(Component.translatable("message.circle_of_inevitability.characteristic.failed",
                percent(chance)));
        sl.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.8f, 1.4f);
        player.kill(sl);                             // the Pathway, Sequence and digestion stay (SoulCore.onPlayerClone)
        return true;
    }

    // =========================
    // Inner workings
    // =========================

    /** Sets the Pathway and Sequence, digestion back to 0, with the level-up sound. */
    private static void become(ServerPlayer player, ServerLevel sl, String pathway, int sequence) {
        SoulCore.setPathway(player, pathway);
        SoulCore.setSequence(player, sequence);
        SoulCore.setDigestion(player, 0f);
        sl.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1f, 0.8f);
    }

    private static boolean hasNoPathway(ServerPlayer player) {
        String pathway = SoulCore.getPathway(player);
        return pathway == null || pathway.isEmpty() || pathway.equals(NO_PATHWAY);
    }

    /** No Pathway counts as sharing every Pathway: the first characteristic decides it. */
    private static boolean sharesPathway(ServerPlayer player, BeyonderCharacteristic characteristic) {
        return hasNoPathway(player) || SoulCore.getPathway(player).equals(characteristic.pathway());
    }

    /** Their Sequence, or NO_PATHWAY_SEQUENCE before they have a Pathway. */
    private static int currentSequence(ServerPlayer player) {
        return hasNoPathway(player) ? NO_PATHWAY_SEQUENCE : SoulCore.getSequence(player);
    }

    private static String percent(double chance) {
        double p = chance * 100.0;
        return p == Math.floor(p) ? String.valueOf((int) p) : String.format("%.1f", p);
    }
}
