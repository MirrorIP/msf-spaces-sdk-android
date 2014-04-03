package de.imc.mirror.sdk.android;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jivesoftware.smackx.pubsub.SimplePayload;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;
import de.imc.mirror.sdk.DataModel;
import de.imc.mirror.sdk.DataObject;
import de.imc.mirror.sdk.Space;
import de.imc.mirror.sdk.SpaceChannel;
import de.imc.mirror.sdk.SpaceMember;
import de.imc.mirror.sdk.SpaceMember.Role;
import de.imc.mirror.sdk.android.data.ChannelsTable;
import de.imc.mirror.sdk.android.data.DataDB;
import de.imc.mirror.sdk.android.data.DataTable;
import de.imc.mirror.sdk.android.data.MembersTable;
import de.imc.mirror.sdk.android.data.SendTable;
import de.imc.mirror.sdk.android.data.SpacesTable;

/**
 * Wrapperclass to access the Caches used by the Handlers.
 * @author mach
 *
 */
public class DataWrapper {

	private DataDB scheme;
	private static DataWrapper instance;
	
	/**
	 * Create a new DataWrapper.
	 * @param context The current appcontext.
	 * @param dbName The name of the database.
	 */
	private DataWrapper(Context context, String dbName) {
		scheme = new DataDB(context, dbName);
	}
	
	/**
	 * Gets a DataWrapper instance. If no instance exists a new one is created.
	 * @param context The current Appcontext. Only used when a new instance has to be created.
	 * @param dbName The name of the database. Only used when a new instance has to be created.
	 * @return A datawrapper instance.
	 */
	protected static DataWrapper getInstance(Context context, String dbName){
		if (instance == null){
			instance = new DataWrapper(context, dbName);
		}
		return instance;
	}
	
	/**
	 * Gets a DataWrapper instance.
	 * @return A datawrapper instance or <code>null</code> if no instance exists.
	 */
	protected static DataWrapper getInstance(){
		return instance;
	}
	
	/**
	 * Deletes all cached spaces for the user.
	 * @param user The user to delete the spaces for.
	 */
	protected synchronized void deleteCachedSpacesForUser(String user){
		List<String> spaceIds = getSpaceIdsForUser(user);
		if (spaceIds.size() > 0){
			List<String> spaceIdsOfOthers = getSpacesAlsoSavedForOthers(spaceIds, user);
			spaceIds.removeAll(spaceIdsOfOthers);
		}
		SQLiteDatabase db = scheme.getWritableDatabase();
		try {
			db.beginTransaction();
			db.execSQL("DELETE FROM " + SpacesTable.TABLE_NAME + " WHERE " + SpacesTable.USER + " ='" + user + "';");
			for (String spaceId:spaceIds){
				db.execSQL("DELETE FROM " + ChannelsTable.TABLE_NAME + " WHERE " + ChannelsTable.SPACE + " ='" + spaceId + "';");
				db.execSQL("DELETE FROM " + MembersTable.TABLE_NAME + " WHERE " + MembersTable.SPACE + " ='" + spaceId + "';");
			}
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
			db.close();
		}
	}
	
	/**
	 * Deletes all cached Information of a space.
	 * @param spaceId The id of the space.
	 */
	protected synchronized void deleteCachedSpace(String spaceId){
		SQLiteDatabase db = scheme.getWritableDatabase();
		try {
			db.beginTransaction();
			db.execSQL("DELETE FROM " + SpacesTable.TABLE_NAME + " WHERE " + SpacesTable.SPACE_ID + " ='" + spaceId + "';");
			db.execSQL("DELETE FROM " + ChannelsTable.TABLE_NAME + " WHERE " + ChannelsTable.SPACE + " ='" + spaceId + "';");
			db.execSQL("DELETE FROM " + MembersTable.TABLE_NAME + " WHERE " + MembersTable.SPACE + " ='" + spaceId + "';");
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
			db.close();
		}
	}
	
