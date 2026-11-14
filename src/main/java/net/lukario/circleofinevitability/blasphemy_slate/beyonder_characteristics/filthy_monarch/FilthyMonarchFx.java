package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.filthy_monarch;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.particles.fx.ModelFx;
import net.lukario.circleofinevitability.particles.fx.ParticleFx;
import net.lukario.circleofinevitability.particles.fx.ParticleShapes;
import net.lukario.circleofinevitability.sounds.ModSounds;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Presentation only. Callers own targeting, timing, damage, forms and status lifecycles.
 * All geometry expires and can be cleared by its caller-supplied unique cast tag.
 * No terrain changes, entity AI, ability registration or resource costs occur here.
 */
@Mod.EventBusSubscriber(modid = "circle_of_inevitability")
public final class FilthyMonarchFx {
    private FilthyMonarchFx() {}
    public static final int BLUE = 0xFF7FD8FF, CORE = 0xFFE6FAFF, BLACK = 0xFF120A0E,
            RED = 0xFFC80815, BLOOD = 0xFF8A0F1A, FILTH = 0xFF5C5A1E;
    public enum Ability {
        LAWLESS_LUNGE("claw_slash"),
        DIRTY_TRICK("grit_burst"),
        SULFUR_FLAME("flame_jet"),
        SINK("sink_seal"),
        KILLING_SPREE("claw_slash", "bloodlust_crown"),
        DEVIL_WORSHIP("ritual_sigil", "devil_projection"),
        SULFUR_FIREBALL("sulfur_fireball"),
        DEVIL_FORM("devil_form", "demon_form"),
        SWORD_OF_LAVA("lava_sword"),
        FLAME_CAGE("flame_cage"),
        VOLCANIC_ERUPTION("warning_ring", "eruption_pillar"),
        TOXIC_VAPOR("toxic_cloud", "corrosive_cloud"),
        HELLFIRE("hellfire_pillar"),
        FLAMES_OF_THE_ABYSS("abyss_flame_beam"),
        SHADOW_SHIFT("shadow_trail", "shadow_afterimage"),
        DESIRE_CONTROL("desire_heart"),
        MENTAL_SHOCK("horn_crown", "mind_wave"),
        DESIRE_INCARNATION("desire_liquid"),
        DESIRE_SYMBIOSIS("symbiosis_link"),
        FILTHY_ILLUSION("filth_phantom"),
        SEED_OF_MALICE("malice_seed"),
        SLOW("word_slow"),
        DIE("word_die", "blood_spray"),
        CORRUPTION("corrupted_fog"),
        CONFUSION("word_confusion"),
        SLOTH("word_sloth"),
        BLASPHEMY("word_blasphemy"),
        BLATHER("word_blather"),
        ACID_SWAMP("acid_pool"),
        BLOOD_IGNITION("burning_heart"),
        BLOOD_BURST("blood_spray"),
        TIDE_OF_FILTH("sludge_wave", "filth_hand", "sludge_pool"),
        THE_CORRODER("corrosion_beam", "barrier_fracture"),
        ABYSSAL_SEA("kingdom_tile", "black_sea"),
        THE_ABYSS("abyss_form");
        private final List<String> models;
        Ability(String... models) { this.models = List.of(models); }
        public String id() { return name().toLowerCase(Locale.ROOT); }
        public String icon() { return "ability_menus/filthy_monarch/" + id(); }
        public List<String> models() { return models; }
    }
    public static final Set<String> ANIMATED = Set.of(
            "devil_form", "demon_form", "devil_projection", "desire_liquid", "filth_phantom", "abyss_form", "filth_hand");
    public static final List<String> MODELS = List.of(
            "claw_slash", "grit_burst", "flame_jet", "sink_seal", "bloodlust_crown", "ritual_sigil", "devil_projection", "sulfur_fireball", "devil_form", "demon_form", "lava_sword", "flame_cage", "warning_ring", "eruption_pillar", "toxic_cloud", "corrosive_cloud", "hellfire_pillar", "abyss_flame_beam", "shadow_trail", "shadow_afterimage", "desire_heart", "horn_crown", "mind_wave", "desire_liquid", "symbiosis_link", "filth_phantom", "malice_seed", "word_slow", "word_die", "blood_spray", "corrupted_fog", "word_confusion", "word_sloth", "word_blasphemy", "word_blather", "acid_pool", "burning_heart", "sludge_wave", "filth_hand", "sludge_pool", "corrosion_beam", "barrier_fracture", "kingdom_tile", "black_sea", "abyss_form", "depravity_seed", "offering_sigil", "danger_marker", "blood_drop", "rebirth_ring", "corrosion_mark");
    public static final List<String> PARTICLES = List.of(
            "sulfur_flame", "black_ember", "blood_drop", "sludge", "fog", "seed", "claw", "spark", "ripple", "foul_glyph", "shadow", "corrosion_streak", "danger_eye", "offering", "horn");

