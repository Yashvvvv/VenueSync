# Session Changes Summary — December 2025 – March 2026

This document outlines all the changes, improvements, and implementations made during the development sessions for VenueSync (formerly Event Management Platform).

> **Last updated:** March 9, 2026

---

## Table of Contents

1. [Ticket Status Tracking System](#1-ticket-status-tracking-system)
2. [Timezone Handling - Wall Clock Time Implementation](#2-timezone-handling---wall-clock-time-implementation)
3. [Sales Period Validation](#3-sales-period-validation)
4. [UI Improvements](#4-ui-improvements)
5. [Organizer Event Filtering](#5-organizer-event-filtering)
6. [Database Constraint Fix](#6-database-constraint-fix)
7. [How to Test](#7-how-to-test)
8. [QR Code Validation Error Handling Improvements](#8-qr-code-validation-error-handling-improvements)
9. [Auto-Refresh Feature for Ticket View Page](#9-auto-refresh-feature-for-ticket-view-page)
10. [Data Cleanup Operations](#10-data-cleanup-operations)
11. [JVM Timezone Configuration Fix](#11-jvm-timezone-configuration-fix)
12. [Null Ticket Availability Fix](#12-null-ticket-availability-fix)
13. [Automatic Event Completion Feature](#13-automatic-event-completion-feature)
14. [PostgreSQL Timezone Configuration](#14-postgresql-timezone-configuration)
15. [Dev Profile Cleanup](#15-dev-profile-cleanup)
16. [Git Repository Configuration](#16-git-repository-configuration)
17. [PostgreSQL Persistent Volume Configuration](#17-postgresql-persistent-volume-configuration)
18. [Scheduled Task Interval Optimization](#18-scheduled-task-interval-optimization)
19. [Frontend Expired Ticket Display Fix](#19-frontend-expired-ticket-display-fix)
20. [Future Work / Roadmap](#20-future-work--roadmap)
21. [Unit & Integration Testing](#21-unit--integration-testing)

---

## 1. Ticket Status Tracking System

### Overview
Implemented a complete ticket lifecycle management system with automatic expiration and status tracking.

### Ticket Status Lifecycle

The system tracks tickets through their entire lifecycle:

```
[Buy Ticket] → PURCHASED → [Scan QR at Event] → USED
                    ↓
              [Event Ends without scanning]
                    ↓
                EXPIRED
                    
              [User/Admin cancels]
                    ↓
               CANCELLED
```

### Status Definitions

| Status | When Set | Meaning | UI Display |
|--------|----------|---------|------------|
| `PURCHASED` | After buying ticket | Ticket is valid, ready to use | "Active" (green) in Upcoming tab |
| `USED` | After QR scan at venue | Attendee entered, can't reuse | "Used" (blue) in Past tab |
| `EXPIRED` | Scheduled job after event ends | Event over, ticket never scanned | "Expired" (yellow) in Past tab |
| `CANCELLED` | When ticket is cancelled/refunded | Ticket invalidated | "Cancelled" (red) in Past tab |

### Backend Changes

#### `TicketStatusEnum.java`
- **Full enum values**: `PURCHASED`, `USED`, `EXPIRED`, `CANCELLED`

#### `TicketRepository.java`
Added new query methods:
```java
// Find active tickets (PURCHASED status, event not ended)
Page<Ticket> findActiveTickets(UUID attendeeId, LocalDateTime now, Pageable pageable);

// Find past tickets (USED, EXPIRED, or event ended)
Page<Ticket> findPastTickets(UUID attendeeId, LocalDateTime now, Pageable pageable);

// Count tickets by status
long countByAttendeeIdAndStatus(UUID attendeeId, TicketStatusEnum status);

// Find tickets for expiration job
List<Ticket> findTicketsToExpire(LocalDateTime now);
```

#### `TicketValidationServiceImpl.java`
- Updated to mark tickets as `USED` when scanned/validated
- Added validation to prevent re-scanning already used tickets

#### `TicketExpirationService.java` (NEW)
```java
@Service
public class TicketExpirationService {
    @Transactional
    public int expireTickets() {
        // Finds PURCHASED tickets where event has ended
        // Updates status to EXPIRED
        // Returns count of expired tickets
    }
}
```

#### `ScheduledTasksConfig.java` (NEW)
```java
@Configuration
@EnableScheduling
public class ScheduledTasksConfig {
    @Scheduled(cron = "0 0 * * * *") // Every hour
    public void expireTickets() {
        // Runs ticket expiration job
    }
}
```

#### `TicketController.java`
- Added `filter` query parameter: `?filter=active` or `?filter=past`
- Returns filtered tickets based on event end time and status

### Frontend Changes

#### `domain.ts`
```typescript
export enum TicketStatus {
  PURCHASED = "PURCHASED",
  USED = "USED",
  EXPIRED = "EXPIRED",
  CANCELLED = "CANCELLED",
}
```

#### `api.ts`
```typescript
export const listTickets = async (
  accessToken: string,
  page: number,
  filter?: "active" | "past",  // NEW parameter
): Promise<SpringBootPagination<TicketSummary>>
```

#### `dashboard-list-tickets.tsx`
- Added **Upcoming** and **Past** tabs with Radix UI Tabs
- Stats summary showing counts for each category
- Status badges on ticket cards
- Color-coded status indicators

#### `ticket-card.tsx`
- Added status badge display with icons and colors for each status:
  - `PURCHASED` → Green "Active" badge with Ticket icon
  - `USED` → Blue "Used" badge with CheckCircle icon
  - `EXPIRED` → Yellow "Expired" badge with Clock icon
  - `CANCELLED` → Red "Cancelled" badge with XCircle icon
- Event information display (name, venue, date)
- Visual differentiation between active and past tickets (opacity change)

#### `dashboard-view-ticket-page.tsx`
- Added status display on ticket detail page with descriptions:
  - `PURCHASED` → "Present this QR code at the venue for entry"
  - `USED` → "This ticket has already been scanned at the venue"
  - `EXPIRED` → "This ticket has expired as the event has ended"
  - `CANCELLED` → "This ticket has been cancelled"

---

## 2. Timezone Handling - Wall Clock Time Implementation

### Overview
Implemented simplified "wall clock" time approach. Event times are stored and displayed **exactly as the user enters them** - no timezone conversion anywhere in the system.

### The Problem (Previous UTC Approach)
- Frontend converted local time to UTC when saving events (using `toISOString()`)
- Backend was configured with `spring.jackson.time-zone=UTC`
- When loading, dates were parsed incorrectly causing wrong display
- **Example**: User enters 2:00 PM IST → Saved as 8:30 AM UTC → Displayed as 8:30 AM (wrong!)

### The Solution: Simplified Wall Clock Time

**Core Principle**: Store times without timezone information. "2:00 PM" means 2:00 PM, period. No conversions.

### How It Works Now

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        SIMPLIFIED TIME FLOW                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  1. USER ENTERS          2. FRONTEND SENDS       3. DATABASE STORES         │
│     "2:00 PM"     →      "2025-12-01T14:00:00"  →  2025-12-01 14:00:00      │
│                               (no 'Z' suffix)        (TIMESTAMP)            │
│                                                                              │
│  4. BACKEND RETURNS      5. FRONTEND DISPLAYS                               │
│     "2025-12-01T14:00:00" →  "2:00 PM"                                      │
│                               (parses as local)                              │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Code Changes Summary

#### Frontend Changes

| File | Change |
|------|--------|
| `date-utils.ts` | **NEW** - Utility functions for wall clock time parsing |
| `domain.ts` | Changed date types from `Date` to `string` |
| `api.ts` | Simplified serialization (no UTC conversion) |
| `dashboard-manage-event-page.tsx` | Removed `toISOString()`, use direct string formatting |
| `event-hero.tsx` | Use `parseWallClockDate()` |
| `event-card.tsx` | Use `parseWallClockDate()` |
| `published-event-card.tsx` | Use `parseWallClockDate()` |
| `ticket-card.tsx` | Use `parseWallClockDate()` |
| `dashboard-view-ticket-page.tsx` | Use `parseWallClockDate()` |
| `dashboard-list-events-page.tsx` | Use `parseWallClockDate()` |

#### Backend Changes

| File | Change |
|------|--------|
| `application.properties` | Removed `spring.jackson.time-zone=UTC` |
| `TicketTypeServiceImpl.java` | Changed from `LocalDateTime.now(ZoneOffset.UTC)` to `LocalDateTime.now()` |

### New Date Utility (`date-utils.ts`)

```typescript
/**
 * Parse a date string from the backend as a wall clock time.
 * This creates a Date object that represents the same wall clock time
 * regardless of the user's timezone.
 */
export const parseWallClockDate = (dateString: string | Date | undefined): Date | undefined => {
  if (!dateString) return undefined;
  if (dateString instanceof Date) return dateString;
  
  // Parse ISO string components: "2025-12-01T14:00:00"
  const match = dateString.match(/^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2}):(\d{2})/);
  
  if (match) {
    const [, year, month, day, hours, minutes, seconds] = match;
    // Create date using LOCAL components (not UTC)
    return new Date(
      parseInt(year),
      parseInt(month) - 1,  // JS months are 0-indexed
      parseInt(day),
      parseInt(hours),
      parseInt(minutes),
      parseInt(seconds)
    );
  }
  
  return new Date(dateString);
};
```

### When This Approach Works ✅

| Scenario | Works? | Explanation |
|----------|--------|-------------|
| Server in India, Events in India | ✅ Yes | Same timezone, times match perfectly |
| Server in US, Events in US | ✅ Yes | Same timezone, times match perfectly |
| Regional app (single country) | ✅ Yes | All users and events share timezone |

### When This Approach Has Issues ⚠️

| Scenario | Issue |
|----------|-------|
| Server in US, Events in India | Times compared incorrectly |
| Global app with multi-timezone events | Need timezone-aware implementation |

### Summary

| Aspect | Before | After |
|--------|--------|-------|
| Frontend saves | `date.toISOString()` (UTC) | `"YYYY-MM-DDTHH:mm:00"` (local) |
| Backend config | `spring.jackson.time-zone=UTC` | No timezone config |
| Backend comparison | `LocalDateTime.now(ZoneOffset.UTC)` | `LocalDateTime.now()` |
| Frontend parses | `new Date(string)` (browser interprets) | `parseWallClockDate()` (explicit) |
| Complexity | High (conversions everywhere) | Low (no conversions) |

---

## 3. Sales Period Validation

### Overview
Added proper validation to prevent ticket purchases outside the sales period.

### Backend Changes

#### `SalesPeriodException.java` (NEW)
```java
public class SalesPeriodException extends RuntimeException {
    public SalesPeriodException(String message) {
        super(message);
    }
}
```

#### `TicketTypeServiceImpl.java`
Added validation in `purchaseTicket()`:
```java
// Updated to use system local time (wall clock approach - see Section 11)
LocalDateTime now = LocalDateTime.now();

if (now.isBefore(ticketType.getEvent().getSalesStart())) {
    throw new SalesPeriodException("Ticket sales have not started yet");
}

if (now.isAfter(ticketType.getEvent().getSalesEnd())) {
    throw new SalesPeriodException("Ticket sales have ended");
}

if (now.isAfter(ticketType.getEvent().getEnd())) {
    throw new SalesPeriodException("This event has already ended");
}
```

#### `GlobalExceptionHandler.java`
Added handler for `SalesPeriodException`:
```java
@ExceptionHandler(SalesPeriodException.class)
public ResponseEntity<ErrorResponse> handleSalesPeriodException(SalesPeriodException ex) {
    return new ResponseEntity<>(
        new ErrorResponse(ex.getMessage()),
        HttpStatus.BAD_REQUEST
    );
}
```

---

## 4. UI Improvements

### Toggle/Switch Component Darkening

#### `switch.tsx`
Changed from light gray to darker styling:
```typescript
// Before
"data-[state=unchecked]:bg-input"

// After  
"data-[state=unchecked]:bg-zinc-700"
```

### Ticket Card Cutout Styling Fix

#### `dashboard-view-ticket-page.tsx`
Fixed the decorative cutout circles on ticket cards to properly blend with background.

---

## 5. Organizer Event Filtering

### Overview
Implemented event filtering by status (Draft, Published, Cancelled, Completed) for organizers, similar to the attendee ticket tabs.

### Backend Changes

#### `EventRepository.java`
```java
Page<Event> findByOrganizerIdAndStatus(UUID organizerId, EventStatusEnum status, Pageable pageable);
long countByOrganizerIdAndStatus(UUID organizerId, EventStatusEnum status);
```

#### `EventService.java`
```java
Page<Event> listEventsForOrganizerByStatus(UUID organizerId, EventStatusEnum status, Pageable pageable);
long countEventsForOrganizerByStatus(UUID organizerId, EventStatusEnum status);
```

#### `EventServiceImpl.java`
Implemented the service methods calling the repository.

#### `EventController.java`
```java
@GetMapping
public ResponseEntity<Page<ListEventResponseDto>> listEvents(
    @AuthenticationPrincipal Jwt jwt, 
    @RequestParam(required = false) String status,  // NEW
    Pageable pageable
) {
    // Filters by status if provided
}

@GetMapping(path = "/counts")  // NEW ENDPOINT
public ResponseEntity<Map<String, Long>> getEventCounts(
    @AuthenticationPrincipal Jwt jwt
) {
    // Returns count for each status
}
```

### Frontend Changes

#### `api.ts`
```typescript
export const listEvents = async (
  accessToken: string,
  page: number,
  status?: string,  // NEW parameter
): Promise<SpringBootPagination<EventSummary>>

export interface EventCounts {
  draft: number;
  published: number;
  cancelled: number;
  completed: number;
}

export const getEventCounts = async (accessToken: string): Promise<EventCounts>
```

#### `dashboard-list-events-page.tsx`
Complete redesign with:
- **Stats Summary Cards**: Visual count of events by status
- **Tab Navigation**: All Events, Draft, Published, Completed, Cancelled
- **Badge Counts**: Number indicators on each tab
- **Status Icons**: FileEdit, Globe, CheckCircle2, XCircle
- **3-Column Grid**: Better use of screen space
- **Automatic Page Reset**: When switching tabs

---

## 6. Database Constraint Fix

### Problem
When validating a QR code, the backend tried to update ticket status from `PURCHASED` to `USED`, but PostgreSQL threw a constraint violation error:

```
ERROR: new row for relation "tickets" violates check constraint "tickets_status_check"
Detail: Failing row contains (..., USED, ...)
```

### Root Cause
The PostgreSQL database had a check constraint (`tickets_status_check`) created when the database was first initialized. This constraint only allowed the original `PURCHASED` value. When we added `USED`, `EXPIRED`, and `CANCELLED` to the Java enum, the database constraint wasn't automatically updated.

### Solution
Updated the database constraint via SQL command:

```sql
ALTER TABLE tickets DROP CONSTRAINT IF EXISTS tickets_status_check;
ALTER TABLE tickets ADD CONSTRAINT tickets_status_check 
  CHECK (status IN ('PURCHASED', 'USED', 'EXPIRED', 'CANCELLED'));
```

### Command Used
```powershell
docker exec backend-db-1 psql -U postgres -d postgres -c "ALTER TABLE tickets DROP CONSTRAINT IF EXISTS tickets_status_check; ALTER TABLE tickets ADD CONSTRAINT tickets_status_check CHECK (status IN ('PURCHASED', 'USED', 'EXPIRED', 'CANCELLED'));"
```

### Important Note
When adding new enum values that are persisted to the database, remember to update:
1. Java enum (`TicketStatusEnum.java`)
2. Frontend enum (`domain.ts`)
3. **Database constraints** (if using CHECK constraints)

---

## 7. How to Test

### Prerequisites
1. Docker running (for PostgreSQL and Keycloak)
2. Backend running on port 8080
3. Frontend running on port 5173

### Starting the Application

```powershell
# Terminal 1: Start Docker containers
cd backend
docker-compose up -d

# Terminal 2: Start backend
cd backend
.\mvnw spring-boot:run

# Terminal 3: Start frontend
cd frontend
npm run dev
```

### Testing Ticket Status Tracking

1. **Login as Attendee** (purchase tickets)
2. **Purchase a ticket** for an upcoming event
3. **Navigate to Dashboard > My Tickets**
4. **Verify tabs**: Should see "Upcoming" and "Past" tabs
5. **Check Upcoming tab**: Shows tickets for future events with PURCHASED status
6. **Check Past tab**: Shows used/expired tickets or tickets for past events

### Testing Ticket Validation (Status Change to USED)

1. **Login as Organizer**
2. **Go to Dashboard > Validate QR**
3. **Scan a valid ticket QR code**
4. **Verify**: Ticket status changes from PURCHASED to USED
5. **Try scanning again**: Should show error (already used)

### Testing Timezone Handling

1. **Create a new event** as organizer
2. **Set sales period**: Note the times you enter
3. **Save and reload** the event
4. **Verify**: Times display correctly in your local timezone
5. **Check backend logs**: Timestamps should be in UTC

### Testing Sales Period Validation

1. **Create an event** with:
   - Sales Start: Future date/time
   - Sales End: After sales start
   - Event End: After sales end
2. **Try purchasing a ticket** before sales start
3. **Expected**: "Ticket sales have not started yet" error
4. **Wait until sales start** (or modify dates)
5. **Purchase ticket**: Should succeed
6. **Modify sales end to past**
7. **Try purchasing**: "Ticket sales have ended" error

### Testing Organizer Event Filtering

1. **Login as Organizer**
2. **Go to Dashboard > My Events**
3. **Verify Stats Cards**: Shows count of Draft, Published, Completed, Cancelled
4. **Click each tab**:
   - **All Events**: Shows all events
   - **Draft**: Only draft events
   - **Published**: Only published events
   - **Completed**: Only completed events
   - **Cancelled**: Only cancelled events
5. **Create events** with different statuses to test filtering
6. **Delete an event**: Counts should update automatically

### Testing Toggle/Switch Styling

1. **Navigate to any form** with a toggle switch
2. **Verify**: Unchecked state is dark gray (not light gray)
3. **Toggle on/off**: Smooth transition between states

---

## Files Modified

### Backend Files
| File | Change Type |
|------|-------------|
| `TicketStatusEnum.java` | Modified |
| `TicketRepository.java` | Modified |
| `TicketValidationServiceImpl.java` | Modified |
| `TicketExpirationService.java` | **NEW** |
| `ScheduledTasksConfig.java` | **NEW** |
| `TicketController.java` | Modified |
| `SalesPeriodException.java` | **NEW** |
| `TicketTypeServiceImpl.java` | Modified |
| `GlobalExceptionHandler.java` | Modified |
| `application.properties` | Modified |
| `EventRepository.java` | Modified |
| `EventService.java` | Modified |
| `EventServiceImpl.java` | Modified |
| `EventController.java` | Modified |
| `TicketValidationRequestDto.java` | Modified (id: UUID → String) |
| `TicketValidationController.java` | Modified (UUID parsing with error handling) |

### Frontend Files
| File | Change Type |
|------|-------------|
| `domain.ts` | Modified (Date → string types) |
| `api.ts` | Modified (simplified serialization) |
| `date-utils.ts` | **NEW** (wall clock time utilities) |
| `dashboard-list-tickets.tsx` | Modified |
| `ticket-card.tsx` | Modified (use parseWallClockDate) |
| `dashboard-view-ticket-page.tsx` | Modified (auto-refresh, parseWallClockDate) |
| `switch.tsx` | Modified |
| `dashboard-list-events-page.tsx` | Modified (use parseWallClockDate) |
| `dashboard-manage-event-page.tsx` | Modified (wall clock time handling) |
| `event-hero.tsx` | Modified (use parseWallClockDate) |
| `event-card.tsx` | Modified (use parseWallClockDate) |
| `published-event-card.tsx` | Modified (use parseWallClockDate) |
| `tsconfig.json` | **NEW** |

### Database Changes
| Table | Change |
|-------|--------|
| `tickets` | Updated `tickets_status_check` constraint to include USED, EXPIRED, CANCELLED |
| `ticket_validations` | Updated `ticket_validations_status_check` constraint to include EXPIRED, ALREADY_USED |

---

## API Endpoints Changed/Added

| Endpoint | Method | Change |
|----------|--------|--------|
| `/api/v1/tickets` | GET | Added `filter` query param |
| `/api/v1/events` | GET | Added `status` query param |
| `/api/v1/events/counts` | GET | **NEW** - Returns event counts by status |

---

## 8. QR Code Validation Error Handling Improvements

### Overview
Fixed 500 Internal Server Error when validating QR codes for various edge cases including re-scanning already used tickets, expired tickets, and invalid/malformed QR codes.

### Problem
When rescanning a used QR code or validating expired/invalid tickets, the backend threw exceptions that resulted in 500 Internal Server Error responses instead of returning proper validation status.

### Root Cause
1. **Database constraint violation**: The `ticket_validations` table had a CHECK constraint that only allowed `'VALID'` and `'INVALID'` status values
2. **Exception-based error handling**: Service methods threw exceptions for not-found QR codes/tickets instead of returning INVALID status
3. **UUID parsing failure**: Invalid UUID format in request body caused deserialization errors before reaching controller logic

### Solution

#### Database Constraint Update
Updated the `ticket_validations_status_check` constraint to include all validation statuses:

```sql
ALTER TABLE ticket_validations DROP CONSTRAINT IF EXISTS ticket_validations_status_check;
ALTER TABLE ticket_validations ADD CONSTRAINT ticket_validations_status_check 
  CHECK (status IN ('VALID', 'INVALID', 'EXPIRED', 'ALREADY_USED'));
```

#### Backend Changes

##### `TicketValidationRequestDto.java`
Changed `id` field type from `UUID` to `String` to allow graceful handling of malformed UUIDs:
```java
// Before
private UUID id;

// After
private String id;
```

##### `TicketValidationController.java`
Added UUID parsing with try-catch to return INVALID status for malformed UUIDs:
```java
@PostMapping
public ResponseEntity<TicketValidationResponseDto> validateTicket(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody TicketValidationRequestDto ticketValidationRequestDto) {
    
    UUID ticketId;
    try {
        ticketId = UUID.fromString(ticketValidationRequestDto.getId());
    } catch (IllegalArgumentException e) {
        // Invalid UUID format - return INVALID status
        TicketValidation invalidValidation = TicketValidation.builder()
                .validationStatus(TicketValidationStatusEnum.INVALID)
                .validatedAt(LocalDateTime.now(ZoneOffset.UTC))
                .build();
        return ResponseEntity.ok(ticketValidationMapper.toDto(invalidValidation));
    }
    
    // ... rest of validation logic
}
```

##### `TicketValidationServiceImpl.java`
Modified to return INVALID status instead of throwing exceptions:

**For QR code validation (`validateQrCode` method):**
```java
// Before: threw IllegalArgumentException for not-found QR codes
QrCode qrCode = qrCodeRepository.findByCode(request.getCode())
    .orElseThrow(() -> new IllegalArgumentException("QR Code not found"));

// After: returns INVALID validation for not-found QR codes
Optional<QrCode> qrCodeOpt = qrCodeRepository.findByCode(request.getCode());
if (qrCodeOpt.isEmpty()) {
    return ticketValidationRepository.save(
        TicketValidation.builder()
            .validationStatus(TicketValidationStatusEnum.INVALID)
            .validatedAt(LocalDateTime.now(ZoneOffset.UTC))
            .build()
    );
}
```

**For ticket ID validation (`validateTicketById` method):**
```java
// Before: threw IllegalArgumentException for not-found tickets
Ticket ticket = ticketRepository.findById(ticketId)
    .orElseThrow(() -> new IllegalArgumentException("Ticket not found"));

// After: returns INVALID validation for not-found tickets
Optional<Ticket> ticketOpt = ticketRepository.findById(ticketId);
if (ticketOpt.isEmpty()) {
    return ticketValidationRepository.save(
        TicketValidation.builder()
            .validationStatus(TicketValidationStatusEnum.INVALID)
            .validatedAt(LocalDateTime.now(ZoneOffset.UTC))
            .build()
    );
}
```

### Validation Status Responses

| Scenario | HTTP Status | Validation Status | UI Message |
|----------|-------------|-------------------|------------|
| Valid ticket, first scan | 200 OK | `VALID` | "Ticket Valid" (green) |
| Already scanned ticket | 200 OK | `ALREADY_USED` | "Already Used" (orange) |
| Expired event ticket | 200 OK | `EXPIRED` | "Ticket Expired" (red) |
| Non-existent QR code/ticket | 200 OK | `INVALID` | "Invalid" (red) |
| Malformed UUID | 200 OK | `INVALID` | "Invalid" (red) |

---

## 9. Auto-Refresh Feature for Ticket View Page

### Overview
Added automatic refresh functionality to the attendee ticket view page so ticket status updates in real-time after being scanned at the venue.

### Problem
When an organizer scanned a ticket QR code, the attendee viewing their ticket had to manually refresh the page to see the updated status (PURCHASED → USED).

### Solution

#### Frontend Changes

##### `dashboard-view-ticket-page.tsx`
Added auto-refresh with 10-second interval and manual refresh button:

```typescript
const AUTO_REFRESH_INTERVAL = 10000; // 10 seconds

const [isRefreshing, setIsRefreshing] = useState(false);
const [lastUpdated, setLastUpdated] = useState<Date>(new Date());

// Memoized fetch function
const fetchTicketData = useCallback(async () => {
  if (!ticketId || !auth?.accessToken) return;
  
  setIsRefreshing(true);
  try {
    const data = await getTicket(auth.accessToken, ticketId);
    setTicket(data);
    setLastUpdated(new Date());
  } catch (error) {
    console.error('Error refreshing ticket:', error);
  } finally {
    setIsRefreshing(false);
  }
}, [ticketId, auth?.accessToken]);

// Auto-refresh interval
useEffect(() => {
  const intervalId = setInterval(fetchTicketData, AUTO_REFRESH_INTERVAL);
  return () => clearInterval(intervalId);
}, [fetchTicketData]);
```

**Manual Refresh Button:**
```tsx
<Button 
  variant="outline" 
  size="sm" 
  onClick={fetchTicketData}
  disabled={isRefreshing}
  className="flex items-center gap-2"
>
  <RefreshCw className={`h-4 w-4 ${isRefreshing ? 'animate-spin' : ''}`} />
  Refresh
</Button>
```

**Last Updated Timestamp:**
```tsx
<span className="text-xs text-zinc-500">
  Last updated: {lastUpdated.toLocaleTimeString()}
</span>
```

### Features
- **Auto-refresh every 10 seconds**: Ticket status polls backend automatically
- **Manual refresh button**: Users can refresh on-demand
- **Spinning animation**: RefreshCw icon spins during refresh
- **Last updated timestamp**: Shows when data was last fetched
- **Disabled during refresh**: Prevents multiple simultaneous requests

---

## 10. Data Cleanup Operations

### Attendee Ticket Data Flush
Deleted all tickets for the attendee user to allow fresh testing:

```sql
DELETE FROM ticket_validations WHERE ticket_id IN 
  (SELECT id FROM tickets WHERE purchaser_id = '92e81060-b4b1-4521-90e5-4bafa5187d56');
DELETE FROM qr_codes WHERE ticket_id IN 
  (SELECT id FROM tickets WHERE purchaser_id = '92e81060-b4b1-4521-90e5-4bafa5187d56');
DELETE FROM tickets WHERE purchaser_id = '92e81060-b4b1-4521-90e5-4bafa5187d56';
```

**Results:**
- 61 ticket validations deleted
- 13 QR codes deleted
- 13 tickets deleted

---

## 11. JVM Timezone Configuration Fix

### Problem
The JVM was defaulting to UTC timezone, causing sales period validation to fail even when the current time was within the sales window.

**Example Issue:**
- User's local time: 21:30 IST (India Standard Time)
- Sales Start: 21:30 (stored in database)
- JVM `LocalDateTime.now()`: 16:00 UTC
- Comparison: 16:00 < 21:30 → "Ticket sales have not started yet" ❌

### Root Cause
The `TicketsApplication.java` main class had:
```java
System.setProperty("user.timezone", "UTC");
```

This forced all `LocalDateTime.now()` calls to return UTC time, not local time.

### Solution

#### 1. Updated `TicketsApplication.java`
```java
public static void main(String[] args) {
  // Set timezone to match where events are happening (wall clock time approach)
  TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
  System.setProperty("user.timezone", "Asia/Kolkata");
  
  SpringApplication.run(TicketsApplication.class, args);
}
```

#### 2. Added `TimezoneConfig.java` (NEW)
```java
@Configuration
@Slf4j
public class TimezoneConfig {
    @Value("${app.timezone:Asia/Kolkata}")
    private String timezone;

    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone(timezone));
        log.info("JVM timezone set to: {} ({})", timezone, TimeZone.getDefault().getDisplayName());
    }
}
```

#### 3. Updated `application.properties`
```properties
# Application Timezone - IMPORTANT for wall clock time approach
app.timezone=Asia/Kolkata
```

### Result
- JVM now runs in `Asia/Kolkata` timezone
- `LocalDateTime.now()` returns correct local time
- Sales period validation works correctly
- Log timestamps show `+05:30` offset instead of `Z` (UTC)

### Important Note
**Change `Asia/Kolkata` to match your deployment region!** The wall clock time approach requires the JVM timezone to match where events are happening.

---

## 12. Null Ticket Availability Fix

### Problem
When a ticket type had `totalAvailable = null` (meaning unlimited tickets), the purchase would fail with a 500 error due to NullPointerException.

### Root Cause
In `TicketTypeServiceImpl.java`:
```java
Integer totalAvailable = ticketType.getTotalAvailable();
if(purchasedTickets + 1 > totalAvailable) {  // NPE when totalAvailable is null!
  throw new TicketsSoldOutException();
}
```

### Solution
Added null check to treat null as "unlimited":
```java
Integer totalAvailable = ticketType.getTotalAvailable();
// If totalAvailable is null, it means unlimited tickets
if(totalAvailable != null && purchasedTickets + 1 > totalAvailable) {
  throw new TicketsSoldOutException();
}
```

### Result
- Ticket types with no limit (null `totalAvailable`) now work correctly
- Unlimited tickets can be purchased without error

---

## Known Issues / Future Improvements

1. **Duplicate H2 dependency** in `pom.xml` - Causes warning but doesn't affect functionality
2. **Ticket expiration job** runs hourly - Consider making configurable
3. **Past tickets** could have a "Download PDF" feature for receipts
4. ~~**Event completion** could be automated based on end date (similar to ticket expiration)~~ ✅ **IMPLEMENTED** (Section 13)
5. **Ticket Cancellation** - `CANCELLED` status exists but cancellation feature not yet implemented:
   - No cancel ticket API endpoint
   - No cancel button in UI
   - No refund logic or cancellation policy

---

## Files Modified Summary (All Sessions)

### Backend Files
| File | Change Type |
|------|-------------|
| `TicketStatusEnum.java` | Modified |
| `TicketRepository.java` | Modified |
| `TicketValidationServiceImpl.java` | Modified |
| `TicketExpirationService.java` | **NEW** |
| `ScheduledTasksConfig.java` | **NEW** → Modified |
| `TicketController.java` | Modified |
| `SalesPeriodException.java` | **NEW** |
| `TicketTypeServiceImpl.java` | Modified |
| `GlobalExceptionHandler.java` | Modified |
| `application.properties` | Modified |
| `EventRepository.java` | Modified |
| `EventService.java` | Modified |
| `EventServiceImpl.java` | Modified |
| `EventController.java` | Modified |
| `TicketValidationRequestDto.java` | Modified |
| `TicketValidationController.java` | Modified |
| `TicketsApplication.java` | Modified (timezone) |
| `TimezoneConfig.java` | **NEW** |
| `EventStatusService.java` | **NEW** |
| `EventStatusServiceImpl.java` | **NEW** |
| `docker-compose.yml` | Modified (TZ, PGTZ) |
| `SecurityConfig.java` | Modified (removed @Profile) |

### Frontend Files
| File | Change Type |
|------|-------------|
| `domain.ts` | Modified |
| `api.ts` | Modified |
| `date-utils.ts` | **NEW** |
| `dashboard-list-tickets.tsx` | Modified |
| `ticket-card.tsx` | Modified |
| `dashboard-view-ticket-page.tsx` | Modified |
| `switch.tsx` | Modified |
| `dashboard-list-events-page.tsx` | Modified |
| `dashboard-manage-event-page.tsx` | Modified |
| `event-hero.tsx` | Modified |
| `event-card.tsx` | Modified |
| `published-event-card.tsx` | Modified |
| `tsconfig.json` | **NEW** |

### Configuration Files
| File | Change Type |
|------|-------------|
| `.gitignore` | **NEW** |
| `docker-compose.yml` | Modified |
| `application.properties` | Modified |

### Deleted Files
| File | Reason |
|------|--------|
| `DevSecurityConfig.java` | Dev profile cleanup |
| `DevDataInitializer.java` | Dev profile cleanup |
| `application-dev.properties` | Dev profile cleanup |

### Database Changes
| Table | Change |
|-------|--------|
| `tickets` | Updated `tickets_status_check` constraint |
| `ticket_validations` | Updated `ticket_validations_status_check` constraint |

---

## Ticket Cancellation - Not Yet Implemented

The `CANCELLED` status is defined in both backend and frontend, with UI styling ready, but the actual cancellation feature is **not yet implemented**.

### What Exists ✅
- `CANCELLED` in `TicketStatusEnum.java` (backend)
- `CANCELLED` in `TicketStatus` enum (frontend `domain.ts`)
- UI badge styling for cancelled tickets (red badge with XCircle icon)
- Database constraint updated to allow `CANCELLED`
- Cancelled tickets appear in "Past" tab

### What's Missing ❌
- **Cancel Ticket Endpoint**: No `POST /api/v1/tickets/{id}/cancel` API
- **Cancel Button in UI**: No way for users to cancel their tickets
- **Refund Logic**: No business logic for refunds
- **Cancellation Policy**: No rules (e.g., can't cancel 24hrs before event)
- **Organizer Cancel**: No way for organizers to cancel tickets

### To Implement Later
1. Backend: Add `cancelTicket()` method in `TicketService`
2. Backend: Add `POST /api/v1/tickets/{id}/cancel` endpoint
3. Backend: Add cancellation validation rules
4. Frontend: Add "Cancel Ticket" button on ticket detail page
5. Frontend: Add confirmation dialog
6. Frontend: Handle success/error states

---

## 13. Automatic Event Completion Feature

### Overview
Implemented automatic status transition for events: when an event's end date passes, its status automatically changes from `PUBLISHED` to `COMPLETED`. This eliminates the need for organizers to manually mark events as completed.

### Problem
Previously, events remained in `PUBLISHED` status indefinitely even after they ended. The organizer's "Test" event with a past end date was still showing as "Published" instead of "Completed".

### Solution
Created a new service that runs as a scheduled task to automatically complete ended events.

### Backend Changes

#### `EventStatusService.java` (NEW)
Interface for managing automatic event status transitions:
```java
public interface EventStatusService {
  /**
   * Automatically marks PUBLISHED events as COMPLETED when their event_end date has passed.
   * @return the number of events that were marked as completed
   */
  int completeEndedEvents();
}
```

#### `EventStatusServiceImpl.java` (NEW)
Implementation that uses the repository to bulk-update ended events:
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class EventStatusServiceImpl implements EventStatusService {

  private final EventRepository eventRepository;

  @Override
  @Transactional
  public int completeEndedEvents() {
    LocalDateTime now = LocalDateTime.now();
    log.debug("Checking for PUBLISHED events that have ended (event_end < {})", now);
    
    int completedCount = eventRepository.completeEndedEvents(
        EventStatusEnum.COMPLETED,
        EventStatusEnum.PUBLISHED,
        now
    );
    
    if (completedCount > 0) {
      log.info("Automatically completed {} events that have ended", completedCount);
    }
    
    return completedCount;
  }
}
```

#### `EventRepository.java` (UPDATED)
Added new `@Modifying` query for bulk event completion:
```java
/**
 * Automatically marks PUBLISHED events as COMPLETED when their event_end date has passed.
 * Only affects events with status = PUBLISHED and event_end < now.
 */
@Modifying
@Query("UPDATE Event e SET e.status = :newStatus, e.updatedAt = :now " +
       "WHERE e.status = :currentStatus AND e.end < :now AND e.end IS NOT NULL")
int completeEndedEvents(
    @Param("newStatus") EventStatusEnum newStatus,
    @Param("currentStatus") EventStatusEnum currentStatus,
    @Param("now") LocalDateTime now
);
```

#### `ScheduledTasksConfig.java` (UPDATED)
Added new scheduled task for event completion:
```java
@Configuration
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class ScheduledTasksConfig {

  private final TicketExpirationService ticketExpirationService;
  private final EventStatusService eventStatusService;  // NEW

  @Scheduled(fixedRate = 3600000, initialDelay = 10000) // Every hour
  public void expireTicketsTask() {
    // ... existing ticket expiration logic
  }

  @Scheduled(fixedRate = 3600000, initialDelay = 10000) // Every hour, 10 seconds after startup
  public void completeEndedEventsTask() {  // NEW
    log.debug("Running scheduled event completion task...");
    try {
      eventStatusService.completeEndedEvents();
    } catch (Exception e) {
      log.error("Error during scheduled event completion", e);
    }
  }
}
```

### How It Works

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    AUTOMATIC EVENT COMPLETION FLOW                          │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  1. Scheduled Task Runs          2. Query Checks                            │
│     (Every hour +                   SELECT events WHERE                     │
│      10 sec after startup)          status = 'PUBLISHED' AND                │
│              ↓                       event_end < NOW()                      │
│                                                                              │
│  3. Bulk Update                  4. Result                                  │
│     UPDATE events                   - Events marked COMPLETED               │
│     SET status = 'COMPLETED'        - updatedAt timestamp set               │
│     WHERE ...                       - Log shows count of updates            │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Event Lifecycle Flow (Updated)

```
                    ┌─────────────┐
   Create Event  →  │   DRAFT     │
                    └──────┬──────┘
                           │ Publish
                           ↓
                    ┌─────────────┐
                    │  PUBLISHED  │  ←─── Tickets can be purchased
                    └──────┬──────┘
                           │
           ┌───────────────┼───────────────┐
           │               │               │
           ↓               ↓               ↓
   ┌─────────────┐  ┌─────────────┐  ┌─────────────┐
   │  CANCELLED  │  │  COMPLETED  │  │  COMPLETED  │
   │  (manual)   │  │  (manual)   │  │(AUTOMATIC)  │
   └─────────────┘  └─────────────┘  └─────────────┘
                                           ↑
                                    Scheduled task
                                    when event_end
                                    has passed
```

### Scheduling Details

| Parameter | Value | Description |
|-----------|-------|-------------|
| `fixedRate` | 3600000ms (1 hour) | How often the task runs |
| `initialDelay` | 10000ms (10 seconds) | Delay after app startup before first run |

### Files Created/Modified

| File | Type | Description |
|------|------|-------------|
| `EventStatusService.java` | **NEW** | Interface for event status management |
| `EventStatusServiceImpl.java` | **NEW** | Implementation with completion logic |
| `EventRepository.java` | Modified | Added `completeEndedEvents()` query |
| `ScheduledTasksConfig.java` | Modified | Added `completeEndedEventsTask()` |

---

## 14. PostgreSQL Timezone Configuration

### Overview
Configured PostgreSQL container to use `Asia/Kolkata` timezone to match the JVM timezone for consistent wall clock time handling.

### Problem
While the JVM was configured to use `Asia/Kolkata` timezone (Section 11), PostgreSQL was still using UTC. This could cause inconsistencies when:
- Comparing timestamps between application and database
- PostgreSQL functions using `NOW()` or `CURRENT_TIMESTAMP`
- Viewing timestamps directly in database tools

### Solution
Added timezone environment variables to `docker-compose.yml`:

```yaml
services:
  db:
    image: postgres:latest
    ports:
      - "5433:5432"
    restart: always
    environment:
      POSTGRES_PASSWORD: changemeinprod!
      TZ: Asia/Kolkata        # NEW - System timezone
      PGTZ: Asia/Kolkata      # NEW - PostgreSQL timezone
```

### Environment Variables Explained

| Variable | Purpose |
|----------|---------|
| `TZ` | Sets the system timezone for the container |
| `PGTZ` | Sets PostgreSQL's default timezone for timestamp operations |

### Result
- `NOW()` in PostgreSQL returns time in `Asia/Kolkata`
- Timestamps display correctly in Adminer (database UI)
- Full consistency between JVM and database time

### Important Note
**After making this change, restart the PostgreSQL container:**
```powershell
cd backend
docker-compose down
docker-compose up -d
```

---

## 15. Dev Profile Cleanup

### Overview
Removed all development profile configurations to simplify the codebase and use only production-ready configurations.

### Files Deleted

| File | Purpose (before deletion) |
|------|---------------------------|
| `DevSecurityConfig.java` | Disabled security in dev profile |
| `DevDataInitializer.java` | Seeded test data in dev profile |
| `application-dev.properties` | Dev-specific property overrides |

### Changes Made

#### `SecurityConfig.java` (UPDATED)
Removed the `@Profile("!dev")` annotation that was added to avoid bean conflicts:
```java
// Before
@Configuration
@EnableWebSecurity
@Profile("!dev")  // ← REMOVED
public class SecurityConfig { ... }

// After
@Configuration
@EnableWebSecurity
public class SecurityConfig { ... }
```

### Result
- Single, unified configuration for all environments
- No risk of accidentally running with disabled security
- Cleaner codebase without unused dev configurations

### Important Note
The application now **always** requires proper Keycloak authentication. Make sure:
1. Keycloak container is running (`docker-compose up -d`)
2. Keycloak is configured with the correct realm and clients
3. Valid OAuth2 tokens are used for API requests

---

## 16. Git Repository Configuration

### Overview
Created a comprehensive `.gitignore` file to exclude build artifacts and IDE files from version control.

### Problem
After running `mvnw clean install`, the `target/` directory was regenerated with ~10,000+ compiled `.class` files. VS Code's Source Control showed these as untracked files.

### Solution
Created `.gitignore` at the repository root:

```gitignore
# Backend - Maven/Java
backend/target/
backend/*.jar
backend/*.war
backend/*.class
backend/.mvn/
backend/*.log

# Frontend - Node.js
frontend/node_modules/
frontend/dist/
frontend/.vite/

# Root level node_modules (if any)
node_modules/
package-lock.json
package.json

# IDE
.idea/
*.iml
.vscode/
*.swp
*.swo

# OS
.DS_Store
Thumbs.db

# Environment files
.env
.env.local
.env.*.local

# Logs
*.log
logs/
```

### Categories Excluded

| Category | Patterns | Reason |
|----------|----------|--------|
| Maven Build | `backend/target/` | Compiled classes, JAR files |
| Node.js | `node_modules/`, `dist/` | Dependencies, build output |
| IDE Files | `.idea/`, `.vscode/`, `*.iml` | Personal IDE settings |
| OS Files | `.DS_Store`, `Thumbs.db` | System-generated files |
| Environment | `.env*` | Secrets and local configs |
| Logs | `*.log`, `logs/` | Runtime log files |

### Git Status After .gitignore
```powershell
git status
# On branch main
# nothing to commit, working tree clean
```

The `target/` folder and all its contents are now properly ignored.

---

## 17. PostgreSQL Persistent Volume Configuration

### Overview
Added persistent volume for PostgreSQL to prevent data loss when containers are restarted or recreated.

### Problem
When running `docker-compose down` or recreating containers (e.g., after changing PostgreSQL version), all database data was lost because it was stored only inside the container's ephemeral filesystem.

### Solution
Added a named Docker volume for PostgreSQL data in `docker-compose.yml`:

```yaml
services:
  db:
    image: postgres:latest
    ports:
      - "5433:5432"
    restart: always
    environment:
      POSTGRES_PASSWORD: changemeinprod!
      TZ: Asia/Kolkata
      PGTZ: Asia/Kolkata
    volumes:
      - postgres-data:/var/lib/postgresql  # Persistent volume

volumes:
  keycloak-data:
    driver: local
  postgres-data:
    driver: local
```

### PostgreSQL 18+ Compatibility Note
PostgreSQL 18+ changed its data directory structure. The volume mount path must be `/var/lib/postgresql` (not `/var/lib/postgresql/data`) for compatibility with the latest PostgreSQL images.

### Result
- Database data persists across container restarts
- `docker-compose down` no longer deletes data
- Only `docker-compose down -v` (with `-v` flag) removes volumes

### Important Warning
⚠️ **Keycloak data is also stored in a volume.** Running `docker-compose down -v` will delete:
- All PostgreSQL data (events, tickets, users)
- All Keycloak data (realm config, users, clients)

---

## 18. Scheduled Task Interval Optimization

### Overview
Reduced the scheduled task interval from 1 hour to 5 minutes for faster automatic status updates.

### Problem
Events and tickets were taking up to 1 hour to be automatically marked as COMPLETED/EXPIRED after their end time passed.

### Solution
Updated `ScheduledTasksConfig.java`:

```java
// Before: Every hour (3600000ms)
@Scheduled(fixedRate = 3600000, initialDelay = 10000)

// After: Every 5 minutes (300000ms)
@Scheduled(fixedRate = 300000, initialDelay = 10000)
```

### Updated Intervals

| Task | Before | After | Purpose |
|------|--------|-------|---------|
| `expireTicketsTask()` | 1 hour | 5 minutes | Mark unused tickets as EXPIRED |
| `completeEndedEventsTask()` | 1 hour | 5 minutes | Mark ended events as COMPLETED |

### Performance Impact
The impact is **negligible** because:
- Queries are lightweight (indexed columns, bulk updates)
- Only touches rows that need updating (usually 0-2)
- Runs in milliseconds

| Interval | Runs per day | Server Load |
|----------|--------------|-------------|
| 1 hour | 24 | Minimal |
| 5 minutes | 288 | Still minimal |

---

## 19. Frontend Expired Ticket Display Fix

### Overview
Fixed an issue where tickets for ended events still showed "Active" status in the attendee dashboard instead of "Expired".

### Problem
When a ticket's event had ended but the backend scheduled task hadn't run yet to update the ticket status from `PURCHASED` to `EXPIRED`, the frontend displayed "Active" badge instead of "Expired".

### Solution
Updated `ticket-card.tsx` to check the event end date on the frontend and display "Expired" visually even before the backend updates the status:

```typescript
export const TicketCard: React.FC<TicketCardProps> = ({ ticket, index = 0 }) => {
  // Check if event has ended (for visual display purposes)
  const eventEnd = ticket.eventEnd ? parseWallClockDate(ticket.eventEnd) : null
  const isEventEnded = eventEnd ? eventEnd < new Date() : false
  
  // Determine the display status:
  // - If ticket is PURCHASED but event has ended, show as "Expired" visually
  // - Otherwise, use the actual ticket status
  const displayStatus = (ticket.status === TicketStatus.PURCHASED && isEventEnded) 
    ? TicketStatus.EXPIRED 
    : ticket.status
  
  const status = statusConfig[displayStatus]
  const isPast = displayStatus === TicketStatus.USED || 
                 displayStatus === TicketStatus.EXPIRED || 
                 displayStatus === TicketStatus.CANCELLED
  
  // ... rest of component
}
```

### Result
- Tickets for ended events immediately show "Expired" badge
- No need to wait for backend scheduled task
- Better user experience with real-time status display

---

## 20. Future Work / Roadmap

### High Priority 🔴

#### 1. External Events API Integration
**Goal**: Display external events from third-party APIs alongside platform events.

**Tasks**:
- Research and select external event APIs (Eventbrite, Ticketmaster, Meetup, etc.)
- Create `ExternalEventService` to fetch and cache external events
- Add `ExternalEvent` model/DTO for normalized event data
- Create API endpoint: `GET /api/v1/events/external`
- Update frontend Discover page to show mixed results
- Add "External" badge to distinguish external events
- Handle deep linking to external ticketing pages

**Technical Considerations**:
- API rate limiting and caching strategy
- Data mapping from different API formats
- Error handling for API unavailability

#### 2. Multi-User Authentication System
**Goal**: Move beyond 3 hardcoded users (organizer, attendee, staff) to support unlimited user registration.

**Tasks**:
- Enable Keycloak self-registration
- Add role management in Keycloak admin
- Create user profile page (view/edit profile)
- Add user settings (notification preferences, etc.)
- Implement email verification flow
- Add password reset functionality
- Consider social login (Google, GitHub, etc.)

**Keycloak Configuration Needed**:
```
Realm Settings → Login:
  - User registration: ON
  - Email as username: Optional
  - Edit username: OFF
  - Forgot password: ON
  - Remember me: ON
  - Verify email: ON
```

### Medium Priority 🟡

#### 3. Ticket Cancellation Feature
**Status**: CANCELLED enum exists, UI styling ready, logic not implemented

**Tasks**:
- Backend: Add `cancelTicket()` method in `TicketService`
- Backend: Add `POST /api/v1/tickets/{id}/cancel` endpoint
- Backend: Add cancellation policy validation (e.g., no cancel 24hrs before event)
- Frontend: Add "Cancel Ticket" button on ticket detail page
- Frontend: Add confirmation dialog with policy info
- Consider: Refund logic integration

#### 4. PDF Ticket Download
**Goal**: Allow attendees to download tickets as PDF for offline use.

**Tasks**:
- Add PDF generation library (iText, OpenPDF, or similar)
- Create PDF template with event details, QR code, attendee info
- Add download button on ticket view page
- Consider email delivery of PDF tickets

#### 5. Event Search and Filters
**Goal**: Enhanced event discovery with search and filtering.

**Tasks**:
- Add search bar to Discover page
- Filter by: date range, location/venue, price range, category
- Sort by: date, popularity, price
- Add event categories/tags system

### Low Priority 🟢

#### 6. Organizer Analytics Dashboard
- Event views and ticket sales charts
- Revenue reports
- Attendee demographics
- Popular event times

#### 7. Email Notifications
- Ticket purchase confirmation
- Event reminders (24hrs, 1hr before)
- Event updates/changes
- Ticket transfer notifications

#### 8. Staff Management
- Organizer can assign staff to events
- Staff-specific QR scanning interface
- Staff activity logs

#### 9. Multi-Timezone Support
- Store event timezone with event data
- Convert times for users in different timezones
- Display timezone info on event pages

#### 10. Payment Integration
- Stripe/PayPal integration
- Paid ticket purchases
- Refund processing
- Payment history

---

## Known Issues

1. **Duplicate H2 dependency** in `pom.xml` - Causes Maven warning but doesn't affect functionality
2. **PageImpl serialization warning** - Spring Data warns about unstable JSON structure for paginated responses
3. **Keycloak data loss on `docker-compose down -v`** - User configuration needs to be re-done after volume deletion

---

## Files Modified Summary (All Sessions)

### Backend Files
| File | Change Type |
|------|-------------|
| `TicketStatusEnum.java` | Modified |
| `TicketRepository.java` | Modified |
| `TicketValidationServiceImpl.java` | Modified |
| `TicketExpirationService.java` | **NEW** |
| `ScheduledTasksConfig.java` | **NEW** → Modified (5 min interval) |
| `TicketController.java` | Modified |
| `SalesPeriodException.java` | **NEW** |
| `TicketTypeServiceImpl.java` | Modified |
| `GlobalExceptionHandler.java` | Modified |
| `application.properties` | Modified |
| `EventRepository.java` | Modified |
| `EventService.java` | Modified |
| `EventServiceImpl.java` | Modified |
| `EventController.java` | Modified |
| `TicketValidationRequestDto.java` | Modified |
| `TicketValidationController.java` | Modified |
| `TicketsApplication.java` | Modified (timezone) |
| `TimezoneConfig.java` | **NEW** |
| `EventStatusService.java` | **NEW** |
| `EventStatusServiceImpl.java` | **NEW** |
| `docker-compose.yml` | Modified (TZ, PGTZ, volumes) |
| `SecurityConfig.java` | Modified (removed @Profile) |

### Frontend Files
| File | Change Type |
|------|-------------|
| `domain.ts` | Modified |
| `api.ts` | Modified |
| `date-utils.ts` | **NEW** |
| `dashboard-list-tickets.tsx` | Modified |
| `ticket-card.tsx` | Modified (expired display fix) |
| `dashboard-view-ticket-page.tsx` | Modified |
| `switch.tsx` | Modified |
| `dashboard-list-events-page.tsx` | Modified |
| `dashboard-manage-event-page.tsx` | Modified |
| `event-hero.tsx` | Modified |
| `event-card.tsx` | Modified |
| `published-event-card.tsx` | Modified |
| `tsconfig.json` | **NEW** |

### Configuration Files
| File | Change Type |
|------|-------------|
| `.gitignore` | **NEW** |
| `docker-compose.yml` | Modified (volumes, timezone) |
| `application.properties` | Modified |

### Deleted Files
| File | Reason |
|------|--------|
| `DevSecurityConfig.java` | Dev profile cleanup |
| `DevDataInitializer.java` | Dev profile cleanup |
| `application-dev.properties` | Dev profile cleanup |

### Database Changes
| Table | Change |
|-------|--------|
| `tickets` | Updated `tickets_status_check` constraint |
| `ticket_validations` | Updated `ticket_validations_status_check` constraint |

---

## Current System Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          EVENT MANAGEMENT PLATFORM                          │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌─────────────┐     ┌─────────────┐     ┌─────────────┐                   │
│  │   Frontend  │────▶│   Backend   │────▶│  PostgreSQL │                   │
│  │  React/Vite │     │ Spring Boot │     │    :5433    │                   │
│  │    :5173    │     │    :8080    │     │             │                   │
│  └─────────────┘     └──────┬──────┘     └─────────────┘                   │
│                              │                                               │
│                              │ OAuth2                                        │
│                              ▼                                               │
│                       ┌─────────────┐                                       │
│                       │  Keycloak   │                                       │
│                       │    :9090    │                                       │
│                       └─────────────┘                                       │
│                                                                              │
│  Scheduled Tasks (Every 5 minutes):                                         │
│  • expireTicketsTask() - Mark unused tickets as EXPIRED                     │
│  • completeEndedEventsTask() - Mark ended events as COMPLETED               │
│                                                                              │
│  Docker Volumes:                                                             │
│  • postgres-data - PostgreSQL data persistence                              │
│  • keycloak-data - Keycloak realm/user persistence                          │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 21. Unit & Integration Testing

### Overview
A comprehensive test suite was added covering all service implementations and all controller endpoints.

### Test Files Added

| File | Type | Coverage |
|------|------|----------|
| `EventServiceImplTest.java` | Unit | CRUD, search, ownership checks, countByStatus |
| `EventStatusServiceImplTest.java` | Unit | Auto-completion scheduler logic |
| `TicketServiceImplTest.java` | Unit | listActiveTickets, listPastTickets, getTicket |
| `TicketTypeServiceImplTest.java` | Unit | purchaseTicket with pessimistic locking |
| `TicketExpirationServiceImplTest.java` | Unit | Bulk expiration logic |
| `QrCodeServiceImplTest.java` | Unit | QR generation and decoding (ZXing) |
| `TicketValidationServiceImplTest.java` | Unit | QR scan + manual validation status assertions |
| `EventControllerTest.java` | Integration | `@WebMvcTest` — all organizer endpoints |
| `PublishedEventControllerTest.java` | Integration | `@WebMvcTest` — public event endpoints |
| `TicketControllerTest.java` | Integration | `@WebMvcTest` — attendee ticket endpoints |
| `TicketTypeControllerTest.java` | Integration | `@WebMvcTest` — ticket type/purchase endpoints |
| `TicketValidationControllerTest.java` | Integration | `@WebMvcTest` — staff validation endpoint |

### Test Configuration
- **Unit tests** use Mockito (`@ExtendWith(MockitoExtension.class)`) with `@MockBean` for all dependencies
- **Controller tests** use `@WebMvcTest` + `MockMvc`, H2 in-memory DB, and `@WithMockUser` / custom security test configs for role-based endpoint testing
- `src/test/resources/application.properties` overrides datasource to H2

---

## Quick Reference Commands

```powershell
# Start all Docker containers
cd backend
docker-compose up -d

# Start backend
cd backend
.\mvnw.cmd spring-boot:run

# Start frontend
cd frontend
npm run dev

# View PostgreSQL logs
docker logs backend-db-1

# Access database directly
docker exec -it backend-db-1 psql -U postgres -d postgres

# Manually complete ended events
docker exec backend-db-1 psql -U postgres -d postgres -c "UPDATE events SET status = 'COMPLETED', updated_at = NOW() WHERE status = 'PUBLISHED' AND event_end < NOW() AND event_end IS NOT NULL;"

# Remove all data (CAUTION!)
docker-compose down -v
```
