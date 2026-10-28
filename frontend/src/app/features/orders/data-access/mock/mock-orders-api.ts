import { HttpErrorResponse } from '@angular/common/http';
import { Injectable, InjectionToken, inject } from '@angular/core';
import { Observable, defer, map, timer } from 'rxjs';
import { FieldError, ProblemDetail } from '../../../../core/http/api-error';
import { Page } from '../../../../shared/models/page';
import {
  CreateOrderRequest,
  Order,
  OrderStatus,
  OrderSummary,
  isTerminalStatus,
} from '../../models/order';
import { OrderListQuery, OrderSortField } from '../../models/order-list-query';
import { calculateLineTotal, calculateOrderTotal } from '../../utils/order-total';
import { OrdersApi } from '../orders-api';
import { createSeedOrders, createUuid } from './mock-orders.seed';

export interface MockOrdersApiConfig {
  latencyMs: number;
  processingStartDelayMs: number;
  processingDurationMs: number;
  seedOrders: () => Order[];
}

export const MOCK_ORDERS_API_CONFIG = new InjectionToken<MockOrdersApiConfig>(
  'MOCK_ORDERS_API_CONFIG',
  {
    providedIn: 'root',
    factory: () => ({
      latencyMs: 450,
      processingStartDelayMs: 2000,
      processingDurationMs: 5000,
      seedOrders: () => createSeedOrders(),
    }),
  },
);

/** Regras mockadas par atestar erros */
export const MOCK_RULES = {
  rejectedEmailDomain: '@erro.test',
  failureKeyword: 'falha',
  maxOrderTotal: 10_000,
} as const;

const SORT_COMPARATORS: Record<OrderSortField, (a: OrderSummary, b: OrderSummary) => number> = {
  createdAt: (a, b) => a.createdAt.localeCompare(b.createdAt),
  customerName: (a, b) => a.customerName.localeCompare(b.customerName, 'pt-BR'),
  totalAmount: (a, b) => a.totalAmount - b.totalAmount,
  status: (a, b) => a.status.localeCompare(b.status),
};

/**
 * Dados mockados
 */
@Injectable()
export class MockOrdersApi extends OrdersApi {
  private readonly config = inject(MOCK_ORDERS_API_CONFIG);
  private readonly orders = new Map<string, Order>();
  private nextItemId = 100_000;

  constructor() {
    super();
    for (const order of this.config.seedOrders()) {
      this.orders.set(order.id, order);
      if (!isTerminalStatus(order.status)) {
        this.scheduleProcessing(order.id);
      }
    }
  }

  override list(query: OrderListQuery): Observable<Page<OrderSummary>> {
    return this.respond(() => this.findPage(query));
  }

  override getById(id: string): Observable<Order> {
    return this.respond(() => {
      const order = this.orders.get(id);
      if (!order) {
        throw problem(404, {
          title: 'Pedido não encontrado',
          detail: `Pedido ${id} não encontrado.`,
        });
      }
      return order;
    });
  }

  override create(request: CreateOrderRequest): Observable<Order> {
    return this.respond(() => {
      this.validate(request);
      const order = this.buildOrder(request);
      this.orders.set(order.id, order);
      this.scheduleProcessing(order.id);
      return order;
    });
  }

  private respond<T>(handler: () => T): Observable<T> {
    return defer(() => timer(this.config.latencyMs).pipe(map(() => structuredClone(handler()))));
  }

  private findPage(query: OrderListQuery): Page<OrderSummary> {
    const from = query.createdFrom ? new Date(`${query.createdFrom}T00:00:00`).getTime() : null;
    const to = query.createdTo ? new Date(`${query.createdTo}T23:59:59.999`).getTime() : null;
    const compare = SORT_COMPARATORS[query.sort.field];
    const direction = query.sort.direction === 'asc' ? 1 : -1;

    const filtered = [...this.orders.values()]
      .filter((order) => !query.status || order.status === query.status)
      .filter((order) => {
        const createdAt = new Date(order.createdAt).getTime();
        return (from === null || createdAt >= from) && (to === null || createdAt <= to);
      })
      .map(toSummary)
      .sort((a, b) => direction * compare(a, b) || b.createdAt.localeCompare(a.createdAt));

    const start = query.page * query.size;
    return {
      content: filtered.slice(start, start + query.size),
      page: {
        number: query.page,
        size: query.size,
        totalElements: filtered.length,
        totalPages: Math.ceil(filtered.length / query.size),
      },
    };
  }

