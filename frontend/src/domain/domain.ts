export interface ErrorResponse {
  error: string;
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
export const isErrorResponse = (obj: any): obj is ErrorResponse => {
  return (
    obj &&
    typeof obj === "object" &&
    "error" in obj &&
    typeof obj.error === "string"
  );
};

export enum EventStatusEnum {
  DRAFT = "DRAFT",
  PUBLISHED = "PUBLISHED",
  CANCELLED = "CANCELLED",
  COMPLETED = "COMPLETED",
}

export interface CreateTicketTypeRequest {
  name: string;
  price: number;
  description: string;
  totalAvailable?: number;
}

export interface CreateEventRequest {
  name: string;
  start?: string;
  end?: string;
  venue: string;
  salesStart?: string;
  salesEnd?: string;
  status: EventStatusEnum;
  ticketTypes: CreateTicketTypeRequest[];
}

export interface UpdateTicketTypeRequest {
  id: string | undefined;
  name: string;
  price: number;
  description: string;
  totalAvailable?: number;
}

export interface UpdateEventRequest {
  id: string;
  name: string;
  start?: string;
  end?: string;
  venue: string;
  salesStart?: string;
  salesEnd?: string;
  status: EventStatusEnum;
  ticketTypes: UpdateTicketTypeRequest[];
}

export interface TicketTypeSummary {
  id: string;
  name: string;
  price: number;
  description: string;
  totalAvailable?: number;
}

export interface EventSummary {
  id: string;
  name: string;
  start?: string;
  end?: string;
  venue: string;
  salesStart?: string;
  salesEnd?: string;
  status: EventStatusEnum;
  ticketTypes: TicketTypeSummary[];
}

export interface PublishedEventSummary {
  id: string;
  name: string;
  start?: string;
  end?: string;
  venue: string;
}

export interface TicketTypeDetails {
  id: string;
  name: string;
  price: number;
  description: string;
  totalAvailable?: number;
}

export interface EventDetails {
  id: string;
  name: string;
  start?: string;
  end?: string;
  venue: string;
  salesStart?: string;
  salesEnd?: string;
  status: EventStatusEnum;
  ticketTypes: TicketTypeDetails[];
  createdAt: string;
  updatedAt: string;
}

export interface SpringBootPagination<T> {
  content: T[]; // The actual data items for the current page
  pageable: {
    sort: {
      empty: boolean;
      sorted: boolean;
      unsorted: boolean;
    };
    offset: number;
    pageNumber: number;
    pageSize: number;
    paged: boolean;
    unpaged: boolean;
  };
  last: boolean; // Whether this is the last page
  totalElements: number; // Total number of items across all pages
  totalPages: number; // Total number of pages
  size: number; // Page size (items per page)
  number: number; // Current page number (zero-based)
  sort: {
    empty: boolean;
    sorted: boolean;
    unsorted: boolean;
  };
  first: boolean; // Whether this is the first page
  numberOfElements: number; // Number of items in the current page
  empty: boolean; // Whether the current page has no items
}

export interface PublishedEventTicketTypeDetails {
  id: string;
  name: string;
  price: number;
  description: string;
}

export interface PublishedEventDetails {
  id: string;
  name: string;
  start?: string;
  end?: string;
  venue: string;
  ticketTypes: PublishedEventTicketTypeDetails[];
}

export enum TicketStatus {
  PURCHASED = "PURCHASED",
  USED = "USED",
  EXPIRED = "EXPIRED",
  CANCELLED = "CANCELLED",
}

export interface TicketSummaryTicketType {
  id: string;
  name: string;
  price: number;
}

export interface TicketSummary {
  id: string;
  status: TicketStatus;
  ticketType: TicketSummaryTicketType;
  eventName: string;
  eventStart?: string;
  eventEnd?: string;
}

export interface TicketDetails {
  id: string;
  status: TicketStatus;
  price: number;
  description: string;
  eventName: string;
  eventVenue: string;
  eventStart: string;
  eventEnd: string;
}

export enum TicketValidationMethod {
  QR_SCAN = "QR_SCAN",
  MANUAL = "MANUAL",
}

export enum TicketValidationStatus {
  VALID = "VALID",
  INVALID = "INVALID",
  EXPIRED = "EXPIRED",
  ALREADY_USED = "ALREADY_USED",
}

export interface TicketValidationRequest {
  id: string;
  method: TicketValidationMethod;
}

export interface TicketValidationResponse {
  ticketId: string;
  status: TicketValidationStatus;
}
