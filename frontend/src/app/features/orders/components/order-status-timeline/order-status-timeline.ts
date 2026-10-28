import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { OrderStatusChange } from '../../models/order';
import { OrderStatusBadge } from '../order-status-badge/order-status-badge';

@Component({
  selector: 'app-order-status-timeline',
  imports: [DatePipe, OrderStatusBadge],
  template: `
    <ol class="timeline">
      @for (change of history(); track change.changedAt + change.toStatus) {
        <li class="timeline__item" [attr.data-status]="change.toStatus">
          <div class="timeline__header">
            <app-order-status-badge [status]="change.toStatus" />
            <time class="text-muted" [attr.datetime]="change.changedAt">
              {{ change.changedAt | date: 'dd/MM/yyyy HH:mm:ss' }}
            </time>
          </div>
          @if (change.reason) {
            <p class="timeline__reason">{{ change.reason }}</p>
          }
        </li>
      }
    </ol>
  `,
  styleUrl: './order-status-timeline.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OrderStatusTimeline {
  readonly history = input.required<OrderStatusChange[]>();
}
