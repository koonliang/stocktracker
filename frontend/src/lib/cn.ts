/** Tiny classname combiner (no dependency). Filters falsy values and dedupes whitespace. */
export function cn(...parts: Array<string | false | null | undefined>): string {
  return parts.filter(Boolean).join(' ').replace(/\s+/g, ' ').trim();
}
