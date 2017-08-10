package com.nearucenterplaza.redenvelopeassistant.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import com.crystal.momo.R;
import com.nearucenterplaza.redenvelopeassistant.service.core.Notifier;
import com.nearucenterplaza.redenvelopeassistant.ui.fragmant.WeChatFragment;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.SuppressLint;
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
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

public class MonoService extends AccessibilityService {

    private static final String TAG = MonoService.class.getSimpleName();
    private List<AccessibilityNodeInfo>  mOnlineList = null;
    private List<AccessibilityNodeInfo>  mCityList = null;
    
    private static final int NODE_INDEX_MSG = 10;//消息
    private static final int NODE_INDEX_ZHAOHU = 15;//打招呼
    private static final int NODE_INDEX_NEARLIST = 20;//附近人
    private static final int NODE_INDEX_ZHAOHULIST = 25;
    private static final int NODE_INDEX_MSGLIST = 30;//消息列表
    
    
    private boolean isChatPage = false;
    private int mCurrentNodeIndex = NODE_INDEX_ZHAOHU;//默认打招呼
    
    private boolean isDebuging = true;
    private String[] biaodianstrs = {"~~","$","?","o","*"};
	private String[] contentstrs = null;
    //msg
    private static final int MSG_ONLINE_LIST = 0;
    private static final int MSG_PERSON_INFO = 1;
    private static final int MSG_CHAT = 2;
    private static final int MSG_CHECK_STEP = 100;
    private static final int MSG_GOBACK = 1001;
    
    private static final int MSG_MSG_MENU = 2002;
    private static final int MSG_ZHAOHU = 3;
    private static final int MSG_ZHAOHU_LIST = 4;
    private static final int MSG_MSG_LIST = 5;
    private static final int MSG_CITY_LIST = 6;
    private static final int MSG_NEAR_LIST = 2001;
    private static final int MSG_STOPING = 3000;
    private static final int MSG_RESTARTING = 3001;
    private static final int MSG_TIMER_COUNT = 3002;
    
    private int handlenumber = 0;
    private int msgtotalnumber = 0;
    private int msgtime = 0;
    private int mhandedNumber = 0;//已经处理数
    
    private boolean isFirstEnterMain = true;
    private List<AccessibilityNodeInfo>  mMsglist = null;
    private List<AccessibilityNodeInfo>  mNearlist = null;
    private List<AccessibilityNodeInfo>  mZhaohulist = null;
    private int mNearIndex = 0;
    private int mNearHandledCount = 0;//本轮打招呼数
    private int mMsgIndex = 0;
    private int mZhaohuIndex = 0;
    
    private boolean isNeedToSkipZhaohu = true;//是否需要切换到招呼
    private boolean isStoping = false;
    
    private  Timer timer = null; 
    private TimerTask task;  
    private int timercount = 0;//计时
    
