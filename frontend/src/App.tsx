import { useState } from "react";
import Shell from "./components/Shell";
import type { Screen } from "./components/Shell";
import Login from "./pages/Login";
import ClientRegistration from "./pages/ClientRegistration";
import Requests from "./pages/Requests";
import FinancialReview from "./pages/FinancialReview";
import Vehicles from "./pages/Vehicles";

export default function App() {
  const [screen, setScreen] = useState<Screen>("login");
  const [user, setUser] = useState<any>(null);

  const handleLogin = (userData: any) => {
    setUser(userData);
    setScreen(userData.tipo === "CLIENTE" ? "requests" : "review");
  };

  const handleLogout = () => {
    setUser(null);
    setScreen("login");
  };

  if (screen === "login") return <Login onLogin={handleLogin} />;

  return (
    <Shell screen={screen} setScreen={setScreen} user={user} onLogout={handleLogout}>
      {screen === "clients" ? (
        <ClientRegistration />
      ) : screen === "review" ? (
        <FinancialReview user={user} />
      ) : screen === "vehicles" ? (
        <Vehicles user={user} />
      ) : (
        <Requests user={user} />
      )}
    </Shell>
  );
}