package de.imc.mirror.sdk.android;

/**
 * Builder for connection configurations.
 * @author nmach, simon.schwantzer(at)im-c.de
 *
 */
public class ConnectionConfigurationBuilder {
	private static final int DEFAULT_PORT = 5222;
	private static final boolean DEFAULT_SECURE_CONNECTION = true;
	private static final boolean DEFAULT_ALLOW_SELFSIGNED_CERTS = true;
	private static final int DEFAULT_TIMEOUT = 2000;
	
	private String domain;
	private String host;
	private int port;
	private int timeout;
	private String applicationID;
	private boolean isSecureConnection;
	private boolean selfSignedCertificateEnabled;
	
	/**
	 * Creates the builder with default values.
	 * @param domain Name of the XMPP domain to connect to.
	 * @param applicationID Arbitrary string identifying the application.
	 */
	public ConnectionConfigurationBuilder(String domain, String applicationID) {
		port = DEFAULT_PORT;
		isSecureConnection = DEFAULT_SECURE_CONNECTION;
		selfSignedCertificateEnabled = DEFAULT_ALLOW_SELFSIGNED_CERTS;
		timeout = DEFAULT_TIMEOUT;
		this.domain = domain;
		this.applicationID = applicationID;
	}
	
	/**
	 * Sets the XMPP domain.
	 * In most cases, the XMPP domains equals the DNS domain.
	 * @param domain Domain name to set.
	 * @return Builder instance.
	 */
	public ConnectionConfigurationBuilder setDomain(String domain){
		this.domain = domain;
		return this;
	}
	
	/**
	 * Sets the XMPP host name or IPv4 address.
	 * If not set, it is assumed the hostname equals the XMPP domain.
	 * @param host Hostname or IPv4 address of the XMPP server.
	 * @return Builder instance.
	 */
	public ConnectionConfigurationBuilder setHost(String host){
		this.host = host;
		return this;
	}
	
	/**
	 * Sets the port for the XMPP connection.
	 * If not set, the standard port (5222) is assumed.
	 * @param port Port for the XMPP connection.
	 * @return Builder instance.
	 */
	public ConnectionConfigurationBuilder setPort(int port){
		this.port = port;
		return this;
	}
	
	/**
	 * Sets the identifier for the application.
	 * The identifier is an arbitrary string, which should be unique for the application.
	 * @param applicationID Arbitrary string identifying the application.
	 * @return Builder instance.
	 */
	public ConnectionConfigurationBuilder setApplicationId(String applicationID){
		this.applicationID = applicationID;
		return this;
	}
	
	/**
	 * Specifies if the connection should be TLS encrypted or not.
	 * @param isSecure <code>true</code> to configure the connection to be encrypted, otherwise <code>false</code>.
	 * @return Builder instance.
	 */
	public ConnectionConfigurationBuilder setSecureConnection(boolean isSecure){
		this.isSecureConnection = isSecure;
		return this;
	}
	
	/**
	 * Specifies if self-signed certificates should be accepted or not.
	 * @param acceptSelfSignedCerts <code>true</code> if self-signed certificates should be accepted, otherwise <code>false</code>.
	 * @return Builder instance.
	 */
	public ConnectionConfigurationBuilder setSelfSignedCertificateEnabled(boolean acceptSelfSignedCerts){
		this.selfSignedCertificateEnabled = acceptSelfSignedCerts;
		return this;
	}
	
	/**
	 * Sets the time the client should wait for an response from the server
	 * @param timeout Timeout for server requests.
	 * @return Builder instance.
	 */
	public ConnectionConfigurationBuilder setTimeout(int timeout) {
		this.timeout = timeout;
		return this;
	}
	
	/**
	 * Builds an connection configuration object based on the given settings.
	 * @return Connection configuration object.
	 */
	public ConnectionConfiguration build() {
		if (applicationID == null || applicationID.trim().length() == 0) {
			throw new IllegalStateException("The application identifier is not initialized or empty, but has to be set.");
		}
		boolean isDomainSet = (domain != null) && (domain.trim().length() != 0);
		boolean isHostSet = (host != null) && (host.trim().length() != 0);
		if (!isDomainSet && !isHostSet){
			throw new IllegalStateException("Either hostname or domain has to be set.");
		}
		if (!isDomainSet && isHostSet){
			domain = host;
		} else if (!isHostSet && isDomainSet){
			host = domain;
		}
		return new ConnectionConfiguration(domain, host, port, timeout, applicationID, isSecureConnection, selfSignedCertificateEnabled);
	}

}
