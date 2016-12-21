import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatManager;
import org.jivesoftware.smack.chat.ChatManagerListener;
import org.jivesoftware.smack.chat.ChatMessageListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntry;
import org.jivesoftware.smack.roster.RosterListener;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smackx.chatstates.ChatState;
import org.jivesoftware.smackx.chatstates.ChatStateListener;
import org.jivesoftware.smackx.chatstates.ChatStateManager;
import org.jivesoftware.smackx.iqregister.AccountManager;
import org.jivesoftware.smackx.receipts.DeliveryReceiptManager;
import org.jivesoftware.smackx.receipts.ReceiptReceivedListener;
import org.jivesoftware.smackx.search.ReportedData;
import org.jivesoftware.smackx.search.UserSearchManager;
import org.jivesoftware.smackx.xdata.Form;
import org.jivesoftware.smackx.xevent.MessageEventManager;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

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
    private MessageEventManager messageEventManager;
    private ChatStateManager chatStateManager;

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
                .setSecurityMode(ConnectionConfiguration.SecurityMode.disabled)
                .setPort(Integer.valueOf(Constants.XMPP_CLIENT_CONNECTION_PORT))
                .setDebuggerEnabled(true)
                .setServiceName(Constants.XMPP_CLIENT_SERVICE_NAME)
                .build();

        xmppConnection = new XMPPTCPConnection(xmppConnectionConfig);
        if (!xmppConnection.isConnected())
            try {
                xmppConnection.connect();
                DeliveryReceiptManager dm = DeliveryReceiptManager
                        .getInstanceFor(xmppConnection);
                dm.setAutoReceiptMode(DeliveryReceiptManager.AutoReceiptMode.always);
                dm.addReceiptReceivedListener(new ReceiptReceivedListener() {

                    @Override
                    public void onReceiptReceived(final String fromid,
                                                  final String toid, final String msgid,
                                                  final Stanza packet) {
                        System.out.println("Delivery Receipt Received.");
                    }
                });

                if (!xmppConnection.isAuthenticated()){
                    xmppChatManager = ChatManager.getInstanceFor(xmppConnection);

                    setUpXmppRoster();
                    xmppConnection.login();
                    chatStateManager = ChatStateManager.getInstance(xmppConnection);
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

    public void establishConnectionAndCreateAccount(String username, String password, String serverName) {

        xmppConnectionConfig = XMPPTCPConnectionConfiguration.builder()
                .setHost(serverName)
                .setSecurityMode(ConnectionConfiguration.SecurityMode.disabled) //TODO work around
                .setPort(Integer.valueOf(Constants.XMPP_CLIENT_CONNECTION_PORT))
                .setDebuggerEnabled(true)
                .setServiceName(Constants.DEFAULT_SERVER_NAME)
                .build();

        xmppConnection = new XMPPTCPConnection(xmppConnectionConfig);
        if (!xmppConnection.isConnected()) {
            try {
                xmppConnection.connect();

                AccountManager accountManager = AccountManager.getInstance(xmppConnection);
//                accountManager.sensitiveOperationOverInsecureConnection(true);
                if (accountManager.supportsAccountCreation()) {
                    accountManager.createAccount(username, password);
                }
                else {
                    System.out.println("No support for account creation");
                    //TODO
                }
            } catch (XMPPException.XMPPErrorException e) {
                if(e.getXMPPError().getCondition() == XMPPError.Condition.conflict) {
                    System.out.println("Username exists");
                    // TODO: 6/4/2016
                }
                //e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (SmackException e) {
                e.printStackTrace();
            } catch (XMPPException e) {
                e.printStackTrace();
            }
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
            DeliveryReceiptManager.addDeliveryReceiptRequest(message);
            chat.sendMessage(message);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
            //TODO log error
        }
    }

    public void sendChatStatus(String username, String messageId) {
        messageEventManager = MessageEventManager.getInstanceFor(xmppConnection);
        try {
            messageEventManager.sendComposingNotification(username, messageId);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }

    }

    public void closeConnection() {
        if (xmppConnection!= null && xmppConnection.isConnected())
            xmppConnection.disconnect();
    }

    private class XmppChatMessageListener implements ChatMessageListener , ChatStateListener {

        @Override
        public void processMessage(Chat chat, Message message) {
            // Print out any messages we get back to standard out.
            System.out.println("Received message: " + message);
        }

        @Override
        public void stateChanged(Chat chat, ChatState state) {
            System.out.println("Received state: " + state.name());
        }
    }

    private class XmppRosterListener implements RosterListener {

        @Override
        public void entriesAdded(Collection<String> addresses) {
            System.out.println("Presence added: " + addresses);
        }

        @Override
        public void entriesUpdated(Collection<String> addresses) {
            System.out.println("Presence updated: " + addresses);
            //TODO
        }

        @Override
        public void entriesDeleted(Collection<String> addresses) {
            System.out.println("Presence deleted: " + addresses);
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

    //TODO refactor
    private boolean xmppUserExists(String user) throws XMPPException, SmackException.NotConnectedException, SmackException.NoResponseException {
        UserSearchManager userSearchManager = new UserSearchManager(xmppConnection);
        Form searchForm = userSearchManager.getSearchForm("search." + xmppConnection.getServiceName());
        Form answerForm = searchForm.createAnswerForm();
        answerForm.setAnswer("Username", true);
        answerForm.setAnswer("search", user);
        ReportedData data = userSearchManager.getSearchResults(answerForm,"search."+xmppConnection.getServiceName());
        if (data.getRows() != null) {
            Iterator<ReportedData.Row> it = (Iterator<ReportedData.Row>) data.getRows();
            while (it.hasNext()) {
                ReportedData.Row row = it.next();
                Iterator iterator = (Iterator) row.getValues("jid");
                if (iterator.hasNext()) {
                    String value = iterator.next().toString();
                    System.out.println("Iteartor values...... " + value);
                }
            }
            return true;
        }
        return false;
    }
}
