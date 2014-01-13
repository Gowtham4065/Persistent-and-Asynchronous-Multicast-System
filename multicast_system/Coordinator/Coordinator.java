import java.io.*;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;
import jdk.nashorn.api.tree.Tree;

class Globals {

  public static final Map<Integer, Socket> participantMap = new HashMap<>();
  public static final List<Integer> activeParticipants = new ArrayList<>();
  public static final Map<Long, String> messageMap = new TreeMap<>();
  public static final Map<Long, List<Integer>> nonMessageRecipients = new TreeMap<>();
}

class connectionThread extends Thread {

  private int port;
  private int timeout;

  connectionThread(int port, int timeout) {
    this.port = port;
    this.timeout = timeout;
  }

  // constructor with port
  @Override
  public void run() {
    // starts server and waits for a connection
    try {
      ServerSocket server = new ServerSocket(port);
      System.out.println("Coordinator started on connectionPort");

      while (true) {
        System.out.println("Waiting at connectionPort for a participant ...");

        Socket socket = server.accept();
        System.out.println("Participant connected to connectionPort");

        // takes input from the client socket
        DataInputStream in = new DataInputStream(
          new BufferedInputStream(socket.getInputStream())
        );
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
        new workerThread(in, out, timeout).start();
      }
    } catch (IOException i) {
      System.out.println(
        "ConnectionThread: Exception is caught" + i.getMessage()
      );
    }
  }
}

class messageSender extends Thread {

  private String message;
  private Integer senderID;

  messageSender(String message, Integer senderID) {
    this.message = message;
    this.senderID = senderID;
  }

  // constructor with port
  @Override
  public void run() {
    // starts server and waits for a connection
    try {
      for (Integer partcipantID : Globals.activeParticipants) {
        if (!partcipantID.equals(senderID)) {
          Socket socket = Globals.participantMap.get(partcipantID);
          // takes input from the client socket
          DataInputStream in = new DataInputStream(
            new BufferedInputStream(socket.getInputStream())
          );
          DataOutputStream out = new DataOutputStream(socket.getOutputStream());
          out.writeUTF(message);
        }
      }
      if (Globals.activeParticipants.size() < Globals.participantMap.size()) {
        Long currentTime = System.currentTimeMillis();
        Globals.messageMap.put(currentTime, message);
        List<Integer> nonRecipients = new ArrayList<>();
        for (Integer partcipantID : Globals.participantMap.keySet()) {
          if (!Globals.activeParticipants.contains(partcipantID)) {
            nonRecipients.add(partcipantID);
          }
          Globals.nonMessageRecipients.put(currentTime, nonRecipients);
        }
      }
    } catch (IOException i) {
      System.out.println("MessageThread: Exception is caught" + i.getMessage());
    }
  }
}

class workerThread extends Thread {

  private int timeout;
  private DataInputStream in = null;
  private DataOutputStream out = null;

  workerThread(DataInputStream in, DataOutputStream out, int timeout) {
    this.timeout = timeout;
    this.in = in;
    this.out = out;
  }

  private void removeOldMessages(long time) {
    for (Long messageTime : Globals.messageMap.keySet()) {
      if ((time - messageTime) / 1000 > timeout) {
        Globals.messageMap.remove(messageTime);
        Globals.nonMessageRecipients.remove(messageTime);
      }
    }
  }

