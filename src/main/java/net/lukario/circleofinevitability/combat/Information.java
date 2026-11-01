package net.lukario.circleofinevitability.combat;

import net.minecraft.world.entity.Entity;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** Shared read/write gate for connections, prediction, dossiers and study. */
public final class Information {
    private static final List<Predicate<Entity>> BLOCKERS=new ArrayList<>();
    private Information(){}
    public static void addBlocker(Predicate<Entity> rule){BLOCKERS.add(rule);}
    public static boolean blocked(Entity e){if(e==null)return false;for(var rule:BLOCKERS)if(rule.test(e))return true;return false;}
    public static boolean blocked(Entity reader,Entity target){return blocked(reader)||blocked(target);}
}
