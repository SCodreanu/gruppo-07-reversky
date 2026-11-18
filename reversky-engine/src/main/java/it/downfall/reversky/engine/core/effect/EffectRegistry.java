package it.downfall.reversky.engine.core.effect;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

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
		// Vuoto finché non arrivano gli effetti: Spell con ENG-19, Hidden con ENG-20, Terreni con ENG-21,
		// Oggetti con ENG-33
		return new EffectRegistry();
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
