package de.imc.mirror.sdk.android;

import java.util.HashSet;
import java.util.Set;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;

import de.imc.mirror.sdk.Space.PersistenceType;
import de.imc.mirror.sdk.Space.Type;
import de.imc.mirror.sdk.Space;
import de.imc.mirror.sdk.SpaceMember;
import de.imc.mirror.sdk.SpaceMember.Role;

/**
 * A space configuration container. 
 * @author nmach, simon.schwantzer(at)im-c.de
 */
public class SpaceConfiguration implements de.imc.mirror.sdk.SpaceConfiguration{
	
	private Space.Type type;
	private String name;
	private Set<SpaceMember> members = new HashSet<SpaceMember>();
	private PersistenceType persistenceType;
	private Duration persistenceDuration;
	
	/**
	 * Creates an empty configuration.
	 * Note: The empty configuration is not valid.  
	 */
	public SpaceConfiguration() {
		this.members = new HashSet<SpaceMember>();
	}
	
	/**
	 * Create a new configuration with the given properties.
	 * @param type The type of the Space.
	 * @param name The name of the Space.
	 * @param ownerJID The bare-JID of the first member (moderator) of the space.
	 * @param persistenceType Persistence settings for the space. See {@link PersistenceType} for details.
	 * @param persistenceDuration Duration for the persistence if the persistence setting is {@link PersistenceType#DURATION}, otherwise <code>null</code>. 
	 */
	public SpaceConfiguration(Space.Type type, String name, String ownerJID, PersistenceType persistenceType, Duration persistenceDuration) {
		this.type = type;
		this.name = name;
		this.members = new HashSet<SpaceMember>();
		SpaceMember owner = new de.imc.mirror.sdk.android.SpaceMember(ownerJID, Role.MODERATOR);
		members.add(owner);
		this.persistenceType = persistenceType;
		this.persistenceDuration = persistenceDuration;
	}
	
	/**
	 * Create a new configuration with the given properties.
	 * @param type The type of the Space.
	 * @param name The name of the Space.
	 * @param members Set of space members. Caution: This set may be modified.
	 * @param persistenceType Persistence settings for the space. See {@link PersistenceType} for details.
	 * @param persistenceDuration Duration for the persistence if the persistence setting is {@link PersistenceType#DURATION}, otherwise <code>null</code>. 
	 */
	public SpaceConfiguration(Space.Type type, String name, Set<SpaceMember> members, PersistenceType persistenceType, Duration persistenceDuration) {
		this.type = type;
		this.name = name;
		this.members = members;
		this.persistenceType = persistenceType;
		this.persistenceDuration = persistenceDuration;
	}
	
	/**
	 * Returns the space type.
	 * @return Space type.
	 */
	@Override
	public Space.Type getType() {
		return type;
	}

	/**
	 * Returns the name set for the space.
	 * @return Human-readable name for the space.
	 */
	@Override
	public String getName() {
		return name;
	}

	/**
	 * Sets the name of the space.
	 * @param name Human-readable name for the space.
	 */
	@Override
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Returns the space members.
	 * Member role are assigned in the space member models.
	 * @return Set of space members.
	 */
	@Override
	public Set<SpaceMember> getMembers() {
		return members;
	}

	/**
	 * Sets the persistence type of the space.
	 * A space can be configured to either do not persist published data objects ({@link PersistenceType#OFF}),
	 * persist data objects ({@link PersistenceType#ON}), or persist data object for a specific duration 
	 * ({@link PersistenceType#DURATION}).
	 * @param persistenceType Persistence type to set.
	 */
	public void setPersistenceType(PersistenceType persistenceType) {
		this.persistenceType = persistenceType;
	}
	
	/**
	 * Returns the persistence configuration of the space.
	 * @return Either {@link PersistenceType#OFF}, {@link PersistenceType#ON}, or {@link PersistenceType#DURATION}.
	 */
	public PersistenceType getPersistenceType() {
		return persistenceType;
	}

	/**
	 * Sets the duration for the data object persistence.
	 * Only required when the persistence type is {@link PersistenceType#DURATION}.
	 * @param duration Duration to set. See {@link DatatypeFactory} for details about instantiating a duration. 
	 */
	public void setPersistenceDuration(Duration duration) {
		this.persistenceDuration = duration;
	}
	
	/**
	 * Returns the duration for the data object persistence.
	 * Only set when the persistence type is {@link PersistenceType#DURATION}.
	 * @return Duration or <code>null</code>, if the space's persistence type is not {@link PersistenceType#DURATION}.
	 */
	public Duration getPersistenceDuration() {
		return persistenceDuration;
	}

	/**
	 * Sets the type for the space.
	 * @param type Space type.
	 */
	@Override
	public void setType(Type type){
		this.type = type;
	}

	/**
	 * Sets the members for the space.
	 * Member role are assigned in the space member models.
	 * @param members Set of space members to set.
	 */
	@Override
	public void setMembers(Set<SpaceMember> members){
		this.members = members;
	}

	/**
	 * Revokes membership of a user.
	 * If the user is not stored in the list of members, nothing will happen. 
	 * @param userId Bare-JID of the user to revoke membership. 
	 */
	@Override
	public boolean removeMember(String userId) {
		for (SpaceMember entry : members){
			if (entry.getJID().equalsIgnoreCase(userId)){
				return members.remove(entry);
			}
		}
		return false;
	}

	/**
	 * Adds a member to the space.
	 * If a given user is already member of the space, only the role will be updated.
	 * @param member Space member model to set.
	 */
	@Override
	public void addMember(SpaceMember member) {
		members.add(member);
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(200);
		builder.append("SpaceConfiguration(")
			.append("name = ").append(name)
			.append(", type = ").append(type)
			.append(", persistenceType = ").append(persistenceType)
			.append(", persistenceDuration = ").append(persistenceDuration);
		builder.append(", members = [");
		String delimiter = "";
		for (SpaceMember member : members) {
			builder.append(delimiter).append(member.toString());
			delimiter = ", ";
		}
		builder.append("])");
		return builder.toString();
	}
}
