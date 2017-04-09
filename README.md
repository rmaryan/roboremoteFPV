# roboremote
A remote control application for the Arduino-based robot

Video stream rendering is done by vlcj
http://capricasoftware.co.uk/#/projects/vlcj
1. Install VLC (make sure to match java and VLC platform - 32 or 64 bit)
2. Add the following jars from the vlcj distro
  - vlcj
  - jna 
  - jna-platform
  - slf4j-api  


 We use JInput library from https://java.net/projects/jinput to work with the joysticks and other gamepads. 

 Set-up instructions for Eclipse:
 1. Extract distribution zip to some folder (including native *.dll and *.so).
 2. Open Project Properties-> Java Build Path -> Libraries. "Add External Jars...": add jinput.jar and jinput-test.jar
 3. Open Project Properties-> Java Build Path -> Source. Expand the application source entry and add the library folder at "Native library location".
  
 Caveats: 
 1. JInput does not refresh a controllers list as well as does not notify if the controller was plugged-in. So start the app with all the controllers connected.
 2. JInput does not provide any means to work in the systems with two controllers with the same names. The first random controller will be chosen in this case.
 
 
 Loggger
 Simple Logging Facade for Java (SLF4J)  https://www.slf4j.org/
 Anyway it is required by vlcj