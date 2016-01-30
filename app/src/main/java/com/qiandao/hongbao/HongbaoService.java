package com.qiandao.hongbao;

import android.accessibilityservice.AccessibilityService;
import android.annotation.TargetApi;
import android.app.KeyguardManager;
import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.Parcelable;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import android.view.Display;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by ZhongyiTong on 9/30/15.
 * <p/>
 * 抢红包主要的逻辑部分
 */
public class HongbaoService extends AccessibilityService {
    private static String Tag = "HongbaoService";
    /**
     * 已获取的红包队列
     */
    private List<String> fetchedIdentifiers = new ArrayList<>();
    /**
     * 待抢的红包队列
     */
    private List<AccessibilityNodeInfo> nodesToFetch = new ArrayList<>();

    /**
     * 允许的最大尝试次数
     */
    private static final int MAX_TTL = 24;

    private boolean flag = false;
    /**
     * 尝试次数
     */
    private int ttl = 0;
    private final static String TAG = "HONGBAO";
    private final static String NOTIFICATION_TIP = "[微信红包]";
    AccessibilityNodeInfo mCurrentNode;
    //电源管理
    private PowerManager pm;
    //唤醒锁
    private PowerManager.WakeLock lock = null;

    private KeyguardManager kManager;
    //安全锁
    private KeyguardManager.KeyguardLock kLock = null;
    //是否进行了亮屏解锁操作
    private boolean isPrepare = false;
    //红包软件是否可用
    private boolean isHongbaoAppOK = false;

    /**
     * AccessibilityEvent的回调方法
     * <p/>
     * 当窗体状态或内容变化时，根据当前阶段选择相应的入口
     *
     * @param event 事件
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
//        if (!Util.isUseable()) {
//            return;
//        }
        if (event.getEventType() == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
            String tip = event.getText().toString();
            Log.e(Tag, "onAccessibilityEvent: tip" + tip);
            if (!tip.contains(NOTIFICATION_TIP)) {
                return;
            }

            if (!isScreenOn(this)) {
                lightScreen();
                isPrepare = true;
            }
            if (isLockOn()) {
                unLock();
                isPrepare = true;
            }
            Parcelable parcelable = event.getParcelableData();
            if (parcelable instanceof Notification) {
                Notification notification = (Notification) parcelable;
                try {
                    if (Stage.getInstance().getCurrentStage() == Stage.FETCHED_STAGE) {
                        Log.e(Tag, "Stage.FETCHED_STAGE_>send()");
                        notification.contentIntent.send();
                    } else if (Stage.getInstance().getCurrentStage() != Stage.OPENING_STAGE) {
                        Log.e(Tag, "呵呵" + Stage.getInstance().getCurrentStage());
                        notification.contentIntent.send();
                    }

                } catch (Exception e) {
                    Log.e(Tag, "PendingIntent.CanceledException", e);
                }
            }
            return;
        }

        if (Stage.getInstance().mutex) return;

        Stage.getInstance().mutex = true;

        try {
            handleWindowChange(event.getSource());
        } finally {
            Stage.getInstance().mutex = false;
        }

    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void handleWindowChange(AccessibilityNodeInfo nodeInfo) {

        switch (Stage.getInstance().getCurrentStage()) {
            case Stage.OPENING_STAGE:
                Log.d(Tag, "OPENING_STAGE");
                // 调试信息，打印TTL
                Log.d("TTL", String.valueOf(ttl));
                int result = openHongbao(nodeInfo);
                /* 如果打开红包失败且还没到达最大尝试次数，重试 */
                if (result == -1 && ttl < MAX_TTL) {
                    return;
                } else if (result == 0) {
                    Log.e(Tag, "opened，to deleting");
                    Stage.getInstance().entering(Stage.DELETING_STAGE);
                    performMyGlobalAction(GLOBAL_ACTION_BACK);
                } else {
                    Log.e(Tag, "reOpen");
                    checkList(nodeInfo);
                    Stage.getInstance().entering(Stage.FETCHED_STAGE);
                }
                ttl = 0;
                if (nodesToFetch.size() == 0) handleWindowChange(nodeInfo);
                break;
            case Stage.OPENED_STAGE:
                Log.d(Tag, "OPENED_STAGE");
                List<AccessibilityNodeInfo> successNodes = nodeInfo.findAccessibilityNodeInfosByText("红包详情");
                if (successNodes.isEmpty() && ttl < MAX_TTL) {
                    ttl += 1;
                    return;
                }
                ttl = 0;
                isHongbaoAppOK = false;
                Stage.getInstance().entering(Stage.FETCHED_STAGE);
                Log.e(Tag, "!@!!!!!!!!!!!!!!!!!!!!!!回退");
                performMyGlobalAction(GLOBAL_ACTION_BACK);
                break;
            case Stage.FETCHED_STAGE:
                Log.d(Tag, "FETCHED_STAGE");
//                deleteHongbao(nodeInfo);
                if (!isHongbaoAppOK) {
                    isHongbaoAppOK = isHongbaoOK(nodeInfo);
                }

