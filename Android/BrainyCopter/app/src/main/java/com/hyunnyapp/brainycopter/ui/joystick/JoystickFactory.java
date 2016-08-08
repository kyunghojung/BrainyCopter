/*
 * JoystickFactory
 *
 *  Created on: May 26, 2011
 *      Author: Dmytro Baryskyy
 */

package com.hyunnyapp.brainycopter.ui.joystick;


import android.content.Context;

import com.hyunnyapp.brainycopter.ui.Sprite.Align;


public class JoystickFactory 
{
	public enum JoystickType {
		NONE,
		ANALOGUE,
		ACCELERO,
	}
	
	public static JoystickBase createAnalogueJoystick(Context context, boolean isRollPitchJoystick,
													  JoystickListener analogueListener,
													  boolean yStickIsBounced)
	{
		AnalogueJoystick joy = new AnalogueJoystick(context, Align.NO_ALIGN, isRollPitchJoystick, yStickIsBounced);
		joy.setOnAnalogueChangedListener(analogueListener);
		
		return joy;
	}
	
	
	public static JoystickBase createAcceleroJoystick(Context context, boolean isRollPitchJoystick,
													  JoystickListener acceleroListener,
													  boolean yStickIsBounced)
	{
		AcceleratorJoystick joy = new AcceleratorJoystick(context, Align.NO_ALIGN, isRollPitchJoystick, yStickIsBounced);
		joy.setOnAnalogueChangedListener(acceleroListener);
		
		return joy;
	}
}