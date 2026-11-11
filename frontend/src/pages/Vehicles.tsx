import { useState, useEffect } from "react";
import Header from "../components/Header";
import Icon from "../components/Icon";
import TextInput from "../components/TextInput";
import { api } from "../services/api";

export default function Vehicles({ user }: { user?: any }) {
  const [veiculos, setVeiculos] = useState<any[]>([]);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editando, setEditando] = useState<any | null>(null);
  const [erro, setErro] = useState("");

  // Estados do formulário
  const [marca, setMarca] = useState("");
  const [modelo, setModelo] = useState("");
  const [ano, setAno] = useState("");
  const [placa, setPlaca] = useState("");
  const [matricula, setMatricula] = useState("");

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
    setMarca(""); setModelo(""); setAno(""); setPlaca(""); setMatricula("");
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
    setPlaca(carro.placa ?? "");
    setMatricula(carro.matricula ?? "");
    setErro("");
    setIsModalOpen(true);
  };

  const salvarVeiculo = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user) return alert("Você precisa estar logado para cadastrar um veículo.");
    setErro("");

    // o proprietário é o usuário logado (definido pelo back-end a partir do token)
    const dados = { marca, modelo, ano: parseInt(ano), placa, matricula };
    try {
      if (editando) {
        await api.atualizarAutomovel(editando.id, dados);
      } else {
        await api.criarAutomovel(dados);
      }
      fecharModal();
      carregarVeiculos();
    } catch (err: any) {
      setErro(err.message);
    }
  };

  const excluirVeiculo = async (carro: any) => {
    if (!confirm(`Excluir o veículo ${carro.marca} ${carro.modelo} (${carro.placa})?`)) return;
    try {
      await api.excluirAutomovel(carro.id);
      carregarVeiculos();
    } catch (err: any) {
      alert(err.message);
    }
  };

  const nomeProprietario = (carro: any) =>
    carro.proprietario?.nome || carro.proprietario?.razaoSocial || carro.proprietario?.login || "—";

  return (
    <div className="page-wrap">
      <Header
        eyebrow="Frota"
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
              <div className="flex items-center justify-between mb-4">
                <span className="grid h-12 w-12 place-items-center rounded-xl bg-slate-100 text-slate-500">
                  <Icon name="car" className="h-6 w-6" />
                </span>
                <span className="text-xs font-bold text-slate-400">ID: {carro.id}</span>
              </div>
              <h3 className="font-semibold text-lg text-slate-900">{carro.marca} {carro.modelo}</h3>
              <p className="text-sm text-slate-500 mt-1">Ano: {carro.ano} • Placa: {carro.placa}</p>
              <p className="text-xs text-slate-400 mt-2">
                Proprietário: {meu ? "Você" : nomeProprietario(carro)}
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
          <div className="bg-white rounded-2xl shadow-xl w-full max-w-lg overflow-hidden">
            <div className="px-6 py-4 border-b border-slate-200 flex justify-between items-center">
              <h3 className="font-semibold text-lg text-slate-900">
                {editando ? `Editar veículo #${editando.id}` : "Novo Automóvel"}
              </h3>
              <button onClick={fecharModal} className="text-slate-400 hover:text-slate-600">
                <Icon name="close" />
              </button>
            </div>
            <form onSubmit={salvarVeiculo} className="p-6 grid gap-4 md:grid-cols-2">
              {erro && (
                <div className="col-span-full rounded-xl bg-red-50 p-3 text-sm font-medium text-red-800">{erro}</div>
              )}
              <TextInput label="Marca" value={marca} onChange={e => setMarca(e.target.value)} required placeholder="Ex: Hyundai" />
              <TextInput label="Modelo" value={modelo} onChange={e => setModelo(e.target.value)} required placeholder="Ex: Ioniq 6" />
              <TextInput label="Ano" type="number" value={ano} onChange={e => setAno(e.target.value)} required placeholder="Ex: 2025" />
              <TextInput label="Placa" value={placa} onChange={e => setPlaca(e.target.value)} required placeholder="ABC-1234" />
              <TextInput label="Matrícula/Chassi" value={matricula} onChange={e => setMatricula(e.target.value)} required placeholder="Nº de registro" wide />

              <div className="col-span-full mt-4 flex justify-end gap-3">
                <button type="button" onClick={fecharModal} className="button-secondary">Cancelar</button>
                <button type="submit" className="button-primary">{editando ? "Salvar alterações" : "Salvar Veículo"}</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
