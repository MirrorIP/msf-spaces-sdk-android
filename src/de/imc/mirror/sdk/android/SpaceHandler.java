package de.imc.mirror.sdk.android;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;

import org.jdom2.Content;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.XMLOutputter;
import org.jdom2.util.IteratorIterable;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.FromContainsFilter;
import org.jivesoftware.smack.filter.IQTypeFilter;
import org.jivesoftware.smack.filter.OrFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.packet.DiscoverInfo;
import org.jivesoftware.smackx.packet.DiscoverItems;

import de.imc.mirror.sdk.ConnectionStatus;
import de.imc.mirror.sdk.ConnectionStatusListener;
import de.imc.mirror.sdk.OfflineModeHandler;
import de.imc.mirror.sdk.OrgaSpace;
import de.imc.mirror.sdk.Space;
import de.imc.mirror.sdk.Space.PersistenceType;
import de.imc.mirror.sdk.SpaceChannel;
import de.imc.mirror.sdk.SpaceConfiguration;
import de.imc.mirror.sdk.SpaceMember;
import de.imc.mirror.sdk.SpaceMember.Role;
import de.imc.mirror.sdk.config.NamespaceConfig;
import de.imc.mirror.sdk.exceptions.ConnectionStatusException;
import de.imc.mirror.sdk.exceptions.SpaceManagementException;
import de.imc.mirror.sdk.exceptions.SpaceManagementException.Type;
import de.imc.mirror.sdk.exceptions.UnknownEntityException;

import android.content.Context;
import android.util.Log;


/**
 * This class provides methods to create, modify, delete and retrieve spaces.
 * The class provides offline functionality, which can be accessed over the methods specified in
 * the {@link OfflineModeHandler} interface.
 * @author mach
 *
 */
public class SpaceHandler implements de.imc.mirror.sdk.OfflineModeHandler, de.imc.mirror.sdk.SpaceHandler {
	private int timeout = 2000;
	
	private ConnectionHandler connectionHandler;
	private Mode userWantedMode;
	private Mode realMode;
	private XMPPConnection connection;
	private String domain;
	private de.imc.mirror.sdk.UserInfo userInfo;
	private Map<String, RequestFuture<Element>> pendingSpacesRequests;
	private List<Space> spaces;
	private DataWrapper datawrapper;
	
	private PacketListener packetListener = new PacketListener(){

		@Override
		public void processPacket(Packet packet) {
			if (pendingSpacesRequests.containsKey(packet.getPacketID())) {
				RequestFuture<Element> spaceFuture = pendingSpacesRequests.get(packet.getPacketID());
				pendingSpacesRequests.remove(packet.getPacketID());
				Element element = parsePacketToElement(packet);
				if (element != null) {
					spaceFuture.setResponse(element);
				}
			}
		}
	};
	
	/**
	 * Creates a new space handler.
	 * @param context The current application context.
	 * @param connectionHandler The current XMPP-connection.
	 * @param dbName A name for the database the caches are saved in. If one was used and you want to use another one you should delete all application data.
	 */
	public SpaceHandler(Context context, ConnectionHandler connectionHandler, String dbName){
		if (context == null || connectionHandler == null || dbName == null || dbName.trim().length() == 0){
			throw new IllegalArgumentException("None of the Arguments may be null.");
		}
		this.timeout = connectionHandler.getConfiguration().requestTimeout();
		this.userWantedMode = Mode.OFFLINE;
		this.connectionHandler = connectionHandler;
		this.connection = this.connectionHandler.getXMPPConnection();
		datawrapper = DataWrapper.getInstance(context, dbName);
		
		pendingSpacesRequests = new ConcurrentHashMap<String, RequestFuture<Element>>();
		this.userInfo = this.connectionHandler.getCurrentUser();
		this.spaces = new ArrayList<Space>();
		connectionHandler.addConnectionStatusListener(new ConnectionStatusListener() {
			
			@Override
			public void connectionStatusChanged(ConnectionStatus newStatus) {
				if (newStatus == ConnectionStatus.ONLINE){
					setRealMode(Mode.ONLINE);	
					prepareOnlineMode();
				} else {
					setRealMode(Mode.OFFLINE);
				}
			}
		});
		if (connectionHandler.getStatus() == ConnectionStatus.ONLINE){
			setRealMode(Mode.ONLINE);
		} else {
			setRealMode(Mode.OFFLINE);
		}
	}

	/**
	 * Creates a private space for the current user.
	 * @return Return The private space created for the current user.
	 * @throws SpaceManagementException Failed to create default space. 
	 * @throws ConnectionStatusException The handler needs to be ONLINE to perform this operation.
	 */
	@Override
	public PrivateSpace createDefaultSpace()
		throws SpaceManagementException, ConnectionStatusException {
		if (getMode() == Mode.OFFLINE){
			throw new ConnectionStatusException("The handler has to be online to use this method.");
		}
		IQ iq = new IQ(){

			@Override
			public String getChildElementXML() {
				Element childElement = new Element("spaces", NamespaceConfig.SPACES_SERVICE);
				childElement.addContent(new Element("create", NamespaceConfig.SPACES_SERVICE));
				XMLOutputter out = new XMLOutputter();
				return out.outputString(childElement);
			}
		};
		iq.setType(IQ.Type.SET);
		iq.setFrom(userInfo.getFullJID());
		iq.setTo(connectionHandler.getNetworkInformation().getSpacesServiceJID());
		RequestFuture<Element> spaceFuture = new RequestFuture<Element>();
		pendingSpacesRequests.put(iq.getPacketID(), spaceFuture);
		connection.sendPacket(iq);
		Element response;
		try {
			response = spaceFuture.get(timeout, TimeUnit.MILLISECONDS);
			if (response != null){
				String type = response.getAttributeValue("type");
				if ("result".equalsIgnoreCase(type)){
					List<Element> elements = getChildren(response, "create");
					if (elements.size() != 1) {
						return null;
					}
					String spaceId = elements.get(0).getAttributeValue("space");
					PrivateSpace defaultSpace = new de.imc.mirror.sdk.android.PrivateSpace("", spaceId, connectionHandler.getConfiguration().getDomain());
					defaultSpace = (PrivateSpace) retrieveAllSpaceInformation(defaultSpace);
					
					return defaultSpace;
				}else if ("error".equalsIgnoreCase(type)){
					//TODO
					List<Element> error = getChildren(response, "text");
					if (error.size() > 0) {
						Log.d("SpaceHandler", error.get(0).getText());
					}
					throw new SpaceManagementException("An error response was received.",  SpaceManagementException.Type.OTHER);
				}
			}
		} catch (InterruptedException e) {
			throw new SpaceManagementException("Receiving a response was interrupted.", SpaceManagementException.Type.OTHER, e);
		} catch (ExecutionException e) {
			throw new SpaceManagementException("Couldn't receive a response.", SpaceManagementException.Type.OTHER, e);
		} catch (TimeoutException e) {
			throw new SpaceManagementException("Receiving a response timed out.", SpaceManagementException.Type.OTHER, e);
		}
		return null;
	}
	
