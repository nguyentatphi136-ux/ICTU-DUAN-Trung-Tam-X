import { isPublished, score, vdt, type Assignment, type Submission } from '../../data/assignments';
import { contribution, RUBRIC_SCALE } from '../../data/rubrics';
import './view.css';

// S6-04. Học viên xem điểm và nhận xét của bài đã chấm, kèm điểm chi tiết theo rubric nếu có.
// Chỉ hiện sau khi giảng viên công bố; trước đó trang chỉ báo bài đang chờ chấm.
export function GradeView({ a, sub }: { a: Assignment; sub?: Submission }) {
  const g = sub?.grade;
  if (!sub || sub.state === 'Cần làm lại') return null;
  if (!isPublished(g))
    return (
      <p className="gv-wait">
        Bài của bạn đã nộp và đang chờ chấm. Điểm và nhận xét sẽ hiện ở đây khi giảng viên công bố.
      </p>
    );
  const v = sub.versions[0];
  const parts = a.rubric && g!.rubric ? a.rubric.map((c) => contribution(c, g!.rubric![c.name] ?? 0, a.max)) : [];
  return (
    <section className="gv">
      <div className="gv-top">
        <div className="gv-score">
          <span>Điểm của bạn</span>
          <b>
            {score(g!.score)}
            <small> / {a.max}</small>
          </b>
          <em>
            Chấm theo bản {v.no}, nộp {vdt(v.at)}
            {g!.publishedAt ? ` · công bố ${vdt(g!.publishedAt)}` : ''}
          </em>
        </div>
        <div className="gv-comment">
          <span className="lbl">Nhận xét của giảng viên</span>
          <p>{g!.comment}</p>
        </div>
      </div>
      {a.rubric && g!.rubric && (
        <>
          <h3 className="section-t">Điểm chi tiết theo rubric</h3>
          <div className="panel">
            <div className="scroll">
              <table>
                <thead>
                  <tr>
                    <th>Tiêu chí</th>
                    <th>Trọng số</th>
                    <th>Điểm</th>
                    <th>Góp vào điểm tổng</th>
                  </tr>
                </thead>
                <tbody>
                  {a.rubric.map((c, i) => (
                    <tr key={c.name}>
                      <td className="strong">
                        {c.name}
                        {c.hint && <small className="lv-sub">{c.hint}</small>}
                      </td>
                      <td>{c.weight}%</td>
                      <td className="strong">
                        {score(g!.rubric![c.name] ?? 0)} / {RUBRIC_SCALE}
                      </td>
                      <td className="strong">{score(parts[i])}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
          <p className="hint" style={{ marginTop: 8 }}>
            Tổng: {parts.map(score).join(' + ')} = {score(g!.score)}
          </p>
        </>
      )}
    </section>
  );
}
