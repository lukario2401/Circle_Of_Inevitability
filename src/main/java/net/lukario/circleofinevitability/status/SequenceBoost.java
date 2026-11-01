package net.lukario.circleofinevitability.status;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonders;
import net.minecraft.world.entity.LivingEntity;

/** A temporary casting gate. Never changes stored Sequence, health, spirituality or passive eligibility. */
public final class SequenceBoost {
    private static final String MARK="spellkit_sequence_boost";
    private SequenceBoost() {}
    public static void apply(LivingEntity e,int stronger,int ticks){Marks.add(e,MARK,Math.clamp(stronger,0,9),9,ticks);}
    public static int effective(LivingEntity e){return Math.max(0,Beyonders.sequenceOf(e)-Marks.count(e,MARK));}
    public static void end(LivingEntity e){Marks.clear(e,MARK);}
}
