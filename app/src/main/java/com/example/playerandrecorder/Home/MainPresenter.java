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

package com.example.playerandrecorder.Home;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;


import androidx.annotation.RequiresApi;

import com.example.playerandrecorder.AppConstants;
import com.example.playerandrecorder.Application.PlayerRecorderApplication;
import com.example.playerandrecorder.BackgroundQueue;
import com.example.playerandrecorder.DatabaseSharePrefUtils.FileRepository;
import com.example.playerandrecorder.DatabaseSharePrefUtils.Prefs;
import com.example.playerandrecorder.DatabaseSharePrefUtils.database.LocalRepository;
import com.example.playerandrecorder.DatabaseSharePrefUtils.database.Record;
import com.example.playerandrecorder.Models.AppRecorder;
import com.example.playerandrecorder.Models.AppRecorderCallback;
import com.example.playerandrecorder.Player.player.PlayerContract;
import com.example.playerandrecorder.R;
import com.example.playerandrecorder.Recorder.Contractrs.RecorderContract;
import com.example.playerandrecorder.Utills.AndroidUtils;
import com.example.playerandrecorder.Utills.FileUtil;
import com.example.playerandrecorder.Utills.TimeUtils;
import com.example.playerandrecorder.ErrorsHandelers.AppException;
import com.example.playerandrecorder.ErrorsHandelers.CantCreateFileException;
import com.example.playerandrecorder.ErrorsHandelers.ErrorParser;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.util.Date;

import timber.log.Timber;

public class MainPresenter implements MainContract.UserActionsListener {

    private MainContract.View view;
    private AppRecorder appRecorder;
    private final PlayerContract.Player audioPlayer;
    private PlayerContract.PlayerCallback playerCallback;
    private AppRecorderCallback appRecorderCallback;
    private final BackgroundQueue loadingTasks;
    private final BackgroundQueue recordingsTasks;
    private final BackgroundQueue importTasks;
    private final FileRepository fileRepository;
    private final LocalRepository localRepository;
    private final Prefs prefs;
    private long songDuration = 0;
    private float dpPerSecond = AppConstants.SHORT_RECORD_DP_PER_SECOND;
    private Record record;
    private boolean isProcessing = false;
    private boolean deleteRecord = false;

    private boolean showImportProgress = false;

    public MainPresenter(final Prefs prefs, final FileRepository fileRepository,
                         final LocalRepository localRepository,
                         PlayerContract.Player audioPlayer,
                         AppRecorder appRecorder,
                         final BackgroundQueue recordingTasks,
                         final BackgroundQueue loadingTasks,
                         final BackgroundQueue importTasks) {
        this.prefs = prefs;
        this.fileRepository = fileRepository;
        this.localRepository = localRepository;
        this.loadingTasks = loadingTasks;
        this.recordingsTasks = recordingTasks;
        this.importTasks = importTasks;
        this.audioPlayer = audioPlayer;
        this.appRecorder = appRecorder;
    }

