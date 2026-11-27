package net.lukario.circleofinevitability.item.artifacts.uniqueness.drawbacks;

import net.lukario.circleofinevitability.item.artifacts.uniqueness.ShadowSoldier;
import net.lukario.circleofinevitability.item.artifacts.uniqueness.UniquenessDrawback;
import net.lukario.circleofinevitability.item.artifacts.uniqueness.UniquenessItem;
import net.lukario.circleofinevitability.item.artifacts.uniqueness.Wielder;
import net.lukario.circleofinevitability.screen.ScreenImages;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * Whispers of war, heard and seen by the wielder alone: war drums, clashing steel, a distant horn, red flashes at the
 * edge of sight, voices, and shadow soldiers that are not there (illusions: they look like the real ones until they
 * strike, or are struck, and vanish). 1 in oneIn[level] each second; 0 = never at that level.
 */
public final class WhispersOfWarDrawback implements UniquenessDrawback {
    public enum Whisper { DRUMS, CLASH, HORN, FLASH, VOICE, PHANTOM }

    private static final String[] VOICES = {"Advance.", "No retreat.", "Burn it all.", "The crown is yours. Take it.",
            "Kneel, or burn.", "They are behind you.", "One more war. Just one more.", "Every banner will be yours."};

    private final int minLevel;
    private final int[] oneIn;
    private final String flashImage;

    public WhispersOfWarDrawback(int minLevel, int[] oneIn, String flashImage) {
        this.minLevel = minLevel;
        this.oneIn = oneIn;
        this.flashImage = flashImage;
    }

    @Override public int minLevel() { return minLevel; }

    @Override
    public Component describe() {
        return Component.literal("Whispers of war: drums, steel and voices only you hear, red flashes, and soldiers that "
                + "are not there; you cannot tell which ones are real.");
    }

    @Override
    public void second(Wielder w) {
        if (!UniquenessItem.active(this, w)) return;
        int chance = w.at(oneIn);
        if (chance > 0 && w.player().getRandom().nextInt(chance) == 0) {
            Whisper[] all = Whisper.values();
            whisper(w, all[w.player().getRandom().nextInt(all.length)]);
        }
    }

    public void whisper(Wielder w, Whisper whisper) {
        ServerPlayer player = w.player();
        var random = player.getRandom();
        switch (whisper) {
            case DRUMS -> {
                for (int i = 0; i < 3; i++) privately(player, SoundEvents.NOTE_BLOCK_BASEDRUM, around(player, 6), 1f, .5f + i * .05f);
            }
            case CLASH -> {
                privately(player, Holder.direct(SoundEvents.ANVIL_LAND), around(player, 8), .4f, 1.6f);
                privately(player, Holder.direct(SoundEvents.PLAYER_ATTACK_STRONG), around(player, 8), 1f, .8f);
            }
            case HORN -> privately(player, SoundEvents.RAID_HORN, around(player, 24), 2f, .9f);
            case FLASH -> ScreenImages.showFullScreen(player, w.key("flash"), flashImage, 12);
            case VOICE -> player.sendSystemMessage(Component.literal(VOICES[random.nextInt(VOICES.length)])
                    .withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
            case PHANTOM -> {
                Vec3 at = ShadowSoldier.spotNear(w.level(), player.position(), 4, 8, random);
                if (at != null)
                    ShadowSoldier.rise(w.level(), at, Math.min(3, w.corruptionLevel()), ShadowSoldier.SHADOW, player, false, true, 20 * 30);
            }
        }
    }

    private static Vec3 around(ServerPlayer player, double distance) {
        double angle = player.getRandom().nextDouble() * Math.PI * 2;
        return player.position().add(Math.cos(angle) * distance, 1, Math.sin(angle) * distance);
    }

    /** A sound only this player hears. */
    private static void privately(ServerPlayer player, Holder<SoundEvent> sound, Vec3 at, float volume, float pitch) {
        player.connection.send(new ClientboundSoundPacket(sound, SoundSource.HOSTILE, at.x, at.y, at.z, volume, pitch,
                player.getRandom().nextLong()));
    }
}
