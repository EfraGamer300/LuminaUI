# Feature gap: LuminaUI vs UltimateUI

Levantamento feito **apenas pela superfície pública** do plugin de referência: nomes de classes e
pacotes, subcomandos, chaves de `config.yml`, pastas de recursos e dependências declaradas. Nenhuma
linha de código foi lida ou copiada — o objetivo é paridade de funcionalidade, não de implementação.

Referência analisada: 389 classes Java, 15 componentes, `pages/`, `effects/`, pack por versão
(`uui_1_20_4` … `uui_26_3`).
Estado atual do LuminaUI: 42 classes, 1 página de exemplo, sem pack.

Legenda: ❌ ausente · ⚠️ parcial · ✅ equivalente

---

## 1. Runtime de render

| Feature | Referência | LuminaUI |
|---|---|---|
| Cursor client-side (glifo, sensibilidade, modo instantâneo) | `ShaderCursor`, `experimental.instant-cursor` | ❌ |
| HUD ancorado no jogador | `HudAnchors`, `HudAlignment`, `HudLookup`, `HudPositionCalculator` | ❌ |
| Reancoragem e movimento do HUD | `HudReanchoring`, `HudMovement`, `HudFollowMode` | ❌ |
| Dimensionamento do HUD | `HudScaleAnimation`, `HudTransforms`, `HudVectors` | ❌ |
| Guarda de passageiro (veículo/câmera) | `HudPassengerGuard`, `HudPassengersPacketListener` | ❌ |
| Pacotes customizados de HUD | `HudPackets`, `HudEntities` | ❌ |
| Formatação de texto do HUD | `HudTextFormat` | ⚠️ (texto simples com MiniMessage) |
| HUD virtual (por jogador) | `VirtualHuds`, `WorldDisplays` | ⚠️ (sessão por jogador, sem multi-HUD) |
| Bossbar customizada via pack | `BossbarPackBuilder`, `BossbarRenderService`, `BossbarTtf`, `BossbarSnapshotBuilder` | ❌ |
| Máscaras de chat/som da bossbar | `BossbarChatPacketListener`, `BossbarSoundMaskPacketListener`, `BossbarMaskPacketListener` | ❌ |
| UI no mundo | `WorldDisplayRenderer`, `WorldUiViewerEntity`, `WorldUiInteractPacketListener` | ❌ |
| Fix de shaders | `GlslShaderInjector`, `editor.rendering.fix-shaders*` | ❌ |
| Editor de shaders | `experimental.shaders-editor`, `ShaderConfiguration` | ❌ |
| Fontes e glifos próprios | `FontAtlasGlyphs`, `GlyphSet`, `GlyphCollection`, `GlyphRowData`, `FontGlyphCodepoint` | ❌ |
| Atlas de imagens em pack | `UiImageAtlasService`, `ImageAtlasBuilder`, `ImageCropTask`, `UiImageLookup` | ❌ |
| Cabeça/skin do jogador como elemento | `PlayerHeadFontService`, `bedrock.player-head` | ❌ |
| Scroll de conteúdo | `ScrollViewState`, `ScrollViewSession`, `TemplateScroll`, `TemplateScrollWindow` | ❌ |
| Animações e efeitos | `AnimationEasingFunctions`, `effects/transform.yml`, `effects/scale.yml` | ❌ |
| Easing/interpolação de transform | `TransformData`, `HudScaleAnimation` | ⚠️ (interpolação fixa de 2 ticks) |
| Paleta de cores nomeada | `UiColorPalette` | ❌ |
| Render em lote | `RenderComponentBatch`, `DisplayEntityData` | ⚠️ (diff por nó, sem batch) |

## 2. Input