	/**
	 * Gets all ids of spaces which are also cached for other users.
	 * @param spaceIds The spaceIds to check.
	 * @param user The user to exclude from the search.
	 * @return A list of all ids of the spaces which are also cached for other users.
	 */
	private synchronized List<String> getSpacesAlsoSavedForOthers(List<String> spaceIds, String user){
		SQLiteDatabase db = scheme.getReadableDatabase();
		StringBuilder builder = new StringBuilder();
		for (int i=0; i<spaceIds.size();i++){
			String spaceId = spaceIds.get(i);
			builder.append(SpacesTable.SPACE_ID + "='");
			builder.append(spaceId);
			builder.append("' ");
			if (i != spaceIds.size()-1){
				builder.append("OR ");
			}
		}
		String query = "SELECT " + SpacesTable.SPACE_ID + " FROM " + SpacesTable.TABLE_NAME 
						+ " WHERE NOT(" + SpacesTable.USER + "='" + user + "') AND (" + 
						builder.toString() + ");";
		Cursor c = db.rawQuery(query, null);
		List<String> result = new ArrayList<String>();
		while(c.moveToNext()){
			String spaceId = c.getString(c.getColumnIndex(SpacesTable.SPACE_ID));
			result.add(spaceId);
		}
		c.close();
		db.close();
		return result;
	}
	
	/**
	 * Gets all ids of the spaces which are cached for the given user.
	 * @param user The user to get the spaceIds for.
	 * @return A list of spaceIds.
	 */
	private synchronized List<String> getSpaceIdsForUser(String user){
		SQLiteDatabase db = scheme.getReadableDatabase();
		String query = "SELECT " + SpacesTable.SPACE_ID + " FROM " + SpacesTable.TABLE_NAME 
						+ " WHERE " + SpacesTable.USER + "='" + user + "';";
		Cursor c = db.rawQuery(query, null);
		List<String> spaceIds = new ArrayList<String>();
		while (c.moveToNext()){
			String spaceId = c.getString(c.getColumnIndex(SpacesTable.SPACE_ID));
			spaceIds.add(spaceId);
		}
		c.close();
		db.close();
		return spaceIds;
	}
	
	/**
	 * Saves a space and all infos of it. The id, name, pubsubnode, jid 
	 * and pubsub service for the node mustn't be null.
	 * @param space The space to cache.
	 * @param user The user to cache the space for.
	 */
	protected synchronized void saveSpace(Space space, String user){
		SQLiteDatabase db = scheme.getWritableDatabase();
		db.beginTransaction();
		try{
			ContentValues values = new ContentValues();
			values.put(SpacesTable.SPACE_ID, space.getId());
			values.put(SpacesTable.SPACE_NAME, space.getName());
			values.put(SpacesTable.SPACE_DOMAIN, space.getDomain());
			values.put(SpacesTable.SPACE_TYPE, space.getType().toString());
			values.put(SpacesTable.SPACE_PERSISTENTTYPE, space.getPersistenceType().name());
			if (space.getPersistenceDuration() != null) {
				values.put(SpacesTable.SPACE_PERSISTENTDURATION, space.getPersistenceDuration().toString());
			} else {
				values.putNull(SpacesTable.SPACE_PERSISTENTDURATION);
			}
			values.put(SpacesTable.USER, user);
			db.insert(SpacesTable.TABLE_NAME, null, values);
			Set<SpaceChannel> channels = space.getChannels();
			for (SpaceChannel channel:channels){
				for (String property:channel.getProperties().keySet()){
					values = new ContentValues();
					values.put(ChannelsTable.TYPE, channel.getType());
					values.put(ChannelsTable.SPACE, space.getId());
					values.put(ChannelsTable.KEY, property);
					values.put(ChannelsTable.VALUE, channel.getProperties().get(property));
					db.insert(ChannelsTable.TABLE_NAME, null, values);
				}
			}
			for (SpaceMember member:space.getMembers()){
				values = new ContentValues();
				values.put(MembersTable.ROLE, member.getRole().name());
				values.put(MembersTable.SPACE, space.getId());
				values.put(MembersTable.BAREJID, member.getJID());
				db.insert(MembersTable.TABLE_NAME, null, values);
			}
			db.setTransactionSuccessful();
		}catch (Exception e){
			Log.d("DataWrapper", "An Exception was thrown while saving a space", e);
		}finally{
			db.endTransaction();
			db.close();
		}	
	}
	
