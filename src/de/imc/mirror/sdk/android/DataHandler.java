package de.imc.mirror.sdk.android;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.XMLOutputter;
import org.jivesoftware.smack.PacketInterceptor;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smackx.pubsub.ItemPublishEvent;
import org.jivesoftware.smackx.pubsub.ItemsExtension;
import org.jivesoftware.smackx.pubsub.LeafNode;
import org.jivesoftware.smackx.pubsub.Node;
import org.jivesoftware.smackx.pubsub.PayloadItem;
import org.jivesoftware.smackx.pubsub.PubSubElementType;
import org.jivesoftware.smackx.pubsub.PubSubManager;
import org.jivesoftware.smackx.pubsub.SimplePayload;
import org.jivesoftware.smackx.pubsub.Subscription;
import org.jivesoftware.smackx.pubsub.listener.ItemEventListener;
import org.jivesoftware.smackx.pubsub.packet.PubSub;

import android.util.Log;
import de.imc.mirror.sdk.ConnectionStatus;
import de.imc.mirror.sdk.ConnectionStatusListener;
import de.imc.mirror.sdk.DataObject;
import de.imc.mirror.sdk.DataObjectFilter;
import de.imc.mirror.sdk.DataObjectListener;
import de.imc.mirror.sdk.OfflineModeHandler;
import de.imc.mirror.sdk.SerializableDataObjectFilter;
import de.imc.mirror.sdk.Space;
import de.imc.mirror.sdk.Space.PersistenceType;
import de.imc.mirror.sdk.SpaceChannel;
import de.imc.mirror.sdk.config.NamespaceConfig;
import de.imc.mirror.sdk.exceptions.ConnectionStatusException;
import de.imc.mirror.sdk.exceptions.InvalidDataException;
import de.imc.mirror.sdk.exceptions.QueryException;
import de.imc.mirror.sdk.exceptions.SpaceManagementException;
import de.imc.mirror.sdk.exceptions.SpaceManagementException.Type;
import de.imc.mirror.sdk.exceptions.UnknownEntityException;
import de.imc.mirror.sdk.android.exceptions.RequestException;
import de.imc.mirror.sdk.android.packet.DeleteRequestIQ;
import de.imc.mirror.sdk.android.packet.DeleteResponseIQ;
import de.imc.mirror.sdk.android.packet.QueryRequestIQ;
import de.imc.mirror.sdk.android.packet.QueryResponseIQ;


/**
 * Handler for the data exchange over MIRROR spaces.
 * One data handler may observe several spaces, with the restriction to one domain.
 * The class provides offline functionality, which can be accessed over the methods specified in
 * the {@link OfflineModeHandler} interface.
 * @author mach
 *
 */
public class DataHandler implements OfflineModeHandler, de.imc.mirror.sdk.DataHandler, ItemEventListener<PayloadItem<SimplePayload>> {
	
	private int timeout = 2000;
	private ConnectionHandler connectionHandler;
	private XMPPConnection connection;
	private ConnectionStatusListener connectionStatusListener;
	private boolean isConnectionResetted;
	private SpaceHandler spaceHandler;
	private de.imc.mirror.sdk.UserInfo userInfo;
	private List<DataObjectListener> listeners;
	private DataWrapper datawrapper;
	private List<Space> handledSpaces;
	private Mode userWantedMode;
	private Mode realMode;
	
	private de.imc.mirror.sdk.DataObjectFilter dataObjectFilter;
	

	private Map<String, RequestFuture<IQ>> pendingPersistenceServiceQueries;
	private Map<String, RequestFuture<List<PayloadItem<SimplePayload>>>> pendingPayloadRequests;
	private Map<String, RequestFuture<IQ>> pendingPublishingRequests; // <pubsub item id, request>
	private Map<String, String> publishIdMap; // <iq packet id, pubsub item id>
	private Map<String, PacketListener> pubsubServiceListeners;
	private Map<String, PubSubManager> pubsubManagers;
	private PacketInterceptor pubsubInterceptor;
	
	private Map<String, PacketListener> persistenceServiceListeners;

	
	/**
	 * Creates a new data handler.
	 * @param connectionHandler The connection handler to be used for requests.
	 * @param spaceHandler An instance of a space handler to be used for requesting space properties.
	 */
	public DataHandler(ConnectionHandler connectionHandler, SpaceHandler spaceHandler){
		if (connectionHandler == null || spaceHandler == null){
			throw new IllegalArgumentException("None of the Arguments may be null");
		}
		this.connectionHandler = connectionHandler;
		this.isConnectionResetted = true;
		this.spaceHandler = spaceHandler;

		this.timeout = connectionHandler.getConfiguration().requestTimeout();
		this.userWantedMode = Mode.OFFLINE;
		this.connection = this.connectionHandler.getXMPPConnection();
		this.listeners = new ArrayList<DataObjectListener>();
		this.handledSpaces = new ArrayList<Space>();
		this.datawrapper = DataWrapper.getInstance();
		
		this.pendingPayloadRequests = new HashMap<String, RequestFuture<List<PayloadItem<SimplePayload>>>>();
		this.pendingPublishingRequests = new HashMap<String, RequestFuture<IQ>>();
		this.pendingPersistenceServiceQueries = new HashMap<String, RequestFuture<IQ>>();
		this.userInfo = this.connectionHandler.getCurrentUser();
		this.dataObjectFilter = null;
		
		this.publishIdMap = new HashMap<String, String>();
		
		this.pubsubServiceListeners = new HashMap<String, PacketListener>();
		this.persistenceServiceListeners = new HashMap<String, PacketListener>();
		this.pubsubManagers = new HashMap<String, PubSubManager>();
		
		setPubSubPacketInterceptor();
		setConnectionStatusListener();
	}
	
