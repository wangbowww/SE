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

import android.app.Activity; // Android 应用程序组件中的 Activity 类，用于创建应用的用户界面
import android.content.ContentResolver; // 用于访问应用程序的内容提供者，执行数据操作
import android.content.ContentUris; // 用于处理内容 URI 和其它相关操作
import android.content.ContentValues; // 用于存储一组键值对，用于数据库的插入、更新等操作
import android.content.Context; // 提供应用程序的全局信息或操作应用级别的资源
import android.database.Cursor; // 用于查询数据库并获取结果集
import android.util.Log; // 用于记录调试信息和日志

import net.micode.notes.R; // 导入应用程序中的 R 类，用于访问资源文件中的资源
import net.micode.notes.data.Notes; // 导入应用程序中的 Notes 类，用于处理笔记相关的数据
import net.micode.notes.data.Notes.DataColumns; // 导入 Notes 类中的 DataColumns 类，包含笔记数据的列名
import net.micode.notes.data.Notes.NoteColumns; // 导入 Notes 类中的 NoteColumns 类，包含笔记的列名
import net.micode.notes.gtask.data.MetaData; // 导入 GTask 中的 MetaData 类，用于处理元数据
import net.micode.notes.gtask.data.Node; // 导入 GTask 中的 Node 类，表示 GTask 中的节点
import net.micode.notes.gtask.data.SqlNote; // 导入 GTask 中的 SqlNote 类，表示 GTask 中的笔记
import net.micode.notes.gtask.data.Task; // 导入 GTask 中的 Task 类，表示 GTask 中的任务
import net.micode.notes.gtask.data.TaskList; // 导入 GTask 中的 TaskList 类，表示 GTask 中的任务列表
import net.micode.notes.gtask.exception.ActionFailureException; // 导入 GTask 中的 ActionFailureException 类，表示动作失败异常
import net.micode.notes.gtask.exception.NetworkFailureException; // 导入 GTask 中的 NetworkFailureException 类，表示网络失败异常
import net.micode.notes.tool.DataUtils; // 导入工具类 DataUtils，用于处理数据相关的工具方法
import net.micode.notes.tool.GTaskStringUtils; // 导入工具类 GTaskStringUtils，用于处理 GTask 字符串相关的工具方法

import org.json.JSONArray; // 用于处理 JSON 数据的数组
import org.json.JSONException; // 用于处理 JSON 数据的异常
import org.json.JSONObject; // 用于处理 JSON 数据的对象

import java.util.HashMap; // 用于存储键值对的数据结构，实现快速查找
import java.util.HashSet; // 用于存储不重复元素的集合
import java.util.Iterator; // 用于迭代集合中的元素
import java.util.Map; // 用于表示键值对的接口

// GTaskManager主要负责管理与Google任务同步相关的操作
public class GTaskManager {
    private static final String TAG = GTaskManager.class.getSimpleName(); // 定义一个静态常量 TAG，用于在日志中标识 GTaskManager 类的简单名称

    public static final int STATE_SUCCESS = 0; // 表示同步成功的状态常量
    public static final int STATE_NETWORK_ERROR = 1; // 表示网络错误的状态常量
    public static final int STATE_INTERNAL_ERROR = 2; // 表示内部错误的状态常量
    public static final int STATE_SYNC_IN_PROGRESS = 3; // 表示同步正在进行中的状态常量
    public static final int STATE_SYNC_CANCELLED = 4; // 表示同步已取消的状态常量

    private static GTaskManager mInstance = null; // 单例模式中的实例对象

    private Activity mActivity; // 当前活动的 Activity 对象
    private Context mContext; // 上下文对象
    private ContentResolver mContentResolver; // 内容解析器，用于访问应用程序的内容提供者

    private boolean mSyncing; // 标识是否正在进行同步操作
    private boolean mCancelled; // 标识同步是否已被取消

    private HashMap<String, TaskList> mGTaskListHashMap; // 用于存储任务列表的哈希映射
    private HashMap<String, Node> mGTaskHashMap; // 用于存储任务的哈希映射
    private HashMap<String, MetaData> mMetaHashMap; // 用于存储元数据的哈希映射
    private TaskList mMetaList; // 元数据列表对象

    private HashSet<Long> mLocalDeleteIdMap; // 用于存储本地删除的 ID 集合
    private HashMap<String, Long> mGidToNid; // 用于存储 Google 任务 ID 到节点 ID 的映射
    private HashMap<Long, String> mNidToGid; // 用于存储节点 ID 到 Google 任务 ID 的映射

    // GTaskManager 类的私有构造函数
    private GTaskManager() {
        mSyncing = false; // 初始化同步状态为 false
        mCancelled = false; // 初始化取消状态为 false
        mGTaskListHashMap = new HashMap<String, TaskList>(); // 初始化任务列表的哈希映射
        mGTaskHashMap = new HashMap<String, Node>(); // 初始化任务的哈希映射
        mMetaHashMap = new HashMap<String, MetaData>(); // 初始化元数据的哈希映射
        mMetaList = null; // 初始化元数据列表为 null
        mLocalDeleteIdMap = new HashSet<Long>(); // 初始化本地删除 ID 的集合
        mGidToNid = new HashMap<String, Long>(); // 初始化 Google 任务 ID 到节点 ID 的映射
        mNidToGid = new HashMap<Long, String>(); // 初始化节点 ID 到 Google 任务 ID 的映射
    }

