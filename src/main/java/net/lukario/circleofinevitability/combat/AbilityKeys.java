package net.lukario.circleofinevitability.combat;

import java.util.*;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.*;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.PathwayAbilities;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.key_of_stars.KeyOfStarsRecord;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.omniscient_eye.Codex;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.worm_of_time.StolenAbilities;
import net.lukario.circleofinevitability.status.KeySeal;
import net.minecraft.server.level.ServerPlayer;

/** Rank the usable key containers, including the powers stored behind a copying key. */
public final class AbilityKeys {
  private record Candidate(int key, int sequence) {}

  private AbilityKeys() {}

  public static int strongest(ServerPlayer p) {
    var candidates = new ArrayList<Candidate>();
    String pathway = SoulCore.getPathway(p);
    for (var ability : PathwayAbilities.of(pathway))
      if (Beyonder.hasSequence(p, ability.sequence()))
        candidates.add(new Candidate(ability.key(), ability.sequence()));
    if (pathway.equals("Worm Of Time"))
      for (var held : StolenAbilities.held(p))
        candidates.add(new Candidate(2, held.ability().sequence()));
    if (pathway.equals("Key Of Stars"))
      for (var page : KeyOfStarsRecord.recorded(p))
        candidates.add(new Candidate(4, page.cast().sequence()));
    if (pathway.equals("Omniscient Eye"))
      for (var row : Codex.rows(p))
        if (row.study() >= 100 && row.sequence() >= SoulCore.getSequence(p))
          candidates.add(new Candidate(4, row.sequence()));
    if (AbilityGrants.has(p)) candidates.add(new Candidate(4, AbilityGrants.sequence(p)));
    return candidates.stream()
        .min(Comparator.comparingInt(Candidate::sequence).thenComparingInt(Candidate::key))
        .map(Candidate::key)
        .orElse(KeySeal.lastUsed(p));
  }
}
