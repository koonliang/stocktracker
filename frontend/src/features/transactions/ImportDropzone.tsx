import { useRef, useState } from 'react';
import { Upload } from 'lucide-react';
import { Button } from '@/components/ui/Button';
import { cn } from '@/lib/cn';

type Props = {
  onFile: (file: File) => void;
  loading?: boolean;
};

export function ImportDropzone({ onFile, loading = false }: Props) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [dragOver, setDragOver] = useState(false);

  return (
    // eslint-disable-next-line jsx-a11y/no-static-element-interactions
    <div
      onDragOver={(e) => {
        e.preventDefault();
        setDragOver(true);
      }}
      onDragLeave={() => setDragOver(false)}
      onDrop={(e) => {
        e.preventDefault();
        setDragOver(false);
        const file = e.dataTransfer.files[0];
        if (file) onFile(file);
      }}
      className={cn(
        'flex flex-col items-center justify-center gap-3 rounded-lg border-2 border-dashed border-border bg-surface-alt/30 px-6 py-10 text-center transition-colors',
        dragOver && 'border-accent bg-accent/5',
      )}
    >
      <div
        aria-hidden
        className="flex h-10 w-10 items-center justify-center rounded-full border border-border bg-surface text-text-muted"
      >
        <Upload size={18} />
      </div>
      <div>
        <p className="font-medium text-text">Drop a CSV file to import</p>
        <p className="mt-1 text-small text-text-muted">
          Header: <code className="font-mono">date,ticker,type,quantity,price,fees</code>
        </p>
      </div>
      <Button variant="secondary" size="sm" type="button" onClick={() => inputRef.current?.click()}>
        {loading ? 'Uploading…' : 'Choose file'}
      </Button>
      <input
        ref={inputRef}
        type="file"
        accept=".csv,text/csv"
        aria-label="Choose CSV file"
        className="sr-only"
        onChange={(e) => {
          const file = e.target.files?.[0];
          if (file) onFile(file);
          e.target.value = '';
        }}
      />
    </div>
  );
}
