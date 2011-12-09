package com.pervasiveradar;

import java.util.ArrayList;

import winterwell.jtwitter.Twitter.Status;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;
import com.pervasiveradar.DirectMsgTask;
import com.pervasiveradar.Retweet;
import com.pervasiveradar.DbOpenHelper;
import com.pervasiveradar.TimelineAdapter;
import com.pervasiveradar.TweetDbActions;
import com.pervasiveradar.ConnectionHelper;
import com.pervasiveradar.OAUTH;
import com.pervasiveradar.Constants;

/**
 * Shows the mentions.
 */
public class MentionsActivity extends Activity {
	ListView listMentions;
	SharedPreferences mSettings,prefs ;	
	ConnectionHelper connHelper ;
	boolean isThereConn = false;
	ArrayList<Status> results = null;
	TimelineAdapter adapter;
	Cursor cursor;
	private SQLiteDatabase db;
	private DbOpenHelper dbHelper;
	AlertDialog.Builder alert;
	private EditText input, inputDirect;
	TweetDbActions dbActions = UpdaterService.getDbActions();
	TweetContextActions contextActions;
	String destinationUsername;
	NotificationManager notificationManager;

	/**
	 * Set everything up
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
		setTitle("Mentions");
		listMentions = (ListView) findViewById(R.id.itemList);	 		
		input = new EditText(this);
		inputDirect = new EditText(this); 	
		mSettings = getSharedPreferences(OAUTH.PREFS, Context.MODE_PRIVATE);
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		dbHelper = new DbOpenHelper(this);
		db = dbHelper.getWritableDatabase(); 
		//dbActions = new TweetDbActions();
		connHelper = new ConnectionHelper(mSettings,TimelineActivity.connec);
		contextActions = new TweetContextActions(connHelper,prefs, mSettings);
		String query = "SELECT tim._id,user,created_at,status,isDisaster FROM MentionsTable AS tim, FriendsTable AS f WHERE tim.userCode = f._id ORDER BY tim.created_at DESC";
		cursor = dbActions.rawQuery(query);

		if (cursor != null)
			if ( cursor.getCount() == 0) {
				finish();
				Toast.makeText(this, "No mentions to be shown", Toast.LENGTH_SHORT).show();     	
			}
		Cursor cursorPictures = db.query(DbOpenHelper.TABLE_PICTURES, null,null, null, null, null, null);
		// Setup the adapter
		cursorPictures.moveToFirst();
		adapter = new TimelineAdapter(this, cursor, cursorPictures);
		listMentions.setAdapter(adapter);
		registerForContextMenu(listMentions);
	}

	/**
	 * Clean up.
	 */
	@Override
	protected void onDestroy() {
		db.close();
		cursor.close();
		super.onDestroy();		
	}


	/**
	 * onResume mainly cancels the pending notification.
	 */
	@Override
	protected void onResume() {
		super.onResume();
		notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		notificationManager.cancel(Constants.MENTION_NOTIFICATION_ID);
	}

	/**
	 * populate the context menu.
	 */
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.add(0, TimelineActivity.REPLY_ID, 0, "Reply");
		menu.add(0, TimelineActivity.RETWEET_ID, 1, "Retweet");	
		menu.add(0,TimelineActivity.DIRECT_ID,2, "Direct Message");
	}  

	/**
	 * Which context menu item was selected?
	 */
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();

		switch(item.getItemId()) {
		case TimelineActivity.REPLY_ID:                                        
			try {
				String user = dbActions.userDbQuery(info, DbOpenHelper.TABLE_MENTIONS);              
				input.setText("@" + user);
				showDialog(0); 

			} catch (Exception ex) {}

			return true;
		case TimelineActivity.RETWEET_ID: 
			if (connHelper.testInternetConnectivity() || 
					prefs.getBoolean("prefDisasterMode", false) == true ) 
				new Retweet(contextActions, DbOpenHelper.TABLE_MENTIONS, this, true).execute(info.id);     


			else 
				Toast.makeText(this, "No internet connectivity", Toast.LENGTH_LONG).show(); 
			return true;
		case TimelineActivity.DIRECT_ID:
			if (connHelper.testInternetConnectivity() || prefs.getBoolean("prefDisasterMode", false) == true) {
				destinationUsername = dbActions.userDbQuery(info, DbOpenHelper.TABLE);

				showDialog(2);
			} 
			else
				Toast.makeText(this, "No internet connectivity", Toast.LENGTH_SHORT).show();   
			return true;

		}
		return super.onContextItemSelected(item);
	}  

	/**
	 * User Dialog to send a DM
	 */
	@Override
	protected Dialog onCreateDialog(int id) {	

		alert = new AlertDialog.Builder(this);

		if (id == 0) {
			alert.setTitle("Send a Tweet");	   	
			// Set an EditText view to get user input     	
			alert.setView(input);  	
			alert.setPositiveButton("Send", new OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					String message = input.getText().toString();	  	
					TimelineActivity.getActivity().sendMessage(message);
					dialog.dismiss();	  		  		
				}
			});

			alert.setNegativeButton("Cancel", new OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					input.setText(""); 
					dialog.dismiss();	  		    
				}
			}); 
		}
		// Dialog to send a direct message
		else {
			alert.setTitle("Insert message");  	
			// Set an EditText view to get user input     	
			alert.setView(inputDirect); 	
			alert.setPositiveButton("Send", new OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					String message = inputDirect.getText().toString();
					new DirectMsgTask(message,destinationUsername,MentionsActivity.this, connHelper,
							mSettings,prefs.getBoolean("prefDisasterMode", false)).execute();	  		

				}
			});

			alert.setNegativeButton("Cancel", new OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					inputDirect.setText(""); 
					dialog.dismiss();	  		    
				}
			}); 
		}

		return alert.create();	
	}





}
