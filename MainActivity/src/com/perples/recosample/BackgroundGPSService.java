package com.perples.recosample;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.SystemClock;
import android.telephony.TelephonyManager;
import android.text.style.SuperscriptSpan;
import android.util.Log;
import android.widget.Toast;

public class BackgroundGPSService extends Service{
	Thread thread2;
	String strUrl;
	
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// TODO Auto-generated method stub




		thread2 = new Thread(new Runnable(){
			GpsInfo gps = new GpsInfo(getApplicationContext());
			@SuppressWarnings("static-access")
			@Override
			public void run(){
				while (!thread2.interrupted()) {
					SystemClock.sleep(300);
					if (gps.isGetLocation()) {
						double latitude = gps.getLatitude();
						double longitude = gps.getLongitude();
						
						Log.d("DBG", "The response is: " + "10011,"+ latitude+","+longitude+","+HeartRateData.getter());
						new DownloadWebpageText().execute("&latitude="+latitude+"&longitude="+longitude);
					}

				}
			}  
		});

		thread2.start();

		// 서버로 전송

		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		
		thread2.interrupt();
		super.onDestroy();
	}
	int CarNumber=10011; 

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

		private String downloadUrl(String latlog) throws IOException {
			//int len = 500;
			HttpURLConnection conn = null;

			strUrl = "http://10.10.100.184:8080/d.js?";
			//http://10.10.100.184:8080/d.js?uuid=10011&latitude=10&longitude=11&heartbeat=55

			//			String addr="주소";

			strUrl = strUrl + "uuid="+CarNumber;
			
			
			
			strUrl = strUrl+latlog;

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
}
