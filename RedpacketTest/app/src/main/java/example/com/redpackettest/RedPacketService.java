package example.com.redpackettest;

import android.accessibilityservice.AccessibilityService;

import android.accessibilityservice.GestureDescription;
import android.app.KeyguardManager;

import android.app.Notification;

import android.app.PendingIntent;


import android.content.Context;

import android.content.Intent;

import android.os.Handler;

import android.os.PowerManager;


import android.os.SystemClock;
import android.text.LoginFilter;
import android.text.TextUtils;

import android.util.Log;

import android.view.accessibility.AccessibilityEvent;

import android.view.accessibility.AccessibilityNodeInfo;

import android.widget.Toast;



import java.util.List;

import static android.R.id.accessibilityActionContextClick;
import static android.R.id.list;


/**

 * 抢红包Service,继承AccessibilityService

 */

public class RedPacketService extends AccessibilityService {

    /**

     * 微信几个页面的包名+地址。用于判断在哪个页面 LAUCHER-微信聊天界面，LUCKEY_MONEY_RECEIVER-点击红包弹出的界面

     */

    private String LAUCHER = "com.tencent.mm.ui.LauncherUI";

    private String LUCKEY_MONEY_DETAIL = "com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyDetailUI";

    private String LUCKEY_MONEY_RECEIVER = "com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyNotHookReceiveUI";

    public static String passwords="199581";


    private KeyguardManager km;

    private PowerManager pm;

    private boolean finishFlag = false;

    private long startTime;

    private boolean found;

    @Override

    public void onAccessibilityEvent(AccessibilityEvent event) {

        if (event != null) {
            int eventType = event.getEventType();
            switch (eventType) {
                case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:        //通知栏变化触发
                    startTime = System.currentTimeMillis();

                    List<CharSequence> texts = event.getText();                //获取通知具体信息
                    for (CharSequence text : texts) {
                        String content = text.toString();
                        if (!TextUtils.isEmpty(content)) {
                            if (content.contains("微信红包")) {
                                openWeChatPage(event);
                            }
                        }
                    }
                    break;
                case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:         //窗口变化触发
                    String className = event.getClassName().toString();
                    Log.d("state", className);
                    if (LAUCHER.equals(className)) {                         //微信聊天页面

                        findRedPacket();
                        Log.d("state", "找红包");
                    }
                    if (LUCKEY_MONEY_RECEIVER.equals(className)) {          //红包页面（未打开）

                        openRedPacket();
                        Log.d("state", "开红包");
                    }
                    if (LUCKEY_MONEY_DETAIL.equals(className)){
                        long finishTime = System.currentTimeMillis();
                        Log.d("time","time"+(startTime-finishTime));
                        if (finishFlag)
                        {
                            new Thread(){
                                @Override
                                public void run() {
                                    slowExcute(2000);
                                    back2Home();
                                    finishFlag=false;
                                }
                            }.start();
                        }

                    }
            }

        }
    }


