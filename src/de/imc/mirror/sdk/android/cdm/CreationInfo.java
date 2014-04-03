package de.imc.mirror.sdk.android.cdm;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;

import de.imc.mirror.sdk.android.utils.DatatypeConverter;

import org.jdom2.Element;

/**
 * Model for the creation information as available with CDM 2.0. 
 * @author simon.schwantzer(at)im-c.de
 *
 */
public class CreationInfo implements Serializable, de.imc.mirror.sdk.cdm.CreationInfo {
	
	private static final long serialVersionUID = 1L;
	private transient Date creationDate;
	private final String creator;
	private final String application;
	
	/**
	 * Create a new creation information for data objects.
	 * @param creationDate Date the data object was created.
	 * @param creator Identifier for the person who created the data object, e.g., the bare-JID of the user logged in. May be <code>null</code>.
	 * @param application Identifier for the application which created the data object, e.g., the application namespace. May be <code>null</code>.
	 */
	public CreationInfo(Date creationDate, String creator, String application) {
		this.creationDate = creationDate;
		this.creator = creator;
		this.application = application;
	}
	
	/**
	 * Creates a new creation information based on a related CDM XML element.
	 * @param element XML element to retrieve data from.
	 */
	public CreationInfo(Element element) {
		Element dateElement = element.getChild("date", element.getNamespace());
		this.creationDate = DatatypeConverter.parseDateTime(dateElement.getText()).getTime();
		Element personElement = element.getChild("person", element.getNamespace());
		this.creator = personElement != null ? personElement.getText() : null;
		Element applicationElement = element.getChild("application", element.getNamespace());
		this.application = applicationElement != null ? applicationElement.getText() : null;
	}

	/**
	 * Returns the date when the data object was created.
	 * @return Data of creation.
	 */
	@Override
	public Date getCreationDate() {
		return creationDate;
	}

	/**
	 * Returns the creator of the data object.
	 * @return Identifier, e.g. bare-JID, for the creator. May be <code>null</code> if no creator is set.
	 */
	@Override
	public String getCreator() {
		return creator;
	}

	/**
	 * Return the application which created the data object. 
	 * @return Identifier (e.g. namespace) of the application in which the data object was created. May be <code>null</code> if this information is not available.
	 */
	@Override
	public String getApplication() {
		return application;
	}
	
	@Override
	public Element generateXMLElement(String namespaceURI) {
		Element element = new Element("creationInfo", namespaceURI);
		Element dateElement = new Element("date", namespaceURI);
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(creationDate);
		dateElement.setText(DatatypeConverter.printDateTime(calendar));
		element.addContent(dateElement);
		if (creator != null) {
			Element personElement = new Element("person", namespaceURI);
			personElement.setText(creator);
			element.addContent(personElement);
		}
		if (application != null) {
			Element applicationElement = new Element("application", namespaceURI);
			applicationElement.setText(application);
			element.addContent(applicationElement);
		}
		return element;
	}
	
	/**
	 * Manual serialization of Date object is required.
	 */
	private synchronized void writeObject(ObjectOutputStream s) throws IOException {
		s.defaultWriteObject();
		s.writeLong(creationDate.getTime());
	}
	
	/**
	 * Manual serialization enforces manual deserialization.
	 */
	private synchronized void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
		s.defaultReadObject();
		creationDate = new Date(s.readLong());
	}
}
