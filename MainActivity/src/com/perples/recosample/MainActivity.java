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

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ToggleButton;

public class MainActivity extends Activity {
	//This is a default proximity uuid of the RECO
	public static final String RECO_UUID = "24DDF411-8CF1-440C-87CD-E368DAF9C93E";

	/**
	 * SCAN_RECO_ONLY:
	 * 
	 * If true, the application scans RECO beacons only, otherwise it scans all beacons.
	 * It will be used when the instance of RECOBeaconManager is created.
	 * 
	 * true일 경우 레코 비콘만 스캔하며, false일 경우 모든 비콘을 스캔합니다.
	 * RECOBeaconManager 객체 생성 시 사용합니다.
	 */
	public static final boolean SCAN_RECO_ONLY = true;

	/**
	 * ENABLE_BACKGROUND_RANGING_TIMEOUT:
	 * 
	 * If true, the application stops to range beacons in the entered region automatically in 10 seconds (background),
	 * otherwise it continues to range beacons. (It affects the battery consumption.)
	 * It will be used when the instance of RECOBeaconManager is created.
	 * 
	 * 백그라운드 ranging timeout을 설정합니다. 
	 * true일 경우, 백그라운드에서 입장한 region에서 ranging이 실행 되었을 때, 10초 후 자동으로 정지합니다.
	 * false일 경우, 계속 ranging을 실행합니다. (배터리 소모율에 영향을 끼칩니다.)
	 * RECOBeaconManager 객체 생성 시 사용합니다.
	 */
	public static final boolean ENABLE_BACKGROUND_RANGING_TIMEOUT = true;

	/**
	 * DISCONTINUOUS_SCAN:
	 * 
	 * There is a known android bug that some android devices scan BLE devices only once. 
	 * (link: http://code.google.com/p/android/issues/detail?id=65863)
	 * To resolve the bug in our SDK, you can use setDiscontinuousScan() method of the RECOBeaconManager. 
	 * This method is to set whether the device scans BLE devices continuously or discontinuously. 
	 * The default is set as FALSE. Please set TRUE only for specific devices.
	 * 
	 * 일부 안드로이드 기기에서 BLE 장치들을 스캔할 때, 한 번만 스캔 후 스캔하지 않는 버그(참고: http://code.google.com/p/android/issues/detail?id=65863)가 있습니다. 
	 * 해당 버그를 SDK에서 해결하기 위해, RECOBeaconManager에 setDiscontinuousScan() 메소드를 이용할 수 있습니다. 
	 * 해당 메소드는 기기에서 BLE 장치들을 스캔할 때(즉, ranging 시에), 연속적으로 계속 스캔할 것인지, 불연속적으로 스캔할 것인지 설정하는 것입니다. 
	 * 기본 값은 FALSE로 설정되어 있으며, 특정 장치에 대해 TRUE로 설정하시길 권장합니다.
	 */
	public static final boolean DISCONTINUOUS_SCAN = false;

	private static final int REQUEST_ENABLE_BT = 1;

	private BluetoothManager mBluetoothManager;
	private BluetoothAdapter mBluetoothAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		//Intent intent = new Intent(MainActivity.this, DemoActivity.class);
		//startActivity(intent); 
		
		
		//If a user device turns off bluetooth, request to turn it on.
		//사용자가 블루투스를 켜도록 요청합니다.
		mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = mBluetoothManager.getAdapter();

		if(mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
			Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBTIntent, REQUEST_ENABLE_BT);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
			//If the request to turn on bluetooth is denied, the app will be finished.
			//사용자가 블루투스 요청을 허용하지 않았을 경우, 어플리케이션은 종료됩니다.
			finish();
			return;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	protected void onResume() {
		Log.i("MainActivity", "onResume()");
		super.onResume();

		if(this.isBackgroundMonitoringServiceRunning(this)) {
			ToggleButton toggle = (ToggleButton)findViewById(R.id.backgroundMonitoringToggleButton);
			toggle.setChecked(true);
		}

		if(this.isBackgroundRangingServiceRunning(this)) {
			ToggleButton toggle = (ToggleButton)findViewById(R.id.backgroundRangingToggleButton);
			toggle.setChecked(true);
		}
		
		if(this.isBackgroundGPSServiceRunning(this)) {
			ToggleButton toggle = (ToggleButton)findViewById(R.id.carGPSmoniteringToggleButton);
			toggle.setChecked(true);
		}
	}

	@Override
	protected void onDestroy() {
		Log.i("MainActivity", "onDestroy");
		super.onDestroy();
	}

	public void onMonitoringToggleButtonClicked(View v) {
		ToggleButton toggle = (ToggleButton)v;
		if(toggle.isChecked()) {
			Log.i("MainActivity", "onMonitoringToggleButtonClicked off to on");
			//Intent intent = new Intent(this, RECOBackgroundMonitoringService.class);
			Intent intent = new Intent(this, HeartService.class);
			startService(intent);
		} else {
			Log.i("MainActivity", "onMonitoringToggleButtonClicked on to off");
			stopService(new Intent(this, HeartService.class));
		}
	}

	public void onRangingToggleButtonClicked(View v) {
		if(v.getId()==R.id.backgroundRangingToggleButton){
			ToggleButton toggle = (ToggleButton)v;
			if(toggle.isChecked()) {
				Log.i("MainActivity", "onRangingToggleButtonClicked off to on");
				Intent intent = new Intent(this, RECOBackgroundRangingService.class);
				startService(intent);
			} else {
				Log.i("MainActivity", "onRangingToggleButtonClicked on to off");
				stopService(new Intent(this, RECOBackgroundRangingService.class));
			}
		}else{
			ToggleButton toggle = (ToggleButton)v;
			if(toggle.isChecked()) {
				Log.i("MainActivity", "onRangingToggleButtonClicked off to on");
				Intent intent = new Intent(this, BackgroundGPSService.class);
				startService(intent);
			} else {
				Log.i("MainActivity", "onRangingToggleButtonClicked on to off");
				stopService(new Intent(this, BackgroundGPSService.class));
			}
		}
	}


	public void onButtonClicked(View v) {
		Button btn = (Button)v;
		if(btn.getId() == R.id.monitoringButton) {
			//final Intent intent = new Intent(this, DemoActivity.class);
			final Intent intent = new Intent(this, SMSsendActivity.class);
			startActivity(intent);
		} else {
			final Intent intent = new Intent(this, HeartActivity.class);
			startActivity(intent);
		}
	}

	private boolean isBackgroundMonitoringServiceRunning(Context context) {
		ActivityManager am = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
		for(RunningServiceInfo runningService : am.getRunningServices(Integer.MAX_VALUE)) {
			if(HeartService.class.getName().equals(runningService.service.getClassName())) {
				return true;
			}
		}
		return false;
	}

	private boolean isBackgroundRangingServiceRunning(Context context) {
		ActivityManager am = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
		for(RunningServiceInfo runningService : am.getRunningServices(Integer.MAX_VALUE)) {
			if(RECOBackgroundRangingService.class.getName().equals(runningService.service.getClassName())) {
				return true;
			}
		}
		return false;
	}
	
	private boolean isBackgroundGPSServiceRunning(Context context) {
		ActivityManager am = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
		for(RunningServiceInfo runningService : am.getRunningServices(Integer.MAX_VALUE)) {
			if(BackgroundGPSService.class.getName().equals(runningService.service.getClassName())) {
				return true;
			}
		}
		return false;
	}

}
