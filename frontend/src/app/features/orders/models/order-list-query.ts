import { ParamMap, Params } from '@angular/router';
import { SortDirection, SortState } from '../../../shared/models/page';
import { ORDER_STATUSES, OrderStatus } from './order';

export const ORDER_SORT_FIELDS = ['createdAt', 'customerName', 'totalAmount', 'status'] as const;
export type OrderSortField = (typeof ORDER_SORT_FIELDS)[number];

export const PAGE_SIZE_OPTIONS = [10, 20, 50] as const;

export interface OrderListFilters {
  status: OrderStatus | null;
  createdFrom: string | null;
  createdTo: string | null;
}

export interface OrderListQuery extends OrderListFilters {
  page: number;
  size: number;
  sort: SortState<OrderSortField>;
}

export const DEFAULT_ORDER_LIST_QUERY: OrderListQuery = {
  page: 0,
  size: PAGE_SIZE_OPTIONS[0],
  sort: { field: 'createdAt', direction: 'desc' },
  status: null,
  createdFrom: null,
  createdTo: null,
};

const ISO_DATE = /^\d{4}-\d{2}-\d{2}$/;

export function parseOrderListQuery(params: ParamMap): OrderListQuery {
  const defaults = DEFAULT_ORDER_LIST_QUERY;
  const page = Number(params.get('page'));
  const size = Number(params.get('size'));
  const status = params.get('status');
  const createdFrom = params.get('createdFrom');
  const createdTo = params.get('createdTo');

  return {
    page: Number.isInteger(page) && page > 0 ? page : defaults.page,
    size: (PAGE_SIZE_OPTIONS as readonly number[]).includes(size) ? size : defaults.size,
    sort: parseSort(params.get('sort')) ?? defaults.sort,
    status: (ORDER_STATUSES as readonly string[]).includes(status ?? '')
      ? (status as OrderStatus)
      : null,
    createdFrom: createdFrom && ISO_DATE.test(createdFrom) ? createdFrom : null,
    createdTo: createdTo && ISO_DATE.test(createdTo) ? createdTo : null,
  };
}

export function toQueryParams(query: OrderListQuery): Params {
  const defaults = DEFAULT_ORDER_LIST_QUERY;
  const sort = formatSort(query.sort);
  return {
    page: query.page !== defaults.page ? query.page : null,
    size: query.size !== defaults.size ? query.size : null,
    sort: sort !== formatSort(defaults.sort) ? sort : null,
    status: query.status,
    createdFrom: query.createdFrom,
    createdTo: query.createdTo,
  };
}

export function formatSort(sort: SortState<OrderSortField>): string {
  return `${sort.field},${sort.direction}`;
}

function parseSort(value: string | null): SortState<OrderSortField> | null {
  const [field, direction] = value?.split(',') ?? [];
  const validField = (ORDER_SORT_FIELDS as readonly string[]).includes(field);
  const validDirection = direction === 'asc' || direction === 'desc';
  return validField && validDirection
    ? { field: field as OrderSortField, direction: direction as SortDirection }
    : null;
}
