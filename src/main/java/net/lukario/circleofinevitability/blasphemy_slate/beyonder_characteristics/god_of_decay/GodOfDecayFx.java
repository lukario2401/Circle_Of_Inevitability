package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.god_of_decay;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.lukario.circleofinevitability.combat.HudTimer;
import net.lukario.circleofinevitability.combat.Later;
import net.lukario.circleofinevitability.combat.SpellFx;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.particles.fx.ModelFx;
import net.lukario.circleofinevitability.particles.fx.ParticleFx;
import net.lukario.circleofinevitability.particles.fx.ParticleShapes;
import net.lukario.circleofinevitability.screen.ScreenAnchor;
import net.lukario.circleofinevitability.screen.ScreenImages;
import net.lukario.circleofinevitability.sounds.ModSounds;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** God of Decay presentation only. No damage, status application, terrain edits or forced movement.
 * Timed models expire on their own; end(...) cancels a cast's tagged models early. The ability owns
 * periodic calls and therefore stops them when dispelled. See docs/GOD_OF_DECAY_FX.md for the contract.
 */
@Mod.EventBusSubscriber(modid = "circle_of_inevitability")
public final class GodOfDecayFx {
    private GodOfDecayFx() {}

    public static final int PATHWAY_COLOR = 0xFF9AB82E, PUS = 0xFFB5C43A, BLUE = 0xFF1F3350,
            DEATH = 0xFF16261A, SURE_HIT = 0xFFB8860B, GOLD = 0xFFA8903C,
            TIME = 0xFF7FB8C8, ASH = 0xFF9A9690, ENTROPY = 0xFF2A3A2A;
    public static final int HAND_RISEN = 16, FORM_RISEN = 20, BEAM_HITS = 6, GRAVEL_INTERVAL = 5;

    /** Every ability has both a wheel glyph and a status glyph. INCUBATION is never shown as a status. */
    public enum Ability {
        ULCEROUS_BURST, CARRIERS_TOUCH, RED_TAPE, OFFICIAL_CAPACITY, CORRUPTION, HOLLOW_OUT,
        LAZINESS, OBESITY, DEGENERATION, INCUBATION, ROT, DIVISION, PATHOGEN_FORM, CERTAIN_DEATH,
        SURE_HIT, DECAY, SWOLLEN_FORM, LEFT_HAND_OF_DECAY, WITHERING_PALM, GRAVEL_SHOT,
        SLOW_TIME, SEGMENTED_TIME, SENESCENCE, ENTROPY, VIRUS_INCARNATION, RAGNAROK;
        public String id() { return name().toLowerCase(Locale.ROOT); }
        public String icon() { return "ability_menus/god_of_decay/" + id(); }
        public String label() { return id().replace('_', ' '); }
    }
    public enum Form { SWOLLEN, TIME_GIANT, ANCIENT_GOD }
    public enum Disease { INFECTED, LAZY, OBESE, DEGENERATE, LATENT, ROTTING, CORRUPTED, HOLLOWED, DOOMED, AGED, SEGMENTED }

    private static final ParticleFx PATHOGEN = ParticleFx.of("fx/decay_pathogen").color(PUS)
            .endColor(0x009AB82E).size(.10f).endSize(.025f).lifetime(24, 8).friction(.91f).randomRotation();
    private static final ParticleFx DUST = ParticleFx.of("fx/decay_ash").color(ASH)
            .endColor(0x009A9690).size(.09f).endSize(.03f).lifetime(30, 10).gravity(.012f).randomRotation();
    private static final ParticleFx SHARD = ParticleFx.of("fx/decay_time_shard").color(TIME)
            .endColor(0x007FB8C8).size(.10f).lifetime(25, 8).glow();

    private static ModelFx model(String name, int ticks, String tag) {
        return ModelFx.of("god_of_decay/" + name).pivot(8, 0, 8)
                .lifetime(Math.max(1, ticks)).fade(Math.min(8, ticks / 4), Math.min(12, ticks / 4)).tag(tag);
    }
    private static Vec3 chest(LivingEntity e) { return e.position().add(0, e.getBbHeight() * .6, 0); }
    private static void sound(ServerLevel sl, Vec3 at, String name) {
        var event = ModSounds.GOD_OF_DECAY.get(name);
        if (event != null) Spells.sound(sl, at, event.get(), 1f, 1f);
    }
    private static void burst(ServerLevel sl, Vec3 at, int color) {
        ParticleShapes.burst(sl, PATHOGEN.color(color), at, 24, .035, .22);
    }
    private static void on(ServerLevel sl, LivingEntity e, String name, int ticks, String tag, float scale) {
        ParticleShapes.modelOnTurning(sl, model(name, ticks, tag).scale(scale), e, Vec3.ZERO, 0, 0, 0);
    }