	/**
	 * Gets all spaces that are cached for the user.
	 * @param user The user to get the spaces for.
	 * @return A list of spaces.
	 */
	protected synchronized List<Space> getCachedSpacesForUser(String user){
		List<Space> spaces = new ArrayList<Space>();
		SQLiteDatabase db = scheme.getReadableDatabase();
		String query = "Select * FROM " + SpacesTable.TABLE_NAME + " WHERE " +
								SpacesTable.USER + "='" + user + "';";
		Cursor c = db.rawQuery(query, null);
		while(c.moveToNext()){
			String spaceId = c.getString(c.getColumnIndex(SpacesTable.SPACE_ID));
			String membersQuery = "SELECT * FROM " + MembersTable.TABLE_NAME + " WHERE " +
				MembersTable.SPACE + "='" + spaceId + "';";
			Cursor membersCursor = db.rawQuery(membersQuery, null);
			Set<SpaceMember> members = new HashSet<SpaceMember>();
			while (membersCursor.moveToNext()){
				int jidColumn = membersCursor.getColumnIndex(MembersTable.BAREJID);
				int roleColumn = membersCursor.getColumnIndex(MembersTable.ROLE);
				members.add(new de.imc.mirror.sdk.android.SpaceMember(membersCursor.getString(jidColumn),
						Role.valueOf(membersCursor.getString(roleColumn))));
			}
			membersCursor.close();
			String channelsTypeQuery = "SELECT DISTINCT " + ChannelsTable.TYPE + " FROM " + ChannelsTable.TABLE_NAME + " WHERE " +
							ChannelsTable.SPACE + "='" + spaceId + "';";
			Cursor channelsTypeCursor = db.rawQuery(channelsTypeQuery, null);
			Set<SpaceChannel> channels = new HashSet<SpaceChannel>();
			while (channelsTypeCursor.moveToNext()){
				String type = channelsTypeCursor.getString(channelsTypeCursor.getColumnIndex(ChannelsTable.TYPE));
				String channelsQuery = "SELECT * FROM " + ChannelsTable.TABLE_NAME + " WHERE " +
					ChannelsTable.SPACE + "='" + spaceId + "' AND " + ChannelsTable.TYPE + "='" + type + "';";
				Cursor channelsCursor = db.rawQuery(channelsQuery, null);
				Map<String, String> properties = new HashMap<String,String>();
				while (channelsCursor.moveToNext()){
					int keyColumn = channelsCursor.getColumnIndex(ChannelsTable.KEY);
					int valueColumn = channelsCursor.getColumnIndex(ChannelsTable.VALUE);
					properties.put(channelsCursor.getString(keyColumn), 
							channelsCursor.getString(valueColumn));
				}
				channelsCursor.close();
				channels.add(new de.imc.mirror.sdk.android.SpaceChannel(type, properties));
			}
			channelsTypeCursor.close();
			String name = c.getString(c.getColumnIndex(SpacesTable.SPACE_NAME));
			String domain = c.getString(c.getColumnIndex(SpacesTable.SPACE_DOMAIN));
			Space.Type type = Space.Type.getType(c.getString(c.getColumnIndex(SpacesTable.SPACE_TYPE)));
			Space.PersistenceType persistenceType = Space.PersistenceType.valueOf(c.getString(c.getColumnIndex(SpacesTable.SPACE_PERSISTENTTYPE)));
			Duration duration;
			try {
				duration = DatatypeFactory.newInstance().newDuration(c.getString(c.getColumnIndex(SpacesTable.SPACE_PERSISTENTDURATION)));
			} catch (Exception e) {
				persistenceType = Space.PersistenceType.OFF;
				duration = null;
			}
			spaces.add(de.imc.mirror.sdk.android.Space.createSpace(name, spaceId, domain, null, type, channels, members, persistenceType, duration));
		}
		c.close();
		db.close();
		return spaces;
	}
	
