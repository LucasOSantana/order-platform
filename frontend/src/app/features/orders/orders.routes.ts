import { Routes } from '@angular/router';

export const ORDERS_ROUTES: Routes = [
  {
    path: '',
    title: 'Pedidos | Orderly',
    loadComponent: () =>
      import('./pages/order-list-page/order-list-page').then((m) => m.OrderListPage),
  },
  {
    path: 'new',
    title: 'Novo pedido | Orderly',
    loadComponent: () =>
      import('./pages/order-create-page/order-create-page').then((m) => m.OrderCreatePage),
  },
  {
    path: ':id',
    title: 'Detalhes do pedido | Orderly',
    loadComponent: () =>
      import('./pages/order-detail-page/order-detail-page').then((m) => m.OrderDetailPage),
  },
];
