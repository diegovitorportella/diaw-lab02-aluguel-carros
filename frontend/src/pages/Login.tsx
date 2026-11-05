import { useState } from "react";
import type { FormEvent } from "react";
import Logo from "../components/Logo";
import Field from "../components/Field";
import Icon from "../components/Icon";
import { api } from "../services/api";

export default function Login({ onLogin }: { onLogin: () => void }) {
  const [visible, setVisible] = useState(false);
  const [username, setUsername] = useState("marina.costa");
  const [password, setPassword] = useState("password123");
  const [error, setError] = useState("");

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    setError("");
    try {
      await api.login({ login: username, senha: password });
      onLogin(); // Se o login der certo, avança para o dashboard
    } catch (err) {
      setError("Credenciais inválidas. Verifique e tente novamente.");
    }
  };

  return (
    <main className="grid min-h-screen bg-white lg:grid-cols-[1.08fr_.92fr]">
      <section className="relative hidden overflow-hidden bg-slate-950 p-12 lg:flex lg:flex-col">
        <img
          src="https://images.unsplash.com/photo-1712214910040-1f7f5925b16a?crop=entropy&cs=tinysrgb&fit=crop&fm=jpg&q=85&w=900"
          alt="Red premium vehicle in a dark showroom"
          className="absolute inset-0 h-full w-full object-cover opacity-55"
        />
        <div className="absolute inset-0 bg-gradient-to-br from-slate-950/95 via-blue-950/55 to-slate-950/80" />
        <div className="relative z-10">
          <Logo light />
        </div>
        <div className="relative z-10 mt-auto max-w-xl pb-10">
          <span className="mb-6 inline-flex rounded-full border border-white/20 bg-white/10 px-4 py-2 text-xs font-semibold uppercase tracking-[.18em] text-blue-100 backdrop-blur">
            Rental & financing, reimagined
          </span>
          <h1 className="text-5xl font-semibold leading-[1.08] tracking-[-.04em] text-white">
            Move forward with complete confidence.
          </h1>
          <p className="mt-6 max-w-lg text-lg leading-8 text-slate-300">
            One secure platform to manage customers, vehicle requests, financial reviews, and credit contracts.
          </p>
        </div>
      </section>
      <section className="flex items-center justify-center px-6 py-12">
        <div className="w-full max-w-md">
          <div className="mb-12 lg:hidden">
            <Logo />
          </div>
          <p className="text-sm font-semibold text-blue-600">WELCOME BACK</p>
          <h2 className="mt-3 text-4xl font-semibold tracking-[-.035em] text-slate-950">
            Sign in to your account
          </h2>
          <p className="mt-3 text-base text-slate-500">
            Enter your credentials to access the platform.
          </p>
          
          {error && (
            <div className="mt-4 rounded-xl bg-red-50 p-4 text-sm font-medium text-red-800">
              {error}
            </div>
          )}

          <form className="mt-8 space-y-5" onSubmit={submit}>
            <Field label="Username" icon="user">
              <input
                required
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                placeholder="seu.login"
                className="w-full bg-transparent outline-none text-slate-900"
              />
            </Field>
            <Field
              label="Password"
              icon="lock"
              action={
                <button
                  type="button"
                  onClick={() => setVisible(!visible)}
                  aria-label="Toggle password visibility"
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
            <button className="mt-2 w-full rounded-xl bg-blue-600 px-5 py-3.5 font-semibold text-white shadow-lg shadow-blue-600/20 transition hover:bg-blue-700">
              Sign in securely
            </button>
          </form>
        </div>
      </section>
    </main>
  );
}