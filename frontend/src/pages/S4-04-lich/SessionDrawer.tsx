import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { Box, Drawer, Pill } from '../../components/ui';
import { clashes, clashText, isPast, longDate, type ClassSession } from '../../data/schedule';

type Props = { s: ClassSession; onClose: () => void; onChange: () => void };

/** Trạng thái hiển thị của một buổi. */
export const sessionState = (s: ClassSession): [string, 'ok' | 'off' | 'info' | 'wait' | undefined] =>
  s.cancelled ? ['Đã huỷ', 'off'] : isPast(s) ? ['Đã diễn ra', undefined] : ['Sắp tới', 'info'];

// S4-04. Bấm vào một buổi trên lịch để xem chi tiết: thời gian, phòng, giảng viên, bài học, xung đột nếu có.
export function SessionDrawer({ s, onClose }: Props) {
  const [state, tone] = sessionState(s);
  const found = clashes(s);
  const facts: [string, ReactNode][] = [
    ['Thời gian', `${longDate(s.date)}, ${s.start} – ${s.end}`],
    ['Phòng', s.room ?? 'Chưa xếp phòng'],
    ['Giảng viên', s.teacher ?? 'Chưa có giảng viên'],
    ['Bài học', `${s.subject} · Bài ${s.lesson}: ${s.topic}`],
  ];
  return (
    <Drawer title="Chi tiết buổi học" onClose={onClose}>
      <div className="sd-head">
        <h3>
          <Link to={'/lop-hoc/' + s.cls}>{s.cls}</Link> · {s.makeup ? 'Buổi học bù' : 'Buổi ' + s.no}
        </h3>
        <Pill tone={tone}>{state}</Pill>
      </div>
      <dl className="sd-facts">
        {facts.map(([k, v]) => (
          <div key={k}>
            <dt>{k}</dt>
            <dd>{v}</dd>
          </div>
        ))}
      </dl>
      {s.movedFrom && <p className="hint">Đã dời từ {longDate(s.movedFrom.slice(0, 10))} {s.movedFrom.slice(11)}.</p>}
      {s.cancelled && (
        <Box icon="x" tone="bad" title="Buổi đã huỷ">
          Lý do: {s.cancelled}
        </Box>
      )}
      {found.map((c, i) => (
        <Box key={i} icon="alert" tone="bad" title={c.kind}>
          {clashText(c)}
        </Box>
      ))}
    </Drawer>
  );
}
