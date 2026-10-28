import { ChangeDetectionStrategy, Component, effect, inject, input, output } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, ValidatorFn } from '@angular/forms';
import { ORDER_STATUS_LABELS, ORDER_STATUSES, OrderStatus } from '../../models/order';
import { OrderListFilters } from '../../models/order-list-query';

const dateRangeValidator: ValidatorFn = (group) => {
  const { createdFrom, createdTo } = group.value as { createdFrom?: string; createdTo?: string };
  return createdFrom && createdTo && createdFrom > createdTo ? { dateRange: true } : null;
};

@Component({
  selector: 'app-order-filters',
  imports: [ReactiveFormsModule],
  templateUrl: './order-filters.html',
  styleUrl: './order-filters.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OrderFilters {
  readonly filters = input.required<OrderListFilters>();
  readonly filtersChange = output<OrderListFilters>();

  protected readonly statusOptions = ORDER_STATUSES.map((value) => ({
    value,
    label: ORDER_STATUS_LABELS[value],
  }));

  private readonly fb = inject(NonNullableFormBuilder);
  protected readonly form = this.fb.group(
    {
      status: this.fb.control<OrderStatus | ''>(''),
      createdFrom: '',
      createdTo: '',
    },
    { validators: dateRangeValidator },
  );

  constructor() {
    // Mantém o formulário sincronizado com a URL (ex.: botão voltar do navegador).
    effect(() => {
      const { status, createdFrom, createdTo } = this.filters();
      this.form.setValue(
        { status: status ?? '', createdFrom: createdFrom ?? '', createdTo: createdTo ?? '' },
        { emitEvent: false },
      );
    });
  }

  protected apply(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { status, createdFrom, createdTo } = this.form.getRawValue();
    this.filtersChange.emit({
      status: status || null,
      createdFrom: createdFrom || null,
      createdTo: createdTo || null,
    });
  }

  protected clear(): void {
    this.form.reset();
    this.filtersChange.emit({ status: null, createdFrom: null, createdTo: null });
  }
}
