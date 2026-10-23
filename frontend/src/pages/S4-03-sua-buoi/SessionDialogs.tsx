import { useState } from 'react';
import { Icon } from '../../components/Icon';
import { Box, Modal } from '../../components/ui';
import { apiEnabled, useLoad } from '../../data/api';
import { classSize, findClass, STAFF } from '../../data/classes';
import { notify } from '../../data/notifications';
import { capacityText, ROOMS } from '../../data/rooms';
import { addDays, clashes, clashText, dow, holidayOn, longDate, SESSIONS, sessionsOf, type ClassSession } from '../../data/schedule';
import { TODAY } from '../../data/funnel';
import { loadRooms, loadStaff } from '../../data/s3api';
import { cancelSession, editSession, loadClassSessions } from '../../data/s4api';
import { saveAll } from '../../data/store';
import './session.css';

const now = () => new Date().toISOString().slice(0, 16);
/** Số người nhận thông báo của lớp: học viên đang học và người dạy. */
function audience(cls: string) {
  const k = findClass(cls);
  const staff = new Set(k?.subjects.flatMap((s) => [s.teacher, ...s.tas]).filter(Boolean));
  return { students: classSize(cls), staff: staff.size };
}

type EditProps = { s: ClassSession; onClose: () => void; onSaved: (msg: string) => void; onCancel: () => void };

