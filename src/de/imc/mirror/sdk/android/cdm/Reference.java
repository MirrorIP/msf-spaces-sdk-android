package de.imc.mirror.sdk.android.cdm;

import java.io.Serializable;

import org.jdom2.Element;

/**
 * Model for a reference as used in the CDM 2.0+.
 * @author simon.schwantzer(at)im-c.de
 */
public class Reference implements Serializable, de.imc.mirror.sdk.cdm.Reference {
	private static final long serialVersionUID = 1L;
	private final String id;
	private final ReferenceType referenceType;
	
	/**
	 * Creates a reference of type {@link de.imc.mirror.sdk.cdm.ReferenceType#DEPENDENCY}.
	 * @param id Identifier of the referenced data object.
	 */
	public Reference(String id) {
		this(id, ReferenceType.DEPENDENCY);
	}
	
	/**
	 * Creates a reference.
	 * @param id Identifier of the referenced data object.
	 * @param referenceType Type of the reference, i.e. {@link de.imc.mirror.sdk.cdm.ReferenceType#DEPENDENCY} (default) or {@link de.imc.mirror.sdk.cdm.ReferenceType#WEAK}. 
	 */
	public Reference(String id, ReferenceType referenceType) {
		this.id = id;
		this.referenceType = referenceType;
	}
	
	/**
	 * Creates a reference object based on a related CDM element.
	 * @param element XML element containing the reference information.
	 */
	public Reference(Element element) {
		id = element.getAttributeValue("id");
		referenceType = ReferenceType.getTypeForString(element.getAttributeValue("type"));
	}
	
	/**
	 * Returns the data object identifier of the reference.
	 * @return Data object identifier.
	 */
	@Override
	public String getId() {
		return id;
	}
	
	/**
	 * Returns the type of the reference. A reference is either {@link de.imc.mirror.sdk.cdm.ReferenceType#WEAK} or marked as {@link de.imc.mirror.sdk.cdm.ReferenceType#DEPENDENCY}.
	 * Dependencies are taken into consideration for the data object lifetime management. Weak dependencies indicate a minor connection between two data objects.
	 * @return {@link de.imc.mirror.sdk.cdm.ReferenceType#DEPENDENCY} or {@link de.imc.mirror.sdk.cdm.ReferenceType#WEAK}.
	 */
	@Override
	public ReferenceType getReferenceType() {
		return referenceType;
	}
	
	@Override
	public Element generateXMLElement(String namespaceURI) {
		Element element = new Element("reference", namespaceURI);
		element.setAttribute("id", id);
		if (referenceType == ReferenceType.WEAK) {
			element.setAttribute("type", referenceType.toString());
		} // DEPENDENCY is default
		return element;
	}
}
