import { useRef, useState, type DragEvent } from 'react';
import { Link } from 'react-router-dom';
import { AppLayout } from '../../components/AppLayout';
import { Icon } from '../../components/Icon';
import { Box, Pill } from '../../components/ui';
import { previewRows, type ImportRow } from './mockImport';
import './import.css';

const STEPS = ['Tải tệp lên', 'Xem trước và kiểm lỗi', 'Kết quả'];
const COLUMNS = ['Họ tên', 'Email', 'Số điện thoại', 'Vai trò'];
const SHOW = ['Tất cả dòng', 'Chỉ dòng lỗi', 'Chỉ dòng hợp lệ'];

/** Tải một tệp văn bản do trình duyệt tạo ra (tệp mẫu, báo cáo lỗi). */
function download(name: string, rows: string[][]) {
  const csv = '﻿' + rows.map((r) => r.map((c) => `"${c.replace(/"/g, '""')}"`).join(',')).join('\n');
  const a = document.createElement('a');
  a.href = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }));
  a.download = name;
  a.click();
  URL.revokeObjectURL(a.href);
}

// S2-01. Nhập nhiều tài khoản từ Excel qua 3 bước: tải tệp, xem trước kèm lỗi theo dòng, tổng kết.
// Khi tích hợp: POST /admin/users/import/preview (multipart) trả về các dòng đã kiểm, POST /admin/users/import để tạo.
// ponytail: chưa có thư viện Excel nên tệp mẫu và báo cáo lỗi xuất CSV ở trình duyệt, và ô tải nhận cả .csv.
// Khi có API: máy chủ sinh .xlsx và chỉ nhận .xlsx như thiết kế.
export function ImportUsersPage() {
  const [step, setStep] = useState(0);
  const [file, setFile] = useState('');
  const [rows, setRows] = useState<ImportRow[]>([]);
  const [error, setError] = useState('');
  const [show, setShow] = useState(SHOW[0]);
  const [drag, setDrag] = useState(false);
  const input = useRef<HTMLInputElement>(null);

  const bad = rows.filter((r) => r.error);
  const good = rows.length - bad.length;
  const visible = show === SHOW[1] ? bad : show === SHOW[2] ? rows.filter((r) => !r.error) : rows;

  function pick(f?: File) {
    if (!f) return;
    if (!/\.(xlsx|csv)$/i.test(f.name)) return setError('Chỉ nhận tệp .xlsx theo đúng mẫu.');
    setError('');
    setFile(f.name);
    setRows(previewRows());
    setShow(SHOW[0]);
    setStep(1);
  }
  const onDrop = (e: DragEvent) => {
    e.preventDefault();
    setDrag(false);
    pick(e.dataTransfer.files[0]);
  };

  const table = (list: ImportRow[], last: string) => (
    <div className="panel scroll">
      <table>
        <thead>
          <tr>
            <th>Dòng</th>
            {COLUMNS.map((c) => (
              <th key={c}>{c}</th>
            ))}
            <th>{last}</th>
          </tr>
        </thead>
        <tbody>
          {list.map((r) => (
            <tr key={r.line} className={r.error && step === 1 ? 'bad' : undefined}>
              <td>{r.line}</td>
              <td className="strong">{r.name || '(trống)'}</td>
              <td>{r.email}</td>
              <td>{r.phone}</td>
              <td>{r.role}</td>
              <td>{r.error ? <p className="err">{r.error}</p> : <Pill tone="ok">Hợp lệ</Pill>}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );

  return (
    <AppLayout crumb={['Người dùng & nhật ký', 'Tài khoản', 'Nhập từ Excel']} module="Người dùng & nhật ký">
      <div className="h">
        <div>
          <h2>Nhập người dùng từ Excel</h2>
          <p>Tạo nhiều tài khoản cùng lúc từ một tệp .xlsx.</p>
        </div>
      </div>
      <ol className="steps">
        {STEPS.map((s, i) => (
          <li key={s} className={i < step ? 'done' : i === step ? 'on' : undefined}>
            <span>{i < step ? <Icon name="check" size={16} /> : i + 1}</span>
            {s}
          </li>
        ))}
      </ol>

      {step === 0 && (
        <>
          <div
            className={'drop' + (drag ? ' over' : '')}
            onDragOver={(e) => {
              e.preventDefault();
              setDrag(true);
            }}
            onDragLeave={() => setDrag(false)}
            onDrop={onDrop}
            onClick={() => input.current?.click()}
          >
            <span className="drop-ic">
              <Icon name="upload" size={28} />
            </span>
            <b>Kéo tệp .xlsx vào đây</b>
            <p>hoặc bấm để chọn tệp từ máy. Chỉ nhận tệp .xlsx theo đúng mẫu.</p>
            <button type="button" className="btn" style={{ width: 140 }}>
              Chọn tệp
            </button>
            {error && <p className="err">{error}</p>}
            <input ref={input} type="file" accept=".xlsx,.csv" hidden onChange={(e) => pick(e.target.files?.[0])} />
          </div>
          <div className="panel tpl">
            <span className="tpl-ic">
              <Icon name="file" size={22} />
            </span>
            <div>
              <b>Tệp mẫu mau-nhap-nguoi-dung.xlsx</b>
              <p>Cột bắt buộc: Họ tên, Email, Số điện thoại, Vai trò. Mỗi dòng là một tài khoản.</p>
            </div>
            <button type="button" className="btn" style={{ width: 170 }} onClick={() => download('mau-nhap-nguoi-dung.csv', [COLUMNS])}>
              <Icon name="dl" size={16} />
              Tải tệp mẫu
            </button>
          </div>
        </>
      )}

      {step === 1 && (
        <>
          <div className="imp-bar">
            <Icon name="file" size={20} />
            <b>
              {file} · {rows.length} dòng
            </b>
            <Pill tone="ok">{good} dòng hợp lệ</Pill>
            <Pill tone="off">{bad.length} dòng lỗi</Pill>
            <label className="filter" style={{ marginLeft: 'auto', width: 220 }}>
              <span>Hiện: {show}</span>
              <Icon name="down" />
              <select value={show} onChange={(e) => setShow(e.target.value)} aria-label="Hiện">
                {SHOW.map((s) => (
                  <option key={s}>{s}</option>
                ))}
              </select>
            </label>
          </div>
          {table(visible, 'Kết quả kiểm tra')}
          <div className="actions">
            <span className="note">Dòng lỗi sẽ bị bỏ qua, dòng hợp lệ vẫn được nhập.</span>
            <button type="button" className="btn" style={{ width: 150 }} onClick={() => setStep(0)}>
              Chọn tệp khác
            </button>
            <button type="button" className="btn primary" style={{ width: 220 }} disabled={!good} onClick={() => setStep(2)}>
              Nhập {good} dòng hợp lệ
            </button>
          </div>
        </>
      )}

      {step === 2 && (
        <>
          <Box icon="check" tone="ok" title={`Đã tạo ${good} tài khoản`}>
            Email kích hoạt kèm mật khẩu tạm đã được gửi tới từng người.
          </Box>
          <div className="stats">
            <div className="stat">
              <b>{rows.length}</b>
              <span>dòng trong tệp</span>
            </div>
            <div className="stat">
              <b style={{ color: 'var(--green)' }}>{good}</b>
              <span>tài khoản đã tạo</span>
            </div>
            <div className="stat">
              <b style={{ color: 'var(--red)' }}>{bad.length}</b>
              <span>dòng bị bỏ qua</span>
            </div>
          </div>
          {bad.length > 0 && (
            <>
              <div className="section-t">Các dòng bị bỏ qua</div>
              {table(bad, 'Lý do')}
            </>
          )}
          <div className="actions">
            {bad.length > 0 && (
              <button
                type="button"
                className="btn"
                style={{ width: 220 }}
                onClick={() => download('bao-cao-loi-nhap.csv', [['Dòng', ...COLUMNS, 'Lý do'], ...bad.map((r) => [String(r.line), r.name, r.email, r.phone, r.role, r.error!])])}
              >
                <Icon name="dl" size={16} />
                Tải báo cáo lỗi (.csv)
              </button>
            )}
            <Link to="/quan-tri/tai-khoan" className="btn primary" style={{ width: 230 }}>
              Về danh sách tài khoản
            </Link>
          </div>
        </>
      )}
    </AppLayout>
  );
}
