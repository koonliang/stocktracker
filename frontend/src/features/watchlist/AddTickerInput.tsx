import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Plus } from 'lucide-react';
import { Input } from '@/components/ui/Input';
import { Button } from '@/components/ui/Button';
import { useWatchlistStore } from '@/stores/watchlistStore';

type Props = {
  watchlistId: string;
};

const schema = z.object({
  symbol: z
    .string()
    .trim()
    .min(1, 'Enter a ticker symbol')
    .max(5, 'Max 5 characters')
    .regex(/^[A-Za-z]+$/, 'Letters only'),
});

type FormValues = z.infer<typeof schema>;

export function AddTickerInput({ watchlistId }: Props) {
  const addTicker = useWatchlistStore((s) => s.addTicker);
  const {
    register,
    handleSubmit,
    reset,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { symbol: '' },
  });

  const onSubmit = handleSubmit((values) => {
    const result = addTicker(watchlistId, values.symbol);
    if (!result.ok) {
      const message =
        result.reason === 'unknown'
          ? 'Not a known ticker'
          : result.reason === 'duplicate'
            ? 'Already in this watchlist'
            : 'Could not add ticker';
      setError('symbol', { type: 'manual', message });
      return;
    }
    reset({ symbol: '' });
  });

  useEffect(() => {
    reset({ symbol: '' });
  }, [watchlistId, reset]);

  return (
    <form onSubmit={onSubmit} className="flex flex-col gap-2 sm:flex-row sm:items-start">
      <div className="flex-1">
        <Input
          {...register('symbol')}
          placeholder="Add ticker (e.g. AAPL)"
          aria-label="Add ticker"
          invalid={!!errors.symbol}
          maxLength={5}
          autoComplete="off"
          style={{ textTransform: 'uppercase' }}
        />
        {errors.symbol?.message && (
          <p role="alert" className="mt-1 text-small text-negative">
            {errors.symbol.message}
          </p>
        )}
      </div>
      <Button type="submit" loading={isSubmitting} className="sm:shrink-0">
        <Plus size={14} aria-hidden />
        Add
      </Button>
    </form>
  );
}