	/**
	 * Creates a space with the given configuration.
	 * @param config Space configuration to apply.
	 * @return Space created.
	 * @throws SpaceManagementException Failed to create the space. 
	 * @throws ConnectionStatusException The handler needs to be ONLINE to perform this operation.
	 */
	@Override
	public Space createSpace(final SpaceConfiguration config) throws 
					SpaceManagementException, ConnectionStatusException {
		if (getMode() == Mode.OFFLINE){
			throw new ConnectionStatusException("The handler has to be online to use this method.");
		}
		checkConfiguration(config);
		IQ iq = new IQ(){

			@Override
			public String getChildElementXML() {
				Element childElement = new Element("spaces", NamespaceConfig.SPACES_SERVICE);
				childElement.addContent(new Element("create", NamespaceConfig.SPACES_SERVICE));
				Element configureElement = new Element("configure", NamespaceConfig.SPACES_SERVICE);
				Element xElement = new Element("x", NamespaceConfig.XMPP_DATA).setAttribute("type", "submit");
				Element fieldElement = new Element("field", NamespaceConfig.XMPP_DATA);
				fieldElement.setAttribute("var", "FORM_TYPE").setAttribute("type", "hidden");
				fieldElement.addContent(new Element("value", NamespaceConfig.XMPP_DATA).setText(NamespaceConfig.SPACES_SERVICE_CONFIG));
				xElement.addContent(fieldElement);
				
				fieldElement = new Element("field", NamespaceConfig.XMPP_DATA);
				fieldElement.setAttribute("var", "spaces#type");
				fieldElement.addContent(new Element("value", NamespaceConfig.XMPP_DATA).setText(config.getType().toString()));
				xElement.addContent(fieldElement);
				
				fieldElement = new Element("field", NamespaceConfig.XMPP_DATA);
				fieldElement.setAttribute("var", "spaces#persistent");
				String persistentString;
				switch (config.getPersistenceType()) {
				case ON:
					persistentString = "true";
					break;
				case DURATION:
					persistentString =  config.getPersistenceDuration().toString();
					break;
				default:
					persistentString = "false";
				}
				fieldElement.addContent(new Element("value", NamespaceConfig.XMPP_DATA).setText(persistentString));
				xElement.addContent(fieldElement);
				
				fieldElement = new Element("field", NamespaceConfig.XMPP_DATA);
				fieldElement.setAttribute("var", "spaces#name");
				fieldElement.addContent(new Element("value", NamespaceConfig.XMPP_DATA).setText(config.getName()));
				xElement.addContent(fieldElement);
				
				Element membersElement = new Element("field", NamespaceConfig.XMPP_DATA).setAttribute("var", "spaces#members");
				Element moderatorsElement = new Element("field", NamespaceConfig.XMPP_DATA).setAttribute("var", "spaces#moderators");
				for (SpaceMember member: config.getMembers()){
					switch(member.getRole()){
					case MEMBER:
						membersElement.addContent(new Element("value", NamespaceConfig.XMPP_DATA).setText(member.getJID()));
						break;
					case MODERATOR:
						membersElement.addContent(new Element("value", NamespaceConfig.XMPP_DATA).setText(member.getJID()));
						moderatorsElement.addContent(new Element("value", NamespaceConfig.XMPP_DATA).setText(member.getJID()));
						
					}
				}
				xElement.addContent(membersElement);
				xElement.addContent(moderatorsElement);
				configureElement.addContent(xElement);
				childElement.addContent(configureElement);
				XMLOutputter out = new XMLOutputter();
				return out.outputString(childElement);
			}
		};
		iq.setType(IQ.Type.SET);
		iq.setTo(connectionHandler.getNetworkInformation().getSpacesServiceJID());
		iq.setFrom(userInfo.getFullJID());
		RequestFuture<Element> spaceFuture = new RequestFuture<Element>();
		pendingSpacesRequests.put(iq.getPacketID(), spaceFuture);
		connection.sendPacket(iq);
		Element response = null;
		try {
			response = spaceFuture.get(timeout, TimeUnit.MILLISECONDS);
			if (response != null){
				String type = response.getAttributeValue("type");
				if ("result".equalsIgnoreCase(type)){
					List<Element> elements = getChildren(response, "create");
					if (elements.size() != 1) {
						return null;
					}
					String spaceId = elements.get(0).getAttributeValue("space");
					Space space = de.imc.mirror.sdk.android.Space.createSpace(config.getName(), 
							spaceId, 
							connectionHandler.getConfiguration().getDomain(),
							null,
							config.getType(), 
							null, 
							config.getMembers(), 
							config.getPersistenceType(),
							config.getPersistenceDuration()
						);

					Space newSpace;
					newSpace = retrieveAllSpaceInformation(space);
					if (newSpace != null){
						for (SpaceMember member:config.getMembers()){
							if (member.getJID().equalsIgnoreCase(userInfo.getBareJID())){
								datawrapper.saveSpace(newSpace, userInfo.getBareJID());
								break;
							}
						}
						spaces.add(newSpace);
						return newSpace;
					}
					return space;
				}else if ("error".equalsIgnoreCase(type)){
					//TODO
					List<Element> error = getChildren(response, "text");
					if (error.size() > 0) {
						Log.d("SpaceHandler", error.get(0).getText());
					}
					throw new SpaceManagementException("An error response was received.",  SpaceManagementException.Type.OTHER);
				}
			}
		}catch (InterruptedException e) {
			throw new SpaceManagementException("Receiving a response was interrupted.", SpaceManagementException.Type.OTHER, e);
		} catch (ExecutionException e) {
			throw new SpaceManagementException("Couldn't receive a response.", SpaceManagementException.Type.OTHER, e);
		} catch (TimeoutException e) {
			throw new SpaceManagementException("Receiving a response timed out.", SpaceManagementException.Type.OTHER, e);
		}
		return null;
	}

