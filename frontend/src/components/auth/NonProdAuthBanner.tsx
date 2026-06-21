export function NonProdAuthBanner() {
  return (
    <span
      data-testid="nonprod-auth-banner"
      className="inline-flex items-center rounded-full border border-accent/20 bg-accent/10 px-3 py-1 font-mono text-micro uppercase tracking-[0.16em] text-accent"
    >
      Dev Preview
    </span>
  );
}
