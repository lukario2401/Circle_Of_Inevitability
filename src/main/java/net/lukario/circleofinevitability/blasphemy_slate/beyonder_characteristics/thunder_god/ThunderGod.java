package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.thunder_god;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonder;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.SoulCore;
import net.lukario.circleofinevitability.combat.Casts;
import net.lukario.circleofinevitability.combat.MeleeCombo;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.menu.AbilityWheel;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.lukario.circleofinevitability.status.ConjuredArmor;
import net.lukario.circleofinevitability.status.Form;
import net.lukario.circleofinevitability.status.Silence;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Playable first half: Sailor (9) through Ocean Songster (5). No higher-sequence entries are registered. */
public final class ThunderGod {
    public static final String PATHWAY = "Thunder God", PREFIX = "thunder_god_";
    public static final String SCALES = PREFIX + "scales", WRATH = PREFIX + "wrath";
    public enum Spell {
        HARPOON(9,10,0,"Throw a harpoon; drag an enemy or grapple to solid ground/water."),
        ILLUSORY_SCALES(9,10,0,"Ten seconds of scales and absorption. Stronger while wet. Cast again to dismiss freely."),
        RAGING_BLOW(8,200,0,"Spend at least three Rage on one blow and its splash. Full Rage breaks a guard."),
        WRATH(8,200,0,"Strength II, Speed I, faster swings and fear immunity for ten seconds; weakness afterward."),
        WATER_VEIL(7,1250,200,"Drown and silence one enemy for six seconds. Cleanse removes the veil."),
        SWEEPING_WAVE(7,1250,160,"A six-block-wide wave sweeps 18 blocks ahead, soaking and pushing enemies."),
        TIDEHEAL(7,1500,280,"Restore 12 health over six seconds; cleanse and extinguish yourself or an ally."),
        WATER_CURTAIN(5,3000,300,"A water curtain stops hostile projectiles, slows enemies and halves fire/explosion damage inside."),
        WATER_SPHERE(5,4000,400,"Enclose an enemy in water for five seconds, holding and drowning it. Bosses: half duration."),
        CORROSIVE_RAIN(5,4500,500,"Ten seconds of local rain; soaks, damages and wears enemy armor, then corrodes persistent targets."),
        WINDBLADE(6,900,60,"Three windblades fan out; each can pierce two enemies."),
        GALE_BURST(6,2450,240,"Blast enemies and hostile projectiles away; draw loose items and arrows into your palm."),
        GALE_RIDE(6,1800,400,"Double running speed on the ground, or fly after a jump. Safe landing; sneak to hover."),
        WIND_BINDING(5,3500,300,"Hold one enemy in spiraling winds for three seconds; nearby enemies for half as long."),
        LIGHTNING_STRIKE(5,3000,120,"Strike a visible enemy with silver lightning, or charge your next six weapon blows."),
        LIGHTNING_BOLT_ARROW(5,4000,160,"Draw a bow/crossbow for one second, then fire a silver bolt through up to two enemies."),
        SIREN_SONG(5,3000,360,"Daze nearby enemies until damaged and empower your next Raging Blow for ten seconds."),
        DISCORD(5,3000,400,"Confuse nearby enemies for eight seconds; mobs fight the nearest body, players falter and bleed."),
        THUNDERCLAP(5,3500,280,"Thunder awes enemies within ten blocks: damage, brief stiffness and fear.");
        public final int sequence, cost, cooldown;
        public final String description;
        Spell(int sequence, int cost, int cooldown, String description) {
            this.sequence=sequence;this.cost=cost;this.cooldown=cooldown;this.description=description;
        }
        public String id() { return name().toLowerCase(java.util.Locale.ROOT); }
        public String label() {
            return java.util.Arrays.stream(id().split("_")).map(s -> Character.toUpperCase(s.charAt(0))+s.substring(1))
                    .collect(java.util.stream.Collectors.joining(" "));
        }
    }
    public static final AbilityWheel TIDES = wheel("tides", "Tides", Spell.WATER_VEIL, Spell.SWEEPING_WAVE, Spell.TIDEHEAL,
            Spell.WATER_CURTAIN, Spell.WATER_SPHERE, Spell.CORROSIVE_RAIN);
    public static final AbilityWheel SKY = wheel("sky", "Sky", Spell.WINDBLADE, Spell.GALE_BURST, Spell.GALE_RIDE, Spell.WIND_BINDING);
    public static final AbilityWheel THUNDER = wheel("thunder", "Thunder", Spell.LIGHTNING_STRIKE, Spell.LIGHTNING_BOLT_ARROW);
    public static final AbilityWheel VOICE = wheel("voice", "Voice", Spell.SIREN_SONG, Spell.DISCORD, Spell.THUNDERCLAP);
    public static final MeleeCombo COMBO = combo();
    private ThunderGod() {}
    private static MeleeCombo combo() {
        MeleeCombo combo = MeleeCombo.forPathway(PATHWAY).resetAfter(40);
        for (int i=0;i<8;i++) { int step=i; combo.step((p,t,sl)->ThunderGodPowers.melee(p,t,sl,step)); }
        return combo;
    }
    private static AbilityWheel wheel(String id, String title, Spell... spells) {
        AbilityWheel wheel=AbilityWheel.create(PREFIX+id,title).pathway(PATHWAY).quickCastCooldown(true).quickCastCost(1);
        for (Spell spell:spells) wheel.entry(spell.id(),"ability_menus/thunder_god/"+spell.id(),spell.label(),spell.sequence,spell.cost,spell.cooldown,
                (p,sl)->ready(p,spell.sequence,Casts.isCopying(p)||Beyonder.isCastingUnbound(p))
                        ? ThunderGodPowers.cast(p,sl,spell) : AbilityWheel.Result.REFUSED).description(spell.description);
        return wheel;
    }
    public static boolean is(LivingEntity e,int sequence) {
        return e instanceof Player p && PATHWAY.equals(SoulCore.getPathway(p)) && SoulCore.getSequence(p)<=sequence;
    }
    private static boolean ready(Player p,int sequence,boolean borrowed) {
        return !p.level().isClientSide() && p.isAlive() && (borrowed||PATHWAY.equals(SoulCore.getPathway(p))) && Beyonder.hasSequence(p,sequence);
    }
    public static void key(Player p,Level level,ServerLevel sl,int key,boolean borrowed) {
        if (!(p instanceof ServerPlayer player) || (!borrowed&&!PATHWAY.equals(SoulCore.getPathway(p)))) return;
        ThunderGodEvents.initialize();
        if (key==7) {
            player.sendOverlayMessage(net.minecraft.network.chat.Component.literal("Storm abilities are not available yet."));
            return;
        }
        int sequence=switch(key){case 1->9;case 2->8;case 3->7;case 4->6;default->5;};
        if (!ready(p,sequence,borrowed)) return;
        if (Root.has(p,RootRestriction.ABILITIES)) return;
        if (key>=3) {
            AbilityWheel wheel=switch(key){case 3->TIDES;case 4->SKY;case 5->THUNDER;default->VOICE;};
            if(p.isShiftKeyDown()) wheel.open(p);else wheel.quickCast(p);
            return;
        }
        Spell spell=key==1?(p.isShiftKeyDown()?Spell.ILLUSORY_SCALES:Spell.HARPOON):(p.isShiftKeyDown()?Spell.WRATH:Spell.RAGING_BLOW);
        if (spell==Spell.ILLUSORY_SCALES && ConjuredArmor.isWorn(p,SCALES) || spell==Spell.WRATH && Form.isIn(p,WRATH)) {
            ThunderGodPowers.cast(player,sl,spell);return;
        }
        if(SoulCore.getSpirituality(p)<Spells.costFor(p,spell.cost)){Spells.paySpirituality(p,spell.cost);return;}
        if(ThunderGodPowers.cast(player,sl,spell)==AbilityWheel.Result.DONE) Spells.paySpirituality(p,spell.cost);
    }
}
