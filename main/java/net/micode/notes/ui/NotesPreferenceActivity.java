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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import net.micode.notes.R;
import net.micode.notes.data.Notes;
import net.micode.notes.data.Notes.NoteColumns;
import net.micode.notes.gtask.remote.GTaskSyncService;

// 笔记应用的偏好设置活动类，允许用户配置个性化设置
public class NotesPreferenceActivity extends PreferenceActivity {
    public static final String PREFERENCE_NAME = "notes_preferences"; // 偏好设置的名称

    public static final String PREFERENCE_SYNC_ACCOUNT_NAME = "pref_key_account_name"; // 同步账户名称的偏好键

    public static final String PREFERENCE_LAST_SYNC_TIME = "pref_last_sync_time"; // 上次同步时间的偏好键

    public static final String PREFERENCE_SET_BG_COLOR_KEY = "pref_key_bg_random_appear"; // 背景颜色设置的偏好键

    private static final String PREFERENCE_SYNC_ACCOUNT_KEY = "pref_sync_account_key"; // 同步账户的偏好键

    private static final String AUTHORITIES_FILTER_KEY = "authorities"; // 权限过滤键

    private PreferenceCategory mAccountCategory; // 账户类别的偏好

    private GTaskReceiver mReceiver; // 广播接收器，用于接收同步服务的状态变更

    private Account[] mOriAccounts; // 原始账户数组

    private boolean mHasAddedAccount; // 是否添加了账户

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        /* 使用应用图标作为导航 */
        getActionBar().setDisplayHomeAsUpEnabled(true);

        // 从资源文件中添加偏好设置项
        addPreferencesFromResource(R.xml.preferences);
        mAccountCategory = (PreferenceCategory) findPreference(PREFERENCE_SYNC_ACCOUNT_KEY);
        mReceiver = new GTaskReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(GTaskSyncService.GTASK_SERVICE_BROADCAST_NAME);
        registerReceiver(mReceiver, filter);

