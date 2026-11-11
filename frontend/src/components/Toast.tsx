import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from "react";
import type { ReactNode } from "react";
import Icon from "./Icon";

type Tipo = "sucesso" | "erro" | "info";

interface Aviso {
  id: number;
  tipo: Tipo;
  titulo: string;
  detalhe?: string;
}

interface ToastApi {
  sucesso: (titulo: string, detalhe?: string) => void;
  erro: (titulo: string, detalhe?: string) => void;
  info: (titulo: string, detalhe?: string) => void;
}

const DURACAO_MS: Record<Tipo, number> = { sucesso: 4500, info: 4500, erro: 7000 };

const ToastContext = createContext<ToastApi | null>(null);

/** Avisos de confirmação/erro exibidos no canto da tela, em qualquer página. */
export function ToastProvider({ children }: { children: ReactNode }) {
  const [avisos, setAvisos] = useState<Aviso[]>([]);
  const proximoId = useRef(1);

  const remover = useCallback((id: number) => {
    setAvisos((atuais) => atuais.filter((a) => a.id !== id));
  }, []);

  const adicionar = useCallback((tipo: Tipo, titulo: string, detalhe?: string) => {
    const id = proximoId.current++;
    setAvisos((atuais) => [...atuais.slice(-3), { id, tipo, titulo, detalhe }]);
  }, []);

  const api = useMemo<ToastApi>(
    () => ({
      sucesso: (t, d) => adicionar("sucesso", t, d),
      erro: (t, d) => adicionar("erro", t, d),
      info: (t, d) => adicionar("info", t, d),
    }),
    [adicionar],
  );

  return (
    <ToastContext.Provider value={api}>
      {children}
      <div
        className="pointer-events-none fixed inset-x-0 top-4 z-[100] flex flex-col items-center gap-3 px-4 sm:inset-x-auto sm:right-6 sm:items-end"
        aria-live="polite"
      >
        {avisos.map((aviso) => (
          <ItemAviso key={aviso.id} aviso={aviso} onFechar={() => remover(aviso.id)} />
        ))}
      </div>
    </ToastContext.Provider>
  );
}

const ESTILOS: Record<Tipo, { caixa: string; icone: string }> = {
  sucesso: { caixa: "border-emerald-200", icone: "bg-emerald-500 text-white" },
  erro: { caixa: "border-red-200", icone: "bg-red-500 text-white" },
  info: { caixa: "border-blue-200", icone: "bg-blue-600 text-white" },
};

function ItemAviso({ aviso, onFechar }: { aviso: Aviso; onFechar: () => void }) {
  useEffect(() => {
    const timer = setTimeout(onFechar, DURACAO_MS[aviso.tipo]);
    return () => clearTimeout(timer);
  }, [aviso.tipo, onFechar]);

  const estilo = ESTILOS[aviso.tipo];
  return (
    <div
      role={aviso.tipo === "erro" ? "alert" : "status"}
      className={`pointer-events-auto flex w-full max-w-sm items-start gap-3 rounded-2xl border bg-white p-4 shadow-xl ${estilo.caixa}`}
    >
      <span className={`mt-0.5 grid h-6 w-6 shrink-0 place-items-center rounded-full ${estilo.icone}`}>
        <Icon name={aviso.tipo === "sucesso" ? "check" : "close"} className="h-3.5 w-3.5" />
      </span>
      <div className="min-w-0 flex-1">
        <p className="text-sm font-semibold text-slate-900">{aviso.titulo}</p>
        {aviso.detalhe && <p className="mt-0.5 text-xs text-slate-500">{aviso.detalhe}</p>}
      </div>
      <button onClick={onFechar} className="text-slate-400 hover:text-slate-700" aria-label="Fechar aviso">
        <Icon name="close" className="h-4 w-4" />
      </button>
    </div>
  );
}

export function useToast(): ToastApi {
  const contexto = useContext(ToastContext);
  if (!contexto) throw new Error("useToast deve ser usado dentro de <ToastProvider>");
  return contexto;
}