    @Override
    public void bindView(final MainContract.View v) {
        this.view = v;
        if (showImportProgress) {
            view.showImportStart();
        } else {
            view.hideImportProgress();
        }

        if (!prefs.hasAskToRenameAfterStopRecordingSetting()) {
            prefs.setAskToRenameAfterStopRecording(true);
        }

        if (appRecorder.isPaused()) {
            view.keepScreenOn(false);
            view.showRecordingPause();
        } else if (appRecorder.isRecording()) {
            view.showRecordingStart();
            view.keepScreenOn(prefs.isKeepScreenOn());
            view.updateRecordingView(appRecorder.getRecordingData());
        } else {
            view.showRecordingStop();
            view.keepScreenOn(false);
        }
        if (appRecorder.isProcessing()) {
            view.showRecordProcessing();
        } else {
            view.hideRecordProcessing();
        }

        if (appRecorderCallback == null) {
            appRecorderCallback = new AppRecorderCallback() {
                @Override
                public void onRecordingStarted() {
                    if (view != null) {
                        view.showRecordingStart();
                        view.keepScreenOn(prefs.isKeepScreenOn());

                    }
                }

                @Override
                public void onRecordingPaused() {
                    if (view != null) {
                        view.keepScreenOn(false);
                        view.showRecordingPause();
                    }
                }

                @Override
                public void onRecordProcessing() {
                    if (view != null) {
                        view.showProgress();
                        view.showRecordProcessing();
                    }
                }

                @Override
                public void onRecordFinishProcessing() {
                    if (view != null) {
                        view.hideRecordProcessing();
                        loadActiveRecord();
                    }
                }

                @Override
                public void onRecordingStopped(long id, File file) {
                    if (view != null) {
                        view.keepScreenOn(false);
                        view.hideProgress();
                        view.showRecordingStop();
                        loadActiveRecord();

                        if (deleteRecord) {
                            view.askDeleteRecord();
                            deleteRecord = false;
                        } else if (prefs.isAskToRenameAfterStopRecording()) {
                            view.askRecordingNewName(id, file);
                        }
                    }
                }

                @Override
                public void onRecordingProgress(final long mills, final int amp) {
                    Timber.v("onRecordProgress time = %d, apm = %d", mills, amp);
                    AndroidUtils.runOnUIThread(new Runnable() {
                        @Override
                        public void run() {
                            if (view != null) {
                                view.onRecordingProgress(mills, amp);
                            }
                        }
                    });
                }

                @Override
                public void onError(AppException throwable) {
                    Timber.e(throwable);
                    if (view != null) {
                        view.showError(ErrorParser.parseException(throwable));
                        view.showRecordingStop();
                    }
                }
            };
        }
        appRecorder.addRecordingCallback(appRecorderCallback);

        if (playerCallback == null) {
            playerCallback = new PlayerContract.PlayerCallback() {
                @Override
                public void onPreparePlay() {
                }

                @Override
                public void onStartPlay() {
                    if (view != null) {
                        view.showPlayStart(true);
                        if (record != null) {
                            view.startPlaybackService(record.getName());
                        }
                    }
                }

                @Override
                public void onPlayProgress(final long mills) {
                    if (view != null) {
                        AndroidUtils.runOnUIThread(new Runnable() {
                            @Override
                            public void run() {
                                if (view != null) {
                                    long duration = songDuration / 1000;
                                    if (duration > 0) {
                                        view.onPlayProgress(mills, AndroidUtils.convertMillsToPx(mills,
                                                AndroidUtils.dpToPx(dpPerSecond)), (int) (1000 * mills / duration));
                                    }
                                }
                            }
                        });
                    }
                }

                @Override
                public void onStopPlay() {
                    if (view != null) {
                        view.showPlayStop();
                        view.showDuration(TimeUtils.formatTimeIntervalHourMinSec2(songDuration / 1000));
                    }
                }

                @Override
                public void onPausePlay() {
                    if (view != null) {
                        view.showPlayPause();
                    }
                }

                @Override
                public void onSeek(long mills) {
                }

                @Override
                public void onError(AppException throwable) {

                }

            };
        }

        this.audioPlayer.addPlayerCallback(playerCallback);

        if (audioPlayer.isPlaying()) {
            view.showPlayStart(false);
        } else {
            view.showPlayStop();
        }
    }

    @Override
    public void unbindView() {
        if (view != null) {
            audioPlayer.removePlayerCallback(playerCallback);
            appRecorder.removeRecordingCallback(appRecorderCallback);
            this.view.stopPlaybackService();
            this.view = null;
        }
    }

    @Override
    public void clear() {
        if (view != null) {
            unbindView();
        }
        localRepository.close();
        audioPlayer.release();
        appRecorder.release();
        loadingTasks.close();
        recordingsTasks.close();
    }

    @Override
    public void executeFirstRun() {
        if (prefs.isFirstRun()) {
            prefs.firstRunExecuted();
        }
    }

    @Override
    public void setAudioRecorder(RecorderContract.Recorder recorder) {
        appRecorder.setRecorder(recorder);
    }

