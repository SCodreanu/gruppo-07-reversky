package it.downfall.reversky.engine.core.effect;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import it.downfall.reversky.engine.core.effect.impl.BakuEffect;
import it.downfall.reversky.engine.core.effect.impl.BellWitchEffect;
import it.downfall.reversky.engine.core.effect.impl.BigfootEffect;
import it.downfall.reversky.engine.core.effect.impl.BiteOfReverseEffect;
import it.downfall.reversky.engine.core.effect.impl.BloodlessHoundEffect;
import it.downfall.reversky.engine.core.effect.impl.BreakingPointEffect;
import it.downfall.reversky.engine.core.effect.impl.BuildingDirectorEffect;
import it.downfall.reversky.engine.core.effect.impl.CallOfTheRiftEffect;
import it.downfall.reversky.engine.core.effect.impl.CrossedDoorsKeeperEffect;
import it.downfall.reversky.engine.core.effect.impl.DeathmothsEffect;
import it.downfall.reversky.engine.core.effect.impl.DeniedExitBadgeEffect;
import it.downfall.reversky.engine.core.effect.impl.DevouringAbyssEffect;
import it.downfall.reversky.engine.core.effect.impl.DissolutionEffect;
import it.downfall.reversky.engine.core.effect.impl.DrownedMemoryEffect;
import it.downfall.reversky.engine.core.effect.impl.EchoRiftEffect;
import it.downfall.reversky.engine.core.effect.impl.EchoStrikeEffect;
import it.downfall.reversky.engine.core.effect.impl.ExitClerkEffect;
import it.downfall.reversky.engine.core.effect.impl.FaceCollectorEffect;
import it.downfall.reversky.engine.core.effect.impl.FadedTagEffect;
import it.downfall.reversky.engine.core.effect.impl.FalsePathsCompassEffect;
import it.downfall.reversky.engine.core.effect.impl.FootprintGathererEffect;
import it.downfall.reversky.engine.core.effect.impl.ForgottenEchoEffect;
import it.downfall.reversky.engine.core.effect.impl.GnawingWitnessEffect;
import it.downfall.reversky.engine.core.effect.impl.HidebehindEffect;
import it.downfall.reversky.engine.core.effect.impl.HollowClearingEffect;
import it.downfall.reversky.engine.core.effect.impl.HollowWyrmEffect;
import it.downfall.reversky.engine.core.effect.impl.HostilePresenceEffect;
import it.downfall.reversky.engine.core.effect.impl.HoundsEffect;
import it.downfall.reversky.engine.core.effect.impl.HowlersEffect;
import it.downfall.reversky.engine.core.effect.impl.IndexMawEffect;
import it.downfall.reversky.engine.core.effect.impl.JorogumoEffect;
import it.downfall.reversky.engine.core.effect.impl.KappaEffect;
import it.downfall.reversky.engine.core.effect.impl.KitsuneEffect;
import it.downfall.reversky.engine.core.effect.impl.LastSanctuaryEffect;
import it.downfall.reversky.engine.core.effect.impl.LivingWaitingRoomEffect;
import it.downfall.reversky.engine.core.effect.impl.MaskedBloomEffect;
import it.downfall.reversky.engine.core.effect.impl.MothmanEffect;
import it.downfall.reversky.engine.core.effect.impl.NameCollectorEffect;
import it.downfall.reversky.engine.core.effect.impl.NamelessCallerEffect;
import it.downfall.reversky.engine.core.effect.impl.NopperaBoEffect;
import it.downfall.reversky.engine.core.effect.impl.NullWitnessEffect;
import it.downfall.reversky.engine.core.effect.impl.NureOnnaEffect;
import it.downfall.reversky.engine.core.effect.impl.NurikabeEffect;
import it.downfall.reversky.engine.core.effect.impl.QueenOfLostPathsEffect;
import it.downfall.reversky.engine.core.effect.impl.RainArmorEffect;
import it.downfall.reversky.engine.core.effect.impl.RainPreacherEffect;
import it.downfall.reversky.engine.core.effect.impl.RegistryPactEffect;
import it.downfall.reversky.engine.core.effect.impl.RemnantsOfRealityEffect;
import it.downfall.reversky.engine.core.effect.impl.ReverseFusionEffect;
import it.downfall.reversky.engine.core.effect.impl.RokurokubiEffect;
import it.downfall.reversky.engine.core.effect.impl.RoomZeroOperatorEffect;
import it.downfall.reversky.engine.core.effect.impl.RootboundDoppelgangerEffect;
import it.downfall.reversky.engine.core.effect.impl.Scp001Effect;
import it.downfall.reversky.engine.core.effect.impl.Scp049Effect;
import it.downfall.reversky.engine.core.effect.impl.Scp053Effect;
import it.downfall.reversky.engine.core.effect.impl.Scp096Effect;
import it.downfall.reversky.engine.core.effect.impl.Scp106Effect;
import it.downfall.reversky.engine.core.effect.impl.Scp109Effect;
import it.downfall.reversky.engine.core.effect.impl.Scp173Effect;
import it.downfall.reversky.engine.core.effect.impl.Scp2006Effect;
import it.downfall.reversky.engine.core.effect.impl.Scp2317Effect;
import it.downfall.reversky.engine.core.effect.impl.Scp2521Effect;
import it.downfall.reversky.engine.core.effect.impl.Scp3000Effect;
import it.downfall.reversky.engine.core.effect.impl.Scp3999Effect;
import it.downfall.reversky.engine.core.effect.impl.Scp458Effect;
import it.downfall.reversky.engine.core.effect.impl.Scp500Effect;
import it.downfall.reversky.engine.core.effect.impl.Scp682Effect;
import it.downfall.reversky.engine.core.effect.impl.SealedFileZeroEffect;
import it.downfall.reversky.engine.core.effect.impl.ServiceDoubleEffect;
import it.downfall.reversky.engine.core.effect.impl.ShutenDojiEffect;
import it.downfall.reversky.engine.core.effect.impl.SilentShepherdEffect;
import it.downfall.reversky.engine.core.effect.impl.SkinFractureEffect;
import it.downfall.reversky.engine.core.effect.impl.SkinStealersEffect;
import it.downfall.reversky.engine.core.effect.impl.SmilerEffect;
import it.downfall.reversky.engine.core.effect.impl.SpearfingerEffect;
import it.downfall.reversky.engine.core.effect.impl.StaticInterferenceEffect;
import it.downfall.reversky.engine.core.effect.impl.StitchedCorridorEffect;
import it.downfall.reversky.engine.core.effect.impl.SurvivalInstinctEffect;
import it.downfall.reversky.engine.core.effect.impl.ThornedMatriarchEffect;
import it.downfall.reversky.engine.core.effect.impl.ThreadboundDollEffect;
import it.downfall.reversky.engine.core.effect.impl.ToriiBellEffect;
import it.downfall.reversky.engine.core.effect.impl.ToriiShadowEffect;
import it.downfall.reversky.engine.core.effect.impl.UndyingBlossomEffect;
import it.downfall.reversky.engine.core.effect.impl.UnseenExitEffect;
import it.downfall.reversky.engine.core.effect.impl.UnseenMatriarchEffect;
import it.downfall.reversky.engine.core.effect.impl.VeilOfAnchoredEffect;
import it.downfall.reversky.engine.core.effect.impl.WampusCatEffect;
import it.downfall.reversky.engine.core.effect.impl.WatchingRiftEffect;
import it.downfall.reversky.engine.core.effect.impl.WendigoEffect;
import it.downfall.reversky.engine.core.effect.impl.WhiteStagEffect;
import it.downfall.reversky.engine.core.effect.impl.WretchesEffect;
import it.downfall.reversky.engine.core.effect.impl.YukiOnnaEffect;
import it.downfall.reversky.engine.core.model.CardType;