	/**
	 * Deletes the space with the given id.
	 * @param spaceId Identifier of the space to delete.
	 * @throws SpaceManagementException Failed to delete the space.
	 * @throws ConnectionStatusException The handler needs to be ONLINE to perform this operation.
	 */
	@Override
	public void deleteSpace(final String spaceId) throws SpaceManagementException, ConnectionStatusException{
		if (getMode() == Mode.OFFLINE){
			throw new ConnectionStatusException("You have to be in onlinemode to delete a Space");
		}
		Space space = this.getSpace(spaceId);
		if (space == null) {
			return;
		}
		if (!isModeratorOfSpace(userInfo.getBareJID(), space)) {
			throw new SpaceManagementException(userInfo.getUsername() + " is no moderator of space " + spaceId, SpaceManagementException.Type.NOT_AUTHORIZED);
		}
		IQ testIq = new IQ(){

			@Override
			public String getChildElementXML() {
				Element childElement = new Element("spaces", NamespaceConfig.SPACES_SERVICE);
				childElement.addContent(new Element("delete", NamespaceConfig.SPACES_SERVICE).setAttribute("space", spaceId));
				XMLOutputter out = new XMLOutputter();
				return out.outputString(childElement);
			}
			
		};
		String id = IQ.nextID();
		testIq.setType(IQ.Type.SET);
		testIq.setTo(connectionHandler.getNetworkInformation().getSpacesServiceJID());
		testIq.setFrom(userInfo.getFullJID());
		testIq.setPacketID(id);
		RequestFuture<Element> spaceFuture = new RequestFuture<Element>();
		pendingSpacesRequests.put(id, spaceFuture);
		connection.sendPacket(testIq);
		try{
		Element response = spaceFuture.get(timeout, TimeUnit.MILLISECONDS);
		if (response != null){
			String type = response.getAttributeValue("type");
			if ("result".equalsIgnoreCase(type)){
				datawrapper.deleteCachedSpace(spaceId);
			}
			else if ("error".equalsIgnoreCase(type)){
				//TODO
				List<Element> error = getChildren(response, "text");
				if (error.size() > 0) {
					Log.d("SpaceHandler", error.get(0).getText());
				}
				throw new SpaceManagementException("An error response was received.",  SpaceManagementException.Type.OTHER);
			}
		}
		} catch (InterruptedException e) {
			throw new SpaceManagementException("Receiving a response was interrupted.", SpaceManagementException.Type.OTHER, e);
		} catch (ExecutionException e) {
			throw new SpaceManagementException("Couldn't receive a response.", SpaceManagementException.Type.OTHER, e);
		} catch (TimeoutException e) {
			throw new SpaceManagementException("Receiving a response timed out.", SpaceManagementException.Type.OTHER, e);
		}
	}
	
	/**
	 * Retrieves the configuration of the space.
	 * @param space The space to get the configuration for.
	 * @return The parsed result or, if there was an error, null.
	 * @throws SpaceManagementException Thrown when no response was received from the server.
	 */
	private SpaceConfiguration getSpaceConfiguration(Space space) throws SpaceManagementException{
		if (space == null){
			throw new IllegalArgumentException("The given space must not be null");
		}
		ServiceDiscoveryManager discoveryManager = ServiceDiscoveryManager.getInstanceFor(connection);
		DiscoverInfo info;
		try {
			info = discoveryManager.discoverInfo(SERVICE_PREFIX + space.getDomain(), space.getId());
		} catch (XMPPException e) {
			if (e.getXMPPError().getCode() == 404) {
				return null;
			} else {
				throw new SpaceManagementException("The Server didn't respond.", Type.OTHER, e);
			}
		}
		Element config = parsePacketToElement(info);
		return parseSpaceConfiguration(config);
	}

	/**
	 * Checks if the given user is a moderator of the given space.
	 * @param user The user to check.
	 * @param space The space to check.
	 * @return If the user is an moderator of the given space. If offline, it returns <code>false</code>
	 */
	private boolean isModeratorOfSpace(String user, Space space){
		if (getMode() == Mode.OFFLINE) {
			return false;
		}
		if (space != null){
			Set<SpaceMember> members = space.getMembers();
			if (members != null){
				for (SpaceMember member:members){
					if (member.getJID().equalsIgnoreCase(user.split("/")[0])){
						if (member.getRole() == Role.MODERATOR){
							return true;
						} else return false;
					}
				}
			}
		}
		return false;
	}
	
	/**
	 * Checks if the given user is a moderator of the given space.
	 * @param user The user to check.
	 * @param spaceId The id of the space to check.
	 * @return If the user is an moderator of the given space. If offline, it returns <code>false</code>
	 * @throws UnknownEntityException The given space does not exist.
	 */
	public boolean isModeratorOfSpace(String user, String spaceId) throws UnknownEntityException {
		//Lazy getSpace?
		Space space = getSpace(spaceId);
		if (space != null) {
			return isModeratorOfSpace(user, space);
		} else {
			throw new UnknownEntityException("No space could be retrieved for id: " + spaceId);
		}
	}
	
	/**
	 * Checks if a SpaceConfiguration is wellformed.
	 * @param config The SpaceConfiguration to check.
	 */
	private void checkConfiguration(SpaceConfiguration config) {
		if (config.getName().length() <= 0){
			throw new IllegalArgumentException("The name may not be empty.");
		}
		if (config.getType().equals(Space.Type.OTHER)){
			throw new IllegalArgumentException("You can only create spaces of the type private, team or orga.");
		}
		if (config.getMembers().size() <= 0){
			throw new IllegalArgumentException("A space needs at least one member.");
		}
		boolean hasModerator = false;
		for (SpaceMember member:config.getMembers()){
			if (member.getRole().equals(Role.MODERATOR)){
				hasModerator = true;
				break;
			}
		}
		if (!hasModerator){
			throw new IllegalArgumentException("A space needs at least one Moderator.");
		}
	}
	
