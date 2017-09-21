# roboremoteFPV
A remote control application for the Arduino-based robot

See the details in our blogs:
 * http://onerobotstory.blogspot.com/ (English)
 * http://refunpro.blogspot.com/ (Ukrainian)

This program is free software. You can redistribute it and/or modify it under the terms of the [GNU General Public License](http://www.fsf.org/licenses/gpl.html) as published by the [Free Software Foundation](http://www.fsf.org/).

## Installation

 1. Download the distribution archive: [roboremote.zip](roboremote.zip)
 2. Extract to any location
 3. Run roboremote.cmd (in Windows) 

## Remote Control Protocol

Remote control is performed over Wi-Fi connection using simple telnet-like connection. It is also compatible with RoboRemo: http://www.roboremo.com/

| Command From RC | Action                 | Response from the Robot (on success)        |
| ----------------| ---------------------- | ------------------------------------------- |
| MI              | Mode-Idle              | MI                                          |
| MA              | Mode-AI                | MA                                          |
| MS              | Mode-Scenario          | MS                                          |
| MR              | Mode-Remote Control    | MR                                          |
| W               | Move Forward           | -                                           | 
| S               | Move Backward          | -                                           |
| A               | Turn Left              | -                                           |
| D               | Turn Right             | -                                           |
| LF              | Toggle Front Lights    | LF1 or LF0                                  |
| LR              | Toggle Rear Lights     | LR1 or LR0                                  |
| LS              | Toggle Side Lights     | LS1 or LS0                                  |
| R0              | Refresh Distances (with the head turn) | RLxx - left distance, where xx is a distance in centimeters with the leading zeros. RL-- means "not available" |
|                 |                        | RFxx - front distance                          |
|                 |                        | RRxx - right distance                          |
|                 |                        | ROABCD - obstacle detectors state ("1"s or "0"s), <BR> A - left-ahead, B - left-edge, C - right-edge, D - right-ahead |
| R1              | Refresh Distances (without the head turn) | same as above                |
| XAAABBB         | Set the drives speed to AAA (left) and BBB (right). The speed is in range 000 - 511. 000 is full reverse, 511 means full ahead. | - |
| -               | Debug message from the robot | ~message text                             |

All commands should be followed by \n.
 
## Third-party Libraries
### vlcj
Video stream rendering is done by vlcj
http://capricasoftware.co.uk/#/projects/vlcj
1. Install VLC (make sure to match java and VLC platform - 32 or 64 bit)
2. Add the following jars from the vlcj distro
  - vlcj
  - jna 
  - jna-platform
  - slf4j-api  

### JInput
We use JInput library from https://java.net/projects/jinput to work with the joysticks and other gamepads. 

Set-up instructions for Eclipse:
 1. Extract distribution zip to some folder (including native *.dll and *.so).
 2. Open Project Properties-> Java Build Path -> Libraries. "Add External Jars...": add jinput.jar and jinput-test.jar
 3. Open Project Properties-> Java Build Path -> Source. Expand the application source entry and add the library folder at "Native library location".
  
Caveats: 
 1. JInput does not refresh a controllers list as well as does not notify if the controller was plugged-in. So start the app with all the controllers connected.
 2. JInput does not provide any means to work in the systems with two controllers with the same names. The first random controller will be chosen in this case.
 
 
### Loggger
Simple Logging Facade for Java (SLF4J)  https://www.slf4j.org/
Anyway it is required by vlcj

### Icons Set
Icons were derived from here: http://icons.webtoolhub.com/icon-p0s1155-set.aspx

## Action Items and Issues
 1. Implement smoother joystick position translation to the motors speed (MainWindow.generateMotorsCommand()) 
 
## Changes List
### 1.0
Initial version. More details here: http://refunpro.blogspot.com/2017/07/17.html (Ukr)
