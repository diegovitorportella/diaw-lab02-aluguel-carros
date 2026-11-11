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
  const n = etapas.length;
  const corTrecho = desvio ? (status === "REJEITADO" ? "#dc2626" : "#94a3b8") : "#f59a0c";

  // Data em que cada etapa foi atingida (primeiro evento daquele status)
  const dataDa = (s: string) => historico.find((e) => e.status === s)?.dataHora;

  // A pista vai do centro da primeira parada ao centro da última.
  const margem = `${100 / (2 * n)}%`;

  return (
    <div>
      <div className="relative">
        <div className="absolute top-3 h-[3px] rounded-full bg-slate-200" style={{ left: margem, right: margem }} />
        {atual > 0 && (
          <div className="absolute top-3" style={{ left: margem, right: margem }}>
            <div style={{ width: `${(atual / (n - 1)) * 100}%` }}>
              {/* linhas de velocidade: o trecho percorrido ecoa o traço da logo */}
              <div className="pista-trecho relative h-[3px] rounded-full" style={{ background: corTrecho }}>
                {!desvio && (
                  <>
                    <span className="absolute -top-[5px] right-3 h-[2px] w-[28%] rounded-full opacity-70" style={{ background: corTrecho }} />
                    <span className="absolute -bottom-[5px] right-6 h-[2px] w-[18%] rounded-full opacity-50" style={{ background: corTrecho }} />
                  </>
                )}
              </div>
            </div>
          </div>
        )}
        <ol className="relative flex items-start">
          {etapas.map((etapa, i) => {
            const feita = i < atual || (i === atual && (concluido || desvio));
            const ativa = i === atual && !concluido && !desvio;
            const ehDesvio = desvio && i === n - 1;
            const marcador = ehDesvio
              ? `${status === "REJEITADO" ? "border-red-600 bg-red-600" : "border-slate-500 bg-slate-500"} text-white`
              : feita
              ? "border-slate-950 bg-slate-950 text-white"
              : ativa
              ? "border-blue-600 bg-white"
              : "border-slate-300 bg-white";
            const data = dataDa(etapa.status);

            return (
              <li key={etapa.status} className="flex flex-1 flex-col items-center text-center">
                <span className={`grid h-[27px] w-[27px] place-items-center rounded-full border-[3px] ${marcador}`}>
                  {feita || ehDesvio ? (
                    <Icon name={ehDesvio ? "close" : "check"} className="h-3 w-3" />
                  ) : ativa ? (
                    <span className="h-2 w-2 rounded-full bg-blue-600" />
                  ) : null}
                </span>
                <span className={`mt-2 text-xs font-bold ${ativa ? "text-blue-700" : feita || ehDesvio ? "text-slate-900" : "text-slate-400"}`}>
                  {etapa.rotulo}
                </span>
                {data && <span className="text-[10px] text-slate-500">{formatarDataHora(data)}</span>}
              </li>
            );
          })}
        </ol>
      </div>

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
