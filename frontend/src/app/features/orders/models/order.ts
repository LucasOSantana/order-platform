export const ORDER_STATUSES = ['RECEIVED', 'PROCESSING', 'COMPLETED', 'FAILED'] as const;

export type OrderStatus = (typeof ORDER_STATUSES)[number];

export const ORDER_STATUS_LABELS: Record<OrderStatus, string> = {
  RECEIVED: 'Recebido',
  PROCESSING: 'Em processamento',
  COMPLETED: 'Concluído',
  FAILED: 'Falhou',
};

export function isTerminalStatus(status: OrderStatus): boolean {
  return status === 'COMPLETED' || status === 'FAILED';
}

export interface OrderItem {
  id: number;
  description: string;
  quantity: number;
  unitPrice: number;
  lineTotal: number;
}

export interface OrderStatusChange {
  fromStatus: OrderStatus | null;
  toStatus: OrderStatus;
  reason: string | null;
  changedAt: string;
}

export interface OrderSummary {
  id: string;
  customerName: string;
  customerEmail: string;
  status: OrderStatus;
  totalAmount: number;
  itemCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface Order extends Omit<OrderSummary, 'itemCount'> {
  items: OrderItem[];
  failureReason: string | null;
  attempts: number;
  processedAt: string | null;
  statusHistory: OrderStatusChange[];
}

export interface CreateOrderItemRequest {
  description: string;
  quantity: number;
  unitPrice: number;
}

export interface CreateOrderRequest {
  customerName: string;
  customerEmail: string;
  items: CreateOrderItemRequest[];
}
