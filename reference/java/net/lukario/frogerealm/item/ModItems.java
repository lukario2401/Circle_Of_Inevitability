package net.lukario.frogerealm.item;

import net.lukario.frogerealm.ForgeRealm;
import net.lukario.frogerealm.item.custom.ModFoodProperties;
import net.lukario.frogerealm.item.custom.ModFuelItem;
import net.lukario.frogerealm.item.custom.ranged.LaserStaff;
import net.lukario.frogerealm.item.custom.ranged.Terminator;
import net.lukario.frogerealm.item.custom.ranged.TarotDeck;
import net.lukario.frogerealm.item.custom.swords.ShadowSword;
import net.lukario.frogerealm.item.custom.StrengthElixir;
import net.lukario.frogerealm.item.custom.swords.ShepardsBlade;
import net.lukario.frogerealm.item.seald_artifacts.*;
import net.lukario.frogerealm.item.seald_artifacts.IronCoif;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import static net.lukario.frogerealm.item.tiers.ModTiers.SHEPARDS_TIER;


public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, ForgeRealm.MOD_ID);

    public static final RegistryObject<Item> SOULESSENCE = ITEMS.register("soulessence", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> COIN = ITEMS.register("coin", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> SOULCOIN = ITEMS.register("soulcoin", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> DEPLETED_ENERGY_SHARD = ITEMS.register("depleted_energy_shard", () -> new ModFuelItem(new Item.Properties(), 48000));

    public static final RegistryObject<Item> ENERGY_SHARD = ITEMS.register("energy_shard",
            () -> new Item(new Item.Properties()
                    .food(new FoodProperties.Builder()
                            .alwaysEdible()
                            .nutrition(4)
                            .saturationModifier(2.0f)
                            .fast()
                            // This is called inside the registry lambda, which is safer
                            .usingConvertsTo(DEPLETED_ENERGY_SHARD.get())
                            .build())
            )
    );

    public static final RegistryObject<Item> DIAMOND_CARROT = ITEMS.register("diamond_carrot", () -> new Item(new Item.Properties().food(ModFoodProperties.DIAMOND_CARROT)));

    public static final RegistryObject<Item> STRENGTH_ELIXIR =
            ITEMS.register("strength_elixir",
                    () -> new StrengthElixir(
                            new Item.Properties().food(StrengthElixir.STRENGTH_ELIXIR)
                    ));


    public static final RegistryObject<Item> SHADOW_SWORD = ITEMS.register(
            "shadow_sword",
            () -> new ShadowSword(
                    Tiers.IRON,
                    new Item.Properties().attributes(
                            SwordItem.createAttributes(
                                    Tiers.IRON,
                                    12,      // extra damage
                                    -1.2F   // attack speed
                            )
                    )
            )
    );

    public static final RegistryObject<Item> SHEPARDS_BLADE = ITEMS.register(
            "shepards_blade",
            () -> new ShepardsBlade(
                    SHEPARDS_TIER, // <-- Put your custom tier here
                    new Item.Properties()
                            // You no longer need .durability(5) here, the Tier handles it!
                            .component(
                                    DataComponents.ATTRIBUTE_MODIFIERS,
                                    SwordItem.createAttributes(SHEPARDS_TIER, 14, -0.8F)
                            )
            )
    );

    public static final RegistryObject<Item> LASER_STAFF = ITEMS.register("laser_staff",
            () -> new LaserStaff(new Item.Properties()));

    public static final RegistryObject<Item> TERMINATOR = ITEMS.register("terminator", ()-> new Terminator(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> TAROTDECK = ITEMS.register("tarot_deck", ()-> new TarotDeck(new Item.Properties()));

    public static final RegistryObject<Item> BONE_DEFINERS_LENS =
            ITEMS.register(
                    "bone_definers_lens",
                    () -> new BoneDefinersLens(
                            new Item.Properties()
                                    .stacksTo(1)
                                    .rarity(Rarity.EPIC)
                    )
            );

    public static final RegistryObject<Item> GRAVEWARDEN_SHROUD =
            ITEMS.register(
                    "gravewarden_shroud",
                    () -> new GraveWardenShroud(
                            new Item.Properties()
                                    .stacksTo(1)
                                    .rarity(Rarity.EPIC)
                    )
            );

    public static final RegistryObject<Item> HOLLOW_NAME =
            ITEMS.register(
                    "hollow_name",
                    () -> new HollowName(
                            new Item.Properties()
                                    .stacksTo(8)
                                    .rarity(Rarity.EPIC)
                    )
            );

    public static final RegistryObject<Item> FRACTURED_CHORUS =
            ITEMS.register(
                    "fractured_chorus",
                    () -> new FracturedChorus(
                            ArmorMaterials.NETHERITE,
                            new Item.Properties()
                                    .stacksTo(1)
                                    .rarity(Rarity.EPIC)
                    )
            );

    public static final RegistryObject<Item> UNFINISHED_HOUR_GLASS =
            ITEMS.register(
                    "unfinished_hour_glass",
                    () -> new UnfinishedHourglass(new Item.Properties()
                            .stacksTo(1)
                            .rarity(Rarity.EPIC)
                    )
            );

    public static final RegistryObject<Item> UNDYING_CANKER =
            ITEMS.register(
                    "undying_canker",
                    () -> new UndyingCanker(
                            new Item.Properties()
                                    .stacksTo(1)
                                    .rarity(Rarity.EPIC)
                    )
            );

    public static final RegistryObject<Item> MIRRORLESS_EYE =
            ITEMS.register(
                    "mirrorless_eye",
                    () -> new MirrorlessEye(
                            new Item.Properties()
                                    .stacksTo(1)
                                    .rarity(Rarity.EPIC)
                    )
            );

    public static final RegistryObject<Item> SUNKEN_CODEX =
            ITEMS.register(
                    "sunken_codex",
                    () -> new SunkenCodex(
                            new Item.Properties()
                                    .stacksTo(1)
                                    .rarity(Rarity.EPIC)
                    )
            );

    public static final RegistryObject<Item> CRYSTALLIZED_PROPHECY_MONOCLE =
            ITEMS.register(
                    "crystallized_prophecy_monocle",
                    () -> new CrystallizedProphecyMonocle(
                            new Item.Properties()
                                    .stacksTo(1)
                                    .rarity(Rarity.EPIC)
                    )
            );

    public static final RegistryObject<Item> SCARLET_SUTURE =
            ITEMS.register(
                    "scarlet_suture",
                    () -> new ScarletSuture(
                            new Item.Properties()
                                    .stacksTo(1)
                                    .rarity(Rarity.EPIC)
                    )
            );

    public static final RegistryObject<Item> HANGED_MAN_CHORD =
            ITEMS.register(
                    "hanged_man_chord",
                    () -> new HangedManChord(
                            new Item.Properties()
                                    .stacksTo(1)
                                    .rarity(Rarity.EPIC)
                    )
            );

    public static final RegistryObject<Item> BONE_FETISH =
            ITEMS.register(
                    "bone_fetish",
                    () -> new BoneFetish(
                            new Item.Properties()
                                    .stacksTo(1)
                                    .rarity(Rarity.EPIC)
                    )
            );

    public static final RegistryObject<Item> CRACKED_BAROMETER =
            ITEMS.register(
                    "cracked_barometer",
                    () -> new CrackedBarometer(
                            new Item.Properties()
                                    .stacksTo(1)
                                    .rarity(Rarity.EPIC)
                    )
            );

    public static final RegistryObject<Item> SPLICED_COMPASS =
            ITEMS.register(
                    "spliced_compass",
                    () -> new SplicedCompass(
                            new Item.Properties()
                                    .stacksTo(1)
                                    .rarity(Rarity.EPIC)
                    )
            );

    public static final RegistryObject<Item> LAST_BREATH =
            ITEMS.register(
                    "last_breath",
                    () -> new LastBreath(
                            new Item.Properties()
                                    .stacksTo(8)
                                    .rarity(Rarity.EPIC)
                    )
            );

    public static final RegistryObject<Item> COLLAPSAR_BEAD =
            ITEMS.register(
                    "collapsar_bead",
                    () -> new CollapsarBead(
                            new Item.Properties()
                                    .stacksTo(8)
                                    .rarity(Rarity.EPIC)
                    )
            );

    public static final RegistryObject<Item> IRON_COIF =
            ITEMS.register("iron_coif",
                    () -> new IronCoif(
                            ArmorMaterials.GOLD,
                            new Item.Properties()
                                    .stacksTo(1)
                                    .rarity(Rarity.EPIC)
//                                    .durability(480) // Durability is now set here!
                    ));

    public static void register(IEventBus eventBus){
        ITEMS.register(eventBus);
    }
}
