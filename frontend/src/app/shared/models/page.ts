export interface Page<T> {
  content: T[];
  page: PageMetadata;
}

export interface PageMetadata {
  size: number;
  number: number;
  totalElements: number;
  totalPages: number;
}

export type SortDirection = 'asc' | 'desc';

export interface SortState<TField extends string> {
  field: TField;
  direction: SortDirection;
}
