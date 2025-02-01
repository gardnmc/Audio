/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.audio;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Sample that demonstrates how to record a device's microphone using {@link AudioRecord}.
 */
public class AudioRecordActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {

    private static final int SAMPLING_RATE_IN_HZ = 44100;

    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;

    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    /**
     * Factor by that the minimum buffer size is multiplied. The bigger the factor is the less
     * likely it is that samples will be dropped, but more memory will be used. The minimum buffer
     * size is determined by {@link AudioRecord#getMinBufferSize(int, int, int)} and depends on the
     * recording settings.
     */
    private static final int BUFFER_SIZE_FACTOR = 2;

    /**
     * Size of the buffer where the audio data is stored by Android
     */

    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLING_RATE_IN_HZ,
            CHANNEL_CONFIG, AUDIO_FORMAT) * BUFFER_SIZE_FACTOR;
//
//    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLING_RATE_IN_HZ,
//            CHANNEL_CONFIG, AUDIO_FORMAT);

    /**
     * Signals whether a recording is in progress (true) or not (false).
     */
    private final AtomicBoolean recordingInProgress = new AtomicBoolean(false);

    private AudioRecord recorder = null;

    private Thread recordingThread;


    private Button startButton;

    private Button stopButton;

    private TextView blockCountTv;
    private int block_count = 0;

//
//    public native String Java_com_example_audio_AudioRecordActivity_stringFromJNI();
//    public native int test(int i);
//    public native short get_max(ByteBuffer dta, int size);


    /** Native methods, implemented in jni folder */
    public static native void createEngine();
    public static native void createBufferQueueAudioPlayer(int sampleRate, int samplesPerBuf);
    public static native boolean createAssetAudioPlayer(AssetManager assetManager, String filename);
    // true == PLAYING, false == PAUSED
    public static native void setPlayingAssetAudioPlayer(boolean isPlaying);
    public static native boolean createUriAudioPlayer(String uri);
    public static native void setPlayingUriAudioPlayer(boolean isPlaying);
    public static native void setLoopingUriAudioPlayer(boolean isLooping);
    public static native void setChannelMuteUriAudioPlayer(int chan, boolean mute);
    public static native void setChannelSoloUriAudioPlayer(int chan, boolean solo);
    public static native int getNumChannelsUriAudioPlayer();
    public static native void setVolumeUriAudioPlayer(int millibel);
    public static native void setMuteUriAudioPlayer(boolean mute);
    public static native void enableStereoPositionUriAudioPlayer(boolean enable);
    public static native void setStereoPositionUriAudioPlayer(int permille);
    public static native boolean selectClip(int which, int count);
    public static native boolean enableReverb(boolean enabled);
    public static native boolean createAudioRecorder();
    public static native void startRecording();
    public static native void shutdown();








    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.AppTheme);
        setContentView(R.layout.audio);
        int v = 9;

        System.loadLibrary("audio");

//        int i = test(v);
//        String s = stringFromJNI();
//        String s = Java_com_example_audio_AudioRecordActivity_stringFromJNI();

        startButton = (Button) findViewById(R.id.btnStart);
        blockCountTv = (TextView) findViewById(R.id.blockCount);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startRecording();
                startButton.setEnabled(false);
                stopButton.setEnabled(true);
            }
        });

        boolean b = hasPermissions();
        stopButton = (Button) findViewById(R.id.btnStop);
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopRecording();
                startButton.setEnabled(true);
                stopButton.setEnabled(false);

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

