// Đọc trang tính đầu tiên của tệp .xlsx hoặc tệp .csv thành bảng chuỗi, chạy hoàn toàn trong trình duyệt.
// .xlsx là tệp zip chứa XML: giải nén bằng DecompressionStream, đọc XML bằng DOMParser, không cần thư viện.
// ponytail: chỉ đọc giá trị ô (không đọc định dạng); ô ngày là số sê-ri Excel, mockImport đổi sang YYYY-MM-DD.

export async function readSheet(file: File): Promise<string[][]> {
  if (/\.csv$/i.test(file.name)) return parseCsv(await file.text());
  if (/\.xlsx$/i.test(file.name)) return readXlsx(new Uint8Array(await file.arrayBuffer()));
  throw new Error('Chỉ nhận tệp .xlsx hoặc .csv.');
}

/** Tách CSV theo dòng; hỗ trợ dấu phẩy hoặc chấm phẩy, giá trị trong ngoặc kép, BOM đầu tệp. */
export function parseCsv(text: string): string[][] {
  return text
    .replace(/^﻿/, '')
    .split(/\r?\n/)
    .map((line) => {
      const delimiter = line.includes(';') && !line.includes(',') ? ';' : ',';
      const cells: string[] = [];
      let cur = '';
      let quoted = false;
      for (let i = 0; i < line.length; i++) {
        const c = line[i];
        if (c === '"') {
          if (quoted && line[i + 1] === '"') {
            cur += '"';
            i++;
          } else quoted = !quoted;
        } else if (c === delimiter && !quoted) {
          cells.push(cur.trim());
          cur = '';
        } else cur += c;
      }
      cells.push(cur.trim());
      return cells;
    });
}

async function readXlsx(bytes: Uint8Array): Promise<string[][]> {
  const zip = unzipIndex(bytes);
  const xml = async (name: string) => {
    const entry = zip.get(name);
    return entry ? new DOMParser().parseFromString(await inflate(bytes, entry), 'application/xml') : null;
  };

  // Trang tính đầu tiên theo thứ tự trong workbook.xml, đường dẫn lấy từ workbook.xml.rels.
  const workbook = await xml('xl/workbook.xml');
  const rels = await xml('xl/_rels/workbook.xml.rels');
  const firstId = workbook?.getElementsByTagName('sheet')[0]?.getAttribute('r:id');
  const target = [...(rels?.getElementsByTagName('Relationship') ?? [])].find((r) => r.getAttribute('Id') === firstId)?.getAttribute('Target');
  const sheetPath = target ? 'xl/' + target.replace(/^\/?xl\//, '') : 'xl/worksheets/sheet1.xml';
  const sheet = await xml(sheetPath);
  if (!sheet) throw new Error('Không tìm thấy trang tính trong tệp .xlsx.');

  const shared = [...((await xml('xl/sharedStrings.xml'))?.getElementsByTagName('si') ?? [])].map((si) =>
    [...si.getElementsByTagName('t')].map((t) => t.textContent ?? '').join(''),
  );

  const table: string[][] = [];
  for (const row of sheet.getElementsByTagName('row')) {
    const cells: string[] = [];
    for (const c of row.getElementsByTagName('c')) {
      const col = columnIndex(c.getAttribute('r') ?? '') ?? cells.length;
      const type = c.getAttribute('t');
      const v = c.getElementsByTagName('v')[0]?.textContent ?? '';
      let value = v;
      if (type === 's') value = shared[Number(v)] ?? '';
      else if (type === 'inlineStr') value = [...c.getElementsByTagName('t')].map((t) => t.textContent ?? '').join('');
      else if (type === 'b') value = v === '1' ? 'TRUE' : 'FALSE';
      // Ô số Excel lưu dạng 9.80000001E8 hoặc 36924.0: số nguyên đổi về chuỗi chữ số (số điện thoại, ngày sê-ri).
      else if ((!type || type === 'n') && v && Number.isInteger(Number(v))) value = String(Number(v));
      while (cells.length < col) cells.push('');
      cells[col] = value.trim();
    }
    const r = Number(row.getAttribute('r'));
    if (r > 0) while (table.length < r - 1) table.push([]);
    table.push(cells);
  }
  return table;
}

/** "C12" -> 2 (cột C). */
function columnIndex(ref: string): number | null {
  const letters = /^[A-Z]+/.exec(ref)?.[0];
  if (!letters) return null;
  return [...letters].reduce((n, ch) => n * 26 + ch.charCodeAt(0) - 64, 0) - 1;
}

type ZipEntry = { offset: number; size: number; method: number };

/** Mục lục tệp zip đọc từ central directory ở cuối tệp. */
function unzipIndex(bytes: Uint8Array): Map<string, ZipEntry> {
  const view = new DataView(bytes.buffer, bytes.byteOffset, bytes.byteLength);
  let eocd = -1;
  for (let i = bytes.length - 22; i >= Math.max(0, bytes.length - 65557); i--) {
    if (view.getUint32(i, true) === 0x06054b50) {
      eocd = i;
      break;
    }
  }
  if (eocd < 0) throw new Error('Tệp .xlsx bị hỏng hoặc không phải tệp Excel.');
  const count = view.getUint16(eocd + 10, true);
  let p = view.getUint32(eocd + 16, true);
  const entries = new Map<string, ZipEntry>();
  const decoder = new TextDecoder();
  for (let i = 0; i < count; i++) {
    if (view.getUint32(p, true) !== 0x02014b50) break;
    const method = view.getUint16(p + 10, true);
    const size = view.getUint32(p + 20, true);
    const nameLen = view.getUint16(p + 28, true);
    const extraLen = view.getUint16(p + 30, true);
    const commentLen = view.getUint16(p + 32, true);
    const offset = view.getUint32(p + 42, true);
    entries.set(decoder.decode(bytes.subarray(p + 46, p + 46 + nameLen)), { offset, size, method });
    p += 46 + nameLen + extraLen + commentLen;
  }
  return entries;
}

async function inflate(bytes: Uint8Array, entry: ZipEntry): Promise<string> {
  const view = new DataView(bytes.buffer, bytes.byteOffset, bytes.byteLength);
  const start = entry.offset + 30 + view.getUint16(entry.offset + 26, true) + view.getUint16(entry.offset + 28, true);
  const data = bytes.slice(start, start + entry.size);
  if (entry.method === 0) return new TextDecoder().decode(data);
  if (entry.method !== 8) throw new Error('Tệp .xlsx dùng kiểu nén không hỗ trợ.');
  const stream = new Blob([data]).stream().pipeThrough(new DecompressionStream('deflate-raw'));
  return new Response(stream).text();
}
