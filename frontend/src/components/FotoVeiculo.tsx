import type { ReactNode } from "react";

// Fotos em src/assets/carros/<chave>.webp (créditos em docs/CREDITOS_FOTOS.md)
const FOTOS = import.meta.glob("../assets/carros/*.webp", { eager: true, query: "?url", import: "default" }) as Record<
  string,
  string
>;
const fotoPorChave = (chave: string): string | undefined => FOTOS[`../assets/carros/${chave}.webp`];

// O veículo é reconhecido pelo modelo, então carros cadastrados pelos usuários com o mesmo modelo também ganham foto.
const MODELOS: [RegExp, string][] = [
  [/argo/, "argo"],
  [/onix/, "onix"],
  [/corolla/, "corolla"],
  [/compass/, "compass"],
  [/t-?cross/, "tcross"],
  [/hr-?v/, "hrv"],
  [/hb-?20/, "hb20"],
  [/tracker/, "tracker"],
  [/dolphin/, "dolphin"],
  [/amg/, "amg"],
  [/palio/, "palio"],
];

const normalizar = (texto: string) =>
  texto.normalize("NFD").replace(/[̀-ͯ]/g, "").toLowerCase();

export function fotoDoVeiculo(carro?: { marca?: string; modelo?: string } | null): string | undefined {
  if (!carro) return undefined;
  const texto = normalizar(`${carro.marca ?? ""} ${carro.modelo ?? ""}`);
  const achado = MODELOS.find(([regra]) => regra.test(texto));
  return achado ? fotoPorChave(achado[1]) : undefined;
}

/** Silhueta usada quando o modelo não tem foto: mantém o cartão com a mesma altura e identidade. */
function Silhueta() {
  return (
    <div className="grid h-full w-full place-items-center bg-linear-to-br from-blue-900 to-slate-950">
      <svg viewBox="0 0 120 56" className="w-2/5 text-blue-300/70" fill="currentColor" aria-hidden="true">
        <path d="M6 40c0-4 2-7 6-8l10-14c2-3 5-5 9-5h26c5 0 8 2 11 5l9 10 12 3c4 1 6 4 6 8v4c0 2-1 3-3 3h-5a9 9 0 0 0-18 0H35a9 9 0 0 0-18 0H9c-2 0-3-1-3-3zM30 21l-7 10h21V19h-9c-2 0-4 1-5 2zm20-2v12h20l-8-10c-1-1-3-2-5-2z" />
        <circle cx="26" cy="47" r="6" />
        <circle cx="86" cy="47" r="6" />
      </svg>
    </div>
  );
}

/**
 * Foto do veículo. O filho (a placa) atravessa a borda inferior da imagem, metade sobre a foto e
 * metade sobre o cartão, para não cobrir o carro.
 */
export default function FotoVeiculo({
  carro,
  className = "aspect-video",
  children,
}: {
  carro?: { marca?: string; modelo?: string } | null;
  className?: string;
  children?: ReactNode;
}) {
  const foto = fotoDoVeiculo(carro);
  const descricao = carro ? `${carro.marca ?? ""} ${carro.modelo ?? ""}`.trim() : "Veículo";

  return (
    <div className="relative">
      <div className={`overflow-hidden bg-slate-950 ${className}`}>
        {foto ? (
          <img src={foto} alt={descricao} loading="lazy" className="h-full w-full object-cover" />
        ) : (
          <Silhueta />
        )}
      </div>
      {children && <div className="absolute -bottom-4 left-4 z-10 drop-shadow-md">{children}</div>}
    </div>
  );
}
