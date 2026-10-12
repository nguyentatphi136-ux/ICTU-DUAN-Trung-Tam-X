import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Icon } from '../../components/Icon';
import { useToast } from '../../components/Toast';
import { ConfirmDialog, type MenuAction } from '../../components/ui';
import { useAuth } from '../../data/auth';
import { CLASSES, type Klass } from '../../data/classes';
import { saveAll } from '../../data/store';
import { canManageClasses, ClassListPage } from '../S3-07-danh-sach-lop/ClassListPage';
import { nextStep, OpenClassDrawer } from './OpenClassDrawer';

// S3-05. Trang Lớp học của Quản lý đào tạo: nút Mở lớp mới và thao tác chuyển vòng đời ở cuối mỗi dòng.
export function ClassesPage() {
  const { user } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();
  const [, setTick] = useState(0);
  const [opening, setOpening] = useState(false);
  const [cancelling, setCancelling] = useState<Klass | null>(null);
  const manage = canManageClasses(user?.active ?? -1);
  const refresh = () => (saveAll(), setTick((t) => t + 1));

  const actions = (k: Klass): MenuAction[] => {
    const step = nextStep(k);
    const list: MenuAction[] = [];
    if (step)
      list.push({
        label: step.label,
        disabled: step.blocked,
        onClick: () => {
          k.status = step.to;
          refresh();
          toast(`Lớp ${k.code} chuyển sang ${step.to}`);
        },
      });
    if (k.status === 'Chuẩn bị' || k.status === 'Đang học') {
      // S3-06
      list.unshift({ label: 'Ghi danh học viên', onClick: () => navigate(`/lop-hoc/${k.code}/ghi-danh`) });
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
      />
      {opening && (
        <OpenClassDrawer
          onClose={() => setOpening(false)}
          onSave={(k) => {
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
