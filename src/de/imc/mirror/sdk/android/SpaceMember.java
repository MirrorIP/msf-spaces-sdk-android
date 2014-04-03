package de.imc.mirror.sdk.android;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.jdom2.Element;

/**
 * Model for a space member, containing its unique identifier and its role in the space. 
 * @author nmach, simon.schwantzer(at)im-c.de
 */
public class SpaceMember implements Serializable, de.imc.mirror.sdk.SpaceMember {

	private static final long serialVersionUID = 1L;
	private String jid;
	private Role role;
	
	/**
	 * Create a new SpaceMember.
	 * @param jid The bare-JID of the member.
	 * @param role The Role of the member.
	 */
	public SpaceMember(String jid, Role role){
		if (!jid.contains("@")){
			throw new IllegalArgumentException("There has to be a domain as part of the jid.");
		}
		if (jid.contains("/")){
			this.jid = jid.split("/")[0];
		}
		else this.jid = jid;
		this.role = role;
	}
	
	/**
	 * Returns the user JID of the member.
	 * @return Bare-JID of the member.
	 */
	@Override
	public String getJID() {
		return jid;
	}

	/**
	 * Returns the role of the member.
	 * @return <code>SpaceMember.Role.MODERATOR</code> if the member is also moderator of the space, otherwise <code>SpaceMember.Role.MEMBER</code>.
	 */
	@Override
	public Role getRole() {
		return role;
	}
	
	@Override
	public int hashCode(){
		return jid.length() + role.name().length();
	}
	
	@Override
	public boolean equals(Object obj){
		if (obj instanceof SpaceMember){
			SpaceMember member = (SpaceMember) obj;
			if (member.getJID().equalsIgnoreCase(this.jid)){
				return true;
			}
		}
		return false;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(200);
		builder.append("SpaceMember(")
			.append("jid = ").append(jid)
			.append(", role = ").append(role).append(")");
		return builder.toString();
	}

	private synchronized void writeObject(ObjectOutputStream s) throws IOException{
		Element element = new Element("SpaceMember");
		element.addContent(new Element("jid").setText(jid));
		element.addContent(new Element("role").setText(role.name()));
		s.writeObject(element);
	}
	
	private synchronized void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException{
		Element element = (Element)s.readObject();
		this.jid = element.getChildText("jid");
		this.role = Role.valueOf(element.getChildText("role"));
	}
}
