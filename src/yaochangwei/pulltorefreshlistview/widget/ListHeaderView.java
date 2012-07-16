package yaochangwei.pulltorefreshlistview.widget;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Timer;
import java.util.TimerTask;

import yaochangwei.pulltorefreshlistview.R;
import yaochangwei.pulltorefreshlistview.widget.RefreshableListView.OnHeaderViewChangedListener;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.CountDownTimer;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;

public class ListHeaderView extends ViewGroup {

	/**
	 * Implement a flash color header.
	 * 
	 * @author Yaochangwei(yaochangwei@gmail.com)
	 * 
	 */

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
	private static final int MAX_DURATION = 450;

	private int mDistance;
	private int mInitHeight;
	private boolean mImediateUpdate = false;

	// The height when user release can trigger update.
	private int mUpdateHeight;

	// The height the view is updating...
	private int mFlashHeight;

	private RefreshableListView mListView;

	private static final int INVALID_STATE = -1;

	/** The state should be when close execute */
	private int mNextState = INVALID_STATE;

	private static final int ANIMATE_INTERVAL = 100;
	private static final int INDICATE_INTERVAL = 300;
	private final ArrayList<Integer> mColors;
	private static final int PIECE_COUNT = 16;

	OnHeaderViewChangedListener mOnHeaderViewChangedListener;

	public ListHeaderView(Context context, RefreshableListView list) {
		super(context);
		mListView = list;

		int[] colors = context.getResources().getIntArray(R.array.colors);
		mColors = new ArrayList<Integer>();
		for (int color : colors) {
			mColors.add(color | 0xff000000);
		}

		mUpdateHeight = (int) (context.getResources().getDisplayMetrics().density * 55 + 0.5f);
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

	private Timer mTimer;
	private Timer mIndicateTimer;
	private boolean mNeedsAnimated;

	private void toggleIndicator() {
		if (mIndicateTimer != null) {
			mIndicateTimer.cancel();
			mIndicateTimer = null;
		} else {
			mNeedsAnimated = true;
			mIndicateTimer = new Timer();
			mIndicateTimer.scheduleAtFixedRate(new TimerTask() {
				public void run() {
					cycleColor();
				}
			}, 0, INDICATE_INTERVAL);
		}
	}

	private void shuffleColor() {
		Collections.shuffle(mColors);
		postInvalidate();
	}

	private void cycleColor() {
		int color = mColors.remove(mColors.size() - 1);
		mColors.add(0, color);
		postInvalidate();
	}

	public void toggle() {

		if (mIndicateTimer != null) {
			mIndicateTimer.cancel();
			mIndicateTimer = null;
		}

		if (mTimer == null) {
			mTimer = new Timer();
			mNeedsAnimated = true;
			mTimer.scheduleAtFixedRate(new TimerTask() {
				public void run() {
					shuffleColor();
				}
			}, 0, ANIMATE_INTERVAL);
		} else {
			mNeedsAnimated = false;
			mTimer.cancel();
			mTimer = null;
		}
	}

	@Override
	protected void dispatchDraw(Canvas canvas) {
		drawBackground(canvas);
		if (!mNeedsAnimated) {
			super.dispatchDraw(canvas);
		}
	}

	private final void drawBackground(Canvas canvas) {
		int height = getMeasuredHeight();
		int pieceWidth = getMeasuredWidth() / PIECE_COUNT;
		mFlashHeight = pieceWidth;
		final Paint paint = new Paint();
		final ArrayList<Integer> colors = mColors;
		for (int i = 0; i < PIECE_COUNT; i++) {
			paint.setColor(colors.get(i));// FIXME the size is 15 and the index
											// is also 15 04-11 12:47:01.476:
											// E/AndroidRuntime(7919):
											// java.lang.IndexOutOfBoundsException:
											// Invalid index 15, size is 15
											// it should be multi-thread bug.

			canvas.drawRect(i * pieceWidth, 0, (i + 1) * pieceWidth, height,
					paint);
		}
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
		}
	}

	public void startUpdate(Runnable runnable) {
		mUpdateRunnable = runnable;
		mInitHeight = mHeight;
		mDistance = mInitHeight - mFlashHeight;
		if (mDistance < 0) {
			mDistance = mInitHeight;
		}
		// getChildView().setVisibility(View.INVISIBLE);
		int duration = (int) (mDistance * 3);
		duration = duration > MAX_DURATION ? MAX_DURATION : duration;
		final CloseTimer timer = new CloseTimer(duration);
		timer.startTimer();
	}

	private Runnable mUpdateRunnable;

	public void close(int nextState) {
		mDistance = mInitHeight = mHeight;
		int duration = (int) (mDistance * 4);
		duration = duration > MAX_DURATION ? MAX_DURATION : duration;
		mNextState = nextState;
		final CloseTimer timer = new CloseTimer(duration);
		timer.startTimer();
	}

	public boolean isUpdateNeeded() {
		if (mImediateUpdate) {
			mImediateUpdate = false;
			return true;
		}

		final int distance = mHeight - mUpdateHeight;
		return distance >= 0;
	}

	public void moveToUpdateHeight() {
		if (mFlashHeight == 0) {
			mFlashHeight = this.getMeasuredWidth() / PIECE_COUNT;
		}
		setHeaderHeight(mFlashHeight);
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

	private View getChildView() {
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

	public void setHeaderHeight(int height) {
		// final int lastHeight = mHeight;
		// final int updateHeight = mUpdateHeight;
		mHeight = height;

		// if (height == 0) {
		// getChildView().setVisibility(View.VISIBLE);
		// }else{
		// getChildView().setVisibility(View.GONE);
		// }
		// final View childView = getChildView();

		// if ((height < updateHeight) && (lastHeight > updateHeight)) {
		// // if (mOnHeaderViewChangedListener != null) {
		// // mOnHeaderViewChangedListener.onViewChanged(this, false);
		// // }
		// toggleIndicator();
		// } else if ((height > updateHeight) && (lastHeight < updateHeight)) {
		// // if (mOnHeaderViewChangedListener != null) {
		// // mOnHeaderViewChangedListener.onViewChanged(this, true);
		// // }
		// toggleIndicator();
		// }
		requestLayout();
	}

}