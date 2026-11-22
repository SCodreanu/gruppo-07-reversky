package it.downfall.reversky.api.user.entity;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Utente registrato. La password è salvata solo come hash BCrypt.
 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "role_id", nullable = false)
	private Role role;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

	@Column(name = "is_enabled", nullable = false)
	private boolean enabled;

    @Column(name = "email_verified_at", nullable = true)
    private Instant emailVerifiedAt;

    @Column(name = "privacy_accepted_at", nullable = false)
    private Instant privacyAcceptedAt;

    @Column(name = "terms_accepted_at", nullable = false)
    private Instant termsAcceptedAt;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	/**
	 * Crea un nuovo utente abilitato, con email non ancora verificata.
	 *
	 * @param acceptedAt momento in cui ha accettato privacy e termini
	 */
	public User(Role role, String username, String email, String passwordHash, Instant acceptedAt) {
        this.role = role;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.privacyAcceptedAt = acceptedAt;
        this.termsAcceptedAt = acceptedAt;
        this.enabled = true;
	}

	public void changePassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
    }

	public void markEmailVerified(Instant verifiedAt) {
        this.emailVerifiedAt = verifiedAt;
    }

	public boolean isEmailVerified() {
        return this.emailVerifiedAt != null;
    }
}
