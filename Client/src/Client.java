import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Random;

/**
 * The {@code Client} class represents a client in a client-server architecture.
 * It connects to a server, receives price offers, and makes purchase decisions based on those offers.
 * The client can handle up to 10 purchases and sends purchase requests to the server.
 */
public class Client {

    /**
     * Initiates the buying process by connecting to the server and managing purchase requests.
     * It connects to the server on a predefined port and interacts with it to handle price offers.
     */
    public void buy() {
        final int port = 9090; // The port number to connect to the server.
        int purchases = 0; // Counter for the number of purchases made.
        int sell_price = 0; // The price offered by the server.
        int buy_price = 0; // The price generated by the client for counteroffer.

        Socket socket = null; // Socket for connecting to the server.
        try {
            // Connect to the server using localhost and the specified port.
            socket = new Socket(InetAddress.getLocalHost(), port);
            System.out.println("Connected to " + socket.getInetAddress().getHostAddress());

            // Create input and output streams for communication with the server.
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter printWriter = new PrintWriter(socket.getOutputStream(), true);

            // Loop until the purchase limit is reached.
            while (purchases <= 10) {
                String string = bufferedReader.readLine(); // Read the price offer from the server.
                sell_price = Integer.parseInt(string); // Parse the offered price.

                System.out.println("Received offer from server: " + sell_price);
                buy_price = generatePrice(); // Generate a buy price for counteroffer.

                System.out.println("Counteroffer: " + buy_price);

                // Compare the sell price with the buy price.
                if (sell_price < buy_price) {
                    purchases++; // Increment the purchase counter.
                    System.out.println("Accepted offer from server");
                    System.out.println("Current purchase count: " + purchases);
                    printWriter.println("Purchase request"); // Send purchase request to server.
                } else {
                    System.out.println("Rejected offer from server");
                }

                // Check if the purchase limit has been reached.
                if (purchases == 10) {
                    System.out.println("Reached purchase limit");
                    printWriter.println("Finished purchasing"); // Notify server of finished purchasing.
                    printWriter.close(); // Close the PrintWriter.
                    socket.close(); // Close the socket.
                }
            }

        } catch (IOException e) {
            System.out.println("Closing connection due to IOException");
        }
    }

    /**
     * Generates a random buy price between 10 and 75.
     *
     * @return a randomly generated buy price.
     */
    private static int generatePrice() {
        Random random = new Random();
        int buy_price = 10 + random.nextInt(66); // Generate a price between 10 and 75.
        return buy_price; // Return the generated buy price.
    }

    /**
     * The main method creates an instance of the {@code Client} class
     * and initiates the buying process.
     *
     * @param args command-line arguments (not used).
     */
    public static void main(String[] args) {
        Client client = new Client(); // Create a new client instance.
        client.buy(); // Start the buying process.
    }
}
