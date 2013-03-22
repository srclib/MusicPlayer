package org.mstevens.musicplayer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;

import android.content.ContentResolver;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Html;
import android.util.Log;

public class Library {
	public int artistcount;
	public int albumcount;
	public int songcount;
	private Hashtable<String,Integer> artistkey;
	private Hashtable<String,Integer> albumkey;
	//public ArrayList<Integer> selections;
	public artist[] artists;
	public album[] albums;
	public song[] songs;
	private String lastalbum;
	private int albumindex;
	public long RunTime;
	private PlayerService p;
	private ContentResolver cr;
	public void save(Bundle b) {
		boolean[] albumexpanded=new boolean[albumcount];
		boolean[] artistexpanded=new boolean[artistcount];
		for (int i=0;i<albumcount;i++) {
			albumexpanded[i]=albums[i].expanded;
		}
		for (int i=0;i<artistcount;i++) {
			artistexpanded[i]=artists[i].expanded;
		}
//		b.putIntegerArrayList("selections", this.b.cAdapter.items);
		b.putBooleanArray("albumexpanded", albumexpanded);
		b.putBooleanArray("artistexpanded", artistexpanded);
	}
	/*public void load(Bundle b) {
		if (b==null) {return;}
		if ( ! (b.containsKey("albumexpanded") && b.containsKey("artistexpanded") && b.containsKey("selections") )) {
			return;
		}
		boolean[] albumexpanded=b.getBooleanArray("albumexpanded");
		boolean[] artistexpanded=b.getBooleanArray("artistexpanded");
		ArrayList<Integer> selections=b.getIntegerArrayList("selections");
		if (albumcount != albumexpanded.length || artistcount != artistexpanded.length) {
			return;
		}
		for (int i=0;i<albumcount;i++) {
			albums[i].expanded=albumexpanded[i];
		}
		for (int i=0;i<artistcount;i++) {
			artists[i].expanded=artistexpanded[i];
		}
		int index;
		for (int i=0;i<selections.size();i++) {
			index=selections.get(i);
			if (index<songcount) {
				songs[index].select();
			} else {
				Log.d("MusicPlayer", "Library Changed");
				return;
			}
		}
	}*/
    public class playlistitem {
    	public int index;
    	public String name;
    	public String album;
    	public String artist;
    	public playlistitem(int index) {
    		this.index=index;
    		this.name=songs[index].name;
    		this.album=songs[index].getparent().name;
    		this.artist=songs[index].getparent().getparent().name;
    	}
    	public playlistitem(int index,String name, String album, String artist) {
    		this.index=index;
    		this.name=name;
    		this.album=album;
    		this.artist=artist;
    	}
    }
	public class libraryitem {
		public String name;
		public int type;//0 artist, 1 album, 2 song
		public int index;
		public int selected;
		public libraryitem(String name,int type,int index, int selected) {
			this.name=name;
			this.type=type;
			this.index=index;
			this.selected=selected;
		}
		public int uid() {//simplistic uid system. uids become invalid whenever library changes
			if (type==0) {
				return index;
			} else if (type==1) {
				return index+artistcount;
			} else {
				return index+artistcount+albumcount;
			}
		}
		public void resetpos() {//convenience function when items deleted from view
			if (this.type==0) {
				artists[index].position=-1;
			} else if (this.type==1) {
				albums[index].position=-1;
			} else if (this.type==2) {
				songs[index].position=-1;
			}
		}
	}
	public int binary_search_artist(String artist) {
		int leftbound=0;
		int rightbound=artistcount-1;
		int testpos;
		int compval;
		boolean found=false;
		while (! found) {
			testpos=leftbound+(rightbound-leftbound)/2;
			Log.d("BinarySearch","Testing at "+testpos);
			if (rightbound-leftbound<=2) {
				if (artist.equals(artists[testpos].name)) {
					return testpos;
				} else if (artist.equals(artists[leftbound].name)) {
					return leftbound;
				} 
				else if (artist.equals(artists[rightbound].name)) {
					return rightbound;
				}
				return -1;
			}
			compval=artist.compareTo(artists[testpos].name);
			Log.d("BinarySearch","Compval "+compval);
			if (compval==0) {
				return testpos;
			} else if (compval<0) {
				rightbound=testpos-1;
			} else {
				leftbound=testpos+1;
			}
		}
		return -1;
	}
	public int search_func(String name,String pattern){
		boolean startswith=false;
		int spaceindex=0;
		name=name.toLowerCase();
		startswith=name.startsWith(pattern);
		spaceindex=name.indexOf(" ");
		while (spaceindex>=0 && startswith==false) {
			name=name.substring(spaceindex+1);
			startswith=name.startsWith(pattern);
			spaceindex=name.indexOf(" ");
		}
		if (startswith) {return 1;}
		if (name.contains(pattern)) {return 2;}
		return 0;
	}
	public ArrayList<Integer> searchselection(String pattern,ArrayList<Integer> indices) {
		ArrayList<Integer> startswithlist = new ArrayList<Integer>();
		ArrayList<Integer> containslist = new ArrayList<Integer>();
		pattern=pattern.toLowerCase();
		int retval;
    	Integer[] indexarray=new Integer[indices.size()];
    	indexarray=(Integer[])indices.toArray(indexarray);
    	Arrays.sort(indexarray);
		song s;
		for (int i=0;i<indexarray.length;i++) {
			s=songs[indexarray[i]];
			retval=search_func(s.name,pattern);
			if (retval==1) {startswithlist.add(s.index);}
			else if (retval==2) {containslist.add(s.index);}
			else {
				retval=search_func(s.getparent().name,pattern);
				if (retval==1) {startswithlist.add(s.index);}
				else if (retval==2) {containslist.add(s.index);}
				else {
					retval=search_func(s.getparent().getparent().name,pattern);
					if (retval==1) {startswithlist.add(s.index);}
					else if (retval==2) {containslist.add(s.index);}
				}
			}
		}
		startswithlist.addAll(containslist);
		return startswithlist;
	}
	public ArrayList<Library.libraryitem> search(String pattern) {
		ArrayList<Library.libraryitem> startswithlist = new ArrayList<Library.libraryitem>();
		ArrayList<Library.libraryitem> containslist = new ArrayList<Library.libraryitem>();
		pattern=pattern.toLowerCase();
		int lastartist=-1;
		int lastalbum=-1;
		int retval;
		song s;
		for (int i=0;i<songcount;i++) {
			s=songs[i];
			if (s.parent != lastalbum) {
				if (s.getparent().parent != lastartist) {
					retval=search_func(s.getparent().getparent().name,pattern);
					if (retval==1) {startswithlist.add(s.getparent().getparent().toitem());}
					else if (retval==2) {containslist.add(s.getparent().getparent().toitem());}
					lastartist=s.getparent().parent;
				}
				retval=search_func(s.getparent().name,pattern);
				if (retval==1) {startswithlist.add(s.getparent().toitem());}
				else if (retval==2) {containslist.add(s.getparent().toitem());}
				lastalbum=s.parent;
			}
			retval=search_func(s.name,pattern);
			if (retval==1) {startswithlist.add(s.toitem());}
			else if (retval==2) {containslist.add(s.toitem());}
		}
		startswithlist.addAll(containslist);
		return startswithlist;
	}
	public int artistindexfromuid(int uid) {
			if (uid>=artistcount+albumcount) {
				return songs[uid-artistcount-albumcount].getparent().parent;
			} else if (uid>=artistcount) {
				return albums[uid-artistcount].parent;
			} else {
				return uid;
			}
		}
	public int typefromuid(int uid) {
		if (uid>=artistcount+albumcount) {
			return 2;
		} else if (uid>=artistcount) {
			return 1;
		} else if (uid<songcount+albumcount+artistcount) {
			return 0;
		}
		return -1;
	}
	public int indexfromuid(int uid) {//convenience function, unused
		if (uid>=artistcount+albumcount) {
			return uid-artistcount-albumcount;
		} else if (uid>=artistcount) {
			return uid-artistcount;
		} else {
			return uid;
		}
	}
	public libraryitem fromuid(int uid) {//convenience function, unused
		if (uid>=artistcount+albumcount) {
			return songs[uid-artistcount-albumcount].toitem();
		} else if (uid>=artistcount) {
			return albums[uid-artistcount].toitem();
		} else {
			return artists[uid].toitem();
		}
	}
	public int positionfromuid(int uid) {//convenience function, unused
		if (uid>=artistcount+albumcount) {
			return songs[uid-artistcount-albumcount].position;
		} else if (uid>=artistcount) {
			return albums[uid-artistcount].position;
		} else {
			return artists[uid].position;
		}
	}
	public int selectedfromuid(int uid) {//convenience function, unused
		if (uid>=artistcount+albumcount) {
			return songs[uid-artistcount-albumcount].selected;
		} else if (uid>=artistcount) {
			return albums[uid-artistcount].selected;
		} else {
			return artists[uid].selected;
		}
	}
	public class artist {
		public ArrayList<Integer> children;
		public int childcount;
		public int index;
		public int position;
		public String key;
		public String name;
		public int selectedcount=0;
		public int selected=0;
		public boolean halfselected=false;
		public boolean expanded=false;
		public artist(String artistname, int index,String key) {
			this.index=index;
			this.key=key;
			position=-1;
			children=new ArrayList<Integer>();
			childcount=0;
			name=artistname;
		}
		public void addchild(int index) {
			if (! children.contains(index)) {
				children.add(index);
				childcount++;
				}
			
		}
		public album[] getchildren() {
			album[] retval=new album[childcount];
			for (int i=0;i<childcount;i++) {
				retval[i]=albums[(Integer)children.get(i)];
			}
			return retval;
		}
		public libraryitem toitem() {
			return new libraryitem(name, 0, index, selected);
		}
		public void expandclose(LibraryAdapter l) {
			expanded= ! expanded;
			setExpand(l);
		}
		public void expand(LibraryAdapter l) {
			if (expanded==false) {
			expanded = true;
			setExpand(l);
			}
		}
		public void close(LibraryAdapter l) {
			if (expanded==true) {
			expanded = false;
			setExpand(l);
			}
		}
		public void setExpand(LibraryAdapter l) { //performs expansion, does not change expanded value. preferably use handler functions above
			if (expanded) {
				int currpos = 1;
				if (childcount==1) {
					albums[(Integer)children.get(0)].expanded=true;//Expands albums when they are the only one
				}
				for (int i=0;i<childcount;i++) {
					l.add(albums[(Integer)children.get(i)].toitem(),position+currpos);
					albums[(Integer)children.get(i)].position=position+currpos;
					currpos++;
					if (albums[(Integer)children.get(i)].expanded) {
						albums[(Integer)children.get(i)].setExpand(l);
						currpos+=albums[(Integer)children.get(i)].childcount;
					}
				}
			} else {
				libraryitem item;
				boolean keep_running=true;
				while (keep_running) {
					if (position+1<l.getCount()) {
					item=(libraryitem)l.getItem(position+1);
					if (item.type == 0) {
						keep_running=false;
					} else {
						item.resetpos();
						l.remove(item);
					}
					} else {keep_running=false;}
				}
			}
			l.updatepositions();
		}
		public void expandcloseall(LibraryAdapter l) {
			expanded= ! expanded;
			setExpandAll(l);
		}
		public void expandall(LibraryAdapter l) {
			if (expanded==false) {
			expanded = true;
			setExpandAll(l);
			}
		}
		public void closeall(LibraryAdapter l) {
			if (expanded==true) {
			expanded = false;
			setExpandAll(l);
			}
		}
		public void setExpandAll(LibraryAdapter l) { //performs expansion, does not change expanded value. preferably use handler functions above
			if (expanded) {
				int currpos=1;
				for (int i=0;i<childcount;i++) {
					l.add(albums[(Integer)children.get(i)].toitem(),position+currpos);
					albums[(Integer)children.get(i)].position=position+currpos;
					albums[(Integer)children.get(i)].expanded=true;
					albums[(Integer)children.get(i)].setExpand(l);
					currpos++;
					currpos+=albums[(Integer)children.get(i)].childcount;
				}
			} else {
				libraryitem item;
				boolean keep_running=true;
				while (keep_running) {
					if (position+1<l.getCount()) {
					item=(libraryitem)l.getItem(position+1);
					if (item.type==1) {
						albums[item.index].expanded=false;
					}
					if (item.type == 0) {
						keep_running=false;
					} else {
						item.resetpos();
						l.remove(item);
					}
					} else {keep_running=false;}
				}
			}
			l.updatepositions();
		}
		public int select() { //cycles through selection modes. default behavior with mixed selection is to select all, though this could be changed
			int totalselectedcount=0;
			int totalchildcount=0;
			for (int i=0;i<childcount;i++) {
				totalselectedcount+=albums[(Integer)children.get(i)].selectedcount;
				totalchildcount+=albums[(Integer)children.get(i)].childcount;
			}
			if (selected==0) {
				selected=2;
				selectedcount=childcount;
				for (int i=0;i<childcount;i++) {
					albums[(Integer)children.get(i)].parentselected();
				}
				return selected;
			} else if (totalselectedcount < totalchildcount) {
				selectedcount=childcount;
				for (int i=0;i<childcount;i++) {
					albums[(Integer)children.get(i)].parentselected();
				}
				selected=2;
				return selected;
			} else {
				selected=0;
				selectedcount=0;
				for (int i=0;i<childcount;i++) {
					albums[(Integer)children.get(i)].parentdeselected();
				}
				return selected;
			}
		}
		public void childselected() {
			int totalselectedcount=0;
			int totalchildcount=0;
			selectedcount=0;
			for (int i=0;i<childcount;i++) {
				if(albums[(Integer)children.get(i)].selected==2) {selectedcount++;};
				totalselectedcount+=albums[(Integer)children.get(i)].selectedcount;
				totalchildcount+=albums[(Integer)children.get(i)].childcount;
			}
			halfselected=(totalselectedcount<totalchildcount);
			if (totalselectedcount<totalchildcount) {selected=1;}
			else {selected=2;}
		}
		public void childdeselected() {
			int totalselectedcount=0;
			int totalchildcount=0;
			selectedcount=0;
			for (int i=0;i<childcount;i++) {
				if(albums[(Integer)children.get(i)].selected==2) {selectedcount++;};
				totalselectedcount+=albums[(Integer)children.get(i)].selectedcount;
				totalchildcount+=albums[(Integer)children.get(i)].childcount;
			}
			if (totalselectedcount==0) {selected=0;}
			else if (totalselectedcount<totalchildcount) {selected=1;}
		}
		public int uid() {
				return index;
			}
	}
	public class album {
		public ArrayList<Integer> children;
		public int index;
		public int parent;
		public int position;
		public String key;
		public int childcount;
		public String name;
		public int selectedcount;
		public int selected=0;
		public boolean expanded=false;
		public CharSequence formatted;
		public album(String albumname, int index,String key) {
			this.index=index;
			this.key=key;
			position=-1;
			children=new ArrayList<Integer>();
			childcount=0;
			name=albumname;
		}
		public void addparent(int p) {
			parent=p;
			this.formatted=Html.fromHtml("by <b>"+getparent().name+"</b> on <i>"+name+"</i>");
		}
		public void addchild(int index) {
			if (! children.contains(index)) {
			children.add(index);
			childcount++;
			}
		}
		public artist getparent() {
			return artists[parent];
		}
		public song[] getchildren() {
			song[] retval=new song[childcount];
			for (int i=0;i<childcount;i++) {
				retval[i]=songs[(Integer)children.get(i)];
			}
			return retval;
		}
		public libraryitem toitem() {
			return new libraryitem(name, 1, index, selected);
		}
		public int select() {//cycles through selection modes. default behavior with mixed selection is to select all, though this could be changed
			if (selected==0) {
				selected=2;
				selectedcount=childcount;
				artists[parent].childselected();
				for (int i=0;i<childcount;i++) {
					songs[(Integer)children.get(i)].parentselected();
				}
				return selected;
			} else if (selected==1) {
				selectedcount=childcount;
				for (int i=0;i<childcount;i++) {
					songs[(Integer)children.get(i)].parentselected();
				}
				selected=2;
				artists[parent].childselected();
				return selected;
			} else {
				selected=0;
				selectedcount=0;
				for (int i=0;i<childcount;i++) {
					songs[(Integer)children.get(i)].parentdeselected();
				}
				artists[parent].childdeselected();
				return selected;
			}
		}
		public void childselected() {
			selectedcount++;
			if (selectedcount<childcount) {selected=1;} 
			else if (selectedcount==childcount) {selected=2;}
			artists[parent].childselected();
		}
		public void childdeselected() {
			selectedcount--;
			if (selectedcount==0) {
				selected=0;
			} else {
				selected=1;
			}
			artists[parent].childdeselected();
		}
		public void parentselected() {
			selected=2;
			selectedcount=childcount;
			for (int i=0;i<childcount;i++) {
				songs[(Integer)children.get(i)].parentselected();
			}
		}
		public void parentdeselected() {
			selected=0;
			selectedcount=0;
			for (int i=0;i<childcount;i++) {
				songs[(Integer)children.get(i)].parentdeselected();
			}
		}
		public void expandclose(LibraryAdapter l) {
			expanded= ! expanded;
			setExpand(l);
		}
		public void expand(LibraryAdapter l) {
			if (expanded==false) {
			expanded = true;
			setExpand(l);
			}
		}
		public void close(LibraryAdapter l) {
			if (expanded==true) {
			expanded = false;
			setExpand(l);
			}
		}
		public void setExpand(LibraryAdapter l) { //performs expansion, does not change expanded value. preferably use handler functions above
			if (expanded) {
				for (int i=0;i<childcount;i++) {
					l.add(songs[(Integer)children.get(i)].toitem(),position+i+1);
				}
			} else {
				libraryitem item;
				boolean keep_running=true;
				while (keep_running) {
					if (position+1<l.getCount()) {
					item=(libraryitem)l.getItem(position+1);
					if (item.type <= 1) {
						keep_running=false;
					} else {
						item.resetpos();
						l.remove(item);
					}
					} else {keep_running=false;}
				}
			}
			l.updatepositions();
		}
		public int uid() {
				return index+artistcount;
			}
	}
	public class song {
		public int parent;
		public int position;
		public int playlistposition;
		public int selected=0;
		public String path;
		public int index;
		public String name;
		public song(String songname, int index, String path) {
			this.index=index;
			this.path=path;
			position=-1;
			name=songname;
		}
		public void play() {
			//b.pService.play(playlistposition);
		}
		public void addparent(int p) {
			parent=p;
		}
		public album getparent() {
			return albums[parent];
		}
		public libraryitem toitem() {
			return new libraryitem(name, 2, index, selected);
		}
		public  void selectdeselect() {
			if (selected==2) {
				deselect();
			} else {
				select();
			}
		}
		public  void select() { //DOES NOT behave like album.select() or artist.select(). for that behavior use selectdeselect().
			if (selected==0) {
				p.select(index);
				//selections.add((Integer) index);//change to write directly to currentplaylist arrayadapter
			}
			selected=2;
			albums[parent].childselected();
		}
		public  void deselect() {
			if (selected==2) {
				p.deselect(index);
				//selections.remove((Integer) index);//change to write directly to currentplaylist arrayadapter
			}
			selected=0;
			albums[parent].childdeselected();
		}
		public void parentselected() {
			if (selected==0) {
				p.select(index);
				//selections.add((Integer) index);//change to write directly to currentplaylist arrayadapter
			}
			selected=2;
		}
		public void parentdeselected() {
			if (selected==2) {
				p.deselect(index);
				//selections.remove((Integer) index);//change to write directly to currentplaylist arrayadapter
			}
			selected=0;
		}
		public int uid() {
			return index+artistcount+albumcount;
		}
	}
	public Library(PlayerService p) {
		this.p=p;
		this.cr=p.getContentResolver();
		//selections=new ArrayList<Integer>();
		artistkey=new Hashtable<String,Integer>();
		albumkey=new Hashtable<String,Integer>();
	}
	public void init() {
		String[] artistproj = {"artist","artist_key"};
		Cursor artistCursor = cr.query(MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,artistproj,null,null,"artist");//only reads music from SD Card
		artistcount=artistCursor.getCount();
		artists=new artist[artistcount];
		artistCursor.moveToFirst();
		int i=0;
		artistprocess(artistCursor,i);
		while (artistCursor.moveToNext()) {
			i+=1;
			artistprocess(artistCursor,i);
		}
		artistCursor.close();
		String[] albumproj = {"album","album_key"};
		Cursor albumCursor = cr.query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,albumproj,null,null,"album");//only reads music from SD Card
		albumcount=albumCursor.getCount();
		albums=new album[albumcount];
		albumCursor.moveToFirst();
		i=0;
		albumprocess(albumCursor,i);
		while (albumCursor.moveToNext()) {
			i+=1;
			albumprocess(albumCursor,i);
		}
		albumCursor.close();
		String[] songproj = {"title","album_key","artist_key","_data"};
		Cursor songCursor = cr.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,songproj,"is_music=1",null,"artist, year, album, track");//only reads music from SD Card
		songcount=songCursor.getCount();
		songs=new song[songcount];
		songCursor.moveToFirst();
		i=0;
		lastalbum="";
		albumindex=0;
		songprocess(songCursor,i);
		while (songCursor.moveToNext()) {
			i+=1;
			songprocess(songCursor,i);
		}
		songCursor.close();
	}
	private void artistprocess(Cursor cursor, int index) { //function for reading MediaStore database, see artistproj for columns
		String key=cursor.getString(1);
		artists[index]=new artist(cursor.getString(0),index,key);
		artistkey.put(key,index);
	}
	private void albumprocess(Cursor cursor, int index) { //function for reading MediaStore database, see albumproj for columns
		String key=cursor.getString(1);
		albums[index]=new album(cursor.getString(0),index,key);
		albumkey.put(key,index);
	}
	private void songprocess(Cursor cursor, int index) { //function for reading MediaStore database, see songproj for columns
		songs[index]=new song(cursor.getString(0),index,cursor.getString(3));
		String album=cursor.getString(1);
		String artist=cursor.getString(2);
		if ( ! album.equals(lastalbum) ) {
			int artistindex=(Integer)artistkey.get(artist);
			albumindex=(Integer)albumkey.get(album);
			artists[artistindex].addchild(albumindex);
			albums[albumindex].addparent(artistindex);
			lastalbum=album;
		}
		albums[albumindex].addchild(index);
		songs[index].addparent(albumindex);
	}
}
