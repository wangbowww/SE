
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

package net.micode.notes.gtask.remote;

// Android应用程序框架的通知类
import android.app.Notification;
// Android应用程序框架的通知管理器类
import android.app.NotificationManager;
// Android应用程序框架的挂起意图类
import android.app.PendingIntent;
// Android应用程序框架的上下文类
import android.content.Context;
// Android应用程序框架的意图类
import android.content.Intent;
// Android应用程序框架的异步任务类
import android.os.AsyncTask;

// 小米便签应用程序的资源类
import net.micode.notes.R;
// 小米便签应用程序的便签列表活动类
import net.micode.notes.ui.NotesListActivity;
// 小米便签应用程序的便签首选项活动类
import net.micode.notes.ui.NotesPreferenceActivity;


// GTaskASyncTask类是一个异步任务类，用于处理与Google任务同步相关的操作
public class GTaskASyncTask extends AsyncTask<Void, String, Integer> {
    // Google任务同步通知的ID
    private static int GTASK_SYNC_NOTIFICATION_ID = 5234235;

    //定义OnCompleteListener接口，用于在任务完成时回调通知
    public interface OnCompleteListener {
        void onComplete();
    }

    // 上下文对象
    private Context mContext;
    // 通知管理器对象
    private NotificationManager mNotifiManager;
    // Google任务管理器对象
    private GTaskManager mTaskManager;
    // 任务完成监听器对象
    private OnCompleteListener mOnCompleteListener;

    //构造函数，初始化相关变量
    public GTaskASyncTask(Context context, OnCompleteListener listener) {
        mContext = context;
        mOnCompleteListener = listener;
        mNotifiManager = (NotificationManager) mContext
                .getSystemService(Context.NOTIFICATION_SERVICE);
        mTaskManager = GTaskManager.getInstance();
    }

    //取消同步操作
    public void cancelSync() {
        mTaskManager.cancelSync();
    }

    //发布同步进度信息
    public void publishProgess(String message) {
        publishProgress(new String[] {
            message
        });
    }

    //显示通知
    private void showNotification(int tickerId, String content) {
        // 创建通知对象
        Notification notification = new Notification(R.drawable.notification, mContext
                .getString(tickerId), System.currentTimeMillis());
        // 设置通知默认灯光
        notification.defaults = Notification.DEFAULT_LIGHTS;
        // 设置通知标志为自动取消
        notification.flags = Notification.FLAG_AUTO_CANCEL;
        PendingIntent pendingIntent;
        // 根据不同的tickerId选择不同的意图
        if (tickerId != R.string.ticker_success) {
            pendingIntent = PendingIntent.getActivity(mContext, 0, new Intent(mContext,
                    NotesPreferenceActivity.class), 0);

        } else {
            pendingIntent = PendingIntent.getActivity(mContext, 0, new Intent(mContext,
                    NotesListActivity.class), 0);
        }
        /**notification.setLatestEventInfo(mContext, mContext.getString(R.string.app_name), content,
                pendingIntent);
        mNotifiManager.notify(GTASK_SYNC_NOTIFICATION_ID, notification);**/
    }

    @Override
    protected Integer doInBackground(Void... unused) {
        // 发布同步进度信息
        publishProgess(mContext.getString(R.string.sync_progress_login, NotesPreferenceActivity
                .getSyncAccountName(mContext)));
        // 执行同步操作并返回结果
        return mTaskManager.sync(mContext, this);
    }

    @Override
    protected void onProgressUpdate(String... progress) {
        // 更新通知显示同步进度
        showNotification(R.string.ticker_syncing, progress[0]);
        // 如果上下文是GTaskSyncService的实例，则发送广播
        if (mContext instanceof GTaskSyncService) {
            ((GTaskSyncService) mContext).sendBroadcast(progress[0]);
        }
    }

    @Override
    protected void onPostExecute(Integer result) {
        // 根据同步结果展示不同的通知
        if (result == GTaskManager.STATE_SUCCESS) {
            showNotification(R.string.ticker_success, mContext.getString(
                    R.string.success_sync_account, mTaskManager.getSyncAccount()));
            // 设置最后同步时间
            NotesPreferenceActivity.setLastSyncTime(mContext, System.currentTimeMillis());
        } else if (result == GTaskManager.STATE_NETWORK_ERROR) {
            showNotification(R.string.ticker_fail, mContext.getString(R.string.error_sync_network));
        } else if (result == GTaskManager.STATE_INTERNAL_ERROR) {
            showNotification(R.string.ticker_fail, mContext.getString(R.string.error_sync_internal));
        } else if (result == GTaskManager.STATE_SYNC_CANCELLED) {
            showNotification(R.string.ticker_cancel, mContext
                    .getString(R.string.error_sync_cancelled));
        }
        // 如果OnCompleteListener不为空，则在新线程中执行完成操作
        if (mOnCompleteListener != null) {
            new Thread(new Runnable() {

                public void run() {
                    mOnCompleteListener.onComplete();
                }
            }).start();
        }
    }
}