/**
 * Collega ogni codice effetto ({@code effectCode} della carta) alla classe che lo implementa e al tipo di carta a cui
 * appartiene (docs/effetti, "Registro degli effetti").
 * <p>
 * Ogni codice vale per un solo tipo di carta: un codice di Terreno su una Spell è un errore dell'input
 * ({@code UNKNOWN_EFFECT}). Il registro crea una <b>nuova istanza</b> dell'effetto a ogni uso, perché un effetto può
 * avere uno stato (es. "una volta").
 * <p>
 * Gli effetti si registrano una volta all'avvio, poi il registro si legge soltanto: la stessa istanza serve tutte le
 * partite, anche in parallelo. L'ordine di registrazione è l'ordine di {@link #codes()}.
 */
public final class EffectRegistry {

	private final Map<String, Registration> registrations = new LinkedHashMap<>();

	/**
	 * Crea un registro vuoto. Per il gioco si usa {@link #withAllEffects()}; un registro vuoto serve ai test.
	 */
	public EffectRegistry() {
	}

	/**
	 * Registro con tutti gli effetti implementati: è quello del gioco.
	 *
	 * @return un nuovo registro
	 */
	public static EffectRegistry withAllEffects() {
		return new EffectRegistry()
				.registerCreature(HidebehindEffect.CODE, HidebehindEffect::new)
				.registerCreature(NurikabeEffect.CODE, NurikabeEffect::new)
				.registerCreature(KappaEffect.CODE, KappaEffect::new)
				.registerCreature(Scp096Effect.CODE, Scp096Effect::new)
				.registerCreature(Scp2317Effect.CODE, Scp2317Effect::new)
				.registerCreature(SmilerEffect.CODE, SmilerEffect::new)
				.registerCreature(WampusCatEffect.CODE, WampusCatEffect::new)
				.registerCreature(SilentShepherdEffect.CODE, SilentShepherdEffect::new)
				.registerCreature(WhiteStagEffect.CODE, WhiteStagEffect::new)
				.registerCreature(NamelessCallerEffect.CODE, NamelessCallerEffect::new)
				.registerCreature(FootprintGathererEffect.CODE, FootprintGathererEffect::new)
				.registerCreature(SpearfingerEffect.CODE, SpearfingerEffect::new)
				.registerCreature(BellWitchEffect.CODE, BellWitchEffect::new)
				.registerCreature(MothmanEffect.CODE, MothmanEffect::new)
				.registerCreature(BigfootEffect.CODE, BigfootEffect::new)
				.registerCreature(ThornedMatriarchEffect.CODE, ThornedMatriarchEffect::new)
				.registerCreature(Scp173Effect.CODE, Scp173Effect::new)
				.registerCreature(Scp049Effect.CODE, Scp049Effect::new)
				.registerCreature(NameCollectorEffect.CODE, NameCollectorEffect::new)
				.registerCreature(Scp053Effect.CODE, Scp053Effect::new)
				.registerCreature(Scp2006Effect.CODE, Scp2006Effect::new)
				.registerCreature(IndexMawEffect.CODE, IndexMawEffect::new)
				.registerCreature(Scp106Effect.CODE, Scp106Effect::new)
				.registerCreature(Scp682Effect.CODE, Scp682Effect::new)
				.registerCreature(DrownedMemoryEffect.CODE, DrownedMemoryEffect::new)
				.registerCreature(Scp3000Effect.CODE, Scp3000Effect::new)
				.registerCreature(Scp001Effect.CODE, Scp001Effect::new)
				.registerCreature(HowlersEffect.CODE, HowlersEffect::new)
				.registerCreature(WretchesEffect.CODE, WretchesEffect::new)
				.registerCreature(FadedTagEffect.CODE, FadedTagEffect::new)
				.registerCreature(LivingWaitingRoomEffect.CODE, LivingWaitingRoomEffect::new)
				.registerCreature(GnawingWitnessEffect.CODE, GnawingWitnessEffect::new)
				.registerCreature(SkinStealersEffect.CODE, SkinStealersEffect::new)
				.registerCreature(ServiceDoubleEffect.CODE, ServiceDoubleEffect::new)
				.registerCreature(BuildingDirectorEffect.CODE, BuildingDirectorEffect::new)
				.registerCreature(HoundsEffect.CODE, HoundsEffect::new)
				.registerCreature(NopperaBoEffect.CODE, NopperaBoEffect::new)
				.registerCreature(RokurokubiEffect.CODE, RokurokubiEffect::new)
				.registerCreature(ThreadboundDollEffect.CODE, ThreadboundDollEffect::new)
				.registerCreature(ToriiShadowEffect.CODE, ToriiShadowEffect::new)
				.registerCreature(MaskedBloomEffect.CODE, MaskedBloomEffect::new)
				.registerCreature(YukiOnnaEffect.CODE, YukiOnnaEffect::new)
				.registerCreature(JorogumoEffect.CODE, JorogumoEffect::new)
				.registerCreature(NureOnnaEffect.CODE, NureOnnaEffect::new)
				.registerCreature(FaceCollectorEffect.CODE, FaceCollectorEffect::new)
				.registerCreature(UnseenMatriarchEffect.CODE, UnseenMatriarchEffect::new)
				.registerCreature(ShutenDojiEffect.CODE, ShutenDojiEffect::new)
				.registerCreature(RootboundDoppelgangerEffect.CODE, RootboundDoppelgangerEffect::new)
				.registerCreature(StitchedCorridorEffect.CODE, StitchedCorridorEffect::new)
				.registerCreature(BloodlessHoundEffect.CODE, BloodlessHoundEffect::new)
				.registerCreature(WendigoEffect.CODE, WendigoEffect::new)
				.registerCreature(ExitClerkEffect.CODE, ExitClerkEffect::new)
				.registerCreature(DeathmothsEffect.CODE, DeathmothsEffect::new)
				.registerCreature(Scp3999Effect.CODE, Scp3999Effect::new)
				.registerCreature(NullWitnessEffect.CODE, NullWitnessEffect::new)
				.registerCreature(HollowWyrmEffect.CODE, HollowWyrmEffect::new)
				.registerCreature(UndyingBlossomEffect.CODE, UndyingBlossomEffect::new)
				.registerCreature(RainPreacherEffect.CODE, RainPreacherEffect::new)
				.registerCreature(Scp2521Effect.CODE, Scp2521Effect::new)
				.registerCreature(BakuEffect.CODE, BakuEffect::new)
				.registerCreature(KitsuneEffect.CODE, KitsuneEffect::new)
				.registerCreature(CrossedDoorsKeeperEffect.CODE, CrossedDoorsKeeperEffect::new)
				.registerCreature(QueenOfLostPathsEffect.CODE, QueenOfLostPathsEffect::new)
				.registerCreature(RoomZeroOperatorEffect.CODE, RoomZeroOperatorEffect::new)
				.registerSpell(BiteOfReverseEffect.CODE, BiteOfReverseEffect::new)
				.registerSpell(SkinFractureEffect.CODE, SkinFractureEffect::new)
				.registerSpell(DissolutionEffect.CODE, DissolutionEffect::new)
				.registerSpell(VeilOfAnchoredEffect.CODE, VeilOfAnchoredEffect::new)
				.registerSpell(RegistryPactEffect.CODE, RegistryPactEffect::new)
				.registerSpell(RainArmorEffect.CODE, RainArmorEffect::new)
				.registerSpell(Scp458Effect.CODE, Scp458Effect::new)
				.registerSpell(Scp109Effect.CODE, Scp109Effect::new)
				.registerSpell(Scp500Effect.CODE, Scp500Effect::new)
				.registerSpell(CallOfTheRiftEffect.CODE, CallOfTheRiftEffect::new)
				.registerSpell(ReverseFusionEffect.CODE, ReverseFusionEffect::new)
				.registerSpell(ForgottenEchoEffect.CODE, ForgottenEchoEffect::new)
				.registerHidden(LastSanctuaryEffect.CODE, LastSanctuaryEffect::new)
				.registerHidden(SurvivalInstinctEffect.CODE, SurvivalInstinctEffect::new)
				.registerHidden(HostilePresenceEffect.CODE, HostilePresenceEffect::new)
				.registerHidden(EchoStrikeEffect.CODE, EchoStrikeEffect::new)
				.registerHidden(BreakingPointEffect.CODE, BreakingPointEffect::new)
				.registerHidden(StaticInterferenceEffect.CODE, StaticInterferenceEffect::new)
				.registerHidden(RemnantsOfRealityEffect.CODE, RemnantsOfRealityEffect::new)
				.registerHidden(UnseenExitEffect.CODE, UnseenExitEffect::new)
				.registerTerrain(EchoRiftEffect.CODE, EchoRiftEffect::new)
				.registerTerrain(HollowClearingEffect.CODE, HollowClearingEffect::new)
				.registerTerrain(WatchingRiftEffect.CODE, WatchingRiftEffect::new)
				.registerTerrain(DevouringAbyssEffect.CODE, DevouringAbyssEffect::new)
				.registerItem(DeniedExitBadgeEffect.CODE, DeniedExitBadgeEffect::new)
				.registerItem(FalsePathsCompassEffect.CODE, FalsePathsCompassEffect::new)
				.registerItem(SealedFileZeroEffect.CODE, SealedFileZeroEffect::new)
				.registerItem(ToriiBellEffect.CODE, ToriiBellEffect::new);
	}

