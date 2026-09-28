/**
 * CapSafe Backend — API REST para persistência das inferências (RF7).
 *
 * Modelo de dados (diagrama ER do enunciado, Figura 2):
 *   SESSAO_INFERENCIA (1) —— possui ——> (N) CAIXA_DELIMITADORA
 *
 * Persistência: arquivos JSON estruturados (opção prevista no enunciado),
 * com coleções separadas para sessões e caixas (relação 1-N via id_sessao).
 *
 * Endpoints (diagrama UML — InferenceController):
 *   POST   /api/inferencia             → salva sessão + caixas (payload do app)
 *   GET    /api/inferencia/historico   → lista sessões (com caixas embutidas)
 *   GET    /api/inferencia/:id         → detalhe de uma sessão + caixas
 *   GET    /api/inferencia/:id/caixas  → apenas as caixas da sessão
 *   DELETE /api/inferencia/:id         → remove sessão (cascata nas caixas)
 *   GET    /api/stats                  → estatísticas agregadas (bônus)
 *   GET    /api/health                 → healthcheck (bônus)
 */
const express = require('express');
const cors = require('cors');
const fs = require('fs');
const path = require('path');

const PORT = process.env.PORT || 3030;
const DATA_DIR = path.join(__dirname, 'data');
const DB_FILE = path.join(DATA_DIR, 'inferences.json');

// ---------- Persistência em JSON ----------

function carregarBanco() {
  try {
    const bruto = fs.readFileSync(DB_FILE, 'utf-8');
    const dados = JSON.parse(bruto);
    return {
      sessoes: Array.isArray(dados.sessoes) ? dados.sessoes : [],
      caixas: Array.isArray(dados.caixas) ? dados.caixas : []
    };
  } catch (e) {
    return { sessoes: [], caixas: [] };
  }
}

function salvarBanco(banco) {
  if (!fs.existsSync(DATA_DIR)) fs.mkdirSync(DATA_DIR, { recursive: true });
  fs.writeFileSync(DB_FILE, JSON.stringify(banco, null, 2), 'utf-8');
}

// ---------- Validação do payload (estrutura do enunciado, seção 4) ----------

function validarPayload(payload) {
  const erros = [];
  if (!payload || typeof payload !== 'object') erros.push('payload ausente');
  if (typeof payload.id_sessao !== 'string' || !payload.id_sessao.trim()) {
    erros.push('id_sessao é obrigatório (string)');
  }
  if (typeof payload.data_hora !== 'string' || !payload.data_hora.trim()) {
    erros.push('data_hora é obrigatório (ISO 8601)');
  }
  if (typeof payload.tempo_execucao_ms !== 'number') erros.push('tempo_execucao_ms deve ser número');
  if (!Array.isArray(payload.caixas_delimitadoras)) erros.push('caixas_delimitadoras deve ser array');

  if (!erros.length) {
    payload.caixas_delimitadoras.forEach((c, i) => {
      if (typeof c.rotulo_classe !== 'string') erros.push(`caixa[${i}].rotulo_classe obrigatório`);
      if (typeof c.centroide_x !== 'number' || typeof c.centroide_y !== 'number') {
        erros.push(`caixa[${i}] precisa de centroide_x/centroide_y numéricos`);
      }
    });
  }
  return erros;
}

// ---------- App ----------

const app = express();
app.use(cors());
app.use(express.json({ limit: '10mb' }));

app.use((req, _res, next) => {
  console.log(`[${new Date().toISOString()}] ${req.method} ${req.url}`);
  next();
});

// RF6 — recebe a inferência do app e persiste (RF7)
app.post('/api/inferencia', (req, res) => {
  const payload = req.body;
  const erros = validarPayload(payload);
  if (erros.length) {
    return res.status(400).json({ erro: 'Payload inválido', detalhes: erros });
  }

  const banco = carregarBanco();
  const agora = new Date().toISOString();

  // Remove duplicidade (mesma sessão reenviada) — upsert
  banco.sessoes = banco.sessoes.filter((s) => s.id_sessao !== payload.id_sessao);
  banco.caixas = banco.caixas.filter((c) => c.id_sessao !== payload.id_sessao);

  const sessao = {
    id_sessao: payload.id_sessao,
    data_hora: payload.data_hora,
    nome_modelo: payload.nome_modelo || 'desconhecido.onnx',
    tempo_execucao_ms: payload.tempo_execucao_ms,
    total_objetos:
      typeof payload.total_objetos === 'number'
        ? payload.total_objetos
        : payload.caixas_delimitadoras.length,
    confianca_media: payload.confianca_media ?? 0,
    dimensao_imagem: payload.dimensao_imagem || { largura_px: 0, altura_px: 0 },
    recebido_em: agora
  };

  const caixas = payload.caixas_delimitadoras.map((c, i) => ({
    id_sessao: payload.id_sessao,
    id_caixa: c.id_caixa ?? i + 1,
    rotulo_classe: c.rotulo_classe,
    confianca: c.confianca ?? 0,
    largura_px: c.largura_px ?? 0,
    altura_px: c.altura_px ?? 0,
    centroide_x: c.centroide_x ?? 0,
    centroide_y: c.centroide_y ?? 0,
    area_px2: c.area_px2 ?? (c.largura_px ?? 0) * (c.altura_px ?? 0)
  }));

  banco.sessoes.push(sessao);
  banco.caixas.push(...caixas);
  salvarBanco(banco);

  res.status(201).json({
    mensagem: 'Inferência salva com sucesso',
    id_sessao: sessao.id_sessao,
    total_caixas: caixas.length,
    recebido_em: agora
  });
});

