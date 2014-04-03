package de.imc.mirror.sdk.android;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.XMLOutputter;
import org.jivesoftware.smack.AccountManager;
import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.packet.DiscoverItems;

import de.imc.mirror.sdk.ConnectionConfiguration;
import de.imc.mirror.sdk.ConnectionStatus;
import de.imc.mirror.sdk.ConnectionStatusListener;
import de.imc.mirror.sdk.UserInfo;
import de.imc.mirror.sdk.config.NamespaceConfig;
import de.imc.mirror.sdk.exceptions.ConnectionStatusException;
import de.imc.mirror.sdk.exceptions.EntityExistsException;

/**
 * Java implementation of the connection handler interface.
 * The implementation is based on the XMPPConnection implementation provided by the SMACK library. 
 * @author nicolas.mach(at)im-c.de, simon.schwantzer(at)im-c.de
 */
public class ConnectionHandler implements de.imc.mirror.sdk.ConnectionHandler, ConnectionListener {
	private static final Logger logger = Logger.getLogger(ConnectionHandler.class.getName());
	private XMPPConnection connection;
	private ConnectionConfiguration connectionConfiguration;
	private ConnectionStatus status;
	private List<ConnectionStatusListener> listeners;
	private UserInfo userInfo;
	private String password;
	private Map<String, RequestFuture<Element>> pendingIQRequests;
	private PacketListener iqPacketListener;
	private NetworkInformation networkInformation;

	/**
	 * Instantiates a connection handler with the given connection configuration and user credentials.
	 * @param user XMPP username of the user to log in when connecting to the server.
	 * @param password Password of the user to log in.
	 * @param connectionConfiguration Connection configuration.
	 */
	public ConnectionHandler(String user, String password, de.imc.mirror.sdk.ConnectionConfiguration connectionConfiguration){
		this.connectionConfiguration = connectionConfiguration;
		this.password = password;
		listeners = new ArrayList<ConnectionStatusListener>();
		setConnectionStatus(ConnectionStatus.OFFLINE);
		ProviderInitializer.initializeProviderManager();
		org.jivesoftware.smack.ConnectionConfiguration config = 
				new org.jivesoftware.smack.ConnectionConfiguration(connectionConfiguration.getHost(),
						connectionConfiguration.getPort());
		SecurityMode mode;
		if (connectionConfiguration.isSecureConnection()){
			mode = SecurityMode.required;
		} else {
			mode = SecurityMode.disabled;
		}
		userInfo = new de.imc.mirror.sdk.android.UserInfo(user, 
									connectionConfiguration.getDomain(), 
									connectionConfiguration.getApplicationID());
		config.setSecurityMode(mode);
		config.setSelfSignedCertificateEnabled(connectionConfiguration.isSelfSignedCertificateEnabled());
		config.setReconnectionAllowed(true);
		connection = new XMPPConnection(config);
		
		networkInformation = null;
		pendingIQRequests = new HashMap<String, RequestFuture<Element>>();
		iqPacketListener = new PacketListener() {
			@Override
			public void processPacket(Packet packet) {
				if (pendingIQRequests.containsKey(packet.getPacketID())){
					RequestFuture<Element> iqFuture = pendingIQRequests.get(packet.getPacketID());
					pendingIQRequests.remove(packet.getPacketID());
					SAXBuilder reader = new SAXBuilder();
					Document document = null;
					try {
						document = reader.build(new StringReader(packet.toXML()));
					} catch (Exception e) {
						logger.log(Level.WARNING, "Failed to parse incoming IQ packet.", e);
					}
					Element childElement = document.getRootElement();
					if (childElement != null) {
						iqFuture.setResponse(childElement);
					}
				}
			}
		};
		connection.addPacketListener(iqPacketListener, new PacketTypeFilter(IQ.class));
	}

	/**
	 * Returns the connection configuration.
	 * @return Current connection configuration. 
	 */
	@Override
	public ConnectionConfiguration getConfiguration() {
		return connectionConfiguration;
	}

	/**
	 * Returns the connection status.
	 * @return Current connection status of this handler.
	 */
	@Override
	public ConnectionStatus getStatus() {
		return status;
	}

