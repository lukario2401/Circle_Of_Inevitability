package net.lukario.circleofinevitability.combat;
import java.util.*;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonders;
import net.lukario.circleofinevitability.status.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
/** Reusable entity inspection by depth: body, defenses, then pathway and cast history. */
public final class Dossier {
    public static List<Component> lines(ServerPlayer viewer,LivingEntity target,int depth){
        List<Component> out=new ArrayList<>();
        String mood=Fear.isAfraid(target)?"afraid":target instanceof Mob mob && mob.getTarget()!=null?"furious":"calm";
        out.add(Component.literal(target.getName().getString()+" | "+target.getHealth()+"/"+target.getMaxHealth()+" HP | "+mood));
        for(var effect:target.getActiveEffects())out.add(Component.literal(effect.toString()));
        if(depth>=2)out.add(Component.literal("Armor "+target.getArmorValue()+" | Substitute "+Substitute.isArmed(target)+" | Death ward "+DeathWard.isArmed(target)+" | Madness "+Madness.get(target)));
        if(depth>=3){
            out.add(Component.literal(Beyonders.describe(target)));
            for(var a:Beyonders.abilitiesOf(target))out.add(Component.literal(a.name()+" — Sequence "+a.sequence()));
            for(var power:AbilityCatalog.of(target))if(power.cast().entry()!=null)out.add(Component.literal(power.name()+" — Sequence "+power.sequence()));
            var cast=Casts.last(target);if(cast!=null && cast.age(viewer.level())<=600)out.add(Component.literal("Last cast: "+cast.name()));
        }
        return List.copyOf(out);
    }
    public static void tell(ServerPlayer viewer,LivingEntity target,int depth){lines(viewer,target,depth).forEach(viewer::sendSystemMessage);}
}
