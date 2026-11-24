package com.alamin.keymapper;

import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

public class OverlayService extends Service {

    WindowManager manager;
    LinearLayout panel;
    WindowManager.LayoutParams params;

    @Override
    public void onCreate() {
        super.onCreate();

        manager = (WindowManager) getSystemService(WINDOW_SERVICE);

        panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.HORIZONTAL);
        panel.setGravity(Gravity.CENTER_VERTICAL);
        panel.setPadding(12, 8, 12, 8);
        panel.setBackgroundColor(Color.rgb(20,20,20));

        TextView title = new TextView(this);
        title.setText(" AL-AMIN ");
        title.setTextColor(Color.rgb(212,175,55));
        title.setTextSize(14);
        panel.addView(title);

        TextView close = new TextView(this);
        close.setText("  X  ");
        close.setTextColor(Color.WHITE);
        close.setTextSize(16);
        panel.addView(close);

        params = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 30;
        params.y = 150;

        title.setOnTouchListener(new android.view.View.OnTouchListener() {
            int startX, startY;
            float touchX, touchY;

            @Override
            public boolean onTouch(android.view.View v, MotionEvent e) {
                if (e.getAction() == MotionEvent.ACTION_DOWN) {
                    startX = params.x;
                    startY = params.y;
                    touchX = e.getRawX();
                    touchY = e.getRawY();
                    return true;
                }

                if (e.getAction() == MotionEvent.ACTION_MOVE) {
                    params.x = startX + (int)(e.getRawX() - touchX);
                    params.y = startY + (int)(e.getRawY() - touchY);
                    manager.updateViewLayout(panel, params);
                    return true;
                }
                return true;
            }
        });

        close.setOnClickListener(v -> stopSelf());

        manager.addView(panel, params);
    }

    @Override
    public void onDestroy() {
        if (panel != null && panel.isAttachedToWindow()) {
            manager.removeView(panel);
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
