# Playing Builder's Balloon

## Install

Use Minecraft Java 1.21.1, Java 21 and NeoForge 21.1.252, our tested launch baseline. Metadata permits later NeoForge 21.1 versions; they are not claimed as tested. Install the same main mod JAR in the `mods` folder on each client and the server. The sources JAR is for developers. Create, Sable and the supported rope mods are optional.

## Start building

1. Craft a Balloon Tether or Metal Balloon Tether.
2. Place it on a floor, wall or ceiling. Placement activates the tether for you and attaches the balloon.
3. Double-tap Jump to start flying. Use normal creative-flight movement: movement keys, Jump to rise and Sneak to descend. Your rebound controls apply.
4. Build inside the boundary. Contact with the edge stops outward movement without popping the balloon; mining while flying has no extra penalty from this mod.
5. Land, then press **Pop Balloon** to detach. This key is **unassigned by default**: choose it in Options → Controls → Key Binds under **Builder's Balloon**.

Removing an active tether also releases it. Release removes the flight supplied by this mod while preserving unrelated flight grants; it does not protect you from a subsequent fall.

## Range and switching

The boundary is a **cube**, measured from the anchor centre along each axis. It is not a spherical radius. A default wooden tether permits 16 blocks in each positive/negative axis direction; default metal permits 32. The metal setting can reach 64 blocks in each direction.

Right-click an inactive tether with an **empty main hand** while inside its range to activate it. If you already have a tether, this switches your binding and preserves flight. Switching does not teleport you. A tether occupied by another player cannot be taken over, and each player has one active tether.

Bindings end on removal, death, logout, dimension change or anchor unload. After restarting the world, placed tethers remain available but inactive; right-click to reactivate them. Sable sublevels carry their tether boundary with the moving frame.

## Crafting

Functional tethers use this 3×3 arrangement; an em dash means an empty slot:

| Left | Centre | Right |
| --- | --- | --- |
| — | Wool | — |
| Rope | Body material | Rope |
| — | Iron ingot | — |

| Tether | Body material | Output |
| --- | --- | --- |
| Balloon Tether | Any item in the Minecraft logs tag | 1 |
| Metal Balloon Tether | Iron block | 1 |
| Metal Balloon Tether, extra Create recipe | Industrial Iron Block | 1 |

Wool determines the tether and balloon colour. Each rope slot accepts a vanilla lead, Supplementaries rope, Farmer's Delight rope or Quark rope. The two slots can use different supported items. **Crafting consumes ingredients normally.**

Decorative bollards use a body material directly above an iron ingot. No wool or rope is needed:

| Bollard | Body material | Output |
| --- | --- | --- |
| Bollard | Any item in the Minecraft logs tag | 2 white bollards |
| Metal Bollard | Iron block | 4 white metal bollards |
| Metal Bollard, extra Create recipe | Industrial Iron Block | 2 white metal bollards |

Create recipes are additional options when Create and its Industrial Iron Block are present. Standard recipes remain available.

Craft any one tether or bollard with a dye to recolour it. All four variants have 16 colours. Same-colour items stack to 64; different colours do not combine into one stack. Colours survive ordinary drops, pick-block, saving and Create wrench pickup.

## Decoration and pickup

Click a decorative Bollard or Metal Bollard with a lead or supported rope to toggle its wraps on or off. **Toggling consumes no item.** Both directions play vanilla knot sounds; adjust the Blocks sound slider to control them. Decorative bollards grant no flight and do not leash mobs. Breaking, picking or wrenching a bollard returns a colour-preserving item without its cosmetic rope state.

Tethers appear in the Functional Blocks creative tab; decorative bollards appear in Building Blocks. All four support waterlogging and placement on six faces. With Create installed, sneak-right-click with its wrench to pick up any variant. Picking up an active tether releases its binding.

Wood and metal can be mined by hand and drop their items. Metal takes longer than wood and survives ordinary TNT and creeper explosions.

## Server range settings

The server controls `builders_balloon-server.toml`; client preferences do not override its values.

| Key | Default | Allowed values |
| --- | --- | --- |
| `range` (wood) | 16 | 8–32 |
| `metalRange` | 32 | 8–64 |

NeoForge loads the default file from the instance/server `config` directory and supports per-world overrides in the world's `serverconfig` directory. For singleplayer, the latter is inside `saves/<world>/serverconfig`; for a dedicated server it is inside the chosen world directory. Shut down the world/server before editing, then restart. A binding captures the range when activated, so release and reactivate existing bindings to use a new value. [NeoForge configuration reference](https://docs.neoforged.net/docs/1.21.1/misc/config/).

Pack authors may extend the `builders_balloon:ropes` item tag with additional rope items. Optional entries from absent mods are ignored.

## Beta feedback

Real multiplayer gameplay testing is deferred for this beta. See [known issues](KNOWN-ISSUES.md) for the observed animation-pack conflict and what to include in a bug report. Sable integration does not establish support for every ship or physics mod.
