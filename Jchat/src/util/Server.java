package util;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private static Map<String, Connection> connectionMap = new ConcurrentHashMap<>();
    public static void main(String[] args) {
        ConsoleHelper.writeMessage("Enter port number:");
        int port = ConsoleHelper.readInt();
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(port);
            ConsoleHelper.writeMessage("The server has been started!");
            while (true) {
                Socket socket = serverSocket.accept();
                new Handler(socket).start();
            }
        } catch (Exception e) {
            ConsoleHelper.writeMessage(e.getMessage());
        } finally {
            try {
                if (serverSocket != null) {
                    serverSocket.close();
                }
            } catch (IOException e) {
                ConsoleHelper.writeMessage(e.getMessage());
            }
        }
    }

    public static void sendBroadcastMessage(Message message){
        Iterator it = connectionMap.entrySet().iterator();
        while (it.hasNext()){
            Map.Entry<String,Connection> pair = (Map.Entry<String, Connection>) it.next();
            try{
                pair.getValue().send(message);
            } catch (IOException e){
                ConsoleHelper.writeMessage(e.getMessage());
            }
        }
    }

    private static class Handler extends Thread {
        private Socket socket;

        public Handler(Socket socket) {
            this.socket = socket;
        }
        private String serverHandshake(Connection connection) throws IOException, ClassNotFoundException {
            Message request = new Message(MessageType.NAME_REQUEST);
            Message answer = null;
            do {
                connection.send(request);
                answer = connection.receive();
            } while (!(answer.getType().equals(MessageType.USER_NAME)) || answer.getData().isEmpty() || connectionMap.containsKey(answer.getData()));
            connectionMap.put(answer.getData(),connection);
            connection.send(new Message(MessageType.NAME_ACCEPTED));
            return answer.getData();
        }
        private void sendListOfUsers(Connection connection, String userName) throws IOException{
            Iterator it = connectionMap.entrySet().iterator();
            while (it.hasNext()){
                Map.Entry<String, Connection> pair = (Map.Entry<String, Connection>) it.next();
                String name = pair.getKey();
                if(!(name.equals(userName))) {
                    Message request = new Message(MessageType.USER_ADDED, name);
                    connection.send(request);
                }
            }
        }
        private void serverMainLoop(Connection connection, String userName) throws IOException, ClassNotFoundException {
            while (true) {
                Message message = connection.receive();
                if (message != null && message.getType() == MessageType.TEXT) {
                    sendBroadcastMessage(new Message(MessageType.TEXT, userName + ": " + message.getData()));
                } else {
                    ConsoleHelper.writeMessage("Error!");
                }
            }
        }
        public void run(){
            Connection connection = null;
            ConsoleHelper.writeMessage("Установленно соединение с адресом " + socket.getRemoteSocketAddress());

            SocketAddress remoteSocketAddress = null;
            String userName = "";
            try{
                connection = new Connection(this.socket);
                remoteSocketAddress = connection.getRemoteSocketAddress();
                if(remoteSocketAddress != null) ConsoleHelper.writeMessage(remoteSocketAddress.toString());
                userName = serverHandshake(connection);
                sendBroadcastMessage(new Message(MessageType.USER_ADDED, userName));
                sendListOfUsers(connection,userName);
                serverMainLoop(connection,userName);
                connectionMap.remove(userName);
                sendBroadcastMessage(new Message(MessageType.USER_REMOVED, userName));

            } catch (IOException | ClassNotFoundException e){
                ConsoleHelper.writeMessage("Error: " + e.getMessage());
            } finally {
                try {
                    if( connection!=null) connection.close();
                } catch (IOException e){

                }
            }
        }

    }
}
