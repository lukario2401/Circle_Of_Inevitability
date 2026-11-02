package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.white_angel;
import java.util.function.Consumer;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.SoulCore;
import net.lukario.circleofinevitability.combat.Zone;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.registries.DeferredRegister;
public final class WhiteAngelGameTests {
    public static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS=DeferredRegister.create(Registries.TEST_FUNCTION,"circle_of_inevitability");
    static {FUNCTIONS.register("white_angel_costs",()->WhiteAngelGameTests::costs);}
    private static void costs(GameTestHelper h){
        var p=h.makeMockServerPlayerInLevel();p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        p.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
        var at=h.absoluteVec(new net.minecraft.world.phys.Vec3(1.5,1,1.5));p.setPos(at.x,at.y,at.z);
        SoulCore.setPathway(p,"White Angel");SoulCore.setSequence(p,4);SoulCore.setSpiritualityQuietly(p,20000);
        h.addCleanup(ok->{for(String n:java.util.List.of("domain","daytime","halo"))Zone.close(p,"white_angel_"+n);p.discard();});
        float before=SoulCore.getSpirituality(p);
        var domain=WhiteAngel.RADIANCE.entries().stream().filter(e->e.key().equals("domain")).findFirst().orElseThrow();
        WhiteAngel.RADIANCE.cast(p,domain);
        h.assertTrue(Zone.isOpen(p,"white_angel_domain"),"Domain opens");
        h.assertTrue(SoulCore.getSpirituality(p)==before-4000,"Domain costs 4000 once");
        SoulCore.setSpiritualityQuietly(p,0);
        WhiteAngel.RADIANCE.cast(p,domain);
        h.assertTrue(!Zone.isOpen(p,"white_angel_domain") && SoulCore.getSpirituality(p)==0,"Domain lowers without mana");
        SoulCore.setSpiritualityQuietly(p,1000);p.setShiftKeyDown(true);
        before=SoulCore.getSpirituality(p);WhiteAngel.key(p,p.level(),h.getLevel(),1,false);
        h.assertTrue(SoulCore.getSpirituality(p)==before-250,"Renewal cannot refund the caster");
        SoulCore.setSequence(p,9);before=SoulCore.getSpirituality(p);WhiteAngel.key(p,p.level(),h.getLevel(),2,false);
        h.assertTrue(SoulCore.getSpirituality(p)==before,"Sequence gate refuses Light at Bard");
        h.succeed();
    }
}
