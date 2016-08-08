package com.hyunnyapp.brainycopter.ipc.view;

import android.content.Context;

import java.util.ArrayList;

public interface OnGalleryItemClick {
    public void onClick(int position,Context context);
    public void delete(ArrayList<Long> selects);
    public void destroy();
}
