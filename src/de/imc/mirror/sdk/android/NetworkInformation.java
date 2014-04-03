package de.imc.mirror.sdk.android;

/**
 * Model for information about the XMPP network and its components.
 * @author simon.schwantzer(at)im-c.de
 */
public class NetworkInformation implements de.imc.mirror.sdk.NetworkInformation {
	private String spacesServiceVersion;
	private String spacesServiceJID;
	private String persistenceServiceJID;	
	
	/**
	 * 
	 * @param spacesServiceVersion
	 * @param spacesServiceJID
	 * @param persistenceServiceJID
	 */
	public NetworkInformation(String spacesServiceJID, String spacesServiceVersion, String persistenceServiceJID) {
		this.spacesServiceVersion = spacesServiceVersion;
		this.spacesServiceJID = spacesServiceJID;
		this.persistenceServiceJID = persistenceServiceJID;
	}
	
	@Override
	public String getSpacesServiceVersion() {
		return spacesServiceVersion;
	}

	@Override
	public String getSpacesServiceJID() {
		return spacesServiceJID;
	}

	@Override
	public String getPersistenceServiceJID() {
		return persistenceServiceJID;
	}

}
