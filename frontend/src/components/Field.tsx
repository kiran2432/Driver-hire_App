import { useId, type ReactNode } from 'react';

interface BaseProps {
  label: string;
  error?: string | undefined;
  hint?: string;
}

function Wrapper({ id, label, error, hint, children }: BaseProps & { id: string; children: ReactNode }) {
  return (
    <div className={`field${error ? ' has-error' : ''}`}>
      <label htmlFor={id}>{label}</label>
      {children}
      {hint && !error && <small className="hint">{hint}</small>}
      {error && (
        <small className="error-text" id={`${id}-error`} role="alert">
          {error}
        </small>
      )}
    </div>
  );
}

interface TextFieldProps extends BaseProps {
  value: string;
  onChange: (value: string) => void;
  type?: 'text' | 'email' | 'password' | 'tel' | 'number' | 'date' | 'datetime-local';
  required?: boolean;
  autoComplete?: string;
  min?: string | number;
  max?: string | number;
  step?: string | number;
  maxLength?: number;
}

export function TextField({ label, error, hint, value, onChange, type = 'text', ...rest }: TextFieldProps) {
  const id = useId();
  return (
    <Wrapper id={id} label={label} error={error} {...(hint ? { hint } : {})}>
      <input
        id={id}
        type={type}
        value={value}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? `${id}-error` : undefined}
        onChange={(e) => onChange(e.target.value)}
        {...rest}
      />
    </Wrapper>
  );
}

interface SelectFieldProps<V extends string> extends BaseProps {
  value: V | '';
  onChange: (value: V) => void;
  options: { value: V; label: string }[];
  placeholder?: string;
  required?: boolean;
}

export function SelectField<V extends string>({
  label,
  error,
  hint,
  value,
  onChange,
  options,
  placeholder,
  required,
}: SelectFieldProps<V>) {
  const id = useId();
  return (
    <Wrapper id={id} label={label} error={error} {...(hint ? { hint } : {})}>
      <select
        id={id}
        value={value}
        required={required}
        aria-invalid={error ? true : undefined}
        onChange={(e) => onChange(e.target.value as V)}
      >
        {placeholder !== undefined && <option value="">{placeholder}</option>}
        {options.map((o) => (
          <option key={o.value} value={o.value}>
            {o.label}
          </option>
        ))}
      </select>
    </Wrapper>
  );
}

interface TextAreaFieldProps extends BaseProps {
  value: string;
  onChange: (value: string) => void;
  maxLength?: number;
  rows?: number;
}

export function TextAreaField({ label, error, hint, value, onChange, maxLength, rows = 3 }: TextAreaFieldProps) {
  const id = useId();
  return (
    <Wrapper id={id} label={label} error={error} {...(hint ? { hint } : {})}>
      <textarea
        id={id}
        value={value}
        rows={rows}
        maxLength={maxLength}
        aria-invalid={error ? true : undefined}
        onChange={(e) => onChange(e.target.value)}
      />
    </Wrapper>
  );
}

interface CheckboxFieldProps {
  label: string;
  checked: boolean;
  onChange: (checked: boolean) => void;
}

export function CheckboxField({ label, checked, onChange }: CheckboxFieldProps) {
  const id = useId();
  return (
    <div className="field checkbox">
      <input id={id} type="checkbox" checked={checked} onChange={(e) => onChange(e.target.checked)} />
      <label htmlFor={id}>{label}</label>
    </div>
  );
}
