import { convertToParamMap } from '@angular/router';
import { DEFAULT_ORDER_LIST_QUERY, parseOrderListQuery, toQueryParams } from './order-list-query';

describe('order-list-query', () => {
  it('returns the defaults when there are no params', () => {
    expect(parseOrderListQuery(convertToParamMap({}))).toEqual(DEFAULT_ORDER_LIST_QUERY);
  });

  it('parses valid params', () => {
    const query = parseOrderListQuery(
      convertToParamMap({
        page: '2',
        size: '20',
        sort: 'totalAmount,asc',
        status: 'FAILED',
        createdFrom: '2026-10-01',
        createdTo: '2026-10-31',
      }),
    );

    expect(query).toEqual({
      page: 2,
      size: 20,
      sort: { field: 'totalAmount', direction: 'asc' },
      status: 'FAILED',
      createdFrom: '2026-10-01',
      createdTo: '2026-10-31',
    });
  });

  it('ignores invalid params', () => {
    const query = parseOrderListQuery(
      convertToParamMap({
        page: '-1',
        size: '999',
        sort: 'password,asc',
        status: 'DELETED',
        createdFrom: '01/10/2026',
      }),
    );

    expect(query).toEqual(DEFAULT_ORDER_LIST_QUERY);
  });

  it('omits default values from the URL', () => {
    expect(toQueryParams(DEFAULT_ORDER_LIST_QUERY)).toEqual({
      page: null,
      size: null,
      sort: null,
      status: null,
      createdFrom: null,
      createdTo: null,
    });
  });

  it('round-trips a custom query', () => {
    const query = {
      ...DEFAULT_ORDER_LIST_QUERY,
      page: 3,
      sort: { field: 'customerName', direction: 'asc' },
      status: 'COMPLETED',
    } as const;
    const params = Object.fromEntries(
      Object.entries(toQueryParams(query)).filter(([, value]) => value !== null),
    );

    expect(parseOrderListQuery(convertToParamMap(params))).toEqual(query);
  });
});