	/**
	 * Registra l'effetto di una creatura.
	 *
	 * @param code    codice dell'effetto
	 * @param factory crea una nuova istanza dell'effetto
	 * @return questo registro
	 * @throws IllegalArgumentException se il codice è vuoto o è già registrato
	 */
	public EffectRegistry registerCreature(String code, Supplier<CreatureEffect> factory) {
		return register(code, CardType.CREATURE, factory);
	}

	/**
	 * Registra l'effetto di una Spell.
	 *
	 * @param code    codice dell'effetto
	 * @param factory crea una nuova istanza dell'effetto
	 * @return questo registro
	 * @throws IllegalArgumentException se il codice è vuoto o è già registrato
	 */
	public EffectRegistry registerSpell(String code, Supplier<SpellEffect> factory) {
		return register(code, CardType.SPELL, factory);
	}

	/**
	 * Registra l'effetto di una Hidden.
	 *
	 * @param code    codice dell'effetto
	 * @param factory crea una nuova istanza dell'effetto
	 * @return questo registro
	 * @throws IllegalArgumentException se il codice è vuoto o è già registrato
	 */
	public EffectRegistry registerHidden(String code, Supplier<HiddenEffect> factory) {
		return register(code, CardType.HIDDEN, factory);
	}

	/**
	 * Registra l'effetto di un Terreno.
	 *
	 * @param code    codice dell'effetto
	 * @param factory crea una nuova istanza dell'effetto
	 * @return questo registro
	 * @throws IllegalArgumentException se il codice è vuoto o è già registrato
	 */
	public EffectRegistry registerTerrain(String code, Supplier<TerrainEffect> factory) {
		return register(code, CardType.TERRAIN, factory);
	}

