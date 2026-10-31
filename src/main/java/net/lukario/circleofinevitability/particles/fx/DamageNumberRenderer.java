package net.lukario.circleofinevitability.particles.fx;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.client.ClientConcealment;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.ParticleGroupRenderState;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Draws the damage numbers of /damagefx (who gets one is decided on the server, in combat/DamageFx).
 *
 * A number appears above the target's head, stays there without moving for HOLD_TICKS and is then gone.
 * It follows the head if the target walks. Several hits at once go on lines above each other, the first one lowest.
 * It always faces you, is always fully bright, and is hidden behind walls like everything else.
 *
 * The digits are textures/particle/fx/damage_digits.png: "0123456789." in white, each 5 x 7 pixels in a
 * 7 x 9 box. They are drawn in COLOR with a dark copy one pixel down and right, like the game's own text.
 *
 * (26.2: the DamageNumberParticles are a particle group of their own, NUMBERS, drawn like the slashes: see
 * SlashRenderTypes.)
 */
public final class DamageNumberRenderer {

    // =========================
    // The look: change these
    // =========================

    /** Colour of the number (0xAARRGGBB). */
    public static int COLOR = 0xFFFF2B2B;
    /** The player's cumulative damage, on a separate line below the red hits. */
    public static int CUMULATIVE_COLOR = 0xFFFFAA00;
    /** How long a number stays, in ticks (20 = one second). */
    public static int HOLD_TICKS = 20;
    /** How long it takes to go away after that. 0 = gone at once. */
    public static int FADE_TICKS = 3;
    /** Size: blocks per pixel of a digit. A digit is 7 pixels tall, so 0.04f makes it 0.28 blocks. */
    public static float PIXEL = 0.04f;
    /** How far above the top of the head the lowest number is, in blocks. */
    public static float ABOVE_HEAD = 0.35f;
    /** The same for players, whose name is in that spot. */
    public static float ABOVE_NAME = 0.85f;
    /** Further away than this many blocks a number is drawn bigger, so it does not get smaller on the screen... */
    public static float GROW_FROM = 10f;
    /** ...up to this many times its size. 1f = never bigger. */
    public static float MOST_GROWN = 4f;

    /** How many numbers fit above one head. One more, and the oldest makes room. */
    private static final int ROWS = 6;
    /** This close to the end of its HOLD_TICKS a number gives its line to a new one. */
    private static final int LEAVING_TICKS = 2;
    /** How dark the copy behind the number is (the game's text uses a quarter of the colour too). */
    private static final float SHADOW = 0.25f;

    // =========================
    // The texture
    // =========================

    private static final Identifier DIGITS =
            Identifier.fromNamespaceAndPath(CircleOfInevitability.MOD_ID, "fx/damage_digits");
    /** The characters in the texture, left to right. */
    private static final String GLYPHS = "0123456789.";
    private static final int CELL_WIDTH = 7;      // one character's box in the texture, with 1 clear pixel around it
    private static final int CELL_HEIGHT = 9;
    private static final int DIGIT_ADVANCE = 6;   // pixels from one digit to the next
    private static final int POINT_ADVANCE = 2;   // the point is 1 pixel wide
    private static final int ROW_HEIGHT = 9;      // pixels from one line of numbers to the next

    /** The particle group every DamageNumberParticle goes in. */
    static final ParticleRenderType NUMBERS = new ParticleRenderType("CIRCLE_OF_INEVITABILITY_DAMAGE_NUMBERS", "COID");

    /** See-through blending from the particle atlas, nothing written to depth (the same as a translucent slash). */
    private static final SingleQuadParticle.Layer LAYER = SlashRenderTypes.TRANSLUCENT;

    private static final List<DamageNumberParticle> ACTIVE = new ArrayList<>();
    private static boolean registered;

    private DamageNumberRenderer() {}

    /** One number as it is drawn this frame. */
    private record Shown(String text, Vec3 top, float above, int row, float alpha, int color) {}

