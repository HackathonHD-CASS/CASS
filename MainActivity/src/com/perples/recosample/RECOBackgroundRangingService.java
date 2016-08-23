/**
 * The MIT License (MIT)
 * 
 * Copyright (c) 2014-2015 Perples, Inc.
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.perples.recosample;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import com.perples.recosdk.RECOBeacon;
import com.perples.recosdk.RECOBeaconManager;
import com.perples.recosdk.RECOBeaconRegion;
import com.perples.recosdk.RECOBeaconRegionState;
import com.perples.recosdk.RECOErrorCode;
import com.perples.recosdk.RECOMonitoringListener;
import com.perples.recosdk.RECORangingListener;
import com.perples.recosdk.RECOServiceConnectListener;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Geocoder;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.webkit.WebView.FindListener;
import android.widget.TextView;
import android.widget.Toast;

/**
 * RECOBackgroundRangingService is to monitor regions and range regions when the device is inside in the BACKGROUND.
 * 
 * RECOBackgroundMonitoringService는 백그라운드에서 monitoring을 수행하며, 특정 region 내부로 진입한 경우 백그라운드 상태에서 ranging을 수행합니다.
 */
public class RECOBackgroundRangingService extends Service implements RECOMonitoringListener, RECORangingListener, RECOServiceConnectListener, SensorEventListener {

	/**
	 * We recommend 1 second for scanning, 10 seconds interval between scanning, and 60 seconds for region expiration time. 
	 * 1초 스캔, 10초 간격으로 스캔, 60초의 region expiration time은 당사 권장사항입니다.
	 */
	private long mScanDuration = 1*500L;
	private long mSleepDuration = 10*1000L;
	private long mRegionExpirationTime = 60*1000L;
	private int mNotificationID = 9999;
	private String strUrl;// = "http://143.248.225.102:8080/jsp/test.jsp?gpsLat=12223.1&gpsLng=111.22&carNumber=33939&phoneNumber=333";

	private RECOBeaconManager mRecoManager;
	private ArrayList<RECOBeaconRegion> mRegions;
	private ArrayList<RECOBeacon> mRangedBeacons;

	private View mView, mView2;
	private WindowManager mManager;

	TextView mtv;
	DatagramSocket socket=null;
	private MediaPlayer mp;
	int CarNumber=0; 
	GpsInfo gps;
	boolean isMove = false;

	WindowManager.LayoutParams mParams, mParams2;

	private float mTouchX, mTouchY;
	private int mViewX, mViewY;

	int width, height;

	private SensorManager mSensorManager;
	Sensor accelerometer;
	Sensor magnetometer;

	private class DownloadWebpageText extends AsyncTask<String, String, String> {
		@Override
		protected String doInBackground(String... arg0) {
			try {
				return (String) downloadUrl((String) arg0[0]);
			} catch (IOException e) {
				Log.d("DBG", "The msg is : " + e.getMessage());
				return "download failed";
			}
		}

		protected void onPostExecute(String result) {
		}

		private String downloadUrl(String strUrl) throws IOException {
			//int len = 500;
			HttpURLConnection conn = null;

			strUrl = "http://10.10.100.184:8080/p.js?";
			//http://10.10.100.184:8080/d.js?uuid=10011&latitude=10&longitude=11&heartbeat=55

			//			String addr="주소";

			strUrl = strUrl + "uuid="+CarNumber;
			strUrl = strUrl + "&latitude="+Double.toString(HeartRateData.getlat()) + "&longitude="+Double.toString(HeartRateData.getlon());

			//	addr = getAddress(latitude, longitude);
			//	strUrl = strUrl + addr;

			strUrl = strUrl + "&heartbeat="+HeartRateData.getter();

			getApplicationContext();
			TelephonyManager telManager = (TelephonyManager)getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE); 
			String phoneNumber = telManager.getLine1Number();

			strUrl = strUrl + "&phonenumber="+phoneNumber;
			Log.d("DBG", strUrl);

