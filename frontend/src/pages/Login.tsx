import { useState } from "react";
import type { FormEvent } from "react";
import Logo from "../components/Logo";
import Field from "../components/Field";
import Icon from "../components/Icon";
import Placa from "../components/Placa";
import { api } from "../services/api";

export default function Login({ onLogin, onRegister }: { onLogin: (user: any) => void; onRegister: () => void }) {
  const [visible, setVisible] = useState(false);
  const [username, setUsername] = useState("marina.costa");
  const [password, setPassword] = useState("password123");
  const [error, setError] = useState("");

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    setError("");
    try {
      const userData = await api.login({ login: username, senha: password });
      onLogin(userData); // Passa os dados reais para o App
    } catch (err) {
      setError("Credenciais inválidas. Verifique e tente novamente.");
    }
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
            <button className="mt-2 w-full rounded-lg bg-blue-600 px-5 py-3.5 font-bold text-white transition hover:bg-blue-700">
              Entrar
            </button>
          </form>
          <p className="mt-6 text-center text-sm text-slate-500">
            Ainda não tem conta?{" "}
            <button type="button" onClick={onRegister} className="font-semibold text-blue-600 hover:text-blue-700">
              Cadastre-se
            </button>
          </p>
        </div>
      </section>
    </main>
  );
}