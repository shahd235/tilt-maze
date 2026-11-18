/*
  maze_node  (maze board, final program)
  Purpose : read every sensor on the maze board and report it to the laptop
            over the XBee, and open the trapdoor when the laptop says DROP.
  Board   : LilyPad Arduino 328 (3.3 V, 8 MHz)

  Wiring  : MPU-6050 tilt sensor   VCC +, GND -, SDA A4, SCL A5
            Light sensor module    (removed from the project, see LIGHT_ENABLED)
            IR sensor module 1     OUT D2, VCC +, GND -      (checkpoint 1)
            IR sensor module 2     OUT D3, VCC +, GND -      (checkpoint 2)
            Reed switch module     D0 D4, + +, G -           (goal)
            Servo (trapdoor)       signal D6, red +/battery, brown -
            XBee carrier           rx pad D7, tx pad D8, power + and -

  Sends   : READY   at power-up
            TILT,x,y   about 5 times a second, whole degrees -45 to 45
            CP1 / CP2  when the ball passes IR module 1 / 2
            GOAL       when the ball (with a magnet) reaches the reed switch
            DARK       only if LIGHT_ENABLED is true (the light sensor was removed)
            DROPPED    after the trapdoor has opened and closed
  Receives: DROP      open the trapdoor now
  Format  : every message is one line of text ending with a newline, 9600 baud
            (see docs/messages.md).

  Use     : hold the board level while it starts. The first second is used to
            learn what "flat" reads.
*/
#include <Wire.h>
#include <SoftwareSerial.h>
#include <Servo.h>

// ---------------------------------------------------------------- pins
const int IR1_PIN   = 2;
const int IR2_PIN   = 3;
const int REED_PIN  = 4;
const int SERVO_PIN = 6;
const int LIGHT_PIN = A0;

SoftwareSerial xbee(8, 7);          // RX = pin 8, TX = pin 7 (the carrier's rx/tx labels are crossed)

// ------------------------------------------------------------ settings
const int MPU_ADDR   = 0x68;        // I2C address of the MPU-6050
const int SAMPLES    = 20;          // readings used to find "flat"
const int FLIP_X     = -1;          // the sensor is mounted turned round:
const int FLIP_Y     = -1;          // use 1 for normal, -1 for reversed
const int DARK_LIMIT = 200;         // light sensor is higher when covered (not used now)
const bool LIGHT_ENABLED = false;   // light sensor removed: its readings were not reliable
const int IR_DETECTED   = LOW;      // IR modules give 0 when they see the ball
const int REED_DETECTED = HIGH;     // this reed module gives 1 when the magnet is near

const unsigned long TILT_EVERY_MS = 200;   // 5 TILT messages per second
const unsigned long LOCKOUT_MS    = 500;   // ignore a sensor for a moment after it fires

// Trapdoor (continuous servo): copy the values found with test 06.
const int STOP_VALUE = 90;          // servo value where it does not move
const int SPEED      = 40;          // how far from stop to turn
const int OPEN_MS    = 400;         // how long the door moves
const int HOLD_MS    = 1000;        // how long the door stays open

// --------------------------------------------------------------- state
float offsetX = 0, offsetY = 0;     // what "flat" reads, in degrees
unsigned long lastTilt = 0;
unsigned long lastCp1 = 0, lastCp2 = 0, lastGoal = 0;
int  lastIr1 = HIGH, lastIr2 = HIGH, lastReed = LOW;
bool wasDark = false;

char lineBuf[16];                   // the command being received
byte lineLen = 0;

Servo trap;

// ----------------------------------------------------------------- MPU
// Read one 16-bit number (two bytes, high byte first) from the MPU.
int16_t readWord() {
  int16_t high = Wire.read();
  int16_t low  = Wire.read();
  return (high << 8) | low;
}

// Read the accelerometer and turn it into two tilt angles in degrees.
bool readTilt(float &tiltX, float &tiltY) {
  Wire.beginTransmission(MPU_ADDR);
  Wire.write(0x3B);                         // first accelerometer register
  if (Wire.endTransmission(false) != 0) return false;
  Wire.requestFrom(MPU_ADDR, 6);
  if (Wire.available() < 6) return false;

  float ax = readWord();
  float ay = readWord();
  float az = readWord();
  tiltX = atan2(ay, az) * 180.0 / PI;                         // roll
  tiltY = atan2(-ax, sqrt(ay * ay + az * az)) * 180.0 / PI;   // pitch
  return true;
}

