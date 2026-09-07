export function formatScore(value: number | string): string {
  if (typeof value === 'number') {
    return Number.isInteger(value) ? String(value) : value.toFixed(1);
  }
  return value;
}
