package com.example.playerandrecorder.Models;


import com.example.playerandrecorder.ErrorsHandelers.AppException;

import java.io.File;

public interface AppRecorderCallback {
	void onRecordingStarted();
	void onRecordingPaused();
	void onRecordProcessing();
	void onRecordFinishProcessing();
	void onRecordingStopped(long id, File file);
	void onRecordingProgress(long mills, int amp);
	void onError(AppException throwable);
}
