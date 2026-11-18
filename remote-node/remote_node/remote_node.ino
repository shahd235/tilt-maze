/*
  remote_node  (remote board, final program)
  Purpose : the Blocker's remote. A button asks the laptop to open the maze's
            trapdoor; the RGB LED and the buzzer show what the game says.
  Board   : LilyPad Arduino 328 (3.3 V, 8 MHz)

  Wiring  : push button    S D2, middle +, - -          (uses the internal pull-up)
            buzzer         S D6, - -                    (middle pin not connected)
            RGB LED        R D9, B D10, G D11, + to +   (LilyPad tri-colour LED, common anode)
            XBee carrier   rx pad D7, tx pad D8, power + and -
                           (the Arduino receives on D8 and sends on D7)

  Sends   : RREADY   at power-up
            BTN      each time the button is pressed
  Receives: LED,GREEN   trap ready          LED,RED   cooldown      LED,BLUE   idle
            BUZZ,1      trap fired          BUZZ,2    cooldown over
            BUZZ,3      round over          BUZZ,4    checkpoint    BUZZ,5   goal reached
            Any other line (for example DROP, which is for the maze) is ignored.
  Format  : one line of text ending with a newline, 9600 baud (docs/messages.md).
*/
#include <SoftwareSerial.h>

// ---------------------------------------------------------------- pins
const int BUTTON_PIN = 2;
const int BUZZER_PIN = 6;
const int RED_PIN    = 9;
const int BLUE_PIN   = 10;
const int GREEN_PIN  = 11;

SoftwareSerial xbee(8, 7);          // RX = pin 8, TX = pin 7 (the carrier's rx/tx labels are crossed)

// ------------------------------------------------------------ settings
const int LED_ON  = LOW;            // common anode LED: LOW lights a colour
const int LED_OFF = HIGH;
const unsigned long DEBOUNCE_MS = 40;     // ignore button bounce
const unsigned long LOCKOUT_MS  = 300;    // one press sends one BTN

// --------------------------------------------------------------- state
int lastButton = HIGH;              // the button reads LOW when pressed
unsigned long lastChange = 0;
unsigned long lastPress  = 0;

char lineBuf[16];                   // the command being received
byte lineLen = 0;

// ----------------------------------------------------------------- LED
// Show one colour (or all off). Only plain on/off is used, so there is no
// conflict between analogWrite and the buzzer's tone().
void setColour(int red, int green, int blue) {
  digitalWrite(RED_PIN,   red   ? LED_ON : LED_OFF);
  digitalWrite(GREEN_PIN, green ? LED_ON : LED_OFF);
  digitalWrite(BLUE_PIN,  blue  ? LED_ON : LED_OFF);
}

void showLed(const char *name) {
  if (strcmp(name, "GREEN") == 0)      setColour(0, 1, 0);
  else if (strcmp(name, "RED") == 0)   setColour(1, 0, 0);
  else if (strcmp(name, "BLUE") == 0)  setColour(0, 0, 1);
}

// -------------------------------------------------------------- buzzer
// One beep: a tone for ms milliseconds, then a short gap.
void beep(int freq, int ms, int gap) {
  tone(BUZZER_PIN, freq, ms);
  delay(ms + gap);
}

// The five patterns. Each is shorter than about a second.
void playBuzz(int n) {
  switch (n) {
    case 1:                                   // trap fired: one low long beep
      beep(300, 500, 0);
      break;
    case 2:                                   // cooldown over: two short high beeps
      beep(1500, 100, 80);
      beep(1500, 100, 0);
      break;
    case 3:                                   // round over: three falling beeps
      beep(1200, 200, 50);
      beep(900, 200, 50);
      beep(600, 300, 0);
      break;
    case 4:                                   // checkpoint: one short beep
      beep(1000, 120, 0);
      break;
    case 5:                                   // goal reached: rising jingle
      beep(800, 120, 30);
      beep(1000, 120, 30);
      beep(1300, 120, 30);
      beep(1600, 300, 0);
      break;
  }
}

// ------------------------------------------------------------ messages
// Send one line to the laptop (and show it on the USB monitor).
void send(const char *text) {
  xbee.println(text);
  Serial.println(text);
}

// Act on one complete line from the laptop. Unknown lines are ignored.
void handleLine(const char *s) {
  if (strncmp(s, "LED,", 4) == 0) {
    showLed(s + 4);
  } else if (strncmp(s, "BUZZ,", 5) == 0) {
    playBuzz(atoi(s + 5));
  }
}

// Collect characters from the XBee into lines.
void readXbee() {
  while (xbee.available()) {
    char c = xbee.read();
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

// -------------------------------------------------------------- button
// Send BTN once per press (debounced).
void checkButton() {
  int now = digitalRead(BUTTON_PIN);
  unsigned long t = millis();
  if (now != lastButton && t - lastChange > DEBOUNCE_MS) {
    lastChange = t;
    lastButton = now;
    if (now == LOW && t - lastPress > LOCKOUT_MS) {   // pressed
      lastPress = t;
      send("BTN");
    }
  }
}

// ---------------------------------------------------------------- main
void setup() {
  Serial.begin(9600);
  xbee.begin(9600);
  pinMode(BUTTON_PIN, INPUT_PULLUP);
  pinMode(BUZZER_PIN, OUTPUT);
  pinMode(RED_PIN, OUTPUT);
  pinMode(GREEN_PIN, OUTPUT);
  pinMode(BLUE_PIN, OUTPUT);
  setColour(0, 0, 1);                         // blue = idle, until the laptop says otherwise
  send("RREADY");
}

void loop() {
  readXbee();                                 // LED and BUZZ commands
  checkButton();                              // BTN
}
