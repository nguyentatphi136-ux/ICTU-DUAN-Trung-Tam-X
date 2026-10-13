import { useState } from 'react';
import { AppLayout } from '../../components/AppLayout';
import { Icon } from '../../components/Icon';
import { Drawer, Filter, Pager, paginate, Pill, Search, usePaging } from '../../components/ui';
import { vdt } from '../../data/assignments';
import { AUDIT, type AuditEntity, type AuditEntry } from '../../data/audit';
import { USERS_MODULE } from '../../data/permissions';
import { download, toXlsx } from '../../data/xlsx';
import { TODAY } from '../../data/funnel';
import '../S3-07-danh-sach-lop/classes.css';
import './audit.css';

const ENTITIES: AuditEntity[] = ['Điểm', 'Điểm danh', 'Học phí', 'Vai trò'];
const tone = (e: AuditEntity) => (e === 'Điểm' ? 'off' : e === 'Học phí' ? 'wait' : e === 'Vai trò' ? 'info' : undefined);

// S6-11. Nhật ký thao tác trên dữ liệu nhạy cảm: điểm, điểm danh, học phí, vai trò người dùng. Mỗi bản ghi có người thực hiện,
// thời điểm, giá trị trước và sau. Lọc theo người thực hiện, loại đối tượng, khoảng thời gian; bấm một dòng xem chi tiết.
export function AuditPage() {
  const [by, setBy] = useState('');
  const [entity, setEntity] = useState('');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [q, setQ] = useState('');
  const [open, setOpen] = useState<AuditEntry | null>(null);
  const { page, setPage, size, setSize } = usePaging(20);
  const kw = q.trim().toLowerCase();
  const list = AUDIT.filter(
    (e) => (!by || e.by === by) && (!entity || e.entity === entity) && (!from || e.at.slice(0, 10) >= from) && (!to || e.at.slice(0, 10) <= to) && (!kw || e.target.toLowerCase().includes(kw)),
  ).sort((a, b) => b.at.localeCompare(a.at));
  const view = paginate(list, page, size);
  const reset = (f: (v: string) => void) => (v: string) => (f(v), setPage(1));

  function exportXlsx() {
    const rows = [['Thời điểm', 'Người thực hiện', 'Vai trò', 'Đối tượng', 'Thao tác', 'Trên', 'Trường', 'Trước', 'Sau', 'Ghi chú'], ...list.flatMap((e) => e.changes.map((c) => [vdt(e.at), e.by, e.role, e.entity, e.action, e.target, c.field, c.before, c.after, e.note ?? '']))];
    download(toXlsx(rows, 'Nhật ký thao tác'), `nhat-ky-thao-tac_${TODAY}.xlsx`);
  }

  return (
    <AppLayout crumb={[USERS_MODULE, 'Nhật ký thao tác']} module={USERS_MODULE}>
      <div className="h">
        <div>
          <h2>Nhật ký thao tác</h2>
          <p>Mọi thao tác trên điểm, học phí và vai trò người dùng</p>
        </div>
        <button type="button" className="btn" onClick={exportXlsx}>
          <Icon name="dl" size={16} />
          Xuất Excel
        </button>
      </div>
      <div className="bar">
        <Filter label="Người thực hiện" value={by} options={[...new Set(AUDIT.map((e) => e.by))]} onChange={reset(setBy)} width={260} />
        <Filter label="Đối tượng" value={entity} options={ENTITIES} onChange={reset(setEntity)} width={210} />
        <div className="cl-range" role="group" aria-label="Khoảng thời gian">
          <input type="date" value={from} aria-label="Từ ngày" onChange={(e) => reset(setFrom)(e.target.value)} />
          <span>–</span>
          <input type="date" value={to} aria-label="Đến ngày" onChange={(e) => reset(setTo)(e.target.value)} />
        </div>
        <Search placeholder="Tìm theo mã học viên, mã lớp" value={q} onChange={reset(setQ)} width={280} />
      </div>
      <div className="panel">
        <div className="scroll">
          <table className="rows">
            <thead>
              <tr>
                <th>Thời điểm</th>
                <th>Người thực hiện</th>
                <th>Đối tượng</th>
                <th>Thao tác</th>
                <th>Trên</th>
                <th>Trước → Sau</th>
              </tr>
            </thead>
            <tbody>
              {view.items.map((e) => (
                <tr key={e.id} tabIndex={0} onClick={() => setOpen(e)} onKeyDown={(k) => k.key === 'Enter' && setOpen(e)}>
                  <td>{vdt(e.at)}</td>
                  <td>
                    <b className="strong">{e.by}</b>
                    <small className="lv-sub">{e.role}</small>
                  </td>
                  <td>
                    <Pill tone={tone(e.entity)}>{e.entity}</Pill>
                  </td>
                  <td className="strong">{e.action}</td>
                  <td>{e.target}</td>
                  <td className="au-diff">
                    {e.changes.slice(0, 1).map((c) => (
                      <span key={c.field}>
                        <del>{c.before}</del> → <ins>{c.after}</ins>
                      </span>
                    ))}
                    {e.changes.length > 1 && <small> và {e.changes.length - 1} trường khác</small>}
                  </td>
                </tr>
              ))}
              {!view.items.length && (
                <tr>
                  <td colSpan={6} className="empty">
                    Không có bản ghi khớp bộ lọc.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
        <Pager page={view.page} pages={view.pages} onPage={setPage} size={size} onSize={setSize}>
          {view.from}–{view.to} trong {list.length} bản ghi
        </Pager>
      </div>
      {open && (
        <Drawer title="Chi tiết bản ghi" onClose={() => setOpen(null)}>
          <dl className="sp-facts au-facts">
            <div>
              <dt>Thời điểm</dt>
              <dd>{vdt(open.at)}</dd>
            </div>
            <div>
              <dt>Đối tượng</dt>
              <dd>
                <Pill tone={tone(open.entity)}>{open.entity}</Pill>
              </dd>
            </div>
            <div>
              <dt>Người thực hiện</dt>
              <dd>
                {open.by} · {open.role}
              </dd>
            </div>
            <div>
              <dt>Thao tác</dt>
              <dd>{open.action}</dd>
            </div>
            <div className="wide">
              <dt>Trên</dt>
              <dd>{open.target}</dd>
            </div>
          </dl>
          <h4>Giá trị trước và sau</h4>
          <table className="au-changes">
            <thead>
              <tr>
                <th>Trường</th>
                <th>Trước</th>
                <th>Sau</th>
              </tr>
            </thead>
            <tbody>
              {open.changes.map((c) => (
                <tr key={c.field}>
                  <td className="strong">{c.field}</td>
                  <td>
                    <del>{c.before}</del>
                  </td>
                  <td>
                    <ins>{c.after}</ins>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {open.note && <p className="hint" style={{ marginTop: 12 }}>Ghi chú: {open.note}</p>}
          <p className="hint" style={{ marginTop: 12 }}>
            Bản ghi nhật ký không sửa, không xoá được.
          </p>
        </Drawer>
      )}
    </AppLayout>
  );
}