    @Override
    public void startRecording() {
        if (audioPlayer.isPlaying()) {
            audioPlayer.stop();
        }
        if (appRecorder.isPaused()) {
            appRecorder.resumeRecording();
        } else if (!appRecorder.isRecording()) {
            try {
                appRecorder.startRecording(fileRepository.provideRecordFile().getAbsolutePath());
            } catch (CantCreateFileException e) {
                e.printStackTrace();
            }
        } else {
            appRecorder.pauseRecording();
        }
    }

    @Override
    public void stopRecording(boolean delete) {
        if (appRecorder.isRecording()) {
            appRecorder.stopRecording();
            deleteRecord = delete;
        }
    }

    @Override
    public void startPlayback() {
        if (record != null) {
            if (!audioPlayer.isPlaying()) {
                audioPlayer.setData(record.getPath());
            }
            audioPlayer.playOrPause();
        }
    }


    @Override
    public void seekPlayback(int px) {
        audioPlayer.seek(AndroidUtils.convertPxToMills(px, AndroidUtils.dpToPx(dpPerSecond)));
    }

    @Override
    public void stopPlayback() {
        audioPlayer.stop();
    }

    @Override
    public void renameRecord(final long id, final String n) {
        if (id < 0 || n == null || n.isEmpty()) {
            AndroidUtils.runOnUIThread(new Runnable() {
                @Override
                public void run() {
                    if (view != null) {
                        view.showError(R.string.error_failed_to_rename);
                    }
                }
            });
            return;
        }
        if (view != null) {
            view.showProgress();
        }
        final String name = FileUtil.removeUnallowedSignsFromName(n);
        loadingTasks.postRunnable(new Runnable() {
            @Override
            public void run() {
//				TODO: This code need to be refactored!
                Record record = localRepository.getRecord((int) id);
                if (record != null) {
                    String nameWithExt;
                    if (prefs.getFormat() == AppConstants.RECORDING_FORMAT_WAV) {
                        nameWithExt = name + AppConstants.EXTENSION_SEPARATOR + AppConstants.WAV_EXTENSION;
                    } else {
                        nameWithExt = name + AppConstants.EXTENSION_SEPARATOR + AppConstants.M4A_EXTENSION;
                    }

                    File file = new File(record.getPath());
                    File renamed = new File(file.getParentFile().getAbsolutePath() + File.separator + nameWithExt);

                    if (renamed.exists()) {
                        AndroidUtils.runOnUIThread(new Runnable() {
                            @Override
                            public void run() {
                                if (view != null) {
                                    view.showError(R.string.error_file_exists);
                                }
                            }
                        });
                    } else {
                        String ext;
                        if (prefs.getFormat() == AppConstants.RECORDING_FORMAT_WAV) {
                            ext = AppConstants.WAV_EXTENSION;
                        } else {
                            ext = AppConstants.M4A_EXTENSION;
                        }
                        if (fileRepository.renameFile(record.getPath(), name, ext)) {
                            MainPresenter.this.record = new Record(record.getId(), nameWithExt, record.getDuration(), record.getCreated(),
                                    record.getAdded(), renamed.getAbsolutePath(), record.isBookmarked(),
                                    record.isWaveformProcessed(), record.getAmps());
                            if (localRepository.updateRecord(MainPresenter.this.record)) {
                                AndroidUtils.runOnUIThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (view != null) {
                                            view.hideProgress();
                                            view.showName(name);
                                        }
                                    }
                                });
                            } else {
                                AndroidUtils.runOnUIThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        view.showError(R.string.error_failed_to_rename);
                                    }
                                });
                                //Restore file name after fail update path in local database.
                                if (renamed.exists()) {
                                    //Try to rename 3 times;
                                    if (!renamed.renameTo(file)) {
                                        if (!renamed.renameTo(file)) {
                                            renamed.renameTo(file);
                                        }
                                    }
                                }
                            }

                        } else {
                            AndroidUtils.runOnUIThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (view != null) {
                                        view.showError(R.string.error_failed_to_rename);
                                    }
                                }
                            });
                        }
                    }
                    AndroidUtils.runOnUIThread(new Runnable() {
                        @Override
                        public void run() {
                            if (view != null) {
                                view.hideProgress();
                            }
                        }
                    });
                }
            }
        });
    }

    @Override
    public void loadActiveRecord() {
        if (!appRecorder.isRecording()) {
            view.showProgress();
            loadingTasks.postRunnable(new Runnable() {
                @Override
                public void run() {
                    final Record rec = localRepository.getRecord((int) prefs.getActiveRecord());
                    record = rec;
                    if (rec != null) {
                        songDuration = rec.getDuration();
                        dpPerSecond = PlayerRecorderApplication.getDpPerSecond((float) songDuration / 1000000f);
                        AndroidUtils.runOnUIThread(new Runnable() {
                            @Override
                            public void run() {
                                if (view != null) {
                                    view.showWaveForm(rec.getAmps(), songDuration);
                                    view.showName(FileUtil.removeFileExtension(rec.getName()));
                                    view.showDuration(TimeUtils.formatTimeIntervalHourMinSec2(songDuration / 1000));
                                    view.showOptionsMenu();
                                    view.hideProgress();
                                }
                            }
                        });
                        if (!rec.isWaveformProcessed() && !isProcessing) {
                            try {
                                if (view != null) {
                                    AndroidUtils.runOnUIThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (view != null) {
                                                view.showRecordProcessing();
                                            }
                                        }
                                    });
                                    isProcessing = true;
                                    localRepository.updateWaveform(rec.getId());
                                    AndroidUtils.runOnUIThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (view != null) {
                                                view.hideRecordProcessing();
                                            }
                                        }
                                    });
                                }
                            } catch (IOException | OutOfMemoryError | IllegalStateException e) {
                                Timber.e(e);
                                AndroidUtils.runOnUIThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (view != null) {
                                            view.showError(R.string.error_process_waveform);
                                        }
                                    }
                                });
                            }
                            isProcessing = false;
                        }
                    } else {
                        AndroidUtils.runOnUIThread(new Runnable() {
                            @Override
                            public void run() {
                                if (view != null) {
                                    view.showWaveForm(new int[]{}, 0);
                                    view.showName("");
                                    view.showDuration(TimeUtils.formatTimeIntervalHourMinSec2(0));
                                    view.hideProgress();
                                    view.hideOptionsMenu();
                                }
                            }
                        });
                    }
                }
            });
        }
    }

    @Override
    public void updateRecordingDir(Context context) {
        fileRepository.updateRecordingDir(context, prefs);
    }

    @Override
    public void setStoragePrivate(Context context) {
        prefs.setStoreDirPublic(false);
        fileRepository.updateRecordingDir(context, prefs);
    }

    @Override
    public boolean isStorePublic() {
        return prefs.isStoreDirPublic();
    }

    @Override
    public String getActiveRecordPath() {
        if (record != null) {
            return record.getPath();
        } else {
            return null;
        }
    }


    @Override
    public int getActiveRecordId() {
        if (record != null) {
            return record.getId();
        } else {
            return -1;
        }
    }

    @Override
    public void deleteActiveRecord() {
        if (record != null) {
            audioPlayer.stop();
        }
        recordingsTasks.postRunnable(new Runnable() {
            @Override
            public void run() {
                if (record != null) {
                    localRepository.deleteRecord(record.getId());
                    fileRepository.deleteRecordFile(record.getPath());
                    prefs.setActiveRecord(-1);
                    dpPerSecond = AppConstants.SHORT_RECORD_DP_PER_SECOND;
                }
                AndroidUtils.runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        if (view != null) {
                            view.showWaveForm(new int[]{}, 0);
                            view.showName("");
                            view.showDuration(TimeUtils.formatTimeIntervalHourMinSec2(0));
                            view.showMessage(R.string.record_deleted_successfully);
                            view.hideOptionsMenu();
                            view.hideProgress();
                            record = null;
                        }
                    }
                });
            }
        });
    }

}