  private validate(request: CreateOrderRequest): void {
    const errors: FieldError[] = [];
    if (request.customerEmail.toLowerCase().endsWith(MOCK_RULES.rejectedEmailDomain)) {
      errors.push({ field: 'customerEmail', message: 'Domínio de e-mail não aceito.' });
    }
    if (request.items.length === 0) {
      errors.push({ field: 'items', message: 'Informe ao menos um item.' });
    }
    if (errors.length > 0) {
      throw problem(400, {
        title: 'Requisição inválida',
        detail: 'Alguns campos não passaram na validação.',
        errors,
      });
    }
  }

  private buildOrder(request: CreateOrderRequest): Order {
    const now = new Date().toISOString();
    const items = request.items.map((item) => ({
      id: this.nextItemId++,
      ...item,
      lineTotal: calculateLineTotal(item),
    }));

    return {
      id: createUuid(),
      customerName: request.customerName,
      customerEmail: request.customerEmail,
      status: 'RECEIVED',
      totalAmount: calculateOrderTotal(items),
      items,
      failureReason: null,
      attempts: 0,
      createdAt: now,
      updatedAt: now,
      processedAt: null,
      statusHistory: [{ fromStatus: null, toStatus: 'RECEIVED', reason: null, changedAt: now }],
    };
  }

  private scheduleProcessing(id: string): void {
    const startDelay =
      this.orders.get(id)?.status === 'RECEIVED' ? this.config.processingStartDelayMs : 0;

    setTimeout(() => {
      if (this.orders.get(id)?.status === 'RECEIVED') {
        this.transition(id, 'PROCESSING', null);
      }
      setTimeout(() => this.finishProcessing(id), this.config.processingDurationMs);
    }, startDelay);
  }

  private finishProcessing(id: string): void {
    const order = this.orders.get(id);
    if (!order || order.status !== 'PROCESSING') {
      return;
    }
    const failureReason = businessRuleViolation(order);
    this.transition(id, failureReason ? 'FAILED' : 'COMPLETED', failureReason);
  }

  private transition(id: string, toStatus: OrderStatus, reason: string | null): void {
    const order = this.orders.get(id);
    if (!order) {
      return;
    }
    const now = new Date().toISOString();
    order.statusHistory.push({ fromStatus: order.status, toStatus, reason, changedAt: now });
    order.status = toStatus;
    order.updatedAt = now;

    if (toStatus === 'PROCESSING') {
      order.attempts += 1;
    }
    if (isTerminalStatus(toStatus)) {
      order.processedAt = now;
      order.failureReason = toStatus === 'FAILED' ? reason : null;
    }
  }
}

function businessRuleViolation(order: Order): string | null {
  if (order.totalAmount > MOCK_RULES.maxOrderTotal) {
    return 'Valor total acima do limite permitido para processamento automático.';
  }
  const rejected = order.items.find((item) =>
    item.description.toLowerCase().includes(MOCK_RULES.failureKeyword),
  );
  return rejected ? `Item "${rejected.description}" reprovado na validação.` : null;
}

function toSummary(order: Order): OrderSummary {
  return {
    id: order.id,
    customerName: order.customerName,
    customerEmail: order.customerEmail,
    status: order.status,
    totalAmount: order.totalAmount,
    itemCount: order.items.length,
    createdAt: order.createdAt,
    updatedAt: order.updatedAt,
  };
}

function problem(status: number, body: Omit<ProblemDetail, 'status'>): HttpErrorResponse {
  return new HttpErrorResponse({
    status,
    statusText: body.title,
    url: '/api/orders',
    error: { ...body, status },
  });
}