        // 初始化原始账户数组和视图
        mOriAccounts = null;
        View header = LayoutInflater.from(this).inflate(R.layout.settings_header, null);
        getListView().addHeaderView(header, null, true);
    }


    @Override
    protected void onResume() {
        super.onResume();

        // 如果用户添加了新账户，则自动设置同步账户
        if (mHasAddedAccount) {
            Account[] accounts = getGoogleAccounts();
            if (mOriAccounts != null && accounts.length > mOriAccounts.length) {
                for (Account accountNew : accounts) {
                    boolean found = false;
                    for (Account accountOld : mOriAccounts) {
                        if (TextUtils.equals(accountOld.name, accountNew.name)) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        setSyncAccount(accountNew.name);
                        break;
                    }
                }
            }
        }

        // 刷新界面
        refreshUI();
    }

    @Override
    protected void onDestroy() {  // 注销广播接收
        if (mReceiver != null) {
            unregisterReceiver(mReceiver);// 注销接收器
        }
        super.onDestroy();
    }

    // 加载账户相关的偏好设置
    private void loadAccountPreference() {
        mAccountCategory.removeAll(); // 清空账户类别中的偏好

        Preference accountPref = new Preference(this); // 创建新的偏好项
        final String defaultAccount = getSyncAccountName(this); // 获取当前设置的同步账户名
        accountPref.setTitle(getString(R.string.preferences_account_title)); // 设置偏好项标题
        accountPref.setSummary(getString(R.string.preferences_account_summary)); // 设置偏好项摘要
        accountPref.setOnPreferenceClickListener(new OnPreferenceClickListener() { // 设置点击监听器
            public boolean onPreferenceClick(Preference preference) {
                if (!GTaskSyncService.isSyncing()) { // 检查是否正在同步
                    if (TextUtils.isEmpty(defaultAccount)) { // 如果没有设置账户，显示选择账户对话框
                        showSelectAccountAlertDialog();
                    } else { // 如果已设置账户，显示更改账户确认对话框
                        showChangeAccountConfirmAlertDialog();
                    }
                } else {
                    Toast.makeText(NotesPreferenceActivity.this,
                            R.string.preferences_toast_cannot_change_account, Toast.LENGTH_SHORT)
                            .show(); // 如果正在同步，提示用户无法更改账户
                }
                return true;
            }
        });

        mAccountCategory.addPreference(accountPref); // 将新的偏好项添加到账户类别中
    }

    // 加载同步按钮相关设置
    private void loadSyncButton() {
        Button syncButton = (Button) findViewById(R.id.preference_sync_button); // 获取同步按钮
        TextView lastSyncTimeView = (TextView) findViewById(R.id.prefenerece_sync_status_textview); // 获取显示最后同步时间的文本视图

        // 设置按钮状态
        if (GTaskSyncService.isSyncing()) {
            syncButton.setText(getString(R.string.preferences_button_sync_cancel)); // 如果正在同步，设置按钮文本为"取消同步"
            syncButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    GTaskSyncService.cancelSync(NotesPreferenceActivity.this); // 设置按钮点击事件为取消同步
                }
            });
        } else {
            syncButton.setText(getString(R.string.preferences_button_sync_immediately)); // 如果未在同步，设置按钮文本为"立即同步"
            syncButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    GTaskSyncService.startSync(NotesPreferenceActivity.this); // 设置按钮点击事件为开始同步
                }
            });
        }
        syncButton.setEnabled(!TextUtils.isEmpty(getSyncAccountName(this))); // 只有设置了同步账户时，按钮才可用

        // 设置最后同步时间显示
        if (GTaskSyncService.isSyncing()) {
            lastSyncTimeView.setText(GTaskSyncService.getProgressString()); // 如果正在同步，显示同步进度
            lastSyncTimeView.setVisibility(View.VISIBLE);
        } else {
            long lastSyncTime = getLastSyncTime(this); // 获取最后同步时间
            if (lastSyncTime != 0) {
                lastSyncTimeView.setText(getString(R.string.preferences_last_sync_time,
                        DateFormat.format(getString(R.string.preferences_last_sync_time_format),
                                lastSyncTime))); // 显示格式化的最后同步时间
                lastSyncTimeView.setVisibility(View.VISIBLE);
            } else {
                lastSyncTimeView.setVisibility(View.GONE); // 如果没有同步过，不显示时间
            }
        }
    }

    // 刷新用户界面
    private void refreshUI() {
        loadAccountPreference(); // 重新加载账户偏好设置
        loadSyncButton(); // 重新加载同步按钮设置
    }

    // 显示选择账户的对话框
    private void showSelectAccountAlertDialog() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        
        View titleView = LayoutInflater.from(this).inflate(R.layout.account_dialog_title, null); // 加载对话框标题布局
        TextView titleTextView = (TextView) titleView.findViewById(R.id.account_dialog_title); // 获取标题视图
        titleTextView.setText(getString(R.string.preferences_dialog_select_account_title)); // 设置标题文本
        TextView subtitleTextView = (TextView) titleView.findViewById(R.id.account_dialog_subtitle); // 获取副标题视图
        subtitleTextView.setText(getString(R.string.preferences_dialog_select_account_tips)); // 设置副标题文本

        dialogBuilder.setCustomTitle(titleView); // 设置自定义标题
                dialogBuilder.setPositiveButton(null, null); // 不设置确定按钮，使用单选项自动处理

        Account[] accounts = getGoogleAccounts(); // 获取谷歌账户
        String defAccount = getSyncAccountName(this); // 获取当前设置的同步账户名称

        mOriAccounts = accounts; // 保存当前账户状态以用于后续比较
        mHasAddedAccount = false; // 重置账户添加状态

        if (accounts.length > 0) {
            CharSequence[] items = new CharSequence[accounts.length]; // 账户名称数组
            final CharSequence[] itemMapping = items; // 映射关系，用于点击事件中获取账户名称
            int checkedItem = -1; // 当前选中的账户索引
            int index = 0;
            for (Account account : accounts) {
                if (TextUtils.equals(account.name, defAccount)) {
                    checkedItem = index; // 标记已设置的账户为选中状态
                }
                items[index++] = account.name; // 填充账户名称数组
            }
            dialogBuilder.setSingleChoiceItems(items, checkedItem, // 设置单选项
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            setSyncAccount(itemMapping[which].toString()); // 设置选中的账户为同步账户
                            dialog.dismiss(); // 关闭对话框
                            refreshUI(); // 刷新界面
                        }
                    });
        }

        View addAccountView = LayoutInflater.from(this).inflate(R.layout.add_account_text, null); // 加载添加账户视图
        dialogBuilder.setView(addAccountView); // 设置添加账户视图

        final AlertDialog dialog = dialogBuilder.show(); // 显示对话框
        addAccountView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mHasAddedAccount = true; // 标记为已添加账户
                Intent intent = new Intent("android.settings.ADD_ACCOUNT_SETTINGS"); // 跳转到添加账户设置页面
                intent.putExtra(AUTHORITIES_FILTER_KEY, new String[] {
                    "gmail-ls"
                });
                startActivityForResult(intent, -1); // 启动添加账户活动
                dialog.dismiss(); // 关闭对话框
            }
        });
    }


    // 显示更改账户确认对话框
    private void showChangeAccountConfirmAlertDialog() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        View titleView = LayoutInflater.from(this).inflate(R.layout.account_dialog_title, null); // 加载标题布局
        TextView titleTextView = (TextView) titleView.findViewById(R.id.account_dialog_title); // 获取标题视图
        titleTextView.setText(getString(R.string.preferences_dialog_change_account_title,
                getSyncAccountName(this))); // 设置标题文本，包括当前账户名称
        TextView subtitleTextView = (TextView) titleView.findViewById(R.id.account_dialog_subtitle); // 获取副标题视图
        subtitleTextView.setText(getString(R.string.preferences_dialog_change_account_warn_msg)); // 设置副标题文本，警告更改账户可能的影响

        dialogBuilder.setCustomTitle(titleView); // 设置自定义标题

        CharSequence[] menuItemArray = new CharSequence[] {
                getString(R.string.preferences_menu_change_account), // 更改账户选项
                getString(R.string.preferences_menu_remove_account), // 移除账户选项
                getString(R.string.preferences_menu_cancel) // 取消操作选项
        };
        dialogBuilder.setItems(menuItemArray, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) { // 如果选择更改账户
                    showSelectAccountAlertDialog(); // 显示选择账户对话框
                } else if (which == 1) { // 如果选择移除账户
                    removeSyncAccount(); // 移除同步账户
                    refreshUI(); // 刷新界面
                }
                // 如果选择取消，对话框自动关闭，无需额外操作
            }
        });
        dialogBuilder.show(); // 显示对话框
    }

    // 获取当前设备上的Google账户
    private Account[] getGoogleAccounts() {
        AccountManager accountManager = AccountManager.get(this); // 获取账户管理器实例
        return accountManager.getAccountsByType("com.google"); // 返回所有类型为"com.google"的账户
    }

    // 设置同步账户
    private void setSyncAccount(String account) {
        if (!getSyncAccountName(this).equals(account)) { // 如果新账户和当前账户不一致
            SharedPreferences settings = getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE); // 获取偏好设置编辑器
            SharedPreferences.Editor editor = settings.edit();
            if (account != null) {
                editor.putString(PREFERENCE_SYNC_ACCOUNT_NAME, account); // 更新账户名称
            } else {
                editor.putString(PREFERENCE_SYNC_ACCOUNT_NAME, ""); // 清空账户名称
            }
            editor.commit(); // 提交修改

            // 重置最后同步时间
            setLastSyncTime(this, 0);

            // 清理本地相关同步信息
            new Thread(new Runnable() {
                public void run() {
                    ContentValues values = new ContentValues();
                    values.put(NoteColumns.GTASK_ID, ""); // 清空GTASK ID
                    values.put(NoteColumns.SYNC_ID, 0); // 重置同步ID
                    getContentResolver().update(Notes.CONTENT_NOTE_URI, values, null, null);
                }
            }).start(); // 在新线程中执行

            Toast.makeText(NotesPreferenceActivity.this,
                    getString(R.string.preferences_toast_success_set_accout, account),
                    Toast.LENGTH_SHORT).show(); // 显示账户设置成功的提示
        }
    }

    // 移除同步账户
    private void removeSyncAccount() {
        SharedPreferences settings = getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        if (settings.contains(PREFERENCE_SYNC_ACCOUNT_NAME)) {
            editor.remove(PREFERENCE_SYNC_ACCOUNT_NAME); // 移除同步账户名称
        }
        if (settings.contains(PREFERENCE_LAST_SYNC_TIME)) {
            editor.remove(PREFERENCE_LAST_SYNC_TIME); // 移除最后同步时间
        }
        editor.commit(); // 提交更改

        // 清理本地相关同步信息
        new Thread(new Runnable() {
            public void run() {
                ContentValues values = new ContentValues();
                values.put(NoteColumns.GTASK_ID, ""); // 清空GTASK ID
                values.put(NoteColumns.SYNC_ID, 0); // 重置同步ID
                getContentResolver().update(Notes.CONTENT_NOTE_URI, values, null, null);
            }
        }).start(); // 在新线程中执行
    }

    // 获取当前同步账户名称
    public static String getSyncAccountName(Context context) {
        SharedPreferences settings = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        return settings.getString(PREFERENCE_SYNC_ACCOUNT_NAME, ""); // 返回当前设置的同步账户名称，默认为空
    }


    // 设置最后同步时间
    public static void setLastSyncTime(Context context, long time) {
        SharedPreferences settings = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putLong(PREFERENCE_LAST_SYNC_TIME, time); // 更新最后同步时间
        editor.commit(); // 提交更改
    }

    // 获取最后同步时间
    public static long getLastSyncTime(Context context) {
        SharedPreferences settings = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        return settings.getLong(PREFERENCE_LAST_SYNC_TIME, 0); // 返回最后同步时间，默认为0
    }

    // 广播接收器，用于处理同步服务发送的广播
    private class GTaskReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            refreshUI(); // 刷新界面
            if (intent.getBooleanExtra(GTaskSyncService.GTASK_SERVICE_BROADCAST_IS_SYNCING, false)) {
                TextView syncStatus = (TextView) findViewById(R.id.prefenerece_sync_status_textview);
                syncStatus.setText(intent
                        .getStringExtra(GTaskSyncService.GTASK_SERVICE_BROADCAST_PROGRESS_MSG)); // 更新同步状态信息
            }

        }
    }
    // 处理选项菜单项的选择事件
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:// 如果点击了返回按钮
                Intent intent = new Intent(this, NotesListActivity.class);// 创建跳转到笔记列表活动的意图
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return true;
            default:
                return false;
        }
    }
}