    private Handler mainHandler = new Handler(){
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) 
            {
	            case MSG_NEAR_LIST://附近人列表
	            {
	            	handleNearlist();
	            	break;
	            }
                case MSG_PERSON_INFO://个人信息
                {
                    handlePersonInfo();
                    break;
                }
                case MSG_CHAT://聊天界面
                {
                    handleChat();
                    break;
                }
                //切换到消息菜单
                case MSG_MSG_MENU:
                {
                	 clickMsgMenu();
                	 break;
                }
                case MSG_ZHAOHU_LIST:
                {
                	handleZhaoHulist();
                	break;
                }
                case MSG_MSG_LIST:
                {
                	handleMsglist();
                	break;
                }
                case MSG_STOPING:
                {
                    android.util.Log.i(TAG,">>>>>软件进入休眠状态休眠"+msgtime+"分钟");
                    mainHandler.removeMessages(MSG_TIMER_COUNT);
                    mainHandler.sendEmptyMessageDelayed(MSG_TIMER_COUNT, 1000);
                    timer = new Timer();
                    timer.schedule(task = new TimerTask() {  
            	        @Override  
            	        public void run() {  
            	            Message message = new Message();  
            	            message.what = MSG_RESTARTING;  
            	            mainHandler.sendMessage(message);  
            	        }  
            	    }, msgtime*1000*60,2000 );
                    break;
                }
                case MSG_RESTARTING:
                {
                    android.util.Log.i(TAG,">>>>>软件结束休眠 继续执行任务。。。。。。。");
                    sleep(2000);
                    timer.cancel();
                    mhandedNumber = 0;
                    timercount = 0;
                    isStoping = false;
                    mainHandler.removeMessages(MSG_TIMER_COUNT);
                    
                    if(mCurrentNodeIndex == NODE_INDEX_ZHAOHULIST)
                    {
                        handleZhaoHulist();
                    }
                    else  if(mCurrentNodeIndex == NODE_INDEX_MSGLIST)
                    {
                        handleMsglist();
                    }
                    break;
                }
                case MSG_TIMER_COUNT:
                {
                    if(isStoping)
                    {
                        android.util.Log.i(TAG,">>>>>软件已经休眠时间:"+timercount++);
                        mainHandler.sendEmptyMessageDelayed(MSG_TIMER_COUNT, 1000);
                    }
                    break;
                }
            default:
                break;
            }
        };
    };
    
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null)
        {
            return;
        }
        android.util.Log.i(TAG,"onAccessibilityEvent type"+event.getEventType());
        AccessibilityNodeInfo nodeInfo = event.getSource();
        if (nodeInfo == null) {
            return;
        }
        AccessibilityNodeInfo rowNode = nodeInfo;// we can also use getRootInActiveWindow() instead;
        if (rowNode == null) {
            return;
        }
        
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                || event.getEventType() == AccessibilityEvent.TYPE_VIEW_SCROLLED
                || event.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
                ) 
        {
            CharSequence currentClassName = event.getClassName();
            android.util.Log.i(TAG,"currentClassName:" + currentClassName);
            if("com.immomo.momo.maintab.MaintabActivity".equals(currentClassName))
            {
            	   initData();
            	 if(!isFirstEnterMain)
            		 {
            		  	android.util.Log.i(TAG,"《》《》《》《》《》进入了首页");
	            		 if(mCurrentNodeIndex == NODE_INDEX_NEARLIST)//如果是附近人就切换到附近人菜单
	            		 {
	            			 mainHandler.removeMessages(MSG_NEAR_LIST);
	    	                 mainHandler.sendEmptyMessageDelayed(MSG_NEAR_LIST, 4000);
	            		 }
	            		 else if(mCurrentNodeIndex == NODE_INDEX_ZHAOHU)//如果是招呼就切换到消息菜单
	            		 {
	            			 mainHandler.removeMessages(MSG_MSG_MENU);
	    	                 mainHandler.sendEmptyMessageDelayed(MSG_MSG_MENU, 4000);
	            		 }
            		 	return;
            		 }
                	isFirstEnterMain = false;
                	//clickNearfind();
                	clickMsgMenu();
            }
            //main listview
            else if("android.widget.ListView".equals(currentClassName))
            {
            	
            	android.util.Log.i(TAG,">>>>>>>>>>android.widget.ListView mCurrentNodeIndex="+mCurrentNodeIndex);
            	//当前处于附近人列表
            	if(mCurrentNodeIndex == NODE_INDEX_NEARLIST)
            	{
	                //onlineListView
	                //dataLayout
	                AccessibilityNodeInfo nodeInfo_ = getRootInActiveWindow();
	                List<AccessibilityNodeInfo>  refreshnode = nodeInfo_.findAccessibilityNodeInfosByText("正在加载");
	                if(refreshnode!=null && refreshnode.size()>0 && refreshnode.get(0).isVisibleToUser())
	                {
	                    if(isDebuging)
	                        android.util.Log.i(TAG,">>>>>>>>>>online ListView refreshing..... ");
	                   
	                    return;
	                }
	                mainHandler.removeMessages(MSG_NEAR_LIST);
	                mainHandler.sendEmptyMessageDelayed(MSG_NEAR_LIST, 3000);
            	}
            	//当前处于招呼操作，应该点击打招呼进入招呼列表
            	else if(mCurrentNodeIndex == NODE_INDEX_ZHAOHU)
            	{
            		 AccessibilityNodeInfo nodeInfo_ = getRootInActiveWindow();
 	                List<AccessibilityNodeInfo>  refreshnode = nodeInfo_.findAccessibilityNodeInfosByText("个人和你打招呼");
 	               if(refreshnode!=null && refreshnode.size()>0 )
 	               {
 	            	  refreshnode.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
 	            	  if(refreshnode.get(0).getParent() != null)
 	            	  {
 	            		 refreshnode.get(0).getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
 	            	  }
 	            	//然后进入招呼列表
 	 	              mCurrentNodeIndex = NODE_INDEX_ZHAOHULIST;
 	               }
 	               else //没有发现打招呼那一栏，然后直接消息发送,翻页知道发现招呼在进去
 	               {
 	            	  mCurrentNodeIndex = NODE_INDEX_MSGLIST;
 	            	  mainHandler.removeMessages(MSG_MSG_LIST);
  	                  mainHandler.sendEmptyMessageDelayed(MSG_MSG_LIST, 3000);
 	               }
 	               
            	}
            	//当前处于招呼列表
            	else if(mCurrentNodeIndex == NODE_INDEX_ZHAOHULIST )
            	{
            		if(isLocked)return;
            		isLocked = true;
            		android.util.Log.i(TAG,">>>>>您已经进入招呼列表进行操作....");
            		mainHandler.removeMessages(MSG_ZHAOHU_LIST);
 	                mainHandler.sendEmptyMessageDelayed(MSG_ZHAOHU_LIST, 3000);
            	}
            	//当前处于消息列表
            	else if(mCurrentNodeIndex == NODE_INDEX_MSGLIST )
            	{
            		if(isLocked)return;
            		isLocked = true;
            		android.util.Log.i(TAG,">>>>>您已经进入消息列表进行操作....");
            		mainHandler.removeMessages(MSG_MSG_LIST);
 	                mainHandler.sendEmptyMessageDelayed(MSG_MSG_LIST, 3000);
            	}
            	
            	
            }
            //person info
            else if("com.immomo.momo.newprofile.activity.OtherProfileActivity".equals(currentClassName))
            {
                if(mStep == 1)
                {
                    mainHandler.removeMessages(MSG_PERSON_INFO);
                    mainHandler.sendEmptyMessageDelayed(MSG_PERSON_INFO, 2000);
                } 
                else
                {
                    goback();
                }
            }
            //chat
            else if("com.immomo.momo.message.activity.ChatActivity".equals(currentClassName))
            {
//            	isChatPage = true;
//            	if(isChating)
//            	{
//	                mainHandler.removeMessages(MSG_CHAT);
//	                mainHandler.sendEmptyMessageDelayed(MSG_CHAT, 2000);
//            	}
//            	else if(isSelectedImg)
//            	{
//            		goback();
//            		isChatPage = false;
//            	}
            	 mStep = 2;
            	 mainHandler.removeMessages(MSG_CHAT);
	             mainHandler.sendEmptyMessageDelayed(MSG_CHAT, 2000);
            	
            }
            else if("com.immomo.momo.message.activity.HiSessionListActivity".equals(currentClassName))
            {
            	
            }
            else if("com.hankkin.WeiXinSelectImgsDemo.AlbumActivity".equals(currentClassName))
            {
                sleep(1500);
                AccessibilityNodeInfo nodeInfo_ = getRootInActiveWindow();
                if(nodeInfo_ == null)
                {
                    nodeInfo_ = rowNode;
                }
                List<AccessibilityNodeInfo>  gridnode = nodeInfo_.findAccessibilityNodeInfosByViewId("com.hankkin.WeiXinSelectImgsDemo:id/id_item_image");
                android.util.Log.i(TAG,"gridnode size = "+gridnode.size());
                if(gridnode != null && gridnode.size() >0)
                {
                	Random rand = new Random();
                    android.util.Log.i(TAG,"gridnode size = "+gridnode.get(rand.nextInt(gridnode.size())).getChildCount());
                    //for(int i=0;i<gridnode.size();i++)
                    //{
                        gridnode.get(rand.nextInt(gridnode.size())).performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    //}
                }
                isSelectedImg= true;
             
                //state
            } else if("com.immomo.momo.feed.activity.PublishFeedActivity".equals(currentClassName))
            {
                //right top --- ActionMenuItemView  id/0x0
                //id/signeditor_tv_text
                //id/layout_add_pic
                //id/layout_add_video
                AccessibilityNodeInfo nodeInfo_ = getRootInActiveWindow();
                if(nodeInfo_ == null)return;
                
              
                    List<AccessibilityNodeInfo> toolbar = nodeInfo_.findAccessibilityNodeInfosByViewId("com.immomo.momo:id/toolbar_id");
                    List<AccessibilityNodeInfo> okbtnlist = nodeInfo_.findAccessibilityNodeInfosByViewId("com.immomo.momo:id/0x0");
                    List<AccessibilityNodeInfo> txtlist = nodeInfo_.findAccessibilityNodeInfosByViewId("com.immomo.momo:id/signeditor_tv_text");
                    List<AccessibilityNodeInfo> piclist = nodeInfo_.findAccessibilityNodeInfosByViewId("com.immomo.momo:id/layout_add_pic");
                    List<AccessibilityNodeInfo> videolist = nodeInfo_.findAccessibilityNodeInfosByViewId("com.immomo.momo:id/layout_add_video");
            
                    //text
                    AccessibilityNodeInfo inputnode = null;
                    if(txtlist!=null && txtlist.size()>0 )
                    {
                        inputnode = txtlist.get(0);
                    }
                    if(inputnode != null)
                    {
                        ClipboardManager clipboard;
                         clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                         
                        ClipData clip = ClipData.newPlainText("text","今天天气好热");
                        clipboard.setPrimaryClip(clip);
                        //焦点    （n是AccessibilityNodeInfo对象）
                        inputnode.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
                        //粘贴进入内容
                        inputnode.performAction(AccessibilityNodeInfo.ACTION_PASTE);
                        inputnode.recycle();
                        
                    }
                    //toolbar
                    if(toolbar != null && toolbar.size()>0)
                    {
                        AccessibilityNodeInfo tool = toolbar.get(0);
                        AccessibilityNodeInfo oktmp = tool.getChild(2);
                        if(oktmp !=null)
                        {
                            oktmp.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        }
                    }
                    
                    //send
                    if(okbtnlist!=null && okbtnlist.size()>0 )
                    {
                        okbtnlist.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        if(okbtnlist.get(0).getParent() != null)
                        {
                            okbtnlist.get(0).getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        }
                    }
                   
            }
            
        }
    }

    @Override
    public void onInterrupt() {
        
    }
    
    @Override
    protected void onServiceConnected() {
        AccessibilityServiceInfo accessibilityServiceInfo = getServiceInfo();
        if (accessibilityServiceInfo == null)
            accessibilityServiceInfo = new AccessibilityServiceInfo();
        accessibilityServiceInfo.eventTypes = AccessibilityEvent.TYPES_ALL_MASK;
        accessibilityServiceInfo.flags |= AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;
        accessibilityServiceInfo.packageNames = new String[] {"com.hankkin.WeiXinSelectImgsDemo", WeChatFragment.WECHAT_PACKAGENAME};
        accessibilityServiceInfo.feedbackType = AccessibilityServiceInfo.FEEDBACK_ALL_MASK;
        accessibilityServiceInfo.notificationTimeout = 10;
        setServiceInfo(accessibilityServiceInfo);
        
        Notifier.getInstance().notify(this,getString(R.string.app_name), getString(R.string.wechat_acc_service_start_notification), getString(R.string.wechat_acc_service_start_notification),
                Notifier.TYPE_WECHAT_SERVICE_RUNNING, false);
        
        initData();
        
    }
    public void initData()
    {
    	//init data
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String aa = sp.getString("content", "你好，很高兴认识你");
		contentstrs = aa.split("\\|");
		handlenumber = sp.getInt("number", 5);//切换出来次数
		msgtotalnumber = sp.getInt("msgnumber", 30);//消息总数
		msgtime = sp.getInt("msgtime", 3);//间隔时间
		android.util.Log.i(TAG,aa +" handlenumber="+handlenumber);
		
    }
    public void onDestroy() {
        super.onDestroy();
        Notifier.getInstance().cancelByType(
                Notifier.TYPE_WECHAT_SERVICE_RUNNING);
    }
    public void sleep(long time)
    {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
        }
    }
    
    /////////////////////////////////////////////////////////////////////
    ///
    /////////////////////////////////////////////////////////////////////
    private int mStep = 0;//0,1,2
    private boolean isMore = false;
    @SuppressLint("NewApi")
	public void handleNearlist()
    {
        sleep(2000);
    	  mStep = 0;
    	AccessibilityNodeInfo nodeInfo_ = getRootInActiveWindow();
    	if(nodeInfo_ == null)return;
    	List<AccessibilityNodeInfo>  onlinelist = nodeInfo_.findAccessibilityNodeInfosByViewId("com.immomo.momo:id/user_layout_root");
    	if(mNearlist == null)
    	{
    		mNearlist = onlinelist;
    		mNearIndex = 1;
    	}
    	 if(mNearHandledCount >= handlenumber)//如何已经处理数大于预定处理数，就休息N分钟
    	 {
//    		 return;
    	 }
    	 if(mNearlist != null && mNearlist.size() > mNearIndex)
    	 {
    		 mStep = 1;
    		 //点击进入个人信息
    		 mNearlist.get(mNearIndex).performAction(AccessibilityNodeInfo.ACTION_CLICK);
             ++mNearIndex;
    	 }else if(mNearlist != null && mNearlist.size() <= mNearIndex)
    	 {
    		 //翻页
    		 for (int i = 0; i < nodeInfo_.getChildCount(); i++)
               {
                   if (nodeInfo_.getChild(i).getClassName().equals("android.widget.ListView"))
                   {
                       AccessibilityNodeInfo list_view = nodeInfo_.getChild(i);
//                	    nodeInfo_ = getRootInActiveWindow();
//                	   List<AccessibilityNodeInfo> list_view = nodeInfo_.findAccessibilityNodeInfosByViewId("com.immomo.momo:id/ptr_swipe_refresh_layout");
                       if(list_view != null)
                       {
                    	   android.util.Log.i(TAG,"<><><   ACTION_SCROLL_FORWARD   <><><>>>>");
                    	   list_view.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
                    	   //if(list_view.get(0).getParent() != null)
                    	  // list_view.get(0).getParent().performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
                       }else
                       {
                    	   return;
                       }
                       
                       new Thread(new Runnable() {
                           @Override
                           public void run() {
                           try {
                            Thread.sleep(3000);
                           } catch (InterruptedException mE) {
                            mE.printStackTrace();
                           }
                           AccessibilityNodeInfo nodeInfo_ = getRootInActiveWindow();
                           List<AccessibilityNodeInfo> list_ = nodeInfo_.findAccessibilityNodeInfosByViewId("com.immomo.momo:id/user_layout_root");
                           android.util.Log.i(TAG, "列表人数: "+list_.size());
                           mNearlist = list_;
                           //滑动之后，上一页的最后一个item为当前的第一个item，所以从第二个开始打招呼
                           mNearIndex = 1;
                           handleNearlist();
                           }
                          }).start();
                   }  
               
               }
    		 
    	 }
    	
    }
    private boolean isLocked = false;
    //处理招呼列表
    @SuppressLint("NewApi")
	public void handleZhaoHulist()
    {
        //休息时间
        if(mhandedNumber >= handlenumber)
        {
            isStoping = true;
          mainHandler.removeMessages(MSG_STOPING);
          mainHandler.sendEmptyMessageDelayed(MSG_STOPING, 2000);
        }
        if(isStoping)
        {
            return;
        }
    	sleep(2000);
    	android.util.Log.i(TAG,">>>>>当前处理招呼索引 = "+mZhaohuIndex);
    	  mStep = 0;
    	AccessibilityNodeInfo nodeInfo_ = getRootInActiveWindow();
    	if(nodeInfo_ == null)return;
    	
    	if(mZhaohulist == null)
    	{
    		List<AccessibilityNodeInfo>  onlinelist = nodeInfo_.findAccessibilityNodeInfosByViewId("com.immomo.momo:id/item_layout");
    		mZhaohulist = onlinelist;
    		mZhaohuIndex = 0;
    	}
    	if(mZhaohulist != null && mZhaohulist.size() == 0)
    	{
    		goback();
    		mCurrentNodeIndex = NODE_INDEX_MSGLIST;
    		return;
    	}
    	
    	 if(mZhaohulist != null && mZhaohulist.size() > mZhaohuIndex)
    	 {
    		 mStep = 1;
    		 //点击进入个人信息
    		 mZhaohulist.get(mZhaohuIndex).performAction(AccessibilityNodeInfo.ACTION_CLICK);
    		 android.util.Log.i(TAG,">>>>>当前处理招呼索引 CLICKED = "+mZhaohuIndex + "size = "+mZhaohulist.size());
             ++mZhaohuIndex;
             mhandedNumber++;
             
    	 }else if(mZhaohulist != null && mZhaohulist.size() <= mZhaohuIndex)
    	 {
    		 //翻页
    		 for (int i = 0; i < nodeInfo_.getChildCount(); i++)
               {
                   if (nodeInfo_.getChild(i).getClassName().equals("android.widget.ListView"))
                   {
                       AccessibilityNodeInfo list_view = nodeInfo_.getChild(i);
                       if(list_view != null)
                       {
                    	   android.util.Log.i(TAG,"<><><   ACTION_SCROLL_FORWARD   <><><>>>>");
                    	   boolean flag = list_view.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
                    	   if(!flag)
                    	   {
                    		 //没有更多页的情况下返回消息界面
                    		  // goback();
                    		  // mCurrentNodeIndex = NODE_INDEX_MSGLIST;
                    		   android.util.Log.i(TAG,">>>>>当前处理招呼翻页失败... ");
                    		   goback();
                     		   mCurrentNodeIndex = NODE_INDEX_MSGLIST;
                     		   return;
                    	   }
                    	  
                    	   //if(list_view.get(0).getParent() != null)
                    	  // list_view.get(0).getParent().performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
                       }else
                       {
                    	   return;
                       }
                       
                       new Thread(new Runnable() {
                           @Override
                           public void run() {
                           try {
                            Thread.sleep(3000);
                           } catch (InterruptedException mE) {
                            mE.printStackTrace();
                           }
                           AccessibilityNodeInfo nodeInfo_ = getRootInActiveWindow();
                           List<AccessibilityNodeInfo> list_ = nodeInfo_.findAccessibilityNodeInfosByViewId("com.immomo.momo:id/item_layout");
                           android.util.Log.i(TAG, "列表人数: "+list_.size());
                           mZhaohulist = list_;
                           //滑动之后，上一页的最后一个item为当前的第一个item，所以从第二个开始打招呼
                           mZhaohuIndex = 0;
                           handleZhaoHulist();
                           }
                          }).start();
                   }  
               
               }
    		 
    	 }
    	
    }
    
    @SuppressLint("NewApi")
	public void handleMsglist()
    {
        
        //休息时间
        if(mhandedNumber >= handlenumber)
        {
            isStoping = true;
           mainHandler.removeMessages(MSG_STOPING);
           mainHandler.sendEmptyMessageDelayed(MSG_STOPING, 2000);
        }
        if(isStoping)
        {
            return;
        }
        
    	sleep(2000);
    	android.util.Log.i(TAG,">>>>>当前处理消息索引 = "+mMsgIndex);
    	  mStep = 0;
    	AccessibilityNodeInfo nodeInfo_ = getRootInActiveWindow();
    	if(nodeInfo_ == null)return;
    	
    	if(mMsglist == null)
    	{List<AccessibilityNodeInfo>  onlinelist = nodeInfo_.findAccessibilityNodeInfosByViewId("com.immomo.momo:id/item_layout");
    		mMsglist = onlinelist;
    		mMsgIndex = 0;
    	}
    	 if(mMsglist != null && mMsglist.size() > mMsgIndex)
    	 {
    		 mStep = 1;
    		 AccessibilityNodeInfo info = mMsglist.get(mMsgIndex);
    		 //chatlist_item_tv_name
    		 List<AccessibilityNodeInfo> names = info.findAccessibilityNodeInfosByViewId("com.immomo.momo:id/chatlist_item_tv_name");
    		 if(names != null && names.size() > 0)
    		 {
    			 AccessibilityNodeInfo name = names.get(0);
    			 String nametext = name.getText().toString();
    			 if(nametext.equals("订阅内容") || nametext.equals("互动通知"))//跳过
    			 {
    				 android.util.Log.i(TAG,">>>>>当前处理消息过滤内容 订阅内容  互动通知");
    				 ++mMsgIndex;
    				 handleMsglist();
    				 return;
    			 }
    			 else if(nametext.contains("个人和你打招呼"))
    			 {
    				 mCurrentNodeIndex = NODE_INDEX_ZHAOHU;
    				  nodeInfo_ = getRootInActiveWindow();
  	                List<AccessibilityNodeInfo>  refreshnode = nodeInfo_.findAccessibilityNodeInfosByText("个人和你打招呼");
  	               if(refreshnode!=null && refreshnode.size()>0 )
  	               {
  	            	  refreshnode.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
  	            	  if(refreshnode.get(0).getParent() != null)
  	            	  {
  	            		 refreshnode.get(0).getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
  	            	  }
  	            	//然后进入招呼列表
  	 	              mCurrentNodeIndex = NODE_INDEX_ZHAOHULIST;
  	               }
    			 }
    			 
    		 }
    		 //点击进入个人信息
    		 mMsglist.get(mMsgIndex).performAction(AccessibilityNodeInfo.ACTION_CLICK);
    		 android.util.Log.i(TAG,">>>>>当前处理消息索引 CLICKED = "+mMsgIndex + "size = "+mMsglist.size());
             ++mMsgIndex;
             mhandedNumber++;
             
    	 }else if(mMsglist != null && mMsglist.size() <= mMsgIndex)
    	 {
    		 //翻页
    		 for (int i = 0; i < nodeInfo_.getChildCount(); i++)
               {
                   if (nodeInfo_.getChild(i).getClassName().equals("android.widget.ListView"))
                   {
                       AccessibilityNodeInfo list_view = nodeInfo_.getChild(i);
                       if(list_view != null)
                       {
                    	   android.util.Log.i(TAG,"<><><   ACTION_SCROLL_FORWARD   <><><>>>>");
                    	   boolean flag = list_view.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
                    	   if(!flag)
                    	   {
                    		 //没有更多页的情况下返回消息界面
                    		  // goback();
                    		  // mCurrentNodeIndex = NODE_INDEX_MSGLIST;
                    		   android.util.Log.i(TAG,">>>>>当前处理消息翻页失败... ");
                     		   return;
                    	   }
                    	  
                    	   //if(list_view.get(0).getParent() != null)
                    	  // list_view.get(0).getParent().performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
                       }else
                       {
                    	   return;
                       }
                       
                       new Thread(new Runnable() {
                           @Override
                           public void run() {
                           try {
                            Thread.sleep(3000);
                           } catch (InterruptedException mE) {
                            mE.printStackTrace();
                           }
                           AccessibilityNodeInfo nodeInfo_ = getRootInActiveWindow();
                           List<AccessibilityNodeInfo> list_ = nodeInfo_.findAccessibilityNodeInfosByViewId("com.immomo.momo:id/item_layout");
                           android.util.Log.i(TAG, "列表人数: "+list_.size());
                           mMsglist = list_;
                           //滑动之后，上一页的最后一个item为当前的第一个item，所以从第二个开始打招呼
                           mMsgIndex = 0;
                           handleMsglist();
                           }
                          }).start();
                   }  
               
               }
    		 
    	 }
    	
    }
    
