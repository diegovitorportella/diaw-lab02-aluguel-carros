import Icon from "./Icon";
import { formatarDataHora } from "../utils/formatters";

interface Evento {
  status: string;
  descricao?: string;
  dataHora?: string;
  responsavelNome?: string;
  responsavelTipo?: string;
}

interface Etapa {
  status: string;
  rotulo: string;
}

// Fluxo normal: Solicitado → Aprovado → Concluído. Os desvios terminam o fluxo na etapa atual.
const FLUXO: Etapa[] = [
  { status: "PENDENTE", rotulo: "Solicitado" },
  { status: "APROVADO", rotulo: "Aprovado" },
  { status: "CONCLUIDO", rotulo: "Concluído" },
];

function etapasDo(status: string): { etapas: Etapa[]; atual: number; desvio: boolean } {
  if (status === "REJEITADO") {
    return { etapas: [FLUXO[0], { status, rotulo: "Rejeitado" }], atual: 1, desvio: true };
  }
  if (status === "CANCELADO") {
    return { etapas: [FLUXO[0], { status, rotulo: "Cancelado" }], atual: 1, desvio: true };
  }
  const atual = Math.max(0, FLUXO.findIndex((e) => e.status === status));
  return { etapas: FLUXO, atual, desvio: false };
}

/** Acompanhamento do pedido: barra de etapas e (opcionalmente) o histórico completo. */
export default function OrderTimeline({
  status,
  historico = [],
  mostrarHistorico = true,
}: {
  status: string;
  historico?: Evento[];
  mostrarHistorico?: boolean;
}) {
  const { etapas, atual, desvio } = etapasDo(status);
  const concluido = status === "CONCLUIDO";
  const corDesvio = status === "REJEITADO" ? "bg-red-500 border-red-500" : "bg-slate-400 border-slate-400";

  // Data em que cada etapa foi atingida (primeiro evento daquele status)
  const dataDa = (s: string) => historico.find((e) => e.status === s)?.dataHora;

  return (
    <div>
      <ol className="flex items-start">
        {etapas.map((etapa, i) => {
          const feita = i < atual || (i === atual && (concluido || desvio));
          const ativa = i === atual && !concluido && !desvio;
          const ehDesvio = desvio && i === etapas.length - 1;
          const marcador = ehDesvio
            ? `${corDesvio} text-white`
            : feita
            ? "bg-emerald-500 border-emerald-500 text-white"
            : ativa
            ? "bg-white border-blue-600 text-blue-600"
            : "bg-white border-slate-300 text-slate-300";
          const data = dataDa(etapa.status);

          return (
            <li key={etapa.status} className="relative flex flex-1 flex-col items-center text-center">
              {i > 0 && (
                <span
                  className={`absolute right-1/2 top-3.5 -z-0 h-0.5 w-full ${
                    i <= atual ? (ehDesvio ? (status === "REJEITADO" ? "bg-red-300" : "bg-slate-300") : "bg-emerald-400") : "bg-slate-200"
                  }`}
                />
              )}
              <span className={`relative z-10 grid h-7 w-7 place-items-center rounded-full border-2 ${marcador}`}>
                {feita || ehDesvio ? (
                  <Icon name={ehDesvio ? "close" : "check"} className="h-3.5 w-3.5" />
                ) : (
                  <span className={`h-2 w-2 rounded-full ${ativa ? "bg-blue-600" : "bg-slate-300"}`} />
                )}
              </span>
              <span className={`mt-2 text-xs font-semibold ${ativa ? "text-blue-700" : feita || ehDesvio ? "text-slate-800" : "text-slate-400"}`}>
                {etapa.rotulo}
              </span>
              {data && <span className="text-[10px] text-slate-400">{formatarDataHora(data)}</span>}
            </li>
          );
        })}
      </ol>

      {mostrarHistorico && historico.length > 0 && (
        <details className="mt-4 text-sm">
          <summary className="cursor-pointer select-none text-xs font-semibold text-blue-600 hover:text-blue-700">
            Ver histórico ({historico.length})
          </summary>
          <ul className="mt-3 space-y-2 border-l-2 border-slate-200 pl-4">
            {historico.map((e, i) => (
              <li key={i} className="relative">
                <span className="absolute -left-[1.4rem] top-1.5 h-2 w-2 rounded-full bg-slate-300" />
                <p className="text-xs font-semibold text-slate-700">{e.descricao}</p>
                <p className="text-[11px] text-slate-400">
                  {formatarDataHora(e.dataHora)}
                  {e.responsavelNome ? ` · ${e.responsavelNome}` : ""}
                </p>
              </li>
            ))}
          </ul>
        </details>
      )}
    </div>
  );
}
