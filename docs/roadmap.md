# GrainCraft Studio roadmap

## Intent

GrainCraft Studio helps a woodworker design a cutting board and then build it. You pick the woods,
the pattern and the finished size. The app shows what the board will look like, warns about wood
choices that are bad for a cutting board, and works backwards to a shop plan: what lumber to buy,
what to cut, in what order to glue, and how much to allow for saw kerf, trimming and flattening,
given the tools in your shop.

Two things matter most:

- **The preview must truly reflect what's being built.** It doesn't need to be photorealistic, but
  each piece must show the right species, the right grain (end-grain rings or long-grain lines),
  and the effects of how it was built (for example, neighbouring end-grain cells that came from the
  same strip show the same rings, mirrored where a slice was flipped).
- **The build math must be right.** A wrong dimension wastes expensive lumber.

The owner's main platform is a **native Linux desktop app**. Windows desktop, Android and web
follow later from the same code. Syncing between the owner's own devices uses a **Dropbox app
folder**. Sharing between users is out of scope, and nothing is designed for it.

The original idea came from a long spec covering eight pattern families, a 3D viewer, photo and
sketch import, AI prompts, three platforms and Google Drive sync. This roadmap splits that into
phases.

## Roadmap

Each phase ends with a usable app.

| Phase | What it adds |
|---|---|
| **1. Core and Linux desktop** | Wood catalog with cutting-board ratings, units, shop tool profile; stripes (edge grain and end grain), checkerboard and brick (end grain); build math and step-by-step build guide; 2D preview; HTML shop packet; local saving; Material 3 desktop app. Designed below. |
| 2. More patterns | Chevron, basketweave, tumbling cubes, chaotic, pixel grid (with identical-row deduplication); face-grain mode; scrap-bin inventory and nesting strips onto offcuts. |
| 3. 3D viewer | Orbitable 3D board in shared Compose code: pre-generated grain textures per face, edge profiles, juice groove, finger grips, feet, exploded glue-up view, raw / oiled / aged finishes. |
| 4. Photo and sketch import | Load a photo or sketch, crop and 4-corner deskew, ink thresholding, CIELAB colour matching to species, a ghost overlay under the editor. |
| 5. Android | Phone and tablet layouts, the system file picker for import and export, camera capture for phase 4, printing through Android's print system. |
| 6. Dropbox sync | Sync designs, shop profile, custom woods and scrap bin through an app folder (`/Apps/GrainCraft Studio/`) on desktop and Android. Conflicts keep both copies. |
| 7. Web | Kotlin/Wasm build of the same app; IndexedDB storage; Dropbox sync in the browser. |
| 8. Plain-English prompts | Offline rule-based prompt parser, plus optional LLM and vision AI (Gemini, Ollama, OpenAI-compatible) that return a structured design. |
| 9. Windows | Windows installer built on a Windows CI runner. Can slot in any time after phase 1. |

Phase order after phase 1 is a proposal; the owner can reorder.

## Why Dropbox, not Google Drive or Firebase

Google Drive needs different sign-in mechanisms on each platform, short-lived tokens in the
browser, ID-based folder lookups, and a `drive.file` scope that can't see files made elsewhere.
Dropbox uses one OAuth flow with PKCE everywhere, has long-lived refresh tokens, addresses files by
path, and supports "fail if the file changed since revision X" uploads, which is exactly the
conflict check this app needs. Firebase only becomes worthwhile for sharing with other users or
live multi-device editing, and its Kotlin libraries are weakest on desktop. Sync sits behind one
interface, so a different back end can be added later without reworking the app. None of this is
in phase 1.

## Designs and plans

Each phase gets its own design and implementation plan under `docs/phaseN/`. Phase 1:
[phase1/phase-1-design.md](phase1/phase-1-design.md).
