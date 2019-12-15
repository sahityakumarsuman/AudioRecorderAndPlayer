package com.example.playerandrecorder.Recorder.Fragments;

import android.Manifest;
import android.animation.Animator;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;

import com.example.playerandrecorder.AppConstants;
import com.example.playerandrecorder.Application.PlayerRecorderApplication;
import com.example.playerandrecorder.ColorMap;
import com.example.playerandrecorder.EventBustModels.EventBusShowToast;
import com.example.playerandrecorder.Home.MainContract;
import com.example.playerandrecorder.R;
import com.example.playerandrecorder.Utills.AndroidUtils;
import com.example.playerandrecorder.Utills.AnimationUtil;
import com.example.playerandrecorder.Utills.FileUtil;
import com.example.playerandrecorder.Utills.ResourcesUtil;
import com.example.playerandrecorder.Utills.TimeUtils;
import com.example.playerandrecorder.Widgets.WaveformView;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import timber.log.Timber;

public class RecorderFragment extends Fragment implements MainContract.View {


    // View Binding
    @BindView(R.id.record)
    WaveformView waveformView;
    @BindView(R.id.txt_progress)
    TextView txtProgress;
    @BindView(R.id.txt_duration)
    TextView txtDuration;
    @BindView(R.id.txt_zero_time)
    TextView txtZeroTime;
    @BindView(R.id.txt_name)
    TextView txtName;
    @BindView(R.id.btn_play)
    ImageButton btnPlay;
    @BindView(R.id.btn_stop)
    ImageButton btnStop;
    @BindView(R.id.btn_record)
    ImageButton btnRecord;
    @BindView(R.id.btn_record_delete)
    ImageButton btnDelete;
    @BindView(R.id.btn_record_stop)
    ImageButton btnRecordingStop;


    @BindView(R.id.progress)
    ProgressBar progressBar;
    @BindView(R.id.play_progress)
    SeekBar playProgress;
    @BindView(R.id.pnl_import_progress)
    LinearLayout pnlImportProgress;
    @BindView(R.id.pnl_record_processing)
    LinearLayout pnlRecordProcessing;

    // Constants
    public static final int REQ_CODE_REC_AUDIO_AND_WRITE_EXTERNAL = 101;
    public static final int REQ_CODE_RECORD_AUDIO = 303;
    public static final int REQ_CODE_WRITE_EXTERNAL_STORAGE = 404;
    public static final int REQ_CODE_READ_EXTERNAL_STORAGE_IMPORT = 405;
    public static final int REQ_CODE_READ_EXTERNAL_STORAGE_PLAYBACK = 406;
    public static final int REQ_CODE_IMPORT_AUDIO = 11;

    // Necessary Variable Declation
    private MainContract.UserActionsListener presenter;
    private ServiceConnection serviceConnection;
    private boolean isBound = false;
    private ColorMap colorMap;
    private ColorMap.OnThemeColorChangeListener onThemeColorChangeListener;

    private Unbinder unbinder;


    @Override
    public void onStart() {
        super.onStart();
        presenter.bindView(RecorderFragment.this);
        presenter.setAudioRecorder(PlayerRecorderApplication.getInjector().provideAudioRecorder());
        presenter.updateRecordingDir(PlayerRecorderApplication.getApplicationContextFromApp());
        presenter.loadActiveRecord();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        colorMap = PlayerRecorderApplication.getInjector().provideColorMap();
        getActivity().setTheme(colorMap.getAppThemeResource());

        View root_view = inflater.inflate(R.layout.fragment_recorder_layout, container, false);

        unbinder = ButterKnife.bind(this, root_view);


        add_event_on_views();

        return root_view;


    }

