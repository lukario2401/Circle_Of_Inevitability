package net.lukario.circleofinevitability.blasphemy_slate.beyonder;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/**
 * A Beyonder's body: attribute changes (size, health, reach, attack speed, footing) that are on while something is
 * true and off when it is not. Safe to call every second or every tick: it only touches the attribute when the
 * value has to change, so it fits a check once a second in a player tick.
 *
 *   private static final Identifier TALL = Physique.id("my_pathway_tall");
 *   Physique.keep(player, Attributes.SCALE, TALL, 0.15, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, isMine);
 *   Physique.keep(player, Attributes.MAX_HEALTH, HEARTS, 4.0, AttributeModifier.Operation.ADD_VALUE, isMine);
 *   Physique.remove(player, Attributes.SCALE, TALL);
 *
 * The modifiers are transient: not saved with the player, so a passive that stops applying (a new Pathway, a
 * lost Sequence) never leaves its body behind after a relog. Call keep(...) once a second for every player, with
 * 'wanted' saying whether they should have it (not from SequencePassives, which only runs for those who do, so it
 * would never take it off): see bodyTick in HandOfGod.
 */
public final class Physique {

    private Physique() {}

    /** An id for a modifier, in this mod's namespace. Keep one per change (start it with your Pathway). */
    public static Identifier id(String name) {
        return Identifier.fromNamespaceAndPath(CircleOfInevitability.MOD_ID, name);
    }

    /** wanted: the modifier is on with this amount (changed if it had another). Not wanted: it is taken off. */
    public static void keep(LivingEntity entity, Holder<Attribute> attribute, Identifier id, double amount,
                            AttributeModifier.Operation operation, boolean wanted) {
        if (!wanted) {
            remove(entity, attribute, id);
            return;
        }
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance == null) return;
        AttributeModifier has = instance.getModifier(id);
        if (has != null && has.amount() == amount && has.operation() == operation) return;      // already right
        if (has != null) instance.removeModifier(id);
        instance.addTransientModifier(new AttributeModifier(id, amount, operation));
        if (entity.getHealth() > entity.getMaxHealth()) entity.setHealth(entity.getMaxHealth()); // less health: not above the new top
    }

    public static void remove(LivingEntity entity, Holder<Attribute> attribute, Identifier id) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance != null && instance.hasModifier(id)) instance.removeModifier(id);
        if (entity.getHealth() > entity.getMaxHealth()) entity.setHealth(entity.getMaxHealth());
    }

    public static boolean has(LivingEntity entity, Holder<Attribute> attribute, Identifier id) {
        AttributeInstance instance = entity.getAttribute(attribute);
        return instance != null && instance.hasModifier(id);
    }
}