// S4-03. Sửa hoặc dời một buổi: đổi ngày, giờ, phòng, giảng viên dạy thay. Mỗi lần đổi kiểm tra trùng lại ngay (S4-02);
// lưu xong thì học viên và giảng viên của lớp nhận thông báo thay đổi.
export function EditSessionDialog({ s, onClose, onSaved, onCancel }: EditProps) {
  const [f, setF] = useState({ date: s.date, start: s.start, end: s.end, room: s.room ?? '', teacher: s.teacher ?? '', reason: '' });
  // Có backend: danh sách phòng, giảng viên kèm id để gửi khi đổi.
  useLoad(() => Promise.all([loadStaff(), loadRooms()]));
  const [busy, setBusy] = useState(false);
  const [override, setOverride] = useState(false);
  const [error, setError] = useState('');
  const draft: ClassSession = { ...s, ...f, room: f.room || undefined, teacher: f.teacher || undefined };
  const found = clashes(draft);
  const changed = f.date !== s.date || f.start !== s.start || f.end !== s.end || f.room !== (s.room ?? '') || f.teacher !== (s.teacher ?? '');
  const moved = f.date !== s.date || f.start !== s.start;
  const who = audience(s.cls);
  const holiday = holidayOn(f.date);
  const msg = `Buổi ${s.no} lớp ${s.cls} ${moved ? `dời từ ${s.start} ${longDate(s.date).toLowerCase()} sang ${f.start} ${longDate(f.date).toLowerCase()}` : 'thay đổi'}${f.room ? ', phòng ' + f.room : ''}${f.teacher !== (s.teacher ?? '') ? ', giảng viên ' + f.teacher : ''}.${f.reason.trim() ? ' Lý do: ' + f.reason.trim().replace(/\.$/, '').toLowerCase() + '.' : ''}`;
  const set = (k: keyof typeof f) => (e: { target: { value: string } }) => setF({ ...f, [k]: e.target.value });

  function save() {
    if (!changed) return setError('Chưa có gì thay đổi');
    if (f.start >= f.end) return setError('Giờ kết thúc phải sau giờ bắt đầu');
    if (!f.reason.trim()) return setError('Nhập lý do thay đổi để học viên hiểu vì sao lịch đổi');
    if (found.length && !override) return setError('Buổi đang trùng lịch. Đổi khung giờ, phòng, giảng viên hoặc xác nhận ghi đè');
    if (apiEnabled) {
      // Máy chủ kiểm tra trùng lịch lần nữa (S4-02), ghi lịch sử và gửi thông báo cho lớp.
      setBusy(true);
      editSession(s, f, override).then(
        () => onSaved(`Đã lưu buổi ${s.no} lớp ${s.cls}. Đã gửi thông báo tới ${who.students} học viên.`),
        (e: Error) => (setBusy(false), setError(e.message)),
      );
      return;
    }
    if (moved) s.movedFrom = s.movedFrom ?? `${s.date} ${s.start}`;
    Object.assign(s, { date: f.date, start: f.start, end: f.end, room: f.room || undefined, teacher: f.teacher || undefined });
    notify({ at: now(), kind: 'Lịch học', title: `Lớp ${s.cls} đổi lịch buổi ${s.no}`, body: msg, link: '/lich-hoc', to: { cls: s.cls } });
    saveAll();
    onSaved(`Đã lưu buổi ${s.no} lớp ${s.cls}. Đã gửi thông báo tới ${who.students} học viên.`);
  }

  return (
    <Modal onClose={onClose} label="Sửa buổi học" wide>
      <h3 style={{ marginBottom: 4 }}>Sửa buổi học</h3>
      <p className="hint" style={{ fontSize: 14, marginBottom: 16 }}>
        {s.cls} · Buổi {s.no} · {s.subject} Bài {s.lesson}: {s.topic}
      </p>
      {found.map((c, i) => (
        <Box key={i} icon="alert" tone="bad" title={'Đang ' + c.kind.toLowerCase()}>
          {clashText(c)} ngày {longDate(f.date)}.
        </Box>
      ))}
      <div className="grid2">
        <div className="field">
          <label htmlFor="es-date">Ngày</label>
          <div className="inp">
            <input id="es-date" type="date" value={f.date} onChange={set('date')} />
          </div>
          {holiday ? <p className="err">Ngày này là {holiday.name}</p> : f.date !== s.date && <p className="hint">Đã dời từ {longDate(s.date).toLowerCase()} sang {longDate(f.date).toLowerCase()}</p>}
        </div>
        <div className="field">
          <span className="lbl">Giờ</span>
          <div className="es-time">
            <input type="time" aria-label="Giờ bắt đầu" value={f.start} onChange={set('start')} />
            <span>–</span>
            <input type="time" aria-label="Giờ kết thúc" value={f.end} onChange={set('end')} />
          </div>
        </div>
        <div className="field">
          <label htmlFor="es-room">Phòng</label>
          <div className="inp">
            <select id="es-room" value={f.room} onChange={set('room')}>
              <option value="">Chưa xếp phòng</option>
              {ROOMS.map((r) => (
                <option key={r.name} value={r.name}>
                  {r.name} · {capacityText(r)}
                </option>
              ))}
            </select>
          </div>
        </div>
        <div className="field">
          <label htmlFor="es-teacher">Giảng viên dạy thay</label>
          <div className="inp">
            <select id="es-teacher" value={f.teacher} onChange={set('teacher')}>
              {s.teacher && <option value={s.teacher}>Giữ nguyên: {s.teacher}</option>}
              {STAFF.filter((p) => p.role === 2 && p.name !== s.teacher).map((p) => (
                <option key={p.name}>{p.name}</option>
              ))}
            </select>
          </div>
        </div>
      </div>
      <div className="field">
        <label htmlFor="es-reason">Lý do thay đổi</label>
        <div className="inp">
          <input id="es-reason" value={f.reason} placeholder="Ví dụ: giảng viên trùng lịch dạy lớp DATA-K07." onChange={set('reason')} />
        </div>
      </div>
      {changed && !found.length && (
        <Box icon="check" tone="ok" title="Không còn trùng giảng viên và phòng ở khung giờ mới" />
      )}
      {found.length > 0 && (
        <label className="check-list es-override">
          <input type="checkbox" checked={override} onChange={(e) => setOverride(e.target.checked)} />
          Tôi xác nhận ghi đè: vẫn lưu dù đang trùng lịch
        </label>
      )}
      {changed && (
        <Box icon="bell" tone="info" title="Gửi thông báo thay đổi khi lưu">
          {who.students} học viên và {who.staff} giảng viên, trợ giảng của lớp {s.cls} sẽ nhận:
          <q className="es-msg">{msg}</q>
        </Box>
      )}
      {error && <p className="err" style={{ marginBottom: 12 }}>{error}</p>}
      <div className="acts es-acts">
        <button type="button" className="link danger" onClick={onCancel}>
          Huỷ buổi này
        </button>
        <button type="button" className="btn" style={{ width: 90 }} onClick={onClose}>
          Đóng
        </button>
        <button type="button" className="btn primary" style={{ width: 120 }} disabled={busy} onClick={save}>
          Lưu buổi
        </button>
      </div>
    </Modal>
  );
}

type CancelProps = { s: ClassSession; onClose: () => void; onSaved: (msg: string) => void };

/** Ngày học kế tiếp sau buổi cuối của lớp, theo các thứ lớp đang học, bỏ qua ngày nghỉ lễ. */
function makeupDate(cls: string) {
  const list = sessionsOf(cls).filter((x) => !x.cancelled);
  const days = new Set(list.map((x) => dow(x.date)));
  let d = list.at(-1)!.date;
  for (let i = 0; i < 60; i++) {
    d = addDays(d, 1);
    if (days.has(dow(d)) && !holidayOn(d)) return d;
  }
  return addDays(list.at(-1)!.date, 7);
}

