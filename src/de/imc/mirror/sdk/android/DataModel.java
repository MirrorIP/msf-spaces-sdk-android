package de.imc.mirror.sdk.android;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.jdom2.Element;

/**
 * A data model consists of a name space and a location of a XML schema definition file.
 * The XSD file specifies the data model in detail.  
 * @author nmach, simon.schwantzer(at)im-c.de
 */
public class DataModel implements Serializable, de.imc.mirror.sdk.DataModel {
	
	private static final long serialVersionUID = 1L;
	private String schemaLocation;
	private String namespace;
	
	/**
	 * Constructor for a DataModel.
	 * @param namespace The namespace of the DataModel.
	 * @param schemaLocation The location of the schema for this DataModel.
	 */
	public DataModel(String namespace, String schemaLocation){
		this.namespace = namespace;
		this.schemaLocation = schemaLocation;
	}

	/**
	 * Returns the namespace of this data model.
	 * @return Namespace URI
	 */
	@Override
	public String getNamespace() {
		return namespace;
	}

	/**
	 * Returns the location of the schema specifying the data model.
	 * @return URL of the related XSD file.
	 */
	@Override
	public String getSchemaLocation() {
		return schemaLocation;
	}
	
	@Override
	public int hashCode(){
		return namespace.length() + schemaLocation.length();
	}
	
	@Override
	public boolean equals(Object o){
		if (o instanceof DataModel){
			DataModel model = (DataModel) o;
			if (model.getSchemaLocation().equalsIgnoreCase(this.schemaLocation) &&
					model.getNamespace().equalsIgnoreCase(this.namespace)){
				return true;
			}
		}
		return false;
	}
	
	private synchronized void writeObject(ObjectOutputStream s) throws IOException{
		Element element = new Element("DataModel");
		element.addContent(new Element("namespace").setText(namespace));
		element.addContent(new Element("schemaLocation").setText(schemaLocation));
		s.writeObject(element);
	}
	
	private synchronized void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException{
		Element element = (Element)s.readObject();
		this.namespace = element.getChildText("namespace");
		this.schemaLocation = element.getChildText("schemaLocation");
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(200);
		builder.append("{namespace: \"").append(namespace).append("\"; schemaLocation: \"").append(schemaLocation).append("\"}");
		return builder.toString();
	}

}
