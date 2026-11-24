package com.alamin.keymapper;

import android.app.*;
import android.os.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.*;
import android.view.inputmethod.*;
import android.content.*;
import android.hardware.input.InputManager;
import android.view.InputDevice;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {

    int gold = Color.rgb(212,175,55);
    int dark = Color.rgb(10,10,10);
    int cardColor = Color.rgb(27,27,27);
    int white = Color.WHITE;

    LinearLayout root;
    TextView inputStatus;
    android.content.SharedPreferences prefs;

    String[][] controls = {
        {"Move Forward","W"},
        {"Move Backward","S"},
        {"Move Left","A"},
        {"Move Right","D"},
        {"Jump","SPACE"},
        {"Crouch","C"},
        {"Reload","R"},
        {"Fire","MOUSE LEFT"},
        {"Scope","MOUSE RIGHT"}
    };

    TextView makeText(String value,int size,int color) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setPadding(8,10,8,10);
        return t;
    }

    void addTitle(String value) {
        TextView t = makeText(value,19,gold);
        t.setTypeface(null,Typeface.BOLD);
        root.addView(t);
    }

    void addCard(String name) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(14,8,14,8);
        box.setBackgroundColor(cardColor);

        LinearLayout.LayoutParams p =
            new LinearLayout.LayoutParams(-1,-2);
        p.setMargins(0,6,0,6);

        root.addView(box,p);
        box.addView(makeText(name,16,gold));

        TextView value = makeText("",14,white);
        box.addView(value);

        Button change = new Button(this);
        change.setText("CHANGE KEY");
        change.setTextColor(gold);
        change.setBackgroundColor(Color.rgb(45,40,25));
        box.addView(change);

        updateValue(name,value);

        change.setOnClickListener(v -> {
            EditText input = new EditText(this);
            input.setSingleLine(true);
            input.setHint("Enter key name (e.g. Q, E, F)");
            input.setText(prefs.getString(name,
                defaultKey(name)));

            new AlertDialog.Builder(this)
                .setTitle("Change " + name)
                .setView(input)
                .setPositiveButton("SAVE",(d,w) -> {
                    String key = input.getText().toString()
                        .trim().toUpperCase(Locale.ROOT);

                    if(key.isEmpty()) {
                        Toast.makeText(this,
                            "Key cannot be empty",
                            Toast.LENGTH_SHORT).show();
                        return;
                    }

                    prefs.edit().putString(name,key).apply();
                    updateValue(name,value);
                    Toast.makeText(this,"Key Saved!",
                        Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("CANCEL",null)
                .show();
        });
    }

    String defaultKey(String name) {
        for(String[] c:controls) {
            if(c[0].equals(name)) return c[1];
        }
        return "";
    }

    void updateValue(String name,TextView value) {
        value.setText("Current Key: " +
            prefs.getString(name,defaultKey(name)));
    }



    void manageProfiles() {
        String[] profiles = {"Free Fire", "Custom 1", "Custom 2"};

        new AlertDialog.Builder(this)
            .setTitle("SELECT CONTROL PROFILE")
            .setSingleChoiceItems(profiles,
                java.util.Arrays.asList(profiles).indexOf(
                    prefs.getString("active_profile", "Free Fire")),
                (dialog, which) -> {
                    String oldProfile = prefs.getString(
                        "active_profile", "Free Fire");
                    String newProfile = profiles[which];

                    android.content.SharedPreferences.Editor editor =
                        prefs.edit();

                    for (String[] control : controls) {
                        String name = control[0];
                        String current = prefs.getString(
                            name, defaultKey(name));
                        editor.putString(
                            "profile_" + oldProfile + "_" + name,
                            current);
                    }

                    for (String[] control : controls) {
                        String name = control[0];
                        String saved = prefs.getString(
                            "profile_" + newProfile + "_" + name,
                            defaultKey(name));
                        editor.putString(name, saved);
                    }

                    editor.putString("active_profile", newProfile);
                    editor.apply();

                    dialog.dismiss();
                    recreate();
                    Toast.makeText(this,
                        newProfile + " Loaded",
                        Toast.LENGTH_SHORT).show();
                })
            .setNegativeButton("CANCEL", null)
            .show();
    }


    void startOverlay() {
        if (!android.provider.Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(
                android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                android.net.Uri.parse("package:" + getPackageName())
            );
            startActivity(intent);
            Toast.makeText(this,
                "Allow AL-AMIN KEY MAPPER overlay permission",
                Toast.LENGTH_LONG).show();
            return;
        }

        startService(new Intent(this, OverlayService.class));
        Toast.makeText(this, "Overlay Started",
            Toast.LENGTH_SHORT).show();
    }


    void stopOverlay() {
        stopService(new Intent(this, OverlayService.class));
        Toast.makeText(this, "Overlay Stopped",
            Toast.LENGTH_SHORT).show();
    }

    void openAccessibility() {
        startActivity(new Intent(
            android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS));
    }

    void openAppSettings() {
        Intent intent = new Intent(
            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(android.net.Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }

    void detectDevices() {
        int[] ids = InputDevice.getDeviceIds();
        boolean keyboard = false;
        boolean mouse = false;
        StringBuilder details = new StringBuilder();

        for(int id:ids) {
            InputDevice device = InputDevice.getDevice(id);
            if(device == null) continue;

            int sources = device.getSources();

            if((sources & InputDevice.SOURCE_KEYBOARD)
                == InputDevice.SOURCE_KEYBOARD) {
                keyboard = true;
            }

            if((sources & InputDevice.SOURCE_MOUSE)
                == InputDevice.SOURCE_MOUSE) {
                mouse = true;
            }

            details.append(device.getName()).append("\n");
        }

        String result =
            "Keyboard: " + (keyboard ? "CONNECTED" : "NOT DETECTED")
            + "\nMouse: " + (mouse ? "CONNECTED" : "NOT DETECTED");

        new AlertDialog.Builder(this)
            .setTitle("USB DEVICE STATUS")
            .setMessage(result + "\n\nDetected Devices:\n" + details)
            .setPositiveButton("OK",null)
            .show();
    }


    @Override
    public boolean onKeyDown(int keyCode, android.view.KeyEvent event) {
        if (inputStatus != null) {
            inputStatus.setText(
                "Keyboard Key: " + event.getDisplayLabel()
                + "  |  Code: " + keyCode);
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onGenericMotionEvent(
        android.view.MotionEvent event) {

        if (inputStatus != null) {
            if ((event.getSource()
                & android.view.InputDevice.SOURCE_MOUSE)
                == android.view.InputDevice.SOURCE_MOUSE) {

                inputStatus.setText(
                    "Mouse Movement: X=" + event.getX()
                    + " Y=" + event.getY());
            } else {
                inputStatus.setText("Mouse / Pointer Input Detected");
            }
        }
        return super.onGenericMotionEvent(event);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences("keymapper",MODE_PRIVATE);

        ScrollView scroll = new ScrollView(this);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(18,20,18,20);
        root.setBackgroundColor(dark);

        TextView logo = makeText("AL-AMIN",30,gold);
        logo.setGravity(Gravity.CENTER);
        logo.setTypeface(null,Typeface.BOLD);
        root.addView(logo);

        TextView heading = makeText("KEY MAPPER",20,white);
        heading.setGravity(Gravity.CENTER);
        root.addView(heading);

        TextView subtitle = makeText(
            "GAMING CONTROL CENTER",11,gold);
        subtitle.setGravity(Gravity.CENTER);
        root.addView(subtitle);

        Button devices = new Button(this);
        devices.setText("DETECT KEYBOARD & MOUSE");
        devices.setTextColor(gold);
        devices.setBackgroundColor(cardColor);
        root.addView(devices);
        devices.setOnClickListener(v -> detectDevices());

        Button overlay = new Button(this);
        overlay.setText("START FLOATING OVERLAY");
        overlay.setTextColor(gold);
        overlay.setBackgroundColor(cardColor);
        root.addView(overlay);
        overlay.setOnClickListener(v -> startOverlay());

        Button stop = new Button(this);
        stop.setText("STOP FLOATING OVERLAY");
        stop.setTextColor(white);
        stop.setBackgroundColor(Color.rgb(100,35,25));
        root.addView(stop);
        stop.setOnClickListener(v -> stopOverlay());

        Button accessibility = new Button(this);
        accessibility.setText("ACCESSIBILITY SETTINGS");
        accessibility.setTextColor(gold);
        accessibility.setBackgroundColor(cardColor);
        root.addView(accessibility);
        accessibility.setOnClickListener(v -> openAccessibility());

        Button settings = new Button(this);
        settings.setText("APP SETTINGS");
        settings.setTextColor(gold);
        settings.setBackgroundColor(cardColor);
        root.addView(settings);
        settings.setOnClickListener(v -> openAppSettings());




        addTitle("LIVE INPUT TEST");

        inputStatus = makeText(
            "Press a keyboard key or move your mouse",
            14, white);
        inputStatus.setBackgroundColor(cardColor);
        root.addView(inputStatus);

        addTitle("CUSTOM KEY MAPPING");

        for(String[] control:controls) {
            addCard(control[0]);
        }


        addTitle("PROFILE SETTINGS");

        Button profile = new Button(this);
        profile.setText("SELECT PROFILE");
        profile.setTextColor(gold);
        profile.setBackgroundColor(cardColor);
        root.addView(profile);
        profile.setOnClickListener(v -> manageProfiles());


        Button reset = new Button(this);
        reset.setText("RESET DEFAULT KEYS");
        reset.setTextColor(white);
        reset.setBackgroundColor(Color.rgb(100,35,25));
        root.addView(reset);

        reset.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                .setTitle("Reset Controls?")
                .setMessage("Restore all default keys?")
                .setPositiveButton("RESET",(d,w) -> {
                    prefs.edit().clear().apply();
                    recreate();
                })
                .setNegativeButton("CANCEL",null)
                .show();
        });

        addTitle("TOUCH POSITION SETTINGS");

        Button jumpPos = new Button(this);
        jumpPos.setText("SET JUMP POSITION");
        root.addView(jumpPos);
        jumpPos.setOnClickListener(v ->
            configureTouchPosition("Jump", "jump", 88, 76));

        Button crouchPos = new Button(this);
        crouchPos.setText("SET CROUCH POSITION");
        root.addView(crouchPos);
        crouchPos.setOnClickListener(v ->
            configureTouchPosition("Crouch", "crouch", 78, 76));

        Button reloadPos = new Button(this);
        reloadPos.setText("SET RELOAD POSITION");
        root.addView(reloadPos);
        reloadPos.setOnClickListener(v ->
            configureTouchPosition("Reload", "reload", 65, 90));

        Button interactPos = new Button(this);
        interactPos.setText("SET INTERACT POSITION");
        root.addView(interactPos);
        interactPos.setOnClickListener(v ->
            configureTouchPosition("Interact", "interact", 50, 85));

        TextView footer = makeText(
            "AL-AMIN KEY MAPPER  |  VERSION 1.0",
            11,gold);
        footer.setGravity(Gravity.CENTER);
        root.addView(footer);

        scroll.addView(root);
        setContentView(scroll);
    }

    private void configureTouchPosition(String label, String action,
                                        float defaultX, float defaultY) {
        android.widget.LinearLayout layout =
            new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(30, 15, 30, 10);

        android.widget.EditText xInput =
            new android.widget.EditText(this);
        xInput.setHint("X position (0-100%)");
        xInput.setInputType(2);

        android.widget.EditText yInput =
            new android.widget.EditText(this);
        yInput.setHint("Y position (0-100%)");
        yInput.setInputType(2);

        float x = prefs.getFloat("touch_" + action + "_x", defaultX);
        float y = prefs.getFloat("touch_" + action + "_y", defaultY);
        xInput.setText(String.valueOf(x));
        yInput.setText(String.valueOf(y));

        layout.addView(xInput);
        layout.addView(yInput);

        new AlertDialog.Builder(this)
            .setTitle(label + " Touch Position (%)")
            .setView(layout)
            .setPositiveButton("SAVE", (dialog, which) -> {
                try {
                    float newX = Float.parseFloat(xInput.getText().toString());
                    float newY = Float.parseFloat(yInput.getText().toString());

                    if (newX < 0 || newX > 100 ||
                        newY < 0 || newY > 100) {
                        new AlertDialog.Builder(this)
                            .setMessage("Enter values between 0 and 100.")
                            .setPositiveButton("OK", null)
                            .show();
                        return;
                    }

                    prefs.edit()
                        .putFloat("touch_" + action + "_x", newX)
                        .putFloat("touch_" + action + "_y", newY)
                        .apply();
                } catch (NumberFormatException e) {
                    new AlertDialog.Builder(this)
                        .setMessage("Invalid position.")
                        .setPositiveButton("OK", null)
                        .show();
                }
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }


}
