/**
 * @author brian.gormanly
 * OASVN (Open Android SVN)
 * Copyright (C) 2012 Brian Gormanly
 * Valley Technologies Group
 * http://www.valleytg.com
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version. 
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 * 
 */

package com.valleytg.oasvn.android.ui.activity;

import java.io.File;

import com.valleytg.oasvn.android.R;
import com.valleytg.oasvn.android.application.OASVNApplication;
import com.valleytg.oasvn.android.ui.activity.ConnectionDetails;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class ConnectionDetails extends Activity {
	
	/**
	 * Application layer
	 */
	OASVNApplication app;
	
	/**
	 * Controls
	 */
	TextView topAreaHeader;
	
	TextView status;
	
	TextView topArea1Title;
	TextView topArea2Title;
	TextView topArea3Title;
	TextView topArea4Title;
	TextView topArea5Title;
	
	TextView topArea1;
	TextView topArea2;
	TextView topArea3;
	TextView topArea4;
	TextView topArea5;
	
	Button btnCheckoutHead;
	Button btnCommit;
	Button btnEdit;
	Button btnLog;
	Button btnRevisions;
	Button btnDelete;
	Button btnRepoDelete;
	Button btnFileManager;
	
	/**
	 * Thread control
	 */
	Boolean running = false;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.connection_details);
        
        // get the application
        this.app = (OASVNApplication)getApplication();
        
        this.topAreaHeader = (TextView)findViewById(R.id.conndetail_top_header);
        this.status = (TextView)findViewById(R.id.conndetail_status);
    	
    	this.topArea1Title = (TextView)findViewById(R.id.conndetail_top1_title);
        this.topArea2Title = (TextView)findViewById(R.id.conndetail_top2_title);
        this.topArea3Title = (TextView)findViewById(R.id.conndetail_top3_title);
        this.topArea4Title = (TextView)findViewById(R.id.conndetail_top4_title);
        this.topArea5Title = (TextView)findViewById(R.id.conndetail_top5_title);
        
        this.topArea1 = (TextView)findViewById(R.id.conndetail_top1);
        this.topArea2 = (TextView)findViewById(R.id.conndetail_top2);
        this.topArea3 = (TextView)findViewById(R.id.conndetail_top3);
        this.topArea4 = (TextView)findViewById(R.id.conndetail_top4);
        this.topArea5 = (TextView)findViewById(R.id.conndetail_top5);
        
        // buttons
        btnCheckoutHead = (Button) findViewById(R.id.conndetail_full_checkout);
        btnCommit = (Button) findViewById(R.id.conndetail_full_commit);
        btnDelete = (Button) findViewById(R.id.conndetail_delete_local);
        btnEdit = (Button) findViewById(R.id.conndetail_edit);
        btnRepoDelete = (Button) findViewById(R.id.conndetail_delete_connection);
        btnFileManager = (Button) findViewById(R.id.conndetail_open_fm);
        btnLog = (Button) findViewById(R.id.conndetail_logs);
        btnRevisions = (Button) findViewById(R.id.conndetail_revisions);
        
        // populate the top
        populateTopInfo();
        
        // Make sure the device does not go to sleep while in this acitvity
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        this.btnCheckoutHead.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				// open the add repository activity
				if(running == false) {
					// set the running flag
					ConnectionDetails.this.running = true;
					
					// set the status
					ConnectionDetails.this.status.setText(R.string.performing_checkout);
					
					CheckoutThread checkoutThread = new CheckoutThread();
					checkoutThread.execute();
				}
				else {
					Toast.makeText(ConnectionDetails.this, getString(R.string.in_progress), 2500).show();
				}
			}
		});
        
        this.btnCommit.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				// try to text the phone number
	        	try {
	                Intent callIntent = new Intent(ConnectionDetails.this, CommitActivity.class);
	                // set the current contact
	                //callIntent.putExtra("number", myList.get(position).getPhoneNumber());
	                ConnectionDetails.this.startActivity(callIntent);
	            } catch (ActivityNotFoundException e) {
	                e.printStackTrace();
	            }
			}
		});
        
        this.btnLog.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				
				// open the add repository activity
				Intent intent = new Intent(ConnectionDetails.this, LogView.class);
				startActivity(intent);
				
			}
		});
        
        this.btnRevisions.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				
				// open the add repository activity
				Intent intent = new Intent(ConnectionDetails.this, Revisions.class);
				startActivity(intent);
				
			}
		});

        this.btnEdit.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				
				// open the add repository activity
				Intent intent = new Intent(ConnectionDetails.this, AddRepository.class);
				startActivity(intent);
				
			}
		});
        
        this.btnFileManager.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				// Warn the user that not all file browser support landing in the correct directory
				Toast.makeText(ConnectionDetails.this, getString(R.string.file_manager_warning), 6000).show();
				
	            Intent intent = new Intent();  
	            intent.addCategory(Intent.CATEGORY_OPENABLE);
	            intent.setAction(Intent.ACTION_GET_CONTENT);  
	            intent.setDataAndType(Uri.parse(app.assignPath().toString()), "*/*");
	            startActivity(intent);  
				
			}
		});
        
        this.btnDelete.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				// open the add repository activity
				if(running	== false) {

					
					
					// double check the users intention
					AlertDialog.Builder builder = new AlertDialog.Builder(ConnectionDetails.this);
					
					builder.setIcon(android.R.drawable.ic_dialog_alert);
					builder.setTitle(R.string.confirm);
					builder.setMessage(getString(R.string.delete_message));
					builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

			            public void onClick(DialogInterface dialog, int which) {
			            	synchronized (this) {
			            		try{
			            			app.initializePath();
			            			File tree = app.assignPath();
			            			app.deleteRecursive(tree);
			            			
			            			// set the connection revision back to 0
		    				        app.getCurrentConnection().setHead(0);
		    				        app.saveConnection(app.getCurrentConnection());
		    				        
		    				        // update the header
		    				        populateTopInfo();
		    				        
			            		} 
			            		catch(Exception e) {
			            			e.printStackTrace();
			            		}
			            	}
							
			            }

			        });
					builder.setNegativeButton(R.string.no, null);
					builder.show();	
					
				}
				else {
					Toast.makeText(ConnectionDetails.this, getString(R.string.in_progress), 2500).show();
				}
			}
		});
        
        this.btnRepoDelete.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				
				// open the add repository activity
				if(running	== false) {

					// double check the users intention
					AlertDialog.Builder builder = new AlertDialog.Builder(ConnectionDetails.this);
					
					builder.setIcon(android.R.drawable.ic_dialog_alert);
					builder.setTitle(R.string.confirm);
					builder.setMessage(getString(R.string.delete_repo_message));
					builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

			            public void onClick(DialogInterface dialog, int which) {
			            	// check to see if the user wants to delete the local folder as well
			            	// double check the users intention
							AlertDialog.Builder builder2 = new AlertDialog.Builder(ConnectionDetails.this);
							
							builder2.setIcon(android.R.drawable.ic_dialog_alert);
							builder2.setTitle(R.string.confirm);
							builder2.setMessage(getString(R.string.delete_folder_too));
							builder2.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

					            public void onClick(DialogInterface dialog2, int which) {
					            	// user choose to delete the local folder
					            	synchronized (this) {
					            		try{
					            			app.initializePath();
					            			File tree = app.assignPath();
					            			app.deleteRecursive(tree);
					            			
					            			// close the activity
					            			ConnectionDetails.this.finish();
					            		} 
					            		catch(Exception e) {
					            			e.printStackTrace();
					            		}
					            	}
					            }
				            });
							builder2.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {

					            public void onClick(DialogInterface dialog2, int which) {
					            	// close the activity
			            			ConnectionDetails.this.finish();
					            }
							});
					        builder2.show();	
			            	
					        // remove the connection from the local database
			            	synchronized (this) {
			            		try{
			            			// remove from the database
			            			app.getCurrentConnection().deleteFromDatabase(app);
			            			
			            			// remove from the allConnections array
			            			app.getAllConnections().remove(app.getCurrentConnection());
			            			
			            		} 
			            		catch(Exception e) {
			            			e.printStackTrace();
			            		}
			            	}
			            	
			            	
			            }

			        });
		        builder.setNegativeButton(R.string.no, null);
		        builder.show();	
					
				}
				else {
					Toast.makeText(ConnectionDetails.this, getString(R.string.in_progress), 2500).show();
				}
			}
		});
	}
	
	@Override
	protected void onRestart() {
		// TODO Auto-generated method stub
		super.onRestart();
		
		// populate the top
        populateTopInfo();
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		
		// populate the top
        populateTopInfo();
	}
	
	

	@Override
	public void finish() {
		// TODO Auto-generated method stub
		super.finish();
		
		app.setCurrentConnection(null);
	}

	public void resetIdle() {
		// set the status
		this.status.setText(R.string.idle);
	}

	private void populateTopInfo() {
		
		// create the header area info
		if(this.app.getCurrentConnection() != null) {
			if(this.app.getCurrentConnection().getActive()) {
				// populate the top header
				this.topAreaHeader.setText(this.getString(R.string.connection) + getText(R.string.colon) + this.app.getCurrentConnection().getName());
				
				String url = "";
				try {
					url = this.app.getCurrentConnection().getTextURL().toString();
				}
				catch(Exception e) {
					url = getString(R.string.unknown);
				}
				
				String protocol = "";
				try {
					protocol = this.app.getCurrentConnection().getType().toString();
				}
				catch(Exception e) {
					protocol = getString(R.string.unknown);
				}
				
				String username = "";
				try {
					username = this.app.getCurrentConnection().getUsername().toString();
				}
				catch(Exception e) {
					username = getString(R.string.unknown);
				}
				
				String folder = "";
				try {
					folder = app.getFullPathToMain().toString() + this.app.getCurrentConnection().getFolder().toString();
				}
				catch(Exception e) {
					folder = getString(R.string.unknown);
				}
				
				String head = "";
				try {
					head = this.app.getCurrentConnection().getHead().toString();
				}
				catch(Exception e) {
					head = getString(R.string.unknown);
				}
				
				// assign text to individual text areas
				this.topArea1Title.setText(this.getString(R.string.url) + this.getString(R.string.colon));
				this.topArea1.setText(url);
				
				this.topArea2Title.setText(this.getString(R.string.protocol) + this.getString(R.string.colon));
				this.topArea2.setText(protocol);
				
				this.topArea3Title.setText(this.getString(R.string.username) + this.getString(R.string.colon));
				this.topArea3.setText(username);
				
				this.topArea4Title.setText(this.getString(R.string.folder) + this.getString(R.string.colon));
				this.topArea4.setText(folder);
				
				this.topArea5Title.setText(this.getString(R.string.head) + this.getString(R.string.colon));
				this.topArea5.setText(head);
	
			}
			else {
				// no ticket was selected go back to ticket screen
				// tell the user we are going to work
	        	Toast toast=Toast.makeText(this, getString(R.string.no_connection_selected), 2500);
	    		toast.show();
	    		this.finish();
			}
			
			// check to see if the system is idle
			if(this.running) {
				ConnectionDetails.this.status.setText(R.string.performing_checkout);
			}
			else {
				ConnectionDetails.this.status.setText(R.string.idle);
			}
		}
		else {
			// no ticket was selected go back to ticket screen
			// tell the user we are going to work
        	Toast toast=Toast.makeText(this, getString(R.string.no_connection_selected), 2500);
    		toast.show();
    		this.finish();
		}

	}
	
	class CheckoutThread extends AsyncTask<Void, Void, String> {

		ProgressDialog dialog;

		@Override
	    protected void onPreExecute() {
	        dialog = new ProgressDialog(ConnectionDetails.this);
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
				    	 ConnectionDetails.this.status.setText(R.string.performing_checkout);

				     }
				});
				
				
				// do the checkout
				returned = app.fullHeadCheckout();
				
				// get the current version
				app.getCurrentConnection().setHead((int)app.getRevisionNumber());
				
				// save the current connection
				app.getCurrentConnection().saveToLocalDB(app);

				
			}
	        catch(Exception e) {
	        	e.printStackTrace();
	        	return e.getMessage();
	        }
			return returned;
		}
		
		protected void onPostExecute(final String result) {
			// unset the running flag
			ConnectionDetails.this.resetIdle();

			android.util.Log.d(getString(R.string.alarm), getString(R.string.checkout_successful));

	        dialog.dismiss();
	        
	        runOnUiThread(new Runnable() {
			     public void run() {
			    	// indicate to the user that the action completed
					Toast.makeText(getApplicationContext(), result, 5000).show();
			     }
	        });
	        
	        // populate the top
	        populateTopInfo();
	        
	        ConnectionDetails.this.status.setText(R.string.idle);
	        
			// unset the running flag
			ConnectionDetails.this.running = false;
	    }
	}
}
