package de.imc.mirror.sdk.android;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;

import org.jdom2.Element;

import de.imc.mirror.sdk.SpaceChannel;
import de.imc.mirror.sdk.SpaceMember;
import de.imc.mirror.sdk.SpaceMember.Role;

/**
 * Superclass for all types of MIRROR reflection spaces.
 * @author nmach, simon.schwantzer(at)im-c.de
 */
public abstract class Space implements Serializable, de.imc.mirror.sdk.Space{

	private static final long serialVersionUID = 1L;
	/**the type of this space*/
	private Type type;
	/**the name of the space*/
	private String name;
	/**the domain of the space*/
	private String domain;
	private String spaceId;
	protected Set<SpaceMember> members;
	protected Set<SpaceChannel> channels;
	private PersistenceType persistenceType;
	private Duration persistenceDuration;
	
	/**
	 * Create a new Space.
	 * @param name The name of the space.
	 * @param spaceId The id of the space.
	 * @param domain The domain of the space.
	 * @param type The type of the space.
	 * @param channels A list of all SpaceChannels of the space.
	 * @param members A list of all Members of this space.
	 * @param persistenceType The persistence setting for the space.
	 * @param persistenceDuration The persistence duration if the persistence type is {@link de.imc.mirror.sdk.PersistenceType#DURATION}, otherwise <code>null</code>.
	 */
	protected Space(String name, String spaceId, String domain, Space.Type type, 
				Set<SpaceChannel> channels, Set<SpaceMember> members, PersistenceType persistenceType, Duration persistenceDuration) {
		if (channels == null){
			channels = new HashSet<SpaceChannel>();
		}
		if (members == null){
			members = new HashSet<SpaceMember>();
		}
		this.members = members;
		this.name = name;
		this.spaceId = spaceId;
		this.channels = channels;
		this.domain = domain;
		this.type = type;
		this.persistenceType = persistenceType;
		this.persistenceDuration = persistenceDuration;
	}
	
	protected static Space createSpace(String name, String spaceId, String domain, Set<de.imc.mirror.sdk.DataModel> dataModels, Space.Type type, 
				Set<SpaceChannel> channels, Set<SpaceMember> members, PersistenceType persistenceType, Duration persistenceDuration){
		if (channels == null){
			channels = new HashSet<SpaceChannel>();
		}
		if (members == null){
			members = new HashSet<SpaceMember>();
		}
		switch (type){
		case PRIVATE:
			return new PrivateSpace(name, spaceId, domain, channels, members, persistenceType, persistenceDuration);
		case TEAM:
			return new TeamSpace(name, spaceId, domain, channels, members, persistenceType, persistenceDuration);
		case ORGA:
			return new OrgaSpace(name, spaceId, domain, dataModels, channels, members, persistenceType, persistenceDuration);
		default:
			return null;
		}
	}

	/**
	 * Returns the id of this space. The space id is unique with a domain.
	 * @return Space identifier.
	 */
	@Override
	public String getId(){
		return spaceId;
	}

	/**
	 * Returns the XMPP domain the space is located on.
	 * E.g., if the space is handled by spaces.mirror-demo.eu, the domain is "mirror-demo.eu". 
	 * @return Domain the spaces service is located, which provides the space.
	 */
	@Override
	public String getDomain() {
		return domain;
	}
	
	/**
	 * Returns the name of the space.
	 * The name is a human-readable string meant to be displayed to the user. 
	 * @return Name of the space or <code>null</code> if no name is set.
	 */
	@Override
	public String getName() {
		return name;
	}

	/**
	 * Returns the type of this space.
	 * @return Space type.
	 */
	@Override
	public Type getType() {
		return type;
	}
	
	/**
	 * Returns the persistence setting of this space.
	 * @return {@link de.imc.mirror.sdk.PersistenceType#OFF}, {@link de.imc.mirror.sdk.PersistenceType#ON}, or {@link de.imc.mirror.sdk.PersistenceType#DURATION}.
	 */
	@Override
	public PersistenceType getPersistenceType() {
		return persistenceType;
	}
	
	/**
	 * Returns the duration for which data objects are persisted after their publishing date.
	 * Only set if the space's persistence setting is {@link de.imc.mirror.sdk.PersistenceType#DURATION}.
	 * @return XSD duration object or <code>null</code> if the persistence setting is not {@link de.imc.mirror.sdk.PersistenceType#DURATION}.  
	 */
	public Duration getPersistenceDuration() {
		return persistenceDuration;
	}

	/**
	 * Returns the list of members of the space.
	 * @return Unmodifiable set of space member models containing user and role information.
	 */
	@Override
	public Set<SpaceMember> getMembers() {
		return Collections.unmodifiableSet(members);
	}
	
