// Máscaras, validações e formatação usadas nos formulários (o back-end valida de novo).

export const somenteDigitos = (valor: string) => valor.replace(/\D/g, "");

/** Aplica a máscara 000.000.000-00 enquanto o usuário digita. */
export function mascaraCpf(valor: string): string {
  const d = somenteDigitos(valor).slice(0, 11);
  return d
    .replace(/^(\d{3})(\d)/, "$1.$2")
    .replace(/^(\d{3})\.(\d{3})(\d)/, "$1.$2.$3")
    .replace(/^(\d{3})\.(\d{3})\.(\d{3})(\d)/, "$1.$2.$3-$4");
}

/** Valida o CPF pelos dígitos verificadores. */
export function cpfValido(valor: string): boolean {
  const d = somenteDigitos(valor);
  if (d.length !== 11 || /^(\d)\1{10}$/.test(d)) return false;
  const digito = (qtd: number) => {
    let soma = 0;
    for (let i = 0; i < qtd; i++) soma += Number(d[i]) * (qtd + 1 - i);
    const resto = (soma * 10) % 11;
    return resto === 10 ? 0 : resto;
  };
  return digito(9) === Number(d[9]) && digito(10) === Number(d[10]);
}

/** Placa em maiúsculas, sem separador, no máximo 7 caracteres. */
export const normalizarPlaca = (valor: string) =>
  valor.replace(/[^0-9A-Za-z]/g, "").toUpperCase().slice(0, 7);

/** Placa antiga (ABC1234) ou Mercosul (ABC1D23). */
export const placaValida = (valor: string) => /^[A-Z]{3}\d[A-Z0-9]\d{2}$/.test(normalizarPlaca(valor));

/** Exibição: placa antiga com hífen (ABC-1234); Mercosul sem hífen. */
export function formatarPlaca(valor: string | null | undefined): string {
  const p = normalizarPlaca(valor ?? "");
  return /^[A-Z]{3}\d{4}$/.test(p) ? `${p.slice(0, 3)}-${p.slice(3)}` : p || (valor ?? "");
}

/** Converte "8.200,50", "8200,50" ou "8200.50" em número (ou NaN). */
export function parseMoeda(valor: string): number {
  const limpo = valor.replace(/R\$|\s/g, "");
  if (!limpo) return NaN;
  const normalizado = limpo.includes(",") ? limpo.replace(/\./g, "").replace(",", ".") : limpo;
  return /^-?\d+(\.\d+)?$/.test(normalizado) ? Number(normalizado) : NaN;
}

const moeda = new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" });
export const formatarMoeda = (valor: number | null | undefined) =>
  valor === null || valor === undefined ? "—" : moeda.format(valor);

export function formatarData(iso: string | null | undefined): string {
  if (!iso) return "—";
  const [ano, mes, dia] = iso.slice(0, 10).split("-");
  return `${dia}/${mes}/${ano}`;
}

export function formatarDataHora(iso: string | null | undefined): string {
  if (!iso) return "—";
  const d = new Date(iso);
  return d.toLocaleString("pt-BR", { day: "2-digit", month: "2-digit", year: "numeric", hour: "2-digit", minute: "2-digit" });
}

/** Data de hoje no formato yyyy-mm-dd (para o atributo min dos campos de data). */
export function hojeISO(): string {
  const d = new Date();
  const mes = String(d.getMonth() + 1).padStart(2, "0");
  const dia = String(d.getDate()).padStart(2, "0");
  return `${d.getFullYear()}-${mes}-${dia}`;
}

/** Diárias de um período inclusivo (mesmo dia = 1). Retorna 0 se as datas forem inválidas. */
export function contarDiarias(inicio: string, fim: string): number {
  if (!inicio || !fim) return 0;
  const ms = new Date(fim + "T00:00:00").getTime() - new Date(inicio + "T00:00:00").getTime();
  if (Number.isNaN(ms) || ms < 0) return 0;
  return Math.round(ms / 86_400_000) + 1;
}