	/**
	 * Tries to apply a configuration to a space.
	 * @param spaceId Identifier of the space to apply configuration on. 
	 * @param config Space configuration to apply.
	 * @return Space after the configuration was successfully applied.
	 * @throws SpaceManagementException Failed to apply space configuration.
	 * @throws ConnectionStatusException The handler needs to be ONLINE to perform this operation.
	 */
	@Override
	public Space configureSpace(final String spaceId, final SpaceConfiguration config) throws 
		SpaceManagementException, ConnectionStatusException {
		if (getMode() == Mode.OFFLINE){
			throw new ConnectionStatusException("Invalid online status. The handler must be online in order to perform this task.");
		}
		Space space = getSpace(spaceId);
		if (space == null) {
			throw new SpaceManagementException("The given space does not exist: " + spaceId, SpaceManagementException.Type.OTHER);
		}
		if (!space.isModerator(userInfo.getBareJID())) {
			throw new SpaceManagementException("The user is no moderator of this space", SpaceManagementException.Type.NOT_AUTHORIZED);
		}
		checkConfiguration(config);
		IQ modifyIq = new IQ(){
			@Override
			public String getChildElementXML() {
				Element childElement = new Element("spaces", NamespaceConfig.SPACES_SERVICE);
				Element configureElement = new Element("configure", NamespaceConfig.SPACES_SERVICE);
				configureElement.setAttribute("space", spaceId);
				childElement.addContent(configureElement);
				Element xElement = new Element("x", NamespaceConfig.XMPP_DATA);
				xElement.setAttribute("type", "submit");
				configureElement.addContent(xElement);
				Element fieldElement = new Element("field", NamespaceConfig.XMPP_DATA);
				fieldElement.setAttribute("var", "FORM_TYPE").setAttribute("type", "hidden");
				fieldElement.addContent(new Element("value", NamespaceConfig.XMPP_DATA).setText(NamespaceConfig.SPACES_SERVICE_CONFIG));
				xElement.addContent(fieldElement);
				
				fieldElement = new Element("field", NamespaceConfig.XMPP_DATA);
				fieldElement.setAttribute("var", "spaces#type");
				fieldElement.addContent(new Element("value", NamespaceConfig.XMPP_DATA).setText(config.getType().toString()));
				xElement.addContent(fieldElement);
				
				fieldElement = new Element("field", NamespaceConfig.XMPP_DATA);
				fieldElement.setAttribute("var", "spaces#persistent");
				String persistentString;
				switch (config.getPersistenceType()) {
				case ON:
					persistentString = "true";
					break;
				case DURATION:
					persistentString = config.getPersistenceDuration().toString();
					break;
				default:
					persistentString = "false";
				}
				fieldElement.addContent(new Element("value", NamespaceConfig.XMPP_DATA).setText(persistentString));
				xElement.addContent(fieldElement);
				
				fieldElement = new Element("field", NamespaceConfig.XMPP_DATA);
				fieldElement.setAttribute("var", "spaces#name");
				fieldElement.addContent(new Element("value", NamespaceConfig.XMPP_DATA).setText(config.getName()));
				xElement.addContent(fieldElement);
				

				Element membersElement = new Element("field", NamespaceConfig.XMPP_DATA).setAttribute("var", "spaces#members");
				Element moderatorsElement = new Element("field", NamespaceConfig.XMPP_DATA).setAttribute("var", "spaces#moderators");
				for (SpaceMember member: config.getMembers()){
					switch(member.getRole()){
					case MEMBER:
						membersElement.addContent(new Element("value", NamespaceConfig.XMPP_DATA).setText(member.getJID()));
						break;
					case MODERATOR:
						membersElement.addContent(new Element("value", NamespaceConfig.XMPP_DATA).setText(member.getJID()));
						moderatorsElement.addContent(new Element("value", NamespaceConfig.XMPP_DATA).setText(member.getJID()));
						
					}
				}
				xElement.addContent(membersElement);
				xElement.addContent(moderatorsElement);
			
				XMLOutputter out = new XMLOutputter();
				return out.outputString(childElement);
			}
			
		};
		String id = IQ.nextID();
		modifyIq.setType(IQ.Type.SET);
		modifyIq.setTo(SERVICE_PREFIX + space.getDomain());
		modifyIq.setFrom(userInfo.getFullJID());
		modifyIq.setPacketID(id);
		RequestFuture<Element> spaceFuture = new RequestFuture<Element>();
		pendingSpacesRequests.put(id, spaceFuture);
		connection.sendPacket(modifyIq);
		Element response;
		try {
			response = spaceFuture.get(timeout, TimeUnit.MILLISECONDS);
			if (response != null){
				String type = response.getAttributeValue("type");
				if ("result".equalsIgnoreCase(type)){
					return getSpace(space.getId());
				}
				else if ("error".equalsIgnoreCase(type)){
					//TODO
					List<Element> error = getChildren(response, "text");
					if (error.size() > 0) {
						Log.d("SpaceHandler", error.get(0).getText());
					}
					throw new SpaceManagementException("An error response was received.",  SpaceManagementException.Type.OTHER);
				}
			}
		} catch (InterruptedException e) {
			throw new SpaceManagementException("Receiving a response was interrupted.", SpaceManagementException.Type.OTHER, e);
		} catch (ExecutionException e) {
			throw new SpaceManagementException("Couldn't receive a response.", SpaceManagementException.Type.OTHER, e);
		} catch (TimeoutException e) {
			throw new SpaceManagementException("Receiving a response timed out.", SpaceManagementException.Type.OTHER, e);
		}
		return null;
	}
	
