package com.example.playerandrecorder.Player.Fragments;

import android.Manifest;
import android.animation.Animator;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.playerandrecorder.AppConstants;
import com.example.playerandrecorder.Application.PlayerRecorderApplication;
import com.example.playerandrecorder.ColorMap;
import com.example.playerandrecorder.EventBustModels.EventBusShowToast;
import com.example.playerandrecorder.Models.RecordsContract;
import com.example.playerandrecorder.R;
import com.example.playerandrecorder.Recorder.Adapters.RecordsAdapter;
import com.example.playerandrecorder.Recorder.Listeners.EndlessRecyclerViewScrollListener;
import com.example.playerandrecorder.Recorder.PojosModels.ListItem;
import com.example.playerandrecorder.Utills.AndroidUtils;
import com.example.playerandrecorder.Utills.FileUtil;
import com.example.playerandrecorder.Utills.TimeUtils;
import com.example.playerandrecorder.Widgets.SimpleWaveformView;
import com.example.playerandrecorder.Widgets.TouchLayout;
import com.example.playerandrecorder.Widgets.WaveformView;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import timber.log.Timber;

public class PlayerFragment extends Fragment implements RecordsContract.View {


    @BindView(R.id.recycler_view)
    RecyclerView recyclerView;
    private LinearLayoutManager layoutManager;
    private RecordsAdapter adapter;


    @BindView(R.id.progress)
    ProgressBar progressBar;
    @BindView(R.id.bottomDivider)
    View bottomDivider;
    @BindView(R.id.btn_play)
    ImageButton btnPlay;
    @BindView(R.id.btn_stop)
    ImageButton btnStop;
    @BindView(R.id.btn_next)
    ImageButton btnNext;
    @BindView(R.id.btn_prev)
    ImageButton btnPrev;
    @BindView(R.id.btn_delete)
    ImageButton btnDelete;

    @BindView(R.id.btn_shuffle)
    ImageButton btnShuffle;
    @BindView(R.id.txt_progress)
    TextView txtProgress;
    @BindView(R.id.txt_duration)
    TextView txtDuration;
    @BindView(R.id.txt_name)
    TextView txtName;
    @BindView(R.id.txtEmpty)
    TextView txtEmpty;

    @BindView(R.id.touch_layout)
    TouchLayout touchLayout;
    @BindView(R.id.record)
    WaveformView waveformView;
    @BindView(R.id.wave_progress)
    ProgressBar panelProgress;
    @BindView(R.id.play_progress)
    SeekBar playProgress;


    public static final int REQ_CODE_READ_EXTERNAL_STORAGE_PLAYBACK = 406;


    private RecordsContract.UserActionsListener presenter;


    private ColorMap colorMap;
    private boolean isBound = false;

    private Unbinder unbinder;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        colorMap = PlayerRecorderApplication.getInjector().provideColorMap();
        getActivity().setTheme(colorMap.getAppThemeResource());

