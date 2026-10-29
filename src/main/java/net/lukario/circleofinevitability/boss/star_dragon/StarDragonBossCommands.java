package net.lukario.circleofinevitability.boss.star_dragon;

import com.mojang.brigadier.arguments.*;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.*;
import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.*;
import net.minecraft.commands.*;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.*;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.List;

/** OP commands for encounter authors. Settings persist in config; per-boss direction persists in entity saves. */
@Mod.EventBusSubscriber(modid=CircleOfInevitability.MOD_ID)
public final class StarDragonBossCommands {
    private static final SimpleCommandExceptionType NOT_BOSS=new SimpleCommandExceptionType(Component.literal("Select a Star Dragon boss."));
    private static final SimpleCommandExceptionType BAD_ABILITY=new SimpleCommandExceptionType(Component.literal("Unknown Star Dragon ability or unavailable at this boss's Sequence."));
    private StarDragonBossCommands() {}
    @SubscribeEvent public static void start(ServerStartingEvent e) {
        try { StarDragonBossSettings.reload(); }
        catch (Exception error) { CircleOfInevitability.LOGGER.error("Boss config rejected; retaining last valid settings",error); }
    }
    @SubscribeEvent public static void register(RegisterCommandsEvent event) {
        var root=Commands.literal("stardragon").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        root.then(Commands.literal("summon").executes(c -> summon(c,c.getSource().getPosition(),2))
                .then(Commands.argument("pos",Vec3Argument.vec3()).executes(c -> summon(c,Vec3Argument.getVec3(c,"pos"),2))));
        root.then(Commands.literal("summon_condenser").executes(c -> summon(c,c.getSource().getPosition(),0))
                .then(Commands.argument("pos",Vec3Argument.vec3()).executes(c -> summon(c,Vec3Argument.getVec3(c,"pos"),0))));
        root.then(Commands.literal("reload").executes(c -> {
            try { StarDragonBossSettings.reload(); }
            catch (Exception error) { c.getSource().sendFailure(Component.literal("Config rejected: "+error.getMessage()+". Previous settings remain active."));return 0; }
            int count=0;
            for (var level:c.getSource().getServer().getAllLevels()) for (Entity entity:level.getAllEntities())
                if (entity instanceof StarDragonBoss boss) { boss.applySettings();count++; }
            int applied=count; c.getSource().sendSuccess(() -> Component.literal("Reloaded Star Dragon settings on "+applied+" loaded boss(es)."),true);return 1;
        }));
        var mode=Commands.argument("bosses",EntityArgument.entities());
        for (String value:List.of("auto","manual","passive")) mode.then(Commands.literal(value).executes(c -> {
            int n=0;for (Entity e:EntityArgument.getEntities(c,"bosses")) if (e instanceof StarDragonBoss boss) { boss.mode(value);n++; }
            int changed=n;c.getSource().sendSuccess(() -> Component.literal("Set "+changed+" boss(es) to "+value+"."),true);return n;
        }));
        root.then(Commands.literal("mode").then(mode));
        root.then(Commands.literal("target").then(Commands.argument("boss",EntityArgument.entity())
                .then(Commands.argument("target",EntityArgument.entity()).executes(c -> {
                    Entity target=EntityArgument.getEntity(c,"target");if (!(target instanceof LivingEntity living)) return 0;
                    boss(c).target(living);return success(c,"Target locked.");
                }))));
        root.then(Commands.literal("clear_target").then(Commands.argument("boss",EntityArgument.entity()).executes(c -> {
            boss(c).clearTarget();return success(c,"Target lock cleared.");
        })));
        root.then(Commands.literal("cast").then(Commands.argument("boss",EntityArgument.entity()).then(abilityArg()
                .then(Commands.argument("target",EntityArgument.entity()).executes(c -> {
                    StarDragonBoss boss=boss(c);MobAbility ability=ability(c);Entity target=EntityArgument.getEntity(c,"target");
                    if (!(target instanceof LivingEntity living) || !boss.queueAttack(ability,living)) {
                        c.getSource().sendFailure(Component.literal("Cast refused: check mode, target, range, enabled attacks, spirituality, cooldown, form and blockers with /stardragon info."));return 0;
                    }
                    return success(c,"Winding up "+ability.name()+".");
                })))));
        root.then(Commands.literal("enable").then(Commands.argument("boss",EntityArgument.entity()).then(abilityArg()
                .then(Commands.argument("enabled",BoolArgumentType.bool()).executes(c -> {
                    MobAbility a=ability(c);boss(c).enable(a.id(),BoolArgumentType.getBool(c,"enabled"));return success(c,"Updated "+a.name()+".");
                })))));
        root.then(Commands.literal("weight").then(Commands.argument("boss",EntityArgument.entity()).then(abilityArg()
                .then(Commands.argument("weight",IntegerArgumentType.integer(0,10000)).executes(c -> {
                    MobAbility a=ability(c);boss(c).weight(a.id(),IntegerArgumentType.getInteger(c,"weight"));return success(c,"Updated selection weight for "+a.name()+".");
                })))));
        root.then(Commands.literal("reset").then(Commands.argument("boss",EntityArgument.entity()).executes(c -> {
            StarDragonBoss boss=boss(c);boss.resetAttacks();MobSpirituality.fill(boss);return success(c,"Reset attack overrides, cooldowns, and spirituality.");
        })));
        root.then(Commands.literal("info").then(Commands.argument("boss",EntityArgument.entity()).executes(c -> {
            StarDragonBoss b=boss(c);StringBuilder text=new StringBuilder("Sequence "+b.encounterSequence()+" Star Dragon | enraged="+b.enraged()+" | mode="+b.mode()+" | health="+b.getHealth()+"/"+b.getMaxHealth()
                    +" | spirituality="+Math.round(MobSpirituality.get(b))+"/"+Math.round(MobSpirituality.max(b))+" | animation="+b.animation());
            for (MobAbility base:MobAbilities.knownBy(b)) {var a=b.settings().attack(base);
                text.append("\n").append(base.id()).append(": ").append(MobCasting.whyNot(b,b.configuredAbility(base)).words())
                        .append(", enabled=").append(b.attackEnabled(base)).append(", weight=").append(b.attackWeight(base))
                        .append(", windup=").append(b.windupTicks(a)).append(", cooldown=").append(a.cooldownTicks()).append(", cost=").append(a.cost());}
            c.getSource().sendSuccess(() -> Component.literal(text.toString()),false);return 1;
        })));
        event.getDispatcher().register(root);
    }
    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack,String> abilityArg() {
        return Commands.argument("ability",StringArgumentType.string()).suggests((c,b) -> SharedSuggestionProvider.suggest(
                MobAbilities.of("Star Dragon").stream().map(a -> "\""+a.id()+"\""),b));
    }
    private static StarDragonBoss boss(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        if (EntityArgument.getEntity(c,"boss") instanceof StarDragonBoss boss) return boss;throw NOT_BOSS.create();
    }
    private static MobAbility ability(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        String id=StringArgumentType.getString(c,"ability");MobAbility a=MobAbilities.byId(id);
        if (a==null || !a.pathway().equals("Star Dragon") || a.sequence()<boss(c).encounterSequence()) throw BAD_ABILITY.create();return a;
    }
    private static int summon(CommandContext<CommandSourceStack> c,Vec3 at,int sequence) {
        LivingEntity boss=BeyonderMobs.summon(c.getSource().getLevel(),sequence==0?StarDragonBossRegistry.CONDENSER.get():StarDragonBossRegistry.BOSS.get(),at,"Star Dragon",sequence);
        return boss==null?0:success(c,"Summoned a Sequence "+sequence+" Star Dragon boss.");
    }
    private static int success(CommandContext<CommandSourceStack> c,String text) {c.getSource().sendSuccess(() -> Component.literal(text),false);return 1;}
}
