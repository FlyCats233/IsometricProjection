# Isometric Projection

[简体中文](README.md) · English

Isometric Projection adds an orthographic isometric camera to **Minecraft 1.21.1 on NeoForge 21.1.265**. The current test version is **0.6.0-beta.2**.

## Installation

Download the mod JAR from [GitHub Releases](https://github.com/FlyCats233/IsometricProjection/releases) and put it in your NeoForge instance's `mods` folder. The camera, controls, and local cutaway work with a client-only installation. Illusion connections work in single-player and require the same mod version on the client and the server in multiplayer.

## Controls

| Default binding | Action |
| --- | --- |
| `I` | Toggle the isometric view |
| `[` / `]` | Rotate the camera by 90° with a smooth transition |
| `-` / `=` | Smoothly zoom out or in |
| `W` / `A` / `S` / `D` | Move toward the top, left, bottom, or right of the screen |
| `V` | Toggle the local cutaway |
| `G` | Toggle illusion connections when the server supports them |
| Left / right mouse button | Attack or interact with the target under the cursor within reach |

You can rebind the controls in Minecraft's Controls menu. The player faces the direction of movement and keeps that facing when you release the keys. The free mouse cursor targets blocks and entities. Vanilla reach and obstruction checks still apply.

A short chat guide appears after joining a world. The HUD above the hotbar shows the current bindings and states for the camera, illusion connections, and cutaway.

## Local cutaway

The cutaway hides blocks at or above the player's eye-level block layer when they fall within a circular opening projected toward the camera. The default opening radius is 4 blocks. Glass and doors follow the same rule. The mouse ray skips hidden blocks so you can target visible objects indoors, while collision and interaction checks still use the actual world.

## Illusion connections

With the isometric view active, press `G` to enable illusion connections. Walk toward the edge of a full block whose top appears to meet another full-block top in the current view. The server validates the connection and moves you to the matching edge. The mode temporarily disables the cutaway and pauses connection detection during camera rotation.

For a quick test, keep the camera at its default 45° angle. Place block A at `(0, 63, 0)` and block B at `(4, 66, -3)`. Stand on A and walk toward its east edge. Leave `(1, 63, 0)` empty and keep the space above both blocks clear. The feature currently requires full-cube collision blocks and a clear landing. It does not trigger while jumping, flying, or riding.

## Settings and languages

Open **Mods → Isometric Projection → Config** to adjust the cutaway, default zoom, zoom step, camera transition durations, status HUD, and join guide. NeoForge stores client preferences in `config/isometricprojection-client.toml`.

The in-game interface follows Minecraft's language setting and supports English (US) and Simplified Chinese (China).

## Build and compatibility

Use JDK 21 and run `gradlew.bat build`. The JAR is written to `build/libs/`. The build runs `verifyControls` to check screen-relative movement and mouse-ray calculations. Use `gradlew.bat runClient` or `gradlew.bat runServer` for development.

This is a beta for Minecraft 1.21.1 on NeoForge. Compatibility with other mods, including shader and rendering optimization mods, has yet to be tested. Ports to other Minecraft versions may follow.

See the [changelog (Chinese)](CHANGELOG.md) and [versioning guide (Chinese)](VERSIONING.md) for release details.

## License

Original project code is licensed under [GNU GPL v3.0 only](LICENSE). Copyright (C) 2026 IsometricProjection contributors. Third-party components such as the Gradle Wrapper retain their own licenses.
