import { HttpErrorResponse } from '@angular/common/http';

export interface ProblemDetail {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  errors?: FieldError[];
}

export interface FieldError {
  field: string;
  message: string;
}

export interface ApiError {
  status: number;
  message: string;
  fieldErrors: FieldError[];
}

export function toApiError(error: unknown): ApiError {
  if (!(error instanceof HttpErrorResponse)) {
    return { status: 0, message: 'Ocorreu um erro inesperado.', fieldErrors: [] };
  }

  if (error.status === 0) {
    return {
      status: 0,
      message: 'Não foi possível conectar ao servidor. Verifique sua conexão.',
      fieldErrors: [],
    };
  }

  const problem = isProblemDetail(error.error) ? error.error : null;
  return {
    status: error.status,
    message: problem?.detail ?? problem?.title ?? `Erro inesperado (HTTP ${error.status}).`,
    fieldErrors: problem?.errors ?? [],
  };
}

function isProblemDetail(body: unknown): body is ProblemDetail {
  return typeof body === 'object' && body !== null && ('title' in body || 'detail' in body);
}
