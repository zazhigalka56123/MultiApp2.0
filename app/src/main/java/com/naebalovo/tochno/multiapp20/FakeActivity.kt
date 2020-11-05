package com.naebalovo.tochno.multiapp20

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import rubikstudio.library.LuckyWheelView
import rubikstudio.library.model.LuckyItem

class FakeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_fake)
        val luckyWheelView = findViewById<LuckyWheelView>(R.id.luckyWheel)


        val data: MutableList<LuckyItem> = ArrayList()


        val luckyItem1 = LuckyItem()
        luckyItem1.secondaryText = "X2"

        val luckyItem2 = LuckyItem()
        luckyItem2.secondaryText = "LOOSE"

        val luckyItem3 = LuckyItem()
        luckyItem3.secondaryText = "X10"

        val luckyItem4 = LuckyItem()
        luckyItem4.secondaryText = "LOOSE"

        val luckyItem5 = LuckyItem()
        luckyItem5.secondaryText = "X3"

        val luckyItem6 = LuckyItem()
        luckyItem6.secondaryText = "LOOSE"

        data.add(luckyItem1)
        data.add(luckyItem2)
        data.add(luckyItem3)
        data.add(luckyItem4)
        data.add(luckyItem5)
        data.add(luckyItem6)


        luckyWheelView.setData(data)
        luckyWheelView.setRound(6)



    }
}