package com.perples.recosample;

import java.util.ArrayList;
import java.util.List;

import android.app.Service;
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
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class HeartService extends Service   {

	int format;

	private BluetoothAdapter mBluetoothAdapter;
	private Handler mHandler;
	private static final long SCAN_PERIOD = 10000;
	private BluetoothLeScanner mLEScanner;
	private ScanSettings settings;
	private List<ScanFilter> filters;
	private BluetoothGatt mGatt;

	Object value;
	private MediaPlayer mp;
	HeartRateData hrd1;

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		mHandler = new Handler();

		hrd1 = new HeartRateData();
		
		final BluetoothManager bluetoothManager =
				(BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = bluetoothManager.getAdapter();
	}
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// TODO Auto-generated method stub

		if (Build.VERSION.SDK_INT >= 21) {
			mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
			settings = new ScanSettings.Builder()
			.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
			.build();
			filters = new ArrayList<ScanFilter>();
		}
		scanLeDevice(true);

		mp = MediaPlayer.create(this, R.raw.alarm);
		
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onDestroy() {
		if (mGatt == null) {
			return;
		}
		mGatt.close();
		mGatt = null;
		super.onDestroy();
	}
	
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
			Thread tr = new Thread() {		
				@Override
				public void run() {
					Log.i("onLeScan", device.toString());
					if(device.toString().equals("D3:44:62:D0:0C:00"))
						connectToDevice(device);
				}
			};
			tr.start();
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
				/**
				 * Find services which we want to manage
				 */
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
						
					hrd1.setter(Integer.parseInt(characteristic.getIntValue(format, 1).toString()));
					Log.i("onCharacteristicRead", HeartRateData.getter()+"");	
						//value = characteristic.getIntValue(format, 1);
						//Log.i("onCharacteristicRead", value.toString());		
//						if(Integer.parseInt(value.toString())>90)
//							mp.start();
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
	};
}

