package de.imc.mirror.sdk.android;

import java.io.IOException;
import java.io.StringReader;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;

import de.imc.mirror.sdk.android.utils.DatatypeConverter;
import de.imc.mirror.sdk.cdm.CDMVersion;
import de.imc.mirror.sdk.config.NamespaceConfig;

/**
 * Builder for data objects.
 * @author nicolas.mach(at)im-c.de
 */
public class DataObjectBuilder {
	protected final String elementName;
	protected final String namespace;
	protected Element element;
	
	/**
	 * Builder for data objects.
	 * @param elementName The tag name of the root element.
	 * @param namespace The namespace for the data object.
	 */
	public DataObjectBuilder(String elementName, String namespace){
		this.elementName = elementName;
		this.namespace = namespace;
		this.element = new Element(elementName, namespace);
	}
	
	/**
	 * Builder for building a data object.
	 * @param element A XML element to be used as root element.
	 * @param namespace The namespace for the data object.
	 */
	public DataObjectBuilder(Element element, String namespace){
		this.element = element;
		this.elementName = element.getName();
		this.namespace = namespace;
	}
	
	/**
	 * Sets the Common Data Model information for the object.
	 * The CDM data object can be generated using the {@link CDMDataBuilder}.
	 * @param cdmData The CDMData to set.
	 * @return This builder instance.
	 */
	public DataObjectBuilder setCDMData(CDMData cdmData){
		cdmData.applyToElement(element);
		CDMVersion version = cdmData.getCDMVersion();
		element.setAttribute("cdmVersion", version.getVersionString());
		return this;
	}
	
	/**
	 * This method adds a CDT creationInfo element.
	 * @param person Identifier for the person who updated the data, e.g., a JID, an email address, or a name. It is recommended to use a JID if available. May be null.
	 * @param date Date the object was created. May be null;
	 * @param application Identifier for the application which created the data, for instance the namespace of a MIRROR application. May be null.
	 * @return This builder instance.
	 */
	public DataObjectBuilder addCDTCreationInfo(Date date, String person, String application) {
		element.addNamespaceDeclaration(Namespace.getNamespace("cdt", NamespaceConfig.MODEL_CDT));
		Element creationInfoElement = new Element("creationInfo", this.namespace);
		if (date != null) {
			Element dateElement = new Element("date", NamespaceConfig.MODEL_CDT);
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(date);
			dateElement.setText(DatatypeConverter.printDateTime(calendar));
			creationInfoElement.addContent(dateElement);
		}
		
		if (person != null) {
			Element personElement = new Element("person", NamespaceConfig.MODEL_CDT);
			personElement.setText(person);
			creationInfoElement.addContent(personElement);
		}
		
		if (application != null){
			Element applicationElement = new Element("application", NamespaceConfig.MODEL_CDT);
			applicationElement.setText(application);
			creationInfoElement.addContent(applicationElement);
		}
		element.addContent(creationInfoElement);
		return this;
	}
	
	/**
	 * Adds a new child element to the root element.
	 * @param name The name of the new element.
	 * @param content The content of the new element.
	 * @param parseContent If set to <code>true</code> the content will be parsed as XML, otherwise it will be added as text. The parsed element shares the namespace with the root element.
	 * @return This builder instance.
	 * @throws IllegalArgumentException The given content could not be parsed.
	 */
	public DataObjectBuilder addElement(String name, String content, boolean parseContent) throws IllegalArgumentException {
		Element newElem = new Element(name, this.namespace);
		if (content != null) {
			if (parseContent) {
				try {
					SAXBuilder reader = new SAXBuilder();
					StringReader in = new StringReader(content);
					Document document = null;
					document = reader.build(in);
					Element elem = document.getRootElement();
					elem.setNamespace(element.getNamespace());
					newElem.addContent(elem.detach());
				} catch (JDOMException e) {
					throw new IllegalArgumentException("Failed to parse content.", e);
				} catch (IOException e) {
					throw new IllegalArgumentException("Failed to access content to parse.", e);
				}
			} else {
				newElem.setText(content);
			}	
		}
		
		element.addContent(newElem);
		return this;
	}
	
	/**
	 * Adds a new child element to the root element.
	 * @param name The name of the new element.
	 * @param attributes A map of attribute names and the corresponding values for the new element. May be null.
	 * @param content The content of the new element.
	 * @param parseContent If set to <code>true</code> the content will be parsed as XML, otherwise it will be added as text.
	 * @return This builder instance.
	 */
	public DataObjectBuilder addElement(String name, Map<String, String> attributes, String content, boolean parseContent) {			
		Element newElem = new Element(name, this.namespace);
		if (content != null) {
			if (parseContent) {
				try {
					SAXBuilder reader = new SAXBuilder();
					StringReader in = new StringReader(content);
					Document document;
					document = reader.build(in);
					Element elem = document.getRootElement();
					newElem.addContent(elem);
				} catch (JDOMException e) {
					Logger logger = Logger.getAnonymousLogger();
					logger.log(Level.WARNING, "Error while parsing the value", e);
				} catch (IOException e) {
					Logger logger = Logger.getAnonymousLogger();
					logger.log(Level.WARNING, "Error while parsing the value", e);
				}
			} else {
				newElem.setText(content);
			}
		}
		if (attributes != null){
			for (String attrName:attributes.keySet()){
				newElem.setAttribute(attrName, attributes.get(attrName));
			}
		}
		element.addContent(newElem);
		
		return this;
	}
	
	/**
	 * Adds the given element as child element for this data object.
	 * @param element XML element to add.
	 * @return This builder instance.
	 */
	public DataObjectBuilder addElement(Element element) {
		this.element.addContent(element);
		return this;
	}

	/**
	 * Sets an attribute value for the root element. Any existing attribute with the same name will be changed. 
	 * @param attributeName The name of the attribute. 
	 * @param value The value of the attribute.
	 * @return The modified Builder.
	 */
	public DataObjectBuilder setAttribute(String attributeName, String value) {
		element.setAttribute(attributeName, value);
		return this;
	}
	
	/**
	 * Sets attribute values for the root element. Any existing attribute with the same name will be changed.
	 * @param attributes Map consisting of the attribute name and value to be set.
	 * @return The modified Builder.
	 */
	public DataObjectBuilder setAttributes(Map<String, String> attributes) {
		for (String name:attributes.keySet()){
			element.setAttribute(name, attributes.get(name));
		}
		return this;
	}
	
	/**
	 * Returns the root element for the dataobject to be build.
	 * @return The root element.
	 */
	public Element getRootElement() {
		return element;
	}
	
	/**
	 * Builds the data object.
	 * @return A new data object based on the given information.
	 */
	public DataObject build() {
		return new DataObject(this);
	}
}
