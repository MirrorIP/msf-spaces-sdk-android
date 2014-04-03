package de.imc.mirror.sdk.android.utils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class DateToXsdDatetimeFormatter {
	private static final DateFormat ISO8061_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
	private static final DateFormat ISO8061_FORMAT_WITHOUT_TIMEZONE = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");

    public DateToXsdDatetimeFormatter () {}

    /**
     * Creates a new DateToXsdDatetimFormatter, which uses the given timezone.
     * @param timeZone The timezone to use.
     */
    public DateToXsdDatetimeFormatter (TimeZone timeZone)  {
    	ISO8061_FORMAT.setTimeZone(timeZone);
    	ISO8061_FORMAT_WITHOUT_TIMEZONE.setTimeZone(timeZone);
    }

    /**
     * Parses a xmldatestring to a dateobject. If no milliseconds are set, these will be set to 0.
     * If no timezone identifier is given, the set timezone will be used.
     * @param xmlDateTime The xmldatestring to be parsed.
     * @return The parsed date.
     * @throws ParseException Thrown if the given string cannot be parsed.
     */
    public synchronized Date parse(String xmlDateTime) throws ParseException  {
    	if (xmlDateTime.length() < 19){
    		throw new ParseException("The given date cannot be in the right format.", 0);
    	}
    	int milliSeconds = xmlDateTime.lastIndexOf(".");
    	int timeZoneIndex = xmlDateTime.lastIndexOf("+");
    	if (timeZoneIndex == -1) {
    		timeZoneIndex = xmlDateTime.lastIndexOf("-") < 10 ? -1 : xmlDateTime.lastIndexOf("-");
    	}
    	if (milliSeconds == 18 || timeZoneIndex == 18) {
    		throw new ParseException("The given date cannot be in the right format.", 0);
    	}
    	if (milliSeconds == -1){
            StringBuilder sb = new StringBuilder(xmlDateTime);
            sb.insert(19, ".0");
            xmlDateTime = sb.toString();
    	}
    	int colonIndex = xmlDateTime.lastIndexOf(":");
    	if (colonIndex <= 16) {
            return ISO8061_FORMAT_WITHOUT_TIMEZONE.parse(xmlDateTime);
    	}
        StringBuilder sb = new StringBuilder(xmlDateTime);
        sb.deleteCharAt(colonIndex);
        return ISO8061_FORMAT.parse(sb.toString());
    }

    /**
     * Formats a date to a ISO8061 formatted string.
     * @param xmlDateTime The date to format.
     * @return The formatted string.
     */
    public synchronized String format(Date xmlDateTime) {
        String s =  ISO8061_FORMAT.format(xmlDateTime);
        StringBuilder sb = new StringBuilder(s);
        sb.insert(26, ':');
        return sb.toString();
    }

    /**
     * Sets a new timezone used to format a date.
     * @param timezone The timezone to use.
     */
    public synchronized void setTimeZone(String timezone)  {
    	ISO8061_FORMAT.setTimeZone(TimeZone.getTimeZone(timezone));
    	ISO8061_FORMAT_WITHOUT_TIMEZONE.setTimeZone(TimeZone.getTimeZone(timezone));
    }
}
