package de.imc.mirror.sdk.android;

/**
 * Configuration for the XMPP connection.
 * @author nmach, simon.schwantzer(at)im-c.de
 */
public class ConnectionConfiguration implements
		de.imc.mirror.sdk.ConnectionConfiguration {
	
	private String domain;
	private String host;
	private int port;
	private int timeout;
	private String applicationID;
	private boolean isSecureConnection;
	private boolean selfSignedCertificateEnabled;

	protected ConnectionConfiguration(String domain, String host, int port, int timeout,
							String applicationID, boolean isSecureConnection, boolean selfSigned){
		this.domain = domain;
		this.host = host;
		this.port = port;
		this.timeout = timeout;
		this.applicationID = applicationID;
		this.isSecureConnection = isSecureConnection;
		this.selfSignedCertificateEnabled = selfSigned;
	}
	
	/**
	 * Returns the XMPP domain.
	 * In most cases, the XMPP domain equals the DNS domain.
	 * @return Domain domain name.
	 */
	@Override
	public String getDomain() {
		return domain;
	}

	/**
	 * Returns the configured host name of IPv4 address.
	 * By default, the hostname equals the XMPP domain name. 
	 * @return Hostname or IPv4 address.
	 */
	@Override
	public String getHost() {
		return host;
	}

	/**
	 * Returns the XMPP server port.
	 * Defaults to the standard port 5222.
	 * @return Server port.
	 */
	@Override
	public int getPort() {
		return port;
	}

	/**
	 * Returns the application identifier.
	 * The identifier is an arbitrary string, which should be unique for the application.
	 * @return Application identifier.
	 */
	@Override
	public String getApplicationID() {
		return applicationID;
	}

	/**
	 * Checks if the connection is configured to be secure.
	 * Defaults to <code>true</code>. 
	 * @return <code>true</code> if the connection is configured to be TLS encrypted, otherwise <code>false</code>.
	 */
	@Override
	public boolean isSecureConnection() {
		return isSecureConnection;
	}

	/**
	 * Checks if the connection accepts self-signed certificates.
	 * Defaults to <code>true</code>. 
	 * @return <code>true</code> if self-signed certificates are accepted, otherwise <code>false</code>.
	 */
	@Override
	public boolean isSelfSignedCertificateEnabled() {
		return selfSignedCertificateEnabled;
	}

	/**
	 * Returns the duration until a request times out.
	 * Defaults to 2000 ms.
	 * @return Timeout in milliseconds.
	 */
	@Override
	public int requestTimeout() {
		return timeout;
	}

}
