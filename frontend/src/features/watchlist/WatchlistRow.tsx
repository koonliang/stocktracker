import { useNavigate } from 'react-router-dom';
import { ArrowDown, ArrowUp, GripVertical, X } from 'lucide-react';
import { formatCurrency, formatSignedCurrency, formatSignedPercent } from '@/lib/format';
import { cn } from '@/lib/cn';

type Props = {
  symbol: string;
  name: string;
  currentPrice: number | null;
  dayChange: number | null;
  dayChangePct: number | null;
  onRemove: () => void;
  onMoveUp?: () => void;
  onMoveDown?: () => void;
  canMoveUp?: boolean;
  canMoveDown?: boolean;
  backTo?: string;
  backLabel?: string;
  /** Props injected by the parent for HTML5 drag-and-drop reorder. */
  dragHandleProps?: {
    draggable?: boolean;
    onDragStart?: (e: React.DragEvent) => void;
    onDragOver?: (e: React.DragEvent) => void;
    onDragEnd?: (e: React.DragEvent) => void;
    onDrop?: (e: React.DragEvent) => void;
  };
};

export function WatchlistRow({
  symbol,
  name,
  currentPrice,
  dayChange,
  dayChangePct,
  onRemove,
  onMoveUp,
  onMoveDown,
  canMoveUp,
  canMoveDown,
  backTo,
  backLabel,
  dragHandleProps,
}: Props) {
  const navigate = useNavigate();
  const positive = (dayChange ?? 0) > 0;
  const negative = (dayChange ?? 0) < 0;

  function open() {
    navigate(`/analysis/${symbol}`, {
      state: {
        backTo: backTo ?? '/watchlists',
        backLabel: backLabel ?? 'Back to watchlists',
      },
    });
  }

  return (
    <li
      data-testid={`watchlist-item-${symbol}`}
      className="group flex items-center gap-3 border-b border-border px-3 py-3 transition-colors last:border-0 hover:bg-surface-alt/60"
      {...dragHandleProps}
    >
      <span
        aria-hidden
        className="flex h-8 w-6 shrink-0 cursor-grab items-center justify-center text-text-subtle"
      >
        <GripVertical size={14} />
      </span>

      <button
        type="button"
        onClick={open}
        aria-label={`Open analysis for ${symbol}`}
        className="flex min-w-0 flex-1 items-center gap-3 text-left focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-focus-ring"
      >
        <div className="flex min-w-0 flex-1 flex-col gap-0.5">
          <span className="font-mono text-body font-semibold leading-none text-text">{symbol}</span>
          <span className="truncate text-small text-text-muted">{name}</span>
        </div>
        <div className="flex shrink-0 flex-col items-end gap-0.5 pr-1 leading-tight sm:flex-row sm:items-center sm:gap-4">
          <div className="font-mono tabular text-body text-text">
            {formatCurrency(currentPrice)}
          </div>
          <div
            className={cn(
              'font-mono tabular text-small sm:min-w-[5.5rem] sm:text-right',
              positive && 'delta-positive',
              negative && 'delta-negative',
              !positive && !negative && 'text-text-muted',
            )}
          >
            <span className="sm:block">{formatSignedCurrency(dayChange)}</span>
            <span className="ml-1 opacity-80 sm:ml-0 sm:block">
              {formatSignedPercent(dayChangePct)}
            </span>
          </div>
        </div>
      </button>

      <div className="flex items-center gap-1">
        <button
          type="button"
          onClick={onMoveUp}
          disabled={!canMoveUp || !onMoveUp}
          aria-label={`Move ${symbol} up`}
          className="rounded p-1.5 text-text-muted hover:bg-surface-alt hover:text-text focus-visible:outline focus-visible:outline-2 focus-visible:outline-focus-ring disabled:cursor-not-allowed disabled:opacity-30"
        >
          <ArrowUp size={14} aria-hidden />
        </button>
        <button
          type="button"
          onClick={onMoveDown}
          disabled={!canMoveDown || !onMoveDown}
          aria-label={`Move ${symbol} down`}
          className="rounded p-1.5 text-text-muted hover:bg-surface-alt hover:text-text focus-visible:outline focus-visible:outline-2 focus-visible:outline-focus-ring disabled:cursor-not-allowed disabled:opacity-30"
        >
          <ArrowDown size={14} aria-hidden />
        </button>
        <button
          type="button"
          onClick={onRemove}
          data-testid="watchlist-remove"
          aria-label={`Remove ${symbol} from watchlist`}
          className="rounded p-1.5 text-text-muted hover:bg-negative/10 hover:text-negative focus-visible:outline focus-visible:outline-2 focus-visible:outline-focus-ring"
        >
          <X size={14} aria-hidden />
        </button>
      </div>
    </li>
  );
}
