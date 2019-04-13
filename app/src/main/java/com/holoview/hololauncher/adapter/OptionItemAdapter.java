/*
 * Copyright (C) 2016 hejunlin <hejunlin2013@gmail.com>
 * Github:https://github.com/hejunlin2013/TVSample
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.holoview.hololauncher.adapter;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.holoview.hololauncher.R;
import com.holoview.hololauncher.bean.LauncherApp;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by hejunlin on 2015/10/16.
 * blog: http://blog.csdn.net/hejjunlin
 */
public class OptionItemAdapter extends RecyclerView.Adapter<OptionItemAdapter.ViewHolder> {


    private List<String> launcherApps = new ArrayList<>();
    private Context mContext;
    private OnOptionItemClickLister mOnOptionItemClickLister;
    private static final String TAG = OptionItemAdapter.class.getSimpleName();

    public interface OnOptionItemClickLister {
        void onOptionItemClick(int position);
    }

    public OptionItemAdapter(Context context, List<String> launcherApps) {
        super();
        this.mContext = context;
        this.launcherApps =launcherApps;
    }


    public void setOnOptionItemClickLister(OnOptionItemClickLister onOptionItemClickLister) {
        this.mOnOptionItemClickLister = onOptionItemClickLister;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.rv_item_option, viewGroup, false);
        ViewHolder holder = new ViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(final ViewHolder viewHolder, final int position) {
        if (launcherApps.size() == 0) {
            Log.d(TAG, "mDataset has no data!");
            return;
        }
        PackageManager pm = mContext.getPackageManager();
        viewHolder.mTextView.setText(launcherApps.get(position));

        viewHolder.itemView.setTag(position);
        viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOnOptionItemClickLister != null) {
                    mOnOptionItemClickLister.onOptionItemClick(position);
                }
            }
        });

    }

    @Override
    public int getItemCount() {
        return launcherApps.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        public TextView mTextView;
        private ImageView mImageView;

        public ViewHolder(View itemView) {
            super(itemView);
            mTextView = (TextView) itemView.findViewById(R.id.item_option_tv);
        }
    }

}