                /* 先消灭待抢红包队列中的红包 */
                if (nodesToFetch.size() > 0) {
                    /* 从最下面的红包开始戳 */
                    AccessibilityNodeInfo node = nodesToFetch.remove(nodesToFetch.size() - 1);
                    if (node.getParent() != null) {
                        String id = getHongbaoHash(node);

                        if (id == null) return;
                        mCurrentNode = node;
                        fetchedIdentifiers.add(id);

                        // 调试信息，在每次打开红包后打印出已经获取的红包
                        Log.d("fetched", Arrays.toString(fetchedIdentifiers.toArray()));

                        Stage.getInstance().entering(Stage.OPENING_STAGE);
                        node.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);

                    }
                    return;
                }
                Stage.getInstance().entering(Stage.FETCHING_STAGE);
                fetchHongbao(nodeInfo);
                Stage.getInstance().entering(Stage.FETCHED_STAGE);

                break;
            case Stage.DELETING_STAGE:
                Log.d(Tag, "DELETING_STAGE");
                checkBackFromHongbaoPage(nodeInfo);
                if (mCurrentNode != null && mCurrentNode.getParent() != null) {
                    mCurrentNode.getParent().performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK);
                    Log.e(Tag, "正在删除");
                    Stage.getInstance().entering(Stage.DELETED_STAGE);
                }
                break;
            case Stage.DELETED_STAGE:
                boolean tem = deleteHongbao(nodeInfo);
                if (!tem) {
                    Stage.getInstance().entering(Stage.DELETED_STAGE);
                    return;
                }
                Stage.getInstance().entering(Stage.FETCHED_STAGE);
                break;
        }
    }


    /**
     * 如果已经接收到红包并且还没有戳开
     * <p/>
     * 在聊天页面中，查找包含“领取红包”的节点，
     * 将这些节点去重后加入待抢红包队列
     *
     * @param nodeInfo 当前窗体的节点信息
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void fetchHongbao(AccessibilityNodeInfo nodeInfo) {
        if (nodeInfo == null) return;

//        List<AccessibilityNodeInfo> myOwnNodes = nodeInfo.findAccessibilityNodeInfosByText("查看红包");
        /* 聊天会话窗口，遍历节点匹配“领取红包” */
        List<AccessibilityNodeInfo> fetchNodes = nodeInfo.findAccessibilityNodeInfosByText("领取红包");
        if (fetchNodes.isEmpty()) {
            if (!flag && isPrepare) {
                flag = checkList(nodeInfo);
            }
            return;
        }
        /*没找到就返回*/
        for (AccessibilityNodeInfo cellNode : fetchNodes) {
//            String id = getHongbaoHash(cellNode);
//            if(fetchedIdentifiers.contains(id)){
//                Log.e(Tag, "id:" + id + ",这个红包，已经打开过");
//                cellNode.getParent().performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK);
//                deleteHongbao(nodeInfo);
//
//            }
//            /* 如果节点没有被回收且该红包没有抢过 */
//            if (id != null && !fetchedIdentifiers.contains(id)) {
            nodesToFetch.add(cellNode);
//                Log.e(Tag, "添加别人发的待抢红包");
        }
    }
