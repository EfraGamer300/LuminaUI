# LuminaUI

Declarative UI runtime for Paper and Folia servers. Pages are authored as YAML, rendered
server-side with display entities, and projected into the world as a billboarded grid — so every
player sees the same layout regardless of resolution or GUI scale.

LuminaUI is an original implementation. It is not derived from, and contains no code or assets
from, any third-party UI plugin.

## Requirements

- Java 21
- Paper 1.21.4+ (or Folia)
- Optional: Geyser + Floodgate for the Bedrock export path

## Build

```bash
./gradlew build
```

The jar lands in `build/libs/LuminaUI-<version>.jar`. CI runs the same command on JDK 21 and
publishes the jar as a workflow artifact.

## Data folder layout

Created on first run inside `plugins/LuminaUI/`:

```
config.yml                 plugin settings
lang.yml                   editable messages (seeded from the bundled locale)
contents/
├── pages/                 page definitions, one .yml per page
├── components/            reusable node fragments
├── effects/               animation definitions
├── images/                image sources for image nodes
├── scripts/               trigger/action scripts
└── world/                 world placements
editor-defaults/
├── pages/                 page templates offered by the editor
└── components/            component templates
resourcepack/              generated pack output
bedrock/
├── generated/             exported Bedrock forms
└── pack/                  Bedrock pack files
guis/                      menu layouts
player_data/  userdata/    per-player state
cache/  backups/
```

Bundled locales live in the jar under `locales/`; only the selected one is written to `lang.yml`.
Pages are read from `contents/pages/`.

## Quick start

```
/lumina create hud
/lumina edit hud
/lumina open hud
```

- `/lumina list` — show every page and its element count
- `/lumina create <id>` — write an empty page file
- `/lumina edit <id>` — open the menu editor and a live world preview
- `/lumina open <id> [distance]` — open a page in front of you
- `/lumina close` — close your page
- `/lumina delete <id> confirm` — remove a page and its file
- `/lumina bedrock build [id]` / `status` — export pages as Bedrock forms
- `/lumina reload` — reload config and pages
- `/lumina debug perf` / `state` — runtime counters

## How it works

| Layer | Package | Responsibility |
|---|---|---|
| API | `dev.lumina.ui.api` | Immutable page/node model, style, events, actions |
| Core | `dev.lumina.ui.core` | Layout, hit testing, dirty tracking, validation, settings |
| Render | `dev.lumina.ui.render` | Display entity pool, projection, text/item renderers |
| Input | `dev.lumina.ui.input` | `Interaction` hitboxes per node |
| Session | `dev.lumina.ui.session` | Per-player page sessions and event dispatch |
| Editor | `dev.lumina.ui.editor` | Menu editor, history, debounced autosave |
| Bedrock | `dev.lumina.ui.bedrock` | Page → Bedrock form export, Geyser pack bridge |
| Storage | `dev.lumina.ui.storage` | YAML codec, schema migration, atomic writes |
| Compat | `dev.lumina.ui.compat` | Folia/Paper scheduler bridge |

Design notes:

- **Layout is pure and thread-safe.** No platform types in the layout path, so it runs anywhere and
  is covered by plain unit tests.
- **Only changes are rendered.** A page that does not move produces an empty delta — no packets, no
  entity updates.
- **One timer for deferred work.** Autosave and preview refreshes share a single scheduler task and
  are debounced per player.
- **Folia first.** Region/entity schedulers are used when available, with a Bukkit fallback.

## Page format

```yaml
schema-version: 1
title: 'Example HUD'
scale: 1.0
persistent: false
layers:
  - id: main
    depth: 1
    interactive: true
    nodes:
      - id: heading
        type: text
        bounds: { x: 36, y: 36, width: 200, height: 28 }
        anchor: TOP_LEFT
        visible: true
        text: '<gradient:#7dd3fc:#a78bfa><bold>LuminaUI</bold></gradient>'
        scale: 0.5
        line-width: 200
        style:
          background: 0
          accent: -1
          opacity: 1.0
          shadow: true
          glow: 0
          billboard: CENTER
```

Node types: `text`, `image`, `shape`, `group`. Files are validated on load and saved atomically, so
an interrupted write can never truncate a page.

## Licence

MIT — see [LICENSE](LICENSE).
