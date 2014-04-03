package de.imc.mirror.sdk.android.filter;

import org.jdom2.Element;

import de.imc.mirror.sdk.DataObject;

/**
 * Request only data objects which refer to a specific object.
 * @author simon.schwantzer(at)im-c.de
 *
 */
public class ReferencesFilter implements de.imc.mirror.sdk.filter.ReferencesFilter {
	private final String referenceId;
	
	/**
	 * Creates the filter.
	 * @param referenceId Identifier for the data object which should be referenced.
	 */
	public ReferencesFilter(String referenceId) {
		this.referenceId = referenceId;
	}
	
	@Override
	public Element getFilterAsXML(String queryNamespace) {
		Element element = new Element("references", queryNamespace);
		element.setAttribute("id", referenceId);
		return element;
	}

	@Override
	public boolean isDataObjectValid(DataObject dataObject) {
		String objectRefValue = dataObject.getElement().getAttributeValue("ref");
		if (objectRefValue == null || !objectRefValue.equals(referenceId)) {
			return false;
		}
		return true;
	}

	@Override
	public String getReferenceId() {
		return referenceId;
	}
	
	@Override
	public int hashCode() {
		int hc = 17;
	    int hashMultiplier = 59;
	    hc = hc * hashMultiplier + referenceId.hashCode();
		return hc;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof DataModelFilter)) return false;
		
		ReferencesFilter that = (ReferencesFilter) obj;
		if (!this.referenceId.equals(that.referenceId)) return false;
		
		return true;
	}

}