	/**
	 * Saves a payload which can't be send.
	 * @param user The user to send the payload.
	 * @param payloadId The id of the payload which should be send.
	 * @param spaceId The id of the node to send the payload to.
	 * @param payload The payload to send.
	 */
	protected synchronized void savePayloadToSend(String user, String id, String spaceId, SimplePayload payload){
		SQLiteDatabase db = scheme.getWritableDatabase();
		String query = "INSERT INTO " + SendTable.TABLE_NAME + "(" + SendTable.SEND_ID+ ", " + 
						SendTable.SEND_NAME + ", " + SendTable.SEND_NAMESPACE + ", " + SendTable.SEND_SPACE + 
						", " + SendTable.SEND_PAYLOAD + ", " + SpacesTable.USER + ") VALUES(?,?,?,?,?,?);";
		SQLiteStatement stmt = db.compileStatement(query);
		stmt.bindString(1, id);
		stmt.bindString(2, payload.getElementName());
		stmt.bindString(3, payload.getNamespace());
		stmt.bindString(4, spaceId);
		stmt.bindString(5, payload.toXML());
		stmt.bindString(6, user);
		try{
			stmt.executeInsert();
		}catch (Exception e){
			Log.d("DataWrapper", "An Exception was thrown while saving a payload to send", e);
		}finally{
			stmt.close();
			db.close();
		}	
	}
	
	/**
	 * Gets all payloads to send for the given user.
	 * @param user The user to get the payloads for.
	 * @return A map consisting of the payloadIds and the corresponding simplepayloads.
	 */
	protected synchronized Map<String, SimplePayload> getPayloadsToSend(String user){
		SQLiteDatabase db = scheme.getReadableDatabase();
		String query = "Select * FROM " + SendTable.TABLE_NAME + " WHERE " + SendTable.USER + "='" + user + "';";
		Cursor c = db.rawQuery(query, null);
		Map<String, SimplePayload> payloads = new HashMap<String, SimplePayload>();
		while(c.moveToNext()){
			SimplePayload payload = new SimplePayload(c.getString(c.getColumnIndex(SendTable.SEND_NAME)),
											c.getString(c.getColumnIndex(SendTable.SEND_NAMESPACE)),
											c.getString(c.getColumnIndex(SendTable.SEND_PAYLOAD)));
			payloads.put(c.getString(c.getColumnIndex(SendTable.SEND_ID)), payload);
		}
		c.close();
		db.close();
		return payloads;
	}
	
	/**
	 * Gets the space to send a payload to.
	 * @param id The id of the payload to get the node for.
	 * @return The id of the space.
	 */
	protected synchronized String getSpaceForPayload(String id){
		SQLiteDatabase db = scheme.getReadableDatabase();
		String query = "Select " + SendTable.SEND_SPACE + " FROM " + SendTable.TABLE_NAME + " WHERE " +
								SendTable.SEND_ID + "='" + id + "';";
		Cursor c = db.rawQuery(query, null);
		if (c.moveToFirst()){
			String result = c.getString(c.getColumnIndex(SendTable.SEND_SPACE));
			c.close();
			db.close();
			return result;
		}
		c.close();
		db.close();
		return null;
	}
	
	/**
	 * Deletes all entries of the sendcache for an user.
	 * @param user The user to delete entries for.
	 */
	protected synchronized void clearSendCache(String user){
		SQLiteDatabase db = scheme.getWritableDatabase();
		db.execSQL("DELETE FROM " + SendTable.TABLE_NAME + " WHERE " + SendTable.USER + " ='" + user +"';");
		db.close();
	}
	
