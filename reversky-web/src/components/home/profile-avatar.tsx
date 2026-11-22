import Image from "next/image";
import { PROFILE_AVATARS } from "@/lib/profile-avatar";
import styles from "./profile-avatar.module.css";

export function ProfileAvatar({
  index,
  className,
}: {
  index: number;
  className: string;
}) {
  const source = PROFILE_AVATARS[index] ?? PROFILE_AVATARS[0];

  return (
    <span className={`${styles.avatar} ${className}`} aria-hidden="true">
      <Image className={styles.image} src={source} alt="" width={256} height={256} />
    </span>
  );
}
