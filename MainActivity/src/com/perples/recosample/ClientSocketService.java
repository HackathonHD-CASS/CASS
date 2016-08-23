package com.perples.recosample;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class ClientSocketService extends Service{

	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// TODO Auto-generated method stub

		Socket socket = null;
		try{ 
			socket = new Socket("10.10.100.191", 8888);
			socket.setSoTimeout(10000);

			DataInputStream din = new DataInputStream(socket.getInputStream());
			int n = din.read();
			if(n!=1) {
				return super.onStartCommand(intent, flags, startId);
			}
		}catch(Exception e) {
			Log.e("소켓접속상태", e.getMessage());
			if(socket!=null)
				try {
					socket.close();
				} catch (IOException e1) {
				}
			return super.onStartCommand(intent, flags, startId);
		}
		
		
		
		
		
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}
}
