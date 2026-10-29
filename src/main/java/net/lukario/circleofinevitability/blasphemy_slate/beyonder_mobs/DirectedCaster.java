package net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs;

/** A custom encounter owns attack selection; shared costs, blockers, cooldowns and cast history still apply. */
public interface DirectedCaster {
    default float abilityDamageMultiplier() { return 1; }
    void onAbilityCast(MobAbility ability);
}
