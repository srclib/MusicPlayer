package org.mstevens.musicplayer;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.Xml;
import android.widget.Toast;

public class PlaylistLibrary {
	public String playlistname="Default";
	public String loadplaylistname;
	public boolean lock=false;
	public boolean savecurrent=false;
	public ArrayList<String> names;
	private ArrayList<Integer> selections;
	private Base b;
	private PlayerService p;
	public boolean saved=true;
	public PlaylistLibrary(Base b,PlayerService p) {
		this.b=b;
		this.p=p;
		names=new ArrayList<String>();
		String[] flist = b.getApplicationContext().fileList();
		for (int i=0;i<flist.length;i++) {
			addname(flist[i]);
		}
	}
	public void lockunlockbuttons() {
		b.findViewById(R.id.addPlaylist).setEnabled(! lock);
		b.findViewById(R.id.savePlaylist).setEnabled(! (lock || saved));
		b.pv.setEnabled(! lock);
	}
	public class alphabetical implements Comparator<String>
	{
	    public int compare(String s1, String s2)
	    {
	        s1=s1.toLowerCase();
	        s2=s2.toLowerCase();
	        return s1.compareTo(s2);
	    }
	}
	private void addname(String name) {
		if (! names.contains(name)) {
			String displayname=name;
			if (name.equals(playlistname)) {
				if (saved) {
					displayname=name+" (Saved)";
				} else {
					displayname="*"+name;
				}
			}
			if (name.equals("Default") && names.size()>0) {
				names.add(0,name);
				p.pAdapter.insert(displayname,0);
			} else {
				names.add(name);
				p.pAdapter.add(displayname);
			}
			Collections.sort(names, new alphabetical());
			
		}
	}
	public void onchange() {
		if (lock) {return;}
		if (saved) {
			saved=false;
			updatenames();
		}
	}
	public void updatenames() {
		b.titles[1]="Current Playlist: "+playlistname;
		p.pAdapter.clear();
		b.findViewById(R.id.savePlaylist).setEnabled(! (lock || saved));
		String name;
 		for (int i=0;i<names.size();i++) {
			Log.d("MusicPlayer", "Playlist name "+names.get(i));
			if (names.get(i).equals(playlistname)) {
				if (saved) {
					name=names.get(i)+" (Saved)";
				} else {
					name="*"+names.get(i);
				}
			} else {
				name=names.get(i);
			}
			if (name.contains("Default") && names.size()>0) {
				p.pAdapter.insert(name, 0);
			} else {
				p.pAdapter.add(name);
			}
		}
		Collections.sort(names, new alphabetical());
	}
	private Handler updateplaylist=new Handler() {
		public void handleMessage (Message msg) {
			if (msg.what==-1) {
				lock=false;
				lockunlockbuttons();
				updatenames();
				return;
			}
			if (msg.what==1) {
				int missedcount=0;
				int index;
				b.clearall(null);
				for (int i=0;i<selections.size();i++) {
					if (selections.get(i)==-1) {
						missedcount++;
					}
					index=selections.get(i);
					b.lib.songs[index].select();
				}
				Toast.makeText(b.getApplicationContext(), ""+missedcount, Toast.LENGTH_SHORT);
			}
			p.lAdapter.updateselections();
			b.updatebuttons();
			//b.playlistChanged();
			saved=true;
			updatenames();
			lock=false;
			lockunlockbuttons();
		}
	};
	public boolean rename(String from, String to) {
		if (names.contains(to)) {return false;}
		if (! names.contains(from)) {return false;}
		if (lock) {return false;}
		try {
		lock=true;
		lockunlockbuttons();
		FileInputStream is=b.getApplicationContext().openFileInput(from);
		OutputStream os=b.getApplicationContext().openFileOutput(to, Context.MODE_PRIVATE);
	    int numRead;
	    byte[] buf=new byte[32];
	    while ( (numRead = is.read(buf) ) >= 0) {
	        os.write(buf, 0, numRead);
	    }
	    deleteplaylist(from);
	    addname(to);
	    if (playlistname.equals(from)) {playlistname=to;}
		lock=false;
		lockunlockbuttons();
	    updatenames();
	    return true;
		} catch (Exception e) {
			updateplaylist.sendEmptyMessage(-1);
			return false;
		}
	}
	public void deleteplaylist(String title) {
		if (! names.contains(title)) {
			return;
		}
		if (title != "Default") {
			names.remove(title);
		}
		try {
			b.deleteFile(title);
		} catch (Exception e) {
			Log.d("MusicPlayer", "File delete failed");
		}
		if (title.equals(playlistname)) {
			loadplaylist("Default");
		}
		updatenames();
	}
	public void loadplaylist(String title) {
		if (lock) {return;}
		lock=true;
		lockunlockbuttons();
		if (title != null) {
		loadplaylistname=title;
		}
		new Thread(doloadplaylist).start();
	}
	Runnable doloadplaylist=new Runnable() {
		public void run() {
			if (savecurrent==true) {
				Thread t = new Thread(dosaveplaylist);
				t.start();
				try {
					t.join();
				} catch (Exception e) {
					Log.d("MusicPlayer", "Save Failed");
				}
			}
			savecurrent=false;
			playlistname=loadplaylistname;
			String[] flist = b.getApplicationContext().fileList();
			boolean exists=false;
			for (int i=0;i<flist.length;i++) {
				//Log.d("MusicPlayer", "File: "+flist[i]);
				if (flist[i].equals(playlistname)) {
					exists=true;
					break;
				}
			}
			if (! exists) {
				if (playlistname.equals("Default")) {
			        selections=new ArrayList<Integer>();
			        updateplaylist.sendEmptyMessage(1);
				} else {
					Log.d("MusicPlayer","File Doesn't Exist");
				}
				updateplaylist.sendEmptyMessage(-1);
				return;
			}
			FileInputStream f;
	        selections=new ArrayList<Integer>();
			try {
				f=b.getApplicationContext().openFileInput(playlistname);
		        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
		        XmlPullParser xpp = factory.newPullParser();
		        xpp.setInput( new InputStreamReader(f) );
		         int eventType = xpp.getEventType();
		         int artistindex;
		         Library.album[] childalbums;
		         Library.song[] childsongs;
		         String tagname="";
		         String artist="";
		         String album="";
		         String song="";
		         while (eventType != XmlPullParser.END_DOCUMENT) {
		          if(eventType == XmlPullParser.START_DOCUMENT) {
		              //Log.d("MusicPlayer", "XML Document Start");
		          } else if(eventType == XmlPullParser.START_TAG) {
		              //Log.d("MusicPlayer","XML Tag Start: "+xpp.getName());
		              tagname=xpp.getName();
		          } else if(eventType == XmlPullParser.END_TAG) {
		        	  if (xpp.getName().equals("track")) {
		        		  artistindex=b.lib.binary_search_artist(artist);
		        		  if (artistindex>=0) {
		        		  childalbums=b.lib.artists[artistindex].getchildren();
		        		  exists=false;
		        		  rootlevel:
		        		  if (!(artist.equals("") || album.equals("") || song.equals(""))) {
		        		  for (int i=0;i<childalbums.length;i++) {
		        			  if (childalbums[i].name.equals(album)) {
		        				  childsongs=childalbums[i].getchildren();
		        				  for (int j=0;j<childsongs.length;j++) {
		        					  if (childsongs[j].name.equals(song)) {
		        						  selections.add(childsongs[j].index);
		        						  exists=true;
		        						  break rootlevel;
		        					  }
		        				  }
		        				  break rootlevel;
		        			  }
		        		  }
		        		  if (! exists) {
		        			  selections.add(-1);
		        		  }
		        		  }
		        		  }
		        	  }
		              //Log.d("MusicPlayer","XML Tag End: "+xpp.getName());
		          } else if(eventType == XmlPullParser.TEXT) {
		        	  if (tagname.equals("creator")) {artist=xpp.getText();}
		        	  if (tagname.equals("album")) {album=xpp.getText();}
		        	  if (tagname.equals("title")) {song=xpp.getText();}
		              //Log.d("MusicPlayer","XML Text"+xpp.getText());
		          }
		          eventType = xpp.next();
		         }
			} catch(Exception e) {
				Log.d("MusicPlayer","File Load Error");
				updateplaylist.sendEmptyMessage(-1);
				return;
			}
	        for (int k=0;k<selections.size();k++) {
	        	Log.d("MusicPlayer","Playlist Item: "+selections.get(k));
	        }
	        updateplaylist.sendEmptyMessage(1);
		}
	};
	private void serializerSet(XmlSerializer s,String key,String value) {
		try {
		s.startTag("", key);
		s.text(value);
		s.endTag("", key);
		//Log.d("MusicPlayer","Saving: "+key+","+value);
		} catch(Exception e) {
			Log.d("MusicPlayer","Playlist Not Saved");
		}
	}
	public void saveplaylist(String title) {
		if (lock) {return;}
		lock=true;
		lockunlockbuttons();
		if (title != null) {
			playlistname=title;
		}
		savecurrent=false;
		new Thread(dosaveplaylist).start();
		//Log.d("MusicPlayer","Playlist XML:\n"+writer.toString());
	}
	public void saveplaylistas(String title) {
		if (lock) {return;}
		if (title=="") {return;}
		lock=true;
		addname(title);
		lockunlockbuttons();
		playlistname=title;
		savecurrent=false;
		new Thread(dosaveplaylist).start();
		//Log.d("MusicPlayer","Playlist XML:\n"+writer.toString());
	}
	private Runnable dosaveplaylist = new Runnable() {
		public void run(){
			Integer[] songs=new Integer[p.cAdapter.items.size()];
			songs=p.cAdapter.items.toArray(songs);
			Library.song song;
			XmlSerializer serializer = Xml.newSerializer();
			try {
			OutputStream os=b.getApplicationContext().openFileOutput(playlistname, Context.MODE_PRIVATE);
			OutputStreamWriter writer = new OutputStreamWriter(os);
			serializer.setOutput(writer);
			serializer.startDocument("UTF-8", true);
			serializer.startTag("", "playlist");
			serializer.attribute("","version","1");
			serializer.attribute("","xmlns","http://xspf.org/ns/0/");
			serializerSet(serializer,"title",playlistname);
			serializer.startTag("", "trackList");
			for (int i=0;i<songs.length;i++) {
				serializer.startTag("","track");
				song=b.lib.songs[songs[i]];
				serializerSet(serializer,"location",Uri.parse(song.path).toString());
				serializerSet(serializer,"title",song.name);
				serializerSet(serializer,"creator",song.getparent().getparent().name);
				serializerSet(serializer,"album",song.getparent().name);
				serializer.endTag("","track");
			}
			serializer.endTag("","trackList");
			serializer.endTag("","playlist");
			serializer.flush();
			writer.flush();
			os.close();
			} catch(Exception e) {
				Log.d("MusicPlayer","Playlist Not Saved");
			}
			if (savecurrent==true) {
				saved=true;
			} else {
		        updateplaylist.sendEmptyMessage(2);
			}
		}
	};
	public ArrayList<Integer> getshufflemask(ArrayList<Integer> playqueue,int length) {
    	ArrayList<Integer> shufflemask=new ArrayList<Integer>(length);
    	int i=playqueue.size();
    	int pqindex;
    	for (int c=0;c<length;c++) {
	        pqindex=playqueue.indexOf(c);
	        if (pqindex>=0) {
	            shufflemask.set(pqindex,c);
	        } else {;
	            shufflemask.set(i,c);
	            i++;
	        }
	    }
	    int t;
	    Random rgen=new Random();
	    for (i=playqueue.size();i<shufflemask.size();i++) {
	        int swap=rgen.nextInt(shufflemask.size()-i)+i;    
	        t=shufflemask.get(swap);
	        shufflemask.set(swap,shufflemask.get(i));
	        shufflemask.set(i,t);
	    }
	    return shufflemask;
	}

}