  // constructor with port
  @Override
  public void run() {
    // starts server and waits for a connection
    try {
      String input = "";
      String errorMessage = "Requested participant not registered yet!";
      while (!input.equals("quit")) {
        input = in.readUTF();
        System.out.println("Participant input:" + input.split("#")[0]); //needs to be input.split("#")[0]
        removeOldMessages(System.currentTimeMillis());
        if (input.indexOf("register") == 0) {
          String[] participantInput = input.split("#");
          Integer participantID = Integer.parseInt(participantInput[1]);
          String participantIP = participantInput[2];
          int participantListenPort = Integer.parseInt(participantInput[3]);
          Socket socket = new Socket(participantIP, participantListenPort);
          Globals.participantMap.put(participantID, socket);
          Globals.activeParticipants.add(participantID);
          out.writeUTF("Participant registered");
        } else if (input.contains("deregister")) {
          String[] particpantInput = input.split("#");
          Integer participantID = Integer.parseInt(particpantInput[1]);
          if (Globals.participantMap.containsKey(participantID)) {
            if (Globals.participantMap.get(participantID) != null) {
              Globals.participantMap.get(participantID).close();
              Globals.activeParticipants.remove(participantID);
            }
            Globals.participantMap.remove(participantID);
            out.writeUTF("Participant deregistered");
          } else out.writeUTF(errorMessage);
        } else if (input.contains("reconnect")) {
          String[] participantInput = input.split("#");
          Integer participantID = Integer.parseInt(participantInput[1]);
          if (Globals.participantMap.containsKey(participantID)) {
            if (!Globals.activeParticipants.contains(participantID)) {
              String participantIP = participantInput[2];
              int participantListenPort = Integer.parseInt(participantInput[3]);
              Socket socket = new Socket(participantIP, participantListenPort);
              Globals.participantMap.put(participantID, socket);
              Globals.activeParticipants.add(participantID);
              out.writeUTF("Participant reconnected");
              Map<Long, String> copyMessageMap = new TreeMap<>(
                Globals.messageMap
              );

              for (Long messageTime : copyMessageMap.keySet()) {
                if (
                  Globals.nonMessageRecipients.containsKey(messageTime) &&
                  Globals.nonMessageRecipients
                    .get(messageTime)
                    .contains(participantID)
                ) {
                  // takes input from the client socket
                  DataOutputStream resend = new DataOutputStream(
                    socket.getOutputStream()
                  );
                  resend.writeUTF(Globals.messageMap.get(messageTime));
                  Globals.nonMessageRecipients
                    .get(messageTime)
                    .remove(participantID);
                  if (Globals.nonMessageRecipients.get(messageTime).isEmpty()) {
                    Globals.nonMessageRecipients.remove(messageTime);
                    Globals.messageMap.remove(messageTime);
                  }
                }
              }
              copyMessageMap.clear();
            } else out.writeUTF("Requested Participant is already Connected");
          } else out.writeUTF(errorMessage);
          //old messages sent
        } else if (input.contains("disconnect")) {
          String[] particpantInput = input.split("#");
          Integer participantID = Integer.parseInt(particpantInput[1]);
          if (Globals.participantMap.containsKey(participantID)) {
            Globals.participantMap.get(participantID).close();
            Globals.participantMap.put(participantID, null);
            Globals.activeParticipants.remove(participantID);
            out.writeUTF("Participant disconnected");
          } else out.writeUTF(errorMessage);
        } else if (input.contains("msend")) {
          String[] particpantInput = input.split("#");
          int senderID = Integer.parseInt(particpantInput[2]);
          if (Globals.participantMap.containsKey(senderID)) {
            if (Globals.activeParticipants.contains(senderID)) {
              String message = particpantInput[1];
              new messageSender(message, senderID).start();
              out.writeUTF("Message Acknowledged");
            } else out.writeUTF("Requested Participant is not Connected");
          } else out.writeUTF(errorMessage);
        }
      }
    } catch (IOException i) {
      System.out.println("workerThread: Exception is caught" + i.getMessage());
    }
  }
}

public class Coordinator {

  // constructor with port
  public Coordinator(int port, int timeout) {
    // starts server and waits for a connection
    new connectionThread(port, timeout).start();
  }

  public static void main(String[] args) {
    if (args.length == 1) {
      File file = new File(args[0]);
      if (file.exists() && file.isFile()) {
        try {
          Scanner sc2 = new Scanner(file);

          int coordinatorListenPort = Integer.parseInt(sc2.nextLine());
          int messageTimeout = Integer.parseInt(sc2.nextLine());

          new Coordinator(coordinatorListenPort, messageTimeout);
          sc2.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      } else System.out.println(
        "Configuration file: " +
        args[1] +
        " not found in the respective directory!"
      );
    } else System.out.println(
      "Missing arguments: Expected configuration file name!"
    );
  }
}
