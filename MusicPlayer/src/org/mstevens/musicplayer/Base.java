package org.mstevens.musicplayer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.Html;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnKeyListener;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class Base extends Activity {
	//constants
	private float MIN_SCROLL_TIME=60;//Lower is faster
	private float MAX_SCROLL_TIME=500;//Lower is faster
	private final int ACTION_RENAME=1;
	private final int ACTION_CREATE=2;
//	final private int ACTION_PLAY_PAUSE=1;
//	final private int ACTION_NEXT=2;
//	final private int ACTION_PREV=3;
//	final private int ACTION_PLAYLIST_CHANGED=4;
//	final private int ACTION_PLAYQUEUE_CHANGED=5;
//	final private int ACTION_SHUFFLE_CHANGED=6;
//	final private int ACTION_REPEAT_CHANGED=7;
//	final private int ACTION_INIT=100;
//	final private int ACTION_SONG_CHANGE=101;
//	final private int ACTION_PLAY_STATE_CHANGE=102;
	private boolean loaded=false;
	//service stuff
	
    //public ServiceIface pService;
    private boolean serviceBound;
    private PlayerService p;
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            //PlayerService.LocalBinder binder = (PlayerService.LocalBinder) service;
            //Messenger m = new Messenger(service);
            p=((PlayerService.LocalBinder)service).getService();
            p.libraryhandler=libraryhandler;
            //pService= new ServiceIface(m,new Messenger(new ClientHandler()));
            serviceBound = true;
            libraryhandler.sendEmptyMessage(0);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            serviceBound = false;
        }
    };
    /*private class ClientHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
			case ACTION_INIT:
				if (msg.arg1==1) {
					Bundle data=msg.getData();
					if (data.containsKey("repeatstate")
							&& data.containsKey("shufflestate")
							&& data.containsKey("paths")
							&& data.containsKey("indices")) {
						Log.d("MusicPlayer","Init data all present");
					}
				}
				return;
			case ACTION_PLAY_STATE_CHANGE:
				setplaying((msg.arg1==1));
				return;
			case ACTION_SONG_CHANGE:
				if (p.nowplaying==msg.arg1) {return;}
				setnowplayingcolor(msg.arg1);
				p.nowplaying=msg.arg1;
				setplayingtext();
				if (p.nowplaying<0) {return;}
				int selection=lib.songs[p.nowplaying].playlistposition-2;
				if (selection<0) {selection=0;}
				cv.setSelection(selection);
				if (shufflestate) {
					cv.scrollBy(0, 1); //shows scrollbar
					cv.scrollBy(0, -1); //shows scrollbar
				}
				return;
			default:
				Log.d("MusicPlayer","Unknown Request Received");
				return;
            }
        }
    }*/
	//view stuff
	ViewFlipper flipper;
	public PlaylistLibrary plib;
	public Library lib;
	public ListView lv;
	public ListView cv;
	public ListView pv;
	public TextView linfo;
	public TextView cinfo;
	public Handler progresshandler;
	public Runnable progressrunnable;
	//public LibraryAdapter lAdapter;
	//public CurrentAdapter cAdapter;
	//public ArrayAdapter<String> pAdapter;
	//player stuff
	public int repeatstate=0;
	public boolean shufflestate=false;
	//view data
	public String[] titles = { "Library", "Current Playlist", "Playlists" };
	public int currentView=0;
	private boolean seeking=false;
	private boolean currentcompact=false;
	//initialization data
	public Handler libraryhandler;
	/*private Runnable loadlibrary=new Runnable(){
		public void run() {
			 libraryhandler.sendMessage(Message.obtain(libraryhandler, 1, R.string.library_loading, 0));
			 lib.init();
			 lib.load(state);
			 libraryhandler.sendEmptyMessage(0);
			 libraryhandler.sendEmptyMessage(10);
		}
	};*/
	//swipe detection data
	public boolean swipe_disabled=false;
	private long downTime;
	private float downXValue;
	private float downYValue;
	private float pointerMinY;
	private float pointerMaxY;
	private int SCROLL_TIME;
	//two-finger detection data

	private int movefrom2finger;
	private int lastpos2finger;
	private int lasttb2finger;
	private class ScrollHandler extends Handler {
		public void RemoveAndPostDelayed(Runnable r, long delay) {
			this.removeCallbacks(scrollup);
			this.removeCallbacks(scrolldown);
			this.postDelayed(r, delay);
		}
		public void removeAllCallbacks() {
			this.removeCallbacks(scrollup);
			this.removeCallbacks(scrolldown);
		}
	}
	private ScrollHandler scrollhandler=new ScrollHandler();
	private Runnable scrollup=new Runnable(){
		public void run() {
			if (! loaded) {
				return;
			}
			if (currentView==1) {
				if (cv.getFirstVisiblePosition()-1>0) {
						cv.setSelection(cv.getFirstVisiblePosition()-1);
					} else {
						cv.setSelection(0);
					}
				scrollhandler.postDelayed(this, SCROLL_TIME);
			}
		}
	};
	private Runnable scrolldown=new Runnable(){
		public void run() {
			if (! loaded) {
				return;
			}
			if (currentView==1) {
				if (cv.getFirstVisiblePosition()+1<cv.getCount()) {
					cv.setSelection(cv.getFirstVisiblePosition()+1);
				} else {
					cv.setSelection(cv.getCount()-1);
				}
				scrollhandler.postDelayed(this, SCROLL_TIME);
			}
		}
	};
	public int[] getplaylistitemunderpointer(int y) {
		int[] retval=new int[2];
		int[] coords=new int[2];
		if (cv.getCount()==0) {
			retval[0]=-1;
			retval[1]=-1;
			return retval;
		}
		View v=cv.getChildAt(0);
		v.getLocationOnScreen(coords);
		int starty=coords[1];
		int ystep=v.getMeasuredHeight()+cv.getDividerHeight();
		if (y<starty) {
			retval[0]=0;
			retval[1]=0;
			return retval;
		}
		v=cv.getChildAt(cv.getLastVisiblePosition()-cv.getFirstVisiblePosition());
		v.getLocationOnScreen(coords);
		int endy=coords[1]+v.getMeasuredHeight();
		if (y>endy) {
			retval[0]=cv.getLastVisiblePosition()-cv.getFirstVisiblePosition();
			retval[1]=1;
			return retval;
		}
		y-=starty;
		if (y%ystep>ystep/2) {
			retval[1]=1;
		} else {
			retval[1]=0;
		}
		y-=y%ystep;
		retval[0]=y/ystep;
		return retval;
	}
	public void start2finger(int y) {
		if (p.is2fingerstate != 1) {
			p.is2fingerstate=1;
			int retval[]=new int[2];
			retval=getplaylistitemunderpointer(y);
			if (retval[0]==-1) {return;}
			lastpos2finger=retval[0]+cv.getFirstVisiblePosition();
			lasttb2finger=retval[1];
			movefrom2finger=lastpos2finger;
			drawmoveselect(lastpos2finger,lasttb2finger);
		}
	}
	public void onmove2finger(int y) {
		if (p.is2fingerstate == 1) {
			int retval[]=new int[2];
			retval=getplaylistitemunderpointer(y);
			if (retval[0]==-1) {return;}
			int pos2finger=retval[0]+cv.getFirstVisiblePosition();
			int tb2finger=retval[1];
			if (pos2finger != lastpos2finger) {
				restoreplaylistitem(lastpos2finger);
			}
			if (pos2finger != lastpos2finger || tb2finger!=lasttb2finger) {
				drawmoveselect(pos2finger, tb2finger);
			}
			lastpos2finger=pos2finger;
			lasttb2finger=tb2finger;
		}
	}
	public void stop2finger(int y) {
		if (p.is2fingerstate == 1) {
			int retval[]=new int[2];
			retval=getplaylistitemunderpointer(y);
			if (retval[0]==-1) {return;}
			int pos2finger=retval[0]+cv.getFirstVisiblePosition();
			int tb2finger=retval[1];
//			if (pos2finger != lastpos2finger) {
//				cAdapter.restore(lastpos2finger);
//			}
			pos2finger+=tb2finger;
			Log.d("MusicPlayer", "Moving from "+movefrom2finger+" to "+pos2finger);
			p.cAdapter.move(movefrom2finger, pos2finger);
			//p.cAdapter.sendtoservice();
		}
		p.is2fingerstate=0;
	}
	//two-finger and swipe detection function
	public boolean dispatchTouchEvent(MotionEvent ev) { //intercepts all touch events for the activity
    	 MotionEvent event=ev;
    	 if (! serviceBound) {return super.dispatchTouchEvent(ev);}
         if (event.getAction()==MotionEvent.ACTION_DOWN) { //Saves values for later calculations
             downXValue = event.getRawX();
             downYValue = event.getRawY();
             downTime = event.getEventTime();
         } else if (event.getAction()==MotionEvent.ACTION_UP) { //Calculates whether event was a true swipe
        	 if (p.is2fingerstate==1) {
        		 scrollhandler.removeAllCallbacks();
            	 stop2finger((int)event.getRawY());
            	 return false;
        	 }
             float currentX = event.getRawX();
             float currentY = event.getRawY();
             long currentTime = event.getEventTime();
             float differenceX = Math.abs(downXValue - currentX);
             float differenceY = Math.abs(downYValue - currentY);
             long time = currentTime - downTime;
             p.wasswipe=false;
             if (differenceX>90 && differenceY/differenceX<0.3 && differenceX/time>.6) { //makes sure swipe was far enough, at the right angle, and fast enough
            	 Log.i("Touch Event:", "Swipe");
            	 p.wasswipe=true;//Variable read by callbacks to make sure they don't read a swipe as a click
            	 if (downXValue > currentX) {
            		 SwipeLeft();
            	 } else {
            		 SwipeRight();
            	 }
             }
         } else if (event.getAction()==MotionEvent.ACTION_POINTER_2_DOWN) {
        	 if (currentView==1) {
	        	 start2finger((int)event.getRawY());
        		 int[] coords=new int[2];
	        	 cv.getLocationOnScreen(coords);
	        	 pointerMinY=coords[1];
	        	 pointerMaxY=pointerMinY+cv.getMeasuredHeight()-15;
	        	 MotionEvent upev=MotionEvent.obtain(ev);
	        	 upev.setAction(MotionEvent.ACTION_UP);
	        	 super.dispatchTouchEvent(upev);
	        	 return true;
        	 }
         } else {
        	 if (p.is2fingerstate==1) {
        		 if (event.getAction() != MotionEvent.ACTION_MOVE) {
        			 return false;
        		 }
                 float currentY = (event.getRawY()-pointerMinY)/(pointerMaxY-pointerMinY);
                 if (currentY<.25) {//Top 1/4 of screen
                	 if (p.screenYpos != 0) {
                		 scrollhandler.RemoveAndPostDelayed(scrollup,100);
                    	 p.screenYpos=0;
                	 }
            		 SCROLL_TIME=(int)(MIN_SCROLL_TIME+(currentY)*4*(MAX_SCROLL_TIME-MIN_SCROLL_TIME));
                 } else if (currentY>0.75) {//Bottom 1/4 of screen
                	 if (p.screenYpos != 3) {
                		 scrollhandler.RemoveAndPostDelayed(scrolldown,100);
                    	 p.screenYpos=3;
                	 }
            		 SCROLL_TIME=(int)(MIN_SCROLL_TIME+(1-currentY)*4*(MAX_SCROLL_TIME-MIN_SCROLL_TIME));
                 } else {
                	 if (currentY>0.5) {p.screenYpos=2;} else {p.screenYpos=1;}
                	 if (p.screenYpos != 1 || p.screenYpos != 2) {
                		 scrollhandler.removeAllCallbacks();
                	 }
                 }
                 if (SCROLL_TIME<MIN_SCROLL_TIME) {SCROLL_TIME=(int)MIN_SCROLL_TIME;}
                 if (SCROLL_TIME>MAX_SCROLL_TIME) {SCROLL_TIME=(int)MAX_SCROLL_TIME;}
                 onmove2finger((int)event.getRawY());
        		 return true;
        	 }
         }
    	 return super.dispatchTouchEvent(ev); //sends event on to child views
     }
	//long click detection
	private int contextuid=0;
    @Override
	public void onCreateContextMenu(ContextMenu menu, View v,
	                                ContextMenuInfo menuInfo) {
	  if (! loaded) {
		  return;
	  }
	  super.onCreateContextMenu(menu, v, menuInfo);
	  MenuInflater inflater = getMenuInflater();
	  if (v.getId()==R.id.currentListItem) {
		  int index=(Integer)v.getTag();
		  contextuid=index;
		  inflater.inflate(R.menu.song_context_menu, menu);
		  if (p.inplayqueue(index)) {
			  menu.findItem(R.id.addendplayqueue).setVisible(false);
			  menu.findItem(R.id.addstartplayqueue).setVisible(false);
		  } else {
			  menu.findItem(R.id.removefromplayqueue).setVisible(false);
		  }
	  } else if (v.getId()==R.id.playlistList) {
		  contextuid=-1-((AdapterContextMenuInfo)menuInfo).position;
		  if (((AdapterContextMenuInfo)menuInfo).position==0) {
			  inflater.inflate(R.menu.default_playlist_context_menu, menu);
			  return;
		  }
		  inflater.inflate(R.menu.playlist_context_menu, menu);
	  }
	}
    @Override
    public boolean onContextItemSelected(MenuItem item) {
//      AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
      Log.d("MusicPlayer", "Context Menu click with UID: "+contextuid);
      String name="";
      switch (item.getItemId()) {
      case R.id.removefromplaylist:
    	  lib.songs[contextuid].deselect();
    	  p.lAdapter.updateselections();
    	  //playlistChanged();
    	  return true;
      case R.id.addstartplayqueue:
    	  p.playqueueaddstart(contextuid);
    	  //pService.addplayqueuestart(song.index, song.path);
    	  return true;
      case R.id.addendplayqueue:
    	  p.playqueueaddend(contextuid);
    	  //pService.addplayqueueend(song.index, song.path);
    	  return true;
      case R.id.removefromplayqueue:
    	  p.playqueueremove(contextuid);
    	  //pService.removefromplayqueue(song.index, song.path);
    	  return true;
      case R.id.playlistclear:
    	  plib.deleteplaylist("Default");
    	  return true;
      case R.id.playlistdelete:
    	  Log.d("MusicPlayer", "deleting "+(-1-contextuid));
    	  name=p.pAdapter.getItem(-1-contextuid);
    	  if (name.endsWith(" (Saved)")) {
				name=name.substring(0, name.length()-8);
    	  } else if (name.startsWith("*")) {
				name=name.substring(1);
    	  }
    	  Log.d("MusicPlayer", "deleting "+name);
    	  plib.deleteplaylist(name);
    	  return true;
      case R.id.playlistrename:
    	  name=p.pAdapter.getItem(-1-contextuid);
    	  if (name.endsWith(" (Saved)")) {
				name=name.substring(0, name.length()-8);
    	  } else if (name.startsWith("*")) {
				name=name.substring(1);
    	  }
    	  if (plib.lock) {return false;}
    	  plib.lock=true;
    	  Intent foo = new Intent(this, TextEntryActivity.class);
    	  foo.putExtra("title", "Rename Playlist");
    	  foo.putExtra("value", name);
    	  this.startActivityForResult(foo, ACTION_RENAME);
    	  return true;
      default:
        return super.onContextItemSelected(item);
      }
    }
    //search stuff
    public EditText lsearch;
    public EditText csearch;
    public int libraryselection=-1;
	public Runnable scrolllibrary=new Runnable(){
		public void run() {
			if (! loaded) {
				return;
			}
			lv.setSelection(libraryselection);
			libraryselection=-1;
		}
	};
	private OnKeyListener librarySearchListener=new OnKeyListener() {
    	public boolean onKey (View v, int keyCode, KeyEvent event) {
    		if (event.getAction()!=KeyEvent.ACTION_UP) {return false;}
    		if (keyCode==KeyEvent.KEYCODE_ENTER) {
    			//p.lAdapter.dosearch(lsearch.getText().toString());
    			return true;
    		} else {
    			Log.d("MusicPlayer", "KeyCode: "+keyCode);
    		}
    		return false;
    	}
    };
    @Override
	public boolean onSearchRequested() {
		if (! loaded) {return false;}
		Log.d("MusicPlayer","Search Request");
		if (currentView==0) {
			//p.lAdapter.togglesearch();
		}
		return true;
	}
    //button callbacks
    //public void playlistChanged() {
    //	if (serviceBound) {cAdapter.sendtoservice();;}
    //}
    @Override
    public void onBackPressed() {
    	if (loaded) {
    		if (currentView==0 && p.lAdapter.searching) {
    			//p.lAdapter.togglesearch();
    			return;
    		}
    	}
    	super.onBackPressed();
    }
    public void sortPlaylist(View v) {
	      if (loaded) {
	    	  int index=(Integer)cv.getChildAt(0).getTag();
	    	  p.cAdapter.sort();
	    	  cv.setSelection(lib.songs[index].playlistposition);
	    	  //playlistChanged();
	      }
	}
    public void setplaying(boolean playing) {
    	if (playing) {
    		findViewById(R.id.CVplay).setBackgroundResource(R.drawable.pausebg);
    	} else {
    		findViewById(R.id.CVplay).setBackgroundResource(R.drawable.playbg);
    	}
    }
    public void setcompact(View v) {
    	currentcompact=true;
    	setplayingtext();
    	findViewById(R.id.CVcompact).setVisibility(View.GONE);
    	findViewById(R.id.CVuncompact).setVisibility(View.VISIBLE);
    	findViewById(R.id.CVsort).setVisibility(View.GONE);
    	findViewById(R.id.seekBar).setVisibility(View.GONE);
    	findViewById(R.id.seekText).setVisibility(View.GONE);
    }
    public void setuncompact(View v) {
    	currentcompact=false;
    	setplayingtext();
    	findViewById(R.id.CVuncompact).setVisibility(View.GONE);
    	findViewById(R.id.CVcompact).setVisibility(View.VISIBLE);
    	findViewById(R.id.CVsort).setVisibility(View.VISIBLE);
    	findViewById(R.id.seekBar).setVisibility(View.VISIBLE);
    	findViewById(R.id.seekText).setVisibility(View.VISIBLE);
    }
    public void redrawpq(int index,int pq) {
    	View v=null;
    	v=cv.getChildAt(lib.songs[index].playlistposition-cv.getFirstVisiblePosition());
    	if (v != null) {
    		String pqstring="";
    		if (pq>=0) {pqstring=String.valueOf(pq+1)+": ";}
            TextView songtext = (TextView) v.findViewById(R.id.currentListSongText);
            songtext.setText(pqstring+lib.songs[index].name);
    	}
    }
    public void setnowplayingcolor(int newindex) {
    	TextView songtext;
    	TextView albumartisttext;
    	Log.d("MusicPlayer","now playing: "+p.nowplaying+", new index: "+newindex);
    	View v=null;
    	if (p.nowplaying>=0 && p.nowplaying<lib.songcount) {
    		v=cv.getChildAt(lib.songs[p.nowplaying].playlistposition-cv.getFirstVisiblePosition());
    	}
    	if (v != null) {
            songtext = (TextView) v.findViewById(R.id.currentListSongText);
            albumartisttext = (TextView) v.findViewById(R.id.currentListArtistAlbumText);
            songtext.setTextColor(0xffffffff);
            albumartisttext.setTextColor(0xffffffff);
    	}
    	v=null;
    	if (newindex>=0 && newindex<lib.songcount) {
    		v=cv.getChildAt(lib.songs[newindex].playlistposition-cv.getFirstVisiblePosition());
    	}
    	if (v != null) {
            songtext = (TextView) v.findViewById(R.id.currentListSongText);
            albumartisttext = (TextView) v.findViewById(R.id.currentListArtistAlbumText);
            songtext.setTextColor(getResources().getColorStateList(R.color.playlisttextcolor));
            albumartisttext.setTextColor(getResources().getColorStateList(R.color.playlisttextcolor));
    	}
    }
    public void setplayingtext() {
    	if (p.nowplaying<0) {
    		((TextView)findViewById(R.id.CVName)).setText(getResources().getText(R.string.nothing_playing));
    		return;
    		}
    	Library.song song=lib.songs[p.nowplaying];
    	if (currentcompact) {
    		((TextView)findViewById(R.id.CVName)).setText(Html.fromHtml("<b>"+song.name+"</b>"));    		
    	} else {
    		((TextView)findViewById(R.id.CVName)).setText(Html.fromHtml("<b>"+song.name+"</b><br>by <b>"+song.getparent().getparent().name+"</b><br>on <i>"+song.getparent().name+"</i>"));
    	}
    }
    public void updateprogress() {
    	if (seeking) {return;}
    	int pos=p.mp.getCurrentPosition()/1000;
    	int max=p.mp.getDuration()/1000;
    	SeekBar s=(SeekBar)findViewById(R.id.seekBar);
    	TextView st=(TextView)findViewById(R.id.seekText);
    	s.setMax(max);
    	s.setProgress(pos);
    	Integer possec=pos%60;
    	Integer posmin=(pos-possec)/60;
    	Integer maxsec=max%60;
    	Integer maxmin=(max-maxsec)/60;
    	String possep=":";
    	if (possec<10) {possep=":0";}
    	String maxsep=":";
    	if (maxsec<10) {maxsep=":0";}
    	st.setText(posmin.toString()+possep+possec.toString()+"/"+maxmin.toString()+maxsep+maxsec.toString());
    }
    public void resetprogress() {
    	SeekBar s=(SeekBar)findViewById(R.id.seekBar);
    	TextView st=(TextView)findViewById(R.id.seekText);
    	s.setMax(100);
    	s.setProgress(0);
    	st.setText("00:00/00:00");
    }
    public void updatebuttons() {
		setTitle(titles[currentView]);
    	Button clearall=(Button)findViewById(R.id.libraryClearAll);
    	Button selectall=(Button)findViewById(R.id.librarySelectAll);
    	Button collapseall=(Button)findViewById(R.id.libraryCollapseAll);
    	Button expandall=(Button)findViewById(R.id.libraryExpandAll);
    	if (clearall==null || selectall==null || collapseall==null || expandall==null) {return;}
    	if (p.cAdapter.getCount()==0) {
    		clearall.setVisibility(View.GONE);
    		selectall.setVisibility(View.VISIBLE);
    		cinfo.setText(getResources().getString(R.string.empty_playlist));
    		cinfo.setVisibility(View.VISIBLE);
    	} else {
    		clearall.setVisibility(View.VISIBLE);
    		selectall.setVisibility(View.GONE);
    		cinfo.setVisibility(View.GONE);
    	}
    	if (p.lAdapter.expandedcount==0) {
    		collapseall.setVisibility(View.GONE);
    		expandall.setVisibility(View.VISIBLE);
    	} else {
    		collapseall.setVisibility(View.VISIBLE);
    		expandall.setVisibility(View.GONE);
    	}
    }
	public void expandall(View v) {
	      if (loaded) {
	    	  int artistindex=lib.artistindexfromuid((Integer)lv.getChildAt(0).getTag());
	    	  p.lAdapter.expandall();
	    	  updatebuttons();
	    	  lv.setSelection(lib.artists[artistindex].position);
	      }
	}
	public void collapseall(View v) { //callback from xml for Library Collapse All button
	      if (loaded) {
	    	  int artistindex=lib.artistindexfromuid((Integer)lv.getChildAt(0).getTag());
	    	  p.lAdapter.closeall();
	    	  updatebuttons();
	    	  lv.setSelection(lib.artists[artistindex].position);
	      }
	}
	public void selectall(View v) { //callback from xml for Library Select All button
	      if (loaded) {
	    	  p.lAdapter.selectall();
	    	  updatebuttons();
	    	  //playlistChanged();
	    	  for (int i=0;i<=lv.getLastVisiblePosition()-lv.getFirstVisiblePosition();i++) {
	    		  ((ImageView)lv.getChildAt(i).findViewById(R.id.libraryListBox)).setImageResource(R.drawable.checked);
	    	  }
	      }
	}
    public void restoreplaylistitem(int pos) {
    	int cvpos=pos-cv.getFirstVisiblePosition();
    	if (cvpos>=0 && cvpos<=cv.getLastVisiblePosition()-cv.getFirstVisiblePosition()) {
    		View v=cv.getChildAt(pos-cv.getFirstVisiblePosition());
    		v.setBackgroundDrawable(getbg(pos));
    	}       	
    }
    public void drawmoveselect(int pos,int bottom) {
    	int bg=getbgcolor(pos);
		View v=cv.getChildAt(pos-cv.getFirstVisiblePosition());
		v.setBackgroundDrawable(drawselection(bg,v.getWidth(),v.getHeight(),bottom)); 		
    }
    private Drawable drawselection(int bg,int w, int h,int bottom) {
    	Bitmap bm =Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
    	Canvas c=new Canvas(bm);
    	Paint pnt=new Paint();
    	pnt.setColor(bg);
    	pnt.setStyle(Paint.Style.FILL);
    	c.drawPaint(pnt);
    	pnt.setColor(p.getResources().getColor(R.color.pressed));
    	if (bottom==1) {
    		c.drawRect(0, h-2, w, h, pnt);
    	} else {
    		c.drawRect(0, 0, w, 2, pnt);
    	}
    	return new BitmapDrawable(bm);
    }
    public int getbgcolor(int pos) {
        if (pos%2==0) {
        	return p.getResources().getColor(R.color.bg1);
        } else {
        	return p.getResources().getColor(R.color.bg2);
        }
      }
    public Drawable getbg(int pos) {
        if (pos%2==0) {
        	return p.getResources().getDrawable(R.drawable.currentbg1);
        } else {
        	return p.getResources().getDrawable(R.drawable.currentbg2);
        }
      }
    public void applySelection(int index,int selected) {
    	if (index<0) {return;}
    	p.lAdapter.getItem(index).selected=selected;
    	View v=lv.getChildAt(index-lv.getFirstVisiblePosition());//getchildat index refers only to visible items
    	if (v != null) {//this check could be made more efficient using lv.getLastVisiblePosition
        ImageView cb = (ImageView) v.findViewById(R.id.libraryListBox);
        if (selected==0) {
        	cb.setImageResource(R.drawable.unchecked);
        } else if (selected==1) {
        	cb.setImageResource(R.drawable.halfchecked);
        } else {
        	cb.setImageResource(R.drawable.checked);
        }
    	} else {
    		Log.d("MusicPlayer", "Null View Checking Box");
    	}
    }
    public void updatechecks() {
    	View v;
    	ImageView cb;
    	int selected;
    	for (int i=0;i<=lv.getLastVisiblePosition()-lv.getFirstVisiblePosition();i++) {
    		v=lv.getChildAt(i);
    		selected=lib.selectedfromuid((Integer)v.getTag());
    		cb=(ImageView)v.findViewById(R.id.libraryListBox);
            if (selected==0) {
            	cb.setImageResource(R.drawable.unchecked);
            } else if (selected==1) {
            	cb.setImageResource(R.drawable.halfchecked);
            } else {
            	cb.setImageResource(R.drawable.checked);
            }
    	}
    }
	public void clearall(View v) { //callback from xml for Library Clear All button
	      if (loaded) {
	    	  if (p.cAdapter.getCount()==0) {return;}
	    	  plib.saved=false;
	    	  p.cAdapter.clearall();
	    	  p.lAdapter.clearall();
	    	  plib.updatenames();
	    	  //playlistChanged();
	    	  updatebuttons();
	  	  	  for (int i=0;i<(lv.getLastVisiblePosition()-lv.getFirstVisiblePosition()+1);i++) {
	  	  		  ((ImageView)lv.getChildAt(i).findViewById(R.id.libraryListBox)).setImageResource(R.drawable.unchecked);
			  }
	      }
	}
	public void shuffleClick(View v) {
		if (serviceBound) {
			shufflestate=p.toggleshuffle();
		    if (shufflestate) {v.setBackgroundResource(R.drawable.shuffleselected);}
		    else {v.setBackgroundResource(R.drawable.shuffle);}
		}
	}
	public void repeatClick(View v) { //callback from xml for Library Collapse All button
	      if (serviceBound) {
	    	  repeatstate=p.togglerepeat();
	    	  if (repeatstate==0) {v.setBackgroundResource(R.drawable.repeat);}
	     	  else if (repeatstate==1) {v.setBackgroundResource(R.drawable.repeatselected);}
	     	  else if (repeatstate==2) {v.setBackgroundResource(R.drawable.repeatselected1);}
	      }
	      //if (serviceBound) {pService.setrepeat(repeatstate);}
	}
	public void playClick(View v) { //callback from xml for Library Collapse All button
		//if (serviceBound) {pService.play(-1);}
		p.doplaypause();
	}
	public void nextClick(View v) { //callback from xml for Library Collapse All button
		//if (serviceBound) {pService.next();}
		p.next();
	}
	public void prevClick(View v) { //callback from xml for Library Collapse All button
		//if (serviceBound) {pService.prev();}
		p.prev();
	}

	
	//playlist callbacks
	@Override
	public void onActivityResult (int requestCode, int resultCode, Intent data) {
		if (resultCode == Activity.RESULT_OK) {
			if (requestCode == ACTION_RENAME) {
		    	String from=p.pAdapter.getItem(-1-contextuid);
		    	if (from.endsWith(" (Saved)")) {
					from=from.substring(0, from.length()-8);
		    	} else if (from.startsWith("*")) {
					from=from.substring(1);
		    	}
				String to=data.getStringExtra("value");
				if (to==null) {return;}
				if (to.equals("")) {return;}
				if (plib.names.contains(to)) {
					Toast.makeText(getApplicationContext(), "Name already taken", Toast.LENGTH_SHORT);
					return;
				}
				plib.lock=false;
				plib.rename(from, to);
				return;
			} else if (requestCode == ACTION_CREATE) {
				String name=data.getStringExtra("value");
				if (name==null) {return;}
				if (name.equals("")) {return;}
				plib.lock=false;
				plib.saveplaylistas(name);
				return;
			}
		} else {
			plib.lock=false;
		}
	}
	DialogInterface.OnClickListener saveCurrentListener = new DialogInterface.OnClickListener() {
	    @Override
	    public void onClick(DialogInterface dialog, int which) {
	        switch (which){
	        case DialogInterface.BUTTON_POSITIVE:
	            plib.savecurrent=true;
	            break;

	        case DialogInterface.BUTTON_NEGATIVE:
	            plib.savecurrent=false;
	            break;
	        }
			plib.loadplaylist(null);
	        
	    }
	};
	AlertDialog.Builder dialogBuilder;
	public void onAddPlaylist(View v) {
		if (plib.lock) {return;}
		plib.lock=true;
		Intent foo = new Intent(this, TextEntryActivity.class);
		foo.putExtra("title", "New Playlist Name");
		foo.putExtra("value", "");
		this.startActivityForResult(foo, ACTION_CREATE);
	}
	public void onSavePlaylist(View v) {
		if (! loaded) {return;}
		plib.saveplaylist(null);
	}
	public OnItemClickListener pvlistener=new OnItemClickListener(){
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			if (! loaded) {return;}
			String name=(String)parent.getItemAtPosition(position);
			if (name.endsWith(" (Saved)")) {
				name=name.substring(0, name.length()-8);
			} else if (name.startsWith("*")) {
				name=name.substring(1);
			}
			Log.d("MusicPlayer", "Playlist Name: <"+name+">");
			if (name.equals(plib.playlistname)) {
				if (! plib.saved) {
					plib.saveplaylist(null);
				}
			}
			else {
				if (plib.saved) {
					plib.savecurrent=false;
					plib.loadplaylist(name);
				} else if (plib.playlistname.equals("Default")) {
					plib.savecurrent=true;
					plib.loadplaylist(name);
				} else {
					plib.loadplaylistname=name;
					dialogBuilder.setMessage("Save Current Playlist?").setPositiveButton("Yes", saveCurrentListener)
					    .setNegativeButton("No", saveCurrentListener).show();
				}
			}
		}
	};
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt("currentView", currentView);
		lib.save(outState);
	}
	@Override
	public void onDestroy() {
		boolean exiting=p.exiting();
		if (exiting) {plib.saveplaylist("Default");}
		
		super.onDestroy();
	}
	@Override
	public void onCreate(Bundle savedInstanceState) {
	 super.onCreate(savedInstanceState);
	 setContentView(R.layout.main);
	 
	 //View Initialization
     libraryhandler=new Handler(){
    	 @Override
    	 public void handleMessage(Message msg) {
    		 if (msg.what==0) {
    			 lib=p.lib;
    			 shufflestate=p.shufflestate;
    			 plib=new PlaylistLibrary(Base.this,p);
    		     lv.setAdapter(p.lAdapter);
    		     cv.setAdapter(p.cAdapter);
    		     pv.setAdapter(p.pAdapter);
    		 	((SeekBar)findViewById(R.id.seekBar)).setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
    				   @Override
    				   public void onStartTrackingTouch(SeekBar seekBar) {seeking=true;}
    				   @Override
    				   public void onStopTrackingTouch(SeekBar seekBar) {if (serviceBound) {seeking=false;p.seek(seekBar.getProgress(),seekBar.getMax());} }
    				   @Override
    				   public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {}
    			});
    			 linfo.setVisibility(View.GONE);
    			 p.b=Base.this;
    			 loaded=true;
    			 //p.lAdapter.setsearchselection();
    			 if (p.cAdapter.getCount()==0) {
    				 plib.loadplaylist("Default");
    			 }
    		 } else if (msg.arg1 != 0) {
    			 linfo.setText(getResources().getString(msg.arg1));
    			 linfo.setVisibility(View.VISIBLE);
    		 } else if (msg.arg2 != 0) {
    			return; 
    		 } else {
        		 linfo.setVisibility(View.GONE);    			 
    		 }
    	 }
     };
	 linfo=(TextView)findViewById(R.id.libraryInfo);
	 cinfo=(TextView)findViewById(R.id.currentInfo);
	 pv = (ListView) findViewById(R.id.playlistList);
     dialogBuilder = new AlertDialog.Builder(this);
     pv.setOnItemClickListener(pvlistener);
	 progresshandler = new Handler();
	 progressrunnable=new Runnable() {
	     public void run()
	     {
	         updateprogress();
	         progresshandler.postDelayed(this, 250);
	     }
	 };
	 flipper = (ViewFlipper) findViewById(R.id.flipper);
	 if (savedInstanceState != null) {
	 if (savedInstanceState.containsKey("currentView")) {
		 currentView=savedInstanceState.getInt("currentView");
	 }
	 }
	 if (currentView==1) {
		 flipper.showNext();
	 } else if (currentView==2) {
		 flipper.showPrevious();
	 }
     //service initialization
     Intent intent = new Intent(Base.this, PlayerService.class);
     startService(intent);
     bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
	 //View Initialition
	 lsearch=(EditText)findViewById(R.id.librarySearch);
	 lsearch.setOnKeyListener(librarySearchListener);
	 lv = (ListView) findViewById(R.id.libraryList);
	 cv = (ListView) findViewById(R.id.currentList);
     registerForContextMenu(pv);
	 ViewChange();
	 //new Thread(loadlibrary).start();
	}
	//swiping functions
	private void ViewChange() {
		setTitle(titles[currentView]);
		if (! loaded) {return;}
//		TextView title=(TextView)flipper.findViewById(R.id.titleCurrent);
//		if (loaded) {
//		title.setText("Welcome to the Playlist Editor: "+lib.selections.size()+" Items Selected");
//		}
	}
	public void SwipeLeft() {
		if (swipe_disabled) {return;}
		flipper.setInAnimation(AnimationUtils.loadAnimation(this, R.anim.infromright));
		flipper.setOutAnimation(AnimationUtils.loadAnimation(this, R.anim.outtoleft));
		flipper.showNext();
		currentView=(currentView+1) % 3;
		ViewChange();
	}
	public void SwipeRight() {
		if (swipe_disabled) {return;}
		flipper.setInAnimation(AnimationUtils.loadAnimation(this, R.anim.infromleft));
		flipper.setOutAnimation(AnimationUtils.loadAnimation(this, R.anim.outtoright));
		flipper.showPrevious();
		currentView=(currentView+2) % 3;// modulus operator doesn't work with negatives, so adding 2 instead of subtracting 1
		ViewChange();
	}
}