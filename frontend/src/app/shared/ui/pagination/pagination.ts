import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { NgIcon, provideIcons } from '@ng-icons/core';
import { lucideChevronLeft, lucideChevronRight } from '@ng-icons/lucide';

const MAX_VISIBLE_PAGES = 5;

@Component({
  selector: 'app-pagination',
  imports: [NgIcon],
  viewProviders: [provideIcons({ lucideChevronLeft, lucideChevronRight })],
  templateUrl: './pagination.html',
  styleUrl: './pagination.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Pagination {
  readonly page = input.required<number>();
  readonly size = input.required<number>();
  readonly totalElements = input.required<number>();
  readonly totalPages = input.required<number>();
  readonly pageSizeOptions = input<readonly number[]>([10, 20, 50]);

  readonly pageChange = output<number>();
  readonly sizeChange = output<number>();

  protected readonly rangeStart = computed(() =>
    this.totalElements() === 0 ? 0 : this.page() * this.size() + 1,
  );
  protected readonly rangeEnd = computed(() =>
    Math.min((this.page() + 1) * this.size(), this.totalElements()),
  );
  protected readonly isFirst = computed(() => this.page() <= 0);
  protected readonly isLast = computed(() => this.page() >= this.totalPages() - 1);

  protected readonly visiblePages = computed(() => {
    const total = this.totalPages();
    const start = Math.max(0, Math.min(this.page() - 2, total - MAX_VISIBLE_PAGES));
    const end = Math.min(total, start + MAX_VISIBLE_PAGES);
    return Array.from({ length: end - start }, (_, index) => start + index);
  });

  protected goTo(page: number): void {
    if (page < 0 || page >= this.totalPages() || page === this.page()) {
      return;
    }
    this.pageChange.emit(page);
  }

  protected onSizeChange(event: Event): void {
    this.sizeChange.emit(Number((event.target as HTMLSelectElement).value));
  }
}
