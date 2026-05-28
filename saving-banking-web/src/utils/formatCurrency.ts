/**
 * Format a number as Vietnamese Dong
 * formatVND(50000000)     → "50.000.000 ₫"
 * formatVND(50000000, false) → "50.000.000"
 */
export function formatVND(amount: number, showSymbol = true): string {
  const formatted = new Intl.NumberFormat('vi-VN').format(amount);
  return showSymbol ? `${formatted} ₫` : formatted;
}

/**
 * Format with explicit currency label
 * formatAmount(50000000, 'VND') → "50.000.000 VND"
 */
export function formatAmount(amount: number, currency = 'VND'): string {
  const formatted = new Intl.NumberFormat('vi-VN').format(amount);
  return `${formatted} ${currency}`;
}

/**
 * Parse formatted string back to number
 * parseVND("50.000.000") → 50000000
 */
export function parseVND(value: string): number {
  return Number(value.replace(/\./g, '').replace(/[^\d]/g, ''));
}

/**
 * Compact format for dashboard cards
 * compactAmount(1500000000) → "1,5 tỷ"
 * compactAmount(180000000)  → "180 triệu"
 */
export function compactAmount(amount: number): string {
  if (amount >= 1_000_000_000) {
    return `${(amount / 1_000_000_000).toFixed(1).replace('.', ',')} tỷ`;
  }
  if (amount >= 1_000_000) {
    return `${Math.round(amount / 1_000_000)} triệu`;
  }
  return formatVND(amount);
}