	private void setConnectionStatusListener() {
		connectionStatusListener = new ConnectionStatusListener() {
			
			@Override
			public void connectionStatusChanged(ConnectionStatus newStatus) {
				switch (newStatus) {
				case OFFLINE:
					isConnectionResetted = true;
					break;
				case ONLINE:
					if (isConnectionResetted) {
						changeConnectionHandler(connectionHandler);
						for (Space space : handledSpaces) {
							registerItemEventListener(space);
						}
						isConnectionResetted = false;
					}
					try {
						sendSavedPayloads();
					} catch (UnknownEntityException e) {
						Log.w("DataHandler", "An UnknownEntityException was thrown while trying to send saved payloads.", e);
					} catch (InvalidDataException e) {
						Log.w("DataHandler", "A cached data object could not be published as it was rejected.", e);
					}
					break;
				default:
					// Do nothing.
				}
			}
		};	
		connectionHandler.addConnectionStatusListener(connectionStatusListener);
		
		switch (connectionHandler.getStatus()) {
		case ONLINE:
			this.realMode = Mode.ONLINE;
			break;
		default:
			this.realMode = Mode.OFFLINE;
			break;
		}
	}
	
	/**
	 * Registers a packet listener for the given pubsub component and instantiates a pubsub manager.
	 * If the given component is already registered, nothing happens.
	 * @param componentJID JID of the pubsub service component in the XMPP network, e.g. pubsub.mydomain.com.
	 */
	private void registerPubsubService(final String componentJID) {
		if (pubsubServiceListeners.containsKey(componentJID)) {
			return;
		}
		
		PacketFilter packetFilter = new PacketFilter() {
			@Override
			public boolean accept(Packet packet) {
				if (!(packet instanceof IQ)) return false; // Accept only IQ packets.
				if (!componentJID.equals(packet.getFrom())) return false; // Accept only packets for the given component.
				IQ.Type type = ((IQ) packet).getType();
				if (type != IQ.Type.RESULT && type != IQ.Type.ERROR) return false;
				return true;
			}
		};
		
		PacketListener packetListener = new PacketListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void processPacket(Packet packet) {
				String packetId = packet.getPacketID();
				if (publishIdMap.containsKey(packetId)) {
					pendingPublishingRequests.get(publishIdMap.get(packetId)).setResponse((IQ) packet);
				} else if (pendingPayloadRequests.containsKey(packetId)) {
					RequestFuture<List<PayloadItem<SimplePayload>>> spaceFuture = pendingPayloadRequests.get(packet.getPacketID());
					ItemsExtension itemsElem = (ItemsExtension) ((PubSub)packet).getExtension(PubSubElementType.ITEMS);
					spaceFuture.setResponse((List<PayloadItem<SimplePayload>>) itemsElem.getItems());
				}
			}
		};
		
		pubsubServiceListeners.put(componentJID, packetListener);
		connection.addPacketListener(packetListener, packetFilter);
		
