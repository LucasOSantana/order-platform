import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router, RouterLink } from '@angular/router';
import { NgIcon, provideIcons } from '@ng-icons/core';
import { lucideArrowLeft } from '@ng-icons/lucide';
import { finalize } from 'rxjs';
import { FieldError, toApiError } from '../../../../core/http/api-error';
import { NotificationService } from '../../../../core/notifications/notification.service';
import { OrderForm } from '../../components/order-form/order-form';
import { OrdersApi } from '../../data-access/orders-api';
import { CreateOrderRequest } from '../../models/order';

@Component({
  selector: 'app-order-create-page',
  imports: [RouterLink, NgIcon, OrderForm],
  viewProviders: [provideIcons({ lucideArrowLeft })],
  templateUrl: './order-create-page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OrderCreatePage {
  private readonly ordersApi = inject(OrdersApi);
  private readonly notifications = inject(NotificationService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly saving = signal(false);
  protected readonly serverErrors = signal<FieldError[]>([]);

  protected create(request: CreateOrderRequest): void {
    this.saving.set(true);
    this.serverErrors.set([]);

    this.ordersApi
      .create(request)
      .pipe(
        finalize(() => this.saving.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (order) => {
          this.notifications.success(
            `Pedido #${order.id.slice(0, 8)} criado e enviado para processamento.`,
          );
          void this.router.navigate(['/orders', order.id]);
        },
        error: (err: unknown) => {
          const apiError = toApiError(err);
          this.serverErrors.set(apiError.fieldErrors);
          this.notifications.error(`Não foi possível criar o pedido. ${apiError.message}`);
        },
      });
  }

  protected cancel(): void {
    void this.router.navigate(['/orders']);
  }
}
