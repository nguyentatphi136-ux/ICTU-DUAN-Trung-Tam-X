// Gọi Maven Wrapper đúng tên theo hệ điều hành (mvnw.cmd trên Windows, mvnw trên macOS và Linux).
// Biến trong backend/.env (PORT, DB_URL, JAVA_HOME...) đã được node nạp sẵn và truyền xuống Maven.
const { spawnSync } = require('node:child_process');
const net = require('node:net');
const path = require('node:path');

const args = process.argv.slice(2);
const win = process.platform === 'win32';

function runMaven() {
  // Đường dẫn đầy đủ trong ngoặc kép: cmd.exe có thể không tìm lệnh ở thư mục hiện tại, và đường dẫn có thể chứa dấu cách.
  const wrapper = path.join(__dirname, win ? 'mvnw.cmd' : 'mvnw');
  const result = win
    ? // chcp 65001: cửa sổ lệnh hiện đúng tiếng Việt (log in ra UTF-8).
      spawnSync(`chcp 65001 >nul && "${wrapper}"`, args, { cwd: __dirname, stdio: 'inherit', shell: true })
    : spawnSync(wrapper, args, { cwd: __dirname, stdio: 'inherit' });
  process.exit(result.status ?? 1);
}

if (!args.includes('exec:exec')) runMaven();
else {
  // Chạy server: kiểm tra cổng trước, tránh chờ biên dịch xong mới báo lỗi lẫn trong log của Tomcat và Maven.
  const port = Number(process.env.PORT || 8080);
  const probe = net.createServer();
  probe.once('error', () => {
    console.error(`Cổng ${port} đang bị chương trình khác dùng (có thể là backend đang chạy ở cửa sổ khác).`);
    console.error('Tắt chương trình đó, hoặc đặt PORT khác trong backend/.env (mẫu: .env.example).');
    process.exit(1);
  });
  probe.listen(port, () => probe.close(runMaven));
}
