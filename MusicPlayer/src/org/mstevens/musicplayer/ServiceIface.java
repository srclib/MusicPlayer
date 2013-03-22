package org.mstevens.musicplayer;

import java.util.ArrayList;

import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;

public class ServiceIface {
	final private int ACTION_PLAY_PAUSE=1;
	final private int ACTION_NEXT=2;
	final private int ACTION_PREV=3;
	final private int ACTION_PLAYLIST_CHANGED=4;
	final private int ACTION_PLAYQUEUE_CHANGED=5;
	final private int ACTION_SHUFFLE_CHANGED=6;
	final private int ACTION_REPEAT_CHANGED=7;
	final private int ACTION_BIND=1000;
	private Messenger m;
	private void trysend(Message msg) {
		try {
			m.send(msg);
		} catch (Exception e) {
			return;
		}
	}
	public ServiceIface(Messenger serviceMessenger,Messenger clientMessenger) {
		this.m=serviceMessenger;
		Message msg=Message.obtain(null, ACTION_BIND);
		msg.replyTo=clientMessenger;
		trysend(msg);
	}
	public void updatePlaylist(ArrayList <Integer> indices, ArrayList<String> paths) {
		Bundle data=new Bundle();
		data.putStringArrayList("paths", paths);
		data.putIntegerArrayList("indices", indices);
		Message msg = Message.obtain(null,ACTION_PLAYLIST_CHANGED);
		msg.setData(data);
		trysend(msg);
	}
	private void playqueuechange(int index,String path,String position) {
		Message msg = Message.obtain(null,ACTION_PLAYQUEUE_CHANGED);
		Bundle data=new Bundle();
		data.putString("path", path);
		data.putInt("index", index);
		data.putString("position", position);
		msg.setData(data);
		trysend(msg);
	}
	public void addplayqueueend(int index,String path) {
		playqueuechange(index,path,"end");
	}
	public void addplayqueuestart(int index,String path) {
		playqueuechange(index,path,"start");
	}
	public void removefromplayqueue(int index,String path) {
		playqueuechange(index,path,"remove");
	}
	public void next() {
		Message msg = Message.obtain(null,ACTION_NEXT);
		trysend(msg);
	}
	public void prev() {
		Message msg = Message.obtain(null,ACTION_PREV);
		trysend(msg);
	}
	public void play(int pos) {
		Message msg = Message.obtain(null,ACTION_PLAY_PAUSE,pos,0);
		trysend(msg);
	}
	public void setrepeat(int repeatstate) {
		Message msg = Message.obtain(null,ACTION_REPEAT_CHANGED,repeatstate,0);
		trysend(msg);
	}
	public void setshuffle(boolean shufflestate) {
		int s=0;
		if (shufflestate) {s++;}
		Message msg = Message.obtain(null,ACTION_SHUFFLE_CHANGED,s,0);
		trysend(msg);
	}
}
