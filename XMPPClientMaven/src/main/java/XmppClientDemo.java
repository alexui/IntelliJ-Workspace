import org.jivesoftware.smack.packet.Message;

/**
 * Created by Alex on 6/3/2016.
 */
public class XmppClientDemo {

    public static void main(String[] args) {
        XmppClient xmppClient = new XmppClient();
//        xmppClient.establishConnectionAndCreateAccount("david.luiz", "casablanca" , Constants.DEFAULT_SERVER_NAME);
        xmppClient.establishConnectionAndLogIn(Constants.DEFAULT_USERNAME, Constants.DEFAULT_PASSWORD, Constants.DEFAULT_SERVER_NAME);

        int count = 1;
        while (count > 0) {
            Message message = new Message();
            message.setType(Message.Type.chat);
            message.addBody("en", "This is the body"); // TODO
            xmppClient.sendMessageToUser("calvin.klein", Constants.DEFAULT_SERVER_NAME, message);
            xmppClient.sendChatStatus("calvin.klein@chester-pc", "1234");
            count--;
        }
        while (true) {

        }
    }
}