    // 用于获取 GTaskManager 单例实例
    public static synchronized GTaskManager getInstance() {
        if (mInstance == null) { // 如果实例为空
            mInstance = new GTaskManager(); // 创建新的 GTaskManager 实例
        }
        return mInstance; // 返回 GTaskManager 实例
    }

    // 用于设置活动的上下文
    public synchronized void setActivityContext(Activity activity) {
        // used for getting authtoken
        mActivity = activity;// 设置活动上下文
    }

    /*用于同步任务
    首先检查是否正在进行同步
    然后设置上下文、清空各种哈希映射、登录 Google 任务、获取任务列表、以及执行内容同步工作
    在异常处理部分，捕获可能出现的异常并进行相应处理，最后清空各种哈希映射并将同步标志设为 false
    最终根据是否取消同步来返回相应的状态。*/
    public int sync(Context context, GTaskASyncTask asyncTask) {
        if (mSyncing) { // 如果正在同步中
            Log.d(TAG, "Sync is in progress"); // 记录日志，同步正在进行中
            return STATE_SYNC_IN_PROGRESS; // 返回同步进行中状态
        }
        mContext = context; // 设置上下文
        mContentResolver = mContext.getContentResolver(); // 获取内容解析器
        mSyncing = true; // 设置同步标志为true
        mCancelled = false; // 取消标志为false
        mGTaskListHashMap.clear(); // 清空任务列表哈希映射
        mGTaskHashMap.clear(); // 清空任务哈希映射
        mMetaHashMap.clear(); // 清空元数据哈希映射
        mLocalDeleteIdMap.clear(); // 清空本地删除ID映射
        mGidToNid.clear(); // 清空GID到NID映射
        mNidToGid.clear(); // 清空NID到GID映射

        try {
            GTaskClient client = GTaskClient.getInstance(); // 获取GTaskClient实例
            client.resetUpdateArray(); // 重置更新数组

            // login google task
            if (!mCancelled) { // 如果未取消
                if (!client.login(mActivity)) { // 如果登录失败
                    throw new NetworkFailureException("login google task failed"); // 抛出网络失败异常
                }
            }

            // get the task list from google
            asyncTask.publishProgess(mContext.getString(R.string.sync_progress_init_list)); // 发布进度信息
            initGTaskList(); // 初始化任务列表

            // do content sync work
            asyncTask.publishProgess(mContext.getString(R.string.sync_progress_syncing)); // 发布进度信息
            syncContent(); // 同步内容
        } catch (NetworkFailureException e) {
            Log.e(TAG, e.toString()); // 记录网络失败异常
            return STATE_NETWORK_ERROR; // 返回网络错误状态
        } catch (ActionFailureException e) {
            Log.e(TAG, e.toString()); // 记录动作失败异常
            return STATE_INTERNAL_ERROR; // 返回内部错误状态
        } catch (Exception e) {
            Log.e(TAG, e.toString()); // 记录异常
            e.printStackTrace(); // 打印堆栈跟踪
            return STATE_INTERNAL_ERROR; // 返回内部错误状态
        } finally {
            mGTaskListHashMap.clear(); // 清空任务列表哈希映射
            mGTaskHashMap.clear(); // 清空任务哈希映射
            mMetaHashMap.clear(); // 清空元数据哈希映射
            mLocalDeleteIdMap.clear(); // 清空本地删除ID映射
            mGidToNid.clear(); // 清空GID到NID映射
            mNidToGid.clear(); // 清空NID到GID映射
            mSyncing = false; // 设置同步标志为false
        }

        return mCancelled ? STATE_SYNC_CANCELLED : STATE_SUCCESS; // 如果取消了返回取消状态，否则返回成功状态
    }

