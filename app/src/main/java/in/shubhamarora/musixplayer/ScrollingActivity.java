package in.shubhamarora.musixplayer;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import in.shubhamarora.musixplayer.MusicService.MusicBinder;

import android.net.Uri;
import android.os.IBinder;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.widget.ListView;
import android.widget.MediaController.MediaPlayerControl;

public class ScrollingActivity extends AppCompatActivity implements MediaPlayerControl {
    //song list variables
    private ArrayList<Song> songList;
    private ListView songView;
    int song_status = 0;
    //service
    private MusicService musicSrv;
    private Intent playIntent;
    //binding
    private boolean musicBound=false;

    //controller
    private MusicController controller;

    //activity and playback pause flags
    private boolean paused=false, playbackPaused=false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scrolling);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);

       fab.setOnClickListener(new View.OnClickListener() {
        @Override
            public void onClick(View view) {
            String s = musicSrv.getSongTitle();
            if (song_status == 0) {
                musicSrv.setSong(0);
                musicSrv.playSong();
                if (playbackPaused) {
                    setController();
                    playbackPaused = false;
                }
                controller.hide();
                FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
                fab.setImageDrawable(getDrawable(R.drawable.ic_pause_playcontrol_normal));
                song_status = 1;
                Snackbar.make(view, (new StringBuilder()).append("Now Playing : ").append(s).toString(), Snackbar.LENGTH_SHORT)
                        .setAction("Action", null).show();
            } else {
                if (song_status == 1) {
                    musicSrv.pausePlayer();
                    song_status = 2;
                    FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
                    fab.setImageDrawable(getDrawable(R.drawable.ic_play_playcontrol_normal));
                    return;
                }
                if (song_status == 2) {
                    musicSrv.go();
                    song_status = 1;
                    FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
                    fab.setImageDrawable(getDrawable(R.drawable.ic_pause_playcontrol_normal));
                    Snackbar.make(view, (new StringBuilder()).append("Now Playing : ").append(s).toString(), Snackbar.LENGTH_SHORT)
                            .setAction("Action", null).show();
                    return;
                }
            }
        }});

        songView = (ListView)findViewById(R.id.song_list);
        //instantiate list
        songList = new ArrayList<Song>();
        //get songs from device
        getSongList();
        //sort alphabetically by title
        Collections.sort(songList, new Comparator<Song>(){
            public int compare(Song a, Song b){
                return a.getTitle().compareTo(b.getTitle());
            }
        });
        //create and set adapter
        SongAdapter songAdt = new SongAdapter(this, songList);
        songView.setAdapter(songAdt);

        //setup controller
        setController();
    }

    private ServiceConnection musicConnection = new ServiceConnection(){

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicBinder binder = (MusicBinder)service;
            //get service
            musicSrv = binder.getService();
            //pass list
            musicSrv.setList(songList);
            musicBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicBound = false;
        }
    };

    //start and bind the service when the activity starts
    @Override
    protected void onStart() {
        super.onStart();
        if(playIntent==null){
            playIntent = new Intent(this, MusicService.class);
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
            startService(playIntent);
        }
    }

    //user song select
    public void songPicked(View view){
        musicSrv.setSong(Integer.parseInt(view.getTag().toString()));
        musicSrv.playSong();
        if(playbackPaused){
            setController();
            playbackPaused=false;
        }
        controller.hide();
        song_status = 1;
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setImageDrawable(getDrawable(R.drawable.ic_pause_playcontrol_normal));
        String of = musicSrv.getSongTitle();
        Snackbar.make(view, (new StringBuilder()).append("Now Playing : ").append(((String) (of))).toString(), Snackbar.LENGTH_SHORT)
                .setAction("Action", null).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_scrolling, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_settings)
        {
            return true;
        }
        if (id == R.id.playprevs)
        {
            String s = musicSrv.getSongTitle();
            musicSrv.playPrev();
            FloatingActionButton floatingactionbutton = (FloatingActionButton)findViewById(R.id.fab);
            floatingactionbutton.setImageDrawable(getDrawable(android.R.drawable.ic_media_pause));
            song_status = 1;
            Snackbar.make(floatingactionbutton, (new StringBuilder()).append("Now Playing : ").append(s).toString(),
                    Snackbar.LENGTH_SHORT).setAction("Action", null).show();
        }
        if (id == R.id.playnexts)
        {
            String s1 = musicSrv.getSongTitle();
            musicSrv.playNext();
            FloatingActionButton floatingactionbutton1 = (FloatingActionButton)findViewById(R.id.fab);
            floatingactionbutton1.setImageDrawable(getDrawable(android.R.drawable.ic_media_pause));
            song_status = 1;
            Snackbar.make(floatingactionbutton1, (new StringBuilder()).append("Now Playing : ").append(s1).toString(),
                    Snackbar.LENGTH_SHORT).setAction("Action", null).show();
        }
        return super.onOptionsItemSelected(item);
    }

    public void getSongList(){
        //query external audio
        ContentResolver musicResolver = getContentResolver();
        Uri musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null);
        //iterate over results if valid
        if(musicCursor!=null && musicCursor.moveToFirst()){
            //get columns
            int titleColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.TITLE);
            int idColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media._ID);
            int artistColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.ARTIST);
            //add songs to list
            do {
                long thisId = musicCursor.getLong(idColumn);
                String thisTitle = musicCursor.getString(titleColumn);
                String thisArtist = musicCursor.getString(artistColumn);
                songList.add(new Song(thisId, thisTitle, thisArtist));
            }
            while (musicCursor.moveToNext());
        }
    }

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return true;
    }

    @Override
    public boolean canSeekForward() {
        return true;
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }

    @Override
    public int getBufferPercentage() {
        return 0;
    }

    @Override
    public int getCurrentPosition() {
        if(musicSrv!=null && musicBound && musicSrv.isPng())
            return musicSrv.getPosn();
        else return 0;
    }

    @Override
    public int getDuration() {
        if(musicSrv!=null && musicBound && musicSrv.isPng())
            return musicSrv.getDur();
        else return 0;
    }

    @Override
    public boolean isPlaying() {
        if(musicSrv!=null && musicBound)
            return musicSrv.isPng();
        return false;
    }

    @Override
    public void pause() {
        playbackPaused=true;
        musicSrv.pausePlayer();
    }

    @Override
    public void seekTo(int pos) {
        musicSrv.seek(pos);
    }

    @Override
    public void start() {
        musicSrv.go();
    }

    //set the controller up
    private void setController(){
        controller = new MusicController(this);
        //set previous and next button listeners
        controller.setPrevNextListeners(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playNext();
            }
        }, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playPrev();
            }
        });
        //set and show
        controller.setMediaPlayer(this);
        controller.setAnchorView(findViewById(R.id.song_list));
        controller.setEnabled(true);
    }

    private void playNext(){
        musicSrv.playNext();
        if(playbackPaused){
            setController();
            playbackPaused=false;
        }
        controller.show(0);
    }

    private void playPrev(){
        musicSrv.playPrev();
        if(playbackPaused){
            setController();
            playbackPaused=false;
        }
        controller.show(0);
    }

    @Override
    protected void onPause(){
        super.onPause();
        paused=true;
    }

    @Override
    protected void onResume(){
        super.onResume();
        if(paused){
            setController();
            paused=false;
        }
    }

    @Override
    protected void onStop() {
        controller.hide();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        stopService(playIntent);
        musicSrv=null;
        super.onDestroy();
    }
}
