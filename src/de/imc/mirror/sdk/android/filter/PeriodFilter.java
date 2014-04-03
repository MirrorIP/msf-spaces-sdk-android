package de.imc.mirror.sdk.android.filter;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.jdom2.Element;

import de.imc.mirror.sdk.DataObject;
import de.imc.mirror.sdk.android.utils.DatatypeConverter;

/**
 * Restricts the period in time the data object was published. 
 * @author simon.schwantzer(at)im-c.de
 */
public class PeriodFilter implements de.imc.mirror.sdk.filter.PeriodFilter {
	private static final DateFormat ISO8061_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

	private final Date from, to;
	
	/**
	 * Creates a period filter.
	 * @param from Specifies the earliest point in time the object was published.
	 * @param to Specifies the latest point in time the object was published.
	 */
	public PeriodFilter(Date from, Date to) {
		this.from = from;
		this.to = to;
	}
	
	@Override
	public Element getFilterAsXML(String queryNamespace) {
		Element element = new Element("period", queryNamespace);
		if (from != null) {
			StringBuffer dateTimeBuffer = new StringBuffer(ISO8061_FORMAT.format(from));
			// fix: timezone information is not encoded correctly (+0200 instead of +02:00)
			dateTimeBuffer.insert(dateTimeBuffer.length() - 2, ":");
			element.setAttribute("from", dateTimeBuffer.toString());
		}
		if (to != null) {
			// fix: timezone information is not encoded correctly (+0200 instead of +02:00)
			StringBuffer dateTimeBuffer = new StringBuffer(ISO8061_FORMAT.format(to));
			dateTimeBuffer.insert(dateTimeBuffer.length() - 2, ":");
			element.setAttribute("to", dateTimeBuffer.toString());
		}
		return element;
	}

	@Override
	public boolean isDataObjectValid(DataObject dataObject) {
		String timestampValue = dataObject.getElement().getAttributeValue("timestamp"); 
		if (timestampValue == null) {
			return false;
		}
		Date objectTimestamp;
		try {
			Calendar calendar = DatatypeConverter.parseDateTime(timestampValue);
			objectTimestamp = calendar.getTime();
		} catch (IllegalArgumentException e) {
			// Failed to parse timestamp
			return false;
		}
		if (from != null && objectTimestamp.before(from)) {
			return false;
		}
		if (to != null && objectTimestamp.after(to)) {
			return false;
		}
		return true;
	}

	@Override
	public Date getFrom() {
		return from;
	}

	@Override
	public Date getTo() {
		return to;
	}
	
	@Override
	public int hashCode() {
		int hc = 17;
	    int hashMultiplier = 59;
	    if (from != null) {
	    	hc = hc * hashMultiplier + from.hashCode();
	    } else {
	    	hc = hc * hashMultiplier + 1;
	    }
	    if (to != null) hc = hc * hashMultiplier + to.hashCode();
		return hc;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof PeriodFilter)) return false;

		PeriodFilter that = (PeriodFilter) obj;
		if (this.from != null) {
			if (!this.from.equals(that.from)) return false;
		} else {
			if (that.from != null) return false;
		}
		
		if (this.to != null) {
			if (!this.to.equals(that.to)) return false;
		} else {
			if (that.to != null) return false;
		}
		
		return true;
	}

}
