package com.canruoxingchen.uglypic.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

import com.canruoxingchen.uglypic.R;

public final class SlipButton extends View implements OnTouchListener {
	
	private static final String TAG = "Slip_Button";

	public static interface OnChangedListener {
		abstract void OnChanged(View v, boolean CheckState);
	}

	private boolean mIsOpen;// 记录当前按钮是否打开,true为打开,此时游标在最右端，flase为关闭,此时游标在最左端
	private boolean mOnSlip;// 记录用户是否在滑动的变量
	private float mDownX, mNowX;// 按下时的x,当前的x,
	private Rect mBtnOnRect, mBtnOffRect;// 打开和关闭状态下,游标的Rect

	private OnChangedListener mOnChangedListener;

	private Bitmap mSlipBtnBg;
	private Bitmap mLeftSlipBtn;
	private Bitmap mLeftSlipBtnNormal;
	private Bitmap mLeftSlipBtnPressed;
	private Bitmap mRightSlipBtn;
	private Bitmap mRightSlipBtnNormal;
	private Bitmap mRightSlipBtnPressed;

	private int mResIdBg = R.drawable.setting_slipbutton_background;
	private int mResIdLeftNormal = R.drawable.setting_slipbutton_handle_off;
	private int mResIdLeftPressed = R.drawable.setting_slipbutton_off_pressed;
	private int mResIdRightNormal = R.drawable.setting_slipbutton_on_normal;
	private int mResIdRightPressed = R.drawable.setting_slipbutton_on_pressed;

	private boolean mIsMoveAction;
	private int mIgnoreRegion;
	private Context mContext;
	private int screenWidth;

	public SlipButton(Context context) {
		super(context);
		mContext = context;
		init();
		
	}