    public static void status(Player player, Ability ability, int ticks) {
        if (ability == Ability.INCUBATION) return;
        String id = "decay_status_" + ability.id();
        ScreenImages.show(player, id, "god_of_decay/" + ability.id(), ScreenAnchor.BOTTOM,
                103 + ability.ordinal() % 9 * 18, -21 - ability.ordinal() / 9 * 18, 16, Math.max(1, ticks));
        HudTimer.show(player, id, ability.label(), ticks, PATHWAY_COLOR & 0xFFFFFF);
    }
    public static void statusEnd(Player player, Ability ability) {
        String id = "decay_status_" + ability.id();
        ScreenImages.hide(player, id);
        HudTimer.hide(player, id);
    }
    /** Use a nonempty unique tag for each cast, shared by all its pieces. Call on each affected entity/spot. */
    public static void end(ServerLevel sl, LivingEntity entity, Vec3 center, String tag) {
        if (tag == null || tag.isBlank()) return;
        ParticleShapes.clearModels(sl, entity, tag);
        ParticleShapes.clearModels(sl, center, tag);
        ParticleShapes.clearModels(sl, center, tag+"_cracks");
        ParticleShapes.clearModels(sl, center, tag+"_sky");
        if (entity instanceof Player player) ScreenImages.hide(player, "decay_" + tag + "_*");
    }

    public static void ulcerousBurst(ServerLevel sl, LivingEntity caster) {
        burst(sl, chest(caster), PUS);
        SpellFx.shockRing(sl, caster.position(), 5, PUS, 16, 0);
        ParticleShapes.ring(sl, PATHOGEN, chest(caster), .7, 48, .28);
        sound(sl, caster.position(), "ulcerous_burst");
    }
    public static void carriersTouch(ServerLevel sl, LivingEntity caster, int ticks, String tag) {
        for (int side : new int[]{-1, 1}) ParticleShapes.modelOnTurning(sl,
                model("pathogen", ticks, tag).scale(.22f).spin(3).key(0, ModelFx.pose().right(side * .4f)), caster,
                new Vec3(0, caster.getBbHeight() * .5, 0), 0, 0, 0);
        sound(sl, caster.position(), "carriers_touch");
    }
    public static void infectedHit(ServerLevel sl, LivingEntity target) { burst(sl, chest(target), PUS); }
    public static void contagion(ServerLevel sl, LivingEntity from, LivingEntity to) {
        ParticleShapes.line(sl, PATHOGEN.size(.07f), chest(from), chest(to), 3);
        infectedHit(sl, to);
        sound(sl, chest(to), "contagion");
    }
    public static void redTape(ServerLevel sl, LivingEntity target, int ticks, String tag) {
        on(sl, target, "red_tape", ticks, tag, Math.max(1, target.getBbHeight()));
        sound(sl, chest(target), "red_tape");
    }
    public static void officialCapacity(ServerLevel sl, LivingEntity caster, int ticks, String tag) {
        on(sl, caster, "time_ring", ticks, tag, 1.4f);
        SpellFx.circleUnder(sl, caster, 1.2, GOLD, ticks, tag);
        sound(sl, caster.position(), "official_capacity");
    }
    public static void capacityLink(ServerLevel sl, LivingEntity caster, LivingEntity ally) {
        ParticleShapes.line(sl, SHARD.color(GOLD), chest(caster), chest(ally), 2);
    }
    public static void corruption(ServerLevel sl, Vec3 center, double radius, int ticks, String tag) {
        zone(sl, center, radius, ticks, tag, GOLD, "corruption");
    }
    public static void hollowOut(ServerLevel sl, LivingEntity target, int ticks, String tag) {
        on(sl, target, "hollow_shell", ticks, tag, target.getBbHeight() / 1.6f);
        sound(sl, chest(target), "hollow_out");
    }

