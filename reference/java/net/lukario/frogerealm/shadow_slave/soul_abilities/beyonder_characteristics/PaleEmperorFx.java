package net.lukario.frogerealm.shadow_slave.soul_abilities.beyonder_characteristics;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.lukario.frogerealm.combat.Later;
import net.lukario.frogerealm.particles.fx.ModelFx;
import net.lukario.frogerealm.particles.fx.ModelFx.Ease;
import net.lukario.frogerealm.particles.fx.ParticleShapes;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * The Death-pathway models, each shown the way it looks best: size, pivot, timing and animation already chosen.
 *
 * ---------- See them in game ----------
 *   /palefx all            every one of them in a ring around you, three times over (/palefx all 5 = five times)
 *   /palefx serpent        just that one, 7 blocks in front of you (press TAB after /palefx for the names)
 * Stand on flat ground: they are placed at the height of your feet.
 *
 * ---------- Use them in an ability ----------
 * Every display is one call. 'ground' is a spot on the floor, 'yaw' the way its front looks (player.getYRot()
 * = away from the player, toward what they look at):
 *   PaleEmperorFx.ribCage(sl, enemy.position(), player.getYRot());
 *   Later.run(sl, PaleEmperorFx.RIB_CAGE_SHUT, () -> ...root and hurt the enemy...);
 * The numbers named ..._TICKS say how long a display lasts; the other numbers are the tick something happens
 * (the cage is shut, the serpent bites), for Later.run.
 *
 * To change one, copy its constant into your class and edit it there; every method on a ModelFx returns a new
 * copy, so PaleEmperorFx.SKULL.scale(4f).lifetime(200) is also fine.
 *
 * The wings and the crown are worn: they are stuck to an entity and move with it. The wings turn when its body
 * turns (ParticleShapes.modelOnTurning), the crown follows its head (ParticleShapes.modelOnHead).
 */
@Mod.EventBusSubscriber(modid = "forgerealmmod")
public final class PaleEmperorFx {

    private PaleEmperorFx() {}

    private static ModelFx.Pose pose() {
        return ModelFx.pose();
    }

    // =====================================================================================
    // The seal on the ground
    // =====================================================================================

    private static final ModelFx SEAL_APPEARS = ModelFx.of("pale_emperor/death_sigil_ring")
            .glow().unshaded().seeThrough().fade(0, 14)
            .key(0, pose().scale(0.2f).alpha(0f))
            .key(10, pose().scale(1f).alpha(1f), Ease.OUT);          // opens out from its middle
    /** The outer half of the seal: runes and the serpent. Turns slowly to the right. */
    public static final ModelFx SEAL_RING = SEAL_APPEARS.spin(1.2f);
    /** The inner half: feathers and the hourglass. Turns the other way, twice as fast. */
    public static final ModelFx SEAL_CORE = ModelFx.of("pale_emperor/death_sigil_core")
            .glow().unshaded().seeThrough().fade(0, 14)
            .key(0, pose().scale(0.2f).alpha(0f))
            .key(10, pose().scale(1f).alpha(1f), Ease.OUT)
            .spin(-2.4f);

    /** The seal, 'blocksWide' across, for 'ticks' ticks. Its two halves turn against each other. */
    public static void seal(ServerLevel sl, Vec3 ground, float yaw, float blocksWide, int ticks) {
        float scale = blocksWide / 3f;                               // the model is 3 blocks wide
        Vec3 floor = ground.add(0, 0.06, 0);                         // a little above the blocks, or it flickers with them
        ParticleShapes.model(sl, SEAL_RING.scale(scale).lifetime(ticks), floor, yaw, 0f, 0f);
        ParticleShapes.model(sl, SEAL_CORE.scale(scale).lifetime(ticks), floor.add(0, 0.02, 0), yaw, 0f, 0f);
    }

    // =====================================================================================
    // The Feathered Serpent
    // =====================================================================================

    public static final int SERPENT_TICKS = 150;
    /** The tick its roar begins (jaws wide, collar flared). */
    public static final int SERPENT_ROARS = 40;
    /** The tick its bite lands: 4 blocks in front of where it rose, at head height. */
    public static final int SERPENT_BITES = 87;

