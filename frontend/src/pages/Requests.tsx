import { useState, useEffect } from "react";
import Header from "../components/Header";
import Icon from "../components/Icon";
import StatusBadge from "../components/StatusBadge";
import { api } from "../services/api";
import type { Status } from "../components/StatusBadge";

export default function Requests() {
  const [pedidos, setPedidos] = useState<any[]>([]);
  const [filter, setFilter] = useState<"All" | Status | string>("All");

  useEffect(() => {
    carregarPedidos();
  }, []);

  const carregarPedidos = async () => {
    try {
      const dados = await api.listarPedidos();
      setPedidos(dados);
    } catch (error) {
      console.error(error);
    }
  };

  const filtered = filter === "All" ? pedidos : pedidos.filter((p) => p.status === filter);

  return (
    <div className="page-wrap">
      <Header
        eyebrow="Client portal"
        title="My rental requests"
        description="Track your applications and manage your vehicle requests."
        action={
          <button className="button-primary">
            <Icon name="plus" className="h-4 w-4" />
            Create new request
          </button>
        }
      />
      <div className="mb-6 flex flex-col justify-between gap-4 sm:flex-row sm:items-center">
        <div className="flex gap-1 rounded-xl bg-slate-200/70 p-1">
          {(["All", "PENDENTE", "APROVADO", "REJEITADO"]).map((item) => (
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
          Showing <b className="text-slate-800">{filtered.length}</b> requests
        </p>
      </div>
      
      {filtered.length === 0 && (
         <p className="text-slate-500 mt-10 text-center">Nenhum pedido encontrado.</p>
      )}

      <section className="grid gap-6 xl:grid-cols-3">
        {filtered.map((pedido) => (
          <article key={pedido.id} className="card group overflow-hidden">
            <div className="relative h-52 overflow-hidden bg-slate-900">
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
                  <p className="text-xs text-slate-400">Client ID</p>
                  <p className="mt-1 text-sm font-semibold text-slate-700">
                    {pedido.cliente?.id}
                  </p>
                </div>
                <div className="pl-4">
                  <p className="text-xs text-slate-400">Financial Review</p>
                  <p className="mt-1 text-sm font-semibold text-slate-700">
                    {pedido.parecerFinanceiro || "Aguardando"}
                  </p>
                </div>
              </div>
            </div>
          </article>
        ))}
      </section>
    </div>
  );
}