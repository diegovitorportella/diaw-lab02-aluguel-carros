import { useState } from "react";
import type { FormEvent } from "react";
import Header from "../components/Header";
import Icon from "../components/Icon";
import TextInput from "../components/TextInput";
import { useToast } from "../components/Toast";
import { api, ApiError } from "../services/api";
import { cpfValido, mascaraCpf, parseMoeda } from "../utils/formatters";

type Erros = Record<string, string>;

const MAX_FONTES = 3;
const SENHA_MINIMA = 8;

export default function ClientRegistration() {
  const [incomes, setIncomes] = useState([{ employer: "", salary: "" }]);
  const toast = useToast();
  const [saved, setSaved] = useState(false);
  const [error, setError] = useState("");
  const [erros, setErros] = useState<Erros>({});
  const [errosRenda, setErrosRenda] = useState<Record<number, Erros>>({});

  const [nome, setNome] = useState("");
  const [cpf, setCpf] = useState("");
  const [rg, setRg] = useState("");
  const [endereco, setEndereco] = useState("");
  const [login, setLogin] = useState("");
  const [senha, setSenha] = useState("");
  const [profissao, setProfissao] = useState("");

  const addIncome = () =>
    incomes.length < MAX_FONTES && setIncomes([...incomes, { employer: "", salary: "" }]);
  const removeIncome = (index: number) =>
    setIncomes(incomes.filter((_, i) => i !== index));

  const updateIncome = (index: number, field: "employer" | "salary", value: string) => {
    const newIncomes = [...incomes];
    newIncomes[index][field] = value;
    setIncomes(newIncomes);
  };

  /** Validação no navegador (o back-end valida de novo). */
  const validar = () => {
    const e: Erros = {};
    if (nome.trim().length < 3) e.nome = "Informe o nome completo (mínimo de 3 caracteres).";
    if (!cpfValido(cpf)) e.cpf = "CPF inválido.";
    if (rg.replace(/[^0-9A-Za-z]/g, "").length < 5) e.rg = "Informe o RG (mínimo de 5 caracteres).";
    if (!endereco.trim()) e.endereco = "O endereço é obrigatório.";
    if (!profissao.trim()) e.profissao = "A profissão é obrigatória.";
    if (!login.trim()) e.login = "O login é obrigatório.";
    if (senha.length < SENHA_MINIMA) e.senha = `A senha deve ter pelo menos ${SENHA_MINIMA} caracteres.`;

    const er: Record<number, Erros> = {};
    incomes.forEach((inc, i) => {
      const linha: Erros = {};
      if (!inc.employer.trim()) linha.employer = "Informe o empregador.";
      const valor = parseMoeda(inc.salary);
      if (Number.isNaN(valor) || valor <= 0) linha.salary = "Informe um rendimento maior que zero.";
      if (Object.keys(linha).length) er[i] = linha;
    });
    return { e, er };
  };

  const submit = async (ev: FormEvent) => {
    ev.preventDefault();
    setError("");
    setSaved(false);

    const { e, er } = validar();
    setErros(e);
    setErrosRenda(er);
    if (Object.keys(e).length > 0 || Object.keys(er).length > 0) {
      setError("Corrija os campos destacados.");
      toast.erro("Confira os campos do cadastro", "Há campos obrigatórios ou inválidos.");
      window.scrollTo({ top: 0, behavior: "smooth" });
      return;
    }

    try {
      await api.criarCliente({
        nome, cpf, rg, endereco, login, senha, profissao,
        rendimentosAuferidos: incomes.map((inc) => parseMoeda(inc.salary)),
        entidadesEmpregadoras: incomes.map((inc) => inc.employer.trim()),
      });

      setSaved(true);
      toast.sucesso("Cadastro realizado com sucesso", "Já é possível entrar com o login e a senha cadastrados.");
      setNome(""); setCpf(""); setRg(""); setEndereco(""); setLogin(""); setSenha(""); setProfissao("");
      setIncomes([{ employer: "", salary: "" }]);
      window.scrollTo({ top: 0, behavior: "smooth" });
    } catch (err: any) {
      if (err instanceof ApiError && Object.keys(err.campos).length > 0) setErros(err.campos);
      setError(err.message || "Erro ao salvar perfil.");
      toast.erro("Não foi possível salvar o cadastro", err.message);
      window.scrollTo({ top: 0, behavior: "smooth" });
    }
  };

  return (
    <div className="page-wrap">
      <Header
        title="Cadastrar novo cliente"
        description="Crie um perfil completo para locação e análise de crédito."
      />
      {saved && (
        <div className="mb-6 flex items-center gap-3 rounded-xl border border-emerald-200 bg-emerald-50 p-4 text-sm font-medium text-emerald-800">
          <span className="grid h-7 w-7 place-items-center rounded-full bg-emerald-500 text-white">
            <Icon name="check" className="h-4 w-4" />
          </span>
          Perfil do cliente salvo com sucesso. Já é possível entrar com o login cadastrado.
        </div>
      )}
      {error && (
        <div className="mb-6 flex items-center gap-3 rounded-xl border border-red-200 bg-red-50 p-4 text-sm font-medium text-red-800">
          {error}
        </div>
      )}
      <form className="space-y-6" onSubmit={submit} noValidate>
        <section className="card p-6 md:p-8">
          <div className="mb-7 flex items-start gap-4">
            <div>
              <h2 className="text-lg font-semibold text-slate-950">Dados pessoais e acesso</h2>
            </div>
          </div>
          <div className="form-grid">
            <TextInput label="Nome completo" value={nome} onChange={e => setNome(e.target.value)} wide error={erros.nome} />
            <TextInput label="CPF" value={cpf} onChange={e => setCpf(mascaraCpf(e.target.value))} inputMode="numeric" placeholder="000.000.000-00" maxLength={14} error={erros.cpf} />
            <TextInput label="RG" value={rg} onChange={e => setRg(e.target.value)} placeholder="Ex: MG-12.345.678" maxLength={20} error={erros.rg} />
            <TextInput label="Endereço" value={endereco} onChange={e => setEndereco(e.target.value)} placeholder="Rua, número, bairro, cidade" wide error={erros.endereco} />
            <TextInput label="Profissão" value={profissao} onChange={e => setProfissao(e.target.value)} error={erros.profissao} />
            <TextInput label="Login (Usuário)" value={login} onChange={e => setLogin(e.target.value)} error={erros.login} />
            <TextInput label="Senha" type="password" value={senha} onChange={e => setSenha(e.target.value)} hint={`Mínimo de ${SENHA_MINIMA} caracteres.`} error={erros.senha} />
          </div>
        </section>

        <section className="card p-6 md:p-8">
          <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
            <div className="mb-7 flex items-start gap-4">
              <div>
                <h2 className="text-lg font-semibold text-slate-950">Fontes de renda</h2>
                <p className="mt-1 text-sm text-slate-500">Informe de 1 a {MAX_FONTES} empregadores ou fontes de renda, cada um com o rendimento mensal.</p>
              </div>
            </div>
            <button type="button" onClick={addIncome} disabled={incomes.length === MAX_FONTES} className="button-secondary shrink-0 disabled:cursor-not-allowed disabled:opacity-40">
              <Icon name="plus" className="h-4 w-4" /> Adicionar fonte de renda
            </button>
          </div>
          {(erros.rendimentosAuferidos || erros.entidadesEmpregadoras) && (
            <p className="mb-4 rounded-xl bg-red-50 p-3 text-sm font-medium text-red-700">
              {erros.rendimentosAuferidos || erros.entidadesEmpregadoras}
            </p>
          )}
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
                  <TextInput label="Empregador / Empresa" value={income.employer} onChange={e => updateIncome(i, "employer", e.target.value)} error={errosRenda[i]?.employer} />
                  <TextInput label="Rendimento mensal (R$)" value={income.salary} onChange={e => updateIncome(i, "salary", e.target.value)} inputMode="decimal" placeholder="Ex: 8.200,00" error={errosRenda[i]?.salary} />
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
