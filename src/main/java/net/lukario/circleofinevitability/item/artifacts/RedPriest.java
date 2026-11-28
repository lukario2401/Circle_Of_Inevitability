package net.lukario.circleofinevitability.item.artifacts;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.conqueror.ConquerorHigher;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.item.artifacts.uniqueness.UniquePower;
import net.lukario.circleofinevitability.item.artifacts.uniqueness.Uniqueness;
import net.lukario.circleofinevitability.item.artifacts.uniqueness.drawbacks.BloodPriceDrawback;
import net.lukario.circleofinevitability.item.artifacts.uniqueness.drawbacks.BurningBloodDrawback;
import net.lukario.circleofinevitability.item.artifacts.uniqueness.drawbacks.FinalConquestDrawback;
import net.lukario.circleofinevitability.item.artifacts.uniqueness.drawbacks.HuntedDrawback;
import net.lukario.circleofinevitability.item.artifacts.uniqueness.drawbacks.LoseYourselfDrawback;
import net.lukario.circleofinevitability.item.artifacts.uniqueness.drawbacks.ProvokerAuraDrawback;
import net.lukario.circleofinevitability.item.artifacts.uniqueness.drawbacks.ShadowSoldiersDrawback;
import net.lukario.circleofinevitability.item.artifacts.uniqueness.drawbacks.StormDrawback;
import net.lukario.circleofinevitability.item.artifacts.uniqueness.drawbacks.WarNeverRestsDrawback;
import net.lukario.circleofinevitability.item.artifacts.uniqueness.drawbacks.WhispersOfWarDrawback;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;

/**
 * The Red Priest: the Uniqueness of the Conqueror Pathway, a war spear. Every number lives here; the mechanics are the
 * reusable Uniqueness framework (item/artifacts/uniqueness). docs/SEALED_ARTIFACTS.md describes it for players.
 *
 * Carried, it lends every Conqueror ability plus Iron Blood (4); Storm of War, Hailstorm, War Cyclone (2); Conquest,
 * Banner of Conquest, Subjugation (1); Crimson Dominion, Spear of Judgement and Endless War (0),
 * cast every 0.2 s for no spirituality; the wielder takes 90% less damage, deals double, and each kill heals them by the
 * victim's maximum health. Its corruption grows 1 point every 8 s carried (and a little per cast):
 *
 *  Low      0+   Sequence 3-9 abilities. Shadow soldiers (Levies), provoker's aura, no sleep, blood price, burning blood.
 *  Medium  25+   + hunted (you glow), whispers of war; Legionaries join the soldiers.
 *  High    50+   Sequence 1-2. + the storm follows; healing halved; villagers will not trade; Centurions.
 *  Extreme 75+   Sequence 0. Bound for good. + lightning finds you, the ground ignites, pets turn, healing stops,
 *                you lose yourself; Warlords. At 100: final conquest in 3 minutes (each kill buys 10 s).
 * Put away before Extreme, the corruption fades 0.5 a second.
 */
public final class RedPriest {
    public static final String ID = "red_priest_uniqueness";
    public static final int LOW = 0, MEDIUM = 1, HIGH = 2, EXTREME = 3;

    public static final ShadowSoldiersDrawback SOLDIERS = new ShadowSoldiersDrawback(
            new int[]{60, 50, 40, 30},                       // seconds between waves, by level
            new int[]{1, 2, 3, 4},                           // soldiers per wave
            new int[]{2, 4, 6, 8},                           // never more alive at once
            new int[][]{{0}, {0, 1}, {1, 2}, {2, 3}},        // ranks: Levy, Legionary, Centurion, Warlord
            20 * 90);                                        // each fades after 90 s
    public static final FinalConquestDrawback FINAL_CONQUEST = new FinalConquestDrawback(180, 10, 20 * 60 * 10);