	/**
	 * Parses a received Element of the configuration of a space.
	 * @param configElement The iq of the configuration of a space.
	 * @return A SpaceConfiguration object with the data of the space, or null if something is wrong.
	 */
	private SpaceConfiguration parseSpaceConfiguration(Element configElement){
		List<Element> fields;
		if (configElement == null || (fields = getChildren(configElement, "field")) == null || fields.size() == 0){
			return null;
		}
		else {
			Set<SpaceMember> spaceMembers = new HashSet<SpaceMember>();
			Set<String> moderators = new HashSet<String>();
			Set<String> members = new HashSet<String>();
			Space.Type type = null;
			PersistenceType persistenceType = PersistenceType.OFF;
			Duration persistenceDuration = null;
			String name = null;
			for (Element field:fields){
				String var = field.getAttributeValue("var");
				if ("spaces#type".equals(var)){
					type = Space.Type.getType(field.getChildText("value", field.getNamespace()));
				} else if ("spaces#persistent".equals(var)) {
					String persistentString = field.getChildText("value", field.getNamespace());
					if ("true".equalsIgnoreCase(persistentString) || "1".equals(persistentString)) {
						persistenceType = PersistenceType.ON;
						persistenceDuration = null;
					} else if ("false".equalsIgnoreCase(persistentString) || "0".equals(persistentString)) {
						// keep defaults
					} else {
						try {
							persistenceDuration = DatatypeFactory.newInstance().newDuration(persistentString);
							persistenceType = PersistenceType.DURATION;
						} catch (Exception e) {
							// keep defaults
						}
					}
				} else if ("spaces#name".equals(var)){
					name = field.getChildText("value", field.getNamespace());
				} else if ("spaces#members".equals(var)){
					List<Element> values = getChildren(field, "value");
					for (Element value:values){
						members.add(value.getText());
					}
				} else if ("spaces#moderators".equals(var)){
					List<Element> values = getChildren(field, "value");
					for (Element value:values){
						moderators.add(value.getText());
					}
				}
			}
			members.removeAll(moderators);
			for (String member:members){
				spaceMembers.add(new de.imc.mirror.sdk.android.SpaceMember(member, Role.MEMBER));
			}
			for (String moderator:moderators){
				spaceMembers.add(new de.imc.mirror.sdk.android.SpaceMember(moderator, Role.MODERATOR));
			}
			SpaceConfiguration config = new de.imc.mirror.sdk.android.SpaceConfiguration(type,name, spaceMembers, persistenceType, persistenceDuration);
			return config;
		}
	}
	
	/**
	 * Parses the given space info to retrieve the id of the pubsub-node and -service
	 * and the id of the MUC if available.
	 * @param spaceInfo The parsed Element of the spaceinfo.
	 * @param space The corresponding space.
	 * @return The space with the set infos.
	 */
	private Space parseSpaceChannels(Element spaceInfo, Space space){
		List<Element> channels = getChildren(spaceInfo, "channel");
		if (channels == null || channels.size() == 0){
			return null;
		}
		Set<SpaceChannel> channelsList = new HashSet<SpaceChannel>();
		for (Element channel: channels){
			Map<String, String> properties = new HashMap<String, String>();
			String type = channel.getAttributeValue("type");
			List<Element> children = getChildren(channel, "property");
			for (Element property : children) {
				properties.put(property.getAttributeValue("key"), property.getText());
			}
			channelsList.add(new de.imc.mirror.sdk.android.SpaceChannel(type, properties));
		}
		Space result = de.imc.mirror.sdk.android.Space.createSpace(
			space.getName(), 
			space.getId(), 
			space.getDomain(), 
			null, 
			space.getType(), 
			channelsList, 
			space.getMembers(), 
			space.getPersistenceType(),
			space.getPersistenceDuration()
		);
		return result;
	}
	
	/**
	 * Setter for the XMPPConnection. Use this only when you create a new connection object.
	 * @param connectionHandler The XMPPConnection object this instance of a SpaceHandler should use.
	 * @throws SpaceManagementException Thrown when there's no response from the server.
	 */
	public void changeConnectionHandler(ConnectionHandler connectionHandler) throws SpaceManagementException {
		this.connectionHandler = connectionHandler;
		this.connection = this.connectionHandler.getXMPPConnection();

		if (connectionHandler.getNetworkInformation().getSpacesServiceJID() != null){
			AndFilter andFilter = new AndFilter();
			OrFilter orFilter = new OrFilter();
			orFilter.addFilter(new IQTypeFilter(IQ.Type.ERROR));
			orFilter.addFilter(new IQTypeFilter(IQ.Type.RESULT));
			andFilter.addFilter(orFilter);
			andFilter.addFilter(new FromContainsFilter(connectionHandler.getNetworkInformation().getSpacesServiceJID()));
			this.connection.addPacketListener(packetListener, andFilter);
		}
	}
	
	/**
	 * Requests the spaces which are available for the user.
	 * @return List<Space> A List of all available spaces, with each having their name, id and domain set.
	 * @throws SpaceManagementException Thrown when there's an error response from the server.
	 */
	private List<Space> retrieveAvailableSpaces() throws SpaceManagementException{
		if (getMode() == Mode.OFFLINE || connectionHandler.getNetworkInformation().getSpacesServiceJID() == null){
			return null;
		}
		List<Space> spacesList = new ArrayList<Space>();
		ServiceDiscoveryManager discoveryManager = ServiceDiscoveryManager.getInstanceFor(connection);
		DiscoverItems itemsInfo;
		try {
			itemsInfo = discoveryManager.discoverItems(connectionHandler.getNetworkInformation().getSpacesServiceJID());
		} catch (XMPPException e) {
			throw new SpaceManagementException("Server didn't respond.", Type.OTHER, e);
		}
		Element element = parsePacketToElement(itemsInfo);
		if ("result".equalsIgnoreCase(element.getAttributeValue("type"))){
			List<Element> items = getChildren(element, "item");
			if (items != null){
				for (Element item:items){
					String domain = item.getAttributeValue("jid").replace(SERVICE_PREFIX, "");
					String[] node = item.getAttributeValue("node").split("#");
					Space.Type type;
					if (node.length>=2){
						type = Space.Type.getType(node[0]);
						if (type == Space.Type.OTHER){
							type = Space.Type.PRIVATE;
						}
					} else {
						type = Space.Type.PRIVATE;
					}
					spacesList.add(
						de.imc.mirror.sdk.android.Space.createSpace(
								item.getAttributeValue("name"),
								item.getAttributeValue("node"),
								domain,
								null,
								type,
								null,
								null,
								PersistenceType.OFF,
								null)
					);
				}
			}
		} //TODO error possible??
		/*else if (iq.getType().equals("error")){
			Log.d("SpaceHandler", iq.error.text.text);
			throw new SpaceManagementException("An error response was received.",  SpaceManagementException.Type.OTHER);
		}*/
		return spacesList;
	}

