import { Icon } from '../../components/Icon';
import { ROLES } from '../../data/permissions';

type Props = {
  /** Danh sách vị trí vai trò (theo ROLES) mà người dùng đang giữ. */
  value: number[];
  onChange: (roles: number[]) => void;
  /** Vai trò không được thu hồi, ví dụ vai trò quản trị của chính người đang thao tác. */
  protectedRole?: number;
  onBlocked?: () => void;
};

// S1-09. Gán và thu hồi vai trò: mỗi vai trò là một thẻ có dấu ×, thêm bằng danh sách chọn.
// Một người giữ được nhiều vai trò cùng lúc.
export function RoleChips({ value, onChange, protectedRole, onBlocked }: Props) {
  const remaining = ROLES.map((_, i) => i).filter((i) => !value.includes(i));

  return (
    <div className="rc-chips">
      {value.map((r) => (
        <span className="rc-chip" key={r}>
          {ROLES[r]}
          <button
            type="button"
            aria-label={`Thu hồi vai trò ${ROLES[r]}`}
            onClick={() => {
              if (r === protectedRole) {
                onBlocked?.();
                return;
              }
              onChange(value.filter((x) => x !== r));
            }}
          >
            <Icon name="x" />
          </button>
        </span>
      ))}
      {remaining.length > 0 && (
        <select
          aria-label="Thêm vai trò"
          value=""
          onChange={(e) => {
            if (e.target.value !== '') onChange([...value, Number(e.target.value)].sort((a, b) => a - b));
          }}
        >
          <option value="">+ Thêm vai trò</option>
          {remaining.map((i) => (
            <option key={i} value={i}>
              {ROLES[i]}
            </option>
          ))}
        </select>
      )}
    </div>
  );
}
