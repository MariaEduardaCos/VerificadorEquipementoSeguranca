# CapSafe Backend — API REST (RF7)

Servidor backend do trabalho N2: recebe, persiste e retorna os relatórios
de inferência do aplicativo CapSafe (detecção de capacete de segurança).

## Como executar

```bash
# com npm
npm install
npm run dev        # node --watch (reinício automático)

# ou com bun
bun install
bun --hot server.js
```

O servidor sobe na porta **3030** (variável `PORT` pode alterar).

## Endpoints (CRUD)

| Operação | Método | Rota | Descrição |
|----------|--------|------|-----------|
| **Create** | POST | `/api/inferencia` | Salva a inferência e suas caixas delimitadoras |
| **Read** | GET | `/api/inferencia/historico` | Histórico cronológico (`?limite=10&modelo=best.onnx`) |
| **Read** | GET | `/api/inferencia/:id` | Detalhe da sessão + caixas |
| **Read** | GET | `/api/inferencia/:id/caixas` | Caixas delimitadoras da sessão |
| **Update** | PUT | `/api/inferencia/:id` | Atualiza sessão e caixas existentes |
| **Delete** | DELETE | `/api/inferencia/:id` | Remove sessão (com exclusão em cascata nas caixas) |
| Metrics | GET | `/api/stats` | Estatísticas agregadas de inferências |
| Health | GET | `/api/health` | Healthcheck do servidor |

## Persistência

Arquivos JSON estruturados em `data/inferences.json`, com **duas coleções**
que materializam o diagrama ER do enunciado:

- `sessoes[]` — SESSAO_INFERENCIA (id_sessao, data_hora, nome_modelo, total_objetos, tempo_ms…)
- `caixas[]` — CAIXA_DELIMITADORA (id_caixa, rotulo_classe, confianca, largura_px, altura_px, centroide_x, centroide_y, area_px2), com FK `id_sessao`

## Teste rápido

```bash
# health
curl http://localhost:3030/api/health

# enviar exemplo
curl -X POST http://localhost:3030/api/inferencia \
  -H "Content-Type: application/json" \
  -d '{"id_sessao":"sessao_teste_001","data_hora":"2026-09-18T23:06:00Z","nome_modelo":"best.onnx","tempo_execucao_ms":850,"total_objetos":1,"confianca_media":0.9,"dimensao_imagem":{"largura_px":1080,"altura_px":1920},"caixas_delimitadoras":[{"id_caixa":1,"rotulo_classe":"capacete","confianca":0.93,"largura_px":120.5,"altura_px":45.0,"centroide_x":210.25,"centroide_y":422.5,"area_px2":5422.5}]}'

# histórico
curl http://localhost:3030/api/inferencia/historico
```

## Conexão com o app Android

- **Emulador:** use `http://10.0.2.2:3030/` na Tela 1 do app.
- **Dispositivo físico na mesma rede:** use `http://<IP_DO_PC>:3030/`
  (ou `adb reverse tcp:3030 tcp:3030` e acesse `http://127.0.0.1:3030/`).
