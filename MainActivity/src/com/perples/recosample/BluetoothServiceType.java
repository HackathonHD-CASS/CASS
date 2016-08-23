package com.perples.recosample;

import java.util.UUID;


public enum BluetoothServiceType {
		HEART_RATE_SERVICE(Datatype.HEART_RATE, UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb"), 
				UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb"), UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));

		private Datatype datatype;
		private UUID serviceUuid;
		private UUID characteristicUuid;
		private UUID descriptorUuid;
		
		private BluetoothServiceType(Datatype datatype, UUID serviceUuid, UUID characteristicUuid, UUID descriptorUuid)
		{
			this.serviceUuid = serviceUuid;
			this.datatype = datatype;
			this.characteristicUuid = characteristicUuid;
			this.descriptorUuid = descriptorUuid;
		}
		
		public UUID getServiceUuid() {
			return serviceUuid;
		}
		
		public Datatype getDatatype() {
			return datatype;
		}
		
		public UUID getCharacteristicUuid() {
			return characteristicUuid;
		}
		
		public UUID getDescriptorUuid() {
			return descriptorUuid;
		}
		
		public static BluetoothServiceType valueOfDatatype(Datatype datatype)
		{
			for (BluetoothServiceType btServiceType: values())
			{
				if (btServiceType.getDatatype() == datatype)
					return btServiceType;
			}
			
			throw new IllegalArgumentException();
		}
	}