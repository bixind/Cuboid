package com.pcom.nextpl;

import android.content.Context;
import android.content.Intent;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.ArrayList;

public class ListActivity extends AppCompatActivity {

    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    class FileAdapter extends RecyclerView.Adapter<FileAdapter.ViewHolder>
    {
        public class ViewHolder extends RecyclerView.ViewHolder
        {
            public TextView name, status;
            public Button genB, playB;
            public ViewHolder(LinearLayout v)
            {
                super(v);
                name = (TextView) v.getChildAt(0);
                status = (TextView) v.getChildAt(1);
                genB = (Button) v.getChildAt(2);
                playB = (Button) v.getChildAt(3);
            }
        }

        File audioD, pitchD, mapD;

        // Provide a suitable constructor (depends on the kind of dataset)
        public FileAdapter(Context context) {
            File f = FileManager.getRootDirectory(context);
            audioD = new File(f, "audio");
            pitchD = new File(f, "pitch");
            mapD = new File(f, "map");

        }

        // Create new views (invoked by the layout manager)
        @Override
        public FileAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LinearLayout v = (LinearLayout) LayoutInflater.from(parent.getContext()).inflate(R.layout.file_entry, parent, false);
            ViewHolder vh = new ViewHolder(v);
            return vh;
        }

        private class ProcessTrack extends AsyncTask<File, Void, Void> {
            static final int CHUNCK_SIZE = 480000;
            protected ArrayList<ArrayList<Float>> pitch;
            protected int duration;
            private void getMelodia(File f)
            {
                MediaExtractor extractor = new MediaExtractor();
                try {
                    extractor.setDataSource(f.getAbsolutePath());
                    MediaFormat form = extractor.getTrackFormat(0);
                    extractor.selectTrack(0);
                    duration = (int) (form.getLong(MediaFormat.KEY_DURATION) / 1000);
                    Log.d("test", form.toString());
                    MediaCodec codec = MediaCodec.createDecoderByType(form.getString(MediaFormat.KEY_MIME));
                    codec.configure(form, null, null, 0);
                    codec.start();
                    ByteBuffer[] inputs = codec.getInputBuffers();
                    ByteBuffer[] outputs = codec.getOutputBuffers();
                    float[] rcmbuf = new float[CHUNCK_SIZE];
                    int sz = 0;
                    while (true) {
                        int bid = codec.dequeueInputBuffer(100);
                        if (bid >= 0) {
                            int ssize = extractor.readSampleData(inputs[bid], 0);
                            if (ssize < 0)
                            {
                                codec.queueInputBuffer(bid, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            } else {
                                codec.queueInputBuffer(bid, 0, ssize, 0, 0);
                                extractor.advance();
                            }
                        }
                        MediaCodec.BufferInfo bufi = new MediaCodec.BufferInfo();
                        int obid = codec.dequeueOutputBuffer(bufi, 1000);
                        if (obid >= 0)
                        {
                            ShortBuffer samples = outputs[obid].order(ByteOrder.nativeOrder()).asShortBuffer();
                            int numch = form.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                            int cnt = bufi.size / 2;
                            for (int i = 0; i < cnt; i+=numch)
                            {
                                Float amp = (float) samples.get(i) / (float) 32768;
                                rcmbuf[sz] = amp;
                                sz++;
                                if (sz >= CHUNCK_SIZE)
                                {
                                    Log.d("proc", ((Integer) process(sz, rcmbuf, pitch)).toString());
                                    sz = 0;
                                }
                            }
                            codec.releaseOutputBuffer(obid, false);
                            if ((bufi.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) > 0) {
                                Log.d("proc", ((Integer) process(sz, rcmbuf, pitch)).toString());
                                sz = 0;
                                break;
                            }
                        } else if (obid == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                            outputs = codec.getOutputBuffers();
                        } else if (obid == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                            // Subsequent data will conform to new format.
                            form = codec.getOutputFormat();
                        }
                    }
                    Log.d("res", ((Integer) sz).toString());
                    codec.stop();
                    codec.release();
                    extractor.release();
                    extractor = null;



                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            protected Void doInBackground(File... fs)
            {
                for (int i = 0; i < fs.length; i++) {
                    File f = fs[i];
                    pitch = new ArrayList<>();
                    duration = 0;
                    getMelodia(f);
                    File pitchFile = new File(pitchD, f.getName() + ".o");
                    MapWriter.writePitches(pitch, duration, pitchFile);
                    MapGenerator.generate(pitchFile, f, mapD);
                }
                return null;
            }

            protected void onPostExecute(Void r)
            {
                Toast.makeText(getApplicationContext(), "Track processed", Toast.LENGTH_LONG).show();
                Log.d("ressz", ((Integer) pitch.size()).toString());
            }
        }

        public class generateButtonListener implements View.OnClickListener
        {
            int p;
            public generateButtonListener(int pos)
            {
                p = pos;
            }

            @Override
            public void onClick(View view) {
                File track = audioD.listFiles()[p];
                new ProcessTrack().execute(track);
            }
        }

        public class playButtonListener implements View.OnClickListener
        {
            int p;
            public playButtonListener(int pos)
            {
                p = pos;
            }

            @Override
            public void onClick(View view) {
                File track = audioD.listFiles()[p];
                File map = new File(mapD, track.getName() + ".map");
                if (map.exists())
                {
                    Intent startAnimation = new Intent(getApplicationContext(), AnimationActivity.class);
                    startAnimation.putExtra(AnimationActivity.EXTRA_TRACK_FILE, track.getAbsolutePath());
                    startAnimation.putExtra(AnimationActivity.EXTRA_MAP_FILE, map.getAbsolutePath());
                    startActivity(startAnimation);
                } else {
                    Toast.makeText(getApplicationContext(), "No map found",Toast.LENGTH_SHORT).show();
                }
            }
        }

        // Replace the contents of a view (invoked by the layout manager)
        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            // - get element from your dataset at this position
            // - replace the contents of the view with that element
            String track = audioD.list()[position];
            holder.name.setText(track);
            String status = "";
            if (new File(pitchD, track + ".o").exists())
                status += "p ";
            if (new File(mapD, track + ".map").exists())
                status += "m";
            holder.status.setText(status);
            holder.genB.setOnClickListener(new generateButtonListener(position));
            holder.playB.setOnClickListener(new playButtonListener(position));
        }

        // Return the size of your dataset (invoked by the layout manager)
        @Override
        public int getItemCount() {
            return audioD.list().length;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        mRecyclerView = (RecyclerView) findViewById(R.id.filelist_view);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
//
//        // specify an adapter (see also next example)
        mAdapter = new FileAdapter(this);
        mRecyclerView.setAdapter(mAdapter);
    }

    public native int process(int size, float[] buf, ArrayList<ArrayList<Float>> pitch);
    static {
        System.loadLibrary("nextpl");
    }
}
