package com.alamin.keymapper;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.os.Build;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.util.DisplayMetrics;

public class KeyMapperService extends AccessibilityService {

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    public boolean onKeyEvent(KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_DOWN ||
            event.getRepeatCount() > 0) {
            return false;
        }

        String pressed = KeyEvent.keyCodeToString(event.getKeyCode())
                .replace("KEYCODE_", "");

        getSharedPreferences("keymapper", MODE_PRIVATE)
                .edit()
                .putString("last_global_key", pressed)
                .apply();

        android.content.SharedPreferences prefs =
                getSharedPreferences("keymapper", MODE_PRIVATE);

        for (String action : new String[]{
                "Jump", "Crouch", "Reload"
        }) {
            String mapped = prefs.getString(action, defaultKey(action));

            if (matches(pressed, mapped)) {
                String id = action.toLowerCase();
                float dx = id.equals("jump") ? 88 :
                           id.equals("crouch") ? 78 : 65;
                float dy = id.equals("reload") ? 90 : 76;
                tapAt(id, dx, dy);
                break;
            }
        }

        return false;
    }

    private String defaultKey(String action) {
        if (action.equals("Jump")) return "SPACE";
        if (action.equals("Crouch")) return "C";
        if (action.equals("Reload")) return "R";
        return "";
    }

    private boolean matches(String pressed, String mapped) {
        if (mapped == null) return false;
        if (mapped.equalsIgnoreCase("SPACE")) {
            return pressed.equals("SPACE");
        }
        return pressed.equalsIgnoreCase(mapped);
    }

    private void tapAt(String action, float defaultX, float defaultY) {
        if (Build.VERSION.SDK_INT < 24) return;

        DisplayMetrics dm = getResources().getDisplayMetrics();
        android.content.SharedPreferences prefs =
                getSharedPreferences("keymapper", MODE_PRIVATE);

        float px = prefs.getFloat("touch_" + action + "_x", defaultX);
        float py = prefs.getFloat("touch_" + action + "_y", defaultY);

        Path path = new Path();
        path.moveTo(dm.widthPixels * px / 100f,
                    dm.heightPixels * py / 100f);

        GestureDescription gesture =
                new GestureDescription.Builder()
                .addStroke(new GestureDescription.StrokeDescription(
                        path, 0, 60))
                .build();

        dispatchGesture(gesture, null, null);
    }
}
