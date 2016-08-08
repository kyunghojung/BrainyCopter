package com.hyunnyapp.brainycopter.sensors;

public interface DeviceOrientationChangeDelegate
{
    public void onDeviceOrientationChanged(float[] orientation, float magneticHeading, int magnetoAccuracy);
}