		pubsubManagers.put(componentJID, new PubSubManager(connection, componentJID));
	}

	
	/**
	 * Removes a packet listener from the given pubsub component and deleted the pubsub manager instance.
	 * If the given component is not registered, nothing happens.
	 * @param componentJID JID of the pubsub service component in the XMPP network, e.g. pubsub.mydomain.com.
	 */
	private void removePubsubService(String componentJID) {
		PacketListener packetListener = pubsubServiceListeners.get(componentJID);
		if (packetListener == null) {
			return;
		}
		
		connection.removePacketListener(packetListener);
		pubsubServiceListeners.remove(componentJID);
		
		pubsubManagers.remove(componentJID);
	}
	
	/**
	 * Registers a packet listener for the given persistence service component.
	 * If the given component is already registered, nothing happens.
	 * @param componentJID JID of the persistence service component in the XMPP network, e.g. persistence.mydomain.com.
	 */
	private void registerPersistenceService(final String componentJID) {
		if (persistenceServiceListeners.containsKey(componentJID)) {
			return;
		}
		
		PacketFilter packetFilter = new PacketFilter() {
			@Override
			public boolean accept(Packet packet) {
				if (!(packet instanceof IQ)) return false; // Accept only IQ packets.
				if (!componentJID.equals(packet.getFrom())) return false; // Accept only packets for the given component.
				IQ.Type type = ((IQ) packet).getType();
				if (type != IQ.Type.RESULT && type != IQ.Type.ERROR) return false;
				return true;
			}
		};
		
		PacketListener packetListener = new PacketListener() {
			@Override
			public void processPacket(Packet packet) {
				String packetId = packet.getPacketID();
				if (pendingPersistenceServiceQueries.containsKey(packetId)) {
					RequestFuture<IQ> iqFuture = (RequestFuture<IQ>) pendingPersistenceServiceQueries.get(packetId);
					pendingPersistenceServiceQueries.remove(packetId);
					iqFuture.setResponse((IQ) packet);
				}
			}
		};
		
		persistenceServiceListeners.put(componentJID, packetListener);
		connection.addPacketListener(packetListener, packetFilter);
	}
	
	/**
	 * Removes a packet listener from the given persistence service component.
	 * If the given component is not registered, nothing happens.
	 * @param componentJID JID of the persistence service component in the XMPP network, e.g. persistence.mydomain.com.
	 */
	private void removePersistenceService(String componentJID) {
		PacketListener packetListener = persistenceServiceListeners.get(componentJID);
		if (packetListener == null) {
			return;
		}
		
		connection.removePacketListener(packetListener);
		persistenceServiceListeners.remove(componentJID);
	}
	
	/**
	 * Registers a packet interceptor for pubsub publishing requests.
	 * When a publishing request is intercepted, the related request future is complemented by the IQ packet id.
	 */
	private void setPubSubPacketInterceptor() {
		PacketFilter packetFilter = new PacketFilter() {
			@Override
			public boolean accept(Packet packet) {
				if (!(packet instanceof IQ)) return false;
				IQ iqPacket = (IQ) packet;
				if (iqPacket.getType() != IQ.Type.SET) return false; 
				if (packet.getExtension(NamespaceConfig.XMPP_PUBSUB) == null) return false;
				return true;
			}
		};
		pubsubInterceptor = new PacketInterceptor() {
			@Override
			public void interceptPacket(Packet packet) {
				IQ iq = (IQ) packet;
				String childElement = iq.getChildElementXML();
				for (String itemId : pendingPublishingRequests.keySet()) {
					if (childElement.contains(itemId)) {
						publishIdMap.put(iq.getPacketID(), itemId);
					}
				}
			}
		};
		connection.addPacketInterceptor(pubsubInterceptor, packetFilter);
	}

	/**
	 * Adds a listener for data objects published on any space handled by this handler.
	 * @param listener Listener to add.
	 * The call-back method of listener is called each time a new item is published on the pubsub node of a MIRROR space.   
	 */
	@Override
	public void addDataObjectListener(DataObjectListener listener){
		if (!listeners.contains(listener))
			listeners.add(listener);
	}
	
	/**
	 * Retrieves a pubsub node from the pubsub service.
	 * @param nodeId The id of the node to retrieve.
	 * @param pubsubService JID of the pubsub service component handling the the node.
	 * @return The pubsub node.
	 * @throws UnknownEntityException No node with the given ID exists.
	 */
	protected Node getNode(String nodeId, String pubsubService) throws UnknownEntityException {
		if (!pubsubManagers.containsKey(pubsubService)) {
			registerPubsubService(pubsubService);
		}
		PubSubManager manager = pubsubManagers.get(pubsubService);
		
		try {
			return manager.getNode(nodeId);
		} catch(XMPPException e){
			throw new UnknownEntityException("The node " + nodeId + " couldn't be retrieved.", e);
		}
	}
	
	/**
	 * Item handler of this data handler implementation. Checks if received items are already cached,
	 * if not, it caches them. Ultimately it sends the items to the subscribed listeners.
	 */
	@Override
	public void handlePublishedItems(ItemPublishEvent<PayloadItem<SimplePayload>> items){
		String spaceId = getSpaceId(items.getNodeId());
		if (spaceId == null) return;
		List<String> toRequest = new ArrayList<String>();
		for (PayloadItem<SimplePayload> item:items.getItems()){
        	if (item.getPayload() == null && !pendingPayloadRequests.containsKey(item.getId())){
        		toRequest.add(item.getId());
        		continue;
        	}
        	DataObject obj = parseItemToDataObject(item);
        	if (dataObjectFilter == null || dataObjectFilter.isDataObjectValid(obj)) {
				if (!datawrapper.isDataObjectAlreadyCached(item.getId())){
					datawrapper.saveDataObject(items.getNodeId(), obj, item.getId());
				}
				for (DataObjectListener listener:listeners){
					listener.handleDataObject(obj, spaceId);
				}
			}
		}
		if (!toRequest.isEmpty()){
			for (String id:toRequest){
				try {
					retrievePayload(items.getNodeId(), id);
				} catch (Exception e) {
					//Ignore
					continue;
				}
			}
		}
	}
	
	/**
	 * Gets the id of the space with the given nodeId.
	 * @param nodeId The nodeId to look for.
	 * @return A spaceId or <code>null</code> if no space was found.
	 */
	private String getSpaceId(String nodeId){
		for (Space space:handledSpaces){
			Map<String, String> prop = space.getPubSubChannel().getProperties();
			if (prop.get("node").equals(nodeId)){
				return space.getId();
			}
		}
		return null;
	}

	/**
	 * Publishes a data object on the space with the given id.
	 * ONLINE mode: The object is directly published on the related pubsub node.
	 * OFFLINE mode: The object is stored locally and published when the connection is establised again.
	 * @param object Data object to publish.
	 * @param spaceId Identifier if the space to publish.
	 * @throws UnknownEntityException A space with the given id is not known to the space handler.
	 * @throws InvalidDataException The data was rejected by the spaces service.
	 */
	@Override
	public void publishDataObject(DataObject object, String spaceId) throws UnknownEntityException, InvalidDataException {
		de.imc.mirror.sdk.android.DataObject obj = (de.imc.mirror.sdk.android.DataObject) object;
		SimplePayload payload = new SimplePayload(obj.getElementName(), 
				obj.getNamespaceURI(), obj.toString());
		publish(spaceId, payload);
	}
	
	/**
	 * Publishes a data objects and returns the object sent over the space.
	 * Use this method to access fields set server-side like the data object identifier.  
	 * @param dataObject Data object to publish.
	 * @param spaceId Identifier if the space to publish.
	 * @return Data object as published on the space.
	 * @throws UnknownEntityException A space with the given id is not known to the space handler.
	 * @throws InvalidDataException The data was rejected by the spaces service.
	 * @throws ConnectionStatusException The data handler has to be online in order to make a synchronous call.
	 */
	public DataObject publishAndRetrieveDataObject(DataObject dataObject, String spaceId) throws UnknownEntityException, InvalidDataException, ConnectionStatusException {
		SimplePayload payload = new SimplePayload(dataObject.getElement().getName(), dataObject.getNamespaceURI(), dataObject.toString());
		return publishAndRetrieve(spaceId, payload);
	}
	
	/**
	 * If online, the method publishs a payload.
	 * If offline, it only saves the payload in the sendcache.
	 * @param spaceId The id of the space to send the payload to.
	 * @param payload The payload to send.
	 * @throws UnknownEntityException Thrown when given space id cannot be mapped.
	 * @throws InvalidDataException The data was rejected by the spaces service. 
	 * @throws RequestException The request failed.
	 */
	private void publish(String spaceId, SimplePayload payload) throws UnknownEntityException, InvalidDataException, RequestException {
		if (getMode() == Mode.ONLINE) {
			SpaceChannel channel = spaceHandler.getPubSubChannel(spaceId);
			Map<String, String> properties = channel.getProperties();
			LeafNode node = (LeafNode) getNode(properties.get("node"), properties.get("domain"));
			if (node == null) {
				throw new UnknownEntityException("There's no node with this id.");
			}
			String itemId = UUID.randomUUID().toString();
			PayloadItem<SimplePayload> item = new PayloadItem<SimplePayload>(itemId, payload);
			RequestFuture<IQ> requestFuture = new RequestFuture<IQ>();
			pendingPublishingRequests.put(itemId, requestFuture);
			node.publish(item);
			try {
				IQ response = requestFuture.get(timeout, TimeUnit.MILLISECONDS);
				XMPPError error = response.getError(); 
				if (error != null) {
					throw new InvalidDataException(error.getMessage());
				}
			} catch (InterruptedException e){
				throw new RequestException("Receiving a response was interrupted.", e);
			} catch (ExecutionException e) {
				throw new RequestException("Couldn't receive a response.", e);
			} catch (TimeoutException e) {
				throw new RequestException("Receiving a response timed out.", e);
			}
		}
		else {
			String id = UUID.randomUUID().toString();
			datawrapper.savePayloadToSend(userInfo.getBareJID(), id, spaceId, payload);
		}
	}
	
	private DataObject publishAndRetrieve(String spaceId, SimplePayload payload) throws UnknownEntityException, ConnectionStatusException, InvalidDataException {
		if (getMode() != Mode.ONLINE) {
			throw new ConnectionStatusException("The data handler has to be ONLINE to publish data objects synchronously.");
		}
		SpaceChannel channel = spaceHandler.getPubSubChannel(spaceId);
		Map<String, String> properties = channel.getProperties();
		LeafNode node = (LeafNode) getNode(properties.get("node"), properties.get("domain"));
		final String itemId = UUID.randomUUID().toString();
		final RequestFuture<DataObject> requestFuture = new RequestFuture<DataObject>();
		final PayloadItem<SimplePayload> itemToPublish = new PayloadItem<SimplePayload>(itemId, payload);
		
		ItemEventListener<PayloadItem<SimplePayload>> itemEventListener = new ItemEventListener<PayloadItem<SimplePayload>>() {
			@Override
			public void handlePublishedItems(ItemPublishEvent<PayloadItem<SimplePayload>> event) {
				for (PayloadItem<SimplePayload> item : event.getItems()) {
					if (itemToPublish.getId().equals(item.getId())) {
			        	DataObject dataObject = parseItemToDataObject(item);
			        	requestFuture.setResponse(dataObject);
					}
				}
			}
		}; 
		node.addItemEventListener(itemEventListener);
		
		RequestFuture<IQ> sentNotificationFuture = new RequestFuture<IQ>();
		pendingPublishingRequests.put(itemId, sentNotificationFuture);
		node.publish(itemToPublish);
		try {
			IQ response = sentNotificationFuture.get(timeout, TimeUnit.MILLISECONDS);
			XMPPError error = response.getError(); 
			if (error != null) {
				throw new InvalidDataException(error.getMessage());
			}
		} catch (InterruptedException e){
			throw new RequestException("Failed to sent data object: Receiving a response was interrupted.", e);
		} catch (ExecutionException e) {
			throw new RequestException("Failed to sent data object: Couldn't receive a response.", e);
		} catch (TimeoutException e) {
			throw new RequestException("Failed to sent data object: Receiving a response timed out.", e);
		}
		
		DataObject dataObject = null;
		try {
			dataObject = requestFuture.get(timeout, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			throw new RequestException("Receiving a response was interrupted.", e);
		} catch (ExecutionException e) {
			throw new RequestException("Couldn't receive a response.", e);
		} catch (TimeoutException e) {
			throw new RequestException("The request timed out.", e);
		} finally {
			node.removeItemEventListener(itemEventListener);
		}
		
		return dataObject;
	}

	/**
	 * Removes a data object listener.
	 * @param listener Listener to remove.
	 */
	@Override
	public void removeDataObjectListener(DataObjectListener listener){
		listeners.remove(listener);
	}
	
	/**
	 * Gets the pubsub service of a pubsub node.
	 * @param nodeId The id of the node to get the pubsub service for.
	 * @return The pubsubservice.
	 */
	protected String getPubsubService(String nodeId){
		String pubsubService = null;
		List<Space> spaces = spaceHandler.getAllSpaces();
		for(Space space:spaces){
			SpaceChannel pubsub = space.getPubSubChannel();
			if (pubsub.getProperties().get("node").equalsIgnoreCase(nodeId))
				return pubsubService = pubsub.getProperties().get("domain");
		}
		return pubsubService;
	}

	/**
	 * Sends a request to the server to retrieve the payload of an item
	 * @param nodeId The id of the node the item is from.
	 * @param payloadId The id of the payload.
	 * @throws SpaceManagementException Thrown when an error occured while retrieving the payload from the server.
	 * @throws UnknownEntityException Thrown when no pubsub node could be retrieved.
	 */
	private void retrievePayload(final String nodeId, final String payloadId) throws SpaceManagementException, UnknownEntityException{
		if (getMode() == Mode.OFFLINE){
			throw new IllegalStateException("Not connected");
		}
		String pubsubService = getPubsubService(nodeId);
		if (pubsubService == null){
			return;
		}
		Node node = this.getNode(nodeId, pubsubService);
		List<Subscription> subs;
		try {
			subs = node.getSubscriptions();
		} catch (XMPPException e) {
			throw new SpaceManagementException("The Server did not respond.", Type.OTHER, e);
		}
		final String subid = subs.get(0).getId();
		IQ requestIq = new IQ() {
			
			@Override
			public String getChildElementXML() {
				Element childElement = new Element("pubsub", NamespaceConfig.XMPP_PUBSUB);
				Element itemsElement = new Element("items").setAttribute("node", nodeId);
				itemsElement.setAttribute("subid", subid);
				itemsElement.addContent(new Element("item").setAttribute("id", payloadId));
				childElement.addContent(itemsElement);
				XMLOutputter out = new XMLOutputter();
				return out.outputString(childElement);
			}
		};
		requestIq.setType(IQ.Type.GET);
		requestIq.setTo(pubsubService);
		requestIq.setPacketID(payloadId);
		RequestFuture<List<PayloadItem<SimplePayload>>> payloadFuture = new RequestFuture<List<PayloadItem<SimplePayload>>>();
		pendingPayloadRequests.put(payloadId, payloadFuture);
		connection.sendPacket(requestIq);
		List<PayloadItem<SimplePayload>> response = null;
		try {
			response = payloadFuture.get(timeout, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			throw new SpaceManagementException("Receiving a response was interrupted.", SpaceManagementException.Type.OTHER, e);
		} catch (ExecutionException e) {
			throw new SpaceManagementException("Couldn't receive a response.", SpaceManagementException.Type.OTHER, e);
		} catch (TimeoutException e) {
			throw new SpaceManagementException("Receiving a response timed out.", SpaceManagementException.Type.OTHER, e);
		}
		if (response == null){
			return;
		}
		ItemPublishEvent<PayloadItem<SimplePayload>> event =
			new ItemPublishEvent<PayloadItem<SimplePayload>>(nodeId, response);
		handlePublishedItems(event);
	}
	
	/**
	 * Sends the payloads of the user, which are in the sendcache.
	 * @throws UnknownEntityException Thrown when no pubsub node could be retrieved.
	 */
	private void sendSavedPayloads() throws UnknownEntityException, InvalidDataException {
		if (datawrapper == null) return;
		Map<String, SimplePayload> payloads = datawrapper.getPayloadsToSend(userInfo.getBareJID());
		for (String id:payloads.keySet()){
			String spaceId = datawrapper.getSpaceForPayload(id);
			if (spaceId == null) continue;
			publish(spaceId, payloads.get(id));
		}
		datawrapper.clearSendCache(userInfo.getBareJID());
	}

	/**
	 * Changes the connection handler used for XMPP requests.
	 * @param connectionHandler The connection handler to be used for requests.
	 */
	public void changeConnectionHandler(ConnectionHandler connectionHandler) {
		// Unregister pubsub services.
		List<String> registeredPubsubServiceComponents = new ArrayList<String>(pubsubServiceListeners.keySet());
		for (String componentJID : registeredPubsubServiceComponents) {
			removePubsubService(componentJID);
		}
		connection.removePacketInterceptor(pubsubInterceptor);
		
		// Unregister persistence services.
		List<String> registeredPersistenceServiceComponents = new ArrayList<String>(persistenceServiceListeners.keySet());
		for (String componentJID : registeredPersistenceServiceComponents) {
			removePersistenceService(componentJID);
		}
		
		// Unregister connection status listner.
		connectionHandler.removeConnectionStatusListener(connectionStatusListener);
		
		// Update handler.
		this.connectionHandler = connectionHandler;
		this.connection = connectionHandler.getXMPPConnection();
		
		// Register pubsub services.
		for (String componentJID : registeredPubsubServiceComponents) {
			registerPubsubService(componentJID);
		}
		this.setPubSubPacketInterceptor();
		
		// Register persistence services.
		for (String componentJID : registeredPersistenceServiceComponents) {
			registerPersistenceService(componentJID);
		}
		
		// Register connection status listener.
		setConnectionStatusListener();
	}
	
	/**
	 * Returns the list of spaces handled by this handler.
	 * @return Unmodifiable list of spaces. May be empty.
	 */
	@Override
	public List<Space> getHandledSpaces() {
		return Collections.unmodifiableList(handledSpaces);
	}

	/**
	 * Adds a space to be observed by this data handler.
	 * If the space is already in the list of observed spaces, nothing happens.
	 * @param spaceId Identifier of the space to observe.
	 * @throws UnknownEntityException Failed to register to the pubsub node of the space.
	 */
	@Override
	public void registerSpace(String spaceId) throws UnknownEntityException {
		Space space = null;
		for (Space cachedSpace : spaceHandler.getCachedSpaces()) {
			if (spaceId.equals(cachedSpace.getId())) {
				space = cachedSpace;
				break;
			}
		}
		if (space == null) space = spaceHandler.getSpace(spaceId);
		if (space == null) {
			throw new UnknownEntityException("Space could not be found: " + spaceId);
		}
		if (handledSpaces.contains(space)) {
			return;
		} else {
			handledSpaces.add(space);
			if (getMode() == Mode.ONLINE){
				registerItemEventListener(space);
			}
		}
	}

	/**
	 * Removes a space from the list of observed spaces.
	 * If the space is not in the list of observed spaces, nothing happens.
	 * @param spaceId Identifier of the space to observe.
	 */
	@Override
	public void removeSpace(String spaceId) {
		Space space = spaceHandler.getSpace(spaceId);
		if (handledSpaces.remove(space)){
			SpaceChannel channel = space.getPubSubChannel();
			Map<String, String> properties = channel.getProperties();
			datawrapper.deleteCachedDataObjectsForSpace(properties.get("node"));
			if (getMode() == Mode.ONLINE){
				try {
					LeafNode node = (LeafNode) getNode(properties.get("node"), properties.get("domain"));
					node.removeItemEventListener(this);
				} catch (UnknownEntityException e) {
					Log.d("DATAHANDLER", "An UnknownEntityException was thrown while removing a registered space.", e);
				}
			}
		}
	}

	/**
	 * Returns a unmodifiable list of data objects previously published on the space.
	 * The data objects are retrieved from the local cache and contain the latest items published.
	 * The amount of objects cache depend on the handler implementation.
	 * @param spaceId Identifier of the space to retrieve data objects for.
	 * @return List of data objects, in reverse order of their publishing time, i.e., the latest first. May be empty.
	 * @throws UnknownEntityException A space with the given id is not known to the space handler.
	 * @deprecated Use {@link DataHandler#queryDataObjectsBySpace(String, Set)} instead.
	 */
	@Override
	@Deprecated
	public List<DataObject> retrieveDataObjects(String spaceId) throws UnknownEntityException {
		SpaceChannel channel = spaceHandler.getPubSubChannel(spaceId);
		Map<String, String> properties = channel.getProperties();
		String nodeId = properties.get("node");
		List<DataObject> objs = datawrapper.getCachedDataObjects(nodeId);
		return Collections.unmodifiableList(objs);
	}

	/**
	 * Convenience method to register the DataHandler as an ItemEventListener to a node of a space.
	 * @param space The space to which node the DataHandler should register.
	 * @return The pubsubnode of the space.
	 */
	private LeafNode registerItemEventListener(Space space) {
		SpaceChannel channel = space.getPubSubChannel();
		Map<String, String> properties = channel.getProperties();
		LeafNode node = null;
		try{
			node = (LeafNode) getNode(properties.get("node"), properties.get("domain"));
		} catch (UnknownEntityException e) {
			Log.d("DataHandler", "An Exception was thrown while trying to register the " +
					"DataHandler to the nodes of the registered Spaces.", e);
		}
		if (node != null){
			node.addItemEventListener(this);
		}
		return node;
	}
	
	/**
	 * Convenience method to parse an PayloadItem to an DataObject.
	 * @param item The Item to parse.
	 * @return The parsed DataObject.
	 */
	private DataObject parseItemToDataObject(PayloadItem<SimplePayload> item){
		SAXBuilder reader = new SAXBuilder();
		StringReader in = new StringReader(item.getPayload().toXML());
		Document document = null;
		try {
		document = reader.build(in);
		} catch (JDOMException e) {
			Log.d("DataHandler", "An JDOMException was thrown while parsing a newly gotten item.", e);
		} catch (IOException e) {
			Log.d("DataHandler", "An IOException was thrown while parsing a newly gotten item.", e);
		}
		if (document == null){
			return null;
		}
		Element elem = document.getRootElement();
		DataObject obj = new DataObjectBuilder(elem, item.getPayload().getNamespace()).build();
		return obj;
	}

	/**
	 * Sets the mode for the handler.
	 * @param mode Mode to set. 
	 */
	@Override
	public void setMode(Mode mode){
		this.userWantedMode = mode;
		if (userWantedMode == Mode.ONLINE && this.getMode() == Mode.ONLINE) {
			try {
				sendSavedPayloads();
			} catch (UnknownEntityException e) {
				Log.w("DataHandler", "An UnknownEntityException was thrown while trying to send saved payloads.", e);
			} catch (InvalidDataException e) {
				Log.w("DataHandler", "A cached data object could not be published as it was rejected.", e);
			}
		}
	}

	/**
	 * Returns the current mode of the handler.
	 * Note: the current mode is dependant on the mode the user set and the current connectionstatus.
	 * @return Mode.ONLINE or Mode.OFFLINE.
	 */
	@Override
	public Mode getMode() {
		if (userWantedMode == Mode.OFFLINE){
			return userWantedMode;
		} else {
			return realMode;
		}
	}
	
	/**
	 * Deletes locally stored data.
	 */
	@Override
	public void clear() {
		datawrapper.clearDataCache();		
	}

	@Override
	public DataObjectFilter getDataObjectFilter() {
		return dataObjectFilter;
	}

	@Override
	public void setDataObjectFilter(DataObjectFilter filter) {
		this.dataObjectFilter = filter;
	}

	@Override
	public DataObject queryDataObjectById(final String objectId) throws UnsupportedOperationException, ConnectionStatusException, QueryException {
		checkPersistenceServiceAvailabilty();
		
		IQ requestIQ = QueryRequestIQ.createQueryByObjectId(objectId);
		requestIQ.setFrom(userInfo.getFullJID());
		requestIQ.setTo(connectionHandler.getNetworkInformation().getPersistenceServiceJID());
		RequestFuture<IQ> queryFuture = new RequestFuture<IQ>();
		pendingPersistenceServiceQueries.put(requestIQ.getPacketID(), queryFuture);
		connection.sendPacket(requestIQ);
		
		List<DataObject> resultList = handleQueryResponse(queryFuture);
		return resultList.isEmpty() ? null : resultList.get(0);
	}

	@Override
	public List<DataObject> queryDataObjectsById(Set<String> objectIds, Set<SerializableDataObjectFilter> filters) throws UnsupportedOperationException, ConnectionStatusException, QueryException {
		checkPersistenceServiceAvailabilty();
		
		IQ requestIQ = QueryRequestIQ.createQueryByObjectIds(objectIds, filters);
		requestIQ.setFrom(userInfo.getFullJID());
		requestIQ.setTo(connectionHandler.getNetworkInformation().getPersistenceServiceJID());
		RequestFuture<IQ> queryFuture = new RequestFuture<IQ>();
		pendingPersistenceServiceQueries.put(requestIQ.getPacketID(), queryFuture);
		connection.sendPacket(requestIQ);
		
		List<DataObject> dataObjects = handleQueryResponse(queryFuture);
		return dataObjects;
	}
	
	/**
	 * Queries data objects persisted on a pubsub node.
	 * This method is used as fallback if no persistence service is available.
	 * @param spaceId Identifier of the space to request data objects for.
	 * @param filters Filter to apply.
	 * @return List of data objects. The list is empty if the space is not configured to persist data objects permanently.
	 * @throws QueryException Failed to retrieve data objects from pubsub node.
	 */
	private List<DataObject> queryDataObjectsFromPubSubNode(String spaceId, Set<SerializableDataObjectFilter> filters) throws QueryException {
		List<DataObject> dataObjects = new ArrayList<DataObject>();
		Space space = spaceHandler.getSpace(spaceId);
		if (space == null) {
			throw new QueryException(QueryException.Type.BAD_REQUEST, "Unknown space id: " + spaceId);
		}
		if (space.getPersistenceType() != PersistenceType.ON) {
			// nothing persisted
			return dataObjects;
		}
		SpaceChannel spaceChannel = space.getPubSubChannel();
		String nodeId = spaceChannel.getProperties().get("node");
		String pubsubJID = spaceChannel.getProperties().get("domain");
		LeafNode node;
		try {
			node = (LeafNode) this.getNode(nodeId, pubsubJID);
			datawrapper.deleteCachedDataObjectsForSpace(node.getId());
			List<PayloadItem<SimplePayload>> items = ((LeafNode) node).getItems(node.getSubscriptions().get(0).getId());
			if (node != null && items != null) {
				for (PayloadItem<SimplePayload> item : items) {
					DataObject dataObject = this.parseItemToDataObject(item);
					if (!filters.isEmpty()) {
						boolean reject = false;
						for (DataObjectFilter filter : filters) {
							if (filter.isDataObjectValid(dataObject)) {
								reject = true;
								break;
							}
						}
						if (reject) continue;
					}
					dataObjects.add(dataObject);
					if (!datawrapper.isDataObjectAlreadyCached(item.getId())){
						datawrapper.saveDataObject(node.getId(), dataObject, item.getId());
					}
				} 
			}
		} catch (UnknownEntityException e) {
			throw new QueryException(QueryException.Type.FAILURE, "Failed to retrieve pubsub node.", e);
		} catch (XMPPException e) {
			throw new QueryException(QueryException.Type.FAILURE, "Failed to access pubsub node.", e);
		}
		return dataObjects;
	}

	@Override
	public List<DataObject> queryDataObjectsBySpace(String spaceId, Set<SerializableDataObjectFilter> filters) throws UnsupportedOperationException, ConnectionStatusException, QueryException {
		try {
			checkPersistenceServiceAvailabilty();
		} catch (UnsupportedOperationException e) {
			return queryDataObjectsFromPubSubNode(spaceId, filters);
		}
		
		IQ requestIQ = QueryRequestIQ.createQueryBySpace(spaceId, filters);
		requestIQ.setFrom(userInfo.getFullJID());
		requestIQ.setTo(connectionHandler.getNetworkInformation().getPersistenceServiceJID());
		
		RequestFuture<IQ> queryFuture = new RequestFuture<IQ>();
		pendingPersistenceServiceQueries.put(requestIQ.getPacketID(), queryFuture);
		connection.sendPacket(requestIQ);
		
		List<DataObject> dataObjects = handleQueryResponse(queryFuture);
		return dataObjects;
	}

	@Override
	public List<DataObject> queryDataObjectsBySpaces(Set<String> spaceIds, Set<SerializableDataObjectFilter> filters) throws UnsupportedOperationException, ConnectionStatusException, QueryException {
		checkPersistenceServiceAvailabilty();
		
		IQ requestIQ = QueryRequestIQ.createQueryBySpaces(spaceIds, filters);
		requestIQ.setFrom(userInfo.getFullJID());
		requestIQ.setTo(connectionHandler.getNetworkInformation().getPersistenceServiceJID());
		
		RequestFuture<IQ> queryFuture = new RequestFuture<IQ>();
		pendingPersistenceServiceQueries.put(requestIQ.getPacketID(), queryFuture);
		connection.sendPacket(requestIQ);
		
		List<DataObject> dataObjects = handleQueryResponse(queryFuture);
		return dataObjects;
	}
	
	@Override
	public boolean deleteDataObject(String objectId) throws UnsupportedOperationException, ConnectionStatusException, QueryException  {
		checkPersistenceServiceAvailabilty();
		
		IQ requestIq = DeleteRequestIQ.createDeleteRequest(objectId);
		requestIq.setFrom(userInfo.getFullJID());
		requestIq.setTo(connectionHandler.getNetworkInformation().getPersistenceServiceJID());
		
		RequestFuture<IQ> queryFuture = new RequestFuture<IQ>();
		pendingPersistenceServiceQueries.put(requestIq.getPacketID(), queryFuture);
		connection.sendPacket(requestIq);
		
		int numberOfDeletedObjects = handleDeleteResponse(queryFuture);
		return numberOfDeletedObjects > 0;
	}
	
	@Override
	public int deleteDataObjects(Set<String> objectIds) throws UnsupportedOperationException, ConnectionStatusException, QueryException {
		checkPersistenceServiceAvailabilty();
		
		IQ requestIq = DeleteRequestIQ.createDeleteRequest(objectIds);
		requestIq.setFrom(userInfo.getFullJID());
		requestIq.setTo(connectionHandler.getNetworkInformation().getPersistenceServiceJID());
		
		RequestFuture<IQ> queryFuture = new RequestFuture<IQ>();
		pendingPersistenceServiceQueries.put(requestIq.getPacketID(), queryFuture);
		connection.sendPacket(requestIq);
		
		int numberOfDeletedObjects = handleDeleteResponse(queryFuture);
		return numberOfDeletedObjects;
	}

	private List<DataObject> handleQueryResponse(RequestFuture<IQ> queryFuture) throws QueryException {
		List<DataObject> dataObjects;
		
		try {
			IQ responseIQ = queryFuture.get(2000, TimeUnit.MILLISECONDS);
			if (responseIQ.getType() == IQ.Type.ERROR) {
				XMPPError error = responseIQ.getError();
				if (XMPPError.Condition.not_allowed.toString().equalsIgnoreCase(error.getCondition())) {
					throw new QueryException(QueryException.Type.ACCESS_DENIED, "The currrent user is not allowed to access this data object.");
				} else {
					throw new QueryException(QueryException.Type.FAILURE, "Failed to perform query. " + error.getCondition() + ": " + error.getMessage());
				}
			} else if (responseIQ instanceof QueryResponseIQ) {
				dataObjects = ((QueryResponseIQ) responseIQ).getResult();
			} else {
				throw new QueryException(QueryException.Type.FAILURE, "Invalid query iq type as response: " + responseIQ.getChildElementXML());
			}
		} catch (InterruptedException e) {
			throw new QueryException(QueryException.Type.FAILURE, "The request was interrupted.", e);
		} catch (ExecutionException e) {
			throw new QueryException(QueryException.Type.FAILURE, "The request handling procedure caused an excution error.", e);
		} catch (TimeoutException e) {
			throw new QueryException(QueryException.Type.FAILURE, "The request timed out.", e);
		}
		
		return dataObjects;
	}
	
	private int handleDeleteResponse(RequestFuture<IQ> queryFuture) throws QueryException {
		int numberOfDeletedObjects;
		try {
			IQ responseIQ = queryFuture.get(2000, TimeUnit.MILLISECONDS);
			if (responseIQ.getType() == IQ.Type.ERROR) {
				XMPPError error = responseIQ.getError();
				if (XMPPError.Condition.not_allowed.toString().equalsIgnoreCase(error.getCondition())) {
					throw new QueryException(QueryException.Type.ACCESS_DENIED, "The currrent user is not allowed to access this data object.");
				} else {
					throw new QueryException(QueryException.Type.FAILURE, "Failed to perform query. " + error.getCondition() + ": " + error.getMessage());
				}
			} else if (responseIQ instanceof DeleteResponseIQ) {
				numberOfDeletedObjects = ((DeleteResponseIQ) responseIQ).getNumberOfDeletedEntries();
			} else {
				throw new QueryException(QueryException.Type.FAILURE, "Invalid query iq type as response: " + responseIQ.getChildElementXML());
			}
		} catch (InterruptedException e) {
			throw new QueryException(QueryException.Type.FAILURE, "The request was interrupted.", e);
		} catch (ExecutionException e) {
			throw new QueryException(QueryException.Type.FAILURE, "The request handling procedure caused an excution error.", e);
		} catch (TimeoutException e) {
			throw new QueryException(QueryException.Type.FAILURE, "The request timed out.", e);
		}
		
		return numberOfDeletedObjects;
	}

	/**
	 * Checks the availability of a persistence service and registers the service. 
	 * @throws ConnectionStatusException The data handler has to be online in order to query data.
	 * @throws UnsupportedOperationException The operation is not supported.
	 */
	private void checkPersistenceServiceAvailabilty() throws ConnectionStatusException, UnsupportedOperationException {
		if (connectionHandler.getStatus() != ConnectionStatus.ONLINE) {
			throw new ConnectionStatusException("You must be online in order to query data.");
		} else {
			String persistenceServiceJID = connectionHandler.getNetworkInformation().getPersistenceServiceJID();
			if (persistenceServiceJID != null) {
				registerPersistenceService(persistenceServiceJID);
			} else {
				throw new UnsupportedOperationException("No persistence service available.");
			}
		}
	}
	
}
