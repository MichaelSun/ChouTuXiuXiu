/**
 * 
 */
package com.canruoxingchen.uglypic.overlay;

import com.canruoxingchen.uglypic.R;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;

/**
 * 编辑View的容器
 * 
 * @author wsf
 * 
 */
public class EditorContainerView extends LinearLayout implements View.OnClickListener {

	private View mViewBack;
	private View mViewFinish;
	private View mViewRedo;
	private View mViewRegret;
	private ViewGroup mVgEditorContainer;

	private View mEditorView;
	private IEditor mEditor;

	public EditorContainerView(Context context, AttributeSet attrs) {
		super(context, attrs);

		initView();
	}

	private void initView() {
		View view = inflate(getContext(), R.layout.photo_editor_editor_view_container, null);
		mViewBack = view.findViewById(R.id.photo_editor_editor_back);
		mViewFinish = view.findViewById(R.id.photo_editor_editor_finish);
		mViewRedo = view.findViewById(R.id.photo_editor_editor_redo);
		mViewRegret = view.findViewById(R.id.photo_editor_editor_regret);
		mVgEditorContainer = (ViewGroup) view.findViewById(R.id.photo_editor_editor_view_container);

		mViewBack.setOnClickListener(this);
		mViewFinish.setOnClickListener(this);
		mViewRedo.setOnClickListener(this);
		mViewRegret.setOnClickListener(this);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.MATCH_PARENT);
		addView(view, params);
	}

	public void setEditorView(View editorView) {
		if(editorView == null) {
			if (mEditorView != null) {
				mVgEditorContainer.removeView(mEditorView);
				mEditorView = null;
				mEditor = null;
			}
		}
		
		if (!(editorView instanceof IEditor)) {
			return;
		}

		if (mEditorView != null) {
			mVgEditorContainer.removeView(mEditorView);
			mEditorView = null;
			mEditor = null;
		}

		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.MATCH_PARENT);
		mVgEditorContainer.addView(editorView, params);
		mEditorView = editorView;
		mEditor = (IEditor) editorView;
	}
	
	public IEditor getEditor() {
		return mEditor;
	}

	@Override
	public void onClick(View v) {
		if (mEditor != null) {
			if (mViewBack == v) {
				mEditor.onCancel();
			} else if (mViewFinish == v) {
				mEditor.onFinish();
			} else if (mViewRegret == v) {
				mEditor.onRegret();
				if(mEditor.hasMoreRedo()) {
					mViewRedo.setEnabled(true);
				} else {
					mViewRedo.setEnabled(false);
				}
				if(mEditor.hasMoreRegret()) {
					mViewRegret.setEnabled(true);
				} else {
					mViewRegret.setEnabled(false);
				}
			} else if (mViewRedo == v) {
				mEditor.onRedo();
				if(mEditor.hasMoreRedo()) {
					mViewRedo.setEnabled(true);
				} else {
					mViewRedo.setEnabled(false);
				}
				if(mEditor.hasMoreRegret()) {
					mViewRegret.setEnabled(true);
				} else {
					mViewRegret.setEnabled(false);
				}
			}
		}
	}
	
	public void onRegretStatusChanged() {
		if(mEditor.hasMoreRedo()) {
			mViewRedo.setEnabled(true);
		} else {
			mViewRedo.setEnabled(false);
		}
		if(mEditor.hasMoreRegret()) {
			mViewRegret.setEnabled(true);
		} else {
			mViewRegret.setEnabled(false);
		}
	}
}
