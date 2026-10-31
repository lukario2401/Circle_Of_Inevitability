package net.lukario.circleofinevitability.combat;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
/** A reusable saved study library. Store under the soul tag to inherit SoulCore's death cloning. */
public final class AbilityStudy {
    public record Row(String id,String name,String icon,int sequence,double study,boolean favorite,boolean selected) {}
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