    /**
     * Tells the particle engine how to draw NUMBERS. Called once from ClientParticleHandler; never throws.
     * If it fails, numbers keep ticking but are not drawn (DamageNumberParticle then stays NO_RENDER).
     */
    public static void register() {
        if (registered) return;
        try {
            ParticleEngine.registerParticleGroup(NUMBERS, NumberGroup::new);
            registered = true;
        } catch (RuntimeException | LinkageError e) {
            CircleOfInevitability.LOGGER.error("Could not register the damage number particle group; damage numbers will not be drawn", e);
        }
    }

    static boolean isRegistered() {
        return registered;
    }

    static void clearTotals() {
        for (DamageNumberParticle active : ACTIVE) {
            if (active.cumulative()) active.discard();
        }
        ACTIVE.removeIf(active -> !active.isAlive());
    }

    /**
     * The line a new number goes on: the lowest one free above its entity. A number in its last moments does not
     * hold its line any more, so damage that ticks once a second (burning) stays on the lowest line instead of
     * hopping between two. Whatever is still on the chosen line is taken away at once: two numbers never overlap.
     */
    static int claimRow(DamageNumberParticle fresh, ClientLevel level) {
        ACTIVE.removeIf(active -> !active.isAlive() || active.isStale(level));
        if (fresh.cumulative()) {
            // Replace this target's previous total in place; gold numbers never take a red hit's row.
            for (DamageNumberParticle active : ACTIVE) {
                if (active.cumulative() && active.entityId() == fresh.entityId()) active.discard();
            }
            ACTIVE.removeIf(active -> !active.isAlive());
            ACTIVE.add(fresh);
            return 0;
        }
        float leaving = Math.max(1, HOLD_TICKS - LEAVING_TICKS);
        boolean[] taken = new boolean[ROWS];
        DamageNumberParticle oldest = null;
        for (DamageNumberParticle active : ACTIVE) {
            if (active.cumulative() || active.entityId() != fresh.entityId() || active.time(0f) >= leaving) continue;
            taken[active.row() - 1] = true;
            if (oldest == null || active.time(0f) > oldest.time(0f)) oldest = active;
        }
        int row = 0;
        while (row < ROWS && taken[row]) row++;
        if (row == ROWS) row = oldest.row() - 1;    // every line is taken: the oldest number makes room
        row++;                                   // row zero is reserved for the gold total
        for (DamageNumberParticle active : ACTIVE) {
            if (!active.cumulative() && active.entityId() == fresh.entityId() && active.row() == row) active.discard();
        }
        ACTIVE.removeIf(active -> !active.isAlive());
        ACTIVE.add(fresh);
        return row;
    }

    // =========================
    // Working out what to draw (once a frame)
    // =========================

    private static List<Shown> collect(Iterable<DamageNumberParticle> particles, float partialTick) {
        List<Shown> shown = new ArrayList<>();
        if (Minecraft.getInstance().level == null) {
            ACTIVE.clear();
            return shown;
        }
        for (DamageNumberParticle particle : particles) {
            if (!particle.isAlive()) continue;
            // a number above someone concealed would give them away (see status/Concealment)
            if (particle.entityId() >= 0 && ClientConcealment.isHidden(particle.entityId())) continue;

            float time = particle.time(partialTick);
            float alpha = particle.cumulative() || FADE_TICKS <= 0 || time <= particle.holdTicks()
                    ? 1f : 1f - (time - particle.holdTicks()) / FADE_TICKS;
            if (alpha <= 0.004f) continue;

            Vec3 top = particle.top(partialTick);
            shown.add(new Shown(particle.text(), top, particle.named() ? ABOVE_NAME : ABOVE_HEAD, particle.row(),
                    Math.min(alpha, 1f), particle.cumulative() ? CUMULATIVE_COLOR : COLOR));
        }
        return shown;
    }

    /** Width of a number in pixels. */
    static int width(String text) {
        int width = 0;
        for (int i = 0; i < text.length(); i++) width += text.charAt(i) == '.' ? POINT_ADVANCE : DIGIT_ADVANCE;
        return Math.max(0, width - 1);      // no gap after the last character
    }

    // =========================
    // Drawing
    // =========================

