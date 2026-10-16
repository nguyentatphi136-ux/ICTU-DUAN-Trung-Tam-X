import { Icon } from './Icon';

// Điều kiện mật khẩu dùng chung cho S1-03 và S1-04, đúng tiêu chí S1-04: tối thiểu 8 ký tự, có chữ và số.
// Phải khớp backend: UserDAO.changePassword và ResetPasswordServlet.
export const PASSWORD_RULES: { label: string; test: (v: string) => boolean }[] = [
  { label: 'Tối thiểu 8 ký tự', test: (v) => v.length >= 8 },
  { label: 'Có chữ cái', test: (v) => /[A-Za-z]/.test(v) },
  { label: 'Có chữ số', test: (v) => /\d/.test(v) },
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
