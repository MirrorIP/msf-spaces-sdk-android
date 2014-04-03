package de.imc.mirror.sdk.android.filter;

import org.jdom2.Element;

import de.imc.mirror.sdk.DataObject;

/**
 * Filter for specific namespaces.
 * Restricts the query to specific namespaces, e.g. "mirror:application:moodmap:mood".
 * @author simon.schwantzer(at)im-c.de
 */
public class NamespaceFilter implements de.imc.mirror.sdk.filter.NamespaceFilter {
	private final String compareString;
	private final CompareType compareType;
	
	/**
	 * Creates a strict namespace filter with the given namespace.
	 * @param compareString Namespace as string.
	 */
	public NamespaceFilter(String compareString) {
		this(CompareType.STRICT, compareString);
	}
	
	/**
	 * Create a namespace filter.
	 * @param compareType Compare type for the namespace filter.
	 * @param compareString String to compare the namespace with.
	 */
	public NamespaceFilter(CompareType compareType, String compareString) {
		this.compareType = compareType;
		this.compareString = compareString;
	}
	
	@Override
	public Element getFilterAsXML(String queryNamespace) {
		Element element = new Element("namespace", queryNamespace);
		element.setAttribute("compareType", compareType.toString());
		element.setText(compareString);
		return element;
	}

	@Override
	public boolean isDataObjectValid(DataObject dataObject) {
		String objectNamespace = dataObject.getNamespaceURI();
		switch (compareType) {
		case STRICT:
			if (!objectNamespace.equals(compareString)) {
				return false;
			}
			break;
		case CONTAINS:
			if (!objectNamespace.contains(compareString)) {
				return false;
			}
			break;
		case REGEX:
			if (!objectNamespace.matches(compareString)) {
				return false;
			}
			break;
		}
		return true;
	}

	@Override
	public String getCompareString() {
		return compareString;
	}

	@Override
	public CompareType getCompareType() {
		return compareType;
	}
	
	@Override
	public int hashCode() {
		int hc = 17;
	    int hashMultiplier = 59;
	    hc = hc * hashMultiplier + compareType.ordinal();
    	hc = hc * hashMultiplier + compareString.hashCode();
		return hc;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof NamespaceFilter)) return false;
		NamespaceFilter that = (NamespaceFilter) obj;
		if (this.compareType != that.compareType) return false;
		if (!this.compareString.equals(that.compareString)) return false;
		return true;
	}
}
