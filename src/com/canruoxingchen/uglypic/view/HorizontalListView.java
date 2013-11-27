/** 
 * HorizontalListView.java v1.5
 *
 * The MIT License
 * Copyright (c) 2011 Paul Soucy (paul@dev-smart.com)
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 */
package com.canruoxingchen.uglypic.view;

import java.util.LinkedList;
import java.util.Queue;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.Scroller;

import com.canruoxingchen.uglypic.util.Logger;

/**
 * A Horizontal ListView base on Paul Soucy's implementation
 * https://github.com/borfast
 * /Android-Horizontal-ListView/blob/master/HorizontalListView.java
 * 
 * @author Shaofeng Wang 2012-8-18下午5:51:12
 */
public class HorizontalListView extends AdapterView<ListAdapter> implements
		OnGestureListener {

	private static final String TAG = "HorizontalListView";

	private ListAdapter mAdapter;

	@SuppressWarnings("unused")
	private float mUnselectedAlpha = 1.0F;

	/**
	 * The index of the first visible item
	 */
	private int mLeftViewIndex = -1;

	/**
	 * The index of the last visible item
	 */
	private int mRightViewIndex = 0;

	/**
	 * X coordinate of the visible items
	 */
	private int mCurrentX = 0;

	/**
	 * The x coordinate that the visible items should move to
	 */
	private int mNextX = 0;

	private int mMaxX = Integer.MAX_VALUE;

	private int mDisplayOffset = 0;

	@SuppressWarnings("unused")
	private int mSelectedPosition = 0;
	
	private View mSelectedChild = null;

	private Scroller mScroller;

	private GestureDetector mGestureDetector;

	private Queue<View> mRemovedViewQueue = new LinkedList<View>();

	private OnItemSelectedListener mOnItemSelectedListener;

	private OnItemClickListener mOnItemClickedListener;

	private boolean mDataChanged = false;
	
	public HorizontalListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initView();
	}

	/**
	 * Update the list when data in the list has changed
	 */
	private DataSetObserver mDataObserver = new DataSetObserver() {
		@Override
		public void onChanged() {
			synchronized (HorizontalListView.this) {
				mDataChanged = true;
			}
			invalidate();
			requestLayout();
		}

		@Override
		public void onInvalidated() {
			reset();
			invalidate();
			requestLayout();
		}
	};

	/**
	 * Re-layout all the views in the list
	 */
	private synchronized void reset() {
		initView();
		removeAllViewsInLayout();
		requestLayout();
	}

	private void initView() {
		mLeftViewIndex = -1;
		mRightViewIndex = 0;
		mDisplayOffset = 0;
		mCurrentX = 0;
		mNextX = 0;
		mMaxX = Integer.MAX_VALUE;
		mScroller = new Scroller(getContext());
		mGestureDetector = new GestureDetector(getContext(), this);
	}

	/**
	 * Add a view into the list
	 * 
	 * @param child
	 * @param viewPos
	 */
	private void addAndMeasureChild(final View child, int viewPos) {
		LayoutParams params = child.getLayoutParams();
		if (params == null) {
			params = new LayoutParams(LayoutParams.MATCH_PARENT,
					LayoutParams.MATCH_PARENT);
		}

		child.measure(
				MeasureSpec.makeMeasureSpec(getWidth(), MeasureSpec.AT_MOST),
				MeasureSpec.makeMeasureSpec(getHeight(), MeasureSpec.AT_MOST));

		addViewInLayout(child, viewPos, params, true);
	}

	@SuppressLint("DrawAllocation")
	@Override
	protected synchronized void onLayout(boolean changed, int left, int top,
			int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		if (mAdapter == null) {
			return;
		}

		// if the data has changed, we should move back to where we were before
		// the changing
		if (mDataChanged) {
			int oldCurrentX = mCurrentX;
			initView();
			removeAllViewsInLayout();
			mNextX = oldCurrentX;
			mDataChanged = false;
		}

		if (mScroller.computeScrollOffset()) {
			int scrollX = mScroller.getCurrX();
			mNextX = scrollX;
		}

		// we can not go out of the view (left)
		if (mNextX <= 0) {
			mNextX = 0;
			mScroller.forceFinished(true);
		}

		// we can not go out of the view (right)
		if (mNextX >= mMaxX) {
			mNextX = mMaxX;
			mScroller.forceFinished(true);
		}

		int deltaX = mCurrentX - mNextX;
		removeNonVisibleItems(deltaX);
		fillList(deltaX);
		positionItems(deltaX);

		mCurrentX = mNextX;
		if (!mScroller.isFinished()) {
			post(new Runnable() {
				@Override
				public void run() {
					requestLayout();
				}
			});
		}
	}

	private void fillList(final int deltaX) {
		int edge = 0;
		View child = getChildAt(getChildCount() - 1);
		if (child != null) {
			edge = child.getRight();
		}
		fillListRight(edge, deltaX);

		child = getChildAt(0);
		if (child != null) {
			edge = child.getLeft();
		}
		fillListLeft(edge, deltaX);
	}

	
	/**
	 * Add views that will be shown on the right
	 * 
	 * @param rightEdge
	 * @param deltaX
	 */
	private void fillListRight(int rightEdge, final int deltaX) {
		;
		while (rightEdge + deltaX < getWidth()
				&& mRightViewIndex < mAdapter.getCount()) {
			View child = mAdapter.getView(mRightViewIndex,
					mRemovedViewQueue.poll(), this);
			addAndMeasureChild(child, -1);
			rightEdge += child.getMeasuredWidth();
			if (mRightViewIndex == mAdapter.getCount() - 1) {
				mMaxX = mCurrentX + rightEdge - getWidth();
			}
			if (mMaxX < 0) {
				mMaxX = 0;
			}
			mRightViewIndex++;
		}
	}

	/**
	 * Add views that will be shown on the left
	 * 
	 * @param leftEdge
	 * @param deltaX
	 */
	private void fillListLeft(int leftEdge, final int deltaX) {
		while (leftEdge + deltaX > 0 && mLeftViewIndex >= 0) {
			View child = mAdapter.getView(mLeftViewIndex,
					mRemovedViewQueue.poll(), this);
			addAndMeasureChild(child, 0);
			leftEdge -= child.getMeasuredWidth();
			mLeftViewIndex--;
			mDisplayOffset -= child.getMeasuredWidth();
		}
	}

	/**
	 * compute and remove invisible items so that views are reused
	 * 
	 * @param deltaX
	 */
	private void removeNonVisibleItems(final int deltaX) {
		View child = getChildAt(0);
		while (child != null && child.getRight() + deltaX <= 0) {
			mDisplayOffset += child.getMeasuredWidth();
			mRemovedViewQueue.offer(child);
			removeViewInLayout(child);
			mLeftViewIndex++;
			child = getChildAt(0);
		}

		child = getChildAt(getChildCount() - 1);
		while (child != null && child.getLeft() + deltaX >= getWidth()) {
			mRemovedViewQueue.offer(child);
			removeViewInLayout(child);
			mRightViewIndex--;
			child = getChildAt(getChildCount() - 1);
		}
	}

	/**
	 * Re-layout the visible items
	 * 
	 * @param deltaX
	 */
	private void positionItems(final int deltaX) {
		int childrenCount = getChildCount();
		if (childrenCount > 0) {
			mDisplayOffset += deltaX;
			int left = mDisplayOffset;
			for (int i = 0; i < childrenCount; i++) {
				View child = getChildAt(i);
				int childWidth = child.getMeasuredWidth();
				child.layout(left, 0, left + childWidth,
						child.getMeasuredHeight());
				left += childWidth;
			}
		}
	}

	public synchronized void scrollTo(int x) {
		mScroller.startScroll(mNextX, 0, x - mNextX, 0);
		requestLayout();
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		if(mGestureDetector != null && ev != null) {
			return mGestureDetector.onTouchEvent(ev);
		}
//		boolean consumed = mGestureDetector.onTouchEvent(ev);
		return false;
	}

	@Override
	public void setOnItemClickListener(OnItemClickListener listener) {
		mOnItemClickedListener = listener;
	}

	@Override
	public void setOnItemSelectedListener(OnItemSelectedListener listener) {
		mOnItemSelectedListener = listener;
	}

	@Override
	public void setAdapter(ListAdapter adapter) {
		if (mAdapter != null) {
			mAdapter.unregisterDataSetObserver(mDataObserver);
		}
		mAdapter = adapter;
		mAdapter.registerDataSetObserver(mDataObserver);
		reset();
	}

	@Override
	public ListAdapter getAdapter() {
		return mAdapter;
	}

	@Override
	public boolean onDown(MotionEvent arg0) {
		mScroller.forceFinished(true);
		return true;
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {
		synchronized (this) {
			mScroller.fling(mNextX, 0, (int) -velocityX, 0, 0, mMaxX, 0, 0);
		}
		requestLayout();

		return true;
	}

	@Override
	public void onLongPress(MotionEvent e) {

	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
		if (e1 != null && e2 != null) {
			synchronized (this) {
				mNextX += (int) distanceX;
			}
			requestLayout();
		}
		return true;
	}

	@Override
	public void onShowPress(MotionEvent e) {

	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		LOGD("onSingleTapUp(" + e.getX() + ")");
		Rect viewRect = new Rect();
		for (int i = 0; i < getChildCount(); i++) {
			View child = getChildAt(i);
			int left = child.getLeft();
			int right = child.getRight();
			int top = child.getTop();
			int bottom = child.getBottom();
			viewRect.set(left, top, right, bottom);
			if (viewRect.contains((int) e.getX(), (int) e.getY())) {
				setSelection(i);
				if (mOnItemClickedListener != null) {
					mOnItemClickedListener.onItemClick(this, child,
							mLeftViewIndex + 1 + i,
							mAdapter.getItemId(mLeftViewIndex + 1 + i));
				}
				if (mOnItemSelectedListener != null) {
					mOnItemSelectedListener.onItemSelected(this, child,
							mLeftViewIndex + 1 + i,
							mAdapter.getItemId(mLeftViewIndex + 1 + i));
				}
				break;
			}

		}
		return true;
	}

	@Override
	public View getSelectedView() {
		return mSelectedChild;
	}

	@Override
	public void setSelection(int position) {
		mSelectedPosition = position;
		mSelectedChild = getChildAt(position);
		LOGD("setSelection(" + position + ")");
		requestLayout();
	}

	private void LOGD(String message) {
		Logger.d(TAG, message);
	}

}
