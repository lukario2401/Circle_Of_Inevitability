package net.lukario.circleofinevitability.blasphemy_slate.beyonder;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/**
 * What a player must have done before /sequence advance lets them go one Sequence down.
 * Extension point: Digestion.requirement holds the one in use.
 *
 * TODO(acting/potions): acting, potion items, ingredients and rituals are not implemented. When they are, add a
 * requirement here (for example "drank the potion of the next Sequence") and combine it with DIGESTED, e.g.
 *   Digestion.requirement = AdvancementRequirement.DIGESTED.and(new DrankPotionRequirement());
 */
public interface AdvancementRequirement {

    /** True if the player may advance now. */
    boolean isMet(Player player);

    /** The message shown when isMet is false. */
    Component refusal(Player player);

    /** The default: digestion of the current Sequence is at 100 percent. */
    AdvancementRequirement DIGESTED = new AdvancementRequirement() {
        @Override
        public boolean isMet(Player player) {
            return SoulCore.getDigestion(player) >= 100f;
        }

        @Override
        public Component refusal(Player player) {
            return Component.translatable("message.circle_of_inevitability.sequence.not_digested",
                    Digestion.percent(player));
        }
    };

    /** Both requirements must be met; the refusal is the first one that is not. */
    default AdvancementRequirement and(AdvancementRequirement other) {
        AdvancementRequirement first = this;
        return new AdvancementRequirement() {
            @Override
            public boolean isMet(Player player) {
                return first.isMet(player) && other.isMet(player);
            }

            @Override
            public Component refusal(Player player) {
                return first.isMet(player) ? other.refusal(player) : first.refusal(player);
            }
        };
    }
}
