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
import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.micode.notes.R;
import net.micode.notes.data.Notes;
import net.micode.notes.data.Notes.NoteColumns;


// 文件夹列表适配器类，用于在列表视图中显示文件夹
public class FoldersListAdapter extends CursorAdapter {
    // 查询结果的列
    public static final String [] PROJECTION = {
        NoteColumns.ID,
        NoteColumns.SNIPPET
    };

    // 列索引
    public static final int ID_COLUMN   = 0; // ID列
    public static final int NAME_COLUMN = 1; // 名称列

    // 构造函数，初始化适配器
    public FoldersListAdapter(Context context, Cursor c) {
        super(context, c);
        // TODO Auto-generated constructor stub
    }

    @Override
    // 创建新的视图用于显示列表项
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return new FolderListItem(context); // 返回一个新的文件夹列表项视图
    }

    @Override
    // 将数据绑定到视图上
    public void bindView(View view, Context context, Cursor cursor) {
        if (view instanceof FolderListItem) { // 确保视图是FolderListItem的实例
            // 根据ID判断是否是根文件夹，并获取文件夹名称
            String folderName = (cursor.getLong(ID_COLUMN) == Notes.ID_ROOT_FOLDER) ? context
                    .getString(R.string.menu_move_parent_folder) : cursor.getString(NAME_COLUMN);
            ((FolderListItem) view).bind(folderName); // 绑定文件夹名称到视图
        }
    }

    // 根据位置获取文件夹名称
    public String getFolderName(Context context, int position) {
        Cursor cursor = (Cursor) getItem(position); // 获取当前位置的Cursor对象
        // 根据ID判断是否是根文件夹，并返回文件夹名称
        return (cursor.getLong(ID_COLUMN) == Notes.ID_ROOT_FOLDER) ? context
                .getString(R.string.menu_move_parent_folder) : cursor.getString(NAME_COLUMN);
    }

    // 文件夹列表项类，继承自LinearLayout
    private class FolderListItem extends LinearLayout {
        private TextView mName; // 显示文件夹名称的TextView

        // 构造函数，初始化文件夹列表项
        public FolderListItem(Context context) {
            super(context);
            inflate(context, R.layout.folder_list_item, this); // 加载布局
            mName = (TextView) findViewById(R.id.tv_folder_name); // 获取显示名称的TextView
        }

        // 绑定文件夹名称到视图
        public void bind(String name) {
            mName.setText(name); // 设置文件夹名称
        }
    }

}
