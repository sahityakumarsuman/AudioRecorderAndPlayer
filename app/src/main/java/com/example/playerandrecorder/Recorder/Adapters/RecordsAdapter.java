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

package com.example.playerandrecorder.Recorder.Adapters;

import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.playerandrecorder.AppConstants;
import com.example.playerandrecorder.R;
import com.example.playerandrecorder.Recorder.PojosModels.ListItem;
import com.example.playerandrecorder.Utills.TimeUtils;
import com.example.playerandrecorder.Widgets.SimpleWaveformView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

public class RecordsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<ListItem> data;

    private boolean showDateHeaders = true;
    private int activeItem = -1;

    private ItemClickListener itemClickListener;

    public RecordsAdapter() {
        this.data = new ArrayList<>();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int type) {
        if (type == ListItem.ITEM_TYPE_HEADER) {

            View view = new View(viewGroup.getContext());
            int height;

            height = (int) viewGroup.getContext().getResources().getDimension(R.dimen.toolbar_height);


            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, height);
            view.setLayoutParams(lp);
            return new UniversalViewHolder(view);
        } else if (type == ListItem.ITEM_TYPE_FOOTER) {
            View view = new View(viewGroup.getContext());
            int height = (int) viewGroup.getContext().getResources().getDimension(R.dimen.panel_height);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, height);
            view.setLayoutParams(lp);
            return new UniversalViewHolder(view);
        } else if (type == ListItem.ITEM_TYPE_DATE) {
            //Create date list item layout programmatically.
            TextView textView = new TextView(viewGroup.getContext());
            ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            textView.setLayoutParams(lp);
            textView.setTypeface(textView.getTypeface(), Typeface.BOLD);

            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, viewGroup.getContext().getResources().getDimension(R.dimen.text_medium));

            int pad = (int) viewGroup.getContext().getResources().getDimension(R.dimen.spacing_small);
            textView.setPadding(pad, pad, pad, pad);
            textView.setGravity(Gravity.CENTER);

            return new UniversalViewHolder(textView);
        } else {
            View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.list_item, viewGroup, false);
            return new ItemViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder viewHolder, final int pos) {
        if (viewHolder.getItemViewType() == ListItem.ITEM_TYPE_NORMAL) {
            final ItemViewHolder holder = (ItemViewHolder) viewHolder;
            final int p = holder.getAdapterPosition();
            holder.name.setText(data.get(p).getName());
            holder.description.setText(data.get(p).getDurationStr());
            holder.created.setText(data.get(p).getAddedTimeStr());


            if (viewHolder.getLayoutPosition() == activeItem) {
                holder.view.setBackgroundResource(R.color.selected_item_color);
            } else {
                holder.view.setBackgroundResource(android.R.color.transparent);
            }


            holder.waveformView.setWaveform(data.get(p).getAmps());

            holder.view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (itemClickListener != null && data.size() > p) {
                        int lpos = viewHolder.getLayoutPosition();
                        itemClickListener.onItemClick(v, data.get(lpos).getId(), data.get(lpos).getPath(), lpos);
                    }
                }
            });
        } else if (viewHolder.getItemViewType() == ListItem.ITEM_TYPE_DATE) {
            UniversalViewHolder holder = (UniversalViewHolder) viewHolder;
            ((TextView) holder.view).setText(TimeUtils.formatDateSmart(data.get(viewHolder.getAdapterPosition()).getAdded(), holder.view.getContext()));
        }
    }


    public void setActiveItem(int activeItem) {
        int prev = this.activeItem;
        this.activeItem = activeItem;
        notifyItemChanged(prev);
        notifyItemChanged(activeItem);
    }


    public int findPositionById(long id) {
        if (id >= 0) {
            for (int i = 0; i < data.size() - 1; i++) {
                if (data.get(i).getId() == id) {
                    return i;
                }
            }
        }
        return -1;
    }

    public long getRantomPositionTrackId() {
        int randomNum = getRandomNumberInRange(0, data.size() - 1);
        if (randomNum >= 0)
            return data.get(randomNum).getId();
        return -1;
    }


    private static int getRandomNumberInRange(int min, int max) {

        if (min >= max) {
            throw new IllegalArgumentException("max must be greater than min");
        }

        Random r = new Random();
        return r.nextInt((max - min) + 1) + min;
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    @Override
    public int getItemViewType(int position) {
        return data.get(position).getType();
    }

    public void setData(List<ListItem> d, int order) {
        updateShowHeader(order);
        if (showDateHeaders) {
            data = addDateHeaders(d);
        } else {
            data = d;
        }
        data.add(0, ListItem.createHeaderItem());
        notifyDataSetChanged();
    }


    public void addData(List<ListItem> d, int order) {
        if (data.size() > 0) {
            updateShowHeader(order);
            if (showDateHeaders) {
                if (findFooter() >= 0) {
                    data.addAll(data.size() - 1, addDateHeaders(d));
                } else {
                    data.addAll(addDateHeaders(d));
                }
            } else {
                if (findFooter() >= 0) {
                    data.addAll(data.size() - 1, d);
                } else {
                    data.addAll(d);
                }
            }
            notifyItemRangeInserted(data.size() - d.size(), d.size());
        }
    }

    public void updateShowHeader(int order) {
        if (order == AppConstants.SORT_DATE) {
            showDateHeaders = true;
        } else {
            showDateHeaders = false;
        }
    }

    public ListItem getItem(int pos) {
        return data.get(pos);
    }

    public List<ListItem> addDateHeaders(List<ListItem> data) {
        if (data.size() > 0) {
            if (!hasDateHeader(data, data.get(0).getAdded())) {
                data.add(0, ListItem.createDateItem(data.get(0).getAdded()));
            }
            Calendar d1 = Calendar.getInstance();
            d1.setTimeInMillis(data.get(0).getAdded());
            Calendar d2 = Calendar.getInstance();
            for (int i = 1; i < data.size(); i++) {
                d1.setTimeInMillis(data.get(i - 1).getAdded());
                d2.setTimeInMillis(data.get(i).getAdded());
                if (!TimeUtils.isSameDay(d1, d2) && !hasDateHeader(data, data.get(i).getAdded())) {
                    data.add(i, ListItem.createDateItem(data.get(i).getAdded()));
                }
            }
        }
        return data;
    }


    public int getAudioRecordsCount() {
        int count = 0;
        for (int i = 0; i < data.size(); i++) {
            if (data.get(i).getType() == ListItem.ITEM_TYPE_NORMAL) {
                count++;
            }
        }
        return count;
    }

    public void showFooter() {
        if (findFooter() == -1) {
            this.data.add(ListItem.createFooterItem());
            notifyItemInserted(data.size() - 1);
        }
    }

    public void hideFooter() {
        int pos = findFooter();
        if (pos != -1) {
            this.data.remove(pos);
            notifyItemRemoved(pos);
        }
    }

    public long getNextTo(long id) {
        if (id >= 0) {
            for (int i = 0; i < data.size() - 1; i++) {
                if (data.get(i).getId() == id) {
                    if (data.get(i + 1).getId() == -1 && i + 2 < data.size()) {
                        return data.get(i + 2).getId();
                    } else {
                        return data.get(i + 1).getId();
                    }
                }
            }
        }
        return -1;
    }

    public long getPrevTo(long id) {
        if (id >= 0) {
            for (int i = 1; i < data.size(); i++) {
                if (data.get(i).getId() == id) {
                    if (data.get(i - 1).getId() == -1 && i - 2 >= 0) {
                        return data.get(i - 2).getId();
                    } else {
                        return data.get(i - 1).getId();
                    }
                }
            }
        }
        return -1;
    }

    public int findFooter() {
        for (int i = data.size() - 1; i >= 0; i--) {
            if (data.get(i).getType() == ListItem.ITEM_TYPE_FOOTER) {
                return i;
            }
        }
        return -1;
    }


    public boolean hasDateHeader(List<ListItem> data, long time) {
        for (int i = data.size() - 1; i >= 0; i--) {
            if (data.get(i).getType() == ListItem.ITEM_TYPE_DATE) {
                Calendar d1 = Calendar.getInstance();
                d1.setTimeInMillis(data.get(i).getAdded());
                Calendar d2 = Calendar.getInstance();
                d2.setTimeInMillis(time);
                if (TimeUtils.isSameDay(d1, d2)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void setItemClickListener(ItemClickListener itemClickListener) {
        this.itemClickListener = itemClickListener;
    }


    public interface ItemClickListener {
        void onItemClick(View view, long id, String path, int position);
    }


    public class ItemViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        TextView description;
        TextView created;

        SimpleWaveformView waveformView;
        View view;

        ItemViewHolder(View itemView) {
            super(itemView);
            view = itemView;
            name = itemView.findViewById(R.id.list_item_name);
            description = itemView.findViewById(R.id.list_item_description);
            created = itemView.findViewById(R.id.list_item_date);
            waveformView = itemView.findViewById(R.id.list_item_waveform);

        }
    }

    public class UniversalViewHolder extends RecyclerView.ViewHolder {
        View view;

        UniversalViewHolder(View view) {
            super(view);
            this.view = view;
        }
    }
}
