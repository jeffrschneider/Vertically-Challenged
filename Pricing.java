package com.transcendcomputing.cloudanalysis;

import com.mongodb.Mongo;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.DBCursor;
import com.mongodb.ServerAddress;

import java.util.Arrays;

public class Pricing {

	private Mongo m = null;
	private DB db = null;
	
	public void establishConnection()
	{
		

		try {
			m = new Mongo( "YOUR IP HERE", 27017);
		} catch(Exception e) { System.out.println(e); }
		
		db = m.getDB( "stack_place_development" );
		boolean auth = db.authenticate("admin", "YOUR PASSWORD HERE".toCharArray());
				
		
		//System.out.println(db.getCollectionNames());
		
		DBCollection collection = db.getCollection("clouds");
		
		System.out.println(collection.count());
		  
		 DBCursor cursor = collection.find();
	        try {
	            while(cursor.hasNext()) {
	                System.out.println(cursor.next());
	            }
	        } finally {
	            cursor.close();
	        }
		
		
		/*
		BasicDBObject query = new BasicDBObject();

	        query.put("i", 71);

	        cursor = coll.find(query);

	        try {
	            while(cursor.hasNext()) {
	                System.out.println(cursor.next());
	            }
	        } finally {
	            cursor.close();
	        }
	        */
		
		//		db.clouds.find({'cloud_provider': 'OpenStack'}, {'compute_prices':1});
		
	}
}
