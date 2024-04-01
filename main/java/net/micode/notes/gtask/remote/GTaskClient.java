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

import android.accounts.Account; // Android账户类，用于管理用户账户信息
import android.accounts.AccountManager; // Android账户管理器类，用于访问和管理用户账户
import android.accounts.AccountManagerFuture; // 用于异步操作的账户管理器Future类
import android.app.Activity; // Android应用程序中的活动类，用于用户界面交互和操作
import android.os.Bundle; // Android应用程序中的Bundle类，用于在Intent中传递数据
import android.text.TextUtils; // Android文本工具类，用于处理文本相关操作
import android.util.Log; // Android日志类，用于在应用程序中输出日志信息

import net.micode.notes.gtask.data.Node; // 自定义类，表示GTask中的节点
import net.micode.notes.gtask.data.Task; // 自定义类，表示GTask中的任务
import net.micode.notes.gtask.data.TaskList; // 自定义类，表示GTask中的任务列表
import net.micode.notes.gtask.exception.ActionFailureException; // 自定义异常类，表示操作失败异常
import net.micode.notes.gtask.exception.NetworkFailureException; // 自定义异常类，表示网络连接失败异常
import net.micode.notes.tool.GTaskStringUtils; // 自定义工具类，提供与GTask相关的字符串操作
import net.micode.notes.ui.NotesPreferenceActivity; // 自定义活动类，表示笔记首选项设置界面

import org.apache.http.HttpEntity; // Apache HTTP实体类，表示HTTP消息的实体部分
import org.apache.http.HttpResponse; // Apache HTTP响应类，表示HTTP请求的响应
import org.apache.http.client.ClientProtocolException; // Apache HTTP客户端协议异常类
import org.apache.http.client.entity.UrlEncodedFormEntity; // Apache HTTP编码表单实体类，用于发送编码的表单数据
import org.apache.http.client.methods.HttpGet; // Apache HTTP GET请求类，用于发送HTTP GET请求
import org.apache.http.client.methods.HttpPost; // Apache HTTP POST请求类，用于发送HTTP POST请求
import org.apache.http.cookie.Cookie; // Apache HTTP Cookie类，表示HTTP请求和响应中的Cookie
import org.apache.http.impl.client.BasicCookieStore; // Apache HTTP基本Cookie存储类，用于在客户端保存Cookie
import org.apache.http.impl.client.DefaultHttpClient; // Apache HTTP默认HTTP客户端类，用于发送HTTP请求
import org.apache.http.message.BasicNameValuePair; // Apache HTTP基本名称值对类，用于构建HTTP请求参数
import org.apache.http.params.BasicHttpParams; // Apache HTTP基本HTTP参数类，用于设置HTTP参数
import org.apache.http.params.HttpConnectionParams; // Apache HTTP连接参数类，用于设置HTTP连接参数
import org.apache.http.params.HttpParams; // Apache HTTP参数类，用于设置HTTP参数
import org.apache.http.params.HttpProtocolParams; // Apache HTTP协议参数类，用于设置HTTP协议参数
import org.json.JSONArray; // JSON数组类，表示JSON中的数组数据结构
import org.json.JSONException; // JSON异常类，表示处理JSON数据时可能发生的异常
import org.json.JSONObject; // JSON对象类，表示JSON中的对象数据结构

import java.io.BufferedReader; // 用于读取字符流的缓冲字符输入流类
import java.io.IOException; // 输入输出异常类，表示输入输出操作可能发生的异常
import java.io.InputStream; // 输入流类，用于读取字节流
import java.io.InputStreamReader; // 用于读取字符流的输入流读取器类
import java.util.LinkedList; // Java集合类，表示双向链表
import java.util.List; // Java集合类，表示列表
import java.util.zip.GZIPInputStream; // 用于解压缩GZIP格式数据的输入流类
import java.util.zip.Inflater; // 用于解压缩数据的类
import java.util.zip.InflaterInputStream; // 用于解压缩数据的输入流类

// GTaskClient类用于处理与Google任务相关的远程操作
public class GTaskClient {
    private static final String TAG = GTaskClient.class.getSimpleName(); // 静态常量，用于标识日志输出的标签

