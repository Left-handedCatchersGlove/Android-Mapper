package com.example.maper;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class markerEditSubMain extends Activity implements OnClickListener
{
	// タイトル用のエディットテキスト
	public EditText marker_editTitle;
	// 説明用のエディットテキスト
	public EditText marker_description;
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.marker_edit_main);
		
		// 戻るボタンの設定
		Button returnButton = (Button) findViewById(R.id.buttonEditReturn);
		// キャンセルボタンの設定
		Button cancelButton = (Button) findViewById(R.id.buttonEditCancrel);
		
		// 各ボタンにクリックイベントリスナーを設定
		returnButton.setOnClickListener(this);
		cancelButton.setOnClickListener(this);
		
		// インテント取得
		Intent intent = getIntent();
		// エディットテキストを取得
		marker_editTitle = (EditText) findViewById(R.id.editText_title);
		// 説明を取得
		marker_description = (EditText) findViewById(R.id.editText_description);
	}

	/**
	 * @brief ボタンの書き込み終了
	 */
	@Override
	public void onClick(View v)
	{
		switch(v.getId())
		{
		case R.id.buttonEditReturn: // マーカーの書き込み終了
			if( !marker_editTitle.getText().toString().equals("") && !marker_description.getText().toString().equals("") )
			{
				Intent intent = new Intent();
				// タイトルをMainActivityに送る
				intent.putExtra("INPUT_MARKER_TITLE", marker_editTitle
						.getText().toString());
				// 説明をMainActivityに送る
				intent.putExtra("INPUT_MARKER_DESCRIPTION", marker_description
						.getText().toString());
				// 元の画面に戻す情報
				setResult(RESULT_OK, intent);
				// Activityの終了
				finish();
			}
			else ShowToast("内容がないよう！");
			break;
		case R.id.buttonEditCancrel:
			setResult(RESULT_CANCELED);
			// Activityの終了
			finish();
			break;
		}
	}
	
	/**
	 * @brief 数秒間表示するメソッド
	 * @param string
	 */
	public void ShowToast(String string)
	{
		Toast t = Toast.makeText(this, string, Toast.LENGTH_SHORT);
		t.show();
	}
}