	/**
	 * Requests the channels of the given space.
	 * @param space The space to get the channels for.
	 * @return The space with the set infos.
	 * @throws SpaceManagementException Thrown when an error occurred while retrieving the channels.
	 */
	private Space retrieveSpaceChannels(final Space space) throws SpaceManagementException{
		IQ channelsIq = new IQ() {
			
			@Override
			public String getChildElementXML() {
				Element childElement = new Element("spaces", NamespaceConfig.SPACES_SERVICE);
				Element channelsElement = new Element("channels", NamespaceConfig.SPACES_SERVICE);
				channelsElement.setAttribute("space", space.getId());
				childElement.addContent(channelsElement);
				XMLOutputter out = new XMLOutputter();
				return out.outputString(childElement);
			}
		};
		channelsIq.setType(IQ.Type.GET);
		channelsIq.setTo(connectionHandler.getNetworkInformation().getSpacesServiceJID());
		RequestFuture<Element> channelsFuture = new RequestFuture<Element>();
		pendingSpacesRequests.put(channelsIq.getPacketID(), channelsFuture);
		connection.sendPacket(channelsIq);
		Element response;
		try {
			response = channelsFuture.get(timeout, TimeUnit.MILLISECONDS);
			return parseSpaceChannels(response, space);
		} catch (InterruptedException e) {
			throw new SpaceManagementException("Receiving a response was interrupted.", SpaceManagementException.Type.OTHER, e);
		} catch (ExecutionException e) {
			throw new SpaceManagementException("Couldn't receive a response.", SpaceManagementException.Type.OTHER, e);
		} catch (TimeoutException e) {
			throw new SpaceManagementException("Receiving a response timed out.", SpaceManagementException.Type.OTHER, e);
		}
	}

	/**
	 * Gets all Information of a space.
	 * @param space The Space to get Information on.
	 * @return A new space with all information set or <code>null</code> if no such space exists.
	 * @throws SpaceManagementException Thrown when an error occured.
	 */
	private Space retrieveAllSpaceInformation(Space space) throws SpaceManagementException{
		SpaceConfiguration config = getSpaceConfiguration(space);
		if (config == null) {
			return null;
		}
		space = retrieveSpaceChannels(space);
		Set<de.imc.mirror.sdk.DataModel> dataModels = new HashSet<de.imc.mirror.sdk.DataModel>();
		if (space.getType() == Space.Type.ORGA){
			try {
				Set<de.imc.mirror.sdk.DataModel> models = retrieveSupportedDataModels(space);
				if (models != null) {
					dataModels.addAll(models);
				}
			} catch (ConnectionStatusException e) {
				Log.d("SpaceHandler", "A ConnectionStatusException was thrown while trying to retrieve the supported DataModels.", e);
			}
		}
		return de.imc.mirror.sdk.android.Space.createSpace(config.getName(), space.getId(), space.getDomain(), dataModels, config.getType(),
						space.getChannels(), config.getMembers(), config.getPersistenceType(), config.getPersistenceDuration());
	}

	/**
	 * Sends an request to the server to get the supported data models of the given space
	 * @param space The space to get the data models for
	 * @return Map<namespace, List<Schemalocation>>
	 * @throws SpaceManagementException Thrown when an error occured while retrieving the DataModels.
	 * @throws ConnectionStatusException You have to be in onlineMode to use this method.
	 */
	private Set<de.imc.mirror.sdk.DataModel> retrieveSupportedDataModels(final Space space)throws SpaceManagementException, ConnectionStatusException{
		if (getMode() == Mode.OFFLINE){
			throw new ConnectionStatusException("You have to be online to retrieve datamodels");
		}
		if (!Space.Type.ORGA.equals(space.getType())){
			throw new IllegalArgumentException("The given space is no organizatorial space");
		}
		IQ modelsIq = new IQ() {
			
			@Override
			public String getChildElementXML() {
				Element childElement = new Element("spaces", NamespaceConfig.SPACES_SERVICE);
				Element modelsElement = new Element("models", NamespaceConfig.SPACES_SERVICE);
				modelsElement.setAttribute("space", space.getId());
				childElement.addContent(modelsElement);
				XMLOutputter out = new XMLOutputter();
				return out.outputString(childElement);
			}
		};
		modelsIq.setType(IQ.Type.GET);
		modelsIq.setTo(connectionHandler.getNetworkInformation().getSpacesServiceJID());
		String id = IQ.nextID();
		modelsIq.setPacketID(id);
		RequestFuture<Element> modelsFuture = new RequestFuture<Element>();
		pendingSpacesRequests.put(id, modelsFuture);
		connection.sendPacket(modelsIq);
		try{
		Element response = modelsFuture.get(timeout, TimeUnit.MILLISECONDS);
		List<Element> models = getChildren(response, "model");
		if (models.size() == 0){
			return null;
		}
		else if ("error".equalsIgnoreCase(response.getAttributeValue("type"))){
			//TODO
			List<Element> error = getChildren(response, "text");
			if (error.size() > 0) {
				Log.d("SpaceHandler", error.get(0).getText());
			}
			throw new SpaceManagementException("An error response was received.",  SpaceManagementException.Type.OTHER);
		}
		Set<de.imc.mirror.sdk.DataModel> dataModels = new HashSet<de.imc.mirror.sdk.DataModel>();
		for (Element model:models){
			dataModels.add(new DataModel(model.getAttributeValue("namespace"), model.getAttributeValue("schemaLocation")));
		}
		return dataModels;
		}catch (InterruptedException e) {
			throw new SpaceManagementException("Receiving a response was interrupted.", SpaceManagementException.Type.OTHER, e);
		} catch (ExecutionException e) {
			throw new SpaceManagementException("Couldn't receive a response.", SpaceManagementException.Type.OTHER, e);
		} catch (TimeoutException e) {
			throw new SpaceManagementException("Receiving a response timed out.", SpaceManagementException.Type.OTHER, e);
		}
	}

