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

import android.app.Activity; // Android应用程序中的活动类，用于用户界面交互和操作
import android.app.Service; // Android应用程序中的服务类，用于在后台执行长时间运行的操作
import android.content.Context; // Android应用程序中的上下文类，提供应用程序全局信息的接口
import android.content.Intent; // Android应用程序中的意图类，用于在组件之间传递消息和操作
import android.os.Bundle; // Android应用程序中的Bundle类，用于在Intent中传递数据
import android.os.IBinder; // Android应用程序中的IBinder接口，用于定义服务的绑定接口

// GTaskSyncService类是用于处理与Google任务同步相关的服务
public class GTaskSyncService extends Service {
    //同步操作类型的Intent Extra名称
    public final static String ACTION_STRING_NAME = "sync_action_type";

    //启动同步操作的标识
    public final static int ACTION_START_SYNC = 0;

    //取消同步操作的标识
    public final static int ACTION_CANCEL_SYNC = 1;

    //无效操作的标识
    public final static int ACTION_INVALID = 2;

    //同步服务广播的名称
    public final static String GTASK_SERVICE_BROADCAST_NAME = "net.micode.notes.gtask.remote.gtask_sync_service";

    //广播中指示是否正在同步的键名
    public final static String GTASK_SERVICE_BROADCAST_IS_SYNCING = "isSyncing";

    //广播中同步进度消息的键名
    public final static String GTASK_SERVICE_BROADCAST_PROGRESS_MSG = "progressMsg";

    //当前同步任务的静态变量
    private static GTaskASyncTask mSyncTask = null;

    //当前同步进度消息的静态变量
    private static String mSyncProgress = "";


    //启动同步任务
    //如果当前没有同步任务在运行，则创建新的异步任务并执行同步操作
    private void startSync() {
        if (mSyncTask == null) {
            // 创建新的异步任务
            mSyncTask = new GTaskASyncTask(this, new GTaskASyncTask.OnCompleteListener() {
                public void onComplete() {
                    // 同步任务完成后，重置mSyncTask、发送广播并停止服务
                    mSyncTask = null;
                    sendBroadcast("");
                    stopSelf();
                }
            });
            // 发送同步广播
            sendBroadcast("");
            // 执行异步任务
            mSyncTask.execute();
        }
    }

    //取消同步任务
    //如果有同步任务在运行，则取消该任务
    private void cancelSync() {
        if (mSyncTask != null) {
            //取消当前正在运行的同步任务
            mSyncTask.cancelSync();
        }
    }

    @Override
    public void onCreate() {
        // 在服务创建时，初始化mSyncTask为null
        mSyncTask = null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) { // 在服务启动时的处理

        Bundle bundle = intent.getExtras();
        if (bundle != null && bundle.containsKey(ACTION_STRING_NAME)) {
            // 检查Intent中是否包含同步操作类型
            switch (bundle.getInt(ACTION_STRING_NAME, ACTION_INVALID)) {
                case ACTION_START_SYNC: // 如果是启动同步操作，则调用startSync方法
                    startSync();
                    break;
                case ACTION_CANCEL_SYNC: // 如果是取消同步操作，则调用cancelSync方法
                    cancelSync();
                    break;
                default:
                    break;
            }
            return START_STICKY;    // 指定服务被杀死后自动重启
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onLowMemory() { // 在系统内存低时的处理
        if (mSyncTask != null) {
            mSyncTask.cancelSync();// 如果有同步任务在运行，取消该任务
        }
    }

    public IBinder onBind(Intent intent) {
        return null;
    }

    // 发送广播
    public void sendBroadcast(String msg) {
        // 设置同步进度消息
        mSyncProgress = msg;
        // 创建新的广播Intent
        Intent intent = new Intent(GTASK_SERVICE_BROADCAST_NAME);
        // 将是否正在同步的信息放入Intent
        intent.putExtra(GTASK_SERVICE_BROADCAST_IS_SYNCING, mSyncTask != null);
        // 将同步进度消息放入Intent
        intent.putExtra(GTASK_SERVICE_BROADCAST_PROGRESS_MSG, msg);
        // 发送广播
        sendBroadcast(intent);
    }

    // 启动同步
    public static void startSync(Activity activity) {
        // 设置Activity上下文
        GTaskManager.getInstance().setActivityContext(activity);
        // 创建新的Intent用于启动同步服务
        Intent intent = new Intent(activity, GTaskSyncService.class);
        // 设置同步操作类型为启动同步
        intent.putExtra(GTaskSyncService.ACTION_STRING_NAME, GTaskSyncService.ACTION_START_SYNC);
        // 启动服务
        activity.startService(intent);
    }

    // 取消同步
    public static void cancelSync(Context context) {
        // 创建新的Intent用于取消同步服务
        Intent intent = new Intent(context, GTaskSyncService.class);
        // 设置同步操作类型为取消同步
        intent.putExtra(GTaskSyncService.ACTION_STRING_NAME, GTaskSyncService.ACTION_CANCEL_SYNC);
        // 启动服务
        context.startService(intent);
    }

    // 检查是否正在同步
    public static boolean isSyncing() {
        return mSyncTask != null;
    }

    // 获取同步进度信息
    public static String getProgressString() {
        return mSyncProgress;
    }
}
