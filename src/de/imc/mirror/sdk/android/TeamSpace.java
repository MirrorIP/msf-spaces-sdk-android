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
 * Model class for team spaces.
 * Team spaces allow the data exchange between multiple users.
 * @author nmach, simon.schwantzer(at)im-c.de
 */
public class TeamSpace extends Space implements Serializable, de.imc.mirror.sdk.TeamSpace {
	

	private static final long serialVersionUID = 2L;

	/**
	 * Create a new TeamSpaceObject with the given properties.
	 * @param name The name of the space.
	 * @param spaceId The Id of the space.
	 * @param domain The domain of the space.
	 * @param channels The channels of the space.
	 * @param members The members of the space.
	 * @param persistenceType The persistence setting for the space.
	 * @param persistenceDuration The persistence duration if the persistence type is {@link PersistenceType#DURATION}, otherwise <code>null</code>.
	 */
	protected TeamSpace(String name, String spaceId, String domain, 
			Set<SpaceChannel> channels, Set<SpaceMember> members, PersistenceType persistenceType, Duration persistenceDuration) {
		super(name,spaceId, domain, Space.Type.TEAM, channels, members, persistenceType, persistenceDuration);
	}

	/**
	 * Returns the multi-user chat channel of the space.
	 * @return Channel containing the multi-user chat room address.
	 */
	@Override
	public SpaceChannel getMUCChannel() {
		for (SpaceChannel channel:channels){
			if (channel.getType().equals("muc")){
				return channel;
			}
		}
		return null;
	}
	
	private synchronized void writeObject(ObjectOutputStream s) throws IOException{
		Element element = new Element("TeamSpace");
		element.setAttribute("channelAmount", Integer.toString(channels.size()));
		setAttributesToSerialize(element);
		element.setAttribute("membersAmount", Integer.toString(members.size()));
		s.writeObject(element);
		for (SpaceMember member: members){
			s.writeObject(member);
		}
		for(SpaceChannel channel:channels){
			s.writeObject(channel);
		}
	}
	
	private synchronized void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException{
		Element element = (Element)s.readObject();
		getSerializedAttributes(element);
		int channelAmount = Integer.parseInt(element.getAttributeValue("channelAmount"));
		int membersAmount = Integer.parseInt(element.getAttributeValue("membersAmount"));
		for (int i=0; i<membersAmount; i++){
			members.add((SpaceMember) s.readObject());
		}
		for(int i=0; i<channelAmount; i++){
			channels.add((SpaceChannel)s.readObject());
		}
	}

}