    /*
    * 用于初始化任务列表
    * 首先根据条件判断是否取消操作
    * 然后获取任务列表数组
    * 并根据特定逻辑初始化元数据列表和任务列表
    * 在遍历任务列表和元数据数组的过程中，根据不同条件创建任务对象、元数据对象，并将它们添加到相应的列表中
    * 在异常处理部分，捕获可能出现的 JSON 异常并抛出相应的动作失败异常。
    * */
    private void initGTaskList() throws NetworkFailureException {
        if (mCancelled) // 如果已取消，直接返回
            return;
        GTaskClient client = GTaskClient.getInstance(); // 获取GTaskClient实例
        try {
            JSONArray jsTaskLists = client.getTaskLists(); // 获取任务列表数组

            // init meta list first
            mMetaList = null; // 初始化元数据列表为null
            for (int i = 0; i < jsTaskLists.length(); i++) { // 遍历任务列表数组
                JSONObject object = jsTaskLists.getJSONObject(i); // 获取当前任务列表对象
                String gid = object.getString(GTaskStringUtils.GTASK_JSON_ID); // 获取任务列表ID
                String name = object.getString(GTaskStringUtils.GTASK_JSON_NAME); // 获取任务列表名称

                if (name.equals(GTaskStringUtils.MIUI_FOLDER_PREFFIX + GTaskStringUtils.FOLDER_META)) { // 如果是元数据文件夹
                    mMetaList = new TaskList(); // 创建新的任务列表对象
                    mMetaList.setContentByRemoteJSON(object); // 根据远程JSON内容设置任务列表

                    // load meta data
                    JSONArray jsMetas = client.getTaskList(gid); // 获取元数据数组
                    for (int j = 0; j < jsMetas.length(); j++) { // 遍历元数据数组
                        object = (JSONObject) jsMetas.getJSONObject(j); // 获取当前元数据对象
                        MetaData metaData = new MetaData(); // 创建新的元数据对象
                        metaData.setContentByRemoteJSON(object); // 根据远程JSON内容设置元数据
                        if (metaData.isWorthSaving()) { // 如果值得保存
                            mMetaList.addChildTask(metaData); // 将元数据添加到元数据列表中
                            if (metaData.getGid() != null) {
                                mMetaHashMap.put(metaData.getRelatedGid(), metaData); // 将元数据ID和元数据对象放入哈希映射中
                            }
                        }
                    }
                }
            }

            // create meta list if not existed
            if (mMetaList == null) { // 如果元数据列表不存在
                mMetaList = new TaskList(); // 创建新的任务列表对象
                mMetaList.setName(GTaskStringUtils.MIUI_FOLDER_PREFFIX + GTaskStringUtils.FOLDER_META); // 设置任务列表名称
                GTaskClient.getInstance().createTaskList(mMetaList); // 创建任务列表
            }

            // init task list
            for (int i = 0; i < jsTaskLists.length(); i++) { // 遍历任务列表数组
                JSONObject object = jsTaskLists.getJSONObject(i); // 获取当前任务列表对象
                String gid = object.getString(GTaskStringUtils.GTASK_JSON_ID); // 获取任务列表ID
                String name = object.getString(GTaskStringUtils.GTASK_JSON_NAME); // 获取任务列表名称

                if (name.startsWith(GTaskStringUtils.MIUI_FOLDER_PREFFIX) // 如果以特定前缀开头
                        && !name.equals(GTaskStringUtils.MIUI_FOLDER_PREFFIX + GTaskStringUtils.FOLDER_META)) { // 并且不是元数据文件夹
                    TaskList tasklist = new TaskList(); // 创建新的任务列表对象
                    tasklist.setContentByRemoteJSON(object); // 根据远程JSON内容设置任务列表
                    mGTaskListHashMap.put(gid, tasklist); // 将任务列表ID和任务列表对象放入哈希映射中
                    mGTaskHashMap.put(gid, tasklist); // 将任务列表ID和任务列表对象放入哈希映射中

                    // load tasks
                    JSONArray jsTasks = client.getTaskList(gid); // 获取任务数组
                    for (int j = 0; j < jsTasks.length(); j++) { // 遍历任务数组
                        object = (JSONObject) jsTasks.getJSONObject(j); // 获取当前任务对象
                        gid = object.getString(GTaskStringUtils.GTASK_JSON_ID); // 获取任务ID
                        Task task = new Task(); // 创建新的任务对象
                        task.setContentByRemoteJSON(object); // 根据远程JSON内容设置任务
                        if (task.isWorthSaving()) { // 如果值得保存
                            task.setMetaInfo(mMetaHashMap.get(gid)); // 设置任务的元数据信息
                            tasklist.addChildTask(task); // 将任务添加到任务列表中
                            mGTaskHashMap.put(gid, task); // 将任务ID和任务对象放入哈希映射中
                        }
                    }
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, e.toString()); // 记录JSON异常
            e.printStackTrace(); // 打印堆栈跟踪
            throw new ActionFailureException("initGTaskList: handing JSONObject failed"); // 抛出动作失败异常
        }
    }

    /*用于同步内容
    首先清空本地删除ID映射
    然后根据条件判断是否取消操作
    接着处理本地已删除的笔记和数据库中存在的笔记，执行相应的内容同步操作
    在处理完剩余项后，通过迭代器遍历剩余的哈希映射项并执行本地添加操作
    最后根据取消状态执行批量删除本地已删除笔记和刷新本地同步ID*/
    private void syncContent() throws NetworkFailureException {
        int syncType;
        Cursor c = null;
        String gid;
        Node node;

        mLocalDeleteIdMap.clear(); // 清空本地删除ID映射

        if (mCancelled) { // 如果已取消，直接返回
            return;
        }

        // for local deleted note
        try {
            c = mContentResolver.query(Notes.CONTENT_NOTE_URI, SqlNote.PROJECTION_NOTE,
                    "(type<>? AND parent_id=?)", new String[] {
                            String.valueOf(Notes.TYPE_SYSTEM), String.valueOf(Notes.ID_TRASH_FOLER)
                    }, null); // 查询非系统类型且不在垃圾箱中的笔记
            if (c != null) {
                while (c.moveToNext()) {
                    gid = c.getString(SqlNote.GTASK_ID_COLUMN); // 获取Google任务ID
                    node = mGTaskHashMap.get(gid); // 获取对应的节点
                    if (node != null) {
                        mGTaskHashMap.remove(gid); // 从哈希映射中移除
                        doContentSync(Node.SYNC_ACTION_DEL_REMOTE, node, c); // 执行内容同步，删除远程笔记
                    }

                    mLocalDeleteIdMap.add(c.getLong(SqlNote.ID_COLUMN)); // 添加本地删除ID
                }
            } else {
                Log.w(TAG, "failed to query trash folder"); // 查询垃圾箱失败
            }
        } finally {
            if (c != null) {
                c.close(); // 关闭游标
                c = null;
            }
        }

        // sync folder first
        syncFolder(); // 先同步文件夹

        // for note existing in database
        try {
            c = mContentResolver.query(Notes.CONTENT_NOTE_URI, SqlNote.PROJECTION_NOTE,
                    "(type=? AND parent_id<>?)", new String[] {
                            String.valueOf(Notes.TYPE_NOTE), String.valueOf(Notes.ID_TRASH_FOLER)
                    }, NoteColumns.TYPE + " DESC"); // 查询数据库中存在的笔记
            if (c != null) {
                while (c.moveToNext()) {
                    gid = c.getString(SqlNote.GTASK_ID_COLUMN); // 获取Google任务ID
                    node = mGTaskHashMap.get(gid); // 获取对应的节点
                    if (node != null) {
                        mGTaskHashMap.remove(gid); // 从哈希映射中移除
                        mGidToNid.put(gid, c.getLong(SqlNote.ID_COLUMN)); // 将Google任务ID和本地笔记ID放入映射中
                        mNidToGid.put(c.getLong(SqlNote.ID_COLUMN), gid); // 将本地笔记ID和Google任务ID放入映射中
                        syncType = node.getSyncAction(c); // 获取同步类型
                    } else {
                        if (c.getString(SqlNote.GTASK_ID_COLUMN).trim().length() == 0) {
                            // local add
                            syncType = Node.SYNC_ACTION_ADD_REMOTE; // 本地添加
                        } else {
                            // remote delete
                            syncType = Node.SYNC_ACTION_DEL_LOCAL; // 远程删除
                        }
                    }
                    doContentSync(syncType, node, c); // 执行内容同步
                }
            } else {
                Log.w(TAG, "failed to query existing note in database"); // 查询数据库中存在的笔记失败
            }

        } finally {
            if (c != null) {
                c.close(); // 关闭游标
                c = null;
            }
        }

        // go through remaining items
        Iterator<Map.Entry<String, Node>> iter = mGTaskHashMap.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, Node> entry = iter.next();
            node = entry.getValue();
            doContentSync(Node.SYNC_ACTION_ADD_LOCAL, node, null); // 执行内容同步，本地添加
        }

        // mCancelled can be set by another thread, so we need to check one by one
        // clear local delete table
        if (!mCancelled) {
            if (!DataUtils.batchDeleteNotes(mContentResolver, mLocalDeleteIdMap)) {
                throw new ActionFailureException("failed to batch-delete local deleted notes"); // 批量删除本地已删除笔记失败
            }
        }

        // refresh local sync id
        if (!mCancelled) {
            GTaskClient.getInstance().commitUpdate(); // 提交更新
            refreshLocalSyncId(); // 刷新本地同步ID
        }
    }

