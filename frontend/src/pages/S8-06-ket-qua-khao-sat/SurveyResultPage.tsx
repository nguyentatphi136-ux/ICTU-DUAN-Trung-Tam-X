import { useState, type CSSProperties } from 'react';
import { AppLayout } from '../../components/AppLayout';
import { Icon } from '../../components/Icon';
import { Filter } from '../../components/ui';
import { useAuth } from '../../data/auth';
import { findClass, mainTeacher } from '../../data/classes';
import { TODAY } from '../../data/funnel';
import { RUNS, type SurveyRun } from '../../data/surveys';
import { download, toXlsx } from '../../data/xlsx';
import { MODULE } from '../S8-05-khao-sat/SurveyPages';
import './result.css';

const one = (n: number) => n.toLocaleString('vi-VN', { minimumFractionDigits: 1, maximumFractionDigits: 1 });
const teacherOf = (r: SurveyRun) => mainTeacher(findClass(r.cls)!) ?? '–';
const avgOf = (r: SurveyRun, q: string) => {
  const v = r.answers.map((a) => a.scores[q]).filter((x): x is number => x != null);
  return v.length ? v.reduce((a, b) => a + b, 0) / v.length : null;
};
const overall = (r: SurveyRun) => {
  const v = r.questions.filter((q) => q.kind === 'scale').map((q) => avgOf(r, q.id)).filter((x): x is number => x != null);
  return v.length ? v.reduce((a, b) => a + b, 0) / v.length : null;
};
const label = (r: SurveyRun) => `${r.cls} · ${r.title.replace('Khảo sát ', '')}`;

