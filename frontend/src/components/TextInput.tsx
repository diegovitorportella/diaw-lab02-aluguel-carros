export default function TextInput({
  label,
  placeholder,
  type = "text",
  wide = false,
  value,
  onChange,
  required = false
}: {
  label: string;
  placeholder?: string;
  type?: string;
  wide?: boolean;
  value?: string;
  onChange?: (e: React.ChangeEvent<HTMLInputElement>) => void;
  required?: boolean;
}) {
  return (
    <label className={wide ? "md:col-span-2" : ""}>
      <span className="mb-2 block text-sm font-semibold text-slate-700">
        {label}
      </span>
      <input 
        className="form-input" 
        type={type} 
        placeholder={placeholder} 
        value={value}
        onChange={onChange}
        required={required}
      />
    </label>
  );
}