	/**
	 * Checks if the given user is member of this space.
	 * @param userId Bare-JID of the user to check the membership for.
	 * @return <code>true</code> if the user is member of the space, otherwise <code>false</code>.
	 */
	@Override
	public boolean isMember(String userId) {
		for (SpaceMember member:members){
			if (member.getJID().equals(userId))
				return true;
		}
		return false;
	}
	
	/**
	 * Checks if the given user is moderator of the space.
	 * @param userId Bare-JID of the user to check.
	 * @return <code>true</code> if the user is moderator of the space, otherwise <code>false</code>.
	 */
	@Override
	public boolean isModerator(String userId) {
		for (SpaceMember member:members){
			if (member.getJID().equals(userId)){
				if (member.getRole() == Role.MODERATOR){
					return true;
				} else return false;
			}
		}
		return false;
	}
	
	/**
	 * Returns the list of channels available for this space.
	 * @return Unmodifiable set of space channel objects.
	 */
	@Override
	public Set<SpaceChannel> getChannels() {
		return Collections.unmodifiableSet(channels);
	}
	
	/**
	 * Returns the publish-subscribe channel of the space.
	 * @return Publish-Subscribe channel of the space.
	 */
	@Override
	public SpaceChannel getPubSubChannel() {
		for (SpaceChannel channel:channels){
			if (channel.getType().equals("pubsub")){
				return channel;
			}
		}
		return null;
	}
	
	/**
	 * Returns the persistence channel of the space.
	 * @return Persistence channel if the space is configured to persist data, otherwise <code>null</code>.
	 */
	public SpaceChannel getPersistenceChannel() {
		for (SpaceChannel channel : channels) {
			if (channel.getType().equals("persistence")) {
				return channel;
			}
		}
		return null;
	}
	
	/**
	 * Generates a space configuration based on the configuration of this space.
	 * Changes applied to the generated configuration are not applied this space. To the change the configuration of a
	 * existing space, perform the following steps:
	 * 1. Generate a space configuration object using this method. 
	 * 2. Modify the space configuration object.
	 * 3. Use <code>SpaceHandler.configureSpace()</code> to apply the configuration.
	 * @return Space configuration with the settings of this space.
	 */
	@Override
	public SpaceConfiguration generateSpaceConfiguration() {
		SpaceConfiguration config = new SpaceConfiguration();
		config.setType(this.type);
		config.setName(this.name);
		config.setPersistenceType(persistenceType);
		config.setPersistenceDuration(persistenceDuration);
		for (SpaceMember localMember : this.members) {
			SpaceMember newMember = new de.imc.mirror.sdk.android.SpaceMember(localMember.getJID(), localMember.getRole());
			config.addMember(newMember);
		}
		return config;
	}
	
	@Override
	public int hashCode(){
		return type.name().length() + spaceId.length();
	}
	
	@Override
	public boolean equals(Object obj){
		if (obj instanceof Space){
			Space space = (Space) obj;
			if (this.type == space.getType() && 
					this.spaceId.equalsIgnoreCase(space.getId()))
				return true;
		}
		return false;
	}
	
	/**
	 * Convenience method for Serialization of a space.
	 * Don't use.
	 */
	protected void setAttributesToSerialize(Element element){
		element.setAttribute("spaceId", spaceId);
		element.setAttribute("name", name);
		element.setAttribute("domain", domain);
		element.setAttribute("type", type.name());
		switch (persistenceType) {
		case OFF:
			element.setAttribute("persistent", "false");
			break;
		case ON:
			element.setAttribute("persistent", "true");
			break;
		case DURATION:
			element.setAttribute("persistent", persistenceDuration.toString());
			break;
		}
		
	}
	
	/**
	 * Convenience method for Deserialization of a space.
	 * Don't use.
	 */
	protected void getSerializedAttributes(Element element){
		this.spaceId = element.getAttributeValue("spaceId");
		this.name = element.getAttributeValue("name");
		this.domain = element.getAttributeValue("domain");
		this.type = Type.getType(element.getAttributeValue("type"));
		String persistenceString = element.getAttributeValue("persistent");
		if ("true".equals(persistenceString)) {
			this.persistenceDuration = null;
			this.persistenceType = PersistenceType.ON;
		} else if ("false".equals(persistenceString)) {
			this.persistenceDuration = null;
			this.persistenceType = PersistenceType.OFF;
		} else {
			try {
				this.persistenceDuration = DatatypeFactory.newInstance().newDuration(persistenceString);
				this.persistenceType = PersistenceType.DURATION;
			} catch (Exception e) {
				// failed to parse duration string
				this.persistenceDuration = null;
				this.persistenceType = PersistenceType.OFF;
			}
		}
	}
}
