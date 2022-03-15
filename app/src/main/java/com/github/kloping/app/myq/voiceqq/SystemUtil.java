package com.github.kloping.app.myq.voiceqq;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import org.bouncycastle.util.encoders.Hex;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static android.content.Context.TELEPHONY_SERVICE;

public class SystemUtil {

    /**
     * 获取默认的imei  一般都是IMEI 1
     *
     * @param context
     * @return
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    public static String getIMEI1(Context context) {
        //优先获取IMEI(即使是电信卡)  不行的话就获取MEID
        return getImeiOrMeid(context, 0);

    }

    /**
     * 获取imei2
     *
     * @param context
     * @return
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    public static String getIMEI2(Context context) {
        //imei2必须与 imei1不一样
        String imeiDefault = getIMEI1(context);
        if (TextUtils.isEmpty(imeiDefault)) {
            //默认的 imei 竟然为空，说明权限还没拿到，或者是平板
            //这种情况下，返回 imei2也应该是空串
            return "";
        }
        //注意，拿第一个 IMEI 是传0，第2个 IMEI 是传1，别搞错了
        String imei1 = getImeiOrMeid(context, 0);
        String imei2 = getImeiOrMeid(context, 1);
        //sim 卡换卡位时，imei1与 imei2有可能互换，而 imeidefault 有可能不变
        if (!TextUtils.equals(imei2, imeiDefault)) {
            //返回与 imeiDefault 不一样的
            return imei2;
        }
        if (!TextUtils.equals(imei1, imeiDefault)) {
            return imei1;
        }
        return "";
    }

    /**
     * 获取 Imei/Meid    优先获取IMEI(即使是电信卡)  不行的话就获取MEID
     * <p>
     * 如果装有CDMA制式的SIM卡(电信卡) ，在Android 8 以下 只能获取MEID ,无法获取到该卡槽的IMEI
     * 8及以上可以通过 #imei 方法获取IMEI  通过 #deviceId 方法获取的是MEID
     *
     * @param context
     * @param slotId  slotId为卡槽Id，它的值为 0、1；
     * @return
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    public static String getImeiOrMeid(Context context, int slotId) {
        String imei = "";
        //Android 6.0 以后需要获取动态权限  检查权限
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return imei;
        }
        try {
            TelephonyManager manager = (TelephonyManager) context.getApplicationContext().getSystemService(TELEPHONY_SERVICE);
            if (manager != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {// android 8 即以后建议用getImei 方法获取 不会获取到MEID
                    Method method = manager.getClass().getMethod("getImei", int.class);
                    imei = (String) method.invoke(manager, slotId);
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    //5.0的系统如果想获取MEID/IMEI1/IMEI2  ----framework层提供了两个属性值“ril.cdma.meid"和“ril.gsm.imei"获取
                    imei = getSystemPropertyByReflect("ril.gsm.imei");
                    //如果获取不到 就调用 getDeviceId 方法获取

                } else {//5.0以下获取imei/meid只能通过 getDeviceId  方法去取
                }
            }
        } catch (Exception e) {
        }
        if (TextUtils.isEmpty(imei)) {
            imei = getDeviceId(context, slotId);
        }
        return imei;
    }


    /**
     * 仅获取 Imei  如果获取到的是meid 或空  均返回空字符串
     *
     * @param slotId slotId为卡槽Id，它的值为 0、1；
     * @return
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    public static String getImeiOnly(Context context, int slotId) {
        String imei = "";

        //Android 6.0 以后需要获取动态权限  检查权限
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return imei;
        }

        try {
            TelephonyManager manager = (TelephonyManager) context.getApplicationContext().getSystemService(TELEPHONY_SERVICE);
            if (manager != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {// android 8 即以后建议用getImei 方法获取 不会获取到MEID
                    Method method = manager.getClass().getMethod("getImei", int.class);
                    imei = (String) method.invoke(manager, slotId);
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    //5.0的系统如果想获取MEID/IMEI1/IMEI2  ----framework层提供了两个属性值“ril.cdma.meid"和“ril.gsm.imei"获取
                    imei = getSystemPropertyByReflect("ril.gsm.imei");
                    //如果获取不到 就调用 getDeviceId 方法获取

                } else {//5.0以下获取imei/meid只能通过 getDeviceId  方法去取
                }
            }
        } catch (Exception e) {
        }

        if (TextUtils.isEmpty(imei)) {
            String imeiOrMeid = getDeviceId(context, slotId);
            //长度15 的是imei  14的是meid
            if (!TextUtils.isEmpty(imeiOrMeid) && imeiOrMeid.length() >= 15) {
                imei = imeiOrMeid;
            }
        }

        return imei;
    }

    /**
     * 仅获取 Meid  如果获取到的是imei 或空  均返回空字符串
     * 一般只有一个 meid  即获取到的二个是相同的
     *
     * @param context
     * @param slotId  slotId为卡槽Id，它的值为 0、1；
     * @return
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    public static String getMeidOnly(Context context, int slotId) {
        String meid = "";
        //Android 6.0 以后需要获取动态权限  检查权限
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return meid;
        }
        try {
            TelephonyManager manager = (TelephonyManager) context.getApplicationContext().getSystemService(TELEPHONY_SERVICE);
            if (manager != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {// android 8 即以后建议用getMeid 方法获取 不会获取到Imei
                    Method method = manager.getClass().getMethod("getMeid", int.class);
                    meid = (String) method.invoke(manager, slotId);
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    //5.0的系统如果想获取MEID/IMEI1/IMEI2  ----framework层提供了两个属性值“ril.cdma.meid"和“ril.gsm.imei"获取
                    meid = getSystemPropertyByReflect("ril.cdma.meid");
                    //如果获取不到 就调用 getDeviceId 方法获取

                } else {//5.0以下获取imei/meid只能通过 getDeviceId  方法去取
                }
            }
        } catch (Exception e) {
        }

        if (TextUtils.isEmpty(meid)) {
            String imeiOrMeid = getDeviceId(context, slotId);
            //长度15 的是imei  14的是meid
            if (imeiOrMeid.length() == 14) {
                meid = imeiOrMeid;
            }
        }
        return meid;
    }


    private static String getSystemPropertyByReflect(String key) {
        try {
            @SuppressLint("PrivateApi")
            Class<?> clz = Class.forName("android.os.SystemProperties");
            Method getMethod = clz.getMethod("get", String.class, String.class);
            return (String) getMethod.invoke(clz, key, "");
        } catch (Exception e) {/**/}
        return "";
    }

    /**
     * 获取 IMEI/MEID
     *
     * @param context 上下文
     * @return 获取到的值 或者 空串""
     */
    public static String getDeviceId(Context context) {
        String imei = "";
        //Android 6.0 以后需要获取动态权限  检查权限
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return imei;
        }
        // 1. 尝试通过系统api获取imei
        imei = getDeviceIdFromSystemApi(context);
        if (TextUtils.isEmpty(imei)) {
            imei = getDeviceIdByReflect(context);
        }
        return imei;
    }

    /**
     * 获取 IMEI/MEID
     *
     * @param context 上下文
     * @param slotId  slotId为卡槽Id，它的值为 0、1；
     * @return 获取到的值 或者 空串""
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    public static String getDeviceId(Context context, int slotId) {
        String imei = "";
        // 1. 尝试通过系统api获取imei
        imei = getDeviceIdFromSystemApi(context, slotId);
        if (TextUtils.isEmpty(imei)) {
            imei = getDeviceIdByReflect(context, slotId);
        }
        return imei;
    }

    /**
     * 调用系统接口获取 IMEI/MEID
     * <p>
     * Android 6.0之后如果用户不允许通过 {@link Manifest.permission#READ_PHONE_STATE} 权限的话，
     * 那么是没办法通过系统api进行获取 IMEI/MEID 的，但是可以通过{@linkplain #getDeviceIdByReflect(Context)} 反射}绕过权限进行获取
     *
     * @param context 上下文
     * @return 获取到的值 或者 空串""
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    public static String getDeviceIdFromSystemApi(Context context, int slotId) {
        String imei = "";
        try {
            TelephonyManager telephonyManager =
                    (TelephonyManager) context.getApplicationContext().getSystemService(TELEPHONY_SERVICE);
            if (telephonyManager != null) {
                imei = telephonyManager.getDeviceId(slotId);
            }
        } catch (Throwable e) {
        }
        return imei;
    }

    public static String getDeviceIdFromSystemApi(Context context) {
        String imei = "";
        try {
            TelephonyManager telephonyManager =
                    (TelephonyManager) context.getApplicationContext().getSystemService(TELEPHONY_SERVICE);
            if (telephonyManager != null) {
                imei = telephonyManager.getDeviceId();
            }
        } catch (Throwable e) {
        }
        return imei;
    }


    /**
     * 反射获取 IMEI/MEID
     * <p>
     * Android 6.0之后如果用户不允许通过 {@link Manifest.permission#READ_PHONE_STATE} 权限的话，
     * 那么是没办法通过系统api进行获取 IMEI/MEID 的，但是可以通过这个反射来尝试绕过权限进行获取
     *
     * @param context 上下文
     * @return 获取到的值 或者 空串""
     */
    public static String getDeviceIdByReflect(Context context) {
        try {
            TelephonyManager tm = (TelephonyManager) context.getApplicationContext().getSystemService(TELEPHONY_SERVICE);
            if (Build.VERSION.SDK_INT >= 21) {
                Method simMethod = TelephonyManager.class.getDeclaredMethod("getDefaultSim");
                Object sim = simMethod.invoke(tm);
                Method method = TelephonyManager.class.getDeclaredMethod("getDeviceId", int.class);
                return method.invoke(tm, sim).toString();
            } else {
                Class<?> clazz = Class.forName("com.android.internal.telephony.IPhoneSubInfo");
                Method subInfoMethod = TelephonyManager.class.getDeclaredMethod("getSubscriberInfo");
                subInfoMethod.setAccessible(true);
                Object subInfo = subInfoMethod.invoke(tm);
                Method method = clazz.getDeclaredMethod("getDeviceId");
                return method.invoke(subInfo).toString();
            }
        } catch (Throwable e) {

        }
        return "";
    }

    /**
     * 反射获取 deviceId
     *
     * @param context
     * @param slotId  slotId为卡槽Id，它的值为 0、1；
     * @return
     */
    public static String getDeviceIdByReflect(Context context, int slotId) {
        try {
            TelephonyManager tm = (TelephonyManager) context.getApplicationContext().getSystemService(TELEPHONY_SERVICE);
            Method method = tm.getClass().getMethod("getDeviceId", int.class);
            return method.invoke(tm, slotId).toString();
        } catch (Throwable e) {
        }
        return "";
    }


    public static Context context;

    /**
     * 反射获取 getSubscriberId ，既imsi
     *
     * @param subId
     * @return
     */
    public static String getSubscriberId(int subId) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(TELEPHONY_SERVICE);
        // 取得相关系统服务
        Class<?> telephonyManagerClass = null;
        String imsi = null;
        try {
            telephonyManagerClass = Class.forName("android.telephony.TelephonyManager");

            if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.LOLLIPOP) {
                Method method = telephonyManagerClass.getMethod("getSubscriberId", int.class);
                imsi = (String) method.invoke(telephonyManager, subId);
            } else if (android.os.Build.VERSION.SDK_INT == android.os.Build.VERSION_CODES.LOLLIPOP) {
                Method method = telephonyManagerClass.getMethod("getSubscriberId", long.class);
                imsi = (String) method.invoke(telephonyManager, (long) subId);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return imsi;
    }

    /**
     * 反射获取 getSubscriptionId ，既 subid
     *
     * @param slotId 卡槽位置（0，1）
     * @return
     */
    public static int getSubscriptionId(int slotId) {
        try {
            Method datamethod;
            int setsubid = -1;//定义要设置为默认数据网络的subid
            //获取默认数据网络subid   getDefaultDataSubId
            Class<?> SubscriptionManager = Class.forName("android.telephony.SubscriptionManager");
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) { // >= 24  7.0
                datamethod = SubscriptionManager.getDeclaredMethod("getDefaultDataSubscriptionId");
            } else {
                datamethod = SubscriptionManager.getDeclaredMethod("getDefaultDataSubId");
            }
            datamethod.setAccessible(true);
            int SubId = (int) datamethod.invoke(SubscriptionManager);


            Method subManagermethod = SubscriptionManager.getDeclaredMethod("from", Context.class);
            subManagermethod.setAccessible(true);
            Object subManager = subManagermethod.invoke(SubscriptionManager, context);

            //getActiveSubscriptionInfoForSimSlotIndex  //获取卡槽0或者卡槽1  可用的subid
            Method getActivemethod = SubscriptionManager.getDeclaredMethod("getActiveSubscriptionInfoForSimSlotIndex", int.class);
            getActivemethod.setAccessible(true);
            Object msubInfo = getActivemethod.invoke(subManager, slotId);  //getSubscriptionId

            Class<?> SubInfo = Class.forName("android.telephony.SubscriptionInfo");

            //slot0   获取卡槽0的subid
            int subid = -1;
            if (msubInfo != null) {
                Method getSubId0 = SubInfo.getMethod("getSubscriptionId");
                getSubId0.setAccessible(true);
                subid = (int) getSubId0.invoke(msubInfo);
            }
            return subid;
        } catch (Exception e) {
        }
        return -1;
    }

    /**
     * 获取运营商 IMSI
     * 默认为 IMEI1对应的 IMSI
     *
     * @return
     */
    public static String getSimOperator() {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(TELEPHONY_SERVICE);// 取得相关系统服务
        return telephonyManager.getSimOperator();
    }

    /**
     * 根据卡槽位置 获取运营商 IMSI
     *
     * @param slotId 卡槽位置（0，1）
     * @return
     */
    public static String getSimOperator(int slotId) {
        int subid = getSubscriptionId(slotId);
        if (subid == -1) {
            return null;
        }

        String imsi = getSubscriberId(subid);
        if (!TextUtils.isEmpty(imsi)) {
            return imsi;
        }

        return null;
    }

    /**
     * 通过卡槽位置拿 IMEI
     *
     * @param slotId (0, 1卡槽位置）
     * @return
     */
    public static String getImei(int slotId) {
        if (slotId != 0 && slotId != 1) {
            return null;
        }
        TelephonyManager tm = (TelephonyManager) context.getSystemService(TELEPHONY_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            return tm.getDeviceId(slotId);

        } else if (slotId == 0) {
            return tm.getDeviceId();

        } else {
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(TELEPHONY_SERVICE);// 取得相关系统服务
            Class<?> telephonyManagerClass = null;
            String imei = null;

            try {
                telephonyManagerClass = Class.forName("android.telephony.TelephonyManager");
                Method method = telephonyManagerClass.getMethod("getImei", int.class);
                imei = (String) method.invoke(telephonyManager, slotId);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return imei;
        }
    }

    /**
     * 将数据进行 MD5 加密，并以16进制字符串格式输出
     *
     * @param data
     * @return
     */
    public static String md5(String data) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            return Hex.toHexString(md.digest(data.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }
}
