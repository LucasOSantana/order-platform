import { CurrencyPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { OrderItem } from '../../models/order';

@Component({
  selector: 'app-order-items-table',
  imports: [CurrencyPipe],
  template: `
    <div class="table-wrapper">
      <table class="table">
        <thead>
          <tr>
            <th scope="col">Descrição</th>
            <th scope="col" class="table__numeric">Quantidade</th>
            <th scope="col" class="table__numeric">Preço unitário</th>
            <th scope="col" class="table__numeric">Subtotal</th>
          </tr>
        </thead>
        <tbody>
          @for (item of items(); track item.id) {
            <tr>
              <td>{{ item.description }}</td>
              <td class="table__numeric">{{ item.quantity }}</td>
              <td class="table__numeric">{{ item.unitPrice | currency }}</td>
              <td class="table__numeric">{{ item.lineTotal | currency }}</td>
            </tr>
          }
        </tbody>
        <tfoot>
          <tr>
            <th scope="row" colspan="3" class="table__numeric">Total</th>
            <td class="table__numeric table__total">{{ total() | currency }}</td>
          </tr>
        </tfoot>
      </table>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OrderItemsTable {
  readonly items = input.required<OrderItem[]>();
  readonly total = input.required<number>();
}
