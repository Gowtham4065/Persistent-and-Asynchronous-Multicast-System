## Programming Projects 3
### Persistent and Asynchronous Multicast System
### About Project
This project is about implementation of persistant, asynchronous and temporal multicast system
#### Persistance
Messages will be stored as a text file for each participant and coordinator is responsible for keeping messages for time td to deliver all participants.
#### Asynchronous
Message sender is not blocked on message sent request, it just resume after getting cordinator's acknowledgement.
#### Temporally-bound persistence
In this model, if a participant is offline more than some specified time threshold (say td), upon reconnecting, it will receive messages that were sent in the past td seconds. In other words, the messages sent before td will be permanently lost to the participant. If a participant is disconnected less than or equal to td seconds, it should not lose any messages. 

**``` Note: Very few functionalities of multi-casting implemented in this project ```**

### About Repository
The repository containing two separate folders for each project and in each project there are sperarate participant and coordinator codes.
### Coding Language
Implementation of code is in java language (v 11.0.10)
### Pre-requiites
1. java environment (open-jdk)

### How to run
#### Run Coordinator
Open terminal in the Sever directory and the enter the command:
```
$ javac Coordinator.java
$ java Coordinator coordinator-conf.txt
```
The server open a socket and ready for connection!
#### Run Participant
Open a terminal on the separate systems in the Client directory and the enter the command:
```
$ javac Participant.java
$ java Participant 1-participant-conf.txt
```
Configuation files are also included in the respository

### Implemented Functionalities
1. Register: Participant has to register with the coordinator specifying its ID, IP address and port number.
    * register [portnumber]
2. Deregister: Participant indicates to the coordinator that it is no longer belongs to the multicast group.
    * deregister
3. Disconnect: Participant indicates to the coordinator that it is temporarily going offline.
    * disconnect
4. Reconnect: Participant indicates to the coordinator that it is online and it will specify the IP address and port number.
    * reconnect [portnumber]
5. Multicast Send: Multicast [message] to all current members. Note that [message] is an alpha-numeric string (e.g., UGACSRocks).
    * msend [message]
 
**All the received messages at participant stores in a text file specified in configuration file**
### Assumptions
1. Configuration files must be available in respective directories and written in standard format as described [here](https://cobweb.cs.uga.edu/~laks/DCS-2016-Sp/pp3/PP3-participant-conf.txt).
2. Not more than 10 participants expected in this system
3. Messages only stores at coordinator when it is live (not stored on external file for persistence)
4. Participant is responsible only allowed to input port number from valid port range.
### HONOR PLEDGE FOR PROJECT
This project was done in its entirety by **Ehsan Latif** and **Subas Rana**. We hereby state that we have not received unauthorized help of any form.
#### Members
1. Ehsan Latif (el44163@uga.edu)
2. Subas Rana (sr07210@uga.edu)
