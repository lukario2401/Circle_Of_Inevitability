package net.lukario.circleofinevitability.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonder;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * What one Beyonder Characteristic item holds: the Pathway (its exact name, as in SequenceCommands.PATHWAYS)
 * and the Sequence (9 weakest .. 0 strongest). Stored on the stack as the data component
 * ModDataComponents.CHARACTERISTIC, so one item can be every Pathway and every Sequence.
 *
 *   ItemStack stack = BeyonderCharacteristicItem.of("Wanderer", 5);
 *   BeyonderCharacteristic c = stack.get(ModDataComponents.CHARACTERISTIC.get());   // null on a bare stack
 *
 * In commands: /give @s circle_of_inevitability:beyonder_characteristic[circle_of_inevitability:characteristic={pathway:"Wanderer",sequence:5}]
 * (a stack made that way gets its name and description once it is in a player's inventory: CharacteristicLooks).
 */
public record BeyonderCharacteristic(String pathway, int sequence) {

    public static final Codec<BeyonderCharacteristic> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("pathway").forGetter(BeyonderCharacteristic::pathway),
            Codec.intRange(Beyonder.STRONGEST_SEQUENCE, Beyonder.WEAKEST_SEQUENCE).fieldOf("sequence").forGetter(BeyonderCharacteristic::sequence)
    ).apply(instance, BeyonderCharacteristic::new));

    public static final StreamCodec<ByteBuf, BeyonderCharacteristic> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, BeyonderCharacteristic::pathway,
            ByteBufCodecs.VAR_INT, BeyonderCharacteristic::sequence,
            BeyonderCharacteristic::new);
}
