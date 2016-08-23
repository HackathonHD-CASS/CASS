package com.perples.recosample;

import java.sql.Date;
import java.util.ArrayList;
import java.util.IllegalFormatConversionException;
import java.util.List;

public enum Datatype {

	DATE(1, Long.SIZE / 8), VARCHAR(2, -1), NUMERIC(3, Double.SIZE / 8), INTEGER(
			4, Integer.SIZE / 8), TIMESTAMP(5, Long.SIZE / 8),

	// Sensor datatype
	HEART_RATE(21, Double.SIZE / 8), BLOOD_PRESSURE(22, Double.SIZE / 8),
	// GPS datatype
	LONGITUDE(23, Long.SIZE / 8), LATITUDE(24, Long.SIZE / 8);

	public static final int UNPREDICTABLE_SIZE = -1;

	private int typeNo;
	private int size;

	private Datatype(int typeNo, int size) {
		this.typeNo = typeNo;
		this.size = size;
	}

	public int getTypeNo() {
		return typeNo;
	}

	public int getSize() {
		return size;
	}

	public static Datatype intValueOf(int typeNo) {
		for (Datatype datatype : values()) {
			if (datatype.typeNo == typeNo) {
				return datatype;
			}
		}

		throw new IllegalArgumentException(
				"There is no datatype which has typeNo '" + typeNo + "'");
	}
}
