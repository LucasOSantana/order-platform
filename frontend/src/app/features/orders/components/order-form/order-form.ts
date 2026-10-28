import { CurrencyPipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  computed,
  effect,
  inject,
  input,
  output,
  signal,
} from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import {
  AbstractControl,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { NgIcon, provideIcons } from '@ng-icons/core';
import { lucidePlus, lucideTrash2 } from '@ng-icons/lucide';
import { map, startWith } from 'rxjs';
import { FieldError } from '../../../../core/http/api-error';
import {
  integerValidator,
  notBlankValidator,
  validationMessage,
} from '../../../../shared/forms/validators';
import { Spinner } from '../../../../shared/ui/spinner/spinner';
import { CreateOrderRequest } from '../../models/order';
import { PricedItem, calculateLineTotal, calculateOrderTotal } from '../../utils/order-total';

@Component({
  selector: 'app-order-form',
  imports: [ReactiveFormsModule, CurrencyPipe, NgIcon, Spinner],
  viewProviders: [provideIcons({ lucidePlus, lucideTrash2 })],
  templateUrl: './order-form.html',
  styleUrl: './order-form.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OrderForm {
  readonly saving = input(false);
  readonly serverErrors = input<readonly FieldError[]>([]);

  readonly submitted = output<CreateOrderRequest>();
  readonly cancelled = output<void>();

  private readonly fb = inject(NonNullableFormBuilder);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

  protected readonly form = this.fb.group({
    customerName: ['', [Validators.required, Validators.maxLength(150), notBlankValidator]],
    customerEmail: ['', [Validators.required, Validators.email, Validators.maxLength(254)]],
    items: this.fb.array([this.createItemGroup()], Validators.minLength(1)),
  });

  protected readonly submitAttempted = signal(false);

  private readonly itemValues = toSignal(
    this.items.valueChanges.pipe(
      startWith(null),
      map(() => this.items.getRawValue().map(toPricedItem)),
    ),
    { requireSync: true },
  );
  protected readonly lineTotals = computed(() => this.itemValues().map(calculateLineTotal));
  protected readonly estimatedTotal = computed(() => calculateOrderTotal(this.itemValues()));

  constructor() {
    effect(() => (this.saving() ? this.form.disable() : this.form.enable()));
    effect(() => this.applyServerErrors(this.serverErrors()));
  }

  protected get items() {
    return this.form.controls.items;
  }

  protected addItem(): void {
    this.items.push(this.createItemGroup());
  }

  protected removeItem(index: number): void {
    if (this.items.length > 1) {
      this.items.removeAt(index);
    }
  }

  protected showError(control: AbstractControl): boolean {
    return control.invalid && (control.touched || this.submitAttempted());
  }

  protected errorMessage(control: AbstractControl): string | null {
    return validationMessage(control);
  }

  protected submit(): void {
    this.submitAttempted.set(true);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.focusFirstInvalidField();
      return;
    }

    const { customerName, customerEmail, items } = this.form.getRawValue();
    this.submitted.emit({
      customerName: customerName.trim(),
      customerEmail: customerEmail.trim(),
      items: items.map((item) => ({
        description: item.description.trim(),
        quantity: Number(item.quantity),
        unitPrice: Number(item.unitPrice),
      })),
    });
  }

  private createItemGroup() {
    return this.fb.group({
      description: ['', [Validators.required, Validators.maxLength(255), notBlankValidator]],
      quantity: this.fb.control<number | null>(1, [
        Validators.required,
        Validators.min(1),
        integerValidator,
      ]),
      unitPrice: this.fb.control<number | null>(null, [Validators.required, Validators.min(0.01)]),
    });
  }

  private applyServerErrors(errors: readonly FieldError[]): void {
    for (const { field, message } of errors) {
      const control = this.form.get(field.replace(/\[(\d+)\]/g, '.$1'));
      if (control) {
        control.setErrors({ ...control.errors, server: message });
        control.markAsTouched();
      }
    }
  }

  private focusFirstInvalidField(): void {
    this.host.nativeElement.querySelector<HTMLElement>('input.ng-invalid')?.focus();
  }
}

function toPricedItem(item: { quantity: number | null; unitPrice: number | null }): PricedItem {
  return { quantity: Number(item.quantity) || 0, unitPrice: Number(item.unitPrice) || 0 };
}