    /** The head and neck, 6 blocks tall. */
    public static final ModelFx SERPENT = ModelFx.of("pale_emperor/feathered_serpent").frames(3).smooth()
            .scale(2f).pivot(8, -16, 12).glow().lifetime(SERPENT_TICKS).fade(0, 4)
            .key(0, pose().up(-6.2f))                                    // under the ground
            .key(12, pose().up(-6.2f))                                   // waits for the coils and the seal
            .key(30, pose().up(0), Ease.OUT)                             // rears up
            .during(34, 40, pose().frame(2), Ease.OUT)                   // roars: jaws wide, collar flared
            .during(60, 68, pose().frame(0), Ease.IN_OUT)                // shuts its mouth
            .during(78, 84, pose().frame(2), Ease.OUT)                   // opens it again...
            .during(80, 86, pose().pitch(28), Ease.IN)                   // ...and strikes forward and down
            .during(86, 89, pose().frame(0), Ease.IN)                    // the bite
            .during(94, 108, pose().pitch(0), Ease.IN_OUT)               // draws back
            .during(128, 148, pose().up(-6.2f), Ease.IN);                // sinks away
    /** Its coiled body, 5 blocks across. */
    public static final ModelFx SERPENT_COIL = ModelFx.of("pale_emperor/feathered_serpent_coil")
            .scale(2f).pivot(8, -16, 8).glow().lifetime(SERPENT_TICKS).fade(0, 4)
            .key(0, pose().up(-2.3f))
            .key(12, pose().up(0), Ease.OUT)
            .during(132, 148, pose().up(-2.3f), Ease.IN);

    /** A seal opens, the coils rise out of it, the serpent rears up, roars, strikes once and sinks away. */
    public static void serpent(ServerLevel sl, Vec3 ground, float yaw) {
        seal(sl, ground, yaw, 7.5f, SERPENT_TICKS);
        ParticleShapes.model(sl, SERPENT_COIL, ground, yaw, 0f, 0f);
        ParticleShapes.model(sl, SERPENT, ground, yaw, 0f, 0f);
    }

    // =====================================================================================
    // Wings
    // =====================================================================================

    public static final int WINGS_TICKS = 132;
    /** The right wing; the left one is the same, mirrored. Each is 3.5 blocks from shoulder to tip. */
    public static final ModelFx WING_RIGHT = wing(1);
    public static final ModelFx WING_LEFT = wing(-1);

    private static ModelFx wing(int side) {                          // 1 = right, -1 = left
        ModelFx wing = ModelFx.of("pale_emperor/pale_wing").frames(5).smooth()
                .scale(1.4f).pivot(-10, 11.5f, 8).glow().lifetime(WINGS_TICKS).fade(2, 6)
                .key(0, pose().right(0.16f * side).forward(-0.28f).scale(0.5f))     // on the back, small and folded
                .during(0, 8, pose().scale(1f), Ease.OUT_BACK)
                .during(3, 15, pose().frame(4), Ease.OUT);                           // unfolds
        for (int t = 26; t <= 86; t += 20) {                                         // four slow beats
            wing = wing.during(t, t + 6, pose().roll(24f * side).frame(3), Ease.IN_OUT)        // down
                    .during(t + 6, t + 18, pose().roll(-10f * side).frame(4), Ease.IN_OUT);    // and up
        }
        wing = wing.during(108, 116, pose().roll(0), Ease.IN_OUT)
                .during(112, 126, pose().frame(0), Ease.IN);                         // folds away
        return side > 0 ? wing : wing.mirrored();
    }

    /** A pair of wings on the back of an entity: they unfold, beat four times and fold away. */
    public static void wings(ServerLevel sl, Entity wearer) {
        Vec3 shoulders = new Vec3(0, wearer.getBbHeight() * 0.75, 0);
        ParticleShapes.modelOnTurning(sl, WING_RIGHT, wearer, shoulders, 0f, 0f, 0f);
        ParticleShapes.modelOnTurning(sl, WING_LEFT, wearer, shoulders, 0f, 0f, 0f);
    }

    // ---------- wings for as long as you want (a flight, a transformation) ----------

    /** The first beat starts here, a beat takes this long, and folding away takes this long at the end. */
    private static final int WINGS_SPREAD = 20;
    private static final int WING_BEAT = 20;
    private static final int WINGS_FOLD = 24;
    /** Fewer ticks than this are too few to unfold wings for: wings(sl, wearer, ticks, tag) shows none then. */
    public static final int WINGS_SHORTEST = WINGS_SPREAD + WING_BEAT + WINGS_FOLD;

    private static ModelFx wingModel() {
        return ModelFx.of("pale_emperor/pale_wing").frames(5).smooth().scale(1.4f).pivot(-10, 11.5f, 8).glow();
    }

    /** One wing that unfolds, beats for as long as 'ticks' lasts and folds away in its last second. 1 = right, -1 = left. */
    private static ModelFx wingFor(int side, int ticks) {
        int foldAt = ticks - WINGS_FOLD;
        // a model holds 64 keys at most: a very long time gets slower beats instead of more of them
        int beat = Math.max(WING_BEAT, (foldAt - WINGS_SPREAD) / 28 + 1);

        ModelFx wing = wingModel().lifetime(ticks).fade(2, 6)
                .key(0, pose().right(0.16f * side).forward(-0.28f).scale(0.5f))     // on the back, small and folded
                .during(0, 8, pose().scale(1f), Ease.OUT_BACK)
                .during(3, 15, pose().frame(4), Ease.OUT);                           // unfolds
        for (int t = WINGS_SPREAD; t + beat <= foldAt; t += beat) {
            int down = t + Math.round(beat * 0.3f);
            int up = t + Math.round(beat * 0.9f);
            wing = wing.during(t, down, pose().roll(24f * side).frame(3), Ease.IN_OUT)        // down
                    .during(down, up, pose().roll(-10f * side).frame(4), Ease.IN_OUT);        // and up
        }
        wing = wing.during(foldAt, foldAt + 8, pose().roll(0), Ease.IN_OUT)
                .during(foldAt + 4, foldAt + 18, pose().frame(0), Ease.IN);          // folds away
        return side > 0 ? wing : wing.mirrored();
    }