        View root_view = inflater.inflate(R.layout.activity_records, container, false);
        unbinder = ButterKnife.bind(this, root_view);
        add_event_listeners_on_view();
        return root_view;
    }

    private void add_event_listeners_on_view() {


        playProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    int val = (int) AndroidUtils.dpToPx(progress * waveformView.getWaveformLength() / 1000);
                    waveformView.seekPx(val);
                    presenter.seekPlayback(val);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });


        touchLayout.setBackgroundResource(colorMap.getPlaybackPanelBackground());
        touchLayout.setOnThresholdListener(new TouchLayout.ThresholdListener() {
            @Override
            public void onTopThreshold() {
                hidePanel();
                presenter.stopPlayback();
            }

            @Override
            public void onBottomThreshold() {
                hidePanel();
                presenter.stopPlayback();
            }

            @Override
            public void onTouchDown() {
            }

            @Override
            public void onTouchUp() {
            }
        });


        set_record_list_adapter_to_rv();

    }

    private void set_record_list_adapter_to_rv() {

        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(getActivity().getApplicationContext());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addOnScrollListener(new MyScrollListener(layoutManager));

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                super.onScrolled(rv, dx, dy);

                if (adapter.getItemCount() < 5 || isListOnBottom()) {
                    bottomDivider.setVisibility(View.GONE);
                } else {
                    bottomDivider.setVisibility(View.VISIBLE);
                }
            }
        });


        SimpleWaveformView.setWaveformColorRes(colorMap.getPrimaryColorRes());
        adapter = new RecordsAdapter();
        adapter.setItemClickListener(new RecordsAdapter.ItemClickListener() {
            @Override
            public void onItemClick(View view, long id, String path, final int position) {
                presenter.setActiveRecord(id, new RecordsContract.Callback() {
                    @Override
                    public void onSuccess() {
                        presenter.stopPlayback();
                        if (startPlayback()) {
                            adapter.setActiveItem(position);
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        Timber.e(e);
                    }
                });
            }
        });


        recyclerView.setAdapter(adapter);

        presenter = PlayerRecorderApplication.getInjector().provideRecordsPresenter();

        waveformView.setOnSeekListener(new WaveformView.OnSeekListener() {
            @Override
            public void onSeek(int px) {
                presenter.seekPlayback(px);
            }

            @Override
            public void onSeeking(int px, long mills) {
                if (waveformView.getWaveformLength() > 0) {
                    playProgress.setProgress(1000 * (int) AndroidUtils.pxToDp(px) / waveformView.getWaveformLength());
                }
                txtProgress.setText(TimeUtils.formatTimeIntervalHourMinSec2(mills));
            }
        });


    }

    private boolean startPlayback() {

        if (FileUtil.isFileInExternalStorage(getActivity().getApplicationContext(), presenter.getActiveRecordPath())) {
            if (checkStoragePermissionPlayback()) {
                presenter.startPlayback();
                return true;
            }
        } else {
            presenter.startPlayback();
            return true;
        }
        return false;
    }


    private boolean checkStoragePermissionPlayback() {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (getActivity().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                    && getActivity().checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQ_CODE_READ_EXTERNAL_STORAGE_PLAYBACK);
                return false;
            }
        }
        return true;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQ_CODE_READ_EXTERNAL_STORAGE_PLAYBACK && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED
                && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
            presenter.startPlayback();
        }
    }


    public void setRecordName(final long recordId, File file) {
        //Create dialog layout programmatically.
        LinearLayout container = new LinearLayout(getActivity().getApplicationContext());
        container.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams containerLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        container.setLayoutParams(containerLp);

        final EditText editText = new EditText(getActivity().getApplicationContext());
        ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        editText.setLayoutParams(lp);
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > AppConstants.MAX_RECORD_NAME_LENGTH) {
                    s.delete(s.length() - 1, s.length());
                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });
        editText.setTextColor(getResources().getColor(R.color.md_black_1000));
        editText.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.text_medium));

        int pad = (int) getResources().getDimension(R.dimen.spacing_normal);
        ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(editText.getLayoutParams());
        params.setMargins(pad, pad, pad, pad);
        editText.setLayoutParams(params);
        container.addView(editText);

        final String fileName = FileUtil.removeFileExtension(file.getName());
        editText.setText(fileName);

        AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.record_name)
                .setView(container)
                .setPositiveButton(R.string.btn_save, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        String newName = editText.getText().toString();
                        if (!fileName.equalsIgnoreCase(newName)) {
                            presenter.renameRecord(recordId, newName);
                            presenter.loadRecords();
                        }
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(R.string.btn_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                })
                .create();
        alertDialog.show();
        alertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                hideKeyboard();
            }
        });
        editText.requestFocus();
        editText.setSelection(editText.getText().length());
        showKeyboard();
    }


    public void hideKeyboard() {
        InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
    }


    public void showKeyboard() {
        InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }

    public boolean isListOnBottom() {
        return (layoutManager.findLastCompletelyVisibleItemPosition() == adapter.getItemCount() - 1);
    }


    @OnClick(R.id.btn_play)
    public void click_on_btn_play() {
        startPlayback();
    }


    @OnClick(R.id.btn_shuffle)
    public void click_on_btn_check_bookmark() {
        presenter.pausePlayback();
        long random_track = adapter.getRantomPositionTrackId();
        if (random_track >= 0) {
            presenter.setActiveRecord(random_track, new RecordsContract.Callback() {
                @Override
                public void onSuccess() {
                    presenter.stopPlayback();
                    if (startPlayback()) {
                        int pos2 = adapter.findPositionById(random_track);
                        if (pos2 >= 0) {
                            recyclerView.scrollToPosition(pos2);
                            adapter.setActiveItem(pos2);
                        }
                    }
                }

                @Override
                public void onError(Exception e) {

                }
            });
        }
    }


    @OnClick(R.id.btn_stop)
    public void click_on_btn_stop() {
        presenter.stopPlayback();
        hidePanel();
    }

    @OnClick(R.id.btn_next)
    public void click_on_btn_next() {
        presenter.pausePlayback();
        final long id = adapter.getNextTo(presenter.getActiveRecordId());
        presenter.setActiveRecord(id, new RecordsContract.Callback() {
            @Override
            public void onSuccess() {
                presenter.stopPlayback();
                if (startPlayback()) {
                    int pos = adapter.findPositionById(id);
                    if (pos >= 0) {
                        recyclerView.scrollToPosition(pos);
                        int o = recyclerView.computeVerticalScrollOffset();
                        if (o > 0) {

                        }
                        adapter.setActiveItem(pos);
                    }
                }
            }

            @Override
            public void onError(Exception e) {
                Timber.e(e);
            }
        });
    }

    @OnClick(R.id.btn_prev)
    public void click_on_btn_prev() {
        presenter.pausePlayback();
        final long id2 = adapter.getPrevTo(presenter.getActiveRecordId());


        Timber.d("click_on_btn_prev :::::::+++++++#### id is :: " + id2);
        presenter.setActiveRecord(id2, new RecordsContract.Callback() {
            @Override
            public void onSuccess() {
                presenter.stopPlayback();
                if (startPlayback()) {
                    int pos2 = adapter.findPositionById(id2);
                    if (pos2 >= 0) {
                        recyclerView.scrollToPosition(pos2);
                        adapter.setActiveItem(pos2);
                    }
                }
            }

            @Override
            public void onError(Exception e) {
                Timber.e(e);
            }
        });
    }

    @OnClick(R.id.btn_delete)
    public void click_on_btn_detele() {
        presenter.pausePlayback();
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.warning)
                .setIcon(R.drawable.ic_delete_forever)
                .setMessage(R.string.delete_record)
                .setCancelable(false)
                .setPositiveButton(R.string.btn_yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        presenter.deleteActiveRecord();
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(R.string.btn_no,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                            }
                        });
        AlertDialog alert = builder.create();
        alert.show();
    }


    @OnClick(R.id.txt_name)
    public void click_on_text_name() {
        if (presenter.getActiveRecordId() != -1) {
            setRecordName(presenter.getActiveRecordId(), new File(presenter.getActiveRecordPath()));
        }
    }


    public class MyScrollListener extends EndlessRecyclerViewScrollListener {

        public <L extends RecyclerView.LayoutManager> MyScrollListener(L layoutManager) {
            super(layoutManager);
        }

        @Override
        public void onLoadMore(int page, int totalItemsCount) {
            presenter.loadRecordsPage(page);
        }
    }


    public boolean isListOnTop() {
        return (layoutManager.findFirstCompletelyVisibleItemPosition() == 0);
    }

    public void hidePanel() {
        if (touchLayout.getVisibility() == View.VISIBLE) {
            adapter.hideFooter();
            final ViewPropertyAnimator animator = touchLayout.animate();
            animator.translationY(touchLayout.getHeight())
                    .setDuration(200)
                    .setListener(new Animator.AnimatorListener() {
                        @Override
                        public void onAnimationStart(Animator animation) {
                        }

                        @Override
                        public void onAnimationEnd(Animator animation) {
                            touchLayout.setVisibility(View.GONE);
                            animator.setListener(null);
                        }

                        @Override
                        public void onAnimationCancel(Animator animation) {
                        }

                        @Override
                        public void onAnimationRepeat(Animator animation) {
                        }
                    })
                    .start();
        }
    }


    @Override
    public void onStop() {
        super.onStop();
        if (presenter != null) {
            presenter.unbindView();
        }
    }


    @Override
    public void showPlayStart() {
        btnPlay.setImageResource(R.drawable.ic_pause_64);
    }

    @Override
    public void showPlayPause() {
        btnPlay.setImageResource(R.drawable.ic_play_64);
    }

    @Override
    public void showPlayStop() {
        waveformView.setPlayback(-1);
        btnPlay.setImageResource(R.drawable.ic_play_64);
        playProgress.setProgress(0);
        adapter.setActiveItem(-1);
    }

    @Override
    public void showNextRecord() {

    }

    @Override
    public void showPrevRecord() {

    }

    @Override
    public void showPlayerPanel() {
        if (touchLayout.getVisibility() != View.VISIBLE) {
            touchLayout.setVisibility(View.VISIBLE);
            if (touchLayout.getHeight() == 0) {
                touchLayout.setTranslationY(AndroidUtils.dpToPx(800));
            } else {
                touchLayout.setTranslationY(touchLayout.getHeight());
            }
            adapter.showFooter();
            final ViewPropertyAnimator animator = touchLayout.animate();
            animator.translationY(0)
                    .setDuration(200)
                    .setListener(new Animator.AnimatorListener() {
                        @Override
                        public void onAnimationStart(Animator animation) {
                        }

                        @Override
                        public void onAnimationEnd(Animator animation) {
                            int o = recyclerView.computeVerticalScrollOffset();
                            int r = recyclerView.computeVerticalScrollRange();
                            int e = recyclerView.computeVerticalScrollExtent();
                            float k = (float) o / (float) (r - e);
                            recyclerView.smoothScrollBy(0, (int) (touchLayout.getHeight() * k));
                            animator.setListener(null);
                        }

                        @Override
                        public void onAnimationCancel(Animator animation) {
                        }

                        @Override
                        public void onAnimationRepeat(Animator animation) {
                        }
                    })
                    .start();
        }
    }


    @Override
    public void showWaveForm(int[] waveForm, long duration) {
        waveformView.setWaveform(waveForm);
        waveformView.setPxPerSecond(AndroidUtils.dpToPx(PlayerRecorderApplication.getDpPerSecond((float) duration / 1000000f)));
    }

    @Override
    public void showDuration(final String duration) {
        txtProgress.setText(duration);
        txtDuration.setText(duration);
    }

    @Override
    public void showRecords(List<ListItem> records, int order) {
        if (records.size() == 0) {
            txtEmpty.setVisibility(View.VISIBLE);
            adapter.setData(new ArrayList<ListItem>(), order);
        } else {
            adapter.setData(records, order);
            txtEmpty.setVisibility(View.GONE);
            if (touchLayout.getVisibility() == View.VISIBLE) {
                adapter.showFooter();
            }
        }
    }

    @Override
    public void addRecords(List<ListItem> records, int order) {
        adapter.addData(records, order);
        txtEmpty.setVisibility(View.GONE);
    }

    @Override
    public void onPlayProgress(final long mills, final int px, final int percent) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                waveformView.setPlayback(px);
                txtProgress.setText(TimeUtils.formatTimeIntervalHourMinSec2(mills));
                playProgress.setProgress(percent);
            }
        });
    }


    @Override
    public void showEmptyList() {
        txtEmpty.setText(R.string.no_records);
        txtEmpty.setVisibility(View.VISIBLE);
    }

    @Override
    public void showEmptyBookmarksList() {
        txtEmpty.setText(R.string.no_bookmarks);
        txtEmpty.setVisibility(View.VISIBLE);
    }

    @Override
    public void showPanelProgress() {
        panelProgress.setVisibility(View.VISIBLE);
    }

    @Override
    public void hidePanelProgress() {
        panelProgress.setVisibility(View.GONE);
    }

    @Override
    public void showRecordName(String name) {
        txtName.setText(name);
    }

    @Override
    public void onDeleteRecord(long id) {
//		adapter.deleteItem(id);
        presenter.loadRecords();
        if (adapter.getAudioRecordsCount() == 0) {
            showEmptyList();
        }
    }

    @Override
    public void hidePlayPanel() {
        hidePanel();
    }


    @Override
    public void showSortType(int type) {
        switch (type) {
            case AppConstants.SORT_DATE:

                break;
            case AppConstants.SORT_NAME:

                break;
            case AppConstants.SORT_DURATION:

                break;
        }
    }


    @Override
    public void showProgress() {
        progressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideProgress() {
        progressBar.setVisibility(View.GONE);
    }

    @Override
    public void showError(String message) {
        EventBus.getDefault().post(new EventBusShowToast(message));
    }

    @Override
    public void showError(int resId) {
        EventBus.getDefault().post(new EventBusShowToast(resId + ""));

    }

    @Override
    public void showMessage(int resId) {
        EventBus.getDefault().post(new EventBusShowToast(resId + ""));

    }

    @Override
    public void onStart() {
        super.onStart();
        presenter.bindView(this);
        presenter.loadRecords();
    }


}
