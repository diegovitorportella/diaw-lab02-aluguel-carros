export type Status = "PENDENTE" | "APROVADO" | "CONCLUIDO" | "REJEITADO" | "CANCELADO";

const ESTILOS: Record<string, string> = {
  PENDENTE: "bg-amber-50 text-amber-700 ring-amber-600/15",
  APROVADO: "bg-blue-50 text-blue-700 ring-blue-600/15",
  CONCLUIDO: "bg-emerald-50 text-emerald-700 ring-emerald-600/15",
  REJEITADO: "bg-red-50 text-red-700 ring-red-600/15",
  CANCELADO: "bg-slate-100 text-slate-600 ring-slate-500/15",
};

export const ROTULOS: Record<string, string> = {
  PENDENTE: "Pendente",
  APROVADO: "Aprovado",
  CONCLUIDO: "Concluído",
  REJEITADO: "Rejeitado",
  CANCELADO: "Cancelado",
};

export default function StatusBadge({ status }: { status: Status | string }) {
  const classes = ESTILOS[status] ?? ESTILOS.PENDENTE;

  return (
    <span
      className={`inline-flex items-center gap-1.5 rounded-full px-3 py-1 text-xs font-semibold ring-1 ring-inset ${classes}`}
    >
      <span className="h-1.5 w-1.5 rounded-full bg-current" />
      {ROTULOS[status] ?? status}
    </span>
  );
}
