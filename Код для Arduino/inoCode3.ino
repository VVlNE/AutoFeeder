#include <SoftwareSerial.h>
#include <SPI.h>
#include <SD.h>

#define espTXPin 9
#define espRXPin 10
#define trigPin A4 //trig дальномера
#define echoPin A5 //echo дальномера
#define lsPin A3 //датчик линии
#define feedButPin A2 //кнопка для подачи еды
#define waterButPin A1 //кнопка для подачи воды
#define wls1Pin 2 //датчик воды в кулере
#define wls2Pin 3 //датчик воды в миске
#define mVPin 5 //скорость вращения моторчика
#define mDPin 4 //направление вращения моторчика
#define pVPin 6
#define pDPin 7
#define sdScPin 8

SoftwareSerial espSerial(espTXPin, espRXPin);

const char *ssid = "VVine";
const char *pass = "OaAvNKB28";
const char *user = "MyCat";
double radius = 14; //радиус кулера с едой
double height = 20; //высота кулера с едой
bool dir = 0;
unsigned long lastWaterCheckTime = 0;
int waterCheckTime = 1; //в минутах

void setup() {
  Serial.begin(115200);
  espSerial.begin(115200);
  pinMode(trigPin, OUTPUT);
  pinMode(echoPin, INPUT);
  pinMode(wls1Pin, INPUT);
  pinMode(wls2Pin, INPUT);
  for (int i = 4; i < 8; i++) {
    pinMode(i, OUTPUT);
  }
}

void loop() {
  if (Serial.available()) {
    espSerial.print(Serial.readString());
  }
  else if (espSerial.available()) {
    String str = espSerial.readString();
    setCommand(str);
  }
  else {
    if (millis() < lastWaterCheckTime) {
      lastWaterCheckTime = millis();
    }
    if ((millis() - lastWaterCheckTime > waterCheckTime * 60000) || (!digitalRead(waterButPin))) {
      lastWaterCheckTime = millis();
      supplyWater();
    }
    if (!digitalRead(feedButPin)) {
      pourFeed();
    }
  }
}

String getResponse() {
  unsigned long timeNow = millis();
  while ((!espSerial.available()) && (millis() - timeNow < 30000) && (millis() > timeNow)) {
    delay(100);
  }
  String str = espSerial.readString();
  str = str.substring(0, str.length() - 2);
  return str;
}

double getFeedResidueAmount() {
  double duration, distance;
  digitalWrite(trigPin, LOW);
  delayMicroseconds(10);
  digitalWrite(trigPin, HIGH);
  delay(50);
  digitalWrite(trigPin, LOW);
  duration = pulseIn(echoPin, HIGH);
  distance = duration * 343 * 100 / (1000000 * 2);
  return PI * pow(radius, 2) * (height - distance);
}

void supplyWater() {
  if (digitalRead(wls1Pin)) { //digitalRead(wls2Pin) пока не используется, тк второго датчика нет
    digitalWrite(pDPin, HIGH);
    digitalWrite(pVPin, HIGH);
    delay(5000);
    digitalWrite(pVPin, LOW);
    digitalWrite(pDPin, LOW);
    espSerial.println("Water=[true]");
  }
  else {
    espSerial.println("Water=[false]");
  }
}

void pourFeed() {
  Serial.println("Food");
  digitalWrite(mDPin, dir);
  for (int i = 30; i <= 50; i = i + 2) {
    analogWrite(mVPin, i);
    delay(100);
  }
  delay(1000);
  analogWrite(mVPin, 0);
  dir = !dir;

  if (digitalRead(lsPin)) {
    feedAgain(2);
  }

  if (digitalRead(lsPin)) {
    espSerial.println("Food=[Error]");
  }
  else {
    postToFood();
  }
}

void feedAgain(int n) {
  if (n > 0) {
    for (int i = 0; i < 5; i++) {
      digitalWrite(mVPin, 1);
      digitalWrite(mDPin, dir);
      delay(200);
      dir = !dir;
    }
    digitalWrite(mVPin, 0);
    if (digitalRead(lsPin)) {
      feedAgain(n - 1);
    }
  }
}

void postToFood() {
  String str = "Food=[";
  str += String(getFeedResidueAmount());
  str += "]";
  espSerial.println(str);

  str = getResponse();
  if (str != "OK") {
    Serial.println("Post again");
    postToFood();
  }
  else {
    Serial.println("Posted");
  }
}

void getWiFiSettings() {
  String str = "SdErr11";

  if (SD.begin(sdScPin)) {
    File myFile = SD.open("INITDATA.txt");

    if (myFile.available()) {

      str = "WiFi=[";
      str += myFile.readStringUntil('\n');
      str += "],[";
      str += myFile.readStringUntil('\n');
      str += "]";
      espSerial.println(str);
      while (!espSerial.available()) {
        delay(1000);
      }
      espSerial.readString();
      while (!espSerial.available()) {
        delay(1000);
      }
      espSerial.readString();
    }
    else {
      str = "InitFileErr11";
    }
    myFile.close();
  }
  setCommand(str);
}

void getClientSettings() {
  String str = "SdErr";
  if (SD.begin(sdScPin)) {
    File myFile = SD.open("INITDATA.txt");
    if (myFile.available()) {
      str = "MQTT=[";
      myFile.readStringUntil('\n');
      myFile.readStringUntil('\n');
      str += myFile.readStringUntil('\n');
      str += "]";
      espSerial.println(str);
      while (!espSerial.available()) {
        delay(1000);
      }
      espSerial.readString();
      while (!espSerial.available()) {
        delay(1000);
      }
      espSerial.readString();
      return;
    }
    else {
      str = "InitFileErr";
    }
    myFile.close();
  }
  setCommand(str);
}


void setTime(String newTime) {
  String str = "SdErr";
  String file = "timeData.txt";
  if (SD.begin(sdScPin)) {
    if (SD.exists(file)) {
      SD.remove(file);
    }
    File myFile = SD.open(file, FILE_WRITE);
    if (myFile) {
      Serial.println("1");
      Serial.println(newTime);
      newTime = newTime.substring(newTime.indexOf('[') + 1, newTime.indexOf(']'));
      Serial.println(newTime);
      myFile.println(newTime);
      str = "Completed";
    }
    else {
      str = "TimeFileErr";
    }
    myFile.close();
  }
  Serial.println(str);
}

void addFeedData(String newData) {
  String str = "SdErr";
  String file = "feedData.txt";
  if (SD.begin(sdScPin)) {
    File myFile = SD.open(file, FILE_WRITE);
    if (myFile) {
      myFile.println(newData);
      str = "Added";
    }
    else {
      str = "DataFileErr";
    }
    myFile.close();
  }
  Serial.println(str);
}

void setCommand(String str) {
  str = str.substring(0, str.length() - 2);
  Serial.println(str);

  if (str == "NoWiFi") {
    getWiFiSettings();
  }
  else if (str == "NoClient") {
    getClientSettings();
  }
  else if (str == "Food") {
    pourFeed();
  }
  else if (str.substring(0, 4) == "Time") {
    setTime(str);
  }
  else if ((str.substring(0, 4) == "WiFi") || (str.substring(0, 4) == "MQTT")) {
    espSerial.println(str);
  }
}
