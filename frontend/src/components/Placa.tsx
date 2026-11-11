import { formatarPlaca } from "../utils/formatters";

const TAMANHOS = { sm: 15, md: 21, lg: 34, xl: 72 } as const;

/**
 * Placa de veículo desenhada como a real: Mercosul (faixa azul "BRASIL" e fundo branco)
 * ou modelo antigo (cinza com hífen). Todas as medidas são em em, então o tamanho é um só.
 */
export default function Placa({
  placa,
  tamanho = "md",
  className = "",
}: {
  placa?: string | null;
  tamanho?: keyof typeof TAMANHOS;
  className?: string;
}) {
  const texto = formatarPlaca(placa);
  if (!texto) return <span className="text-slate-400">—</span>;

  const antiga = texto.includes("-");

  return (
    <span
      role="img"
      aria-label={`Placa ${texto}`}
      style={{ fontSize: TAMANHOS[tamanho] }}
      className={`inline-flex shrink-0 select-none flex-col overflow-hidden rounded-[.2em] border-[.07em] border-slate-950 ${
        antiga ? "bg-[#d6d9de]" : "bg-white"
      } ${className}`}
    >
      <span
        className={`px-[.5em] py-[.2em] text-center font-sans text-[.3em] font-bold leading-none tracking-[.35em] ${
          antiga ? "bg-[#c3c7cd] text-slate-600" : "bg-[#0b3fb8] text-white"
        }`}
      >
        BRASIL
      </span>
      <span className="px-[.45em] py-[.08em] text-center font-plate text-[1em] font-bold leading-[1.08] tracking-[.07em] text-slate-950">
        {texto}
      </span>
    </span>
  );
}
