package de.imc.mirror.sdk.android.exceptions;

/**
 * Exception thrown when a request fails.
 * @author simon.schwantzer(at)im-c.de
 */
public class RequestException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public RequestException(String message, Throwable e) {
		super(message, e);
	}
}
