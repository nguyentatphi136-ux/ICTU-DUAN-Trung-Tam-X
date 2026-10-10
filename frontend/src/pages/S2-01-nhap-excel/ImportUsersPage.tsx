import { useMemo, useRef, useState, type DragEvent } from 'react';
import { Link } from 'react-router-dom';
import { AppLayout } from '../../components/AppLayout';
import { Icon } from '../../components/Icon';
import { Box, Pager, paginate, Pill, Search, usePaging } from '../../components/ui';
import { api, ApiError, apiEnabled, json } from '../../data/api';
import { ROLES, USERS_MODULE } from '../../data/permissions';
import { COLUMNS, evaluate, importOk, MAX_ROWS, TEMPLATE_ROWS, toRaws, type ImportRow, type RowStatus } from './mockImport';
import { readSheet } from './readSheet';
import './import.css';

const STEPS = ['Tải tệp lên', 'Xem trước và kiểm lỗi', 'Kết quả'];
const SHOW: [string, RowStatus | ''][] = [
  ['Tất cả dòng', ''],
  ['Chỉ dòng hợp lệ', 'ok'],
  ['Chỉ dòng trùng', 'duplicate'],
  ['Chỉ dòng lỗi', 'error'],
];
const MAX_BYTES = 10 * 1024 * 1024;

/** Mã vai trò máy chủ -> tên hiển thị. */
const ROLE_NAME: Record<string, string> = {
  STUDENT: ROLES[0], TA: ROLES[1], INSTRUCTOR: ROLES[2], ADMISSIONS: ROLES[3], ACCOUNTANT: ROLES[4], TRAINING_MANAGER: ROLES[5],
};

type ApiRow = { rowIndex: number; fullName: string; email: string; phone: string; role: string; dateOfBirth: string; status: RowStatus; issues: { message: string }[] };
type ApiSkipped = { rowIndex: number; fullName: string; email: string; phone: string; type: 'DUPLICATE' | 'INVALID'; reason: string };
type Result = { total: number; created: number; skipped: ImportRow[] };

const fromApi = (r: ApiRow): ImportRow => ({
  line: r.rowIndex, name: r.fullName, email: r.email, phone: r.phone, role: ROLE_NAME[r.role] ?? r.role, birth: r.dateOfBirth, status: r.status, issues: r.issues.map((i) => i.message),
});

/** Tải một tệp CSV do trình duyệt tạo (tệp mẫu, báo cáo dòng bị bỏ qua). Có BOM để Excel mở đúng tiếng Việt. */
function download(name: string, rows: string[][]) {
  const csv = '﻿' + rows.map((r) => r.map((c) => `"${c.replace(/"/g, '""')}"`).join(',')).join('\r\n');
  const a = document.createElement('a');
  a.href = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }));
  a.download = name;
  a.click();
  URL.revokeObjectURL(a.href);
}

const STATUS_LABEL: Record<RowStatus, string> = { ok: 'Hợp lệ', duplicate: 'Trùng', error: 'Lỗi' };

