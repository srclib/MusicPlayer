package org.mstevens.musicplayer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;

public class PlayerService extends Service implements OnErrorListener,
		OnBufferingUpdateListener, OnCompletionListener, OnPreparedListener {
	/*final private int ACTION_PLAY_PAUSE=1;
	final private int ACTION_NEXT=2;
	final private int ACTION_PREV=3;
	final private int ACTION_PLAYLIST_CHANGED=4;
	final private int ACTION_PLAYQUEUE_CHANGED=5;
	final private int ACTION_SHUFFLE_CHANGED=6;
	final private int ACTION_REPEAT_CHANGED=7;
	final private int ACTION_BIND=1000;*/
	public Base b = null;
	public MediaPlayer mp;
	
	public Library lib;
	public LibraryAdapter lAdapter;
	public CurrentAdapter cAdapter;
	public ArrayAdapter<String> pAdapter;
	
	public boolean wasswipe;
	public int screenYpos=1;
	public int is2fingerstate;
	
	public int nowplaying=-1;
	
    private boolean playstate=false;
    private int playposition=-1;
    private int shuffleplayposition=-1;
    private String nowplayingpath;
    private ArrayList<Integer> shufflemask;
    public boolean shufflestate;
    public int repeatstate;
    private boolean isprepared=false;
    //private ClientIface client=new ClientIface();
	//private ArrayList<String> paths=new ArrayList<String>();
	//private ArrayList<Integer> indices=new ArrayList<Integer>();
	private ArrayList<Integer> playqueue=new ArrayList<Integer>();
	public Handler libraryhandler;
    // Binder given to clients
    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    
	
	
	/*private class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
        	Bundle data;
            switch (msg.what) {
            case ACTION_BIND:
        		client.attachMessenger(msg.replyTo);
            	if (! firstbind) {
            		int nowplaying;
            		nowplaying=-1;
            		if (playposition>=0) {
            			nowplaying=indices.get(playposition);
            		}
            		client.init(indices, paths, repeatstate, shufflestate, nowplaying, playstate);
            	}
            	firstbind=false;
			case ACTION_PLAY_PAUSE:
				Log.d("MusicPlayerService","Play/Pause Received");
				setshuffleposition();
				play(msg.arg1);
				return;
			case ACTION_NEXT:
				Log.d("MusicPlayerService","Next Received");
				setshuffleposition();
				next();
				return;
			case ACTION_PREV:
				Log.d("MusicPlayerService","Prev Received");
				prev();
				return;
			case ACTION_PLAYLIST_CHANGED: //FIXME: race condition with next/play
				Log.d("MusicPlayerService","Playlist Received");
				data=msg.getData();
				if (data.containsKey("paths") && data.containsKey("indices")) {
					updatePlaylist(data.getIntegerArrayList("indices"),data.getStringArrayList("paths"));
					setshuffleposition();
				}
				return;
			case ACTION_PLAYQUEUE_CHANGED:
				data=msg.getData();
				if (data.containsKey("path") && data.containsKey("position") && data.containsKey("index")) {
					String position=data.getString("position");
					String path=data.getString("path");
					if (position.equals("end")) {
						playqueueadd(path,playqueue.size());
					}
					if (position.equals("start")) {
						playqueueadd(path,playqueueposition+1);
					}
				}
				Log.d("MusicPlayerService","PlayQueue Received");
				return;
			case ACTION_SHUFFLE_CHANGED:
				Log.d("MusicPlayerService","Shuffle Received");
				shufflestate=(msg.arg1==1);
				getshufflemask();
				return;
			case ACTION_REPEAT_CHANGED:
				Log.d("MusicPlayerService","Repeat Received");
				repeatstate=msg.arg1;
				return;
			default:
				Log.d("MusicPlayerService","Unknown Request Received");
            }
        }
    }
    final Messenger mMessenger = new Messenger(new IncomingHandler());
    public class LocalBinder extends Binder {
        PlayerService getService() {
            // Return this instance of LocalService so clients can call public methods
            return PlayerService.this;
        }
    }
    */
    public class LocalBinder extends Binder {
        PlayerService getService() {
            return PlayerService.this;
        }
    }
    private final IBinder mBinder = new LocalBinder();
	@Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
	@Override
	public void onCreate() {
		super.onCreate();
		Log.d("MusicPlayerService","onCreate");
		mp=new MediaPlayer();
		mp.setOnCompletionListener(this);
		lib=new Library(this);
	    pAdapter=new ArrayAdapter<String>(this, R.layout.playlistitem,R.id.playlistTV);
	    lAdapter = new LibraryAdapter(getApplicationContext(),R.layout.librarylistitem,new ArrayList<Library.libraryitem>(),this);
	    cAdapter = new CurrentAdapter(getApplicationContext(),R.layout.currentlistitem,new ArrayList<Integer>(),this);
		lib.init();
		lAdapter.addAll();
	}
	public boolean exiting() {
		if (playstate) {
			b=null;
			return false;
		} else {
			shutdown();
			return true;
		}
	}
	public void shutdown() {
		mp.reset();
		mp.release();
		stopSelf();		
	}
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d("MusicPlayerService","onStartCommand");
		return START_STICKY;
	}
	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onBufferingUpdate(MediaPlayer mp, int percent) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onCompletion(MediaPlayer mp) {
		// TODO Auto-generated method stub
		setshuffleposition();
		next();
	}

	@Override
	public void onPrepared(MediaPlayer mp) {
		// TODO Auto-generated method stub

	}

	public void getshufflemask() {
		int length=cAdapter.getCount();
    	Integer[] smask=new Integer[length];
    	int i=0;//playqueue.size();
    	for (int c=0;c<length;c++) {
//	        pqindex=playqueue.indexOf(c);
//	        if (pqindex>=0) {
//	            smask[pqindex]=c;
//	        } else {;
	            smask[i]=c;
	            i++;
//	        }
	    }
	    int t;
	    if (shufflestate) {
	    Random rgen=new Random();
//	    for (i=playqueue.size();i<length;i++) {
	    for (i=0;i<length;i++) {
	        int swap=rgen.nextInt(length-i)+i;    
	        t=smask[swap];
	        smask[swap]=smask[i];
	        smask[i]=t;
	    }
	    }
	    shufflemask=new ArrayList<Integer>(Arrays.asList(smask));
	}
    public void shufflemaskmove(int from, int to) {
		int item=shufflemask.get(from);
		shufflemask.remove(from);
		if (from<to) {
			to--;
		}
		shufflemask.add(to, item);
    }
    public void redrawpqall() {
    	if (b==null) {return;}
    	for (int i=0;i<playqueue.size();i++) {
    		b.redrawpq(playqueue.get(i),i);
    	}
    }
    public int pqindex(Integer index) {
    	return playqueue.indexOf(index);
    }
	public boolean inplayqueue(Integer index) {
		return playqueue.contains(index);
	}
	public void playqueueaddstart(Integer index) {
		if (playqueue.size()>0) {playqueue.add(0,index);}
		else {playqueue.add(index);}
		redrawpqall();
	}
	public void playqueueaddend(Integer index) {
		playqueue.add(index);
		if (b!=null) {b.redrawpq(index,playqueue.size()-1);}
	}
	public void playqueueremove(Integer index) {
		if (playqueue.contains(index)) {playqueue.remove(index);}
		if (b!=null) {b.redrawpq(index,-1);}
		redrawpqall();
	}
	public void setshuffleposition() {
		playposition=cAdapter.paths.indexOf(nowplayingpath);
		if (shufflestate) {
			shuffleplayposition=shufflemask.indexOf(playposition);
		} else {
			shuffleplayposition=playposition;
		}
	}
	public void updatePlaylist(ArrayList<Integer> indices, ArrayList<String> paths) {
		getshufflemask();
	}
	public void doplaypause() {
		if (playstate) {
			dopause();
		} else {
			doplay();
		}
	}
	public void dochangesong() {
		try {
			mp.reset();
			if (cAdapter.getCount()==0) {return;}
			if (playposition>=cAdapter.getCount()) {Log.d("MusicPlayerService", "Play Position out of bounds");return;}
			nowplayingpath=cAdapter.paths.get(playposition);
			int index = cAdapter.items.get(playposition);
			if (playqueue.indexOf(index)==0) {playqueueremove(index);}
			mp.setDataSource(nowplayingpath);
			mp.prepare();
			if (b != null) {b.setnowplayingcolor(index);}
			nowplaying=index;
			if (b!= null) {b.setplayingtext();}
			int nowplaying=cAdapter.items.get(playposition);
			Log.d("MusicPlayerService", "now playing: "+nowplaying);
			//client.setnowplaying(nowplaying);
			isprepared=true;
		} catch (Exception e) {
			Log.d("MusicPlayerService", "play error");
		}
		doplay();
	}
	public void dochangesongpq(int index,String path) {
		try {
			mp.reset();
			mp.setDataSource(path);
			mp.prepare();
			if (b != null) {b.setnowplayingcolor(index);}
			nowplaying=index;
			nowplayingpath=path;
			if (b!= null) {b.setplayingtext();}
			Log.d("MusicPlayerService", "now playing: "+nowplaying);
			//client.setnowplaying(nowplaying);
			isprepared=true;
		} catch (Exception e) {
			Log.d("MusicPlayerService", "play error");
		}
		doplay();
	}
	public void doplay() {
		playstate=true;
		if (! isprepared) {play(0);return;}
		try {
			mp.start();
			if (b!=null) {b.progresshandler.post(b.progressrunnable);b.setplaying(playstate);}
			//client.setplaystate(playstate);
		} catch (Exception e) {
			Log.d("MusicPlayerService", "play error");
		}
	}
	public void dopause() {
		playstate=false;
		mp.pause();
		if (b!=null) {b.progresshandler.removeCallbacks(b.progressrunnable);b.setplaying(playstate);}
		//updateprogress.run();
		//client.setplaystate(playstate);
		Log.d("MusicPlayerService", "Pausing "+cAdapter.paths.get(playposition));
	}
	public void seek(int progress,int max) {
		if (playstate==true) {
		Float pct = (float)progress/(float)max;
		Float dur = (float)mp.getDuration();
		Float pos=dur*pct;
		mp.seekTo(pos.intValue());
		}
	}
	public void stop() {
		playposition=-1;
		shuffleplayposition=-1;
		nowplayingpath="";
		playstate=false;
		if (b==null) {shutdown();}
		mp.reset();
		if (b!=null) {b.progresshandler.removeCallbacks(b.progressrunnable);b.resetprogress();b.setnowplayingcolor(-1);b.setplaying(playstate);}
		nowplaying=-1;
		if (b!=null) {b.setplayingtext();}
		//client.setplaystate(false);
		//client.setnowplaying(-1);
		isprepared=false;
	}
	public boolean play(int position) {
		setshuffleposition();
		if (position>=0) { 
			if (position==playposition) {
				doplaypause();
			} else {
				playposition=position;
				dochangesong();
			}
		} else { //play button
			if (isprepared) {
				doplaypause();
			} else {
				shuffleplayposition=-1;
				next();
			}
		}
		return playstate;
	}
	public void applyshuffle() {
		if (shufflestate) {
			playposition=shufflemask.get(shuffleplayposition);
		} else {
			playposition=shuffleplayposition;
		}
	}
	public void next() {
		if (playqueue.size()>0) {
			int index;
			index=playqueue.get(0);
			dochangesongpq(index, lib.songs[index].path);
			playqueue.remove(0);
			if (b!=null) {b.redrawpq(index,-1);}
			redrawpqall();
			return;
		}
		if (cAdapter.getCount()==0) {stop();return;}
		boolean isrepeating=false;
		if (repeatstate==2) {
			if (shuffleplayposition<0) {
				shuffleplayposition=0;
				applyshuffle();
			}
			dochangesong();
			return;
		}
		setshuffleposition();
		shuffleplayposition++;
		if (shuffleplayposition>=cAdapter.getCount()) {
			shuffleplayposition=0;
			isrepeating=true;
		}
		applyshuffle();
		if (isrepeating && repeatstate==0) {stop();return;}
		dochangesong();
	}
	public void prev() {
		boolean isrepeating=false;
		if (repeatstate==2) {
			if (shuffleplayposition<0) {
				shuffleplayposition=cAdapter.getCount()-1;
				applyshuffle();
			}
			dochangesong();
			return;
		}
		setshuffleposition();
		shuffleplayposition--;
		if (shuffleplayposition<0) {
			shuffleplayposition=cAdapter.getCount()-1;
			isrepeating=true;
		}
		applyshuffle();
		if (isrepeating && repeatstate==0) {stop();return;}
		dochangesong();
	}
	public boolean toggleshuffle() {
		shufflestate=! shufflestate;
		getshufflemask();
		return shufflestate;
	}
	public int togglerepeat() {
		repeatstate=(repeatstate+1)%3;
		return repeatstate;
	}
	public void setrepeat(int repeatstate) {
		return;
	}
	public void select(int index) {
		if (b != null) {cAdapter.add(index);}
	}
	public void deselect(int index) {
		if (b != null) {cAdapter.remove(index);}
	}
	public void applySelection(int index, int selected) {
		if (b != null) {b.applySelection(index,selected);}
	}
	public void playlist_onchange() {
		if (b != null) {b.plib.onchange();}
	}
	public void updatebuttons() {
		if (b != null) {b.updatebuttons();}
	}
	public void updatechecks() {
		if (b != null) {b.updatechecks();}
	}
	public void registerForContextMenu(View v) {
		if (b != null) {b.registerForContextMenu(v);}
	}

}
