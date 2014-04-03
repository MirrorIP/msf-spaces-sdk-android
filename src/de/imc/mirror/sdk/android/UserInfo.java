package de.imc.mirror.sdk.android;

/**
 * Container for XMPP user information. 
 * @author nmach, simon.schwantzer(at)im-c.de
 *
 */
public class UserInfo implements de.imc.mirror.sdk.UserInfo {
	
	private final String username;
	private final String domain;
	private final String resource;
	
	protected UserInfo(String username, String domain, String resource){
		this.username = username;
		this.domain = domain;
		this.resource = resource;
	}

	/**
	 * Returns the XMPP username (aka node-id) of the user.
	 * The XMPP username is unique in an XMPP domain. 
	 * @return Username of the user.
	 */
	@Override
	public String getUsername() {
		return username;
	}

	/**
	 * Returns the XMPP domain the user is registered.
	 * @return XMPP domain string.
	 */
	@Override
	public String getDomain() {
		return domain;
	}

	/**
	 * Returns the XMPP resource identifier of the client the user is connected with.
	 * @return XMPP resource string.
	 */
	@Override
	public String getResource() {
		return resource;
	}

	/**
	 * Returns the bare-JID of the user.
	 * The bare-JID contains the node-id and domain string, e.g. "alice@mirror-demo.eu".
	 * @return Bare-JID as string.
	 */
	@Override
	public String getBareJID() {
		StringBuilder builder = new StringBuilder();
		builder.append(username)
			   .append("@")
			   .append(domain);
		return builder.toString();
	}

	/**
	 * Returns the full JID of the user.
	 * Additionally to the bare-JID, the fill-JID also contains the resource identifier,
	 * e.g. "alice@mirror-demo.eu/myApp01".
	 * @return Full-JID as string.
	 */
	@Override
	public String getFullJID() {
		StringBuilder builder = new StringBuilder();
		builder.append(username)
			   .append("@")
			   .append(domain)
			   .append("/")
			   .append(resource);
		return builder.toString();
	}

}
