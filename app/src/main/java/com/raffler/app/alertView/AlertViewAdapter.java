package com.raffler.app.alertView;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.raffler.app.R;

import java.util.List;

public class AlertViewAdapter extends BaseAdapter {
    private List<String> mDatas;
    private List<String> mDestructive;
    public AlertViewAdapter(List<String> datas, List<String> destructive){
        this.mDatas =datas;
        this.mDestructive =destructive;
    }
    @Override
    public int getCount() {
        return mDatas.size();
    }

    @Override
    public Object getItem(int position) {
        return mDatas.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        String data= mDatas.get(position);
        Holder holder=null;
        View view =convertView;
        if(view==null){
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            view=inflater.inflate(R.layout.item_actionsheetbutton, null);
            holder=creatHolder(view);
            view.setTag(holder);
        }
        else{
            holder=(Holder) view.getTag();
        }
        holder.UpdateUI(parent.getContext(),data,position);
        return view;
    }
    public Holder creatHolder(View view){
        return new Holder(view);
    }
    class Holder {
        private ImageView imageView;
        private TextView tvAlert;

        public Holder(View view){
            tvAlert = (TextView) view.findViewById(R.id.tvAlert);
            imageView = (ImageView) view.findViewById(R.id.imgView);
        }
        public void UpdateUI(Context context, String data, int position){
            tvAlert.setText(data);
            /*if (data.equals(context.getString(R.string.vweet_abuse_title))) {
                imageView.setImageResource(R.drawable.ic_flag);
            } else if (data.equals(context.getString(R.string.vweet_user_ban))) {
                imageView.setImageResource(R.drawable.ic_block);
            } else if (data.equals(context.getString(R.string.vweet_user_follow))) {
                imageView.setImageResource(R.drawable.ic_foot);
            } else if (data.equals(context.getString(R.string.vweet_user_unfollow))) {
                imageView.setImageResource(R.drawable.ic_foot);
            } else {
                imageView.setVisibility(View.INVISIBLE);
            }*/

            imageView.setVisibility(View.INVISIBLE);

            if (mDestructive!= null && mDestructive.contains(data)){
                tvAlert.setTextColor(context.getResources().getColor(R.color.textColor_alert_button_destructive));
            }
            else{
                tvAlert.setTextColor(context.getResources().getColor(R.color.textColor_alert_button_others));
            }
        }
    }
}