	/**
	 * Sets the data models supported by an organizational space.
	 * Performing this operation on spaces of types other than Space.Type.ORGA will fail.
	 * Only data objects instantiating one of the supported data models may be published on this space. 
	 * @param spaceId Identifier of the organizational space to set list of supported models for.
	 * @param dataModels Set of data models to support.
	 * @return Space after the setting has been successfully applied.
	 * @throws SpaceManagementException Failed to apply setting, e.g., because the space is not of type Space.Type.ORGA.
	 * @throws ConnectionStatusException The handler needs to be ONLINE to perform this operation.
	 */
	@Override
	public OrgaSpace setModelsSupportedBySpace(String spaceId, final Set<de.imc.mirror.sdk.DataModel> dataModels) throws SpaceManagementException, ConnectionStatusException{
		if (getMode() == Mode.OFFLINE){
			throw new ConnectionStatusException("You have to be online to set datamodels");
		}
		final OrgaSpace space = (OrgaSpace)getSpace(spaceId +"");
		if (!Space.Type.ORGA.equals(space.getType())){
			throw new IllegalArgumentException("The given space is no organizatorial space");
		}
		if (!isModeratorOfSpace(userInfo.getBareJID(), space)){
			throw new SpaceManagementException("The user " + userInfo.getUsername() + " is no moderator of the space " + space.getId(), SpaceManagementException.Type.NOT_AUTHORIZED);
		}
		IQ modelsIq = new IQ() {
			
			@Override
			public String getChildElementXML() {

				Element childElement = new Element("spaces", NamespaceConfig.SPACES_SERVICE);
				Element modelsElement = new Element("models", NamespaceConfig.SPACES_SERVICE);
				modelsElement.setAttribute("space", space.getId());
				for (de.imc.mirror.sdk.DataModel model:dataModels){
					Element modelElement = new Element("model", NamespaceConfig.SPACES_SERVICE);
					modelElement.setAttribute("namespace", model.getNamespace());
					modelElement.setAttribute("schemaLocation", model.getSchemaLocation());
					modelsElement.addContent(modelElement);
				}
				childElement.addContent(modelsElement);
				XMLOutputter out = new XMLOutputter();
				return out.outputString(childElement);
			}
		};
		modelsIq.setType(IQ.Type.SET);
		modelsIq.setTo(connectionHandler.getNetworkInformation().getSpacesServiceJID());
		String id = IQ.nextID();
		modelsIq.setPacketID(id);
		RequestFuture<Element> modelsFuture = new RequestFuture<Element>();
		pendingSpacesRequests.put(id, modelsFuture);
		connection.sendPacket(modelsIq);
		Element response = null;
		try{
		response = modelsFuture.get(timeout, TimeUnit.MILLISECONDS);
		if (response == null){
			return null;
		}
		else if ("error".equalsIgnoreCase(response.getAttributeValue("type"))){
			//TODO
			List<Element> error = getChildren(response, "text");
			if (error.size() > 0) {
			Log.d("SpaceHandler", error.get(0).getText());
			}
			throw new SpaceManagementException("An error response was received.",  SpaceManagementException.Type.OTHER);
		}
		OrgaSpace orga = new de.imc.mirror.sdk.android.OrgaSpace(space.getName(), 
																spaceId +"", 
																space.getDomain(), 
																dataModels, 
																space.getChannels(),
																space.getMembers(),
																space.getPersistenceType(),
																space.getPersistenceDuration());
		return orga;
		}catch (InterruptedException e) {
			throw new SpaceManagementException("Receiving a response was interrupted.", SpaceManagementException.Type.OTHER, e);
		} catch (ExecutionException e) {
			throw new SpaceManagementException("Couldn't receive a response.", SpaceManagementException.Type.OTHER, e);
		} catch (TimeoutException e) {
			throw new SpaceManagementException("Receiving a response timed out.", SpaceManagementException.Type.OTHER, e);
		}
	}
	
	/**
	 * Called when the mode provided by the connection is changed to online or the user sets the mode to online.
	 * If Handler is in onlinemode some preparations are done like checking and perhaps retrieving the spaces service adress.
	 */
	private void prepareOnlineMode(){
		if (getMode() == Mode.ONLINE){
			if (domain == null){
				domain = userInfo.getDomain();
			}
			if (connectionHandler.getNetworkInformation().getSpacesServiceJID() != null) {
				this.connection.removePacketListener(packetListener);
				AndFilter andFilter = new AndFilter();
				OrFilter orFilter = new OrFilter();
				orFilter.addFilter(new IQTypeFilter(IQ.Type.ERROR));
				orFilter.addFilter(new IQTypeFilter(IQ.Type.RESULT));
				andFilter.addFilter(orFilter);
				andFilter.addFilter(new FromContainsFilter(connectionHandler.getNetworkInformation().getSpacesServiceJID()));
				this.connection.addPacketListener(packetListener, andFilter);
			}
		}
	}

