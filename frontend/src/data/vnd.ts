// Tiền Việt Nam: hiển thị "28.000.000 ₫", đọc từ ô nhập có dấu chấm, và đọc số thành chữ cho biên lai (S7-01, S7-03).

export const vnd = (n: number) => Math.round(n).toLocaleString('vi-VN') + ' ₫';
/** "28.000.000" hoặc "28000000" thành số; rỗng là NaN. */
export const parseVnd = (s: string) => (s.replace(/[.\s₫]/g, '') === '' ? NaN : Number(s.replace(/[.\s₫]/g, '')));
/** Định dạng lại khi đang gõ: chỉ giữ chữ số, thêm dấu chấm phân cách hàng nghìn. */
export const typingVnd = (s: string) => {
  const d = s.replace(/\D/g, '').replace(/^0+(?=\d)/, '');
  return d ? Number(d).toLocaleString('vi-VN') : '';
};

const DIGITS = ['không', 'một', 'hai', 'ba', 'bốn', 'năm', 'sáu', 'bảy', 'tám', 'chín'];

/** Đọc một nhóm ba chữ số. full = true khi nhóm này không phải nhóm đầu (cần đọc "không trăm", "lẻ"). */
function group(n: number, full: boolean) {
  const [h, t, u] = [Math.floor(n / 100), Math.floor((n % 100) / 10), n % 10];
  const out: string[] = [];
  if (h || full) out.push(DIGITS[h], 'trăm');
  if (t > 1) {
    out.push(DIGITS[t], 'mươi');
    if (u === 1) out.push('mốt');
    else if (u === 5) out.push('lăm');
    else if (u === 4) out.push('tư');
    else if (u) out.push(DIGITS[u]);
  } else if (t === 1) {
    out.push('mười');
    if (u === 5) out.push('lăm');
    else if (u) out.push(DIGITS[u]);
  } else if (u) {
    if (h || full) out.push('lẻ');
    out.push(DIGITS[u]);
  }
  return out.join(' ');
}

/** 3000000 thành "Ba triệu đồng chẵn"; 10080000 thành "Mười triệu không trăm tám mươi nghìn đồng". */
export function vndWords(n: number) {
  n = Math.round(n);
  if (n === 0) return 'Không đồng';
  const units = ['', ' nghìn', ' triệu', ' tỷ'];
  const groups: number[] = [];
  for (let x = n; x > 0; x = Math.floor(x / 1000)) groups.push(x % 1000);
  const words: string[] = [];
  for (let i = groups.length - 1; i >= 0; i--) {
    if (!groups[i]) continue;
    words.push(group(groups[i], i < groups.length - 1) + units[i % 4] + (i >= 4 ? ' tỷ' : ''));
  }
  const s = words.join(' ') + ' đồng' + (n % 1000 === 0 && n % 1000000 === 0 ? ' chẵn' : '');
  return s.charAt(0).toUpperCase() + s.slice(1);
}
