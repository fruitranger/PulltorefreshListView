package yaochangwei.pulltorefreshlistview.widget;

import android.content.Context;
import android.view.View;

public class ListBottomView extends ListHeaderView {

	public ListBottomView(Context context, RefreshableListView list) {
		super(context, list);
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		final View childView = getChildView();
		if (childView == null) {
			return;
		}

		final int childViewWidth = childView.getMeasuredWidth();
		final int childViewHeight = childView.getMeasuredHeight();
		childView.layout(0, 0, childViewWidth, childViewHeight);
	}
	
	public void setBottomHeight(int height){
		setHeaderHeight(height);
		mListView.setSelection(mListView.getAdapter().getCount() - 1);
	}

	@Override
	public void setHeaderHeight(int height) {
		super.setHeaderHeight(height);
	}

}
