import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useToast } from '../../components/Toast';
import { Box, Modal } from '../../components/ui';
import { apiEnabled } from '../../data/api';
import { classSize, isOpen, type Klass } from '../../data/classes';
import { setClassRoom } from '../../data/s3api';
import { capacityText, classesIn, findRoom, roomHref, ROOMS, type Room } from '../../data/rooms';
import { saveAll } from '../../data/store';
import './room.css';

type Props = { k: Klass; manage: boolean; onChange: () => void };

// S3-09. Tab Phòng học của một lớp: xem phòng đang xếp và xếp phòng khác.
// Sĩ số tối đa của lớp vượt sức chứa phòng thì cảnh báo và gợi ý phòng đủ chỗ; vẫn cho xếp nếu người dùng xác nhận.
export function RoomTab({ k, manage, onChange }: Props) {
  const toast = useToast();
  const [picking, setPicking] = useState(false);
  const room = findRoom(k.room);
  const over = room?.capacity && room.capacity < k.max;

  return (
    <>
      <div className="panel sp-card rm-current">
        <div>
          <span className="hint">Phòng đang xếp</span>
          {room ? (
            <>
              <b>{room.name}</b>
              <span>
                {room.kind} ·{' '}
                {room.kind === 'Trực tuyến' ? (
                  <a href={roomHref(room.where)} target="_blank" rel="noreferrer">
                    {room.where}
                  </a>
                ) : (
                  room.where
                )}{' '}
                · {capacityText(room)}
              </span>
            </>
          ) : (
            <b>Chưa xếp phòng</b>
          )}
        </div>
        {manage && isOpen(k) && (
          <button type="button" className="btn" onClick={() => setPicking(true)}>
            {room ? 'Đổi phòng' : 'Xếp phòng'}
          </button>
        )}
      </div>
      {over && (
        <Box icon="alert" tone="warn" title="Sĩ số lớp vượt sức chứa phòng">
          {room!.name} có {room!.capacity} chỗ, lớp {k.code} có sĩ số tối đa {k.max}.
        </Box>
      )}
      <p className="hint">
        Quản lý danh sách phòng ở trang <Link to="/phong-hoc">Phòng học</Link>.
      </p>
      {picking && (
        <PickRoom
          k={k}
          onClose={() => setPicking(false)}
          onPick={(r) => {
            if (apiEnabled)
              return void setClassRoom(k.code, r.id).then(
                () => (setPicking(false), onChange(), toast(`Đã xếp phòng ${r.name} cho lớp ${k.code}`)),
                (e: Error) => toast('Chưa xếp được phòng', e.message),
              );
            k.room = r.name;
            saveAll();
            setPicking(false);
            onChange();
            toast(`Đã xếp phòng ${r.name} cho lớp ${k.code}`);
          }}
        />
      )}
    </>
  );
}

function PickRoom({ k, onClose, onPick }: { k: Klass; onClose: () => void; onPick: (r: Room) => void }) {
  const [sel, setSel] = useState<Room | undefined>(findRoom(k.room));
  const fits = (r: Room) => !r.capacity || r.capacity >= k.max;
  const tooSmall = sel && !fits(sel);
  const better = ROOMS.filter((r) => r.kind === 'Vật lý' && fits(r) && r !== sel).map((r) => r.name);
  const group = (kind: Room['kind']) => ROOMS.filter((r) => r.kind === kind);

  const option = (r: Room) => {
    const busy = classesIn(r.name).filter((c) => c.code !== k.code);
    return (
      <label key={r.name} className={'rm-opt' + (sel === r ? ' on' : '')}>
        <input type="radio" name="room" checked={sel === r} onChange={() => setSel(r)} />
        <span>
          <b>{r.name}</b>
          <small>
            {r.where}
            {busy.length ? ' · đang dùng cho ' + busy.map((c) => c.code).join(', ') : ''}
          </small>
        </span>
        <em className={fits(r) ? undefined : 'warn'}>{capacityText(r)}</em>
      </label>
    );
  };

  return (
    <Modal onClose={onClose} label={'Xếp phòng cho lớp ' + k.code} wide>
      <h3 style={{ marginBottom: 4 }}>Xếp phòng cho lớp {k.code}</h3>
      <p className="hint" style={{ fontSize: 14, marginBottom: 16 }}>
        Sĩ số tối đa {k.max} · đang có {classSize(k.code)} học viên · hình thức {k.mode}
      </p>
      <span className="lbl">Phòng vật lý</span>
      <div className="rm-opts">{group('Vật lý').map(option)}</div>
      {tooSmall && (
        <Box icon="alert" tone="warn" title="Sĩ số lớp vượt sức chứa phòng">
          {sel!.name} chỉ có {sel!.capacity} chỗ, lớp {k.code} có sĩ số tối đa {k.max}. {better.length ? `Chọn ${better.slice(0, 2).join(' hoặc ')}, hoặc giảm sĩ số tối đa của lớp.` : 'Giảm sĩ số tối đa của lớp hoặc chọn phòng trực tuyến.'}
        </Box>
      )}
      <span className="lbl">Phòng trực tuyến</span>
      <div className="rm-opts">{group('Trực tuyến').map(option)}</div>
      <div className="acts">
        <button type="button" className="btn" style={{ width: 100 }} onClick={onClose}>
          Huỷ
        </button>
        <button type="button" className={'btn ' + (tooSmall ? 'outline-danger' : 'primary')} disabled={!sel} onClick={() => sel && onPick(sel)}>
          {tooSmall ? 'Vẫn xếp phòng này' : 'Xếp phòng'}
        </button>
      </div>
    </Modal>
  );
}

