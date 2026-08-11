import {
  canMoveDown,
  canMoveUp,
  moveItemDown,
  moveItemUp,
  toOrderedIds,
} from '@/features/training/utils/reorderList';

const items = [
  { id: 'a', label: 'A' },
  { id: 'b', label: 'B' },
  { id: 'c', label: 'C' },
];

describe('reorderList', () => {
  it('moves item up', () => {
    expect(toOrderedIds(moveItemUp(items, 'b'))).toEqual(['b', 'a', 'c']);
  });

  it('moves item down', () => {
    expect(toOrderedIds(moveItemDown(items, 'b'))).toEqual(['a', 'c', 'b']);
  });

  it('does not move first item up', () => {
    expect(moveItemUp(items, 'a')).toEqual(items);
    expect(canMoveUp(items, 'a')).toBe(false);
  });

  it('does not move last item down', () => {
    expect(moveItemDown(items, 'c')).toEqual(items);
    expect(canMoveDown(items, 'c')).toBe(false);
  });
});
