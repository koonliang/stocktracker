import { PageHeader } from '@/components/layout/PageHeader';
import { EmptyState } from '@/components/ui/EmptyState';

type Props = {
  eyebrow: string;
  title: string;
  description: string;
};

export function PlaceholderRoute({ eyebrow, title, description }: Props) {
  return (
    <>
      <PageHeader eyebrow={eyebrow} title={title} />
      <EmptyState
        eyebrow="Coming next"
        title="This view is part of a later iteration."
        description={description}
      />
    </>
  );
}
