import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../../../../core/http/api-base-url';
import { Page } from '../../../../shared/models/page';
import { CreateOrderRequest, Order, OrderSummary } from '../../models/order';
import { OrderListQuery, formatSort } from '../../models/order-list-query';
import { OrdersApi } from '../orders-api';

@Injectable()
export class OrdersHttpApi extends OrdersApi {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${inject(API_BASE_URL)}/orders`;

  override list(query: OrderListQuery): Observable<Page<OrderSummary>> {
    return this.http.get<Page<OrderSummary>>(this.baseUrl, {
      params: toOrderListParams(query),
    });
  }

  override getById(id: string): Observable<Order> {
    return this.http.get<Order>(`${this.baseUrl}/${encodeURIComponent(id)}`);
  }

  override create(request: CreateOrderRequest): Observable<Order> {
    return this.http.post<Order>(this.baseUrl, request);
  }
}

// Filtros da listagem
export function toOrderListParams(query: OrderListQuery): HttpParams {
  const params: Record<string, string | number> = {
    page: query.page,
    size: query.size,
    sort: formatSort(query.sort),
  };
  if (query.status) {
    params['status'] = query.status;
  }
  if (query.createdFrom) {
    params['createdFrom'] = query.createdFrom;
  }
  if (query.createdTo) {
    params['createdTo'] = query.createdTo;
  }
  return new HttpParams({ fromObject: params });
}