    /** LATENT is deliberately a no-op: not even a sound, marker, HUD icon or timer. */
    public static void disease(ServerLevel sl, LivingEntity target, Disease kind, int ticks, String tag) {
        switch (kind) {
            case LATENT -> { return; }
            case LAZY -> {
                ParticleShapes.modelOnHead(sl, model("time_ring", ticks, tag).color(PATHWAY_COLOR).scale(.65f).spin(-.4f), target, .25, 0, 0, 0);
                sound(sl, chest(target), "laziness");
            }
            case OBESE -> {
                ParticleShapes.modelOn(sl, model("time_ring", ticks, tag).color(PUS).scale(1.5f), target,
                        new Vec3(0, target.getBbHeight() * .45, 0), 0, 0, 0);
                sound(sl, chest(target), "obesity");
            }
            case DEGENERATE -> { on(sl, target, "decay_cracks", ticks, tag, 1.2f); sound(sl, chest(target), "degeneration"); }
            case ROTTING -> { on(sl, target, "pool", ticks, tag, 1.5f); sound(sl, chest(target), "rot"); }
            case CORRUPTED -> { SpellFx.circleUnder(sl, target, .8, GOLD, ticks, tag); }
            case HOLLOWED -> hollowOut(sl, target, ticks, tag);
            case DOOMED -> {
                ParticleShapes.modelOnHead(sl, model("pathogen", ticks, tag).color(DEATH).scale(.5f).spin(-2), target, .4, 0, 0, 0);
            }
            case AGED -> { on(sl, target, "decay_cracks", ticks, tag, 1.4f); sound(sl, chest(target), "senescence"); }
            case SEGMENTED -> segmentedTime(sl, target, ticks, tag);
            case INFECTED -> infectedHit(sl, target);
        }
    }
    /** Optional once-per-second accents while a visible disease is active. Never call for a latent disease. */
    public static void diseaseTick(ServerLevel sl, LivingEntity target, Disease kind) {
        if (kind == Disease.LATENT) return;
        int tint = switch (kind) {
            case AGED, DEGENERATE -> ASH;
            case CORRUPTED, HOLLOWED -> GOLD;
            case SEGMENTED -> TIME;
            case DOOMED -> DEATH;
            default -> PUS;
        };
        ParticleShapes.burst(sl, (kind == Disease.AGED ? DUST : PATHOGEN).color(tint), chest(target), 6, .01, .04);
    }

    /** Called only after the hidden eight-second incubation has actually broken out. */
    public static void outbreak(ServerLevel sl, LivingEntity target, int ticks, String tag) {
        for (Disease kind : List.of(Disease.LAZY, Disease.OBESE, Disease.DEGENERATE)) disease(sl, target, kind, ticks, tag);
        burst(sl, chest(target), PUS);
        sound(sl, chest(target), "outbreak");
    }
    /** Refresh from the owner only, e.g. every 20 ticks. No broadcast particles and no hidden disease pips. */
    public static void sickness(ServerPlayer owner, LivingEntity target, int count) {
        if (owner.level() != target.level()) return;
        Vec3 right = Spells.rightOf(owner);
        for (int i = 0, n = Math.clamp(count, 0, 6); i < n; i++) {
            Vec3 at = target.position().add(0, target.getBbHeight() + .45, 0).add(right.scale((i - (n - 1) * .5) * .15));
            ParticleShapes.spawnFor(owner, PATHOGEN.size(.07f).lifetime(22, 0).gravity(0), at, Vec3.ZERO);
        }
    }

    public static void division(ServerLevel sl, Vec3 at, float yaw, int ticks, String tag) {
        ParticleShapes.model(sl, model("double", ticks, tag).scale(.9f).color(0xB09AB82E)
                .key(0, ModelFx.pose().scale(.1f)).key(16, ModelFx.pose().scale(1), ModelFx.Ease.OUT), at, yaw, 0, 0);
        burst(sl, at.add(0, 1, 0), PUS);
        sound(sl, at, "division");
    }
    public static void pathogenForm(ServerLevel sl, LivingEntity caster, int ticks, String tag, boolean virus) {
        on(sl, caster, "pathogen", ticks, tag, virus ? 1.7f : .8f);
        burst(sl, chest(caster), PUS);
        sound(sl, caster.position(), virus ? "virus_incarnation" : "pathogen_form");
    }
    /** Called every 5 ticks while dissolved; does not hide the real entity (the ability owns that). */
    public static void pathogenTrail(ServerLevel sl, LivingEntity caster, boolean virus) {
        ParticleShapes.spiral(sl, PATHOGEN, caster.position().add(0, .15, 0), 3, virus ? 24 : 12, .1, virus ? 2 : .8, 1, .03);
    }
    public static void reform(ServerLevel sl, LivingEntity caster, String tag) {
        end(sl, caster, caster.position(), tag);
        ParticleShapes.spiral(sl, PATHOGEN, caster.position(), 3, 40, .2, 2, 2, -.08);
        sound(sl, caster.position(), "reform");
    }

