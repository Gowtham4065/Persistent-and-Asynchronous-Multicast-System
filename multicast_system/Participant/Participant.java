import java.io.*;
import java.net.*;
import java.nio.CharBuffer;
import java.text.DecimalFormat;
import java.util.Scanner;
import javax.lang.model.util.ElementScanner6;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.Port;

class workerThread extends Thread {

  private int uniqueID;
  private String coordinatorIP;
  private DataInputStream input = null;
  private DataInputStream coordinatorInput = null;
  private DataOutputStream coordinatorOut = null;
  private String fileName;
  private Socket socket;
  receiverThread nreceiverThread;

  workerThread(int id, String fname, String address, int port) {
    uniqueID = id;
    fileName = fname;
    coordinatorIP = address;

    try {
      socket = new Socket(coordinatorIP, port);
      System.out.println("Connected to port: " + port);

      // takes input from terminal
      input = new DataInputStream(System.in);

      // takes input from the coordinator socket
      coordinatorInput =
        new DataInputStream(new BufferedInputStream(socket.getInputStream()));

      // sends output to the socket
      coordinatorOut = new DataOutputStream(socket.getOutputStream());
    } catch (UnknownHostException u) {
      System.out.println(u);
    } catch (IOException i) {
      System.out.println(i);
    }
  }

  enum Status {
    Registered,
    Deregistered,
    Disconnected,
    Reconnected,
  }

  @Override
  public void run() {
    try {
      // Displaying the thread that is running
      String line = "";
      String command = "";
      Status status = Status.Deregistered;
      String keyrange = 1023;

      // keep reading until "quit" is input
      while (line != "quit") {
        System.out.print("input> ");
        line = input.readLine();
        String error = "";

        if (line.indexOf("register ") == 0) {
          if (line.split(" ").length != 2) error =
            "register: missing operand"; else {
            if (status.equals(Status.Deregistered)) {
              int listnerPort = Integer.parseInt(line.split(" ")[1]);
              nreceiverThread = new receiverThread(fileName, listnerPort);
              InetAddress localhost = InetAddress.getLocalHost();
              String ipAddress = (localhost.getHostAddress()).trim();
              command =
                "register#" + uniqueID + "#" + ipAddress + "#" + listnerPort;
            } else error = "Participant is already registered!";
          }
        } else if (line.equals("deregister") && !line.contains(" ")) {
          if (!status.equals(Status.Deregistered)) command =
            line + "#" + uniqueID; else error =
            "Participant is not registered yet!";
        } else if (line.equals("disconnect") && !line.contains(" ")) {
          if (
            !(
              status.equals(Status.Disconnected) ||
              (status.equals(Status.Deregistered))
            )
          ) command = line + "#" + uniqueID; else error =
            "Participant is already disconnected!";
        } else if (line.contains("reconnect ")) {
          if (line.split(" ").length != 2) error =
            "reconnect: missing operand"; else {
            if (status.equals(Status.Disconnected)) {
              int listnerPort = Integer.parseInt(line.split(" ")[1]);
              nreceiverThread = new receiverThread(fileName, listnerPort);

              InetAddress localhost = InetAddress.getLocalHost();
              String ipAddress = (localhost.getHostAddress()).trim();
              command =
                "reconnect#" + uniqueID + "#" + ipAddress + "#" + listnerPort;
            } else if (status.equals(Status.Deregistered)) error =
              "Participant is not registered yet!"; else error =
              "Participant is already connected!";
          }
        } else if (line.contains("msend ")) {
          if (line.split(" ").length < 2) {
            error = "msend: missing operand";
          } else {
            if (
              status.equals(Status.Registered) ||
              (status.equals(Status.Reconnected))
            ) {
              line = line.replaceFirst(" ", "#");
              command = line + "#" + uniqueID;
            } else if (status.equals(Status.Deregistered)) error =
              "Participant is not registered yet!"; else error =
              "Participant is not connected!";
          }
        } else {
          error = "invalid Command";
        }
        //put error
        if (error.isEmpty()) {
          coordinatorOut.writeUTF(command);
          String result = coordinatorInput.readUTF();
          if (result.contains("deregister")) {
            nreceiverThread.stop();
            status = Status.Deregistered;
          } else if (result.contains("disconnect")) {
            nreceiverThread.stop();
            status = Status.Disconnected;
          } else if (result.contains("reconnect")) {
            status = Status.Reconnected;
          } else if (result.contains(" register")) {
            status = Status.Registered;
          }

          System.out.println(result);
        } else System.out.println(error);
      }

      // close the connection
      input.close();
      coordinatorOut.close();
      coordinatorInput.close();
      socket.close();
    } catch (Exception e) {
      // Throwing an exception
      // System.out.println("workerThread: Exception is caught" + e.getMessage());
    }
  }
}

class receiverThread implements Runnable {

  private Socket listenerSocket = null;
  private ServerSocket listener = null;
  private DataInputStream messageInput = null;
  private DataOutputStream messageOutput = null;
  private String fileName;
  private int listenport;
  private FileOutputStream fileOutStream;

  receiverThread(String fname, int port) {
    fileName = fname;
    listenport = port;
    new Thread(this).start();
  }

  @Override
  public void run() {
    try {
      listener = new ServerSocket(listenport);
      System.out.println("Listener started on listenerPort");

      System.out.println("Waiting at listenerPort for a Coordinator ...");

      listenerSocket = listener.accept();
      System.out.println("Coordinator connected to listenerPort");

      messageInput =
        new DataInputStream(
          new BufferedInputStream(listenerSocket.getInputStream())
        );
      messageOutput = new DataOutputStream(listenerSocket.getOutputStream());
      File logFile = new File(fileName);
      fileOutStream = new FileOutputStream(logFile, true);
      logFile.createNewFile();

      String message = "";
      while (!message.equals("eof")) {
        message = messageInput.readUTF();
        System.out.println(message + "\ninput>");
        fileOutStream.write((message + "\n").getBytes());
      }
    } catch (Exception e) {
      // System.out.println(
      //   "Receiver Thread: Exception is caught" + e.getMessage()
      // );
    }
  }

  public void stop() {
    try {
      System.out.println("closing everything");
      listenerSocket.close();
      listener.close();
      messageInput.close();
      messageOutput.close();
      fileOutStream.flush();
      fileOutStream.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}

public class Participant {

  // constructor to put ip address and port
  public Participant(int id, String logFile, String address, int nPort) {
    //create user thread
    new workerThread(id, logFile, address, nPort).start();
  }

  public static void main(String[] args) {
    if (args.length == 1) {
      File file = new File(args[0]);
      if (file.exists() && file.isFile()) {
        try {
          Scanner sc2 = new Scanner(file);

          int uniqueID = Integer.parseInt(sc2.nextLine());
          String logFileName = sc2.nextLine();
          String[] socketLine = sc2.nextLine().split(" ");
          String coordinatorIP = socketLine[0];
          int coordinatorListenPort = Integer.parseInt(socketLine[1]);

          sc2.close();

          new Participant(
            uniqueID,
            logFileName,
            coordinatorIP,
            coordinatorListenPort
          );
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
