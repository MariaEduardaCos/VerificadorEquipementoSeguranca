# 🦺 CapSafe — Verificação de Capacete de Segurança com Visão Computacional

**Trabalho Prático N2 — Desenvolvimento de Software para Dispositivos Móveis (UniRV)**
Aplicativo Android **Full-Stack em Kotlin**: Visão Computacional, **Inferência On-Device (YOLOv8/ONNX)** e Persistência em Servidor.

> Tema do grupo: verificar se o trabalhador está utilizando o **capacete de segurança (EPI)**,
> usando um dataset de capacetes do Roboflow e o modelo YOLO exportado pelo professor em `.onnx`.

---

## 1. Visão geral

| Item | Descrição |
|------|-----------|
| **App (pasta raiz deste projeto)** | Kotlin + **Jetpack Compose** (100% Compose, sem XML Views), **Light Mode** obrigatório |
| **Navegação** | **Bottom Navigation Bar** (`Detectar` / `Histórico`) + 4 telas (1 a 4 do enunciado) |
| **Inferência** | **ONNX Runtime Android** — execução 100% local (CPU, com opção NNAPI), sem nuvem |
| **Persistência local** | Room (espelha o diagrama ER: `SESSAO_INFERENCIA 1—N CAIXA_DELIMITADORA`) |
| **Backend** | `backend/` — API REST Node.js/Express com persistência em arquivos JSON |
| **Arquitetura** | MVVM, Coroutines, **StateFlow**, Repository (RF9) |

### Cobertura dos Requisitos Funcionais (enunciado, seção 3)

| RF | Requisito | Onde está implementado |
|----|-----------|------------------------|
| RF1 | Gerenciamento de modelos ONNX (embarcado ou do dispositivo) | `ui/detect/DetectViewModel.kt` (carregarModeloEmbarcado / onModelSelected), `assets/best.onnx` |
| RF2 | Aquisição de imagens (Câmera / Galeria) | `ui/detect/DetectScreen.kt` (ActivityResultContracts.TakePicture + GetContent) |
| RF3 | Ajuste dinâmico: Slider de confiança + filtro de classes | `DetectScreen.kt` (Slider + FilterChips), `YoloOnnxDetector.detect()` |
| RF4 | Pipeline de inferência local on-device | `inference/YoloOnnxDetector.kt` (letterbox → CHW → ORT → NMS) |
| RF5 | Métricas geométricas (W, H, centróide, área) | `data/model/BoundingBox.kt` (Xc = Xmin + W/2 ; Yc = Ymin + H/2 ; A = W×H) |
| RF6 | Comunicação com backend (POST JSON automático) | `data/InferenceRepository.salvarInferencia()` + `data/remote/InferenceApi.kt` |
| RF7 | Persistência e consulta no servidor | `backend/server.js` (coleções JSON 1-N) + `importarDoServidor()` |
| RF8 | Histórico + detalhamento (Telas 3 e 4) | `ui/history/HistoryScreen.kt`, `ui/details/BoxDetailsScreen.kt` |
| RF9 | Arquitetura limpa (MVVM, Coroutines, StateFlow) | `ui/detect/DetectViewModel.kt`, `ui/history/HistoryViewModel.kt` |

---

## 2. Dataset (Etapa 1 — Roboflow)

Sugestão já alinhada ao tema (verificar com o professor — Etapa 2):

- **Hard Hat Workers** — `https://universe.roboflow.com/joseph-nelson/hard-hat-workers`
  Classes: `head`, `helmet`, `person`.
- Alternativas: "Safety Helmet Detection" (Wenyu Lv / Roboflow Universe), "Construction Site Safety".

No app, os rótulos em português são tratados assim (`data/model/BoundingBox.kt`):

- `helmet` → **capacete** (conformidade, caixa verde)
- `head` → **cabeca** (violação — trabalhador sem capacete, caixa vermelha)
- `person` → **pessoa** (caixa âmbar)

⚠️ **Importante:** a lista em `app/src/main/assets/labels.txt` deve conter os rótulos
**na mesma ordem das classes do treinamento**. Você também pode ajustar os rótulos
diretamente na Tela 1 (campo "Rótulos personalizados").

---

## 3. Treinamento YOLOv8 e exportação ONNX (Etapas 3 e 4 da aula)

O professor treina a rede e entrega o `best.onnx`. A exportação equivale a:

```python
from ultralytics import YOLO

model = YOLO('weights/best.pt')
model.export(format='onnx', dynamic=True, simplify=True)  # → best.onnx
```

O fluxo de inferência implementado no app segue a aula (*Visão Computacional e Aprendizado Profundo*):

```
Imagem → Letterbox 640×640 → tensor CHW [1,3,640,640] normalizado
       → ONNX Runtime (forward pass, pesos congelados)
       → saída [1, 4+nc, 8400] → filtro de confiança (RF3)
       → NMS (IoU 0.45) → reescala p/ resolução original
       → BoundingBox(W, H, Centróide, Área)  (RF5)
```

