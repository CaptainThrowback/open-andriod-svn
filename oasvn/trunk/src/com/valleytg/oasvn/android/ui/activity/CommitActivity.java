package com.valleytg.oasvn.android.ui.activity;

import com.valleytg.oasvn.android.R;
import com.valleytg.oasvn.android.application.OASVNApplication;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class CommitActivity extends Activity {
	
	/**
	 * Thread control
	 */
	Boolean running = false;
	
	/**
	 * Application layer
	 */
	OASVNApplication app;
	
	Button btnCommit;
	EditText comments;
	
	TextView status;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.commit);
		
		// get the application
        this.app = (OASVNApplication)getApplication();
		
		btnCommit = (Button) findViewById(R.id.commit_button);
		comments = (EditText) findViewById(R.id.commit_comments);
		status = (TextView) findViewById(R.id.commit_status);
		
        this.btnCommit.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				// open the add repository activity
				if(running	== false) {
					// set the comments
					app.setCommitComments(CommitActivity.this.comments.getText().toString());
					
					// set the running flag
					CommitActivity.this.running = true;
					
					CommitThread commitThread = new CommitThread();
					commitThread.execute();
				}
				else {
					Toast.makeText(CommitActivity.this, getString(R.string.in_progress), 1500).show();
				}
			}
		});
	}
	
	
	class CommitThread extends AsyncTask<Void, Void, String> {

		ProgressDialog dialog;

		@Override
	    protected void onPreExecute() {
	        dialog = new ProgressDialog(CommitActivity.this);
	        dialog.setMessage(getString(R.string.in_progress));
	        dialog.setIndeterminate(true);
	        dialog.setCancelable(false);
	        dialog.show();
	    }
		
		@Override
		protected String doInBackground(Void... unused) {
			try {
				Looper.myLooper();
				Looper.prepare();
			}
			catch(Exception e) {
				// Looper only needs to be created if the thread is new, if reusing the thread we end up here
			}
			
			String returned;
			
			try {
				runOnUiThread(new Runnable() {
				     public void run() {
				    	// set the status
				    	 CommitActivity.this.status.setText(R.string.performing_commit);

				     }
				});
				
				// do the commit
				returned = app.fullCommit();
				
				// try to set the revision number
				try {
					CommitActivity.this.app.getCurrentConnection().setHead(Integer.parseInt(returned));
				}
				catch (Exception se) {
					CommitActivity.this.app.getCurrentConnection().setHead(0);
					se.getMessage();
				}
				
				// save the current connection
				CommitActivity.this.app.getCurrentConnection().saveToLocalDB(app);
				
				
			}			
	        catch(Exception e) {
	        	
	        	e.printStackTrace();
	        	return e.getMessage();
	        }
	        return returned;
		}
		
		protected void onPostExecute(final String result) {
			
			android.util.Log.d("alarm", "Commit Complete!");
			
			dialog.dismiss();
	        
	        runOnUiThread(new Runnable() {
			     public void run() {
			    	// indicate to the user that the action completed
					Toast.makeText(getApplicationContext(), result, 5000).show();
			     }
	        });

			// unset the running flag
			CommitActivity.this.running = false;
			
			CommitActivity.this.status.setText(R.string.idle);
			
			//close this activity
			CommitActivity.this.finish();
	    }
	}
	
	
}