	/**
	 * Checks if an item was already cached.
	 * @param id The id of the item to check.
	 * @return If the item was already cached.
	 */
	protected synchronized boolean isDataObjectAlreadyCached(String id){
		SQLiteDatabase db = scheme.getReadableDatabase();
		String query = "Select * FROM " + DataTable.TABLE_NAME + " WHERE " +
						DataTable.DATA_ID + "='" + id + "';";
		Cursor c = db.rawQuery(query, null);
		if (c.getCount()>0){
			c.close();
			db.close();
			return true;
		}
		c.close();
		db.close();
		return false;
	}
	
	/**
	 * Saves a item.
	 * @param nodeId The id of the node the item is from.
	 * @param obj The dataobject to save.
	 * @param id The itemid of the item the dataobject was received from.
	 */
	protected synchronized void saveDataObject(String nodeId, DataObject obj, String id){
		SQLiteDatabase db = scheme.getWritableDatabase();
		String query = "INSERT INTO " + DataTable.TABLE_NAME + "(" + DataTable.DATA_ID+ ", " + 
						DataTable.DATA_NAME + ", " + DataTable.DATA_NAMESPACE + ", " + DataTable.DATA_NODE + 
						", " + DataTable.DATA_PAYLOAD + ") VALUES(?,?,?,?,?);";
		SQLiteStatement stmt = db.compileStatement(query);
		stmt.bindString(1, id);
		stmt.bindString(2, obj.getElement().getName());
		stmt.bindString(3, obj.getNamespaceURI());
		stmt.bindString(4, nodeId);
		stmt.bindString(5, obj.toString());
		try{
			stmt.executeInsert();
		}catch (Exception e){
			Log.d("DataWrapper", "An Exception was thrown while saving an item", e);
		}finally{
			stmt.close();
			db.close();
		}
	}
	
	/**
	 * Gets all cached items.
	 * @param nodeId The id of the node to get the items for.
	 * @return A list of all cached items.
	 */
	protected synchronized List<DataObject>  getCachedDataObjects(String nodeId){
		SQLiteDatabase db = scheme.getReadableDatabase();
		String query = "Select * FROM " + DataTable.TABLE_NAME + " WHERE " +
								DataTable.DATA_NODE + "='" + nodeId + "';";
		Cursor c = db.rawQuery(query, null);
		List<DataObject> objs = new ArrayList<DataObject>();
		while(c.moveToNext()){
			String namespace = c.getString(c.getColumnIndex(DataTable.DATA_NAMESPACE));
			String payload = c.getString(c.getColumnIndex(DataTable.DATA_PAYLOAD));
			
			SAXBuilder reader = new SAXBuilder();
			StringReader in = new StringReader(payload);
			Document document = null;
			try {
			document = reader.build(in);
			} catch (JDOMException e) {
				Log.d("DataWrapper", "An JDOMException was thrown while parsing a newly gotten item.", e);
			} catch (IOException e) {
				Log.d("DataWrapper", "An IOException was thrown while parsing a newly gotten item.", e);
			}
			if (document == null){
			}
			Element elem = document.getRootElement();
			DataObject obj = new DataObjectBuilder(elem, namespace).build();
			objs.add(obj);
		}
		c.close();
		db.close();
		return objs;
	}
	
