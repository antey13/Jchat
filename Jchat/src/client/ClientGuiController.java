package client;

public class ClientGuiController extends Client {
    private ClientGuiModel model = new ClientGuiModel();
    private ClientGuiView view = new ClientGuiView(this);

    protected SocketThread getSocketThread(){
        return new GuiSocketThread();
    }
    public void run(){
        getSocketThread().run();
    }
    public String getServerAddress(){
        return view.getServerAddress();
    }
    public int getServerPort(){
        return view.getServerPort();
    }
    public String getUserName(){
        return view.getUserName();
    }
    public ClientGuiModel getModel(){
        return this.model;
    }

    public static void main(String[] args){
        new ClientGuiController().run();
    }

    public class GuiSocketThread extends SocketThread{
        public void processIncomingMessage(String message){
            model.setNewMessage(message);
            view.refreshMessages();
        }
        public void informAboutAddingNewUser(String userName){
            model.addUser(userName);
            view.refreshUsers();
        }
        public void informAboutDeletingNewUser(String userName){
            model.deleteUser(userName);
            view.refreshUsers();
        }
        public void notifyConnectionStatusChanged(boolean clientConnected){
            view.notifyConnectionStatusChanged(clientConnected);
        }
    }

}
