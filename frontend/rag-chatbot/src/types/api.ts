export interface ApiResponse<T> {
  code: number;
  message: string;
  data?: T;
}

export interface ConversationResponse {
  id: string;
  title: string;
  userId: string;
  createdDate: string; // Using string for Instant, will be parsed as ISO date string
}

export interface PageResponse<T> {
  currentPage: number;
  totalPages: number;
  pageSize: number;
  totalElements: number;
  hasNextPage: boolean;
  hasPreviousPage: boolean;
  result: T[];
}
