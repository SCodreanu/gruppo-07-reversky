import Image from "next/image";
import Link from "next/link";

export function Brand({ compact = false, href = "/" }: { compact?: boolean; href?: string }) {
  return (
    <Link className={`brand${compact ? " brand--compact" : ""}`} href={href} aria-label="Reversky home">
      <Image
        className="brand__logo"
        src="/images/reversky-logo-white.png"
        alt=""
        width={1400}
        height={990}
        unoptimized
      />
    </Link>
  );
}
