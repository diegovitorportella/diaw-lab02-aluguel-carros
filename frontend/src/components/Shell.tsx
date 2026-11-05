import type { ReactNode } from "react";
import Logo from "./Logo";
import Icon from "./Icon";

export type Screen = "login" | "clients" | "requests" | "review" | "vehicles";

export default function Shell({
  screen,
  setScreen,
  user,
  onLogout,
  children,
}: {
  screen: Screen;
  setScreen: (screen: Screen) => void;
  user?: any;
  onLogout?: () => void;
  children: ReactNode;
}) {
  const nav = [
    ["requests", "grid", "Meus pedidos"],
    ["clients", "users", "Clientes"],
    ["review", "file", "Análise financeira"],
  ] as const;

  const nomeUsuario = user?.nome || user?.login || "Usuário";
  const iniciais = nomeUsuario.substring(0, 2).toUpperCase();
  const tipoConta = user?.profissao ? "Conta Cliente" : "Conta Agente";

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
          <button className="nav-item">
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
            <button onClick={onLogout} title="Sair" className="ml-auto text-slate-400 hover:text-white transition">
              <Icon name="close" className="h-4 w-4" />
            </button>
          </div>
        </div>
      </aside>

      <header className="sticky top-0 z-20 flex h-20 items-center border-b border-slate-200 bg-white/90 px-5 backdrop-blur lg:ml-64 lg:px-8">
        <div className="lg:hidden">
          <Logo />
        </div>
        <div className="ml-auto flex items-center gap-2">
          <button className="header-button relative">
            <Icon name="bell" />
            <span className="absolute right-2.5 top-2.5 h-2 w-2 rounded-full border-2 border-white bg-blue-600" />
          </button>
          <span className="mx-2 hidden h-8 w-px bg-slate-200 sm:block" />
          
          <button onClick={onLogout} title="Sair do sistema" className="flex items-center gap-3 rounded-xl p-1.5 hover:bg-slate-50 transition">
            <span className="avatar bg-blue-100 text-blue-700">{iniciais}</span>
            <span className="hidden text-left md:block">
              <span className="block text-sm font-semibold">{nomeUsuario}</span>
              <span className="block text-xs text-slate-500">{tipoConta}</span>
            </span>
            <Icon name="down" className="hidden h-4 w-4 text-slate-400 md:block" />
          </button>
        </div>
      </header>

      <div className="fixed inset-x-0 bottom-0 z-30 flex h-16 items-center justify-around border-t border-slate-200 bg-white lg:hidden">
        {nav.map(([id, icon, label]) => (
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