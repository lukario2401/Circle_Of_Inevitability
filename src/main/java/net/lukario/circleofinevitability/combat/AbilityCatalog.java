package net.lukario.circleofinevitability.combat;
import java.util.*;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonders;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.PathwayAbilities;
import net.lukario.circleofinevitability.menu.AbilityWheel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
/** Stable IDs for native key variants and individual wheel entries, usable by any learning/inspection power. */
public final class AbilityCatalog {
    private AbilityCatalog() {}
    public record Power(String id, String name, String icon, int sequence, Casts.Cast cast) {}
    public static String id(Casts.Cast cast) {
        return cast.entry()!=null ? "wheel/"+cast.wheel().id()+"/"+cast.entry().key()
                : "key/"+cast.pathway()+"/"+cast.key()+"/"+cast.sneak();
    }
    public static Map<String,Power> all() {
        AbilityCatalogBootstrap.initialize();
        Map<String,Power> result=new LinkedHashMap<>();
        UUID nobody=new UUID(0,0);
        for (var a:PathwayAbilities.ALL) if (a.stealable()) {
            for (boolean sneak:new boolean[]{false,true}) {
                if (sneak && !a.sneakForm()) continue;
                var cast=new Casts.Cast(nobody,"",a.pathway(),a.key(),sneak,null,null,a,"",Vec3.ZERO,0);
                String slug=a.pathway().toLowerCase(Locale.ROOT).replace(' ','_');
                String icon="ability_menus/"+slug+"/"+cast.name().replace(" (sneak)","").toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+","_");
                result.put(id(cast),new Power(id(cast),a.pathway()+": "+cast.name(),icon,a.sequence(),cast));
            }
        }
        for (var wheel:AbilityWheel.registered()) for (var e:wheel.entries()) if (e.copyable()) {
            var cast=new Casts.Cast(nobody,"",wheel.pathway(),0,false,wheel,e,null,"",Vec3.ZERO,0);
            result.put(id(cast),new Power(id(cast),e.name(),e.icon(),e.sequence(),cast));
        }
        return result;
    }
    public static List<Power> of(LivingEntity entity) {
        String pathway=Beyonders.pathwayOf(entity);int sequence=Beyonders.sequenceOf(entity);
        return all().values().stream().filter(p->p.sequence()>=sequence && p.cast().pathway().equals(pathway)).toList();
    }
}
