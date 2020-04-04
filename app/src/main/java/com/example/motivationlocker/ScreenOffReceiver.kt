package com.example.motivationlocker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class ScreenOffReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        when{
            intent?.action == Intent.ACTION_SCREEN_OFF ->{
                Log.d(" ", "화면꺼짐")

                // 화면꺼지면 locker 액티비티 실행
                val intent = Intent(context, motivationLockerActivity::class.java)

                //새로운 액티비티로 실행
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                // 기존 액티비티 스택 제거
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

                // 액티비티 실행
                context?.startActivity(intent)
            }
        }

    }
}