import { useEffect, useState } from 'react';
import { AppLayout } from '../../components/AppLayout';
import { Icon } from '../../components/Icon';
import { useToast } from '../../components/Toast';
import { Box, initial } from '../../components/ui';
import { api, ApiError, apiEnabled, json } from '../../data/api';
import { useAuth } from '../../data/auth';
import { ROLES } from '../../data/permissions';
import { saveAll } from '../../data/store';
import { phoneOwner, PROFILES, USERS } from '../../data/users';
import { digits, isPhone, PHONE_MSG } from '../../data/validate';
import { AvatarCropDialog } from './AvatarCropDialog';
import './profile.css';

const MAX_BYTES = 2 * 1024 * 1024;
type Info = { name: string; phone: string; birth: string; address: string };
type ApiProfile = { fullName: string; phone: string; dateOfBirth: string; address: string };

const fromApi = (p: ApiProfile): Info => ({ name: p.fullName, phone: p.phone, birth: p.dateOfBirth, address: p.address });
const me = (email: string) => USERS.find((u) => u.email.toLowerCase() === email.toLowerCase());

/** Hồ sơ trong dữ liệu mẫu: họ tên, số điện thoại ở USERS; ngày sinh, địa chỉ ở PROFILES. */
function localProfile(email: string, name: string): Info {
  const p = PROFILES[email] ?? { birth: '', address: '' };
  return { name: me(email)?.name ?? name, phone: me(email)?.phone ?? '', birth: p.birth, address: p.address };
}

// S2-02. Người dùng tự sửa họ tên, số điện thoại, ngày sinh, địa chỉ; email và vai trò bị khoá.
// S2-03. Đổi ảnh đại diện: JPG/PNG tối đa 2MB, cắt vuông, tạo bản 96px và 40px.
// Có backend: GET/PUT /api/profile. Chưa có: đọc và ghi dữ liệu mẫu, giữ qua tải trang.
// Ảnh đại diện vẫn chỉ lưu trong phiên (PUT /me/avatar chưa có ở máy chủ).
export function ProfilePage() {
  const { user, updateUser } = useAuth();
  const toast = useToast();
  const [saved, setSaved] = useState<Info>(() => localProfile(user?.email ?? '', user?.name ?? ''));
  const [info, setInfo] = useState(saved);
  const [loading, setLoading] = useState(apiEnabled);
  const [saving, setSaving] = useState(false);
  const [phoneError, setPhoneError] = useState('');
  const [errors, setErrors] = useState<{ name?: string; birth?: string; form?: string }>({});
  const [avatar, setAvatar] = useState('');
  const [fileError, setFileError] = useState('');
  const [cropping, setCropping] = useState<File | null>(null);

  useEffect(() => {
    if (!apiEnabled) return;
    api<ApiProfile>('/api/profile')
      .then((p) => {
        setSaved(fromApi(p));
        setInfo(fromApi(p));
      })
      .catch((e: ApiError) => setErrors({ form: 'Không tải được hồ sơ: ' + e.message }))
      .finally(() => setLoading(false));
  }, []);

  if (!user) return <AppLayout crumb={['Hồ sơ cá nhân']}>{null}</AppLayout>;

  const set = (k: keyof Info) => (e: { target: { value: string } }) => {
    setInfo((x) => ({ ...x, [k]: e.target.value }));
    if (k === 'phone') setPhoneError('');
    else setErrors((x) => ({ ...x, [k]: undefined, form: undefined }));
  };

  async function save() {
    const name = info.name.trim();
    const e: typeof errors = {};
    if (!name) e.name = 'Nhập họ và tên';
    if (info.birth && info.birth > new Date().toISOString().slice(0, 10)) e.birth = 'Ngày sinh không được ở tương lai';
    const phoneMsg = info.phone.trim() && !isPhone(info.phone) ? PHONE_MSG : '';
    setErrors(e);
    setPhoneError(phoneMsg);
    if (e.name || e.birth || phoneMsg) return;

    const next: Info = { ...info, name, phone: info.phone.trim(), address: info.address.trim() };
    if (apiEnabled) {
      setSaving(true);
      try {
        const p = await api<ApiProfile>('/api/profile', json('PUT', { fullName: next.name, phone: digits(next.phone), dateOfBirth: next.birth, address: next.address }));
        done(fromApi(p));
      } catch (err) {
        const ae = err as ApiError;
        if (ae.code === 'PHONE_DUPLICATE' || ae.code === 'INVALID_PHONE_FORMAT') setPhoneError(ae.code === 'PHONE_DUPLICATE' ? 'Số điện thoại đã được dùng cho tài khoản khác' : ae.message);
        else setErrors({ form: 'Chưa lưu được: ' + ae.message });
      } finally {
        setSaving(false);
      }
      return;
    }
    const mine = me(user!.email);
    if (phoneOwner(next.phone, mine?.id ?? -1)) return setPhoneError('Số điện thoại đã được dùng cho tài khoản khác');
    if (mine) Object.assign(mine, { name: next.name, phone: next.phone });
    PROFILES[user!.email] = { birth: next.birth, address: next.address };
    saveAll();
    done(next);
  }

  function done(next: Info) {
    setSaved(next);
    setInfo(next);
    updateUser({ name: next.name });
    toast('Đã lưu hồ sơ', 'Thông tin cá nhân đã được cập nhật.');
  }

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
          aria-busy={loading}
          onSubmit={(e) => {
            e.preventDefault();
            save();
          }}
        >
          {errors.form && <Box icon="alert" tone="bad" title={errors.form} />}
          <h3>Thông tin cá nhân</h3>
          <div className="grid2">
            <div className="field">
              <label htmlFor="pf-name">Họ và tên</label>
              <div className={'inp' + (errors.name ? ' bad' : '')}>
                <input id="pf-name" value={info.name} onChange={set('name')} disabled={loading} />
              </div>
              {errors.name && <p className="err">{errors.name}</p>}
            </div>
            <div className="field">
              <label htmlFor="pf-phone">Số điện thoại</label>
              <div className={'inp lead' + (phoneError ? ' bad' : '')}>
                <Icon name="phone" />
                <input id="pf-phone" inputMode="tel" value={info.phone} onChange={set('phone')} disabled={loading} />
              </div>
              {phoneError && <p className="err">{phoneError}</p>}
            </div>
            <div className="field">
              <label htmlFor="pf-birth">Ngày sinh</label>
              <div className={'inp' + (errors.birth ? ' bad' : '')}>
                <input id="pf-birth" type="date" value={info.birth} max={new Date().toISOString().slice(0, 10)} onChange={set('birth')} disabled={loading} />
              </div>
              {errors.birth && <p className="err">{errors.birth}</p>}
            </div>
            <div className="field">
              <label htmlFor="pf-addr">Địa chỉ</label>
              <div className="inp">
                <input id="pf-addr" value={info.address} onChange={set('address')} disabled={loading} />
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
                setErrors({});
              }}
            >
              Huỷ
            </button>
            <button type="submit" className="btn primary" style={{ width: 150 }} disabled={loading || saving}>
              {saving ? 'Đang lưu…' : 'Lưu thay đổi'}
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
