import Header from "../components/Header";
import Icon from "../components/Icon";

const TIPOS: Record<string, string> = {
  CLIENTE: "Cliente",
  BANCO: "Banco (agente)",
  EMPRESA: "Empresa (agente)",
};

export default function Settings({ user, onLogout }: { user?: any; onLogout: () => void }) {
  const nome = user?.nome || user?.razaoSocial || user?.login || "Usuário";

  const dados: [string, string | undefined][] = [
    ["Nome", nome],
    ["Usuário (login)", user?.login],
    ["Tipo de conta", TIPOS[user?.tipo] ?? user?.tipo],
    ...(user?.tipo === "CLIENTE"
      ? ([["CPF", user?.cpf], ["Profissão", user?.profissao]] as [string, string | undefined][])
      : ([["CNPJ", user?.cnpj]] as [string, string | undefined][])),
  ];

  const sair = () => {
    if (confirm("Deseja realmente sair da conta?")) onLogout();
  };

  return (
    <div className="page-wrap">
      <Header
        eyebrow="Conta"
        title="Configurações"
        description="Consulte os dados da sua conta e gerencie o acesso ao sistema."
      />

      <section className="card p-6 md:p-8">
        <h2 className="text-lg font-semibold text-slate-950">Dados da conta</h2>
        <dl className="mt-5 grid gap-5 sm:grid-cols-2">
          {dados.map(([rotulo, valor]) => (
            <div key={rotulo}>
              <dt className="text-xs font-bold uppercase tracking-wider text-slate-400">{rotulo}</dt>
              <dd className="mt-1 text-sm font-medium text-slate-800">{valor || "—"}</dd>
            </div>
          ))}
        </dl>
      </section>

      <section className="card mt-6 p-6 md:p-8">
        <h2 className="text-lg font-semibold text-slate-950">Sessão</h2>
        <p className="mt-1 text-sm text-slate-500">Encerre sua sessão neste dispositivo.</p>
        <button onClick={sair} className="button-secondary mt-5 text-red-600">
          <Icon name="close" className="h-4 w-4" /> Sair da conta
        </button>
      </section>
    </div>
  );
}
