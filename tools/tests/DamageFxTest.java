package net.lukario.circleofinevitability.combat;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import net.lukario.circleofinevitability.particles.fx.DamageNumber;
import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

/** Regression checks without starting a Minecraft client or server. */
public final class DamageFxTest {
    private static void close(float actual, float expected) {
        if (!Float.isFinite(actual) || Math.abs(actual - expected) > 0.0001f)
            throw new AssertionError(actual + " != " + expected);
    }

    private static void same(DamageNumber actual, DamageNumber expected) {
        close(actual.amount(), expected.amount());
        if (actual.entityId() != expected.entityId() || actual.cumulative() != expected.cumulative()
                || actual.holdTicks() != expected.holdTicks() || !actual.text().equals(expected.text()))
            throw new AssertionError("Particle did not survive serialization");
    }

    public static void main(String[] args) {
        UUID first = UUID.randomUUID(), second = UUID.randomUUID();
        DamageTotals player = new DamageTotals();
        close(player.add(first, 2.5f, 0, 100), 2.5f);
        close(player.add(first, 3f, 99, 100), 5.5f);
        // The timeout is measured from the latest hit, rather than the start of the streak.
        close(player.add(first, 4f, 198, 100), 9.5f);
        close(player.add(first, 1f, 298, 100), 1f); // exact reset boundary
        close(player.add(first, 2f, 500, 100), 2f); // beyond the boundary
        close(player.add(second, 10f, 501, 100), 10f);
        close(player.add(first, 3f, 502, 100), 5f); // another target never contributes
        close(new DamageTotals().add(first, 7f, 502, 100), 7f); // another dealer
        close(player.add(first, 1f, 503, 1), 1f); // customized timeout
        close(player.add(first, 2f, 0, 100), 2f); // time moving backwards cannot retain a streak
        DamageTotals tiny = new DamageTotals();
        for (int i = 0; i < 10; i++) tiny.add(first, 0.01f, i, 100);
        close(tiny.add(first, 0.01f, 10, 100), 0.11f); // sub-display hits still accumulate
        DamageTotals large = new DamageTotals();
        large.add(first, Float.MAX_VALUE, 0, 100);
        close(large.add(first, Float.MAX_VALUE, 1, 100), Float.MAX_VALUE);
        for (float invalid : new float[]{0f, -1f, Float.NaN, Float.POSITIVE_INFINITY}) {
            try {
                player.add(first, invalid, 1, 100);
                throw new AssertionError("Invalid damage accepted");
            } catch (IllegalArgumentException expected) { }
        }

        for (DamageNumber number : new DamageNumber[]{new DamageNumber(7.5f, 42),
                new DamageNumber(123.5f, 42, true, 100), new DamageNumber(2f, -1, true, 72000),
                new DamageNumber(0f, -1, true, 1)}) {
            var json = DamageNumber.CODEC.codec().encodeStart(JsonOps.INSTANCE, number).getOrThrow();
            same(DamageNumber.CODEC.codec().parse(JsonOps.INSTANCE, json).getOrThrow(), number);
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
            try {
                DamageNumber.STREAM_CODEC.encode(buffer, number);
                same(DamageNumber.STREAM_CODEC.decode(buffer), number);
                if (buffer.isReadable()) throw new AssertionError("Unread particle bytes");
            } finally {
                buffer.release();
            }
        }
        same(DamageNumber.CODEC.codec().parse(JsonOps.INSTANCE,
                JsonParser.parseString("{\"amount\":7.5,\"entity\":42}")).getOrThrow(), new DamageNumber(7.5f, 42));
        System.out.println("Cumulative damage and particle serialization checks passed.");
    }
}
