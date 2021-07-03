#include <ESP8266WiFi.h>
#include <PubSubClient.h>
#include <NTPClient.h>
#include <WiFiUdp.h>
#include <ArduinoJson.h>

const char *ssid =  "";
const char *pass =  "";
const char *server = "broker.hivemq.com";
const char *user = "";
const char *foodOutTopic = "";
const char *waterTopic = "";
const char *foodInTopic = "";
const char *foodTimeTopic = "";
const char *foodErrTopic = "";
String curTime = "";
int numTimes = 2;
String* feedingTime;

WiFiClient wclient;
PubSubClient client(wclient);

WiFiUDP ntpUDP;
NTPClient timeClient(ntpUDP);

void setup() {
  Serial.begin(115200);
  client.setServer(server, 1883);
  client.setCallback(callback);
  timeClient.begin();
  timeClient.setTimeOffset(10800);
  feedingTime = new String[numTimes];
  feedingTime[0] = "08:00";
  feedingTime[1] = "18:00";
}

void loop() {
  if (WiFi.status() != WL_CONNECTED) {
    if ((ssid != "") && (pass != "")) {
      connectToWiFi();
    }
    delay(4000);
  }
  else {
    if (!client.connected()) {
      if (user != "") {
        client.connect(user);
        client.subscribe(foodInTopic);
        client.subscribe(foodTimeTopic);
        Serial.println("OK");
      }
      delay(4000);
    }
    else {
      client.loop();
    }

    while (!timeClient.update()) {
      timeClient.forceUpdate();
    }
  }

  if (Serial.available() > 0) {
    String cmd = Serial.readString(), com = "";
    if (strchr(cmd.c_str(), '=')) {
      com = cmd.substring(cmd.indexOf("=") + 1, cmd.length() - 2);
      cmd = cmd.substring(0, cmd.indexOf("="));
    }
    else {
      cmd = cmd.substring(0, cmd.length() - 2);
    }
    setCommand(cmd, com);
  }

  getTime();

  delay(1000);
}

void connectToWiFi() {
  WiFi.begin(ssid, pass);
  unsigned long connectionTime = millis();
  while ((WiFi.status() != WL_CONNECTED) && (true)) {
    delay(400);
    if (millis() - connectionTime > 30000) {
      break;
    }
  }

  if (WiFi.status() != WL_CONNECTED) {
    ssid = "";
    pass = "";
  }
  else {
    Serial.println("OK");
  }
}

void postToFood(String str) {
  StaticJsonDocument<100> jsonData;
  String formattedDate = timeClient.getFormattedDate();
  jsonData["date"] = formattedDate.substring(0, formattedDate.indexOf("T"));
  jsonData["time"] = formattedDate.substring(formattedDate.indexOf("T") + 1, formattedDate.length() - 4);

  jsonData["foodLeft"] = str;

  char lastFeeding[100];
  serializeJsonPretty(jsonData, lastFeeding);

  if (client.connected()) {
    client.publish(foodOutTopic, lastFeeding, true);
  }

  delay(500);
  Serial.println("OK");
}

void callback(char* topic, byte* payload, unsigned int length) {
  String str = "";
  for (int i = 0; i < length; i++) {
    str += (char)payload[i];
  }
  if (((String)topic) == ((String)foodInTopic)) {
    if (str == "Food") {
      Serial.println(str);
    }
  }
  else if (((String)topic) == ((String)foodTimeTopic)) {
    for (int i = 0; i < str.length(); i++) {
      Serial.print("Time=[");
      Serial.print(str);
      Serial.println("]");
      numTimes = 0;
      for (int i = 0; i < str.length(); i++) {
        if (str[i] == 'T') {
          numTimes++;
        }
      }

      feedingTime = new String[numTimes];
      for (int i = 0; i < numTimes; i++) {
        feedingTime[i] = str.substring(i * 5 + i, i * 5 + 5 + i);
      }
    }
  }
}

void setMqttSettings(String com) {
  if (client.connected()) {
    client.disconnect();
  }

  unsigned char* buf = new unsigned char[100];
  com.substring(1, com.length() - 1).getBytes(buf, 100, 0);
  user = (const char*)buf;

  com = user;
  com += "/Food/out";
  buf = new unsigned char[100];
  com.getBytes(buf, 100, 0);
  foodOutTopic = (const char*)buf;

  com = user;
  com += "/Water";
  buf = new unsigned char[100];
  com.getBytes(buf, 100, 0);
  waterTopic = (const char*)buf;

  com = user;
  com += "/Food/in";
  buf = new unsigned char[100];
  com.getBytes(buf, 100, 0);
  foodInTopic = (const char*)buf;

  com = user;
  com += "/Food/Time";
  buf = new unsigned char[100];
  com.getBytes(buf, 100, 0);
  foodTimeTopic = (const char*)buf;

  com = user;
  com += "/Food/Error";
  buf = new unsigned char[100];
  com.getBytes(buf, 100, 0);
  foodErrTopic = (const char*)buf;
}

void postToWater(String com) {
  if (client.connected()) {
    client.publish(waterTopic, com.substring(1, com.length() - 1).c_str(), true);
  }
}

void getTime() {
  if (WiFi.status() != WL_CONNECTED) {
    Serial.println("NoWiFi");
  }
  else if (!client.connected()) {
    Serial.println("NoClient");
  }
  else {
    String formattedDate = timeClient.getFormattedDate();
    formattedDate = formattedDate.substring(formattedDate.indexOf("T") + 1, formattedDate.length() - 4);
    if (formattedDate != curTime) {
      curTime = formattedDate;
      for (int i = 0; i < numTimes; i++) {
        if (feedingTime[i] == curTime) {
          Serial.println("Food");
        }
      }
    }
  }
}

void setWifiSettings(String com) {
  if (WiFi.status() == WL_CONNECTED) {
    WiFi.disconnect();
  }

  int n = 0;
  for (int i = 2; i < com.length(); i++) {
    if ((com[i - 2] == ']') && (com[i - 1] == ',') && (com[i] == '[')) {
      n = i - 1;
    }
  }

  unsigned char* buf = new unsigned char[100];
  com.substring(1, n - 1).getBytes(buf, 100, 0);
  ssid = (const char*)buf;
  Serial.println(ssid);

  buf = new unsigned char[100];
  com.substring(n + 2, com.length() - 1).getBytes(buf, 100, 0);
  pass = (const char*)buf;
  Serial.println(pass);
}

void setCommand(String cmd, String com) {
  if (com == "") {
    if (cmd == "AT") {
      Serial.println("OK");
    }
    else if (cmd == "Time") {
      getTime();
    }
  }
  else {
    if (cmd == "WiFi") {
      setWifiSettings(com);
    }
    else if (cmd == "MQTT") {
      setMqttSettings(com);
    }
    else if (cmd == "Food") {
      if (com != "[Error]") {
        postToFood(com.substring(1, com.length() - 1));
      }
      else {
        if (client.connected()) {
          client.publish(foodErrTopic, com.substring(1, com.length() - 1).c_str(), true);
        }
      }
    }
    else if (cmd == "Water") {
      postToWater(com);
    }
  }
}
