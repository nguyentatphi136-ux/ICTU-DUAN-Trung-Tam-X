import { useState } from 'react';
import { AppLayout } from '../../components/AppLayout';
import { useToast } from '../../components/Toast';
import { MODULE, TrainingTabs, useCanEditTraining } from '../../components/TrainingTabs';
import { Pill } from '../../components/ui';
import { DEFAULT_THRESHOLD, flagAll, FLAG_TO, THRESHOLDS } from '../../data/attendance';
import { PROGRAM_SUBJECTS } from '../../data/classes';
import { saveAll } from '../../data/store';
import { PROGRAMS } from '../../data/training';
import { ErrorPage } from '../S1-07-trang-loi/ErrorPage';
import './threshold.css';

// S5-02. Ngưỡng vắng cấu hình theo từng chương trình, mặc định 20% số buổi. Học viên vượt ngưỡng bị gắn cờ trên danh sách lớp
// và hồ sơ; giảng viên đang dạy lớp và Quản lý đào tạo nhận thông báo khi cờ được bật (tuỳ chọn thêm tư vấn viên).
export function ThresholdPage() {
  const toast = useToast();
  const canEdit = useCanEditTraining();
  const programs = PROGRAMS.filter((p) => p.active && PROGRAM_SUBJECTS[p.name]);
  const [values, setValues] = useState<Record<string, string>>(() => Object.fromEntries(programs.map((p) => [p.name, String(THRESHOLDS[p.name] ?? DEFAULT_THRESHOLD)])));
  const [counselor, setCounselor] = useState(FLAG_TO.counselor);
  const [error, setError] = useState('');
  if (!canEdit) return <ErrorPage code="403" />;
  const total = (name: string) => PROGRAM_SUBJECTS[name]().reduce((n, s) => n + s.sessions, 0);

  function save() {
    for (const p of programs) {
      const v = Number(values[p.name]);
      if (!Number.isFinite(v) || v < 1 || v > 50) return setError(`Ngưỡng của ${p.name} phải từ 1 đến 50%`);
    }
    for (const p of programs) {
      const v = Number(values[p.name]);
      if (v === DEFAULT_THRESHOLD) delete THRESHOLDS[p.name];
      else THRESHOLDS[p.name] = v;
    }
    FLAG_TO.counselor = counselor;
    const n = flagAll();
    saveAll();
    setError('');
    toast('Đã lưu ngưỡng vắng', n ? `${n} học viên vừa bị gắn cờ, thông báo đã gửi.` : 'Ngưỡng mới áp dụng cho các buổi từ nay.');
  }

  return (
    <AppLayout crumb={[MODULE, 'Ngưỡng vắng']} module={MODULE}>
      <div className="h">
        <div>
          <h2>Ngưỡng vắng theo chương trình</h2>
          <p>Học viên vắng quá ngưỡng sẽ được gắn cờ và hệ thống gửi thông báo</p>
        </div>
      </div>
      <TrainingTabs />
      <div className="th-grid">
        <div>
          <div className="panel">
            <div className="scroll">
              <table>
                <thead>
                  <tr>
                    <th>Mã</th>
                    <th>Chương trình</th>
                    <th>Tổng số buổi</th>
                    <th>Ngưỡng vắng</th>
                    <th>Tương đương</th>
                    <th />
                  </tr>
                </thead>
                <tbody>
                  {programs.map((p) => {
                    const v = Number(values[p.name]);
                    const changed = v !== DEFAULT_THRESHOLD;
                    return (
                      <tr key={p.code}>
                        <td className="strong">{p.code}</td>
                        <td className="strong">{p.name}</td>
                        <td>{total(p.name)}</td>
                        <td>
                          <span className="th-inp">
                            <input type="number" min={1} max={50} aria-label={'Ngưỡng vắng ' + p.name} value={values[p.name]} onChange={(e) => setValues({ ...values, [p.name]: e.target.value })} />%
                          </span>
                        </td>
                        <td>tối đa {Math.floor((total(p.name) * (v || 0)) / 100)} buổi</td>
                        <td className="th-state">
                          {changed ? <Pill tone="info">Đã chỉnh</Pill> : <Pill>Mặc định</Pill>}
                          {changed && (
                            <button type="button" className="link" onClick={() => setValues({ ...values, [p.name]: String(DEFAULT_THRESHOLD) })}>
                              Về mặc định
                            </button>
                          )}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>
          <div className="box info" style={{ marginTop: 16 }}>
            <div>
              <b>Cách tính</b>
              Số buổi vắng gồm vắng có phép và vắng không phép, so với tổng số buổi của lớp. Ngưỡng mới áp dụng cho các lớp đang chạy ngay khi lưu. Đi muộn vẫn tính là có mặt.
            </div>
          </div>
          {error && <p className="err">{error}</p>}
          <div className="acts" style={{ justifyContent: 'flex-start' }}>
            <button type="button" className="btn" style={{ width: 100 }} onClick={() => setValues(Object.fromEntries(programs.map((p) => [p.name, String(THRESHOLDS[p.name] ?? DEFAULT_THRESHOLD)])))}>
              Huỷ
            </button>
            <button type="button" className="btn primary" style={{ width: 140 }} onClick={save}>
              Lưu ngưỡng
            </button>
          </div>
        </div>
        <aside className="panel sp-card th-to">
          <h3>Khi cờ được bật, thông báo gửi tới</h3>
          <label className="check-list">
            <input type="checkbox" checked disabled /> Giảng viên đang dạy lớp
          </label>
          <label className="check-list">
            <input type="checkbox" checked disabled /> Quản lý đào tạo
          </label>
          <label className="check-list">
            <input type="checkbox" checked={counselor} onChange={(e) => setCounselor(e.target.checked)} /> Tư vấn viên của học viên
          </label>
          <p className="hint">Hai mục đầu là bắt buộc theo yêu cầu.</p>
        </aside>
      </div>
    </AppLayout>
  );
}
