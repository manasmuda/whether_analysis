
#include <DHT.h> 
#include "MQ135.h"

#include <ESP8266WiFi.h>
 
String apiKey = "69PDNHCUT6EMYEV0";    

const char *ssid =  "Shamit";    
const char *pass =  "1111444411";
const char* server = "api.thingspeak.com";

#define DHTPIN 0     
 
DHT dht(DHTPIN, DHT11);

#define PIN_MQ135 A0
MQ135 mq135_sensor = MQ135(PIN_MQ135);


WiFiClient client;
 
void setup() 
{
       Serial.begin(115200);
       delay(10);
       dht.begin();
 
       Serial.println("Connecting to ");
       Serial.println(ssid);
 
 
       WiFi.begin(ssid, pass);
 
      while (WiFi.status() != WL_CONNECTED) 
     {
            delay(500);
            Serial.print(".");
     }
      Serial.println("");
      Serial.println("WiFi connected");
 
}
 
void loop() 
{
  
      float h = dht.readHumidity();
      float t = dht.readTemperature();
      
      if (isnan(h) || isnan(t)) 
       {
          Serial.println("Failed to read from DHT sensor!");
          return;
       }
      float rzero = mq135_sensor.getRZero();
      float correctedRZero = mq135_sensor.getCorrectedRZero(t,h);
      float resistance = mq135_sensor.getResistance();
      float ppm = mq135_sensor.getPPM();
      float correctedPPM = mq135_sensor.getCorrectedPPM(t,h);
      float rs=50-10*correctedPPM;
      float co2ppm=(146.15*(2.868-rs/76.63)+10);
      float co2percentage=co2ppm/100;
        if (client.connect(server,80))   
        {  
                            
          String postStr = apiKey;
          postStr +="&field1=";
           postStr += String(t);
           postStr +="&field2=";
           postStr += String(h);
           postStr +="&field3=";
           postStr += String(co2percentage);
           postStr += "\r\n\r\n";
 
          client.print("POST /update HTTP/1.1\n");
          client.print("Host: api.thingspeak.com\n");
          client.print("Connection: close\n");
          client.print("X-THINGSPEAKAPIKEY: "+apiKey+"\n");
          client.print("Content-Type: application/x-www-form-urlencoded\n");
          client.print("Content-Length: ");
          client.print(postStr.length());
          client.print("\n\n");
          client.print(postStr);
 
          Serial.print("Temperature: ");
          Serial.print(t);
          Serial.print(" degrees Celcius, Humidity: ");
          Serial.print(h);
          Serial.print("Corrected co2:");
          Serial.print(co2percentage); 
          Serial.println("%. Send to Thingspeak.");
                        }
          client.stop();
 
          Serial.println("Waiting...");
  
  delay( 1000);
}
