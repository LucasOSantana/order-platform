import { Provider } from '@angular/core';
import { OrdersHttpApi } from './http/orders';
import { MockOrdersApi } from './mock/mock-orders-api';
import { OrdersApi } from './orders-api';

export type OrdersApiSource = 'http' | 'mock';

export function provideOrdersApi(source: OrdersApiSource): Provider {
  return { provide: OrdersApi, useClass: source === 'http' ? OrdersHttpApi : MockOrdersApi };
}