    private static final String GTASK_URL = "https://mail.google.com/tasks/"; // GTask的基础URL

    private static final String GTASK_GET_URL = "https://mail.google.com/tasks/ig"; // GTask的GET请求URL

    private static final String GTASK_POST_URL = "https://mail.google.com/tasks/r/ig"; // GTask的POST请求URL

    private static GTaskClient mInstance = null; // 静态实例变量，用于保存单例实例

    private DefaultHttpClient mHttpClient; // HTTP客户端变量

    private String mGetUrl; // GET请求URL变量

    private String mPostUrl; // POST请求URL变量

    private long mClientVersion; // 客户端版本号变量

    private boolean mLoggedin; // 登录状态变量

    private long mLastLoginTime; // 上次登录时间变量

    private int mActionId; // 操作ID变量

    private Account mAccount; // 账户变量

    private JSONArray mUpdateArray; // 更新数组变量

    // 初始化函数
    private GTaskClient() {
        mHttpClient = null; // 初始化HTTP客户端为null
        mGetUrl = GTASK_GET_URL; // 初始化GET请求URL
        mPostUrl = GTASK_POST_URL; // 初始化POST请求URL
        mClientVersion = -1; // 初始化客户端版本号为-1
        mLoggedin = false; // 初始化登录状态为false
        mLastLoginTime = 0; // 初始化上次登录时间为0
        mActionId = 1; // 初始化操作ID为1
        mAccount = null; // 初始化账户为null
        mUpdateArray = null; // 初始化更新数组为null
    }

    // 用于获取GTaskClient类的单例实例
    public static synchronized GTaskClient getInstance() {
        if (mInstance == null) { // 如果单例实例为空
            mInstance = new GTaskClient(); // 创建新的GTaskClient实例
        }
        return mInstance; // 返回单例实例
    }

    // 用于登录用户的Google账户并访问GTask服务
    public boolean login(Activity activity) {
        // we suppose that the cookie would expire after 5 minutes
        // then we need to re-login
        final long interval = 1000 * 60 * 5; // 定义5分钟的时间间隔
        if (mLastLoginTime + interval < System.currentTimeMillis()) { // 如果距离上次登录时间超过5分钟
            mLoggedin = false; // 设置登录状态为false
        }

        // need to re-login after account switch
        if (mLoggedin && !TextUtils.equals(getSyncAccount().name, NotesPreferenceActivity.getSyncAccountName(activity))) {
            mLoggedin = false; // 如果已登录且账户不匹配，则设置登录状态为false
        }

        if (mLoggedin) {
            Log.d(TAG, "already logged in"); // 已经登录，输出日志
            return true;
        }

        mLastLoginTime = System.currentTimeMillis(); // 更新上次登录时间为当前时间
        String authToken = loginGoogleAccount(activity, false); // 登录Google账户，获取认证令牌
        if (authToken == null) {
            Log.e(TAG, "login google account failed"); // 登录Google账户失败，输出错误日志
            return false;
        }

        // login with custom domain if necessary
        if (!(mAccount.name.toLowerCase().endsWith("gmail.com") || mAccount.name.toLowerCase().endsWith("googlemail.com"))) {
            StringBuilder url = new StringBuilder(GTASK_URL).append("a/"); // 构建自定义域名URL
            int index = mAccount.name.indexOf('@') + 1; // 获取@符号后的位置
            String suffix = mAccount.name.substring(index); // 获取域名后缀
            url.append(suffix + "/"); // 拼接域名后缀
            mGetUrl = url.toString() + "ig"; // 设置GET请求URL
            mPostUrl = url.toString() + "r/ig"; // 设置POST请求URL

            if (tryToLoginGtask(activity, authToken)) { // 尝试使用认证令牌登录GTask
                mLoggedin = true; // 登录成功
            }
        }

        // try to login with google official url
        if (!mLoggedin) {
            mGetUrl = GTASK_GET_URL; // 设置GET请求URL为默认值
            mPostUrl = GTASK_POST_URL; // 设置POST请求URL为默认值
            if (!tryToLoginGtask(activity, authToken)) { // 尝试使用认证令牌登录GTask
                return false; // 登录失败
            }
        }

        mLoggedin = true; // 设置登录状态为true
        return true;
    }

