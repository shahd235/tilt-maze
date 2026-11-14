/*
  05_ir_and_reed  (maze node, test 5)
  Purpose : check the two IR sensors (checkpoints) and the reed switch (goal).
  Wiring  : IR 1  OUT -> D2    IR 2  OUT -> D3    reed module D0 -> D4
            every module: VCC -> + (3.3 V), GND -> -
  Use     : hold the steel ball about 1 cm in front of IR 1, then IR 2.
            Bring the magnet near the reed switch.
            Turn the small blue screw on each module until it triggers at
            the distance you want. The LED on the module lights up when it
            detects something.
  Result  : the Serial Monitor (9600) prints a line every time a sensor
            changes.
            Measured on our modules:
              IR modules : 0 (LOW)  = ball detected
              reed module: 1 (HIGH) = magnet near
*/

const int IR1_PIN  = 2;
const int IR2_PIN  = 3;
const int REED_PIN = 4;

const int IR_DETECTED   = LOW;    // IR modules give 0 when they see the ball
const int REED_DETECTED = HIGH;   // this reed module gives 1 when the magnet is near

int lastIr1  = -1;                // -1 means "not read yet"
int lastIr2  = -1;
int lastReed = -1;

void setup() {
  Serial.begin(9600);
  pinMode(IR1_PIN,  INPUT);       // the modules drive the pin themselves
  pinMode(IR2_PIN,  INPUT);
  pinMode(REED_PIN, INPUT);
  Serial.println("READY");
}

// Print a message when a pin changes. Returns the new value.
int check(int pin, int last, const char *name, int detectedValue) {
  int now = digitalRead(pin);
  if (now != last) {
    Serial.print(name);
    Serial.print(" = ");
    Serial.print(now);
    Serial.println(now == detectedValue ? "  (detected)" : "  (clear)");
  }
  return now;
}

void loop() {
  lastIr1  = check(IR1_PIN,  lastIr1,  "IR1",  IR_DETECTED);
  lastIr2  = check(IR2_PIN,  lastIr2,  "IR2",  IR_DETECTED);
  lastReed = check(REED_PIN, lastReed, "REED", REED_DETECTED);
  delay(20);
}
