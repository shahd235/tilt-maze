# java-game

The Java program that runs on the laptop. It reads the messages from the maze and the remote through the XBee on the laptop, runs the game, and sends commands back. All messages follow `../docs/messages.md`.

Originally assigned to Abdelhameed. Taken over by Shahd on Nov 15, 2026. The code was written with AI assistance (see section 7 of the report).

## What it does

| Receives | What the game does |
|---|---|
| `TILT,x,y` | shows the tilt of the maze |
| `CP1`, `CP2` | scores the checkpoint once, sends `BUZZ,4` |
| `GOAL` | the Runner wins, sends `BUZZ,5`, LED blue |
| `DARK` | counts the dark-zone entry |
| `BTN` | if the trap is ready: sends `DROP`, `BUZZ,1`, `LED,RED`, starts the cooldown |
| `DROPPED` | counts the drop, takes `DROP_PENALTY_SECONDS` off the Runner's clock |
| `RREADY` | sends the remote the current LED colour |
| cooldown over | sends `BUZZ,2` and `LED,GREEN` |
| time up | sends `BUZZ,3`, LED blue |

Rules are numbers in `src/tiltmaze/Config.java` (round time 120 s, cooldown 10 s, drop penalty 10 s, points). Change them there.

## Design patterns

- **Observer**: `Game` (subject) notifies every `GameListener` (the window `GamePanel`) when something changes.
- **State**: `Game` is always in one `GameState` (`IdleState`, `RunningState`, `FinishedState`) that decides what each message does.
- `Link` is an interface with two versions: `SerialLink` (real XBee on COM port) and `LoopbackLink` (test mode, no hardware).

Class diagram: `../docs/class_diagram.png` (source: `../docs/class_diagram.mmd`).

## Run on Windows

1. Install a JDK (17 or newer). Check with `javac -version` in Command Prompt.
2. Download `jSerialComm-2.11.0.jar` (Maven Central: com.fazecast / jSerialComm / 2.11.0) and put it in the `lib` folder.
3. Double-click `build.bat` (or run it in Command Prompt inside this folder).
4. Run `test.bat`: it plays a pretend maze and remote against the game and prints PASS/FAIL (needs no hardware). Expect `ALL CHECKS PASSED`.
5. Run `run.bat --test` to try the window without hardware (buttons at the bottom pretend to be the maze and remote).
6. Real run: close XCTU first (it keeps the port busy), then `run.bat COM4` (use your XBee Explorer's port), or `run.bat` and choose the port in the window.

## Real-hardware checklist (not tested yet)

- [ ] Window connects to the XBee port without an error.
- [ ] Maze powered: Maze shows "online" and TILT moves the dot.
- [ ] Start round: remote LED turns green.
- [ ] Ball over IR 1 and IR 2: the remote buzzer plays pattern 4.
- [ ] Remote button: trapdoor opens (`DROP`), LED turns red, buzzer pattern 1.
- [ ] After the cooldown: LED green, buzzer pattern 2.
- [ ] Ball at the goal: buzzer pattern 5, state GOAL REACHED.
- [ ] Let the clock run out: buzzer pattern 3, state TIME UP.