| Feature | Referência | LuminaUI |
|---|---|---|
| Clique em hitbox | `Interaction` | ✅ |
| Hover (enter/exit) | `UltimateUIHoverEvent`, `HoverEffectTarget`, `HoverElement` | ❌ |
| Drag | `UltimateUIDragEvent`, `EvtUltimateUiDrag` | ❌ |
| Cursor virtual (posição, movimento) | `UltimateUICursorMoveEvent`, `ApiPointer` | ❌ |
| Keybinds | `KeybindService`, `KeybindPacketListener`, `UltimateUIKeyEvent` | ❌ |
| Entrada de texto por Anvil | `AnvilInputService`, `AnvilInputSession`, `AnvilInputRequest` | ❌ |
| Filtros e motivos de fechamento | `InputFilter`, `InputCloseReason`, `InputSubmit` | ❌ |
| NPC de cursor | `CursorNpcService`, `CursorNpcPacketListener`, `NpcPluginType` | ❌ |
| Cancelar self-interact | `CancelSelfInteractPacketListener` | ❌ |
| Interceptar uso de item | `ClientUseItemPacketListener` | ❌ |
| Máscara de câmera/equipamento/som | `CameraEquipmentMask`, `CameraSoundMask`, `AnvilScreenMask` | ❌ |
| Modo espectador de câmera | `CameraSpectate`, `CameraSessions`, `CameraPlacement` | ❌ |

## 3. Scripting

Subsistema inteiro ausente no LuminaUI (existe apenas a interface `UiAction`/`UiCondition`).

**Engine de script (AST própria)**

- Statements: `if`, `while`, `for`, `for` contado, `foreach`, `wait`, `return`, `jump`, comando,
  atribuição, atribuição composta, expressão.
- Expressões: literal, variável, binária, unária, lógica, ternária, acesso a índice, lista, mapa,
  interpolação de string, chamada de método, acesso a propriedade, PlaceholderAPI, identificador
  especial.
- Estruturas de suporte: `CompiledScript`, `ScriptExecutionContext`, `ScriptScopeContext`,
  `ScriptFunctionDef`, `ScriptEventHandlerDef`, `ScriptErrorEntry`, `ScopeRestoreExecutionStep`.
- Debug e erros: `ScriptSyntaxException`, `editor.debug-scripting`, `ScriptCompilationResult`.

**Bridge Skript**

- Condições: `CondIsUiOpen`, `CondIsUiHoveringOrDragging`, `CondIsUiElementVisibleOrExists`.
- Efeitos: `EffOpenUi`, `EffCloseUi`, `EffOpenUiInput`, `EffRunUiAction`, `EffSetUiElement`,
  `EffUiElementVisibility`, `EffUiElementHierarchy`.
- Eventos: `EvtUltimateUiBlockClick`, `EvtUltimateUiDrag`, `EvtUltimateUiHover`.
- Expressões: cursor (X/Y/coordenadas), evento (bloco, layer, chave, página, tipo, input, drag),
  propriedade/estrutura de elemento, ids de elementos/páginas, estado do jogador.

## 4. Editor

| Feature | Referência | LuminaUI |
|---|---|---|
| Editor no mundo (não inventário) | `service/gui/editor/**` (~40 classes) | ❌ |
| Navbar | `editor_navbar_item.yml` | ❌ |
| Sidebar de layers | `editor_layer.yml`, `editor_activepage.yml` | ❌ |
| Sidebar de ferramentas | `editor_tool.yml` | ❌ |
| Arrastar e redimensionar elementos | `EditorImageDragPart`, `GuiBoundingBox`, `DoubleBounds` | ❌ |
| Popup de salvar | `editor_save_popup.yml` | ❌ |
| Popup de boas-vindas | `editor_welcome_popup.yml` | ❌ |
| Popup de preferências | `editor_preferences_popup.yml`, `EditorPagePreferencesPart` | ❌ |
| Popup de gerenciar arquivos | `editor_manage_file_popup.yml`, `EditorRuntimeFilePart` | ❌ |
| Popup de informação | `editor_info_popup.yml`, `EditorPopupState` | ❌ |
| Seletor de cor | `ColorPickerCoords`, `ColorPickerResolver` | ❌ |
| Dropdown de valores | `editor_dropdown_value.yml` | ❌ |
| Editor de animação de elemento | `editor_animation_element.yml` | ❌ |
| Edição de arquivo cru | `EditorRawWriterPart` | ❌ |
| Criação de página pelo editor | `EditorPopupPagePart` | ❌ |
| Undo/redo | `editor.history.undo-limit` | ✅ |
| Autosave | `EditorAutosave`, `editor.autosave` | ✅ |
| Sons de interface | `editor.sounds` | ❌ |
| Mostrar NPC no editor | `editor.show-npc` | ❌ |
| Mostrar arquivos do editor | `editor.show-editor-files` | ❌ |
| Limite de elementos | `editor.max-page-elements` | ✅ |
| Placeholders resolvidos no editor | `editor.placeholders.resolve-in-editor`, `text-refresh` | ❌ |
| Preview com zoom | `screen.preview.defaultZoom` | ❌ |

