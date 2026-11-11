import type { ReactNode } from "react";

export default function Header({
  title,
  description,
  action,
}: {
  title: string;
  description: string;
  action?: ReactNode;
}) {
  return (
    <div className="mb-8 flex flex-col gap-5 md:flex-row md:items-end md:justify-between">
      <div>
        <h1 className="text-3xl font-bold tracking-[-.01em] text-slate-950 md:text-[2.5rem] md:leading-[1.1]">
          {title}
        </h1>
        <p className="mt-2 max-w-2xl text-slate-600">{description}</p>
      </div>
      {action}
    </div>
  );
}
