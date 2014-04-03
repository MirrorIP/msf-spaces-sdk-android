package de.imc.mirror.sdk.android;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jdom2.Element;

/**
 * Represents a channel used by spaces to exchange data. 
 * @author nmach, simon.schwantzer(at)im-c.de
 */
public class SpaceChannel implements Serializable, de.imc.mirror.sdk.SpaceChannel {

	private static final long serialVersionUID = 1L;
	private Map<String, String> properties;
	private String type;
	
	/**
	 * Create a new SpaceChannelObject with the given type and properties.
	 * @param type The type of the SpaceChannel.
	 * @param properties The properties of the SpaceChannel.
	 */
	public SpaceChannel(String type, Map<String, String> properties){
		this.type = type;
		this.properties = properties;
	}
	
	/**
	 * Returns a map of channel properties.
	 * The availability of properties depends on the channel type. 
	 * @return Unmodifiable map of channel properties, i.e., key-value pairs.
	 */
	@Override
	public Map<String, String> getProperties() {
		return Collections.unmodifiableMap(properties);
	}
	
	/**
	 * Returns the channel type.
	 * @return Type of the channel.
	 */
	@Override
	public String getType() {
		return type;
	}
	
	private synchronized void writeObject(ObjectOutputStream s) throws IOException{
		Element element = new Element("SpaceChannel");
		element.addContent(new Element("type").setText(type));
		Element propertiesElement = new Element("Properties");
		for (String key:properties.keySet()){
			propertiesElement.addContent(new Element(key).setText(properties.get(key)));			
		}
		element.addContent(propertiesElement);
		s.writeObject(element);
	}
	
	private synchronized void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException{
		Element element = (Element)s.readObject();
		this.type = element.getChildText("type");
		Element propertiesElement = element.getChild("Properties");
		properties = new HashMap<String, String>();
		for(Element elem:propertiesElement.getChildren()){
			this.properties.put(elem.getName(), elem.getText());
		}
	}

}
