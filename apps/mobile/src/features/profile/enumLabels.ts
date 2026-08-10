export function formatEnumLabel(value: string): string {
  return value
    .toLowerCase()
    .split('_')
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ');
}

export function enumOptions<T extends string>(values: readonly T[]) {
  return values.map((value) => ({
    value,
    label: formatEnumLabel(value),
  }));
}
