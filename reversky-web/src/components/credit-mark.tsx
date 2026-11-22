import Image from "next/image";

export function CreditMark({ size = 16, className }: { size?: number; className?: string }) {
  return (
    <Image
      alt=""
      aria-hidden="true"
      className={className}
      height={size}
      src="/images/credit-coin.png"
      width={size}
    />
  );
}