	/**
	 * Adds a listener to be notified when the connection status changes.
	 * @param listener Listener to add. 
	 */
	@Override
	public void addConnectionStatusListener(ConnectionStatusListener listener) {
		listeners.add(listener);
	}

	/**
	 * Removes a connection status listener.
	 * If the listener is not set, nothing will happen.
	 * @param listener Listener to remove.
	 */
	@Override
	public void removeConnectionStatusListener(ConnectionStatusListener listener) {
		listeners.remove(listener);
	}

	/**
	 * Tries to establish a connection and perform the login.
	 * @throws ConnectionStatusException The connection could not be established.
	 */
	@Override
	public void connect() throws ConnectionStatusException {
		this.establishConnection();
		this.login();
	}

	/**
	 * Tries to establish a connection, create the configured user, and perform the login.
	 * @throws UnsupportedOperationException <code>createUser</code> is set to <code>true</code>, but the XMPP server does not support in-band registration.
	 * @throws EntityExistsException <code>createUser</code> is set to <code>true</code>, but a XMPP user account with the given username already exists.
	 * @throws ConnectionStatusException The connection could not be established.
	 */
	@Override
	public void connectAndCreateUser() throws UnsupportedOperationException, EntityExistsException, ConnectionStatusException {
		this.establishConnection();
		this.createUser();
		this.login();
	}

	private void establishConnection() throws ConnectionStatusException{
		if (status == ConnectionStatus.ONLINE) {
			throw new ConnectionStatusException("A connection is already established.");
		}
		try {
			setConnectionStatus(ConnectionStatus.PENDING);
			connection.connect();
			connection.addConnectionListener(this);
		} catch (XMPPException e) {
			setConnectionStatus(ConnectionStatus.ERROR);
			throw new ConnectionStatusException("Failed to establish connection to the XMPP server.", e);
		}
	}
	
	private void login() throws ConnectionStatusException {
		if (status == ConnectionStatus.ONLINE) {
			throw new ConnectionStatusException("A connection is already established.");
		}
		try {
			connection.login(userInfo.getUsername(), password, userInfo.getResource());
			this.networkInformation = requestNetworkInformation();
		} catch (XMPPException e) {
			setConnectionStatus(ConnectionStatus.ERROR);
			throw new ConnectionStatusException("Failed to login to the XMPP server.", e);
		}
		setConnectionStatus(ConnectionStatus.ONLINE);
	}
	
	/**
	 * Request information about the XMPP domain.  
	 * @return Network information container.
	 */
	private NetworkInformation requestNetworkInformation() {
		// Retrieve JID of spaces service and persistence service.
		String spacesServiceJID = null;
		String persistenceServiceJID = null;
		ServiceDiscoveryManager discoveryManager = ServiceDiscoveryManager.getInstanceFor(connection);
		DiscoverItems items;
		try {
			items = discoveryManager.discoverItems(connectionConfiguration.getDomain());
			if (items.getType() == IQ.Type.RESULT) {
				Iterator<DiscoverItems.Item> itemIterator = items.getItems();
				while (spacesServiceJID == null && itemIterator.hasNext()) {
					DiscoverItems.Item item = itemIterator.next();
					if ("MIRROR Spaces Service".equalsIgnoreCase(item.getName())) {
						spacesServiceJID = item.getEntityID();
					} else if ("MIRROR Persistence Service".equalsIgnoreCase(item.getName())) {
						persistenceServiceJID = item.getEntityID();
					}
				}
			}
		} catch (XMPPException e) {
			logger.log(Level.WARNING, "Failed to discover service addresses.", e);
		}
		
		// Retrieve version of the spaces service.
		String spacesServiceVersion = null;
		if (spacesServiceJID != null) {
			IQ versionRequestIQ = new IQ(){

				@Override
				public String getChildElementXML() {
					Element childElement = new Element("spaces", NamespaceConfig.SPACES_SERVICE);
					childElement.addContent(new Element("version", NamespaceConfig.SPACES_SERVICE));
					XMLOutputter out = new XMLOutputter();
					return out.outputString(childElement);
				}
			};
			versionRequestIQ.setType(IQ.Type.GET);
			versionRequestIQ.setTo(spacesServiceJID);
			versionRequestIQ.setFrom(userInfo.getFullJID());
			
			RequestFuture<Element> iqFuture = new RequestFuture<Element>();
			
			pendingIQRequests.put(versionRequestIQ.getPacketID(), iqFuture);
			connection.sendPacket(versionRequestIQ);
			
			try {
				Element response = iqFuture.get(2000, TimeUnit.MILLISECONDS);
				if (response != null){
					if ("result".equals(response.getAttributeValue("type"))) {
						spacesServiceVersion = response.getChild("spaces", Namespace.getNamespace(NamespaceConfig.SPACES_SERVICE)).getChildText("version", Namespace.getNamespace(NamespaceConfig.SPACES_SERVICE));
					} else {
						logger.log(Level.WARNING, "Received error response on spaces service request.");
					}
				}
			} catch (InterruptedException e) {
				logger.log(Level.WARNING, "Receiving a response was interrupted.", e);
			} catch (ExecutionException e) {
				logger.log(Level.WARNING, "Failed to handle IQ response.", e);
			} catch (TimeoutException e) {
				logger.log(Level.WARNING, "IQ request timed out.", e);
			}
		}
		return new NetworkInformation(spacesServiceJID, spacesServiceVersion, persistenceServiceJID);
	}

