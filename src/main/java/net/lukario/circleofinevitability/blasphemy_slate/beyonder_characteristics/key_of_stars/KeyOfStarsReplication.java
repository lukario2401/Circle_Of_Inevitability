package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.key_of_stars;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonder;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.SoulCore;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.omniscient_eye.Codex;
import net.lukario.circleofinevitability.combat.AbilityCatalog;
import net.lukario.circleofinevitability.combat.AbilityStudy;
import net.lukario.circleofinevitability.combat.Casts;
import net.lukario.circleofinevitability.combat.Cooldowns;
import net.lukario.circleofinevitability.combat.Echo;
import net.lukario.circleofinevitability.combat.Spells;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/** A Planeswalker's permanent memory of witnessed powers. Scribe pages have their own one-use inventory. */
public final class KeyOfStarsReplication {
    public static final AbilityStudy MEMORY = new AbilityStudy("key_of_stars_replication");
    static final String COOLDOWN_KEY = "key_of_stars_replication";
    static final int SEQUENCE = 2;
    static final int COST = 15000;
    static final int COOLDOWN = 200;
    static final float STRENGTH = 0.8f;

    private KeyOfStarsReplication() {}

    public static boolean enabled(Player player) {
        return KeyOfStars.PATHWAY.equals(SoulCore.getPathway(player)) && SoulCore.getSequence(player) <= SEQUENCE;
    }

    public static void use(Player player) {
        if (!(player instanceof ServerPlayer caster) || !KeyOfStars.ready(caster, false, SEQUENCE)) return;
        if (caster.isShiftKeyDown()) {
            Codex.open(caster);
            return;
        }

        AbilityCatalog.Power power = MEMORY.selectedPower(caster);
        if (power == null) {
            caster.sendSystemMessage(Component.literal("Sneak + ability 7 to choose a fully studied power at your Sequence or weaker."));
            return;
        }
        if (!Cooldowns.ready(caster, COOLDOWN_KEY, "Replication")) return;
        int cost = KeyOfStars.wheelCostFor(caster, COST);
        if (SoulCore.getSpirituality(caster) < Spells.costFor(caster, cost)) {
            caster.sendSystemMessage(Component.translatable("message.circle_of_inevitability.not_enough_spirituality"));
            return;
        }
        boolean cast = Beyonder.atOwnSequence(caster,
                () -> Echo.weaker(caster, STRENGTH, () -> Casts.recast(caster, power.cast())));
        if (!cast) {
            caster.sendSystemMessage(Component.literal(power.name() + " would not go off from here."));
            return;
        }
        Spells.paySpirituality(caster, cost);
        Cooldowns.start(caster, COOLDOWN_KEY, KeyOfStars.cooldownFor(caster, COOLDOWN));
        KeyOfStarsFx.replication(caster.level(), caster);
        caster.sendSystemMessage(Component.literal("Replicated " + power.name() + "."));
    }
}
