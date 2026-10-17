import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { Icon } from '../../components/Icon';
import { useToast } from '../../components/Toast';
import { Modal } from '../../components/ui';
import { heldSessions, sheetOf, type Mark } from '../../data/attendance';
import { TODAY } from '../../data/funnel';
import { dm } from '../../data/schedule';
import { download, toXlsx, type Cell } from '../../data/xlsx';
import type { Row } from '../S5-01-chuyen-can/MatrixPage';

const SYMBOL: Record<Mark, string> = { 'Có mặt': 'C', 'Đi muộn': 'M', 'Vắng có phép': 'P', 'Vắng không phép': 'K' };

/** Tên tệp có mã lớp và ngày xuất, ví dụ chuyen-can_WEB-K15_2026-10-19.xlsx. */
export const exportName = (cls: string) => `chuyen-can_${cls}_${TODAY}.xlsx`;

/** Bảng giá trị giữ đúng cấu trúc ma trận trên màn hình và hai cột tổng. */
export function matrixRows(cls: string, rows: Row[]): Cell[][] {
  const sessions = heldSessions(cls).filter((s) => sheetOf(s));
  return [
    ['Mã học viên', 'Họ tên', ...sessions.map((s, i) => `Buổi ${i + 1} (${dm(s.date)})`), 'Số buổi vắng', 'Chuyên cần (%)'],
    ...rows.map(({ s, stat }) => [s.code, s.name, ...sessions.map((x) => SYMBOL[sheetOf(x)!.marks[s.code]] ?? ''), stat.absent, stat.rate]),
  ];
}

// S5-04. Xuất bảng chuyên cần ra tệp Excel: giữ nguyên cấu trúc ma trận và hai cột tổng, tên tệp chứa mã lớp và ngày xuất.
export function ExportButton({ rows, filtered }: { rows: Row[]; filtered: Row[] }) {
  const { cls = '' } = useParams();
  const toast = useToast();
  const [open, setOpen] = useState(false);
  const [scope, setScope] = useState<'all' | 'filtered'>('all');
  const sessions = heldSessions(cls).filter((s) => sheetOf(s)).length;
  const inClass = rows.filter((r) => r.s.cls === cls).length;

  function save() {
    download(toXlsx(matrixRows(cls, scope === 'all' ? rows : filtered), 'Chuyên cần ' + cls), exportName(cls));
    setOpen(false);
    toast('Đã tải tệp ' + exportName(cls));
  }

  return (
    <>
      <button type="button" className="btn" onClick={() => setOpen(true)}>
        <Icon name="dl" size={16} />
        Xuất Excel
      </button>
      {open && (
        <Modal onClose={() => setOpen(false)} label="Xuất bảng chuyên cần ra Excel">
          <h3 style={{ marginBottom: 4 }}>Xuất bảng chuyên cần ra Excel</h3>
          <p className="hint" style={{ fontSize: 14, marginBottom: 16 }}>
            Tệp giữ nguyên cấu trúc ma trận và hai cột tổng như đang thấy trên màn hình.
          </p>
          <div className="field">
            <label htmlFor="ex-name">Tên tệp</label>
            <div className="inp lead">
              <Icon name="file" />
              <input id="ex-name" value={exportName(cls)} disabled />
            </div>
            <p className="hint">Tự đặt theo mã lớp và ngày xuất</p>
          </div>
          <fieldset className="check-list" style={{ marginBottom: 16 }}>
            <legend>Phạm vi</legend>
            <label>
              <input type="radio" name="scope" checked={scope === 'all'} onChange={() => setScope('all')} />
              Cả lớp, {inClass} học viên, {sessions} buổi đã học
            </label>
            <label>
              <input type="radio" name="scope" checked={scope === 'filtered'} onChange={() => setScope('filtered')} />
              Chỉ các dòng đang lọc trên màn hình ({filtered.length} học viên)
            </label>
          </fieldset>
          <span className="lbl">Nội dung tệp</span>
          <ul className="ex-list">
            <li>Một dòng cho mỗi học viên, một cột cho mỗi buổi kèm ngày học</li>
            <li>Ô ghi ký hiệu: C có mặt, M đi muộn, P vắng có phép, K vắng không phép</li>
            <li>Hai cột cuối: số buổi vắng và tỷ lệ chuyên cần</li>
          </ul>
          <div className="acts">
            <button type="button" className="btn" style={{ width: 100 }} onClick={() => setOpen(false)}>
              Huỷ
            </button>
            <button type="button" className="btn primary" style={{ width: 130 }} onClick={save}>
              <Icon name="dl" size={16} />
              Tải tệp
            </button>
          </div>
        </Modal>
      )}
    </>
  );
}
