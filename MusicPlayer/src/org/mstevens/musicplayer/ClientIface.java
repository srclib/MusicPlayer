package org.mstevens.musicplayer;

import java.util.ArrayList;

import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

public class ClientIface {
	//final private int ACTION_BIND=1000;
	final private int ACTION_INIT=100;
	final private int ACTION_SONG_CHANGE=101;
	final private int ACTION_PLAY_STATE_CHANGE=102;
	final private int ACTION_PLAY_QUEUE_UPDATE=103;
	private boolean connected;
	private Messenger m;
	private void trysend(Message msg) {
		try {
			m.send(msg);
		} catch (Exception e) {
			return;
		}
	}
	public ClientIface() {
		connected=false;
	}
	public void attachMessenger(Messenger clientMessenger) {
		this.m=clientMessenger;
		connected=true;
	}
	public void detachMessenger() {
		this.m=null;
		connected=false;
	}
	public void init(ArrayList <Integer> indices, ArrayList<String> paths,int repeatstate,boolean shufflestate,int nowplaying,boolean playstate) {
		if (! connected) {return;}
		Bundle data=new Bundle();
		data.putStringArrayList("paths", paths);
		data.putIntegerArrayList("indices", indices);
		data.putInt("repeatstate",repeatstate);
		data.putBoolean("shufflestate", shufflestate);
		data.putInt("nowplaying", nowplaying);
		data.putBoolean("playstate", playstate);
		Message msg=Message.obtain(null,ACTION_INIT);
		msg.setData(data);
		trysend(msg);
	}
	public void setnowplaying(int nowplaying) {
		if (! connected) {return;}
		Log.d("ClientIface","now playing: "+nowplaying);
		Message msg = Message.obtain(null,ACTION_SONG_CHANGE,nowplaying,0);
		trysend(msg);
	}
	public void setplaystate(boolean playstate) {
		if (! connected) {return;}
		int p=0;
		if (playstate) {p++;}
		Message msg = Message.obtain(null,ACTION_PLAY_STATE_CHANGE,p,0);
		trysend(msg);
	}
	public void setplayqueue(ArrayList<Integer> indices) {
		if (! connected) {return;}
		Bundle data=new Bundle();
		data.putIntegerArrayList("indices", indices);
		Message msg=Message.obtain(null,ACTION_PLAY_QUEUE_UPDATE);
		msg.setData(data);
		trysend(msg);
	}
}
