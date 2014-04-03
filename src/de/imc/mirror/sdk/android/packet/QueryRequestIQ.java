package de.imc.mirror.sdk.android.packet;

import java.util.Set;

import org.jdom2.Element;
import org.jdom2.output.XMLOutputter;
import org.jivesoftware.smack.packet.IQ;

import de.imc.mirror.sdk.SerializableDataObjectFilter;
import de.imc.mirror.sdk.config.NamespaceConfig;
import de.imc.mirror.sdk.exceptions.QueryException;

public class QueryRequestIQ extends IQ {
	/**
	 * Creates a query for a data object with the given ID.
	 * @param objectId Identifier of the data object to retrieve.
	 * @return IQ for the query.
	 */
	public static QueryRequestIQ createQueryByObjectId(String objectId) {
		Element queryElement = new Element("query", NamespaceConfig.PERSISTENCE_SERVICE);
		Element objectElement = new Element("object", NamespaceConfig.PERSISTENCE_SERVICE);
		objectElement.setAttribute("id", objectId);
		queryElement.addContent(objectElement);
		return new QueryRequestIQ(queryElement);
	}
	
	/**
	 * Creates a filtered query for all data object with the given identifiers.
	 * @param objectIds List of the identifiers for the data objects to retrieve.
	 * @param filters Set of filters to apply. May be empty. 
	 * @return IQ for the query.
	 * @throws QueryException No valid query can be created based on the given parameters. 
	 */
	public static QueryRequestIQ createQueryByObjectIds(Set<String> objectIds, Set<SerializableDataObjectFilter> filters) throws QueryException {
		if (objectIds.isEmpty()) throw new QueryException(QueryException.Type.BAD_REQUEST, "A least on data object identifier is required to perform a query.");
		
		Element queryElement = new Element("query", NamespaceConfig.PERSISTENCE_SERVICE);
		Element objectsElement = new Element("objects", NamespaceConfig.PERSISTENCE_SERVICE);
		for (String objectId : objectIds) {
			Element objectElement = new Element("object", NamespaceConfig.PERSISTENCE_SERVICE);
			objectElement.setAttribute("id", objectId);
			objectsElement.addContent(objectElement);
		}
		queryElement.addContent(objectsElement);
		if (!filters.isEmpty()) {
			Element filtersElement = new Element("filters", NamespaceConfig.PERSISTENCE_SERVICE);
			for (SerializableDataObjectFilter filter : filters) {
				filtersElement.addContent(filter.getFilterAsXML(NamespaceConfig.PERSISTENCE_SERVICE));
			}
			queryElement.addContent(filtersElement);
		}
		
		return new QueryRequestIQ(queryElement);
	}
	
	/**
	 * Creates a filtered query for the data object of a given space.
	 * @param spaceId Identifier of the space to retrieve data objects from.
	 * @param filters Set of filters to apply. May be empty. 
	 * @return IQ for the query.
	 */
	public static QueryRequestIQ createQueryBySpace(String spaceId, Set<SerializableDataObjectFilter> filters) {
		Element queryElement = new Element("query", NamespaceConfig.PERSISTENCE_SERVICE);
		Element spaceElement = new Element("objectsForSpace", NamespaceConfig.PERSISTENCE_SERVICE);
		spaceElement.setAttribute("id", spaceId);
		queryElement.addContent(spaceElement);
		if (!filters.isEmpty()) {
			Element filtersElement = new Element("filters", NamespaceConfig.PERSISTENCE_SERVICE);
			for (SerializableDataObjectFilter filter : filters) {
				filtersElement.addContent(filter.getFilterAsXML(NamespaceConfig.PERSISTENCE_SERVICE));
			}
			queryElement.addContent(filtersElement);
		}
		return new QueryRequestIQ(queryElement);
	}
	
	/**
	 * Creates a space-spanning, filtered query for data object.
	 * @param spaceIds List of the identifiers for spaces to perform query on.
	 * @param filters Set of filters to apply. May be empty. 
	 * @return IQ for the query.
	 * @throws QueryException No valid query can be created based on the given parameters. 
	 */
	public static QueryRequestIQ createQueryBySpaces(Set<String> spaceIds, Set<SerializableDataObjectFilter> filters) throws QueryException {
		if (spaceIds.isEmpty()) throw new QueryException(QueryException.Type.BAD_REQUEST, "A least on data space identifier is required to perform a query.");
		
		Element queryElement = new Element("query", NamespaceConfig.PERSISTENCE_SERVICE);
		Element spacesElement = new Element("objectsForSpaces", NamespaceConfig.PERSISTENCE_SERVICE);
		for (String spaceId : spaceIds) {
			Element spaceElement = new Element("space", NamespaceConfig.PERSISTENCE_SERVICE);
			spaceElement.setAttribute("id", spaceId);
			spacesElement.addContent(spaceElement);
		}
		queryElement.addContent(spacesElement);
		if (!filters.isEmpty()) {
			Element filtersElement = new Element("filters", NamespaceConfig.PERSISTENCE_SERVICE);
			for (SerializableDataObjectFilter filter : filters) {
				filtersElement.addContent(filter.getFilterAsXML(NamespaceConfig.PERSISTENCE_SERVICE));
			}
			queryElement.addContent(filtersElement);
		}
		
		return new QueryRequestIQ(queryElement);
	}
	
	private Element childElement;
	
	/**
	 * Private constructor.
	 * @param childElement Element representing the IQ packet.
	 */
	private QueryRequestIQ(Element childElement) {
		super.setType(IQ.Type.GET);
		this.childElement = childElement;
	}
	
	@Override
	public String getChildElementXML() {
		XMLOutputter out = new XMLOutputter();
		return out.outputString(childElement);
	}
}
