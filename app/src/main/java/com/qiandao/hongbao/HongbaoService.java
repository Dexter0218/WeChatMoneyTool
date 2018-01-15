package com.qiandao.hongbao;

import android.accessibilityservice.AccessibilityService;
import android.annotation.TargetApi;
import android.app.KeyguardManager;
import android.app.Notification;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Parcelable;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import com.qiandao.hongbao.util.ScreenUtil;
import com.qiandao.hongbao.util.Validator;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by ZhongyiTong on 9/30/15.
 * <p/>
 * 抢红包主要的逻辑部分
 */
public class HongbaoService extends AccessibilityService implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static String TAG = "HongbaoService";
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
    private static final int MAX_TTL = 100;

    private boolean flag = false;
    /**
     * 尝试次数
     */
    private int ttl = 0;
    private final static String NOTIFICATION_TIP = "[微信红包]";
    private final static String ZHIFUBAO_HONGHAO_TIP="人人可领，领完就能用。祝大家领取的红包金额大大大！#吱口令#长按复制此消息，打开支付宝就能领取！fbQfV839B2 ";
    AccessibilityNodeInfo mCurrentNode;

    //是否进行了亮屏解锁操作
    private boolean isPrepare = false;
    //红包软件是否可用
    private boolean isHongbaoAppOK = false;
    private SharedPreferences sharedPreferences;
    private ClipboardManager cm;
    private long timeOld;
    private long timeNew;

    @Override
    public void onCreate() {
        super.onCreate();
        cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        timeOld = System.currentTimeMillis();
    }

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
        Log.d(TAG, "event:" + event.getEventType());
        if (event.getEventType() == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
            String tip = event.getText().toString();
            Log.i(TAG, "onAccessibilityEvent: tip" + tip);
            if (!tip.contains(NOTIFICATION_TIP) || isPersonalTailor(tip)) {
                return;
            }

            if (!ScreenUtil.isScreenOn(this)) {
                if (!StatusValue.getInstance().isSupportBlackSreen()) return;
                ScreenUtil.lightScreen(this);
                isPrepare = true;
            } else {
                isPrepare = false;
            }
            if (ScreenUtil.isLockOn(this)) {
                ScreenUtil.unLock(this);
                isPrepare = true;
            } else {
                isPrepare = isPrepare || false;
            }
            Parcelable parcelable = event.getParcelableData();
            if (parcelable instanceof Notification) {
                Notification notification = (Notification) parcelable;
                try {
                    if (Stage.getInstance().getCurrentStage() == Stage.FETCHED_STAGE) {
                        Log.i(TAG, "Stage.FETCHED_STAGE_>send()");
                        notification.contentIntent.send();
                    } else if (Stage.getInstance().getCurrentStage() != Stage.OPENING_STAGE) {
                        Stage.getInstance().entering(Stage.FETCHED_STAGE);
                        Log.i(TAG, "呵呵" + Stage.getInstance().getCurrentStage());
                        notification.contentIntent.send();
                    }

                } catch (Exception e) {
                    Log.w(TAG, "PendingIntent.CanceledException", e);
                }
            }
            return;
        }

        if (Stage.getInstance().mutex) {
            return;
        }
        Stage.getInstance().mutex = true;

        try {
            handleWindowChange(event.getSource());
        } finally {
            Stage.getInstance().mutex = false;
        }

    }


    /**
     * 鉴别是否私人定制的红包
     *
     * @param str
     * @return
     */
    private boolean isPersonalTailor(String str) {
//        String[] match = {"专属", "定向"};
//        String value = PreferenceManager.getDefaultSharedPreferences(this).getString("pref_watch_exclude_words", "");
        String mExculdeWords = StatusValue.getInstance().getExculdeWords();
        if (!mExculdeWords.equals("")) {
            String[] words = mExculdeWords.split(" ");
            for (int i = 0; i < words.length; i++) {
                Log.i(TAG, "exculde words:" + words[i]);
                if (!words[i].equals("") && str.contains(words[i])) {
                    Log.i(TAG, "contains:" + words[i]);
                    return true;
                }
            }
        }
        Log.i(TAG, "No words match");
        return false;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void handleWindowChange(AccessibilityNodeInfo nodeInfo) {
        if (nodeInfo == null) return;
        timeNew = System.currentTimeMillis();
        if (Validator.isDaysChanged(timeOld, timeNew)) {
            cm.setText(ZHIFUBAO_HONGHAO_TIP);
            timeOld = timeNew;
        }

        switch (Stage.getInstance().getCurrentStage()) {
            case Stage.OPENING_STAGE:
                Log.d(TAG, "OPENING_STAGE");
                // 调试信息，打印TTL
                Log.d("TTL", String.valueOf(ttl));
                // 将文本内容放到系统剪贴板里。
                cm.setText(ZHIFUBAO_HONGHAO_TIP);
                int result = openHongbao(nodeInfo);
                /* 如果打开红包失败且还没到达最大尝试次数，重试 */
                if (result == -1 && ttl < MAX_TTL) {
                    return;
                } else if (result == 0) {
                    Log.i(TAG, "opened，to deleting");
                    //不自动拆时。如果在详情界面，就退出，下一步进入DELETING_STAGE，进行删除；
                    // 如果不在，则进行手动拆，下一步进入FETCHED_STAGE状态，。
                    if (!StatusValue.getInstance().isSupportAutoRob()) {
                        Log.i(TAG, "不拆2");
                        if (!checkBackFromHongbaoPage(nodeInfo)) {
                            Stage.getInstance().entering(Stage.FETCHED_STAGE);
                        }
                    } else {
                        if (StatusValue.getInstance().isSupportDelete()) {
                            Stage.getInstance().entering(Stage.DELETING_STAGE);
                        } else {
                            Stage.getInstance().entering(Stage.FETCHED_STAGE);
                        }
                        performMyGlobalAction(GLOBAL_ACTION_BACK);
                    }
                } else {
                    Log.i(TAG, "reOpen");
                    checkList(nodeInfo);
                    Stage.getInstance().entering(Stage.FETCHED_STAGE);
                }
                ttl = 0;
                if (nodesToFetch.size() == 0) handleWindowChange(nodeInfo);
                break;
            case Stage.OPENED_STAGE:
                Log.d(TAG, "OPENED_STAGE");
                List<AccessibilityNodeInfo> successNodes = nodeInfo.findAccessibilityNodeInfosByText("红包详情");
                if (successNodes.isEmpty() && ttl < MAX_TTL) {
                    ttl += 1;
                    return;
                }
                ttl = 0;
                isHongbaoAppOK = false;

                if (!StatusValue.getInstance().isSupportAutoRob()) {
                    Log.i(TAG, "不拆4");
                    if (!checkBackFromHongbaoPage(nodeInfo)) {
                        Stage.getInstance().entering(Stage.FETCHED_STAGE);
                    }
                } else {
                    if (StatusValue.getInstance().isSupportDelete()) {
                        Stage.getInstance().entering(Stage.DELETING_STAGE);
                    } else {
                        Stage.getInstance().entering(Stage.FETCHED_STAGE);
                    }
                    performMyGlobalAction(GLOBAL_ACTION_BACK);
                }
                break;
            case Stage.FETCHED_STAGE:
                Log.d(TAG, "FETCHED_STAGE");
                if (!isHongbaoAppOK) {
                    try {
                        isHongbaoAppOK = isHongbaoOK(nodeInfo);
                    } catch (Exception e) {
                        Log.w(TAG, "isHongbaoOK Exception");
                    }
                }
                Log.d(TAG, "nodesToFetch.size():" + nodesToFetch.size());
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
//                        Log.d("fetched", Arrays.toString(fetchedIdentifiers.toArray()));

                        Stage.getInstance().entering(Stage.OPENING_STAGE);
                        node.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);

                    }
                    return;
                }
                Stage.getInstance().entering(Stage.FETCHING_STAGE);
                Log.i(TAG, "before fetchHongbao");
                fetchHongbao(nodeInfo);
                Stage.getInstance().entering(Stage.FETCHED_STAGE);

                break;
            case Stage.DELETING_STAGE:
                Log.d(TAG, "DELETING_STAGE");
                checkBackFromHongbaoPage(nodeInfo);
                if (mCurrentNode != null && mCurrentNode.getParent() != null) {
                    mCurrentNode.getParent().performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK);
                    Log.i(TAG, "正在删除");
                    Stage.getInstance().entering(Stage.DELETED_STAGE);
                }
                break;
            case Stage.DELETED_STAGE:
                if (!deleteHongbao(nodeInfo)) {
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
        List<AccessibilityNodeInfo> fetchhongbao = nodeInfo.findAccessibilityNodeInfosByText(NOTIFICATION_TIP);
        Log.i(TAG, "检查红包：" + fetchhongbao);
        if (fetchNodes.isEmpty()) {
            if (!flag && isPrepare) {
                flag = checkList(nodeInfo);
            }
            Log.i(TAG, "fetchNodes找不到'领取红包'");
            return;
        }
        /*没找到就返回*/
        for (AccessibilityNodeInfo cellNode : fetchNodes) {
            if (cellNode.getParent() != null && cellNode.getParent().getClassName().equals("android.widget.LinearLayout")) {
                Log.i(TAG, "红包上的文字：" + cellNode.getParent().getChild(0).getText().toString());
                if (isPersonalTailor(cellNode.getParent().getChild(0).getText().toString()))
                    continue;
                nodesToFetch.add(cellNode);
            }

        }
    }


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
        if (nodeInfo == null) return -2;
        /* 戳开红包，红包已被抢完，遍历节点匹配“红包详情”、“手慢了”和“过期” */
        List<AccessibilityNodeInfo> failureNoticeNodes = new ArrayList<>();
        failureNoticeNodes.addAll(nodeInfo.findAccessibilityNodeInfosByText("红包详情"));
        failureNoticeNodes.addAll(nodeInfo.findAccessibilityNodeInfosByText("手慢了"));
        failureNoticeNodes.addAll(nodeInfo.findAccessibilityNodeInfosByText("过期"));
        failureNoticeNodes.addAll(nodeInfo.findAccessibilityNodeInfosByText("失效"));
        if (!failureNoticeNodes.isEmpty()) {
            Log.d(TAG, "点了拆，等待返回。。。");
            return 0;
        }
