import { useQuotesStore, selectAnyStale } from '@/stores/quotesStore';
import { formatRelativeTime } from '@/lib/format';
import { cn } from '@/lib/cn';

/**
 * "Last updated <relative time>" with a stale badge when the provider is not responding (FR-005).
 * Reads the visibility-aware quote poll in {@link useQuotesStore}.
 */
export function LiveQuotesIndicator() {
  const lastUpdated = useQuotesStore((s) => s.lastUpdated);
  const stale = useQuotesStore(selectAnyStale);

  return (
    <div className="flex items-center gap-2 text-small text-text-muted">
      <span data-testid="quote-last-updated">Last updated {formatRelativeTime(lastUpdated)}</span>
      {stale ? (
        <span
          data-testid="quote-stale"
          className={cn(
            'inline-flex items-center rounded-full border border-warning/40 bg-warning/10',
            'px-2 py-0.5 text-small font-medium text-warning',
          )}
        >
          Stale
        </span>
      ) : null}
    </div>
  );
}
