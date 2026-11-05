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

  useEffect(() => {
    carregarPedidos();
  }, []);

  const carregarPedidos = async () => {
    try {
      const dados = await api.listarPedidos();
      // Mostra apenas os pedidos do cliente logado
      const meusPedidos = user ? dados.filter((p: any) => p.cliente?.id === user.id) : dados;
      setPedidos(meusPedidos);
    } catch (error) {
      console.error(error);
    }
  };

  const criarPedido = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user) return alert("Você precisa estar logado como cliente!");
    try {
      await api.criarPedido({
        cliente: { id: user.id },
        automovel: { id: parseInt(carroId) }
      });
      setIsModalOpen(false);
      setCarroId("");
      carregarPedidos();
    } catch (err) {
      alert("Erro ao criar pedido. Verifique se o ID do veículo existe.");
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
          <button onClick={() => setIsModalOpen(true)} className="button-primary">
            <Icon name="plus" className="h-4 w-4" />
            Criar novo pedido
          </button>
        }
      />
      <div className="mb-6 flex flex-col justify-between gap-4 sm:flex-row sm:items-center">
        <div className="flex gap-1 rounded-xl bg-slate-200/70 p-1">
          {(["Todos", "PENDENTE", "APROVADO", "REJEITADO"]).map((item) => (
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
            </div>
          </article>
        ))}
      </section>

      {/* Modal de Criação de Pedido */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/50 backdrop-blur-sm px-4">
          <div className="bg-white rounded-2xl shadow-xl w-full max-w-md overflow-hidden">
            <div className="px-6 py-4 border-b border-slate-200 flex justify-between items-center">
              <h3 className="font-semibold text-lg text-slate-900">Solicitar Veículo</h3>
              <button onClick={() => setIsModalOpen(false)} className="text-slate-400 hover:text-slate-600">
                <Icon name="close" />
              </button>
            </div>
            <form onSubmit={criarPedido} className="p-6">
              <p className="text-sm text-slate-500 mb-4">Insira o ID do veículo que deseja alugar (Ex: 1). Você poderá gerenciar os veículos na aba Veículos em breve.</p>
              <TextInput label="ID do Automóvel" value={carroId} onChange={(e) => setCarroId(e.target.value)} required placeholder="Ex: 1" />
              <div className="mt-6 flex justify-end gap-3">
                <button type="button" onClick={() => setIsModalOpen(false)} className="button-secondary">Cancelar</button>
                <button type="submit" className="button-primary">Confirmar Pedido</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}