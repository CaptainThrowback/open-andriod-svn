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

package com.valleytg.oasvn.android.application;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.BasicAuthenticationManager;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNCommitClient;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNUpdateClient;
import org.tmatesoft.svn.core.wc.SVNWCClient;
import org.tmatesoft.svn.core.wc.SVNWCUtil;
import org.tmatesoft.svn.core.wc.admin.SVNLookClient;

import com.valleytg.oasvn.android.R;
import com.valleytg.oasvn.android.database.DatabaseHelper;
import com.valleytg.oasvn.android.model.Connection;
import com.valleytg.oasvn.android.model.LogItem;
import com.valleytg.oasvn.android.util.Settings;

import android.app.Application;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.util.Log;

public class OASVNApplication extends Application {

	/**
	 * database
	 */
	public SQLiteDatabase database;
	
	/**
	 * Full path to main folder
	 */
	private String fullPathToMain = "";
	
	/**
	 * Maintains application perspective on whether or not there is external 
	 * storage available.
	 */
	private boolean mExternalStorageAvailable = false;
	
	/**
	 * Maintains applications perspective on whether or not the external storage
	 * is writable.
	 */
    private boolean mExternalStorageWriteable = false;
    
    /**
     * Path information
     */
    private File rootPath = null;
    
    /**
     * Current connection
     */
    private Connection currentConnection;
    
    /**
     * Current Log
     */
    private LogItem currentLog;
    
    /**
     * Current Revision
     */
    private SVNLogEntry currentRevision;
    
    /**
     * All connections
     */
    private ArrayList<Connection> allConnections;
    
    /**
     * BasicAithenticationManager sets up the svn authentication with the server.
     */
    private BasicAuthenticationManager myAuthManager;
    
    /**
     * The SVNClientManager class is used to manage SVN*Client objects
     */
    private SVNClientManager clientManager;
    
    /**
     * The SVNLookClient class provides API for examining different aspects of a 
     * Subversion repository. Its functionality is similar to the one of the 
     * Subversion command-line utility called svnlook. The following table matches 
     * methods of SVNLookClient to the corresponding commands of the svnlook 
     * utility (to make sense what its different methods are for):
     */
    private SVNLookClient lookClient;
    
    /**
     * This class provides methods which allow to check out, update, switch and 
     * relocate a Working Copy as well as export an unversioned directory or file 
     * from a repository.
     */
    private SVNUpdateClient updateClient;
    
    /**
     * The SVNCommitClient class provides methods to perform operations that relate 
     * to committing changes to an SVN repository. These operations are similar to 
     * respective commands of the native SVN command line client and include ones 
     * which operate on working copy items as well as ones that operate only on a 
     * repository.
     */
    private SVNCommitClient commitClient;
    
    /**
     * The SVNWCClient class combines a number of version control operations mainly 
     * intended for local work with Working Copy items. This class includes those 
     * operations that are destined only for local work on a Working Copy as well 
     * as those that are moreover able to access a repository.
     */
    private SVNWCClient wcClient;
    
    /**
     * The SVNWCUtil is a utility class providing some common methods used by 
     * Working Copy API classes for such purposes as creating default run-time 
     * configuration and authentication drivers and some others.
     */
    private SVNWCUtil wcUtil;
    
    /**
     * 
     */
    private SVNCommitInfo info;
    
    /**
     * Commit comments
     */
    private String commitComments = "";
    
    
    /**
     * Constructor
     */
    public OASVNApplication() {
    	
    	// initialize arraylists
    	this.allConnections = new ArrayList<Connection>();
    	
    	// Initialize the settings
		Settings.getInstance();
    	
    	// initialize the storage state
    	this.discoverStorageState();
    	
    	// make sure the app is initialized
		this.initAuthManager();
		
		// make sure the path is ready
		this.initializePath();
    	
    }
    
    /**
     * OnCreate
     */
    @Override
	public void onCreate() {
		super.onCreate();
		
		// retrieve the database
		DatabaseHelper helper = new DatabaseHelper(this, this);
		database = helper.getWritableDatabase();
		
		// try to retrieve the settings data
		this.initalizeSettings();
    }
    