    /**
     * Writes one number into the particle buffer. Positions are camera-relative, like every particle's.
     * u0..v1 = where damage_digits.png is in the particle atlas.
     */
    static void draw(VertexConsumer buffer, String text, Vec3 top, float above, int row, float alpha, int color,
                     Vec3 cam, float cameraYaw, float u0, float u1, float v0, float v1) {
        // the spot above the head, seen from the camera
        double px = top.x - cam.x;
        double py = top.y + above - cam.y;
        double pz = top.z - cam.z;
        double distance = Math.sqrt(px * px + py * py + pz * pz);
        if (distance < 1.0E-4) return;
        double fx = px / distance, fy = py / distance, fz = pz / distance;      // the way we look at it

        // "up" on the screen for something seen that way: straight up, and the more it is seen from above or
        // below, the more toward where the camera faces (Minecraft's camera never rolls). yaw 0 = south (+Z).
        double yaw = Math.toRadians(cameraYaw);
        double hx = fy * Math.sin(yaw);
        double hy = Math.sqrt(Math.max(0.0, 1.0 - fy * fy));
        double hz = -fy * Math.cos(yaw);
        // right = look x up
        double rx = fy * hz - fz * hy, ry = fz * hx - fx * hz, rz = fx * hy - fy * hx;
        double length = Math.sqrt(rx * rx + ry * ry + rz * rz);
        if (length < 1.0E-6) return;
        rx /= length;
        ry /= length;
        rz /= length;
        // up = right x look (exactly across the line of sight, so the number faces the camera)
        double ux = ry * fz - rz * fy, uy = rz * fx - rx * fz, uz = rx * fy - ry * fx;

        // one pixel of a digit, in blocks
        double pixel = PIXEL * Math.max(1.0, Math.min(MOST_GROWN, distance / GROW_FROM));
        rx *= pixel;
        ry *= pixel;
        rz *= pixel;
        ux *= pixel;
        uy *= pixel;
        uz *= pixel;

        float left = -width(text) / 2f;             // centred on the spot
        float bottom = row * ROW_HEIGHT;
        float red = ((color >> 16) & 0xFF) / 255f;
        float green = ((color >> 8) & 0xFF) / 255f;
        float blue = (color & 0xFF) / 255f;
        float opacity = ((color >>> 24) & 0xFF) / 255f * alpha;

        // the dark copy first, one pixel right and down, then the number on top of it
        for (int pass = 0; pass < 2; pass++) {
            float shade = pass == 0 ? SHADOW : 1f;
            float penX = pass == 0 ? left + 1f : left;
            float penY = pass == 0 ? bottom - 1f : bottom;
            for (int i = 0; i < text.length(); i++) {
                char character = text.charAt(i);
                int cell = GLYPHS.indexOf(character);
                if (cell >= 0) {
                    // the whole box is drawn, clear edge included, so the texture's pixels stay whole
                    float x0 = penX - 1f, x1 = x0 + CELL_WIDTH;
                    float y0 = penY - 1f, y1 = y0 + CELL_HEIGHT;
                    float cu0 = Mth.lerp((float) cell / GLYPHS.length(), u0, u1);
                    float cu1 = Mth.lerp((float) (cell + 1) / GLYPHS.length(), u0, u1);
                    // front (seen from the camera: bottom right, top right, top left, bottom left, like a vanilla particle)
                    vertex(buffer, px, py, pz, rx, ry, rz, ux, uy, uz, x1, y0, cu1, v1, red * shade, green * shade, blue * shade, opacity);
                    vertex(buffer, px, py, pz, rx, ry, rz, ux, uy, uz, x1, y1, cu1, v0, red * shade, green * shade, blue * shade, opacity);
                    vertex(buffer, px, py, pz, rx, ry, rz, ux, uy, uz, x0, y1, cu0, v0, red * shade, green * shade, blue * shade, opacity);
                    vertex(buffer, px, py, pz, rx, ry, rz, ux, uy, uz, x0, y0, cu0, v1, red * shade, green * shade, blue * shade, opacity);
                    // back, so it shows whichever side the game keeps (as SlashParticle does)
                    vertex(buffer, px, py, pz, rx, ry, rz, ux, uy, uz, x0, y0, cu0, v1, red * shade, green * shade, blue * shade, opacity);
                    vertex(buffer, px, py, pz, rx, ry, rz, ux, uy, uz, x0, y1, cu0, v0, red * shade, green * shade, blue * shade, opacity);
                    vertex(buffer, px, py, pz, rx, ry, rz, ux, uy, uz, x1, y1, cu1, v0, red * shade, green * shade, blue * shade, opacity);
                    vertex(buffer, px, py, pz, rx, ry, rz, ux, uy, uz, x1, y0, cu1, v1, red * shade, green * shade, blue * shade, opacity);
                }
                penX += character == '.' ? POINT_ADVANCE : DIGIT_ADVANCE;
            }
        }
    }