//        List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByViewId("@id/c22");
//        for (AccessibilityNodeInfo item : list) {
//            Log.d(TAG,"list.getClassName:"+item.getClassName());
//            item.performAction(AccessibilityNodeInfo.ACTION_CLICK);
//        }
        AccessibilityNodeInfo successNoticeNodes = findOpenButton(nodeInfo);

        if (successNoticeNodes != null) {
            Log.i(TAG, "successNoticeNodes:" + successNoticeNodes.getClassName());
        }
        if (successNoticeNodes != null && successNoticeNodes.getClassName().equals("android.widget.Button")) {
            final AccessibilityNodeInfo openNode = successNoticeNodes;
            Stage.getInstance().entering(Stage.OPENED_STAGE);
            int delayFlag = sharedPreferences.getInt("pref_open_delay", 0) * 500;


            new android.os.Handler().postDelayed(
                    new Runnable() {
                        public void run() {
                            try {
                                if (StatusValue.getInstance().isSupportAutoRob()) {
                                    openNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                                }
                                Log.i(TAG, "拆红包");

                            } catch (Exception e) {


                            }
                        }
                    },
                    delayFlag);
            if (checkBackFromHongbaoPage(openNode)) {
                return 0;
            } else {
                return -1;
            }
        } else {
            Log.i(TAG, "正在打开");
            Stage.getInstance().entering(Stage.OPENING_STAGE);
            ttl += 1;
            return -1;
        }
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    /**
     * 递归查找拆红包按钮
     */
    private AccessibilityNodeInfo findOpenButton(AccessibilityNodeInfo node) {
        Log.i(TAG, "findOpenButton");
        if (node == null)
            return null;

        //非layout元素
        if (node.getChildCount() == 0) {
            Log.d(TAG, "node.getClassName():" + node.getClassName());
            Log.d(TAG, "node.getText():" + node.getText());
            if ("android.widget.Button".equals(node.getClassName()))
                return node;
            else
                return null;
        }

        //layout元素，遍历找button
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo button = findOpenButton(node.getChild(i));
            if (button != null)
                return button;
        }
        return null;
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
            Log.i(TAG, "deleteHongbao.size():" + successNoticeNodes.size());
            if (deleteNode.getParent() != null && deleteNode.getText() != null && deleteNode.getText().toString().equals("删除") && deleteNode.getParent().getPackageName().equals("com.tencent.mm")) {
                Log.i(TAG, "找删除文字");
                if (deleteNode.getParent().getClassName().equals("android.widget.LinearLayout")) {
                    Log.i(TAG, "點擊刪除1");
                    deleteNode.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    deleteAndClean(deleteNode);
                    return true;
                } else if (deleteNode.getParent().getClassName().equals("android.widget.ListView")) {
                    Log.i(TAG, "點擊刪除2");
                    deleteNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    deleteAndClean(deleteNode);
                    return true;
                }
            }
        }
        return false;
    }

    private void deleteAndClean(AccessibilityNodeInfo deleteNode) {
        flag = false;
        isHongbaoAppOK = false;
        if (isPrepare && nodesToFetch.size() == 0) {
            if (performGlobalAction(GLOBAL_ACTION_RECENTS)) {
                ScreenUtil.clean();
            }
            isPrepare = false;
        }
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
        List<AccessibilityNodeInfo> noticeNodes = nodeInfo.findAccessibilityNodeInfosByText("删除");
        if (!noticeNodes.isEmpty()) {
            AccessibilityNodeInfo deleteNode = noticeNodes.get(noticeNodes.size() - 1);
            Log.i(TAG, "deleteHongbao.size():" + noticeNodes.size());
            Log.i(TAG, "deleteNode:" + deleteNode.isClickable());
            if (deleteNode != null && deleteNode.getParent() != null && deleteNode.getText() != null && deleteNode.getText().toString().equals("删除")) {
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
    private boolean checkBackFromHongbaoPage(AccessibilityNodeInfo nodeInfo) {
        if (nodeInfo != null) {
            Log.i(TAG, "checkBackFromHongbaoPage");
            List<AccessibilityNodeInfo> hongbaoDetailNodes = nodeInfo.findAccessibilityNodeInfosByText("红包详情");
//            List<AccessibilityNodeInfo> successNodes2 = nodeInfo.findAccessibilityNodeInfosByText("微信安全支付");
            if (!hongbaoDetailNodes.isEmpty()) {
                for (int i = 0; i < hongbaoDetailNodes.size(); i++) {
                    Log.i(TAG, "checkBackFromHongbaoPage_index:" + i);
                    if (hongbaoDetailNodes.get(i).getParent() != null) {
//                        Log.i(TAG, "hongbaoDetailNodes.get(i)..getParent().getChildCount():" + hongbaoDetailNodes.get(i).getParent().getChildCount());
//                        Log.i(TAG, "hongbaoDetailNodes.get(i).getParent().getChild(1.getText():" + hongbaoDetailNodes.get(i).getParent().getChild(1).getText());
//                        Log.i(TAG, "hongbaoDetailNodes.get(i).getParent().getChild(2).getText():" + hongbaoDetailNodes.get(i).getParent().getChild(2).getText());
                        if (hongbaoDetailNodes.get(i).getParent().getChildCount() == 3 && hongbaoDetailNodes.get(i).getParent().getChild(2).getText().toString().equals("红包详情")) {
                            if (StatusValue.getInstance().isSupportDelete()) {
                                Stage.getInstance().entering(Stage.DELETING_STAGE);
                            } else {
                                Stage.getInstance().entering(Stage.FETCHED_STAGE);
                            }
                            Log.w(TAG, "卡在详情界面，回退1");
                            performMyGlobalAction(GLOBAL_ACTION_BACK);
                            return true;
                        }
                        if (hongbaoDetailNodes.get(i).getParent().getChildCount() == 4) {
//                            Log.d(TAG, "hongbaoDetailNodes.get(i).getParent().getChild(1.getText():" + hongbaoDetailNodes.get(i).getParent().getChild(1).getText());
//                            Log.d(TAG, "hongbaoDetailNodes.get(i).getParent().getChild(2).getText():" + hongbaoDetailNodes.get(i).getParent().getChild(2).getText());
//                            Log.d(TAG, "hongbaoDetailNodes.get(i).getParent().getChild(3).getText():" + hongbaoDetailNodes.get(i).getParent().getChild(3).getText());
                            if (hongbaoDetailNodes.get(i).getParent().getChild(3).getText().equals("红包记录")) {
                                if (StatusValue.getInstance().isSupportDelete()) {
                                    Stage.getInstance().entering(Stage.DELETING_STAGE);
                                } else {
                                    Stage.getInstance().entering(Stage.FETCHED_STAGE);
                                }
                                Log.w(TAG, "卡在详情界面，回退2");
                                performMyGlobalAction(GLOBAL_ACTION_BACK);
                            }
                        }
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public void performMyGlobalAction(int action) {
        Stage.getInstance().mutex = false;
        performGlobalAction(action);
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
        Log.d(TAG, "checkList");
        Log.d(TAG, " nodeInfo:" + nodeInfo.toString());

        List<AccessibilityNodeInfo> mynodes = nodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/adh");
        if (!mynodes.isEmpty()) {
            for (int i = 0; i < mynodes.size(); i++) {
                Log.d(TAG, "checkList->>mynodes.get(i):" + mynodes.get(i).getText());
                if (mynodes.get(i) != null && mynodes.get(i).getText().toString().contains(NOTIFICATION_TIP)) {
                    Log.d(TAG, " checkList->>nodeName:" + nodeInfo.getClassName());
                    Log.d(TAG, " checkList->>node,getClassName:" + mynodes.get(i).getParent().getParent().getClassName());
                    Log.d(TAG, "checkList->> node,getChild(0):" + mynodes.get(i).getParent().getParent().getChild(0));
                    Log.d(TAG, " checkList->>node,isClickable:" + mynodes.get(i).getParent().getParent().getChild(0).isClickable());
                    AccessibilityNodeInfo nodeToClick = mynodes.get(i).getParent().getParent().getChild(0);
                    if (nodeToClick == null) return false;
                    if (nodeToClick.isClickable()) {
                        nodeToClick.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        Log.d(TAG, "checkList->>ACTION_CLICK");
//                Log.d(TAG, "fetchHongbao: "+ NOTIFICATION_TIP);
//                lastContentDescription = contentDescription.toString();
                        return true;
                    }
                }
            }
        }
//        List<AccessibilityNodeInfo> nodes =nodeInfo.findAccessibilityNodeInfosByText(NOTIFICATION_TIP);
//        if (!nodes.isEmpty()) {
//            for(int i=0;i<nodes.size();i++){
//                Log.d(TAG,"nodes.get(i):"+nodes.get(i));
//            }
//            AccessibilityNodeInfo nodeToClick = nodes.get(0);
//            if (nodeToClick == null) return false;
//            CharSequence contentDescription = nodeToClick.getContentDescription();
////            Log.d(TAG,"contentDescription:"+contentDescription);
////            Log.d(TAG,"lastContentDescription:"+lastContentDescription);
//            if (contentDescription != null && nodeToClick.isClickable()/*&& !lastContentDescription.equals(contentDescription)*/) {
//                nodeToClick.performAction(AccessibilityNodeInfo.ACTION_CLICK);
//                Log.d(TAG, "checkList->>ACTION_CLICK");
////                Log.d(TAG, "fetchHongbao: "+ NOTIFICATION_TIP);
////                lastContentDescription = contentDescription.toString();
//                return true;
//            }
//        }
        return false;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {

    }
}