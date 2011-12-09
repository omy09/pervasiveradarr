package com.pervasiveradar;


public class HelloPacket extends AbstractPacket {
	
	public long timestamp;
	public long messagesSeenLastTime;
	
	public HelloPacket(long timestamp, long number, int type) {
		this.timestamp = timestamp;
		this.messagesSeenLastTime = number;
		closeConnection = false;
		this.type = type;
	}

}
