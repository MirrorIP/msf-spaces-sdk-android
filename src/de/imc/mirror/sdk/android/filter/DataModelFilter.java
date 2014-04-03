package de.imc.mirror.sdk.android.filter;

import org.jdom2.Element;

import de.imc.mirror.sdk.CDMData;
import de.imc.mirror.sdk.DataObject;

/**
 * Filter for data model information.
 * @author simon.schwantzer(at)im-c.de
 */
public class DataModelFilter implements de.imc.mirror.sdk.filter.DataModelFilter {
	private final String namespace;
	private final String version;
	
	/**
	 * Creates a filter for the data model an object has to instantiate in an arbitrary version.
	 * @param namespace Namespace of the data model.
	 */
	public DataModelFilter(String namespace) {
		this(namespace, null);
	}
	
	/**
	 * Creates a filter for the data model an object has to instantiate in an specific version.
	 * @param namespace Namespace of the data model.
	 * @param version Version of the data model.
	 */
	public DataModelFilter(String namespace, String version) {
		this.namespace = namespace;
		this.version = version;
	}
	
	@Override
	public Element getFilterAsXML(String queryNamespace) {
		Element element = new Element("dataModel", queryNamespace);
		element.setAttribute("namespace", namespace);
		if (this.version != null) {
			element.setAttribute("version", version);
		}
		return element;
	}

	@Override
	public boolean isDataObjectValid(DataObject dataObject) {
		if (!namespace.equals(dataObject.getNamespaceURI())) {
			return false;
		}
		if (version != null) {
			CDMData cdmData = dataObject.getCDMData();
			if (cdmData == null) {
				return false;
			}
			try {
				String modelVersion = (String) cdmData.getClass().getMethod("modelVersion").invoke(cdmData);
				return version.equals(modelVersion);
			} catch (Exception e) {
				// CDM < 1.0
				return false;
			}
		} else {
			return true;
		}
	}

	@Override
	public String getNamespace() {
		return namespace;
	}

	@Override
	public String getVersion() {
		return version;
	}

	@Override
	public int hashCode() {
		int hc = 17;
	    int hashMultiplier = 59;
	    hc = hc * hashMultiplier + namespace.hashCode();
	    if (version != null) {
	    	hc = hc * hashMultiplier + version.hashCode();
	    }
		return hc;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof DataModelFilter)) return false;
		DataModelFilter that = (DataModelFilter) obj;
		if (!this.namespace.equals(that.namespace)) return false;
		if (this.version != null && !this.version.equals(that.version)) return false;
		if (this.version == null && that.version != null) return false;
		return true;
	}
}
