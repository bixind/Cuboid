package com.pcom.nextpl;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaFormat;
import android.media.MediaPlayer;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;

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

public class DrawSurface extends GLSurfaceView {

    private final DrawRenderer renderer;

    private MediaPlayer mediaPlayer;

    public DrawSurface(Context context, MediaPlayer mp)
    {
        super(context);
        setEGLContextClientVersion(2);
        renderer = new DrawRenderer();
        setRenderer(renderer);

        mediaPlayer = mp;
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        File f = new File(FileManager.getRootDirectory(context),"audio");
        f = f.listFiles()[0];
        try {
            mediaPlayer.setDataSource(f.getAbsolutePath());
            mediaPlayer.prepare();
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e){
        if (e.getAction() == MotionEvent.ACTION_DOWN) {
            if (renderer.isStarted)
            {
                renderer.isStarted = false;
                mediaPlayer.pause();
            }
            else {
                renderer.start = SystemClock.uptimeMillis();
                renderer.isStarted = true;
                mediaPlayer.start();
            }
        }
        return true;
    }

    class DrawRenderer implements GLSurfaceView.Renderer{

//        private final FloatBuffer mTriangleVertices;


        public MapReader mp;
        ArrayList<Double> pnts;
        FloatBuffer pointsBuffer;
        FloatBuffer colorBuffer;
        public volatile long start;
        public volatile boolean isStarted = false;

        public DrawRenderer(){

            File f = new File(Environment.getExternalStorageDirectory(), "Cuboid" + "/map");
            f = f.listFiles()[0];
            mp = new MapReader();
            mp.readPitches(f);
            double dx = ((double) mp.duration / 1000 / mp.pitch.size());
            pnts = new ArrayList<>();
            for (int i = 0; i < mp.pitch.size(); i++)
                for (int j = 0; j < mp.pitch.get(i).size(); j++)
                {
                    double a = mp.pitch.get(i).get(j);
                    a = (Math.log(a / 55) * 0.5d);
                    pnts.add(a - 1f);
                    pnts.add(dx * i);
                    pnts.add(0d);
                }
            pointsBuffer = ByteBuffer.allocateDirect(pnts.size() * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
            for (int i = 0; i < pnts.size(); i++)
            {
                pointsBuffer.put((float) (double) pnts.get(i));
            }
            colorBuffer = ByteBuffer.allocateDirect(4 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
            colorBuffer.put(new float[] {1f, 1f, 0f, 0f});
            start = SystemClock.uptimeMillis();
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

        public void onSurfaceCreated(GL10 unused, EGLConfig config){
            GLES20.glClearColor(1f, 1f, 1f, 1);
            Matrix.setLookAtM(viewMatrix, 0, 0.0f, 0.0f, 1.5f, 0f, 0f, -5f, 0f, 1f, 0f);

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

            final float ratio = (float) width / height;
//            Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 1f, 10f);
            Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 1f, 10f);
        }

        private float[] modelMatrix = new float[16];
        private long time = 0;

        public void onDrawFrame(GL10 unused) {
            GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);

//            float angleInDegrees = (360.0f / 10000.0f) * ((int) time);

            // Draw the triangle facing straight on.
            Matrix.setIdentityM(modelMatrix, 0);
            drawLine();
            Matrix.setIdentityM(modelMatrix, 0);
//            Matrix.rotateM(modelMatrix, 0, angleInDegrees, 0.0f, 0.0f, 1.0f);
//            Matrix.translateM(modelMatrix, 0,  0, dx, 0);
//            Log.d("x", (pnts.get(60)).toString());
//            Log.d("y", (pnts.get(61)).toString());
//            Log.d("z", (pnts.get(62)).toString());
            if (isStarted) {
                long now = SystemClock.uptimeMillis();
                time += (now - start);
                start = now;
                double dx = (double) (time) / 1000;
                pointsBuffer.position(0);
                for (int i = 0; i < pnts.size(); i += 3) {
                    pointsBuffer.put((float) (double) pnts.get(i));
                    pointsBuffer.put((float) (double) (pnts.get(i + 1) - dx));
                    pointsBuffer.put((float) (double) pnts.get(i + 2));
                }
//                Log.d("what", ((Float) (float) dx).toString());
            }
            drawPoints();
//            drawTriangle(mTriangleVertices);

        }

        private float[] MVPMatrix = new float[16];

        private void drawPoints(){
            pointsBuffer.position(0);
            GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 3 * 4, pointsBuffer);
            GLES20.glEnableVertexAttribArray(positionHandle);
            GLES20.glVertexAttribPointer(colorHandle, 4, GLES20.GL_FLOAT, false, 0, colorBuffer);
            GLES20.glEnableVertexAttribArray(colorHandle);
            Matrix.setIdentityM(modelMatrix, 0);
            Matrix.multiplyMM(MVPMatrix, 0, viewMatrix, 0, modelMatrix, 0);
            Matrix.multiplyMM(MVPMatrix, 0, projectionMatrix, 0, MVPMatrix, 0);
            GLES20.glUniformMatrix4fv(MVPMatrixHandle, 1, false, MVPMatrix, 0);
            GLES20.glDrawArrays(GLES20.GL_POINTS, 0, pnts.size());
        }

        private void drawLine(){
            float[] lineData = {
                    -2f, 0f, 0.4f,
                    1f, 0f, 0f, 4f,
                    2f, 0f, 0.4f,
                    1f, 0f, 0f, 1f,
            };
            FloatBuffer lineBuffer = ByteBuffer.allocateDirect(lineData.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
            lineBuffer.put(lineData);
            lineBuffer.position(0);
            GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 7 * 4, lineBuffer);
            GLES20.glEnableVertexAttribArray(positionHandle);
            lineBuffer.position(3);
            GLES20.glVertexAttribPointer(colorHandle, 4, GLES20.GL_FLOAT, false, 4 * 7, lineBuffer);
            GLES20.glEnableVertexAttribArray(colorHandle);
            Matrix.setIdentityM(modelMatrix, 0);
            Matrix.multiplyMM(MVPMatrix, 0, viewMatrix, 0, modelMatrix, 0);
            Matrix.multiplyMM(MVPMatrix, 0, projectionMatrix, 0, MVPMatrix, 0);
            GLES20.glUniformMatrix4fv(MVPMatrixHandle, 1, false, MVPMatrix, 0);
            GLES20.glDrawArrays(GLES20.GL_LINES, 0, 2);
        }

//        private void drawTriangle(final FloatBuffer aTriangleBuffer)
//        {
//            // Pass in the position information
//            aTriangleBuffer.position(0);
//            GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false,
//                    7 * 4, aTriangleBuffer);
//
//            GLES20.glEnableVertexAttribArray(positionHandle);
//
//            // Pass in the color information
//            aTriangleBuffer.position(3);
//            GLES20.glVertexAttribPointer(colorHandle, 4, GLES20.GL_FLOAT, false,
//                    4*7, aTriangleBuffer);
//
//            GLES20.glEnableVertexAttribArray(colorHandle);
//
//            // This multiplies the view matrix by the model matrix, and stores the result in the MVP matrix
//            // (which currently contains model * view).
//            Matrix.multiplyMM(MVPMatrix, 0, viewMatrix, 0, modelMatrix, 0);
//
//            // This multiplies the modelview matrix by the projection matrix, and stores the result in the MVP matrix
//            // (which now contains model * view * projection).
//            Matrix.multiplyMM(MVPMatrix, 0, projectionMatrix, 0, MVPMatrix, 0);
//
//            GLES20.glUniformMatrix4fv(MVPMatrixHandle, 1, false, MVPMatrix, 0);
//            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3);
//        }
    }
}