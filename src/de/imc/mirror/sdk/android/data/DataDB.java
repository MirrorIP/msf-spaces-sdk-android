package de.imc.mirror.sdk.android.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DataDB extends SQLiteOpenHelper {
	
	private static final int DB_VERSION = 4;
	
	public DataDB(Context context, String dbName){
		super(context, dbName, null, DB_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(SpacesTable.SQL_CREATE);
		db.execSQL(SendTable.SQL_CREATE);
		db.execSQL(DataTable.SQL_CREATE);
		db.execSQL(ChannelsTable.SQL_CREATE);
		db.execSQL(MembersTable.SQL_CREATE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL(SpacesTable.SQL_DROP);
		db.execSQL(SendTable.SQL_DROP);
		db.execSQL(DataTable.SQL_DROP);
		db.execSQL(MembersTable.SQL_DROP);
		db.execSQL(ChannelsTable.SQL_DROP);
		onCreate(db);
	}

}
