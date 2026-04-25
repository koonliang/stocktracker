import { useEffect, useRef, useState } from 'react';
import { Check, Pencil, Trash2, X } from 'lucide-react';
import type { Watchlist } from '@/lib/types';
import { Button } from '@/components/ui/Button';
import { Dialog } from '@/components/ui/Dialog';
import { Input } from '@/components/ui/Input';
import { Label } from '@/components/ui/Label';
import { useWatchlistStore } from '@/stores/watchlistStore';

type Props = {
  watchlist: Watchlist;
  onDeleted: () => void;
};

export function WatchlistHeader({ watchlist, onDeleted }: Props) {
  const rename = useWatchlistStore((s) => s.rename);
  const remove = useWatchlistStore((s) => s.remove);

  const [editing, setEditing] = useState(false);
  const [draft, setDraft] = useState(watchlist.name);
  const [error, setError] = useState<string | null>(null);
  const [confirmOpen, setConfirmOpen] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (editing) inputRef.current?.select();
  }, [editing]);

  useEffect(() => {
    setDraft(watchlist.name);
  }, [watchlist.name]);

  function save() {
    const res = rename(watchlist.id, draft);
    if (!res.ok) {
      const message =
        res.reason === 'duplicate-name'
          ? 'A watchlist with this name already exists'
          : res.reason === 'too-long'
            ? 'Max 40 characters'
            : 'Name is required';
      setError(message);
      return;
    }
    setError(null);
    setEditing(false);
  }

  function cancel() {
    setDraft(watchlist.name);
    setError(null);
    setEditing(false);
  }

  return (
    <>
      <div className="mb-8 flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
        <div className="min-w-0">
          <div className="eyebrow">Watchlist</div>
          {editing ? (
            <div className="mt-1 flex flex-col gap-2">
              <Label htmlFor="watchlist-name" className="sr-only">
                Watchlist name
              </Label>
              <div className="flex items-center gap-2">
                <Input
                  id="watchlist-name"
                  ref={inputRef}
                  value={draft}
                  onChange={(e) => setDraft(e.target.value)}
                  maxLength={40}
                  invalid={!!error}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter') {
                      e.preventDefault();
                      save();
                    } else if (e.key === 'Escape') {
                      e.preventDefault();
                      cancel();
                    }
                  }}
                  className="max-w-sm"
                  aria-label="Watchlist name"
                />
                <Button
                  type="button"
                  variant="primary"
                  size="sm"
                  onClick={save}
                  aria-label="Save name"
                >
                  <Check size={14} aria-hidden />
                  Save
                </Button>
                <Button
                  type="button"
                  variant="ghost"
                  size="sm"
                  onClick={cancel}
                  aria-label="Cancel rename"
                >
                  <X size={14} aria-hidden />
                </Button>
              </div>
              {error && (
                <p role="alert" className="text-small text-negative">
                  {error}
                </p>
              )}
            </div>
          ) : (
            <h1 className="mt-1 flex items-center gap-3 font-display text-display text-text">
              <span className="truncate">{watchlist.name}</span>
              <button
                type="button"
                onClick={() => setEditing(true)}
                aria-label="Rename watchlist"
                className="rounded p-1.5 text-text-muted hover:bg-surface-alt hover:text-text focus-visible:outline focus-visible:outline-2 focus-visible:outline-focus-ring"
              >
                <Pencil size={16} aria-hidden />
              </button>
            </h1>
          )}
          {!editing && <div className="page-title-rule" aria-hidden />}
          {!editing && (
            <p className="mt-3 text-body text-text-muted">
              {watchlist.tickers.length === 0
                ? 'No tickers yet.'
                : `${watchlist.tickers.length} ticker${watchlist.tickers.length === 1 ? '' : 's'}`}
            </p>
          )}
        </div>

        {!editing && (
          <div className="flex flex-wrap gap-2">
            <Button variant="secondary" size="sm" onClick={() => setConfirmOpen(true)}>
              <Trash2 size={14} aria-hidden />
              Delete
            </Button>
          </div>
        )}
      </div>

      <Dialog
        open={confirmOpen}
        onClose={() => setConfirmOpen(false)}
        title="Delete watchlist"
        description={`This will permanently delete "${watchlist.name}". This cannot be undone.`}
        footer={
          <>
            <Button variant="ghost" onClick={() => setConfirmOpen(false)}>
              Cancel
            </Button>
            <Button
              variant="danger"
              onClick={() => {
                remove(watchlist.id);
                setConfirmOpen(false);
                onDeleted();
              }}
            >
              Delete
            </Button>
          </>
        }
      >
        <p className="text-body text-text-muted">
          Deleting this watchlist does not affect your portfolio or transactions.
        </p>
      </Dialog>
    </>
  );
}
