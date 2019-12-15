/*
 * Copyright (C) 2015 Google Inc.
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

package com.example.playerandrecorder;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;


import androidx.annotation.RequiresApi;

import com.example.playerandrecorder.Application.PlayerRecorderApplication;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.Arrays;

import timber.log.Timber;

public class SoundFile {

    private File mInputFile = null;

    private boolean isFailed = false;

    private float dpPerSec = AppConstants.SHORT_RECORD_DP_PER_SECOND;
    private int mFileSize;
    private int mSampleRate;
    private int mChannels;
    private int mNumSamples;  // total number of samples per channel in audio file
    private ByteBuffer mDecodedBytes;  // Raw audio data
    private ShortBuffer mDecodedSamples;  // shared buffer with mDecodedBytes.

    private int mNumFrames;
    private int[] mFrameGains;
    private long duration;
    private static final String[] SUPPORTED_EXT = new String[]{"mp3", "wav", "3gpp", "3gp", "amr", "aac", "m4a", "ogg"};

    private SoundFile() {
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public static SoundFile create(String fileName) throws IOException, OutOfMemoryError, IllegalStateException, FileNotFoundException {
        // First check that the file exists and that its extension is supported.
        File f = new File(fileName);
        if (!f.exists()) {
            throw new java.io.FileNotFoundException(fileName);
        }
        String name = f.getName().toLowerCase();
        String[] components = name.split("\\.");
        if (components.length < 2) {
            return null;
        }
        if (!Arrays.asList(SUPPORTED_EXT).contains(components[components.length - 1])) {
            return null;
        }
        SoundFile soundFile = new SoundFile();
        soundFile.readFile(f);
        return soundFile;
    }

    public int getSamplesPerFrame() {
        return calculateSamplesPerFrame();
    }

    public long getDuration() {
        return duration;
    }

    private int calculateSamplesPerFrame() {
//		return mSampleRate / AppConstants.PIXELS_PER_SECOND;
        return (int) (mSampleRate / dpPerSec);
    }

    // Should be removed when the app will use directly the samples instead of the frames.
    public int[] getFrameGains() {
        return mFrameGains;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private void readFile(File inputFile) throws IOException, OutOfMemoryError, IllegalStateException {
        MediaExtractor extractor = new MediaExtractor();
        MediaFormat format = null;
        int i;

        mInputFile = inputFile;
        mFileSize = (int) mInputFile.length();
        extractor.setDataSource(mInputFile.getPath());
        int numTracks = extractor.getTrackCount();
        // find and select the first audio track present in the file.
        for (i = 0; i < numTracks; i++) {
            format = extractor.getTrackFormat(i);
            if (format.getString(MediaFormat.KEY_MIME).startsWith("audio/")) {
                extractor.selectTrack(i);
                break;
            }
        }

        if (i == numTracks) {
            throw new IOException("No audio track found in " + mInputFile.toString());
        }
        mChannels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
        mSampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        // Expected total number of samples per channel.
        int expectedNumSamples = 0;
        try {
            expectedNumSamples = (int) ((format.getLong(MediaFormat.KEY_DURATION) / 1000000.f) * mSampleRate + 0.5f);

            //SoundFile duration.
            duration = format.getLong(MediaFormat.KEY_DURATION);
        } catch (Exception e) {
            Timber.e(e);
        }
        dpPerSec = PlayerRecorderApplication.getDpPerSecond((float) duration / 1000000f);

        MediaCodec codec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME));
        codec.configure(format, null, null, 0);
        codec.start();

        int decodedSamplesSize = 0;  // size of the output buffer containing decoded samples.
        byte[] decodedSamples = null;
        ByteBuffer[] inputBuffers = codec.getInputBuffers();
        ByteBuffer[] outputBuffers = codec.getOutputBuffers();
        int sample_size;
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        long presentation_time;
        int tot_size_read = 0;
        boolean done_reading = false;


        try {
            mDecodedBytes = ByteBuffer.allocate(1 << 20);
        } catch (IllegalArgumentException e) {
            Timber.e(e);
            mDecodedBytes = ByteBuffer.allocate(1 << 10);
        }

        Boolean firstSampleData = true;
        while (true) {
            int inputBufferIndex = codec.dequeueInputBuffer(100);
            if (!done_reading && inputBufferIndex >= 0) {
                sample_size = extractor.readSampleData(inputBuffers[inputBufferIndex], 0);
                if (firstSampleData
                        && format.getString(MediaFormat.KEY_MIME).equals("audio/mp4a-latm")
                        && sample_size == 2) {

                    extractor.advance();
                    tot_size_read += sample_size;
                } else if (sample_size < 0) {
                    // All samples have been read.
                    codec.queueInputBuffer(
                            inputBufferIndex, 0, 0, -1, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    done_reading = true;
                } else {
                    presentation_time = extractor.getSampleTime();
                    codec.queueInputBuffer(inputBufferIndex, 0, sample_size, presentation_time, 0);
                    extractor.advance();
                    tot_size_read += sample_size;
                }
                firstSampleData = false;
            }

            int outputBufferIndex = codec.dequeueOutputBuffer(info, 100);
            if (outputBufferIndex >= 0 && info.size > 0) {
                if (decodedSamplesSize < info.size) {
                    decodedSamplesSize = info.size;
                    decodedSamples = new byte[decodedSamplesSize];
                }
                outputBuffers[outputBufferIndex].get(decodedSamples, 0, info.size);
                outputBuffers[outputBufferIndex].clear();
                if (mDecodedBytes.remaining() < info.size) {
                    // Getting a rough estimate of the total size, allocate 20% more, and
                    // make sure to allocate at least 5MB more than the initial size.
                    int position = mDecodedBytes.position();
                    int newSize = (int) ((position * (1.0 * mFileSize / tot_size_read)) * 1.2);
                    if (newSize - position < info.size + 5 * (1 << 20)) {
                        newSize = position + info.size + 5 * (1 << 20);
                    }
                    ByteBuffer newDecodedBytes = null;
                    // Try to allocate memory. If we are OOM, try to run the garbage collector.
                    int retry = 10;
                    while (retry > 0) {
                        try {
                            newDecodedBytes = ByteBuffer.allocate(newSize);
                            break;
                        } catch (OutOfMemoryError oome) {
                            // setting android:largeHeap="true" in <application> seem to help not
                            // reaching this section.
                            retry--;
                        }
                    }
                    if (retry == 0) {
                        // Failed to allocate memory... Stop reading more data and finalize the
                        // instance with the data decoded so far.
                        mFrameGains = new int[PlayerRecorderApplication.getLongWaveformSampleCount()];
                        isFailed = true;
                        break;
                    }
                    //ByteBuffer newDecodedBytes = ByteBuffer.allocate(newSize);
                    mDecodedBytes.rewind();
                    newDecodedBytes.put(mDecodedBytes);
                    mDecodedBytes = newDecodedBytes;
                    mDecodedBytes.position(position);
                }
                mDecodedBytes.put(decodedSamples, 0, info.size);
                codec.releaseOutputBuffer(outputBufferIndex, false);
            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                outputBuffers = codec.getOutputBuffers();
            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {

            }
            if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0
                    || (mDecodedBytes.position() / (2 * mChannels)) >= expectedNumSamples) {
                break;
            }
        }
        mNumSamples = mDecodedBytes.position() / (mChannels * 2);  // One sample = 2 bytes.
        mDecodedBytes.rewind();
        mDecodedBytes.order(ByteOrder.LITTLE_ENDIAN);
        mDecodedSamples = mDecodedBytes.asShortBuffer();

        extractor.release();
        extractor = null;
        codec.stop();
        codec.release();
        codec = null;

        if (!isFailed) {
            // Temporary hack to make it work with the old version.
            mNumFrames = mNumSamples / getSamplesPerFrame();
            if (mNumSamples % getSamplesPerFrame() != 0) {
                mNumFrames++;
            }
            mFrameGains = new int[mNumFrames];
            int j;
            int gain, value;
            for (i = 0; i < mNumFrames; i++) {
                gain = -1;
                for (j = 0; j < getSamplesPerFrame(); j++) {
                    value = 0;
                    for (int k = 0; k < mChannels; k++) {
                        if (mDecodedSamples.remaining() > 0) {
                            value += Math.abs(mDecodedSamples.get());
                        }
                    }
                    value /= mChannels;
                    if (gain < value) {
                        gain = value;
                    }
                }
                mFrameGains[i] = (int) Math.sqrt(gain);  // here gain = sqrt(max value of 1st channel)...
            }
            mDecodedSamples.rewind();
        }
    }
}
