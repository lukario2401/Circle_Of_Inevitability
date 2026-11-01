package net.lukario.circleofinevitability.combat;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
/** A reusable saved study library. Store under the soul tag to inherit SoulCore's death cloning. */
public final class AbilityStudy {
    public record Row(String id,String name,String icon,int sequence,double study,boolean favorite,boolean selected) {
        /** Derived from the stable identity so it survives saves and existing packet formats. */
        public boolean fromWheel(){return id.startsWith("wheel/");}
    }
    private final String name;
    public AbilityStudy(String name) { this.name=name; }
    public CompoundTag data(Player p) {
        CompoundTag all=p.getPersistentData();
        if(!all.contains("circle_of_inevitability"))all.put("circle_of_inevitability",new CompoundTag());
        CompoundTag soul=all.getCompoundOrEmpty("circle_of_inevitability");
        if(!soul.contains(name))soul.put(name,new CompoundTag());
        return soul.getCompoundOrEmpty(name);
    }
    public double progress(Player p,String id) {
        double value=data(p).getDoubleOr(id,0);
        return Double.isFinite(value)?Math.clamp(value,0,100):0;
    }
    public boolean complete(Player p,String id) { return progress(p,id)>=100; }
    public double add(Player p,String id,double amount) {
        double next=Math.clamp(progress(p,id)+Math.max(0,Double.isFinite(amount)?amount:0),0,100);
        data(p).putDouble(id,next);return next;
    }
    public boolean select(Player p,String id) {
        if(!complete(p,id))return false;
        data(p).putString("selected",id);return true;
    }
    public String selected(Player p){return data(p).getStringOr("selected","");}
    /** Removes a record and any favorite/selection pointing at it, including obsolete catalog IDs. */
    public boolean forget(Player p,String id){
        CompoundTag library=data(p);
        boolean known=library.contains(id)||library.contains("favorite/"+id)||selected(p).equals(id);
        library.remove(id);library.remove("favorite/"+id);
        if(selected(p).equals(id))library.remove("selected");
        return known;
    }
    /** Clears only this named library; other soul data and other libraries are preserved. */
    public int forgetAll(Player p){
        CompoundTag library=data(p);int count=0;
        for(String id:library.keySet())if(!id.equals("selected")&&!id.startsWith("favorite/")&&library.getDoubleOr(id,0)>0)count++;
        p.getPersistentData().getCompoundOrEmpty("circle_of_inevitability").remove(name);
        return count;
    }
    public boolean favorite(Player p,String id){return data(p).getBooleanOr("favorite/"+id,false);}
    public void toggleFavorite(Player p,String id){if(progress(p,id)>0)data(p).putBoolean("favorite/"+id,!favorite(p,id));}
    public java.util.List<Row> rows(Player p,java.util.Collection<AbilityCatalog.Power> powers){
        java.util.List<Row> rows=new java.util.ArrayList<>();
        for(var power:powers){double study=progress(p,power.id());if(study>0)rows.add(new Row(power.id(),power.name(),power.icon(),power.sequence(),study,favorite(p,power.id()),selected(p).equals(power.id())));}
        rows.sort(java.util.Comparator.comparing(Row::favorite).reversed().thenComparing(java.util.Comparator.comparingDouble(Row::study).reversed())
                .thenComparingInt(Row::sequence).thenComparing(Row::name,String.CASE_INSENSITIVE_ORDER));
        return java.util.List.copyOf(rows);
    }
}
