package net.lukario.frogerealm.screen;

/**
 * Which part of the screen an image is attached to.
 * The same part of the image lines up with it, e.g. BOTTOM_RIGHT puts the image's
 * bottom-right corner in the screen's bottom-right corner; CENTER centers it.
 * The x/y offsets you pass to ScreenImages then move it from there (+x = right, +y = down).
 */
public enum ScreenAnchor {
    TOP_LEFT(0f, 0f),    TOP(0.5f, 0f),    TOP_RIGHT(1f, 0f),
    LEFT(0f, 0.5f),      CENTER(0.5f, 0.5f), RIGHT(1f, 0.5f),
    BOTTOM_LEFT(0f, 1f), BOTTOM(0.5f, 1f), BOTTOM_RIGHT(1f, 1f);

    public final float fractionX;
    public final float fractionY;

    ScreenAnchor(float fractionX, float fractionY) {
        this.fractionX = fractionX;
        this.fractionY = fractionY;
    }
}
