package net.lukario.circleofinevitability.combat;

/** Reusable, renderer-independent growth clock. The owning garden supplies time and speed,
 * handles stage changes, pulses mature effects and removes DEAD entries. Nothing loads chunks.
 * See docs/pathways/nature_walker/NATURE_WALKER_EXTENSION_GUIDE.md for a complete integration example. */
public final class Growth {
    public enum Stage { SEED, SPROUT, BLOOM, WITHERED, DEAD }
    private final int sprout, mature, wither;
    private double age;
    private long expires, witherUntil;
    public Growth(int sproutTicks,int matureTicks,int witherTicks) {
        if(sproutTicks<0||matureTicks<sproutTicks||witherTicks<1)throw new IllegalArgumentException("Invalid growth timing");
        sprout=sproutTicks;mature=matureTicks;wither=witherTicks;
    }
    public double age() { return age; }
    public boolean mature() { return age>=mature; }
    public boolean withering() { return witherUntil!=0; }
    public void advance(double ticks) { if(!withering()&&Double.isFinite(ticks)&&ticks>0)age+=ticks; }
    public void ripen() { if(!withering())age=Math.max(age,mature); }
    /** Shorten the mature life, e.g. after its fruit has been eaten. */
    public void expireBy(long tick) { if(expires>0)expires=Math.min(expires,tick); }
    public Stage tick(long now,double speed,int lifetime) {
        if(witherUntil>0)return now>=witherUntil?Stage.DEAD:Stage.WITHERED;
        advance(speed);
        if(mature()&&expires==0)expires=now+Math.max(1,lifetime);
        if(expires>0&&now>=expires) { witherUntil=now+wither;return Stage.WITHERED; }
        return mature()?Stage.BLOOM:age>=sprout?Stage.SPROUT:Stage.SEED;
    }
}