    private void add_event_on_views() {

        txtProgress.setText(TimeUtils.formatTimeIntervalHourMinSec2(0));

        btnDelete.setVisibility(View.INVISIBLE);
        btnDelete.setEnabled(false);
        btnRecordingStop.setVisibility(View.INVISIBLE);
        btnRecordingStop.setEnabled(false);

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

        presenter = PlayerRecorderApplication.getInjector().provideMainPresenter();
        presenter.executeFirstRun();


        presenter = PlayerRecorderApplication.getInjector().provideMainPresenter();
        presenter.executeFirstRun();

        waveformView.setOnSeekListener(new WaveformView.OnSeekListener() {
            @Override
            public void onSeek(int px) {
                presenter.seekPlayback(px);
            }

            @Override
            public void onSeeking(int px, long mills) {
                int length = waveformView.getWaveformLength();
                if (length > 0) {
                    playProgress.setProgress(1000 * (int) AndroidUtils.pxToDp(px) / length);
                }
                txtProgress.setText(TimeUtils.formatTimeIntervalHourMinSec2(mills));
            }
        });
        onThemeColorChangeListener = new ColorMap.OnThemeColorChangeListener() {
            @Override
            public void onThemeColorChange(int pos) {
                getActivity().setTheme(colorMap.getAppThemeResource());
                getActivity().recreate();
            }
        };
        colorMap.addOnThemeColorChangeListener(onThemeColorChangeListener);

    }


    @OnClick(R.id.btn_play)
    public void click_on_btn_play() {

        if (FileUtil.isFileInExternalStorage(getActivity().getApplicationContext(), presenter.getActiveRecordPath())) {
            if (checkStoragePermissionPlayback()) {
                presenter.startPlayback();
            }
        } else {
            presenter.startPlayback();
        }
    }

    @OnClick(R.id.btn_record)
    public void click_on_btn_record() {
        if (checkRecordPermission2()) {
            if (checkStoragePermission2()) {
                Timber.d("all given working show");
                presenter.startRecording();
            } else {
                Timber.d("Permission Storage not given");
            }
        } else {
            Timber.d("Permission Record not given");
        }
    }


    @OnClick(R.id.btn_record_stop)
    public void click_on_btn_record_stop() {
        presenter.stopRecording(false);

    }


    @OnClick(R.id.btn_stop)
    public void click_on_btn_stop() {
        presenter.stopPlayback();
    }


    @OnClick(R.id.txt_name)
    public void click_on_txt_name() {
        if (presenter.getActiveRecordId() != -1) {
            setRecordName(presenter.getActiveRecordId(), new File(presenter.getActiveRecordPath()));
        }

    }

    @OnClick(R.id.btn_record_delete)
    public void click_on_btn_delete() {
        presenter.stopRecording(true);
    }


