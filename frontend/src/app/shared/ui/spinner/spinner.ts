import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'app-spinner',
  template: `
    <span class="spinner" aria-hidden="true"></span>
    @if (label(); as text) {
      <span class="spinner__label">{{ text }}</span>
    }
  `,
  styleUrl: './spinner.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    role: 'status',
    '[attr.data-size]': 'size()',
    '[attr.aria-label]': 'label() ?? "Carregando"',
  },
})
export class Spinner {
  readonly label = input<string>();
  readonly size = input<'sm' | 'md'>('md');
}
