import { Injectable, signal } from '@angular/core';

export type NotificationKind = 'success' | 'error' | 'info';

export interface AppNotification {
  id: number;
  kind: NotificationKind;
  message: string;
}

const DEFAULT_DURATION_MS = 4000;

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly _notifications = signal<AppNotification[]>([]);
  private nextId = 0;

  readonly notifications = this._notifications.asReadonly();

  success(message: string): void {
    this.push('success', message, DEFAULT_DURATION_MS);
  }

  error(message: string): void {
    this.push('error', message);
  }

  info(message: string): void {
    this.push('info', message, DEFAULT_DURATION_MS);
  }

  dismiss(id: number): void {
    this._notifications.update((list) => list.filter((notification) => notification.id !== id));
  }

  private push(kind: NotificationKind, message: string, durationMs?: number): void {
    const id = this.nextId++;
    this._notifications.update((list) => [...list, { id, kind, message }]);
    if (durationMs !== undefined) {
      setTimeout(() => this.dismiss(id), durationMs);
    }
  }
}
