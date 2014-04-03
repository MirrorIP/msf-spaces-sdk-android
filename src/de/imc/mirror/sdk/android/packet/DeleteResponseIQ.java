package de.imc.mirror.sdk.android.packet;

import org.jdom2.Element;
import org.jdom2.output.XMLOutputter;
import org.jivesoftware.smack.packet.IQ;

public class DeleteResponseIQ extends IQ {

	private int numberOfDeletedEntries;	
	private Element childElement;
	
	/**
	 * Creates a IQ based on the given query response.
	 * @param deleteElement Child element of the response.
	 */
	protected DeleteResponseIQ(Element deleteElement) {
		this.childElement = deleteElement;
		String objectsDeletedString = deleteElement.getAttributeValue("objectsDeleted");
		numberOfDeletedEntries = Integer.parseInt(objectsDeletedString);
	}
	
	/**
	 * Returns the number of deleted data objects of this IQ represents a response to a delete request.
	 * @return Number of entries deleted.
	 */
	public int getNumberOfDeletedEntries() {
		return numberOfDeletedEntries;
	}
	
	@Override
	public String getChildElementXML() {
		XMLOutputter out = new XMLOutputter();
		return out.outputString(childElement);
	}
}
