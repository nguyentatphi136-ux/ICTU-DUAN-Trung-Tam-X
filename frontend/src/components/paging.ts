// Phân trang phía máy khách, tách khỏi ui.tsx để kiểm thử được bằng Node.

/** Số trang hiện ra: trang đầu, cuối và hai trang quanh trang hiện tại; 0 là dấu "…". */
export function pageNumbers(page: number, pages: number): number[] {
  if (pages <= 7) return [...Array(pages)].map((_, i) => i + 1);
  const keep = [...new Set([1, page - 1, page, page + 1, pages])].filter((n) => n >= 1 && n <= pages).sort((a, b) => a - b);
  return keep.flatMap((n, i) => (i && n - keep[i - 1] > 1 ? [0, n] : [n]));
}

/** Chia trang danh sách phía máy khách. Khi có API: dùng ?page=&size= của máy chủ. */
export function paginate<T>(rows: T[], page: number, size = 20) {
  const pages = Math.max(1, Math.ceil(rows.length / size));
  const p = Math.min(page, pages);
  const from = (p - 1) * size;
  return { page: p, pages, items: rows.slice(from, from + size), from: rows.length ? from + 1 : 0, to: Math.min(from + size, rows.length) };
}