    /** A spread wing that folds away at once. */
    private static ModelFx wingFolding(int side) {
        ModelFx wing = wingModel().lifetime(20).fade(0, 6)
                .key(0, pose().right(0.16f * side).forward(-0.28f).frame(4))
                .during(0, 14, pose().frame(0), Ease.IN);
        return side > 0 ? wing : wing.mirrored();
    }

    /**
     * Wings on the back of an entity for 'ticks' ticks: they unfold, keep beating and fold away in the last
     * second. They turn with the wearer, in the air too. 'tag' is their name: calling this again with the same
     * tag replaces them (use it to show them again after the wearer logged in), and foldWings(...) with it
     * takes them off early. Works with status/Flight: Flight.grant(player, 400); wings(sl, player, 400, "my_wings");
     */
    public static void wings(ServerLevel sl, Entity wearer, int ticks, String tag) {
        ParticleShapes.clearModels(sl, wearer, tag);
        if (ticks < WINGS_SHORTEST) return;
        Vec3 shoulders = new Vec3(0, wearer.getBbHeight() * 0.75, 0);
        ParticleShapes.modelOnTurning(sl, wingFor(1, ticks).tag(tag), wearer, shoulders, 0f, 0f, 0f);
        ParticleShapes.modelOnTurning(sl, wingFor(-1, ticks).tag(tag), wearer, shoulders, 0f, 0f, 0f);
    }

    /** Folds away, now, the wings that were put on with this tag. */
    public static void foldWings(ServerLevel sl, Entity wearer, String tag) {
        ParticleShapes.clearModels(sl, wearer, tag);
        Vec3 shoulders = new Vec3(0, wearer.getBbHeight() * 0.75, 0);
        ParticleShapes.modelOnTurning(sl, wingFolding(1).tag(tag), wearer, shoulders, 0f, 0f, 0f);
        ParticleShapes.modelOnTurning(sl, wingFolding(-1).tag(tag), wearer, shoulders, 0f, 0f, 0f);
    }

    // =====================================================================================
    // The crown
    // =====================================================================================

    public static final int CROWN_TICKS = 200;
    public static final ModelFx CROWN = ModelFx.of("pale_emperor/pale_crown")
            .glow().lifetime(CROWN_TICKS).fade(0, 12)
            .key(0, pose().up(1.4f).alpha(0f).spin(-270))
            .key(26, pose().up(0f).alpha(1f).spin(0), Ease.OUT);     // comes down turning, and settles on the head

    /** The crown comes down onto the head of an entity and stays there. It looks where the head looks. */
    public static void crown(ServerLevel sl, Entity wearer) {
        ParticleShapes.modelOnHead(sl, CROWN, wearer, 0, 0f, 0f, 0f);
    }

    /**
     * The crown for 'ticks' ticks: it comes down onto the head, follows it wherever it looks and fades at the
     * end. 'tag' is its name: calling this again with the same tag replaces it, and takeCrown(...) with it takes
     * it off early. The wearer does not see it in first person.
     */
    public static void crown(ServerLevel sl, Entity wearer, int ticks, String tag) {
        ParticleShapes.clearModels(sl, wearer, tag);
        ParticleShapes.modelOnHead(sl, CROWN.lifetime(ticks).tag(tag), wearer, 0, 0f, 0f, 0f);
    }

    /** Takes off, now, the crown that was put on with this tag. */
    public static void takeCrown(ServerLevel sl, Entity wearer, String tag) {
        ParticleShapes.clearModels(sl, wearer, tag);
    }

    // =====================================================================================
    // The giant skull
    // =====================================================================================

    public static final int SKULL_TICKS = 130;
    /** The tick its jaw has dropped (a scream, a breath of something). */
    public static final int SKULL_SCREAMS = 36;

    public static final ModelFx SKULL = ModelFx.of("pale_emperor/death_skull").frames(3).smooth()
            .scale(2f).pivot(8, 8, 8).glow().lifetime(SKULL_TICKS).fade(0, 12)
            .key(0, pose().scale(0.15f).alpha(0f))
            .key(10, pose().scale(1f).alpha(1f), Ease.OUT_BACK)          // pops into being
            .during(10, 40, pose().up(0.3f), Ease.IN_OUT)                // hangs in the air, rising and falling
            .during(40, 70, pose().up(0f), Ease.IN_OUT)
            .during(70, 100, pose().up(0.3f), Ease.IN_OUT)
            .during(30, 36, pose().frame(2).pitch(-10), Ease.OUT)        // throws its head back, jaw wide
            .during(64, 72, pose().frame(0).pitch(0), Ease.IN)           // shuts it
            .during(84, 87, pose().frame(1), Ease.OUT)                   // its teeth chatter twice
            .during(87, 90, pose().frame(0), Ease.IN)
            .during(92, 95, pose().frame(1), Ease.OUT)
            .during(95, 98, pose().frame(0), Ease.IN)
            .during(116, 130, pose().scale(0.5f).up(1.2f), Ease.IN);     // shrinks away upward

