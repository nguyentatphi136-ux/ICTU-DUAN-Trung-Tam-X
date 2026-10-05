import { useState } from 'react';
import { AppLayout } from '../../components/AppLayout';
import { Icon } from '../../components/Icon';
import { useToast } from '../../components/Toast';
import { initial } from '../../components/ui';
import { useAuth } from '../../data/auth';
import { ROLES } from '../../data/permissions';
import { PHONE_RE } from '../S1-08-tai-khoan/UserDrawer';
import { AvatarCropDialog } from './AvatarCropDialog';
import './profile.css';

const MAX_BYTES = 2 * 1024 * 1024;
type Info = { name: string; phone: string; birth: string; address: string };

// S2-02. Người dùng tự sửa họ tên, số điện thoại, ngày sinh, địa chỉ; email và vai trò bị khoá.
// S2-03. Đổi ảnh đại diện: JPG/PNG tối đa 2MB, cắt vuông, tạo bản 96px và 40px.
// Khi tích hợp: GET/PUT /me/profile, PUT /me/avatar (multipart, máy chủ cắt và tạo bản thu nhỏ).
export function ProfilePage() {
  const { user } = useAuth();
  const toast = useToast();
  const start: Info = { name: user?.name ?? '', phone: '0912 345 678', birth: '2004-03-14', address: '25 Trần Hưng Đạo, Thái Bình' };
  const [saved, setSaved] = useState(start);
  const [info, setInfo] = useState(start);
  const [phoneError, setPhoneError] = useState('');
  const [avatar, setAvatar] = useState('');
  const [fileError, setFileError] = useState('');
  const [cropping, setCropping] = useState<File | null>(null);

  if (!user) return <AppLayout crumb={['Hồ sơ cá nhân']}>{null}</AppLayout>;

  const set = (k: keyof Info) => (e: { target: { value: string } }) => {
    setInfo((x) => ({ ...x, [k]: e.target.value }));
    if (k === 'phone') setPhoneError('');
  };

  function pickFile(f?: File) {
    if (!f) return;
    if (!/^image\/(jpeg|png)$/.test(f.type)) return setFileError('Chỉ nhận ảnh JPG hoặc PNG.');
    if (f.size > MAX_BYTES) return setFileError('Ảnh nặng hơn 2MB. Hãy chọn ảnh nhỏ hơn.');
    setFileError('');
    setCropping(f);
  }

  return (
    <AppLayout crumb={['Hồ sơ cá nhân']}>
      <div className="h">
        <div>
          <h2>Hồ sơ cá nhân</h2>
          <p>Thông tin liên lạc để trung tâm gọi được khi cần.</p>
        </div>
      </div>
      <div className="pf">
        <section className="panel pf-ava">
          {avatar ? <img src={avatar} alt="Ảnh đại diện" className="pf-img" /> : <span className="pf-img">{initial(user.name)}</span>}
          <b>{user.name}</b>
          <span className="hint">{user.email}</span>
          <span className="pill info">{ROLES[user.active]}</span>
          <label className="btn" style={{ width: 200, marginTop: 8 }}>
            <Icon name="cam" size={16} />
            Đổi ảnh đại diện
            <input
              type="file"
              accept="image/jpeg,image/png"
              hidden
              onChange={(e) => {
                pickFile(e.target.files?.[0]);
                e.target.value = '';
              }}
            />
          </label>
          {fileError ? <p className="err">{fileError}</p> : <p className="hint">JPG hoặc PNG, tối đa 2MB. Ảnh được cắt vuông.</p>}
        </section>

        <form
          className="panel pf-form"
          noValidate
          onSubmit={(e) => {
            e.preventDefault();
            if (!PHONE_RE.test(info.phone.replace(/\s/g, ''))) return setPhoneError('Số điện thoại Việt Nam gồm 10 chữ số, bắt đầu bằng 0');
            setSaved(info);
            toast('Đã lưu hồ sơ', 'Thông tin cá nhân đã được cập nhật.');
          }}
        >
          <h3>Thông tin cá nhân</h3>
          <div className="grid2">
            <div className="field">
              <label htmlFor="pf-name">Họ và tên</label>
              <div className="inp">
                <input id="pf-name" value={info.name} onChange={set('name')} />
              </div>
            </div>
            <div className="field">
              <label htmlFor="pf-phone">Số điện thoại</label>
              <div className={'inp lead' + (phoneError ? ' bad' : '')}>
                <Icon name="phone" />
                <input id="pf-phone" inputMode="tel" value={info.phone} onChange={set('phone')} />
              </div>
              {phoneError && <p className="err">{phoneError}</p>}
            </div>
            <div className="field">
              <label htmlFor="pf-birth">Ngày sinh</label>
              <div className="inp">
                <input id="pf-birth" type="date" value={info.birth} onChange={set('birth')} />
              </div>
            </div>
            <div className="field">
              <label htmlFor="pf-addr">Địa chỉ</label>
              <div className="inp">
                <input id="pf-addr" value={info.address} onChange={set('address')} />
              </div>
            </div>
          </div>
          <h3>Thông tin tài khoản</h3>
          <div className="grid2">
            <div className="field">
              <label htmlFor="pf-email">Email</label>
              <div className="inp lead">
                <Icon name="mail" />
                <input id="pf-email" value={user.email} disabled />
              </div>
            </div>
            <div className="field">
              <label htmlFor="pf-role">Vai trò</label>
              <div className="inp">
                <input id="pf-role" value={user.roles.map((r) => ROLES[r]).join(', ')} disabled />
              </div>
            </div>
          </div>
          <p className="hint pf-lock">
            <Icon name="lock" size={16} />
            Bạn không tự đổi được email và vai trò. Hãy liên hệ quản trị hệ thống nếu cần.
          </p>
          <div className="actions">
            <button
              type="button"
              className="btn"
              style={{ width: 100 }}
              onClick={() => {
                setInfo(saved);
                setPhoneError('');
              }}
            >
              Huỷ
            </button>
            <button type="submit" className="btn primary" style={{ width: 150 }}>
              Lưu thay đổi
            </button>
          </div>
        </form>
      </div>

      {cropping && (
        <AvatarCropDialog
          file={cropping}
          onClose={() => setCropping(null)}
          onSave={(url) => {
            setAvatar(url);
            setCropping(null);
            toast('Đã đổi ảnh đại diện');
          }}
        />
      )}
    </AppLayout>
  );
}