    private void openRedPacket() {
        new Thread() {                              //开启子线程进行延迟操作，防止代码执行速度过快，系统切换活动需要时间，界面根元素无法获取
            @Override
            public void run() {
                slowExcute(300);
                AccessibilityNodeInfo rootNode = getRootInActiveWindow();
                if (rootNode == null) {
                    Log.d("state", "rootnode is null");
                } else {
                    List<AccessibilityNodeInfo> nodeInfoList = rootNode.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/den");
                    if (nodeInfoList != null && nodeInfoList.size() > 0) {
                        Log.d("state", "开始执行");
                        nodeInfoList.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);

                    } else{
                        Log.d("state","红包已被领取完");
                        List<AccessibilityNodeInfo> nodeInfos = rootNode.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/dem");
                        if (nodeInfos!=null && nodeInfos.size()>0){
                            nodeInfos.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            back2Home();
                        }
                    }
                }
            }
        }.start();

    }

    private void findRedPacket() {
        found=true;
        new Thread(){
            @Override
            public void run() {
                Log.d("find","find");
                slowExcute(250);
                AccessibilityNodeInfo rootNode = getRootInActiveWindow();
                if (rootNode==null){
                    Log.d("state","rootnode is null");
                }
                else {
                    List<AccessibilityNodeInfo> nodeInfoList = rootNode.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/al7");
                    if (nodeInfoList!=null && nodeInfoList.size()>0){

                        for (AccessibilityNodeInfo nodeInfo : nodeInfoList){
                            if (nodeInfo.getChildCount()==2){
                                nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                                finishFlag = true;
                                break;
                            }
                        }
                    }
                }
            }
        }.start();
    }



    private void Unlock(){

        new Thread(){
            @Override
            public void run() {
                slowExcute(700);
                android.graphics.Path path =new android.graphics.Path();
                path.moveTo(540,700);
                path.lineTo(540,50);
                GestureDescription.Builder builder = new GestureDescription.Builder();
                GestureDescription gestureDescription = builder.addStroke(new GestureDescription.StrokeDescription(path,0,600)).build();
                dispatchGesture(gestureDescription,null,null);    //上滑
                Log.d("state","滑动");
                slowExcute(500);   //等待上滑后界面所需时间
                AccessibilityNodeInfo rootNode = getRootInActiveWindow();
                found=false;
                for (int i=0;i<=5;i++)
                {
                    List<AccessibilityNodeInfo> nodeInfoList = rootNode.findAccessibilityNodeInfosByViewId("com.android.systemui:id/key"+passwords.charAt(i));
                    if (nodeInfoList!=null && nodeInfoList.size()>0){
                        nodeInfoList.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    }
                    else Log.d("state","null");
                    slowExcute(20);      //每输入一个数字间隔时间
                }
                slowExcute(300);
                if(!found){
                    findRedPacket();
                }
            }
        }.start();
    }

    private void openWeChatPage(final AccessibilityEvent event) {

        if (event.getParcelableData() != null && event.getParcelableData() instanceof Notification) {     //当来通知时，获取第一手信息
            Notification notification = (Notification) event.getParcelableData();
            final PendingIntent pendingIntent = notification.contentIntent;
            sendPdIntent(pendingIntent);

        km = (KeyguardManager)getSystemService(Context.KEYGUARD_SERVICE);
        pm = (PowerManager)getSystemService(Context.POWER_SERVICE);    //电源管理

        if (!pm.isInteractive()){                //黑屏状态（不可交互）
            PowerManager.WakeLock wakeLock = pm.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "bright");
            wakeLock.acquire(4000);                  //触发点亮屏幕
            wakeLock.release();
            Unlock();
        }

        else if (km.inKeyguardRestrictedInputMode()){   //亮屏幕未解锁状态
            Unlock();
        }

        }
    }
    private void slowExcute(final long time){
        try {
            Thread.sleep(time);
        }
        catch (InterruptedException e){
            e.printStackTrace();
        }
    }

    private void sendPdIntent(PendingIntent pendingIntent){
        try {
            pendingIntent.send();
            Log.d("state", "pendingIntent 执行");
        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onServiceConnected() {
        Toast.makeText(this, "抢红包服务开启", Toast.LENGTH_SHORT).show();
        super.onServiceConnected();

    }

    @Override

    public void onInterrupt() {
        Toast.makeText(this, "我快被终结了啊-----", Toast.LENGTH_SHORT).show();
    }

    @Override

    public boolean onUnbind(Intent intent) {

        Toast.makeText(this, "抢红包服务已被关闭", Toast.LENGTH_SHORT).show();
        return super.onUnbind(intent);
    }

    private void back2Home() {
        Intent home=new Intent(Intent.ACTION_MAIN);
        home.addCategory(Intent.CATEGORY_HOME);
        startActivity(home);

    }
    }


