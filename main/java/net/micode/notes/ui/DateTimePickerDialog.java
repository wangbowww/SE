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

import java.util.Calendar;

import net.micode.notes.R;
import net.micode.notes.ui.DateTimePicker;
import net.micode.notes.ui.DateTimePicker.OnDateTimeChangedListener;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.text.format.DateFormat;
import android.text.format.DateUtils;

// 日期时间选择对话框类，扩展自AlertDialog
public class DateTimePickerDialog extends AlertDialog implements OnClickListener {

    private Calendar mDate = Calendar.getInstance();
    private boolean mIs24HourView;
    private OnDateTimeSetListener mOnDateTimeSetListener;
    private DateTimePicker mDateTimePicker;

    // 定义日期时间设置监听器接口
    public interface OnDateTimeSetListener {
        void OnDateTimeSet(AlertDialog dialog, long date);
    }

    // 构造函数，初始化日期时间选择对话框
    public DateTimePickerDialog(Context context, long date) {
        super(context);
        mDateTimePicker = new DateTimePicker(context); // 创建日期时间选择器
        setView(mDateTimePicker); // 将日期时间选择器设置为对话框视图
        mDateTimePicker.setOnDateTimeChangedListener(new OnDateTimeChangedListener() {
            public void onDateTimeChanged(DateTimePicker view, int year, int month,
                    int dayOfMonth, int hourOfDay, int minute) {
                // 当日期时间发生变化时，更新Calendar对象
                mDate.set(Calendar.YEAR, year);
                mDate.set(Calendar.MONTH, month);
                mDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                mDate.set(Calendar.HOUR_OF_DAY, hourOfDay);
                mDate.set(Calendar.MINUTE, minute);
                updateTitle(mDate.getTimeInMillis()); // 更新对话框标题以显示新的日期时间
            }
        });
        mDate.setTimeInMillis(date); // 使用给定的日期时间初始化Calendar对象
        mDate.set(Calendar.SECOND, 0); // 秒数设为0
        mDateTimePicker.setCurrentDate(mDate.getTimeInMillis()); // 在日期时间选择器上显示当前日期时间
        setButton(context.getString(R.string.datetime_dialog_ok), this); // 设置“确定”按钮
        setButton2(context.getString(R.string.datetime_dialog_cancel), (OnClickListener)null); // 设置“取消”按钮
        set24HourView(DateFormat.is24HourFormat(this.getContext())); // 根据设备设置决定是否使用24小时制
        updateTitle(mDate.getTimeInMillis()); // 更新对话框标题
    }

    // 设置是否使用24小时制的方法
    public void set24HourView(boolean is24HourView) {
        mIs24HourView = is24HourView;
    }

    // 设置日期时间设置监听器的方法
    public void setOnDateTimeSetListener(OnDateTimeSetListener callBack) {
        mOnDateTimeSetListener = callBack;
    }

    // 更新对话框标题的方法，显示日期时间
    private void updateTitle(long date) {
        int flag =
            DateUtils.FORMAT_SHOW_YEAR | // 显示年份
            DateUtils.FORMAT_SHOW_DATE | // 显示日期
            DateUtils.FORMAT_SHOW_TIME; // 显示时间
        flag |= mIs24HourView ? DateUtils.FORMAT_24HOUR : DateUtils.FORMAT_24HOUR;
        setTitle(DateUtils.formatDateTime(this.getContext(), date, flag));
    }

    // “确定”或“取消”按钮点击时的回调方法
    public void onClick(DialogInterface arg0, int arg1) {
        if (mOnDateTimeSetListener != null) { // 如果设置了监听器，调用其OnDateTimeSet方法
            mOnDateTimeSetListener.OnDateTimeSet(this, mDate.getTimeInMillis());
        }
    }

}