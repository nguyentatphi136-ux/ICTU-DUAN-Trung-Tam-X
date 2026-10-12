import { useState, type ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { useToast } from '../../components/Toast';
import { Box, Drawer, Pill } from '../../components/ui';
import { useAuth } from '../../data/auth';
import { sheetOf, summary } from '../../data/attendance';
import { clashes, clashText, isPast, longDate, type ClassSession } from '../../data/schedule';
import { CancelSessionDialog, EditSessionDialog } from '../S4-03-sua-buoi/SessionDialogs';
import { canManageClasses } from '../S3-07-danh-sach-lop/ClassListPage';

type Props = { s: ClassSession; onClose: () => void; onChange: () => void };

/** Trạng thái hiển thị của một buổi. */
export const sessionState = (s: ClassSession): [string, 'ok' | 'off' | 'info' | 'wait' | undefined] =>
  s.cancelled ? ['Đã huỷ', 'off'] : sheetOf(s) ? ['Đã điểm danh', 'ok'] : isPast(s) ? ['Chưa điểm danh', 'wait'] : ['Sắp tới', 'info'];

/** Buổi đã điểm danh (S4-07) thì khoá ngày, giờ, phòng, giảng viên và không huỷ được. */
export const isLocked = (s: ClassSession) => !!sheetOf(s);

// S4-04. Bấm vào một buổi trên lịch để xem chi tiết: thời gian, phòng, giảng viên, bài học, xung đột nếu có.
// S4-03. Quản lý đào tạo sửa, dời hoặc huỷ buổi ngay trong ngăn này; buổi đã điểm danh thì hai nút bị tắt kèm lý do.
export function SessionDrawer({ s, onClose, onChange }: Props) {
  const { user } = useAuth();
  const toast = useToast();
  const [dialog, setDialog] = useState<'edit' | 'cancel' | null>(null);
  const manage = canManageClasses(user?.active ?? -1);
  const locked = isLocked(s);
  const done = (msg: string) => {
    setDialog(null);
    onChange();
    toast(msg);
  };
  const [state, tone] = sessionState(s);
  const found = clashes(s);
  const facts: [string, ReactNode][] = [
    ['Thời gian', `${longDate(s.date)}, ${s.start} – ${s.end}`],
    ['Phòng', s.room ?? 'Chưa xếp phòng'],
    ['Giảng viên', s.teacher ?? 'Chưa có giảng viên'],
    ['Bài học', `${s.subject} · Bài ${s.lesson}: ${s.topic}`],
    ...(sheetOf(s) ? [['Điểm danh', <Link key="a" to={'/diem-danh/' + s.id}>{summary(sheetOf(s))}</Link>] as [string, ReactNode]] : []),
  ];
  return (
    <Drawer
      title="Chi tiết buổi học"
      onClose={onClose}
      footer={
        manage && !s.cancelled ? (
          <>
            <button type="button" className="btn" disabled={locked} onClick={() => setDialog('edit')}>
              Sửa hoặc dời
            </button>
            <button type="button" className="btn outline-danger" disabled={locked} onClick={() => setDialog('cancel')}>
              Huỷ buổi
            </button>
          </>
        ) : undefined
      }
    >
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
      {manage && locked && !s.cancelled && (
        <Box icon="lock" tone="warn" title="Không sửa được buổi đã điểm danh">
          Ngày, giờ, phòng và giảng viên của buổi này đã khoá. Buổi cũng không huỷ được.
        </Box>
      )}
      {dialog === 'edit' && <EditSessionDialog s={s} onClose={() => setDialog(null)} onSaved={done} onCancel={() => setDialog('cancel')} />}
      {dialog === 'cancel' && <CancelSessionDialog s={s} onClose={() => setDialog(null)} onSaved={done} />}
    </Drawer>
  );
}
