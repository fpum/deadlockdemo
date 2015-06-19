package de.pum.deadlockdemo;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.TextView;
import com.couchbase.lite.Manager;
import com.couchbase.lite.android.AndroidContext;
import com.couchbase.lite.listener.LiteListener;
import com.couchbase.lite.util.Log;
import org.lightcouch.Changes;
import org.lightcouch.CouchDbClientAndroid;
import org.lightcouch.Response;

/**
 * Created by florian on 19.06.2015.
 */
public class DemoActivity extends Activity {

    private static String TAG = "DemoActivity";

    private TextView textView = null;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        textView = new TextView(this);
        this.setContentView(textView);
        try {
            Manager manager = new Manager(new AndroidContext(this), Manager.DEFAULT_OPTIONS);
            manager.getDatabase("demo");
            LiteListener listener = new LiteListener(manager, 5934, null);
            listener.start();
            new DemoTask().execute(listener.getListenPort());
        } catch (Exception e) {
            textView.setText("An error occurred while initializing the demo.");
            Log.e(TAG, "An error occurred while initializing the demo.", e);
        }
    }

    private class DemoTask extends AsyncTask<Integer, String, Integer> {

        protected Integer doInBackground(Integer... integers) {
            CouchDbClientAndroid client = new CouchDbClientAndroid("demo", true, "http", "127.0.0.1", integers[0], null, null);
            for(int count = 1; count <= 1000; count++) {
                TestDoc doc = new TestDoc(count);
                Response response = client.save(doc);
                client.remove(response.getId(), response.getRev());
                publishProgress("Created change number " + count + ".");
                int changeCount = 0;
                Changes changes = client.changes()
                        .heartBeat(3000)
                        .includeDocs(true)
                        .since("0")
                        .continuousChanges();
                while(changes.hasNext()) {
                    changes.next();
                    ++changeCount;
                    if(changeCount == count) {
                        changes.stop();
                    }
                }
                publishProgress(changeCount + " changes found.");
            }

            return 0;
        }

        protected void onProgressUpdate(String... values) {
            DemoActivity.this.textView.append(values[0] + '\n');
        }

    }

}
