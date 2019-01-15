import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.SocketException;

public class Client {
    private Socket socket;
    private ConnectionHandler connectionHandler;
    private boolean running;
    private Gson gson = new Gson();
    private JsonReader input;
    private JsonWriter output;
    private Player player;

    Client(Socket socket) {
        this.socket = socket;

        // initialize json readers/writers and attach them to the socket's input/output streams
        try {
            input = new JsonReader(new InputStreamReader(socket.getInputStream()));
            output = new JsonWriter(new OutputStreamWriter(socket.getOutputStream()));
        } catch (IOException e) {
            System.err.println("problem making input reader/output writer");
            e.printStackTrace();
        }

        // set up client
        try {
            StartClientPacket startClientPacket = gson.fromJson(input, StartClientPacket.class);
            player = new Player(startClientPacket.getCharacterSprite(), startClientPacket.getCarSprite());
        } catch (Exception e) {

        }
    }

    public Player getPlayer() {
        return player;
    }

    public void startThread() {
        connectionHandler = new ConnectionHandler();
        Thread t = new Thread(connectionHandler);
        running = true;
        t.start();
    }

    public void send(Object packet) {
        connectionHandler.send(packet);
    }

    private class ConnectionHandler implements Runnable{
        private ConnectionHandler() {

        }

        public void run() {
            while(running) {  // loop unit a message is received
                try {
                    // adds message to messageBuffer
                    ClientPacket clientPacket = gson.fromJson(input, ClientPacket.class);
                    System.out.println("got message");
                    // todo process the received packet
                } catch (JsonIOException e) {
                    System.err.println("Failed to receive msg from the socket");
                    e.printStackTrace();
                } catch (JsonSyntaxException e) {
                    // check if socket is disconnected
                    try {
                        if (e.getCause() instanceof SocketException) {
                            System.out.println("closing socket");
                            socket.close();
                            running = false;
                        } else {
                            System.err.println("problem with JSON syntax");
                            socket.close();
                            running = false;
                            e.printStackTrace();
                        }
                    } catch (IOException e1) {
                        System.err.println("couldnt close socket");
                        e.printStackTrace();
                    }
                }
            }
        }

        public void send(Object packet) {
            gson.toJson(packet, packet.getClass(), output);
            try {
                output.flush();
            } catch (IOException e) {
                System.err.println("Packet failed to send");
                e.printStackTrace();
            }
        }
    }
}