    /** A skull 4 blocks tall hangs over 'ground', screams once and fades. */
    public static void skull(ServerLevel sl, Vec3 ground, float yaw) {
        ParticleShapes.model(sl, SKULL, ground.add(0, 3.6, 0), yaw, 0f, 0f);
    }

    // =====================================================================================
    // The rib cage
    // =====================================================================================

    public static final int RIB_CAGE_TICKS = 116;
    /** The tick the cage is shut. */
    public static final int RIB_CAGE_SHUT = 20;
    /** The tick it starts to open again. */
    public static final int RIB_CAGE_OPENS = 90;

    /** Inside it is 2.6 blocks wide and 3.4 tall when shut. */
    public static final ModelFx RIB_CAGE = ModelFx.of("pale_emperor/rib_cage").frames(5).smooth()
            .scale(2f).pivot(8, -16, 8).glow().lifetime(RIB_CAGE_TICKS).fade(0, 4)
            .key(0, pose().up(-4.3f))
            .key(8, pose().up(0), Ease.OUT)                              // the open ribs break out of the ground
            .during(12, 20, pose().frame(4), Ease.IN)                    // and snap shut
            .during(90, 100, pose().frame(0), Ease.OUT)                  // they open
            .during(100, 114, pose().up(-4.3f), Ease.IN);                // and sink

    /** Ribs break out of the ground round a spot, close over it, hold, open and sink. */
    public static void ribCage(ServerLevel sl, Vec3 ground, float yaw) {
        ParticleShapes.model(sl, RIB_CAGE, ground, yaw, 0f, 0f);
    }

    public static final int RIBS_AROUND_TICKS = 44;
    /** The tick the ribs around an entity are shut. */
    public static final int RIBS_AROUND_SHUT = 20;

    /**
     * Ribs break out of the ground round an ENTITY, snap shut over it and fade into it: a ward closing on
     * someone. They are sized to fit the entity and go with it if it walks.
     */
    public static void ribsAround(ServerLevel sl, Entity entity) {
        // at scale 1 the shut cage is 1.3 blocks wide and 1.7 tall inside
        float scale = Math.max((entity.getBbHeight() + 0.2f) / 1.7f, (entity.getBbWidth() + 0.6f) / 1.3f);
        ModelFx ribs = RIB_CAGE.noKeys().scale(scale).lifetime(RIBS_AROUND_TICKS).fade(0, 12)
                .key(0, pose().up(-2.15f * scale))
                .key(8, pose().up(0), Ease.OUT)                              // the open ribs break out of the ground
                .during(12, RIBS_AROUND_SHUT, pose().frame(4), Ease.IN)      // and snap shut
                .during(28, RIBS_AROUND_TICKS, pose().scale(0.85f), Ease.IN);    // they draw in on it as they fade
        ParticleShapes.modelOn(sl, ribs, entity, Vec3.ZERO, entity.getYRot(), 0f, 0f);
    }

    // =====================================================================================
    // Bone spikes
    // =====================================================================================

    public static final int SPIKES_TICKS = 84;
    /** The tick the spikes are fully out. */
    public static final int SPIKES_OUT = 5;

    /** 4 blocks tall, 2.5 wide. */
    public static final ModelFx SPIKES = ModelFx.of("pale_emperor/bone_spikes")
            .scale(1.5f).pivot(8, -16, 8).glow().lifetime(SPIKES_TICKS).fade(0, 4)
            .key(0, pose().up(-4.3f))
            .key(5, pose().up(0), Ease.OUT_BACK)                         // burst out, a little too far, and settle
            .during(64, 82, pose().up(-4.3f), Ease.IN);

    /** Spikes burst out of the ground, stand, and sink back. */
    public static void boneSpikes(ServerLevel sl, Vec3 ground, float yaw) {
        ParticleShapes.model(sl, SPIKES, ground, yaw, 0f, 0f);
    }

    // =====================================================================================
    // The bone spear
    // =====================================================================================

    public static final int SPEAR_TICKS = 80;
    /** The tick the hovering spear lets fly. */
    public static final int SPEAR_FLIES = 64;

