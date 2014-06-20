package com.example.maper;

import static com.google.android.gms.maps.GoogleMap.MAP_TYPE_HYBRID;
import static com.google.android.gms.maps.GoogleMap.MAP_TYPE_NORMAL;
import static com.google.android.gms.maps.GoogleMap.MAP_TYPE_SATELLITE;
import static com.google.android.gms.maps.GoogleMap.MAP_TYPE_TERRAIN;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMyLocationChangeListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.PolylineOptions;

public class MainActivity extends android.support.v4.app.FragmentActivity implements ActionBar.OnNavigationListener, OnItemClickListener
{
	/*** マップ関係 ***/
	// GoogleMapクラスのインスタンス
	private GoogleMap mMap;
	// GoogleMapFragmentクラスのインスタンス
	private SupportMapFragment mapFragment;
	
	/*** 検索関係 ***/
	// ゲオコーダーするための配列
	List<Address> getList;
	// 検索した座標用の変数
	LatLng getPlace;
	// 検索した名前と説明
	String getName, getSnippet;
	
	/*** マーカーセット関係 ***/
	// アクションバーのメニュー用の配列
	ArrayAdapter<CharSequence> adapter;
	// Intent処理のための変数
	private static final int MARKER_EDIT_SUBACTIVITY = 1;
	// マーカーにセットするタイトル
	public String title;
	// マーカーにセットする説明
	public String description;
	// タッチした場所を保管しておく
	private LatLng tmpPoint;
	
	/** 描画関係 **/
	// マナー範囲の中心ポイント記録
	private LatLng manorRegionPoint;
	// AudioManagerで、マナーモードを設定
	private AudioManager am;

	/*** フラグ関係 ***/
	// マーカーセットのためのフラグ
	private boolean isMarkFlg = false;
	// アニメーションのためのフラグ
	private boolean isAnimFlg = true;
	// 歩き機能の回数のためのフラグ変数
	private int isDrawFlg = 2;
	// マーカー削除のためのフラグ
	private boolean isDeleteMarkerFlg = false;
	// マナー範囲決定のためのフラグ
	private boolean isManorRegionFlg = false;
	// 主観モード機能のためのフラグ
	private boolean isLookMeFlg = false;
	
	/*** フリッパー関係 ***/
	// フリッパークラス
	ViewFlipper vf;
	// リスト表示
	ListView lv;
	// リスト表示されたものの座標
	LatLng listLatLng[];
	// リスト表示させるための文字列リストの生成
	ArrayAdapter<String> addlistview;
	
	/** 各アニメーションの定義 **/
	private Animation inFromRightAnimation; // 右に入る
	private Animation inFromLeftAnimation;  // 左に入る
	private Animation outToRightAnimation;  // 右に出る
	private Animation outToLeftAnimation;   // 左に出る
	
	/** プログレスバー表示関係 **/
	// プログレスバー
	ProgressDialog progressDialog;
	// プログレス表示時間
	int SHOW_TIME = 3200;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		// レイアウトの定義
		setLayout();
		// レイアウトの読み込み
		setContentView(R.layout.activity_main);
		
		// フリッパーの定義
		vf = (ViewFlipper) findViewById(R.id.flipper);
		// リストビューの定義
		lv = (ListView) findViewById(R.id.listView);
		// リストにタッチイベントリスナーをセット
		lv.setOnItemClickListener(this);
		// アニメーションの設定
		inFromRightAnimation = AnimationUtils.loadAnimation(this, R.anim.right_in);
		inFromLeftAnimation  = AnimationUtils.loadAnimation(this, R.anim.left_in);
		outToRightAnimation  = AnimationUtils.loadAnimation(this, R.anim.right_out);
		outToLeftAnimation   = AnimationUtils.loadAnimation(this, R.anim.left_out);
		
