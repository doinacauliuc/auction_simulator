import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The {@code Server} class simulates a price generator and a multi-client system.
 * It listens on a specific port, waits for clients to connect, and sends random prices to the connected clients.
 * The server generates random prices and sends them to clients connected to it.
 *
 * <p>Clients can send requests to purchase, and the server manages the connection and communication with each client.
 * The server stops when all clients have disconnected.</p>
 */
public class Server {

    /** The port number the server listens on. */
    private static final int port = 9090;

    /** The time to sleep between price generations in milliseconds. */
    private static final long sleeptime = 2000;

    /** Random number generator for price generation. */
    private static final Random random = new Random();

    /** List of PrintWriters for communicating with connected clients. */
    private static final List<PrintWriter> clientWriters = new ArrayList<>();

    /** Thread for generating prices. */
    private static Thread t1;

    /** An instance to keep track of connected clients. */
    private static ConnectedClients nClients;

    /** A flag to control the running state of the server. Defined as volatile since it may be modified by different threads. */
    private static volatile boolean running;

    /** The server socket for listening to client connections. */
    private static ServerSocket serverSocket;

    /**
     * The main method initializes the server, listens for client connections,
     * and starts the price generation when a sufficient number of clients have connected.
     *
     * @param args not used.
     */
    public static void main( final String[] args) {

        try {

            serverSocket = new ServerSocket(port);
            System.out.println("Waiting for connection...");

            //thread to generate prices
            t1 = new Thread(Server::generatePrice);

            //object to keep track of connected clients
            nClients = new ConnectedClients();

            //initial state of the server set to running
            running = true;

            while (running) {
                try {

                    //server is listening to connections
                    Socket clientSocket = serverSocket.accept();

                    //increases the ConnectedClients count after each connection
                    nClients.increase();

                    //starts a thread to handle each client concurrently
                    new Thread(new ClientHandler(clientSocket)).start();

                    System.out.println("Client " + nClients.getCount() + " connected to " + clientSocket.getRemoteSocketAddress());

                    //creates a PrinterWriter object for each client
                    synchronized (clientWriters) {
                        clientWriters.add(new PrintWriter(clientSocket.getOutputStream(), true));
                    }

                    // Start generating and sending prices when at least 2 clients are connected
                    if (nClients.getCount() == 2) {
                        t1.start();
                    }

                } catch (IOException e) {
                    //function to stop server
                    stopServer();
                }

            }

        } catch (IOException e) {
            System.out.println("Failed to listen on port " + port + ": " + e.getMessage());
        }

    }

    /**
     * This method generates random prices between 10 and 100.
     * It sends these prices to all connected clients every {@code sleeptime} milliseconds.
     */
    public static void generatePrice() {
        try {
            //while there are clients connected
            while (nClients.getCount() != 0) {
                //generates int between 10 and 100
                int price = 10 + random.nextInt(91);
                System.out.println("Offered price: " + price);

                //sends the generated price to all connected client
                synchronized (clientWriters) {
                    for (PrintWriter writer : clientWriters) {
                        writer.println(price);
                    }
                }

                //thread pauses
                Thread.sleep(sleeptime);
            }
        } catch (InterruptedException e) {
            System.out.println("Stopped generating prices");
        }
    }

    /**
     * {@code ClientHandler} handles the communication with a single client.
     * It listens for messages from the client, such as "Purchase request" or "Finished purchasing".
     */
    public static class ClientHandler implements Runnable {
        private final Socket client; /** The socket connected to the client. */

        //each client can send messages to the server with individual BufferedReader objects
        BufferedReader in;

        /**
         * Constructs a {@code ClientHandler} with a specific client socket.
         *
         * @param client the socket connected to the client.
         */
        public ClientHandler(Socket client) {
            this.client = client;
        }

        /**
         * The run method listens for client messages and handles disconnections.
         */
        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(client.getInputStream()));


                //handles client based on received messages
                while (client.isConnected()) {
                    String message = in.readLine();
                    if (message.equals("Purchase request")) {
                        System.out.println("Purchase request received from: " + client);
                    } else if (message.equals("Finished purchasing")) {
                        System.out.println("Client " + client + " finished purchasing");
                        in.close();
                        client.close();
                    }
                }
            } catch (IOException e) {
                //if a client disconnects remove it from ConnectedClients count
                nClients.decrease();
                System.out.println("Connection to client: " + client + " closed");

                //check if there are clients left, if no clients are left stops the server from running
                if (nClients.getCount() == 0) {
                    running = false;
                    try {
                        //closing server socket
                        serverSocket.close();
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Stops the server, including stopping the price generation thread
     * and closing all PrintWriter connections to clients.
     */
    public static void stopServer() {

        try {
            System.out.println("Server is stopping...");
            // Interrupt the price generation thread
            if (t1 != null && t1.isAlive()) {
                t1.interrupt();
                t1.join(); // Wait for the thread to finish
            }

            // Close all opened PrintWriters
            synchronized (clientWriters) {
                for (PrintWriter writer : clientWriters) {
                    writer.close();
                }
            }
            System.out.println("Server stopped.");

        } catch (InterruptedException e) {
            System.out.println("Server stop interrupted: " + e.getMessage());
        }
    }

    /**
     * This class manages the count of connected clients.
     * It provides methods to increase, decrease, and get the client count.
     */
    public static class ConnectedClients {
        private int count;

        /**
         * Initializes the client count to 0.
         */
        public ConnectedClients() {
            count = 0;
        }

        /**
         * Increases the number of connected clients by 1.
         * <p>This method is synchronized to ensure that only one thread can
         * modify the client count at a time. This prevents race conditions
         * where multiple threads might attempt to increase the count simultaneously,
         * potentially leading to an inconsistent or incorrect value.</p>
         */

        public synchronized void increase() {
            count++;
        }

        /**
         * Decreases the number of connected clients by 1.
         * <p>This method is synchronized to ensure that only one thread can
         * modify the client count at a time. This prevents race conditions
         * where multiple threads might attempt to increase the count simultaneously,
         * potentially leading to an inconsistent or incorrect value.</p>
         */

        public synchronized void decrease() {
            count--;
        }

        /**
         * Gets the current number of connected clients.
         *
         * @return the number of connected clients.
         */
        public int getCount() {
            return count;
        }
    }
}
