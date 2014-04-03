package de.imc.mirror.sdk.android.cdm;

import java.io.Serializable;

import org.jdom2.Element;

/**
 * Model for a data object summary as specified in CDM 2.0.
 * @author simon.schwantzer(at)im-c.de
 */
public class Summary implements Serializable, de.imc.mirror.sdk.cdm.Summary {
	private static final long serialVersionUID = 1L;
	private final String summary;
	
	/**
	 * Creates a summary.
	 * @param summary Text for the summary.
	 */
	public Summary(String summary) {
		this.summary = summary;
	}
	
	/**
	 * Creates a summary object based on a related CDM element.
	 * @param element XML element containing the summary.
	 */
	public Summary(Element element) {
		summary = element.getText();
	}

	@Override
	public String getSummary() {
		return summary;
	}

	@Override
	public Element generateXMLElement(String namespaceURI) {
		Element summaryElement = new Element("summary", namespaceURI);
		summaryElement.setText(summary);
		return summaryElement;
	}
}
