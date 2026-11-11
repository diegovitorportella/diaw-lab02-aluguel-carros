const API_URL = 'http://localhost:8080/api';

// Token JWT da sessão atual (somente em memória: recarregar a página encerra a sessão).
let token: string | null = null;
let aoExpirarSessao: (() => void) | null = null;

export const setToken = (novo: string | null) => {
  token = novo;
};

/** Registra o callback chamado quando a API responde 401 (token inválido ou expirado). */
export const setOnUnauthorized = (callback: (() => void) | null) => {
  aoExpirarSessao = callback;
};

/** Erro da API: mensagem geral e, em falhas de validação, a mensagem de cada campo. */
export class ApiError extends Error {
  campos: Record<string, string>;

  constructor(mensagem: string, campos: Record<string, string> = {}) {
    super(mensagem);
    this.campos = campos;
  }
}

interface Opcoes {
  method?: string;
  body?: unknown;
  publica?: boolean; // rota pública: não exige nem envia token
}

// Faz a requisição com o header Authorization e converte erros em Error com a mensagem { erro } do back-end.
async function requisitar(caminho: string, erroPadrao: string, opcoes: Opcoes = {}): Promise<any> {
  const headers: Record<string, string> = {};
  if (opcoes.body !== undefined) headers['Content-Type'] = 'application/json';
  if (token && !opcoes.publica) headers['Authorization'] = `Bearer ${token}`;

  const response = await fetch(`${API_URL}${caminho}`, {
    method: opcoes.method ?? 'GET',
    headers,
    body: opcoes.body !== undefined ? JSON.stringify(opcoes.body) : undefined,
  });

  if (!response.ok) {
    if (response.status === 401 && !opcoes.publica) {
      aoExpirarSessao?.();
      throw new Error('Sessão expirada. Faça login novamente.');
    }
    let mensagem = erroPadrao;
    let campos: Record<string, string> = {};
    try {
      const corpo = await response.json();
      if (corpo?.erro) mensagem = corpo.erro;
      if (corpo?.campos) campos = corpo.campos;
    } catch {
      /* corpo vazio ou não-JSON */
    }
    throw new ApiError(mensagem, campos);
  }

  return response.status === 204 ? null : response.json();
}

export const api = {
  // --- AUTENTICAÇÃO ---
  login: async (credenciais: { login: string; senha: string }) => {
    const resposta = await requisitar('/auth/login', 'Credenciais inválidas', {
      method: 'POST',
      body: credenciais,
      publica: true,
    });
    setToken(resposta.token);
    return resposta.usuario; // { id, login, tipo, ... }
  },

  // --- CLIENTES ---
  criarCliente: (cliente: any) =>
    requisitar('/clientes', 'Erro ao criar cliente', { method: 'POST', body: cliente, publica: true }),

  listarClientes: () => requisitar('/clientes', 'Erro ao buscar clientes'),

  // --- PEDIDOS DE ALUGUEL (o usuário vem do token; o back-end escopa por perfil) ---
  criarPedido: (pedido: any) =>
    requisitar('/pedidos', 'Erro ao criar pedido', { method: 'POST', body: pedido }),

  listarPedidos: (status?: string) =>
    requisitar(`/pedidos${status ? `?status=${status}` : ''}`, 'Erro ao buscar pedidos'),

  modificarPedido: (id: number, dados: any) =>
    requisitar(`/pedidos/${id}`, 'Erro ao modificar pedido', { method: 'PUT', body: dados }),

  cancelarPedido: (id: number) =>
    requisitar(`/pedidos/${id}/cancelar`, 'Erro ao cancelar pedido', { method: 'PUT' }),

  /** Simulação de financiamento do pedido (somente agentes): parcela, total pago e comprometimento da renda. */
  simularFinanciamento: (
    pedidoId: number,
    params: { parcelas: number; juros: number; entrada?: number; valorFinanciado?: number },
  ) => {
    const q = new URLSearchParams({ parcelas: String(params.parcelas), juros: String(params.juros) });
    if (params.entrada !== undefined) q.set('entrada', String(params.entrada));
    if (params.valorFinanciado !== undefined) q.set('valorFinanciado', String(params.valorFinanciado));
    return requisitar(`/pedidos/${pedidoId}/simulacao?${q}`, 'Erro ao simular financiamento');
  },

  avaliarPedido: (id: number, parecer: string) =>
    requisitar(`/pedidos/${id}/avaliar?parecer=${parecer}`, 'Erro ao avaliar pedido', { method: 'PUT' }),

  // --- CONTRATOS ---
  gerarContrato: (pedidoId: number, contratoCredito?: any) =>
    requisitar(`/contratos/gerar/${pedidoId}`, 'Erro ao gerar contrato', {
      method: 'POST',
      body: contratoCredito || {},
    }),

  // --- AUTOMÓVEIS (o proprietário é o usuário logado) ---
  criarAutomovel: (automovel: any) =>
    requisitar('/automoveis', 'Erro ao criar automóvel', { method: 'POST', body: automovel }),

  listarAutomoveis: () => requisitar('/automoveis', 'Erro ao buscar automóveis'),

  atualizarAutomovel: (id: number, automovel: any) =>
    requisitar(`/automoveis/${id}`, 'Erro ao atualizar automóvel', { method: 'PUT', body: automovel }),

  excluirAutomovel: (id: number) =>
    requisitar(`/automoveis/${id}`, 'Erro ao excluir automóvel', { method: 'DELETE' }),
};
