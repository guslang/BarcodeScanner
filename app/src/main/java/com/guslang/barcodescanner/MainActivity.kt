package com.guslang.barcodescanner

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.*
import com.google.zxing.integration.android.IntentIntegrator
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.jsoup.Jsoup
import org.jsoup.select.Elements


class MainActivity : AppCompatActivity() {

    val TAG: String =  "로그"
    val errMsg : String = "유통물류 DB에 등록되지 않은 코드입니다."
//    var IsFindProduct = false
    lateinit var mAdView : AdView
    lateinit var mInterstitialAd: InterstitialAd
    var clickCnt = 0  //전면 광고를 띄우기 위한 카운트

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //firebase-admob 초기화
        //MobileAds.initialize(this,getString(R.string.admob_app_id))
        MobileAds.initialize(this) {}
        mAdView = findViewById(R.id.adView)
        val adRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)
        //
        // 전면광고 설정
        mInterstitialAd = InterstitialAd(this)
        mInterstitialAd.adUnitId = getString(R.string.Interstitial_ad_unit_id)
        mInterstitialAd.loadAd(AdRequest.Builder().build())

        mInterstitialAd.adListener = object: AdListener() {
            override fun onAdLoaded() {
                // Code to be executed when an ad finishes loading.
            }

            override fun onAdFailedToLoad(errorCode: Int) {
                // Code to be executed when an ad request fails.
            }

            override fun onAdOpened() {
                // Code to be executed when the ad is displayed.
            }

            override fun onAdClicked() {
                // Code to be executed when the user clicks on an ad.
            }

            override fun onAdLeftApplication() {
                // Code to be executed when the user has left the app.
            }

            override fun onAdClosed() {
                // Code to be executed when the interstitial ad is closed.
                mInterstitialAd.loadAd(AdRequest.Builder().build())
            }
        }
        //
        // 앱 공유하기
        imageView_share.setOnClickListener {
            Log.d(TAG, "MainActivity - onCreate() share sns called")

            val Sharing_intent = Intent(Intent.ACTION_SEND)
            Sharing_intent.type = "text/plain"

            val Test_Message = "https://play.google.com/store/apps/details?id=$packageName"

            Sharing_intent.putExtra(Intent.EXTRA_TEXT, Test_Message)

            val Sharing = Intent.createChooser(Sharing_intent, "공유하기")
            startActivity(Sharing)
        }
        // 별점 주기 이동
        imageView_rate.setOnClickListener {
            Log.d(TAG, "MainActivity - onCreate() rating called")
            val uri: Uri = Uri.parse("market://details?id=$packageName")
            val goToMarket = Intent(Intent.ACTION_VIEW, uri)
            // To count with Play market backstack, After pressing back button,
            // to taken back to our application, we need to add following flags to intent.
            goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or
                    Intent.FLAG_ACTIVITY_NEW_DOCUMENT or
                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            try {
                startActivity(goToMarket)
            } catch (e: ActivityNotFoundException) {
                startActivity(Intent(Intent.ACTION_VIEW,
                    Uri.parse("http://play.google.com/store/apps/details?id=$packageName")))
            }
        }
    }

    // 광고 로딩
    private fun loadInterstitialAd(){
        clickCnt += 1
        Log.d(TAG, "loadInterstitialAd: clickCnt = $clickCnt")

        if ( clickCnt > 5) {
            // admob 전면 광고
            if (mInterstitialAd.isLoaded) {
                mInterstitialAd.show()
            } else {
                Log.d(TAG, "changePattern: The interstitial wasn't loaded yet.")
            }
            clickCnt = 0
        }
    }

    private fun getDIP(dp: Int):Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    // 동적 버튼 생성
    private fun alterDynamicBtn(gb: Int) {
        btnView2.removeAllViews()
        when(gb){
            1 -> {
                makeButton(100, 50, 1)
                makeButton(100, 50, 4)
            }   // 도서
            2 -> {
                makeButton(100, 50, 2)
                makeButton(100, 50, 4)
            }   // URL
            3 -> {
                makeButton(100, 50, 3)
                makeButton(100, 50, 4)
            }   // 제품
            else -> {
                makeButton(100, 50, 4)
            }  // 기타
        }
    }
    // 바로가기 버튼 생성
    private fun makeButton(w: Int, h: Int, gb: Int) {
        Log.d(TAG, "makeButton: called - gb = $gb")
        val dynamicButton = Button(this).apply {
            width = getDIP(w)
            height =getDIP(h)
//            setTextColor(Color.parseColor("#000000"))
            background = getDrawable(R.drawable.rounded_corner5)
//            setPadding(15,15,15,15)
            // Layout Margin 추가
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.setMargins(20, 20, 20, 20);
            layoutParams = lp
            text = when(gb) {
                1 -> {
                    "Book"
                }
                2 -> {
                    "URL"
                }
                3 -> {
                    "Product"
                }
                else ->{
                    "Search"
                }
            }
            setOnClickListener {
                Log.d(TAG, "onCreate: dynamicButton clicked")
                when(gb) {
                    1 -> {
                        goShopping(1)
                    } //도서
                    2 -> {
                        goWebsite()
                    } // URL
                    3 -> {
                        goShopping(2)
                    } // 제품
                    else -> {
                        searchOnGoogle()
                    } // 기타
                }
            }
        }
        // Margin 설정
//        val lp = LinearLayout.LayoutParams(
//            ViewGroup.LayoutParams.WRAP_CONTENT,
//            ViewGroup.LayoutParams.WRAP_CONTENT
//        )
//        lp.setMargins(20,20,20,20);
//        dynamicButton.layoutParams = lp;
        // LinearView에 버튼 추가
        btnView2.addView(dynamicButton)
    }

    private fun changeDP(value: Int) : Int{
        var displayMetrics = resources.displayMetrics
        var dp = Math.round(value * displayMetrics.density)
        return dp
    }

    // 웹 URL 사이트로 이동하는 function
    private fun goWebsite(){
        Log.d(TAG, "goWebsite: called")
        val codeUrl : String= txtCode.text.toString()
        var intent = Intent(Intent.ACTION_VIEW, Uri.parse(codeUrl))
        startActivity(intent)
    }
    private fun goWebsite(codeUrl: String){
        Log.d(TAG, "goWebsite: called")
        var intent = Intent(Intent.ACTION_VIEW, Uri.parse(codeUrl))
        startActivity(intent)
    }

    // 구글 검색
    private fun searchOnGoogle(){
        Log.d(TAG, "searchOnGoogle: called")
        val keyword = if (txtProductName.text.toString() =="" || txtProductName.text.toString()  == errMsg) txtCode.text.toString()
                        else txtProductName.text.toString()
        val url = "https://www.google.com/search?q=$keyword"
        var intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }

    // 도서/쇼핑/검색하기
    private fun goShopping(gb: Int){
        Log.d(TAG, "goShopping: called")
        var url = ""
        when (gb) {
            //도서검색
            1 -> url = "https://www.google.com/search?tbm=bks&q=${txtCode.text}"
            //쇼핑
            2 -> url = "https://www.google.com/search?tbm=shop&q=${txtProductName.text}"
            else -> {
                searchOnGoogle()
            }
        }
//        val url = "https://www.coupang.com/np/search?component=&q=${txtProductName.text}&channel=user"
        var intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }

    // 상품정보 크롤링
    private fun setProductInfo(code: String) {
        Log.d(TAG, "getProductInfo: called")
        suspend fun getResultFromApi(): String {
            // do something
//            val code = txtProductName.text.toString()
            val url = "http://www.koreannet.or.kr/home/hpisSrchGtin.gs1?gtin=${code}"
            val doc = Jsoup.connect(url).timeout(1000 * 10).get()  //타임아웃 10초
            val contentData : Elements = doc.select("div.productTit")
            val productName = contentData.toString().substringAfterLast("&nbsp;").substringBefore("</div>")
            var rtnValue : String = ""
            if ( productName.toString().trim() !="" ) {
                rtnValue = productName.toString().trim()
//                IsFindProduct = true
            }
            else {
                rtnValue = errMsg //"유통물류 DB에 등록되지 않은 코드입니다."
//                IsFindProduct = false
            }
//            Log.d(TAG, "getProductInfo: called -IsFindProduct = $IsFindProduct")
            return rtnValue
        }

        CoroutineScope(IO).launch {
            val resultStr = withTimeoutOrNull(10000) {
                getResultFromApi()
            }

            if (resultStr != null) {
                withContext(Main) {
                    txtProductName.text = resultStr
                }
            }
        }
    }

    fun startBarcodeReaderCustomActivity(view: View) {
        Log.d(TAG, "startBarcodeReaderCustomActivity: called")
        txtProductName.text = ""
        txtCode.text = ""
        val integrator = IntentIntegrator(this)
        integrator.setBarcodeImageEnabled(true)
        integrator.captureActivity = MyBarcodeReaderActivity::class.java
        integrator.initiateScan()
        
        // 광고 호출
        loadInterstitialAd()
    }

    // QR/바코드 스캔 결과
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.d(TAG, "onActivityResult: called")
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents != null) {
                Log.d(TAG, "onActivityResult: result - ${result.contents}")
                txtCode.text = result.contents
                // 웹 사이트
                if (result.contents.startsWith("http", false)) {
                    Log.d(TAG, "onActivityResult: resultStr - $result.contents")
                    // 동적 버튼 생성
                    alterDynamicBtn(2)
                }
                // 상품인 경우
                else {
//                    Toast.makeText(
//                        this,
//                        "scanned: ${result.contents} format: ${result.formatName}",
//                        Toast.LENGTH_LONG
//                    ).show()
                    // 바코드 분류 (도서,상품)
                    if (result.contents.startsWith("97")){
                        // 도서
                        // 동적 버튼 생성
                        alterDynamicBtn(1)
                    } else {
                        // 상품정보 크롤링 호출
                        setProductInfo(result.contents)
                        // 동적 버튼 생성
//                        Log.d(TAG, "onActivityResult: IsFindProduct- $IsFindProduct")
//                        if (IsFindProduct)
                            alterDynamicBtn(3)
//                        else
//                            alterDynamicBtn(4)
                    }

                }

                // 스캔 결과 이미지 출력
//                if (result.barcodeImagePath != null) {
//                    val bitmap = BitmapFactory.decodeFile(result.barcodeImagePath)
//                    imgScanned.setImageBitmap(bitmap)
//                }

            } else {
                Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }

    }
    // 클릭 이벤트 처리
//    override fun onClick(v: View?) {
//        Log.d(TAG, "onClick: called")
//        when (v?.id) {
//            R.id.btnBook -> goShopping(1)   // 도서
//            R.id.btnShopping -> goShopping(2)  // 쇼핑
//            R.id.btnGoogle -> searchOnGoogle()   // 구글 검색
//            R.id.btnWeb -> goWebsite()  // URL 이동
//        }
//    }

    override fun onResume() {
        super.onResume()
//        startBarcodeReaderCustom()
    }


}
