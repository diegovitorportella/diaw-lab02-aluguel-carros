export default function TextInput({
  label,
  placeholder,
  type = "text",
  wide = false,
  value,
  onChange,
  required = false,
  error,
  maxLength,
  inputMode,
  min,
  step,
  hint,
}: {
  label: string;
  placeholder?: string;
  type?: string;
  wide?: boolean;
  value?: string;
  onChange?: (e: React.ChangeEvent<HTMLInputElement>) => void;
  required?: boolean;
  error?: string;
  maxLength?: number;
  inputMode?: "text" | "numeric" | "decimal";
  min?: string | number;
  step?: string | number;
  hint?: string;
}) {
  return (
    <label className={wide ? "md:col-span-2" : ""}>
      <span className="mb-2 block text-sm font-semibold text-slate-700">
        {label}
      </span>
      <input
        className={`form-input ${error ? "border-red-400 ring-1 ring-red-200" : ""}`}
        type={type}
        placeholder={placeholder}
        value={value}
        onChange={onChange}
        required={required}
        maxLength={maxLength}
        inputMode={inputMode}
        min={min}
        step={step}
        aria-invalid={error ? true : undefined}
      />
      {error ? (
        <span className="mt-1.5 block text-xs font-medium text-red-600">{error}</span>
      ) : hint ? (
        <span className="mt-1.5 block text-xs text-slate-400">{hint}</span>
      ) : null}
    </label>
  );
}
