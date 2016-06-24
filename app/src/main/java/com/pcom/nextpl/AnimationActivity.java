package com.pcom.nextpl;

import android.app.Activity;
import android.media.MediaPlayer;
import android.opengl.GLSurfaceView;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.File;

public class AnimationActivity extends Activity {

    private GLSurfaceView surface;

    private MediaPlayer mediaPlayer;

    public static final String EXTRA_TRACK_FILE = "com.pcom.nextpl.EXTRA_TRACK_FILE";
    public static final  String EXTRA_MAP_FILE = "com.pcom.nextpl.EXTRA_MAP_FILE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mediaPlayer = new MediaPlayer();
        Bundle extras = getIntent().getExtras();
        surface = new AnimationSurface(this, mediaPlayer, new File(extras.getString(EXTRA_TRACK_FILE)), new File(extras.getString(EXTRA_MAP_FILE)));
        setContentView(surface);
    }

    public void showAndQuit(int points, int total)
    {
        Toast.makeText(this, points + " points out of " + total, Toast.LENGTH_SHORT).show();
        this.finish();
    }

    @Override
    protected void onResume()
    {
        // The activity must call the GL surface view's onResume() on activity onResume().
        super.onResume();
        surface.onResume();
    }

    @Override
    protected void onPause()
    {
        // The activity must call the GL surface view's onPause() on activity onPause().
        super.onPause();
        surface.onPause();
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        mediaPlayer.release();
    }
}
