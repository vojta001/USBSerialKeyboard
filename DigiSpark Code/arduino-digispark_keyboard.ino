#include <DigiKeyboard.h>
#include <SoftSerial_INT0.h>

#define P_RX 2
#define P_TX 1

SoftSerial usbModule(P_RX, P_TX); // RX, TX

void setup() {

  usbModule.begin(9600);
  usbModule.println("ready");

}

void loop() {

  static char input;
  if ((millis() % 3000) == 0) {
    usbModule.println("ready");
    DigiKeyboard.delay(1);
  }

  if (usbModule.available()) {
    input = usbModule.read();
    DigiKeyboard.delay(10);
    usbModule.print(input);
    DigiKeyboard.print(input);
  }
  DigiKeyboard.update();
}




