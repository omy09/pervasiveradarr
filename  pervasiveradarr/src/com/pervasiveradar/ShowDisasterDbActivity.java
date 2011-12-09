package com.pervasiveradar;

import java.util.Calendar;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;


/**
 * Activity to show the tweets in the disaster DB.
 */
public class ShowDisasterDbActivity extends Activity{

	ListView listTimeline;  

	DbOpenHelper dbHelper;
	// SQLiteDatabase db ;
	Cursor cursor;
	TweetDbActions dbActions = UpdaterService.getDbActions();
	TimelineAdapter adapter;
	Calendar cal = Calendar.getInstance(); 
	private static final String TAG = "showDisasterDb";
	int order = 0;
	TwitterStatusReceiver twitterStatusReceiver;

	/**
	 * Set up.
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// Are we in disaster mode?
		if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("prefDisasterMode", false) == true) {
			setTheme(R.style.twimightDisasterTheme);
		} else {
			setTheme(R.style.twimightTheme);
		}

		super.onCreate(savedInstanceState);
		setContentView(R.layout.simplelist);			
		// Find views by id
		listTimeline = (ListView) findViewById(R.id.itemList);
		setTitle("Disaster Database");
		dbHelper = new DbOpenHelper(this);			  	  

		// Get the data from the DB
		String query = "SELECT tim._id,user,created_at,status FROM DisasterTable AS tim, FriendsTable AS f WHERE tim.userCode = f._id AND isValid=1 ORDER BY tim.created_at DESC";
		cursor = dbActions.rawQuery(query);
		Log.d(TAG,"disaster table count = " + cursor.getCount()); 
		startManagingCursor(cursor);  

		// Setup the adapter		    
		Cursor cursorPictures = dbActions.queryGeneric(DbOpenHelper.TABLE_PICTURES, null, null, null);
		cursorPictures.moveToFirst();
		adapter = new TimelineAdapter(this, cursor, cursorPictures);    
		listTimeline.setAdapter(adapter);   

		twitterStatusReceiver = new TwitterStatusReceiver();
		registerReceiver(twitterStatusReceiver, new IntentFilter(Constants.ACTION_NEW_DISASTER_TWEET));

	}

	/**
	 * Cancels the user notifiaction about new disaster tweets.
	 */
	private void cancelNotification() {
		NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		notificationManager.cancel(Constants.NOTIFICATION_ID);
	}

	/**
	 * onResume cancels the user notification about new disaster tweets.
	 */
	@Override
	public void onResume() {
		super.onResume();
		// Cancel notification
		cancelNotification(); 

	}

	/**
	 * Populates options menu.
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu){
		super.onCreateOptionsMenu(menu);
		//MenuInflater inflater = getMenuInflater();
		//inflater.inflate(R.menu.menu_disaster_db, menu);
		return true;
	}

	/**
	 * Which option did the user select from the options menu?
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
		case R.id.menu_clear:
			//db.delete(DbOpenHelper.TABLE_DISASTER, null, null);
			//db.delete(DbOpenHelper.TABLE_ADDRESSES, null, null);
			//db.delete(DbOpenHelper.TABLE,DbOpenHelper.C_IS_DISASTER + "=" + Timeline.TRUE, null);
			cursor.requery();
			return true;		

		}
		return false;
	}

	/**
	 * Clean up.
	 */
	@Override
	protected void onDestroy() {

		super.onDestroy();
		cursor.close();		
		unregisterReceiver(twitterStatusReceiver);
	}

	/**
	 * Requery if there are new tweets.
	 * @author pcarta
	 *
	 */
	class TwitterStatusReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {		   
			cursor.requery();
		}
	}




}
