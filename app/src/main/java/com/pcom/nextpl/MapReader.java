package com.pcom.nextpl;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * Created by bixind on 11.06.16.
 */
public class MapReader {

    public ArrayList<ArrayList<Float>> pitch;
    public int duration;

    ArrayList<Integer> roads;
    ArrayList<Float> times;

    MapReader(){
        pitch = null;
        duration = 0;

        roads = null;
        times = null;
    }

    public int readInt(InputStream s, ByteBuffer b) throws IOException
    {
        s.read(b.array(), 0, 4);
        b.rewind();
        return b.getInt();
    }

    public float readFloat(InputStream s, ByteBuffer b) throws IOException
    {
        s.read(b.array(), 0, 4);
        b.rewind();
        return b.getFloat();
    }

    public void readPitches(File f)
    {
        ByteBuffer b = ByteBuffer.allocate(4);
        this.pitch = new ArrayList<>();
        duration = 0;
        try {
            InputStream in = new BufferedInputStream(new FileInputStream(f));
            in.read(b.array(), 0, 4);
            b.rewind();
            duration = b.getInt();
            in.read(b.array(), 0, 4);
            b.rewind();
            int n = b.getInt();
            pitch = new ArrayList<>(n);
            for (int i = 0; i < n; i++)
            {

                in.read(b.array(), 0, 4);
                b.rewind();
                int m = b.getInt();
                ArrayList<Float> voices = new ArrayList<>(m);
                for (int j = 0; j < m; j++)
                {
                    in.read(b.array(), 0, 4);
                    b.rewind();
                    float val = b.getFloat();
                    voices.add(val);
                }
                pitch.add(voices);
            }
            in.close();
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    void readMap(File mapFile)
    {
        ByteBuffer b = ByteBuffer.allocate(4);
        try {
            InputStream in = new BufferedInputStream(new FileInputStream(mapFile));
            int n = readInt(in, b);
            roads = new ArrayList<>(n);
            times = new ArrayList<>(n);
            for (int i = 0; i < n; i++)
            {
                roads.add(readInt(in, b));
                times.add(readFloat(in, b));
            }
            in.close();
        } catch (IOException e){
            e.printStackTrace();
        }
    }
//    public void readMap
}
