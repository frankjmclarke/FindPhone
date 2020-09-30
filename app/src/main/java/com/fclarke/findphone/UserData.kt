package com.fclarke.findphone

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences

class UserData(context: Context) {
    var context: Context? = context
    private var sharedPrefs: SharedPreferences? = null

    init {
        this.sharedPrefs = context.getSharedPreferences("userData", Context.MODE_PRIVATE)
    }

    fun savePhone(phoneNumber: String) {
        val editor = sharedPrefs!!.edit()
        editor.putString("phoneNumber", phoneNumber)
        editor.apply()
    }

    fun loadPhoneNumber(): String {
        val phoneNumber = sharedPrefs!!.getString("phoneNumber", "empty")
        return phoneNumber!!
    }

    fun isFirstTimeLoad() {
        val phoneNumber = sharedPrefs!!.getString("phoneNumber", "empty")
        if (phoneNumber.equals("empty")) {
            val intent = Intent(context, Login::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context!!.startActivity(intent)
        }
    }

    fun saveContactInfo() {
        var listOfTrackers = ""
        for ((key, value) in myTrackers) {

            if (listOfTrackers.isEmpty()) {
                listOfTrackers = "$key%$value"
            } else {
                listOfTrackers += "%$key%$value"
            }
        }

        if (listOfTrackers.isEmpty()) {
            listOfTrackers = "empty"
        }

        val editor = sharedPrefs!!.edit()
        editor.putString("listOfTrackers", listOfTrackers)
        editor.apply()
    }


    fun loadContactInfo() {
        myTrackers.clear()
        val listOfTrackers = sharedPrefs!!.getString("listOfTrackers", "empty")

        if (!listOfTrackers.equals("empty")) {
            val usersInfo = listOfTrackers!!.split("%").toTypedArray()
            var i = 0
            while (i < usersInfo.size) {

                myTrackers[usersInfo[i]] = usersInfo[i + 1]
                i += 2
            }
        }
    }

    companion object {
        var myTrackers: MutableMap<String, String> = HashMap()
        fun formatPhoneNumber(phoneNumber: String): String {
            var onlyNumber = phoneNumber.replace("[^0-9]".toRegex(), "")
            if (phoneNumber[0] == '+') {
                onlyNumber = "+$phoneNumber"
            }
            return onlyNumber
        }
    }

}
