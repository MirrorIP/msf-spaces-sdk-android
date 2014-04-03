package de.imc.mirror.sdk.android.cdm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jdom2.Element;

import de.imc.mirror.sdk.cdm.CDMElement;
import de.imc.mirror.sdk.android.CDMData;
import de.imc.mirror.sdk.android.exceptions.InvalidBuildException;

/**
 * Typed class for Common Data Models of version 0.2.
 * @author nmach, simon.schwantzer(at)im-c.de
 */
public class CDMData_0_2 extends CDMData implements de.imc.mirror.sdk.cdm.CDMData_0_2 {
	private static final long serialVersionUID = 2L;
	private final String creator;
	private final String ref;
	
	public CDMData_0_2(Element element){
		super(element);
		creator = element.getAttributeValue("creator");
		ref = element.getAttributeValue("ref");
	}

	public CDMData_0_2(Map<String, String> attributes, Map<String, CDMElement> elements) throws InvalidBuildException {
		super(attributes, elements);
		
		creator = attributes.get("creator");
		ref = attributes.get("ref");
	}
	
	/**
	 * Verifies if the given element contains the attributes required for CDM version 0.2.
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
		return Collections.unmodifiableList(errors);
	}

	/**
	 * Returns the publisher of the data object. 
	 * This attribute is optional, but is verified by the service if set.
	 * @return Full-JID of the data object publisher. 
	 */
	@Override
	public String getCreator() {
		return creator;
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
		if (creator != null){
			element.setAttribute("creator", creator);
		}
		if (ref != null){
			element.setAttribute("ref", ref);
		}
	}
}
