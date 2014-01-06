/**
 * 
 */
package com.canruoxingchen.uglypic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.os.Handler;
import android.os.Message;

/**
 * 
 * 消息中心，维护 消息类型-Handler的列表，相当于总线，负责接收和分发消息
 * 
 * @author wsf
 * 
 */
public class MessageCenter {

	private Map<Integer, List<Handler> > mHandlerMaps = 
			new HashMap<Integer, List<Handler> >();
	
	private static MessageCenter sCenter = null;
	
	private Context mContext;
	
	private static final byte[] sLock = new byte[0];
	
	public MessageCenter(Context context) {
		this.mContext = context.getApplicationContext();
	}
	
	public static MessageCenter getInstance(Context context) {
		if(sCenter == null) {
			synchronized (sLock) {
				if(sCenter == null) {
					sCenter = new MessageCenter(context);
				}
			}
		}
		return sCenter;
	}
	
	public synchronized void registerMessage(int what, Handler handler) {
		if(handler == null) {
			return;
		}
		if(mHandlerMaps.containsKey(what)) {
			List<Handler> handlers = mHandlerMaps.get(what);
			if(handlers != null) {
				//是否已经添加过此Handler
				for(Handler h : handlers) {
					if(handler == h) {
						return;
					}
				}
				handlers.add(handler);
			} else {
				handlers = new ArrayList<Handler>();
				handlers.add(handler);
				mHandlerMaps.put(what, handlers);
			}
		} else {
			List<Handler> handlers = new ArrayList<Handler>();
			handlers.add(handler);
			mHandlerMaps.put(what, handlers);
		}
	}
	
	public synchronized void unregisterMessage(int what, Handler handler) {
		if(handler == null) {
			return;
		}
		
		if(mHandlerMaps.containsKey(what)) {
			List<Handler> handlers = mHandlerMaps.get(what);
			if(handlers != null) {
				handlers.remove(handler);
			}
		}
	}
	
	public synchronized void notifyHandlers(int what, int arg1, int arg2, Object obj) {
		if(mHandlerMaps.containsKey(what)) {
			List<Handler> handlers = mHandlerMaps.get(what);
			if(handlers != null) {
				for(Handler handler : handlers) {
					Message msg = Message.obtain(handler, what, arg1, arg2, obj);
					msg.sendToTarget();
				}
			}
		}
	}
	
	public synchronized void notifyHandlers(int what) {
		notifyHandlers(what, 0, 0, null);
	}
}
