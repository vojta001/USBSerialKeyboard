package tk.greenscreener.usbserialkeyboard;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;

import android.content.res.AssetManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.DialogFragment;


import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.util.Xml;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.content.ClipDescription.MIMETYPE_TEXT_PLAIN;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";


    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case UsbService.ACTION_USB_PERMISSION_GRANTED: // USB PERMISSION GRANTED
                    Toast.makeText(context, "USB Ready", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_PERMISSION_NOT_GRANTED: // USB PERMISSION NOT GRANTED
                    Toast.makeText(context, "USB Permission not granted", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_NO_USB: // NO USB CONNECTED
                    Toast.makeText(context, "No USB connected", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_DISCONNECTED: // USB DISCONNECTED
                    Toast.makeText(context, "USB disconnected", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_NOT_SUPPORTED: // USB NOT SUPPORTED
                    Toast.makeText(context, "USB device not supported", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };
    private UsbService usbService;
    private MyHandler mHandler;
    Button clipboardButton;
    Button sendButton;
    EditText editText;
    static Boolean keyboardReady = false;
    static Boolean canUseClipboard;
    static Integer selectedLayout = 0;
    private final ServiceConnection usbConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
            usbService = ((UsbService.UsbBinder) arg1).getService();
            usbService.setHandler(mHandler);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            usbService = null;
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        setFilters();  // Start listening notifications from UsbService
        startService(UsbService.class, usbConnection, null); // Start UsbService(if it was not started before) and Bind it
    }

    private void startService(Class<?> service, ServiceConnection serviceConnection, Bundle extras) {
        if (!UsbService.SERVICE_CONNECTED) {
            Intent startService = new Intent(this, service);
            if (extras != null && !extras.isEmpty()) {
                Set<String> keys = extras.keySet();
                for (String key : keys) {
                    String extra = extras.getString(key);
                    startService.putExtra(key, extra);
                }
            }
            startService(startService);
        }
        Intent bindingIntent = new Intent(this, service);
        bindService(bindingIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void setFilters() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbService.ACTION_USB_PERMISSION_GRANTED);
        filter.addAction(UsbService.ACTION_NO_USB);
        filter.addAction(UsbService.ACTION_USB_DISCONNECTED);
        filter.addAction(UsbService.ACTION_USB_NOT_SUPPORTED);
        filter.addAction(UsbService.ACTION_USB_PERMISSION_NOT_GRANTED);
        registerReceiver(mUsbReceiver, filter);
    }





    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(mUsbReceiver);
        unbindService(usbConnection);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_keyboardMenu:
                LayoutDialogFragment layoutDialog = new LayoutDialogFragment();
                layoutDialog.show(getSupportFragmentManager(), "layout");
                return true;
            case R.id.action_help:
                HelpDialogFragment helpDialog = new HelpDialogFragment();
                helpDialog.show(getSupportFragmentManager(), "help");
                return true;
            case R.id.action_about:
                AboutDialogFragment aboutDialog = new AboutDialogFragment();
                aboutDialog.show(getSupportFragmentManager(), "about");
                return true;
            default:
                return super.onOptionsItemSelected(item);

        }
    }
    public static class HelpDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            SpannableString s = new SpannableString("Welcome to the USBSerialKeyboard app. \n\nFirst connect your USBSerialKeyboard and then android should automatically ask you if you want to launch the application. If all the buttons are greyed out, the USBSerialKeyboard isn't correctly connected. Please make sure both USB ports are plugged in with the PCBs on the receiving computer's side. If that doesn't work, try relaunching the app. \n\nThe device obviously cannot detect the keyboard layout of the receiving computer, so we have to translate the keystrokes accordingly. Please select the keyboard layout of the receiving computer in the menu located under the keyboard shaped button. Some characters cannot be typed due to not being present in the keyboard layout. To add a layout send me a PR in the GitHub repo. ");
            Linkify.addLinks(s, Linkify.WEB_URLS);
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(s);
            builder.setTitle("Help");
            builder.setCancelable(true);
            builder.setPositiveButton("Close",null);
            return builder.create();
        }
    }
    public static class AboutDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            SpannableString s = new SpannableString("Companion application for the USBSerialKeyboard. \nMade by Greenscreener, using the library UsbSerial and example code by felHR85 with a lot of help from Stack Overflow.\n\nv1.0\nGithub: github.com/Greenscreener/USBSerialKeyboard");
            Linkify.addLinks(s, Linkify.WEB_URLS);
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(s);
            builder.setTitle("About");
            builder.setCancelable(true);
            builder.setPositiveButton("Close",null);
            return builder.create();
        }
    }
    public static class LayoutDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("Please select keyboard layout.");
            builder.setSingleChoiceItems(R.array.layout_menu, selectedLayout, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    selectedLayout = i;
                }
            });
            builder.setCancelable(true);
            builder.setPositiveButton("Close",null);
            return builder.create();
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mHandler = new MyHandler(this);

        Toolbar mainToolbar = (Toolbar) findViewById(R.id.mainToolbar);
        setSupportActionBar(mainToolbar);

        clipboardButton = (Button) findViewById(R.id.clipboardButton);
        sendButton = (Button) findViewById(R.id.sendButton);
        editText = (EditText) findViewById(R.id.editText);

        clipboardButton.setEnabled(false);
        sendButton.setEnabled(false);
        editText.setEnabled(false);

        final ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        String pasteData = "";

        if (!(clipboard.hasPrimaryClip())) {
            clipboardButton.setEnabled(false);
            canUseClipboard = false;
        } else if (!(clipboard.getPrimaryClipDescription().hasMimeType(MIMETYPE_TEXT_PLAIN))) {
            clipboardButton.setEnabled(false);
            canUseClipboard = false;
        } else {
            if (keyboardReady) {
                clipboardButton.setEnabled(true);
            }
            canUseClipboard = true;
        }


        clipboardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String pasteData = "";
                ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
                pasteData = item.getText().toString();
                if (pasteData == null) {
                    Uri pasteUri = item.getUri();
                    if (pasteUri != null) {
                        pasteData = pasteUri.toString();
                        return;
                    } else {
                        Log.e(TAG,"Clipboard contains an invalid data type");
                        return;
                    }
                }
                sendData(pasteData);

            }
        });

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!editText.getText().toString().equals("")) {
                    String data = editText.getText().toString();
                    sendData(data);
                }
            }
        });



    }

    private void sendData(final String data) {
        if (usbService != null) {
            byte[] nullByte = {0};
            usbService.write(nullByte);
            /*for (int i = 0; i < data.length(); i++) {
                usbService.write((translateChar(data.charAt(i)) + "").getBytes());
            }*/
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i < data.length(); i++) {
                        usbService.write((Layouts.translateChar(data.charAt(i), selectedLayout) + "").getBytes());
                        try {
                            Thread.sleep(27);
                        } catch (InterruptedException e) {
                            Thread.interrupted();
                        }
                    }
                }
            });
            thread.start();
            editText.setText("");
        }
    }
    /*
    private char translateChar(char inputChar) {
        char outputChar;
        String selectedLayoutName = getResources().getStringArray(R.array.layouts)[selectedLayout];
        String regexString = "<\\?xml version=\"1.0\" encoding=\"UTF-8\"\\?><root>.*<" + selectedLayoutName + ".*>.*<k><i>\\Q" + inputChar + "\\E<\\/i><o>(.)<\\/o>.*<\\/" + selectedLayoutName + ">";
        Pattern regexPattern = Pattern.compile(regexString);
        InputStream in = null;
        try {
            in = getAssets().open("layouts.xml");
            Scanner scanner = new Scanner(in, "UTF-8");
            String match = scanner.findWithinHorizon(regexPattern, 0);
            if (match != null) {
                outputChar = scanner.match().group(1).charAt(0);
            } else {
                switch (inputChar) {
                    case ' ':
                        outputChar = ' ';
                        break;
                    case '\n':
                        outputChar = '\n';
                        break;
                    default:
                        outputChar = Character.MIN_VALUE;
                        Log.e(TAG,"Character not found.");
                        break;
                }

            }


        } catch (IOException e) {
            e.printStackTrace();
            outputChar = Character.MIN_VALUE;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }

        return outputChar;
    }
    */
    private static class MyHandler extends Handler {

        private final WeakReference<MainActivity> mActivity;

        public MyHandler(MainActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UsbService.MESSAGE_FROM_SERIAL_PORT:
                    String data = (String) msg.obj;
                    if (data.trim().equals("ready")) {
                        if (!keyboardReady) {
                            keyboardReady = true;
                            if (canUseClipboard) {
                                mActivity.get().clipboardButton.setEnabled(true);
                            }
                            mActivity.get().sendButton.setEnabled(true);
                            mActivity.get().editText.setEnabled(true);
                            Toast.makeText(mActivity.get(), "USBSerialKeyboard ready", Toast.LENGTH_SHORT).show();
                        }
                    }

                    break;
            }
        }
    }




}
