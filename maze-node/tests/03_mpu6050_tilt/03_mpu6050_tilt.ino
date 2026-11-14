/*
  03_mpu6050_tilt  (maze node, test 3)
  Purpose : read the MPU-6050 and print the tilt of the maze as TILT,x,y
            in whole degrees (-45 to 45), the same format as in
            docs/messages.md.
  Wiring  : MPU-6050 (GY-521 board)   LilyPad
              VCC  ------------------ + (3.3 V)
              GND  ------------------ -
              SDA  ------------------ A4 (SDA)
              SCL  ------------------ A5 (SCL)
  Use     : power it up with the maze board lying FLAT. The first second
            is used to learn what "flat" reads, so it is subtracted later.
  Result  : Serial Monitor (9600) shows TILT,x,y about 5 times a second.
            Tilt right: x up. Tilt left: x down. Tilt forward: y up.
            Tilt back: y down. (FLIP_X / FLIP_Y fix the direction.)
*/
#include <Wire.h>

const int MPU_ADDR = 0x68;        // I2C address (AD0 pin not connected)
const int SAMPLES  = 20;          // readings used to find "flat"

// The MPU is mounted turned round, so both directions are reversed.
// Use 1 for normal and -1 for reversed. Change these if the board is turned.
const int FLIP_X = -1;
const int FLIP_Y = -1;

float offsetX = 0;                // what "flat" reads for x, in degrees
float offsetY = 0;                // what "flat" reads for y, in degrees

// Read one 16-bit number (two bytes, high byte first) from the MPU.
int16_t readWord() {
  int16_t high = Wire.read();
  int16_t low  = Wire.read();
  return (high << 8) | low;
}

// Read the accelerometer and turn it into two tilt angles in degrees.
bool readTilt(float &tiltX, float &tiltY) {
  Wire.beginTransmission(MPU_ADDR);
  Wire.write(0x3B);               // first accelerometer register
  if (Wire.endTransmission(false) != 0) return false;   // no answer
  Wire.requestFrom(MPU_ADDR, 6);  // x, y, z are 2 bytes each
  if (Wire.available() < 6) return false;

  float ax = readWord();
  float ay = readWord();
  float az = readWord();

  tiltX = atan2(ay, az) * 180.0 / PI;                         // roll
  tiltY = atan2(-ax, sqrt(ay * ay + az * az)) * 180.0 / PI;   // pitch
  return true;
}

void setup() {
  Serial.begin(9600);
  Wire.begin();

  // The MPU-6050 starts asleep. Writing 0 to register 0x6B wakes it up.
  Wire.beginTransmission(MPU_ADDR);
  Wire.write(0x6B);
  Wire.write(0);
  if (Wire.endTransmission(true) != 0) {
    Serial.println("MPU-6050 not found: check VCC, GND, SDA, SCL");
  }

  // Learn what "flat" reads by averaging a few readings.
  float sumX = 0, sumY = 0, x, y;
  int good = 0;
  for (int i = 0; i < SAMPLES; i++) {
    if (readTilt(x, y)) { sumX += x; sumY += y; good++; }
    delay(20);
  }
  if (good > 0) { offsetX = sumX / good; offsetY = sumY / good; }
  Serial.println("READY");
}

void loop() {
  float x, y;
  if (readTilt(x, y)) {
    int tx = constrain((int)(FLIP_X * (x - offsetX)), -45, 45);   // keep inside -45..45
    int ty = constrain((int)(FLIP_Y * (y - offsetY)), -45, 45);
    Serial.print("TILT,");
    Serial.print(tx);
    Serial.print(",");
    Serial.println(ty);
  }
  delay(200);                     // about 5 times a second
}
