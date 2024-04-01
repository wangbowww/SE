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

package net.micode.notes.gtask.exception; // 声明ActionFailureException类所在的包路径

/*
* ActionFailureException类继承自RuntimeException
* 提供了多个构造函数以支持不同的异常信息传递方式
* 这个异常类被用于捕获和处理操作失败的情况，并向调用者提供相关的异常信息
*/

//用于表示操作失败的异常
public class ActionFailureException extends RuntimeException { // RuntimeException的子类
    // 序列化版本UID，用于在反序列化时验证类的版本一致性。
    private static final long serialVersionUID = 4425249765923293627L;

    // 无参构造函数，调用父类RuntimeException的默认构造函数
    public ActionFailureException() {
        super();
    }

    // 带字符串参数的构造函数，调用父类RuntimeException的构造函数并传入参数
    public ActionFailureException(String paramString) {
        super(paramString);
    }

    // 又一个构造函数，依然调用父类的构造函数
    public ActionFailureException(String paramString, Throwable paramThrowable) {
        super(paramString, paramThrowable);
    }
}
