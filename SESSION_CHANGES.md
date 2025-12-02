# Session Changes Summary - December 2025

This document outlines all the changes, improvements, and implementations made during the development sessions for the Event Management Platform.

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
4. **Event completion** could be automated based on end date (similar to ticket expiration)
5. **Ticket Cancellation** - `CANCELLED` status exists but cancellation feature not yet implemented:
   - No cancel ticket API endpoint
   - No cancel button in UI
   - No refund logic or cancellation policy

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
