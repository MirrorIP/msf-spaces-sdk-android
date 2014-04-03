package de.imc.mirror.sdk.android.exceptions;

import java.util.ArrayList;
import java.util.List;

/**
 * Exception thrown when a build fails.
 * @author simon.schwantzer(at)im-c.de
 */
public class InvalidBuildException extends Exception {
	private static final long serialVersionUID = 1L;
	private final List<String> buildErrors;
	
	/**
	 * Creates an exception with the given message.
	 * @param message Message describing the exception.
	 */
	public InvalidBuildException(String message) {
		super(message);
		buildErrors = new ArrayList<String>();
	}
	
	/**
	 * Creates an exception with a list of build errors.
	 * @param message Message describing the exception.
	 * @param buildErrors List of errors which occurred during the build process.
	 */
	public InvalidBuildException(String message, List<String> buildErrors) {
		super(message);
		this.buildErrors = buildErrors;
	}
	
	/**
	 * Returns the errors which occurred during the build.
	 * @return List of build error messages.
	 */
	public List<String> getBuildErrors() {
		return buildErrors;
	}
}
