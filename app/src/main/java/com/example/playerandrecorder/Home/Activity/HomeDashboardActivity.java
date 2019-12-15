package com.example.playerandrecorder.Home.Activity;

import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import com.example.playerandrecorder.Application.PlayerRecorderApplication;
import com.example.playerandrecorder.EventBustModels.EventBusShowToast;
import com.example.playerandrecorder.Home.Adapters.TabAdapter;
import com.example.playerandrecorder.Player.Fragments.PlayerFragment;
import com.example.playerandrecorder.R;
import com.example.playerandrecorder.Recorder.Fragments.RecorderFragment;
import com.google.android.material.tabs.TabLayout;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import butterknife.BindView;
import butterknife.ButterKnife;

public class HomeDashboardActivity extends AppCompatActivity {

    private TabAdapter adapter;
    @BindView(R.id.fragmentPestTabLayout)
    TabLayout fragmentPestTabLayout;
    @BindView(R.id.viewPager)
    ViewPager viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);


        if (!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this);

        adapter = new TabAdapter(getSupportFragmentManager());
        adapter.addFragment(new RecorderFragment(), "Recorder");
        adapter.addFragment(new PlayerFragment(), "Player");

        viewPager.setAdapter(adapter);
        fragmentPestTabLayout.setupWithViewPager(viewPager);

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        PlayerRecorderApplication.getInjector().releaseRecordsPresenter();
    }


    @Subscribe
    public void show_toast(EventBusShowToast eventBusShowToast) {
        Toast.makeText(HomeDashboardActivity.this, eventBusShowToast.getMessage(), Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().unregister(this);
    }
}
