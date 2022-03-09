package com.sample.neuroid.us

const val NID_STRUCT_CREATE_SESSION = "\\{\"type\":\"CREATE_SESSION\",\"ts\":\\d{13,},\"f\":\"(.*?)\",\"sid\":\"(.*?)\",\"cid\":\"(.*?)\",\"did\":\"(.*?)\",\"iid\":\"(.*?)\",\"loc\":\"(.*?)\",\"ua\":\"(.*?)\",\"tzo\":(.*?),\"lng\":\"(.*?)\",\"ce\":true,\"je\":true,\"ol\":true,\"p\":\"Android\",\"dnt\":false,\"url\":\"(.*?)\",\"ns\":\"nid\",\"jsl\":\"\\[na\\]\"\\}"
const val NID_STRUCT_USER_ID = "\\{\"type\":\"SET_USER_ID\",\"ts\":(.*?),\"uid\":\"(.*?)\"\\}"