    /**
	 * Try to retrieve the settings from the database if they exist.
	 * If they do not yet exist, create them.
	 */
	public void initalizeSettings() {
		// try to retrieve the data
		this.retrieveSettings();
		
		// see if settings existed
		if(Settings.getInstance().getRootFolder().length() == 0) {
			// there are no settings in the database create default
			Settings.getInstance().setRootFolder("OASVN/");
			Settings.getInstance().saveToLocalDB(this);
		}
	}
    
	/**
	 * This method should be called anytime a new currentConnection is chosen
	 * before any action is attempted.
	 * 
	 */
    public void initAuthManager() {
    	try {
	    	// check to see that we have a current connection
	    	if(currentConnection != null) { 
	    		// initialize the Auth manager
	    		myAuthManager = new BasicAuthenticationManager(this.currentConnection.getUsername(), this.currentConnection.getPassword());
	    	}
    	}
    	catch(Exception e) {
    		e.printStackTrace();
    	}
    	
    	// initialize the clientManager
    	this.initClientManager();
    	
    	// initialize the clientManager children
    	this.initManagerChildren();
    }
    
    public void initClientManager() {
    	clientManager = SVNClientManager.newInstance();
    	clientManager.setAuthenticationManager(myAuthManager); 
    }
    
    public void initManagerChildren() {
    	// Update Client
    	updateClient = clientManager.getUpdateClient();
    	
    	// Commit Client
    	commitClient = clientManager.getCommitClient();
    	
    	// look client
    	lookClient = clientManager.getLookClient();
    	
    	// Working copy client
    	wcClient = new SVNWCClient(this.myAuthManager, null); 
    	
    	// working copy util
    	wcUtil = new SVNWCUtil();
    }
    
