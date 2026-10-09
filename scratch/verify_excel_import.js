const fs = require('fs');
const testCsv = fs.readFileSync('scratch/test_import_users.csv', 'utf8');

const lines = testCsv.split(/\r?\n/).filter(l => l.trim().length > 0);
console.log('Total lines in test CSV:', lines.length);

const parseLine = (line) => {
  const result = [];
  let cur = '';
  let inQuotes = false;
  const delimiter = line.includes(';') && !line.includes(',') ? ';' : ',';
  for (let i = 0; i < line.length; i++) {
    const char = line[i];
    if (char === '"') {
      if (inQuotes && line[i + 1] === '"') { cur += '"'; i++; }
      else { inQuotes = !inQuotes; }
    } else if (char === delimiter && !inQuotes) {
      result.push(cur.trim());
      cur = '';
    } else {
      cur += char;
    }
  }
  result.push(cur.trim());
  return result;
};

const rows = [];
for (let i = 1; i < lines.length; i++) {
  const parts = parseLine(lines[i]);
  rows.push({
    fullName: parts[0] || '',
    email: parts[1] || '',
    phone: parts[2] || '',
    role: parts[3] || 'student'
  });
}

console.log('Parsed rows count:', rows.length);

const vnImportPhoneRegex = /^(0|\+84)(3|5|7|8|9)[0-9]{8}$/;
const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const validRoles = ['student', 'instructor', 'ta', 'training-manager', 'admissions', 'accountant', 'admin'];

let validCount = 0;
let errorCount = 0;

rows.forEach((r, idx) => {
  const errors = [];
  if (!r.fullName) errors.push('Thiếu họ và tên');
  if (!r.email) errors.push('Email không được để trống');
  else if (!emailRegex.test(r.email)) errors.push('Định dạng email không hợp lệ');
  if (r.phone && !vnImportPhoneRegex.test(r.phone)) errors.push('SĐT không đúng định dạng VN');
  if (!validRoles.includes(r.role)) errors.push('Vai trò không hợp lệ');

  const isValid = errors.length === 0;
  if (isValid) validCount++; else errorCount++;
  console.log(`Row ${idx + 1}: ${isValid ? '✓ HỢP LỆ' : '✗ LỖI (' + errors.join(', ') + ')'}`);
});

console.log(`\n=> KẾT QUẢ: Hợp lệ: ${validCount}, Lỗi bị bỏ qua: ${errorCount} (Tổng: ${rows.length})`);
