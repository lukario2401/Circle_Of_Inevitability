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

/** Complete Thunder God pathway: Sailor (9) through Tyrant (0), with full-price wheel recasts. */
public final class ThunderGod {
    public static final String PATHWAY = "Thunder God", PREFIX = "thunder_god_";
    public static final String SCALES = PREFIX + "scales", WRATH = PREFIX + "wrath", SOURCE = PREFIX + "source";
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
        THUNDERCLAP(5,3500,280,"Thunder awes enemies within ten blocks: damage, brief stiffness and fear."),
        SEA_MASTER(3,12000,900,"A calm lake slows hostile projectiles, reflects recent casts and summons three sea creatures."),
        MAELSTROM(3,12000,700,"A whirlpool drags enemies and flyers inward; teleporting and substitutes cannot free its victims."),
        DESTRUCTIVE_DROPLET(3,14000,600,"Charge a water drop, then burst it into a silent destructive flash; survivors are Parched."),
        WINDBLADE_LEGION(4,6000,240,"Twelve waves of converging blades strike a five-block sphere and wear armor."),
        SONIC_BOOM(3,12000,200,"A distant sonic explosion throws enemies away and interrupts casting."),
        CLOUD_SWORD(3,12000,600,"Guard an aimed ally with a cloud sword, or drop it on enemies along a line."),
        INCARNATION_OF_LIGHT(1,30000,900,"Become untouchable light, pass through enemies, then erupt; sneak to leave early."),
        STARFALL(0,50000,1800,"Warn the target for 1.5 seconds, then descend with a massive electrified impact."),
        CHAIN_LIGHTNING(4,6000,200,"Three bolts chain between enemies; Soaked victims conduct extra branches."),
        LIGHTNING_STORM(3,12000,600,"Eight seconds of silver bolts; allies in the storm take half damage."),
        MAGNETIC_POLE_PULL(2,20000,500,"Gather enemies and metal projectiles, whirl them around, then throw them outward."),
        ELECTROMAGNETIC_DISRUPTION(2,18000,900,"An invisible silent field interrupts information powers and disrupts aimed casts."),
        HEAVENLY_PUNISHMENT(1,30000,800,"A dark pillar warns of an unavoidable true lightning strike that tears through hidden forms."),
        OCEAN_OF_LIGHTNING(1,40000,1200,"A broad electric sea destroys enemy projectiles and repeatedly shocks Soaked enemies."),
        LIGHTNING_FOREST(0,60000,1800,"A forest of lightning tears enemy barriers and veils; electrified ground persists afterward."),
        ROAR(4,6000,240,"A ship-toppling cone of sound breaks guards and throws riders off mounts."),
        DEATH_DIRGE(4,7000,600,"A moving dirge slows and harms enemies and locks their Sealed Artifacts."),
        SOUND_OF_HORROR(2,20000,900,"Extreme fear seizes enemies in a wide area and locks their Sealed Artifacts."),
        REQUIEM(2,18000,600,"A spectral song silences one enemy, disrupts its attacks and builds Madness."),
        ROAR_OF_THE_THUNDER_GOD(1,30000,800,"A devastating instant roar frightens, stuns and purifies enemies around you."),
        TYRANTS_PRESENCE(0,45000,1200,"Terror stiffens enemies, strips invisibility, repels possession and infuses your storms with fear."),
        HURRICANE(4,7000,500,"A moving hurricane throws enemies and projectiles out and blocks outside targeting."),
        TSUNAMI(4,8000,600,"A towering wave crosses the battlefield; Sea Kings leave a lingering flood."),
        WRATH_OF_THE_SEA(3,12000,800,"The sea rejects a marked enemy; nearby water and weather repeatedly strike it."),
        STORM_INCARNATION(2,22000,1200,"Steer a vast angry cloud with your gaze; it rains, roars and calls down lightning."),
        STORM_HELL(1,40000,2400,"A local calamity grounds flyers, blinds enemies, extinguishes projectiles and rains lightning."),
        CHASM_OF_STORMS(0,50000,2400,"A sealed storm kingdom fills Rage and halves the cost and cooldown of Sky, Thunder and Storm."),
        DREADFUL_HEAVEN(0,50000,2400,"A sealed kingdom enforces fear, doubles enemy ability costs and makes your Voice casts free."),
        BANISH_INTO_THE_STORM(0,25000,900,"Hide an enemy in a storm until it returns hurt, Soaked and afraid, or briefly shelter an ally."),
        SOURCE_OF_WRATH(2,25000,0,"Spend ten Rage for one stronger casting Sequence; lose restraint, then become Spent.");
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
            Spell.WATER_CURTAIN, Spell.WATER_SPHERE, Spell.CORROSIVE_RAIN, Spell.SEA_MASTER, Spell.MAELSTROM, Spell.DESTRUCTIVE_DROPLET);
    public static final AbilityWheel SKY = wheel("sky", "Sky", Spell.WINDBLADE, Spell.GALE_BURST, Spell.GALE_RIDE, Spell.WIND_BINDING, Spell.WINDBLADE_LEGION, Spell.SONIC_BOOM, Spell.CLOUD_SWORD, Spell.INCARNATION_OF_LIGHT, Spell.STARFALL);
    public static final AbilityWheel THUNDER = wheel("thunder", "Thunder", Spell.LIGHTNING_STRIKE, Spell.LIGHTNING_BOLT_ARROW, Spell.CHAIN_LIGHTNING, Spell.LIGHTNING_STORM, Spell.MAGNETIC_POLE_PULL, Spell.ELECTROMAGNETIC_DISRUPTION, Spell.HEAVENLY_PUNISHMENT, Spell.OCEAN_OF_LIGHTNING, Spell.LIGHTNING_FOREST);
    public static final AbilityWheel VOICE = wheel("voice", "Voice", Spell.SIREN_SONG, Spell.DISCORD, Spell.THUNDERCLAP, Spell.ROAR, Spell.DEATH_DIRGE, Spell.SOUND_OF_HORROR, Spell.REQUIEM, Spell.ROAR_OF_THE_THUNDER_GOD, Spell.TYRANTS_PRESENCE);
    public static final AbilityWheel STORM = wheel("storm", "Storm", Spell.HURRICANE, Spell.TSUNAMI, Spell.WRATH_OF_THE_SEA, Spell.STORM_INCARNATION, Spell.STORM_HELL, Spell.CHASM_OF_STORMS, Spell.DREADFUL_HEAVEN, Spell.BANISH_INTO_THE_STORM);
    public static final MeleeCombo COMBO = combo();
    private ThunderGod() {}
    private static MeleeCombo combo() {
        MeleeCombo combo = MeleeCombo.forPathway(PATHWAY).resetAfter(40);
        for (int i=0;i<8;i++) { int step=i; combo.step((p,t,sl)->ThunderGodPowers.melee(p,t,sl,step)); }
        return combo;
    }
    private static AbilityWheel wheel(String id, String title, Spell... spells) {
        AbilityWheel wheel=AbilityWheel.create(PREFIX+id,title).pathway(PATHWAY).quickCastCooldown(true).quickCastCost(1).sequenceBy(ThunderGod::sequence)
                .cooldownBy((p,ticks)->ThunderGodHigher.inChasm(p)&&!id.equals("voice")&&!id.equals("tides")?ticks/2:ticks);
        for (Spell spell:spells) wheel.entry(spell.id(),"ability_menus/thunder_god/"+spell.id(),spell.label(),spell.sequence,spell.cost,spell.cooldown,
                (p,sl)->ready(p,spell.sequence,Casts.isCopying(p)||Beyonder.isCastingUnbound(p))
                        ? ThunderGodPowers.cast(p,sl,spell) : AbilityWheel.Result.REFUSED).description(spell.description)
                .costBy((p,cost)->ThunderGodHigher.inDreadful(p)&&id.equals("voice")?0:ThunderGodHigher.inChasm(p)&&!id.equals("voice")&&!id.equals("tides")?cost/2:cost)
                .free(p->ThunderGodHigher.dismissable(p,spell));
        return wheel;
    }
    public static int sequence(LivingEntity e) { return net.lukario.circleofinevitability.status.SequenceBoost.effective(e); }
    public static boolean canUse(Player p,int seq) { return Beyonder.hasSequence(p,seq)||sequence(p)<=seq; }
    public static boolean targets(LivingEntity caster,LivingEntity target) {
        if(ThunderGodHigher.source(caster))return target!=caster&&target.isAlive()&&!target.isSpectator()
                &&!(target instanceof net.minecraft.world.entity.decoration.ArmorStand)&&!ThunderGodHigher.separated(caster,target)&&!net.lukario.circleofinevitability.status.Concealment.isHidden(target);
        return Spells.isEnemy(caster,target);
    }
    public static java.util.List<LivingEntity> around(LivingEntity caster,ServerLevel level,net.minecraft.world.phys.Vec3 at,double radius) {
        if(!ThunderGodHigher.source(caster))return Spells.enemiesAround(caster,level,at,radius);
        return Spells.livingAround(caster,level,at,radius).stream().filter(e->!ThunderGodHigher.separated(caster,e)).toList();
    }
    public static net.minecraft.world.phys.Vec3 look(Player p) {
        return ThunderGodHigher.source(p)?p.getLookAngle().yRot((float)Math.toRadians((p.getRandom().nextDouble()-.5)*10)):p.getLookAngle();
    }
    public static LivingEntity aimEnemy(Player p,ServerLevel level,double range) {
        var from=p.getEyePosition();var far=from.add(look(p).scale(range));
        var end=level.clip(new net.minecraft.world.level.ClipContext(from,far,net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,p)).getLocation();
        return Spells.firstEnemyOnLine(p,level,from,end,.3);
    }
    public static boolean is(LivingEntity e,int sequence) {
        return e instanceof Player p && PATHWAY.equals(SoulCore.getPathway(p)) && SoulCore.getSequence(p)<=sequence;
    }
    private static boolean ready(Player p,int sequence,boolean borrowed) {
        return !p.level().isClientSide() && p.isAlive() && (borrowed||PATHWAY.equals(SoulCore.getPathway(p))) && canUse(p,sequence);
    }
    public static void key(Player p,Level level,ServerLevel sl,int key,boolean borrowed) {
        if (!(p instanceof ServerPlayer player) || (!borrowed&&!PATHWAY.equals(SoulCore.getPathway(p)))) return;
        ThunderGodEvents.initialize();
        int sequence=switch(key){case 1->9;case 2->8;case 3->7;case 4->6;case 7->4;default->5;};
        if (!ready(p,sequence,borrowed)) return;
        if (Root.has(p,RootRestriction.ABILITIES)) return;
        if (key>=3) {
            AbilityWheel wheel=switch(key){case 3->TIDES;case 4->SKY;case 5->THUNDER;case 6->VOICE;default->STORM;};
            if(p.isShiftKeyDown()) wheel.open(p);else wheel.quickCast(p);
            return;
        }
        Spell spell=key==1?(p.isShiftKeyDown()?Spell.ILLUSORY_SCALES:Spell.HARPOON):(p.isShiftKeyDown()?Spell.WRATH:Spell.RAGING_BLOW);
        if(key==2&&p.isShiftKeyDown()&&(ThunderGodHigher.source(p)||is(p,2)&&net.lukario.circleofinevitability.status.Rage.count(p)==10))spell=Spell.SOURCE_OF_WRATH;
        if(spell==Spell.SOURCE_OF_WRATH&&ThunderGodHigher.source(p)){ThunderGodPowers.cast(player,sl,spell);return;}
        if (spell==Spell.ILLUSORY_SCALES && ConjuredArmor.isWorn(p,SCALES) || spell==Spell.WRATH && Form.isIn(p,WRATH)) {
            ThunderGodPowers.cast(player,sl,spell);return;
        }
        if(SoulCore.getSpirituality(p)<Spells.costFor(p,spell.cost)){Spells.paySpirituality(p,spell.cost);return;}
        if(ThunderGodPowers.cast(player,sl,spell)==AbilityWheel.Result.DONE) Spells.paySpirituality(p,spell.cost);
    }
}
