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

package com.example.playerandrecorder.Models;


import com.example.playerandrecorder.Recorder.PojosModels.ListItem;

import java.util.List;

public interface RecordsContract {

    interface View extends Contract.View {

        void showPlayStart();

        void showPlayPause();

        void showPlayStop();

        void onPlayProgress(long mills, int px, int percent);

        void showNextRecord();

        void showPrevRecord();

        void showPlayerPanel();



        void showWaveForm(int[] waveForm, long duration);

        void showDuration(String duration);

        void showRecords(List<ListItem> records, int order);

        void addRecords(List<ListItem> records, int order);

        void showEmptyList();

        void showEmptyBookmarksList();

        void showPanelProgress();

        void hidePanelProgress();

        void showRecordName(String name);

        void onDeleteRecord(long id);

        void hidePlayPanel();

        void showSortType(int type);



    }

    interface UserActionsListener extends Contract.UserActionsListener<RecordsContract.View> {

        void startPlayback();

        void pausePlayback();

        void seekPlayback(int px);

        void stopPlayback();

        void playNext();

        void playPrev();

        void deleteActiveRecord();

        void deleteRecord(long id, String path);

        void renameRecord(long id, String name);


        void loadRecords();


        void loadRecordsPage(int page);

        void setActiveRecord(long id, Callback callback);

        long getActiveRecordId();

        String getActiveRecordPath();


    }

    interface Callback {
        void onSuccess();

        void onError(Exception e);
    }
}
