import { useState } from "react";
import Shell from "./components/Shell";
import type { Screen } from "./components/Shell";
import Login from "./pages/Login";
import ClientRegistration from "./pages/ClientRegistration";
import Requests from "./pages/Requests";
import FinancialReview from "./pages/FinancialReview";

export default function App() {
  const [screen, setScreen] = useState<Screen>("login");

  if (screen === "login") return <Login onLogin={() => setScreen("requests")} />;

  return (
    <Shell screen={screen} setScreen={setScreen}>
      {screen === "clients" ? (
        <ClientRegistration />
      ) : screen === "review" ? (
        <FinancialReview />
      ) : (
        <Requests />
      )}
    </Shell>
  );
}