package de.imc.mirror.sdk.android.data;

/**
 * This class provides a set of strings for creation, deletion and all columnnames of the database table
 * where all received data will be saved.
 * @author Mach
 */
public class DataTable {
	public static final String TABLE_NAME = "data_table";
	public static final String DATA_ID = "send_id";
	public static final String DATA_NODE = "send_node";
	public static final String DATA_NAME = "send_name";
	public static final String DATA_NAMESPACE = "send_namespace";
	public static final String DATA_PAYLOAD = "payload";
	public static final String SQL_CREATE = "CREATE TABLE " + TABLE_NAME + " (" +
			DATA_ID + " STRING NOT NULL," +
			DATA_NODE + " STRING NOT NULL," +
			DATA_NAME + " STRING NOT NULL," +
			DATA_NAMESPACE + " STRING NOT NULL," + 
			DATA_PAYLOAD + " STRING NOT NULL);";

	public static final String SQL_DROP = "DROP TABLE IF EXISTS " + TABLE_NAME + ";";

}