    /** The spear as it is, 3.6 blocks long, with a pale-green glow round it. */
    public static final ModelFx SPEAR = ModelFx.of("pale_emperor/bone_spear")
            .scale(1.2f).pivot(8, 8, 8).glow().aura(0x409CFFD2, 0.07f, 2).lifetime(40).fade(0, 4);
    private static final ModelFx SPEAR_HOVERS = SPEAR.lifetime(SPEAR_TICKS)
            .key(0, pose().scale(0.3f).alpha(0f))
            .key(8, pose().scale(1f).alpha(1f), Ease.OUT_BACK)           // appears, point up
            .during(8, 50, pose().spin(360), Ease.IN_OUT)                // turns once
            .during(50, 60, pose().pitch(90), Ease.IN_OUT)               // tips over: the point looks forward
            .during(60, 64, pose().forward(-1f), Ease.OUT)               // draws back
            .during(64, 76, pose().forward(26f), Ease.IN);               // and flies
    private static final ModelFx SPEAR_THROWN = SPEAR.lifetime(24)
            .key(0, pose().up(-0.8f).scale(0.5f))
            .key(4, pose().up(-1.4f).scale(1f), Ease.OUT)                // drawn back
            .key(16, pose().up(24f), Ease.IN)                            // 24 blocks in 12 ticks
            .during(4, 16, pose().spin(360));

    /** A spear appears over 'ground' point up, turns once, tips forward and flies 26 blocks the way 'yaw' looks. */
    public static void boneSpear(ServerLevel sl, Vec3 ground, float yaw) {
        ParticleShapes.model(sl, SPEAR_HOVERS, ground.add(0, 2.2, 0), yaw, 0f, 0f);
    }

    /** A spear thrown from 'from' along 'direction': it is 24 blocks away 16 ticks later. */
    public static void boneSpearThrow(ServerLevel sl, Vec3 from, Vec3 direction) {
        ParticleShapes.modelAlong(sl, SPEAR_THROWN, from, direction);
    }

    // =====================================================================================
    // The coffin and the wraith
    // =====================================================================================

    public static final int COFFIN_TICKS = 138;
    /** The tick the lid stands open. */
    public static final int COFFIN_OPEN = 48;

    /** 3.6 blocks tall. */
    public static final ModelFx COFFIN = ModelFx.of("pale_emperor/coffin").frames(4).smooth()
            .scale(1.3f).pivot(8, -16, 8).glow().lifetime(COFFIN_TICKS).fade(0, 4)
            .key(0, pose().up(-3.7f))
            .key(20, pose().up(0), Ease.OUT)                             // rises out of the ground
            .during(30, 48, pose().frame(3), Ease.IN_OUT)                // the lid swings open
            .during(104, 112, pose().frame(0), Ease.IN)                  // and slams
            .during(118, 136, pose().up(-3.7f), Ease.IN);

    public static final int WRAITH_TICKS = 110;
    /** The tick its arms are stretched out. */
    public static final int WRAITH_REACHES = 50;

    /** 3 blocks tall, hanging a little above the ground. */
    public static final ModelFx WRAITH = ModelFx.of("pale_emperor/wraith").frames(3).smooth()
            .scale(1.1f).pivot(8, -16, 8).glow().lifetime(WRAITH_TICKS).fade(0, 16)
            .key(0, pose().up(-0.5f).alpha(0f))
            .key(14, pose().up(0.35f).alpha(1f), Ease.OUT)               // rises out of the ground as it appears
            .during(14, 38, pose().up(0.6f), Ease.IN_OUT)                // hangs in the air, rising and falling
            .during(38, 62, pose().up(0.35f), Ease.IN_OUT)
            .during(62, 86, pose().up(0.6f), Ease.IN_OUT)
            .during(36, 60, pose().forward(1.6f), Ease.IN_OUT)           // drifts forward
            .during(40, 50, pose().frame(2), Ease.OUT)                   // and reaches out
            .during(78, 88, pose().frame(0), Ease.IN_OUT);

    /** A wraith rises at 'ground', drifts 1.6 blocks the way 'yaw' looks with its arms out, and fades. */
    public static void wraith(ServerLevel sl, Vec3 ground, float yaw) {
        ParticleShapes.model(sl, WRAITH, ground, yaw, 0f, 0f);
    }

    /** A coffin rises, its lid swings open, a wraith comes out of it; then the lid slams and the coffin sinks. */
    public static void coffin(ServerLevel sl, Vec3 ground, float yaw) {
        ParticleShapes.model(sl, COFFIN, ground, yaw, 0f, 0f);
        ModelFx out = WRAITH.delay(COFFIN_OPEN - 4).lifetime(70)
                .key(0, pose().up(0.2f).forward(0.5f).alpha(0f))         // it appears in the open doorway
                .key(14, pose().up(0.35f).alpha(1f), Ease.OUT);
        ParticleShapes.model(sl, out, ground, yaw, 0f, 0f);
    }

    // =====================================================================================
    // The tombstone and the hand from the grave
    // =====================================================================================

    public static final int TOMBSTONE_TICKS = 116;
    /** The tick the hand from the grave closes. */
    public static final int GRAVE_HAND_GRASPS = 62;

