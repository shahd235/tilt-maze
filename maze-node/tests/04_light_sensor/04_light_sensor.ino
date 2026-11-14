/*
  04_light_sensor  (maze node, test 4)
  Purpose : read the light sensor and find the number that means "dark".
  Wiring  : light sensor module   LilyPad
              S (signal) -------- A0
              middle pin -------- + (3.3 V)
              - (GND) ----------- -
  Use     : 1) Run it in normal room light and note the numbers.
            2) Cover the sensor with your hand and note the numbers.
            3) Put a number between the two in DARK_LIMIT below.
            If your numbers go UP when covered, change the "<" in loop()
            to ">".
  Result  : prints the raw value, and DARK when the sensor is covered.
*/

const int LIGHT_PIN  = A0;        // analog pin of the light sensor
const int DARK_LIMIT = 200;       // this sensor reads HIGHER when covered (0 uncovered, 445-710 covered)

bool wasDark = false;             // remember the last state, print only changes

void setup() {
  Serial.begin(9600);
  Serial.println("READY");
}

void loop() {
  int value = analogRead(LIGHT_PIN);     // 0 to 1023
  bool dark = (value > DARK_LIMIT);   // inverted: higher = darker

  Serial.print("light = ");
  Serial.println(value);

  if (dark && !wasDark) {
    Serial.println("DARK");              // same word the maze will send
  }
  wasDark = dark;
  delay(200);
}
