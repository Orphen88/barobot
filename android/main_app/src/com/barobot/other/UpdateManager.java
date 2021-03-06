package com.barobot.other;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;

import com.barobot.BarobotMain;
import com.barobot.R;
import com.barobot.android.Android;
import com.barobot.android.InternetHelpers;
import com.barobot.common.Initiator;
import com.barobot.common.IspOverSerial;
import com.barobot.common.constant.Constant;
import com.barobot.common.interfaces.OnDownloadReadyRunnable;
import com.barobot.common.interfaces.serial.IspCommunicator;
import com.barobot.common.interfaces.serial.Wire;
import com.barobot.gui.dataobjects.Log_start;
import com.barobot.hardware.Arduino;
import com.barobot.hardware.devices.BarobotConnector;
import com.barobot.isp.UploadCallBack;
import com.barobot.isp.Uploader;
import com.barobot.isp.enums.Board;
import com.barobot.isp.enums.UploadErrors;
import com.barobot.parser.Queue;
import com.barobot.parser.message.AsyncMessage;
import com.barobot.parser.message.Mainboard;
import com.barobot.parser.utils.Decoder;
import com.eclipsesource.json.JsonObject;

public class UpdateManager{
/*
	public void load(){
		File dir		= new File(Environment.getExternalStorageDirectory(), "Barobot");
		String path 	= dir.getAbsolutePath()+"/"+"drinks.json";
		InternetHelpers.doDownload(Constant.drinks, path, new OnDownloadReadyRunnable() {
			private String source;
			public void sendSource( String source ) {
				this.source = source;
				Initiator.logger.i("update_drinks.sendSource", source);
			}
		    @Override
			public void run() {
		    	InternetHelpers.parseJson( this.source );
			}
		});
	}
	public void sendAllLogs( BarobotConnector barobot ){
		boolean success = false;
		List<com.barobot.gui.dataobjects.Log> logs = 
				Model.fetchQuery(ModelQuery.select()
						.from(com.barobot.gui.dataobjects.Log.class)
						.where(C.eq("send_time", 0))
						.orderBy("Log.time")
						.limit(100)
						.getQuery(),com.barobot.gui.dataobjects.Log.class);

		JsonArray jsonArray = new JsonArray().add( "John" ).add( 23 );
		for(com.barobot.gui.dataobjects.Log log : logs)
		{
			JsonObject c = log.getJson();
			jsonArray.add(c);
		}

		MultipartEntityBuilder builder = MultipartEntityBuilder.create();
		String data				= jsonArray.toString();
		try {
			builder.addPart("log", new StringBody( data ));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return;
		}
		success = prepare_connection(barobot,Constant.upload, builder, true);
		if(success == true){
			for(com.barobot.gui.dataobjects.Log log : logs)
			{
				String c = log.content;
				log.send_time  = 1;
				log.update();
			}
		}
	}

	public void upload_drinks( BarobotConnector barobot ){
		MultipartEntityBuilder builder = MultipartEntityBuilder.create();
		File file				= new File(Constant.localDbPath);
		builder.addPart("myFile", new FileBody(file));
		prepare_connection(barobot,Constant.upload, builder, true);
	}

	public static boolean prepare_connection( BarobotConnector barobot, String address, MultipartEntityBuilder builder, boolean addLogin ){
		HttpClient httpClient	= new DefaultHttpClient();
		HttpPost httpPost		= new HttpPost(address);	
		builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);

		if(addLogin){
			Charset chars			= Charset.forName("UTF-8");
			HardwareState state		= barobot.state;
			try {
				builder.addPart("who", new StringBody( state.get( "MAIN_LOGIN","anonymous"), chars));
				builder.addPart("who2", new StringBody( state.get( "MAIN_PASSWORD","anonymous"), chars));
			} catch (UnsupportedEncodingException e) {
				Initiator.logger.w("update_drinks.prepare_connection UnsupportedEncodingException", e);
			}
		}
		HttpEntity entity = builder.build();
		httpPost.setEntity(entity);
		try {
			HttpResponse response = httpClient.execute(httpPost);
			HttpEntity resEntity = response.getEntity();
			if (resEntity == null) { 
				return false;
			}else{
			    String responseStr = EntityUtils.toString(resEntity).trim();
			    Initiator.logger.i("update_drinks.result: ", responseStr);
			}
		} catch (ClientProtocolException e) {
			Initiator.logger.w("update_drinks.prepare_connection ClientProtocolException", e);
			return false;
		} catch (IOException e) {
			Initiator.logger.w("update_drinks.prepare_connection IOException", e);
			return false;
		}
		return true;
	}
*/