    /** 2.1 blocks tall; its grave reaches 1.5 blocks in front of it. */
    public static final ModelFx TOMBSTONE = ModelFx.of("pale_emperor/tombstone")
            .scale(1.2f).pivot(8, -16, 8).glow().lifetime(TOMBSTONE_TICKS).fade(0, 4)
            .key(0, pose().up(-2.3f))
            .key(14, pose().up(0), Ease.OUT_BACK)
            .during(100, 114, pose().up(-2.3f), Ease.IN);
    private static final ModelFx GRAVE_HAND = ModelFx.of("pale_emperor/skeletal_hand_grasp").frames(5).smooth()
            .scale(0.5f).pivot(8, -16, 8).glow().delay(30).lifetime(64).fade(0, 4)
            .key(0, pose().forward(0.95f).up(-1.6f).frame(3))            // under the middle of the grave mound
            .key(8, pose().up(-0.1f), Ease.OUT)                          // breaks out
            .during(4, 12, pose().frame(0), Ease.OUT)                    // spreads its fingers
            .during(26, 32, pose().frame(4), Ease.IN)                    // closes on whatever stands there
            .during(50, 62, pose().up(-1.6f), Ease.IN);                  // and pulls it down

    /** A headstone rises, and a skeletal hand breaks out of the grave in front of it and grasps. */
    public static void tombstone(ServerLevel sl, Vec3 ground, float yaw) {
        ParticleShapes.model(sl, TOMBSTONE, ground, yaw, 0f, 0f);
        ParticleShapes.model(sl, GRAVE_HAND, ground, yaw, 0f, 0f);
    }

    /**
     * A headstone (no hand) that rises, stands for as long as you want and sinks again in its last second:
     * 'ticks' in all, 40 at least. It is returned, not played, so you can still add to it:
     *   ParticleShapes.model(sl, PaleEmperorFx.tombstoneFor(300).delay(6).tag("my_graves"), ground, yaw, 0f, 0f);
     */
    public static ModelFx tombstoneFor(int ticks) {
        int all = Math.max(40, ticks);
        return TOMBSTONE.noKeys().lifetime(all)
                .key(0, pose().up(-2.3f))
                .key(14, pose().up(0), Ease.OUT_BACK)
                .during(all - 16, all - 2, pose().up(-2.3f), Ease.IN);
    }

    // =====================================================================================
    // Chains
    // =====================================================================================

    public static final int CHAIN_TICKS = 70;
    /** The tick a chain has reached what it was sent at. */
    public static final int CHAIN_ARRIVES = 5;
    private static final float CHAIN_LENGTH = 2.875f;                // blocks, at scale 1

    public static final ModelFx CHAIN = ModelFx.of("pale_emperor/underworld_chain")
            .pivot(8, -14, 8).glow().lifetime(CHAIN_TICKS).fade(0, 4)
            .key(0, pose().scale(0.04f))
            .key(5, pose().scale(1f), Ease.OUT)                          // shoots out of the spot it starts from
            .during(58, 68, pose().scale(0.04f), Ease.IN);               // and is drawn back in

    /** A chain shoots from 'from' and the claws at its end reach 'to'. It is stretched to fit. */
    public static void chain(ServerLevel sl, Vec3 from, Vec3 to) {
        Vec3 reach = to.subtract(from);
        float length = (float) reach.length() / CHAIN_LENGTH;
        float thick = Math.max(1.1f, Math.min(1.8f, length));        // long chains get longer links, not fatter ones
        ParticleShapes.modelAlong(sl, CHAIN.scale(thick, length, thick), from, reach);
    }

    /** A seal opens and four chains shoot out of its rim and seize what stands in its middle. */
    public static void chains(ServerLevel sl, Vec3 ground, float yaw) {
        seal(sl, ground, yaw, 6f, CHAIN_TICKS);
        Vec3 heart = ground.add(0, 1.5, 0);
        for (int i = 0; i < 4; i++) {
            double angle = Math.toRadians(yaw + 45 + i * 90);
            Vec3 from = ground.add(-Math.sin(angle) * 2.6, 0, Math.cos(angle) * 2.6);
            Vec3 stop = heart.add(from.subtract(heart).normalize().scale(0.45));    // the claws stop just short of the middle
            chain(sl, from, stop);
        }
    }

    // =====================================================================================
    // Falling feathers
    // =====================================================================================

    public static final int FEATHERS_TICKS = 130;
    /** One feather, 0.85 blocks long. */
    public static final ModelFx FEATHER = ModelFx.of("pale_emperor/pale_feather")
            .scale(0.55f).pivot(8, 8, 8).glow().lifetime(90).fade(6, 12);

