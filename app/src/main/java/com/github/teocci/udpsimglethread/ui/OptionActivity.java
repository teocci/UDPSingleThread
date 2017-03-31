package com.github.teocci.udpsimglethread.ui;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.github.teocci.udpsimglethread.R;
import com.github.teocci.udpsimglethread.utils.LogHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OptionActivity extends ListActivity
{
    public static final String TAG = LogHelper.makeLogTag(OptionActivity.class);

    // map keys
    private static final String TITLE = "title";
    private static final String DESCRIPTION = "description";
    private static final String CLASS_NAME = "class_name";

    private boolean isExit;

    /**
     * Each entry has three strings: the test title, the test description, and the name of
     * the activity class.
     */
    private static final String[][] MODES = {
            {"Server Mode",
                    "Start the application in Server Mode",
                    "CallModeActivity"},
            {"Client Mode",
                    "Start the application in Client Mode",
                    "ClientModeActivity"},
            {"Call Mode",
                    "Start the application in Call Mode",
                    "CallModeActivity"}
    };

    /**
     * Compares two list items.
     */
    private static final Comparator<Map<String, Object>> MODE_LIST_COMPARATOR =
            new Comparator<Map<String, Object>>()
            {
                @Override
                public int compare(Map<String, Object> map1, Map<String, Object> map2)
                {
                    String title1 = (String) map1.get(TITLE);
                    String title2 = (String) map2.get(TITLE);
                    return title1.compareTo(title2);
                }
            };


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_option);

        setListAdapter(new SimpleAdapter(
                this,
                createActivityList(),
                android.R.layout.two_line_list_item,
                new String[]{TITLE, DESCRIPTION},
                new int[]{android.R.id.text1, android.R.id.text2}
        ));
    }

    /**
     * Creates the list of activities from the string arrays.
     */
    private List<Map<String, Object>> createActivityList()
    {
        List<Map<String, Object>> testList = new ArrayList<Map<String, Object>>();

        for (String[] test : MODES) {
            Map<String, Object> tmp = new HashMap<String, Object>();
            tmp.put(TITLE, test[0]);
            tmp.put(DESCRIPTION, test[1]);
            Intent intent = new Intent();
            // Do the class name resolution here, so we crash up front rather than when the
            // activity list item is selected if the class name is wrong.
            try {
                Class cls = Class.forName("com.github.teocci.udpsimglethread.ui." + test[2]);
                intent.setClass(this, cls);
                tmp.put(CLASS_NAME, intent);
            } catch (ClassNotFoundException cnfe) {
                throw new RuntimeException("Unable to find " + test[2], cnfe);
            }
            testList.add(tmp);
        }

        Collections.sort(testList, MODE_LIST_COMPARATOR);

        return testList;
    }

    @Override
    protected void onListItemClick(ListView listView, View view, int position, long id)
    {
        Map<String, Object> map = (Map<String, Object>) listView.getItemAtPosition(position);
        Intent intent = (Intent) map.get(CLASS_NAME);
        startActivity(intent);
    }

    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item)
    {
        final LayoutInflater layoutInflater = LayoutInflater.from(this);
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        switch (item.getItemId()) {
            case R.id.actionAbout: {
                final View dialogView = layoutInflater.inflate(R.layout.dialog_about, null);
                final TextView textView = (TextView) dialogView.findViewById(R.id.textView);
                textView.setMovementMethod(LinkMovementMethod.getInstance());
                dialogBuilder.setTitle(R.string.about);
                dialogBuilder.setView(dialogView);
                dialogBuilder.setPositiveButton(getString(R.string.close), null);
                final AlertDialog dialog = dialogBuilder.create();
                dialog.show();
            }
            break;

            case R.id.actionExit:
                isExit = true;
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}