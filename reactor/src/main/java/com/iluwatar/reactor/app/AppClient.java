package com.iluwatar.reactor.app;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Represents the clients of Reactor pattern. Multiple clients are run concurrently and send logging
 * requests to Reactor.
 */
public class AppClient {
  private final ExecutorService service = Executors.newFixedThreadPool(4);

  /**
   * App client entry.
   * 
   * @throws IOException if any I/O error occurs.
   */
  public static void main(String[] args) throws IOException {
    AppClient appClient = new AppClient();
    appClient.start();
  }

  /**
   * Starts the logging clients.
   * 
   * @throws IOException if any I/O error occurs.
   */
  public void start() throws IOException {
    service.execute(new TCPLoggingClient("Client 1", 6666));
    service.execute(new TCPLoggingClient("Client 2", 6667));
    service.execute(new UDPLoggingClient("Client 3", 6668));
    service.execute(new UDPLoggingClient("Client 4", 6668));
  }

  /**
   * Stops logging clients. This is a blocking call.
   */
  public void stop() {
    service.shutdown();
    if (!service.isTerminated()) {
      service.shutdownNow();
      try {
        service.awaitTermination(1000, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  private static void artificialDelayOf(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  /**
   * A logging client that sends requests to Reactor on TCP socket.
   */
  static class TCPLoggingClient implements Runnable {

    private final int serverPort;
    private final String clientName;

    /**
     * Creates a new TCP logging client.
     * 
     * @param clientName the name of the client to be sent in logging requests.
     * @param port the port on which client will send logging requests.
     */
    public TCPLoggingClient(String clientName, int serverPort) {
      this.clientName = clientName;
      this.serverPort = serverPort;
    }

    public void run() {
      try (Socket socket = new Socket(InetAddress.getLocalHost(), serverPort)) {
        OutputStream outputStream = socket.getOutputStream();
        PrintWriter writer = new PrintWriter(outputStream);
        sendLogRequests(writer, socket.getInputStream());
      } catch (IOException e) {
        e.printStackTrace();
        throw new RuntimeException(e);
      }
    }

    private void sendLogRequests(PrintWriter writer, InputStream inputStream) throws IOException {
      for (int i = 0; i < 4; i++) {
        writer.println(clientName + " - Log request: " + i);
        writer.flush();

        byte[] data = new byte[1024];
        int read = inputStream.read(data, 0, data.length);
        if (read == 0) {
          System.out.println("Read zero bytes");
        } else {
          System.out.println(new String(data, 0, read));
        }

        artificialDelayOf(100);
      }
    }

  }

  /**
   * A logging client that sends requests to Reactor on UDP socket.
   */
  static class UDPLoggingClient implements Runnable {
    private final String clientName;
    private final InetSocketAddress remoteAddress;

    /**
     * Creates a new UDP logging client.
     * 
     * @param clientName the name of the client to be sent in logging requests.
     * @param port the port on which client will send logging requests.
     * @throws UnknownHostException if localhost is unknown
     */
    public UDPLoggingClient(String clientName, int port) throws UnknownHostException {
      this.clientName = clientName;
      this.remoteAddress = new InetSocketAddress(InetAddress.getLocalHost(), port);
    }

    @Override
    public void run() {
      try (DatagramSocket socket = new DatagramSocket()) {
        for (int i = 0; i < 4; i++) {

          String message = clientName + " - Log request: " + i;
          DatagramPacket request =
              new DatagramPacket(message.getBytes(), message.getBytes().length, remoteAddress);

          socket.send(request);

          byte[] data = new byte[1024];
          DatagramPacket reply = new DatagramPacket(data, data.length);
          socket.receive(reply);
          if (reply.getLength() == 0) {
            System.out.println("Read zero bytes");
          } else {
            System.out.println(new String(reply.getData(), 0, reply.getLength()));
          }

          artificialDelayOf(100);
        }
      } catch (IOException e1) {
        e1.printStackTrace();
      }
    }
  }
}