    @Override
    public void keepScreenOn(boolean on) {

        if (on) {
            getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    @Override
    public void showRecordingStart() {

        txtName.setClickable(false);
        txtName.setFocusable(false);
        txtName.setCompoundDrawables(null, null, null, null);
        txtName.setVisibility(View.VISIBLE);
        txtName.setText(R.string.recording_progress);
        txtZeroTime.setVisibility(View.INVISIBLE);
        txtDuration.setVisibility(View.INVISIBLE);
        btnRecord.setImageResource(R.drawable.ic_pause_circle_filled);
        btnPlay.setEnabled(false);
        btnDelete.setVisibility(View.VISIBLE);
        btnDelete.setEnabled(true);
        btnRecordingStop.setVisibility(View.VISIBLE);
        btnRecordingStop.setEnabled(true);
        playProgress.setProgress(0);
        playProgress.setEnabled(false);
        txtDuration.setText(R.string.zero_time);
        waveformView.showRecording();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void showRecordingStop() {

        txtName.setClickable(true);
        txtName.setFocusable(true);
        txtName.setText("");
        txtZeroTime.setVisibility(View.VISIBLE);
        txtDuration.setVisibility(View.VISIBLE);
        txtName.setCompoundDrawablesWithIntrinsicBounds(null, null, getActivity().getDrawable(R.drawable.ic_pencil_small), null);
        txtName.setVisibility(View.INVISIBLE);
        btnRecord.setImageResource(R.drawable.ic_record);
        btnPlay.setEnabled(true);

        playProgress.setEnabled(true);
        btnDelete.setVisibility(View.INVISIBLE);
        btnDelete.setEnabled(false);
        btnRecordingStop.setVisibility(View.INVISIBLE);
        btnRecordingStop.setEnabled(false);
        waveformView.hideRecording();
        waveformView.clearRecordingData();

    }

    @Override
    public void showRecordingPause() {

        txtName.setText(R.string.recording_paused);
        btnRecord.setImageResource(R.drawable.ic_record_rec);
    }

    @Override
    public void onRecordingProgress(long mills, int amp) {

        txtProgress.setText(TimeUtils.formatTimeIntervalHourMinSec2(mills));
        waveformView.addRecordAmp(amp);
    }

    @Override
    public void askRecordingNewName(long id, File file) {
        setRecordName(id, file);
    }


    @Override
    public void startPlaybackService(String name) {

    }

    @Override
    public void stopPlaybackService() {
        if (isBound && serviceConnection != null) {
            getActivity().unbindService(serviceConnection);
            isBound = false;
        }

    }

    @Override
    public void showPlayStart(boolean animate) {
        btnRecord.setEnabled(false);
        if (animate) {
            AnimationUtil.viewAnimationX(btnPlay, -75f, new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    btnStop.setVisibility(View.VISIBLE);
                    btnPlay.setImageResource(R.drawable.ic_pause);
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                }

                @Override
                public void onAnimationRepeat(Animator animation) {
                }
            });
        } else {
            btnPlay.setTranslationX(-75f);
            btnStop.setVisibility(View.VISIBLE);
            btnPlay.setImageResource(R.drawable.ic_pause);
        }
    }

    @Override
    public void showPlayPause() {
        btnPlay.setImageResource(R.drawable.ic_play);
    }

    @Override
    public void showPlayStop() {
        btnPlay.setImageResource(R.drawable.ic_play);
        waveformView.setPlayback(-1);
        btnRecord.setEnabled(true);
        playProgress.setProgress(0);
        txtProgress.setText(TimeUtils.formatTimeIntervalHourMinSec2(0));
        AnimationUtil.viewAnimationX(btnPlay, 0f, new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                btnStop.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });
    }

    @Override
    public void onPlayProgress(long mills, int px, int percent) {
        playProgress.setProgress(percent);
        waveformView.setPlayback(px);
        txtProgress.setText(TimeUtils.formatTimeIntervalHourMinSec2(mills));

    }

