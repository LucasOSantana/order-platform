import { CurrencyPipe, DatePipe, NgTemplateOutlet, SlicePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';
import { NgIcon, provideIcons } from '@ng-icons/core';
import { lucideChevronDown, lucideChevronUp, lucideChevronsUpDown } from '@ng-icons/lucide';
import { SortState } from '../../../../shared/models/page';
import { OrderSummary } from '../../models/order';
import { OrderSortField } from '../../models/order-list-query';
import { OrderStatusBadge } from '../order-status-badge/order-status-badge';

interface SortableColumn {
  field: OrderSortField;
  label: string;
}

@Component({
  selector: 'app-order-table',
  imports: [
    RouterLink,
    NgTemplateOutlet,
    NgIcon,
    CurrencyPipe,
    DatePipe,
    SlicePipe,
    OrderStatusBadge,
  ],
  viewProviders: [provideIcons({ lucideChevronUp, lucideChevronDown, lucideChevronsUpDown })],
  templateUrl: './order-table.html',
  styleUrl: './order-table.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OrderTable {
  readonly orders = input.required<OrderSummary[]>();
  readonly sort = input.required<SortState<OrderSortField>>();
  readonly sortChange = output<SortState<OrderSortField>>();

  protected readonly columns = {
    customerName: { field: 'customerName', label: 'Cliente' },
    status: { field: 'status', label: 'Status' },
    totalAmount: { field: 'totalAmount', label: 'Total' },
    createdAt: { field: 'createdAt', label: 'Criado em' },
  } satisfies Record<OrderSortField, SortableColumn>;

  protected toggleSort(field: OrderSortField): void {
    const current = this.sort();
    const direction =
      current.field === field
        ? current.direction === 'asc'
          ? 'desc'
          : 'asc'
        : field === 'createdAt' || field === 'totalAmount'
          ? 'desc'
          : 'asc';
    this.sortChange.emit({ field, direction });
  }

  protected sortIcon(field: OrderSortField): string {
    switch (this.ariaSort(field)) {
      case 'ascending':
        return 'lucideChevronUp';
      case 'descending':
        return 'lucideChevronDown';
      default:
        return 'lucideChevronsUpDown';
    }
  }

  protected ariaSort(field: OrderSortField): 'ascending' | 'descending' | 'none' {
    const current = this.sort();
    if (current.field !== field) {
      return 'none';
    }
    return current.direction === 'asc' ? 'ascending' : 'descending';
  }
}
