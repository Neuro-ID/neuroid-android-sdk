package com.neuroid.example

class DataManager {
    var allInfo: AllInfo = AllInfo()

    fun clearData() {
        allInfo.zip = ""
        allInfo.address = ""
        allInfo.city = ""
        allInfo.firstName = ""
        allInfo.lastName = ""
        allInfo.dob = ""
        allInfo.email = ""
        allInfo.phone = ""
        allInfo.employerPhone = ""
        allInfo.employerAddress = ""
        allInfo.employerName = ""
    }

}