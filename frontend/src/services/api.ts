const API_URL = 'http://localhost:8080/api';

// Lança um Error com a mensagem { erro } devolvida pelo back-end (ou uma mensagem padrão).
const falhar = async (response: Response, padrao: string): Promise<never> => {
  let mensagem = padrao;
  try {
    const corpo = await response.json();
    if (corpo?.erro) mensagem = corpo.erro;
  } catch {
    /* corpo vazio ou não-JSON */
  }
  throw new Error(mensagem);
};

export const api = {
  // --- AUTENTICAÇÃO ---
  login: async (credenciais: any) => {
    const response = await fetch(`${API_URL}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(credenciais),
    });
    if (!response.ok) throw new Error('Credenciais inválidas');
    return response.json();
  },

  // --- CLIENTES ---
  criarCliente: async (cliente: any) => {
    const response = await fetch(`${API_URL}/clientes`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(cliente),
    });
    if (!response.ok) return falhar(response, 'Erro ao criar cliente');
    return response.json();
  },

  listarClientes: async () => {
    const response = await fetch(`${API_URL}/clientes`);
    if (!response.ok) throw new Error('Erro ao buscar clientes');
    return response.json();
  },

  // --- PEDIDOS DE ALUGUEL ---
  criarPedido: async (pedido: any) => {
    const response = await fetch(`${API_URL}/pedidos`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(pedido),
    });
    if (!response.ok) return falhar(response, 'Erro ao criar pedido');
    return response.json();
  },

  listarPedidos: async () => {
    const response = await fetch(`${API_URL}/pedidos`);
    if (!response.ok) throw new Error('Erro ao buscar pedidos');
    return response.json();
  },

  modificarPedido: async (id: number, usuarioId: number, dados: any) => {
    const response = await fetch(`${API_URL}/pedidos/${id}?usuarioId=${usuarioId}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(dados),
    });
    if (!response.ok) return falhar(response, 'Erro ao modificar pedido');
    return response.json();
  },

  cancelarPedido: async (id: number, usuarioId: number) => {
    const response = await fetch(`${API_URL}/pedidos/${id}/cancelar?usuarioId=${usuarioId}`, {
      method: 'PUT',
    });
    if (!response.ok) return falhar(response, 'Erro ao cancelar pedido');
    return response.json();
  },

  avaliarPedido: async (id: number, parecer: string, agenteId: number) => {
    const response = await fetch(`${API_URL}/pedidos/${id}/avaliar?parecer=${parecer}&agenteId=${agenteId}`, {
      method: 'PUT',
    });
    if (!response.ok) return falhar(response, 'Erro ao avaliar pedido');
    return response.json();
  },

  // --- CONTRATOS ---
  gerarContrato: async (pedidoId: number, contratoCredito?: any) => {
    const response = await fetch(`${API_URL}/contratos/gerar/${pedidoId}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(contratoCredito || {}),
    });
    if (!response.ok) return falhar(response, 'Erro ao gerar contrato');
    return response.json();
  },

  // --- AUTOMÓVEIS ---
  criarAutomovel: async (automovel: any) => {
    const response = await fetch(`${API_URL}/automoveis`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(automovel),
    });
    if (!response.ok) return falhar(response, 'Erro ao criar automóvel');
    return response.json();
  },

  listarAutomoveis: async () => {
    const response = await fetch(`${API_URL}/automoveis`);
    if (!response.ok) throw new Error('Erro ao buscar automóveis');
    return response.json();
  }
};