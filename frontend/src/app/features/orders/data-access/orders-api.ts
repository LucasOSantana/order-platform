import { Observable } from 'rxjs';
import { Page } from '../../../shared/models/page';
import { CreateOrderRequest, Order, OrderSummary } from '../models/order';
import { OrderListQuery } from '../models/order-list-query';

export abstract class OrdersApi {
  /** GET /api/orders */
  abstract list(query: OrderListQuery): Observable<Page<OrderSummary>>;

  /** GET /api/orders/{id} */
  abstract getById(id: string): Observable<Order>;

  /** POST /api/orders */
  abstract create(request: CreateOrderRequest): Observable<Order>;
}