	/**
	 * Registra l'effetto di un Oggetto.
	 *
	 * @param code    codice dell'effetto
	 * @param factory crea una nuova istanza dell'effetto
	 * @return questo registro
	 * @throws IllegalArgumentException se il codice è vuoto o è già registrato
	 */
	public EffectRegistry registerItem(String code, Supplier<ItemEffect> factory) {
		return register(code, CardType.ITEM, factory);
	}

	/**
	 * Tipo di carta a cui appartiene un codice.
	 *
	 * @param code codice dell'effetto
	 * @return il tipo di carta, o vuoto se il codice non è registrato
	 */
	public Optional<CardType> appliesTo(String code) {
		return Optional.ofNullable(registrations.get(code)).map(Registration::appliesTo);
	}

	/**
	 * Indica se un codice è registrato per un tipo di carta.
	 *
	 * @param code     codice dell'effetto
	 * @param cardType tipo della carta che lo porta
	 * @return {@code true} se il codice esiste ed è di quel tipo
	 */
	public boolean exists(String code, CardType cardType) {
		return appliesTo(code).filter(type -> type == cardType).isPresent();
	}

	/**
	 * Tutti i codici registrati, con il tipo di carta, nell'ordine di registrazione (per {@code GET /api/v1/effects}).
	 *
	 * @return mappa non modificabile codice → tipo di carta
	 */
	public Map<String, CardType> codes() {
		Map<String, CardType> codes = new LinkedHashMap<>();
		registrations.forEach((code, registration) -> codes.put(code, registration.appliesTo()));
		return Collections.unmodifiableMap(codes);
	}

