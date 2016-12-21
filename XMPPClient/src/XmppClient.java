import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatManager;
import org.jivesoftware.smack.chat.ChatManagerListener;
import org.jivesoftware.smack.chat.ChatMessageListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntry;
import org.jivesoftware.smack.roster.RosterListener;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smackx.iqregister.AccountManager;

import java.io.IOException;
import java.util.Collection;

/**
 * Created by Alex on 6/3/2016.
 */
public class XmppClient {

    private XMPPTCPConnectionConfiguration xmppConnectionConfig;
    private XMPPTCPConnection xmppConnection;
    private Presence presence;

    private ChatManager xmppChatManager;
    private Roster xmppRoster;

    private XmppRosterListener xmppRosterListener;
    private ChatMessageListener xmppChatMessageListener;

    public XmppClient() {
        xmppChatMessageListener = new XmppChatMessageListener();
        presence = new Presence(Presence.Type.available);
    }

    /*
     * DNS SRV lookup will be performed to find the exact address and port
     * default port is 5222
     * TLS is activated
     * the XMPP resource name "Smack" will be used for the connection
     */
    public void establishConnectionAndLogIn(String username, String password, String serverName) {

        xmppConnectionConfig = XMPPTCPConnectionConfiguration.builder()
                .setUsernameAndPassword(username, password)
                .setHost(serverName)
                .setSecurityMode(ConnectionConfiguration.SecurityMode.required)
                .setPort(Integer.valueOf(Constants.XMPP_CLIENT_CONNECTION_PORT))
                .setDebuggerEnabled(true)
                .setServiceName(Constants.XMPP_CLIENT_SERVICE_NAME)
                .build();

        xmppConnection = new XMPPTCPConnection(xmppConnectionConfig);
        if (!xmppConnection.isConnected())
            try {
                xmppConnection.connect();
                if (!xmppConnection.isAuthenticated()){
                    xmppChatManager = ChatManager.getInstanceFor(xmppConnection);

                    setUpXmppRoster();
                    xmppConnection.login();
                    setUpXmppMessageListener();
                    setPresenceType(Presence.Type.available);
                }
                // It is possible to log in without sending an initial available presence by using
                // ConnectionConfiguration.Builder.setSendPresence(boolean).
                // Finally, if you want to not pass a password and instead use a more advanced mechanism while
                // using SASL then you may be interested in using ConnectionConfiguration.Builder.setCallbackHandler
                // (javax.security.auth.callback.CallbackHandler). For more advanced login settings see ConnectionConfiguration.
                // https://www.javacodegeeks.com/2010/09/xmpp-im-with-smack-for-java.html
            } catch (SmackException e) {
                e.printStackTrace();
            } catch (XMPPException e) {
                e.printStackTrace();
                //TODO log error
            } catch (IOException e) {
                e.printStackTrace();
                //TODO log error
            }
    }

    public void establishConnectionAndCreateAccount(String serverName) {

        xmppConnectionConfig = XMPPTCPConnectionConfiguration.builder()
                .setHost(serverName)
                .setSecurityMode(ConnectionConfiguration.SecurityMode.required)
                .setPort(Integer.valueOf(Constants.XMPP_CLIENT_CONNECTION_PORT))
                .setDebuggerEnabled(true)
                .setServiceName(Constants.XMPP_CLIENT_SERVICE_NAME)
                .build();

        if (!xmppConnection.isConnected())
            try {
                xmppConnection.connect();

                AccountManager accountManager = AccountManager.getInstance(xmppConnection);
                if (accountManager.supportsAccountCreation()) {
                    System.out.println(accountManager.getAccountInstructions());
                    System.out.println(accountManager.getAccountAttributes());
                    //TODO create account
                }
                else {
                    System.out.println("No support for account creation");
                }
            } catch (XMPPException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (SmackException e) {
                e.printStackTrace();
            }
            //TODO log error
    }

    private void setUpXmppRoster() {
        xmppRoster = Roster.getInstanceFor(xmppConnection);
        xmppRosterListener = new XmppRosterListener();
        xmppRoster.addRosterListener(xmppRosterListener);
        xmppRoster.setSubscriptionMode(Roster.SubscriptionMode.manual);
        //TODO
        // If using the manual mode, a PacketListener should be registered that
        // listens for Presence packets that have a type of Presence.Type.subscribe
    }

    public void setPresenceType(Presence.Type type) {
        presence.setType(type);
        updatePresence();
    }

    public void setPresenceMode(Presence.Mode mode) {
        presence.setMode(mode);
        updatePresence();
    }

    public void updatePresence() {
        try {
            xmppConnection.sendStanza(presence);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
            //TODO log error
        }
    }

    public void getRosterEntries() {
        Collection<RosterEntry> rosterEntries = xmppRoster.getEntries();
        for (RosterEntry rosterEntry : rosterEntries) {
            System.out.println(rosterEntry);
            //TODO do something
        }
    }

    public void sendMessageToUser(String userrname, String serverName, Message message) {
        Chat chat = xmppChatManager.createChat(userrname + "@" + serverName, xmppChatMessageListener);
        try {
            chat.sendMessage(message);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
            //TODO log error
        }
    }

    public void closeConnection() {
        if (xmppConnection!= null && xmppConnection.isConnected())
            xmppConnection.disconnect();
    }

    private class XmppChatMessageListener implements ChatMessageListener {

        @Override
        public void processMessage(Chat chat, Message message) {
            // Print out any messages we get back to standard out.
            System.out.println("Received message: " + message);
        }
    }

    private class XmppRosterListener implements RosterListener {

        @Override
        public void entriesAdded(Collection<String> addresses) {

        }

        @Override
        public void entriesUpdated(Collection<String> addresses) {
            //TODO
        }

        @Override
        public void entriesDeleted(Collection<String> addresses) {

        }

        @Override
        public void presenceChanged(Presence presence) {
            System.out.println("Presence changed: " + presence.getFrom() + " " + presence);
        }
    }

    private void setUpXmppMessageListener() {
        xmppChatManager.addChatListener(new ChatManagerListener() {
            @Override
            public void chatCreated(Chat chat, boolean createdLocally) {
                if (!createdLocally)
                    chat.addMessageListener(xmppChatMessageListener);;
            }
        });
    }
}
