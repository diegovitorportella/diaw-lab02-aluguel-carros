import { useState, useEffect } from "react";
import Header from "../components/Header";
import Icon from "../components/Icon";
import OrderTimeline from "../components/OrderTimeline";
import StatusBadge from "../components/StatusBadge";
import TextInput from "../components/TextInput";
import { api, ApiError } from "../services/api";
import { formatarData, formatarMoeda, formatarPlaca, parseMoeda } from "../utils/formatters";

type Erros = Record<string, string>;
type Aba = "analise" | "contrato" | "historico";
const STATUS_HISTORICO = ["CONCLUIDO", "REJEITADO", "CANCELADO"];
const ROTULO_STATUS: Record<string, string> = { CONCLUIDO: "Concluídos", REJEITADO: "Rejeitados", CANCELADO: "Cancelados" };

export default function FinancialReview({ user }: { user?: any }) {
  const [pedidos, setPedidos] = useState<any[]>([]);
  const [aba, setAba] = useState<Aba>("analise");
  const [selected, setSelected] = useState<any | null>(null);
  const [decision, setDecision] = useState<"approve" | "reject">("approve");
  const [mensagemSucesso, setMensagemSucesso] = useState("");

  // aba "Histórico": análises já encerradas
  const [filtroStatus, setFiltroStatus] = useState("TODOS");
  const [somenteMinhas, setSomenteMinhas] = useState(false);

  // modificação do pedido (fase de análise)
  const [observacao, setObservacao] = useState("");

  // execução do contrato (pedido aprovado)
  const [comCredito, setComCredito] = useState(false);
  const [entrada, setEntrada] = useState("");
  const [parcelas, setParcelas] = useState("12");
  const [juros, setJuros] = useState("");
  const [valorFinanciado, setValorFinanciado] = useState("");
  const [simulacao, setSimulacao] = useState<any | null>(null);
  const [erroSimulacao, setErroSimulacao] = useState("");

  const [erro, setErro] = useState("");
  const [erros, setErros] = useState<Erros>({});
  const [aviso, setAviso] = useState("");

  const isBanco = user?.tipo === "BANCO";

  useEffect(() => {
    carregarPedidos();
  }, []);

  const carregarPedidos = async () => {
    try {
      setPedidos(await api.listarPedidos());
    } catch (err) {
      console.error(err);
    }
  };

  const emAnalise = pedidos.filter((p) => p.status === "PENDENTE");
  const aguardandoContrato = pedidos.filter((p) => p.status === "APROVADO");
  const encerrados = pedidos
    .filter((p) => STATUS_HISTORICO.includes(p.status))
    .filter((p) => filtroStatus === "TODOS" || p.status === filtroStatus)
    .filter((p) => !somenteMinhas || p.agenteAvaliador?.id === user?.id)
    .sort((a, b) => b.id - a.id);
  const totalEncerrados = pedidos.filter((p) => STATUS_HISTORICO.includes(p.status)).length;
  const lista = aba === "analise" ? emAnalise : aba === "contrato" ? aguardandoContrato : encerrados;
  const historico = aba === "historico";

  const selecionar = (pedido: any | null) => {
    setSelected(pedido);
    setDecision("approve");
    setObservacao(pedido?.observacao ?? "");
    setComCredito(false);
    setEntrada(""); setParcelas("12"); setJuros(""); setValorFinanciado("");
    setSimulacao(null); setErroSimulacao("");
    setErro(""); setErros({}); setAviso(""); setMensagemSucesso("");
  };

  const rendaTotal = (pedido: any) =>
    (pedido?.cliente?.rendimentosAuferidos ?? []).reduce((soma: number, v: number) => soma + v, 0);

  // Simulação ao vivo do financiamento (parcela, total pago e comprometimento da renda)
  useEffect(() => {
    if (!selected || selected.status !== "APROVADO" || !isBanco || !comCredito) {
      setSimulacao(null);
      setErroSimulacao("");
      return;
    }
    const nParcelas = parseInt(parcelas);
    const nJuros = parseMoeda(juros);
    const nEntrada = entrada.trim() ? parseMoeda(entrada) : 0;
    const nFinanciado = valorFinanciado.trim() ? parseMoeda(valorFinanciado) : undefined;
    if (!nParcelas || Number.isNaN(nJuros) || Number.isNaN(nEntrada) || (nFinanciado !== undefined && Number.isNaN(nFinanciado))) {
      setSimulacao(null);
      setErroSimulacao("");
      return;
    }
    const timer = setTimeout(async () => {
      try {
        const resultado = await api.simularFinanciamento(selected.id, {
          parcelas: nParcelas,
          juros: nJuros,
          entrada: nEntrada,
          valorFinanciado: nFinanciado,
        });
        setSimulacao(resultado);
        setErroSimulacao("");
      } catch (err: any) {
        setSimulacao(null);
        setErroSimulacao(err.message);
      }
    }, 400);
    return () => clearTimeout(timer);
  }, [selected, isBanco, comCredito, entrada, parcelas, juros, valorFinanciado]);

  const tratarErro = (err: any) => {
    if (err instanceof ApiError && Object.keys(err.campos).length > 0) setErros(err.campos);
    setErro(err.message);
  };

  const salvarAlteracoes = async () => {
    if (!selected) return;
    setErro(""); setErros({}); setAviso("");
    try {
      const atualizado = await api.modificarPedido(selected.id, {
        observacao: observacao || null,
      });
      setSelected(atualizado);
      setAviso("Observação atualizada.");
      carregarPedidos();
    } catch (err: any) {
      tratarErro(err);
    }
  };

  const concluirAposAcao = (mensagem: string) => {
    setMensagemSucesso(mensagem);
    setTimeout(() => {
      selecionar(null);
      carregarPedidos();
    }, 1800);
  };

  const enviarParecer = async () => {
    if (!selected) return;
    setErro(""); setErros({}); setAviso("");
    try {
      await api.avaliarPedido(selected.id, decision === "approve" ? "POSITIVO" : "NEGATIVO");
      concluirAposAcao(decision === "approve"
        ? "Pedido aprovado. Ele segue para a aba “Aguardando contrato”."
        : "Pedido rejeitado.");
    } catch (err: any) {
      tratarErro(err);
    }
  };

  const executarContrato = async () => {
    if (!selected) return;
    setErro(""); setErros({}); setAviso("");

    let corpo: any = {};
    if (isBanco && comCredito) {
      const nEntrada = entrada.trim() ? parseMoeda(entrada) : 0;
      const nFinanciado = valorFinanciado.trim() ? parseMoeda(valorFinanciado) : null;
      const nJuros = parseMoeda(juros);
      const nParcelas = parseInt(parcelas);
      const e: Erros = {};
      if (Number.isNaN(nEntrada) || nEntrada < 0) e.valorEntrada = "Entrada inválida.";
      if (!nParcelas || nParcelas < 1 || nParcelas > 120) e.numeroParcelas = "Informe de 1 a 120 parcelas.";
      if (Number.isNaN(nJuros) || nJuros < 0) e.taxaJuros = "Informe a taxa de juros anual.";
      if (nFinanciado !== null && (Number.isNaN(nFinanciado) || nFinanciado <= 0)) e.valorFinanciado = "Valor inválido.";
      if (Object.keys(e).length > 0) {
        setErros(e);
        setErro("Corrija os campos destacados.");
        return;
      }
      corpo = { valorEntrada: nEntrada, numeroParcelas: nParcelas, taxaJuros: nJuros, valorFinanciado: nFinanciado };
    }

    try {
      await api.gerarContrato(selected.id, corpo);
      concluirAposAcao(isBanco && comCredito
        ? "Contrato com financiamento executado. O pedido está concluído."
        : "Contrato executado. O pedido está concluído.");
    } catch (err: any) {
      tratarErro(err);
    }
  };

  return (
    <div className="page-wrap max-w-none">
      <Header
        eyebrow="Operações de Crédito"
        title="Análise financeira"
        description="Analise as solicitações, emita o parecer e execute o contrato dos pedidos aprovados."
      />

      <div className="mt-6 flex gap-1 rounded-xl bg-slate-200/70 p-1 sm:w-fit">
        {([
          ["analise", `Em análise (${emAnalise.length})`],
          ["contrato", `Aguardando contrato (${aguardandoContrato.length})`],
          ["historico", `Histórico (${totalEncerrados})`],
        ] as const).map(([id, rotulo]) => (
          <button
            key={id}
            onClick={() => { setAba(id); selecionar(null); }}
            className={`rounded-lg px-4 py-2 text-sm font-semibold transition ${
              aba === id ? "bg-white text-slate-950 shadow-sm" : "text-slate-500 hover:text-slate-800"
            }`}
          >
            {rotulo}
          </button>
        ))}
      </div>

      {historico && (
        <div className="mt-4 flex flex-wrap items-center gap-3 text-sm">
          <div className="flex gap-1 rounded-xl bg-slate-200/70 p-1">
            {["TODOS", ...STATUS_HISTORICO].map((s) => (
              <button
                key={s}
                onClick={() => setFiltroStatus(s)}
                className={`rounded-lg px-3 py-1.5 text-xs font-semibold transition ${
                  filtroStatus === s ? "bg-white text-slate-950 shadow-sm" : "text-slate-500 hover:text-slate-800"
                }`}
              >
                {s === "TODOS" ? "Todos" : ROTULO_STATUS[s]}
              </button>
            ))}
          </div>
          <label className="flex cursor-pointer items-center gap-2 font-medium text-slate-600">
            <input type="checkbox" checked={somenteMinhas} onChange={(e) => setSomenteMinhas(e.target.checked)} className="h-4 w-4 accent-blue-600" />
            Somente as minhas análises
          </label>
        </div>
      )}

      <section className="card overflow-hidden mt-4">
        <div className="overflow-x-auto">
          <table className="w-full min-w-215 text-left">
            <thead>
              <tr className="border-b border-slate-200 bg-slate-50 text-[11px] font-bold uppercase tracking-wider text-slate-400">
                <th className="px-6 py-4">Pedido / Cliente</th>
                <th>Veículo</th>
                <th>Período</th>
                <th>Valor total</th>
                <th>Renda do cliente</th>
                <th>Status</th>
                {historico && <th>Analisado por</th>}
                <th />
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {lista.length === 0 && (
                <tr>
                  <td colSpan={historico ? 8 : 7} className="p-6 text-center text-slate-500">
                    {aba === "analise"
                      ? "Nenhum pedido pendente de análise."
                      : aba === "contrato"
                        ? "Nenhum pedido aprovado aguardando contrato."
                        : "Nenhuma análise encerrada com esses filtros."}
                  </td>
                </tr>
              )}
              {lista.map((row) => (
                <tr
                  key={row.id}
                  onClick={() => selecionar(row)}
                  className={`cursor-pointer text-sm transition hover:bg-blue-50/50 ${
                    selected?.id === row.id ? "bg-blue-50/70" : "bg-white"
                  }`}
                >
                  <td className="px-6 py-4 font-semibold text-slate-900">
                    REQ-{row.id}
                    <span className="block text-xs font-normal text-slate-500">{row.cliente?.nome || row.cliente?.login}</span>
                  </td>
                  <td className="font-medium text-slate-700">
                    {row.automovel ? `${row.automovel.marca} ${row.automovel.modelo}` : "N/A"}
                    <span className="block text-xs font-normal text-slate-400">{formatarPlaca(row.automovel?.placa)}</span>
                  </td>
                  <td className="text-slate-600">{formatarData(row.dataInicio)} a {formatarData(row.dataFim)}</td>
                  <td className="font-semibold text-slate-900">{formatarMoeda(row.valorTotal)}</td>
                  <td className="text-slate-600">{formatarMoeda(rendaTotal(row))}</td>
                  <td><StatusBadge status={row.status} /></td>
                  {historico && (
                    <td className="text-slate-600">
                      {row.agenteAvaliador ? (row.agenteAvaliador.razaoSocial || row.agenteAvaliador.login) : "—"}
                    </td>
                  )}
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
                <h2 className="mt-1 text-xl font-semibold">
                  {selected.status === "PENDENTE"
                    ? "Analisar solicitação"
                    : selected.status === "APROVADO"
                      ? "Executar contrato"
                      : "Detalhes da análise"}
                </h2>
              </div>
              <button onClick={() => selecionar(null)} className="header-button"><Icon name="close" /></button>
            </div>

            <div className="space-y-7 p-6">
              {mensagemSucesso && (
                <div className="flex items-center gap-3 rounded-xl bg-emerald-50 p-4 text-sm font-semibold text-emerald-800">
                  <Icon name="check" className="h-5 w-5" /> {mensagemSucesso}
                </div>
              )}
              {erro && <div className="rounded-xl bg-red-50 p-4 text-sm font-medium text-red-800">{erro}</div>}
              {aviso && <div className="rounded-xl bg-emerald-50 p-4 text-sm font-medium text-emerald-800">{aviso}</div>}

              {/* Resumo do pedido */}
              <div className="rounded-2xl border border-slate-200 bg-slate-50/70 p-5">
                <dl className="grid grid-cols-2 gap-x-4 gap-y-3 text-sm">
                  <div><dt className="text-xs text-slate-400">Cliente</dt><dd className="font-semibold text-slate-800">{selected.cliente?.nome || selected.cliente?.login}</dd></div>
                  <div><dt className="text-xs text-slate-400">Profissão</dt><dd className="font-semibold text-slate-800">{selected.cliente?.profissao || "—"}</dd></div>
                  <div><dt className="text-xs text-slate-400">Veículo</dt><dd className="font-semibold text-slate-800">{selected.automovel ? `${selected.automovel.marca} ${selected.automovel.modelo}` : "—"}</dd></div>
                  <div><dt className="text-xs text-slate-400">Período</dt><dd className="font-semibold text-slate-800">{formatarData(selected.dataInicio)} a {formatarData(selected.dataFim)}</dd></div>
                  <div><dt className="text-xs text-slate-400">Valor total do aluguel</dt><dd className="font-semibold text-slate-950">{formatarMoeda(selected.valorTotal)}</dd></div>
                  <div><dt className="text-xs text-slate-400">Renda mensal total</dt><dd className="font-semibold text-slate-950">{formatarMoeda(rendaTotal(selected))}</dd></div>
                </dl>
              </div>

              <div>
                <h3 className="panel-title">Acompanhamento</h3>
                <div className="mt-4">
                  <OrderTimeline status={selected.status} historico={selected.historico} />
                </div>
              </div>

              {/* ---- Análise encerrada (somente leitura) ---- */}
              {STATUS_HISTORICO.includes(selected.status) && (
                <div className="rounded-2xl border border-slate-200 p-5">
                  <h3 className="panel-title">Resultado</h3>
                  <dl className="mt-4 grid grid-cols-2 gap-x-4 gap-y-3 text-sm">
                    <div>
                      <dt className="text-xs text-slate-400">Situação</dt>
                      <dd className="mt-1"><StatusBadge status={selected.status} /></dd>
                    </div>
                    <div>
                      <dt className="text-xs text-slate-400">Analisado por</dt>
                      <dd className="font-semibold text-slate-800">
                        {selected.agenteAvaliador
                          ? `${selected.agenteAvaliador.razaoSocial || selected.agenteAvaliador.login} (${selected.agenteAvaliador.tipo === "BANCO" ? "Banco" : "Empresa"})`
                          : "Não chegou a ser analisado"}
                      </dd>
                    </div>
                    {selected.parecerFinanceiro && (
                      <div>
                        <dt className="text-xs text-slate-400">Parecer</dt>
                        <dd className="font-semibold text-slate-800">{selected.parecerFinanceiro}</dd>
                      </div>
                    )}
                    {selected.observacao && (
                      <div className="col-span-2">
                        <dt className="text-xs text-slate-400">Observação</dt>
                        <dd className="font-semibold text-slate-800">{selected.observacao}</dd>
                      </div>
                    )}
                  </dl>
                  {selected.contrato?.valorFinanciado != null && (
                    <div className="mt-4 rounded-xl bg-blue-50 p-4 text-sm text-blue-900">
                      <p className="font-semibold">Financiamento — {selected.contrato.banco?.razaoSocial || "Banco"}</p>
                      <p className="mt-1">
                        {formatarMoeda(selected.contrato.valorFinanciado)} em {selected.contrato.numeroParcelas ?? "—"}x de{" "}
                        <b>{formatarMoeda(selected.contrato.valorParcela)}</b> ({selected.contrato.taxaJuros}% a.a.)
                      </p>
                    </div>
                  )}
                  {selected.status === "CONCLUIDO" && selected.contrato && selected.contrato.valorFinanciado == null && (
                    <p className="mt-4 text-sm text-slate-600">Contrato de aluguel executado sem financiamento.</p>
                  )}
                </div>
              )}

              {/* ---- Fase de análise (pedido pendente) ---- */}
              {selected.status === "PENDENTE" && (
                <>
                  <div>
                    <h3 className="panel-title">Observação do pedido</h3>
                    <p className="mt-1 text-xs text-slate-500">Veículo e período são definidos pelo cliente; o agente só registra observações.</p>
                    <div className="mt-4 grid grid-cols-1 gap-4">
                      <TextInput label="Observação" value={observacao} onChange={e => setObservacao(e.target.value)} error={erros.observacao} />
                    </div>
                    <button onClick={salvarAlteracoes} className="button-secondary mt-4">Salvar observação</button>
                  </div>

                  <div>
                    <h3 className="panel-title">Parecer financeiro</h3>
                    <div className="mt-4 grid grid-cols-2 gap-3">
                      <button onClick={() => setDecision("approve")} className={`decision ${decision === "approve" ? "approve" : ""}`}>
                        <span className="decision-icon"><Icon name="check" /></span>
                        <span><b>Aprovar</b><small>Parecer positivo</small></span>
                      </button>
                      <button onClick={() => setDecision("reject")} className={`decision ${decision === "reject" ? "reject" : ""}`}>
                        <span className="decision-icon"><Icon name="close" /></span>
                        <span><b>Rejeitar</b><small>Parecer negativo</small></span>
                      </button>
                    </div>
                    <p className="mt-3 text-xs text-slate-500">
                      Aprovar não gera o contrato: o pedido segue para “Aguardando contrato”, onde o contrato é executado.
                    </p>
                  </div>
                </>
              )}

              {/* ---- Execução do contrato (pedido aprovado) ---- */}
              {selected.status === "APROVADO" && (
                <div className="rounded-2xl border border-blue-100 bg-blue-50/40 p-5">
                  <div className="mb-5 flex items-center gap-3">
                    <span className="grid h-9 w-9 place-items-center rounded-lg bg-blue-600 text-white"><Icon name="file" className="h-4 w-4" /></span>
                    <div>
                      <h3 className="font-semibold text-slate-950">Contrato</h3>
                      <p className="text-xs text-slate-500">
                        {isBanco ? "Com ou sem financiamento concedido pelo banco" : "Contrato de aluguel (somente bancos concedem crédito)"}
                      </p>
                    </div>
                  </div>

                  {isBanco && (
                    <label className="mb-4 flex cursor-pointer items-center gap-3 text-sm font-semibold text-slate-700">
                      <input type="checkbox" checked={comCredito} onChange={(e) => setComCredito(e.target.checked)} className="h-4 w-4 accent-blue-600" />
                      Conceder financiamento
                    </label>
                  )}

                  {isBanco && comCredito && (
                    <div className="space-y-4">
                      <div className="grid grid-cols-2 gap-4">
                        <TextInput label="Entrada (R$)" value={entrada} onChange={e => setEntrada(e.target.value)} inputMode="decimal" placeholder="0,00" error={erros.valorEntrada} />
                        <TextInput label="Nº de parcelas" value={parcelas} onChange={e => setParcelas(e.target.value.replace(/\D/g, ""))} inputMode="numeric" placeholder="12" error={erros.numeroParcelas} />
                        <TextInput label="Taxa de juros (% a.a.)" value={juros} onChange={e => setJuros(e.target.value)} inputMode="decimal" placeholder="Ex: 8,75" error={erros.taxaJuros} />
                        <TextInput
                          label="Valor financiado (R$)"
                          value={valorFinanciado}
                          onChange={e => setValorFinanciado(e.target.value)}
                          inputMode="decimal"
                          placeholder={selected.valorTotal != null ? "Total − entrada" : "Obrigatório"}
                          hint={selected.valorTotal != null ? "Em branco = valor total menos a entrada." : "Pedido sem valor total: informe o valor."}
                          error={erros.valorFinanciado}
                        />
                      </div>

                      {/* Simulação */}
                      <div className="rounded-xl border border-slate-200 bg-white p-4 text-sm">
                        <p className="text-xs font-bold uppercase tracking-wider text-blue-700">Simulação (tabela Price)</p>
                        {erroSimulacao && <p className="mt-2 text-xs font-medium text-red-600">{erroSimulacao}</p>}
                        {!simulacao && !erroSimulacao && (
                          <p className="mt-2 text-slate-500">Informe parcelas e juros para simular.</p>
                        )}
                        {simulacao && (
                          <>
                            <p className="mt-2 text-slate-600">
                              {formatarMoeda(simulacao.valorFinanciado)} em {simulacao.numeroParcelas}x de{" "}
                              <b className="text-lg text-slate-950">{formatarMoeda(simulacao.valorParcela)}</b>
                            </p>
                            <p className="mt-1 text-xs text-slate-500">
                              Juros de {simulacao.taxaJurosMensal}% ao mês · total pago {formatarMoeda(simulacao.totalPago)}
                            </p>
                            <div className={`mt-3 rounded-lg p-3 text-xs font-semibold ${simulacao.acimaDoLimite ? "bg-amber-50 text-amber-800" : "bg-emerald-50 text-emerald-800"}`}>
                              A parcela compromete {(simulacao.comprometimentoRenda * 100).toFixed(1).replace(".", ",")}% da renda de{" "}
                              {formatarMoeda(simulacao.rendaTotal)}.{" "}
                              {simulacao.acimaDoLimite
                                ? `Atenção: acima do limite de ${Math.round(simulacao.limiteComprometimento * 100)}%.`
                                : `Dentro do limite de ${Math.round(simulacao.limiteComprometimento * 100)}%.`}
                            </div>
                          </>
                        )}
                      </div>
                    </div>
                  )}
                </div>
              )}
            </div>

            <div className="sticky bottom-0 flex gap-3 border-t border-slate-200 bg-white p-6">
              {selected.status === "PENDENTE" && (
                <button onClick={enviarParecer} className={`flex flex-1 items-center justify-center gap-2 rounded-xl px-5 py-3 text-sm font-semibold text-white ${decision === "approve" ? "bg-blue-600 hover:bg-blue-700" : "bg-red-600 hover:bg-red-700"}`}>
                  Enviar parecer <Icon name="chevron" className="h-4 w-4" />
                </button>
              )}
              {selected.status === "APROVADO" && (
                <button onClick={executarContrato} className="flex flex-1 items-center justify-center gap-2 rounded-xl bg-blue-600 px-5 py-3 text-sm font-semibold text-white hover:bg-blue-700">
                  Executar contrato <Icon name="chevron" className="h-4 w-4" />
                </button>
              )}
            </div>
          </aside>
        </>
      )}
    </div>
  );
}
