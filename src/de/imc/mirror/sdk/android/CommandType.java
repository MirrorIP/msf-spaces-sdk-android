package de.imc.mirror.sdk.android;

/**
 * Enumeration for commands available in spaces.
 * @author simon.schwantzer(at)im-c.de
 *
 */
public enum CommandType {
	CREATE,
	CONFIGURE,
	DELETE,
	CHANNELS,
	MODELS,
	VERSION,
	UNKNOWN;
	
	public static CommandType getTypeForTagName(String tagName) {
		CommandType result;
		try {
			result = CommandType.valueOf(tagName.toUpperCase());
		} catch (IllegalArgumentException e) {
			result = CommandType.UNKNOWN;
		}
		
		return result;
	}
	
	/**
	 * @return Type string in lower case.
	 */
	@Override
	public String toString() {
		return super.toString().toLowerCase();
	}
}
