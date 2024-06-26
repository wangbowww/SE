package net.micode.notes.data;

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Handler;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Data;
import android.telephony.PhoneNumberUtils;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.HashMap;

/**
 * 联系人工具类。方法 getContact 用于获取给定电话号码对应的联系人名称
 * 这个方法首先检查一个静态 HashMap sContactCache 中是否已经缓存了该电话号码对应的联系人名称，如果缓存中存在，则直接返回该名称。
 * 如果缓存中不存在，它会构建一个查询字符串，用于查询 Android 系统的联系人数据库。然后执行查询，并根据结果获取联系人名称。如果查询结果不为空，它会将电话号码和联系人名称存入缓存中，并返回联系人名称。
 */
public class Contact {
    private static HashMap<String, String> sContactCache;
    private static final String TAG = "Contact";

    private static final String CALLER_ID_SELECTION = "PHONE_NUMBERS_EQUAL(" + Phone.NUMBER
            + ",?) AND " + Data.MIMETYPE + "='" + Phone.CONTENT_ITEM_TYPE + "'"
            + " AND " + Data.RAW_CONTACT_ID + " IN "
            + "(SELECT raw_contact_id "
            + " FROM phone_lookup"
            + " WHERE min_match = '+')";

    private static WeakReference<Context> sContextRef;

    public static void init(Context context) {
        sContextRef = new WeakReference<>(context.getApplicationContext());
    }

    public static String getContact(String phoneNumber) {
        if (sContactCache == null) {
            sContactCache = new HashMap<>();
        }

        if (sContactCache.containsKey(phoneNumber)) {
            return sContactCache.get(phoneNumber);
        }

        Context context = sContextRef.get();
        if (context == null) {
            Log.e(TAG, "Context is null");
            return null;
        }

        String selection = CALLER_ID_SELECTION.replace("+",
                PhoneNumberUtils.toCallerIDMinMatch(phoneNumber));
        Cursor cursor = context.getContentResolver().query(
                Data.CONTENT_URI,
                new String[] { Phone.DISPLAY_NAME },
                selection,
                new String[] { phoneNumber },
                null);

        if (cursor != null && cursor.moveToFirst()) {
            try {
                String name = cursor.getString(0);
                sContactCache.put(phoneNumber, name);
                return name;
            } catch (IndexOutOfBoundsException e) {
                Log.e(TAG, " Cursor get string error " + e.toString());
                return null;
            } finally {
                cursor.close();
            }
        } else {
            Log.d(TAG, "No contact matched with number:" + phoneNumber);
            return null;
        }
    }

    //lzier
    public static class ContactObserver extends ContentObserver {
        public ContactObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            // 清除联系人缓存
            sContactCache = null;
        }
    }
}
