import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Icon } from '../../components/Icon';
import { useToast } from '../../components/Toast';
import { ConfirmDialog, type MenuAction } from '../../components/ui';
import { apiEnabled } from '../../data/api';
import { useAuth } from '../../data/auth';
import { CLASSES, type Klass } from '../../data/classes';
import { openClass, setClassStatus } from '../../data/s3api';
import { saveAll } from '../../data/store';
import { canManageClasses, ClassListPage } from '../S3-07-danh-sach-lop/ClassListPage';
import { nextStep, OpenClassDrawer } from './OpenClassDrawer';

// S3-05. Trang Lớp học của Quản lý đào tạo: nút Mở lớp mới và thao tác chuyển vòng đời ở cuối mỗi dòng.
export function ClassesPage() {
  const { user } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();
  const [tick, setTick] = useState(0);
  const [opening, setOpening] = useState(false);
  const [cancelling, setCancelling] = useState<Klass | null>(null);
  const manage = canManageClasses(user?.active ?? -1);
  // Có backend: tick tăng thì ClassListPage tải lại danh sách từ máy chủ.
  const refresh = () => (apiEnabled || saveAll(), setTick((t) => t + 1));
  const sync = (call: Promise<unknown>, title: string, sub?: string, after?: () => void) =>
    call.then(
      () => (after?.(), refresh(), toast(title, sub)),
      (e: Error) => toast('Chưa lưu được', e.message),
    );

  const actions = (k: Klass): MenuAction[] => {
    const step = nextStep(k);
    const list: MenuAction[] = [];
    if (step)
      list.push({
        label: step.label,
        disabled: step.blocked,
        onClick: () => {
          if (apiEnabled) return void sync(setClassStatus(k.code, step.to), `Lớp ${k.code} chuyển sang ${step.to}`);
          k.status = step.to;
          refresh();
          toast(`Lớp ${k.code} chuyển sang ${step.to}`);
        },
      });
    if (k.status === 'Chuẩn bị' || k.status === 'Đang học') {
      // S3-06
      list.unshift({ label: 'Ghi danh học viên', onClick: () => navigate(`/lop-hoc/${k.code}/ghi-danh`) });
      // S4-01
      list.splice(1, 0, { label: 'Sinh thời khoá biểu', onClick: () => navigate(`/lop-hoc/${k.code}/sinh-lich`) });
      // S3-08
      list.splice(1, 0, { label: 'Phân công giảng dạy', onClick: () => navigate(`/lop-hoc/${k.code}/phan-cong`) });
      list.push({ label: 'Huỷ lớp', danger: true, onClick: () => setCancelling(k) });
    }
    return list.length ? list : [{ label: 'Lớp đã ' + k.status.toLowerCase(), disabled: true }];
  };

  return (
    <>
      <ClassListPage
        head={
          <>
            {/* S4-04 */}
            <Link to="/thoi-khoa-bieu" className="btn">
              Thời khoá biểu
            </Link>
            {/* S3-09 */}
            <Link to="/phong-hoc" className="btn">
              Phòng học
            </Link>
            {manage && (
              <button type="button" className="btn primary" style={{ width: 160 }} onClick={() => setOpening(true)}>
                <Icon name="plus" size={16} />
                Mở lớp mới
              </button>
            )}
          </>
        }
        rowActions={manage ? actions : undefined}
        onOpen={(k) => navigate('/lop-hoc/' + k.code)}
        version={tick}
      />
      {opening && (
        <OpenClassDrawer
          onClose={() => setOpening(false)}
          onSave={(k, programCode) => {
            if (apiEnabled)
              return void sync(openClass(k, programCode), 'Đã mở lớp ' + k.code, `${k.program} · ${k.subjects.length} môn sao chép từ chương trình`, () => setOpening(false));
            CLASSES.unshift(k);
            setOpening(false);
            refresh();
            toast('Đã mở lớp ' + k.code, `${k.program} · ${k.subjects.length} môn sao chép từ chương trình`);
          }}
        />
      )}
      {cancelling && (
        <ConfirmDialog
          title={'Huỷ lớp ' + cancelling.code}
          permanent
          confirmLabel="Huỷ lớp"
          onClose={() => setCancelling(null)}
          onConfirm={() => {
            if (apiEnabled) return void sync(setClassStatus(cancelling.code, 'Huỷ'), 'Đã huỷ lớp ' + cancelling.code, undefined, () => setCancelling(null));
            cancelling.status = 'Huỷ';
            setCancelling(null);
            refresh();
            toast('Đã huỷ lớp ' + cancelling.code);
          }}
        >
          Lớp <b>{cancelling.code}</b> chuyển sang Huỷ và không mở lại được. Học viên trong lớp cần được chuyển sang lớp khác.
        </ConfirmDialog>
      )}
    </>
  );
}
