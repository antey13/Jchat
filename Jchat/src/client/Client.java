package client;


import util.*;
import java.io.IOException;
import java.net.Socket;

public class Client {
    protected Connection connection;
    private volatile boolean clientConnected = false;

    public static void main(String[] args){
        Client client = new Client();
        client.run();
    }


    public class SocketThread extends Thread {
        protected void processIncomingMessage(String message){
            ConsoleHelper.writeMessage(message);
        }
        protected void informAboutAddingNewUser(String userName){
            ConsoleHelper.writeMessage(userName+" - connected.");
        }
        protected void informAboutDeletingNewUser(String userName){
            ConsoleHelper.writeMessage(userName+" leave this chat");
        }
        protected void notifyConnectionStatusChanged(boolean clientConnected){
            Client.this.clientConnected = clientConnected;
            synchronized (Client.this){
                Client.this.notify();
            }
        }
        protected void clientHandshake() throws IOException, ClassNotFoundException {
            Message message;
            while (true) {
                try {
                    message = connection.receive();
                } catch (Exception e) {
                    break;
                }
                if (message != null) {
                    if (message.getType() == MessageType.NAME_REQUEST) {
                        connection.send(new Message(MessageType.USER_NAME, getUserName()));
                    } else {
                        if (message.getType() == MessageType.NAME_ACCEPTED) {
                            notifyConnectionStatusChanged(true);
                            return;
                        } else {
                            throw new IOException("Unexpected util.MessageType");
                        }
                    }
                }
            }
        }
        protected void clientMainLoop() throws IOException, ClassNotFoundException {
            Message message;
            while (true) {
                try {
                    message = connection.receive();
                } catch (Exception e) {
                    break;
                }
                if (message != null) {
                    if (message.getType() == MessageType.TEXT) {
                        processIncomingMessage(message.getData());
                    } else {
                        if (message.getType() == MessageType.USER_ADDED) {
                            informAboutAddingNewUser(message.getData());
                        } else {
                            if (message.getType() == MessageType.USER_REMOVED) {
                                informAboutDeletingNewUser(message.getData());
                            } else {
                                break;
                            }
                        }
                    }
                }
            }
            throw new IOException("Unexpected util.MessageType");
        }

        @Override
        public void run() {
            String serverAdress = getServerAddress();
            int serverPort = getServerPort();

            try {
                Socket socket = new Socket(serverAdress, serverPort);
                connection = new Connection(socket);
                clientHandshake();
                clientMainLoop();
            } catch (IOException e) {
                notifyConnectionStatusChanged(false);
            } catch (ClassNotFoundException e) {
                notifyConnectionStatusChanged(false);
            }
        }
    }


    protected String getServerAddress(){
        ConsoleHelper.writeMessage("Enter server address:");
        return ConsoleHelper.readString();
    }
    protected int getServerPort(){
        ConsoleHelper.writeMessage("Enter server port:");
        return ConsoleHelper.readInt();
    }
    protected String getUserName(){
        ConsoleHelper.writeMessage("Enter username:");
        return ConsoleHelper.readString();
    }
    protected boolean shouldSendTextFromConsole(){
        return true;
    }
    protected SocketThread getSocketThread(){
        return new SocketThread();
    }
    protected void sendTextMessage(String text){
        try{
            connection.send(new Message(MessageType.TEXT, text));
        } catch (IOException e){
            ConsoleHelper.writeMessage("Error");
            clientConnected = false;
        }
    }
    public void run(){
        SocketThread socketThread = getSocketThread();
        socketThread.setDaemon(true);
        socketThread.start();
        synchronized (this) {
            try{
                wait();
            } catch (InterruptedException e){
                ConsoleHelper.writeMessage("Error!");
            }
        }
        if(clientConnected) ConsoleHelper.writeMessage("Соединение установлено. Для выхода наберите команду 'exit'.");
        else ConsoleHelper.writeMessage("Произошла ошибка во время работы клиента.");
        while (clientConnected){
            String text = ConsoleHelper.readString();
            if(text.equals("exit")) break;
            if(shouldSendTextFromConsole()) sendTextMessage(text);
        }

    }
}
