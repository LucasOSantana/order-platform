export interface PricedItem {
  quantity: number;
  unitPrice: number;
}

export function roundToCents(value: number): number {
  return Math.round((value + Number.EPSILON) * 100) / 100;
}

export function calculateLineTotal(item: PricedItem): number {
  return roundToCents(item.quantity * item.unitPrice);
}

/**
 * Mock
 */
export function calculateOrderTotal(items: readonly PricedItem[]): number {
  return roundToCents(items.reduce((total, item) => total + calculateLineTotal(item), 0));
}
