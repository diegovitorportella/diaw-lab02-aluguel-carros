import type { ReactNode } from "react";
import Icon from "./Icon";

export default function Field({
  label,
  icon,
  action,
  children,
}: {
  label: string;
  icon?: string;
  action?: ReactNode;
  children: ReactNode;
}) {
  return (
    <label className="block">
      <span className="mb-2 block text-sm font-semibold text-slate-700">
        {label}
      </span>
      <span className="input-shell">
        {icon && (
          <Icon name={icon} className="h-[18px] w-[18px] text-slate-400" />
        )}
        <span className="min-w-0 flex-1">{children}</span>
        {action}
      </span>
    </label>
  );
}