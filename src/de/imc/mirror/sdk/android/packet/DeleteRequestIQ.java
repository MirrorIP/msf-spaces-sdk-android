package de.imc.mirror.sdk.android.packet;

import java.util.Set;

import org.jdom2.Element;
import org.jdom2.output.XMLOutputter;
import org.jivesoftware.smack.packet.IQ;

import de.imc.mirror.sdk.config.NamespaceConfig;
import de.imc.mirror.sdk.exceptions.QueryException;

public class DeleteRequestIQ extends IQ {
	
	/**
	 * Creates a request to delete a single data object.
	 * @param objectId Identifier for the data object to delete.
	 * @return IQ for the request.
	 */
	public static DeleteRequestIQ createDeleteRequest(String objectId) {
		Element deleteElement = new Element("delete", NamespaceConfig.PERSISTENCE_SERVICE);
		Element objectElement = new Element("object", NamespaceConfig.PERSISTENCE_SERVICE);
		objectElement.setAttribute("id", objectId);
		deleteElement.addContent(objectElement);
		return new DeleteRequestIQ(deleteElement);
	}
	
	/**
	 * Create a request to delete multiple data objects.
	 * @param objectIds Set of identifiers for the data objects to delete.
	 * @return IQ for the request.
	 * @throws QueryException No valid query can be created based on the given paramters.
	 */
	public static DeleteRequestIQ createDeleteRequest(Set<String> objectIds) throws QueryException {
		if (objectIds.isEmpty()) throw new QueryException(QueryException.Type.BAD_REQUEST, "At least one data object identifier is required to perform a delete request.");
		
		Element deleteElement = new Element("delete", NamespaceConfig.PERSISTENCE_SERVICE);
		Element objectsElement = new Element("objects", NamespaceConfig.PERSISTENCE_SERVICE);
		for (String objectId : objectIds) {
			Element objectElement = new Element("object", NamespaceConfig.PERSISTENCE_SERVICE);
			objectElement.setAttribute("id", objectId);
			objectsElement.addContent(objectElement);
		}
		deleteElement.addContent(objectsElement);
		return new DeleteRequestIQ(deleteElement);
	}

	private Element childElement;
	
	/**
	 * Private constructor.
	 * @param childElement Element representing the IQ packet.
	 */
	private DeleteRequestIQ(Element childElement) {
		super.setType(IQ.Type.SET);
		this.childElement = childElement;
	}
	
	@Override
	public String getChildElementXML() {
		XMLOutputter out = new XMLOutputter();
		return out.outputString(childElement);
	}
}
