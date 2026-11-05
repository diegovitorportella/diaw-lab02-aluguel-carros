export type Status = "Pending" | "Approved" | "Rejected" | "PENDENTE" | "APROVADO" | "REJEITADO";

export default function StatusBadge({ status }: { status: Status | string }) {
  const isApproved = status === "Approved" || status === "APROVADO";
  const isRejected = status === "Rejected" || status === "REJEITADO";

  const classes = isApproved
    ? "bg-emerald-50 text-emerald-700 ring-emerald-600/15"
    : isRejected
    ? "bg-red-50 text-red-700 ring-red-600/15"
    : "bg-amber-50 text-amber-700 ring-amber-600/15";

  return (
    <span
      className={`inline-flex items-center gap-1.5 rounded-full px-3 py-1 text-xs font-semibold ring-1 ring-inset ${classes}`}
    >
      <span className="h-1.5 w-1.5 rounded-full bg-current" />
      {status}
    </span>
  );
}