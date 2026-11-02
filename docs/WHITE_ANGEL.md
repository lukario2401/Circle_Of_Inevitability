# White Angel: first ability tranche

Fifteen of the 29 draft abilities are playable. Set the pathway with `/sequence pathway <player> "White Angel"`
and use the usual Sequence commands to select an unlocked tier.

| Key | Unlock | Normal press | Sneak press |
| --- | --- | --- | --- |
| 1 | 9 | Hymn of Valor | Hymn of Renewal |
| 2 | 8 | Sunshine | Daytime (press again to lower for free) |
| 3 | 7 | Theurgy wheel | Last Theurgy pick |
| 4 | 6 | Notarization wheel | Last Notarization pick |
| 5 | 4 | Radiance wheel | Last Radiance pick |

Theurgy supplies Holy Light Summoning, Sun Halo, Holy Oath, Cleave of Purification and Fire of Light.
Notarization supplies Contract, Authenticate, Amplify and Nullify. Radiance currently supplies Purification
and Unshadowed Domain. Keys 6 and 7 and the other four Radiance entries remain for the next tranche.

There are no ability cooldowns. Opening a wheel is free; casts consume spirituality and refused targets
consume none. Daytime, Halo and Domain close for free even with an empty spirituality pool. Named fields
and weapon coats replace existing instances. Fields end on death, logout, dimension change or pathway loss.
Radius and buff duration scale by five percent per Sequence gained. Light fields supply forced daylight
without changing the world's time. Base purification damage favors undead, evil and corrupted targets.

Contract automatically binds eligible mobs. Players receive an offer and must run `/whitepact accept`
within 20 seconds while within 16 blocks. The offer is charged when sent. Pacts have per-caster slot limits;
violating one causes six pulses of magic damage. The specially protected stronger Sequence cannot violate
it; excessively strong players cannot accept it. Holy Contracts add a health penalty.

The implementation includes the Sequence 5 holy-light upgrade, stronger purification halos, shared Solar
Oath, stronger Amplify, Holy Contract strength, Domain status immunities, and Sun's quarter-price Notarization.
Night Vision, Fire Resistance, Horror Immunity and Notary Fortune are available at their respective tiers.

Some draft refinements remain: full Blessing/Fervor presets and physical passives, client-only Evil Detection,
custom Notary overhead proclamations, Holy Contract area binding, Nullify's shell weakening, and the exact
strongest-key ranking across copied abilities. Purification selects the strongest unlocked native key.
Golden-flame breach punishment currently uses magic damage pulses rather than an immunity-bypassing damage type.
These differences are explicit starting behavior for this tranche; the remaining authority abilities are absent.

Run `./gradlew build` and `./gradlew runGameTestServer -PwhiteAngelTests`. The server regression verifies
Domain's price and free lowering with zero mana, Renewal's lack of a self-refund, and Sequence gating.
The existing asset validator covers resource integrity. Final visual placement and multiplayer balance still
need a development-client review.
