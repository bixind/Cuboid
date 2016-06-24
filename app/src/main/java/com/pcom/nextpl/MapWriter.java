package com.pcom.nextpl;

import android.os.Environment;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * Created by bixind on 18.06.16.
 */

public class MapWriter {

    static private void writeFloat(OutputStream s, float f, ByteBuffer b) throws IOException
    {
        b.rewind();
        b.putFloat(f);
        s.write(b.array(), 0, 4);
    }

    static private void writeInt(OutputStream s, int i, ByteBuffer b) throws IOException
    {
        b.rewind();
        b.putInt(i);
        s.write(b.array(), 0, 4);
    }

    static void writePitches(ArrayList<ArrayList<Float>> pitch, int duration, File pitchFile)
    {
        ByteBuffer b = ByteBuffer.allocate(4);
        try {
            OutputStream w = new BufferedOutputStream(new FileOutputStream(pitchFile));
            b.rewind();
            b.putInt(duration);
            w.write(b.array(), 0, 4);
            b.rewind();
            b.putInt(pitch.size());
            w.write(b.array(), 0, 4);
            for (int i = 0; i < pitch.size(); i++)
            {
                ArrayList<Float> voices = pitch.get(i);
                b.rewind();
                b.putInt(voices.size());
                w.write(b.array(), 0, 4);
                for (int j = 0; j < voices.size(); j++)
                {
                    b.rewind();
                    b.putFloat(voices.get(j));
                    w.write(b.array(), 0, 4);
                }
            }
            w.flush();
            w.close();
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    static void writeMap(ArrayList<Integer> roads, ArrayList<Float> times, File mapFile)
    {
        ByteBuffer b = ByteBuffer.allocate(4);
        try {
            OutputStream w = new BufferedOutputStream(new FileOutputStream(mapFile));
            writeInt(w, roads.size(), b);
            for (int i = 0; i < roads.size(); i++)
            {
                writeInt(w, roads.get(i), b);
                writeFloat(w, times.get(i), b);
            }
            w.flush();
            w.close();
        } catch (IOException e){
            e.printStackTrace();
        }
    }
}