	/**
	 * Tries to disconnect from the XMPP server.
	 */
	@Override
	public void disconnect() {
		if (status == ConnectionStatus.OFFLINE) {
			return;
		}
		setConnectionStatus(ConnectionStatus.PENDING);
		connection.removeConnectionListener(this);
		connection.disconnect();
		setConnectionStatus(ConnectionStatus.OFFLINE);
	}

	/**
	 * Returns the user information of the currently logged in user.
	 * @return User information.
	 */
	@Override
	public UserInfo getCurrentUser() {
		return userInfo;
	}
	
	@Override
	public NetworkInformation getNetworkInformation() {
		return networkInformation;
	}

	/**
	 * Returns the XMPPConnection object used by this implementation.
	 * @return SMACK XMPPConnection handler. 
	 */
	public XMPPConnection getXMPPConnection(){
		return connection;
	}
	
	/**
	 * Sets the connection status and notifies all connection status listeners.
	 */
	private void setConnectionStatus(ConnectionStatus newStatus) {
		this.status = newStatus;
		for (ConnectionStatusListener listener : listeners){
			listener.connectionStatusChanged(status);
		}
	}

	@Override
	public void connectionClosed() {
		setConnectionStatus(ConnectionStatus.OFFLINE);
	}

	@Override
	public void connectionClosedOnError(Exception e) {
		setConnectionStatus(ConnectionStatus.ERROR);
	}

	@Override
	public void reconnectingIn(int seconds) {
		setConnectionStatus(ConnectionStatus.ERROR);		
	}

	@Override
	public void reconnectionFailed(Exception e) {
		setConnectionStatus(ConnectionStatus.PENDING);
	}

	@Override
	public void reconnectionSuccessful() {
		setConnectionStatus(ConnectionStatus.ONLINE);
	}

	private void createUser() throws UnsupportedOperationException,
			EntityExistsException {
		if (status == ConnectionStatus.ONLINE) {
				throw new EntityExistsException("The user " + userInfo.getUsername() + " already exists.");
		}
		AccountManager manager = connection.getAccountManager();
		if (!manager.supportsAccountCreation()){
			setConnectionStatus(ConnectionStatus.ERROR);
			throw new UnsupportedOperationException("The server doesn't support account creation.");
		}
		try {
			manager.createAccount(userInfo.getUsername(), password);
		} catch (XMPPException e) {
			setConnectionStatus(ConnectionStatus.ERROR);
			switch (e.getXMPPError().getCode()){
			case 409:
				throw new EntityExistsException("The user " + userInfo.getUsername() + " already exists.", e);
			case 406:
				throw new IllegalArgumentException("Not enough or malformed arguments to create user.", e);
			default:
				throw new UnsupportedOperationException("The user couldn't be created.", e);
			}
		}
	}
}
