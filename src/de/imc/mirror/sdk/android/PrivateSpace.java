package de.imc.mirror.sdk.android;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Set;

import javax.xml.datatype.Duration;

import org.jdom2.Element;

import de.imc.mirror.sdk.SpaceChannel;
import de.imc.mirror.sdk.SpaceMember;
import de.imc.mirror.sdk.Space.PersistenceType;

/**
 * Model class for private spaces.
 * Private spaces can be accessed only by their owner.
 * @author nmach, simon.schwantzer(at)im-c.de
 */
public class PrivateSpace extends Space implements Serializable, de.imc.mirror.sdk.PrivateSpace {

	private static final long serialVersionUID = 1L;

	/**
	 * Create a new PrivateSpaceObject with the given properties.
	 * @param name The name of the space.
	 * @param spaceId The Id of the space.
	 * @param domain The domain of the space.
	 */
	protected PrivateSpace(String name, String spaceId, String domain){
		super(name, spaceId, domain, Space.Type.PRIVATE, null, null, PersistenceType.OFF, null);
	}
	
	/**
	 * Create a new PrivateSpaceObject with the given properties.
	 * @param name The name of the space.
	 * @param spaceId The Id of the space.
	 * @param domain The domain of the space.
	 * @param channels The channels of the space.
	 * @param members The members of the space.
	 * @param persistenceType The persistence setting for the space.
	 * @param persistenceDuration The persistence duration if the persistence type is {@link PersistenceType#DURATION}, otherwise <code>null</code>.
	 */
	protected PrivateSpace(String name, String spaceId, String domain, 
			Set<SpaceChannel> channels, Set<SpaceMember> members, PersistenceType persistenceType, Duration persistenceDuration){
		super(name,spaceId, domain, Space.Type.PRIVATE, channels, members, persistenceType, persistenceDuration);
	}

	/**
	 * Returns the owner of the private space.
	 * @return Bare-JID of the owner of this space.
	 */
	@Override
	public String getOwner() {
		return ((SpaceMember)members.toArray()[0]).getJID();
	}
	

	private synchronized void writeObject(ObjectOutputStream s) throws IOException{
		Element element = new Element("PrivateSpace");
		element.setAttribute("channelAmount", Integer.toString(channels.size()));
		setAttributesToSerialize(element);
		s.writeObject(element);
		s.writeObject(getOwner());
		for(SpaceChannel channel:channels){
			s.writeObject(channel);
		}
	}
	
	private synchronized void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException{
		Element element = (Element)s.readObject();
		int channelAmount = Integer.parseInt(element.getAttributeValue("channelAmount"));
		getSerializedAttributes(element);
		SpaceMember owner = (SpaceMember)s.readObject();
		this.members.add(owner);
		for(int i=0; i<channelAmount; i++){
			channels.add((SpaceChannel)s.readObject());
		}
	}

}