    // 用于登录用户的Google账户并获取认证令牌
    private String loginGoogleAccount(Activity activity, boolean invalidateToken) {
        String authToken; // 声明认证令牌变量
        AccountManager accountManager = AccountManager.get(activity); // 获取账户管理器实例
        Account[] accounts = accountManager.getAccountsByType("com.google"); // 获取所有Google账户

        if (accounts.length == 0) {
            Log.e(TAG, "there is no available google account"); // 如果没有可用的Google账户，输出错误日志
            return null;
        }

        String accountName = NotesPreferenceActivity.getSyncAccountName(activity); // 获取同步账户名称
        Account account = null;
        for (Account a : accounts) {
            if (a.name.equals(accountName)) { // 查找与设置中的账户名称匹配的账户
                account = a; // 找到匹配的账户
                break;
            }
        }
        if (account != null) {
            mAccount = account; // 将找到的账户设置为当前账户
        } else {
            Log.e(TAG, "unable to get an account with the same name in the settings"); // 找不到匹配的账户，输出错误日志
            return null;
        }

        // 获取令牌
        AccountManagerFuture<Bundle> accountManagerFuture = accountManager.getAuthToken(account, "goanna_mobile", null, activity, null, null);
        try {
            Bundle authTokenBundle = accountManagerFuture.getResult(); // 获取账户令牌信息
            authToken = authTokenBundle.getString(AccountManager.KEY_AUTHTOKEN); // 获取认证令牌
            if (invalidateToken) {
                accountManager.invalidateAuthToken("com.google", authToken); // 使令牌无效
                loginGoogleAccount(activity, false); // 重新登录Google账户
            }
        } catch (Exception e) {
            Log.e(TAG, "get auth token failed"); // 获取认证令牌失败，输出错误日志
            authToken = null; // 设置认证令牌为null
        }

        return authToken; // 返回认证令牌
    }

    // 用于尝试登录GTask服务
    private boolean tryToLoginGtask(Activity activity, String authToken) {
        if (!loginGtask(authToken)) { // 尝试登录GTask服务，如果失败
            // 可能认证令牌已过期，现在让我们使令牌失效并重试
            authToken = loginGoogleAccount(activity, true); // 使认证令牌失效并重新获取
            if (authToken == null) {
                Log.e(TAG, "login google account failed"); // 登录Google账户失败，输出错误日志
                return false;
            }

            if (!loginGtask(authToken)) { // 再次尝试登录GTask服务
                Log.e(TAG, "login gtask failed"); // 登录GTask服务失败，输出错误日志
                return false;
            }
        }
        return true; // 登录成功
    }

