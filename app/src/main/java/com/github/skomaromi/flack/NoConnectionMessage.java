package com.github.skomaromi.flack;

import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class NoConnectionMessage extends RelativeLayout {
    public NoConnectionMessage(Context context) {
        super(context);
        inflate(context, R.layout.element_no_connection_message, this);
        init();
    }

    public NoConnectionMessage(Context context, AttributeSet attrs) {
        super(context, attrs);
        inflate(context, R.layout.element_no_connection_message, this);
        init();
    }

    public NoConnectionMessage(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate(context, R.layout.element_no_connection_message, this);
        init();
    }

    public NoConnectionMessage(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        inflate(context, R.layout.element_no_connection_message, this);
        init();
    }

    private void init() {
        ButterKnife.bind(this);
    }

    @OnClick(R.id.noconnmsg_btn_configure)
    public void configureButtonClicked() {
        Context context = getContext();

        Intent serverInputActivity = new Intent(
                context, ServerInputActivity.class
        );

        serverInputActivity.putExtra(ServerInputActivity.KEY_FROM_STARTACTIVITY, false);

        context.startActivity(serverInputActivity);
    }

}
