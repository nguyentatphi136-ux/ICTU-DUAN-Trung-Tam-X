import { useState } from 'react';
import { AppLayout } from '../../components/AppLayout';
import { Icon } from '../../components/Icon';
import { useToast } from '../../components/Toast';
import { MODULES, ROLES, type Permission } from '../../data/permissions';
import './matrix.css';

const OPTIONS: Permission[] = ['F', 'W', 'W*', 'R', 'R*', '–'];
const LEGEND: [Permission, string][] = [
  ['F', 'Toàn quyền'],
  ['W', 'Ghi trong phạm vi được giao'],
  ['R', 'Chỉ xem'],
  ['–', 'Không truy cập'],
];
const tone = (p: Permission) => ({ F: 'mx-f', W: 'mx-w', R: 'mx-r', '–': 'mx-n' })[p[0] as 'F' | 'W' | 'R' | '–'];
const initial = () => MODULES.map((m) => [...m.perms]);

// S1-05. Ma trận module × vai trò. Mỗi ô là danh sách chọn; cột Quản trị hệ thống bị khoá ở toàn quyền.
// Khi tích hợp: GET/PUT /admin/permissions. Máy chủ kiểm quyền mọi API, mặc định là từ chối.
export function PermissionMatrixPage() {
  const toast = useToast();
  const [saved, setSaved] = useState(initial);
  const [perms, setPerms] = useState(initial);
  const dirty = JSON.stringify(saved) !== JSON.stringify(perms);

  const set = (m: number, r: number, v: Permission) => setPerms((p) => p.map((row, i) => (i === m ? row.map((x, j) => (j === r ? v : x)) : row)));

  return (
    <AppLayout crumb={['Người dùng & nhật ký', 'Phân quyền']} module="Người dùng & nhật ký">
      <div className="h">
        <div>
          <h2>Ma trận phân quyền</h2>
        </div>
        <button type="button" className="btn" disabled={!dirty} onClick={() => setPerms(saved)} style={{ width: 150 }}>
          Huỷ thay đổi
        </button>
        <button
          type="button"
          className="btn primary"
          disabled={!dirty}
          style={{ width: 170 }}
          onClick={() => {
            MODULES.forEach((m, i) => (m.perms = perms[i]));
            setSaved(perms);
            toast('Đã lưu phân quyền', 'Thay đổi có hiệu lực từ lần đăng nhập hoặc chuyển vai trò kế tiếp.');
          }}
        >
          Lưu phân quyền
        </button>
      </div>
      <div className="mx-legend">
        {LEGEND.map(([p, label]) => (
          <span key={p}>
            <i className={'mx-tag ' + tone(p)}>{p}</i>
            {label}
          </span>
        ))}
      </div>
      <div className="panel scroll">
        <table className="mx">
          <thead>
            <tr>
              <th>Module</th>
              {ROLES.map((r, i) => (
                <th key={r} className={i === ROLES.length - 1 ? 'admin' : undefined}>
                  {r}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {MODULES.map((m, mi) => (
              <tr key={m.name}>
                <td className="mx-mod">
                  <Icon name={m.icon} />
                  {m.name}
                </td>
                {perms[mi].map((p, ri) => (
                  <td key={ri}>
                    <label className={'mx-cell ' + tone(p)}>
                      {p}
                      <Icon name="down" />
                      <select value={p} aria-label={`${m.name} – ${ROLES[ri]}`} onChange={(e) => set(mi, ri, e.target.value as Permission)}>
                        {OPTIONS.map((o) => (
                          <option key={o}>{o}</option>
                        ))}
                      </select>
                    </label>
                  </td>
                ))}
                <td>
                  <span className="mx-cell mx-f locked" title="Quản trị hệ thống luôn toàn quyền">
                    F
                    <Icon name="lock" />
                  </span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <p className="hint mx-foot">
        * Chỉ trên dữ liệu của chính mình hoặc của lớp mình phụ trách. Mọi chức năng đều kiểm quyền ở tầng server, mặc định là từ chối. Cột Quản trị hệ thống bị khoá ở toàn quyền.
      </p>
    </AppLayout>
  );
}
