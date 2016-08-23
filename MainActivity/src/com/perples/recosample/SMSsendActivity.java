package com.perples.recosample;

import java.util.List;
import java.util.Locale;

import android.app.ActionBar;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class SMSsendActivity extends Activity {

	private GpsInfo gps;
	private EditText et;
	private String smsNumber= "01030099369";
	private String smsContents= "사고발생!!\n";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ActionBar ab = getActionBar(); 
		ab.setTitle("괜찮으세요??");
		setContentView(R.layout.userinput);
		et = (EditText)findViewById(R.id.userinputText);
		Button sendBtn = (Button) findViewById(R.id.sendbutton1);
		Button closeBtn = (Button) findViewById(R.id.closebutton1);
		sendBtn.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {

				String addr="주소";
				// TODO Auto-generated method stub
				try {

					gps = new GpsInfo(SMSsendActivity.this);
					// GPS 사용유무 가져오기
					if (gps.isGetLocation()) {

						double latitude = gps.getLatitude();
						double longitude = gps.getLongitude();

						addr = getAddress(latitude, longitude);
					}

					sendSMS(smsNumber, smsContents+ addr + "\n교통사고 발생!\n"+et.getText().toString());


				} catch (Exception e) {
					Toast.makeText(SMSsendActivity.this, e.toString(), Toast.LENGTH_LONG).show();
					e.printStackTrace();
				}
			}
		});
		closeBtn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				finish();
			}
		});
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


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.userinput, menu);
		return true;
	}

	public void sendSMS(String phoneNumber, String message) {        
		String SENT = "SMS_SENT";
		String DELIVERED = "SMS_DELIVERED";

		PendingIntent sentPI = PendingIntent.getBroadcast(this, 0,
				new Intent(SENT), 0);

		PendingIntent deliveredPI = PendingIntent.getBroadcast(this, 0,
				new Intent(DELIVERED), 0);

		//---when the SMS has been sent---
		registerReceiver(new BroadcastReceiver(){

			@Override
			public void onReceive(Context context, Intent intent) {
				// TODO Auto-generated method stub
				switch (getResultCode())
				{
				case Activity.RESULT_OK:
					Toast.makeText(getBaseContext(), "SMS 발송 완료", Toast.LENGTH_SHORT).show();
					break;
				case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
					Toast.makeText(getBaseContext(), "Generic failure", Toast.LENGTH_SHORT).show();
					break;
				case SmsManager.RESULT_ERROR_NO_SERVICE:
					Toast.makeText(getBaseContext(), "No service", Toast.LENGTH_SHORT).show();
					break;
				case SmsManager.RESULT_ERROR_NULL_PDU:
					Toast.makeText(getBaseContext(), "Null PDU", Toast.LENGTH_SHORT).show();
					break;
				case SmsManager.RESULT_ERROR_RADIO_OFF:
					Toast.makeText(getBaseContext(), "Radio off", Toast.LENGTH_SHORT).show();
					break;
				}
			}
		}, new IntentFilter(SENT));

		//---when the SMS has been delivered---
		registerReceiver(new BroadcastReceiver(){
			@Override
			public void onReceive(Context arg0, Intent arg1) {
				switch (getResultCode())
				{
				case Activity.RESULT_OK:
					Toast.makeText(getBaseContext(), "SMS delivered", 
							Toast.LENGTH_SHORT).show();
					break;
				case Activity.RESULT_CANCELED:
					Toast.makeText(getBaseContext(), "SMS not delivered", 
							Toast.LENGTH_SHORT).show();
					break;                        
				}
			}
		}, new IntentFilter(DELIVERED));        

		SmsManager sms = SmsManager.getDefault();
		sms.sendTextMessage(phoneNumber, null, message, sentPI, deliveredPI);        
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
