import { useState } from 'react';
import { Icon } from '../../components/Icon';
import { useToast } from '../../components/Toast';
import { Drawer, Pill } from '../../components/ui';
import { fileSize, isPublished, lateHours, vdt, type Assignment, type Submission } from '../../data/assignments';
import { TODAY } from '../../data/funnel';
import { notify } from '../../data/notifications';
import { saveAll } from '../../data/store';
import type { SubRow } from '../S5-10-bai-nop/SubmissionsPage';
import './versions.css';

// S5-08. Mỗi lần nộp là một phiên bản riêng, không ghi đè bản cũ. Bản mới nhất là bản được chấm.
export function VersionList({ a, sub }: { a: Assignment; sub: Submission }) {
  return (
    <ol className="vs-list">
      {sub.versions.map((v, i) => {
        const late = lateHours(a, v);
        return (
          <li key={v.no}>
            <div className="vs-top">
              <b>Bản {v.no}</b>
              {i === 0 && <Pill tone="info">Mới nhất</Pill>}
              {i === 0 && sub.state === 'Đã chấm' && isPublished(sub.grade) && <Pill tone="ok">Bản được chấm</Pill>}
              {late > 0 && <Pill tone="off">Trễ {late} giờ</Pill>}
              <small>{vdt(v.at)}</small>
            </div>
            {v.note && <p className="hint">{v.note}</p>}
            <ul className="as-files">
              {v.files.map((f) => (
                <li key={f.name}>
                  <Icon name="file" size={16} />
                  <span>{f.name}</span>
                  <small>{fileSize(f.size)}</small>
                </li>
              ))}
              {v.repo && (
                <li>
                  <Icon name="go" size={16} />
                  <a href={v.repo} target="_blank" rel="noreferrer">
                    {v.repo.replace(/^https:\/\//, '')}
                  </a>
                </li>
              )}
            </ul>
          </li>
        );
      })}
    </ol>
  );
}

/** Cột thao tác ở danh sách bài nộp: mở lịch sử nộp của học viên; bài đã chấm thì mở lại được cho học viên nộp lại. */
export function HistoryAction({ a, r, onChange }: { a: Assignment; r: SubRow; onChange: () => void }) {
  const toast = useToast();
  const [open, setOpen] = useState(false);
  const sub = r.sub!;
  return (
    <>
      <button type="button" className="link" onClick={() => setOpen(true)}>
        Lịch sử nộp
      </button>
      {open && (
        <Drawer
          title={'Lịch sử nộp của ' + r.st.name}
          onClose={() => setOpen(false)}
          footer={
            sub.state === 'Đã chấm' && !sub.reopened ? (
              <button
                type="button"
                className="btn"
                onClick={() => {
                  sub.reopened = true;
                  notify({ at: TODAY + 'T' + new Date().toTimeString().slice(0, 5), kind: 'Bài tập', title: 'Bài đã được mở lại: ' + a.title.split(':')[0], body: 'Giảng viên đã mở lại bài, bạn có thể nộp phiên bản mới.', link: '/bai-tap-cua-toi/' + a.id, to: { users: [r.st.name] } });
                  saveAll();
                  onChange();
                  toast('Đã mở lại bài cho ' + r.st.name);
                }}
              >
                <Icon name="lock" size={16} />
                Mở lại bài cho học viên nộp lại
              </button>
            ) : undefined
          }
        >
          <p className="hint" style={{ marginBottom: 16 }}>
            {r.st.code} · {sub.versions.length} phiên bản · bản mới nhất được dùng để chấm
            {sub.reopened ? ' · đã mở lại cho nộp lại' : ''}
          </p>
          <VersionList a={a} sub={sub} />
        </Drawer>
      )}
    </>
  );
}
