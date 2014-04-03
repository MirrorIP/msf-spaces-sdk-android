package de.imc.mirror.sdk.android.cdm;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.jdom2.Element;

/**
 * Model for data object references.
 * @author simon.schwantzer(at)im-c.de
 */
public class References implements Serializable, de.imc.mirror.sdk.cdm.References {
	private static final long serialVersionUID = 2L;
	private transient List<Reference> references;
	
	/**
	 * Creates an references object.
	 * @param references List of references to include.
	 */
	public References(List<Reference> references) {
		this.references = references;
	}
	
	public References(Element referencesElement) {
		this.references = new ArrayList<Reference>();
		for (Element referenceElement : referencesElement.getChildren()) {
			references.add(new Reference(referenceElement));
		}
	}

	@Override
	public List<Reference> getReferences() {
		return references;
	}

	@Override
	public Element generateXMLElement(String namespaceURI) {
		Element referencesElement = new Element("references", namespaceURI);
		for (Reference reference : references) {
			referencesElement.addContent(reference.generateXMLElement(namespaceURI));
		}
		return referencesElement;
	}
	
	/**
	 * Manual serialization of list is required, otherwise deserialzation will cause SerialID missmatch. 
	 */
	private synchronized void writeObject(ObjectOutputStream s) throws IOException {
		s.writeInt(references.size());
		for (Reference reference : references) {
			s.writeObject(reference);
		}
	}
	
	/**
	 * Manual serialization enforces manual deserialization.
	 */
	private synchronized void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
		int size = s.readInt();
		references = new ArrayList<Reference>(size);
		for (int i = 0; i < size; i++) {
			references.add((Reference) s.readObject());
		}
	}
}
