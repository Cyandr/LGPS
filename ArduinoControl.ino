#include <SPI.h>
#include <SPI.h>
#include <Ethernet.h>
#include <EthernetUdp.h>

byte mac[] = {0xDE, 0xAD, 0xBE, 0xEF, 0xFE, 0xED};
EthernetUDP Udp;
IPAddress timeServer(202, 120, 2, 101); // time-a.timefreq.bldrdoc.gov
char timeServerA[] = "time.nist.gov"; // time.nist.gov NTP server
int error = 10;
typedef byte Sequence[36];
Sequence  s2;
int baseTime = 0;
int pastByHour = 0;
byte mHour = 0, mMinute = 0, mSecond = 0;
boolean time_updated = false;
const int timeZone = 8;     // Central Time
unsigned int localPort = 8080;  // local port to listen for UDP packets
unsigned int rport = 8081;
unsigned int timeport = 9000;
unsigned long tLastScan = 0, tLastSecond = 0, tLastMinute = 0, tLastSync = 0, tNow = 0;
byte lTs1, lTs2;
byte the_scan_time = 0, the_scan_second = 0;
const int NTP_PACKET_SIZE = 48; // NTP time is in the first 48 bytes of message
byte packetBuffer[NTP_PACKET_SIZE]; //buffer to hold incoming & outgoing packets
boolean getNtpTime();
boolean getNtpTimeA();
void sendNTPpacket(char* address);
// totally 40 bytes will send to phone, 0-31 represent the data producted by the MSC,33 means the second, 34th means the
//minute and 35th means the hour;
//32 means the error code
//included  NTP_error=89; Serial2_error=87; Ethernet_error=86;  Time_parse_error=86;
byte NTP_error=89,Serial2_error=87,Ethernet_error=86, Time_parse_error=86,Recived_error=85;
int minute_flag=0,fminute_flag=0;
boolean forward=false;
void setup() {
  Serial.begin(9600);
  if (Ethernet.begin(mac)) {
    Udp.begin(timeport);
    IPAddress LoIp = Ethernet.localIP();
    Serial.println("connected to the Internet succeed!______!!");
    Serial.println(LoIp);
  } else
  {
    while (millis() > 20000) {
      Serial.println("can not linked to the Internet!!!");\
      error=Ethernet_error;
    }
  }
  sendNTPpacket(timeServerA);
  //time_updated = getNtpTimeA();
  Serial2.begin(9600);
  for (int i = 0; i <= 35; i++) {
    s2[i] = i + 1;
  }
}


void loop() {

  tNow = millis();
  if (tNow - tLastScan >= 200 || tNow - tLastScan <= 500) {

    Serial2.write(0x7D);
    
    float abstrct_second = (tNow - tLastSecond) / 1000;
    float left_second = left_second + abstrct_second - 1.0;
    if(abstrct_second>=1){mSecond++;minute_flag++;forward=true;}
    
    if(left_second>=1){mSecond++;left_second=0;minute_flag++;forward=true;}
    if(forward){
        forward=false;
        the_scan_time=0;
      Serial.println(tNow - tLastScan);
      Serial.print(mHour);
      Serial.print("-");
      Serial.print(mMinute);
      Serial.print("-");
      Serial.println(mSecond);
      s2[34] = mMinute; s2[32]=error;s2[34]=mHour;s2[33]=mSecond; s2[35] = error;
      if (mSecond>=60) {
        
          sendNTPpacket(timeServerA);
          time_updated=false;
        Ethernet.maintain();
      }
      tLastSecond = tNow;
      the_scan_time = 0;
       Serial.print(">>>");
      for(int x=0;x<=35;x++){
         Serial.print(s2[x]);
          Serial.print(" ");
          }
           Serial.println(" ");
        
    }


  //  int i = 0;
    //while (Serial2.available() > 0 && i <= 7) {
    //while ( i <= 7) {
      //  s2[i + 8 * lTs2] = (byte) Serial2.read();
     // s2[i + 8 * the_scan_time]=i + 8 * the_scan_time;
     // i++;
   // }
   for(int i=0;i<=35;i++){s2[i]=i;}
    //}
    //    byte zero=(byte)Serial2.read();
    the_scan_time++;
    tLastScan = tNow;
    }
    int R_size = Udp.parsePacket();
    if (R_size >= 2 && R_size <= 5) {
      IPAddress remote_ip = Udp.remoteIP();
      int remote_port = Udp.remotePort();
      unsigned char packetBuffer[3];
      Udp.read(packetBuffer, 3);
      Serial.println( "Recieved Terminal Request");
      int m = (packetBuffer[0] == packetBuffer[2]) ? (byte)packetBuffer[0] : error = Recived_error;
      Udp.beginPacket(remote_ip, remote_port); Udp.write(s2, 36); Udp.endPacket();
      Serial.println( "Send LGPS Data");
      
    }
    else if (R_size >= 40) {                          // We've received a packet, read the data from it
      Serial.println("Start waiting NTP Response***:");
      Udp.read(packetBuffer, NTP_PACKET_SIZE); // read the packet into the buffer

      // the timestamp starts at byte 40 of the received packet and is four bytes,
      // or two words, long. First, extract the two words:

      unsigned long highWord = word(packetBuffer[40], packetBuffer[41]);// combine the four bytes (two words) into a long integer
      unsigned long lowWord = word(packetBuffer[42], packetBuffer[43]);
      unsigned long secsSince1900 = highWord << 16 | lowWord; // this is NTP time (seconds since Jan 1 1900):
      Serial.print("Seconds since Jan 1 1900 = ");
      Serial.println(secsSince1900);// now convert NTP time into everyday time:
      Serial.print("time = ");// Unix time starts on Jan 1 1970. In seconds, that's 2208988800:
      const unsigned long seventyYears = 2208988800UL;// subtract seventy years:
      unsigned long epoch = secsSince1900 - seventyYears + 8 * 3600;// print Unix time:
      Serial.println(epoch);
      mHour = (epoch  % 86400L) / 3600;
      mMinute = (epoch % 3600) / 60;
      mSecond = epoch % 60;
      time_updated=true;
    }
}

//void sendNTPpacket(IPAddress &address)
void sendNTPpacket(char* address)
{
  // set all bytes in the buffer to 0
  memset(packetBuffer, 0, NTP_PACKET_SIZE);
  // Initialize values needed to form NTP request
  // (see URL above for details on the packets)
  packetBuffer[0] = 0b11100011;   // LI, Version, Mode
  packetBuffer[1] = 0;     // Stratum, or type of clock
  packetBuffer[2] = 6;     // Polling Interval
  packetBuffer[3] = 0xEC;  // Peer Clock Precision
  // 8 bytes of zero for Root Delay & Root Dispersion
  packetBuffer[12]  = 49;
  packetBuffer[13]  = 0x4E;
  packetBuffer[14]  = 49;
  packetBuffer[15]  = 52;
  // all NTP fields have been given values, now
  // you can send a packet requesting a timestamp:
  Udp.beginPacket(address, 123); //NTP requests are to port 123
  Udp.write(packetBuffer, NTP_PACKET_SIZE);
  Serial.println("the NTP Request had been sent!");
  Udp.endPacket();
}