    /** Beam windup is six ticks. Ability resolves damage at BEAM_HITS and calls beamImpact there. */
    public static void beam(ServerLevel sl, LivingEntity caster, Vec3 destination, boolean certainDeath) {
        int color = certainDeath ? DEATH : SURE_HIT;
        Vec3 start = caster.getEyePosition().add(caster.getLookAngle().scale(.5));
        burst(sl, start, color);
        sound(sl, start, certainDeath ? "certain_death" : "sure_hit");
        Later.run(sl, BEAM_HITS, () -> {
            if (!caster.isAlive() || caster.level() != sl) return;
            if (certainDeath) ParticleShapes.line(sl, SHARD.color(color).size(.22f), start, destination, 5);
            else {
                // A visibly curved guaranteed-hit path; actual cover traversal belongs to the ability.
                Vec3 previous = start;
                for (int i = 1; i <= 24; i++) {
                    double t = i / 24.0;
                    Vec3 next = start.lerp(destination, t).add(0, Math.sin(t * Math.PI) * 2, 0);
                    ParticleShapes.line(sl, SHARD.color(color), previous, next, 4);
                    previous = next;
                }
            }
        });
    }
    public static void beamImpact(ServerLevel sl, LivingEntity target, boolean certainDeath) {
        burst(sl, chest(target), certainDeath ? DEATH : SURE_HIT);
    }
    public static void sureHitSwing(ServerLevel sl, LivingEntity caster, LivingEntity target) {
        Vec3 from = chest(caster), to = chest(target), bend = from.lerp(to, .5).add(0, 1, 0);
        ParticleShapes.line(sl, SHARD.color(SURE_HIT), from, bend, 5);
        ParticleShapes.line(sl, SHARD.color(SURE_HIT), bend, to, 5);
        beamImpact(sl, target, false);
    }
    public static void dissolvedBeam(ServerLevel sl, LivingEntity caster, boolean certainDeath) {
        int color = certainDeath ? DEATH : SURE_HIT;
        SpellFx.shockRing(sl, caster.position(), 8, color, 16, 0);
        burst(sl, chest(caster), color);
        sound(sl, caster.position(), certainDeath ? "certain_death" : "sure_hit");
    }