    /*用于同步文件夹
    首先根据条件判断是否取消操作
    然后处理根文件夹、通话记录文件夹以及本地存在的文件夹
    接着处理远程添加的文件夹，并根据取消状态提交更新。*/
    private void syncFolder() throws NetworkFailureException {
        Cursor c = null;
        String gid;
        Node node;
        int syncType;

        if (mCancelled) { // 如果已取消，直接返回
            return;
        }

        // for root folder
        try {
            c = mContentResolver.query(ContentUris.withAppendedId(Notes.CONTENT_NOTE_URI,
                    Notes.ID_ROOT_FOLDER), SqlNote.PROJECTION_NOTE, null, null, null); // 查询根文件夹
            if (c != null) {
                c.moveToNext();
                gid = c.getString(SqlNote.GTASK_ID_COLUMN); // 获取Google任务ID
                node = mGTaskHashMap.get(gid); // 获取对应的节点
                if (node != null) {
                    mGTaskHashMap.remove(gid); // 从哈希映射中移除
                    mGidToNid.put(gid, (long) Notes.ID_ROOT_FOLDER); // 将Google任务ID和根文件夹ID放入映射中
                    mNidToGid.put((long) Notes.ID_ROOT_FOLDER, gid); // 将根文件夹ID和Google任务ID放入映射中
                    if (!node.getName().equals(
                            GTaskStringUtils.MIUI_FOLDER_PREFFIX + GTaskStringUtils.FOLDER_DEFAULT))
                        doContentSync(Node.SYNC_ACTION_UPDATE_REMOTE, node, c); // 执行内容同步，更新远程名称
                } else {
                    doContentSync(Node.SYNC_ACTION_ADD_REMOTE, node, c); // 执行内容同步，远程添加
                }
            } else {
                Log.w(TAG, "failed to query root folder"); // 查询根文件夹失败
            }
        } finally {
            if (c != null) {
                c.close(); // 关闭游标
                c = null;
            }
        }

        // for call-note folder
        try {
            c = mContentResolver.query(Notes.CONTENT_NOTE_URI, SqlNote.PROJECTION_NOTE, "(_id=?)",
                    new String[] {
                            String.valueOf(Notes.ID_CALL_RECORD_FOLDER)
                    }, null); // 查询通话记录文件夹
            if (c != null) {
                if (c.moveToNext()) {
                    gid = c.getString(SqlNote.GTASK_ID_COLUMN); // 获取Google任务ID
                    node = mGTaskHashMap.get(gid); // 获取对应的节点
                    if (node != null) {
                        mGTaskHashMap.remove(gid); // 从哈希映射中移除
                        mGidToNid.put(gid, (long) Notes.ID_CALL_RECORD_FOLDER); // 将Google任务ID和通话记录文件夹ID放入映射中
                        mNidToGid.put((long) Notes.ID_CALL_RECORD_FOLDER, gid); // 将通话记录文件夹ID和Google任务ID放入映射中
                        if (!node.getName().equals(
                                GTaskStringUtils.MIUI_FOLDER_PREFFIX
                                        + GTaskStringUtils.FOLDER_CALL_NOTE))
                            doContentSync(Node.SYNC_ACTION_UPDATE_REMOTE, node, c); // 执行内容同步，更新远程名称
                    } else {
                        doContentSync(Node.SYNC_ACTION_ADD_REMOTE, node, c); // 执行内容同步，远程添加
                    }
                }
            } else {
                Log.w(TAG, "failed to query call note folder"); // 查询通话记录文件夹失败
            }
        } finally {
            if (c != null) {
                c.close(); // 关闭游标
                c = null;
            }
        }

        // for local existing folders
        try {
            c = mContentResolver.query(Notes.CONTENT_NOTE_URI, SqlNote.PROJECTION_NOTE,
                    "(type=? AND parent_id<>?)", new String[] {
                            String.valueOf(Notes.TYPE_FOLDER), String.valueOf(Notes.ID_TRASH_FOLER)
                    }, NoteColumns.TYPE + " DESC"); // 查询本地存在的文件夹
            if (c != null) {
                while (c.moveToNext()) {
                    gid = c.getString(SqlNote.GTASK_ID_COLUMN); // 获取Google任务ID
                    node = mGTaskHashMap.get(gid); // 获取对应的节点
                    if (node != null) {
                        mGTaskHashMap.remove(gid); // 从哈希映射中移除
                        mGidToNid.put(gid, c.getLong(SqlNote.ID_COLUMN)); // 将Google任务ID和本地文件夹ID放入映射中
                        mNidToGid.put(c.getLong(SqlNote.ID_COLUMN), gid); // 将本地文件夹ID和Google任务ID放入映射中
                        syncType = node.getSyncAction(c); // 获取同步类型
                    } else {
                        if (c.getString(SqlNote.GTASK_ID_COLUMN).trim().length() == 0) {
                            // local add
                            syncType = Node.SYNC_ACTION_ADD_REMOTE; // 本地添加
                        } else {
                            // remote delete
                            syncType = Node.SYNC_ACTION_DEL_LOCAL; // 远程删除
                        }
                    }
                    doContentSync(syncType, node, c); // 执行内容同步
                }
            } else {
                Log.w(TAG, "failed to query existing folder"); // 查询本地存在的文件夹失败
            }
        } finally {
            if (c != null) {
                c.close(); // 关闭游标
                c = null;
            }
        }

        // for remote add folders
        Iterator<Map.Entry<String, TaskList>> iter = mGTaskListHashMap.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, TaskList> entry = iter.next();
            gid = entry.getKey();
            node = entry.getValue();
            if (mGTaskHashMap.containsKey(gid)) {
                mGTaskHashMap.remove(gid); // 从哈希映射中移除
                doContentSync(Node.SYNC_ACTION_ADD_LOCAL, node, null); // 执行内容同步，本地添加
            }
        }

