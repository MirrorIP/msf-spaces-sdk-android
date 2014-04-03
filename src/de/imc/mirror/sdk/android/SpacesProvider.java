package de.imc.mirror.sdk.android;

import org.jdom2.Element;
import org.jdom2.output.XMLOutputter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.xmlpull.v1.XmlPullParser;


/**
 * Provider for IQ packages starting with a <code>spaces</code> tag.
 * Used internally by the spaces manager.
 *
 */
public class SpacesProvider implements IQProvider {
	
	public class SpacesIQ extends IQ {
		private Element element;
		public SpacesIQ(Element element) {
			this.element = element;
		}
		
		@Override
		public String getChildElementXML() {
			XMLOutputter out = new XMLOutputter();
			return out.outputString(element);
		}
	}

	@Override
	public IQ parseIQ(XmlPullParser parser) throws Exception {
		String rootTagName = parser.getName();
		String namespace = parser.getNamespace();
		Element spacesElement = new Element(rootTagName, namespace); 
		boolean done = false;
		while (!done) {
			int event = parser.next();
			switch (event) {
			case XmlPullParser.START_TAG:
				String tagName = parser.getName();
				CommandType command = CommandType.getTypeForTagName(tagName);
				switch (command) {
				case CREATE:
					parseCreateTag(parser, spacesElement);
					break;
				case CHANNELS:
					parseChannelsTag(parser, spacesElement);
					break;
				case MODELS:
					parseModelsTag(parser, spacesElement);
					break;
				case VERSION:
					parseVersion(parser, spacesElement);
					break;
				case CONFIGURE:
				case DELETE:
				default:
					Element errorElement = new Element("error");
					errorElement.setText("Unsupported command: " + tagName);
					spacesElement.setContent(errorElement);
				}
				break;
			case XmlPullParser.END_TAG:
				if ("spaces".equals(parser.getName())) {
					done = true;
				}
				break;
			}
		}
		return new SpacesIQ(spacesElement);
	}
	
	/**
	 * Parses a createtag.
	 * @param parser The used XmlPullParser.
	 * @param parent The parent of this tag.
	 */
	private void parseCreateTag(XmlPullParser parser, Element parent) {
		Element createElement = new Element(parser.getName(), parent.getNamespace());
		parent.addContent(createElement);
		for (int i = 0; i < parser.getAttributeCount(); i++) {
			createElement.setAttribute(parser.getAttributeName(i), parser.getAttributeValue(i));
		}
	}
	
	/**
	 * Parses the version tag.
	 * @param parser The used XmlPullParser.
	 * @param parent The parent of this tag.
	 */
	private void parseVersion(XmlPullParser parser, Element parent) throws Exception {
		Element versionElement = new Element(parser.getName(), parent.getNamespace());
		parent.addContent(versionElement);
		for (int i = 0; i < parser.getAttributeCount(); i++) {
			versionElement.setAttribute(parser.getAttributeName(i), parser.getAttributeValue(i));
		}
		int event = parser.next();
		if (event == XmlPullParser.TEXT) {
			versionElement.setText(parser.getText());
		}
	}
	
	/**
	 * Parses a channelstag.
	 * @param parser The used XmlPullParser.
	 * @param parent The parent of this tag.
	 */
	private void parseChannelsTag(XmlPullParser parser, Element parent) throws Exception {
		String elementName = parser.getName();
		Element channelsElement = new Element(elementName, parent.getNamespace());
		parent.addContent(channelsElement);
		for (int i = 0; i < parser.getAttributeCount(); i++) {
			channelsElement.setAttribute(parser.getAttributeName(i), parser.getAttributeValue(i));
		}
		
		boolean done = false;
		while (!done) {
			int event = parser.next();
			switch (event) {
			case XmlPullParser.START_TAG:
				String tagName = parser.getName();
				if ("channel".equals(tagName)) {
					parseChannelTag(parser, channelsElement);
				} else {
					throw new RuntimeException("Illegal response: <channels /> element may not contain: " + tagName);
				}
				break;
			case XmlPullParser.END_TAG:
				if (parser.getName().equals(elementName)) {
					done = true;
				}
			}
		}
	}
	
	/**
	 * Parses a channeltag.
	 * @param parser The used XmlPullParser.
	 * @param parent The parent of this tag.
	 */
	private void parseChannelTag(XmlPullParser parser, Element parent) throws Exception {
		String elementName = parser.getName();
		Element channelElement = new Element(elementName, parent.getNamespace());
		parent.addContent(channelElement);
		for (int i = 0; i < parser.getAttributeCount(); i++) {
			channelElement.setAttribute(parser.getAttributeName(i), parser.getAttributeValue(i));
		}
		boolean done = false;
		Element propertyElement = null;
		while (!done) {
			int event = parser.next();
			switch (event) {
			case XmlPullParser.START_TAG:
				String tagName = parser.getName();
				if ("property".equals(tagName)) {
					propertyElement = new Element(tagName, parent.getNamespace());
					channelElement.addContent(propertyElement);
					for (int i = 0; i < parser.getAttributeCount(); i++) {
						propertyElement.setAttribute(parser.getAttributeName(i), parser.getAttributeValue(i));
					}
				} else {
					throw new RuntimeException("Illegal response: <channel /> element may not contain: " + tagName);
				}
				break;
			case XmlPullParser.TEXT:
				String text = parser.getText();
				if (propertyElement != null) {
					propertyElement.setText(text);
				} else {
					throw new RuntimeException("Illegal response: Invalid node content: " + text);
				}
				
			case XmlPullParser.END_TAG:
				tagName = parser.getName();
				if (elementName.equals(tagName)) {
					done = true;
				}
			}
		}		
	}
	
	/**
	 * Parses a modelstag.
	 * @param parser The used XmlPullParser.
	 * @param parent The parent of this tag.
	 */
	private void parseModelsTag(XmlPullParser parser, Element parent) throws Exception {
		String elementName = parser.getName();
		Element modelsElement = new Element(elementName, parent.getNamespace());
		parent.addContent(modelsElement);
		for (int i = 0; i < parser.getAttributeCount(); i++) {
			modelsElement.setAttribute(parser.getAttributeName(i), parser.getAttributeValue(i));
		}
		boolean done = false;
		Element modelElement = null;
		while (!done) {
			int event = parser.next();
			switch (event) {
			case XmlPullParser.START_TAG:
				String tagName = parser.getName();
				if ("model".equals(tagName)) {
					modelElement = new Element(tagName, parent.getNamespace());
					modelsElement.addContent(modelElement);
					for (int i = 0; i < parser.getAttributeCount(); i++) {
						modelElement.setAttribute(parser.getAttributeName(i), parser.getAttributeValue(i));
					}
				} else {
					throw new RuntimeException("Illegal response: <channel /> element may not contain: " + tagName);
				}
				break;
			case XmlPullParser.TEXT:
				String text = parser.getText();
				if (modelElement != null) {
					modelElement.setText(text);
				} else {
					throw new RuntimeException("Illegal response: Invalid node content: " + text);
				}
				
			case XmlPullParser.END_TAG:
				tagName = parser.getName();
				if (elementName.equals(tagName)) {
					done = true;
				}
			}
		}	
	}
}