package com.nearucenterplaza.redenvelopeassistant.service;

import java.util.ArrayList;
import java.util.List;

import com.crystal.momo.R;
import com.nearucenterplaza.redenvelopeassistant.ui.RedEnvelopeApplication;
import com.nearucenterplaza.redenvelopeassistant.ui.bean.User;
import com.nearucenterplaza.redenvelopeassistant.ui.db.UserDao;
import com.nearucenterplaza.redenvelopeassistant.ui.fragmant.WeChatFragment;
import com.nearucenterplaza.redenvelopeassistant.service.core.Notifier;
import com.nearucenterplaza.redenvelopeassistant.service.core.RedEnvelopeHelper;
import com.nearucenterplaza.redenvelopeassistant.service.core.SettingHelper;
import com.nearucenterplaza.redenvelopeassistant.service.core.SoundHelper;
import com.nearucenterplaza.redenvelopeassistant.utils.ActivityHelper;
import com.nearucenterplaza.redenvelopeassistant.utils.LogUtil;
import com.nearucenterplaza.redenvelopeassistant.utils.XLog;

import android.R.anim;
import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.TargetApi;
import android.app.Notification;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class WechatAccService extends AccessibilityService {

    private static final String TAG = WechatAccService.class.getSimpleName();
    
	public static void log(String message) {
		XLog.e("WechatAccService", message);
	} 
	
	private static final int MSG_NEAR_LIST = 0;
	private static final int MSG_PERSON_INFO = 1;
	private static final int MSG_CHAT = 2;
	private static final int MSG_SKIP_TO_NEAR = 3;
    private static final int MSG_CHECK_STEP = 4;
    private   boolean isfirst = true;
	private Handler mainHandler = new Handler(){
	    public void handleMessage(android.os.Message msg) {
	        switch (msg.what) {
	        case MSG_CHECK_STEP:
	        {
	            android.util.Log.i(TAG,"MSG_CHECK_STEP mStep=" + mStep);
	            if(mStep == 0 && mLastStep ==0)
	            {
	                android.util.Log.i(TAG,"MSG_CHECK_STEP >>>>>>>>>>> 0");
	                handleNearList(null);
	            }
	            else if(mStep == 3 && mLastStep ==2)
	            {
	                android.util.Log.i(TAG,"MSG_CHECK_STEP >>>>>>>>>>> 3");
	                //mainHandler.sendEmptyMessage(MSG_CHAT);
	            }
	            else if(mStep == 2 && mLastStep == 1)
	            {
	                android.util.Log.i(TAG,"MSG_CHECK_STEP >>>>>>>>>>> 2");
	                //mainHandler.sendEmptyMessage(MSG_PERSON_INFO);
	            }
	            
	            break;
	        }
	        case MSG_SKIP_TO_NEAR:
	        {
	        	break;
	        }
            case MSG_NEAR_LIST:
            {
            	//Toast.makeText(getApplicationContext(), "MSG_NEAR_LIST", Toast.LENGTH_SHORT).show();
                AccessibilityNodeInfo rowNode = (AccessibilityNodeInfo) msg.obj;
                handleNearList(rowNode);
                break;
            }
            case MSG_PERSON_INFO:
            {
                    android.util.Log.i(TAG,"NewFriendInfo:mStep=" + mStep);
                    AccessibilityNodeInfo nodeInfo_ = getRootInActiveWindow();
//                  if(rowNode !=null)
                    {
                        //List<AccessibilityNodeInfo> list=nodeInfo_.findAccessibilityNodeInfosByViewId("com.blackbean.cnmeach:id/ax1");
                        List<AccessibilityNodeInfo> list=nodeInfo_.findAccessibilityNodeInfosByText("聊天");
                        if(list != null && list.size()>0)
                        {
                            
                            for(int i=0;i<list.size();i++)
                            {
                                android.util.Log.i(TAG,"NewFriendInfo:clicked");
                                if(list.get(i).performAction(AccessibilityNodeInfo.ACTION_CLICK)
                                        || (list.get(i).getParent() != null && (list.get(i).getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK)))
                                        )
                                {
                                    mLastStep = 1;
                                    mStep = 2;
                                    mainHandler.sendEmptyMessageDelayed(MSG_CHECK_STEP, 1000);
                                }
                                else
                                {
                                    mainHandler.sendEmptyMessageDelayed(MSG_CHECK_STEP, 2000);
                                }
                            }
                            
                            
                        }
                        else
                        {
                            mainHandler.sendEmptyMessageDelayed(MSG_CHECK_STEP, 2000);
                        }
                    }
                break;   
            }
            case MSG_CHAT:
            {
                mLastStep = 2;
                mStep = 3;
                AccessibilityNodeInfo nodeInfo_ = getRootInActiveWindow();
                if(nodeInfo_!=null)
                {
                	
                	if(Build.VERSION.SDK_INT >=Build.VERSION_CODES.LOLLIPOP )//5.0
                	{
                		//a2y
    	                List<AccessibilityNodeInfo> list=nodeInfo_.findAccessibilityNodeInfosByViewId("com.blackbean.cnmeach:id/a2y");
    	                nodeInfo_.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
    	                if(list != null&& list.size() >0)
    	                {
    	                    SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    	                       Bundle arguments = new Bundle();
    	                       arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
    	                               sp.getString("content", "你好，很高兴认识你"));
    	                       list.get(0).performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
    	                       list.get(0).performAction(AccessibilityNodeInfo.ACTION_FOCUS);
    	                       list.get(0).recycle();
    	                }
    	                //send
    	                List<AccessibilityNodeInfo> sendbtn=nodeInfo_.findAccessibilityNodeInfosByViewId("com.blackbean.cnmeach:id/a33");
    	                if(sendbtn != null&& sendbtn.size() >0)
    	                {
    	                        sendbtn.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
    	                        sendbtn.get(0).recycle();
    	                }
                	}
                	else//4.0
                	{
    	                AccessibilityNodeInfo inputnode = nodeInfo_.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
    	                if(inputnode != null)
    	                {
    	                	 SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    	                	ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
    	                	ClipData clip = ClipData.newPlainText("text", sp.getString("content", "你好，很高兴认识你"));
    	                	clipboard.setPrimaryClip(clip);
    	                	//焦点    （n是AccessibilityNodeInfo对象）
    	                	inputnode.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
    	                	//粘贴进入内容
    	                	inputnode.performAction(AccessibilityNodeInfo.ACTION_PASTE);
    	                }
    	                //send
    	                List<AccessibilityNodeInfo> sendbtn=nodeInfo_.findAccessibilityNodeInfosByText("发送");
    	                if(sendbtn != null&& sendbtn.size() >0)
    	                {
    	                        sendbtn.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
    	                        sendbtn.get(0).recycle();
    	                }
                	}
	                
                }
                //back
                new Thread(
                        new Runnable() {
                            
                            @Override
                            public void run() {
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                performGlobalAction(GLOBAL_ACTION_BACK);
                            }
                        }
                        ).start();
                break;
            }
            
            default:
                break;
            }
	    };
	};
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onServiceConnected() {
		AccessibilityServiceInfo accessibilityServiceInfo = getServiceInfo();
		if (accessibilityServiceInfo == null)
			accessibilityServiceInfo = new AccessibilityServiceInfo();
		accessibilityServiceInfo.eventTypes = AccessibilityEvent.TYPES_ALL_MASK;
		accessibilityServiceInfo.flags |= AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;
		accessibilityServiceInfo.packageNames = new String[] { WeChatFragment.WECHAT_PACKAGENAME };
		accessibilityServiceInfo.feedbackType = AccessibilityServiceInfo.FEEDBACK_ALL_MASK;
		accessibilityServiceInfo.notificationTimeout = 10;
		setServiceInfo(accessibilityServiceInfo);
		// 4.0之后可通过xml进行配置,以下加入到Service里面
		/*
		 * <meta-data android:name="android.accessibilityservice"
		 * android:resource="@xml/accessibility" />
		 */
		isfirst = true;
		 android.util.Log.i(TAG,"onServiceConnected");
		Notifier.getInstance().notify(this,getString(R.string.app_name), getString(R.string.wechat_acc_service_start_notification), getString(R.string.wechat_acc_service_start_notification),
				Notifier.TYPE_WECHAT_SERVICE_RUNNING, false);
		mNearList = null;
	}

	
	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	@Override
	public void onAccessibilityEvent(AccessibilityEvent event) {
		if (event == null)
		{
		    android.util.Log.i(TAG,"event == null");
			return;
		}
		android.util.Log.i(TAG,"onAccessibilityEvent type"+event.getEventType());
		AccessibilityNodeInfo nodeInfo = event.getSource();
		if (nodeInfo == null) {
		    android.util.Log.i(TAG,"nodeInfo == null");
		    return;
		}
		AccessibilityNodeInfo rowNode = nodeInfo;// we can also use getRootInActiveWindow() instead;
		if (rowNode == null) {
			log( "noteInfo is　null");
			return;
		}
		LogUtil.d("eventtype:" + event.getEventType());
		// String currentActivityName =
		// ActivityHelper.getTopActivityName(RedEnvelopeApplication.getInstance());
		if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
				|| event.getEventType() == AccessibilityEvent.TYPE_VIEW_SCROLLED
				|| event.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
				) 
		{
			CharSequence currentClassName = event.getClassName();
			android.util.Log.i(TAG,"currentClassName:" + currentClassName);
			//near people page
//			com.blackbean.cnmeach.module.look.NewLookActivity
			if("com.blackbean.cnmeach.module.home.MainActivity".equals(currentClassName))
			{
				isfirst = true;
			        mNearList = null;
				 	List<AccessibilityNodeInfo> list=rowNode.findAccessibilityNodeInfosByText("发现");
	                if(list!=null && list.size()>0)
	                {
	                    android.util.Log.i(TAG,"发现:" + currentClassName);
	                    list.get(0).getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
	                    list.get(0).getParent().recycle();
	                }
	                try {
						Thread.sleep(200);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
	                list=rowNode.findAccessibilityNodeInfosByText("附近的人");
	                if(list!=null && list.size()>0)
	                {
	                    android.util.Log.i(TAG,"附近的人:" + currentClassName);
	                    list.get(0).getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
	                    list.get(0).getParent().recycle();
	                }
			}
			else if("com.blackbean.cnmeach.module.look.NewLookActivity".equals(currentClassName)
			        )
			{
			    Message msg = new Message();
			    msg.what = MSG_NEAR_LIST;
			    msg.obj = rowNode;
			    if(isfirst)
			    {
			    	isfirst = false;
			    	mainHandler.sendMessageDelayed(msg, 6000);
			    }
			    else
			    {
			    	mainHandler.sendMessageDelayed(msg, 3000);
			    }
			    
				
			}
			//new friend info page
			else if("com.blackbean.cnmeach.module.personalinfo.NewFriendInfo".equals(currentClassName)){
			    if(mStep == 1)
                {
                    Message msg = new Message();
                    msg.what = MSG_PERSON_INFO;
                    msg.obj = rowNode;
                    mainHandler.sendMessageDelayed(msg, 2000);
                } 
			    else
                {
                    //back
			        new Thread(
	                        new Runnable() {
	                            
	                            @Override
	                            public void run() {
	                                try {
	                                    Thread.sleep(1000);
	                                } catch (InterruptedException e) {
	                                    e.printStackTrace();
	                                }
	                                performGlobalAction(GLOBAL_ACTION_BACK);
	                            }
	                        }
	                        ).start();
                }
			}
			//chat page
			else if("com.blackbean.cnmeach.module.chat.ChatMain".equals(currentClassName))
			{
			    android.util.Log.i(TAG,"ChatMain:mStep=" + mStep);
			    if(mStep == 2)
			    {
			        Message msg = new Message();
                    msg.what = MSG_CHAT;
                    msg.obj = rowNode;
                    mainHandler.sendMessageDelayed(msg, 2000);
			    }
			}
			
		}
	}
	
	
	
	//1:handle near people list 
	private List<AccessibilityNodeInfo>  mNearList = null;
	private AccessibilityNodeInfo mlistview = null;
    private int mCurrIndex = 0;
    private int mStep = 0;//0,1,2
    private int mLastStep = 0;
    private boolean isMore = false;
	public void handleNearList(AccessibilityNodeInfo rowNode)
	{
	    mStep = 0;
	    mLastStep = 0;
	    if(mNearList == null)
	    {
	        AccessibilityNodeInfo nodeInfo_ = getRootInActiveWindow();
//            List<AccessibilityNodeInfo> list_ = nodeInfo_.findAccessibilityNodeInfosByViewId("com.blackbean.cnmeach:id/b0x");//bk0//nodeInfo_.findAccessibilityNodeInfosByText("岁");
	        List<AccessibilityNodeInfo> list_ = nodeInfo_.findAccessibilityNodeInfosByText("岁");
	        android.util.Log.i(TAG,"handleNearList ing...km.."+list_.size());
            mNearList = list_;
	    }
	    
	    if(mNearList != null && mNearList.size() > mCurrIndex)
	    {
	        String username = "";
	        //query from db
	        boolean isexist = new UserDao(getApplicationContext()).getByName(username);
	        if(isexist)
	        {
	            ++mCurrIndex;//skip to next
	            
	            return ;
	        }
	        else
	        {
	            //add to db
	            User u = new User();
	            u.setName(username);
	            new UserDao(getApplicationContext()).add(u);
	        }
	        
	        mStep = 1;
	        android.util.Log.i(TAG,"handleNearList mNearList click index= "+mCurrIndex);
	        mNearList.get(mCurrIndex).performAction(AccessibilityNodeInfo.ACTION_CLICK);
	        if(mNearList.get(mCurrIndex).getParent() != null)
	        mNearList.get(mCurrIndex).getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
	        ++mCurrIndex;
	    }
	    else if(mNearList == null || mNearList.size() == 0)
	    {
	        //
	        mainHandler.sendEmptyMessageDelayed(MSG_CHECK_STEP, 2000);
	    }
	    else if(mNearList != null && mNearList.size() <= mCurrIndex)
	    {
	       
	    	if(isMore)
	    	{
	    		isMore = false;
	    		AccessibilityNodeInfo nodeInfo_ = getRootInActiveWindow();
                List<AccessibilityNodeInfo> list_ = nodeInfo_.findAccessibilityNodeInfosByText("岁");
                android.util.Log.i(TAG, "列表人数: "+list_.size());
                mNearList = list_;
                handleNearList(null);
                return;
	    	}
	    	
	        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
	        //判断是否有更多
	        List<AccessibilityNodeInfo> list_ = nodeInfo.findAccessibilityNodeInfosByText("点击查看更多");
	        if(list_ !=null && list_.size()>0)
	        {
	        	for(int i=0;i<list_.size();i++)
	        	{
	        		list_.get(i).performAction(AccessibilityNodeInfo.ACTION_CLICK);
	        		isMore = true;
	        	}
	        	
	        }
	        else
	        {
	        
            for (int i = 0; i < nodeInfo.getChildCount(); i++)
            {
                if (nodeInfo.getChild(i).getClassName().equals("android.widget.ListView"))
                {
                    AccessibilityNodeInfo node_lsv = nodeInfo.getChild(i);
                    node_lsv.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
                    
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                        try {
                         Thread.sleep(3000);
                        } catch (InterruptedException mE) {
                         mE.printStackTrace();
                        }
                        AccessibilityNodeInfo nodeInfo_ = getRootInActiveWindow();
                        List<AccessibilityNodeInfo> list_ = nodeInfo_.findAccessibilityNodeInfosByText("岁");
                        android.util.Log.i(TAG, "列表人数: "+list_.size());
                        mNearList = list_;
                        //滑动之后，上一页的最后一个item为当前的第一个item，所以从第二个开始打招呼
                        mCurrIndex = 1;
                        handleNearList(null);
                        }
                       }).start();
                }  
            
            }
	        }
            
            }
	        
	}
	/** handle notification notice */
	@TargetApi(Build.VERSION_CODES.KITKAT)
	public void handleNotificationChange(AccessibilityEvent event) {
		log( "handleNotificationChange eventtype:" + event.getEventType());
		if (event == null)
			return;
		
		if (!(event.getEventType() == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED)) {
			return;
		}
		if (event.getParcelableData() instanceof Notification) {
			Notification notification = (Notification) event
					.getParcelableData();
			if (notification.tickerText != null
					&& notification.tickerText.toString().contains(getString(R.string.wechat_acc_service_red_envelope_notification_identification))) {
				log("来红包啦 get red envelope message");
				RedEnvelopeHelper.openNotification(event);
			}
		}
	}


	public void handleChatPage(AccessibilityNodeInfo node) {
		if (node == null)
			return;
		LogUtil.d("handleChatPage");
		if(android.os.Build.VERSION.SDK_INT>=Build.VERSION_CODES.KITKAT){
			 AccessibilityNodeInfo tempNode=RedEnvelopeHelper.getLastWechatRedEnvelopeNodeById(node);
			 if(tempNode!=null){
				 tempNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
				 tempNode.recycle();
			 }
		}else if(android.os.Build.VERSION.SDK_INT>=Build.VERSION_CODES.JELLY_BEAN){
			 AccessibilityNodeInfo tempNode=RedEnvelopeHelper.getLastWechatRedEnvelopeNodeByText(node,this);
			 if(tempNode!=null){
				 tempNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
				 tempNode.recycle();
			 }
		}
	}

	public void handleLuckyMoneyReceivePage(AccessibilityNodeInfo node) {
		if (node == null)
			return;
		LogUtil.d("handleLuckyMoneyReceivePage");
		AccessibilityNodeInfo nodeDetail = RedEnvelopeHelper.getWechatRedEnvelopeOpenDetailNode(node);
		LogUtil.d("nodeDetail="+nodeDetail);
		if (nodeDetail != null) {// the red envelope already opened
									// 红包已经被打开
			if (SettingHelper.getREAutoMode())
				ActivityHelper.goHome(this);
		} else {
			AccessibilityNodeInfo nodeOpen = RedEnvelopeHelper.getWechatRedEnvelopeOpenNode(node);
			LogUtil.d("nodeOpen="+nodeOpen);
			if (nodeOpen != null) {
				nodeOpen.performAction(AccessibilityNodeInfo.ACTION_CLICK);
				nodeOpen.recycle();
			} else {// this page is loading red envelope data, no action

			}
		}
	}

	public void handleLuckyMoneyDetailPage(AccessibilityNodeInfo node) {
		if (node == null)
			return;
		ActivityHelper.goHome(this);
	}

	

	@Override
	public void onInterrupt() {
		log("onInterrupt");
	}

	public void onDestroy() {
		super.onDestroy();
		Notifier.getInstance().cancelByType(
				Notifier.TYPE_WECHAT_SERVICE_RUNNING);
	}

}