//    public void handleOnlineList()
//    {
//    	isChating = true; 
//        AccessibilityNodeInfo nodeInfo_ = getRootInActiveWindow();
//        if(nodeInfo_ == null)return;
//        List<AccessibilityNodeInfo>  onlinelist = nodeInfo_.findAccessibilityNodeInfosByViewId("com.psd:id/dataLayout");
//        if(isDebuging)
//            android.util.Log.i(TAG,">>>>>>>>>>online ListView  "+onlinelist.size());
//        if(monlinelist == null){
//            monlinelist = onlinelist;
//        }
//        
//        if(mCurrOnlineIndex >= handlenumber)
//        {
//        	mCurrOnlineIndex = 0;
//        	clickcity();
//        	return;
//        }
//        if(monlinelist != null && monlinelist.size() > mCurrOnlineIndex)
//        {
//            mStep = 1;
//            monlinelist.get(mCurrOnlineIndex).performAction(AccessibilityNodeInfo.ACTION_CLICK);
//            ++mCurrOnlineIndex;
//        }else if(monlinelist != null && monlinelist.size() <= mCurrOnlineIndex)
//        {
//        	if(isMore)
//	    	{
//	    		isMore = false;
//                List<AccessibilityNodeInfo> list_ = nodeInfo_.findAccessibilityNodeInfosByViewId("com.psd:id/dataLayout");
//                android.util.Log.i(TAG, "列表人数: "+list_.size());
//                monlinelist = list_;
//                handleOnlineList();
//                return;
//	    	}
//        	 //判断是否有更多
//	        List<AccessibilityNodeInfo> list_ = nodeInfo_.findAccessibilityNodeInfosByText("点击查看更多");
//	        if(list_ !=null && list_.size()>0)
//	        {
//	        	for(int i=0;i<list_.size();i++)
//	        	{
//	        		list_.get(i).performAction(AccessibilityNodeInfo.ACTION_CLICK);
//	        		isMore = true;
//	        	}
//	        	
//	        }
//	        else
//	        {
//	        
//            for (int i = 0; i < nodeInfo_.getChildCount(); i++)
//            {
//                if (nodeInfo_.getChild(i).getClassName().equals("android.widget.ListView"))
//                {
//                    AccessibilityNodeInfo node_lsv = nodeInfo_.getChild(i);
//                    node_lsv.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
//                    
//                    new Thread(new Runnable() {
//                        @Override
//                        public void run() {
//                        try {
//                         Thread.sleep(3000);
//                        } catch (InterruptedException mE) {
//                         mE.printStackTrace();
//                        }
//                        AccessibilityNodeInfo nodeInfo_ = getRootInActiveWindow();
//                        List<AccessibilityNodeInfo> list_ = nodeInfo_.findAccessibilityNodeInfosByViewId("com.psd:id/dataLayout");
//                        android.util.Log.i(TAG, "列表人数: "+list_.size());
//                        monlinelist = list_;
//                        //滑动之后，上一页的最后一个item为当前的第一个item，所以从第二个开始打招呼
//                        mCurrOnlineIndex = 1;
//                        handleOnlineList();
//                        }
//                       }).start();
//                }  
//            
//            }
//	        }
//        	
//        }
//    }
//    
//    
//    public void handleCityList()
//    {
//    	isChating = true;
//        AccessibilityNodeInfo nodeInfo_ = getRootInActiveWindow();
//        if(nodeInfo_ == null)return;
//        List<AccessibilityNodeInfo>  onlinelist = nodeInfo_.findAccessibilityNodeInfosByViewId("com.psd:id/dataLayout");
//        if(isDebuging)
//            android.util.Log.i(TAG,">>>>>>>>>>online ListView  "+onlinelist.size());
//        if(mCityList == null){
//        	mCityList = onlinelist;
//        }
//        
//        if(mCurrCityIndex >= handlenumber)//从同城切换到消息
//        {
//        	mCurrCityIndex = 0;
//        	mCurrentNodeIndex = NODE_INDEX_ZHAOHU;
//        	mainHandler.removeMessages(MSG_ZHAOHU);
//	        mainHandler.sendEmptyMessageDelayed(MSG_ZHAOHU, 3000);
//        	
//        	return;
//        }
//        if(mCityList != null && mCityList.size() > mCurrCityIndex)
//        {
//            mStep = 1;
//            mCityList.get(mCurrCityIndex).performAction(AccessibilityNodeInfo.ACTION_CLICK);
//            ++mCurrCityIndex;
//        }else if(mCityList != null && mCityList.size() <= mCurrCityIndex)
//        {
//        	if(isMore)
//	    	{
//	    		isMore = false;
//                List<AccessibilityNodeInfo> list_ = nodeInfo_.findAccessibilityNodeInfosByViewId("com.psd:id/dataLayout");
//                android.util.Log.i(TAG, "列表人数: "+list_.size());
//                mCityList = list_;
//                handleCityList();
//                return;
//	    	}
//        	 //判断是否有更多
//	        List<AccessibilityNodeInfo> list_ = nodeInfo_.findAccessibilityNodeInfosByText("点击查看更多");
//	        if(list_ !=null && list_.size()>0)
//	        {
//	        	for(int i=0;i<list_.size();i++)
//	        	{
//	        		list_.get(i).performAction(AccessibilityNodeInfo.ACTION_CLICK);
//	        		isMore = true;
//	        	}
//	        	
//	        }
//	        else
//	        {
//	        
//            for (int i = 0; i < nodeInfo_.getChildCount(); i++)
//            {
//                if (nodeInfo_.getChild(i).getClassName().equals("android.widget.ListView"))
//                {
//                    AccessibilityNodeInfo node_lsv = nodeInfo_.getChild(i);
//                    node_lsv.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
//                    
//                    new Thread(new Runnable() {
//                        @Override
//                        public void run() {
//                        try {
//                         Thread.sleep(3000);
//                        } catch (InterruptedException mE) {
//                         mE.printStackTrace();
//                        }
//                        AccessibilityNodeInfo nodeInfo_ = getRootInActiveWindow();
//                        List<AccessibilityNodeInfo> list_ = nodeInfo_.findAccessibilityNodeInfosByViewId("com.psd:id/dataLayout");
//                        android.util.Log.i(TAG, "列表人数: "+list_.size());
//                        mCityList = list_;
//                        //滑动之后，上一页的最后一个item为当前的第一个item，所以从第二个开始打招呼
//                        mCurrCityIndex = 1;
//                        handleCityList();
//                        }
//                       }).start();
//                }  
//            
//            }
//	        }
//        	
//        }
//    }
    
    public void goback()
    {
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
    public void goback2()
    {
        new Thread(
                new Runnable() {
                    
                    @Override
                    public void run() {
                        performGlobalAction(GLOBAL_ACTION_BACK);
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
    
    public void handlePersonInfo()
    {
        sleep(2000);
    	isChating = true; 
        AccessibilityNodeInfo nodeInfo_ = getRootInActiveWindow();
       if(nodeInfo_ !=null)
        {
            List<AccessibilityNodeInfo> list=nodeInfo_.findAccessibilityNodeInfosByText("对话");
            if(list != null && list.size()>0)
            {
                
                for(int i=0;i<list.size();i++)
                {
                    android.util.Log.i(TAG,"NewFriendInfo:clicked");
                    if(list.get(i).performAction(AccessibilityNodeInfo.ACTION_CLICK)
                            || (list.get(i).getParent() != null && (list.get(i).getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK)))
                            )
                    {
                        mStep = 2;
                    }
                }
                
                
            }
        }
    }
    private boolean isChating = false;
    public void handleChat()
    {
//        mStep = 3;
//        isChating = false;
//        if(		
//        	mCurrentNodeIndex == NODE_INDEX_ZHAOLIST
//        		|| mCurrentNodeIndex == NODE_INDEX_MSG
//        		)
//        {
//        	
//        	android.util.Log.i(TAG,"<><><><><><>    发送图片和文字");
//        	sendWord();
//        	sendImg();
//        }
//        else if(mCurrentNodeIndex == NODE_INDEX_ONLINE
//        		|| mCurrentNodeIndex == NODE_INDEX_CITY
//        		)
//        {
//        	android.util.Log.i(TAG,"<><><><><><>    只发送文字");
//        	sendWord();
//        	isChating = true;
//        	isChatPage = false;
//        	goback();
//        }
    	sendWord();
    	sleep(2000);
    	isLocked = false;
    	goback();
    }
    
    private boolean isSelectedImg = false;
    public void sendWord()
    {
        sleep(2000);
        AccessibilityNodeInfo nodeInfo_ = getRootInActiveWindow();
        if(nodeInfo_!=null)
        {
            
                //a2y
                //List<AccessibilityNodeInfo> list=nodeInfo_.findAccessibilityNodeInfosByViewId("com.blackbean.cnmeach:id/a2y");
                    //AccessibilityNodeInfo inputnode = nodeInfo_.findFocus(AccessibilityNodeInfo.FOCUS_INPUT); 
        	if(Build.VERSION.SDK_INT >=Build.VERSION_CODES.LOLLIPOP )//5.0
        	{
        		AccessibilityNodeInfo inputnode = null;
                    List<AccessibilityNodeInfo> list = nodeInfo_.findAccessibilityNodeInfosByViewId("com.immomo.momo:id/message_ed_msgeditor");
                    if(list != null)
                    {
                    	inputnode = list.get(0);
                    }
                    
                    if(inputnode == null)return;
                    inputnode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    inputnode.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
                       Bundle arguments = new Bundle();
                       String sendwords = getRandomString2();
                      android.util.Log.i(TAG,"sendwords = "+sendwords);
                       arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                    		   sendwords );
                       inputnode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
                       
                       inputnode.recycle();
        	}
        	else
        	{
        		AccessibilityNodeInfo inputnode = nodeInfo_.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
                if(inputnode != null)
                {
                	ClipboardManager clipboard;
                	 clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                	 String sendwords = getRandomString2();
                	ClipData clip = ClipData.newPlainText("text",sendwords);
                	clipboard.setPrimaryClip(clip);
                	//焦点    （n是AccessibilityNodeInfo对象）
                	inputnode.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
                	//粘贴进入内容
                	inputnode.performAction(AccessibilityNodeInfo.ACTION_PASTE);
                	inputnode.recycle();
                	
                }
        	}
                     sleep(1500);
                     nodeInfo_ = getRootInActiveWindow();
                       List<AccessibilityNodeInfo> sendbtn=nodeInfo_.findAccessibilityNodeInfosByText("发送");
                       android.util.Log.i(TAG,"》》》》》SoftInputWindow》sendbtn="+sendbtn.size());
                       if(sendbtn != null&& sendbtn.size() >0)
                       {
                               sendbtn.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
                               sendbtn.get(0).recycle();
                       }
                      
        }
    }
    public void sendImg()
    {
        
    	isSelectedImg = false;
        AccessibilityNodeInfo nodeInfo_ = getRootInActiveWindow();
        if(nodeInfo_!=null)
        {
            List<AccessibilityNodeInfo> more=nodeInfo_.findAccessibilityNodeInfosByViewId("com.psd:id/moreButton");
          if(more != null&& more.size() >0)
          {
              more.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
              more.get(0).recycle();
          }
          sleep(1000);
          nodeInfo_ = getRootInActiveWindow();
          List<AccessibilityNodeInfo> imgbutton=nodeInfo_.findAccessibilityNodeInfosByViewId("com.psd:id/itemLayout");
          android.util.Log.i(TAG,">>>> imgbutton size = "+imgbutton.size());
          if(imgbutton != null&& imgbutton.size() >5)
          {
//        	  for(int i=1;i<imgbutton.size();i++)
        	  {
        		  imgbutton.get(4).performAction(AccessibilityNodeInfo.ACTION_CLICK);
        		  imgbutton.get(4).recycle();
        	  }
          }
          sleep(1000);
          nodeInfo_ = getRootInActiveWindow();
          List<AccessibilityNodeInfo> selectimg=nodeInfo_.findAccessibilityNodeInfosByViewId("com.psd:id/btn_two");
          if(selectimg != null&& selectimg.size() >0)
          {
        	  selectimg.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
        	  selectimg.get(0).recycle();
          }
         
        }
    }
    
    public String getRandomBiaodian()
	{
		int i=0;
		 Random random = new Random();
		 i = random.nextInt(biaodianstrs.length);
		return biaodianstrs[i];
	}
	public String getRandomString(int i)
	{
		if(i>contentstrs.length )
		{
			i = contentstrs.length -1;
		}
		return contentstrs[i] + getRandomBiaodian();
	}
	
	public String getRandomString2()
	{
		
		if(contentstrs == null)
		{
			return getString(R.string.sendcontent)+ getRandomBiaodian();
		}
		int i=0;
		 Random random = new Random();
		 i = random.nextInt(contentstrs.length);
		return contentstrs[i] + getRandomBiaodian();
	}
	public void clickNearfind()//点击附近菜单
	{
		isChating = true;
		android.util.Log.i(TAG,"<><><><><><><> clickfind ....");
		AccessibilityNodeInfo nodeInfo_ = getRootInActiveWindow();
        List<AccessibilityNodeInfo> list=nodeInfo_.findAccessibilityNodeInfosByText("附近");
        if(list!=null && list.size()>0)
        {
            list.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
            list.get(0).getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
            list.get(0).getParent().recycle();
        }
        sleep(200);
        //indicator
        List<AccessibilityNodeInfo>  indicators = nodeInfo_.findAccessibilityNodeInfosByViewId("com.immomo.momo:id/tablayout_id");
        if(indicators!=null && indicators.size()>0)
        {
            AccessibilityNodeInfo indicator = indicators.get(0);
            if(isDebuging)
            android.util.Log.i(TAG,">>>>>>>>>>indicator  "+indicator.getChildCount());
            AccessibilityNodeInfo peoplenode = indicator.getChild(1);
            peoplenode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            mCurrentNodeIndex = NODE_INDEX_NEARLIST;
            isFirstEnterMain = false;
        }
	}
	
	public void clickMsgMenu()//点击消息菜单
	{
	    sleep(2000);
		isChating = true;
		android.util.Log.i(TAG,"<><><><><><><> click message menu....");
		AccessibilityNodeInfo nodeInfo_ = getRootInActiveWindow();
        List<AccessibilityNodeInfo> list=nodeInfo_.findAccessibilityNodeInfosByText("消息");
        if(list!=null && list.size()>0)
        {
            list.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
            list.get(0).getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
            list.get(0).getParent().recycle();
        }
        sleep(1500);
	}
	public void PublishFeedToMoMo()
	{
//	    com.immomo.momo.feed.activity.PublishFeedActivity
//	    com.immomo.momo/com.immomo.momo.feed.activity.PublishFeedActivity
//	    ActivityManager: START u0 {act=android.intent.action.SEND typ=image/* flg=0x1 cmp=com.immomo.momo/.feed.activity.SharePublishFeedActivity clip={image/* U:file:///storage/emulated/0/tencent/QQmail/screenshot/%E6%82%A8%E7%9A%84%20iCloud%20%E5%82%A8%E5%AD%98%E7%A9%BA%E9%97%B4%E5%B0%86%E6%BB%A1%E3%80%82.jpg} (has extras)} from uid 10108 from pid 17342 on display 0 from pid 17342
//	    if (Intent.ACTION_SEND.equals(action) && type != null) {
//	        if ("text/plain".equals(type)) {
//	            handleSendText(intent); // 处理发送来的文字
//	        } else if (type.startsWith("image/")) {
//	            handleSendImage(intent); // 处理发送来的图片
//	        }
//	    } else if (Intent.ACTION_SEND_MULTIPLE.equals(action) && type != null) {
//	        if (type.startsWith("image/")) {
//	            handleSendMultipleImages(intent); // 处理发送来的多张图片
//	        }
//	    } else {
//	        // 处理其他intents，比如由主屏启动
//	    }
	    
	    
	}
	
}
