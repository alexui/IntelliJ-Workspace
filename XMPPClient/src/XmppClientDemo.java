/**
 * Created by Alex on 6/3/2016.
 */
public class XmppClientDemo {

    public static void main(String[] args) {
        XmppClient xmppClient = new XmppClient();
        xmppClient.establishConnectionAndCreateAccount(Constants.DEFAULT_SERVER_NAME);
    }
}
