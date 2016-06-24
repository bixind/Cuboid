package com.pcom.nextpl;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaFormat;
import android.media.MediaPlayer;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class AnimationSurface extends GLSurfaceView {

    private final AnimationRenderer renderer;

    private MediaPlayer mediaPlayer;

    private volatile int points;

    private AnimationActivity masterActivity;

    public AnimationSurface(AnimationActivity context, MediaPlayer mp, File audio, File map)
    {
        super(context);
        masterActivity = context;
        setEGLContextClientVersion(2);
        renderer = new AnimationRenderer(this, map);
        setRenderer(renderer);

        mediaPlayer = mp;
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        points = 0;
        try {
            mediaPlayer.setDataSource(audio.getAbsolutePath());
            mediaPlayer.prepare();
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e){
        Log.d("test", e.toString());
        if (e.getActionMasked() == MotionEvent.ACTION_DOWN || e.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN) {
            if (!renderer.isStarted) {
                renderer.start = SystemClock.uptimeMillis() / 1000d;
                renderer.isStarted = true;
                mediaPlayer.start();
            } else {
                int ind;
                if (e.getAction() == MotionEvent.ACTION_DOWN)
                {
                    ind = 0;
                }
                else
                {
                    ind = e.getActionIndex();
                }
                int r = ((int) e.getX(ind)) / (renderer.widthS / 4);
                Log.d("test", r + "");
                if (renderer.curCube[r] >= 0) {
                    renderer.lineCubes.get(r).get(renderer.curCube[r]).setUpColor(renderer.green);
                    renderer.lineCubes.get(r).get(renderer.curCube[r]).setDownColor(renderer.dgreen);
                    points++;
                }
            }
        }
        return true;
    }

    class AnimationRenderer implements GLSurfaceView.Renderer{

        final float[] red = {1f, 0f, 0f, 1f};
        final float[] dred = {0.5f, 0f, 0f, 1f};

        final float[] green = {0f, 1f, 0f, 1f};
        final float[] dgreen = {0f, 0.5f, 0f, 1f};

        private class Cube{
            private float sideLength;
            private float[] upColor;
            private float[] downColor;
            private float dx;
            private float dy;
            private float dz;
            private final float[] vertexCarcass = {
                    -1f, -1f, 1f,
                    -1f, 1f, 1f,
                    1f, 1f, 1f,
                    1f, -1f, 1f,

                    -1f, -1f, -1f,
                    -1f, 1f, -1f,
                    1f, 1f, -1f,
                    1f, -1f, -1f
            };

            Cube(float length, float x, float y, float z, float[] upC, float[] downC)
            {
                dx = x;
                dy = y;
                dz = z;
                sideLength = length;
                upColor = new float[4];
                downColor = new float[4];
                for (int i = 0; i < 4; i++)
                {
                    upColor[i] = upC[i];
                    downColor[i] = downC[i];
                }
            }

            void setUpColor(float[] upC)
            {
                upColor = upC;
            }

            void setDownColor(float[] downC)
            {
                downColor = downC;
            }

            void addSide(int a, int b, int c, int d)
            {
//                Log.d("tset", vBufferOffset + " ");
                indexBuffer.put((short) (vBufferOffset + b));
                iBufferOffset++;
                indexBuffer.put((short) (vBufferOffset + a));
                iBufferOffset++;
                indexBuffer.put((short) (vBufferOffset + c));
                iBufferOffset++;
                indexBuffer.put((short) (vBufferOffset + a));
                iBufferOffset++;
                indexBuffer.put((short) (vBufferOffset + d));
                iBufferOffset++;
                indexBuffer.put((short) (vBufferOffset + c));
                iBufferOffset++;
            }

            protected void addToBuffers()
            {
                addSide(0, 1, 2, 3);
                addSide(4, 0, 3, 7);
                addSide(5, 4, 7, 6);
                addSide(1, 5, 6, 2);
                addSide(5, 1, 0, 4);
                addSide(7, 3, 2, 6);
                float halfl = sideLength / 2;
                for (int i = 0; i < 8; i++)
                {
                    verticesBuffer.put(vertexCarcass[3 * i] * halfl + dx);
                    verticesBuffer.put(vertexCarcass[3 * i + 1] * halfl + dy);
                    verticesBuffer.put(vertexCarcass[3 * i + 2] * halfl + dz);
                    if (i < 4)
                    {
                        verticesBuffer.put(upColor);
                    }
                    else
                    {
                        verticesBuffer.put(downColor);
                    }
                    vBufferOffset++;
                }
            }
        }

        public MapReader mp;
        ArrayList<ArrayList<Cube>> lineCubes;
        int[] p;
        volatile int[] curCube;

        FloatBuffer verticesBuffer;
        int vBufferOffset;
        ShortBuffer indexBuffer;
        int iBufferOffset;
        public volatile double start;
        public volatile boolean isStarted = false;

        final double speed = 1.5;

        float[] linePoints = {
                -5f, 0f, 0.1f,
                0f, 0f, 1f, 1f,
                5f, 0f, 0.1f,
                0f, 0f, 1f, 1f
        };

        FloatBuffer lineBuffer;
        AnimationSurface masterSurface;

        public AnimationRenderer(AnimationSurface s, File map){
            masterSurface = s;
            mp = new MapReader();
            mp.readMap(map);
            lineCubes = new ArrayList<>();
            p = new int[4];
            curCube = new int[4];
            for (int i = 0; i < 4; i++) {
                lineCubes.add(new ArrayList<Cube>());
                p[i] = 0;
                curCube[i] = -1;
            }
            start = SystemClock.uptimeMillis() / 1000d;
            verticesBuffer = ByteBuffer.allocateDirect(1000 * 7 * 8 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
            vBufferOffset = 0;
            indexBuffer = ByteBuffer.allocateDirect(262144).order(ByteOrder.nativeOrder()).asShortBuffer();
            iBufferOffset = 0;
            lineBuffer = ByteBuffer.allocateDirect(linePoints.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
            lineBuffer.put(linePoints);

        }

        final String vertexShader =
                "uniform mat4 u_MVPMatrix;      \n"

                        + "attribute vec4 a_Position;     \n"
                        + "attribute vec4 a_Color;        \n"

                        + "varying vec4 v_Color;          \n"

                        + "void main()                    \n"
                        + "{                              \n"
                        + "   v_Color = a_Color;          \n"
                        + "   gl_Position = u_MVPMatrix   \n"
                        + "               * a_Position;   \n"
                        + "}                              \n";

        final String fragmentShader =
                "precision mediump float;       \n"

                        + "varying vec4 v_Color;          \n"

                        + "void main()                    \n"
                        + "{                              \n"
                        + "   gl_FragColor = v_Color;     \n"
                        + "}                              \n";

        private float[] viewMatrix = new float[16];

        private int MVPMatrixHandle;
        private int positionHandle;
        private int colorHandle;

        protected int widthS;
        protected int heightS;
        private float zHalfWidth;
        private float zHalfHeight;

        public void onSurfaceCreated(GL10 unused, EGLConfig config){
            GLES20.glEnable(GLES20.GL_CULL_FACE);
            GLES20.glEnable(GLES20.GL_DEPTH_TEST);

            GLES20.glClearColor(1f, 1f, 1f, 1);
            Matrix.setLookAtM(viewMatrix, 0, 0.0f, 0.0f, 1.5f, 0f, 0f, -5f, 0f, 5f, 0f);

            int vertexShaderHandle = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
            GLES20.glShaderSource(vertexShaderHandle, vertexShader);
            GLES20.glCompileShader(vertexShaderHandle);

            int fragmentShaderHandle = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
            GLES20.glShaderSource(fragmentShaderHandle, fragmentShader);
            GLES20.glCompileShader(fragmentShaderHandle);

            int programHandle = GLES20.glCreateProgram();
            GLES20.glAttachShader(programHandle, vertexShaderHandle);
            GLES20.glAttachShader(programHandle, fragmentShaderHandle);
            GLES20.glBindAttribLocation(programHandle, 0, "a_Position");
            GLES20.glBindAttribLocation(programHandle, 1, "a_Color");
            GLES20.glLinkProgram(programHandle);

            MVPMatrixHandle = GLES20.glGetUniformLocation(programHandle, "u_MVPMatrix");
            positionHandle = GLES20.glGetAttribLocation(programHandle, "a_Position");
            colorHandle = GLES20.glGetAttribLocation(programHandle, "a_Color");
            GLES20.glUseProgram(programHandle);

        }

        private float[] projectionMatrix = new float[16];

        public void onSurfaceChanged(GL10 unused, int width, int height) {
            GLES20.glViewport(0, 0, width, height);
            widthS = width;
            heightS = height;
            final float ratio = (float) width / height;
            final float near = 1f;
            zHalfWidth = ratio / near * 1.5f;
            zHalfHeight = 1 / near * 1.5f;
//            Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 1f, 10f);
            Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -0.5f, 1.5f, near, 10f);
            for (int i = 0; i < 4; i++)
                lineCubes.get(i).clear();
            final float ofs = 2 * zHalfWidth / 4;
            final float lg = ofs * 0.7f;
            for (int i = 0; i < mp.times.size(); i++)
            {
                lineCubes.get(mp.roads.get(i)).add(new Cube(lg, ofs * (0.5f + mp.roads.get(i)) - zHalfWidth, (float) (mp.times.get(i) * speed), -lg / 2 - 0.1f, red, dred));
            }

        }

        private float[] modelMatrix = new float[16];
        private long time = 0;

        public void onDrawFrame(GL10 unused) {
            GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);

//            float angleInDegrees = (360.0f / 10000.0f) * ((int) time);

            verticesBuffer.rewind();
            indexBuffer.rewind();
            vBufferOffset = 0;
            iBufferOffset = 0;
            double time = SystemClock.uptimeMillis() / 1000d;
            Matrix.setIdentityM(modelMatrix, 0);
            drawLine();
            Matrix.setIdentityM(modelMatrix, 0);
            float mvy = 0f;
            if (isStarted) {
                mvy = (float) ((start - time) * speed);
            }
            Matrix.translateM(modelMatrix, 0, 0f, mvy, 0f);
            boolean mapEnded = true;
            for (int j = 0; j < lineCubes.size(); j++)
            {
                curCube[j] = -1;
                if (p[j] < lineCubes.get(j).size())
                    mapEnded = false;
                for (int i = p[j]; i < lineCubes.get(j).size(); i++) {
                    float posy = lineCubes.get(j).get(i).dy + mvy;
                    if (posy < -2f) {
                        p[j]++;
                        continue;
                    }
                    if (posy > 10f) {
                        break;
                    }
                    lineCubes.get(j).get(i).addToBuffers();
                    float hl = lineCubes.get(j).get(i).sideLength / 2;
                    if (posy < hl && posy > -hl)
                        curCube[j] = i;
                }
            }
            if (mapEnded) {
                Handler mainHandler = new Handler(masterSurface.getContext().getMainLooper());
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        int total = 0;
                        for (int i = 0; i < lineCubes.size(); i++)
                            total += lineCubes.get(i).size();
                        masterSurface.masterActivity.showAndQuit(points, total);
                    }
                });
            }
            draw();

        }

        private float[] MVPMatrix = new float[16];

        private void draw(){
            verticesBuffer.position(0);
            int stride = 4 * 7;
            GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, stride, verticesBuffer);
            GLES20.glEnableVertexAttribArray(positionHandle);
            verticesBuffer.position(3);
            GLES20.glVertexAttribPointer(colorHandle, 4, GLES20.GL_FLOAT, false, stride, verticesBuffer);
            GLES20.glEnableVertexAttribArray(colorHandle);
            Matrix.multiplyMM(MVPMatrix, 0, viewMatrix, 0, modelMatrix, 0);
            Matrix.multiplyMM(MVPMatrix, 0, projectionMatrix, 0, MVPMatrix, 0);
            GLES20.glUniformMatrix4fv(MVPMatrixHandle, 1, false, MVPMatrix, 0);
            indexBuffer.position(0);
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, iBufferOffset, GLES20.GL_UNSIGNED_SHORT, indexBuffer);
//            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3);
        }

        private  void drawLine()
        {
            lineBuffer.position(0);
            int stride = 4 * 7;
            GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, stride, lineBuffer);
            GLES20.glEnableVertexAttribArray(positionHandle);
            lineBuffer.position(3);
            GLES20.glVertexAttribPointer(colorHandle, 4, GLES20.GL_FLOAT, false, stride, lineBuffer);
            GLES20.glEnableVertexAttribArray(colorHandle);
            Matrix.multiplyMM(MVPMatrix, 0, viewMatrix, 0, modelMatrix, 0);
            Matrix.multiplyMM(MVPMatrix, 0, projectionMatrix, 0, MVPMatrix, 0);
            GLES20.glUniformMatrix4fv(MVPMatrixHandle, 1, false, MVPMatrix, 0);
            GLES20.glDrawArrays(GLES20.GL_LINES, 0, 2);
        }

    }
}