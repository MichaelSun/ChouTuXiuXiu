package com.canruoxingchen.uglypic.util;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.security.MessageDigest;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.view.WindowManager;

/** 
 * 字符串操作工具包
 */

public class StringUtils {
	
	private final static Pattern emailer = Pattern.compile("\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*");
	private final static String[] hexDigits = {
        "0", "1", "2", "3", "4", "5", "6", "7",
        "8", "9", "a", "b", "c", "d", "e", "f"};

    
	public static String unGzipBytesToString(InputStream in) {

		try {
			PushbackInputStream pis = new PushbackInputStream(in, 2);
			byte[] signature = new byte[2];
			pis.read(signature);
			pis.unread(signature);
			int head = ((signature[0] & 0x00FF) | ((signature[1] << 8) & 0xFF00));
			if (head != GZIPInputStream.GZIP_MAGIC) {
				return new String(toByteArray(pis), "UTF-8").trim();
			}
			GZIPInputStream gzip = new GZIPInputStream(pis);
			byte[] readBuf = new byte[8 * 1024];
			ByteArrayOutputStream outputByte = new ByteArrayOutputStream();
			int readCount = 0;
			do {
				readCount = gzip.read(readBuf);
				if (readCount > 0) {
					outputByte.write(readBuf, 0, readCount);
				}
			} while (readCount > 0);
			closeQuietly(gzip);
			closeQuietly(pis);
			closeQuietly(in);
			if (outputByte.size() > 0) {
				return new String(outputByte.toByteArray());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
    
    public static String MD5Encode(String origin) {
        String resultString = null;
        try {           
            MessageDigest md = MessageDigest.getInstance("MD5");
            resultString = byteArrayToHexString(md.digest(origin.getBytes()));
        }
        catch (Exception ex) {
        
        }
        return resultString;
    }
    
    private static String byteArrayToHexString(byte[] b) {
        StringBuffer resultSb = new StringBuffer();
        for (int i = 0; i < b.length; i++) {
            resultSb.append(byteToHexString(b[i]));
        }
        return resultSb.toString();
    }
    
    private static String byteToHexString(byte b) {
        int n = b;
        if (n < 0)
            n = 256 + n;
        int d1 = n >>> 4 & 0xf;
        int d2 = n & 0xf;
        return hexDigits[d1] + hexDigits[d2];
    }
    
	/**
	 * 关闭InputStream
	 */
	public static void closeQuietly(InputStream is) {
		try {
			if (is != null) {
				is.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 关闭InputStream
	 */
	public static void closeQuietly(OutputStream os) {
		try {
			if (os != null) {
				os.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 将input流转为byte数组，自动关闭
	 * 
	 * @param input
	 * @return
	 */
	public static byte[] toByteArray(InputStream input) throws Exception {
		if (input == null) {
			return null;
		}
		ByteArrayOutputStream output = null;
		byte[] result = null;
		try {
			output = new ByteArrayOutputStream();
			byte[] buffer = new byte[1024 * 100];
			int n = 0;
			while (-1 != (n = input.read(buffer))) {
				output.write(buffer, 0, n);
			}
			result = output.toByteArray();
		} finally {
			closeQuietly(input);
			closeQuietly(output);
		}
		return result;
	}
	
	/**
	 * 判断给定字符串是否空白串。
	 * 空白串是指由空格、制表符、回车符、换行符组成的字符串
	 * 若输入字符串为null或空字符串，返回true
	 * @param input
	 * @return boolean
	 */
	public static boolean isEmpty( String input ) 
	{
		if ( input == null || "".equals( input ) )
			return true;
		
		for ( int i = 0; i < input.length(); i++ ) 
		{
			char c = input.charAt( i );
			if ( c != ' ' && c != '\t' && c != '\r' && c != '\n' )
			{
				return false;
			}
		}
		return true;
	}

	/**
	 * 判断是不是一个合法的电子邮件地址
	 * @param email
	 * @return
	 */
	public static boolean isEmail(String email){
		if(email == null || email.trim().length()==0) 
			return false;
	    return emailer.matcher(email).matches();
	}
	/**
	 * 字符串转整数
	 * @param str
	 * @param defValue
	 * @return
	 */
	public static int toInt(String str, int defValue) {
		try{
			return Integer.parseInt(str);
		}catch(Exception e){}
		return defValue;
	}
	/**
	 * 对象转整数
	 * @param obj
	 * @return 转换异常返回 0
	 */
	public static int toInt(Object obj) {
		if(obj==null) return 0;
		return toInt(obj.toString(),0);
	}
	/**
	 * 对象转整数
	 * @param obj
	 * @return 转换异常返回 0
	 */
	public static long toLong(String obj) {
		try{
			return Long.parseLong(obj);
		}catch(Exception e){}
		return 0;
	}
	
	/**
	 * 字符串转布尔值
	 * @param b
	 * @return 转换异常返回 false
	 */
	public static boolean toBool(String b) {
		try{
			return Boolean.parseBoolean(b);
		}catch(Exception e){}
		return false;
	}
	
	public static String loadRawRes(Context context, int resId) throws Exception {
		InputStream in = context.getResources().openRawResource(resId);
		return new String(StringUtils.toByteArray(in));
	}
	
	/**
	 * 将URL转为圆角图片URL
	 * @param url
	 * @param isRoundCorner
	 * @return
	 */
	public static String convertRoundCornerUrl(String url , boolean isRoundCorner){
		if(isRoundCorner){
			return url+ "#";
		}
		return url;
	}


	public static String isToStr(InputStream is) {
		if (is != null) {
			BufferedReader reader = new BufferedReader(
					new InputStreamReader(is));
			StringBuilder sb = new StringBuilder();
			String line = null;

			try {
				while ((line = reader.readLine()) != null) {
					sb.append(line + "\n");
				}
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			} finally {
				try {
					is.close();
					is = null;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			return sb.toString();

		} else {
			return null;
		}
	}
	
	
	public static CharSequence equipStringWithCustomSpans(
			CharSequence str,int start,int end, Object[] spans){
		if(TextUtils.isEmpty(str) || spans==null || spans.length<1 || start>=end){
			return str;
		}
		SpannableStringBuilder ssb= new SpannableStringBuilder(str);
		for(Object span : spans){
			ssb.setSpan(span, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		}
		return ssb;
	}
}