    // 用于登录GTask服务
    private boolean loginGtask(String authToken) {
        int timeoutConnection = 10000; // 设置连接超时时间为10秒
        int timeoutSocket = 15000; // 设置Socket超时时间为15秒
        HttpParams httpParameters = new BasicHttpParams(); // 创建HTTP参数对象
        HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection); // 设置连接超时时间
        HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket); // 设置Socket超时时间
        mHttpClient = new DefaultHttpClient(httpParameters); // 创建默认的HTTP客户端
        BasicCookieStore localBasicCookieStore = new BasicCookieStore(); // 创建基本的Cookie存储
        mHttpClient.setCookieStore(localBasicCookieStore); // 设置HTTP客户端的Cookie存储
        HttpProtocolParams.setUseExpectContinue(mHttpClient.getParams(), false); // 设置不使用Expect: 100-continue机制

        // login gtask
        try {
            String loginUrl = mGetUrl + "?auth=" + authToken; // 构建登录URL
            HttpGet httpGet = new HttpGet(loginUrl); // 创建HTTP GET请求
            HttpResponse response = mHttpClient.execute(httpGet); // 执行HTTP GET请求

            // get the cookie now
            List<Cookie> cookies = mHttpClient.getCookieStore().getCookies(); // 获取Cookie
            boolean hasAuthCookie = false;
            for (Cookie cookie : cookies) {
                if (cookie.getName().contains("GTL")) { // 检查是否包含名为"GTL"的Cookie
                    hasAuthCookie = true;
                }
            }
            if (!hasAuthCookie) {
                Log.w(TAG, "it seems that there is no auth cookie"); // 如果没有认证Cookie，输出警告日志
            }

            // get the client version
            String resString = getResponseContent(response.getEntity()); // 获取响应内容
            String jsBegin = "_setup(";
            String jsEnd = ")}</script>";
            int begin = resString.indexOf(jsBegin);
            int end = resString.lastIndexOf(jsEnd);
            String jsString = null;
            if (begin != -1 && end != -1 && begin < end) {
                jsString = resString.substring(begin + jsBegin.length(), end); // 提取JavaScript字符串
            }
            JSONObject js = new JSONObject(jsString); // 创建JSON对象
            mClientVersion = js.getLong("v"); // 获取客户端版本号
        } catch (JSONException e) {
            Log.e(TAG, e.toString()); // JSON异常处理，输出错误日志
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            // simply catch all exceptions
            Log.e(TAG, "httpget gtask_url failed"); // 捕获所有其他异常，输出错误日志
            return false;
        }

        return true; // 登录成功
    }

    // 用于获取动作ID
    private int getActionId() {
        return mActionId++; // 返回当前的 mActionId 值，然后递增 mActionId
    }

    // 用于创建一个HttpPost对象并设置相应的请求头信息
    private HttpPost createHttpPost() {
        HttpPost httpPost = new HttpPost(mPostUrl); // 创建一个HttpPost对象并传入mPostUrl
        httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded;charset=utf-8"); // 设置请求头Content-Type为application/x-www-form-urlencoded;charset=utf-8
        httpPost.setHeader("AT", "1"); // 设置请求头AT为1
        return httpPost; // 返回创建的HttpPost对象
    }

    // 用于从HttpEntity中获取响应内容
    private String getResponseContent(HttpEntity entity) throws IOException {
        String contentEncoding = null; // 初始化contentEncoding变量为null

        // 检查实体的内容编码
        if (entity.getContentEncoding() != null) {
            contentEncoding = entity.getContentEncoding().getValue(); // 获取内容编码的值
            Log.d(TAG, "encoding: " + contentEncoding); // 打印内容编码
        }

        InputStream input = entity.getContent(); // 获取实体的内容输入流

        // 根据内容编码进行处理
        if (contentEncoding != null && contentEncoding.equalsIgnoreCase("gzip")) {
            input = new GZIPInputStream(entity.getContent()); // 如果内容编码为gzip，使用GZIPInputStream解压缩输入流
        } else if (contentEncoding != null && contentEncoding.equalsIgnoreCase("deflate")) {
            Inflater inflater = new Inflater(true); // 创建一个用于解压缩的Inflater对象
            input = new InflaterInputStream(entity.getContent(), inflater); // 如果内容编码为deflate，使用InflaterInputStream解压缩输入流
        }

        try {
            InputStreamReader isr = new InputStreamReader(input); // 创建InputStreamReader对象
            BufferedReader br = new BufferedReader(isr); // 创建BufferedReader对象
            StringBuilder sb = new StringBuilder(); // 创建StringBuilder对象用于存储响应内容

            // 逐行读取响应内容并存储到StringBuilder中
            while (true) {
                String buff = br.readLine(); // 读取一行内容
                if (buff == null) {
                    return sb.toString(); // 如果读取完毕，返回StringBuilder中的内容
                }
                sb = sb.append(buff); // 将读取的内容添加到StringBuilder中
            }
        } finally {
            input.close(); // 关闭输入流
        }
    }

    // 用于发送Post请求并获取响应内容
    private JSONObject postRequest(JSONObject js) throws NetworkFailureException {
        if (!mLoggedin) {
            Log.e(TAG, "please login first");
            throw new ActionFailureException("not logged in");
        }

        HttpPost httpPost = createHttpPost(); // 创建一个HttpPost对象
        try {
            LinkedList<BasicNameValuePair> list = new LinkedList<BasicNameValuePair>(); // 创建一个LinkedList用于存储请求参数
            list.add(new BasicNameValuePair("r", js.toString())); // 将传入的JSONObject转换为字符串并添加到请求参数中
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(list, "UTF-8"); // 创建一个UrlEncodedFormEntity对象
            httpPost.setEntity(entity); // 设置HttpPost的实体

            // 执行Post请求
            HttpResponse response = mHttpClient.execute(httpPost); // 发起HTTP请求
            String jsString = getResponseContent(response.getEntity()); // 获取响应内容并转换为字符串
            return new JSONObject(jsString); // 将响应内容转换为JSONObject

        } catch (ClientProtocolException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
            throw new NetworkFailureException("postRequest failed due to ClientProtocolException");
        } catch (IOException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
            throw new NetworkFailureException("postRequest failed due to IOException");
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
            throw new ActionFailureException("unable to convert response content to JSONObject");
        } catch (Exception e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
            throw new ActionFailureException("error occurs when posting request");
        }
    }

    // 用于创建任务并发送到服务器
    public void createTask(Task task) throws NetworkFailureException {
        commitUpdate(); // 提交更新

        try {
            JSONObject jsPost = new JSONObject(); // 创建一个新的JSONObject对象
            JSONArray actionList = new JSONArray(); // 创建一个新的JSONArray对象用于存储操作列表

            // 构建操作列表
            actionList.put(task.getCreateAction(getActionId())); // 将创建任务的操作添加到操作列表中
            jsPost.put(GTaskStringUtils.GTASK_JSON_ACTION_LIST, actionList); // 将操作列表添加到jsPost中，使用常量作为键名

            // 添加客户端版本信息
            jsPost.put(GTaskStringUtils.GTASK_JSON_CLIENT_VERSION, mClientVersion); // 将客户端版本信息添加到jsPost中，使用常量作为键名

            // 发送Post请求并获取响应
            JSONObject jsResponse = postRequest(jsPost); // 发送Post请求并获取响应
            JSONObject jsResult = (JSONObject) jsResponse.getJSONArray(GTaskStringUtils.GTASK_JSON_RESULTS).get(0); // 获取响应中的结果信息
            task.setGid(jsResult.getString(GTaskStringUtils.GTASK_JSON_NEW_ID)); // 设置任务的全局唯一标识符（GID）

        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
            throw new ActionFailureException("create task: handling JSONObject failed");
        }
    }

    // 用于创建任务列表并发送到服务器
    public void createTaskList(TaskList tasklist) throws NetworkFailureException {
        commitUpdate(); // 提交更新

        try {
            JSONObject jsPost = new JSONObject(); // 创建一个新的JSONObject对象
            JSONArray actionList = new JSONArray(); // 创建一个新的JSONArray对象用于存储操作列表

            // action_list
            actionList.put(tasklist.getCreateAction(getActionId())); // 将创建任务列表的操作添加到操作列表中
            jsPost.put(GTaskStringUtils.GTASK_JSON_ACTION_LIST, actionList); // 将操作列表添加到jsPost中，使用常量作为键名

            // client version
            jsPost.put(GTaskStringUtils.GTASK_JSON_CLIENT_VERSION, mClientVersion); // 将客户端版本信息添加到jsPost中，使用常量作为键名

            // post
            JSONObject jsResponse = postRequest(jsPost); // 发送Post请求并获取响应
            JSONObject jsResult = (JSONObject) jsResponse.getJSONArray(GTaskStringUtils.GTASK_JSON_RESULTS).get(0); // 获取响应中的结果信息
            tasklist.setGid(jsResult.getString(GTaskStringUtils.GTASK_JSON_NEW_ID)); // 设置任务列表的全局唯一标识符（GID）

        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
            throw new ActionFailureException("create tasklist: handling JSONObject failed");
        }
    }

    // 用于提交更新到服务器
    public void commitUpdate() throws NetworkFailureException {
        if (mUpdateArray != null) { // 检查更新数组是否为空
            try {
                JSONObject jsPost = new JSONObject(); // 创建一个新的JSONObject对象

                // action_list
                jsPost.put(GTaskStringUtils.GTASK_JSON_ACTION_LIST, mUpdateArray); // 将更新数组添加到操作列表中

                // client_version
                jsPost.put(GTaskStringUtils.GTASK_JSON_CLIENT_VERSION, mClientVersion); // 将客户端版本信息添加到jsPost中，使用常量作为键名

                postRequest(jsPost); // 发送Post请求
                mUpdateArray = null; // 将更新数组置空，表示更新已提交
            } catch (JSONException e) {
                Log.e(TAG, e.toString());
                e.printStackTrace();
                throw new ActionFailureException("commit update: handling JSONObject failed");
            }
        }
    }

    // 用于向更新数组中添加节点的更新操作
    public void addUpdateNode(Node node) throws NetworkFailureException {
        if (node != null) { // 检查节点是否为空
            // too many update items may result in an error
            // set max to 10 items
            if (mUpdateArray != null && mUpdateArray.length() > 10) { // 检查更新数组是否已经存在且长度大于10
                commitUpdate(); // 如果更新数组长度大于10，则提交更新
            }

            if (mUpdateArray == null) // 如果更新数组为空
                mUpdateArray = new JSONArray(); // 创建一个新的JSONArray对象

            mUpdateArray.put(node.getUpdateAction(getActionId())); // 将节点的更新操作添加到更新数组中
        }
    }

    // 用于移动任务到不同的任务列表中
    public void moveTask(Task task, TaskList preParent, TaskList curParent)
            throws NetworkFailureException {
        commitUpdate(); // 提交当前的更新

        try {
            JSONObject jsPost = new JSONObject(); // 创建一个新的JSONObject对象
            JSONArray actionList = new JSONArray(); // 创建一个新的JSONArray对象
            JSONObject action = new JSONObject(); // 创建一个新的JSONObject对象用于表示动作

            // action_list
            action.put(GTaskStringUtils.GTASK_JSON_ACTION_TYPE,
                    GTaskStringUtils.GTASK_JSON_ACTION_TYPE_MOVE); // 设置动作类型为移动
            action.put(GTaskStringUtils.GTASK_JSON_ACTION_ID, getActionId()); // 设置动作ID
            action.put(GTaskStringUtils.GTASK_JSON_ID, task.getGid()); // 设置任务的ID
            if (preParent == curParent && task.getPriorSibling() != null) {
                // put prioring_sibing_id only if moving within the tasklist and
                // it is not the first one
                action.put(GTaskStringUtils.GTASK_JSON_PRIOR_SIBLING_ID, task.getPriorSibling()); // 在任务列表内移动且不是第一个任务时，设置前一个兄弟节点的ID
            }
            action.put(GTaskStringUtils.GTASK_JSON_SOURCE_LIST, preParent.getGid()); // 设置源任务列表的ID
            action.put(GTaskStringUtils.GTASK_JSON_DEST_PARENT, curParent.getGid()); // 设置目标父节点的ID
            if (preParent != curParent) {
                // put the dest_list only if moving between tasklists
                action.put(GTaskStringUtils.GTASK_JSON_DEST_LIST, curParent.getGid()); // 当在不同任务列表之间移动时，设置目标任务列表的ID
            }
            actionList.put(action); // 将动作添加到动作列表中
            jsPost.put(GTaskStringUtils.GTASK_JSON_ACTION_LIST, actionList); // 将动作列表添加到jsPost中

            // client_version
            jsPost.put(GTaskStringUtils.GTASK_JSON_CLIENT_VERSION, mClientVersion); // 将客户端版本信息添加到jsPost中

            postRequest(jsPost); // 发送Post请求

        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
            throw new ActionFailureException("move task: handling JSONObject failed");
        }
    }

    // 用于删除节点并提交相应的更新
    public void deleteNode(Node node) throws NetworkFailureException {
        commitUpdate(); // 提交当前的更新

        try {
            JSONObject jsPost = new JSONObject(); // 创建一个新的JSONObject对象
            JSONArray actionList = new JSONArray(); // 创建一个新的JSONArray对象

            // action_list
            node.setDeleted(true); // 标记节点为已删除
            actionList.put(node.getUpdateAction(getActionId())); // 将节点的更新操作添加到动作列表中
            jsPost.put(GTaskStringUtils.GTASK_JSON_ACTION_LIST, actionList); // 将动作列表添加到jsPost中

            // client_version
            jsPost.put(GTaskStringUtils.GTASK_JSON_CLIENT_VERSION, mClientVersion); // 将客户端版本信息添加到jsPost中

            postRequest(jsPost); // 发送Post请求
            mUpdateArray = null; // 将更新数组置为空

        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
            throw new ActionFailureException("delete node: handling JSONObject failed");
        }
    }

    // 用于获取任务列表并处理相应的响应
    public JSONArray getTaskLists() throws NetworkFailureException {
        if (!mLoggedin) {
            Log.e(TAG, "please login first"); // 如果未登录，记录错误信息
            throw new ActionFailureException("not logged in"); // 抛出未登录异常
        }

        try {
            HttpGet httpGet = new HttpGet(mGetUrl); // 创建一个HttpGet对象
            HttpResponse response = mHttpClient.execute(httpGet); // 执行HTTP GET请求

            // get the task list
            String resString = getResponseContent(response.getEntity()); // 获取响应内容
            String jsBegin = "_setup("; // JSON字符串的起始标志
            String jsEnd = ")}</script>"; // JSON字符串的结束标志
            int begin = resString.indexOf(jsBegin); // 获取起始位置
            int end = resString.lastIndexOf(jsEnd); // 获取结束位置
            String jsString = null;
            if (begin != -1 && end != -1 && begin < end) {
                jsString = resString.substring(begin + jsBegin.length(), end); // 截取JSON字符串
            }
            JSONObject js = new JSONObject(jsString); // 创建一个新的JSONObject对象
            return js.getJSONObject("t").getJSONArray(GTaskStringUtils.GTASK_JSON_LISTS); // 返回任务列表的JSONArray

        } catch (ClientProtocolException e) {
            Log.e(TAG, e.toString()); // 记录异常信息
            e.printStackTrace();
            throw new NetworkFailureException("gettasklists: httpget failed"); // 抛出网络请求异常
        } catch (IOException e) {
            Log.e(TAG, e.toString()); // 记录异常信息
            e.printStackTrace();
            throw new NetworkFailureException("gettasklists: httpget failed"); // 抛出网络请求异常
        } catch (JSONException e) {
            Log.e(TAG, e.toString()); // 记录异常信息
            e.printStackTrace();
            throw new ActionFailureException("get task lists: handling JSONObject failed"); // 抛出处理JSON对象异常
        }
    }

    // 用于获取特定任务列表的任务并处理相应的响应
    public JSONArray getTaskList(String listGid) throws NetworkFailureException {
        commitUpdate(); // 提交当前的更新

        try {
            JSONObject jsPost = new JSONObject(); // 创建一个新的JSONObject对象
            JSONArray actionList = new JSONArray(); // 创建一个新的JSONArray对象
            JSONObject action = new JSONObject(); // 创建一个新的JSONObject对象

            // action_list
            action.put(GTaskStringUtils.GTASK_JSON_ACTION_TYPE, GTaskStringUtils.GTASK_JSON_ACTION_TYPE_GETALL); // 设置动作类型为获取所有任务
            action.put(GTaskStringUtils.GTASK_JSON_ACTION_ID, getActionId()); // 设置动作ID
            action.put(GTaskStringUtils.GTASK_JSON_LIST_ID, listGid); // 设置任务列表ID
            action.put(GTaskStringUtils.GTASK_JSON_GET_DELETED, false); // 设置是否获取已删除任务
            actionList.put(action); // 将动作添加到动作列表中
            jsPost.put(GTaskStringUtils.GTASK_JSON_ACTION_LIST, actionList); // 将动作列表添加到jsPost中

            // client_version
            jsPost.put(GTaskStringUtils.GTASK_JSON_CLIENT_VERSION, mClientVersion); // 将客户端版本信息添加到jsPost中

            JSONObject jsResponse = postRequest(jsPost); // 发送Post请求并获取响应
            return jsResponse.getJSONArray(GTaskStringUtils.GTASK_JSON_TASKS); // 返回任务列表的JSONArray

        } catch (JSONException e) {
            Log.e(TAG, e.toString()); // 记录异常信息
            e.printStackTrace();
            throw new ActionFailureException("get task list: handling JSONObject failed"); // 抛出处理JSON对象异常
        }
    }

    // 用于返回同步账户信息
    public Account getSyncAccount() {
        return mAccount; // 返回同步账户信息
    }

    // 用于将更新数组重置为null
    public void resetUpdateArray() {
        mUpdateArray = null; // 重置更新数组为null
    }
}
