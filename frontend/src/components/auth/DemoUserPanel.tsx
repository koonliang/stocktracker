import { Button } from '@/components/ui/Button';
import type { DemoUserCatalog, DemoUserListItem } from '@/api/types';

type Props = {
  catalog: DemoUserCatalog | null;
  loading?: boolean;
  pendingSlot?: number | null;
  feedback?: string | null;
  onCreate: () => void;
  onLogin: (slot: number) => void;
};

export function DemoUserPanel({
  catalog,
  loading = false,
  pendingSlot = null,
  feedback = null,
  onCreate,
  onLogin,
}: Props) {
  const users = catalog?.users ?? [];

  return (
    <section className="rounded-xl border border-border bg-surface p-4 shadow-card sm:p-5">
      <div className="flex items-start justify-between gap-4">
        <div>
          <p className="eyebrow">Quick Access</p>
          <h2 className="mt-2 font-display text-title">Demo accounts</h2>
          <p className="mt-1 text-small text-text-muted">
            Jump into seeded portfolios for fast QA and walkthroughs.
          </p>
        </div>
        {catalog ? (
          <span className="shrink-0 rounded-full border border-border bg-surface-alt px-2.5 py-1 font-mono text-small tabular tracking-normal text-text-muted">
            {users.length}/{catalog.maxUsers}
          </span>
        ) : null}
      </div>

      <div className="mt-3.5 space-y-2" data-testid="demo-user-list">
        {users.map((user: DemoUserListItem) => (
          <div
            key={user.slot}
            className="flex items-center justify-between gap-3 rounded-lg border border-border bg-surface-alt px-3.5 py-2.5"
          >
            <div className="min-w-0">
              <p className="truncate font-medium text-text">{user.label}</p>
              <p className="mt-0.5 text-small text-text-muted">Seeded portfolio data</p>
            </div>
            <Button
              type="button"
              size="sm"
              variant="secondary"
              data-testid={`demo-user-login-${user.slot}`}
              className="min-w-24"
              loading={pendingSlot === user.slot}
              onClick={() => onLogin(user.slot)}
            >
              Use demo
            </Button>
          </div>
        ))}
      </div>

      {feedback ? (
        <p data-testid="demo-user-limit-message" className="mt-3.5 text-small text-negative">
          {feedback}
        </p>
      ) : null}

      <Button
        type="button"
        data-testid="demo-user-create"
        variant={catalog != null && !catalog.canCreate ? 'secondary' : 'primary'}
        className="mt-4 w-full"
        loading={loading}
        onClick={onCreate}
        disabled={catalog != null && !catalog.canCreate}
      >
        {catalog != null && !catalog.canCreate ? 'All Demo Slots In Use' : 'Create Demo User'}
      </Button>
    </section>
  );
}