    /** 'count' feathers drift down onto a round patch of ground, swaying and turning, each in its own time. */
    public static void featherFall(ServerLevel sl, Vec3 ground, double radius, int count) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2, far = Math.sqrt(random.nextDouble()) * radius;
            float fall = 3.5f + random.nextFloat() * 2f;
            float sway = 20f + random.nextFloat() * 20f;
            ModelFx feather = FEATHER.delay(random.nextInt(40)).spin(random.nextBoolean() ? 3f : -3f)
                    .key(0, pose().pitch(25f + random.nextFloat() * 40f).roll(-sway))    // leaning over, as a feather falls
                    .key(90, pose().up(-fall))
                    .during(0, 22, pose().roll(sway), Ease.IN_OUT)
                    .during(22, 45, pose().roll(-sway), Ease.IN_OUT)
                    .during(45, 68, pose().roll(sway), Ease.IN_OUT)
                    .during(68, 90, pose().roll(-sway), Ease.IN_OUT);
            Vec3 start = ground.add(Math.cos(angle) * far, fall + 0.3, Math.sin(angle) * far);
            ParticleShapes.model(sl, feather, start, random.nextFloat() * 360f, 0f, 0f);
        }
    }

    // =====================================================================================
    // The hands
    // =====================================================================================

    public static final int EMPEROR_HAND_TICKS = 96;
    /** The tick the hand has closed. */
    public static final int EMPEROR_HAND_GRASPS = 42;

    /** 6 blocks tall. Its palm is its front. */
    public static final ModelFx EMPEROR_HAND = ModelFx.of("pale_emperor/emperor_hand").frames(5).smooth()
            .scale(2f).pivot(8, -16, 8).glow().lifetime(EMPEROR_HAND_TICKS).fade(0, 4)
            .key(0, pose().frame(3).up(-6.2f))                           // half closed, under the ground
            .key(12, pose().up(0), Ease.OUT)                             // rises
            .during(6, 18, pose().frame(0), Ease.OUT)                    // opens wide
            .during(36, 42, pose().frame(4), Ease.IN)                    // closes
            .during(76, 94, pose().up(-6.2f), Ease.IN);                  // and takes what it holds down with it

    /** The hand of the Pale Emperor rises out of the ground, opens, closes and sinks. */
    public static void emperorHand(ServerLevel sl, Vec3 ground, float yaw) {
        ParticleShapes.model(sl, EMPEROR_HAND, ground, yaw, 0f, 0f);
    }

    public static final int ARMS_TICKS = 100;
    /** 3.3 blocks tall. */
    public static final ModelFx UNDERWORLD_ARM = ModelFx.of("pale_emperor/underworld_arm").frames(5).smooth()
            .scale(1.1f).pivot(8, -16, 8).glow().lifetime(80).fade(0, 4)
            .key(0, pose().frame(3).up(-3.4f))
            .key(10, pose().up(0), Ease.OUT)
            .during(4, 14, pose().frame(0), Ease.OUT)                    // opens
            .during(28, 34, pose().frame(4), Ease.IN)                    // clutches
            .during(40, 48, pose().frame(1), Ease.OUT)                   // opens again
            .during(52, 57, pose().frame(4), Ease.IN)                    // clutches again
            .during(64, 78, pose().up(-3.4f), Ease.IN);

    /**
     * One arm of the dead rises at a spot, its palm toward where 'yaw' looks and leaning that way, clutches
     * twice and sinks: put it beside someone, looking at them. 'tag' is its name ("" = none), to take it away
     * early with ParticleShapes.clearModels(sl, ground, tag).
     */
    public static void underworldArm(ServerLevel sl, Vec3 ground, float yaw, String tag) {
        ModelFx arm = ThreadLocalRandom.current().nextBoolean() ? UNDERWORLD_ARM : UNDERWORLD_ARM.mirrored();
        ParticleShapes.model(sl, arm.tag(tag), ground, yaw, 16f, 0f);
    }

    /** 'count' arms of the dead rise in a ring, all leaning in and clutching at the spot in the middle. */
    public static void underworldArms(ServerLevel sl, Vec3 ground, float yaw, int count) {
        for (int i = 0; i < count; i++) {
            float around = yaw + i * 360f / count;
            double angle = Math.toRadians(around);
            Vec3 at = ground.add(-Math.sin(angle) * 1.7, 0, Math.cos(angle) * 1.7);
            ModelFx arm = UNDERWORLD_ARM.delay(i * 3 % 10);
            // around + 180 = its palm looks back at the middle; pitch 16 = it leans that way
            ParticleShapes.model(sl, i % 2 == 0 ? arm : arm.mirrored(), at, around + 180f, 16f, 0f);
        }
    }

    // =====================================================================================
    // The display cases
    // =====================================================================================

    private interface Show {
        void play(ServerLevel sl, Entity viewer, Vec3 spot, float yaw);
    }

    /** worn = it goes on the viewer, not on a spot. away = it is shown looking away from the viewer. */
    private record Case(String name, int ticks, boolean worn, boolean away, Show show) {}

    private static final List<Case> CASES = List.of(
            new Case("serpent", SERPENT_TICKS, false, false, (sl, viewer, spot, yaw) -> serpent(sl, spot, yaw)),
            new Case("skull", SKULL_TICKS, false, false, (sl, viewer, spot, yaw) -> skull(sl, spot, yaw)),
            new Case("rib_cage", RIB_CAGE_TICKS, false, false, (sl, viewer, spot, yaw) -> ribCage(sl, spot, yaw)),
            new Case("spikes", SPIKES_TICKS, false, false, (sl, viewer, spot, yaw) -> boneSpikes(sl, spot, yaw)),
            new Case("spear", SPEAR_TICKS, false, true, (sl, viewer, spot, yaw) -> boneSpear(sl, spot, yaw)),
            new Case("coffin", COFFIN_TICKS, false, false, (sl, viewer, spot, yaw) -> coffin(sl, spot, yaw)),
            new Case("tombstone", TOMBSTONE_TICKS, false, false, (sl, viewer, spot, yaw) -> tombstone(sl, spot, yaw)),
            new Case("chains", CHAIN_TICKS, false, false, (sl, viewer, spot, yaw) -> chains(sl, spot, yaw)),
            new Case("wraith", WRAITH_TICKS, false, false, (sl, viewer, spot, yaw) -> wraith(sl, spot, yaw)),
            new Case("seal", 120, false, false, (sl, viewer, spot, yaw) -> seal(sl, spot, yaw, 6f, 120)),
            new Case("feathers", FEATHERS_TICKS, false, false, (sl, viewer, spot, yaw) -> featherFall(sl, spot, 2.5, 22)),
            new Case("emperor_hand", EMPEROR_HAND_TICKS, false, false, (sl, viewer, spot, yaw) -> emperorHand(sl, spot, yaw)),
            new Case("arms", ARMS_TICKS, false, false, (sl, viewer, spot, yaw) -> underworldArms(sl, spot, yaw, 6)),
            new Case("wings", WINGS_TICKS, true, false, (sl, viewer, spot, yaw) -> wings(sl, viewer)),
            new Case("crown", CROWN_TICKS, true, false, (sl, viewer, spot, yaw) -> crown(sl, viewer)),
            new Case("ribs_around", RIBS_AROUND_TICKS, true, false, (sl, viewer, spot, yaw) -> ribsAround(sl, viewer)));

    /** The names /palefx knows. */
    public static List<String> names() {
        List<String> names = new ArrayList<>();
        for (Case c : CASES) names.add(c.name());
        return names;
    }

    /** One display, 7 blocks in front of the viewer and turned toward them. False if there is none of that name. */
    public static boolean show(ServerLevel sl, Entity viewer, String name) {
        for (Case c : CASES) {
            if (!c.name().equals(name)) continue;
            float look = viewer.getYRot();
            Vec3 spot = viewer.position().add(ahead(look).scale(7));
            c.show().play(sl, viewer, spot, c.away() ? look : look + 180f);
            return true;
        }
        return false;
    }

    /** Every display at once, in a ring 16 blocks out from the viewer, each played 'rounds' times. */
    public static void gallery(ServerLevel sl, Entity viewer, int rounds) {
        List<Case> standing = new ArrayList<>();
        for (Case c : CASES) if (!c.worn()) standing.add(c);
        Vec3 middle = viewer.position();
        for (int i = 0; i < CASES.size(); i++) {
            Case c = CASES.get(i);
            int place = Math.max(0, standing.indexOf(c));                        // (what is worn has no place in the ring)
            float out = viewer.getYRot() + place * 360f / standing.size();       // the first one is straight ahead
            Vec3 spot = middle.add(ahead(out).scale(16));
            float yaw = c.away() ? out : out + 180f;
            for (int round = 0; round < rounds; round++) {
                Later.run(sl, round * (c.ticks() + 10), () -> {
                    if (viewer.isAlive()) c.show().play(sl, viewer, spot, yaw);
                });
            }
        }
    }

    /** One block the way a yaw looks, level with the ground. */
    private static Vec3 ahead(float yaw) {
        double angle = Math.toRadians(yaw);
        return new Vec3(-Math.sin(angle), 0, Math.cos(angle));
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("palefx")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("all")
                        .executes(ctx -> showAll(ctx.getSource(), 3))
                        .then(Commands.argument("rounds", IntegerArgumentType.integer(1, 20))
                                .executes(ctx -> showAll(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "rounds")))))
                .then(Commands.argument("name", StringArgumentType.word())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(names(), builder))
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            String name = StringArgumentType.getString(ctx, "name");
                            if (!show(ctx.getSource().getLevel(), player, name)) {
                                ctx.getSource().sendFailure(Component.literal("No display called " + name + ". There are: " + String.join(", ", names())));
                                return 0;
                            }
                            return 1;
                        })));
    }

    private static int showAll(CommandSourceStack source, int rounds) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        Player player = source.getPlayerOrException();
        gallery(source.getLevel(), player, rounds);
        return 1;
    }
}
