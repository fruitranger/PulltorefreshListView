package yaochangwei.pulltorefreshlistview;

import java.util.ArrayList;

import yaochangwei.pulltorefreshlistview.widget.RefreshableListView;
import yaochangwei.pulltorefreshlistview.widget.RefreshableListView.OnPullUpUpdateTask;
import yaochangwei.pulltorefreshlistview.widget.RefreshableListView.OnUpdateTask;
import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;

public class TestActivity extends Activity {

	private RefreshableListView mListView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_test);

		final ArrayList<String> listItemDatas = new ArrayList<String>();
		for (int i = 0; i < 10; i++) {
			listItemDatas.add("list item " + i);
		}

		final ArrayAdapter<String> aa = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, android.R.id.text1,
				listItemDatas);
		final RefreshableListView list = (RefreshableListView) findViewById(R.id.refreshablelistview);
		mListView = list;
		list.setAdapter(aa);

		/* We set the onrefreshListener. */
		list.setOnUpdateTask(new OnUpdateTask() {

			public void updateBackground() {
				// simulate long times operation.
				try {
					Thread.sleep(1500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			public void updateUI() {
				// aa.notifyDataSetChanged();
			}

			public void onUpdateStart() {

			}

		});

		list.setOnPullUpUpdateTask(new OnPullUpUpdateTask() {

			@Override
			public void onUpdateStart() {

			}

			@Override
			public void updateBackground() {
				// simulate long times operation.
				try {
					Thread.sleep(1500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			@Override
			public void updateUI() {
				listItemDatas.add("list item" + listItemDatas.size());
				aa.notifyDataSetChanged();
			}

		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_test, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		/* You can start update immediately. */
		mListView.startUpdateImmediate();
		return true;
	}

}
