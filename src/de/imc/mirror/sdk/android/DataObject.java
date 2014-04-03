package de.imc.mirror.sdk.android;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.jdom2.Attribute;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.output.XMLOutputter;

import de.imc.mirror.sdk.CDMData;
import de.imc.mirror.sdk.cdm.CDMVersion;
import de.imc.mirror.sdk.android.cdm.CDMData_0_1;
import de.imc.mirror.sdk.android.cdm.CDMData_0_2;
import de.imc.mirror.sdk.android.cdm.CDMData_1_0;
import de.imc.mirror.sdk.android.cdm.CDMData_2_0;

/**
 * A data object represents an item published on a pubsub node of a space.
 * @author nmach, simon.schwantzer(at)im-c.de
 *
 */
public class DataObject implements Serializable, de.imc.mirror.sdk.DataObject{

	private static final long serialVersionUID = 1L;
	protected static final String CDTDATE = "cdt:date";
	protected static final String DATE = "date";
	protected static final String CDTPERSON = "cdt:person";
	protected static final String PERSON = "person";
	protected static final String PUBLISHER = "publisher";
	protected static final String ID = "id";
	protected static final String CDMVERSION = "cdmVersion";
	protected static final String MODELVERSION = "modelVersion";
	protected static final String CREATIONINFO = "creationInfo";
	protected static final String MIRROR_NSPREFIX = "mirror:";
	protected static final String CDM_0_1 = "0.1";
	protected static final String CDM_0_2 = "0.2";
	protected static final String CDM_1_0 = "1.0";
	
	private Element element;
	private String elementName;
	private CDMData cdmData;
	
	protected DataObject(DataObjectBuilder builder) {
		this.elementName = builder.elementName;
		this.element = builder.element;
		String cdmVersion = this.getCDMVersion();
		if (cdmVersion != null) {
			switch (CDMVersion.getVersionForString(cdmVersion)) {
			case CDM_0_1:
				cdmData = CDMData_0_1.verify(element).isEmpty() ? new CDMData_0_1(element) : null;
				break;
			case CDM_0_2:
				cdmData = CDMData_0_2.verify(element).isEmpty() ? new CDMData_0_2(element) : null;
				break;
			case CDM_1_0:
				cdmData = CDMData_1_0.verify(element).isEmpty() ? new CDMData_1_0(element) : null;
				break;
			case CDM_2_0:
				cdmData = CDMData_2_0.verify(element).isEmpty() ? new CDMData_2_0(element) : null;
				break;
			default:
				cdmData = null;
			}
		} else {
			cdmData = guessCDMVersion(element);
		}
	}
	
	/**
	 * Tries to create a CDM object for the given data object element. Returns the highest CDM version which validates the element.  
	 * @param element Root element of a data object.
	 * @return CDM data object or <code>null</code> if no CDM version can be applied. 
	 */
	private CDMData guessCDMVersion(Element element) {
		if (CDMData_2_0.verify(element).size() == 0) {
			return new CDMData_2_0(element);
		} else if (CDMData_1_0.verify(element).size() == 0) {
			return new CDMData_1_0(element);
		} else if (CDMData_0_2.verify(element).size() == 0) {
			return new CDMData_0_2(element);
		} else if (CDMData_0_1.verify(element).size() == 0) {
			return new CDMData_0_1(element);
		} else {
			return null;
		}
	}

	/**
	 * Returns an XML string representing the data object.
	 * @return The object as XML string.
	 */
	@Override
	public String toString(){
		XMLOutputter out = new XMLOutputter();
		return out.outputString(element);
	}
	
	/**
	 * @return The element name of the object.
	 */
	public String getElementName(){
		return elementName;
	}
	
	/**
	 * Returns the XML element for this data object.
	 * @return XML element. This is the payload published on the pubsub node.
	 */
	@Override
	public Element getElement(){
		return element;
	}
	
	/**
	 * Returns the unique identifier of this data object provided by the CDM.
	 * Refers to DataObject.getCDMData().getId();
	 * @see CDMData#getId()
	 * @return Data object identifier.
	 */
	@Override
	public String getId() {
		return element.getAttributeValue(ID);
	}
	
	/**
	 * @return The set cdmversion. If it wasn't set this method returns null.
	 */
	public String getCDMVersion(){
		return element.getAttributeValue(CDMVERSION);		
	}
	
	/**
	 * @return The set modelversion. If it wasn't set this method returns null.
	 */
	public String getModelVersion(){
		return element.getAttributeValue(MODELVERSION);		
	}

	/**
	 * Returns the common data model information for this object.
	 * If no information is contained, i.e., the data object does not instantiate a
	 * MIRROR data model, <code>null</code> will be returned.
	 * @return CDM information container or <code>null</code> if no CDM data is available.
	 */
	@Override
	public CDMData getCDMData() {
		return cdmData;
	}

	/**
	 * Returns the namespace URI of the data object.
	 * @return Namespace URI string.
	 */
	@Override
	public String getNamespaceURI() {
		return element.getNamespaceURI();
	}

	/**
	 * Checks if the data object claims to be an instance of a MIRROR data model.
	 * A simple namespace comparison is applied, but no verification.
	 * @return <code>true</code> if the XML object is from the MIRROR application namespace, otherwise <code>false</code>.
	 */
	@Override
	public boolean isMIRRORDataObject() {
		return this.getNamespaceURI().startsWith(MIRROR_NSPREFIX);
	}

	/**
	 * Returns the data model for the data object.
	 * The returned model is not necessarily a MIRROR data model. No object verification is applied.
	 * @return Model the data object claims to instantiate.
	 */
	@Override
	public DataModel getDataModel(){
		Namespace ns = element.getNamespace("xsi");
		Attribute dataAttribute = element.getAttribute("schemaLocation", ns);
		DataModel result = null;
		if (dataAttribute != null){
			String schemaLocation = dataAttribute.getValue();
			String[] string = schemaLocation.split(" ");
			result = new DataModel(string[0], string[1]);
		}
		return result;
	}
	
	private synchronized void writeObject(ObjectOutputStream s) throws IOException{
		s.writeObject(element);
		s.writeObject(cdmData);
	}
	
	private synchronized void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException{
		this.element = (Element) s.readObject();
		this.cdmData = (CDMData) s.readObject();
		this.elementName = element.getName();
	}

}
