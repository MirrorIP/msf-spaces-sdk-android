package de.imc.mirror.sdk.android.filter;

import org.jdom2.Element;

import de.imc.mirror.sdk.DataObject;

/**
 * Only data objects from the given publisher (bare-JID or full-JID) are returned. Removes all non-personalized data objects.
 * @author simon.schwantzer(at)im-c.de
 *
 */
public class PublisherFilter implements de.imc.mirror.sdk.filter.PublisherFilter {
	
	private final String publisher;
	
	/**
	 * Creates a new publisher filter.
	 * @param publisher Either bare-JID or full-JID of the publisher.
	 */
	public PublisherFilter(String publisher) {
		this.publisher = publisher;
	}

	@Override
	public Element getFilterAsXML(String queryNamespace) {
		Element element = new Element("publisher", queryNamespace);
		element.setText(publisher);
		return element;
	}

	@Override
	public boolean isDataObjectValid(DataObject dataObject) {
		String objectPublisher = dataObject.getElement().getAttributeValue("publisher");
		if (objectPublisher == null || !objectPublisher.startsWith(publisher)) {
			return false;
		}
		return true;
	}

	@Override
	public String getPublisher() {
		return publisher;
	}
	
	@Override
	public int hashCode() {
		int hc = 17;
	    int hashMultiplier = 59;
    	hc = hc * hashMultiplier + publisher.hashCode();
    	return hc;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof PublisherFilter)) return false;

		PublisherFilter that = (PublisherFilter) obj;
		if (!this.publisher.equals(that.publisher)) return false;
		
		return true;
	}
}
