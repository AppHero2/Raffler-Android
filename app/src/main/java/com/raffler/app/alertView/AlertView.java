package com.raffler.app.alertView;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.raffler.app.R;
import com.raffler.app.classes.AppConsts;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AlertView {
    public enum Style{
        ActionSheet,
        Alert
    }
    private final FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM
    );
    public static final int HORIZONTAL_BUTTONS_MAXCOUNT = 2;
    public static final int CANCELPOSITION = -1;

    private String headerImage;
    private String title;
    private String msg;
    private String[] destructive;
    private String[] others;
    private List<String> mDestructive;
    private List<String> mOthers;
    private String cancel;
    private ArrayList<String> mDatas = new ArrayList<String>();

    private WeakReference<Context> contextWeak;
    private ViewGroup contentContainer;
    private ViewGroup decorView;
    private ViewGroup rootView;
    private ViewGroup loAlertHeader;

    private Style style = Style.Alert;

    private OnDismissListener onDismissListener;
    private OnItemClickListener onItemClickListener;
    private boolean isShowing;

    private Animation outAnim;
    private Animation inAnim;
    private int gravity = Gravity.CENTER;

    public AlertView(Builder builder) {
        this.contextWeak = new WeakReference<>(builder.context);
        this.style = builder.style;
        this.headerImage = builder.headerImage;
        this.title = builder.title;
        this.msg = builder.msg;
        this.cancel = builder.cancel;
        this.destructive = builder.destructive;
        this.others = builder.others;
        this.onItemClickListener = builder.onItemClickListener;

        initData(title, msg, cancel, destructive, others);
        initViews();
        init();
        initEvents();
    }

    public AlertView(String title, String msg, String cancel, String[] destructive, String[] others, Context context, Style style, OnItemClickListener onItemClickListener){
        this.contextWeak = new WeakReference<>(context);
        if(style != null)this.style = style;
        this.onItemClickListener = onItemClickListener;

        initData(title, msg, cancel, destructive, others);
        initViews();
        init();
        initEvents();
    }

    protected void initData(String title, String msg, String cancel, String[] destructive, String[] others) {

        this.title = title;
        this.msg = msg;
        if (destructive != null){
            this.mDestructive = Arrays.asList(destructive);
            this.mDatas.addAll(mDestructive);
        }
        if (others != null){
            this.mOthers = Arrays.asList(others);
            this.mDatas.addAll(mOthers);
        }
        if (cancel != null){
            this.cancel = cancel;
            if(style == Style.Alert && mDatas.size() < HORIZONTAL_BUTTONS_MAXCOUNT){
                this.mDatas.add(0,cancel);
            }
        }

    }
    protected void initViews(){
        Context context = contextWeak.get();
        if(context == null) return;
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        decorView = (ViewGroup) ((Activity)context).getWindow().getDecorView().findViewById(android.R.id.content);
        rootView = (ViewGroup) layoutInflater.inflate(R.layout.layout_alertview, decorView, false);
        rootView.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ));
        contentContainer = (ViewGroup) rootView.findViewById(R.id.content_container);
        int margin_alert_left_right = 0;
        switch (style){
            case ActionSheet:
                params.gravity = Gravity.BOTTOM;
                margin_alert_left_right = context.getResources().getDimensionPixelSize(R.dimen.margin_actionsheet_left_right);
                params.setMargins(margin_alert_left_right,0,margin_alert_left_right,margin_alert_left_right);
                contentContainer.setLayoutParams(params);
                gravity = Gravity.BOTTOM;
                //contentContainer.setBackgroundColor(context.getResources().getColor(R.color.color_Background));
                initActionSheetViews(layoutInflater);
                break;
            case Alert:
                params.gravity = Gravity.CENTER;
                margin_alert_left_right = context.getResources().getDimensionPixelSize(R.dimen.margin_alert_left_right);
                params.setMargins(margin_alert_left_right,0,margin_alert_left_right,0);
                contentContainer.setLayoutParams(params);
                gravity = Gravity.CENTER;
                initAlertViews(layoutInflater);
                break;
        }
    }

    protected void initHeaderView(ViewGroup viewGroup){
        loAlertHeader = (ViewGroup) viewGroup.findViewById(R.id.loAlertHeader);
        TextView tvAlertTitle = (TextView) viewGroup.findViewById(R.id.tvAlertTitle);
        TextView tvAlertMsg = (TextView) viewGroup.findViewById(R.id.tvAlertMsg);
        if (headerImage != null) {
            ImageView imgHeader = (ImageView) viewGroup.findViewById(R.id.imgHeader);
            AppConsts.setProfileImage(headerImage, imgHeader);
        } else {
            ImageView imgHeader = (ImageView) viewGroup.findViewById(R.id.imgHeader);
            if (imgHeader != null)
                imgHeader.setVisibility(View.GONE);
        }
        if(title != null) {
            tvAlertTitle.setText(title);
        }else{
            tvAlertTitle.setVisibility(View.GONE);
        }
        if(msg != null) {
            tvAlertMsg.setText(msg);
        }else{
            tvAlertMsg.setVisibility(View.GONE);
        }
    }
    protected void initListView(){
        Context context = contextWeak.get();
        if(context == null) return;

        ListView alertButtonListView = (ListView) contentContainer.findViewById(R.id.alertButtonListView);

        if(cancel != null && style == Style.Alert){
            View itemView = LayoutInflater.from(context).inflate(R.layout.item_actionsheetbutton, null);
            TextView tvAlert = (TextView) itemView.findViewById(R.id.tvAlert);
            tvAlert.setText(cancel);
            tvAlert.setClickable(true);
            tvAlert.setTypeface(Typeface.DEFAULT_BOLD);
            tvAlert.setTextColor(context.getResources().getColor(R.color.textColor_alert_button_cancel));
            tvAlert.setBackgroundResource(R.drawable.bg_alertbutton_bottom);
            tvAlert.setOnClickListener(new OnTextClickListener(CANCELPOSITION));
            alertButtonListView.addFooterView(itemView);
        }
        AlertViewAdapter adapter = new AlertViewAdapter(mDatas,mDestructive);
        alertButtonListView.setAdapter(adapter);
        alertButtonListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                if(onItemClickListener != null)onItemClickListener.onItemClick(AlertView.this,position);
                dismiss();
            }
        });
    }
    protected void initActionSheetViews(LayoutInflater layoutInflater) {
        ViewGroup viewGroup = (ViewGroup) layoutInflater.inflate(R.layout.layout_alertview_actionsheet,contentContainer);
        initHeaderView(viewGroup);

        initListView();
        TextView tvAlertCancel = (TextView) contentContainer.findViewById(R.id.tvAlertCancel);
        if(cancel != null){
            tvAlertCancel.setVisibility(View.VISIBLE);
            tvAlertCancel.setText(cancel);
        }
        tvAlertCancel.setOnClickListener(new OnTextClickListener(CANCELPOSITION));
    }
    protected void initAlertViews(LayoutInflater layoutInflater) {
        Context context = contextWeak.get();
        if(context == null) return;

        ViewGroup viewGroup = (ViewGroup) layoutInflater.inflate(R.layout.layout_alertview_alert, contentContainer);
        initHeaderView(viewGroup);

        int position = 0;

        if(mDatas.size()<=HORIZONTAL_BUTTONS_MAXCOUNT){
            ViewStub viewStub = (ViewStub) contentContainer.findViewById(R.id.viewStubHorizontal);
            viewStub.inflate();
            LinearLayout loAlertButtons = (LinearLayout) contentContainer.findViewById(R.id.loAlertButtons);
            for (int i = 0; i < mDatas.size(); i ++) {
                if (i != 0){
                    View divier = new View(context);
                    divier.setBackgroundColor(context.getResources().getColor(R.color.bgColor_divier));
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams((int)context.getResources().getDimension(R.dimen.size_divier), LinearLayout.LayoutParams.MATCH_PARENT);
                    loAlertButtons.addView(divier,params);
                }
                View itemView = LayoutInflater.from(context).inflate(R.layout.item_alertbutton, null);
                TextView tvAlert = (TextView) itemView.findViewById(R.id.tvAlert);
                tvAlert.setClickable(true);

                if(mDatas.size() == 1){
                    tvAlert.setBackgroundResource(R.drawable.bg_alertbutton_bottom);
                }
                else if(i == 0){
                    tvAlert.setBackgroundResource(R.drawable.bg_alertbutton_left);
                }
                else if(i == mDatas.size() - 1){
                    tvAlert.setBackgroundResource(R.drawable.bg_alertbutton_right);
                }
                String data = mDatas.get(i);
                tvAlert.setText(data);

                if (data == cancel){
                    tvAlert.setTypeface(Typeface.DEFAULT_BOLD);
                    tvAlert.setTextColor(context.getResources().getColor(R.color.textColor_alert_button_cancel));
                    tvAlert.setOnClickListener(new OnTextClickListener(CANCELPOSITION));
                    position = position - 1;
                }
                else if (mDestructive!= null && mDestructive.contains(data)){
                    tvAlert.setTextColor(context.getResources().getColor(R.color.textColor_alert_button_destructive));
                }

                tvAlert.setOnClickListener(new OnTextClickListener(position));
                position++;
                loAlertButtons.addView(itemView,new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            }
        }
        else{
            ViewStub viewStub = (ViewStub) contentContainer.findViewById(R.id.viewStubVertical);
            viewStub.inflate();
            initListView();
        }
    }

    protected void init() {
        inAnim = getInAnimation();
        outAnim = getOutAnimation();
    }
    protected void initEvents() {
    }
    public AlertView addExtView(View extView){
        loAlertHeader.addView(extView);
        return this;
    }

    private void onAttached(View view) {
        isShowing = true;
        decorView.addView(view);
        contentContainer.startAnimation(inAnim);
    }


    public void show() {
        if (isShowing()) {
            return;
        }
        onAttached(rootView);
    }


    public boolean isShowing() {
        return rootView.getParent() != null && isShowing;
    }

    public void dismiss() {
        //消失动画
        outAnim.setAnimationListener(outAnimListener);
        contentContainer.startAnimation(outAnim);
    }

    public void dismissImmediately() {
        decorView.removeView(rootView);
        isShowing = false;
        if(onDismissListener != null){
            onDismissListener.onDismiss(this);
        }

    }

    public Animation getInAnimation() {
        Context context = contextWeak.get();
        if(context == null) return null;

        int res = AlertAnimateUtil.getAnimationResource(this.gravity, true);
        return AnimationUtils.loadAnimation(context, res);
    }

    public Animation getOutAnimation() {
        Context context = contextWeak.get();
        if(context == null) return null;

        int res = AlertAnimateUtil.getAnimationResource(this.gravity, false);
        return AnimationUtils.loadAnimation(context, res);
    }

    public AlertView setOnDismissListener(OnDismissListener onDismissListener) {
        this.onDismissListener = onDismissListener;
        return this;
    }

    class OnTextClickListener implements View.OnClickListener{

        private int position;
        public OnTextClickListener(int position){
            this.position = position;
        }
        @Override
        public void onClick(View view) {
            if(onItemClickListener != null)onItemClickListener.onItemClick(AlertView.this,position);
            dismiss();
        }
    }
    private Animation.AnimationListener outAnimListener = new Animation.AnimationListener() {
        @Override
        public void onAnimationStart(Animation animation) {

        }

        @Override
        public void onAnimationEnd(Animation animation) {
            dismissImmediately();
        }

        @Override
        public void onAnimationRepeat(Animation animation) {

        }
    };

    public void setMarginBottom(int marginBottom){
        Context context = contextWeak.get();
        if(context == null) return;

        int margin_alert_left_right = context.getResources().getDimensionPixelSize(R.dimen.margin_alert_left_right);
        params.setMargins(margin_alert_left_right,0,margin_alert_left_right,marginBottom);
        contentContainer.setLayoutParams(params);
    }
    public AlertView setCancelable(boolean isCancelable) {
        View view = rootView.findViewById(R.id.outmost_container);

        if (isCancelable) {
            view.setOnTouchListener(onCancelableTouchListener);
        }
        else{
            view.setOnTouchListener(null);
        }
        return this;
    }

    private final View.OnTouchListener onCancelableTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                dismiss();
            }
            return false;
        }
    };

    public static class Builder {
        private Context context;
        private Style style;
        private String headerImage;
        private String title;
        private String msg;
        private String cancel;
        private String[] destructive;
        private String[] others;
        private OnItemClickListener onItemClickListener;

        public Builder setContext(Context context) {
            this.context = context;
            return this;
        }

        public Builder setStyle(Style style) {
            if(style != null) {
                this.style = style;
            }
            return this;
        }


        public Builder setHeaderImage(String link) {
            this.headerImage = link;
            return this;
        }

        public Builder setTitle(String title) {
            this.title = title;
            return this;
        }

        public Builder setMessage(String msg) {
            this.msg = msg;
            return this;
        }

        public Builder setCancelText(String cancel) {
            this.cancel = cancel;
            return this;
        }

        public Builder setDestructive(String... destructive) {
            this.destructive = destructive;
            return this;
        }

        public Builder setOthers(String[] others) {
            this.others = others;
            return this;
        }

        public Builder setOnItemClickListener(OnItemClickListener onItemClickListener) {
            this.onItemClickListener = onItemClickListener;
            return this;
        }

        public AlertView build() {
            return new AlertView(this);
        }
    }
}