// S8-06. Kết quả khảo sát: điểm trung bình từng câu theo lớp hoặc theo giảng viên, so sánh các lớp của một giảng viên,
// câu trả lời tự luận không kèm danh tính, xuất Excel.
export function SurveyResultPage() {
  const { user } = useAuth();
  const teacherOnly = user?.active === 2;
  const runs = RUNS.filter((r) => r.answers.length && (!teacherOnly || teacherOf(r) === user!.name));
  const teachers = [...new Set(runs.map(teacherOf))];
  const [by, setBy] = useState<'cls' | 'teacher'>('teacher');
  const [teacher, setTeacher] = useState(teacherOnly ? user!.name : (teachers.find((t) => runs.filter((r) => teacherOf(r) === t).length > 1) ?? teachers[0] ?? ''));
  const [runId, setRunId] = useState(runs[0]?.id);
  const shown = by === 'teacher' ? runs.filter((r) => teacherOf(r) === teacher) : runs.filter((r) => r.id === runId);
  const scale = (shown[0]?.questions ?? []).filter((q) => q.kind === 'scale');
  const texts = (shown[0]?.questions ?? []).filter((q) => q.kind === 'text');
  const answered = shown.reduce((n, r) => n + r.responded.length, 0);
  const size = shown.reduce((n, r) => n + r.size, 0);
  // Mức chênh lớn nhất giữa các lớp trên cùng một câu.
  let gap: { q: string; lo: SurveyRun; hi: SurveyRun; d: number } | null = null;
  if (shown.length > 1)
    for (const q of scale) {
      const vals = shown.map((r) => ({ r, v: avgOf(r, q.id) ?? 0 })).sort((a, b) => a.v - b.v);
      const d = vals.at(-1)!.v - vals[0].v;
      if (!gap || d > gap.d) gap = { q: q.text, lo: vals[0].r, hi: vals.at(-1)!.r, d };
    }

  function exportXlsx() {
    download(
      toXlsx(
        [
          ['Câu hỏi', ...shown.map(label)],
          ...scale.map((q) => [q.text, ...shown.map((r) => Math.round((avgOf(r, q.id) ?? 0) * 100) / 100)]),
          ['Đã trả lời', ...shown.map((r) => `${r.responded.length}/${r.size}`)],
          [],
          ['Câu trả lời tự luận (không kèm tên)'],
          ...shown.flatMap((r) => texts.flatMap((q) => r.answers.map((a) => a.texts[q.id]).filter(Boolean).map((t) => [r.cls, t]))),
        ],
        'Kết quả',
      ),
      `ket-qua-khao-sat-${TODAY}.xlsx`,
    );
  }

  return (
    <AppLayout crumb={[MODULE, 'Kết quả khảo sát']} module={MODULE}>
      <div className="h">
        <div>
          <h2>Kết quả khảo sát</h2>
          <p>
            {answered} trên {size} học viên đã trả lời{by === 'teacher' ? ` · ${shown.length} lớp của giảng viên đang chọn` : ''}
          </p>
        </div>
        <button type="button" className="btn" disabled={!shown.length} onClick={exportXlsx}>
          <Icon name="dl" size={16} />
          Xuất Excel
        </button>
      </div>
      <div className="bar rs-bar">
        <span className="hint">Xem theo</span>
        <div className="seg" role="radiogroup" aria-label="Xem theo">
          {(
            [
              ['cls', 'Lớp'],
              ['teacher', 'Giảng viên'],
            ] as const
          ).map(([k, l]) => (
            <button key={k} type="button" role="radio" aria-checked={by === k} className={by === k ? 'on' : undefined} onClick={() => setBy(k)}>
              {l}
            </button>
          ))}
        </div>
        {by === 'teacher' ? (
          !teacherOnly && <Filter label="Giảng viên" value={teacher} options={teachers} onChange={(v) => v && setTeacher(v)} width={280} />
        ) : (
          <label className="filter" style={{ width: 300 }}>
            <span>Đợt: {shown[0] ? label(shown[0]) : '–'}</span>
            <Icon name="down" />
            <select aria-label="Đợt khảo sát" value={runId} onChange={(e) => setRunId(+e.target.value)}>
              {runs.map((r) => (
                <option key={r.id} value={r.id}>
                  {label(r)}
                </option>
              ))}
            </select>
          </label>
        )}
      </div>
      {!shown.length ? (
        <p className="empty">Chưa có kết quả khảo sát.</p>
      ) : (
        <>
          <div className="rs-grid">
            <section className="panel fe-pad">
              <h3>{by === 'teacher' ? `Điểm trung bình từng câu, so sánh các lớp của ${teacher}` : `Điểm trung bình từng câu · ${label(shown[0])}`}</h3>
              <div className="rs-legend">
                {shown.map((r, i) => (
                  <span key={r.id} className={'k' + (i % 3)}>
                    {label(r)} ({r.responded.length} trả lời)
                  </span>
                ))}
              </div>
              {scale.map((q) => (
                <div key={q.id} className="rs-q">
                  <span>{q.text}</span>
                  <div>
                    {shown.map((r, i) => {
                      const v = avgOf(r, q.id) ?? 0;
                      return (
                        <div key={r.id} className={'rs-bar-row k' + (i % 3) + (v < 3.5 ? ' low' : '')}>
                          <i style={{ '--w': `${(100 * v) / 5}%` } as CSSProperties} />
                          <b>{one(v)}</b>
                        </div>
                      );
                    })}
                  </div>
                </div>
              ))}
              {gap && gap.d >= 0.3 && (
                <p className="rs-gap">
                  Câu "{gap.q}" ở lớp {gap.lo.cls} thấp hơn lớp {gap.hi.cls} {one(gap.d)} điểm, mức chênh lớn nhất.
                </p>
              )}
            </section>
            <aside className="panel fe-pad rs-texts">
              <div className="fe-head" style={{ padding: 0 }}>
                <h3>Câu trả lời tự luận</h3>
                <span className="hint">Không kèm tên</span>
              </div>
              {texts.map((q) => {
                const all = shown.flatMap((r) => r.answers.map((a) => a.texts[q.id]).filter(Boolean));
                return (
                  <div key={q.id}>
                    <p className="hint">
                      {q.text} · {all.length} câu trả lời
                    </p>
                    {shown.map((r) => {
                      const list = r.answers.map((a) => a.texts[q.id]).filter(Boolean);
                      return (
                        list.length > 0 && (
                          <div key={r.id} className="rs-cls">
                            <span className="rs-tag">{r.cls}</span>
                            {[...new Set(list)].map((t) => (
                              <blockquote key={t}>{t}</blockquote>
                            ))}
                          </div>
                        )
                      );
                    })}
                  </div>
                );
              })}
            </aside>
          </div>
          <div className="panel" style={{ marginTop: 16 }}>
            <table>
              <thead>
                <tr>
                  <th>Lớp</th>
                  <th>Đợt</th>
                  <th>Giảng viên</th>
                  <th>Đã trả lời</th>
                  <th>Điểm trung bình</th>
                </tr>
              </thead>
              <tbody>
                {shown.map((r) => (
                  <tr key={r.id}>
                    <td className="code">{r.cls}</td>
                    <td>{r.title}</td>
                    <td>{teacherOf(r)}</td>
                    <td>
                      {r.responded.length} / {r.size}
                    </td>
                    <td className="strong">{overall(r) == null ? '–' : `${one(overall(r)!)} / 5`}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}
    </AppLayout>
  );
}
