package com.example.playaudiodemo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener,
        AudioManager.OnAudioFocusChangeListener,
        MediaPlayer.OnCompletionListener,
        SeekBar.OnSeekBarChangeListener {

    private static final String TAG = "MainActivity";
    // 界面元素初始定义
    TextView playerPosition, playerDuration, songName, volumeSize;
    SeekBar seekBar, volumeSeekBar;
    ImageView btRew, btPlay, btPause, btFf, btVolume;

    MediaPlayer mediaPlayer;

    private AudioManager mAudioManager;
    private AudioAttributes mAudioAttributes;
    private AudioFocusRequest mFocusRequest;

    public List<SongInfo> songInfoList = new ArrayList<>();
    public int songIndex = 0;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //初始化控件
        initView();
        permissionsRequest();

    }

    // 界面元素实例化
    private void initView() {

        playerPosition = findViewById(R.id.player_position);
        playerDuration = findViewById(R.id.player_duration);
        seekBar = findViewById(R.id.seek_bar);
        btRew = findViewById(R.id.bt_rew);
        btPlay = findViewById(R.id.bt_play);
        btPause = findViewById(R.id.bt_pause);
        btFf = findViewById(R.id.bt_ff);
        songName = findViewById(R.id.song_name);

        volumeSize = findViewById(R.id.volumeSize);
        volumeSeekBar = findViewById(R.id.volumeSeekBar);
        btVolume = findViewById(R.id.bt_volume);

        btPlay.setOnClickListener(this);
        btPause.setOnClickListener(this);
        btFf.setOnClickListener(this);
        btRew.setOnClickListener(this);

    }


    // android8.0以及以上版本适用
    // 初始化设置
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void initMediaPlayer(Context context) {
        // 初始化AudioManager对象
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        //音频焦点处理
        requestFocus();

        if (songInfoList != null && songInfoList.size()>0){
            mediaPlayer = new MediaPlayer();
            // 设置播放流类型
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            setSongInfo();
        }
        // 音频播放监听
        mediaPlayer.setOnCompletionListener(this);

        // 首次进入设置初始音乐进度条最大值
        seekBar.setMax(mediaPlayer.getDuration());

        //为Seekbar添加监听
        seekBar.setOnSeekBarChangeListener(this);
        final Handler handler = new Handler();
        final int delay = 1000;

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                String currentTime = formatTime(mediaPlayer.getCurrentPosition());
                playerPosition.setText(currentTime);
                seekBar.setProgress(mediaPlayer.getCurrentPosition());
                handler.postDelayed(this, delay);
            }
        }, delay);

        //最大音量
        int maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        //当前音量
        int currentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        volumeSeekBar.setMax(maxVolume);
        volumeSeekBar.setProgress(currentVolume);