    private static void requireTag(String tag) {
        if (tag == null || tag.isBlank()) throw new IllegalArgumentException("A nonempty cast tag is required");
    }
    /** Animated forms have a 360-tick lease (61 keys); refresh them before expiry. */
    public static ModelFx model(String name, int ticks, String tag) {
        requireTag(tag);
        if (!MODELS.contains(name)) throw new IllegalArgumentException("Unknown Filthy Monarch model: " + name);
        int life = Math.clamp(ticks, 1, ANIMATED.contains(name) ? 360 : 6000);
        ModelFx fx = ModelFx.of("filthy_monarch/" + name).pivot(8,0,8).lifetime(life)
                .fade(Math.min(4,life/4),Math.min(8,life/4)).tag(tag);
        if (ANIMATED.contains(name)) {
            fx = fx.frames(4).smooth();
            for (int tick=0; tick<=life; tick+=6) fx=fx.key(tick,ModelFx.pose().frame((tick/6)%4));
        }
        if (Set.of("flame_jet", "sulfur_fireball", "lava_sword", "flame_cage", "eruption_pillar",
                "hellfire_pillar", "abyss_flame_beam", "warning_ring", "horn_crown").contains(name))
            return fx.glow().unshaded();
        if (Set.of("toxic_cloud", "corrosive_cloud", "corrupted_fog", "shadow_afterimage").contains(name))
            return fx.color(0x88FFFFFF);
        return fx;
    }
    public static ParticleFx particle(String name, int color) {
        if (!PARTICLES.contains(name)) throw new IllegalArgumentException("Unknown Filthy Monarch particle: " + name);
        return ParticleFx.of("fx/filthy_monarch/"+name).color(color).endColor(color & 0x00FFFFFF)
                .size(.18f).endSize(.04f).lifetime(24,6).friction(.94f).randomRotation();
    }
    public static void sound(ServerLevel sl, Vec3 at, String name) {
        var event=ModSounds.FILTHY_MONARCH.get(name);
        if (event==null) throw new IllegalArgumentException("Unknown Filthy Monarch sound: "+name);
        Spells.sound(sl,at,event.get(),.65f,1f);
    }
    private static void at(ServerLevel sl,Vec3 at,String name,int ticks,String tag,float scale) {
        ParticleShapes.model(sl,model(name,ticks,tag).scale(scale),at,0,0,0);
    }
    private static void burst(ServerLevel sl,Vec3 at,String sprite,int color,int count) {
        ParticleShapes.burst(sl,particle(sprite,color),at,count,.025,.16);
    }
    public static void form(ServerLevel sl,LivingEntity entity,String name,int ticks,String tag,float scale) {
        if (!ANIMATED.contains(name)) throw new IllegalArgumentException("Unknown form: "+name);
        requireTag(tag);
        ParticleShapes.clearModels(sl,entity,tag);
        ParticleShapes.modelOnTurning(sl,model(name,ticks,tag).scale(Math.clamp(scale,.1f,4f)),entity,Vec3.ZERO,0,0,0);
    }
    /** Stateless overhead marks. The caller owns count, ownership, expiration and refresh. */
    public static void seeds(ServerLevel sl,LivingEntity target,int count,int ticks,String tag) {
        requireTag(tag);ParticleShapes.clearModels(sl,target,tag);
        int stacks=Math.clamp(count,0,5);
        for (int i=0;i<stacks;i++) ParticleShapes.modelOn(sl,model("depravity_seed",ticks,tag).scale(.16f),
                target,new Vec3((i-(stacks-1)/2.0)*.24,target.getBbHeight()+.3,0),0,0,0);
    }
    /** Private passive warning: neither marker nor chime is broadcast to other players. */
    public static void danger(ServerPlayer viewer,LivingEntity threat,boolean premonition) {
        String cue=premonition ? "danger_premonition" : "instinct";
        viewer.connection.send(new ClientboundSoundPacket(
                BuiltInRegistries.SOUND_EVENT.wrapAsHolder(ModSounds.FILTHY_MONARCH.get(cue).get()),
                SoundSource.PLAYERS,viewer.getX(),viewer.getY(),viewer.getZ(),.45f,1f,viewer.getRandom().nextLong()));
        ParticleShapes.spawnFor(viewer,particle("danger_eye",RED).size(.4f),
                threat.position().add(0,threat.getBbHeight()+.4,0),Vec3.ZERO);
    }
    /** Fixed geometry budget: 32 open lattice tiles plus a sea disc, regardless of radius. */
    public static void kingdom(ServerLevel sl,Vec3 center,float radius,int ticks,String tag) {
        requireTag(tag);float r=Math.clamp(radius,1,32);
        ParticleShapes.clearModels(sl,center,tag);
        for (int row=0;row<4;row++) {
            double elevation=(row+.5)*Math.PI/4-Math.PI/2;
            for (int i=0;i<8;i++) {
                double angle=i*Math.PI/4;
                Vec3 offset=new Vec3(Math.cos(angle)*Math.cos(elevation),Math.sin(elevation),Math.sin(angle)*Math.cos(elevation)).scale(r);
                ParticleShapes.modelFacing(sl,model("kingdom_tile",ticks,tag).pivot(8,8,8).scale(r*.75f),center.add(offset),offset.normalize());
            }
        }
        at(sl,center.add(0,.03,0),"black_sea",ticks,tag,r*1.7f);
    }
    /** Thin Y-axis beam fitted to endpoints. Caller repeats this for a channel's beat. */
    public static void beam(ServerLevel sl,Vec3 from,Vec3 to,boolean corrosion,int ticks,String tag) {
        Vec3 delta=to.subtract(from);double length=Math.min(64,delta.length());
        if (length<.001) return;
        ModelFx fx=model(corrosion ? "corrosion_beam" : "abyss_flame_beam",ticks,tag)
                .scale(corrosion ? .45f : 1.5f,(float)(length/(corrosion ? 2 : 1.625)),corrosion ? .45f : 1.5f);
        ParticleShapes.modelAlong(sl,fx,from,delta.normalize());
    }
    public static void melee(ServerLevel sl,LivingEntity caster,Vec3 hit,int combo,boolean impact) {
        int step=Math.floorMod(combo,6);
        burst(sl,hit,impact ? "blood_drop" : "claw",impact ? BLOOD : BLUE,12+step*2);
        sound(sl,hit,impact ? "melee_impact" : "melee_swing");
    }
    /** Geometry-only cast vignette; endpoints and durations come from the gameplay caller. */
    public static void cast(ServerLevel sl,LivingEntity caster,Vec3 target,Ability ability,int ticks,String tag) {
        requireTag(tag);int life=Math.clamp(ticks,1,6000);
        sound(sl,target,ability.id());
        switch (ability) {
            case DEVIL_FORM -> form(sl,caster,"devil_form",life,tag,1);
            case DESIRE_INCARNATION -> form(sl,caster,"desire_liquid",life,tag,1);
            case THE_ABYSS -> form(sl,caster,"abyss_form",life,tag,1.5f);
            case ABYSSAL_SEA -> kingdom(sl,target,16,life,tag);
            case THE_CORRODER, FLAMES_OF_THE_ABYSS -> beam(sl,caster.getEyePosition(),target,
                    ability==Ability.THE_CORRODER,Math.min(life,100),tag);
            case SULFUR_FLAME -> {
                Vec3 delta=target.subtract(caster.getEyePosition());
                ParticleShapes.modelAlong(sl,model("flame_jet",Math.min(life,30),tag).scale(1,4,1),caster.getEyePosition(),delta);
                ParticleShapes.cone(sl,particle("sulfur_flame",BLUE),caster.getEyePosition(),delta.normalize(),70,40,.08,.3);
            }
            case SULFUR_FIREBALL -> {
                Vec3 origin=caster.getEyePosition(),delta=target.subtract(origin);
                for (int i=-1;i<=1;i++) ParticleShapes.modelAlong(sl,model("sulfur_fireball",24,tag).scale(.7f)
                        .key(0,ModelFx.pose().right(i*.6f)).key(20,ModelFx.pose().up((float)Math.min(40,delta.length()))),origin,delta);
            }
            case VOLCANIC_ERUPTION -> {
                at(sl,target,"warning_ring",Math.min(life,20),tag,4);
                ParticleShapes.model(sl,model("eruption_pillar",Math.max(40,Math.min(life,80)),tag).scale(3)
                        .key(0,ModelFx.pose().scale(0)).key(19,ModelFx.pose().scale(0))
                        .key(26,ModelFx.pose().scale(1)),target,0,0,0);
            }
            case DEVIL_WORSHIP -> {
                at(sl,target,"ritual_sigil",life,tag,3);
                ParticleShapes.model(sl,model("devil_projection",life,tag).key(0,ModelFx.pose().up(-2))
                        .key(20,ModelFx.pose().up(0)),target,0,0,0);
            }
            case FILTHY_ILLUSION -> {
                Vec3 from=caster.position(),delta=target.subtract(from);
                for (int i=-1;i<=1;i++) ParticleShapes.modelFacing(sl,model("filth_phantom",Math.min(120,life),tag)
                        .key(0,ModelFx.pose().right(i*1.4f)).key(30,ModelFx.pose().forward((float)Math.min(24,delta.length()))),from,delta);
            }
            case TIDE_OF_FILTH -> {
                Vec3 from=caster.position(),delta=target.subtract(from);
                ParticleShapes.modelFacing(sl,model("sludge_wave",Math.min(life,80),tag).scale(4)
                        .key(0,ModelFx.pose().forward(0)).key(40,ModelFx.pose().forward((float)Math.min(24,delta.length()))),from,delta);
                at(sl,target,"sludge_pool",life,tag,8);
                for (int i=0;i<6;i++) {
                    double a=i*Math.PI/3;at(sl,target.add(Math.cos(a)*3,0,Math.sin(a)*3),"filth_hand",Math.min(life,80),tag,1.2f);
                }
            }
            case SHADOW_SHIFT -> {
                at(sl,caster.position(),"shadow_afterimage",30,tag,1);
                ParticleShapes.line(sl,particle("shadow",BLACK).size(.4f),caster.position(),target,3);
                burst(sl,target,"shadow",BLACK,28);
            }
            case DESIRE_SYMBIOSIS -> {
                ParticleShapes.line(sl,particle("blood_drop",RED),caster.getEyePosition(),target,3);
                at(sl,target,"symbiosis_link",life,tag,1);
            }
            case LAWLESS_LUNGE, KILLING_SPREE, SWORD_OF_LAVA -> {
                int count=ability==Ability.KILLING_SPREE ? 3 : 1;
                for (int i=0;i<count;i++) ParticleShapes.modelOnTurning(sl,
                        model(ability.models().getFirst(),24+i*4,tag).key(0,ModelFx.pose().yaw(-60+i*30))
                                .key(20,ModelFx.pose().yaw(80+i*30)),caster,new Vec3(0,.6,1),0,0,0);
                burst(sl,target,"claw",BLUE,18);
            }
            case BLOOD_BURST, BLOOD_IGNITION -> {
                at(sl,target,ability.models().getFirst(),Math.min(life,120),tag,2);
                burst(sl,target.add(0,1,0),"blood_drop",BLOOD,40);
            }
            default -> {
                at(sl,target,ability.models().getFirst(),life,tag,
                        ability==Ability.ACID_SWAMP ? 8 : ability==Ability.FLAME_CAGE ? 2 : 1.5f);
                boolean foul=ability.ordinal()>=Ability.SLOW.ordinal() && ability.ordinal()<=Ability.BLATHER.ordinal();
                burst(sl,target.add(0,1,0),foul ? "foul_glyph" : "fog",foul ? RED : FILTH,24);
            }
        }
    }
    public static void end(ServerLevel sl,LivingEntity entity,Vec3 at,String tag) {
        requireTag(tag);ParticleShapes.clearModels(sl,entity,tag);ParticleShapes.clearModels(sl,at,tag);
    }
    private static List<String> abilityNames() { return Arrays.stream(Ability.values()).map(Ability::id).toList(); }
    private static String previewTag(ServerPlayer p) { return "filthy_preview_"+p.getUUID(); }
    private static Vec3 previewPoint(ServerPlayer p) {
        Vec3 v=p.getLookAngle();return p.position().add(new Vec3(v.x,0,v.z).normalize().scale(5));
    }
    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("filthyfx")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(ctx -> {ctx.getSource().sendSuccess(() -> Component.literal(
                        "/filthyfx <ability> | model <id> | sound <id> | particles | seeds | stop"),false);return 1;})
                .then(Commands.literal("stop").executes(ctx -> {
                    ServerPlayer p=ctx.getSource().getPlayerOrException();end(p.level(),p,previewPoint(p),previewTag(p));return 1;
                }))
                .then(Commands.literal("model").then(Commands.argument("id",StringArgumentType.word())
                        .suggests((ctx,b)->SharedSuggestionProvider.suggest(MODELS,b)).executes(ctx -> {
                            ServerPlayer p=ctx.getSource().getPlayerOrException();String id=StringArgumentType.getString(ctx,"id");
                            if (!MODELS.contains(id)) {ctx.getSource().sendFailure(Component.literal("Unknown model: "+id));return 0;}
                            end(p.level(),p,previewPoint(p),previewTag(p));at(p.level(),previewPoint(p),id,160,previewTag(p),1.5f);return 1;
                        })))
                .then(Commands.literal("sound").then(Commands.argument("id",StringArgumentType.word())
                        .suggests((ctx,b)->SharedSuggestionProvider.suggest(ModSounds.FILTHY_MONARCH.keySet(),b)).executes(ctx -> {
                            ServerPlayer p=ctx.getSource().getPlayerOrException();String id=StringArgumentType.getString(ctx,"id");
                            if (!ModSounds.FILTHY_MONARCH.containsKey(id)) {ctx.getSource().sendFailure(Component.literal("Unknown sound: "+id));return 0;}
                            sound(p.level(),p.position(),id);return 1;
                        })))
                .then(Commands.literal("seeds").executes(ctx -> {
                    ServerPlayer p=ctx.getSource().getPlayerOrException();seeds(p.level(),p,5,160,previewTag(p));return 1;
                }))
                .then(Commands.literal("particles").executes(ctx -> {
                    ServerPlayer p=ctx.getSource().getPlayerOrException();
                    for(int i=0;i<PARTICLES.size();i++) burst(p.level(),previewPoint(p).add((i%4-1.5)*.8,.5+i/4*.6,0),PARTICLES.get(i),i%2==0?BLUE:RED,6);
                    return 1;
                }))
                .then(Commands.argument("ability",StringArgumentType.word())
                        .suggests((ctx,b)->SharedSuggestionProvider.suggest(abilityNames(),b)).executes(ctx -> {
                            ServerPlayer p=ctx.getSource().getPlayerOrException();String id=StringArgumentType.getString(ctx,"ability");
                            if (!abilityNames().contains(id)) {ctx.getSource().sendFailure(Component.literal("Unknown ability: "+id));return 0;}
                            end(p.level(),p,previewPoint(p),previewTag(p));
                            cast(p.level(),p,previewPoint(p),Ability.valueOf(id.toUpperCase(Locale.ROOT)),160,previewTag(p));return 1;
                        })));
    }
}
