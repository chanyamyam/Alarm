package com.app.part3.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import java.util.*

class MainActivity : AppCompatActivity() {

    private val onOffButton: Button by lazy {
        findViewById(R.id.onOffButton)
    }
    private val changeAlarmButton: Button by lazy {
        findViewById(R.id.changeAlarmButton)
    }
    private val amPmTextView: TextView by lazy {
        findViewById(R.id.ampmTextView)
    }
    private val timeTextView: TextView by lazy {
        findViewById(R.id.timeTextView)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //step0 뷰를 초기화해주기
        initViews()
        //step1 데이터 가져오기

        //step2 뷰에 데이터를 그려주기 -> 어떤거해야될지 적어놓고 작업하기기
    }
    private fun initViews() {
        initOnOffButton()
        initChangeTimeButton()

        val model = fetchDataFromSharedPreferences()
        renderView(model)
    }
    private fun initOnOffButton() {
        onOffButton.setOnClickListener {
            // 데이터를 확인
            val model = it.tag as? AlarmDisplayModel ?: return@setOnClickListener
            val newModel = saveAlarmModel(model.hour,model.minute, !model.onOff)
            renderView(newModel)


            // 온오프에 따라 작업을 처리한다.

            if(newModel.onOff) {
                // 켜진 경우 -> 알람을 등록
                val calendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, newModel.hour)
                    set(Calendar.MINUTE, newModel.minute)
                    if(before(Calendar.getInstance())) {
                        add(Calendar.DATE,1)
                    }
                }
                val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val intent = Intent(this, AlarmReceiver::class.java)
                val pendingIntent = PendingIntent.getBroadcast(this, ALARM_REQUEST_CODE,
                    intent, PendingIntent.FLAG_UPDATE_CURRENT)

                alarmManager.setInexactRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
                )

            } else {
                // 꺼진 경우 -> 알람을 제거
                cancelAlarm()
            }


            // 오프 -> 알람을 제거
            // 온 -> 알람을 등록

            // 데이터를 저장한다.
        }
    }

    private fun initChangeTimeButton(){
        changeAlarmButton.setOnClickListener {
            // 현재시간을 일단 가져온다.
            val calendar = Calendar.getInstance()

            // TimePickDialog 띄워줘서 시간 설정을 하도록 하게끔 하고
            TimePickerDialog(this, {picker,hour,minute ->

                // 그 결과를 가져와서 데이터를 저장한다.
                val model = saveAlarmModel(hour,minute,false)

                // 뷰를 업데이트
                renderView(model)
                // 기존에 있던 알람을 삭제한다.
                cancelAlarm()


            }, calendar.get(Calendar.HOUR_OF_DAY),calendar.get(Calendar.MINUTE),false)
                .show()
        }
    }

    private fun saveAlarmModel(
        hour: Int,
        minute: Int,
        onOff: Boolean
    ): AlarmDisplayModel {
        val model = AlarmDisplayModel(
            hour = hour,
            minute = minute,
            onOff = onOff
        )

        val sharedPreference = getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)

        with(sharedPreference.edit()) {
            putString(AlARM_KEY, model.makeDataForDB())
            putBoolean(ONOFF_KEY,model.onOff)
        }

        return model
    }

    private fun fetchDataFromSharedPreferences(): AlarmDisplayModel {
        val sharedPreferences = getSharedPreferences(SHARED_PREFERENCES_NAME,Context.MODE_PRIVATE )

        val timeDBValue = sharedPreferences.getString(AlARM_KEY, "9:30") ?: "9:30"
        val onOffDBValue = sharedPreferences.getBoolean(ONOFF_KEY, false)
        val alarmData = timeDBValue.split(":")

        val alarmModel = AlarmDisplayModel(
            hour = alarmData[0].toInt(),
            minute = alarmData[1].toInt(),
            onOff = onOffDBValue
        )

        val pendingIntent = PendingIntent.getBroadcast(this, ALARM_REQUEST_CODE,
            Intent(this,AlarmReceiver::class.java),PendingIntent.FLAG_NO_CREATE)

        if((pendingIntent == null) and alarmModel.onOff) {
            // 알람은 꺼져있는데, 데이터는 켜져있는 경우
            alarmModel.onOff = false
        } else if ((pendingIntent != null) and alarmModel.onOff){
            //알람은 켜져있는데, 데이터는 꺼져있는 경우 알람 취소
            pendingIntent.cancel()
        }

        return alarmModel
    }

    private fun renderView(model: AlarmDisplayModel) {
        amPmTextView.apply {
            text = model.ampmText
        }
        timeTextView.apply {
            text = model.timeText
        }
        onOffButton.apply {
            text = model.onOffText
            tag = model
        }

    }

    private fun cancelAlarm() {
        val pendingIntent = PendingIntent.getBroadcast(this, ALARM_REQUEST_CODE,
            Intent(this,AlarmReceiver::class.java),PendingIntent.FLAG_NO_CREATE)
        pendingIntent?.cancel()
    }

    companion object {
        private const val AlARM_KEY = "alarm"
        private const val ONOFF_KEY = "onOff"
        private const val SHARED_PREFERENCES_NAME = "time"
        private const val ALARM_REQUEST_CODE = 1000
    }
}