//        btVolume.setImageResource(R.drawable.ic_round_volume_mute);
        Log.d(TAG, "------------------------------"+volumeSeekBar.getMin());
        Log.d(TAG, "------------------------------"+volumeSeekBar.getMax());

        volumeSeekBar.setOnSeekBarChangeListener(this);
        btVolume.setOnClickListener(this);

    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        boolean flag = super.onKeyUp(keyCode, event);
        int currentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        volumeShow(currentVolume);
        volumeSeekBar.setProgress(currentVolume);
        return flag;
    }

    // 请求获取焦点
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void requestFocus() {
        if (mFocusRequest == null) {
            if (mAudioAttributes == null) {
                mAudioAttributes = new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build();
            }
            mFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(mAudioAttributes)
                    .setWillPauseWhenDucked(true)
                    .setOnAudioFocusChangeListener(this)
                    .build();
        }
        mAudioManager.requestAudioFocus(mFocusRequest);

    }

    // 按钮点击监听回调
    @RequiresApi(api = Build.VERSION_CODES.O)
    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.bt_play:
                if (mediaPlayer != null &&!mediaPlayer.isPlaying()){
                    btPlay.setVisibility(View.GONE);
                    btPause.setVisibility(View.VISIBLE);
                    mediaPlayer.start();
                    seekBar.setMax(mediaPlayer.getDuration());
                }
                break;
            case R.id.bt_pause:
                if (mediaPlayer != null && mediaPlayer.isPlaying()){
                    btPlay.setVisibility(View.VISIBLE);
                    btPause.setVisibility(View.GONE);
                    mediaPlayer.pause();
                    releaseAudioFocus();
                }
                break;
            case R.id.bt_rew:
                if (mediaPlayer != null && songInfoList != null){
                    btPlay.setVisibility(View.GONE);
                    btPause.setVisibility(View.VISIBLE);
                    if (songInfoList != null) {
                        if (songIndex == 0) {
                            //当前为第一首歌时,则切换到列表的最后一首歌
                            songIndex = songInfoList.size() - 1;
                        } else {
                            songIndex --;
                        }
                        mediaPlayer.reset();
                        setSongInfo();
                        mediaPlayer.start();
                        seekBar.setMax(mediaPlayer.getDuration());
                    }
                }
                break;
            case R.id.bt_ff:
                if (mediaPlayer != null && songInfoList != null) {
                    btPlay.setVisibility(View.GONE);
                    btPause.setVisibility(View.VISIBLE);
                    if (songIndex < songInfoList.size() - 1) {
                        //当前为最后一首歌时,则切换到列表的第一首歌
                        songIndex ++;
                    } else {
                        songIndex = 0;
                    }
                    mediaPlayer.reset();
                    setSongInfo();
                    mediaPlayer.start();
                    seekBar.setMax(mediaPlayer.getDuration());
                }
                break;
            case R.id.bt_volume:
                if (volumeSeekBar.getVisibility() == View.GONE){
                    volumeSeekBar.setVisibility(View.VISIBLE);
                    volumeSize.setVisibility(View.VISIBLE);
                } else {
                    volumeSeekBar.setVisibility(View.GONE);
                    volumeSize.setVisibility(View.GONE);
                }
                volumeShow(mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
                break;
            default:
                break;
        }
    }

    // 进度条监听
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        //当进度发生改变时
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        //当开始拖拽进度条时

    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        //当结束拖拽进度条时
        switch (seekBar.getId()){
            case R.id.seek_bar:
                if (!mediaPlayer.isPlaying()){
                    btPlay.setVisibility(View.GONE);
                    btPause.setVisibility(View.VISIBLE);
                    mediaPlayer.start();
                    seekBar.setMax(mediaPlayer.getDuration());
                }
                mediaPlayer.seekTo(seekBar.getProgress());
                Log.d(TAG, "----------------" + seekBar.getProgress());
                break;
            case R.id.volumeSeekBar:
                int sv = seekBar.getProgress();
                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, sv,0);
                volumeShow(sv);
                break;
        }
    }

    // 音频播放完成监听回调
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
//        btFf.performClick();
        if (songInfoList != null) {
            btPlay.setVisibility(View.GONE);
            btPause.setVisibility(View.VISIBLE);
            if (songIndex < songInfoList.size() - 1) {
                //当前为最后一首歌时,则切换到列表的第一首歌
                songIndex ++;
            } else {
                songIndex = 0;
            }
            mediaPlayer.reset();
            setSongInfo();
            mediaPlayer.start();
            seekBar.setMax(mediaPlayer.getDuration());
        }
    }

    // 请求授权
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void permissionsRequest(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        } else {
            ScanMusic(this);
            initMediaPlayer(this);
        }
    }

    // 授权回调
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                ScanMusic(this);
                initMediaPlayer(this);
            } else {
                Toast.makeText(this, "授权失败", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    // 焦点变更回调
    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange){
            case AudioManager.AUDIOFOCUS_LOSS:
                //长时间丢失焦点,当其他应用申请的焦点为AUDIOFOCUS_GAIN时，
                //会触发此回调事件，例如播放QQ音乐，网易云音乐等
                //通常需要暂停音乐播放，若没有暂停播放就会出现和其他音乐同时输出声音
                Log.d(TAG, "AUDIOFOCUS_LOSS");
                //释放焦点，该方法可根据需要来决定是否调用
                //若焦点释放掉之后，将不会再自动获得
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                //短暂性丢失焦点，
                //会触发此回调事件，例如播放短视频，拨打电话等。
                //通常需要暂停音乐播放
                Log.d(TAG, "AUDIOFOCUS_LOSS_TRANSIENT");
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                //短暂性丢失焦点并作降音处理
                Log.d(TAG, "AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK");
                break;
            case AudioManager.AUDIOFOCUS_GAIN:
                //当其他应用申请焦点之后又释放焦点会触发此回调
                //可重新播放音乐
                Log.d(TAG, "AUDIOFOCUS_GAIN");
                break;
        }
    }

    // 释放焦点
    public void releaseAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mAudioManager.abandonAudioFocusRequest(mFocusRequest);
        } else {
            mAudioManager.abandonAudioFocus(this);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null){
            mediaPlayer.stop();
            mediaPlayer.release();
        }
    }

    // 获取本地音乐文件
    private void ScanMusic(Context context){
       Cursor cursor = context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
               , null, null, null, MediaStore.Audio.Media.DEFAULT_SORT_ORDER);

        if (cursor != null && cursor.getCount() > 0){
            // 有下个文件时继续获取数据
            while (cursor.moveToNext()){
                // 判断是否为音乐类型
                int isMusic = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.IS_MUSIC);
                if (isMusic == 0){
                    continue;
                }
                // 歌曲id
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID));
                // 歌曲名称
                String name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
                // 歌手
                String singer = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
                // 持续时间
                long duration = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION));
                // 歌曲大小
                long size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE));
                // 歌曲路径
                String path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));

                SongInfo songInfo = new SongInfo();
                songInfo.setId(id);
                songInfo.setName(name);
                songInfo.setSinger(singer);
                songInfo.setDuration(duration);
                songInfo.setSize(size);
                songInfo.setPath(path);

                songInfoList.add(songInfo);

            }
            cursor.close();
        }
    }

    public void setSongInfo(){
        try {
            String path = songInfoList.get(songIndex).getPath();
            String name = songInfoList.get(songIndex).getName();
            String duration = formatTime((int) songInfoList.get(songIndex).getDuration());
            Log.d(TAG, "播放音乐路径"+path);
            songName.setText(name);
            playerDuration.setText(duration);
            playerPosition.setText(R.string.start_position);
            seekBar.setProgress(0);

            mediaPlayer.setDataSource(path);
            mediaPlayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 转换歌曲时间的格式
    public static String formatTime(int time) {
        String tt;
        if (time / 1000 % 60 < 10) {
            tt = time / 1000 / 60 + ":0" + time / 1000 % 60;
        } else {
            tt = time / 1000 / 60 + ":" + time / 1000 % 60;
        }
        return tt;
    }

    public void volumeShow(int volume){
        if (volumeSeekBar.getVisibility() == View.VISIBLE){
            if (volume < 1){
            btVolume.setImageResource(R.drawable.ic_volume_off);
        } else if (volume < mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)/2){
            btVolume.setImageResource(R.drawable.ic_volume_down);
        } else {
            btVolume.setImageResource(R.drawable.ic_volume_up);
        }
            volumeSize.setText(String.valueOf(volume));
        } else {
            btVolume.setImageResource(R.drawable.ic_round_volume_mute);
        }
    }

}