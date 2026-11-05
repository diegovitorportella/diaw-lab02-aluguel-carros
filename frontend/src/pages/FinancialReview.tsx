import { useState, useEffect } from "react";
import Header from "../components/Header";
import Icon from "../components/Icon";
import TextInput from "../components/TextInput";
import { api } from "../services/api";

export default function FinancialReview({ user }: { user?: any }) {
  const [pedidos, setPedidos] = useState<any[]>([]);
  const [selected, setSelected] = useState<any | null>(null);
  const [decision, setDecision] = useState<"approve" | "reject">("approve");
  const [submitted, setSubmitted] = useState(false);

  const [valorFinanciado, setValorFinanciado] = useState("");
  const [taxaJuros, setTaxaJuros] = useState("");

  const [dataInicio, setDataInicio] = useState("");
  const [dataFim, setDataFim] = useState("");
  const [observacao, setObservacao] = useState("");
  const [erro, setErro] = useState("");
  const [aviso, setAviso] = useState("");
  const isBanco = user?.tipo === "BANCO";

  const selecionar = (pedido: any | null) => {
    setSelected(pedido);
    setDataInicio(pedido?.dataInicio ?? "");
    setDataFim(pedido?.dataFim ?? "");
    setObservacao(pedido?.observacao ?? "");
    setErro("");
    setAviso("");
  };

  const salvarAlteracoes = async () => {
    if (!selected || !user) return;
    setErro(""); setAviso("");
    try {
      const atualizado = await api.modificarPedido(selected.id, user.id, {
        automovel: { id: selected.automovel?.id },
        dataInicio: dataInicio || null,
        dataFim: dataFim || null,
        observacao: observacao || null,
      });
      setSelected(atualizado);
      setAviso("Pedido modificado com sucesso.");
      carregarPedidos();
    } catch (err: any) {
      setErro(err.message);
    }
  };

  useEffect(() => {
    carregarPedidos();
  }, []);

  const carregarPedidos = async () => {
    try {
      const dados = await api.listarPedidos();
      setPedidos(dados.filter((p: any) => p.status === 'PENDENTE'));
    } catch (err) {
      console.error(err);
    }
  };

  const submitDecision = async () => {
    if (!selected || !user) return;
    setErro(""); setAviso("");
    try {
      const parecer = decision === "approve" ? "POSITIVO" : "NEGATIVO";
      await api.avaliarPedido(selected.id, parecer, user.id);

      if (parecer === "POSITIVO" && valorFinanciado && isBanco) {
        await api.gerarContrato(selected.id, {
          valorFinanciado: parseFloat(valorFinanciado),
          taxaJuros: parseFloat(taxaJuros),
          bancoId: user.id,
        });
      }

      setSubmitted(true);
      setValorFinanciado(""); setTaxaJuros("");
      setTimeout(() => {
        setSubmitted(false);
        selecionar(null);
        carregarPedidos();
      }, 2000);
    } catch (err: any) {
      setErro(err.message);
    }
  };

  return (
    <div className="page-wrap max-w-none">
      <Header
        eyebrow="Operações de Crédito"
        title="Análise financeira"
        description="Analise solicitações pendentes e emita decisões de crédito."
      />
      
      <section className="card overflow-hidden mt-6">
        <div className="overflow-x-auto">
          <table className="w-full min-w-215 text-left">
            <thead>
              <tr className="border-b border-slate-200 bg-slate-50 text-[11px] font-bold uppercase tracking-wider text-slate-400">
                <th className="px-6 py-4">ID / Nome do Cliente</th>
                <th>Modelo do Veículo</th>
                <th>Status</th>
                <th>Parecer</th>
                <th />
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {pedidos.length === 0 && (
                <tr><td colSpan={5} className="p-6 text-center text-slate-500">Nenhum pedido pendente de análise.</td></tr>
              )}
              {pedidos.map((row) => (
                <tr
                  key={row.id}
                  onClick={() => selecionar(row)}
                  className={`cursor-pointer text-sm transition hover:bg-blue-50/50 ${
                    selected?.id === row.id ? "bg-blue-50/70" : "bg-white"
                  }`}
                >
                  <td className="px-6 py-4 font-semibold text-slate-900">
                    {row.cliente?.nome} (ID: {row.cliente?.id})
                  </td>
                  <td className="font-medium text-slate-700">{row.automovel?.modelo || "N/A"}</td>
                  <td className="text-amber-600 font-bold">{row.status}</td>
                  <td className="text-slate-500">{row.parecerFinanceiro || "Pendente"}</td>
                  <td><Icon name="chevron" className="h-4 w-4 text-slate-400" /></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      {selected && (
        <>
          <button className="fixed inset-0 z-40 bg-slate-950/25 backdrop-blur-[2px]" onClick={() => selecionar(null)} />
          <aside className="fixed inset-y-0 right-0 z-50 w-full max-w-127.5 overflow-y-auto bg-white shadow-2xl">
            <div className="sticky top-0 z-10 flex items-center justify-between border-b border-slate-200 bg-white/95 px-6 py-5 backdrop-blur">
              <div>
                <p className="text-xs font-bold uppercase tracking-wider text-blue-600">REQ-{selected.id}</p>
                <h2 className="mt-1 text-xl font-semibold">Analisar solicitação</h2>
              </div>
              <button onClick={() => selecionar(null)} className="header-button"><Icon name="close" /></button>
            </div>
            
            <div className="space-y-7 p-6">
              {submitted && (
                <div className="flex items-center gap-3 rounded-xl bg-emerald-50 p-4 text-sm font-semibold text-emerald-800">
                  <Icon name="check" className="h-5 w-5" /> Decisão enviada com sucesso
                </div>
              )}
              
              {erro && <div className="rounded-xl bg-red-50 p-4 text-sm font-medium text-red-800">{erro}</div>}
              {aviso && <div className="rounded-xl bg-emerald-50 p-4 text-sm font-medium text-emerald-800">{aviso}</div>}

              <div>
                <h3 className="panel-title">Modificar pedido</h3>
                <div className="mt-4 grid grid-cols-2 gap-4">
                  <TextInput label="Data inicial" type="date" value={dataInicio} onChange={e => setDataInicio(e.target.value)} />
                  <TextInput label="Data final" type="date" value={dataFim} onChange={e => setDataFim(e.target.value)} />
                  <TextInput label="Observação" value={observacao} onChange={e => setObservacao(e.target.value)} wide />
                </div>
                <button onClick={salvarAlteracoes} className="button-secondary mt-4">Salvar alterações</button>
              </div>

              <div>
                <h3 className="panel-title">Análise financeira</h3>
                <div className="mt-4 grid grid-cols-2 gap-3">
                  <button onClick={() => setDecision("approve")} className={`decision ${decision === "approve" ? "approve" : ""}`}>
                    <span className="decision-icon"><Icon name="check" /></span>
                    <span><b>Aprovar</b><small>Elegível para crédito</small></span>
                  </button>
                  <button onClick={() => setDecision("reject")} className={`decision ${decision === "reject" ? "reject" : ""}`}>
                    <span className="decision-icon"><Icon name="close" /></span>
                    <span><b>Rejeitar</b><small>Recusar pedido</small></span>
                  </button>
                </div>
              </div>

              {decision === "approve" && isBanco && (
                <div className="rounded-2xl border border-blue-100 bg-blue-50/40 p-5">
                  <div className="mb-5 flex items-center gap-3">
                    <span className="grid h-9 w-9 place-items-center rounded-lg bg-blue-600 text-white"><Icon name="file" className="h-4 w-4" /></span>
                    <div>
                      <h3 className="font-semibold text-slate-950">Contrato de crédito</h3>
                      <p className="text-xs text-slate-500">Configuração do agente bancário</p>
                    </div>
                  </div>
                  <div className="grid grid-cols-2 gap-4">
                    <TextInput label="Valor financiado" value={valorFinanciado} onChange={e => setValorFinanciado(e.target.value)} placeholder="Ex: 38900.00" />
                    <TextInput label="Taxa de juros (a.a.)" value={taxaJuros} onChange={e => setTaxaJuros(e.target.value)} placeholder="Ex: 8.75" />
                  </div>
                </div>
              )}
            </div>
            <div className="sticky bottom-0 flex gap-3 border-t border-slate-200 bg-white p-6">
              <button onClick={submitDecision} className={`flex flex-1 items-center justify-center gap-2 rounded-xl px-5 py-3 text-sm font-semibold text-white ${decision === "approve" ? "bg-blue-600 hover:bg-blue-700" : "bg-red-600 hover:bg-red-700"}`}>
                Enviar decisão <Icon name="chevron" className="h-4 w-4" />
              </button>
            </div>
          </aside>
        </>
      )}
    </div>
  );
}