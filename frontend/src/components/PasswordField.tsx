import { useState } from 'react';
import { Icon } from './Icon';

type Props = {
  id: string;
  label: string;
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  error?: string;
  autoComplete?: string;
};

// Ô mật khẩu có ổ khoá bên trái và nút con mắt để hiện hoặc ẩn.
export function PasswordField({ id, label, value, onChange, placeholder, error, autoComplete = 'new-password' }: Props) {
  const [shown, setShown] = useState(false);
  return (
    <div className="field">
      <label htmlFor={id}>{label}</label>
      <div className={'inp lead' + (error ? ' bad' : '')}>
        <Icon name="lock" />
        <input
          id={id}
          type={shown ? 'text' : 'password'}
          value={value}
          placeholder={placeholder}
          autoComplete={autoComplete}
          onChange={(e) => onChange(e.target.value)}
        />
        <button
          type="button"
          className="eye"
          aria-label={shown ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
          onClick={() => setShown((s) => !s)}
        >
          <Icon name={shown ? 'eyeoff' : 'eye'} />
        </button>
      </div>
      {error && <p className="err">{error}</p>}
    </div>
  );
}
