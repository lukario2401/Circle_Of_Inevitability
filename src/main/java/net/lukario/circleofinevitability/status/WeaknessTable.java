package net.lukario.circleofinevitability.status;

import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * What something is weak to, as data: a list of rules read from the top, the first that fits wins, and a word for
 * everything else. Each Pathway keeps a table of its own (its own words), so two Pathways never fight over one:
 *
 *   private static final WeaknessTable WEAKNESS = WeaknessTable.otherwise("pierce")
 *           .rule("starlight", one -> one.is(EntityTypeTags.UNDEAD) || Marionette.isSummoned(one))
 *           .rule("corrosion", one -> one.getArmorValue() >= 10 || Construct.isConstruct(one))
 *           .rule("gravity",   one -> one.isNoGravity());
 *
 *   String weak = WEAKNESS.of(target);          // "starlight", ... or "pierce"
 *
 * To change what is weak to what, change the rules (their order counts). Hand the answer to Appraisal.mark(...),
 * or use it on its own (a coating that bites harder on what it fits).
 */
public final class WeaknessTable {

    private record Rule(String weakness, Predicate<LivingEntity> fits) {}

    private final List<Rule> rules = new ArrayList<>();
    private final String otherwise;

    private WeaknessTable(String otherwise) {
        this.otherwise = otherwise;
    }

    /** A table whose answer is 'otherwise' when no rule fits. */
    public static WeaknessTable otherwise(String otherwise) {
        return new WeaknessTable(otherwise);
    }

    /** One more rule, read after the ones before it. */
    public WeaknessTable rule(String weakness, Predicate<LivingEntity> fits) {
        rules.add(new Rule(weakness, fits));
        return this;
    }

    /** What 'one' is weak to: the first rule that fits, else the table's 'otherwise'. */
    public String of(LivingEntity one) {
        for (Rule rule : rules) {
            if (rule.fits().test(one)) return rule.weakness();
        }
        return otherwise;
    }
}
