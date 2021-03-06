package com.barobot.debug;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;

import com.barobot.BarobotMain;
import com.barobot.R;
import com.barobot.android.Android;
import com.barobot.android.AndroidHelpers;
import com.barobot.android.InternetHelpers;
import com.barobot.common.Initiator;
import com.barobot.common.constant.Constant;
import com.barobot.common.interfaces.OnDownloadReadyRunnable;
import com.barobot.gui.dataobjects.Engine;
import com.barobot.hardware.Arduino;
import com.barobot.hardware.devices.BarobotConnector;
import com.barobot.other.UpdateManager;
import com.barobot.parser.Queue;

public class button_click implements OnClickListener{
	private Activity dbw;
	public button_click(Activity debugWindow){
		dbw = debugWindow;
	}
	@Override
	public void onClick(final View v) {		// get out of the UI thread
		Log.i("button click","click");
		new Thread( new Runnable(){
			@Override
			public void run() {
				Log.i("button click","exec start");
				exec(v);
				Log.i("button click","exec end");
			}}).start();
	}
	public void exec(View v) {
		final BarobotConnector barobot	= Arduino.getInstance().barobot;
		switch (v.getId()) {
		case R.id.download_database:
			BarobotMain.getInstance().runOnUiThread(new Runnable() {
				  public void run() {
			    	new AlertDialog.Builder(dbw).setTitle("Are you sure?").setMessage("Are you sure?")
				    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
				        public void onClick(DialogInterface dialog, int which) {
				    //    	boolean success = false;
				        	String path6 = 	Environment.getExternalStorageDirectory()+ Constant.copyPath;
				  			InternetHelpers.doDownload(Constant.databaseWeb, path6, new OnDownloadReadyRunnable() {
				  				public void sendSource( String source ) {	
				  				}
				  			    @Override
				  				public void run() {
				  			    	Android.alertOk(dbw);
				  				}
								@Override
								public void sendProgress(int value) {
									// TODO Auto-generated method stub
								}
				  			});
				        }
				    }).setIcon(android.R.drawable.ic_dialog_alert).show();
			    }
			});
			break;

		case R.id.reset_database:
			BarobotMain.getInstance().runOnUiThread(new Runnable() {
				  public void run() {
			    	new AlertDialog.Builder(dbw).setTitle("Are you sure?").setMessage("Are you sure?")
				    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
				        public void onClick(DialogInterface dialog, int which) {
				        	boolean success = false;
				        	String error_name = "Error";
				        	try {
				        		Date dNow			= new Date( );
				        		SimpleDateFormat dd =  new SimpleDateFormat ("yyyy.MM.dd.hh.mm.ss");
				        		String resetPath	= 	Environment.getExternalStorageDirectory()+ Constant.copyPath;
				        		String backupPath 	= 	Environment.getExternalStorageDirectory()+ Constant.backupPath;
				        		backupPath 			= 	backupPath.replace("%DATE%", dd.format(dNow));
				        		Initiator.logger.i(Constant.TAG,"backupPath path" + backupPath);

				        		// do backup
				        		success = Android.copy( Constant.localDbPath, backupPath );
				        		if(success){
				        			success = Android.copy( resetPath, Constant.localDbPath );
				        			Engine.GetInstance().invalidateData();
				        			AndroidHelpers.resetApplicationData(barobot);
				        		}
							} catch (IOException e) {
								e.printStackTrace();
								success		= false;
								error_name	= e.getMessage();
								Initiator.logger.i(Constant.TAG,"download_database", e);
							}
				        	final String message = success ? "OK": error_name;
				        	dbw.runOnUiThread(new Runnable() {
								  public void run() {
							    	new AlertDialog.Builder(dbw).setTitle("Message").setMessage( message )
								    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
								        public void onClick(DialogInterface dialog, int which) { 
								        	Android.pleaseResetApp(dbw);
								        }
								    }).setIcon(android.R.drawable.ic_dialog_alert).show();
							    }
							});
				        }
				    }).setIcon(android.R.drawable.ic_dialog_alert).show();
			    }
			});
			break;
		case R.id.firmware_download:	
			Android.burnFirmware( dbw, false );
			
			break;
		case R.id.firmware_download_manual:
			Android.burnFirmware( dbw,true );
			break;
		case R.id.force_app_update:
			String url = Constant.android_app;
			if(barobot.pcb_type == 3){
				if(Constant.use_beta){
					url = Constant.android3_app_beta;
				}else{
					url = Constant.android3_app;
				}
			}else if(barobot.pcb_type == 2){
				url = Constant.android_app;
			}
			UpdateManager.openInBrowser( dbw, url );
			break;

		case R.id.new_robot_id:
			Android.setNewRobotId(dbw, barobot);
			break;
		}
	}
}
