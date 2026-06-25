import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Dialog } from '@/components/ui/Dialog';
import { Input } from '@/components/ui/Input';
import { Label } from '@/components/ui/Label';
import { Button } from '@/components/ui/Button';
import { useWatchlistStore } from '@/stores/watchlistStore';

type Props = {
  open: boolean;
  onClose: () => void;
  onCreated?: (id: string) => void;
};

const schema = z.object({
  name: z.string().trim().min(1, 'Name is required').max(40, 'Max 40 characters'),
});

type FormValues = z.infer<typeof schema>;

export function NewWatchlistDialog({ open, onClose, onCreated }: Props) {
  const create = useWatchlistStore((s) => s.create);

  const {
    register,
    handleSubmit,
    reset,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { name: '' },
  });

  useEffect(() => {
    if (!open) reset({ name: '' });
  }, [open, reset]);

  const onSubmit = handleSubmit(async (values) => {
    const result = await create(values.name);
    if (!result.ok) {
      const message =
        result.reason === 'duplicate-name'
          ? 'A watchlist with this name already exists'
          : result.reason === 'too-long'
            ? 'Max 40 characters'
            : result.reason === 'server'
              ? 'Could not create watchlist right now'
              : 'Name is required';
      setError('name', { type: 'manual', message });
      return;
    }
    onCreated?.(result.id);
    onClose();
  });

  return (
    <Dialog
      open={open}
      onClose={onClose}
      title="New Watchlist"
      description="Give your watchlist a name. You can add tickers after it's created."
      footer={
        <>
          <Button type="button" variant="ghost" onClick={onClose}>
            Cancel
          </Button>
          <Button type="submit" form="new-watchlist-form" loading={isSubmitting}>
            Create
          </Button>
        </>
      }
    >
      <form id="new-watchlist-form" onSubmit={onSubmit} className="flex flex-col gap-2">
        <Label htmlFor="new-watchlist-name">Name</Label>
        <Input
          id="new-watchlist-name"
          {...register('name')}
          placeholder="Tech Majors"
          maxLength={40}
          invalid={!!errors.name}
          autoComplete="off"
        />
        {errors.name?.message && (
          <p role="alert" className="text-small text-negative">
            {errors.name.message}
          </p>
        )}
      </form>
    </Dialog>
  );
}
