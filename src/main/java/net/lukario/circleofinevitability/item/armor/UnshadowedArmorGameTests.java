package net.lukario.circleofinevitability.item.armor;

import com.mojang.authlib.GameProfile;
import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.item.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.DeferredRegister;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/** Unshadowed set: smithing, purification of undead, longer spear reach, full-set reach and the light ward. */
public final class UnshadowedArmorGameTests {
    public static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, CircleOfInevitability.MOD_ID);
    static {
        FUNCTIONS.register("unshadowed_armor_equipment", () -> UnshadowedArmorGameTests::equipment);
        FUNCTIONS.register("unshadowed_armor_purify", () -> UnshadowedArmorGameTests::purify);
        FUNCTIONS.register("unshadowed_armor_reach", () -> UnshadowedArmorGameTests::reach);
        FUNCTIONS.register("unshadowed_armor_full_set", () -> UnshadowedArmorGameTests::fullSet);
        FUNCTIONS.register("unshadowed_armor_ward", () -> UnshadowedArmorGameTests::ward);
    }
    private static ServerPlayer player(GameTestHelper h) {
        var cookie=CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "Sun"+UUID.randomUUID().toString().substring(0,6)),false);
        ServerPlayer p=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),cookie.gameProfile(),cookie.clientInformation());
        Connection connection=new Connection(PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        h.getLevel().getServer().getPlayerList().placeNewPlayer(connection,p,cookie);
        p.setGameMode(GameType.SURVIVAL);
        p.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
        Vec3 at=h.absoluteVec(new Vec3(4.5,2,4.5));p.setPos(at.x,at.y,at.z);p.setNoGravity(true);
        p.setItemSlot(EquipmentSlot.CHEST,new ItemStack(ModItems.UNSHADOWED_CHESTPLATE.get()));
        // Embedded test connections do not drive the player's tick; doTick runs Forge's living tick hook.
        h.onEachTick(p::doTick);
        h.addCleanup(ok -> {TentacleCombat.cancel(p);h.getLevel().getServer().getPlayerList().remove(p);});
        return p;
    }
    private static Mob target(GameTestHelper h,ServerPlayer p,EntityType<? extends Mob> type,double distance) {
        Mob m=h.spawnWithNoFreeWill(type,1,2,1);m.setNoGravity(true);
        m.getPersistentData().putBoolean("circle_of_inevitability_beyonder_rolled",true);
        net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.BeyonderMobs.clear(m);
        m.setPos(p.getX()+distance,p.getY(),p.getZ());
        m.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);m.setHealth(100);
        h.addCleanup(ok -> m.discard());return m;
    }
    private static void equipSet(ServerPlayer p) {
        p.setItemSlot(EquipmentSlot.HEAD,new ItemStack(ModItems.UNSHADOWED_HELMET.get()));
        p.setItemSlot(EquipmentSlot.LEGS,new ItemStack(ModItems.UNSHADOWED_LEGGINGS.get()));
        p.setItemSlot(EquipmentSlot.FEET,new ItemStack(ModItems.UNSHADOWED_BOOTS.get()));
    }
    private static void equipment(GameTestHelper h) {
        var p=player(h);
        h.assertTrue(!UnshadowedArmor.fullSet(p) && !InevitabilityArmor.wearing(p,EquipmentSlot.CHEST),"Chest alone is not the full set");
        equipSet(p);h.assertTrue(UnshadowedArmor.fullSet(p) && !InevitabilityArmor.fullSet(p),"Four pieces activate only this set");
        for(var slot:InevitabilityArmor.SLOTS) {
            ItemStack stack=p.getItemBySlot(slot);
            h.assertTrue(stack.get(DataComponents.EQUIPPABLE).slot()==slot,"Correct equipment slot: "+slot);
            h.assertTrue(stack.has(DataComponents.ENCHANTABLE) && stack.has(DataComponents.REPAIRABLE),"Vanilla enchanting and repair components");
        }
        var bases=List.of(Items.NETHERITE_HELMET,Items.NETHERITE_CHESTPLATE,Items.NETHERITE_LEGGINGS,Items.NETHERITE_BOOTS);
        var results=List.of(ModItems.UNSHADOWED_HELMET.get(),ModItems.UNSHADOWED_CHESTPLATE.get(),
                ModItems.UNSHADOWED_LEGGINGS.get(),ModItems.UNSHADOWED_BOOTS.get());
        for(int i=0;i<4;i++) {
            ItemStack base=new ItemStack(bases.get(i));base.setDamageValue(9);
            var input=new net.minecraft.world.item.crafting.SmithingRecipeInput(
                    new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE),base,new ItemStack(ModItems.UNSHADOWED_CORE.get()));
            var recipe=h.getLevel().getServer().getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.SMITHING,input,h.getLevel()).orElseThrow();
            ItemStack result=recipe.value().assemble(input);
            h.assertTrue(result.is(results.get(i)) && result.getDamageValue()==9,"Smithing creates Unshadowed armor and keeps damage");
        }
        h.succeed();
    }
    private static void purify(GameTestHelper h) {
        var p=player(h);var husk=target(h,p,EntityTypes.HUSK,2);
        h.runAfterDelay(4,() -> h.assertTrue(husk.getHealth()==100,"Windup has not dealt damage"));
        h.runAfterDelay(18,() -> {
            h.assertTrue(husk.getHealth()<92 && husk.getHealth()>85,"Undead take double spear damage; health="+husk.getHealth());
            h.assertTrue(husk.isOnFire(),"Purified undead burn");
            h.assertTrue(p.getItemBySlot(EquipmentSlot.CHEST).getDamageValue()==1,"A landed strike costs one durability");
        });
        h.runAfterDelay(28,() -> {
            h.assertTrue(p.getItemBySlot(EquipmentSlot.CHEST).getDamageValue()==1,"Cooldown prevents repeated strikes");h.succeed();
        });
    }
    private static void reach(GameTestHelper h) {
        var p=player(h);var spider=target(h,p,EntityTypes.SPIDER,4.5);
        h.assertTrue(TentacleCombat.canHit(p,spider,UnshadowedArmor.CHEST,TentacleCombat::hostile)
                && !TentacleCombat.canHit(p,spider,InevitabilityArmor.FULL_SET,TentacleCombat::hostile),"Spears out-reach the tentacles");
        h.runAfterDelay(18,() -> {
            h.assertTrue(spider.getHealth()<=95.01f && spider.getHealth()>94,"Living hostiles take the base 5; health="+spider.getHealth());
            h.assertTrue(!spider.isOnFire(),"Only undead are set alight");h.succeed();
        });
    }
    private static void fullSet(GameTestHelper h) {
        var p=player(h);var spider=target(h,p,EntityTypes.SPIDER,5.5);
        h.runAfterDelay(18,() -> {h.assertTrue(spider.getHealth()==100,"Chest alone cannot reach 5.5 blocks");equipSet(p);});
        h.runAfterDelay(40,() -> {h.assertTrue(spider.getHealth()<=93.01f,"Full set reaches further and hits for 7; health="+spider.getHealth());h.succeed();});
    }
    private static void ward(GameTestHelper h) {
        var p=player(h);
        p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,200));
        h.runAfterDelay(3,() -> {
            h.assertTrue(p.hasEffect(MobEffects.BLINDNESS),"Chest alone does not ward off blindness");
            equipSet(p);p.addEffect(new MobEffectInstance(MobEffects.DARKNESS,200));
        });
        h.runAfterDelay(6,() -> {
            h.assertTrue(!p.hasEffect(MobEffects.BLINDNESS) && !p.hasEffect(MobEffects.DARKNESS),"The Unshadowed cannot be blinded");h.succeed();
        });
    }
}
