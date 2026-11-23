export const PROFILE_AVATARS = Array.from(
  { length: 8 },
  (_, index) => `/images/avatars/avatar-${index + 1}.png`,
);

function storageKey(userId: number): string {
  return `reversky:profile-avatar:${userId}`;
}

function isValidAvatarIndex(value: number): boolean {
  return Number.isInteger(value) && value >= 0 && value < PROFILE_AVATARS.length;
}

export function getOrCreateProfileAvatar(userId: number): number {
  if (typeof window === "undefined") return 0;

  try {
    const storedValue = window.localStorage.getItem(storageKey(userId));
    if (storedValue !== null) {
      const stored = Number(storedValue);
      if (isValidAvatarIndex(stored)) return stored;
    }

    const selected = Math.floor(Math.random() * PROFILE_AVATARS.length);
    window.localStorage.setItem(storageKey(userId), String(selected));
    return selected;
  } catch {
    return Math.floor(Math.random() * PROFILE_AVATARS.length);
  }
}

export function saveProfileAvatar(userId: number, avatarIndex: number): number {
  const selected = isValidAvatarIndex(avatarIndex) ? avatarIndex : 0;
  if (typeof window === "undefined") return selected;

  try {
    window.localStorage.setItem(storageKey(userId), String(selected));
  } catch {
    // Keep the selection for this render if browser storage is unavailable.
  }
  return selected;
}

/**
 * Avatar of any player as seen in this browser: the one they chose here if any, otherwise a stable one from the id
 * (the avatar is not stored by the API yet, so the opponent's choice is not known).
 */
export function playerAvatar(userId: number): number {
  if (typeof window !== "undefined") {
    try {
      const stored = Number(window.localStorage.getItem(storageKey(userId)));
      if (window.localStorage.getItem(storageKey(userId)) !== null && isValidAvatarIndex(stored)) return stored;
    } catch {
      // Fall back to the stable avatar below.
    }
  }
  return Math.abs(userId) % PROFILE_AVATARS.length;
}
