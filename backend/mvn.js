// Gọi Maven Wrapper đúng tên theo hệ điều hành (mvnw.cmd trên Windows, mvnw trên macOS và Linux).
// Biến trong backend/.env (PORT, DB_URL, JAVA_HOME...) đã được node nạp sẵn và truyền xuống Maven.
const { spawnSync } = require('node:child_process');
const path = require('node:path');

const win = process.platform === 'win32';
// Đường dẫn đầy đủ trong ngoặc kép: cmd.exe có thể không tìm lệnh ở thư mục hiện tại, và đường dẫn có thể chứa dấu cách.
const wrapper = path.join(__dirname, win ? 'mvnw.cmd' : 'mvnw');
const result = win
  ? // chcp 65001: cửa sổ lệnh hiện đúng tiếng Việt (log in ra UTF-8).
    spawnSync(`chcp 65001 >nul && "${wrapper}"`, process.argv.slice(2), { cwd: __dirname, stdio: 'inherit', shell: true })
  : spawnSync(wrapper, process.argv.slice(2), { cwd: __dirname, stdio: 'inherit' });
process.exit(result.status ?? 1);