## 5. Templates e composição

| Feature | Referência | LuminaUI |
|---|---|---|
| Componentes reutilizáveis | `components/` (15 arquivos) | ❌ |
| Parâmetros de componente | `TemplateParams`, `params:` | ❌ |
| Loops em template | `TemplateLoops`, `TemplateListLayout` | ❌ |
| Condicionais em template | `TemplateCases` | ❌ |
| Expressões em template | `TemplateExpressions` | ❌ |
| Template de layout | `TemplateLayout`, `TemplateBase` | ❌ |
| Templates aninhados | `TemplateComponents` | ❌ |
| Resolver de template | `GuiTemplateResolver`, `GuiServiceEditorShellSupport` | ❌ |
| Pasta de defaults do editor | `editor-defaults/pages`, `editor-defaults/components` | ⚠️ (pasta criada, sem uso) |

## 6. Resource pack

| Feature | Referência | LuminaUI |
|---|---|---|
| Geração de pack | `ResourcePackDelivery`, `PackFileUtil`, `PackSourceFile` | ❌ |
| Servidor HTTP embutido | `ResourcePackHttpServer` (Range, teto de tamanho) | ❌ |
| Pack por versão do cliente | `pack/uui_1_20_4` … `uui_26_3` | ❌ |
| Aplicar ao entrar | `resource-pack.apply-on-join` | ❌ |
| Expulsar ao recusar/falhar | `kick-on-decline`, `kick-on-fail` | ❌ |
| Compressão de imagens | `compress-images` | ❌ |
| Remover hotbar padrão | `remove-default-hotbar` | ❌ |
| Auto-hospedagem | `hosting.self-host` (IP/porta) | ❌ |
| Hospedagem externa | `hosting.external-host.url` | ❌ |
| Pack de plugins externos | `hosting.external-pack` (ItemsAdder, Nexo, Oraxen) | ❌ |
| Sincronizar com setups Xqedii | `hosting.xqedii-setups` | ❌ |
| Mensagens de carregamento (chat/title/sound/prompt) | `resource-pack.messages.*` | ❌ |
| Atlas de imagens gerado | `AtlasGenerationResult`, `ImageAtlasBuilder` | ❌ |
| Glifos de UI no pack | `FontAtlasGlyphs`, codepoints | ❌ |
| Ícones do editor no pack | `pack/**/editor/icons`, `layout` | ❌ |

## 7. Bedrock

| Feature | Referência | LuminaUI |
|---|---|---|
| Export de página para form | `BedrockFormHandler`, `BedrockUiService` | ✅ |
| Canvas de referência (1920×1080) | `bedrock.canvas`, `reference-ui` | ⚠️ (config, sem uso no export) |
| Input inline | `bedrock.input.inline` | ❌ |
| Manter form aberto | `bedrock.stay-open` | ❌ |
| Auto-build | `bedrock.auto-build` | ⚠️ (manual via comando) |
| Auto-instalar no Geyser | `auto-install-to-geyser` | ✅ |
| Pasta de packs do Geyser | `geyser-packs-folder` | ✅ |
| Cabeça/skin do jogador | `bedrock.player-head`, `player-skin`, probe de URL | ❌ |
| Export de texturas do pack | `BedrockPackTextureExporter`, `BedrockExportContext` | ❌ |
| Escala de texto por modo | `bedrock.text.scale-multiplier`, `java-font-size`, `block-glyph-height` | ❌ |
| Listagem de páginas Bedrock | `/uui bedrock pages` | ❌ |

## 8. Integrações

| Integração | Referência | LuminaUI |
|---|---|---|
| PlaceholderAPI | `PlaceholderApiExpressionNode`, refresh configurável | ❌ (softdepend declarada) |
| Skript | `UltimateUiSkriptBridge` + 18 elementos | ❌ (softdepend declarada) |
| Geyser / Floodgate | detecção e pasta de packs | ⚠️ (parcial) |
| ItemsAdder / Nexo / Oraxen | `CustomItemPlugin`, `CustomItemHookResult` | ❌ |
| CraftEngine | `CustomItemPlugin` | ❌ |
| BetterHud | `experimental.betterhud-support` | ❌ |
| Citizens / FancyNpcs | `NpcPluginType`, `CursorNpcService` | ❌ |

