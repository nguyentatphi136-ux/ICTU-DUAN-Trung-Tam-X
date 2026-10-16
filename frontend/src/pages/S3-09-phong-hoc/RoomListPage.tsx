import { useState } from 'react';
import { Link } from 'react-router-dom';
import { AppLayout } from '../../components/AppLayout';
import { Icon } from '../../components/Icon';
import { useToast } from '../../components/Toast';
import { ConfirmDialog, Drawer, Filter, LoadState, Search } from '../../components/ui';
import { apiEnabled, useLoad } from '../../data/api';
import { useAuth } from '../../data/auth';
import { deleteRoom, loadClasses, loadRooms, saveRoom } from '../../data/s3api';
import { capacityText, classesIn, ROOM_KINDS, roomHref, ROOMS, type Room, type RoomKind } from '../../data/rooms';
import { saveAll } from '../../data/store';
import '../S3-04-ho-so-hoc-vien/student.css';
import { canManageClasses, MODULE } from '../S3-07-danh-sach-lop/ClassListPage';
import './room.css';

// S3-09. Danh sách phòng học: phòng vật lý kèm sức chứa, hoặc phòng trực tuyến kèm liên kết.
// Cột Lớp đang dùng cho biết phòng nào còn trống; phòng đang có lớp thì không xoá được.
export function RoomListPage() {
  const { user } = useAuth();
  const toast = useToast();
  const manage = canManageClasses(user?.active ?? -1);
  const [list, setList] = useState(ROOMS);
  const [q, setQ] = useState('');
  const [kind, setKind] = useState('');
  const [editing, setEditing] = useState<Room | 'new' | null>(null);
  const [deleting, setDeleting] = useState<Room | null>(null);
  // Có backend: tải phòng và lớp (cột Lớp đang dùng).
  const load = useLoad(async () => {
    await Promise.all([loadRooms(), loadClasses()]);
    setList([...ROOMS]);
  });
  const sync = (call: Promise<unknown>, title: string, after: () => void) =>
    call.then(
      () => (after(), load.reload(), toast(title)),
      (e: Error) => toast('Chưa lưu được', e.message),
    );

  const commit = (next: Room[]) => {
    ROOMS.splice(0, ROOMS.length, ...next);
    saveAll();
    setList(next);
  };
  const shown = list.filter((r) => (!q.trim() || r.name.toLowerCase().includes(q.trim().toLowerCase())) && (!kind || r.kind === kind));

  return (
    <AppLayout crumb={[MODULE, 'Phòng học']} module={MODULE}>
      <Link to="/lop-hoc" className="back">
        <Icon name="back" size={16} />
        Lớp học
      </Link>
      <div className="h">
        <div>
          <h2>Phòng học</h2>
          <p>
            {list.filter((r) => r.kind === 'Vật lý').length} phòng vật lý · {list.filter((r) => r.kind === 'Trực tuyến').length} phòng trực tuyến
          </p>
        </div>
        {manage && (
          <button type="button" className="btn primary" style={{ width: 150 }} onClick={() => setEditing('new')}>
            <Icon name="plus" size={16} />
            Thêm phòng
          </button>
        )}
      </div>
      <LoadState {...load} />
      <div className="bar">
        <Search placeholder="Tìm theo tên phòng" value={q} onChange={setQ} width={280} />
        <Filter label="Loại" value={kind} options={ROOM_KINDS} onChange={setKind} />
      </div>
      <div className="panel">
        <div className="scroll">
          <table>
            <thead>
              <tr>
                <th>Tên phòng</th>
                <th>Loại</th>
                <th>Vị trí hoặc liên kết</th>
                <th>Sức chứa</th>
                <th>Lớp đang dùng</th>
                {manage && <th />}
              </tr>
            </thead>
            <tbody>
              {shown.map((r) => {
                const used = classesIn(r.name);
                return (
                  <tr key={r.name}>
                    <td className="strong">{r.name}</td>
                    <td className="strong">{r.kind}</td>
                    <td>
                      {r.kind === 'Trực tuyến' ? (
                        <a href={roomHref(r.where)} target="_blank" rel="noreferrer">
                          {r.where}
                        </a>
                      ) : (
                        r.where
                      )}
                    </td>
                    <td className="strong">{capacityText(r)}</td>
                    <td className={used.length ? 'strong' : undefined}>{used.length ? used.map((k) => k.code).join(', ') : 'Chưa dùng'}</td>
                    {manage && (
                      <td className="rm-acts">
                        <button type="button" className="link" onClick={() => setEditing(r)}>
                          Sửa
                        </button>
                        <button type="button" className="link danger" disabled={!!used.length} title={used.length ? 'Phòng đang có lớp dùng' : undefined} onClick={() => setDeleting(r)}>
                          Xoá
                        </button>
                      </td>
                    )}
                  </tr>
                );
              })}
              {!shown.length && (
                <tr>
                  <td colSpan={6} className="empty">
                    Không có phòng khớp.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {editing && (
        <RoomDrawer
          room={editing === 'new' ? null : editing}
          onClose={() => setEditing(null)}
          onSave={(r) => {
            if (apiEnabled)
              return void sync(saveRoom(r, editing === 'new' ? undefined : editing.id), editing === 'new' ? 'Đã thêm phòng ' + r.name : 'Đã lưu phòng ' + r.name, () => setEditing(null));
            commit(editing === 'new' ? [...list, r] : list.map((x) => (x === editing ? r : x)));
            setEditing(null);
            toast(editing === 'new' ? 'Đã thêm phòng ' + r.name : 'Đã lưu phòng ' + r.name);
          }}
        />
      )}
      {deleting && (
        <ConfirmDialog
          title={'Xoá phòng ' + deleting.name}
          permanent
          confirmLabel="Xoá phòng"
          onClose={() => setDeleting(null)}
          onConfirm={() => {
            if (apiEnabled && deleting.id) return void sync(deleteRoom(deleting.id), 'Đã xoá phòng ' + deleting.name, () => setDeleting(null));
            commit(list.filter((x) => x !== deleting));
            setDeleting(null);
            toast('Đã xoá phòng ' + deleting.name);
          }}
        >
          Phòng <b>{deleting.name}</b> không còn trong danh sách chọn khi xếp phòng cho lớp.
        </ConfirmDialog>
      )}
    </AppLayout>
  );
}

function RoomDrawer({ room, onClose, onSave }: { room: Room | null; onClose: () => void; onSave: (r: Room) => void }) {
  const [name, setName] = useState(room?.name ?? '');
  const [kind, setKind] = useState<RoomKind>(room?.kind ?? 'Vật lý');
  const [where, setWhere] = useState(room?.where ?? '');
  const [cap, setCap] = useState(room?.capacity ? String(room.capacity) : '');
  const [errors, setErrors] = useState<Record<string, string>>({});
  const online = kind === 'Trực tuyến';

  function save() {
    const e: Record<string, string> = {};
    const n = name.trim();
    if (!n) e.name = 'Nhập tên phòng';
    else if (ROOMS.some((r) => r !== room && r.name.toLowerCase() === n.toLowerCase())) e.name = `Đã có phòng tên ${n}`;
    if (!where.trim()) e.where = online ? 'Nhập liên kết phòng học trực tuyến' : 'Nhập vị trí phòng';
    else if (online && !/^(https?:\/\/)?[\w-]+(\.[\w-]+)+(\/\S*)?/.test(where.trim().replace(/\s+/g, ''))) e.where = 'Liên kết chưa đúng dạng, ví dụ meet.google.com/abc-defg-hij';
    const c = cap.trim() ? Number(cap) : undefined;
    if (!online && !c) e.cap = 'Phòng vật lý cần sức chứa';
    else if (c !== undefined && (!Number.isInteger(c) || c < 1)) e.cap = 'Sức chứa là số nguyên dương';
    setErrors(e);
    if (!Object.keys(e).length) onSave({ name: n, kind, where: where.trim(), capacity: c });
  }

  return (
    <Drawer
      title={room ? 'Sửa phòng ' + room.name : 'Thêm phòng'}
      onClose={onClose}
      footer={
        <>
          <button type="button" className="btn" style={{ width: 100 }} onClick={onClose}>
            Huỷ
          </button>
          <button type="button" className="btn primary" style={{ width: 120 }} onClick={save}>
            Lưu phòng
          </button>
        </>
      }
    >
      <div className="field">
        <span className="lbl">Loại phòng</span>
        <div className="seg rm-kind" role="radiogroup" aria-label="Loại phòng">
          {ROOM_KINDS.map((k) => (
            <button key={k} type="button" role="radio" aria-checked={kind === k} className={kind === k ? 'on' : undefined} onClick={() => setKind(k)}>
              {k}
            </button>
          ))}
        </div>
      </div>
      <div className="field">
        <label htmlFor="rm-name">Tên phòng</label>
        <div className={'inp' + (errors.name ? ' bad' : '')}>
          <input id="rm-name" value={name} placeholder={online ? 'Zoom 03' : 'P.402'} onChange={(e) => setName(e.target.value)} />
        </div>
        {errors.name && <p className="err">{errors.name}</p>}
      </div>
      <div className="field">
        <label htmlFor="rm-where">{online ? 'Liên kết phòng học' : 'Vị trí'}</label>
        <div className={'inp' + (errors.where ? ' bad' : '')}>
          <input id="rm-where" value={where} placeholder={online ? 'meet.google.com/tms-data-k08' : 'Tầng 4, cơ sở Cầu Giấy'} onChange={(e) => setWhere(e.target.value)} />
        </div>
        {errors.where && <p className="err">{errors.where}</p>}
      </div>
      <div className="field">
        <label htmlFor="rm-cap">Sức chứa {online && '(bỏ trống nếu không giới hạn)'}</label>
        <div className={'inp' + (errors.cap ? ' bad' : '')}>
          <input id="rm-cap" type="number" min={1} value={cap} onChange={(e) => setCap(e.target.value)} />
        </div>
        {errors.cap && <p className="err">{errors.cap}</p>}
      </div>
    </Drawer>
  );
}
