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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**AlarmReceiver 是一个在接收到特定的闹钟广播时被触发的组件。
 * 它的工作是启动 AlarmAlertActivity，后者负责显示闹钟提醒并让用户进行相应的操作
 * （如关闭闹钟或打开笔记）。
 * 这种设计允许应用即使在后台运行或设备休眠时也能唤醒设备并提醒用户。
 * 
 */


public class AlarmReceiver extends BroadcastReceiver {
    // 当接收器接收到一个广播时，会调用这个方法
    @Override
    public void onReceive(Context context, Intent intent) {
         // 将接收到的 Intent 的类设置为 AlarmAlertActivity，这样 Intent 现在指向 AlarmAlertActivity
        intent.setClass(context, AlarmAlertActivity.class);
        
        // 为 Intent 添加一个标志，这个标志的作用是如果接收到的广播启动了一个活动，而当前没有任何活动在运行，
        // 系统会创建一个新的任务栈并在这个栈中启动这个活动
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        
        // 使用提供的上下文启动 AlarmAlertActivity 活动。由于这个接收器可能是在应用程序的任何其他组件之外接收到广播的，
        // 所以必须为这个 Intent 添加 FLAG_ACTIVITY_NEW_TASK 标志，以确保活动可以正确启动
        context.startActivity(intent);
    }
}
