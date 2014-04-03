package de.imc.mirror.sdk.android;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jdom2.Element;

import de.imc.mirror.sdk.cdm.CDMElement;
import de.imc.mirror.sdk.cdm.CDMVersion;
import de.imc.mirror.sdk.android.exceptions.InvalidBuildException;
import de.imc.mirror.sdk.android.utils.DatatypeConverter;

/**
 * Superclass for the Common Data Model. 
 */
public abstract class CDMData implements Serializable, de.imc.mirror.sdk.CDMData {
	private static final long serialVersionUID = 1L;

	private final CDMVersion cdmVersion;
	private final String id;
	private final Date timestamp;
	
	/**
	 * Constructor for a new CDMData Object
	 * @param element JDomElement with all CDMInformation.
	 */
	public CDMData(Element element) {
		cdmVersion = CDMVersion.getVersionForString(element.getAttributeValue("cdmVersion"));
		id = element.getAttributeValue("id");
		String date = element.getAttributeValue("timestamp");
		Date parsedDate = null;
		if (date != null) {
			try {
				parsedDate = DatatypeConverter.parseDateTime(date).getTime(); 
			} catch (IllegalArgumentException e) {
				Logger logger = Logger.getAnonymousLogger();
				logger.log(Level.WARNING, "The timestamp seems to be in a false Format.", e);
			}
		}
		timestamp = (parsedDate != null) ? parsedDate : null;
	}
	
	/**
	 * Creates a CDM data object based on the given attributes and elements.
	 * @param attributes Map of attribute names and values.
	 * @param elements Map of element names and XML element objects.
	 * @throws InvalidBuildException Failed to create an CDMData object with the given arguments.
	 */
	public CDMData(Map<String, String> attributes, Map<String, CDMElement> elements) throws InvalidBuildException {
		List<String> buildErrors = new ArrayList<String>();
		
		cdmVersion = CDMVersion.getVersionForString(attributes.get("cdmVersion"));
		id = attributes.get("id");
		Date parsedDate = null;
		if (attributes.containsKey("timestamp")) {
			try {
				parsedDate = DatatypeConverter.parseDateTime(attributes.get("timestamp")).getTime(); 
			} catch (IllegalArgumentException e) {
				buildErrors.add("The given timestamp cannot be parsed.");
				throw new InvalidBuildException("Failed to create CDM object.", buildErrors);
			}
		}
		timestamp = (parsedDate != null) ? parsedDate : null;
	}
	
	/**
	 * Returns the version information of common data model implemented.
	 * The CDM version information is only available in all objects with support for CDM version 1.0 or higher.  
	 * @return Version information if a CDM version is set, otherwise <code>null</code>.
	 */
	@Override
	public CDMVersion getCDMVersion() {
		return cdmVersion;
	}

	/**
	 * Returns the ID of the data object.
	 * The ID is unique within the domain and set by the server.
	 * @return Data object identifier.
	 */
	@Override
	public String getId() {
		return id;
	}

	/**
	 * Returns the time the object was published.
	 * The point in time is determined by the server.
	 * @return Point in time the data object was published.
	 */
	@Override
	public Date getTimeStamp() {
		return timestamp;
	}
	
	/**
	 * Applies the data to the given XML element.
	 * @param element XML element to add CDM information to.
	 */
	public void applyToElement(Element element) {
		if (cdmVersion != null) element.setAttribute("cdmVersion", cdmVersion.getVersionString());
		if (id != null) element.setAttribute("id", id);
		if (timestamp != null) {
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(timestamp);
			element.setAttribute("timestamp", DatatypeConverter.printDateTime(calendar));
		}
	}
}
