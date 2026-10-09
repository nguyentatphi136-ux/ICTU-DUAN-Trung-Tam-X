import { Icon } from './Icon';

// Bốn điều kiện mật khẩu dùng chung cho S1-03 và S1-04.
export const PASSWORD_RULES: { label: string; test: (v: string) => boolean }[] = [
  { label: 'Tối thiểu 8 ký tự', test: (v) => v.length >= 8 },
  { label: 'Bao gồm chữ cái in hoa và chữ cái thường', test: (v) => /[a-z]/.test(v) && /[A-Z]/.test(v) },
  { label: 'Có các chữ số', test: (v) => /\d/.test(v) },
  { label: 'Ít nhất một ký tự đặc biệt (!, @, #, $, ^, *)', test: (v) => /[!@#$^*]/.test(v) },
];

export const isStrongPassword = (v: string) => PASSWORD_RULES.every((r) => r.test(v));

export function PasswordRules({ value }: { value: string }) {
  return (
    <div className="rules">
      <b>Mật khẩu cần có:</b>
      <ul>
        {PASSWORD_RULES.map((r) => (
          <li key={r.label} className={r.test(value) ? 'ok' : undefined}>
            <Icon name="check" />
            {r.label}
          </li>
        ))}
      </ul>
    </div>
  );
}
