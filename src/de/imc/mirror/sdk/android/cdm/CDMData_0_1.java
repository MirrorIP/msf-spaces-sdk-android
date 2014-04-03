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
 * Typed class for Common Data Models of version 0.1.
 * @author nmach, simon.schwantzer(at)im-c.de
 */
public class CDMData_0_1 extends CDMData implements de.imc.mirror.sdk.cdm.CDMData_0_1 {
	private static final long serialVersionUID = 2L;
	private final String creator;
	
	public CDMData_0_1(Element element){
		super(element);
		creator = element.getAttributeValue("creator");
	}
	
	public CDMData_0_1(Map<String, String> attributes, Map<String, CDMElement> elements) throws InvalidBuildException {
		super(attributes, elements);
		this.creator = attributes.get("creator");
	}
	
	/**
	 * Verifies if the given element contains the attributes required for CDM version 0.1.
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
	 * Returns the creator of the data object.
	 * @return Full JID of the creator of the data object. May be <code>null</code>.
	 */
	@Override
	public String getCreator() {
		return creator;
	}
	
	@Override
	public void applyToElement(Element element) {
		super.applyToElement(element);
		if (creator != null) element.setAttribute("creator", creator);
	}
}