//        startButton.setEnabled(true);
//        stopButton.setEnabled(false);
    }

    @Override
    protected void onPause() {
        super.onPause();

        //stopRecording();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 41: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("MG", "RECORD_AUDIO permission was granted");
                } else {
                    Log.d("MG", "RECORD_AUDIO permission was denied");
                }
            }
            case 42: {
                if (grantResults.length > 0
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("MG", "WRITE_EXTERNAL_STORAGE permission was granted");
                } else {
                    Log.d("MG", "WRITE_EXTERNAL_STORAGE permission was denied");
                }
            }
            case 43: {
                if (grantResults.length > 0
                        && grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("MG", "READ_EXTERNAL_STORAGE permission was granted");
                } else {
                    Log.d("MG", "READ_EXTERNAL_STORAGE permission was denied");
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void startRecording() {

        recordingThread = new Thread(new RecordingRunnable(), "Recording Thread");

        block_count = 0;
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLING_RATE_IN_HZ,
                CHANNEL_CONFIG, AUDIO_FORMAT, BUFFER_SIZE);

        recordingInProgress.set(true);
        recorder.startRecording();

        if (recordingThread != null) {
            recordingThread.start();
        }
        else {
            Log.d("TAG", "recordingThread is not null");
        }
    }

    private boolean hasPermissions() {

        int permissionsCode = 42;
        String[] permissions = {Manifest.permission.RECORD_AUDIO,  Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
        Log.d("TAG", "Requesting Permissions");
        requestPermissions(permissions, permissionsCode);

        return true;
    }

    private void stopRecording() {
        if (null == recorder) {
            return;
        }

        recordingInProgress.set(false);
        recorder.stop();
        recorder.release();
        recorder = null;

    }

    private class RecordingRunnable implements Runnable {

        @Override
        public  void run() {
            int result;
            int accum_result = 0;
            int x = 0;
            short v = 0;
            short max = 0;
            boolean fcreated = false;
            FileOutputStream fos = null;

            //final File file = new File(Environment.getExternalStorageDirectory(), "MG/recording.pcm");
            File dir = commonDocumentDirPath("MG");
            final File file = new File(dir, "recording.pcm");
            final ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER_SIZE );

            Log.d("TAG", "run method invoked");

            try {
                if (!file.exists()) {
                    if (file.createNewFile()){
                        fcreated = true;
                    }
                    else {
                        fcreated = false;
                        return;
                    }
                }
                else  {
                    fos = new FileOutputStream(file);
                }
            } catch (Exception e) {
                Log.e("TAG", e.getMessage());
            }

            while (recordingInProgress.get()) {

                v=0;
                max=0;
                result = recorder.read(buffer,  BUFFER_SIZE );
                if (result < 0) {
                    throw new RuntimeException("Reading of audio buffer failed: " +
                            getBufferReadFailureReason(result));
                }

               // max = get_max(buffer, BUFFER_SIZE );

                try {
                    fos.write(buffer.array(), 0, BUFFER_SIZE );
                } catch (IOException e) {
                    throw new RuntimeException("Writing of data block to file failed: ", e);
                }
                buffer.clear();
                block_count++;
                accum_result += result;
                blockCountTv.setText("MaxV: " + Integer.toString(max) + "  Blocks:" + Integer.toHexString(block_count) + ",    KBytes: " + Integer.toString(accum_result/1024));
                if (block_count > 200) {
                    recordingInProgress.set(false);
                }
            }
            try {
                Log.d("TAG", "Closing file, wrote " + Integer.toString(block_count) + " blocks");
                fos.close();
            } catch (IOException e) {
                throw new RuntimeException("Closing File failed: ", e);
            }
        }

        private String getBufferReadFailureReason(int errorCode) {
            switch (errorCode) {
                case AudioRecord.ERROR_INVALID_OPERATION:
                    return "ERROR_INVALID_OPERATION";
                case AudioRecord.ERROR_BAD_VALUE:
                    return "ERROR_BAD_VALUE";
                case AudioRecord.ERROR_DEAD_OBJECT:
                    return "ERROR_DEAD_OBJECT";
                case AudioRecord.ERROR:
                    return "ERROR";
                default:
                    return "Unknown (" + errorCode + ")";
            }
        }
    }


    public static File commonDocumentDirPath(String FolderName)
    {
        File dir = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
        {
            dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/" + FolderName);
        }
        else
        {
            dir = new File(Environment.getExternalStorageDirectory() + "/" + FolderName);
        }

        // Make sure the path directory exists.
        if (!dir.exists())
        {
            // Make it, if it doesn't exit
            boolean success = dir.mkdirs();
            if (!success)
            {
                dir = null;
            }
        }
        return dir;
    }

}