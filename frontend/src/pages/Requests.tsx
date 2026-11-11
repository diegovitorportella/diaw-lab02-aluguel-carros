import { useState, useEffect } from "react";
import Header from "../components/Header";
import Icon from "../components/Icon";
import OrderTimeline from "../components/OrderTimeline";
import StatusBadge, { ROTULOS } from "../components/StatusBadge";
import TextInput from "../components/TextInput";
import { api, ApiError } from "../services/api";
import { contarDiarias, formatarData, formatarMoeda, formatarPlaca, hojeISO } from "../utils/formatters";

type Erros = Record<string, string>;

const FILTROS = ["Todos", "PENDENTE", "APROVADO", "CONCLUIDO", "REJEITADO", "CANCELADO"];

export default function Requests({ user }: { user?: any }) {
  const [pedidos, setPedidos] = useState<any[]>([]);
  const [filter, setFilter] = useState<string>("Todos");
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [carroId, setCarroId] = useState("");
  const [dataInicio, setDataInicio] = useState("");
  const [dataFim, setDataFim] = useState("");
  const [observacao, setObservacao] = useState("");
  const [editando, setEditando] = useState<any | null>(null);
  const [erro, setErro] = useState("");
  const [erros, setErros] = useState<Erros>({});
  const [veiculos, setVeiculos] = useState<any[]>([]);

  useEffect(() => {
    carregarPedidos();
    api.listarAutomoveis().then(setVeiculos).catch(console.error);
  }, []);

  const carregarPedidos = async () => {
    try {
      setPedidos(await api.listarPedidos()); // o back-end devolve apenas os pedidos do cliente logado
    } catch (error) {
      console.error(error);
    }
  };

  const limparFormulario = () => {
    setCarroId(""); setDataInicio(""); setDataFim(""); setObservacao("");
    setEditando(null); setIsModalOpen(false); setErro(""); setErros({});
  };

  const abrirEdicao = (pedido: any) => {
    setEditando(pedido);
    setCarroId(String(pedido.automovel?.id ?? ""));
    setDataInicio(pedido.dataInicio ?? "");
    setDataFim(pedido.dataFim ?? "");
    setObservacao(pedido.observacao ?? "");
    setErro(""); setErros({});
    setIsModalOpen(true);
  };

  const cancelarPedido = async (pedido: any) => {
    if (!user || !confirm(`Cancelar o pedido REQ-${pedido.id}?`)) return;
    try {
      await api.cancelarPedido(pedido.id);
      carregarPedidos();
    } catch (err: any) {
      alert(err.message);
    }
  };

  // Simulação do valor do aluguel (o back-end recalcula e grava o valor oficial)
  const veiculoSelecionado = veiculos.find((v) => String(v.id) === carroId);
  const diarias = contarDiarias(dataInicio, dataFim);
  const totalSimulado = veiculoSelecionado?.valorDiaria != null && diarias > 0
    ? diarias * veiculoSelecionado.valorDiaria
    : null;

  /** Validação no navegador (o back-end valida de novo). */
  const validar = (): Erros => {
    const e: Erros = {};
    if (!carroId) e.automovel = "Selecione um veículo.";
    if (!dataInicio) e.dataInicio = "Informe a data inicial.";
    else if (!editando && dataInicio < hojeISO()) e.dataInicio = "A data inicial não pode ser anterior a hoje.";
    if (!dataFim) e.dataFim = "Informe a data final.";
    else if (dataInicio && dataFim < dataInicio) e.dataFim = "A data final não pode ser anterior à data inicial.";
    return e;
  };

  const salvarPedido = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user) return alert("Você precisa estar logado como cliente!");
    setErro("");
    const encontrados = validar();
    setErros(encontrados);
    if (Object.keys(encontrados).length > 0) return;

    const dados = {
      automovel: { id: parseInt(carroId) },
      dataInicio,
      dataFim,
      observacao: observacao || null,
    };
    try {
      if (editando) {
        await api.modificarPedido(editando.id, dados);
      } else {
        await api.criarPedido(dados);
      }
      limparFormulario();
      carregarPedidos();
    } catch (err: any) {
      if (err instanceof ApiError && Object.keys(err.campos).length > 0) setErros(err.campos);
      setErro(err.message);
    }
  };

  const filtered = filter === "Todos" ? pedidos : pedidos.filter((p) => p.status === filter);
  const veiculosDisponiveis = veiculos.filter((v) => v.proprietario?.id !== user?.id); // não se aluga o próprio carro

  return (
    <div className="page-wrap">
      <Header
        eyebrow="Portal do Cliente"
        title="Meus pedidos de aluguel"
        description="Acompanhe suas solicitações do pedido até a assinatura do contrato."
        action={
          <button onClick={() => { limparFormulario(); setIsModalOpen(true); }} className="button-primary">
            <Icon name="plus" className="h-4 w-4" />
            Criar novo pedido
          </button>
        }
      />
      <div className="mb-6 flex flex-col justify-between gap-4 sm:flex-row sm:items-center">
        <div className="flex flex-wrap gap-1 rounded-xl bg-slate-200/70 p-1">
          {FILTROS.map((item) => (
            <button
              onClick={() => setFilter(item)}
              key={item}
              className={`rounded-lg px-4 py-2 text-sm font-semibold transition ${
                filter === item ? "bg-white text-slate-950 shadow-sm" : "text-slate-500 hover:text-slate-800"
              }`}
            >
              {item === "Todos" ? item : ROTULOS[item]}
            </button>
          ))}
        </div>
        <p className="text-sm text-slate-500">
          Exibindo <b className="text-slate-800">{filtered.length}</b> solicitações
        </p>
      </div>

      {filtered.length === 0 && (
         <p className="text-slate-500 mt-10 text-center">Nenhum pedido encontrado.</p>
      )}

      <section className="grid gap-6 lg:grid-cols-2 2xl:grid-cols-3">
        {filtered.map((pedido) => (
          <article key={pedido.id} className="card overflow-hidden">
            <div className="flex items-center justify-between bg-slate-900 px-5 py-4">
              <div className="flex items-center gap-3">
                <span className="grid h-11 w-11 place-items-center rounded-xl bg-white/10 text-slate-200">
                  <Icon name="car" className="h-6 w-6" />
                </span>
                <div>
                  <p className="text-xs font-bold uppercase tracking-wider text-blue-300">
                    {pedido.automovel?.marca || "Veículo"}
                  </p>
                  <h2 className="text-lg font-semibold text-white">
                    {pedido.automovel?.modelo || "Modelo não especificado"}
                  </h2>
                </div>
              </div>
              <StatusBadge status={pedido.status} />
            </div>

            <div className="p-5">
              <dl className="grid grid-cols-2 gap-x-4 gap-y-3 text-sm">
                <div>
                  <dt className="text-xs text-slate-400">Pedido</dt>
                  <dd className="font-semibold text-slate-700">REQ-{pedido.id}</dd>
                </div>
                <div>
                  <dt className="text-xs text-slate-400">Placa</dt>
                  <dd className="font-semibold text-slate-700">{formatarPlaca(pedido.automovel?.placa) || "—"}</dd>
                </div>
                <div>
                  <dt className="text-xs text-slate-400">Período</dt>
                  <dd className="font-semibold text-slate-700">
                    {formatarData(pedido.dataInicio)} a {formatarData(pedido.dataFim)}
                  </dd>
                </div>
                <div>
                  <dt className="text-xs text-slate-400">Valor total</dt>
                  <dd className="font-semibold text-slate-900">{formatarMoeda(pedido.valorTotal)}</dd>
                </div>
              </dl>

              {pedido.contrato?.valorFinanciado != null && (
                <div className="mt-4 rounded-xl border border-blue-100 bg-blue-50/50 p-3 text-xs text-slate-600">
                  <p className="font-semibold text-blue-800">Financiamento — {pedido.contrato.banco?.razaoSocial || "Banco"}</p>
                  <p className="mt-1">
                    {formatarMoeda(pedido.contrato.valorFinanciado)} em {pedido.contrato.numeroParcelas ?? "—"}x de{" "}
                    <b>{formatarMoeda(pedido.contrato.valorParcela)}</b> ({pedido.contrato.taxaJuros}% a.a.)
                  </p>
                </div>
              )}

              <div className="mt-5 border-t border-slate-100 pt-5">
                <OrderTimeline status={pedido.status} historico={pedido.historico} />
              </div>

              {(pedido.status === "PENDENTE" || pedido.status === "APROVADO") && (
                <div className="mt-5 flex gap-3">
                  {pedido.status === "PENDENTE" && (
                    <button onClick={() => abrirEdicao(pedido)} className="button-secondary flex-1">Modificar</button>
                  )}
                  <button onClick={() => cancelarPedido(pedido)} className="button-secondary flex-1 text-red-600">Cancelar</button>
                </div>
              )}
            </div>
          </article>
        ))}
      </section>

      {/* Modal de criação / modificação de pedido */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/50 backdrop-blur-sm px-4">
          <div className="max-h-[92vh] overflow-y-auto bg-white rounded-2xl shadow-xl w-full max-w-md">
            <div className="px-6 py-4 border-b border-slate-200 flex justify-between items-center">
              <h3 className="font-semibold text-lg text-slate-900">
                {editando ? `Modificar pedido REQ-${editando.id}` : "Solicitar Veículo"}
              </h3>
              <button onClick={limparFormulario} className="text-slate-400 hover:text-slate-600">
                <Icon name="close" />
              </button>
            </div>
            <form onSubmit={salvarPedido} noValidate className="p-6 space-y-4">
              {erro && Object.keys(erros).length === 0 && (
                <div className="rounded-xl bg-red-50 p-3 text-sm font-medium text-red-800">{erro}</div>
              )}
              <label className="block text-sm font-semibold text-slate-700">
                Automóvel
                <select
                  value={carroId}
                  onChange={(e) => setCarroId(e.target.value)}
                  className={`mt-2 w-full rounded-xl border bg-white px-3 py-2.5 text-slate-900 ${erros.automovel ? "border-red-400" : "border-slate-200"}`}
                >
                  <option value="">Selecione um veículo</option>
                  {veiculosDisponiveis.map((v) => (
                    <option key={v.id} value={v.id}>
                      {v.marca} {v.modelo} ({v.ano}) — {formatarPlaca(v.placa)}
                      {v.valorDiaria != null ? ` — ${formatarMoeda(v.valorDiaria)}/dia` : ""}
                    </option>
                  ))}
                </select>
                {erros.automovel && <span className="mt-1.5 block text-xs font-medium text-red-600">{erros.automovel}</span>}
              </label>
              <div className="grid grid-cols-2 gap-4">
                <TextInput label="Data inicial" type="date" value={dataInicio} min={editando ? undefined : hojeISO()} onChange={(e) => setDataInicio(e.target.value)} error={erros.dataInicio} />
                <TextInput label="Data final" type="date" value={dataFim} min={dataInicio || (editando ? undefined : hojeISO())} onChange={(e) => setDataFim(e.target.value)} error={erros.dataFim} />
              </div>

              {/* Simulação do valor */}
              <div className="rounded-xl border border-blue-100 bg-blue-50/60 p-4 text-sm">
                <p className="text-xs font-bold uppercase tracking-wider text-blue-700">Simulação do valor</p>
                {totalSimulado !== null ? (
                  <p className="mt-2 text-slate-700">
                    {diarias} {diarias === 1 ? "diária" : "diárias"} × {formatarMoeda(veiculoSelecionado.valorDiaria)} ={" "}
                    <b className="text-lg text-slate-950">{formatarMoeda(totalSimulado)}</b>
                  </p>
                ) : (
                  <p className="mt-2 text-slate-500">
                    {veiculoSelecionado && veiculoSelecionado.valorDiaria == null
                      ? "Este veículo não tem diária cadastrada, então o valor total não pode ser calculado."
                      : "Selecione o veículo e o período para ver o valor total."}
                  </p>
                )}
              </div>

              <TextInput label="Observação" value={observacao} onChange={(e) => setObservacao(e.target.value)} placeholder="Opcional" />
              <div className="mt-6 flex justify-end gap-3">
                <button type="button" onClick={limparFormulario} className="button-secondary">Cancelar</button>
                <button type="submit" className="button-primary">{editando ? "Salvar alterações" : "Confirmar Pedido"}</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
