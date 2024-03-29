package com.example.playerandrecorder.Models;


import com.example.playerandrecorder.Recorder.Contractrs.RecorderContract;

import java.util.List;

public interface AppRecorder {

	void addRecordingCallback(AppRecorderCallback recorderCallback);
	void removeRecordingCallback(AppRecorderCallback recorderCallback);
	void setRecorder(RecorderContract.Recorder recorder);
	void startRecording(String filePath);
	void pauseRecording();
	void resumeRecording();
	void stopRecording();
	List<Integer> getRecordingData();
	boolean isRecording();
	boolean isPaused();
	boolean isProcessing();
	void release();
}
