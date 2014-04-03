package de.imc.mirror.sdk.android.packet;

import org.jdom2.Element;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.xmlpull.v1.XmlPullParser;


public class PersistenceServiceQueryProvider implements IQProvider {
	@Override
	public IQ parseIQ(XmlPullParser parser) throws Exception {
		String rootTagName = parser.getName();
		String namespace = parser.getNamespace();
		Element childElement = new Element(rootTagName, namespace);
		boolean done = false;
		while (!done) {
			int event = parser.next();
			switch (event) {
			case XmlPullParser.START_TAG:
				childElement.addContent(parseArbitraryElement(parser));
				break;
			case XmlPullParser.END_TAG:
				if ("query".equals(parser.getName())) {
					done = true;
				}
				break;
			}
		}
		IQ iq = new QueryResponseIQ(childElement);
		return iq;
	}
	
	private Element parseArbitraryElement(XmlPullParser parser) throws Exception {
		String elementName = parser.getName();
		String namespace = parser.getNamespace();
		Element element = new Element(elementName, namespace);
		for (int i = 0; i < parser.getAttributeCount(); i++) {
			element.setAttribute(parser.getAttributeName(i), parser.getAttributeValue(i));
		}
		
		boolean done = false;
		while (!done) {
			int event = parser.next();
			switch (event) {
			case XmlPullParser.START_TAG:
				element.addContent(parseArbitraryElement(parser));
				break;
			case XmlPullParser.TEXT:
				element.setText(parser.getText());
				break;
			case XmlPullParser.END_TAG:
				if (parser.getName().equals(elementName)) {
					done = true;
				}
			}
		}
		return element;
	}

}