//        if(!myOwnNodes.isEmpty()){
//            for (AccessibilityNodeInfo cellNode : myOwnNodes) {
//                String id = getHongbaoHash(cellNode);
//
//            /* 如果节点没有被回收且该红包没有抢过 */
//                if (id != null && !fetchedIdentifiers.contains(id)) {
//                    nodesToFetch.add(cellNode);
//                    Log.e(Tag, "cellNode:"+cellNode.);
//                    Log.e(Tag, "添加自己发的待抢红包");
//                }
//            }
//        }

    // 调试信息，在每次fetch后打印出待抢红包
//         Log.d("toFetch", Arrays.toString(nodesToFetch.toArray()));
//    }


    /**
     * 如果戳开红包但还未领取
     * <p/>
     * 第一种情况，当界面上出现“过期”(红包超过有效时间)、
     * “手慢了”(红包发完但没抢到)或“红包详情”(已经抢到)时，
     * 直接返回聊天界面
     * <p/>
     * 第二种情况，界面上出现“拆红包”时
     * 点击该节点，并将阶段标记为OPENED_STAGE
     * <p/>
     * 第三种情况，以上节点没有出现，
     * 说明窗体可能还在加载中，维持当前状态，TTL增加，返回重试
     *
     * @param nodeInfo 当前窗体的节点信息
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private int openHongbao(AccessibilityNodeInfo nodeInfo) {
        if (nodeInfo == null) return -1;
        /* 戳开红包，红包已被抢完，遍历节点匹配“红包详情”、“手慢了”和“过期” */
        List<AccessibilityNodeInfo> failureNoticeNodes = new ArrayList<>();
        failureNoticeNodes.addAll(nodeInfo.findAccessibilityNodeInfosByText("红包详情"));
        failureNoticeNodes.addAll(nodeInfo.findAccessibilityNodeInfosByText("手慢了"));
        failureNoticeNodes.addAll(nodeInfo.findAccessibilityNodeInfosByText("过期"));
        failureNoticeNodes.addAll(nodeInfo.findAccessibilityNodeInfosByText("失效"));
        if (!failureNoticeNodes.isEmpty()) {
            return 0;
        }

        /* 戳开红包，红包还没抢完，遍历节点匹配“拆红包” */
