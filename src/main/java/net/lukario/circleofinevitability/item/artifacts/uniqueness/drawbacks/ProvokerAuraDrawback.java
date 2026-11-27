package net.lukario.circleofinevitability.item.artifacts.uniqueness.drawbacks;

import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.item.artifacts.uniqueness.ShadowSoldier;
import net.lukario.circleofinevitability.item.artifacts.uniqueness.UniquenessDrawback;
import net.lukario.circleofinevitability.item.artifacts.uniqueness.UniquenessItem;
import net.lukario.circleofinevitability.item.artifacts.uniqueness.Wielder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MerchantMenu;

/**
 * Provoker's aura: every two seconds the neutral creatures that can fight (wolves, iron golems, bees, piglins,
 * endermen...) within radius[level] blocks turn on the wielder. From noTradeFrom villagers refuse to trade with them;
 * from petsFrom their own pets break loose and attack them too.
 */
public final class ProvokerAuraDrawback implements UniquenessDrawback {
    private final int[] radius;
    private final int noTradeFrom, petsFrom;

    public ProvokerAuraDrawback(int[] radius, int noTradeFrom, int petsFrom) {
        this.radius = radius;
        this.noTradeFrom = noTradeFrom;
        this.petsFrom = petsFrom;
    }

    @Override
    public Component describe() {
        return Component.literal("Provoker's aura: neutral mobs and golems attack you; at High villagers refuse to trade, "
                + "at Extreme your own pets turn on you.");
    }

    @Override
    public void second(Wielder w) {
        if (!UniquenessItem.active(this, w) || w.level().getGameTime() / 20 % 2 != 0) return;
        provoke(w);
    }

    /** Turns them now. Returns how many. */
    public int provoke(Wielder w) {
        var player = w.player();
        boolean pets = w.corruptionLevel() >= petsFrom;
        int turned = 0;
        for (Mob mob : w.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(w.at(radius)),
                m -> m.isAlive() && !m.isNoAi() && !(m instanceof Enemy) && !(m instanceof ShadowSoldier)
                        && m.getAttribute(Attributes.ATTACK_DAMAGE) != null)) {
            if (mob instanceof TamableAnimal pet && pet.isOwnedBy(player)) {
                if (!pets) continue;
                pet.setOrderedToSit(false);
                pet.setTame(false, true);
                pet.setOwner(null);
                player.sendSystemMessage(Component.literal(mob.getName().getString() + " no longer knows you.")
                        .withStyle(ChatFormatting.DARK_RED));
            } else if (Spells.isFriend(player, mob)) continue;
            if (mob instanceof NeutralMob neutral) {
                neutral.setPersistentAngerTarget(EntityReference.of(player));
                neutral.startPersistentAngerTimer();
            }
            if (mob.getTarget() != player) mob.setTarget(player);
            if (++turned >= 16) break;
        }
        return turned;
    }

    @Override
    public boolean refusesMenu(Wielder w, AbstractContainerMenu menu) {
        if (!(menu instanceof MerchantMenu) || w.corruptionLevel() < noTradeFrom) return false;
        w.player().sendOverlayMessage(Component.literal("The villager backs away from you. No trade.").withStyle(ChatFormatting.RED));
        return true;
    }
}