    private static void zone(ServerLevel sl, Vec3 center, double radius, int ticks, String tag, int color, String sound) {
        float scale = (float) Math.clamp(radius * 16 / 7, .1, 110);
        ParticleShapes.model(sl, model("time_ring", ticks, tag).scale(scale, .5f, scale).color(color).spin(.15f), center.add(0, .05, 0), 0, 0, 0);
        SpellFx.shockRing(sl, center, radius, color, 20, 0);
        sound(sl, center, sound);
    }
    /** Area accents, called once per second; particle count stays bounded even for a 48-block domain. */
    public static void moveZone(ServerLevel sl, Vec3 center, String tag, Ability ability) {
        ParticleShapes.moveModel(sl,tag,center.add(0,.05,0),0,0,0);
        if(ability==Ability.DECAY)ParticleShapes.moveModel(sl,tag+"_cracks",center.add(0,.03,0),0,0,0);
        if(ability==Ability.RAGNAROK)ParticleShapes.moveModel(sl,tag+"_sky",center.add(0,24,0),0,0,0);
    }
    public static void zoneTick(ServerLevel sl, Vec3 center, double radius, int color) {
        ParticleShapes.ring(sl, DUST.color(color), center.add(0, .2, 0), Math.clamp(radius, 0, 48), 48, -.015);
    }
    public static void decay(ServerLevel sl, Vec3 center, double radius, int ticks, String tag, boolean aged) {
        zone(sl, center, radius, ticks, tag, aged ? ASH : PATHWAY_COLOR, "decay");
        ParticleShapes.model(sl, model("decay_cracks", ticks, tag+"_cracks").scale((float) radius), center.add(0, .03, 0), 0, 0, 0);
    }
    public static void weathering(ServerLevel sl, Vec3 at) {
        ParticleShapes.burst(sl, DUST, at, 14, .02, .13);
    }
    public static void form(ServerLevel sl, LivingEntity caster, Form form, float height, int ticks, String tag) {
        String name = switch (form) { case SWOLLEN -> "swollen_body"; case TIME_GIANT -> "time_giant"; case ANCIENT_GOD -> "ancient_god"; };
        ParticleShapes.modelOnTurning(sl, model(name, ticks, tag).scale(Math.clamp(height, 1, 48) / 2)
                .key(0, ModelFx.pose().scale(.1f).alpha(0)).key(FORM_RISEN, ModelFx.pose().scale(1).alpha(1), ModelFx.Ease.OUT), caster, Vec3.ZERO, 0, 0, 0);
        sound(sl, caster.position(), form == Form.SWOLLEN ? "swollen_form" : name);
    }
    public static void pathogenPool(ServerLevel sl, Vec3 at, int ticks, String tag) {
        ParticleShapes.model(sl, model("pool", ticks, tag).scale(2), at.add(0, .025, 0), 0, 0, 0);
        ParticleShapes.ring(sl, PATHOGEN, at.add(0, .1, 0), .8, 12, .01);
    }
    public static void formStep(ServerLevel sl, LivingEntity caster, Form form, float height) {
        SpellFx.shockRing(sl, caster.position(), Math.min(10, height * .6), form == Form.TIME_GIANT ? TIME : ASH, 12, 0);
        weathering(sl, caster.position());
    }
    public static void madness(Player viewer, float fraction, int ticks, String tag) {
        if (fraction <= 0) { ScreenImages.hide(viewer, "decay_" + tag + "_madness"); return; }
        ScreenImages.show(viewer, "decay_" + tag + "_madness", "god_of_decay/hand_vision", 80 + (int)(Math.clamp(fraction, 0, 1) * 100), ticks);
    }
    public static void leftHand(ServerLevel sl, LivingEntity caster, int ticks, String tag, boolean shield) {
        ParticleShapes.modelOnTurning(sl, model("left_hand", ticks, tag).seenByWearer().scale(shield ? 2 : 3)
                .key(0, ModelFx.pose().right(-.6f).forward(2).up(-2).alpha(0))
                .key(HAND_RISEN, ModelFx.pose().up(.2f).alpha(1), ModelFx.Ease.OUT), caster, Vec3.ZERO, 0, 0, 0);
        sound(sl, caster.position(), shield ? "withering_palm" : "left_hand_of_decay");
    }
    /** Only affected players receive the vision. Ability supplies the duration remaining after HAND_RISEN. */
    public static void handVision(Player target, int ticks, String tag) {
        ScreenImages.show(target, "decay_" + tag + "_hand", "god_of_decay/hand_vision", 320, ticks);
    }
    public static void palmBlock(ServerLevel sl, Vec3 impact) { weathering(sl, impact); sound(sl, impact, "weathering"); }
    /** Call six times at GRAVEL_INTERVAL ticks; one model travels to the snapshot aim in six ticks. */
    public static void gravelLaunch(ServerLevel sl, LivingEntity caster) {
        sound(sl,caster.getEyePosition(),"gravel_shot");
        ParticleShapes.burst(sl,SHARD,caster.getEyePosition().add(caster.getLookAngle()),6,.01,.08);
    }
    public static void gravelShot(ServerLevel sl, LivingEntity caster, Vec3 target, String tag) {
        Vec3 from = caster.getEyePosition();
        Vec3 delta = target.subtract(from);
        ParticleShapes.modelAlong(sl, model("gravel", 8, tag).scale(.25f)
                .key(0, ModelFx.pose()).key(6, ModelFx.pose().up((float) delta.length())), from, delta);
        ParticleShapes.line(sl, DUST, from, target, 2);
        sound(sl, from, "gravel_shot");
    }
    public static void slowTime(ServerLevel sl, Vec3 center, double radius, int ticks, String tag) {
        zone(sl, center, radius, ticks, tag, TIME, "slow_time");
    }
    public static void segmentedTime(ServerLevel sl, LivingEntity target, int ticks, String tag) {
        ParticleShapes.modelOnHead(sl, model("time_ring", ticks, tag).color(TIME).scale(1.2f).spin(2), target, .4, 0, 0, 0);
        sound(sl, chest(target), "segmented_time");
    }
    public static void heldBlow(ServerLevel sl, LivingEntity target, boolean release) {
        ParticleShapes.burst(sl, SHARD, chest(target), 12, release ? .1 : .01, release ? .3 : .03);
    }
    public static void senescence(ServerLevel sl, Vec3 center, double radius, int ticks, String tag) {
        zone(sl, center, radius, ticks, tag, ASH, "senescence");
        weathering(sl, center.add(0, 1, 0));
    }
    public static void entropy(ServerLevel sl, Vec3 center, double radius, int ticks, String tag) {
        zone(sl, center, radius, ticks, tag, ENTROPY, "entropy");
    }
    public static void ragnarok(ServerLevel sl, Vec3 center, double radius, int ticks, String tag) {
        zone(sl, center, radius, ticks, tag, ENTROPY, "ragnarok");
        ParticleShapes.model(sl, model("time_ring", ticks, tag+"_sky").scale((float)(radius * 2)).color(0xA02A3A2A).spin(-.3f), center.add(0, 24, 0), 0, 0, 0);
    }
    /** Refresh only for players inside the domain. A short TTL removes the sky promptly on exit. */
    public static void duskSky(Player viewer, String tag) {
        ScreenImages.show(viewer, "decay_" + tag + "_sky", "god_of_decay/dusk_sky", ScreenAnchor.TOP, 0, 0, 640, 160, 25);
    }
    public static void entropyCount(ServerPlayer owner, LivingEntity target, int count) {
        float scale = .2f + Math.clamp(count, 0, 100) / 100f;
        ParticleShapes.spawnFor(owner, model("time_ring", 22, "decay_count_" + target.getId()).scale(scale).color(ENTROPY)
                .following(target, new Vec3(0, target.getBbHeight() + .7, 0)), target.position().add(0, target.getBbHeight() + .7, 0), Vec3.ZERO);
    }
    public static void collapse(ServerLevel sl, LivingEntity target) {
        ParticleShapes.burst(sl, DUST, chest(target), 48, .04, .3);
        SpellFx.shockRing(sl, target.position(), 3, ASH, 12, 0);
        sound(sl, chest(target), "collapse");
    }