	/**
	 * Saves all given spaces in the local cache for the given user.
	 * @param spaces The spaces to save.
	 * @param user The user to save the spaces for.
	 */
	protected synchronized void saveSpaces(List<Space> spaces, String user){
		SQLiteDatabase db = scheme.getWritableDatabase();
		db.beginTransaction();
		try{
			for (Space space:spaces){
				ContentValues values = new ContentValues();
				values.put(SpacesTable.SPACE_ID, space.getId());
				values.put(SpacesTable.SPACE_NAME, space.getName());
				values.put(SpacesTable.SPACE_DOMAIN, space.getDomain());
				values.put(SpacesTable.SPACE_TYPE, space.getType().toString());
				values.put(SpacesTable.SPACE_PERSISTENTTYPE, space.getPersistenceType().name());if (space.getPersistenceDuration() != null) {
					values.put(SpacesTable.SPACE_PERSISTENTDURATION, space.getPersistenceDuration().toString());
				} else {
					values.putNull(SpacesTable.SPACE_PERSISTENTDURATION);
				}
				values.put(SpacesTable.USER, user);
				db.insert(SpacesTable.TABLE_NAME, null, values);
				Set<SpaceChannel> channels = space.getChannels();
				for (SpaceChannel channel:channels){
					for (String property:channel.getProperties().keySet()){
						values = new ContentValues();
						values.put(ChannelsTable.TYPE, channel.getType());
						values.put(ChannelsTable.SPACE, space.getId());
						values.put(ChannelsTable.KEY, property);
						values.put(ChannelsTable.VALUE, channel.getProperties().get(property));
						db.insert(ChannelsTable.TABLE_NAME, null, values);
					}
				}
				for (SpaceMember member:space.getMembers()){
					values = new ContentValues();
					values.put(MembersTable.ROLE, member.getRole().name());
					values.put(MembersTable.SPACE, space.getId());
					values.put(MembersTable.BAREJID, member.getJID());
					db.insert(MembersTable.TABLE_NAME, null, values);
				}
			}
			db.setTransactionSuccessful();
		}catch (Exception e){
			Log.d("DataWrapper", "An Exception was thrown while saving spaces", e);
		}finally{
			db.endTransaction();
			db.close();
		}
	}
	
	/**
	 * Updates the cached information for a space.
	 * @param space The space to update the information for.
	 */
	protected synchronized void updateCachedSpaceInformation(Space space){
		String spaceId = space.getId();
		SQLiteDatabase db = scheme.getWritableDatabase();
		try {
			db.beginTransaction();
			db.execSQL("UPDATE " + SpacesTable.TABLE_NAME + " SET " + 
					SpacesTable.SPACE_DOMAIN + "='" + space.getDomain()+ "', " +
					SpacesTable.SPACE_NAME + "='" + space.getName()+ "', " +
					SpacesTable.SPACE_PERSISTENTTYPE + "='" + space.getPersistenceType().name() + "', " +
					SpacesTable.SPACE_PERSISTENTDURATION + "='" + (space.getPersistenceDuration() == null? "null":space.getPersistenceDuration().toString()) + "', " +
					SpacesTable.SPACE_TYPE + "='" + space.getType().name()+ "' " +
					" WHERE " + SpacesTable.SPACE_ID + " ='" + spaceId + "';");
			db.execSQL("DELETE FROM " + ChannelsTable.TABLE_NAME + " WHERE " + ChannelsTable.SPACE + " ='" + spaceId + "';");
			db.execSQL("DELETE FROM " + MembersTable.TABLE_NAME + " WHERE " + MembersTable.SPACE + " ='" + spaceId + "';");
			Set<SpaceChannel> channels = space.getChannels();
			ContentValues values;
			for (SpaceChannel channel:channels){
				for (String property:channel.getProperties().keySet()){
					values = new ContentValues();
					values.put(ChannelsTable.TYPE, channel.getType());
					values.put(ChannelsTable.SPACE, space.getId());
					values.put(ChannelsTable.KEY, property);
					values.put(ChannelsTable.VALUE, channel.getProperties().get(property));
					db.insert(ChannelsTable.TABLE_NAME, null, values);
				}
			}
			for (SpaceMember member:space.getMembers()){
				values = new ContentValues();
				values.put(MembersTable.ROLE, member.getRole().name());
				values.put(MembersTable.SPACE, space.getId());
				values.put(MembersTable.BAREJID, member.getJID());
				db.insert(MembersTable.TABLE_NAME, null, values);
			}
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
			db.close();
		}
	}
	
