/*
  07_xbee_link_test  (network test, run on the maze LilyPad)
  Purpose : measure how reliable the XBee link is, in both directions.
  Wiring  : XBee carrier  3.3V -> +    - -> -
                          pad 2 (DOUT) -> pin 7    pad 3 (DIN) -> pin 8
  Use     : the laptop module is in XCTU (Consoles). Click in the console
            log, type one command and press Enter:

              A   maze sends the 6 dictionary messages, 50 times each
                  (READY CP1 CP2 GOAL DARK DROPPED), 5 per second
              B   maze sends TILT,0001,0 ... TILT,0600,0 at 5 per second
                  (2 minutes) and then END
              C   clear the DROP counter
              R   maze reports how many DROP lines it has received
              DROP  (sent 50 times from XCTU) is counted by the maze

  Note    : SoftwareSerial cannot send and receive at the same moment, so
            the tests run one at a time.
*/
#include <SoftwareSerial.h>

SoftwareSerial xbee(7, 8);        // RX = pin 7 (XBee DOUT), TX = pin 8 (XBee DIN)

const char *NAMES[] = {"READY", "CP1", "CP2", "GOAL", "DARK", "DROPPED"};
const int NUM_NAMES = 6;

unsigned int dropCount = 0;       // DROP lines received from the laptop
char lineBuf[16];                 // the line being received
byte lineLen = 0;

// Send each dictionary message 50 times, 5 per second.
void sendDictionary() {
  for (int n = 0; n < NUM_NAMES; n++) {
    for (int i = 0; i < 50; i++) {
      xbee.println(NAMES[n]);
      delay(200);
    }
  }
  xbee.println("END");
}

// Send 600 numbered TILT lines, 5 per second (2 minutes).
void sendTilt() {
  char buf[16];
  for (int i = 1; i <= 600; i++) {
    snprintf(buf, sizeof(buf), "TILT,%04d,0", i);
    xbee.println(buf);
    delay(200);
  }
  xbee.println("END");
}

// Report the DROP counter over the radio and on the USB monitor.
void report() {
  xbee.print("DROPS,");
  xbee.println(dropCount);
  Serial.print("DROPS,");
  Serial.println(dropCount);
}

// Act on one complete line from the laptop.
void handleLine(const char *s) {
  if (strcmp(s, "A") == 0)         sendDictionary();
  else if (strcmp(s, "B") == 0)    sendTilt();
  else if (strcmp(s, "C") == 0)    dropCount = 0;
  else if (strcmp(s, "R") == 0)    report();
  else if (strcmp(s, "DROP") == 0) dropCount++;
}

void setup() {
  Serial.begin(9600);
  xbee.begin(9600);
  Serial.println("LINK TEST READY");
  xbee.println("READY");
}

void loop() {
  while (xbee.available()) {
    char c = xbee.read();
    Serial.write(c);                          // show what arrives on the USB monitor
    if (c == '\r' || c == '\n') {             // end of a line
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
