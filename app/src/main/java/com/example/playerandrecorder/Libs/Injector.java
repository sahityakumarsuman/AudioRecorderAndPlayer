/*
 * Copyright 2018 Dmitriy Ponomarenko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.playerandrecorder.Libs;

import android.content.Context;

import com.example.playerandrecorder.AppConstants;
import com.example.playerandrecorder.AppRecorderImpl;
import com.example.playerandrecorder.BackgroundQueue;
import com.example.playerandrecorder.ColorMap;
import com.example.playerandrecorder.DatabaseSharePrefUtils.FileRepository;
import com.example.playerandrecorder.DatabaseSharePrefUtils.FileRepositoryImpl;
import com.example.playerandrecorder.DatabaseSharePrefUtils.Prefs;
import com.example.playerandrecorder.DatabaseSharePrefUtils.PrefsImpl;
import com.example.playerandrecorder.DatabaseSharePrefUtils.database.LocalRepository;
import com.example.playerandrecorder.DatabaseSharePrefUtils.database.LocalRepositoryImpl;
import com.example.playerandrecorder.DatabaseSharePrefUtils.database.RecordsDataSource;
import com.example.playerandrecorder.Home.MainContract;
import com.example.playerandrecorder.Home.MainPresenter;
import com.example.playerandrecorder.Models.AppRecorder;
import com.example.playerandrecorder.Models.RecordsContract;
import com.example.playerandrecorder.Player.player.AudioPlayer;
import com.example.playerandrecorder.Player.player.PlayerContract;
import com.example.playerandrecorder.Recorder.Contractrs.AudioRecorder;
import com.example.playerandrecorder.Recorder.Contractrs.RecorderContract;
import com.example.playerandrecorder.Recorder.Contractrs.WavRecorder;
import com.example.playerandrecorder.Recorder.RecordsPresenter;


public class Injector {

    private Context context;

    private BackgroundQueue loadingTasks;
    private BackgroundQueue recordingTasks;
    private BackgroundQueue importTasks;
    private BackgroundQueue processingTasks;
    private BackgroundQueue copyTasks;

    private MainContract.UserActionsListener mainPresenter;
    private RecordsContract.UserActionsListener recordsPresenter;


    public Injector(Context context) {
        this.context = context;
    }

    public Prefs providePrefs() {
        return PrefsImpl.getInstance(context);
    }

    public RecordsDataSource provideRecordsDataSource() {
        return RecordsDataSource.getInstance(context);
    }

    public FileRepository provideFileRepository() {
        return FileRepositoryImpl.getInstance(context, providePrefs());
    }

    public LocalRepository provideLocalRepository() {
        return LocalRepositoryImpl.getInstance(provideRecordsDataSource());
    }

    public AppRecorder provideAppRecorder() {
        return AppRecorderImpl.getInstance(provideAudioRecorder(), provideLocalRepository(),
                provideLoadingTasksQueue(), provideProcessingTasksQueue(), providePrefs());
    }

    public BackgroundQueue provideLoadingTasksQueue() {
        if (loadingTasks == null) {
            loadingTasks = new BackgroundQueue("LoadingTasks");
        }
        return loadingTasks;
    }

    public BackgroundQueue provideRecordingTasksQueue() {
        if (recordingTasks == null) {
            recordingTasks = new BackgroundQueue("RecordingTasks");
        }
        return recordingTasks;
    }

    public BackgroundQueue provideImportTasksQueue() {
        if (importTasks == null) {
            importTasks = new BackgroundQueue("ImportTasks");
        }
        return importTasks;
    }

    public BackgroundQueue provideProcessingTasksQueue() {
        if (processingTasks == null) {
            processingTasks = new BackgroundQueue("ProcessingTasks");
        }
        return processingTasks;
    }

    public BackgroundQueue provideCopyTasksQueue() {
        if (copyTasks == null) {
            copyTasks = new BackgroundQueue("CopyTasks");
        }
        return copyTasks;
    }

    public ColorMap provideColorMap() {
        return ColorMap.getInstance(providePrefs());
    }

    public PlayerContract.Player provideAudioPlayer() {
        return AudioPlayer.getInstance();
    }

    public RecorderContract.Recorder provideAudioRecorder() {
        if (providePrefs().getFormat() == AppConstants.RECORDING_FORMAT_WAV) {
            return WavRecorder.getInstance();
        } else {
            return AudioRecorder.getInstance();
        }
    }

    public MainContract.UserActionsListener provideMainPresenter() {
        if (mainPresenter == null) {
            mainPresenter = new MainPresenter(providePrefs(), provideFileRepository(),
                    provideLocalRepository(), provideAudioPlayer(), provideAppRecorder(),
                    provideLoadingTasksQueue(), provideRecordingTasksQueue(), provideImportTasksQueue());
        }
        return mainPresenter;
    }

    public RecordsContract.UserActionsListener provideRecordsPresenter() {
        if (recordsPresenter == null) {
            recordsPresenter = new RecordsPresenter(provideLocalRepository(), provideFileRepository(),
                    provideLoadingTasksQueue(), provideRecordingTasksQueue(), provideCopyTasksQueue(),
                    provideAudioPlayer(), provideAppRecorder(), providePrefs());
        }
        return recordsPresenter;
    }



    public void releaseRecordsPresenter() {
        if (recordsPresenter != null) {
            recordsPresenter.clear();
            recordsPresenter = null;
        }
    }

    public void releaseMainPresenter() {
        if (mainPresenter != null) {
            mainPresenter.clear();
            mainPresenter = null;
        }
    }



    public void closeTasks() {
        loadingTasks.cleanupQueue();
        loadingTasks.close();
        importTasks.cleanupQueue();
        importTasks.close();
        processingTasks.cleanupQueue();
        processingTasks.close();
        recordingTasks.cleanupQueue();
        recordingTasks.close();
    }
}
