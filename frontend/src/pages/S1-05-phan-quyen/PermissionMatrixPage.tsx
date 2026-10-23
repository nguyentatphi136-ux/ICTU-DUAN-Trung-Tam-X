import { useState } from 'react';
import { AppLayout } from '../../components/AppLayout';
import { Link } from 'react-router-dom';
import { Icon } from '../../components/Icon';
import { useToast } from '../../components/Toast';
import { LoadState } from '../../components/ui';
import { apiEnabled, useLoad } from '../../data/api';
import { auditBy } from '../../data/audit';
import { useAuth } from '../../data/auth';
import { adminPerm, MODULES, ROLES, USERS_MODULE, type Permission } from '../../data/permissions';
import { ADMIN_CORE, loadPermissions, loadRoles, saveRolePermissions, type ServerPermission, type ServerRole } from '../../data/s1api';
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

// S1-05. Có backend: ma trận vai trò × mã quyền lưu trên máy chủ (ServerMatrix). Không có backend: ma trận module × vai trò
// theo sheet 2 (F/W/R), lưu trong trình duyệt. Máy chủ kiểm quyền mọi API, mặc định là từ chối.
export function PermissionMatrixPage() {
  return apiEnabled ? <ServerMatrix /> : <LocalMatrix />;
}

function LocalMatrix() {
  const toast = useToast();
  const { user } = useAuth();
  const [saved, setSaved] = useState(initial);
  const [perms, setPerms] = useState(initial);
  const dirty = JSON.stringify(saved) !== JSON.stringify(perms);

  const set = (m: number, r: number, v: Permission) => setPerms((p) => p.map((row, i) => (i === m ? row.map((x, j) => (j === r ? v : x)) : row)));

  return (
    <AppLayout crumb={[USERS_MODULE, 'Phân quyền']} module={USERS_MODULE}>
      <div className="h">
        <div>
          <h2>Ma trận phân quyền</h2>
        </div>
        <Link to="/quan-tri/tai-khoan" className="btn">
          <Icon name="users" size={16} />
          Danh sách tài khoản
        </Link>
        <button type="button" className="btn" disabled={!dirty} onClick={() => setPerms(saved)} style={{ width: 150 }}>
          Huỷ thay đổi
        </button>
        <button
          type="button"
          className="btn primary"
          disabled={!dirty}
          style={{ width: 170 }}
          onClick={() => {
            // S6-11: mỗi ô đổi là một dòng trong bản ghi nhật ký.
            const changes = MODULES.flatMap((m, i) => ROLES.flatMap((r, j) => (saved[i][j] === perms[i][j] ? [] : [{ field: `${m.name} · ${r}`, before: saved[i][j], after: perms[i][j] }])));
            auditBy(user, { entity: 'Vai trò', action: 'Sửa phân quyền', target: 'Ma trận phân quyền', changes });
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
                  <span className={'mx-cell locked ' + tone(adminPerm(m.name))} title="Quản trị hệ thống chỉ quản lý tài khoản (cố định)">
                    {adminPerm(m.name)}
                    <Icon name="lock" />
                  </span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <p className="hint mx-foot">
        * Chỉ trên dữ liệu của chính mình hoặc của lớp mình phụ trách. Mọi chức năng đều kiểm quyền ở tầng server, mặc định là từ chối. Cột Quản trị hệ thống cố định: chỉ quản lý tài khoản, vai trò và nhật ký; riêng xoá vĩnh viễn trong thùng rác vẫn do Quản trị làm.
      </p>
    </AppLayout>
  );
}

const MODULE_LABEL: Record<string, string> = {
  AUTH: 'Đăng nhập & phân quyền',
  DASHBOARD: 'Tổng quan',
  PROFILE: 'Hồ sơ cá nhân',
  USER: 'Người dùng',
  PROGRAM: 'Chương trình & môn học',
  CLASS: 'Lớp học & hồ sơ học viên',
  SCHEDULE: 'Thời khoá biểu',
  ADMISSIONS: 'Tuyển sinh & lead',
  ATTENDANCE: 'Điểm danh',
  ASSIGNMENT: 'Bài tập',
  GRADE: 'Điểm',
  FINANCE: 'Học phí & công nợ',
  REPORT: 'Báo cáo',
  PUBLIC: 'Trang công khai',
  SYSTEM: 'Hệ thống',
};
const sameSet = (a: string[], b: string[]) => [...a].sort().join() === [...b].sort().join();

/** S1-05 khi có backend: mỗi vai trò một bộ mã quyền (GET /api/admin/roles, PUT /api/admin/roles/{mã}/permissions). */
function ServerMatrix() {
  const toast = useToast();
  const { user } = useAuth();
  const [roles, setRoles] = useState<ServerRole[]>([]);
  const [perms, setPerms] = useState<ServerPermission[]>([]);
  const [grants, setGrants] = useState<Record<string, string[]>>({});
  const [saving, setSaving] = useState(false);
  const load = useLoad(async () => {
    const [r, p] = await Promise.all([loadRoles(), loadPermissions()]);
    setRoles(r);
    setPerms(p);
    setGrants(Object.fromEntries(r.map((x) => [x.roleCode, x.permissionCodes])));
  });
  const changed = roles.filter((r) => !sameSet(grants[r.roleCode] ?? [], r.permissionCodes));
  const modules = [...new Set(perms.map((p) => p.module))];
  const toggle = (role: string, code: string) =>
    setGrants((g) => ({ ...g, [role]: g[role]?.includes(code) ? g[role].filter((c) => c !== code) : [...(g[role] ?? []), code] }));

  async function save() {
    setSaving(true);
    try {
      for (const r of changed) {
        const now = grants[r.roleCode] ?? [];
        await saveRolePermissions(r.roleCode, now);
        // S6-11: ghi quyền được cấp thêm và bị thu hồi của từng vai trò.
        const added = now.filter((c) => !r.permissionCodes.includes(c));
        const removed = r.permissionCodes.filter((c) => !now.includes(c));
        auditBy(user, {
          entity: 'Vai trò',
          action: 'Sửa phân quyền',
          target: `Vai trò ${r.roleName}`,
          changes: [...(added.length ? [{ field: 'Cấp thêm', before: '–', after: added.join(', ') }] : []), ...(removed.length ? [{ field: 'Thu hồi', before: removed.join(', '), after: '–' }] : [])],
        });
      }
      toast('Đã lưu phân quyền', 'Có hiệu lực ngay ở thao tác kế tiếp, người dùng không cần đăng nhập lại.');
    } catch (e) {
      toast('Chưa lưu được phân quyền', (e as Error).message);
    }
    setSaving(false);
    load.reload();
  }

  return (
    <AppLayout crumb={[USERS_MODULE, 'Phân quyền']} module={USERS_MODULE}>
      <div className="h">
        <div>
          <h2>Ma trận phân quyền</h2>
          <p>{changed.length ? `${changed.length} vai trò có thay đổi chưa lưu` : 'Quyền đang áp dụng trên máy chủ'}</p>
        </div>
        <Link to="/quan-tri/tai-khoan" className="btn">
          <Icon name="users" size={16} />
          Danh sách tài khoản
        </Link>
        <button type="button" className="btn" disabled={!changed.length || saving} onClick={() => setGrants(Object.fromEntries(roles.map((x) => [x.roleCode, x.permissionCodes])))} style={{ width: 150 }}>
          Huỷ thay đổi
        </button>
        <button type="button" className="btn primary" disabled={!changed.length || saving} style={{ width: 170 }} onClick={() => void save()}>
          {saving ? 'Đang lưu…' : 'Lưu phân quyền'}
        </button>
      </div>
      <LoadState {...load} />
      <div className="panel scroll">
        <table className="mx">
          <thead>
            <tr>
              <th>Quyền</th>
              {roles.map((r) => (
                <th key={r.roleCode} className={r.roleCode === 'ADMIN' ? 'admin' : undefined}>
                  {r.roleName}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {modules.map((m) => [
              <tr key={m}>
                <td className="mx-mod" colSpan={roles.length + 1} style={{ fontWeight: 600, background: 'var(--surface)' }}>
                  {MODULE_LABEL[m] ?? m}
                </td>
              </tr>,
              ...perms
                .filter((p) => p.module === m)
                .map((p) => (
                  <tr key={p.permissionCode}>
                    <td className="mx-mod" title={p.description}>
                      {p.permissionName}
                      <small style={{ display: 'block', color: 'var(--muted)', fontWeight: 400 }}>{p.permissionCode}</small>
                    </td>
                    {roles.map((r) => {
                      const locked = r.roleCode === 'ADMIN' && ADMIN_CORE.includes(p.permissionCode);
                      return (
                        <td key={r.roleCode}>
                          <input
                            type="checkbox"
                            checked={grants[r.roleCode]?.includes(p.permissionCode) ?? false}
                            disabled={locked || saving}
                            title={locked ? 'Quyền quản trị cốt lõi, không thu hồi được' : undefined}
                            aria-label={`${p.permissionName} – ${r.roleName}`}
                            onChange={() => toggle(r.roleCode, p.permissionCode)}
                          />
                        </td>
                      );
                    })}
                  </tr>
                )),
            ])}
          </tbody>
        </table>
      </div>
      <p className="hint mx-foot">
        Máy chủ kiểm quyền mọi API theo bảng này, mặc định là từ chối. Ô khoá là quyền quản trị cốt lõi. Menu bên trái vẫn theo ma trận
        module của sheet 2.
      </p>
    </AppLayout>
  );
}
