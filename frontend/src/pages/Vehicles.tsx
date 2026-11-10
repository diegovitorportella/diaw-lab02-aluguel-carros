import { useState, useEffect } from "react";
import Header from "../components/Header";
import Icon from "../components/Icon";
import TextInput from "../components/TextInput";
import { api } from "../services/api";

export default function Vehicles({ user }: { user?: any }) {
  const [veiculos, setVeiculos] = useState<any[]>([]);
  const [isModalOpen, setIsModalOpen] = useState(false);

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

  const cadastrarVeiculo = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user) return alert("Você precisa estar logado para cadastrar um veículo.");

    try {
      await api.criarAutomovel({
        marca,
        modelo,
        ano: parseInt(ano),
        placa,
        matricula, // o proprietário é o usuário logado (definido pelo back-end)
      });
      setIsModalOpen(false);
      setMarca(""); setModelo(""); setAno(""); setPlaca(""); setMatricula("");
      carregarVeiculos();
    } catch (err) {
      alert("Erro ao cadastrar veículo.");
    }
  };

  return (
    <div className="page-wrap">
      <Header
        eyebrow="Frota"
        title="Gerenciar Veículos"
        description="Visualize e cadastre os automóveis disponíveis no sistema."
        action={
          <button onClick={() => setIsModalOpen(true)} className="button-primary">
            <Icon name="plus" className="h-4 w-4" /> Adicionar Veículo
          </button>
        }
      />

      <section className="grid gap-6 md:grid-cols-2 xl:grid-cols-4 mt-6">
        {veiculos.length === 0 && (
          <p className="text-slate-500 col-span-full">Nenhum veículo cadastrado no sistema.</p>
        )}
        {veiculos.map((carro) => (
          <div key={carro.id} className="card p-5">
            <div className="flex items-center justify-between mb-4">
              <span className="grid h-12 w-12 place-items-center rounded-xl bg-slate-100 text-slate-500">
                <Icon name="car" className="h-6 w-6" />
              </span>
              <span className="text-xs font-bold text-slate-400">ID: {carro.id}</span>
            </div>
            <h3 className="font-semibold text-lg text-slate-900">{carro.marca} {carro.modelo}</h3>
            <p className="text-sm text-slate-500 mt-1">Ano: {carro.ano} • Placa: {carro.placa}</p>
          </div>
        ))}
      </section>

      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/50 backdrop-blur-sm px-4">
          <div className="bg-white rounded-2xl shadow-xl w-full max-w-lg overflow-hidden">
            <div className="px-6 py-4 border-b border-slate-200 flex justify-between items-center">
              <h3 className="font-semibold text-lg text-slate-900">Novo Automóvel</h3>
              <button onClick={() => setIsModalOpen(false)} className="text-slate-400 hover:text-slate-600">
                <Icon name="close" />
              </button>
            </div>
            <form onSubmit={cadastrarVeiculo} className="p-6 grid gap-4 md:grid-cols-2">
              <TextInput label="Marca" value={marca} onChange={e => setMarca(e.target.value)} required placeholder="Ex: Hyundai" />
              <TextInput label="Modelo" value={modelo} onChange={e => setModelo(e.target.value)} required placeholder="Ex: Ioniq 6" />
              <TextInput label="Ano" type="number" value={ano} onChange={e => setAno(e.target.value)} required placeholder="Ex: 2025" />
              <TextInput label="Placa" value={placa} onChange={e => setPlaca(e.target.value)} required placeholder="ABC-1234" />
              <TextInput label="Matrícula/Chassi" value={matricula} onChange={e => setMatricula(e.target.value)} required placeholder="Nº de registro" wide />
              
              <div className="col-span-full mt-4 flex justify-end gap-3">
                <button type="button" onClick={() => setIsModalOpen(false)} className="button-secondary">Cancelar</button>
                <button type="submit" className="button-primary">Salvar Veículo</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}