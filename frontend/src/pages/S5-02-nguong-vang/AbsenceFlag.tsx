import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Icon } from '../../components/Icon';
import { FLAGS, statOf } from '../../data/attendance';
import { viDate } from '../../data/leads';
import './threshold.css';

type Props = { code: string; cls: string; program: string; total: number; canNote: boolean; onNote: (text: string) => void };

// S5-02. Cờ vượt ngưỡng vắng trên hồ sơ học viên: số buổi vắng (có phép, không phép), ngưỡng, nút xem chuyên cần và ghi chú đã liên hệ.
export function AbsenceFlag({ code, cls, program, total, canNote, onNote }: Props) {
  const [noted, setNoted] = useState(false);
  const a = statOf(code, cls, program, total);
  if (!a.over) return null;
  const since = FLAGS[`${cls}:${code}`];
  return (
    <div className="box bad flag-box">
      <Icon name="alert" size={20} />
      <div>
        <b>Vượt ngưỡng vắng{since ? ' từ ' + viDate(since) : ''}</b>
        Vắng {a.absent}/{total} buổi ({a.excused} có phép, {a.unexcused} không phép). Ngưỡng của chương trình là {a.max} buổi.
        <div className="acts">
          <Link to={'/chuyen-can/' + cls} className="btn">
            Xem chuyên cần
          </Link>
          {canNote && (
            <button
              type="button"
              className="btn"
              disabled={noted}
              onClick={() => {
                onNote(`Đã liên hệ học viên về việc vắng ${a.absent} buổi ở lớp ${cls}.`);
                setNoted(true);
              }}
            >
              {noted ? 'Đã ghi chú' : 'Ghi chú đã liên hệ'}
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
