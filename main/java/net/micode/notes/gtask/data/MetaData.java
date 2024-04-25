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
import android.util.Log;

import net.micode.notes.tool.GTaskStringUtils;

import org.json.JSONException;
import org.json.JSONObject;

/*
 * MetaData 类是针对Google任务（GTask）附加元数据信息的特殊类型任务。它扩展了基类Task，
 * 提供了与GTask标识符（gid）关联以及将元数据作为JSON对象存储在任务笔记字段中的功能
 */
public class MetaData extends Task {

    //用于日志记录的标签

    private final static String TAG = MetaData.class.getSimpleName();

    //与Google任务关联的标识符（gid），作为元数据的一部分存储

    private String mRelatedGid = null;

    /*
     * 为当前MetaData实例设置元数据。接收GTask的gid和包含其他元信息的JSONObject
     * 将gid添加到metaInfo中，并将整个metaInfo转换为字符串存入notes字段
     * 同时将任务名称设置为特定的常量值（GTaskStringUtils.META_NOTE_NAME）以标识此任务为元数据任务
     */
    public void setMeta(String gid, JSONObject metaInfo) {
        try {
            metaInfo.put(GTaskStringUtils.META_HEAD_GTASK_ID, gid);
        } catch (JSONException e) {
            Log.e(TAG, "将相关gid写入失败");
        }
        setNotes(metaInfo.toString());
        setName(GTaskStringUtils.META_NOTE_NAME);
    }

    //返回与此MetaData实例关联的GTask标识符（gid）

    public String getRelatedGid() {
        return mRelatedGid;
    }

    /*
     * 重写父类方法，判断当前任务是否值得保存。对于 MetaData 类型的任务，
     * 若其notes字段不为null，则认为该任务值得保存
     */
    @Override
    public boolean isWorthSaving() {
        return getNotes() != null;
    }

    /*
     * 从远程JSON数据设置任务内容。首先调用父类方法进行基本内容设置，然后检查notes字段是否非空
     * 若非空，尝试从中解析出包含GTask gid的JSON对象，并提取gid存储到mRelatedGid变量
     * 如果解析过程中发生JSONException，记录警告并清空mRelatedGid
     */
    @Override
    public void setContentByRemoteJSON(JSONObject js) {
        super.setContentByRemoteJSON(js);
        if (getNotes() != null) {
            try {
                JSONObject metaInfo = new JSONObject(getNotes().trim());
                mRelatedGid = metaInfo.getString(GTaskStringUtils.META_HEAD_GTASK_ID);
            } catch (JSONException e) {
                Log.w(TAG, "从notes中获取相关gid失败");
                mRelatedGid = null;
            }
        }
    }

    //抛出非法访问错误，指示该方法在 MetaData 类中不应被调用。

    @Override
    public void setContentByLocalJSON(JSONObject js) {
        // 此函数不应被调用
        throw new IllegalAccessError("MetaData:setContentByLocalJSON 不应被调用");
    }

    //抛出非法访问错误，指示该方法在MetaData类中不应被调用。

    @Override
    public JSONObject getLocalJSONFromContent() {
        throw new IllegalAccessError("MetaData:getLocalJSONFromContent 不应被调用");
    }

    //抛出非法访问错误，指示该方法在MetaData类中不应被调用。
    @Override
    public int getSyncAction(Cursor c) {
        throw new IllegalAccessError("MetaData:getSyncAction 不应被调用");
    }

}