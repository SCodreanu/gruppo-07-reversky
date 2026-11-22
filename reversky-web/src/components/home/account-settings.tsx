"use client";

import Link from "next/link";
import { ProfileAvatar } from "@/components/home/profile-avatar";
import { usePortalProfile } from "@/components/home/portal-page-frame";
import { PROFILE_AVATARS } from "@/lib/profile-avatar";
import styles from "./account-settings.module.css";

export function AccountSettings() {
  const { user, avatarIndex, chooseAvatar } = usePortalProfile();

  return (
    <>
      <section className={styles.profileCard} aria-labelledby="profile-title">
        <div className={styles.profileIntro}>
          <ProfileAvatar index={avatarIndex} className={styles.profileAvatar} />
          <div>
            <p className="eyebrow">Your profile</p>
            <h2 id="profile-title" className="display-type">{user.username}</h2>
          </div>
        </div>
        <dl className={styles.details}>
          <div><dt>Player name</dt><dd>{user.username}</dd></div>
          <div><dt>Email address</dt><dd>{user.email}</dd></div>
          <div>
            <dt>Email status</dt>
            <dd className={user.emailVerified ? styles.verified : styles.unverified}>
              {user.emailVerified ? "Verified" : "Not verified"}
            </dd>
          </div>
        </dl>
        {!user.emailVerified && (
          <p className={styles.verificationNote}>
            You can resend the verification email from <Link href="/home#account">your home page</Link>.
          </p>
        )}
      </section>

      <section className={styles.avatarCard} aria-labelledby="avatar-title">
        <div className={styles.cardHeading}>
          <div>
            <p className="eyebrow">Personalize your space</p>
            <h2 id="avatar-title" className="display-type">Choose your avatar</h2>
          </div>
          <span className={styles.selectionCount}>{PROFILE_AVATARS.length} portraits</span>
        </div>
        <p className={styles.cardIntro}>Your avatar is saved in this browser for now. Account-wide syncing can be added with backend support.</p>
        <div className={styles.avatarGrid} role="group" aria-label="Choose a profile avatar">
          {PROFILE_AVATARS.map((_, index) => (
            <button
              className={`${styles.avatarChoice} ${avatarIndex === index ? styles.avatarSelected : ""}`}
              type="button"
              aria-label={`Choose avatar ${index + 1}`}
              aria-pressed={avatarIndex === index}
              onClick={() => chooseAvatar(index)}
              key={index}
            >
              <ProfileAvatar index={index} className={styles.choiceAvatar} />
              <span>{String(index + 1).padStart(2, "0")}</span>
            </button>
          ))}
        </div>
      </section>

      <section className={styles.securityCard} aria-labelledby="security-title">
        <div>
          <p className="eyebrow">Account security</p>
          <h2 id="security-title" className="display-type">Keep your account safe</h2>
          <p>Use a password reset link to choose a new password for this account.</p>
        </div>
        <Link className="button button--quiet" href="/forgot-password">Reset password</Link>
      </section>
    </>
  );
}