// S2-01. Nhập nhiều tài khoản từ Excel qua 3 bước: tải tệp, xem trước, tổng kết.
// Xem trước tách ba nhóm Hợp lệ / Trùng / Lỗi, tìm theo email hoặc họ tên, phân trang (tệp vài nghìn dòng).
// Có backend: gửi tệp lên POST /api/admin/users/import/preview (máy chủ đọc .xlsx bằng Apache POI) rồi
// POST /api/admin/users/import. Chưa có backend: đọc tệp ngay trong trình duyệt và kiểm cùng quy tắc với máy chủ.
export function ImportUsersPage() {
  const [step, setStep] = useState(0);
  const [file, setFile] = useState('');
  const [rows, setRows] = useState<ImportRow[]>([]);
  const [result, setResult] = useState<Result | null>(null);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState('');
  const [drag, setDrag] = useState(false);
  const input = useRef<HTMLInputElement>(null);

  const count = (s: RowStatus) => rows.filter((r) => r.status === s).length;
  const good = count('ok');

  async function pick(f?: File) {
    if (!f || busy) return;
    if (!/\.(xlsx|csv)$/i.test(f.name)) return setError('Chỉ nhận tệp .xlsx hoặc .csv theo đúng mẫu.');
    if (f.size > MAX_BYTES) return setError('Tệp nặng hơn 10MB. Hãy chia thành nhiều tệp nhỏ hơn.');
    setError('');
    setBusy('Đang đọc và kiểm tra tệp…');
    try {
      let list: ImportRow[];
      if (apiEnabled) {
        const form = new FormData();
        form.append('file', f);
        list = (await api<{ rows: ApiRow[] }>('/api/admin/users/import/preview', { method: 'POST', body: form })).rows.map(fromApi);
      } else {
        const raws = toRaws(await readSheet(f));
        if (raws.length > MAX_ROWS) throw new Error(`Tệp có ${raws.length} dòng, vượt giới hạn ${MAX_ROWS} dòng mỗi lần nhập.`);
        list = evaluate(raws);
      }
      if (!list.length) throw new Error('Tệp không có dòng dữ liệu nào.');
      setFile(f.name);
      setRows(list);
      setStep(1);
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setBusy('');
      if (input.current) input.current.value = '';
    }
  }

  async function runImport() {
    setBusy('Đang tạo tài khoản…');
    setError('');
    try {
      if (apiEnabled) {
        const s = await api<{ successRows: number; errors: ApiSkipped[] }>('/api/admin/users/import', json('POST', { fileName: file }));
        setResult({
          total: rows.length,
          created: s.successRows,
          skipped: s.errors.map((e) => ({ line: e.rowIndex, name: e.fullName, email: e.email, phone: e.phone, role: '', birth: '', status: e.type === 'DUPLICATE' ? 'duplicate' : 'error', issues: [e.reason] })),
        });
      } else {
        setResult({ total: rows.length, created: importOk(rows), skipped: rows.filter((r) => r.status !== 'ok') });
      }
      setStep(2);
    } catch (e) {
      setError((e as ApiError).message);
    } finally {
      setBusy('');
    }
  }

  const onDrop = (e: DragEvent) => {
    e.preventDefault();
    setDrag(false);
    pick(e.dataTransfer.files[0]);
  };

  return (
    <AppLayout crumb={[USERS_MODULE, 'Tài khoản', 'Nhập từ Excel']} module={USERS_MODULE}>
      <div className="h">
        <div>
          <h2>Nhập người dùng từ Excel</h2>
          <p>Tạo nhiều tài khoản cùng lúc từ một tệp .xlsx (tối đa {MAX_ROWS.toLocaleString('vi-VN')} dòng, 10MB).</p>
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
            className={'drop' + (drag ? ' over' : '') + (busy ? ' busy' : '')}
            aria-busy={!!busy}
            onDragOver={(e) => {
              e.preventDefault();
              setDrag(true);
            }}
            onDragLeave={() => setDrag(false)}
            onDrop={onDrop}
            onClick={() => !busy && input.current?.click()}
          >
            <span className="drop-ic">
              <Icon name={busy ? 'clock' : 'upload'} size={28} />
            </span>
            <b>{busy || 'Kéo tệp .xlsx vào đây'}</b>
            <p>{busy ? 'Tệp lớn có thể mất vài giây.' : 'hoặc bấm để chọn tệp từ máy. Nhận tệp .xlsx hoặc .csv theo đúng mẫu.'}</p>
            <button type="button" className="btn" style={{ width: 140 }} disabled={!!busy}>
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
              <b>Tệp mẫu mau-nhap-nguoi-dung.csv</b>
              <p>Mở bằng Excel rồi lưu lại dạng .xlsx. Cột bắt buộc: Họ và tên, Email. Vai trò ghi bằng tiếng Việt, ví dụ Học viên, Giảng viên.</p>
            </div>
            <button type="button" className="btn" style={{ width: 170 }} onClick={() => download('mau-nhap-nguoi-dung.csv', [COLUMNS, ...TEMPLATE_ROWS])}>
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
              {file} · {rows.length.toLocaleString('vi-VN')} dòng
            </b>
            <Pill tone="ok">{good} hợp lệ</Pill>
            <Pill tone="wait">{count('duplicate')} trùng</Pill>
            <Pill tone="off">{count('error')} lỗi</Pill>
          </div>
          <RowsTable rows={rows} resultCol="Kết quả kiểm tra" filters />
          {error && <Box icon="alert" tone="bad" title={error} />}
          <div className="actions">
            <span className="note">Dòng trùng và dòng lỗi sẽ bị bỏ qua, dòng hợp lệ vẫn được nhập.</span>
            <button type="button" className="btn" style={{ width: 150 }} disabled={!!busy} onClick={() => setStep(0)}>
              Chọn tệp khác
            </button>
            <button type="button" className="btn primary" style={{ width: 220 }} disabled={!good || !!busy} onClick={runImport}>
              {busy || `Nhập ${good} dòng hợp lệ`}
            </button>
          </div>
        </>
      )}

      {step === 2 && result && (
        <>
          <Box icon="check" tone="ok" title={`Đã tạo ${result.created} tài khoản`}>
            Email kích hoạt kèm mật khẩu tạm được gửi tới từng người.
          </Box>
          <div className="stats imp-stats">
            <div className="stat">
              <b>{result.total}</b>
              <span>dòng trong tệp</span>
            </div>
            <div className="stat">
              <b style={{ color: 'var(--green)' }}>{result.created}</b>
              <span>tài khoản đã tạo</span>
            </div>
            <div className="stat">
              <b style={{ color: 'var(--warn)' }}>{result.skipped.filter((r) => r.status === 'duplicate').length}</b>
              <span>dòng trùng bị bỏ qua</span>
            </div>
            <div className="stat">
              <b style={{ color: 'var(--red)' }}>{result.skipped.filter((r) => r.status === 'error').length}</b>
              <span>dòng lỗi bị bỏ qua</span>
            </div>
          </div>
          {result.skipped.length > 0 && (
            <>
              <div className="section-t">Các dòng bị bỏ qua</div>
              <RowsTable rows={result.skipped} resultCol="Lý do" filters />
            </>
          )}
          <div className="actions">
            {result.skipped.length > 0 && (
              <button
                type="button"
                className="btn"
                style={{ width: 220 }}
                onClick={() =>
                  download('bao-cao-dong-bo-qua.csv', [
                    ['Dòng', 'Họ và tên', 'Email', 'Số điện thoại', 'Nhóm', 'Lý do'],
                    ...result.skipped.map((r) => [String(r.line), r.name, r.email, r.phone, STATUS_LABEL[r.status], r.issues.join('; ')]),
                  ])
                }
              >
                <Icon name="dl" size={16} />
                Tải báo cáo (.csv)
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

/** Bảng dòng nhập: tìm theo email hoặc họ tên, lọc nhóm, phân trang. */
function RowsTable({ rows, resultCol, filters }: { rows: ImportRow[]; resultCol: string; filters?: boolean }) {
  const [q, setQ] = useState('');
  const [show, setShow] = useState<RowStatus | ''>('');
  const { page, setPage, size, setSize } = usePaging();
  const visible = useMemo(() => {
    const k = q.trim().toLowerCase();
    return rows.filter((r) => (!show || r.status === show) && (!k || r.email.toLowerCase().includes(k) || r.name.toLowerCase().includes(k)));
  }, [rows, q, show]);
  const view = paginate(visible, page, size);
  const options = SHOW.filter(([, s]) => !s || rows.some((r) => r.status === s));

  return (
    <div className="panel">
      {filters && (
        <div className="imp-filters">
          <Search placeholder="Tìm theo email hoặc họ tên" value={q} onChange={(v) => (setQ(v), setPage(1))} width={360} />
          {options.length > 2 && (
            <label className="filter" style={{ width: 220 }}>
              <span>Hiện: {SHOW.find(([, s]) => s === show)?.[0]}</span>
              <Icon name="down" />
              <select value={show} onChange={(e) => (setShow(e.target.value as RowStatus | ''), setPage(1))} aria-label="Hiện">
                {options.map(([label, s]) => (
                  <option key={label} value={s}>
                    {label}
                  </option>
                ))}
              </select>
            </label>
          )}
        </div>
      )}
      <div className="scroll">
        <table>
          <thead>
            <tr>
              <th>Dòng</th>
              <th>Họ và tên</th>
              <th>Email</th>
              <th>Số điện thoại</th>
              <th>Vai trò</th>
              <th>{resultCol}</th>
            </tr>
          </thead>
          <tbody>
            {view.items.map((r) => (
              <tr key={r.line} className={r.status === 'error' ? 'bad' : r.status === 'duplicate' ? 'dup' : undefined}>
                <td>{r.line}</td>
                <td className="strong">{r.name || '(trống)'}</td>
                <td>{r.email}</td>
                <td>{r.phone}</td>
                <td>{r.role}</td>
                <td>
                  {r.status === 'ok' ? (
                    <Pill tone="ok">Hợp lệ</Pill>
                  ) : (
                    <span className="imp-issue">
                      <Pill tone={r.status === 'duplicate' ? 'wait' : 'off'}>{STATUS_LABEL[r.status]}</Pill>
                      <span>{r.issues.join('; ')}</span>
                    </span>
                  )}
                </td>
              </tr>
            ))}
            {!view.items.length && (
              <tr>
                <td colSpan={6} style={{ textAlign: 'center', padding: 32 }}>
                  Không có dòng nào khớp.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
      <Pager page={view.page} pages={view.pages} onPage={setPage} size={size} onSize={setSize}>
        Hiển thị {view.from}–{view.to} trong {visible.length.toLocaleString('vi-VN')} dòng
      </Pager>
    </div>
  );
}
