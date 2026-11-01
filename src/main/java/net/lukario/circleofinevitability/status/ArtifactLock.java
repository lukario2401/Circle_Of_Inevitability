package net.lukario.circleofinevitability.status;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;

/** Locks only Sealed Artifacts; ordinary weapons, food, potions and ability keys still work. */
public final class ArtifactLock {
    private static final String MARK="spellkit_artifact_lock";
    private ArtifactLock(){}
    public static void apply(LivingEntity e,int ticks){Marks.add(e,MARK,1,Math.max(ticks,Marks.ticksLeft(e,MARK)));}
    public static boolean has(Entity e){return Marks.count(e,MARK)>0;}
    public static void clear(LivingEntity e){Marks.clear(e,MARK);}
}
