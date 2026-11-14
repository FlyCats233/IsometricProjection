# Isometric Projection

[简体中文](README.md) · English

Inspired by the game *Monument Valley*, **Isometric Projection** aims to bring gameplay centered on optical illusions to Minecraft. Rotate the isometric view until two separated block tops appear to share an edge, then walk across the apparent connection. Use these connections to build paths and puzzles that depend on your viewing angle.

The current release, **1.0.0**, supports **Minecraft 1.21.1 on NeoForge 21.1.265**.

## Installation

Download the mod JAR from [GitHub Releases](https://github.com/FlyCats233/IsometricProjection/releases) and put it in your NeoForge instance's `mods` folder. The camera, controls, and local cutaway work with a client-only installation. Illusion connections work in single-player and require the same mod version on the client and the server in multiplayer.

On servers without occlusion support, the mod's creative tab and editor search entry are hidden. Giving or dropping the editor from saved creative hotbars is blocked. The entries return in single-player and on supported servers.

Ponder is optional and has its own multiplayer requirements. In testing, Ponder 1.0.82 installed only on the client refused a connection to a NeoForge server without Ponder because its network channels were missing. Configure Ponder on the server as required for that combination, or omit it; this mod's camera features do not require Ponder.

## Controls

| Default binding | Action |
| --- | --- |
| `I` | Toggle the isometric view |
| Hold Mouse 4 | Hide the cursor and move the mouse horizontally to rotate freely. Tap to align |
| `W` / `A` / `S` / `D` | Move toward the top, left, bottom, or right of the screen |
| Left / right mouse button | Attack or interact with the target under the cursor within reach |

You can rebind the controls in Minecraft's Controls menu. The player faces the direction of movement and keeps that facing when you release the keys. The free mouse cursor targets blocks and entities. Vanilla reach and obstruction checks still apply.

A short chat guide appears after joining a world. Use the bottom-right sliders to rotate and zoom. Hold Ctrl while dragging the rotation slider to align, or hold Ctrl and scroll to zoom. The panel also provides cutaway, illusion connections, overhead auto-zoom, and More Settings.

## Local cutaway

Cutaway and overhead auto-zoom are off by default. Illusion Connections retains its switch state when entering or leaving isometric view, and only operates in isometric view.

The cutaway hides blocks at or above the player's eye-level block layer when they fall within a circular opening projected toward the camera. The default opening radius is 4 blocks. Glass and doors follow the same rule. The mouse ray skips hidden blocks so you can target visible objects indoors, while collision and interaction checks still use the actual world.

## Illusion connections

With Ponder installed on the client, run `/isometricprojection ponder` in a world or click the join guide to watch a quartz Penrose triangle demonstrate an illusion crossing.

With the isometric view active, click Illusion Connections in the bottom-right panel. Walk toward the edge of a full block whose top appears to meet another full-block top in the current view. The server validates the connection and moves you to the matching edge. The mode temporarily disables the cutaway and pauses connection detection during camera rotation.

For a quick test, keep the camera at its default 45° angle. Place block A at `(0, 63, 0)` and block B at `(4, 66, -3)`. Stand on A and walk toward its east edge. Leave `(1, 63, 0)` empty and keep the space above both blocks clear. The feature currently requires full-cube collision blocks and a clear landing. It does not trigger while jumping, flying, or riding.

## Settings and languages

### Authored occlusion (0.8.0 beta)

In Creative mode, get the Occlusion Tool from the Isometric Projection tab or run `/give @s isometricprojection:occlusion_tool`.
In isometric view, right-click block A, then block B (any face). The blocks must share a projected top edge in at least one of the four standard views. Where their projections overlap, visible faces of B cover A, including top/side overlaps. The overlap updates continuously during fine rotation.
Sneak-right-click either block to remove its rule, or sneak-right-click the air to cancel a selection.
The editor shows a filled orange cube while selecting A, then a filled blue cube while selecting B. Completing or canceling the selection restores the initial appearance.

While holding the editor, nearby valid relations show orange outlines for A, blue outlines for B, and a yellow **A → B** arrow meaning “A is covered by B”. Guides work in first-person and isometric views, and disappear when the tool is put away.

With Ponder installed, hover the editor in your inventory and hold the default W key, or run `/isometricprojection ponder occlusion`. The Penrose triangle tutorial links the two upper pillar blocks to the same covering beam endpoint and explains deleting rules. Quartz blocks and the editor share the Isometric Projection topic and both tutorial chapters. Use page navigation to switch chapters. Quartz blocks and `/isometricprojection ponder` start with crossing; the editor and its command start with occlusion editing.

Rules are saved per dimension and synchronized by the server. Both sides need this version in multiplayer.
The initial implementation supports static, opaque full blocks, with up to 128 rules per dimension and a 64-block distance limit between endpoints.
Players standing on B inherit its precedence over A, retaining that relation while airborne until they land. Armor, held items, and capes use the standard player renderer. Other mobs and moving contraptions still require special compatibility work.

Experimental shader support targets **Iris 1.8.14-beta.1 + Complementary Reimagined r5.9.3**. Occlusion uses the pack's own shading path with an orthographic view-direction correction. Shader pack files and user options are left unchanged. Sodium 0.8.13 also retains its native terrain shading when shaders are disabled. Other packs, the full range of screen-space reflection effects, and complex multiplayer scenes remain unverified.

Open **Mods → Isometric Projection → Config** to adjust the cutaway, default zoom, zoom step, camera transition durations, status HUD, and join guide. NeoForge stores client preferences in `config/isometricprojection-client.toml`.

The in-game interface follows Minecraft's language setting and supports English (US) and Simplified Chinese (China).

## Build and compatibility

Use JDK 21 and run `gradlew.bat build`. The JAR is written to `build/libs/`. The build runs `verifyControls` to check screen-relative movement and mouse-ray calculations. Use `gradlew.bat runClient` or `gradlew.bat runServer` for development.

This release supports Minecraft 1.21.1 on NeoForge. Broader compatibility with other mods, including shaders and rendering optimization mods, has yet to be tested. Ports to other Minecraft versions may follow.

With Create installed, Flywheel's `indirect` backend may cause animated parts and moving contraptions on the far side of the player to disappear in isometric view. Run `/flywheel backend flywheel:instancing` on the client to restore their rendering. This workaround has been verified for copper valve handles, mechanical bearing heads, and moving contraptions.

See the [changelog (Chinese)](CHANGELOG.md) and [versioning guide (Chinese)](VERSIONING.md) for release details.

## License

Original project code is licensed under [GNU GPL v3.0 only](LICENSE). Copyright (C) 2026 IsometricProjection contributors. Third-party components such as the Gradle Wrapper retain their own licenses.
