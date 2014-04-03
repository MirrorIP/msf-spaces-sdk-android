package de.imc.mirror.sdk.android.data;

/**
 * This class provides a set of strings for creation, deletion and all columnnames of the database table
 * where all spaces information will be saved.
 * @author Mach
 */
public class SpacesTable {
	public static final String TABLE_NAME = "spaces_table";
	public static final String SPACE_ID = "space_id";
	public static final String SPACE_NAME = "space_name";
	public static final String SPACE_DOMAIN = "space_domain";
	public static final String SPACE_TYPE = "space_type";
	public static final String SPACE_PERSISTENTTYPE = "space_persistenttype";
	public static final String SPACE_PERSISTENTDURATION = "space_persistentduration";
	public static final String USER = "user";
	public static final String SQL_CREATE = 
			"CREATE TABLE " + TABLE_NAME + " (" +
			SPACE_ID + " STRING NOT NULL," +
			SPACE_NAME + " STRING NOT NULL," +
			SPACE_DOMAIN + " STRING NOT NULL, " +
			SPACE_TYPE + " STRING NOT NULL, " +
			SPACE_PERSISTENTTYPE + " STRING NOT NULL, " +
			SPACE_PERSISTENTDURATION + " STRING, " +
			USER + " STRING NOT NULL);";

	public static final String SQL_DROP = "DROP TABLE IF EXISTS " + TABLE_NAME + ";";
}
