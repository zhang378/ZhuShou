package com.nearucenterplaza.redenvelopeassistant.ui.fragmant;

import java.io.File;
import java.util.ArrayList;
import java.util.Random;

import com.crystal.momo.R;
import com.crystal.momo.R.string;
import com.nearucenterplaza.redenvelopeassistant.ui.activity.HomeActivity;
import com.nearucenterplaza.redenvelopeassistant.ui.activity.SettingActivity;
import com.nearucenterplaza.redenvelopeassistant.utils.AccessibilityServiceHelper;
import com.nearucenterplaza.redenvelopeassistant.utils.PackageUtils;
import com.nearucenterplaza.redenvelopeassistant.utils.XLog;
import com.nearucenterplaza.redenvelopeassistant.utils.XToast;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class WeChatFragment extends Fragment implements OnClickListener  {
//Intent intent = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
//	startActivityForResult(intent, 0);
	/**
	 * The fragment argument representing the section number for this fragment.
	 */
	private static final String ARG_SECTION_NUMBER = "section_number";
	

	/**
	 * Returns a new instance of this fragment for the given section number.
	 */
	public static WeChatFragment newInstance(int sectionNumber) {
		WeChatFragment fragment = new WeChatFragment();
		Bundle args = new Bundle();
		args.putInt(ARG_SECTION_NUMBER, sectionNumber);
		fragment.setArguments(args);
		return fragment;
	}

	public WeChatFragment() {
	}

	TextView mOneKeyCleanTv;
	TextView mServiceStateTv;
	TextView mOpenWechatTv;
	EditText sendtxt;
	EditText handlenumber;
	RadioGroup radgroup;
	RadioButton rdo_word;
	RadioButton rdo_img;
	View save;

	EditText msgnumber;
	EditText msgtime;
	
	public  final static String WECHAT_PACKAGENAME = "com.immomo.momo";//"com.tencent.mm";


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_wechat, container, false);
		initView(rootView);
		return rootView;
	}
	
	@Override
	public void onResume(){
		super.onResume();
		refreshLayout();
	}

	void initView(View view) {
		// find views
		mServiceStateTv = (TextView) view.findViewById(R.id.wechat_service_state_tv);
		mOneKeyCleanTv = (TextView) view.findViewById(R.id.wechat_onekey_clean_data_tv);
		mOpenWechatTv = (TextView) view.findViewById(R.id.wechat_openapp_tv);

		// set listeners
		mServiceStateTv.setOnClickListener(this);
		mOneKeyCleanTv.setOnClickListener(this);
		mOpenWechatTv.setOnClickListener(this);
		view.findViewById(R.id.wechat_setting_tv).setOnClickListener(this);

		
		sendtxt = (EditText)view.findViewById(R.id.sendtxt);
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
		
		sendtxt.setText(sp.getString("content", "你好，很高兴认识你"));
		
		handlenumber = (EditText)view.findViewById(R.id.handlenumber);
		handlenumber.setText(sp.getInt("number", 5)+"");
		
		msgnumber = (EditText)view.findViewById(R.id.msgnumber);
		msgnumber.setText(sp.getInt("msgnumber", 30)+"");
		
		msgtime = (EditText)view.findViewById(R.id.msgtime);
		msgtime.setText(sp.getInt("msgtime", 3)+"");
		
		save = view.findViewById(R.id.save);
		save.setOnClickListener(this);
		
		radgroup = (RadioGroup)view.findViewById(R.id.radgroup);
		rdo_word = (RadioButton)view.findViewById(R.id.rdo_word);
		rdo_img = (RadioButton)view.findViewById(R.id.rdo_img);
		int index = sp.getInt("rdo", 0);
		if(index == 0)
		    rdo_word.setChecked(true);
		else
		    rdo_img.setChecked(true);
		radgroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            
            @Override
            public void onCheckedChanged(RadioGroup rdog, int index) {
                int radioButtonId = rdog.getCheckedRadioButtonId();
                int selected = 0;
                switch (radioButtonId) {
                    case R.id.rdo_word:
                    {
                        selected = 0;
                        break;
                    }
                    case R.id.rdo_img:
                    {   
                        selected = 1;
                        break;
                    }
                default:
                    break;
                }
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
                sp.edit().putInt("rdo", selected).commit();
            }
        });
		// set values
		refreshLayout();
	}

	void refreshLayout(){
		if(mServiceStateTv==null)
			return;
		Context context=getActivity();
		if(context==null)
			return;
		boolean isRunning = AccessibilityServiceHelper.isAccessibilitySettingsOn(context);
		mServiceStateTv.setText(isRunning ? getString(R.string.ui_wechat_tv_service_on) : getString(R.string.ui_wechat_tv_service_off));
		mServiceStateTv.setTextColor(isRunning ? Color.BLUE : Color.RED);

		mOneKeyCleanTv.setText(isRunning ? getString(R.string.ui_wechat_btn_service_on_text) : getString(R.string.ui_wechat_btn_service_off_text));
		mOneKeyCleanTv.setTextColor(isRunning ? Color.RED : Color.WHITE);
		
		if(PackageUtils.isAppInstalled(getActivity(), WECHAT_PACKAGENAME)){
			mOpenWechatTv.setVisibility(View.VISIBLE);
		}else{
			mOpenWechatTv.setVisibility(View.GONE);
		}
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.wechat_service_state_tv:
//			AccessibilityServiceHelper.startService(getActivity());
			break;
		case R.id.wechat_onekey_clean_data_tv:
			Context context=getActivity();
			if(context==null)
				return;
			if(AccessibilityServiceHelper.isAccessibilitySettingsOn(getActivity())){
				XToast.xtShort(context, getText(R.string.close_accessibility_service_hint));
			}else{
				XToast.xtShort(context, getText(R.string.open_accessibility_service_hint));
			}
			AccessibilityServiceHelper.startService(getActivity());
			break;
		case R.id.wechat_openapp_tv:
			//PackageUtils.openApp(getActivity(), WECHAT_PACKAGENAME);
		{
		    sendImgs();
		    break;
		}
		case R.id.wechat_setting_tv:
			SettingActivity.actionTo(getActivity());
			break;
		case R.id.save:
		    String txt = sendtxt.getText().toString();
		    String handlenumbertext = handlenumber.getText().toString();
		    String msgnumbertext = msgnumber.getText().toString();
		    String msgtimetext = msgtime.getText().toString();
		    SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
		    sp.edit().putString("content", txt).commit();
		    sp.edit().putInt("number", Integer.valueOf(handlenumbertext)).commit();
		    sp.edit().putInt("msgnumber", Integer.valueOf(msgnumbertext)).commit();
		    sp.edit().putInt("msgtime", Integer.valueOf(msgtimetext)).commit();
		    Toast.makeText(getActivity(), "save ok", Toast.LENGTH_SHORT).show();
		    break;
		}
	}
	
	public void sendtxt()
	{
	    Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, "This is my text to send.");
        sendIntent.setType("text/plain");
        startActivity(sendIntent);
	}

	public void sendImgAndVideo()
	{
	    Intent shareIntent = new Intent();
	    shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
	    shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File("")));
	    shareIntent.setType("image/*");
	    startActivity(Intent.createChooser(shareIntent, "testmomo"));
	}
	public void sendImgs()
    {
//        Intent shareIntent = new Intent();
//        shareIntent.setAction(Intent.ACTION_SEND);
//        shareIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
//        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File("")));
//        shareIntent.setType("image/*");
//        startActivity(Intent.createChooser(shareIntent, "testmomo"));
	    ArrayList<Uri> imageUris = new ArrayList<Uri>();  
	    imageUris.add(Uri.parse("1")); // Add your image URIs here  
	    imageUris.add(Uri.parse("2"));  
	      
	    Intent shareIntent = new Intent();  
	    shareIntent.setAction(Intent.ACTION_SEND);  
	    shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, imageUris); 
	    shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File("")));
	    shareIntent.setType("image/*");  
	    startActivity(Intent.createChooser(shareIntent, "Share images to.."));  
//	    com.immomo.momo/com.immomo.momo.multpic.activity.MulImagePickerActivity
//	    com.immomo.momo/com.immomo.momo.feed.activity.PublishFeedActivity
	    
    }
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		((HomeActivity) activity).onSectionAttached(getArguments().getInt(ARG_SECTION_NUMBER));
	}

	public static void log(String msg) {
		XLog.i("wechat", msg);
	}
	
}
