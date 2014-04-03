package de.imc.mirror.sdk.android.packet;

import java.util.ArrayList;
import java.util.List;

import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.output.XMLOutputter;
import org.jivesoftware.smack.packet.IQ;

import de.imc.mirror.sdk.DataObject;
import de.imc.mirror.sdk.config.NamespaceConfig;
import de.imc.mirror.sdk.android.DataObjectBuilder;

public class QueryResponseIQ extends IQ {

	private List<DataObject> retrievedDataObjects;	
	private Element childElement;
	
	/**
	 * Creates a IQ based on the given query response.
	 * @param queryElement Child element of the response.
	 */
	protected QueryResponseIQ(Element queryElement) {
		this.childElement = queryElement;
		retrievedDataObjects = new ArrayList<DataObject>();
		Element resultElement = queryElement.getChild("result", Namespace.getNamespace(NamespaceConfig.PERSISTENCE_SERVICE));
		for (Element dataObjectElement : resultElement.getChildren()) {
			DataObjectBuilder builder = new DataObjectBuilder(dataObjectElement, dataObjectElement.getNamespaceURI()); 
			retrievedDataObjects.add(builder.build());
		}
	}
	
	/**
	 * Returns the result of an query. 
	 * @return Result as list of data objects if the IQ is a query result, otherwise <code>null</code>. The list may be empty.
	 */
	public List<DataObject> getResult() {
		return retrievedDataObjects;
	}
	
	@Override
	public String getChildElementXML() {
		XMLOutputter out = new XMLOutputter();
		return out.outputString(childElement);
	}
}
