package com.pcom.nextpl;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

public class MapGenerator {

    final static int durationCutoff = 30;
    final static int absenceCutoff = 3;
    final static double ratioCutoff = Math.sqrt(1.05946309436d);
    final static double base = 55;
    final static float mergeTime = 0.3f;

    static class NoteLine implements Comparable<NoteLine>{
        protected ArrayList<Float> freq;
        protected boolean used;
        protected int absence;
        protected int i;
        NoteLine(int ni){
            freq = new ArrayList<>();
            i = ni;
            used = false;
            absence = 0;
        }

        public int compareTo(NoteLine nl)
        {
            return ((Integer) i).compareTo(nl.i);
        }

        protected Float last()
        {
            if (freq.size() > 0)
                return freq.get(freq.size() - 1);
            else
                return null;
        }

        protected Float first()
        {
            return freq.get(0);
        }

        protected void put(Float nt)
        {
            if (absence > 0) {
                Float l = last();
                for (int j = 0; j < absence; j++)
                    freq.add(l);
                absence = 0;
            }
            freq.add(nt);
            used = true;
        }
    }

    static private boolean passRatioCutoff(double base, double pitch)
    {
        if (base > pitch)
        {
            double buf = base;
            base = pitch;
            pitch = buf;
        }
        if ((pitch / base) > ratioCutoff)
            return false;
        else
            return true;
    }

    static public void generate(File pFile, File aFile, File mapD){
        File mapFile = new File(mapD, aFile.getName() + ".map");
        MapReader mp = new MapReader();
        mp.readPitches(pFile);
        mapFile.getParentFile().mkdirs();
        ArrayList<ArrayList<Float>> nPitch = new ArrayList<>(mp.pitch.size());
        ArrayList<NoteLine> melody = new ArrayList<>();
        ArrayList<NoteLine> waitList = new ArrayList<>();

        for (int i = 0; i < mp.pitch.size(); i++)
        {
            nPitch.add(new ArrayList<Float>());
            for (int h = 0; h < melody.size(); h++)
                melody.get(h).used = false;

            for (int j = 0; j < mp.pitch.get(i).size(); j++)
            {
                float nt = mp.pitch.get(i).get(j);
                if (nt > 105)
                {
                    int msz = melody.size();
                    boolean found = false;
                    for (int h = 0; h < msz; h++)
                    {
                        if ((!melody.get(h).used) && (passRatioCutoff((float) melody.get(h).first(), nt)))
                        {
                            found = true;
                            melody.get(h).put(nt);
                            break;
                        }
                    }
                    if (!found)
                    {
                        NoteLine nl = new NoteLine(i);
                        nl.put(nt);
                        melody.add(nl);
                    }
                }
            }
            for (int h = 0; h < melody.size(); h++) {
                if (!melody.get(h).used)
                {
                    NoteLine nl = melody.get(h);
                    if (nl.absence >= absenceCutoff)
                    {
                        if (nl.freq.size() >= durationCutoff)
                        {
                            for (int p = 0; p < nl.freq.size(); p++)
                            {
//                                Log.d("test", nPitch.size() + " " + nl.freq.size() + " " + (p + nl.i));
                                nPitch.get(p + nl.i).add(nl.freq.get(p));
                            }
                            waitList.add(nl);
                        }
                        melody.remove(h);
                        h--;
                    }
                    else
                    {
                        nl.absence++;
                    }
                }
            }
        }
        for (int h = 0; h < melody.size(); h++)
        {
            NoteLine nl = melody.get(h);
            if (nl.freq.size() >= durationCutoff)
            {
                for (int p = 0; p < nl.freq.size(); p++)
                {
                    nPitch.get(p + nl.i).add(nl.freq.get(p));
                }
                waitList.add(nl);
            }
        }
        Collections.sort(waitList);
        double dt = mp.duration / 1000d / (double) mp.pitch.size();
        ArrayList<Float> times = new ArrayList<>();
        ArrayList<Integer> roads = new ArrayList<>();
        ArrayList<ArrayList<Float>> timesLined = new ArrayList<>();
        for (int i = 0; i < 4; i++)
            timesLined.add(new ArrayList<Float>());
        for (int i = 0; i < waitList.size(); i++)
        {
            NoteLine nl = waitList.get(i);
//            times.add((float) (nl.i * dt));
            double lvl = Math.log(nl.first() / base) / Math.log(2) * 12;
//            Log.d("test", "" + nl.i * dt + " " + lvl);
            long r = Math.round(lvl) % 4;
//            roads.add((int) r);
            timesLined.get((int) r).add((float) (nl.i * dt));
        }

        for (int i = 0; i < timesLined.size(); i++)
        {
            int prv = -1;
            for (int j = 0; j < timesLined.get(i).size(); j++)
            {
                if (j == 0 || timesLined.get(i).get(j) - timesLined.get(i).get(prv) > mergeTime)
                {
                    times.add(timesLined.get(i).get(j));
                    roads.add(i);
                    prv = j;
                    Log.d("test", i + " " + timesLined.get(i).get(j));
                }
            }
        }
        MapWriter.writeMap(roads, times, mapFile);
    }
}
