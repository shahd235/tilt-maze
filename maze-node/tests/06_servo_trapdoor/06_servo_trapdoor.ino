/*
  06_servo_trapdoor  (maze node, test 6)
  Purpose : move the FS90R continuous servo for a set time to open and
            close the trapdoor, and find the "stop" value of your servo.
  Wiring  : servo brown wire  -> - (GND)
            servo red wire    -> + (power)   (use the battery, see below)
            servo yellow wire -> D6 (signal)
  Power   : the servo needs more current than the FTDI can give. If the
            board resets when the servo moves, wait for the battery. Do
            not worry, the reset will not damage anything.
  Use     : type a letter in the Serial Monitor (9600) and press Enter:
              o = open the trapdoor    c = close it    s = stop
              + = raise the stop value    - = lower it
            1) A continuous servo should NOT move at its stop value.
               If it creeps, press + or - until it is still, then copy
               that number into STOP_VALUE below.
            2) Change OPEN_MS until the door opens the right amount.
*/
#include <Servo.h>

const int SERVO_PIN = 6;
int STOP_VALUE      = 90;         // 90 is usually "stop"; trim with + and -
const int SPEED     = 40;         // how far from stop to turn (bigger = faster)
const int OPEN_MS   = 400;        // how long the door moves, in milliseconds

Servo trap;

void stopServo()  { trap.write(STOP_VALUE); }

void openDoor() {
  trap.write(STOP_VALUE + SPEED); // turn one way
  delay(OPEN_MS);
  stopServo();
}

void closeDoor() {
  trap.write(STOP_VALUE - SPEED); // turn the other way for the same time
  delay(OPEN_MS);
  stopServo();
}

void setup() {
  Serial.begin(9600);
  trap.attach(SERVO_PIN);
  stopServo();
  Serial.println("READY  (o = open, c = close, s = stop, + / - = trim)");
}

void loop() {
  if (Serial.available()) {
    char key = Serial.read();
    if (key == 'o') { Serial.println("opening");  openDoor(); }
    if (key == 'c') { Serial.println("closing");  closeDoor(); }
    if (key == 's') { Serial.println("stop");     stopServo(); }
    if (key == '+') { STOP_VALUE++; stopServo(); Serial.print("stop value = "); Serial.println(STOP_VALUE); }
    if (key == '-') { STOP_VALUE--; stopServo(); Serial.print("stop value = "); Serial.println(STOP_VALUE); }
  }
}
