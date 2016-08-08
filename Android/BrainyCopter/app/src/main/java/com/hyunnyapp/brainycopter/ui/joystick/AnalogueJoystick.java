/*
 * AnalogueJoystick
 *
 *  Created on: May 26, 2011
 *      Author: Dmytro Baryskyy
 */

package com.hyunnyapp.brainycopter.ui.joystick;

import android.content.Context;

import com.hyunnyapp.brainycopter.R;

public class AnalogueJoystick
        extends JoystickBase {

    public AnalogueJoystick(Context context, Align align, boolean isRollPitchJoystick, boolean yStickIsBounced) {
        super(context, align, isRollPitchJoystick, yStickIsBounced);
    }

    @Override
    protected int getBackgroundDrawableId() {
        return R.drawable.joystick_bg;
    }

    @Override
    protected int getTumbDrawableId() {
        return R.drawable.joystick_rudder_throttle_new;
    }
}