	/**
	 * Sets the mode for the handler.
	 * @param mode Mode to set. 
	 */
	@Override
	public void setMode(Mode mode){
		this.userWantedMode = mode;
		if (userWantedMode == Mode.ONLINE){
			prepareOnlineMode();
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
	 * Sets the current mode provided by the Connection;
	 * @param mode Mode to set.
	 */
	private void setRealMode(Mode mode){
		this.realMode = mode;
	}

	/**
	 * Returns a list of all spaces available to the user.
	 * ONLINE mode: The information is retrieved from the server, the local cache is updated, and the information is returned.
	 * OFFLINE mode: The the information available in the local cache is returned.
	 * @return Unmodifiable list containing all spaces available to the user. May be empty.
	 */
	@Override
	public List<Space> getAllSpaces() {	
		List<Space> result = new ArrayList<Space>();
		if (getMode() == Mode.ONLINE){
			try {
			List<Space> spacesList = retrieveAvailableSpaces();	
			if (spacesList == null){
				return null;
			}
			for (Space space:spacesList){
				space = retrieveAllSpaceInformation(space);
				result.add(space);
			}
			datawrapper.deleteCachedSpacesForUser(userInfo.getBareJID());
			datawrapper.saveSpaces(result, userInfo.getBareJID());
			} catch (SpaceManagementException e) {
				Log.d("SpaceHandler", "A SpaceManagementException was thrown while retrieving all spaces.", e);
			} 
		} else {
			spaces = datawrapper.getCachedSpacesForUser(userInfo.getBareJID());
			return Collections.unmodifiableList(spaces);
		}
		return Collections.unmodifiableList(result);
	}

	/**
	 * Returns the private space of the user. 
	 * ONLINE mode: The information is retrieved from the server, the local cache is updated, and the information is returned.
	 * OFFLINE mode: The the information available in the local cache is returned.
	 * @return Private space of the user or <code>null</code> if no private space is set up.
	 */
	@Override
	public PrivateSpace getDefaultSpace() {
		return (PrivateSpace) getSpace(userInfo.getUsername());
	}

	/**
	 * Returns a specific space.
	 * ONLINE mode: The information is retrieved from the server, the local cache is updated, and the information is returned.
	 * OFFLINE mode: The the information from the local cache is returned if available.
	 * @param spaceId Identifier of the space to retrieve.
	 * @return Space with the given id or <code>null</code> if no space with such an id is available for the user.
	 */
	@Override
	public Space getSpace(String spaceId) {
		if (getMode() == Mode.ONLINE) {
			Space space;
			Space.Type type;
			if (spaceId.contains("team#")) {
				type = Space.Type.TEAM;
			} else if (spaceId.contains("orga#")) {
				type = Space.Type.ORGA;
			} else {
				type = Space.Type.PRIVATE;
			}
			space = de.imc.mirror.sdk.android.Space.createSpace(null, 
																spaceId, 
																userInfo.getDomain(), 
																null, 
																type, 
																null, 
																null, 
																PersistenceType.OFF, 
																null);
			Space result;
			try {
				result = retrieveAllSpaceInformation(space);
				if (result == null) {
					return null;
				}
				if (datawrapper.isSpaceAlreadyCached(spaceId)){
					datawrapper.updateCachedSpaceInformation(result);
				} else datawrapper.saveSpace(result, userInfo.getBareJID());
			} catch (SpaceManagementException e) {
				Log.d("SpaceHandler", "A SpaceManagementException was thrown while retrieving a single space.", e);
				result = null;
			}
			return result;
		} else {
			spaces = datawrapper.getCachedSpacesForUser(userInfo.getBareJID());
			for (Space space:spaces){
				if (space.getId().equalsIgnoreCase(spaceId)){
					return space;
				}
			}
		}
		return null;
	}
	
	/**
	 * Returns the list of spaces which are cached locally. 
	 * @return List of cached spaces, may be empty.
	 */
	protected List<Space> getCachedSpaces() {
		return datawrapper.getCachedSpacesForUser(userInfo.getBareJID());
	}

	/**
	 * Returns a map containing the space identifiers and names for all spaces available to the user.
	 * ONLINE mode: The information is retrieved from the server and the information is returned.
	 * OFFLINE mode: The the information available in the local cache is returned.
	 * Requesting the list without spaces details is faster and should be preferred to <code>SpacesManager.getAllSpaces()</code> whenever possible.
	 * @return Unmodifiable map of space identifiers and related space names.
	 */
	@Override
	public Map<String, String> getSpacesList() {
		Map<String, String> spacesMap = new HashMap<String, String>();
		if (getMode() == Mode.ONLINE){
			try {
				List<Space> spacesList = retrieveAvailableSpaces();
				for (Space space:spacesList){
					spacesMap.put(space.getId(), space.getName());
				}
				return Collections.unmodifiableMap(spacesMap);
			} catch (SpaceManagementException e) {
				Log.d("SpaceHandler", "A SpaceManagementException was thrown while retrieving a list of all spaces.", e);
			}
		}
		spaces = datawrapper.getCachedSpacesForUser(userInfo.getBareJID());
		for (Space space:spaces){
			spacesMap.put(space.getId(), space.getName());
		}
		return Collections.unmodifiableMap(spacesMap);
	}

	/**
	 * Deletes locally stored data.
	 */
	@Override
	public void clear() {
		datawrapper.clearSpacesCache();	
	}
	
	/**
	 * Convenience method to parse an Packet to an JDOMElement.
	 * @param packet The packet to parse.
	 * @return The parsed JDOMElement.
	 */
	private Element parsePacketToElement(Packet packet){
		SAXBuilder reader = new SAXBuilder();
		StringReader in = new StringReader(packet.toXML());
		Document document = null;
		try {
		document = reader.build(in);
		} catch (JDOMException e) {
			Log.d("SpaceHandler", "An JDOMException was thrown while parsing a newly gotten item.", e);
		} catch (IOException e) {
			Log.d("SpaceHandler", "An IOException was thrown while parsing a newly gotten item.", e);
		}
		if (document == null){
			return null;
		}
		Element elem = document.getRootElement();
		return elem;
	}
	
	private List<Element> getChildren(Element parentElement, String tagName) {
		IteratorIterable<Content> iter = parentElement.getDescendants();
		List<Element> children = new ArrayList<Element>();
		while (iter.hasNext()) {
			Element elem;
			try {
				elem = (Element) iter.next();
			} catch (ClassCastException e) {
				continue;
			}
			if (tagName.equalsIgnoreCase(elem.getName())) {
				children.add(elem);
			}
		}
		return children;
	}
	
	/**
	 * This method returns the pubsub channel of a space.
	 * It first tries to get them from the local cache, if that fails it tries to get them from the server.
	 * @param spaceId The Id of the space to get the Node and Domain for.
	 * @return The pubsub channel of the space.
	 * @throws UnknownEntityException A space with the given ID is not available for the current user.
	 */
	protected SpaceChannel getPubSubChannel(String spaceId) throws UnknownEntityException {
		// Try to retrieve data from cache. 
		List<Space> spacesList = datawrapper.getCachedSpacesForUser(userInfo.getBareJID());
		for (Space space:spacesList) {
			if (space.getId().equalsIgnoreCase(spaceId)){
				return space.getPubSubChannel();
			}
		}
		
		// Request data.
		if (getMode() == Mode.ONLINE) {
			Space space = getSpace(spaceId);
			if (space != null && space.getId().equalsIgnoreCase(spaceId)) {
				return space.getPubSubChannel();
			} else {
				throw new UnknownEntityException("No space with ID " + spaceId + " available.");
			}
		} else {
			throw new UnknownEntityException("OFFLINE mode: No space with ID " + spaceId + " cached.");
		}
	}
}
