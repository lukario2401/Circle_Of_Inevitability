package net.lukario.circleofinevitability.item.armor;

import com.mojang.authlib.GameProfile;
import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.item.ModItems;
import net.lukario.circleofinevitability.status.Concealment;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.DeferredRegister;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/** Gray Fog set: smithing, Spirit Vision, the Paper Substitute, what Acrobatics may dodge, and the full-set veil. */
public final class GrayFogArmorGameTests {
    public static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, CircleOfInevitability.MOD_ID);
    static {
        FUNCTIONS.register("gray_fog_armor_equipment", () -> GrayFogArmorGameTests::equipment);
        FUNCTIONS.register("gray_fog_armor_spirit_vision", () -> GrayFogArmorGameTests::spiritVision);
        FUNCTIONS.register("gray_fog_armor_substitute", () -> GrayFogArmorGameTests::substitute);
        FUNCTIONS.register("gray_fog_armor_dodgeable", () -> GrayFogArmorGameTests::dodgeable);
        FUNCTIONS.register("gray_fog_armor_veil", () -> GrayFogArmorGameTests::veil);
    }
    private static ServerPlayer player(GameTestHelper h, EquipmentSlot slot) {
        var cookie=CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "Fog"+UUID.randomUUID().toString().substring(0,6)),false);
        ServerPlayer p=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),cookie.gameProfile(),cookie.clientInformation());
        Connection connection=new Connection(PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        h.getLevel().getServer().getPlayerList().placeNewPlayer(connection,p,cookie);
        p.setGameMode(GameType.SURVIVAL);
        p.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
        Vec3 at=h.absoluteVec(new Vec3(4.5,2,4.5));p.setPos(at.x,at.y,at.z);p.setNoGravity(true);
        if(slot!=null)p.setItemSlot(slot,new ItemStack(item(slot)));
        // Embedded test connections do not drive the player's tick; doTick runs Forge's living tick hook.
        h.onEachTick(p::doTick);
        h.addCleanup(ok -> {Concealment.reveal(p);h.getLevel().getServer().getPlayerList().remove(p);});
        return p;
    }
    private static net.minecraft.world.item.Item item(EquipmentSlot slot) {
        return switch(slot) {
            case HEAD -> ModItems.GRAY_FOG_HELMET.get();
            case CHEST -> ModItems.GRAY_FOG_CHESTPLATE.get();
            case LEGS -> ModItems.GRAY_FOG_LEGGINGS.get();
            default -> ModItems.GRAY_FOG_BOOTS.get();
        };
    }
    private static void equipSet(ServerPlayer p) {
        for(var slot:InevitabilityArmor.SLOTS)p.setItemSlot(slot,new ItemStack(item(slot)));
    }
    private static Mob zombie(GameTestHelper h,ServerPlayer p,double distance) {
        Mob m=h.spawnWithNoFreeWill(EntityTypes.ZOMBIE,1,2,1);m.setNoGravity(true);
        m.getPersistentData().putBoolean("circle_of_inevitability_beyonder_rolled",true);
        net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.BeyonderMobs.clear(m);
        m.setPos(p.getX()+distance,p.getY(),p.getZ());
        h.addCleanup(ok -> m.discard());return m;
    }
    private static void equipment(GameTestHelper h) {
        var p=player(h,EquipmentSlot.CHEST);
        h.assertTrue(!GrayFogArmor.fullSet(p) && GrayFogArmor.wearing(p,EquipmentSlot.CHEST),"Coat alone is not the full set");
        equipSet(p);h.assertTrue(GrayFogArmor.fullSet(p) && !UnshadowedArmor.fullSet(p) && !InevitabilityArmor.fullSet(p),"Four pieces activate only this set");
        for(var slot:InevitabilityArmor.SLOTS) {
            ItemStack stack=p.getItemBySlot(slot);
            h.assertTrue(stack.get(DataComponents.EQUIPPABLE).slot()==slot,"Correct equipment slot: "+slot);
            h.assertTrue(stack.has(DataComponents.ENCHANTABLE) && stack.has(DataComponents.REPAIRABLE),"Vanilla enchanting and repair components");
        }
        var bases=List.of(Items.NETHERITE_HELMET,Items.NETHERITE_CHESTPLATE,Items.NETHERITE_LEGGINGS,Items.NETHERITE_BOOTS);
        for(int i=0;i<4;i++) {
            ItemStack base=new ItemStack(bases.get(i));base.setDamageValue(9);
            var input=new net.minecraft.world.item.crafting.SmithingRecipeInput(
                    new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE),base,new ItemStack(ModItems.GRAY_FOG_CORE.get()));
            var recipe=h.getLevel().getServer().getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.SMITHING,input,h.getLevel()).orElseThrow();
            ItemStack result=recipe.value().assemble(input);
            h.assertTrue(result.is(item(InevitabilityArmor.SLOTS[i])) && result.getDamageValue()==9,"Smithing creates Gray Fog armor and keeps damage");
        }
        h.succeed();
    }
    private static void spiritVision(GameTestHelper h) {
        var p=player(h,EquipmentSlot.HEAD);var hunter=zombie(h,p,6);var idle=zombie(h,p,3);
        hunter.setTarget(p);
        h.runAfterDelay(15,() -> {
            h.assertTrue(hunter.hasEffect(MobEffects.GLOWING),"A mob hunting the wearer glows");
            h.assertTrue(!idle.hasEffect(MobEffects.GLOWING),"A mob that is not hunting stays unseen");
            h.succeed();
        });
    }
    private static void substitute(GameTestHelper h) {
        var p=player(h,EquipmentSlot.CHEST);var attacker=zombie(h,p,2);
        h.runAfterDelay(2,() -> {
            p.hurtServer(h.getLevel(),p.damageSources().mobAttack(attacker),2);
            h.assertTrue(p.getHealth()<20,"A small hit is not worth a figurine");
            h.assertTrue(GrayFogArmor.paperReadyAt(p)==0,"Figurine still ready");
            p.setHealth(20);p.invulnerableTime=0;
            // The small hit already wore the coat like any armor; the figurine costs 3 on top of that.
            int worn=p.getItemBySlot(EquipmentSlot.CHEST).getDamageValue();
            p.hurtServer(h.getLevel(),p.damageSources().mobAttack(attacker),12);
            h.assertTrue(p.getHealth()==20,"The figurine took a 60% hit; health="+p.getHealth());
            h.assertTrue(GrayFogArmor.paperReadyAt(p)>h.getLevel().getGameTime(),"The figurine is recharging");
            h.assertTrue(p.getItemBySlot(EquipmentSlot.CHEST).getDamageValue()==worn+3,"Burning the figurine costs coat durability");
        });
        h.runAfterDelay(30,() -> {
            p.hurtServer(h.getLevel(),p.damageSources().mobAttack(attacker),12);
            h.assertTrue(p.getHealth()<20,"While recharging a big hit lands");h.succeed();
        });
    }
    private static void dodgeable(GameTestHelper h) {
        var p=player(h,EquipmentSlot.LEGS);var attacker=zombie(h,p,2);
        h.assertTrue(GrayFogArmor.dodgeable(p,p.damageSources().mobAttack(attacker),4),"Melee swings can be dodged");
        h.assertTrue(!GrayFogArmor.dodgeable(p,p.damageSources().fall(),4),"Falls cannot be dodged");
        h.assertTrue(!GrayFogArmor.dodgeable(p,p.damageSources().onFire(),4),"Fire cannot be dodged");
        h.assertTrue(!GrayFogArmor.dodgeable(p,p.damageSources().explosion(attacker,attacker),4),"Explosions cannot be dodged");
        h.succeed();
    }
    private static void veil(GameTestHelper h) {
        var p=player(h,null);equipSet(p);p.setOnGround(true);p.setShiftKeyDown(true);
        h.runAfterDelay(10,() -> h.assertTrue(!Concealment.isHidden(p),"Half a second of crouching is not enough"));
        h.runAfterDelay(28,() -> {h.assertTrue(Concealment.isHidden(p) && GrayFogArmor.veiled(p),"Crouching still for a second vanishes into the fog");
            p.setShiftKeyDown(false);});
        h.runAfterDelay(32,() -> {h.assertTrue(!Concealment.isHidden(p),"Standing up steps back out of the fog");h.succeed();});
    }
}