    /** One corner: the spot, plus x pixels to the right and y pixels up. */
    private static void vertex(VertexConsumer buffer, double px, double py, double pz,
                               double rx, double ry, double rz, double ux, double uy, double uz,
                               float x, float y, float u, float v, float red, float green, float blue, float alpha) {
        buffer.addVertex((float) (px + rx * x + ux * y), (float) (py + ry * x + uy * y), (float) (pz + rz * x + uz * y))
                .setUv(u, v)
                .setColor(red, green, blue, alpha)
                .setLight(LightCoordsUtil.FULL_BRIGHT);
    }

    // =========================
    // Particle group and render state
    // =========================

    private static final class NumberGroup extends ParticleGroup<DamageNumberParticle> {

        private NumberGroup(ParticleEngine engine) {
            super(engine);
        }

        @Override
        public ParticleGroupRenderState extractRenderState(Frustum frustum, Camera camera, float partialTick) {
            List<Shown> shown = collect(this.particles, partialTick);
            float u0 = 0f, u1 = 0f, v0 = 0f, v1 = 0f;
            if (!shown.isEmpty()) {
                TextureAtlas atlas = (TextureAtlas) Minecraft.getInstance().getTextureManager()
                        .getTexture(TextureAtlas.LOCATION_PARTICLES);
                TextureAtlasSprite sprite = atlas.getSprite(DIGITS);
                if (sprite == atlas.missingSprite()) {
                    shown.clear();          // a missing texture draws nothing
                } else {
                    u0 = sprite.getU0();
                    u1 = sprite.getU1();
                    v0 = sprite.getV0();
                    v1 = sprite.getV1();
                }
            }
            return new NumberState(shown, camera.position(), camera.yRot(), u0, u1, v0, v1);
        }
    }

    /** Drawn by vanilla's particle renderer, like any see-through particle; instead of one square per particle it writes the digits. */
    private static final class NumberState extends QuadParticleRenderState {

        private final List<Shown> numbers;
        private final Vec3 camera;
        private final float cameraYaw;
        private final float u0, u1, v0, v1;
        private final Set<SingleQuadParticle.Layer> layers = new LinkedHashSet<>();

        private NumberState(List<Shown> numbers, Vec3 camera, float cameraYaw, float u0, float u1, float v0, float v1) {
            this.numbers = numbers;
            this.camera = camera;
            this.cameraYaw = cameraYaw;
            this.u0 = u0;
            this.u1 = u1;
            this.v0 = v0;
            this.v1 = v1;
            if (!numbers.isEmpty()) layers.add(LAYER);
        }

        @Override
        public boolean isEmpty() {
            return numbers.isEmpty();
        }

        @Override
        public Set<SingleQuadParticle.Layer> layers() {
            return layers;
        }

        @Override
        public void buildLayer(SingleQuadParticle.Layer layer, VertexConsumer buffer) {
            if (layer != LAYER) return;
            for (Shown number : numbers) {
                draw(buffer, number.text(), number.top(), number.above(), number.row(), number.alpha(), number.color(),
                        camera, cameraYaw, u0, u1, v0, v1);
            }
        }

        @Override
        public void submit(SubmitNodeCollector submitNodeCollector, CameraRenderState cameraState) {
            if (!numbers.isEmpty()) submitNodeCollector.submitQuadParticleGroup(this);
        }

        @Override
        public void clear() {
            numbers.clear();
            layers.clear();
        }
    }
}
