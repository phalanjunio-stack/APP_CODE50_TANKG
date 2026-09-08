# SR Lakes Tone

Aplicativo Android para guitarristas que usam **M-VAVE TANK-G** + **Marshall CODE50**,
pensado para o celular preso ao pedestal durante ensaio, passagem de som e show.

> **Regra que atravessa o projeto inteiro:** o analisador **mede e sugere**.
> Ele **nunca** altera timbre sozinho. Quem escolhe a cena é o músico.
> Não existe caminho de código em que uma medição chame o `DeviceManager`.

Estado atual: **Fase 1 (MVP)** — funcional de ponta a ponta com aparelhos **simulados**.
Compila, instala e roda. O analisador de áudio é real e funciona pelo microfone.

Repositório: **https://github.com/phalanjunio-stack/APP_CODE50_TANKG** (público —
é o que permite o app se atualizar sozinho, veja a seção 12).

**Para instalar no celular da banda, sem computador**, use a página de instalação
(botão de download + QR code): **https://phalanjunio-stack.github.io/APP_CODE50_TANKG/**

---

## Índice

1. [Abrir no Android Studio](#1-abrir-no-android-studio)
2. [Compilar](#2-compilar)
3. [Instalar no celular](#3-instalar-no-celular)
4. [Testar o analisador](#4-testar-o-analisador)
5. [Mapa dos módulos](#5-mapa-dos-módulos)
6. [Onde fica o código do protocolo TANK-G](#6-onde-fica-o-código-do-protocolo-tank-g)
7. [Onde fica o código do protocolo Marshall CODE50](#7-onde-fica-o-código-do-protocolo-marshall-code50)
8. [Como trocar os MockServices pelos serviços reais](#8-como-trocar-os-mockservices-pelos-serviços-reais)
9. [Integração com o SR Lakes Studio](#9-integração-com-o-sr-lakes-studio)
10. [Limitações honestas](#10-limitações-honestas)
11. [O que vem na Fase 2](#11-o-que-vem-na-fase-2)
12. [Atualização online e como publicar uma release](#12-atualização-online-e-como-publicar-uma-release)

---

## 1. Abrir no Android Studio

Requisitos:

- Android Studio Ladybug (2024.2) ou mais novo
- JDK 17 ou superior (o projeto foi construído com JDK 21)
- Android SDK com a plataforma **API 35** instalada
- Gradle 8.11.1 (já vem pelo wrapper — não instale nada)

Passos:

1. `File > Open` e escolha a pasta raiz do projeto (a que contém `settings.gradle.kts`).
2. Espere o **Gradle Sync** terminar. Na primeira vez ele baixa as dependências.
3. Se o Studio reclamar do SDK, aponte `local.properties` para o seu SDK:

```bash
echo "sdk.dir=C:\\Android\\Sdk" > local.properties
```

O projeto usa **version catalog** (`gradle/libs.versions.toml`). Todas as versões de
biblioteca estão nesse arquivo — é o único lugar para mexer nelas.

---

## 2. Compilar

Pela linha de comando, na raiz do projeto:

```bash
./gradlew assembleDebug
```

O APK sai em `app/build/outputs/apk/debug/app-debug.apk`.

Rodar os testes de unidade (FFT, medidores, comparações, MIDI, macros,
casamento de títulos):

```bash
./gradlew test
```

São 36 testes. Os dois de integração com o SR Lakes Studio só rodam se houver
um Studio no ar — veja a [seção 9](#9-integração-com-o-sr-lakes-studio).

Build de release (não assinado):

```bash
./gradlew assembleRelease
```

No Android Studio: selecione a configuração **app** e clique em Run.

---

## 3. Instalar no celular

### Para a banda: sem computador nem cabo

O jeito pensado para os músicos é a página de instalação, publicada pelo
próprio repositório (GitHub Pages):

**https://phalanjunio-stack.github.io/APP_CODE50_TANKG/**

Abre no navegador do celular, tem um botão de download grande e um QR code
que aponta para o mesmo lugar — a versão mais recente publicada, sempre. Essa
é a **única vez** que precisa de um passo manual: depois de instalado uma
vez, o próprio app confere e se atualiza sozinho (seção 12).

O link de download é estável entre versões
(`releases/latest/download/app-release.apk`), então essa página nunca
precisa ser republicada quando sai uma versão nova.

### Para desenvolvimento: com Android Studio ou adb

Com o celular no modo desenvolvedor e depuração USB ligada:

```bash
./gradlew installDebug
```

Ou manualmente:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

O build de debug (`applicationId` termina em `.debug`) convive no aparelho
com uma instalação da versão de release baixada pela página acima — são
pacotes diferentes, um não substitui o outro.

Na primeira abertura o app cria os **dados de demonstração**: sete presets
(`SR Clean Valve`, `SR Pop Rock Base`, `SR Pop Rock Solo`, `SR Crunch`, ...),
cinco músicas com cenas e seções prontas, e o setlist `SHOW 07/09`.

Permissões que o app pede, e por quê:

| Permissão | Para quê |
|---|---|
| `RECORD_AUDIO` | Medir o som que sai do amplificador. Sem ela o analisador não abre. |
| `BLUETOOTH_SCAN` / `BLUETOOTH_CONNECT` | Encontrar e conectar os aparelhos (Android 12+). |
| `BLUETOOTH` / `BLUETOOTH_ADMIN` / `ACCESS_FINE_LOCATION` | O mesmo, no Android 11 e anteriores. |
| `INTERNET` / `ACCESS_NETWORK_STATE` | Falar com o SR Lakes Studio na **rede local**. Só isso. |

**Sobre a rede:** o único destino que o app procura é o servidor do SR Lakes
Studio no notebook da banda, e só quando você liga a sincronia (que vem
desligada). Não há servidor na internet, nem análise na nuvem, nem IA: FFT,
VU, pico, RMS, ruído, espectro, A/B e Base x Solo são todos calculados no
aparelho e os dados ficam nele.

---

## 4. Testar o analisador

O analisador funciona **de verdade** já nesta fase — ele não depende de nenhum
aparelho conectado.

### Teste rápido, em casa

1. Abra a aba **Analisador** e conceda a permissão de microfone.
2. Toque qualquer coisa perto do celular (a guitarra pelo amp, ou até música numa
   caixa de som). O espectro e os medidores devem reagir na hora.
3. Confira o topo da tela:
   - **"Captura UNPROCESSED disponível"** (verde) na aba Dispositivos = as medições
     são confiáveis.
   - Um aviso âmbar de **"Medição não calibrada"** = seu aparelho não oferece captura
     crua, e o Android está aplicando ganho automático. Nesse caso use só as
     *diferenças* entre medições, nunca os valores absolutos.

### Teste do BASE x SOLO (o mais útil na prática)

1. Prenda o celular no pedestal, na posição em que ele vai ficar no show.
2. Aba **Analisador** → painel **Base x Solo**.
3. **GRAVAR BASE** e toque a sua base por 8 segundos.
4. **GRAVAR SOLO** e toque o solo por 8 segundos, **sem mover o celular**.
5. O app mostra a diferença em dB e um veredito:
   `SOLO MUITO BAIXO` / `SOLO EQUILIBRADO` / `SOLO MUITO ALTO`.
   Um solo saudável costuma ficar entre **+2 e +6 dB** sobre a base.
6. Ajuste **você mesmo** o volume no pedal ou no amp e meça de novo.

Se o piso de ruído mudar muito entre as duas capturas, o app avisa que a comparação
provavelmente não é válida — sinal de que o celular ou o ambiente mudaram.

### Teste do A/B

Mesma ideia, mas para comparar duas regulagens: **SALVAR A**, mude o que quiser,
**SALVAR B**. O app mostra o delta de RMS, de pico e de cada uma das cinco faixas.

### Salvar no histórico

Depois de um A/B ou um BASE x SOLO, preencha música/contexto/observação e toque
**SALVAR MEDIÇÃO**. Fica guardado com data, e o **áudio cru de cada captura é gravado
em WAV** dentro de `files/captures/` — dá para reouvir e reanalisar meses depois.

### O que esperar dos Tone Insights

São regras simples sobre a distribuição de energia, com histerese para não piscar:

| Situação | Mensagem |
|---|---|
| Muita energia em 150–300 Hz | "Som pode estar embolado" |
| Concentração em 3–5 kHz | "Som pode estar áspero" |
| Excesso em 6–10 kHz | "Excesso de brilho / fizz" |
| Poucos médios (800 Hz – 2 kHz) | "Som pode desaparecer na mistura" |
| Amostras estourando | "Entrada saturando" |
| Piso de ruído acima de −45 dB | "Ruído elevado" |

São **sugestões**. O app nunca aplica nada.

---

## 5. Mapa dos módulos

```
:app                Compose, navegação, telas, ViewModels, injeção manual
:core:model         Modelos puros (Kotlin JVM, zero Android)
:core:analysis      FFT, medidores, faixas, Tone Insights, comparações  (Kotlin puro)
:core:protocol      MIDI, DeviceProfile, MIDI Learn, motor de macros    (Kotlin puro)
:device:api         Interfaces: DeviceTransport, TankGService, MarshallCodeService
:device:mock        Implementações simuladas (a Fase 1 roda em cima delas)
:audio              AudioRecord, fontes de microfone e USB, WAV
:data               Room, repositórios, DataStore, dados de demonstração
:sync               Ponte com o SR Lakes Studio (Socket.IO, mDNS)
```

Os três módulos `:core:*` são **Kotlin puro, sem nenhum import de Android**. Isso é
proposital: quando quiser portar para Windows ou iOS, eles viram módulos Kotlin
Multiplatform sem reescrever nada. Não usei KMP agora porque ele traz muito atrito
de build para um MVP.

Fluxo do áudio, como no plano original:

```
AudioInputManager
  ├─ MicrophoneAudioSource   (AMP MIC)
  └─ UsbAudioSource          (USB DIRECT)
        ↓  Dispatchers.IO, uma única corrotina
   AudioAnalyzer
        ├─ Fft + janela de Hann
        ├─ LevelMeterProcessor   (peak, RMS, ruído, clipping, crest)
        ├─ ToneBandProcessor     (LOW … HIGH)
        └─ SpectrumSmoother + LogSpectrumMapper
        ↓  StateFlow<AudioAnalysis>, limitado ao refresh configurado
   AnalyzerViewModel → Jetpack Compose
```

Nada de FFT na thread de UI. O `AudioAnalyzer` não aloca a cada quadro (tabelas de
twiddle e buffers pré-alocados), e a emissão para a tela é limitada pelo *refresh rate*
das Configurações para o Compose não recompor 47 vezes por segundo à toa.

**O aplicativo não fica no caminho do sinal da guitarra.** Ele só escuta em paralelo.
Por isso não adiciona latência nenhuma durante uma apresentação.

---

## 6. Onde fica o código do protocolo TANK-G

Hoje: `device/mock/src/main/java/com/srlakes/tone/device/mock/MockDevices.kt`
(classe `MockTankGService`, perfil `MockProfiles.tankG`).

Amanhã: crie o módulo **`:device:blemidi`** e a classe `BleMidiTankGService`.

Ela **não precisa reimplementar nada de lógica**: basta estender
`BaseMidiDeviceService` (em `:device:api`) passando um `DeviceTransport` real.
Toda a tradução de parâmetro → mensagem MIDI já está escrita e testada.

```kotlin
class BleMidiTankGService(
    scope: CoroutineScope,
    transport: BleMidiTransport
) : BaseMidiDeviceService(
    kind = DeviceKind.TANK_G,
    transport = transport,
    initialProfile = DeviceProfile.empty(DeviceKind.TANK_G),
    scope = scope
), TankGService
```

Repare no `DeviceProfile.empty(...)`: um aparelho real começa **sem nenhum mapeamento**.
Nós não chutamos números de CC. Eles chegam pelo **MIDI Learn** e ficam marcados com
`learned = true` (veja `MidiLearnSession` em `:core:protocol`), persistidos pelo
`DeviceRepository`.

### Antes de escrever a camada BLE, verifique isto

Se o TANK-G falar **BLE MIDI padrão** (o perfil público da MIDI Association), o
trabalho é pequeno: o Android tem suporte nativo via
`MidiManager.openBluetoothDevice()`, e Program Change / Control Change funcionam sem
nenhuma engenharia reversa.

Confira com o app **nRF Connect** (grátis): ligue o pedal, escaneie, e veja se aparece
o serviço BLE MIDI. Se aparecer, implemente `BleMidiTransport` em cima do
`android.media.midi` e pronto. Se for um serviço proprietário, aí sim é engenharia
reversa — e o `MidiMessage.SysEx` já existe para carregar o resultado.

---

## 7. Onde fica o código do protocolo Marshall CODE50

Hoje: a mesma `MockDevices.kt` (classe `MockMarshallCodeService`).

Amanhã: `MarshallCodeService` real. **Atenção a uma premissa importante:**

O CODE50 faz *streaming de áudio* por Bluetooth, o que é **A2DP = Bluetooth Classic**,
não BLE. É bem provável que o canal de controle do app Marshall Gateway ande junto,
por **RFCOMM/SPP**, e não por GATT. Por isso a abstração deste projeto é
`DeviceTransport` e **não** "camada BLE": trocar de transporte não obriga a reescrever
nada acima.

Implementações previstas, todas com a mesma interface:

| Classe a criar | Módulo | Quando usar |
|---|---|---|
| `UsbMidiTransport` | `:device:usbmidi` | CODE aceita MIDI por USB — caminho mais estável e sem engenharia reversa |
| `RfcommTransport` | `:device:rfcomm` | Bluetooth Classic, se o Gateway usar SPP |
| `BleGattTransport` | `:device:blemidi` | Só se a verificação mostrar que é mesmo BLE |

Verificação sugerida, na ordem:

1. **nRF Connect** — o CODE50 aparece como BLE ou só como dispositivo Classic pareado?
2. **USB OTG** — o Android enumera o amp como dispositivo MIDI? Se sim, comece por aí.
3. Só em último caso, captura de **HCI snoop log** do app oficial.

Enquanto nada disso estiver pronto, `savePreset()` do `BaseMidiDeviceService` falha de
forma **explícita** em vez de fingir que funcionou — o preset é salvo só no aplicativo,
e a mensagem diz isso.

---

## 8. Como trocar os MockServices pelos serviços reais

Este é o ponto do projeto que foi desenhado para essa troca doer o mínimo possível.

**Um arquivo só:** `app/src/main/java/com/srlakes/tone/di/AppContainer.kt`.

```kotlin
// Fase 1 - simulado
private val tankG: TankGService by lazy { MockTankGService(appScope) }
private val code50: MarshallCodeService by lazy { MockMarshallCodeService(appScope) }

// Fase 2 - real
private val tankG: TankGService by lazy {
    BleMidiTankGService(appScope, BleMidiTransport(applicationContext))
}
private val code50: MarshallCodeService by lazy {
    UsbMidiMarshallCodeService(appScope, UsbMidiTransport(applicationContext))
}
```

Nada mais muda. Nenhuma tela, nenhum ViewModel, nenhum repositório importa
`:device:mock` ou saberia dizer se está falando com um aparelho real. Todos conhecem
apenas as interfaces de `:device:api` e o `DeviceManager`.

Passo a passo:

1. Crie o módulo novo (`:device:blemidi`, por exemplo) e registre em `settings.gradle.kts`.
2. Implemente `DeviceTransport` — só cinco funções: `scan`, `connect`, `disconnect`,
   `send`, e os dois `Flow` de entrada (`incoming`, `log`).
3. Estenda `BaseMidiDeviceService` como no exemplo da seção 6.
4. Troque as duas linhas no `AppContainer`.
5. Adicione a dependência no `app/build.gradle.kts`.

Para rodar simulado e real lado a lado durante o desenvolvimento, o modelo
`AppSettings` já tem o campo `useMockDevices`.

---

## 9. Integração com o SR Lakes Studio

O **SR Lakes Studio** (o aplicativo do notebook que conduz o show) já roda
um servidor Socket.IO na porta **7575** e transmite o estado ao vivo para os
celulares da banda. O SR Lakes Tone entra nessa mesma rede como **receptor** —
o mesmo papel do celular que mostra letra e cifra.

**Trocou de música no notebook, o timbre daquela música vem junto**: macros,
equalização e os efeitos (reverb, delay, boost, gate). É o preset completo da
cena BASE da música.

### O que o Studio transmite

O transmissor publica o evento `state`, e o servidor repassa aos receptores:

```json
{"songId":2,"partIdx":0,"beat":1,"rep":1,"bar":1,
 "playing":true,"elapsedSec":12.4,"bpm":65,
 "activeChord":"D","activeLyric":"Não fala nada"}
```

O que o SR Lakes Tone usa é o **`songId`**. Ele não publica nada — só escuta.
O transmissor continua sendo o notebook, sempre.

### Seguir as seções da música

O Studio guarda as seções **na timeline**, na trilha `tr-section`, com blocos
`{start, length, type, content}` medidos em compassos. É o que aparece na faixa
"Secoes" da tela dele: INTRODUÇÃO, VERSO, PRÉ REFRÃO, REFRÃO...

O evento `state` já transmite `timelinePos`. Com isso o SR Lakes Tone resolve o
bloco ativo sozinho, **sem nenhuma mudança no Studio**, usando exatamente a
mesma regra de lá:

```
start <= pos < start + length
```

O `type` do bloco vira uma seção daqui: `intro`, `verso`, `prerefrao`, `refrao`,
`ponte`, `solo`, `final` → INTRO, VERSO, PRÉ-REFRÃO, REFRÃO, PONTE, SOLO, OUTRO.
Tipo desconhecido **não muda nada** — errar a cena no palco é pior do que não
mudar.

> **Blocos sobrepostos.** No acervo real existe sobreposição: em "SUA MANEIRA"
> um bloco vai do compasso 2 ao 6.025 e outro começa no 6. O Studio resolve com
> `clips.find(...)`, que devolve o **primeiro da ordem do array** — então o app
> preserva a ordem do JSON em vez de ordenar por posição. Concordar com o que
> está escrito na tela do notebook vale mais do que uma ordem bonita. Há teste
> para isso.

### A trilha Controladora

A ideia: uma trilha nova no Studio onde você desenha o timbre junto com a
música. O boost sobe no compasso que você quiser, não só na virada de seção.

Do lado do Studio custa pouco, porque o resolvedor de lá já é genérico pelo
`kind` — a Controladora é só mais um `kind`, com a mesma estrutura de blocos e
a mesma interface de arrastar:

```json
{
  "id": "tr-control",
  "kind": "control",
  "name": "Controladora",
  "clips": [
    { "start": 22, "length": 8, "type": "scene", "content": "SOLO",
      "control": { "scene": "SOLO", "boost": true, "delay": true } },

    { "start": 12, "length": 2, "type": "effect", "content": "BOOST",
      "control": { "scene": "BASE", "boost": true, "volume": 8 } }
  ]
}
```

| Campo | O que é |
|---|---|
| `start`, `length` | Em compassos, como nas outras trilhas. |
| `content` | O rótulo que aparece escrito no bloco na tela. |
| `control.scene` | Nome da cena, do tipo de cena (`CLEAN`/`BASE`/`SOLO`) ou de um preset. Casado sem acento e sem caixa. |
| `control.boost`, `delay`, `reverb`, `gate`, `modulation` | `true` liga, `false` desliga. **Só o que você escrever vira comando** — o resto fica como o preset manda. |
| `control.volume` | Volume absoluto de 0 a 10, na mesma escala dos knobs do app. Opcional. |

**Repare no que NÃO está aí: gain, bass, mid, treble.** O bloco diz *o quê*
("aqui é SOLO, com boost"); o SR Lakes Tone continua dono do *quanto*. Uma
fonte de verdade só para os valores — e eles precisam ser regulados com o
analisador ligado, ouvindo o Marshall, coisa que só existe deste lado. Se você
trocar de pedal um dia, mexe nos presets num lugar só e a timeline continua
valendo.

**Tolerância proposital**, porque o Studio ainda está sendo feito: um bloco
**sem** o objeto `control` vale pelo rótulo. Um bloco escrito só `SOLO` já
funciona.

### Quem ganha quando os dois existem

A Controladora tem prioridade sobre a trilha de seções — ela é o que você
desenhou de propósito para o timbre. A ordem de resolução de um bloco é:

1. o nome bate com uma **cena** desta música;
2. bate com um **tipo de cena** (CLEAN / BASE / SOLO);
3. o `type` do bloco bate com uma **seção** desta música;
4. o nome bate com um **preset** solto.

Não achou nada? Não mexe no timbre, e avisa na tela.

### Como ligar

1. **Dispositivos → SR Lakes Studio → LIGAR SINCRONIA**.
2. O app procura o notebook por mDNS. Se aparecer, toque nele. Se o Wi-Fi da
   casa de show isolar os aparelhos (acontece), digite o IP: `192.168.0.10`.
3. Escolha o que seguir: **TROCA DE MÚSICA** e **SEÇÕES / CONTROLADORA**.
4. **SINCRONIZAR MÚSICAS**: lê `GET /api/songs` e casa os títulos sozinho,
   ignorando acento, caixa e pontuação — `SÓ POR MEU PRAZER` encontra
   `Só por Meu Prazer`. O que sobrar aparece em âmbar para você associar na mão.
   Cada música mostra quantas seções tem e se já existe trilha Controladora.

### A regra de segurança de palco

Na tela Performance existe uma faixa que responde a uma pergunta só: **quem
está comandando o timbre agora?**

| Estado | O que significa |
|---|---|
| `SEGUINDO` (verde) | O notebook manda. A faixa mostra o que está tocando: `Só Por Meu Prazer - REFRÃO`. |
| `MANUAL` (âmbar) | **Você mexeu com a mão** — o app soltou o piloto automático até a próxima música. |
| `MÚSICA NÃO ASSOCIADA` (vermelho) | O Studio trocou para uma música que ninguém casou ainda. |
| `AGUARDANDO` (azul) | Ligado, mas o Studio ainda não falou. |
| `DESLIGADO` | Sincronia desligada. É o padrão. |

O estado `MANUAL` existe porque mexer num knob no meio de uma música e o app
desfazer isso um segundo depois seria pior do que não ter sincronia nenhuma.
Um toque em **VOLTAR A SEGUIR AGORA** devolve o comando sem esperar.

Isso **não** contradiz a regra do analisador. A regra é: *a análise nunca
altera timbre*. E não altera — não existe nenhuma chamada de `AnalyzerViewModel`
para `DeviceManager`. Aqui quem decide continua sendo gente; a mão é a de quem
está no notebook.

### Onde fica o código

```
:sync   StageSyncClient       cliente Socket.IO (identifica-se como "rx")
        StudioApi             GET /api/health e /api/songs
        StageDiscovery        mDNS (_http._tcp, nome "Sr Lakes Stage Sync")
:app    StageSyncCoordinator  descoberta, conexão e songId -> música daqui
        PerformanceViewModel  o ÚNICO lugar onde o timbre muda de fato
```

A separação é deliberada: o coordenador nunca aplica preset. Existe um lugar
só onde o timbre muda, e dá para auditar.

### Rodando o teste de integração

Há um teste que conecta num Studio de verdade e prova o protocolo — não só
que compila:

```bash
cd "<pasta do SR Lakes Studio>/apps/desktop" && PORT=7599 node server.js
```

Em outro terminal, na pasta do SR Lakes Tone:

```bash
./gradlew :sync:test
```

Ele sobe um cliente receptor, um segundo cliente fazendo o papel do
transmissor, e confere que a troca de música atravessa a rede e vira um
`StageState`. Também lê a biblioteca de verdade e valida que todo bloco de
seção do acervo resolve para ele mesmo — foi assim que a sobreposição de
"SUA MANEIRA" apareceu. Sem servidor no ar, os dois testes são **pulados** —
a máquina de outra pessoa não tem o notebook da banda na rede.

O entendimento do formato da timeline tem 9 testes que **não** precisam de
servidor (`TimelineParsingTest`), montados sobre a forma real do `songs.json`.

### Limitações desta integração

- **A trilha Controladora ainda não existe no Studio.** O app já sabe lê-la;
  falta criá-la lá. Enquanto isso ele segue a trilha de seções, que já existe.
- **Nem toda música tem as seções desenhadas.** "ABERTURA-NA MORAL" hoje está
  sem trilha de seções — nela o app segue só a troca de música.
- **O Studio ainda está em desenvolvimento.** Por isso a ponte é tolerante:
  fica **desligada por padrão**, e sem o Studio na rede o SR Lakes Tone
  funciona exatamente como antes.
- **Rede local sem HTTPS.** O Studio serve HTTP puro numa rede fechada. O
  `network_security_config.xml` libera texto claro, e esse é o único tráfego
  de rede do aplicativo — análise, presets e histórico continuam locais.
- **mDNS falha em algumas redes** com isolamento de cliente. Por isso o campo
  de IP manual não é um plano B envergonhado: em palco costuma ser o principal.

---

## 10. Limitações honestas

Coisas que o app **não** consegue fazer, e que ele diz na cara em vez de esconder:

**O microfone do celular não é calibrado.** Todos os números são **dBFS**, não dB SPL.
Só as *diferenças* entre medições feitas na mesma posição têm significado. A tela do
analisador repete isso no rodapé.

**O Android processa o microfone.** O app tenta abrir a captura na ordem
`UNPROCESSED` → `VOICE_RECOGNITION` → `MIC`. Se o aparelho não oferecer `UNPROCESSED`,
o Android aplica ganho automático e redução de ruído, o que estraga medições de nível.
Nesse caso o app mostra um aviso âmbar — ele não finge que os valores estão certos.

**Em ensaio com a banda inteira tocando, o microfone capta a banda, não a sua guitarra.**
A bateria domina graves e agudos. Os Tone Insights só fazem sentido com você tocando
sozinho — passagem de som, ou uma pausa do ensaio.

**Mover o celular invalida a comparação.** 30 cm já mudam a resposta. Por isso o app
compara os pisos de ruído entre duas capturas e avisa quando eles divergem demais.

**USB DIRECT é melhor esforço.** Depende do Android enumerar o aparelho como USB Audio
Class; alguns fabricantes bloqueiam. E com o cabo ocupado o celular não carrega — para
um show inteiro, use um hub OTG com alimentação. Um driver UAC próprio está fora de
escopo.

**Os mapeamentos de CC dos aparelhos simulados são arbitrários.** Estão lá só para o
simulador ter o que registrar no log. Eles não representam o protocolo real, e por isso
têm `learned = false`. A tela Dispositivos avisa: *"Nenhum mapeamento verificado por
você ainda"*.

---

## 11. O que vem na Fase 2

Em ordem de valor prático:

1. **Verificar os dois aparelhos** com o nRF Connect (10 minutos) — isso decide toda a
   arquitetura de transporte.
2. **BLE MIDI para o TANK-G**, se ele for padrão. Provavelmente controle real sem
   engenharia reversa nenhuma.
3. **Protocol Lab com MIDI Learn e CC Sweep.** Transforma a engenharia reversa em
   recurso do app: você mexe no knob, o app aprende. O protocolo deixa de ser
   bloqueante. A base já está escrita (`MidiLearnSession`, `DeviceProfile.withMapping`,
   persistência no `DeviceRepository`) — falta a tela.
4. **Footswitch BLE MIDI** para avançar seção e música com o pé. Com o celular no
   pedestal e as duas mãos na guitarra, você não vai tocar na tela durante a música.
   É pouco código: só receber MIDI. (A sincronia com o Studio já resolve boa parte
   disso para a troca de MÚSICA; o footswitch resolve a troca de SEÇÃO.)
   
5. **USB MIDI** para o CODE50.
6. **RFCOMM / protocolo nativo do CODE50** — a parte de fato incerta, e a única.
   Fica isolada num módulo, sem bloquear nada.

Depois disso: USB DIRECT audio, rotina de calibração com 3 segundos de silêncio,
exportação do histórico, e o `ToneAdvisor` com IA — que já é uma interface
(`:core:analysis/ToneAdvisor.kt`) com uma implementação offline por regras. Trocar
é uma linha, e a análise continua funcionando sem internet de qualquer jeito.

---

## 12. Atualização online e como publicar uma release

O app não está em nenhuma loja. Sem isso, atualizar significa gerar um APK,
mandar por WhatsApp e cada músico instalar na mão — o motivo deste módulo existir.

### Como funciona, do lado de quem usa

Em **Configurações → Atualizações**: o app confere sozinho a cada 6 horas
(desligável), ou na hora, tocando no ícone de atualizar. Quando há uma versão
nova:

1. Aparece o número da versão e as notas de lançamento (o texto da release).
2. **BAIXAR E INSTALAR** baixa o APK direto do GitHub Release, com barra de
   progresso.
3. O Android abre a tela de instalação — **essa confirmação é sempre do
   sistema**, o app nunca instala nada sozinho.
4. Na primeira vez, o Android pode pedir para autorizar "Instalar apps
   desconhecidos" para o SR Lakes Tone. O app detecta isso e mostra o botão
   certo em vez de falhar calado.

**AGORA NÃO** dispensa aquela versão especificamente — ela não volta a
incomodar sozinha, mas continua disponível se você tocar em verificar de novo.

### Por que o repositório precisa ser público

O atualizador chama `GET /repos/{owner}/{repo}/releases/latest` **sem
autenticação nenhuma**. É proposital: o app roda no celular de cada músico, e
não existe forma segura de embutir um token pessoal do GitHub dentro de
um APK — qualquer um consegue descompilar e extrair. Um repositório privado
exigiria isso, então ele precisa ser público para a atualização funcionar sem
gambiarra. Não há nada sensível no código (conferido antes da primeira
publicação: sem chave, sem senha, sem dado de ninguém).

### Onde fica o código

```
:update   VersionComparator      compara "1.2.0" com "v1.1.0-mvp" etc.
          GithubReleaseChecker   le GET /releases/latest, acha o .apk anexado
          ApkDownloader          baixa com progresso, escreve em .part e so
                                 renomeia no final (nunca sobra apk pela metade)
          UpdateInstaller        FileProvider + Intent de instalacao do Android
          UpdateSettingsStore    3 preferencias, em SharedPreferences proprio
                                 (nao entra em AppSettings - isto nao e um
                                 conceito de guitarra, e um utilitario)
:app      UpdateCoordinator      liga tudo: quando verificar, o que fazer com
          (di/../update/)        o resultado, e guarda o estado para a tela
```

`:update` não depende de nenhum outro módulo do projeto — é o único pensado
para, um dia, virar uma biblioteca reaproveitável fora deste app.

### Assinatura de release — o detalhe que faz a atualização funcionar de verdade

O Android só deixa instalar um APK **por cima** de um já instalado se os dois
tiverem a **mesma assinatura**. Sem uma chave de release estável, cada
"atualização" seria na prática desinstalar + reinstalar — perdendo presets,
músicas e histórico salvos no aparelho.

Por isso existe `keystore/srlakes-release.jks` (fora do git, veja
`.gitignore`) e `keystore.properties` na raiz do projeto, lido por
`app/build.gradle.kts`:

```properties
storeFile=keystore/srlakes-release.jks
storePassword=...
keyAlias=srlakes-tone
keyPassword=...
```

Sem esse arquivo, `assembleRelease` ainda compila — só que sem assinatura de
release, incapaz de atualizar um app já instalado.

> **Faça backup do `.jks` e do `keystore.properties` em um lugar que não seja
> só este computador.** Perder os dois significa perder, para sempre, a
> capacidade de atualizar qualquer celular que já tenha uma versão assinada
> com essa chave instalada.

### Como publicar uma nova versão

1. Suba `versionCode` e `versionName` em `app/build.gradle.kts`.
2. `./gradlew :app:assembleRelease` — o APK sai assinado em
   `app/build/outputs/apk/release/app-release.apk`.
3. `git push` das mudanças.
4. Crie uma release no GitHub com a tag `vX.Y.Z` (o `v` é removido
   automaticamente na comparação de versão) e anexe esse APK.

O app em campo confere a próxima vez que abrir (ou na hora, se alguém tocar em
verificar) e encontra a versão nova sozinho — nenhum celular precisa saber que
existe uma versão nova antes disso.