    public static final Uniqueness UNIQUENESS = Uniqueness.create(ID, "The Red Priest")
            .pathway("Conqueror")
            .power(UniquePower.of("red_priest/iron_blood", "Iron Blood", "red_priest/iron_blood", 4, 20 * 8,
                    "Heal a third of your health and put out your fire. Works even when nothing else heals you.",
                    ConquerorHigher::ironBlood))
            .power(UniquePower.of("red_priest/storm_of_war", "Storm of War", "red_priest/storm_of_war", 2, 20 * 3,
                    "Weather Warlock: a storm over where you look; 8 lightning strikes on everyone in it, then a gale.",
                    ConquerorHigher::stormOfWar))
            .power(UniquePower.of("red_priest/conquest", "Conquest", "red_priest/conquest", 1, 20 * 5,
                    "Conqueror: everyone hostile in sight kneels and loses a quarter of their health; the broken ones serve you for 30 s.",
                    ConquerorHigher::conquest))
            .power(UniquePower.of("red_priest/crimson_dominion", "Crimson Dominion", "red_priest/crimson_dominion", 0, 20 * 20,
                    "Red Priest: the sky bleeds, burning spears rain on your enemies, and a legion of shadow soldiers rises for you.",
                    ConquerorHigher::crimsonDominion))
            .power(UniquePower.of("red_priest/hailstorm", "Hailstorm", "red_priest/hailstorm", 2, 20 * 4,
                    "Weather Warlock: ice falls over where you look for 4 s, battering, slowing and freezing everyone in it.",
                    ConquerorHigher::hailstorm))
            .power(UniquePower.of("red_priest/war_cyclone", "War Cyclone", "red_priest/war_cyclone", 2, 20 * 6,
                    "Weather Warlock: a whirlwind drags enemies in and grinds them for 5 s, then flings them away.",
                    ConquerorHigher::warCyclone))
            .power(UniquePower.of("red_priest/banner_of_conquest", "Banner of Conquest", "red_priest/banner_of_conquest", 1, 20 * 25,
                    "Conqueror: raise a war banner for 20 s. Within 12 blocks you and your allies gain Strength II, Speed and Resistance; enemies are weakened, slowed and revealed.",
                    ConquerorHigher::bannerOfConquest))
            .power(UniquePower.of("red_priest/subjugation", "Subjugation", "red_priest/subjugation", 1, 20 * 8,
                    "Conqueror: break the one you look at for 40% of its health (12% for bosses). A mob left under half serves you for 60 s.",
                    ConquerorHigher::subjugation))
            .power(UniquePower.of("red_priest/spear_of_judgement", "Spear of Judgement", "red_priest/spear_of_judgement", 0, 20 * 12,
                    "Red Priest: hurl a giant burning spear through everything in a line (60 damage), bursting in fire where it lands.",
                    ConquerorHigher::spearOfJudgement))
            .power(UniquePower.of("red_priest/endless_war", "Endless War", "red_priest/endless_war", 0, 20 * 60,
                    "Red Priest: for 30 s you gain Strength II and Speed II, and everyone you kill rises again as your shadow soldier (up to 8).",
                    ConquerorHigher::endlessWar))
            .level("Low", 0, 0xD8B060)
            .level("Medium", 25, 0xE8902E)
            .level("High", 50, 0xE8452A)
            .level("Extreme", 75, 0xC0102E)
            .gate(sequence -> sequence == 0 ? EXTREME : sequence <= 2 ? HIGH : LOW)
            .noReturnFrom(EXTREME)
            .corruption(1 / 8.0,                                                              // per second carried
                    sequence -> sequence == 0 ? .5 : sequence <= 2 ? .25 : sequence <= 4 ? .1 : .05,  // per cast
                    .5)                                                                       // faded per second put away
            .cooldown(4)                                                                      // 0.2 s
            .damageTaken(.1f)                                                                 // 90% less
            .damageDealt(2f)
            .killHeal(1f)                                                                     // the victim's max health
            .weapon(12, -2.6f, 1.5f)                                                          // a spear: reach +1.5
            .spear()                                                                          // held and stabbed like one
            .hudIcon("red_priest/corruption")
            .drawback(SOLDIERS)
            .drawback(new ProvokerAuraDrawback(new int[]{12, 16, 24, 32}, HIGH, EXTREME))
            .drawback(new WarNeverRestsDrawback(HIGH, .5f, EXTREME))
            .drawback(new BloodPriceDrawback(sequence -> sequence == 0 ? 3 : sequence <= 2 ? 1.5 : sequence <= 4 ? .75 : .25))
            .drawback(new BurningBloodDrawback(new int[]{150, 110, 75, 40}, 1f, new float[]{.5f, .5f, 1f, 1.5f}, EXTREME, 25))
            .drawback(new HuntedDrawback(MEDIUM, new int[]{0, 24, 32, 48}))
            .drawback(new WhispersOfWarDrawback(MEDIUM, new int[]{0, 45, 30, 15}, "red_priest/war_flash"))
            .drawback(new StormDrawback(HIGH, EXTREME, 25, .25f, 6))
            .drawback(new LoseYourselfDrawback(EXTREME, 20, 6, .1f, .3f))
            .drawback(FINAL_CONQUEST)
            .effects(new Uniqueness.Effects() {
                @Override
                public void cast(ServerLevel level, ServerPlayer player, UniquePower power) {
                    Vec3 tip = player.getEyePosition().add(player.getLookAngle().scale(1.6)).add(0, .2, 0);
                    level.sendParticles(new DustParticleOptions(0xD0202E, 1f), tip.x, tip.y, tip.z, 6, .12, .12, .12, 0);
                    level.sendParticles(ParticleTypes.SMALL_FLAME, tip.x, tip.y, tip.z, 3, .08, .08, .08, .01);
                }

                @Override
                public void levelReached(ServerLevel level, ServerPlayer player, Uniqueness.Level reached) {
                    Vec3 at = player.position();
                    level.sendParticles(new DustParticleOptions(reached.rgb(), 1.6f), at.x, at.y + 1, at.z, 40, .6, .9, .6, 0);
                    Spells.sound(level, at, SoundEvents.RAID_HORN.value(), 1f, .6f);
                }
            });

    private RedPriest() {}
}
