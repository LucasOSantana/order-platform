import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { NgIcon, provideIcons } from '@ng-icons/core';
import { lucidePlus, lucideRefreshCw } from '@ng-icons/lucide';
import {
  EMPTY,
  Observable,
  Subject,
  catchError,
  finalize,
  interval,
  map,
  merge,
  of,
  switchMap,
  tap,
} from 'rxjs';
import { toApiError } from '../../../../core/http/api-error';
import { NotificationService } from '../../../../core/notifications/notification.service';
import { Page, SortState } from '../../../../shared/models/page';
import { Pagination } from '../../../../shared/ui/pagination/pagination';
import { Spinner } from '../../../../shared/ui/spinner/spinner';
import { OrderFilters } from '../../components/order-filters/order-filters';
import { OrderTable } from '../../components/order-table/order-table';
import { OrdersApi } from '../../data-access/orders-api';
import { OrderSummary } from '../../models/order';
import {
  DEFAULT_ORDER_LIST_QUERY,
  OrderListFilters,
  OrderListQuery,
  OrderSortField,
  PAGE_SIZE_OPTIONS,
  parseOrderListQuery,
  toQueryParams,
} from '../../models/order-list-query';

export const ORDER_LIST_POLLING_INTERVAL_MS = 5000;

type LoadTrigger = 'initial' | 'refresh' | 'poll';

@Component({
  selector: 'app-order-list-page',
  imports: [RouterLink, DatePipe, NgIcon, OrderFilters, OrderTable, Pagination, Spinner],
  viewProviders: [provideIcons({ lucidePlus, lucideRefreshCw })],
  templateUrl: './order-list-page.html',
  styleUrl: './order-list-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OrderListPage {
  private readonly ordersApi = inject(OrdersApi);
  private readonly notifications = inject(NotificationService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  protected readonly query = toSignal(this.route.queryParamMap.pipe(map(parseOrderListQuery)), {
    initialValue: DEFAULT_ORDER_LIST_QUERY,
  });
  protected readonly filters = computed<OrderListFilters>(() => {
    const { status, createdFrom, createdTo } = this.query();
    return { status, createdFrom, createdTo };
  });
  protected readonly hasActiveFilters = computed(() => {
    const { status, createdFrom, createdTo } = this.filters();
    return Boolean(status || createdFrom || createdTo);
  });

  protected readonly result = signal<Page<OrderSummary> | null>(null);
  protected readonly loading = signal(true);
  protected readonly refreshing = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly lastUpdatedAt = signal<Date | null>(null);

  protected readonly pageSizeOptions = PAGE_SIZE_OPTIONS;
  protected readonly pollingSeconds = ORDER_LIST_POLLING_INTERVAL_MS / 1000;

  private readonly manualRefresh$ = new Subject<void>();

  constructor() {
    toObservable(this.query)
      .pipe(
        switchMap((query) =>
          merge(
            of<LoadTrigger>('initial'),
            this.manualRefresh$.pipe(map((): LoadTrigger => 'refresh')),
            interval(ORDER_LIST_POLLING_INTERVAL_MS).pipe(map((): LoadTrigger => 'poll')),
          ).pipe(switchMap((trigger) => this.load(query, trigger))),
        ),
        takeUntilDestroyed(),
      )
      .subscribe();
  }

  protected refresh(): void {
    this.manualRefresh$.next();
  }

  protected onFiltersChange(filters: OrderListFilters): void {
    this.navigate({ ...this.query(), ...filters, page: 0 });
  }

  protected onSortChange(sort: SortState<OrderSortField>): void {
    this.navigate({ ...this.query(), sort, page: 0 });
  }

  protected onPageChange(page: number): void {
    this.navigate({ ...this.query(), page });
  }

  protected onSizeChange(size: number): void {
    this.navigate({ ...this.query(), size, page: 0 });
  }

  protected clearFilters(): void {
    this.onFiltersChange({ status: null, createdFrom: null, createdTo: null });
  }

  protected goToFirstPage(): void {
    this.onPageChange(0);
  }

  private load(query: OrderListQuery, trigger: LoadTrigger): Observable<unknown> {
    (trigger === 'initial' ? this.loading : this.refreshing).set(true);

    return this.ordersApi.list(query).pipe(
      tap((page) => {
        this.result.set(page);
        this.error.set(null);
        this.lastUpdatedAt.set(new Date());
      }),
      catchError((err: unknown) => {
        const { message } = toApiError(err);
        this.error.set(message);
        if (trigger === 'refresh') {
          this.notifications.error(message);
        }
        return EMPTY;
      }),
      finalize(() => {
        this.loading.set(false);
        this.refreshing.set(false);
      }),
    );
  }

  private navigate(query: OrderListQuery): void {
    void this.router.navigate([], { relativeTo: this.route, queryParams: toQueryParams(query) });
  }
}
