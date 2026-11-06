import { useEffect, useState } from "react";
import type { ReactNode } from "react";
import Logo from "./Logo";
import Icon from "./Icon";
import { api } from "../services/api";

export type Screen = "login" | "clients" | "requests" | "review" | "vehicles" | "settings";

export default function Shell({
  screen,
  setScreen,
  user,
  children,
}: {
  screen: Screen;
  setScreen: (screen: Screen) => void;
  user?: any;
  children: ReactNode;
}) {
  const todos = [
    ["requests", "grid", "Meus pedidos"],
    ["clients", "users", "Clientes"],
    ["review", "file", "Análise financeira"],
  ] as const;
  // Clientes não acessam a análise financeira; agentes não têm "Meus pedidos".
  const nav = todos.filter(([id]) => (user?.tipo === "CLIENTE" ? id !== "review" : id !== "requests"));

  const [notificacoes, setNotificacoes] = useState<{ id: number; texto: string }[]>([]);
  const [bellOpen, setBellOpen] = useState(false);

  // Notificações derivadas dos pedidos: clientes veem decisões; agentes veem pedidos aguardando análise.
  useEffect(() => {
    if (!user) return;
    api.listarPedidos()
      .then((pedidos: any[]) => {
        const lista = user.tipo === "CLIENTE"
          ? pedidos
              .filter((p) => p.cliente?.id === user.id && p.status !== "PENDENTE")
              .map((p) => ({ id: p.id, texto: `Seu pedido REQ-${p.id} está ${p.status}.` }))
          : pedidos
              .filter((p) => p.status === "PENDENTE")
              .map((p) => ({ id: p.id, texto: `Pedido REQ-${p.id} aguardando análise.` }));
        setNotificacoes(lista.reverse().slice(0, 8));
      })
      .catch(console.error);
  }, [user, screen]);

  const nomeUsuario = user?.nome || user?.login || "Usuário";
  const iniciais = nomeUsuario.substring(0, 2).toUpperCase();
  const isCliente = user?.tipo === "CLIENTE";
  const tipoConta = isCliente ? "Conta Cliente" : user?.tipo === "BANCO" ? "Conta Banco" : "Conta Empresa";

  return (
    <div className="min-h-screen bg-[#f6f8fb] text-slate-900">
      <aside className="fixed inset-y-0 left-0 z-30 hidden w-64 flex-col border-r border-slate-200 bg-white px-4 py-6 lg:flex">
        <div className="px-3">
          <Logo />
        </div>
        <nav className="mt-10 space-y-1.5">
          <p className="px-3 pb-2 text-[11px] font-bold uppercase tracking-[.16em] text-slate-400">
            Espaço de Trabalho
          </p>
          {nav.map(([id, icon, label]) => (
            <button
              key={id}
              onClick={() => setScreen(id as Screen)}
              className={`nav-item ${screen === id ? "active" : ""}`}
            >
              <Icon name={icon} />
              {label}
            </button>
          ))}
          <p className="px-3 pb-2 pt-7 text-[11px] font-bold uppercase tracking-[.16em] text-slate-400">
            Gerenciar
          </p>
          <button 
            className={`nav-item ${screen === "vehicles" ? "active" : ""}`} 
            onClick={() => setScreen("vehicles")}
          >
            <Icon name="car" />
            Veículos
          </button>
          <button
            className={`nav-item ${screen === "settings" ? "active" : ""}`}
            onClick={() => setScreen("settings")}
          >
            <Icon name="settings" />
            Configurações
          </button>
        </nav>
        
        <div className="mt-auto rounded-2xl bg-slate-950 p-4 text-white">
          <div className="flex items-center gap-3">
            <span className="avatar bg-blue-500">{iniciais}</span>
            <div className="min-w-0 flex-1">
              <p className="truncate text-sm font-semibold">{nomeUsuario}</p>
              <p className="text-xs text-slate-400">{tipoConta}</p>
            </div>
          </div>
        </div>
      </aside>

      <header className="sticky top-0 z-20 flex h-20 items-center border-b border-slate-200 bg-white/90 px-5 backdrop-blur lg:ml-64 lg:px-8">
        <div className="lg:hidden">
          <Logo />
        </div>
        <div className="ml-auto flex items-center gap-2">
          <div className="relative">
            <button className="header-button relative" onClick={() => setBellOpen(!bellOpen)} aria-label="Notificações">
              <Icon name="bell" />
              {notificacoes.length > 0 && (
                <span className="absolute right-2.5 top-2.5 h-2 w-2 rounded-full border-2 border-white bg-blue-600" />
              )}
            </button>
            {bellOpen && (
              <>
                <button className="fixed inset-0 z-40 cursor-default" onClick={() => setBellOpen(false)} aria-label="Fechar notificações" />
                <div className="absolute right-0 z-50 mt-2 w-80 overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-xl">
                  <p className="border-b border-slate-100 px-4 py-3 text-sm font-semibold text-slate-900">Notificações</p>
                  {notificacoes.length === 0 ? (
                    <p className="px-4 py-6 text-center text-sm text-slate-500">Nenhuma notificação.</p>
                  ) : (
                    <ul className="max-h-80 divide-y divide-slate-100 overflow-y-auto">
                      {notificacoes.map((n) => (
                        <li key={n.id} className="px-4 py-3 text-sm text-slate-700">{n.texto}</li>
                      ))}
                    </ul>
                  )}
                </div>
              </>
            )}
          </div>
          <span className="mx-2 hidden h-8 w-px bg-slate-200 sm:block" />

          <div className="flex items-center gap-3 p-1.5">
            <span className="avatar bg-blue-100 text-blue-700">{iniciais}</span>
            <span className="hidden text-left md:block">
              <span className="block text-sm font-semibold">{nomeUsuario}</span>
            </span>
          </div>
        </div>
      </header>

      <div className="fixed inset-x-0 bottom-0 z-30 flex h-16 items-center justify-around border-t border-slate-200 bg-white lg:hidden">
        {[...nav, ["settings", "settings", "Configurações"] as const].map(([id, icon, label]) => (
          <button
            key={id}
            onClick={() => setScreen(id as Screen)}
            className={`flex min-w-20 flex-col items-center gap-1 text-[11px] font-semibold ${
              screen === id ? "text-blue-600" : "text-slate-400"
            }`}
          >
            <Icon name={icon} className="h-5 w-5" />
            {label}
          </button>
        ))}
      </div>
      <main className="pb-24 lg:ml-64 lg:pb-0">{children}</main>
    </div>
  );
}