        if (!mCancelled)
            GTaskClient.getInstance().commitUpdate(); // 提交更新
    }

    /*用于根据同步类型执行相应的操作
    包括添加本地节点、添加远程节点、删除本地节点、删除远程节点、更新本地节点、更新远程节点等*/
    private void doContentSync(int syncType, Node node, Cursor c) throws NetworkFailureException {
        if (mCancelled) {
            return;
        }

        MetaData meta;
        switch (syncType) {
            case Node.SYNC_ACTION_ADD_LOCAL:
                addLocalNode(node); // 添加本地节点
                break;
            case Node.SYNC_ACTION_ADD_REMOTE:
                addRemoteNode(node, c); // 添加远程节点
                break;
            case Node.SYNC_ACTION_DEL_LOCAL:
                meta = mMetaHashMap.get(c.getString(SqlNote.GTASK_ID_COLUMN));
                if (meta != null) {
                    GTaskClient.getInstance().deleteNode(meta); // 删除本地节点
                }
                mLocalDeleteIdMap.add(c.getLong(SqlNote.ID_COLUMN)); // 将本地删除的ID添加到映射中
                break;
            case Node.SYNC_ACTION_DEL_REMOTE:
                meta = mMetaHashMap.get(node.getGid());
                if (meta != null) {
                    GTaskClient.getInstance().deleteNode(meta); // 删除远程节点的元数据
                }
                GTaskClient.getInstance().deleteNode(node); // 删除远程节点
                break;
            case Node.SYNC_ACTION_UPDATE_LOCAL:
                updateLocalNode(node, c); // 更新本地节点
                break;
            case Node.SYNC_ACTION_UPDATE_REMOTE:
                updateRemoteNode(node, c); // 更新远程节点
                break;
            case Node.SYNC_ACTION_UPDATE_CONFLICT:
                // merging both modifications maybe a good idea
                // right now just use local update simply
                updateRemoteNode(node, c); // 更新冲突节点，暂时只使用本地更新
                break;
            case Node.SYNC_ACTION_NONE:
                break;
            case Node.SYNC_ACTION_ERROR:
            default:
                throw new ActionFailureException("unkown sync action type"); // 抛出未知同步操作类型异常
        }
    }

    /*用于添加本地节点
    根据节点类型和内容创建相应的SqlNote对象，并根据节点的本地JSON内容设置SqlNote对象的内容和父文件夹
    最后提交SqlNote对象，更新Google任务ID和本地ID的映射，并更新远程元数据。*/
    private void addLocalNode(Node node) throws NetworkFailureException {
        if (mCancelled) {
            return;
        }

        SqlNote sqlNote;
        if (node instanceof TaskList) {
            if (node.getName().equals(
                    GTaskStringUtils.MIUI_FOLDER_PREFFIX + GTaskStringUtils.FOLDER_DEFAULT)) {
                sqlNote = new SqlNote(mContext, Notes.ID_ROOT_FOLDER); // 创建根文件夹的SqlNote对象
            } else if (node.getName().equals(
                    GTaskStringUtils.MIUI_FOLDER_PREFFIX + GTaskStringUtils.FOLDER_CALL_NOTE)) {
                sqlNote = new SqlNote(mContext, Notes.ID_CALL_RECORD_FOLDER); // 创建通话记录文件夹的SqlNote对象
            } else {
                sqlNote = new SqlNote(mContext);
                sqlNote.setContent(node.getLocalJSONFromContent()); // 设置内容为节点的本地JSON内容
                sqlNote.setParentId(Notes.ID_ROOT_FOLDER); // 设置父文件夹为根文件夹
            }
        } else {
            sqlNote = new SqlNote(mContext);
            JSONObject js = node.getLocalJSONFromContent();
            try {
                if (js.has(GTaskStringUtils.META_HEAD_NOTE)) {
                    JSONObject note = js.getJSONObject(GTaskStringUtils.META_HEAD_NOTE);
                    if (note.has(NoteColumns.ID)) {
                        long id = note.getLong(NoteColumns.ID);
                        if (DataUtils.existInNoteDatabase(mContentResolver, id)) {
                            // the id is not available, have to create a new one
                            note.remove(NoteColumns.ID);
                        }
                    }
                }

                if (js.has(GTaskStringUtils.META_HEAD_DATA)) {
                    JSONArray dataArray = js.getJSONArray(GTaskStringUtils.META_HEAD_DATA);
                    for (int i = 0; i < dataArray.length(); i++) {
                        JSONObject data = dataArray.getJSONObject(i);
                        if (data.has(DataColumns.ID)) {
                            long dataId = data.getLong(DataColumns.ID);
                            if (DataUtils.existInDataDatabase(mContentResolver, dataId)) {
                                // the data id is not available, have to create a new one
                                data.remove(DataColumns.ID);
                            }
                        }
                    }
                }
            } catch (JSONException e) {
                Log.w(TAG, e.toString());
                e.printStackTrace();
            }
            sqlNote.setContent(js);

            Long parentId = mGidToNid.get(((Task) node).getParent().getGid());
            if (parentId == null) {
                Log.e(TAG, "cannot find task's parent id locally");
                throw new ActionFailureException("cannot add local node");
            }
            sqlNote.setParentId(parentId.longValue());
        }

        // create the local node
        sqlNote.setGtaskId(node.getGid());
        sqlNote.commit(false); // 提交SqlNote对象

        // update gid-nid mapping
        mGidToNid.put(node.getGid(), sqlNote.getId()); // 更新Google任务ID和本地ID的映射
        mNidToGid.put(sqlNote.getId(), node.getGid()); // 更新本地ID和Google任务ID的映射

        // update meta
        updateRemoteMeta(node.getGid(), sqlNote); // 更新远程元数据
    }

    /*用于更新本地节点
    根据传入的节点和游标c，创建相应的SqlNote对象，并根据节点的本地JSON内容设置SqlNote对象的内容和父文件夹
    最后提交SqlNote对象，更新本地节点信息并更新远程元数据信息。*/
    private void updateLocalNode(Node node, Cursor c) throws NetworkFailureException {
        if (mCancelled) {
            return;
        }

        SqlNote sqlNote;
        // update the note locally
        sqlNote = new SqlNote(mContext, c); // 根据游标c创建SqlNote对象
        sqlNote.setContent(node.getLocalJSONFromContent()); // 设置SqlNote对象的内容为节点的本地JSON内容

        Long parentId = (node instanceof Task) ? mGidToNid.get(((Task) node).getParent().getGid())
                : new Long(Notes.ID_ROOT_FOLDER);
        if (parentId == null) {
            Log.e(TAG, "cannot find task's parent id locally");
            throw new ActionFailureException("cannot update local node");
        }
        sqlNote.setParentId(parentId.longValue()); // 设置SqlNote对象的父ID
        sqlNote.commit(true); // 提交SqlNote对象，进行更新操作

        // update meta info
        updateRemoteMeta(node.getGid(), sqlNote); // 更新远程元数据信息
    }

    /*用于添加远程节点
    根据传入的节点和游标c，创建相应的SqlNote对象，并根据SqlNote对象的类型进行远程更新操作
    根据节点类型（任务或任务列表）进行相应的处理，更新远程任务或任务列表信息，并更新本地SqlNote对象的任务ID和本地修改状态，以及更新Google任务ID和本地ID的映射关系。*/
    private void addRemoteNode(Node node, Cursor c) throws NetworkFailureException {
        if (mCancelled) { // 如果任务被取消，则返回
            return;
        }

        SqlNote sqlNote = new SqlNote(mContext, c); // 根据游标c创建SqlNote对象
        Node n;

        // update remotely
        if (sqlNote.isNoteType()) { // 如果SqlNote对象是笔记类型
            Task task = new Task(); // 创建一个任务对象
            task.setContentByLocalJSON(sqlNote.getContent()); // 根据SqlNote对象的内容设置任务对象的内容

            String parentGid = mNidToGid.get(sqlNote.getParentId()); // 获取父任务列表的Google ID
            if (parentGid == null) { // 如果找不到父任务列表的Google ID
                Log.e(TAG, "cannot find task's parent tasklist"); // 记录错误日志
                throw new ActionFailureException("cannot add remote task"); // 抛出异常
            }
            mGTaskListHashMap.get(parentGid).addChildTask(task); // 在父任务列表中添加子任务

            GTaskClient.getInstance().createTask(task); // 创建任务
            n = (Node) task; // 将任务对象转换为节点对象

            // add meta
            updateRemoteMeta(task.getGid(), sqlNote); // 更新远程元数据
        } else {
            TaskList tasklist = null;

            // we need to skip folder if it has already existed
            String folderName = GTaskStringUtils.MIUI_FOLDER_PREFFIX;
            if (sqlNote.getId() == Notes.ID_ROOT_FOLDER)
                folderName += GTaskStringUtils.FOLDER_DEFAULT;
            else if (sqlNote.getId() == Notes.ID_CALL_RECORD_FOLDER)
                folderName += GTaskStringUtils.FOLDER_CALL_NOTE;
            else
                folderName += sqlNote.getSnippet();

            Iterator<Map.Entry<String, TaskList>> iter = mGTaskListHashMap.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<String, TaskList> entry = iter.next();
                String gid = entry.getKey();
                TaskList list = entry.getValue();

                if (list.getName().equals(folderName)) {
                    tasklist = list;
                    if (mGTaskHashMap.containsKey(gid)) {
                        mGTaskHashMap.remove(gid);
                    }
                    break;
                }
            }

            // no match we can add now
            if (tasklist == null) {
                tasklist = new TaskList();
                tasklist.setContentByLocalJSON(sqlNote.getContent());
                GTaskClient.getInstance().createTaskList(tasklist);
                mGTaskListHashMap.put(tasklist.getGid(), tasklist);
            }
            n = (Node) tasklist;
        }

        // update local note
        sqlNote.setGtaskId(n.getGid()); // 设置SqlNote对象的Google任务ID
        sqlNote.commit(false); // 提交SqlNote对象，不包含本地修改
        sqlNote.resetLocalModified(); // 重置本地修改状态
        sqlNote.commit(true); // 提交SqlNote对象，包含本地修改

        // gid-id mapping
        mGidToNid.put(n.getGid(), sqlNote.getId()); // 将Google任务ID和本地ID进行映射
        mNidToGid.put(sqlNote.getId(), n.getGid()); // 将本地ID和Google任务ID进行映射
    }

    /*用于更新远程节点
    根据传入的节点和游标c，创建相应的SqlNote对象，并根据SqlNote对象的内容更新节点的内容，并将更新后的节点同步到远程
    同时更新远程元数据，如果需要移动任务，则根据当前和之前的父任务列表进行相应的操作，最后清除本地修改标志*/
    private void updateRemoteNode(Node node, Cursor c) throws NetworkFailureException {
        if (mCancelled) { // 如果任务被取消，则返回
            return;
        }

        SqlNote sqlNote = new SqlNote(mContext, c); // 根据游标c创建SqlNote对象

        // update remotely
        node.setContentByLocalJSON(sqlNote.getContent()); // 根据SqlNote对象的内容设置节点的内容
        GTaskClient.getInstance().addUpdateNode(node); // 更新节点到远程

        // update meta
        updateRemoteMeta(node.getGid(), sqlNote); // 更新远程元数据

        // move task if necessary
        if (sqlNote.isNoteType()) { // 如果SqlNote对象是笔记类型
            Task task = (Task) node; // 将节点转换为任务对象
            TaskList preParentList = task.getParent(); // 获取任务的当前父任务列表

            String curParentGid = mNidToGid.get(sqlNote.getParentId()); // 获取当前父任务列表的Google ID
            if (curParentGid == null) { // 如果找不到当前父任务列表的Google ID
                Log.e(TAG, "cannot find task's parent tasklist"); // 记录错误日志
                throw new ActionFailureException("cannot update remote task"); // 抛出异常
            }
            TaskList curParentList = mGTaskListHashMap.get(curParentGid); // 获取当前父任务列表

            if (preParentList != curParentList) { // 如果当前父任务列表与之前的父任务列表不同
                preParentList.removeChildTask(task); // 从之前的父任务列表中移除任务
                curParentList.addChildTask(task); // 在当前父任务列表中添加任务
                GTaskClient.getInstance().moveTask(task, preParentList, curParentList); // 移动任务到新的父任务列表
            }
        }

        // clear local modified flag
        sqlNote.resetLocalModified(); // 清除本地修改标志
        sqlNote.commit(true); // 提交SqlNote对象，包含本地修改
    }

    /*用于更新远程元数据
    根据传入的Google ID和SqlNote对象，如果SqlNote对象不为空且是笔记类型，根据Google ID获取相应的元数据对象
    如果元数据对象存在，则更新元数据内容并同步到远程
    如果元数据对象不存在，则创建新的元数据对象，并将其添加到元数据列表和哈希映射中，最后创建元数据任务并同步到远程*/
    private void updateRemoteMeta(String gid, SqlNote sqlNote) throws NetworkFailureException {
        if (sqlNote != null && sqlNote.isNoteType()) { // 如果SqlNote对象不为空且是笔记类型
            MetaData metaData = mMetaHashMap.get(gid); // 根据Google ID获取元数据对象
            if (metaData != null) { // 如果元数据对象不为空
                metaData.setMeta(gid, sqlNote.getContent()); // 设置元数据内容
                GTaskClient.getInstance().addUpdateNode(metaData); // 更新元数据到远程
            } else {
                metaData = new MetaData(); // 创建新的元数据对象
                metaData.setMeta(gid, sqlNote.getContent()); // 设置元数据内容
                mMetaList.addChildTask(metaData); // 在元数据列表中添加元数据对象
                mMetaHashMap.put(gid, metaData); // 将元数据对象添加到元数据哈希映射中
                GTaskClient.getInstance().createTask(metaData); // 创建元数据任务
            }
        }
    }

    /*用于刷新本地同步ID
    首先清空相关哈希映射，然后查询本地笔记并遍历结果，根据Google ID获取节点并更新本地笔记的同步ID为节点的最后修改时间
    如果节点为空，则记录错误日志并抛出异常*/
    private void refreshLocalSyncId() throws NetworkFailureException {
        if (mCancelled) { // 如果任务被取消，则返回
            return;
        }

        // get the latest gtask list
        mGTaskHashMap.clear(); // 清空Google任务哈希映射
        mGTaskListHashMap.clear(); // 清空Google任务列表哈希映射
        mMetaHashMap.clear(); // 清空元数据哈希映射
        initGTaskList(); // 初始化Google任务列表

        Cursor c = null; // 声明游标对象
        try {
            c = mContentResolver.query(Notes.CONTENT_NOTE_URI, SqlNote.PROJECTION_NOTE,
                    "(type<>? AND parent_id<>?)", new String[] {
                            String.valueOf(Notes.TYPE_SYSTEM), String.valueOf(Notes.ID_TRASH_FOLER)
                    }, NoteColumns.TYPE + " DESC"); // 查询本地笔记，按类型降序排列
            if (c != null) {
                while (c.moveToNext()) { // 遍历查询结果
                    String gid = c.getString(SqlNote.GTASK_ID_COLUMN); // 获取Google ID
                    Node node = mGTaskHashMap.get(gid); // 根据Google ID获取节点
                    if (node != null) { // 如果节点不为空
                        mGTaskHashMap.remove(gid); // 移除Google任务哈希映射中的节点
                        ContentValues values = new ContentValues(); // 创建内容值对象
                        values.put(NoteColumns.SYNC_ID, node.getLastModified()); // 设置同步ID为节点的最后修改时间
                        mContentResolver.update(ContentUris.withAppendedId(Notes.CONTENT_NOTE_URI,
                                c.getLong(SqlNote.ID_COLUMN)), values, null, null); // 更新本地笔记的同步ID
                    } else {
                        Log.e(TAG, "something is missed"); // 记录错误日志
                        throw new ActionFailureException(
                                "some local items don't have gid after sync"); // 抛出异常
                    }
                }
            } else {
                Log.w(TAG, "failed to query local note to refresh sync id"); // 记录警告日志
            }
        } finally {
            if (c != null) {
                c.close(); // 关闭游标
                c = null; // 将游标置为null
            }
        }
    }

    /*用于获取同步账户的名称
    调用了GTaskClient类的静态方法getInstance()获取GTaskClient的实例，然后获取该实例的同步账户名称并返回*/
    public String getSyncAccount() {
        return GTaskClient.getInstance().getSyncAccount().name; // 返回GTaskClient实例的同步账户名称
    }

    /*用于取消同步操作,将类中的mCancelled变量设置为true，表示取消当前的同步操作*/
    public void cancelSync() {
        mCancelled = true;
    }
}
