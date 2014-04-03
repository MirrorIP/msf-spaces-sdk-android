package de.imc.mirror.sdk.android.cdm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jdom2.Element;

import de.imc.mirror.sdk.cdm.CDMElement;
import de.imc.mirror.sdk.cdm.CDMVersion;
import de.imc.mirror.sdk.android.CDMData;
import de.imc.mirror.sdk.android.exceptions.InvalidBuildException;

/**
 * Typed class for Common Data Models of version 1.0.
 * @author nmach, simon.schwantzer(at)im-c.de
 */
public class CDMData_1_0 extends CDMData implements de.imc.mirror.sdk.cdm.CDMData_1_0 {
	private static final long serialVersionUID = 3L;
	private final String customId;
	private final String modelVersion;
	private final String publisher;
	private final String ref;
	
	public CDMData_1_0(Element element) {
		super(element);
		customId = element.getAttributeValue("customId");
		modelVersion = element.getAttributeValue("modelVersion");
		publisher = element.getAttributeValue("publisher");
		ref = element.getAttributeValue("ref");
	}
	
	public CDMData_1_0(Map<String, String> attributes, Map<String, CDMElement> elements) throws InvalidBuildException {
		super(attributes, elements);
		List<String> buildErrors = new ArrayList<String>();

		modelVersion = attributes.get("modelVersion");
		if (modelVersion == null || modelVersion.trim().length() == 0) {
			buildErrors.add("Missing or empty attribute: modelVersion.");
		}
		customId = attributes.get("customId");
		publisher = attributes.get("publisher");
		ref = attributes.get("ref");
		
		if (!buildErrors.isEmpty()) {
			throw new InvalidBuildException("Failed to create CDM 1.0 object with the given data." , buildErrors);
		}
	}
	
	/**
	 * Verifies if the given element contains the attributes required for CDM version 1.0.
	 * @param element XML element to verify.
	 * @return List of errors. If the list is empty, the validation succeeded. 
	 */
	public static List<String> verify(Element element) {
		List<String> errors = new ArrayList<String>();
		String id = element.getAttributeValue("id");
		if (id == null || id.trim().length() == 0){
			errors.add("Missing or empty attribute: id.");
		}
		String timestamp = element.getAttributeValue("timestamp");
		if (timestamp == null || timestamp.trim().length() == 0){
			errors.add("Missing or empty attribute: timestamp.");
		}
		String modelVersion = element.getAttributeValue("modelVersion");
		if (modelVersion == null || modelVersion.trim().length() == 0){
			errors.add("Missing or empty attribute: modelVersion.");
		}
		return Collections.unmodifiableList(errors);
	}
	
	@Override
	public CDMVersion getCDMVersion() {
		return CDMVersion.CDM_1_0;
	}

	/**
	 * Returns the custom identifier of the data object.
	 * @return Custom object identifier or <code>null</code> if not set. 
	 */
	@Override
	public String getCustomId() {
		return customId;
	}

	/**
	 * Returns the version of the data model the object instantiates.
	 * @return Model version string.
	 */
	@Override
	public String getModelVersion() {
		return modelVersion;
	}

	/**
	 * Returns the publisher of the data object. 
	 * This attribute is optional, but is verified by the service if set.
	 * @return Full-JID of the data object publisher. 
	 */
	@Override
	public String getPublisher() {
		return publisher;
	}
	
	/**
	 * Returns the URI the object refers to, e.g., a the id of a parent object.
	 * @return URI string if a object reference is set, otherwise <code>null</code>.
	 */
	@Override
	public String getRef() {
		return ref;
	}
	
	@Override
	public void applyToElement(Element element){
		super.applyToElement(element);
		if (publisher != null){
			element.setAttribute("publisher", publisher);
		}
		if (ref != null){
			element.setAttribute("ref", ref);
		}
		if (customId != null){
			element.setAttribute("customId", customId);
		}
		element.setAttribute("modelVersion", modelVersion);
	}
}