    /**
     * Gets the current connection path 
     * @return the path as a File.
     */
    public File assignPath() {
    	// check to see that there is a path
    	try {
	    	if(this.currentConnection != null && this.currentConnection.getFolder().length() > 0) {
	    		// get the sd card directory
				File file = new File(this.getRootPath(), this.currentConnection.getFolder());
				
				return file;
	    	}
    	}
    	catch(Exception e) {
    		e.printStackTrace();
    		return null;
    	}
    	return null;
    }
    
    
    public void deleteRecursive(File tree) {
    	if (tree.isDirectory())
	        for (File child : tree.listFiles())
	            this.deleteRecursive(child);

		tree.delete();
    }
    
    
    public void discoverStorageState() {
    	// get the current state of external storage
    	String state = Environment.getExternalStorageState();

	    if (Environment.MEDIA_MOUNTED.equals(state)) {
	        // We can read and write the media
	        setmExternalStorageAvailable(setmExternalStorageWriteable(true));
	    } 
	    else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
	        // We can only read the media
	        setmExternalStorageAvailable(true);
	        setmExternalStorageWriteable(false);
	    } 
	    else {
	        // Something else is wrong. It may be one of many other states, but all we need
	        //  to know is we can neither read nor write
	        setmExternalStorageAvailable(setmExternalStorageWriteable(false));
	    }
    }
    
    public void initializePath() {
    	try {
    		String mainFolder = "";
    		
    		// check to see if there is a default folder from the settings
    		if(Settings.getInstance().getRootFolder().length() == 0) {
    			mainFolder = "OASVN/";
    		}
    		else {
    			mainFolder = Settings.getInstance().getRootFolder();
    		}
    		
    		// set the full path to main
    		this.setFullPathToMain(Environment.getExternalStorageDirectory() + "/" + mainFolder);
    		
	    	File folder = new File(this.getFullPathToMain());
	    	
		    if(!folder.exists()){
		    	// folder does not yet exist, create it.
		         folder.mkdir();
		         this.setRootPath(folder);
		         Log.i(getString(R.string.FILE), getString(R.string.directory_created)); 
		    }
		    else {
		    	// folder already exists
		    	this.setRootPath(folder);
		    	//Log.i(getString(R.string.FILE), getString(R.string.directory_exists)); 
		    }
		    
    	}
    	catch(Exception e) {
    		Log.e("FILE", "can't create folder");
    		e.printStackTrace();
    	}
    }
    
    // SVNKit wrapper
    
    /**
     * Check to see if the folder exists as a local working copy, and is under version
     * control
     * @param directory to verify
     * @return true if the folder is a version controlled working copy
     */
    public Boolean verifyWorkingCopy(File file) {
    	Boolean state = true;
    	
    	try {
    		state = wcUtil.isWorkingCopyRoot(file);
    		state = wcUtil.isVersionedDirectory(file);
    	}
		catch(VerifyError ve) {
			String msg = ve.getMessage();
			
			// log this failure
			this.getCurrentConnection().createLogEntry(this, getString(R.string.error), ve.getMessage().substring(0, 19), ve.getMessage().toString());
			
			ve.printStackTrace();
			
			// set the state to false
			state = false;
		}
		catch(Exception e) {
			String msg = e.getMessage();
			
			// log this failure
			this.getCurrentConnection().createLogEntry(this, getString(R.string.error), e.getCause().toString().substring(0, 19), e.getMessage().toString());
			
			e.printStackTrace();
			
			// set the state to false
			state = false;
		}
    	
    	return state;
    }
    
    /**
     * Retrieves all of the directories for the current repository, from the root
     * @return ArrayList<SVNDirEntry> - Contains all directories as objects
     */
    public Collection<SVNLogEntry> getAllRevisions() {
    	// initialize the auth manager
		this.initAuthManager();
		
    	SVNURL url = this.currentConnection.getRepositoryURL();

    	Collection<SVNLogEntry> logEntries = null;
    	
    	long startRevision = 0;
    	long endRevision = -1; //HEAD (i.e. the latest) revision
    	
		try {
			SVNRepository repos = SVNRepositoryFactory.create(url);
			repos.setAuthenticationManager(getMyAuthManager());

	    	logEntries = repos.log( new String[] { "" }, null, startRevision, endRevision, true, true );
	    	
	    	
		} 
		catch(SVNException se) {
			String msg = se.getMessage();
			
			// log this failure
			this.getCurrentConnection().createLogEntry(this, getString(R.string.error), se.getMessage().substring(0, 19), se.getMessage().toString());
			
			se.printStackTrace();
		}
		catch(VerifyError ve) {
			String msg = ve.getMessage();
			
			// log this failure
			this.getCurrentConnection().createLogEntry(this, getString(R.string.error), ve.getMessage().substring(0, 19), ve.getMessage().toString());
			
			ve.printStackTrace();
		}
		catch(Exception e) {
			String msg = e.getMessage();
			
			// log this failure
			this.getCurrentConnection().createLogEntry(this, getString(R.string.error), e.getCause().toString().substring(0, 19), e.getMessage().toString());
			
			e.printStackTrace();
		}
		
    	return logEntries;
    	
    }
    
    /**
     * Retrieves the last (num) the revisions for the current repository, from the root
     * NOTE: not helping. will need another approach.
     * @param num = The number of revisions you want to recover.  Will retrieve the last
     * (num) revisions
     * @return ArrayList<SVNDirEntry> - Contains all directories as objects
     */
    public Collection<SVNLogEntry> getXRevisions(Long num) {
    	// initialize the auth manager
		this.initAuthManager();
		
    	SVNURL url = this.currentConnection.getRepositoryURL();

    	Collection<SVNLogEntry> logEntries = null;
    	
    	
    	
		try {
			SVNRepository repos = SVNRepositoryFactory.create(url);
			
			// set the authentication manager
			repos.setAuthenticationManager(getMyAuthManager());
			
			// get the most recent revision
			long endRevision = repos.getLatestRevision();
		
			long startRevision;
			// determine the start revision number
			if (endRevision - num >= 0) {
				startRevision = endRevision - num;
			}
			else {
				startRevision = 0;
			}

	    	logEntries = repos.log( new String[] { "" }, null, startRevision, endRevision, true, true );
	    	
	    	
		} 
		catch (SVNException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch(VerifyError ve) {
			String msg = ve.getMessage();
			
			// log this failure
			this.getCurrentConnection().createLogEntry(this, getString(R.string.error), ve.getMessage().substring(0, 19), ve.getMessage().toString());
			
			ve.printStackTrace();
		}
		catch(Exception e) {
			String msg = e.getMessage();
			
			// log this failure
			this.getCurrentConnection().createLogEntry(this, getString(R.string.error), e.getCause().toString().substring(0, 19), e.getMessage().toString());
			e.printStackTrace();
		}
		
    	return logEntries;
    	
    }
    
    /**
     * Retrieves all of the directories for the current repository, from the root
     * @return ArrayList<SVNDirEntry> - Contains all directories as objects
     */
    public Collection<SVNDirEntry> getAllDirectories() {
    	// initialize the auth manager
		this.initAuthManager();
		
    	SVNURL url = this.currentConnection.getRepositoryURL();

    	Collection<SVNDirEntry> entriesList = null;
		try {
			SVNRepository repos = SVNRepositoryFactory.create(url);
			repos.setAuthenticationManager(getMyAuthManager());
	    	long headRevision = repos.getLatestRevision();
			entriesList = repos.getDir("", headRevision, null, (Collection) null);

		} 
		catch (SVNException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch(VerifyError ve) {
			String msg = ve.getMessage();
			
			// log this failure
			this.getCurrentConnection().createLogEntry(this, getString(R.string.error), ve.getMessage().substring(0, 19), ve.getMessage().toString());
			
			ve.printStackTrace();
		}
		catch(Exception e) {
			String msg = e.getMessage();
			
			// log this failure
			this.getCurrentConnection().createLogEntry(this, getString(R.string.error), e.getCause().toString().substring(0, 19), e.getMessage().toString());
			e.printStackTrace();
		}
		
    	return entriesList;
    	
    }
    
    /**
     * Does full checkout of the Head revision
     * @return
     */
    public String fullHeadCheckout() {
    	try {
    		// initialize the auth manager
    		this.initAuthManager();
    		
    		// make sure the path is ready
    		initializePath();
    		
    		SVNURL myURL = this.currentConnection.getRepositoryURL();
    		File myFile = this.assignPath();
    		SVNRevision pegRevision = SVNRevision.UNDEFINED;
    		SVNRevision myRevision = SVNRevision.HEAD;
    		SVNDepth depth = SVNDepth.INFINITY;
    		try {
    			// do the checkout
    			Long rev = updateClient.doCheckout(myURL, myFile, pegRevision, myRevision, depth, true);
    			
    			// log this success
    			this.getCurrentConnection().createLogEntry(this, getString(R.string.checkout), "", getString(R.string.revision) + " " + rev.toString());
    		}
    		catch(SVNException se) {
    			String msg = se.getMessage();
    			
    			// log this failure
    			this.getCurrentConnection().createLogEntry(this, getString(R.string.error), se.getMessage().substring(0, 19), se.getMessage().toString());
    			
    			return msg;
    		}
    		catch(VerifyError ve) {
    			String msg = ve.getMessage();
    			
    			// log this failure
    			this.getCurrentConnection().createLogEntry(this, getString(R.string.error), ve.getMessage().substring(0, 19), ve.getMessage().toString());
    			
    			ve.printStackTrace();
    			return getString(R.string.verify) + " " + msg;
    		}
    		catch(Exception e) {
    			String msg = e.getMessage();
    			
    			// log this failure
    			this.getCurrentConnection().createLogEntry(this, getString(R.string.error), e.getCause().toString().substring(0, 19), e.getMessage().toString());
    			
    			e.printStackTrace();
    			return getString(R.string.exception) + " " + msg;
    		}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return e.getMessage();
		}
		return "success";
    }
    
    /**
     * Does a full commit to the repository
     * @return Response from the server (error code or success)
     */
    @SuppressWarnings("deprecation")
	public String fullCommit() {
    	
		
		// make sure the path is ready
		initializePath();
		
		SVNURL myURL = this.currentConnection.getRepositoryURL();
		File myFile = this.assignPath();
		SVNRevision myRevision = SVNRevision.HEAD;
		
		try {
			wcClient.doAdd(myFile, true, false, false, SVNDepth.INFINITY, false, 
			false, false);
		} catch (SVNException se) {
			// log this failure
			this.getCurrentConnection().createLogEntry(this, getString(R.string.error), se.getMessage().substring(0, 19), se.getMessage().toString());
			se.printStackTrace();
		} 
		catch(Exception e) {
			String msg = e.getMessage();
			
			// log this failure
			this.getCurrentConnection().createLogEntry(this, getString(R.string.error), e.getMessage().substring(0, 19), e.getMessage().toString());
			
			return msg;
		}
		

		try {
			setInfo(commitClient.doCommit(new File[] {myFile} , false, this.commitComments , false , true));
			System.out.println("Revision " + getInfo().getNewRevision());
			
			// check to see if the commit revision is -1 (means nothing was committed - no change)
			if(getInfo().getNewRevision() == -1) {
				this.getCurrentConnection().createLogEntry(this, getString(R.string.commit), getString(R.string.no_change) + getString(R.string.colon) + " "
						+ Long.toString(getInfo().getNewRevision()), getString(R.string.no_changes_available));
			}
			else {
				// log that the commit was successful
				this.getCurrentConnection().createLogEntry(this, getString(R.string.commit), getString(R.string.commit_success) + getString(R.string.colon) + " " 
						+ Long.toString(getInfo().getNewRevision()), getString(R.string.commit_comments) + getString(R.string.colon) + " " 
						+ this.commitComments + "\n" + getString(R.string.author) + getString(R.string.colon) + " " + getInfo().getAuthor());
			}
		}
		catch(SVNException e) {
			String msg = e.getMessage();
			
			// log this failure
			this.getCurrentConnection().createLogEntry(this, getString(R.string.error), e.getMessage().substring(0, 19), e.getMessage().toString());
			
			return msg;
		}
		
		Long revision = getInfo().getNewRevision();
		
		return Long.toString(revision);
		
    }
    
    public String update() {
    	
    	Long rev = 0L;
    	try {
    		
    		SVNURL myURL = this.currentConnection.getRepositoryURL();
    		File myFile = this.assignPath();
    		SVNRevision pegRevision = SVNRevision.UNDEFINED;
    		SVNRevision myRevision = SVNRevision.HEAD;
    		SVNDepth depth = SVNDepth.INFINITY;
    		try {
    			// do the checkout
    			rev = updateClient.doUpdate(myFile, myRevision, SVNDepth.INFINITY, false, true);
    			
    			// log this success
    			this.getCurrentConnection().createLogEntry(this, getString(R.string.update), " ", getString(R.string.revision) + " " + rev.toString());
    		}
    		catch(SVNException se) {
    			String msg = se.getMessage();
    			
    			// log this failure
    			this.getCurrentConnection().createLogEntry(this, getString(R.string.error), se.getMessage().substring(0, 19), se.getMessage().toString());
    			
    			return msg;
    		}
    		catch(VerifyError ve) {
    			String msg = ve.getMessage();
    			
    			// log this failure
    			this.getCurrentConnection().createLogEntry(this, getString(R.string.error), ve.getMessage().substring(0, 19), ve.getMessage().toString());
    			
    			ve.printStackTrace();
    			return getString(R.string.verify) + " " + msg;
    		}
    		catch(Exception e) {
    			String msg = e.getMessage();
    			
    			// log this failure
    			this.getCurrentConnection().createLogEntry(this, getString(R.string.error), e.getCause().toString().substring(0, 19), e.getMessage().toString());
    			
    			e.printStackTrace();
    			return getString(R.string.exception) + " " + msg;
    		}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return e.getMessage();
		}
    	
    	return Long.toString(rev);
    }
    
    /**
     * Clean up the current connection
     * Recursively cleans up the working copy, removing locks and resuming unfinished operations.
     * If you ever get a "working copy locked" error, use this method to remove stale locks and 
     * get your working copy into a usable state again.
     * @return operation result text to be logged or displayed to the user
     */
    public String cleanUp() {
    	try {
    		// do the cleanup on the current connection
    		wcClient.doCleanup(this.assignPath());
    		
    		// log this success
			this.getCurrentConnection().createLogEntry(this, getString(R.string.cleanup), "",  getString(R.string.success));
    	} 
    	catch (SVNException se) {
    		String msg = se.getMessage();
    		
			// log this failure
			this.getCurrentConnection().createLogEntry(this, getString(R.string.error), se.getMessage().substring(0, 19), se.getMessage().toString());
			
			// catlog the failure
			se.printStackTrace();
			
			// display the failure
			return msg;
		} 
		catch(Exception e) {
			String msg = e.getMessage();
			
			// log this failure
			this.getCurrentConnection().createLogEntry(this, getString(R.string.error), e.getMessage().substring(0, 19), e.getMessage().toString());
			
			// catlog the failure
			e.printStackTrace();
			
			// display the failure
			return msg;
		}
		
		return getString(R.string.success);
    }
    
    /**
     * Gets the revision number.
     * @return integer value of the current checked out revision
     */
    public Integer getRevisionNumber() {
    	
    	try {
    		// make sure there is a selected connection
        	if(this.getCurrentConnection() != null) {
        		// initialize the auth manager
        		//this.initAuthManager();
        		
        		Integer rev = (int) clientManager.getStatusClient().doStatus(this.assignPath(), false).getRevision().getNumber();
        		
        		// log that the revision number was retrieved 
        		this.getCurrentConnection().createLogEntry(this, getString(R.string.revision), getString(R.string.rev_no) + getString(R.string.colon) 
        				+ " " + rev, getString(R.string.local_rev_updated) + getString(R.string.colon) + " " + rev);
        		
        		return rev;
        	}
        	else {
        		return 0;
        	}
			
    	}
    	catch(SVNException se) {
			String msg = se.getMessage();
			
			// log this failure
			this.getCurrentConnection().createLogEntry(this, getString(R.string.error), se.getMessage().substring(0, 19), se.getMessage().toString());
			
			return 0;
		}
		catch(VerifyError ve) {
			String msg = ve.getMessage();
			
			// log this failure
			this.getCurrentConnection().createLogEntry(this, getString(R.string.error), ve.getMessage().substring(0, 19), ve.getMessage().toString());
			
			ve.printStackTrace();
			return 0;
		}
		catch(Exception e) {
			String msg = e.getMessage();
			
			// log this failure
			this.getCurrentConnection().createLogEntry(this, getString(R.string.error), e.getCause().toString().substring(0, 19), e.getMessage().toString());
			
			e.printStackTrace();
			return 0;
		}

    }
    
    //public ArrayList<Revision> getAllRevisions() {
    //	lookClient.
    //}
    
    
    /**
     * Local member special methods
     */
    
    /**
     * Get all stored connections from the local database
     */
    public void retrieveAllConnections() {
    	String sql = "select * from connection where active > 0;";
		Cursor dbCursor = this.database.rawQuery(sql, null);
		dbCursor.moveToFirst();
		
		if(!dbCursor.isAfterLast()) {
			
			// clear out any user currently stored in mem
			this.allConnections.removeAll(this.allConnections);
			
			// iterate through local and populate
			while(!dbCursor.isAfterLast()) {
				Connection thisConnection = new Connection();
				thisConnection.setData(dbCursor);
				dbCursor.moveToNext();
				
				this.allConnections.add(thisConnection);
			}
		}
		dbCursor.close();
    }
    
   
    
    public void retrieveSettings() {
    	String sql = "select * from setting where id = 1;";
		Cursor dbCursor = this.database.rawQuery(sql, null);
		dbCursor.moveToFirst();
		
		if(!dbCursor.isAfterLast()) {
			// iterate through local and populate
			while(!dbCursor.isAfterLast()) {
				Settings.getInstance().setData(dbCursor);
				dbCursor.moveToNext();
			}
		}
		dbCursor.close();
    }
    
    /**
     * Handles saving the connection to the in-memory arraylist and to the local
     * database.  Manages whether or not a connection is an update to an existing
     * connection or a new connection.
     * @param connection - connection to be saved or updated.
     */
    public void saveConnection(Connection connection) {
		
		// check all existing connection
		int flag = -1;
		if(this.getAllConnections().size() > 0) {
			for(int i=0; i < this.getAllConnections().size(); i++) {
				if(connection.getLocalDBId() == this.getAllConnections().get(i).getLocalDBId()) {
					flag = i;
				}
			}
		}
		
		// do the update or insert to the arraylist
		if(flag >= 0) {
			// entry exists, replace it
			this.getAllConnections().set(flag, connection);
		}
		else {
			// new
			this.getAllConnections().add(connection);
		}

		// save to the local database
		connection.saveToLocalDB(this);
		
	}
    
   
	public void setmExternalStorageAvailable(boolean mExternalStorageAvailable) {
		this.mExternalStorageAvailable = mExternalStorageAvailable;
	}

	public boolean ismExternalStorageAvailable() {
		return mExternalStorageAvailable;
	}

	public boolean setmExternalStorageWriteable(boolean mExternalStorageWriteable) {
		this.mExternalStorageWriteable = mExternalStorageWriteable;
		return mExternalStorageWriteable;
	}

	public boolean ismExternalStorageWriteable() {
		return mExternalStorageWriteable;
	}

	public void setRootPath(File rootPath) {
		this.rootPath = rootPath;
	}

	public File getRootPath() {
		return rootPath;
	}

	public SQLiteDatabase getDatabase() {
		return database;
	}

	public void setDatabase(SQLiteDatabase database) {
		this.database = database;
	}

	public Connection getCurrentConnection() {
		return currentConnection;
	}

	public void setCurrentConnection(Connection currentConnection) {
		this.currentConnection = currentConnection;
	}

	public BasicAuthenticationManager getMyAuthManager() {
		return myAuthManager;
	}

	public void setMyAuthManager(BasicAuthenticationManager myAuthManager) {
		this.myAuthManager = myAuthManager;
	}

	public SVNClientManager getCm() {
		return clientManager;
	}

	public void setCm(SVNClientManager clientManager) {
		this.clientManager = clientManager;
	}

	public SVNUpdateClient getUc() {
		return updateClient;
	}

	public void setUc(SVNUpdateClient updateClient) {
		this.updateClient = updateClient;
	}

	public void setAllConnections(ArrayList<Connection> allConnections) {
		this.allConnections = allConnections;
	}
	
	public ArrayList<Connection> getAllConnections() {
		return this.allConnections;
	}

	public void setInfo(SVNCommitInfo info) {
		this.info = info;
	}

	public SVNCommitInfo getInfo() {
		return info;
	}

	public void setCommitComments(String commitComments) {
		this.commitComments = commitComments;
	}

	public String getCommitComments() {
		return commitComments;
	}

	public void setFullPathToMain(String fullPathToMain) {
		this.fullPathToMain = fullPathToMain;
	}

	public String getFullPathToMain() {
		return fullPathToMain;
	}

	public void setCurrentLog(LogItem currentLog) {
		this.currentLog = currentLog;
	}

	public LogItem getCurrentLog() {
		return currentLog;
	}

	public void setCurrentRevision(SVNLogEntry currentRevision) {
		this.currentRevision = currentRevision;
	}

	public SVNLogEntry getCurrentRevision() {
		return currentRevision;
	}

	public void setWcUtil(SVNWCUtil wcUtil) {
		this.wcUtil = wcUtil;
	}

	public SVNWCUtil getWcUtil() {
		return wcUtil;
	}
}
