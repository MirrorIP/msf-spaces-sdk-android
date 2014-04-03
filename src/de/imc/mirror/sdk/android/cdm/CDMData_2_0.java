package de.imc.mirror.sdk.android.cdm;

import java.io.Serializable;
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
 * Typed class for Common Data Models of version 2.0.
 * @author nmach, simon.schwantzer(at)im-c.de
 */
public class CDMData_2_0 extends CDMData implements Serializable, de.imc.mirror.sdk.cdm.CDMData_2_0 {
	
	private static final long serialVersionUID = 1L;
	private final String customId;
	private final String modelVersion;
	private final String publisher;
	private final String ref;
	private final String updates;
	private final String copyOf;
	private final Summary summary;
	private final References references;
	private final CreationInfo creationInfo;
	
	public CDMData_2_0(Element element) {
		super(element);
		customId = element.getAttributeValue("customId");
		modelVersion = element.getAttributeValue("modelVersion");
		publisher = element.getAttributeValue("publisher");
		ref = element.getAttributeValue("ref");
		updates = element.getAttributeValue("updates");
		copyOf = element.getAttributeValue("copyOf");
		Element referencesElement = element.getChild("references", element.getNamespace());
		if (referencesElement != null) {
			references = new References(referencesElement);
		} else {
			references = null;
		}
		Element creationInfoElement = element.getChild("creationInfo", element.getNamespace());
		if (creationInfoElement != null) {
			creationInfo = new CreationInfo(creationInfoElement);
		} else {
			creationInfo = null;
		}
		Element summaryElement = element.getChild("summary", element.getNamespace());
		if (summaryElement != null) {
			summary = new Summary(summaryElement);
		} else {
			summary = null;
		}
	}
	
	public CDMData_2_0(Map<String, String> attributes, Map<String, CDMElement> elements) throws InvalidBuildException {
		super(attributes, elements);
		List<String> buildErrors = new ArrayList<String>();
		
		modelVersion = attributes.get("modelVersion");
		if (modelVersion == null || modelVersion.trim().length() == 0){
			buildErrors.add("Missing or empty attribute: modelVersion.");
		}
		
		customId = attributes.get("customId");
		copyOf = attributes.get("copyOf");
		publisher = attributes.get("publisher");
		ref = attributes.get("ref");
		updates = attributes.get("updates");
		creationInfo = (CreationInfo) elements.get("creationInfo");
		summary = (Summary) elements.get("summary");
		references = (References) elements.get("references");
		if (references != null && references.getReferences().isEmpty()) {
			buildErrors.add("The references object needs at least one item.");
		}
		
		if (!buildErrors.isEmpty()) {
			throw new InvalidBuildException("Failed to create CDM 2.0 object with the given data." , buildErrors);
		}
	}
	
	/**
	 * Verifies if the given element contains the attributes required for CDM version 2.0.
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
		return CDMVersion.CDM_2_0;
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
	public void applyToElement(Element element) {
		super.applyToElement(element);
		element.setAttribute("modelVersion", modelVersion);
		if (publisher != null) element.setAttribute("publisher", publisher);
		if (ref != null) element.setAttribute("ref", ref);
		if (customId != null) element.setAttribute("customId", customId);
		if (copyOf != null) element.setAttribute("copyOf", copyOf);
		if (updates != null) element.setAttribute("updates", updates);
		if (summary != null) {
			element.addContent(summary.generateXMLElement(element.getNamespaceURI()));
		}
		if (references != null) {
			element.addContent(references.generateXMLElement(element.getNamespaceURI()));
		}
		if (creationInfo != null) {
			element.addContent(creationInfo.generateXMLElement(element.getNamespaceURI()));
		}
	}

	@Override
	public boolean isUpdate() {
		return updates != null;
	}

	@Override
	public String getUpdatedObjectId() {
		return updates;
	}

	@Override
	public boolean isCopy() {
		return copyOf != null;
	}

	@Override
	public String getCopyOf() {
		return copyOf;
	}

	@Override
	public Summary getSummary() {
		return summary;
	}

	@Override
	public References getReferences() {
		return references;
	}

	@Override
	public CreationInfo getCreationInfo() {
		return creationInfo;
	}
}
