import { CurrencyPipe, DatePipe, SlicePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { NgIcon, provideIcons } from '@ng-icons/core';
import { lucideArrowLeft } from '@ng-icons/lucide';
import {
  BehaviorSubject,
  catchError,
  combineLatest,
  exhaustMap,
  map,
  of,
  switchMap,
  takeWhile,
  timer,
} from 'rxjs';
import { ApiError, toApiError } from '../../../../core/http/api-error';
import { Spinner } from '../../../../shared/ui/spinner/spinner';
import { OrderItemsTable } from '../../components/order-items-table/order-items-table';
import { OrderStatusBadge } from '../../components/order-status-badge/order-status-badge';
import { OrderStatusTimeline } from '../../components/order-status-timeline/order-status-timeline';
import { OrdersApi } from '../../data-access/orders-api';
import { Order, isTerminalStatus } from '../../models/order';

export const ORDER_DETAIL_POLLING_INTERVAL_MS = 3000;

type LoadResult = { order: Order } | { error: ApiError };

@Component({
  selector: 'app-order-detail-page',
  imports: [
    RouterLink,
    CurrencyPipe,
    DatePipe,
    SlicePipe,
    NgIcon,
    Spinner,
    OrderStatusBadge,
    OrderItemsTable,
    OrderStatusTimeline,
  ],
  viewProviders: [provideIcons({ lucideArrowLeft })],
  templateUrl: './order-detail-page.html',
  styleUrl: './order-detail-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OrderDetailPage {
  readonly id = input.required<string>();

  private readonly ordersApi = inject(OrdersApi);

  protected readonly order = signal<Order | null>(null);
  protected readonly error = signal<ApiError | null>(null);
  protected readonly loading = signal(true);
  protected readonly isProcessing = computed(() => {
    const order = this.order();
    return order !== null && !isTerminalStatus(order.status);
  });

  private readonly reload$ = new BehaviorSubject<void>(undefined);

  constructor() {
    //Pooling para status enquanto não for concluído ou erro
    combineLatest([toObservable(this.id), this.reload$])
      .pipe(
        switchMap(([id]) => {
          if (this.order()?.id !== id) {
            this.order.set(null);
          }
          this.loading.set(true);
          return timer(0, ORDER_DETAIL_POLLING_INTERVAL_MS).pipe(
            exhaustMap(() =>
              this.ordersApi.getById(id).pipe(
                map((order): LoadResult => ({ order })),
                catchError((err: unknown) => of<LoadResult>({ error: toApiError(err) })),
              ),
            ),
            takeWhile(
              (result) => 'order' in result && !isTerminalStatus(result.order.status),
              true,
            ),
          );
        }),
        takeUntilDestroyed(),
      )
      .subscribe((result) => {
        this.loading.set(false);
        if ('order' in result) {
          this.order.set(result.order);
          this.error.set(null);
        } else {
          this.error.set(result.error);
        }
      });
  }

  protected retry(): void {
    this.error.set(null);
    this.reload$.next();
  }
}
