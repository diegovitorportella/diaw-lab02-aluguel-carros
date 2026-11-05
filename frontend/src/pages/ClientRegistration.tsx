import { useState } from "react";
import type { FormEvent } from "react";
import Header from "../components/Header";
import Icon from "../components/Icon";
import TextInput from "../components/TextInput";
import { api } from "../services/api";

export default function ClientRegistration() {
  const [incomes, setIncomes] = useState([{ employer: "", salary: "" }]);
  const [saved, setSaved] = useState(false);
  const [error, setError] = useState("");

  const [nome, setNome] = useState("");
  const [cpf, setCpf] = useState("");
  const [login, setLogin] = useState("");
  const [senha, setSenha] = useState("");
  const [profissao, setProfissao] = useState("");

  const addIncome = () =>
    incomes.length < 3 && setIncomes([...incomes, { employer: "", salary: "" }]);
  const removeIncome = (index: number) =>
    setIncomes(incomes.filter((_, i) => i !== index));

  const updateIncome = (index: number, field: "employer" | "salary", value: string) => {
    const newIncomes = [...incomes];
    newIncomes[index][field] = value;
    setIncomes(newIncomes);
  };

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    setError("");
    setSaved(false);

    try {
      const rendimentosAuferidos = incomes.map(inc => parseFloat(inc.salary.replace(/[^0-9.-]+/g,"")) || 0);
      const entidadesEmpregadoras = incomes.map(inc => inc.employer);

      await api.criarCliente({
        nome, cpf, login, senha, profissao,
        rendimentosAuferidos,
        entidadesEmpregadoras
      });

      setSaved(true);
      window.scrollTo({ top: 0, behavior: "smooth" });
    } catch (err) {
      setError("Erro ao salvar perfil. Verifique se o login já existe e se há no máximo 3 rendimentos.");
      window.scrollTo({ top: 0, behavior: "smooth" });
    }
  };

  return (
    <div className="page-wrap">
      <Header
        eyebrow="Gestão de Clientes"
        title="Cadastrar novo cliente"
        description="Crie um perfil completo para locação e análise de crédito."
      />
      {saved && (
        <div className="mb-6 flex items-center gap-3 rounded-xl border border-emerald-200 bg-emerald-50 p-4 text-sm font-medium text-emerald-800">
          <span className="grid h-7 w-7 place-items-center rounded-full bg-emerald-500 text-white">
            <Icon name="check" className="h-4 w-4" />
          </span>
          Perfil do cliente salvo com sucesso.
        </div>
      )}
      {error && (
        <div className="mb-6 flex items-center gap-3 rounded-xl border border-red-200 bg-red-50 p-4 text-sm font-medium text-red-800">
          {error}
        </div>
      )}
      <form className="space-y-6" onSubmit={submit}>
        <section className="card p-6 md:p-8">
          <div className="mb-7 flex items-start gap-4">
            <span className="grid h-9 w-9 shrink-0 place-items-center rounded-xl bg-blue-50 text-xs font-bold text-blue-600">01</span>
            <div>
              <h2 className="text-lg font-semibold text-slate-950">Dados Pessoais e Acesso</h2>
            </div>
          </div>
          <div className="form-grid">
            <TextInput label="Nome completo" value={nome} onChange={e => setNome(e.target.value)} required wide />
            <TextInput label="CPF" value={cpf} onChange={e => setCpf(e.target.value)} required />
            <TextInput label="Profissão" value={profissao} onChange={e => setProfissao(e.target.value)} required />
            <TextInput label="Login (Usuário)" value={login} onChange={e => setLogin(e.target.value)} required />
            <TextInput label="Senha" type="password" value={senha} onChange={e => setSenha(e.target.value)} required />
          </div>
        </section>

        <section className="card p-6 md:p-8">
          <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
            <div className="mb-7 flex items-start gap-4">
              <span className="grid h-9 w-9 shrink-0 place-items-center rounded-xl bg-blue-50 text-xs font-bold text-blue-600">02</span>
              <div>
                <h2 className="text-lg font-semibold text-slate-950">Fontes de Renda</h2>
                <p className="mt-1 text-sm text-slate-500">Adicione até 3 empregadores ou fontes de renda.</p>
              </div>
            </div>
            <button type="button" onClick={addIncome} disabled={incomes.length === 3} className="button-secondary shrink-0 disabled:cursor-not-allowed disabled:opacity-40">
              <Icon name="plus" className="h-4 w-4" /> Adicionar fonte de renda
            </button>
          </div>
          <div className="mt-2 space-y-4">
            {incomes.map((income, i) => (
              <div key={i} className="rounded-2xl border border-slate-200 bg-slate-50/70 p-5">
                <div className="mb-4 flex items-center justify-between">
                  <p className="text-sm font-bold text-slate-800">Fonte de renda {i + 1}</p>
                  {incomes.length > 1 && (
                    <button type="button" onClick={() => removeIncome(i)} className="text-slate-400 hover:text-red-500">
                      <Icon name="trash" className="h-4 w-4" />
                    </button>
                  )}
                </div>
                <div className="grid gap-5 md:grid-cols-2">
                  <TextInput label="Empregador / Empresa" value={income.employer} onChange={e => updateIncome(i, "employer", e.target.value)} />
                  <TextInput label="Rendimento mensal (R$)" value={income.salary} onChange={e => updateIncome(i, "salary", e.target.value)} placeholder="8200.00" />
                </div>
              </div>
            ))}
          </div>
        </section>
        <div className="flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
          <button type="submit" className="button-primary justify-center">
            <Icon name="check" className="h-4 w-4" />
            Salvar perfil do cliente
          </button>
        </div>
      </form>
    </div>
  );
}