// RF7/RF8 — histórico cronológico (mais recente primeiro)
app.get('/api/inferencia/historico', (req, res) => {
  const banco = carregarBanco();
  let sessoes = [...banco.sessoes].sort((a, b) =>
    String(b.data_hora).localeCompare(String(a.data_hora))
  );

  // Filtros opcionais: ?limite=10&modelo=best.onnx
  const { limite, modelo } = req.query;
  if (modelo) sessoes = sessoes.filter((s) => s.nome_modelo === modelo);
  if (limite && !Number.isNaN(Number(limite))) sessoes = sessoes.slice(0, Number(limite));

  // Embed das caixas (1-N) para importação offline no app
  const comCaixas = sessoes.map((s) => ({
    ...s,
    sincronizado: true,
    caixas_delimitadoras: banco.caixas
      .filter((c) => c.id_sessao === s.id_sessao)
      .sort((a, b) => a.id_caixa - b.id_caixa)
  }));

  res.json(comCaixas);
});

// Detalhe de uma sessão
app.get('/api/inferencia/:id', (req, res) => {
  const banco = carregarBanco();
  const sessao = banco.sessoes.find((s) => s.id_sessao === req.params.id);
  if (!sessao) return res.status(404).json({ erro: 'Sessão não encontrada' });
  const caixas = banco.caixas
    .filter((c) => c.id_sessao === req.params.id)
    .sort((a, b) => a.id_caixa - b.id_caixa);
  res.json({ ...sessao, caixas_delimitadoras: caixas });
});

// Caixas da sessão (UML: GET /api/inferencia/{id}/caixas)
app.get('/api/inferencia/:id/caixas', (req, res) => {
  const banco = carregarBanco();
  const existe = banco.sessoes.some((s) => s.id_sessao === req.params.id);
  if (!existe) return res.status(404).json({ erro: 'Sessão não encontrada' });
  const caixas = banco.caixas
    .filter((c) => c.id_sessao === req.params.id)
    .sort((a, b) => a.id_caixa - b.id_caixa);
  res.json(caixas);
});

// Remoção (cascata)
app.delete('/api/inferencia/:id', (req, res) => {
  const banco = carregarBanco();
  const antes = banco.sessoes.length;
  banco.sessoes = banco.sessoes.filter((s) => s.id_sessao !== req.params.id);
  banco.caixas = banco.caixas.filter((c) => c.id_sessao !== req.params.id);
  if (banco.sessoes.length === antes) {
    return res.status(404).json({ erro: 'Sessão não encontrada' });
  }
  salvarBanco(banco);
  res.json({ mensagem: 'Sessão removida', id_sessao: req.params.id });
});

// Bônus — estatísticas agregadas (útil para relatório do trabalho)
app.get('/api/stats', (_req, res) => {
  const banco = carregarBanco();
  const totalSessoes = banco.sessoes.length;
  const totalObjetos = banco.sessoes.reduce((soma, s) => soma + (s.total_objetos || 0), 0);
  const tempoMedio =
    totalSessoes > 0
      ? Math.round(banco.sessoes.reduce((soma, s) => soma + (s.tempo_execucao_ms || 0), 0) / totalSessoes)
      : 0;

  const porClasse = {};
  banco.caixas.forEach((c) => {
    porClasse[c.rotulo_classe] = (porClasse[c.rotulo_classe] || 0) + 1;
  });

  const porModelo = {};
  banco.sessoes.forEach((s) => {
    porModelo[s.nome_modelo] = (porModelo[s.nome_modelo] || 0) + 1;
  });

  res.json({ total_sessoes: totalSessoes, total_objetos: totalObjetos, tempo_medio_ms: tempoMedio, por_classe: porClasse, por_modelo: porModelo });
});

app.get('/api/health', (_req, res) => {
  res.json({ status: 'ok', servico: 'CapSafe Backend', versao: '1.0.0' });
});

// Raiz — documentação rápida
app.get('/', (_req, res) => {
  res.json({
    servico: 'CapSafe Backend — API REST de inferências',
    endpoints: [
      'POST   /api/inferencia',
      'GET    /api/inferencia/historico?limite=10&modelo=best.onnx',
      'GET    /api/inferencia/:id',
      'GET    /api/inferencia/:id/caixas',
      'DELETE /api/inferencia/:id',
      'GET    /api/stats',
      'GET    /api/health'
    ]
  });
});

app.listen(PORT, () => {
  console.log(`🦺 CapSafe Backend rodando em http://localhost:${PORT}`);
  console.log(`   Persistência: ${DB_FILE}`);
});
