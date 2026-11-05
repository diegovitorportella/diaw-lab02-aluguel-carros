import type { ReactNode } from "react";

export default function Header({
  eyebrow,
  title,
  description,
  action,
}: {
  eyebrow: string;
  title: string;
  description: string;
  action?: ReactNode;
}) {
  return (
    <div className="mb-8 flex flex-col gap-5 md:flex-row md:items-end md:justify-between">
      <div>
        <p className="text-xs font-bold uppercase tracking-[.16em] text-blue-600">
          {eyebrow}
        </p>
        <h1 className="mt-2 text-3xl font-semibold tracking-[-.03em] text-slate-950 md:text-4xl">
          {title}
        </h1>
        <p className="mt-2 text-slate-500">{description}</p>
      </div>
      {action}
    </div>
  );
}