	public static void checkNewVersion( final Activity c, final BarobotConnector barobot, final boolean alertResult) {
		InternetHelpers.downloadNewestVersionNumber( new OnDownloadReadyRunnable() {
			private String res;
			public void sendSource(String source) {
				this.res = source;
			}
			@Override
			public void run() {
		//		Initiator.logger.w("update_drinks.checkNewVersion", ""+res );
				if(res == null){
					Initiator.logger.e("update_drinks.checkNewVersion", "null" );
					if(alertResult){
						Android.alertMessage(c, "Server Response error");
					}
				}else{
					JsonObject jsonObject = null;
					try {
						jsonObject = JsonObject.readFrom(res);
					} catch (com.eclipsesource.json.ParseException e) {
						Initiator.logger.e("UpdateManager.checkNewVersion", "readFrom exception", e );
					}
					if( jsonObject != null && res.length()> 20 && jsonObject.isObject()){
						String key = "version" + barobot.pcb_type + ( Constant.use_beta ? "_beta" : "" );	// version2 or version3 or version2_beta or version3_beta
						Initiator.logger.d("update_drinks.checkNewVersion", "key: "+key );
						if( jsonObject.get(key) == null){
							if(alertResult){
								Android.alertMessage(c, "Error parsing document (532)");
							}
							return;
						}
						JsonObject version = jsonObject.get(key).asObject();
						
						int newest_android_version	= Decoder.toInt(version.get("android").toString());
						int newest_arduino_version	= Decoder.toInt(version.get("arduino").toString());
						int newest_database_version = Decoder.toInt(version.get("database").toString());

						Initiator.logger.d("update_drinks.android", ""+newest_android_version );
						Initiator.logger.d("update_drinks.arduino", ""+newest_arduino_version );
						Initiator.logger.d("update_drinks.database", ""+newest_database_version );

						JsonObject paths = version.get("files").asObject();
						int arduino_ver = barobot.state.getInt("ARDUINO_VERSION", 0);
						if( newest_android_version > Constant.ANDROID_APP_VERSION ){
					//		Initiator.logger.e("update_drinks.checkNewVersion", "newest_android_version: "+ res );
							String androidUrl	= paths.get("android").toString();
							androidUrl = androidUrl.replace("\"", "");
							openInBrowser( c, androidUrl );

						}else if( arduino_ver > 0 && newest_arduino_version > arduino_ver && newest_arduino_version <= Constant.MAX_FIRMWARE_VERSION ){	// maximum version handled by this app version
							String arduinoUrl	= paths.get("arduino").toString();
							downloadAndBurnFirmware( c, barobot.pcb_type, false );
						}else{
							if(alertResult){
								Android.alertMessage(c, "Your firmware version " + newest_android_version + ".0 is up to date.");
							}
						}
						if( newest_database_version > barobot.state.getInt("ARDUINO_VERSION", 0 ) ){
			//				downloadAndUseDatabase( c, Constant.use_beta );
						}
					}else{
						Initiator.logger.e("update_drinks.checkNewVersion", "error: "+ res );
						if(alertResult){
							Android.alertMessage(c, "Server Response error 32");
						}
					}
				}
			}
			@Override
			public void sendProgress(int value) {
			}
		});
	}
	public static void openInBrowser(final Activity c, final String url) {
		c.runOnUiThread(new Runnable() {
			  public void run() {
				  new AlertDialog.Builder(c).setIcon(android.R.drawable.ic_dialog_alert)
			        .setTitle(R.string.update_title)
			        .setMessage(R.string.update_msg)
			        .setPositiveButton(R.string.update_yes, new DialogInterface.OnClickListener() {
			            @Override
			            public void onClick(DialogInterface dialog, int which) {
			            	UpdateManager.downloadAndInstall( c, url );
			            //	Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
			        	//	c.startActivity(browserIntent);
			            }
			        })
			        .setNegativeButton(R.string.update_no, null)
			        .show();
			  }
			});
	}

