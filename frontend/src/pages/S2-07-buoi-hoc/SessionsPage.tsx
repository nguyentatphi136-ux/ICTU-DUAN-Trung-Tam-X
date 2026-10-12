import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { AppLayout } from '../../components/AppLayout';
import { Icon } from '../../components/Icon';
import { useToast } from '../../components/Toast';
import { Box, ConfirmDialog, Modal, Pill } from '../../components/ui';
import { useAuth } from '../../data/auth';
import { LESSONS, SUBJECTS, type Lesson } from '../../data/training';
import { moveToTrash, restore } from '../../data/trash';
import { ErrorPage } from '../S1-07-trang-loi/ErrorPage';
import { MODULE, useCanEditTraining } from '../../components/TrainingTabs';

// S2-07. Buổi học của một môn: số thứ tự, chủ đề, mục tiêu. Không khai báo quá số buổi của môn.
// Nhân bản danh sách buổi từ môn khác, cảnh báo trước khi ghi đè. Khi tích hợp: /training/subjects/:code/sessions.
export function SessionsPage() {
  const code = useParams().code ?? '';
  const subject = SUBJECTS.find((s) => s.code === code);
  const toast = useToast();
  const canEdit = useCanEditTraining();
  const [lessons, setLessons] = useState<Lesson[]>(() => LESSONS[code] ?? []);
  const [editing, setEditing] = useState<number | 'new' | null>(null);
  const [cloning, setCloning] = useState(false);
  const [deleting, setDeleting] = useState<number | null>(null);
  const { user } = useAuth();

  if (!subject) return <ErrorPage code="404" />;

  const commit = (list: Lesson[]) => {
    LESSONS[code] = list;
    setLessons(list);
  };
  const trash = (i: number) => {
    const l = lessons[i];
    const id = moveToTrash({ kind: 'lesson', item: l, subject: code, index: i }, `${subject.name} · Buổi ${i + 1}: ${l.topic}`, user?.name ?? '');
    setLessons([...(LESSONS[code] ?? [])]);
    setDeleting(null);
    toast('Đã chuyển vào thùng rác', `Buổi ${i + 1}: ${l.topic}`, {
      label: 'Hoàn tác',
      onClick: () => {
        restore(id);
        setLessons([...(LESSONS[code] ?? [])]);
      },
    });
  };
  const full = lessons.length >= subject.sessions;
  const left = subject.sessions - lessons.length;

  return (
    <AppLayout crumb={[MODULE, 'Môn học', subject.name]} module={MODULE}>
      <div className="h">
        <div>
          <h2>{subject.name}</h2>
          <p>Khai báo chủ đề và mục tiêu của từng buổi.</p>
        </div>
        {canEdit && (
          <>
            <button type="button" className="btn" style={{ width: 230 }} onClick={() => setCloning(true)}>
              <Icon name="copy" size={16} />
              Nhân bản từ môn khác
            </button>
            <button type="button" className="btn primary" style={{ width: 150 }} disabled={full} onClick={() => setEditing('new')}>
              <Icon name="plus" size={16} />
              Thêm buổi
            </button>
          </>
        )}
      </div>
      <div style={{ marginBottom: 16 }}>
        <Pill tone="info">
          {lessons.length}/{subject.sessions} buổi đã khai báo
        </Pill>
      </div>
      <div className="panel scroll">
        <table>
          <thead>
            <tr>
              <th style={{ width: 80 }}>Buổi</th>
              <th>Chủ đề</th>
              <th>Mục tiêu</th>
              {canEdit && <th style={{ width: 100 }} />}
            </tr>
          </thead>
          <tbody>
            {lessons.map((l, i) => (
              <tr key={i}>
                <td className="strong">{i + 1}</td>
                <td>{l.topic}</td>
                <td>{l.goal}</td>
                {canEdit && (
                  <td style={{ whiteSpace: 'nowrap' }}>
                    <button type="button" className="icon-btn" aria-label={`Sửa buổi ${i + 1}`} onClick={() => setEditing(i)}>
                      <Icon name="edit" />
                    </button>
                    <button
                      type="button"
                      className="icon-btn"
                      aria-label={`Xoá buổi ${i + 1}`}
                      onClick={() => setDeleting(i)}
                    >
                      <Icon name="trash" />
                    </button>
                  </td>
                )}
              </tr>
            ))}
            {!lessons.length && (
              <tr>
                <td colSpan={4} style={{ textAlign: 'center', padding: 40 }}>
                  Chưa có buổi nào.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
      <div style={{ marginTop: 16 }}>
        <Box icon="alert" title={`Môn này có ${subject.sessions} buổi`}>
          {full ? 'Đã khai báo đủ số buổi, nút Thêm buổi đã khoá.' : `Bạn còn khai báo được ${left} buổi nữa. Khi đủ ${subject.sessions} buổi, nút Thêm buổi sẽ bị khoá.`}
        </Box>
      </div>

      {editing !== null && (
        <LessonModal
          index={editing === 'new' ? lessons.length : editing}
          lesson={editing === 'new' ? undefined : lessons[editing]}
          onClose={() => setEditing(null)}
          onSave={(l) => {
            commit(editing === 'new' ? [...lessons, l] : lessons.map((x, j) => (j === editing ? l : x)));
            setEditing(null);
          }}
        />
      )}
      {cloning && (
        <CloneDialog
          target={code}
          targetSessions={subject.sessions}
          current={lessons.length}
          onClose={() => setCloning(false)}
          onClone={(list) => {
            commit(list.slice(0, subject.sessions));
            setCloning(false);
            toast('Đã nhân bản danh sách buổi', `${Math.min(list.length, subject.sessions)} buổi`);
          }}
        />
      )}
      {deleting !== null && lessons[deleting] && (
        <ConfirmDialog title="Chuyển buổi học vào thùng rác?" onClose={() => setDeleting(null)} onConfirm={() => trash(deleting)}>
          Buổi <b>{deleting + 1}: {lessons[deleting].topic}</b> sẽ bị gỡ, các buổi sau được đánh số lại.
        </ConfirmDialog>
      )}
    </AppLayout>
  );
}

function LessonModal({ index, lesson, onClose, onSave }: { index: number; lesson?: Lesson; onClose: () => void; onSave: (l: Lesson) => void }) {
  const [topic, setTopic] = useState(lesson?.topic ?? '');
  const [goal, setGoal] = useState(lesson?.goal ?? '');
  const [touched, setTouched] = useState(false);
  return (
    <Modal onClose={onClose} label={`Buổi ${index + 1}`}>
      <h3>
        {lesson ? 'Sửa' : 'Thêm'} buổi {index + 1}
      </h3>
      <div className="field">
        <label htmlFor="ls-topic">Chủ đề</label>
        <div className={'inp' + (touched && !topic.trim() ? ' bad' : '')}>
          <input id="ls-topic" value={topic} onChange={(e) => setTopic(e.target.value)} />
        </div>
        {touched && !topic.trim() && <p className="err">Nhập chủ đề của buổi</p>}
      </div>
      <div className="field">
        <label htmlFor="ls-goal">Mục tiêu</label>
        <div className="inp">
          <textarea id="ls-goal" value={goal} onChange={(e) => setGoal(e.target.value)} />
        </div>
      </div>
      <div className="acts">
        <button type="button" className="btn" style={{ width: 100 }} onClick={onClose}>
          Huỷ
        </button>
        <button
          type="button"
          className="btn primary"
          style={{ width: 150 }}
          onClick={() => {
            setTouched(true);
            if (topic.trim()) onSave({ topic: topic.trim(), goal: goal.trim() });
          }}
        >
          Lưu buổi
        </button>
      </div>
    </Modal>
  );
}

function CloneDialog({ target, targetSessions, current, onClose, onClone }: { target: string; targetSessions: number; current: number; onClose: () => void; onClone: (l: Lesson[]) => void }) {
  const sources = SUBJECTS.filter((s) => s.code !== target && LESSONS[s.code]?.length);
  const [from, setFrom] = useState(sources[0]?.code ?? '');
  const list = LESSONS[from] ?? [];
  const n = Math.min(list.length, targetSessions);
  return (
    <Modal onClose={onClose} label="Nhân bản danh sách buổi">
      <h3>Nhân bản danh sách buổi</h3>
      <div className="field">
        <label htmlFor="cl-from">Lấy từ môn</label>
        <div className="inp">
          <select id="cl-from" value={from} onChange={(e) => setFrom(e.target.value)}>
            {sources.map((s) => (
              <option key={s.code} value={s.code}>
                {s.code} · {s.name} · {LESSONS[s.code].length} buổi
              </option>
            ))}
          </select>
        </div>
      </div>
      {sources.length ? (
        <Box icon="copy" title={`Sẽ chép ${n} buổi kèm chủ đề và mục tiêu`}>
          {list.length > targetSessions
            ? `Môn đích có ${targetSessions} buổi nên chỉ chép ${targetSessions} buổi đầu, phần vượt sẽ bị bỏ.`
            : `Môn đích có ${targetSessions} buổi nên chép được đủ. Nếu môn nguồn nhiều buổi hơn, phần vượt sẽ bị bỏ.`}
        </Box>
      ) : (
        <p className="hint">Chưa có môn nào khác đã khai báo buổi học.</p>
      )}
      {current > 0 && (
        <Box icon="alert" tone="warn" title={`${current} buổi đang có sẽ bị thay thế`}>
          Thao tác này ghi đè danh sách buổi hiện tại của {target}.
        </Box>
      )}
      <div className="acts">
        <button type="button" className="btn" style={{ width: 100 }} onClick={onClose}>
          Huỷ
        </button>
        <button type="button" className="btn primary" style={{ width: 150 }} disabled={!n} onClick={() => onClone(list)}>
          <Icon name="copy" size={16} />
          Nhân bản
        </button>
      </div>
    </Modal>
  );
}