## 9. API pública

| Feature | Referência | LuminaUI |
|---|---|---|
| API de registro de UI | `UltimateUIAPI`, `UiBuilder` | ⚠️ (`LuminaApi` básica) |
| Criação/escrita de elemento | `UiElementCreate`, `UiElementWrite` | ❌ |
| Sessão por jogador exposta | `UiSession`, `PlayerGuiSession` | ❌ |
| Manipulação de árvore de elementos | `ApiTree`, `ApiElements` | ❌ |
| Evento de abertura | `UltimateUIOpenEvent`, `UltimateUIOpenedEvent` | ❌ |
| Evento de fechamento | `UltimateUICloseEvent` | ⚠️ (`PAGE_CLOSE` interno) |
| Evento de input submit | `UltimateUIInputSubmitEvent` | ❌ |
| Evento de tecla | `UltimateUIKeyEvent` | ❌ |
| Evento de clique em bloco | `UltimateUIBlockClickEvent` | ❌ |
| Evento de cursor/drag/hover | `UltimateUICursorMoveEvent`, `UltimateUIDragEvent`, `UltimateUIHoverEvent` | ❌ |

## 10. Comandos

| Subcomando | Referência | LuminaUI |
|---|---|---|
| `open <page>` | ✅ | ✅ |
| `close` | ✅ | ✅ |
| `create <page>` | ✅ | ✅ |
| `delete <page>` | ✅ | ✅ |
| `edit <page>` | ✅ | ✅ |
| `list` | ✅ | ✅ |
| `reload` | ✅ | ✅ |
| `menu` | ✅ | ❌ |
| `sensitivity <0.1-5>` | ✅ | ❌ |
| `shaders [on\|off\|toggle\|reset] [player]` | ✅ | ❌ |
| `cursor [on\|off\|toggle\|reset] [player]` | ✅ | ❌ |
| `world place\|list\|remove\|movehere\|move\|rotate\|lookat\|interactive\|perplayer\|size\|mode\|toggle\|tp` | ✅ | ❌ |
| `bedrock build\|pages\|status` | ✅ | ⚠️ (`build`, `status`) |

## 11. Persistência e estado

| Feature | Referência | LuminaUI |
|---|---|---|
| Dados por jogador | `PlayerSessions`, `player_data/`, `userdata/` | ⚠️ (pastas criadas) |
| Preferências do editor por jogador | `EditorPagePreferencesPart` | ❌ |
| Sincronização de tempo de servidor | `ClockNetworkSnapshot`, `ServerTimeUpdatePacketListener` | ❌ |
| Snapshot de bossbar | `BossbarSnapshotBuilder`, `BossbarDisplayState` | ❌ |
| Cache de YAML | `CachedYamlEntry` | ⚠️ (registro em memória) |
| Migração de layout legado | `migrateLegacyResourcePackLayout` | ❌ |

---

## Prioridade sugerida

Ordem pensada para desbloquear uso real, não para copiar a árvore de features:

1. **Cursor + hover/drag** — sem isso `open` é uma página estática de leitura.
2. **Template engine + componentes** — sem isso toda página é artesanal e o editor não escala.
3. **Resource pack + atlas de imagens** — sem isso `ImageNode` não tem fonte de textura.
4. **HUD ancorado no jogador** — o caso de uso principal (placar, hud de corrida).
5. **Scripting (ações/condições)** — depois que 1–4 estabilizarem, para não escrever duas vezes.
6. **Editor in-world** — só faz sentido depois de templates e atlas existirem.
7. **Bedrock: stay-open + input inline** — melhoria incremental sobre o export atual.
8. **Integrações** (PAPI, Skript, itens custom) — barato depois da API pública estabilizar.

Itens deliberadamente fora de escopo por ora: `fix-shaders`/`shaders-editor` (dependem de pack
próprio e de injeção de shader por versão), bossbar customizada (requer pipeline de TTF), câmera
(requer mascaramento de pacotes).
