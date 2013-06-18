package yaochangwei.pulltorefreshlistview.widget;

import yaochangwei.pulltorefreshlistview.widget.RefreshableListView.OnHeaderViewChangedListener;
import android.content.Context;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;

public class ListHeaderView extends ViewGroup {

	/** Set the height of the list header */
	private int mHeight;

	/** Interpolator */
	private static final Interpolator sInterpolator = new Interpolator() {

		public float getInterpolation(float t) {
			t -= 1.0f;
			return t * t * t + 1.0f;
		}

	};

	/** Max Duration */
	private static final int MAX_DURATION = 350;

	private int mDistance;
	private int mInitHeight;
	private boolean mImediateUpdate = false;

	// The height when user release can trigger update.
	private int mUpdateHeight;

	protected RefreshableListView mListView;

	private static final int INVALID_STATE = -1;

	/** The state shoudl be when close execute */
	private int mNextState = INVALID_STATE;

	OnHeaderViewChangedListener mOnHeaderViewChangedListener;

	/** The header upate status control the header view */
	int mUpdatingStatus = UPDATING_IDLE;

	private static final int UPDATING_IDLE = 0;
	private static final int UPDATING_READY = 1;
	private static final int UPDATING_ON_GOING = 2;
	private static final int UPDATING_FINISH = 3;

	/** Max pull height. */
	private int mMaxPullHeight;

	private static final int MAX_PULL_HEIGHT_DP = 200;

	public ListHeaderView(Context context, RefreshableListView list) {
		super(context);
		mListView = list;
		mMaxPullHeight = (int) (context.getResources().getDisplayMetrics().density
				* MAX_PULL_HEIGHT_DP + 0.5f);
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		final View childView = getChildView();
		if (childView == null) {
			return;
		}

		final int childViewWidth = childView.getMeasuredWidth();
		final int childViewHeight = childView.getMeasuredHeight();
		final int measuredHeight = getMeasuredHeight();
		childView.layout(0, measuredHeight - childViewHeight, childViewWidth,
				measuredHeight);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int width = MeasureSpec.getSize(widthMeasureSpec);
		if (mHeight < 0) {
			mHeight = 0;
		}
		setMeasuredDimension(width, mHeight);

		final View childView = getChildView();
		if (childView != null) {
			childView.measure(widthMeasureSpec, heightMeasureSpec);
			mUpdateHeight = childView.getMeasuredHeight();
		}
	}

	public void startUpdate(Runnable runnable) {
		mUpdatingStatus = UPDATING_READY;
		mUpdateRunnable = runnable;
		mInitHeight = mHeight;
		mDistance = mInitHeight - mUpdateHeight;
		if (mDistance < 0) {
			mDistance = mInitHeight;
		}
		// getChildView().setVisibility(View.INVISIBLE);
		int duration = (int) (mDistance * 3);
		duration = duration > MAX_DURATION ? MAX_DURATION : duration;
		Log.d(VIEW_LOG_TAG, "duration:" + duration);
		final CloseTimer timer = new CloseTimer(duration);
		timer.startTimer();
	}

	private Runnable mUpdateRunnable;

	public int close(int nextState) {
		mUpdatingStatus = UPDATING_FINISH;
		if (mOnHeaderViewChangedListener != null) {
			mOnHeaderViewChangedListener.onViewUpdateFinish(this);
		}
		mDistance = mInitHeight = mHeight;
		int duration = (int) (mDistance * 4);
		duration = duration > MAX_DURATION ? MAX_DURATION : duration;
		mNextState = nextState;
		final CloseTimer timer = new CloseTimer(duration);
		timer.startTimer();
		return duration;
	}

	public boolean isUpdateNeeded() {
		if (mImediateUpdate) {
			mImediateUpdate = false;
			return true;
		}

		final int distance = mHeight - mUpdateHeight;
		boolean needUpdate = distance >= 0;
		return needUpdate;
	}

	public void moveToUpdateHeight() {
		setHeaderHeight(mUpdateHeight);
		mImediateUpdate = true;
	}

	private class CloseTimer extends CountDownTimer {

		private long mStart;
		private float mDurationReciprocal;

		private static final int COUNT_DOWN_INTERVAL = 15;

		public CloseTimer(long millisInFuture) {
			super(millisInFuture, COUNT_DOWN_INTERVAL);
			mDurationReciprocal = 1.0f / millisInFuture;
		}

		public void startTimer() {
			mStart = AnimationUtils.currentAnimationTimeMillis();
			start();
		}

		@Override
		public void onFinish() {
			float x = 1.0f;
			if (mNextState != INVALID_STATE) {
				mListView.setState(mNextState);
				mNextState = INVALID_STATE;
			}
			setHeaderHeight((int) (mInitHeight - mDistance * x));
			if (mUpdateRunnable != null) {
				final Runnable runnable = mUpdateRunnable;
				new Thread(runnable).start();
				mUpdateRunnable = null;
			}
		}

		@Override
		public void onTick(long millisUntilFinished) {
			final int interval = (int) (AnimationUtils
					.currentAnimationTimeMillis() - mStart);
			float x = interval * mDurationReciprocal;
			x = sInterpolator.getInterpolation(x);
			setHeaderHeight((int) (mInitHeight - mDistance * x));
		}

	}

	protected View getChildView() {
		final int childCount = getChildCount();
		if (childCount != 1) {
			return null;
		}
		return getChildAt(0);
	}

	@Override
	public void addView(View child) {
		final int childCount = getChildCount();
		if (childCount > 0) {
			throw new IllegalStateException(
					"ListHeaderView can only have one child view");
		}
		super.addView(child);
	}

	private boolean mCanUpdate;

	public void setHeaderHeight(int height) {
		if (mHeight == height && height == 0) {
			// ignore duplicate 0 height setting.
			return;
		}

		if (height > mMaxPullHeight) {
			return;
		}

		final int updateHeight = mUpdateHeight;
		mHeight = height;

		if (mUpdatingStatus != UPDATING_IDLE) {
			if (mUpdatingStatus == UPDATING_READY
					&& mOnHeaderViewChangedListener != null) {
				mOnHeaderViewChangedListener.onViewUpdating(this);
				mUpdatingStatus = UPDATING_ON_GOING;
			}
		} else {
			if ((height < updateHeight) && mCanUpdate) {
				if (mOnHeaderViewChangedListener != null) {
					mOnHeaderViewChangedListener.onViewChanged(this, false);
				}
				mCanUpdate = false;
			} else if ((height >= updateHeight) && !mCanUpdate) {
				if (mOnHeaderViewChangedListener != null) {
					mOnHeaderViewChangedListener.onViewChanged(this, true);
				}
				mCanUpdate = true;
			}
		}
		requestLayout();

		if (height == 0) {
			mUpdatingStatus = UPDATING_IDLE;
			mCanUpdate = false;
		}
	}

}