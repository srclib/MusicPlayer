package org.mstevens.musicplayer;
import java.util.ArrayList;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class LibraryAdapter extends ArrayAdapter<Library.libraryitem> {

        private ArrayList<Library.libraryitem> items;
        private int[] typeStyle={Typeface.BOLD,Typeface.ITALIC,Typeface.NORMAL};//determines type style for artist/album/song
        private int[] typeSize={24,21,18};
        private int[] typeTextPadding={12,12,6};
        private int[] typePadding={14,36,60};//determines indents for albums and songs
        private int[] typeBackground=new int[3];//determines colors for artist/album/song
        private int[] typeTextColor={0xFF000000,0xFFFFFFFF,0xFFFFFFFF};//determines colors for artist/album/song
        private int otherPadding=14;
        public int expandedcount=0;
        private OnLongClickListener longClickListener;
        private OnClickListener clickListener;
        //private Base b;
        private PlayerService p;
        private OnClickListener checkListener;
        private Library lib;
        //private ListView lv;
        public boolean searching=false;
        //private int searchselection=-1;
/*        public void togglesearch() {
        	if (searching) {
        		stopsearching();
        	} else {
        		startsearching();
        	}
        }
        public void startsearching() {
        	if (searching) {return;}
        	b.lsearch.setVisibility(View.VISIBLE);
        	b.lsearch.requestFocus();
        	InputMethodManager mgr = (InputMethodManager) b.getSystemService(Context.INPUT_METHOD_SERVICE);
        	mgr.showSoftInput(b.lsearch, InputMethodManager.SHOW_IMPLICIT);
			b.libraryhandler.sendMessage(Message.obtain(b.libraryhandler, 1, R.string.search_text, 0));
        	b.swipe_disabled=true;
        	clear();
        	searching=true;
        }
        public void stopsearching() {
        	if (! searching) {return;}
        	searching=false;
        	b.lsearch.setVisibility(View.GONE);
        	InputMethodManager mgr = (InputMethodManager) b.getSystemService(Context.INPUT_METHOD_SERVICE);
        	mgr.hideSoftInputFromWindow(b.lsearch.getWindowToken(), 0);
        	clear();
        	b.libraryhandler.sendMessage(Message.obtain(b.libraryhandler, 0));
        	b.swipe_disabled=false;
        }
        public void dosearch(String pattern) {
        	startsearching();
        	b.linfo.setVisibility(View.GONE);
        	clear();
        	ArrayList<Library.libraryitem> searchitems=lib.search(pattern);
        	for (int i=0;i<searchitems.size();i++) {
        		add(searchitems.get(i));
        	}
        	if (getCount()==0) {
    			b.libraryhandler.sendMessage(Message.obtain(b.libraryhandler, 1, R.string.not_found, 0));
        	} else {
            	InputMethodManager mgr = (InputMethodManager) b.getSystemService(Context.INPUT_METHOD_SERVICE);
            	mgr.hideSoftInputFromWindow(b.lsearch.getWindowToken(), 0);
        	}
        }
        public void setsearchselection() {
        	if (searchselection < 0) {return;}
        	lv.setSelection(lib.positionfromuid(searchselection)-2);
        	searchselection=-1;
        }*/
        public void expandall() { //collapses all expanded artists/albums
        	for (int i=0;i<lib.artistcount;i++) {
        		lib.artists[i].expanded=true;
        	}
        	for (int i=0;i<lib.albumcount;i++) {
        		lib.albums[i].expanded=true;
        	}
        	clear();
        	addAll();
        	expandedcount=lib.artistcount;
        }
        public void closeall() { //collapses all expanded artists/albums
        	for (int i=0;i<this.items.size();i++) {
        		Library.libraryitem l = this.items.get(i);
            	if (l.type==0) { // is an artist
            		lib.artists[l.index].expanded=false;
            	} else if(l.type>0) { //is an album or song.
            		l.resetpos();
            		remove(l);
            		i--;
            	}
        	}
        	for (int i=0;i<lib.albumcount;i++) {
        		lib.albums[i].expanded=false;
        	}
        	expandedcount=0;
        	updatepositions();
        }
        public void selectall() { //collapses all expanded artists/albums
        	for (int i=0;i<lib.songcount;i++) {
        		lib.songs[i].select();
            }
        	for (int i=0;i<this.items.size();i++) {
        		this.items.get(i).selected=2;
            }
        }
        public void clearall() {
      	  for (int i=0;i<getCount();i++) {
    		  getItem(i).selected=0;
    	  }
        }
        public void updateselections() {//run through visible items and set their positions
        	for (int i=0;i<this.items.size();i++) {//items no longer visible may still have nonzero positions
        		Library.libraryitem l = this.items.get(i);
            	if (l.type==0) {
            		l.selected=lib.artists[l.index].selected;
            	} else if (l.type==1) {
            		l.selected=lib.albums[l.index].selected;
            	} else if (l.type==2) {
            		l.selected=lib.songs[l.index].selected;
            	}
            	p.updatechecks();
        	} 
        }
        public void updatepositions() {//run through visible items and set their positions
        	for (int i=0;i<this.items.size();i++) {//items no longer visible may still have nonzero positions
        		Library.libraryitem l = this.items.get(i);
            	if (l.type==0) {
            		lib.artists[l.index].position=i;
            	} else if (l.type==1) {
            		lib.albums[l.index].position=i;
            	} else if (l.type==2) {
            		lib.songs[l.index].position=i;
            	}
        	} 
        }

        public LibraryAdapter(Context context, int textViewResourceId, ArrayList<Library.libraryitem> items,PlayerService p) {
            super(context, textViewResourceId, items);
            	this.p=p;
                this.lib=p.lib;
                this.items = items;
                this.typeBackground[0]=p.getResources().getColor(R.color.artistbg);
                this.typeBackground[1]=p.getResources().getColor(R.color.albumbg);
                this.typeBackground[2]=p.getResources().getColor(R.color.songbg);
                this.longClickListener = new OnLongClickListener() {
                	public boolean onLongClick(View v) {
                		longclick(v);
                		return true;
                	}
                };
                this.clickListener = new OnClickListener(){
					public void onClick(View v) {
						click(v);
					}
		        };
                this.checkListener = new OnClickListener(){
					public void onClick(View v) {
						check((ImageView)v);
					}
		        };
        }
        public void add(Library.libraryitem item,int pos) {//like insert, convenience function to avoid index out of range error
        	if (pos>=items.size()) {
        		add(item);
        	} else {
        		insert(item,pos);
        	}
        }
        public void addAll() {
//        	if (searching) {
//        	clear();
//        	for (int i=0;i<itemscopy.size();i++) {
//        		add(itemscopy.get(i));
//        	}
//        	searching= false;
//        	return;
//        	}
    		for (int i=0;i<lib.artistcount;i++) {
    			addArtistEnd(lib.artists[i]);//artists will recursively add albums if necessary
    		}
        }
        public void addArtistEnd(Library.artist artist) {
        	artist.position = this.items.size();
        	this.add(artist.toitem());
        	if (artist.expanded) {
        		expandedcount++;
        		Library.album[] children = artist.getchildren();
        		for (int i=0;i<children.length;i++) {
        			addAlbumEnd(children[i]);
        		}
        	}
        }
        public void addAlbumEnd(Library.album album) {
        	album.position = this.items.size();
        	this.add(album.toitem());
        	if (album.expanded) {
        		Library.song[] children = album.getchildren();
        		for (int i=0;i<children.length;i++) {
        			addSongEnd(children[i]);
        		}
        	}
        }
        public void addSongEnd(Library.song song) {
        	song.position = this.items.size();
        	this.add(song.toitem());
        }
        public void check(ImageView cb) {
			if (p.wasswipe) {
				return;
			}
        	int uid=(Integer)((View)cb.getParent()).getTag();
        	int type=lib.typefromuid(uid);
        	int index=lib.indexfromuid(uid);
        	if (type==0) {
        		Library.artist artist=lib.artists[index];
        		artist.select();
        		int pos=artist.position;
        		Library.libraryitem l=this.items.get(pos);
        		l.selected=artist.selected;
        		pos++;
        		l=this.items.get(pos);
        		while (l.type>0) {
        			l.selected=lib.selectedfromuid(l.uid());
            		pos++;
            		if (pos>=this.items.size()) {break;}
            		l=this.items.get(pos);
        		}
        		p.updatechecks();
//        		Library.album[] albums=artist.getchildren();
//        		Library.album album;
//        		Library.song[] songs;
//        		Library.song song;
//        		for (int i=0;i<artist.childcount;i++){
//        			album=albums[i];
//        			applySelection(album.position, album.selected);
//        			songs=album.getchildren();
//            		for (int j=0;j<album.childcount;j++){
//            			song=songs[j];
//            			applySelection(song.position, song.selected);
//            		}
//        		}
//        		cb.setChecked(artist.selected);
        	} else if (type==1) {
        		Library.album album=lib.albums[index];
        		Library.artist artist=album.getparent();
        		//Library.song[] songs=album.getchildren();
        		//Library.song song;
        		album.select();
        		int pos=album.position;
        		this.items.get(pos).selected=album.selected;
        		this.items.get(artist.position).selected=artist.selected;
//    			applySelection(artist.position, artist.selected);
        		pos++;
        		Library.libraryitem l=this.items.get(pos);
        		while (l.type>1) {
        			l.selected=lib.selectedfromuid(l.uid());
            		pos++;
            		if (pos>=this.items.size()) {break;}
            		l=this.items.get(pos);
        		}
        		p.updatechecks();
//        		for (int i=0;i<album.childcount;i++){
//        			song=songs[i];
//        			applySelection(song.position, song.selected);
//        		}
//        		cb.setChecked(album.selected);
        	} else if (type==2) {
        		Library.song song=lib.songs[index];
        		Library.album album=song.getparent();
        		Library.artist artist=album.getparent();
        		song.selectdeselect();
        		this.items.get(song.position).selected=song.selected;
        		this.items.get(album.position).selected=album.selected;
        		this.items.get(artist.position).selected=artist.selected;
        		p.updatechecks();
//        		cb.setChecked(lib.songs[index].selected);
//    			applySelection(album.position, album.selected);
//    			applySelection(artist.position, artist.selected);
        	}
        	
    		p.updatebuttons();
    		//p.cAdapter.sendtoservice();
    		p.playlist_onchange();
        }
        public void click(View v) {
			if (p.wasswipe) {
				return;
			}
        	int uid=(Integer)v.getTag();
        	int type=lib.typefromuid(uid);
        	int index=lib.indexfromuid(uid);
        	/*if (searching) {
	        	if (type==0) {
	        		Library.artist a=lib.artists[index];
	        		a.expanded=true;
	        		searchselection=a.uid();
	        	} else if (type==1) {
	        		Library.album a= lib.albums[index];
	        		a.getparent().expanded=true;
	        		a.expanded=true;
	        		searchselection=a.uid();
	        	} else if (type==2) {
	        		Library.song s = lib.songs[index];
	        		s.getparent().getparent().expanded=true;
	        		s.getparent().expanded=true;
	        		searchselection=s.uid();
	        	}
        		stopsearching();
        	} else {*/
	        	if (type==0) {
	        		Library.artist artist = lib.artists[index];
	        		artist.expandclose(this);
	        		if (artist.expanded) {
	        			expandedcount++;
	        		} else {
	        			expandedcount--;
	        		}
	        	} else if (type==1) {
	        		lib.albums[index].expandclose(this);
	        	} else if (type==2) {
	        		Library.song song = lib.songs[index];
	        		song.selectdeselect();
	        		this.items.get(song.position).selected=song.selected;
	        		p.applySelection(song.position,song.selected);
	        		p.applySelection(song.getparent().position,song.getparent().selected);
	        		p.applySelection(song.getparent().getparent().position,song.getparent().getparent().selected);
	        		p.playlist_onchange();
	        		//p.cAdapter.sendtoservice();
	        	}
        	//}
    		p.updatebuttons();
        }
        public void longclick(View v) {
//        	EditText e=(EditText)b.findViewById(R.id.librarySearch);
//        	if (searching) {
//        		stopsearching();
//            	e.setVisibility(View.GONE);
//        	} else {
//        		startsearching();
//            	e.setVisibility(View.VISIBLE);
//        	}
        	int uid=(Integer)v.getTag();
        	int type=lib.typefromuid(uid);
        	int index=lib.indexfromuid(uid);
        	if (type==0) {
        		Library.artist artist=lib.artists[index];
        		artist.expandcloseall(this);
	    		if (artist.expanded) {
					expandedcount++;
				} else {
					expandedcount--;
				}
	    		p.updatebuttons();
        	} else {
        		click(v);
        		lib.songs[index].play();
        	}
        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
        		int vtype=-1;
                View v = convertView;
            	LayoutInflater vi = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                Library.libraryitem o = items.get(position);
                if (v != null) {
                	vtype=lib.typefromuid((Integer)v.getTag());
                } else {
                vtype=-1;
            	if (o==null) {
                	v = vi.inflate(R.layout.librarylistitem, null);
                	return v;
            	} else {
                	v = vi.inflate(R.layout.librarylistitem, null);
                }
            	if (! searching) {
                v.setOnLongClickListener(longClickListener);
            	}
                v.setOnClickListener(clickListener);
                ((ImageView) v.findViewById(R.id.libraryListBox)).setOnClickListener(checkListener);;
                } 
                TextView tt = (TextView) v.findViewById(R.id.libraryListText);
            	if (o.type != vtype) {
            		v.setBackgroundColor(typeBackground[o.type]);
            		v.setPadding(typePadding[o.type], 0, otherPadding, 0);
            		tt.setPadding(0, typeTextPadding[o.type], 0, typeTextPadding[o.type]);
            		tt.setTypeface(null, typeStyle[o.type]);
            		tt.setTextColor(typeTextColor[o.type]);
            		tt.setTextSize(typeSize[o.type]);
            	}
        		v.setTag(o.uid());
                if (tt != null) {
                      tt.setText(o.name);
                }
          		ImageView cb=(ImageView)v.findViewById(R.id.libraryListBox);
                if (o.selected==0) {
                	cb.setImageResource(R.drawable.unchecked);
                } else if (o.selected==1) {
                	cb.setImageResource(R.drawable.halfchecked);
                } else {
                	cb.setImageResource(R.drawable.checked);
                }
                return v;
        }
}