	public static int getNewRobotId() {
		Initiator.logger.i("update_drinks", "getNewRobotId" );
		String ret = InternetHelpers.downloadRobotId();
		if(ret == null){
			Initiator.logger.e("update_drinks.getRobotId", "null" );
		}else{
			String[] versions = ret.split(",");
			if(versions.length >= 2 ){
				if( versions[0].equals("OK")){
					return Decoder.toInt( versions[1], -1 );
				}else{
					Initiator.logger.e("update_drinks.getRobotId", "error: "+ ret+ " length:" + versions.length+ " 0 =[ versions[0]"+  versions[0]+"]" );
				}
			}else{
				Initiator.logger.e("update_drinks.getRobotId", "error: "+ ret );
			}
		}
		return -1;
	}

	
	public static void downloadAndBurnFirmware(final Activity c, final int pcb_type, final boolean manual_reset ) {
		// ask before
		if(BarobotMain.getInstance().isDestroyed() || BarobotMain.getInstance().isFinishing()){
			return;
		}
		BarobotMain.getInstance().runOnUiThread(new Runnable() {
			  public void run() {
				  new AlertDialog.Builder(c).setTitle("Are you sure?").setMessage("Do you want to update your firmware now? ("+pcb_type+")")
				    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
				        public void onClick(DialogInterface dialog, int which) { 
				        	update_firmware_step2_download(c, pcb_type, manual_reset);
				        }
				    })
				    .setIcon(android.R.drawable.ic_dialog_alert).show();
			  }
			});
	}
	public static void update_firmware_step2_download(final Activity c, final int pcb_type, final boolean manual_reset) {
		// download new version
		final BarobotConnector barobot	= Arduino.getInstance().barobot;
		final String url				= getFirmwareFileUrl( Constant.use_beta, pcb_type );
		final String path9				= getFirmwareFilePath( Constant.use_beta, pcb_type );

		Log_start ls 			= new Log_start();
		ls.datetime				= Decoder.getTimestamp() ;
		ls.start_type			= "fu";
		ls.robot_id				= barobot.getRobotId();
		ls.language				= barobot.state.get("LANG", "pl" );
		ls.app_starts			= barobot.state.getInt("STAT1", 0);
		ls.arduino_starts		= barobot.state.getInt("ARDUINO_STARTS", 0);
		ls.serial_starts		= barobot.state.getInt("STAT2", 0);
		ls.app_version			= Constant.ANDROID_APP_VERSION;
		ls.arduino_version		= barobot.state.getInt("ARDUINO_VERSION", 0);
//		ls.database_version		= Constant.ANDROID_APP_VERSION;
		ls.temp_start			= barobot.getLastTemp();
		ls.insert();

		c.runOnUiThread(new Runnable() {
			   public void run() {
				   	if (c.isFinishing()) {
				   		return;
					}
					updateBarHandler	= new Handler();
					barProgressDialog	= new ProgressDialog(c);
					barProgressDialog.setTitle("Downloading firmware...");
					barProgressDialog.setMessage("Downloading in progress.");
					barProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
					barProgressDialog.setProgress(0);
					barProgressDialog.setMax(100);
					barProgressDialog.show();

					InternetHelpers.doDownload(url, path9, new OnDownloadReadyRunnable() {
						public void sendSource( String source ) {
						}
					    @Override
						public void run() {
				        	updateBarHandler.post(new Runnable() {
			                    public void run() {
			                    	barProgressDialog.dismiss();
			                    }
			                });
				        	if(manual_reset){
				        		update_firmware_manual_reset( c, Constant.use_beta, url, pcb_type );
				        	}else{
				        		update_firmware_step3_burn( c, Constant.use_beta, url, pcb_type );
				        	}
						}
						@Override
						public void sendProgress(final int value) {
				        	updateBarHandler.post(new Runnable() {
			                    public void run() {
			                    	barProgressDialog.setProgress(value);
			            //        	 Initiator.logger.i("fimwareBurn.download","setProgress"+value );
			                    }
			                });
						}
					});
			   }
			});
	}

	private static String getFirmwareFilePath(boolean use_beta, int pcb_type) {
		File base = Environment.getExternalStorageDirectory();
		String res = "";

		if( pcb_type == 2 ){
			res =  base.getAbsolutePath() + ((use_beta) ? Constant.firmware2_beta : Constant.firmware2);
		}else{
			res =  base.getAbsolutePath() + ((use_beta) ? Constant.firmware3_beta : Constant.firmware3);
		}
	   	Initiator.logger.i("UpdateManager.getFirmwareFilePath",res);
		return res;
		
	}
	private static String getFirmwareFileUrl(boolean use_beta, int pcb_type) {
		if( pcb_type == 2 ){
			return (use_beta) ? Constant.firmware2Web_beta : Constant.firmware2Web;
		}else{
			return (use_beta) ? Constant.firmware3Web_beta : Constant.firmware3Web;
		}
	}

	static Handler updateBarHandler;
	static ProgressDialog barProgressDialog;
	public static void update_firmware_step3_burn(final Activity c, boolean use_beta, String url, final int pcb_type) {
		// burn it
		c.runOnUiThread(new Runnable() {
		   public void run() {
				String msg			= c.getResources().getString(R.string.firmware_uploading_in_progress);
				barProgressDialog	= new ProgressDialog(c);
				barProgressDialog.setTitle(R.string.firmware_uploading_title);
				barProgressDialog.setMessage(msg);
				barProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
				barProgressDialog.setProgress(0);
				barProgressDialog.setMax(100);
		   }
		});
		Arduino ar 						= Arduino.getInstance();
		final BarobotConnector barobot	= ar.barobot;
		final Queue q					= barobot.main_queue;
		final Wire connection			= ar.getConnection();
		final String hex_firmware_path	= getFirmwareFilePath( use_beta, pcb_type );
		final IspOverSerial mSerial		= new IspOverSerial(connection);
		final Uploader  ispUploader		= new Uploader();

		barobot.lightManager.setAllLeds(q, "11", 254, 254, 0, 0);
		Arduino.firmwareUpload			= true;

        q.add( new AsyncMessage( true ) {		// download new file
			@Override
			public String getName() {
				return "Open dialog";
			}
			@Override
			public Queue run(Mainboard dev, Queue queue) {
	        	updateBarHandler.post(new Runnable() {
                    public void run() {
        			   	if (c.isFinishing()) {
        			   		return;
        				}
                    	barProgressDialog.show();
                    }
                });
				return null;
			}
    	});
        q.addWait( 100 );
		q.add( "RESET", true );													// ret command will come 1 sec before reset
		q.addWait( barobot.state.getInt( "RESET_TIME", 200) );					// synchronize android and arduino
		q.add( new AsyncMessage( true, true ) {									// arduino was reseted but we keep going
			public void setProgress( final int value ) {
	        	updateBarHandler.post(new Runnable() {
                    public void run() {
                    	barProgressDialog.setProgress(value);
                    }
                });
			}
			@Override
			public String getName() {
				return "Upload";
			}
			private void afterReady(Mainboard dev, Queue queue) {		
		    	setProgress(80);
		    	Initiator.logger.d("onPostUpload.schedule", "now!!!");
		    	mSerial.free();
				dev.unlockRet( this, "isp burnt");			// unlock this AsyncMessage
			}
			@Override
			public Queue run(final Mainboard dev, final Queue queue) {
				Initiator.logger.i("fimwareBurn.upload","try start");
				ispUploader.setCallBack( new UploadCallBack() {	// call back is very important for error handling so do it first
			        @Override
			        public void onUploading(int value) {
			        	setProgress(value * 2/3);
			        }
			        @Override
			        public void onPreUpload() {
			        	 Initiator.logger.i("fimwareBurn.upload","Upload : Start");
			        }
			        @Override
			        public void onPostUpload(boolean success) {
			        	if(success){			// if success run after all
			                Initiator.logger.i(" fimwareBurn.upload","Upload : Successful");
			                setProgress(70);
							new Timer().schedule(new TimerTask() {
							    public void run() {
							    	setProgress(75);
							    }
							}, 1000 );// wait for first - run with new firmware
							new Timer().schedule(new TimerTask() {
							    public void run() {
							    	afterReady(dev, queue);
							    }
							}, 6000 );// wait for first - run with new firmware				                
			        	}else{
			        		setProgress(0);
				        	updateBarHandler.post(new Runnable() {
			                    public void run() {
			                    	barProgressDialog.dismiss();
			                    }
			                });
			        		Arduino.firmwareUpload	= false;
			        		mSerial.free();
			        		queue.clear();
			        		Initiator.logger.i(" fimwareBurn.upload1", "Upload fail");
			        	}
			        }
			        @Override
			        public void onError(UploadErrors err) {
						Initiator.logger.e("fimwareBurn.upload2","Error  : "+err.getDescription());
			        }
			        @Override
			        public void resetDevice(boolean reset, IspCommunicator mComm ){
			    	}
			    });
				ispUploader.setBoard( Board.ARDUINO_PRO_5V_328 );
				//boolean res = 
				ispUploader.setHex( hex_firmware_path );	// needs callback to work
				ispUploader.setSerial(mSerial);
		        try {
			        ispUploader.upload();
		        } catch (RuntimeException e) {
		        	Initiator.logger.i("prepareMB2.upload", e.toString());
		        }
				return null;
			}
		});
		q.addWait( 500 );
		q.add( "PING", "RPONG" );
		q.add( "Q00ff0000", true );			// set red
		q.addWait( 500 );
		q.add( "Q000000ff", true );			// set blue
		q.addWait( 1000 );
		q.add( new AsyncMessage( true ) {	// when version readed
			@Override
			public String getName() {
				return "Check upload";
			}
			@Override
			public Queue run(Mainboard dev, Queue queue) {
	        	updateBarHandler.post(new Runnable() {
                    public void run() {
                    	barProgressDialog.setTitle("Checking system...");
                    	barProgressDialog.setProgress(85);
                    }
                });
				return null;
			}
		});
		q.add( "Q00ffffff", true );		// set white
		barobot.readHardwareRobotId(q);
//		barobot.doHoming(q, true);
		q.add( "Q00112211", true );				// set green
		q.addWait( 500 );
		q.add( "Q00115511", true );				// set green
		q.add( new AsyncMessage( true ) {		// when version readed
			@Override
			public String getName() {
				return "Check upload";
			}
			@Override
			public Queue run(Mainboard dev, Queue queue) {
	        	updateBarHandler.post(new Runnable() {
                    public void run() {
                    	barProgressDialog.setProgress(90);
                    }
                });
	        	Initiator.logger.w("update_firmware_step3_burn.new_robot_id?", "robot_id: "+ barobot.getRobotId() +"ready: "+ barobot.robot_id_ready +" error:" + barobot.robot_id_error);

				if( barobot.robot_id_error ){
					int robot_id = UpdateManager.getNewRobotId();		// download new robot_id (init hardware)
					Initiator.logger.w("update_firmware_step3_burn.new_robot_id?", "robot_id" + robot_id);
					if( robot_id > 0 ){		// save robot_id to android and arduino
						Queue q = new Queue();
						barobot.setRobotId( q, robot_id, barobot.pcb_type);
						return q;
					}
				}
				return null;
			}
		});
		q.add( "Q00119911", true );				// set green
	//	barobot.setRobotId(q, barobot.getRobotId() );		// eeprom ic blank after burning
		barobot.readHardwareRobotId(q);
		q.add( "Q0000ff00", true );				// set green
		q.add( new AsyncMessage( true ) {		// when version readed
			@Override
			public String getName() {
				return "Check upload";
			}
			@Override
			public Queue run(Mainboard dev, Queue queue) {
	        	updateBarHandler.post(new Runnable() {
                    public void run() {
                    	barProgressDialog.setProgress(100);
                    	barProgressDialog.dismiss();
                    }
                });
				Initiator.logger.i("Check upload", "jea!" );
	        	Arduino.firmwareUpload	= false;
	        	alertOk( c );
				return null;
			}
		});	
	}

	public static void alertOk( final Activity c ) {
		BarobotMain.getInstance().runOnUiThread(new Runnable() {
			  public void run() {
				  new AlertDialog.Builder(c).setTitle("Message").setMessage("It is pleasure to inform that operation was completed successfully.")
				    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
				        public void onClick(DialogInterface dialog, int which) { 
				        }
				    })
				    .setIcon(android.R.drawable.ic_dialog_alert).show();
			  }
			});
	}
	
	
	

	
	public static void update_firmware_manual_reset(final Activity c, boolean use_beta, String url, final int pcb_type) {
		// burn it
		c.runOnUiThread(new Runnable() {
		   public void run() {
			   String msg		= c.getResources().getString(R.string.update_manual_message);
				barProgressDialog	= new ProgressDialog(c);
				barProgressDialog.setTitle(R.string.update_manual_title);
				barProgressDialog.setMessage(msg);
				barProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
				barProgressDialog.setProgress(0);
				barProgressDialog.setMax(100);
		   }
		});
		Arduino ar 						= Arduino.getInstance();
		final BarobotConnector barobot	= ar.barobot;
		final Queue q					= barobot.main_queue;
		final Wire connection			= ar.getConnection();
		final String hex_firmware_path	= getFirmwareFilePath( use_beta, pcb_type );
		Arduino.firmwareUpload			= true;

		q.clear();
		updateBarHandler.post(new Runnable() {
            public void run() {
			   	if (c.isFinishing()) {
			   		return;
				}
            	barProgressDialog.show();
            }
        });
		AsyncMessage am = new AsyncMessage( true, true ) {		// when version readed
			public void setProgress( final int value ) {
	        	updateBarHandler.post(new Runnable() {
                    public void run() {
                    	barProgressDialog.setMessage("Uploading in progress. Don't touch Barobot. Don't unplug anything before done.");
                    	barProgressDialog.setProgress(value);
                    }
                });
			}
			private void afterReady(Mainboard dev, Queue queue) {
				q.addWait( 1000 );
				q.add( "PING", "RPONG" );
				q.add( "Q00ff0000", true );		// set red
				q.addWait( 1000 );
				q.add( "Q000000ff", true );		// set blue
				q.addWait( 1000 );
				q.add( new AsyncMessage( true ) {		// when version readed
					@Override
					public String getName() {
						return "Check upload";
					}
					@Override
					public Queue run(Mainboard dev, Queue queue) {
			        	updateBarHandler.post(new Runnable() {
		                    public void run() {
		                    	barProgressDialog.setTitle("Checking system...");
		                    	barProgressDialog.setProgress(85);
		                    }
		                });
						return null;
					}
				});
				q.add( "Q00ffffff", true );		// set white
				barobot.readHardwareRobotId(q);
		//		barobot.doHoming(q, true);
				q.add( "Q00112211", true );				// set green
				q.addWait( 1000 );
				q.add( "Q00115511", true );				// set green
				q.add( new AsyncMessage( true ) {		// when version readed
					@Override
					public String getName() {
						return "Check upload";
					}
					@Override
					public Queue run(Mainboard dev, Queue queue) {
			        	updateBarHandler.post(new Runnable() {
		                    public void run() {
		                    	barProgressDialog.setProgress(90);
		                    }
		                });
						return null;
					}
				});
				q.add( "Q00119911", true );				// set green
				barobot.setRobotId(q, barobot.getRobotId(), barobot.pcb_type );		// eeprom is blank after burning
				barobot.readHardwareRobotId(q);
				q.add( "Q0000ff00", true );				// set green
				q.add( new AsyncMessage( true ) {		// when version readed
					@Override
					public String getName() {
						return "Check upload";
					}
					@Override
					public Queue run(Mainboard dev, Queue queue) {
			        	updateBarHandler.post(new Runnable() {
		                    public void run() {
		                    	barProgressDialog.setProgress(100);
		                    	barProgressDialog.dismiss();
		                    }
		                });
						Initiator.logger.i("Check upload", "jea!" );
			        	Arduino.firmwareUpload	= false;
			        	alertOk( c );
						return null;
					}
				});	
			}
			public Queue run(final Mainboard dev, final Queue queue) {
				final IspOverSerial mSerial		= new IspOverSerial(connection);
			//	final AsyncMessage msg			= this;
				Uploader  ispUploader			= new Uploader();
				ispUploader.setCallBack( new UploadCallBack() {	// call back is very important for error handling so do it first
			        @Override
			        public void onUploading(int value) {
			        	setProgress(value * 2/3);
			        }
			        @Override
			        public void onPreUpload() {
			        	 Initiator.logger.i("fimwareBurn.upload","Upload : Start");
			        }
			        @Override
			        public void onPostUpload(boolean success) {
			        	if(success){			// if success run after all
			                Initiator.logger.i(" fimwareBurn.upload","Upload : Successful");
			                setProgress(70);
							new Timer().schedule(new TimerTask() {
							    public void run() {
							    	setProgress(75);
							    }
							}, 2000 );// wait for first - run with new firmware
							new Timer().schedule(new TimerTask() {
							    public void run() {
							    	setProgress(80);
							    	Initiator.logger.w("onPostUpload.schedule", "now!!!");
							    	mSerial.free();
							    	afterReady( dev, queue );
							    }
							}, 6000 );// wait for first - run with new firmware				                
			        	}else{
			        		setProgress(0);
				        	updateBarHandler.post(new Runnable() {
			                    public void run() {
			                    	barProgressDialog.dismiss();
			                    }
			                });
			        		Arduino.firmwareUpload	= false;
			        		mSerial.free();
			        		queue.clear();
			     //   		dev.unlockRet( msg, "isp burnt");			// unlock this AsyncMessage
			        		Initiator.logger.i(" fimwareBurn.upload", "Upload fail");
				        	updateBarHandler.post(new Runnable() {
			                    public void run() {
			                    	Android.alertMessage( c, "No response. Have you reset device? You can try one more time.");
			                    }
			                });
			        	}
			        }
			        @Override
			        public void onError(UploadErrors err) {
						Initiator.logger.e("fimwareBurn.upload","Error  : "+err.getDescription());
			        }
			        @Override
			        public void resetDevice(boolean reset, IspCommunicator mComm ){
			    	}
			    });
				ispUploader.setSerial(mSerial);
				ispUploader.setBoard( Board.ARDUINO_PRO_5V_328 );
				ispUploader.setHex( hex_firmware_path );
		        try {
			        ispUploader.upload();
		        } catch (RuntimeException e) {
		        	Initiator.logger.i("prepareMB2.upload", e.toString());
		        }
				return null;
			}
		};
		am.run(null, q);
	}

	public static void downloadAndInstall(final Activity c, String url) {
		Date dNow				= new Date();
		SimpleDateFormat dd		= new SimpleDateFormat ("yyyy.MM.dd.hh.mm.ss");
		final String path6 		= Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() +"/Barobot_update-"+ dd.format(dNow)+".apk";
		//final String path6 		= Environment.getExternalStorageDirectory()+ Constant.home_path +"/update-"+ dd.format(dNow)+".apk";
		url = url.trim();
		final String sourceUrl = url;//"http://barobot.com/synchro/Barobot3.apk";//url.trim();

		Initiator.logger.i("downloadAndInstall1", ""+ url.length()+" /Path is "+url);
		//final String sourceUrl = Uri.parse(url).toString();
		c.runOnUiThread(new Runnable() {
			   public void run() {
				   	if (c.isFinishing()) {
				   		return;
					}
					updateBarHandler	= new Handler();
					barProgressDialog	= new ProgressDialog(c);
					barProgressDialog.setTitle("Downloading application from  "+ sourceUrl );
					barProgressDialog.setMessage("Downloading in progress. \n\nDestination: " + path6 );
					barProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
					barProgressDialog.setProgress(0);
					barProgressDialog.setMax(100);
					barProgressDialog.show();
			   }
		});
		InternetHelpers.doDownload( sourceUrl, path6, new OnDownloadReadyRunnable() {
				public void sendSource( String source ) {	
				}
			    @Override
				public void run() {
		        	updateBarHandler.post(new Runnable() {
	                    public void run() {
	                    	barProgressDialog.dismiss();
	                    }
	                });
			   // 	File apkFile	= new File( path6 );
					Intent intent	= new Intent(Intent.ACTION_VIEW);
				//	Initiator.logger.e("UpdateApp.run", path6);
				//	Initiator.logger.i("UpdateApp.run1", ""+apkFile );
				//	Initiator.logger.i("UpdateApp.run2", ""+Uri.parse("file:/"+apkFile) );
				//	Initiator.logger.i("UpdateApp.run3", ""+Uri.fromFile(apkFile) );
				//	Initiator.logger.i("UpdateApp.run4", ""+Uri.parse("file:/"+apkFile.getAbsolutePath()) );
				    intent.setDataAndType(Uri.fromFile(new File(path6)), "application/vnd.android.package-archive");
					intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // without this flag android returned a intent error!
					c.startActivity(intent);
					c.finish();
				}
			@Override
			public void sendProgress(final int value) {
	        	updateBarHandler.post(new Runnable() {
                    public void run() {
                    	 barProgressDialog.setProgress(value);
                    }
                });
			}
		});
	}
}

