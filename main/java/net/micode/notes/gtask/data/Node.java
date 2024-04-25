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

package net.micode.notes.gtask.data;

import android.database.Cursor;

import org.json.JSONObject;

/*
 * 抽象类  Node 表示一个具有同步功能的数据节点
 * 此类定义了同步动作的枚举常量、节点的基本属性（如Gid、名称、最后修改时间、删除状态）以及与远程和本地数据交互的方法
 * 具体的Node子类需要实现与同步相关的抽象方法，
 * 如创建、更新动作的JSON表示，以及根据远程和本地JSON数据设置内容、从内容生成本地JSON数据以及确定同步动作等
 */
public abstract class Node {

    //同步操作：无动作
    public static final int SYNC_ACTION_NONE = 0;

    //同步操作：向远程添加新节点
    public static final int SYNC_ACTION_ADD_REMOTE = 1;

    //同步操作：向本地添加新节点
    public static final int SYNC_ACTION_ADD_LOCAL = 2;

    //同步操作：从远程删除节点
    public static final int SYNC_ACTION_DEL_REMOTE = 3;

    //同步操作：从本地删除节点
    public static final int SYNC_ACTION_DEL_LOCAL = 4;

    //同步操作：更新远程节点
    public static final int SYNC_ACTION_UPDATE_REMOTE = 5;

    //同步操作：更新本地节点
    public static final int SYNC_ACTION_UPDATE_LOCAL = 6;

    //同步操作：存在更新冲突
    public static final int SYNC_ACTION_UPDATE_CONFLICT = 7;

    //同步操作：出现错误
    public static final int SYNC_ACTION_ERROR = 8;

    //节点全局唯一标识符（Gid）
    private String mGid;

    //节点名称
    private String mName;

    // 最后一次修改时间戳
    private long mLastModified;

    //节点是否已被标记为删除
    private boolean mDeleted;

    //构造函数，初始化节点属性。Gid设为null，名称设为空字符串，最后修改时间设为0，删除状态设为false。
    public Node() {
        mGid = null;
        mName = "";
        mLastModified = 0;
        mDeleted = false;
    }

    //抽象方法，由子类实现。根据指定的动作ID（如ADD_REMOTE）生成对应创建动作的JSON对象。
    public abstract JSONObject getCreateAction(int actionId);

    //抽象方法，由子类实现。根据指定的动作ID（如UPDATE_REMOTE）生成对应更新动作的JSON对象。
    public abstract JSONObject getUpdateAction(int actionId);

    //抽象方法，由子类实现。根据远程JSON数据设置节点的内容。
    public abstract void setContentByRemoteJSON(JSONObject js);

    //抽象方法，由子类实现。根据本地JSON数据设置节点的内容。
    public abstract void setContentByLocalJSON(JSONObject js);

    //抽象方法，由子类实现。根据节点当前内容生成本地JSON表示。
    public abstract JSONObject getLocalJSONFromContent();

    //抽象方法，由子类实现。根据传入的Cursor对象确定当前节点应执行的同步动作。

    public abstract int getSyncAction(Cursor c);

    //设置节点全局唯一标识符（Gid）
    public void setGid(String gid) {
        this.mGid = gid;
    }

    //设置节点名称
    public void setName(String name) {
        this.mName = name;
    }

    //设置节点最后一次修改时间
    public void setLastModified(long lastModified) {
        this.mLastModified = lastModified;
    }

    //设置节点删除状态
    public void setDeleted(boolean deleted) {
        this.mDeleted = deleted;
    }

    //获取节点全局唯一标识符（Gid）
    public String getGid() {
        return this.mGid;
    }

    //获取节点名称
    public String getName() {
        return this.mName;
    }

    //获取节点最后一次修改时间
    public long getLastModified() {
        return this.mLastModified;
    }

    //获取节点删除状态
    public boolean getDeleted() {
        return this.mDeleted;
    }

}