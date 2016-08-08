package com.hyunnyapp.brainycopter.ipc.view.adapter;

import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;

import com.hyunnyapp.brainycopter.ipc.view.SettingView;

import java.util.List;


public class SettingsViewAdapter extends PagerAdapter 
{

	private List<SettingView> views;
	
	public SettingsViewAdapter(List<SettingView> views)
	{
		this.views = views;
	}
	

	@Override
	public Object instantiateItem(ViewGroup container, int position) 
	{	
		View view = views.get(position).getContent();
		((ViewPager)container).addView(view, 0);
		
		return view;
	}
	

	@Override
	public void destroyItem(ViewGroup container, int position, Object object) 
	{
		((ViewPager)container).removeView((View)object);
	}


	@Override
	public int getCount() 
	{
		return views.size();
	}


	@Override
	public boolean isViewFromObject(View arg0, Object arg1) {
		return arg0.equals(arg1);
	}

}