//        List<AccessibilityNodeInfo> successNoticeNodes = nodeInfo.findAccessibilityNodeInfosByText("拆红包");
//        List<AccessibilityNodeInfo> preventNoticeNodes = nodeInfo.findAccessibilityNodeInfosByText("领取红包");
        AccessibilityNodeInfo successNoticeNodes = (nodeInfo.getChildCount() > 3) ? nodeInfo.getChild(3) : null;
        if (successNoticeNodes != null) {
            Log.e(Tag, "successNoticeNodes:" + successNoticeNodes.getClassName());
        }
        if (successNoticeNodes != null && successNoticeNodes.getClassName().equals("android.widget.Button")) {
//        if (!successNoticeNodes.isEmpty()) {
//            AccessibilityNodeInfo openNode = successNoticeNodes.get(successNoticeNodes.size() - 1);
//            Log.e(Tag, "successNoticeNodes.size():" + successNoticeNodes.size());
//            Log.e(Tag, "openNode:" + openNode.isClickable());
            AccessibilityNodeInfo openNode = successNoticeNodes;
            Stage.getInstance().entering(Stage.OPENED_STAGE);
            openNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            Log.e(Tag, "拆红包");
            try {
                Thread.sleep(500);
            } catch (Exception e) {

            }
            return 0;
        } else {
            try {
                Thread.sleep(5);
            } catch (Exception e) {

            }
            Log.e(Tag, "正在打开");
            Stage.getInstance().entering(Stage.OPENING_STAGE);
            ttl += 1;
            return -1;
        }
    }

    /**
     * 检测“删除”，并删除红包
     *
     * @param nodeInfo
     * @return
     */
    private boolean deleteHongbao(AccessibilityNodeInfo nodeInfo) {
        if (nodeInfo == null) return false;
        /* 删除 */
        List<AccessibilityNodeInfo> successNoticeNodes = nodeInfo.findAccessibilityNodeInfosByText("删除");
        if (!successNoticeNodes.isEmpty()) {
            AccessibilityNodeInfo deleteNode = successNoticeNodes.get(successNoticeNodes.size() - 1);
            Log.e(Tag, "deleteHongbao.size():" + successNoticeNodes.size());
            Log.e(Tag, "deleteNode:" + deleteNode.isClickable());
            if (deleteNode.getParent() != null && deleteNode.getText() != null && deleteNode.getText().equals("删除")) {
                if (deleteNode.getParent().getPackageName().equals("com.tencent.mm") && deleteNode.getParent().getClassName().equals("android.widget.LinearLayout")) {
                    Log.e(Tag, "點擊刪除");
                    deleteNode.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    flag = false;
                    isHongbaoAppOK = false;
                    if (isPrepare && nodesToFetch.size() == 0) {
                        if (performGlobalAction(GLOBAL_ACTION_RECENTS)) {
                            clean();
                        }
                        isPrepare = false;
                    }
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 通过长按消息，检测是否有“删除”，判定红包软件是否可用
     *
     * @param nodeInfo
     * @return
     */
    private boolean isHongbaoOK(AccessibilityNodeInfo nodeInfo) {
        if (nodeInfo == null) return false;
        /* 删除 */
        List<AccessibilityNodeInfo> successNoticeNodes = nodeInfo.findAccessibilityNodeInfosByText("删除");
        if (!successNoticeNodes.isEmpty()) {
            AccessibilityNodeInfo deleteNode = successNoticeNodes.get(successNoticeNodes.size() - 1);
            Log.e(Tag, "deleteHongbao.size():" + successNoticeNodes.size());
            Log.e(Tag, "deleteNode:" + deleteNode.isClickable());
            if (deleteNode != null && deleteNode.getParent() != null && deleteNode.getText() != null && deleteNode.getText().equals("删除")) {
                if (deleteNode.getParent().getPackageName().equals("com.tencent.mm") && deleteNode.getParent().getClassName().equals("android.widget.LinearLayout")) {
                    Toast.makeText(this, "软件开启成功", Toast.LENGTH_SHORT).show();
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 获取节点对象唯一的id，通过正则表达式匹配
     * AccessibilityNodeInfo@后的十六进制数字
     *
     * @param node AccessibilityNodeInfo对象
     * @return id字符串
     */
    private String getNodeId(AccessibilityNodeInfo node) {
        /* 用正则表达式匹配节点Object */
        Pattern objHashPattern = Pattern.compile("(?<=@)[0-9|a-z]+(?=;)");
        Matcher objHashMatcher = objHashPattern.matcher(node.toString());

        // AccessibilityNodeInfo必然有且只有一次匹配，因此不再作判断
        objHashMatcher.find();

        return objHashMatcher.group(0);
    }

    /**
     * 将节点对象的id和红包上的内容合并
     * 用于表示一个唯一的红包
     *
     * @param node 任意对象
     * @return 红包标识字符串
     */
    private String getHongbaoHash(AccessibilityNodeInfo node) {
        /* 获取红包上的文本 */
        String content;
        try {
            AccessibilityNodeInfo i = node.getParent().getChild(0);
            content = i.getText().toString();
        } catch (NullPointerException npr) {
            return null;
        }

        return content + "@" + getNodeId(node);
    }

    @Override
    public void onInterrupt() {

    }

    /**
     * 检查是否要从红包详情页面退出
     *
     * @param nodeInfo
     */
    private void checkBackFromHongbaoPage(AccessibilityNodeInfo nodeInfo) {
        if (nodeInfo != null) {
            List<AccessibilityNodeInfo> successNodes = nodeInfo.findAccessibilityNodeInfosByText("红包详情");
//            List<AccessibilityNodeInfo> successNodes2 = nodeInfo.findAccessibilityNodeInfosByText("微信安全支付");
            if (!successNodes.isEmpty()) {
//                Log.e(Tag, "successNodes.get(0).getClassName():" + successNodes.get(0).getClassName());
//                Log.e(Tag, "successNodes.size:" + successNodes.size());

                for (int i = 0; i < successNodes.size(); i++) {
                    if (successNodes.get(i).getParent() != null && successNodes.get(i).getParent().getChildCount() == 3 && successNodes.get(i).getParent().getChild(2).getText().equals("微信安全支付")) {
//                        Log.e(Tag, "successNodes.get(i).getParent().getChildCount():" + successNodes.get(i).getParent().getChildCount());
//                        Log.e(Tag, "successNodes.get(i).getParent().getChild1:" + successNodes.get(i).getParent().getChild(0).getText());
//                        Log.e(Tag, "successNodes.get(i).getParent().getChild2:" + successNodes.get(i).getParent().getChild(1).getText());
//                        Log.e(Tag, "successNodes.get(i).getParent().getChild3:" + successNodes.get(i).getParent().getChild(2).getText());
                        Stage.getInstance().entering(Stage.DELETING_STAGE);
                        Log.e(Tag, "卡在详情界面，回退");
//                        Toast.makeText(this, "插件有异常，请见谅",Toast.LENGTH_SHORT).show();
                        performMyGlobalAction(GLOBAL_ACTION_BACK);
                    }
//                    successNodes.get(i).getParent();
                }
//                if (successNodes.get(0).getParent().equals(successNodes2.get(0).getParent())) {
//
//                }
            }
        }

    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public void performMyGlobalAction(int action) {
        Stage.getInstance().mutex = false;
        performGlobalAction(action);
    }


    /**
     * 点亮屏幕
     */
    private void lightScreen() {
        if (pm == null) {
            pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        }
        Log.e(Tag, "lightScreen()");
        lock = pm.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.SCREEN_BRIGHT_WAKE_LOCK, TAG);
        lock.acquire();
    }

    /**
     * 解锁
     */
    private void unLock() {
        if (kManager == null) {
            kManager = ((KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE));
        }
        Log.e(Tag, "unLock()");
        kLock = kManager.newKeyguardLock(TAG);
        kLock.disableKeyguard();
    }

    /**
     * 清理环境
     */
    private void clean() {

        Log.e(Tag, "clean()");
        if (kLock != null) {
            kLock.reenableKeyguard();
            kLock = null;
        }
        if (lock != null) {
            lock.release();
            lock = null;
        }
    }

    /**
     * 判断是否加了安全锁
     *
     * @return
     */
    private boolean isLockOn() {
        KeyguardManager kM = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        if (kM != null) {
            if (kM.isKeyguardLocked()) { // && kM.isKeyguardSecure()) {
                return true;
            }
        }

        return false;
    }

    /**
     * 判断屏幕是否亮
     *
     * @param context the context
     * @return true when (at least one) screen is on
     */
    public boolean isScreenOn(Context context) {
//        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
//            DisplayManager dm = (DisplayManager) context
//                    .getSystemService(Context.DISPLAY_SERVICE);
//            boolean screenOn = false;
//            for (Display display : dm.getDisplays()) {
//                if (display.getState() != Display.STATE_OFF) {
//                    screenOn = true;
//                }
//            }
//            return screenOn;
//        } else {
        PowerManager pm = (PowerManager) context
                .getSystemService(Context.POWER_SERVICE);
        // noinspection deprecation
        return pm.isScreenOn();
//        }
    }

    /**
     * 检查聊天列表界面是否有“【微信红包】”，有就点进去
     *
     * @param nodeInfo
     * @return
     */
    private boolean checkList(AccessibilityNodeInfo nodeInfo) {
        if (nodeInfo == null) {
            return false;
        }
        List<AccessibilityNodeInfo> nodes = nodeInfo.findAccessibilityNodeInfosByText(NOTIFICATION_TIP);
        if (!nodes.isEmpty()) {
            AccessibilityNodeInfo nodeToClick = nodes.get(0);
            CharSequence contentDescription = nodeToClick.getContentDescription();
//            Log.e(Tag,"contentDescription:"+contentDescription);
//            Log.e(Tag,"lastContentDescription:"+lastContentDescription);
            if (contentDescription != null && nodeToClick.isClickable()/*&& !lastContentDescription.equals(contentDescription)*/) {
                nodeToClick.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                Log.e(Tag, "checkList->>ACTION_CLICK");
//                Log.e(Tag, "fetchHongbao: "+ NOTIFICATION_TIP);
//                lastContentDescription = contentDescription.toString();
                return true;
            }
        }
        return false;
    }
}