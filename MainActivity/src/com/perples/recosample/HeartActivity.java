package com.perples.recosample;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;


public class HeartActivity extends Activity {
	TextView hr;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.heart);
		hr = (TextView)findViewById(R.id.hr);

		if(!this.isBackgroundRangingServiceRunning(this)) {
			hr.setText("Please On Heartrate Monitoring Service");
		}else{


			Thread myThread = new Thread(new Runnable(){
				public void run(){
					while (true){
						try {
							handler.sendMessage(handler.obtainMessage());
							Thread.sleep(1000);
						} catch (Throwable t) {
						}

					}
				}

			});
			myThread.start();
		}
	}

	Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			updateThread();
		}

		private void updateThread() {
			hr.setText("HEART RATE \n " + HeartRateData.getter()+"");
		}

	};

	private boolean isBackgroundRangingServiceRunning(Context context) {
		ActivityManager am = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
		for(RunningServiceInfo runningService : am.getRunningServices(Integer.MAX_VALUE)) {
			if(HeartService.class.getName().equals(runningService.service.getClassName())) {
				return true;
			}
		}
		return false;
	}
}
/*
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

public class HeartActivity extends Activity {

	int format;

	private BluetoothAdapter mBluetoothAdapter;
	private int REQUEST_ENABLE_BT = 1;

	private static final long SCAN_PERIOD = 10000;
	private BluetoothLeScanner mLEScanner;
	private ScanSettings settings;
	private List<ScanFilter> filters;
	private BluetoothGatt mGatt;

	TextView hr;
	Object value;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.heart);
		hr = (TextView)findViewById(R.id.hr);

		if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			Toast.makeText(this, "BLE Not Supported",
					Toast.LENGTH_SHORT).show();
			finish();
		}
		final BluetoothManager bluetoothManager =
				(BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = bluetoothManager.getAdapter();

	}

	Handler mHandler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			//super.handleMessage(msg);
			updateHR();
		}
	};

	protected void updateHR() {
		// TODO Auto-generated method stub
		hr.setText("HeartRate : "+value.toString());
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		} else {
			if (Build.VERSION.SDK_INT >= 21) {
				mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
				settings = new ScanSettings.Builder()
				.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
				.build();
				filters = new ArrayList<ScanFilter>();
			}
			scanLeDevice(true);
		}
	}



	@Override
	protected void onPause() {
		super.onPause();
		if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
			scanLeDevice(false);
		}
	}

	@Override
	protected void onDestroy() {
		if (mGatt == null) {
			return;
		}
		mGatt.close();
		mGatt = null;
		super.onDestroy();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_ENABLE_BT) {
			if (resultCode == Activity.RESULT_CANCELED) {
				//Bluetooth not enabled.
				finish();
				return;
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@SuppressWarnings("deprecation")
	private void scanLeDevice(final boolean enable) {
		if (enable) {
			mHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					mBluetoothAdapter.stopLeScan(mLeScanCallback);
				}
			}, SCAN_PERIOD);
			mBluetoothAdapter.startLeScan(mLeScanCallback);
		} else {
			mBluetoothAdapter.stopLeScan(mLeScanCallback);
		}
	}



	private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

		@Override
		public void onLeScan(final BluetoothDevice device, int rssi,
				byte[] scanRecord) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Log.i("onLeScan", device.toString());
					if(device.toString().equals("EE:BA:36:B4:CF:CF"))
						connectToDevice(device);
				}
			});
		}
	};

	public void connectToDevice(BluetoothDevice device) {
		if (mGatt == null) {
			mGatt = device.connectGatt(this, false, gattCallback);
			scanLeDevice(false);// will stop after first device detection
		}
	}

	private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
			Log.i("onConnectionStateChange", "Status: " + status);
			switch (newState) {
			case BluetoothProfile.STATE_CONNECTED:
				Log.i("gattCallback", "STATE_CONNECTED");
				gatt.discoverServices();
				break;
			case BluetoothProfile.STATE_DISCONNECTED:
				Log.e("gattCallback", "STATE_DISCONNECTED");
				break;
			default:
				Log.e("gattCallback", "STATE_OTHER");
			}

		}

		//		@Override
		//		public void onServicesDiscovered(BluetoothGatt gatt, int status) {
		//			List<BluetoothGattService> services = gatt.getServices();
		//			Log.i("onServicesDiscovered", services.toString());
		//			gatt.readCharacteristic(services.get(0).getCharacteristics().get(0));
		//			//gatt.readCharacteristic(services.get(0).getCharacteristics().get	(0));
		//		}

		public void onServicesDiscovered(BluetoothGatt gatt, int status) {
			Log.i("gattCallback", "onServicesDiscovered : "+ status);
			switch (status) {
			case BluetoothGatt.GATT_SUCCESS: // GATT_SERVICES_DISCOVERED

				//  Find services which we want to manage

				for (BluetoothServiceType bluetoothServiceType : BluetoothServiceType.values()) {
					Log.i("gattCallback", ""+BluetoothServiceType.values());
					BluetoothGattService bluetoothGattService = gatt.getService(bluetoothServiceType.getServiceUuid());
					if (bluetoothGattService == null) {
						return;
					}

					BluetoothGattCharacteristic bluetoothGattCharacteristic = bluetoothGattService
							.getCharacteristic(bluetoothServiceType
									.getCharacteristicUuid());

					if (bluetoothGattCharacteristic == null) {
						return;
					}

					int prop = bluetoothGattCharacteristic.getProperties();

					if ((prop & 0x01) != 0) {
						format = BluetoothGattCharacteristic.FORMAT_UINT16;
					} else {
						format = BluetoothGattCharacteristic.FORMAT_UINT8;
					}

					if ((prop | android.bluetooth.BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
						gatt.readCharacteristic(bluetoothGattCharacteristic);
					}
					if ((prop | android.bluetooth.BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
						BluetoothGattDescriptor descriptor = bluetoothGattCharacteristic
								.getDescriptor(bluetoothServiceType
										.getDescriptorUuid());
						descriptor
						.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
						gatt.writeDescriptor(descriptor);

						gatt.setCharacteristicNotification(
								bluetoothGattCharacteristic, true);

						break;
					}
				}
			}
		}


		//		@Override
		//		public void onCharacteristicRead(BluetoothGatt gatt,
		//				BluetoothGattCharacteristic
		//				characteristic, int status) {
		//			Log.i("onCharacteristicRead", characteristic.toString());
		//			gatt.disconnect();
		//		}
		//	};


		@Override
		public void onCharacteristicRead(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic, int status) {
			super.onCharacteristicRead(gatt, characteristic, status);
			Toast.makeText(getApplicationContext(), "onCharacteristicRead", Toast.LENGTH_SHORT).show();
			onCharacteristicUpdated(gatt, characteristic);
		}
		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt,
				android.bluetooth.BluetoothGattCharacteristic characteristic) {
			super.onCharacteristicChanged(gatt, characteristic);

			onCharacteristicUpdated(gatt, characteristic);
		}


		public void onCharacteristicUpdated(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic) {

			for (BluetoothServiceType btServiceType : BluetoothServiceType
					.values()) {
				if (btServiceType.getCharacteristicUuid().equals(characteristic.getUuid())) {

					switch (format) {
					case BluetoothGattCharacteristic.FORMAT_SINT8:
					case BluetoothGattCharacteristic.FORMAT_SINT16:
					case BluetoothGattCharacteristic.FORMAT_SINT32:
					case BluetoothGattCharacteristic.FORMAT_UINT8:
					case BluetoothGattCharacteristic.FORMAT_UINT16:
					case BluetoothGattCharacteristic.FORMAT_UINT32:
						value = characteristic.getIntValue(format, 1);
						mHandler.sendMessage(mHandler.obtainMessage());
						Log.i("onCharacteristicRead", value.toString());
						break;
					case BluetoothGattCharacteristic.FORMAT_FLOAT:
					case BluetoothGattCharacteristic.FORMAT_SFLOAT:
						// Float value = characteristic.getFloatValue(format,
						// ?);
						break;
					}
				}
			}
			//gatt.disconnect();
		}

		private void broadcastUpdate(final String action) {
		    final Intent intent = new Intent(action);
		    sendBroadcast(intent);
		}

//		private void broadcastUpdate(final String action,
//		                             final BluetoothGattCharacteristic characteristic) {
//		    final Intent intent = new Intent(action);
//
//		    // This is special handling for the Heart Rate Measurement profile. Data
//		    // parsing is carried out as per profile specifications.
//		    if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
//		        int flag = characteristic.getProperties();
//		        int format = -1;
//		        if ((flag & 0x01) != 0) {
//		            format = BluetoothGattCharacteristic.FORMAT_UINT16;
//		            Log.d("broadcastupdate", "Heart rate format UINT16.");
//		        } else {
//		            format = BluetoothGattCharacteristic.FORMAT_UINT8;
//		            Log.d("broadcastupdate", "Heart rate format UINT8.");
//		        }
//		        final int heartRate = characteristic.getIntValue(format, 1);
//		        Log.d("broadcastupdate", String.format("Received heart rate: %d", heartRate));
//		        intent.putExtra(EXTRA_DATA, String.valueOf(heartRate));
//		    } else {
//		        // For all other profiles, writes the data formatted in HEX.
//		        final byte[] data = characteristic.getValue();
//		        if (data != null && data.length > 0) {
//		            final StringBuilder stringBuilder = new StringBuilder(data.length);
//		            for(byte byteChar : data)
//		                stringBuilder.append(String.format("%02X ", byteChar));
//		            intent.putExtra(EXTRA_DATA, new String(data) + "\n" +
//		                    stringBuilder.toString());
//		        }
//		    }
//		    sendBroadcast(intent);
//		}

	};

		@Override
		public boolean onCreateOptionsMenu(Menu menu) {
			// Inflate the menu; this adds items to the action bar if it is present.
			getMenuInflater().inflate(R.menu.heart, menu);
			return true;
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


 */
