package com.canruoxingchen.uglypic.util;

import java.lang.reflect.Field;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;

import com.canruoxingchen.uglypic.Config;

/**
 * 
 * @Description 实现一些不对外界公开的工具方法
 * 
 * @author xinzhang.wei
 * 
 * @time 2013-6-4 下午2:22:14
 * 
 */
public class InvisibleUtils {

	protected static StringBuilder getFieldValues(Object obj) {
		StringBuilder sb = new StringBuilder();
		if (obj == null) {
			return sb;
		}
		Class<?> clazz = obj.getClass();
		Field[] fields = clazz.getDeclaredFields();
		sb.append("[" + clazz.getSimpleName() + "--start--");
		for (Field field : fields) {
			sb.append(field.getName() + ":");
			field.setAccessible(true);
			try {
				sb.append(String.valueOf(field.get(obj)));
			} catch (IllegalArgumentException e) {
				if (Config.DEBUG)
					e.printStackTrace();
			} catch (IllegalAccessException e) {
				if (Config.DEBUG)
					e.printStackTrace();
			}
			sb.append(";");
		}
		sb.append("--end--" + clazz.getSimpleName() + "]");
		return sb;
	}
	
	public static class RoundAngleCreater {
		private Bitmap oriBitmap ;
		private Bitmap mResult;
		private Canvas mCanvas;
		private int roundWidth;
		private int roundHeight;
		private int destWidth ;
		private int destHeight;
		private static Paint paint;
		static {
			paint = new Paint();
			paint.setColor(Color.TRANSPARENT);
			paint.setAntiAlias(true);
			paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
		}

		public RoundAngleCreater(Bitmap bitmap, int roundWidth, int roundHeight) {
			this.destHeight = bitmap.getHeight();
			this.destWidth = bitmap.getWidth();
			mResult = Bitmap.createBitmap(destWidth, destHeight, android.graphics.Bitmap.Config.ARGB_8888);
			oriBitmap = bitmap;
			this.mCanvas = new Canvas(mResult);
			this.roundWidth = roundWidth;
			this.roundHeight = roundHeight;
		}

		public Bitmap getBitmap() {
			
			createDifAngle();
			
//			if(roundWidth == roundHeight){
//				createDifAngle();
//			}else{
//				createDifAngle();
//			}
			return mResult;
		}

		public void createDifAngle(){
			this.mCanvas.drawBitmap(oriBitmap, new Matrix(), new Paint());
			drawLiftUp(mCanvas);
			drawRightUp(mCanvas);
			drawLiftDown(mCanvas);
			drawRightDown(mCanvas);
		}
		
		public  void createSameAngle() { 
			 
	        final int color = 0xff424242; 
	        final Paint paint = new Paint(); 
	        final Rect rect = new Rect(0, 0, destWidth, destHeight); 
	        final RectF rectF = new RectF(rect); 
	        paint.setAntiAlias(true); 
	        mCanvas.drawARGB(0, 0, 0, 0); 
	        paint.setColor(color); 
	        mCanvas.drawRoundRect(rectF, roundWidth, roundHeight, paint); 
	        paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN)); 
	        mCanvas.drawBitmap(oriBitmap, rect, rect, paint);  
	    }
		
		private void drawLiftUp(Canvas canvas) {
			Path path = new Path();
			path.moveTo(0, roundHeight);
			path.lineTo(0, 0);
			path.lineTo(roundWidth, 0);
			path.arcTo(new RectF(0, 0, roundWidth * 2, roundHeight * 2), -90,
					-90);
			path.close();
			canvas.drawPath(path, paint);
		}

		private void drawLiftDown(Canvas canvas) {
			Path path = new Path();
			path.moveTo(0, destHeight - roundHeight);
			path.lineTo(0,destHeight);
			path.lineTo(roundWidth, destHeight);
			path.arcTo(new RectF(0, destHeight - roundHeight * 2,
					0 + roundWidth * 2,destHeight), 90, 90);
			path.close();
			canvas.drawPath(path, paint);
		}

		private void drawRightDown(Canvas canvas) {
			Path path = new Path();
			path.moveTo(destWidth- roundWidth, destHeight);
			path.lineTo(destWidth, destHeight);
			path.lineTo(destWidth, destHeight- roundHeight);
			path.arcTo(new RectF(destWidth - roundWidth * 2, destHeight
					- roundHeight * 2, destWidth, destHeight), 0, 90);
			path.close();
			canvas.drawPath(path, paint);
		}

		private void drawRightUp(Canvas canvas) {
			Path path = new Path();
			path.moveTo(destWidth, roundHeight);
			path.lineTo(destWidth, 0);
			path.lineTo(destWidth - roundWidth, 0);
			path.arcTo(new RectF(destWidth - roundWidth * 2, 0, destWidth,
					0 + roundHeight * 2), -90, 90);
			path.close();
			canvas.drawPath(path, paint);
		}
	}
}
