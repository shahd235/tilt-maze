# Message dictionary

Every message is one line of plain text with no spaces, ends with a newline, and is sent at 9600 baud. Numbers are whole numbers. Unknown or damaged lines are ignored and never crash a program.

| From to | Message | Sent when | Meaning |
|---|---|---|---|
| Maze to laptop | `READY` | Power up | Maze node booted |
| Maze to laptop | `TILT,x,y` | About 5 per second | Tilt in whole degrees, -45 to 45 (example `TILT,-7,12`) |
| Maze to laptop | `CP1` / `CP2` | Ball passes IR 1 / IR 2 | Checkpoint reached |
| Maze to laptop | `GOAL` | Reed switch closes | Ball reached the goal |
| Maze to laptop | `DARK` | Light sensor covered | Ball is in the dark zone |
| Maze to laptop | `DROPPED` | Trapdoor finished | Trapdoor opened and closed |
| Remote to laptop | `RREADY` | Power up | Remote booted |
| Remote to laptop | `BTN` | Button pressed | Blocker wants to open the trapdoor |
| Laptop to maze | `DROP` | Laptop accepts a BTN | Open the trapdoor now |
| Laptop to remote | `LED,GREEN` / `LED,RED` / `LED,BLUE` | State change | Green = trap ready, red = cooldown, blue = idle |
| Laptop to remote | `BUZZ,n` | Game event | Remote buzzer pattern n (1 trap fired, 2 cooldown over, 3 round over, 4 checkpoint reached, 5 goal reached) |

## Pin map (proposed)

Both boards are 3.3 V LilyPads. XBee on D7 (DOUT) and D8 (DIN).

| Maze node | Pin | Remote node | Pin |
|---|---|---|---|
| MPU-6050 SDA / SCL | A4 / A5 | Button (to GND) | D2 |
| Light sensor | A0 | Buzzer | D6 |
| IR 1 / IR 2 | D2 / D3 | RGB LED red / blue / green | D9 / D10 / D11 |
| Reed switch | D4 | | |
| Servo signal | D6 | | |