	public SlipButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
		init();
	}

	private void init() {// 初始化
		// 载入图片资源
		
		mSlipBtnBg = BitmapFactory.decodeResource(getResources(), mResIdBg);
		mLeftSlipBtnNormal = BitmapFactory.decodeResource(getResources(), mResIdLeftNormal);
		mLeftSlipBtnPressed = BitmapFactory.decodeResource(getResources(), mResIdLeftPressed);
		mRightSlipBtnNormal = BitmapFactory.decodeResource(getResources(), mResIdRightNormal);
		mRightSlipBtnPressed = BitmapFactory.decodeResource(getResources(), mResIdRightPressed);
		mLeftSlipBtn = mLeftSlipBtnNormal;
		mRightSlipBtn = mRightSlipBtnNormal;
		
		// 获得需要的Rect数据
		mBtnOnRect = new Rect(0, 0, mLeftSlipBtn.getWidth(), mLeftSlipBtn.getHeight());
		mBtnOffRect = new Rect(mSlipBtnBg.getWidth() - mRightSlipBtn.getWidth(), 0, mSlipBtnBg.getWidth(),
				mRightSlipBtn.getHeight());
		setOnTouchListener(this);// 设置监听器,也可以直接复写OnTouchEvent

		mIgnoreRegion = mSlipBtnBg.getWidth() / 20;
        DisplayMetrics dm = mContext.getApplicationContext().getResources().getDisplayMetrics();
        screenWidth = dm.widthPixels;
		   
	}

	@Override
	protected void onDraw(Canvas canvas) {// 绘图函数
		super.onDraw(canvas);
		Matrix matrix = new Matrix();
		Paint paint = new Paint();
		float x;
		// 根据设置判断初始状态

		if (mOnSlip) {
			if (mNowX >= mSlipBtnBg.getWidth()) {
				x = mSlipBtnBg.getWidth() - mLeftSlipBtn.getWidth() / 2;// 减去游标1/2的长度...
			} else {
				x = mNowX - mLeftSlipBtn.getWidth() / 2;
			}
		} else {
			if (mIsOpen) {
				x = mBtnOffRect.left;
				mNowX = x;
			} else {
				x = mBtnOnRect.left;
				mNowX = x;
			}
		}
		if (x < 0) {
			x = 0;
		} else if (x > mSlipBtnBg.getWidth() - mRightSlipBtn.getWidth()) {
			x = mSlipBtnBg.getWidth() - mRightSlipBtn.getWidth();
		}

		if (mNowX < (mSlipBtnBg.getWidth() / 2)) {
			canvas.drawBitmap(mSlipBtnBg, matrix, paint);// 画出关闭时的背景
		} else {
			canvas.drawBitmap(mSlipBtnBg, matrix, paint);// 画出打开时的背景
		}

		if(mOnSlip && mIsOpen) {
			canvas.drawBitmap(mRightSlipBtn, x, 0, paint);// 画出游标
		}else if(!mOnSlip && mIsOpen) {
			canvas.drawBitmap(mRightSlipBtn, x, 0, paint);// 画出游标.
		}else if(mOnSlip && !mIsOpen) {
			canvas.drawBitmap(mLeftSlipBtn, x, 0, paint);// 画出游标
		}else if(!mOnSlip && !mIsOpen) {
			canvas.drawBitmap(mLeftSlipBtn, x, 0, paint);// 画出游标.
		}
		
//		if (!mOnSlip && mIsOpen) {
//			canvas.drawBitmap(mRightSlipBtn, x, 0, paint);// 画出游标.
//		} else {
//			canvas.drawBitmap(mLeftSlipBtn, x, 0, paint);// 画出游标.
//		}
		
	}
	
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:// 按下
			if (event.getX() > mSlipBtnBg.getWidth() || event.getY() > mSlipBtnBg.getHeight()) {
				return false;
			}
			mDownX = event.getX();
			mNowX = mDownX;
			mIsMoveAction = false;
			
			//按下高亮显示效果
			mRightSlipBtn = mRightSlipBtnPressed;		
			mLeftSlipBtn = mLeftSlipBtnPressed;
			
			
			break;
		case MotionEvent.ACTION_MOVE:// 滑动
			mNowX = event.getX();
			if (Math.abs(mNowX - mDownX) > mIgnoreRegion) {
				mIsMoveAction = true;
				mOnSlip = true;
			}
			if (mIsMoveAction) {
				if (event.getX() >= (mSlipBtnBg.getWidth() / 2)) {
					mIsOpen = true;
				} else {
					mIsOpen = false;
				}
			}
			
			invalidate();
			return true;
			//            break;
		case MotionEvent.ACTION_UP:// 松开
			mOnSlip = false;
			if (!mIsMoveAction) {
				mIsOpen = !mIsOpen;
			} else {
				// move action
				if (event.getX() >= (mSlipBtnBg.getWidth() / 2)) {
					mIsOpen = true;
				} else {
					mIsOpen = false;
				}
			}

			if (mOnChangedListener != null) {
				mOnChangedListener.OnChanged(v, mIsOpen);
			}
			
			//松开恢复无高亮显示效果
			mRightSlipBtn = mRightSlipBtnNormal;
			mLeftSlipBtn = mLeftSlipBtnNormal;
			
			break;
		case MotionEvent.ACTION_CANCEL:
			
			//松开恢复无高亮显示效果
			mRightSlipBtn = mRightSlipBtnNormal;
			mLeftSlipBtn = mLeftSlipBtnNormal;
		
		default:
			mOnSlip = false;
		if(event.getX()<screenWidth-30-mSlipBtnBg.getWidth()/2){
				mIsOpen = false;
			}
			else{
				mIsOpen = true;
			}
			if (mOnChangedListener != null) {
				mOnChangedListener.OnChanged(v, mIsOpen);
			}
		}
		
//		if(mIsMoveAction) {
//			//松开恢复无高亮显示效果
//			mRightSlipBtn = mRightSlipBtnNormal;
//			mLeftSlipBtn = mLeftSlipBtnNormal;
//		}
		invalidate();
		return true;
	}

	public void SetOnChangedListener(OnChangedListener l) {// 设置监听器,当状态修改的时候
		mOnChangedListener = l;
	}

	public void setStatus(boolean status) {
		mIsOpen = status;
		if (mIsOpen) {
			mNowX = mSlipBtnBg.getWidth();
		} else {
			mNowX = 0;
		}
	}

	public void setBitmap(int resIdBg, int resIdLeftNormal, int resIdLeftPressed,  int resIdRightNormal, int resIdRightPressed) {
		mResIdBg = resIdBg;	
		mResIdLeftNormal = resIdLeftNormal;
		mResIdLeftPressed = resIdLeftPressed;
		mResIdRightNormal = resIdRightNormal;
		mResIdRightPressed = resIdRightPressed;
		
		init();
	}
}