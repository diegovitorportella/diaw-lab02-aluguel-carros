import logo from "../assets/logo.png";
import logoLight from "../assets/logo-light.png";

// `light` seleciona a versão com texto branco, para fundos escuros.
export default function Logo({ light = false }: { light?: boolean }) {
  return <img src={light ? logoLight : logo} alt="DriveJá" className="h-9 w-auto" />;
}
