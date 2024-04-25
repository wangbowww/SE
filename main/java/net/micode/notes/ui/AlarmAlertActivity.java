/*
 * Copyright (c) 2010-2011, The MiCode Open Source Community (www.micode.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.micode.notes.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.Window;
import android.view.WindowManager;

import net.micode.notes.R;
import net.micode.notes.data.Notes;
import net.micode.notes.tool.DataUtils;

import java.io.IOException;

/**
 * 这段代码主要是一个活动(Activity)，用于在安卓应用中处理闹钟提醒。
 * 当闹钟响起时，这个活动会显示一个包含笔记片段的对话框，并播放闹钟声音。
 * 用户可以选择关闭闹钟或者进入应用查看完整的笔记内容。
 */
public class AlarmAlertActivity extends Activity implements DialogInterface.OnClickListener, DialogInterface.OnDismissListener {
    private long mNoteId; // 存储笔记ID
    private String mSnippet; // 存储笔记片段
    private static final int SNIPPET_PREW_MAX_LEN = 60; // 定义片段预览的最大长度
    private MediaPlayer mPlayer = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        final Window win = getWindow();
        win.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);

        manageScreenBrightness();

        handleIntent();

        // 如果笔记在数据库中可见，则显示操作对话框并播放闹钟声音，否则结束活动
        if (DataUtils.visibleInNoteDatabase(getContentResolver(), mNoteId, Notes.TYPE_NOTE)) {
            showActionDialog();
            playAlarmSound();
        } else {
            finish();
        }
    }

    /**
     * 管理屏幕亮度设置，确保在需要时亮起屏幕。
     */
    private void manageScreenBrightness() {
        final Window win = getWindow();
        if (!isScreenOn()) {
            win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR);
        }
    }

    /**
     * 从Intent中获取笔记ID和片段。
     */
    private void handleIntent() {
        Intent intent = getIntent();
        try {
            mNoteId = Long.valueOf(intent.getData().getPathSegments().get(1));
            mSnippet = DataUtils.getSnippetById(this.getContentResolver(), mNoteId);
            mSnippet = mSnippet.length() > SNIPPET_PREW_MAX_LEN ? mSnippet.substring(0, SNIPPET_PREW_MAX_LEN) + getResources().getString(R.string.notelist_string_info) : mSnippet;
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            finish();
        }
    }

    /**
     * 检查屏幕是否已经点亮。
     */
    private boolean isScreenOn() {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        return pm.isInteractive(); // Updated to use isInteractive for API level 20 and above
    }

    /**
     * 播放闹钟声音。
     */
    private void playAlarmSound() {
        Uri alarmSoundUri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM);
        mPlayer = new MediaPlayer();

        try {
            mPlayer.setDataSource(this, alarmSoundUri);
            mPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
            mPlayer.prepare();
            mPlayer.setLooping(true);
            mPlayer.start();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (mPlayer != null) {
                mPlayer.setOnCompletionListener(mp -> {
                    stopAlarmSound();
                });
            }
        }
    }

    /**
     * 显示操作对话框。
     */
    private void showActionDialog() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle(R.string.app_name);
        dialog.setMessage(mSnippet);
        dialog.setPositiveButton(R.string.notealert_ok, this);
        if (isScreenOn()) {
            dialog.setNegativeButton(R.string.notealert_enter, this);
        }
        dialog.show().setOnDismissListener(this);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_NEGATIVE) {
            Intent intent = new Intent(this, NoteEditActivity.class);
            intent.setAction(Intent.ACTION_VIEW);
            intent.putExtra(Intent.EXTRA_UID, mNoteId);
            startActivity(intent);
        }
        dialog.dismiss();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        stopAlarmSound();
        finish();
    }

    /**
     * 停止播放闹钟声音并释放资源。
     */
    private void stopAlarmSound() {
        if (mPlayer != null) {
            mPlayer.stop();
            mPlayer.release();
            mPlayer = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAlarmSound(); // Ensure media player is released when Activity is destroyed
    }
}
