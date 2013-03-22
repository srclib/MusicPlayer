package org.mstevens.musicplayer;
import java.util.ArrayList;
import java.util.Arrays;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class CurrentAdapter extends ArrayAdapter<Integer> {

        public ArrayList<Integer> items;
        public ArrayList<String> paths;
        //private ArrayList<CharSequence> albumartists;
        //private Drawable[] bg;
        //private OnLongClickListener longClickListener;
        private OnClickListener clickListener;
        //private Base b;
        private PlayerService p;
        private Library lib;
        //private ListView cv;
        public ArrayList<Integer> getindices() {
        	return items;
        }
        public ArrayList<String> getpaths() {
        	ArrayList<String> paths=new ArrayList<String>();
        	for (int i=0;i<getCount();i++) {
        		paths.add(lib.songs[getItem(i)].path);
        	}
        	return paths;
        }
        public void pathmove(int from,int to) {
			String path=paths.get(from);
			paths.remove(path);
			if (from<to) {
				to--;
			}
        	paths.add(to,path);
        }
        public void move(int from, int to) {
        	pathmove(from,to);
			int item=items.get(from);
			remove(item);
			if (from<to) {
				to--;
			}
        	insert(item,to);
        	updatepositions();
        }

        public void updatepositions() {//run through visible items and set their positions
        	for (int i=0;i<this.items.size();i++) {//items no longer visible may still have nonzero positions
        		int index = this.items.get(i);
            	lib.songs[index].playlistposition=i;
        	} 
        }
        public CurrentAdapter(Context context, int textViewResourceId, ArrayList<Integer> items,PlayerService p) {
            super(context, textViewResourceId, items);
            	this.p=p;
                this.lib=p.lib;
                //this.cv=b.cv;
                this.items = items;
                this.paths=new ArrayList<String>();
                //this.bg=new Drawable[2];
                //this.albumartists = new ArrayList<CharSequence>();
                //this.names = new ArrayList<String>();
//                this.longClickListener = new OnLongClickListener() {
//                	public boolean onLongClick(View v) {
//                		longclick(v);
//                		return true;
//                	}
//                };
                this.clickListener = new OnClickListener(){
					public void onClick(View v) {
						click(v);
					}
		        };
//                this.checkListener = new OnClickListener(){
//					public void onClick(View v) {
//						check((CheckBox)v);
//					}
//                };
        }
        public void sort() {
        	if (items.size()<=1){return;}
        	Integer[] itemarray=new Integer[items.size()];
        	itemarray=(Integer[])items.toArray(itemarray);
        	Arrays.sort(itemarray);
        	clear();
        	for (int i=0;i<itemarray.length;i++) {
        		add(itemarray[i]);
        	}
        	paths=getpaths();
        }
        @Override
        public void add(Integer object) {
        	//albumartists.add(lib.songs[object].getparent().formatted);
        	//names.add(lib.songs[object].name);
        	Library.song s=lib.songs[object];
        	s.playlistposition=items.size();
        	paths.add(s.path);
        	super.add(object);
        }
        @Override
        public void insert(Integer object, int pos) {
        	//albumartists.add(lib.songs[object].getparent().formatted);
        	//names.add(lib.songs[object].name);
        	if (pos<getCount()) {
            	Library.song s=lib.songs[object];
        		s.playlistposition=pos;
        		paths.add(pos, s.path);
        		super.insert(object,pos);
        	} else if (pos<0) {
        		return;
        	} else {
        		add(object);
        	}
        }
        @Override
        public void remove(Integer object) {
        	lib.songs[object].playlistposition=-1;
        	paths.remove(lib.songs[object].path);
        	super.remove(object);
        }
        public void clearall() {
        int item;
        while (getCount()>0) {
          item=getItem(0);
    	  lib.songs[item].deselect();
        }
        }
        public void addSongEnd(Library.song song) {
        	this.add(song.index);
        }
        public void click(View v) {
			if (p.wasswipe) {
				return;
			}
			v.refreshDrawableState();
			int[] coords=new int[2];
			v.getLocationOnScreen(coords);
			Log.d("MusicPlayer","Height: "+v.getHeight()+","+v.getMeasuredHeight()+","+coords[1]);
    		int index=(Integer)v.getTag();
    		int pos=this.items.indexOf(index);
    		Log.d("Touch Event", "Box Click. 2finger="+p.is2fingerstate);
    		if (p.is2fingerstate>0){return;}
    		if (p.is2fingerstate !=0 && pos==items.size()-1) {
    			p.screenYpos=2;
    		}
        	Library.song song=lib.songs[(Integer)v.getTag()];
        	//b.setnowplayingcolor(song.index);
        	//b.nowplaying=song.index;
        	//b.setplayingtext();
        	p.play(song.playlistposition);
        	Log.d("MusicPlayer", "Song "+index+" clicked");
        	//b.pService.play(song.playlistposition);
        }
        public Drawable getbg(int pos) {
            if (pos%2==0) {
            	return p.getResources().getDrawable(R.drawable.currentbg1);
            } else {
            	return p.getResources().getDrawable(R.drawable.currentbg2);
            }
          }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
                View v = convertView;
                int o = items.get(position);
                if (v == null) {
                	LayoutInflater vi = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    v = vi.inflate(R.layout.currentlistitem, null);
                }
                v.setBackgroundDrawable(getbg(position));
                p.registerForContextMenu(v);
                v.setOnClickListener(clickListener);
                Object lasttag=v.getTag();
                int lastindex=-1;
                if (lasttag != null) {
                	lastindex=(Integer)lasttag;
                }
        		v.setTag(o);
                TextView songtext = (TextView) v.findViewById(R.id.currentListSongText);
                int pq=p.pqindex(o);
                String pqstring="";
                if (pq>=0) {
                	pqstring=String.valueOf(pq+1)+": ";
                }
                if (songtext != null) {
                    songtext.setText(pqstring+lib.songs[o].name);
                }
                TextView albumartisttext = (TextView) v.findViewById(R.id.currentListArtistAlbumText);
                if (albumartisttext != null) {
                    albumartisttext.setText(lib.songs[o].getparent().formatted);
                    //Log.d("MusicPlayer", albumartists.get(position).toString());
                }
                if (p.nowplaying==o) {
                    songtext.setTextColor(p.getResources().getColorStateList(R.color.playlisttextcolor));
                    albumartisttext.setTextColor(p.getResources().getColorStateList(R.color.playlisttextcolor));
                } else if (p.nowplaying==lastindex) {
                	songtext.setTextColor(0xffffffff);
                	albumartisttext.setTextColor(0xffffffff);
                }
                return v;
        }
}