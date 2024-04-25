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

import android.content.Context;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.micode.notes.R;
import net.micode.notes.data.Notes;
import net.micode.notes.tool.DataUtils;
import net.micode.notes.tool.ResourceParser.NoteItemBgResources;

// 笔记列表中的单个笔记项视图
public class NotesListItem extends LinearLayout {
    private ImageView mAlert;  // 提醒图标
    private TextView mTitle;  // 标题文本
    private TextView mTime;  // 显示修改时间的文本
    private TextView mCallName;  // 电话记录名称文本
    private NoteItemData mItemData;  // 笔记项的数据模型
    private CheckBox mCheckBox;  // 选项框

    // 构造函数，初始化视图和组件
    public NotesListItem(Context context) {
        super(context);
        inflate(context, R.layout.note_item, this);  // 加载布局
        mAlert = (ImageView) findViewById(R.id.iv_alert_icon);  // 初始化提醒图标
        mTitle = (TextView) findViewById(R.id.tv_title);  // 初始化标题文本
        mTime = (TextView) findViewById(R.id.tv_time);  // 初始化时间文本
        mCallName = (TextView) findViewById(R.id.tv_name);  // 初始化电话记录名称文本
        mCheckBox = (CheckBox) findViewById(android.R.id.checkbox);  // 初始化选项框
    }
    // 绑定数据到视图，设置视图的显示
    public void bind(Context context, NoteItemData data, boolean choiceMode, boolean checked) {
        if (choiceMode && data.getType() == Notes.TYPE_NOTE) {  // 如果处于选择模式并且是笔记类型
            mCheckBox.setVisibility(View.VISIBLE);  // 显示复选框
            mCheckBox.setChecked(checked);  // 设置复选框的选中状态
        } else {
            mCheckBox.setVisibility(View.GONE);  // 不显示复选框
        }

 mItemData = data;  // 保存笔记数据
        if (data.getId() == Notes.ID_CALL_RECORD_FOLDER) {  // 如果是电话记录文件夹
            mCallName.setVisibility(View.GONE);  // 隐藏电话名称
            mAlert.setVisibility(View.VISIBLE);  // 显示提醒图标
            mTitle.setTextAppearance(context, R.style.TextAppearancePrimaryItem);  // 设置标题样式
            mTitle.setText(context.getString(R.string.call_record_folder_name)
                    + context.getString(R.string.format_folder_files_count, data.getNotesCount()));  // 设置标题
            mAlert.setImageResource(R.drawable.call_record);  // 设置提醒图标为电话记录图标
        } else if (data.getParentId() == Notes.ID_CALL_RECORD_FOLDER) {  // 如果属于电话记录文件夹
            mCallName.setVisibility(View.VISIBLE);  // 显示电话名称
            mCallName.setText(data.getCallName());  // 设置电话名称
            mTitle.setTextAppearance(context, R.style.TextAppearanceSecondaryItem);  // 设置标题样式
            mTitle.setText(DataUtils.getFormattedSnippet(data.getSnippet()));  // 设置格式化的摘要作为标题
            if (data.hasAlert()) {  // 如果设置了提醒
                mAlert.setImageResource(R.drawable.clock);  // 设置提醒图标为时钟
                mAlert.setVisibility(View.VISIBLE);  // 显示提醒图标
            } else {
                mAlert.setVisibility(View.GONE);  // 隐藏提醒图标
            }
        } else {  // 如果是其他
            mCallName.setVisibility(View.GONE);  // 隐藏电话名称
            mTitle.setTextAppearance(context, R.style.TextAppearancePrimaryItem);  // 设置标题的样式

            if (data.getType() == Notes.TYPE_FOLDER) {  // 如果数据类型是文件夹
                mTitle.setText(data.getSnippet()
                        + context.getString(R.string.format_folder_files_count,
                                data.getNotesCount()));  // 设置标题显示文件夹名称及包含的笔记数量
                mAlert.setVisibility(View.GONE);  // 隐藏提醒图标
            } else {  // 如果是笔记类型
                mTitle.setText(DataUtils.getFormattedSnippet(data.getSnippet()));  // 设置格式化后的笔记摘要为标题
                if (data.hasAlert()) {  // 如果笔记设置了提醒
                    mAlert.setImageResource(R.drawable.clock);  // 设置提醒图标为时钟
                    mAlert.setVisibility(View.VISIBLE);  // 显示提醒图标
                } else {
                    mAlert.setVisibility(View.GONE);  // 否则隐藏提醒图标
                }
            }
        }
        mTime.setText(DateUtils.getRelativeTimeSpanString(data.getModifiedDate()));

        setBackground(data); // 根据笔记类型和状态设置背景
    }
// 根据笔记的类型和状态设置背景
    private void setBackground(NoteItemData data) {
        int id = data.getBgColorId();  // 获取背景颜色ID
        if (data.getType() == Notes.TYPE_NOTE) {  // 如果是笔记类型
            if (data.isSingle() || data.isOneFollowingFolder()) {  // 如果是单独的笔记或紧随文件夹后的单笔记
                setBackgroundResource(NoteItemBgResources.getNoteBgSingleRes(id));  // 设置为单笔记背景
            } else if (data.isLast()) {  // 如果是最后一条笔记
                setBackgroundResource(NoteItemBgResources.getNoteBgLastRes(id));  // 设置为最后一条笔记的背景
            } else if (data.isFirst() || data.isMultiFollowingFolder()) {  // 如果是第一条或多条笔记后的第一条
                setBackgroundResource(NoteItemBgResources.getNoteBgFirstRes(id));  // 设置为第一条笔记的背景
            } else {
                setBackgroundResource(NoteItemBgResources.getNoteBgNormalRes(id));  // 设置为普通笔记背景
            }
        } else {
            setBackgroundResource(NoteItemBgResources.getFolderBgRes());  // 如果是文件夹类型，则设置文件夹背景
        }
    }

    // 获取当前项的笔记数据
    public NoteItemData getItemData() {
        return mItemData;
    }
}