		// マナーモードを管理する
		am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		
		// マップ生成
		if (mMap == null) // マップが生成されていないとき
		{
			if( savedInstanceState == null )
			{
				// Activityが初めて生成された
				mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
				setUpMap();
			}
			else
			{
				// Activityが再生成された
				mMap = mapFragment.getMap();
			}
		}
	}

	protected void setLayout()
	{
		final ActionBar actionBar = getActionBar();
		// アクションバーのモードをドロップダウンリストナビゲーションに変更する
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		// アプリタイトルを非表示に設定
		actionBar.setDisplayOptions(0, ActionBar.DISPLAY_SHOW_TITLE);
		// アダプタを作成する
		adapter = ArrayAdapter.createFromResource(this, R.array.action_list, android.R.layout.simple_list_item_1);
		// ナビゲーションにアダプタとコールバックをセットする
		actionBar.setListNavigationCallbacks(adapter, this);
	}

	/**
	 * @brief オプションメニューの生成
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		super.onCreateOptionsMenu(menu);
		// スプリットメニューの読み込み
		getMenuInflater().inflate(R.menu.split_menu, menu);
		return true;
	}

	/**
	 * @brief ナビゲーションリストを選択した際の処理。地図変更
	 */
	@Override
	public boolean onNavigationItemSelected(int itemPosition, long itemId)
	{
		switch ((int) adapter.getItemId(itemPosition))
		{
		case 0: // 通常状態のマップに変更
			mMap.setMapType(MAP_TYPE_NORMAL);
			ShowToast("NORMAL MAP");
			break;
		case 1: // 衛星写真のマップに変更
			mMap.setMapType(MAP_TYPE_SATELLITE);
			ShowToast("SATELLITE MAP");
			break;
		case 2: // 国道や線路などのマップに変更
			mMap.setMapType(MAP_TYPE_TERRAIN);
			ShowToast("TERRAIN MAP");
			break;
		case 3: // 1,2のどちらも兼ね備えたマップに変更
			mMap.setMapType(MAP_TYPE_HYBRID);
			ShowToast("HYBRID MAP");
			break;
		}
		return false;
	}

	/**
	 * @brief メニューアイテム選択イベントの処理
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
		/** MyPlaces：アクションメニュー **/
		case R.id.addMarker:      // マーカーをセットする
			if (item.isChecked()) // ラジオボタンが押されているとき
			{
				item.setChecked(false);
				isMarkFlg = false;
			}
			else
			{
				item.setChecked(true);
				isMarkFlg = true;
			}
			return true;
			
		case R.id.none:           // 通常のマップに戻る
			if(item.isChecked())
				item.setChecked(false);
			else
			{
				item.setChecked(true);
				// 通常のモードに戻るためマーカーフラグをオフ
				isMarkFlg = false;
			}
			return true;
			
		/** View:アクションメニュー **/
		case R.id.SearchPlace:    // 住所から建物を検索する
			// LayoutInflaterクラスを使用して、レイアウトからViewを作成する
			LayoutInflater inflater = LayoutInflater.from(this);
			// ビューを読み込む
			View view = inflater.inflate(R.layout.serach_dialog, null);
			final EditText tx = (EditText) view.findViewById(R.id.editText_SearchWord);
			// アラートダイアログを生成
			AlertDialog.Builder alert = new AlertDialog.Builder(this);
			// AlertDialogにビューを設定する
			alert.setTitle("検索するんじゃ");
			alert.setView(view);
			alert.setPositiveButton("検索", new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					// Geocorderするためのクラスのインスタンス
					Geocoder geoc = new Geocoder( MainActivity.this, Locale.getDefault() );
					try
					{
						// 末尾は検索結果数(１～5)
						getList = geoc.getFromLocationName(tx.getText().toString(), 1);
						if( getList.isEmpty() )
							ShowToast("List is empty");
						else
						{
							// 取得した情報を入れる
							Address address = getList.get(0);
							// 取得した座標を入れる
							getPlace = new LatLng(address.getLatitude(), address.getLongitude());
							// 検索結果の名前を入れる
							getName = address.getAddressLine(2);
							// 検索結果の説明を入れる
							getSnippet = address.getAddressLine(1);
							// マーカーのセット
							setAndAddMarker(getPlace, getName, getSnippet, BitmapDescriptorFactory.HUE_RED, 1, true);
						}
					}
					catch(IOException e) { ShowToast("IOException発生"); }
				}
			});
			alert.setNegativeButton("キャンセル", null);
			alert.show();
			return true;
			
		case R.id.lookMe:
			if( item.isChecked() )
			{
				item.setChecked(false);
				isLookMeFlg = false;
			}
			else
			{
				item.setChecked(true);
				ShowToast("主観モードになるんじゃ");
				isLookMeFlg = true;
			}
			return true;
			
		/** Edit:アクションメニュー **/
		case R.id.drawLine:
			ShowToast("開始位置をタップしてください!");
			if( isDrawFlg != 0 ) isDrawFlg = 0;
			return true;
			
		case R.id.drawPolygon:
			if( item.isChecked())
			{
				item.setChecked(false);
				isManorRegionFlg = false;
			}
			else
			{
				item.setChecked(true);
				ShowToast("マナーにしたい範囲を設定してください");
				isManorRegionFlg = true;
			}
			return true;

		/** Preferences:アクションメニュー **/
		case R.id.setAnim: // アニメーションをオフにするか
			if(item.isChecked())
			{
				item.setChecked(false);
				// アニメーションオン
				isAnimFlg = true;
			}
			else
			{
				item.setChecked(true);
				//　アニメーションアフ
				isAnimFlg = false;
			}
			return true;
			
		case R.id.clearMap: // マップを初期状態に戻す
			mMap.clear();
			return true;
			
		case R.id.deleteMarker: // マーカーを消す
			if(item.isChecked())
			{
				item.setChecked(false);
				// マーカー削除フラグオン
				isDeleteMarkerFlg = false;
			}
			else
			{
				item.setChecked(true);
				// マーカー削除フラグオフ
				isDeleteMarkerFlg = true;
			}
			return true;

		/** Delete：アクションメニュー **/
		case R.id.splitMenu_delete:
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * @brief マップ生成のメソッド
	 */
	private void setUpMap()
	{
		// 初期位置は岐阜大学
		LatLng place_first = new LatLng(35.464412, 136.737259);

		// マップが表示されるときに表示される文字列
		ShowToast("L('ω')┘三└('ω')」");

		/*** UI設定（GoogleMapクラスから） ***/
		// GoogleMapクラスからのUI設定
		mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
		mMap.setMyLocationEnabled(true);
		// 屋内マップ表示を有効にする
		mMap.setIndoorEnabled(true);
		// 現在位置表示の有効化
		mMap.setMyLocationEnabled(true);
		mMap.setTrafficEnabled(true);
		mMap.getUiSettings().setZoomControlsEnabled(false);

		/*** UI設定(UIsettingsから) ***/
		// 設定の取得
		UiSettings uiSet = mMap.getUiSettings();
		// ズームイン・アウトボタンの有効化
		uiSet.setZoomControlsEnabled(true);
		// ズームジェスチャー（ピンチイン・アウト）の有効化
		uiSet.setZoomGesturesEnabled(true);
		// 現在位置に移動するボタンの有効化
		uiSet.setMyLocationButtonEnabled(true);
		// すべてのジェスチャーの有効化
		uiSet.setAllGesturesEnabled(true);
		// 回転ジェスチャーの有効化
		uiSet.setRotateGesturesEnabled(true);
		// スクロールジェスチャーの有効化
		uiSet.setScrollGesturesEnabled(true);

		/** 現在地の更新 **/
		mMap.setOnMyLocationChangeListener(new OnMyLocationChangeListener()
		{
			@Override
			public void onMyLocationChange(Location myLoc)
			{
				if( isManorRegionFlg )
				{
					// 中心点との距離を計測
					double l = Math.sqrt( Math.pow( Math.abs(myLoc.getLatitude() - manorRegionPoint.latitude), 2) +
							Math.pow( Math.abs(myLoc.getLongitude() - manorRegionPoint.longitude), 2) );
					if( l <= 0.005f )
					{
						// マナーモードに設定
						am.setRingerMode(AudioManager.RINGER_MODE_SILENT);
					}
				}
				if( isLookMeFlg )
				{
					// GPSが変わったときに常に座標を記録する
					LatLng now = new LatLng( myLoc.getLatitude(), myLoc.getLongitude() );
					// カメラを常に固定
					moveCameraToLatLng( now, 28.0f, 90.0f, myLoc.getBearing() );
				}
			}
		});

		/** 長押し処理 **/
		mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener()
		{
			@Override
			public void onMapLongClick(LatLng point)
			{
				if ( isMarkFlg )
				{
					// 内容を書いたときに仮のポイントを入れる
					tmpPoint = point;
					// マーカーの内容をセット
					markerIntentStart();
				}
				if( isManorRegionFlg )
				{
					// 描画するポイントを記録
					manorRegionPoint = point;
					setDrawPolygon( point );
				}
			}
		});
		
		/** マップをタップしたときの処理 **/
		mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener()
		{
			@Override
			public void onMapClick(LatLng point)
			{
				if( isDrawFlg == 0 ) // 1回目のタップ
				{
					// マーカーの設置
					setAndAddMarker( point, "start", "開始位置です", 0, 2, false );
					// 2回目に向けてのフラグの加算
					isDrawFlg++;
					//　メッセージの出力
					ShowToast("次は終了位置をタップしてください");
					// タップした座標
					tmpPoint = point;
				}
				else if( isDrawFlg == 1 ) // 2回目のタップ
				{
					// 広すぎると時間がかかるため、特定の距離しか許可しない
					if( Math.abs(tmpPoint.latitude - point.latitude) < 0.01f && Math.abs(tmpPoint.longitude - point.longitude) < 0.01f  )
					{
						// ダイアログの表示
						ShowDialog();
						// マーカーの設置
						setAndAddMarker(point, "End", "目標位置です", 0, 2, false);
						// もう入ってこれないように加算
						isDrawFlg++;
						// ラインを引く
						setDrawLine(tmpPoint, point);
						// 周辺情報をリスト情報で取得
						if( setAndAddListView( tmpPoint, point ) != 0 ) // 検索成功
							// 次のページに行くアニメーション
							nextAnimation();
						else // 検索失敗
							mMap.clear();
					}
					else ShowToast("それは散歩できる距離じゃないゾ☆");
				}
			}
		});
		
		/** 選択したマーカーの処理 **/
		mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener()
		{
			public void onInfoWindowClick(Marker m)
			{
				if( isDeleteMarkerFlg )
				{
					ShowToast("削除します");
					m.remove();
				}
			}
		});
		
		// 目的地までカメラを移動
		moveCameraToLatLng(place_first, 18.0f, 90.0f, 0.0f);
	}

	/**
	 * @brief マーカーの内容をセットする
	 */
	public void markerIntentStart()
	{
		// サブアクティビティーのコンストラクタ
		Intent intent = new Intent(this, markerEditSubMain.class);
		// サブアクティビティー起動
		startActivityForResult(intent, MARKER_EDIT_SUBACTIVITY);
	}

	/*
	 * @brief サブアクティビティーから返ってきたときに入る関数
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent)
	{
		super.onActivityResult(requestCode, resultCode, intent);
		if( requestCode == MARKER_EDIT_SUBACTIVITY )
		{
			if( resultCode == RESULT_OK )
			{
				// タイトルを受け取る
				Bundle extras_title = intent.getExtras();
				if( extras_title != null )
					// 受け取ったタイトルをセット
					title = extras_title.getString("INPUT_MARKER_TITLE");
				else
					title = "extra is null";

				// 説明を受け取る
				Bundle extras_descr = intent.getExtras();
				if( extras_descr != null )
					// 受け取った説明をセット
					description = extras_descr.getString("INPUT_MARKER_DESCRIPTION");
				else
					description = "extras is null";
				
				// マーカーのセット
				setAndAddMarker(tmpPoint, title, description, BitmapDescriptorFactory.HUE_BLUE, 0, true);
			}
		}
	}
	
	/**
	 * @brief 線付近の調査
	 * @param start : 開始地点
	 * @param end   : 終端地点
	 */
	protected int setAndAddListView( LatLng start, LatLng end )
	{
		double i = 0.00025f;
		// アダプターの宣言
		addlistview = new ArrayAdapter<String>( this, R.layout.list );
		// アダプターの設定
		lv.setAdapter(addlistview);
		listLatLng = new LatLng[50];
		// Geocorderするためのクラスのインスタンス
		Geocoder geoc = new Geocoder(MainActivity.this, Locale.getDefault());
		String preGeoc = "";
		String prePreGeoc = "";
		int count = 0;
		// 縦のループ(latitude)
		while ( end.latitude + i < start.latitude )
		{
			// 横のループ（longtitude） 0.0001は若干重いかも
			
			for (double j = 0; end.longitude + j < start.longitude; j += 0.00025f)
			{
				try
				{
					// 位置をずらして情報を取る
					List<Address> addList = geoc.getFromLocation( end.latitude + i, end.longitude + j, 1 );
					
					if ( !addList.isEmpty() ) // 情報があった場合
					{
						Address address;
						// 情報を取得する
						address = addList.get(0);
						if( !preGeoc.equals(address.getAddressLine(1)) && !prePreGeoc.equals(address.getAddressLine(1)) && !address.getAddressLine(1).equals("日本") )
						{
							listLatLng[count] = new LatLng(end.latitude + i, end.longitude + j);
							count++;
							addlistview.add( address.getAddressLine(1) );
							prePreGeoc = preGeoc;
							preGeoc = address.getAddressLine(1);
						}
					}
				}
				catch (IOException e)
				{
					ShowToast("IOException発生:よって、中止");
					break;
				}
			}
			i += 0.00025f;
		}
		return count;
	}
	
	/**
	 * @brief プログレスバーの表示
	 */
	protected void ShowDialog() {
		// ProgressDialogクラスのインスタンスを生成
		progressDialog = new ProgressDialog(this);
		// プログレスダイアログのスタイルを設定
		progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		// キャンセル可能にする
		progressDialog.setCancelable(true);
		// タイトルを表示
		progressDialog.setTitle("起動");
		// メッセージを表示
		progressDialog.setMessage("しばし待たれよ！");
		// プログレスダイアログを表示
		progressDialog.show();
		// 4秒後にプログレスダイアログを終了
		new Handler().postDelayed(new Runnable() {
			public void run() {
				progressDialog.dismiss();
			}
		}, SHOW_TIME);
	}
	
	/**
	 * @brief リストのアイテムをクリックしたときの処理
	 */
	@Override
	public void onItemClick(AdapterView<?> parent, View v, int pos, long id)
	{
		ListView list = (ListView) parent;
		String item = (String) list.getItemAtPosition(pos);
		mMap.clear();
		setAndAddMarker(listLatLng[(int)list.getItemIdAtPosition(pos)], item, item, 0, 2, true);
		previousAnimation();
	}
	
	/**
	 * @brief 線を引く関数
	 * @param start : 開始座標
	 * @param end   : 終了座標
	 */
	protected void setDrawLine( LatLng start, LatLng end )
	{
		// 設定
		PolylineOptions options = new PolylineOptions();
		// 線の始端
		options.add(start);
		// 線の終端
		options.add(end);
		// 線の色
		options.color(Color.BLACK);
		// 線の太さ
		options.width(10);
		// 測地線で表示
		options.geodesic(true);
		// 線を描画
		mMap.addPolyline(options);
	}
	
	/**
	 * @brief 図形をセットする
	 * @param targetLatLng : 中心位置
	 */
	protected void setDrawPolygon( LatLng targetLatLng )
	{
		// 前の描画を全てクリア
		mMap.clear();
		// 設定
	    PolygonOptions options = new PolygonOptions();
	    // 点の数
	    int numPoints = 400;
	    // 横サイズ
        float semiHorizontalAxis = 0.005f;
        // 縦サイズ
        float semiVerticalAxis = 0.005f;
        // 分割
        double phase = 2 * Math.PI / numPoints;
        for( int i = 0; i <= numPoints; i++ )
        {
			options.add(new LatLng(targetLatLng.latitude + semiVerticalAxis
					* Math.sin(i * phase), targetLatLng.longitude
					+ semiHorizontalAxis * Math.cos(i * phase)));
        }
		// 塗り
		options.fillColor(0x44ff0000);
		// 線
		options.strokeColor(Color.RED);
		// 図形を描画
		mMap.addPolygon(options);
	}
	
	/**
	 * @brief マーカーを任意の位置にセットする
	 * @param target  : 緯度経度情報
	 * @param title   : セットするタイトル
	 * @param snippet : セットする説明
	 * @param icon    : セットするiconの種類
	 * @param flg     : 0:マーカー 1:検索 2:edit
	 */
	protected void setAndAddMarker( LatLng target, String title, String snippet, float icon, int flg, boolean isMoveCamera )
	{
		// マーカーのオプション設定
		MarkerOptions options = new MarkerOptions();
		// 緯度・軽度設定
		options.position(target);
		// タイトル・スニペット設定
		options.title(title);
		options.snippet(snippet);
		switch(flg)
		{
		case 0: // マーカー
			// マーカーの色
			options.icon(BitmapDescriptorFactory.defaultMarker(icon));
			break;
		case 1: // 検索
			// アイコン
			options.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_menu_view));
			break;
		case 2:
			// 開始点と終了点
			options.icon(BitmapDescriptorFactory.fromResource(R.drawable.edit));
			break;
		}
		// マーカーを貼り付け
		mMap.addMarker(options);
		if( isMoveCamera )
		  // カメラを移動
		  moveCameraToLatLng(target, 18.0f, 90.0f, 0.0f);
	}

	/**
	 * @brief 渡された目的地までカメラを移動する
	 * @param isAnimFlg : アニメーションで移動するかの判断フラグ
	 * @param target    : 目的地の座標
	 * @param zoom      : ズーム
	 * @param tilt      : チルトアングル
	 * @param bearing   : 向き
	 */
	private void moveCameraToLatLng(LatLng target, float zoom, float tilt, float bearing)
	{
		// カメラの詳細なポジション設定
		CameraPosition pos = new CameraPosition(target, zoom, tilt, bearing);
		// 設定したカメラ情報でアップデート
		CameraUpdate camera = CameraUpdateFactory.newCameraPosition(pos);

		if (isAnimFlg) // アニメーション有り
			// アップデートした内容にカメラを移動していく
			mMap.animateCamera(camera);
		else
			// アニメーションなし
			mMap.moveCamera(camera);
	}
	
	/**
	 * @brief 次のビューに行くためのアニメーション
	 */
	protected void nextAnimation()
	{
		vf.setInAnimation(inFromRightAnimation);
		vf.setOutAnimation(outToLeftAnimation);
		vf.showNext();
	}
	
	/**
	 * @brief 前のビューに戻るアニメーション
	 */
	protected void previousAnimation()
	{
		vf.setInAnimation(inFromLeftAnimation);
		vf.setOutAnimation(outToRightAnimation);
		vf.showPrevious();
	}

	/**
	 * @brief 数秒間表示するメソッド
	 * @param string
	 */
	public void ShowToast(String string) {
		Toast t = Toast.makeText(this, string, Toast.LENGTH_SHORT);
		t.show();
	}
}
