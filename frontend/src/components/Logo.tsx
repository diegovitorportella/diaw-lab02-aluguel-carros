import Icon from "./Icon";

export default function Logo({ light = false }: { light?: boolean }) {
  return (
    <div className="flex items-center gap-3">
      <span
        className={`grid h-10 w-10 place-items-center rounded-xl ${
          light ? "bg-white text-blue-700" : "bg-blue-600 text-white"
        } shadow-sm`}
      >
        <Icon name="car" />
      </span>
      <span
        className={`text-xl font-bold tracking-tight ${
          light ? "text-white" : "text-slate-950"
        }`}
      >
        Drive
        <span className={light ? "text-blue-200" : "text-blue-600"}>Flow</span>
      </span>
    </div>
  );
}