	/**
	 * Crea l'effetto di una creatura che entra in campo: una nuova istanza per ogni entrata (D-31).
	 *
	 * @param code codice dell'effetto
	 * @return una nuova istanza
	 * @throws IllegalArgumentException se il codice non è registrato per una creatura
	 */
	public CreatureEffect createCreature(String code) {
		return create(code, CardType.CREATURE, CreatureEffect.class);
	}

	/**
	 * Crea l'effetto di una Spell.
	 *
	 * @param code codice dell'effetto
	 * @return una nuova istanza
	 * @throws IllegalArgumentException se il codice non è registrato per una Spell
	 */
	public SpellEffect createSpell(String code) {
		return create(code, CardType.SPELL, SpellEffect.class);
	}

	/**
	 * Crea l'effetto di una Hidden.
	 *
	 * @param code codice dell'effetto
	 * @return una nuova istanza
	 * @throws IllegalArgumentException se il codice non è registrato per una Hidden
	 */
	public HiddenEffect createHidden(String code) {
		return create(code, CardType.HIDDEN, HiddenEffect.class);
	}

	/**
	 * Crea l'effetto di un Terreno.
	 *
	 * @param code codice dell'effetto
	 * @return una nuova istanza
	 * @throws IllegalArgumentException se il codice non è registrato per un Terreno
	 */
	public TerrainEffect createTerrain(String code) {
		return create(code, CardType.TERRAIN, TerrainEffect.class);
	}

