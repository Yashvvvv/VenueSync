/**
 * Date utility functions for handling "wall clock" times
 * 
 * The app stores and displays times as "wall clock" times - meaning
 * if an event is at 2:00 PM, it shows as 2:00 PM regardless of the
 * viewer's timezone. This is the standard for event management apps.
 * 
 * Backend stores: LocalDateTime (no timezone)
 * Frontend receives: "2025-12-01T14:00:00" (ISO format, no Z suffix)
 * Frontend displays: The time as-is, no conversion
 */

/**
 * Parse a date string from the backend as a wall clock time.
 * This creates a Date object that represents the same wall clock time
 * regardless of the user's timezone.
 * 
 * @param dateString - Date string from backend (e.g., "2025-12-01T14:00:00")
 * @returns Date object representing the wall clock time
 */
export const parseWallClockDate = (dateString: string | Date | undefined): Date | undefined => {
  if (!dateString) return undefined;
  
  // If already a Date, return as-is
  if (dateString instanceof Date) return dateString;
  
  // Parse the ISO string components
  // Format: "2025-12-01T14:00:00" or "2025-12-01T14:00:00.000"
  const match = dateString.match(/^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2}):(\d{2})/);
  
  if (match) {
    const [, year, month, day, hours, minutes, seconds] = match;
    // Create date using local components (not UTC)
    return new Date(
      parseInt(year),
      parseInt(month) - 1, // JS months are 0-indexed
      parseInt(day),
      parseInt(hours),
      parseInt(minutes),
      parseInt(seconds)
    );
  }
  
  // Fallback: try standard Date parsing
  // This might cause timezone issues but is better than failing
  return new Date(dateString);
};

/**
 * Format a date for display. Uses the wall clock time interpretation.
 * 
 * @param dateString - Date string from backend
 * @param formatFn - date-fns format function
 * @param formatStr - Format string (e.g., "h:mm a", "EEE, MMM d")
 * @returns Formatted string
 */
export const formatWallClockDate = (
  dateString: string | Date | undefined,
  formatFn: (date: Date, formatStr: string) => string,
  formatStr: string
): string => {
  const date = parseWallClockDate(dateString);
  if (!date) return '';
  return formatFn(date, formatStr);
};

/**
 * Extract just the time portion from a date string
 * @param dateString - Date string (e.g., "2025-12-01T14:00:00")
 * @returns Time string (e.g., "14:00")
 */
export const extractTime = (dateString: string | undefined): string => {
  if (!dateString) return '';
  
  const match = dateString.match(/T(\d{2}):(\d{2})/);
  if (match) {
    return `${match[1]}:${match[2]}`;
  }
  
  // Fallback
  const date = parseWallClockDate(dateString);
  if (!date) return '';
  
  const hours = date.getHours().toString().padStart(2, '0');
  const minutes = date.getMinutes().toString().padStart(2, '0');
  return `${hours}:${minutes}`;
};

/**
 * Extract just the date portion (year, month, day) from a date string
 * @param dateString - Date string (e.g., "2025-12-01T14:00:00")
 * @returns Date object with only date components set
 */
export const extractDateOnly = (dateString: string | undefined): Date | undefined => {
  if (!dateString) return undefined;
  
  const match = dateString.match(/^(\d{4})-(\d{2})-(\d{2})/);
  if (match) {
    return new Date(parseInt(match[1]), parseInt(match[2]) - 1, parseInt(match[3]));
  }
  
  return parseWallClockDate(dateString);
};

/**
 * Combine a date and time into a LocalDateTime string for the backend
 * @param date - Date object (only date portion used)
 * @param time - Time string (e.g., "14:00")
 * @returns LocalDateTime string (e.g., "2025-12-01T14:00:00")
 */
export const combineToLocalDateTime = (date: Date, time: string): string => {
  const year = date.getFullYear();
  const month = (date.getMonth() + 1).toString().padStart(2, '0');
  const day = date.getDate().toString().padStart(2, '0');
  
  // Ensure time has proper format
  const [hours = '00', minutes = '00'] = time.split(':');
  
  return `${year}-${month}-${day}T${hours.padStart(2, '0')}:${minutes.padStart(2, '0')}:00`;
};
