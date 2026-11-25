package net.lukario.circleofinevitability.item.armor;

import com.mojang.authlib.GameProfile;
import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.combat.Friends;
import net.lukario.circleofinevitability.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.DeferredRegister;
import java.util.UUID;
import java.util.function.Consumer;

/** Runs against actual equipment events, server damage and vanilla tracking stack data. */
public final class InevitabilityArmorGameTests {
    public static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, CircleOfInevitability.MOD_ID);
    static {
        FUNCTIONS.register("inevitability_armor_equipment", () -> InevitabilityArmorGameTests::equipment);
        FUNCTIONS.register("inevitability_armor_targets", () -> InevitabilityArmorGameTests::targets);
        FUNCTIONS.register("inevitability_armor_strike", () -> InevitabilityArmorGameTests::strike);
        FUNCTIONS.register("inevitability_armor_full_set", () -> InevitabilityArmorGameTests::fullSet);
        FUNCTIONS.register("inevitability_armor_remove", () -> h -> interrupt(h, false));
        FUNCTIONS.register("inevitability_armor_wall", () -> h -> interrupt(h, true));
        FUNCTIONS.register("inevitability_armor_friend_windup", () -> InevitabilityArmorGameTests::friendWindup);
    }
    private static ServerPlayer player(GameTestHelper h) {
        var cookie=CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "Armor"+UUID.randomUUID().toString().substring(0,6)),false);
        ServerPlayer p=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),cookie.gameProfile(),cookie.clientInformation());
        Connection connection=new Connection(PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        h.getLevel().getServer().getPlayerList().placeNewPlayer(connection,p,cookie);
        p.setGameMode(GameType.SURVIVAL);
        p.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
        Vec3 at=h.absoluteVec(new Vec3(4.5,2,4.5));p.setPos(at.x,at.y,at.z);p.setNoGravity(true);
        p.setItemSlot(EquipmentSlot.CHEST,new ItemStack(ModItems.INEVITABILITY_CHESTPLATE.get()));
        // Embedded test connections do not drive the player's network-owned simulation tick.
        // doTick calls the real Player/LivingEntity tick, including Forge's armor event hook.
        h.onEachTick(p::doTick);
        h.addCleanup(ok -> {TentacleCombat.cancel(p);h.getLevel().getServer().getPlayerList().remove(p);});
        return p;
    }
    private static Mob target(GameTestHelper h,ServerPlayer p,EntityType<? extends Mob> type,double distance) {
        Mob m=h.spawnWithNoFreeWill(type,1,2,1);m.setNoGravity(true);
        // First-tick random Beyonder assignment would change health/defense and may cast unrelated powers.
        m.getPersistentData().putBoolean("circle_of_inevitability_beyonder_rolled",true);
        net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.BeyonderMobs.clear(m);
        m.setPos(p.getX()+distance,p.getY(),p.getZ());
        m.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);m.setHealth(100);
        h.addCleanup(ok -> m.discard());return m;
    }
    private static void equipSet(ServerPlayer p) {
        p.setItemSlot(EquipmentSlot.HEAD,new ItemStack(ModItems.INEVITABILITY_HELMET.get()));
        p.setItemSlot(EquipmentSlot.LEGS,new ItemStack(ModItems.INEVITABILITY_LEGGINGS.get()));
        p.setItemSlot(EquipmentSlot.FEET,new ItemStack(ModItems.INEVITABILITY_BOOTS.get()));
    }
    private static void equipment(GameTestHelper h) {
        var p=player(h);
        h.assertTrue(!InevitabilityArmor.fullSet(p),"Chest alone is not the full set");
        equipSet(p);h.assertTrue(InevitabilityArmor.fullSet(p),"Four pieces activate set bonus");
        for(var slot:InevitabilityArmor.SLOTS) {
            ItemStack stack=p.getItemBySlot(slot);
            h.assertTrue(stack.get(DataComponents.EQUIPPABLE).slot()==slot,"Correct equipment slot: "+slot);
            h.assertTrue(stack.isDamageableItem() && stack.getMaxStackSize()==1,"Durable single armor item");
            h.assertTrue(stack.has(DataComponents.ENCHANTABLE) && stack.has(DataComponents.REPAIRABLE),"Vanilla enchanting and repair components");
        }
        p.setItemSlot(EquipmentSlot.HEAD,ItemStack.EMPTY);
        h.assertTrue(!InevitabilityArmor.fullSet(p),"Missing helmet removes bonus");
        var bases=java.util.List.of(net.minecraft.world.item.Items.NETHERITE_HELMET,net.minecraft.world.item.Items.NETHERITE_CHESTPLATE,
                net.minecraft.world.item.Items.NETHERITE_LEGGINGS,net.minecraft.world.item.Items.NETHERITE_BOOTS);
        var results=java.util.List.of(ModItems.INEVITABILITY_HELMET.get(),ModItems.INEVITABILITY_CHESTPLATE.get(),
                ModItems.INEVITABILITY_LEGGINGS.get(),ModItems.INEVITABILITY_BOOTS.get());
        for(int i=0;i<4;i++) {
            ItemStack base=new ItemStack(bases.get(i));
            base.set(DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Preserved name"));
            base.setDamageValue(17);
            var input=new net.minecraft.world.item.crafting.SmithingRecipeInput(
                    new ItemStack(net.minecraft.world.item.Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE),base,
                    new ItemStack(ModItems.INEVITABILITY_CORE.get()));
            var recipe=h.getLevel().getServer().getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.SMITHING,input,h.getLevel()).orElseThrow();
            ItemStack result=recipe.value().assemble(input);
            h.assertTrue(result.is(results.get(i)) && result.getDamageValue()==17
                    && result.get(DataComponents.CUSTOM_NAME).equals(base.get(DataComponents.CUSTOM_NAME)),"Smithing creates armor and preserves components");
        }
        h.succeed();
    }
    private static void targets(GameTestHelper h) {
        var p=player(h);var enemy=target(h,p,EntityTypes.HUSK,2);var pig=target(h,p,EntityTypes.PIG,1);
        var wolf=target(h,p,EntityTypes.WOLF,1.5);var enderman=target(h,p,EntityTypes.ENDERMAN,2.5);
        h.assertTrue(TentacleCombat.canHit(p,enemy,InevitabilityArmor.CHEST,TentacleCombat::hostile),"Visible nearby hostile allowed");
        h.assertTrue(!TentacleCombat.hostile(p,pig) && !TentacleCombat.hostile(p,wolf) && !TentacleCombat.hostile(p,enderman),"Passive and neutral mobs spared");
        h.assertTrue(!TentacleCombat.hostile(p,p),"Players never selected");
        Friends.add(p,enemy.getUUID(),"Friendly husk",false);
        h.assertTrue(!TentacleCombat.hostile(p,enemy),"Explicit hostile mob friends spared");
        Friends.remove(p,enemy.getUUID());
        enemy.setPos(p.getX()+3.5,p.getY(),p.getZ());
        h.assertTrue(!TentacleCombat.canHit(p,enemy,InevitabilityArmor.CHEST,TentacleCombat::hostile)
                && TentacleCombat.canHit(p,enemy,InevitabilityArmor.FULL_SET,TentacleCombat::hostile),"Full set extends range");
        h.succeed();
    }
    private static void strike(GameTestHelper h) {
        var p=player(h);var enemy=target(h,p,EntityTypes.HUSK,2);
        h.runAfterDelay(4,() -> {
            h.assertTrue(enemy.getHealth()==100,"Windup has not dealt damage; health="+enemy.getHealth());
            h.assertTrue(p.getItemBySlot(EquipmentSlot.CHEST).getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY)
                    .copyTag().contains(TentacleCombat.VISUAL_KEY),"Strike snapshot is synchronized in equipment");
        });
        h.runAfterDelay(15,() -> {
            h.assertTrue(enemy.getHealth()<100 && enemy.getHealth()>92,"One six-damage strike after armor reduction");
            h.assertTrue(p.getItemBySlot(EquipmentSlot.CHEST).getDamageValue()==1,"Successful lash costs one durability");
        });
        h.runAfterDelay(25,() -> {
            h.assertTrue(p.getItemBySlot(EquipmentSlot.CHEST).getDamageValue()==1,"Cooldown prevents repeated strikes");h.succeed();
        });
    }
    private static void fullSet(GameTestHelper h) {
        var p=player(h);var enemy=target(h,p,EntityTypes.HUSK,3.5);
        h.runAfterDelay(15,() -> {h.assertTrue(enemy.getHealth()==100,"Chest alone cannot reach 3.5 blocks");equipSet(p);});
        h.runAfterDelay(30,() -> {h.assertTrue(enemy.getHealth()<94,"Full set reaches target with stronger strike; health="+enemy.getHealth());h.succeed();});
    }
    private static void interrupt(GameTestHelper h,boolean wall) {
        var p=player(h);var enemy=target(h,p,EntityTypes.HUSK,2.5);
        ItemStack chest=p.getItemBySlot(EquipmentSlot.CHEST);
        h.runAfterDelay(4,() -> {
            if(wall) for(int y=1;y<=5;y++) for(int z=3;z<=5;z++) h.setBlock(new BlockPos(5,y,z),Blocks.STONE);
            else p.setItemSlot(EquipmentSlot.CHEST,ItemStack.EMPTY);
        });
        h.runAfterDelay(15,() -> {
            h.assertTrue(enemy.getHealth()==100,"Interrupted strike never damages target; health="+enemy.getHealth());
            h.assertTrue(chest.getDamageValue()==0,"Missed strike does not consume durability");
            if(!wall) {
                h.assertTrue(!chest.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().contains(TentacleCombat.VISUAL_KEY),"Unequipped armor clears animation");
                p.setItemSlot(EquipmentSlot.CHEST,chest);
            }
        });
        h.runAfterDelay(25,() -> {h.assertTrue(enemy.getHealth()==100,"Wall/removal and re-equip cannot bypass recovery");h.succeed();});
    }
    private static void friendWindup(GameTestHelper h) {
        var p=player(h);var enemy=target(h,p,EntityTypes.HUSK,2);
        h.runAfterDelay(4,() -> Friends.add(p,enemy.getUUID(),"New ally",false));
        h.runAfterDelay(15,() -> {h.assertTrue(enemy.getHealth()==100,"Ally protection is rechecked at impact");h.succeed();});
    }
}
