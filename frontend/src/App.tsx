import { useEffect, useState } from "react";
import Shell from "./components/Shell";
import type { Screen } from "./components/Shell";
import Login from "./pages/Login";
import ClientRegistration from "./pages/ClientRegistration";
import Requests from "./pages/Requests";
import FinancialReview from "./pages/FinancialReview";
import Vehicles from "./pages/Vehicles";
import Settings from "./pages/Settings";
import { ToastProvider, useToast } from "./components/Toast";
import { salvarUsuario, setOnUnauthorized, setToken, usuarioSalvo } from "./services/api";

const CHAVE_TELA = "driveja.tela";
const telaInicial = (u: any): Screen => {
  try {
    const salva = sessionStorage.getItem(CHAVE_TELA) as Screen | null;
    if (salva && salva !== "login" && salva !== "register") return salva;
  } catch {
    /* ignora */
  }
  return u.tipo === "CLIENTE" ? "requests" : "review";
};

export default function App() {
  return (
    <ToastProvider>
      <Aplicacao />
    </ToastProvider>
  );
}

function Aplicacao() {
  const toast = useToast();
  // Restaura a sessão guardada (F5 não desloga).
  const [user, setUser] = useState<any>(() => usuarioSalvo());
  const [screen, setScreen] = useState<Screen>(() => {
    const u = usuarioSalvo();
    return u ? telaInicial(u) : "login";
  });

  useEffect(() => {
    try {
      if (screen !== "login" && screen !== "register") sessionStorage.setItem(CHAVE_TELA, screen);
    } catch {
      /* ignora */
    }
  }, [screen]);

  const handleLogin = (userData: any) => {
    salvarUsuario(userData);
    setUser(userData);
    try {
      sessionStorage.removeItem(CHAVE_TELA);
    } catch {
      /* ignora */
    }
    setScreen(userData.tipo === "CLIENTE" ? "requests" : "review");
  };

  const handleLogout = () => {
    setToken(null);
    setUser(null);
    try {
      sessionStorage.removeItem(CHAVE_TELA);
    } catch {
      /* ignora */
    }
    setScreen("login");
  };

  // Token expirado/inválido: volta para o login
  useEffect(() => {
    setOnUnauthorized(() => {
      handleLogout();
      toast.info("Sua sessão expirou", "Entre novamente para continuar.");
    });
    return () => setOnUnauthorized(null);
  }, []);

  if (screen === "login") return <Login onLogin={handleLogin} onRegister={() => setScreen("register")} />;

  // Autocadastro público de cliente (antes do login)
  if (screen === "register") {
    return (
      <div className="min-h-screen bg-[#f6f8fb]">
        <div className="mx-auto max-w-5xl px-4 pt-6">
          <button onClick={() => setScreen("login")} className="button-secondary">
            ← Voltar ao login
          </button>
        </div>
        <ClientRegistration />
      </div>
    );
  }

  return (
    <Shell screen={screen} setScreen={setScreen} user={user}>
      {screen === "clients" ? (
        <ClientRegistration />
      ) : screen === "review" ? (
        <FinancialReview user={user} />
      ) : screen === "settings" ? (
        <Settings user={user} onLogout={() => { handleLogout(); toast.info("Você saiu da conta"); }} />
      ) : screen === "vehicles" ? (
        <Vehicles user={user} />
      ) : (
        <Requests user={user} />
      )}
    </Shell>
  );
}