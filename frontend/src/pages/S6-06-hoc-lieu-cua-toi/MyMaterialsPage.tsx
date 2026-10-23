import { useState } from 'react';
import { Navigate } from 'react-router-dom';
import { AppLayout } from '../../components/AppLayout';
import { Icon } from '../../components/Icon';
import { Filter, Pill, Search } from '../../components/ui';
import { fileSize } from '../../data/assignments';
import { useAuth } from '../../data/auth';
import { findClass } from '../../data/classes';
import { TODAY } from '../../data/funnel';
import { EARLY, materialsOf, type Material } from '../../data/materials';
import { addDays, longDate, sessionsOf, type ClassSession } from '../../data/schedule';
import { useMe } from '../S4-05-lich-cua-toi/MySchedulePage';
import { MODULE } from '../S6-05-hoc-lieu/MaterialsPage';
import '../S6-05-hoc-lieu/materials.css';

const visible = (s: ClassSession) => s.date <= TODAY || !!EARLY[s.id];

// S6-06. Học viên xem và tải tài liệu của lớp mình, nhóm theo buổi học hoặc theo môn. Chỉ hiện tài liệu của buổi đã diễn ra
// hoặc buổi được giảng viên mở sớm; buổi sắp tới chưa mở thì báo tài liệu sẽ hiện sau khi buổi học diễn ra.
export function MyMaterialsPage() {
  const { user } = useAuth();
  const me = useMe();
  const [mode, setMode] = useState<'session' | 'subject'>('session');
  const [subject, setSubject] = useState('');
  const [q, setQ] = useState('');
  if (user && user.active !== 0) return <Navigate to="/hoc-lieu" replace />;
  const k = findClass(me?.cls);
  if (!k)
    return (
      <AppLayout crumb={[MODULE, 'Học liệu của tôi']} module={MODULE}>
        <div className="dash">Bạn chưa được xếp lớp nên chưa có học liệu.</div>
      </AppLayout>
    );
  const kw = q.trim().toLowerCase();
  const match = (m: Material) => !kw || m.name.toLowerCase().includes(kw);
  const sessions = sessionsOf(k.code)
    .filter((s) => !s.cancelled && (!subject || s.subject === subject) && s.date <= addDays(TODAY, 7) && (materialsOf(s.id).length || s.date > TODAY))
    .reverse();
  const subjects = k.subjects.filter((s) => sessionsOf(k.code).some((x) => x.subject === s.code && materialsOf(x.id).length));

  const files = (list: Material[]) => (
    <div className="mm-files">
      {list.filter(match).map((m) =>
        m.url ? (
          <a key={m.id} href={m.url} target="_blank" rel="noreferrer">
            <Icon name="go" size={16} />
            {m.name}
          </a>
        ) : (
          <span key={m.id} title="Bản demo chưa có máy chủ tệp">
            <Icon name="file" size={16} />
            {m.name}
            <small>{fileSize(m.size ?? 0)}</small>
          </span>
        ),
      )}
    </div>
  );

  return (
    <AppLayout crumb={[MODULE, 'Học liệu của tôi']} module={MODULE}>
      <div className="h">
        <div>
          <h2>Học liệu của tôi</h2>
          <p>Lớp {k.code} · tài liệu hiện khi buổi học đã diễn ra hoặc được giảng viên mở sớm</p>
        </div>
      </div>
      <div className="bar mm-bar">
        <div className="seg" role="tablist">
          <button type="button" role="tab" aria-selected={mode === 'session'} className={mode === 'session' ? 'on' : undefined} onClick={() => setMode('session')}>
            Theo buổi học
          </button>
          <button type="button" role="tab" aria-selected={mode === 'subject'} className={mode === 'subject' ? 'on' : undefined} onClick={() => setMode('subject')}>
            Theo môn
          </button>
        </div>
        <Filter label="Môn" value={subject} options={k.subjects.map((s) => s.code)} onChange={setSubject} width={220} />
        <Search placeholder="Tìm tên tài liệu" value={q} onChange={setQ} width={280} />
      </div>
      {mode === 'session'
        ? sessions.map((s) => {
            const open = visible(s);
            const list = materialsOf(s.id);
            if (kw && !list.some(match)) return null;
            return (
              <section key={s.id} className="mm-group">
                <div className="panel">
                  <div className="mm-head">
                    <div>
                      <b>
                        Buổi {s.no} · Bài {s.lesson}: {s.topic}
                      </b>
                      <small>
                        {longDate(s.date)} · {s.date > TODAY ? 'sắp diễn ra' : s.teacher}
                      </small>
                    </div>
                    {s.date > TODAY && open ? <Pill tone="info">Mở sớm</Pill> : list.some((m) => m.at >= addDays(TODAY, -3)) ? <Pill tone="ok">Mới</Pill> : null}
                  </div>
                  {open && list.length ? files(list) : <p className="mm-wait">{open ? 'Buổi này chưa có tài liệu.' : 'Tài liệu sẽ hiện sau khi buổi học diễn ra.'}</p>}
                </div>
              </section>
            );
          })
        : (subject ? subjects.filter((s) => s.code === subject) : subjects).map((sub) => {
            const list = sessionsOf(k.code)
              .filter((s) => s.subject === sub.code && visible(s))
              .flatMap((s) => materialsOf(s.id));
            return (
              <section key={sub.code} className="mm-group">
                <div className="panel">
                  <div className="mm-head">
                    <div>
                      <b>
                        {sub.code} · {sub.name}
                      </b>
                      <small>{list.length} tài liệu</small>
                    </div>
                  </div>
                  {files(list)}
                </div>
              </section>
            );
          })}
      <p className="hint">Chế độ Theo môn gộp toàn bộ tài liệu của một môn thành một danh sách.</p>
    </AppLayout>
  );
}