---

## 4. Configurando o modelo no app

**Opção A (embarcado):** copie o modelo para
`app/src/main/assets/best.onnx` e os rótulos para `app/src/main/assets/labels.txt`.

**Opção B (do dispositivo):** sem arquivo nos assets, abra o app e use
**"Selecionar modelo"** na Tela 1 (RF1 — escolhe um `.onnx` do armazenamento).

---

## 5. Backend (Etapa 5 do enunciado)

```bash
cd backend
npm install && npm run dev     # ou: bun install && bun --hot server.js
# → http://localhost:3030
```

Endpoints (diagrama UML): `POST /api/inferencia`, `GET /api/inferencia/historico`,
`GET /api/inferencia/{id}/caixas` (+ `GET /:id`, `DELETE /:id`, `GET /api/stats`, `GET /api/health`).
Detalhes em `backend/README.md`.

### Conectar o app ao servidor

| Ambiente | URL na Tela 1 |
|----------|---------------|
| Emulador Android | `http://10.0.2.2:3030/` (localhost do host) |
| Dispositivo físico (mesma rede) | `http://IP_DO_SEU_PC:3030/` |
| Dispositivo físico (USB) | `adb reverse tcp:3030 tcp:3030` → `http://127.0.0.1:3030/` |

O envio é automático após cada inferência (RF6). Se o servidor estiver fora do ar,
a sessão fica salva no Room como **"Pendente"** e pode ser reenviada em
Histórico → **"Enviar pendentes"** ou importada com **"Importar do servidor"**.

---

## 6. Como executar o app (Android Studio)

1. Abra a pasta `android/` no Android Studio (Koala ou superior, JDK 17).
2. Aguarde a sincronização do Gradle (wrapper 8.9).
3. Copie o `best.onnx` para `app/src/main/assets/` (Opção A) ou use a Opção B.
4. Rode em um **emulador ou dispositivo com Android 8.0+ (API 26)**.
5. Suba o backend (`backend/`) e confira a URL na Tela 1.

Gerar APK: `Build → Build Bundle(s) / APK(s) → Build APK(s)`.

---

## 7. Estrutura do projeto

```
android/
├── app/src/main/
│   ├── assets/                     # best.onnx (você adiciona) + labels.txt
│   ├── res/                        # ícone adaptativo, tema base, file_paths
│   └── java/br/unirv/capsafe/
│       ├── MainActivity.kt
│       ├── navigation/CapSafeApp.kt        # Bottom Nav + rotas das 4 telas
│       ├── ui/theme/                       # DESIGN SYSTEM "Obra Segura"
│       │   ├── Color.kt  Type.kt  Shape.kt  Theme.kt
│       ├── ui/components/CommonComponents.kt
│       ├── ui/detect/                      # Tela 1 (config/entrada) + ViewModel
│       ├── ui/result/                      # Tela 2 (resultado gráfico)
│       ├── ui/history/                     # Tela 3 (histórico) + ViewModel
│       ├── ui/details/                     # Tela 4 (detalhamento geométrico)
│       ├── inference/YoloOnnxDetector.kt   # RF1/RF4 — pipeline ONNX + NMS + anotação
│       ├── data/
│       │   ├── model/ (BoundingBox, InferenceResult, DTOs)
│       │   ├── local/ (Room: Entities, DAO, Database)
│       │   ├── remote/ (Retrofit: InferenceApi, ApiClient)
│       │   ├── AppPrefs.kt  InferenceRepository.kt (RF6/RF7)
│       └── util/AppUtils.kt
└── backend/                        # API REST Node/Express (JSON, porta 3030)
```

## 8. Payload de comunicação (referência do enunciado, seção 4)

```json
{
  "id_sessao": "sessao_20260918_230601123",
  "data_hora": "2026-09-18T23:06:00Z",
  "nome_modelo": "best.onnx",
  "tempo_execucao_ms": 909,
  "total_objetos": 2,
  "confianca_media": 0.9,
  "dimensao_imagem": { "largura_px": 1080, "altura_px": 1920 },
  "caixas_delimitadoras": [
    {
      "id_caixa": 1,
      "rotulo_classe": "capacete",
      "confianca": 0.93,
      "largura_px": 120.5,
      "altura_px": 45.0,
      "centroide_x": 210.25,
      "centroide_y": 422.5,
      "area_px2": 5422.5
    }
  ]
}
```

## 9. Equipe e entrega

- Formato: grupo (até 4 alunos) — preencha os nomes aqui.
- Submissão: repositórios públicos no GitHub (App + Backend) e APK.
- Dica de repositório: separe `capSafe-android` (esta pasta) e `capSafe-backend`
  em dois repositórios, ou use um repositório único com as pastas em `apps/`.
