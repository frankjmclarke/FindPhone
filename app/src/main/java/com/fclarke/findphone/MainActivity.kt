package com.fclarke.findphone

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.ContactsContract
import androidx.core.app.ActivityCompat
import android.view.*
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.Toast
import com.google.firebase.database.*
import  kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.contact_ticket.view.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    var adapter: ContactAdapter?=null
    var listOfContact=ArrayList<UserContact>()
    var databaseRef:DatabaseReference?=null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val userData= UserData(this)
        userData.isFirstTimeLoad()

        databaseRef= FirebaseDatabase.getInstance().reference

        // For Deby=ug only
        //dummpyData()

        adapter = ContactAdapter(this, listOfContact)
        lvContactList.adapter= adapter
        lvContactList.onItemClickListener= AdapterView.OnItemClickListener{
            parent,view,postion,id ->
            val userInfo =listOfContact[postion]
            val df =SimpleDateFormat("yyyy/MMM/dd HH:MM:ss")
            val date =Date()
            // save to datetime database
            databaseRef!!.child("Users").child(userInfo.phoneNumber!!).child("request").setValue(df.format(date).toString())

            val intent =Intent(applicationContext,MapsActivity::class.java)
            intent.putExtra("phoneNumber",userInfo.phoneNumber)
            startActivity(intent)
        }



    }


    override fun onResume() {
        super.onResume()

        val userData= UserData(this)
        if (userData.loadPhoneNumber()=="empty"){
            return
        }
        refreshUsers()

        if (MyService.isServiceRunning) return // Donot run again
        checkContactPermission()
        checkLocationPermission()

    }

    fun refreshUsers(){
         val userData= UserData(this)
        databaseRef!!.child("Users").
                child(userData.loadPhoneNumber()).
                child("Finders").addValueEventListener(object :
        ValueEventListener{

            override fun onDataChange(dataSnapshot: DataSnapshot) {
               try {
                   val td = dataSnapshot.value as HashMap<String,Any>

                   listOfContact.clear()

                   for (key in td.keys){
                       val name = listOfContacts[key]
                       listOfContact.add(UserContact(name.toString() ,key))
                   }

                   adapter!!.notifyDataSetChanged()
               }catch (ex:Exception){
                   listOfContact.clear()
                   listOfContact.add(UserContact("NO_USERS","nothing"))
                   adapter!!.notifyDataSetChanged()
                   return
               }
            }

            override fun onCancelled(p0: DatabaseError) {

            }
        })
    }
    //for debug first time
    fun dummpyData(){
        listOfContact.add(UserContact("frank","123444"))
        listOfContact.add(UserContact("jena","344343"))
        listOfContact.add(UserContact("laya","434543"))
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {

        val inflater=menuInflater
        inflater.inflate(R.menu.main_menu,menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when(item!!.itemId){
            R.id.addTracker ->{
                val intent= Intent(this,MyTrackers::class.java)
                startActivity(intent)
            }
            R.id.help ->{
                //TODO:: ask for help from friend
            }
            else ->{
                return super.onOptionsItemSelected(item)
            }
        }

        return true
    }



    class ContactAdapter(context: Context, var listOfContact: ArrayList<UserContact>) : BaseAdapter() {
        var context: Context?= context
        override fun getView(p0: Int, p1: View?, p2: ViewGroup?): View {
            val userContact = listOfContact[p0]

            if (userContact.name.equals("NO_USERS")){
                val inflator = context!!.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                return inflator.inflate(R.layout.no_user, null)
            }else {
                val inflator = context!!.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                val contactTicketView = inflator.inflate(R.layout.contact_ticket, null)
                contactTicketView.tvName.text = userContact.name
                contactTicketView.tvPhoneNumber.text = userContact.phoneNumber

                return contactTicketView
            }
        }

        override fun getItem(p0: Int): Any {

            return listOfContact[p0]
        }

        override fun getItemId(p0: Int): Long {
            return p0.toLong()
        }

        override fun getCount(): Int {

            return listOfContact.size
        }

    }

    private val contactCode =123
    private fun checkContactPermission(){
        if(Build.VERSION.SDK_INT>=23){
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_CONTACTS) !=
                    PackageManager.PERMISSION_GRANTED ){
                requestPermissions(arrayOf(android.Manifest.permission.READ_CONTACTS), contactCode)
                return
            }
        }
        loadContact()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {

        when (requestCode) {
            contactCode-> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    loadContact()
                } else {
                    Toast.makeText(this, "Cannot acces to contact ", Toast.LENGTH_LONG).show()
                }
            }
            locationCode->{
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getUserLocation()
                } else {
                    Toast.makeText(this, "Cannot acces to contact ", Toast.LENGTH_LONG).show()
                }
            }
            else ->{
                super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            }
        }
    }

    var listOfContacts=HashMap<String,String>()
    private fun loadContact() {

        try{
            listOfContacts.clear()

            val cursor=contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,null,null,null,null)
            cursor!!.moveToFirst()
            do {
                val name= cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                val phoneNumber=cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))

                listOfContacts[UserData.formatPhoneNumber(phoneNumber)] = name
            }while (cursor.moveToNext())
        }catch (ex:Exception){}
    }



    private val locationCode =124
    private fun checkLocationPermission(){
        getUserLocation()
    }

    private fun getUserLocation(){

        // Start service
        if(!MyService.isServiceRunning){
            val intent= Intent(baseContext,MyService::class.java)
            startService(intent)
        }

    }

}
