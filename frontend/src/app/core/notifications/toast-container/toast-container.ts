import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { NgIcon, provideIcons } from '@ng-icons/core';
import { lucideX } from '@ng-icons/lucide';
import { NotificationService } from '../notification.service';

@Component({
  selector: 'app-toast-container',
  imports: [NgIcon],
  viewProviders: [provideIcons({ lucideX })],
  templateUrl: './toast-container.html',
  styleUrl: './toast-container.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ToastContainer {
  protected readonly notificationService = inject(NotificationService);
}
