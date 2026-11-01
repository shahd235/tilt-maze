# Smart Tilt-Maze with Remote Sabotage

COE 421L final project. A Runner tilts a maze to roll a ball to the goal. A Blocker uses a wireless remote to open a trapdoor under the ball. A Java program on the laptop runs the game and talks to both boards over ZigBee (XBee).

## Who owns what

| Folder | Owner | What is inside |
|---|---|---|
| `maze-node/` | Shahd | Arduino code for the maze board (tilt, IR, reed, light sensors, trapdoor) |
| `remote-node/` | Abdulla | Arduino code for the Blocker's remote (button, LED, buzzer) |
| `network/` | Joud | XBee settings, range and message test results |
| `java-game/` | Abdelhameed | The Java game program and the class diagram |
| `docs/` | Everyone | The message dictionary and report material |

## Rules

- Commit your own work under your own account, after every working step, with a clear message (for example `IR sensor reads checkpoint`).
- Do not push broken code to `main`.
- All code is commented.
- Messages between the boards and the laptop follow `docs/messages.md`. If a message has to change, tell the group first.
