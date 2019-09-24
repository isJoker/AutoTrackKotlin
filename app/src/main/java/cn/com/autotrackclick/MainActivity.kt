package cn.com.autotrackclick

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.TextView
import android.widget.Toast

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<TextView>(R.id.tv_content).setOnClickListener(onClickListener)
    }

    private val onClickListener = View.OnClickListener {
        Toast.makeText(this,"点击",Toast.LENGTH_SHORT).show()
    }
}
