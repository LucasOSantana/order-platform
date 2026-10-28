import { InjectionToken } from '@angular/core';

// Prefixo da API
export const API_BASE_URL = new InjectionToken<string>('API_BASE_URL', {
  providedIn: 'root',
  factory: () => '/api',
});
