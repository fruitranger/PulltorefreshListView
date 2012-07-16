package yaochangwei.pulltorefreshlistview.widget;

import android.content.Context;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ListView;

public class RefreshableListView extends ListView {
	/**
	 * Refreshable List View, multi-touch event handled rightly.
	 * 
	 * @author Yaochangwei(yaochangwei@gmail.com)
	 * 
	 */
	private static final int STATE_NORMAL = 0;
	private static final int STATE_READY = 1;
	private static final int STATE_PULL = 2;
	private static final int STATE_UPDATING = 3;
	private static final int INVALID_POINTER_ID = -1;

	private static final int MIN_UPDATE_TIME = 500;

	private ListHeaderView mListHeaderView;

	private int mActivePointerId;
	private float mLastY;

	private int mState;

	private OnRefreshListener mOnRefreshListener;

	public interface OnRefreshListener {
		public void doInBackground();

		public void updateUI();
	}

	public RefreshableListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initialize();
	}

	public RefreshableListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initialize();
	}

	private void initialize() {
		final Context context = getContext();
		mListHeaderView = new ListHeaderView(context, this);
		addHeaderView(mListHeaderView, null, false);
		mState = STATE_NORMAL;
	}

	public void setHeaderResource(int res) {
		final View v = LayoutInflater.from(getContext()).inflate(res, this,
				false);
		mListHeaderView.addView(v);
	}

	public void setOnRefreshListener(OnRefreshListener listener) {
		mOnRefreshListener = listener;
	}

	public void startUpdateImmediate() {
		if (mState == STATE_UPDATING) {
			return;
		}
		setSelectionFromTop(0, 0);
		mListHeaderView.moveToUpdateHeight();
		update();
	}

	private void update() {
		if (mListHeaderView.isUpdateNeeded()) {
			mListHeaderView.toggle();
			mListHeaderView.startUpdate(new Runnable() {
				public void run() {
					final long b = System.currentTimeMillis();
					if (mOnRefreshListener != null) {
						mOnRefreshListener.doInBackground();
					}
					final long delta = MIN_UPDATE_TIME
							- (System.currentTimeMillis() - b);
					if (delta > 0) {
						try {
							Thread.sleep(delta);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					post(new Runnable() {
						public void run() {
							mListHeaderView.toggle();
							mListHeaderView.close(STATE_NORMAL);
						}
					});

					postDelayed(new Runnable() {
						public void run() {
							if (mOnRefreshListener != null) {
								mOnRefreshListener.updateUI();
							}
						}
					}, 50);
				}
			});
			mState = STATE_UPDATING;
		} else {
			mListHeaderView.close(STATE_NORMAL);
		}
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		if (mState == STATE_UPDATING) {
			return super.dispatchTouchEvent(ev);
		}
		final int action = ev.getAction() & MotionEventCompat.ACTION_MASK;
		switch (action) {
		case MotionEvent.ACTION_DOWN:
			mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
			mLastY = ev.getY();
			isFirstViewTop();
			break;
		case MotionEvent.ACTION_MOVE:
			if (mActivePointerId == INVALID_POINTER_ID) {
				break;
			}

			if (mState == STATE_NORMAL) {
				isFirstViewTop();
			}

			if (mState == STATE_READY) {
				final int activePointerId = mActivePointerId;
				final int activePointerIndex = MotionEventCompat
						.findPointerIndex(ev, activePointerId);
				final float y = MotionEventCompat.getY(ev, activePointerIndex);
				final int deltaY = (int) (y - mLastY);
				mLastY = y;
				if (deltaY < 0) {
					mState = STATE_NORMAL;
				} else {
					mState = STATE_PULL;
					ev.setAction(MotionEvent.ACTION_CANCEL);
					super.dispatchTouchEvent(ev);
				}
			}

			if (mState == STATE_PULL) {
				final int activePointerId = mActivePointerId;
				final int activePointerIndex = MotionEventCompat
						.findPointerIndex(ev, activePointerId);
				final float y = MotionEventCompat.getY(ev, activePointerIndex);
				final int deltaY = (int) (y - mLastY);
				mLastY = y;

				final int headerHeight = mListHeaderView.getHeight();
				setHeaderHeight(headerHeight + deltaY * 5 / 9);
				return true;
			}

			break;
		case MotionEvent.ACTION_CANCEL:
		case MotionEvent.ACTION_UP:
			mActivePointerId = INVALID_POINTER_ID;
			if (mState == STATE_PULL) {
				update();
			}
			break;
		case MotionEventCompat.ACTION_POINTER_DOWN:
			final int index = MotionEventCompat.getActionIndex(ev);
			final float y = MotionEventCompat.getY(ev, index);
			mLastY = y;
			mActivePointerId = MotionEventCompat.getPointerId(ev, index);
			break;
		case MotionEventCompat.ACTION_POINTER_UP:
			onSecondaryPointerUp(ev);
			break;
		}
		return super.dispatchTouchEvent(ev);
	}

	private void onSecondaryPointerUp(MotionEvent ev) {
		final int pointerIndex = MotionEventCompat.getActionIndex(ev);
		final int pointerId = MotionEventCompat.getPointerId(ev, pointerIndex);
		if (pointerId == mActivePointerId) {
			final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
			mLastY = MotionEventCompat.getY(ev, newPointerIndex);
			mActivePointerId = MotionEventCompat.getPointerId(ev,
					newPointerIndex);
		}
	}

	public void setState(int state) {
		mState = state;
	}

	private void setHeaderHeight(int height) {
		mListHeaderView.setHeaderHeight(height);
	}

	private boolean isFirstViewTop() {
		final int count = getChildCount();
		if (count == 0) {
			return true;
		}
		final int firstVisiblePosition = this.getFirstVisiblePosition();
		final View firstChildView = getChildAt(0);
		boolean needs = firstChildView.getTop() == 0
				&& (firstVisiblePosition == 0);
		if (needs) {
			mState = STATE_READY;
		}

		return needs;
	}

	public void setOnHeaderViewChanged(OnHeaderViewChangedListener listener) {
		mListHeaderView.mOnHeaderViewChangedListener = listener;
	}

	public static interface OnHeaderViewChangedListener {
		void onViewChanged(View v, boolean canUpdate);
	}

}
