import dayjs from 'dayjs';
import 'dayjs/locale/vi';
import relativeTime from 'dayjs/plugin/relativeTime';
import duration from 'dayjs/plugin/duration';

dayjs.extend(relativeTime);
dayjs.extend(duration);
dayjs.locale('vi');

/**
 * Format ISO date string to DD/MM/YYYY
 * formatDate("2026-05-27") → "27/05/2026"
 */
export function formatDate(date: string | Date | null | undefined): string {
  if (!date) return '—';
  return dayjs(date).format('DD/MM/YYYY');
}

/**
 * Format ISO datetime string to DD/MM/YYYY HH:mm
 * formatDateTime("2026-05-27T10:30:00Z") → "27/05/2026 10:30"
 */
export function formatDateTime(date: string | Date | null | undefined): string {
  if (!date) return '—';
  return dayjs(date).format('DD/MM/YYYY HH:mm');
}

/**
 * Relative time from now
 * fromNow("2026-05-25T10:00:00Z") → "2 ngày trước"
 */
export function fromNow(date: string | Date): string {
  return dayjs(date).fromNow();
}

/**
 * Days remaining until a future date
 * daysUntil("2026-06-01") → 5
 */
export function daysUntil(date: string | Date): number {
  return dayjs(date).diff(dayjs(), 'day');
}

/**
 * Days elapsed since a past date
 */
export function daysSince(date: string | Date): number {
  return dayjs().diff(dayjs(date), 'day');
}

/**
 * Check if a date is in the past
 */
export function isPast(date: string | Date): boolean {
  return dayjs(date).isBefore(dayjs());
}

/**
 * Check if a date is within N days from now
 */
export function isWithinDays(date: string | Date, days: number): boolean {
  const diff = daysUntil(date);
  return diff >= 0 && diff <= days;
}

/**
 * Format date range for display
 * formatDateRange("2026-01-01", "2026-06-01") → "01/01/2026 – 01/06/2026"
 */
export function formatDateRange(from: string, to: string): string {
  return `${formatDate(from)} – ${formatDate(to)}`;
}

export { dayjs };
