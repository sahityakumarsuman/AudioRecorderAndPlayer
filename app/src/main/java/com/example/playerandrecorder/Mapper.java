package com.example.playerandrecorder;


import com.example.playerandrecorder.DatabaseSharePrefUtils.database.Record;
import com.example.playerandrecorder.Recorder.PojosModels.ListItem;
import com.example.playerandrecorder.Utills.TimeUtils;

import java.util.ArrayList;
import java.util.List;

public class Mapper {
    private Mapper() {
    }

    public static ListItem recordToListItem(Record record) {
        return new ListItem(
                record.getId(),
                ListItem.ITEM_TYPE_NORMAL,
                record.getName().substring(0, record.getName().length() - 4),
                TimeUtils.formatTimeIntervalMinSec(record.getDuration() / 1000),
                record.getDuration() / 1000,
                record.getCreated(),
                record.getAdded(),
                record.getPath(),
                record.isBookmarked(),
                record.getAmps());
    }

    public static List<ListItem> recordsToListItems(List<Record> records) {
        List<ListItem> items = new ArrayList<>(records.size());
        for (int i = 0; i < records.size(); i++) {
            items.add(recordToListItem(records.get(i)));
        }
        return items;
    }

}
