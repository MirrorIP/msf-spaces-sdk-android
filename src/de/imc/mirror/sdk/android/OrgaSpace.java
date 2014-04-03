package de.imc.mirror.sdk.android;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.xml.datatype.Duration;

import org.jdom2.Element;

import de.imc.mirror.sdk.DataModel;
import de.imc.mirror.sdk.SpaceChannel;
import de.imc.mirror.sdk.SpaceMember;

/**
 * Model class for organizational spaces.
 * Data transfer over organizational spaces is restricted to specified data model. 
 * @author nmach, simon.schwantzer(at)im-c.de
 */
public class OrgaSpace extends Space implements Serializable, de.imc.mirror.sdk.OrgaSpace {

	private static final long serialVersionUID = 1L;
	private Set<DataModel> dataModels;
	
	/**
	 * Create a new OrgaSpaceObject with the given properties.
	 * @param name The name of the space.
	 * @param spaceId The Id of the space.
	 * @param domain The domain of the space.
	 * @param dataModels The supported datamodels of this space. Cannot be changed later.
	 * @param channels The channels of the space.
	 * @param members The members of the space.
	 * @param persistenceType The persistence setting for the space.
	 * @param persistenceDuration The persistence duration if the persistence type is {@link de.imc.mirror.sdk.PersistenceType#DURATION}, otherwise <code>null</code>.
	 */
	protected OrgaSpace(String name, String spaceId, String domain, Set<DataModel> dataModels,
			Set<SpaceChannel> channels, Set<SpaceMember> members, PersistenceType persistenceType, Duration persistenceDuration){
		super(name,spaceId, domain, Space.Type.ORGA, channels, members, persistenceType, persistenceDuration);
		this.dataModels = new HashSet<DataModel>();
		if (dataModels != null) {
			this.dataModels.addAll(dataModels);
		}
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

	/**
	 * Returns the list of data models supported by this space.
	 * @return Unmodifiable set of data model objects.
	 */
	@Override
	public Set<DataModel> getSupportedDataModels() {
		return Collections.unmodifiableSet(dataModels);
	}
	
	private synchronized void writeObject(ObjectOutputStream s) throws IOException{
		Element element = new Element("OrgaSpace");
		element.setAttribute("channelAmount", Integer.toString(channels.size()));
		setAttributesToSerialize(element);
		element.setAttribute("membersAmount", Integer.toString(members.size()));
		element.setAttribute("modelsAmount", Integer.toString(dataModels.size()));
		s.writeObject(element);
		for (SpaceMember member: members){
			s.writeObject(member);
		}
		for (DataModel model:dataModels){
			s.writeObject(model);
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
		int modelsAmount = Integer.parseInt(element.getAttributeValue("modelsAmount"));
		for (int i=0; i<membersAmount; i++){
			members.add((SpaceMember) s.readObject());
		}
		dataModels = new HashSet<DataModel>();
		for (int i=0; i<modelsAmount; i++){
			dataModels.add((DataModel) s.readObject());
		}
		for(int i=0; i<channelAmount; i++){
			channels.add((SpaceChannel)s.readObject());
		}
	}


}
