import { useEffect, useState } from "react";
import Shell from "./components/Shell";
import type { Screen } from "./components/Shell";
import Login from "./pages/Login";
import ClientRegistration from "./pages/ClientRegistration";
import Requests from "./pages/Requests";
import FinancialReview from "./pages/FinancialReview";
import Vehicles from "./pages/Vehicles";
import Settings from "./pages/Settings";
import { setOnUnauthorized, setToken } from "./services/api";

export default function App() {
  const [screen, setScreen] = useState<Screen>("login");
  const [user, setUser] = useState<any>(null);

  const handleLogin = (userData: any) => {
    setUser(userData);
    setScreen(userData.tipo === "CLIENTE" ? "requests" : "review");
  };

  const handleLogout = () => {
    setToken(null);
    setUser(null);
    setScreen("login");
  };

  // Token expirado/inválido: volta para o login
  useEffect(() => {
    setOnUnauthorized(handleLogout);
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
        <Settings user={user} onLogout={handleLogout} />
      ) : screen === "vehicles" ? (
        <Vehicles user={user} />
      ) : (
        <Requests user={user} />
      )}
    </Shell>
  );
}