			try {
				URL url = new URL(strUrl);
				conn = (HttpURLConnection) url.openConnection();
				conn.setReadTimeout(5000);
				conn.setConnectTimeout(5000);
				conn.setRequestMethod("GET");
				conn.setDoInput(true);
				conn.connect();
				int resp = conn.getResponseCode();
				Log.d("DBG", "The response is: " + resp);
				return new String("Test");
			} finally {
				conn.disconnect();
			}
		}
	}

	/** 위도와 경도 기반으로 주소를 리턴하는 메서드*/
	public String getAddress(double lat, double lng){
		String address = null;

		//위치정보를 활용하기 위한 구글 API 객체
		Geocoder geocoder = new Geocoder(this, Locale.getDefault());

		//주소 목록을 담기 위한 HashMap
		List<Address> list = null;

		try{
			list = geocoder.getFromLocation(lat, lng, 1);
		} catch(Exception e){
			e.printStackTrace();
		}

		if(list == null){
			Log.e("getAddress", "주소 데이터 얻기 실패");
			return null;
		}

		if(list.size() > 0){
			Address addr = list.get(0);
			address =// addr.getCountryName() + " "
					//+ addr.getPostalCode() + " "
					//+
					addr.getLocality() + " "
					+ addr.getThoroughfare() + " "
					+ addr.getFeatureName();
		}

		return address;

	}

	private OnTouchListener mViewTouchListener = new OnTouchListener() {

		@Override
		public boolean onTouch(View v, MotionEvent event) {

			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				isMove = false;

				mTouchX = event.getRawX();
				mTouchY = event.getRawY();
				mViewX = mParams.x;
				mViewY = mParams.y;

				break;

			case MotionEvent.ACTION_UP:
				if (!isMove) {

				}

				break;

			case MotionEvent.ACTION_MOVE:
				isMove = true;

				int x = (int) (event.getRawX() - mTouchX);
				int y = (int) (event.getRawY() - mTouchY);

				final int num = 5;
				if ((x > -num && x < num) && (y > -num && y < num)) {
					isMove = false;
					break;
				}

				mParams.x = mViewX + x;
				mParams.y = mViewY + y;

				mManager.updateViewLayout(mView, mParams);

				break;
			}

			return true;
		}
	};

	@Override
	public void onCreate() {		

		Log.i("RECOBackgroundRangingService", "onCreate()");
		super.onCreate();

		DisplayMetrics dm = getApplicationContext().getResources().getDisplayMetrics();
		width = dm.widthPixels;
		height = dm.heightPixels;

		LayoutInflater mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mView = mInflater.inflate(R.layout.on_top_view, null);
		mView2 = mInflater.inflate(R.layout.on_top_view2, null);
		mtv = (TextView)mView.findViewById(R.id.mtoastview);

		mView.setOnTouchListener(mViewTouchListener);

		mManager = (WindowManager) getSystemService(WINDOW_SERVICE);
		mParams2 = new WindowManager.LayoutParams(
				WindowManager.LayoutParams.WRAP_CONTENT,
				WindowManager.LayoutParams.WRAP_CONTENT,
				WindowManager.LayoutParams.TYPE_PHONE,
				WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
				PixelFormat.TRANSLUCENT);


		mManager.addView(mView2, mParams2);

		mParams = new WindowManager.LayoutParams(
				WindowManager.LayoutParams.WRAP_CONTENT,
				WindowManager.LayoutParams.WRAP_CONTENT,
				WindowManager.LayoutParams.TYPE_PHONE,
				WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
				PixelFormat.TRANSLUCENT);
		//mParams.gravity = Gravity.BOTTOM | Gravity.RIGHT;
		mManager.addView(mView, mParams);


		//시스템으로부터 센서 메니저 객체 얻어오기
		mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
		accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
		mSensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME);
	}


	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i("RECOBackgroundRangingService", "onStartCommand");
		mp = MediaPlayer.create(this, R.raw.alarm);

		gps = new GpsInfo(RECOBackgroundRangingService.this);
		try {
			socket = new DatagramSocket(8888);
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}




		/**
		 * Create an instance of RECOBeaconManager (to set scanning target and ranging timeout in the background.)
		 * If you want to scan only RECO, and do not set ranging timeout in the backgournd, create an instance: 
		 * 		mRecoManager = RECOBeaconManager.getInstance(getApplicationContext(), true, false);
		 * WARNING: False enableRangingTimeout will affect the battery consumption.
		 * 
		 * RECOBeaconManager 인스턴스틀 생성합니다. (스캔 대상 및 백그라운드 ranging timeout 설정)
		 * RECO만을 스캔하고, 백그라운드 ranging timeout을 설정하고 싶지 않으시다면, 다음과 같이 생성하시기 바랍니다.
		 * 		mRecoManager = RECOBeaconManager.getInstance(getApplicationContext(), true, false); 
		 * 주의: enableRangingTimeout을 false로 설정 시, 배터리 소모량이 증가합니다.
		 */
		mRecoManager = RECOBeaconManager.getInstance(getApplicationContext(), MainActivity.SCAN_RECO_ONLY, false);//MainActivity.ENABLE_BACKGROUND_RANGING_TIMEOUT);
		this.bindRECOService();
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		mSensorManager.unregisterListener(this);
		if (mView != null) {
			mManager.removeView(mView);
			mView = null;
		}
		if (mView2 != null) {
			mManager.removeView(mView2);
			mView2 = null;
		}
		socket.close();
		Log.i("RECOBackgroundRangingService", "onDestroy()");
		this.tearDown();
		this.popupNotification("RARP terminated!");
		super.onDestroy();
	}

	@Override
	public void onTaskRemoved(Intent rootIntent) {
		Log.i("RECOBackgroundRangingService", "onTaskRemoved()");
		super.onTaskRemoved(rootIntent);
	}

	private void bindRECOService() {
		Log.i("RECOBackgroundRangingService", "bindRECOService()");

		mRegions = new ArrayList<RECOBeaconRegion>();
		this.generateBeaconRegion();

		mRecoManager.setMonitoringListener(this);
		mRecoManager.setRangingListener(this);
		mRecoManager.bind(this);
	}

	private void generateBeaconRegion() {
		Log.i("RECOBackgroundRangingService", "generateBeaconRegion()");

		RECOBeaconRegion recoRegion;

		recoRegion = new RECOBeaconRegion(MainActivity.RECO_UUID, "RECO Sample Region");
		recoRegion.setRegionExpirationTimeMillis(this.mRegionExpirationTime);
		mRegions.add(recoRegion);
	}

	private void startMonitoring() {
		Log.i("RECOBackgroundRangingService", "startMonitoring()");

		mRecoManager.setScanPeriod(this.mScanDuration);
		mRecoManager.setSleepPeriod(this.mSleepDuration);

		for(RECOBeaconRegion region : mRegions) {
			try {
				mRecoManager.startMonitoringForRegion(region);
			} catch (RemoteException e) {
				Log.e("RECOBackgroundRangingService", "RemoteException has occured while executing RECOManager.startMonitoringForRegion()");
				e.printStackTrace();
			} catch (NullPointerException e) {
				Log.e("RECOBackgroundRangingService", "NullPointerException has occured while executing RECOManager.startMonitoringForRegion()");
				e.printStackTrace();
			}
		}
	}

	private void stopMonitoring() {
		Log.i("RECOBackgroundRangingService", "stopMonitoring()");

		for(RECOBeaconRegion region : mRegions) {
			try {
				mRecoManager.stopMonitoringForRegion(region);
			} catch (RemoteException e) {
				Log.e("RECOBackgroundRangingService", "RemoteException has occured while executing RECOManager.stopMonitoringForRegion()");
				e.printStackTrace();
			} catch (NullPointerException e) {
				Log.e("RECOBackgroundRangingService", "NullPointerException has occured while executing RECOManager.stopMonitoringForRegion()");
				e.printStackTrace();
			}
		}
	}

	private void startRangingWithRegion(RECOBeaconRegion region) {
		Log.i("RECOBackgroundRangingService", "startRangingWithRegion()");

		/**
		 * There is a known android bug that some android devices scan BLE devices only once. (link: http://code.google.com/p/android/issues/detail?id=65863)
		 * To resolve the bug in our SDK, you can use setDiscontinuousScan() method of the RECOBeaconManager. 
		 * This method is to set whether the device scans BLE devices continuously or discontinuously. 
		 * The default is set as FALSE. Please set TRUE only for specific devices.
		 * 
		 * mRecoManager.setDiscontinuousScan(true);
		 */

		try {
			mRecoManager.startRangingBeaconsInRegion(region);
		} catch (RemoteException e) {
			Log.e("RECOBackgroundRangingService", "RemoteException has occured while executing RECOManager.startRangingBeaconsInRegion()");
			e.printStackTrace();
		} catch (NullPointerException e) {
			Log.e("RECOBackgroundRangingService", "NullPointerException has occured while executing RECOManager.startRangingBeaconsInRegion()");
			e.printStackTrace();
		}
	}

	private void stopRangingWithRegion(RECOBeaconRegion region) {
		Log.i("RECOBackgroundRangingService", "stopRangingWithRegion()");

		try {
			mRecoManager.stopRangingBeaconsInRegion(region);
		} catch (RemoteException e) {
			Log.e("RECOBackgroundRangingService", "RemoteException has occured while executing RECOManager.stopRangingBeaconsInRegion()");
			e.printStackTrace();
		} catch (NullPointerException e) {
			Log.e("RECOBackgroundRangingService", "NullPointerException has occured while executing RECOManager.stopRangingBeaconsInRegion()");
			e.printStackTrace();
		}
	}

	private void tearDown() {
		Log.i("RECOBackgroundRangingService", "tearDown()");
		this.stopMonitoring();

		try {
			mRecoManager.unbind();
		} catch (RemoteException e) {
			Log.e("RECOBackgroundRangingService", "RemoteException has occured while executing unbind()");
			e.printStackTrace();
		}
	}

	@Override
	public void onServiceConnect() {
		Log.i("RECOBackgroundRangingService", "onServiceConnect()");
		this.startMonitoring();
		//Write the code when RECOBeaconManager is bound to RECOBeaconService
	}
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public void didDetermineStateForRegion(RECOBeaconRegionState state, RECOBeaconRegion region) {
		Log.i("RECOBackgroundRangingService", "didDetermineStateForRegion()");
		Log.i("RECOBackgroundRangingService", state.toString());
		//Write the code when the state of the monitored region is changed
		if(state.toString().equals("RECOBeaconRegionInside")){
			didEnterRegion(region, null);
		}else if(state.toString().equals("RECOBeaconRegionOutside")){
			didExitRegion(region);
		}
	}

	@Override
	public void didEnterRegion(RECOBeaconRegion region, Collection<RECOBeacon> beacons) {
		/**
		 * For the first run, this callback method will not be called. 
		 * Please check the state of the region using didDetermineStateForRegion() callback method.
		 * 
		 * 최초 실행시, 이 콜백 메소드는 호출되지 않습니다. 
		 * didDetermineStateForRegion() 콜백 메소드를 통해 region 상태를 확인할 수 있습니다.
		 */

		//Get the region and found beacon list in the entered region
		Log.i("RECOBackgroundRangingService", "didEnterRegion() - " + region.getUniqueIdentifier());
		this.popupNotification("RARP Start.....!");
		//Write the code when the device is enter the region

		this.startRangingWithRegion(region); //start ranging to get beacons inside of the region
		//from now, stop ranging after 10 seconds if the device is not exited

	}

	@Override
	public void didExitRegion(RECOBeaconRegion region) {
		/**
		 * For the first run, this callback method will not be called. 
		 * Please check the state of the region using didDetermineStateForRegion() callback method.
		 * 
		 * 최초 실행시, 이 콜백 메소드는 호출되지 않습니다. 
		 * didDetermineStateForRegion() 콜백 메소드를 통해 region 상태를 확인할 수 있습니다.
		 */

		Log.i("RECOBackgroundRangingService", "didExitRegion() - " + region.getUniqueIdentifier());
		//this.popupNotification("Outside of " + region.getUniqueIdentifier());
		//this.popupNotification("안전이어폰 종료");
		//Write the code when the device is exit the region

		this.stopRangingWithRegion(region); //stop ranging because the device is outside of the region from now
	}
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public void didStartMonitoringForRegion(RECOBeaconRegion region) {
		Log.i("RECOBackgroundRangingService", "didStartMonitoringForRegion() - " + region.getUniqueIdentifier());
		//Write the code when starting monitoring the region is started successfully
		//didEnterRegion(region, null);
	}
	int degree = 0;
	int nearbeacon = 0;
	String nearAlarm = "";
	@Override
	public void didRangeBeaconsInRegion(Collection<RECOBeacon> beacons, RECOBeaconRegion region) {
		Log.i("RECOBackgroundRangingService", "didRangeBeaconsInRegion() - " + region.getUniqueIdentifier() + " with " + beacons.size() + " beacons");
		//Write the code when the beacons inside of the region is received


		if (gps.isGetLocation()) {

			HeartRateData hrd = new HeartRateData();
			hrd.setlat(gps.getLatitude());
			hrd.setlon(gps.getLongitude());

		}

		mRangedBeacons = new ArrayList<RECOBeacon>(beacons);
		RECOBeacon recoBeacon;
		nearAlarm = "현재방위="+mAzimuth+"도, 차량방위="+degree+"\n";


		nearbeacon=0;

		for(int position=0;position<beacons.size();position++){
			recoBeacon = mRangedBeacons.get(position);


			if(recoBeacon.getAccuracy()<5){ //멀리서 접근(서버에서 판단을 요청)	
				nearbeacon++;
				//Toast.makeText(RECOBackgroundRangingService.this,  recoBeacon.getMinor()+","+recoBeacon.getAccuracy(), Toast.LENGTH_SHORT).show();
				//Toast.makeText(RECOBackgroundRangingService.this,  recoBeacon.getMinor()+" 차량이"+Math.round((recoBeacon.getAccuracy()*100))/100.0+"m밖 접근중", Toast.LENGTH_SHORT).show();
				if(position==1)
					nearAlarm += "\n";
				nearAlarm += ""+recoBeacon.getMinor()+" 차량 "+Math.round((recoBeacon.getAccuracy()*100))/100.0+"m 거리 위치";
				Log.i("RECOBackgroundRangingService",  recoBeacon.getMinor()+","+recoBeacon.getAccuracy());
				//서버로 보냄

			}

			if(recoBeacon.getAccuracy()<3&&recoBeacon.getMinor()!=10019){ //근접



				CarNumber = recoBeacon.getMinor();
				//Log.i("RECOBackgroundRangingService", recoBeacon.getMinor()+","+position);


				// 서버로 전송
				new DownloadWebpageText().execute(strUrl);

				//서버 응답 대기

				if(!gps.isWaiting){
					gps.isWaiting=true;
					new Thread(new Runnable(){
						public void run(){
							byte[] buf = new byte[10];
							DatagramPacket packet = new DatagramPacket(buf,buf.length);
							try {
								socket.receive(packet);
								if(packet.getLength()>1){
									String s = new String(packet.getData(),0,packet.getLength()); 
									degree = Integer.parseInt(s);
									if(degree<0){
										//degree = -degree;
										mp.start();
									}
								}

							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							gps.isWaiting=false;
						}
					}).start();
				}
				
			}

		}
		

		if(nearbeacon==0||degree>=0)
			mView2.setVisibility(View.INVISIBLE);
		else{
			degree=-degree;
			int a = (degree-mAzimuth+360)%360;
			if(a>342.5&&a<=22.5){				//N
				mParams2.x = 0;
				mParams2.y = -height/2;
			}else if(a>22.5&&a<=67.5){		//NE
				mParams2.x = width/2;
				mParams2.y = -height/2;
			}else if(a>67.5&&a<=112.5){		//E
				mParams2.x = width/2;
				mParams2.y = 0;
			}else if(a>112.5&&a<=157.5){		//SE
				mParams2.x = width/2;
				mParams2.y = height/2;
			}else if(a>157.5&&a<=202.5){		//S
				mParams2.x = 0;
				mParams2.y = height/2;
			}else if(a>202.5&&a<=247.5){		//SW
				mParams2.x = -width/2;
				mParams2.y = height/2;
			}else if(a>247.5&&a<=302.5){		//W
				mParams2.x = -width/2;
				mParams2.y = 0;
			}else{									//NW
				mParams2.x = -width/2;
				mParams2.y = -height/2;
			}

			mView2.setVisibility(View.VISIBLE);
			mManager.updateViewLayout(mView2, mParams2);
			//mView2.setVisibility(View.invisible);
			//mView2.setVisibility(View.visible);
		}
		
		mtv.setText(nearAlarm);
		//Toast.makeText(RECOBackgroundRangingService.this,  nearAlarm, Toast.LENGTH_SHORT).show();
	}

	private void popupNotification(String msg) {
		Log.i("RECOBackgroundRangingService", "popupNotification()");
		String currentTime = new SimpleDateFormat("HH:mm:ss", Locale.KOREA).format(new Date());
		NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		NotificationCompat.Builder builder = new NotificationCompat.Builder(this).setSmallIcon(R.drawable.ic_launcher)
				.setContentTitle(msg)
				.setContentText(currentTime);

		NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
		builder.setStyle(inboxStyle);
		nm.notify(mNotificationID, builder.build());
		mNotificationID = (mNotificationID - 1) % 1000 + 9000;
	}

	@Override
	public IBinder onBind(Intent intent) {
		//This method is not used
		return null;
	}

	@Override
	public void onServiceFail(RECOErrorCode errorCode) {
		//Write the code when the RECOBeaconService is failed.
		//See the RECOErrorCode in the documents.
		return;
	}

	@Override
	public void monitoringDidFailForRegion(RECOBeaconRegion region, RECOErrorCode errorCode) {
		//Write the code when the RECOBeaconService is failed to monitor the region.
		//See the RECOErrorCode in the documents.
		return;
	}

	@Override
	public void rangingBeaconsDidFailForRegion(RECOBeaconRegion region, RECOErrorCode errorCode) {
		//Write the code when the RECOBeaconService is failed to range beacons in the region.
		//See the RECOErrorCode in the documents.
		return;
	}

	public void httpRequestGet() throws Exception{
		HttpClient client = new DefaultHttpClient();
		String url = "http://10.10.100.184:8080/d.js?uuid=10011&latitude=10&longitude=11&heartbeat=55";

		HttpGet get = new HttpGet(url);
		HttpResponse response = client.execute(get);
		HttpEntity resEntity = response.getEntity();
		if(resEntity != null){
			String res = EntityUtils.toString(resEntity);
		}
	}


	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {
		// TODO Auto-generated method stub

	}

	float[] gData = new float[3]; // accelerometer
	float[] mData = new float[3]; // magnetometer
	float[] rMat = new float[9];
	float[] iMat = new float[9];
	float[] orientation = new float[3];
	int mAzimuth = 0;
	@Override
	public void onSensorChanged(SensorEvent event) {
		// TODO Auto-generated method stub

		float[] data;
		switch ( event.sensor.getType() ) {
		case Sensor.TYPE_ACCELEROMETER:
			gData = event.values.clone();
			break;
		case Sensor.TYPE_MAGNETIC_FIELD:
			mData = event.values.clone();
			break;
		default: return;
		}

		if ( SensorManager.getRotationMatrix( rMat, iMat, gData, mData ) ) {
			mAzimuth= (int) ( Math.toDegrees( SensorManager.getOrientation( rMat, orientation )[0] ) + 360 ) % 360;
		}
		//mtv.setText(""+mAzimuth); // orientation contains: azimut, pitch and roll



		//Toast.makeText(RECOBackgroundRangingService.this,  ""+event.values[0]+","+event.values[1]+","+event.values[2], Toast.LENGTH_SHORT).show();


	}
}
