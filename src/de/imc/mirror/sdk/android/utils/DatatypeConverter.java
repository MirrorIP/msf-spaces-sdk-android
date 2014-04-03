package de.imc.mirror.sdk.android.utils;

import java.util.Calendar;
import java.util.GregorianCalendar;

import javax.xml.datatype.DatatypeFactory;

import android.util.Log;

/**
 * Android implementations of the methods used from <code>javax.xml.bind.DatatypeConverter</code>.
 * @author simon.schwantzer(at)im-c.de
 */
public class DatatypeConverter {

	public static Calendar parseDateTime(String dateTimeString) throws IllegalArgumentException {
		try {
			DatatypeFactory factory = DatatypeFactory.newInstance();
			return factory.newXMLGregorianCalendar(dateTimeString).toGregorianCalendar();
		} catch (Exception e) {
			Log.w("DatatypeConverter", "Failed to parse dateTime string.", e);
			throw new IllegalArgumentException("Failed to parse dateTime string.", e);
		}
	}
	
	public static String printDateTime(Calendar dateTime) {
		try {
			DatatypeFactory factory = DatatypeFactory.newInstance();
			return factory.newXMLGregorianCalendar((GregorianCalendar) dateTime).toXMLFormat();
		} catch (Exception e) {
			Log.w("DatatypeConverter", "Failed to generate dateTime string.", e);
			throw new IllegalArgumentException("Failed to generate dateTime string.", e);
		}
	}
}
