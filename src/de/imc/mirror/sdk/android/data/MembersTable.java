package de.imc.mirror.sdk.android.data;

/**
 * This class provides a set of strings for creation, deletion and all columnnames of the database table
 * where all member information of a space will be saved.
 * @author Mach
 */
public class MembersTable {
	public static final String TABLE_NAME = "members_table";
	public static final String SPACE = "space";
	public static final String ROLE = "role";
	public static final String BAREJID = "barejid";
	public static final String SQL_CREATE = 
			"CREATE TABLE " + TABLE_NAME + " (" +
			SPACE + " STRING NOT NULL," +
			ROLE + " STRING NOT NULL," +
			BAREJID + " STRING NOT NULL);";

	public static final String SQL_DROP = "DROP TABLE IF EXISTS " + TABLE_NAME + ";";

}