	/**
	 * Checks if a space with the given id is already cached.
	 * @param spaceId The spaceid to look for.
	 * @return If a entry was found or not.
	 */
	protected synchronized boolean isSpaceAlreadyCached(String spaceId){
		SQLiteDatabase db = scheme.getReadableDatabase();
		String query = "Select * FROM " + SpacesTable.TABLE_NAME + " WHERE " +
						SpacesTable.SPACE_ID + "='" + spaceId + "';";
		Cursor c = db.rawQuery(query, null);
		if (c.getCount()>0){
			c.close();
			db.close();
			return true;
		}
		c.close();
		db.close();
		return false;
	}
	
	/**
	 * Deletes all sent, received and to-be-send data currently saved.
	 */
	protected synchronized void clearDataCache(){
		SQLiteDatabase db = scheme.getWritableDatabase();
		db.execSQL("DELETE FROM " + SendTable.TABLE_NAME);
		db.execSQL("DELETE FROM " + DataTable.TABLE_NAME);
		db.close();
	}
	
	/**
	 * Deletes all saved spaces-information.
	 */
	protected synchronized void clearSpacesCache(){
		SQLiteDatabase db = scheme.getWritableDatabase();
		db.execSQL("DELETE FROM " + SpacesTable.TABLE_NAME);
		db.execSQL("DELETE FROM " + ChannelsTable.TABLE_NAME);
		db.execSQL("DELETE FROM " + MembersTable.TABLE_NAME);
		db.close();
	}

	/**
	 * Deletes all saved DataObjects.
	 */
	protected synchronized void clearSavedDataObjects(){
		SQLiteDatabase db = scheme.getWritableDatabase();
		db.execSQL("DELETE FROM " + DataTable.TABLE_NAME);
		db.close();
	}

	/**
	 * Deletes all DataObjects which doesn't implement the given datamodels.
	 * @param dataModels The datamodels to check against.
	 */
	protected synchronized void updateCachedDataObjects(Set<DataModel> dataModels){
		SQLiteDatabase db = scheme.getReadableDatabase();
		String query = "Select * FROM " + DataTable.TABLE_NAME + ";";
		Cursor c = db.rawQuery(query, null);
		List<DataObject> objs = new ArrayList<DataObject>();
		while(c.moveToNext()){
			String namespace = c.getString(c.getColumnIndex(DataTable.DATA_NAMESPACE));
			String payload = c.getString(c.getColumnIndex(DataTable.DATA_PAYLOAD));
			
			SAXBuilder reader = new SAXBuilder();
			StringReader in = new StringReader(payload);
			Document document = null;
			try {
			document = reader.build(in);
			} catch (JDOMException e) {
				Log.d("DataWrapper", "An JDOMException was thrown while parsing a newly gotten item.", e);
			} catch (IOException e) {
				Log.d("DataWrapper", "An IOException was thrown while parsing a newly gotten item.", e);
			}
			if (document == null){
				continue;
			}
			Element elem = document.getRootElement();
			DataObject obj = new DataObjectBuilder(elem, namespace).build();
			if (!dataModels.contains(obj.getDataModel())){
				objs.add(obj);
			}
		}
		if (objs.size()>0){
			StringBuilder builder = new StringBuilder("DELETE FROM " + DataTable.TABLE_NAME + " WHERE ");
			for (int i=0; i<objs.size(); i++){
				DataObject obj = objs.get(i);
				builder.append(DataTable.DATA_ID + "=" + obj.getId());
				if (i != objs.size()-1){
					builder.append(" OR ");
				}
			}
			db.execSQL(builder.toString());
		}
		c.close();
		db.close();
	}

	/**
	 * Deletes all DataObjects for a specific Space.
	 * @param nodeId The id of the pubsubnode of the Space.
	 */
	protected synchronized void deleteCachedDataObjectsForSpace(String nodeId){
		SQLiteDatabase db = scheme.getWritableDatabase();
		db.execSQL("DELETE FROM " + DataTable.TABLE_NAME + " WHERE " + DataTable.DATA_NODE + "='" + nodeId + "'");
		db.close();
	}
}