    @Override
    public void showImportStart() {
        pnlImportProgress.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideImportProgress() {
        pnlImportProgress.setVisibility(View.INVISIBLE);
    }

    @Override
    public void showOptionsMenu() {

    }

    @Override
    public void hideOptionsMenu() {

    }

    @Override
    public void showRecordProcessing() {
        pnlRecordProcessing.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideRecordProcessing() {
        pnlRecordProcessing.setVisibility(View.INVISIBLE);
    }

    @Override
    public void showWaveForm(int[] waveForm, long duration) {
        if (waveForm.length > 0) {
            btnPlay.setVisibility(View.VISIBLE);
            txtDuration.setVisibility(View.VISIBLE);
            txtZeroTime.setVisibility(View.VISIBLE);
        } else {
            btnPlay.setVisibility(View.INVISIBLE);
            txtDuration.setVisibility(View.INVISIBLE);
            txtZeroTime.setVisibility(View.INVISIBLE);
        }
        waveformView.setWaveform(waveForm);
        waveformView.setPxPerSecond(AndroidUtils.dpToPx(PlayerRecorderApplication.getDpPerSecond((float) duration / 1000000f)));

    }

    @Override
    public void showDuration(String duration) {
        txtDuration.setText(duration);
    }

    @Override
    public void showName(String name) {
        if (name == null || name.isEmpty()) {
            txtName.setVisibility(View.INVISIBLE);
        } else if (txtName.getVisibility() == View.INVISIBLE) {
            txtName.setVisibility(View.VISIBLE);
        }
        txtName.setText(name);
    }

    @Override
    public void askDeleteRecord() {
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


    @Override
    public void updateRecordingView(List<Integer> data) {
        waveformView.setRecordingData(data);
    }

    @Override
    public void showProgress() {
        waveformView.setVisibility(View.INVISIBLE);
        progressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideProgress() {
        waveformView.setVisibility(View.VISIBLE);
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

    private boolean checkRecordPermission2() {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (getActivity().checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_CODE_RECORD_AUDIO);
                return false;
            }
        }
        return true;
    }

    private boolean checkStoragePermission2() {
        if (presenter.isStorePublic()) {
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (getActivity().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    AndroidUtils.showDialog(getActivity(), R.string.warning, R.string.need_write_permission,
                            new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    requestPermissions(
                                            new String[]{
                                                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                                    Manifest.permission.READ_EXTERNAL_STORAGE},
                                            REQ_CODE_WRITE_EXTERNAL_STORAGE);
                                }
                            }, null

                    );
                    return false;
                }
            }
        }
        return true;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQ_CODE_REC_AUDIO_AND_WRITE_EXTERNAL && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED
                && grantResults[1] == PackageManager.PERMISSION_GRANTED
                && grantResults[2] == PackageManager.PERMISSION_GRANTED) {
            presenter.startRecording();
        } else if (requestCode == REQ_CODE_RECORD_AUDIO && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (checkStoragePermission2()) {
                presenter.startRecording();
            }
        } else if (requestCode == REQ_CODE_WRITE_EXTERNAL_STORAGE && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED
                && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
            if (checkRecordPermission2()) {
                presenter.startRecording();
            }
        } else if (requestCode == REQ_CODE_READ_EXTERNAL_STORAGE_IMPORT && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED
                && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
            startFileSelector();
        } else if (requestCode == REQ_CODE_READ_EXTERNAL_STORAGE_PLAYBACK && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED
                && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
            presenter.startPlayback();
        } else if (requestCode == REQ_CODE_WRITE_EXTERNAL_STORAGE && grantResults.length > 0
                && (grantResults[0] == PackageManager.PERMISSION_DENIED
                || grantResults[1] == PackageManager.PERMISSION_DENIED)) {
            presenter.setStoragePrivate(getActivity().getApplicationContext());
            presenter.startRecording();
        }
    }


    private void startFileSelector() {
        Intent intent_upload = new Intent();
        intent_upload.setType("audio/*");
        intent_upload.addCategory(Intent.CATEGORY_OPENABLE);
        intent_upload.setAction(Intent.ACTION_OPEN_DOCUMENT);
        startActivityForResult(intent_upload, REQ_CODE_IMPORT_AUDIO);
    }

    private boolean checkStoragePermissionImport() {
        if (presenter.isStorePublic()) {
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (getActivity().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                        && getActivity().checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(
                            new String[]{
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                    Manifest.permission.READ_EXTERNAL_STORAGE},
                            REQ_CODE_READ_EXTERNAL_STORAGE_IMPORT);
                    return false;
                }
            }
        }
        return true;
    }


    public void setRecordName(final long recordId, File file) {
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
        editText.setTextColor(getResources().getColor(R.color.text_primary_light));
        editText.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.text_medium));
        editText.setTextColor(ResourcesUtil.getColor(R.color.md_black_1000));

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


    /**
     * Show soft keyboard for a dialog.
     */
    public void showKeyboard() {
        InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }

    /**
     * Hide soft keyboard after a dialog.
     */
    public void hideKeyboard() {
        InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        colorMap.removeOnThemeColorChangeListener(onThemeColorChangeListener);
    }


    @Override
    public void onStop() {
        super.onStop();
        if (presenter != null) {
            presenter.unbindView();
        }
    }

}
