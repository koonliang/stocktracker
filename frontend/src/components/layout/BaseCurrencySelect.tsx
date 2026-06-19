import { useEffect, useState } from 'react';
import { getBaseCurrency, updateBaseCurrency } from '@/api/settingsApi';
import { SUPPORTED_CURRENCIES_CHANGED_EVENT } from '@/api/searchApi';
import { usePortfolioStore } from '@/stores/portfolioStore';

/**
 * Base reporting-currency picker (FR-031). Reads the current value + supported list, persists a
 * change, then re-loads the dashboard so combined totals re-express in the new base currency.
 */
export function BaseCurrencySelect() {
  const [base, setBase] = useState<string>('');
  const [supported, setSupported] = useState<string[]>([]);
  const [saving, setSaving] = useState(false);
  const loadDashboard = usePortfolioStore((s) => s.loadDashboard);

  useEffect(() => {
    let active = true;
    const loadSettings = () =>
      getBaseCurrency()
        .then((response) => {
          if (!active) return;
          setBase(response.baseCurrency);
          setSupported(response.supported);
        })
        .catch(() => {
          /* leave hidden if settings are unavailable */
        });
    const refreshSupportedCurrencies = () => {
      if (!saving) {
        void loadSettings();
      }
    };
    void loadSettings();
    globalThis.window.addEventListener(
      SUPPORTED_CURRENCIES_CHANGED_EVENT,
      refreshSupportedCurrencies,
    );
    return () => {
      active = false;
      globalThis.window.removeEventListener(
        SUPPORTED_CURRENCIES_CHANGED_EVENT,
        refreshSupportedCurrencies,
      );
    };
  }, [saving]);

  async function handleChange(next: string) {
    const previous = base;
    setBase(next);
    setSaving(true);
    try {
      await updateBaseCurrency(next);
      await loadDashboard();
      window.dispatchEvent(new CustomEvent('stocktracker:base-currency-changed'));
    } catch {
      setBase(previous); // revert on failure
    } finally {
      setSaving(false);
    }
  }

  if (supported.length === 0) {
    return null;
  }

  return (
    <select
      value={base}
      onChange={(e) => void handleChange(e.target.value)}
      disabled={saving}
      aria-label="Base currency"
      data-testid="base-currency-select"
      className="h-9 rounded-md border border-border bg-surface px-2 text-small text-text transition-colors hover:border-border-strong focus-visible:outline focus-visible:outline-2 focus-visible:outline-focus-ring disabled:opacity-50"
    >
      {supported.map((currency) => (
        <option key={currency} value={currency}>
          {currency}
        </option>
      ))}
    </select>
  );
}