	/**
	 * Crea l'effetto di un Oggetto.
	 *
	 * @param code codice dell'effetto
	 * @return una nuova istanza
	 * @throws IllegalArgumentException se il codice non è registrato per un Oggetto
	 */
	public ItemEffect createItem(String code) {
		return create(code, CardType.ITEM, ItemEffect.class);
	}

	private EffectRegistry register(String code, CardType appliesTo, Supplier<? extends CardEffect> factory) {
		if (code == null || code.isBlank()) {
			throw new IllegalArgumentException("Codice effetto vuoto");
		}
		if (registrations.containsKey(code)) {
			throw new IllegalArgumentException("Codice effetto già registrato: " + code);
		}
		registrations.put(code, new Registration(appliesTo, factory));
		return this;
	}

	private <E extends CardEffect> E create(String code, CardType cardType, Class<E> kind) {
		if (!exists(code, cardType)) {
			// La validazione dell'input lo esclude: arrivare qui è un bug del motore
			throw new IllegalArgumentException("Nessun effetto " + cardType + " con codice " + code);
		}
		E effect = kind.cast(registrations.get(code).factory().get());
		if (!code.equals(effect.code())) {
			throw new IllegalStateException("L'effetto registrato come " + code + " ha codice " + effect.code());
		}
		return effect;
	}

	/** Un codice registrato: a quale tipo di carta appartiene e come si crea l'effetto. */
	private record Registration(CardType appliesTo, Supplier<? extends CardEffect> factory) {
	}
}
