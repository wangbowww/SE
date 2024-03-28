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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;

import net.micode.notes.data.Notes;
import net.micode.notes.data.Notes.NoteColumns;

/**这个 AlarmInitReceiver 主要在设备启动后通过接收特定的广播
 * （通常是在系统启动完成后）或者应用特定的逻辑触发时，
 * 遍历数据库中所有未来需要提醒的笔记，并为每个笔记设置一个闹钟。
 * 这样可以确保即使设备重启，之前设置的笔记提醒也不会丢失。
 * 
 */


// 定义 AlarmInitReceiver 类，继承自 BroadcastReceiver，用于初始化闹钟
public class AlarmInitReceiver extends BroadcastReceiver {

    // 定义查询数据库时需要的列
    private static final String [] PROJECTION = new String [] {
        NoteColumns.ID,// 笔记ID
        NoteColumns.ALERTED_DATE// 预定的闹钟提醒时间
    };

    // 定义列索引常量
    private static final int COLUMN_ID                = 0;// 笔记ID列的索引
    private static final int COLUMN_ALERTED_DATE      = 1;// 预定的闹钟提醒时间列的索引

    // 当接收到广播时，执行此方法
    @Override
    public void onReceive(Context context, Intent intent) {
        long currentDate = System.currentTimeMillis(); // 获取当前时间
        // 查询数据库，找出所有设定的提醒时间晚于当前时间的笔记
        Cursor c = context.getContentResolver().query(Notes.CONTENT_NOTE_URI,
                PROJECTION,
                NoteColumns.ALERTED_DATE + ">? AND " + NoteColumns.TYPE + "=" + Notes.TYPE_NOTE,
                new String[] { String.valueOf(currentDate) },
                null);
        // 如果查询结果不为空
        if (c != null) {
            // 如果至少有一条记录
            if (c.moveToFirst()) {
                do {
                     // 获取提醒时间
                    long alertDate = c.getLong(COLUMN_ALERTED_DATE);
                    // 创建一个指向 AlarmReceiver 的 Intent
                    Intent sender = new Intent(context, AlarmReceiver.class);
                    // 设置数据URI，以便 AlarmReceiver 可以知道是哪个笔记的提醒
                    sender.setData(ContentUris.withAppendedId(Notes.CONTENT_NOTE_URI, c.getLong(COLUMN_ID)));
                    // 创建一个等待的Intent，当闹钟触发时发送
                    PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, sender, 0);
                    // 获取 AlarmManager 服务
                    AlarmManager alermManager = (AlarmManager) context
                            .getSystemService(Context.ALARM_SERVICE);
                    // 设置闹钟，在预定的提醒时间唤醒设备并发送广播
                    alermManager.set(AlarmManager.RTC_WAKEUP, alertDate, pendingIntent);
                } while (c.moveToNext()); // 遍历所有找到的记录
            }
            c.close();// 关闭游标
        }
    }
}
