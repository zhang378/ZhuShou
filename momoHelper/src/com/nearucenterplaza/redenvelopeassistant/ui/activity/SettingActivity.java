package com.nearucenterplaza.redenvelopeassistant.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.Switch;

import com.crystal.momo.R;
import com.nearucenterplaza.redenvelopeassistant.service.core.SettingHelper;

public class SettingActivity extends ActionBarActivity{
	RadioGroup mModeRg;
	RadioGroup mLanguageRg;
	RadioButton mAutoModeRb;
	RadioButton mSafeModeRb;
	
	Switch mSoundSwitch;
	
	public static void actionTo(Context context){
		Intent intent=new Intent(context, SettingActivity.class);
		context.startActivity(intent);
	}
	
	public void onCreate(Bundle bundle){
		super.onCreate(bundle);
		setContentView(R.layout.activity_settings);
		initView();
	}
	
	void initView() {
		// find views
		mModeRg=(RadioGroup) findViewById(R.id.settings_re_mode_rg);
		mLanguageRg=(RadioGroup)findViewById(R.id.settings_language_rg);
		mAutoModeRb = (RadioButton) findViewById(R.id.settings_re_auto_mode_rb);
		mSafeModeRb=(RadioButton)findViewById(R.id.settings_re_safe_mode_rb);
		mSoundSwitch = (Switch)findViewById(R.id.settings_sound_play);

		// set listeners
		mModeRg.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				switch (checkedId) {
				case R.id.settings_re_auto_mode_rb:
					SettingHelper.setREAutoMode(mAutoModeRb.isChecked());
					SettingHelper.setRESafeMode(!mAutoModeRb.isChecked());
					break;
				case R.id.settings_re_safe_mode_rb:
					SettingHelper.setREAutoMode(!mSafeModeRb.isChecked());
					SettingHelper.setRESafeMode(mSafeModeRb.isChecked());
					break;
				default:
					break;
				}
			}
		});
		
		mLanguageRg.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				switch (checkedId) {
				case R.id.settings_language_simple_chinese_rb:
					break;
				case R.id.settings_language_traditional_chinese_rb:
					break;
				case R.id.settings_language_english_rb:
					break;
				default:
					break;
				}
			}
		});
		
		mSoundSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				SettingHelper.setRESound(isChecked);
			}
		});
		
		// set values
		setTitle(getString(R.string.action_settings));
		mAutoModeRb.setChecked(SettingHelper.getREAutoMode());
		mSafeModeRb.setChecked(SettingHelper.getRESafeMode());
		mSoundSwitch.setChecked(SettingHelper.isRESound());
	}

}
