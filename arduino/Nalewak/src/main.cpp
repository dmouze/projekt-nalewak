#include "BluetoothSerial.h"

#if !defined(CONFIG_BT_ENABLED) || !defined(CONFIG_BLUEDROID_ENABLED)
#error Bluetooth is not enabled! Please run `make menuconfig` to enable it
#endif

BluetoothSerial SerialBT;
int received;
char receivedChar;
const int ledPin = 2;
const int signalPin = 4;
const int przekaznikOutput = 16;
bool buttonPressed = false; // Zmienna przechowująca stan przycisku
bool cleanMode = false;

void setup()
{
  Serial.begin(115200);
  SerialBT.begin("LufaNaLezaco");
  pinMode(ledPin, OUTPUT);
  pinMode(signalPin, INPUT);
  pinMode(przekaznikOutput, OUTPUT);
  digitalWrite(przekaznikOutput, LOW);
}

void loop()
{
  if (cleanMode == false)
  {
    int pushButtonState = digitalRead(signalPin);

    if (Serial.available())
    {
      SerialBT.write(Serial.read());
    }

    if (pushButtonState == HIGH && !buttonPressed)
    {
      // Jeśli przycisk został wcześniej zwolniony, to go teraz wciśnięto
      digitalWrite(ledPin, HIGH);
      digitalWrite(przekaznikOutput, HIGH);
      SerialBT.write('a');

      buttonPressed = true;
      Serial.println("Wysłano 'a'");
    }

    if (pushButtonState == LOW && buttonPressed)
    {
      // Jeśli przycisk został wcześniej wciśnięty, to go teraz zwolniono
      digitalWrite(ledPin, LOW);
      digitalWrite(przekaznikOutput, LOW);
      SerialBT.write('b');
      buttonPressed = false;
      Serial.println("Wysłano 'b'");
    }

    
  }
  else
  {
    digitalWrite(ledPin, HIGH);
    digitalWrite(przekaznikOutput, HIGH);

    if (SerialBT.available())
    {
      receivedChar = SerialBT.read();

      if (receivedChar == 'd')
      {
        cleanMode = false;
        digitalWrite(ledPin, LOW);
        digitalWrite(przekaznikOutput, LOW);
      }
    }
  }

  if (SerialBT.available())
  {
    receivedChar = SerialBT.read();

    if (receivedChar == 'b')
    {
      digitalWrite(ledPin, HIGH);
    }

    if (receivedChar == 'a')
    {
      digitalWrite(ledPin, LOW);
    }

    if (receivedChar == 'c')
    {
      cleanMode = true;
    }
  }
}