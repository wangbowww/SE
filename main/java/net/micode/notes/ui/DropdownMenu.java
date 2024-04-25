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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;

import net.micode.notes.R;

// 下拉菜单类
public class DropdownMenu {
    private Button mButton; // 触发下拉菜单的按钮
    private PopupMenu mPopupMenu; // 弹出式菜单
    private Menu mMenu; // 菜单对象

    // 构造函数，初始化下拉菜单
    public DropdownMenu(Context context, Button button, int menuId) {
        mButton = button; // 设置按钮
        mButton.setBackgroundResource(R.drawable.dropdown_icon); // 设置按钮背景为下拉图标
        mPopupMenu = new PopupMenu(context, mButton); // 创建弹出式菜单
        mMenu = mPopupMenu.getMenu(); // 获取菜单
        mPopupMenu.getMenuInflater().inflate(menuId, mMenu); // 根据menuId填充菜单
        mButton.setOnClickListener(new OnClickListener() { // 设置按钮点击监听器
            public void onClick(View v) {
                mPopupMenu.show(); // 显示菜单
            }
        });
    }
    // 设置菜单项点击监听器
    public void setOnDropdownMenuItemClickListener(OnMenuItemClickListener listener) {
        if (mPopupMenu != null) {
            mPopupMenu.setOnMenuItemClickListener(listener);
        }
    }

    // 根据ID查找菜单项
    public MenuItem findItem(int id) {
        return mMenu.findItem(id);
    }

    // 设置标题
    public void setTitle(CharSequence title) {
        mButton.setText(title);
    }
}
