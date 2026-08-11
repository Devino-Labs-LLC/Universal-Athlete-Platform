export function moveItemUp<T extends { id: string }>(items: T[], itemId: string): T[] {
  const index = items.findIndex((item) => item.id === itemId);
  if (index <= 0) {
    return items;
  }
  const next = [...items];
  const [item] = next.splice(index, 1);
  next.splice(index - 1, 0, item!);
  return next;
}

export function moveItemDown<T extends { id: string }>(items: T[], itemId: string): T[] {
  const index = items.findIndex((item) => item.id === itemId);
  if (index < 0 || index >= items.length - 1) {
    return items;
  }
  const next = [...items];
  const [item] = next.splice(index, 1);
  next.splice(index + 1, 0, item!);
  return next;
}

export function toOrderedIds<T extends { id: string }>(items: T[]): string[] {
  return items.map((item) => item.id);
}

export function canMoveUp<T extends { id: string }>(items: T[], itemId: string): boolean {
  const index = items.findIndex((item) => item.id === itemId);
  return index > 0;
}

export function canMoveDown<T extends { id: string }>(items: T[], itemId: string): boolean {
  const index = items.findIndex((item) => item.id === itemId);
  return index >= 0 && index < items.length - 1;
}
