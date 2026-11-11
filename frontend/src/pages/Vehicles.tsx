import { useState, useEffect } from "react";
import Header from "../components/Header";
import Icon from "../components/Icon";
import Placa from "../components/Placa";
import TextInput from "../components/TextInput";
import { useToast } from "../components/Toast";
import { api, ApiError } from "../services/api";
import { formatarMoeda, formatarPlaca, normalizarPlaca, parseMoeda, placaValida } from "../utils/formatters";

type Erros = Record<string, string>;

export default function Vehicles({ user }: { user?: any }) {
  const [veiculos, setVeiculos] = useState<any[]>([]);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editando, setEditando] = useState<any | null>(null);
  const [erro, setErro] = useState("");
  const [erros, setErros] = useState<Erros>({});
  const [enviando, setEnviando] = useState(false);
  const toast = useToast();

  // Estados do formulário
  const [marca, setMarca] = useState("");
  const [modelo, setModelo] = useState("");
  const [ano, setAno] = useState("");
  const [placa, setPlaca] = useState("");
  const [matricula, setMatricula] = useState("");
  const [valorDiaria, setValorDiaria] = useState("");

  useEffect(() => {
    carregarVeiculos();
  }, []);

  const carregarVeiculos = async () => {
    try {
      const dados = await api.listarAutomoveis();
      setVeiculos(dados);
    } catch (err) {
      console.error(err);
    }
  };

  const fecharModal = () => {
    setIsModalOpen(false);
    setEditando(null);
    setErro("");
    setErros({});
    setMarca(""); setModelo(""); setAno(""); setPlaca(""); setMatricula(""); setValorDiaria("");
  };

  const abrirCadastro = () => {
    fecharModal();
    setIsModalOpen(true);
  };

  const abrirEdicao = (carro: any) => {
    setEditando(carro);
    setMarca(carro.marca ?? "");
    setModelo(carro.modelo ?? "");
    setAno(String(carro.ano ?? ""));
    setPlaca(normalizarPlaca(carro.placa ?? ""));
    setMatricula(carro.matricula ?? "");
    setValorDiaria(carro.valorDiaria != null ? String(carro.valorDiaria).replace(".", ",") : "");
    setErro("");
    setErros({});
    setIsModalOpen(true);
  };

  /** Validação no navegador (o back-end valida de novo). */
  const validar = (): Erros => {
    const e: Erros = {};
    const anoMax = new Date().getFullYear() + 1;
    if (!marca.trim()) e.marca = "A marca é obrigatória.";
    if (!modelo.trim()) e.modelo = "O modelo é obrigatório.";
    const anoNum = parseInt(ano);
    if (!ano || Number.isNaN(anoNum) || anoNum < 1900 || anoNum > anoMax) e.ano = `Informe um ano entre 1900 e ${anoMax}.`;
    if (!placaValida(placa)) e.placa = "Placa inválida. Use ABC-1234 ou ABC1D23 (Mercosul).";
    if (!matricula.trim()) e.matricula = "A matrícula é obrigatória.";
    const diaria = parseMoeda(valorDiaria);
    if (Number.isNaN(diaria) || diaria <= 0) e.valorDiaria = "Informe um valor de diária maior que zero.";
    return e;
  };

  const salvarVeiculo = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user || enviando) return;
    setErro("");
    const encontrados = validar();
    setErros(encontrados);
    if (Object.keys(encontrados).length > 0) {
      toast.erro("Confira os campos do veículo", "Há campos obrigatórios ou inválidos.");
      return;
    }

    // o proprietário é o usuário logado (definido pelo back-end a partir do token)
    const dados = { marca, modelo, ano: parseInt(ano), placa, matricula, valorDiaria: parseMoeda(valorDiaria) };
    setEnviando(true);
    try {
      if (editando) {
        await api.atualizarAutomovel(editando.id, dados);
        toast.sucesso("Veículo atualizado", `${marca} ${modelo} foi salvo com as novas informações.`);
      } else {
        await api.criarAutomovel(dados);
        toast.sucesso("Veículo cadastrado", `${marca} ${modelo} já está disponível para aluguel.`);
      }
      fecharModal();
      carregarVeiculos();
    } catch (err: any) {
      if (err instanceof ApiError && Object.keys(err.campos).length > 0) setErros(err.campos);
      setErro(err.message);
      toast.erro("Não foi possível salvar o veículo", err.message);
    } finally {
      setEnviando(false);
    }
  };

  const excluirVeiculo = async (carro: any) => {
    if (!confirm(`Excluir o veículo ${carro.marca} ${carro.modelo} (${formatarPlaca(carro.placa)})?`)) return;
    try {
      await api.excluirAutomovel(carro.id);
      toast.sucesso("Veículo excluído", `${carro.marca} ${carro.modelo} foi removido.`);
      carregarVeiculos();
    } catch (err: any) {
      toast.erro("Não foi possível excluir o veículo", err.message);
    }
  };

  const nomeProprietario = (carro: any) =>
    carro.proprietario?.nome || carro.proprietario?.razaoSocial || carro.proprietario?.login || "—";

  return (
    <div className="page-wrap">
      <Header
        title="Gerenciar Veículos"
        description="Visualize e cadastre os automóveis disponíveis no sistema. Você pode editar e excluir os veículos que cadastrou."
        action={
          <button onClick={abrirCadastro} className="button-primary">
            <Icon name="plus" className="h-4 w-4" /> Adicionar Veículo
          </button>
        }
      />

      <section className="grid gap-6 md:grid-cols-2 xl:grid-cols-4 mt-6">
        {veiculos.length === 0 && (
          <p className="text-slate-500 col-span-full">Nenhum veículo cadastrado no sistema.</p>
        )}
        {veiculos.map((carro) => {
          const meu = carro.proprietario?.id === user?.id;
          return (
            <div key={carro.id} className="card p-5">
              <Placa placa={carro.placa} tamanho="lg" />
              <h3 className="mt-4 text-lg font-bold tracking-[-.01em] text-slate-950">{carro.marca} {carro.modelo}</h3>
              <p className="mt-0.5 text-sm text-slate-500">Ano {carro.ano}</p>
              <p className="mt-3 font-display text-xl font-bold text-slate-950">
                {carro.valorDiaria != null ? (
                  <>{formatarMoeda(carro.valorDiaria)} <span className="font-sans text-sm font-medium text-slate-500">por dia</span></>
                ) : (
                  <span className="font-sans text-sm font-semibold text-amber-700">Diária não informada</span>
                )}
              </p>
              <p className="mt-2 text-xs text-slate-500">
                Proprietário: {meu ? "você" : nomeProprietario(carro)}
              </p>
              {meu && (
                <div className="mt-4 flex gap-3">
                  <button onClick={() => abrirEdicao(carro)} className="button-secondary flex-1">Editar</button>
                  <button onClick={() => excluirVeiculo(carro)} className="button-secondary flex-1 text-red-600">Excluir</button>
                </div>
              )}
            </div>
          );
        })}
      </section>

      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/50 backdrop-blur-sm px-4">
          <div className="max-h-[92vh] overflow-y-auto bg-white rounded-2xl shadow-xl w-full max-w-lg">
            <div className="px-6 py-4 border-b border-slate-200 flex justify-between items-center">
              <h3 className="font-semibold text-lg text-slate-900">
                {editando ? `Editar veículo #${editando.id}` : "Novo Automóvel"}
              </h3>
              <button onClick={fecharModal} className="text-slate-400 hover:text-slate-600">
                <Icon name="close" />
              </button>
            </div>
            <form onSubmit={salvarVeiculo} noValidate className="p-6 grid gap-4 md:grid-cols-2">
              {erro && Object.keys(erros).length === 0 && (
                <div className="col-span-full rounded-xl bg-red-50 p-3 text-sm font-medium text-red-800">{erro}</div>
              )}
              <TextInput label="Marca" value={marca} onChange={e => setMarca(e.target.value)} placeholder="Ex: Hyundai" error={erros.marca} />
              <TextInput label="Modelo" value={modelo} onChange={e => setModelo(e.target.value)} placeholder="Ex: Ioniq 6" error={erros.modelo} />
              <TextInput label="Ano" type="number" value={ano} onChange={e => setAno(e.target.value)} placeholder="Ex: 2025" error={erros.ano} />
              <TextInput
                label="Placa"
                value={placa}
                onChange={e => setPlaca(normalizarPlaca(e.target.value))}
                placeholder="ABC1234 ou ABC1D23"
                maxLength={8}
                error={erros.placa}
              />
              <TextInput
                label="Valor da diária (R$)"
                value={valorDiaria}
                onChange={e => setValorDiaria(e.target.value)}
                inputMode="decimal"
                placeholder="Ex: 180,00"
                error={erros.valorDiaria}
              />
              <TextInput label="Matrícula/Chassi" value={matricula} onChange={e => setMatricula(e.target.value)} placeholder="Nº de registro" error={erros.matricula} />

              <div className="col-span-full mt-4 flex justify-end gap-3">
                <button type="button" onClick={fecharModal} className="button-secondary">Cancelar</button>
                <button type="submit" disabled={enviando} className="button-primary disabled:cursor-wait disabled:opacity-60">
                  {enviando ? "Salvando..." : editando ? "Salvar alterações" : "Salvar Veículo"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