    private static final List<String> EXTRAS = List.of("outbreak", "contagion", "collapse", "reform", "time_giant", "ancient_god", "pathogen_pool", "sickness", "weathering", "avatars", "dissolved_beam", "sure_hit_swing", "held_blow", "madness");
    public static List<String> names() {
        return java.util.stream.Stream.concat(Arrays.stream(Ability.values()).map(Ability::id), EXTRAS.stream()).toList();
    }
    private static LivingEntity nearest(ServerLevel sl, Player viewer) {
        return sl.getEntitiesOfClass(LivingEntity.class, viewer.getBoundingBox().inflate(12), e -> e != viewer && e.isAlive())
                .stream().min(java.util.Comparator.comparingDouble(e -> e.distanceToSqr(viewer))).orElse(viewer);
    }
    /** Safe preview, including when no target is present. Durations are shortened to 4 seconds. */
    public static boolean show(ServerLevel sl, ServerPlayer viewer, String name) {
        if (!names().contains(name)) return false;
        LivingEntity target = nearest(sl, viewer);
        Vec3 at = viewer.position().add(Spells.flatLook(viewer).scale(6));
        String tag = "decay_preview_" + UUID.randomUUID();
        int ticks = 80;
        switch (name) {
            case "ulcerous_burst" -> ulcerousBurst(sl, viewer);
            case "carriers_touch" -> carriersTouch(sl, viewer, ticks, tag);
            case "red_tape" -> redTape(sl, target, ticks, tag);
            case "official_capacity" -> { officialCapacity(sl, viewer, ticks, tag); capacityLink(sl, viewer, target); }
            case "corruption" -> corruption(sl, at, 6, ticks, tag);
            case "hollow_out" -> hollowOut(sl, target, ticks, tag);
            case "laziness" -> disease(sl, target, Disease.LAZY, ticks, tag);
            case "obesity" -> disease(sl, target, Disease.OBESE, ticks, tag);
            case "degeneration" -> disease(sl, target, Disease.DEGENERATE, ticks, tag);
            case "incubation" -> { /* Intentionally nothing; /decayfx outbreak is the separate reveal. */ }
            case "rot" -> disease(sl, target, Disease.ROTTING, ticks, tag);
            case "division" -> division(sl, at, viewer.getYRot(), ticks, tag);
            case "pathogen_form", "virus_incarnation" -> {
                boolean virus = name.equals("virus_incarnation");
                pathogenForm(sl, viewer, ticks, tag, virus);
                for (int t = 5; t < ticks; t += 5) Later.run(sl, t, () -> { if (viewer.isAlive() && viewer.level() == sl) pathogenTrail(sl, viewer, virus); });
            }
            case "certain_death", "sure_hit" -> {
                boolean death = name.equals("certain_death");
                beam(sl, viewer, chest(target), death);
                Later.run(sl, BEAM_HITS, () -> { if (target.isAlive() && target.level() == sl) { beamImpact(sl, target, death); if (death) disease(sl, target, Disease.DOOMED, ticks, tag); } });
            }
            case "decay" -> decay(sl, at, 6, ticks, tag, false);
            case "swollen_form" -> form(sl, viewer, Form.SWOLLEN, 5, ticks, tag);
            case "left_hand_of_decay" -> { leftHand(sl, viewer, ticks, tag, false); handVision(viewer, ticks, tag); }
            case "withering_palm" -> { leftHand(sl, viewer, ticks, tag, true); palmBlock(sl, at); }
            case "gravel_shot" -> {
                for (int i = 0; i < 6; i++) Later.run(sl, i * GRAVEL_INTERVAL, () -> { if (viewer.isAlive() && viewer.level() == sl) gravelShot(sl, viewer, at.add(0, 1, 0), tag); });
            }
            case "slow_time" -> slowTime(sl, at, 8, ticks, tag);
            case "segmented_time" -> segmentedTime(sl, target, ticks, tag);
            case "senescence" -> { senescence(sl, at, 5, ticks, tag); disease(sl, target, Disease.AGED, ticks, tag); }
            case "entropy" -> entropy(sl, at, 8, ticks, tag);
            case "ragnarok" -> { ragnarok(sl, at, 12, ticks, tag); duskSky(viewer, tag); entropyCount(viewer, target, 75); }
            case "outbreak" -> outbreak(sl, target, ticks, tag);
            case "contagion" -> contagion(sl, viewer, target);
            case "collapse" -> collapse(sl, target);
            case "reform" -> reform(sl, viewer, tag);
            case "time_giant" -> form(sl, viewer, Form.TIME_GIANT, 24, ticks, tag);
            case "ancient_god" -> form(sl, viewer, Form.ANCIENT_GOD, 30, ticks, tag);
            case "pathogen_pool" -> pathogenPool(sl, at, ticks, tag);
            case "sickness" -> sickness(viewer, target, 6);
            case "weathering" -> weathering(sl, at);
            case "avatars" -> { for (int i = -1; i <= 1; i++) division(sl, at.add(i * 2, 0, 0), viewer.getYRot(), ticks, tag); }
            case "dissolved_beam" -> dissolvedBeam(sl, viewer, true);
            case "sure_hit_swing" -> sureHitSwing(sl, viewer, target);
            case "held_blow" -> heldBlow(sl, target, true);
            case "madness" -> madness(viewer, .7f, ticks, tag);
            default -> { return false; }
        }
        return true;
    }
    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("decayfx")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("all").executes(ctx -> {
                    ServerPlayer viewer = ctx.getSource().getPlayerOrException();
                    ServerLevel sl = ctx.getSource().getLevel();
                    int delay = 0;
                    for (String name : names()) {
                        Later.run(sl, delay, () -> {
                            if (!viewer.isAlive() || viewer.level() != sl || viewer.hasDisconnected()) return;
                            viewer.sendOverlayMessage(Component.literal("God of Decay FX: " + name));
                            show(sl, viewer, name);
                        });
                        delay += 100;
                    }
                    return 1;
                }))
                .then(Commands.argument("name", StringArgumentType.word())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(names(), builder))
                        .executes(ctx -> {
                            String name = StringArgumentType.getString(ctx, "name");
                            if (show(ctx.getSource().getLevel(), ctx.getSource().getPlayerOrException(), name)) return 1;
                            ctx.getSource().sendFailure(Component.literal("Unknown God of Decay FX: " + name));
                            return 0;
                        })));
    }
}