// Wake the MPU-6050 (it starts asleep) and learn what "flat" reads.
void startMpu() {
  Wire.begin();
  Wire.beginTransmission(MPU_ADDR);
  Wire.write(0x6B);
  Wire.write(0);
  Wire.endTransmission(true);

  float sumX = 0, sumY = 0, x, y;
  int good = 0;
  for (int i = 0; i < SAMPLES; i++) {
    if (readTilt(x, y)) { sumX += x; sumY += y; good++; }
    delay(20);
  }
  if (good > 0) { offsetX = sumX / good; offsetY = sumY / good; }
}

// ------------------------------------------------------------ messages
// Send one line to the laptop (and show it on the USB monitor).
void send(const char *text) {
  xbee.println(text);
  Serial.println(text);
}

void sendTilt() {
  float x, y;
  if (!readTilt(x, y)) return;              // sensor did not answer
  int tx = constrain((int)(FLIP_X * (x - offsetX)), -45, 45);
  int ty = constrain((int)(FLIP_Y * (y - offsetY)), -45, 45);
  char buf[20];
  snprintf(buf, sizeof(buf), "TILT,%d,%d", tx, ty);
  send(buf);
}

// ------------------------------------------------------------ trapdoor
// Open the trapdoor, hold it open, close it and report DROPPED.
// The servo is attached only while it moves: the Servo library uses a timer
// that can disturb SoftwareSerial, so it is switched off the rest of the time.
void dropTrapdoor() {
  trap.attach(SERVO_PIN);
  trap.write(STOP_VALUE + SPEED);           // turn one way: door opens
  delay(OPEN_MS);
  trap.write(STOP_VALUE);
  delay(HOLD_MS);
  trap.write(STOP_VALUE - SPEED);           // turn back: door closes
  delay(OPEN_MS);
  trap.write(STOP_VALUE);
  trap.detach();
  send("DROPPED");
}

// Act on one complete line from the laptop. Unknown lines are ignored.
void handleLine(const char *s) {
  if (strcmp(s, "DROP") == 0) dropTrapdoor();
}

// Collect characters from the XBee into lines.
void readXbee() {
  while (xbee.available()) {
    char c = xbee.read();
    if (c == '\r' || c == '\n') {           // end of a line
      if (lineLen > 0) {
        lineBuf[lineLen] = 0;
        handleLine(lineBuf);
        lineLen = 0;
      }
    } else if (lineLen < sizeof(lineBuf) - 1) {
      lineBuf[lineLen++] = c;
    }
  }
}

// -------------------------------------------------------------- sensors
// Send a message when a digital sensor changes to "detected".
// Each sensor is ignored for LOCKOUT_MS afterwards so one pass sends one message.
void checkDigitalSensors() {
  unsigned long now = millis();

  int ir1 = digitalRead(IR1_PIN);
  if (ir1 == IR_DETECTED && lastIr1 != IR_DETECTED && now - lastCp1 > LOCKOUT_MS) {
    send("CP1");
    lastCp1 = now;
  }
  lastIr1 = ir1;

  int ir2 = digitalRead(IR2_PIN);
  if (ir2 == IR_DETECTED && lastIr2 != IR_DETECTED && now - lastCp2 > LOCKOUT_MS) {
    send("CP2");
    lastCp2 = now;
  }
  lastIr2 = ir2;

  int reed = digitalRead(REED_PIN);
  if (reed == REED_DETECTED && lastReed != REED_DETECTED && now - lastGoal > LOCKOUT_MS) {
    send("GOAL");
    lastGoal = now;
  }
  lastReed = reed;
}

// Send DARK once each time the ball enters the dark zone.
void checkLight() {
  bool dark = (analogRead(LIGHT_PIN) > DARK_LIMIT);
  if (dark && !wasDark) send("DARK");
  wasDark = dark;
}

// ---------------------------------------------------------------- main
void setup() {
  Serial.begin(9600);
  xbee.begin(9600);
  pinMode(IR1_PIN, INPUT);
  pinMode(IR2_PIN, INPUT);
  pinMode(REED_PIN, INPUT);

  startMpu();
  lastIr1  = digitalRead(IR1_PIN);          // start from what the sensors read now,
  lastIr2  = digitalRead(IR2_PIN);          // so nothing is sent at power-up
  lastReed = digitalRead(REED_PIN);
  if (LIGHT_ENABLED) wasDark = (analogRead(LIGHT_PIN) > DARK_LIMIT);
  send("READY");
}

void loop() {
  readXbee();                               // commands from the laptop
  checkDigitalSensors();                    // CP1, CP2, GOAL
  if (LIGHT_ENABLED) checkLight();          // DARK (off: sensor removed)

  if (millis() - lastTilt >= TILT_EVERY_MS) {
    lastTilt = millis();
    sendTilt();
  }
}

