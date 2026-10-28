import { Routes } from '@angular/router';

export const ORDERS_ROUTES: Routes = [
  {
    path: '',
    title: 'Pedidos | Order Platform',
    loadComponent: () =>
      import('./pages/order-list-page/order-list-page').then((m) => m.OrderListPage),
  },
  {
    path: 'new',
    title: 'Novo pedido | Order Platform',
    loadComponent: () =>
      import('./pages/order-create-page/order-create-page').then((m) => m.OrderCreatePage),
  },
  {
    path: ':id',
    title: 'Detalhes do pedido | Order Platform',
    loadComponent: () =>
      import('./pages/order-detail-page/order-detail-page').then((m) => m.OrderDetailPage),
  },
];
