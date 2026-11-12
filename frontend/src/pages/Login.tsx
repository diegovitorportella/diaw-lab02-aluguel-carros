import { useState } from "react";
import type { FormEvent } from "react";
import Logo from "../components/Logo";
import Field from "../components/Field";
import Icon from "../components/Icon";
import Placa from "../components/Placa";
import { api } from "../services/api";

// Acesso rápido da versão publicada (VITE_MODO_DEMO=true). As contas e a senha pública são criadas
// no back-end quando DEMO_ENABLED=true.
const MODO_DEMO = import.meta.env.VITE_MODO_DEMO === "true";
const SENHA_DEMO = "demo12345";
const CONTAS_DEMO = [
  { login: "demo.cliente", titulo: "Cliente", descricao: "Pede, modifica e cancela aluguéis" },
  { login: "demo.banco", titulo: "Banco", descricao: "Analisa e concede financiamento" },
  { login: "demo.empresa", titulo: "Empresa", descricao: "Analisa e executa contratos" },
];

export default function Login({ onLogin, onRegister }: { onLogin: (user: any) => void; onRegister: () => void }) {
  const [visible, setVisible] = useState(false);
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [carregando, setCarregando] = useState(false);

  const entrar = async (login: string, senha: string) => {
    if (carregando) return;
    setError("");
    setCarregando(true);
    try {
      const userData = await api.login({ login, senha });
      onLogin(userData); // Passa os dados reais para o App
    } catch (err) {
      setError(
        err instanceof TypeError
          ? "Não foi possível conectar ao servidor. Aguarde alguns segundos e tente de novo."
          : "Credenciais inválidas. Verifique e tente novamente.",
      );
    } finally {
      setCarregando(false);
    }
  };

  const submit = (event: FormEvent) => {
    event.preventDefault();
    entrar(username, password);
  };

  return (
    <main className="grid min-h-screen bg-white lg:grid-cols-[1.08fr_.92fr]">
      <section className="relative hidden overflow-hidden bg-slate-950 p-12 lg:flex lg:flex-col">
        <Logo light />

        {/* Assinatura da marca: a placa chegando, com o rastro âmbar da logo */}
        <div className="my-auto flex items-center py-10" aria-hidden="true">
          <div className="mr-5 flex flex-col items-end gap-3">
            {["11rem", "7.5rem", "13.5rem"].map((largura, i) => (
              <span
                key={largura}
                className="pista-trecho block h-[7px] rounded-full bg-amber-500"
                style={{ width: largura, transformOrigin: "right center", animationDelay: `${i * 90}ms` }}
              />
            ))}
          </div>
          <Placa placa="DRV1J25" tamanho="xl" />
        </div>

        <div className="max-w-xl pb-6">
          <h1 className="text-5xl font-bold leading-[1.08] tracking-[-.012em] text-white">
            Do pedido ao contrato, no mesmo lugar.
          </h1>
          <p className="mt-5 max-w-md text-lg leading-8 text-slate-300">
            O cliente pede o veículo, o banco ou a empresa analisa e assina. Cada etapa fica registrada e visível para quem acompanha.
          </p>
        </div>
      </section>
      <section className="flex items-center justify-center px-6 py-12">
        <div className="w-full max-w-md">
          <div className="mb-12 lg:hidden">
            <Logo />
          </div>
          <h2 className="text-4xl font-bold tracking-[-.03em] text-slate-950">
            Entrar no DriveJá
          </h2>
          <p className="mt-3 text-base text-slate-600">
            Use o login e a senha da sua conta.
          </p>
          
          {error && (
            <div className="mt-4 rounded-xl bg-red-50 p-4 text-sm font-medium text-red-800">
              {error}
            </div>
          )}

          <form className="mt-8 space-y-5" onSubmit={submit}>
            <Field label="Usuário" icon="user">
              <input
                required
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                placeholder="seu.login"
                className="w-full bg-transparent outline-none text-slate-900"
              />
            </Field>
            <Field
              label="Senha"
              icon="lock"
              action={
                <button
                  type="button"
                  onClick={() => setVisible(!visible)}
                  aria-label="Alternar visibilidade da senha"
                >
                  <Icon name="eye" />
                </button>
              }
            >
              <input
                required
                type={visible ? "text" : "password"}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••"
                className="w-full bg-transparent outline-none text-slate-900"
              />
            </Field>
            <button
              disabled={carregando}
              className="mt-2 w-full rounded-lg bg-blue-600 px-5 py-3.5 font-bold text-white transition hover:bg-blue-700 disabled:cursor-wait disabled:opacity-60"
            >
              {carregando ? "Conectando..." : "Entrar"}
            </button>
            {carregando && (
              <p className="text-center text-xs text-slate-500">
                O servidor de demonstração pode levar até 1 minuto para acordar na primeira vez.
              </p>
            )}
          </form>
          <p className="mt-6 text-center text-sm text-slate-500">
            Ainda não tem conta?{" "}
            <button type="button" onClick={onRegister} className="font-semibold text-blue-600 hover:text-blue-700">
              Cadastre-se
            </button>
          </p>

          {MODO_DEMO && (
            <div className="mt-8 rounded-lg border border-slate-200 bg-slate-50 p-5">
              <h3 className="text-base font-bold text-slate-950">Conhecer o sistema sem cadastro</h3>
              <p className="mt-1 text-sm text-slate-600">Entre com uma conta de demonstração e explore à vontade.</p>
              <div className="mt-4 grid gap-2 sm:grid-cols-3">
                {CONTAS_DEMO.map((conta) => (
                  <button
                    key={conta.login}
                    type="button"
                    disabled={carregando}
                    onClick={() => entrar(conta.login, SENHA_DEMO)}
                    className="rounded-lg border border-slate-300 bg-white px-3 py-2.5 text-left transition hover:border-blue-600 disabled:cursor-wait disabled:opacity-60"
                  >
                    <span className="block text-sm font-bold text-slate-900">{conta.titulo}</span>
                    <span className="block text-xs text-slate-500">{conta.descricao}</span>
                  </button>
                ))}
              </div>
            </div>
          )}
        </div>
      </section>
    </main>
  );
}