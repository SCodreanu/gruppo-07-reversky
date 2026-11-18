package it.downfall.reversky.api.friendship.entity;

/**
 * Stato di una relazione in {@code friendships} (docs/amicizie, "Stati").
 * <p>
 * Nel database è l'enum Postgres {@code friendship_status}.
 */
public enum FriendshipStatus {
	PENDING,
	ACCEPTED
}