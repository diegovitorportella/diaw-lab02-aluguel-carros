import { useState, useEffect } from "react";
import Header from "../components/Header";
import Icon from "../components/Icon";
import StatusBadge from "../components/StatusBadge";
import TextInput from "../components/TextInput";
import { api } from "../services/api";
import type { Status } from "../components/StatusBadge";

export default function Requests({ user }: { user?: any }) {
  const [pedidos, setPedidos] = useState<any[]>([]);
  const [filter, setFilter] = useState<"Todos" | Status | string>("Todos");
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [carroId, setCarroId] = useState("");
  const [dataInicio, setDataInicio] = useState("");
  const [dataFim, setDataFim] = useState("");
  const [observacao, setObservacao] = useState("");
  const [editando, setEditando] = useState<any | null>(null);
  const [erro, setErro] = useState("");
  const [veiculos, setVeiculos] = useState<any[]>([]);

  useEffect(() => {
    carregarPedidos();
    api.listarAutomoveis().then(setVeiculos).catch(console.error);
  }, []);

  const limparFormulario = () => {
    setCarroId(""); setDataInicio(""); setDataFim(""); setObservacao("");
    setEditando(null); setIsModalOpen(false); setErro("");
  };

  const abrirEdicao = (pedido: any) => {
    setEditando(pedido);
    setCarroId(String(pedido.automovel?.id ?? ""));
    setDataInicio(pedido.dataInicio ?? "");
    setDataFim(pedido.dataFim ?? "");
    setObservacao(pedido.observacao ?? "");
    setErro("");
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

  const carregarPedidos = async () => {
    try {
      const dados = await api.listarPedidos();
      setPedidos(dados); // o back-end já devolve apenas os pedidos do cliente logado
    } catch (error) {
      console.error(error);
    }
  };

  const salvarPedido = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user) return alert("Você precisa estar logado como cliente!");
    setErro("");
    const dados = {
      automovel: { id: parseInt(carroId) },
      dataInicio: dataInicio || null,
      dataFim: dataFim || null,
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
      setErro(err.message);
    }
  };

  const filtered = filter === "Todos" ? pedidos : pedidos.filter((p) => p.status === filter);

  return (
    <div className="page-wrap">
      <Header
        eyebrow="Portal do Cliente"
        title="Meus pedidos de aluguel"
        description="Acompanhe suas solicitações e gerencie seus pedidos de veículos."
        action={
          <button onClick={() => { limparFormulario(); setIsModalOpen(true); }} className="button-primary">
            <Icon name="plus" className="h-4 w-4" />
            Criar novo pedido
          </button>
        }
      />
      <div className="mb-6 flex flex-col justify-between gap-4 sm:flex-row sm:items-center">
        <div className="flex gap-1 rounded-xl bg-slate-200/70 p-1">
          {(["Todos", "PENDENTE", "APROVADO", "REJEITADO", "CANCELADO"]).map((item) => (
            <button
              onClick={() => setFilter(item)}
              key={item}
              className={`rounded-lg px-4 py-2 text-sm font-semibold transition ${
                filter === item ? "bg-white text-slate-950 shadow-sm" : "text-slate-500 hover:text-slate-800"
              }`}
            >
              {item}
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

      <section className="grid gap-6 xl:grid-cols-3">
        {filtered.map((pedido) => (
          <article key={pedido.id} className="card group overflow-hidden">
            <div className="relative h-52 overflow-hidden bg-slate-900 flex items-center justify-center">
              <Icon name="car" className="h-20 w-20 text-slate-700" />
              <div className="absolute left-4 top-4">
                <StatusBadge status={pedido.status} />
              </div>
            </div>
            <div className="p-5">
              <div className="flex items-start justify-between">
                <div>
                  <p className="text-xs font-bold uppercase tracking-wider text-blue-600">
                    {pedido.automovel?.marca || "Veículo"}
                  </p>
                  <h2 className="mt-1.5 text-xl font-semibold text-slate-950">
                    {pedido.automovel?.modelo || "Modelo não especificado"}
                  </h2>
                </div>
              </div>
              <div className="my-5 grid grid-cols-2 divide-x divide-slate-200 border-y border-slate-100 py-4">
                <div>
                  <p className="text-xs text-slate-400">ID do Pedido</p>
                  <p className="mt-1 text-sm font-semibold text-slate-700">REQ-{pedido.id}</p>
                </div>
                <div className="pl-4">
                  <p className="text-xs text-slate-400">Análise Financeira</p>
                  <p className="mt-1 text-sm font-semibold text-slate-700">
                    {pedido.parecerFinanceiro || "Aguardando"}
                  </p>
                </div>
              </div>
              {(pedido.dataInicio || pedido.dataFim) && (
                <p className="mb-4 text-xs text-slate-500">
                  Período: {pedido.dataInicio || "—"} a {pedido.dataFim || "—"}
                </p>
              )}
              {(pedido.status === "PENDENTE" || (pedido.status === "APROVADO" && !pedido.contrato)) && (
                <div className="flex gap-3">
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
          <div className="bg-white rounded-2xl shadow-xl w-full max-w-md overflow-hidden">
            <div className="px-6 py-4 border-b border-slate-200 flex justify-between items-center">
              <h3 className="font-semibold text-lg text-slate-900">
                {editando ? `Modificar pedido REQ-${editando.id}` : "Solicitar Veículo"}
              </h3>
              <button onClick={limparFormulario} className="text-slate-400 hover:text-slate-600">
                <Icon name="close" />
              </button>
            </div>
            <form onSubmit={salvarPedido} className="p-6 space-y-4">
              {erro && <div className="rounded-xl bg-red-50 p-3 text-sm font-medium text-red-800">{erro}</div>}
              <label className="block text-sm font-medium text-slate-700">
                Automóvel
                <select
                  required
                  value={carroId}
                  onChange={(e) => setCarroId(e.target.value)}
                  className="mt-1 w-full rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-slate-900"
                >
                  <option value="">Selecione um veículo</option>
                  {veiculos.map((v) => (
                    <option key={v.id} value={v.id}>
                      {v.marca} {v.modelo} ({v.ano}) — {v.placa}
                    </option>
                  ))}
                </select>
              </label>
              <div className="grid grid-cols-2 gap-4">
                <TextInput label="Data inicial" type="date" value={dataInicio} onChange={(e) => setDataInicio(e.target.value)} />
                <TextInput label="Data final" type="date" value={dataFim} onChange={(e) => setDataFim(e.target.value)} />
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