// S4-03. Huỷ một buổi bắt buộc nhập lý do; chọn xếp buổi bù vào cuối lịch hoặc để xếp sau. Lớp nhận thông báo.
export function CancelSessionDialog({ s, onClose, onSaved }: CancelProps) {
  const [reason, setReason] = useState('');
  const [makeup, setMakeup] = useState(true);
  const [error, setError] = useState('');
  // Có backend: cả lịch của lớp để tính ngày học bù sau buổi cuối.
  useLoad(() => loadClassSessions(s.cls, TODAY), [s.cls]);
  const [busy, setBusy] = useState(false);
  const who = audience(s.cls);

  function confirm() {
    if (!reason.trim()) return setError('Bắt buộc nhập lý do trước khi huỷ buổi');
    if (apiEnabled) {
      const date = makeup ? makeupDate(s.cls) : undefined;
      setBusy(true);
      cancelSession(s, reason.trim(), date).then(
        () => onSaved(`Đã huỷ buổi ${s.no} lớp ${s.cls}.${date ? ` Học bù ${longDate(date).toLowerCase()} lúc ${s.start}.` : ''}`),
        (e: Error) => (setBusy(false), setError(e.message)),
      );
      return;
    }
    s.cancelled = reason.trim();
    let extra = '';
    if (makeup) {
      const date = makeupDate(s.cls);
      const no = Math.max(...sessionsOf(s.cls).map((x) => x.no)) + 1;
      SESSIONS.push({ ...s, id: `${s.cls}-${no}`, no, date, cancelled: undefined, movedFrom: undefined, makeup: true });
      extra = ` Học bù ${longDate(date).toLowerCase()} lúc ${s.start}.`;
    }
    notify({ at: now(), kind: 'Lịch học', title: `Lớp ${s.cls} huỷ buổi ${s.no}`, body: `Buổi ${s.no} ${longDate(s.date).toLowerCase()} đã huỷ. Lý do: ${s.cancelled}.${extra}`, link: '/lich-hoc', to: { cls: s.cls } });
    saveAll();
    onSaved(`Đã huỷ buổi ${s.no} lớp ${s.cls}.${extra}`);
  }

  return (
    <Modal onClose={onClose} label="Huỷ buổi học">
      <h3 style={{ marginBottom: 4 }}>Huỷ buổi học</h3>
      <p className="hint" style={{ fontSize: 14, marginBottom: 16 }}>
        {s.cls} · Buổi {s.no} · {longDate(s.date)}, {s.start} – {s.end}
        {s.room ? ' · ' + s.room : ''}
      </p>
      <div className="field">
        <label htmlFor="cs-reason">
          Lý do huỷ <span className="pill off">bắt buộc</span>
        </label>
        <div className={'inp' + (error ? ' bad' : '')}>
          <textarea
            id="cs-reason"
            value={reason}
            placeholder="Ví dụ: giảng viên bận đột xuất."
            onChange={(e) => {
              setReason(e.target.value);
              setError('');
            }}
          />
        </div>
        {error && <p className="err">{error}</p>}
      </div>
      <fieldset className="check-list" style={{ marginBottom: 16 }}>
        <legend>Sau khi huỷ</legend>
        <label>
          <input type="radio" name="mk" checked={makeup} onChange={() => setMakeup(true)} />
          Thêm một buổi bù vào cuối lịch
        </label>
        <label>
          <input type="radio" name="mk" checked={!makeup} onChange={() => setMakeup(false)} />
          Không xếp buổi bù, tôi sẽ xếp sau
        </label>
      </fieldset>
      <Box icon="bell" tone="info" title={`${who.students} học viên và ${s.teacher ? 'giảng viên ' + s.teacher : who.staff + ' người dạy'} sẽ nhận thông báo`}>
        Buổi bị huỷ vẫn hiện màu xám trên lịch kèm lý do, không mở điểm danh.
      </Box>
      <div className="acts">
        <button type="button" className="btn" style={{ width: 90 }} onClick={onClose}>
          Đóng
        </button>
        <button type="button" className="btn danger" style={{ width: 120 }} disabled={busy} onClick={confirm}>
          <Icon name="x" size={16} />
          Huỷ buổi
        </button>
      </div>
    </Modal>
  );
}
